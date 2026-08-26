package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.bag.ReturnItemCachePoint;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.model.bag.BagReturnItemMacroIntent;
import com.bot.dhxy.model.bag.BagReturnItemMacroResult;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
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
    private static final String MAIN_BAG_ANCHOR_TEMPLATE = "images/template/bag/anchor_cunkuan.png";
    private static final double MAIN_BAG_ANCHOR_MATCH_RATE = 0.8;
    private static final double BAG_ITEM_MATCH_RATE = 0.85;
    private static final double BAG_ITEM_MATCH_MIN_DISTANCE = 24.0;
    private static final String INCENSE_ITEM_TEMPLATE = "bag/sheyaoxiang_item.png";
    private static final int BAG_OPEN_WAIT_MS = 1200;
    private static final int BAG_LATE_RENDER_WAIT_MS = 700;
    private static final String TASK_TAB_FALLBACK_A_TEMPLATE = "images/template/bag/task_tab_fallback_a.png";
    private static final String TASK_TAB_FALLBACK_B_TEMPLATE = "images/template/bag/task_tab_fallback_b.png";
    private static final double TASK_TAB_MATCH_RATE = 0.80;
    /*
     * 锚点(存款按钮)位于包裹面板左半边:实测同窗页签在锚点右侧 152px,故 60px 的旧安全距离
     * 允许随机落点仍停在面板自己身上,悬停物品即弹说明框盖住锚点 -> 判定"包裹没开" -> 再按
     * 一次 Alt+E 把已开的包裹关掉(2026-08-25 18:59 hwnd-710DE6 实例:落点仅在锚点右 177px)。
     * 用户 2026-08-25 拍板改为 200px,确保随机落点越过整个面板宽度。
     */
    private static final int MAIN_BAG_MOUSE_SAFE_OFFSET_X = 200;
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int FIRST_SEARCHABLE_PAGE_INDEX = 0;
    private static final int LAST_SEARCHABLE_PAGE_INDEX = 4;
    private static final int MAIN_BAG_CACHED_ANCHOR_ROI_HALF_WIDTH = 45;
    private static final int MAIN_BAG_CACHED_ANCHOR_ROI_HALF_HEIGHT = 35;

    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final BoundWindowKeyboardService boundWindowKeyboardService;
    private final WindowFocusService windowFocusService;
    private final Map<String, Integer> visiblePageCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> itemPageCache = new ConcurrentHashMap<>();
    private final Map<String, Point> lastMainBagAnchorCache = new ConcurrentHashMap<>();
    /**
     * 主包裹任务页由队长在任务启动前视觉校准一次。页码是同一游戏程序的 UI 布局属性，
     * 不是某个 HWND 的屏幕坐标；各窗口仍各自用本窗口锚点换算最终点击点。
     */
    private final AtomicReference<Integer> calibratedMainBagTaskTabIndex = new AtomicReference<>();

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
            -176, 41, 312, 208, 152, 57, 35
    );

    public static final BagLayout GIVE_BAG = new BagLayout(
            null, false,
            359, 276, 308, 206, 681, 292, 35
    );

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
     * TURN-40B-C2 guarded variant of the queue-owning entry for turn
     * local-service bag operations.
     *
     * <p>The exclusive callback's FIRST action — before {@code ensureBagOpened} and before any
     * physical input — evaluates the live identity {@code admission} predicate, then the captured
     * live {@code stopToken}. A rejection only sets the callback-local flag and returns false: no
     * exception crosses the input-worker boundary and no Cloud type enters this service. After the
     * queue wait returns false, a flagged rejection (or a captured token that stopped during the
     * wait) converts to the existing {@link TaskStopRequestedException}; an ordinary queue failure
     * with an unstopped token keeps the existing generic null semantics. Bag-open failure inside
     * an admitted session also keeps its existing generic null result. The existing
     * {@code withMainBagOpenExclusive} path is byte-unchanged.</p>
     *
     * @param source diagnostic source included in the input queue request name.
     * @param admission live reference-identity predicate captured at action resolution.
     * @param stopToken captured live stop token of the action-owning task; nullable.
     * @param operation receives the main-bag session.
     * @return operation result, or null on generic queue/open failure.
     * @throws TaskStopRequestedException on identity replacement or a requested stop.
     */
    public <T> T withMainBagOpenGuarded(String source,
                                        BooleanSupplier admission,
                                        TaskStopToken stopToken,
                                        Function<MainBagSession, T> operation) {
        Objects.requireNonNull(admission, "admission");
        Objects.requireNonNull(operation, "operation");
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<GuardedRejection> rejection = new AtomicReference<>();
        String requestName = "bag:withMainBagOpenGuarded:"
                + (source == null || source.isBlank() ? "unknown" : source);
        boolean ok = inputSequences.submitExclusiveAndWait(requestName, () -> {
            if (!admission.getAsBoolean()) {
                rejection.set(GuardedRejection.IDENTITY_REPLACED);
                return false;
            }
            if (stopToken != null && stopToken.isStopRequested()) {
                rejection.set(GuardedRejection.STOP_REQUESTED);
                return false;
            }
            result.set(withMainBagOpenExclusive(null, operation));
            return true;
        });
        if (!ok) {
            GuardedRejection flagged = rejection.get();
            if (flagged != null) {
                throw new TaskStopRequestedException("guarded main-bag admission rejected: " + flagged);
            }
            if (stopToken != null && stopToken.isStopRequested()) {
                throw new TaskStopRequestedException("stop requested during guarded main-bag queue wait");
            }
            throwIfInterrupted("Bag guarded batch operation input was interrupted");
            return null;
        }
        return result.get();
    }

    /** Callback-owned guarded rejection kinds; stack-local per call, never a field or store. */
    private enum GuardedRejection {
        IDENTITY_REPLACED,
        STOP_REQUESTED
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

    public boolean findAndSelectItemDirectForExclusive(BagLayout layout, String targetItemTemplate, Integer knownBagIndex) {
        if (!isInputWorkerThread()) {
            return findAndSelectItem(layout, targetItemTemplate, knownBagIndex);
        }
        return interactWithItemExclusive(layout, targetItemTemplate, knownBagIndex, ItemAction.SELECT, null);
    }

    /**
     * W-BAG-MACRO-LOCAL-MECHANICS-IMP1: run exactly one committed Bag return-item flow as a single
     * local macro, already inside the input worker's exclusive section.
     *
     * <p>This entry must be invoked from within the serialized input worker (the remote handler's
     * {@code submitRemoteExclusiveAndWaitDetailed} boundary), so it never acquires the exclusive
     * queue again — reusing the three committed {@code ...Exclusive} cores directly avoids the
     * queue-in-queue deadlock. It changes no committed template path, page order, capture count,
     * coordinate, delay, fallback, or return value; it only projects the committed outcome onto the
     * closed {@link BagReturnItemMacroResult}. A null cached point preserves the committed
     * {@code false} (mapped to {@code NOT_USED}). Pause/stop is honored by the cores' existing
     * {@code throwIfStopRequested(context)} checkpoints.</p>
     *
     * @param intent closed selection of one of the three covered flows and its committed parameters
     * @param context current exact task-execution context carrying stop/pause and window binding
     * @return closed typed macro result (FOUND/NOT_FOUND for prescans, USED/NOT_USED for cached use)
     */
    public BagReturnItemMacroResult runReturnItemMacroDirectForExclusive(
            BagReturnItemMacroIntent intent, TaskExecutionContext context) {
        if (intent == null) {
            throw new IllegalArgumentException("intent must not be null");
        }
        if (!isInputWorkerThread()) {
            throw new IllegalStateException(
                    "bag return-item macro must run inside the exclusive input worker section");
        }
        return switch (intent.getKind()) {
            case PRESCAN_TASK_PAGE -> {
                ReturnItemCachePoint point = findMainBagTaskPageItemPointExclusive(
                        intent.getTargetItemTemplate(), intent.getSource(), context);
                yield point != null
                        ? BagReturnItemMacroResult.found(point)
                        : BagReturnItemMacroResult.notFound();
            }
            case PRESCAN_FROM_BACK -> {
                ReturnItemCachePoint point = findMainBagItemFromBackPointExclusive(
                        intent.getTargetItemTemplate(), intent.getMaxBagIndex(), intent.getSource(), context);
                yield point != null
                        ? BagReturnItemMacroResult.found(point)
                        : BagReturnItemMacroResult.notFound();
            }
            case USE_CACHED_RETURN_ITEM -> {
                ReturnItemCachePoint cachedPoint = intent.getCachedPoint();
                boolean used = cachedPoint != null
                        && useCachedMainBagReturnItemExclusive(cachedPoint, intent.getSource(), context);
                yield used ? BagReturnItemMacroResult.used() : BagReturnItemMacroResult.notUsed();
            }
            case FIND_AND_USE_TASK_PAGE -> {
                boolean used = interactWithMainBagTaskPageItemExclusive(
                        intent.getTargetItemTemplate(), ItemAction.USE, context);
                yield used ? BagReturnItemMacroResult.used() : BagReturnItemMacroResult.notUsed();
            }
        };
    }

    /**
     * Run the closed incense-use macro inside the input worker's current exclusive section.
     *
     * <p>The template is deliberately fixed locally and is not accepted from the remote request.
     * This method reuses the existing main-bag scan/use core directly so it does not acquire the
     * input queue a second time.</p>
     *
     * @param context optional task context for the existing stop checkpoints; may be null when the
     *                remote input queue supplies the stop/pause fences
     * @return true when the fixed incense item was found and its use click completed; false when it
     *         was not found or the bag could not be opened
     */
    public boolean runUseIncenseMacroDirectForExclusive(TaskExecutionContext context) {
        if (!isInputWorkerThread()) {
            throw new IllegalStateException(
                    "bag use-incense macro must run inside the exclusive input worker section");
        }
        return interactWithItemExclusive(
                MAIN_BAG, INCENSE_ITEM_TEMPLATE, null, ItemAction.USE, context);
    }

    public boolean findAndUseItemFromBack(BagLayout layout, String targetItemTemplate, int maxBagIndex,
                                          TaskExecutionContext context) {
        return interactWithItemFromBack(layout, targetItemTemplate, maxBagIndex, ItemAction.USE, context);
    }

    protected Point ensureBagOpened(BagLayout layout, TaskExecutionContext context) {
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
        if (!pressBackgroundAltE("open-main-bag-first")) {
            return null;
        }
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
            Point cleared = recheckAnchorAfterHoverClear(layout, context, "after-alt-e-first");
            if (cleared != null) {
                moveMouseAwayFromMainBagAnchor(layout, cleared, context, "after-alt-e-first-hover-cleared");
                return cleared;
            }
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
                Point cleared = recheckAnchorAfterHoverClear(layout, context, "after-alt-e-late-render");
                if (cleared != null) {
                    moveMouseAwayFromMainBagAnchor(layout, cleared, context, "after-alt-e-late-render-hover-cleared");
                    return cleared;
                }
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
        if (!pressBackgroundAltE("open-main-bag-retry")) {
            return null;
        }
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
            Point cleared = recheckAnchorAfterHoverClear(layout, context, "after-alt-e-second");
            if (cleared != null) {
                moveMouseAwayFromMainBagAnchor(layout, cleared, context, "after-alt-e-second-hover-cleared");
                return cleared;
            }
            log.warn("[bag] main bag panel is visible after Alt+E retry but primary anchor is still missing: visibleBy={}",
                    check.visibleBy);
        } else {
            log.warn("[bag] no main bag UI indicators after Alt+E retry: primaryAnchor={}",
                    layout.anchorTemplate);
        }
        return null;
    }

    /**
     * 2026-08-23 用户契约（停止=彻底清空）：清该窗口的包裹缓存（锚点绝对坐标/可见页/物品页）。
     * 3519 案根因即"18:23 学的绝对锚点 + 窗口挪动 + 缓存永生"；新一轮非续跑启动前必清。
     */
    public void forgetWindowRealityMemory(String windowId) {
        if (windowId == null || windowId.isBlank()) {
            return;
        }
        // 审查修正：无上下文线程写入的 "global|" 键对所有窗口都是潜在毒缓存，任一窗口
        // fresh-start 时一并清除（3519 同族风险）。
        String prefix = windowId + "|";
        java.util.function.Predicate<String> stale = key ->
                key.startsWith(prefix) || key.startsWith("global|");
        lastMainBagAnchorCache.keySet().removeIf(stale);
        visiblePageCache.keySet().removeIf(stale);
        itemPageCache.keySet().removeIf(stale);
        log.info("[bag] window reality caches cleared: windowId={}", windowId);
    }

    /** 全进程只校准一次的主包任务页签行号：fresh-start 时清掉，下次用到重新校准。 */
    public void forgetMainBagTaskTabCalibration() {
        calibratedMainBagTaskTabIndex.set(null);
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
            // 首次开包无缓存锚点:仍要把鼠标挪开——悬停提示可能正挡着锚点将要出现的位置
            // (2026-08-08 实测:新进程首开必失败,重试鼠标不动则永远失败)。
            log.debug("[bag] no cached main bag anchor before open; move mouse to opposite corner instead: stage={}", stage);
            moveMouseToSafeParkPoint(context, stage + "-no-cache");
            return;
        }
        moveMouseAwayFromMainBagAnchor(layout, cached, context, stage, forceMove);
    }

    /**
     * 不依赖任何缓存的兜底挪鼠标(用户 2026-08-08 拍板选 A):复用既有 pointer-clear 首选停靠点
     * ——窗口左下贴边 (left+1, bottom-2),与 CloudPlayerStateIncenseStatusPort 的清指针候选一致,
     * 贴边像素悬停不到任何 UI。用于首次开包(无缓存锚点)与"面板可见但锚点缺失"的悬停清除复查。
     */
    private void moveMouseToSafeParkPoint(TaskExecutionContext context, String stage) {
        // (left+1, bottom-2) 实测会落到窗口客户区之外(差一点点),moveMouse 被丢弃、悬停提示
        // 仍在;各内缩 10px 保证落点在窗口内(2026-08-08 用户实测拍板)。
        int targetX = tracker.getWindowBaseX() + 11;
        int targetY = tracker.getWindowBaseY() + GAME_CLIENT_HEIGHT - 12;
        log.info("[bag] move mouse to safe park point to clear hover tooltip: stage={} target=({}, {})",
                stage, targetX, targetY);
        if (!InputActionScope.checkpoint()) {
            return;
        }
        inputProvider.moveMouse(targetX, targetY);
        TaskSleep.sleepOrStop(context, 150, "Bag mouse move wait was interrupted");
        InputActionScope.checkpoint();
    }

    /**
     * "面板可见但主锚点缺失"的悬停清除复查:头号成因是鼠标悬停提示正盖着锚点。
     * 挪开鼠标后复查一次;仍缺失才交还调用方按原语义失败。
     */
    private Point recheckAnchorAfterHoverClear(BagLayout layout, TaskExecutionContext context, String stage) {
        log.warn("[bag] panel visible but primary anchor missing; clear possible hover tooltip and recheck: stage={}", stage);
        moveMouseToSafeParkPoint(context, stage);
        BagOpenCheck check = checkBagOpened(layout, context, stage + "-hover-cleared");
        if (check.ready()) {
            Point p = check.anchor;
            log.info("[bag] anchor appeared after hover clear: ({}, {}) stage={}", p.x, p.y, stage);
            rememberMainBagAnchor(layout, p, stage + "-hover-cleared");
            return p;
        }
        return null;
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

    protected void closeBagIfNeeded(BagLayout layout, TaskExecutionContext context) {
        if (layout.autoManageUI) {
            log.info("[bag] close main bag by Alt+E");
            if (!InputActionScope.checkpoint()) {
                return;
            }
            if (!pressBackgroundAltE("close-main-bag")) {
                return;
            }
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
                Integer taskTabIndex = resolveMainBagTaskTabIndex(baseAnchor, context);
                Point pt = taskTabIndex == null ? null : searchItemInTabOnly(
                        MAIN_BAG, baseAnchor, targetItemTemplate, taskTabIndex, context);
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
            Integer taskTabIndex = resolveMainBagTaskTabIndex(baseAnchor, context);
            Point taskPageItem = taskTabIndex == null ? null : searchItemInTabOnly(
                    MAIN_BAG, baseAnchor, targetItemTemplate, taskTabIndex, context);
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

    protected ItemCountResult countItemUpToInOpenMainBag(Point baseAnchor, String targetItemTemplate,
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
        Point anchor = coordinateHelper.findImageAbsoluteCoordinate(layout.anchorTemplate, MAIN_BAG_ANCHOR_MATCH_RATE);
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
        /*
         * The bag is movable. Only a real anchor learned for this exact window may define a fast ROI;
         * the window origin is never a bag-position substitute. On the first use, or whenever the
         * cached ROI misses, restore the validated full-window search and refresh the cache through
         * the existing caller path before any second Alt+E toggle is considered.
         */
        Point cachedAnchor = lastMainBagAnchorCache.get(bagCacheKey(layout));
        if (cachedAnchor != null) {
            int left = cachedAnchor.x - MAIN_BAG_CACHED_ANCHOR_ROI_HALF_WIDTH;
            int top = cachedAnchor.y - MAIN_BAG_CACHED_ANCHOR_ROI_HALF_HEIGHT;
            int right = cachedAnchor.x + MAIN_BAG_CACHED_ANCHOR_ROI_HALF_WIDTH;
            int bottom = cachedAnchor.y + MAIN_BAG_CACHED_ANCHOR_ROI_HALF_HEIGHT;
            long captureStartedAt = LatencyMetrics.start();
            BufferedImage roi = tracker.captureExactWindowRegionFastToMemory(
                    "bag-open-cached-anchor:" + stage, left, top, right, bottom);
            if (roi != null) {
                try {
                    BufferedImage template = ImageIO.read(Path.of(layout.anchorTemplate).toFile());
                    /*
                     * 2026-08-23 用户批准（3519 案）：请求 90x70 的检查框若被窗口边缘削小到
                     * 比模板还小（当晚实截 17x70 < 模板 25x12），说明缓存锚点已随窗口位置失效。
                     * 这种"假成功"截图必须按截图失败处理，走下面既有的全窗搜索兜底，
                     * 严禁喂给 matchTemplate（会抛 cv 异常炸掉整个输入事务）。
                     */
                    boolean roiClipped = template != null
                            && (roi.getWidth() < template.getWidth()
                            || roi.getHeight() < template.getHeight());
                    if (roiClipped) {
                        log.warn("[bag] cached open-check capture clipped smaller than template; treat as "
                                        + "capture failure and fall back to full window: stage={} roi={}x{} "
                                        + "template={}x{} cachedAnchor=({}, {})",
                                stage, roi.getWidth(), roi.getHeight(),
                                template.getWidth(), template.getHeight(),
                                cachedAnchor.x, cachedAnchor.y);
                    }
                    double[] match = template == null || roiClipped
                            ? null
                            : ImageFinder.find(roi, template, MAIN_BAG_ANCHOR_MATCH_RATE);
                    // 用户铁律（2026-08-18 全量清扫）：模板匹配点落盘判定原图。
                    if (template != null) {
                        MatchEvidenceStore.save("bag-open-anchor", null, roi, template, match);
                    }
                    LatencyMetrics.info(log, "bag.openCheck", captureStartedAt,
                            "stage=" + stage + " provider=HWND_BITBLT_REGION source=cached-anchor matched="
                                    + (match != null));
                    if (match != null && match.length >= 2) {
                        Point anchor = new Point(
                                left + (int) Math.round(match[0]),
                                top + (int) Math.round(match[1]));
                        return BagOpenCheck.ready(anchor, layout.anchorTemplate + ":cached-roi");
                    }
                } catch (IOException e) {
                    log.warn("[bag] failed to load cached main bag anchor template: stage={} template={} reason={}",
                            stage, layout.anchorTemplate, e.getMessage());
                } finally {
                    roi.flush();
                }
            } else {
                log.warn("[bag] cached open-check capture failed, fall back to full window: "
                                + "stage={} cachedAnchor=({}, {}) rect=({}, {})-({}, {})",
                        stage, cachedAnchor.x, cachedAnchor.y, left, top, right, bottom);
            }
            log.info("[bag] cached main bag anchor missed, search full window: stage={} cachedAnchor=({}, {})",
                    stage, cachedAnchor.x, cachedAnchor.y);
        }

        if (!tracker.updateGlobalVision()) {
            log.warn("[bag] full-window open check capture failed: stage={} cachedAnchor={}",
                    stage, formatPoint(cachedAnchor));
            return BagOpenCheck.notVisible();
        }
        String screenPath = tracker.getLatestVisionPath();
        Point anchor = findTemplateInScreen(layout.anchorTemplate, screenPath, MAIN_BAG_ANCHOR_MATCH_RATE, context);
        if (anchor != null) {
            log.info("[bag] full-window main bag anchor matched: stage={} point=({}, {}) cachedBefore={}",
                    stage, anchor.x, anchor.y, formatPoint(cachedAnchor));
            return BagOpenCheck.ready(anchor, layout.anchorTemplate + ":full-window");
        }
        return BagOpenCheck.notVisible();
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
        if (!tracker.captureExactWindowRegionFastToFile("bag-scan", path, startX, startY, endX, endY)) {
            log.warn("[bag] page {} capture failed", tabIndex + 1);
            return List.of();
        }
        throwIfStopRequested(context);

        List<double[]> matches = ImageFinder.findAll(
                path, "images/template/" + targetItemTemplate, BAG_ITEM_MATCH_RATE, BAG_ITEM_MATCH_MIN_DISTANCE);
        throwIfStopRequested(context);
        if (matches.isEmpty()) {
            log.info("[bag] page {} no match: template={}", tabIndex + 1, targetItemTemplate);
            // bag_scan.png is overwritten by the very next scan, so the frame that missed is gone by the
            // time anyone looks. Keep a copy of that one.
            BagScanMissDump.copy(path, targetItemTemplate, tabIndex + 1);
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
        if (!tracker.captureExactWindowRegionFastToFile(
                "bag-scan-current", path, startX, startY, endX, endY)) {
            log.warn("[bag] current visible page capture failed: template={}", targetItemTemplate);
            return null;
        }
        throwIfStopRequested(context);

        double[] res = ImageFinder.find(path, "images/template/" + targetItemTemplate, BAG_ITEM_MATCH_RATE);
        BufferedImage evidenceFrame = ImagePreprocessor.pathToBufferedImage(path);
        BufferedImage evidenceTemplate =
                ImagePreprocessor.pathToBufferedImage("images/template/" + targetItemTemplate);
        MatchEvidenceStore.save("bag-item-current-page", null, evidenceFrame, evidenceTemplate, res);
        if (evidenceFrame != null) {
            evidenceFrame.flush();
        }
        if (evidenceTemplate != null) {
            evidenceTemplate.flush();
        }
        throwIfStopRequested(context);
        if (res == null || res.length < 2) {
            log.info("[bag] current visible page no match: template={}", targetItemTemplate);
            // Same reason as the paged scan: bag_scan_current.png is gone on the next call.
            BagScanMissDump.copy(path, targetItemTemplate, 0);
            return null;
        }

        Point found = new Point(startX + (int) Math.round(res[0] / scale), startY + (int) Math.round(res[1] / scale));
        log.info("[bag] current visible page matched: template={} point=({}, {})",
                targetItemTemplate, found.x, found.y);
        return found;
    }

    /**
     * Calibrate the process-wide main-bag task tab before the leader task loop starts.
     *
     * <p>The supplied binding is frozen by the input queue, so opening the bag, taking the visual sample,
     * and closing it stay scoped to the exact leader HWND. The result is a tab index only; each later
     * window still derives its own physical click coordinate from its own bag anchor.</p>
     *
     * @param context leader runtime context that owns the one-time calibration.
     * @param binding current exact native binding for {@code context}.
     * @return true when an existing or newly matched task-tab index is available.
     */
    public boolean calibrateMainBagTaskTabAtStartup(WindowRuntimeContext context, WindowNativeBinding binding) {
        if (calibratedMainBagTaskTabIndex.get() != null) {
            return true;
        }
        if (context == null || !context.isLeader() || binding == null
                || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            log.info("[bag] skip startup task-tab calibration: windowId={} role={} bindingReady={}",
                    context == null ? "unknown" : context.getWindowId(),
                    context == null ? "unknown" : context.getRole(),
                    binding != null && binding.hasNativeHandle() && binding.hasGeometry());
            return false;
        }
        AtomicReference<Boolean> calibrated = new AtomicReference<>(false);
        InputActionExecutionResult result = inputSequences.submitFrozenExactWindowExclusiveAndWait(
                "bag:startup-task-tab-calibration:" + context.getWindowId(), context, binding,
                () -> windowTaskContextHolder.callWith(context, () -> {
                    Point baseAnchor = ensureBagOpened(MAIN_BAG, null);
                    if (baseAnchor == null) {
                        return false;
                    }
                    try {
                        calibrated.set(calibrateMainBagTaskTab(baseAnchor, context.getWindowId()));
                        return calibrated.get();
                    } finally {
                        closeBagIfNeeded(MAIN_BAG, null);
                    }
                }));
        boolean available = result.isCompleted() && Boolean.TRUE.equals(calibrated.get());
        log.info("[bag] startup task-tab calibration result: windowId={} status={} available={} pageIndex={}",
                context.getWindowId(), result.getStatus(), available, calibratedMainBagTaskTabIndex.get());
        return available;
    }

    /**
     * Returns the cached task tab, calibrating it from the already-open exact window when startup could not yet
     * prove a local leader. This remains fail-closed: no tab is clicked until a template establishes the page.
     */
    private Integer resolveMainBagTaskTabIndex(Point baseAnchor, TaskExecutionContext context) {
        Integer cached = calibratedMainBagTaskTabIndex.get();
        if (cached == null && baseAnchor != null) {
            String windowId = context == null ? "unknown" : context.getWindowId();
            log.info("[bag] task-page tab missing at action time; calibrate from the already-open task window: windowId={}",
                    windowId);
            calibrateMainBagTaskTab(baseAnchor, windowId);
            cached = calibratedMainBagTaskTabIndex.get();
        }
        if (cached == null) {
            log.warn("[bag] main-bag task tab has not been calibrated; skip task-page action: windowId={} role={}",
                    context == null ? "unknown" : context.getWindowId(),
                    context == null ? "unknown" : context.getWindowRole());
        }
        return cached;
    }

    private boolean calibrateMainBagTaskTab(Point baseAnchor, String windowId) {
        if (!tracker.updateGlobalVision()) {
            log.warn("[bag] startup task-tab calibration capture failed: windowId={}", windowId);
            return false;
        }
        String screenPath = tracker.getLatestVisionPath();
        Point tabCenter = findTemplateInScreen(TASK_TAB_FALLBACK_A_TEMPLATE, screenPath, TASK_TAB_MATCH_RATE, null);
        String matchedTemplate = TASK_TAB_FALLBACK_A_TEMPLATE;
        if (tabCenter == null) {
            tabCenter = findTemplateInScreen(TASK_TAB_FALLBACK_B_TEMPLATE, screenPath, TASK_TAB_MATCH_RATE, null);
            matchedTemplate = TASK_TAB_FALLBACK_B_TEMPLATE;
        }
        if (tabCenter == null) {
            log.warn("[bag] startup task-tab calibration did not match either task-tab template: windowId={} anchor=({}, {})",
                    windowId, baseAnchor.x, baseAnchor.y);
            return false;
        }
        double scale = coordinateHelper.getScaleRatio();
        double firstTabCenterY = baseAnchor.y + MAIN_BAG.tabOffsetY / scale;
        double stepY = MAIN_BAG.tabStepY / scale;
        int pageIndex = (int) Math.round((tabCenter.y - firstTabCenterY) / stepY);
        if (pageIndex < 0) {
            log.warn("[bag] startup task-tab calibration rejected negative page: windowId={} template={} tabCenter=({}, {}) anchor=({}, {}) pageIndex={}",
                    windowId, matchedTemplate, tabCenter.x, tabCenter.y, baseAnchor.x, baseAnchor.y, pageIndex);
            return false;
        }
        calibratedMainBagTaskTabIndex.compareAndSet(null, pageIndex);
        Integer calibrated = calibratedMainBagTaskTabIndex.get();
        log.info("[bag] main-bag task tab calibrated from exact task window: windowId={} template={} tabCenter=({}, {}) anchor=({}, {}) pageIndex={} click=({}, {})",
                windowId, matchedTemplate, tabCenter.x, tabCenter.y, baseAnchor.x, baseAnchor.y, calibrated,
                baseAnchor.x + (int) Math.round(MAIN_BAG.tabOffsetX / scale),
                baseAnchor.y + (int) Math.round((MAIN_BAG.tabOffsetY + calibrated * MAIN_BAG.tabStepY) / scale));
        return true;
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

    private boolean pressBackgroundAltE(String source) {
        if (inputProvider.requiresForegroundKeyboard()) {
            var current = windowTaskContextHolder.rawCurrent();
            if (current.isEmpty() || current.get().getNativeBinding() == null) {
                log.warn("[bag] driver Alt+E rejected without an exact window binding: source={}", source);
                return false;
            }
            var context = current.get();
            boolean focused = windowFocusService.isForeground(context.getNativeBinding());
            log.info("[bag] driver Alt+E foreground check: source={} windowId={} focused={}",
                    source, context.getWindowId(), focused);
            if (!focused) {
                log.warn("[bag] driver Alt+E rejected because queue-entry focus no longer owns exact window: "
                                + "source={} windowId={} handle={}",
                        source, context.getWindowId(), context.getNativeBinding().getNativeHandle());
                return false;
            }
            try {
                inputProvider.pressAltE();
                return true;
            } catch (RuntimeException inputFailure) {
                log.warn("[bag] FakerInput Alt+E failed: source={} reason={}",
                        source, inputFailure.toString());
                return false;
            }
        }
        var current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty() || current.get().getNativeBinding() == null) {
            log.warn("[bag] background Alt+E rejected without an exact window binding: source={}", source);
            return false;
        }
        var context = current.get();
        var attempt = boundWindowKeyboardService.pressShortcut(
                context.getNativeBinding(), context.getWindowId(),
                BoundWindowKeyboardService.AltShortcut.ALT_E);
        if (!attempt.attempted() || !attempt.success()) {
            log.warn("[bag] background Alt+E failed: source={} windowId={} reason={}",
                    source, context.getWindowId(), attempt.reason());
            return false;
        }
        return true;
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

        /**
         * Uses an item from the first main-bag tab only.
         *
         * <p>新手节点的升级/海螺/修复道具明确固定在 tab {@code 0}。This method deliberately
         * does not fall through to another page: a visual match on a different tab would be a
         * different item-selection decision.</p>
         *
         * @param targetItemTemplate local template path for the item inside tab {@code 0}; nonblank.
         * @return true only when this tab contained the template and the serialized right click ran.
         */
        public boolean useItemOnFirstTab(String targetItemTemplate) {
            Point item = searchItemInTabOnly(MAIN_BAG, baseAnchor, targetItemTemplate, 0, context);
            return item != null && executeSafeAction(item, ItemAction.USE, context);
        }
    }
}
