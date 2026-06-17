package com.D3D.projectenervate.client;

import com.D3D.projectenervate.menu.CelestialMappingMenu;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

/**
 * Compatibility shim for older local copies that imported the client package.
 * New server-side code should use com.D3D.projectenervate.celestial.CelestialMappingData.
 */
@Deprecated(forRemoval = false)
public final class CelestialMappingData {
    private CelestialMappingData() {
    }

    public static List<CelestialMappingMenu.CelestialBodyView> build(ServerLevel level) {
        return com.D3D.projectenervate.celestial.CelestialMappingData.build(level);
    }
}
