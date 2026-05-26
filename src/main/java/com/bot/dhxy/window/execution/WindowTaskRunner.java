package com.bot.dhxy.window.execution;

import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.TaskFactory;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.startup.TaskTeamAssignmentPolicy;
import com.bot.dhxy.team.TeamRoleDetectionService;
import com.bot.dhxy.team.TeamRoleStatus;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
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

    private final WindowRuntimeContext windowContext;
    private final TaskFactory taskFactory;
    private final WindowTaskContextHolder contextHolder;
    private final WindowTaskStartupInitializer startupInitializer;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final InputSequences inputSequences;
    private final TeamRoleDetectionService teamRoleDetectionService;
    private final TaskTeamAssignmentPolicy taskTeamAssignmentPolicy;
    private final ExecutorService executor;

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
     */
    public WindowTaskRunner(WindowRuntimeContext windowContext,
                            TaskFactory taskFactory,
                            WindowTaskContextHolder contextHolder,
                            WindowTaskStartupInitializer startupInitializer,
                            TaskExecutionContextHolder taskExecutionContextHolder,
                            InputSequences inputSequences,
                            TeamRoleDetectionService teamRoleDetectionService,
                            TaskTeamAssignmentPolicy taskTeamAssignmentPolicy) {
        this.windowContext = Objects.requireNonNull(windowContext, "windowContext must not be null");
        this.taskFactory = Objects.requireNonNull(taskFactory, "taskFactory must not be null");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder must not be null");
        this.startupInitializer = Objects.requireNonNull(startupInitializer, "startupInitializer must not be null");
        this.taskExecutionContextHolder = Objects.requireNonNull(taskExecutionContextHolder, "taskExecutionContextHolder must not be null");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences must not be null");
        this.teamRoleDetectionService = Objects.requireNonNull(teamRoleDetectionService, "teamRoleDetectionService must not be null");
        this.taskTeamAssignmentPolicy = Objects.requireNonNull(taskTeamAssignmentPolicy, "taskTeamAssignmentPolicy must not be null");
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("window-task-" + windowContext.getWindowId() + "-" + THREAD_ID.getAndIncrement());
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
     */
    public void stopCurrentTask() {
        RunningTaskHandle taskHandle = getActiveTaskHandle();
        if (taskHandle == null) {
            if (windowContext.getStatus() == WindowRuntimeStatus.ERROR
                    || windowContext.getStatus() == WindowRuntimeStatus.STOPPING) {
                windowContext.markStoppedAfterTerminalStop("stop requested after task already ended");
                log.info("window [{}] terminal task state cleared by stop command: status={}",
                        windowContext.getWindowId(), windowContext.getStatus());
            }
            return;
        }
        windowContext.markStopping("stop requested");
        windowContext.getGameContext().runWithState(windowContext.getGameState(), () -> taskHandle.requestStop("stop requested"));
        taskHandle.forceCancel("stop requested");
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
        for (int i = 0; i < taskTypes.size(); i++) {
            TaskType requestedTaskType = taskTypes.get(i);
            TaskType taskType = requestedTaskType;
            if (Thread.currentThread().isInterrupted() || (stopToken != null && stopToken.isStopRequested())) {
                windowContext.markFinished(WindowRuntimeStatus.STOPPED, taskType, TaskRunResult.STOPPED, "task queue stopped");
                queueResult = TaskRunResult.STOPPED;
                break;
            }

            TaskExecutionContext preflightContext = buildExecutionContext(requestedTaskType, stopToken, pauseToken);
            taskType = resolveTaskTypeBeforeStart(requestedTaskType, preflightContext);
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
        windowContext.markStarted(taskType);
        log.info("{} window [{}] start task: {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName());

        TaskRunResult result = TaskRunResult.FAILED;
        AtomicReference<String> finishMessage = new AtomicReference<>("task result: " + result);
        try {
            result = taskExecutionContextHolder.callWith(executionContext, () -> {
                if (!startupInitializer.beforeTask(windowContext, executionContext)) {
                    finishMessage.set("window startup initialization failed");
                    log.warn("{} window [{}] startup initialization failed, skip task: {}",
                            executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName());
                    return TaskRunResult.FAILED;
                }
                executionContext.throwIfStopRequested();
                return task.execute(executionContext);
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
            finishMessage.set("task exception: " + e.getClass().getSimpleName());
            log.error("{} window [{}] task error: {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), e);
        } finally {
            WindowRuntimeStatus status = toWindowStatus(result);
            windowContext.markFinished(status, taskType, result, finishMessage.get());
            log.info("{} window [{}] task finished: {} -> {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), result);
        }
        return result;
    }

    /**
     * Run live role detection and possibly reassign a requested task before it starts.
     */
    private TaskType resolveTaskTypeBeforeStart(TaskType requestedTaskType,
                                                TaskExecutionContext preflightContext) {
        if (!taskTeamAssignmentPolicy.shouldDetectRoleBeforeStart(requestedTaskType)) {
            return requestedTaskType;
        }
        preflightContext.throwIfStopRequested();
        TeamRoleStatus role = teamRoleDetectionService.detectCurrentRole(preflightContext);
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
        preflightContext.throwIfStopRequested();
        return resolvedTaskType;
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
}
