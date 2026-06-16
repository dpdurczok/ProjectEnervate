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
    private void projectenervate$markCreativeGrantedStackVerified(
            ServerboundSetCreativeModeSlotPacket packet,
            CallbackInfo ci
    ) {
        ItemStack stack = packet.itemStack();

        if (stack.isEmpty()) {
            return;
        }

        // This packet is the actual creative inventory source path. Marking here
        // avoids the old bug where every item received by a creative player was
        // trusted, including machine-generated unknown outputs.
        ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(stack);
    }
}
