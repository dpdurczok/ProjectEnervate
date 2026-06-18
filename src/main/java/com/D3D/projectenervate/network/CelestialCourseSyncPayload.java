package com.D3D.projectenervate.network;

import com.D3D.projectenervate.ProjectEnervate;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Method;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CelestialCourseSyncPayload(String encodedBodies, long worldSeed) implements CustomPacketPayload {
    public static final Type<CelestialCourseSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ProjectEnervate.MOD_ID, "celestial_course_sync")
    );

    public static final StreamCodec<ByteBuf, CelestialCourseSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            CelestialCourseSyncPayload::encodedBodies,
            ByteBufCodecs.VAR_LONG,
            CelestialCourseSyncPayload::worldSeed,
            CelestialCourseSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CelestialCourseSyncPayload payload, IPayloadContext context) {
        try {
            Class<?> stateClass = Class.forName("com.D3D.projectenervate.client.CelestialClientCourseState");
            Method apply = stateClass.getMethod("applyEncoded", String.class, long.class);
            apply.invoke(null, payload.encodedBodies(), payload.worldSeed());
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
