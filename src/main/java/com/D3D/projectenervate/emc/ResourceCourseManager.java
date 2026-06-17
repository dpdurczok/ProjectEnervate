package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervateConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ResourceCourseManager {
    private static final BigDecimal MIN_MULTIPLIER = new BigDecimal("0.1");
    private static final BigDecimal MAX_MULTIPLIER = new BigDecimal("2.0");
    private static final BigDecimal STANDARD_MULTIPLIER = BigDecimal.ONE;
    private static final BigDecimal COURSE_HOLD_START = new BigDecimal("0.8");
    private static final long MINECRAFT_DAY_TICKS = 24000L;
    private static final int MAX_COURSE_SCAN_COUNT = 1_000_000;

    private ResourceCourseManager() {
    }

    public static void applyNaturalCourseOrVerify(ServerLevel level, List<ItemEntity> drops) {
        if (drops == null || drops.isEmpty()) {
            return;
        }

        for (ItemEntity drop : drops) {
            if (drop == null) {
                continue;
            }

            applyNaturalCourseOrVerify(level, drop.getItem());
        }
    }

    public static void applyNaturalCourseOrVerify(ServerLevel level, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        if (ProjectEnervateSourceHelper.hasProjectEnervateData(stack)) {
            return;
        }

        if (applyNaturalCourse(level, stack)) {
            return;
        }

        ProjectEnervateSourceHelper.markVerifiedIfBaseEmcPreservingExisting(stack);
    }

    public static boolean applyNaturalCourse(ServerLevel level, ItemStack stack) {
        if (!ProjectEnervateConfig.enableRandomCourses()) {
            return false;
        }

        if (level == null || stack == null || stack.isEmpty()) {
            return false;
        }

        if (ProjectEnervateSourceHelper.hasProjectEnervateData(stack)) {
            return false;
        }

        ResourceLocation resource = getConfiguredCanonicalResource(stack);

        if (resource == null) {
            return false;
        }

        BigDecimal baseStackEmc = AdaptiveEmcOutputHelper.getBaseStackEmc(stack);

        if (baseStackEmc.signum() <= 0) {
            return false;
        }

        CourseSnapshot snapshot = currentSnapshot(level, configuredCourseBodies(configuredResources()));
        ResourceLocation courseBody = courseBodyForResource(resource);
        BigDecimal multiplier = snapshot.currentMultipliers.getOrDefault(courseBody, STANDARD_MULTIPLIER);
        BigDecimal proposedStackEmc = baseStackEmc.multiply(multiplier);

        AdaptiveEmcOutputHelper.applyUncappedAdaptiveStackEmc(stack, proposedStackEmc);
        return true;
    }

    public static Map<ResourceLocation, BigDecimal> currentMultipliers(ServerLevel level) {
        Set<ResourceLocation> resources = configuredResources();
        Map<ResourceLocation, BigDecimal> result = new LinkedHashMap<>();

        if (resources.isEmpty()) {
            return result;
        }

        if (!ProjectEnervateConfig.enableRandomCourses()) {
            for (ResourceLocation resource : resources) {
                result.put(resource, STANDARD_MULTIPLIER);
            }
            return result;
        }

        Map<ResourceLocation, BigDecimal> bodyMultipliers = currentSnapshot(level, configuredCourseBodies(resources)).currentMultipliers;

        for (ResourceLocation resource : resources) {
            result.put(resource, bodyMultipliers.getOrDefault(courseBodyForResource(resource), STANDARD_MULTIPLIER));
        }

        return result;
    }

    public static String currentCourseStatus(ServerLevel level) {
        Set<ResourceLocation> resources = configuredResources();

        if (resources.isEmpty()) {
            return "no configured resources";
        }

        if (!ProjectEnervateConfig.enableRandomCourses()) {
            return "disabled";
        }

        CourseSnapshot snapshot = currentSnapshot(level, configuredCourseBodies(resources));
        return "seeded course " + snapshot.courseIndex
                + ", dayTime " + snapshot.dayTime
                + ", course ticks " + snapshot.courseStartTick + "-" + snapshot.courseEndTick;
    }

    public static ResourceLocation canonicalResourceForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (id == null) {
            return null;
        }

        if (!"minecraft".equals(id.getNamespace())) {
            return id;
        }

        return switch (id.getPath()) {
            case "coal_ore", "deepslate_coal_ore" -> minecraft("coal");
            case "iron_ore", "deepslate_iron_ore" -> minecraft("raw_iron");
            case "copper_ore", "deepslate_copper_ore" -> minecraft("raw_copper");
            case "gold_ore", "deepslate_gold_ore" -> minecraft("raw_gold");
            case "nether_gold_ore" -> minecraft("gold_nugget");
            case "diamond_ore", "deepslate_diamond_ore" -> minecraft("diamond");
            case "emerald_ore", "deepslate_emerald_ore" -> minecraft("emerald");
            case "lapis_ore", "deepslate_lapis_ore" -> minecraft("lapis_lazuli");
            case "redstone_ore", "deepslate_redstone_ore" -> minecraft("redstone");
            case "nether_quartz_ore" -> minecraft("quartz");
            case "glowstone" -> minecraft("glowstone_dust");
            case "amethyst_cluster", "large_amethyst_bud", "medium_amethyst_bud", "small_amethyst_bud" -> minecraft("amethyst_shard");
            default -> id;
        };
    }

    private static ResourceLocation getConfiguredCanonicalResource(ItemStack stack) {
        ResourceLocation resource = canonicalResourceForStack(stack);

        if (resource == null) {
            return null;
        }

        return configuredResources().contains(resource) ? resource : null;
    }

    private static Set<ResourceLocation> configuredResources() {
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();

        for (String raw : ProjectEnervateConfig.randomCourseResources()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(raw.trim());

            if (id != null) {
                result.add(id);
            }
        }

        return result;
    }


    private static Set<ResourceLocation> configuredCourseBodies(Set<ResourceLocation> resources) {
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();

        for (ResourceLocation resource : resources) {
            result.add(courseBodyForResource(resource));
        }

        return result;
    }

    private static ResourceLocation courseBodyForResource(ResourceLocation resource) {
        if (resource == null) {
            return null;
        }

        if ("minecraft".equals(resource.getNamespace())
                && ("raw_gold".equals(resource.getPath()) || "gold_nugget".equals(resource.getPath()))) {
            return minecraft("raw_gold");
        }

        return resource;
    }

    private static CourseSnapshot currentSnapshot(ServerLevel level, Set<ResourceLocation> resources) {
        long seed = resolveCourseSeed(level);
        long dayTime = resolveCourseDayTime(level);
        long courseStartTick = 0L;
        long courseIndex = 0L;
        Map<ResourceLocation, BigDecimal> startMultipliers = standardMultipliers(resources);
        Map<ResourceLocation, BigDecimal> targetMultipliers;

        for (int scanCount = 0; scanCount < MAX_COURSE_SCAN_COUNT; scanCount++) {
            targetMultipliers = generateTargetMultipliers(resources, randomFor(seed, courseIndex, 0x434f55525345544cL));
            long durationTicks = randomDurationTicks(randomFor(seed, courseIndex, 0x434f555253454455L));
            long courseEndTick = safeAdd(courseStartTick, durationTicks);

            if (dayTime < courseEndTick || courseEndTick == Long.MAX_VALUE) {
                Map<ResourceLocation, BigDecimal> currentMultipliers = calculateCurrentMultipliers(
                        resources,
                        startMultipliers,
                        targetMultipliers,
                        courseStartTick,
                        courseEndTick,
                        dayTime
                );

                return new CourseSnapshot(courseIndex, dayTime, courseStartTick, courseEndTick, currentMultipliers);
            }

            startMultipliers = targetMultipliers;
            courseStartTick = courseEndTick;
            courseIndex++;
        }

        return fallbackSnapshot(seed, dayTime, resources);
    }

    private static CourseSnapshot fallbackSnapshot(long seed, long dayTime, Set<ResourceLocation> resources) {
        long estimatedIndex = Math.max(0L, dayTime / (MINECRAFT_DAY_TICKS * 5L));
        long courseStartTick = Math.max(0L, estimatedIndex * MINECRAFT_DAY_TICKS * 5L);
        long courseEndTick = safeAdd(courseStartTick, MINECRAFT_DAY_TICKS * 5L);
        Map<ResourceLocation, BigDecimal> startMultipliers = generateTargetMultipliers(resources, randomFor(seed, Math.max(0L, estimatedIndex - 1L), 0x434f55525345544cL));
        Map<ResourceLocation, BigDecimal> targetMultipliers = generateTargetMultipliers(resources, randomFor(seed, estimatedIndex, 0x434f55525345544cL));
        Map<ResourceLocation, BigDecimal> currentMultipliers = calculateCurrentMultipliers(
                resources,
                startMultipliers,
                targetMultipliers,
                courseStartTick,
                courseEndTick,
                dayTime
        );

        return new CourseSnapshot(estimatedIndex, dayTime, courseStartTick, courseEndTick, currentMultipliers);
    }

    private static long resolveCourseSeed(ServerLevel level) {
        ServerLevel courseLevel = courseLevel(level);
        return courseLevel == null ? 0L : courseLevel.getSeed();
    }

    private static long resolveCourseDayTime(ServerLevel level) {
        ServerLevel courseLevel = courseLevel(level);
        return Math.max(0L, courseLevel == null ? 0L : courseLevel.getDayTime());
    }

    private static ServerLevel courseLevel(ServerLevel level) {
        if (level == null) {
            return null;
        }

        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        return overworld == null ? level : overworld;
    }

    private static Map<ResourceLocation, BigDecimal> standardMultipliers(Set<ResourceLocation> resources) {
        Map<ResourceLocation, BigDecimal> result = new LinkedHashMap<>();

        for (ResourceLocation resource : resources) {
            result.put(resource, STANDARD_MULTIPLIER);
        }

        return result;
    }

    private static Map<ResourceLocation, BigDecimal> calculateCurrentMultipliers(
            Set<ResourceLocation> resources,
            Map<ResourceLocation, BigDecimal> startMultipliers,
            Map<ResourceLocation, BigDecimal> targetMultipliers,
            long courseStartTick,
            long courseEndTick,
            long dayTime
    ) {
        Map<ResourceLocation, BigDecimal> result = new LinkedHashMap<>();

        for (ResourceLocation resource : resources) {
            BigDecimal start = startMultipliers.getOrDefault(resource, STANDARD_MULTIPLIER);
            BigDecimal target = targetMultipliers.getOrDefault(resource, STANDARD_MULTIPLIER);
            result.put(resource, calculateCurrentMultiplier(start, target, courseStartTick, courseEndTick, dayTime));
        }

        return result;
    }

    private static BigDecimal calculateCurrentMultiplier(
            BigDecimal start,
            BigDecimal target,
            long courseStartTick,
            long courseEndTick,
            long dayTime
    ) {
        if (dayTime >= courseEndTick) {
            return normalize(target);
        }

        long duration = Math.max(1L, courseEndTick - courseStartTick);
        long elapsed = Math.max(0L, dayTime - courseStartTick);
        BigDecimal progress = BigDecimal.valueOf(elapsed)
                .divide(BigDecimal.valueOf(duration), AdaptiveEmcValues.INTERNAL_SCALE, RoundingMode.HALF_UP);

        double progressDouble = progress.doubleValue();
        double eased;

        if (progress.compareTo(COURSE_HOLD_START) < 0) {
            double chaseProgress = progress.divide(COURSE_HOLD_START, AdaptiveEmcValues.INTERNAL_SCALE, RoundingMode.HALF_UP).doubleValue();
            eased = 1.0D - Math.pow(0.05D, chaseProgress);
        } else {
            double holdProgress = progress.subtract(COURSE_HOLD_START)
                    .divide(BigDecimal.ONE.subtract(COURSE_HOLD_START), AdaptiveEmcValues.INTERNAL_SCALE, RoundingMode.HALF_UP)
                    .doubleValue();
            holdProgress = clampDouble(holdProgress, 0.0D, 1.0D);
            double smooth = holdProgress * holdProgress * (3.0D - 2.0D * holdProgress);
            eased = 0.95D + (0.05D * smooth);
        }

        if (progressDouble <= 0.0D) {
            eased = 0.0D;
        }

        BigDecimal value = start.add(
                target.subtract(start).multiply(BigDecimal.valueOf(clampDouble(eased, 0.0D, 1.0D)))
        );

        return normalize(clamp(value));
    }

    private static Map<ResourceLocation, BigDecimal> generateTargetMultipliers(Set<ResourceLocation> resources, Random random) {
        Map<ResourceLocation, BigDecimal> result = standardMultipliers(resources);
        List<ResourceLocation> shuffled = new ArrayList<>(resources);
        Collections.shuffle(shuffled, random);

        if (shuffled.isEmpty()) {
            return result;
        }

        if (shuffled.size() == 1) {
            result.put(shuffled.get(0), normalize(randomBetween(random, MIN_MULTIPLIER, MAX_MULTIPLIER)));
            return result;
        }

        double strength = 0.75D + (random.nextDouble() * 0.25D);
        BigDecimal lowOutlier = BigDecimal.ONE.subtract(new BigDecimal("0.9").multiply(BigDecimal.valueOf(strength)));
        BigDecimal highOutlier = BigDecimal.ONE.add(BigDecimal.valueOf(strength));

        if (random.nextBoolean()) {
            result.put(shuffled.get(0), normalize(clamp(lowOutlier)));
            result.put(shuffled.get(1), normalize(clamp(highOutlier)));
        } else {
            result.put(shuffled.get(0), normalize(clamp(highOutlier)));
            result.put(shuffled.get(1), normalize(clamp(lowOutlier)));
        }

        int availableOthers = Math.max(0, shuffled.size() - 2);
        int otherAffected = Math.min(availableOthers, 4 + random.nextInt(5));
        int lowSideCount = otherAffected / 2;
        int highSideCount = otherAffected - lowSideCount;

        if (random.nextBoolean()) {
            int swap = lowSideCount;
            lowSideCount = highSideCount;
            highSideCount = swap;
        }

        BigDecimal lowMaxDistance = BigDecimal.ONE.subtract(lowOutlier).multiply(new BigDecimal("0.85"));
        BigDecimal highMaxDistance = highOutlier.subtract(BigDecimal.ONE).multiply(new BigDecimal("0.85"));
        int index = 2;

        for (int i = 0; i < lowSideCount && index < shuffled.size(); i++, index++) {
            BigDecimal distance = randomDistance(random, lowMaxDistance);
            result.put(shuffled.get(index), normalize(clamp(BigDecimal.ONE.subtract(distance))));
        }

        for (int i = 0; i < highSideCount && index < shuffled.size(); i++, index++) {
            BigDecimal distance = randomDistance(random, highMaxDistance);
            result.put(shuffled.get(index), normalize(clamp(BigDecimal.ONE.add(distance))));
        }

        return result;
    }

    private static BigDecimal randomDistance(Random random, BigDecimal maxDistance) {
        BigDecimal minimum = new BigDecimal("0.05");

        if (maxDistance.compareTo(minimum) <= 0) {
            return maxDistance.max(BigDecimal.ZERO);
        }

        return minimum.add(maxDistance.subtract(minimum).multiply(BigDecimal.valueOf(random.nextDouble())));
    }

    private static long randomDurationTicks(Random random) {
        return (1L + random.nextInt(10)) * MINECRAFT_DAY_TICKS;
    }

    private static BigDecimal randomBetween(Random random, BigDecimal min, BigDecimal max) {
        return min.add(max.subtract(min).multiply(BigDecimal.valueOf(random.nextDouble())));
    }

    private static Random randomFor(long worldSeed, long courseIndex, long salt) {
        long mixed = worldSeed;
        mixed ^= Long.rotateLeft(courseIndex * 0x9E3779B97F4A7C15L, 17);
        mixed ^= salt;
        return new Random(mix64(mixed));
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static long safeAdd(long a, long b) {
        long result = a + b;
        return result < 0L ? Long.MAX_VALUE : result;
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value.setScale(AdaptiveEmcValues.INTERNAL_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(MIN_MULTIPLIER) < 0) {
            return MIN_MULTIPLIER;
        }

        if (value.compareTo(MAX_MULTIPLIER) > 0) {
            return MAX_MULTIPLIER;
        }

        return value;
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    private record CourseSnapshot(
            long courseIndex,
            long dayTime,
            long courseStartTick,
            long courseEndTick,
            Map<ResourceLocation, BigDecimal> currentMultipliers
    ) {
    }
}
