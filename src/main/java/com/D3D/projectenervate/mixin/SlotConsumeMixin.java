package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.api.ProjectEnervateTransmutationAccess;
import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import com.D3D.projectenervate.emc.TransmutationBurnHelper;
import java.math.BigInteger;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotConsume;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SlotConsume.class, remap = false)
public abstract class SlotConsumeMixin {

    @Shadow
    @Final
    private TransmutationInventory inv;

    @Overwrite
    public boolean mayPlace(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.is(PEItems.TOME_OF_KNOWLEDGE)) {
            return true;
        }

        if (!TransmutationBurnHelper.shouldHandleAsBurnable(stack)) {
            return false;
        }

        BigInteger freeEmc =
                ((ProjectEnervateTransmutationAccess) inv).projectenervate$getFreeStarEmc();

        return AdaptiveEmcHelper.getMaxItemsThatFit(freeEmc, stack) > 0;
    }

    @Overwrite
    public void set(@NotNull ItemStack stack) {
        if (!inv.isServer() || stack.isEmpty()) {
            return;
        }

        if (stack.is(PEItems.TOME_OF_KNOWLEDGE)) {
            inv.handleKnowledge(stack);
            ((Slot) (Object) this).setChanged();
            return;
        }

        int burned = TransmutationBurnHelper.burnFromStack(inv, stack, stack.getCount());

        if (burned > 0) {
            ((Slot) (Object) this).setChanged();
        }
    }
}