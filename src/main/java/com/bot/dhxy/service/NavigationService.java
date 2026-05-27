package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.npc.NpcClickRequest;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogHandleResult;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.vision.GameTextLineOcrService;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates map navigation for the currently bound game window.
 *
 * <p>This service owns navigation mechanics: it converts logical game map
 * coordinates into screen-absolute click points, submits real input through
 * {@link InputSequences}, and keeps screenshots/OCR tied to the current
 * {@link WindowTaskContextHolder} binding. Callers must pass logical in-game
 * map coordinates, not screen pixels. Task-level turn/yield policy belongs to
 * the task transaction layer, not the mini-map click helper.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NavigationService {

    private static final String XUNLU_TEMPLATE_PATH = "images/template/xunlu.png";
    private static final int DEFAULT_LOGICAL_COORDINATE = Integer.MIN_VALUE;
    /*
     * Route-result OCR only needs the lower result-list area after the world-map search has been
     * scrolled to the bottom. These dimensions pair with bot.dhxy.anchor_windowTo_map_search_X/Y
     * and are intentionally narrower than the whole route panel so OCR sees fewer white hints,
     * borders, and scroll controls. The current calibration was measured from base=(1461,525):
     * crop=(1809,901)-(2132,1039), so the window-relative rectangle is (348,376)-(671,514).
     */
    private static final int MAP_SEARCH_RECT_WIDTH = 323;
    private static final int MAP_SEARCH_RECT_HEIGHT = 138;
    private static final double THRESHOLD_NORMAL = 0.8;
    private static final int MAP_POPUP_RECT_X_OFFSET = 278;
    private static final int MAP_POPUP_RECT_Y_OFFSET = 595;
    private static final int MAP_POPUP_RECT_WIDTH = 406;
    private static final int MAP_POPUP_RECT_HEIGHT = 137;
    private static final int MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS = 2;
    private static final int MAP_RESULT_SCROLL_DOWN_UNITS = 3;
    private static final long MAP_RESULT_SCROLL_INTERVAL_MS = 80L;
    private static final long MAP_RESULT_SCROLL_SETTLE_MS = 300L;
    private static final long MOVING_NAVIGATION_YIELD_MS = 1500L;
    private static final long MINI_MAP_PATHING_CONFIRM_TIMEOUT_MS = 2200L;
    private static final long MINI_MAP_PATHING_CONFIRM_POLL_MS = 250L;
    private static final int MAP_NAVIGATION_RECLICK_STUCK_SCANS = 2;
    private static final int MAP_NAVIGATION_REOPEN_STUCK_SCANS = 3;
    private static final String MAP_LING_SHOU_VILLAGE = "灵兽村";
    private static final String MAP_CHANG_AN = "长安";
    private static final String NPC_ZHANG_WEN = "张闻";
    private static final int ZHANG_WEN_APPROACH_X = 219;
    private static final int ZHANG_WEN_APPROACH_Y = 100;
    private static final int ZHANG_WEN_NPC_X = 224;
    private static final int ZHANG_WEN_NPC_Y = 100;
    private static final long LING_SHOU_ROUTE_CONFIRM_TIMEOUT_MS = 20000L;

    private final BotProperties config;
    private final GameContext context;
    private final GameClientTracker tracker;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final GameTextLineOcrService gameTextLineOcrService;
    private final GameStateUtil gameStateUtil;
    private final CoordinateHelper coordinateHelper;
    private final UICleanerService uiCleanerService;
    private final DialogService dialogService;
    private final NpcClickService npcClickService;
    private final WindowScopedTempPath windowScopedTempPath;
    private final Random random = new Random();
    private final PlayerStateService playerStateService;
    private final BattleRadarService battleRadarService;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final BoundWindowKeyboardService boundWindowKeyboardService;

    private final Map<String, NavigationRuntimeState> runtimeStates = new ConcurrentHashMap<>();

    // ==============================
    // Public navigation entry points
    // ==============================

    /**
     * Navigate to a fixed NPC coordinate, optionally keeping the current task turn after pathing starts.
     *
     * @param request NPC navigation request. Coordinates are logical in-game map coordinates; nullable
     *                fields such as target name/source are used only for diagnostics. When
     *                returnOnPathingStarted is true, the method reports PATHING_STARTED as soon as
     *                either the map route or the current-map click begins moving.
     * @return structured navigation result. ARRIVED means map navigation and current-map coordinate
     *         navigation completed; other statuses tell task code whether to retry, yield, or fail.
     */
    public NavigationResult navigateToNPC(NavigationRequest request) {
        if (request == null) {
            log.warn("navigateToNPC skipped: request is null");
            return NavigationResult.failed("request is null");
        }
        if (request.getTargetMapName() == null || request.getTargetMapName().isBlank()
                || request.getTargetX() == null || request.getTargetY() == null) {
            log.warn("navigateToNPC skipped: incomplete target request={}", request);
            return NavigationResult.failed("incomplete target request");
        }
        long latencyStart = LatencyMetrics.start();
        NavigationResult result = NavigationResult.failed("not started");
        try {
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

            // Step 1: only solve the cross-map route. This method must not hide map+coordinate as a new abstraction.
            NavigationResult mapResult = navigateToMap(request.toBuilder()
                    .source(request.getSource() + ":map")
                    .build());
            if (!mapResult.success()) {
                result = mapResult;
                return result;
            }
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

            // Step 2: after the map is correct, click/path to the NPC's logical coordinate on that map.
            NavigationResult currentMapResult = navigateInCurrentMap(request.toBuilder()
                    .source(request.getSource() + ":currentMap")
                    .build());
            if (!currentMapResult.success()) {
                result = currentMapResult;
                return result;
            }
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

            // Step 3: NPC navigation never cleans dialogs here; the task layer owns the opened option/story dialog.
            log.info("skip arrival cleanup after NPC navigation; task layer will process any opened dialog");
            result = NavigationResult.arrived("npc coordinate reached");
            return result;
        } finally {
            LatencyMetrics.info(log, "navigation.toNpc", latencyStart,
                    "result=" + result.getStatus() + " source=" + request.getSource() + " target=" + request.getTargetMapName()
                            + "(" + request.getTargetX() + "," + request.getTargetY() + ")"
                            + " returnOnPathing=" + request.isReturnOnPathingStarted());
        }
    }

    /**
     * Navigate across maps using the world-map search UI.
     *
     * @param request map navigation request. The target map name is the game-visible map name used
     *                for route search and arrival confirmation. returnOnPathingStarted lets phase
     *                tasks yield after the world-map route begins moving.
     * @return structured navigation result. ARRIVED means the game reaches the target map or already
     *         appears to be there.
     */
    private NavigationResult navigateToMap(NavigationRequest request) {
        if (request == null) {
            log.warn("navigateToMap skipped: request is null");
            return NavigationResult.failed("request is null");
        }
        String targetMapName = request.getTargetMapName();
        String source = request.getSource();
        long latencyStart = LatencyMetrics.start();
        NavigationResult result = NavigationResult.mapNotReached("not started");
        try {
            PlayerCharacter me = context.getMe();
            log.info("navigate to map: {} current={}", targetMapName, me.getCurrentMapName());

            /*
             * Fast path: cached state already says we are on the target map. Avoid opening the world map
             * and burning input queue time when no cross-map route is needed.
             */
            if (targetMapName.equals(me.getCurrentMapName())) {
                result = NavigationResult.arrived("already on target map");
                return result;
            }

            /*
             * If the cache is blank, do exactly one shared map confirmation before submitting input.
             * A blank map usually means startup/registration has not refreshed identity/location yet.
             */
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
            if (me.getCurrentMapName() == null || me.getCurrentMapName().isBlank()) {
                log.info("current map is unknown before navigation, confirming target map once");
                boolean arrivedAfterSync = gameStateUtil.confirmCurrentMapFresh(
                        targetMapName, 0L, "navigateToMap:blankCurrentMap");
                log.info("navigate to map after sync: {} current={}", targetMapName, me.getCurrentMapName());
                if (arrivedAfterSync) {
                    result = NavigationResult.arrived("target map confirmed after sync");
                    return result;
                }
            }

            /*
             * Ling Shou Village cannot be reached by the normal world-map search. Its validated
             * entrance is Chang'an -> Zhang Wen -> transfer option, so failures here must retry that
             * NPC chain instead of falling through to a generic route search for the target map.
             */
            if (MAP_LING_SHOU_VILLAGE.equals(targetMapName)) {
                result = navigateToLingShouVillageViaZhangWen(request);
                return result;
            }

            /*
             * First route submission: open the world map, search the target map, scroll to the bottom
             * result, and click the last route link. The called method owns the exclusive input section.
             */
            if (!submitWorldMapSearchAndClickDestination(targetMapName)) {
                log.warn("first navigate attempt failed, entering retry loop");
            }

            long startTime = System.currentTimeMillis();
            long timeoutMs = 180000L;
            int stuckCount = 0;
            String lastObservedMapName = me.getCurrentMapName();

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

                /*
                 * Movement means the submitted route is still making progress. Do not fight for focus
                 * or re-click the world-map result while the game is already pathing.
                 */
                GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                if (isActiveNavigationMovement(movementState)) {
                    stuckCount = 0;
                    if (request.isReturnOnPathingStarted()) {
                        result = NavigationResult.pathingStarted("map route pathing started");
                        return result;
                    }
                    log.info("navigate to map yielding while moving: target={} state={} sleepMs={}",
                            targetMapName, movementState, MOVING_NAVIGATION_YIELD_MS);
                    if (!TaskSleep.sleep(MOVING_NAVIGATION_YIELD_MS)) {
                        result = NavigationResult.stopped("interrupted while waiting for map pathing");
                        return result;
                    }
                    TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                    continue;
                }

                /*
                 * Stopped movement may be a route option dialog rather than a real failure. Handle the
                 * route dialog before paying for OCR or re-clicking the last search result.
                 */
                DialogHandleResult dialogResult = dialogService.handleDialog(
                        DialogHandleRequest.clickKeyword("navigation", targetMapName, true));
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                if (dialogResult == DialogHandleResult.OPTION_KEYWORD_CLICKED
                        || dialogResult == DialogHandleResult.FALLBACK_CLICKED) {
                    stuckCount = 0;
                    if (!TaskSleep.sleep(1500)) {
                        result = NavigationResult.stopped("interrupted after route dialog handling");
                        return result;
                    }
                    TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                    continue;
                }

                /*
                 * OCR/template location confirmation is the authoritative map-arrival check. It is
                 * intentionally after dialog handling because route dialogs can block the mini-map label.
                 */
                LocationInfo locationInfo = playerStateService.syncMyPosition();
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                if (locationInfo != null) {
                    if (targetMapName.equals(locationInfo.mapName)) {
                        log.info("arrived map: {}", targetMapName);
                        result = NavigationResult.arrived("target map reached");
                        return result;
                    }
                    /*
                     * Multi-hop world-map routes can briefly stop on intermediate maps such as
                     * Fengchao floors while the game opens/handles the next transfer step. Treat a map
                     * change as fresh route progress, not as a stuck signal. Re-clicking the cached
                     * world-map result here can reopen the search overlay and interrupt the route that
                     * is already progressing.
                     */
                    if (lastObservedMapName == null || !lastObservedMapName.equals(locationInfo.mapName)) {
                        log.info("navigate to map observed intermediate map progress: target={} previous={} current={} coord=({}, {}), wait before retry",
                                targetMapName, lastObservedMapName, locationInfo.mapName, locationInfo.x, locationInfo.y);
                        lastObservedMapName = locationInfo.mapName;
                        stuckCount = 0;
                        if (!TaskSleep.sleep(1500)) {
                            result = NavigationResult.stopped("interrupted while waiting on intermediate map");
                            return result;
                        }
                        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                        continue;
                    }
                }

                /*
                 * Retry policy: require repeated still/non-target observations before re-clicking or
                 * reopening the world-map search. This avoids interrupting multi-hop transfer progress.
                 */
                stuckCount++;
                if (stuckCount < MAP_NAVIGATION_RECLICK_STUCK_SCANS) {
                    /*
                     * A route can pause on any intermediate map while the game opens a transfer dialog,
                     * loads the next floor, or briefly reports no movement. Do not re-click the world-map
                     * result on the first still frame; require the same non-target state to remain stable
                     * for several scans before treating it as stuck.
                     */
                    log.info("navigate to map non-target still frame, wait before retry: target={} current={} stuckScan={}/{}",
                            targetMapName, me.getCurrentMapName(), stuckCount, MAP_NAVIGATION_RECLICK_STUCK_SCANS);
                } else if (stuckCount >= MAP_NAVIGATION_REOPEN_STUCK_SCANS) {
                    if (submitWorldMapSearchAndClickDestination(targetMapName)) {
                        stuckCount = 0;
                    }
                } else {
                    retryWorldMapDestinationClick(targetMapName);
                }
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

                if (!TaskSleep.sleep(1500)) {
                    result = NavigationResult.stopped("interrupted while waiting before map retry");
                    return result;
                }
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
            }

            log.error("map navigation timeout");
            result = NavigationResult.mapNotReached("map navigation timeout");
            return result;
        } finally {
            LatencyMetrics.info(log, "navigation.toMap", latencyStart,
                    "result=" + result.getStatus() + " source=" + source + " target=" + targetMapName
                            + " returnOnPathing=" + request.isReturnOnPathingStarted());
        }
    }

    /**
     * Navigate within the current map by clicking mini-map logical coordinates until arrival.
     *
     * @param request current-map navigation request. targetX/targetY are logical in-game coordinates
     *                on the active map; returnOnPathingStarted lets phase tasks yield after a mini-map
     *                click starts movement.
     * @return structured navigation result. ARRIVED means the current window reaches the coordinate
     *         tolerance; POINT_NOT_REACHED means timeout or exhausted click candidates.
     */
    private NavigationResult navigateInCurrentMap(NavigationRequest request) {
        if (request == null) {
            log.warn("navigateInCurrentMap skipped: request is null");
            return NavigationResult.failed("request is null");
        }
        if (request.getTargetX() == null || request.getTargetY() == null) {
            log.warn("navigateInCurrentMap skipped: target coordinate is null request={}", request);
            return NavigationResult.failed("target coordinate is null");
        }
        int targetX = request.getTargetX();
        int targetY = request.getTargetY();
        long latencyStart = LatencyMetrics.start();
        NavigationResult result = NavigationResult.pointNotReached("not started");
        try {
            String mapName = context.getMe().getCurrentMapName();
            log.info("navigate in map: {} target=({}, {})", mapName, targetX, targetY);

            long startTime = System.currentTimeMillis();
            long timeoutMs = 60000;
            int failedMiniMapClicks = 0;

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

                if (battleRadarService.checkAndSyncCombatState()) {
                    log.warn("navigate in current map interrupted by battle: target=({}, {})", targetX, targetY);
                    result = NavigationResult.interrupted("interrupted by battle");
                    return result;
                }

                context.setCurrentActionState(GameContext.ActionState.NAVIGATING);

                if (syncAndCheckArrived(targetX, targetY, "navigateInCurrentMap:loop")) {
                    result = NavigationResult.arrived("target coordinate reached");
                    return result;
                }

                GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                if (isActiveNavigationMovement(movementState)) {
                    if (request.isReturnOnPathingStarted()) {
                        result = NavigationResult.pathingStarted("current-map pathing already active");
                        return result;
                    }
                    log.info("navigate in current map yielding while moving: target=({}, {}) state={} sleepMs={}",
                            targetX, targetY, movementState, MOVING_NAVIGATION_YIELD_MS);
                    if (!TaskSleep.sleep(MOVING_NAVIGATION_YIELD_MS)) {
                        result = NavigationResult.stopped("interrupted while waiting for map pathing");
                        return result;
                    }
                    TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                    continue;
                }

                CoordinateHelper.MiniMapClickPoint clickPoint = coordinateHelper.resolveMiniMapClickPoint(
                        mapName, targetX, targetY, failedMiniMapClicks);
                if (clickPoint == null) {
                    log.warn("navigate in current map exhausted mini-map click points: target=({}, {}) failedClicks={}",
                            targetX, targetY, failedMiniMapClicks);
                    result = NavigationResult.pointNotReached("exhausted mini-map click points");
                    return result;
                }

                MiniMapPathingAttemptResult attemptResult = clickMiniMapPointAndConfirm(
                        clickPoint, "navigateInCurrentMap:click");
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                if (attemptResult == MiniMapPathingAttemptResult.PATHING_STARTED) {
                    log.info("navigate in current map mini-map click started pathing: target=({}, {}) clickPoint=({}, {}) reason={}",
                            targetX, targetY, clickPoint.logicalX(), clickPoint.logicalY(), clickPoint.reason());
                    if (request.isReturnOnPathingStarted()) {
                        result = NavigationResult.pathingStarted("current-map mini-map click started pathing");
                        return result;
                    }
                    continue;
                }
                if (attemptResult == MiniMapPathingAttemptResult.NO_PATHING) {
                    if (battleRadarService.checkAndSyncCombatState()) {
                        log.warn("navigate in current map mini-map confirmation was interrupted by battle; keep original click point: target=({}, {})",
                                targetX, targetY);
                        result = NavigationResult.interrupted("mini-map click interrupted by battle");
                        return result;
                    }
                    failedMiniMapClicks++;
                } else {
                    result = NavigationResult.pointNotReached("mini-map click failed");
                    return result;
                }

                if (!TaskSleep.sleep(500)) {
                    result = NavigationResult.stopped("interrupted while waiting before current-map retry");
                    return result;
                }
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
            }

            log.error("navigate timeout");
            result = NavigationResult.pointNotReached("current-map navigation timeout");
            return result;
        } finally {
            LatencyMetrics.info(log, "navigation.currentMap", latencyStart,
                    "result=" + result.getStatus() + " source=" + request.getSource()
                            + " target=(" + targetX + "," + targetY + ")"
                            + " returnOnPathing=" + request.isReturnOnPathingStarted());
        }
    }

    // =========================
    // Special map-entry routes
    // =========================

    /**
     * Enter Ling Shou Village through Zhang Wen in Chang'an.
     *
     * <p>This is a map-entry rule rather than a Xiuluo-task shortcut. The world-map search cannot
     * reach Ling Shou Village directly, so the normal route target is redirected to Chang'an first.
     * The individual steps keep using their existing retry/fallback behavior; this method only
     * composes the validated transfer chain.</p>
     */
    private NavigationResult navigateToLingShouVillageViaZhangWen(NavigationRequest request) {
        PlayerCharacter me = context.getMe();
        log.info("navigate to Ling Shou Village through Zhang Wen: current={}", me.getCurrentMapName());
        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

        /*
         * This special route is still step-wise navigation. If going to Chang'an or approaching
         * Zhang Wen starts movement, return PATHING_STARTED so the task phase can yield like any
         * other navigation leg. The next task turn will re-enter this method and continue from the
         * current map/state instead of hiding a long route chain inside NavigationService.
         */
        NavigationResult changAnResult = navigateToMap(request.toBuilder()
                .targetMapName(MAP_CHANG_AN)
                .source(request.getSource() + ":viaChangAn")
                .build());
        if (changAnResult.getStatus() == NavigationResultStatus.PATHING_STARTED) {
            return changAnResult;
        }
        if (!changAnResult.success()) {
            log.warn("Ling Shou Village route failed before Zhang Wen: unable to reach Chang'an");
            return NavigationResult.mapNotReached("Ling Shou Village route failed before Zhang Wen");
        }
        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

        NavigationResult zhangWenApproachResult = navigateInCurrentMap(request.toBuilder()
                .targetX(ZHANG_WEN_APPROACH_X)
                .targetY(ZHANG_WEN_APPROACH_Y)
                .source(request.getSource() + ":zhangWenApproach")
                .build());
        if (zhangWenApproachResult.getStatus() == NavigationResultStatus.PATHING_STARTED) {
            return zhangWenApproachResult;
        }
        if (!zhangWenApproachResult.success()) {
            log.warn("Ling Shou Village route failed: unable to approach Zhang Wen target=({}, {})",
                    ZHANG_WEN_APPROACH_X, ZHANG_WEN_APPROACH_Y);
            return NavigationResult.pointNotReached("Ling Shou Village route failed near Zhang Wen");
        }
        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

        boolean npcClicked = npcClickService.clickNpcSmart(NpcClickRequest.fixed(
                me, MAP_CHANG_AN, ZHANG_WEN_NPC_X, ZHANG_WEN_NPC_Y, NPC_ZHANG_WEN, null));
        if (!npcClicked) {
            log.warn("Ling Shou Village route Zhang Wen click not verified, checking dialog anyway");
        }
        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

        DialogHandleResult dialogResult = dialogService.handleDialog(
                DialogHandleRequest.clickKeyword("navigation:ling-shou-village", MAP_LING_SHOU_VILLAGE, false));
        if (dialogResult != DialogHandleResult.OPTION_KEYWORD_CLICKED) {
            log.warn("Ling Shou Village route transfer option not handled: result={}", dialogResult);
            return NavigationResult.mapNotReached("Ling Shou Village transfer option not handled");
        }

        boolean arrived = gameStateUtil.confirmCurrentMap(
                MAP_LING_SHOU_VILLAGE,
                LING_SHOU_ROUTE_CONFIRM_TIMEOUT_MS,
                "navigateToLingShouVillage");
        log.info("Ling Shou Village route confirm result={}", arrived);
        return arrived
                ? NavigationResult.arrived("Ling Shou Village reached through Zhang Wen")
                : NavigationResult.mapNotReached("Ling Shou Village route confirm failed");
    }

    // ========================
    // World-map search helpers
    // ========================

    private boolean retryWorldMapDestinationClick(String targetMapName) {
        NavigationRuntimeState state = state();
        if (state.lastAbsoluteLogicalX != DEFAULT_LOGICAL_COORDINATE
                && state.lastAbsoluteLogicalY != DEFAULT_LOGICAL_COORDINATE) {
            int clickX = state.lastAbsoluteLogicalX + random.nextInt(7) - 3;
            int clickY = state.lastAbsoluteLogicalY + random.nextInt(7) - 3;
            boolean clicked = inputSequences.submitExclusiveAndWait("retryWorldMapDestinationClick", () -> {
                if (!openWorldMapRoutePanelDirect()) {
                    return false;
                }
                inputProvider.clickLeft(clickX, clickY, 150);
                if (!TaskSleep.sleep(2000)) {
                    return false;
                }
                gameStateUtil.recordMovementIntent("retryWorldMapDestinationClick");
                return true;
            });
            return clicked;
        }
        return targetMapName != null && !targetMapName.isBlank()
                && submitWorldMapSearchAndClickDestination(targetMapName);
    }

    private boolean submitWorldMapSearchAndClickDestination(String targetMapName) {
        boolean clicked = inputSequences.submitExclusiveAndWait("submitWorldMapSearchAndClickDestination:" + targetMapName,
                () -> {
                    log.info("navigation map search start: target={}", targetMapName);
                    if (!isWorldMapOpened()) {
                        log.info("navigation map search: world map not open, press Alt+2");
                        inputProvider.pressAlt2();
                        TaskSleep.sleep(500);
                    }

                    Point xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(XUNLU_TEMPLATE_PATH, THRESHOLD_NORMAL);
                    if (xunluPoint == null) {
                        log.warn("navigation map search: xunlu button not found, target={}", targetMapName);
                        return false;
                    }

                    boolean searchInputTouched = false;
                    boolean routeClicked = false;
                    try {
                        for (int attempt = 1; attempt <= 2; attempt++) {
                            /*
                             * From this point the route-search input may stay on screen if OCR/scroll/click
                             * fails. Always use the narrow x2-only cleanup on failure so later Alt+1 mini-map
                             * navigation does not click through a stale search overlay.
                             */
                            log.info("navigation map search: click xunlu button=({}, {}) attempt={}/{}",
                                    xunluPoint.x, xunluPoint.y, attempt, 2);
                            inputProvider.clickLeft(xunluPoint.x, xunluPoint.y, 120);
                            searchInputTouched = true;
                            TaskSleep.sleep(250);

                            int scrollFocusX = tracker.getWindowBaseX() + config.getAnchor_windowTo_map_scroll_X();
                            int scrollFocusY = tracker.getWindowBaseY() + config.getAnchor_windowTo_map_scroll_Y();
                            log.info("navigation map search: type target map={} attempt={}/{}", targetMapName, attempt, 2);
                            inputProvider.typeTextUnicode(targetMapName);
                            TaskSleep.sleep(100);
                            inputProvider.pressEnter();
                            if (!scrollWorldMapSearchResultsToBottomDirect(scrollFocusX, scrollFocusY,
                                    "submitWorldMapSearchAndClickDestination:" + targetMapName + ":attempt" + attempt)) {
                                return false;
                            }

                            WorldMapDestinationClickResult status = clickDestinationFromWorldMapSearchResults(
                                    "submitWorldMapSearchAndClickDestination:lastLink", true, targetMapName);
                            routeClicked = status == WorldMapDestinationClickResult.CLICKED;
                            log.info("navigation map search: last coordinate click result={} status={} attempt={}/{}",
                                    routeClicked, status, attempt, 2);
                            if (routeClicked) {
                                return true;
                            }
                            if (status == WorldMapDestinationClickResult.WRONG_DESTINATION && attempt < 2) {
                                closeMapSearchInputAfterRouteClick(
                                        "submitWorldMapSearchAndClickDestination:destinationMismatch:attempt" + attempt);
                                searchInputTouched = false;
                                if (!TaskSleep.sleep(250)) {
                                    return false;
                                }
                                continue;
                            }
                            return false;
                        }
                        return false;
                    } finally {
                        if (searchInputTouched && !routeClicked) {
                            closeMapSearchInputAfterRouteClick("submitWorldMapSearchAndClickDestination:failed");
                        }
                    }
                });
        return clicked;
    }

    private WorldMapDestinationClickResult clickDestinationFromWorldMapSearchResults(String description,
                                                                                    boolean directInput,
                                                                                    String expectedDestinationName) {
        int[] mapRect = coordinateHelper.getScaledRect(
                config.getAnchor_windowTo_map_search_X(), config.getAnchor_windowTo_map_search_Y(),
                MAP_SEARCH_RECT_WIDTH, MAP_SEARCH_RECT_HEIGHT);

        String mapResultImagePath = windowScopedTempPath.resolve("map_result_scan.png");
        log.info("navigation map search: scan result image={} rect=({}, {})-({}, {})",
                mapResultImagePath, mapRect[0], mapRect[1], mapRect[2], mapRect[3]);
        if (!tracker.captureToFile("map result", mapResultImagePath, mapRect[0], mapRect[1], mapRect[2], mapRect[3])) {
            log.warn("navigation map search: map result capture failed");
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        GameTextLineOcrService.WorldMapRouteDestinationResult destinationResult =
                gameTextLineOcrService.verifyWorldMapRouteDestination(mapResultImagePath, expectedDestinationName);
        if (!destinationResult.allowClick()) {
            log.warn("navigation map search: destination mismatch before route click, will retype target expected={} actual={} yellow={}",
                    expectedDestinationName, destinationResult.rawActual(), destinationResult.yellowImagePath());
            return WorldMapDestinationClickResult.WRONG_DESTINATION;
        }

        GameTextLineOcrService.WorldMapRouteCoordinateResult coordinateResult =
                gameTextLineOcrService.findLastWorldMapRouteCoordinate(mapResultImagePath);
        Point relativeCenter = coordinateResult.relativeCenter();
        String routeOcrImagePath = coordinateResult.ocrImagePath();
        if (relativeCenter == null) {
            log.warn("navigation route scan found no coordinate link");
            return WorldMapDestinationClickResult.NOT_FOUND;
        }

        NavigationRuntimeState state = state();
        state.lastAbsoluteLogicalX = mapRect[0] + relativeCenter.x;
        state.lastAbsoluteLogicalY = mapRect[1] + relativeCenter.y;
        log.info("navigation route coordinate click: base=({}, {}) mapRect=({}, {})-({}, {}) "
                        + "image={} relative=({}, {}) absolute=({}, {})",
                tracker.getWindowBaseX(), tracker.getWindowBaseY(),
                mapRect[0], mapRect[1], mapRect[2], mapRect[3], routeOcrImagePath,
                relativeCenter.x, relativeCenter.y, state.lastAbsoluteLogicalX, state.lastAbsoluteLogicalY);

        if (directInput) {
            inputProvider.clickLeft(state.lastAbsoluteLogicalX, state.lastAbsoluteLogicalY, 150);
            closeMapSearchInputAfterRouteClick(description);
            if (!TaskSleep.sleep(2000)) {
                return WorldMapDestinationClickResult.NOT_FOUND;
            }
            gameStateUtil.recordMovementIntent(description);
            return WorldMapDestinationClickResult.CLICKED;
        }
        boolean submitted = inputSequences.submitAndWait(description, List.of(
                InputAction.clickLeft(state.lastAbsoluteLogicalX, state.lastAbsoluteLogicalY, 150),
                InputAction.sleep(2000)
        ));
        if (submitted) {
            gameStateUtil.recordMovementIntent(description);
        }
        return submitted ? WorldMapDestinationClickResult.CLICKED : WorldMapDestinationClickResult.NOT_FOUND;
    }

    /**
     * Close the world-map search input after clicking a route result.
     *
     * <p>This method is called only from direct-input navigation paths that already own the exclusive
     * input worker callback. It delegates to {@link UICleanerService} but restricts the close template
     * to {@code x2.png}; the broad generic-window cleanup is deliberately avoided because a route click
     * may leave task or dialog UI nearby and navigation should not close unrelated panels.</p>
     *
     * @param source navigation source label for logs.
     */
    private void closeMapSearchInputAfterRouteClick(String source) {
        boolean closed = uiCleanerService.closeMapSearchInputByX2Direct("navigation:" + source + ":closeMapSearchInput");
        log.info("navigation map search: x2-only close after route click source={} closed={}", source, closed);
    }

    private boolean openWorldMapRoutePanelDirect() {
        if (!isWorldMapOpened()) {
            inputProvider.pressAlt2();
            if (!TaskSleep.sleep(500)) {
                return false;
            }
        }

        Point xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(XUNLU_TEMPLATE_PATH, THRESHOLD_NORMAL);
        if (xunluPoint == null) {
            return false;
        }

        inputProvider.clickLeft(xunluPoint.x, xunluPoint.y, 120);
        return TaskSleep.sleep(250);
    }

    private boolean isWorldMapOpened() {
        Point titlePoint = coordinateHelper.findImageAbsoluteCoordinate("images/template/map/world_map_title.png", THRESHOLD_NORMAL);
        if (titlePoint != null) {
            return true;
        }

        int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
        return coordinateHelper.findImageInRegion("images/template/map/checkbox_checked.png", rect, 0.95) != null
                || coordinateHelper.findImageInRegion("images/template/map/checkbox_unchecked.png", rect, 0.95) != null;
    }


    private boolean scrollWorldMapSearchResultsToBottomDirect(int targetX, int targetY, String source) {
        log.info("navigation map search: force scroll to bottom source={} focus=({}, {}) attempts={} units={}",
                source, targetX, targetY, MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS, MAP_RESULT_SCROLL_DOWN_UNITS);
        inputProvider.clickLeft(targetX, targetY, 50);
        for (int i = 1; i <= MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS; i++) {
            inputProvider.scrollDown(MAP_RESULT_SCROLL_DOWN_UNITS);
            if (!TaskSleep.sleep(MAP_RESULT_SCROLL_INTERVAL_MS)) {
                return false;
            }
        }
        return TaskSleep.sleep(MAP_RESULT_SCROLL_SETTLE_MS);
    }

    // =====================
    // Mini-map click helpers
    // =====================

    /**
     * Submit one atomic mini-map click sequence.
     *
     * <p>Alt+1 open, map click, and Alt+1 close stay in one queued request so another window cannot
     * steal focus between the open and the close.</p>
     *
     * @param pixelPoint screen-absolute click point produced by {@link CoordinateHelper}.
     * @param description input queue request label.
     * @return true if the queue accepted and completed the sequence.
     */
    private boolean submitMiniMapClick(Point pixelPoint, String description) {
        return inputSequences.submitExclusiveAndWait(description, () -> {
            if (!isWorldMapOpened()) {
                pressAlt1ForMiniMap(description + ":open");
                if (!TaskSleep.sleep(800)) {
                    return false;
                }
            } else {
                log.info("mini-map already open before coordinate click: source={}", description);
            }

            if (!isWorldMapOpened()) {
                log.warn("mini-map did not open after Alt+1, retrying shortcut without title-bar click: source={}",
                        description);
                pressAlt1ForMiniMap(description + ":open-retry");
                if (!TaskSleep.sleep(800)) {
                    return false;
                }
            }

            if (!isWorldMapOpened()) {
                log.warn("mini-map still not open after retry, abort coordinate click: source={} pixel=({}, {})",
                        description, pixelPoint.x, pixelPoint.y);
                return false;
            }

            inputProvider.clickLeft(pixelPoint.x, pixelPoint.y, 200);
            if (!TaskSleep.sleep(500)) {
                return false;
            }
            pressAlt1ForMiniMap(description + ":close");
            if (!TaskSleep.sleep(300)) {
                return false;
            }

            if (isWorldMapOpened()) {
                log.warn("mini-map remained open after close shortcut, pressing Alt+1 once more: source={}", description);
                pressAlt1ForMiniMap(description + ":close-retry");
                if (!TaskSleep.sleep(300)) {
                    return false;
                }
            }
            return true;
        });
    }

    private void pressAlt1ForMiniMap(String source) {
        BoundWindowKeyboardService.ShortcutAttempt attempt =
                boundWindowKeyboardService.pressShortcut(BoundWindowKeyboardService.AltShortcut.ALT_1);
        if (attempt.attempted() && attempt.success()) {
            log.info("mini-map Alt+1 sent through HWND keyboard: source={}", source);
            return;
        }
        if (attempt.attempted()) {
            log.warn("mini-map HWND Alt+1 failed, fallback to focused input: source={} reason={}",
                    source, attempt.reason());
        } else {
            log.info("mini-map HWND Alt+1 not attempted, fallback to focused input: source={} reason={}",
                    source, attempt.reason());
        }
        inputProvider.pressAlt1();
    }


    /**
     * Submit one mini-map click point and confirm only whether it started pathing.
     *
     * @param clickPoint logical point and screen-absolute click point to try.
     * @param description log/input source prefix for this physical input sequence.
     * @return PATHING_STARTED when movement begins; NO_PATHING only when the click was submitted but
     *         produced no movement; INCONCLUSIVE when input/stop prevents judging the point.
     */
    private MiniMapPathingAttemptResult clickMiniMapPointAndConfirm(CoordinateHelper.MiniMapClickPoint clickPoint,
                                                                    String description) {
        if (clickPoint == null) {
            return MiniMapPathingAttemptResult.INCONCLUSIVE;
        }
        String source = description + ":" + clickPoint.reason()
                + ":logical=(" + clickPoint.logicalX() + "," + clickPoint.logicalY() + ")";
        if (!submitMiniMapClick(clickPoint.pixelPoint(), source)) {
            log.warn("mini-map click input failed: source={} pixel=({}, {})",
                    source, clickPoint.pixelPoint().x, clickPoint.pixelPoint().y);
            return MiniMapPathingAttemptResult.INCONCLUSIVE;
        }
        MiniMapPathingAttemptResult confirmResult = confirmMiniMapPathingStarted(source);
        if (confirmResult != MiniMapPathingAttemptResult.PATHING_STARTED) {
            log.info("mini-map click did not start pathing: source={} pixel=({}, {})",
                    source, clickPoint.pixelPoint().x, clickPoint.pixelPoint().y);
            return confirmResult;
        }
        gameStateUtil.recordMovementIntent(source);
        return MiniMapPathingAttemptResult.PATHING_STARTED;
    }

    /**
     * Refresh the current minimap coordinate and check whether the active window has reached a target.
     *
     * @param targetX logical in-game X coordinate.
     * @param targetY logical in-game Y coordinate.
     * @param source diagnostic source written to the arrival log.
     * @return true when the current coordinate is within the navigation tolerance; false otherwise.
     */
    private boolean syncAndCheckArrived(int targetX, int targetY, String source) {
        playerStateService.syncMyPosition();
        PlayerCharacter me = context.getMe();
        boolean arrived = Math.abs(me.getX() - targetX) <= 2 && Math.abs(me.getY() - targetY) <= 2;
        if (arrived) {
            log.info("arrived: source={} current=({}, {}) target=({}, {})",
                    source, me.getX(), me.getY(), targetX, targetY);
        }
        return arrived;
    }

    /**
     * Poll for the observable movement result of a mini-map click.
     *
     * @param source log label for the click attempt being confirmed.
     * @return PATHING_STARTED when pixel/coordinate movement starts, NO_PATHING on a clean timeout,
     *         or INCONCLUSIVE when task stop interrupts polling.
     */
    private MiniMapPathingAttemptResult confirmMiniMapPathingStarted(String source) {
        long deadline = System.currentTimeMillis() + MINI_MAP_PATHING_CONFIRM_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

            GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
            if (movementState == GameStateUtil.MovementState.MOVING
                    || movementState == GameStateUtil.MovementState.MAYBE_MOVING) {
                log.info("mini-map pathing confirmation: movement detected source={} state={}",
                        source, movementState);
                return MiniMapPathingAttemptResult.PATHING_STARTED;
            }

            log.info("mini-map pathing confirmation: no movement yet source={} state={}", source, movementState);
            if (!TaskSleep.sleep(MINI_MAP_PATHING_CONFIRM_POLL_MS)) {
                return MiniMapPathingAttemptResult.INCONCLUSIVE;
            }
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
        }
        return MiniMapPathingAttemptResult.NO_PATHING;
    }

    // ==========================
    // Shared navigation utilities
    // ==========================

    private boolean isActiveNavigationMovement(GameStateUtil.MovementState state) {
        return state == GameStateUtil.MovementState.MOVING
                || state == GameStateUtil.MovementState.PATHING_ACTIVE
                || state == GameStateUtil.MovementState.MAYBE_MOVING;
    }

    private NavigationRuntimeState state() {
        String key = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
        return runtimeStates.computeIfAbsent(key, ignored -> new NavigationRuntimeState());
    }

    private static class NavigationRuntimeState {
        private int lastAbsoluteLogicalX = DEFAULT_LOGICAL_COORDINATE;
        private int lastAbsoluteLogicalY = DEFAULT_LOGICAL_COORDINATE;
    }

    private enum MiniMapPathingAttemptResult {
        PATHING_STARTED,
        NO_PATHING,
        INCONCLUSIVE
    }

    private enum WorldMapDestinationClickResult {
        CLICKED,
        NOT_FOUND,
        WRONG_DESTINATION
    }
}
