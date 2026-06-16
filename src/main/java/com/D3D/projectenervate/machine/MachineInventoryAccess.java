package com.D3D.projectenervate.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public interface MachineInventoryAccess {
    int getSlots();

    ItemStack getStackInSlotCopy(int slot);

    void setStackInSlot(int slot, ItemStack stack);

    static @Nullable MachineInventoryAccess create(BlockEntity blockEntity) {
        if (blockEntity.isRemoved()) {
            return null;
        }

        if (blockEntity instanceof Container container && container.getContainerSize() > 0) {
            return new ContainerAccess(container);
        }

        Level level = blockEntity.getLevel();

        if (level == null) {
            return null;
        }

        IItemHandlerModifiable nullSideHandler = getModifiableHandler(level, blockEntity, null);

        if (nullSideHandler != null && nullSideHandler.getSlots() > 0) {
            return new ItemHandlerAccess(nullSideHandler);
        }

        IItemHandlerModifiable bestHandler = null;
        int bestSlots = 0;

        for (Direction direction : Direction.values()) {
            IItemHandlerModifiable candidate = getModifiableHandler(level, blockEntity, direction);

            if (candidate != null && candidate.getSlots() > bestSlots) {
                bestHandler = candidate;
                bestSlots = candidate.getSlots();
            }
        }

        if (bestHandler == null || bestSlots <= 0) {
            return null;
        }

        return new ItemHandlerAccess(bestHandler);
    }

    private static @Nullable IItemHandlerModifiable getModifiableHandler(
            Level level,
            BlockEntity blockEntity,
            @Nullable Direction side
    ) {
        IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                blockEntity.getBlockPos(),
                blockEntity.getBlockState(),
                blockEntity,
                side
        );

        if (handler instanceof IItemHandlerModifiable modifiable) {
            return modifiable;
        }

        return null;
    }

    final class ContainerAccess implements MachineInventoryAccess {
        private final Container container;

        private ContainerAccess(Container container) {
            this.container = container;
        }

        @Override
        public int getSlots() {
            return container.getContainerSize();
        }

        @Override
        public ItemStack getStackInSlotCopy(int slot) {
            if (slot < 0 || slot >= container.getContainerSize()) {
                return ItemStack.EMPTY;
            }

            return container.getItem(slot).copy();
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 0 || slot >= container.getContainerSize()) {
                return;
            }

            container.setItem(slot, stack);
            container.setChanged();
        }
    }

    final class ItemHandlerAccess implements MachineInventoryAccess {
        private final IItemHandlerModifiable handler;

        private ItemHandlerAccess(IItemHandlerModifiable handler) {
            this.handler = handler;
        }

        @Override
        public int getSlots() {
            return handler.getSlots();
        }

        @Override
        public ItemStack getStackInSlotCopy(int slot) {
            if (slot < 0 || slot >= handler.getSlots()) {
                return ItemStack.EMPTY;
            }

            return handler.getStackInSlot(slot).copy();
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 0 || slot >= handler.getSlots()) {
                return;
            }

            handler.setStackInSlot(slot, stack);
        }
    }
}