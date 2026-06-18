package com.D3D.projectenervate.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ProjectEnervateNetwork {
    private ProjectEnervateNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                EmcAssignmentApplyPayload.TYPE,
                EmcAssignmentApplyPayload.STREAM_CODEC,
                EmcAssignmentApplyPayload::handle
        );

        registrar.playToClient(
                CelestialCourseSyncPayload.TYPE,
                CelestialCourseSyncPayload.STREAM_CODEC,
                CelestialCourseSyncPayload::handle
        );
    }
}
