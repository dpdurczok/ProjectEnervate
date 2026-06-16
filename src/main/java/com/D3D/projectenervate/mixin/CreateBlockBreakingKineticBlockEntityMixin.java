package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.CreateEmcIntegrationHelper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity", remap = false)
public abstract class CreateBlockBreakingKineticBlockEntityMixin {

    @Inject(method = "onBlockBroken", at = @At("HEAD"), require = 0)
    private void projectenervate$pushCreateBlockBreakBudget(BlockState stateToBreak, CallbackInfo ci) {
        CreateEmcIntegrationHelper.pushBlockBreakBudget(this, stateToBreak);
    }

    @Inject(method = "onBlockBroken", at = @At("RETURN"), require = 0)
    private void projectenervate$popCreateBlockBreakBudget(BlockState stateToBreak, CallbackInfo ci) {
        CreateEmcIntegrationHelper.popBlockBreakBudget();
    }
}
