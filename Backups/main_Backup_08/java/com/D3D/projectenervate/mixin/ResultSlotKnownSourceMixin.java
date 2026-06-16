package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotKnownSourceMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void projectenervate$markCraftingResultKnown(
            Player player,
            ItemStack stack,
            CallbackInfo ci
    ) {
        ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(stack);
    }
}