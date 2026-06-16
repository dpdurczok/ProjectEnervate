package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervate;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class UnknownSourceEmcSafetyEvents {
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final double WORLD_ITEM_SCAN_RADIUS = 48.0D;
    private static final int MACHINE_SEARCH_RADIUS = 4;

    private UnknownSourceEmcSafetyEvents() {
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.getGameTime() % SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            scanOpenModdedMenuOutputSlots(player);
            scanNearbyWorldItems(level, player);
        }
    }

    private static void scanOpenModdedMenuOutputSlots(ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;

        if (menu == null || menu == player.inventoryMenu) {
            return;
        }

        if (shouldSkipMenu(menu)) {
            return;
        }

        int changed = 0;

        for (Slot slot : menu.slots) {
            if (!slot.hasItem()) {
                continue;
            }

            if (isPlayerInventorySlot(slot)) {
                continue;
            }

            ItemStack stack = slot.getItem();

            if (!isLikelyOutputSlot(slot, stack)) {
                continue;
            }

            if (UnknownSourceEmcSafetyHelper.applyMinimumIfUnknown(stack)) {
                slot.setChanged();
                changed++;
            }
        }

        if (changed > 0) {
            ProjectEnervate.LOGGER.info(
                    "ProjectEnervate unknown-source safety adapted {} output stacks in menu {}",
                    changed,
                    menu.getClass().getName()
            );
        }
    }

    private static void scanNearbyWorldItems(ServerLevel level, ServerPlayer player) {
        AABB scanBox = player.getBoundingBox().inflate(WORLD_ITEM_SCAN_RADIUS);

        Set<ItemEntity> itemEntities = new HashSet<>(level.getEntitiesOfClass(
                ItemEntity.class,
                scanBox,
                entity -> !entity.isRemoved() && !entity.getItem().isEmpty()
        ));

        if (itemEntities.isEmpty()) {
            return;
        }

        int changed = 0;

        for (ItemEntity itemEntity : itemEntities) {
            if (!isNearLikelyModdedMachine(level, itemEntity.blockPosition())) {
                continue;
            }

            ItemStack stack = itemEntity.getItem();

            if (UnknownSourceEmcSafetyHelper.applyMinimumIfUnknown(stack)) {
                itemEntity.setItem(stack);
                changed++;
            }
        }

        if (changed > 0) {
            ProjectEnervate.LOGGER.info(
                    "ProjectEnervate unknown-source safety adapted {} nearby world item stacks",
                    changed
            );
        }
    }

    private static boolean shouldSkipMenu(AbstractContainerMenu menu) {
        String className = menu.getClass().getName();

        return className.startsWith("net.minecraft.")
                || className.startsWith("moze_intel.projecte.");
    }

    private static boolean isPlayerInventorySlot(Slot slot) {
        return slot.container instanceof Inventory;
    }

    private static boolean isLikelyOutputSlot(Slot slot, ItemStack stack) {
        try {
            if (!slot.mayPlace(stack)) {
                return true;
            }
        } catch (Throwable ignored) {
            return false;
        }

        String slotClassName = slot.getClass().getName().toLowerCase();

        return slotClassName.contains("output")
                || slotClassName.contains("result");
    }

    private static boolean isNearLikelyModdedMachine(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-MACHINE_SEARCH_RADIUS, -MACHINE_SEARCH_RADIUS, -MACHINE_SEARCH_RADIUS),
                center.offset(MACHINE_SEARCH_RADIUS, MACHINE_SEARCH_RADIUS, MACHINE_SEARCH_RADIUS)
        )) {
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            if (isLikelyModdedMachineBlock(level, pos, state)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isLikelyModdedMachineBlock(ServerLevel level, BlockPos pos, BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String namespace = id.getNamespace().toLowerCase();
        String path = id.getPath().toLowerCase();

        if (namespace.equals("minecraft")) {
            return false;
        }

        if (isKnownStorageOrTransport(path)) {
            return false;
        }

        if (namespace.equals("create") || namespace.equals("exdeorum")) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity != null) {
            String blockEntityClass = blockEntity.getClass().getName().toLowerCase();

            if (blockEntityClass.contains("machine")
                    || blockEntityClass.contains("processor")
                    || blockEntityClass.contains("crusher")
                    || blockEntityClass.contains("sieve")
                    || blockEntityClass.contains("mill")
                    || blockEntityClass.contains("press")) {
                return true;
            }
        }

        return path.contains("sieve")
                || path.contains("sifter")
                || path.contains("sift")
                || path.contains("mill")
                || path.contains("millstone")
                || path.contains("press")
                || path.contains("crusher")
                || path.contains("crushing")
                || path.contains("grinder")
                || path.contains("grind")
                || path.contains("pulverizer")
                || path.contains("washer")
                || path.contains("washing")
                || path.contains("fan")
                || path.contains("mixer")
                || path.contains("basin")
                || path.contains("depot")
                || path.contains("saw")
                || path.contains("cutter")
                || path.contains("processor")
                || path.contains("machine");
    }

    private static boolean isKnownStorageOrTransport(String path) {
        return path.contains("chest")
                || path.contains("barrel")
                || path.contains("shulker")
                || path.contains("drawer")
                || path.contains("storage")
                || path.contains("vault")
                || path.contains("tank")
                || path.contains("pipe")
                || path.contains("duct")
                || path.contains("cable")
                || path.contains("trash");
    }
}