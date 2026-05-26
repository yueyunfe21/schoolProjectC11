package com.bot.dhxy.window.execution;

import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.model.TaskType;

import java.time.LocalDateTime;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable runtime handle for the currently executing task queue on one window.
 *
 * <p>The handle is shared between UI/control code and the runner thread. Task type/index are atomic
 * because a queue can advance from one task to the next while snapshots are being read. Stop and
 * pause are cooperative tokens, with {@link #requestStop(String)} also interrupting the running
 * thread to unblock long sleeps.</p>
 */
public class RunningTaskHandle {

    private final String windowId;
    private final TaskStopToken stopToken;
    private final TaskPauseToken pauseToken;
    private final Future<?> future;
    private final LocalDateTime startedAt;
    private final WindowTaskQueue taskQueue;
    private final AtomicInteger taskIndex = new AtomicInteger(-1);
    private final AtomicReference<Thread> runningThread = new AtomicReference<>();
    private final AtomicReference<TaskType> taskType = new AtomicReference<>(TaskType.UNKNOWN);
    private final AtomicReference<GameTask> task = new AtomicReference<>();

    /**
     * Legacy single-task constructor.
     *
     * @param windowId owning window id.
     * @param taskType running task type.
     * @param task running task instance, nullable before creation.
     * @param stopToken cooperative stop token.
     * @param future executor future.
     */
    public RunningTaskHandle(String windowId,
                             TaskType taskType,
                             GameTask task,
                             TaskStopToken stopToken,
                             Future<?> future) {
        this(windowId, WindowTaskQueue.single(taskType), taskType, task, stopToken, new TaskPauseToken(), future);
    }

    /**
     * Queue constructor with an implicit pause token.
     *
     * @param windowId owning window id.
     * @param taskQueue submitted task queue; null becomes empty.
     * @param taskType current task type.
     * @param task current task instance, nullable before creation.
     * @param stopToken cooperative stop token.
     * @param future executor future.
     */
    public RunningTaskHandle(String windowId,
                             WindowTaskQueue taskQueue,
                             TaskType taskType,
                             GameTask task,
                             TaskStopToken stopToken,
                             Future<?> future) {
        this(windowId, taskQueue, taskType, task, stopToken, new TaskPauseToken(), future);
    }

    /**
     * Full queue constructor.
     *
     * @param windowId owning window id.
     * @param taskQueue submitted task queue; null becomes empty.
     * @param taskType current task type.
     * @param task current task instance, nullable until created.
     * @param stopToken cooperative stop token.
     * @param pauseToken cooperative pause token; null creates a fresh token.
     * @param future executor future.
     */
    public RunningTaskHandle(String windowId,
                             WindowTaskQueue taskQueue,
                             TaskType taskType,
                             GameTask task,
                             TaskStopToken stopToken,
                             TaskPauseToken pauseToken,
                             Future<?> future) {
        this.windowId = windowId;
        this.taskQueue = taskQueue == null ? WindowTaskQueue.empty() : taskQueue;
        updateTask(taskType, task);
        this.stopToken = stopToken;
        this.pauseToken = pauseToken == null ? new TaskPauseToken() : pauseToken;
        this.future = future;
        this.startedAt = LocalDateTime.now();
    }

    /** @return owning window id. */
    public String getWindowId() { return windowId; }

    /** @return current task type, or UNKNOWN before a queue item is created. */
    public TaskType getTaskType() { return taskType.get(); }

    /** @return current task instance, or null before creation/after cleanup. */
    public GameTask getTask() { return task.get(); }

    /** @return submitted task queue. */
    public WindowTaskQueue getTaskQueue() { return taskQueue; }

    /** @return zero-based queue index of the current task, or -1 before the first task starts. */
    public int getTaskIndex() { return taskIndex.get(); }

    /** @return total number of tasks in the queue. */
    public int getTaskTotal() { return taskQueue.size(); }

    /** @return display text for the queue. */
    public String getTaskQueueDisplayText() { return taskQueue.toDisplayText(); }

    /** @return queue failure policy. */
    public WindowTaskFailurePolicy getTaskQueueFailurePolicy() { return taskQueue.getFailurePolicy(); }

    /**
     * @return human-readable queue progress, such as {@code 1/2}, or {@code -} before start.
     */
    public String getTaskProgressText() {
        int total = getTaskTotal();
        int index = taskIndex.get();
        if (total <= 0 || index < 0) {
            return "-";
        }
        return (index + 1) + "/" + total;
    }

    /** @return cooperative stop token. */
    public TaskStopToken getStopToken() { return stopToken; }

    /** @return cooperative pause token. */
    public TaskPauseToken getPauseToken() { return pauseToken; }

    /** @return executor future for the running queue. */
    public Future<?> getFuture() { return future; }

    /** @return queue start timestamp. */
    public LocalDateTime getStartedAt() { return startedAt; }

    /** @return true when the future is done and no runner thread remains alive. */
    public boolean isDone() { return future == null || (future.isDone() && !isRunningThreadAlive()); }

    /** @return true while the future or runner thread is still active. */
    public boolean isRunning() { return future != null && (!future.isDone() || isRunningThreadAlive()); }

    /**
     * Update the current task object without changing queue index.
     *
     * @param nextTaskType current task type; null becomes UNKNOWN.
     * @param nextTask current task instance.
     */
    public void updateTask(TaskType nextTaskType, GameTask nextTask) {
        taskType.set(nextTaskType == null ? TaskType.UNKNOWN : nextTaskType);
        task.set(nextTask);
    }

    /**
     * Update current queue index and task object.
     *
     * @param nextTaskIndex zero-based queue index.
     * @param nextTaskType current task type.
     * @param nextTask current task instance.
     */
    public void updateTask(int nextTaskIndex, TaskType nextTaskType, GameTask nextTask) {
        taskIndex.set(nextTaskIndex);
        updateTask(nextTaskType, nextTask);
    }

    /**
     * Remember the actual runner thread for interruption and liveness checks.
     *
     * @param thread runner thread.
     */
    public void markRunningThread(Thread thread) {
        if (thread != null) {
            runningThread.set(thread);
        }
    }

    /**
     * Clear the runner-thread marker after the queue exits.
     */
    public void clearRunningThread() {
        runningThread.set(null);
    }

    /**
     * @return true when the remembered runner thread still exists and is alive.
     */
    public boolean isRunningThreadAlive() {
        Thread thread = runningThread.get();
        return thread != null && thread.isAlive();
    }

    /**
     * @return true when a pause has been requested for the current queue.
     */
    public boolean isPauseRequested() {
        return pauseToken != null && pauseToken.isPauseRequested();
    }

    /**
     * Request cooperative pause.
     *
     * @param reason diagnostic reason stored on the pause token.
     */
    public void requestPause(String reason) {
        if (pauseToken != null) {
            pauseToken.requestPause(reason);
        }
    }

    /**
     * Clear cooperative pause request.
     */
    public void resume() {
        if (pauseToken != null) {
            pauseToken.resume();
        }
    }

    /**
     * Request cooperative stop and interrupt the running thread.
     *
     * @param reason diagnostic stop reason.
     */
    public void requestStop(String reason) {
        if (stopToken != null) {
            stopToken.requestStop(reason);
        }
        if (pauseToken != null) {
            pauseToken.resume();
        }
        GameTask runningTask = task.get();
        if (runningTask != null) {
            runningTask.stop();
        }
        Thread thread = runningThread.get();
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * Cancel the backing future and unblock paused tasks.
     *
     * @param reason diagnostic cancellation reason.
     */
    public void forceCancel(String reason) {
        if (stopToken != null) {
            stopToken.requestStop(reason);
        }
        if (pauseToken != null) {
            pauseToken.resume();
        }
        if (future != null) {
            future.cancel(true);
        }
    }
}
