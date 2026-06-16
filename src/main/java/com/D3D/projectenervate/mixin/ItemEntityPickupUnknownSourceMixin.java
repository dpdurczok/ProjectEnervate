package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupUnknownSourceMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void projectenervate$unknownSourceBeforePickup(Player player, CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;

        if (!(itemEntity.level() instanceof ServerLevel)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();

        if (stack.isEmpty()) {
            return;
        }

        if (ProjectEnervateSourceHelper.hasKnownSource(stack)) {
            return;
        }

        ProjectEnervateSourceHelper.enforceUnknownMinimum(stack);
    }
}
