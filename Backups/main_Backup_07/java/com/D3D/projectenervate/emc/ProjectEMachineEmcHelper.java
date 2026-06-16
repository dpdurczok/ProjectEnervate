package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class ProjectEMachineEmcHelper {

    private ProjectEMachineEmcHelper() {
    }

    public static List<ItemStack> snapshotInventory(IItemHandler handler) {
        List<ItemStack> snapshot = new ArrayList<>();

        if (handler == null) {
            return snapshot;
        }

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            snapshot.add(handler.getStackInSlot(slot).copy());
        }

        return snapshot;
    }

    public static BigDecimal getOneItemInputBudget(ItemStack input) {
        if (input.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return AdaptiveEmcHelper.getSingleSellValueDecimal(input);
    }

    public static long getEffectiveSingleSellValueAsLong(ItemStack stack) {
        BigInteger value = AdaptiveEmcHelper.getStackSellValue(stack, 1);

        if (value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            return Long.MAX_VALUE;
        }

        return value.longValue();
    }

    public static void applyGeneratedOutputBudget(
            List<ItemStack> beforeOutputs,
            IItemHandler outputInventory,
            BigDecimal inputBudget
    ) {
        if (outputInventory == null) {
            return;
        }

        List<GeneratedOutput> generatedOutputs = findGeneratedOutputs(beforeOutputs, outputInventory);

        if (generatedOutputs.isEmpty()) {
            return;
        }

        if (inputBudget == null || inputBudget.signum() <= 0) {
            for (GeneratedOutput generatedOutput : generatedOutputs) {
                AdaptiveEmcOutputHelper.mergeGeneratedIntoResultStack(
                        generatedOutput.beforeStack(),
                        generatedOutput.afterStack(),
                        generatedOutput.generatedCount(),
                        BigDecimal.ZERO
                );
            }
            return;
        }

        BigDecimal totalBaseWeight = BigDecimal.ZERO;

        for (GeneratedOutput generatedOutput : generatedOutputs) {
            ItemStack generatedStack = generatedOutput.afterStack().copy();
            generatedStack.setCount(generatedOutput.generatedCount());
            totalBaseWeight = totalBaseWeight.add(AdaptiveEmcOutputHelper.getBaseStackEmc(generatedStack));
        }

        if (totalBaseWeight.signum() <= 0) {
            for (GeneratedOutput generatedOutput : generatedOutputs) {
                ProjectEnervateSourceHelper.clearProjectEnervateData(generatedOutput.afterStack());
            }
            return;
        }

        for (GeneratedOutput generatedOutput : generatedOutputs) {
            ItemStack generatedStack = generatedOutput.afterStack().copy();
            generatedStack.setCount(generatedOutput.generatedCount());

            BigDecimal generatedBaseWeight = AdaptiveEmcOutputHelper.getBaseStackEmc(generatedStack);
            BigDecimal generatedBudget = inputBudget
                    .multiply(generatedBaseWeight)
                    .divide(totalBaseWeight, AdaptiveEmcValues.INTERNAL_SCALE, RoundingMode.HALF_UP);

            AdaptiveEmcOutputHelper.mergeGeneratedIntoResultStack(
                    generatedOutput.beforeStack(),
                    generatedOutput.afterStack(),
                    generatedOutput.generatedCount(),
                    generatedBudget
            );
        }
    }

    private static List<GeneratedOutput> findGeneratedOutputs(List<ItemStack> beforeOutputs, IItemHandler outputInventory) {
        List<GeneratedOutput> generatedOutputs = new ArrayList<>();
        int slots = outputInventory.getSlots();

        for (int slot = 0; slot < slots; slot++) {
            ItemStack afterStack = outputInventory.getStackInSlot(slot);

            if (afterStack.isEmpty()) {
                continue;
            }

            ItemStack beforeStack = slot < beforeOutputs.size() ? beforeOutputs.get(slot) : ItemStack.EMPTY;
            int generatedCount = getGeneratedCount(beforeStack, afterStack);

            if (generatedCount > 0) {
                generatedOutputs.add(new GeneratedOutput(beforeStack, afterStack, generatedCount));
            }
        }

        return generatedOutputs;
    }

    private static int getGeneratedCount(ItemStack beforeStack, ItemStack afterStack) {
        if (afterStack.isEmpty()) {
            return 0;
        }

        if (beforeStack.isEmpty()) {
            return afterStack.getCount();
        }

        if (!AdaptiveEmcHelper.canMergeIgnoringProjectEnervateMetadata(beforeStack, afterStack)) {
            return afterStack.getCount();
        }

        return Math.max(0, afterStack.getCount() - beforeStack.getCount());
    }

    private record GeneratedOutput(ItemStack beforeStack, ItemStack afterStack, int generatedCount) {
    }
}
