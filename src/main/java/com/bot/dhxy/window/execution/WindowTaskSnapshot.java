package com.bot.dhxy.window.execution;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowNativeBinding;
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
    private final String playerName;
    private final String playerId;
    private final String serverName;
    private final WindowRole role;
    private final WindowRuntimeStatus status;
    private final TaskType selectedTaskType;
    private final TaskType runningTaskType;
    private final TaskType lastTaskType;
    private final TaskRunResult lastResult;
    private final boolean running;
    private final LocalDateTime taskStartedAt;
    private final LocalDateTime lastStartedAt;
    private final LocalDateTime lastFinishedAt;
    private final String lastMessage;
    private final String lastResultMessage;
    private final String lastQueueDisplayText;
    private final TaskRunResult lastQueueResult;
    private final String lastQueueMessage;
    private final WindowTaskFailurePolicy lastQueueFailurePolicy;
    private final WindowNativeBinding nativeBinding;
    private final String runningQueueDisplayText;
    private final String runningQueueProgressText;
    private final int runningQueueSize;
    private final WindowTaskFailurePolicy runningQueueFailurePolicy;
    private final boolean acceptingTaskQueue;

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
        this(windowId, roleName, role, status, selectedTaskType, runningTaskType, TaskType.UNKNOWN, null, running,
                taskStartedAt, lastStartedAt, lastFinishedAt, lastMessage, null, WindowNativeBinding.empty(),
                null, null, null, null, "-", "-", 0, null, !running,
                null, null, null);
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
        this(windowId, roleName, role, status, selectedTaskType, runningTaskType, TaskType.UNKNOWN, null, running,
                taskStartedAt, lastStartedAt, lastFinishedAt, lastMessage, null, nativeBinding,
                null, null, null, null, "-", "-", 0, null, !running,
                null, null, null);
    }

    public WindowTaskSnapshot(String windowId,
                              String roleName,
                              WindowRole role,
                              WindowRuntimeStatus status,
                              TaskType selectedTaskType,
                              TaskType runningTaskType,
                              TaskType lastTaskType,
                              TaskRunResult lastResult,
                              boolean running,
                              LocalDateTime taskStartedAt,
                              LocalDateTime lastStartedAt,
                              LocalDateTime lastFinishedAt,
                              String lastMessage,
                              String lastResultMessage,
                              WindowNativeBinding nativeBinding) {
        this(windowId, roleName, role, status, selectedTaskType, runningTaskType, lastTaskType, lastResult, running,
                taskStartedAt, lastStartedAt, lastFinishedAt, lastMessage, lastResultMessage, nativeBinding,
                null, null, null, null, "-", "-", 0, null, !running,
                null, null, null);
    }

    public WindowTaskSnapshot(String windowId,
                              String roleName,
                              WindowRole role,
                              WindowRuntimeStatus status,
                              TaskType selectedTaskType,
                              TaskType runningTaskType,
                              TaskType lastTaskType,
                              TaskRunResult lastResult,
                              boolean running,
                              LocalDateTime taskStartedAt,
                              LocalDateTime lastStartedAt,
                              LocalDateTime lastFinishedAt,
                              String lastMessage,
                              String lastResultMessage,
                              WindowNativeBinding nativeBinding,
                              String runningQueueDisplayText,
                              String runningQueueProgressText,
                              int runningQueueSize) {
        this(windowId, roleName, role, status, selectedTaskType, runningTaskType, lastTaskType, lastResult, running,
                taskStartedAt, lastStartedAt, lastFinishedAt, lastMessage, lastResultMessage, nativeBinding,
                null, null, null, null, runningQueueDisplayText, runningQueueProgressText, runningQueueSize, null, !running,
                null, null, null);
    }

    public WindowTaskSnapshot(String windowId,
                              String roleName,
                              WindowRole role,
                              WindowRuntimeStatus status,
                              TaskType selectedTaskType,
                              TaskType runningTaskType,
                              TaskType lastTaskType,
                              TaskRunResult lastResult,
                              boolean running,
                              LocalDateTime taskStartedAt,
                              LocalDateTime lastStartedAt,
                              LocalDateTime lastFinishedAt,
                              String lastMessage,
                              String lastResultMessage,
                              WindowNativeBinding nativeBinding,
                              String lastQueueDisplayText,
                              TaskRunResult lastQueueResult,
                              String lastQueueMessage,
                              WindowTaskFailurePolicy lastQueueFailurePolicy,
                              String runningQueueDisplayText,
                              String runningQueueProgressText,
                              int runningQueueSize,
                              WindowTaskFailurePolicy runningQueueFailurePolicy,
                              boolean acceptingTaskQueue) {
        this(windowId, roleName, role, status, selectedTaskType, runningTaskType, lastTaskType, lastResult, running,
                taskStartedAt, lastStartedAt, lastFinishedAt, lastMessage, lastResultMessage, nativeBinding,
                lastQueueDisplayText, lastQueueResult, lastQueueMessage, lastQueueFailurePolicy,
                runningQueueDisplayText, runningQueueProgressText, runningQueueSize, runningQueueFailurePolicy,
                acceptingTaskQueue, null, null, null);
    }

    public WindowTaskSnapshot(String windowId,
                              String roleName,
                              WindowRole role,
                              WindowRuntimeStatus status,
                              TaskType selectedTaskType,
                              TaskType runningTaskType,
                              TaskType lastTaskType,
                              TaskRunResult lastResult,
                              boolean running,
                              LocalDateTime taskStartedAt,
                              LocalDateTime lastStartedAt,
                              LocalDateTime lastFinishedAt,
                              String lastMessage,
                              String lastResultMessage,
                              WindowNativeBinding nativeBinding,
                              String lastQueueDisplayText,
                              TaskRunResult lastQueueResult,
                              String lastQueueMessage,
                              WindowTaskFailurePolicy lastQueueFailurePolicy,
                              String runningQueueDisplayText,
                              String runningQueueProgressText,
                              int runningQueueSize,
                              WindowTaskFailurePolicy runningQueueFailurePolicy,
                              boolean acceptingTaskQueue,
                              String playerName,
                              String playerId,
                              String serverName) {
        this.windowId = windowId;
        this.roleName = roleName;
        this.playerName = normalizeBlank(playerName);
        this.playerId = normalizeBlank(playerId);
        this.serverName = normalizeBlank(serverName);
        this.role = role == null ? WindowRole.UNKNOWN : role;
        this.status = status == null ? WindowRuntimeStatus.IDLE : status;
        this.selectedTaskType = selectedTaskType == null ? TaskType.UNKNOWN : selectedTaskType;
        this.runningTaskType = runningTaskType == null ? TaskType.UNKNOWN : runningTaskType;
        this.lastTaskType = lastTaskType == null ? TaskType.UNKNOWN : lastTaskType;
        this.lastResult = lastResult;
        this.running = running;
        this.taskStartedAt = taskStartedAt;
        this.lastStartedAt = lastStartedAt;
        this.lastFinishedAt = lastFinishedAt;
        this.lastMessage = lastMessage;
        this.lastResultMessage = lastResultMessage;
        this.lastQueueDisplayText = lastQueueDisplayText == null || lastQueueDisplayText.isBlank() ? "-" : lastQueueDisplayText;
        this.lastQueueResult = lastQueueResult;
        this.lastQueueMessage = lastQueueMessage;
        this.lastQueueFailurePolicy = lastQueueFailurePolicy;
        this.nativeBinding = nativeBinding == null ? WindowNativeBinding.empty() : nativeBinding;
        this.runningQueueDisplayText = runningQueueDisplayText == null || runningQueueDisplayText.isBlank() ? "-" : runningQueueDisplayText;
        this.runningQueueProgressText = runningQueueProgressText == null || runningQueueProgressText.isBlank() ? "-" : runningQueueProgressText;
        this.runningQueueSize = Math.max(0, runningQueueSize);
        this.runningQueueFailurePolicy = runningQueueFailurePolicy;
        this.acceptingTaskQueue = acceptingTaskQueue;
    }

    public String getWindowId() { return windowId; }

    public String getRoleName() { return roleName; }

    public String getPlayerName() { return playerName; }

    public String getPlayerId() { return playerId; }

    public String getServerName() { return serverName; }

    public WindowRole getRole() { return role; }

    public WindowRuntimeStatus getStatus() { return status; }

    public TaskType getSelectedTaskType() { return selectedTaskType; }

    public TaskType getRunningTaskType() { return runningTaskType; }

    public TaskType getLastTaskType() { return lastTaskType; }

    public TaskRunResult getLastResult() { return lastResult; }

    public boolean isRunning() { return running; }

    public boolean isBusy() { return running || status.isBusy(); }

    public LocalDateTime getTaskStartedAt() { return taskStartedAt; }

    public LocalDateTime getLastStartedAt() { return lastStartedAt; }

    public LocalDateTime getLastFinishedAt() { return lastFinishedAt; }

    public String getLastMessage() { return lastMessage; }

    public String getLastResultMessage() { return lastResultMessage; }

    public String getLastQueueDisplayText() { return lastQueueDisplayText; }

    public TaskRunResult getLastQueueResult() { return lastQueueResult; }

    public String getLastQueueMessage() { return lastQueueMessage; }

    public WindowTaskFailurePolicy getLastQueueFailurePolicy() { return lastQueueFailurePolicy; }

    public WindowNativeBinding getNativeBinding() { return nativeBinding; }

    public String getRunningQueueDisplayText() { return runningQueueDisplayText; }

    public String getRunningQueueProgressText() { return runningQueueProgressText; }

    public int getRunningQueueSize() { return runningQueueSize; }

    public WindowTaskFailurePolicy getRunningQueueFailurePolicy() { return runningQueueFailurePolicy; }

    public boolean isAcceptingTaskQueue() { return acceptingTaskQueue; }

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

    public String getLastTaskDisplayName() { return lastTaskType.getDisplayName(); }

    public String getLastResultDisplayName() { return lastResult == null ? "-" : lastResult.name(); }

    public String getLastQueueResultDisplayName() { return lastQueueResult == null ? "-" : lastQueueResult.name(); }

    public String getLastQueueFailurePolicyDisplayName() {
        return lastQueueFailurePolicy == null ? "-" : lastQueueFailurePolicy.name();
    }

    public String getRunningQueueFailurePolicyDisplayName() {
        return runningQueueFailurePolicy == null ? "-" : runningQueueFailurePolicy.name();
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
