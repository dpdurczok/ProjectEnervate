package com.D3D.projectenervate.emc;

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

        if (ProjectEnervateSourceHelper.isZero(stack)) {
            return BigDecimal.ZERO;
        }

        Optional<BigDecimal> adaptiveValue = AdaptiveEmcValues.get(stack);

        if (adaptiveValue.isPresent()) {
            return adaptiveValue.get();
        }

        if (ProjectEnervateSourceHelper.isVerified(stack)) {
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

        BigDecimal singleValue = getSingleSellValueDecimal(stack);

        return getStackSellValue(singleValue, count);
    }

    public static BigInteger getStackSellValue(BigDecimal singleValue, int count) {
        if (count <= 0) {
            return BigInteger.ZERO;
        }

        if (singleValue.compareTo(BigDecimal.ZERO) <= 0) {
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

        BigDecimal singleValue = getSingleSellValueDecimal(stack);

        return getMaxItemsThatFit(freeEmc, singleValue, stack.getCount());
    }

    public static int getMaxItemsThatFit(BigInteger freeEmc, BigDecimal singleValue, int maxCount) {
        if (freeEmc.signum() <= 0 || maxCount <= 0) {
            return 0;
        }

        if (singleValue.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        BigDecimal max = new BigDecimal(freeEmc)
                .divide(singleValue, 0, RoundingMode.DOWN);

        if (max.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            return maxCount;
        }

        return Math.min(maxCount, max.intValue());
    }

    public static boolean canMergeIgnoringAdaptiveEmc(ItemStack existing, ItemStack incoming) {
        if (existing.isEmpty() || incoming.isEmpty()) {
            return false;
        }

        if (!existing.is(incoming.getItem())) {
            return false;
        }

        ItemStack cleanExisting = ProjectEnervateSourceHelper.copyWithoutProjectEnervateData(existing);
        ItemStack cleanIncoming = ProjectEnervateSourceHelper.copyWithoutProjectEnervateData(incoming);

        return ItemStack.isSameItemSameComponents(cleanExisting, cleanIncoming);
    }

    public static boolean shouldUseAdaptiveMerge(ItemStack existing, ItemStack incoming) {
        if (existing.isEmpty() || incoming.isEmpty()) {
            return false;
        }

        if (!hasMergeRelevantEmc(existing) && !hasMergeRelevantEmc(incoming)) {
            return false;
        }

        return canMergeIgnoringAdaptiveEmc(existing, incoming);
    }

    public static int mergeIntoExistingStack(ItemStack existing, ItemStack incoming) {
        return mergeIntoExistingStack(existing, incoming, incoming.getCount(), existing.getMaxStackSize());
    }

    public static int mergeIntoExistingStack(ItemStack existing, ItemStack incoming, int maxMove, int maxStackSize) {
        if (!shouldUseAdaptiveMerge(existing, incoming)) {
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

        redistributeAdaptiveEmc(existing, incoming, movedCount);

        existing.grow(movedCount);
        incoming.shrink(movedCount);

        return movedCount;
    }

    public static int mergeIntoSlot(Slot slot, ItemStack incoming) {
        return mergeIntoSlot(slot, incoming, incoming.getCount());
    }

    public static int mergeIntoSlot(Slot slot, ItemStack incoming, int maxMove) {
        if (slot == null || incoming.isEmpty()) {
            return 0;
        }

        if (!slot.hasItem()) {
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

    private static void redistributeAdaptiveEmc(ItemStack existing, ItemStack incoming, int movedCount) {
        BigDecimal existingEach = getSingleMergeValueDecimal(existing);
        BigDecimal incomingEach = getSingleMergeValueDecimal(incoming);

        BigDecimal existingTotal = existingEach.multiply(BigDecimal.valueOf(existing.getCount()));
        BigDecimal incomingMovedTotal = incomingEach.multiply(BigDecimal.valueOf(movedCount));

        int newCount = existing.getCount() + movedCount;

        BigDecimal newEach = existingTotal
                .add(incomingMovedTotal)
                .divide(
                        BigDecimal.valueOf(newCount),
                        AdaptiveEmcValues.INTERNAL_SCALE,
                        RoundingMode.HALF_UP
                );

        BigDecimal baseEach = AdaptiveEmcOutputHelper.getBaseSingleEmc(existing);

        if (newEach.signum() <= 0) {
            if (!ProjectEnervateSourceHelper.markZeroIfBaseEmc(existing)) {
                ProjectEnervateSourceHelper.clearProjectEnervateData(existing);
            }
            return;
        }

        if (baseEach.signum() > 0 && newEach.compareTo(baseEach) >= 0) {
            if (!ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(existing)) {
                ProjectEnervateSourceHelper.clearProjectEnervateData(existing);
            }
            return;
        }

        ProjectEnervateSourceHelper.markAdaptive(existing, newEach);
    }

    public static BigDecimal getSingleMergeValueDecimal(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return getSingleSellValueDecimal(stack);
    }

    private static boolean hasMergeRelevantEmc(ItemStack stack) {
        return ProjectEnervateSourceHelper.hasProjectEnervateData(stack);
    }
}
