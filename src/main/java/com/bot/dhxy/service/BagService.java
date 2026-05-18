package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.List;

/**
 * 万能包裹/物品栏自动化服务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BagService {

    private final InputSequences inputSequences;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;

    public enum ItemAction { SELECT, USE }

    public static class BagLayout {
        final String anchorTemplate;
        final boolean autoManageUI;
        final int gridOffsetX, gridOffsetY, gridW, gridH;
        final int tabOffsetX, tabOffsetY, tabStepY;

        public BagLayout(String anchorTemplate, boolean autoManageUI, int gridOffsetX, int gridOffsetY,
                         int gridW, int gridH, int tabOffsetX, int tabOffsetY, int tabStepY) {
            this.anchorTemplate = anchorTemplate;
            this.autoManageUI = autoManageUI;
            this.gridOffsetX = gridOffsetX;
            this.gridOffsetY = gridOffsetY;
            this.gridW = gridW;
            this.gridH = gridH;
            this.tabOffsetX = tabOffsetX;
            this.tabOffsetY = tabOffsetY;
            this.tabStepY = tabStepY;
        }
    }

    public static final BagLayout MAIN_BAG = new BagLayout(
            "images/template/anchor_huanzhuang.png", true,
            -299, 16, 312, 208, 29, 32, 35
    );

    public static final BagLayout GIVE_BAG = new BagLayout(
            null, false,
            359, 276, 308, 206, 681, 292, 35
    );

    public Integer findItemPageIndex(BagLayout layout, String targetItemTemplate) {
        return findItemPageIndex(layout, targetItemTemplate, null);
    }

    public Integer findItemPageIndex(BagLayout layout, String targetItemTemplate, TaskExecutionContext context) {
        throwIfStopRequested(context);
        if (!ensureBagOpened(layout, context)) return null;

        Integer foundIndex = null;
        try {
            Point baseAnchor = getBaseAnchor(layout, context);
            if (baseAnchor != null) {
                for (int i = 0; i <= 4; i++) {
                    throwIfStopRequested(context);
                    if (searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, i, context) != null) {
                        foundIndex = i;
                        break;
                    }
                }
            }
        } finally {
            closeBagIfNeeded(layout, context);
        }
        return foundIndex;
    }

    public boolean findAndSelectItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex) {
        return interactWithItem(layout, targetItemTemplate, knownBagIndex, ItemAction.SELECT, null);
    }

    public boolean findAndSelectItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex, TaskExecutionContext context) {
        return interactWithItem(layout, targetItemTemplate, knownBagIndex, ItemAction.SELECT, context);
    }

    public boolean findAndUseItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex) {
        return interactWithItem(layout, targetItemTemplate, knownBagIndex, ItemAction.USE, null);
    }

    public boolean findAndUseItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex, TaskExecutionContext context) {
        return interactWithItem(layout, targetItemTemplate, knownBagIndex, ItemAction.USE, context);
    }

    private boolean ensureBagOpened(BagLayout layout, TaskExecutionContext context) {
        throwIfStopRequested(context);
        if (!layout.autoManageUI) return true;

        Point p = getBaseAnchor(layout, context);
        if (p == null) {
            log.info("Main bag not open, pressing Alt+E");
            if (!inputSequences.submitAndWait("bag:openAltE", List.of(
                    InputAction.pressAltE(),
                    InputAction.sleep(800)
            ))) {
                return false;
            }
            p = getBaseAnchor(layout, context);
        }

        return p != null;
    }

    private void closeBagIfNeeded(BagLayout layout, TaskExecutionContext context) {
        if (layout.autoManageUI) {
            inputSequences.submitAndWait("bag:closeAltE", List.of(
                    InputAction.pressAltE(),
                    InputAction.sleep(500)
            ));
        }
    }

    private boolean interactWithItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex, ItemAction action, TaskExecutionContext context) {
        throwIfStopRequested(context);
        if (!ensureBagOpened(layout, context)) return false;

        boolean success = false;
        try {
            Point baseAnchor = getBaseAnchor(layout, context);
            if (baseAnchor == null) return false;

            if (knownBagIndex != null && knownBagIndex >= 0 && knownBagIndex <= 4) {
                throwIfStopRequested(context);
                Point pt = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, knownBagIndex, context);
                if (pt != null) {
                    executeSafeAction(pt, action, context);
                    success = true;
                }
            }

            if (!success) {
                for (int i = 0; i <= 4; i++) {
                    throwIfStopRequested(context);
                    if (knownBagIndex != null && i == knownBagIndex) continue;
                    Point pt = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, i, context);
                    if (pt != null) {
                        executeSafeAction(pt, action, context);
                        success = true;
                        break;
                    }
                }
            }
        } finally {
            closeBagIfNeeded(layout, context);
        }
        return success;
    }

    private Point getBaseAnchor(BagLayout layout, TaskExecutionContext context) {
        throwIfStopRequested(context);
        if (layout.anchorTemplate == null) {
            return new Point(tracker.getWindowBaseX(), tracker.getWindowBaseY());
        }
        return coordinateHelper.findImageAbsoluteCoordinate(layout.anchorTemplate, 0.8);
    }

    private Point searchItemInTabOnly(BagLayout layout, Point baseAnchor, String targetItemTemplate, int tabIndex, TaskExecutionContext context) {
        throwIfStopRequested(context);
        switchBagTab(layout, baseAnchor, tabIndex, context);
        throwIfStopRequested(context);

        double scale = coordinateHelper.getScaleRatio();
        int startX = baseAnchor.x + (int) Math.round(layout.gridOffsetX / scale);
        int startY = baseAnchor.y + (int) Math.round(layout.gridOffsetY / scale);
        int endX = startX + (int) Math.round(layout.gridW / scale);
        int endY = startY + (int) Math.round(layout.gridH / scale);

        String path = windowScopedTempPath.resolve("bag_scan.png");
        if (!tracker.captureToFile("局部扫描", path, startX, startY, endX, endY)) return null;
        throwIfStopRequested(context);

        double[] res = ImageFinder.find(path, "images/template/" + targetItemTemplate, 0.85);
        throwIfStopRequested(context);
        if (res == null || res.length < 2) return null;

        return new Point(startX + (int)Math.round(res[0]/scale), startY + (int)Math.round(res[1]/scale));
    }

    private void switchBagTab(BagLayout layout, Point baseAnchor, int tabIndex, TaskExecutionContext context) {
        throwIfStopRequested(context);
        double scale = coordinateHelper.getScaleRatio();
        int tx = baseAnchor.x + (int) Math.round(layout.tabOffsetX / scale);
        int ty = baseAnchor.y + (int) Math.round((layout.tabOffsetY + tabIndex * layout.tabStepY) / scale);
        inputSequences.submitAndWait("bag:switchTab", List.of(
                InputAction.clickLeft(tx, ty, 100),
                InputAction.sleep(500)
        ));
    }

    private void executeSafeAction(Point raw, ItemAction action, TaskExecutionContext context) {
        throwIfStopRequested(context);
        Point p = coordinateHelper.getRandomizedPoint(raw, 10, 10);
        InputAction clickAction = action == ItemAction.USE
                ? InputAction.clickRight(p.x, p.y, 100)
                : InputAction.clickLeft(p.x, p.y, 100);
        inputSequences.submitAndWait("bag:itemAction:" + action, List.of(
                clickAction,
                InputAction.sleep(500)
        ));
    }

    private void throwIfStopRequested(TaskExecutionContext context) {
        if (context != null) {
            context.throwIfStopRequested();
        }
    }

    private void sleep(TaskExecutionContext context, long ms) {
        if (ms <= 0) {
            return;
        }
        throwIfStopRequested(context);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskStopRequestedException("包裹操作等待被中断");
        }
        throwIfStopRequested(context);
    }
}
