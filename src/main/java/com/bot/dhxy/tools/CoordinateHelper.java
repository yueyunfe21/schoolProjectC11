package com.bot.dhxy.tools;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 坐标与矩阵计算核心大脑。
 */
@Component
@Slf4j
public class CoordinateHelper {

    private final GameClientTracker tracker;
    private final WindowScopedTempPath windowScopedTempPath;
    private final Random random = new Random();

    private static final String MAP_CONFIG_PATH = "config/maps.json";
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int APPROACH_LOGICAL_OFFSET = 2;
    private static final int MINI_MAP_EDGE_INSET_TRIGGER_PX = 240;
    private static final int MINI_MAP_CLICK_RANDOM_RADIUS_PX = 4;
    private static final int MINI_MAP_FALLBACK_MAX_RING_RADIUS = 10;
    private Map<String, MapTransform> mapTransforms = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CoordinateHelper(@Lazy GameClientTracker tracker, WindowScopedTempPath windowScopedTempPath) {
        this.tracker = tracker;
        this.windowScopedTempPath = windowScopedTempPath;
    }

    private double systemScaleRatio = 1.0;

    @PostConstruct
    public void initScaleRatio() {
        try {
            GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            AffineTransform transform = config.getDefaultTransform();
            this.systemScaleRatio = transform.getScaleX();
            log.info("System scale ratio: {}", this.systemScaleRatio);
        } catch (Exception e) {
            log.error("Failed to detect system scale, fallback to 1.0", e);
            this.systemScaleRatio = 1.0;
        }
    }

    public static class MapTransform {
        public int zeroOffsetX;
        public int zeroOffsetY;
        public double scaleX;
        public double scaleY;

        public MapTransform() {}

        public MapTransform(int zeroOffsetX, int zeroOffsetY, double scaleX, double scaleY) {
            this.zeroOffsetX = zeroOffsetX;
            this.zeroOffsetY = zeroOffsetY;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }
    }

    @PostConstruct
    public void loadMapConfig() {
        File file = new File(MAP_CONFIG_PATH);
        if (!file.exists()) {
            log.warn("Map config {} not found, using empty map transforms", MAP_CONFIG_PATH);
            file.getParentFile().mkdirs();
            return;
        }
        try {
            mapTransforms = objectMapper.readValue(file, new TypeReference<Map<String, MapTransform>>() {});
            log.info("Loaded {} map transforms", mapTransforms.size());
        } catch (IOException e) {
            log.error("Failed to read map config", e);
        }
    }

