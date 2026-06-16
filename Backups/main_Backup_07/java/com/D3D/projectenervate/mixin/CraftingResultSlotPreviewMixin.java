package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.CraftingAdaptiveEmcHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class CraftingResultSlotPreviewMixin {

    @Inject(method = "getItem", at = @At("RETURN"))
    private void projectenervate$applyAdaptiveEmcBeforeCraftingResultIsTaken(
            CallbackInfoReturnable<ItemStack> cir
    ) {
        Slot slot = (Slot) (Object) this;

        if (!(slot instanceof ResultSlot)) {
            return;
        }

        ItemStack resultStack = cir.getReturnValue();

        if (resultStack.isEmpty()) {
            return;
        }

        CraftingContainer craftSlots =
                ((ResultSlotAccessor) slot).projectenervate$getCraftSlots();

        CraftingAdaptiveEmcHelper.applyToCraftingOutput(craftSlots, resultStack);
    }
}