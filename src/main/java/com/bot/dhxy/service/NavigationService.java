package com.bot.dhxy.service;



import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.navigation.MapNavigationRequest;
import com.bot.dhxy.model.navigation.NpcNavigationRequest;
import com.bot.dhxy.model.npc.NpcClickRequest;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogHandleResult;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private static final String MAP_LING_SHOU_VILLAGE = "\u7075\u517d\u6751";
    private static final String MAP_CHANG_AN = "\u957f\u5b89";
    private static final String NPC_ZHANG_WEN = "\u5f20\u95fb";
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
    private final TextRecognizer ocr;
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
    private final TaskTurnCoordinator taskTurnCoordinator;
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
     *                fields such as target name/source are used only for diagnostics.
     * @return true when map navigation and current-map coordinate navigation complete; false when
     *         input fails, transforms are missing, the task stops, or timeout occurs.
     */
    public boolean navigateToNPC(NpcNavigationRequest request) {
        if (request == null) {
            log.warn("navigateToNPC skipped: request is null");
            return false;
        }
        long latencyStart = LatencyMetrics.start();
        boolean releaseTurnOnPathing = !request.isKeepTaskTurnUntilHandled();
        boolean result = false;
        try {
            checkpointTask();

            // Step 1: only solve the cross-map route. This method must not hide map+coordinate as a new abstraction.
            if (!navigateToMap(MapNavigationRequest.builder()
                    .targetMapName(request.getTargetMapName())
                    .keepTaskTurnUntilHandled(request.isKeepTaskTurnUntilHandled())
                    .source(request.getSource() + ":map")
                    .build())) {
                if (releaseTurnOnPathing) {
                    taskTurnCoordinator.forceRelease("navigation:" + request.getSource() + ":map-failed");
                }
                return false;
            }
            checkpointTask();

            // Step 2: after the map is correct, click/path to the NPC's logical coordinate on that map.
            boolean currentMapResult = navigateInCurrentMap(request.getTargetX(), request.getTargetY());
            if (!currentMapResult) {
                if (releaseTurnOnPathing) {
                    taskTurnCoordinator.forceRelease("navigation:" + request.getSource() + ":current-map-failed");
                }
                return false;
            }
            checkpointTask();

            // Step 3: NPC navigation never cleans dialogs here; the task layer owns the opened option/story dialog.
            log.info("skip arrival cleanup after NPC navigation; task layer will process any opened dialog");
            result = true;
            return true;
        } finally {
            LatencyMetrics.info(log, "navigation.toNpc", latencyStart,
                    "result=" + result + " source=" + request.getSource() + " target=" + request.getTargetMapName()
                            + "(" + request.getTargetX() + "," + request.getTargetY() + ")"
                            + " keepTurn=" + request.isKeepTaskTurnUntilHandled());
        }
    }

    /**
     * Navigate across maps using the world-map search UI.
     *
     * @param request map navigation request. The target map name is the game-visible map name used
     *                for route search and arrival confirmation.
     * @return true when the game reaches the target map or already appears to be there.
     */
    public boolean navigateToMap(MapNavigationRequest request) {
        if (request == null) {
            log.warn("navigateToMap skipped: request is null");
            return false;
        }
        String targetMapName = request.getTargetMapName();
        String source = request.getSource();
        boolean releaseTurnOnPathing = !request.isKeepTaskTurnUntilHandled();
        long latencyStart = LatencyMetrics.start();
        boolean result = false;
        try {
            PlayerCharacter me = context.getMe();
            log.info("navigate to map: {} current={}", targetMapName, me.getCurrentMapName());

            /*
             * Fast path: cached state already says we are on the target map. Avoid opening the world map
             * and burning input queue time when no cross-map route is needed.
             */
            if (targetMapName.equals(me.getCurrentMapName())) {
                result = true;
                return true;
            }

            /*
             * If the cache is blank, do exactly one shared map confirmation before submitting input.
             * A blank map usually means startup/registration has not refreshed identity/location yet.
             */
            checkpointTask();
            if (me.getCurrentMapName() == null || me.getCurrentMapName().isBlank()) {
                log.info("current map is unknown before navigation, confirming target map once");
                boolean arrivedAfterSync = gameStateUtil.confirmCurrentMapFresh(
                        targetMapName, 0L, "navigateToMap:blankCurrentMap");
                log.info("navigate to map after sync: {} current={}", targetMapName, me.getCurrentMapName());
                if (arrivedAfterSync) {
                    result = true;
                    return true;
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
            ensureTaskTurn("navigateToMap:firstPathing");
            if (!submitWorldMapSearchAndClickDestination(targetMapName, releaseTurnOnPathing)) {
                log.warn("first navigate attempt failed, entering retry loop");
            }

            long startTime = System.currentTimeMillis();
            long timeoutMs = 180000L;
            int stuckCount = 0;
            String lastObservedMapName = me.getCurrentMapName();

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                checkpointTask();
                if (Thread.currentThread().isInterrupted()) {
                    return false;
                }

                /*
                 * Movement means the submitted route is still making progress. Do not fight for focus
                 * or re-click the world-map result while the game is already pathing.
                 */
                GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
                checkpointTask();
                if (isActiveNavigationMovement(movementState)) {
                    stuckCount = 0;
                    log.info("navigate to map yielding while moving: target={} state={} sleepMs={}",
                            targetMapName, movementState, MOVING_NAVIGATION_YIELD_MS);
                    if (!TaskSleep.sleep(MOVING_NAVIGATION_YIELD_MS)) {
                        return false;
                    }
                    checkpointTask();
                    continue;
                }

                /*
                 * Stopped movement may be a route option dialog rather than a real failure. Handle the
                 * route dialog before paying for OCR or re-clicking the last search result.
                 */
                ensureTaskTurn("navigateToMap:dialogOrRetry");
                DialogHandleResult dialogResult = dialogService.handleDialog(
                        DialogHandleRequest.clickKeyword("navigation", targetMapName, true));
                checkpointTask();
                if (dialogResult == DialogHandleResult.OPTION_KEYWORD_CLICKED
                        || dialogResult == DialogHandleResult.FALLBACK_CLICKED) {
                    stuckCount = 0;
                    if (!TaskSleep.sleep(1500)) {
                        return false;
                    }
                    checkpointTask();
                    continue;
                }

                /*
                 * OCR/template location confirmation is the authoritative map-arrival check. It is
                 * intentionally after dialog handling because route dialogs can block the mini-map label.
                 */
                LocationInfo locationInfo = playerStateService.syncMyPosition();
                checkpointTask();
                if (locationInfo != null) {
                    if (targetMapName.equals(locationInfo.mapName)) {
                        log.info("arrived map: {}", targetMapName);
                        result = true;
                        return true;
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
                            return false;
                        }
                        checkpointTask();
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
                    ensureTaskTurn("navigateToMap:reopenPathing");
                    if (submitWorldMapSearchAndClickDestination(targetMapName, releaseTurnOnPathing)) {
                        stuckCount = 0;
                    }
                } else {
                    ensureTaskTurn("navigateToMap:reclickPathing");
                    retryWorldMapDestinationClick(targetMapName, releaseTurnOnPathing);
                }
                checkpointTask();

                if (!TaskSleep.sleep(1500)) {
                    return false;
                }
                checkpointTask();
            }

            log.error("map navigation timeout");
            return false;
        } finally {
            LatencyMetrics.info(log, "navigation.toMap", latencyStart,
                    "result=" + result + " source=" + source + " target=" + targetMapName
                            + " keepTurn=" + request.isKeepTaskTurnUntilHandled());
        }
    }

    /**
     * Navigate within the current map by clicking mini-map logical coordinates until arrival.
     *
     * @param targetX logical in-game X coordinate on the active map.
     * @param targetY logical in-game Y coordinate on the active map.
     * @return true when the current window reaches the coordinate tolerance; false on stop, battle,
     *         exhausted click candidates, or timeout.
     */
    public boolean navigateInCurrentMap(int targetX, int targetY) {
        long latencyStart = LatencyMetrics.start();
        boolean result = false;
        try {
            String mapName = context.getMe().getCurrentMapName();
            log.info("navigate in map: {} target=({}, {})", mapName, targetX, targetY);

            long startTime = System.currentTimeMillis();
            long timeoutMs = 60000;
            int failedMiniMapClicks = 0;

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                checkpointTask();
                if (Thread.currentThread().isInterrupted()) {
                    return false;
                }

                if (battleRadarService.checkAndSyncCombatState()) {
                    log.warn("navigate in current map interrupted by battle: target=({}, {})", targetX, targetY);
                    return false;
                }

                context.setCurrentActionState(GameContext.ActionState.NAVIGATING);

                if (syncAndCheckArrived(targetX, targetY, "navigateInCurrentMap:loop")) {
                    result = true;
                    return true;
                }

                GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
                checkpointTask();
                if (isActiveNavigationMovement(movementState)) {
                    log.info("navigate in current map yielding while moving: target=({}, {}) state={} sleepMs={}",
                            targetX, targetY, movementState, MOVING_NAVIGATION_YIELD_MS);
                    if (!TaskSleep.sleep(MOVING_NAVIGATION_YIELD_MS)) {
                        return false;
                    }
                    checkpointTask();
                    continue;
                }

                ensureTaskTurn("navigateInCurrentMap:activeCheck");

                CoordinateHelper.MiniMapClickPoint clickPoint = coordinateHelper.resolveMiniMapClickPoint(
                        mapName, targetX, targetY, failedMiniMapClicks);
                if (clickPoint == null) {
                    log.warn("navigate in current map exhausted mini-map click points: target=({}, {}) failedClicks={}",
                            targetX, targetY, failedMiniMapClicks);
                    return false;
                }

                MiniMapPathingAttemptResult attemptResult = clickMiniMapPointAndConfirm(
                        clickPoint, "navigateInCurrentMap:click");
                checkpointTask();
                if (attemptResult == MiniMapPathingAttemptResult.PATHING_STARTED) {
                    log.info("navigate in current map mini-map click started pathing: target=({}, {}) clickPoint=({}, {}) reason={}",
                            targetX, targetY, clickPoint.logicalX(), clickPoint.logicalY(), clickPoint.reason());
                    continue;
                }
                if (attemptResult == MiniMapPathingAttemptResult.NO_PATHING) {
                    if (battleRadarService.checkAndSyncCombatState()) {
                        log.warn("navigate in current map mini-map confirmation was interrupted by battle; keep original click point: target=({}, {})",
                                targetX, targetY);
                        return false;
                    }
                    failedMiniMapClicks++;
                } else {
                    return false;
                }

                if (!TaskSleep.sleep(500)) {
                    return false;
                }
                checkpointTask();
            }

            log.error("navigate timeout");
            return false;
        } finally {
            LatencyMetrics.info(log, "navigation.currentMap", latencyStart,
                    "result=" + result + " target=(" + targetX + "," + targetY + ")");
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
    private boolean navigateToLingShouVillageViaZhangWen(MapNavigationRequest request) {
        boolean releaseTurnOnPathing = !request.isKeepTaskTurnUntilHandled();
        PlayerCharacter me = context.getMe();
        log.info("navigate to Ling Shou Village through Zhang Wen: current={}", me.getCurrentMapName());
        checkpointTask();

        if (!navigateToMap(request.toBuilder()
                .targetMapName(MAP_CHANG_AN)
                .source(request.getSource() + ":viaChangAn")
                .build())) {
            log.warn("Ling Shou Village route failed before Zhang Wen: unable to reach Chang'an");
            return false;
        }
        checkpointTask();

        if (!navigateInCurrentMap(ZHANG_WEN_APPROACH_X, ZHANG_WEN_APPROACH_Y)) {
            log.warn("Ling Shou Village route failed: unable to approach Zhang Wen target=({}, {})",
                    ZHANG_WEN_APPROACH_X, ZHANG_WEN_APPROACH_Y);
            return false;
        }
        checkpointTask();

        ensureTaskTurn("navigateToLingShouVillage:zhangWen");
        boolean npcClicked = npcClickService.clickNpcSmart(NpcClickRequest.fixed(
                me, MAP_CHANG_AN, ZHANG_WEN_NPC_X, ZHANG_WEN_NPC_Y, NPC_ZHANG_WEN, null));
        if (!npcClicked) {
            log.warn("Ling Shou Village route Zhang Wen click not verified, checking dialog anyway");
        }
        checkpointTask();

        ensureTaskTurn("navigateToLingShouVillage:transferOption");
        DialogHandleResult dialogResult = dialogService.handleDialog(
                DialogHandleRequest.clickKeyword("navigation:ling-shou-village", MAP_LING_SHOU_VILLAGE, false));
        if (dialogResult != DialogHandleResult.OPTION_KEYWORD_CLICKED) {
            log.warn("Ling Shou Village route transfer option not handled: result={}", dialogResult);
            return false;
        }

        boolean arrived = gameStateUtil.confirmCurrentMap(
                MAP_LING_SHOU_VILLAGE,
                LING_SHOU_ROUTE_CONFIRM_TIMEOUT_MS,
                "navigateToLingShouVillage");
        log.info("Ling Shou Village route confirm result={}", arrived);
        return arrived;
    }

    // ========================
    // World-map search helpers
    // ========================

    private boolean retryWorldMapDestinationClick(String targetMapName, boolean releaseTurnOnPathing) {
        ensureTaskTurn("retryWorldMapDestinationClick");
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
            if (clicked && releaseTurnOnPathing) {
                releaseTaskTurnAfterPathing("retryWorldMapDestinationClick");
            }
            return clicked;
        }
        return targetMapName != null && !targetMapName.isBlank()
                && submitWorldMapSearchAndClickDestination(targetMapName, releaseTurnOnPathing);
    }

    private boolean submitWorldMapSearchAndClickDestination(String targetMapName, boolean releaseTurnOnPathing) {
        ensureTaskTurn("submitWorldMapSearchAndClickDestination");
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
        if (clicked && releaseTurnOnPathing) {
            releaseTaskTurnAfterPathing("submitWorldMapSearchAndClickDestination:" + targetMapName);
        }
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
        if (!verifyMapRouteDestinationBeforeClick(mapResultImagePath, expectedDestinationName)) {
            return WorldMapDestinationClickResult.WRONG_DESTINATION;
        }
        String routeOcrImagePath = preprocessMapRouteResultForCoordinateOcr(mapResultImagePath);

        /*
         * Route-result OCR is the expensive part of the world-map search path. Keep a dedicated
         * timing log here so delays after scrolling can be separated from scroll/input latency.
         */
        long routeOcrStartedAt = System.currentTimeMillis();
        Point relativeCenter = ocr.findLastCoordinateLink(routeOcrImagePath);
        boolean usedGreenPreprocessedImage = !routeOcrImagePath.equals(mapResultImagePath);
        if (relativeCenter == null && usedGreenPreprocessedImage) {
            log.info("navigation map search: green route OCR missed, fallback raw image={}", mapResultImagePath);
            relativeCenter = ocr.findLastCoordinateLink(mapResultImagePath);
            routeOcrImagePath = mapResultImagePath;
        }
        log.info("navigation map search: route coordinate OCR elapsedMs={} found={} image={}",
                System.currentTimeMillis() - routeOcrStartedAt, relativeCenter != null, routeOcrImagePath);
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
            releaseTaskTurnAfterPathing(description);
        }
        return submitted ? WorldMapDestinationClickResult.CLICKED : WorldMapDestinationClickResult.NOT_FOUND;
    }

    /**
     * Confirm the route-result destination before clicking the final green coordinate.
     *
     * <p>The route result screenshot contains yellow map names on the left and green clickable
     * coordinates on the right. If OCR can read the final yellow map name and it does not match the
     * requested destination, this method stops the click so the caller can close the stale search
     * input and the navigation retry path can type the destination again. If yellow OCR is empty,
     * the method deliberately allows the old green-coordinate behavior to continue.</p>
     *
     * @param rawImagePath window-scoped route-result screenshot path.
     * @param expectedDestinationName map name originally typed into the world-map search box; null
     *                                or blank disables this guard for callers that only reuse an
     *                                already visible route result.
     * @return true when clicking may continue; false only for a clear yellow-destination mismatch.
     */
    private boolean verifyMapRouteDestinationBeforeClick(String rawImagePath, String expectedDestinationName) {
        if (expectedDestinationName == null || expectedDestinationName.isBlank()) {
            return true;
        }
        String yellowPath = preprocessMapRouteDestinationForOcr(rawImagePath);
        if (yellowPath.equals(rawImagePath)) {
            log.info("navigation map search: destination guard skipped, yellow preprocessing unavailable raw={}",
                    rawImagePath);
            return true;
        }

        long startedAt = System.currentTimeMillis();
        String lastYellowName = findLastYellowRouteDestination(yellowPath);
        String expected = normalizeRouteDestinationName(expectedDestinationName);
        String actual = normalizeRouteDestinationName(lastYellowName);
        boolean matched = !actual.isBlank() && actual.equals(expected);
        log.info("navigation map search: destination guard elapsedMs={} expected={} actual={} rawActual={} matched={} yellow={}",
                System.currentTimeMillis() - startedAt, expected, actual, lastYellowName, matched, yellowPath);
        if (actual.isBlank()) {
            return true;
        }
        if (!matched) {
            log.warn("navigation map search: destination mismatch before route click, will retype target expected={} actual={} yellow={}",
                    expectedDestinationName, lastYellowName, yellowPath);
        }
        return matched;
    }

    /**
     * Build the yellow-map-name OCR image from the same route-result screenshot used for green
     * coordinate detection.
     *
     * @param rawImagePath filesystem path to the current route-result screenshot.
     * @return yellow-washed image path, or the raw path when preprocessing fails.
     */
    private String preprocessMapRouteDestinationForOcr(String rawImagePath) {
        Path rawPath = Path.of(rawImagePath);
        String rawFileName = rawPath.getFileName().toString();
        String yellowFileName = rawFileName.endsWith(".png")
                ? rawFileName.substring(0, rawFileName.length() - ".png".length()) + "_yellow.png"
                : rawFileName + "_yellow.png";
        Path yellowPath = rawPath.resolveSibling(yellowFileName);
        try {
            ImagePreprocessor.washYellowText(rawImagePath, yellowPath.toString());
            if (Files.exists(yellowPath)) {
                log.info("navigation map search: route destination yellow preprocessing raw={} yellow={}",
                        rawImagePath, yellowPath);
                return yellowPath.toString();
            }
            log.warn("navigation map search: route destination yellow preprocessing produced no file raw={}", rawImagePath);
        } catch (Exception e) {
            log.warn("navigation map search: route destination yellow preprocessing failed raw={} reason={}",
                    rawImagePath, e.getMessage(), e);
        }
        return rawImagePath;
    }

    /**
     * Read the bottom-most yellow route map name from a washed route-result image.
     *
     * @param yellowImagePath yellow-washed route-result image path. OCR boxes are image-local and
     *                        do not need coordinate translation because only text order is used.
     * @return final yellow route name, or an empty string when local OCR finds no usable text.
     */
    private String findLastYellowRouteDestination(String yellowImagePath) {
        List<OcrWordResult> words = ocr.getAllTextResultsLocalOnly(yellowImagePath);
        OcrWordResult last = null;
        for (OcrWordResult word : words) {
            if (word == null || word.getText() == null || word.getText().isBlank()) {
                continue;
            }
            if (last == null || word.getTop() + word.getHeight() > last.getTop() + last.getHeight()) {
                last = word;
            }
        }
        String value = findLastYellowRouteDestinationLine(words);
        if (value.isBlank() && last != null) {
            value = last.getText();
        }
        log.info("navigation map search: last yellow destination OCR words={} last={}",
                formatRouteOcrWords(words), value);
        return value;
    }

    private String findLastYellowRouteDestinationLine(List<OcrWordResult> words) {
        List<OcrWordResult> usable = new ArrayList<>();
        for (OcrWordResult word : words) {
            if (word == null || word.getText() == null || word.getText().isBlank()) {
                continue;
            }
            usable.add(word);
        }
        if (usable.isEmpty()) {
            return "";
        }

        OcrWordResult bottom = usable.get(0);
        for (OcrWordResult word : usable) {
            if (centerY(word) > centerY(bottom)) {
                bottom = word;
            }
        }

        int bottomCenterY = centerY(bottom);
        int rowTolerance = Math.max(8, bottom.getHeight());
        List<OcrWordResult> bottomLine = new ArrayList<>();
        for (OcrWordResult word : usable) {
            if (Math.abs(centerY(word) - bottomCenterY) <= rowTolerance) {
                bottomLine.add(word);
            }
        }
        bottomLine.sort((a, b) -> Integer.compare(a.getLeft(), b.getLeft()));

        StringBuilder builder = new StringBuilder();
        for (OcrWordResult word : bottomLine) {
            builder.append(word.getText());
        }
        return builder.toString();
    }

    private int centerY(OcrWordResult word) {
        return word.getTop() + Math.max(1, word.getHeight()) / 2;
    }

    private String formatRouteOcrWords(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "[]";
        }
        return words.stream()
                .map(word -> word == null ? "null" : word.getText() + "@("
                        + word.getLeft() + "," + word.getTop() + ","
                        + word.getWidth() + "x" + word.getHeight() + ")")
                .toList()
                .toString();
    }

    private String normalizeRouteDestinationName(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^\\u4E00-\\u9FFFA-Za-z0-9]+", "").trim();
    }

    /**
     * Build the OCR source image for the final route-coordinate click.
     *
     * <p>The world-map route result mixes bright-green clickable coordinates with white explanatory
     * text, panel texture, scrollbars, and borders. For this specific navigation step we care only
     * about the green coordinate/link text, so the raw capture is normalized into a same-sized
     * black/white image before OCR. Keeping the same dimensions preserves the OCR point coordinate
     * space: the returned point is still local to {@code rawImagePath}'s map-result rectangle.</p>
     *
     * @param rawImagePath filesystem path to the freshly captured route-result image. The image is
     *                     window-scoped and owned by the current navigation attempt.
     * @return filesystem path passed to OCR. Normally this is {@code rawImagePath + "_green.png"};
     * when preprocessing fails or cannot write the debug image, the raw path is returned so
     * navigation can still use the old OCR behavior.
     */
    private String preprocessMapRouteResultForCoordinateOcr(String rawImagePath) {
        Path rawPath = Path.of(rawImagePath);
        String rawFileName = rawPath.getFileName().toString();
        String greenFileName = rawFileName.endsWith(".png")
                ? rawFileName.substring(0, rawFileName.length() - ".png".length()) + "_green.png"
                : rawFileName + "_green.png";
        Path greenPath = rawPath.resolveSibling(greenFileName);
        try {
            ImagePreprocessor.washGreenTextToBlackAndWhite(rawImagePath, greenPath.toString());
            if (Files.exists(greenPath)) {
                log.info("navigation map search: route OCR green preprocessing raw={} green={}",
                        rawImagePath, greenPath);
                return greenPath.toString();
            }
            log.warn("navigation map search: route OCR green preprocessing produced no file raw={}", rawImagePath);
        } catch (Exception e) {
            log.warn("navigation map search: route OCR green preprocessing failed raw={} reason={}",
                    rawImagePath, e.getMessage(), e);
        }
        return rawImagePath;
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
        ensureTaskTurn(description);
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
            checkpointTask();
            if (Thread.currentThread().isInterrupted()) {
                return MiniMapPathingAttemptResult.INCONCLUSIVE;
            }

            GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
            checkpointTask();
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
            checkpointTask();
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

    /**
     * Release the task-level turn after the game has accepted a pathing command.
     *
     * <p>Once the character is auto-moving, this window usually does not need to keep exclusive
     * business ownership. Releasing here lets follower/other windows run safe background checks until
     * this navigation flow later calls {@link #ensureTaskTurn(String)} before the next focused action.</p>
     *
     * @param source short diagnostic label for the navigation action that started pathing.
     */
    private void releaseTaskTurnAfterPathing(String source) {
        log.info("navigation pathing started, release task turn: source={}", source);
        taskTurnCoordinator.forceRelease("navigation:pathing-started:" + source);
    }

    /**
     * Acquire the task-level turn before navigation performs a focused or state-mutating action.
     *
     * <p>This is different from the physical input queue. The input queue serializes one
     * mouse/keyboard sequence, while the task turn decides which window is allowed to continue a
     * larger business chain such as opening the world map, handling a route dialog, re-clicking a
     * route result, or cleaning UI after arrival. Navigation may release the turn after pathing
     * starts so other windows can do safe maintenance; any later focused action must call this method
     * to re-enter the turn.</p>
     *
     * @param source short diagnostic suffix describing the navigation stage requesting the turn;
     *               it is appended to the `navigation:` transaction name in logs. Must be non-null
     *               enough for debugging, but it does not affect routing behavior.
     */
    private void ensureTaskTurn(String source) {
        taskTurnCoordinator.enter("navigation:" + source);
    }

    private void checkpointTask() {
        taskExecutionContextHolder.checkpointIfPresent();
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
