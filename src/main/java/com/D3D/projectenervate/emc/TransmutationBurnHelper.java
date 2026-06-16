package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.api.ProjectEnervateTransmutationAccess;
import java.math.BigInteger;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.items.Tome;
import net.minecraft.world.item.ItemStack;

public final class TransmutationBurnHelper {

    private TransmutationBurnHelper() {
    }

    public static boolean shouldHandleAsBurnable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null) {
            return false;
        }

        if (stack.getItem() instanceof Tome) {
            return false;
        }

        return AdaptiveEmcHelper.hasPositiveSellValue(stack);
    }

    public static int burnFromStack(
            TransmutationInventory inventory,
            ItemStack sourceStack,
            int requestedCount
    ) {
        if (inventory == null || sourceStack.isEmpty() || requestedCount <= 0) {
            return 0;
        }
        ProjectEnervateSourceHelper.enforceUnknownMinimum(sourceStack);

        ProjectEnervateSourceHelper.enforceUnknownMinimum(sourceStack);
        if (!shouldHandleAsBurnable(sourceStack)) {
            return 0;
        }

        ProjectEnervateTransmutationAccess access =
                (ProjectEnervateTransmutationAccess) inventory;

        BigInteger freeEmc = access.projectenervate$getFreeStarEmc();

        if (freeEmc.signum() <= 0) {
            return 0;
        }

        ItemStack limitedStack = sourceStack.copy();
        limitedStack.setCount(Math.min(sourceStack.getCount(), requestedCount));

        int itemsThatFit = AdaptiveEmcHelper.getMaxItemsThatFit(freeEmc, limitedStack);

        if (itemsThatFit <= 0) {
            return 0;
        }

        int itemsToBurn = Math.min(itemsThatFit, requestedCount);

        ItemStack burnedStack = sourceStack.copy();
        burnedStack.setCount(itemsToBurn);

        BigInteger emcToAdd = AdaptiveEmcHelper.getStackSellValue(burnedStack, itemsToBurn);

        if (emcToAdd.signum() <= 0) {
            return 0;
        }

        if (inventory.isServer()) {
            inventory.handleKnowledge(burnedStack);
            inventory.addEmc(emcToAdd);
        }

        sourceStack.shrink(itemsToBurn);

        return itemsToBurn;
    }
}