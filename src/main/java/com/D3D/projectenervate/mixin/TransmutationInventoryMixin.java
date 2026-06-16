package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.api.ProjectEnervateTransmutationAccess;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.math.BigInteger;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider.TargetUpdateType;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.utils.MathUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TransmutationInventory.class, remap = false)
public abstract class TransmutationInventoryMixin implements ProjectEnervateTransmutationAccess {

    @Unique
    private static final int PROJECTENERVATE_LOCK_INDEX = 8;

    @Shadow
    @Final
    private IItemHandlerModifiable inputLocks;

    @Shadow
    public abstract void syncChangedSlots(IntList slotsChanged, TargetUpdateType updateTargets);

    @Shadow
    private void updateEmcAndSync(BigInteger emc) {
        throw new AssertionError();
    }

    @Unique
    private IItemEmcHolder projectenervate$getEmcHolder(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        return stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
    }

    /**
     * ProjectE normally returns player EMC plus Klein Star EMC.
     * ProjectEnervate returns Klein Star EMC only.
     */
    @Overwrite
    public BigInteger getAvailableEmc() {
        BigInteger emc = BigInteger.ZERO;

        for (int slotIndex = 0; slotIndex < inputLocks.getSlots(); slotIndex++) {
            if (slotIndex == PROJECTENERVATE_LOCK_INDEX) {
                continue;
            }

            ItemStack stack = inputLocks.getStackInSlot(slotIndex);
            IItemEmcHolder emcHolder = projectenervate$getEmcHolder(stack);

            if (emcHolder != null) {
                emc = emc.add(BigInteger.valueOf(emcHolder.getStoredEmc(stack)));
            }
        }

        return emc;
    }

    /**
     * Same rule as getAvailableEmc(), but clamped to long for display and output checks.
     */
    @Overwrite
    public long getAvailableEmcAsLong() {
        return MathUtils.clampToLong(getAvailableEmc());
    }

    /**
     * Free EMC space inside the inserted left-side EMC-holder items.
     */
    @Override
    public BigInteger projectenervate$getFreeStarEmc() {
        BigInteger freeEmc = BigInteger.ZERO;

        for (int slotIndex = 0; slotIndex < inputLocks.getSlots(); slotIndex++) {
            if (slotIndex == PROJECTENERVATE_LOCK_INDEX) {
                continue;
            }

            ItemStack stack = inputLocks.getStackInSlot(slotIndex);
            IItemEmcHolder emcHolder = projectenervate$getEmcHolder(stack);

            if (emcHolder != null) {
                freeEmc = freeEmc.add(BigInteger.valueOf(emcHolder.getNeededEmc(stack)));
            }
        }

        return freeEmc;
    }

    @Override
    public boolean projectenervate$canAcceptEmc(BigInteger value) {
        if (value.signum() <= 0) {
            return true;
        }

        return projectenervate$getFreeStarEmc().compareTo(value) >= 0;
    }

    /**
     * Add EMC into left-side EMC-holder items only.
     * No leftover EMC is stored globally.
     */
    @Overwrite
    public void addEmc(BigInteger value) {
        if (value.signum() == 0) {
            updateEmcAndSync(BigInteger.ZERO);
            return;
        }

        if (value.signum() < 0) {
            removeEmc(value.negate());
            return;
        }

        IntList changedSlots = new IntArrayList();

        for (int slotIndex = 0; slotIndex < inputLocks.getSlots(); slotIndex++) {
            if (slotIndex == PROJECTENERVATE_LOCK_INDEX) {
                continue;
            }

            if (value.signum() == 0) {
                break;
            }

            ItemStack stack = inputLocks.getStackInSlot(slotIndex);
            IItemEmcHolder emcHolder = projectenervate$getEmcHolder(stack);

            if (emcHolder == null) {
                continue;
            }

            long amountToInsert = MathUtils.clampToLong(value);
            long inserted = emcHolder.insertEmc(stack, amountToInsert, EmcAction.EXECUTE);

            if (inserted > 0) {
                changedSlots.add(slotIndex);
                value = value.subtract(BigInteger.valueOf(inserted));
            }
        }

        syncChangedSlots(changedSlots, TargetUpdateType.ALL);

        // Force ProjectE's player/global EMC pool to stay empty.
        updateEmcAndSync(BigInteger.ZERO);
    }

    /**
     * Remove EMC from left-side EMC-holder items only.
     * Player/global EMC is ignored.
     */
    @Overwrite
    public void removeEmc(BigInteger value) {
        if (value.signum() == 0) {
            updateEmcAndSync(BigInteger.ZERO);
            return;
        }

        if (value.signum() < 0) {
            addEmc(value.negate());
            return;
        }

        BigInteger available = getAvailableEmc();

        if (available.compareTo(value) < 0) {
            updateEmcAndSync(BigInteger.ZERO);
            return;
        }

        IntList changedSlots = new IntArrayList();
        BigInteger remaining = value;

        for (int slotIndex = 0; slotIndex < inputLocks.getSlots(); slotIndex++) {
            if (slotIndex == PROJECTENERVATE_LOCK_INDEX) {
                continue;
            }

            if (remaining.signum() == 0) {
                break;
            }

            ItemStack stack = inputLocks.getStackInSlot(slotIndex);
            IItemEmcHolder emcHolder = projectenervate$getEmcHolder(stack);

            if (emcHolder == null) {
                continue;
            }

            long amountToExtract = MathUtils.clampToLong(remaining);
            long extracted = emcHolder.extractEmc(stack, amountToExtract, EmcAction.EXECUTE);

            if (extracted > 0) {
                changedSlots.add(slotIndex);
                remaining = remaining.subtract(BigInteger.valueOf(extracted));
            }
        }

        syncChangedSlots(changedSlots, TargetUpdateType.ALL);

        // Force ProjectE's player/global EMC pool to stay empty.
        updateEmcAndSync(BigInteger.ZERO);
    }
}