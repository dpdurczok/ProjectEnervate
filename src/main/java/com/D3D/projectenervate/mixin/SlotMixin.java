package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(
            method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void projectenervate$mergeAdaptiveStackOnSafeInsert(
            ItemStack incoming,
            int amount,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (incoming.isEmpty() || amount <= 0) {
            return;
        }

        // safeInsert is also a movement path. Preserve/rebalance existing state only.
        Slot slot = (Slot) (Object) this;

        int moved = AdaptiveEmcHelper.mergeIntoSlot(slot, incoming, amount);

        if (moved > 0) {
            cir.setReturnValue(incoming);
        }
    }
}