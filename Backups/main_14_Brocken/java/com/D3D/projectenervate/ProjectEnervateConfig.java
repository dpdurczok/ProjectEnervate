package com.D3D.projectenervate;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class ProjectEnervateConfig {
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
    private final ModConfigSpec.ConfigValue<List<? extends String>> randomCourseResources;

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

        builder.pop();

        builder.push("resource_courses");

        enableRandomCourses = builder
                .comment("If true, Celestial Mapping resource multipliers follow the day/night-cycle bound course system.")
                .translation("projectenervate.configuration.enable_random_courses")
                .define("enableRandomCourses", true);

        randomCourseResources = builder
                .comment("Resource item ids that receive Celestial Mapping course multipliers.",
                        "Use item ids like minecraft:coal, minecraft:iron_ingot, or modid:item_name.")
                .translation("projectenervate.configuration.random_course_resources")
                .defineList("randomCourseResources", List.of(), value -> value instanceof String);

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

    public static List<? extends String> randomCourseResources() {
        return CONFIG.randomCourseResources.get();
    }
}
