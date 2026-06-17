package com.bot.dhxy.window.execution;

import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogFingerprintWashMode;
import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogPreparationRequest;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.navigation.PendingTransferChoiceMemory;
import com.bot.dhxy.model.navigation.TemplateLocationInfo;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.DialogChoiceMemoryService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.TaskTrackerPanelService;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.TaskFactory;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.startup.TaskTeamAssignmentPolicy;
import com.bot.dhxy.team.TeamRoleDetectionService;
import com.bot.dhxy.team.TeamRoleStatus;
import com.bot.dhxy.tools.ImagePreprocessor;
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

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private static final long WINDOW_COMBAT_GUARD_IDLE_INTERVAL_MS = 6_000L;
    private static final long WINDOW_PATHING_PROBE_ACTIVE_INTERVAL_MS = 1_000L;
    private static final long WINDOW_PATHING_PROBE_MIN_INTERVAL_MS = 2_000L;
    private static final long WINDOW_PATHING_COORDINATE_STOPPED_AWAY_MS = 2_200L;
    private static final long WINDOW_PATHING_MAP_ROUTE_STOPPED_AWAY_MS = 8_000L;
    private static final long WINDOW_PATHING_COORDINATE_AWAY_STOPPED_MS = 30_000L;
    private static final long WINDOW_PATHING_SLOW_PROBE_LOG_MS = 1_500L;
    private static final long WINDOW_DIALOG_PREPARE_ACTIVE_INTERVAL_MS = 100L;
    private static final long WINDOW_DIALOG_ATTENTION_RECENT_MS = 2_500L;
    private static final long WINDOW_DIALOG_VISIBLE_MAX_AGE_MS = 3_000L;
    private static final int WINDOW_DIALOG_FINGERPRINT_MAX_DISTANCE = 8;

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
    private final TaskTrackerPanelService taskTrackerPanelService;
    private final DialogChoiceMemoryService dialogChoiceMemoryService;
    private final List<WindowDialogPreparationProvider> dialogPreparationProviders;
    private final WindowReadyEventBus windowReadyEventBus;
    private final ExecutorService executor;
    private final ExecutorService combatWatcherExecutor;

    private volatile RunningTaskHandle currentTask;
    private volatile boolean shutdown;

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
     * @param dialogChoiceMemoryService route-option memory updated after watcher proof.
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
                            TaskTrackerPanelService taskTrackerPanelService,
                            DialogChoiceMemoryService dialogChoiceMemoryService,
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
        this.taskTrackerPanelService = Objects.requireNonNull(taskTrackerPanelService, "taskTrackerPanelService must not be null");
        this.dialogChoiceMemoryService = Objects.requireNonNull(dialogChoiceMemoryService, "dialogChoiceMemoryService must not be null");
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
        WindowTaskQueue safeQueue = queue == null ? WindowTaskQueue.empty() : queue;
        TaskType firstTaskType = safeQueue.firstTaskType();
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
        FutureTask<Void> futureTask = new FutureTask<>(() -> {
            runQueue(safeQueue, stopToken, pauseToken);
            return null;
        });

        currentTask = new RunningTaskHandle(windowContext.getWindowId(), safeQueue, firstTaskType, null, stopToken, pauseToken, futureTask);
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
        RunningTaskHandle taskHandle = currentTask;
        if (taskHandle != null) {
            windowContext.getGameContext().runWithState(windowContext.getGameState(), () -> taskHandle.requestStop("runner closed"));
            taskHandle.forceCancel("runner closed");
        }
        combatWatcherExecutor.shutdownNow();
        executor.shutdownNow();
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
        List<TaskType> taskTypes = queue.getTaskTypes();
        WindowTaskFailurePolicy failurePolicy = queue.getFailurePolicy();
        TaskRunResult queueResult = TaskRunResult.SKIPPED;
        int completedCount = 0;
        TaskType activeTaskType = TaskType.UNKNOWN;
        try {
            for (int i = 0; i < taskTypes.size(); i++) {
                TaskType requestedTaskType = taskTypes.get(i);
                activeTaskType = requestedTaskType;
                TaskExecutionContext preflightContext = buildExecutionContext(requestedTaskType, stopToken, pauseToken);
                TaskCheckpoint.throwIfStopRequested(preflightContext, "task queue stopped before preflight");

                TaskType taskType = resolveTaskTypeBeforeStart(requestedTaskType, preflightContext);
                activeTaskType = taskType == TaskType.UNKNOWN ? requestedTaskType : taskType;
                TaskCheckpoint.throwIfStopRequested(preflightContext, "task queue stopped after preflight");
                if (taskType == TaskType.UNKNOWN) {
                    windowContext.markFinished(WindowRuntimeStatus.IDLE, requestedTaskType, TaskRunResult.SKIPPED, "task skipped by team role policy");
                    log.info("{} window [{}] skip task by team role policy: requested={}",
                            preflightContext.getLogPrefix(), windowContext.getWindowId(), requestedTaskType);
                    completedCount++;
                    queueResult = mergeQueueResult(queueResult, TaskRunResult.SKIPPED);
                    continue;
                }

                GameTask task = taskFactory.createTask(windowContext, taskType);
                if (task == null) {
                    windowContext.markError("cannot create task: " + taskType);
                    log.error("window [{}] cannot create task: {}", windowContext.getWindowId(), taskType);
                    queueResult = TaskRunResult.FAILED;
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
                TaskExecutionContext executionContext = buildExecutionContext(requestedTaskType, task, stopToken, pauseToken);
                TaskRunResult result = runTaskWithBoundGameState(taskType, task, executionContext);
                completedCount++;
                queueResult = mergeQueueResult(queueResult, result);
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
            shouldRunWindowObserver(taskType), taskType == TaskType.DEBUG_NAVIGATION_STRESS);
        return new CombatWatcherHandle(running, future);
    }

    private boolean shouldRunWindowObserver(TaskType taskType) {
        return taskType == TaskType.WUHuan_V2
            || taskType == TaskType.XIULUO_V2
            || taskType == TaskType.WUBEI
            || taskType == TaskType.DEBUG_NAVIGATION_STRESS;
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
                                }
                                while (running.get() && !Thread.currentThread().isInterrupted()) {
                                    executionContext.throwIfStopRequested();
                                    AutoCombatService.TickResult tick = combatGuardEnabled
                                            ? autoCombatService.handleWindowCombatGuardTick(
                                                    executionContext, "window-combat-watch:" + taskType.getCode())
                                            : AutoCombatService.TickResult.NONE;
                                    boolean pathingIntentActive = windowContext.getActivePathingIntent().isPresent();
                                    PreparedDialogAction preparedDialogAction = null;
                                    WindowPathingSnapshot pathingSnapshot = null;
                                    long tickStartedAt = System.currentTimeMillis();
                                    long pathingElapsedMs = -1L;
                                    long routePrepareElapsedMs = -1L;
                                    long taskTrackerPrepareElapsedMs = -1L;
                                    long attentionDetectElapsedMs = -1L;
                                    long attentionPublishElapsedMs = -1L;
                                    long attentionRoutePrepareElapsedMs = -1L;
                                    long attentionTotalElapsedMs = -1L;
                                    String observerBranch = pathingIntentActive ? "active-pathing" : "idle";
                                    if (pathingIntentActive) {
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
                                        preparedDialogAction = refreshDialogPreparationSignal(taskType, executionContext);
                                        routePrepareElapsedMs = Math.max(0L, System.currentTimeMillis() - routePrepareStartedAt);
                                        if (preparedDialogAction == null) {
                                            preparedDialogAction = refreshTaskDialogInterestPreparationSignal(taskType, executionContext);
                                        }
                                    } else {
                                        long routePrepareStartedAt = System.currentTimeMillis();
                                        preparedDialogAction = refreshDialogPreparationSignal(taskType, executionContext);
                                        routePrepareElapsedMs = Math.max(0L, System.currentTimeMillis() - routePrepareStartedAt);
                                        if (preparedDialogAction == null) {
                                            preparedDialogAction = refreshTaskDialogInterestPreparationSignal(taskType, executionContext);
                                        }
                                        if (preparedDialogAction == null) {
                                            long taskTrackerPrepareStartedAt = System.currentTimeMillis();
                                            preparedDialogAction = refreshTaskTrackerPreparationSignal(taskType, executionContext);
                                            taskTrackerPrepareElapsedMs = Math.max(0L, System.currentTimeMillis() - taskTrackerPrepareStartedAt);
                                        }
                                    }
                                    if (preparedDialogAction == null) {
                                        long[] attentionTimings = new long[] {-1L, -1L, -1L, -1L};
                                        preparedDialogAction = publishTaskAttentionIfDialogVisible(
                                                taskType, executionContext, attentionTimings);
                                        attentionDetectElapsedMs = attentionTimings[0];
                                        attentionPublishElapsedMs = attentionTimings[1];
                                        attentionRoutePrepareElapsedMs = attentionTimings[2];
                                        attentionTotalElapsedMs = attentionTimings[3];
                                    }
                                    long intervalMs = tick == AutoCombatService.TickResult.IN_COMBAT
                                            ? autoCombatService.getDynamicPollingIntervalMs()
                                            : WINDOW_COMBAT_GUARD_IDLE_INTERVAL_MS;
                                    if (pathingSnapshot != null && pathingSnapshot.hasActiveIntent()) {
                                        intervalMs = Math.min(intervalMs, WINDOW_PATHING_PROBE_ACTIVE_INTERVAL_MS);
                                    }
                                    if (windowContext.getDialogPreparationRequest() != null || preparedDialogAction != null) {
                                        intervalMs = Math.min(intervalMs, WINDOW_DIALOG_PREPARE_ACTIVE_INTERVAL_MS);
                                    }
                                    logSlowObserverTick(taskType, executionContext, observerBranch, pathingSnapshot,
                                            preparedDialogAction, pathingElapsedMs, routePrepareElapsedMs,
                                            taskTrackerPrepareElapsedMs, attentionDetectElapsedMs,
                                            attentionPublishElapsedMs, attentionRoutePrepareElapsedMs,
                                            attentionTotalElapsedMs,
                                            Math.max(0L, System.currentTimeMillis() - tickStartedAt), intervalMs);
                                    if (!TaskSleep.sleep(intervalMs)) {
                                        break;
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
     * Log only meaningful watcher latency so multi-window handoff stalls can be diagnosed without
     * flooding the console on every 100ms dialog-preparation tick.
     */
    private void logSlowObserverTick(TaskType taskType,
                                     TaskExecutionContext executionContext,
                                     String branch,
                                     WindowPathingSnapshot pathingSnapshot,
                                     PreparedDialogAction preparedDialogAction,
                                     long pathingElapsedMs,
                                     long routePrepareElapsedMs,
                                     long taskTrackerPrepareElapsedMs,
                                     long attentionDetectElapsedMs,
                                     long attentionPublishElapsedMs,
                                     long attentionRoutePrepareElapsedMs,
                                     long attentionTotalElapsedMs,
                                     long totalElapsedMs,
                                     long nextIntervalMs) {
        boolean activePathing = pathingSnapshot != null && pathingSnapshot.hasActiveIntent();
        boolean slow = totalElapsedMs >= 1_000L
                || pathingElapsedMs >= 1_000L
                || routePrepareElapsedMs >= 1_000L
                || taskTrackerPrepareElapsedMs >= 1_000L
                || attentionDetectElapsedMs >= 1_000L
                || attentionPublishElapsedMs >= 1_000L
                || attentionRoutePrepareElapsedMs >= 1_000L
                || attentionTotalElapsedMs >= 1_000L;
        if (!slow && preparedDialogAction == null && !activePathing) {
            return;
        }
        WindowPathingIntent activeIntent = windowContext.getActivePathingIntent().orElse(null);
        WindowPathingIntent snapshotIntent = pathingSnapshot == null ? null : pathingSnapshot.getIntent();
        log.info("{} window [{}] window observer tick: task={} branch={} totalMs={} pathingMs={} routePrepareMs={} taskTrackerPrepareMs={} attentionDetectMs={} attentionPublishMs={} attentionRoutePrepareMs={} attentionTotalMs={} nextIntervalMs={} activeIntentId={} activeIntentTarget={} activeIntentAgeMs={} pathingState={} pathingCurrent={} pathingTarget={} preparedOperation={} preparedTarget={}",
                executionContext.getLogPrefix(),
                windowContext.getWindowId(),
                taskType,
                branch,
                totalElapsedMs,
                pathingElapsedMs,
                routePrepareElapsedMs,
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

    private PreparedDialogAction refreshTaskTrackerPreparationSignal(TaskType taskType,
                                                                     TaskExecutionContext executionContext) {
        if (taskType != TaskType.WUHuan_V2 || windowContext.getDialogPreparationRequest() != null) {
            return null;
        }
        PreparedDialogAction existing = windowContext.getPreparedDialogAction();
        if (existing != null) {
            if (!existing.matches(DialogOperation.TASK_TRACKER_PATHING, "wuhuan")) {
                return null;
            }
            if (validatePreparedDialogAction(existing, taskType, executionContext)) {
                return windowContext.getPreparedDialogAction();
            }
        }

        long startedAt = System.currentTimeMillis();
        return taskTrackerPanelService.prepareWuhuanPathingLink("window-task-tracker-prepare:" + taskType.getCode())
                .map(action -> {
                    PreparedDialogAction boundAction = action.toBuilder()
                            .windowId(windowContext.getWindowId())
                            .hwnd(windowContext.getNativeBinding().getNativeHandle())
                            .build();
                    windowContext.updatePreparedDialogAction(boundAction);
                    publishPreparedActionReady(taskType, boundAction, executionContext,
                            "task-tracker-prepared");
                    log.info("{} window [{}] task tracker panel prepared: task={} operation={} click=({}, {}) elapsedMs={}",
                            executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                            boundAction.getOperation(), boundAction.getAbsoluteX(), boundAction.getAbsoluteY(),
                            Math.max(0L, System.currentTimeMillis() - startedAt));
                    return boundAction;
                })
                .orElse(null);
    }

    private PreparedDialogAction refreshTaskDialogInterestPreparationSignal(TaskType taskType,
                                                                           TaskExecutionContext executionContext) {
        if (windowContext.getDialogPreparationRequest() != null) {
            return null;
        }
        Optional<WindowDialogInterest> interestOpt = windowContext.getDialogInterest();
        if (interestOpt.isEmpty()) {
            return null;
        }
        WindowDialogInterest interest = interestOpt.get();
        if (interest.getTaskType() != taskType || interest.getOperations() == null || interest.getOperations().isEmpty()) {
            return null;
        }
        Optional<WindowDialogSnapshot> visibleDialogOpt = windowContext.getVisibleDialogSnapshot();
        long now = System.currentTimeMillis();
        if (visibleDialogOpt.isEmpty()
                || visibleDialogOpt.get().getType() == DialogType.NONE
                || now - visibleDialogOpt.get().getDetectedAtMs() > WINDOW_DIALOG_VISIBLE_MAX_AGE_MS) {
            return null;
        }
        PreparedDialogAction existing = windowContext.getPreparedDialogAction();
        if (existing != null) {
            if (!interest.supports(taskType, existing.getOperation())) {
                return null;
            }
            if (validatePreparedDialogAction(existing, taskType, executionContext)) {
                return windowContext.getPreparedDialogAction();
            }
        }

        long startedAt = System.currentTimeMillis();
        for (DialogOperation operation : interest.getOperations()) {
            for (WindowDialogPreparationProvider provider : dialogPreparationProviders) {
                if (!provider.supports(taskType, operation)) {
                    continue;
                }
                String source = "window-task-dialog-prepare:" + taskType.getCode() + ":" + operation;
                Optional<PreparedDialogAction> prepared = provider.prepare(interest, operation, source);
                if (prepared.isEmpty()) {
                    continue;
                }
                PreparedDialogAction boundAction = prepared.get().toBuilder()
                        .windowId(windowContext.getWindowId())
                        .hwnd(windowContext.getNativeBinding().getNativeHandle())
                        .build();
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
        return null;
    }

    private PreparedDialogAction publishTaskAttentionIfDialogVisible(TaskType taskType,
                                                                     TaskExecutionContext executionContext,
                                                                     long[] timingMs) {
        long methodStartedAt = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        Optional<WindowReadyEvent> recent = windowReadyEventBus.latest(
                windowContext.getWindowId(), WindowReadyEventType.TASK_ATTENTION_REQUIRED);
        if (recent.isPresent() && now - recent.get().getCreatedAtMs() < WINDOW_DIALOG_ATTENTION_RECENT_MS) {
            timingMs[3] = Math.max(0L, System.currentTimeMillis() - methodStartedAt);
            return null;
        }
        try {
            String probeSource = "window-task-attention:" + taskType.getCode();
            long detectStartedAt = System.currentTimeMillis();
            DialogType visibleType = dialogService.detectDialogTypeNoFocus(
                    probeSource,
                    false,
                    0);
            timingMs[0] = Math.max(0L, System.currentTimeMillis() - detectStartedAt);
            if (visibleType == DialogType.NONE) {
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
             * Publish the soft wake before any route OCR/template preparation. Preparation can be
             * slow when the visible dialog is unstable; the scheduler should still learn quickly
             * that this window needs a foreground turn.
             */
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
            PreparedDialogAction preparedAction = refreshDialogPreparationSignal(taskType, executionContext);
            if (preparedAction == null) {
                preparedAction = refreshTaskDialogInterestPreparationSignal(taskType, executionContext);
            }
            timingMs[2] = Math.max(0L, System.currentTimeMillis() - prepareStartedAt);
            if (preparedAction != null) {
                long preparedPublishStartedAt = System.currentTimeMillis();
                windowReadyEventBus.publish(WindowReadyEvent.builder()
                        .windowId(windowContext.getWindowId())
                        .hwnd(windowContext.getNativeBinding().getNativeHandle())
                        .type(WindowReadyEventType.TASK_ATTENTION_REQUIRED)
                        .taskType(taskType)
                        .source("dialog-visible-prepared:" + visibleType)
                        .operation(preparedAction.getOperation())
                        .targetKeyword(preparedAction.getTargetKeyword())
                        .createdAtMs(System.currentTimeMillis())
                        .build());
                timingMs[1] = Math.max(timingMs[1],
                        Math.max(0L, System.currentTimeMillis() - preparedPublishStartedAt));
            }
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
            log.debug("{} window [{}] task attention probe failed: task={} reason={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, e.getMessage());
            return null;
        }
    }

    private PreparedDialogAction refreshDialogPreparationSignal(TaskType taskType,
                                                                TaskExecutionContext executionContext) {
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
            if (validatePreparedDialogAction(existing, taskType, executionContext)) {
                return windowContext.getPreparedDialogAction();
            }
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
            DialogChoiceMemoryService.DialogChoiceEntry remembered = request == null
                    ? findRouteMemoryForIntent(activeIntent, targetKeyword)
                    : null;
            Integer rememberedRelativeX = request == null
                    ? remembered == null ? null : remembered.relativeX
                    : request.getRememberedRelativeX();
            Integer rememberedRelativeY = request == null
                    ? remembered == null ? null : remembered.relativeY
                    : request.getRememberedRelativeY();
            String rememberedOptionText = request == null
                    ? remembered == null ? null : remembered.optionText
                    : request.getRememberedOptionText();
            Optional<PreparedDialogAction> prepared = rememberedRelativeX != null && rememberedRelativeY != null
                    ? dialogService.prepareRememberedRouteOption(
                    source,
                    targetKeyword,
                    rememberedRelativeX,
                    rememberedRelativeY,
                    rememberedOptionText)
                    : dialogService.prepareRouteKeywordOption(source, targetKeyword);
            return prepared
                    .map(action -> {
                        DialogPreparationRequest currentRequest = windowContext.getDialogPreparationRequest();
                        WindowPathingIntent currentIntent = windowContext.getActivePathingIntent().orElse(null);
                        /*
                         * OCR/template work can take noticeable time. Before publishing the click
                         * candidate, ensure the request or active pathing intent that authorized it
                         * is still the same one.
                         */
                        if (request != null && currentRequest != request) {
                            windowContext.markDialogPreparationFailed(request, "stale request");
                            logRouteDialogPreparation("stale-request", taskType, executionContext,
                                    request, activeIntent, targetKeyword, visibleSnapshot.get(),
                                    Math.max(0L, System.currentTimeMillis() - request.getCreatedAtMs()),
                                    Math.max(0L, System.currentTimeMillis() - startedAt),
                                    action.getMatchedText(), null, 0, 0);
                            return null;
                        }
                        if (request == null && !isSamePathingIntent(activeIntent, currentIntent)) {
                            logRouteDialogPreparation("stale-intent", taskType, executionContext,
                                    null, activeIntent, targetKeyword, visibleSnapshot.get(),
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
                                request, activeIntent, targetKeyword, visibleSnapshot.get(),
                                request == null ? -1L : Math.max(0L, System.currentTimeMillis() - request.getCreatedAtMs()),
                                Math.max(0L, System.currentTimeMillis() - startedAt),
                                boundAction.getMatchedText(), boundAction.getSource(),
                                boundAction.getAbsoluteX(), boundAction.getAbsoluteY());
                        return boundAction;
                    })
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

    private DialogChoiceMemoryService.DialogChoiceEntry findRouteMemoryForIntent(WindowPathingIntent intent,
                                                                                 String targetKeyword) {
        if (intent == null || targetKeyword == null) {
            return null;
        }
        WindowPathingSnapshot snapshot = windowContext.getPathingSnapshot();
        String fromMap = snapshot == null ? null : snapshot.getCurrentMapName();
        return dialogChoiceMemoryService.findUsableRoute(fromMap, targetKeyword).orElse(null);
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

    private boolean validatePreparedDialogAction(PreparedDialogAction action,
                                                 TaskType taskType,
                                                 TaskExecutionContext executionContext) {
        if (action == null || action.getFingerprint() == null || action.getFingerprint().isBlank()) {
            return false;
        }
        if (!matchesCurrentWindowBinding(action)) {
            windowContext.clearPreparedDialogAction("dialog prepared action belongs to another binding");
            return false;
        }
        int left = action.getValidationLeft();
        int top = action.getValidationTop();
        int right = action.getValidationRight();
        int bottom = action.getValidationBottom();
        if (right <= left || bottom <= top) {
            windowContext.clearPreparedDialogAction("invalid dialog validation rect");
            return false;
        }
        BufferedImage raw = null;
        BufferedImage washed = null;
        try {
            raw = dialogService.captureDialogValidationImage("dialog-prepare-validate:" + taskType.getCode(),
                    left, top, right, bottom);
            washed = washPreparedValidationCrop(raw, action.getWashMode());
            String currentFingerprint = ImagePreprocessor.buildBinaryFingerprint(washed);
            int distance = ImagePreprocessor.binaryFingerprintDistance(action.getFingerprint(), currentFingerprint);
            if (action.getOperation() != DialogOperation.TASK_TRACKER_PATHING
                    && !isTaskInterestPreparedOperation(taskType, action.getOperation())) {
                WindowPathingIntent currentIntent = windowContext.getActivePathingIntent().orElse(null);
                if (action.getIntentId() != null) {
                    if (currentIntent == null || !Objects.equals(action.getIntentId(), currentIntent.getIntentId())) {
                        log.debug("{} window [{}] dialog prepared action validation skipped: task={} operation={} target={} reason=intent-consumed actionIntentId={} currentIntentId={}",
                                executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                                action.getOperation(), action.getTargetKeyword(), action.getIntentId(),
                                currentIntent == null ? null : currentIntent.getIntentId());
                        return false;
                    }
                } else {
                    DialogPreparationRequest currentRequest = windowContext.getDialogPreparationRequest();
                    if (currentRequest == null || currentRequest.getOperation() != action.getOperation()
                            || !action.matches(currentRequest.getOperation(), currentRequest.getTargetKeyword())) {
                        log.debug("{} window [{}] dialog prepared action validation skipped: task={} operation={} target={} reason=request-consumed",
                                executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                                action.getOperation(), action.getTargetKeyword());
                        return false;
                    }
                }
                if (currentIntent != null
                        && action.getIntentId() == null
                        && action.getOperation() == DialogOperation.ROUTE_TRANSFER
                        && !Objects.equals(action.getTargetKeyword(), currentIntent.getTargetMapName())) {
                    log.debug("{} window [{}] dialog prepared action validation skipped: task={} operation={} target={} reason=request-consumed",
                            executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                            action.getOperation(), action.getTargetKeyword());
                    return false;
                }
            }
            if (distance <= WINDOW_DIALOG_FINGERPRINT_MAX_DISTANCE) {
                PreparedDialogAction refreshed = action.toBuilder()
                        .lastVerifiedAtMs(System.currentTimeMillis())
                        .build();
                windowContext.updatePreparedDialogAction(refreshed);
                publishPreparedActionReady(taskType, refreshed, executionContext,
                        "prepared-action-verified");
                log.debug("{} window [{}] dialog prepared action verified: task={} operation={} target={} distance={} click=({}, {})",
                        executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                        action.getOperation(), action.getTargetKeyword(), distance,
                        action.getAbsoluteX(), action.getAbsoluteY());
                return true;
            }
            windowContext.clearPreparedDialogAction("dialog validation fingerprint changed");
            log.info("{} window [{}] dialog prepared action invalidated: task={} operation={} target={} distance={} maxDistance={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    action.getOperation(), action.getTargetKeyword(), distance,
                    WINDOW_DIALOG_FINGERPRINT_MAX_DISTANCE);
            return false;
        } catch (RuntimeException e) {
            log.debug("{} window [{}] dialog prepared action validation failed: task={} operation={} target={} reason={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    action.getOperation(), action.getTargetKeyword(), e.getMessage());
            return false;
        } finally {
            if (raw != null) {
                raw.flush();
            }
            if (washed != null && washed != raw) {
                washed.flush();
            }
        }
    }

    private boolean isTaskInterestPreparedOperation(TaskType taskType, DialogOperation operation) {
        return windowContext.getDialogInterest()
                .map(interest -> interest.supports(taskType, operation))
                .orElse(false);
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
                .type(WindowReadyEventType.TASK_ATTENTION_REQUIRED)
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

    private BufferedImage washPreparedValidationCrop(BufferedImage raw, DialogFingerprintWashMode washMode) {
        if (raw == null) {
            return null;
        }
        if (washMode == DialogFingerprintWashMode.YELLOW) {
            return ImagePreprocessor.washYellowTextToBlackAndWhite(raw);
        }
        if (washMode == DialogFingerprintWashMode.GREEN) {
            return ImagePreprocessor.washGreenTextToBlackAndWhite(raw);
        }
        if (washMode == DialogFingerprintWashMode.WHITE) {
            return ImagePreprocessor.washThinWhiteTextToBlackAndWhite(raw);
        }
        return ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(raw);
    }

    private boolean matchesCurrentWindowBinding(PreparedDialogAction action) {
        if (action == null) {
            return false;
        }
        if (action.getWindowId() != null && !Objects.equals(action.getWindowId(), windowContext.getWindowId())) {
            return false;
        }
        String currentHwnd = windowContext.getNativeBinding().getNativeHandle();
        return action.getHwnd() == null || Objects.equals(action.getHwnd(), currentHwnd);
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
        return windowContext.getActivePathingIntent()
                .map(intent -> refreshPathingSignal(taskType, executionContext, intent))
                .orElse(null);
    }

    private WindowPathingSnapshot refreshPathingSignal(TaskType taskType,
                                                       TaskExecutionContext executionContext,
                                                       WindowPathingIntent intent) {
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
                    .map(location -> updatePathingFromLocation(taskType, executionContext, intent, previous, location, startedAt))
                    .orElseGet(() -> updateUnknownPathing(taskType, executionContext, intent, previous, startedAt,
                            "mini-map template location miss"));
        } catch (RuntimeException e) {
            log.debug("{} window [{}] pathing watcher probe failed: task={} source={} reason={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    intent.getSource(), e.getMessage());
            return updateUnknownPathing(taskType, executionContext, intent, previous, startedAt, e.getClass().getSimpleName());
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
                                                            long startedAt) {
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
        WindowPathingState state = classifyPathingState(intent, previous, location, locationChanged,
                locationChangedAtMs, now);
        long probeMs = Math.max(0L, now - startedAt);
        WindowPathingSnapshot snapshot = WindowPathingSnapshot.builder()
                .state(state)
                .intent(intent)
                .currentMapName(location.mapName())
                .currentX(coordinate == null ? null : coordinate.getX())
                .currentY(coordinate == null ? null : coordinate.getY())
                .message("mini-map template location refreshed")
                .locationChangedAtMs(locationChangedAtMs)
                .updatedAtMs(now)
                .probeStartedAtMs(startedAt)
                .probeFinishedAtMs(now)
                .probeInProgress(false)
                .uiCleanupRecommended(previous != null && previous.isUiCleanupRecommended())
                .uiCleanupReason(previous == null ? null : previous.getUiCleanupReason())
                .uiCleanupRecommendedAtMs(previous == null ? 0L : previous.getUiCleanupRecommendedAtMs())
                .build();
        windowContext.updatePathingSnapshot(snapshot);

        long wallStationaryMs = Math.max(0L, now - locationChangedAtMs);
        long observedStationaryMs = Math.max(0L, snapshot.getUpdatedAtMs() - locationChangedAtMs);
        boolean stateChanged = previous == null || state != previous.getState();
        if (state == WindowPathingState.ARRIVED || stateChanged) {
            log.info("{} window [{}] pathing watcher update: task={} state={} source={} target={}({}, {}) current={}({}, {}) observedStationaryMs={} wallStationaryMs={} probeMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, state,
                    intent.getSource(), intent.getTargetMapName(), intent.getTargetX(), intent.getTargetY(),
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    observedStationaryMs, wallStationaryMs, probeMs);
        } else if (probeMs >= WINDOW_PATHING_SLOW_PROBE_LOG_MS) {
            log.info("{} window [{}] pathing watcher slow probe: task={} state={} source={} target={}({}, {}) current={}({}, {}) observedStationaryMs={} wallStationaryMs={} probeMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, state,
                    intent.getSource(), intent.getTargetMapName(), intent.getTargetX(), intent.getTargetY(),
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    observedStationaryMs, wallStationaryMs, probeMs);
        }
        publishPathingTerminalEventIfNeeded(taskType, intent, snapshot, state, stateChanged);
        settlePendingTransferChoiceMemory(intent, snapshot, state, executionContext);
        return snapshot;
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
            dialogChoiceMemoryService.recordRouteSuccess(
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
            dialogChoiceMemoryService.recordRouteFailure(
                    consumed.getFromMap(),
                    consumed.getTargetMap(),
                    consumed.getSource() + ":watcher-stopped-away");
            log.warn("{} window [{}] reject pending route memory: source={} target={} current={}({}, {}) ageMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), consumed.getSource(),
                    consumed.getTargetMap(), snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    Math.max(0L, System.currentTimeMillis() - consumed.getCreatedAtMs()));
        }
    }

    private WindowPathingSnapshot updateUnknownPathing(TaskType taskType,
                                                       TaskExecutionContext executionContext,
                                                       WindowPathingIntent intent,
                                                       WindowPathingSnapshot previous,
                                                       long startedAt,
                                                       String message) {
        long now = System.currentTimeMillis();
        if (!isCurrentPathingIntent(intent)) {
            log.info("{} window [{}] discard stale pathing unknown result: task={} source={} target={}({}, {}) reason={} probeMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType,
                    intent.getSource(), intent.getTargetMapName(), intent.getTargetX(), intent.getTargetY(),
                    message, Math.max(0L, now - startedAt));
            return windowContext.getPathingSnapshot();
        }
        long previousUpdatedAtMs = previous == null ? 0L : previous.getUpdatedAtMs();
        WindowPathingSnapshot snapshot = WindowPathingSnapshot.builder()
                .state(WindowPathingState.UNKNOWN)
                .intent(intent)
                .currentMapName(previous == null ? null : previous.getCurrentMapName())
                .currentX(previous == null ? null : previous.getCurrentX())
                .currentY(previous == null ? null : previous.getCurrentY())
                .message(message)
                .locationChangedAtMs(previous == null ? 0L : previous.getLocationChangedAtMs())
                .updatedAtMs(previousUpdatedAtMs)
                .probeStartedAtMs(startedAt)
                .probeFinishedAtMs(now)
                .probeInProgress(false)
                .uiCleanupRecommended(previous != null && previous.isUiCleanupRecommended())
                .uiCleanupReason(previous == null ? null : previous.getUiCleanupReason())
                .uiCleanupRecommendedAtMs(previous == null ? 0L : previous.getUiCleanupRecommendedAtMs())
                .build();
        windowContext.updatePathingSnapshot(snapshot);
        long probeMs = Math.max(0L, now - startedAt);
        if (probeMs >= WINDOW_PATHING_SLOW_PROBE_LOG_MS) {
            log.info("{} window [{}] pathing watcher unknown slow probe: task={} source={} reason={} probeMs={}",
                    executionContext.getLogPrefix(), windowContext.getWindowId(), taskType, intent.getSource(),
                    message, probeMs);
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
                                                    long now) {
        MapCoordinate coordinate = location.coordinate();
        if (intent.getType() != WindowPathingIntentType.UNTARGETED_TRACKER
                && hasArrived(intent, location.mapName(), coordinate)) {
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

    private long resolvePathingStoppedAwayMs(WindowPathingIntent intent, String currentMapName) {
        if (intent.getType() == WindowPathingIntentType.UNTARGETED_TRACKER) {
            return WINDOW_PATHING_COORDINATE_STOPPED_AWAY_MS;
        }
        if (intent.getTargetX() == null || intent.getTargetY() == null) {
            return WINDOW_PATHING_MAP_ROUTE_STOPPED_AWAY_MS;
        }
        if (intent.getTargetMapName() != null
                && currentMapName != null
                && !Objects.equals(intent.getTargetMapName(), currentMapName)) {
            return WINDOW_PATHING_COORDINATE_AWAY_STOPPED_MS;
        }
        return WINDOW_PATHING_COORDINATE_STOPPED_AWAY_MS;
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
        TeamRoleStatus role = teamRoleDetectionService.detectCurrentRole(preflightContext);
        TaskCheckpoint.throwIfStopRequested(preflightContext, "task queue stopped during team role detection");
        if ((role == null || role.isUnknown()) && windowContext.getRole() != null) {
            TeamRoleStatus contextRole = toTeamRoleStatus(windowContext.getRole());
            if (!contextRole.isUnknown()) {
                role = contextRole;
                log.info("{} window [{}] use existing window role after live role unknown: requested={} role={}",
                        preflightContext.getLogPrefix(), windowContext.getWindowId(), requestedTaskType, role);
            }
        }
        syncWindowRole(role);
        TaskType resolvedTaskType = taskTeamAssignmentPolicy.resolveTaskForRole(requestedTaskType, role);
        if (resolvedTaskType != requestedTaskType) {
            log.info("{} window [{}] task reassigned by team role: requested={} role={} resolved={}",
                    preflightContext.getLogPrefix(), windowContext.getWindowId(),
                    requestedTaskType, role, resolvedTaskType);
        } else {
            log.info("{} window [{}] task kept by team role: requested={} role={}",
                    preflightContext.getLogPrefix(), windowContext.getWindowId(), requestedTaskType, role);
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
        return buildExecutionContext(requestedTaskType, task.getTaskCode(), task.getTaskName(), stopToken, pauseToken);
    }

    private TaskExecutionContext buildExecutionContext(TaskType taskType, TaskStopToken stopToken, TaskPauseToken pauseToken) {
        TaskType safeTaskType = taskType == null ? TaskType.UNKNOWN : taskType;
        return buildExecutionContext(safeTaskType, safeTaskType.getCode(), safeTaskType.getDisplayName(), stopToken, pauseToken);
    }

    private TaskExecutionContext buildExecutionContext(TaskType requestedTaskType,
                                                       String taskCode,
                                                       String taskName,
                                                       TaskStopToken stopToken,
                                                       TaskPauseToken pauseToken) {
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
                .stopToken(stopToken)
                .pauseToken(pauseToken)
                .startedAt(LocalDateTime.now())
                .build();
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

    private static long ageMs(long nowMs, long timestampMs) {
        return timestampMs <= 0L ? -1L : Math.max(0L, nowMs - timestampMs);
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
