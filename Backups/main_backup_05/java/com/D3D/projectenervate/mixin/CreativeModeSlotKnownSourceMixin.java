package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeModeSlotKnownSourceMixin {

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"))
    private void projectenervate$markCreativeGrantedStackZero(
            ServerboundSetCreativeModeSlotPacket packet,
            CallbackInfo ci
    ) {
        ItemStack stack = packet.itemStack();

        if (stack.isEmpty()) {
            return;
        }

        // Creative inventory and JEI cheat-mode grants are not tracked conversions.
        // Treat them exactly like other untracked item creation: base-EMC items
        // become zero/corrupted immediately instead of visually looking trusted.
        ProjectEnervateSourceHelper.markZeroIfBaseEmc(stack);
    }
}
