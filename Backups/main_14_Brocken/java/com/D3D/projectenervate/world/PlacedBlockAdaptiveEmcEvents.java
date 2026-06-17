package com.D3D.projectenervate.world;

import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import com.D3D.projectenervate.emc.AdaptiveEmcOutputHelper;
import com.D3D.projectenervate.emc.AdaptiveEmcValues;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class PlacedBlockAdaptiveEmcEvents {

    private static final int PENDING_PLACEMENT_TIMEOUT_TICKS = 10;
    private static final int NATURAL_DROP_BUDGET_TIMEOUT_TICKS = 20;

    private static final Map<UUID, PendingPlacement> PENDING_PLACEMENTS = new HashMap<>();
    private static final Map<NaturalDropBudgetKey, NaturalDropBudget> NATURAL_DROP_BUDGETS = new HashMap<>();

    private PlacedBlockAdaptiveEmcEvents() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (stack.isEmpty()) {
            return;
        }

        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        Item expectedPlacedItem = blockItem.getBlock().asItem();

        if (expectedPlacedItem == Items.AIR) {
            return;
        }

        BigDecimal placedBlockEmc = AdaptiveEmcHelper.getSingleSellValueDecimal(stack);

        // Placement must preserve the stack's effective economic value even when the
        // block later drops itself, drops multiple items, or drops a different item.
        // A clean/unverified base-EMC block has an effective value of zero, so it is
        // stored as zero instead of being allowed to become verified after breaking.
        if (placedBlockEmc.signum() <= 0 && !ProjectEnervateSourceHelper.hasBaseEmc(stack)) {
            return;
        }

        PENDING_PLACEMENTS.put(
                event.getEntity().getUUID(),
                new PendingPlacement(
                        expectedPlacedItem,
                        placedBlockEmc.max(BigDecimal.ZERO),
                        true,
                        level.getGameTime()
                )
        );
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Always clear stale data at this position first.
        // This prevents old EMC from surviving if the block was replaced.
        PlacedBlockAdaptiveEmcData.get(level).remove(event.getPos());
        projectenervate$clearNaturalDropBudget(level, event.getPos());

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PendingPlacement pending = PENDING_PLACEMENTS.remove(player.getUUID());

        if (pending == null) {
            return;
        }

        long now = level.getGameTime();

        if (now - pending.gameTime() > PENDING_PLACEMENT_TIMEOUT_TICKS) {
            return;
        }

        BlockState placedState = event.getPlacedBlock();
        Item actualPlacedItem = placedState.getBlock().asItem();

        if (actualPlacedItem != pending.expectedPlacedItem()) {
            return;
        }

        if (pending.alwaysApply()) {
            PlacedBlockAdaptiveEmcData.get(level).put(
                    event.getPos(),
                    pending.emcPerPlacedBlock(),
                    true
            );
            return;
        }

        if (!projectenervate$mightCreateLoopWhenBroken(level, event.getPos(), placedState)) {
            return;
        }

        PlacedBlockAdaptiveEmcData.get(level).put(
                event.getPos(),
                pending.emcPerPlacedBlock(),
                false
        );
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        projectenervate$clearNaturalDropBudget(level, event.getPos());

        Optional<PlacedBlockAdaptiveEmcData.Entry> storedEntry = PlacedBlockAdaptiveEmcData
                .get(level)
                .remove(event.getPos());

        if (storedEntry.isEmpty()) {
            return;
        }

        PlacedBlockAdaptiveEmcData.Entry entry = storedEntry.get();

        if (entry.emc().signum() <= 0) {
            if (entry.alwaysApply()) {
                projectenervate$applyUnknownSourceToDrops(event.getDrops());
            }
            return;
        }

        if (!entry.alwaysApply() && !projectenervate$actualDropsCreateLoopRisk(event)) {
            return;
        }

        projectenervate$applyBlockEmcToDrops(entry.emc(), event.getDrops());
    }

    public static void onTrackedBlockStateRemoved(
            Level level,
            BlockPos pos,
            BlockState oldState,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (pos == null) {
            return;
        }

        if (movedByPiston) {
            return;
        }

        if (oldState == null || oldState.isAir()) {
            return;
        }

        if (newState != null && oldState.getBlock() == newState.getBlock()) {
            return;
        }

        PlacedBlockAdaptiveEmcData.get(serverLevel).remove(pos);
        projectenervate$clearNaturalDropBudget(serverLevel, pos);
    }

    public static boolean applyStoredBlockEmcToPoppedStack(Level level, BlockPos pos, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (stack.isEmpty()) {
            return false;
        }

        projectenervate$clearExpiredNaturalDropBudgets(serverLevel);

        NaturalDropBudgetKey key = new NaturalDropBudgetKey(serverLevel.dimension(), pos.asLong());
        NaturalDropBudget budget = NATURAL_DROP_BUDGETS.get(key);

        if (budget == null) {
            Optional<PlacedBlockAdaptiveEmcData.Entry> storedEntry = PlacedBlockAdaptiveEmcData
                    .get(serverLevel)
                    .remove(pos);

            if (storedEntry.isEmpty()) {
                return false;
            }

            PlacedBlockAdaptiveEmcData.Entry entry = storedEntry.get();
            budget = new NaturalDropBudget(
                    entry.emc().max(BigDecimal.ZERO),
                    entry.alwaysApply(),
                    serverLevel.getGameTime()
            );
            NATURAL_DROP_BUDGETS.put(key, budget);
        }

        if (budget.remainingEmc().signum() <= 0) {
            if (budget.alwaysApply()) {
                ProjectEnervateSourceHelper.markUnknownSource(stack);
                return true;
            }

            return false;
        }

        BigDecimal baseStackEmc = AdaptiveEmcOutputHelper.getBaseStackEmc(stack);

        if (baseStackEmc.signum() <= 0) {
            ProjectEnervateSourceHelper.clearProjectEnervateData(stack);
            return true;
        }

        BigDecimal stackBudget = budget.takeUpTo(baseStackEmc);

        AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(stack, stackBudget);
        return true;
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING_PLACEMENTS.remove(event.getEntity().getUUID());
    }

    private static void projectenervate$clearNaturalDropBudget(ServerLevel level, BlockPos pos) {
        NATURAL_DROP_BUDGETS.remove(new NaturalDropBudgetKey(level.dimension(), pos.asLong()));
    }

    private static void projectenervate$clearExpiredNaturalDropBudgets(ServerLevel level) {
        long now = level.getGameTime();
        ResourceKey<Level> dimension = level.dimension();

        NATURAL_DROP_BUDGETS.entrySet().removeIf(entry ->
                entry.getKey().dimension().equals(dimension)
                        && now - entry.getValue().gameTime() > NATURAL_DROP_BUDGET_TIMEOUT_TICKS
        );
    }

    private static boolean projectenervate$mightCreateLoopWhenBroken(
            ServerLevel level,
            BlockPos pos,
            BlockState placedState
    ) {
        Item placedBlockItem = placedState.getBlock().asItem();

        if (placedBlockItem == Items.AIR) {
            return true;
        }

        List<ItemStack> simulatedDrops = Block.getDrops(
                placedState,
                level,
                pos,
                null
        );

        if (simulatedDrops.size() != 1) {
            return true;
        }

        ItemStack onlyDrop = simulatedDrops.get(0);

        if (onlyDrop.isEmpty()) {
            return true;
        }

        if (onlyDrop.getCount() != 1) {
            return true;
        }

        return onlyDrop.getItem() != placedBlockItem;
    }

    private static boolean projectenervate$actualDropsCreateLoopRisk(BlockDropsEvent event) {
        Item placedBlockItem = event.getState().getBlock().asItem();

        if (placedBlockItem == Items.AIR) {
            return true;
        }

        int nonEmptyDropStacks = 0;
        int totalDroppedItems = 0;
        ItemStack onlyDrop = ItemStack.EMPTY;

        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack dropStack = itemEntity.getItem();

            if (dropStack.isEmpty()) {
                continue;
            }

            nonEmptyDropStacks++;
            totalDroppedItems += dropStack.getCount();
            onlyDrop = dropStack;
        }

        if (nonEmptyDropStacks != 1) {
            return true;
        }

        if (totalDroppedItems != 1) {
            return true;
        }

        return onlyDrop.getItem() != placedBlockItem;
    }

    private static void projectenervate$applyBlockEmcToDrops(
            BigDecimal totalBlockEmc,
            List<ItemEntity> drops
    ) {
        if (totalBlockEmc.signum() <= 0) {
            return;
        }

        BigDecimal totalBaseDropWeight = BigDecimal.ZERO;
        int totalDroppedItems = 0;

        for (ItemEntity itemEntity : drops) {
            ItemStack dropStack = itemEntity.getItem();

            if (dropStack.isEmpty()) {
                continue;
            }

            totalDroppedItems += dropStack.getCount();
            totalBaseDropWeight = totalBaseDropWeight.add(
                    AdaptiveEmcOutputHelper.getBaseStackEmc(dropStack)
            );
        }

        if (totalDroppedItems <= 0) {
            return;
        }

        if (totalBaseDropWeight.signum() <= 0) {
            projectenervate$applyEqualBlockEmcToDrops(totalBlockEmc, drops, totalDroppedItems);
            return;
        }

        for (ItemEntity itemEntity : drops) {
            ItemStack dropStack = itemEntity.getItem();

            if (dropStack.isEmpty()) {
                continue;
            }

            BigDecimal dropBaseStackWeight = AdaptiveEmcOutputHelper.getBaseStackEmc(dropStack);

            if (dropBaseStackWeight.signum() <= 0) {
                ProjectEnervateSourceHelper.clearProjectEnervateData(dropStack);
                continue;
            }

            BigDecimal proposedDropStackEmc = totalBlockEmc
                    .multiply(dropBaseStackWeight)
                    .divide(
                            totalBaseDropWeight,
                            AdaptiveEmcValues.INTERNAL_SCALE,
                            RoundingMode.HALF_UP
                    );

            AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(
                    dropStack,
                    proposedDropStackEmc
            );
        }
    }

    private static void projectenervate$applyUnknownSourceToDrops(List<ItemEntity> drops) {
        for (ItemEntity itemEntity : drops) {
            ItemStack dropStack = itemEntity.getItem();

            if (dropStack.isEmpty()) {
                continue;
            }

            ProjectEnervateSourceHelper.markUnknownSource(dropStack);
        }
    }

    private static void projectenervate$applyEqualBlockEmcToDrops(
            BigDecimal totalBlockEmc,
            List<ItemEntity> drops,
            int totalDroppedItems
    ) {
        BigDecimal emcPerDroppedItem = totalBlockEmc.divide(
                BigDecimal.valueOf(totalDroppedItems),
                AdaptiveEmcValues.INTERNAL_SCALE,
                RoundingMode.HALF_UP
        );

        for (ItemEntity itemEntity : drops) {
            ItemStack dropStack = itemEntity.getItem();

            if (dropStack.isEmpty()) {
                continue;
            }

            BigDecimal proposedDropStackEmc = emcPerDroppedItem.multiply(
                    BigDecimal.valueOf(dropStack.getCount())
            );

            AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(
                    dropStack,
                    proposedDropStackEmc
            );
        }
    }

    private record NaturalDropBudgetKey(
            ResourceKey<Level> dimension,
            long pos
    ) {
    }

    private static final class NaturalDropBudget {
        private BigDecimal remainingEmc;
        private final boolean alwaysApply;
        private final long gameTime;

        private NaturalDropBudget(BigDecimal remainingEmc, boolean alwaysApply, long gameTime) {
            this.remainingEmc = remainingEmc == null ? BigDecimal.ZERO : remainingEmc;
            this.alwaysApply = alwaysApply;
            this.gameTime = gameTime;
        }

        private BigDecimal remainingEmc() {
            return remainingEmc;
        }

        private boolean alwaysApply() {
            return alwaysApply;
        }

        private long gameTime() {
            return gameTime;
        }

        private BigDecimal takeUpTo(BigDecimal requestedEmc) {
            if (requestedEmc == null || requestedEmc.signum() <= 0 || remainingEmc.signum() <= 0) {
                return BigDecimal.ZERO;
            }

            BigDecimal taken = remainingEmc.min(requestedEmc);
            remainingEmc = remainingEmc.subtract(taken).max(BigDecimal.ZERO);
            return taken;
        }
    }

    private record PendingPlacement(
            Item expectedPlacedItem,
            BigDecimal emcPerPlacedBlock,
            boolean alwaysApply,
            long gameTime
    ) {
    }
}
