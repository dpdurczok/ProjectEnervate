package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.CreateEmcIntegrationHelper;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.recipe.RecipeApplier", remap = false)
public abstract class CreateRecipeApplierMixin {

    @Inject(
            method = "applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private static void projectenervate$capCreateRecipeApplierOutputs(
            Level level,
            ItemStack stackIn,
            Recipe<?> recipe,
            boolean returnProcessingRemainder,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        if (level == null || level.isClientSide()) {
            return;
        }

        CreateEmcIntegrationHelper.applyRecipeOutputs(stackIn, cir.getReturnValue());
    }
}
