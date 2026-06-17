package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervateConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class AdaptiveEmcConversionHelper {

    private AdaptiveEmcConversionHelper() {
    }

    public static BigDecimal getOneItemBudget(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal value = AdaptiveEmcOutputHelper.getEffectiveSingleEmc(stack);

        if (value.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        return value;
    }

    public static BigDecimal getOneItemBudgetFromContainer(Container container) {
        if (container == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;

        for (int slotIndex = 0; slotIndex < container.getContainerSize(); slotIndex++) {
            ItemStack stack = container.getItem(slotIndex);

            if (stack.isEmpty()) {
                continue;
            }

            total = total.add(getOneItemBudget(stack));
        }

        return total;
    }

    public static void applyBudgetToOutputsInPlace(
            BigDecimal inputBudget,
            List<ItemStack> outputs
    ) {
        if (outputs == null || outputs.isEmpty()) {
            return;
        }

        if (!ProjectEnervateConfig.adaptiveEmc()) {
            for (ItemStack output : outputs) {
                if (!ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(output)) {
                    ProjectEnervateSourceHelper.clearProjectEnervateData(output);
                }
            }
            return;
        }

        if (inputBudget == null || inputBudget.signum() <= 0) {
            for (ItemStack output : outputs) {
                AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(output, BigDecimal.ZERO);
            }
            return;
        }

        BigDecimal totalOutputBaseEmc = BigDecimal.ZERO;

        for (ItemStack output : outputs) {
            totalOutputBaseEmc = totalOutputBaseEmc.add(
                    AdaptiveEmcOutputHelper.getBaseStackEmc(output)
            );
        }

        if (totalOutputBaseEmc.signum() <= 0) {
            for (ItemStack output : outputs) {
                ProjectEnervateSourceHelper.clearProjectEnervateData(output);
            }
            return;
        }

        if (inputBudget.compareTo(totalOutputBaseEmc) >= 0 && ProjectEnervateConfig.chooseBaseEmcIfLower()) {
            for (ItemStack output : outputs) {
                if (!ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(output)) {
                    ProjectEnervateSourceHelper.clearProjectEnervateData(output);
                }
            }
            return;
        }

        for (ItemStack output : outputs) {
            if (output.isEmpty()) {
                continue;
            }

            BigDecimal outputBaseEmc = AdaptiveEmcOutputHelper.getBaseStackEmc(output);

            if (outputBaseEmc.signum() <= 0) {
                ProjectEnervateSourceHelper.clearProjectEnervateData(output);
                continue;
            }

            BigDecimal proposedOutputBudget = inputBudget
                    .multiply(outputBaseEmc)
                    .divide(
                            totalOutputBaseEmc,
                            AdaptiveEmcValues.INTERNAL_SCALE,
                            RoundingMode.HALF_UP
                    );

            AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(
                    output,
                    proposedOutputBudget
            );
        }
    }

    public static List<ItemStack> createCappedOutputCopies(
            BigDecimal inputBudget,
            List<ItemStack> outputTemplates
    ) {
        List<ItemStack> copies = new ArrayList<>();

        for (ItemStack output : outputTemplates) {
            copies.add(output.copy());
        }

        applyBudgetToOutputsInPlace(inputBudget, copies);
        return copies;
    }
}
