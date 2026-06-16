package com.D3D.projectenervate.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class MachineInventorySnapshot {
    private final List<ItemStack> stacks;

    private MachineInventorySnapshot(List<ItemStack> stacks) {
        this.stacks = Collections.unmodifiableList(stacks);
    }

    public static MachineInventorySnapshot capture(MachineInventoryAccess access) {
        List<ItemStack> captured = new ArrayList<>();

        for (int slot = 0; slot < access.getSlots(); slot++) {
            captured.add(access.getStackInSlotCopy(slot));
        }

        return new MachineInventorySnapshot(captured);
    }

    public int size() {
        return stacks.size();
    }

    public ItemStack get(int slot) {
        if (slot < 0 || slot >= stacks.size()) {
            return ItemStack.EMPTY;
        }

        return stacks.get(slot).copy();
    }
}