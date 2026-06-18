package com.D3D.projectenervate.network;

import com.D3D.projectenervate.ProjectEnervate;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Method;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TransmutationStorageMessagePayload(String message) implements CustomPacketPayload {
    public static final Type<TransmutationStorageMessagePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ProjectEnervate.MOD_ID, "transmutation_storage_message")
    );

    public static final StreamCodec<ByteBuf, TransmutationStorageMessagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            TransmutationStorageMessagePayload::message,
            TransmutationStorageMessagePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TransmutationStorageMessagePayload payload, IPayloadContext context) {
        try {
            Class<?> clientClass = Class.forName("com.D3D.projectenervate.client.TransmutationStorageMessageClient");
            Method show = clientClass.getMethod("show", String.class);
            show.invoke(null, payload.message());
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
