package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEMachineEmcHelper;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.block_entities.CondenserMK2BlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CondenserMK2BlockEntity.class, remap = false)
public abstract class ProjectECondenserMK2BlockEntityMixin {

    @Redirect(
            method = "condense",
            at = @At(
                    value = "INVOKE",
                    target = "Lmoze_intel/projecte/api/proxy/IEMCProxy;getSellValue(Lnet/minecraft/world/item/ItemStack;)J"
            )
    )
    private long projectenervate$getEffectiveCondenserInputValue(IEMCProxy proxy, ItemStack stack) {
        return ProjectEMachineEmcHelper.getEffectiveSingleSellValueAsLong(stack);
    }
}
