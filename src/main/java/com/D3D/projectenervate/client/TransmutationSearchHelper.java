package com.D3D.projectenervate.client;

import com.D3D.projectenervate.mixin.GUITransmutationAccessor;
import moze_intel.projecte.gameObjs.gui.GUITransmutation;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.item.ItemStack;

public final class TransmutationSearchHelper {

    private TransmutationSearchHelper() {
    }

    public static boolean searchForStack(GUITransmutation screen, ItemStack stack) {
        if (screen == null || stack.isEmpty()) {
            return false;
        }

        String searchText = stack.getHoverName().getString().trim();

        if (searchText.isEmpty()) {
            return false;
        }

        return setSearchText(screen, searchText);
    }

    public static boolean setSearchText(GUITransmutation screen, String searchText) {
        if (screen == null || searchText == null) {
            return false;
        }

        EditBox textBox = ((GUITransmutationAccessor) screen).projectenervate$getTextBoxFilter();

        if (textBox == null) {
            return false;
        }

        textBox.setValue(searchText);
        textBox.setCursorPosition(searchText.length());
        textBox.setFocused(true);
        return true;
    }
}
