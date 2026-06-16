package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TransmutationContainer.class, remap = false)
public abstract class TransmutationQuickMoveKnownSourceMixin {
    private static final int PROJECTE_FIRST_OUTPUT_SLOT = 11;
    private static final int PROJECTE_LAST_OUTPUT_SLOT = 26;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void projectenervate$markTransmutationOutputBeforeQuickMove(
            @NotNull Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (slotIndex < PROJECTE_FIRST_OUTPUT_SLOT || slotIndex > PROJECTE_LAST_OUTPUT_SLOT) {
            return;
        }

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }

        Slot slot = menu.slots.get(slotIndex);

        if (!slot.hasItem()) {
            return;
        }

        ProjectEnervateSourceHelper.markKnownIfBaseEmc(
                slot.getItem(),
                ProjectEnervateSourceHelper.SOURCE_TRANSMUTATION
        );
    }
}