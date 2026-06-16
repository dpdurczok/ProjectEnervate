package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcConversionHelper;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    @Shadow
    @Final
    protected Container inputSlots;

    @Shadow
    @Final
    protected ResultContainer resultSlots;

    @Inject(method = "slotsChanged", at = @At("RETURN"))
    private void projectenervate$applyAdaptiveEmcToItemCombinerOutput(
            Container container,
            CallbackInfo ci
    ) {
        ItemStack outputStack = resultSlots.getItem(0);

        if (outputStack.isEmpty()) {
            return;
        }

        BigDecimal inputBudget = AdaptiveEmcConversionHelper.getOneItemBudgetFromContainer(inputSlots);

        AdaptiveEmcConversionHelper.applyBudgetToOutputsInPlace(
                inputBudget,
                List.of(outputStack)
        );
    }
}