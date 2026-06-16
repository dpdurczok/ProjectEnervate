package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.world.item.ItemStack;

public final class UnknownSourceEmcSafetyHelper {
    private static final BigDecimal UNKNOWN_SOURCE_SINGLE_ITEM_EMC = BigDecimal.ONE;

    private UnknownSourceEmcSafetyHelper() {
    }

    public static boolean applyMinimumIfUnknown(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (AdaptiveEmcValues.has(stack)) {
            return false;
        }

        if (isProtectedProjectEItem(stack)) {
            return false;
        }

        BigDecimal baseSingleEmc = AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);

        if (baseSingleEmc.signum() <= 0) {
            return false;
        }

        BigDecimal proposedStackEmc = UNKNOWN_SOURCE_SINGLE_ITEM_EMC
                .multiply(BigDecimal.valueOf(stack.getCount()));

        AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(stack, proposedStackEmc);

        return AdaptiveEmcValues.has(stack);
    }

    private static boolean isProtectedProjectEItem(ItemStack stack) {
        if (stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null) {
            return true;
        }

        return stack.is(PEItems.TOME_OF_KNOWLEDGE);
    }
}