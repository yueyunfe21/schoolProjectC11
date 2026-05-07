package com.bot.dhxy.tools; // 保持您原文件的包名

import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import static java.lang.Thread.sleep;

/**
 * 🎒 背包/物品栏自动化服务
 * 核心战术：锚点定位 + 矩阵偏移计算 (动态适应窗口拖动) + 仿人手随机防封
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BagService {

    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;

    // ========================================================================
    // 📐 物理测绘数据
    // ========================================================================

    private static final String ANCHOR_TEMPLATE_PATH = "images/template/anchor_huanzhuang.png";

    // 🌟 1. 第一格相对偏移 (中心起点 -> 左上边缘)
    private static final int OFFSET_TO_SLOT_EDGE_X = -299;
    private static final int OFFSET_TO_SLOT_EDGE_Y = 16;

    // 🌟 2. 矩阵步长与尺寸
    private static final int SLOT_STEP_X = 52;
    private static final int SLOT_STEP_Y = 52;
    private static final int SLOT_SIZE = 52;

    // 🌟 3. 背包规格
    private static final int MAX_ROW = 4;
    private static final int MAX_COL = 6;

    // 🌟 4. 右侧页签测绘 (中心 -> 标签中心)
    private static final int OFFSET_TAB_1_X = 29;
    private static final int OFFSET_TAB_1_Y = 32;
    private static final int TAB_STEP_Y = 35;

    // ========================================================================
    // 🛠️ 核心基础动作
    // ========================================================================

    /**
     * 👁️ 寻找锚点 (转换为屏幕逻辑绝对坐标)
     */
    public Point findInventoryAnchor() {
        if (!tracker.bringWindowToFront()) {
            log.warn("❌ [背包雷达] 游戏窗口无法置顶！");
            return null;
        }

        tracker.updateGlobalVision();
        String screenPath = GameClientTracker.LATEST_VISION_PATH;
        double[] result = ImageFinder.find(screenPath, ANCHOR_TEMPLATE_PATH, 0.8);

        if (result != null && result.length >= 2) {
            double scale = coordinateHelper.getScaleRatio();
            int absoluteX = (int) Math.round(result[0] / scale) + tracker.getWindowBaseX();
            int absoluteY = (int) Math.round(result[1] / scale) + tracker.getWindowBaseY();

            log.info("✅ 锁定【换装】锚点！屏幕绝对坐标:({},{})", absoluteX, absoluteY);
            return new Point(absoluteX, absoluteY);
        }
        log.warn("❌ 未发现物品栏锚点！");
        return null;
    }

    /**
     * 🧠 计算格子坐标 (物理测量 + 自动中心对齐)
     */
    public Point calculateSlotCoordinate(Point anchor, int row, int col) {
        if (row < 0 || row >= MAX_ROW || col < 0 || col >= MAX_COL) return null;
        double scale = coordinateHelper.getScaleRatio();

        int halfSlot = (int) Math.round((SLOT_SIZE / 2.0) / scale);
        int scaledEdgeOffsetX = (int) Math.round(OFFSET_TO_SLOT_EDGE_X / scale);
        int scaledEdgeOffsetY = (int) Math.round(OFFSET_TO_SLOT_EDGE_Y / scale);
        int scaledStepX = (int) Math.round(SLOT_STEP_X / scale);
        int scaledStepY = (int) Math.round(SLOT_STEP_Y / scale);

        int targetX = anchor.x + scaledEdgeOffsetX + halfSlot + (col * scaledStepX);
        int targetY = anchor.y + scaledEdgeOffsetY + halfSlot + (row * scaledStepY);

        return new Point(targetX, targetY);
    }

    /**
     * 🎯 点击背包指定格子 (随机防封版)
     */
    public boolean clickBagSlot(int row, int col) {
        Point anchor = findInventoryAnchor();
        if (anchor == null) return false;

        Point targetPoint = calculateSlotCoordinate(anchor, row, col);
        if (targetPoint == null) return false;

        double scale = coordinateHelper.getScaleRatio();
        int randomRadius = (int) Math.round(12 / scale);
        int finalClickX = targetPoint.x + randomOffset(randomRadius);
        int finalClickY = targetPoint.y + randomOffset(randomRadius);

        log.info("🚀 点击背包格子 [{},{}] -> 落点:({}, {})", row, col, finalClickX, finalClickY);

        inputProvider.moveMouse(finalClickX, finalClickY);
        sleep(100);
        inputProvider.clickLeft(finalClickX, finalClickY, 100);
        return true;
    }

    // ========================================================================
    // 🔄 切页与巡回引擎
    // ========================================================================

    /**
     * ⚙️ [内部核心] 高速切换页签 (复用锚点)
     */
    private boolean switchBagTab(int tabIndex, Point anchor) {
        if (tabIndex < 0 || tabIndex > 5) return false;
        double scale = coordinateHelper.getScaleRatio();

        int targetX = anchor.x + (int) Math.round(OFFSET_TAB_1_X / scale);
        int targetY = anchor.y + (int) Math.round(OFFSET_TAB_1_Y / scale)
                + (int) Math.round((tabIndex * TAB_STEP_Y) / scale);

        int randomRadius = (int) Math.round(6 / scale);
        int finalClickX = targetX + randomOffset(randomRadius);
        int finalClickY = targetY + randomOffset(randomRadius);

        inputProvider.moveMouse(finalClickX, finalClickY);
        sleep(100 + ThreadLocalRandom.current().nextInt(50));
        inputProvider.clickLeft(finalClickX, finalClickY, 100 + ThreadLocalRandom.current().nextInt(30));

        sleep(400 + ThreadLocalRandom.current().nextInt(100)); // 等待 UI 刷新
        return true;
    }

    /**
     * ⚙️ [内部引擎] 遍历 0-4 号页并执行任务
     */
    private <T> T bagRotation(Point anchor, Function<Integer, T> pageTask) {
        for (int i = 0; i <= 4; i++) {
            if (!switchBagTab(i, anchor)) continue;
            T result = pageTask.apply(i);
            if (result != null) return result;
            sleep(150);
        }
        return null;
    }

    // ========================================================================
    // 🔍 物品查找雷达 (局部精准)
    // ========================================================================

    /**
     * 🔍 [公共接口] 指定页雷达：切换到第 index 个包裹并检查是否存在指定物品
     * @param bagIndex 第几个包裹 (0-5, 5通常是任务卷轴)
     * @param templateName 物品图片 (如 "shoe.png")
     */
    public Point checkItemInBag(int bagIndex, String templateName) {
        log.info("🎯 [定点搜查] 准备检查第 {} 个包裹中的物品: [{}]", bagIndex + 1, templateName);

        Point anchor = findInventoryAnchor();
        if (anchor == null) return null;

        // 1. 先切换到目标页
        if (!switchBagTab(bagIndex, anchor)) {
            log.warn("❌ [定点搜查] 切换到第 {} 页失败", bagIndex + 1);
            return null;
        }

        // 2. 执行局部极速扫描
        return checkItemInCurrentPageLocal(templateName, anchor);
    }

    /**
     * ⚙️ [内部核心] 极速局部扫描：只截取背包 24 格所在的矩形区域
     */
    private Point checkItemInCurrentPageLocal(String templateName, Point anchor) {
        String templatePath = "images/template/" + templateName;
        double scale = coordinateHelper.getScaleRatio();

        // 算背包内胆边框
        Point topLeft = calculateSlotCoordinate(anchor, 0, 0);
        Point bottomRight = calculateSlotCoordinate(anchor, MAX_ROW - 1, MAX_COL - 1);
        int half = (int) Math.round((SLOT_SIZE / 2.0) / scale);

        int startX = topLeft.x - half;
        int startY = topLeft.y - half;
        int endX = bottomRight.x + half;
        int endY = bottomRight.y + half;

        String localPath = "images/temp/bag_local.png";
        tracker.captureToFile("背包局部", localPath, startX, startY, endX, endY);

        double[] result = ImageFinder.find(localPath, templatePath, 0.85);

        if (result != null && result.length >= 2) {
            int absX = startX + (int) Math.round(result[0] / scale);
            int absY = startY + (int) Math.round(result[1] / scale);
            log.info("✅ 锁定物品 [{}] 坐标: ({},{})", templateName, absX, absY);
            return new Point(absX, absY);
        }
        return null;
    }

    /**
     * 🔍 [公共接口] 全包裹搜查
     */
    public Point searchItemInAllBags(String templateName) {
        Point anchor = findInventoryAnchor();
        if (anchor == null) return null;

        return bagRotation(anchor, pageIndex -> checkItemInCurrentPageLocal(templateName, anchor));
    }

    // ========================================================================
    // 🛠️ 辅助工具
    // ========================================================================

    private int randomOffset(int radius) {
        if (radius <= 0) return 0;
        return ThreadLocalRandom.current().nextInt(-radius, radius + 1);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}