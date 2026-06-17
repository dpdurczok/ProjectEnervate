package com.D3D.projectenervate.menu;

import com.D3D.projectenervate.blockentity.EmcAssignmentStationBlockEntity;
import com.D3D.projectenervate.registry.ProjectEnervateBlocks;
import com.D3D.projectenervate.registry.ProjectEnervateMenus;
import com.D3D.projectenervate.station.EmcAssignmentHelper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EmcAssignmentMenu extends AbstractContainerMenu {
    public static final int STAR_SLOT = 0;
    public static final int ITEM_SLOT = 1;
    private static final int PLAYER_INVENTORY_START = 2;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final EmcAssignmentStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final ItemStackHandler fallbackInventory;

    public EmcAssignmentMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getClientStation(playerInventory, extraData));
    }

    public EmcAssignmentMenu(int containerId, Inventory playerInventory, EmcAssignmentStationBlockEntity station) {
        super(ProjectEnervateMenus.EMC_ASSIGNMENT_STATION.get(), containerId);
        this.station = station;
        this.access = station == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        this.fallbackInventory = station == null ? new ItemStackHandler(EmcAssignmentStationBlockEntity.SLOT_COUNT) : null;

        ItemStackHandler stationInventory = station == null ? fallbackInventory : station.getInventory();

        addSlot(new SlotItemHandler(stationInventory, EmcAssignmentStationBlockEntity.STAR_SLOT, 26, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EmcAssignmentHelper.isEmcHolder(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addSlot(new SlotItemHandler(stationInventory, EmcAssignmentStationBlockEntity.ITEM_SLOT, 134, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !EmcAssignmentHelper.isEmcHolder(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private static EmcAssignmentStationBlockEntity getClientStation(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        Level level = playerInventory.player.level();

        if (level.getBlockEntity(pos) instanceof EmcAssignmentStationBlockEntity station) {
            return station;
        }

        return null;
    }

    public ItemStack getStarStack() {
        return getSlot(STAR_SLOT).getItem();
    }

    public ItemStack getTargetStack() {
        return getSlot(ITEM_SLOT).getItem();
    }

    public BigInteger getCost(String rawValue) {
        Optional<BigDecimal> parsed = EmcAssignmentHelper.parseValue(rawValue);

        if (parsed.isEmpty() || getTargetStack().isEmpty()) {
            return BigInteger.ZERO;
        }

        return EmcAssignmentHelper.getCost(parsed.get(), 1);
    }

    public long getStoredStarEmc() {
        return EmcAssignmentHelper.getStoredEmc(getStarStack());
    }

    public String getFailureReason(String rawValue) {
        return EmcAssignmentHelper.getFailureReason(getStarStack(), getTargetStack(), rawValue);
    }

    public boolean canApply(String rawValue) {
        return EmcAssignmentHelper.canApply(getStarStack(), getTargetStack(), rawValue);
    }

    public boolean applyFromClient(String rawValue, ServerPlayer player) {
        if (rawValue == null || rawValue.length() > 64) {
            player.displayClientMessage(Component.literal("input emc amount"), false);
            return false;
        }

        String failureReason = getFailureReason(rawValue);

        if (!failureReason.isEmpty()) {
            player.displayClientMessage(Component.literal(failureReason), false);
            return false;
        }

        boolean applied = EmcAssignmentHelper.apply(player, getStarStack(), getTargetStack(), rawValue);

        if (!applied) {
            player.displayClientMessage(Component.literal("not enough emc in the Klein star"), false);
            return false;
        }

        if (station != null) {
            station.setChanged();
        }

        getSlot(STAR_SLOT).setChanged();
        getSlot(ITEM_SLOT).setChanged();
        broadcastChanges();
        player.displayClientMessage(Component.literal("EMC assigned. This item cannot be reassigned."), false);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ProjectEnervateBlocks.EMC_ASSIGNMENT_STATION.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (EmcAssignmentHelper.isEmcHolder(stack)) {
            if (!moveItemStackTo(stack, STAR_SLOT, STAR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, ITEM_SLOT, ITEM_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 108 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 166));
        }
    }
}
