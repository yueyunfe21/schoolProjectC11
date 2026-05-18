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
        this.windowId = Objects.requireNonNull(windowId, "windowId must not be null");
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
        this.roleName = roleName;
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
        return role == WindowRole.LEADER;
    }

    public boolean isMember() {
        return role == WindowRole.MEMBER;
    }

    public WindowRuntimeStatus getStatus() {
        return status;
    }

    public void setStatus(WindowRuntimeStatus status) {
        this.status = status == null ? WindowRuntimeStatus.IDLE : status;
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
        this.lastMessage = message;
    }

    public LocalDateTime getLastFinishedAt() {
        return lastFinishedAt;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void markFinished(WindowRuntimeStatus status, String message) {
        this.status = status == null ? WindowRuntimeStatus.IDLE : status;
        this.lastFinishedAt = LocalDateTime.now();
        this.lastMessage = message;
    }
}
