package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervateConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private static final Object SNAPSHOT_CACHE_LOCK = new Object();
    private static volatile CachedCourseSnapshot cachedCourseSnapshot;

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

        ResourceLocation canonicalResource = canonicalResourceForStack(stack);
        StarDefinitionManager.ActiveStar star = StarDefinitionManager.findActiveStarForStack(stack, canonicalResource);

        if (star == null) {
            return false;
        }

        BigDecimal baseStackEmc = AdaptiveEmcOutputHelper.getBaseStackEmc(stack);

        if (baseStackEmc.signum() <= 0) {
            return false;
        }

        List<StarDefinitionManager.ActiveStar> activeStars = StarDefinitionManager.activeStars();
        CourseSnapshot snapshot = currentSnapshot(level, activeStars);
        BigDecimal multiplier = snapshot.currentMultipliers.getOrDefault(star.index(), STANDARD_MULTIPLIER);
        BigDecimal proposedStackEmc = baseStackEmc.multiply(multiplier);

        AdaptiveEmcOutputHelper.applyUncappedAdaptiveStackEmc(stack, proposedStackEmc);
        return true;
    }

    public static List<StarCourseView> currentStarCourses(ServerLevel level) {
        List<StarDefinitionManager.ActiveStar> stars = StarDefinitionManager.activeStars();
        List<StarCourseView> result = new ArrayList<>();

        if (stars.isEmpty()) {
            return result;
        }

        Map<Integer, BigDecimal> multipliers = ProjectEnervateConfig.enableRandomCourses()
                ? currentSnapshot(level, stars).currentMultipliers
                : standardMultipliers(stars);

        for (StarDefinitionManager.ActiveStar star : stars) {
            result.add(new StarCourseView(
                    star.index(),
                    star.name(),
                    star.configuredResources(),
                    star.activeResources(),
                    multipliers.getOrDefault(star.index(), STANDARD_MULTIPLIER)
            ));
        }

        return result;
    }

    public static Map<ResourceLocation, BigDecimal> currentMultipliers(ServerLevel level) {
        Map<ResourceLocation, BigDecimal> result = new LinkedHashMap<>();

        for (StarCourseView star : currentStarCourses(level)) {
            for (ResourceLocation concrete : StarDefinitionManager.activeConcreteItemResources(new StarDefinitionManager.ActiveStar(
                    star.index(),
                    star.name(),
                    star.configuredResources(),
                    star.activeResources()
            ))) {
                result.put(concrete, star.multiplier());
            }
        }

        return result;
    }

    public static String currentCourseStatus(ServerLevel level) {
        List<StarDefinitionManager.ActiveStar> stars = StarDefinitionManager.activeStars();

        if (stars.isEmpty()) {
            return "no active configured stars";
        }

        if (!ProjectEnervateConfig.enableRandomCourses()) {
            return "disabled";
        }

        CourseSnapshot snapshot = currentSnapshot(level, stars);
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

    private static CourseSnapshot currentSnapshot(ServerLevel level, List<StarDefinitionManager.ActiveStar> stars) {
        long seed = resolveCourseSeed(level);
        long dayTime = resolveCourseDayTime(level);
        long starsRevision = ProjectEnervateConfig.starsRevision();
        CachedCourseSnapshot localCache = cachedCourseSnapshot;

        if (localCache != null
                && localCache.seed() == seed
                && localCache.dayTime() == dayTime
                && localCache.starsRevision() == starsRevision) {
            return localCache.snapshot();
        }

        synchronized (SNAPSHOT_CACHE_LOCK) {
            localCache = cachedCourseSnapshot;
            if (localCache != null
                    && localCache.seed() == seed
                    && localCache.dayTime() == dayTime
                    && localCache.starsRevision() == starsRevision) {
                return localCache.snapshot();
            }

            CourseSnapshot snapshot = computeSnapshot(seed, dayTime, stars);
            cachedCourseSnapshot = new CachedCourseSnapshot(seed, dayTime, starsRevision, snapshot);
            return snapshot;
        }
    }

    private static CourseSnapshot computeSnapshot(long seed, long dayTime, List<StarDefinitionManager.ActiveStar> stars) {
        long courseStartTick = 0L;
        long courseIndex = 0L;
        Map<Integer, BigDecimal> startMultipliers = standardMultipliers(stars);
        Map<Integer, BigDecimal> targetMultipliers;

        for (int scanCount = 0; scanCount < MAX_COURSE_SCAN_COUNT; scanCount++) {
            targetMultipliers = generateTargetMultipliers(seed, courseIndex, stars);
            long durationTicks = randomDurationTicks(randomFor(seed, courseIndex, 0x434f555253454455L));
            long courseEndTick = safeAdd(courseStartTick, durationTicks);

            if (dayTime < courseEndTick || courseEndTick == Long.MAX_VALUE) {
                Map<Integer, BigDecimal> currentMultipliers = calculateCurrentMultipliers(
                        stars,
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

        return fallbackSnapshot(seed, dayTime, stars);
    }

    private static CourseSnapshot fallbackSnapshot(long seed, long dayTime, List<StarDefinitionManager.ActiveStar> stars) {
        long estimatedIndex = Math.max(0L, dayTime / (MINECRAFT_DAY_TICKS * 5L));
        long courseStartTick = Math.max(0L, estimatedIndex * MINECRAFT_DAY_TICKS * 5L);
        long courseEndTick = safeAdd(courseStartTick, MINECRAFT_DAY_TICKS * 5L);
        Map<Integer, BigDecimal> startMultipliers = generateTargetMultipliers(seed, Math.max(0L, estimatedIndex - 1L), stars);
        Map<Integer, BigDecimal> targetMultipliers = generateTargetMultipliers(seed, estimatedIndex, stars);
        Map<Integer, BigDecimal> currentMultipliers = calculateCurrentMultipliers(
                stars,
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

    private static Map<Integer, BigDecimal> standardMultipliers(List<StarDefinitionManager.ActiveStar> stars) {
        Map<Integer, BigDecimal> result = new LinkedHashMap<>();

        for (StarDefinitionManager.ActiveStar star : stars) {
            result.put(star.index(), STANDARD_MULTIPLIER);
        }

        return result;
    }

    private static Map<Integer, BigDecimal> calculateCurrentMultipliers(
            List<StarDefinitionManager.ActiveStar> stars,
            Map<Integer, BigDecimal> startMultipliers,
            Map<Integer, BigDecimal> targetMultipliers,
            long courseStartTick,
            long courseEndTick,
            long dayTime
    ) {
        Map<Integer, BigDecimal> result = new LinkedHashMap<>();

        for (StarDefinitionManager.ActiveStar star : stars) {
            BigDecimal start = startMultipliers.getOrDefault(star.index(), STANDARD_MULTIPLIER);
            BigDecimal target = targetMultipliers.getOrDefault(star.index(), STANDARD_MULTIPLIER);
            result.put(star.index(), calculateCurrentMultiplier(start, target, courseStartTick, courseEndTick, dayTime));
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

    private static Map<Integer, BigDecimal> generateTargetMultipliers(
            long seed,
            long courseIndex,
            List<StarDefinitionManager.ActiveStar> stars
    ) {
        Map<Integer, BigDecimal> result = new LinkedHashMap<>();

        for (StarDefinitionManager.ActiveStar star : stars) {
            Random random = randomFor(seed, courseIndex, 0x535441525f4d554cL ^ ((long) star.index() * 0x9E3779B97F4A7C15L));
            result.put(star.index(), normalize(targetMultiplierForStar(random)));
        }

        return result;
    }

    private static BigDecimal targetMultiplierForStar(Random random) {
        double roll = random.nextDouble();
        BigDecimal min;
        BigDecimal max;

        if (roll < 0.18D) {
            min = MIN_MULTIPLIER;
            max = new BigDecimal("0.35");
        } else if (roll < 0.36D) {
            min = new BigDecimal("1.65");
            max = MAX_MULTIPLIER;
        } else {
            min = new BigDecimal("0.72");
            max = new BigDecimal("1.35");
        }

        return randomBetween(random, min, max);
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

    public record StarCourseView(
            int index,
            String name,
            List<String> configuredResources,
            List<String> activeResources,
            BigDecimal multiplier
    ) {
    }

    private record CachedCourseSnapshot(
            long seed,
            long dayTime,
            long starsRevision,
            CourseSnapshot snapshot
    ) {
    }

    private record CourseSnapshot(
            long courseIndex,
            long dayTime,
            long courseStartTick,
            long courseEndTick,
            Map<Integer, BigDecimal> currentMultipliers
    ) {
    }
}
