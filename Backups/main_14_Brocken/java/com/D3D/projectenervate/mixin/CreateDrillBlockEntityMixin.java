package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.CreateEmcIntegrationHelper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.drill.DrillBlockEntity", remap = false)
public abstract class CreateDrillBlockEntityMixin {

    @Inject(method = "optimiseCobbleGen", at = @At("HEAD"), require = 0)
    private void projectenervate$pushCreateOptimisedDrillBudget(
            BlockState stateToBreak,
            CallbackInfoReturnable<Boolean> cir
    ) {
        CreateEmcIntegrationHelper.pushBlockBreakBudget(this, stateToBreak);
    }

    @Inject(method = "optimiseCobbleGen", at = @At("RETURN"), require = 0)
    private void projectenervate$popCreateOptimisedDrillBudget(
            BlockState stateToBreak,
            CallbackInfoReturnable<Boolean> cir
    ) {
        CreateEmcIntegrationHelper.popBlockBreakBudget();
    }
}
