package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.api.ProjectEnervateTransmutationAccess;
import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import com.D3D.projectenervate.emc.TransmutationBurnHelper;
import java.math.BigInteger;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.gui.GUITransmutation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "yalter.mousetweaks.Main", remap = false)
public abstract class MouseTweaksMainMixin {

    private static final int PROJECTE_CONSUME_SLOT = 9;
    private static final int PROJECTE_FIRST_OUTPUT_SLOT = 11;
    private static final int PROJECTE_LAST_OUTPUT_SLOT = 26;
    private static final int PROJECTE_FIRST_PLAYER_SLOT = 27;

    @Inject(method = "onMouseScrolled", at = @At("HEAD"), cancellable = true)
    private static void projectenervate$overrideMouseTweaksScrollInTransmutation(
            Screen screen,
            double mouseX,
            double mouseY,
            double scrollDelta,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(screen instanceof GUITransmutation)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.gameMode == null) {
            cir.setReturnValue(true);
            return;
        }

        if (!(minecraft.player.containerMenu instanceof TransmutationContainer menu)) {
            cir.setReturnValue(true);
            return;
        }

        Slot hoveredSlot = ((AbstractContainerScreenAccessor) (AbstractContainerScreen<?>) screen)
                .projectenervate$getHoveredSlot();

        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            cir.setReturnValue(true);
            return;
        }

        if (!menu.getCarried().isEmpty()) {
            cir.setReturnValue(true);
            return;
        }

        int hoveredMenuSlot = menu.slots.indexOf(hoveredSlot);

        if (hoveredMenuSlot < 0) {
            cir.setReturnValue(true);
            return;
        }

        markAllTransmutationVisibleStacksKnown(menu);

        boolean scrollDown = scrollDelta < 0;

        if (hoveredMenuSlot >= PROJECTE_FIRST_OUTPUT_SLOT && hoveredMenuSlot <= PROJECTE_LAST_OUTPUT_SLOT) {
            if (scrollDown) {
                tryMoveOneOutputToInventory(minecraft, menu, hoveredMenuSlot);
            } else {
                tryBurnOneMatchingInventoryItemFromOutput(minecraft, menu, hoveredMenuSlot);
            }

            markAllTransmutationVisibleStacksKnown(menu);
            cir.setReturnValue(true);
            return;
        }

        if (hoveredMenuSlot >= PROJECTE_FIRST_PLAYER_SLOT) {
            if (scrollDown) {
                tryMoveOneMatchingOutputToInventory(minecraft, menu, hoveredMenuSlot);
            } else {
                tryBurnOneFromPlayerSlot(minecraft, menu, hoveredMenuSlot);
            }

            markAllTransmutationVisibleStacksKnown(menu);
            cir.setReturnValue(true);
            return;
        }

        cir.setReturnValue(true);
    }

    private static void tryBurnOneMatchingInventoryItemFromOutput(
            Minecraft minecraft,
            TransmutationContainer menu,
            int hoveredOutputSlotId
    ) {
        Slot outputSlot = menu.slots.get(hoveredOutputSlotId);

        if (!outputSlot.hasItem()) {
            return;
        }

        markStackKnown(outputSlot.getItem());

        ItemStack outputStack = outputSlot.getItem();

        int matchingInventorySlot = findMatchingInventorySlot(menu, outputStack);

        if (matchingInventorySlot < 0) {
            return;
        }

        tryBurnOneFromPlayerSlot(minecraft, menu, matchingInventorySlot);
    }

    private static int findMatchingInventorySlot(TransmutationContainer menu, ItemStack wantedStack) {
        for (int i = PROJECTE_FIRST_PLAYER_SLOT; i < menu.slots.size(); i++) {
            Slot inventorySlot = menu.slots.get(i);

            if (!inventorySlot.hasItem()) {
                continue;
            }

            ItemStack inventoryStack = inventorySlot.getItem();

            if (AdaptiveEmcHelper.canMergeIgnoringAdaptiveEmc(inventoryStack, wantedStack)) {
                return i;
            }
        }

        return -1;
    }

    private static void tryMoveOneMatchingOutputToInventory(
            Minecraft minecraft,
            TransmutationContainer menu,
            int hoveredInventorySlotId
    ) {
        Slot inventorySlot = menu.slots.get(hoveredInventorySlotId);

        if (!inventorySlot.hasItem()) {
            return;
        }

        ItemStack wantedStack = inventorySlot.getItem();

        int matchingOutputSlot = findMatchingOutputSlot(menu, wantedStack);

        if (matchingOutputSlot < 0) {
            return;
        }

        tryMoveOneOutputToSpecificInventorySlot(
                minecraft,
                menu,
                matchingOutputSlot,
                hoveredInventorySlotId
        );
    }

    private static int findMatchingOutputSlot(TransmutationContainer menu, ItemStack wantedStack) {
        for (int i = PROJECTE_FIRST_OUTPUT_SLOT; i <= PROJECTE_LAST_OUTPUT_SLOT; i++) {
            Slot outputSlot = menu.slots.get(i);

            if (!outputSlot.hasItem()) {
                continue;
            }

            markStackKnown(outputSlot.getItem());

            ItemStack outputStack = outputSlot.getItem();

            if (AdaptiveEmcHelper.canMergeIgnoringAdaptiveEmc(wantedStack, outputStack)) {
                return i;
            }
        }

        return -1;
    }

    private static void tryMoveOneOutputToSpecificInventorySlot(
            Minecraft minecraft,
            TransmutationContainer menu,
            int outputSlotId,
            int targetInventorySlotId
    ) {
        Slot outputSlot = menu.slots.get(outputSlotId);
        Slot targetSlot = menu.slots.get(targetInventorySlotId);

        if (!outputSlot.hasItem()) {
            return;
        }

        markStackKnown(outputSlot.getItem());

        ItemStack outputStack = outputSlot.getItem();

        if (!targetSlot.mayPlace(outputStack)) {
            return;
        }

        click(minecraft, menu, outputSlotId, 1, ClickType.PICKUP);

        if (menu.getCarried().isEmpty()) {
            return;
        }

        click(minecraft, menu, targetInventorySlotId, 1, ClickType.PICKUP);

        if (!menu.getCarried().isEmpty()) {
            int fallbackSlot = findInventoryTargetSlot(menu, menu.getCarried());

            if (fallbackSlot >= 0) {
                click(minecraft, menu, fallbackSlot, 0, ClickType.PICKUP);
            }
        }

        if (!menu.getCarried().isEmpty()) {
            click(minecraft, menu, outputSlotId, 0, ClickType.PICKUP);
        }
    }

    private static void tryMoveOneOutputToInventory(
            Minecraft minecraft,
            TransmutationContainer menu,
            int outputSlotId
    ) {
        Slot outputSlot = menu.slots.get(outputSlotId);

        if (!outputSlot.hasItem()) {
            return;
        }

        markStackKnown(outputSlot.getItem());

        ItemStack outputStack = outputSlot.getItem();

        if (outputStack.isEmpty()) {
            return;
        }

        int targetSlotId = findInventoryTargetSlot(menu, outputStack);

        if (targetSlotId < 0) {
            return;
        }

        Slot targetSlot = menu.slots.get(targetSlotId);
        boolean targetWasEmpty = !targetSlot.hasItem();

        click(minecraft, menu, outputSlotId, 1, ClickType.PICKUP);

        if (menu.getCarried().isEmpty()) {
            return;
        }

        click(minecraft, menu, targetSlotId, targetWasEmpty ? 0 : 1, ClickType.PICKUP);

        if (!menu.getCarried().isEmpty()) {
            click(minecraft, menu, outputSlotId, 0, ClickType.PICKUP);
        }
    }

    private static void tryBurnOneFromPlayerSlot(
            Minecraft minecraft,
            TransmutationContainer menu,
            int playerSlotId
    ) {
        Slot sourceSlot = menu.slots.get(playerSlotId);

        if (!sourceSlot.hasItem()) {
            return;
        }

        ProjectEnervateSourceHelper.enforceUnknownMinimum(sourceSlot.getItem());

        ItemStack sourceStack = sourceSlot.getItem();

        if (sourceStack.isEmpty()) {
            return;
        }

        if (!TransmutationBurnHelper.shouldHandleAsBurnable(sourceStack)) {
            return;
        }

        ProjectEnervateTransmutationAccess access =
                (ProjectEnervateTransmutationAccess) menu.transmutationInventory;

        if (TransmutationBurnHelper.isEmcHolder(sourceStack)
                && access.projectenervate$canStoreEmcHolder(sourceStack)) {
            click(minecraft, menu, playerSlotId, 0, ClickType.QUICK_MOVE);
            return;
        }

        BigInteger freeEmc = access.projectenervate$getFreeStarEmc();

        if (TransmutationBurnHelper.getMaxBurnableItems(freeEmc, sourceStack, 1) <= 0) {
            return;
        }

        click(minecraft, menu, playerSlotId, 0, ClickType.PICKUP);

        if (menu.getCarried().isEmpty()) {
            return;
        }

        click(minecraft, menu, PROJECTE_CONSUME_SLOT, 1, ClickType.PICKUP);

        if (!menu.getCarried().isEmpty()) {
            click(minecraft, menu, playerSlotId, 0, ClickType.PICKUP);
        }
    }

    private static int findInventoryTargetSlot(TransmutationContainer menu, ItemStack stackToInsert) {
        for (int i = PROJECTE_FIRST_PLAYER_SLOT; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);

            if (!slot.hasItem()) {
                continue;
            }

            ItemStack existing = slot.getItem();

            if (!AdaptiveEmcHelper.canMergeIgnoringAdaptiveEmc(existing, stackToInsert)) {
                continue;
            }

            if (existing.getCount() < Math.min(slot.getMaxStackSize(existing), existing.getMaxStackSize())) {
                return i;
            }
        }

        for (int i = PROJECTE_FIRST_PLAYER_SLOT; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);

            if (!slot.hasItem() && slot.mayPlace(stackToInsert)) {
                return i;
            }
        }

        return -1;
    }

    private static void markAllTransmutationVisibleStacksKnown(TransmutationContainer menu) {
        for (int i = PROJECTE_FIRST_OUTPUT_SLOT; i <= PROJECTE_LAST_OUTPUT_SLOT; i++) {
            Slot slot = menu.slots.get(i);

            if (!slot.hasItem()) {
                continue;
            }

            markStackKnown(slot.getItem());
        }
    }

    private static void markStackKnown(ItemStack stack) {
        ProjectEnervateSourceHelper.markKnownIfBaseEmc(
                stack,
                ProjectEnervateSourceHelper.SOURCE_TRANSMUTATION
        );
    }

    private static void click(
            Minecraft minecraft,
            AbstractContainerMenu menu,
            int slotId,
            int button,
            ClickType clickType
    ) {
        Player player = minecraft.player;

        if (player == null || minecraft.gameMode == null) {
            return;
        }

        minecraft.gameMode.handleInventoryMouseClick(
                menu.containerId,
                slotId,
                button,
                clickType,
                player
        );
    }
}
