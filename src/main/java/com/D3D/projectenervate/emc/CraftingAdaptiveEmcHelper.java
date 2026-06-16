package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public final class CraftingAdaptiveEmcHelper {

    private static final ThreadLocal<CraftingTakeAllocation> CURRENT_TAKE_ALLOCATION = new ThreadLocal<>();

    private CraftingAdaptiveEmcHelper() {
    }

    public static void applyToCraftingOutput(CraftingContainer craftSlots, ItemStack craftedStack) {
        CraftingTakeAllocation allocation = createAllocation(craftSlots, craftedStack);

        if (allocation == null) {
            AdaptiveEmcValues.remove(craftedStack);
            return;
        }

        AdaptiveEmcOutputHelper.copyAdaptiveState(allocation.mainOutput(), craftedStack);
    }

    public static void prepareForTake(CraftingContainer craftSlots, ItemStack craftedStack) {
        CraftingTakeAllocation allocation = createAllocation(craftSlots, craftedStack);
        CURRENT_TAKE_ALLOCATION.set(allocation);

        if (allocation == null) {
            AdaptiveEmcValues.remove(craftedStack);
            return;
        }

        AdaptiveEmcOutputHelper.copyAdaptiveState(allocation.mainOutput(), craftedStack);
    }

    public static void applyPreparedRemainders(NonNullList<ItemStack> remainders) {
        CraftingTakeAllocation allocation = CURRENT_TAKE_ALLOCATION.get();

        if (allocation == null || remainders == null) {
            return;
        }

        NonNullList<ItemStack> allocatedRemainders = allocation.remaindersBySlot();

        int max = Math.min(remainders.size(), allocatedRemainders.size());

        for (int slotIndex = 0; slotIndex < max; slotIndex++) {
            ItemStack actualRemainder = remainders.get(slotIndex);
            ItemStack allocatedRemainder = allocatedRemainders.get(slotIndex);

            if (actualRemainder.isEmpty() || allocatedRemainder.isEmpty()) {
                continue;
            }

            AdaptiveEmcOutputHelper.copyAdaptiveState(allocatedRemainder, actualRemainder);
        }
    }

    public static void clearPreparedTake() {
        CURRENT_TAKE_ALLOCATION.remove();
    }

    private static CraftingTakeAllocation createAllocation(
            CraftingContainer craftSlots,
            ItemStack craftedStack
    ) {
        if (craftSlots == null || craftedStack.isEmpty()) {
            return null;
        }

        BigDecimal inputBudget = BigDecimal.ZERO;

        List<ItemStack> outputTemplates = new ArrayList<>();
        List<Integer> outputRemainderSlots = new ArrayList<>();

        outputTemplates.add(craftedStack.copy());
        outputRemainderSlots.add(-1);

        for (int slotIndex = 0; slotIndex < craftSlots.getContainerSize(); slotIndex++) {
            ItemStack inputStack = craftSlots.getItem(slotIndex);

            if (inputStack.isEmpty()) {
                continue;
            }

            inputBudget = inputBudget.add(
                    AdaptiveEmcConversionHelper.getOneItemBudget(inputStack)
            );

            ItemStack remainder = inputStack.getCraftingRemainingItem();

            if (!remainder.isEmpty()) {
                outputTemplates.add(remainder.copy());
                outputRemainderSlots.add(slotIndex);
            }
        }

        if (inputBudget.signum() <= 0) {
            return null;
        }

        List<ItemStack> cappedOutputs = AdaptiveEmcConversionHelper.createCappedOutputCopies(
                inputBudget,
                outputTemplates
        );

        if (cappedOutputs.isEmpty()) {
            return null;
        }

        ItemStack cappedMainOutput = cappedOutputs.get(0);

        NonNullList<ItemStack> cappedRemainders = NonNullList.withSize(
                craftSlots.getContainerSize(),
                ItemStack.EMPTY
        );

        for (int outputIndex = 1; outputIndex < cappedOutputs.size(); outputIndex++) {
            int slotIndex = outputRemainderSlots.get(outputIndex);

            if (slotIndex >= 0 && slotIndex < cappedRemainders.size()) {
                cappedRemainders.set(slotIndex, cappedOutputs.get(outputIndex));
            }
        }

        return new CraftingTakeAllocation(cappedMainOutput, cappedRemainders);
    }

    private record CraftingTakeAllocation(
            ItemStack mainOutput,
            NonNullList<ItemStack> remaindersBySlot
    ) {
    }
}