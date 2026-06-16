package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.ProjectEnervateConfig;
import com.D3D.projectenervate.emc.ProjectEMachineEmcHelper;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.block_entities.CollectorMK1BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CollectorMK1BlockEntity.class, remap = false)
public abstract class ProjectECollectorBlockEntityMixin {

    @Inject(method = "updateEmc", at = @At("HEAD"), cancellable = true)
    private void projectenervate$disableCollectorGeneration(Level level, BlockPos pos, CallbackInfo ci) {
        if (ProjectEnervateConfig.disableCollectors()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "updateEmc",
            at = @At(
                    value = "INVOKE",
                    target = "Lmoze_intel/projecte/api/proxy/IEMCProxy;getValue(Lnet/minecraft/world/item/ItemStack;)J",
                    ordinal = 1
            )
    )
    private long projectenervate$getEffectiveFuelInputValue(IEMCProxy proxy, ItemStack stack) {
        return ProjectEMachineEmcHelper.getEffectiveSingleSellValueAsLong(stack);
    }

    @Inject(method = "updateEmc", at = @At("RETURN"))
    private void projectenervate$validateCollectorUpgradeOutput(Level level, BlockPos pos, CallbackInfo ci) {
        CollectorMK1BlockEntity collector = (CollectorMK1BlockEntity) (Object) this;
        IItemHandler aux = collector.getAux();
        ItemStack upgraded = aux.getStackInSlot(CollectorMK1BlockEntity.UPGRADE_SLOT);

        if (!upgraded.isEmpty()) {
            ProjectEnervateSourceHelper.markVerifiedIfBaseEmcPreservingExisting(upgraded);
        }
    }
}
