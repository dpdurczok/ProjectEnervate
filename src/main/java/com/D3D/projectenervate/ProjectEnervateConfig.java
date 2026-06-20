package com.D3D.projectenervate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class ProjectEnervateConfig {
    private static final String COMMON_CONFIG_FILE_NAME = "projectenervate-common.toml";
    private static final String STARS_CONFIG_FILE_NAME = "projectenervate-stars.toml";
    private static final String STARS_SECTION = "[stars]";
    private static final String STARS_KEY = "Definitions";

    private static final Object STARS_CACHE_LOCK = new Object();
    private static volatile List<String> cachedStars;
    private static volatile long cachedStarsModifiedTime = Long.MIN_VALUE;
    private static volatile long cachedStarsSize = Long.MIN_VALUE;
    private static volatile long starsRevision;

    private static final List<String> DEFAULT_STARS = List.of(
            "Carbona|minecraft:coal,#c:ores/coal,#c:storage_blocks/coal",
            "Ferrum|minecraft:raw_iron,minecraft:iron_ingot,minecraft:iron_nugget,#c:raw_materials/iron,#c:raw_blocks/iron,#c:ores/iron,#c:ingots/iron,#c:nuggets/iron,#c:dusts/iron,#c:plates/iron,#c:storage_blocks/iron",
            "Cupria|minecraft:raw_copper,minecraft:copper_ingot,#c:raw_materials/copper,#c:raw_blocks/copper,#c:ores/copper,#c:ingots/copper,#c:nuggets/copper,#c:dusts/copper,#c:plates/copper,#c:wires/copper,#c:storage_blocks/copper",
            "Aurum|minecraft:raw_gold,minecraft:gold_ingot,minecraft:gold_nugget,#c:raw_materials/gold,#c:raw_blocks/gold,#c:ores/gold,#c:ingots/gold,#c:nuggets/gold,#c:dusts/gold,#c:plates/gold,#c:wires/gold,#c:storage_blocks/gold",
            "Adamantis|minecraft:diamond,#c:gems/diamond,#c:ores/diamond,#c:dusts/diamond,#c:storage_blocks/diamond",
            "Smaragda|minecraft:emerald,#c:gems/emerald,#c:ores/emerald,#c:storage_blocks/emerald",
            "Lazula|minecraft:lapis_lazuli,#c:gems/lapis,#c:ores/lapis,#c:dusts/lapis,#c:storage_blocks/lapis",
            "Rubra|minecraft:redstone,#c:dusts/redstone,#c:ores/redstone,#c:storage_blocks/redstone",
            "Quartzia|minecraft:quartz,#c:gems/quartz,#c:ores/quartz,#c:dusts/quartz,#c:storage_blocks/quartz",
            "Lumina|minecraft:glowstone_dust,#c:dusts/glowstone,#c:storage_blocks/glowstone",
            "Amethys|minecraft:amethyst_shard,#c:gems/amethyst,#c:storage_blocks/amethyst",
            "Nethera|minecraft:ancient_debris,minecraft:netherite_scrap,minecraft:netherite_ingot,#c:ores/netherite_scrap,#c:ingots/netherite,#c:nuggets/netherite,#c:storage_blocks/netherite",
            "Stannum|#c:raw_materials/tin,#c:raw_blocks/tin,#c:ores/tin,#c:ingots/tin,#c:nuggets/tin,#c:dusts/tin,#c:plates/tin,#c:wires/tin,#c:storage_blocks/tin",
            "Plumbum|#c:raw_materials/lead,#c:raw_blocks/lead,#c:ores/lead,#c:ingots/lead,#c:nuggets/lead,#c:dusts/lead,#c:plates/lead,#c:storage_blocks/lead",
            "Argentum|#c:raw_materials/silver,#c:raw_blocks/silver,#c:ores/silver,#c:ingots/silver,#c:nuggets/silver,#c:dusts/silver,#c:plates/silver,#c:storage_blocks/silver",
            "Niccolum|#c:raw_materials/nickel,#c:raw_blocks/nickel,#c:ores/nickel,#c:ingots/nickel,#c:nuggets/nickel,#c:dusts/nickel,#c:plates/nickel,#c:storage_blocks/nickel",
            "Zincum|#c:raw_materials/zinc,#c:raw_blocks/zinc,#c:ores/zinc,#c:ingots/zinc,#c:nuggets/zinc,#c:dusts/zinc,#c:plates/zinc,#c:storage_blocks/zinc",
            "Alumen|#c:raw_materials/aluminum,#c:raw_materials/aluminium,#c:raw_blocks/aluminum,#c:raw_blocks/aluminium,#c:ores/aluminum,#c:ores/aluminium,#c:ingots/aluminum,#c:ingots/aluminium,#c:nuggets/aluminum,#c:nuggets/aluminium,#c:dusts/aluminum,#c:dusts/aluminium,#c:plates/aluminum,#c:plates/aluminium,#c:storage_blocks/aluminum,#c:storage_blocks/aluminium",
            "Osmium|#c:raw_materials/osmium,#c:raw_blocks/osmium,#c:ores/osmium,#c:ingots/osmium,#c:nuggets/osmium,#c:dusts/osmium,#c:plates/osmium,#c:storage_blocks/osmium",
            "Urania|#c:raw_materials/uranium,#c:raw_blocks/uranium,#c:ores/uranium,#c:ingots/uranium,#c:nuggets/uranium,#c:dusts/uranium,#c:plates/uranium,#c:storage_blocks/uranium",
            "Platina|#c:raw_materials/platinum,#c:raw_blocks/platinum,#c:ores/platinum,#c:ingots/platinum,#c:nuggets/platinum,#c:dusts/platinum,#c:plates/platinum,#c:storage_blocks/platinum",
            "Iridium|#c:raw_materials/iridium,#c:raw_blocks/iridium,#c:ores/iridium,#c:ingots/iridium,#c:nuggets/iridium,#c:dusts/iridium,#c:plates/iridium,#c:storage_blocks/iridium",
            "Cobaltum|#c:raw_materials/cobalt,#c:raw_blocks/cobalt,#c:ores/cobalt,#c:ingots/cobalt,#c:nuggets/cobalt,#c:dusts/cobalt,#c:plates/cobalt,#c:storage_blocks/cobalt",
            "Sulfuris|#c:gems/sulfur,#c:dusts/sulfur,#c:ores/sulfur,#c:storage_blocks/sulfur",
            "Salpetrum|#c:dusts/saltpeter,#c:ores/saltpeter,#c:gems/saltpeter,#c:storage_blocks/saltpeter",
            "Salina|#c:dusts/salt,#c:gems/salt,#c:ores/salt,#c:storage_blocks/salt",
            "Cinnabar|#c:gems/cinnabar,#c:dusts/cinnabar,#c:ores/cinnabar,#c:storage_blocks/cinnabar",
            "Apatite|#c:gems/apatite,#c:dusts/apatite,#c:ores/apatite,#c:storage_blocks/apatite",
            "Ruby|#c:gems/ruby,#c:dusts/ruby,#c:ores/ruby,#c:storage_blocks/ruby",
            "Sapphire|#c:gems/sapphire,#c:dusts/sapphire,#c:ores/sapphire,#c:storage_blocks/sapphire",
            "Peridot|#c:gems/peridot,#c:dusts/peridot,#c:ores/peridot,#c:storage_blocks/peridot",
            "Certus|#c:gems/certus_quartz,#c:gems/charged_certus_quartz,#c:dusts/certus_quartz,#c:ores/certus_quartz,#c:storage_blocks/certus_quartz",
            "Fluorite|#c:gems/fluorite,#c:dusts/fluorite,#c:ores/fluorite,#c:storage_blocks/fluorite",
            "Bronze|#c:raw_materials/bronze,#c:raw_blocks/bronze,#c:ingots/bronze,#c:nuggets/bronze,#c:dusts/bronze,#c:plates/bronze,#c:storage_blocks/bronze",
            "Steel|#c:raw_materials/steel,#c:raw_blocks/steel,#c:ingots/steel,#c:nuggets/steel,#c:dusts/steel,#c:plates/steel,#c:storage_blocks/steel",
            "Brass|#c:ingots/brass,#c:nuggets/brass,#c:dusts/brass,#c:plates/brass,#c:storage_blocks/brass",
            "Electrum|#c:ingots/electrum,#c:nuggets/electrum,#c:dusts/electrum,#c:plates/electrum,#c:storage_blocks/electrum",
            "Invar|#c:ingots/invar,#c:nuggets/invar,#c:dusts/invar,#c:plates/invar,#c:storage_blocks/invar",
            "Constantan|#c:ingots/constantan,#c:nuggets/constantan,#c:dusts/constantan,#c:plates/constantan,#c:storage_blocks/constantan"
    );

    public static final ProjectEnervateConfig CONFIG;
    public static final ModConfigSpec SPEC;

    static {
        Pair<ProjectEnervateConfig, ModConfigSpec> pair = new ModConfigSpec.Builder()
                .configure(ProjectEnervateConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    private final ModConfigSpec.BooleanValue voidUnknownSources;
    private final ModConfigSpec.BooleanValue adaptiveEmc;
    private final ModConfigSpec.BooleanValue disableCollectors;
    private final ModConfigSpec.BooleanValue capMaxEmcToKleinStars;
    private final ModConfigSpec.BooleanValue enableRandomCourses;
    private final ModConfigSpec.BooleanValue chooseBaseEmcIfLower;
    private final ModConfigSpec.BooleanValue backtrackMissingBaseEmc;

    private ProjectEnervateConfig(ModConfigSpec.Builder builder) {
        builder.push("economy");

        voidUnknownSources = builder
                .comment("If true, untracked vanilla/ProjectE EMC items are marked as VOID and burn for 0 EMC.",
                        "If false, unknown source items keep their normal base EMC value.")
                .translation("projectenervate.configuration.void_unknown_sources")
                .define("voidUnknownSources", true);

        adaptiveEmc = builder
                .comment("If true, outputs created from tracked conversions are capped to the EMC budget of their inputs.",
                        "If false, ProjectEnervate conversion capping is disabled and outputs use normal base EMC behavior.")
                .translation("projectenervate.configuration.adaptive_emc")
                .define("adaptiveEmc", true);

        disableCollectors = builder
                .comment("If true, ProjectE Energy Collectors are disabled and their recipes are hidden/blocked.",
                        "If false, ProjectE collectors can be crafted and used normally.")
                .translation("projectenervate.configuration.disable_collectors")
                .define("disableCollectors", true);

        capMaxEmcToKleinStars = builder
                .comment("If true, ProjectE transmutation EMC storage is limited to inserted Klein Star/EMC-holder capacity.",
                        "If false, ProjectE's normal player/global EMC pool behavior is restored.")
                .translation("projectenervate.configuration.cap_max_emc_to_klein_stars")
                .define("capMaxEmcToKleinStars", true);

        enableRandomCourses = builder
                .comment("If true, natural block drops for configured star resources receive cycling celestial-course EMC multipliers.",
                        "This only affects clean naturally mined/generated block drops. Placed blocks with ProjectEnervate EMC data keep their stored value.",
                        "Star definitions are stored in config/" + STARS_CONFIG_FILE_NAME + " because commands must be able to rewrite them cleanly at runtime.")
                .translation("projectenervate.configuration.enable_random_courses")
                .define("EnableRandomCourses", true);

        chooseBaseEmcIfLower = builder
                .comment("If true, adaptive outputs whose calculated EMC is above the ProjectE base value are reduced to the lower base EMC.",
                        "If false, adaptive outputs keep their calculated EMC even when it is above base, allowing high-course resource value to survive crafting, smelting, trades, block drops, and other tracked conversions.")
                .translation("projectenervate.configuration.choose_base_emc_if_lower")
                .define("ChooseBaseEMCIfLower", false);

        backtrackMissingBaseEmc = builder
                .comment("If true, ProjectEnervate infers missing ProjectE base EMC values after ProjectE's normal mapper pass.",
                        "It backtracks from known-valued recipe outputs and block drops to missing-valued ingredients or block items.",
                        "ProjectE base EMC is whole-number only, so fractional inferred values are rounded up to at least 1 EMC.")
                .translation("projectenervate.configuration.backtrack_missing_base_emc")
                .define("BacktrackMissingBaseEMC", true);

        builder.pop();
    }

    public static boolean voidUnknownSources() {
        return CONFIG.voidUnknownSources.get();
    }

    public static boolean adaptiveEmc() {
        return CONFIG.adaptiveEmc.get();
    }

    public static boolean disableCollectors() {
        return CONFIG.disableCollectors.get();
    }

    public static boolean capMaxEmcToKleinStars() {
        return CONFIG.capMaxEmcToKleinStars.get();
    }

    public static boolean enableRandomCourses() {
        return CONFIG.enableRandomCourses.get();
    }

    public static boolean chooseBaseEmcIfLower() {
        return CONFIG.chooseBaseEmcIfLower.get();
    }

    public static boolean backtrackMissingBaseEmc() {
        return CONFIG.backtrackMissingBaseEmc.get();
    }

    public static List<String> stars() {
        Path path = starsConfigFilePath();
        ensureStarsConfigExists(path);

        long modifiedTime = fileModifiedTime(path);
        long fileSize = fileSize(path);
        List<String> localCache = cachedStars;

        if (localCache != null
                && cachedStarsModifiedTime == modifiedTime
                && cachedStarsSize == fileSize) {
            return new ArrayList<>(localCache);
        }

        synchronized (STARS_CACHE_LOCK) {
            modifiedTime = fileModifiedTime(path);
            fileSize = fileSize(path);
            localCache = cachedStars;

            if (localCache != null
                    && cachedStarsModifiedTime == modifiedTime
                    && cachedStarsSize == fileSize) {
                return new ArrayList<>(localCache);
            }

            List<String> parsed = readStarsFromPhysicalConfig(path);
            cachedStars = List.copyOf(parsed);
            cachedStarsModifiedTime = modifiedTime;
            cachedStarsSize = fileSize;
            starsRevision++;
            com.D3D.projectenervate.emc.StarDefinitionManager.invalidateCaches();
            return new ArrayList<>(cachedStars);
        }
    }

    public static void setStars(List<String> entries) {
        List<String> safeEntries = new ArrayList<>(entries);
        saveStarsToPhysicalConfig(safeEntries);

        synchronized (STARS_CACHE_LOCK) {
            Path path = starsConfigFilePath();
            cachedStars = List.copyOf(safeEntries);
            cachedStarsModifiedTime = fileModifiedTime(path);
            cachedStarsSize = fileSize(path);
            starsRevision++;
            com.D3D.projectenervate.emc.StarDefinitionManager.invalidateCaches();
        }
    }

    public static long starsRevision() {
        return starsRevision;
    }

    public static Path starsConfigFilePath() {
        return FMLPaths.CONFIGDIR.get().resolve(STARS_CONFIG_FILE_NAME);
    }

    public static Path configFilePath() {
        return starsConfigFilePath();
    }

    private static long fileModifiedTime(Path path) {
        try {
            return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : Long.MIN_VALUE;
        } catch (IOException exception) {
            return Long.MIN_VALUE;
        }
    }

    private static long fileSize(Path path) {
        try {
            return Files.exists(path) ? Files.size(path) : Long.MIN_VALUE;
        } catch (IOException exception) {
            return Long.MIN_VALUE;
        }
    }

    private static void ensureStarsConfigExists(Path path) {
        if (Files.exists(path)) {
            return;
        }

        List<String> migratedCommonStars = readStarsFromPhysicalConfig(commonConfigFilePath());
        saveStarsToPhysicalConfig(migratedCommonStars.isEmpty() ? DEFAULT_STARS : migratedCommonStars);
    }

    private static Path commonConfigFilePath() {
        return FMLPaths.CONFIGDIR.get().resolve(COMMON_CONFIG_FILE_NAME);
    }

    private static List<String> readStarsFromPhysicalConfig(Path path) {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<String> parsed = parseStarsBlock(lines);
            if (!parsed.isEmpty() || containsStarsKey(lines)) {
                return parsed;
            }
        } catch (IOException exception) {
            ProjectEnervate.LOGGER.error("ProjectEnervate could not read star config from {}", path, exception);
        }

        return new ArrayList<>(DEFAULT_STARS);
    }

    private static void saveStarsToPhysicalConfig(List<String> entries) {
        Path path = starsConfigFilePath();

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, formatStarsFile(entries), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            ProjectEnervate.LOGGER.error("ProjectEnervate could not write star config to {}", path, exception);
        }
    }

    private static List<String> formatStarsFile(List<String> entries) {
        List<String> lines = new ArrayList<>();
        lines.add("# ProjectEnervate star definitions.");
        lines.add("# Format per entry: Star Name|resource1,resource2,#tag_namespace:tag_path");
        lines.add("# The list order is the star id and sky spawn order.");
        lines.add("# Append new stars to keep earlier star ids and sky positions stable.");
        lines.add("# Missing item ids and missing/inactive tags are ignored at runtime.");
        lines.add("# A star with no installed valid base-EMC resources is skipped and does not generate.");
        lines.add("# Tags use Minecraft item tags. NeoForge common resource tags normally use the c namespace, for example #c:ingots/copper.");
        lines.add("");
        lines.add(STARS_SECTION);
        lines.add(STARS_KEY + " = [");

        for (int i = 0; i < entries.size(); i++) {
            String comma = i + 1 < entries.size() ? "," : "";
            lines.add("\t\"" + escapeTomlString(entries.get(i)) + "\"" + comma);
        }

        lines.add("]");
        return lines;
    }

    private static List<String> parseStarsBlock(List<String> lines) {
        List<String> result = new ArrayList<>();
        int keyLine = findKey(lines, STARS_KEY);

        if (keyLine < 0) {
            return result;
        }

        String firstLine = stripComment(lines.get(keyLine));
        int equals = firstLine.indexOf('=');
        if (equals < 0) {
            return result;
        }

        StringBuilder rawValue = new StringBuilder(firstLine.substring(equals + 1));
        if (!firstLine.contains("]") && firstLine.contains("[")) {
            for (int i = keyLine + 1; i < lines.size(); i++) {
                String line = stripComment(lines.get(i));
                rawValue.append('\n').append(line);
                if (line.contains("]")) {
                    break;
                }
            }
        }

        return parseTomlStringArray(rawValue.toString());
    }

    private static List<String> parseTomlStringArray(String rawValue) {
        List<String> result = new ArrayList<>();
        int start = rawValue.indexOf('[');
        int end = rawValue.lastIndexOf(']');

        if (start < 0 || end <= start) {
            return result;
        }

        String content = rawValue.substring(start + 1, end);
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (!inString) {
                if (c == '"') {
                    inString = true;
                    current.setLength(0);
                }
                continue;
            }

            if (escaping) {
                current.append(switch (c) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> c;
                });
                escaping = false;
                continue;
            }

            if (c == '\\') {
                escaping = true;
                continue;
            }

            if (c == '"') {
                inString = false;
                result.add(current.toString());
                continue;
            }

            current.append(c);
        }

        return result;
    }

    private static boolean containsStarsKey(List<String> lines) {
        return findKey(lines, STARS_KEY) >= 0;
    }

    private static int findKey(List<String> lines, String key) {
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = stripComment(lines.get(i)).trim();
            if (trimmed.startsWith(key + " ") || trimmed.startsWith(key + "=")) {
                return i;
            }
        }

        return -1;
    }

    private static String stripComment(String line) {
        if (line == null) {
            return "";
        }

        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }

            if (c == '\\') {
                escaping = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (c == '#' && !inString) {
                return line.substring(0, i);
            }
        }

        return line;
    }

    private static String escapeTomlString(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
