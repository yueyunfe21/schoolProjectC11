package com.bot.dhxy.tools;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 坐标与矩阵计算核心大脑。
 */
@Component
@Slf4j
public class CoordinateHelper {

    private final GameClientTracker tracker;
    private final WindowScopedTempPath windowScopedTempPath;
    private final Random random = new Random();

    public CoordinateHelper(@Lazy GameClientTracker tracker,
                            WindowScopedTempPath windowScopedTempPath) {
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
        BufferedImage raw = ImagePreprocessor.pathToBufferedImage(rawScanPath);
        if (raw == null) {
            return null;
        }
        BufferedImage washed = ImagePreprocessor.washGreenTextToBlackAndWhite(raw);
        raw.flush();
        if (washed == null || !ImagePreprocessor.saveImage(washed, washedScanPath)) {
            if (washed != null) {
                washed.flush();
            }
            log.info("Green text wash missed [{}]", templatePath);
            return null;
        }
        washed.flush();

        double[] result = ImageFinder.find(washedScanPath, templatePath, matchRate);
        if (result != null && result.length >= 2) {
            Point absolute = resolveMatchedPointInRect(rect, result);
            log.info("Green text matched [{}] at ({},{})", templatePath, absolute.x, absolute.y);
            return absolute;
        }
        return null;
    }
}
