package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervateConfig;
import com.D3D.projectenervate.api.ProjectEnervateTransmutationAccess;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.items.Tome;
import net.minecraft.world.item.ItemStack;

public final class TransmutationBurnHelper {
    public static final String MESSAGE_ADD_KLEIN_STARS = "Add klein stars";
    public static final String MESSAGE_STORAGE_FULL = "Storage Full";

    public record BurnResult(int itemsConsumed, boolean changed) {
        public static final BurnResult NONE = new BurnResult(0, false);
    }

    private TransmutationBurnHelper() {
    }

    public static boolean shouldHandleAsBurnable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof Tome) {
            return false;
        }

        IItemEmcHolder emcHolder = getEmcHolder(stack);

        if (emcHolder != null) {
            return emcHolder.getStoredEmc(stack) > 0
                    || getSingleBurnValueDecimal(stack).signum() > 0;
        }

        Optional<BigDecimal> adaptiveValue = AdaptiveEmcValues.get(stack);

        if (adaptiveValue.isPresent() && adaptiveValue.get().signum() <= 0) {
            return ProjectEnervateSourceHelper.hasBaseEmc(stack);
        }

        if (!ProjectEnervateSourceHelper.hasSourceMarker(stack)
                && adaptiveValue.isEmpty()) {
            return ProjectEnervateSourceHelper.hasBaseEmc(stack);
        }

        return AdaptiveEmcHelper.hasPositiveSellValue(stack);
    }

    public static int getMaxBurnableItems(BigInteger freeEmc, ItemStack stack, int requestedCount) {
        if (stack.isEmpty() || requestedCount <= 0) {
            return 0;
        }

        int maxCount = Math.min(stack.getCount(), requestedCount);

        IItemEmcHolder emcHolder = getEmcHolder(stack);

        if (emcHolder != null) {
            if (freeEmc.signum() <= 0) {
                return 0;
            }

            if (emcHolder.getStoredEmc(stack) > 0) {
                return 1;
            }

            BigInteger starValue = getStackBurnValue(stack, 1);

            return starValue.signum() > 0 && freeEmc.compareTo(starValue) >= 0 ? 1 : 0;
        }

        if (isZeroValueBurn(stack)) {
            return maxCount;
        }

        return AdaptiveEmcHelper.getMaxItemsThatFit(
                freeEmc,
                getSingleBurnValueDecimal(stack),
                maxCount
        );
    }

    public static BigInteger getStackBurnValue(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0 || isZeroValueBurn(stack)) {
            return BigInteger.ZERO;
        }

        return AdaptiveEmcHelper.getStackSellValue(getSingleBurnValueDecimal(stack), count);
    }

    public static int burnFromStack(
            TransmutationInventory inventory,
            ItemStack sourceStack,
            int requestedCount
    ) {
        return burnFromStackWithResult(inventory, sourceStack, requestedCount).itemsConsumed();
    }

    public static BurnResult burnFromStackWithResult(
            TransmutationInventory inventory,
            ItemStack sourceStack,
            int requestedCount
    ) {
        if (inventory == null || sourceStack.isEmpty() || requestedCount <= 0) {
            return BurnResult.NONE;
        }
        ProjectEnervateSourceHelper.enforceUnknownMinimum(sourceStack);
        if (!shouldHandleAsBurnable(sourceStack)) {
            return BurnResult.NONE;
        }

        if (isEmcHolder(sourceStack)) {
            return burnEmcHolderFromStack(inventory, sourceStack);
        }

        boolean zeroValueSource = isZeroValueBurn(sourceStack);
        ProjectEnervateTransmutationAccess access =
                (ProjectEnervateTransmutationAccess) inventory;

        BigInteger freeEmc = access.projectenervate$getFreeStarEmc();

        if (!zeroValueSource && freeEmc.signum() <= 0) {
            showStorageBlockedMessage(access);
            return BurnResult.NONE;
        }

        int itemsThatFit = getMaxBurnableItems(freeEmc, sourceStack, requestedCount);

        if (itemsThatFit <= 0) {
            if (!zeroValueSource) {
                showStorageBlockedMessage(access);
            }
            return BurnResult.NONE;
        }

        int itemsToBurn = Math.min(itemsThatFit, requestedCount);

        ItemStack burnedStack = sourceStack.copy();
        burnedStack.setCount(itemsToBurn);

        BigInteger emcToAdd = getStackBurnValue(burnedStack, itemsToBurn);

        if (!zeroValueSource && emcToAdd.signum() <= 0) {
            return BurnResult.NONE;
        }

        if (inventory.isServer()) {
            ItemStack knowledgeStack = zeroValueSource
                    ? AdaptiveEmcValues.copyWithoutAdaptiveEmc(burnedStack)
                    : burnedStack;

            inventory.handleKnowledge(knowledgeStack);
            if (emcToAdd.signum() > 0) {
                inventory.addEmc(emcToAdd);
            }
        }

        sourceStack.shrink(itemsToBurn);

        return new BurnResult(itemsToBurn, true);
    }

    private static BurnResult burnEmcHolderFromStack(
            TransmutationInventory inventory,
            ItemStack sourceStack
    ) {
        IItemEmcHolder emcHolder = getEmcHolder(sourceStack);

        if (emcHolder == null) {
            return BurnResult.NONE;
        }

        ProjectEnervateTransmutationAccess access =
                (ProjectEnervateTransmutationAccess) inventory;
        boolean changed = false;

        BigInteger freeEmc = access.projectenervate$getFreeStarEmc();
        long storedEmc = emcHolder.getStoredEmc(sourceStack);

        if (inventory.isServer() && storedEmc > 0 && freeEmc.signum() > 0) {
            BigInteger transferEmc = BigInteger.valueOf(storedEmc).min(freeEmc);
            long extracted = emcHolder.extractEmc(sourceStack, transferEmc.longValue(), EmcAction.EXECUTE);

            if (extracted > 0) {
                inventory.addEmc(BigInteger.valueOf(extracted));
                changed = true;
            }
        }

        freeEmc = access.projectenervate$getFreeStarEmc();
        BigInteger starValue = getStackBurnValue(sourceStack, 1);

        if (starValue.signum() <= 0 || freeEmc.compareTo(starValue) < 0) {
            if (storedEmc > 0 || starValue.signum() > 0) {
                showStorageBlockedMessage(access);
            }
            return new BurnResult(0, changed);
        }

        if (inventory.isServer()) {
            inventory.handleKnowledge(sourceStack);
            inventory.addEmc(starValue);
        }

        sourceStack.shrink(1);

        return new BurnResult(1, true);
    }

    private static void showStorageBlockedMessage(ProjectEnervateTransmutationAccess access) {
        if (!ProjectEnervateConfig.capMaxEmcToKleinStars()) {
            return;
        }

        if (!access.projectenervate$hasAnyEmcHolder()) {
            access.projectenervate$showStorageMessage(MESSAGE_ADD_KLEIN_STARS);
            return;
        }

        access.projectenervate$showStorageMessage(MESSAGE_STORAGE_FULL);
    }

    private static boolean isZeroValueBurn(ItemStack stack) {
        if (isEmcHolder(stack)) {
            return false;
        }

        if (ProjectEnervateSourceHelper.isZero(stack)) {
            return true;
        }

        return ProjectEnervateConfig.voidUnknownSources()
                && !ProjectEnervateSourceHelper.hasSourceMarker(stack)
                && ProjectEnervateSourceHelper.hasBaseEmc(stack);
    }

    private static BigDecimal getSingleBurnValueDecimal(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (isEmcHolder(stack)) {
            return getEmcHolderBaseSingleEmc(stack);
        }

        return AdaptiveEmcHelper.getSingleSellValueDecimal(stack);
    }

    private static BigDecimal getEmcHolderBaseSingleEmc(ItemStack stack) {
        ItemStack singleStack = AdaptiveEmcValues.copyWithoutAdaptiveEmc(stack);
        singleStack.setCount(1);

        IItemEmcHolder emcHolder = getEmcHolder(singleStack);

        if (emcHolder == null) {
            return BigDecimal.ZERO;
        }

        long totalSellValue = IEMCProxy.INSTANCE.getSellValue(singleStack);
        long storedEmc = emcHolder.getStoredEmc(singleStack);
        long itemValue = totalSellValue - storedEmc;

        return itemValue > 0 ? BigDecimal.valueOf(itemValue) : BigDecimal.ZERO;
    }

    public static boolean isEmcHolder(ItemStack stack) {
        return getEmcHolder(stack) != null;
    }

    private static IItemEmcHolder getEmcHolder(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        return stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
    }
}
