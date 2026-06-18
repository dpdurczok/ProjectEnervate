package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.registry.ProjectEnervateItems;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerCelestialScopeMixin {
    @Inject(method = "isScoping", at = @At("HEAD"), cancellable = true)
    private void projectenervate$isUsingCelestialScope(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (player.isUsingItem() && player.getUseItem().is(ProjectEnervateItems.CELESTIAL_SCOPE.get())) {
            cir.setReturnValue(true);
        }
    }
}
