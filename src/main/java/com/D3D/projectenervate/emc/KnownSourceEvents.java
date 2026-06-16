package com.D3D.projectenervate.emc;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class KnownSourceEvents {
    private static final int PLAYER_INVENTORY_MARK_INTERVAL_TICKS = 20;

    private KnownSourceEvents() {
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        for (ItemEntity drop : event.getDrops()) {
            ProjectEnervateSourceHelper.markKnownIfBaseEmc(
                    drop.getItem(),
                    ProjectEnervateSourceHelper.SOURCE_BLOCK_DROP
            );
        }
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        for (ItemEntity drop : event.getDrops()) {
            ProjectEnervateSourceHelper.markKnownIfBaseEmc(
                    drop.getItem(),
                    ProjectEnervateSourceHelper.SOURCE_LIVING_DROP
            );
        }
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.getGameTime() % PLAYER_INVENTORY_MARK_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            markPlayerInventoryKnown(player);
        }
    }

    private static void markPlayerInventoryKnown(ServerPlayer player) {
        markListKnown(player.getInventory().items);
        markListKnown(player.getInventory().armor);
        markListKnown(player.getInventory().offhand);
    }

    private static void markListKnown(NonNullList<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ProjectEnervateSourceHelper.markKnownIfBaseEmc(
                    stack,
                    ProjectEnervateSourceHelper.SOURCE_PLAYER_INVENTORY
            );
        }
    }
}