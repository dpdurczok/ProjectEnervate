package com.D3D.projectenervate.client;

import com.D3D.projectenervate.ProjectEnervate;
import com.D3D.projectenervate.menu.CelestialMappingMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CelestialSkyOverlay {
    private static final ResourceLocation STAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ProjectEnervate.MOD_ID,
            "textures/gui/celestial_star_sky.png"
    );

    private static final double MIN_STAR_WORLD_SIZE = 1.5D;
    private static final double MID_STAR_WORLD_SIZE = 3.0D;
    private static final double MAX_STAR_WORLD_SIZE = 10.0D;

    private static final int STAR_FRAME_SIZE = 16;
    private static final int STAR_FRAME_COUNT = 6;
    private static final int STAR_TEXTURE_WIDTH = STAR_FRAME_SIZE * STAR_FRAME_COUNT;
    private static final int STAR_TEXTURE_HEIGHT = STAR_FRAME_SIZE;
    private static final int STAR_FRAME_TICKS = 7;

    private static final double MIN_MULTIPLIER = 0.1D;
    private static final double MAX_MULTIPLIER = 2.0D;
    private static final double MID_MULTIPLIER = 1.0D;

    /*
     * Constellation controls.
     * Distances are sky angle degrees between star centers inside the moon-side sky field.
     */
    private static final int MIN_CONSTELLATION_SIZE = 3;
    private static final int MAX_CONSTELLATION_SIZE = 5;
    private static final double MIN_CONSTELLATION_STAR_DISTANCE_DEGREES = 7.0D;
    private static final double MAX_CONSTELLATION_STAR_DISTANCE_DEGREES = 14.0D;

    private static final double MAX_CONSTELLATION_LOCAL_RADIUS_DEGREES = 24.0D;
    private static final double INTER_CONSTELLATION_STAR_DISTANCE_DEGREES = 18.0D;
    private static final double CONSTELLATION_EDGE_MARGIN_DEGREES = 2.0D;
    private static final int CONSTELLATION_SHAPE_ATTEMPTS = 96;
    private static final int CONSTELLATION_CENTER_ATTEMPTS = 256;
    private static final int CONSTELLATION_FALLBACK_CENTER_ATTEMPTS = 160;

    /*
     * These are sky-sphere units, not GUI pixels.
     * The star is rendered on an artificial sky shell around the camera, so camera translation causes no parallax.
     */
    private static final double SKY_DISTANCE = 100.0D;

    private static final double MIN_MOON_OFFSET_DEGREES = 10.0D;
    private static final double MAX_MOON_OFFSET_DEGREES = 78.0D;

    private static final double VANILLA_SPYGLASS_FOV_MULTIPLIER = 0.1D;
    private static final double SCOPED_SCREEN_MARGIN = 1.06D;

    private static final Vec3 BASE_MOON_DIRECTION = new Vec3(0.0D, -1.0D, 0.0D);
    private static final Vec3 BASE_MOON_TANGENT_RIGHT = new Vec3(1.0D, 0.0D, 0.0D);
    private static final Vec3 BASE_MOON_TANGENT_UP = new Vec3(0.0D, 0.0D, 1.0D);
    private static final double VANILLA_SKY_YAW_DEGREES = -90.0D;

    private CelestialSkyOverlay() {
    }

    public record ScopedStarHit(
            CelestialMappingMenu.CelestialBodyView body,
            int x,
            int y,
            double angleDegrees
    ) {
    }

    public static Optional<ScopedStarHit> findScopedStarHit(Minecraft minecraft, Camera camera, int guiWidth, int guiHeight, float partialTick) {
        if (minecraft == null || minecraft.level == null || camera == null || guiWidth <= 0 || guiHeight <= 0) {
            return Optional.empty();
        }

        if (getNightAlpha(minecraft.level.getDayTime()) <= 0.0F) {
            return Optional.empty();
        }

        List<CelestialMappingMenu.CelestialBodyView> bodies = CelestialClientCourseState.getBodies();
        if (bodies.isEmpty()) {
            return Optional.empty();
        }

        Vec3 look = fromVector3f(camera.getLookVector()).normalize();
        Vec3 right = fromVector3f(camera.getLeftVector()).normalize().scale(-1.0D);
        Vec3 up = fromVector3f(camera.getUpVector()).normalize();

        double verticalTan = Math.tan(Math.toRadians(scopedVerticalFovDegrees(minecraft)) * 0.5D);
        double horizontalTan = verticalTan * ((double) guiWidth / (double) guiHeight);
        double celestialAngleDegrees = minecraft.level.getTimeOfDay(partialTick) * 360.0D;
        long skySeed = CelestialClientCourseState.getWorldSeed();

        ScopedStarHit best = null;
        double bestAngle = Double.MAX_VALUE;

        for (StarPlacement placement : generateSeededStarPlacements(bodies, skySeed)) {
            CelestialMappingMenu.CelestialBodyView body = placement.body();
            Vec3 worldDirection = moonRelativeSkyDirectionFor(placement, celestialAngleDegrees);
            double forward = worldDirection.dot(look);
            if (forward <= 0.001D) {
                continue;
            }

            double ndcX = worldDirection.dot(right) / (forward * horizontalTan);
            double ndcY = worldDirection.dot(up) / (forward * verticalTan);

            if (Math.abs(ndcX) > SCOPED_SCREEN_MARGIN || Math.abs(ndcY) > SCOPED_SCREEN_MARGIN) {
                continue;
            }

            double angleDegrees = Math.toDegrees(Math.acos(clamp(forward, -1.0D, 1.0D)));
            if (angleDegrees >= bestAngle) {
                continue;
            }

            double starAngularRadius = (starWorldSizeForMultiplier(body.multiplier()) * 0.5D) / SKY_DISTANCE;
            int starPixelRadius = Math.max(6, (int) Math.ceil((starAngularRadius / verticalTan) * guiHeight * 0.5D));
            int x = (int) Math.round((guiWidth * 0.5D) + ndcX * guiWidth * 0.5D);
            int y = (int) Math.round((guiHeight * 0.5D) - ndcY * guiHeight * 0.5D + starPixelRadius + 4);

            best = new ScopedStarHit(body, x, y, angleDegrees);
            bestAngle = angleDegrees;
        }

        return Optional.ofNullable(best);
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        /*
         * Do not check minecraft.screen here.
         * The chat window and inventory are screens, but the world is still rendered behind them.
         * Skipping when a screen is open makes the stars pop out exactly when chat opens.
         */
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }

        float nightAlpha = getNightAlpha(minecraft.level.getDayTime());
        if (nightAlpha <= 0.0F) {
            return;
        }

        List<CelestialMappingMenu.CelestialBodyView> bodies = CelestialClientCourseState.getBodies();
        if (bodies.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) {
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        Camera camera = event.getCamera();
        if (camera == null) {
            return;
        }

        /*
         * RenderLevelStageEvent AFTER_SKY gives us a sky render slot. The star directions are stable
         * world-sky directions, then rotated into camera space manually. Translation is intentionally
         * ignored because the stars sit on an infinite sky shell.
         */
        Quaternionf worldToCameraRotation = new Quaternionf(camera.rotation()).conjugate();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double celestialAngleDegrees = minecraft.level.getTimeOfDay(partialTick) * 360.0D;
        long skySeed = CelestialClientCourseState.getWorldSeed();
        List<StarPlacement> placements = generateSeededStarPlacements(bodies, skySeed);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, STAR_TEXTURE);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        for (int i = 0; i < placements.size(); i++) {
            StarPlacement placement = placements.get(i);
            CelestialMappingMenu.CelestialBodyView body = placement.body();
            Vec3 worldDirection = moonRelativeSkyDirectionFor(placement, celestialAngleDegrees);

            Vec3 center = worldDirection.scale(SKY_DISTANCE);
            double halfSize = starWorldSizeForMultiplier(body.multiplier()) * 0.5D;

            Vec3 right = worldDirection.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (right.lengthSqr() < 0.0001D) {
                right = worldDirection.cross(new Vec3(1.0D, 0.0D, 0.0D));
            }
            right = right.normalize();
            Vec3 up = right.cross(worldDirection).normalize();

            double textureRotationRadians = starTextureRotationRadiansFor(body, skySeed);
            Vec3 rotatedRight = rotateBillboardAxisRight(right, up, textureRotationRadians);
            Vec3 rotatedUp = rotateBillboardAxisUp(right, up, textureRotationRadians);

            Vec3 r = rotatedRight.scale(halfSize);
            Vec3 u = rotatedUp.scale(halfSize);

            Vec3 p0 = rotateIntoCameraSpace(center.subtract(r).subtract(u), worldToCameraRotation);
            Vec3 p1 = rotateIntoCameraSpace(center.add(r).subtract(u), worldToCameraRotation);
            Vec3 p2 = rotateIntoCameraSpace(center.add(r).add(u), worldToCameraRotation);
            Vec3 p3 = rotateIntoCameraSpace(center.subtract(r).add(u), worldToCameraRotation);

            int frame = frameFor(gameTime + i, STAR_FRAME_TICKS, STAR_FRAME_COUNT);
            float u0 = (float) (frame * STAR_FRAME_SIZE) / (float) STAR_TEXTURE_WIDTH;
            float u1 = (float) ((frame + 1) * STAR_FRAME_SIZE) / (float) STAR_TEXTURE_WIDTH;
            float v0 = 0.0F;
            float v1 = (float) STAR_FRAME_SIZE / (float) STAR_TEXTURE_HEIGHT;
            int alpha = Mth.clamp((int) (nightAlpha * 255.0F), 0, 255);

            addVertex(buffer, matrix, p0, u0, v1, alpha);
            addVertex(buffer, matrix, p1, u1, v1, alpha);
            addVertex(buffer, matrix, p2, u1, v0, alpha);
            addVertex(buffer, matrix, p3, u0, v0, alpha);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static Vec3 rotateIntoCameraSpace(Vec3 worldVector, Quaternionf worldToCameraRotation) {
        Vector3f transformed = new Vector3f((float) worldVector.x, (float) worldVector.y, (float) worldVector.z);
        transformed.rotate(worldToCameraRotation);
        return new Vec3(transformed.x(), transformed.y(), transformed.z());
    }

    private static Vec3 rotateBillboardAxisRight(Vec3 right, Vec3 up, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return right.scale(cos).add(up.scale(sin)).normalize();
    }

    private static Vec3 rotateBillboardAxisUp(Vec3 right, Vec3 up, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return up.scale(cos).subtract(right.scale(sin)).normalize();
    }

    private static double starTextureRotationRadiansFor(CelestialMappingMenu.CelestialBodyView body, long skySeed) {
        long seed = starSeedFor(body, skySeed);
        long rotationSeed = mix64(seed ^ 0x535441525f524f54L);
        return unsignedUnit(rotationSeed) * Math.PI * 2.0D;
    }

    private static Vec3 fromVector3f(Vector3f vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double scopedVerticalFovDegrees(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null || minecraft.options.fov() == null) {
            return 7.0D;
        }

        return clamp(minecraft.options.fov().get() * VANILLA_SPYGLASS_FOV_MULTIPLIER, 3.0D, 11.0D);
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 pos, float u, float v, int alpha) {
        buffer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .setUv(u, v)
                .setColor(255, 255, 255, alpha);
    }

    private static float getNightAlpha(long dayTimeRaw) {
        long dayTime = Math.floorMod(dayTimeRaw, 24000L);

        if (dayTime < 12000L || dayTime > 24000L) {
            return 0.0F;
        }

        if (dayTime < 14000L) {
            return (dayTime - 12000L) / 2000.0F;
        }

        if (dayTime > 22000L) {
            return (24000L - dayTime) / 2000.0F;
        }

        return 1.0F;
    }

    private static int frameFor(long gameTime, int frameTicks, int frameCount) {
        if (frameCount <= 1) {
            return 0;
        }

        return (int) ((gameTime / Math.max(1, frameTicks)) % frameCount);
    }

    private static double starWorldSizeForMultiplier(double multiplier) {
        double normalized = (multiplier - MIN_MULTIPLIER) / (MAX_MULTIPLIER - MIN_MULTIPLIER);
        normalized = Mth.clamp(normalized, 0.0D, 1.0D);

        double normalizedMid = (MID_MULTIPLIER - MIN_MULTIPLIER) / (MAX_MULTIPLIER - MIN_MULTIPLIER);
        normalizedMid = Mth.clamp(normalizedMid, 0.000001D, 0.999999D);

        double midSizeNormalized = (MID_STAR_WORLD_SIZE - MIN_STAR_WORLD_SIZE) / (MAX_STAR_WORLD_SIZE - MIN_STAR_WORLD_SIZE);
        midSizeNormalized = Mth.clamp(midSizeNormalized, 0.000001D, 0.999999D);

        double curvePower = Math.log(midSizeNormalized) / Math.log(normalizedMid);
        double curved = Math.pow(normalized, curvePower);

        return Mth.lerp(curved, MIN_STAR_WORLD_SIZE, MAX_STAR_WORLD_SIZE);
    }

    private static List<StarPlacement> generateSeededStarPlacements(List<CelestialMappingMenu.CelestialBodyView> bodies, long skySeed) {
        if (bodies.isEmpty()) {
            return List.of();
        }

        List<CelestialMappingMenu.CelestialBodyView> orderedBodies = new ArrayList<>(bodies);
        orderedBodies.sort(Comparator.comparingLong(body -> mix64(starSeedFor(body, skySeed) ^ 0x4f524445525f5354L)));

        SeededRandom groupRandom = new SeededRandom(mix64(skySeed ^ 0x434f4e5354454c4cL));
        List<LocalConstellation> localConstellations = new ArrayList<>();

        int index = 0;
        int constellationIndex = 0;
        while (index < orderedBodies.size()) {
            int remaining = orderedBodies.size() - index;
            int constellationSize = chooseConstellationSize(remaining, groupRandom);
            List<CelestialMappingMenu.CelestialBodyView> group = new ArrayList<>(orderedBodies.subList(index, index + constellationSize));

            long firstBodySeed = group.isEmpty() ? 0L : starSeedFor(group.get(0), skySeed);
            SeededRandom shapeRandom = new SeededRandom(mix64(
                    skySeed
                            ^ firstBodySeed
                            ^ ((long) constellationIndex * 0x9E3779B97F4A7C15L)
                            ^ 0x53484150455f5354L
            ));

            GeneratedShape shape = growConstellationShape(group.size(), shapeRandom);
            localConstellations.add(new LocalConstellation(group, shape.points(), shape.radiusDegrees()));

            index += constellationSize;
            constellationIndex++;
        }

        List<PlacedConstellation> placedConstellations = new ArrayList<>();
        List<StarPlacement> placements = new ArrayList<>(orderedBodies.size());
        SeededRandom placementRandom = new SeededRandom(mix64(skySeed ^ 0x504c4143455f5354L));

        for (int i = 0; i < localConstellations.size(); i++) {
            LocalConstellation local = localConstellations.get(i);
            SkyPoint center = chooseConstellationCenter(local, placedConstellations, placementRandom, skySeed, i);
            List<SkyPoint> finalPoints = new ArrayList<>(local.points().size());

            for (int j = 0; j < local.points().size(); j++) {
                SkyPoint localPoint = local.points().get(j);
                SkyPoint finalPoint = new SkyPoint(center.x() + localPoint.x(), center.y() + localPoint.y());
                finalPoints.add(finalPoint);
                placements.add(new StarPlacement(local.bodies().get(j), finalPoint.x(), finalPoint.y()));
            }

            placedConstellations.add(new PlacedConstellation(center.x(), center.y(), local.radiusDegrees(), finalPoints));
        }

        return placements;
    }

    private static int chooseConstellationSize(int remaining, SeededRandom random) {
        int minSize = Math.max(1, Math.min(MIN_CONSTELLATION_SIZE, MAX_CONSTELLATION_SIZE));
        int maxSize = Math.max(minSize, MAX_CONSTELLATION_SIZE);

        if (remaining <= maxSize) {
            return remaining;
        }

        List<Integer> candidates = new ArrayList<>();
        for (int size = minSize; size <= maxSize && size <= remaining; size++) {
            int after = remaining - size;
            if (after == 0 || after >= minSize) {
                candidates.add(size);
            }
        }

        if (candidates.isEmpty()) {
            return Math.min(maxSize, remaining);
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private static GeneratedShape growConstellationShape(int size, SeededRandom random) {
        if (size <= 0) {
            return new GeneratedShape(List.of(), 0.0D);
        }

        List<SkyPoint> rawPoints = new ArrayList<>(size);
        rawPoints.add(new SkyPoint(0.0D, 0.0D));

        double lastAngle = random.nextDouble() * Math.PI * 2.0D;
        for (int i = 1; i < size; i++) {
            GrowthCandidate candidate = null;

            for (int attempt = 0; attempt < CONSTELLATION_SHAPE_ATTEMPTS; attempt++) {
                int parentIndex = chooseGrowthParent(rawPoints.size(), random);
                SkyPoint parent = rawPoints.get(parentIndex);
                double angle = chooseGrowthAngle(parent, rawPoints.size(), lastAngle, random);
                double distance = Mth.lerp(random.nextDouble(), MIN_CONSTELLATION_STAR_DISTANCE_DEGREES, MAX_CONSTELLATION_STAR_DISTANCE_DEGREES);
                SkyPoint point = new SkyPoint(
                        parent.x() + Math.cos(angle) * distance,
                        parent.y() + Math.sin(angle) * distance
                );

                if (isValidLocalConstellationPoint(point, rawPoints)) {
                    candidate = new GrowthCandidate(point, angle);
                    break;
                }
            }

            if (candidate == null) {
                candidate = findFallbackGrowthPoint(rawPoints, lastAngle);
            }

            rawPoints.add(candidate.point());
            lastAngle = candidate.angleRadians();
        }

        return centerConstellationShape(rawPoints);
    }

    private static int chooseGrowthParent(int existingCount, SeededRandom random) {
        if (existingCount <= 1) {
            return 0;
        }

        double roll = random.nextDouble();
        if (roll < 0.58D) {
            return existingCount - 1;
        }

        if (roll < 0.82D) {
            return random.nextInt(existingCount);
        }

        return Math.max(0, existingCount - 2);
    }

    private static double chooseGrowthAngle(SkyPoint parent, int existingCount, double lastAngle, SeededRandom random) {
        if (existingCount <= 1) {
            return random.nextDouble() * Math.PI * 2.0D;
        }

        double roll = random.nextDouble();
        if (roll < 0.44D) {
            return lastAngle + Math.toRadians(random.nextSignedDouble(35.0D));
        }

        if (roll < 0.67D) {
            double side = random.nextBoolean() ? 1.0D : -1.0D;
            return lastAngle + side * Math.PI * 0.5D + Math.toRadians(random.nextSignedDouble(30.0D));
        }

        if (roll < 0.84D) {
            return Math.atan2(-parent.y(), -parent.x()) + Math.toRadians(random.nextSignedDouble(55.0D));
        }

        return random.nextDouble() * Math.PI * 2.0D;
    }

    private static boolean isValidLocalConstellationPoint(SkyPoint point, List<SkyPoint> existingPoints) {
        if (length(point) > MAX_CONSTELLATION_LOCAL_RADIUS_DEGREES) {
            return false;
        }

        for (SkyPoint existing : existingPoints) {
            if (distance(point, existing) < MIN_CONSTELLATION_STAR_DISTANCE_DEGREES) {
                return false;
            }
        }

        return true;
    }

    private static GrowthCandidate findFallbackGrowthPoint(List<SkyPoint> existingPoints, double lastAngle) {
        for (int parentIndex = existingPoints.size() - 1; parentIndex >= 0; parentIndex--) {
            SkyPoint parent = existingPoints.get(parentIndex);

            for (int step = 0; step < 48; step++) {
                double angle = lastAngle + step * (Math.PI * 2.0D / 48.0D);
                SkyPoint point = new SkyPoint(
                        parent.x() + Math.cos(angle) * MIN_CONSTELLATION_STAR_DISTANCE_DEGREES,
                        parent.y() + Math.sin(angle) * MIN_CONSTELLATION_STAR_DISTANCE_DEGREES
                );

                if (isValidLocalConstellationPoint(point, existingPoints)) {
                    return new GrowthCandidate(point, angle);
                }
            }
        }

        SkyPoint last = existingPoints.get(existingPoints.size() - 1);
        SkyPoint point = new SkyPoint(
                last.x() + Math.cos(lastAngle) * MIN_CONSTELLATION_STAR_DISTANCE_DEGREES,
                last.y() + Math.sin(lastAngle) * MIN_CONSTELLATION_STAR_DISTANCE_DEGREES
        );
        return new GrowthCandidate(point, lastAngle);
    }

    private static GeneratedShape centerConstellationShape(List<SkyPoint> rawPoints) {
        if (rawPoints.isEmpty()) {
            return new GeneratedShape(List.of(), 0.0D);
        }

        double averageX = 0.0D;
        double averageY = 0.0D;
        for (SkyPoint point : rawPoints) {
            averageX += point.x();
            averageY += point.y();
        }
        averageX /= rawPoints.size();
        averageY /= rawPoints.size();

        List<SkyPoint> centeredPoints = new ArrayList<>(rawPoints.size());
        double radius = 0.0D;
        for (SkyPoint point : rawPoints) {
            SkyPoint centered = new SkyPoint(point.x() - averageX, point.y() - averageY);
            centeredPoints.add(centered);
            radius = Math.max(radius, length(centered));
        }

        return new GeneratedShape(centeredPoints, radius);
    }

    private static SkyPoint chooseConstellationCenter(
            LocalConstellation local,
            List<PlacedConstellation> placedConstellations,
            SeededRandom random,
            long skySeed,
            int constellationIndex
    ) {
        double margin = local.radiusDegrees() + CONSTELLATION_EDGE_MARGIN_DEGREES;
        double minCenterRadius = Math.min(MAX_MOON_OFFSET_DEGREES, MIN_MOON_OFFSET_DEGREES + margin);
        double maxCenterRadius = Math.max(minCenterRadius, MAX_MOON_OFFSET_DEGREES - margin);

        SkyPoint best = null;
        double bestScore = Double.MAX_VALUE;

        for (int attempt = 0; attempt < CONSTELLATION_CENTER_ATTEMPTS; attempt++) {
            SkyPoint candidate = randomCenterPoint(random, minCenterRadius, maxCenterRadius);
            double score = constellationPlacementScore(candidate, local, placedConstellations);
            if (score <= 0.0D) {
                return candidate;
            }

            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }

        long fallbackSeed = mix64(skySeed ^ ((long) constellationIndex * 0x94d049bb133111ebL) ^ 0x46414c4c4241434bL);
        for (int attempt = 0; attempt < CONSTELLATION_FALLBACK_CENTER_ATTEMPTS; attempt++) {
            double angle = unsignedUnit(mix64(fallbackSeed + attempt * 0x9E3779B97F4A7C15L)) * Math.PI * 2.0D
                    + attempt * 2.399963229728653D;
            double ring = CONSTELLATION_FALLBACK_CENTER_ATTEMPTS <= 1
                    ? 0.0D
                    : (double) attempt / (double) (CONSTELLATION_FALLBACK_CENTER_ATTEMPTS - 1);
            double radius = Mth.lerp(ring, minCenterRadius, maxCenterRadius);
            SkyPoint candidate = new SkyPoint(Math.cos(angle) * radius, Math.sin(angle) * radius);
            double score = constellationPlacementScore(candidate, local, placedConstellations);
            if (score <= 0.0D) {
                return candidate;
            }

            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }

        return best == null ? new SkyPoint(minCenterRadius, 0.0D) : best;
    }

    private static SkyPoint randomCenterPoint(SeededRandom random, double minCenterRadius, double maxCenterRadius) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radiusT = Math.sqrt(random.nextDouble());
        double radius = Mth.lerp(radiusT, minCenterRadius, maxCenterRadius);
        return new SkyPoint(Math.cos(angle) * radius, Math.sin(angle) * radius);
    }

    private static double constellationPlacementScore(
            SkyPoint center,
            LocalConstellation local,
            List<PlacedConstellation> placedConstellations
    ) {
        double score = 0.0D;
        List<SkyPoint> candidatePoints = new ArrayList<>(local.points().size());

        for (SkyPoint localPoint : local.points()) {
            SkyPoint finalPoint = new SkyPoint(center.x() + localPoint.x(), center.y() + localPoint.y());
            candidatePoints.add(finalPoint);

            double radius = length(finalPoint);
            if (radius < MIN_MOON_OFFSET_DEGREES) {
                double miss = MIN_MOON_OFFSET_DEGREES - radius;
                score += miss * miss * 200.0D;
            }

            if (radius > MAX_MOON_OFFSET_DEGREES) {
                double miss = radius - MAX_MOON_OFFSET_DEGREES;
                score += miss * miss * 200.0D;
            }
        }

        for (PlacedConstellation placed : placedConstellations) {
            double centerDistance = distance(center, new SkyPoint(placed.centerX(), placed.centerY()));
            double requiredCenterDistance = local.radiusDegrees()
                    + placed.radiusDegrees()
                    + INTER_CONSTELLATION_STAR_DISTANCE_DEGREES;

            if (centerDistance < requiredCenterDistance) {
                double miss = requiredCenterDistance - centerDistance;
                score += miss * miss * 80.0D;
            }

            for (SkyPoint candidatePoint : candidatePoints) {
                for (SkyPoint placedPoint : placed.finalPoints()) {
                    double pointDistance = distance(candidatePoint, placedPoint);
                    if (pointDistance < INTER_CONSTELLATION_STAR_DISTANCE_DEGREES) {
                        double miss = INTER_CONSTELLATION_STAR_DISTANCE_DEGREES - pointDistance;
                        score += miss * miss * 120.0D;
                    }
                }
            }
        }

        return score;
    }

    private static Vec3 moonRelativeSkyDirectionFor(StarPlacement placement, double celestialAngleDegrees) {
        double rightRadians = Math.toRadians(placement.offsetRightDegrees());
        double upRadians = Math.toRadians(placement.offsetUpDegrees());

        /*
         * Treat the generated 2D constellation layout as tangent-plane angular offsets from the moon.
         * Keeping every offset below 90 degrees guarantees the stars remain on the moon-side hemisphere.
         */
        Vec3 baseDirection = BASE_MOON_DIRECTION
                .add(BASE_MOON_TANGENT_RIGHT.scale(Math.tan(rightRadians)))
                .add(BASE_MOON_TANGENT_UP.scale(Math.tan(upRadians)))
                .normalize();

        return applyVanillaCelestialRotation(baseDirection, celestialAngleDegrees).normalize();
    }

    private static Vec3 applyVanillaCelestialRotation(Vec3 vector, double celestialAngleDegrees) {
        /*
         * Vanilla renders sun and moon with this transform order:
         *   Y axis -90 degrees, then X axis timeOfDay * 360 degrees.
         * For a vertex this means the direction is rotated around X first, then around Y.
         */
        return rotateAroundY(rotateAroundX(vector, celestialAngleDegrees), VANILLA_SKY_YAW_DEGREES);
    }

    private static Vec3 rotateAroundX(Vec3 vector, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double y = vector.y * cos - vector.z * sin;
        double z = vector.y * sin + vector.z * cos;
        return new Vec3(vector.x, y, z);
    }

    private static Vec3 rotateAroundY(Vec3 vector, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vector.x * cos + vector.z * sin;
        double z = -vector.x * sin + vector.z * cos;
        return new Vec3(x, vector.y, z);
    }

    private static double distance(SkyPoint a, SkyPoint b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double length(SkyPoint point) {
        return Math.sqrt(point.x() * point.x() + point.y() * point.y());
    }

    private static long starSeedFor(CelestialMappingMenu.CelestialBodyView body, long skySeed) {
        long value = skySeed;
        value ^= ((long) body.resourceId().hashCode()) * 0x9E3779B97F4A7C15L;
        value ^= Long.rotateLeft((long) body.celestialName().hashCode(), 32);
        return mix64(value);
    }

    private static double unsignedUnit(long value) {
        long positive = value >>> 1;
        return positive / (double) Long.MAX_VALUE;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private record StarPlacement(
            CelestialMappingMenu.CelestialBodyView body,
            double offsetRightDegrees,
            double offsetUpDegrees
    ) {
    }

    private record SkyPoint(double x, double y) {
    }

    private record GrowthCandidate(SkyPoint point, double angleRadians) {
    }

    private record GeneratedShape(List<SkyPoint> points, double radiusDegrees) {
    }

    private record LocalConstellation(
            List<CelestialMappingMenu.CelestialBodyView> bodies,
            List<SkyPoint> points,
            double radiusDegrees
    ) {
    }

    private record PlacedConstellation(
            double centerX,
            double centerY,
            double radiusDegrees,
            List<SkyPoint> finalPoints
    ) {
    }

    private static final class SeededRandom {
        private long state;

        private SeededRandom(long seed) {
            this.state = seed;
        }

        private double nextDouble() {
            state += 0x9E3779B97F4A7C15L;
            return unsignedUnit(mix64(state));
        }

        private double nextSignedDouble(double maxAbsoluteValue) {
            return (nextDouble() * 2.0D - 1.0D) * maxAbsoluteValue;
        }

        private int nextInt(int bound) {
            if (bound <= 1) {
                return 0;
            }

            return Math.floorMod((int) (nextDouble() * bound), bound);
        }

        private boolean nextBoolean() {
            return nextDouble() < 0.5D;
        }
    }
}
