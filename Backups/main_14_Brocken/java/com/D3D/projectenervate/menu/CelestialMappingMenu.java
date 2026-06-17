package com.D3D.projectenervate.menu;

import com.D3D.projectenervate.registry.ProjectEnervateMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class CelestialMappingMenu extends AbstractContainerMenu {
    public CelestialMappingMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    public CelestialMappingMenu(int containerId, Inventory playerInventory) {
        super(ProjectEnervateMenus.CELESTIAL_MAPPING.get(), containerId);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
