package com.D3D.projectenervate.item;

import com.D3D.projectenervate.celestial.CelestialMappingData;
import com.D3D.projectenervate.menu.CelestialMappingMenu;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class CelestialMappingItem extends Item {
    public CelestialMappingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            List<CelestialMappingMenu.CelestialBodyView> bodies = CelestialMappingData.build(serverLevel);

            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, playerInventory, p) -> new CelestialMappingMenu(containerId, playerInventory, bodies),
                            Component.translatable("container.projectenervate.celestial_mapping")
                    ),
                    buffer -> CelestialMappingMenu.writeBodies(buffer, bodies)
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right click to view celestial alignments."));
    }
}
