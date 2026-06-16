package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.client.TransmutationSearchHelper;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.client.Minecraft;
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


    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void projectenervate$closeWithNonTypingInventoryKey(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null || minecraft.options == null) {
            return;
        }

        if (!minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return;
        }

        if (projectenervate$isTextTypingKey(keyCode)) {
            return;
        }

        ((GUITransmutation) (Object) this).onClose();
        cir.setReturnValue(true);
    }

    private static boolean projectenervate$isTextTypingKey(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return true;
        }

        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return true;
        }

        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return true;
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE,
                    GLFW.GLFW_KEY_APOSTROPHE,
                    GLFW.GLFW_KEY_COMMA,
                    GLFW.GLFW_KEY_MINUS,
                    GLFW.GLFW_KEY_PERIOD,
                    GLFW.GLFW_KEY_SLASH,
                    GLFW.GLFW_KEY_SEMICOLON,
                    GLFW.GLFW_KEY_EQUAL,
                    GLFW.GLFW_KEY_LEFT_BRACKET,
                    GLFW.GLFW_KEY_BACKSLASH,
                    GLFW.GLFW_KEY_RIGHT_BRACKET,
                    GLFW.GLFW_KEY_KP_DECIMAL,
                    GLFW.GLFW_KEY_KP_DIVIDE,
                    GLFW.GLFW_KEY_KP_MULTIPLY,
                    GLFW.GLFW_KEY_KP_SUBTRACT,
                    GLFW.GLFW_KEY_KP_ADD,
                    GLFW.GLFW_KEY_KP_EQUAL -> true;
            default -> false;
        };
    }

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
