package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.world.PlacedBlockAdaptiveEmcEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateRemoveAdaptiveEmcMixin {

    @Shadow
    protected abstract BlockState asState();

    @Inject(
            method = "onRemove(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V",
            at = @At("RETURN")
    )
    private void projectenervate$scheduleStalePlacedBlockEmcCleanup(
            Level level,
            BlockPos pos,
            BlockState replacementState,
            boolean movedByPiston,
            CallbackInfo ci
    ) {
        PlacedBlockAdaptiveEmcEvents.onTrackedBlockStateRemoved(
                level,
                pos,
                asState(),
                replacementState,
                movedByPiston
        );
    }
}
