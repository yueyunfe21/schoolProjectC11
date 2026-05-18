package com.bot.dhxy.runner;

import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.model.TaskType;

import java.time.LocalDateTime;
import java.util.concurrent.Future;

/**
 * 单个窗口当前正在执行的任务句柄。
 */
public class RunningTaskHandle {

    private final String windowId;
    private final TaskType taskType;
    private final GameTask task;
    private final TaskStopToken stopToken;
    private final Future<?> future;
    private final LocalDateTime startedAt;

    public RunningTaskHandle(String windowId,
                             TaskType taskType,
                             GameTask task,
                             TaskStopToken stopToken,
                             Future<?> future) {
        this.windowId = windowId;
        this.taskType = taskType;
        this.task = task;
        this.stopToken = stopToken;
        this.future = future;
        this.startedAt = LocalDateTime.now();
    }

    public String getWindowId() {
        return windowId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public GameTask getTask() {
        return task;
    }

    public TaskStopToken getStopToken() {
        return stopToken;
    }

    public Future<?> getFuture() {
        return future;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void requestStop(String reason) {
        if (stopToken != null) {
            stopToken.requestStop(reason);
        }
        if (task != null) {
            task.stop();
        }
        if (future != null) {
            future.cancel(true);
        }
    }
}
