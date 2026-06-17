package com.D3D.projectenervate.client;

import com.D3D.projectenervate.menu.EmcAssignmentMenu;
import com.D3D.projectenervate.network.EmcAssignmentApplyPayload;
import java.math.BigInteger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class EmcAssignmentScreen extends AbstractContainerScreen<EmcAssignmentMenu> {
    private EditBox emcInput;
    private Button applyButton;
    private String statusText = "";

    public EmcAssignmentScreen(EmcAssignmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 198;
        inventoryLabelY = 96;
    }

    @Override
    protected void init() {
        super.init();

        emcInput = new EditBox(font, leftPos + 56, topPos + 38, 64, 18, Component.literal("EMC"));
        emcInput.setMaxLength(32);
        emcInput.setFilter(this::isValidNumericDraft);
        emcInput.setResponder(value -> updateButtonText());
        addRenderableWidget(emcInput);

        applyButton = Button.builder(Component.literal("☐ Apply"), button -> applyClicked())
                .bounds(leftPos + 56, topPos + 62, 64, 20)
                .build();
        addRenderableWidget(applyButton);

        updateButtonText();
    }

    private boolean isValidNumericDraft(String value) {
        return value.isEmpty() || value.matches("[0-9]{0,18}(\\.[0-9]{0,8})?");
    }

    private void updateButtonText() {
        if (applyButton == null || emcInput == null) {
            return;
        }

        boolean valid = menu.canApply(emcInput.getValue());
        applyButton.setMessage(Component.literal(valid ? "☑ Apply" : "☐ Apply"));
        applyButton.active = valid;
    }

    private void applyClicked() {
        String value = emcInput.getValue();
        if (!menu.canApply(value)) {
            statusText = menu.getFailureReason(value);
            updateButtonText();
            return;
        }

        statusText = "request sent";
        PacketDistributor.sendToServer(new EmcAssignmentApplyPayload(value));
        updateButtonText();
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (applyButton != null
                && !applyButton.active
                && mouseX >= applyButton.getX()
                && mouseY >= applyButton.getY()
                && mouseX < applyButton.getX() + applyButton.getWidth()
                && mouseY < applyButton.getY() + applyButton.getHeight()) {
            statusText = menu.getFailureReason(emcInput == null ? "" : emcInput.getValue());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateButtonText();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1B171F);
        guiGraphics.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFF2A2330);

        drawSlot(guiGraphics, x + 25, y + 35);
        drawSlot(guiGraphics, x + 133, y + 35);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(guiGraphics, x + 7 + column * 18, y + 107 + row * 18);
            }
        }

        for (int column = 0; column < 9; column++) {
            drawSlot(guiGraphics, x + 7 + column * 18, y + 165);
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF0E0E10);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF3A3340);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 7, 0xE6E6E6, false);
        guiGraphics.drawString(font, Component.literal("Klein Star"), 11, 25, 0xC8C8C8, false);
        guiGraphics.drawString(font, Component.literal("Item"), 133, 25, 0xC8C8C8, false);
        guiGraphics.drawString(font, Component.literal("EMC value"), 56, 28, 0xC8C8C8, false);

        BigInteger cost = menu.getCost(emcInput == null ? "" : emcInput.getValue());
        String costText = "Cost: " + cost + " EMC";
        guiGraphics.drawString(font, Component.literal(costText), 8, 86, 0xF0DFA0, false);

        String storedText = "Star: " + menu.getStoredStarEmc() + " EMC";
        guiGraphics.drawString(font, Component.literal(storedText), 92, 86, 0xAFC8F0, false);

        if (!statusText.isEmpty()) {
            guiGraphics.drawString(font, Component.literal(statusText), 8, 96, 0xFF7777, false);
        }

        guiGraphics.drawString(font, Component.literal("emc cant be reasigned, choose wisely"), 8, 194 - font.lineHeight, 0xB8B8B8, false);
    }
}
