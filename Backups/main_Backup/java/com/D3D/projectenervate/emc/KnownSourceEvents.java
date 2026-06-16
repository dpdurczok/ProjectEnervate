package com.D3D.projectenervate.emc;

import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class KnownSourceEvents {
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

    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer().isCreative()) {
            ProjectEnervateSourceHelper.markKnownIfBaseEmc(
                    event.getEntity().getItem(),
                    ProjectEnervateSourceHelper.SOURCE_CREATIVE_OR_COMMAND
            );
            return;
        }

        ProjectEnervateSourceHelper.enforceUnknownMinimum(event.getEntity().getItem());
    }
}
