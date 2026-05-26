package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 单个游戏窗口的运行上下文。
 *
 * 一个独立游戏窗口对应一个 WindowRuntimeContext。
 * 这里保存窗口元信息、窗口级 GameContext.State、当前任务状态和最近一次运行信息。
 */
public class WindowRuntimeContext {

    private final String windowId;
    private final GameContext gameContext;
    private final GameContext.State gameState;

    private volatile String roleName;
    private volatile WindowRole role = WindowRole.UNKNOWN;
    private volatile WindowRuntimeStatus status = WindowRuntimeStatus.IDLE;
    private volatile TaskType selectedTaskType = TaskType.UNKNOWN;
    private volatile WindowNativeBinding nativeBinding = WindowNativeBinding.empty();
    private volatile LocalDateTime lastStartedAt;
    private volatile LocalDateTime lastFinishedAt;
    private volatile String lastMessage;
    private volatile TaskType lastTaskType = TaskType.UNKNOWN;
    private volatile TaskRunResult lastResult;
    private volatile String lastResultMessage;
    private volatile String lastQueueDisplayText;
    private volatile TaskRunResult lastQueueResult;
    private volatile String lastQueueMessage;
    private volatile WindowTaskFailurePolicy lastQueueFailurePolicy;

    public WindowRuntimeContext(String windowId, GameContext gameContext) {
        String normalizedWindowId = normalizeWindowId(windowId);
        this.windowId = Objects.requireNonNull(normalizedWindowId, "windowId must not be blank");
        this.gameContext = Objects.requireNonNull(gameContext, "gameContext must not be null");
        this.gameState = gameContext.newState();
    }

    public String getWindowId() { return windowId; }

    public GameContext getGameContext() { return gameContext; }

    public GameContext.State getGameState() { return gameState; }

    public String getRoleName() { return roleName; }

    public void setRoleName(String roleName) { this.roleName = normalize(roleName); }

    public WindowRole getRole() { return role; }

    public void setRole(WindowRole role) { this.role = role == null ? WindowRole.UNKNOWN : role; }

    public void updateRole(WindowRole role, String roleName) {
        setRole(role);
        setRoleName(roleName);
    }

    public boolean isLeader() { return role.isLeader(); }

    public boolean isMember() { return role.isMember(); }

    public WindowRuntimeStatus getStatus() { return status; }

    public void setStatus(WindowRuntimeStatus status) { this.status = status == null ? WindowRuntimeStatus.IDLE : status; }

    public boolean isBusy() { return status != null && status.isBusy(); }

    public TaskType getSelectedTaskType() { return selectedTaskType; }

    public void setSelectedTaskType(TaskType selectedTaskType) {
        this.selectedTaskType = selectedTaskType == null ? TaskType.UNKNOWN : selectedTaskType;
    }

    public WindowNativeBinding getNativeBinding() { return nativeBinding; }

    public void setNativeBinding(WindowNativeBinding nativeBinding) {
        this.nativeBinding = nativeBinding == null ? WindowNativeBinding.empty() : nativeBinding;
    }

    public boolean hasNativeBinding() {
        return nativeBinding != null && nativeBinding.hasNativeHandle();
    }

    public LocalDateTime getLastStartedAt() { return lastStartedAt; }

    public LocalDateTime getLastFinishedAt() { return lastFinishedAt; }

    public String getLastMessage() { return lastMessage; }

    public TaskType getLastTaskType() { return lastTaskType; }

    public TaskRunResult getLastResult() { return lastResult; }

    public String getLastResultMessage() { return lastResultMessage; }

    public String getLastQueueDisplayText() { return lastQueueDisplayText; }

    public TaskRunResult getLastQueueResult() { return lastQueueResult; }

    public String getLastQueueMessage() { return lastQueueMessage; }

    public WindowTaskFailurePolicy getLastQueueFailurePolicy() { return lastQueueFailurePolicy; }

    public void markQueued(TaskType taskType) {
        this.lastTaskType = resolveTaskForRuntimeEvent(taskType);
        this.status = WindowRuntimeStatus.QUEUED;
        this.lastMessage = "任务已排队：" + this.lastTaskType.getDisplayName();
        this.lastResultMessage = null;
    }

