package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEMachineEmcHelper;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.block_entities.CondenserBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CondenserBlockEntity.class, remap = false)
public abstract class ProjectECondenserBlockEntityMixin {

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

    @Redirect(
            method = "pushStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/items/ItemHandlerHelper;insertItemStacked(Lnet/neoforged/neoforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack projectenervate$markCondenserOutputVerified(
            IItemHandler inventory,
            ItemStack stack,
            boolean simulate
    ) {
        ProjectEnervateSourceHelper.markVerifiedIfBaseEmcPreservingExisting(stack);
        return ItemHandlerHelper.insertItemStacked(inventory, stack, simulate);
    }
}
