package com.bot.dhxy.window.execution;

import com.bot.dhxy.cloud.task.DialogPolicyPreClickCloudDecision;
import com.bot.dhxy.cloud.task.RouteCloudDecisionService;
import com.bot.dhxy.cloud.task.RouteMemoryOutcomeReport;
import com.bot.dhxy.cloud.task.RouteMemoryOutcomeIngestResult;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogDetection;
import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogPreparationRequest;
import com.bot.dhxy.model.dialog.DialogPreparationStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.job.PreparedActionJob;
import com.bot.dhxy.model.job.PreparedActionJobType;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.model.navigation.PendingTransferChoiceMemory;
import com.bot.dhxy.model.navigation.TemplateLocationInfo;
import com.bot.dhxy.model.navigation.PendingRouteOutcome;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelNegativeResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelPrepareResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.context.TaskStartupMode;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.service.MapNameCanonicalizer;
import com.bot.dhxy.service.MemoryService;
import com.bot.dhxy.service.TaskMaintenanceService;
import com.bot.dhxy.service.TaskTrackerPanelService;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.TaskFactory;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.xiuluo.XiuluoDialogCatalog;
import com.bot.dhxy.task.startup.TaskTeamAssignmentPolicy;
import com.bot.dhxy.team.TeamRoleDetectionService;
import com.bot.dhxy.team.TeamRoleStatus;
import com.bot.dhxy.window.dialog.WindowDialogPreparationProvider;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowReadyEvent;
import com.bot.dhxy.window.model.WindowReadyEventType;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowReadyEventBus;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Single-window task executor.
 *
 * <p>Each registered game window owns one runner and one single-thread executor. The runner binds
 * {@link WindowRuntimeContext} and {@link TaskExecutionContext} before creating/executing tasks, so
 * screenshots, OCR, temp files, and input submissions all resolve to the correct hwnd/window state.
 * Public control methods are safe to call from UI/control code; actual task work stays on the runner
 * thread.</p>
 */
@Slf4j
public class WindowTaskRunner {

    private static final AtomicInteger THREAD_ID = new AtomicInteger(1);
    private static final AtomicLong GLOBAL_TASK_RUN_SEQUENCE = new AtomicLong();
    private static final long WINDOW_COMBAT_GUARD_IDLE_INTERVAL_MS = 6_000L;
    private static final long WINDOW_PATHING_PROBE_ACTIVE_INTERVAL_MS = 1_000L;
    private static final long WINDOW_PATHING_PROBE_MIN_INTERVAL_MS = 2_000L;
    private static final long WINDOW_PATHING_ARRIVAL_STATIONARY_MS = 600L;
    private static final long WINDOW_PATHING_SHORTCUT_STOPPED_AWAY_MS = 2_200L;
    private static final long WINDOW_PATHING_MINI_MAP_HANDOFF_STOPPED_AWAY_MS = 2_200L;
    private static final long WINDOW_PATHING_SLOW_PROBE_LOG_MS = 1_500L;
    private static final long WINDOW_DIALOG_PREPARE_WUBEI_ENTER_BATTLE_INTERVAL_MS = 100L;
    private static final long WINDOW_DIALOG_PREPARE_WUBEI_PROBE_STORY_INTERVAL_MS = 200L;
    private static final long WINDOW_DIALOG_PREPARE_ROUTE_INTERVAL_MS = 1_000L;
    private static final long WINDOW_ROUTE_DIALOG_PREPARE_TIMEOUT_MS = 2_500L;
    private static final long WINDOW_DIALOG_PREPARE_TASK_TRACKER_INTERVAL_MS = 1_000L;
    private static final long WINDOW_DIALOG_PREPARE_DEFAULT_INTERVAL_MS = 500L;
    private static final long WINDOW_DIALOG_ATTENTION_RECENT_MS = 2_500L;
    private static final long WINDOW_DIALOG_VISIBLE_MAX_AGE_MS = 3_000L;
    private static final long WINDOW_TASK_DIALOG_PREPARED_STALE_MS = WINDOW_DIALOG_VISIBLE_MAX_AGE_MS;
    private static final long WINDOW_TASK_DIALOG_STALE_REPUBLISH_COOLDOWN_MS = 1_000L;
    private static final long PREPARED_TRACKER_ACTION_MAX_AGE_MS = 2_500L;
    private static final long WINDOW_OBSERVER_WAKE_CHECK_INTERVAL_MS = 100L;
    private static final long WINDOW_PAUSED_READONLY_INTERVAL_MS = 500L;
    private static final long WINDOW_POST_COMBAT_IDLE_TIMEOUT_MS = 180_000L;
    /*
     * CR266 contract item 3: one global pre-battle budget per 五倍 round — from the successful task
     * accept until the runner first confirms IN_COMBAT — covering maintenance, tracker read, green
     * link, navigation, ENTER_BATTLE and every internal wait/retry. Was 300s from the first green
     * click; now 180s from accept (user-approved baseline diff, 业务逻辑.md 普通怪入战前超时).
     */
    private static final long WUBEI_ORDINARY_PRE_BATTLE_TIMEOUT_MS = 180_000L;
    private static final long ROUTE_OUTCOME_RETRY_BASE_DELAY_MS = 1_000L;
    private static final long ROUTE_OUTCOME_RETRY_MAX_DELAY_MS = 30_000L;
    private static final String XIULUO_TRACKER_SHORTCUT_SOURCE_PREFIX = "xiuluo-v2:tracker-shortcut";

    private final WindowRuntimeContext windowContext;
    private final TaskFactory taskFactory;
    private final WindowTaskContextHolder contextHolder;
    private final WindowTaskStartupInitializer startupInitializer;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final InputSequences inputSequences;
    private final TeamRoleDetectionService teamRoleDetectionService;
    private final TaskTeamAssignmentPolicy taskTeamAssignmentPolicy;
    private final AutomationMetricsService automationMetricsService;
    private final AutoCombatService autoCombatService;
    private final MiniMapCoordinateReader miniMapCoordinateReader;
    private final DialogService dialogService;
    private final UICleanerService uiCleanerService;
    private final TaskTrackerPanelService taskTrackerPanelService;
    private final MapNameCanonicalizer mapNameCanonicalizer;
    private final MemoryService memoryService;
    private final RouteCloudDecisionService routeCloudDecisionService;
    private final TaskMaintenanceService taskMaintenanceService;
    private final List<WindowDialogPreparationProvider> dialogPreparationProviders;
    private final WindowReadyEventBus windowReadyEventBus;
    private final ExecutorService executor;
    private final ExecutorService combatWatcherExecutor;
    private final ExecutorService dialogPreparationExecutor;
    private final AtomicLong lastTaskDialogStaleRepublishAtMs = new AtomicLong();
    // Delivery only: never used for route selection and deliberately never persisted.
    private final ConcurrentHashMap<String, PendingRouteOutcomeDelivery> pendingRouteOutcomeDeliveries =
            new ConcurrentHashMap<>();
    private volatile RunningTaskHandle currentTask;
    private volatile boolean shutdown;
    private volatile String activeLocalTeamSessionKey;
    private volatile String activeLocalLeaderWindowId;
    private volatile boolean activeLocalLeaderPresent;

