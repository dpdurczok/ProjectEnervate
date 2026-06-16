package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public final class CraftingAdaptiveEmcHelper {

    private CraftingAdaptiveEmcHelper() {
    }

    public static void applyToCraftingOutput(CraftingContainer craftSlots, ItemStack craftedStack) {
        if (craftSlots == null || craftedStack.isEmpty()) {
            return;
        }

        BigDecimal totalInputEmc = BigDecimal.ZERO;
        boolean hasAdaptiveInput = false;

        for (int slotIndex = 0; slotIndex < craftSlots.getContainerSize(); slotIndex++) {
            ItemStack inputStack = craftSlots.getItem(slotIndex);

            if (inputStack.isEmpty()) {
                continue;
            }

            if (AdaptiveEmcValues.has(inputStack)) {
                hasAdaptiveInput = true;
            }

            BigDecimal singleInputValue = AdaptiveEmcHelper.getSingleSellValueDecimal(inputStack);

            if (singleInputValue.signum() > 0) {
                totalInputEmc = totalInputEmc.add(singleInputValue);
            }
        }

        if (!hasAdaptiveInput) {
            AdaptiveEmcValues.remove(craftedStack);
            return;
        }

        AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(craftedStack, totalInputEmc);
    }
}