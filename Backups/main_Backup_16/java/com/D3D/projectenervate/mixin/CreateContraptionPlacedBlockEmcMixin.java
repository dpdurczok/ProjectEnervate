package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.api.ProjectEnervateCreateContraptionEmcAccess;
import com.D3D.projectenervate.emc.CreateContraptionEmcHelper;
import com.D3D.projectenervate.world.PlacedBlockAdaptiveEmcData;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.Contraption", remap = false)
public abstract class CreateContraptionPlacedBlockEmcMixin implements ProjectEnervateCreateContraptionEmcAccess {
    @Unique
    private final Map<Long, PlacedBlockAdaptiveEmcData.Entry> projectenervate$placedBlockEmcByLocalPos = new HashMap<>();

    @Override
    public Map<Long, PlacedBlockAdaptiveEmcData.Entry> projectenervate$getCreateContraptionPlacedBlockEmc() {
        return projectenervate$placedBlockEmcByLocalPos;
    }

    @Inject(method = "addBlock", at = @At("RETURN"), require = 0)
    private void projectenervate$capturePlacedBlockEmcForContraption(
            Level level,
            BlockPos pos,
            @Coerce Object capturedPair,
            CallbackInfo ci
    ) {
        CreateContraptionEmcHelper.captureFromWorld(this, level, pos);
    }

    @Inject(method = "removeBlocksFromWorld", at = @At("HEAD"), require = 0)
    private void projectenervate$consumePlacedBlockEmcSources(
            Level level,
            BlockPos offset,
            CallbackInfo ci
    ) {
        CreateContraptionEmcHelper.consumeCapturedSourcesFromWorld(this, level, offset);
    }

    @Inject(method = "addBlocksToWorld", at = @At("RETURN"), require = 0)
    private void projectenervate$restorePlacedBlockEmcFromContraption(
            Level level,
            @Coerce Object transform,
            CallbackInfo ci
    ) {
        CreateContraptionEmcHelper.restoreToWorld(this, level, transform);
    }

    @Inject(method = "writeNBT", at = @At("RETURN"), require = 0)
    private void projectenervate$writePlacedBlockEmcToContraptionNbt(
            HolderLookup.Provider registries,
            boolean spawnPacket,
            CallbackInfoReturnable<CompoundTag> cir
    ) {
        CreateContraptionEmcHelper.writeToNbt(this, cir.getReturnValue());
    }

    @Inject(method = "readNBT", at = @At("RETURN"), require = 0)
    private void projectenervate$readPlacedBlockEmcFromContraptionNbt(
            Level level,
            CompoundTag nbt,
            boolean spawnData,
            CallbackInfo ci
    ) {
        CreateContraptionEmcHelper.readFromNbt(this, nbt);
    }
}
