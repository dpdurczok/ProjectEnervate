package com.D3D.projectenervate.celestial;

import com.D3D.projectenervate.emc.ResourceCourseManager;
import com.D3D.projectenervate.menu.CelestialMappingMenu;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public final class CelestialMappingData {
    private static final Map<String, String> CELESTIAL_NAMES = new LinkedHashMap<>();

    static {
        CELESTIAL_NAMES.put("minecraft:coal", "Carbona");
        CELESTIAL_NAMES.put("minecraft:raw_iron", "Ferrum");
        CELESTIAL_NAMES.put("minecraft:raw_copper", "Cupria");
        CELESTIAL_NAMES.put("minecraft:raw_gold", "Aurum");
        CELESTIAL_NAMES.put("minecraft:gold_nugget", "Aurum");
        CELESTIAL_NAMES.put("minecraft:diamond", "Adamantis");
        CELESTIAL_NAMES.put("minecraft:emerald", "Smaragda");
        CELESTIAL_NAMES.put("minecraft:lapis_lazuli", "Lazula");
        CELESTIAL_NAMES.put("minecraft:redstone", "Rubra");
        CELESTIAL_NAMES.put("minecraft:quartz", "Quartzia");
        CELESTIAL_NAMES.put("minecraft:glowstone_dust", "Lumina");
        CELESTIAL_NAMES.put("minecraft:amethyst_shard", "Amethys");
        CELESTIAL_NAMES.put("minecraft:ancient_debris", "Nethera");
    }

    private CelestialMappingData() {
    }

    public static List<CelestialMappingMenu.CelestialBodyView> build(ServerLevel level) {
        Map<ResourceLocation, BigDecimal> multipliers = ResourceCourseManager.currentMultipliers(level);
        Map<String, BodyAccumulator> grouped = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, BigDecimal> entry : multipliers.entrySet()) {
            ResourceLocation id = entry.getKey();
            String idText = id.toString();
            String celestialName = CELESTIAL_NAMES.get(idText);

            if (celestialName == null) {
                continue;
            }

            grouped.computeIfAbsent(celestialName, ignored -> new BodyAccumulator(idText, celestialName))
                    .add(entry.getValue());
        }

        List<CelestialMappingMenu.CelestialBodyView> result = new ArrayList<>();

        for (BodyAccumulator accumulator : grouped.values()) {
            result.add(accumulator.toView());
        }

        result.sort(Comparator.comparing(CelestialMappingMenu.CelestialBodyView::celestialName)
                .thenComparing(CelestialMappingMenu.CelestialBodyView::resourceId));
        return result;
    }

    private static final class BodyAccumulator {
        private final String resourceId;
        private final String celestialName;
        private double totalMultiplier;
        private int count;

        private BodyAccumulator(String resourceId, String celestialName) {
            this.resourceId = resourceId;
            this.celestialName = celestialName;
        }

        private void add(BigDecimal multiplier) {
            if (multiplier == null) {
                return;
            }

            totalMultiplier += multiplier.doubleValue();
            count++;
        }

        private CelestialMappingMenu.CelestialBodyView toView() {
            double multiplier = count <= 0 ? 1.0D : totalMultiplier / count;
            return new CelestialMappingMenu.CelestialBodyView(resourceId, celestialName, multiplier);
        }
    }
}
