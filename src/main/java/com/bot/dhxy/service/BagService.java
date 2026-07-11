package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.bag.ReturnItemCachePoint;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bag/item automation service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BagService {
    private static final String MAIN_BAG_ANCHOR_TEMPLATE = "images/template/bag/anchor_huanzhuang.png";
    private static final String MAIN_BAG_CUNKUAN_ANCHOR_TEMPLATE = "images/template/bag/anchor_cunkuan.png";
    private static final String[] MAIN_BAG_TAB_FALLBACK_TEMPLATES = {
            "images/template/bag/task_tab_fallback_a.png",
            "images/template/bag/task_tab_fallback_b.png"
    };
    private static final double MAIN_BAG_ANCHOR_MATCH_RATE = 0.8;
    private static final double MAIN_BAG_TAB_FALLBACK_MATCH_RATE = 0.8;
    private static final double BAG_ITEM_MATCH_RATE = 0.85;
    private static final double BAG_ITEM_MATCH_MIN_DISTANCE = 24.0;
    private static final int BAG_OPEN_WAIT_MS = 1200;
    private static final int BAG_LATE_RENDER_WAIT_MS = 700;
    private static final int BAG_TAB_CLICK_WAIT_MS = 500;
    private static final int MAIN_BAG_TASK_TAB_INDEX = 5;
    private static final int MAIN_BAG_MOUSE_SAFE_OFFSET_X = 60;
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int FIRST_SEARCHABLE_PAGE_INDEX = 0;
    private static final int LAST_SEARCHABLE_PAGE_INDEX = 4;

    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final Map<String, Integer> visiblePageCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> itemPageCache = new ConcurrentHashMap<>();
    private final Map<String, Point> lastMainBagAnchorCache = new ConcurrentHashMap<>();

    public enum ItemAction { SELECT, USE }

    /**
     * Summary for bounded item counting in an open bag.
     *
     * @param count number of distinct template hits found before the requested cap was reached.
     * @param firstPageIndex zero-based bag page of the first hit, or null when no item matched.
     */
    public record ItemCountResult(int count, Integer firstPageIndex) {
    }

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
            MAIN_BAG_ANCHOR_TEMPLATE, true,
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

    /**
     * Open the main bag once, run a caller-owned set of item operations, then close the bag once.
     *
     * @param source diagnostic source included in the input queue request name.
     * @param context optional stop token.
     * @param operation receives a session whose methods reuse the same confirmed bag geometry.
     * @return operation result, or null when the bag cannot be opened or the queued input section is
     *         interrupted before execution.
     */
    public <T> T withMainBagOpen(String source, TaskExecutionContext context, Function<MainBagSession, T> operation) {
        AtomicReference<T> result = new AtomicReference<>();
        String requestName = "bag:withMainBagOpen:" + (source == null || source.isBlank() ? "unknown" : source);
        boolean ok = inputSequences.submitExclusiveAndWait(requestName, () -> {
            result.set(withMainBagOpenExclusive(context, operation));
            return true;
        });
        if (!ok) {
            throwIfStopRequested(context);
            throwIfInterrupted("Bag batch operation input was interrupted");
            return null;
        }
        return result.get();
    }

    private Integer findItemPageIndexExclusive(BagLayout layout, String targetItemTemplate, TaskExecutionContext context) {
        throwIfStopRequested(context);
        log.info("[bag] start find item page: template={} autoManageUI={}", targetItemTemplate, layout.autoManageUI);
        Point baseAnchor = ensureBagOpened(layout, context);
        if (baseAnchor == null) {
            log.warn("[bag] open/confirm bag failed, abort find: template={}", targetItemTemplate);
            return null;
        }

        Integer foundIndex = null;
        try {
            int[] scanOrder = pageScanOrder(preferredStartPage(layout, targetItemTemplate), null);
            log.info("[bag] confirmed bag geometry anchor=({}, {}), scan tabs order={}",
                    baseAnchor.x, baseAnchor.y, displayPageOrder(scanOrder));
            for (int i : scanOrder) {
                throwIfStopRequested(context);
                Point found = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, i, context);
                if (found != null) {
                    foundIndex = i;
                    rememberItemPage(layout, targetItemTemplate, i);
                    log.info("[bag] found item: template={} page={} point=({}, {})",
                            targetItemTemplate, i + 1, found.x, found.y);
                    break;
                }
            }
        } finally {
            closeBagIfNeeded(layout, context);
        }

        if (foundIndex == null) {
            log.warn("[bag] item not found: template={}", targetItemTemplate);
        }
        return foundIndex;
    }

    private <T> T withMainBagOpenExclusive(TaskExecutionContext context, Function<MainBagSession, T> operation) {
        throwIfStopRequested(context);
        Point baseAnchor = ensureBagOpened(MAIN_BAG, context);
        if (baseAnchor == null) {
            log.warn("[bag] batch main bag operation aborted because the bag could not be opened");
            return null;
        }
        try {
            return operation.apply(new MainBagSession(baseAnchor, context));
        } finally {
            closeBagIfNeeded(MAIN_BAG, context);
        }
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

    /**
     * Locate a return item on the main-bag task page without clicking it.
     *
     * @param targetItemTemplate item template path relative to {@code images/template/}.
     * @param source diagnostic source describing why the prescan is running.
     * @param context optional task context for stop/pause checks.
     * @return cached screen-absolute item point when the item is found; null otherwise.
     */
    public ReturnItemCachePoint prescanMainBagTaskPageItem(String targetItemTemplate,
                                                           String source,
                                                           TaskExecutionContext context) {
        AtomicReference<ReturnItemCachePoint> result = new AtomicReference<>();
        boolean ok = inputSequences.submitExclusiveAndWait(
                "bag:prescanTaskPageItem:" + targetItemTemplate,
                () -> {
                    result.set(findMainBagTaskPageItemPointExclusive(targetItemTemplate, source, context));
                    return true;
                });
        if (!ok) {
            throwIfStopRequested(context);
            throwIfInterrupted("Bag task-page item prescan input was interrupted");
            return null;
        }
        return result.get();
    }

    /**
     * Locate a return item by scanning normal main-bag pages from the back without clicking it.
     *
     * @param targetItemTemplate item template path relative to {@code images/template/}.
     * @param maxBagIndex highest zero-based normal bag page to consider.
     * @param source diagnostic source describing why the prescan is running.
     * @param context optional task context for stop/pause checks.
     * @return cached screen-absolute item point when the item is found; null otherwise.
     */
    public ReturnItemCachePoint prescanMainBagItemFromBack(String targetItemTemplate,
                                                           int maxBagIndex,
                                                           String source,
                                                           TaskExecutionContext context) {
        AtomicReference<ReturnItemCachePoint> result = new AtomicReference<>();
        boolean ok = inputSequences.submitExclusiveAndWait(
                "bag:prescanItemFromBack:" + targetItemTemplate,
                () -> {
                    result.set(findMainBagItemFromBackPointExclusive(targetItemTemplate, maxBagIndex, source, context));
                    return true;
                });
        if (!ok) {
            throwIfStopRequested(context);
            throwIfInterrupted("Bag from-back item prescan input was interrupted");
            return null;
        }
        return result.get();
    }

    /**
     * Open the main bag and click a previously learned return-item point.
     *
     * @param cachedPoint screen-absolute point produced by one of the prescan methods.
     * @param source diagnostic source describing why the cached point is being consumed.
     * @param context optional task context for stop/pause checks.
     * @return true when the bag opened and the cached item click was submitted.
     */
    public boolean useCachedMainBagReturnItem(ReturnItemCachePoint cachedPoint,
                                              String source,
                                              TaskExecutionContext context) {
        if (cachedPoint == null) {
            return false;
        }
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        boolean ok = inputSequences.submitExclusiveAndWait(
                "bag:useCachedReturnItem:" + cachedPoint.getTemplatePath(),
                () -> {
                    result.set(useCachedMainBagReturnItemExclusive(cachedPoint, source, context));
                    return true;
                });
        if (!ok) {
            throwIfStopRequested(context);
            throwIfInterrupted("Cached return item input was interrupted");
            return false;
        }
        return result.get();
    }

    /**
     * Use a task item that is expected to live only on the main-bag task page.
     *
     * @param targetItemTemplate item template path relative to {@code images/template/}, such as
     *                           {@code bag/xiuluo_return_item.png}; must not be null/blank.
     * @param context optional task execution context used for stop checks.
     * @return true when the task page was opened, the template was found there, and the item click
     *         was submitted; false when the bag cannot be opened or the item is not on the task
     *         page. This method deliberately does not scan normal bag pages.
     */
    public boolean findAndUseMainBagTaskPageItem(String targetItemTemplate, TaskExecutionContext context) {
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        boolean ok = inputSequences.submitExclusiveAndWait(
                "bag:taskPageItemAction:USE:" + targetItemTemplate,
                () -> {
                    result.set(interactWithMainBagTaskPageItemExclusive(targetItemTemplate, ItemAction.USE, context));
                    return true;
                });
        if (!ok) {
            throwIfStopRequested(context);
            throwIfInterrupted("Bag task-page item action input was interrupted");
            return false;
        }
        return result.get();
    }

    public boolean isMainBagOpen(TaskExecutionContext context) {
        throwIfStopRequested(context);
        return getBaseAnchor(MAIN_BAG, context) != null;
    }

    private Point ensureBagOpened(BagLayout layout, TaskExecutionContext context) {
        throwIfStopRequested(context);
        if (!layout.autoManageUI) {
            log.info("[bag] layout does not need auto-open UI, use window base directly");
            return getBaseAnchor(layout, context);
        }

        BagOpenCheck check = checkBagOpened(layout, context, "initial");
        if (check.ready()) {
            Point p = check.anchor;
            log.info("[bag] main bag already open: geometryAnchor=({}, {}) source={}", p.x, p.y, check.visibleBy);
            rememberMainBagAnchor(layout, p, "initial");
            moveMouseAwayFromMainBagAnchor(layout, p, context, "initial");
            return p;
        }
        if (check.panelVisible) {
            log.warn("[bag] main bag UI is visible before Alt+E but primary anchor is still missing; skip Alt+E toggle: visibleBy={}",
                    check.visibleBy);
            return null;
        }

        log.info("[bag] main bag not open, press Alt+E");
        moveMouseAwayFromCachedMainBagAnchor(layout, context, "before-alt-e-first", true);
        if (!InputActionScope.checkpoint()) {
            return null;
        }
        inputProvider.pressAltE();
        TaskSleep.sleepOrStop(context, BAG_OPEN_WAIT_MS, "Bag operation wait was interrupted");
        if (!InputActionScope.checkpoint()) {
            return null;
        }

        check = checkBagOpened(layout, context, "after-alt-e-first");
        if (check.ready()) {
            Point p = check.anchor;
            log.info("[bag] found main bag geometry anchor after Alt+E: ({}, {}) source={}", p.x, p.y, check.visibleBy);
            rememberMainBagAnchor(layout, p, "after-alt-e-first");
            moveMouseAwayFromMainBagAnchor(layout, p, context, "after-alt-e-first");
            return p;
        }
        if (check.panelVisible) {
            log.warn("[bag] main bag panel is visible but primary anchor is still missing; skip second Alt+E to avoid closing it: visibleBy={}",
                    check.visibleBy);
            return null;
        }

        if (!check.panelVisible) {
            log.warn("[bag] Alt+E did not confirm anchor immediately, retry after short wait: template={}", layout.anchorTemplate);
            TaskSleep.sleepOrStop(context, BAG_LATE_RENDER_WAIT_MS, "Bag operation wait was interrupted");
            if (!InputActionScope.checkpoint()) {
                return null;
            }
            check = checkBagOpened(layout, context, "after-alt-e-late-render");
            if (check.ready()) {
                Point p = check.anchor;
                log.info("[bag] found main bag geometry anchor after late render wait: ({}, {}) source={}",
                        p.x, p.y, check.visibleBy);
                rememberMainBagAnchor(layout, p, "after-alt-e-late-render");
                moveMouseAwayFromMainBagAnchor(layout, p, context, "after-alt-e-late-render");
                return p;
            }
            if (check.panelVisible) {
                log.warn("[bag] main bag panel appeared after late render wait but primary anchor is still missing; skip second Alt+E: visibleBy={}",
                        check.visibleBy);
                return null;
            }
        }

        log.warn("[bag] Alt+E first attempt still has no bag UI indicators, sending one retry");
        moveMouseAwayFromCachedMainBagAnchor(layout, context, "before-alt-e-second", true);
        if (!InputActionScope.checkpoint()) {
            return null;
        }
        inputProvider.pressAltE();
        TaskSleep.sleepOrStop(context, BAG_OPEN_WAIT_MS, "Bag operation wait was interrupted");
        if (!InputActionScope.checkpoint()) {
            return null;
        }
        check = checkBagOpened(layout, context, "after-alt-e-second");
        if (check.ready()) {
            Point p = check.anchor;
            log.info("[bag] found main bag geometry anchor after Alt+E retry: ({}, {}) source={}",
                    p.x, p.y, check.visibleBy);
            rememberMainBagAnchor(layout, p, "after-alt-e-second");
            moveMouseAwayFromMainBagAnchor(layout, p, context, "after-alt-e-second");
            return p;
        }
        if (check.panelVisible) {
            log.warn("[bag] main bag panel is visible after Alt+E retry but primary anchor is still missing: visibleBy={}",
                    check.visibleBy);
        } else {
            log.warn("[bag] no main bag UI indicators after Alt+E retry: primaryAnchor={} fallbackTabs={} extraAnchor={}",
                    layout.anchorTemplate, String.join(",", MAIN_BAG_TAB_FALLBACK_TEMPLATES), MAIN_BAG_CUNKUAN_ANCHOR_TEMPLATE);
        }
        return null;
    }

    private void rememberMainBagAnchor(BagLayout layout, Point anchor, String stage) {
        if (layout != MAIN_BAG || anchor == null) {
            return;
        }
        lastMainBagAnchorCache.put(bagCacheKey(layout), new Point(anchor));
        log.debug("[bag] remember main bag anchor: stage={} anchor=({}, {})", stage, anchor.x, anchor.y);
    }

    private void moveMouseAwayFromCachedMainBagAnchor(BagLayout layout, TaskExecutionContext context, String stage, boolean forceMove) {
        if (layout != MAIN_BAG) {
            return;
        }
        Point cached = lastMainBagAnchorCache.get(bagCacheKey(layout));
        if (cached == null) {
            log.debug("[bag] no cached main bag anchor before open: stage={}", stage);
            return;
        }
        moveMouseAwayFromMainBagAnchor(layout, cached, context, stage, forceMove);
    }

    private void moveMouseAwayFromMainBagAnchor(BagLayout layout, Point anchor, TaskExecutionContext context, String stage) {
        moveMouseAwayFromMainBagAnchor(layout, anchor, context, stage, false);
    }

    private void moveMouseAwayFromMainBagAnchor(BagLayout layout, Point anchor, TaskExecutionContext context, String stage, boolean forceMove) {
        if (layout != MAIN_BAG || anchor == null) {
            return;
        }

        int safeMinX = anchor.x + MAIN_BAG_MOUSE_SAFE_OFFSET_X;
        Point mouse = currentLogicalMousePoint();
        if (!forceMove && mouse != null && mouse.x >= safeMinX) {
            log.info("[bag] mouse already right of main bag anchor: stage={} mouse=({}, {}) safeMinX={}",
                    stage, mouse.x, mouse.y, safeMinX);
            return;
        }

        int minX = Math.max(safeMinX, tracker.getWindowBaseX() + 80);
        int maxX = tracker.getWindowBaseX() + GAME_CLIENT_WIDTH - 80;
        int minY = tracker.getWindowBaseY() + 90;
        int maxY = tracker.getWindowBaseY() + GAME_CLIENT_HEIGHT - 90;
        if (minX > maxX) {
            minX = tracker.getWindowBaseX() + GAME_CLIENT_WIDTH - 160;
        }
        int targetX = randomBetween(minX, maxX);
        int targetY = randomBetween(minY, maxY);
        log.info("[bag] move mouse away from main bag anchor: stage={} forceMove={} anchor=({}, {}) mouse={} target=({}, {}) safeMinX={}",
                stage, forceMove, anchor.x, anchor.y, formatPoint(mouse), targetX, targetY, safeMinX);
        if (!InputActionScope.checkpoint()) {
            return;
        }
        inputProvider.moveMouse(targetX, targetY);
        TaskSleep.sleepOrStop(context, 120, "Bag mouse move wait was interrupted");
        InputActionScope.checkpoint();
    }

    private Point currentLogicalMousePoint() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            return null;
        }
        double scale = coordinateHelper.getScaleRatio();
        Point physical = pointerInfo.getLocation();
        return new Point((int) Math.round(physical.x / scale), (int) Math.round(physical.y / scale));
    }

    private int randomBetween(int minInclusive, int maxInclusive) {
        if (minInclusive >= maxInclusive) {
            return minInclusive;
        }
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private String formatPoint(Point point) {
        return point == null ? "unknown" : "(" + point.x + ", " + point.y + ")";
    }

    private void closeBagIfNeeded(BagLayout layout, TaskExecutionContext context) {
        if (layout.autoManageUI) {
            log.info("[bag] close main bag by Alt+E");
            if (!InputActionScope.checkpoint()) {
                return;
            }
            inputProvider.pressAltE();
            TaskSleep.sleepOrStop(context, 500, "Bag operation wait was interrupted");
            InputActionScope.checkpoint();
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
        Point baseAnchor = ensureBagOpened(layout, context);
        if (baseAnchor == null) {
            return false;
        }

        boolean success = false;
        try {
            if (isSearchablePage(knownBagIndex)) {
                throwIfStopRequested(context);
                log.info("[bag] scan known page first: page={}", knownBagIndex + 1);
                Point pt = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, knownBagIndex, context);
                if (pt != null) {
                    rememberItemPage(layout, targetItemTemplate, knownBagIndex);
                    success = executeSafeAction(pt, action, context);
                }
            }

            if (!success) {
                int[] scanOrder = pageScanOrder(preferredStartPage(layout, targetItemTemplate), knownBagIndex);
                log.info("[bag] scan remaining pages: template={} order={}", targetItemTemplate, displayPageOrder(scanOrder));
                for (int i : scanOrder) {
                    throwIfStopRequested(context);
                    Point pt = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, i, context);
                    if (pt != null) {
                        rememberItemPage(layout, targetItemTemplate, i);
                        success = executeSafeAction(pt, action, context);
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

    private boolean interactWithMainBagTaskPageItemExclusive(String targetItemTemplate,
                                                             ItemAction action,
                                                             TaskExecutionContext context) {
        throwIfStopRequested(context);
        log.info("[bag] start task-page item action: template={} action={}", targetItemTemplate, action);
        Point baseAnchor = ensureBagOpened(MAIN_BAG, context);
        if (baseAnchor == null) {
            return false;
        }

        boolean success = false;
        try {
            Point currentPageItem = searchItemInCurrentPageOnly(MAIN_BAG, baseAnchor, targetItemTemplate, context);
            if (currentPageItem != null) {
                success = executeSafeAction(currentPageItem, action, context);
            }
            if (!success) {
                Point pt = searchItemInTabOnly(MAIN_BAG, baseAnchor, targetItemTemplate,
                        MAIN_BAG_TASK_TAB_INDEX, context);
                if (pt != null) {
                    success = executeSafeAction(pt, action, context);
                }
            }
        } finally {
            closeBagIfNeeded(MAIN_BAG, context);
        }
        log.info("[bag] task-page item action finished: template={} action={} success={}",
                targetItemTemplate, action, success);
        return success;
    }

    private ReturnItemCachePoint findMainBagTaskPageItemPointExclusive(String targetItemTemplate,
                                                                       String source,
                                                                       TaskExecutionContext context) {
        throwIfStopRequested(context);
        log.info("[bag] start task-page item prescan: template={} source={}", targetItemTemplate, source);
        Point baseAnchor = ensureBagOpened(MAIN_BAG, context);
        if (baseAnchor == null) {
            return null;
        }

        try {
            Point currentPageItem = searchItemInCurrentPageOnly(MAIN_BAG, baseAnchor, targetItemTemplate, context);
            if (currentPageItem != null) {
                return toReturnItemCachePoint(targetItemTemplate, currentPageItem, source + ":current-page");
            }
            Point taskPageItem = searchItemInTabOnly(MAIN_BAG, baseAnchor, targetItemTemplate,
                    MAIN_BAG_TASK_TAB_INDEX, context);
            return toReturnItemCachePoint(targetItemTemplate, taskPageItem, source + ":task-page");
        } finally {
            closeBagIfNeeded(MAIN_BAG, context);
        }
    }

    private ReturnItemCachePoint findMainBagItemFromBackPointExclusive(String targetItemTemplate,
                                                                       int maxBagIndex,
                                                                       String source,
                                                                       TaskExecutionContext context) {
        throwIfStopRequested(context);
        int safeMaxBagIndex = Math.max(0, Math.min(maxBagIndex, 5));
        log.info("[bag] start from-back item prescan: template={} maxPage={} source={}",
                targetItemTemplate, safeMaxBagIndex + 1, source);
        Point baseAnchor = ensureBagOpened(MAIN_BAG, context);
        if (baseAnchor == null) {
            return null;
        }

        try {
            Point currentPageItem = searchItemInCurrentPageOnly(MAIN_BAG, baseAnchor, targetItemTemplate, context);
            if (currentPageItem != null) {
                return toReturnItemCachePoint(targetItemTemplate, currentPageItem, source + ":current-page");
            }
            for (int i = safeMaxBagIndex; i >= FIRST_SEARCHABLE_PAGE_INDEX; i--) {
                throwIfStopRequested(context);
                Point pt = searchItemInTabOnly(MAIN_BAG, baseAnchor, targetItemTemplate, i, context);
                if (pt != null) {
                    rememberItemPage(MAIN_BAG, targetItemTemplate, i);
                    return toReturnItemCachePoint(targetItemTemplate, pt, source + ":page-" + (i + 1));
                }
            }
            log.warn("[bag] from-back item prescan no match: template={} maxPage={} source={}",
                    targetItemTemplate, safeMaxBagIndex + 1, source);
            return null;
        } finally {
            closeBagIfNeeded(MAIN_BAG, context);
        }
    }

    private boolean useCachedMainBagReturnItemExclusive(ReturnItemCachePoint cachedPoint,
                                                        String source,
                                                        TaskExecutionContext context) {
        throwIfStopRequested(context);
        log.info("[bag] start cached return item use: template={} point=({}, {}) source={} learnedSource={}",
                cachedPoint.getTemplatePath(), cachedPoint.getClickX(), cachedPoint.getClickY(),
                source, cachedPoint.getSource());
        Point baseAnchor = ensureBagOpened(MAIN_BAG, context);
        if (baseAnchor == null) {
            return false;
        }

        try {
            return executeSafeAction(new Point(cachedPoint.getClickX(), cachedPoint.getClickY()), ItemAction.USE, context);
        } finally {
            closeBagIfNeeded(MAIN_BAG, context);
        }
    }

    private ReturnItemCachePoint toReturnItemCachePoint(String targetItemTemplate, Point point, String source) {
        if (point == null) {
            return null;
        }
        log.info("[bag] return item prescan matched: template={} point=({}, {}) source={}",
                targetItemTemplate, point.x, point.y, source);
        return ReturnItemCachePoint.builder()
                .templatePath(targetItemTemplate)
                .clickX(point.x)
                .clickY(point.y)
                .learnedAtMs(System.currentTimeMillis())
                .source(source)
                .build();
    }

    private Integer findItemPageIndexInOpenMainBag(Point baseAnchor, String targetItemTemplate,
                                                   TaskExecutionContext context) {
        Integer foundIndex = null;
        int[] scanOrder = pageScanOrder(preferredStartPage(MAIN_BAG, targetItemTemplate), null);
        log.info("[bag] scan open main bag pages: template={} order={}", targetItemTemplate, displayPageOrder(scanOrder));
        for (int i : scanOrder) {
            throwIfStopRequested(context);
            Point found = searchItemInTabOnly(MAIN_BAG, baseAnchor, targetItemTemplate, i, context);
            if (found != null) {
                foundIndex = i;
                rememberItemPage(MAIN_BAG, targetItemTemplate, i);
                log.info("[bag] found item in open main bag: template={} page={} point=({}, {})",
                        targetItemTemplate, i + 1, found.x, found.y);
                break;
            }
        }
        if (foundIndex == null) {
            log.warn("[bag] item not found in open main bag: template={}", targetItemTemplate);
        }
        return foundIndex;
    }

    private ItemCountResult countItemUpToInOpenMainBag(Point baseAnchor, String targetItemTemplate,
                                                       int requiredCount, TaskExecutionContext context) {
        int safeRequiredCount = Math.max(0, requiredCount);
        if (safeRequiredCount == 0) {
            return new ItemCountResult(0, null);
        }

        int count = 0;
        Integer firstPageIndex = null;
        int[] scanOrder = pageScanOrder(preferredStartPage(MAIN_BAG, targetItemTemplate), null);
        log.info("[bag] count open main bag item: template={} required={} order={}",
                targetItemTemplate, safeRequiredCount, displayPageOrder(scanOrder));
        for (int i : scanOrder) {
            throwIfStopRequested(context);
            List<Point> found = searchItemsInTabOnly(
                    MAIN_BAG, baseAnchor, targetItemTemplate, i, safeRequiredCount - count, context);
            if (!found.isEmpty()) {
                if (firstPageIndex == null) {
                    firstPageIndex = i;
                }
                rememberItemPage(MAIN_BAG, targetItemTemplate, i);
                count += found.size();
                log.info("[bag] count open main bag item page matched: template={} page={} pageCount={} total={}/{}",
                        targetItemTemplate, i + 1, found.size(), count, safeRequiredCount);
                if (count >= safeRequiredCount) {
                    break;
                }
            }
        }

        log.info("[bag] count open main bag item result: template={} count={} required={} firstPage={}",
                targetItemTemplate, count, safeRequiredCount,
                firstPageIndex == null ? "none" : firstPageIndex + 1);
        return new ItemCountResult(count, firstPageIndex);
    }

    private boolean interactWithItemInOpenMainBag(Point baseAnchor, String targetItemTemplate,
                                                  Integer knownBagIndex, ItemAction action,
                                                  TaskExecutionContext context) {
        boolean success = false;
        if (isSearchablePage(knownBagIndex)) {
            throwIfStopRequested(context);
            log.info("[bag] scan known page in open main bag first: page={} template={}",
                    knownBagIndex + 1, targetItemTemplate);
            Point pt = searchItemInTabOnly(MAIN_BAG, baseAnchor, targetItemTemplate, knownBagIndex, context);
            if (pt != null) {
                rememberItemPage(MAIN_BAG, targetItemTemplate, knownBagIndex);
                success = executeSafeAction(pt, action, context);
            }
        }

        if (!success) {
            int[] scanOrder = pageScanOrder(preferredStartPage(MAIN_BAG, targetItemTemplate), knownBagIndex);
            log.info("[bag] scan remaining open main bag pages: template={} order={}",
                    targetItemTemplate, displayPageOrder(scanOrder));
            for (int i : scanOrder) {
                throwIfStopRequested(context);
                Point pt = searchItemInTabOnly(MAIN_BAG, baseAnchor, targetItemTemplate, i, context);
                if (pt != null) {
                    rememberItemPage(MAIN_BAG, targetItemTemplate, i);
                    success = executeSafeAction(pt, action, context);
                    break;
                }
            }
        }
        log.info("[bag] open main bag item action finished: template={} action={} success={}",
                targetItemTemplate, action, success);
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
        Point baseAnchor = ensureBagOpened(layout, context);
        if (baseAnchor == null) {
            return false;
        }

        boolean success = false;
        try {
            /*
             * Return items often leave the bag on the task-item page after the first successful use.
             * Scan the currently visible grid once before switching tabs; if the item is already
             * visible this avoids an unnecessary task-tab/last-page click sequence.
             */
            Point currentPageItem = searchItemInCurrentPageOnly(layout, baseAnchor, targetItemTemplate, context);
            if (currentPageItem != null) {
                success = executeSafeAction(currentPageItem, action, context);
            }

            for (int i = safeMaxBagIndex; i >= 0; i--) {
                if (success) {
                    break;
                }
                throwIfStopRequested(context);
                Point pt = searchItemInTabOnly(layout, baseAnchor, targetItemTemplate, i, context);
                if (pt != null) {
                    rememberItemPage(layout, targetItemTemplate, i);
                    success = executeSafeAction(pt, action, context);
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
            if (!tracker.refreshWindowState()) {
                log.warn("[bag] no-anchor layout base refresh failed, abort item action to avoid stale base: layout={}",
                        layoutName(layout));
                return null;
            }
            Point base = new Point(tracker.getWindowBaseX(), tracker.getWindowBaseY());
            log.info("[bag] no-anchor layout uses refreshed window base: layout={} base=({}, {})",
                    layoutName(layout), base.x, base.y);
            return base;
        }
        Point anchor = coordinateHelper.findImageAbsoluteCoordinate(layout.anchorTemplate, 0.8);
        log.debug("[bag] anchor search result: template={} point={}", layout.anchorTemplate, anchor);
        return anchor;
    }

    private BagOpenCheck checkBagOpened(BagLayout layout, TaskExecutionContext context, String stage) {
        throwIfStopRequested(context);
        if (layout != MAIN_BAG) {
            return BagOpenCheck.ready(getBaseAnchor(layout, context), layout.anchorTemplate);
        }
        /*
         * Alt+E is a toggle. If any bag-panel indicator is visible, do not press Alt+E again
         * blindly; otherwise a missed primary anchor can turn into closing an already-open bag.
         */
        if (!tracker.updateGlobalVision()) {
            log.warn("[bag] open check capture failed: stage={}", stage);
            return BagOpenCheck.notVisible();
        }
        String screenPath = tracker.getLatestVisionPath();

        Point anchor = findTemplateInScreen(layout.anchorTemplate, screenPath, MAIN_BAG_ANCHOR_MATCH_RATE, context);
        if (anchor != null) {
            return BagOpenCheck.ready(anchor, layout.anchorTemplate);
        }

        Point tabPoint = findFirstTemplateInScreen(MAIN_BAG_TAB_FALLBACK_TEMPLATES, screenPath,
                MAIN_BAG_TAB_FALLBACK_MATCH_RATE, context);
        if (tabPoint != null) {
            log.info("[bag] main bag task-tab fallback matched: stage={} point=({}, {}), derive geometry without clicking task page",
                    stage, tabPoint.x, tabPoint.y);
            Point geometryAnchor = deriveAnchorFromTaskTab(layout, tabPoint);
            log.info("[bag] use task tab fallback as bag geometry anchor: stage={} tab=({}, {}) geometryAnchor=({}, {})",
                    stage, tabPoint.x, tabPoint.y, geometryAnchor.x, geometryAnchor.y);
            return BagOpenCheck.ready(geometryAnchor, "task-tab-fallback");
        }

        Point cunkuanAnchor = findTemplateInScreen(MAIN_BAG_CUNKUAN_ANCHOR_TEMPLATE, screenPath,
                MAIN_BAG_ANCHOR_MATCH_RATE, context);
        if (cunkuanAnchor != null) {
            log.info("[bag] main bag extra anchor matched: stage={} template={} point=({}, {})",
                    stage, MAIN_BAG_CUNKUAN_ANCHOR_TEMPLATE, cunkuanAnchor.x, cunkuanAnchor.y);
            return BagOpenCheck.visible(null, MAIN_BAG_CUNKUAN_ANCHOR_TEMPLATE);
        }

        return BagOpenCheck.notVisible();
    }

    private Point deriveAnchorFromTaskTab(BagLayout layout, Point taskTabPoint) {
        double scale = coordinateHelper.getScaleRatio();
        int anchorX = taskTabPoint.x - (int) Math.round(layout.tabOffsetX / scale);
        int anchorY = taskTabPoint.y - (int) Math.round((layout.tabOffsetY + MAIN_BAG_TASK_TAB_INDEX * layout.tabStepY) / scale);
        return new Point(anchorX, anchorY);
    }

    private Point findFirstTemplateInScreen(String[] templates, String screenPath, double matchRate, TaskExecutionContext context) {
        for (String template : templates) {
            throwIfStopRequested(context);
            Point point = findTemplateInScreen(template, screenPath, matchRate, context);
            if (point != null) {
                log.info("[bag] fallback template matched: template={} point=({}, {})",
                        template, point.x, point.y);
                return point;
            }
        }
        return null;
    }

    private Point findTemplateInScreen(String template, String screenPath, double matchRate, TaskExecutionContext context) {
        throwIfStopRequested(context);
        return coordinateHelper.findImageAbsoluteCoordinateByImagePath(template, screenPath, matchRate);
    }

    private Point searchItemInTabOnly(BagLayout layout, Point baseAnchor, String targetItemTemplate,
                                      int tabIndex, TaskExecutionContext context) {
        List<Point> found = searchItemsInTabOnly(layout, baseAnchor, targetItemTemplate, tabIndex, 1, context);
        return found.isEmpty() ? null : found.get(0);
    }

    private List<Point> searchItemsInTabOnly(BagLayout layout, Point baseAnchor, String targetItemTemplate,
                                             int tabIndex, int maxMatches, TaskExecutionContext context) {
        throwIfStopRequested(context);
        int safeMaxMatches = Math.max(1, maxMatches);
        log.info("[bag] switch and scan page {}: template={} maxMatches={}",
                tabIndex + 1, targetItemTemplate, safeMaxMatches);
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
            return List.of();
        }
        throwIfStopRequested(context);

        List<double[]> matches = ImageFinder.findAll(
                path, "images/template/" + targetItemTemplate, BAG_ITEM_MATCH_RATE, BAG_ITEM_MATCH_MIN_DISTANCE);
        throwIfStopRequested(context);
        if (matches.isEmpty()) {
            log.info("[bag] page {} no match: template={}", tabIndex + 1, targetItemTemplate);
            return List.of();
        }

        List<Point> found = new ArrayList<>();
        for (double[] match : matches) {
            if (match == null || match.length < 2) {
                continue;
            }
            Point point = new Point(
                    startX + (int) Math.round(match[0] / scale),
                    startY + (int) Math.round(match[1] / scale));
            found.add(point);
            if (found.size() >= safeMaxMatches) {
                break;
            }
        }
        if (found.isEmpty()) {
            log.info("[bag] page {} no valid match point after filtering: template={}", tabIndex + 1, targetItemTemplate);
            return List.of();
        }
        log.info("[bag] page {} matched: template={} count={} firstPoint=({}, {})",
                tabIndex + 1, targetItemTemplate, found.size(), found.get(0).x, found.get(0).y);
        return found;
    }

    private Point searchItemInCurrentPageOnly(BagLayout layout, Point baseAnchor, String targetItemTemplate,
                                              TaskExecutionContext context) {
        throwIfStopRequested(context);

        double scale = coordinateHelper.getScaleRatio();
        int startX = baseAnchor.x + (int) Math.round(layout.gridOffsetX / scale);
        int startY = baseAnchor.y + (int) Math.round(layout.gridOffsetY / scale);
        int endX = startX + (int) Math.round(layout.gridW / scale);
        int endY = startY + (int) Math.round(layout.gridH / scale);

        String path = windowScopedTempPath.resolve("bag_scan_current.png");
        log.info("[bag] capture current visible page first: path={} rect=({}, {})-({}, {}) template={}",
                path, startX, startY, endX, endY, targetItemTemplate);
        if (!tracker.captureToFile("bag-scan-current", path, startX, startY, endX, endY)) {
            log.warn("[bag] current visible page capture failed: template={}", targetItemTemplate);
            return null;
        }
        throwIfStopRequested(context);

        double[] res = ImageFinder.find(path, "images/template/" + targetItemTemplate, BAG_ITEM_MATCH_RATE);
        throwIfStopRequested(context);
        if (res == null || res.length < 2) {
            log.info("[bag] current visible page no match: template={}", targetItemTemplate);
            return null;
        }

        Point found = new Point(startX + (int) Math.round(res[0] / scale), startY + (int) Math.round(res[1] / scale));
        log.info("[bag] current visible page matched: template={} point=({}, {})",
                targetItemTemplate, found.x, found.y);
        return found;
    }

    private void switchBagTab(BagLayout layout, Point baseAnchor, int tabIndex, TaskExecutionContext context) {
        throwIfStopRequested(context);
        double scale = coordinateHelper.getScaleRatio();
        int tx = baseAnchor.x + (int) Math.round(layout.tabOffsetX / scale);
        int ty = baseAnchor.y + (int) Math.round((layout.tabOffsetY + tabIndex * layout.tabStepY) / scale);
        log.info("[bag] click page {} tab: ({}, {})", tabIndex + 1, tx, ty);
        if (!InputActionScope.checkpoint()) {
            return;
        }
        inputProvider.clickLeft(tx, ty, 100);
        rememberVisiblePage(layout, tabIndex);
        TaskSleep.sleepOrStop(context, 500, "Bag operation wait was interrupted");
        InputActionScope.checkpoint();
    }

    private int[] pageScanOrder(Integer preferredPageIndex, Integer skipPageIndex) {
        List<Integer> order = new ArrayList<>();
        if (isSearchablePage(preferredPageIndex) && !preferredPageIndex.equals(skipPageIndex)) {
            order.add(preferredPageIndex);
        }
        for (int i = FIRST_SEARCHABLE_PAGE_INDEX; i <= LAST_SEARCHABLE_PAGE_INDEX; i++) {
            if (Integer.valueOf(i).equals(skipPageIndex) || order.contains(i)) {
                continue;
            }
            order.add(i);
        }
        return order.stream().mapToInt(Integer::intValue).toArray();
    }

    private Integer preferredStartPage(BagLayout layout, String targetItemTemplate) {
        Integer visiblePage = visiblePageCache.get(bagCacheKey(layout));
        if (isSearchablePage(visiblePage)) {
            return visiblePage;
        }
        Integer itemPage = itemPageCache.get(itemCacheKey(layout, targetItemTemplate));
        return isSearchablePage(itemPage) ? itemPage : null;
    }

    private void rememberVisiblePage(BagLayout layout, int pageIndex) {
        if (isSearchablePage(pageIndex)) {
            visiblePageCache.put(bagCacheKey(layout), pageIndex);
        }
    }

    private void rememberItemPage(BagLayout layout, String targetItemTemplate, int pageIndex) {
        if (isSearchablePage(pageIndex)) {
            rememberVisiblePage(layout, pageIndex);
            itemPageCache.put(itemCacheKey(layout, targetItemTemplate), pageIndex);
        }
    }

    private boolean isSearchablePage(Integer pageIndex) {
        return pageIndex != null
                && pageIndex >= FIRST_SEARCHABLE_PAGE_INDEX
                && pageIndex <= LAST_SEARCHABLE_PAGE_INDEX;
    }

    private String itemCacheKey(BagLayout layout, String targetItemTemplate) {
        return bagCacheKey(layout) + "|item=" + targetItemTemplate;
    }

    private String bagCacheKey(BagLayout layout) {
        String windowId = windowTaskContextHolder.rawCurrent()
                .map(context -> context.getWindowId())
                .orElse("global");
        return windowId + "|" + layoutName(layout);
    }

    private String layoutName(BagLayout layout) {
        if (layout == MAIN_BAG) {
            return "MAIN_BAG";
        }
        if (layout == GIVE_BAG) {
            return "GIVE_BAG";
        }
        return String.valueOf(layout.anchorTemplate);
    }

    private String displayPageOrder(int[] order) {
        List<Integer> display = new ArrayList<>();
        for (int pageIndex : order) {
            display.add(pageIndex + 1);
        }
        return display.toString();
    }

    private boolean executeSafeAction(Point raw, ItemAction action, TaskExecutionContext context) {
        throwIfStopRequested(context);
        Point p = coordinateHelper.getRandomizedPoint(raw, 10, 10);
        log.info("[bag] execute item click: action={} raw=({}, {}) click=({}, {})",
                action, raw.x, raw.y, p.x, p.y);
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        if (action == ItemAction.USE) {
            inputProvider.clickRight(p.x, p.y, 100);
        } else {
            inputProvider.clickLeft(p.x, p.y, 100);
        }
        TaskSleep.sleepOrStop(context, 500, "Bag operation wait was interrupted");
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        return true;
    }

    private void throwIfStopRequested(TaskExecutionContext context) {
        TaskCheckpoint.throwIfStopRequested(context, "Bag operation was interrupted");
    }

    private void throwIfInterrupted(String message) {
        TaskCheckpoint.throwIfInterrupted(message);
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }

    private static final class BagOpenCheck {
        private final Point anchor;
        private final boolean panelVisible;
        private final String visibleBy;

        private BagOpenCheck(Point anchor, boolean panelVisible, String visibleBy) {
            this.anchor = anchor;
            this.panelVisible = panelVisible;
            this.visibleBy = visibleBy;
        }

        private static BagOpenCheck ready(Point anchor, String visibleBy) {
            return new BagOpenCheck(anchor, anchor != null, visibleBy);
        }

        private static BagOpenCheck visible(Point anchor, String visibleBy) {
            return new BagOpenCheck(anchor, true, visibleBy);
        }

        private static BagOpenCheck notVisible() {
            return new BagOpenCheck(null, false, "none");
        }

        private boolean ready() {
            return anchor != null;
        }
    }

    public final class MainBagSession {
        private final Point baseAnchor;
        private final TaskExecutionContext context;

        private MainBagSession(Point baseAnchor, TaskExecutionContext context) {
            this.baseAnchor = baseAnchor;
            this.context = context;
        }

        public Integer findItemPageIndex(String targetItemTemplate) {
            return findItemPageIndexInOpenMainBag(baseAnchor, targetItemTemplate, context);
        }

        public ItemCountResult countItemUpTo(String targetItemTemplate, int requiredCount) {
            return countItemUpToInOpenMainBag(baseAnchor, targetItemTemplate, requiredCount, context);
        }

        public boolean useItem(String targetItemTemplate, Integer knownBagIndex) {
            return interactWithItemInOpenMainBag(baseAnchor, targetItemTemplate, knownBagIndex, ItemAction.USE, context);
        }
    }
}
