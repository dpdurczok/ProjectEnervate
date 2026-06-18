package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.api.ProjectEnervateTransmutationAccess;
import com.D3D.projectenervate.emc.TransmutationBurnHelper;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.items.Tome;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TransmutationContainer.class, remap = false)
public abstract class TransmutationContainerMixin {

    @Shadow
    @Final
    public TransmutationInventory transmutationInventory;

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void projectenervate$adaptiveShiftClickBurn(
            @NotNull Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        if (slotIndex <= 26) {
            return;
        }

        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }

        Slot slot = menu.slots.get(slotIndex);

        if (!slot.hasItem()) {
            return;
        }

        ItemStack slotStack = slot.getItem();

        if (shouldLetProjectEStoreEmcHolder(slotStack)) {
            return;
        }

        if (slotStack.getItem() instanceof Tome) {
            return;
        }

        if (!TransmutationBurnHelper.shouldHandleAsBurnable(slotStack)) {
            return;
        }

        projectenervate$burnShiftClickedStack(slot, slotStack, cir);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void projectenervate$mouseTweaksSafeShiftClickBurn(
            @NotNull Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (cir.isCancelled()) {
            return;
        }

        if (!com.D3D.projectenervate.compat.ProjectEnervateCompat.isMouseTweaksLoaded()) {
            return;
        }

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        if (slotIndex <= 26) {
            return;
        }

        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }

        Slot slot = menu.slots.get(slotIndex);

        if (!slot.hasItem()) {
            return;
        }

        ItemStack slotStack = slot.getItem();

        if (shouldLetProjectEStoreEmcHolder(slotStack)) {
            return;
        }

        if (!TransmutationBurnHelper.shouldHandleAsBurnable(slotStack)) {
            return;
        }

        projectenervate$burnShiftClickedStack(slot, slotStack, cir);
    }

    @Unique
    private void projectenervate$burnShiftClickedStack(
            Slot slot,
            ItemStack slotStack,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        ItemStack burnedStack = slotStack.copy();
        TransmutationBurnHelper.BurnResult result = TransmutationBurnHelper.burnFromStackWithResult(
                transmutationInventory,
                slotStack,
                slotStack.getCount()
        );

        if (!result.changed()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (result.itemsConsumed() > 0) {
            burnedStack.setCount(result.itemsConsumed());
            cir.setReturnValue(burnedStack);
        } else {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Unique
    private boolean shouldLetProjectEStoreEmcHolder(ItemStack stack) {
        return TransmutationBurnHelper.isEmcHolder(stack)
                && ((ProjectEnervateTransmutationAccess) transmutationInventory)
                        .projectenervate$canStoreEmcHolder(stack);
    }
}
