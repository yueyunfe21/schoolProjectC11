package com.bot.dhxy.service;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;

/**
 * 🎒 万能包裹/物品栏自动化服务 (智能环境管理版)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BagService {

    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;

    public enum ItemAction { SELECT, USE }

    // ========================================================================
    // 📐 包裹图纸定义
    // ========================================================================
    public static class BagLayout {
        final String anchorTemplate;
        final boolean autoManageUI; // 🌟 核心开关：是否自动执行 Alt+E 的开启与关闭
        final int gridOffsetX, gridOffsetY, gridW, gridH;
        final int tabOffsetX, tabOffsetY, tabStepY;

        public BagLayout(String anchorTemplate, boolean autoManageUI, int gridOffsetX, int gridOffsetY, int gridW, int gridH, int tabOffsetX, int tabOffsetY, int tabStepY) {
            this.anchorTemplate = anchorTemplate;
            this.autoManageUI = autoManageUI;
            this.gridOffsetX = gridOffsetX; this.gridOffsetY = gridOffsetY;
            this.gridW = gridW; this.gridH = gridH;
            this.tabOffsetX = tabOffsetX; this.tabOffsetY = tabOffsetY; this.tabStepY = tabStepY;
        }
    }

    // 🌟 主背包：需要自动管理 UI (Alt+E)
    public static final BagLayout MAIN_BAG = new BagLayout(
            "images/template/anchor_huanzhuang.png", true,
            -299, 16, 312, 208, 29, 32, 35
    );

    // 🌟 给予包裹：不需要自动管理 (由 NPC 对话触发)
    public static final BagLayout GIVE_BAG = new BagLayout(
            null, false,
            359, 276, 308, 206, 681, 292, 35
    );

    // ========================================================================
    // 🚀 公共接口 (已注入环境管理)
    // ========================================================================

    public Integer findItemPageIndex(BagLayout layout, String targetItemTemplate) {
        // 1. 尝试开启环境
        if (!ensureBagOpened(layout)) return null;

        Integer foundIndex = null;
        try {
            log.info("🎒 [包裹引擎-侦察] 搜索物品: [{}]", targetItemTemplate);
            Point baseAnchor = getBaseAnchor(layout);
            if (baseAnchor != null) {
                for (int i = 0; i <= 4; i++) {
                    if (searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, i) != null) {
                        foundIndex = i;
                        break;
                    }
                }
            }
        } finally {
            // 2. 无论是否找到，只要是主包裹，操作完都要随手关门
            closeBagIfNeeded(layout);
        }
        return foundIndex;
    }

    public boolean findAndSelectItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex) {
        return interactWithItem(layout, targetItemTemplate, knownBagIndex, ItemAction.SELECT);
    }

    public boolean findAndUseItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex) {
        return interactWithItem(layout, targetItemTemplate, knownBagIndex, ItemAction.USE);
    }

    // ========================================================================
    // ⚙️ 智能环境管理引擎
    // ========================================================================

    /**
     * 🚪 确保包裹已打开
     * 只有 layout.autoManageUI 为 true 时，才会尝试 Alt+E
     */
    private boolean ensureBagOpened(BagLayout layout) {
        if (!layout.autoManageUI) return true; // 不需要自动管理的，默认视为已打开

        Point p = getBaseAnchor(layout);
        if (p == null) {
            log.info("🚪 [环境管理] 主包裹未开启，按下 Alt+E 唤起...");
            inputProvider.pressAltE();
            sleep(800); // 等待 UI 弹出
            p = getBaseAnchor(layout);
        }

        return p != null;
    }

    /**
     * 🚪 操作完成后的“随手关门”逻辑
     */
    private void closeBagIfNeeded(BagLayout layout) {
        if (layout.autoManageUI) {
            log.info("🚪 [环境管理] 操作结束，按下 Alt+E 关闭主包裹，清理战场。");
            inputProvider.pressAltE();
            sleep(500);
        }
    }

    private boolean interactWithItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex, ItemAction action) {
        if (!ensureBagOpened(layout)) return false;

        boolean success = false;
        try {
            Point baseAnchor = getBaseAnchor(layout);
            if (baseAnchor == null) return false;

            // 1. 尝试已知页码
            if (knownBagIndex != null && knownBagIndex >= 0 && knownBagIndex <= 4) {
                Point pt = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, knownBagIndex);
                if (pt != null) {
                    executeSafeAction(pt, action);
                    success = true;
                }
            }

            // 2. 兜底扫荡
            if (!success) {
                for (int i = 0; i <= 4; i++) {
                    if (knownBagIndex != null && i == knownBagIndex) continue;
                    Point pt = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, i);
                    if (pt != null) {
                        executeSafeAction(pt, action);
                        success = true;
                        break;
                    }
                }
            }
        } finally {
            closeBagIfNeeded(layout);
        }
        return success;
    }

    // ========================================================================
    // 👁️ 底层视觉引擎 (保持不变)
    // ========================================================================

    private Point getBaseAnchor(BagLayout layout) {
        if (layout.anchorTemplate == null) {
            return new Point(tracker.getWindowBaseX(), tracker.getWindowBaseY());
        }
        return coordinateHelper.findImageAbsoluteCoordinate(layout.anchorTemplate, 0.8);
    }

    private Point searchItemInTabOnly(BagLayout layout, Point baseAnchor, String targetItemTemplate, int tabIndex) {
        switchBagTab(layout, baseAnchor, tabIndex);
        double scale = coordinateHelper.getScaleRatio();
        int startX = baseAnchor.x + (int) Math.round(layout.gridOffsetX / scale);
        int startY = baseAnchor.y + (int) Math.round(layout.gridOffsetY / scale);
        int endX = startX + (int) Math.round(layout.gridW / scale);
        int endY = startY + (int) Math.round(layout.gridH / scale);

        String path = "images/temp/bag_scan.png";
        if (!tracker.captureToFile("局部扫描", path, startX, startY, endX, endY)) return null;

        double[] res = ImageFinder.find(path, "images/template/" + targetItemTemplate, 0.85);
        if (res == null || res.length < 2) return null;

        return new Point(startX + (int)Math.round(res[0]/scale), startY + (int)Math.round(res[1]/scale));
    }

    private void switchBagTab(BagLayout layout, Point baseAnchor, int tabIndex) {
        double scale = coordinateHelper.getScaleRatio();
        int tx = baseAnchor.x + (int) Math.round(layout.tabOffsetX / scale);
        int ty = baseAnchor.y + (int) Math.round((layout.tabOffsetY + tabIndex * layout.tabStepY) / scale);
        inputProvider.clickLeft(tx, ty, 100);
        sleep(500);
    }

    private void executeSafeAction(Point raw, ItemAction action) {
        Point p = coordinateHelper.getRandomizedPoint(raw, 10, 10);
        if (action == ItemAction.USE) inputProvider.clickRight(p.x, p.y, 100);
        else inputProvider.clickLeft(p.x, p.y, 100);
        sleep(500);
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}
