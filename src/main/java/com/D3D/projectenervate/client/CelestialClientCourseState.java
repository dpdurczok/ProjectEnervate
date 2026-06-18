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

            String[] parts = line.split("\\t", 4);
            if (parts.length != 4) {
                continue;
            }

            try {
                parsed.add(new CelestialMappingMenu.CelestialBodyView(
                        Integer.parseInt(parts[0]),
                        parts[1],
                        parts[2],
                        Double.parseDouble(parts[3])
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

            builder.append(body.starId())
                    .append('\t')
                    .append(clean(body.resourceId()))
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
                new CelestialMappingMenu.CelestialBodyView(0, "minecraft:coal", "Carbona", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(1, "minecraft:raw_iron", "Ferrum", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(2, "minecraft:raw_copper", "Cupria", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(3, "minecraft:raw_gold,minecraft:gold_nugget", "Aurum", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(4, "minecraft:diamond", "Adamantis", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(5, "minecraft:emerald", "Smaragda", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(6, "minecraft:lapis_lazuli", "Lazula", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(7, "minecraft:redstone", "Rubra", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(8, "minecraft:quartz", "Quartzia", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(9, "minecraft:glowstone_dust", "Lumina", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(10, "minecraft:amethyst_shard", "Amethys", 1.0D),
                new CelestialMappingMenu.CelestialBodyView(11, "minecraft:ancient_debris", "Nethera", 1.0D)
        );
    }
}
