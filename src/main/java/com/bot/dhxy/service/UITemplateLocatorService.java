package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.io.File;

/**
 * 大地图寻路雷达服务
 * 负责在当前游戏画面中，寻找特定图标（如“寻路”按钮）的绝对屏幕坐标
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UITemplateLocatorService {

    private static final double DEFAULT_THRESHOLD = 0.85; // 默认匹配相似度底线

    // 只需要注入追踪器，不需要注入 CoordinateHelper，因为在这里不需要算缩放比例
    private final GameClientTracker tracker;

    /**
     * 寻找模板，并返回可以直接让鼠标去点击的【屏幕逻辑绝对坐标】
     */
    public Point findTemplateCenter(String templatePath) {
        File template = new File(templatePath);
        if (!template.exists()) {
            log.warn("[导航] 模板不存在: {}", templatePath);
            return null;
        }

        // 🌟 直接呼叫追踪器：给我最新视野！
        // （未来如果你做了大循环，甚至可以把这行移到外层 Task 去，这里连截图都不用管了）
        if (!tracker.updateGlobalVision()) {
            log.warn("[导航] 视野刷新失败");
            return null;
        }

        // 直接读取 GameClientTracker 里的标准路径
        double[] result = ImageFinder.find(GameClientTracker.LATEST_VISION_PATH, templatePath, DEFAULT_THRESHOLD);
        if (result == null) return null;

        int relativeLogicalX = (int) Math.round(result[0]);
        int relativeLogicalY = (int) Math.round(result[1]);

        int absoluteLogicalX = relativeLogicalX + tracker.getWindowBaseX();
        int absoluteLogicalY = relativeLogicalY + tracker.getWindowBaseY();

        return new Point(absoluteLogicalX, absoluteLogicalY);
    }

}