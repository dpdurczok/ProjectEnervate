package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class TransmutationMenuKnownSourceMixin {
    private static final int PROJECTE_FIRST_OUTPUT_SLOT = 11;
    private static final int PROJECTE_LAST_OUTPUT_SLOT = 26;

    @Inject(method = "clicked", at = @At("RETURN"))
    private void projectenervate$markProjectECarriedStackKnown(
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

        for (int i = PROJECTE_FIRST_OUTPUT_SLOT; i <= PROJECTE_LAST_OUTPUT_SLOT; i++) {
            if (i < 0 || i >= menu.slots.size()) {
                continue;
            }

            Slot slot = menu.slots.get(i);

            if (!slot.hasItem()) {
                continue;
            }

            ProjectEnervateSourceHelper.markKnownIfBaseEmc(
                    slot.getItem(),
                    ProjectEnervateSourceHelper.SOURCE_TRANSMUTATION
            );
        }
    }
}
