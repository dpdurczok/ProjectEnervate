package com.D3D.projectenervate.client;

import com.D3D.projectenervate.ProjectEnervate;
import com.D3D.projectenervate.menu.CelestialMappingMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class CelestialMappingScreen extends AbstractContainerScreen<CelestialMappingMenu> {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ProjectEnervate.MOD_ID,
            "textures/gui/celestial_mapping_bg.png"
    );

    private static final ResourceLocation STAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ProjectEnervate.MOD_ID,
            "textures/gui/celestial_star.png"
    );

    private static final ResourceLocation CENTER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ProjectEnervate.MOD_ID,
            "textures/gui/celestial_center.png"
    );

    private static final DecimalFormat DECIMAL = new DecimalFormat("0.00");

    /*
     * Actual background texture canvas size.
     * Your celestial_mapping_bg.png should be 240x320.
     */
    private static final int TEXTURE_WIDTH = 240;
    private static final int TEXTURE_HEIGHT = 320;

    /*
     * Visual GUI size in Minecraft.
     * Change these two values to scale the whole book UI without resizing the PNG.
     *
     * 180x240 keeps the same 3:4 ratio as 240x320.
     */
    private static final int GUI_WIDTH = 225;
    private static final int GUI_HEIGHT = 310;

    /*
     * Padding for the usable star field inside the parchment/background.
     * These are in final drawn GUI pixels, not source texture pixels.
     */
    private static final int ORBIT_SIDE_PADDING = 4;
    private static final int ORBIT_TOP_PADDING = 12;
    private static final int ORBIT_BOTTOM_PADDING = 12;

    /*
     * STAR_FRAME_SIZE is the actual pixel size of one animation frame in celestial_star.png.
     * STAR_DRAW_SIZE is how large it appears in the GUI.
     *
     * For 6 frames:
     * celestial_star.png must be 96x16.
     */
    private static final int STAR_FRAME_SIZE = 16;
    private static final int STAR_DRAW_SIZE = 22;
    private static final int STAR_FRAME_COUNT = 6;
    private static final int STAR_TEXTURE_WIDTH = STAR_FRAME_SIZE * STAR_FRAME_COUNT;
    private static final int STAR_TEXTURE_HEIGHT = STAR_FRAME_SIZE;
    private static final int STAR_FRAME_TICKS = 7;

    /*
     * CENTER_FRAME_SIZE is the actual pixel size of one animation frame in celestial_center.png.
     * CENTER_DRAW_SIZE is how large it appears in the GUI.
     *
     * For 6 frames:
     * celestial_center.png must be 144x24.
     */
    private static final int CENTER_FRAME_SIZE = 24;
    private static final int CENTER_DRAW_SIZE = 30;
    private static final int CENTER_FRAME_COUNT = 6;
    private static final int CENTER_TEXTURE_WIDTH = CENTER_FRAME_SIZE * CENTER_FRAME_COUNT;
    private static final int CENTER_TEXTURE_HEIGHT = CENTER_FRAME_SIZE;
    private static final int CENTER_FRAME_TICKS = 8;

    private static final int HOVER_RADIUS = Math.max(8, STAR_DRAW_SIZE / 2 + 2);

    private static final double MIN_MULTIPLIER = 0.1D;
    private static final double MAX_MULTIPLIER = 2.0D;
    private static final double MIN_RADIUS = 6.0D;

    /*
     * Very slow constant linear speed.
     * Outer stars get lower angular speed so they do not all have the same RPM.
     */
    private static final double LINEAR_ORBIT_SPEED_PIXELS_PER_TICK = 0.001D;

    private final List<RenderedBody> renderedBodies = new ArrayList<>();

    public CelestialMappingScreen(CelestialMappingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
        titleLabelX = 10000;
        titleLabelY = 10000;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 10000;
        titleLabelY = 10000;
        inventoryLabelY = 10000;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderHoveredTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderedBodies.clear();

        int x = leftPos;
        int y = topPos;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawScaledTexture(
                guiGraphics,
                BACKGROUND_TEXTURE,
                x,
                y,
                GUI_WIDTH,
                GUI_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );

        double fieldLeft = x + ORBIT_SIDE_PADDING;
        double fieldRight = x + imageWidth - ORBIT_SIDE_PADDING;
        double fieldTop = y + ORBIT_TOP_PADDING;
        double fieldBottom = y + imageHeight - ORBIT_BOTTOM_PADDING;

        double centerX = (fieldLeft + fieldRight) / 2.0D;
        double centerY = (fieldTop + fieldBottom) / 2.0D;

        double maxOrbitRadius = Math.min(fieldRight - fieldLeft, fieldBottom - fieldTop) / 2.0D
                - Math.max(STAR_DRAW_SIZE, CENTER_DRAW_SIZE) / 2.0D
                - 4.0D;

        maxOrbitRadius = Math.max(MIN_RADIUS, maxOrbitRadius);

        long gameTime = minecraft == null || minecraft.level == null ? 0L : minecraft.level.getGameTime();
        double orbitTime = gameTime + partialTick;

        List<CelestialMappingMenu.CelestialBodyView> bodies = menu.getBodies();

        for (int i = 0; i < bodies.size(); i++) {
            CelestialMappingMenu.CelestialBodyView body = bodies.get(i);

            double radius = radiusForMultiplier(body.multiplier(), i, maxOrbitRadius);
            double baseAngle = ((Math.PI * 2.0D) / Math.max(1, bodies.size())) * i;

            double angularSpeed = LINEAR_ORBIT_SPEED_PIXELS_PER_TICK / Math.max(1.0D, radius);
            double angle = baseAngle + orbitTime * angularSpeed;

            double bodyX = centerX + Math.cos(angle) * radius;
            double bodyY = centerY + Math.sin(angle) * radius;

            double starX = bodyX - STAR_DRAW_SIZE / 2.0D;
            double starY = bodyY - STAR_DRAW_SIZE / 2.0D;

            int starFrame = frameFor(gameTime + i, STAR_FRAME_TICKS, STAR_FRAME_COUNT);

            drawAnimatedSprite(
                    guiGraphics,
                    STAR_TEXTURE,
                    starX,
                    starY,
                    STAR_FRAME_SIZE,
                    STAR_FRAME_SIZE,
                    STAR_DRAW_SIZE,
                    STAR_DRAW_SIZE,
                    starFrame,
                    STAR_TEXTURE_WIDTH,
                    STAR_TEXTURE_HEIGHT
            );

            renderedBodies.add(new RenderedBody(body, bodyX, bodyY));
        }

        int centerFrame = frameFor(gameTime, CENTER_FRAME_TICKS, CENTER_FRAME_COUNT);

        drawAnimatedSprite(
                guiGraphics,
                CENTER_TEXTURE,
                centerX - CENTER_DRAW_SIZE / 2.0D,
                centerY - CENTER_DRAW_SIZE / 2.0D,
                CENTER_FRAME_SIZE,
                CENTER_FRAME_SIZE,
                CENTER_DRAW_SIZE,
                CENTER_DRAW_SIZE,
                centerFrame,
                CENTER_TEXTURE_WIDTH,
                CENTER_TEXTURE_HEIGHT
        );

        RenderSystem.disableBlend();
    }

    private int frameFor(long gameTime, int frameTicks, int frameCount) {
        if (frameCount <= 1) {
            return 0;
        }

        return (int) ((gameTime / Math.max(1, frameTicks)) % frameCount);
    }

    private void drawScaledTexture(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            double x,
            double y,
            int drawWidth,
            int drawHeight,
            int textureWidth,
            int textureHeight
    ) {
        float scaleX = (float) drawWidth / (float) textureWidth;
        float scaleY = (float) drawHeight / (float) textureHeight;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float) x, (float) y, 0.0F);
        guiGraphics.pose().scale(scaleX, scaleY, 1.0F);

        guiGraphics.blit(
                texture,
                0,
                0,
                0,
                0,
                textureWidth,
                textureHeight,
                textureWidth,
                textureHeight
        );

        guiGraphics.pose().popPose();
    }

    private void drawAnimatedSprite(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            double x,
            double y,
            int frameWidth,
            int frameHeight,
            int drawWidth,
            int drawHeight,
            int frame,
            int textureWidth,
            int textureHeight
    ) {
        float scaleX = (float) drawWidth / (float) frameWidth;
        float scaleY = (float) drawHeight / (float) frameHeight;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float) x, (float) y, 0.0F);
        guiGraphics.pose().scale(scaleX, scaleY, 1.0F);

        guiGraphics.blit(
                texture,
                0,
                0,
                frameWidth * frame,
                0,
                frameWidth,
                frameHeight,
                textureWidth,
                textureHeight
        );

        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Intentionally blank. This screen is visual-only until a star is hovered.
    }

    private double radiusForMultiplier(double multiplier, int index, double maxRadius) {
        double normalized = (multiplier - MIN_MULTIPLIER) / (MAX_MULTIPLIER - MIN_MULTIPLIER);
        normalized = Mth.clamp(normalized, 0.0D, 1.0D);

        double radius = Mth.lerp(1.0D - normalized, MIN_RADIUS, maxRadius);

        double laneOffset = ((index % 3) - 1) * 3.0D;
        return Mth.clamp(radius + laneOffset, MIN_RADIUS, maxRadius);
    }

    private void renderHoveredTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (RenderedBody rendered : renderedBodies) {
            double dx = mouseX - rendered.x();
            double dy = mouseY - rendered.y();

            if (dx * dx + dy * dy <= HOVER_RADIUS * HOVER_RADIUS) {
                List<FormattedCharSequence> tooltip = List.of(
                        Component.literal(rendered.body().celestialName()).getVisualOrderText(),
                        Component.literal("x" + DECIMAL.format(rendered.body().multiplier())).getVisualOrderText()
                );

                guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
                return;
            }
        }
    }

    private record RenderedBody(
            CelestialMappingMenu.CelestialBodyView body,
            double x,
            double y
    ) {
    }
}