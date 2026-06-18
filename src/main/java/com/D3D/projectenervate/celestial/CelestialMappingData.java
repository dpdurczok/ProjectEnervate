package com.D3D.projectenervate.celestial;

import com.D3D.projectenervate.emc.ResourceCourseManager;
import com.D3D.projectenervate.menu.CelestialMappingMenu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CelestialMappingData {
    private CelestialMappingData() {
    }

    public static List<CelestialMappingMenu.CelestialBodyView> build(net.minecraft.server.level.ServerLevel level) {
        List<CelestialMappingMenu.CelestialBodyView> result = new ArrayList<>();

        for (ResourceCourseManager.StarCourseView star : ResourceCourseManager.currentStarCourses(level)) {
            result.add(new CelestialMappingMenu.CelestialBodyView(
                    star.index(),
                    String.join(",", star.activeResources()),
                    star.name(),
                    star.multiplier().doubleValue()
            ));
        }

        result.sort(Comparator.comparingInt(CelestialMappingMenu.CelestialBodyView::starId));
        return result;
    }
}
