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
    private void projectenervate$prepareCraftingConversion(
            Player player,
            ItemStack craftedStack,
            CallbackInfo ci
    ) {
        if (player.level().isClientSide) {
            return;
        }

        CraftingAdaptiveEmcHelper.prepareForTake(craftSlots, craftedStack);
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void projectenervate$clearCraftingConversion(
            Player player,
            ItemStack craftedStack,
            CallbackInfo ci
    ) {
        if (player.level().isClientSide) {
            return;
        }

        CraftingAdaptiveEmcHelper.clearPreparedTake();
    }
}