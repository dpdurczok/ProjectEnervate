package com.D3D.projectenervate.machine;

import com.D3D.projectenervate.ProjectEnervate;
import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import com.D3D.projectenervate.emc.AdaptiveEmcOutputHelper;
import com.D3D.projectenervate.emc.AdaptiveEmcValues;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class MachineConversionTracker {
    private static final int MAX_DIRTY_POSITIONS_PER_LEVEL_TICK = 512;
    private static final int PENDING_BUDGET_LIFETIME_TICKS = 80;
    private static final double PENDING_ITEM_ENTITY_RADIUS_SQR = 25.0D;

    private static final Map<ResourceKey<Level>, Map<BlockPos, MachineInventorySnapshot>> LAST_SNAPSHOTS = new HashMap<>();
    private static final Map<ResourceKey<Level>, LinkedHashSet<BlockPos>> DIRTY_POSITIONS = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<PendingConversionBudget>> PENDING_BUDGETS = new HashMap<>();

    private MachineConversionTracker() {
    }

    public static void markDirty(BlockEntity blockEntity) {
        Level level = blockEntity.getLevel();

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (shouldIgnoreBlockEntity(blockEntity)) {
            return;
        }

        DIRTY_POSITIONS
                .computeIfAbsent(serverLevel.dimension(), ignored -> new LinkedHashSet<>())
                .add(blockEntity.getBlockPos().immutable());
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        expireOldPendingBudgets(level);
        processDirtyPositions(level);
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();

        if (stack.isEmpty()) {
            return;
        }

        PendingConversionBudget budget = findNearestPendingBudget(level, itemEntity.position());

        if (budget == null || budget.isEmpty()) {
            return;
        }

        ItemStack adapted = stack.copy();
        AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(adapted, budget.budget());
        itemEntity.setItem(adapted);

        removePendingBudget(level, budget);

        ProjectEnervate.LOGGER.debug(
                "ProjectEnervate applied pending machine EMC budget {} to spawned item {} near {}",
                budget.budget(),
                adapted.getDisplayName().getString(),
                budget.pos()
        );
    }

    private static void processDirtyPositions(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        LinkedHashSet<BlockPos> dirty = DIRTY_POSITIONS.get(dimension);

        if (dirty == null || dirty.isEmpty()) {
            return;
        }

        Map<BlockPos, MachineInventorySnapshot> snapshots =
                LAST_SNAPSHOTS.computeIfAbsent(dimension, ignored -> new HashMap<>());

        int processed = 0;
        Iterator<BlockPos> iterator = dirty.iterator();

        while (iterator.hasNext() && processed < MAX_DIRTY_POSITIONS_PER_LEVEL_TICK) {
            BlockPos pos = iterator.next();
            iterator.remove();
            processed++;

            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity == null || blockEntity.isRemoved() || shouldIgnoreBlockEntity(blockEntity)) {
                snapshots.remove(pos);
                continue;
            }

            MachineInventoryAccess access = MachineInventoryAccess.create(blockEntity);

            if (access == null) {
                snapshots.remove(pos);
                continue;
            }

            MachineInventorySnapshot before = snapshots.get(pos);
            MachineInventorySnapshot after = MachineInventorySnapshot.capture(access);

            if (before != null) {
                processInventoryDelta(level, pos, access, before, after);
                after = MachineInventorySnapshot.capture(access);
            }

            snapshots.put(pos.immutable(), after);
        }
    }

    private static void processInventoryDelta(
            ServerLevel level,
            BlockPos pos,
            MachineInventoryAccess access,
            MachineInventorySnapshot before,
            MachineInventorySnapshot after
    ) {
        List<ConsumedStack> consumed = new ArrayList<>();
        List<GeneratedSlot> generated = new ArrayList<>();

        int slots = Math.max(before.size(), after.size());

        for (int slot = 0; slot < slots; slot++) {
            ItemStack beforeStack = before.get(slot);
            ItemStack afterStack = after.get(slot);

            if (beforeStack.isEmpty() && afterStack.isEmpty()) {
                continue;
            }

            if (!beforeStack.isEmpty()
                    && !afterStack.isEmpty()
                    && AdaptiveEmcHelper.canMergeIgnoringAdaptiveEmc(beforeStack, afterStack)) {
                int delta = afterStack.getCount() - beforeStack.getCount();

                if (delta < 0) {
                    consumed.add(new ConsumedStack(beforeStack, -delta));
                } else if (delta > 0) {
                    generated.add(new GeneratedSlot(slot, beforeStack, delta));
                }

                continue;
            }

            if (!beforeStack.isEmpty()) {
                consumed.add(new ConsumedStack(beforeStack, beforeStack.getCount()));
            }

            if (!afterStack.isEmpty()) {
                generated.add(new GeneratedSlot(slot, ItemStack.EMPTY, afterStack.getCount()));
            }
        }

        BigDecimal consumedBudget = calculateConsumedBudget(consumed);

        if (generated.isEmpty()) {
            if (consumedBudget.signum() > 0) {
                addPendingBudget(level, pos, consumedBudget);
            }

            return;
        }

        BigDecimal pendingBudget = consumePendingBudgetAt(level, pos);
        BigDecimal totalBudget = consumedBudget.add(pendingBudget);

        if (totalBudget.signum() <= 0) {
            return;
        }

        applyBudgetToGeneratedSlots(access, generated, totalBudget);

        ProjectEnervate.LOGGER.debug(
                "ProjectEnervate tracked machine conversion at {} with EMC budget {} and {} generated slot changes",
                pos,
                totalBudget,
                generated.size()
        );
    }

    private static BigDecimal calculateConsumedBudget(List<ConsumedStack> consumed) {
        BigDecimal budget = BigDecimal.ZERO;

        for (ConsumedStack consumedStack : consumed) {
            ItemStack stack = consumedStack.stack();

            if (stack.isEmpty() || consumedStack.count() <= 0) {
                continue;
            }

            BigDecimal single = AdaptiveEmcOutputHelper.getEffectiveSingleEmc(stack);

            if (single.signum() <= 0) {
                continue;
            }

            budget = budget.add(single.multiply(BigDecimal.valueOf(consumedStack.count())));
        }

        return budget;
    }

    private static void applyBudgetToGeneratedSlots(
            MachineInventoryAccess access,
            List<GeneratedSlot> generated,
            BigDecimal totalBudget
    ) {
        BigDecimal totalBaseWeight = BigDecimal.ZERO;

        for (GeneratedSlot generatedSlot : generated) {
            ItemStack current = access.getStackInSlotCopy(generatedSlot.slot());

            if (current.isEmpty()) {
                continue;
            }

            int generatedCount = Math.min(generatedSlot.generatedCount(), current.getCount());

            if (generatedCount <= 0) {
                continue;
            }

            BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(current);

            if (baseSingle.signum() > 0) {
                totalBaseWeight = totalBaseWeight.add(baseSingle.multiply(BigDecimal.valueOf(generatedCount)));
            }
        }

        int usableGeneratedSlots = countUsableGeneratedSlots(access, generated);

        if (usableGeneratedSlots <= 0) {
            return;
        }

        for (GeneratedSlot generatedSlot : generated) {
            ItemStack current = access.getStackInSlotCopy(generatedSlot.slot());

            if (current.isEmpty()) {
                continue;
            }

            int generatedCount = Math.min(generatedSlot.generatedCount(), current.getCount());

            if (generatedCount <= 0) {
                continue;
            }

            BigDecimal allocatedBudget;

            if (totalBaseWeight.signum() > 0) {
                BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(current);
                BigDecimal weight = baseSingle.multiply(BigDecimal.valueOf(generatedCount));

                if (weight.signum() <= 0) {
                    allocatedBudget = BigDecimal.ZERO;
                } else {
                    allocatedBudget = totalBudget
                            .multiply(weight)
                            .divide(totalBaseWeight, AdaptiveEmcValues.INTERNAL_SCALE, RoundingMode.HALF_UP);
                }
            } else {
                allocatedBudget = totalBudget.divide(
                        BigDecimal.valueOf(usableGeneratedSlots),
                        AdaptiveEmcValues.INTERNAL_SCALE,
                        RoundingMode.HALF_UP
                );
            }

            if (allocatedBudget.signum() <= 0) {
                continue;
            }

            ItemStack beforeSlot = generatedSlot.beforeSlotStack();

            if (!beforeSlot.isEmpty() && !AdaptiveEmcHelper.canMergeIgnoringAdaptiveEmc(beforeSlot, current)) {
                beforeSlot = ItemStack.EMPTY;
            }

            AdaptiveEmcOutputHelper.mergeGeneratedIntoResultStack(
                    beforeSlot,
                    current,
                    generatedCount,
                    allocatedBudget
            );

            access.setStackInSlot(generatedSlot.slot(), current);
        }
    }

    private static int countUsableGeneratedSlots(MachineInventoryAccess access, List<GeneratedSlot> generated) {
        int count = 0;

        for (GeneratedSlot generatedSlot : generated) {
            ItemStack current = access.getStackInSlotCopy(generatedSlot.slot());

            if (!current.isEmpty() && generatedSlot.generatedCount() > 0) {
                count++;
            }
        }

        return count;
    }

    private static void addPendingBudget(ServerLevel level, BlockPos pos, BigDecimal budget) {
        if (budget.signum() <= 0) {
            return;
        }

        long expiresAt = level.getGameTime() + PENDING_BUDGET_LIFETIME_TICKS;

        PENDING_BUDGETS
                .computeIfAbsent(level.dimension(), ignored -> new ArrayList<>())
                .add(new PendingConversionBudget(pos, budget, expiresAt));

        ProjectEnervate.LOGGER.debug(
                "ProjectEnervate stored pending machine EMC budget {} at {}",
                budget,
                pos
        );
    }

    private static BigDecimal consumePendingBudgetAt(ServerLevel level, BlockPos pos) {
        List<PendingConversionBudget> list = PENDING_BUDGETS.get(level.dimension());

        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        Iterator<PendingConversionBudget> iterator = list.iterator();

        while (iterator.hasNext()) {
            PendingConversionBudget budget = iterator.next();

            if (budget.isExpired(level.getGameTime()) || budget.isEmpty()) {
                iterator.remove();
                continue;
            }

            if (budget.pos().equals(pos)) {
                total = total.add(budget.budget());
                iterator.remove();
            }
        }

        return total;
    }

    private static PendingConversionBudget findNearestPendingBudget(ServerLevel level, net.minecraft.world.phys.Vec3 vec) {
        List<PendingConversionBudget> list = PENDING_BUDGETS.get(level.dimension());

        if (list == null || list.isEmpty()) {
            return null;
        }

        PendingConversionBudget nearest = null;
        double nearestDistance = PENDING_ITEM_ENTITY_RADIUS_SQR;
        long gameTime = level.getGameTime();

        Iterator<PendingConversionBudget> iterator = list.iterator();

        while (iterator.hasNext()) {
            PendingConversionBudget budget = iterator.next();

            if (budget.isExpired(gameTime) || budget.isEmpty()) {
                iterator.remove();
                continue;
            }

            double distance = budget.distanceToSqr(vec);

            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = budget;
            }
        }

        return nearest;
    }

    private static void removePendingBudget(ServerLevel level, PendingConversionBudget budget) {
        List<PendingConversionBudget> list = PENDING_BUDGETS.get(level.dimension());

        if (list != null) {
            list.remove(budget);
        }
    }

    private static void expireOldPendingBudgets(ServerLevel level) {
        List<PendingConversionBudget> list = PENDING_BUDGETS.get(level.dimension());

        if (list == null || list.isEmpty()) {
            return;
        }

        long gameTime = level.getGameTime();
        list.removeIf(budget -> budget.isExpired(gameTime) || budget.isEmpty());
    }

    private static boolean shouldIgnoreBlockEntity(BlockEntity blockEntity) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        String path = blockId.getPath();

        return path.contains("chest")
                || path.contains("barrel")
                || path.contains("shulker")
                || path.contains("hopper")
                || path.contains("dropper")
                || path.contains("dispenser")
                || path.contains("drawer")
                || path.contains("crate")
                || path.contains("storage")
                || path.contains("vault")
                || path.contains("tank")
                || path.contains("trash");
    }

    private record ConsumedStack(ItemStack stack, int count) {
    }

    private record GeneratedSlot(int slot, ItemStack beforeSlotStack, int generatedCount) {
    }
}