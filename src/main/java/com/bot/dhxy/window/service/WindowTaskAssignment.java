package com.bot.dhxy.window.service;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRole;

/**
 * 单个窗口的一次任务分配结果。
 */
public class WindowTaskAssignment {

    private final String windowId;
    private final WindowRole role;
    private final TaskType taskType;
    private final boolean executable;
    private final String reason;

    public WindowTaskAssignment(String windowId,
                                WindowRole role,
                                TaskType taskType,
                                boolean executable,
                                String reason) {
        this.windowId = windowId;
        this.role = role == null ? WindowRole.UNKNOWN : role;
        this.taskType = taskType == null ? TaskType.UNKNOWN : taskType;
        this.executable = executable;
        this.reason = reason;
    }

    public static WindowTaskAssignment executable(String windowId, WindowRole role, TaskType taskType) {
        return new WindowTaskAssignment(windowId, role, taskType, true, "OK");
    }

    public static WindowTaskAssignment skipped(String windowId, WindowRole role, TaskType taskType, String reason) {
        return new WindowTaskAssignment(windowId, role, taskType, false, reason);
    }

    public String getWindowId() {
        return windowId;
    }

    public WindowRole getRole() {
        return role;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public boolean isExecutable() {
        return executable;
    }

    public String getReason() {
        return reason;
    }
}
