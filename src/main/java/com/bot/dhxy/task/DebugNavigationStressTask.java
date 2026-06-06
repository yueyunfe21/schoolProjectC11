package com.bot.dhxy.task;

import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogPreparationStatus;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Debug-only navigation pressure task for multi-window task-turn validation.
 *
 * <p>Each window receives the same five fixed destination points as a Five-ring-style route chain,
 * but the task cuts out the left tracker/template business logic. Each fixed destination is wrapped
 * as a {@link NavigationRequest} and sent through the production {@link NavigationService}, so route
 * selection, transfer dialogs, mini-map clicks, and arrival checks stay on the validated navigation
 * path. Whenever navigation reports {@link TaskTransactionResult#PATHING_STARTED}, this task yields
 * so registered windows can stress input serialization and handoff behavior without accepting or
 * completing a real business task.</p>
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class DebugNavigationStressTask implements GameTask {

    private static final String TASK_CODE = "debug_navigation_stress";
    private static final String TASK_NAME = "导航压力测试";
    private static final long PATHING_HANDOFF_DELAY_MS = 250L;
    private static final long RETRYABLE_ERROR_BACKOFF_MS = 3_000L;
    private static final long PATHING_RECHECK_GRACE_MS = 2_000L;
    private static final long PATHING_OBSERVER_FAST_WAIT_MS = 2_500L;
    private static final long PATHING_POSITION_SYNC_INTERVAL_MS = 1_500L;
    private static final long PATHING_STATIONARY_RETRY_MS = 5_000L;
    private static final long MAP_LEG_STATIONARY_LOG_INTERVAL_MS = 5_000L;
    private static final long COORDINATE_LEG_CROSS_MAP_GRACE_MS = 30_000L;
    private static final long PATHING_TARGET_WAIT_TIMEOUT_MS = 90_000L;
    private static final long ROUTE_DIALOG_REQUESTED_WAIT_TIMEOUT_MS = 3_000L;
    private static final long ROUTE_DIALOG_PREPARING_WAIT_TIMEOUT_MS = 10_000L;
    private static final long ROUTE_DIALOG_FAILED_RETRY_MS = 3_000L;
    private static final long PAUSE_TIMER_COMPENSATION_THRESHOLD_MS = 1_000L;
    private static final long OBSERVER_SNAPSHOT_MAX_AGE_MS = 3_000L;
    private static final int TARGET_REACHED_TOLERANCE = 5;
    private static final int MAX_LOOP_GUARD = 600;
    private static final int MAX_NAVIGATION_RETRY = 1;

    private static final List<TargetSpec> TARGET_SPECS = List.of(
            new TargetSpec("长安", 216, 129),
            new TargetSpec("长安城东", 166, 118),
            new TargetSpec("大唐边境", 137, 121),
            new TargetSpec("龙宫", 110, 54),
            new TargetSpec("大雁塔二层", 76, 73)
    );

    private final NavigationService navigationService;
    private final PlayerStateService playerStateService;
    private final TaskTransactionRunner taskTransactionRunner;
    private final GameStateUtil gameStateUtil;
    private final WindowTaskContextHolder windowTaskContextHolder;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    /**
     * Run the fixed five-target 五环 route probe for the current bound window.
     *
     * @param executionContext nullable runner context. When absent, the task still runs with a
     *                         minimal debug context so stop checks and logs have task identity.
     * @return SUCCESS when every generated target is reached, STOPPED on cooperative stop, or FAILED
     *         when one target repeatedly cannot start or verify navigation.
     */
    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        TaskExecutionContext context = resolveExecutionContext(executionContext);
        NavigationStressState state = new NavigationStressState(generateTargets(context));
        int loopGuard = 0;

        log.info("{} [nav-stress] start targets={}", context.getLogPrefix(), state.describeTargets());
        try {
            while (!state.isFinished()) {
                TaskCheckpoint.throwIfStopRequested(context, "Navigation stress task interrupted");
                /*
                 * Pathing wait loops are expected to poll the background observer every 250ms while
                 * the game client is walking or loading a new map. They already have a wall-clock
                 * timeout, so counting those polls here makes a healthy long route fail with a fake
                 * loop-guard error. Keep this guard only for non-waiting navigation state churn.
                 */
                if (!state.isWaitingOutsideTurn() && ++loopGuard > MAX_LOOP_GUARD) {
                    log.error("{} [nav-stress] loop guard exceeded: index={} waitingPathing={} waitingRouteDialog={} target={}",
                            context.getLogPrefix(), state.targetIndex,
                            state.waitingPathing, state.waitingRouteDialogPreparation, state.currentTargetText());
                    return TaskRunResult.FAILED;
                }

                String transactionName = transactionName(state);
                String transactionTarget = state.currentTargetText();
                long sinceLastYieldMs = state.lastYieldAt <= 0L
                        ? -1L
                        : Math.max(0L, System.currentTimeMillis() - state.lastYieldAt);
                long transactionStartedAt = System.currentTimeMillis();
                TaskTransactionOutcome outcome;
                if (state.isWaitingOutsideTurn()) {
                    /*
                     * Once a route click has started movement, this probe should not compete for
                     * the coarse task turn just to poll the watcher. Otherwise five windows create
                     * noisy 250ms turn churn while nobody is doing real input. If polling discovers
                     * that navigation must be retried, the next navigate phase will request input
                     * normally.
                     */
                    outcome = runWaitPathingWithoutTaskTurn(context, state, transactionName);
                } else {
                    log.info("{} [nav-stress-timeline] requestTurn transaction={} target={} waitingPathing={} waitingRouteDialog={} sinceLastYieldMs={}",
                            context.getLogPrefix(), transactionName, transactionTarget,
                            state.waitingPathing, state.waitingRouteDialogPreparation, sinceLastYieldMs);
                    outcome = runNavigationWithoutTaskTurn(context, state, transactionName);
                }
                long transactionElapsedMs = Math.max(0L, System.currentTimeMillis() - transactionStartedAt);
                log.info("{} [nav-stress-latency] transaction={} target={} result={} yield={} totalMs={} sinceLastYieldMs={}",
                        context.getLogPrefix(), transactionName, transactionTarget,
                        outcome.result(), outcome.yieldPolicy(), transactionElapsedMs, sinceLastYieldMs);

                if (outcome.result() == TaskTransactionResult.STOPPED) {
                    return TaskRunResult.STOPPED;
                }
                if (outcome.result() == TaskTransactionResult.FAILED) {
                    return TaskRunResult.FAILED;
                }
                long pauseAfterYieldMs = pauseAfterYieldMs(outcome.result(), outcome.yieldPolicy());
                if (pauseAfterYieldMs > 0L) {
                    state.lastYieldAt = System.currentTimeMillis();
                    log.info("{} [nav-stress-latency] yielding after transaction={} delayMs={}",
                            context.getLogPrefix(), transactionName, pauseAfterYieldMs);
                    TaskSleep.sleepOrStop(context, pauseAfterYieldMs,
                            "Navigation stress task interrupted");
                }
            }

            log.info("{} [nav-stress] finished all targets", context.getLogPrefix());
            return TaskRunResult.SUCCESS;
        } finally {
            taskTransactionRunner.forceReleaseTurn("debug-navigation-stress:execute-finished");
        }
    }

    @Override
    public void stop() {
        log.info("[nav-stress] stop requested");
    }

    private TaskTransactionResult navigateNextTarget(TaskExecutionContext context, NavigationStressState state) {
        TaskCheckpoint.throwIfStopRequested(context, "Navigation stress task interrupted");
        NavigationTarget target = state.currentTarget();
        if (target == null) {
            return TaskTransactionResult.TASK_FINISHED;
        }

        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot preNavigationSnapshot = runtime == null ? null : runtime.getPathingSnapshot();
        if (preNavigationSnapshot != null && preNavigationSnapshot.getIntent() != null) {
            WindowPathingIntent intent = preNavigationSnapshot.getIntent();
            boolean sameIntentMap = gameStateUtil.isSameMapName(intent.getTargetMapName(), target.mapName);
            boolean coordinateIntent = intent.getTargetX() != null && intent.getTargetY() != null;
            boolean sameIntentCoordinate = !coordinateIntent
                    || (intent.getTargetX() == target.x && intent.getTargetY() == target.y);
            long snapshotAgeMs = Math.max(0L, System.currentTimeMillis() - preNavigationSnapshot.getUpdatedAtMs());
            if (sameIntentMap && sameIntentCoordinate && snapshotAgeMs <= OBSERVER_SNAPSHOT_MAX_AGE_MS) {
                WindowPathingState observedState = preNavigationSnapshot.getState();
                int arrivalTolerance = observerTolerance(preNavigationSnapshot);
                boolean hasPreNavigationCoordinate = preNavigationSnapshot.getCurrentMapName() != null
                        && preNavigationSnapshot.getCurrentX() != null
                        && preNavigationSnapshot.getCurrentY() != null;
                boolean nearTarget = hasPreNavigationCoordinate && gameStateUtil.isNearCoordinate(
                        preNavigationSnapshot.getCurrentMapName(),
                        preNavigationSnapshot.getCurrentX(),
                        preNavigationSnapshot.getCurrentY(),
                        target.mapName,
                        target.x,
                        target.y,
                        arrivalTolerance);
                if ((coordinateIntent && observedState == WindowPathingState.ARRIVED) || nearTarget) {
                    log.info("{} [nav-stress-latency] consume watcher terminal state before navigation: target={} state={} current={}({}, {}) ageMs={} tolerance={}",
                            context.getLogPrefix(), target, observedState,
                            preNavigationSnapshot.getCurrentMapName(),
                            preNavigationSnapshot.getCurrentX(),
                            preNavigationSnapshot.getCurrentY(),
                            snapshotAgeMs, arrivalTolerance);
                    runtime.clearPathingSignal("nav stress consumed terminal snapshot before navigation");
                    state.completeCurrentTarget(context);
                    return state.isFinished() ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE;
                }
                if (observedState == WindowPathingState.ARRIVED) {
                    /*
                     * A map-only ARRIVED snapshot means the route reached the target map, not the
                     * final logical coordinate. Clear it and let NavigationService continue with the
                     * current-map coordinate click instead of completing the target at the map edge.
                     */
                    log.info("{} [nav-stress-latency] consume map-only watcher arrival before coordinate navigation: target={} current={}({}, {}) ageMs={} tolerance={}",
                            context.getLogPrefix(), target,
                            preNavigationSnapshot.getCurrentMapName(),
                            preNavigationSnapshot.getCurrentX(),
                            preNavigationSnapshot.getCurrentY(),
                            snapshotAgeMs, arrivalTolerance);
                    runtime.clearPathingSignal("nav stress consumed map-only arrival before coordinate navigation");
                    DialogPreparationStatus status = runtime.getDialogPreparationStatus();
                    PreparedDialogAction action = runtime.getPreparedDialogAction();
                    if ((status != null && status.matches(DialogOperation.ROUTE_TRANSFER, target.mapName))
                            || (action != null && action.matches(DialogOperation.ROUTE_TRANSFER, target.mapName))) {
                        runtime.clearDialogPreparationRequest(
                                "nav stress consumed map arrival before coordinate navigation");
                    }
                }
                if (observedState == WindowPathingState.STOPPED_AWAY) {
                    /*
                     * The background watcher is the authority for a released route leg. If it has
                     * already proved that this same target stopped away, do not submit a fresh
                     * navigation action as if the previous state were still ACTIVE. End the local
                     * wait state and retry from the stopped coordinate immediately.
                     */
                    log.info("{} [nav-stress-latency] consume watcher stopped-away before navigation retry: target={} current={}({}, {}) ageMs={}",
                            context.getLogPrefix(), target,
                            preNavigationSnapshot.getCurrentMapName(),
                            preNavigationSnapshot.getCurrentX(),
                            preNavigationSnapshot.getCurrentY(),
                            snapshotAgeMs);
                    runtime.clearPathingSignal("nav stress consumed stopped-away before navigation retry");
                    state.finishWaitingForPathing();
                }
            }
        }

        long navigationStartedAt = System.currentTimeMillis();
        log.info("{} [nav-stress-timeline] actionStart kind=navigate target={} retry={}/{}",
                context.getLogPrefix(), target, state.navigationRetryCount, MAX_NAVIGATION_RETRY);
        String source = "debug-nav-stress:" + target.sequence;
        NavigationRequest request = target.toNavigationRequest(source);
        NavigationResult result = navigationService.navigateToNPC(request);
        long navigationElapsedMs = Math.max(0L, System.currentTimeMillis() - navigationStartedAt);
        NavigationResultStatus status = result.getStatus();
        log.info("{} [nav-stress-latency] productionNavigate target={} status={} elapsedMs={} retry={}/{} message={}",
                context.getLogPrefix(), target, status, navigationElapsedMs,
                state.navigationRetryCount, MAX_NAVIGATION_RETRY, result.getMessage());

        if (status == NavigationResultStatus.PATHING_STARTED) {
            String pathingStartMap = currentCachedMap();
            boolean startedAcrossMap = inferStartedAcrossMap(result.getMessage(), pathingStartMap, target);
            log.info("{} [nav-stress-timeline] pathingConfirmed target={} startMap={} rawStartMap={} currentAfterClick={} acrossMap={} message={} elapsedMs={} next=yield",
                    context.getLogPrefix(), target, pathingStartMap, pathingStartMap,
                    formatPosition(null, currentCachedPlayer()), startedAcrossMap, result.getMessage(), navigationElapsedMs);
            state.waitingPathing = true;
            state.waitingRouteDialogPreparation = false;
            state.deferFirstMovementProbe = true;
            state.pathingStartedAt = System.currentTimeMillis();
            state.pathingStartedFromMap = pathingStartMap;
            state.pathingStartedAcrossMap = startedAcrossMap;
            state.navigationRetryCount = 0;
            return TaskTransactionResult.PATHING_STARTED;
        }
        if (status == NavigationResultStatus.DIALOG_PREPARING) {
            log.info("{} [nav-stress-latency] route dialog preparing in background; wait without retaking turn: target={} elapsedMs={}",
                    context.getLogPrefix(), target, navigationElapsedMs);
            state.waitingPathing = false;
            state.waitingRouteDialogPreparation = true;
            state.routeDialogPreparationStartedAt = System.currentTimeMillis();
            state.pathingStartedAt = state.routeDialogPreparationStartedAt;
            state.navigationRetryCount = 0;
            return TaskTransactionResult.PATHING_STARTED;
        }
        if (status == NavigationResultStatus.ARRIVED || result.success()) {
            state.completeCurrentTarget(context);
            return state.isFinished() ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE;
        }
        if (status == NavigationResultStatus.STOPPED || status == NavigationResultStatus.INTERRUPTED) {
            return TaskTransactionResult.STOPPED;
        }

        state.navigationRetryCount++;
        if (state.navigationRetryCount > MAX_NAVIGATION_RETRY) {
            log.error("{} [nav-stress] target failed after retries: target={} status={} message={}",
                    context.getLogPrefix(), target, status, result.getMessage());
            return TaskTransactionResult.FAILED;
        }
        return TaskTransactionResult.RETRYABLE_ERROR;
    }

    private PlayerCharacter currentCachedPlayer() {
        return windowTaskContextHolder.rawCurrent()
                .map(runtime -> runtime.getGameState().getMe())
                .orElse(null);
    }

    private String currentCachedMap() {
        PlayerCharacter cached = currentCachedPlayer();
        return cached == null ? null : cached.getCurrentMapName();
    }

    private boolean inferStartedAcrossMap(String message, String pathingStartMap, NavigationTarget target) {
        String normalizedMessage = message == null ? "" : message.toLowerCase();
        if (normalizedMessage.contains("current-map")
                || normalizedMessage.contains("mini-map coordinate")
                || normalizedMessage.contains("exact mini-map")) {
            return false;
        }
        if (normalizedMessage.contains("map route") || normalizedMessage.contains("route dialog")) {
            return true;
        }
        return pathingStartMap != null
                && !pathingStartMap.isBlank()
                && !gameStateUtil.isSameMapName(pathingStartMap, target.mapName);
    }

    /**
     * Run the real NavigationService call outside the task-turn lock for this debug probe.
     *
     * <p>The navigation algorithm and all physical input still stay inside the normal
     * skipped here so this stress task can measure whether other windows can prepare/queue their
     * navigation while the current window is doing OCR or post-click confirmation.</p>
     *
     * @param context current debug task execution context.
     * @param state per-window stress state that records the active target and pathing phase.
     * @param transactionName diagnostic name used in the same timeline format as normal
     *                        task-turn transactions.
     * @return transaction-like outcome so the outer loop can keep the same stop/failure/yield logic.
     */
    private TaskTransactionOutcome runNavigationWithoutTaskTurn(TaskExecutionContext context,
                                                                NavigationStressState state,
                                                                String transactionName) {
        long startedAt = System.currentTimeMillis();
        TaskTransactionResult result = TaskTransactionResult.FAILED;
        boolean completed = false;
        log.info("{} [nav-stress-timeline] outsideTurnStart transaction={} target={}",
                context.getLogPrefix(), transactionName, state.currentTargetText());
        try {
            result = navigateNextTarget(context, state);
            completed = true;
            return new TaskTransactionOutcome(
                    transactionName,
                    TaskTransactionResult.READY_TO_CONTINUE,
                    TaskYieldPolicy.MUST_YIELD,
                    result,
                    true);
        } catch (TaskStopRequestedException e) {
            result = TaskTransactionResult.STOPPED;
            completed = true;
            return new TaskTransactionOutcome(
                    transactionName,
                    TaskTransactionResult.READY_TO_CONTINUE,
                    TaskYieldPolicy.MUST_YIELD,
                    result,
                    true);
        } catch (RuntimeException e) {
            if (Thread.currentThread().isInterrupted()) {
                result = TaskTransactionResult.STOPPED;
                completed = true;
                return new TaskTransactionOutcome(
                        transactionName,
                        TaskTransactionResult.READY_TO_CONTINUE,
                        TaskYieldPolicy.MUST_YIELD,
                        result,
                        true);
            }
            throw e;
        } finally {
            long elapsedMs = Math.max(0L, System.currentTimeMillis() - startedAt);
            log.info("{} [nav-stress-latency] outsideTurnEnd transaction={} target={} result={} completed={} elapsedMs={}",
                    context.getLogPrefix(), transactionName, state.currentTargetText(),
                    result, completed, elapsedMs);
        }
    }

    /**
     * Poll route progress without taking the coarse task turn.
     *
     * <p>After the debug route click has been submitted, the only expected work is HWND screenshot
     * observation, cached coordinate checks, or a throttled fallback sync. None of those should stop
     * other windows from starting their own navigation. This method returns the same outcome shape as
     * a normal transaction so the outer state machine can still handle completion, retry, and stop in
     * one place.</p>
     *
     * @param context current debug task execution context.
     * @param state per-window stress state with the active target and pathing timestamps.
     * @param transactionName diagnostic name for timeline logs.
     * @return transaction-like outcome; PATHING_STARTED means keep waiting without owning the turn.
     */
    private TaskTransactionOutcome runWaitPathingWithoutTaskTurn(TaskExecutionContext context,
                                                                 NavigationStressState state,
                                                                 String transactionName) {
        long startedAt = System.currentTimeMillis();
        TaskTransactionResult result = TaskTransactionResult.FAILED;
        boolean completed = false;
        log.info("{} [nav-stress-timeline] pathWaitOutsideTurnStart transaction={} target={} ageMs={} phase={}",
                context.getLogPrefix(), transactionName, state.currentTargetText(),
                state.pathingAgeMs(), state.waitingRouteDialogPreparation
                        ? "route-dialog-preparing"
                        : (state.pathingStartedAcrossMap ? "map-leg" : "coordinate-leg"));
        try {
            result = waitForPathing(context, state);
            completed = true;
            return new TaskTransactionOutcome(
                    transactionName,
                    TaskTransactionResult.READY_TO_CONTINUE,
                    TaskYieldPolicy.MUST_YIELD,
                    result,
                    true);
        } catch (TaskStopRequestedException e) {
            result = TaskTransactionResult.STOPPED;
            completed = true;
            return new TaskTransactionOutcome(
                    transactionName,
                    TaskTransactionResult.READY_TO_CONTINUE,
                    TaskYieldPolicy.MUST_YIELD,
                    result,
                    true);
        } catch (RuntimeException e) {
            if (Thread.currentThread().isInterrupted()) {
                result = TaskTransactionResult.STOPPED;
                completed = true;
                return new TaskTransactionOutcome(
                        transactionName,
                        TaskTransactionResult.READY_TO_CONTINUE,
                        TaskYieldPolicy.MUST_YIELD,
                        result,
                        true);
            }
            throw e;
        } finally {
            long elapsedMs = Math.max(0L, System.currentTimeMillis() - startedAt);
            log.info("{} [nav-stress-timeline] pathWaitOutsideTurnEnd transaction={} target={} result={} completed={} elapsedMs={} totalPathingAgeMs={}",
                    context.getLogPrefix(), transactionName, state.currentTargetText(),
                    result, completed, elapsedMs, state.pathingAgeMs());
        }
    }

    private TaskTransactionResult waitForPathing(TaskExecutionContext context, NavigationStressState state) {
        long checkpointStartedAt = System.currentTimeMillis();
        TaskCheckpoint.throwIfStopRequested(context, "Navigation stress task interrupted");
        long checkpointBlockedMs = Math.max(0L, System.currentTimeMillis() - checkpointStartedAt);
        if (checkpointBlockedMs >= PAUSE_TIMER_COMPENSATION_THRESHOLD_MS) {
            /*
             * A user pause can block this checkpoint for minutes. That wall-clock gap must not
             * count toward the route wait timeout, otherwise resume immediately fails even though
             * the task was intentionally frozen.
             */
            state.compensatePausedDuration(checkpointBlockedMs);
            log.info("{} [nav-stress] pathing wait timer paused: blockedMs={} adjustedPathingStartedAt={} adjustedLastYieldAt={} adjustedLastSyncAt={}",
                    context.getLogPrefix(), checkpointBlockedMs, state.pathingStartedAt,
                    state.lastYieldAt, state.lastPathingSyncAt);
        }
        NavigationTarget target = state.currentTarget();
        if (target == null) {
            return TaskTransactionResult.TASK_FINISHED;
        }
        if (state.waitingRouteDialogPreparation) {
            return waitForRouteDialogPreparation(context, state, target);
        }

        long waitStartedAt = System.currentTimeMillis();
        long pathingAgeMs = state.pathingStartedAt <= 0L
                ? Long.MAX_VALUE
                : Math.max(0L, waitStartedAt - state.pathingStartedAt);
        log.info("{} [nav-stress-timeline] actionStart kind=waitPathing target={} pathingAgeMs={}",
                context.getLogPrefix(), target, pathingAgeMs);

        if (state.deferFirstMovementProbe) {
            state.deferFirstMovementProbe = false;
            state.pathingStartedAt = System.currentTimeMillis();
            log.info("{} [nav-stress-latency] defer first movement probe to release task turn quickly: target={}",
                    context.getLogPrefix(), target);
            return TaskTransactionResult.PATHING_STARTED;
        }

        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        PreparedDialogAction preparedAction = runtime == null ? null : runtime.getPreparedDialogAction();
        if (preparedAction != null && preparedAction.matches(DialogOperation.ROUTE_TRANSFER, target.mapName)) {
            /*
             * A route dialog candidate means the map route has already stopped at a selectable
             * transfer dialog. Treat it as the terminal signal for this released pathing leg and
             * immediately return to NavigationService so it can click the cached option before
             * another window starts a fresh world-map search.
             */
            log.info("{} [nav-stress-latency] prepared route dialog interrupts pathing wait; re-enter navigation: "
                            + "target={} matched={} click=({}, {}) verifiedAgeMs={}",
                    context.getLogPrefix(), target, preparedAction.getMatchedText(),
                    preparedAction.getAbsoluteX(), preparedAction.getAbsoluteY(),
                    Math.max(0L, System.currentTimeMillis() - preparedAction.getLastVerifiedAtMs()));
            state.finishWaitingForPathing();
            return TaskTransactionResult.READY_TO_CONTINUE;
        }
        WindowPathingSnapshot pathingSnapshot = runtime == null ? null : runtime.getPathingSnapshot();
        if (pathingSnapshot != null && pathingSnapshot.getIntent() != null) {
            WindowPathingIntent intent = pathingSnapshot.getIntent();
            boolean sameIntentMap = gameStateUtil.isSameMapName(intent.getTargetMapName(), target.mapName);
            boolean coordinateIntent = intent.getTargetX() != null && intent.getTargetY() != null;
            boolean sameIntentCoordinate = !coordinateIntent
                    || (intent.getTargetX() == target.x && intent.getTargetY() == target.y);
            if (sameIntentMap && sameIntentCoordinate) {
                WindowPathingState observedState = pathingSnapshot.getState();
                int arrivalTolerance = observerTolerance(pathingSnapshot);
                long snapshotAgeMs = Math.max(0L, System.currentTimeMillis() - pathingSnapshot.getUpdatedAtMs());
                boolean observedChanged = state.lastObservedPathingState != observedState
                        || state.lastObservedPathingUpdatedAtMs != pathingSnapshot.getUpdatedAtMs();
                if (observedChanged || observedState != WindowPathingState.ACTIVE) {
                    log.info("{} [nav-stress-observer] target={} state={} coordinateIntent={} current={}({}, {}) message={} snapshotAgeMs={}",
                            context.getLogPrefix(), target, observedState, coordinateIntent,
                            pathingSnapshot.getCurrentMapName(), pathingSnapshot.getCurrentX(), pathingSnapshot.getCurrentY(),
                            pathingSnapshot.getMessage(),
                            snapshotAgeMs);
                    state.lastObservedPathingState = observedState;
                    state.lastObservedPathingUpdatedAtMs = pathingSnapshot.getUpdatedAtMs();
                }
                /*
                 * The watcher can have a near-target coordinate before its state is consumed as
                 * ARRIVED by this task loop. Treat that coordinate as terminal immediately so we do
                 * not submit another mini-map click just because the state still reads ACTIVE.
                 */
                boolean hasObserverCoordinate = pathingSnapshot.getCurrentMapName() != null
                        && pathingSnapshot.getCurrentX() != null
                        && pathingSnapshot.getCurrentY() != null;
                boolean observerNearTarget = hasObserverCoordinate
                        && gameStateUtil.isNearCoordinate(
                        pathingSnapshot.getCurrentMapName(),
                        pathingSnapshot.getCurrentX(),
                        pathingSnapshot.getCurrentY(),
                        target.mapName,
                        target.x,
                        target.y,
                        arrivalTolerance);
                if (coordinateIntent && observerNearTarget && !pathingSnapshot.isProbeInProgress()) {
                    log.info("{} [nav-stress-latency] target reached by active watcher coordinate: target={} state={} current={}({}, {}) tolerance={}",
                            context.getLogPrefix(), target, observedState,
                            pathingSnapshot.getCurrentMapName(),
                            pathingSnapshot.getCurrentX(),
                            pathingSnapshot.getCurrentY(),
                            arrivalTolerance);
                    runtime.clearPathingSignal("nav stress consumed watcher coordinate near target");
                    state.completeCurrentTarget(context);
                    return state.isFinished() ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE;
                }

                /*
                 * ARRIVED snapshots are terminal and the watcher stops refreshing them. If the task
                 * misses the first fresh read, consuming the old arrival later can make this debug
                 * task believe a map leg has arrived while NavigationService's real scan still sees
                 * the previous map. Clear stale terminal signals and let the normal fallback decide.
                 */
                if (pathingSnapshot.isProbeInProgress()) {
                    return TaskTransactionResult.PATHING_STARTED;
                }
                if (snapshotAgeMs > OBSERVER_SNAPSHOT_MAX_AGE_MS) {
                    if (observedState == WindowPathingState.ARRIVED
                            || observedState == WindowPathingState.STOPPED_AWAY) {
                        log.warn("{} [nav-stress-observer] ignored stale terminal snapshot: target={} state={} ageMs={} maxAgeMs={} current={}({}, {})",
                                context.getLogPrefix(), target, observedState, snapshotAgeMs, OBSERVER_SNAPSHOT_MAX_AGE_MS,
                                pathingSnapshot.getCurrentMapName(), pathingSnapshot.getCurrentX(), pathingSnapshot.getCurrentY());
                        runtime.clearPathingSignal("nav stress ignored stale terminal pathing snapshot");
                    }
                } else
                if (observedState == WindowPathingState.ARRIVED) {
                    if (coordinateIntent) {
                        log.info("{} [nav-stress-latency] target reached by window pathing observer: target={} current={}({}, {})",
                                context.getLogPrefix(), target,
                                pathingSnapshot.getCurrentMapName(), pathingSnapshot.getCurrentX(), pathingSnapshot.getCurrentY());
                        runtime.clearPathingSignal("nav stress consumed coordinate arrival");
                        state.completeCurrentTarget(context);
                        return state.isFinished() ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE;
                    }
                    /*
                     * Map-only pathing proves the map transition, not the final NPC coordinate.
                     * Re-enter NavigationService so it can continue with the current-map click.
                     */
                    log.info("{} [nav-stress-latency] map leg arrived by window pathing observer; resume coordinate navigation: target={} current={}({}, {})",
                            context.getLogPrefix(), target,
                            pathingSnapshot.getCurrentMapName(), pathingSnapshot.getCurrentX(), pathingSnapshot.getCurrentY());
                    runtime.clearPathingSignal("nav stress consumed map arrival");
                    state.finishWaitingForPathing();
                    return TaskTransactionResult.READY_TO_CONTINUE;
                }

                if (observedState == WindowPathingState.ACTIVE) {
                    if (pathingAgeMs >= PATHING_TARGET_WAIT_TIMEOUT_MS) {
                        log.error("{} [nav-stress] pathing wait timeout while observer still reports ACTIVE: target={} ageMs={} timeoutMs={}",
                                context.getLogPrefix(), target, pathingAgeMs, PATHING_TARGET_WAIT_TIMEOUT_MS);
                        return TaskTransactionResult.FAILED;
                    }
                    boolean hasObservedPosition = pathingSnapshot.getCurrentMapName() != null
                            && pathingSnapshot.getCurrentX() != null
                            && pathingSnapshot.getCurrentY() != null;
                    long stationaryMs = pathingSnapshot.getLocationChangedAtMs() <= 0L
                            ? pathingAgeMs
                            : Math.max(0L, System.currentTimeMillis() - pathingSnapshot.getLocationChangedAtMs());
                    long observedStationaryMs = pathingSnapshot.getLocationChangedAtMs() <= 0L
                            ? 0L
                            : Math.max(0L, pathingSnapshot.getUpdatedAtMs() - pathingSnapshot.getLocationChangedAtMs());
                    long probeMs = pathingSnapshot.getProbeFinishedAtMs() > 0L
                            && pathingSnapshot.getProbeStartedAtMs() > 0L
                            ? Math.max(0L, pathingSnapshot.getProbeFinishedAtMs() - pathingSnapshot.getProbeStartedAtMs())
                            : 0L;
                    /*
                     * ACTIVE without a coordinate only proves the background watcher has not
                     * produced a readable mini-map sample yet. It must not clear the pathing signal:
                     * in five-window runs a slow HWND capture can report ARRIVED several seconds
                     * later, and the task must still be waiting to consume that terminal snapshot.
                     */
                    if (!hasObservedPosition) {
                        return TaskTransactionResult.PATHING_STARTED;
                    }
                    boolean sameMapAsTarget = gameStateUtil.isSameMapName(
                            pathingSnapshot.getCurrentMapName(), target.mapName);
                    if (coordinateIntent && !sameMapAsTarget) {
                        /*
                         * Current-map coordinate navigation can temporarily leave the target map
                         * because the game client may choose a transfer route by itself. A stale
                         * ACTIVE snapshot on an intermediate map is not enough evidence to reopen
                         * the world map.
                         */
                        long now = System.currentTimeMillis();
                        if (state.lastCoordinateLegTransitLogAt <= 0L
                                || now - state.lastCoordinateLegTransitLogAt >= MAP_LEG_STATIONARY_LOG_INTERVAL_MS) {
                            state.lastCoordinateLegTransitLogAt = now;
                            log.info("{} [nav-stress-latency] coordinate-leg active on off-target map; keep waiting for map transit: target={} current={}({}, {}) pathingAgeMs={} graceMs={} snapshotAgeMs={} probeInProgress={} probeMs={} observedStationaryMs={} wallStationaryMs={}",
                                    context.getLogPrefix(), target,
                                    pathingSnapshot.getCurrentMapName(),
                                    pathingSnapshot.getCurrentX(),
                                    pathingSnapshot.getCurrentY(),
                                    pathingAgeMs,
                                    COORDINATE_LEG_CROSS_MAP_GRACE_MS,
                                    snapshotAgeMs,
                                    pathingSnapshot.isProbeInProgress(),
                                    probeMs,
                                    observedStationaryMs,
                                    stationaryMs);
                        }
                        if (pathingAgeMs < COORDINATE_LEG_CROSS_MAP_GRACE_MS
                                || pathingSnapshot.isProbeInProgress()
                                || snapshotAgeMs > OBSERVER_SNAPSHOT_MAX_AGE_MS) {
                            return TaskTransactionResult.PATHING_STARTED;
                        }
                    }
                    if (hasObservedPosition
                            && !coordinateIntent
                            && state.pathingStartedAcrossMap
                            && observedStationaryMs >= PATHING_STATIONARY_RETRY_MS) {
                        /*
                         * Map legs can legitimately pause on intermediate maps such as 大雁塔一层
                         * while the route/transfer continues to 大雁塔二层. Re-opening the world map
                         * from an ACTIVE map-leg snapshot duplicates input and can interrupt the
                         * route. Let the watcher produce ARRIVED/STOPPED_AWAY or hit the global
                         * wait timeout instead.
                         */
                        long now = System.currentTimeMillis();
                        if (state.lastMapLegStationaryLogAt <= 0L
                                || now - state.lastMapLegStationaryLogAt >= MAP_LEG_STATIONARY_LOG_INTERVAL_MS) {
                            state.lastMapLegStationaryLogAt = now;
                            log.info("{} [nav-stress-latency] map-leg active position stalled but still waiting for observer terminal state: target={} current={}({}, {}) stationaryMs={} retryAfterMs={} timeoutMs={}",
                                    context.getLogPrefix(), target,
                                    pathingSnapshot.getCurrentMapName(),
                                    pathingSnapshot.getCurrentX(),
                                    pathingSnapshot.getCurrentY(),
                                    observedStationaryMs,
                                    PATHING_STATIONARY_RETRY_MS,
                                    PATHING_TARGET_WAIT_TIMEOUT_MS);
                        }
                        return TaskTransactionResult.PATHING_STARTED;
                    }
                    if (hasObservedPosition
                            && observedStationaryMs >= PATHING_STATIONARY_RETRY_MS
                            && !pathingSnapshot.isProbeInProgress()
                            && snapshotAgeMs <= OBSERVER_SNAPSHOT_MAX_AGE_MS) {
                        boolean nearTarget = gameStateUtil.isNearCoordinate(
                                pathingSnapshot.getCurrentMapName(),
                                pathingSnapshot.getCurrentX(),
                                pathingSnapshot.getCurrentY(),
                                target.mapName,
                                target.x,
                                target.y,
                                arrivalTolerance);
                        if (nearTarget) {
                            log.info("{} [nav-stress-latency] active observer reached target after stationary check: target={} current={}({}, {}) observedStationaryMs={} wallStationaryMs={}",
                                    context.getLogPrefix(), target,
                                    pathingSnapshot.getCurrentMapName(),
                                    pathingSnapshot.getCurrentX(),
                                    pathingSnapshot.getCurrentY(),
                                    observedStationaryMs,
                                    stationaryMs);
                            runtime.clearPathingSignal("nav stress consumed active observer near target");
                            state.completeCurrentTarget(context);
                            return state.isFinished() ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE;
                        }
                        boolean moving = gameStateUtil.confirmPathingStartedByEdgePixelDiff(
                                "nav-stress-active-stalled:" + target.sequence);
                        if (moving) {
                            log.info("{} [nav-stress-latency] active observer looked stalled, but edge pixels still confirm movement: target={} current={}({}, {}) observedStationaryMs={} wallStationaryMs={} snapshotAgeMs={} probeMs={}",
                                    context.getLogPrefix(), target,
                                    pathingSnapshot.getCurrentMapName(),
                                    pathingSnapshot.getCurrentX(),
                                    pathingSnapshot.getCurrentY(),
                                    observedStationaryMs,
                                    stationaryMs,
                                    snapshotAgeMs,
                                    probeMs);
                            return TaskTransactionResult.PATHING_STARTED;
                        }
                        log.info("{} [nav-stress-latency] re-enter navigation after confirmed stalled fresh snapshot: mode={} target={} current={}({}, {}) observedStationaryMs={} wallStationaryMs={} retryAfterMs={} snapshotAgeMs={} probeMs={}",
                                context.getLogPrefix(),
                                sameMapAsTarget ? "current-map coordinate" : "world-map",
                                target,
                                pathingSnapshot.getCurrentMapName(),
                                pathingSnapshot.getCurrentX(),
                                pathingSnapshot.getCurrentY(),
                                observedStationaryMs,
                                stationaryMs,
                                PATHING_STATIONARY_RETRY_MS,
                                snapshotAgeMs,
                                probeMs);
                        runtime.clearPathingSignal("nav stress consumed stalled active snapshot");
                        state.finishWaitingForPathing();
                        return TaskTransactionResult.READY_TO_CONTINUE;
                    }
                    if (snapshotAgeMs > OBSERVER_SNAPSHOT_MAX_AGE_MS && !pathingSnapshot.isProbeInProgress()) {
                        log.info("{} [nav-stress-latency] active observer snapshot is stale; allow fallback sync: target={} current={}({}, {}) snapshotAgeMs={} maxAgeMs={} wallStationaryMs={}",
                                context.getLogPrefix(), target,
                                pathingSnapshot.getCurrentMapName(),
                                pathingSnapshot.getCurrentX(),
                                pathingSnapshot.getCurrentY(),
                                snapshotAgeMs,
                                OBSERVER_SNAPSHOT_MAX_AGE_MS,
                                stationaryMs);
                    } else {
                        return TaskTransactionResult.PATHING_STARTED;
                    }
                }

                if (observedState == WindowPathingState.UNKNOWN) {
                    if (pathingAgeMs >= PATHING_TARGET_WAIT_TIMEOUT_MS) {
                        log.error("{} [nav-stress] pathing wait timeout while observer remains UNKNOWN: target={} ageMs={} timeoutMs={} message={}",
                                context.getLogPrefix(), target, pathingAgeMs, PATHING_TARGET_WAIT_TIMEOUT_MS,
                                pathingSnapshot.getMessage());
                        return TaskTransactionResult.FAILED;
                    }
                    /*
                     * UNKNOWN means the background watcher has a matching intent but currently
                     * missed the mini-map label. Do not retry navigation during the fast wait, but
                     * after that let the throttled position sync below prove whether the window is
                     * still moving or stalled. This avoids both 90s "unknown" loops and immediate
                     * duplicate world-map input.
                     */
                    log.info("{} [nav-stress-latency] observer unknown; verify with fallback sync when due: "
                                    + "target={} ageMs={} timeoutMs={} message={}",
                            context.getLogPrefix(), target, pathingAgeMs, PATHING_TARGET_WAIT_TIMEOUT_MS,
                            pathingSnapshot.getMessage());
                    if (pathingSnapshot.getCurrentMapName() != null
                            && pathingSnapshot.getCurrentX() != null
                            && pathingSnapshot.getCurrentY() != null) {
                        boolean nearTarget = gameStateUtil.isNearCoordinate(
                                pathingSnapshot.getCurrentMapName(),
                                pathingSnapshot.getCurrentX(),
                                pathingSnapshot.getCurrentY(),
                                target.mapName,
                                target.x,
                                target.y,
                                arrivalTolerance);
                        long stationaryMs = Math.max(0L,
                                System.currentTimeMillis() - pathingSnapshot.getLocationChangedAtMs());
                        long observedStationaryMs = pathingSnapshot.getLocationChangedAtMs() <= 0L
                                ? 0L
                                : Math.max(0L, pathingSnapshot.getUpdatedAtMs() - pathingSnapshot.getLocationChangedAtMs());
                        long unknownSnapshotAgeMs = Math.max(0L,
                                System.currentTimeMillis() - pathingSnapshot.getUpdatedAtMs());
                        log.info("{} [nav-stress-latency] observer unknown snapshot position: target={} current={}({}, {}) nearTarget={} stationaryMs={}",
                                context.getLogPrefix(), target,
                                pathingSnapshot.getCurrentMapName(),
                                pathingSnapshot.getCurrentX(),
                                pathingSnapshot.getCurrentY(),
                                nearTarget, stationaryMs);
                        if (nearTarget) {
                            runtime.clearPathingSignal("nav stress consumed unknown snapshot near target");
                            state.completeCurrentTarget(context);
                            return state.isFinished() ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE;
                        }
                        if (pathingSnapshot.isProbeInProgress()
                                || observedStationaryMs < PATHING_STATIONARY_RETRY_MS) {
                            log.info("{} [nav-stress-latency] observer unknown lacks fresh completed stalled proof; keep waiting: target={} observedStationaryMs={} wallStationaryMs={} snapshotAgeMs={} probeInProgress={}",
                                    context.getLogPrefix(), target,
                                    observedStationaryMs,
                                    stationaryMs,
                                    unknownSnapshotAgeMs,
                                    pathingSnapshot.isProbeInProgress());
                            return TaskTransactionResult.PATHING_STARTED;
                        }
                        if (unknownSnapshotAgeMs > OBSERVER_SNAPSHOT_MAX_AGE_MS) {
                            log.info("{} [nav-stress-latency] observer unknown snapshot is stale; allow fallback sync: target={} observedStationaryMs={} wallStationaryMs={} snapshotAgeMs={} maxAgeMs={}",
                                    context.getLogPrefix(), target,
                                    observedStationaryMs,
                                    stationaryMs,
                                    unknownSnapshotAgeMs,
                                    OBSERVER_SNAPSHOT_MAX_AGE_MS);
                        } else {

                            boolean moving = gameStateUtil.confirmPathingStartedByEdgePixelDiff(
                                    "nav-stress-observer-unknown:" + target.sequence);
                            if (moving) {
                                log.info("{} [nav-stress-latency] observer unknown but edge pixels still confirm movement: target={} stationaryMs={}",
                                        context.getLogPrefix(), target, stationaryMs);
                                return TaskTransactionResult.PATHING_STARTED;
                            }

                            WindowPathingSnapshot latestSnapshot = runtime.getPathingSnapshot();
                            if (latestSnapshot != null && latestSnapshot.getIntent() != null) {
                                WindowPathingIntent latestIntent = latestSnapshot.getIntent();
                                boolean latestSameIntentMap = gameStateUtil.isSameMapName(
                                        latestIntent.getTargetMapName(), target.mapName);
                                boolean latestCoordinateIntent = latestIntent.getTargetX() != null
                                        && latestIntent.getTargetY() != null;
                                boolean latestSameIntentCoordinate = !latestCoordinateIntent
                                        || (latestIntent.getTargetX() == target.x
                                        && latestIntent.getTargetY() == target.y);
                                if (latestSameIntentMap && latestSameIntentCoordinate) {
                                    int latestArrivalTolerance = observerTolerance(latestSnapshot);
                                    boolean latestHasCoordinate = latestSnapshot.getCurrentMapName() != null
                                            && latestSnapshot.getCurrentX() != null
                                            && latestSnapshot.getCurrentY() != null;
                                    boolean latestNearTarget = latestHasCoordinate && gameStateUtil.isNearCoordinate(
                                            latestSnapshot.getCurrentMapName(),
                                            latestSnapshot.getCurrentX(),
                                            latestSnapshot.getCurrentY(),
                                            target.mapName,
                                            target.x,
                                            target.y,
                                            latestArrivalTolerance);
                                    if ((latestSnapshot.getState() == WindowPathingState.ARRIVED || latestNearTarget)
                                            && !latestSnapshot.isProbeInProgress()) {
                                        log.info("{} [nav-stress-latency] observer refreshed while checking edge pixels; consume arrival before retry: target={} state={} current={}({}, {})",
                                                context.getLogPrefix(), target, latestSnapshot.getState(),
                                                latestSnapshot.getCurrentMapName(),
                                                latestSnapshot.getCurrentX(),
                                                latestSnapshot.getCurrentY());
                                        runtime.clearPathingSignal("nav stress consumed refreshed pathing arrival");
                                        state.completeCurrentTarget(context);
                                        return state.isFinished() ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE;
                                    }
                                    if (latestSnapshot.getUpdatedAtMs() > pathingSnapshot.getUpdatedAtMs()) {
                                        boolean sameStoppedPosition =
                                                gameStateUtil.isSameMapName(pathingSnapshot.getCurrentMapName(),
                                                        latestSnapshot.getCurrentMapName())
                                                        && pathingSnapshot.getCurrentX() != null
                                                        && pathingSnapshot.getCurrentY() != null
                                                        && latestSnapshot.getCurrentX() != null
                                                        && latestSnapshot.getCurrentY() != null
                                                        && pathingSnapshot.getCurrentX().equals(latestSnapshot.getCurrentX())
                                                        && pathingSnapshot.getCurrentY().equals(latestSnapshot.getCurrentY());
                                        long latestStationaryMs = Math.max(0L,
                                                System.currentTimeMillis() - latestSnapshot.getLocationChangedAtMs());
                                        long latestObservedStationaryMs = latestSnapshot.getLocationChangedAtMs() <= 0L
                                                ? 0L
                                                : Math.max(0L, latestSnapshot.getUpdatedAtMs() - latestSnapshot.getLocationChangedAtMs());
                                        long latestSnapshotAgeMs = Math.max(0L,
                                                System.currentTimeMillis() - latestSnapshot.getUpdatedAtMs());
                                        if (sameStoppedPosition
                                                && latestObservedStationaryMs >= PATHING_STATIONARY_RETRY_MS
                                                && !latestSnapshot.isProbeInProgress()
                                                && latestSnapshotAgeMs <= OBSERVER_SNAPSHOT_MAX_AGE_MS) {
                                            boolean sameMapAsTarget = gameStateUtil.isSameMapName(
                                                    latestSnapshot.getCurrentMapName(), target.mapName);
                                            log.info("{} [nav-stress-latency] observer refreshed same confirmed stalled position; re-enter {} navigation: target={} current={}({}, {}) observedStationaryMs={} wallStationaryMs={} retryAfterMs={}",
                                                    context.getLogPrefix(),
                                                    sameMapAsTarget ? "current-map coordinate" : "world-map",
                                                    target,
                                                    latestSnapshot.getCurrentMapName(),
                                                    latestSnapshot.getCurrentX(),
                                                    latestSnapshot.getCurrentY(),
                                                    latestObservedStationaryMs,
                                                    latestStationaryMs,
                                                    PATHING_STATIONARY_RETRY_MS);
                                            runtime.clearPathingSignal("nav stress consumed refreshed stalled unknown snapshot");
                                            state.finishWaitingForPathing();
                                            return TaskTransactionResult.READY_TO_CONTINUE;
                                        }
                                        /*
                                         * Edge-pixel confirmation takes close to a second. If the watcher
                                         * refreshed to a genuinely new state/location during that window,
                                         * do not retry from the old UNKNOWN snapshot; let the next
                                         * lightweight pass consume the fresh state.
                                         */
                                        log.info("{} [nav-stress-latency] observer refreshed during edge check; skip stale retry: target={} oldState={} newState={} oldUpdatedAt={} newUpdatedAt={} current={}({}, {})",
                                                context.getLogPrefix(), target,
                                                pathingSnapshot.getState(), latestSnapshot.getState(),
                                                pathingSnapshot.getUpdatedAtMs(), latestSnapshot.getUpdatedAtMs(),
                                                latestSnapshot.getCurrentMapName(),
                                                latestSnapshot.getCurrentX(),
                                                latestSnapshot.getCurrentY());
                                        return TaskTransactionResult.PATHING_STARTED;
                                    }
                                }
                            }

                            boolean sameMapAsTarget = gameStateUtil.isSameMapName(
                                    pathingSnapshot.getCurrentMapName(), target.mapName);
                            log.info("{} [nav-stress-latency] observer unknown and snapshot confirmed stalled; re-enter {} navigation: target={} current={}({}, {}) observedStationaryMs={} wallStationaryMs={} retryAfterMs={}",
                                    context.getLogPrefix(),
                                    sameMapAsTarget ? "current-map coordinate" : "debug-local",
                                    target,
                                    pathingSnapshot.getCurrentMapName(),
                                    pathingSnapshot.getCurrentX(),
                                    pathingSnapshot.getCurrentY(),
                                    observedStationaryMs,
                                    stationaryMs,
                                    PATHING_STATIONARY_RETRY_MS);
                            runtime.clearPathingSignal("nav stress consumed stalled unknown snapshot");
                            state.finishWaitingForPathing();
                            return TaskTransactionResult.READY_TO_CONTINUE;
                        }
                    }
                }

                if (observedState == WindowPathingState.STOPPED_AWAY) {
                    if (pathingSnapshot.isProbeInProgress()) {
                        return TaskTransactionResult.PATHING_STARTED;
                    }
                    boolean hasStoppedAwayCoordinate = pathingSnapshot.getCurrentMapName() != null
                            && pathingSnapshot.getCurrentX() != null
                            && pathingSnapshot.getCurrentY() != null;
                    boolean stoppedAwaySameMapAsTarget = hasStoppedAwayCoordinate
                            && gameStateUtil.isSameMapName(pathingSnapshot.getCurrentMapName(), target.mapName);
                    if (coordinateIntent
                            && hasStoppedAwayCoordinate
                            && !stoppedAwaySameMapAsTarget
                            && pathingAgeMs < COORDINATE_LEG_CROSS_MAP_GRACE_MS) {
                        long now = System.currentTimeMillis();
                        if (state.lastCoordinateLegTransitLogAt <= 0L
                                || now - state.lastCoordinateLegTransitLogAt >= MAP_LEG_STATIONARY_LOG_INTERVAL_MS) {
                            state.lastCoordinateLegTransitLogAt = now;
                            log.info("{} [nav-stress-latency] coordinate-leg stopped away on off-target map but still within transit grace; keep waiting: target={} current={}({}, {}) pathingAgeMs={} graceMs={} snapshotAgeMs={}",
                                    context.getLogPrefix(), target,
                                    pathingSnapshot.getCurrentMapName(),
                                    pathingSnapshot.getCurrentX(),
                                    pathingSnapshot.getCurrentY(),
                                    pathingAgeMs,
                                    COORDINATE_LEG_CROSS_MAP_GRACE_MS,
                                    snapshotAgeMs);
                        }
                        return TaskTransactionResult.PATHING_STARTED;
                    }
                    if (hasStoppedAwayCoordinate && gameStateUtil.isNearCoordinate(
                            pathingSnapshot.getCurrentMapName(),
                            pathingSnapshot.getCurrentX(),
                            pathingSnapshot.getCurrentY(),
                            target.mapName,
                            target.x,
                            target.y,
                            arrivalTolerance)) {
                        log.info("{} [nav-stress-latency] target reached by stopped-away observer: target={} current={}({}, {}) tolerance={}",
                                context.getLogPrefix(), target,
                                pathingSnapshot.getCurrentMapName(), pathingSnapshot.getCurrentX(), pathingSnapshot.getCurrentY(),
                                arrivalTolerance);
                        runtime.clearPathingSignal("nav stress consumed stopped-away near target");
                        state.completeCurrentTarget(context);
                        return state.isFinished() ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE;
                    }
                    DialogPreparationStatus routeDialogStatus = runtime.getDialogPreparationStatus();
                    if (state.pathingStartedAcrossMap
                            && routeDialogStatus != null
                            && routeDialogStatus.matches(DialogOperation.ROUTE_TRANSFER, target.mapName)) {
                        DialogPreparationPhase phase = routeDialogStatus.getPhase();
                        if (phase == DialogPreparationPhase.REQUESTED || phase == DialogPreparationPhase.PREPARING) {
                            log.info("{} [nav-stress-latency] pathing stopped away but route dialog preparation is active; keep waiting: target={} phase={} current={}({}, {})",
                                    context.getLogPrefix(), target, phase,
                                    pathingSnapshot.getCurrentMapName(),
                                    pathingSnapshot.getCurrentX(),
                                    pathingSnapshot.getCurrentY());
                            return TaskTransactionResult.PATHING_STARTED;
                        }
                        if (phase == DialogPreparationPhase.READY) {
                            log.info("{} [nav-stress-latency] pathing stopped away with prepared route dialog; re-enter navigation to consume dialog: target={} current={}({}, {})",
                                    context.getLogPrefix(), target,
                                    pathingSnapshot.getCurrentMapName(),
                                    pathingSnapshot.getCurrentX(),
                                    pathingSnapshot.getCurrentY());
                            runtime.clearPathingSignal("nav stress consumed stopped-away with prepared route dialog");
                            state.finishWaitingForPathing();
                            return TaskTransactionResult.READY_TO_CONTINUE;
                        }
                    }
                    boolean sameMapAsTarget = gameStateUtil.isSameMapName(
                            pathingSnapshot.getCurrentMapName(), target.mapName);
                    log.info("{} [nav-stress-latency] pathing observer stopped away; re-enter {} navigation: target={} current={}({}, {})",
                            context.getLogPrefix(),
                            sameMapAsTarget ? "current-map coordinate" : "world-map",
                            target,
                            pathingSnapshot.getCurrentMapName(), pathingSnapshot.getCurrentX(), pathingSnapshot.getCurrentY());
                    runtime.clearPathingSignal("nav stress consumed stopped-away");
                    state.finishWaitingForPathing();
                    return TaskTransactionResult.READY_TO_CONTINUE;
                }
            }
        }

        PlayerCharacter me = runtime == null ? null : runtime.getGameState().getMe();
        if (me != null && gameStateUtil.isNearCoordinate(
                me.getCurrentMapName(), me.getX(), me.getY(),
                target.mapName, target.x, target.y, TARGET_REACHED_TOLERANCE)) {
            /*
             * This debug task only needs to prove handoff latency. Once the cached per-window
             * location is already at the target, avoid the heavier movement detector so another
             * window is not blocked by a 3-4s stopped-state confirmation.
             */
            log.info("{} [nav-stress-latency] target reached by cached coordinate before movement probe: "
                            + "target={} current={} tolerance={}",
                    context.getLogPrefix(), target, formatPosition(null, me), TARGET_REACHED_TOLERANCE);
            state.completeCurrentTarget(context);
            return state.isFinished() ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE;
        }

        pathingAgeMs = state.pathingStartedAt <= 0L
                ? Long.MAX_VALUE
                : Math.max(0L, System.currentTimeMillis() - state.pathingStartedAt);
        if (pathingAgeMs < PATHING_RECHECK_GRACE_MS) {
            /*
             * The stress probe is measuring turn handoff. During the grace window we avoid any
             * navigation retry and give the next window a chance to take the task turn.
             */
            log.info("{} [nav-stress-latency] pathing grace active: target={} ageMs={} graceMs={}",
                    context.getLogPrefix(), target, pathingAgeMs, PATHING_RECHECK_GRACE_MS);
            return TaskTransactionResult.PATHING_STARTED;
        }

        if (pathingAgeMs < PATHING_OBSERVER_FAST_WAIT_MS) {
            /*
             * This debug task measures task-turn handoff latency. The window observer already polls
             * mini-map state outside the turn, so do not spend several seconds here proving movement
             * with GameStateUtil.detectMovementState().
             */
            log.info("{} [nav-stress-latency] wait for background pathing observer without heavy movement probe: "
                            + "target={} ageMs={} fastWaitMs={}",
                    context.getLogPrefix(), target, pathingAgeMs, PATHING_OBSERVER_FAST_WAIT_MS);
            return TaskTransactionResult.PATHING_STARTED;
        }

        if (pathingAgeMs >= PATHING_TARGET_WAIT_TIMEOUT_MS) {
            log.error("{} [nav-stress] pathing wait timeout without observer arrival: target={} ageMs={} timeoutMs={}",
                    context.getLogPrefix(), target, pathingAgeMs, PATHING_TARGET_WAIT_TIMEOUT_MS);
            return TaskTransactionResult.FAILED;
        }

        long now = System.currentTimeMillis();
        if (state.lastPathingSyncAt <= 0L
                || now - state.lastPathingSyncAt >= PATHING_POSITION_SYNC_INTERVAL_MS) {
            long syncStartedAt = System.currentTimeMillis();
            LocationInfo fresh = playerStateService.syncMyPosition();
            long syncElapsedMs = Math.max(0L, System.currentTimeMillis() - syncStartedAt);
            LocationInfo previous = state.lastPathingSyncPosition;
            state.lastPathingSyncAt = System.currentTimeMillis();
            if (fresh != null) {
                boolean nearTarget = gameStateUtil.isNearCoordinate(
                        fresh.mapName, fresh.x, fresh.y,
                        target.mapName, target.x, target.y, TARGET_REACHED_TOLERANCE);
                boolean sameAsPrevious = previous != null
                        && gameStateUtil.isSameMapName(previous.mapName, fresh.mapName)
                        && previous.x == fresh.x
                        && previous.y == fresh.y;
                log.info("{} [nav-stress-latency] observer unavailable fallback sync: target={} current={} nearTarget={} sameAsPrevious={} ageMs={} elapsedMs={}",
                        context.getLogPrefix(), target, formatPosition(fresh, null),
                        nearTarget, sameAsPrevious, pathingAgeMs, syncElapsedMs);
                if (nearTarget) {
                    state.completeCurrentTarget(context);
                    return state.isFinished() ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE;
                }
                state.lastPathingSyncPosition = fresh;
                /*
                 * If the watcher is silent and two throttled mini-map reads report the same
                 * non-target coordinate, the debug route leg has probably stopped or failed. Return
                 * to the local debug navigation path instead of burning the full 90s timeout.
                 */
                if (sameAsPrevious && pathingAgeMs >= PATHING_STATIONARY_RETRY_MS) {
                    log.info("{} [nav-stress-latency] observer unavailable and position stalled; re-enter debug-local navigation: target={} current={} ageMs={} retryAfterMs={}",
                            context.getLogPrefix(), target, formatPosition(fresh, null),
                            pathingAgeMs, PATHING_STATIONARY_RETRY_MS);
                    state.finishWaitingForPathing();
                    return TaskTransactionResult.READY_TO_CONTINUE;
                }
            } else {
                log.info("{} [nav-stress-latency] observer unavailable fallback sync returned empty: target={} ageMs={} elapsedMs={}",
                        context.getLogPrefix(), target, pathingAgeMs, syncElapsedMs);
            }
        }

        log.info("{} [nav-stress-latency] observer signal unavailable; keep yielding after fallback check: target={} ageMs={}",
                context.getLogPrefix(), target, pathingAgeMs);
        return TaskTransactionResult.PATHING_STARTED;
    }

    private TaskTransactionResult waitForRouteDialogPreparation(TaskExecutionContext context,
                                                                NavigationStressState state,
                                                                NavigationTarget target) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        long ageMs = state.routeDialogPreparationStartedAt <= 0L
                ? Long.MAX_VALUE
                : Math.max(0L, System.currentTimeMillis() - state.routeDialogPreparationStartedAt);
        if (runtime == null) {
            log.info("{} [nav-stress-latency] route dialog wait has no window context; retry navigation: target={}",
                    context.getLogPrefix(), target);
            state.finishWaitingForPathing();
            return TaskTransactionResult.READY_TO_CONTINUE;
        }

        DialogPreparationStatus status = runtime.getDialogPreparationStatus();
        if (status != null && status.matches(DialogOperation.ROUTE_TRANSFER, target.mapName)) {
            DialogPreparationPhase phase = status.getPhase();
            if (phase == DialogPreparationPhase.READY) {
                log.info("{} [nav-stress-latency] route dialog prepared in background; re-enter navigation to click: target={} ageMs={}",
                        context.getLogPrefix(), target, ageMs);
                state.finishWaitingForPathing();
                return TaskTransactionResult.READY_TO_CONTINUE;
            }
            if (phase == DialogPreparationPhase.REQUESTED) {
                if (ageMs < ROUTE_DIALOG_REQUESTED_WAIT_TIMEOUT_MS) {
                    log.info("{} [nav-stress-latency] route dialog request waiting for watcher; keep yielding: target={} phase={} ageMs={} timeoutMs={}",
                            context.getLogPrefix(), target, phase, ageMs, ROUTE_DIALOG_REQUESTED_WAIT_TIMEOUT_MS);
                    return TaskTransactionResult.PATHING_STARTED;
                }
                log.info("{} [nav-stress-latency] route dialog request not picked up in time; re-enter navigation foreground path: target={} phase={} ageMs={} timeoutMs={}",
                        context.getLogPrefix(), target, phase, ageMs, ROUTE_DIALOG_REQUESTED_WAIT_TIMEOUT_MS);
                state.finishWaitingForPathing();
                return TaskTransactionResult.READY_TO_CONTINUE;
            }
            if (phase == DialogPreparationPhase.PREPARING) {
                if (ageMs < ROUTE_DIALOG_PREPARING_WAIT_TIMEOUT_MS) {
                    log.info("{} [nav-stress-latency] route dialog still preparing; keep yielding: target={} phase={} ageMs={}",
                            context.getLogPrefix(), target, phase, ageMs);
                    return TaskTransactionResult.PATHING_STARTED;
                }
                log.info("{} [nav-stress-latency] route dialog preparing timed out; re-enter navigation foreground path: target={} phase={} ageMs={} timeoutMs={}",
                        context.getLogPrefix(), target, phase, ageMs, ROUTE_DIALOG_PREPARING_WAIT_TIMEOUT_MS);
                state.finishWaitingForPathing();
                return TaskTransactionResult.READY_TO_CONTINUE;
            }
            if (phase == DialogPreparationPhase.FAILED && ageMs < ROUTE_DIALOG_FAILED_RETRY_MS) {
                log.info("{} [nav-stress-latency] route dialog prepare recently failed; yield before retry: target={} ageMs={}",
                        context.getLogPrefix(), target, ageMs);
                return TaskTransactionResult.PATHING_STARTED;
            }
        }

        log.info("{} [nav-stress-latency] route dialog wait ended; retry navigation: target={} ageMs={} statusPhase={}",
                context.getLogPrefix(), target, ageMs, status == null ? null : status.getPhase());
        state.finishWaitingForPathing();
        return TaskTransactionResult.READY_TO_CONTINUE;
    }

    private String formatPosition(LocationInfo freshPosition, PlayerCharacter cached) {
        if (freshPosition != null) {
            return freshPosition.mapName + "(" + freshPosition.x + "," + freshPosition.y + ")";
        }
        if (cached != null) {
            return cached.getCurrentMapName() + "(" + cached.getX() + "," + cached.getY() + ")";
        }
        return "-";
    }

    private int observerTolerance(WindowPathingSnapshot snapshot) {
        WindowPathingIntent intent = snapshot == null ? null : snapshot.getIntent();
        return intent == null ? TARGET_REACHED_TOLERANCE : Math.max(0, intent.getTolerance());
    }

    private List<NavigationTarget> generateTargets(TaskExecutionContext context) {
        List<NavigationTarget> targets = new ArrayList<>();
        /*
         * The navigation watcher debug needs to exercise the full declared route. Older IDE/debug
         * runs used -Dnavigation.stress.targetCount=2, which made the task look healthy while only
         * covering the first two legs. Keep the list itself as the single switch for enabled points.
         */
        int targetCount = TARGET_SPECS.size();
        log.info("{} [nav-stress] target count={} fullRoute=true",
                context.getLogPrefix(), targetCount);
        for (int i = 0; i < targetCount; i++) {
            TargetSpec spec = TARGET_SPECS.get(i);
            targets.add(new NavigationTarget(i + 1, spec.mapName, spec.x, spec.y,
                    "mock-" + spec.mapName + "-" + (i + 1)));
        }
        return targets;
    }

    private TaskExecutionContext resolveExecutionContext(TaskExecutionContext executionContext) {
        if (executionContext != null) {
            return executionContext;
        }
        return TaskExecutionContext.builder()
                .taskCode(TASK_CODE)
                .taskName(TASK_NAME)
                .startedAt(LocalDateTime.now())
                .build();
    }

    private String transactionName(NavigationStressState state) {
        NavigationTarget target = state.currentTarget();
        String suffix = target == null ? "finished" : target.sequence + "-" + target.mapName;
        String phase = state.waitingRouteDialogPreparation ? "dialog:" : (state.waitingPathing ? "wait:" : "navigate:");
        return "debug-nav-stress:" + phase + suffix;
    }

    private long pauseAfterYieldMs(TaskTransactionResult result, TaskYieldPolicy yieldPolicy) {
        if (result == TaskTransactionResult.PATHING_STARTED) {
            return PATHING_HANDOFF_DELAY_MS;
        }
        if (result == TaskTransactionResult.RETRYABLE_ERROR) {
            return RETRYABLE_ERROR_BACKOFF_MS;
        }
        if (yieldPolicy == TaskYieldPolicy.RETRY_LATER) {
            return PATHING_HANDOFF_DELAY_MS;
        }
        return 0L;
    }

    private static class NavigationStressState {
        private final List<NavigationTarget> targets;
        private int targetIndex;
        private boolean waitingPathing;
        private boolean waitingRouteDialogPreparation;
        private boolean deferFirstMovementProbe;
        private int navigationRetryCount;
        private long lastYieldAt;
        private long pathingStartedAt;
        private long routeDialogPreparationStartedAt;
        private String pathingStartedFromMap;
        private boolean pathingStartedAcrossMap;
        private long lastPathingSyncAt;
        private LocationInfo lastPathingSyncPosition;
        private WindowPathingState lastObservedPathingState;
        private long lastObservedPathingUpdatedAtMs;
        private long lastMapLegStationaryLogAt;
        private long lastCoordinateLegTransitLogAt;

        private NavigationStressState(List<NavigationTarget> targets) {
            this.targets = targets == null ? List.of() : List.copyOf(targets);
        }

        private boolean isFinished() {
            return targetIndex >= targets.size();
        }

        private NavigationTarget currentTarget() {
            return isFinished() ? null : targets.get(targetIndex);
        }

        private boolean isWaitingOutsideTurn() {
            return waitingPathing || waitingRouteDialogPreparation;
        }

        private String currentTargetText() {
            NavigationTarget target = currentTarget();
            return target == null ? "-" : target.toString();
        }

        private String describeTargets() {
            return targets.stream()
                    .map(NavigationTarget::toString)
                    .collect(Collectors.joining(" -> "));
        }

        private long pathingAgeMs() {
            if (pathingStartedAt <= 0L) {
                return -1L;
            }
            return Math.max(0L, System.currentTimeMillis() - pathingStartedAt);
        }

        private void compensatePausedDuration(long blockedMs) {
            if (blockedMs <= 0L) {
                return;
            }
            if (pathingStartedAt > 0L) {
                pathingStartedAt += blockedMs;
            }
            if (lastYieldAt > 0L) {
                lastYieldAt += blockedMs;
            }
            if (lastPathingSyncAt > 0L) {
                lastPathingSyncAt += blockedMs;
            }
        }

        private void completeCurrentTarget(TaskExecutionContext context) {
            NavigationTarget target = currentTarget();
            if (target != null) {
                log.info("{} [nav-stress] target reached: target={} completed={}/{}",
                        context.getLogPrefix(), target, targetIndex + 1, targets.size());
            }
            targetIndex++;
            waitingPathing = false;
            waitingRouteDialogPreparation = false;
            deferFirstMovementProbe = false;
            pathingStartedAt = 0L;
            routeDialogPreparationStartedAt = 0L;
            pathingStartedFromMap = null;
            pathingStartedAcrossMap = false;
            lastPathingSyncAt = 0L;
            lastPathingSyncPosition = null;
            lastObservedPathingState = null;
            lastObservedPathingUpdatedAtMs = 0L;
            lastMapLegStationaryLogAt = 0L;
            lastCoordinateLegTransitLogAt = 0L;
            navigationRetryCount = 0;
        }

        private void finishWaitingForPathing() {
            waitingPathing = false;
            waitingRouteDialogPreparation = false;
            deferFirstMovementProbe = false;
            pathingStartedAt = 0L;
            routeDialogPreparationStartedAt = 0L;
            pathingStartedFromMap = null;
            pathingStartedAcrossMap = false;
            lastPathingSyncAt = 0L;
            lastPathingSyncPosition = null;
            lastObservedPathingState = null;
            lastObservedPathingUpdatedAtMs = 0L;
            lastMapLegStationaryLogAt = 0L;
            lastCoordinateLegTransitLogAt = 0L;
        }

    }

    private static class TargetSpec {
        private final String mapName;
        private final int x;
        private final int y;

        private TargetSpec(String mapName, int x, int y) {
            this.mapName = mapName;
            this.x = x;
            this.y = y;
        }
    }

    private static class NavigationTarget {
        private final int sequence;
        private final String mapName;
        private final int x;
        private final int y;
        private final String name;

        private NavigationTarget(int sequence, String mapName, int x, int y, String name) {
            this.sequence = sequence;
            this.mapName = mapName;
            this.x = x;
            this.y = y;
            this.name = name;
        }

        @Override
        public String toString() {
            return "#" + sequence + " " + mapName + "(" + x + "," + y + ")";
        }

        private NavigationRequest toNavigationRequest(String source) {
            return NavigationRequest.builder()
                    .targetMapName(mapName)
                    .targetX(x)
                    .targetY(y)
                    .targetName(name)
                    .returnOnPathingStarted(true)
                    .publishWindowPathingIntent(true)
                    .arrivalTolerance(TARGET_REACHED_TOLERANCE)
                    .source(source)
                    .build();
        }
    }
}
