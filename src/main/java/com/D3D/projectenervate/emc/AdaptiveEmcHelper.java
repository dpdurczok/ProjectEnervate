package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervateConfig;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AdaptiveEmcHelper {
    private static final BigDecimal INTEGER_ROUNDING_TOLERANCE = new BigDecimal("0.000001");

    private AdaptiveEmcHelper() {
    }

    public static boolean hasAdaptiveValue(ItemStack stack) {
        return AdaptiveEmcValues.has(stack);
    }

    public static BigDecimal getSingleSellValueDecimal(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (ProjectEnervateSourceHelper.isProtectedProjectEItem(stack)) {
            return AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);
        }

        if (ProjectEnervateSourceHelper.isZero(stack)) {
            return BigDecimal.ZERO;
        }

        if (!ProjectEnervateConfig.adaptiveEmc()) {
            return AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);
        }

        Optional<BigDecimal> adaptiveValue = AdaptiveEmcValues.get(stack);

        if (adaptiveValue.isPresent()) {
            return adaptiveValue.get();
        }

        if (ProjectEnervateSourceHelper.isVerified(stack) || !ProjectEnervateConfig.voidUnknownSources()) {
            return AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);
        }

        return BigDecimal.ZERO;
    }

    public static boolean hasPositiveSellValue(ItemStack stack) {
        return getSingleSellValueDecimal(stack).compareTo(BigDecimal.ZERO) > 0;
    }

    public static BigInteger getStackSellValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigInteger.ZERO;
        }

        return getStackSellValue(stack, stack.getCount());
    }

    public static BigInteger getStackSellValue(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            return BigInteger.ZERO;
        }

        return getStackSellValue(getSingleSellValueDecimal(stack), count);
    }

    public static BigInteger getStackSellValue(BigDecimal singleValue, int count) {
        if (count <= 0 || singleValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigInteger.ZERO;
        }

        BigDecimal totalValue = singleValue.multiply(BigDecimal.valueOf(count));
        BigInteger flooredValue = totalValue
                .setScale(0, RoundingMode.DOWN)
                .toBigInteger();

        BigDecimal fractionalValue = totalValue.subtract(new BigDecimal(flooredValue));

        if (fractionalValue.compareTo(BigDecimal.ONE.subtract(INTEGER_ROUNDING_TOLERANCE)) >= 0) {
            return flooredValue.add(BigInteger.ONE);
        }

        return flooredValue;
    }

    public static int getMaxItemsThatFit(BigInteger freeEmc, ItemStack stack) {
        if (stack.isEmpty() || freeEmc.signum() <= 0) {
            return 0;
        }

        return getMaxItemsThatFit(freeEmc, getSingleSellValueDecimal(stack), stack.getCount());
    }

    public static int getMaxItemsThatFit(BigInteger freeEmc, BigDecimal singleValue, int maxCount) {
        if (freeEmc.signum() <= 0 || maxCount <= 0 || singleValue.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        BigDecimal max = new BigDecimal(freeEmc).divide(singleValue, 0, RoundingMode.DOWN);

        if (max.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            return maxCount;
        }

        return Math.min(maxCount, max.intValue());
    }

    public static boolean canMergeIgnoringAdaptiveEmc(ItemStack existing, ItemStack incoming) {
        return canMergeIgnoringProjectEnervateMetadata(existing, incoming);
    }

    public static boolean canMergeIgnoringProjectEnervateMetadata(ItemStack existing, ItemStack incoming) {
        return areSameItemSameComponentsIgnoringProjectEnervate(existing, incoming);
    }

    public static boolean areSameItemSameComponentsIgnoringProjectEnervate(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return false;
        }

        if (!first.is(second.getItem())) {
            return false;
        }

        ItemStack cleanFirst = ProjectEnervateSourceHelper.copyWithoutProjectEnervateData(first);
        ItemStack cleanSecond = ProjectEnervateSourceHelper.copyWithoutProjectEnervateData(second);

        return cleanFirst.getComponents().equals(cleanSecond.getComponents());
    }

    public static boolean shouldUseAdaptiveMerge(ItemStack existing, ItemStack incoming) {
        return shouldUseProjectEnervateMerge(existing, incoming);
    }

    public static boolean shouldUseProjectEnervateMerge(ItemStack existing, ItemStack incoming) {
        if (!ProjectEnervateConfig.adaptiveEmc() && !ProjectEnervateConfig.voidUnknownSources()) {
            return false;
        }

        if (existing.isEmpty() || incoming.isEmpty()) {
            return false;
        }

        if (!canMergeIgnoringProjectEnervateMetadata(existing, incoming)) {
            return false;
        }

        return ProjectEnervateSourceHelper.hasProjectEnervateData(existing)
                || ProjectEnervateSourceHelper.hasProjectEnervateData(incoming);
    }

    public static int mergeIntoExistingStack(ItemStack existing, ItemStack incoming) {
        return mergeIntoExistingStack(existing, incoming, incoming.getCount(), existing.getMaxStackSize());
    }

    public static int mergeIntoExistingStack(ItemStack existing, ItemStack incoming, int maxMove, int maxStackSize) {
        if (!shouldUseProjectEnervateMerge(existing, incoming)) {
            return 0;
        }

        int freeSpace = maxStackSize - existing.getCount();

        if (freeSpace <= 0) {
            return 0;
        }

        int movedCount = Math.min(Math.min(freeSpace, incoming.getCount()), maxMove);

        if (movedCount <= 0) {
            return 0;
        }

        ItemStack beforeExisting = existing.copy();
        ItemStack incomingTemplate = incoming.copy();
        incomingTemplate.setCount(movedCount);

        existing.grow(movedCount);
        incoming.shrink(movedCount);

        applyMergedEconomicState(beforeExisting, incomingTemplate, existing, movedCount);

        return movedCount;
    }

    public static int mergeIntoSlot(Slot slot, ItemStack incoming) {
        return mergeIntoSlot(slot, incoming, incoming.getCount());
    }

    public static int mergeIntoSlot(Slot slot, ItemStack incoming, int maxMove) {
        if (slot == null || incoming.isEmpty() || !slot.hasItem()) {
            return 0;
        }

        if (!slot.mayPlace(incoming)) {
            return 0;
        }

        ItemStack existing = slot.getItem();
        int slotLimit = Math.min(slot.getMaxStackSize(incoming), existing.getMaxStackSize());
        int moved = mergeIntoExistingStack(existing, incoming, maxMove, slotLimit);

        if (moved > 0) {
            slot.setChanged();
        }

        return moved;
    }

    public static boolean mergeIntoInventoryList(NonNullList<ItemStack> items, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return false;
        }

        boolean changed = false;

        for (ItemStack existing : items) {
            if (incoming.isEmpty()) {
                break;
            }

            if (existing.isEmpty()) {
                continue;
            }

            int moved = mergeIntoExistingStack(existing, incoming);

            if (moved > 0) {
                changed = true;
            }
        }

        return changed;
    }

    public static boolean mergeIntoMenuSlots(
            NonNullList<Slot> slots,
            ItemStack incoming,
            int startIndex,
            int endIndex,
            boolean reverseDirection
    ) {
        if (incoming.isEmpty()) {
            return false;
        }

        boolean changed = false;

        if (reverseDirection) {
            for (int i = endIndex - 1; i >= startIndex; i--) {
                if (incoming.isEmpty()) {
                    break;
                }

                Slot slot = slots.get(i);
                int moved = mergeIntoSlot(slot, incoming);

                if (moved > 0) {
                    changed = true;
                }
            }
        } else {
            for (int i = startIndex; i < endIndex; i++) {
                if (incoming.isEmpty()) {
                    break;
                }

                Slot slot = slots.get(i);
                int moved = mergeIntoSlot(slot, incoming);

                if (moved > 0) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    /**
     * Recomputes the economic state of a stack after vanilla or a modded inventory
     * has already merged inserted items into it because ProjectEnervate metadata was
     * ignored for stack compatibility.
     */
    public static void applyMergedEconomicState(
            ItemStack beforeExisting,
            ItemStack incomingTemplate,
            ItemStack mergedStack,
            int movedCount
    ) {
        if (mergedStack.isEmpty() || incomingTemplate.isEmpty() || movedCount <= 0) {
            return;
        }

        if (!beforeExisting.isEmpty()
                && !canMergeIgnoringProjectEnervateMetadata(beforeExisting, incomingTemplate)) {
            return;
        }

        if (!canMergeIgnoringProjectEnervateMetadata(mergedStack, incomingTemplate)) {
            return;
        }

        BigDecimal existingTotal = BigDecimal.ZERO;

        if (!beforeExisting.isEmpty()) {
            existingTotal = getSingleMergeValueDecimal(beforeExisting)
                    .multiply(BigDecimal.valueOf(beforeExisting.getCount()));
        }

        BigDecimal incomingTotal = getSingleMergeValueDecimal(incomingTemplate)
                .multiply(BigDecimal.valueOf(movedCount));

        BigDecimal combinedTotal = existingTotal.add(incomingTotal);

        if (AdaptiveEmcOutputHelper.getBaseSingleEmc(mergedStack).signum() <= 0
                && combinedTotal.signum() > 0) {
            BigDecimal perItem = combinedTotal.divide(
                    BigDecimal.valueOf(mergedStack.getCount()),
                    AdaptiveEmcValues.INTERNAL_SCALE,
                    RoundingMode.HALF_UP
            );
            ProjectEnervateSourceHelper.markAssignedEmc(mergedStack, perItem);
            return;
        }

        AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(mergedStack, combinedTotal);
    }

    public static BigDecimal getSingleMergeValueDecimal(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return getSingleSellValueDecimal(stack);
    }
}
