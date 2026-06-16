package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.CraftingAdaptiveEmcHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @Inject(method = "getRemainingItemsFor", at = @At("RETURN"))
    private void projectenervate$applyAdaptiveEmcToCraftingRemainders(
            RecipeType<?> recipeType,
            RecipeInput input,
            Level level,
            CallbackInfoReturnable<NonNullList<ItemStack>> cir
    ) {
        CraftingAdaptiveEmcHelper.applyPreparedRemainders(cir.getReturnValue());
    }
}