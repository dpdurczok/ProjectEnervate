package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import com.D3D.projectenervate.emc.ResourceCourseManager;
import com.D3D.projectenervate.world.PlacedBlockAdaptiveEmcEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockPopResourceKnownSourceMixin {

    @Inject(
            method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private static void projectenervate$markBlockPopResourceKnown(
            Level level,
            BlockPos pos,
            ItemStack stack,
            CallbackInfo ci
    ) {
        projectenervate$markBlockGeneratedStackKnown(level, pos, stack);
    }

    @Inject(
            method = "popResourceFromFace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private static void projectenervate$markBlockPopResourceFromFaceKnown(
            Level level,
            BlockPos pos,
            Direction direction,
            ItemStack stack,
            CallbackInfo ci
    ) {
        projectenervate$markBlockGeneratedStackKnown(level, pos, stack);
    }

    private static void projectenervate$markBlockGeneratedStackKnown(Level level, BlockPos pos, ItemStack stack) {
        if (level.isClientSide()) {
            return;
        }

        if (stack.isEmpty()) {
            return;
        }

        // This hook only runs for stacks spawned by Block.popResource / popResourceFromFace.
        // That covers non-player world block drops such as explosions, decay, fluids, pistons,
        // and other vanilla block-pop paths without trusting generic item entities, commands,
        // creative inventory, JEI, or machine outputs.
        if (PlacedBlockAdaptiveEmcEvents.applyStoredBlockEmcToPoppedStack(level, pos, stack)) {
            return;
        }

        if (ProjectEnervateSourceHelper.hasProjectEnervateData(stack)) {
            return;
        }

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ResourceCourseManager.applyNaturalCourseOrVerify(serverLevel, stack);
            return;
        }

        ProjectEnervateSourceHelper.markVerifiedIfBaseEmcPreservingExisting(stack);
    }
}
