package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogPreparationRequest;
import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogPreparationStatus;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.navigation.TemplateLocationInfo;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.model.npc.NpcTooltipType;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.vision.GameTextLineOcrService;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
    private static final String WORLD_MAP_TITLE_TEMPLATE_PATH = "images/template/map/world_map_title.png";
    private static final String MINI_MAP_PANEL_CHECKED_TEMPLATE = "images/template/map/checkbox_checked.png";
    private static final String MINI_MAP_PANEL_UNCHECKED_TEMPLATE = "images/template/map/checkbox_unchecked.png";
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
    private static final int MAP_ROUTE_TARGET_INPUT_X = 252;
    private static final int MAP_ROUTE_TARGET_INPUT_Y = 194;
    private static final int MAP_ROUTE_SEARCH_BUTTON_X = 397;
    private static final int MAP_ROUTE_SEARCH_BUTTON_Y = 194;
    private static final int MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS = 3;
    private static final int MAP_RESULT_SCROLL_DOWN_UNITS = 6;
    private static final long MAP_RESULT_SCROLL_INTERVAL_MS = 80L;
    private static final long MAP_RESULT_SCROLL_SETTLE_MS = 300L;
    private static final long WORLD_MAP_SEARCH_TYPE_SETTLE_MS = 500L;
    private static final int ROUTE_CLOSE_RANDOM_MOUSE_MIN_X = 120;
    private static final int ROUTE_CLOSE_RANDOM_MOUSE_MAX_X = 900;
    private static final int ROUTE_CLOSE_RANDOM_MOUSE_MIN_Y = 130;
    private static final int ROUTE_CLOSE_RANDOM_MOUSE_MAX_Y = 620;
    private static final Path ROUTE_FAILURE_CASE_DIR = Path.of("images", "failure-cases", "world-map-route");
    private static final DateTimeFormatter FAILURE_CASE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final long MOVING_NAVIGATION_YIELD_MS = 1500L;
    private static final long RECENT_PATHING_SNAPSHOT_MAX_AGE_MS = 1500L;
    private static final long ROUTE_DIALOG_PREPARE_REQUEST_TTL_MS = 120_000L;
    private static final long ROUTE_DIALOG_PREPARED_CLICK_MAX_AGE_MS = 2_500L;
    private static final long ROUTE_DIALOG_PREPARED_WAIT_MS = 200L;
    private static final long ROUTE_DIALOG_PREPARED_WAIT_POLL_MS = 50L;
    private static final long ROUTE_DIALOG_REQUESTED_YIELD_MAX_MS = 3_000L;
    private static final long ROUTE_DIALOG_PREPARING_YIELD_MAX_MS = 30_000L;
    private static final long ROUTE_DIALOG_FAILED_BACKGROUND_RETRY_MS = 3_000L;
    private static final long ROUTE_DIALOG_VISIBLE_RESCUE_SNAPSHOT_MAX_AGE_MS = 120_000L;
    private static final long MINI_MAP_OPEN_SETTLE_MS = 500L;
    private static final long MINI_MAP_CLICK_SETTLE_MS = 250L;
    private static final long ROUTE_DIALOG_SETTLE_MS = 500L;
    private static final long ROUTE_DIALOG_ARRIVAL_CONFIRM_TIMEOUT_MS = 2500L;
    private static final long ROUTE_DIALOG_ARRIVAL_CONFIRM_POLL_MS = 500L;
    private static final long MINI_MAP_PATHING_CONFIRM_TIMEOUT_MS = 1500L;
    private static final long MINI_MAP_PATHING_CONFIRM_POLL_MS = 250L;
    private static final long MINI_MAP_PATHING_COORD_CONFIRM_TIMEOUT_MS = 1000L;
    private static final long MINI_MAP_PATHING_COORD_CONFIRM_POLL_MS = 200L;
    private static final int MAP_NAVIGATION_RECLICK_STUCK_SCANS = 2;
    private static final int MAP_NAVIGATION_REOPEN_STUCK_SCANS = 3;
    private static final String MAP_LING_SHOU_VILLAGE = "灵兽村";
    private static final String MAP_CHANG_AN = "长安";
    private static final String NPC_ZHANG_WEN = "张闻";
    private static final int ZHANG_WEN_APPROACH_X = 219;
    private static final int ZHANG_WEN_APPROACH_Y = 100;
    private static final int ZHANG_WEN_NPC_X = 224;
    private static final int ZHANG_WEN_NPC_Y = 100;
    private static final NpcTarget ZHANG_WEN_NPC = NpcTarget.builder()
            .key("navigation.zhangWen")
            .mapName(MAP_CHANG_AN)
            .name(NPC_ZHANG_WEN)
            .x(ZHANG_WEN_NPC_X)
            .y(ZHANG_WEN_NPC_Y)
            .role(NpcRole.INTERACTION_TARGET)
            .movementType(NpcMovementType.FIXED)
            .tooltipType(NpcTooltipType.NONE)
            .source("navigation")
            .build();
    private static final long LING_SHOU_ROUTE_CONFIRM_TIMEOUT_MS = 20000L;

    private final BotProperties config;
    private final GameContext context;
    private final GameClientTracker tracker;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final GameTextLineOcrService gameTextLineOcrService;
    private final MiniMapCoordinateReader miniMapCoordinateReader;
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
    private final TransferChoiceMemoryService transferChoiceMemoryService;

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

            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
            long stageStartedAt = System.currentTimeMillis();
            /*
             * Fresh confirmation is mandatory before map navigation can report ARRIVED. A stale
             * cached map is especially dangerous for navigateToNPC(): it would make the next step
             * click the NPC coordinate on whatever map is actually open.
             */
            Boolean snapshotMapCheck = confirmCurrentMapFromRecentPathingSnapshot(
                    targetMapName, "navigateToMap:staleCacheGuard");
            boolean arrivedAfterFreshCheck;
            if (snapshotMapCheck != null) {
                arrivedAfterFreshCheck = snapshotMapCheck;
            } else {
                arrivedAfterFreshCheck = gameStateUtil.confirmCurrentMapFresh(
                        targetMapName, 0L, "navigateToMap:staleCacheGuard");
            }
            log.info("navigate to map stale-cache guard: target={} current={} arrived={}",
                    targetMapName, me.getCurrentMapName(), arrivedAfterFreshCheck);
            log.info("[productionNavigate-latency] stage=map-confirm elapsedMs={} totalMs={} target={} source={} arrived={}",
                    Math.max(0L, System.currentTimeMillis() - stageStartedAt),
                    LatencyMetrics.elapsedMs(latencyStart),
                    targetMapName, source, arrivedAfterFreshCheck);
            if (arrivedAfterFreshCheck) {
                if (request.isReturnOnPathingStarted()) {
                    WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
                    DialogPreparationStatus status = runtime == null ? null : runtime.getDialogPreparationStatus();
                    PreparedDialogAction action = runtime == null ? null : runtime.getPreparedDialogAction();
                    if ((status != null && status.matches(DialogOperation.ROUTE_TRANSFER, targetMapName))
                            || (action != null && action.matches(DialogOperation.ROUTE_TRANSFER, targetMapName))) {
                        runtime.clearDialogPreparationRequest("target map confirmed before route dialog click");
                    }
                }
                result = NavigationResult.arrived("target map confirmed by stale-cache guard");
                return result;
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

            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
            stageStartedAt = System.currentTimeMillis();
            WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
            if (request.isReturnOnPathingStarted() && runtime != null) {
                DialogPreparationStatus status = runtime.getDialogPreparationStatus();
                PreparedDialogAction action = runtime.getPreparedDialogAction();
                boolean staleRoutePreparation =
                        (status != null
                                && status.getOperation() == DialogOperation.ROUTE_TRANSFER
                                && status.getTargetKeyword() != null
                                && !targetMapName.equals(status.getTargetKeyword()))
                                || (action != null
                                && action.getOperation() == DialogOperation.ROUTE_TRANSFER
                                && action.getTargetKeyword() != null
                                && !targetMapName.equals(action.getTargetKeyword()));
                if (staleRoutePreparation) {
                    log.info("clear stale route dialog preparation before map navigation: source={} target={} statusTarget={} preparedTarget={}",
                            source, targetMapName,
                            status == null ? null : status.getTargetKeyword(),
                            action == null ? null : action.getTargetKeyword());
                    runtime.clearDialogPreparationRequest("route dialog target changed before map navigation");
                }
            }
            if (request.isReturnOnPathingStarted()) {
                logRouteDialogPreparationSnapshot(runtime, targetMapName, source);
            }
            if (request.isReturnOnPathingStarted()
                    && hasMatchingRouteDialogPreparation(runtime, targetMapName)) {
                /*
                 * A previous world-map route click may have opened a transfer dialog while this
                 * window yielded its turn. Consume that prepared/current dialog before opening the
                 * world map again, otherwise route handoff turns into a duplicate search loop.
                 */
                log.info("consume prepared route dialog before world-map search: target={} source={}",
                        targetMapName, source);
                RouteDialogClickResult routeDialog = clickRouteDialogOption(
                        "navigateToMap:prepared-route-dialog", targetMapName, false, true);
                if (routeDialog.result() == DialogResultStatus.OPTION_KEYWORD_CLICKED) {
                    result = NavigationResult.pathingStarted("route dialog clicked; observer will confirm pathing");
                    return result;
                }
                if (routeDialog.result() == DialogResultStatus.NO_DIALOG
                        && targetMapName.equals(routeDialog.fromMap())) {
                    result = NavigationResult.arrived("route dialog skipped because target map is already current");
                    return result;
                }
                if (routeDialog.result() == DialogResultStatus.DIALOG_PREPARING) {
                    result = NavigationResult.dialogPreparing("route dialog preparation in progress");
                    return result;
                }
                /*
                 * A matching route-dialog preparation means the previous route click may already
                 * have opened a transfer dialog. A foreground NO_DIALOG read is not proof that the
                 * dialog is gone; HWND capture/OCR can miss one pass under multi-window load. Yield
                 * once instead of immediately opening the world map again and closing/covering the
                 * dialog the watcher is supposed to consume.
                 */
                log.info("prepared route dialog not usable yet; yield before world-map retry: target={} result={}",
                        targetMapName, routeDialog.result());
                result = NavigationResult.dialogPreparing("route dialog not usable yet; yield before world-map retry");
                return result;
            }
            if (request.isReturnOnPathingStarted()
                    && shouldTryVisibleRouteDialogRescue(runtime, targetMapName)) {
                /*
                 * If the watcher has already proven the previous map-route handoff stopped away
                 * from the target, the transfer dialog may still be visible even after its
                 * preparation request was cleared/expired. Try the foreground route-dialog handler
                 * once before opening the world map again. This rescue deliberately disables a new
                 * background request so a no-dialog screen cannot recreate the old empty-prep loop.
                 */
                log.info("try visible route dialog rescue before world-map search: target={} source={}",
                        targetMapName, source);
                RouteDialogClickResult routeDialog = clickRouteDialogOption(
                        "navigateToMap:visible-route-dialog-rescue", targetMapName, false, false);
                if (routeDialog.result() == DialogResultStatus.OPTION_KEYWORD_CLICKED) {
                    result = NavigationResult.pathingStarted("visible route dialog clicked; observer will confirm pathing");
                    return result;
                }
                if (routeDialog.result() == DialogResultStatus.NO_DIALOG
                        && targetMapName.equals(routeDialog.fromMap())) {
                    result = NavigationResult.arrived("visible route dialog rescue skipped because target map is already current");
                    return result;
                }
                if (routeDialog.result() == DialogResultStatus.DIALOG_PREPARING) {
                    result = NavigationResult.dialogPreparing("visible route dialog preparation in progress");
                    return result;
                }
                log.info("visible route dialog rescue not usable, continue world-map search: target={} result={}",
                        targetMapName, routeDialog.result());
            }
            log.info("[productionNavigate-latency] stage=route-dialog-precheck elapsedMs={} totalMs={} target={} source={} hasPreparation={}",
                    Math.max(0L, System.currentTimeMillis() - stageStartedAt),
                    LatencyMetrics.elapsedMs(latencyStart),
                    targetMapName, source, hasMatchingRouteDialogPreparation(runtime, targetMapName));

            boolean routeProgressSubmitted = false;

            /*
             * First route submission: open the world map, search the target map, scroll to the bottom
             * result, and click the last route link. The called method owns the exclusive input section.
             */
            stageStartedAt = System.currentTimeMillis();
            if (!submitWorldMapSearchAndClickDestination(targetMapName)) {
                log.info("[productionNavigate-latency] stage=world-map-submit elapsedMs={} totalMs={} target={} source={} result=failed",
                        Math.max(0L, System.currentTimeMillis() - stageStartedAt),
                        LatencyMetrics.elapsedMs(latencyStart),
                        targetMapName, source);
                log.warn("first navigate attempt failed, entering retry loop");
                if (request.isReturnOnPathingStarted()) {
                    /*
                     * Phase tasks call map navigation while holding the task turn. If the first
                     * route submission cannot even click a route, do not monopolize the turn for
                     * the long map-arrival loop; return a retryable navigation result quickly so
                     * the task layer can clean/retry without starving other windows.
                     */
                    result = NavigationResult.mapNotReached("map route submit failed");
                    return result;
                }
            } else {
                log.info("[productionNavigate-latency] stage=world-map-submit elapsedMs={} totalMs={} target={} source={} result=clicked",
                        Math.max(0L, System.currentTimeMillis() - stageStartedAt),
                        LatencyMetrics.elapsedMs(latencyStart),
                        targetMapName, source);
                routeProgressSubmitted = true;
                requestRouteDialogPreparationAfterMapRouteClick(
                        request, targetMapName, "navigateToMap:map-route-clicked");
                if (request.isReturnOnPathingStarted()) {
                    /*
                     * The route link was clicked successfully. Do not let the short edge-pixel probe
                     * veto this handoff: world-map route startup can lag behind the click while the
                     * map closes or an intermediate transfer begins. Register the route intent and let
                     * the window pathing observer decide whether the window actually moves, arrives,
                     * or stalls and needs a retry.
                     */
                    result = NavigationResult.pathingStarted("map route submitted; observer will confirm pathing");
                    return result;
                }
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
                stageStartedAt = System.currentTimeMillis();
                GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
                long movementProbeElapsedMs = Math.max(0L, System.currentTimeMillis() - stageStartedAt);
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                if (isActiveNavigationMovement(movementState)) {
                    stuckCount = 0;
                    if (request.isReturnOnPathingStarted()) {
                        /*
                         * Visible movement is only proof of this map route when the current
                         * navigateToMap call actually submitted a route option/search. Otherwise a
                         * previous phase such as Ling Shou Village exit pre-pathing can be mistaken
                         * for target-map navigation and release the task turn too early.
                         */
                        if (routeProgressSubmitted) {
                            result = NavigationResult.pathingStarted("map route pathing started");
                            return result;
                        }
                        log.info("navigate to map sees movement without current route submit proof; keep turn: target={} state={} sleepMs={}",
                                targetMapName, movementState, MOVING_NAVIGATION_YIELD_MS);
                    }
                    log.info("navigate to map waiting while moving: target={} state={} routeSubmitProof={} sleepMs={}",
                            targetMapName, movementState, routeProgressSubmitted, MOVING_NAVIGATION_YIELD_MS);
                    if (!TaskSleep.sleep(MOVING_NAVIGATION_YIELD_MS)) {
                        result = NavigationResult.stopped("interrupted while waiting for map pathing");
                        return result;
                    }
                    TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                    continue;
                }

                /*
                 * OCR/template location confirmation is the authoritative map-arrival check. It is
                 * intentionally not preceded by a speculative route-dialog probe here. The only
                 * generic route-dialog resume check lives at the start of navigateToMap; stopped
                 * movement in the main loop should first prove the current location instead of
                 * repeatedly preparing dialog OCR against empty scenery.
                 */
                stageStartedAt = System.currentTimeMillis();
                LocationInfo locationInfo = playerStateService.syncMyPosition();
                log.info("[productionNavigate-latency] stage=loop-position-sync elapsedMs={} totalMs={} target={} source={} movementProbeMs={} movementState={} location={}",
                        Math.max(0L, System.currentTimeMillis() - stageStartedAt),
                        LatencyMetrics.elapsedMs(latencyStart),
                        targetMapName, source, movementProbeElapsedMs, movementState,
                        locationInfo == null ? "null" : locationInfo.mapName + "(" + locationInfo.x + "," + locationInfo.y + ")");
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
                        routeProgressSubmitted = true;
                        stuckCount = 0;
                    }
                } else {
                    if (retryWorldMapDestinationClick(targetMapName)) {
                        routeProgressSubmitted = true;
                    }
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
            if (result.getStatus() == NavigationResultStatus.PATHING_STARTED) {
                registerWindowPathingIntent(request, "navigateToMap", result.getMessage(), false);
            }
            LatencyMetrics.info(log, "navigation.toMap", latencyStart,
                    "result=" + result.getStatus() + " source=" + source + " target=" + targetMapName
                            + " returnOnPathing=" + request.isReturnOnPathingStarted());
        }
    }

    private Boolean confirmCurrentMapFromRecentPathingSnapshot(String targetMapName, String source) {
        WindowRuntimeContext windowContext = windowTaskContextHolder.rawCurrent().orElse(null);
        if (windowContext == null) {
            return null;
        }
        WindowPathingSnapshot snapshot = windowContext.getPathingSnapshot();
        if (snapshot == null
                || snapshot.getState() == WindowPathingState.NONE
                || snapshot.getState() == WindowPathingState.UNKNOWN
                || snapshot.getCurrentMapName() == null
                || snapshot.getCurrentMapName().isBlank()) {
            return null;
        }
        long ageMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
        if (ageMs > RECENT_PATHING_SNAPSHOT_MAX_AGE_MS) {
            return null;
        }
        boolean arrived = gameStateUtil.isSameMapName(snapshot.getCurrentMapName(), targetMapName);
        log.info("navigate to map uses recent pathing snapshot: source={} target={} current={} state={} ageMs={} arrived={}",
                source, targetMapName, snapshot.getCurrentMapName(), snapshot.getState(), ageMs, arrived);
        return arrived;
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
        int arrivalTolerance = navigationArrivalTolerance(request);
        long latencyStart = LatencyMetrics.start();
        NavigationResult result = NavigationResult.pointNotReached("not started");
        boolean skipFinishClose = false;
        try {
            String mapName = context.getMe().getCurrentMapName();
            log.info("navigate in map: {} target=({}, {})", mapName, targetX, targetY);

            if (request.isExactMiniMapClickOnly()) {
                String clickMapName = mapName == null || mapName.isBlank()
                        ? request.getTargetMapName()
                        : mapName;
                Point pixelPoint = coordinateHelper.getPhysicalMapPoint(clickMapName, targetX, targetY);
                if (pixelPoint == null) {
                    result = NavigationResult.pointNotReached("exact mini-map coordinate has no transform");
                    return result;
                }
                String source = request.getSource()
                        + ":exactMiniMapClick"
                        + ":map=" + clickMapName
                        + ":logical=(" + targetX + "," + targetY + ")"
                        + ":pixel=(" + pixelPoint.x + "," + pixelPoint.y + ")";
                log.info("navigate in current map exact mini-map click: source={} noJitter=true noFallback=true",
                        source);
                TemplateLocationInfo baselineLocation = miniMapCoordinateReader.readCurrentTemplateLocation().orElse(null);
                MapCoordinate baseline = baselineLocation == null
                        ? currentKnownCoordinate()
                        : baselineLocation.coordinate();
                if (!submitMiniMapClick(pixelPoint, source, false, false)) {
                    result = NavigationResult.pointNotReached("exact mini-map click input failed");
                    return result;
                }
                if (request.isReturnOnPathingStarted()) {
                    /*
                     * Handoff callers still need movement proof, but use the fast two-edge frame
                     * diff instead of coordinate/OCR polling.
                     */
                    if (!gameStateUtil.confirmPathingStartedByEdgePixelDiff(source)) {
                        result = NavigationResult.pointNotReached("exact mini-map click did not start pathing");
                        return result;
                    }
                    gameStateUtil.recordMovementIntent(source);
                    result = NavigationResult.pathingStarted("exact mini-map coordinate clicked");
                    return result;
                }
                MiniMapPathingAttemptResult confirmResult = confirmMiniMapPathingStarted(
                        source, baseline, clickMapName, baselineLocation);
                if (confirmResult != MiniMapPathingAttemptResult.PATHING_STARTED) {
                    log.info("exact mini-map click did not start pathing: source={} pixel=({}, {}) result={}",
                            source, pixelPoint.x, pixelPoint.y, confirmResult);
                    result = NavigationResult.pointNotReached("exact mini-map click did not start pathing");
                    return result;
                }
                gameStateUtil.recordMovementIntent(source);
                result = NavigationResult.arrived("exact mini-map coordinate clicked");
                return result;
            }

            long startTime = System.currentTimeMillis();
            long timeoutMs = 60000;
            int failedMiniMapClicks = 0;
            Set<String> attemptedMiniMapLogicalPoints = new HashSet<>();

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

                if (battleRadarService.checkAndSyncCombatState()) {
                    log.warn("navigate in current map interrupted by battle: target=({}, {})", targetX, targetY);
                    result = NavigationResult.interrupted("interrupted by battle");
                    return result;
                }

                context.setCurrentActionState(GameContext.ActionState.NAVIGATING);

                if (isCurrentCachedCoordinateNear(targetX, targetY, arrivalTolerance, "navigateInCurrentMap:cached")) {
                    result = NavigationResult.arrived("target coordinate reached by cached state");
                    skipFinishClose = request.isReturnOnPathingStarted()
                            && request.getSource() != null
                            && request.getSource().startsWith("debug-nav-stress");
                    return result;
                }

                if (!request.isReturnOnPathingStarted()
                        && syncAndCheckArrived(targetX, targetY, arrivalTolerance, "navigateInCurrentMap:loop")) {
                    result = NavigationResult.arrived("target coordinate reached");
                    return result;
                }

                if (!request.isReturnOnPathingStarted()) {
                    GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
                    TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                    if (isConfirmedNavigationMovement(movementState)) {
                        log.info("navigate in current map yielding while moving: target=({}, {}) state={} sleepMs={}",
                                targetX, targetY, movementState, MOVING_NAVIGATION_YIELD_MS);
                        if (!TaskSleep.sleep(MOVING_NAVIGATION_YIELD_MS)) {
                            result = NavigationResult.stopped("interrupted while waiting for map pathing");
                            return result;
                        }
                        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                        continue;
                    }
                    if (movementState == GameStateUtil.MovementState.MAYBE_MOVING) {
                        /*
                         * Before this current-map call submits a mini-map click, MAYBE_MOVING is only a
                         * weak pixel/low-sample hint. Let coordinate arrival and the click candidates
                         * proceed instead of returning PATHING_STARTED forever on animated scenery.
                         */
                        log.info("navigate in current map ignores weak movement before click: target=({}, {}) state={}",
                                targetX, targetY, movementState);
                    }
                } else {
                    /*
                     * The caller only needs this navigation to submit a mini-map click and yield once
                     * pathing starts. A full movement-state probe can spend several seconds proving
                     * the character is stationary before the first click, so skip it here and let the
                     * click confirmation below use a short coordinate-change probe instead.
                     */
                    log.info("navigate in current map skips heavy pre-click movement probe: target=({}, {}) source={}",
                            targetX, targetY, request.getSource());
                }

                CoordinateHelper.MiniMapClickPoint clickPoint = null;
                while (clickPoint == null && System.currentTimeMillis() - startTime < timeoutMs) {
                    CoordinateHelper.MiniMapClickPoint candidate = coordinateHelper.resolveMiniMapClickPoint(
                            mapName, targetX, targetY, failedMiniMapClicks);
                    if (candidate == null) {
                        break;
                    }
                    String logicalKey = candidate.logicalX() + "," + candidate.logicalY();
                    if (attemptedMiniMapLogicalPoints.add(logicalKey)) {
                        clickPoint = candidate;
                    } else {
                        log.info("navigate in current map skip duplicate mini-map logical point: target=({}, {}) logical=({}, {}) failedClicks={} reason={}",
                                targetX, targetY, candidate.logicalX(), candidate.logicalY(), failedMiniMapClicks,
                                candidate.reason());
                        failedMiniMapClicks++;
                    }
                }
                if (clickPoint == null) {
                    log.warn("navigate in current map exhausted mini-map click points: target=({}, {}) failedClicks={}",
                            targetX, targetY, failedMiniMapClicks);
                    result = NavigationResult.pointNotReached("exhausted mini-map click points");
                    return result;
                }

                boolean checkPanelBeforeOpen = failedMiniMapClicks > 0;
                MiniMapPathingAttemptResult attemptResult = request.isReturnOnPathingStarted()
                        ? clickMiniMapPointForHandoff(clickPoint, "navigateInCurrentMap:click", mapName, checkPanelBeforeOpen)
                        : clickMiniMapPointAndConfirm(clickPoint, "navigateInCurrentMap:click", false, checkPanelBeforeOpen);
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                if (attemptResult == MiniMapPathingAttemptResult.PATHING_STARTED) {
                    log.info("navigate in current map mini-map click started pathing: target=({}, {}) logicalClick=({}, {}) basePixel=({}, {}) actualPixel=({}, {}) jitter=({}, {}) reason={}",
                            targetX, targetY,
                            clickPoint.logicalX(), clickPoint.logicalY(),
                            clickPoint.basePixelPoint().x, clickPoint.basePixelPoint().y,
                            clickPoint.pixelPoint().x, clickPoint.pixelPoint().y,
                            clickPoint.jitterX(), clickPoint.jitterY(),
                            clickPoint.reason());
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
                    if (request.isReturnOnPathingStarted()) {
                        /*
                         * Yield-mode callers are coordinating multiple windows. If one mini-map
                         * click does not confirm movement, do not run the whole fallback click batch
                         * while holding the task turn; let the task retry on a later turn instead.
                         * Keep the mini-map open so that retry can reuse it instead of paying another
                         * Alt+1 open/close cycle.
                         */
                        skipFinishClose = true;
                        if (request.isPublishWindowPathingIntent()) {
                            /*
                             * The background observer is the source of truth for the navigation
                             * stress task after handoff. A coordinate can remain unchanged during
                             * the short post-click probe even though the game accepted the click and
                             * starts moving a moment later, so publish the intent and let the
                             * observer classify ACTIVE / ARRIVED / STOPPED_AWAY instead of failing
                             * this turn on a one-second local sample.
                             */
                            log.info("navigate in current map mini-map click submitted without immediate coordinate delta; "
                                            + "handoff to window observer: target=({}, {}) source={} pixel=({}, {}) result={}",
                                    targetX, targetY, request.getSource(),
                                    clickPoint.pixelPoint().x, clickPoint.pixelPoint().y,
                                    attemptResult);
                            result = NavigationResult.pathingStarted(
                                    "current-map mini-map click submitted; observer will confirm pathing");
                            return result;
                        }
                        log.info("navigate in current map mini-map click did not start pathing; yield before fallback batch: target=({}, {}) source={} pixel=({}, {})",
                                targetX, targetY, request.getSource(),
                                clickPoint.pixelPoint().x, clickPoint.pixelPoint().y);
                        result = NavigationResult.pointNotReached(
                                "current-map mini-map click did not start pathing before yield");
                        return result;
                    }
                    failedMiniMapClicks++;
                } else {
                    result = NavigationResult.pointNotReached("mini-map click failed");
                    return result;
                }

                if (!TaskSleep.sleep(200)) {
                    result = NavigationResult.stopped("interrupted while waiting before current-map retry");
                    return result;
                }
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
            }

            log.error("navigate timeout");
            result = NavigationResult.pointNotReached("current-map navigation timeout");
            return result;
        } finally {
            if (result.getStatus() == NavigationResultStatus.PATHING_STARTED) {
                registerWindowPathingIntent(request, "navigateInCurrentMap", result.getMessage(), true);
            }
            if (request.isReturnOnPathingStarted()
                    && result.getStatus() == NavigationResultStatus.PATHING_STARTED) {
                log.info("navigate in current map skips mini-map close before yield: source={} result={}",
                        request.getSource(), result.getStatus());
            } else if (skipFinishClose) {
                log.info("navigate in current map skips mini-map close after cached debug arrival: source={} result={}",
                        request.getSource(), result.getStatus());
            } else {
                closeMiniMapIfOpen("navigateInCurrentMap:finish");
            }
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

        boolean npcClicked = npcClickService.clickNpcSmart(ZHANG_WEN_NPC.toClickRequest(me));
        if (!npcClicked) {
            log.warn("Ling Shou Village route Zhang Wen click not verified, checking dialog anyway");
        }
        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

        RouteDialogClickResult routeDialog = clickRouteDialogOption(
                "navigation:ling-shou-village", MAP_LING_SHOU_VILLAGE, false, true);
        DialogResultStatus dialogResult = routeDialog.result();
        if (dialogResult != DialogResultStatus.OPTION_KEYWORD_CLICKED) {
            log.warn("Ling Shou Village route transfer option not handled: result={}", dialogResult);
            return NavigationResult.mapNotReached("Ling Shou Village transfer option not handled");
        }

        boolean arrived = gameStateUtil.confirmCurrentMapFresh(
                MAP_LING_SHOU_VILLAGE,
                LING_SHOU_ROUTE_CONFIRM_TIMEOUT_MS,
                "navigateToLingShouVillage");
        recordRouteDialogOutcome(routeDialog, arrived, "navigation:ling-shou-village");
        if (arrived) {
            closeMapSearchInputAfterRouteDialog("navigation:ling-shou-village");
        }
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

    /**
     * Click a route-transfer option with learned memory first, then OCR fallback.
     *
     * @param source short diagnostic source.
     * @param targetMapName destination map expected after this transfer dialog.
     * @param allowFallbackOptionClick whether the OCR path may click the last green option when the
     *                                 target text is not matched. Fallback clicks are never learned.
     * @return route dialog click result with enough context to update transfer memory after arrival
     * confirmation.
     */
    private RouteDialogClickResult clickRouteDialogOption(String source,
                                                          String targetMapName,
                                                          boolean allowFallbackOptionClick,
                                                          boolean allowBackgroundPreparation) {
        String fromMap = null;
        Integer fromX = null;
        Integer fromY = null;
        WindowPathingSnapshot snapshot = windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getPathingSnapshot)
                .orElse(null);
        if (snapshot != null && snapshot.getCurrentMapName() != null && !snapshot.getCurrentMapName().isBlank()) {
            long ageMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
            if (ageMs <= RECENT_PATHING_SNAPSHOT_MAX_AGE_MS) {
                fromMap = snapshot.getCurrentMapName();
                fromX = snapshot.getCurrentX();
                fromY = snapshot.getCurrentY();
                log.info("route dialog probe uses recent pathing snapshot: source={} target={} current={}({}, {}) state={} ageMs={}",
                        source, targetMapName, fromMap, fromX, fromY, snapshot.getState(), ageMs);
            }
        }
        if (fromMap == null) {
            LocationInfo before = playerStateService.syncMyPosition();
            fromMap = before != null && before.mapName != null && !before.mapName.isBlank()
                    ? before.mapName
                    : context.getMe().getCurrentMapName();
            fromX = before == null ? null : before.x;
            fromY = before == null ? null : before.y;
        }

        /*
         * A fresh pathing snapshot or sync can prove that a previously started route has already
         * reached the target map. In that case there is no route option left to click; probing a
         * stale dialog area can waste several seconds and may click unrelated route text.
         */
        if (targetMapName != null && targetMapName.equals(fromMap)) {
            log.info("route dialog probe skipped: already on target map source={} target={} coord=({}, {})",
                    source, targetMapName, fromX, fromY);
            windowTaskContextHolder.rawCurrent()
                    .ifPresent(runtime -> runtime.clearDialogPreparationRequest("already on route target map"));
            return new RouteDialogClickResult(
                    DialogResultStatus.NO_DIALOG,
                    false,
                    fromMap,
                    fromX,
                    fromY,
                    targetMapName,
                    null,
                    null,
                    null);
        }

        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        var remembered = transferChoiceMemoryService.findUsable(fromMap, targetMapName);
        if (runtime != null && targetMapName != null && !targetMapName.isBlank()
                && allowBackgroundPreparation) {
            long now = System.currentTimeMillis();
            PreparedDialogAction preparedBeforeRequest = runtime.getPreparedDialogAction();
            TransferChoiceMemoryService.TransferChoiceEntry rememberedEntry = remembered.orElse(null);
            DialogPreparationStatus existingPreparationStatus = runtime.getDialogPreparationStatus();
            boolean shouldRequestBackgroundPreparation = true;
            if (existingPreparationStatus != null
                    && existingPreparationStatus.matches(DialogOperation.ROUTE_TRANSFER, targetMapName)) {
                DialogPreparationPhase phase = existingPreparationStatus.getPhase();
                if (phase == DialogPreparationPhase.REQUESTED || phase == DialogPreparationPhase.PREPARING) {
                    shouldRequestBackgroundPreparation = false;
                    log.info("route dialog preparation reuses active request: source={} target={} phase={} requestAgeMs={}",
                            source, targetMapName, phase,
                            existingPreparationStatus.getRequestCreatedAtMs() <= 0
                                    ? null
                                    : Math.max(0L, now - existingPreparationStatus.getRequestCreatedAtMs()));
                } else if (phase == DialogPreparationPhase.FAILED) {
                    long failedAgeMs = existingPreparationStatus.getCompletedAtMs() <= 0
                            ? Long.MAX_VALUE
                            : Math.max(0L, now - existingPreparationStatus.getCompletedAtMs());
                    if (failedAgeMs <= ROUTE_DIALOG_FAILED_BACKGROUND_RETRY_MS) {
                        shouldRequestBackgroundPreparation = false;
                        log.info("route dialog preparation recently failed; keep request for watcher retry: source={} target={} failedAgeMs={} reason={}",
                                source, targetMapName, failedAgeMs, existingPreparationStatus.getFailureReason());
                    }
                }
            }
            if (shouldRequestBackgroundPreparation) {
                runtime.updateDialogPreparationRequest(DialogPreparationRequest.builder()
                        .operation(DialogOperation.ROUTE_TRANSFER)
                        .targetKeyword(targetMapName)
                        .source(source)
                        .fromMap(fromMap)
                        .rememberedRelativeX(rememberedEntry == null ? null : rememberedEntry.relativeX)
                        .rememberedRelativeY(rememberedEntry == null ? null : rememberedEntry.relativeY)
                        .rememberedOptionText(rememberedEntry == null ? null : rememberedEntry.optionText)
                        .createdAtMs(now)
                        .expiresAtMs(now + ROUTE_DIALOG_PREPARE_REQUEST_TTL_MS)
                        .build());
                log.info("route dialog preparation requested: source={} from={} coord=({}, {}) target={} memory={} memoryRel=({}, {}) preparedBefore={} preparedTarget={} preparedVerifiedAgeMs={}",
                        source, fromMap, fromX, fromY, targetMapName,
                        rememberedEntry != null,
                        rememberedEntry == null ? null : rememberedEntry.relativeX,
                        rememberedEntry == null ? null : rememberedEntry.relativeY,
                        preparedBeforeRequest != null,
                        preparedBeforeRequest == null ? null : preparedBeforeRequest.getTargetKeyword(),
                        preparedBeforeRequest == null || preparedBeforeRequest.getLastVerifiedAtMs() <= 0
                                ? null
                                : Math.max(0L, now - preparedBeforeRequest.getLastVerifiedAtMs()));
            }
            PreparedDialogAction preparedAction = waitForPreparedRouteDialogAction(
                    runtime, targetMapName, now, source);
            long preparedCheckAt = System.currentTimeMillis();
            if (preparedAction != null
                    && preparedAction.matches(DialogOperation.ROUTE_TRANSFER, targetMapName)
                    && matchesCurrentPreparedDialogBinding(runtime, preparedAction)
                    && preparedAction.verifiedWithin(preparedCheckAt, ROUTE_DIALOG_PREPARED_CLICK_MAX_AGE_MS)) {
                log.info("route dialog probe uses prepared action: source={} target={} matched={} click=({}, {}) verifiedAgeMs={}",
                        source, targetMapName, preparedAction.getMatchedText(),
                        preparedAction.getAbsoluteX(), preparedAction.getAbsoluteY(),
                        Math.max(0L, preparedCheckAt - preparedAction.getLastVerifiedAtMs()));
                boolean clicked = inputSequences.moveAndClickLeft("navigation:preparedRouteDialog:" + targetMapName,
                        preparedAction.getAbsoluteX(), preparedAction.getAbsoluteY(), 80, 150);
                if (!clicked) {
                    log.warn("route dialog prepared action click failed: source={} target={} click=({}, {})",
                            source, targetMapName, preparedAction.getAbsoluteX(), preparedAction.getAbsoluteY());
                    runtime.clearPreparedDialogAction("prepared route dialog click failed");
                    return new RouteDialogClickResult(
                            DialogResultStatus.FAILED,
                            false,
                            fromMap,
                            fromX,
                            fromY,
                            targetMapName,
                            preparedAction.getRelativeX(),
                            preparedAction.getRelativeY(),
                            preparedAction.getMatchedText());
                }
                runtime.clearDialogPreparationRequest("prepared route dialog clicked");
                runtime.clearPreparedDialogAction("prepared route dialog consumed");
                return new RouteDialogClickResult(
                        DialogResultStatus.OPTION_KEYWORD_CLICKED,
                        false,
                        fromMap,
                        fromX,
                        fromY,
                        targetMapName,
                        preparedAction.getRelativeX(),
                        preparedAction.getRelativeY(),
                        preparedAction.getMatchedText());
            }
            if (preparedAction == null) {
                log.info("route dialog prepared action unavailable; continue normal path: source={} target={}",
                        source, targetMapName);
            } else {
                log.info("route dialog prepared action not usable; continue normal path: source={} target={} preparedTarget={} sameBinding={} verifiedAgeMs={} maxAgeMs={}",
                        source, targetMapName, preparedAction.getTargetKeyword(),
                        matchesCurrentPreparedDialogBinding(runtime, preparedAction),
                        preparedAction.getLastVerifiedAtMs() <= 0
                                ? null
                                : Math.max(0L, now - preparedAction.getLastVerifiedAtMs()),
                        ROUTE_DIALOG_PREPARED_CLICK_MAX_AGE_MS);
            }
            DialogPreparationStatus preparationStatus = runtime.getDialogPreparationStatus();
            if (isMatchingRouteDialogPreparing(preparationStatus, targetMapName)) {
                long preparingAgeMs = preparationStatus.getPreparingStartedAtMs() <= 0
                        ? 0L
                        : Math.max(0L, System.currentTimeMillis() - preparationStatus.getPreparingStartedAtMs());
                if (preparingAgeMs > ROUTE_DIALOG_PREPARING_YIELD_MAX_MS) {
                    /*
                     * Background dialog preparation owns the route-transfer window, but a stuck OCR
                     * probe must not keep the task yielding forever. Clear the stale request and let
                     * the foreground path handle the currently visible dialog once.
                     */
                    log.warn("route dialog preparation exceeded foreground handoff limit; clear stale request: source={} target={} preparingAgeMs={} maxYieldMs={}",
                            source, targetMapName, preparingAgeMs, ROUTE_DIALOG_PREPARING_YIELD_MAX_MS);
                    runtime.clearDialogPreparationRequest("route dialog preparation exceeded foreground handoff limit");
                } else {
                    log.info("route dialog preparation still running; yield before foreground OCR: source={} target={} preparingAgeMs={}",
                            source, targetMapName,
                            preparingAgeMs);
                    return new RouteDialogClickResult(
                            DialogResultStatus.DIALOG_PREPARING,
                            false,
                            fromMap,
                            fromX,
                            fromY,
                            targetMapName,
                            null,
                            null,
                            null);
                }
            }
            if (preparationStatus != null
                    && preparationStatus.getPhase() == DialogPreparationPhase.REQUESTED
                    && preparationStatus.matches(DialogOperation.ROUTE_TRANSFER, targetMapName)) {
                long requestAgeMs = preparationStatus.getRequestCreatedAtMs() <= 0
                        ? 0L
                        : Math.max(0L, System.currentTimeMillis() - preparationStatus.getRequestCreatedAtMs());
                /*
                 * The watcher only sees dialog-preparation requests on its next polling tick. If
                 * the task immediately takes the foreground path, multi-window runs lose the idle
                 * time that should have been used for background OCR. Yield briefly once or twice
                 * while the request is still young, then fall back to foreground handling if the
                 * watcher never starts.
                 */
                if (requestAgeMs <= ROUTE_DIALOG_REQUESTED_YIELD_MAX_MS) {
                    log.info("route dialog preparation requested; yield for watcher start: source={} target={} requestAgeMs={} maxYieldMs={}",
                            source, targetMapName, requestAgeMs, ROUTE_DIALOG_REQUESTED_YIELD_MAX_MS);
                    return new RouteDialogClickResult(
                            DialogResultStatus.DIALOG_PREPARING,
                            false,
                            fromMap,
                            fromX,
                            fromY,
                            targetMapName,
                            null,
                            null,
                            null);
                }
                log.warn("route dialog preparation request did not start in time; clear stale request before foreground handling: source={} target={} requestAgeMs={} maxYieldMs={}",
                        source, targetMapName, requestAgeMs, ROUTE_DIALOG_REQUESTED_YIELD_MAX_MS);
                runtime.clearDialogPreparationRequest("route dialog request did not start before foreground fallback");
            }
        }

        /*
         * Transfer memory is scoped to an active navigation transaction. It is safe to try before OCR
         * because the caller is already expecting a route option dialog for targetMapName.
         */
        if (remembered.isPresent()) {
            if (runtime != null) {
                PreparedDialogAction preparedAction = runtime.getPreparedDialogAction();
                long now = System.currentTimeMillis();
                if (isPreparedRouteDialogActionUsable(runtime, preparedAction, targetMapName, now)) {
                    log.info("route dialog memory path uses late prepared action: source={} target={} matched={} click=({}, {}) verifiedAgeMs={}",
                            source, targetMapName, preparedAction.getMatchedText(),
                            preparedAction.getAbsoluteX(), preparedAction.getAbsoluteY(),
                            Math.max(0L, now - preparedAction.getLastVerifiedAtMs()));
                    boolean clicked = inputSequences.moveAndClickLeft("navigation:preparedRouteDialog:late:" + targetMapName,
                            preparedAction.getAbsoluteX(), preparedAction.getAbsoluteY(), 80, 150);
                    if (!clicked) {
                        log.warn("route dialog late prepared action click failed: source={} target={} click=({}, {})",
                                source, targetMapName, preparedAction.getAbsoluteX(), preparedAction.getAbsoluteY());
                        runtime.clearPreparedDialogAction("late prepared route dialog click failed");
                        return new RouteDialogClickResult(
                                DialogResultStatus.FAILED,
                                false,
                                fromMap,
                                fromX,
                                fromY,
                                targetMapName,
                                preparedAction.getRelativeX(),
                                preparedAction.getRelativeY(),
                                preparedAction.getMatchedText());
                    }
                    runtime.clearDialogPreparationRequest("late prepared route dialog clicked");
                    runtime.clearPreparedDialogAction("late prepared route dialog consumed");
                    return new RouteDialogClickResult(
                            DialogResultStatus.OPTION_KEYWORD_CLICKED,
                            false,
                            fromMap,
                            fromX,
                            fromY,
                            targetMapName,
                            preparedAction.getRelativeX(),
                            preparedAction.getRelativeY(),
                            preparedAction.getMatchedText());
                }
            }
            TransferChoiceMemoryService.TransferChoiceEntry entry = remembered.get();
            DialogResult rememberedResult = dialogService.handleDialog(DialogHandleRequest.handleRememberedRouteOption(
                    source + ":memory", entry.relativeX, entry.relativeY, targetMapName));
            if (rememberedResult.getStatus() == DialogResultStatus.OPTION_KEYWORD_CLICKED) {
                log.info("[transfer-memory] clicked remembered route option: source={} from={} target={} rel=({}, {})",
                        source, fromMap, targetMapName, entry.relativeX, entry.relativeY);
                if (runtime != null) {
                    runtime.clearDialogPreparationRequest("remembered route dialog clicked");
                    runtime.clearPreparedDialogAction("remembered route dialog consumed");
                }
                return new RouteDialogClickResult(
                        rememberedResult.getStatus(),
                        true,
                        fromMap,
                        fromX,
                        fromY,
                        targetMapName,
                        entry.relativeX,
                        entry.relativeY,
                        entry.optionText);
            }
        }

        DialogResult ocrResult = dialogService.handleDialog(DialogHandleRequest.handleRouteKeywordOption(
                source, targetMapName, allowFallbackOptionClick));
        if (runtime != null && (ocrResult.getStatus() == DialogResultStatus.OPTION_KEYWORD_CLICKED
                || ocrResult.getStatus() == DialogResultStatus.FALLBACK_CLICKED)) {
            runtime.clearDialogPreparationRequest("route dialog handled by normal path");
        }
        return new RouteDialogClickResult(
                ocrResult.getStatus(),
                false,
                fromMap,
                fromX,
                fromY,
                targetMapName,
                ocrResult.getRelativeX(),
                ocrResult.getRelativeY(),
                ocrResult.getMatchedText());
    }

    private boolean matchesCurrentPreparedDialogBinding(WindowRuntimeContext runtime, PreparedDialogAction action) {
        if (runtime == null || action == null) {
            return false;
        }
        if (action.getWindowId() != null && !action.getWindowId().equals(runtime.getWindowId())) {
            return false;
        }
        String currentHwnd = runtime.getNativeBinding().getNativeHandle();
        return action.getHwnd() == null || action.getHwnd().equals(currentHwnd);
    }

    private PreparedDialogAction waitForPreparedRouteDialogAction(WindowRuntimeContext runtime,
                                                                  String targetMapName,
                                                                  long requestedAtMs,
                                                                  String source) {
        long deadline = requestedAtMs + ROUTE_DIALOG_PREPARED_WAIT_MS;
        PreparedDialogAction action = runtime.getPreparedDialogAction();
        while (!isPreparedRouteDialogActionUsable(runtime, action, targetMapName, System.currentTimeMillis())
                && System.currentTimeMillis() < deadline) {
            long sleepMs = Math.min(ROUTE_DIALOG_PREPARED_WAIT_POLL_MS,
                    Math.max(1L, deadline - System.currentTimeMillis()));
            if (!TaskSleep.sleep(sleepMs)) {
                return action;
            }
            action = runtime.getPreparedDialogAction();
        }
        long now = System.currentTimeMillis();
        boolean usable = isPreparedRouteDialogActionUsable(runtime, action, targetMapName, now);
        log.info("route dialog prepared wait finished: source={} target={} waitedMs={} usable={} preparedTarget={} verifiedAgeMs={}",
                source, targetMapName, Math.max(0L, now - requestedAtMs), usable,
                action == null ? null : action.getTargetKeyword(),
                action == null || action.getLastVerifiedAtMs() <= 0
                        ? null
                        : Math.max(0L, now - action.getLastVerifiedAtMs()));
        return action;
    }

    private boolean isPreparedRouteDialogActionUsable(WindowRuntimeContext runtime,
                                                      PreparedDialogAction action,
                                                      String targetMapName,
                                                      long nowMs) {
        return action != null
                && action.matches(DialogOperation.ROUTE_TRANSFER, targetMapName)
                && matchesCurrentPreparedDialogBinding(runtime, action)
                && action.verifiedWithin(nowMs, ROUTE_DIALOG_PREPARED_CLICK_MAX_AGE_MS);
    }

    private boolean hasMatchingRouteDialogPreparation(WindowRuntimeContext runtime, String targetMapName) {
        if (runtime == null || targetMapName == null || targetMapName.isBlank()) {
            return false;
        }
        long now = System.currentTimeMillis();
        PreparedDialogAction action = runtime.getPreparedDialogAction();
        if (isPreparedRouteDialogActionUsable(runtime, action, targetMapName, now)) {
            return true;
        }
        DialogPreparationStatus status = runtime.getDialogPreparationStatus();
        if (status == null || !status.matches(DialogOperation.ROUTE_TRANSFER, targetMapName)) {
            return false;
        }
        DialogPreparationPhase phase = status.getPhase();
        if (phase == DialogPreparationPhase.READY) {
            log.info("route dialog preparation ready but prepared action is not directly usable: target={} preparedTarget={} sameBinding={} verifiedAgeMs={} maxAgeMs={}",
                    targetMapName,
                    action == null ? null : action.getTargetKeyword(),
                    matchesCurrentPreparedDialogBinding(runtime, action),
                    action == null || action.getLastVerifiedAtMs() <= 0
                            ? null
                            : Math.max(0L, now - action.getLastVerifiedAtMs()),
                    ROUTE_DIALOG_PREPARED_CLICK_MAX_AGE_MS);
            return false;
        }
        return phase == DialogPreparationPhase.REQUESTED
                || phase == DialogPreparationPhase.PREPARING;
    }

    private boolean shouldTryVisibleRouteDialogRescue(WindowRuntimeContext runtime, String targetMapName) {
        if (runtime == null || targetMapName == null || targetMapName.isBlank()) {
            return false;
        }
        WindowPathingSnapshot snapshot = runtime.getPathingSnapshot();
        if (snapshot == null || snapshot.getState() != WindowPathingState.STOPPED_AWAY) {
            return false;
        }
        WindowPathingIntent intent = snapshot.getIntent();
        if (intent == null || intent.getTargetMapName() == null
                || !targetMapName.equals(intent.getTargetMapName())) {
            return false;
        }
        long ageMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
        return ageMs <= ROUTE_DIALOG_VISIBLE_RESCUE_SNAPSHOT_MAX_AGE_MS;
    }

    private void logRouteDialogPreparationSnapshot(WindowRuntimeContext runtime,
                                                   String targetMapName,
                                                   String source) {
        if (runtime == null || targetMapName == null || targetMapName.isBlank()) {
            return;
        }
        DialogPreparationStatus status = runtime.getDialogPreparationStatus();
        PreparedDialogAction action = runtime.getPreparedDialogAction();
        long now = System.currentTimeMillis();
        log.info("route dialog preparation snapshot before world-map search: source={} windowId={} target={} statusPhase={} statusTarget={} preparedTarget={} sameBinding={} verifiedAgeMs={} usable={}",
                source,
                runtime.getWindowId(),
                targetMapName,
                status == null ? null : status.getPhase(),
                status == null ? null : status.getTargetKeyword(),
                action == null ? null : action.getTargetKeyword(),
                matchesCurrentPreparedDialogBinding(runtime, action),
                action == null || action.getLastVerifiedAtMs() <= 0
                        ? null
                        : Math.max(0L, now - action.getLastVerifiedAtMs()),
                isPreparedRouteDialogActionUsable(runtime, action, targetMapName, now));
    }

    private boolean isMatchingRouteDialogPreparing(DialogPreparationStatus status, String targetMapName) {
        return status != null
                && status.getPhase() == DialogPreparationPhase.PREPARING
                && status.matches(DialogOperation.ROUTE_TRANSFER, targetMapName);
    }

    private void requestRouteDialogPreparationAfterMapRouteClick(NavigationRequest request,
                                                                 String targetMapName,
                                                                 String source) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null || targetMapName == null || targetMapName.isBlank()) {
            return;
        }
        String fromMap = null;
        Integer fromX = null;
        Integer fromY = null;
        WindowPathingSnapshot snapshot = runtime.getPathingSnapshot();
        if (snapshot != null && snapshot.getCurrentMapName() != null && !snapshot.getCurrentMapName().isBlank()) {
            long ageMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
            if (ageMs <= RECENT_PATHING_SNAPSHOT_MAX_AGE_MS) {
                fromMap = snapshot.getCurrentMapName();
                fromX = snapshot.getCurrentX();
                fromY = snapshot.getCurrentY();
            }
        }
        if (fromMap == null) {
            PlayerCharacter me = context.getMe();
            fromMap = me == null ? null : me.getCurrentMapName();
            fromX = me == null ? null : me.getX();
            fromY = me == null ? null : me.getY();
        }
        if (targetMapName.equals(fromMap)) {
            return;
        }

        var remembered = transferChoiceMemoryService.findUsable(fromMap, targetMapName).orElse(null);
        long now = System.currentTimeMillis();
        runtime.clearPreparedDialogAction("route link clicked; prepare route dialog from fresh screen");
        runtime.updateDialogPreparationRequest(DialogPreparationRequest.builder()
                .operation(DialogOperation.ROUTE_TRANSFER)
                .targetKeyword(targetMapName)
                .source(source)
                .fromMap(fromMap)
                .rememberedRelativeX(remembered == null ? null : remembered.relativeX)
                .rememberedRelativeY(remembered == null ? null : remembered.relativeY)
                .rememberedOptionText(remembered == null ? null : remembered.optionText)
                .createdAtMs(now)
                .expiresAtMs(now + ROUTE_DIALOG_PREPARE_REQUEST_TTL_MS)
                .build());
        log.info("route dialog preparation requested after map route click: source={} requestSource={} windowId={} from={} coord=({}, {}) target={} memory={}",
                request == null ? null : request.getSource(), source, runtime.getWindowId(),
                fromMap, fromX, fromY, targetMapName, remembered != null);
    }

    private void recordRouteDialogOutcome(RouteDialogClickResult routeDialog, boolean arrived, String source) {
        if (routeDialog == null || routeDialog.relativeX() == null || routeDialog.relativeY() == null) {
            return;
        }
        if (arrived) {
            transferChoiceMemoryService.recordSuccess(
                    routeDialog.fromMap(),
                    routeDialog.fromX(),
                    routeDialog.fromY(),
                    routeDialog.targetMap(),
                    routeDialog.relativeX(),
                    routeDialog.relativeY(),
                    routeDialog.optionText(),
                    source);
            return;
        }
        if (routeDialog.fromMemory()) {
            transferChoiceMemoryService.recordFailure(routeDialog.fromMap(), routeDialog.targetMap(), source);
        }
    }

    private boolean submitWorldMapSearchAndClickDestination(String targetMapName) {
        boolean clicked = inputSequences.submitExclusiveAndWait("submitWorldMapSearchAndClickDestination:" + targetMapName,
                () -> {
                    log.info("navigation map search start: target={}", targetMapName);
                    if (InputActionScope.isCancelled()) {
                        log.info("navigation map search cancelled before input: target={}", targetMapName);
                        return false;
                    }
                    if (!isWorldMapTitleVisible()) {
                        log.info("navigation map search: world map not open, press Alt+2");
                        inputProvider.pressAlt2();
                        if (!TaskSleep.sleep(500) || InputActionScope.isCancelled()) {
                            return false;
                        }
                    }

                    boolean searchInputTouched = false;
                    boolean routeClicked = false;
                    try {
                        for (int attempt = 1; attempt <= 2; attempt++) {
                            if (InputActionScope.isCancelled()) {
                                log.info("navigation map search cancelled before attempt: target={} attempt={}/{}",
                                        targetMapName, attempt, 2);
                                return false;
                            }
                            /*
                             * From this point the route-search input may stay on screen if OCR/scroll/click
                             * fails. Always use the narrow x2-only cleanup on failure so later Alt+1 mini-map
                             * navigation does not click through a stale search overlay.
                             */
                            boolean useOpenRoutePanel = false;
                            Point xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(
                                    XUNLU_TEMPLATE_PATH, THRESHOLD_NORMAL);
                            if (xunluPoint == null && isWorldMapTitleVisible()) {
                                boolean closed = uiCleanerService.closeMapSearchInputByX2Direct(
                                        "navigation:stale-route-panel-before-search:" + targetMapName);
                                log.info("navigation map search: stale route panel close before xunlu target={} closed={}",
                                        targetMapName, closed);
                                if (!closed) {
                                    /*
                                     * Some stale world-map route panels are the full "from/to/search" page. In that
                                     * state the normal xunlu button is hidden and no x2 exists, but the destination
                                     * input is usable. Reuse it directly instead of burning retries on an impossible
                                     * xunlu template.
                                     */
                                    useOpenRoutePanel = true;
                                    log.info("navigation map search: reuse open route panel input target={} attempt={}/{}",
                                            targetMapName, attempt, 2);
                                }
                                if (!TaskSleep.sleep(250) || InputActionScope.isCancelled()) {
                                    return false;
                                }
                            }
                            if (!useOpenRoutePanel && xunluPoint == null && !isWorldMapTitleVisible()) {
                                log.info("navigation map search: world map closed before attempt, press Alt+2 target={}",
                                        targetMapName);
                                inputProvider.pressAlt2();
                                if (!TaskSleep.sleep(500) || InputActionScope.isCancelled()) {
                                    return false;
                                }
                                xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(
                                        XUNLU_TEMPLATE_PATH, THRESHOLD_NORMAL);
                            }

                            int scrollFocusX = tracker.getWindowBaseX() + config.getAnchor_windowTo_map_scroll_X();
                            int scrollFocusY = tracker.getWindowBaseY() + config.getAnchor_windowTo_map_scroll_Y();
                            if (useOpenRoutePanel) {
                                int targetInputX = tracker.getWindowBaseX() + MAP_ROUTE_TARGET_INPUT_X;
                                int targetInputY = tracker.getWindowBaseY() + MAP_ROUTE_TARGET_INPUT_Y;
                                int searchButtonX = tracker.getWindowBaseX() + MAP_ROUTE_SEARCH_BUTTON_X;
                                int searchButtonY = tracker.getWindowBaseY() + MAP_ROUTE_SEARCH_BUTTON_Y;
                                log.info("navigation map search: type target through open route panel target={} input=({}, {}) search=({}, {}) attempt={}/{}",
                                        targetMapName, targetInputX, targetInputY, searchButtonX, searchButtonY, attempt, 2);
                                inputProvider.clickLeft(targetInputX, targetInputY, 80);
                                if (InputActionScope.isCancelled()) {
                                    return false;
                                }
                                inputProvider.pressCtrlA();
                                inputProvider.typeTextUnicode(targetMapName);
                                if (!TaskSleep.sleep(300) || InputActionScope.isCancelled()) {
                                    return false;
                                }
                                inputProvider.clickLeft(searchButtonX, searchButtonY, 120);
                                searchInputTouched = true;
                                if (!TaskSleep.sleep(500) || InputActionScope.isCancelled()) {
                                    return false;
                                }
                            } else {
                                if (xunluPoint == null) {
                                    log.warn("navigation map search: xunlu button not found, target={} attempt={}/{}",
                                            targetMapName, attempt, 2);
                                    return false;
                                }
                                log.info("navigation map search: click xunlu button=({}, {}) attempt={}/{}",
                                        xunluPoint.x, xunluPoint.y, attempt, 2);
                                inputProvider.clickLeft(xunluPoint.x, xunluPoint.y, 120);
                                searchInputTouched = true;
                                if (!TaskSleep.sleep(250) || InputActionScope.isCancelled()) {
                                    return false;
                                }
                                closeWorldMapAfterXunluDirect(targetMapName, attempt);
                                if (InputActionScope.isCancelled()) {
                                    return false;
                                }
                                log.info("navigation map search: type target map={} attempt={}/{}", targetMapName, attempt, 2);
                                inputProvider.typeTextUnicode(targetMapName);
                                if (!TaskSleep.sleep(WORLD_MAP_SEARCH_TYPE_SETTLE_MS) || InputActionScope.isCancelled()) {
                                    return false;
                                }
                                inputProvider.pressEnter();
                            }
                            if (InputActionScope.isCancelled()) {
                                return false;
                            }
                            if (!scrollWorldMapSearchResultsToBottomDirect(scrollFocusX, scrollFocusY,
                                    "submitWorldMapSearchAndClickDestination:" + targetMapName + ":attempt" + attempt)) {
                                return false;
                            }
                            if (InputActionScope.isCancelled()) {
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
                                if (!TaskSleep.sleep(250) || InputActionScope.isCancelled()) {
                                    return false;
                                }
                                continue;
                            }
                            return false;
                        }
                        return false;
                    } finally {
                        if (searchInputTouched && !routeClicked) {
                            if (InputActionScope.isCancelled()) {
                                /*
                                 * This callback is already inside the single input worker. Once the waiting task
                                 * has been interrupted/cancelled, do not perform extra direct-input cleanup here,
                                 * otherwise an old navigation attempt can steal focus after a newer window gets
                                 * the turn.
                                 */
                                log.info("navigation map search cleanup skipped because input request was cancelled: target={}",
                                        targetMapName);
                            } else {
                                closeMapSearchInputAfterRouteClick("submitWorldMapSearchAndClickDestination:failed");
                            }
                        }
                    }
                });
        return clicked;
    }

    private void closeWorldMapAfterXunluDirect(String targetMapName, int attempt) {
        /*
         * The xunlu template can only be clicked while the Alt+2 world map is visible. Close that
         * backing map immediately after the route panel is opened; waiting until the route link click
         * is racy because the game may auto-close the map first and a late Alt+2 would reopen it.
         */
        inputProvider.pressAlt2();
        TaskSleep.sleep(250);
        log.info("navigation map search: close world map immediately after xunlu click target={} attempt={}/{}",
                targetMapName, attempt, 2);
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
        if (InputActionScope.isCancelled()) {
            log.info("navigation map search cancelled after result capture: target={}", expectedDestinationName);
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        GameTextLineOcrService.WorldMapRouteDestinationResult destinationResult =
                gameTextLineOcrService.verifyWorldMapRouteDestination(mapResultImagePath, expectedDestinationName);
        if (InputActionScope.isCancelled()) {
            log.info("navigation map search cancelled after destination OCR: target={}", expectedDestinationName);
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        if (!destinationResult.allowClick()) {
            log.warn("navigation map search: destination mismatch before route click, will retype target expected={} actual={} yellow={}",
                    expectedDestinationName, destinationResult.rawActual(), destinationResult.yellowImagePath());
            archiveMapRouteFailure("destination-mismatch", mapResultImagePath, expectedDestinationName,
                    destinationResult, null);
            return WorldMapDestinationClickResult.WRONG_DESTINATION;
        }

        GameTextLineOcrService.WorldMapRouteCoordinateResult coordinateResult =
                gameTextLineOcrService.findLastWorldMapRouteCoordinate(mapResultImagePath, destinationResult);
        if (InputActionScope.isCancelled()) {
            log.info("navigation map search cancelled after coordinate OCR: target={}", expectedDestinationName);
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        Point relativeCenter = coordinateResult.relativeCenter();
        String routeOcrImagePath = coordinateResult.ocrImagePath();
        if (relativeCenter == null) {
            log.warn("navigation route scan found no coordinate link");
            archiveMapRouteFailure("coordinate-not-found", mapResultImagePath, expectedDestinationName,
                    destinationResult, coordinateResult);
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
            if (InputActionScope.isCancelled()) {
                log.info("navigation map search direct route click skipped because input request was cancelled: target={}",
                        expectedDestinationName);
                return WorldMapDestinationClickResult.NOT_FOUND;
            }
            inputProvider.clickLeft(state.lastAbsoluteLogicalX, state.lastAbsoluteLogicalY, 150);
            gameStateUtil.recordMovementIntent(description);
            closeMapSearchInputAfterRouteClick(description + ":routeClicked");
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
     * Archive route-result screenshots that blocked world-map navigation.
     *
     * <p>The live window-scoped temp image is overwritten on the next navigation attempt, so failed
     * OCR/template cases need to be copied immediately. These files are local regression samples:
     * future route-destination algorithms should be run against this folder before asking the user
     * to test in-game again.</p>
     *
     * @param reason compact failure type, for example {@code destination-mismatch}.
     * @param rawImagePath raw route-result screenshot path, image-local to the result panel.
     * @param expectedDestinationName map name that the route search was trying to click.
     * @param destination destination OCR guard result, or null when it was not reached.
     * @param coordinate coordinate OCR result, or null when it was not reached.
     */
    private void archiveMapRouteFailure(String reason,
                                        String rawImagePath,
                                        String expectedDestinationName,
                                        GameTextLineOcrService.WorldMapRouteDestinationResult destination,
                                        GameTextLineOcrService.WorldMapRouteCoordinateResult coordinate) {
        try {
            String time = LocalDateTime.now().format(FAILURE_CASE_TIME_FORMAT);
            String expected = safeFailureFileName(expectedDestinationName == null ? "unknown" : expectedDestinationName);
            String safeReason = safeFailureFileName(reason == null ? "unknown" : reason);
            Path caseDir = ROUTE_FAILURE_CASE_DIR.resolve(time + "_" + expected + "_" + safeReason).normalize();
            Files.createDirectories(caseDir);

            List<String> copied = new ArrayList<>();
            copyFailureImage(rawImagePath, caseDir.resolve("raw.png"), copied);
            if (destination != null) {
                copyFailureImage(destination.yellowImagePath(), caseDir.resolve("yellow.png"), copied);
            }
            if (coordinate != null) {
                copyFailureImage(coordinate.ocrImagePath(), caseDir.resolve("coordinate_ocr.png"), copied);
            }
            Files.writeString(caseDir.resolve("metadata.txt"), routeFailureMetadata(
                    reason, rawImagePath, expectedDestinationName, destination, coordinate, copied),
                    StandardCharsets.UTF_8);
            log.warn("navigation route failure archived: reason={} expected={} dir={} copied={}",
                    reason, expectedDestinationName, caseDir, copied);
        } catch (Exception e) {
            log.warn("navigation route failure archive failed: reason={} raw={} expected={} error={}",
                    reason, rawImagePath, expectedDestinationName, e.getMessage(), e);
        }
    }

    private void copyFailureImage(String sourcePath, Path targetPath, List<String> copied) throws Exception {
        if (sourcePath == null || sourcePath.isBlank()) {
            return;
        }
        Path source = Path.of(sourcePath);
        if (!Files.exists(source)) {
            return;
        }
        Files.copy(source, targetPath, StandardCopyOption.REPLACE_EXISTING);
        copied.add(targetPath.getFileName().toString());
    }

    private String routeFailureMetadata(String reason,
                                        String rawImagePath,
                                        String expectedDestinationName,
                                        GameTextLineOcrService.WorldMapRouteDestinationResult destination,
                                        GameTextLineOcrService.WorldMapRouteCoordinateResult coordinate,
                                        List<String> copied) {
        StringBuilder builder = new StringBuilder();
        builder.append("reason=").append(reason).append('\n');
        builder.append("expectedDestination=").append(expectedDestinationName).append('\n');
        builder.append("rawImagePath=").append(rawImagePath).append('\n');
        builder.append("windowBase=(").append(tracker.getWindowBaseX()).append(',')
                .append(tracker.getWindowBaseY()).append(")\n");
        windowTaskContextHolder.rawCurrent().ifPresent(context -> builder
                .append("windowId=").append(context.getWindowId()).append('\n')
                .append("windowTitle=").append(context.getNativeBinding().getTitle()).append('\n'));
        if (destination != null) {
            builder.append("destination.checked=").append(destination.checked()).append('\n');
            builder.append("destination.matched=").append(destination.matched()).append('\n');
            builder.append("destination.allowClick=").append(destination.allowClick()).append('\n');
            builder.append("destination.expected=").append(destination.expected()).append('\n');
            builder.append("destination.actual=").append(destination.actual()).append('\n');
            builder.append("destination.rawActual=").append(destination.rawActual()).append('\n');
            builder.append("destination.center=(").append(destination.destinationCenterX()).append(',')
                    .append(destination.destinationCenterY()).append(")\n");
            builder.append("destination.yellowImagePath=").append(destination.yellowImagePath()).append('\n');
            builder.append("destination.elapsedMs=").append(destination.elapsedMs()).append('\n');
            builder.append("destination.message=").append(destination.message()).append('\n');
        }
        if (coordinate != null) {
            builder.append("coordinate.found=").append(coordinate.found()).append('\n');
            builder.append("coordinate.relativeCenter=").append(coordinate.relativeCenter()).append('\n');
            builder.append("coordinate.ocrImagePath=").append(coordinate.ocrImagePath()).append('\n');
            builder.append("coordinate.elapsedMs=").append(coordinate.elapsedMs()).append('\n');
            builder.append("coordinate.message=").append(coordinate.message()).append('\n');
        }
        builder.append("copied=").append(copied).append('\n');
        return builder.toString();
    }

    private String safeFailureFileName(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    /**
     * Close the route search/input panel after clicking a route result.
     *
     * <p>This method is called only from direct-input navigation paths that already own the exclusive
     * input worker callback. At this point the Alt+2 world map backing panel has already been closed
     * right after clicking 寻路; the remaining blocker is the route panel itself, so use the narrow
     * x2-only cleanup instead of toggling Alt+2 and risking reopening the world map.</p>
     *
     * @param source navigation source label for logs.
     */
    private void closeMapSearchInputAfterRouteClick(String source) {
        boolean closed = uiCleanerService.closeMapSearchInputByX2Direct(source + ":closeRoutePanel");
        log.info("navigation map search: route panel x2 close after route click source={} closed={}",
                source, closed);
        if (closed) {
            moveMouseAwayFromRouteCloseDirect(source);
        }
    }

    private void closeWorldMapAfterRouteHandoff(String source) {
        WindowRuntimeContext windowContext = windowTaskContextHolder.rawCurrent().orElse(null);
        if (windowContext == null) {
            log.info("navigation map search background close skipped: source={} reason=no-window-context", source);
            return;
        }
        CompletableFuture.runAsync(() -> windowTaskContextHolder.runWith(windowContext, () -> {
            try {
                if (!isWorldMapTitleVisible()) {
                    log.info("navigation map search background close skipped: source={} windowId={} reason=already-closed",
                            source, windowContext.getWindowId());
                    return;
                }
                BoundWindowKeyboardService.ShortcutAttempt attempt =
                        boundWindowKeyboardService.pressShortcut(BoundWindowKeyboardService.AltShortcut.ALT_2);
                if (attempt.attempted() && attempt.success()) {
                    log.info("navigation map search background close requested through HWND keyboard: source={} windowId={}",
                            source, windowContext.getWindowId());
                } else {
                    log.info("navigation map search background close skipped without focused fallback: source={} windowId={} attempted={} success={} reason={}",
                            source, windowContext.getWindowId(),
                            attempt.attempted(), attempt.success(), attempt.reason());
                }
            } catch (Exception e) {
                log.warn("navigation map search background close failed: source={} windowId={} error={}",
                        source, windowContext.getWindowId(), e.getMessage(), e);
            }
        }));
    }

    private void closeMapSearchInputAfterRouteDialog(String source) {
        boolean closed = inputSequences.submitExclusiveAndWait("navigation:routeDialogCloseX2:" + source,
                () -> {
                    boolean result = uiCleanerService.closeMapSearchInputByX2Direct(source + ":closeMapSearchInput");
                    if (result) {
                        moveMouseAwayFromRouteCloseDirect(source);
                    }
                    return result;
                });
        log.info("navigation route dialog: x2-only close after confirmed arrival source={} closed={}", source, closed);
    }

    private void moveMouseAwayFromRouteCloseDirect(String source) {
        int x = tracker.getWindowBaseX() + random.nextInt(
                ROUTE_CLOSE_RANDOM_MOUSE_MAX_X - ROUTE_CLOSE_RANDOM_MOUSE_MIN_X + 1)
                + ROUTE_CLOSE_RANDOM_MOUSE_MIN_X;
        int y = tracker.getWindowBaseY() + random.nextInt(
                ROUTE_CLOSE_RANDOM_MOUSE_MAX_Y - ROUTE_CLOSE_RANDOM_MOUSE_MIN_Y + 1)
                + ROUTE_CLOSE_RANDOM_MOUSE_MIN_Y;
        inputProvider.moveMouse(x, y);
        log.info("navigation map search: mouse moved away after x2 close source={} point=({}, {})", source, x, y);
    }

    private boolean openWorldMapRoutePanelDirect() {
        if (!isWorldMapTitleVisible()) {
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



    private boolean isWorldMapTitleVisible() {
        return coordinateHelper.findImageAbsoluteCoordinate(WORLD_MAP_TITLE_TEMPLATE_PATH, THRESHOLD_NORMAL) != null;
    }

    private boolean isMiniMapPanelVisible() {
        int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
        return coordinateHelper.findImageInRegion(MINI_MAP_PANEL_CHECKED_TEMPLATE, rect, 0.95) != null
                || coordinateHelper.findImageInRegion(MINI_MAP_PANEL_UNCHECKED_TEMPLATE, rect, 0.95) != null;
    }

    private boolean scrollWorldMapSearchResultsToBottomDirect(int targetX, int targetY, String source) {
        log.info("navigation map search: force scroll to bottom source={} focus=({}, {}) attempts={} units={}",
                source, targetX, targetY, MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS, MAP_RESULT_SCROLL_DOWN_UNITS);
        if (InputActionScope.isCancelled()) {
            log.info("navigation map search scroll skipped because input request was cancelled: source={}", source);
            return false;
        }
        inputProvider.clickLeft(targetX, targetY, 50);
        for (int i = 1; i <= MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS; i++) {
            if (InputActionScope.isCancelled()) {
                log.info("navigation map search scroll cancelled: source={} attempt={}/{}",
                        source, i, MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS);
                return false;
            }
            inputProvider.scrollDown(MAP_RESULT_SCROLL_DOWN_UNITS);
            if (!TaskSleep.sleep(MAP_RESULT_SCROLL_INTERVAL_MS) || InputActionScope.isCancelled()) {
                return false;
            }
        }
        return TaskSleep.sleep(MAP_RESULT_SCROLL_SETTLE_MS) && !InputActionScope.isCancelled();
    }

    // =====================
    // Mini-map click helpers
    // =====================

    /**
     * Submit one atomic mini-map click sequence.
     *
     * <p>The first current-map attempt intentionally skips mini-map panel matching: assume the panel
     * is closed, press Alt+1, and click immediately. Retries may enable {@code checkPanelBeforeOpen}
     * so a positive panel hit can prevent toggling an already-open panel closed.</p>
     *
     * @param pixelPoint screen-absolute click point produced by {@link CoordinateHelper}.
     * @param description input queue request label.
     * @param closeAfterClick true when this sequence must close Alt+1 after clicking.
     * @param checkPanelBeforeOpen true only on retry paths that should trust a positive panel hit
     *                             before deciding whether Alt+1 is needed.
     * @return true if the queue accepted and completed the sequence.
     */
    private boolean submitMiniMapClick(Point pixelPoint,
                                       String description,
                                       boolean closeAfterClick,
                                       boolean checkPanelBeforeOpen) {
        return inputSequences.submitExclusiveAndWait(description, () -> {
            /*
             * Only retry paths pay the template-match cost. A positive hit is reliable enough to
             * skip Alt+1; a miss is not authoritative, so the fallback remains "press Alt+1 once".
             */
            if (checkPanelBeforeOpen && isMiniMapPanelVisible()) {
                log.info("mini-map panel visible before coordinate click; skip Alt+1 open: source={}",
                        description);
            } else {
                pressAlt1ForMiniMap(description + ":open");
                if (!TaskSleep.sleep(MINI_MAP_OPEN_SETTLE_MS)) {
                    return false;
                }
                log.info("mini-map Alt+1 open assumed before coordinate click: source={} checkPanelBeforeOpen={}",
                        description, checkPanelBeforeOpen);
            }

            inputProvider.clickLeft(pixelPoint.x, pixelPoint.y, 200);
            if (!TaskSleep.sleep(MINI_MAP_CLICK_SETTLE_MS)) {
                return false;
            }
            if (!closeAfterClick) {
                /*
                 * Leave the panel state unknown after the click. If a retry is needed, that retry
                 * will do the positive-only panel check before deciding whether to press Alt+1.
                 */
                log.info("mini-map left in unknown/open state after coordinate click: source={}", description);
                return true;
            }
            if (isMiniMapPanelVisible()) {
                pressAlt1ForMiniMap(description + ":close");
                if (!TaskSleep.sleep(300)) {
                    return false;
                }
                if (isMiniMapPanelVisible()) {
                    log.warn("mini-map panel remained visible after close, pressing Alt+1 once more: source={}",
                            description);
                    pressAlt1ForMiniMap(description + ":close-retry");
                    return TaskSleep.sleep(300);
                }
            }
            return true;
        });
    }

    private void closeMiniMapIfOpen(String source) {
        inputSequences.submitExclusiveAndWait(source, () -> {
            if (!isMiniMapPanelVisible()) {
                return true;
            }
            pressAlt1ForMiniMap(source + ":close");
            if (!TaskSleep.sleep(300)) {
                return false;
            }
            if (isMiniMapPanelVisible()) {
                log.warn("mini-map panel remained visible after finish close, pressing Alt+1 once more: source={}",
                        source);
                pressAlt1ForMiniMap(source + ":close-retry");
                return TaskSleep.sleep(300);
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
    private MiniMapPathingAttemptResult clickMiniMapPointForHandoff(CoordinateHelper.MiniMapClickPoint clickPoint,
                                                                    String description,
                                                                    String expectedMapName,
                                                                    boolean checkPanelBeforeOpen) {
        if (clickPoint == null) {
            return MiniMapPathingAttemptResult.INCONCLUSIVE;
        }
        String source = description + ":" + clickPoint.reason()
                + ":logical=(" + clickPoint.logicalX() + "," + clickPoint.logicalY() + ")"
                + ":pixel=(" + clickPoint.pixelPoint().x + "," + clickPoint.pixelPoint().y + ")"
                + ":jitter=(" + clickPoint.jitterX() + "," + clickPoint.jitterY() + ")";
        TemplateLocationInfo baselineLocation = miniMapCoordinateReader.readCurrentTemplateLocation().orElse(null);
        MapCoordinate baseline = baselineLocation == null
                ? currentKnownCoordinate()
                : baselineLocation.coordinate();
        if (!submitMiniMapClick(clickPoint.pixelPoint(), source, false, checkPanelBeforeOpen)) {
            log.warn("mini-map handoff click input failed: source={} pixel=({}, {})",
                    source, clickPoint.pixelPoint().x, clickPoint.pixelPoint().y);
            return MiniMapPathingAttemptResult.INCONCLUSIVE;
        }
        /*
         * Handoff callers care more about releasing the task turn quickly than proving arrival in
         * this foreground slice. Try the cheap edge-frame proof first; if it is inconclusive, fall
         * back to the coordinate reader so failed mini-map clicks still get a retry path.
         */
        MiniMapPathingAttemptResult confirmResult = confirmMiniMapPathingStartedForHandoff(
                source, baseline, expectedMapName, baselineLocation);
        if (confirmResult != MiniMapPathingAttemptResult.PATHING_STARTED) {
            log.info("mini-map handoff click did not start pathing: source={} pixel=({}, {}) baseline={} result={}",
                    source, clickPoint.pixelPoint().x, clickPoint.pixelPoint().y,
                    formatCoordinate(baseline), confirmResult);
            return confirmResult;
        }
        gameStateUtil.recordMovementIntent(source);
        closeMiniMapAfterHandoff(source);
        return MiniMapPathingAttemptResult.PATHING_STARTED;
    }

    private MiniMapPathingAttemptResult confirmMiniMapPathingStartedForHandoff(String source,
                                                                              MapCoordinate baseline,
                                                                              String expectedMapName,
                                                                              TemplateLocationInfo baselineLocation) {
        long startedAt = System.currentTimeMillis();
        if (gameStateUtil.confirmPathingStartedByEdgePixelDiff(source + ":handoff-fast-edge")) {
            log.info("mini-map handoff pathing confirmed by fast edge pixels: source={} elapsedMs={}",
                    source, Math.max(0L, System.currentTimeMillis() - startedAt));
            return MiniMapPathingAttemptResult.PATHING_STARTED;
        }
        long fallbackStartedAt = System.currentTimeMillis();
        MiniMapPathingAttemptResult result = confirmMiniMapPathingStarted(
                source, baseline, expectedMapName, baselineLocation);
        log.info("mini-map handoff coordinate fallback completed: source={} result={} edgeElapsedMs={} fallbackElapsedMs={}",
                source, result,
                Math.max(0L, fallbackStartedAt - startedAt),
                Math.max(0L, System.currentTimeMillis() - fallbackStartedAt));
        return result;
    }

    private void closeMiniMapAfterHandoff(String source) {
        WindowRuntimeContext windowContext = windowTaskContextHolder.rawCurrent().orElse(null);
        if (windowContext == null) {
            log.info("mini-map background close skipped: source={} reason=no-window-context", source);
            return;
        }
        CompletableFuture.runAsync(() -> windowTaskContextHolder.runWith(windowContext, () -> {
            try {
                if (!isMiniMapPanelVisible()) {
                    log.info("mini-map background close skipped: source={} windowId={} reason=already-closed",
                            source, windowContext.getWindowId());
                    return;
                }
                BoundWindowKeyboardService.ShortcutAttempt attempt =
                        boundWindowKeyboardService.pressShortcut(BoundWindowKeyboardService.AltShortcut.ALT_1);
                if (attempt.attempted() && attempt.success()) {
                    log.info("mini-map background close requested through HWND keyboard: source={} windowId={}",
                            source, windowContext.getWindowId());
                } else {
                    log.info("mini-map background close skipped without focused fallback: source={} windowId={} attempted={} success={} reason={}",
                            source, windowContext.getWindowId(),
                            attempt.attempted(), attempt.success(), attempt.reason());
                }
            } catch (Exception e) {
                log.warn("mini-map background close failed: source={} windowId={} error={}",
                        source, windowContext.getWindowId(), e.getMessage(), e);
            }
        }));
    }

    private MiniMapPathingAttemptResult clickMiniMapPointAndConfirm(CoordinateHelper.MiniMapClickPoint clickPoint,
                                                                    String description,
                                                                    boolean closeAfterClick,
                                                                    boolean checkPanelBeforeOpen) {
        if (clickPoint == null) {
            return MiniMapPathingAttemptResult.INCONCLUSIVE;
        }
        String source = description + ":" + clickPoint.reason()
                + ":logical=(" + clickPoint.logicalX() + "," + clickPoint.logicalY() + ")"
                + ":pixel=(" + clickPoint.pixelPoint().x + "," + clickPoint.pixelPoint().y + ")"
                + ":jitter=(" + clickPoint.jitterX() + "," + clickPoint.jitterY() + ")";
        TemplateLocationInfo baselineLocation = miniMapCoordinateReader.readCurrentTemplateLocation().orElse(null);
        MapCoordinate baseline = baselineLocation == null
                ? currentKnownCoordinate()
                : baselineLocation.coordinate();
        if (!submitMiniMapClick(clickPoint.pixelPoint(), source, closeAfterClick, checkPanelBeforeOpen)) {
            log.warn("mini-map click input failed: source={} pixel=({}, {})",
                    source, clickPoint.pixelPoint().x, clickPoint.pixelPoint().y);
            return MiniMapPathingAttemptResult.INCONCLUSIVE;
        }
        MiniMapPathingAttemptResult confirmResult = confirmMiniMapPathingStarted(
                source, baseline, null, baselineLocation);
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
    private boolean syncAndCheckArrived(int targetX, int targetY, int tolerance, String source) {
        playerStateService.syncMyPosition();
        PlayerCharacter me = context.getMe();
        boolean arrived = me != null
                && gameStateUtil.isNearCoordinate(null, me.getX(), me.getY(), null, targetX, targetY, tolerance);
        if (arrived) {
            log.info("arrived: source={} current=({}, {}) target=({}, {}) tolerance={}",
                    source, me.getX(), me.getY(), targetX, targetY, tolerance);
        }
        return arrived;
    }

    /**
     * Cheap arrival guard for handoff-style navigation.
     *
     * @param targetX target logical X coordinate.
     * @param targetY target logical Y coordinate.
     * @param source log source. This method deliberately avoids OCR; callers that must prove arrival
     *               from a fresh screenshot should use {@link #syncAndCheckArrived(int, int, int, String)}.
     * @return true when the already-synced per-window state is near the target coordinate.
     */
    private boolean isCurrentCachedCoordinateNear(int targetX, int targetY, int tolerance, String source) {
        PlayerCharacter me = context.getMe();
        boolean arrived = me != null
                && gameStateUtil.isNearCoordinate(null, me.getX(), me.getY(), null, targetX, targetY, tolerance);
        if (arrived) {
            log.info("arrived by cached coordinate: source={} current=({}, {}) target=({}, {}) tolerance={}",
                    source, me.getX(), me.getY(), targetX, targetY, tolerance);
        }
        return arrived;
    }

    /**
     * Poll for the observable movement result of a mini-map click.
     *
     * @param source log label for the click attempt being confirmed.
     * @param expectedMapName map that should still contain this current-map click; null allows the
     *                        legacy coordinate-only confirmation.
     * @param baselineLocation optional pre-click map+coordinate snapshot from the mini-map strip.
     * @return PATHING_STARTED only when post-click coordinates differ without leaving the expected
     *         map. The cached player coordinate is diagnostic-only here; a stale/misread cached value
     *         must not make a failed click look successful.
     */
    private MiniMapPathingAttemptResult confirmMiniMapPathingStarted(String source,
                                                                     MapCoordinate baseline,
                                                                     String expectedMapName,
                                                                     TemplateLocationInfo baselineLocation) {
        long deadline = System.currentTimeMillis() + MINI_MAP_PATHING_COORD_CONFIRM_TIMEOUT_MS;
        MapCoordinate previousReadable = null;
        String expectedMap = expectedMapName == null || expectedMapName.isBlank()
                ? (baselineLocation == null ? null : baselineLocation.mapName())
                : expectedMapName;
        while (System.currentTimeMillis() < deadline) {
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

            TemplateLocationInfo currentLocation = miniMapCoordinateReader.readCurrentTemplateLocation().orElse(null);
            MapCoordinate current = currentLocation == null ? null : currentLocation.coordinate();
            if (current == null) {
                current = miniMapCoordinateReader.readCurrentCoordinate().orElse(null);
            }
            if (current != null) {
                if (isCoordinateChanged(baseline, current) || isCoordinateChanged(previousReadable, current)) {
                    String currentMap = currentLocation == null ? null : currentLocation.mapName();
                    if (expectedMap != null
                            && currentMap != null
                            && !gameStateUtil.isSameMapName(currentMap, expectedMap)) {
                        log.warn("mini-map pathing confirmation rejected: coordinate changed on unexpected map source={} expectedMap={} baselineMap={} currentMap={} baseline={} previous={} current={}",
                                source, expectedMap, baselineLocation == null ? null : baselineLocation.mapName(),
                                currentMap, formatCoordinate(baseline), formatCoordinate(previousReadable),
                                formatCoordinate(current));
                        return MiniMapPathingAttemptResult.NO_PATHING;
                    }
                    log.info("mini-map pathing confirmation: post-click coordinate changed source={} expectedMap={} currentMap={} baseline={} previous={} current={}",
                            source, expectedMap, currentMap, formatCoordinate(baseline),
                            formatCoordinate(previousReadable), formatCoordinate(current));
                    return MiniMapPathingAttemptResult.PATHING_STARTED;
                }
                previousReadable = current;
            }

            log.info("mini-map pathing confirmation: no post-click coordinate movement yet source={} expectedMap={} currentMap={} baseline={} previous={} current={}",
                    source, expectedMap, currentLocation == null ? null : currentLocation.mapName(),
                    formatCoordinate(baseline), formatCoordinate(previousReadable), formatCoordinate(current));
            if (!TaskSleep.sleep(MINI_MAP_PATHING_COORD_CONFIRM_POLL_MS)) {
                return MiniMapPathingAttemptResult.INCONCLUSIVE;
            }
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
        }
        return MiniMapPathingAttemptResult.NO_PATHING;
    }

    private MapCoordinate currentKnownCoordinate() {
        PlayerCharacter me = context.getMe();
        if (me == null) {
            return null;
        }
        return new MapCoordinate(me.getX(), me.getY());
    }

    private boolean isCoordinateChanged(MapCoordinate baseline, MapCoordinate current) {
        return baseline != null
                && current != null
                && (baseline.getX() != current.getX() || baseline.getY() != current.getY());
    }

    /**
     * Publish navigation handoff intent to the current bound window.
     *
     * @param request original navigation request. Target map is always used; target coordinates are
     *                recorded only for current-map coordinate pathing.
     * @param phase navigation phase that produced PATHING_STARTED.
     * @param message navigation result message.
     * @param includeCoordinate true when targetX/targetY belong to the active pathing leg.
     */
    private void registerWindowPathingIntent(NavigationRequest request,
                                             String phase,
                                             String message,
                                             boolean includeCoordinate) {
        if (request == null || !request.isPublishWindowPathingIntent()) {
            return;
        }
        windowTaskContextHolder.rawCurrent().ifPresent((WindowRuntimeContext windowContext) -> {
            WindowPathingIntent intent = WindowPathingIntent.builder()
                    .source(request.getSource() + ":" + phase + ":" + message)
                    .targetMapName(request.getTargetMapName())
                    .targetX(includeCoordinate ? request.getTargetX() : null)
                    .targetY(includeCoordinate ? request.getTargetY() : null)
                    .tolerance(navigationArrivalTolerance(request))
                    .build();
            windowContext.markPathingStarted(intent);
            log.info("window pathing intent registered: windowId={} phase={} source={} targetMap={} target=({}, {}) tolerance={}",
                    windowContext.getWindowId(), phase, intent.getSource(), intent.getTargetMapName(),
                    intent.getTargetX(), intent.getTargetY(), intent.getTolerance());
        });
    }

    private int navigationArrivalTolerance(NavigationRequest request) {
        if (request != null && request.isExactMiniMapClickOnly()) {
            return 0;
        }
        return request == null ? 5 : Math.max(0, request.getArrivalTolerance());
    }

    private String formatCoordinate(MapCoordinate coordinate) {
        if (coordinate == null) {
            return "null";
        }
        return "(" + coordinate.getX() + "," + coordinate.getY() + ")";
    }

    // ==========================
    // Shared navigation utilities
    // ==========================

    private boolean isActiveNavigationMovement(GameStateUtil.MovementState state) {
        return state == GameStateUtil.MovementState.MOVING
                || state == GameStateUtil.MovementState.PATHING_ACTIVE
                || state == GameStateUtil.MovementState.MAYBE_MOVING;
    }

    private boolean isConfirmedNavigationMovement(GameStateUtil.MovementState state) {
        return state == GameStateUtil.MovementState.MOVING
                || state == GameStateUtil.MovementState.PATHING_ACTIVE;
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

    private record RouteDialogClickResult(
            DialogResultStatus result,
            boolean fromMemory,
            String fromMap,
            Integer fromX,
            Integer fromY,
            String targetMap,
            Integer relativeX,
            Integer relativeY,
            String optionText) {
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
