package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.machine.MachineConversionTracker;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntitySetChangedMixin {

    @Inject(method = "setChanged", at = @At("HEAD"))
    private void projectenervate$markMachineInventoryDirty(CallbackInfo ci) {
        MachineConversionTracker.markDirty((BlockEntity) (Object) this);
    }
}