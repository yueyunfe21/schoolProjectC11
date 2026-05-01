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

/**
 * 坐标与矩阵计算核心大脑
 * 专治各种 DPI 缩放换算问题，并自动叠加窗口偏移量
 */
@Component
@Slf4j
public class CoordinateHelper {

    // 🌟 核心防爆指南：这里必须加 @Lazy！
    private final GameClientTracker tracker;

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
    // 🗺️ 地图参数加载与保存逻辑 (新加入)
    // ==========================================

    /**
     * 地图矩阵基因数据模型
     */
    public static class MapTransform {
        public int zeroOffsetX; // (0,0)点相对于游戏窗口左上角的物理X偏移
        public int zeroOffsetY; // (0,0)点相对于游戏窗口左上角的物理Y偏移
        public double scaleX;   // X轴缩放比
        public double scaleY;   // Y轴缩放比

        public MapTransform() {} // 供 Jackson 反序列化使用

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

    /**
     * 供【全自动悬停测绘雷达】调用，将新测出的地图数据保存到 JSON 硬盘中
     */
    public void saveNewMapConfig(String mapName, MapTransform transform) {
        mapTransforms.put(mapName, transform);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(MAP_CONFIG_PATH), mapTransforms);
            log.info("💾 地图 [{}] 的坐标数据已成功持久化保存到 JSON！", mapName);
        } catch (IOException e) {
            log.error("❌ 保存地图配置到文件失败！", e);
        }
    }

    /**
     * 供【寻路引擎】调用：传入地图名和游戏逻辑坐标，直接返回要点击的屏幕物理点
     */
    public Point getPhysicalMapPoint(String mapName, int logicalX, int logicalY) {
        MapTransform transform = mapTransforms.get(mapName);
        if (transform == null) {
            log.error("❌ 字典中未配置地图 [{}] 的数据，请先运行测绘雷达！", mapName);
            return null;
        }
        // 基于当前游戏窗口的 BaseX/BaseY 加上偏移量，再应用比例。抗窗口拖拽！
        int absoluteX = (int) Math.round(tracker.getWindowBaseX() + transform.zeroOffsetX + logicalX * transform.scaleX);
        int absoluteY = (int) Math.round(tracker.getWindowBaseY() + transform.zeroOffsetY + logicalY * transform.scaleY);

        return new Point(absoluteX, absoluteY);
    }

    // ==========================================
    // 🧱 以下为你原有的像素比例换算逻辑，完全保留
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
}