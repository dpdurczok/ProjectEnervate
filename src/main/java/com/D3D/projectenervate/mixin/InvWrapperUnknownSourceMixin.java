package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InvWrapper.class, remap = false)
public abstract class InvWrapperUnknownSourceMixin {

    @Inject(
            method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD")
    )
    private void projectenervate$unknownSourceBeforeInvWrapperInsert(
            int slot,
            ItemStack stack,
            boolean simulate,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!simulate) {
            ProjectEnervateSourceHelper.enforceUnknownMinimum(stack);
        }
    }
}