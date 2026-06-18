package com.D3D.projectenervate.menu;

import com.D3D.projectenervate.registry.ProjectEnervateMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class CelestialMappingMenu extends AbstractContainerMenu {
    private final List<CelestialBodyView> bodies;

    public CelestialMappingMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ProjectEnervateMenus.CELESTIAL_MAPPING.get(), containerId);
        this.bodies = readBodies(buffer);
    }

    public CelestialMappingMenu(int containerId, Inventory inventory, List<CelestialBodyView> bodies) {
        super(ProjectEnervateMenus.CELESTIAL_MAPPING.get(), containerId);
        this.bodies = List.copyOf(bodies);
    }

    public List<CelestialBodyView> getBodies() {
        return bodies;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public static void writeBodies(FriendlyByteBuf buffer, List<CelestialBodyView> bodies) {
        buffer.writeVarInt(bodies.size());

        for (CelestialBodyView body : bodies) {
            buffer.writeVarInt(body.starId());
            buffer.writeUtf(body.resourceId());
            buffer.writeUtf(body.celestialName());
            buffer.writeDouble(body.multiplier());
        }
    }

    private static List<CelestialBodyView> readBodies(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<CelestialBodyView> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            result.add(new CelestialBodyView(
                    buffer.readVarInt(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readDouble()
            ));
        }

        return result;
    }

    public record CelestialBodyView(
            int starId,
            String resourceId,
            String celestialName,
            double multiplier
    ) {
    }
}
