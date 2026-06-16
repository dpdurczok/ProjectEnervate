package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class TransmutationMenuKnownSourceMixin {
    private static final int PROJECTE_FIRST_OUTPUT_SLOT = 11;
    private static final int PROJECTE_LAST_OUTPUT_SLOT = 26;

    @Inject(method = "clicked", at = @At("HEAD"))
    private void projectenervate$markClickedProjectEOutputBeforePickup(
            int slotId,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        if (!(menu instanceof TransmutationContainer)) {
            return;
        }

        if (!projectenervate$isProjectEOutputSlot(slotId)) {
            return;
        }

        if (slotId < 0 || slotId >= menu.slots.size()) {
            return;
        }

        Slot slot = menu.slots.get(slotId);

        if (slot.hasItem()) {
            ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(slot.getItem());
        }
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void projectenervate$markProjectEClickOutputsKnown(
            int slotId,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        if (!(menu instanceof TransmutationContainer)) {
            return;
        }

        if (projectenervate$isProjectEOutputSlot(slotId)) {
            ItemStack carried = menu.getCarried();

            if (!carried.isEmpty()) {
                ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(carried);
            }
        }

        projectenervate$markVisibleOutputSlots(menu);
    }

    private static void projectenervate$markVisibleOutputSlots(AbstractContainerMenu menu) {
        for (int i = PROJECTE_FIRST_OUTPUT_SLOT; i <= PROJECTE_LAST_OUTPUT_SLOT; i++) {
            if (i < 0 || i >= menu.slots.size()) {
                continue;
            }

            Slot slot = menu.slots.get(i);

            if (!slot.hasItem()) {
                continue;
            }

            ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(slot.getItem());
        }
    }

    private static boolean projectenervate$isProjectEOutputSlot(int slotId) {
        return slotId >= PROJECTE_FIRST_OUTPUT_SLOT && slotId <= PROJECTE_LAST_OUTPUT_SLOT;
    }
}
