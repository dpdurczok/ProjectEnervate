package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.CraftingAdaptiveEmcHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {

    @Shadow
    @Final
    private CraftingContainer craftSlots;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void projectenervate$applyAdaptiveEmcToCraftingResultOnTake(
            Player player,
            ItemStack craftedStack,
            CallbackInfo ci
    ) {
        if (player.level().isClientSide) {
            return;
        }

        CraftingAdaptiveEmcHelper.applyToCraftingOutput(craftSlots, craftedStack);
    }
}