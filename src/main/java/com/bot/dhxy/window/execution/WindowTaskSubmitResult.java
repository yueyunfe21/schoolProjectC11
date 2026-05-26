package com.bot.dhxy.window.execution;

import com.bot.dhxy.task.model.TaskType;

/**
 * 单个窗口提交任务的诊断结果。
 */
public class WindowTaskSubmitResult {

    private final String windowId;
    private final WindowTaskQueue taskQueue;
    private final WindowTaskSubmitStatus status;
    private final boolean success;
    private final String message;

    private WindowTaskSubmitResult(String windowId, TaskType taskType, boolean success, String message) {
        this(windowId, WindowTaskQueue.single(taskType), inferStatus(success), success, message);
    }

    private WindowTaskSubmitResult(String windowId, WindowTaskQueue taskQueue, boolean success, String message) {
        this(windowId, taskQueue, inferStatus(success), success, message);
    }

    private WindowTaskSubmitResult(String windowId,
                                   WindowTaskQueue taskQueue,
                                   WindowTaskSubmitStatus status,
                                   boolean success,
                                   String message) {
        this.windowId = windowId;
        this.taskQueue = taskQueue == null ? WindowTaskQueue.empty() : taskQueue;
        this.status = status == null ? inferStatus(success) : status;
        this.success = success;
        this.message = message == null || message.isBlank() ? "-" : message;
    }

    public static WindowTaskSubmitResult success(String windowId, TaskType taskType, String message) {
        return new WindowTaskSubmitResult(windowId, taskType, true, message);
    }

    public static WindowTaskSubmitResult success(String windowId, WindowTaskQueue taskQueue, String message) {
        return new WindowTaskSubmitResult(windowId, taskQueue, WindowTaskSubmitStatus.ACCEPTED, true, message);
    }

    public static WindowTaskSubmitResult failed(String windowId, TaskType taskType, String message) {
        return new WindowTaskSubmitResult(windowId, taskType, false, message);
    }

    public static WindowTaskSubmitResult failed(String windowId, WindowTaskQueue taskQueue, String message) {
        return failed(windowId, taskQueue, WindowTaskSubmitStatus.SUBMIT_REJECTED, message);
    }

    public static WindowTaskSubmitResult failed(String windowId,
                                                WindowTaskQueue taskQueue,
                                                WindowTaskSubmitStatus status,
                                                String message) {
        return new WindowTaskSubmitResult(windowId, taskQueue, status, false, message);
    }

    public String getWindowId() {
        return windowId;
    }

    public TaskType getTaskType() {
        return taskQueue.firstTaskType();
    }

    public WindowTaskQueue getTaskQueue() {
        return taskQueue;
    }

    public WindowTaskSubmitStatus getStatus() {
        return status;
    }

    public int getTaskQueueSize() {
        return taskQueue.size();
    }

    public String getTaskQueueDisplayText() {
        return taskQueue.toDisplayText();
    }

    public WindowTaskFailurePolicy getTaskQueueFailurePolicy() {
        return taskQueue.getFailurePolicy();
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailed() {
        return !success;
    }

    public String getMessage() {
        return message;
    }

    public String getTaskDisplayName() {
        return getTaskType().getDisplayName();
    }

    public String getStatusDisplayName() {
        return status == null ? "-" : status.name();
    }

    private static WindowTaskSubmitStatus inferStatus(boolean success) {
        return success ? WindowTaskSubmitStatus.ACCEPTED : WindowTaskSubmitStatus.SUBMIT_REJECTED;
    }
}
