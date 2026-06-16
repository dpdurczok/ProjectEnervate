package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
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

    @Inject(method = "clicked", at = @At("RETURN"))
    private void projectenervate$markProjectECarriedStackKnown(
            int slotId,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        String className = menu.getClass().getName();

        if (!className.startsWith("moze_intel.projecte.")) {
            return;
        }

        ItemStack carried = menu.getCarried();

        ProjectEnervateSourceHelper.markKnownIfBaseEmc(
                carried,
                ProjectEnervateSourceHelper.SOURCE_TRANSMUTATION
        );

        for (Slot slot : menu.slots) {
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