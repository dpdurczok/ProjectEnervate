package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.world.item.ItemStack;

public final class AdaptiveEmcOutputHelper {

    private AdaptiveEmcOutputHelper() {
    }

    /**
     * Global ProjectEnervate output rule:
     *
     * If the proposed adaptive stack value is lower than the output's normal base EMC,
     * apply adaptive EMC.
     *
     * If the proposed adaptive stack value is equal to or higher than the output's normal base EMC,
     * remove adaptive EMC so the stack uses its normal/base value.
     *
     * If the output has no base EMC, remove adaptive EMC.
     */
    public static void applyCappedAdaptiveStackEmc(
            ItemStack outputStack,
            BigDecimal proposedAdaptiveStackEmc
    ) {
        if (outputStack.isEmpty()) {
            return;
        }

        if (proposedAdaptiveStackEmc == null || proposedAdaptiveStackEmc.signum() <= 0) {
            AdaptiveEmcValues.remove(outputStack);
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

        // If adaptive value would be equal to or above base value,
        // keep the item normal. This prevents overvalued adaptive outputs.
        if (proposedAdaptiveStackEmc.compareTo(baseStackEmc) >= 0) {
            AdaptiveEmcValues.remove(outputStack);
            return;
        }

        BigDecimal adaptivePerItem = proposedAdaptiveStackEmc.divide(
                BigDecimal.valueOf(outputCount),
                AdaptiveEmcValues.INTERNAL_SCALE,
                RoundingMode.HALF_UP
        );

        if (adaptivePerItem.signum() <= 0) {
            AdaptiveEmcValues.remove(outputStack);
            return;
        }

        AdaptiveEmcValues.setExact(outputStack, adaptivePerItem);
    }

    public static BigDecimal getBaseStackEmc(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int count = stack.getCount();

        if (count <= 0) {
            return BigDecimal.ZERO;
        }

        ItemStack cleanStack = AdaptiveEmcValues.copyWithoutAdaptiveEmc(stack);

        long baseSingleEmc = IEMCProxy.INSTANCE.getSellValue(cleanStack);

        if (baseSingleEmc <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(baseSingleEmc)
                .multiply(BigDecimal.valueOf(count));
    }

    public static BigDecimal getBaseSingleEmc(ItemStack stack) {
        if (stack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        ItemStack cleanStack = AdaptiveEmcValues.copyWithoutAdaptiveEmc(stack);

        long baseSingleEmc = IEMCProxy.INSTANCE.getSellValue(cleanStack);

        if (baseSingleEmc <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(baseSingleEmc);
    }
}