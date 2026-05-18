package com.bot.dhxy.window.runner;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;

import java.time.LocalDateTime;

/**
 * 单个窗口当前任务状态快照。
 *
 * 这个对象主要给 UI / 日志 / 调试面板读取，不暴露内部 Future、线程、任务对象。
 */
public class WindowTaskSnapshot {

    private final String windowId;
    private final String roleName;
    private final WindowRole role;
    private final WindowRuntimeStatus status;
    private final TaskType selectedTaskType;
    private final TaskType runningTaskType;
    private final boolean running;
    private final LocalDateTime taskStartedAt;
    private final LocalDateTime lastStartedAt;
    private final LocalDateTime lastFinishedAt;
    private final String lastMessage;

    public WindowTaskSnapshot(String windowId,
                              String roleName,
                              WindowRole role,
                              WindowRuntimeStatus status,
                              TaskType selectedTaskType,
                              TaskType runningTaskType,
                              boolean running,
                              LocalDateTime taskStartedAt,
                              LocalDateTime lastStartedAt,
                              LocalDateTime lastFinishedAt,
                              String lastMessage) {
        this.windowId = windowId;
        this.roleName = roleName;
        this.role = role;
        this.status = status;
        this.selectedTaskType = selectedTaskType;
        this.runningTaskType = runningTaskType;
        this.running = running;
        this.taskStartedAt = taskStartedAt;
        this.lastStartedAt = lastStartedAt;
        this.lastFinishedAt = lastFinishedAt;
        this.lastMessage = lastMessage;
    }

    public String getWindowId() {
        return windowId;
    }

    public String getRoleName() {
        return roleName;
    }

    public WindowRole getRole() {
        return role;
    }

    public WindowRuntimeStatus getStatus() {
        return status;
    }

    public TaskType getSelectedTaskType() {
        return selectedTaskType;
    }

    public TaskType getRunningTaskType() {
        return runningTaskType;
    }

    public boolean isRunning() {
        return running;
    }

    public LocalDateTime getTaskStartedAt() {
        return taskStartedAt;
    }

    public LocalDateTime getLastStartedAt() {
        return lastStartedAt;
    }

    public LocalDateTime getLastFinishedAt() {
        return lastFinishedAt;
    }

    public String getLastMessage() {
        return lastMessage;
    }
}