    public void saveNewMapConfig(String mapName, MapTransform transform) {
        mapTransforms.put(mapName, transform);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(MAP_CONFIG_PATH), mapTransforms);
            log.info("Saved map transform: {}", mapName);
        } catch (IOException e) {
            log.error("Failed to save map config", e);
        }
    }

    public Point getPhysicalMapPoint(String mapName, int logicalX, int logicalY) {
        MapTransform transform = mapTransforms.get(mapName);
        if (transform == null) {
            log.error("Missing map transform: {}", mapName);
            return null;
        }
        tracker.refreshWindowState();
        int absoluteX = (int) Math.round(tracker.getWindowBaseX() + transform.zeroOffsetX + logicalX * transform.scaleX);
        int absoluteY = (int) Math.round(tracker.getWindowBaseY() + transform.zeroOffsetY + logicalY * transform.scaleY);
        return new Point(absoluteX, absoluteY);
    }

    public MapTransform getMapTransform(String mapName) {
        return mapTransforms.get(mapName);
    }

    /**
     * Check whether a logical map coordinate is plausible for the configured mini-map transform.
     *
     * <p>Location OCR can occasionally read an extra digit, for example {@code 135} as
     * {@code 1135}. A coordinate like that maps far outside the 1024x768 game client and must not be
     * written into runtime player state. This guard validates the logical coordinate in window-local
     * mini-map pixel space without using or changing the current window focus.</p>
     *
     * @param mapName configured in-game map name; unknown maps return true so new maps are not
     *                blocked before calibration.
     * @param logicalX OCR/template logical X coordinate.
     * @param logicalY OCR/template logical Y coordinate.
     * @param marginPx tolerated pixel overflow around the 1024x768 client for edge labels/jitter.
     * @return false when the known transform maps the coordinate far outside the game client.
     */
    public boolean isLogicalCoordinatePlausible(String mapName, int logicalX, int logicalY, int marginPx) {
        MapTransform transform = mapTransforms.get(mapName);
        if (transform == null) {
            return true;
        }
        double relativeX = transform.zeroOffsetX + logicalX * transform.scaleX;
        double relativeY = transform.zeroOffsetY + logicalY * transform.scaleY;
        int margin = Math.max(0, marginPx);
        return relativeX >= -margin
                && relativeX <= GAME_CLIENT_WIDTH + margin
                && relativeY >= -margin
                && relativeY <= GAME_CLIENT_HEIGHT + margin;
    }

    /**
     * Resolve the mini-map logical coordinate and screen-absolute click point for a navigation attempt.
     *
     * <p>{@code failedClickCount == 0} returns the original target point. Higher values calculate one
     * fallback logical coordinate at a time, using the original target's screen-relative position to
     * bias edge fallbacks away from the map border before trying nearby coordinates. This keeps
     * NavigationService from pre-building candidate lists or duplicating map-transform math.</p>
     *
     * @param mapName map name whose transform is registered in {@code config/maps.json}; null or
     *                unknown maps return null.
     * @param targetX original logical in-game X coordinate.
     * @param targetY original logical in-game Y coordinate.
     * @param failedClickCount number of prior mini-map clicks that did not start pathing; zero means
     *                         use the original point.
     * @return resolved logical coordinate, screen-absolute pixel point, and diagnostic reason; null
     *         when the transform is missing or all fallback points are exhausted.
     */
    public MiniMapClickPoint resolveMiniMapClickPoint(String mapName,
                                                      int targetX,
                                                      int targetY,
                                                      int failedClickCount) {
        Point originalPixelPoint = getPhysicalMapPoint(mapName, targetX, targetY);
        if (originalPixelPoint == null) {
            return null;
        }
        if (failedClickCount <= 0) {
            return buildMiniMapClickPoint(targetX, targetY, originalPixelPoint, "original");
        }

        MapTransform transform = getMapTransform(mapName);
        if (transform == null) {
            return null;
        }

        tracker.refreshWindowState();
        int relativeX = originalPixelPoint.x - tracker.getWindowBaseX();
        int relativeY = originalPixelPoint.y - tracker.getWindowBaseY();
        int dx = 0;
        int dy = 0;
        if (relativeX <= MINI_MAP_EDGE_INSET_TRIGGER_PX) {
            dx = logicalStepForPixelDirection(1, transform.scaleX);
        } else if (GAME_CLIENT_WIDTH - relativeX <= MINI_MAP_EDGE_INSET_TRIGGER_PX) {
            dx = logicalStepForPixelDirection(-1, transform.scaleX);
        }
        if (relativeY <= MINI_MAP_EDGE_INSET_TRIGGER_PX) {
            dy = logicalStepForPixelDirection(1, transform.scaleY);
        } else if (GAME_CLIENT_HEIGHT - relativeY <= MINI_MAP_EDGE_INSET_TRIGGER_PX) {
            dy = logicalStepForPixelDirection(-1, transform.scaleY);
        }

        log.info("mini-map fallback analysis after failed click: map={} target=({}, {}) pixel=({}, {}) relative=({}, {}) dx={} dy={} failedClicks={}",
                mapName, targetX, targetY, originalPixelPoint.x, originalPixelPoint.y,
                relativeX, relativeY, dx, dy, failedClickCount);

        List<int[]> fallbackOffsets = buildMiniMapFallbackOffsets(dx, dy);
        int fallbackIndex = failedClickCount - 1;
        if (fallbackIndex >= 0 && fallbackIndex < fallbackOffsets.size()) {
            int[] offset = fallbackOffsets.get(fallbackIndex);
            return resolveMiniMapFallbackPoint(mapName, targetX + offset[0], targetY + offset[1],
                    "fallback-" + failedClickCount + "-offset=(" + offset[0] + "," + offset[1] + ")");
        }
        return null;
    }

    /**
     * Build mini-map retry offsets from small nudges to wider rings.
     *
     * <p>Some cave maps, especially 蜂巢, contain hollow areas that repeatedly swallow tiny
     * +/-1 retries. Keep edge-biased offsets first when the target is near a visible map border,
     * then expand in logical-coordinate rings so later retries actually escape the same hollow
     * patch. A linked set keeps the sequence deterministic and prevents duplicate logical points.</p>
     */
    private List<int[]> buildMiniMapFallbackOffsets(int edgeDx, int edgeDy) {
        Set<String> seen = new LinkedHashSet<>();
        List<int[]> offsets = new ArrayList<>();

        addMiniMapOffset(offsets, seen, 0, edgeDy);
        addMiniMapOffset(offsets, seen, 0, edgeDy * 2);
        addMiniMapOffset(offsets, seen, edgeDx, 0);
        addMiniMapOffset(offsets, seen, edgeDx * 2, 0);
        addMiniMapOffset(offsets, seen, edgeDx, edgeDy);
        addMiniMapOffset(offsets, seen, edgeDx * 2, edgeDy * 2);

        for (int radius = 1; radius <= MINI_MAP_FALLBACK_MAX_RING_RADIUS; radius++) {
            addMiniMapOffset(offsets, seen, 0, -radius);
            addMiniMapOffset(offsets, seen, 0, radius);
            addMiniMapOffset(offsets, seen, -radius, 0);
            addMiniMapOffset(offsets, seen, radius, 0);
            addMiniMapOffset(offsets, seen, -radius, -radius);
            addMiniMapOffset(offsets, seen, -radius, radius);
            addMiniMapOffset(offsets, seen, radius, -radius);
            addMiniMapOffset(offsets, seen, radius, radius);
        }
        return offsets;
    }

    private void addMiniMapOffset(List<int[]> offsets, Set<String> seen, int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        String key = dx + "," + dy;
        if (seen.add(key)) {
            offsets.add(new int[] {dx, dy});
        }
    }

    private MiniMapClickPoint resolveMiniMapFallbackPoint(String mapName, int logicalX, int logicalY, String reason) {
        Point pixelPoint = getPhysicalMapPoint(mapName, logicalX, logicalY);
        return pixelPoint == null ? null : buildMiniMapClickPoint(logicalX, logicalY, pixelPoint, reason);
    }

    private MiniMapClickPoint buildMiniMapClickPoint(int logicalX, int logicalY, Point basePixelPoint, String reason) {
        Point randomizedPixelPoint = randomizeMiniMapClickPoint(basePixelPoint);
        int jitterX = randomizedPixelPoint.x - basePixelPoint.x;
        int jitterY = randomizedPixelPoint.y - basePixelPoint.y;
        return MiniMapClickPoint.builder()
                .logicalX(logicalX)
                .logicalY(logicalY)
                .basePixelPoint(basePixelPoint)
                .pixelPoint(randomizedPixelPoint)
                .jitterX(jitterX)
                .jitterY(jitterY)
                .reason(reason)
                .build();
    }

    private Point randomizeMiniMapClickPoint(Point basePixelPoint) {
        tracker.refreshWindowState();
        int offsetX = random.nextInt(MINI_MAP_CLICK_RANDOM_RADIUS_PX * 2 + 1) - MINI_MAP_CLICK_RANDOM_RADIUS_PX;
        int offsetY = random.nextInt(MINI_MAP_CLICK_RANDOM_RADIUS_PX * 2 + 1) - MINI_MAP_CLICK_RANDOM_RADIUS_PX;
        if (offsetX == 0 && offsetY == 0) {
            offsetX = 1;
        }

        int minX = tracker.getWindowBaseX() + 2;
        int maxX = tracker.getWindowBaseX() + GAME_CLIENT_WIDTH - 2;
        int minY = tracker.getWindowBaseY() + 2;
        int maxY = tracker.getWindowBaseY() + GAME_CLIENT_HEIGHT - 2;
        int randomizedX = clamp(basePixelPoint.x + offsetX, minX, maxX);
        int randomizedY = clamp(basePixelPoint.y + offsetY, minY, maxY);
        return new Point(randomizedX, randomizedY);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Resolved mini-map click target.
     *
     * @param logicalX logical in-game X coordinate that will be clicked on the mini-map.
     * @param logicalY logical in-game Y coordinate that will be clicked on the mini-map.
     * @param basePixelPoint screen-absolute point calculated directly from the map transform, before
     *                       physical-click randomization.
     * @param pixelPoint final screen-absolute pixel point for the physical click.
     * @param jitterX X random offset in screen pixels applied to {@code basePixelPoint}.
     * @param jitterY Y random offset in screen pixels applied to {@code basePixelPoint}.
     * @param reason diagnostic label such as {@code original}, {@code edge-fallback-1}, or
     *               {@code near-fallback-3}.
     */
    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    public static class MiniMapClickPoint {
        int logicalX;
        int logicalY;
        Point basePixelPoint;
        Point pixelPoint;
        int jitterX;
        int jitterY;
        String reason;
    }

    /**
     * Calculate a nearby logical coordinate for task targets that should be approached, not stood on.
     *
     * <p>The input and output coordinates are logical in-game map coordinates. The helper uses the
     * current bound window base plus the map transform to infer which side of the visible game area
     * the target is on, then offsets the coordinate slightly toward the screen center/interior. This
     * keeps task code from duplicating map-transform math while keeping navigation itself unaware of
     * business concepts such as 修罗怪 or NPC interaction policy.</p>
     *
     * @param mapName map name whose transform is registered in {@code config/maps.json}; nullable
     *                or unknown maps return the original coordinate.
     * @param targetX original logical in-game X coordinate.
     * @param targetY original logical in-game Y coordinate.
     * @return logical in-game coordinate near the target; returns the original coordinate when the
     *         transform or physical point cannot be calculated.
     */
    public MapCoordinate calculateApproachCoordinate(String mapName, int targetX, int targetY) {
        if (shouldUseOriginalTargetCoordinate(mapName)) {
            /*
             * 龙窟/凤巢有大量中空地形。这里如果把修罗目标改成“靠近点”，
             * 小地图点击很容易落到不可走空洞，反而比原始任务坐标更不稳。
             */
            log.info("approach coordinate skipped for cave map: map={} target=({}, {})",
                    mapName, targetX, targetY);
            return new MapCoordinate(targetX, targetY);
        }

        Point originalPixelPoint = getPhysicalMapPoint(mapName, targetX, targetY);
        MapTransform transform = getMapTransform(mapName);
        if (originalPixelPoint == null || transform == null) {
            log.warn("approach coordinate fallback to original: map={} target=({}, {}) transformMissing={} pixelMissing={}",
                    mapName, targetX, targetY, transform == null, originalPixelPoint == null);
            return new MapCoordinate(targetX, targetY);
        }

        tracker.refreshWindowState();
        int relativeX = originalPixelPoint.x - tracker.getWindowBaseX();
        int relativeY = originalPixelPoint.y - tracker.getWindowBaseY();
        int pixelDirectionX = relativeX < GAME_CLIENT_WIDTH / 2 ? 1 : -1;
        int pixelDirectionY = relativeY < GAME_CLIENT_HEIGHT / 2 ? 1 : -1;
        int dx = logicalStepForPixelDirection(pixelDirectionX, transform.scaleX);
        int dy = logicalStepForPixelDirection(pixelDirectionY, transform.scaleY);
        int approachX = targetX + dx * APPROACH_LOGICAL_OFFSET;
        int approachY = targetY + dy * APPROACH_LOGICAL_OFFSET;

        log.info("approach coordinate calculated: map={} target=({}, {}) approach=({}, {}) relative=({}, {}) logicalStep=({}, {})",
                mapName, targetX, targetY, approachX, approachY, relativeX, relativeY, dx, dy);
        return new MapCoordinate(approachX, approachY);
    }

    private boolean shouldUseOriginalTargetCoordinate(String mapName) {
        return mapName != null && (mapName.contains("龙窟") || mapName.contains("凤巢"));
    }

    private int logicalStepForPixelDirection(int pixelDirection, double scale) {
        if (scale == 0) {
            return 0;
        }
        return pixelDirection / scale > 0 ? 1 : -1;
    }

    public Point getRandomizedPoint(int baseX, int baseY, int maxRadiusX, int maxRadiusY) {
        int offsetX = maxRadiusX <= 0 ? 0 : random.nextInt(maxRadiusX * 2 + 1) - maxRadiusX;
        int offsetY = maxRadiusY <= 0 ? 0 : random.nextInt(maxRadiusY * 2 + 1) - maxRadiusY;
        return new Point(baseX + offsetX, baseY + offsetY);
    }

    public Point getRandomizedPoint(Point base, int maxRadiusX, int maxRadiusY) {
        if (base == null) return null;
        return getRandomizedPoint(base.x, base.y, maxRadiusX, maxRadiusY);
    }

    /**
     * Apply an asymmetric random offset to a screen-absolute point.
     *
     * @param base screen-absolute base point. Null returns null.
     * @param minOffsetX minimum X offset in screen pixels.
     * @param maxOffsetX maximum X offset in screen pixels.
     * @param randomRadiusY symmetric Y random radius in screen pixels. Values less than or equal to
     *                      zero keep Y unchanged.
     * @return randomized screen-absolute point.
     */
    public Point getRandomizedPoint(Point base, int minOffsetX, int maxOffsetX, int randomRadiusY) {
        if (base == null) {
            return null;
        }
        int lowX = Math.min(minOffsetX, maxOffsetX);
        int highX = Math.max(minOffsetX, maxOffsetX);
        int offsetX = lowX == highX ? lowX : random.nextInt(highX - lowX + 1) + lowX;
        int offsetY = randomRadiusY <= 0 ? 0 : random.nextInt(randomRadiusY * 2 + 1) - randomRadiusY;
        return new Point(base.x + offsetX, base.y + offsetY);
    }

    public double getScaleRatio() {
        return this.systemScaleRatio;
    }

    public int[] getScaledRect(int offsetX, int offsetY, int width, int height) {
        tracker.refreshWindowState();
        int xStart = tracker.getWindowBaseX() + offsetX;
        int yStart = tracker.getWindowBaseY() + offsetY;
        int xEnd = xStart + width;
        int yEnd = yStart + height;
        return new int[]{xStart, yStart, xEnd, yEnd};
    }

    public int[] getOffsets(int physicalX, int physicalY) {
        tracker.refreshWindowState();
        int logicalX = (int) Math.round(physicalX / systemScaleRatio) - tracker.getWindowBaseX();
        int logicalY = (int) Math.round(physicalY / systemScaleRatio) - tracker.getWindowBaseY();
        log.info("Offset from window: X={}, Y={}", logicalX, logicalY);
        return new int[]{logicalX, logicalY};
    }

    public Point findImageAbsoluteCoordinate(String templatePath, double matchRate) {
        tracker.updateGlobalVision();
        String screenPath = tracker.getLatestVisionPath();

        double[] result = ImageFinder.find(screenPath, templatePath, matchRate);

        if (result != null && result.length >= 2) {
            tracker.refreshWindowState();
            int absoluteX = (int) Math.round(result[0] / systemScaleRatio) + tracker.getWindowBaseX();
            int absoluteY = (int) Math.round(result[1] / systemScaleRatio) + tracker.getWindowBaseY();
            log.info("Image matched [{}] at absolute coordinate ({},{})", templatePath, absoluteX, absoluteY);
            return new Point(absoluteX, absoluteY);
        }

        return null;
    }

    public Point findImageAbsoluteCoordinateByImagePath(String templatePath, String screenPath, double matchRate) {
        double[] result = ImageFinder.find(screenPath, templatePath, matchRate);

        if (result != null && result.length >= 2) {
            tracker.refreshWindowState();
            int absoluteX = (int) Math.round(result[0] / systemScaleRatio) + tracker.getWindowBaseX();
            int absoluteY = (int) Math.round(result[1] / systemScaleRatio) + tracker.getWindowBaseY();
            log.info("Image matched [{}] at absolute coordinate ({},{})", templatePath, absoluteX, absoluteY);
            return new Point(absoluteX, absoluteY);
        }

        return null;
    }

    public int[] getAbsoluteRectByAnchor(Point anchor, int offsetX, int offsetY, int width, int height) {
        if (anchor == null) return null;
        int startX = anchor.x + offsetX;
        int startY = anchor.y + offsetY;
        int endX = startX + width;
        int endY = startY + height;
        return new int[]{startX, startY, endX, endY};
    }

    /**
     * Convert an image-template match inside a captured rectangle into a screen-absolute point.
     *
     * @param rect screen-absolute capture rectangle as {@code [left, top, right, bottom]}.
     * @param matchResult result returned by {@link ImageFinder#find(String, String, double)}. The
     *                    first two values are image-local X/Y inside {@code rect}.
     * @return screen-absolute point, or null when the inputs do not contain a usable match.
     */
    public Point resolveMatchedPointInRect(int[] rect, double[] matchResult) {
        if (rect == null || rect.length < 2 || matchResult == null || matchResult.length < 2) {
            return null;
        }
        int absoluteX = rect[0] + (int) Math.round(matchResult[0]);
        int absoluteY = rect[1] + (int) Math.round(matchResult[1]);
        return new Point(absoluteX, absoluteY);
    }

    public Point findImageInRegion(String templatePath, int[] rect, double matchRate) {
        String roiPath = windowScopedTempPath.resolve("roi_scan.png");
        if (!tracker.captureToFile("ROI-Scan", roiPath, rect[0], rect[1], rect[2], rect[3])) {
            return null;
        }

        long start = LatencyMetrics.start();
        double[] result;
        try {
            result = ImageFinder.find(roiPath, templatePath, matchRate);
        } catch (Throwable e) {
            log.warn("[latency] event=coordinate.findImageInRegion elapsedMs={} detail={}",
                    LatencyMetrics.elapsedMs(start),
                    "result=exception template=" + templatePath
                            + " roi=" + roiPath
                            + " rect=" + rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3]
                            + " matchRate=" + matchRate
                            + " error=" + e.getClass().getName() + ":" + e.getMessage());
            log.warn("findImageInRegion ImageFinder failed: template={} roi={} rect=({}, {})-({}, {}) matchRate={}",
                    templatePath, roiPath, rect[0], rect[1], rect[2], rect[3], matchRate, e);
            throw e;
        }

        if (result != null && result.length >= 2) {
            Point absolute = resolveMatchedPointInRect(rect, result);
            log.debug("[latency] event=coordinate.findImageInRegion elapsedMs={} detail={}",
                    LatencyMetrics.elapsedMs(start),
                    "result=matched template=" + templatePath
                            + " roi=" + roiPath
                            + " rect=" + rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3]
                            + " point=" + absolute.x + "," + absolute.y);
            log.debug("Region image matched [{}] at ({},{})", templatePath, absolute.x, absolute.y);
            return absolute;
        }

        log.debug("[latency] event=coordinate.findImageInRegion elapsedMs={} detail={}",
                LatencyMetrics.elapsedMs(start),
                "result=miss template=" + templatePath
                        + " roi=" + roiPath
                        + " rect=" + rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3]
                        + " matchRate=" + matchRate);
        return null;
    }

    /**
     * Find multiple template centers inside one screen-absolute capture rectangle.
     *
     * @param templatePath template image path.
     * @param rect screen-absolute rectangle in {@code [left, top, right, bottom]} form.
     * @param matchRate minimum OpenCV correlation score.
     * @param minDistancePx minimum screen-pixel distance for de-duplicating nearby hits.
     * @return score-sorted screen-absolute centers. Empty means no match or capture failure.
     */
    public List<Point> findImagesInRegion(String templatePath, int[] rect, double matchRate, double minDistancePx) {
        String roiPath = windowScopedTempPath.resolve("roi_scan_all.png");
        if (!tracker.captureToFile("ROI-Scan-All", roiPath, rect[0], rect[1], rect[2], rect[3])) {
            return List.of();
        }

        long start = LatencyMetrics.start();
        List<double[]> results;
        try {
            results = ImageFinder.findAll(roiPath, templatePath, matchRate, minDistancePx);
        } catch (Throwable e) {
            log.warn("[latency] event=coordinate.findImagesInRegion elapsedMs={} detail={}",
                    LatencyMetrics.elapsedMs(start),
                    "result=exception template=" + templatePath
                            + " roi=" + roiPath
                            + " rect=" + rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3]
                            + " matchRate=" + matchRate
                            + " error=" + e.getClass().getName() + ":" + e.getMessage());
            throw e;
        }

        if (results == null || results.isEmpty()) {
            log.debug("[latency] event=coordinate.findImagesInRegion elapsedMs={} detail={}",
                    LatencyMetrics.elapsedMs(start),
                    "result=miss template=" + templatePath
                            + " roi=" + roiPath
                            + " rect=" + rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3]
                            + " matchRate=" + matchRate);
            return List.of();
        }

        List<Point> points = new ArrayList<>();
        for (double[] result : results) {
            Point absolute = resolveMatchedPointInRect(rect, result);
            if (absolute != null) {
                points.add(absolute);
            }
        }
        log.info("[latency] event=coordinate.findImagesInRegion elapsedMs={} detail={}",
                LatencyMetrics.elapsedMs(start),
                "result=matched template=" + templatePath
                        + " roi=" + roiPath
                        + " count=" + points.size()
                        + " points=" + points);
        return List.copyOf(points);
    }

    public Point findGreenTextInRegion(String templatePath, int[] rect, double matchRate) {
        String rawScanPath = windowScopedTempPath.resolve("tem_dialog_cut.png");

        if (!tracker.captureToFile("临时截图处理黑白", rawScanPath, rect[0], rect[1], rect[2], rect[3])) {
            return null;
        }
        String washedScanPath = windowScopedTempPath.resolve("tem_dialog_cut_washed.png");
        ImagePreprocessor.washGreenTextToBlackAndWhite(rawScanPath, washedScanPath);

        double[] result = ImageFinder.find(washedScanPath, templatePath, matchRate);
        if (result != null && result.length >= 2) {
            Point absolute = resolveMatchedPointInRect(rect, result);
            log.info("Green text matched [{}] at ({},{})", templatePath, absolute.x, absolute.y);
            return absolute;
        }
        return null;
    }
}
