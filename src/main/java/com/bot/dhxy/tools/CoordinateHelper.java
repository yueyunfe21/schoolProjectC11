package com.bot.dhxy.tools;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
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
import java.util.HashMap;
import java.util.Map;
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

    private static final String MAP_CONFIG_PATH = "config/maps.json";
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
        int absoluteX = (int) Math.round(tracker.getWindowBaseX() + transform.zeroOffsetX + logicalX * transform.scaleX);
        int absoluteY = (int) Math.round(tracker.getWindowBaseY() + transform.zeroOffsetY + logicalY * transform.scaleY);
        return new Point(absoluteX, absoluteY);
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

    public double getScaleRatio() {
        return this.systemScaleRatio;
    }

    public int[] getScaledRect(int offsetX, int offsetY, int width, int height) {
        int xStart = tracker.getWindowBaseX() + offsetX;
        int yStart = tracker.getWindowBaseY() + offsetY;
        int xEnd = xStart + width;
        int yEnd = yStart + height;
        return new int[]{xStart, yStart, xEnd, yEnd};
    }

    public int[] getOffsets(int physicalX, int physicalY) {
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

    public Point findImageInRegion(String templatePath, int[] rect, double matchRate) {
        if (!tracker.bringWindowToFront()) {
            log.warn("Region image search failed because game window cannot focus");
            return null;
        }

        String roiPath = windowScopedTempPath.resolve("roi_scan.png");
        if (!tracker.captureToFile("ROI-Scan", roiPath, rect[0], rect[1], rect[2], rect[3])) {
            return null;
        }

        double[] result = ImageFinder.find(roiPath, templatePath, matchRate);

        if (result != null && result.length >= 2) {
            int absoluteX = rect[0] + (int) Math.round(result[0]);
            int absoluteY = rect[1] + (int) Math.round(result[1]);
            log.info("Region image matched [{}] at ({},{})", templatePath, absoluteX, absoluteY);
            return new Point(absoluteX, absoluteY);
        }

        return null;
    }

    public Point findGreenTextInRegion(String templatePath, int[] rect, double matchRate) {
        if (!tracker.bringWindowToFront()) {
            log.warn("Green text search failed because game window cannot focus");
            return null;
        }
        String rawScanPath = windowScopedTempPath.resolve("tem_dialog_cut.png");

        if (!tracker.captureToFile("临时截图处理黑白", rawScanPath, rect[0], rect[1], rect[2], rect[3])) {
            return null;
        }
        String washedScanPath = windowScopedTempPath.resolve("tem_dialog_cut_washed.png");
        ImagePreprocessor.washGreenTextToBlackAndWhite(rawScanPath, washedScanPath);

        double[] result = ImageFinder.find(washedScanPath, templatePath, matchRate);
        if (result != null && result.length >= 2) {
            int absoluteX = rect[0] + (int) Math.round(result[0]);
            int absoluteY = rect[1] + (int) Math.round(result[1]);
            log.info("Green text matched [{}] at ({},{})", templatePath, absoluteX, absoluteY);
            return new Point(absoluteX, absoluteY);
        }
        return null;
    }
}
