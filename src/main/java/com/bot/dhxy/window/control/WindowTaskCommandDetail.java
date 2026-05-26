package com.bot.dhxy.window.control;

import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;
import com.bot.dhxy.window.execution.WindowTaskSubmitResult;
import com.bot.dhxy.window.execution.WindowTaskSubmitStatus;

/**
 * 单个窗口在一次批量命令中的执行明细。
 */
public class WindowTaskCommandDetail {

    private final String windowId;
    private final boolean success;
    private final String message;
    private final WindowTaskSubmitStatus submitStatus;
    private final String taskQueueDisplayText;
    private final WindowTaskFailurePolicy taskQueueFailurePolicy;

    private WindowTaskCommandDetail(String windowId, boolean success, String message) {
        this(windowId, success, message, null, null, null);
    }

    private WindowTaskCommandDetail(String windowId,
                                    boolean success,
                                    String message,
                                    WindowTaskSubmitStatus submitStatus,
                                    String taskQueueDisplayText,
                                    WindowTaskFailurePolicy taskQueueFailurePolicy) {
        this.windowId = windowId;
        this.success = success;
        this.message = message == null ? "" : message;
        this.submitStatus = submitStatus;
        this.taskQueueDisplayText = taskQueueDisplayText == null || taskQueueDisplayText.isBlank() ? "-" : taskQueueDisplayText;
        this.taskQueueFailurePolicy = taskQueueFailurePolicy;
    }

    public static WindowTaskCommandDetail success(String windowId, String message) {
        return new WindowTaskCommandDetail(windowId, true, message);
    }

    public static WindowTaskCommandDetail failed(String windowId, String message) {
        return new WindowTaskCommandDetail(windowId, false, message);
    }

    public static WindowTaskCommandDetail fromSubmitResult(WindowTaskSubmitResult submitResult, String message) {
        if (submitResult == null) {
            return failed(null, message);
        }
        return new WindowTaskCommandDetail(
                submitResult.getWindowId(),
                submitResult.isSuccess(),
                message,
                submitResult.getStatus(),
                submitResult.getTaskQueueDisplayText(),
                submitResult.getTaskQueueFailurePolicy()
        );
    }

    public String getWindowId() { return windowId; }

    public boolean isSuccess() { return success; }

    public boolean isFailed() { return !success; }

    public String getMessage() { return message; }

    public WindowTaskSubmitStatus getSubmitStatus() { return submitStatus; }

    public String getSubmitStatusDisplayName() { return submitStatus == null ? "-" : submitStatus.name(); }

    public String getTaskQueueDisplayText() { return taskQueueDisplayText; }

    public WindowTaskFailurePolicy getTaskQueueFailurePolicy() { return taskQueueFailurePolicy; }

    public String getTaskQueueFailurePolicyDisplayName() {
        return taskQueueFailurePolicy == null ? "-" : taskQueueFailurePolicy.name();
    }
}
