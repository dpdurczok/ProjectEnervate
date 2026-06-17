package com.D3D.projectenervate.network;

import com.D3D.projectenervate.ProjectEnervate;
import com.D3D.projectenervate.menu.EmcAssignmentMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EmcAssignmentApplyPayload(String value) implements CustomPacketPayload {
    public static final Type<EmcAssignmentApplyPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ProjectEnervate.MOD_ID, "emc_assignment_apply")
    );

    public static final StreamCodec<ByteBuf, EmcAssignmentApplyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            EmcAssignmentApplyPayload::value,
            EmcAssignmentApplyPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EmcAssignmentApplyPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (player.containerMenu instanceof EmcAssignmentMenu menu) {
            menu.applyFromClient(payload.value(), player);
        }
    }
}
