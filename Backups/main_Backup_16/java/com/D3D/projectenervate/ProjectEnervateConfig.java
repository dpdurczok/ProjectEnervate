package com.D3D.projectenervate;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class ProjectEnervateConfig {
    private static final List<String> DEFAULT_RANDOM_COURSE_RESOURCES = List.of(
            "minecraft:coal",
            "minecraft:raw_iron",
            "minecraft:raw_copper",
            "minecraft:raw_gold",
            "minecraft:gold_nugget",
            "minecraft:diamond",
            "minecraft:emerald",
            "minecraft:lapis_lazuli",
            "minecraft:redstone",
            "minecraft:quartz",
            "minecraft:glowstone_dust",
            "minecraft:amethyst_shard",
            "minecraft:ancient_debris"
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
    private final ModConfigSpec.ConfigValue<List<String>> randomCourseResources;

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
                .comment("If true, natural block drops for configured resources receive cycling planetary-course EMC multipliers.",
                        "This only affects clean naturally mined/generated block drops. Placed blocks with ProjectEnervate EMC data keep their stored value.")
                .translation("projectenervate.configuration.enable_random_courses")
                .define("EnableRandomCourses", true);

        chooseBaseEmcIfLower = builder
                .comment("If true, adaptive outputs whose calculated EMC is above the ProjectE base value are reduced to the lower base EMC.",
                        "If false, adaptive outputs keep their calculated EMC even when it is above base, allowing high-course resource value to survive crafting, smelting, trades, block drops, and other tracked conversions.")
                .translation("projectenervate.configuration.choose_base_emc_if_lower")
                .define("ChooseBaseEMCIfLower", false);

        randomCourseResources = builder
                .comment("Item ids that can be selected for random planetary-course EMC multipliers.",
                        "Use the resource item id, for example minecraft:diamond, minecraft:raw_iron, minecraft:quartz.",
                        "Vanilla silk-touch ore drops are internally mapped to their matching resource where possible.")
                .translation("projectenervate.configuration.random_course_resources")
                .define("RandomCourseResources", DEFAULT_RANDOM_COURSE_RESOURCES);

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

    public static List<String> randomCourseResources() {
        return new ArrayList<>(CONFIG.randomCourseResources.get());
    }
}
