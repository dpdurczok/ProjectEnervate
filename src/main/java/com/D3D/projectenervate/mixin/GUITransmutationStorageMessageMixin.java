package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.client.TransmutationStorageMessageClient;
import moze_intel.projecte.gameObjs.gui.GUITransmutation;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GUITransmutation.class, remap = false)
public abstract class GUITransmutationStorageMessageMixin {

    @Inject(method = "renderLabels", at = @At("RETURN"), require = 0)
    private void projectenervate$renderStorageMessage(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        TransmutationStorageMessageClient.renderOverEmcNumber(graphics);
    }
}
