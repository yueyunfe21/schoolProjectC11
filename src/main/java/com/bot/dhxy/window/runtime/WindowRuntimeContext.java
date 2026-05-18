package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;

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
    private volatile LocalDateTime lastStartedAt;
    private volatile LocalDateTime lastFinishedAt;
    private volatile String lastMessage;

    public WindowRuntimeContext(String windowId, GameContext gameContext) {
        String normalizedWindowId = normalizeWindowId(windowId);
        this.windowId = Objects.requireNonNull(normalizedWindowId, "windowId must not be blank");
        this.gameContext = Objects.requireNonNull(gameContext, "gameContext must not be null");
        this.gameState = gameContext.newState();
    }

    public String getWindowId() {
        return windowId;
    }

    public GameContext getGameContext() {
        return gameContext;
    }

    public GameContext.State getGameState() {
        return gameState;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = normalize(roleName);
    }

    public WindowRole getRole() {
        return role;
    }

    public void setRole(WindowRole role) {
        this.role = role == null ? WindowRole.UNKNOWN : role;
    }

    public void updateRole(WindowRole role, String roleName) {
        setRole(role);
        setRoleName(roleName);
    }

    public boolean isLeader() {
        return role.isLeader();
    }

    public boolean isMember() {
        return role.isMember();
    }

    public WindowRuntimeStatus getStatus() {
        return status;
    }

    public void setStatus(WindowRuntimeStatus status) {
        this.status = status == null ? WindowRuntimeStatus.IDLE : status;
    }

    public boolean isBusy() {
        return status != null && status.isBusy();
    }

    public TaskType getSelectedTaskType() {
        return selectedTaskType;
    }

    public void setSelectedTaskType(TaskType selectedTaskType) {
        this.selectedTaskType = selectedTaskType == null ? TaskType.UNKNOWN : selectedTaskType;
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

    public void markQueued(TaskType taskType) {
        this.selectedTaskType = taskType == null ? TaskType.UNKNOWN : taskType;
        this.status = WindowRuntimeStatus.QUEUED;
        this.lastMessage = null;
    }

    public void markStarted(TaskType taskType) {
        this.selectedTaskType = taskType == null ? TaskType.UNKNOWN : taskType;
        this.status = WindowRuntimeStatus.RUNNING;
        this.lastStartedAt = LocalDateTime.now();
        this.lastMessage = null;
    }

    public void markStopping(String message) {
        this.status = WindowRuntimeStatus.STOPPING;
        this.lastMessage = normalize(message);
    }

    public void markFinished(WindowRuntimeStatus status, String message) {
        this.status = status == null ? WindowRuntimeStatus.IDLE : status;
        this.lastFinishedAt = LocalDateTime.now();
        this.lastMessage = normalize(message);
    }

    public void markError(String message) {
        markFinished(WindowRuntimeStatus.ERROR, message);
    }

    public void resetRuntimeState() {
        this.status = WindowRuntimeStatus.IDLE;
        this.lastStartedAt = null;
        this.lastFinishedAt = null;
        this.lastMessage = null;
        this.gameState.resetRuntimeState();
    }

    public void applyRegistration(WindowRegistrationRequest request, boolean allowTaskChange) {
        if (request == null) {
            return;
        }
        updateRole(request.getRole(), request.getRoleName());
        if (allowTaskChange) {
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
}
