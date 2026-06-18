package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.ProjectEnervateConfig;
import com.D3D.projectenervate.network.TransmutationStorageMessagePayload;
import com.D3D.projectenervate.api.ProjectEnervateTransmutationAccess;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.math.BigInteger;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider.TargetUpdateType;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.PlayerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;
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
    @Final
    public IKnowledgeProvider provider;

    @Shadow
    public net.minecraft.world.entity.player.Player player;

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
     * ProjectEnervate returns Klein Star EMC only while capMaxEmcToKleinStars is enabled.
     */
    @Overwrite
    public BigInteger getAvailableEmc() {
        BigInteger emc = ProjectEnervateConfig.capMaxEmcToKleinStars()
                ? BigInteger.ZERO
                : provider.getEmc();

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
     * Free EMC space inside inserted EMC-holder items while capped mode is enabled.
     * In normal ProjectE mode the player/global EMC pool is effectively unbounded, so this
     * returns a very large value to avoid blocking ProjectEnervate burn checks.
     */
    @Override
    public BigInteger projectenervate$getFreeStarEmc() {
        if (!ProjectEnervateConfig.capMaxEmcToKleinStars()) {
            return BigInteger.valueOf(Long.MAX_VALUE);
        }

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
        if (!ProjectEnervateConfig.capMaxEmcToKleinStars()) {
            return true;
        }

        if (value.signum() <= 0) {
            return true;
        }

        return projectenervate$getFreeStarEmc().compareTo(value) >= 0;
    }

    @Override
    public boolean projectenervate$canStoreEmcHolder(ItemStack stack) {
        if (projectenervate$getEmcHolder(stack) == null) {
            return false;
        }

        for (int slotIndex = 0; slotIndex < inputLocks.getSlots(); slotIndex++) {
            if (slotIndex == PROJECTENERVATE_LOCK_INDEX) {
                continue;
            }

            ItemStack remainder = inputLocks.insertItem(slotIndex, stack.copy(), true);

            if (remainder.getCount() < stack.getCount()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean projectenervate$hasAnyEmcHolder() {
        for (int slotIndex = 0; slotIndex < inputLocks.getSlots(); slotIndex++) {
            if (slotIndex == PROJECTENERVATE_LOCK_INDEX) {
                continue;
            }

            if (projectenervate$getEmcHolder(inputLocks.getStackInSlot(slotIndex)) != null) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void projectenervate$showStorageMessage(String message) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PacketDistributor.sendToPlayer(serverPlayer, new TransmutationStorageMessagePayload(message));
    }

    /**
     * Capped mode: add EMC into left-side EMC-holder items only.
     * Normal mode: use ProjectE's provider/global EMC pool after filling EMC holders.
     */
    @Overwrite
    public void addEmc(BigInteger value) {
        if (value.signum() == 0) {
            if (ProjectEnervateConfig.capMaxEmcToKleinStars()) {
                updateEmcAndSync(BigInteger.ZERO);
            }
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

        if (ProjectEnervateConfig.capMaxEmcToKleinStars()) {
            syncChangedSlots(changedSlots, TargetUpdateType.ALL);
            updateEmcAndSync(BigInteger.ZERO);
            return;
        }

        syncChangedSlots(changedSlots, value.signum() == 0 ? TargetUpdateType.ALL : TargetUpdateType.NONE);

        if (value.signum() > 0) {
            projectenervate$updateProviderEmc(provider.getEmc().add(value));
        }
    }

    /**
     * Capped mode: remove EMC from left-side EMC-holder items only.
     * Normal mode: use ProjectE's provider/global EMC pool first, then EMC holders.
     */
    @Overwrite
    public void removeEmc(BigInteger value) {
        if (value.signum() == 0) {
            if (ProjectEnervateConfig.capMaxEmcToKleinStars()) {
                updateEmcAndSync(BigInteger.ZERO);
            }
            return;
        }

        if (value.signum() < 0) {
            addEmc(value.negate());
            return;
        }

        if (ProjectEnervateConfig.capMaxEmcToKleinStars()) {
            BigInteger available = getAvailableEmc();

            if (available.compareTo(value) < 0) {
                updateEmcAndSync(BigInteger.ZERO);
                return;
            }

            IntList changedSlots = projectenervate$extractFromHolders(value);
            syncChangedSlots(changedSlots, TargetUpdateType.ALL);
            updateEmcAndSync(BigInteger.ZERO);
            return;
        }

        BigInteger currentEmc = provider.getEmc();

        if (value.compareTo(currentEmc) <= 0) {
            projectenervate$updateProviderEmc(currentEmc.subtract(value));
            return;
        }

        BigInteger toRemoveFromHolders = value.subtract(currentEmc);
        IntList changedSlots = projectenervate$extractFromHolders(toRemoveFromHolders);
        syncChangedSlots(changedSlots, TargetUpdateType.NONE);
        projectenervate$updateProviderEmc(BigInteger.ZERO);
    }

    @Unique
    private IntList projectenervate$extractFromHolders(BigInteger value) {
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

        return changedSlots;
    }

    @Unique
    private void projectenervate$updateProviderEmc(BigInteger emc) {
        if (emc.signum() < 0) {
            emc = BigInteger.ZERO;
        }

        provider.setEmc(emc);
        provider.syncEmc((ServerPlayer) player);
        PlayerHelper.updateScore((ServerPlayer) player, PlayerHelper.SCOREBOARD_EMC, emc);
    }
}
