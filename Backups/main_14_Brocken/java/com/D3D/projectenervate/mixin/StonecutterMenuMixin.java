package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcConversionHelper;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin {

    @Shadow
    @Final
    private Container container;

    @Shadow
    @Final
    private ResultContainer resultContainer;

    @Inject(method = "setupResultSlot", at = @At("RETURN"))
    private void projectenervate$applyAdaptiveEmcToStonecutterOutput(CallbackInfo ci) {
        ItemStack inputStack = container.getItem(0);
        ItemStack outputStack = resultContainer.getItem(0);

        if (inputStack.isEmpty() || outputStack.isEmpty()) {
            return;
        }

        BigDecimal inputBudget = AdaptiveEmcConversionHelper.getOneItemBudget(inputStack);

        AdaptiveEmcConversionHelper.applyBudgetToOutputsInPlace(
                inputBudget,
                List.of(outputStack)
        );
    }
}