    public void markStarted(TaskType taskType) {
        this.lastTaskType = resolveTaskForRuntimeEvent(taskType);
        this.status = WindowRuntimeStatus.RUNNING;
        this.lastStartedAt = LocalDateTime.now();
        this.lastMessage = "任务开始：" + this.lastTaskType.getDisplayName();
        this.lastResult = null;
        this.lastResultMessage = null;
    }

    public void markStopping(String message) {
        this.status = WindowRuntimeStatus.STOPPING;
        this.lastMessage = normalize(message);
    }

    public void markPauseRequested(String message) {
        this.status = WindowRuntimeStatus.PAUSED;
        this.lastMessage = normalize(message);
    }

    public void markResumed(String message) {
        this.status = WindowRuntimeStatus.RUNNING;
        this.lastMessage = normalize(message);
    }

    public void markFinished(WindowRuntimeStatus status, String message) {
        markFinished(status, null, null, message);
    }

    public void markFinished(WindowRuntimeStatus status, TaskType taskType, TaskRunResult result, String message) {
        this.status = status == null ? WindowRuntimeStatus.IDLE : status;
        this.lastFinishedAt = LocalDateTime.now();
        if (taskType != null && taskType != TaskType.UNKNOWN) {
            this.lastTaskType = taskType;
        }
        this.lastResult = result;
        this.lastMessage = normalize(message);
        this.lastResultMessage = normalize(message);
    }

    public void markQueueFinished(WindowRuntimeStatus status,
                                  TaskRunResult result,
                                  String queueDisplayText,
                                  WindowTaskFailurePolicy failurePolicy,
                                  String message) {
        this.status = status == null ? WindowRuntimeStatus.IDLE : status;
        this.lastFinishedAt = LocalDateTime.now();
        this.lastMessage = normalize(message);
        this.lastQueueDisplayText = normalize(queueDisplayText);
        this.lastQueueResult = result;
        this.lastQueueMessage = normalize(message);
        this.lastQueueFailurePolicy = failurePolicy;
    }

    public void markError(String message) { markFinished(WindowRuntimeStatus.ERROR, null, TaskRunResult.FAILED, message); }

    /**
     * Mark an already-terminal window as explicitly stopped by the user.
     *
     * <p>This is used when the UI sends a stop command after a task has already failed and no
     * runner thread is active anymore. The window-level status should stop showing "异常" once the
     * user has acknowledged/stopped it, but the last task result/message are preserved so the detail
     * panel can still explain the original failure.</p>
     *
     * @param message user-facing status message for the stop acknowledgement.
     */
    public void markStoppedAfterTerminalStop(String message) {
        this.status = WindowRuntimeStatus.STOPPED;
        this.lastMessage = normalize(message);
        if (this.lastResult == null) {
            this.lastResult = TaskRunResult.STOPPED;
            this.lastResultMessage = normalize(message);
        }
    }

    public void resetRuntimeState() {
        this.status = WindowRuntimeStatus.IDLE;
        this.lastStartedAt = null;
        this.lastFinishedAt = null;
        this.lastMessage = null;
        this.lastTaskType = TaskType.UNKNOWN;
        this.lastResult = null;
        this.lastResultMessage = null;
        this.lastQueueDisplayText = null;
        this.lastQueueResult = null;
        this.lastQueueMessage = null;
        this.lastQueueFailurePolicy = null;
        this.gameState.resetRuntimeState();
    }

    public void applyRegistration(WindowRegistrationRequest request, boolean allowTaskChange) {
        if (request == null) {
            return;
        }
        if ((request.getRole() != null && request.getRole() != WindowRole.UNKNOWN)
                || request.getRoleName() != null) {
            updateRole(request.getRole(), request.getRoleName());
        }
        if (request.hasNativeBinding()) {
            setNativeBinding(request.getNativeBinding());
        }
        if (allowTaskChange && request.hasSelectedTask()) {
            setSelectedTaskType(request.getSelectedTaskType());
        }
    }

    private static String normalizeWindowId(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TaskType resolveTaskForRuntimeEvent(TaskType taskType) {
        if (taskType != null && taskType != TaskType.UNKNOWN) {
            return taskType;
        }
        return selectedTaskType == null ? TaskType.UNKNOWN : selectedTaskType;
    }
}
