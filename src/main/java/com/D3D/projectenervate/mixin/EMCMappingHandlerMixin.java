package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.ProjectEnervateConfig;
import com.D3D.projectenervate.emc.AdaptiveEmcOutputHelper;
import com.D3D.projectenervate.emc.BacktrackedBaseEmcMapper;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.Set;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.emc.EMCMappingHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EMCMappingHandler.class, remap = false)
public abstract class EMCMappingHandlerMixin {

    @Inject(method = "updateEmcValues", at = @At("HEAD"))
    private static void projectenervate$forceCustomAndBacktrackedEmcIntoFinalMap(
            Object2LongMap<ItemInfo> data,
            CallbackInfoReturnable<Integer> cir
    ) {
        AdaptiveEmcOutputHelper.clearBaseEmcCache();
        Set<ItemInfo> customRemovedItems = BacktrackedBaseEmcMapper.collectCustomRemovedItems();

        projectenervate$applyCustomEmc(data);

        if (ProjectEnervateConfig.backtrackMissingBaseEmc()) {
            BacktrackedBaseEmcMapper.apply(data, customRemovedItems);
        }

        projectenervate$applyCustomEmc(data);
    }

    private static void projectenervate$applyCustomEmc(Object2LongMap<ItemInfo> data) {
        if (data == null || CustomEMCParser.currentEntries == null) {
            return;
        }

        for (Object2LongMap.Entry<NSSItem> entry : CustomEMCParser.currentEntries.entries().object2LongEntrySet()) {
            NSSItem nssItem = entry.getKey();
            long emc = entry.getLongValue();

            ItemInfo itemInfo = ItemInfo.fromNSS(nssItem);

            if (itemInfo == null) {
                continue;
            }

            if (emc <= 0) {
                data.removeLong(itemInfo);
            } else {
                data.put(itemInfo, emc);
            }
        }
    }
}
