package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AdaptiveEmcHelper {

    private AdaptiveEmcHelper() {
    }

    public static boolean hasAdaptiveValue(ItemStack stack) {
        return AdaptiveEmcValues.has(stack);
    }

    public static BigDecimal getSingleSellValueDecimal(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return AdaptiveEmcValues.get(stack)
                .orElseGet(() -> BigDecimal.valueOf(IEMCProxy.INSTANCE.getSellValue(stack)));
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

        if (singleValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigInteger.ZERO;
        }

        return singleValue
                .multiply(BigDecimal.valueOf(count))
                .setScale(0, RoundingMode.DOWN)
                .toBigInteger();
    }

    public static int getMaxItemsThatFit(BigInteger freeEmc, ItemStack stack) {
        if (stack.isEmpty() || freeEmc.signum() <= 0) {
            return 0;
        }

        BigDecimal singleValue = getSingleSellValueDecimal(stack);

        if (singleValue.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        BigDecimal max = new BigDecimal(freeEmc)
                .divide(singleValue, 0, RoundingMode.DOWN);

        if (max.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            return stack.getCount();
        }

        return Math.min(stack.getCount(), max.intValue());
    }

    public static boolean canMergeIgnoringAdaptiveEmc(ItemStack existing, ItemStack incoming) {
        if (existing.isEmpty() || incoming.isEmpty()) {
            return false;
        }

        if (!existing.is(incoming.getItem())) {
            return false;
        }

        ItemStack cleanExisting = AdaptiveEmcValues.copyWithoutAdaptiveEmc(existing);
        ItemStack cleanIncoming = AdaptiveEmcValues.copyWithoutAdaptiveEmc(incoming);

        return ItemStack.isSameItemSameComponents(cleanExisting, cleanIncoming);
    }

    public static boolean shouldUseAdaptiveMerge(ItemStack existing, ItemStack incoming) {
        if (existing.isEmpty() || incoming.isEmpty()) {
            return false;
        }

        if (!AdaptiveEmcValues.has(existing) && !AdaptiveEmcValues.has(incoming)) {
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
        BigDecimal existingEach = getSingleSellValueDecimal(existing);
        BigDecimal incomingEach = getSingleSellValueDecimal(incoming);

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

        AdaptiveEmcValues.setExact(existing, newEach);
    }
}