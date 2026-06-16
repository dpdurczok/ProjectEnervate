package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.CraftingAdaptiveEmcHelper;
import com.D3D.projectenervate.emc.ProjectECollectorDisabler;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Inject(method = "getRemainingItemsFor", at = @At("RETURN"))
    private void projectenervate$applyAdaptiveEmcToCraftingRemainders(
            RecipeType<?> recipeType,
            RecipeInput input,
            Level level,
            CallbackInfoReturnable<NonNullList<ItemStack>> cir
    ) {
        CraftingAdaptiveEmcHelper.applyPreparedRemainders(cir.getReturnValue());
    }

    @Inject(method = "getAllRecipesFor", at = @At("RETURN"), cancellable = true, require = 0)
    private void projectenervate$hideCollectorRecipesFromType(
            RecipeType<?> recipeType,
            CallbackInfoReturnable<List<RecipeHolder<?>>> cir
    ) {
        cir.setReturnValue(ProjectECollectorDisabler.filterList(cir.getReturnValue(), registries));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(
            method = "getRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void projectenervate$hideCollectorRecipesFromMatches(
            RecipeType recipeType,
            RecipeInput input,
            Level level,
            CallbackInfoReturnable<List> cir
    ) {
        cir.setReturnValue(ProjectECollectorDisabler.filterList(cir.getReturnValue(), level.registryAccess()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(
            method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void projectenervate$hideCollectorRecipeFromMatch(
            RecipeType recipeType,
            RecipeInput input,
            Level level,
            CallbackInfoReturnable<Optional> cir
    ) {
        cir.setReturnValue(ProjectECollectorDisabler.filterOptional(cir.getReturnValue(), level.registryAccess()));
    }
}
