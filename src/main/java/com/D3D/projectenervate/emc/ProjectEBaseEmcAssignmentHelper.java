package com.D3D.projectenervate.emc;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.math.BigDecimal;
import java.math.RoundingMode;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.emc.EMCMappingHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ProjectEBaseEmcAssignmentHelper {
    private ProjectEBaseEmcAssignmentHelper() {
    }

    public static long toWholeEmc(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return 0L;
        }

        BigDecimal rounded = value.setScale(0, RoundingMode.CEILING);

        if (rounded.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            return Long.MAX_VALUE;
        }

        return rounded.longValue();
    }

    public static boolean assignBaseEmc(ServerPlayer player, ItemStack targetStack, long emc) {
        if (player == null || targetStack.isEmpty() || emc <= 0) {
            return false;
        }

        NSSItem targetNss = NSSItem.createItem(targetStack.getItem());
        ItemInfo targetInfo = ItemInfo.fromNSS(targetNss);

        if (targetInfo == null) {
            return false;
        }

        if (CustomEMCParser.currentEntries == null) {
            CustomEMCParser.init(player.registryAccess());
        }

        CustomEMCParser.addToFile(targetNss, emc);
        CustomEMCParser.flush(player.registryAccess());
        updateLiveMap(targetInfo, emc);
        ProjectEnervateSourceHelper.clearProjectEnervateData(targetStack);
        ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(targetStack);
        requestProjectEReload(player);
        return true;
    }

    private static void updateLiveMap(ItemInfo targetInfo, long emc) {
        Object2LongOpenHashMap<ItemInfo> map = new Object2LongOpenHashMap<>();
        map.defaultReturnValue(0L);

        for (ItemInfo mappedItem : EMCMappingHandler.getMappedItems()) {
            long mappedEmc = EMCMappingHandler.getStoredEmcValue(mappedItem);

            if (mappedEmc > 0) {
                map.put(mappedItem, mappedEmc);
            }
        }

        map.put(targetInfo, emc);
        EMCMappingHandler.updateEmcValues(map);
    }

    private static void requestProjectEReload(ServerPlayer player) {
        if (player.server == null) {
            return;
        }

        try {
            CommandSourceStack source = player.createCommandSourceStack()
                    .withPermission(4);
            player.server.getCommands().performPrefixedCommand(source, "projecte reloadEMC");
        } catch (Exception ignored) {
            // The live EMC map was already updated. If ProjectE changes the command name,
            // the persisted custom_emc.json entry will still load on the next normal EMC reload.
        }
    }
}
