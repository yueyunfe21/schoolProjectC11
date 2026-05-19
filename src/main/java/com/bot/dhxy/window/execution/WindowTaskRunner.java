package com.bot.dhxy.window.execution;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.TaskFactory;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class WindowTaskRunner {

    private static final AtomicInteger THREAD_ID = new AtomicInteger(1);

    private final WindowRuntimeContext windowContext;
    private final TaskFactory taskFactory;
    private final WindowTaskContextHolder contextHolder;
    private final ExecutorService executor;

    private volatile RunningTaskHandle currentTask;
    private volatile boolean shutdown;

    public WindowTaskRunner(WindowRuntimeContext windowContext, TaskFactory taskFactory, WindowTaskContextHolder contextHolder) {
        this.windowContext = Objects.requireNonNull(windowContext, "windowContext must not be null");
        this.taskFactory = Objects.requireNonNull(taskFactory, "taskFactory must not be null");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder must not be null");
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("window-task-" + windowContext.getWindowId() + "-" + THREAD_ID.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized boolean submit(TaskType taskType) {
        TaskType safeTaskType = taskType == null ? TaskType.UNKNOWN : taskType;
        if (shutdown) {
            windowContext.markError("runner is closed");
            log.warn("window [{}] runner is closed, reject task {}", windowContext.getWindowId(), safeTaskType);
            return false;
        }
        if (safeTaskType == TaskType.UNKNOWN) {
            windowContext.markError("invalid task type");
            log.warn("window [{}] invalid task type", windowContext.getWindowId());
            return false;
        }
        if (isRunning()) {
            log.warn("window [{}] already has running task, reject {}", windowContext.getWindowId(), safeTaskType);
            return false;
        }

        GameTask task = taskFactory.createTask(windowContext, safeTaskType);
        if (task == null) {
            windowContext.markError("cannot create task: " + safeTaskType);
            log.error("window [{}] cannot create task: {}", windowContext.getWindowId(), safeTaskType);
            return false;
        }

        TaskStopToken stopToken = new TaskStopToken();
        TaskExecutionContext executionContext = buildExecutionContext(task, stopToken);
        FutureTask<Void> futureTask = new FutureTask<>(() -> {
            runTask(safeTaskType, task, executionContext);
            return null;
        });

        currentTask = new RunningTaskHandle(windowContext.getWindowId(), safeTaskType, task, stopToken, futureTask);
        windowContext.markQueued(safeTaskType);
        executor.execute(futureTask);
        return true;
    }

    public void refreshRegistration(WindowRegistrationRequest request) {
        if (request != null) {
            windowContext.applyRegistration(request, !isRunning());
        }
    }

    public void stopCurrentTask() {
        RunningTaskHandle taskHandle = currentTask;
        if (taskHandle == null || !taskHandle.isRunning()) {
            return;
        }
        windowContext.markStopping("stop requested");
        windowContext.getGameContext().runWithState(windowContext.getGameState(), () -> taskHandle.requestStop("stop requested"));
        taskHandle.forceCancel("stop requested");
    }

    public RunningTaskHandle getCurrentTask() { return currentTask; }

    public WindowRuntimeContext getWindowContext() { return windowContext; }

    public boolean isRunning() {
        RunningTaskHandle taskHandle = currentTask;
        return taskHandle != null && taskHandle.isRunning();
    }

    public boolean isShutdown() { return shutdown; }

    public WindowTaskSnapshot snapshot() {
        RunningTaskHandle taskHandle = currentTask;
        boolean running = taskHandle != null && taskHandle.isRunning();
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
                windowContext.getNativeBinding()
        );
    }

    public void shutdownNow() {
        shutdown = true;
        RunningTaskHandle taskHandle = currentTask;
        if (taskHandle != null) {
            windowContext.getGameContext().runWithState(windowContext.getGameState(), () -> taskHandle.requestStop("runner closed"));
            taskHandle.forceCancel("runner closed");
        }
        executor.shutdownNow();
    }

    private void runTask(TaskType taskType, GameTask task, TaskExecutionContext executionContext) {
        RunningTaskHandle taskHandle = currentTask;
        if (taskHandle != null) {
            taskHandle.markRunningThread(Thread.currentThread());
        }
        try {
            contextHolder.runWith(windowContext,
                    () -> windowContext.getGameContext().runWithState(windowContext.getGameState(),
                            () -> runTaskWithBoundGameState(taskType, task, executionContext)));
        } finally {
            RunningTaskHandle latestHandle = currentTask;
            if (latestHandle != null) {
                latestHandle.clearRunningThread();
            }
        }
    }

    private void runTaskWithBoundGameState(TaskType taskType, GameTask task, TaskExecutionContext executionContext) {
        windowContext.markStarted(taskType);
        log.info("{} window [{}] start task: {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName());

        TaskRunResult result = TaskRunResult.FAILED;
        String finishMessage = "task result: " + result;
        try {
            result = task.execute(executionContext);
            finishMessage = "task result: " + result;
        } catch (TaskStopRequestedException | CancellationException e) {
            result = TaskRunResult.STOPPED;
            finishMessage = "task stopped: " + normalizeMessage(e.getMessage());
            log.info("{} window [{}] task stopped: {} reason={}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), e.getMessage());
        } catch (Exception e) {
            result = TaskRunResult.FAILED;
            finishMessage = "task exception: " + e.getClass().getSimpleName();
            log.error("{} window [{}] task error: {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), e);
        } finally {
            WindowRuntimeStatus status = toWindowStatus(result);
            windowContext.markFinished(status, taskType, result, finishMessage);
            log.info("{} window [{}] task finished: {} -> {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), result);
            currentTask = null;
        }
    }

    private TaskExecutionContext buildExecutionContext(GameTask task, TaskStopToken stopToken) {
        WindowNativeBinding binding = windowContext.getNativeBinding();
        return TaskExecutionContext.builder()
                .taskCode(task.getTaskCode())
                .taskName(task.getTaskName())
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

    private String normalizeMessage(String message) {
        return message == null || message.isBlank() ? "-" : message;
    }
}