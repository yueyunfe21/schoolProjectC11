package com.bot.dhxy.service;

import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.cloud.runtime.RuntimeDecisionShadowService;
import com.bot.dhxy.cloud.task.ImagePreprocessCloudRequest;
import com.bot.dhxy.cloud.task.RouteCloudDecision;
import com.bot.dhxy.cloud.task.RouteCloudDecisionService;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.DialogPreparationRequest;
import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogPreparationStatus;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.navigation.PendingTransferChoiceMemory;
import com.bot.dhxy.model.navigation.TemplateLocationInfo;
import com.bot.dhxy.model.navigation.PendingRouteOutcome;
import com.bot.dhxy.model.navigation.WorldMapRouteResultMode;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.model.npc.NpcTooltipType;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowReadyEvent;
import com.bot.dhxy.window.model.WindowReadyEventType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowReadyEventBus;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
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
    private static final long MAP_RESULT_SCROLL_SETTLE_MS = 200L;
    private static final long WORLD_MAP_SEARCH_TYPE_SETTLE_MS = 200L;
    private static final int GAME_WINDOW_WIDTH = 1024;
    private static final int GAME_WINDOW_HEIGHT = 768;
    private static final String ROUTE_RESULT_PAYLOAD_MIME_TYPE = "image/png";
    private static final int ROUTE_CLOSE_RANDOM_MOUSE_MIN_X = 120;
    private static final int ROUTE_CLOSE_RANDOM_MOUSE_MAX_X = 900;
    private static final int ROUTE_CLOSE_RANDOM_MOUSE_MIN_Y = 130;
    private static final int ROUTE_CLOSE_RANDOM_MOUSE_MAX_Y = 620;
    private static final DateTimeFormatter FAILURE_CASE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final long RECENT_PATHING_SNAPSHOT_MAX_AGE_MS = 1500L;
    private static final long ROUTE_PREPARED_DIALOG_CLICK_MAX_AGE_MS = 10_000L;
    private static final long ROUTE_DIALOG_REQUESTED_YIELD_MAX_MS = 3_000L;
    private static final long ROUTE_DIALOG_PREPARING_YIELD_MAX_MS = 30_000L;
    private static final long ROUTE_DIALOG_VISIBLE_RESCUE_SNAPSHOT_MAX_AGE_MS = 120_000L;
    private static final long ROUTE_DIALOG_VISIBLE_GATE_MAX_AGE_MS = 10_000L;
    private static final long ROUTE_DIALOG_ATTENTION_GATE_MAX_AGE_MS = 10_000L;
    private static final long ROUTE_DIALOG_ACTIVE_INTENT_GATE_MAX_AGE_MS = 60_000L;
    private static final long ROUTE_DIALOG_UNKNOWN_INTENT_GATE_MAX_AGE_MS = 10_000L;
    private static final long MINI_MAP_OPEN_SETTLE_MS = 500L;
    private static final long MINI_MAP_CLICK_SETTLE_MS = 250L;
    private static final long ROUTE_DIALOG_SETTLE_MS = 500L;
    private static final long ROUTE_DIALOG_ARRIVAL_CONFIRM_TIMEOUT_MS = 2500L;
    private static final long ROUTE_DIALOG_ARRIVAL_CONFIRM_POLL_MS = 500L;
    private static final long MINI_MAP_PATHING_CONFIRM_TIMEOUT_MS = 1500L;
    private static final long MINI_MAP_PATHING_CONFIRM_POLL_MS = 250L;
    private static final long MINI_MAP_PATHING_COORD_CONFIRM_TIMEOUT_MS = 1000L;
    private static final long MINI_MAP_PATHING_COORD_CONFIRM_POLL_MS = 200L;
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
    private final WindowReadyEventBus windowReadyEventBus;
    private final MapNameCanonicalizer mapNameCanonicalizer;
    private final MemoryService memoryService;
    private final RuntimeDecisionShadowService runtimeDecisionShadowService;
    private final RouteCloudDecisionService routeCloudDecisionService;

    private final Map<String, NavigationRuntimeState> runtimeStates = new ConcurrentHashMap<>();

    // ==============================
    // Public navigation entry points
    // ==============================

    /**
     * Navigate to a fixed NPC coordinate, optionally keeping the current task turn after pathing starts.
     *
     * @param request NPC navigation request. Coordinates are logical in-game map coordinates; nullable
     *                fields such as target name/source are used only for diagnostics. The method
     *                reports PATHING_STARTED as soon as either the map route or the current-map click
     *                begins moving.
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
                            + "(" + request.getTargetX() + "," + request.getTargetY() + ")");
        }
    }

    /**
     * Navigate across maps using the world-map search UI.
     *
     * @param request map navigation request. The target map name is the game-visible map name used
     *                for route search and arrival confirmation. Cross-map navigation uses pathing
     *                handoff semantics: once a route link/dialog is clicked, the window watcher owns
     *                movement, arrival, and stopped-away classification.
     * @return structured navigation result. ARRIVED means the game reaches the target map or already
     *         appears to be there.
     */
    public NavigationResult navigateToMap(NavigationRequest request) {
        if (request == null) {
            log.warn("navigateToMap skipped: request is null");
            return NavigationResult.failed("request is null");
        }
        String targetMapName = request.getTargetMapName();
        String source = request.getSource();
        long latencyStart = LatencyMetrics.start();
        NavigationResult result = NavigationResult.mapNotReached("not started");
        boolean pathingIntentAlreadyActive = false;
        boolean pathingIntentOwnedByNestedRoute = false;
        try {
            PlayerCharacter me = context.getMe();
            log.info("navigate to map: {} current={}", targetMapName, me.getCurrentMapName());

            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
            WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
            if (runtime != null) {
                /*
                 * A ready route-transfer dialog is the highest-priority leader action. It means the
                 * watcher has already proven that the transfer option is visible and click-ready; if
                 * we let the stale-cache/pathing guard run first, it can return PATHING_ACTIVE and
                 * leave the transfer dialog sitting open for another turn.
                 */
                RouteDialogClickResult routeDialog = consumePreparedRouteDialogAction(
                        runtime,
                        targetMapName,
                        source,
                        "navigateToMap:prepared-route-dialog-priority",
                        null,
                        null,
                        null,
                        ":priority-prepared");
                if (routeDialog != null) {
                    if (routeDialog.result() == DialogResultStatus.OPTION_KEYWORD_CLICKED) {
                        pathingIntentAlreadyActive = runtime.getActivePathingIntent()
                                .map(intent -> isActivePathingIntentCompatibleWithRequest(intent, targetMapName, source))
                                .orElse(false);
                        result = NavigationResult.pathingStarted("route dialog clicked before pathing guard; observer will confirm pathing");
                        return result;
                    }
                    log.info("prepared route dialog priority consume did not click: target={} source={} result={}",
                            targetMapName, source, routeDialog.result());
                }
            }
            long stageStartedAt = System.currentTimeMillis();
            /*
             * Fresh confirmation is mandatory before map navigation can report ARRIVED. A stale
             * cached map is especially dangerous for navigateToNPC(): it would make the next step
             * click the NPC coordinate on whatever map is actually open.
             */
            RecentPathingMapCheck snapshotMapCheck = confirmCurrentMapFromRecentPathingSnapshot(
                    targetMapName, "navigateToMap:staleCacheGuard");
            boolean arrivedAfterFreshCheck;
            if (snapshotMapCheck == RecentPathingMapCheck.ARRIVED) {
                arrivedAfterFreshCheck = true;
            } else if (snapshotMapCheck == RecentPathingMapCheck.PATHING_ACTIVE) {
                DialogPreparationStatus status = runtime == null ? null : runtime.getDialogPreparationStatus();
                PreparedDialogAction action = runtime == null ? null : runtime.getPreparedDialogAction();
                boolean currentAlreadyTarget = gameStateUtil.isSameMapName(me.getCurrentMapName(), targetMapName);
                boolean sameTargetIntent = runtime != null
                        && runtime.getActivePathingIntent()
                        .map(intent -> gameStateUtil.isSameMapName(intent.getTargetMapName(), targetMapName))
                        .orElse(false);
                WindowPathingIntent activeIntent = runtime == null
                        ? null
                        : runtime.getActivePathingIntent().orElse(null);
                boolean compatibleActiveIntent =
                        isActivePathingIntentCompatibleWithRequest(activeIntent, targetMapName, source);
                if (runtime != null && !currentAlreadyTarget && compatibleActiveIntent) {
                    if (shouldYieldForRouteDialogBeforeWorldMap(
                            runtime, targetMapName, source, source + ":pathing-active-gate")) {
                        boolean freshSameTargetRoutePending = isFreshSameTargetRoutePending(
                                runtime, targetMapName, source, System.currentTimeMillis(), true);
                        if (freshSameTargetRoutePending) {
                            pathingIntentAlreadyActive = true;
                        }
                        result = freshSameTargetRoutePending
                                ? NavigationResult.pathingStarted("same target route already submitted; watcher will confirm pathing")
                                : NavigationResult.dialogPreparing("pathing active but route option is visible; watcher will prepare route dialog");
                        return result;
                    }
                    log.info("navigate to map pathing-active route gate skipped: source={} target={} current={} existingPrepPhase={} preparedTarget={} sameTargetIntent={}",
                            source, targetMapName, me.getCurrentMapName(),
                            status == null ? null : status.getPhase(),
                            action == null ? null : action.getTargetKeyword(),
                            sameTargetIntent);
                }
                if (runtime != null
                        && compatibleActiveIntent
                        && ((status != null && status.matches(DialogOperation.ROUTE_TRANSFER, targetMapName))
                        || (action != null && action.matches(DialogOperation.ROUTE_TRANSFER, targetMapName)))) {
                    /*
                     * The route is already being followed by the window watcher. Any previous
                     * route-dialog preparation belongs to the same submitted route and must not
                     * later time out into a fresh world-map search.
                     */
                    runtime.clearDialogPreparationRequest("target route already pathing by watcher snapshot");
                }
                if (compatibleActiveIntent) {
                    log.info("navigate to map stale-cache guard: target={} current={} pathingActive=true",
                            targetMapName, me.getCurrentMapName());
                    pathingIntentAlreadyActive = true;
                    result = NavigationResult.pathingStarted("recent window pathing still active; observer will confirm map");
                    return result;
                }
                log.info("navigate to map active pathing ignored because target/source changed: source={} target={} activeIntentSource={} activeIntentTarget={} current={}",
                        source, targetMapName,
                        activeIntent == null ? null : activeIntent.getSource(),
                        activeIntent == null ? null : activeIntent.getTargetMapName(),
                        me.getCurrentMapName());
                arrivedAfterFreshCheck = gameStateUtil.confirmCurrentMapFresh(
                        targetMapName, 0L, "navigateToMap:staleCacheGuard:mismatched-active-intent");
            } else if (hasFreshCurrentLocationForMapGuard(request)) {
                arrivedAfterFreshCheck = gameStateUtil.isSameMapName(request.getFreshCurrentMapName(), targetMapName);
                log.info("[map-confirm] reused caller fresh location reason=navigateToMap:staleCacheGuard target={} current={} coord=({}, {}) arrived={}",
                        targetMapName, request.getFreshCurrentMapName(),
                        request.getFreshCurrentX(), request.getFreshCurrentY(), arrivedAfterFreshCheck);
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
                DialogPreparationStatus status = runtime == null ? null : runtime.getDialogPreparationStatus();
                PreparedDialogAction action = runtime == null ? null : runtime.getPreparedDialogAction();
                if (runtime != null
                        && ((status != null && status.matches(DialogOperation.ROUTE_TRANSFER, targetMapName))
                        || (action != null && action.matches(DialogOperation.ROUTE_TRANSFER, targetMapName)))) {
                    runtime.clearDialogPreparationRequest("target map confirmed before route dialog click");
                }
                result = NavigationResult.arrived("target map confirmed by stale-cache guard");
                return result;
            }
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
            stageStartedAt = System.currentTimeMillis();
            if (runtime != null) {
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
            logRouteDialogPreparationSnapshot(runtime, targetMapName, source);
            NavigationResult gateResult = routeDialogGateBeforeWorldMap(
                    targetMapName, source, "navigateToMap:before-world-map");
            if (gateResult != null) {
                if (gateResult.getStatus() == NavigationResultStatus.PATHING_STARTED) {
                    pathingIntentAlreadyActive = runtime != null && runtime.getActivePathingIntent()
                            .map(intent -> isActivePathingIntentCompatibleWithRequest(intent, targetMapName, source))
                            .orElse(false);
                }
                result = gateResult;
                return result;
            }

            /*
             * First route submission: open the world map, search the target map, scroll to the bottom
             * result, and click the last route link. The called method owns the exclusive input section.
             */
            stageStartedAt = System.currentTimeMillis();
            NavigationResult submitResult = submitWorldMapSearchAndClickDestination(request, targetMapName, source);
            if (submitResult.getStatus() == NavigationResultStatus.DIALOG_PREPARING) {
                result = submitResult;
                return result;
            }
            if (submitResult.getStatus() == NavigationResultStatus.PATHING_STARTED) {
                log.info("[productionNavigate-latency] stage=world-map-submit elapsedMs={} totalMs={} target={} source={} result=clicked",
                        Math.max(0L, System.currentTimeMillis() - stageStartedAt),
                        LatencyMetrics.elapsedMs(latencyStart),
                        targetMapName, source);
                /*
                 * submitWorldMapSearchAndClickDestination owns the route-result pathing intent.
                 * The only remaining route is CR99's yellow-link path, which registers the final
                 * coordinate intent there. The old green-link route is deleted: requests without
                 * final coordinates are rejected before submission and never register a map-only
                 * legacy intent. Do not let the outer finally replace the current-leg intent with
                 * a second same-leg navigateToMap intent.
                 */
                pathingIntentOwnedByNestedRoute = true;
                result = submitResult;
                return result;
            }
            if (!submitResult.success()) {
                log.info("[productionNavigate-latency] stage=world-map-submit elapsedMs={} totalMs={} target={} source={} result=failed",
                        Math.max(0L, System.currentTimeMillis() - stageStartedAt),
                        LatencyMetrics.elapsedMs(latencyStart),
                        targetMapName, source);
                log.warn("first navigate attempt failed, entering retry loop");
                /*
                 * Phase tasks call map navigation while holding the task turn. If the first
                 * route submission cannot even click a route, do not monopolize the turn for
                 * the removed blocking arrival loop; return a retryable navigation result quickly
                 * so the task layer can clean/retry without starving other windows.
                 */
                result = NavigationResult.mapNotReached("map route submit failed");
                return result;
            }
            result = submitResult;
            return result;
        } finally {
            if (result.getStatus() == NavigationResultStatus.PATHING_STARTED) {
                if (pathingIntentAlreadyActive) {
                    log.info("skip duplicate pathing intent registration: source={} target={} reason=watcher-already-active",
                            source, targetMapName);
                } else if (pathingIntentOwnedByNestedRoute) {
                    log.info("skip outer pathing intent registration: source={} target={} reason=nested-route-owns-current-leg",
                            source, targetMapName);
                } else {
                    registerWindowPathingIntent(request, "navigateToMap", result.getMessage(), false);
                }
            }
            LatencyMetrics.info(log, "navigation.toMap", latencyStart,
                    "result=" + result.getStatus() + " source=" + source + " target=" + targetMapName);
        }
    }

    private RecentPathingMapCheck confirmCurrentMapFromRecentPathingSnapshot(String targetMapName, String source) {
        WindowRuntimeContext windowContext = windowTaskContextHolder.rawCurrent().orElse(null);
        if (windowContext == null) {
            return RecentPathingMapCheck.NO_USABLE_SNAPSHOT;
        }
        WindowPathingSnapshot snapshot = windowContext.getPathingSnapshot();
        if (snapshot == null
                || snapshot.getState() == WindowPathingState.NONE
                || snapshot.getState() == WindowPathingState.UNKNOWN
                || snapshot.getCurrentMapName() == null
                || snapshot.getCurrentMapName().isBlank()) {
            return RecentPathingMapCheck.NO_USABLE_SNAPSHOT;
        }
        long ageMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
        if (ageMs > RECENT_PATHING_SNAPSHOT_MAX_AGE_MS) {
            return RecentPathingMapCheck.NO_USABLE_SNAPSHOT;
        }
        boolean arrived = gameStateUtil.isSameMapName(snapshot.getCurrentMapName(), targetMapName);
        RecentPathingMapCheck result = arrived
                ? RecentPathingMapCheck.ARRIVED
                : (snapshot.getState() == WindowPathingState.ACTIVE
                ? RecentPathingMapCheck.PATHING_ACTIVE
                : RecentPathingMapCheck.NO_USABLE_SNAPSHOT);
        log.info("navigate to map uses recent pathing snapshot: source={} target={} current={} state={} ageMs={} result={}",
                source, targetMapName, snapshot.getCurrentMapName(), snapshot.getState(), ageMs, result);
        return result;
    }

    /**
     * Navigate within the current map by clicking mini-map logical coordinates until arrival.
     *
     * @param request current-map navigation request. targetX/targetY are logical in-game coordinates
     *                on the active map. Current-map navigation uses pathing handoff semantics: once a
     *                mini-map click is proven to start movement, the caller receives PATHING_STARTED
     *                and the window watcher owns arrival/stopped-away classification.
     * @return structured navigation result. ARRIVED means the current window reaches the coordinate
     *         tolerance; POINT_NOT_REACHED means timeout or exhausted click candidates.
     */
    public NavigationResult navigateInCurrentMap(NavigationRequest request) {
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
        boolean pathingIntentRegistered = false;
        try {
            String mapName = context.getMe().getCurrentMapName();
            log.info("navigate in map: {} target=({}, {})", mapName, targetX, targetY);

            long startTime = System.currentTimeMillis();
            long timeoutMs = 60000;
            int failedMiniMapClicks = 0;
            Set<String> attemptedMiniMapLogicalPoints = new HashSet<>();

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");

                if (context.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
                    log.warn("navigate in current map interrupted by synced combat state: target=({}, {})",
                            targetX, targetY);
                    result = NavigationResult.interrupted("interrupted by synced combat state");
                    return result;
                }

                if (isCurrentCachedCoordinateNear(targetX, targetY, arrivalTolerance, "navigateInCurrentMap:cached")) {
                    result = NavigationResult.arrived("target coordinate reached by cached state");
                    return result;
                }

                /*
                 * Current-map callers now all use handoff semantics. Skip the heavy pre-click
                 * movement probe; after the mini-map click below, still prove that movement really
                 * started before registering a pathing intent and yielding the task turn.
                 */
                log.info("navigate in current map skips heavy pre-click movement probe: target=({}, {}) source={}",
                        targetX, targetY, request.getSource());

                CoordinateHelper.MiniMapClickPoint clickPoint = null;
                while (clickPoint == null && System.currentTimeMillis() - startTime < timeoutMs) {
                    CoordinateHelper.MiniMapClickPoint candidate = coordinateHelper.resolveMiniMapClickPoint(
                            mapName, targetX, targetY, failedMiniMapClicks,
                            request.isRandomizeMiniMapClickPoint(),
                            request.getMiniMapClickRandomRadiusPx());
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
                boolean immediateMiniMapFireAndHandoff = isImmediateMiniMapFireAndHandoff(request);
                MiniMapPathingAttemptResult attemptResult;
                if (immediateMiniMapFireAndHandoff) {
                    attemptResult = clickMiniMapPointForFireAndHandoff(
                            clickPoint, "navigateInCurrentMap:immediate-mini-map-fire-and-handoff", mapName);
                } else {
                    attemptResult = clickMiniMapPointForHandoff(
                            clickPoint, "navigateInCurrentMap:click", mapName, checkPanelBeforeOpen);
                }
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                if (attemptResult == MiniMapPathingAttemptResult.PATHING_STARTED) {
                    log.info("navigate in current map mini-map click started pathing: target=({}, {}) logicalClick=({}, {}) basePixel=({}, {}) actualPixel=({}, {}) jitter=({}, {}) reason={}",
                            targetX, targetY,
                            clickPoint.logicalX(), clickPoint.logicalY(),
                            clickPoint.basePixelPoint().x, clickPoint.basePixelPoint().y,
                            clickPoint.pixelPoint().x, clickPoint.pixelPoint().y,
                            clickPoint.jitterX(), clickPoint.jitterY(),
                            clickPoint.reason());
                    String pathingMessage = immediateMiniMapFireAndHandoff
                            ? "current-map mini-map click fire-and-handoff"
                            : "current-map mini-map click started pathing";
                    pathingIntentRegistered = registerWindowPathingIntent(
                            request, "navigateInCurrentMap", pathingMessage, true);
                    if (request.isKeepTurnOnCurrentMapPathing()) {
                        /*
                         * Short leader-only corrections should not yield. They are followed
                         * immediately by NPC/dialog work, so letting another window take the turn
                         * creates the exact post-return/heal-pet interleave we are avoiding.
                         */
                        log.info("navigate in current map keeps turn after short pathing: source={} target=({}, {}) tolerance={}",
                                request.getSource(), targetX, targetY, arrivalTolerance);
                        long keepTurnDeadline = System.currentTimeMillis() + Math.min(
                                10000L, Math.max(1000L, timeoutMs - (System.currentTimeMillis() - startTime)));
                        while (System.currentTimeMillis() < keepTurnDeadline) {
                            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "navigation interrupted");
                            if (isCurrentCachedCoordinateNear(targetX, targetY, arrivalTolerance,
                                    "navigateInCurrentMap:keepTurn")) {
                                result = NavigationResult.arrived("target coordinate reached after short pathing");
                                return result;
                            }
                            WindowPathingSnapshot snapshot = windowTaskContextHolder.rawCurrent()
                                    .map(WindowRuntimeContext::getPathingSnapshot)
                                    .orElse(null);
                            if (snapshot != null
                                    && snapshot.getIntent() != null
                                    && snapshot.getState() == WindowPathingState.STOPPED_AWAY) {
                                log.info("navigate in current map keep-turn pathing stopped away; retry foreground click: source={} current={}({}, {}) target=({}, {})",
                                        request.getSource(), snapshot.getCurrentMapName(),
                                        snapshot.getCurrentX(), snapshot.getCurrentY(), targetX, targetY);
                                break;
                            }
                            if (!TaskSleep.sleep(250)) {
                                result = NavigationResult.stopped("interrupted while waiting for short current-map pathing");
                                return result;
                            }
                        }
                        failedMiniMapClicks++;
                        log.info("navigate in current map keep-turn pathing did not arrive before retry: source={} target=({}, {}) failedClicks={}",
                                request.getSource(), targetX, targetY, failedMiniMapClicks);
                        continue;
                    }
                    result = NavigationResult.pathingStarted(pathingMessage);
                    return result;
                }
                if (immediateMiniMapFireAndHandoff) {
                    log.warn("immediate mini-map fire-and-handoff click failed; skip alternate mini-map retries: target=({}, {}) result={}",
                            targetX, targetY, attemptResult);
                    result = NavigationResult.pointNotReached("immediate mini-map fire-and-handoff click failed");
                    return result;
                }
                if (attemptResult == MiniMapPathingAttemptResult.NO_PATHING) {
                    if (context.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
                        log.warn("navigate in current map mini-map confirmation saw synced combat state; keep original click point: target=({}, {})",
                                targetX, targetY);
                        result = NavigationResult.interrupted("mini-map click interrupted by synced combat state");
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
            if (result.getStatus() == NavigationResultStatus.PATHING_STARTED && !pathingIntentRegistered) {
                registerWindowPathingIntent(request, "navigateInCurrentMap", result.getMessage(), true);
            }
            if (result.getStatus() == NavigationResultStatus.PATHING_STARTED) {
                log.info("navigate in current map already closed mini-map after confirmed handoff click: source={} result={}",
                        request.getSource(), result.getStatus());
            } else {
                closeMiniMapIfOpen("navigateInCurrentMap:finish");
            }
            LatencyMetrics.info(log, "navigation.currentMap", latencyStart,
                    "result=" + result.getStatus() + " source=" + request.getSource()
                            + " target=(" + targetX + "," + targetY + ")");
        }
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

    private RouteDialogClickResult consumePreparedRouteDialogAction(WindowRuntimeContext runtime,
                                                                    String targetMapName,
                                                                    String source,
                                                                    String reason,
                                                                    String fromMap,
                                                                    Integer fromX,
                                                                    Integer fromY,
                                                                    String memorySourceSuffix) {
        if (runtime == null || targetMapName == null || targetMapName.isBlank()) {
            return null;
        }
        PreparedDialogAction action = runtime.consumePreparedDialogActionValidated(
                DialogOperation.ROUTE_TRANSFER,
                targetMapName,
                reason,
                true,
                prepared -> dialogService.validatePreparedDialogActionForConsume(prepared, reason));
        if (action == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (!matchesCurrentPreparedDialogBinding(runtime, action)
                || !matchesActivePreparedRouteIntent(runtime, action)
                || !action.verifiedWithin(now, ROUTE_PREPARED_DIALOG_CLICK_MAX_AGE_MS)) {
            WindowPathingIntent activeIntent = runtime.getActivePathingIntent().orElse(null);
            WindowDialogSnapshot visibleSnapshot = runtime.getVisibleDialogSnapshot().orElse(null);
            log.warn("prepared route dialog consumed but invalid; skip click and continue fallback: source={} windowId={} title={} hwnd={} target={} actionIntentId={} activeIntentId={} actionWindow={} currentWindow={} sameBinding={} sameIntent={} visibleType={} visibleSource={} verifiedAgeMs={} maxAgeMs={} click=({}, {})",
                    source,
                    runtime.getWindowId(),
                    runtime.getNativeBinding() == null ? null : runtime.getNativeBinding().getTitle(),
                    runtime.getNativeBinding() == null ? null : runtime.getNativeBinding().getNativeHandle(),
                    targetMapName,
                    action.getIntentId(),
                    activeIntent == null ? null : activeIntent.getIntentId(),
                    action.getWindowId(),
                    runtime.getWindowId(),
                    matchesCurrentPreparedDialogBinding(runtime, action),
                    matchesActivePreparedRouteIntent(runtime, action),
                    visibleSnapshot == null ? null : visibleSnapshot.getType(),
                    visibleSnapshot == null ? null : visibleSnapshot.getSource(),
                    action.getLastVerifiedAtMs() <= 0 ? null : Math.max(0L, now - action.getLastVerifiedAtMs()),
                    ROUTE_PREPARED_DIALOG_CLICK_MAX_AGE_MS,
                    action.getAbsoluteX(),
                    action.getAbsoluteY());
            return null;
        }
        WindowPathingIntent activeIntent = runtime.getActivePathingIntent().orElse(null);
        WindowDialogSnapshot visibleSnapshot = runtime.getVisibleDialogSnapshot().orElse(null);
        Long visibleAgeMs = visibleSnapshot == null ? null : Math.max(0L, now - visibleSnapshot.getDetectedAtMs());
        boolean clearedIntentRecovery = action.getIntentId() != null && activeIntent == null;
        if (clearedIntentRecovery) {
            log.info("prepared-route-fresh-with-cleared-intent: source={} windowId={} title={} hwnd={} target={} actionIntentId={} activeIntentId=null preparedAgeMs={} verifiedAgeMs={} click=({}, {}) matched={}",
                    source,
                    runtime.getWindowId(),
                    runtime.getNativeBinding() == null ? null : runtime.getNativeBinding().getTitle(),
                    runtime.getNativeBinding() == null ? null : runtime.getNativeBinding().getNativeHandle(),
                    targetMapName,
                    action.getIntentId(),
                    action.getPreparedAtMs() <= 0 ? null : Math.max(0L, now - action.getPreparedAtMs()),
                    action.getLastVerifiedAtMs() <= 0 ? null : Math.max(0L, now - action.getLastVerifiedAtMs()),
                    action.getAbsoluteX(),
                    action.getAbsoluteY(),
                    action.getMatchedText());
        }
        log.info("route dialog uses consumed prepared action: source={} windowId={} title={} hwnd={} target={} actionIntentId={} activeIntentId={} actionSource={} visibleType={} visibleSource={} visibleAgeMs={} matched={} click=({}, {}) verifiedAgeMs={}",
                source,
                runtime.getWindowId(),
                runtime.getNativeBinding() == null ? null : runtime.getNativeBinding().getTitle(),
                runtime.getNativeBinding() == null ? null : runtime.getNativeBinding().getNativeHandle(),
                targetMapName,
                action.getIntentId(),
                activeIntent == null ? null : activeIntent.getIntentId(),
                action.getSource(),
                visibleSnapshot == null ? null : visibleSnapshot.getType(),
                visibleSnapshot == null ? null : visibleSnapshot.getSource(),
                visibleAgeMs,
                action.getMatchedText(),
                action.getAbsoluteX(),
                action.getAbsoluteY(),
                Math.max(0L, now - action.getLastVerifiedAtMs()));
        boolean clicked = inputSequences.moveAndClickLeft("navigation:preparedRouteDialog:" + targetMapName,
                action.getAbsoluteX(), action.getAbsoluteY(), 80, 150);
        if (!clicked) {
            log.warn("route dialog consumed prepared action click failed: source={} target={} click=({}, {})",
                    source, targetMapName, action.getAbsoluteX(), action.getAbsoluteY());
            runtime.clearDialogPreparationRequest("prepared route dialog click failed");
            return new RouteDialogClickResult(
                    DialogResultStatus.FAILED,
                    false,
                    fromMap,
                    fromX,
                    fromY,
                    targetMapName,
                    action.getRelativeX(),
                    action.getRelativeY(),
                    action.getMatchedText());
        }
        runtime.clearDialogPreparationRequest("prepared route dialog clicked");
        RouteDialogClickResult clickedResult = new RouteDialogClickResult(
                DialogResultStatus.OPTION_KEYWORD_CLICKED,
                false,
                fromMap,
                fromX,
                fromY,
                targetMapName,
                action.getRelativeX(),
                action.getRelativeY(),
                action.getMatchedText());
        rememberPendingRouteDialogClick(clickedResult, source + memorySourceSuffix);
        return clickedResult;
    }

    private boolean isPreparedRouteDialogActionUsable(WindowRuntimeContext runtime,
                                                      PreparedDialogAction action,
                                                      String targetMapName,
                                                      long nowMs) {
        return action != null
                && action.matches(DialogOperation.ROUTE_TRANSFER, targetMapName)
                && matchesCurrentPreparedDialogBinding(runtime, action)
                && matchesActivePreparedRouteIntent(runtime, action)
                && action.verifiedWithin(nowMs, ROUTE_PREPARED_DIALOG_CLICK_MAX_AGE_MS);
    }

    private boolean matchesActivePreparedRouteIntent(WindowRuntimeContext runtime, PreparedDialogAction action) {
        if (runtime == null || action == null || action.getIntentId() == null) {
            return true;
        }
        Optional<WindowPathingIntent> activeIntent = runtime.getActivePathingIntent();
        if (activeIntent.isPresent()) {
            return Objects.equals(activeIntent.get().getIntentId(), action.getIntentId());
        }
        return action.getOperation() == DialogOperation.ROUTE_TRANSFER;
    }

    private boolean shouldYieldForRouteDialogBeforeWorldMap(WindowRuntimeContext runtime,
                                                            String targetMapName,
                                                            String source,
                                                            String logSource) {
        if (runtime == null || targetMapName == null || targetMapName.isBlank()) {
            return false;
        }
        long now = System.currentTimeMillis();
        Optional<WindowDialogSnapshot> visible = runtime.getVisibleDialogSnapshot();
        WindowDialogSnapshot visibleSnapshot = visible.orElse(null);
        Long visibleAgeMs = visibleSnapshot == null
                ? null
                : Math.max(0L, now - visibleSnapshot.getDetectedAtMs());
        WindowPathingIntent activeIntent = runtime.getActivePathingIntent().orElse(null);
        boolean sameTargetIntent = activeIntent != null
                && gameStateUtil.isSameMapName(activeIntent.getTargetMapName(), targetMapName);
        boolean compatibleActiveIntent =
                isActivePathingIntentCompatibleWithRequest(activeIntent, targetMapName, source);
        WindowPathingSnapshot pathingSnapshot = runtime.getPathingSnapshot();
        WindowPathingState pathingState = pathingSnapshot == null ? null : pathingSnapshot.getState();
        Long pathingSnapshotAgeMs = pathingSnapshot == null || pathingSnapshot.getUpdatedAtMs() <= 0
                ? null
                : Math.max(0L, now - pathingSnapshot.getUpdatedAtMs());
        boolean freshActiveRoutePending = compatibleActiveIntent
                && isFreshRoutePendingForWorldMapGate(pathingSnapshot, activeIntent, now);
        boolean freshVisibleDialog = visibleSnapshot != null
                && visibleSnapshot.getType() != DialogType.NONE
                && visibleAgeMs != null
                && visibleAgeMs <= ROUTE_DIALOG_VISIBLE_GATE_MAX_AGE_MS
                && compatibleActiveIntent;

        DialogPreparationStatus status = runtime.getDialogPreparationStatus();
        boolean matchingStatus = status != null && status.matches(DialogOperation.ROUTE_TRANSFER, targetMapName);
        boolean freshRequested = matchingStatus
                && status.getPhase() == DialogPreparationPhase.REQUESTED
                && ageWithin(now, status.getRequestCreatedAtMs(), ROUTE_DIALOG_REQUESTED_YIELD_MAX_MS);
        boolean freshPreparing = matchingStatus
                && status.getPhase() == DialogPreparationPhase.PREPARING
                && ageWithin(now, status.getPreparingStartedAtMs(), ROUTE_DIALOG_PREPARING_YIELD_MAX_MS);

        PreparedDialogAction action = runtime.getPreparedDialogAction();
        boolean preparedUsable = isPreparedRouteDialogActionUsable(runtime, action, targetMapName, now);
        Optional<WindowReadyEvent> taskAttention = windowReadyEventBus.latest(
                runtime.getWindowId(), WindowReadyEventType.TASK_ATTENTION_REQUIRED);
        WindowReadyEvent taskAttentionEvent = taskAttention.orElse(null);
        Long taskAttentionAgeMs = taskAttentionEvent == null
                ? null
                : Math.max(0L, now - taskAttentionEvent.getCreatedAtMs());
        boolean freshTaskAttention = taskAttentionEvent != null
                && taskAttentionAgeMs != null
                && taskAttentionAgeMs <= ROUTE_DIALOG_ATTENTION_GATE_MAX_AGE_MS
                && compatibleActiveIntent;
        boolean gate = freshVisibleDialog
                || freshRequested
                || freshPreparing
                || preparedUsable
                || freshTaskAttention
                || freshActiveRoutePending;
        Long requestAgeMs = status == null || status.getRequestCreatedAtMs() <= 0
                ? null
                : Math.max(0L, now - status.getRequestCreatedAtMs());
        Long preparingAgeMs = status == null || status.getPreparingStartedAtMs() <= 0
                ? null
                : Math.max(0L, now - status.getPreparingStartedAtMs());
        Long preparedAgeMs = action == null || action.getPreparedAtMs() <= 0
                ? null
                : Math.max(0L, now - action.getPreparedAtMs());
        Long preparedVerifiedAgeMs = action == null || action.getLastVerifiedAtMs() <= 0
                ? null
                : Math.max(0L, now - action.getLastVerifiedAtMs());
        Long intentAgeMs = activeIntent == null || activeIntent.getCreatedAtMs() <= 0
                ? null
                : Math.max(0L, now - activeIntent.getCreatedAtMs());
        String visibleReason = visibleSnapshot == null
                ? "absent"
                : visibleAgeMs == null || visibleAgeMs > ROUTE_DIALOG_VISIBLE_GATE_MAX_AGE_MS
                ? "visible-stale"
                : !sameTargetIntent
                ? "visible-target-mismatch"
                : !compatibleActiveIntent
                ? "visible-source-mismatch"
                : visibleSnapshot.getType() == DialogType.OPTION
                ? "fresh-visible-option"
                : visibleSnapshot.getType() == DialogType.STORY
                ? "fresh-visible-story"
                : "fresh-visible-dialog";
        String attentionReason = taskAttentionEvent == null
                ? "absent"
                : taskAttentionAgeMs == null || taskAttentionAgeMs > ROUTE_DIALOG_ATTENTION_GATE_MAX_AGE_MS
                ? "attention-stale"
                : !sameTargetIntent
                ? "attention-target-mismatch"
                : !compatibleActiveIntent
                ? "attention-source-mismatch"
                : "fresh-attention";
        String pathingReason = pathingSnapshot == null
                ? "absent"
                : !sameTargetIntent
                ? "pathing-target-mismatch"
                : !compatibleActiveIntent
                ? "pathing-source-mismatch"
                : pathingState == WindowPathingState.NONE
                ? "pathing-none"
                : pathingState == WindowPathingState.ARRIVED
                ? "pathing-arrived"
                : pathingState == WindowPathingState.STOPPED_AWAY
                ? "pathing-stopped-away"
                : !freshActiveRoutePending
                ? "pathing-intent-stale"
                : pathingState == WindowPathingState.UNKNOWN
                ? "fresh-unknown-intent"
                : "fresh-active-intent";
        String statusReason = status == null
                ? "absent"
                : !matchingStatus
                ? "status-target-mismatch"
                : freshRequested
                ? "fresh-requested"
                : freshPreparing
                ? "fresh-preparing"
                : "status-stale";
        String preparedReason = action == null
                ? "absent"
                : !action.matches(DialogOperation.ROUTE_TRANSFER, targetMapName)
                ? "prepared-target-mismatch"
                : !matchesCurrentPreparedDialogBinding(runtime, action)
                ? "prepared-binding-mismatch"
                : !matchesActivePreparedRouteIntent(runtime, action)
                ? "prepared-intent-mismatch"
                : !action.verifiedWithin(now, ROUTE_PREPARED_DIALOG_CLICK_MAX_AGE_MS)
                ? "prepared-stale"
                : "prepared-usable";
        String gateReason = freshVisibleDialog
                ? "same-target-visible-dialog-yield"
                : freshRequested
                ? "same-target-dialog-requested-yield"
                : freshPreparing
                ? "same-target-dialog-preparing-yield"
                : preparedUsable
                ? "same-target-prepared-action-yield"
                : freshTaskAttention
                ? "same-target-task-attention-yield"
                : freshActiveRoutePending
                ? "same-target-active-intent-yield"
                : "allow-world-map-retry";
        log.info("route dialog world-map gate: result={} reason={} source={} windowId={} title={} hwnd={} target={} activeIntentId={} activeIntentTarget={} activeIntentSource={} intentAgeMs={} pathingReason={} pathingState={} snapshotAgeMs={} visibleReason={} visibleType={} visibleAgeMs={} visibleSource={} statusReason={} statusPhase={} statusTarget={} requestAgeMs={} preparingAgeMs={} preparedReason={} preparedIntentId={} preparedTarget={} preparedSource={} preparedAgeMs={} preparedVerifiedAgeMs={} preparedUsable={} attentionReason={} attentionAgeMs={} attentionSource={} sameTargetIntent={} compatibleActiveIntent={}",
                gate,
                gate ? gateReason : "allow-world-map-retry:" + visibleReason + "/" + statusReason + "/" + preparedReason + "/" + attentionReason + "/" + pathingReason,
                logSource,
                runtime.getWindowId(),
                runtime.getNativeBinding() == null ? null : runtime.getNativeBinding().getTitle(),
                runtime.getNativeBinding() == null ? null : runtime.getNativeBinding().getNativeHandle(),
                targetMapName,
                activeIntent == null ? null : activeIntent.getIntentId(),
                activeIntent == null ? null : activeIntent.getTargetMapName(),
                activeIntent == null ? null : activeIntent.getSource(),
                intentAgeMs,
                pathingReason,
                pathingState,
                pathingSnapshotAgeMs,
                visibleReason,
                visibleSnapshot == null ? null : visibleSnapshot.getType(),
                visibleAgeMs,
                visibleSnapshot == null ? null : visibleSnapshot.getSource(),
                statusReason,
                status == null ? null : status.getPhase(),
                status == null ? null : status.getTargetKeyword(),
                requestAgeMs,
                preparingAgeMs,
                preparedReason,
                action == null ? null : action.getIntentId(),
                action == null ? null : action.getTargetKeyword(),
                action == null ? null : action.getSource(),
                preparedAgeMs,
                preparedVerifiedAgeMs,
                preparedUsable,
                attentionReason,
                taskAttentionAgeMs,
                taskAttentionEvent == null ? null : taskAttentionEvent.getSource(),
                sameTargetIntent,
                compatibleActiveIntent);
        return gate;
    }

    private NavigationResult routeDialogGateBeforeWorldMap(String targetMapName,
                                                           String source,
                                                           String reason) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        RouteDialogClickResult consumed = consumePreparedRouteDialogAction(
                runtime,
                targetMapName,
                source,
                reason,
                null,
                null,
                null,
                ":world-map-gate");
        if (consumed != null) {
            if (consumed.result() == DialogResultStatus.OPTION_KEYWORD_CLICKED) {
                return NavigationResult.pathingStarted("route dialog clicked before world-map search");
            }
            if (consumed.result() == DialogResultStatus.FAILED) {
                return NavigationResult.mapNotReached("route dialog prepared action click failed before world-map search");
            }
        }
        if (shouldYieldForRouteDialogBeforeWorldMap(runtime, targetMapName, source, source + ":" + reason)) {
            if (isFreshSameTargetRoutePending(runtime, targetMapName, source, System.currentTimeMillis(), true)) {
                return NavigationResult.pathingStarted("same target route already submitted before world-map search; watcher will confirm pathing");
            }
            return NavigationResult.dialogPreparing("route dialog visible/preparing or route intent pending before world-map search");
        }
        return null;
    }

    private boolean isFreshSameTargetRoutePending(WindowRuntimeContext runtime,
                                                   String targetMapName,
                                                   String requestSource,
                                                   long now,
                                                   boolean requireActiveState) {
        if (runtime == null || targetMapName == null || targetMapName.isBlank()) {
            return false;
        }
        WindowPathingSnapshot snapshot = runtime.getPathingSnapshot();
        WindowPathingIntent intent = runtime.getActivePathingIntent().orElse(null);
        if (!isActivePathingIntentCompatibleWithRequest(intent, targetMapName, requestSource)) {
            return false;
        }
        if (requireActiveState && (snapshot == null || snapshot.getState() != WindowPathingState.ACTIVE)) {
            return false;
        }
        return isFreshRoutePendingForWorldMapGate(snapshot, intent, now);
    }

    private boolean isFreshRoutePendingForWorldMapGate(WindowPathingSnapshot snapshot,
                                                       WindowPathingIntent intent,
                                                       long now) {
        if (snapshot == null || intent == null) {
            return false;
        }
        WindowPathingState state = snapshot.getState();
        if (state == null
                || state == WindowPathingState.NONE
                || state == WindowPathingState.ARRIVED
                || state == WindowPathingState.STOPPED_AWAY) {
            return false;
        }
        long maxAgeMs = state == WindowPathingState.UNKNOWN
                ? ROUTE_DIALOG_UNKNOWN_INTENT_GATE_MAX_AGE_MS
                : ROUTE_DIALOG_ACTIVE_INTENT_GATE_MAX_AGE_MS;
        boolean freshIntent = ageWithin(now, intent.getCreatedAtMs(), maxAgeMs);
        boolean freshSnapshot = snapshot.getUpdatedAtMs() <= 0
                || now - snapshot.getUpdatedAtMs() <= maxAgeMs;
        return freshIntent && freshSnapshot;
    }

    private boolean ageWithin(long nowMs, long timestampMs, long maxAgeMs) {
        return timestampMs > 0 && maxAgeMs >= 0 && nowMs - timestampMs <= maxAgeMs;
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

    private void rememberPendingRouteDialogClick(RouteDialogClickResult routeDialog, String source) {
        if (routeDialog == null || routeDialog.relativeX() == null || routeDialog.relativeY() == null) {
            return;
        }
        if (routeDialog.result() != DialogResultStatus.OPTION_KEYWORD_CLICKED
                && routeDialog.result() != DialogResultStatus.FALLBACK_CLICKED) {
            return;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return;
        }
        runtime.updatePendingTransferChoiceMemory(PendingTransferChoiceMemory.builder()
                .fromMap(routeDialog.fromMap())
                .fromX(routeDialog.fromX())
                .fromY(routeDialog.fromY())
                .targetMap(routeDialog.targetMap())
                .relativeX(routeDialog.relativeX())
                .relativeY(routeDialog.relativeY())
                .optionText(routeDialog.optionText())
                .source(source)
                .createdAtMs(System.currentTimeMillis())
                .build());
        log.info("[dialog-choice-memory] pending route click: source={} from={}({}, {}) target={} rel=({}, {}) option={}",
                source, routeDialog.fromMap(), routeDialog.fromX(), routeDialog.fromY(),
                routeDialog.targetMap(), routeDialog.relativeX(), routeDialog.relativeY(), routeDialog.optionText());
    }

    private boolean performWorldMapSearchAndClickDestination(String fromMapName,
                                                             String targetMapName,
                                                             String canonicalTargetMapName,
                                                             NavigationRequest request) {
        long totalStartMs = System.currentTimeMillis();
        state().clearWorldMapRouteResultClick();
        if (request == null || request.getTargetX() == null || request.getTargetY() == null) {
            log.warn("navigation world-map route rejected: final mini-map coordinate is required after legacy green route removal target={} request={}",
                    targetMapName, request);
            return false;
        }
        String routeMode = "yellow-destination-mini-map";
        for (int attempt = 1; attempt <= 2; attempt++) {
            final int currentAttempt = attempt;
            final String currentTargetMapName = targetMapName;
            long prepareStartMs = System.currentTimeMillis();
            boolean prepared = inputSequences.submitExclusiveAndWait(
                    "submitWorldMapSearchAndClickDestination:prepare:" + currentTargetMapName + ":attempt" + currentAttempt,
                    () -> prepareWorldMapSearchResultsDirect(currentTargetMapName, currentAttempt));
            long prepareElapsedMs = System.currentTimeMillis() - prepareStartMs;
            log.info("navigation map search split: stage=prepare target={} attempt={}/{} elapsedMs={}",
                    currentTargetMapName, currentAttempt, 2, prepareElapsedMs);
            if (!prepared) {
                return false;
            }

            long scanStartMs = System.currentTimeMillis();
            WorldMapDestinationClickResult status = clickYellowDestinationAndTargetMiniMap(
                    "submitWorldMapSearchAndClickDestination:yellowDestinationMiniMap",
                    targetMapName,
                    request);
            long scanElapsedMs = System.currentTimeMillis() - scanStartMs;
            log.info("navigation map search split: stage={} target={} attempt={}/{} status={} elapsedMs={} totalMs={}",
                    routeMode,
                    currentTargetMapName, currentAttempt, 2, status, scanElapsedMs,
                    System.currentTimeMillis() - totalStartMs);
            boolean routeClicked = status == WorldMapDestinationClickResult.CLICKED;
            log.info("navigation map search: route action result={} status={} attempt={}/{} mode={}",
                    routeClicked, status, attempt, 2,
                    routeMode);
            if (routeClicked) {
                return true;
            }
            if (status == WorldMapDestinationClickResult.WRONG_DESTINATION && attempt < 2) {
                closeRouteSearchPanelQueued("submitWorldMapSearchAndClickDestination:destinationMismatch:attempt" + attempt);
                if (!TaskSleep.sleep(250)) {
                    return false;
                }
                continue;
            }
            closeRouteSearchPanelQueued("submitWorldMapSearchAndClickDestination:failed");
            return false;
        }
        return false;
    }

    private WorldMapDestinationClickResult executeCloudRouteCandidateClick(String description,
                                                                           RouteCloudDecision routeDecision,
                                                                           String routeMode,
                                                                           String candidateSource,
                                                                           String expectedDestinationName,
                                                                           NavigationRequest request) {
        if (routeDecision == null || !routeDecision.isCloudExecuted()) {
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        Point cloudRelativeClick = routeDecision.getCloudWindowRelativeClickPoint();
        if (cloudRelativeClick == null) {
            return WorldMapDestinationClickResult.NOT_FOUND;
        }

        int cloudClickX = tracker.getWindowBaseX() + cloudRelativeClick.x;
        int cloudClickY = tracker.getWindowBaseY() + cloudRelativeClick.y;
        log.info("navigation route candidate cloud execute: target={} mode={} candidateSource={} "
                        + "cloudRelative=({}, {}) cloudAbs=({}, {}) localShadowDecision={}",
                expectedDestinationName, routeMode, candidateSource,
                cloudRelativeClick.x, cloudRelativeClick.y, cloudClickX, cloudClickY,
                routeDecision.getLocalShadowDecision());
        if ("yellow-destination-mini-map".equals(routeMode)) {
            return executeCloudYellowDestinationRouteCandidateClick(
                    description, expectedDestinationName, request, cloudRelativeClick, cloudClickX, cloudClickY,
                    routeDecision.getRouteDecisionId());
        }
        log.warn("navigation route candidate cloud execute skipped: unsupported routeMode={} target={} localShadowDecision={}",
                routeMode, expectedDestinationName, routeDecision.getLocalShadowDecision());
        return WorldMapDestinationClickResult.NOT_FOUND;
    }

    private WorldMapDestinationClickResult executeCloudYellowDestinationRouteCandidateClick(
            String description,
            String expectedDestinationName,
            NavigationRequest request,
            Point cloudRelativeClick,
            int cloudClickX,
            int cloudClickY,
            String routeDecisionId) {
        if (request == null || request.getTargetX() == null || request.getTargetY() == null) {
            log.warn("navigation cloud yellow route skipped: final coordinate missing request={} target={}",
                    request, expectedDestinationName);
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        CoordinateHelper.MiniMapClickPoint miniMapClickPoint = coordinateHelper.resolveMiniMapClickPoint(
                expectedDestinationName,
                request.getTargetX(),
                request.getTargetY(),
                0,
                request.isRandomizeMiniMapClickPoint());
        if (miniMapClickPoint == null) {
            log.warn("navigation cloud yellow route skipped: mini-map coordinate mapping missing target={} final=({}, {})",
                    expectedDestinationName, request.getTargetX(), request.getTargetY());
            return WorldMapDestinationClickResult.NOT_FOUND;
        }

        /*
         * Cloud candidate click owns the yellow-destination decision and keeps the established
         * destination mini-map handoff:
         * once the final destination mini-map point is clicked, register movement and run the
         * task-scoped route UI cleanup policy below.
         */
        boolean submitted = inputSequences.submitExclusiveAndWait(description, () -> {
            if (InputActionScope.isCancelled()) {
                log.info("navigation cloud yellow route click skipped because input request was cancelled: target={}",
                        expectedDestinationName);
                return false;
            }
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.clickLeft(cloudClickX, cloudClickY, 150);
            if (!TaskSleep.sleep(MINI_MAP_OPEN_SETTLE_MS) || InputActionScope.isCancelled()) {
                return false;
            }
            if (InputActionScope.isCancelled() || !InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.clickLeft(miniMapClickPoint.pixelPoint().x, miniMapClickPoint.pixelPoint().y, 200);
            if (!TaskSleep.sleep(MINI_MAP_CLICK_SETTLE_MS)) {
                return false;
            }
            return true;
        });
        if (!submitted) {
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        gameStateUtil.recordMovementIntent(description);
        cleanupYellowDestinationRouteAfterFireAndHandoff(request, description + ":cloud-yellow-route");
        log.info("navigation cloud yellow route: final mini-map coordinate fire-and-handoff target={} logical=({}, {}) pixel=({}, {})",
                expectedDestinationName,
                miniMapClickPoint.logicalX(), miniMapClickPoint.logicalY(),
                miniMapClickPoint.pixelPoint().x, miniMapClickPoint.pixelPoint().y);
        NavigationRuntimeState state = state();
        state.lastAbsoluteLogicalX = cloudClickX;
        state.lastAbsoluteLogicalY = cloudClickY;
        state.lastWorldMapRouteRelativeX = cloudRelativeClick.x;
        state.lastWorldMapRouteRelativeY = cloudRelativeClick.y;
        state.lastWorldMapRouteMatchedText = expectedDestinationName;
        state.lastWorldMapRouteUsedMemory = false;
        state.lastWorldMapRouteDecisionId = routeDecisionId;
        return WorldMapDestinationClickResult.CLICKED;
    }

    private boolean prepareWorldMapSearchResultsDirect(String targetMapName, int attempt) {
        log.info("navigation map search start: target={} attempt={}/{}", targetMapName, attempt, 2);
        if (InputActionScope.isCancelled()) {
            log.info("navigation map search cancelled before input: target={} attempt={}/{}", targetMapName, attempt, 2);
            return false;
        }
        if (!isWorldMapTitleVisible()) {
            log.info("navigation map search: world map not open, press Alt+2");
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.pressAlt2();
            if (!TaskSleep.sleep(500) || InputActionScope.isCancelled()) {
                return false;
            }
        }

        boolean useOpenRoutePanel = false;
        Point xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(XUNLU_TEMPLATE_PATH, THRESHOLD_NORMAL);
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
            log.info("navigation map search: world map closed before attempt, press Alt+2 target={}", targetMapName);
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.pressAlt2();
            if (!TaskSleep.sleep(500) || InputActionScope.isCancelled()) {
                return false;
            }
            xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(XUNLU_TEMPLATE_PATH, THRESHOLD_NORMAL);
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
            if (InputActionScope.isCancelled() || !InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.clickLeft(targetInputX, targetInputY, 80);
            if (InputActionScope.isCancelled()) {
                return false;
            }
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.pressCtrlA();
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.typeTextUnicode(targetMapName);
            if (!TaskSleep.sleep(200) || InputActionScope.isCancelled()) {
                return false;
            }
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.clickLeft(searchButtonX, searchButtonY, 120);
            if (!TaskSleep.sleep(200) || InputActionScope.isCancelled()) {
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
            if (InputActionScope.isCancelled() || !InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.clickLeft(xunluPoint.x, xunluPoint.y, 120);
            if (!TaskSleep.sleep(250) || InputActionScope.isCancelled()) {
                return false;
            }
            closeWorldMapAfterXunluDirect(targetMapName, attempt);
            if (InputActionScope.isCancelled()) {
                return false;
            }
            log.info("navigation map search: type target map={} attempt={}/{}", targetMapName, attempt, 2);
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.typeTextUnicode(targetMapName);
            if (!TaskSleep.sleep(WORLD_MAP_SEARCH_TYPE_SETTLE_MS) || InputActionScope.isCancelled()) {
                return false;
            }
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.pressEnter();
        }
        if (InputActionScope.isCancelled()) {
            return false;
        }
        return scrollWorldMapSearchResultsToBottomDirect(scrollFocusX, scrollFocusY,
                "submitWorldMapSearchAndClickDestination:" + targetMapName + ":attempt" + attempt);
    }

    private NavigationResult submitWorldMapSearchAndClickDestination(NavigationRequest request,
                                                                     String targetMapName,
                                                                     String source) {
        NavigationResult gateResult = routeDialogGateBeforeWorldMap(
                targetMapName, source, "submitWorldMapSearchAndClickDestination:before-open");
        if (gateResult != null) {
            return gateResult;
        }
        String fromMapName = canonicalCurrentMapForWorldMapRouteMemory(source);
        String canonicalTargetMapName = canonicalMapName(targetMapName, "world-map-route-memory:target:" + source);
        boolean clicked = performWorldMapSearchAndClickDestination(fromMapName, targetMapName, canonicalTargetMapName, request);
        if (!clicked) {
            return NavigationResult.mapNotReached("world-map route submit failed");
        }
        registerWindowPathingIntent(request, "worldMapYellowDestinationMiniMap",
                source + ":yellow-destination-mini-map-pathing-confirmed", true);
        rememberPendingRouteOutcome(fromMapName, canonicalTargetMapName, source,
                WorldMapRouteResultMode.YELLOW_DESTINATION_MINI_MAP);
        return NavigationResult.pathingStarted("world-map yellow destination mini-map pathing confirmed");
    }

    private void rememberPendingRouteOutcome(String fromMapName,
                                             String targetMapName,
                                             String source,
                                             WorldMapRouteResultMode routeMode) {
        NavigationRuntimeState state = state();
        if (state.lastWorldMapRouteRelativeX == null || state.lastWorldMapRouteRelativeY == null) {
            return;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return;
        }
        WindowPathingIntent intent = runtime.getActivePathingIntent().orElse(null);
        if (intent == null) {
            return;
        }
        String from = normalizeNullable(fromMapName);
        String target = normalizeNullable(targetMapName);
        if (from == null || target == null) {
            log.info("[route-outcome] pending skipped: reason=blank-map fromMap={} targetMap={} intentId={} source={}",
                    from, target, intent.getIntentId(), source);
            return;
        }
        PendingRouteOutcome pending = PendingRouteOutcome.builder()
                .fromMap(from)
                .targetMap(target)
                .routeMode(routeMode)
                .relativeX(state.lastWorldMapRouteRelativeX)
                .relativeY(state.lastWorldMapRouteRelativeY)
                .matchedText(state.lastWorldMapRouteMatchedText)
                .source(source)
                .usedMemory(state.lastWorldMapRouteUsedMemory)
                .routeDecisionId(state.lastWorldMapRouteDecisionId)
                .intentId(intent.getIntentId())
                .createdAtMs(System.currentTimeMillis())
                .build();
        PendingRouteOutcome previous = runtime.getPendingRouteOutcome();
        if (previous != null && !Objects.equals(previous.getIntentId(), pending.getIntentId())) {
            log.info("[route-outcome] supersede live pending outcome through runner owner: previousIntentId={} nextIntentId={} previousTarget={} nextTarget={}",
                    previous.getIntentId(), pending.getIntentId(), previous.getTargetMap(), pending.getTargetMap());
        }
        runtime.requestPendingRouteOutcomeReplacement(pending, "second-navigation");
        log.info("[route-outcome] pending created: routeMode={} fromMap={} targetMap={} rel=({}, {}) matchedText={} usedMemory={} routeDecisionId={} intentId={} source={}",
                pending.getRouteMode(), pending.getFromMap(), pending.getTargetMap(),
                pending.getRelativeX(), pending.getRelativeY(),
                pending.getMatchedText(), pending.isUsedMemory(), pending.getRouteDecisionId(),
                pending.getIntentId(), pending.getSource());
    }

    private void closeWorldMapAfterXunluDirect(String targetMapName, int attempt) {
        /*
         * The xunlu template can only be clicked while the Alt+2 world map is visible. Close that
         * backing map immediately after the route panel is opened; waiting until the route link click
         * is racy because the game may auto-close the map first and a late Alt+2 would reopen it.
         */
        if (!InputActionScope.checkpoint()) {
            return;
        }
        inputProvider.pressAlt2();
        TaskSleep.sleep(250);
        log.info("navigation map search: close world map immediately after xunlu click target={} attempt={}/{}",
                targetMapName, attempt, 2);
    }

    private WorldMapDestinationClickResult clickYellowDestinationAndTargetMiniMap(String description,
                                                                                  String expectedDestinationName,
                                                                                  NavigationRequest request) {
        if (request == null || request.getTargetX() == null || request.getTargetY() == null) {
            log.warn("navigation yellow route skipped: final coordinate missing request={} target={}",
                    request, expectedDestinationName);
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        int[] mapRect = coordinateHelper.getScaledRect(
                config.getAnchor_windowTo_map_search_X(), config.getAnchor_windowTo_map_search_Y(),
                MAP_SEARCH_RECT_WIDTH, MAP_SEARCH_RECT_HEIGHT);

        String mapResultImagePath = windowScopedTempPath.resolve("map_result_scan.png");
        log.info("navigation yellow route: scan result image={} rect=({}, {})-({}, {}) target={} final=({}, {})",
                mapResultImagePath, mapRect[0], mapRect[1], mapRect[2], mapRect[3],
                expectedDestinationName, request.getTargetX(), request.getTargetY());
        if (!tracker.captureToFile("map result", mapResultImagePath, mapRect[0], mapRect[1], mapRect[2], mapRect[3])) {
            log.warn("navigation yellow route: map result capture failed target={}", expectedDestinationName);
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        if (InputActionScope.isCancelled()) {
            log.info("navigation yellow route cancelled after result capture: target={}", expectedDestinationName);
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        RouteResultImagePayload routeResultPayload = routeResultImagePayload(mapResultImagePath, mapRect);
        if (routeResultPayload == null) {
            return WorldMapDestinationClickResult.NOT_FOUND;
        }
        String requestSource = requestSource(request);
        RouteCloudDecision routeDecision = decideRouteCandidateClick(
                requestSource,
                "world-map-route-result",
                canonicalCurrentMapForWorldMapRouteMemory(requestSource),
                expectedDestinationName,
                canonicalMapName(expectedDestinationName, "world-map-route-candidate:target:" + requestSource),
                "yellow-destination-mini-map",
                "cloud-primary-route-result",
                WorldMapDestinationClickResult.NOT_FOUND,
                0,
                null,
                null,
                "cloud-primary-route-result",
                routeResultPayload,
                request);
        WorldMapDestinationClickResult status = executeCloudRouteCandidateClick(
                description, routeDecision, "yellow-destination-mini-map", "cloud-primary-route-result",
                expectedDestinationName, request);
        if (status != WorldMapDestinationClickResult.CLICKED) {
            log.warn("navigation cloud-primary yellow route did not click: target={} status={} decisionStatus={} localShadowDecision={} reason={}",
                    expectedDestinationName,
                    status,
                    routeDecision == null ? null : routeDecision.getStatus(),
                    routeDecision == null ? null : routeDecision.getLocalShadowDecision(),
                    routeDecision == null ? "missing route decision" : routeDecision.getRejectReason());
            return status;
        }
        return WorldMapDestinationClickResult.CLICKED;
    }

    private boolean hasFreshCurrentLocationForMapGuard(NavigationRequest request) {
        if (request == null || request.getFreshCurrentMapName() == null || request.getFreshCurrentMapName().isBlank()) {
            return false;
        }
        if (request.isFreshCurrentLocationPhaseBound()) {
            return true;
        }
        long capturedAtMs = request.getFreshCurrentLocationAtMs();
        return capturedAtMs > 0L && System.currentTimeMillis() - capturedAtMs <= 3000L;
    }

    private void cleanupYellowDestinationRouteQueued(String source) {
        boolean submitted = inputSequences.submitExclusiveAndWait(
                source + ":cleanup-yellow-destination-route",
                () -> {
                    cleanupYellowDestinationRouteAfterCoordinateClickDirect(source);
                    return true;
                });
        log.info("navigation yellow route cleanup submitted: source={} submitted={}", source, submitted);
    }

    private void cleanupYellowDestinationRouteAfterFireAndHandoff(NavigationRequest request, String source) {
        if (shouldDeferYellowDestinationRoutePanelCleanup(request)) {
            submitMiniMapCloseAfterFireAndHandoff(source);
            return;
        }
        cleanupYellowDestinationRouteQueued(source);
    }

    private boolean shouldDeferYellowDestinationRoutePanelCleanup(NavigationRequest request) {
        return "wuhuan_v2".equals(navigationTaskCode(request, null));
    }

    private void cleanupYellowDestinationRouteAfterCoordinateClickDirect(String source) {
        /*
         * This method runs inside a queued exclusive input callback. Keep the actual cleanup direct
         * here; callers outside the worker must use cleanupYellowDestinationRouteQueued(...).
         */
        if (isMiniMapPanelVisible(source + ":mini-map-before-close", true)) {
            if (pressAlt1ForMiniMap(source + ":close-destination-mini-map")) {
                TaskSleep.sleep(300);
                if (isMiniMapPanelVisible(source + ":mini-map-close-check", true)) {
                    log.warn("navigation yellow route cleanup: mini-map still visible after close, retry once source={}",
                            source);
                    if (pressAlt1ForMiniMap(source + ":close-destination-mini-map-retry")) {
                        TaskSleep.sleep(300);
                    }
                }
            } else {
                log.warn("navigation yellow route cleanup: failed to send mini-map close shortcut source={}", source);
            }
        }
        closeMapSearchInputAfterRouteClick(source + ":closeRoutePanel");
        if (isWorldMapTitleVisible()) {
            if (!InputActionScope.checkpoint()) {
                return;
            }
            inputProvider.pressAlt2();
            TaskSleep.sleep(250);
            log.info("navigation yellow route cleanup: closed remaining world map source={}", source);
        }
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

    private void closeRouteSearchPanelQueued(String source) {
        inputSequences.submitExclusiveAndWait("navigation:routePanelCleanup:" + source,
                () -> {
                    if (InputActionScope.isCancelled()) {
                        log.info("navigation map search cleanup skipped because input request was cancelled: source={}",
                                source);
                        return false;
                    }
                    closeMapSearchInputAfterRouteClick(source);
                    return true;
                });
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
        if (!InputActionScope.checkpoint()) {
            return;
        }
        inputProvider.moveMouse(x, y);
        log.info("navigation map search: mouse moved away after x2 close source={} point=({}, {})", source, x, y);
    }

    private boolean isWorldMapTitleVisible() {
        return coordinateHelper.findImageAbsoluteCoordinate(WORLD_MAP_TITLE_TEMPLATE_PATH, THRESHOLD_NORMAL) != null;
    }

    private boolean isMiniMapPanelVisible() {
        return isMiniMapPanelVisible("unspecified", false);
    }

    private boolean isMiniMapPanelVisible(String source, boolean saveDebugSnapshot) {
        int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
        if (saveDebugSnapshot) {
            return isMiniMapPanelVisibleWithDebug(source, rect);
        }
        return coordinateHelper.findImageInRegion(MINI_MAP_PANEL_CHECKED_TEMPLATE, rect, 0.95) != null
                || coordinateHelper.findImageInRegion(MINI_MAP_PANEL_UNCHECKED_TEMPLATE, rect, 0.95) != null;
    }

    private boolean isMiniMapPanelVisibleWithDebug(String source, int[] rect) {
        String timestamp = LocalDateTime.now().format(FAILURE_CASE_TIME_FORMAT);
        String roiPath = windowScopedTempPath.resolve("mini_map_panel_visible_check_" + timestamp + "_roi.png");
        if (!tracker.captureToFile("mini-map-panel-visible-debug", roiPath, rect[0], rect[1], rect[2], rect[3])) {
            log.warn("mini-map panel visible debug capture failed: source={} roi={} rect=({}, {})-({}, {})",
                    source, roiPath, rect[0], rect[1], rect[2], rect[3]);
            return false;
        }

        double[] checkedMatch = findMiniMapPanelTemplateForDebug(roiPath, MINI_MAP_PANEL_CHECKED_TEMPLATE, source);
        double[] uncheckedMatch = findMiniMapPanelTemplateForDebug(roiPath, MINI_MAP_PANEL_UNCHECKED_TEMPLATE, source);
        double[] matched = checkedMatch != null ? checkedMatch : uncheckedMatch;
        String template = checkedMatch != null ? MINI_MAP_PANEL_CHECKED_TEMPLATE : MINI_MAP_PANEL_UNCHECKED_TEMPLATE;
        if (matched == null) {
            log.info("mini-map panel visible debug miss: source={} roi={} rect=({}, {})-({}, {})",
                    source, roiPath, rect[0], rect[1], rect[2], rect[3]);
            return false;
        }

        Point absolute = coordinateHelper.resolveMatchedPointInRect(rect, matched);
        log.warn("mini-map panel visible debug matched: source={} template={} roi={} rect=({}, {})-({}, {}) local=({}, {}) absolute=({}, {})",
                source, template, roiPath, rect[0], rect[1], rect[2], rect[3],
                Math.round(matched[0]), Math.round(matched[1]), absolute.x, absolute.y);
        return true;
    }

    private double[] findMiniMapPanelTemplateForDebug(String roiPath, String templatePath, String source) {
        try {
            return ImageFinder.find(roiPath, templatePath, 0.95);
        } catch (Throwable e) {
            log.warn("mini-map panel visible debug match failed: source={} roi={} template={} error={}:{}",
                    source, roiPath, templatePath, e.getClass().getName(), e.getMessage());
            return null;
        }
    }

    private boolean scrollWorldMapSearchResultsToBottomDirect(int targetX, int targetY, String source) {
        log.info("navigation map search: force scroll to bottom source={} focus=({}, {}) attempts={} units={}",
                source, targetX, targetY, MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS, MAP_RESULT_SCROLL_DOWN_UNITS);
        if (InputActionScope.isCancelled()) {
            log.info("navigation map search scroll skipped because input request was cancelled: source={}", source);
            return false;
        }
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(targetX, targetY, 50);
        for (int i = 1; i <= MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS; i++) {
            if (InputActionScope.isCancelled()) {
                log.info("navigation map search scroll cancelled: source={} attempt={}/{}",
                        source, i, MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS);
                return false;
            }
            if (!InputActionScope.checkpoint()) {
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
                if (!pressAlt1ForMiniMap(description + ":open")) {
                    return false;
                }
                if (!TaskSleep.sleep(MINI_MAP_OPEN_SETTLE_MS)) {
                    return false;
                }
                log.info("mini-map Alt+1 open assumed before coordinate click: source={} checkPanelBeforeOpen={}",
                        description, checkPanelBeforeOpen);
            }

            if (InputActionScope.isCancelled() || !InputActionScope.checkpoint()) {
                return false;
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
                if (!pressAlt1ForMiniMap(description + ":close")) {
                    return false;
                }
                if (!TaskSleep.sleep(300)) {
                    return false;
                }
                if (isMiniMapPanelVisible()) {
                    log.warn("mini-map panel remained visible after close, pressing Alt+1 once more: source={}",
                            description);
                    if (!pressAlt1ForMiniMap(description + ":close-retry")) {
                        return false;
                    }
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
            if (!pressAlt1ForMiniMap(source + ":close")) {
                return false;
            }
            if (!TaskSleep.sleep(300)) {
                return false;
            }
            if (isMiniMapPanelVisible()) {
                log.warn("mini-map panel remained visible after finish close, pressing Alt+1 once more: source={}",
                        source);
                if (!pressAlt1ForMiniMap(source + ":close-retry")) {
                    return false;
                }
                return TaskSleep.sleep(300);
            }
            return true;
        });
    }

    private boolean pressAlt1ForMiniMap(String source) {
        BoundWindowKeyboardService.ShortcutAttempt attempt =
                boundWindowKeyboardService.pressShortcut(BoundWindowKeyboardService.AltShortcut.ALT_1);
        if (attempt.attempted() && attempt.success()) {
            log.info("mini-map Alt+1 sent through HWND keyboard: source={}", source);
            return true;
        }
        if (attempt.terminalFailure()) {
            log.warn("mini-map HWND Alt+1 terminally rejected; skip focused input fallback: source={} reason={}",
                    source, attempt.reason());
            return false;
        }
        if (attempt.attempted()) {
            log.warn("mini-map HWND Alt+1 failed, fallback to focused input: source={} reason={}",
                    source, attempt.reason());
        } else {
            log.info("mini-map HWND Alt+1 not attempted, fallback to focused input: source={} reason={}",
                    source, attempt.reason());
        }
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.pressAlt1();
        return true;
    }

    private boolean isImmediateMiniMapFireAndHandoff(NavigationRequest request) {
        if (request == null || request.getTargetX() == null || request.getTargetY() == null) {
            return false;
        }
        /*
         * Only the discussed current-map routes may skip foreground movement proof. The mini-map
         * coordinate conversion and click point are still produced by the existing click resolver.
         */
        if ("xiuluo-v2:start-exit-prepath:currentMap".equals(request.getSource())) {
            return gameStateUtil.isSameMapName(request.getTargetMapName(), MAP_LING_SHOU_VILLAGE)
                    && request.getTargetX() == 11
                    && request.getTargetY() == 8;
        }
        if ("wuhuan-v2:acceptNpc:navigate:currentMap".equals(request.getSource())) {
            return gameStateUtil.isSameMapName(request.getTargetMapName(), MAP_CHANG_AN)
                    && request.getTargetX() == 87
                    && request.getTargetY() == 174;
        }
        if ("wuhuan-v2:shoe-shop-entry-exact-130-130:currentMap".equals(request.getSource())) {
            return gameStateUtil.isSameMapName(request.getTargetMapName(), MAP_CHANG_AN)
                    && request.getTargetX() == 130
                    && request.getTargetY() == 130;
        }
        return false;
    }

    private MiniMapPathingAttemptResult clickMiniMapPointForFireAndHandoff(CoordinateHelper.MiniMapClickPoint clickPoint,
                                                                           String description,
                                                                           String expectedMapName) {
        if (clickPoint == null) {
            return MiniMapPathingAttemptResult.INCONCLUSIVE;
        }
        String source = description + ":" + clickPoint.reason()
                + ":logical=(" + clickPoint.logicalX() + "," + clickPoint.logicalY() + ")"
                + ":pixel=(" + clickPoint.pixelPoint().x + "," + clickPoint.pixelPoint().y + ")"
                + ":jitter=(" + clickPoint.jitterX() + "," + clickPoint.jitterY() + ")";
        log.info("immediate mini-map fire-and-handoff click: source={} expectedMap={} pixel=({}, {})",
                source, expectedMapName, clickPoint.pixelPoint().x, clickPoint.pixelPoint().y);
        boolean submitted = submitMiniMapClick(clickPoint.pixelPoint(), source, false, false);
        if (!submitted) {
            log.warn("immediate mini-map fire-and-handoff click input failed: source={} pixel=({}, {})",
                    source, clickPoint.pixelPoint().x, clickPoint.pixelPoint().y);
            closeMiniMapAfterFireAndHandoff(source + ":after-failed-click");
            return MiniMapPathingAttemptResult.INCONCLUSIVE;
        }
        gameStateUtil.recordMovementIntent(source);
        closeMiniMapAfterFireAndHandoff(source);
        log.info("immediate mini-map fire-and-handoff completed before movement proof: source={} expectedMap={}",
                source, expectedMapName);
        return MiniMapPathingAttemptResult.PATHING_STARTED;
    }

    private void closeMiniMapAfterFireAndHandoff(String source) {
        submitMiniMapCloseAfterFireAndHandoff(source);
    }

    private void submitMiniMapCloseAfterFireAndHandoff(String source) {
        /*
         * Fire-and-handoff navigation has already clicked the final mini-map coordinate. Alt+1 is a
         * cheap cleanup action and is safe to enqueue without making the current task wait. This path
         * is reserved for tasks, currently 五环, that intentionally keep the route panel for their
         * own later cleanup instead of closing the input panel in generic navigation.
         */
        boolean closeSubmitted = inputSequences.submit(
                source + ":close-mini-map-fire-and-handoff",
                java.util.List.of(InputAction.pressAlt1(), InputAction.sleep(300)));
        log.info("mini-map close queued after fire-and-handoff: source={} submitted={}",
                source, closeSubmitted);
    }


    /**
     * Submit one mini-map click point for a turn handoff and confirm it actually starts pathing.
     *
     * @param clickPoint logical point and screen-absolute click point to try.
     * @param description log/input source prefix for this physical input sequence.
     * @param expectedMapName current map name expected during the short post-click confirmation.
     * @param checkPanelBeforeOpen true when retry cleanup should verify/close stale UI before opening mini-map.
     * @return PATHING_STARTED only after movement is observed; NO_PATHING means this point should be
     *         retried/fallbacked in the same foreground turn.
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
        MiniMapPathingAttemptResult confirmResult = confirmMiniMapPathingStartedForHandoff(
                source, baseline, expectedMapName, baselineLocation);
        if (confirmResult != MiniMapPathingAttemptResult.PATHING_STARTED) {
            log.info("mini-map handoff click did not start pathing: source={} pixel=({}, {}) baseline={} result={}",
                    source, clickPoint.pixelPoint().x, clickPoint.pixelPoint().y,
                    formatCoordinate(baseline), confirmResult);
            return confirmResult;
        }
        gameStateUtil.recordMovementIntent(source);
        closeMiniMapAfterConfirmedPathing(source);
        return MiniMapPathingAttemptResult.PATHING_STARTED;
    }

    private MiniMapPathingAttemptResult confirmMiniMapPathingStartedForHandoff(String source,
                                                                              MapCoordinate baseline,
                                                                              String expectedMapName,
                                                                              TemplateLocationInfo baselineLocation) {
        long startedAt = System.currentTimeMillis();
        if (gameStateUtil.isMovingByPixelDiff(source + ":handoff-fast-edge")) {
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

    private void closeMiniMapAfterConfirmedPathing(String source) {
        boolean closeSubmitted = inputSequences.submitExclusiveAndWait(source + ":close-mini-map-after-confirmed-pathing", () -> {
            /*
             * Current-map navigation opens the Alt+1 panel itself, and the game does not close that
             * panel automatically after an in-map coordinate click. Close it synchronously before
             * yielding the task turn so the next window/action does not inherit this UI state.
             */
            return pressAlt1ForMiniMap(source + ":close-after-confirmed-pathing")
                    && TaskSleep.sleep(300);
        });
        if (!closeSubmitted || Thread.currentThread().isInterrupted()) {
            log.info("mini-map close after confirmed pathing stopped before fallback check: source={} submitted={}",
                    source, closeSubmitted);
            return;
        }
        if (isMiniMapPanelVisible(source + ":after-close-check", true)) {
            log.warn("mini-map still visible after confirmed pathing close; falling back to generic close button: source={}",
                    source);
            uiCleanerService.closeAllGenericWindows();
        }
    }

    /**
     * Cheap arrival guard for handoff-style navigation.
     *
     * @param targetX target logical X coordinate.
     * @param targetY target logical Y coordinate.
     * @param source log source. This method deliberately avoids OCR and only consumes already-synced
     *               window/pathing state.
     * @return true when the already-synced per-window state is near the target coordinate.
     */
    private boolean isCurrentCachedCoordinateNear(int targetX, int targetY, int tolerance, String source) {
        WindowPathingSnapshot snapshot = windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getPathingSnapshot)
                .orElse(null);
        if (snapshot != null && snapshot.getUpdatedAtMs() > 0L) {
            long ageMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
            WindowPathingIntent intent = snapshot.getIntent();
            boolean sameTargetIntent = intent != null
                    && intent.getTargetX() != null
                    && intent.getTargetY() != null
                    && intent.getTargetX() == targetX
                    && intent.getTargetY() == targetY;
            if (ageMs <= RECENT_PATHING_SNAPSHOT_MAX_AGE_MS && sameTargetIntent) {
                boolean arrived = snapshot.getState() == WindowPathingState.ARRIVED
                        && gameStateUtil.isNearCoordinate(null,
                        snapshot.getCurrentX(), snapshot.getCurrentY(), null, targetX, targetY, tolerance);
                log.info("cached coordinate checked by pathing snapshot: source={} state={} current=({}, {}) target=({}, {}) tolerance={} ageMs={} arrived={}",
                        source, snapshot.getState(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                        targetX, targetY, tolerance, ageMs, arrived);
                return arrived;
            }
        }

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
                        /*
                         * A current-map click can still hand control to the game's route planner.
                         * The planner may step through a nearby transfer point and briefly report a
                         * different map before it reaches the requested coordinate. Treat any real
                         * post-click coordinate change as pathing instead of retrying another
                         * foreground mini-map click that can interrupt the route already in flight.
                         */
                        log.info("mini-map pathing confirmation: coordinate changed on unexpected map, treat as pathing started source={} expectedMap={} baselineMap={} currentMap={} baseline={} previous={} current={}",
                                source, expectedMap, baselineLocation == null ? null : baselineLocation.mapName(),
                                currentMap, formatCoordinate(baseline), formatCoordinate(previousReadable),
                                formatCoordinate(current));
                        return MiniMapPathingAttemptResult.PATHING_STARTED;
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

    private String canonicalCurrentMapForWorldMapRouteMemory(String source) {
        PlayerCharacter me = context.getMe();
        String raw = me == null ? null : me.getCurrentMapName();
        return canonicalMapName(raw, "world-map-route-memory:from:" + source);
    }

    private String canonicalMapName(String rawMapName, String source) {
        String normalized = normalizeNullable(rawMapName);
        if (normalized == null) {
            return null;
        }
        String canonical = mapNameCanonicalizer.canonicalize(normalized, source);
        return normalizeNullable(canonical);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private RouteCloudDecision shadowRouteCandidate(String source,
                                                    String phase,
                                                    String fromMapName,
                                                    String targetMapName,
                                                    String canonicalTargetMapName,
                                                    String routeMode,
                                                    String candidateSource,
                                                    WorldMapDestinationClickResult status,
                                                    int attempt,
                                                    boolean routeClicked,
                                                    NavigationRequest request) {
        String taskCode = navigationTaskCode(request, source);
        NavigationRuntimeState state = state();
        String click = routeClicked
                && state.lastWorldMapRouteRelativeX != null
                && state.lastWorldMapRouteRelativeY != null
                ? state.lastWorldMapRouteRelativeX + "," + state.lastWorldMapRouteRelativeY
                : "";
        String localDecision = "mode=" + safeShadowValue(routeMode)
                + ";candidateSource=" + safeShadowValue(candidateSource)
                + ";status=" + enumName(status)
                + ";click=" + click
                + ";routeClicked=" + routeClicked
                + ";attempt=" + attempt
                + ";reason=local-shadow";
        Map<String, String> context = new LinkedHashMap<>();
        context.put("fromMap", safeShadowValue(fromMapName));
        context.put("targetMap", safeShadowValue(targetMapName));
        context.put("canonicalTargetMap", safeShadowValue(canonicalTargetMapName));
        context.put("routeMode", safeShadowValue(routeMode));
        context.put("candidateSource", safeShadowValue(candidateSource));
        context.put("status", enumName(status));
        context.put("routeClicked", Boolean.toString(routeClicked));
        context.put("attempt", Integer.toString(attempt));
        context.put("localWouldClick", click);
        if (request != null) {
            context.put("targetX", safeShadowValue(request.getTargetX()));
            context.put("targetY", safeShadowValue(request.getTargetY()));
            context.put("targetName", safeShadowValue(request.getTargetName()));
            context.put("requestSource", safeShadowValue(request.getSource()));
        }
        RouteCloudDecision decision = routeCloudDecisionService.decideRouteCandidate(
                taskCode,
                source,
                phase,
                localDecision,
                context);
        shadowPolicyVersion(source, taskCode, "navigation-route-candidate", routeMode);
        return decision;
    }

    private RouteCloudDecision decideRouteCandidateClick(String source,
                                                         String phase,
                                                         String fromMapName,
                                                         String targetMapName,
                                                         String canonicalTargetMapName,
                                                         String routeMode,
                                                         String candidateSource,
                                                         WorldMapDestinationClickResult status,
                                                         int attempt,
                                                         Integer relativeX,
                                                         Integer relativeY,
                                                         String reason,
                                                         RouteResultImagePayload routeResultPayload,
                                                         NavigationRequest request) {
        String taskCode = navigationTaskCode(request, source);
        String click = relativeX == null || relativeY == null ? "" : relativeX + "," + relativeY;
        String localDecision = "mode=" + safeShadowValue(routeMode)
                + ";candidateSource=" + safeShadowValue(candidateSource)
                + ";status=" + enumName(status)
                + ";click=" + click
                + ";routeClicked=" + (status == WorldMapDestinationClickResult.CLICKED)
                + ";attempt=" + attempt
                + ";reason=" + safeShadowValue(reason);
        Map<String, String> context = new LinkedHashMap<>();
        context.put("fromMap", safeShadowValue(fromMapName));
        context.put("targetMap", safeShadowValue(targetMapName));
        context.put("canonicalTargetMap", safeShadowValue(canonicalTargetMapName));
        context.put("routeMode", safeShadowValue(routeMode));
        context.put("candidateSource", safeShadowValue(candidateSource));
        context.put("status", enumName(status));
        context.put("routeClicked", Boolean.toString(status == WorldMapDestinationClickResult.CLICKED));
        context.put("attempt", Integer.toString(attempt));
        context.put("localWouldClick", click);
        context.put("reason", safeShadowValue(reason));
        if (routeResultPayload != null) {
            context.put("imagePayloadBase64", routeResultPayload.imagePayloadBase64());
            context.put("payloadMimeType", routeResultPayload.payloadMimeType());
            context.put("imageSha256", routeResultPayload.imageSha256());
            context.put("rawImagePath", routeResultPayload.rawImagePath());
            context.put("debugImageId", routeResultPayload.debugImageId());
            context.put("windowSize", routeResultPayload.windowSize());
            context.put("roi", routeResultPayload.roi());
        }
        if (request != null) {
            context.put("targetX", safeShadowValue(request.getTargetX()));
            context.put("targetY", safeShadowValue(request.getTargetY()));
            context.put("targetName", safeShadowValue(request.getTargetName()));
            context.put("requestSource", safeShadowValue(request.getSource()));
        }
        RouteCloudDecision decision = routeCloudDecisionService.decideRouteCandidate(
                taskCode,
                source,
                phase,
                localDecision,
                context);
        shadowPolicyVersion(source, taskCode, "navigation-route-candidate", routeMode);
        return decision;
    }

    private RouteResultImagePayload routeResultImagePayload(String mapResultImagePath, int[] mapRect) {
        if (mapResultImagePath == null || mapResultImagePath.isBlank() || mapRect == null || mapRect.length < 4) {
            return null;
        }
        ImagePreprocessCloudRequest.Roi roi = ImagePreprocessCloudRequest.Roi.builder()
                .x(mapRect[0] - tracker.getWindowBaseX())
                .y(mapRect[1] - tracker.getWindowBaseY())
                .width(mapRect[2] - mapRect[0])
                .height(mapRect[3] - mapRect[1])
                .build();
        String roiText = roiText(roi);
        String rejectReason = routeResultRoiRejectReason(roi);
        if (rejectReason != null) {
            log.warn("navigation route candidate payload skipped: image={} roi={} reason={}",
                    mapResultImagePath, roiText, rejectReason);
            return null;
        }
        try {
            byte[] imageBytes = Files.readAllBytes(Path.of(mapResultImagePath));
            if (imageBytes.length == 0) {
                log.warn("navigation route candidate payload skipped: image={} roi={} reason=empty-image",
                        mapResultImagePath, roiText);
                return null;
            }
            return new RouteResultImagePayload(
                    Base64.getEncoder().encodeToString(imageBytes),
                    ROUTE_RESULT_PAYLOAD_MIME_TYPE,
                    sha256Hex(imageBytes),
                    mapResultImagePath,
                    "world-map-route-result",
                    GAME_WINDOW_WIDTH + "," + GAME_WINDOW_HEIGHT,
                    roiText);
        } catch (Exception e) {
            log.warn("navigation route candidate payload skipped: image={} roi={} error={}",
                    mapResultImagePath, roiText, e.getMessage());
            log.debug("navigation route candidate payload failure stack", e);
            return null;
        }
    }

    private static String routeResultRoiRejectReason(ImagePreprocessCloudRequest.Roi roi) {
        if (roi == null) {
            return "missing-roi";
        }
        if (roi.getX() < 0 || roi.getY() < 0 || roi.getWidth() <= 0 || roi.getHeight() <= 0) {
            return "invalid-window-relative-roi";
        }
        long maxX = (long) roi.getX() + roi.getWidth();
        long maxY = (long) roi.getY() + roi.getHeight();
        if (maxX > GAME_WINDOW_WIDTH || maxY > GAME_WINDOW_HEIGHT) {
            return "roi-outside-window";
        }
        return null;
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    private static String roiText(ImagePreprocessCloudRequest.Roi roi) {
        if (roi == null) {
            return "";
        }
        return roi.getX() + "," + roi.getY() + "," + roi.getWidth() + "," + roi.getHeight();
    }

    private Point effectiveRouteClick(RouteCloudDecision routeDecision) {
        Point effective = routeDecision == null ? null : routeDecision.getCloudWindowRelativeClickPoint();
        if (effective == null) {
            throw new IllegalStateException("route cloud decision did not provide an executable click");
        }
        return effective;
    }

    private void shadowPolicyVersion(String source, String taskCode, String phase, String routeMode) {
        String policyVersion = "navigation-shadow-v1";
        Map<String, String> context = new LinkedHashMap<>();
        context.put("policyVersion", policyVersion);
        context.put("routeMode", safeShadowValue(routeMode));
        context.put("owner", "NavigationService");
        context.put("diagnosticOnly", "true");
        context.put("diagnosticNote", "POLICY_VERSION does not control navigation");
        log.info("navigation policy-version diagnostic-only: source={} taskCode={} phase={} routeMode={} policyVersion={} note=does not control navigation",
                source, taskCode, phase, routeMode, policyVersion);
        runtimeDecisionShadowService.shadow(
                CloudDecisionServiceId.POLICY_VERSION,
                taskCode,
                phase,
                source,
                "policyVersion=" + policyVersion,
                context);
    }

    private String requestSource(NavigationRequest request) {
        return request == null ? "navigation" : request.getSource();
    }

    private boolean isActivePathingIntentCompatibleWithRequest(WindowPathingIntent intent,
                                                               String targetMapName,
                                                               String requestSource) {
        return intent != null
                && gameStateUtil.isSameMapName(intent.getTargetMapName(), targetMapName)
                && isPathingSourceCompatibleForDuplicate(intent.getSource(), requestSource);
    }

    static boolean isPathingSourceCompatibleForDuplicate(String activeSource, String requestSource) {
        String active = normalizeSourceForDuplicate(activeSource);
        String request = normalizeSourceForDuplicate(requestSource);
        return active != null
                && request != null
                && (active.equals(request) || active.startsWith(request + ":"));
    }

    private static String normalizeSourceForDuplicate(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String navigationTaskCode(NavigationRequest request, String source) {
        String value = normalizeNullable(source);
        if (value == null && request != null) {
            value = normalizeNullable(request.getSource());
        }
        if (value == null) {
            return "navigation";
        }
        String lower = value.toLowerCase();
        if (lower.startsWith("wubei")) {
            return "wubei";
        }
        if (lower.startsWith("xiuluo")) {
            return "xiuluo_v2";
        }
        if (lower.startsWith("wuhuan")) {
            return "wuhuan_v2";
        }
        return "navigation";
    }

    private String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private String safeShadowValue(Object value) {
        return value == null ? "" : String.valueOf(value);
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
    private boolean registerWindowPathingIntent(NavigationRequest request,
                                                String phase,
                                                String message,
                                                boolean includeCoordinate) {
        if (request == null) {
            return false;
        }
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            return false;
        }
        WindowRuntimeContext windowContext = current.get();
        WindowPathingIntent intent = WindowPathingIntent.builder()
                .source(request.getSource() + ":" + phase + ":" + message)
                .targetMapName(request.getTargetMapName())
                .targetX(includeCoordinate ? request.getTargetX() : null)
                .targetY(includeCoordinate ? request.getTargetY() : null)
                .tolerance(navigationArrivalTolerance(request))
                .build();
        windowContext.markPathingStarted(intent);
        PlayerCharacter me = context.getMe();
        log.info("window pathing intent registered: windowId={} title={} hwnd={} intentId={} phase={} source={} currentMap={} current=({}, {}) targetMap={} target=({}, {}) tolerance={}",
                windowContext.getWindowId(),
                windowContext.getNativeBinding() == null ? null : windowContext.getNativeBinding().getTitle(),
                windowContext.getNativeBinding() == null ? null : windowContext.getNativeBinding().getNativeHandle(),
                intent.getIntentId(),
                phase,
                intent.getSource(),
                me == null ? null : me.getCurrentMapName(),
                me == null ? null : me.getX(),
                me == null ? null : me.getY(),
                intent.getTargetMapName(),
                intent.getTargetX(), intent.getTargetY(), intent.getTolerance());
        return true;
    }

    private int navigationArrivalTolerance(NavigationRequest request) {
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

    private NavigationRuntimeState state() {
        String key = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
        return runtimeStates.computeIfAbsent(key, ignored -> new NavigationRuntimeState());
    }

    private static class NavigationRuntimeState {
        private int lastAbsoluteLogicalX = DEFAULT_LOGICAL_COORDINATE;
        private int lastAbsoluteLogicalY = DEFAULT_LOGICAL_COORDINATE;
        private Integer lastWorldMapRouteRelativeX;
        private Integer lastWorldMapRouteRelativeY;
        private String lastWorldMapRouteMatchedText;
        private boolean lastWorldMapRouteUsedMemory;
        private String lastWorldMapRouteDecisionId;

        private void clearWorldMapRouteResultClick() {
            lastWorldMapRouteRelativeX = null;
            lastWorldMapRouteRelativeY = null;
            lastWorldMapRouteMatchedText = null;
            lastWorldMapRouteUsedMemory = false;
            lastWorldMapRouteDecisionId = null;
        }
    }

    private record RouteResultImagePayload(
            String imagePayloadBase64,
            String payloadMimeType,
            String imageSha256,
            String rawImagePath,
            String debugImageId,
            String windowSize,
            String roi) {
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

    private enum RecentPathingMapCheck {
        ARRIVED,
        PATHING_ACTIVE,
        NO_USABLE_SNAPSHOT
    }

    private enum WorldMapDestinationClickResult {
        CLICKED,
        NOT_FOUND,
        WRONG_DESTINATION
    }
}
