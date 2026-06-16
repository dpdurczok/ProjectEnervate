package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.world.item.ItemStack;

public final class AdaptiveEmcOutputHelper {

    private AdaptiveEmcOutputHelper() {
    }

    public static void applyCappedAdaptiveStackEmc(
            ItemStack outputStack,
            BigDecimal proposedAdaptiveStackEmc
    ) {
        if (outputStack.isEmpty()) {
            return;
        }

        if (proposedAdaptiveStackEmc == null || proposedAdaptiveStackEmc.signum() <= 0) {
            ProjectEnervateSourceHelper.markUnknownSource(outputStack);
            return;
        }

        int outputCount = outputStack.getCount();

        if (outputCount <= 0) {
            AdaptiveEmcValues.remove(outputStack);
            return;
        }

        BigDecimal baseStackEmc = getBaseStackEmc(outputStack);

        if (baseStackEmc.signum() <= 0) {
            AdaptiveEmcValues.remove(outputStack);
            return;
        }

        if (proposedAdaptiveStackEmc.compareTo(baseStackEmc) >= 0) {
            AdaptiveEmcValues.remove(outputStack);
            ProjectEnervateSourceHelper.markKnown(outputStack, ProjectEnervateSourceHelper.SOURCE_TRACKED_CONVERSION);
            return;
        }

        BigDecimal adaptivePerItem = proposedAdaptiveStackEmc.divide(
                BigDecimal.valueOf(outputCount),
                AdaptiveEmcValues.INTERNAL_SCALE,
                RoundingMode.HALF_UP
        );

        if (adaptivePerItem.signum() <= 0) {
            ProjectEnervateSourceHelper.markUnknownSource(outputStack);
            return;
        }

        AdaptiveEmcValues.setExact(outputStack, adaptivePerItem);
        ProjectEnervateSourceHelper.markKnown(outputStack, ProjectEnervateSourceHelper.SOURCE_TRACKED_CONVERSION);
    }

    public static BigDecimal getBaseStackEmc(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int count = stack.getCount();

        if (count <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal single = getBaseSingleEmc(stack);

        if (single.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        return single.multiply(BigDecimal.valueOf(count));
    }

    public static BigDecimal getBaseSingleEmc(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        ItemStack cleanStack = AdaptiveEmcValues.copyWithoutAdaptiveEmc(stack);
        cleanStack.setCount(1);

        long baseSingleEmc = IEMCProxy.INSTANCE.getSellValue(cleanStack);

        if (baseSingleEmc <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(baseSingleEmc);
    }

    public static BigDecimal getEffectiveStackEmc(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal single = AdaptiveEmcHelper.getSingleSellValueDecimal(stack);

        if (single.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        return single.multiply(BigDecimal.valueOf(stack.getCount()));
    }

    public static BigDecimal getEffectiveSingleEmc(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return AdaptiveEmcHelper.getSingleSellValueDecimal(stack);
    }

    public static void copyAdaptiveState(ItemStack from, ItemStack to) {
        if (from.isEmpty() || to.isEmpty()) {
            return;
        }

        Optional<BigDecimal> value = AdaptiveEmcValues.get(from);

        if (value.isPresent()) {
            AdaptiveEmcValues.setExact(to, value.get());
        } else {
            AdaptiveEmcValues.remove(to);
        }
    }

    public static void mergeGeneratedIntoResultStack(
            ItemStack beforeResult,
            ItemStack afterResult,
            int generatedCount,
            BigDecimal generatedBudget
    ) {
        if (afterResult.isEmpty()) {
            return;
        }

        if (generatedCount <= 0) {
            return;
        }

        BigDecimal oldStackEmc = BigDecimal.ZERO;

        if (!beforeResult.isEmpty()) {
            oldStackEmc = getEffectiveStackEmc(beforeResult);
        }

        ItemStack generatedStack = afterResult.copy();
        generatedStack.setCount(generatedCount);

        applyCappedAdaptiveStackEmc(generatedStack, generatedBudget);

        BigDecimal generatedFinalEmc = getEffectiveStackEmc(generatedStack);
        BigDecimal combinedEmc = oldStackEmc.add(generatedFinalEmc);

        applyCappedAdaptiveStackEmc(afterResult, combinedEmc);
    }
}
