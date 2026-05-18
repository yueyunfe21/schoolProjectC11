package com.bot.dhxy.window.execution;

import com.bot.dhxy.task.model.TaskType;

/**
 * 单个窗口提交任务的诊断结果。
 */
public class WindowTaskSubmitResult {

    private final String windowId;
    private final TaskType taskType;
    private final boolean success;
    private final String message;

    private WindowTaskSubmitResult(String windowId, TaskType taskType, boolean success, String message) {
        this.windowId = windowId;
        this.taskType = taskType == null ? TaskType.UNKNOWN : taskType;
        this.success = success;
        this.message = message == null || message.isBlank() ? "-" : message;
    }

    public static WindowTaskSubmitResult success(String windowId, TaskType taskType, String message) {
        return new WindowTaskSubmitResult(windowId, taskType, true, message);
    }

    public static WindowTaskSubmitResult failed(String windowId, TaskType taskType, String message) {
        return new WindowTaskSubmitResult(windowId, taskType, false, message);
    }

    public String getWindowId() {
        return windowId;
    }

    public TaskType getTaskType() {
        return taskType;
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
        return taskType.getDisplayName();
    }
}
