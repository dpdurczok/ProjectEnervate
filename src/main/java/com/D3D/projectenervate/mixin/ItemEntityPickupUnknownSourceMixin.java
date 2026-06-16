package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupUnknownSourceMixin {
    private static final int MACHINE_SEARCH_RADIUS = 4;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void projectenervate$unknownSourceBeforePickup(Player player, CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;

        if (!(itemEntity.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();

        if (stack.isEmpty()) {
            return;
        }

        if (ProjectEnervateSourceHelper.hasKnownSource(stack)) {
            return;
        }

        if (isNearLikelyModdedMachine(level, itemEntity.blockPosition())) {
            ProjectEnervateSourceHelper.enforceUnknownMinimum(stack);
            return;
        }

        ProjectEnervateSourceHelper.markKnownIfBaseEmc(
                stack,
                ProjectEnervateSourceHelper.SOURCE_PLAYER_PICKUP
        );
    }

    private static boolean isNearLikelyModdedMachine(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-MACHINE_SEARCH_RADIUS, -MACHINE_SEARCH_RADIUS, -MACHINE_SEARCH_RADIUS),
                center.offset(MACHINE_SEARCH_RADIUS, MACHINE_SEARCH_RADIUS, MACHINE_SEARCH_RADIUS)
        )) {
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            if (isLikelyModdedMachineBlock(state)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isLikelyModdedMachineBlock(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String namespace = id.getNamespace().toLowerCase();
        String path = id.getPath().toLowerCase();

        if (namespace.equals("minecraft")) {
            return false;
        }

        if (isKnownStorageOrTransport(path)) {
            return false;
        }

        return namespace.equals("create")
                || namespace.equals("exdeorum")
                || path.contains("sieve")
                || path.contains("sifter")
                || path.contains("sift")
                || path.contains("mill")
                || path.contains("millstone")
                || path.contains("press")
                || path.contains("crusher")
                || path.contains("crushing")
                || path.contains("grinder")
                || path.contains("grind")
                || path.contains("pulverizer")
                || path.contains("washer")
                || path.contains("washing")
                || path.contains("fan")
                || path.contains("mixer")
                || path.contains("basin")
                || path.contains("depot")
                || path.contains("saw")
                || path.contains("cutter")
                || path.contains("processor")
                || path.contains("machine");
    }

    private static boolean isKnownStorageOrTransport(String path) {
        return path.contains("chest")
                || path.contains("barrel")
                || path.contains("shulker")
                || path.contains("drawer")
                || path.contains("storage")
                || path.contains("vault")
                || path.contains("tank")
                || path.contains("pipe")
                || path.contains("duct")
                || path.contains("cable")
                || path.contains("trash");
    }
}