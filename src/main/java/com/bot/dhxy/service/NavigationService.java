package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogHandleResult;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.team.TeamRoleDetectionService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.vision.LocationVisionService;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates map navigation for the currently bound game window.
 *
 * <p>This service owns the task-level navigation contract: it converts logical
 * game map coordinates into screen-absolute click points, submits real input
 * through {@link InputSequences}, releases the task turn when pathing has
 * safely started, and keeps screenshots/OCR tied to the current
 * {@link WindowTaskContextHolder} binding. Callers must pass logical in-game
 * map coordinates, not screen pixels.</p>
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
    private static final double MAP_STARTUP_OPTION_MATCH_RATE = 0.95;
    private static final String MAP_TRACKING_CHECKED_TEMPLATE = "images/template/map/checkbox_checked.png";
    private static final String MAP_TRACKING_UNCHECKED_TEMPLATE = "images/template/map/checkbox_unchecked.png";
    private static final String AUTO_CLOSE_MAP_CHECKED_TEMPLATE = "images/template/map/auto_close_map_checked.png";
    private static final String AUTO_CLOSE_MAP_UNCHECKED_TEMPLATE = "images/template/map/auto_close_map_unchecked.png";
    private static final String OPEN_FLY_CHECKED_TEMPLATE = "images/template/map/open_fly_checked.png";
    private static final String OPEN_FLY_UNCHECKED_TEMPLATE = "images/template/map/open_fly_unchecked.png";
    private static final int MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS = 2;
    private static final int MAP_RESULT_SCROLL_DOWN_UNITS = 3;
    private static final long MAP_RESULT_SCROLL_INTERVAL_MS = 80L;
    private static final long MAP_RESULT_SCROLL_SETTLE_MS = 300L;
    private static final long MOVING_NAVIGATION_YIELD_MS = 1500L;
    private static final long NPC_DIALOG_FAST_POLL_TIMEOUT_MS = 20000L;
    private static final long NPC_DIALOG_FAST_POLL_INTERVAL_MS = 350L;
    private static final long NPC_DIALOG_MOVEMENT_CHECK_INTERVAL_MS = 1600L;
    /*
     * NPC mini-map clicks are only a pre-positioning step before clickNpcSmart/tooltip matching.
     * If no dialog appears and the movement detector is already inactive on the first scheduled
     * check, return to the task layer quickly instead of burning several seconds in a hard wait.
     */
    private static final long NPC_DIALOG_MIN_WAIT_BEFORE_INACTIVE_FALLBACK_MS = 1600L;
    private static final String ALT6_VISIBILITY_TEMPLATE = "images/template/status/blacklist_crowd.png";
    private static final int ALT6_VISIBILITY_RECT_X_OFFSET = 359;
    private static final int ALT6_VISIBILITY_RECT_Y_OFFSET = 271;
    private static final int ALT6_VISIBILITY_RECT_WIDTH = 317;
    private static final int ALT6_VISIBILITY_RECT_HEIGHT = 288;
    private static final double ALT6_VISIBILITY_MATCH_RATE = 0.85;
    private static final int ALT6_VISIBILITY_MAX_ATTEMPTS = 3;
    private static final long ALT6_VISIBILITY_RECHECK_DELAY_MS = 500L;
    private static final long ALT6_OVERLAY_FADEOUT_WAIT_MS = 1000L;
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int MINI_MAP_EDGE_INSET_TRIGGER_PX = 240;
    private static final long MINI_MAP_PATHING_CONFIRM_TIMEOUT_MS = 2200L;
    private static final long MINI_MAP_PATHING_CONFIRM_POLL_MS = 250L;
    private static final int COMBAT_TARGET_APPROACH_OFFSET = 2;
    private static final int MAP_NAVIGATION_RECLICK_STUCK_SCANS = 2;
    private static final int MAP_NAVIGATION_REOPEN_STUCK_SCANS = 3;

    private final BotProperties config;
    private final GameContext context;
    private final LocationVisionService locationRadar;
    private final GameClientTracker tracker;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final TextRecognizer ocr;
    private final GameStateUtil gameStateUtil;
    private final CoordinateHelper coordinateHelper;
    private final UICleanerService uiCleanerService;
    private final DialogService dialogService;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowFocusService windowFocusService;
    private final Random random = new Random();
    private final PlayerStateService playerStateService;
    private final BattleRadarService battleRadarService;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TeamRoleDetectionService teamRoleDetectionService;
    private final TaskTurnCoordinator taskTurnCoordinator;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final BoundWindowKeyboardService boundWindowKeyboardService;

    private final Map<String, NavigationRuntimeState> runtimeStates = new ConcurrentHashMap<>();
    private static final long LIGHTWEIGHT_CLEAN_INTERVAL_MS = 2500L;

    /**
     * Navigate to an NPC coordinate on a target map using normal task-turn release behavior.
     *
     * @param targetMapName target map name as shown by the game/OCR route system.
     * @param targetX logical in-game map X coordinate of the NPC.
     * @param targetY logical in-game map Y coordinate of the NPC.
     * @return true when the map and current-map navigation steps complete; false when map
     *         transforms are missing, input cannot be submitted, the task is stopped, or timeout occurs.
     */
    public boolean navigateToNPC(String targetMapName, int targetX, int targetY) {
        return navigateToNPC(targetMapName, targetX, targetY, true);
    }

    /**
     * Navigate to an NPC without releasing the task turn after pathing starts.
     *
     * <p>This is used for leader-only task startup/acceptance flows where the leader must keep
     * ownership until the next task transaction explicitly decides to yield. The coordinates are
     * logical in-game map coordinates.</p>
     *
     * @param targetMapName target map name.
     * @param targetX logical in-game map X coordinate.
     * @param targetY logical in-game map Y coordinate.
     * @return true on successful navigation setup; false on missing transform, failed input, stop, or timeout.
     */
    public boolean navigateToNPCWithoutTurnRelease(String targetMapName, int targetX, int targetY) {
        return navigateToNPC(targetMapName, targetX, targetY, false);
    }

    private boolean navigateToNPC(String targetMapName, int targetX, int targetY, boolean releaseTurnOnPathing) {
        return navigateToMapPoint(targetMapName, targetX, targetY, releaseTurnOnPathing, "navigateToNPC");
    }

    /**
     * Navigate near a combat target, not exactly onto the target coordinate.
     *
     * <p>Monster/NPC battle targets can overlap the player if we path to the exact coordinate.
     * This method first computes a small logical approach coordinate via
     * {@link #calculateCombatTargetApproach(String, int, int)}, then delegates to normal navigation.
     * The original target coordinate remains the business target for later OCR/NPC-click logic.</p>
     *
     * @param targetMapName target map name.
     * @param targetX logical in-game X coordinate of the combat target.
     * @param targetY logical in-game Y coordinate of the combat target.
     * @return true when the approach coordinate is reached; false when normal navigation fails.
     */
    public boolean navigateToCombatTarget(String targetMapName, int targetX, int targetY) {
        MapCoordinate approach = calculateCombatTargetApproach(targetMapName, targetX, targetY);
        return navigateToMapPoint(targetMapName, approach.getX(), approach.getY(), true,
                "navigateToCombatTarget", true, true);
    }

    /**
     * Shared map-then-coordinate navigation wrapper.
     *
     * <p>The method intentionally performs arrival cleanup only after checking whether a business
     * dialog is already open. Navigation may directly trigger an NPC/task option dialog; in that
     * case generic UI cleanup must not close it before the task layer can process it.</p>
     */
    private boolean navigateToMapPoint(String targetMapName,
                                       int targetX,
                                       int targetY,
                                       boolean releaseTurnOnPathing,
                                       String source) {
        return navigateToMapPoint(targetMapName, targetX, targetY, releaseTurnOnPathing, source, false, false);
    }

    /**
     * Shared map-then-coordinate navigation wrapper with task-specific performance switches.
     *
     * <p>The extra switches are intentionally private because they encode business semantics,
     * not generic navigation preferences. Combat-target approach navigation must always submit
     * the first mini-map click so the character is positioned for the later NPC/monster click,
     * and it can skip generic arrival cleanup because the task layer immediately performs the
     * target interaction. Ordinary navigation keeps the safer default checks.</p>
     *
     * @param targetMapName destination map name as used by the world-map search input.
     * @param targetX logical in-game X coordinate on the destination map.
     * @param targetY logical in-game Y coordinate on the destination map.
     * @param releaseTurnOnPathing whether the task turn may be yielded after pathing starts.
     * @param source short log source used to separate NPC, combat target, and normal map calls.
     * @param forceFirstMiniMapClick true to skip the expensive pre-click arrival sync and click
     *                               the mini-map once even when cached coordinates look close.
     * @param skipArrivalCleanup true when the caller will immediately handle the next UI/dialog
     *                           state and generic cleanup would only add latency.
     * @return true when map navigation plus current-map coordinate navigation succeeds; false on
     *         failed map search/click, current-map navigation failure, stop, or interruption.
     */
    private boolean navigateToMapPoint(String targetMapName,
                                       int targetX,
                                       int targetY,
                                       boolean releaseTurnOnPathing,
                                       String source,
                                       boolean forceFirstMiniMapClick,
                                       boolean skipArrivalCleanup) {
        long latencyStart = LatencyMetrics.start();
        boolean result = navigateToMapPointInternal(targetMapName, targetX, targetY, releaseTurnOnPathing,
                source, forceFirstMiniMapClick, skipArrivalCleanup);
        LatencyMetrics.info(log, "navigation.mapPoint", latencyStart,
                "result=" + result + " source=" + source + " target=" + targetMapName
                        + "(" + targetX + "," + targetY + ") releaseTurn=" + releaseTurnOnPathing);
        return result;
    }

    private boolean navigateToMapPointInternal(String targetMapName,
                                               int targetX,
                                               int targetY,
                                               boolean releaseTurnOnPathing,
                                               String source,
                                               boolean forceFirstMiniMapClick,
                                               boolean skipArrivalCleanup) {
        checkpointTask();

        /*
         * Stage 1: get onto the destination map through the world-map search UI. This only solves
         * the cross-map route; the target coordinate is handled by the mini-map stage below.
         */
        if (!navigateToMap(targetMapName, releaseTurnOnPathing)) {
            if (releaseTurnOnPathing) {
                taskTurnCoordinator.forceRelease("navigation:" + source + ":map-failed");
            }
            return false;
        }
        checkpointTask();

        /*
         * Stage 2: after the map is correct, click the current-map mini-map coordinate. NPC routes
         * keep a different success rule because opening the NPC dialog is a valid arrival signal.
         */
        boolean expectNpcDialog = "navigateToNPC".equals(source);
        boolean result = navigateInCurrentMap(targetX, targetY, releaseTurnOnPathing, expectNpcDialog, forceFirstMiniMapClick);
        if (!result) {
            if (releaseTurnOnPathing) {
                taskTurnCoordinator.forceRelease("navigation:" + source + ":current-map-failed");
            }
            return false;
        }
        checkpointTask();

        /*
         * Stage 3: clean only when the caller is not about to handle a dialog/target interaction.
         * This avoids closing a business dialog that navigation just opened successfully.
         */
        if (expectNpcDialog) {
            log.info("skip arrival cleanup after NPC navigation; task layer will process any opened dialog");
            return true;
        }
        if (skipArrivalCleanup) {
            log.info("skip arrival cleanup after {} navigation; task layer will process target interaction", source);
            return true;
        }
        ensureTaskTurn(source + ":arrivalCleanup");
        DialogService.DialogType type = dialogService.detectDialogTypeNoFocus(source + ":arrivalCleanup-check");
        if (type == DialogService.DialogType.NONE) {
            uiCleanerService.cleanUpAll();
        } else {
            log.info("skip arrival cleanup because dialog is open: type={}", type);
        }
        return true;
    }

    /**
     * Navigate within the current map to a logical in-game coordinate.
     *
     * @param targetX logical in-game map X coordinate on the current map.
     * @param targetY logical in-game map Y coordinate on the current map.
     * @return true when pathing reaches the coordinate tolerance or opens a dialog; false on input failure,
     *         missing transform, interruption, or timeout.
     */
    public boolean navigateInCurrentMap(int targetX, int targetY) {
        return navigateInCurrentMap(targetX, targetY, true);
    }

    /**
     * Navigate within the current map to a combat-target approach coordinate.
     *
     * <p>The provided coordinate is the real monster/NPC logical coordinate. The method computes
     * a small nearby approach coordinate first so the character does not stand directly under the
     * target nameplate. This method still uses the existing mini-map navigation implementation.</p>
     *
     * @param targetX logical in-game X coordinate of the combat target.
     * @param targetY logical in-game Y coordinate of the combat target.
     * @return true when normal current-map navigation succeeds for the approach coordinate.
     */
    public boolean navigateInCurrentMapNearCombatTarget(int targetX, int targetY) {
        String mapName = context.getMe().getCurrentMapName();
        MapCoordinate approach = calculateCombatTargetApproach(mapName, targetX, targetY);
        return navigateInCurrentMap(approach.getX(), approach.getY(), true);
    }

    /**
     * Mini-map pathing loop for one logical coordinate.
     *
     * <p>All real Alt+1/click/Alt+1 sequences go through the input queue. When movement starts,
     * this method may release the task turn so follower windows can perform safe background work.
     * It also treats an opened dialog as success because an NPC/business dialog can be the desired
     * arrival signal.</p>
     */
    private boolean navigateInCurrentMap(int targetX, int targetY, boolean releaseTurnOnPathing) {
        return navigateInCurrentMap(targetX, targetY, releaseTurnOnPathing, false);
    }

    private boolean navigateInCurrentMap(int targetX, int targetY, boolean releaseTurnOnPathing, boolean expectNpcDialog) {
        return navigateInCurrentMap(targetX, targetY, releaseTurnOnPathing, expectNpcDialog, false);
    }

    /**
     * Mini-map pathing loop with an optional forced first click.
     *
     * <p>Normally this method can return before clicking if a fresh minimap read says the player
     * is already within tolerance. Combat-target approach calls deliberately bypass that shortcut:
     * the task needs a real mini-map click to center/position the character before the following
     * tooltip/NPC-click pipeline. The forced-click flag only affects the first pre-click shortcut;
     * all later movement, dialog, retry, and stop handling remains the shared navigation logic.</p>
     */
    private boolean navigateInCurrentMap(int targetX,
                                         int targetY,
                                         boolean releaseTurnOnPathing,
                                         boolean expectNpcDialog,
                                         boolean forceFirstMiniMapClick) {
        long latencyStart = LatencyMetrics.start();
        boolean result = navigateInCurrentMapInternal(targetX, targetY, releaseTurnOnPathing,
                expectNpcDialog, forceFirstMiniMapClick);
        LatencyMetrics.info(log, "navigation.currentMap", latencyStart,
                "result=" + result + " target=(" + targetX + "," + targetY + ")"
                        + " expectNpcDialog=" + expectNpcDialog
                        + " forceFirstClick=" + forceFirstMiniMapClick);
        return result;
    }

    private boolean navigateInCurrentMapInternal(int targetX,
                                                 int targetY,
                                                 boolean releaseTurnOnPathing,
                                                 boolean expectNpcDialog,
                                                 boolean forceFirstMiniMapClick) {
        String mapName = context.getMe().getCurrentMapName();
        log.info("navigate in map: {} target=({}, {})", mapName, targetX, targetY);

        Point pixelPoint = coordinateHelper.getPhysicalMapPoint(mapName, targetX, targetY);
        if (pixelPoint == null) {
            log.error("map transform missing: {}", mapName);
            return false;
        }
        List<MiniMapClickCandidate> clickCandidates = buildMiniMapClickCandidates(mapName, targetX, targetY, pixelPoint);

        long startTime = System.currentTimeMillis();
        long timeoutMs = 60000;

        /*
         * Ordinary coordinate navigation may finish before clicking when the minimap coordinate is
         * already within tolerance. NPC-accept navigation is different: the purpose is to make the
         * game path/click into the NPC interaction state. Being within +/-2 logical coordinates is
         * not enough because the character can still be off-center for the later yellow-name/Ctrl
         * click pipeline. For NPC navigation, log the near-arrival but still submit the first
         * mini-map click.
         */
        if (!forceFirstMiniMapClick && !expectNpcDialog
                && syncAndCheckArrived(targetX, targetY, "navigateInCurrentMap:before-first-click")) {
            return true;
        } else if (forceFirstMiniMapClick) {
            log.info("force first mini-map click before arrival sync: target=({}, {}) source=combat-target-approach",
                    targetX, targetY);
        } else if (expectNpcDialog) {
            syncAndLogNearArrived(targetX, targetY, "navigateInCurrentMap:npc-before-first-click");
        }

        MiniMapClickOutcome firstClickOutcome = clickMiniMapPointAndConfirm(
                clickCandidates, "navigateInCurrentMap:first", releaseTurnOnPathing, false,
                targetX, targetY, expectNpcDialog, forceFirstMiniMapClick);
        if (firstClickOutcome == MiniMapClickOutcome.ARRIVED) {
            return true;
        }
        if (firstClickOutcome == MiniMapClickOutcome.FAILED) {
            return false;
        }
        if (firstClickOutcome == MiniMapClickOutcome.DIALOG_OPENED) {
            log.info("navigate in current map reached dialog during first mini-map click: target=({}, {})",
                    targetX, targetY);
            return true;
        }
        if (expectNpcDialog && firstClickOutcome == MiniMapClickOutcome.PENDING_CONFIRMATION
                && waitForExpectedNpcDialogAfterMiniMapClick(targetX, targetY)) {
            return true;
        }
        if (expectNpcDialog && syncAndCheckArrived(targetX, targetY, "navigateInCurrentMap:npc-after-first-click")) {
            log.info("NPC navigation reached target after forced mini-map click; delegate final NPC interaction to task layer: target=({}, {})",
                    targetX, targetY);
            return true;
        }

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            checkpointTask();
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }

            if (battleRadarService.checkAndSyncCombatState()) {
                checkpointTask();
                if (!sleepInterruptible(battleRadarService.getDynamicPollingIntervalMs())) {
                    return false;
                }
                checkpointTask();
                startTime = System.currentTimeMillis();
                continue;
            }

            context.setCurrentActionState(GameContext.ActionState.NAVIGATING);

            /*
             * Ordinary current-map navigation is a coordinate problem, so a stable in-tolerance
             * minimap coordinate can complete the call. NPC navigation is an interaction problem:
             * the task needs the NPC dialog or a clean fallback into clickNpcSmart, so coordinate
             * proximity alone must not short-circuit the retry loop.
             */
            if (syncAndCheckArrived(targetX, targetY, expectNpcDialog
                    ? "navigateInCurrentMap:npc-loop-after-click"
                    : "navigateInCurrentMap:loop")) {
                if (expectNpcDialog) {
                    log.info("NPC navigation reached target after mini-map click; task layer will handle dialog/NPC smart click: target=({}, {})",
                            targetX, targetY);
                }
                return true;
            }

            GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
            checkpointTask();
            if (isActiveNavigationMovement(movementState)) {
                log.info("navigate in current map yielding while moving: target=({}, {}) state={} sleepMs={}",
                        targetX, targetY, movementState, MOVING_NAVIGATION_YIELD_MS);
                if (!sleepInterruptible(MOVING_NAVIGATION_YIELD_MS)) {
                    return false;
                }
                checkpointTask();
                continue;
            }

            ensureTaskTurn("navigateInCurrentMap:activeCheck");
            cleanLightweightInterruptions("navigateInCurrentMap");

            DialogService.DialogType dialogType = dialogService.detectDialogTypeNoFocus("navigateInCurrentMap:stopped-dialog-check");
            checkpointTask();
            if (dialogType != DialogService.DialogType.NONE) {
                log.info("navigate in current map stopped on dialog: target=({}, {}) type={}", targetX, targetY, dialogType);
                return true;
            }

            MiniMapClickOutcome retryOutcome = clickMiniMapPointAndConfirm(
                    clickCandidates, "navigateInCurrentMap:retry", releaseTurnOnPathing, true, targetX, targetY, expectNpcDialog);
            if (!expectNpcDialog && retryOutcome == MiniMapClickOutcome.ARRIVED) {
                return true;
            }
            if (retryOutcome == MiniMapClickOutcome.FAILED) {
                return false;
            }
            if (retryOutcome == MiniMapClickOutcome.DIALOG_OPENED) {
                log.info("navigate in current map reached dialog during retry mini-map click: target=({}, {})",
                        targetX, targetY);
                return true;
            }
            checkpointTask();

            if (!sleepInterruptible(500)) {
                return false;
            }
            checkpointTask();
        }

        log.error("navigate timeout");
        return false;
    }

    /**
     * Start mini-map pathing on the current map without releasing the task turn.
     *
     * <p>This is a low-level helper for task transactions that need to initiate movement but keep
     * ownership until the caller records a transaction result. It submits one queued physical input
     * sequence: open mini-map with Alt+1, click the screen-absolute pixel mapped from the logical
     * coordinate, close the mini-map, and record movement intent.</p>
     *
     * @param mapName map whose transform should be used.
     * @param targetX logical in-game X coordinate.
     * @param targetY logical in-game Y coordinate.
     * @param source log/input-queue description; blank values are replaced by a default label.
     * @return true when the input sequence was accepted by the queue; false if the map transform is missing
     *         or queue submission fails.
     */
    public boolean triggerMiniMapPathingWithoutTurnRelease(String mapName, int targetX, int targetY, String source) {
        String safeSource = source == null || source.isBlank() ? "triggerMiniMapPathingWithoutTurnRelease" : source;
        Point pixelPoint = coordinateHelper.getPhysicalMapPoint(mapName, targetX, targetY);
        if (pixelPoint == null) {
            log.error("mini-map pathing trigger failed, map transform missing: source={} map={} target=({}, {})",
                    safeSource, mapName, targetX, targetY);
            return false;
        }
        boolean submitted = inputSequences.submitAndWait(safeSource, List.of(
                InputAction.pressAlt1(),
                InputAction.sleep(800),
                InputAction.clickLeft(pixelPoint.x, pixelPoint.y, 200),
                InputAction.sleep(500),
                InputAction.pressAlt1(),
                InputAction.sleep(1200)
        ));
        if (submitted) {
            gameStateUtil.recordMovementIntent(safeSource);
        }
        return submitted;
    }

    /**
     * Trigger world-map routing to a map name without releasing the task turn.
     *
     * <p>The whole map search flow runs inside one exclusive input callback because opening the map,
     * typing the map name, scrolling results, and clicking the final route link are one atomic user
     * action. Nested queue calls must not be used inside that callback.</p>
     *
     * @param targetMapName map name to type into the world-map route search.
     * @param source log/input-queue source label.
     * @return true if the route link was clicked and movement intent was recorded.
     */
    public boolean triggerWorldMapPathingWithoutTurnRelease(String targetMapName, String source) {
        String safeSource = source == null || source.isBlank() ? "triggerWorldMapPathingWithoutTurnRelease" : source;
        boolean clicked = inputSequences.submitExclusiveAndWait(safeSource + ":" + targetMapName,
                () -> openMapInputTargetAndClickLastNavPointExclusive(targetMapName));
        if (clicked) {
            gameStateUtil.recordMovementIntent(safeSource);
        }
        return clicked;
    }


    /**
     * Navigate across maps using the world-map search UI.
     *
     * <p>Business dialogs are handled between movement checks because pathing can require clicking
     * route-option dialogs. When movement is active the method sleeps and yields quickly instead of
     * repeatedly fighting for input focus.</p>
     */
    private boolean navigateToMap(String targetMapName, boolean releaseTurnOnPathing) {
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
             * If the cache is blank, do exactly one bound-window position sync before submitting input.
             * A blank map usually means startup/registration has not refreshed identity/location yet.
             */
            checkpointTask();
            if (me.getCurrentMapName() == null || me.getCurrentMapName().isBlank()) {
                log.info("current map is unknown before navigation, syncing position once");
                playerStateService.syncMyPosition();
                log.info("navigate to map after sync: {} current={}", targetMapName, me.getCurrentMapName());
                if (targetMapName.equals(me.getCurrentMapName())) {
                    result = true;
                    return true;
                }
            }

            /*
             * First route submission: open the world map, search the target map, scroll to the bottom
             * result, and click the last route link. The called method owns the exclusive input section.
             */
            ensureTaskTurn("navigateToMap:firstPathing");
            if (!openMapInputTargetAndClickLastNavPoint(targetMapName, releaseTurnOnPathing)) {
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
                    if (!sleepInterruptible(MOVING_NAVIGATION_YIELD_MS)) {
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
                    if (!sleepInterruptible(1500)) {
                        return false;
                    }
                    checkpointTask();
                    continue;
                }

                /*
                 * OCR/template location confirmation is the authoritative map-arrival check. It is
                 * intentionally after dialog handling because route dialogs can block the mini-map label.
                 */
                TextRecognizer.LocationInfo locationInfo = locationRadar.scanCurrentLocation();
                checkpointTask();
                if (locationInfo != null) {
                    me.setCurrentMapName(locationInfo.mapName);
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
                        if (!sleepInterruptible(1500)) {
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
                    if (openMapInputTargetAndClickLastNavPoint(targetMapName, releaseTurnOnPathing)) {
                        stuckCount = 0;
                    }
                } else {
                    ensureTaskTurn("navigateToMap:reclickPathing");
                    clickLastNavPoint(targetMapName, true, releaseTurnOnPathing);
                }
                checkpointTask();

                if (!sleepInterruptible(1500)) {
                    return false;
                }
                checkpointTask();
            }

            log.error("map navigation timeout");
            return false;
        } finally {
            LatencyMetrics.info(log, "navigation.toMap", latencyStart,
                    "result=" + result + " target=" + targetMapName + " releaseTurn=" + releaseTurnOnPathing);
        }
    }

    private boolean isActiveNavigationMovement(GameStateUtil.MovementState state) {
        return state == GameStateUtil.MovementState.MOVING
                || state == GameStateUtil.MovementState.PATHING_ACTIVE
                || state == GameStateUtil.MovementState.MAYBE_MOVING;
    }

    /**
     * Re-click the most recent route result or open a fresh map search.
     *
     * @param targetMapName map name for fresh search fallback; may be blank only when reclick is false.
     * @param reclick true to reuse the last remembered screen-absolute route-link point.
     * @return true when a route click was submitted or performed.
     */
    public boolean clickLastNavPoint(String targetMapName, boolean reclick) {
        return clickLastNavPoint(targetMapName, reclick, true);
    }

    private boolean clickLastNavPoint(String targetMapName, boolean reclick, boolean releaseTurnOnPathing) {
        ensureTaskTurn("clickLastNavPoint");
        if (reclick) {
            NavigationRuntimeState state = state();
            if (state.lastAbsoluteLogicalX != DEFAULT_LOGICAL_COORDINATE
                    && state.lastAbsoluteLogicalY != DEFAULT_LOGICAL_COORDINATE) {
                int clickX = state.lastAbsoluteLogicalX + random.nextInt(7) - 3;
                int clickY = state.lastAbsoluteLogicalY + random.nextInt(7) - 3;
                boolean clicked = inputSequences.submitExclusiveAndWait("clickLastNavPoint:reclick", () -> {
                    if (!openMapDirect()) {
                        return false;
                    }
                    inputProvider.clickLeft(clickX, clickY, 150);
                    if (!sleepInterruptible(2000)) {
                        return false;
                    }
                    gameStateUtil.recordMovementIntent("clickLastNavPoint:reclick");
                    return true;
                });
                if (clicked && releaseTurnOnPathing) {
                    releaseTaskTurnAfterPathing("clickLastNavPoint:reclick");
                }
                return clicked;
            }
            return targetMapName != null && !targetMapName.isBlank()
                    && openMapInputTargetAndClickLastNavPoint(targetMapName, releaseTurnOnPathing);
        }

        return clickLastNavPointFromCurrentMapResult("clickLastNavPoint:first", false, targetMapName) == RouteClickStatus.CLICKED;
    }

    private boolean openMapInputTargetAndClickLastNavPoint(String targetMapName) {
        return openMapInputTargetAndClickLastNavPoint(targetMapName, true);
    }

    private boolean openMapInputTargetAndClickLastNavPoint(String targetMapName, boolean releaseTurnOnPathing) {
        ensureTaskTurn("openMapInputTargetAndClickLastNavPoint");
        boolean clicked = inputSequences.submitExclusiveAndWait("openMapInputTargetAndClickLastNavPoint:" + targetMapName,
                () -> openMapInputTargetAndClickLastNavPointExclusive(targetMapName));
        if (clicked && releaseTurnOnPathing) {
            releaseTaskTurnAfterPathing("openMapInputTargetAndClickLastNavPoint:" + targetMapName);
        }
        return clicked;
    }

    private boolean openMapInputTargetAndClickLastNavPointExclusive(String targetMapName) {
        log.info("navigation map search start: target={}", targetMapName);
        if (!isWorldMapOpened()) {
            log.info("navigation map search: world map not open, press Alt+2");
            inputProvider.pressAlt2();
            sleepInterruptible(500);
        }

        Point xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(XUNLU_TEMPLATE_PATH, THRESHOLD_NORMAL);
        if (xunluPoint == null) {
            log.warn("navigation map search: xunlu button not found, target={}", targetMapName);
            return false;
        }

        boolean searchInputTouched = false;
        boolean clicked = false;
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
                sleepInterruptible(250);

                int scrollFocusX = tracker.getWindowBaseX() + config.getAnchor_windowTo_map_scroll_X();
                int scrollFocusY = tracker.getWindowBaseY() + config.getAnchor_windowTo_map_scroll_Y();
                log.info("navigation map search: type target map={} attempt={}/{}", targetMapName, attempt, 2);
                inputProvider.typeTextUnicode(targetMapName);
                sleepInterruptible(100);
                inputProvider.pressEnter();
                if (!forceScrollToBottomDirect(scrollFocusX, scrollFocusY,
                        "openMapInputTargetAndClickLastNavPoint:" + targetMapName + ":attempt" + attempt)) {
                    return false;
                }

                RouteClickStatus status = clickLastNavPointFromCurrentMapResult(
                        "openMapInputTargetAndClickLastNavPoint:lastLink", true, targetMapName);
                clicked = status == RouteClickStatus.CLICKED;
                log.info("navigation map search: last coordinate click result={} status={} attempt={}/{}",
                        clicked, status, attempt, 2);
                if (clicked) {
                    return true;
                }
                if (status == RouteClickStatus.DESTINATION_MISMATCH && attempt < 2) {
                    closeMapSearchInputAfterRouteClick(
                            "openMapInputTargetAndClickLastNavPoint:destinationMismatch:attempt" + attempt);
                    searchInputTouched = false;
                    if (!sleepInterruptible(250)) {
                        return false;
                    }
                    continue;
                }
                return false;
            }
            return false;
        } finally {
            if (searchInputTouched && !clicked) {
                closeMapSearchInputAfterRouteClick("openMapInputTargetAndClickLastNavPoint:failed");
            }
        }
    }

    private RouteClickStatus clickLastNavPointFromCurrentMapResult(String description,
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
            return RouteClickStatus.NOT_FOUND;
        }
        if (!verifyMapRouteDestinationBeforeClick(mapResultImagePath, expectedDestinationName)) {
            return RouteClickStatus.DESTINATION_MISMATCH;
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
            return RouteClickStatus.NOT_FOUND;
        }

        NavigationRuntimeState state = state();
        state.lastAbsoluteLogicalX = mapRect[0] + relativeCenter.x;
        state.lastAbsoluteLogicalY = mapRect[1] + relativeCenter.y;
        log.info("navigation route coordinate click: windowId={} boundHwnd={} foregroundHwnd={} base=({}, {}) "
                        + "mapRect=({}, {})-({}, {}) image={} relative=({}, {}) absolute=({}, {})",
                currentWindowId(), currentBoundHandle(), windowFocusService.getForegroundNativeHandleText(),
                tracker.getWindowBaseX(), tracker.getWindowBaseY(),
                mapRect[0], mapRect[1], mapRect[2], mapRect[3], routeOcrImagePath,
                relativeCenter.x, relativeCenter.y, state.lastAbsoluteLogicalX, state.lastAbsoluteLogicalY);

        if (directInput) {
            inputProvider.clickLeft(state.lastAbsoluteLogicalX, state.lastAbsoluteLogicalY, 150);
            closeMapSearchInputAfterRouteClick(description);
            if (!sleepInterruptible(2000)) {
                return RouteClickStatus.NOT_FOUND;
            }
            gameStateUtil.recordMovementIntent(description);
            return RouteClickStatus.CLICKED;
        }
        boolean submitted = inputSequences.submitAndWait(description, List.of(
                InputAction.clickLeft(state.lastAbsoluteLogicalX, state.lastAbsoluteLogicalY, 150),
                InputAction.sleep(2000)
        ));
        if (submitted) {
            gameStateUtil.recordMovementIntent(description);
            releaseTaskTurnAfterPathing(description);
        }
        return submitted ? RouteClickStatus.CLICKED : RouteClickStatus.NOT_FOUND;
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
        List<TextRecognizer.OcrWordResult> words = ocr.getAllTextResultsLocalOnly(yellowImagePath);
        TextRecognizer.OcrWordResult last = null;
        for (TextRecognizer.OcrWordResult word : words) {
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

    private String findLastYellowRouteDestinationLine(List<TextRecognizer.OcrWordResult> words) {
        List<TextRecognizer.OcrWordResult> usable = new ArrayList<>();
        for (TextRecognizer.OcrWordResult word : words) {
            if (word == null || word.getText() == null || word.getText().isBlank()) {
                continue;
            }
            usable.add(word);
        }
        if (usable.isEmpty()) {
            return "";
        }

        TextRecognizer.OcrWordResult bottom = usable.get(0);
        for (TextRecognizer.OcrWordResult word : usable) {
            if (centerY(word) > centerY(bottom)) {
                bottom = word;
            }
        }

        int bottomCenterY = centerY(bottom);
        int rowTolerance = Math.max(8, bottom.getHeight());
        List<TextRecognizer.OcrWordResult> bottomLine = new ArrayList<>();
        for (TextRecognizer.OcrWordResult word : usable) {
            if (Math.abs(centerY(word) - bottomCenterY) <= rowTolerance) {
                bottomLine.add(word);
            }
        }
        bottomLine.sort((a, b) -> Integer.compare(a.getLeft(), b.getLeft()));

        StringBuilder builder = new StringBuilder();
        for (TextRecognizer.OcrWordResult word : bottomLine) {
            builder.append(word.getText());
        }
        return builder.toString();
    }

    private int centerY(TextRecognizer.OcrWordResult word) {
        return word.getTop() + Math.max(1, word.getHeight()) / 2;
    }

    private String formatRouteOcrWords(List<TextRecognizer.OcrWordResult> words) {
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

    private boolean openMap() {
        if (!isWorldMapOpened()) {
            if (!inputSequences.submitAndWait("openMap:pressAlt2", List.of(
                    InputAction.pressAlt2(),
                    InputAction.sleep(500)
            ))) {
                return false;
            }
        }

        Point xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(XUNLU_TEMPLATE_PATH, THRESHOLD_NORMAL);
        if (xunluPoint == null) {
            return false;
        }

        return inputSequences.submitAndWait("openMap:clickXunlu", List.of(
                InputAction.clickLeft(xunluPoint.x, xunluPoint.y, 120),
                InputAction.sleep(250)
        ));
    }

    private boolean openMapDirect() {
        if (!isWorldMapOpened()) {
            inputProvider.pressAlt2();
            if (!sleepInterruptible(500)) {
                return false;
            }
        }

        Point xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(XUNLU_TEMPLATE_PATH, THRESHOLD_NORMAL);
        if (xunluPoint == null) {
            return false;
        }

        inputProvider.clickLeft(xunluPoint.x, xunluPoint.y, 120);
        return sleepInterruptible(250);
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

    /**
     * Debug/manual helper that scrolls a world-map result list to the bottom.
     *
     * @param targetX screen-absolute X coordinate used to focus the scroll area.
     * @param targetY screen-absolute Y coordinate used to focus the scroll area.
     */
    public void forceScrollToBottom(int targetX, int targetY) {
        List<InputAction> actions = new ArrayList<>();
        actions.add(InputAction.clickLeft(targetX, targetY, 50));
        for (int i = 0; i < MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS; i++) {
            actions.add(InputAction.scrollDown(MAP_RESULT_SCROLL_DOWN_UNITS));
            actions.add(InputAction.sleep((int) MAP_RESULT_SCROLL_INTERVAL_MS));
        }
        actions.add(InputAction.sleep((int) MAP_RESULT_SCROLL_SETTLE_MS));
        inputSequences.submitAndWait("forceScrollToBottom", actions);
    }

    private boolean forceScrollToBottomDirect(int targetX, int targetY, String source) {
        log.info("navigation map search: force scroll to bottom source={} focus=({}, {}) attempts={} units={}",
                source, targetX, targetY, MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS, MAP_RESULT_SCROLL_DOWN_UNITS);
        inputProvider.clickLeft(targetX, targetY, 50);
        for (int i = 1; i <= MAP_RESULT_SCROLL_TO_BOTTOM_ATTEMPTS; i++) {
            inputProvider.scrollDown(MAP_RESULT_SCROLL_DOWN_UNITS);
            if (!sleepInterruptible(MAP_RESULT_SCROLL_INTERVAL_MS)) {
                return false;
            }
        }
        return sleepInterruptible(MAP_RESULT_SCROLL_SETTLE_MS);
    }

    /**
     * Ensure the mini-map tracking checkbox is enabled.
     *
     * <p>The complete open/check/click/close flow runs as one exclusive input callback so another
     * window cannot focus or click between opening the map and closing it.</p>
     *
     * @return true when the checked state is confirmed or successfully enabled; false when the checkbox
     *         templates cannot be found or the operation is interrupted.
     */
    public boolean ensureMapTrackingOption() {
        return inputSequences.submitExclusiveAndWait("ensureMapTrackingOption", () -> {
            return ensureMapTrackingOptionDirect();
        });
    }

    /**
     * Run the leader task startup visibility preparation.
     *
     * <p>This performs mini-map tracking setup and Alt+6 visibility setup in one exclusive input
     * section. The final wait lets the game's "hide players" overlay fade out before later dialog
     * detection captures screenshots.</p>
     *
     * @return true when both map tracking and Alt+6 visibility are confirmed.
     */
    public boolean prepareTaskStartupWindow() {
        return inputSequences.submitExclusiveAndWait("taskStartup:mapTrackingAndAlt6", () -> {
            boolean mapReady = ensureMapTrackingOptionDirect();
            if (!sleepInterruptible(200)) {
                return false;
            }
            boolean visibilityReady = ensureAlt6VisibilityDirect();
            if (visibilityReady) {
                log.info("task startup visibility: waiting overlay fadeout ms={}", ALT6_OVERLAY_FADEOUT_WAIT_MS);
                if (!sleepInterruptible(ALT6_OVERLAY_FADEOUT_WAIT_MS)) {
                    return false;
                }
            }
            return mapReady && visibilityReady;
        });
    }

    /**
     * Run startup preparation for tasks that only require mini-map tracking.
     *
     * @return true when the mini-map tracking checkbox is enabled; false on detection/input failure.
     */
    public boolean prepareTaskStartupMapOnly() {
        return inputSequences.submitExclusiveAndWait("taskStartup:mapTrackingOnly", () -> {
            log.info("task startup map-only: ensure map tracking option without Alt+6");
            return ensureMapTrackingOptionDirect();
        });
    }

    /**
     * Confirm the Alt+6 player-visibility state and press Alt+6 until the configured template appears.
     *
     * <p>This method is called only from an exclusive input callback; it uses direct
     * {@link InputProvider} calls to avoid queue-in-queue deadlock.</p>
     */
    private boolean ensureAlt6VisibilityDirect() {
        if (isAlt6VisibilityConfirmed()) {
            log.info("task startup visibility: already confirmed by template={}", ALT6_VISIBILITY_TEMPLATE);
            return true;
        }

        for (int attempt = 1; attempt <= ALT6_VISIBILITY_MAX_ATTEMPTS; attempt++) {
            log.info("task startup visibility: press Alt+6 attempt={}/{}",
                    attempt, ALT6_VISIBILITY_MAX_ATTEMPTS);
            inputProvider.pressAlt6();
            if (!sleepInterruptible(ALT6_VISIBILITY_RECHECK_DELAY_MS)) {
                return false;
            }
            if (isAlt6VisibilityConfirmed()) {
                log.info("task startup visibility: confirmed after Alt+6 attempt={}", attempt);
                return true;
            }
        }

        log.warn("task startup visibility: template confirmation failed after {} attempts template={}",
                ALT6_VISIBILITY_MAX_ATTEMPTS, ALT6_VISIBILITY_TEMPLATE);
        return false;
    }

    private boolean isAlt6VisibilityConfirmed() {
        int[] rect = coordinateHelper.getScaledRect(
                ALT6_VISIBILITY_RECT_X_OFFSET,
                ALT6_VISIBILITY_RECT_Y_OFFSET,
                ALT6_VISIBILITY_RECT_WIDTH,
                ALT6_VISIBILITY_RECT_HEIGHT);
        Point matched = coordinateHelper.findImageInRegion(
                ALT6_VISIBILITY_TEMPLATE,
                rect,
                ALT6_VISIBILITY_MATCH_RATE);
        boolean confirmed = matched != null;
        log.info("task startup visibility check: confirmed={} template={} rect=({}, {})-({}, {}) match={}",
                confirmed, ALT6_VISIBILITY_TEMPLATE, rect[0], rect[1], rect[2], rect[3],
                matched == null ? "-" : matched.x + "," + matched.y);
        return confirmed;
    }

    /**
     * Direct implementation of mini-map tracking setup.
     *
     * <p>The method opens the mini-map with Alt+1, searches a window-relative option area, toggles
     * required startup options when needed, and always attempts to close the map. It assumes the caller
     * already owns the input queue worker, so it uses direct {@link InputProvider} calls rather than
     * submitting nested queue requests.</p>
     */
    private boolean ensureMapTrackingOptionDirect() {
        inputProvider.pressAlt1();
        if (!sleepInterruptible(400)) {
            return false;
        }

        int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
        String startupOptionsScanPath = windowScopedTempPath.resolve("map_startup_options_scan.png");
        if (!tracker.captureToFile("map startup options", startupOptionsScanPath, rect[0], rect[1], rect[2], rect[3])) {
            log.warn("ensureMapTrackingOption failed to capture startup option region: rect=({}, {})-({}, {})",
                    rect[0], rect[1], rect[2], rect[3]);
            inputProvider.pressAlt1();
            warnIfMapStillOpenAfterAlt1Close();
            return false;
        }

        /*
         * All three options live in the same Alt+1 settings panel. Keep them in one open/close
         * sequence and one screenshot so multi-window startup cannot steal focus between option
         * toggles and template checks cannot observe different frames.
         */
        boolean trackingReady = ensureStartupMapPanelOption(
                "map-tracking",
                MAP_TRACKING_CHECKED_TEMPLATE,
                MAP_TRACKING_UNCHECKED_TEMPLATE,
                rect,
                startupOptionsScanPath);
        boolean autoCloseReady = ensureStartupMapPanelOption(
                "auto-close-map",
                AUTO_CLOSE_MAP_CHECKED_TEMPLATE,
                AUTO_CLOSE_MAP_UNCHECKED_TEMPLATE,
                rect,
                startupOptionsScanPath);
        boolean openFlyReady = ensureStartupMapPanelOption(
                "open-fly",
                OPEN_FLY_CHECKED_TEMPLATE,
                OPEN_FLY_UNCHECKED_TEMPLATE,
                rect,
                startupOptionsScanPath);

        log.info("ensureMapTrackingOption startup option check finished: tracking={} autoCloseMap={} openFly={} pressing Alt+1 to close map",
                trackingReady, autoCloseReady, openFlyReady);
        inputProvider.pressAlt1();
        warnIfMapStillOpenAfterAlt1Close();
        return trackingReady && autoCloseReady && openFlyReady;
    }

    /**
     * Ensure one startup option in the Alt+1 map settings panel is enabled.
     *
     * @param optionName log label for the option being checked.
     * @param checkedTemplate template path for the already-enabled state.
     * @param uncheckedTemplate template path for the disabled state that should be clicked.
     * @param rect screen-absolute search rectangle returned by {@link CoordinateHelper#getScaledRect};
     *             it covers only the map settings option rows, not the full client.
     * @param scanPath window-scoped screenshot of {@code rect}; all startup options in the current
     *                 pass must share this same image to avoid re-capturing between template checks.
     * @return true when the option is already enabled or was clicked to enable; false when neither
     *         state template was found or the task was interrupted while waiting after the click.
     */
    private boolean ensureStartupMapPanelOption(String optionName, String checkedTemplate, String uncheckedTemplate, int[] rect, String scanPath) {
        Point checkedRes = findStartupMapOptionInCapturedRegion(checkedTemplate, rect, scanPath);
        if (checkedRes != null) {
            log.info("ensureMapTrackingOption startup option already checked: option={} point=({}, {})",
                    optionName, checkedRes.x, checkedRes.y);
            return true;
        }

        Point uncheckedRes = findStartupMapOptionInCapturedRegion(uncheckedTemplate, rect, scanPath);
        if (uncheckedRes == null) {
            log.warn("ensureMapTrackingOption startup option template missing: option={} checked={} unchecked={}",
                    optionName, checkedTemplate, uncheckedTemplate);
            return false;
        }

        /*
         * Existing checkbox templates are captured from the option text/icon area; clicking slightly
         * left of the matched point lands on the actual checkbox for all three startup options.
         */
        int clickX = uncheckedRes.x - 13;
        int clickY = uncheckedRes.y;
        log.info("ensureMapTrackingOption enabling startup option: option={} click=({}, {}) matched=({}, {})",
                optionName, clickX, clickY, uncheckedRes.x, uncheckedRes.y);
        inputProvider.clickLeft(clickX, clickY, 150);
        return sleepInterruptible(500);
    }

    /**
     * Match one startup option template inside the already-captured Alt+1 panel region.
     *
     * @param templatePath template to match inside {@code scanPath}.
     * @param rect screen-absolute ROI rectangle used to produce {@code scanPath}; its top-left corner
     *             is added back to the image-local match point.
     * @param scanPath window-scoped ROI image captured once by {@link #ensureMapTrackingOptionDirect()}.
     * @return screen-absolute top-left match point, or null when the template is not present.
     */
    private Point findStartupMapOptionInCapturedRegion(String templatePath, int[] rect, String scanPath) {
        double[] imagePoint = ImageFinder.find(scanPath, templatePath, MAP_STARTUP_OPTION_MATCH_RATE);
        if (imagePoint == null || imagePoint.length < 2) {
            return null;
        }
        int imageX = (int) Math.round(imagePoint[0]);
        int imageY = (int) Math.round(imagePoint[1]);
        Point absolutePoint = new Point(rect[0] + imageX, rect[1] + imageY);
        log.info("ensureMapTrackingOption startup option template matched: template={} image=({}, {}) absolute=({}, {})",
                templatePath, imageX, imageY, absolutePoint.x, absolutePoint.y);
        return absolutePoint;
    }

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
        return inputSequences.submitExclusiveAndWait(description,
                () -> submitMiniMapClickDirect(pixelPoint, description));
    }

    private boolean submitMiniMapClickDirect(Point pixelPoint, String description) {
        if (!isWorldMapOpened()) {
            pressAlt1ForMiniMap(description + ":open");
            if (!sleepInterruptible(800)) {
                return false;
            }
        } else {
            log.info("mini-map already open before coordinate click: source={}", description);
        }

        if (!isWorldMapOpened()) {
            log.warn("mini-map did not open after Alt+1, refocusing title bar before retry: source={}", description);
            clickWindowTitleBarForShortcutFocus(description);
            pressAlt1ForMiniMap(description + ":open-retry");
            if (!sleepInterruptible(800)) {
                return false;
            }
        }

        if (!isWorldMapOpened()) {
            log.warn("mini-map still not open after retry, abort coordinate click: source={} pixel=({}, {})",
                    description, pixelPoint.x, pixelPoint.y);
            return false;
        }

        inputProvider.clickLeft(pixelPoint.x, pixelPoint.y, 200);
        if (!sleepInterruptible(500)) {
            return false;
        }
        pressAlt1ForMiniMap(description + ":close");
        if (!sleepInterruptible(300)) {
            return false;
        }

        if (isWorldMapOpened()) {
            log.warn("mini-map remained open after close shortcut, pressing Alt+1 once more: source={}", description);
            pressAlt1ForMiniMap(description + ":close-retry");
            if (!sleepInterruptible(300)) {
                return false;
            }
        }
        return true;
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

    private void clickWindowTitleBarForShortcutFocus(String source) {
        WindowNativeBinding binding = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getNativeBinding())
                .orElse(null);
        if (binding == null || !binding.hasGeometry()) {
            log.warn("mini-map focus retry skipped, no bound window geometry: source={}", source);
            return;
        }

        int focusX = binding.getX() + Math.max(80, binding.getWidth() / 4);
        int focusY = binding.getY() + 14;
        log.info("mini-map focus retry click title bar: source={} point=({}, {}) window={}",
                source, focusX, focusY, binding.getGeometryText());
        inputProvider.clickLeft(focusX, focusY, 120);
        sleepInterruptible(120);
    }

    /**
     * Try mini-map click candidates until movement or a dialog is confirmed.
     *
     * <p>Movement confirmation is intentionally separated from click submission: the click itself is
     * serialized through the input queue, while screenshot-based confirmation can run afterward using
     * the current window binding. A dialog is a success because clicking an NPC coordinate can open
     * the business dialog immediately.</p>
     */
    private MiniMapClickOutcome clickMiniMapPointAndConfirm(List<MiniMapClickCandidate> candidates,
                                                            String description,
                                                            boolean releaseTurnOnPathing,
                                                            boolean allowCandidateFallback,
                                                            int targetX,
                                                            int targetY) {
        return clickMiniMapPointAndConfirm(candidates, description, releaseTurnOnPathing,
                allowCandidateFallback, targetX, targetY, false);
    }

    /**
     * Try mini-map click candidates until movement or a dialog is confirmed.
     *
     * @param candidates ordered mini-map click candidates in logical/pixel coordinates.
     * @param description log/input source prefix.
     * @param releaseTurnOnPathing whether to yield the task turn after movement starts.
     * @param allowCandidateFallback whether later inset/nearby candidates may be tried immediately.
     * @param targetX logical in-game X coordinate used for ordinary arrival checks.
     * @param targetY logical in-game Y coordinate used for ordinary arrival checks.
     * @param requireNpcDialog true for NPC-accept navigation. In that mode coordinate tolerance is
     *                         only diagnostic; this method must submit a mini-map click and must not
     *                         return ARRIVED without seeing movement/dialog evidence.
     * @return outcome describing movement/dialog/arrival/failure.
     */
    private MiniMapClickOutcome clickMiniMapPointAndConfirm(List<MiniMapClickCandidate> candidates,
                                                            String description,
                                                            boolean releaseTurnOnPathing,
                                                            boolean allowCandidateFallback,
                                                            int targetX,
                                                            int targetY,
                                                            boolean requireNpcDialog) {
        return clickMiniMapPointAndConfirm(candidates, description, releaseTurnOnPathing,
                allowCandidateFallback, targetX, targetY, requireNpcDialog, false);
    }

    /**
     * Try mini-map click candidates until movement or a dialog is confirmed.
     *
     * <p>The {@code skipBeforeClickArrivalCheck} switch is only for an already-decided forced first
     * click, currently combat-target approach navigation. The outer caller has deliberately chosen
     * to submit one mini-map click even when cached/current coordinates look close, so doing another
     * {@link #syncAndCheckArrived(int, int, String)} inside this method would re-enter the slow
     * position OCR path and defeat that decision. Retry candidates keep the normal pre-click check.</p>
     *
     * @param candidates ordered mini-map click candidates in logical/pixel coordinates.
     * @param description log/input source prefix.
     * @param releaseTurnOnPathing whether to yield the task turn after movement starts.
     * @param allowCandidateFallback whether later inset/nearby candidates may be tried immediately.
     * @param targetX logical in-game X coordinate used for ordinary arrival checks.
     * @param targetY logical in-game Y coordinate used for ordinary arrival checks.
     * @param requireNpcDialog true for NPC-accept navigation where coordinate tolerance is only
     *                         diagnostic and dialog evidence is preferred.
     * @param skipBeforeClickArrivalCheck true to submit the first click without the internal
     *                                    pre-click coordinate sync/OCR.
     * @return outcome describing movement/dialog/arrival/failure.
     */
    private MiniMapClickOutcome clickMiniMapPointAndConfirm(List<MiniMapClickCandidate> candidates,
                                                            String description,
                                                            boolean releaseTurnOnPathing,
                                                            boolean allowCandidateFallback,
                                                            int targetX,
                                                            int targetY,
                                                            boolean requireNpcDialog,
                                                            boolean skipBeforeClickArrivalCheck) {
        for (MiniMapClickCandidate candidate : candidates) {
            checkpointTask();
            if (Thread.currentThread().isInterrupted()) {
                return MiniMapClickOutcome.FAILED;
            }

            String source = description + ":" + candidate.reason();
            if (!skipBeforeClickArrivalCheck && !requireNpcDialog
                    && syncAndCheckArrived(targetX, targetY, source + ":before-click")) {
                return MiniMapClickOutcome.ARRIVED;
            } else if (skipBeforeClickArrivalCheck) {
                log.info("skip mini-map pre-click arrival sync: source={} target=({}, {})",
                        source, targetX, targetY);
            } else if (requireNpcDialog) {
                syncAndLogNearArrived(targetX, targetY, source + ":npc-before-click");
            }

            log.info("mini-map pathing click attempt: source={} logical=({}, {}) pixel=({}, {})",
                    source, candidate.logicalX(), candidate.logicalY(),
                    candidate.pixelPoint().x, candidate.pixelPoint().y);

            if (!submitMiniMapClick(candidate.pixelPoint(), source)) {
                return MiniMapClickOutcome.FAILED;
            }

            if (!allowCandidateFallback) {
                log.info("mini-map pathing click submitted, defer first-attempt confirmation to navigation loop: source={}",
                        source);
                return MiniMapClickOutcome.PENDING_CONFIRMATION;
            }

            MiniMapPathingConfirm confirm = confirmMiniMapPathingStarted(source);
            if (confirm == MiniMapPathingConfirm.MOVING) {
                gameStateUtil.recordMovementIntent(source);
                if (releaseTurnOnPathing) {
                    releaseTaskTurnAfterPathing(source);
                }
                return MiniMapClickOutcome.PATHING_STARTED;
            }
            if (confirm == MiniMapPathingConfirm.DIALOG_OPENED) {
                log.info("mini-map pathing click opened dialog directly: source={}", source);
                return MiniMapClickOutcome.DIALOG_OPENED;
            }
            if (!requireNpcDialog && syncAndCheckArrived(targetX, targetY, source + ":after-confirm")) {
                return MiniMapClickOutcome.ARRIVED;
            } else if (requireNpcDialog) {
                syncAndLogNearArrived(targetX, targetY, source + ":npc-after-confirm");
            }
            checkpointTask();

            log.warn("mini-map pathing click produced no movement, try next candidate: source={} confirm={}",
                    source, confirm);
        }

        log.warn("mini-map pathing click exhausted candidates without movement: description={} attempts={}",
                description, candidates.size());
        return MiniMapClickOutcome.FAILED;
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
     * Refresh the minimap coordinate and log whether the window is near an NPC target without
     * allowing that proximity to complete navigation.
     *
     * <p>NPC-accept flows need a real mini-map click or an opened option dialog. A coordinate that
     * is merely within tolerance can still leave the player off-center, which makes the later yellow
     * text and Ctrl-menu click strategies unstable. This helper preserves the diagnostic arrival
     * evidence while keeping the NPC navigation pipeline moving.</p>
     *
     * @param targetX logical in-game X coordinate of the NPC.
     * @param targetY logical in-game Y coordinate of the NPC.
     * @param source diagnostic source written to the log.
     */
    private boolean syncAndLogNearArrived(int targetX, int targetY, String source) {
        playerStateService.syncMyPosition();
        PlayerCharacter me = context.getMe();
        boolean near = Math.abs(me.getX() - targetX) <= 2 && Math.abs(me.getY() - targetY) <= 2;
        if (near) {
            log.info("near NPC target but continue mini-map click: source={} current=({}, {}) target=({}, {})",
                    source, me.getX(), me.getY(), targetX, targetY);
        }
        return near;
    }

    /**
     * Fast path for NPC navigation.
     *
     * <p>When the mini-map click is aimed at an NPC, the useful completion signal is usually the
     * option dialog, not "stable stopped". Polling the full movement detector before every dialog
     * check can delay the task by several seconds after the dialog is already visible. This helper
     * keeps dialog checks frequent and only samples movement occasionally to escape quickly when the
     * click clearly did not start pathing.</p>
     */
    private boolean waitForExpectedNpcDialogAfterMiniMapClick(int targetX, int targetY) {
        long startedAt = System.currentTimeMillis();
        long deadline = startedAt + NPC_DIALOG_FAST_POLL_TIMEOUT_MS;
        long nextMovementCheckAt = startedAt + NPC_DIALOG_MOVEMENT_CHECK_INTERVAL_MS;
        while (System.currentTimeMillis() < deadline) {
            checkpointTask();
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }

            DialogService.DialogType dialogType = dialogService.detectDialogTypeNoFocus("navigateInCurrentMap:npc-dialog-fast");
            checkpointTask();
            if (dialogType != DialogService.DialogType.NONE) {
                long elapsedMs = System.currentTimeMillis() - startedAt;
                log.info("navigate in current map reached expected NPC dialog: target=({}, {}) type={} elapsedMs={}",
                        targetX, targetY, dialogType, elapsedMs);
                return true;
            }

            long now = System.currentTimeMillis();
            if (now >= nextMovementCheckAt) {
                long elapsedMs = now - startedAt;
                if (elapsedMs < NPC_DIALOG_MIN_WAIT_BEFORE_INACTIVE_FALLBACK_MS) {
                    log.info("expected NPC dialog still pending, skip heavy movement check during minimum wait: target=({}, {}) elapsedMs={}",
                            targetX, targetY, elapsedMs);
                    nextMovementCheckAt = now + NPC_DIALOG_MOVEMENT_CHECK_INTERVAL_MS;
                    continue;
                }
                GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
                checkpointTask();
                if (!isActiveNavigationMovement(movementState)) {
                    log.info("expected NPC dialog not open and movement is inactive, return to normal navigation loop: target=({}, {}) state={}",
                            targetX, targetY, movementState);
                    return false;
                }
                nextMovementCheckAt = now + NPC_DIALOG_MOVEMENT_CHECK_INTERVAL_MS;
            }

            if (!sleepInterruptible(NPC_DIALOG_FAST_POLL_INTERVAL_MS)) {
                return false;
            }
        }

        log.info("expected NPC dialog fast poll timed out, return to normal navigation loop: target=({}, {})",
                targetX, targetY);
        return false;
    }

    /**
     * Poll for the observable result of a mini-map click.
     *
     * @param source log label for the click attempt being confirmed.
     * @return MOVING when pixel/coordinate movement starts, DIALOG_OPENED when a dialog appears,
     *         NO_MOVEMENT on timeout, or INTERRUPTED when task stop interrupts polling.
     */
    private MiniMapPathingConfirm confirmMiniMapPathingStarted(String source) {
        long deadline = System.currentTimeMillis() + MINI_MAP_PATHING_CONFIRM_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            checkpointTask();
            if (Thread.currentThread().isInterrupted()) {
                return MiniMapPathingConfirm.INTERRUPTED;
            }

            DialogService.DialogType dialogType = dialogService.detectDialogTypeNoFocus(source + ":confirm-dialog");
            checkpointTask();
            if (dialogType != DialogService.DialogType.NONE) {
                log.info("mini-map pathing confirmation: dialog opened source={} type={}", source, dialogType);
                return MiniMapPathingConfirm.DIALOG_OPENED;
            }

            GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
            checkpointTask();
            if (movementState == GameStateUtil.MovementState.MOVING
                    || movementState == GameStateUtil.MovementState.MAYBE_MOVING) {
                log.info("mini-map pathing confirmation: movement detected source={} state={}",
                        source, movementState);
                return MiniMapPathingConfirm.MOVING;
            }

            log.info("mini-map pathing confirmation: no movement yet source={} state={}", source, movementState);
            if (!sleepInterruptible(MINI_MAP_PATHING_CONFIRM_POLL_MS)) {
                return MiniMapPathingConfirm.INTERRUPTED;
            }
            checkpointTask();
        }
        return MiniMapPathingConfirm.NO_MOVEMENT;
    }

    /**
     * Calculate a nearby logical approach coordinate for combat targets.
     *
     * <p>The returned coordinate is still a logical in-game map coordinate. It is offset two logical
     * steps toward the screen center/map interior so pathing stops near the monster/NPC instead of
     * exactly under its nameplate. If the map transform is unavailable, the original coordinate is
     * returned and logged as a fallback.</p>
     *
     * @param mapName map whose transform maps logical coordinates to screen pixels.
     * @param targetX original logical in-game X coordinate of the combat target.
     * @param targetY original logical in-game Y coordinate of the combat target.
     * @return approach coordinate to pass into normal mini-map navigation.
     */
    public MapCoordinate calculateCombatTargetApproach(String mapName, int targetX, int targetY) {
        Point originalPixelPoint = coordinateHelper.getPhysicalMapPoint(mapName, targetX, targetY);
        CoordinateHelper.MapTransform transform = coordinateHelper.getMapTransform(mapName);
        if (originalPixelPoint == null || transform == null) {
            log.warn("mini-map combat target approach fallback to original: map={} target=({}, {}) transformMissing={} pixelMissing={}",
                    mapName, targetX, targetY, transform == null, originalPixelPoint == null);
            return new MapCoordinate(targetX, targetY);
        }

        tracker.refreshWindowState();
        int relativeX = originalPixelPoint.x - tracker.getWindowBaseX();
        int relativeY = originalPixelPoint.y - tracker.getWindowBaseY();
        int pixelDirectionX = relativeX < GAME_CLIENT_WIDTH / 2 ? 1 : -1;
        int pixelDirectionY = relativeY < GAME_CLIENT_HEIGHT / 2 ? 1 : -1;
        int dx = logicalStepForPixelDirection(pixelDirectionX, transform.scaleX);
        int dy = logicalStepForPixelDirection(pixelDirectionY, transform.scaleY);
        int approachX = targetX + dx * COMBAT_TARGET_APPROACH_OFFSET;
        int approachY = targetY + dy * COMBAT_TARGET_APPROACH_OFFSET;

        log.info("mini-map combat target approach: map={} target=({}, {}) approach=({}, {}) relative=({}, {}) logicalStep=({}, {})",
                mapName, targetX, targetY, approachX, approachY, relativeX, relativeY, dx, dy);
        return new MapCoordinate(approachX, approachY);
    }

    /**
     * Build fallback logical coordinates for mini-map pathing.
     *
     * <p>The first candidate is always the requested target. Extra candidates only cover edge cases
     * where clicking the exact point does not start movement, such as map-border coordinates or tiny
     * click target inaccuracies. This method does not know task business semantics.</p>
     */
    private List<MiniMapClickCandidate> buildMiniMapClickCandidates(String mapName,
                                                                    int targetX,
                                                                    int targetY,
                                                                    Point originalPixelPoint) {
        List<MiniMapClickCandidate> candidates = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        CoordinateHelper.MapTransform transform = coordinateHelper.getMapTransform(mapName);
        if (transform == null) {
            addMiniMapCandidate(candidates, seen, mapName, targetX, targetY, originalPixelPoint, "original");
            return candidates;
        }

        tracker.refreshWindowState();
        int relativeX = originalPixelPoint.x - tracker.getWindowBaseX();
        int relativeY = originalPixelPoint.y - tracker.getWindowBaseY();

        addMiniMapCandidate(candidates, seen, mapName, targetX, targetY, originalPixelPoint, "original");

        int dx = 0;
        int dy = 0;
        if (relativeX <= MINI_MAP_EDGE_INSET_TRIGGER_PX) {
            dx = logicalStepForPixelDirection(1, transform.scaleX);
        } else if (GAME_CLIENT_WIDTH - relativeX <= MINI_MAP_EDGE_INSET_TRIGGER_PX) {
            dx = logicalStepForPixelDirection(-1, transform.scaleX);
        }
        if (relativeY <= MINI_MAP_EDGE_INSET_TRIGGER_PX) {
            dy = logicalStepForPixelDirection(1, transform.scaleY);
        } else if (GAME_CLIENT_HEIGHT - relativeY <= MINI_MAP_EDGE_INSET_TRIGGER_PX) {
            dy = logicalStepForPixelDirection(-1, transform.scaleY);
        }

        log.info("mini-map edge inset analysis: map={} target=({}, {}) pixel=({}, {}) relative=({}, {}) dx={} dy={}",
                mapName, targetX, targetY, originalPixelPoint.x, originalPixelPoint.y, relativeX, relativeY, dx, dy);

        if (dy != 0) {
            addMiniMapCandidate(candidates, seen, mapName, targetX, targetY + dy, null, "inset-y1");
            addMiniMapCandidate(candidates, seen, mapName, targetX, targetY + dy * 2, null, "inset-y2");
            addMiniMapCandidate(candidates, seen, mapName, targetX - 1, targetY + dy, null, "inset-y-left");
            addMiniMapCandidate(candidates, seen, mapName, targetX + 1, targetY + dy, null, "inset-y-right");
        }
        if (dx != 0) {
            addMiniMapCandidate(candidates, seen, mapName, targetX + dx, targetY, null, "inset-x1");
            addMiniMapCandidate(candidates, seen, mapName, targetX + dx * 2, targetY, null, "inset-x2");
            addMiniMapCandidate(candidates, seen, mapName, targetX + dx, targetY - 1, null, "inset-x-up");
            addMiniMapCandidate(candidates, seen, mapName, targetX + dx, targetY + 1, null, "inset-x-down");
        }
        if (dx != 0 && dy != 0) {
            addMiniMapCandidate(candidates, seen, mapName, targetX + dx, targetY + dy, null, "inset-corner1");
            addMiniMapCandidate(candidates, seen, mapName, targetX + dx * 2, targetY + dy * 2, null, "inset-corner2");
        }

        addMiniMapCandidate(candidates, seen, mapName, targetX, targetY - 1, null, "near-up");
        addMiniMapCandidate(candidates, seen, mapName, targetX, targetY + 1, null, "near-down");
        addMiniMapCandidate(candidates, seen, mapName, targetX - 1, targetY, null, "near-left");
        addMiniMapCandidate(candidates, seen, mapName, targetX + 1, targetY, null, "near-right");

        return candidates;
    }

    private void addMiniMapCandidate(List<MiniMapClickCandidate> candidates,
                                     Set<String> seen,
                                     String mapName,
                                     int logicalX,
                                     int logicalY,
                                     Point knownPixelPoint,
                                     String reason) {
        String key = logicalX + "," + logicalY;
        if (!seen.add(key)) {
            return;
        }
        Point pixelPoint = knownPixelPoint == null
                ? coordinateHelper.getPhysicalMapPoint(mapName, logicalX, logicalY)
                : knownPixelPoint;
        if (pixelPoint == null) {
            return;
        }
        candidates.add(new MiniMapClickCandidate(logicalX, logicalY, pixelPoint, reason));
    }

    private int logicalStepForPixelDirection(int pixelDirection, double scale) {
        if (scale == 0) {
            return 0;
        }
        return pixelDirection / scale > 0 ? 1 : -1;
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

    private void warnIfMapStillOpenAfterAlt1Close() {
        if (!sleepInterruptible(400)) {
            return;
        }
        if (isWorldMapOpened()) {
            log.warn("ensureMapTrackingOption pressed Alt+1 but map still appears open; later UI cleanup will handle it");
        }
    }

    private NavigationRuntimeState state() {
        String key = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
        return runtimeStates.computeIfAbsent(key, ignored -> new NavigationRuntimeState());
    }

    private String currentWindowId() {
        return windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
    }

    private String currentBoundHandle() {
        return windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getNativeBinding().getNativeHandle())
                .orElse(null);
    }

    private boolean sleepInterruptible(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void cleanLightweightInterruptions(String source) {
        if (!isAutoBattleRunningInCurrentWindow()) {
            return;
        }
        if (!teamRoleDetectionService.shouldRunLightweightCleanup((TaskExecutionContext) null)) {
            return;
        }
        NavigationRuntimeState state = state();
        long now = System.currentTimeMillis();
        if (state.lastLightweightCleanAt > 0
                && now - state.lastLightweightCleanAt < LIGHTWEIGHT_CLEAN_INTERVAL_MS) {
            return;
        }
        state.lastLightweightCleanAt = now;
        if (uiCleanerService.cleanLightweightInterruptions("navigation:" + source)) {
            log.info("navigation lightweight cleanup handled interruption: source={}", source);
        }
    }

    private boolean isAutoBattleRunningInCurrentWindow() {
        return windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getLastTaskType() == TaskType.AUTO_BATTLE)
                .orElse(false);
    }

    private static class NavigationRuntimeState {
        private int lastAbsoluteLogicalX = DEFAULT_LOGICAL_COORDINATE;
        private int lastAbsoluteLogicalY = DEFAULT_LOGICAL_COORDINATE;
        private long lastLightweightCleanAt = 0L;
    }

    private record MiniMapClickCandidate(int logicalX, int logicalY, Point pixelPoint, String reason) {
    }

    private enum MiniMapPathingConfirm {
        MOVING,
        DIALOG_OPENED,
        NO_MOVEMENT,
        INTERRUPTED
    }

    private enum MiniMapClickOutcome {
        ARRIVED,
        PATHING_STARTED,
        DIALOG_OPENED,
        PENDING_CONFIRMATION,
        FAILED
    }

    private enum RouteClickStatus {
        CLICKED,
        NOT_FOUND,
        DESTINATION_MISMATCH
    }
}
