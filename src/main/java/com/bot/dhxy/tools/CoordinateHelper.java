package com.bot.dhxy.tools;

import com.bot.dhxy.core.GameClientTracker;
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
import com.bot.dhxy.core.ImageFinder;

/**
 * 坐标与矩阵计算核心大脑
 * 专治各种 DPI 缩放换算问题，并自动叠加窗口偏移量
 */
@Component
@Slf4j
public class CoordinateHelper {

    // 🌟 核心防爆指南：这里必须加 @Lazy！
    private final GameClientTracker tracker;

    // 🌟 注入全局统一的随机数发生器 (拟人化防封核心)
    private final Random random = new Random();

    // ==========================================
    // 🗺️ 地图测绘持久化相关组件
    // ==========================================
    private static final String MAP_CONFIG_PATH = "config/maps.json";
    private Map<String, MapTransform> mapTransforms = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CoordinateHelper(@Lazy GameClientTracker tracker) {
        this.tracker = tracker;
    }

    // 唯一的真相：系统实际缩放比例
    private double systemScaleRatio = 1.0;

    @PostConstruct
    public void initScaleRatio() {
        try {
            GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            AffineTransform transform = config.getDefaultTransform();

            this.systemScaleRatio = transform.getScaleX();

            log.info("🖥️ 缩放雷达启动：已自动锁定系统缩放比例为 {}%", (int)(this.systemScaleRatio * 100));
        } catch (Exception e) {
            log.error("⚠️ 自动探测失败，防具破损，默认退回 100% 比例", e);
            this.systemScaleRatio = 1.0;
        }
    }

    // ==========================================
    // 🗺️ 地图参数加载与保存逻辑
    // ==========================================

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
            log.warn("⚠️ 地图配置文件 {} 不存在，初始化为空字典（等雷达测绘后会自动生成）。", MAP_CONFIG_PATH);
            file.getParentFile().mkdirs();
            return;
        }
        try {
            mapTransforms = objectMapper.readValue(file, new TypeReference<Map<String, MapTransform>>() {});
            log.info("✅ 成功加载了 {} 张地图的坐标配置！", mapTransforms.size());
        } catch (IOException e) {
            log.error("❌ 读取地图配置文件失败！", e);
        }
    }

    public void saveNewMapConfig(String mapName, MapTransform transform) {
        mapTransforms.put(mapName, transform);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(MAP_CONFIG_PATH), mapTransforms);
            log.info("💾 地图 [{}] 的坐标数据已成功持久化保存到 JSON！", mapName);
        } catch (IOException e) {
            log.error("❌ 保存地图配置到文件失败！", e);
        }
    }

    public Point getPhysicalMapPoint(String mapName, int logicalX, int logicalY) {
        MapTransform transform = mapTransforms.get(mapName);
        if (transform == null) {
            log.error("❌ 字典中未配置地图 [{}] 的数据，请先运行测绘雷达！", mapName);
            return null;
        }
        int absoluteX = (int) Math.round(tracker.getWindowBaseX() + transform.zeroOffsetX + logicalX * transform.scaleX);
        int absoluteY = (int) Math.round(tracker.getWindowBaseY() + transform.zeroOffsetY + logicalY * transform.scaleY);

        return new Point(absoluteX, absoluteY);
    }

    // ==========================================
    // 🎭 拟人化引擎：坐标防封抖动发生器
    // ==========================================

    /**
     * 为绝对坐标注入灵魂（随机抖动），模拟人类点击的误差
     * @param baseX 原始绝对 X 坐标
     * @param baseY 原始绝对 Y 坐标
     * @param maxRadiusX X 轴最大允许的左右偏移量 (像素)
     * @param maxRadiusY Y 轴最大允许的上下偏移量 (像素)
     * @return 抖动后的安全物理坐标
     */
    public Point getRandomizedPoint(int baseX, int baseY, int maxRadiusX, int maxRadiusY) {
        int offsetX = maxRadiusX <= 0 ? 0 : random.nextInt(maxRadiusX * 2 + 1) - maxRadiusX;
        int offsetY = maxRadiusY <= 0 ? 0 : random.nextInt(maxRadiusY * 2 + 1) - maxRadiusY;
        return new Point(baseX + offsetX, baseY + offsetY);
    }

    /**
     * 重载版本：直接传入 Point 对象
     */
    public Point getRandomizedPoint(Point base, int maxRadiusX, int maxRadiusY) {
        if (base == null) return null;
        return getRandomizedPoint(base.x, base.y, maxRadiusX, maxRadiusY);
    }

    // ==========================================
    // 🧱 像素比例换算逻辑
    // ==========================================

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
        log.info("📍 反算相对偏移 -> X:{}, Y:{}", logicalX, logicalY);
        return new int[]{logicalX, logicalY};
    }

    // ==========================================
    // 👁️ 万能寻图雷达 (视觉定位转换)
    // ==========================================

    public Point findImageAbsoluteCoordinate(String templatePath, double matchRate) {
        if (!tracker.bringWindowToFront()) {
            log.warn("❌ [坐标计算] 游戏窗口无法置顶！");
            return null;
        }

        tracker.updateGlobalVision();
        String screenPath = GameClientTracker.LATEST_VISION_PATH;

        double[] result = ImageFinder.find(screenPath, templatePath, matchRate);

        if (result != null && result.length >= 2) {
            int absoluteX = (int) Math.round(result[0] / systemScaleRatio) + tracker.getWindowBaseX();
            int absoluteY = (int) Math.round(result[1] / systemScaleRatio) + tracker.getWindowBaseY();

            log.info("✅ [坐标雷达] 锁定目标 [{}] 中心点，屏幕绝对坐标:({},{})", templatePath, absoluteX, absoluteY);
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

    // ==========================================
    // 🎯 新增：指定区域找图 (ROI 局部雷达)
    // ==========================================
    public Point findImageInRegion(String templatePath, int[] rect, double matchRate) {
        if (!tracker.bringWindowToFront()) {
            log.warn("❌ [局部雷达] 游戏窗口无法置顶！");
            return null;
        }

        String roiPath = "images/temp/roi_scan.png";
        // rect[0]=startX, rect[1]=startY, rect[2]=endX, rect[3]=endY
        if (!tracker.captureToFile("ROI-Scan", roiPath, rect[0], rect[1], rect[2], rect[3])) {
            return null;
        }

        double[] result = ImageFinder.find(roiPath, templatePath, matchRate);

        if (result != null && result.length >= 2) {
            // 参考您在 P2 打怪阶段写的最稳计算公式：局部偏移直接加到矩形起点上
            int absoluteX = rect[0] + (int) Math.round(result[0]);
            int absoluteY = rect[1] + (int) Math.round(result[1]);

            log.info("✅ [局部雷达] 在指定区域内锁定目标 [{}] 中心点，绝对坐标:({},{})", templatePath, absoluteX, absoluteY);
            return new Point(absoluteX, absoluteY);
        }

        return null;
    }
}