    /**
     * Create a runner for one registered window.
     *
     * @param windowContext owning runtime context.
     * @param taskFactory task factory used after role reassignment.
     * @param contextHolder thread-local window binding holder.
     * @param startupInitializer per-task startup preparation.
     * @param taskExecutionContextHolder thread-local task execution context holder.
     * @param inputSequences serialized input API used by tasks.
     * @param teamRoleDetectionService live role detector used before leader/team tasks.
     * @param taskTeamAssignmentPolicy role-to-task reassignment policy.
     * @param automationMetricsService local business metrics sink for task start/end events.
     * @param autoCombatService window-level combat guard reused by normal task runners.
     * @param miniMapCoordinateReader lightweight mini-map location reader used by the background
     *                                watcher to refresh pathing state without taking the task turn.
     * @param dialogService dialog detector used by the watcher for prepare-only option matching.
     * @param taskTrackerPanelService left task-tracker panel reader used for prepared pathing links.
     * @param mapNameCanonicalizer canonicalizer used when comparing tracker target maps with
     *                             mini-map OCR/current-map readings.
     * @param memoryService unified memory facade used by watcher-settled dialog and route memories.
     * @param routeCloudDecisionService route-memory cloud outcome reporter used after watcher settlement.
     * @param taskMaintenanceService local team support/session capability registry.
     * @param dialogPreparationProviders task-owned providers used for explicitly registered
     *                                   business dialogs.
     * @param windowReadyEventBus soft wake bus published after watcher terminal observations.
     */
    public WindowTaskRunner(WindowRuntimeContext windowContext,
                            TaskFactory taskFactory,
                            WindowTaskContextHolder contextHolder,
                            WindowTaskStartupInitializer startupInitializer,
                            TaskExecutionContextHolder taskExecutionContextHolder,
                            InputSequences inputSequences,
                            TeamRoleDetectionService teamRoleDetectionService,
                            TaskTeamAssignmentPolicy taskTeamAssignmentPolicy,
                            AutomationMetricsService automationMetricsService,
                            AutoCombatService autoCombatService,
                             MiniMapCoordinateReader miniMapCoordinateReader,
                             DialogService dialogService,
                             UICleanerService uiCleanerService,
                              TaskTrackerPanelService taskTrackerPanelService,
                             MapNameCanonicalizer mapNameCanonicalizer,
                             MemoryService memoryService,
                             RouteCloudDecisionService routeCloudDecisionService,
                             TaskMaintenanceService taskMaintenanceService,
                            List<WindowDialogPreparationProvider> dialogPreparationProviders,
                            WindowReadyEventBus windowReadyEventBus) {
        this.windowContext = Objects.requireNonNull(windowContext, "windowContext must not be null");
        this.taskFactory = Objects.requireNonNull(taskFactory, "taskFactory must not be null");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder must not be null");
        this.startupInitializer = Objects.requireNonNull(startupInitializer, "startupInitializer must not be null");
        this.taskExecutionContextHolder = Objects.requireNonNull(taskExecutionContextHolder, "taskExecutionContextHolder must not be null");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences must not be null");
        this.teamRoleDetectionService = Objects.requireNonNull(teamRoleDetectionService, "teamRoleDetectionService must not be null");
        this.taskTeamAssignmentPolicy = Objects.requireNonNull(taskTeamAssignmentPolicy, "taskTeamAssignmentPolicy must not be null");
        this.automationMetricsService = Objects.requireNonNull(automationMetricsService, "automationMetricsService must not be null");
        this.autoCombatService = Objects.requireNonNull(autoCombatService, "autoCombatService must not be null");
        this.miniMapCoordinateReader = Objects.requireNonNull(miniMapCoordinateReader, "miniMapCoordinateReader must not be null");
        this.dialogService = Objects.requireNonNull(dialogService, "dialogService must not be null");
        this.uiCleanerService = Objects.requireNonNull(uiCleanerService, "uiCleanerService must not be null");
        this.taskTrackerPanelService = Objects.requireNonNull(taskTrackerPanelService, "taskTrackerPanelService must not be null");
        this.mapNameCanonicalizer = Objects.requireNonNull(mapNameCanonicalizer, "mapNameCanonicalizer must not be null");
        this.memoryService = Objects.requireNonNull(memoryService, "memoryService must not be null");
        this.routeCloudDecisionService = Objects.requireNonNull(routeCloudDecisionService, "routeCloudDecisionService must not be null");
        this.taskMaintenanceService = Objects.requireNonNull(taskMaintenanceService, "taskMaintenanceService must not be null");
        this.dialogPreparationProviders = dialogPreparationProviders == null
                ? List.of()
                : List.copyOf(dialogPreparationProviders);
        this.windowReadyEventBus = Objects.requireNonNull(windowReadyEventBus, "windowReadyEventBus must not be null");
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("window-task-" + windowContext.getWindowId() + "-" + THREAD_ID.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
        this.combatWatcherExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("window-combat-watch-" + windowContext.getWindowId() + "-" + THREAD_ID.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
        this.dialogPreparationExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("window-dialog-prepare-" + windowContext.getWindowId() + "-" + THREAD_ID.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Submit one task to this window.
     *
     * @param taskType requested task type; UNKNOWN/null becomes an invalid queue.
     * @return true when accepted by the runner.
     */
    public synchronized boolean submit(TaskType taskType) {
        return submit(WindowTaskQueue.single(taskType));
    }

    /**
     * Submit a task queue to this window.
     *
     * @param queue queue of requested task types. The runner accepts only one live queue at a time.
     * @return true when the queue is accepted and scheduled.
     */
    public synchronized boolean submit(WindowTaskQueue queue) {
        return submit(queue, null, null, false);
    }

    /**
     * Submit a task queue to this window with optional local-team session metadata.
     *
     * @param queue queue of requested task types. The runner accepts only one live queue at a time.
     * @param localTeamSessionKey shared id for the UI-started local team run; blank means standalone.
     * @param localLeaderWindowId local leader window id for diagnostics and member gates.
     * @param localLeaderPresent true when this submitted batch contains a local leader.
     * @return true when the queue is accepted and scheduled.
     */
    public synchronized boolean submit(WindowTaskQueue queue,
                                       String localTeamSessionKey,
                                       String localLeaderWindowId,
                                       boolean localLeaderPresent) {
        WindowTaskQueue safeQueue = queue == null ? WindowTaskQueue.empty() : queue;
        if (shutdown) {
            windowContext.markError("runner is closed");
            log.warn("window [{}] runner is closed, reject queue {}", windowContext.getWindowId(), safeQueue.toLogText());
            return false;
        }
        if (safeQueue.isEmpty()) {
            windowContext.markError("invalid task type");
            log.warn("window [{}] invalid task queue", windowContext.getWindowId());
            return false;
        }
        if (getActiveTaskHandle() != null) {
            log.warn("window [{}] already has running task, reject queue {}", windowContext.getWindowId(), safeQueue.toLogText());
            return false;
        }

        TaskStopToken stopToken = new TaskStopToken();
        TaskPauseToken pauseToken = new TaskPauseToken();
        String sessionKey = normalizeSessionText(localTeamSessionKey);
        String leaderWindowId = normalizeSessionText(localLeaderWindowId);
        boolean leaderPresent = localLeaderPresent && sessionKey != null;
        WindowTaskQueue executionQueue = safeQueue;
        TaskType firstTaskType = executionQueue.firstTaskType();
        FutureTask<Void> futureTask = new FutureTask<>(() -> {
            activeLocalTeamSessionKey = leaderPresent ? sessionKey : null;
            activeLocalLeaderWindowId = leaderPresent ? leaderWindowId : null;
            activeLocalLeaderPresent = leaderPresent;
            runQueue(executionQueue, stopToken, pauseToken);
            return null;
        });

        currentTask = new RunningTaskHandle(windowContext.getWindowId(), executionQueue, firstTaskType, null, stopToken, pauseToken, futureTask);
        windowContext.markQueued(firstTaskType);
        executor.execute(futureTask);
        return true;
    }

    /**
     * Apply updated registration metadata.
     *
     * @param request new registration data. Selected task changes are ignored while a task is running.
     */
    public void refreshRegistration(WindowRegistrationRequest request) {
        if (request != null) {
            windowContext.applyRegistration(request, !isRunning());
        }
    }

    /**
     * Stop the current task queue, if any.
     *
     * <p>The method marks runtime state, requests cooperative stop, interrupts the runner thread, and
     * cancels the future. It does not directly release the global input worker; in-flight input
     * requests finish or observe interruption through their own paths.</p>
     *
     * @return true when a live task queue existed and accepted the stop request.
     */
    public boolean stopCurrentTask() {
        RunningTaskHandle taskHandle = getActiveTaskHandle();
        if (taskHandle == null) {
            if (windowContext.getStatus() == WindowRuntimeStatus.ERROR
                    || windowContext.getStatus() == WindowRuntimeStatus.STOPPING) {
                windowContext.markStoppedAfterTerminalStop("stop requested after task already ended");
                log.info("window [{}] terminal task state cleared by stop command: status={}",
                        windowContext.getWindowId(), windowContext.getStatus());
                return true;
            }
            log.info("window [{}] stop ignored: no active task queue", windowContext.getWindowId());
            return false;
        }
        log.info("window [{}] stop requested: queue={} progress={} currentTask={}",
                windowContext.getWindowId(),
                taskHandle.getTaskQueueDisplayText(),
                taskHandle.getTaskProgressText(),
                taskHandle.getTaskType());
        windowContext.markStopping("stop requested");
        windowContext.getGameContext().runWithState(windowContext.getGameState(), () -> taskHandle.requestStop("stop requested"));
        windowReadyEventBus.wakeForTaskStop(windowContext.getWindowId(), "stop requested");
        settleRouteOutcomesBeforeWatcherStop("stop-current-task");
        taskHandle.forceCancel("stop requested");
        return true;
    }

    /**
     * Request cooperative pause for the current task queue.
     *
     * @return true when a live task accepted the pause request.
     */
    public boolean pauseCurrentTask() {
        RunningTaskHandle taskHandle = getActiveTaskHandle();
        if (taskHandle == null) {
            return false;
        }
        taskHandle.requestPause("pause requested");
        windowReadyEventBus.wakeForTaskPause(windowContext.getWindowId(), "pause requested");
        if (windowContext.isLeader()) {
            windowContext.wakeObserver("pause requested");
            taskMaintenanceService.markLocalTeamLeaderPaused(
                    windowContext.getWindowId(), true, "window-task-runner:pause");
        }
        windowContext.markPauseRequested("pause requested");
        log.info("window [{}] task pause requested: queue={} progress={}",
                windowContext.getWindowId(), taskHandle.getTaskQueueDisplayText(), taskHandle.getTaskProgressText());
        return true;
    }

    /**
     * Resume a paused task queue.
     *
     * @return true when a live task existed and its pause token was cleared.
     */
    public boolean resumeCurrentTask() {
        RunningTaskHandle taskHandle = getActiveTaskHandle();
        if (taskHandle == null) {
            return false;
        }
        taskHandle.resume();
        if (windowContext.isLeader()) {
            taskMaintenanceService.markLocalTeamLeaderPaused(
                    windowContext.getWindowId(), false, "window-task-runner:resume");
        }
        windowContext.markResumed("resume requested");
        log.info("window [{}] task resume requested: queue={} progress={}",
                windowContext.getWindowId(), taskHandle.getTaskQueueDisplayText(), taskHandle.getTaskProgressText());
        return true;
    }

    /** @return current task handle, possibly stale/done until {@link #isRunning()} refreshes it. */
    public RunningTaskHandle getCurrentTask() { return currentTask; }

    /** @return owning window runtime context. */
    public WindowRuntimeContext getWindowContext() { return windowContext; }

    /** @return true when this runner currently has an active queue. */
    public boolean isRunning() {
        return getActiveTaskHandle() != null;
    }

    /** @return true when the runner is open and no queue is active. */
    public boolean canAcceptTaskQueue() {
        return !shutdown && !isRunning();
    }

    /** @return true after {@link #shutdownNow()} has been called. */
    public boolean isShutdown() { return shutdown; }

    /**
     * Build a UI snapshot of this runner.
     *
     * @return immutable-ish snapshot containing runtime state, native binding, queue progress, and
     * player identity values known on this window.
     */
    public WindowTaskSnapshot snapshot() {
        RunningTaskHandle taskHandle = getActiveTaskHandle();
        boolean running = taskHandle != null && taskHandle.isRunning();
        PlayerCharacter me = windowContext.getGameState().getMe();
        return new WindowTaskSnapshot(
                windowContext.getWindowId(),
                windowContext.getRoleName(),
                windowContext.getRole(),
                windowContext.getStatus(),
                windowContext.getSelectedTaskType(),
                taskHandle == null ? null : taskHandle.getTaskType(),
                windowContext.getLastTaskType(),
                windowContext.getLastResult(),
                running,
                taskHandle == null ? null : taskHandle.getStartedAt(),
                windowContext.getLastStartedAt(),
                windowContext.getLastFinishedAt(),
                windowContext.getLastMessage(),
                windowContext.getLastResultMessage(),
                windowContext.getNativeBinding(),
                windowContext.getLastQueueDisplayText(),
                windowContext.getLastQueueResult(),
                windowContext.getLastQueueMessage(),
                windowContext.getLastQueueFailurePolicy(),
                taskHandle == null ? "-" : taskHandle.getTaskQueueDisplayText(),
                taskHandle == null ? "-" : taskHandle.getTaskProgressText(),
                windowContext.getRunningTaskProgressText(),
                taskHandle == null ? 0 : taskHandle.getTaskTotal(),
                taskHandle == null ? null : taskHandle.getTaskQueueFailurePolicy(),
                canAcceptTaskQueue(),
                me == null ? null : me.getName(),
                me == null ? null : me.getId(),
                me == null ? null : me.getGameServerName()
        );
    }

    /**
     * Permanently close this runner and cancel any live queue.
     */
    public void shutdownNow() {
        shutdown = true;
        settleRouteOutcomesBeforeWatcherStop("runner-shutdown");
        taskMaintenanceService.clearSummonSkillQueueForWindow(windowContext.getWindowId(), "runner-shutdown");
        taskMaintenanceService.clearPostCombatFirstAidForWindow(windowContext.getWindowId(), "runner-shutdown");
        RunningTaskHandle taskHandle = currentTask;
        if (taskHandle != null) {
            windowContext.getGameContext().runWithState(windowContext.getGameState(), () -> taskHandle.requestStop("runner closed"));
            taskHandle.forceCancel("runner closed");
        }
        combatWatcherExecutor.shutdownNow();
        dialogPreparationExecutor.shutdownNow();
        executor.shutdownNow();
    }

    /**
     * Runner-owned watcher-stop boundary for route outcomes. Consume each live or queued record
     * before a task or runner stops its watcher, so no cloud decision is left without a terminal
     * report. Atomic consume prevents a concurrent watcher settlement from double-reporting.
     */
    private void settleRouteOutcomesBeforeWatcherStop(String reason) {
        PendingRouteOutcome live = windowContext.consumePendingRouteOutcome();
        if (live != null) {
            reportRouteOutcome(live, null, RouteMemoryOutcomeReport.Result.ABANDONED, reason);
        }
        WindowRuntimeContext.PendingRouteOutcomeAbandonment abandonment;
        while ((abandonment = windowContext.pollPendingRouteOutcomeAbandonment()) != null) {
            reportRouteOutcome(abandonment.outcome(), null, RouteMemoryOutcomeReport.Result.ABANDONED,
                    normalizeNullable(abandonment.reason()) == null ? reason : abandonment.reason());
        }
        WindowRuntimeContext.PendingRouteOutcomeReplacement replacement;
        while ((replacement = windowContext.pollPendingRouteOutcomeReplacement()) != null) {
            reportRouteOutcome(replacement.outcome(), null, RouteMemoryOutcomeReport.Result.ABANDONED,
                    reason + ":before-replacement");
        }
        retryPendingRouteOutcomeDeliveries(true, reason);
        if (shutdown && !pendingRouteOutcomeDeliveries.isEmpty()) {
            log.warn("window [{}] route outcome delivery queue has {} unsent in-memory record(s) at shutdown; "
                            + "they will not survive process exit",
                    windowContext.getWindowId(), pendingRouteOutcomeDeliveries.size());
        }
    }

    /**
     * Bind the window/game state and run the accepted queue on this runner's executor thread.
     */
    private void runQueue(WindowTaskQueue queue, TaskStopToken stopToken, TaskPauseToken pauseToken) {
        RunningTaskHandle taskHandle = currentTask;
        if (taskHandle != null) {
            taskHandle.markRunningThread(Thread.currentThread());
        }
        try {
            contextHolder.runWith(windowContext,
                    () -> windowContext.getGameContext().runWithState(windowContext.getGameState(),
                            () -> runQueueWithBoundGameState(queue, stopToken, pauseToken)));
        } finally {
            taskMaintenanceService.clearSummonSkillQueueForWindow(windowContext.getWindowId(), "runner-queue-finished");
            taskMaintenanceService.clearPostCombatFirstAidForWindow(windowContext.getWindowId(), "runner-queue-finished");
            if (activeLocalTeamSessionKey != null) {
                taskMaintenanceService.completeLocalTeamSessionWindow(
                        activeLocalTeamSessionKey,
                        windowContext.getWindowId(),
                        "runner-queue-finished");
            }
            activeLocalTeamSessionKey = null;
            activeLocalLeaderWindowId = null;
            activeLocalLeaderPresent = false;
            if (taskHandle != null) {
                taskHandle.clearRunningThread();
            }
            if (currentTask == taskHandle) {
                currentTask = null;
            }
        }
    }

    private RunningTaskHandle getActiveTaskHandle() {
        RunningTaskHandle taskHandle = currentTask;
        if (taskHandle == null) {
            return null;
        }
        if (taskHandle.isRunning()) {
            return taskHandle;
        }
        if (currentTask == taskHandle) {
            currentTask = null;
        }
        return null;
    }

    /**
     * Execute every task in the queue after the correct window context and game state are bound.
     */
    private void runQueueWithBoundGameState(WindowTaskQueue queue, TaskStopToken stopToken, TaskPauseToken pauseToken) {
        log.info("window [{}] start task queue: {}", windowContext.getWindowId(), queue.toLogText());
        windowContext.clearTaskQueueStartupPreparationState("task queue started");
        List<TaskType> taskTypes = queue.getTaskTypes();
        WindowTaskFailurePolicy failurePolicy = queue.getFailurePolicy();
        TaskRunResult queueResult = TaskRunResult.SKIPPED;
        int completedCount = 0;
        TaskType activeTaskType = TaskType.UNKNOWN;
        TaskRunResult previousTaskResult = null;
        TaskType previousRequestedTaskType = TaskType.UNKNOWN;
        try {
            for (int i = 0; i < taskTypes.size(); i++) {
                TaskType requestedTaskType = taskTypes.get(i);
                activeTaskType = requestedTaskType;
                TaskExecutionContext startupProbeContext = buildExecutionContext(requestedTaskType, stopToken, pauseToken);
                TaskCheckpoint.throwIfStopRequested(startupProbeContext, "task queue stopped before preflight");
                TaskStartupMode startupMode = deferStartupIfAlreadyInCombat(requestedTaskType, startupProbeContext);
                startupMode = resolveCleanQueueTransitionStartupMode(
                        startupMode, previousRequestedTaskType, requestedTaskType, previousTaskResult);
                TaskExecutionContext preflightContext = buildExecutionContext(requestedTaskType, stopToken, pauseToken, startupMode);

                TaskType taskType = resolveTaskTypeBeforeStart(requestedTaskType, preflightContext);
                activeTaskType = taskType == TaskType.UNKNOWN ? requestedTaskType : taskType;
                TaskCheckpoint.throwIfStopRequested(preflightContext, "task queue stopped after preflight");
                if (taskType == TaskType.UNKNOWN) {
                    windowContext.markFinished(WindowRuntimeStatus.IDLE, requestedTaskType, TaskRunResult.SKIPPED, "task skipped by team role policy");
                    log.info("{} window [{}] skip task by team role policy: requested={}",
                            preflightContext.getLogPrefix(), windowContext.getWindowId(), requestedTaskType);
                    completedCount++;
                    queueResult = mergeQueueResult(queueResult, TaskRunResult.SKIPPED);
                    previousTaskResult = TaskRunResult.SKIPPED;
                    previousRequestedTaskType = requestedTaskType;
                    continue;
                }

                GameTask task = taskFactory.createTask(windowContext, taskType);
                if (task == null) {
                    windowContext.markError("cannot create task: " + taskType);
                    log.error("window [{}] cannot create task: {}", windowContext.getWindowId(), taskType);
                    queueResult = TaskRunResult.FAILED;
                    previousTaskResult = TaskRunResult.FAILED;
                    previousRequestedTaskType = requestedTaskType;
                    if (shouldStopQueueAfterFailure(failurePolicy)) {
                        log.warn("window [{}] stop task queue after task creation failure: policy={}",
                                windowContext.getWindowId(), failurePolicy);
                        break;
                    }
                    continue;
                }

                RunningTaskHandle taskHandle = currentTask;
                if (taskHandle != null) {
                    taskHandle.updateTask(i, taskType, task);
                }
                TaskExecutionContext executionContext = buildExecutionContext(requestedTaskType, task, stopToken, pauseToken, startupMode);
                TaskRunResult result = runTaskWithBoundGameState(taskType, task, executionContext);
                completedCount++;
                queueResult = mergeQueueResult(queueResult, result);
                previousTaskResult = result;
                previousRequestedTaskType = requestedTaskType;
                if (result == TaskRunResult.STOPPED) {
                    break;
                }
                if (result == TaskRunResult.FAILED && shouldStopQueueAfterFailure(failurePolicy)) {
                    log.warn("window [{}] stop task queue after failed task: task={} policy={}",
                            windowContext.getWindowId(), taskType, failurePolicy);
                    break;
                }
            }
        } catch (TaskStopRequestedException | CancellationException e) {
            queueResult = TaskRunResult.STOPPED;
            String message = "task queue stopped: " + normalizeMessage(e.getMessage());
            windowContext.markFinished(WindowRuntimeStatus.STOPPED, activeTaskType, TaskRunResult.STOPPED, message);
            log.info("window [{}] task queue stopped before task finished: queue={} activeTask={} reason={}",
                    windowContext.getWindowId(), queue.toLogText(), activeTaskType, e.getMessage());
        } catch (RuntimeException e) {
            queueResult = TaskRunResult.FAILED;
            String message = "task queue exception before/around task start: "
                    + e.getClass().getSimpleName() + ": " + normalizeMessage(e.getMessage());
            windowContext.markFinished(WindowRuntimeStatus.ERROR, activeTaskType, TaskRunResult.FAILED, message);
            /*
             * FutureTask swallows uncaught runtime exceptions from the runner thread. Without this
             * guard the UI can stay at QUEUED while the task handle has already disappeared, which
             * makes the table show "排队中 / 未知任务" and hides the real startup failure.
             */
            log.error("window [{}] task queue crashed: queue={} activeTask={}",
                    windowContext.getWindowId(), queue.toLogText(), activeTaskType, e);
        }
        String queueMessage = "task queue result: " + queueResult
                + " completed=" + completedCount + "/" + taskTypes.size()
                + " policy=" + failurePolicy;
        windowContext.markQueueFinished(toWindowStatus(queueResult), queueResult, queue.toDisplayText(), failurePolicy, queueMessage);
        log.info("window [{}] task queue finished: {}", windowContext.getWindowId(), queue.toLogText());
    }

    private TaskStartupMode resolveCleanQueueTransitionStartupMode(TaskStartupMode startupMode,
                                                                   TaskType previousRequestedTaskType,
                                                                   TaskType requestedTaskType,
                                                                   TaskRunResult previousTaskResult) {
        if (startupMode != TaskStartupMode.NORMAL) {
            return startupMode;
        }
        if (previousTaskResult != TaskRunResult.SUCCESS) {
            return startupMode;
        }
        if (previousRequestedTaskType == null
                || previousRequestedTaskType == TaskType.UNKNOWN
                || requestedTaskType == null
                || requestedTaskType == TaskType.UNKNOWN
                || previousRequestedTaskType == requestedTaskType) {
            return startupMode;
        }
        if (!isCleanQueueTransitionStartupTask(requestedTaskType)) {
            return startupMode;
        }
        if (!windowContext.isTaskQueueStartupPreparationDone()) {
            log.info("window [{}] clean queued task transition not enabled: previous={} requested={} reason=common-startup-marker-missing",
                    windowContext.getWindowId(), previousRequestedTaskType, requestedTaskType);
            return startupMode;
        }
        log.info("window [{}] clean queued task transition startup: previous={} requested={} mode={}",
                windowContext.getWindowId(), previousRequestedTaskType, requestedTaskType,
                TaskStartupMode.CLEAN_QUEUE_TRANSITION);
        return TaskStartupMode.CLEAN_QUEUE_TRANSITION;
    }

    private boolean isCleanQueueTransitionStartupTask(TaskType taskType) {
        return taskType == TaskType.WUBEI
                || taskType == TaskType.XIULUO_V2
                || taskType == TaskType.WUHuan_V2;
    }

    /**
     * If a leader task is submitted while the bound window is already fighting, wait for combat to
     * leave before any role detection, identity/position sync, or startup hotkey preparation.
     */
    private TaskStartupMode deferStartupIfAlreadyInCombat(TaskType requestedTaskType,
                                                          TaskExecutionContext startupContext) {
        if (!shouldDeferStartupWhileInCombat(requestedTaskType)) {
            return TaskStartupMode.NORMAL;
        }
        AutoCombatService.TickResult tick = autoCombatService.probeWindowCombatStateReadOnly(
                startupContext, "task-startup-combat-check:" + requestedTaskType.getCode());
        if (tick != AutoCombatService.TickResult.IN_COMBAT) {
            return TaskStartupMode.NORMAL;
        }
        long startedAt = System.currentTimeMillis();
        int polls = 0;
        log.info("{} window [{}] startup combat defer started: requested={}",
                startupContext.getLogPrefix(), windowContext.getWindowId(), requestedTaskType);
        while (tick == AutoCombatService.TickResult.IN_COMBAT) {
            TaskCheckpoint.throwIfStopRequested(startupContext,
                    "task queue stopped while waiting startup combat exit");
            long sleepMs = Math.max(500L, Math.min(4_000L, autoCombatService.getDynamicPollingIntervalMs()));
            TaskSleep.sleepOrStop(startupContext, sleepMs,
                    "task queue stopped while waiting startup combat exit");
            tick = autoCombatService.probeWindowCombatStateReadOnly(
                    startupContext, "task-startup-combat-defer:" + requestedTaskType.getCode());
            polls++;
        }
        log.info("{} window [{}] startup combat defer finished: requested={} elapsedMs={} polls={}",
                startupContext.getLogPrefix(), windowContext.getWindowId(), requestedTaskType,
                Math.max(0L, System.currentTimeMillis() - startedAt), polls);
        return TaskStartupMode.AFTER_COMBAT_EXIT_STARTUP;
    }

    private boolean shouldDeferStartupWhileInCombat(TaskType taskType) {
        return taskType == TaskType.XIULUO_V2 || taskType == TaskType.WUBEI;
    }

    /**
     * Run one concrete task with startup initialization and task execution context binding.
     */
    private TaskRunResult runTaskWithBoundGameState(TaskType taskType, GameTask task, TaskExecutionContext executionContext) {
        long startedMs = System.currentTimeMillis();
        windowContext.markStarted(taskType);
        automationMetricsService.recordTaskStarted(executionContext);
        log.info("{} window [{}] start task: {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName());

        TaskRunResult result = TaskRunResult.FAILED;
        AtomicReference<String> finishMessage = new AtomicReference<>("task result: " + result);
        AtomicReference<String> errorCode = new AtomicReference<>();
        try {
            result = taskExecutionContextHolder.callWith(executionContext, () -> {
                if (!startupInitializer.beforeTask(windowContext, executionContext)) {
                    finishMessage.set("window startup initialization failed");
                    log.warn("{} window [{}] startup initialization failed, skip task: {}",
                            executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName());
                    return TaskRunResult.FAILED;
                }
                executionContext.throwIfStopRequested();
                CombatWatcherHandle combatWatcher = startCombatWatcherIfNeeded(taskType, executionContext);
                try {
                    return task.execute(executionContext);
                } finally {
                    settleRouteOutcomesBeforeWatcherStop("task-watcher-stopped:" + taskType.getCode());
                    combatWatcher.stop();
                }
            });
            if (!"window startup initialization failed".equals(finishMessage.get())) {
                finishMessage.set("task result: " + result);
            }
        } catch (TaskStopRequestedException | CancellationException e) {
            result = TaskRunResult.STOPPED;
            finishMessage.set("task stopped: " + normalizeMessage(e.getMessage()));
            log.info("{} window [{}] task stopped: {} reason={}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), e.getMessage());
        } catch (Exception e) {
            result = TaskRunResult.FAILED;
            errorCode.set(e.getClass().getSimpleName());
            finishMessage.set("task exception: " + e.getClass().getSimpleName()
                    + ": " + normalizeMessage(e.getMessage()));
            log.error("{} window [{}] task error: {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), e);
        } catch (Throwable e) {
            result = TaskRunResult.FAILED;
            errorCode.set(e.getClass().getSimpleName());
            finishMessage.set("task fatal throwable: " + e.getClass().getSimpleName()
                    + ": " + normalizeMessage(e.getMessage()));
            log.error("{} window [{}] task fatal error: {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), e);
            if (e instanceof Error error) {
                throw error;
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e);
        } finally {
            WindowRuntimeStatus status = toWindowStatus(result);
            windowContext.markFinished(status, taskType, result, finishMessage.get());
            automationMetricsService.recordTaskFinished(executionContext, taskType, result, finishMessage.get(),
                    Math.max(0L, System.currentTimeMillis() - startedMs), errorCode.get());
            log.info("{} window [{}] task finished: {} -> {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), result);
        }
        return result;
    }

    /**
     * Start the window-level observer for combat and opt-in pathing probes.
     *
     * <p>Normal business tasks still use this watcher as a combat guard. The debug navigation stress
     * task uses the same thread as a pathing-only observer, so it can validate window-level arrival
     * signals without triggering auto-combat input.</p>
     *
     * @param taskType resolved task type currently executing in this window.
     * @param executionContext bound execution context shared with the task for stop checks.
     * @return handle that must be stopped when the task exits.
     */
    private CombatWatcherHandle startCombatWatcherIfNeeded(TaskType taskType, TaskExecutionContext executionContext) {
        if (!shouldRunWindowObserver(taskType)) {
            return CombatWatcherHandle.noop();
        }
        AtomicBoolean running = new AtomicBoolean(true);
        Future<?> future = combatWatcherExecutor.submit(() -> runCombatWatcherLoop(taskType, executionContext, running));
        log.info("{} window [{}] window observer started for task={} combatGuard={} pathingProbe={}",
                executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
            shouldRunWindowObserver(taskType), false);
        return new CombatWatcherHandle(running, future);
    }

    private boolean shouldRunWindowObserver(TaskType taskType) {
        return taskType == TaskType.WUHuan_V2
            || taskType == TaskType.XIULUO_V2
            || taskType == TaskType.WUBEI;
    }

    private void runCombatWatcherLoop(TaskType taskType,
                                      TaskExecutionContext executionContext,
                                      AtomicBoolean running) {
        try {
            contextHolder.runWith(windowContext,
                    () -> windowContext.getGameContext().runWithState(windowContext.getGameState(),
                            () -> taskExecutionContextHolder.callWith(executionContext, () -> {
                                boolean combatGuardEnabled = shouldRunWindowObserver(taskType);
                                if (combatGuardEnabled) {
                                    autoCombatService.initializeForCurrentWindow();
                                    // CR252: a new task run never inherits the previous run's
                                    // enter-battle detection authorization.
                                    autoCombatService.revokeCombatDetectionAuthority(
                                            "watcher-start:" + taskType.getCode());
                                }
                                // CR253: a new task run never inherits the previous run's
                                // green-chain schedule or typed prepared jobs.
                                windowContext.clearXiuluoGreenChainSchedule(
                                        "watcher-start:" + taskType.getCode());
                                AutoCombatService.TickResult lastCombatTick = AutoCombatService.TickResult.NONE;
                                PostCombatIdleTracker postCombatIdleTracker = new PostCombatIdleTracker();
                                while (running.get() && !Thread.currentThread().isInterrupted()) {
                                    if (executionContext.isPauseRequested()) {
                                        TaskCheckpoint.throwIfStopRequested(executionContext.getStopToken(),
                                                "combat watcher interrupted during paused read-only observation");
                                        long tickStartedAt = System.currentTimeMillis();
                                        postCombatIdleTracker.onPauseTick(tickStartedAt);
                                        long observerWakeSeq = windowContext.getObserverWakeSeq();
                                        long combatStartedAt = System.currentTimeMillis();
                                        AutoCombatService.TickResult tick = combatGuardEnabled
                                                ? autoCombatService.probePausedWindowCombatStateReadOnly(
                                                        executionContext,
                                                        "window-combat-watch:paused-readonly:" + taskType.getCode())
                                                : AutoCombatService.TickResult.NONE;
                                        long combatElapsedMs = Math.max(0L, System.currentTimeMillis() - combatStartedAt);
                                        if (combatGuardEnabled && tick != lastCombatTick) {
                                            log.info("[latency] event=paused-readonly.combat.state.changed windowId={} hwnd={} task={} source={} oldTick={} newTick={} elapsedMs={} inputAllowed=false preparedActionAllowed=false",
                                                    windowContext.getWindowId(),
                                                    windowContext.getNativeBinding() == null
                                                            ? null
                                                            : windowContext.getNativeBinding().getNativeHandle(),
                                                    taskType, executionContext.getLogPrefix(), lastCombatTick, tick, combatElapsedMs);
                                            lastCombatTick = tick;
                                        }
                                        long pathingStartedAt = System.currentTimeMillis();
                                        WindowPathingSnapshot pathingSnapshot = refreshPathingSignal(
                                                taskType, executionContext, true);
                                        long pathingElapsedMs = Math.max(0L, System.currentTimeMillis() - pathingStartedAt);
                                        String observerBranch = "paused-readonly-observer";
                                        long intervalMs = pathingSnapshot != null && pathingSnapshot.hasActiveIntent()
                                                ? WINDOW_PATHING_PROBE_ACTIVE_INTERVAL_MS
                                                : WINDOW_PAUSED_READONLY_INTERVAL_MS;
                                        log.info("{} window [{}] paused-readonly-observer tick: task={} tick={} pathingState={} pathingTarget={} pathingCurrent={} inputAllowed=false preparedActionAllowed=false intervalMs={}",
                                                executionContext.getLogPrefix(), windowContext.getWindowId(),
                                                taskType, tick,
                                                pathingSnapshot == null ? null : pathingSnapshot.getState(),
                                                pathingSnapshot == null || pathingSnapshot.getIntent() == null
                                                        ? null : pathingSnapshot.getIntent().getTargetMapName(),
                                                pathingSnapshot == null ? null : pathingSnapshot.getCurrentMapName(),
                                                intervalMs);
                                        logSlowObserverTick(taskType, executionContext, observerBranch,
                                                pathingSnapshot == null ? null : pathingSnapshot.getIntent(), pathingSnapshot,
                                                null, combatElapsedMs, pathingElapsedMs, -1L,
                                                -1L, -1L, -1L, -1L, -1L, -1L,
                                                Math.max(0L, System.currentTimeMillis() - tickStartedAt), intervalMs);
                                        if (!sleepObserver(intervalMs, observerWakeSeq)) {
                                            break;
                                        }
                                        continue;
                                    }
                                    retryPendingRouteOutcomeDeliveries(false, "watcher-tick");
                                    executionContext.throwIfStopRequested();
                                    long tickStartedAt = System.currentTimeMillis();
                                    long observerWakeSeq = windowContext.getObserverWakeSeq();
                                    long combatStartedAt = System.currentTimeMillis();
                                    AutoCombatService.TickResult tick = combatGuardEnabled
                                            ? autoCombatService.handleWindowCombatGuardTick(
                                                    executionContext, "window-combat-watch:" + taskType.getCode())
                                            : AutoCombatService.TickResult.NONE;
                                    long combatElapsedMs = Math.max(0L, System.currentTimeMillis() - combatStartedAt);
                                    postCombatIdleTracker.onCombatTick(lastCombatTick, tick, System.currentTimeMillis());
                                    if (combatGuardEnabled && tick != lastCombatTick) {
                                        publishCombatStateChanged(taskType, executionContext, lastCombatTick, tick, combatElapsedMs);
                                        lastCombatTick = tick;
                                    }
                                    publishOrdinaryPreBattleTimeoutIfNeeded(taskType, executionContext, tick);
                                    Optional<WindowPathingIntent> activePathingIntentSnapshot =
                                            windowContext.getActivePathingIntent();
                                    boolean pathingIntentActive = activePathingIntentSnapshot.isPresent();
                                    settleQueuedRouteOutcomeReplacements(executionContext);
                                    settleQueuedRouteOutcomeAbandonments(executionContext);
                                    settleOrphanedRouteOutcome(activePathingIntentSnapshot.orElse(null), executionContext);
                                    PreparedDialogAction preparedDialogAction = null;
                                    WindowPathingSnapshot pathingSnapshot = null;
                                    TickDialogProbe tickDialogProbe = null;
                                    long pathingElapsedMs = -1L;
                                    long routePrepareElapsedMs = -1L;
                                    long taskDialogPrepareElapsedMs = -1L;
                                    long taskTrackerPrepareElapsedMs = -1L;
                                    long attentionDetectElapsedMs = -1L;
                                    long attentionPublishElapsedMs = -1L;
                                    long attentionRoutePrepareElapsedMs = -1L;
                                    long attentionTotalElapsedMs = -1L;
                                    String observerBranch = pathingIntentActive ? "active-pathing" : "idle";
                                    if (tick == AutoCombatService.TickResult.IN_COMBAT) {
                                        observerBranch = "in-combat";
                                        long intervalMs = autoCombatService.getDynamicPollingIntervalMs();
                                        logSlowObserverTick(taskType, executionContext, observerBranch,
                                                activePathingIntentSnapshot.orElse(null), pathingSnapshot,
                                                preparedDialogAction, combatElapsedMs, pathingElapsedMs, routePrepareElapsedMs,
                                                taskDialogPrepareElapsedMs,
                                                taskTrackerPrepareElapsedMs, attentionDetectElapsedMs,
                                                attentionPublishElapsedMs, attentionRoutePrepareElapsedMs,
                                                attentionTotalElapsedMs,
                                                Math.max(0L, System.currentTimeMillis() - tickStartedAt), intervalMs);
                                        if (!sleepObserver(intervalMs, observerWakeSeq)) {
                                            break;
                                        }
                                    } else {
                                        tickDialogProbe = new TickDialogProbe(taskType, executionContext);
                                        if (pathingIntentActive) {
                                        boolean taskDialogInterestActive = hasTaskDialogInterest(taskType);
                                        boolean probeOnlyInterest = taskDialogInterestActive
                                                && isProbeOnlyTaskDialogInterest(taskType);
                                        if (probeOnlyInterest) {
                                            /*
                                             * CR232: probe-only interest (修罗 shortcut enter-battle). During active
                                             * pathing the watcher must NOT run generic dialog detection; only the
                                             * provider's local small-ROI template probe may run, and it is delay-gated
                                             * by probeStartAtMs inside the preparation signal.
                                             */
                                            observerBranch = "active-pathing-local-probe-only";
                                            long taskDialogPrepareStartedAt = System.currentTimeMillis();
                                            preparedDialogAction = refreshTaskDialogInterestPreparationSignal(
                                                    taskType, executionContext, tickDialogProbe);
                                            taskDialogPrepareElapsedMs = Math.max(0L, System.currentTimeMillis() - taskDialogPrepareStartedAt);
                                        } else if (taskDialogInterestActive) {
                                            /*
                                             * Some released pathing flows intentionally wait for a task dialog while
                                             * standing still, e.g. 五倍 uses 显形镜 and waits for WUBEI_PROBE_STORY.
                                             * In that state the dialog is the stronger signal; running the minimap
                                             * pathing probe first can spend seconds in coordinate OCR and delay the
                                             * already requested dialog/template preparation.
                                             */
                                            observerBranch = "active-pathing-dialog-first";
                                            long[] attentionTimings = new long[] {-1L, -1L, -1L, -1L};
                                            preparedDialogAction = publishTaskAttentionIfDialogVisible(
                                                    taskType, executionContext, attentionTimings, tickDialogProbe);
                                            attentionDetectElapsedMs = attentionTimings[0];
                                            attentionPublishElapsedMs = attentionTimings[1];
                                            attentionRoutePrepareElapsedMs = attentionTimings[2];
                                            attentionTotalElapsedMs = attentionTimings[3];
                                            if (preparedDialogAction == null) {
                                                long taskDialogPrepareStartedAt = System.currentTimeMillis();
                                                preparedDialogAction = refreshTaskDialogInterestPreparationSignal(
                                                        taskType, executionContext, tickDialogProbe);
                                                taskDialogPrepareElapsedMs = Math.max(0L, System.currentTimeMillis() - taskDialogPrepareStartedAt);
                                            }
                                        }
                                        /*
                                         * Pathing observation is the scheduler signal for released navigation turns.
                                         * Do it before any dialog OCR: dialog preparation can spend seconds on OCR when
                                         * no route dialog is present, and starving this snapshot makes task code keep
                                         * waiting forever even though the window has already stopped or arrived.
                                         */
                                        //它会去读小地图/坐标，然后更新 WindowPathingSnapshot。这个 snapshot 会告诉任务线程：
                                        //
                                        //这个窗口现在还在路上？
                                        //已经到目标了？
                                        //停在半路了？
                                        //小地图读不到，不确定？
                                        //
                                        //里面最终会更新这些状态：
                                        if (preparedDialogAction == null) {
                                            long pathingStartedAt = System.currentTimeMillis();
                                            pathingSnapshot = refreshPathingSignal(taskType, executionContext);
                                            pathingElapsedMs = Math.max(0L, System.currentTimeMillis() - pathingStartedAt);
                                            /*
                                             * Route-transfer dialogs are a stronger signal than pathing state. The game
                                             * can leave an option dialog open while the lightweight pathing probe still
                                             * reports ACTIVE/unknown, so do not wait for STOPPED_AWAY before preparing
                                             * the click. Phase 5 lets the runner prepare from the active intent even when
                                             * Navigation no longer writes a DialogPreparationRequest.
                                             */
                                            long routePrepareStartedAt = System.currentTimeMillis();
                                            preparedDialogAction = refreshDialogPreparationSignal(
                                                    taskType, executionContext, tickDialogProbe);
                                            routePrepareElapsedMs = Math.max(0L, System.currentTimeMillis() - routePrepareStartedAt);
                                            if (preparedDialogAction == null && !taskDialogInterestActive) {
                                                long taskDialogPrepareStartedAt = System.currentTimeMillis();
                                                preparedDialogAction = refreshTaskDialogInterestPreparationSignal(
                                                        taskType, executionContext, tickDialogProbe);
                                                taskDialogPrepareElapsedMs = Math.max(0L, System.currentTimeMillis() - taskDialogPrepareStartedAt);
                                            }
                                        }
                                    } else {
                                        long routePrepareStartedAt = System.currentTimeMillis();
                                        preparedDialogAction = refreshDialogPreparationSignal(
                                                taskType, executionContext, tickDialogProbe);
                                        routePrepareElapsedMs = Math.max(0L, System.currentTimeMillis() - routePrepareStartedAt);
                                        if (preparedDialogAction == null) {
                                            long taskDialogPrepareStartedAt = System.currentTimeMillis();
                                            preparedDialogAction = refreshTaskDialogInterestPreparationSignal(
                                                    taskType, executionContext, tickDialogProbe);
                                            taskDialogPrepareElapsedMs = Math.max(0L, System.currentTimeMillis() - taskDialogPrepareStartedAt);
                                        }
                                        if (preparedDialogAction == null) {
                                            long taskTrackerPrepareStartedAt = System.currentTimeMillis();
                                            preparedDialogAction = refreshTaskTrackerPreparationSignal(taskType, executionContext);
                                            taskTrackerPrepareElapsedMs = Math.max(0L, System.currentTimeMillis() - taskTrackerPrepareStartedAt);
                                        }
                                    }
                                    if (preparedDialogAction == null) {
                                        /*
                                         * CR232: while a pathing intent is still being walked the watcher must not
                                         * fall back to generic dialog attention (full-screen dialog detection).
                                         * Route dialog preparation and the delay-gated local template probe above
                                         * are the only allowed lookups during movement; generic attention resumes
                                         * after ARRIVED/STOPPED_AWAY or once the intent is cleared.
                                         */
                                        WindowPathingSnapshot attentionGateSnapshot = pathingSnapshot != null
                                                ? pathingSnapshot
                                                : windowContext.getPathingSnapshot();
                                        boolean pathingStillMoving = pathingIntentActive
                                                && (attentionGateSnapshot == null
                                                        || isActiveOrUnknownPathing(attentionGateSnapshot));
                                        if (pathingStillMoving) {
                                            log.info("{} window [{}] CR232 attention checkpoint: stage=skip reason=active-pathing windowId={} hwnd={} task={} branch={} pathingState={}",
                                                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                                                    windowContext.getWindowId(),
                                                    windowContext.getNativeBinding().getNativeHandle(),
                                                    taskType, observerBranch,
                                                    attentionGateSnapshot == null ? null : attentionGateSnapshot.getState());
                                        } else {
                                            long[] attentionTimings = new long[] {-1L, -1L, -1L, -1L};
                                            preparedDialogAction = publishTaskAttentionIfDialogVisible(
                                                    taskType, executionContext, attentionTimings, tickDialogProbe);
                                            attentionDetectElapsedMs = attentionTimings[0];
                                            attentionPublishElapsedMs = attentionTimings[1];
                                            attentionRoutePrepareElapsedMs = attentionTimings[2];
                                            attentionTotalElapsedMs = attentionTimings[3];
                                        }
                                    }
                                    maybePublishXiuluoSummonSkillCleanupJob(taskType, executionContext);
                                    publishPostCombatIdleTimeoutIfNeeded(
                                            taskType,
                                            executionContext,
                                            postCombatIdleTracker,
                                            pathingSnapshot == null ? windowContext.getPathingSnapshot() : pathingSnapshot,
                                            preparedDialogAction == null
                                                    ? windowContext.getPreparedDialogAction()
                                                    : preparedDialogAction,
                                            windowContext.getDialogPreparationStatus(),
                                            windowContext.getVisibleDialogSnapshot(WINDOW_DIALOG_VISIBLE_MAX_AGE_MS)
                                                    .orElse(null));
                                    long intervalMs = WINDOW_COMBAT_GUARD_IDLE_INTERVAL_MS;
                                    if (pathingSnapshot != null && pathingSnapshot.hasActiveIntent()) {
                                        intervalMs = Math.min(intervalMs, WINDOW_PATHING_PROBE_ACTIVE_INTERVAL_MS);
                                    }
                                    long dialogPrepareIntervalMs = resolveDialogPrepareIntervalMs(taskType);
                                    if (dialogPrepareIntervalMs > 0L) {
                                        intervalMs = Math.min(intervalMs, dialogPrepareIntervalMs);
                                    }
                                    logSlowObserverTick(taskType, executionContext, observerBranch,
                                            activePathingIntentSnapshot.orElse(null), pathingSnapshot,
                                            preparedDialogAction, combatElapsedMs, pathingElapsedMs, routePrepareElapsedMs,
                                            taskDialogPrepareElapsedMs,
                                            taskTrackerPrepareElapsedMs, attentionDetectElapsedMs,
                                            attentionPublishElapsedMs, attentionRoutePrepareElapsedMs,
                                            attentionTotalElapsedMs,
                                            Math.max(0L, System.currentTimeMillis() - tickStartedAt), intervalMs);
                                    if (!sleepObserver(intervalMs, observerWakeSeq)) {
                                        break;
                                    }
                                    }
                                }
                                return null;
                            })));
        } catch (TaskStopRequestedException | CancellationException e) {
            log.info("{} window [{}] combat watcher stopped: task={} reason={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("{} window [{}] combat watcher failed: task={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, e);
        }
    }

    /**
     * Sleep between observer ticks, but allow task-owned dialog interest updates to wake the watcher
     * before the normal 6s idle interval expires.
     */
    private boolean sleepObserver(long intervalMs, long initialWakeSeq) {
        long deadline = System.currentTimeMillis() + Math.max(0L, intervalMs);
        while (System.currentTimeMillis() < deadline) {
            if (windowContext.getObserverWakeSeq() != initialWakeSeq) {
                return true;
            }
            long remainingMs = Math.max(0L, deadline - System.currentTimeMillis());
            long sleepMs = Math.min(WINDOW_OBSERVER_WAKE_CHECK_INTERVAL_MS, remainingMs);
            if (sleepMs <= 0L) {
                return true;
            }
            if (!TaskSleep.sleep(sleepMs)) {
                return false;
            }
        }
        return true;
    }

    private long resolveDialogPrepareIntervalMs(TaskType taskType) {
        DialogPreparationRequest request = windowContext.getDialogPreparationRequest();
        if (request != null) {
            return prepareIntervalMs(request.getOperation());
        }
        Optional<WindowDialogInterest> interestOpt = windowContext.getDialogInterest();
        if (interestOpt.isPresent() && interestOpt.get().getTaskType() == taskType
                && interestOpt.get().getOperations() != null
                && !interestOpt.get().getOperations().isEmpty()) {
            return interestOpt.get().getOperations().stream()
                    .mapToLong(this::prepareIntervalMs)
                    .min()
                    .orElse(WINDOW_DIALOG_PREPARE_DEFAULT_INTERVAL_MS);
        }
        if (windowContext.getActivePathingIntent().isPresent()) {
            return WINDOW_DIALOG_PREPARE_ROUTE_INTERVAL_MS;
        }
        if (taskType == TaskType.WUHuan_V2) {
            return WINDOW_DIALOG_PREPARE_TASK_TRACKER_INTERVAL_MS;
        }
        return -1L;
    }

    private long prepareIntervalMs(DialogOperation operation) {
        if (operation == DialogOperation.WUBEI_ENTER_BATTLE) {
            return WINDOW_DIALOG_PREPARE_WUBEI_ENTER_BATTLE_INTERVAL_MS;
        }
        if (operation == DialogOperation.WUBEI_PROBE_STORY) {
            return WINDOW_DIALOG_PREPARE_WUBEI_PROBE_STORY_INTERVAL_MS;
        }
        if (operation == DialogOperation.ROUTE_TRANSFER) {
            return WINDOW_DIALOG_PREPARE_ROUTE_INTERVAL_MS;
        }
        if (operation == DialogOperation.TASK_TRACKER_PATHING) {
            return WINDOW_DIALOG_PREPARE_TASK_TRACKER_INTERVAL_MS;
        }
        return WINDOW_DIALOG_PREPARE_DEFAULT_INTERVAL_MS;
    }

    /**
     * Log only meaningful watcher latency so multi-window handoff stalls can be diagnosed without
     * flooding the console on every 100ms dialog-preparation tick.
     */
    private void logSlowObserverTick(TaskType taskType,
                                     TaskExecutionContext executionContext,
                                     String branch,
                                     WindowPathingIntent activeIntent,
                                     WindowPathingSnapshot pathingSnapshot,
                                     PreparedDialogAction preparedDialogAction,
                                     long combatElapsedMs,
                                     long pathingElapsedMs,
                                     long routePrepareElapsedMs,
                                     long taskDialogPrepareElapsedMs,
                                     long taskTrackerPrepareElapsedMs,
                                     long attentionDetectElapsedMs,
                                     long attentionPublishElapsedMs,
                                     long attentionRoutePrepareElapsedMs,
                                     long attentionTotalElapsedMs,
                                     long totalElapsedMs,
                                     long nextIntervalMs) {
        boolean activePathing = pathingSnapshot != null && pathingSnapshot.hasActiveIntent();
        boolean slow = totalElapsedMs >= 1_000L
                || combatElapsedMs >= 1_000L
                || pathingElapsedMs >= 1_000L
                || routePrepareElapsedMs >= 1_000L
                || taskDialogPrepareElapsedMs >= 1_000L
                || taskTrackerPrepareElapsedMs >= 1_000L
                || attentionDetectElapsedMs >= 1_000L
                || attentionPublishElapsedMs >= 1_000L
                || attentionRoutePrepareElapsedMs >= 1_000L
                || attentionTotalElapsedMs >= 1_000L;
        if (!slow && preparedDialogAction == null && !activePathing) {
            return;
        }
        WindowPathingIntent snapshotIntent = pathingSnapshot == null ? null : pathingSnapshot.getIntent();
        log.info("{} window [{}] window observer tick: task={} branch={} totalMs={} combatMs={} pathingMs={} routePrepareMs={} taskDialogPrepareMs={} taskTrackerPrepareMs={} attentionDetectMs={} attentionPublishMs={} attentionRoutePrepareMs={} attentionTotalMs={} nextIntervalMs={} activeIntentId={} activeIntentTarget={} activeIntentAgeMs={} pathingState={} pathingCurrent={} pathingTarget={} preparedOperation={} preparedTarget={}",
                executionContext.getLogPrefix(),
                windowContext.getWindowId(),
                taskType,
                branch,
                totalElapsedMs,
                combatElapsedMs,
                pathingElapsedMs,
                routePrepareElapsedMs,
                taskDialogPrepareElapsedMs,
                taskTrackerPrepareElapsedMs,
                attentionDetectElapsedMs,
                attentionPublishElapsedMs,
                attentionRoutePrepareElapsedMs,
                attentionTotalElapsedMs,
                nextIntervalMs,
                activeIntent == null ? null : activeIntent.getIntentId(),
                activeIntent == null ? null : activeIntent.getTargetMapName(),
                activeIntent == null ? -1L : Math.max(0L, System.currentTimeMillis() - activeIntent.getCreatedAtMs()),
                pathingSnapshot == null ? null : pathingSnapshot.getState(),
                pathingSnapshot == null ? null : pathingSnapshot.getCurrentMapName(),
                snapshotIntent == null ? null : snapshotIntent.getTargetMapName(),
                preparedDialogAction == null ? null : preparedDialogAction.getOperation(),
                preparedDialogAction == null ? null : preparedDialogAction.getTargetKeyword());
    }

    private void publishCombatStateChanged(TaskType taskType,
                                           TaskExecutionContext executionContext,
                                           AutoCombatService.TickResult oldTick,
                                           AutoCombatService.TickResult newTick,
                                           long detectElapsedMs) {
        WindowNativeBinding binding = windowContext.getNativeBinding();
        String hwnd = binding == null ? null : binding.getNativeHandle();
        windowReadyEventBus.publish(WindowReadyEvent.builder()
                .windowId(windowContext.getWindowId())
                .hwnd(hwnd)
                .type(WindowReadyEventType.COMBAT_STATE_CHANGED)
                .taskType(taskType)
                .source("window-combat-watch:" + taskType.getCode())
                .build());
        if (taskType == TaskType.WUBEI
                && oldTick != AutoCombatService.TickResult.IN_COMBAT
                && newTick == AutoCombatService.TickResult.IN_COMBAT) {
            clearWubeiDialogStateOnCombatEntry(oldTick, newTick, hwnd);
        }
        if (taskType == TaskType.XIULUO_V2
                && oldTick != AutoCombatService.TickResult.IN_COMBAT
                && newTick == AutoCombatService.TickResult.IN_COMBAT) {
            clearXiuluoDialogStateOnCombatEntry(oldTick, newTick, hwnd);
        }
        clearWubeiTrackerGreenPathingOnCombatEntry(taskType, oldTick, newTick, hwnd);
        clearXiuluoTrackerShortcutPathingOnCombatEntry(taskType, oldTick, newTick, hwnd);
        log.info("[latency] event=window.combat.state.changed windowId={} hwnd={} task={} source={} oldTick={} newTick={} elapsedMs={}",
                windowContext.getWindowId(), hwnd, taskType,
                executionContext == null ? null : executionContext.getLogPrefix(),
                oldTick, newTick, Math.max(0L, detectElapsedMs));
    }

    private void clearWubeiTrackerGreenPathingOnCombatEntry(TaskType taskType,
                                                            AutoCombatService.TickResult oldTick,
                                                            AutoCombatService.TickResult newTick,
                                                            String hwnd) {
        if (taskType != TaskType.WUBEI
                || oldTick == AutoCombatService.TickResult.IN_COMBAT
                || newTick != AutoCombatService.TickResult.IN_COMBAT) {
            return;
        }
        boolean cleared = windowContext.clearPathingSignalIfSourcePrefix(
                "wubei:tracker-green-click", "wubei combat entered");
        if (cleared) {
            log.info("[latency] event=wubei.tracker-green.pathing-cleared windowId={} hwnd={} reason=combat-entered oldTick={} newTick={}",
                    windowContext.getWindowId(), hwnd, oldTick, newTick);
        }
    }

    private void clearXiuluoTrackerShortcutPathingOnCombatEntry(TaskType taskType,
                                                                AutoCombatService.TickResult oldTick,
                                                                AutoCombatService.TickResult newTick,
                                                                String hwnd) {
        if (taskType != TaskType.XIULUO_V2
                || oldTick == AutoCombatService.TickResult.IN_COMBAT
                || newTick != AutoCombatService.TickResult.IN_COMBAT) {
            return;
        }
        boolean cleared = windowContext.clearPathingSignalIfSourcePrefix(
                "xiuluo-v2:tracker-shortcut", "xiuluo combat entered");
        if (cleared) {
            log.info("[latency] event=xiuluo.tracker-shortcut.pathing-cleared windowId={} hwnd={} reason=combat-entered oldTick={} newTick={}",
                    windowContext.getWindowId(), hwnd, oldTick, newTick);
        }
    }

    /**
     * CR232: clear 修罗 dialog preparation state at the combat-entry boundary. Once combat is
     * confirmed, any 修罗 enter-battle interest / request / prepared action is stale — the "看打"
     * click already worked (or combat came from elsewhere). Without this cleanup, a late watcher
     * re-preparation could publish PREPARED_ACTION_READY during combat and cause a stale re-click
     * after exit. This is the systemic half of the one-shot consumption rule.
     *
     * @param oldTick previous combat watcher tick.
     * @param newTick new combat watcher tick, expected to be {@code IN_COMBAT}.
     * @param hwnd native window handle used only for structured diagnostics.
     */
    private void clearXiuluoDialogStateOnCombatEntry(AutoCombatService.TickResult oldTick,
                                                     AutoCombatService.TickResult newTick,
                                                     String hwnd) {
        // CR253 contract: confirmed combat entry discards the green-chain schedule and every
        // pending typed prepared job of the finished attempt.
        windowContext.clearXiuluoGreenChainSchedule("xiuluo combat entered");
        WindowDialogInterest interest = windowContext.getDialogInterest().orElse(null);
        DialogPreparationRequest request = windowContext.getDialogPreparationRequest();
        PreparedDialogAction preparedAction = windowContext.getPreparedDialogAction();
        boolean clearInterest = interest != null && interest.getTaskType() == TaskType.XIULUO_V2;
        boolean clearPrepared = preparedAction != null
                && preparedAction.getOperation() == DialogOperation.XIULUO_ENTER_BATTLE;
        if (!clearInterest && !clearPrepared) {
            return;
        }
        if (clearInterest) {
            windowContext.clearDialogInterest("xiuluo combat entered");
        }
        if (clearPrepared) {
            windowContext.clearPreparedDialogAction("xiuluo combat entered");
        }
        log.info("[latency] event=xiuluo.combat-entry.dialog-cleanup windowId={} hwnd={} oldTick={} newTick={} clearedInterest={} interestOperations={} clearedPrepared={} preparedOperation={} preparedSource={} requestPresent={}",
                windowContext.getWindowId(), hwnd, oldTick, newTick,
                clearInterest, interest == null ? null : interest.getOperations(),
                clearPrepared, preparedAction == null ? null : preparedAction.getOperation(),
                preparedAction == null ? null : normalizeMessage(preparedAction.getSource()),
                request != null);
    }

    /**
     * Clear 五倍 dialog preparation state at the exact boundary where the watcher confirms combat.
     *
     * <p>五倍 may leave an interest/request/prepared action behind while racing from the final
     * route or enter-battle dialog into combat. Once combat is confirmed, those candidates are stale:
     * keeping them would make later combat ticks look like dialog preparation work and could also
     * leave an old READY action for the next phase.</p>
     *
     * @param oldTick previous combat watcher tick.
     * @param newTick new combat watcher tick, expected to be {@code IN_COMBAT}.
     * @param hwnd native window handle used only for structured diagnostics.
     */
    private void clearWubeiDialogStateOnCombatEntry(AutoCombatService.TickResult oldTick,
                                                    AutoCombatService.TickResult newTick,
                                                    String hwnd) {
        WindowDialogInterest interest = windowContext.getDialogInterest().orElse(null);
        DialogPreparationRequest request = windowContext.getDialogPreparationRequest();
        PreparedDialogAction preparedAction = windowContext.getPreparedDialogAction();
        boolean clearInterest = interest != null && interest.getTaskType() == TaskType.WUBEI;
        boolean clearRequest = request != null;
        boolean clearPrepared = !clearRequest && preparedAction != null;
        if (clearInterest) {
            windowContext.clearDialogInterest("wubei combat entered");
        }
        if (clearRequest) {
            windowContext.clearDialogPreparationRequest("wubei combat entered");
        } else if (clearPrepared) {
            windowContext.clearPreparedDialogAction("wubei combat entered");
        }
        windowContext.clearOrdinaryEnterBattleTargetMapGate("wubei combat entered");
        // CR266: the runner's confirmed combat entry is the authoritative end of the global
        // pre-battle budget; combat duration is never limited by it.
        windowContext.clearOrdinaryPreBattleTimer("wubei combat entered (runner confirmed)");
        log.info("[latency] event=wubei.combat-entry.dialog-cleanup windowId={} hwnd={} oldTick={} newTick={} clearedInterest={} interestOperations={} clearedRequest={} requestOperation={} requestTarget={} requestSource={} clearedPrepared={} preparedOperation={} preparedTarget={} preparedSource={}",
                windowContext.getWindowId(), hwnd, oldTick, newTick,
                clearInterest, interest == null ? null : interest.getOperations(),
                clearRequest, request == null ? null : request.getOperation(),
                request == null ? null : request.getTargetKeyword(),
                request == null ? null : request.getSource(),
                clearRequest || clearPrepared,
                preparedAction == null ? null : preparedAction.getOperation(),
                preparedAction == null ? null : preparedAction.getTargetKeyword(),
                preparedAction == null ? null : preparedAction.getSource());
    }

    /**
     * Publish the 五倍 ordinary-monster pre-battle timeout from the runner side.
     *
     * <p>The leader releases the task turn after the first ordinary green-link click, so this
     * timeout cannot live only in {@code WubeiTask}. The timer is intentionally not reset by
     * pathing-terminal re-navigation; it ends only when battle entry is consumed by task code or
     * the round/runtime resets.</p>
     *
     * @param taskType task currently observed by this runner.
     * @param executionContext stop-aware task context used for diagnostics.
     * @param combatTick latest combat watcher state; in-combat time must not be counted.
     */
    private void publishOrdinaryPreBattleTimeoutIfNeeded(TaskType taskType,
                                                         TaskExecutionContext executionContext,
                                                         AutoCombatService.TickResult combatTick) {
        if (taskType != TaskType.WUBEI || combatTick == AutoCombatService.TickResult.IN_COMBAT) {
            return;
        }
        long startedAt = windowContext.getOrdinaryPreBattleStartedAtMs();
        if (startedAt <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsedMs = Math.max(0L, now - startedAt);
        if (elapsedMs < WUBEI_ORDINARY_PRE_BATTLE_TIMEOUT_MS) {
            return;
        }
        if (!windowContext.markOrdinaryPreBattleTimeoutPublished(now)) {
            return;
        }
        WindowNativeBinding binding = windowContext.getNativeBinding();
        String hwnd = binding == null ? null : binding.getNativeHandle();
        windowReadyEventBus.publish(WindowReadyEvent.builder()
                .windowId(windowContext.getWindowId())
                .hwnd(hwnd)
                .type(WindowReadyEventType.PRE_BATTLE_TIMEOUT)
                .taskType(TaskType.WUBEI)
                .source("wubei:ordinary-prebattle-timeout")
                .targetKeyword(windowContext.getOrdinaryPreBattleTargetKeyword())
                .createdAtMs(now)
                .build());
        log.warn("[wubei ordinary-prebattle] timeout published by runner: windowId={} hwnd={} task={} source={} target={} elapsedMs={} timeoutMs={} context={}",
                windowContext.getWindowId(), hwnd, windowContext.getOrdinaryPreBattleTaskType(),
                windowContext.getOrdinaryPreBattleSource(), windowContext.getOrdinaryPreBattleTargetKeyword(),
                elapsedMs, WUBEI_ORDINARY_PRE_BATTLE_TIMEOUT_MS,
                executionContext == null ? null : executionContext.getLogPrefix());
    }

    /**
     * Publish CR146's post-combat idle soft event after the runner has observed the window sitting
     * still after combat exit.
     *
     * <p>The runner deliberately publishes only a ready event. It does not click, navigate, or
     * mutate 五倍/修罗 phase state; task code must consume this hint at a normal phase boundary and
     * decide how to restart its own runtime.</p>
     *
     * @param taskType task currently running in this window.
     * @param executionContext task context used only for structured diagnostics.
     * @param tracker runner-local idle tracker that owns elapsed/pause accounting.
     * @param pathingSnapshot latest window pathing/location snapshot, nullable.
     * @param preparedAction latest prepared dialog action, nullable.
     * @param dialogStatus latest dialog-preparation status, nullable.
     * @param visibleDialog latest visible dialog snapshot, nullable.
     */
    private void publishPostCombatIdleTimeoutIfNeeded(TaskType taskType,
                                                      TaskExecutionContext executionContext,
                                                      PostCombatIdleTracker tracker,
                                                      WindowPathingSnapshot pathingSnapshot,
                                                      PreparedDialogAction preparedAction,
                                                      DialogPreparationStatus dialogStatus,
                                                      WindowDialogSnapshot visibleDialog) {
        if ((taskType != TaskType.WUBEI && taskType != TaskType.XIULUO_V2) || tracker == null) {
            return;
        }
        PostCombatIdleDecision decision = tracker.evaluate(
                System.currentTimeMillis(),
                WINDOW_POST_COMBAT_IDLE_TIMEOUT_MS,
                pathingSnapshot,
                preparedAction,
                dialogStatus,
                visibleDialog);
        if (!decision.publish()) {
            return;
        }
        WindowNativeBinding binding = windowContext.getNativeBinding();
        String hwnd = binding == null ? null : binding.getNativeHandle();
        windowReadyEventBus.publish(WindowReadyEvent.builder()
                .windowId(windowContext.getWindowId())
                .hwnd(hwnd)
                .type(WindowReadyEventType.POST_COMBAT_IDLE_TIMEOUT)
                .taskType(taskType)
                .source("runner-post-combat-idle-watchdog")
                .pathingState(pathingSnapshot == null ? null : pathingSnapshot.getState())
                .pathingIntent(pathingSnapshot == null ? null : pathingSnapshot.getIntent())
                .pathingSnapshot(pathingSnapshot)
                .lastCombatExitAtMs(decision.lastCombatExitAtMs())
                .elapsedMs(decision.elapsedMs())
                .summary(decision.summary())
                .createdAtMs(System.currentTimeMillis())
                .build());
        log.warn("[post-combat-idle-watchdog] timeout published by runner: windowId={} hwnd={} task={} source=runner-post-combat-idle-watchdog lastCombatExitAtMs={} elapsedMs={} timeoutMs={} summary={} context={}",
                windowContext.getWindowId(), hwnd, taskType,
                decision.lastCombatExitAtMs(), decision.elapsedMs(), WINDOW_POST_COMBAT_IDLE_TIMEOUT_MS,
                decision.summary(), executionContext == null ? null : executionContext.getLogPrefix());
    }

    private PreparedDialogAction refreshTaskTrackerPreparationSignal(TaskType taskType,
                                                                     TaskExecutionContext executionContext) {
        if (taskType != TaskType.WUHuan_V2 || windowContext.getDialogPreparationRequest() != null) {
            return null;
        }
        long now = System.currentTimeMillis();
        WindowPathingSnapshot pathingSnapshot = windowContext.getPathingSnapshot();
        if (isWindowCombatActiveForTrackerPrepare()
                || windowContext.getActivePathingIntent().isPresent()
                || isActiveOrUnknownPathing(pathingSnapshot)) {
            log.debug("{} window [{}] skip task tracker prepare: task={} reason=active-pathing-or-combat pathingState={} probeInProgress={} activeIntentPresent={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    pathingSnapshot == null ? null : pathingSnapshot.getState(),
                    pathingSnapshot != null && pathingSnapshot.isProbeInProgress(),
                    windowContext.getActivePathingIntent().isPresent());
            return null;
        }
        PreparedDialogAction existing = windowContext.getPreparedDialogAction();
        if (existing != null) {
            if (hasHigherPriorityPreparedAction(existing)) {
                log.debug("{} window [{}] skip task tracker prepare: task={} reason=higher-priority-prepared operation={} target={} source={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                        existing.getOperation(), existing.getTargetKeyword(), existing.getSource());
                return null;
            }
            if (!existing.matches(DialogOperation.TASK_TRACKER_PATHING, "wuhuan")) {
                return null;
            }
            if (!existing.verifiedWithin(now, PREPARED_TRACKER_ACTION_MAX_AGE_MS)) {
                windowContext.clearPreparedDialogAction("stale wuhuan tracker prepared before runner refresh");
            } else {
                log.debug("{} window [{}] task tracker prepared action already current; skip background validation: task={} operation={} target={} source={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                        existing.getOperation(), existing.getTargetKeyword(), existing.getSource());
                return existing;
            }
        }

        long startedAt = System.currentTimeMillis();
        TaskTrackerPrepareOwner prepareOwner = captureTaskTrackerPrepareOwner(taskType, executionContext);
        if (!isTaskTrackerPrepareOwnerCurrent(prepareOwner, taskType, executionContext)) {
            log.debug("{} window [{}] skip task tracker prepare: task={} reason=task-owner-or-runner-stopped",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType);
            return null;
        }
        TaskTrackerPanelPrepareResult prepareResult = taskTrackerPanelService.prepareWuhuanPathingLink(
                "window-task-tracker-prepare:" + taskType.getCode(), true);
        if (prepareResult == null || (!prepareResult.hasAction() && !prepareResult.hasNegative())) {
            return null;
        }
        WindowPathingSnapshot afterPrepareSnapshot = windowContext.getPathingSnapshot();
        DialogPreparationRequest afterPrepareRequest = windowContext.getDialogPreparationRequest();
        PreparedDialogAction afterPrepareExisting = windowContext.getPreparedDialogAction();
        boolean staleOwner = !isTaskTrackerPrepareOwnerCurrent(prepareOwner, taskType, executionContext);
        boolean combatActive = isWindowCombatActiveForTrackerPrepare();
        boolean activeIntentPresent = windowContext.getActivePathingIntent().isPresent();
        boolean activeOrUnknownPathing = isActiveOrUnknownPathing(afterPrepareSnapshot);
        if (shouldDiscardTaskTrackerPrepareResultAfterPrepare(
                staleOwner,
                combatActive,
                activeIntentPresent,
                activeOrUnknownPathing,
                afterPrepareRequest,
                afterPrepareExisting)) {
            String staleReason = staleOwner
                    ? "task-owner-or-runner-stopped"
                    : afterPrepareRequest != null
                    ? "dialog-preparation-request-present"
                    : "active-pathing-combat-or-priority";
            log.info("{} window [{}] task tracker panel prepared action discarded as stale: task={} reason={} pathingState={} probeInProgress={} activeIntentPresent={} dialogPreparationRequestPresent={} preparedOperation={} preparedTarget={} elapsedMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    staleReason,
                    afterPrepareSnapshot == null ? null : afterPrepareSnapshot.getState(),
                    afterPrepareSnapshot != null && afterPrepareSnapshot.isProbeInProgress(),
                    activeIntentPresent,
                    afterPrepareRequest != null,
                    afterPrepareExisting == null ? null : afterPrepareExisting.getOperation(),
                    afterPrepareExisting == null ? null : afterPrepareExisting.getTargetKeyword(),
                    Math.max(0L, System.currentTimeMillis() - startedAt));
            return null;
        }
        PreparedDialogAction action = prepareResult.getPreparedAction();
        if (action != null) {
            PreparedDialogAction boundAction = action.toBuilder()
                    .windowId(windowContext.getWindowId())
                    .hwnd(windowContext.getNativeBinding().getNativeHandle())
                    .build();
            windowContext.clearTaskTrackerPanelNegativeResult("positive wuhuan tracker action prepared");
            windowContext.updatePreparedDialogAction(boundAction);
            publishPreparedActionReady(taskType, boundAction, executionContext,
                    "task-tracker-prepared");
            log.info("{} window [{}] task tracker panel prepared: task={} operation={} click=({}, {}) trackerPanelRegion={} wuhuanBlockRegion={} elapsedMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    boundAction.getOperation(), boundAction.getAbsoluteX(), boundAction.getAbsoluteY(),
                    prepareResult.getTrackerPanelRegion() == null ? null : prepareResult.getTrackerPanelRegion().toShortText(),
                    prepareResult.getWuhuanTrackerBlockRegion() == null ? null : prepareResult.getWuhuanTrackerBlockRegion().toShortText(),
                    Math.max(0L, System.currentTimeMillis() - startedAt));
            return boundAction;
        }
        TaskTrackerPanelNegativeResult negative = prepareResult.getNegativeResult();
        if (negative != null) {
            TaskTrackerPanelNegativeResult boundNegative = negative.toBuilder()
                    .windowId(windowContext.getWindowId())
                    .taskType(taskType)
                    .taskCode("wuhuan")
                    .observedAtMs(System.currentTimeMillis())
                    .build();
            windowContext.updateTaskTrackerPanelNegativeResult(boundNegative);
            publishTaskTrackerNegativeReady(taskType, boundNegative, executionContext,
                    "task-tracker-negative");
            log.info("{} window [{}] task tracker panel negative prepared: task={} status={} reason={} trackerPanelRegion={} wuhuanBlockRegion={} elapsedMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    boundNegative.getStatus(), boundNegative.getReason(),
                    boundNegative.getTrackerPanelRegion() == null ? null : boundNegative.getTrackerPanelRegion().toShortText(),
                    boundNegative.getWuhuanTrackerBlockRegion() == null ? null : boundNegative.getWuhuanTrackerBlockRegion().toShortText(),
                    Math.max(0L, System.currentTimeMillis() - startedAt));
        }
        return null;
    }

    static boolean shouldDiscardTaskTrackerPrepareResultAfterPrepare(boolean staleOwner,
                                                                     boolean combatActive,
                                                                     boolean activePathingIntentPresent,
                                                                     boolean activeOrUnknownPathing,
                                                                     DialogPreparationRequest afterPrepareRequest,
                                                                     PreparedDialogAction afterPrepareExisting) {
        return staleOwner
                || combatActive
                || activePathingIntentPresent
                || activeOrUnknownPathing
                || afterPrepareRequest != null
                || hasHigherPriorityPreparedAction(afterPrepareExisting);
    }

    private TaskTrackerPrepareOwner captureTaskTrackerPrepareOwner(TaskType taskType,
                                                                   TaskExecutionContext executionContext) {
        RunningTaskHandle taskHandle = currentTask;
        return new TaskTrackerPrepareOwner(
                windowContext.getWindowId(),
                taskType,
                executionContext == null ? null : executionContext.getTaskCode(),
                executionContext == null ? 0L : executionContext.getTaskRunId(),
                taskHandle,
                taskHandle == null ? -1 : taskHandle.getTaskIndex());
    }

    private boolean isTaskTrackerPrepareOwnerCurrent(TaskTrackerPrepareOwner owner,
                                                     TaskType taskType,
                                                     TaskExecutionContext executionContext) {
        if (owner == null || shutdown || Thread.currentThread().isInterrupted()
                || executionContext == null || executionContext.isStopRequested()) {
            return false;
        }
        if (!Objects.equals(owner.windowId(), windowContext.getWindowId())
                || owner.taskType() != taskType
                || !Objects.equals(owner.taskCode(), executionContext.getTaskCode())
                || owner.taskRunId() != executionContext.getTaskRunId()) {
            return false;
        }
        RunningTaskHandle taskHandle = currentTask;
        if (taskHandle == null || taskHandle != owner.taskHandle() || !taskHandle.isRunning()) {
            return false;
        }
        return taskHandle.getTaskType() == taskType
                && taskHandle.getTaskIndex() == owner.taskIndex()
                && taskHandle.getStopToken() == executionContext.getStopToken();
    }

    private record TaskTrackerPrepareOwner(String windowId,
                                           TaskType taskType,
                                           String taskCode,
                                           long taskRunId,
                                           RunningTaskHandle taskHandle,
                                           int taskIndex) {
    }

    private boolean isWindowCombatActiveForTrackerPrepare() {
        return windowContext.getGameState().getCurrentActionState() == GameContext.ActionState.IN_COMBAT;
    }

    private boolean isActiveOrUnknownPathing(WindowPathingSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (snapshot.isProbeInProgress()) {
            return true;
        }
        WindowPathingState state = snapshot.getState();
        return state == WindowPathingState.ACTIVE || state == WindowPathingState.UNKNOWN;
    }

    private static boolean hasHigherPriorityPreparedAction(PreparedDialogAction action) {
        return action != null && action.getOperation() != DialogOperation.TASK_TRACKER_PATHING;
    }

    private boolean hasTaskDialogInterest(TaskType taskType) {
        Optional<WindowDialogInterest> interestOpt = windowContext.getDialogInterest();
        if (interestOpt.isEmpty()) {
            return false;
        }
        WindowDialogInterest interest = interestOpt.get();
        return interest.getTaskType() == taskType
                && interest.getOperations() != null
                && !interest.getOperations().isEmpty();
    }

    /** CR232: true when the current interest only allows the local small-ROI template probe. */
    private boolean isProbeOnlyTaskDialogInterest(TaskType taskType) {
        WindowDialogInterest interest = windowContext.getDialogInterest().orElse(null);
        return interest != null
                && interest.getTaskType() == taskType
                && interest.isLocalTemplateProbeOnly();
    }

    private PreparedDialogAction refreshTaskDialogInterestPreparationSignal(TaskType taskType,
                                                                           TaskExecutionContext executionContext,
                                                                           TickDialogProbe tickDialogProbe) {
        DialogPreparationRequest activePreparationRequest = windowContext.getDialogPreparationRequest();
        if (activePreparationRequest != null) {
            if (taskType == TaskType.XIULUO_V2) {
                log.info("{} window [{}] CR133 task dialog prepare checkpoint: stage=skip reason=explicit-dialog-preparation-request-present windowId={} hwnd={} task={} requestOperation={} requestTarget={} requestSource={} requestAgeMs={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                        taskType, activePreparationRequest.getOperation(), activePreparationRequest.getTargetKeyword(),
                        normalizeMessage(activePreparationRequest.getSource()),
                        ageMs(System.currentTimeMillis(), activePreparationRequest.getCreatedAtMs()));
            }
            return null;
        }
        Optional<WindowDialogInterest> interestOpt = windowContext.getDialogInterest();
        if (interestOpt.isEmpty()) {
            if (taskType == TaskType.XIULUO_V2) {
                log.info("{} window [{}] CR133 task dialog prepare checkpoint: stage=skip reason=no-dialog-interest windowId={} hwnd={} task={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(), taskType);
            }
            return null;
        }
        WindowDialogInterest interest = interestOpt.get();
        if (interest.getTaskType() != taskType || interest.getOperations() == null || interest.getOperations().isEmpty()) {
            if (taskType == TaskType.XIULUO_V2) {
                log.info("{} window [{}] CR133 task dialog prepare checkpoint: stage=skip reason=interest-mismatch windowId={} hwnd={} task={} interestTask={} interestOperations={} interestSource={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                        taskType, interest.getTaskType(), interest.getOperations(), normalizeMessage(interest.getSource()));
            }
            return null;
        }
        boolean traceCr133 = taskType == TaskType.XIULUO_V2
                && interest.supports(taskType, DialogOperation.XIULUO_ENTER_BATTLE);
        // CR232: delay gate — the probe may only start at/after probeStartAtMs (accept + 25s).
        if (!interest.isProbeStartReached(System.currentTimeMillis())) {
            if (traceCr133) {
                log.info("{} window [{}] CR232 task dialog prepare checkpoint: stage=skip reason=probe-start-not-reached windowId={} hwnd={} task={} probeStartAtMs={} interestSource={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                        taskType, interest.getProbeStartAtMs(), normalizeMessage(interest.getSource()));
            }
            return null;
        }
        Optional<WindowDialogSnapshot> visibleDialogOpt = windowContext.getVisibleDialogSnapshot(
                WINDOW_DIALOG_VISIBLE_MAX_AGE_MS);
        boolean hasVisibleDialog = visibleDialogOpt.isPresent()
                && visibleDialogOpt.get().getType() != DialogType.NONE;
        boolean hasProviderOwnedAbsentSignal = interest.getOperations().stream()
                .anyMatch(this::canPrepareTaskDialogWithoutVisibleSnapshot);
        // CR232: probe-only interest captures its own small ROI and never has a generic dialog
        // snapshot (generic detection is skipped during active pathing), so it bypasses this gate.
        if (!hasVisibleDialog && !hasProviderOwnedAbsentSignal && !interest.isLocalTemplateProbeOnly()) {
            if (traceCr133) {
                log.info("{} window [{}] CR133 task dialog prepare checkpoint: stage=skip reason=no-visible-dialog-snapshot windowId={} hwnd={} task={} interestOperations={} interestSource={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                        taskType, interest.getOperations(), normalizeMessage(interest.getSource()));
            }
            return null;
        }
        PreparedDialogAction existing = windowContext.getPreparedDialogAction();
        if (existing != null) {
            if (!interest.supports(taskType, existing.getOperation())) {
                long existingAgeMs = Math.max(0L, System.currentTimeMillis() - existing.getPreparedAtMs());
                if (shouldClearPreparedActionForTaskInterest(taskType, interest, existing, visibleDialogOpt)) {
                    windowContext.clearPreparedDialogAction("task dialog interest overrides existing prepared action");
                    log.info("{} window [{}] task dialog interest overrides existing prepared action: task={} interestOperations={} existingOperation={} existingTarget={} existingAgeMs={} interestSource={}",
                            executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                            interest.getOperations(), existing.getOperation(), existing.getTargetKeyword(),
                            existingAgeMs, normalizeMessage(interest.getSource()));
                } else if (existingAgeMs <= WINDOW_DIALOG_VISIBLE_MAX_AGE_MS) {
                    log.debug("{} window [{}] task dialog interest blocked by existing prepared action: task={} interestOperations={} existingOperation={} existingTarget={} existingAgeMs={}",
                            executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                            interest.getOperations(), existing.getOperation(), existing.getTargetKeyword(),
                            existingAgeMs);
                    if (traceCr133) {
                        log.info("{} window [{}] CR133 task dialog prepare checkpoint: stage=skip reason=existing-prepared-blocks-interest windowId={} hwnd={} task={} interestOperations={} interestSource={} existingOperation={} existingTarget={} existingSource={} existingAgeMs={} existingVerifiedAgeMs={}",
                                executionContext.getLogPrefix(), windowContext.getWindowId(),
                                windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                                taskType, interest.getOperations(), normalizeMessage(interest.getSource()),
                                existing.getOperation(), existing.getTargetKeyword(), normalizeMessage(existing.getSource()),
                                existingAgeMs, ageMs(System.currentTimeMillis(), existing.getLastVerifiedAtMs()));
                    }
                    return null;
                }
                windowContext.clearPreparedDialogAction("stale prepared action does not match current interest");
            }
            PreparedDialogAction current = windowContext.getPreparedDialogAction();
            if (current != null && interest.supports(taskType, current.getOperation())) {
                long now = System.currentTimeMillis();
                long preparedAgeMs = ageMs(now, current.getPreparedAtMs());
                long verifiedAgeMs = ageMs(now, current.getLastVerifiedAtMs());
                WindowDialogSnapshot visibleDialog = visibleDialogOpt.orElse(null);
                WindowPathingSnapshot pathingSnapshot = windowContext.getPathingSnapshot();
                WindowPathingIntent activeIntent = windowContext.getActivePathingIntent().orElse(null);
                long stationaryMs = pathingSnapshot == null
                        ? -1L
                        : ageMs(now, pathingSnapshot.getLocationChangedAtMs());
                boolean stale = verifiedAgeMs < 0L || verifiedAgeMs > WINDOW_TASK_DIALOG_PREPARED_STALE_MS;
                boolean visibleMatch = visibleDialogMatchesPreparedTaskInterest(
                        taskType, interest, current, visibleDialogOpt);
                boolean pathingStoppedStationary = isStationaryForStaleTaskDialogReprepare(pathingSnapshot, now);
                long lastRepublishAtMs = lastTaskDialogStaleRepublishAtMs.get();
                long cooldownAgeMs = ageMs(now, lastRepublishAtMs);
                boolean cooldownOpen = cooldownAgeMs < 0L
                        || cooldownAgeMs >= WINDOW_TASK_DIALOG_STALE_REPUBLISH_COOLDOWN_MS;
                boolean noPathingEnterBattleReady = isNoPathingEnterBattleReadyForStaleTaskDialogReprepare(
                        taskType, interest, current, visibleDialog, activeIntent, pathingSnapshot,
                        stale, visibleMatch, cooldownOpen);
                long cooldownRemainingMs = cooldownOpen
                        ? 0L
                        : Math.max(0L, WINDOW_TASK_DIALOG_STALE_REPUBLISH_COOLDOWN_MS - cooldownAgeMs);
                if (stale && visibleMatch
                        && (pathingStoppedStationary || noPathingEnterBattleReady)
                        && cooldownOpen) {
                    /*
                     * CR144: a task turn can be blocked by maintenance long enough for the cached
                     * click to exceed task freshness. Re-run provider preparation only after the
                     * same dialog is still visible and pathing evidence says the window is stopped;
                     * this is not a background fingerprint keepalive.
                     */
                    lastTaskDialogStaleRepublishAtMs.set(now);
                    String reprepareReason = pathingStoppedStationary
                            ? "stale task dialog prepared action stationary reprepare"
                            : "stale task dialog prepared action no-pathing enter-battle reprepare";
                    windowContext.clearPreparedDialogAction(reprepareReason);
                    log.info("{} window [{}] stale task dialog prepared action reprepare: reason={} windowId={} hwnd={} task={} operation={} target={} source={} preparedAgeMs={} verifiedAgeMs={} staleMs={} visibleType={} visibleAgeMs={} pathingState={} pathingStoppedStationary={} noPathingEnterBattleReady={} stationaryMs={} cooldownAgeMs={} cooldownRemainingMs={} interestSource={} activeIntentSource={}",
                            executionContext.getLogPrefix(), windowContext.getWindowId(),
                            reprepareReason,
                            windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                            taskType, current.getOperation(), current.getTargetKeyword(),
                            normalizeMessage(current.getSource()), preparedAgeMs, verifiedAgeMs,
                            WINDOW_TASK_DIALOG_PREPARED_STALE_MS,
                            visibleDialog == null ? null : visibleDialog.getType(),
                            visibleDialog == null ? -1L : ageMs(now, visibleDialog.getDetectedAtMs()),
                            pathingSnapshot == null ? null : pathingSnapshot.getState(),
                            pathingStoppedStationary, noPathingEnterBattleReady,
                            stationaryMs, cooldownAgeMs, cooldownRemainingMs,
                            normalizeMessage(interest.getSource()),
                            activeIntent == null ? null : normalizeMessage(activeIntent.getSource()));
                } else {
                    String skipReason = !stale
                            ? "reason=not-stale"
                            : !visibleMatch
                            ? "reason=visible-mismatch"
                            : !cooldownOpen
                            ? "reason=cooldown"
                            : !(pathingStoppedStationary || noPathingEnterBattleReady)
                            ? "reason=moving-or-no-pathing-not-ready"
                            : "reason=cooldown";
                    if (stale) {
                        log.debug("{} window [{}] stale task dialog prepared action retained: {} windowId={} hwnd={} task={} operation={} target={} source={} preparedAgeMs={} verifiedAgeMs={} staleMs={} visibleType={} visibleAgeMs={} pathingState={} pathingStoppedStationary={} noPathingEnterBattleReady={} stationaryMs={} cooldownAgeMs={} cooldownRemainingMs={} interestSource={} activeIntentSource={}",
                                executionContext.getLogPrefix(), windowContext.getWindowId(), skipReason,
                                windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                                taskType, current.getOperation(), current.getTargetKeyword(),
                                normalizeMessage(current.getSource()), preparedAgeMs, verifiedAgeMs,
                                WINDOW_TASK_DIALOG_PREPARED_STALE_MS,
                                visibleDialog == null ? null : visibleDialog.getType(),
                                visibleDialog == null ? -1L : ageMs(now, visibleDialog.getDetectedAtMs()),
                                pathingSnapshot == null ? null : pathingSnapshot.getState(),
                                pathingStoppedStationary, noPathingEnterBattleReady,
                                stationaryMs, cooldownAgeMs, cooldownRemainingMs,
                                normalizeMessage(interest.getSource()),
                                activeIntent == null ? null : normalizeMessage(activeIntent.getSource()));
                    } else {
                        log.debug("{} window [{}] task dialog prepared action already current; skip background validation: {} task={} operation={} target={} source={} preparedAgeMs={} verifiedAgeMs={} visibleType={} pathingState={} pathingStoppedStationary={} noPathingEnterBattleReady={} cooldownRemainingMs={}",
                                executionContext.getLogPrefix(), windowContext.getWindowId(), skipReason,
                                taskType, current.getOperation(), current.getTargetKeyword(),
                                normalizeMessage(current.getSource()), preparedAgeMs, verifiedAgeMs,
                                visibleDialog == null ? null : visibleDialog.getType(),
                                pathingSnapshot == null ? null : pathingSnapshot.getState(),
                                pathingStoppedStationary, noPathingEnterBattleReady, cooldownRemainingMs);
                    }
                    if (traceCr133) {
                        log.info("{} window [{}] CR133 task dialog prepare checkpoint: stage=existing-current windowId={} hwnd={} task={} operation={} target={} source={} preparedAgeMs={} verifiedAgeMs={} skipReason={} visibleType={} pathingState={} pathingStoppedStationary={} noPathingEnterBattleReady={} stationaryMs={} cooldownRemainingMs={}",
                                executionContext.getLogPrefix(), windowContext.getWindowId(),
                                windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                                taskType, current.getOperation(), current.getTargetKeyword(),
                                normalizeMessage(current.getSource()), preparedAgeMs, verifiedAgeMs, skipReason,
                                visibleDialog == null ? null : visibleDialog.getType(),
                                pathingSnapshot == null ? null : pathingSnapshot.getState(),
                                pathingStoppedStationary, noPathingEnterBattleReady,
                                stationaryMs, cooldownRemainingMs);
                    }
                    return current;
                }
            }
        }

        long startedAt = System.currentTimeMillis();
        for (DialogOperation operation : interest.getOperations()) {
            if (!hasVisibleDialog && !canPrepareTaskDialogWithoutVisibleSnapshot(operation)) {
                continue;
            }
            for (WindowDialogPreparationProvider provider : dialogPreparationProviders) {
                if (!provider.supports(taskType, operation)) {
                    continue;
                }
                String source = "window-task-dialog-prepare:" + taskType.getCode() + ":" + operation;
                if (traceCr133) {
                    WindowDialogSnapshot visible = visibleDialogOpt.orElse(null);
                    log.info("{} window [{}] CR133 task dialog prepare checkpoint: stage=provider-start windowId={} hwnd={} task={} operation={} provider={} visibleType={} visibleAgeMs={} interestSource={} source={}",
                            executionContext.getLogPrefix(), windowContext.getWindowId(),
                            windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                            taskType, operation, provider.getClass().getSimpleName(),
                            visible == null ? null : visible.getType(),
                            visible == null ? -1L : ageMs(System.currentTimeMillis(), visible.getDetectedAtMs()),
                            normalizeMessage(interest.getSource()), source);
                }
                Optional<PreparedDialogAction> prepared;
                try {
                    prepared = provider.prepare(
                            interest, operation, source, tickDialogProbe.currentDetection());
                } catch (RuntimeException e) {
                    if (traceCr133) {
                        log.warn("{} window [{}] CR133 task dialog prepare checkpoint: stage=provider-exception windowId={} hwnd={} task={} operation={} provider={} source={} exception={} reason={}",
                                executionContext.getLogPrefix(), windowContext.getWindowId(),
                                windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                                taskType, operation, provider.getClass().getSimpleName(), source,
                                e.getClass().getSimpleName(), e.getMessage(), e);
                    }
                    throw e;
                }
                if (prepared.isEmpty()) {
                    if (traceCr133) {
                        log.info("{} window [{}] CR133 task dialog prepare checkpoint: stage=provider-miss windowId={} hwnd={} task={} operation={} provider={} source={} elapsedMs={}",
                                executionContext.getLogPrefix(), windowContext.getWindowId(),
                                windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                                taskType, operation, provider.getClass().getSimpleName(), source,
                                Math.max(0L, System.currentTimeMillis() - startedAt));
                    }
                    continue;
                }
                PreparedDialogAction boundAction = prepared.get().toBuilder()
                        .windowId(windowContext.getWindowId())
                        .hwnd(windowContext.getNativeBinding().getNativeHandle())
                        .build();
                /*
                 * CR253 review P1: the local kanda hit must carry the SAME attempt identity as the
                 * typed prepared jobs. While the green-chain schedule is open, stamp the current
                 * attemptId (the green click's pathing intent id, globally unique) so publish,
                 * consume, and schedule replacement all validate one rule. Outside the green chain
                 * (e.g. the WAIT_COMBAT re-registration retry) no schedule is open and the action
                 * stays unstamped with its existing semantics.
                 */
                if (taskType == TaskType.XIULUO_V2
                        && boundAction.getOperation() == DialogOperation.XIULUO_ENTER_BATTLE) {
                    XiuluoGreenChainSchedule greenChainSchedule =
                            windowContext.getXiuluoGreenChainSchedule().orElse(null);
                    if (greenChainSchedule != null) {
                        boundAction = boundAction.toBuilder()
                                .intentId(greenChainSchedule.getAttemptId())
                                .build();
                    }
                }
                windowContext.updatePreparedDialogAction(boundAction);
                publishPreparedActionReady(taskType, boundAction, executionContext,
                        "task-dialog-interest-prepared");
                log.info("{} window [{}] task dialog prepared: task={} operation={} target={} matched={} click=({}, {}) clickRequired={} elapsedMs={} interestSource={} provider={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                        boundAction.getOperation(), boundAction.getTargetKeyword(),
                        normalizeMessage(boundAction.getMatchedText()),
                        boundAction.getAbsoluteX(), boundAction.getAbsoluteY(),
                        boundAction.isClickRequired(),
                        Math.max(0L, System.currentTimeMillis() - startedAt),
                        normalizeMessage(interest.getSource()),
                        provider.getClass().getSimpleName());
                return boundAction;
            }
        }
        if (traceCr133) {
            log.info("{} window [{}] CR133 task dialog prepare checkpoint: stage=all-providers-miss windowId={} hwnd={} task={} interestOperations={} interestSource={} elapsedMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                    windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                    taskType, interest.getOperations(), normalizeMessage(interest.getSource()),
                    Math.max(0L, System.currentTimeMillis() - startedAt));
        }
        return null;
    }

    private boolean shouldClearPreparedActionForTaskInterest(TaskType taskType,
                                                             WindowDialogInterest interest,
                                                             PreparedDialogAction existing,
                                                             Optional<WindowDialogSnapshot> visibleDialogOpt) {
        if (taskType != TaskType.XIULUO_V2
                || interest == null
                || existing == null
                || existing.getOperation() != DialogOperation.ROUTE_TRANSFER
                || !interest.supports(taskType, DialogOperation.XIULUO_ENTER_BATTLE)) {
            return false;
        }
        return visibleDialogOpt
                .map(WindowDialogSnapshot::getType)
                .filter(type -> type == DialogType.OPTION)
                .isPresent();
    }

    private boolean isStationaryForStaleTaskDialogReprepare(WindowPathingSnapshot snapshot, long nowMs) {
        if (snapshot == null) {
            return false;
        }
        if (snapshot.isProbeInProgress()) {
            return false;
        }
        WindowPathingState state = snapshot.getState();
        return state == WindowPathingState.ARRIVED || state == WindowPathingState.STOPPED_AWAY;
    }

    private boolean isNoPathingEnterBattleReadyForStaleTaskDialogReprepare(TaskType taskType,
                                                                           WindowDialogInterest interest,
                                                                           PreparedDialogAction action,
                                                                           WindowDialogSnapshot visibleDialog,
                                                                           WindowPathingIntent activeIntent,
                                                                           WindowPathingSnapshot pathingSnapshot,
                                                                           boolean stale,
                                                                           boolean visibleMatch,
                                                                           boolean cooldownOpen) {
        if (!stale || !visibleMatch || !cooldownOpen) {
            return false;
        }
        if (activeIntent != null) {
            return false;
        }
        if (pathingSnapshot != null) {
            if (pathingSnapshot.isProbeInProgress()) {
                return false;
            }
            WindowPathingState state = pathingSnapshot.getState();
            if (state == WindowPathingState.ACTIVE || state == WindowPathingState.UNKNOWN) {
                return false;
            }
            if (state != WindowPathingState.NONE) {
                return false;
            }
        }
        if (taskType != TaskType.WUBEI) {
            return false;
        }
        if (action == null || action.getOperation() != DialogOperation.WUBEI_ENTER_BATTLE) {
            return false;
        }
        if (visibleDialog == null || visibleDialog.getType() != DialogType.OPTION) {
            return false;
        }
        if (action.getDialogType() != DialogType.OPTION || action.getDialogType() != visibleDialog.getType()) {
            return false;
        }
        return interest != null && interest.supports(taskType, action.getOperation());
    }

    private boolean visibleDialogMatchesPreparedTaskInterest(TaskType taskType,
                                                             WindowDialogInterest interest,
                                                             PreparedDialogAction action,
                                                             Optional<WindowDialogSnapshot> visibleDialogOpt) {
        if (interest == null || action == null || !interest.supports(taskType, action.getOperation())) {
            return false;
        }
        if (visibleDialogOpt.isEmpty()) {
            return false;
        }
        WindowDialogSnapshot visible = visibleDialogOpt.get();
        if (!Objects.equals(visible.getWindowId(), windowContext.getWindowId())) {
            return false;
        }
        Long currentHwnd = WindowHandleParser.parseHandle(windowContext.getNativeBinding().getNativeHandle());
        if (visible.getHwnd() != null && currentHwnd != null && !Objects.equals(visible.getHwnd(), currentHwnd)) {
            return false;
        }
        DialogType visibleType = visible.getType();
        if (visibleType == null || visibleType == DialogType.NONE) {
            return false;
        }
        DialogType preparedType = action.getDialogType();
        return preparedType != null && preparedType != DialogType.NONE && preparedType == visibleType;
    }

    private boolean canPrepareTaskDialogWithoutVisibleSnapshot(DialogOperation operation) {
        return operation == DialogOperation.WUBEI_PROBE_STORY;
    }

    private PreparedDialogAction publishTaskAttentionIfDialogVisible(TaskType taskType,
                                                                     TaskExecutionContext executionContext,
                                                                     long[] timingMs,
                                                                     TickDialogProbe tickDialogProbe) {
        long methodStartedAt = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        try {
            String probeSource = "window-task-attention:" + taskType.getCode();
            long detectStartedAt = System.currentTimeMillis();
            DialogDetection detection = tickDialogProbe.detect(probeSource, "attention");
            DialogType visibleType = detection == null || detection.type() == null
                    ? DialogType.NONE
                    : detection.type();
            timingMs[0] = Math.max(0L, System.currentTimeMillis() - detectStartedAt);
            if (visibleType == DialogType.NONE) {
                windowContext.clearVisibleDialogSnapshot("runner-attention-none");
                log.debug("{} window [{}] visible dialog probe none: task={} source={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, probeSource);
                timingMs[3] = Math.max(0L, System.currentTimeMillis() - methodStartedAt);
                return null;
            }
            long detectedAtMs = System.currentTimeMillis();
            windowContext.updateVisibleDialogSnapshot(WindowDialogSnapshot.builder()
                    .windowId(windowContext.getWindowId())
                    .hwnd(WindowHandleParser.parseHandle(windowContext.getNativeBinding().getNativeHandle()))
                    .type(visibleType)
                    .source(probeSource)
                    .detectedAtMs(detectedAtMs)
                    .build(), "runner-attention-probe");
            /*
             * CR255: a confirmed STORY is additionally published as the strongly-typed
             * STORY_DIALOG_VISIBLE fact. TASK_ATTENTION_REQUIRED stays too broad to drive input;
             * this event has exactly one meaning ("this window is covered by a confirmed story
             * dialog") and one consumer contract (one fast story click per sequence, executed by
             * the input-owning task boundary — never by this observer). OPTION deliberately does
             * NOT publish it: an unknown option must not be blind-clicked or disguised as a story
             * blocker; validated options keep flowing through PREPARED_ACTION_READY only. Reuses
             * the existing attention recency limit so a persisting story republishes (new
             * sequence) at the same bounded cadence.
             */
            if (visibleType == DialogType.STORY) {
                Optional<WindowReadyEvent> recentStory = windowReadyEventBus.latest(
                        windowContext.getWindowId(), WindowReadyEventType.STORY_DIALOG_VISIBLE);
                if (recentStory.isEmpty()
                        || now - recentStory.get().getCreatedAtMs() >= WINDOW_DIALOG_ATTENTION_RECENT_MS) {
                    windowReadyEventBus.publish(WindowReadyEvent.builder()
                            .windowId(windowContext.getWindowId())
                            .hwnd(windowContext.getNativeBinding().getNativeHandle())
                            .type(WindowReadyEventType.STORY_DIALOG_VISIBLE)
                            .taskType(taskType)
                            .source(probeSource + ":story-confirmed")
                            .createdAtMs(detectedAtMs)
                            .build());
                }
            }
            Optional<WindowDialogInterest> interestOpt = windowContext.getDialogInterest();
            WindowDialogInterest interest = interestOpt.orElse(null);
            WindowPathingIntent activeIntentAtVisible = windowContext.getActivePathingIntent().orElse(null);
            PreparedDialogAction existingPreparedAtVisible = windowContext.getPreparedDialogAction();
            long checkpointAt = System.currentTimeMillis();
            log.info("{} window [{}] CR133 attention checkpoint: stage=after-visible-update windowId={} hwnd={} task={} visibleDialog={} hasDetection={} hasDetectionImage={} interestPresent={} interestTask={} interestOperations={} interestSource={} interestAgeMs={} interestTtlMs={} supportsXiuluoEnterBattle={} activeIntentId={} activeIntentTarget={} activeIntentSource={} activeIntentAgeMs={} existingPreparedOperation={} existingPreparedTarget={} existingPreparedSource={} existingPreparedAgeMs={} existingPreparedVerifiedAgeMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                    windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                    taskType, visibleType,
                    detection != null,
                    detection != null && detection.image() != null,
                    interest != null,
                    interest == null ? null : interest.getTaskType(),
                    interest == null ? null : interest.getOperations(),
                    interest == null ? null : normalizeMessage(interest.getSource()),
                    interest == null ? -1L : ageMs(checkpointAt, interest.getCreatedAtMs()),
                    interest == null || interest.getExpiresAtMs() <= 0L
                            ? -1L
                            : Math.max(0L, interest.getExpiresAtMs() - checkpointAt),
                    interest != null && interest.supports(taskType, DialogOperation.XIULUO_ENTER_BATTLE),
                    activeIntentAtVisible == null ? null : activeIntentAtVisible.getIntentId(),
                    activeIntentAtVisible == null ? null : activeIntentAtVisible.getTargetMapName(),
                    activeIntentAtVisible == null ? null : activeIntentAtVisible.getSource(),
                    activeIntentAtVisible == null ? -1L : ageMs(checkpointAt, activeIntentAtVisible.getCreatedAtMs()),
                    existingPreparedAtVisible == null ? null : existingPreparedAtVisible.getOperation(),
                    existingPreparedAtVisible == null ? null : existingPreparedAtVisible.getTargetKeyword(),
                    existingPreparedAtVisible == null ? null : normalizeMessage(existingPreparedAtVisible.getSource()),
                    existingPreparedAtVisible == null ? -1L : ageMs(checkpointAt, existingPreparedAtVisible.getPreparedAtMs()),
                    existingPreparedAtVisible == null ? -1L : ageMs(checkpointAt, existingPreparedAtVisible.getLastVerifiedAtMs()));
            boolean ordinaryWubeiEnterBattleInterest = taskType == TaskType.WUBEI
                    && interestOpt
                    .filter(candidateInterest -> candidateInterest.supports(taskType, DialogOperation.WUBEI_ENTER_BATTLE))
                    .map(candidateInterest -> normalizeNullable(candidateInterest.getSource()))
                    .filter(source -> source.startsWith("wubei:normal-enter-battle"))
                    .isPresent();
            if (ordinaryWubeiEnterBattleInterest) {
                if (visibleType != DialogType.OPTION) {
                    timingMs[3] = Math.max(0L, System.currentTimeMillis() - methodStartedAt);
                    log.info("{} window [{}] ordinary enter-battle dialog ignored: task={} visibleDialog={} reason=non-option-no-wake",
                            executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, visibleType);
                    return null;
                }
                long prepareStartedAt = System.currentTimeMillis();
                PreparedDialogAction preparedAction = refreshTaskDialogInterestPreparationSignal(
                        taskType, executionContext, tickDialogProbe);
                timingMs[2] = Math.max(0L, System.currentTimeMillis() - prepareStartedAt);
                timingMs[3] = Math.max(0L, System.currentTimeMillis() - methodStartedAt);
                log.info("{} window [{}] ordinary enter-battle dialog checked: task={} visibleDialog={} prepared={} reason={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, visibleType,
                        preparedAction != null,
                        preparedAction == null ? "option-template-miss-no-wake" : "option-template-hit-prepared");
                return preparedAction;
            }
            if (isWubeiDialogVisibleAttention(taskType)) {
                long prepareStartedAt = System.currentTimeMillis();
                PreparedDialogAction preparedAction = refreshDialogPreparationSignal(
                        taskType, executionContext, tickDialogProbe);
                if (preparedAction == null) {
                    preparedAction = refreshTaskDialogInterestPreparationSignal(
                            taskType, executionContext, tickDialogProbe);
                }
                timingMs[1] = 0L;
                timingMs[2] = Math.max(0L, System.currentTimeMillis() - prepareStartedAt);
                timingMs[3] = Math.max(0L, System.currentTimeMillis() - methodStartedAt);
                log.info("{} window [{}] wubei visible dialog ignored for generic attention: task={} visibleDialog={} prepared={} reason={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, visibleType,
                        preparedAction != null,
                        preparedAction == null
                                ? "plain-dialog-no-business-wake"
                                : "explicit-prepared-action-ready");
                return preparedAction;
            }
            /*
             * Publish the soft wake before any route OCR/template preparation. Preparation can be
             * slow when the visible dialog is unstable; the scheduler should still learn quickly
             * that this window needs a foreground turn.
             */
            Optional<WindowReadyEvent> recent = windowReadyEventBus.latest(
                    windowContext.getWindowId(), WindowReadyEventType.TASK_ATTENTION_REQUIRED);
            boolean publishVisibleAttention = recent.isEmpty()
                    || now - recent.get().getCreatedAtMs() >= WINDOW_DIALOG_ATTENTION_RECENT_MS;
            log.info("{} window [{}] CR133 attention checkpoint: stage=soft-wake-decision windowId={} hwnd={} task={} visibleDialog={} publishVisibleAttention={} recentAttentionAgeMs={} recentAttentionSource={} recentAttentionTask={} recentAttentionSequence={} interestOperations={} interestSource={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                    windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                    taskType, visibleType, publishVisibleAttention,
                    recent.map(event -> ageMs(now, event.getCreatedAtMs())).orElse(-1L),
                    recent.map(WindowReadyEvent::getSource).orElse(null),
                    recent.map(WindowReadyEvent::getTaskType).orElse(null),
                    recent.map(WindowReadyEvent::getSequence).orElse(-1L),
                    interest == null ? null : interest.getOperations(),
                    interest == null ? null : normalizeMessage(interest.getSource()));
            if (publishVisibleAttention) {
                long publishStartedAt = System.currentTimeMillis();
                windowReadyEventBus.publish(WindowReadyEvent.builder()
                        .windowId(windowContext.getWindowId())
                        .hwnd(windowContext.getNativeBinding().getNativeHandle())
                        .type(WindowReadyEventType.TASK_ATTENTION_REQUIRED)
                        .taskType(taskType)
                        .source("dialog-visible:" + visibleType)
                        .createdAtMs(detectedAtMs)
                        .build());
                timingMs[1] = Math.max(0L, System.currentTimeMillis() - publishStartedAt);
            } else {
                timingMs[1] = 0L;
            }
            WindowPathingIntent activeIntent = windowContext.getActivePathingIntent().orElse(null);
            log.info("{} window [{}] task attention published: windowId={} hwnd={} task={} visibleDialog={} preparedRoute={} activeIntentId={} activeIntentTarget={} activeIntentSource={} activeIntentAgeMs={} reason={} attentionDetectMs={} attentionPublishMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                    windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                    taskType, visibleType, false,
                    activeIntent == null ? null : activeIntent.getIntentId(),
                    activeIntent == null ? null : activeIntent.getTargetMapName(),
                    activeIntent == null ? null : activeIntent.getSource(),
                    activeIntent == null ? -1L : Math.max(0L, detectedAtMs - activeIntent.getCreatedAtMs()),
                    "visible-first",
                    timingMs[0], timingMs[1]);

            /*
             * If this visible option belongs to the active route intent, prepare the click while
             * the screenshot is still fresh. The watcher still does not click or close anything;
             * task/navigation code must later consume the prepared action atomically.
             */
            long prepareStartedAt = System.currentTimeMillis();
            PreparedDialogAction preparedAction = null;
            boolean prioritizeTaskDialogInterest = shouldPrioritizeTaskDialogInterest(taskType, visibleType);
            log.info("{} window [{}] CR133 attention checkpoint: stage=prepare-chain-start windowId={} hwnd={} task={} visibleDialog={} prioritizeTaskDialogInterest={} interestOperations={} interestSource={} activeIntentId={} activeIntentTarget={} activeIntentSource={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                    windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                    taskType, visibleType, prioritizeTaskDialogInterest,
                    interest == null ? null : interest.getOperations(),
                    interest == null ? null : normalizeMessage(interest.getSource()),
                    activeIntent == null ? null : activeIntent.getIntentId(),
                    activeIntent == null ? null : activeIntent.getTargetMapName(),
                    activeIntent == null ? null : activeIntent.getSource());
            if (prioritizeTaskDialogInterest) {
                long stepStartedAt = System.currentTimeMillis();
                log.info("{} window [{}] CR133 attention checkpoint: stage=prepare-step-start step=task-dialog-interest-priority windowId={} hwnd={} task={} visibleDialog={} interestOperations={} interestSource={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                        taskType, visibleType,
                        interest == null ? null : interest.getOperations(),
                        interest == null ? null : normalizeMessage(interest.getSource()));
                preparedAction = refreshTaskDialogInterestPreparationSignal(
                        taskType, executionContext, tickDialogProbe);
                log.info("{} window [{}] CR133 attention checkpoint: stage=prepare-step-end step=task-dialog-interest-priority windowId={} hwnd={} task={} prepared={} operation={} target={} source={} elapsedMs={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                        taskType, preparedAction != null,
                        preparedAction == null ? null : preparedAction.getOperation(),
                        preparedAction == null ? null : preparedAction.getTargetKeyword(),
                        preparedAction == null ? null : normalizeMessage(preparedAction.getSource()),
                        Math.max(0L, System.currentTimeMillis() - stepStartedAt));
            }
            if (preparedAction == null) {
                long stepStartedAt = System.currentTimeMillis();
                log.info("{} window [{}] CR133 attention checkpoint: stage=prepare-step-start step=route-dialog windowId={} hwnd={} task={} visibleDialog={} activeIntentId={} activeIntentTarget={} activeIntentSource={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                        taskType, visibleType,
                        activeIntent == null ? null : activeIntent.getIntentId(),
                        activeIntent == null ? null : activeIntent.getTargetMapName(),
                        activeIntent == null ? null : activeIntent.getSource());
                preparedAction = refreshDialogPreparationSignal(
                        taskType, executionContext, tickDialogProbe);
                log.info("{} window [{}] CR133 attention checkpoint: stage=prepare-step-end step=route-dialog windowId={} hwnd={} task={} prepared={} operation={} target={} source={} elapsedMs={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                        taskType, preparedAction != null,
                        preparedAction == null ? null : preparedAction.getOperation(),
                        preparedAction == null ? null : preparedAction.getTargetKeyword(),
                        preparedAction == null ? null : normalizeMessage(preparedAction.getSource()),
                        Math.max(0L, System.currentTimeMillis() - stepStartedAt));
            }
            if (preparedAction == null) {
                long stepStartedAt = System.currentTimeMillis();
                log.info("{} window [{}] CR133 attention checkpoint: stage=prepare-step-start step=task-dialog-interest-fallback windowId={} hwnd={} task={} visibleDialog={} interestOperations={} interestSource={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                        taskType, visibleType,
                        interest == null ? null : interest.getOperations(),
                        interest == null ? null : normalizeMessage(interest.getSource()));
                preparedAction = refreshTaskDialogInterestPreparationSignal(
                        taskType, executionContext, tickDialogProbe);
                log.info("{} window [{}] CR133 attention checkpoint: stage=prepare-step-end step=task-dialog-interest-fallback windowId={} hwnd={} task={} prepared={} operation={} target={} source={} elapsedMs={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                        taskType, preparedAction != null,
                        preparedAction == null ? null : preparedAction.getOperation(),
                        preparedAction == null ? null : preparedAction.getTargetKeyword(),
                        preparedAction == null ? null : normalizeMessage(preparedAction.getSource()),
                        Math.max(0L, System.currentTimeMillis() - stepStartedAt));
            }
            timingMs[2] = Math.max(0L, System.currentTimeMillis() - prepareStartedAt);
            timingMs[3] = Math.max(0L, System.currentTimeMillis() - methodStartedAt);
            log.info("{} window [{}] task attention prepared follow-up: windowId={} hwnd={} task={} visibleDialog={} preparedRoute={} activeIntentId={} activeIntentTarget={} activeIntentSource={} activeIntentAgeMs={} reason={} attentionRoutePrepareMs={} attentionTotalMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                    windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                    taskType, visibleType, preparedAction != null,
                    activeIntent == null ? null : activeIntent.getIntentId(),
                    activeIntent == null ? null : activeIntent.getTargetMapName(),
                    activeIntent == null ? null : activeIntent.getSource(),
                    activeIntent == null ? -1L : Math.max(0L, detectedAtMs - activeIntent.getCreatedAtMs()),
                    preparedAction == null ? "visible-only" : "visible-prepared",
                    timingMs[2], timingMs[3]);
            return preparedAction;
        } catch (RuntimeException e) {
            timingMs[3] = Math.max(0L, System.currentTimeMillis() - methodStartedAt);
            log.warn("{} window [{}] CR133 attention checkpoint: stage=attention-probe-exception task={} elapsedMs={} exception={} reason={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    timingMs[3], e.getClass().getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    private boolean shouldPrioritizeTaskDialogInterest(TaskType taskType, DialogType visibleType) {
        return taskType == TaskType.XIULUO_V2
                && visibleType == DialogType.OPTION
                && windowContext.getDialogInterest()
                .filter(interest -> interest.supports(taskType, DialogOperation.XIULUO_ENTER_BATTLE))
                .isPresent();
    }

    private boolean isWubeiDialogVisibleAttention(TaskType taskType) {
        return taskType == TaskType.WUBEI;
    }

    private PreparedDialogAction refreshDialogPreparationSignal(TaskType taskType,
                                                                TaskExecutionContext executionContext,
                                                                TickDialogProbe tickDialogProbe) {
        DialogPreparationRequest request = windowContext.getDialogPreparationRequest();
        WindowPathingIntent activeIntent = windowContext.getActivePathingIntent().orElse(null);
        if (request == null && !isRoutePreparationIntent(activeIntent)) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (request != null && request.isExpired(now)) {
            windowContext.clearDialogPreparationRequest("dialog preparation request expired");
            log.info("{} window [{}] dialog preparation expired: task={} operation={} target={} source={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    request.getOperation(), request.getTargetKeyword(), request.getSource());
            return null;
        }
        String targetKeyword = resolveRoutePreparationTarget(request, activeIntent);
        String source = resolveRoutePreparationSource(request, activeIntent);
        String intentId = activeIntent == null ? null : activeIntent.getIntentId();
        long requestAgeMs = request == null ? -1L : Math.max(0L, now - request.getCreatedAtMs());
        PreparedDialogAction existing = windowContext.getPreparedDialogAction();
        if (existing != null && existing.matches(DialogOperation.ROUTE_TRANSFER, targetKeyword)
                && (existing.getIntentId() == null || Objects.equals(existing.getIntentId(), intentId))) {
            log.debug("{} window [{}] route prepared action already current; skip background validation: task={} operation={} target={} source={} intentId={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    existing.getOperation(), existing.getTargetKeyword(), existing.getSource(), existing.getIntentId());
            return existing;
        }
        if (request != null && request.getOperation() != DialogOperation.ROUTE_TRANSFER) {
            logRouteDialogPreparation("skipped-unsupported-operation", taskType, executionContext,
                    request, activeIntent, targetKeyword, null, requestAgeMs, 0L, null, null, 0, 0);
            return null;
        }
        if (targetKeyword == null) {
            if (request != null) {
                windowContext.markDialogPreparationFailed(request, "missing route target");
            }
            logRouteDialogPreparation("missing-target", taskType, executionContext,
                    request, activeIntent, null, null, requestAgeMs, 0L, null, null, 0, 0);
            return null;
        }
        Optional<WindowDialogSnapshot> visibleSnapshot = findUsableRouteDialogSnapshot(
                taskType, executionContext, request, activeIntent, targetKeyword, now, requestAgeMs);
        if (visibleSnapshot.isEmpty()) {
            return null;
        }
        long startedAt = System.currentTimeMillis();
        try {
            logRouteDialogPreparation("start", taskType, executionContext, request, activeIntent, targetKeyword,
                    visibleSnapshot.get(),
                    request == null ? -1L : Math.max(0L, startedAt - request.getCreatedAtMs()),
                    0L, null, null, 0, 0);
            if (request != null) {
                windowContext.markDialogPreparationStarted(request);
            }
            MemoryService.DialogChoiceEntry remembered = request == null
                    ? findRouteMemoryForIntent(activeIntent, targetKeyword)
                    : null;
            Integer rememberedRelativeX = request == null
                    ? remembered == null ? null : remembered.getRelativeX()
                    : request.getRememberedRelativeX();
            Integer rememberedRelativeY = request == null
                    ? remembered == null ? null : remembered.getRelativeY()
                    : request.getRememberedRelativeY();
            String rememberedOptionText = request == null
                    ? remembered == null ? null : remembered.getOptionText()
                    : request.getRememberedOptionText();
            Optional<PreparedDialogAction> prepared = runRouteDialogPreparationWithTimeout(
                    taskType,
                    executionContext,
                    request,
                    activeIntent,
                    targetKeyword,
                    source,
                    visibleSnapshot.get(),
                    rememberedRelativeX,
                    rememberedRelativeY,
                    rememberedOptionText,
                    tickDialogProbe.currentDetection(),
                    startedAt);
            return prepared
                    .map(action -> bindAndPublishRouteDialogAction(taskType, executionContext, request,
                            activeIntent, targetKeyword, intentId, visibleSnapshot.get(), startedAt, action))
                    .orElseGet(() -> {
                        if (request != null) {
                            windowContext.markDialogPreparationFailed(request, "prepare miss");
                        }
                        logRouteDialogPreparation("prepare-miss", taskType, executionContext,
                                request, activeIntent, targetKeyword, visibleSnapshot.get(),
                                request == null ? -1L : Math.max(0L, System.currentTimeMillis() - request.getCreatedAtMs()),
                                Math.max(0L, System.currentTimeMillis() - startedAt),
                                null, null, 0, 0);
                        return null;
                    });
        } catch (RuntimeException e) {
            if (request != null) {
                windowContext.markDialogPreparationFailed(request, e.getMessage());
            }
            logRouteDialogPreparation("failed:" + normalizeMessage(e.getMessage()), taskType, executionContext,
                    request, activeIntent, targetKeyword, visibleSnapshot.get(),
                    request == null ? -1L : Math.max(0L, System.currentTimeMillis() - request.getCreatedAtMs()),
                    Math.max(0L, System.currentTimeMillis() - startedAt),
                    null, null, 0, 0);
            return null;
        }
    }

    private Optional<PreparedDialogAction> runRouteDialogPreparationWithTimeout(TaskType taskType,
                                                                               TaskExecutionContext executionContext,
                                                                               DialogPreparationRequest request,
                                                                               WindowPathingIntent activeIntent,
                                                                               String targetKeyword,
                                                                               String source,
                                                                               WindowDialogSnapshot visibleSnapshot,
                                                                               Integer rememberedRelativeX,
                                                                               Integer rememberedRelativeY,
                                                                               String rememberedOptionText,
                                                                               DialogDetection suppliedDetection,
                                                                               long startedAt) {
        Future<Optional<PreparedDialogAction>> future = dialogPreparationExecutor.submit(() ->
                contextHolder.callWith(windowContext, () ->
                        taskExecutionContextHolder.callWith(executionContext, () -> {
                            if (rememberedRelativeX != null && rememberedRelativeY != null) {
                                return dialogService.prepareRememberedRouteOption(
                                        source,
                                        targetKeyword,
                                        rememberedRelativeX,
                                        rememberedRelativeY,
                                        rememberedOptionText,
                                        suppliedDetection);
                            }
                            return dialogService.prepareRouteKeywordOption(source, targetKeyword, suppliedDetection);
                        })));
        try {
            return future.get(WINDOW_ROUTE_DIALOG_PREPARE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            if (request != null) {
                windowContext.markDialogPreparationFailed(request, "route dialog preparation timed out");
            }
            logRouteDialogPreparation("prepare-timeout", taskType, executionContext,
                    request, activeIntent, targetKeyword, visibleSnapshot,
                    request == null ? -1L : Math.max(0L, System.currentTimeMillis() - request.getCreatedAtMs()),
                    Math.max(0L, System.currentTimeMillis() - startedAt),
                    null, null, 0, 0);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            if (request != null) {
                windowContext.markDialogPreparationFailed(request, "route dialog preparation interrupted");
            }
            logRouteDialogPreparation("prepare-interrupted", taskType, executionContext,
                    request, activeIntent, targetKeyword, visibleSnapshot,
                    request == null ? -1L : Math.max(0L, System.currentTimeMillis() - request.getCreatedAtMs()),
                    Math.max(0L, System.currentTimeMillis() - startedAt),
                    null, null, 0, 0);
            return Optional.empty();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            String reason = cause == null ? e.getMessage() : cause.getMessage();
            if (request != null) {
                windowContext.markDialogPreparationFailed(request, reason);
            }
            logRouteDialogPreparation("failed:" + normalizeMessage(reason), taskType, executionContext,
                    request, activeIntent, targetKeyword, visibleSnapshot,
                    request == null ? -1L : Math.max(0L, System.currentTimeMillis() - request.getCreatedAtMs()),
                    Math.max(0L, System.currentTimeMillis() - startedAt),
                    null, null, 0, 0);
            return Optional.empty();
        }
    }

    private PreparedDialogAction bindAndPublishRouteDialogAction(TaskType taskType,
                                                                 TaskExecutionContext executionContext,
                                                                 DialogPreparationRequest request,
                                                                 WindowPathingIntent activeIntent,
                                                                 String targetKeyword,
                                                                 String intentId,
                                                                 WindowDialogSnapshot visibleSnapshot,
                                                                 long startedAt,
                                                                 PreparedDialogAction action) {
        DialogPreparationRequest currentRequest = windowContext.getDialogPreparationRequest();
        WindowPathingIntent currentIntent = windowContext.getActivePathingIntent().orElse(null);
        /*
         * OCR/template work can take noticeable time. Before publishing the click candidate, ensure
         * the request or active pathing intent that authorized it is still the same one.
         */
        if (request != null && currentRequest != request) {
            windowContext.markDialogPreparationFailed(request, "stale request");
            logRouteDialogPreparation("stale-request", taskType, executionContext,
                    request, activeIntent, targetKeyword, visibleSnapshot,
                    Math.max(0L, System.currentTimeMillis() - request.getCreatedAtMs()),
                    Math.max(0L, System.currentTimeMillis() - startedAt),
                    action.getMatchedText(), null, 0, 0);
            return null;
        }
        if (request == null && !isSamePathingIntent(activeIntent, currentIntent)) {
            logRouteDialogPreparation("stale-intent", taskType, executionContext,
                    null, activeIntent, targetKeyword, visibleSnapshot,
                    -1L, Math.max(0L, System.currentTimeMillis() - startedAt),
                    action.getMatchedText(), null, 0, 0);
            return null;
        }
        PreparedDialogAction boundAction = action.toBuilder()
                .windowId(windowContext.getWindowId())
                .hwnd(windowContext.getNativeBinding().getNativeHandle())
                .intentId(intentId)
                .targetKeyword(targetKeyword)
                .build();
        windowContext.updatePreparedDialogAction(boundAction);
        publishPreparedActionReady(taskType, boundAction, executionContext,
                "route-dialog-prepared");
        logRouteDialogPreparation("prepared", taskType, executionContext,
                request, activeIntent, targetKeyword, visibleSnapshot,
                request == null ? -1L : Math.max(0L, System.currentTimeMillis() - request.getCreatedAtMs()),
                Math.max(0L, System.currentTimeMillis() - startedAt),
                boundAction.getMatchedText(), boundAction.getSource(),
                boundAction.getAbsoluteX(), boundAction.getAbsoluteY());
        return boundAction;
    }

    private String resolveRoutePreparationTarget(DialogPreparationRequest request, WindowPathingIntent activeIntent) {
        String requestTarget = normalizeNullable(request == null ? null : request.getTargetKeyword());
        if (requestTarget != null) {
            return requestTarget;
        }
        return normalizeNullable(activeIntent == null ? null : activeIntent.getTargetMapName());
    }

    private String resolveRoutePreparationSource(DialogPreparationRequest request, WindowPathingIntent activeIntent) {
        String requestSource = normalizeNullable(request == null ? null : request.getSource());
        if (requestSource != null) {
            return requestSource;
        }
        String intentSource = normalizeNullable(activeIntent == null ? null : activeIntent.getSource());
        return intentSource == null ? "window-route-intent" : intentSource;
    }

    private boolean isRoutePreparationIntent(WindowPathingIntent intent) {
        return intent != null && normalizeNullable(intent.getTargetMapName()) != null;
    }

    private MemoryService.DialogChoiceEntry findRouteMemoryForIntent(WindowPathingIntent intent,
                                                                     String targetKeyword) {
        if (intent == null || targetKeyword == null) {
            return null;
        }
        WindowPathingSnapshot snapshot = windowContext.getPathingSnapshot();
        String fromMap = snapshot == null ? null : snapshot.getCurrentMapName();
        return memoryService.findUsableRouteDialogChoice(fromMap, targetKeyword).orElse(null);
    }

    private Optional<WindowDialogSnapshot> findUsableRouteDialogSnapshot(TaskType taskType,
                                                                         TaskExecutionContext executionContext,
                                                                         DialogPreparationRequest request,
                                                                         WindowPathingIntent activeIntent,
                                                                         String targetKeyword,
                                                                         long now,
                                                                         long requestAgeMs) {
        Optional<WindowDialogSnapshot> snapshot = windowContext.getVisibleDialogSnapshot();
        if (snapshot.isEmpty()) {
            logRouteDialogPreparation("visible-absent", taskType, executionContext, request, activeIntent, targetKeyword,
                    null, requestAgeMs, 0L, null, null, 0, 0);
            return Optional.empty();
        }
        WindowDialogSnapshot visible = snapshot.get();
        long visibleAgeMs = ageMs(now, visible.getDetectedAtMs());
        if (!Objects.equals(visible.getWindowId(), windowContext.getWindowId())) {
            if (request != null) {
                windowContext.markDialogPreparationFailed(request, "visible window mismatch");
            }
            logRouteDialogPreparation("visible-window-mismatch", taskType, executionContext, request, activeIntent,
                    targetKeyword, visible, requestAgeMs, 0L, null, null, 0, 0);
            return Optional.empty();
        }
        Long currentHwnd = WindowHandleParser.parseHandle(windowContext.getNativeBinding().getNativeHandle());
        if (visible.getHwnd() == null || currentHwnd == null || !Objects.equals(visible.getHwnd(), currentHwnd)) {
            if (request != null) {
                windowContext.markDialogPreparationFailed(request, "visible hwnd mismatch");
            }
            logRouteDialogPreparation("visible-hwnd-mismatch", taskType, executionContext, request, activeIntent,
                    targetKeyword, visible, requestAgeMs, 0L, null, null, 0, 0);
            return Optional.empty();
        }
        if (visibleAgeMs > WINDOW_DIALOG_VISIBLE_MAX_AGE_MS) {
            if (request != null) {
                windowContext.markDialogPreparationFailed(request, "visible snapshot expired");
            }
            logRouteDialogPreparation("visible-expired", taskType, executionContext, request, activeIntent,
                    targetKeyword, visible, requestAgeMs, 0L, null, null, 0, 0);
            return Optional.empty();
        }
        if (visible.getType() != DialogType.OPTION) {
            logRouteDialogPreparation("visible-not-option", taskType, executionContext, request, activeIntent,
                    targetKeyword, visible, requestAgeMs, 0L, null, null, 0, 0);
            return Optional.empty();
        }
        return snapshot;
    }

    private void logRouteDialogPreparation(String result,
                                           TaskType taskType,
                                           TaskExecutionContext executionContext,
                                           DialogPreparationRequest request,
                                           WindowPathingIntent activeIntent,
                                           String targetKeyword,
                                           WindowDialogSnapshot visible,
                                           long requestAgeMs,
                                           long elapsedMs,
                                           String matchedText,
                                           String actionSource,
                                           int absoluteX,
                                           int absoluteY) {
        long now = System.currentTimeMillis();
        DialogOperation operation = request == null ? DialogOperation.ROUTE_TRANSFER : request.getOperation();
        String source = request == null
                ? activeIntent == null ? null : activeIntent.getSource()
                : request.getSource();
        long intentAgeMs = activeIntent == null ? -1L : ageMs(now, activeIntent.getCreatedAtMs());
        log.info("{} window [{}] route dialog preparation: result={} windowId={} hwnd={} intentId={} taskType={} operation={} target={} source={} actionSource={} visibleType={} visibleAgeMs={} requestAgeMs={} intentAgeMs={} matchedText={} click=({}, {}) elapsedMs={}",
                executionContext.getLogPrefix(), windowContext.getWindowId(), result,
                windowContext.getWindowId(), windowContext.getNativeBinding().getNativeHandle(),
                activeIntent == null ? null : activeIntent.getIntentId(),
                taskType, operation, targetKeyword,
                source, actionSource,
                visible == null ? null : visible.getType(),
                visible == null ? -1L : ageMs(now, visible.getDetectedAtMs()),
                requestAgeMs, intentAgeMs, normalizeMessage(matchedText), absoluteX, absoluteY, elapsedMs);
    }

    private void publishTaskTrackerNegativeReady(TaskType taskType,
                                                 TaskTrackerPanelNegativeResult negative,
                                                 TaskExecutionContext executionContext,
                                                 String reason) {
        if (negative == null || negative.getStatus() == null) {
            return;
        }
        windowReadyEventBus.publish(WindowReadyEvent.builder()
                .windowId(windowContext.getWindowId())
                .hwnd(windowContext.getNativeBinding().getNativeHandle())
                .type(WindowReadyEventType.TASK_TRACKER_NEGATIVE_READY)
                .taskType(taskType)
                .source(reason + ":" + negative.getSource())
                .operation(DialogOperation.TASK_TRACKER_PATHING)
                .targetKeyword(negative.getTaskCode())
                .summary(negative.getStatus() + ":" + negative.getReason())
                .createdAtMs(System.currentTimeMillis())
                .build());
        log.info("{} window [{}] task tracker negative ready published: task={} status={} taskCode={} source={} reason={}",
                executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                negative.getStatus(), negative.getTaskCode(), negative.getSource(), reason);
    }

    private void publishPreparedActionReady(TaskType taskType,
                                            PreparedDialogAction action,
                                            TaskExecutionContext executionContext,
                                            String reason) {
        if (action == null || action.getOperation() == null) {
            return;
        }
        windowReadyEventBus.publish(WindowReadyEvent.builder()
                .windowId(windowContext.getWindowId())
                .hwnd(windowContext.getNativeBinding().getNativeHandle())
                .type(WindowReadyEventType.PREPARED_ACTION_READY)
                .taskType(taskType)
                .source(reason + ":" + action.getSource())
                .operation(action.getOperation())
                .targetKeyword(action.getTargetKeyword())
                .createdAtMs(System.currentTimeMillis())
                .build());
        log.info("{} window [{}] prepared action ready published: task={} operation={} target={} source={} reason={} click=({}, {})",
                executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                action.getOperation(), action.getTargetKeyword(), action.getSource(), reason,
                action.getAbsoluteX(), action.getAbsoluteY());
    }

    /**
     * Refresh the background pathing signal for this window without taking the task turn.
     *
     * <p>The watcher only observes HWND-backed mini-map state and updates the per-window cached
     * player map/coordinate. It never sends input and never advances task phases. Task code can later
     * consume this fresh state immediately after reacquiring the turn.</p>
     *
     * @param taskType task currently running on this window, used only for logs.
     * @param executionContext current task execution context for stop checks and log prefix.
     * @return latest pathing snapshot, or null when there is no active pathing intent.
     */
    private WindowPathingSnapshot refreshPathingSignal(TaskType taskType, TaskExecutionContext executionContext) {
        return refreshPathingSignal(taskType, executionContext, false);
    }

    /**
     * Refresh an active pathing fact from the mini-map.
     *
     * @param taskType task currently observed by this runner.
     * @param executionContext current task context used only for stop checks and diagnostics.
     * @param pausedReadOnly whether this is a paused observer tick, where only local runtime facts
     *                       and the existing terminal wake may be updated.
     * @return latest pathing snapshot, or {@code null} when no active intent exists.
     */
    private WindowPathingSnapshot refreshPathingSignal(TaskType taskType,
                                                       TaskExecutionContext executionContext,
                                                       boolean pausedReadOnly) {
        return windowContext.getActivePathingIntent()
                .map(intent -> refreshPathingSignal(taskType, executionContext, intent, pausedReadOnly))
                .orElse(null);
    }

    private WindowPathingSnapshot refreshPathingSignal(TaskType taskType,
                                                       TaskExecutionContext executionContext,
                                                       WindowPathingIntent intent,
                                                       boolean pausedReadOnly) {
        long startedAt = System.currentTimeMillis();
        WindowPathingSnapshot previous = windowContext.getPathingSnapshot();
        if (previous != null
                && previous.getIntent() != null
                && !isSamePathingIntent(previous.getIntent(), intent)) {
            log.debug("{} window [{}] skip pathing probe because snapshot intent changed before start: task={} source={} target={}({}, {})",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    intent.getSource(), intent.getTargetMapName(), intent.getTargetX(), intent.getTargetY());
            return previous;
        }
        if (shouldReuseRecentPathingSnapshot(previous, intent, startedAt)) {
            return previous;
        }
        WindowPathingSnapshot probeSnapshot = markPathingProbeStarted(previous, intent, startedAt);
        windowContext.updatePathingSnapshot(probeSnapshot);
        try {
            return miniMapCoordinateReader.readCurrentTemplateLocation()
                    .map(location -> updatePathingFromLocation(
                            taskType, executionContext, intent, previous, location, startedAt, pausedReadOnly))
                    .orElseGet(() -> updateUnknownPathing(taskType, executionContext, intent, previous, startedAt,
                            "mini-map template location miss", pausedReadOnly));
        } catch (RuntimeException e) {
            log.debug("{} window [{}] pathing watcher probe failed: task={} source={} reason={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    intent.getSource(), e.getMessage());
            return updateUnknownPathing(taskType, executionContext, intent, previous, startedAt,
                    e.getClass().getSimpleName(), pausedReadOnly);
        }
    }

    private boolean shouldReuseRecentPathingSnapshot(WindowPathingSnapshot previous,
                                                     WindowPathingIntent intent,
                                                     long now) {
        if (previous == null || previous.getIntent() == null || !isSamePathingIntent(previous.getIntent(), intent)) {
            return false;
        }
        // Dialog preparation can wake the watcher at 100ms cadence. Reuse a fresh pathing
        // observation so multiple windows do not repeatedly queue mini-map captures for the same intent.
        if (previous.isProbeInProgress()) {
            return true;
        }
        long lastProbeAt = previous.getProbeFinishedAtMs() > 0L
                ? previous.getProbeFinishedAtMs()
                : previous.getUpdatedAtMs();
        return lastProbeAt > 0L && now - lastProbeAt < WINDOW_PATHING_PROBE_MIN_INTERVAL_MS;
    }

    private WindowPathingSnapshot markPathingProbeStarted(WindowPathingSnapshot previous,
                                                          WindowPathingIntent intent,
                                                          long startedAt) {
        if (previous == null) {
            return WindowPathingSnapshot.builder()
                    .state(WindowPathingState.ACTIVE)
                    .intent(intent)
                    .message("pathing watcher probe in progress")
                    .locationChangedAtMs(0L)
                    .updatedAtMs(0L)
                    .probeStartedAtMs(startedAt)
                    .probeInProgress(true)
                    .build();
        }
        return previous.toBuilder()
                .intent(intent)
                .probeStartedAtMs(startedAt)
                .probeInProgress(true)
                .build();
    }

    private WindowPathingSnapshot updatePathingFromLocation(TaskType taskType,
                                                            TaskExecutionContext executionContext,
                                                            WindowPathingIntent intent,
                                                            WindowPathingSnapshot previous,
                                                            TemplateLocationInfo location,
                                                            long startedAt,
                                                            boolean pausedReadOnly) {
        MapCoordinate coordinate = location.coordinate();
        long now = System.currentTimeMillis();
        if (!isCurrentPathingIntent(intent)) {
            log.info("{} window [{}] discard stale pathing probe result: task={} source={} target={}({}, {}) probeMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    intent.getSource(), intent.getTargetMapName(), intent.getTargetX(), intent.getTargetY(),
                    Math.max(0L, now - startedAt));
            return windowContext.getPathingSnapshot();
        }
        PlayerCharacter me = windowContext.getGameState().getMe();
        if (me != null && coordinate != null) {
            me.setCurrentMapName(location.mapName());
            me.setX(coordinate.getX());
            me.setY(coordinate.getY());
        }

        boolean locationChanged = hasLocationChanged(previous, location.mapName(), coordinate);
        long locationChangedAtMs = locationChanged || previous == null
                ? now
                : previous.getLocationChangedAtMs();
        /*
         * CR266: only a location change between two REAL observations of the same intent counts as
         * a movement fact. hasLocationChanged also fires on the very first observation (the
         * registration snapshot has a null location), which proves nothing about actual walking.
         * The intent-mismatch guard at the top of refreshPathingSignal already prevents carrying a
         * previous snapshot from another intent into this update.
         */
        boolean previousHasRealLocation = previous != null
                && previous.getCurrentMapName() != null
                && previous.getCurrentX() != null
                && previous.getCurrentY() != null;
        long movementObservedAtMs = locationChanged && previousHasRealLocation
                ? now
                : previous == null ? 0L : previous.getMovementObservedAtMs();
        PathingDialogBlock dialogBlock = resolvePathingDialogBlock(now);
        WindowPathingState state = classifyPathingState(intent, previous, location, locationChanged,
                locationChangedAtMs, now, dialogBlock);
        long probeMs = Math.max(0L, now - startedAt);
        WindowPathingSnapshot snapshot = WindowPathingSnapshot.builder()
                .state(state)
                .intent(intent)
                .currentMapName(location.mapName())
                .currentX(coordinate == null ? null : coordinate.getX())
                .currentY(coordinate == null ? null : coordinate.getY())
                .message(pathingMessageForState(state, "mini-map template location refreshed", dialogBlock))
                .locationChangedAtMs(locationChangedAtMs)
                .movementObservedAtMs(movementObservedAtMs)
                .updatedAtMs(now)
                .probeStartedAtMs(startedAt)
                .probeFinishedAtMs(now)
                .probeInProgress(false)
                .uiCleanupRecommended(previous != null && previous.isUiCleanupRecommended())
                .uiCleanupReason(previous == null ? null : previous.getUiCleanupReason())
                .uiCleanupRecommendedAtMs(previous == null ? 0L : previous.getUiCleanupRecommendedAtMs())
                .dialogBlocking(dialogBlock.blocking())
                .dialogBlockingReason(dialogBlock.reason())
                .dialogBlockingType(dialogBlock.dialogType())
                .dialogBlockingDetectedAtMs(dialogBlock.detectedAtMs())
                .dialogPreparationPhase(dialogBlock.preparationPhase())
                .dialogPreparationOperation(dialogBlock.operation())
                .dialogPreparationTarget(dialogBlock.targetKeyword())
                .build();
        windowContext.updatePathingSnapshot(snapshot);
        if (!pausedReadOnly) {
            openWubeiOrdinaryEnterBattleInterestIfTargetMapMatched(taskType, executionContext, location, now);
        }

        long wallStationaryMs = Math.max(0L, now - locationChangedAtMs);
        long observedStationaryMs = Math.max(0L, snapshot.getUpdatedAtMs() - locationChangedAtMs);
        boolean stateChanged = previous == null || state != previous.getState();
        if (state == WindowPathingState.ARRIVED || stateChanged) {
            log.info("{} window [{}] pathing watcher update: task={} state={} source={} target={}({}, {}) current={}({}, {}) observedStationaryMs={} wallStationaryMs={} probeMs={} dialogBlocking={} dialogAttentionOnly={} dialogReason={} dialogType={} dialogPhase={} dialogOperation={} dialogTarget={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, state,
                    intent.getSource(), intent.getTargetMapName(), intent.getTargetX(), intent.getTargetY(),
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    observedStationaryMs, wallStationaryMs, probeMs, dialogBlock.blocking(),
                    dialogBlock.blocking(), dialogBlock.reason(), dialogBlock.dialogType(),
                    dialogBlock.preparationPhase(), dialogBlock.operation(), dialogBlock.targetKeyword());
        } else if (probeMs >= WINDOW_PATHING_SLOW_PROBE_LOG_MS) {
            log.info("{} window [{}] pathing watcher slow probe: task={} state={} source={} target={}({}, {}) current={}({}, {}) observedStationaryMs={} wallStationaryMs={} probeMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, state,
                    intent.getSource(), intent.getTargetMapName(), intent.getTargetX(), intent.getTargetY(),
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    observedStationaryMs, wallStationaryMs, probeMs);
        }
        if (!pausedReadOnly) {
            logUntargetedDialogAttentionEvidence(taskType, executionContext, intent, locationChanged,
                    stateChanged, probeMs, dialogBlock);
            settlePendingTransferChoiceMemory(intent, snapshot, state, executionContext);
            settlePendingRouteOutcome(intent, snapshot, state, executionContext);
        }
        publishPathingTerminalEventIfNeeded(taskType, intent, snapshot, state, stateChanged);
        if (!pausedReadOnly) {
            maybeSubmitXiuluoGreenStopStaticArbitration(taskType, executionContext, intent, state, stateChanged);
        }
        return snapshot;
    }

    /**
     * Open 五倍 ordinary/黄袍第一战 enter-battle interest only after the tracker target map matches.
     *
     * @param taskType task currently observed by this runner.
     * @param executionContext bound execution context used for log prefix.
     * @param location latest mini-map location read by the pathing watcher.
     * @param nowMs wall-clock timestamp in milliseconds.
     */
    private void openWubeiOrdinaryEnterBattleInterestIfTargetMapMatched(TaskType taskType,
                                                                        TaskExecutionContext executionContext,
                                                                        TemplateLocationInfo location,
                                                                        long nowMs) {
        if (taskType != TaskType.WUBEI || location == null) {
            return;
        }
        String targetMap = normalizeNullable(windowContext.getOrdinaryEnterBattleTargetMapName());
        long gateStartedAt = windowContext.getOrdinaryEnterBattleTargetMapGateStartedAtMs();
        if (gateStartedAt <= 0L || targetMap == null || windowContext.getOrdinaryEnterBattleTargetMapOpenedAtMs() > 0L) {
            return;
        }
        String currentMap = normalizeNullable(location.mapName());
        if (currentMap == null) {
            return;
        }
        String gateSource = normalizeNullable(windowContext.getOrdinaryEnterBattleTargetMapSource());
        String canonicalTarget = mapNameCanonicalizer.canonicalize(
                targetMap, "wubei-tracker-green-map-gate:" + normalizeMessage(gateSource));
        String canonicalCurrent = mapNameCanonicalizer.canonicalize(
                currentMap, "wubei-current-map:" + normalizeMessage(gateSource));
        if (!Objects.equals(canonicalCurrent, canonicalTarget)) {
            log.debug("{} window [{}] ordinary enter-battle target map gate waiting: task={} source={} currentMap={} canonicalCurrent={} targetMap={} canonicalTarget={} gateAgeMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, gateSource,
                    currentMap, canonicalCurrent, targetMap, canonicalTarget, ageMs(nowMs, gateStartedAt));
            return;
        }
        if (!windowContext.markOrdinaryEnterBattleTargetMapGateOpened(nowMs)) {
            return;
        }
        String source = "wubei:normal-enter-battle-map-matched:" + normalizeMessage(gateSource);
        windowContext.updateDialogInterest(WindowDialogInterest.builder()
                .taskType(TaskType.WUBEI)
                .operations(List.of(DialogOperation.WUBEI_ENTER_BATTLE))
                .source(source)
                .build(), source);
        log.info("{} window [{}] ordinary enter-battle target map gate opened: task={} source={} currentMap={} targetMap={} canonicalMap={} gateAgeMs={}",
                executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, source,
                currentMap, targetMap, canonicalCurrent, ageMs(nowMs, gateStartedAt));
    }

    private void publishPathingTerminalEventIfNeeded(TaskType taskType,
                                                     WindowPathingIntent intent,
                                                     WindowPathingSnapshot snapshot,
                                                     WindowPathingState state,
                                                     boolean stateChanged) {
        if (!stateChanged || (state != WindowPathingState.ARRIVED && state != WindowPathingState.STOPPED_AWAY)) {
            return;
        }
        /*
         * This is deliberately a soft wake only. The watcher has already written the source of truth
         * into WindowRuntimeContext; consumers must re-read that snapshot before taking the task turn
         * or sending input.
         */
        windowReadyEventBus.publish(WindowReadyEvent.builder()
                .windowId(windowContext.getWindowId())
                .hwnd(windowContext.getNativeBinding().getNativeHandle())
                .type(WindowReadyEventType.PATHING_TERMINAL)
                .taskType(taskType)
                .source(intent == null ? null : intent.getSource())
                .targetKeyword(intent == null ? null : intent.getTargetMapName())
                .pathingState(state)
                .pathingIntent(intent)
                .pathingSnapshot(snapshot)
                .createdAtMs(snapshot == null ? System.currentTimeMillis() : snapshot.getUpdatedAtMs())
                .build());
    }

    /**
     * CR253 background terminal pipeline: a 修罗 green-chain stop (ARRIVED/STOPPED_AWAY) no longer
     * wakes the foreground. This watcher-side hook captures the moment, then submits the stop-static
     * cloud arbitration on the preparation executor. Its only outputs are typed prepared jobs
     * ({@code XIULUO_ENTER_BATTLE} on a cloud 看打 hit, {@code TRACKER_GREEN_RETRY} on the cloud's
     * explicit fallback); every other verdict keeps the foreground parked on the local probe.
     *
     * @param taskType task currently observed by this runner.
     * @param executionContext bound execution context for identity stamping and logs.
     * @param intent pathing intent that produced the terminal observation.
     * @param state terminal pathing state.
     * @param stateChanged whether this observation changed the pathing state (same trigger as the
     *                     PATHING_TERMINAL soft wake).
     */
    private void maybeSubmitXiuluoGreenStopStaticArbitration(TaskType taskType,
                                                             TaskExecutionContext executionContext,
                                                             WindowPathingIntent intent,
                                                             WindowPathingState state,
                                                             boolean stateChanged) {
        if (taskType != TaskType.XIULUO_V2
                || !stateChanged
                || (state != WindowPathingState.ARRIVED && state != WindowPathingState.STOPPED_AWAY)
                || intent == null
                || intent.getIntentId() == null
                || intent.getSource() == null
                || !intent.getSource().startsWith(XIULUO_TRACKER_SHORTCUT_SOURCE_PREFIX)) {
            return;
        }
        XiuluoGreenChainSchedule schedule = windowContext.getXiuluoGreenChainSchedule().orElse(null);
        if (schedule == null) {
            log.info("{} window [{}] xiuluo stop-static arbitration skipped: reason=no-green-chain-schedule intentId={} state={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), intent.getIntentId(), state);
            return;
        }
        if (!intent.getIntentId().equals(schedule.getAttemptId())) {
            // Round-87 class: a late terminal from a previous attempt must not trigger arbitration.
            log.info("{} window [{}] xiuluo stale-attempt terminal discarded by background pipeline: snapshotAttempt={} currentAttempt={} state={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                    intent.getIntentId(), schedule.getAttemptId(), state);
            return;
        }
        /*
         * No per-attempt dedupe here on purpose: the stateChanged gate already limits submissions
         * to real terminal transitions, and CR232's fourth review requires that an un-executed
         * re-press can be cloud-confirmed AGAIN for the same still-open attempt. Each arbitration
         * publishes at most one pending job per type, so repeats cannot double-click.
         */
        String attemptId = schedule.getAttemptId();
        log.info("{} window [{}] xiuluo pathing terminal; submit stop static for background cloud enter-battle arbitration: state={} attempt={} round={}",
                executionContext.getLogPrefix(), windowContext.getWindowId(), state, attemptId, schedule.getRound());
        dialogPreparationExecutor.submit(() ->
                contextHolder.callWith(windowContext, () ->
                        taskExecutionContextHolder.callWith(executionContext, () -> {
                            runXiuluoGreenStopStaticArbitration(taskType, executionContext, schedule, state);
                            return null;
                        })));
    }

    /**
     * Run one stop-static cloud arbitration for the current green-chain attempt and publish the
     * resulting typed prepared job. Both the pre-call and pre-publish identity checks are the
     * background half of the CR253 double invalidation gate.
     */
    private void runXiuluoGreenStopStaticArbitration(TaskType taskType,
                                                     TaskExecutionContext executionContext,
                                                     XiuluoGreenChainSchedule schedule,
                                                     WindowPathingState state) {
        XiuluoGreenChainSchedule current = windowContext.getXiuluoGreenChainSchedule().orElse(null);
        if (current == null || !current.getAttemptId().equals(schedule.getAttemptId())
                || current.getTaskRunId() != schedule.getTaskRunId()
                || current.getRound() != schedule.getRound()) {
            log.info("{} window [{}] xiuluo stop-static arbitration aborted before capture: reason=schedule-changed submitted=[{}] current=[{}]",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), schedule.identityText(),
                    current == null ? null : current.identityText());
            return;
        }
        DialogPolicyPreClickCloudDecision decision;
        try {
            decision = dialogService.decideXiuluoEnterBattleStopStatic(
                    DialogHandleRequest.handleGreenTemplateOption(
                            "xiuluo-v2:kanda-static:" + schedule.getRound() + ":attempt-" + schedule.getAttemptId(),
                            XiuluoDialogCatalog.enterBattleSpecs(),
                            true));
        } catch (RuntimeException e) {
            log.warn("{} window [{}] xiuluo stop-static arbitration failed; foreground stays parked on local probe: attempt={} round={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                    schedule.getAttemptId(), schedule.getRound(), e);
            return;
        }
        long now = System.currentTimeMillis();
        if (decision != null && decision.isCloudExecuted()
                && decision.getWindowRelativeClickPoint() != null) {
            PreparedActionJob job = PreparedActionJob.builder()
                    .type(PreparedActionJobType.XIULUO_ENTER_BATTLE)
                    .windowId(schedule.getWindowId())
                    .hwnd(schedule.getHwnd())
                    .taskRunId(schedule.getTaskRunId())
                    .round(schedule.getRound())
                    .attemptId(schedule.getAttemptId())
                    .windowRelativeX(decision.getWindowRelativeClickPoint().x)
                    .windowRelativeY(decision.getWindowRelativeClickPoint().y)
                    .matchedText(decision.getMatchedText())
                    .reason(decision.getReason())
                    .source("xiuluo-v2:background-stop-static:cloud-kanda:" + state)
                    .preparedAtMs(now)
                    .build();
            if (windowContext.publishPreparedActionJob(job, "background-stop-static-cloud-hit")) {
                publishPreparedActionJobReady(taskType, job, "background-stop-static-cloud-hit");
            }
            return;
        }
        if (decision != null && decision.getStatus() == DialogPolicyPreClickCloudDecision.Status.CLOUD_NO_ACTION) {
            PreparedActionJob job = PreparedActionJob.builder()
                    .type(PreparedActionJobType.TRACKER_GREEN_RETRY)
                    .windowId(schedule.getWindowId())
                    .hwnd(schedule.getHwnd())
                    .taskRunId(schedule.getTaskRunId())
                    .round(schedule.getRound())
                    .attemptId(schedule.getAttemptId())
                    .reason(decision.getReason())
                    .source("xiuluo-v2:background-stop-static:cloud-fallback:" + state)
                    .preparedAtMs(now)
                    .build();
            if (windowContext.publishPreparedActionJob(job, "background-stop-static-cloud-fallback")) {
                publishPreparedActionJobReady(taskType, job, "background-stop-static-cloud-fallback");
            }
            return;
        }
        // Capture failure, cloud disabled/unavailable/required-failure, or no decision: publish
        // nothing. The local kanda probe keeps running and the pre-combat watchdog stays the net.
        log.info("{} window [{}] xiuluo stop-static arbitration produced no prepared work; keep parked: attempt={} round={} status={} reason={}",
                executionContext.getLogPrefix(), windowContext.getWindowId(),
                schedule.getAttemptId(), schedule.getRound(),
                decision == null ? "CAPTURE_OR_SERVICE_UNAVAILABLE" : decision.getStatus(),
                decision == null ? null : decision.getReason());
    }

    /**
     * CR253 background summon-skill due publisher: while the 修罗 leader is parked on the green
     * chain, the watcher — not the foreground phase — detects the three-skill due state and turns
     * it into a typed {@code SUMMON_SKILL_CLEANUP} job. The consumer runs the complete maintenance
     * flow on the task thread and parks again; without this the infinite park would starve the
     * leader's summon-skill cadence.
     */
    private void maybePublishXiuluoSummonSkillCleanupJob(TaskType taskType,
                                                         TaskExecutionContext executionContext) {
        if (taskType != TaskType.XIULUO_V2
                || executionContext == null
                || "MEMBER".equalsIgnoreCase(executionContext.getWindowRole())) {
            return;
        }
        XiuluoGreenChainSchedule schedule = windowContext.getXiuluoGreenChainSchedule().orElse(null);
        if (schedule == null
                || windowContext.peekPreparedActionJob(PreparedActionJobType.SUMMON_SKILL_CLEANUP) != null) {
            return;
        }
        /*
         * CR253 review P1: no arbitrary republish backoff — whether to publish is decided ONLY by
         * "the current attempt has no pending job yet" plus the summon subsystem's own cooldown and
         * unknown-failure retry timing (both inside the read-only due probe). A new attempt that
         * discarded the previous job therefore republishes on the next tick instead of starving the
         * already-due leader maintenance.
         */
        if (!taskMaintenanceService.isSummonSkillCleanDueForCurrentWindow(executionContext)) {
            return;
        }
        long now = System.currentTimeMillis();
        PreparedActionJob job = PreparedActionJob.builder()
                .type(PreparedActionJobType.SUMMON_SKILL_CLEANUP)
                .windowId(schedule.getWindowId())
                .hwnd(schedule.getHwnd())
                .taskRunId(schedule.getTaskRunId())
                .round(schedule.getRound())
                .attemptId(schedule.getAttemptId())
                .reason("summon-skill-due")
                .source("xiuluo-v2:background-summon-due")
                .preparedAtMs(now)
                .build();
        if (windowContext.publishPreparedActionJob(job, "background-summon-skill-due")) {
            publishPreparedActionJobReady(taskType, job, "background-summon-skill-due");
        }
    }

    /**
     * Soft wake for a freshly published typed prepared job. Like every other ready event this is a
     * hint only: the parked consumer re-validates the job identity before any input.
     */
    private void publishPreparedActionJobReady(TaskType taskType, PreparedActionJob job, String reason) {
        windowReadyEventBus.publish(WindowReadyEvent.builder()
                .windowId(windowContext.getWindowId())
                .hwnd(windowContext.getNativeBinding().getNativeHandle())
                .type(WindowReadyEventType.PREPARED_ACTION_READY)
                .taskType(taskType)
                .source(reason + ":" + job.getType() + ":" + job.getSource())
                .createdAtMs(System.currentTimeMillis())
                .build());
    }

    private void settlePendingTransferChoiceMemory(WindowPathingIntent intent,
                                                   WindowPathingSnapshot snapshot,
                                                   WindowPathingState state,
                                                   TaskExecutionContext executionContext) {
        PendingTransferChoiceMemory pending = windowContext.getPendingTransferChoiceMemory();
        if (pending == null || pending.getTargetMap() == null || pending.getRelativeX() == null
                || pending.getRelativeY() == null) {
            return;
        }
        if (intent == null || !Objects.equals(pending.getTargetMap(), intent.getTargetMapName())) {
            PendingTransferChoiceMemory cleared = windowContext.consumePendingTransferChoiceMemory();
            if (cleared != null) {
                log.info("{} window [{}] discard pending route memory: source={} pendingTarget={} intentTarget={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), cleared.getSource(),
                        cleared.getTargetMap(), intent == null ? null : intent.getTargetMapName());
            }
            return;
        }
        if (state == WindowPathingState.ARRIVED) {
            PendingTransferChoiceMemory consumed = windowContext.consumePendingTransferChoiceMemory();
            if (consumed == null) {
                return;
            }
            memoryService.recordRouteDialogChoiceSuccess(
                    consumed.getFromMap(),
                    consumed.getFromX(),
                    consumed.getFromY(),
                    consumed.getTargetMap(),
                    consumed.getRelativeX(),
                    consumed.getRelativeY(),
                    consumed.getOptionText(),
                    consumed.getSource() + ":watcher-arrived");
            log.info("{} window [{}] confirm pending route memory: source={} target={} current={}({}, {}) ageMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), consumed.getSource(),
                    consumed.getTargetMap(), snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    Math.max(0L, System.currentTimeMillis() - consumed.getCreatedAtMs()));
        } else if (state == WindowPathingState.STOPPED_AWAY) {
            PendingTransferChoiceMemory consumed = windowContext.consumePendingTransferChoiceMemory();
            if (consumed == null) {
                return;
            }
            memoryService.recordRouteDialogChoiceFailure(
                    consumed.getFromMap(),
                    consumed.getTargetMap(),
                    consumed.getSource() + ":watcher-stopped-away");
            log.warn("{} window [{}] reject pending route memory: source={} target={} current={}({}, {}) ageMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), consumed.getSource(),
                    consumed.getTargetMap(), snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    Math.max(0L, System.currentTimeMillis() - consumed.getCreatedAtMs()));
        }
    }

    private void settlePendingRouteOutcome(WindowPathingIntent intent,
                                            WindowPathingSnapshot snapshot,
                                            WindowPathingState state,
                                            TaskExecutionContext executionContext) {
        PendingRouteOutcome pending = windowContext.getPendingRouteOutcome();
        if (pending == null || pending.getTargetMap() == null || pending.getRelativeX() == null
                || pending.getRelativeY() == null) {
            return;
        }
        String intentId = intent == null ? null : intent.getIntentId();
        if (pending.getIntentId() != null && !Objects.equals(pending.getIntentId(), intentId)) {
            PendingRouteOutcome consumed = windowContext.consumePendingRouteOutcome();
            if (consumed != null) {
                reportRouteOutcome(consumed, snapshot, RouteMemoryOutcomeReport.Result.ABANDONED, "intent-replaced");
                log.info("{} window [{}] abandon pending route outcome: routeMode={} source={} pendingIntentId={} currentIntentId={} target={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), consumed.getRouteMode(),
                        consumed.getSource(), consumed.getIntentId(), intentId, consumed.getTargetMap());
            }
            return;
        }
        String canonicalIntentTarget = intent == null ? null : mapNameCanonicalizer.canonicalize(
                intent.getTargetMapName(), "world-map-route-memory:settlement-intent");
        if (intent == null || !Objects.equals(pending.getTargetMap(), normalizeNullable(canonicalIntentTarget))) {
            PendingRouteOutcome consumed = windowContext.consumePendingRouteOutcome();
            if (consumed != null) {
                reportRouteOutcome(consumed, snapshot, RouteMemoryOutcomeReport.Result.ABANDONED, "target-replaced");
                log.info("{} window [{}] abandon pending route outcome: routeMode={} source={} pendingTarget={} intentTarget={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), consumed.getRouteMode(), consumed.getSource(),
                        consumed.getTargetMap(), intent == null ? null : intent.getTargetMapName());
            }
            return;
        }
        if (state == WindowPathingState.ARRIVED) {
            PendingRouteOutcome consumed = windowContext.consumePendingRouteOutcome();
            if (consumed == null) {
                return;
            }
            reportRouteOutcome(consumed, snapshot, RouteMemoryOutcomeReport.Result.SUCCESS,
                    "watcher-arrived");
            log.info("{} window [{}] confirm pending route outcome: routeMode={} source={} target={} current={}({}, {}) ageMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), consumed.getRouteMode(), consumed.getSource(),
                    consumed.getTargetMap(), snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    Math.max(0L, System.currentTimeMillis() - consumed.getCreatedAtMs()));
        } else if (state == WindowPathingState.STOPPED_AWAY) {
            PendingRouteOutcome consumed = windowContext.consumePendingRouteOutcome();
            if (consumed == null) {
                return;
            }
            reportRouteOutcome(consumed, snapshot, RouteMemoryOutcomeReport.Result.FAILURE,
                    "watcher-stopped-away");
            log.warn("{} window [{}] reject pending route outcome: routeMode={} source={} target={} current={}({}, {}) ageMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), consumed.getRouteMode(), consumed.getSource(),
                    consumed.getTargetMap(), snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    Math.max(0L, System.currentTimeMillis() - consumed.getCreatedAtMs()));
        }
    }

    /**
     * Runner-only settlement for local lifecycle boundaries that superseded a cloud route decision.
     * Runtime contexts queue these records but never issue HTTP themselves.
     */
    private void settleQueuedRouteOutcomeAbandonments(TaskExecutionContext executionContext) {
        WindowRuntimeContext.PendingRouteOutcomeAbandonment abandonment;
        while ((abandonment = windowContext.pollPendingRouteOutcomeAbandonment()) != null) {
            PendingRouteOutcome pending = abandonment.outcome();
            reportRouteOutcome(pending, null, RouteMemoryOutcomeReport.Result.ABANDONED,
                    normalizeNullable(abandonment.reason()) == null ? "runtime-abandoned" : abandonment.reason());
            log.info("{} window [{}] settle queued route outcome abandonment: intentId={} target={} reason={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                    pending == null ? null : pending.getIntentId(), pending == null ? null : pending.getTargetMap(),
                    abandonment.reason());
        }
    }

    /**
     * Runner-only replacement boundary. The prior cloud decision is reported as ABANDONED before
     * the next route outcome occupies the live runtime slot.
     */
    private void settleQueuedRouteOutcomeReplacements(TaskExecutionContext executionContext) {
        WindowRuntimeContext.PendingRouteOutcomeReplacement replacement;
        while ((replacement = windowContext.pollPendingRouteOutcomeReplacement()) != null) {
            PendingRouteOutcome previous = windowContext.getPendingRouteOutcome();
            if (previous != null) {
                reportRouteOutcome(previous, null, RouteMemoryOutcomeReport.Result.ABANDONED,
                        normalizeNullable(replacement.reason()) == null
                                ? "route-outcome-replaced"
                                : replacement.reason());
                windowContext.consumePendingRouteOutcome();
                log.info("{} window [{}] settle route outcome replacement: previousIntentId={} previousTarget={} reason={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(),
                        previous.getIntentId(), previous.getTargetMap(), replacement.reason());
            }
            windowContext.updatePendingRouteOutcome(replacement.outcome());
            log.info("{} window [{}] install replacement route outcome: intentId={} target={} reason={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(),
                    replacement.outcome().getIntentId(), replacement.outcome().getTargetMap(), replacement.reason());
        }
    }

    /**
     * A task may clear its pathing signal after consuming a terminal state. If it cleared before
     * the watcher could own settlement, the runner turns that unmatched live outcome into ABANDONED.
     */
    private void settleOrphanedRouteOutcome(WindowPathingIntent activeIntent,
                                             TaskExecutionContext executionContext) {
        PendingRouteOutcome pending = windowContext.getPendingRouteOutcome();
        if (pending == null || activeIntent != null) {
            return;
        }
        PendingRouteOutcome consumed = windowContext.consumePendingRouteOutcome();
        if (consumed == null) {
            return;
        }
        reportRouteOutcome(consumed, null, RouteMemoryOutcomeReport.Result.ABANDONED,
                "pathing-cleared-before-runner-settlement");
        log.info("{} window [{}] settle orphaned route outcome as abandoned: intentId={} target={}",
                executionContext.getLogPrefix(), windowContext.getWindowId(),
                consumed.getIntentId(), consumed.getTargetMap());
    }

    private void reportRouteOutcome(PendingRouteOutcome pending,
                                    WindowPathingSnapshot snapshot,
                                    RouteMemoryOutcomeReport.Result result,
                                    String reason) {
        if (pending == null || result == null) {
            return;
        }
        RouteMemoryOutcomeReport.Result effectiveResult = result;
        if (!hasText(pending.getRouteDecisionId())) {
            if (result == RouteMemoryOutcomeReport.Result.SUCCESS && !pending.isUsedMemory()) {
                effectiveResult = RouteMemoryOutcomeReport.Result.LEARN_CANDIDATE;
            } else {
                return;
            }
        }
        RouteMemoryOutcomeReport report = RouteMemoryOutcomeReport.builder()
                .routeDecisionId(pending.getRouteDecisionId())
                .intentId(pending.getIntentId())
                .fromMap(pending.getFromMap())
                .targetMap(pending.getTargetMap())
                .routeMode(pending.getRouteMode() == null ? null : pending.getRouteMode().name())
                .clickX(pending.getRelativeX())
                .clickY(pending.getRelativeY())
                .observedMap(snapshot == null ? null : snapshot.getCurrentMapName())
                .observedX(snapshot == null ? null : snapshot.getCurrentX())
                .observedY(snapshot == null ? null : snapshot.getCurrentY())
                .result(effectiveResult)
                .elapsedMs(Math.max(0L, System.currentTimeMillis() - pending.getCreatedAtMs()))
                .reason(reason)
                .source(pending.getSource())
                .build();
        submitRouteOutcomeReport(report, "settlement");
    }

    /**
     * Submit one terminal route outcome. Transport failures remain only in this runner's memory so
     * a watcher tick can retry them without rebuilding the deleted local route-memory store.
     */
    private void submitRouteOutcomeReport(RouteMemoryOutcomeReport report, String source) {
        submitRouteOutcomeReport(report, source, 0);
    }

    private void submitRouteOutcomeReport(RouteMemoryOutcomeReport report, String source, int priorAttempts) {
        if (report == null) {
            return;
        }
        String fallbackKey = routeOutcomeIdempotencyKey(report);
        RouteMemoryOutcomeIngestResult ingest;
        try {
            ingest = routeCloudDecisionService.reportRouteMemoryOutcome(report);
        } catch (RuntimeException e) {
            enqueueRouteOutcomeDelivery(fallbackKey, report, priorAttempts,
                    "client exception: " + e.getClass().getSimpleName());
            log.warn("cloud route-memory outcome delivery failed: windowId={} key={} source={} reason={}",
                    windowContext.getWindowId(), fallbackKey, source, e.toString());
            return;
        }
        String key = ingest == null || !hasText(ingest.getIdempotencyKey())
                ? fallbackKey
                : ingest.getIdempotencyKey();
        RouteMemoryOutcomeIngestResult.Status status = ingest == null ? null : ingest.getStatus();
        if (status == RouteMemoryOutcomeIngestResult.Status.SUBMITTED
                || status == RouteMemoryOutcomeIngestResult.Status.DUPLICATE_SKIPPED) {
            pendingRouteOutcomeDeliveries.remove(key);
        } else if (status == RouteMemoryOutcomeIngestResult.Status.FAILED || status == null) {
            enqueueRouteOutcomeDelivery(key, report, priorAttempts,
                    ingest == null ? "missing ingest result" : ingest.getReason());
        } else {
            // SKIPPED covers missing decision/configuration and must never become an infinite retry.
            pendingRouteOutcomeDeliveries.remove(key);
        }
        log.info("cloud route-memory outcome report: windowId={} routeDecisionId={} intentId={} fromMap={} targetMap={} routeMode={} result={} ingestStatus={} reason={} source={}",
                windowContext.getWindowId(), report.getRouteDecisionId(), report.getIntentId(), report.getFromMap(),
                report.getTargetMap(), report.getRouteMode(), report.getResult(), status,
                ingest == null ? "missing ingest result" : ingest.getReason(), source);
    }

    private void enqueueRouteOutcomeDelivery(String key,
                                             RouteMemoryOutcomeReport report,
                                             int priorAttempts,
                                             String reason) {
        long now = System.currentTimeMillis();
        pendingRouteOutcomeDeliveries.compute(key, (ignored, existing) -> {
            int attempts = Math.max(priorAttempts + 1, existing == null ? 1 : existing.attempts() + 1);
            return new PendingRouteOutcomeDelivery(report, attempts,
                    now + routeOutcomeRetryDelayMs(attempts), reason);
        });
        PendingRouteOutcomeDelivery queued = pendingRouteOutcomeDeliveries.get(key);
        log.warn("cloud route-memory outcome queued for in-memory retry: windowId={} key={} attempts={} nextRetryMs={} reason={}",
                windowContext.getWindowId(), key, queued == null ? null : queued.attempts(),
                queued == null ? null : queued.nextRetryAtMs(), reason);
    }

    private void retryPendingRouteOutcomeDeliveries(boolean force, String source) {
        long now = System.currentTimeMillis();
        pendingRouteOutcomeDeliveries.forEach((key, delivery) -> {
            if (!force && delivery.nextRetryAtMs() > now) {
                return;
            }
            if (pendingRouteOutcomeDeliveries.remove(key, delivery)) {
                submitRouteOutcomeReport(delivery.report(), source + ":retry-" + delivery.attempts(), delivery.attempts());
            }
        });
    }

    private long routeOutcomeRetryDelayMs(int attempts) {
        long delayMs = ROUTE_OUTCOME_RETRY_BASE_DELAY_MS;
        for (int attempt = 1; attempt < attempts && delayMs < ROUTE_OUTCOME_RETRY_MAX_DELAY_MS; attempt++) {
            delayMs = Math.min(ROUTE_OUTCOME_RETRY_MAX_DELAY_MS, delayMs * 2L);
        }
        return delayMs;
    }

    private String routeOutcomeIdempotencyKey(RouteMemoryOutcomeReport report) {
        return routeOutcomeIdempotencyPart(report.getRouteDecisionId())
                + "|" + routeOutcomeIdempotencyPart(report.getIntentId())
                + "|" + routeOutcomeIdempotencyPart(report.getFromMap())
                + "|" + routeOutcomeIdempotencyPart(report.getTargetMap())
                + "|" + routeOutcomeIdempotencyPart(report.getRouteMode())
                + "|" + routeOutcomeIdempotencyPart(report.getResult() == null ? null : report.getResult().name());
    }

    private String routeOutcomeIdempotencyPart(String value) {
        return value == null ? "" : value;
    }

    private WindowPathingSnapshot updateUnknownPathing(TaskType taskType,
                                                       TaskExecutionContext executionContext,
                                                       WindowPathingIntent intent,
                                                       WindowPathingSnapshot previous,
                                                       long startedAt,
                                                       String message,
                                                       boolean pausedReadOnly) {
        long now = System.currentTimeMillis();
        if (!isCurrentPathingIntent(intent)) {
            log.info("{} window [{}] discard stale pathing unknown result: task={} source={} target={}({}, {}) reason={} probeMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    intent.getSource(), intent.getTargetMapName(), intent.getTargetX(), intent.getTargetY(),
                    message, Math.max(0L, now - startedAt));
            return windowContext.getPathingSnapshot();
        }
        long previousUpdatedAtMs = previous == null ? 0L : previous.getUpdatedAtMs();
        PathingDialogBlock dialogBlock = resolvePathingDialogBlock(now);
        WindowPathingState state = classifyUnknownPathingState(intent, dialogBlock);
        WindowPathingSnapshot snapshot = WindowPathingSnapshot.builder()
                .state(state)
                .intent(intent)
                .currentMapName(previous == null ? null : previous.getCurrentMapName())
                .currentX(previous == null ? null : previous.getCurrentX())
                .currentY(previous == null ? null : previous.getCurrentY())
                .message(pathingMessageForState(state, message, dialogBlock))
                .locationChangedAtMs(previous == null ? 0L : previous.getLocationChangedAtMs())
                .movementObservedAtMs(previous == null ? 0L : previous.getMovementObservedAtMs())
                .updatedAtMs(previousUpdatedAtMs)
                .probeStartedAtMs(startedAt)
                .probeFinishedAtMs(now)
                .probeInProgress(false)
                .uiCleanupRecommended(previous != null && previous.isUiCleanupRecommended())
                .uiCleanupReason(previous == null ? null : previous.getUiCleanupReason())
                .uiCleanupRecommendedAtMs(previous == null ? 0L : previous.getUiCleanupRecommendedAtMs())
                .dialogBlocking(dialogBlock.blocking())
                .dialogBlockingReason(dialogBlock.reason())
                .dialogBlockingType(dialogBlock.dialogType())
                .dialogBlockingDetectedAtMs(dialogBlock.detectedAtMs())
                .dialogPreparationPhase(dialogBlock.preparationPhase())
                .dialogPreparationOperation(dialogBlock.operation())
                .dialogPreparationTarget(dialogBlock.targetKeyword())
                .build();
        windowContext.updatePathingSnapshot(snapshot);
        boolean stateChanged = previous == null || state != previous.getState();
        if (!pausedReadOnly) {
            settlePendingRouteOutcome(intent, snapshot, state, executionContext);
        }
        publishPathingTerminalEventIfNeeded(taskType, intent, snapshot, state, stateChanged);
        if (!pausedReadOnly) {
            maybeSubmitXiuluoGreenStopStaticArbitration(taskType, executionContext, intent, state, stateChanged);
        }
        long probeMs = Math.max(0L, now - startedAt);
        if (state == WindowPathingState.STOPPED_AWAY || probeMs >= WINDOW_PATHING_SLOW_PROBE_LOG_MS) {
            log.info("{} window [{}] pathing watcher unknown probe: task={} state={} source={} reason={} probeMs={} dialogBlocking={} dialogAttentionOnly={} dialogReason={} dialogType={} dialogPhase={} dialogOperation={} dialogTarget={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, state,
                    intent.getSource(), message, probeMs, dialogBlock.blocking(), dialogBlock.blocking(),
                    dialogBlock.reason(), dialogBlock.dialogType(), dialogBlock.preparationPhase(),
                    dialogBlock.operation(), dialogBlock.targetKeyword());
        } else {
            log.debug("{} window [{}] pathing watcher unknown: task={} source={} reason={} probeMs={}",
                executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, intent.getSource(),
                    message, probeMs);
        }
        return snapshot;
    }

    private WindowPathingState classifyPathingState(WindowPathingIntent intent,
                                                    WindowPathingSnapshot previous,
                                                    TemplateLocationInfo location,
                                                    boolean locationChanged,
                                                    long locationChangedAtMs,
                                                    long now,
                                                    PathingDialogBlock dialogBlock) {
        MapCoordinate coordinate = location.coordinate();
        if (intent.getType() != WindowPathingIntentType.UNTARGETED_TRACKER
                && hasArrived(intent, location.mapName(), coordinate)) {
            /*
             * CR142: a coordinate movement intent enters the tolerance box before the character
             * fully stops. Only publish ARRIVED immediately when there is no observed movement for
             * this intent; otherwise require a short stationary window so downstream NPC/dialog
             * clicks do not start while the role is still sliding into place.
             */
            if (intent.getTargetX() != null
                    && intent.getTargetY() != null
                    && previous != null
                    && locationChanged) {
                return WindowPathingState.ACTIVE;
            }
            if (intent.getTargetX() != null
                    && intent.getTargetY() != null
                    && previous != null
                    && locationChangedAtMs > 0L
                    && now - locationChangedAtMs < WINDOW_PATHING_ARRIVAL_STATIONARY_MS) {
                return WindowPathingState.ACTIVE;
            }
            return WindowPathingState.ARRIVED;
        }
        if (locationChanged) {
            return WindowPathingState.ACTIVE;
        }
        long stoppedAwayMs = resolvePathingStoppedAwayMs(intent, location.mapName());
        if (locationChangedAtMs > 0L
                && now - locationChangedAtMs >= stoppedAwayMs
                && now - intent.getCreatedAtMs() >= stoppedAwayMs) {
            return WindowPathingState.STOPPED_AWAY;
        }
        return WindowPathingState.ACTIVE;
    }

    private WindowPathingState classifyUnknownPathingState(WindowPathingIntent intent,
                                                           PathingDialogBlock dialogBlock) {
        return WindowPathingState.UNKNOWN;
    }

    private String pathingMessageForState(WindowPathingState state, String fallback, PathingDialogBlock dialogBlock) {
        return fallback;
    }

    private void logUntargetedDialogAttentionEvidence(TaskType taskType,
                                                      TaskExecutionContext executionContext,
                                                      WindowPathingIntent intent,
                                                      boolean locationChanged,
                                                      boolean stateChanged,
                                                      long probeMs,
                                                      PathingDialogBlock dialogBlock) {
        if (intent == null
                || intent.getType() != WindowPathingIntentType.UNTARGETED_TRACKER
                || dialogBlock == null
                || !dialogBlock.blocking()) {
            return;
        }
        if (!locationChanged && !stateChanged && probeMs < WINDOW_PATHING_SLOW_PROBE_LOG_MS) {
            return;
        }
        log.info("{} window [{}] pathing watcher kept dialog evidence as attention-only: task={} intentType={} source={} locationChanged={} reason={} dialogType={} phase={} operation={} target={} detectedAgeMs={}",
                executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                intent.getType(), intent.getSource(), locationChanged, dialogBlock.reason(),
                dialogBlock.dialogType(), dialogBlock.preparationPhase(), dialogBlock.operation(),
                dialogBlock.targetKeyword(), ageMs(System.currentTimeMillis(), dialogBlock.detectedAtMs()));
    }

    private PathingDialogBlock resolvePathingDialogBlock(long now) {
        PreparedDialogAction preparedAction = windowContext.getPreparedDialogAction();
        if (preparedAction != null) {
            long detectedAtMs = preparedAction.getLastVerifiedAtMs() > 0L
                    ? preparedAction.getLastVerifiedAtMs()
                    : preparedAction.getPreparedAtMs();
            return PathingDialogBlock.attention(
                    "prepared-dialog",
                    preparedAction.getDialogType(),
                    detectedAtMs,
                    DialogPreparationPhase.READY,
                    preparedAction.getOperation(),
                    preparedAction.getTargetKeyword());
        }

        DialogPreparationStatus preparationStatus = windowContext.getDialogPreparationStatus();
        if (preparationStatus != null && isAttentionPreparationPhase(preparationStatus.getPhase())) {
            long detectedAtMs = preparationStatus.getCompletedAtMs() > 0L
                    ? preparationStatus.getCompletedAtMs()
                    : preparationStatus.getPreparingStartedAtMs() > 0L
                    ? preparationStatus.getPreparingStartedAtMs()
                    : preparationStatus.getRequestCreatedAtMs();
            String reason = "dialog-preparation-" + preparationStatus.getPhase();
            return PathingDialogBlock.attention(
                    reason,
                    null,
                    detectedAtMs,
                    preparationStatus.getPhase(),
                    preparationStatus.getOperation(),
                    preparationStatus.getTargetKeyword());
        }

        Optional<WindowDialogSnapshot> visible =
                windowContext.getVisibleDialogSnapshot(WINDOW_DIALOG_VISIBLE_MAX_AGE_MS);
        if (visible.isPresent() && visible.get().getType() != DialogType.NONE) {
            WindowDialogSnapshot snapshot = visible.get();
            return PathingDialogBlock.attention(
                    "visible-dialog-" + snapshot.getType(),
                    snapshot.getType(),
                    snapshot.getDetectedAtMs(),
                    DialogPreparationPhase.NONE,
                    null,
                    null);
        }
        return PathingDialogBlock.none(now);
    }

    private boolean isAttentionPreparationPhase(DialogPreparationPhase phase) {
        return phase == DialogPreparationPhase.REQUESTED
                || phase == DialogPreparationPhase.PREPARING
                || phase == DialogPreparationPhase.READY;
    }

    private long resolvePathingStoppedAwayMs(WindowPathingIntent intent, String currentMapName) {
        if (intent.getType() == WindowPathingIntentType.UNTARGETED_TRACKER) {
            return WINDOW_PATHING_SHORTCUT_STOPPED_AWAY_MS;
        }
        /*
         * CR122: production movement semantics are tracker/shortcut pathing and mini-map coordinate
         * handoff. Old map-route/cross-map buckets are retained only as historical source text, not
         * as live stopped-away policy; default targeted pathing uses the handoff threshold.
         */
        return WINDOW_PATHING_MINI_MAP_HANDOFF_STOPPED_AWAY_MS;
    }

    private boolean isCurrentPathingIntent(WindowPathingIntent intent) {
        return windowContext.getActivePathingIntent()
                .map(current -> isSamePathingIntent(current, intent))
                .orElse(false);
    }

    private boolean isSamePathingIntent(WindowPathingIntent left, WindowPathingIntent right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getSource(), right.getSource())
                && Objects.equals(left.getIntentId(), right.getIntentId())
                && left.getType() == right.getType()
                && Objects.equals(left.getTargetMapName(), right.getTargetMapName())
                && Objects.equals(left.getTargetX(), right.getTargetX())
                && Objects.equals(left.getTargetY(), right.getTargetY())
                && left.getTolerance() == right.getTolerance()
                && left.getCreatedAtMs() == right.getCreatedAtMs();
    }

    private boolean hasArrived(WindowPathingIntent intent, String currentMapName, MapCoordinate coordinate) {
        if (intent.getTargetMapName() != null
                && currentMapName != null
                && !intent.getTargetMapName().equals(currentMapName)) {
            return false;
        }
        if (intent.getTargetX() == null || intent.getTargetY() == null) {
            return intent.getTargetMapName() == null || intent.getTargetMapName().equals(currentMapName);
        }
        if (coordinate == null) {
            return false;
        }
        int tolerance = Math.max(0, intent.getTolerance());
        return Math.abs(coordinate.getX() - intent.getTargetX()) <= tolerance
                && Math.abs(coordinate.getY() - intent.getTargetY()) <= tolerance;
    }

    private boolean hasLocationChanged(WindowPathingSnapshot previous, String currentMapName, MapCoordinate coordinate) {
        if (previous == null || coordinate == null) {
            return false;
        }
        if (!Objects.equals(previous.getCurrentMapName(), currentMapName)) {
            return true;
        }
        return previous.getCurrentX() == null
                || previous.getCurrentY() == null
                || previous.getCurrentX() != coordinate.getX()
                || previous.getCurrentY() != coordinate.getY();
    }

    /**
     * Run live role detection and possibly reassign a requested task before it starts.
     */
    private TaskType resolveTaskTypeBeforeStart(TaskType requestedTaskType,
                                                TaskExecutionContext preflightContext) {
        if (!taskTeamAssignmentPolicy.shouldDetectRoleBeforeStart(requestedTaskType)) {
            return requestedTaskType;
        }
        TaskCheckpoint.throwIfStopRequested(preflightContext, "task queue stopped before team role detection");
        // A visible option dialog suppresses the hover tooltip used by live role detection. Reuse the
        // existing cleanup policy before probing; it handles option dialogs and leaves story handling
        // to its established safety rules.
        uiCleanerService.forceCloseDialog();
        TaskCheckpoint.throwIfStopRequested(preflightContext, "task queue stopped during preflight dialog cleanup");
        TeamRoleDetectionService.TeamRoleDetectionResult liveDetection =
                teamRoleDetectionService.detectCurrentRoleWithEvidence(preflightContext);
        TeamRoleStatus liveRole = liveDetection.role();
        TaskCheckpoint.throwIfStopRequested(preflightContext, "task queue stopped during team role detection");
        TeamRoleStatus assignmentRole = liveRole;
        if ((assignmentRole == null || assignmentRole.isUnknown()) && windowContext.getRole() != null) {
            TeamRoleStatus contextRole = toTeamRoleStatus(windowContext.getRole());
            if (!contextRole.isUnknown()) {
                assignmentRole = contextRole;
                log.info("{} window [{}] use existing window role for assignment after live role unknown: requested={} liveRole={} assignmentRole={}",
                        preflightContext.getLogPrefix(), windowContext.getWindowId(),
                        requestedTaskType, liveRole, assignmentRole);
            }
        }
        syncWindowRole(liveRole);
        TeamRoleDetectionService.TeamTooltipGroupEvidence tooltipEvidence =
                liveDetection.tooltipGroupEvidence();
        if (tooltipEvidence != null
                && liveRole != null
                && liveRole.isMember()
                && !preflightContext.hasLocalTeamSession()) {
            TaskMaintenanceService.LocalTeamSessionAttachResult attachResult =
                    taskMaintenanceService.attachExistingLocalTeamSessionForMember(
                            windowContext.getWindowId(),
                            tooltipEvidence.currentPlayerId(),
                            tooltipEvidence.groupHash(),
                            tooltipEvidence.leaderPlayerId(),
                            liveRole.name(),
                            "runner-role-preflight:late-member");
            if (attachResult.status() == TaskMaintenanceService.LocalTeamSessionAttachStatus.ATTACHED) {
                activeLocalTeamSessionKey = attachResult.sessionKey();
                activeLocalLeaderWindowId = attachResult.leaderWindowId();
                activeLocalLeaderPresent = true;
                preflightContext = buildExecutionContext(
                        requestedTaskType,
                        preflightContext.getStopToken(),
                        preflightContext.getPauseToken(),
                        preflightContext.getStartupMode());
                log.info("{} window [{}] late member attached to existing local-team session: requested={} session={} leaderWindow={} leaderPlayerId={} currentPlayerId={} groupHash={}",
                        preflightContext.getLogPrefix(), windowContext.getWindowId(), requestedTaskType,
                        attachResult.sessionKey(), attachResult.leaderWindowId(),
                        tooltipEvidence.leaderPlayerId(), tooltipEvidence.currentPlayerId(),
                        tooltipEvidence.groupHash());
            } else if (attachResult.status() == TaskMaintenanceService.LocalTeamSessionAttachStatus.AMBIGUOUS_MATCH) {
                throw new IllegalStateException("late member local-team session ambiguous: leaderPlayerId="
                        + tooltipEvidence.leaderPlayerId() + ", windowId=" + windowContext.getWindowId());
            } else {
                log.warn("{} window [{}] late member has no existing local-team session attachment: requested={} status={} leaderPlayerId={} currentPlayerId={} groupHash={}; continue as standalone auto-battle only after explicit warning",
                        preflightContext.getLogPrefix(), windowContext.getWindowId(), requestedTaskType,
                        attachResult.status(), tooltipEvidence.leaderPlayerId(),
                        tooltipEvidence.currentPlayerId(), tooltipEvidence.groupHash());
            }
        }
        if (tooltipEvidence != null) {
            taskMaintenanceService.recordLocalTeamTooltipGroup(
                    preflightContext,
                    windowContext.getWindowId(),
                    tooltipEvidence.currentPlayerId(),
                    tooltipEvidence.groupHash(),
                    tooltipEvidence.leaderPlayerId(),
                    liveRole == null ? null : liveRole.name(),
                    "runner-role-preflight");
        }
        taskMaintenanceService.markLocalTeamWindowRoleDetected(
                preflightContext,
                windowContext.getWindowId(),
                liveRole == null ? null : liveRole.name(),
                "runner-role-preflight");
        TaskType resolvedTaskType = taskTeamAssignmentPolicy.resolveTaskForRole(requestedTaskType, assignmentRole);
        if (resolvedTaskType != requestedTaskType) {
            log.info("{} window [{}] task reassigned by team role: requested={} liveRole={} assignmentRole={} resolved={}",
                    preflightContext.getLogPrefix(), windowContext.getWindowId(),
                    requestedTaskType, liveRole, assignmentRole, resolvedTaskType);
        } else {
            log.info("{} window [{}] task kept by team role: requested={} liveRole={} assignmentRole={}",
                    preflightContext.getLogPrefix(), windowContext.getWindowId(),
                    requestedTaskType, liveRole, assignmentRole);
        }
        TaskCheckpoint.throwIfStopRequested(preflightContext, "task queue stopped after team role assignment");
        return resolvedTaskType;
    }

    private TeamRoleStatus toTeamRoleStatus(WindowRole role) {
        if (role == null) {
            return TeamRoleStatus.UNKNOWN;
        }
        return switch (role) {
            case LEADER -> TeamRoleStatus.LEADER;
            case MEMBER -> TeamRoleStatus.MEMBER;
            case UNKNOWN -> TeamRoleStatus.UNKNOWN;
        };
    }

    private void syncWindowRole(TeamRoleStatus role) {
        if (role == null) {
            return;
        }
        if (role.isLeader()) {
            windowContext.setRole(WindowRole.LEADER);
        } else if (role.isMember()) {
            windowContext.setRole(WindowRole.MEMBER);
        }
    }

    /**
     * Build the per-task execution context with native binding metadata captured from this window.
     */
    private TaskExecutionContext buildExecutionContext(TaskType requestedTaskType,
                                                       GameTask task,
                                                       TaskStopToken stopToken,
                                                       TaskPauseToken pauseToken) {
        return buildExecutionContext(requestedTaskType, task, stopToken, pauseToken, TaskStartupMode.NORMAL);
    }

    private TaskExecutionContext buildExecutionContext(TaskType requestedTaskType,
                                                       GameTask task,
                                                       TaskStopToken stopToken,
                                                       TaskPauseToken pauseToken,
                                                       TaskStartupMode startupMode) {
        return buildExecutionContext(requestedTaskType, task.getTaskCode(), task.getTaskName(),
                stopToken, pauseToken, startupMode);
    }

    private TaskExecutionContext buildExecutionContext(TaskType taskType, TaskStopToken stopToken, TaskPauseToken pauseToken) {
        return buildExecutionContext(taskType, stopToken, pauseToken, TaskStartupMode.NORMAL);
    }

    private TaskExecutionContext buildExecutionContext(TaskType taskType,
                                                       TaskStopToken stopToken,
                                                       TaskPauseToken pauseToken,
                                                       TaskStartupMode startupMode) {
        TaskType safeTaskType = taskType == null ? TaskType.UNKNOWN : taskType;
        return buildExecutionContext(safeTaskType, safeTaskType.getCode(), safeTaskType.getDisplayName(),
                stopToken, pauseToken, startupMode);
    }

    private TaskExecutionContext buildExecutionContext(TaskType requestedTaskType,
                                                       String taskCode,
                                                       String taskName,
                                                       TaskStopToken stopToken,
                                                       TaskPauseToken pauseToken,
                                                       TaskStartupMode startupMode) {
        WindowNativeBinding binding = windowContext.getNativeBinding();
        TaskType safeRequestedTaskType = requestedTaskType == null ? TaskType.UNKNOWN : requestedTaskType;
        return TaskExecutionContext.builder()
                .taskCode(taskCode)
                .taskName(taskName)
                .requestedTaskCode(safeRequestedTaskType.getCode())
                .requestedTaskName(safeRequestedTaskType.getDisplayName())
                .windowId(windowContext.getWindowId())
                .windowRole(windowContext.getRole().name())
                .nativeWindowHandle(binding.getNativeHandle())
                .nativeWindowTitle(binding.getTitle())
                .nativeWindowClassName(binding.getClassName())
                .nativeWindowProcessId(binding.getProcessId())
                .nativeWindowX(binding.getX())
                .nativeWindowY(binding.getY())
                .nativeWindowWidth(binding.getWidth())
                .nativeWindowHeight(binding.getHeight())
                .localTeamSessionKey(activeLocalTeamSessionKey)
                .localLeaderWindowId(resolveLocalLeaderWindowId())
                .localLeaderPresent(activeLocalLeaderPresent)
                .localSupportMember(activeLocalLeaderPresent && windowContext.getRole().isMember())
                .stopToken(stopToken)
                .pauseToken(pauseToken)
                .windowRuntimeContext(windowContext)
                .taskRunId(GLOBAL_TASK_RUN_SEQUENCE.incrementAndGet())
                .startupMode(startupMode == null ? TaskStartupMode.NORMAL : startupMode)
                .startedAt(LocalDateTime.now())
                .build();
    }

    private String resolveLocalLeaderWindowId() {
        if (activeLocalLeaderWindowId != null && !activeLocalLeaderWindowId.isBlank()) {
            return activeLocalLeaderWindowId;
        }
        if (activeLocalLeaderPresent && windowContext.getRole().isLeader()) {
            return windowContext.getWindowId();
        }
        return activeLocalLeaderWindowId;
    }

    private String normalizeSessionText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private WindowRuntimeStatus toWindowStatus(TaskRunResult result) {
        if (result == null) {
            return WindowRuntimeStatus.ERROR;
        }
        return switch (result) {
            case SUCCESS -> WindowRuntimeStatus.IDLE;
            case FAILED -> WindowRuntimeStatus.ERROR;
            case STOPPED -> WindowRuntimeStatus.STOPPED;
            case SKIPPED -> WindowRuntimeStatus.IDLE;
        };
    }

    private TaskRunResult mergeQueueResult(TaskRunResult current, TaskRunResult next) {
        if (current == TaskRunResult.STOPPED || next == TaskRunResult.STOPPED) {
            return TaskRunResult.STOPPED;
        }
        if (current == TaskRunResult.FAILED || next == TaskRunResult.FAILED) {
            return TaskRunResult.FAILED;
        }
        if (current == TaskRunResult.SUCCESS || next == TaskRunResult.SUCCESS) {
            return TaskRunResult.SUCCESS;
        }
        return TaskRunResult.SKIPPED;
    }

    private boolean shouldStopQueueAfterFailure(WindowTaskFailurePolicy failurePolicy) {
        return failurePolicy == WindowTaskFailurePolicy.STOP_ON_FAILURE;
    }

    private String normalizeMessage(String message) {
        return message == null || message.isBlank() ? "-" : message;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static long ageMs(long nowMs, long timestampMs) {
        return timestampMs <= 0L ? -1L : Math.max(0L, nowMs - timestampMs);
    }

    private record PostCombatIdleDecision(boolean publish,
                                          long lastCombatExitAtMs,
                                          long elapsedMs,
                                          String summary) {

        private static PostCombatIdleDecision no() {
            return new PostCombatIdleDecision(false, 0L, 0L, null);
        }
    }

    /** Runtime-only cloud terminal delivery state. It is intentionally discarded on process exit. */
    private record PendingRouteOutcomeDelivery(RouteMemoryOutcomeReport report,
                                               int attempts,
                                               long nextRetryAtMs,
                                               String lastFailureReason) {
    }

    private static class PostCombatIdleTracker {
        private long lastCombatExitAtMs;
        private long idleStartedAtMs;
        private long pausedAccumulatedMs;
        private long pauseStartedAtMs;
        private long lastLocationChangedAtMs;
        private String lastLocationSignature;
        private boolean timeoutPublished;

        private void onCombatTick(AutoCombatService.TickResult oldTick,
                                  AutoCombatService.TickResult newTick,
                                  long nowMs) {
            if (newTick == AutoCombatService.TickResult.IN_COMBAT) {
                reset();
                return;
            }
            if (oldTick == AutoCombatService.TickResult.IN_COMBAT
                    && newTick != AutoCombatService.TickResult.IN_COMBAT) {
                lastCombatExitAtMs = nowMs;
                idleStartedAtMs = nowMs;
                pausedAccumulatedMs = 0L;
                pauseStartedAtMs = 0L;
                lastLocationChangedAtMs = nowMs;
                lastLocationSignature = null;
                timeoutPublished = false;
            }
        }

        private void onPauseTick(long nowMs) {
            if (lastCombatExitAtMs <= 0L || pauseStartedAtMs > 0L) {
                return;
            }
            pauseStartedAtMs = nowMs;
        }

        private PostCombatIdleDecision evaluate(long nowMs,
                                                long timeoutMs,
                                                WindowPathingSnapshot pathingSnapshot,
                                                PreparedDialogAction preparedAction,
                                                DialogPreparationStatus dialogStatus,
                                                WindowDialogSnapshot visibleDialog) {
            if (lastCombatExitAtMs <= 0L) {
                return PostCombatIdleDecision.no();
            }
            resumePauseIfNeeded(nowMs);
            String locationSignature = locationSignature(pathingSnapshot);
            boolean locationChanged = lastLocationSignature == null
                    || !Objects.equals(lastLocationSignature, locationSignature)
                    || (pathingSnapshot != null
                            && pathingSnapshot.getLocationChangedAtMs() > lastLocationChangedAtMs);
            if (locationChanged) {
                lastLocationSignature = locationSignature;
                lastLocationChangedAtMs = pathingSnapshot == null
                        ? nowMs
                        : Math.max(nowMs, pathingSnapshot.getLocationChangedAtMs());
                resetIdleTimer(nowMs);
                return PostCombatIdleDecision.no();
            }

            boolean activePathing = isActivePathing(pathingSnapshot);
            boolean freshPreparedAction = isFreshPreparedAction(nowMs, preparedAction);
            boolean dialogProgress = isDialogProgress(nowMs, dialogStatus, visibleDialog);
            if (activePathing || freshPreparedAction || dialogProgress) {
                resetIdleTimer(nowMs);
                return PostCombatIdleDecision.no();
            }

            long elapsedMs = Math.max(0L, nowMs - idleStartedAtMs - pausedAccumulatedMs);
            if (elapsedMs < timeoutMs || timeoutPublished) {
                return PostCombatIdleDecision.no();
            }
            timeoutPublished = true;
            String summary = "location=" + locationSignature
                    + ", stationaryMs=" + elapsedMs
                    + ", lastLocationChangedAtMs=" + lastLocationChangedAtMs
                    + ", pathingState=" + (pathingSnapshot == null ? null : pathingSnapshot.getState())
                    + ", activePathing=" + activePathing
                    + ", freshPreparedAction=" + freshPreparedAction
                    + ", dialogProgress=" + dialogProgress
                    + ", pausedAccumulatedMs=" + pausedAccumulatedMs;
            return new PostCombatIdleDecision(true, lastCombatExitAtMs, elapsedMs, summary);
        }

        private void resetIdleTimer(long nowMs) {
            idleStartedAtMs = nowMs;
            pausedAccumulatedMs = 0L;
            pauseStartedAtMs = 0L;
            timeoutPublished = false;
        }

        private void resumePauseIfNeeded(long nowMs) {
            if (pauseStartedAtMs <= 0L) {
                return;
            }
            pausedAccumulatedMs += Math.max(0L, nowMs - pauseStartedAtMs);
            pauseStartedAtMs = 0L;
        }

        private boolean isActivePathing(WindowPathingSnapshot snapshot) {
            if (snapshot == null) {
                return false;
            }
            return snapshot.isProbeInProgress()
                    || snapshot.getState() == WindowPathingState.ACTIVE
                    || snapshot.getState() == WindowPathingState.UNKNOWN;
        }

        private boolean isFreshPreparedAction(long nowMs, PreparedDialogAction action) {
            if (action == null) {
                return false;
            }
            long lastVerifiedAtMs = action.getLastVerifiedAtMs() > 0L
                    ? action.getLastVerifiedAtMs()
                    : action.getPreparedAtMs();
            return lastVerifiedAtMs > 0L
                    && nowMs - lastVerifiedAtMs <= WINDOW_DIALOG_VISIBLE_MAX_AGE_MS;
        }

        private boolean isDialogProgress(long nowMs,
                                         DialogPreparationStatus status,
                                         WindowDialogSnapshot visibleDialog) {
            if (status != null
                    && (status.getPhase() == DialogPreparationPhase.REQUESTED
                            || status.getPhase() == DialogPreparationPhase.PREPARING
                            || status.getPhase() == DialogPreparationPhase.READY)) {
                return true;
            }
            return visibleDialog != null
                    && visibleDialog.getType() != null
                    && visibleDialog.getType() != DialogType.NONE
                    && visibleDialog.getDetectedAtMs() > 0L
                    && nowMs - visibleDialog.getDetectedAtMs() <= WINDOW_DIALOG_VISIBLE_MAX_AGE_MS;
        }

        private String locationSignature(WindowPathingSnapshot snapshot) {
            if (snapshot == null) {
                return "no-pathing-snapshot";
            }
            return normalizeSignaturePart(snapshot.getCurrentMapName())
                    + ":" + snapshot.getCurrentX()
                    + "," + snapshot.getCurrentY();
        }

        private String normalizeSignaturePart(String value) {
            return value == null || value.isBlank() ? "unknown-map" : value.trim();
        }

        private void reset() {
            lastCombatExitAtMs = 0L;
            idleStartedAtMs = 0L;
            pausedAccumulatedMs = 0L;
            pauseStartedAtMs = 0L;
            lastLocationChangedAtMs = 0L;
            lastLocationSignature = null;
            timeoutPublished = false;
        }
    }

    private record PathingDialogBlock(boolean blocking,
                                      String reason,
                                      DialogType dialogType,
                                      long detectedAtMs,
                                      DialogPreparationPhase preparationPhase,
                                      DialogOperation operation,
                                      String targetKeyword) {

        private static PathingDialogBlock attention(String reason,
                                                    DialogType dialogType,
                                                    long detectedAtMs,
                                                    DialogPreparationPhase preparationPhase,
                                                    DialogOperation operation,
                                                    String targetKeyword) {
            return new PathingDialogBlock(true, reason, dialogType, detectedAtMs,
                    preparationPhase, operation, targetKeyword);
        }

        private static PathingDialogBlock none(long now) {
            return new PathingDialogBlock(false, null, DialogType.NONE, now,
                    DialogPreparationPhase.NONE, null, null);
        }
    }

    private class TickDialogProbe {
        private final TaskType taskType;
        private final TaskExecutionContext executionContext;
        private DialogDetection detection;
        private String source;

        private TickDialogProbe(TaskType taskType, TaskExecutionContext executionContext) {
            this.taskType = taskType;
            this.executionContext = executionContext;
        }

        private DialogDetection detect(String source, String consumer) {
            if (detection == null) {
                detection = dialogService.detectDialogSnapshotNoFocus(source, false, 0);
                this.source = source;
                log.info("{} window [{}] tick dialog detection captured: task={} title={} consumer={} source={} type={} hasImage={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, consumer,
                        windowContext.getNativeBinding() == null ? null : windowContext.getNativeBinding().getTitle(),
                        source, detection == null ? null : detection.type(),
                        detection != null && detection.image() != null);
            } else {
                log.info("{} window [{}] tick dialog detection reused: task={} title={} consumer={} originalSource={} requestedSource={} type={} hasImage={}",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, consumer,
                        windowContext.getNativeBinding() == null ? null : windowContext.getNativeBinding().getTitle(),
                        this.source, source, detection.type(), detection.image() != null);
            }
            return detection;
        }

        private DialogDetection currentDetection() {
            return detection;
        }
    }

    private static class CombatWatcherHandle {
        private static final CombatWatcherHandle NOOP = new CombatWatcherHandle(null, null);

        private final AtomicBoolean running;
        private final Future<?> future;

        private CombatWatcherHandle(AtomicBoolean running, Future<?> future) {
            this.running = running;
            this.future = future;
        }

        private static CombatWatcherHandle noop() {
            return NOOP;
        }

        private void stop() {
            if (running != null) {
                running.set(false);
            }
            if (future != null) {
                future.cancel(true);
            }
        }
    }
}
