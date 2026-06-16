package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InvWrapper.class, remap = false)
public abstract class InvWrapperUnknownSourceMixin {

    @Shadow
    public abstract ItemStack getStackInSlot(int slot);

    @Unique
    private ItemStack projectenervate$slotBeforeInsert = ItemStack.EMPTY;

    @Unique
    private ItemStack projectenervate$incomingBeforeInsert = ItemStack.EMPTY;

    @Inject(
            method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD")
    )
    private void projectenervate$captureBeforeInvWrapperInsert(
            int slot,
            ItemStack stack,
            boolean simulate,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (simulate || stack.isEmpty()) {
            projectenervate$slotBeforeInsert = ItemStack.EMPTY;
            projectenervate$incomingBeforeInsert = ItemStack.EMPTY;
            return;
        }

        projectenervate$slotBeforeInsert = getStackInSlot(slot).copy();
        projectenervate$incomingBeforeInsert = stack.copy();
    }

    @Inject(
            method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN")
    )
    private void projectenervate$recomputeAfterInvWrapperInsert(
            int slot,
            ItemStack stack,
            boolean simulate,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (simulate || projectenervate$incomingBeforeInsert.isEmpty()) {
            return;
        }

        ItemStack remainder = cir.getReturnValue();
        int remainderCount = remainder.isEmpty() ? 0 : remainder.getCount();
        int insertedCount = projectenervate$incomingBeforeInsert.getCount() - remainderCount;

        if (insertedCount <= 0) {
            return;
        }

        ItemStack insertedTemplate = projectenervate$incomingBeforeInsert.copy();
        insertedTemplate.setCount(insertedCount);

        AdaptiveEmcHelper.applyMergedEconomicState(
                projectenervate$slotBeforeInsert,
                insertedTemplate,
                getStackInSlot(slot),
                insertedCount
        );
    }
}
