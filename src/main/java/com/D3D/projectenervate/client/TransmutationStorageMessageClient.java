package com.D3D.projectenervate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class TransmutationStorageMessageClient {
    private static final long FULL_OPACITY_MS = 2_000L;
    private static final long FADE_MS = 900L;
    private static final int TEXT_RGB = 0xFF5555;
    private static final int BACKGROUND_RGB = 0xC6C6C6;

    private static final int EMC_TEXT_X = 6;
    private static final int EMC_VALUE_Y = 102;
    private static final int COVER_WIDTH = 82;
    private static final int COVER_HEIGHT = 10;

    private static String message = "";
    private static long shownAtMs;

    private TransmutationStorageMessageClient() {
    }

    public static void show(String newMessage) {
        if (newMessage == null || newMessage.isBlank()) {
            return;
        }

        message = newMessage;
        shownAtMs = System.currentTimeMillis();
    }

    public static void renderOverEmcNumber(GuiGraphics graphics) {
        if (message.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null || minecraft.font == null) {
            return;
        }

        long ageMs = System.currentTimeMillis() - shownAtMs;
        long totalMs = FULL_OPACITY_MS + FADE_MS;

        if (ageMs >= totalMs) {
            message = "";
            return;
        }

        float alpha = 1.0F;

        if (ageMs > FULL_OPACITY_MS) {
            alpha = 1.0F - ((float) (ageMs - FULL_OPACITY_MS) / (float) FADE_MS);
        }

        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        int backgroundColor = (alphaByte << 24) | BACKGROUND_RGB;
        int textColor = (alphaByte << 24) | TEXT_RGB;

        Font font = minecraft.font;
        int textWidth = font.width(message);
        int x = EMC_TEXT_X;

        if (textWidth > COVER_WIDTH) {
            x = EMC_TEXT_X - Math.min(6, textWidth - COVER_WIDTH);
        }

        graphics.fill(EMC_TEXT_X - 1, EMC_VALUE_Y - 1, EMC_TEXT_X + COVER_WIDTH, EMC_VALUE_Y + COVER_HEIGHT, backgroundColor);
        graphics.drawString(font, Component.literal(message), x, EMC_VALUE_Y, textColor, false);
    }
}
