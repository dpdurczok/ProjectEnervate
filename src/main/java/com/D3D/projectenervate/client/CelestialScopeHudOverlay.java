package com.D3D.projectenervate.client;

import com.D3D.projectenervate.ProjectEnervate;
import com.D3D.projectenervate.registry.ProjectEnervateItems;
import com.mojang.blaze3d.systems.RenderSystem;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public final class CelestialScopeHudOverlay {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(
            ProjectEnervate.MOD_ID,
            "celestial_scope_tooltip"
    );

    private static final DecimalFormat MULTIPLIER_FORMAT = new DecimalFormat(
            "0.##",
            DecimalFormatSymbols.getInstance(Locale.US)
    );

    private static final int BACKGROUND_COLOR = 0xB0101020;
    private static final int BORDER_COLOR = 0xCC8E78FF;
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int MULTIPLIER_COLOR = 0xFFBCAEFF;

    private CelestialScopeHudOverlay() {
    }

    public static void registerGuiLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, CelestialScopeHudOverlay::renderGuiLayer);
    }

    private static void renderGuiLayer(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        renderScopeTooltip(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    public static void renderAfterSpyglassOverlay(GuiGraphics guiGraphics) {
        renderScopeTooltip(guiGraphics, 0.0F);
    }

    private static void renderScopeTooltip(GuiGraphics guiGraphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (!minecraft.player.isUsingItem() || !minecraft.player.getUseItem().is(ProjectEnervateItems.CELESTIAL_SCOPE.get())) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        Optional<CelestialSkyOverlay.ScopedStarHit> hit = CelestialSkyOverlay.findScopedStarHit(
                minecraft,
                camera,
                width,
                height,
                partialTick
        );

        if (hit.isEmpty()) {
            return;
        }

        drawTooltipSafely(guiGraphics, minecraft.font, hit.get(), width, height);
    }

    private static void drawTooltipSafely(
            GuiGraphics guiGraphics,
            Font font,
            CelestialSkyOverlay.ScopedStarHit hit,
            int width,
            int height
    ) {
        guiGraphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 1000.0F);
        renderStarTooltip(guiGraphics, font, hit, width, height);
        guiGraphics.pose().popPose();

        guiGraphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void renderStarTooltip(
            GuiGraphics guiGraphics,
            Font font,
            CelestialSkyOverlay.ScopedStarHit hit,
            int screenWidth,
            int screenHeight
    ) {
        Component name = Component.literal(hit.body().celestialName());
        Component multiplier = Component.literal("x" + MULTIPLIER_FORMAT.format(hit.body().multiplier()) + " EMC");

        int textWidth = Math.max(font.width(name), font.width(multiplier));
        int boxWidth = textWidth + 10;
        int boxHeight = font.lineHeight * 2 + 8;
        int left = clamp(hit.x() - boxWidth / 2, 3, Math.max(3, screenWidth - boxWidth - 3));
        int top = clamp(hit.y(), 3, Math.max(3, screenHeight - boxHeight - 3));

        guiGraphics.fill(left, top, left + boxWidth, top + boxHeight, BACKGROUND_COLOR);
        guiGraphics.fill(left, top, left + boxWidth, top + 1, BORDER_COLOR);
        guiGraphics.fill(left, top + boxHeight - 1, left + boxWidth, top + boxHeight, BORDER_COLOR);
        guiGraphics.fill(left, top, left + 1, top + boxHeight, BORDER_COLOR);
        guiGraphics.fill(left + boxWidth - 1, top, left + boxWidth, top + boxHeight, BORDER_COLOR);

        guiGraphics.drawString(font, name, left + 5, top + 4, NAME_COLOR, false);
        guiGraphics.drawString(font, multiplier, left + 5, top + 4 + font.lineHeight, MULTIPLIER_COLOR, false);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
