package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcConversionHelper;
import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import com.D3D.projectenervate.emc.TransmutationBurnHelper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Unique
    private static final int PROJECTENERVATE_CONSUME_SLOT = 9;

    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Shadow
    public abstract ItemStack getCarried();

    @Shadow
    public abstract void setCarried(ItemStack stack);

    @Shadow
    public abstract void broadcastChanges();

    @Unique
    private boolean projectenervate$adaptiveMenuMerged;

    @Unique
    private BigDecimal projectenervate$pendingStonecutterBudget;

    @Unique
    private ItemStack projectenervate$pendingStonecutterOutput = ItemStack.EMPTY;

    @Inject(
            method = "clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void projectenervate$partialBurnOnConsumeSlotClick(
            int slotIndex,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        if (!((Object) this instanceof TransmutationContainer menu)) {
            return;
        }

        if (slotIndex != PROJECTENERVATE_CONSUME_SLOT) {
            return;
        }

        if (clickType != ClickType.PICKUP) {
            return;
        }

        if (button != 0 && button != 1) {
            return;
        }

        ItemStack carried = getCarried();

        if (carried.isEmpty()) {
            return;
        }

        if (!TransmutationBurnHelper.shouldHandleAsBurnable(carried)) {
            return;
        }

        int requestedCount = button == 0 ? carried.getCount() : 1;

        int burned = TransmutationBurnHelper.burnFromStack(
                menu.transmutationInventory,
                carried,
                requestedCount
        );

        if (burned > 0) {
            setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            broadcastChanges();
        }

        ci.cancel();
    }

    @Inject(
            method = "clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void projectenervate$mergeAdaptiveStacksOnMouseClick(
            int slotIndex,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        if (clickType != ClickType.PICKUP) {
            return;
        }

        if (button != 0 && button != 1) {
            return;
        }

        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return;
        }

        Slot slot = slots.get(slotIndex);

        if (!slot.hasItem()) {
            return;
        }

        ItemStack carried = getCarried();

        if (carried.isEmpty()) {
            return;
        }

        // GUI movement is not an item-creation boundary. Do not mark clean stacks
        // as zero here; untrusted stacks are still constrained at burn/pickup/source boundaries.
        int maxMove = button == 0 ? carried.getCount() : 1;

        int moved = AdaptiveEmcHelper.mergeIntoSlot(slot, carried, maxMove);

        if (moved <= 0) {
            return;
        }

        setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        slot.setChanged();
        broadcastChanges();

        ci.cancel();
    }

    @Inject(
            method = "moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void projectenervate$mergeAdaptiveStacksBeforeMoveItemStackTo(
            ItemStack incoming,
            int startIndex,
            int endIndex,
            boolean reverseDirection,
            CallbackInfoReturnable<Boolean> cir
    ) {
        projectenervate$adaptiveMenuMerged = false;

        if (incoming.isEmpty()) {
            return;
        }

        // Shift-click movement must preserve/rebalance existing economic state,
        // not decide that an unmarked carried/result stack is fake.
        projectenervate$adaptiveMenuMerged = AdaptiveEmcHelper.mergeIntoMenuSlots(
                slots,
                incoming,
                startIndex,
                endIndex,
                reverseDirection
        );

        if (incoming.isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void projectenervate$forceSuccessfulMoveReturnIfAdaptiveMerged(
            ItemStack incoming,
            int startIndex,
            int endIndex,
            boolean reverseDirection,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (projectenervate$adaptiveMenuMerged) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD")
    )
    private void projectenervate$captureStonecutterResultBeforeClick(
            int slotIndex,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        projectenervate$pendingStonecutterBudget = null;
        projectenervate$pendingStonecutterOutput = ItemStack.EMPTY;

        if (!((Object) this instanceof StonecutterMenu)) {
            return;
        }

        if (clickType != ClickType.PICKUP || (button != 0 && button != 1)) {
            return;
        }

        // Vanilla stonecutter layout: slot 0 is input, slot 1 is result.
        if (slotIndex != 1 || slots.size() <= 1) {
            return;
        }

        Slot inputSlot = slots.get(0);
        Slot resultSlot = slots.get(1);

        if (!inputSlot.hasItem() || !resultSlot.hasItem()) {
            return;
        }

        projectenervate$pendingStonecutterBudget = AdaptiveEmcConversionHelper.getOneItemBudget(inputSlot.getItem());
        projectenervate$pendingStonecutterOutput = resultSlot.getItem().copy();
    }

    @Inject(
            method = "clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("RETURN")
    )
    private void projectenervate$applyStonecutterStateToCarriedResult(
            int slotIndex,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        if (projectenervate$pendingStonecutterBudget == null
                || projectenervate$pendingStonecutterOutput.isEmpty()) {
            return;
        }

        try {
            ItemStack carried = getCarried();

            if (carried.isEmpty()) {
                return;
            }

            if (!AdaptiveEmcHelper.canMergeIgnoringProjectEnervateMetadata(
                    carried,
                    projectenervate$pendingStonecutterOutput
            )) {
                return;
            }

            List<ItemStack> outputs = new ArrayList<>(1);
            outputs.add(carried);

            AdaptiveEmcConversionHelper.applyBudgetToOutputsInPlace(
                    projectenervate$pendingStonecutterBudget,
                    outputs
            );

            setCarried(carried);
            broadcastChanges();
        } finally {
            projectenervate$pendingStonecutterBudget = null;
            projectenervate$pendingStonecutterOutput = ItemStack.EMPTY;
        }
    }

}