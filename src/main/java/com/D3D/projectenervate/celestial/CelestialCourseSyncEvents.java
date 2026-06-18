package com.D3D.projectenervate.celestial;

import com.D3D.projectenervate.menu.CelestialMappingMenu;
import com.D3D.projectenervate.network.CelestialCourseSyncPayload;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CelestialCourseSyncEvents {
    private static final long SYNC_INTERVAL_TICKS = 100L;

    private CelestialCourseSyncEvents() {
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        if (level.getGameTime() % SYNC_INTERVAL_TICKS != 0L) {
            return;
        }

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        String encoded = encode(CelestialMappingData.build(level));
        CelestialCourseSyncPayload payload = new CelestialCourseSyncPayload(encoded, level.getSeed());

        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static String encode(List<CelestialMappingMenu.CelestialBodyView> bodies) {
        StringBuilder builder = new StringBuilder();

        for (CelestialMappingMenu.CelestialBodyView body : bodies) {
            if (body == null) {
                continue;
            }

            builder.append(clean(body.resourceId()))
                    .append('\t')
                    .append(clean(body.celestialName()))
                    .append('\t')
                    .append(body.multiplier())
                    .append('\n');
        }

        return builder.toString();
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
