package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import com.D3D.projectenervate.emc.TransmutationBurnHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Unique
    private static final int PROJECTENERVATE_CONSUME_SLOT = 9;

    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Shadow
    public abstract ItemStack getCarried();

    @Shadow
    public abstract void setCarried(ItemStack stack);

    @Shadow
    public abstract void broadcastChanges();

    @Unique
    private boolean projectenervate$adaptiveMenuMerged;

    @Inject(
            method = "clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void projectenervate$partialBurnOnConsumeSlotClick(
            int slotIndex,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        if (!((Object) this instanceof TransmutationContainer menu)) {
            return;
        }

        if (slotIndex != PROJECTENERVATE_CONSUME_SLOT) {
            return;
        }

        if (clickType != ClickType.PICKUP) {
            return;
        }

        if (button != 0 && button != 1) {
            return;
        }

        ItemStack carried = getCarried();

        if (carried.isEmpty()) {
            return;
        }

        if (!TransmutationBurnHelper.shouldHandleAsBurnable(carried)) {
            return;
        }

        int requestedCount = button == 0 ? carried.getCount() : 1;

        int burned = TransmutationBurnHelper.burnFromStack(
                menu.transmutationInventory,
                carried,
                requestedCount
        );

        if (burned > 0) {
            setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            broadcastChanges();
        }

        ci.cancel();
    }

    @Inject(
            method = "clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void projectenervate$mergeAdaptiveStacksOnMouseClick(
            int slotIndex,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        if (clickType != ClickType.PICKUP) {
            return;
        }

        if (button != 0 && button != 1) {
            return;
        }

        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return;
        }

        Slot slot = slots.get(slotIndex);

        if (!slot.hasItem()) {
            return;
        }

        ItemStack carried = getCarried();

        if (carried.isEmpty()) {
            return;
        }

        int maxMove = button == 0 ? carried.getCount() : 1;

        int moved = AdaptiveEmcHelper.mergeIntoSlot(slot, carried, maxMove);

        if (moved <= 0) {
            return;
        }

        setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        slot.setChanged();
        broadcastChanges();

        ci.cancel();
    }

    @Inject(
            method = "moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void projectenervate$mergeAdaptiveStacksBeforeMoveItemStackTo(
            ItemStack incoming,
            int startIndex,
            int endIndex,
            boolean reverseDirection,
            CallbackInfoReturnable<Boolean> cir
    ) {
        projectenervate$adaptiveMenuMerged = false;

        if (incoming.isEmpty()) {
            return;
        }

        projectenervate$adaptiveMenuMerged = AdaptiveEmcHelper.mergeIntoMenuSlots(
                slots,
                incoming,
                startIndex,
                endIndex,
                reverseDirection
        );

        if (incoming.isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void projectenervate$forceSuccessfulMoveReturnIfAdaptiveMerged(
            ItemStack incoming,
            int startIndex,
            int endIndex,
            boolean reverseDirection,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (projectenervate$adaptiveMenuMerged) {
            cir.setReturnValue(true);
        }
    }
}