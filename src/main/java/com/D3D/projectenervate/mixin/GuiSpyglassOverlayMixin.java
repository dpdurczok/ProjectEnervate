package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.client.CelestialScopeHudOverlay;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiSpyglassOverlayMixin {
    @Inject(method = "renderSpyglassOverlay", at = @At("TAIL"))
    private void projectenervate$renderCelestialScopeTooltipAfterSpyglass(
            GuiGraphics guiGraphics,
            float scopeScale,
            CallbackInfo ci
    ) {
        CelestialScopeHudOverlay.renderAfterSpyglassOverlay(guiGraphics);
    }
}
