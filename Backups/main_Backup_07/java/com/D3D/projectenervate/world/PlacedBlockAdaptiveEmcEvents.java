package com.D3D.projectenervate.world;

import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import com.D3D.projectenervate.emc.AdaptiveEmcOutputHelper;

public final class PlacedBlockAdaptiveEmcEvents {

    private static final int PENDING_PLACEMENT_TIMEOUT_TICKS = 10;

    private static final Map<UUID, PendingPlacement> PENDING_PLACEMENTS = new HashMap<>();

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

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING_PLACEMENTS.remove(event.getEntity().getUUID());
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

    private record PendingPlacement(
            Item expectedPlacedItem,
            BigDecimal emcPerPlacedBlock,
            boolean alwaysApply,
            long gameTime
    ) {
    }
}
