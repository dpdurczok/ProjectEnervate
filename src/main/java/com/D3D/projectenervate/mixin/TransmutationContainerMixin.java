package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.api.ProjectEnervateTransmutationAccess;
import java.math.BigInteger;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
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

    /**
     * Allows partial shift-click burning.
     *
     * Example:
     * Stack has 64 diamonds.
     * Klein Stars only have space for 10 diamonds worth of EMC.
     * Result: 10 diamonds are burned, 54 diamonds remain in the inventory slot.
     */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void projectenervate$partialShiftClickBurn(
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

        // Let ProjectE handle shift-clicking Klein Stars into the left input slots.
        if (slotStack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null) {
            return;
        }

        // Keep ProjectE's Tome behavior unchanged.
        if (slotStack.getItem() instanceof Tome) {
            return;
        }

        long singleItemEmc = IEMCProxy.INSTANCE.getSellValue(slotStack);

        if (singleItemEmc <= 0) {
            return;
        }

        ProjectEnervateTransmutationAccess access =
                (ProjectEnervateTransmutationAccess) transmutationInventory;

        BigInteger freeEmc = access.projectenervate$getFreeStarEmc();
        BigInteger singleItemEmcBig = BigInteger.valueOf(singleItemEmc);

        int maxItemsThatFit = freeEmc.divide(singleItemEmcBig).intValue();

        if (maxItemsThatFit <= 0) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        int itemsToBurn = Math.min(slotStack.getCount(), maxItemsThatFit);

        // If the whole stack fits, let ProjectE's original code handle it.
        if (itemsToBurn >= slotStack.getCount()) {
            return;
        }

        ItemStack burnedStack = slotStack.copy();
        burnedStack.setCount(itemsToBurn);

        BigInteger emcToAdd = singleItemEmcBig.multiply(BigInteger.valueOf(itemsToBurn));

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