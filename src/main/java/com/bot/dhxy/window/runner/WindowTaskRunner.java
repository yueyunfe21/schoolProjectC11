package com.bot.dhxy.window.runner;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.TaskFactory;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单个游戏窗口的任务执行器。
 *
 * 一个 WindowTaskRunner 只负责一个独立游戏窗口。
 * 窗口内部使用单线程执行，避免同一个角色同时跑多个任务。
 */
@Slf4j
public class WindowTaskRunner {

    private static final AtomicInteger THREAD_ID = new AtomicInteger(1);

    private final WindowRuntimeContext windowContext;
    private final TaskFactory taskFactory;
    private final ExecutorService executor;

    private volatile RunningTaskHandle currentTask;
    private volatile boolean shutdown;

    public WindowTaskRunner(WindowRuntimeContext windowContext, TaskFactory taskFactory) {
        this.windowContext = Objects.requireNonNull(windowContext, "windowContext must not be null");
        this.taskFactory = Objects.requireNonNull(taskFactory, "taskFactory must not be null");
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
            windowContext.markError("窗口执行器已关闭");
            log.warn("窗口 [{}] 执行器已关闭，拒绝提交任务：{}", windowContext.getWindowId(), safeTaskType);
            return false;
        }
        if (safeTaskType == TaskType.UNKNOWN) {
            windowContext.markError("任务类型无效");
            log.warn("窗口 [{}] 任务类型无效，拒绝提交", windowContext.getWindowId());
            return false;
        }
        if (isRunning()) {
            log.warn("窗口 [{}] 已有任务运行，拒绝提交新任务：{}", windowContext.getWindowId(), safeTaskType);
            return false;
        }

        GameTask task = taskFactory.createTask(windowContext, safeTaskType);
        if (task == null) {
            windowContext.markError("无法创建任务：" + safeTaskType);
            log.error("窗口 [{}] 无法创建任务：{}", windowContext.getWindowId(), safeTaskType);
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
        if (request == null) {
            return;
        }
        windowContext.applyRegistration(request, !isRunning());
    }

    public void stopCurrentTask() {
        RunningTaskHandle taskHandle = currentTask;
        if (taskHandle == null || !taskHandle.isRunning()) {
            return;
        }
        windowContext.markStopping("用户请求停止窗口任务");
        taskHandle.requestStop("用户请求停止窗口任务");
    }

    public RunningTaskHandle getCurrentTask() {
        return currentTask;
    }

    public WindowRuntimeContext getWindowContext() {
        return windowContext;
    }

    public boolean isRunning() {
        RunningTaskHandle taskHandle = currentTask;
        return taskHandle != null && taskHandle.isRunning();
    }

    public boolean isShutdown() {
        return shutdown;
    }

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
                running,
                taskHandle == null ? null : taskHandle.getStartedAt(),
                windowContext.getLastStartedAt(),
                windowContext.getLastFinishedAt(),
                windowContext.getLastMessage(),
                windowContext.getNativeBinding()
        );
    }

    public void shutdownNow() {
        shutdown = true;
        stopCurrentTask();
        executor.shutdownNow();
    }

    private void runTask(TaskType taskType, GameTask task, TaskExecutionContext executionContext) {
        windowContext.getGameContext().runWithState(windowContext.getGameState(), () -> runTaskWithBoundGameState(taskType, task, executionContext));
    }

    private void runTaskWithBoundGameState(TaskType taskType, GameTask task, TaskExecutionContext executionContext) {
        windowContext.markStarted(taskType);
        log.info("{} 窗口 [{}] 开始执行任务：{}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName());

        TaskRunResult result = TaskRunResult.FAILED;
        try {
            result = task.execute(executionContext);
        } catch (TaskStopRequestedException | CancellationException e) {
            log.info("{} 窗口 [{}] 任务被停止：{}，原因：{}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), e.getMessage());
            result = TaskRunResult.STOPPED;
        } catch (Exception e) {
            log.error("{} 窗口 [{}] 任务异常：{}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), e);
            result = TaskRunResult.FAILED;
        } finally {
            WindowRuntimeStatus status = toWindowStatus(result);
            windowContext.markFinished(status, "任务结束：" + result);
            log.info("{} 窗口 [{}] 任务结束：{} -> {}", executionContext.getLogPrefix(), windowContext.getWindowId(), task.getTaskName(), result);
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
}
