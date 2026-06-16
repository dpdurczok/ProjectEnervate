package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.api.ProjectEnervateTransmutationAccess;
import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import java.math.BigInteger;
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

        ProjectEnervateTransmutationAccess access =
                (ProjectEnervateTransmutationAccess) transmutationInventory;

        BigInteger freeEmc = access.projectenervate$getFreeStarEmc();

        int itemsToBurn = AdaptiveEmcHelper.getMaxItemsThatFit(freeEmc, slotStack);

        if (itemsToBurn <= 0) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        BigInteger emcToAdd = AdaptiveEmcHelper.getStackSellValue(slotStack, itemsToBurn);

        if (emcToAdd.signum() <= 0) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        ItemStack burnedStack = slotStack.copy();
        burnedStack.setCount(itemsToBurn);

        if (transmutationInventory.isServer()) {
            transmutationInventory.handleKnowledge(burnedStack);
            transmutationInventory.addEmc(emcToAdd);
        }

        slotStack.shrink(itemsToBurn);

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        cir.setReturnValue(burnedStack);
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

        if (slotStack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null) {
            return;
        }

        if (slotStack.getItem() instanceof Tome) {
            return;
        }

        if (!com.D3D.projectenervate.emc.AdaptiveEmcHelper.hasPositiveSellValue(slotStack)) {
            return;
        }

        ProjectEnervateTransmutationAccess access =
                (ProjectEnervateTransmutationAccess) transmutationInventory;

        BigInteger freeEmc = access.projectenervate$getFreeStarEmc();

        int itemsToBurn = com.D3D.projectenervate.emc.AdaptiveEmcHelper.getMaxItemsThatFit(freeEmc, slotStack);

        if (itemsToBurn <= 0) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        BigInteger emcToAdd = com.D3D.projectenervate.emc.AdaptiveEmcHelper.getStackSellValue(slotStack, itemsToBurn);

        if (emcToAdd.signum() <= 0) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        ItemStack burnedStack = slotStack.copy();
        burnedStack.setCount(itemsToBurn);

        if (transmutationInventory.isServer()) {
            transmutationInventory.handleKnowledge(burnedStack);
            transmutationInventory.addEmc(emcToAdd);
        }

        slotStack.shrink(itemsToBurn);

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        cir.setReturnValue(burnedStack);
    }
}