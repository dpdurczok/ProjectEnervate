package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import com.D3D.projectenervate.emc.TransmutationBurnHelper;
import moze_intel.projecte.api.capabilities.PECapabilities;
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

        if (slotStack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null) {
            return;
        }

        if (slotStack.getItem() instanceof Tome) {
            return;
        }

        if (!AdaptiveEmcHelper.hasAdaptiveValue(slotStack)) {
            return;
        }

        if (!AdaptiveEmcHelper.hasPositiveSellValue(slotStack)) {
            cir.setReturnValue(ItemStack.EMPTY);
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
        int burned = TransmutationBurnHelper.burnFromStack(
                transmutationInventory,
                slotStack,
                slotStack.getCount()
        );

        if (burned <= 0) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        burnedStack.setCount(burned);

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        cir.setReturnValue(burnedStack);
    }
}
