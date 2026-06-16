package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow
    @Final
    public NonNullList<ItemStack> items;

    @Shadow
    @Final
    public Player player;

    @Unique
    private boolean projectenervate$adaptiveInventoryMerged;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void projectenervate$mergeAdaptiveStacksBeforeAdd(
            ItemStack incoming,
            CallbackInfoReturnable<Boolean> cir
    ) {
        projectenervate$adaptiveInventoryMerged = false;

        if (incoming.isEmpty()) {
            return;
        }

        projectenervate$prepareIncomingStack(incoming);
        projectenervate$adaptiveInventoryMerged = AdaptiveEmcHelper.mergeIntoInventoryList(items, incoming);

        if (incoming.isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void projectenervate$forceSuccessfulAddReturnIfAdaptiveMerged(
            ItemStack incoming,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (projectenervate$adaptiveInventoryMerged) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void projectenervate$mergeAdaptiveStacksBeforeIndexedAdd(
            int selectedSlot,
            ItemStack incoming,
            CallbackInfoReturnable<Boolean> cir
    ) {
        projectenervate$adaptiveInventoryMerged = false;

        if (incoming.isEmpty()) {
            return;
        }

        projectenervate$prepareIncomingStack(incoming);
        projectenervate$adaptiveInventoryMerged = AdaptiveEmcHelper.mergeIntoInventoryList(items, incoming);

        if (incoming.isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void projectenervate$forceSuccessfulIndexedAddReturnIfAdaptiveMerged(
            int selectedSlot,
            ItemStack incoming,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (projectenervate$adaptiveInventoryMerged) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void projectenervate$mergeAdaptiveStacksBeforePlaceBack(
            ItemStack incoming,
            boolean sendPacket,
            CallbackInfo ci
    ) {
        if (incoming.isEmpty()) {
            return;
        }

        projectenervate$prepareIncomingStack(incoming);
        AdaptiveEmcHelper.mergeIntoInventoryList(items, incoming);

        if (incoming.isEmpty()) {
            ci.cancel();
        }
    }

    @Unique
    private void projectenervate$prepareIncomingStack(ItemStack incoming) {
        if (player != null && player.isCreative()) {
            ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(incoming);
            return;
        }

        ProjectEnervateSourceHelper.enforceUnknownMinimum(incoming);
    }
}
