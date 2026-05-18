package com.bot.dhxy.window.runner;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowNativeBinding;

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
    private final WindowNativeBinding nativeBinding;

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
        this(windowId, roleName, role, status, selectedTaskType, runningTaskType, running,
                taskStartedAt, lastStartedAt, lastFinishedAt, lastMessage, WindowNativeBinding.empty());
    }

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
                              String lastMessage,
                              WindowNativeBinding nativeBinding) {
        this.windowId = windowId;
        this.roleName = roleName;
        this.role = role == null ? WindowRole.UNKNOWN : role;
        this.status = status == null ? WindowRuntimeStatus.IDLE : status;
        this.selectedTaskType = selectedTaskType == null ? TaskType.UNKNOWN : selectedTaskType;
        this.runningTaskType = runningTaskType == null ? TaskType.UNKNOWN : runningTaskType;
        this.running = running;
        this.taskStartedAt = taskStartedAt;
        this.lastStartedAt = lastStartedAt;
        this.lastFinishedAt = lastFinishedAt;
        this.lastMessage = lastMessage;
        this.nativeBinding = nativeBinding == null ? WindowNativeBinding.empty() : nativeBinding;
    }

    public String getWindowId() { return windowId; }

    public String getRoleName() { return roleName; }

    public WindowRole getRole() { return role; }

    public WindowRuntimeStatus getStatus() { return status; }

    public TaskType getSelectedTaskType() { return selectedTaskType; }

    public TaskType getRunningTaskType() { return runningTaskType; }

    public boolean isRunning() { return running; }

    public boolean isBusy() { return running || status.isBusy(); }

    public LocalDateTime getTaskStartedAt() { return taskStartedAt; }

    public LocalDateTime getLastStartedAt() { return lastStartedAt; }

    public LocalDateTime getLastFinishedAt() { return lastFinishedAt; }

    public String getLastMessage() { return lastMessage; }

    public WindowNativeBinding getNativeBinding() { return nativeBinding; }

    public String getNativeHandle() { return nativeBinding.getNativeHandle(); }

    public String getNativeTitle() { return nativeBinding.getTitle(); }

    public String getNativeClassName() { return nativeBinding.getClassName(); }

    public long getNativeProcessId() { return nativeBinding.getProcessId(); }

    public String getGeometryText() { return nativeBinding.getGeometryText(); }

    public boolean hasNativeBinding() { return nativeBinding.hasNativeHandle(); }

    public String getRoleDisplayName() { return role.getDisplayName(); }

    public String getStatusDisplayName() { return status.getDisplayName(); }

    public String getSelectedTaskDisplayName() { return selectedTaskType.getDisplayName(); }

    public String getRunningTaskDisplayName() { return runningTaskType.getDisplayName(); }
}
