package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.api.ProjectEnervateTransmutationAccess;
import java.math.BigInteger;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotConsume;
import moze_intel.projecte.gameObjs.registries.PEItems;
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

    /**
     * ProjectE normally allows any EMC-valued item.
     * ProjectEnervate allows it only if the inserted Klein Stars have enough free space.
     */
    @Overwrite
    public boolean mayPlace(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Keep ProjectE's Tome of Knowledge behavior.
        if (stack.is(PEItems.TOME_OF_KNOWLEDGE)) {
            return true;
        }

        long sellValue = IEMCProxy.INSTANCE.getSellValue(stack);

        if (sellValue <= 0) {
            return false;
        }

        BigInteger emcToAdd = BigInteger.valueOf(sellValue)
                .multiply(BigInteger.valueOf(stack.getCount()));

        return ((ProjectEnervateTransmutationAccess) inv).projectenervate$canAcceptEmc(emcToAdd);
    }
}