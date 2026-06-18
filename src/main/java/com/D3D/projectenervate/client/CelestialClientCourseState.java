package com.D3D.projectenervate.client;

import com.D3D.projectenervate.menu.CelestialMappingMenu;
import java.util.ArrayList;
import java.util.List;

public final class CelestialClientCourseState {
    private static volatile List<CelestialMappingMenu.CelestialBodyView> bodies = defaultBodies();
    private static volatile long worldSeed;

    private CelestialClientCourseState() {
    }

    public static List<CelestialMappingMenu.CelestialBodyView> getBodies() {
        return bodies;
    }

    public static long getWorldSeed() {
        return worldSeed;
    }

    public static void applyEncoded(String encoded) {
        applyEncoded(encoded, worldSeed);
    }

    public static void applyEncoded(String encoded, long seed) {
        worldSeed = seed;
        if (encoded == null || encoded.isBlank()) {
            bodies = defaultBodies();
            return;
        }

        List<CelestialMappingMenu.CelestialBodyView> parsed = new ArrayList<>();
        String[] lines = encoded.split("\\n");

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\\t", 3);
            if (parts.length != 3) {
                continue;
            }

            try {
                parsed.add(new CelestialMappingMenu.CelestialBodyView(
                        parts[0],
                        parts[1],
                        Double.parseDouble(parts[2])
                ));
            } catch (NumberFormatException ignored) {
            }
        }

        if (!parsed.isEmpty()) {
            bodies = List.copyOf(parsed);
        }
    }

    public static String encode(List<CelestialMappingMenu.CelestialBodyView> bodies) {
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

    private static List<CelestialMappingMenu.CelestialBodyView> defaultBodies() {
        return List.of(
                new CelestialMappingMenu.CelestialBodyView("minecraft:diamond", "Adamantis", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:amethyst_shard", "Amethys", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:raw_gold", "Aurum", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:coal", "Carbona", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:raw_copper", "Cupria", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:raw_iron", "Ferrum", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:lapis_lazuli", "Lazula", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:glowstone_dust", "Lumina", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:ancient_debris", "Nethera", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:quartz", "Quartzia", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:redstone", "Rubra", 1.0D),
                new CelestialMappingMenu.CelestialBodyView("minecraft:emerald", "Smaragda", 1.0D)
        );
    }
}
