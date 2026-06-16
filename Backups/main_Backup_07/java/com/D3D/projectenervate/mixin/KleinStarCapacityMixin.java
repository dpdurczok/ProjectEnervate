package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.KleinStarCapacityOverrides;
import moze_intel.projecte.gameObjs.items.KleinStar;
import moze_intel.projecte.gameObjs.items.KleinStar.KleinTier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KleinStar.class, remap = false)
public abstract class KleinStarCapacityMixin {

    @Shadow
    @Final
    public KleinTier tier;

    @Inject(method = "getMaximumEmc", at = @At("RETURN"), cancellable = true)
    private void projectenervate$overrideMaximumEmc(
            ItemStack stack,
            CallbackInfoReturnable<Long> cir
    ) {
        cir.setReturnValue(KleinStarCapacityOverrides.getMaxEmc(tier));
    }

    @Inject(method = "getStoredEmc", at = @At("RETURN"), cancellable = true)
    private void projectenervate$capStoredEmcRead(
            ItemStack stack,
            CallbackInfoReturnable<Long> cir
    ) {
        long stored = cir.getReturnValue();
        long max = KleinStarCapacityOverrides.getMaxEmc(tier);

        if (stored > max) {
            cir.setReturnValue(max);
        }
    }

    @Inject(method = "getWidthForBar", at = @At("HEAD"), cancellable = true)
    private void projectenervate$overrideBarWidth(
            ItemStack stack,
            CallbackInfoReturnable<Float> cir
    ) {
        KleinStar self = (KleinStar) (Object) this;

        long stored = self.getStoredEmc(stack);
        long max = Math.max(1L, KleinStarCapacityOverrides.getMaxEmc(tier));

        if (stored <= 0) {
            cir.setReturnValue(1.0F);
            return;
        }

        cir.setReturnValue((float) (1.0D - Math.min(stored, max) / (double) max));
    }
}