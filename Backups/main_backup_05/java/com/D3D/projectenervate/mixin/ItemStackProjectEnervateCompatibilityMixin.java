package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackProjectEnervateCompatibilityMixin {

    @Inject(method = "isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private static void projectenervate$ignoreOwnMetadataForStackCompatibility(
            ItemStack first,
            ItemStack second,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ()) {
            return;
        }

        if (first.isEmpty() || second.isEmpty()) {
            return;
        }

        if (!first.is(second.getItem())) {
            return;
        }

        if (!ProjectEnervateSourceHelper.hasProjectEnervateData(first)
                && !ProjectEnervateSourceHelper.hasProjectEnervateData(second)) {
            return;
        }

        if (AdaptiveEmcHelper.areSameItemSameComponentsIgnoringProjectEnervate(first, second)) {
            cir.setReturnValue(true);
        }
    }
}
