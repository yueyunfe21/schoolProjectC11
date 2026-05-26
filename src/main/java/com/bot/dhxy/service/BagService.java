package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bag/item automation service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BagService {

    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
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
            "images/template/bag/anchor_huanzhuang.png", true,
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
        AtomicReference<Integer> result = new AtomicReference<>();
        boolean ok = inputSequences.submitExclusiveAndWait("bag:findItemPage:" + targetItemTemplate, () -> {
            result.set(findItemPageIndexExclusive(layout, targetItemTemplate, context));
            return true;
        });
        if (!ok) {
            throwIfStopRequested(context);
            throwIfInterrupted("Bag find page input was interrupted");
            return null;
        }
        return result.get();
    }

    private Integer findItemPageIndexExclusive(BagLayout layout, String targetItemTemplate, TaskExecutionContext context) {
        throwIfStopRequested(context);
        log.info("[bag] start find item page: template={} autoManageUI={}", targetItemTemplate, layout.autoManageUI);
        if (!ensureBagOpened(layout, context)) {
            log.warn("[bag] open/confirm bag failed, abort find: template={}", targetItemTemplate);
            return null;
        }

        Integer foundIndex = null;
        try {
            Point baseAnchor = getBaseAnchor(layout, context);
            if (baseAnchor != null) {
                log.info("[bag] confirmed anchor=({}, {}), scan tabs", baseAnchor.x, baseAnchor.y);
                for (int i = 0; i <= 4; i++) {
                    throwIfStopRequested(context);
                    Point found = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, i, context);
                    if (found != null) {
                        foundIndex = i;
                        log.info("[bag] found item: template={} page={} point=({}, {})",
                                targetItemTemplate, i + 1, found.x, found.y);
                        break;
                    }
                }
            } else {
                log.warn("[bag] bag anchor is null, cannot scan: template={}", targetItemTemplate);
            }
        } finally {
            closeBagIfNeeded(layout, context);
        }

        if (foundIndex == null) {
            log.warn("[bag] item not found: template={}", targetItemTemplate);
        }
        return foundIndex;
    }

    public boolean findAndSelectItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex) {
        return interactWithItem(layout, targetItemTemplate, knownBagIndex, ItemAction.SELECT, null);
    }

    public boolean findAndSelectItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex, TaskExecutionContext context) {
        return interactWithItem(layout, targetItemTemplate, knownBagIndex, ItemAction.SELECT, context);
    }

    public boolean findAndSelectItemDirectForExclusive(BagLayout layout, String targetItemTemplate, Integer knownBagIndex) {
        if (!isInputWorkerThread()) {
            return findAndSelectItem(layout, targetItemTemplate, knownBagIndex);
        }
        return interactWithItemExclusive(layout, targetItemTemplate, knownBagIndex, ItemAction.SELECT, null);
    }

    public boolean findAndUseItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex) {
        return interactWithItem(layout, targetItemTemplate, knownBagIndex, ItemAction.USE, null);
    }

    public boolean findAndUseItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex, TaskExecutionContext context) {
        return interactWithItem(layout, targetItemTemplate, knownBagIndex, ItemAction.USE, context);
    }

    public boolean findAndUseItemFromBack(BagLayout layout, String targetItemTemplate, int maxBagIndex,
                                          TaskExecutionContext context) {
        return interactWithItemFromBack(layout, targetItemTemplate, maxBagIndex, ItemAction.USE, context);
    }

    public boolean isMainBagOpen(TaskExecutionContext context) {
        throwIfStopRequested(context);
        return getBaseAnchor(MAIN_BAG, context) != null;
    }

    private boolean ensureBagOpened(BagLayout layout, TaskExecutionContext context) {
        throwIfStopRequested(context);
        if (!layout.autoManageUI) {
            log.info("[bag] layout does not need auto-open UI, use window base directly");
            return true;
        }

        Point p = getBaseAnchor(layout, context);
        if (p != null) {
            log.info("[bag] main bag already open: anchor=({}, {})", p.x, p.y);
            return true;
        }

        log.info("[bag] main bag not open, press Alt+E");
        inputProvider.pressAltE();
        sleep(context, 1200);

        p = getBaseAnchor(layout, context);
        if (p == null) {
            log.warn("[bag] Alt+E did not confirm anchor immediately, retry after short wait: template={}", layout.anchorTemplate);
            sleep(context, 700);
            p = getBaseAnchor(layout, context);
        }
        if (p == null) {
            log.warn("[bag] Alt+E first attempt still has no anchor, sending one retry");
            inputProvider.pressAltE();
            sleep(context, 1200);
            p = getBaseAnchor(layout, context);
        }
        if (p == null) {
            log.warn("[bag] no main bag anchor after Alt+E: {}", layout.anchorTemplate);
            return false;
        }

        log.info("[bag] found main bag anchor after Alt+E: ({}, {})", p.x, p.y);
        return true;
    }

    private void closeBagIfNeeded(BagLayout layout, TaskExecutionContext context) {
        if (layout.autoManageUI) {
            log.info("[bag] close main bag by Alt+E");
            inputProvider.pressAltE();
            sleep(context, 500);
        }
    }

    private boolean interactWithItem(BagLayout layout, String targetItemTemplate, Integer knownBagIndex,
                                     ItemAction action, TaskExecutionContext context) {
        long latencyStart = LatencyMetrics.start();
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        boolean ok = inputSequences.submitExclusiveAndWait("bag:itemAction:" + action + ":" + targetItemTemplate, () -> {
            result.set(interactWithItemExclusive(layout, targetItemTemplate, knownBagIndex, action, context));
            return true;
        });
        if (!ok) {
            throwIfStopRequested(context);
            throwIfInterrupted("Bag item action input was interrupted");
            LatencyMetrics.info(log, "bag.itemAction", latencyStart,
                    "result=false submitted=false action=" + action + " template=" + targetItemTemplate);
            return false;
        }
        boolean success = result.get();
        LatencyMetrics.info(log, "bag.itemAction", latencyStart,
                "result=" + success + " submitted=true action=" + action + " template=" + targetItemTemplate);
        return success;
    }

    private boolean interactWithItemExclusive(BagLayout layout, String targetItemTemplate, Integer knownBagIndex,
                                              ItemAction action, TaskExecutionContext context) {
        throwIfStopRequested(context);
        log.info("[bag] start item action: template={} knownPage={} action={}",
                targetItemTemplate, knownBagIndex == null ? "unknown" : knownBagIndex + 1, action);
        if (!ensureBagOpened(layout, context)) {
            return false;
        }

        boolean success = false;
        try {
            Point baseAnchor = getBaseAnchor(layout, context);
            if (baseAnchor == null) {
                log.warn("[bag] item action failed: anchor is null template={}", targetItemTemplate);
                return false;
            }

            if (knownBagIndex != null && knownBagIndex >= 0 && knownBagIndex <= 4) {
                throwIfStopRequested(context);
                log.info("[bag] scan known page first: page={}", knownBagIndex + 1);
                Point pt = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, knownBagIndex, context);
                if (pt != null) {
                    executeSafeAction(pt, action, context);
                    success = true;
                }
            }

            if (!success) {
                for (int i = 0; i <= 4; i++) {
                    throwIfStopRequested(context);
                    if (knownBagIndex != null && i == knownBagIndex) {
                        continue;
                    }
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
        log.info("[bag] item action finished: template={} action={} success={}", targetItemTemplate, action, success);
        return success;
    }

    private boolean interactWithItemFromBack(BagLayout layout, String targetItemTemplate, int maxBagIndex,
                                             ItemAction action, TaskExecutionContext context) {
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        boolean ok = inputSequences.submitExclusiveAndWait("bag:itemActionFromBack:" + action + ":" + targetItemTemplate, () -> {
            result.set(interactWithItemFromBackExclusive(layout, targetItemTemplate, maxBagIndex, action, context));
            return true;
        });
        if (!ok) {
            throwIfStopRequested(context);
            throwIfInterrupted("Bag reverse item action input was interrupted");
            return false;
        }
        return result.get();
    }

    private boolean interactWithItemFromBackExclusive(BagLayout layout, String targetItemTemplate, int maxBagIndex,
                                                      ItemAction action, TaskExecutionContext context) {
        throwIfStopRequested(context);
        int safeMaxBagIndex = Math.max(0, Math.min(maxBagIndex, 5));
        log.info("[bag] start reverse item action: template={} maxPage={} action={}",
                targetItemTemplate, safeMaxBagIndex + 1, action);
        if (!ensureBagOpened(layout, context)) {
            return false;
        }

        boolean success = false;
        try {
            Point baseAnchor = getBaseAnchor(layout, context);
            if (baseAnchor == null) {
                log.warn("[bag] reverse item action failed: anchor is null template={}", targetItemTemplate);
                return false;
            }

            for (int i = safeMaxBagIndex; i >= 0; i--) {
                throwIfStopRequested(context);
                Point pt = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, i, context);
                if (pt != null) {
                    executeSafeAction(pt, action, context);
                    success = true;
                    break;
                }
            }
        } finally {
            closeBagIfNeeded(layout, context);
        }
        log.info("[bag] reverse item action finished: template={} action={} success={}",
                targetItemTemplate, action, success);
        return success;
    }

    private Point getBaseAnchor(BagLayout layout, TaskExecutionContext context) {
        throwIfStopRequested(context);
        if (layout.anchorTemplate == null) {
            Point base = new Point(tracker.getWindowBaseX(), tracker.getWindowBaseY());
            log.debug("[bag] no anchor template, use window base: ({}, {})", base.x, base.y);
            return base;
        }
        Point anchor = coordinateHelper.findImageAbsoluteCoordinate(layout.anchorTemplate, 0.8);
        log.debug("[bag] anchor search result: template={} point={}", layout.anchorTemplate, anchor);
        return anchor;
    }

    private Point searchItemInTabOnly(BagLayout layout, Point baseAnchor, String targetItemTemplate,
                                      int tabIndex, TaskExecutionContext context) {
        throwIfStopRequested(context);
        log.info("[bag] switch and scan page {}: template={}", tabIndex + 1, targetItemTemplate);
        switchBagTab(layout, baseAnchor, tabIndex, context);
        throwIfStopRequested(context);

        double scale = coordinateHelper.getScaleRatio();
        int startX = baseAnchor.x + (int) Math.round(layout.gridOffsetX / scale);
        int startY = baseAnchor.y + (int) Math.round(layout.gridOffsetY / scale);
        int endX = startX + (int) Math.round(layout.gridW / scale);
        int endY = startY + (int) Math.round(layout.gridH / scale);

        String path = windowScopedTempPath.resolve("bag_scan.png");
        log.info("[bag] capture scan page {}: path={} rect=({}, {})-({}, {})",
                tabIndex + 1, path, startX, startY, endX, endY);
        if (!tracker.captureToFile("bag-scan", path, startX, startY, endX, endY)) {
            log.warn("[bag] page {} capture failed", tabIndex + 1);
            return null;
        }
        throwIfStopRequested(context);

        double[] res = ImageFinder.find(path, "images/template/" + targetItemTemplate, 0.85);
        throwIfStopRequested(context);
        if (res == null || res.length < 2) {
            log.info("[bag] page {} no match: template={}", tabIndex + 1, targetItemTemplate);
            return null;
        }

        Point found = new Point(startX + (int) Math.round(res[0] / scale), startY + (int) Math.round(res[1] / scale));
        log.info("[bag] page {} matched: template={} point=({}, {})",
                tabIndex + 1, targetItemTemplate, found.x, found.y);
        return found;
    }

    private void switchBagTab(BagLayout layout, Point baseAnchor, int tabIndex, TaskExecutionContext context) {
        throwIfStopRequested(context);
        double scale = coordinateHelper.getScaleRatio();
        int tx = baseAnchor.x + (int) Math.round(layout.tabOffsetX / scale);
        int ty = baseAnchor.y + (int) Math.round((layout.tabOffsetY + tabIndex * layout.tabStepY) / scale);
        log.info("[bag] click page {} tab: ({}, {})", tabIndex + 1, tx, ty);
        inputProvider.clickLeft(tx, ty, 100);
        sleep(context, 500);
    }

    private void executeSafeAction(Point raw, ItemAction action, TaskExecutionContext context) {
        throwIfStopRequested(context);
        Point p = coordinateHelper.getRandomizedPoint(raw, 10, 10);
        log.info("[bag] execute item click: action={} raw=({}, {}) click=({}, {})",
                action, raw.x, raw.y, p.x, p.y);
        if (action == ItemAction.USE) {
            inputProvider.clickRight(p.x, p.y, 100);
        } else {
            inputProvider.clickLeft(p.x, p.y, 100);
        }
        sleep(context, 500);
    }

    private void throwIfStopRequested(TaskExecutionContext context) {
        if (context != null) {
            context.throwIfStopRequested();
        }
    }

    private void throwIfInterrupted(String message) {
        if (Thread.currentThread().isInterrupted()) {
            throw new TaskStopRequestedException(message);
        }
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
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
            throw new TaskStopRequestedException("Bag operation wait was interrupted");
        }
        throwIfStopRequested(context);
    }
}
