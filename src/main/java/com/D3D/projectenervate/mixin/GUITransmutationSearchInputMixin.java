package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.client.TransmutationSearchHelper;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.gui.GUITransmutation;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GUITransmutation.class, remap = false)
public abstract class GUITransmutationSearchInputMixin {

    private static final int PROJECTE_FIRST_PLAYER_SLOT = 27;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void projectenervate$middleClickInventoryItemIntoSearch(
            double mouseX,
            double mouseY,
            int mouseButton,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            return;
        }

        GUITransmutation screen = (GUITransmutation) (Object) this;
        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) (Object) this;

        if (!(containerScreen.getMenu() instanceof TransmutationContainer menu)) {
            return;
        }

        Slot hoveredSlot = ((AbstractContainerScreenAccessor) this).projectenervate$getHoveredSlot();

        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return;
        }

        int hoveredMenuSlot = menu.slots.indexOf(hoveredSlot);

        if (hoveredMenuSlot < PROJECTE_FIRST_PLAYER_SLOT) {
            return;
        }

        ItemStack stack = hoveredSlot.getItem();

        if (TransmutationSearchHelper.searchForStack(screen, stack)) {
            cir.setReturnValue(true);
        }
    }
}
