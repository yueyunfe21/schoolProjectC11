package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.DialogPreparationRequest;
import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogPreparationStatus;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
    private final AtomicReference<WindowPathingSnapshot> pathingSnapshot =
            new AtomicReference<>(WindowPathingSnapshot.idle());
    private final AtomicReference<DialogPreparationRequest> dialogPreparationRequest = new AtomicReference<>();
    private final AtomicReference<PreparedDialogAction> preparedDialogAction = new AtomicReference<>();
    private final AtomicReference<DialogPreparationStatus> dialogPreparationStatus =
            new AtomicReference<>(DialogPreparationStatus.none());

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
        WindowNativeBinding next = nativeBinding == null ? WindowNativeBinding.empty() : nativeBinding;
        if (!sameNativeBinding(this.nativeBinding, next)) {
            clearDialogPreparationRequest("native binding changed");
        }
        this.nativeBinding = next;
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

    public WindowPathingSnapshot getPathingSnapshot() { return pathingSnapshot.get(); }

    public DialogPreparationRequest getDialogPreparationRequest() { return dialogPreparationRequest.get(); }

    public PreparedDialogAction getPreparedDialogAction() { return preparedDialogAction.get(); }

    public DialogPreparationStatus getDialogPreparationStatus() { return dialogPreparationStatus.get(); }

    public void updateDialogPreparationRequest(DialogPreparationRequest request) {
        dialogPreparationRequest.set(request);
        if (request == null) {
            preparedDialogAction.set(null);
            dialogPreparationStatus.set(DialogPreparationStatus.none());
            return;
        }
        dialogPreparationStatus.set(DialogPreparationStatus.builder()
                .phase(DialogPreparationPhase.REQUESTED)
                .operation(request.getOperation())
                .targetKeyword(request.getTargetKeyword())
                .source(request.getSource())
                .requestCreatedAtMs(request.getCreatedAtMs())
                .build());
    }

    public void clearDialogPreparationRequest(String reason) {
        dialogPreparationRequest.set(null);
        preparedDialogAction.set(null);
        dialogPreparationStatus.set(DialogPreparationStatus.none());
    }

    public void markDialogPreparationStarted(DialogPreparationRequest request) {
        if (request == null || dialogPreparationRequest.get() != request) {
            return;
        }
        dialogPreparationStatus.set(DialogPreparationStatus.builder()
                .phase(DialogPreparationPhase.PREPARING)
                .operation(request.getOperation())
                .targetKeyword(request.getTargetKeyword())
                .source(request.getSource())
                .requestCreatedAtMs(request.getCreatedAtMs())
                .preparingStartedAtMs(System.currentTimeMillis())
                .build());
    }

    public void markDialogPreparationFailed(DialogPreparationRequest request, String reason) {
        if (request == null || dialogPreparationRequest.get() != request) {
            return;
        }
        dialogPreparationStatus.set(DialogPreparationStatus.builder()
                .phase(DialogPreparationPhase.FAILED)
                .operation(request.getOperation())
                .targetKeyword(request.getTargetKeyword())
                .source(request.getSource())
                .requestCreatedAtMs(request.getCreatedAtMs())
                .completedAtMs(System.currentTimeMillis())
                .failureReason(normalize(reason))
                .build());
    }

    /**
     * Store the latest prepared dialog click candidate for this bound window.
     *
     * <p>The watcher may update this without taking task ownership, but it must never click or
     * advance a task phase. Task code later decides whether the cached action still matches its
     * current operation before sending real input.</p>
     *
     * @param action prepared action for this window; null clears the cache.
     */
    public void updatePreparedDialogAction(PreparedDialogAction action) {
        preparedDialogAction.set(action);
        if (action != null) {
            dialogPreparationStatus.set(DialogPreparationStatus.builder()
                    .phase(DialogPreparationPhase.READY)
                    .operation(action.getOperation())
                    .targetKeyword(action.getTargetKeyword())
                    .source(action.getSource())
                    .preparingStartedAtMs(action.getPreparedAtMs())
                    .completedAtMs(System.currentTimeMillis())
                    .build());
        }
    }

    public void clearPreparedDialogAction(String reason) {
        preparedDialogAction.set(null);
        DialogPreparationStatus status = dialogPreparationStatus.get();
        if (status != null && status.getPhase() == DialogPreparationPhase.READY) {
            dialogPreparationStatus.set(DialogPreparationStatus.none());
        }
    }

    public Optional<WindowPathingIntent> getActivePathingIntent() {
        WindowPathingSnapshot snapshot = pathingSnapshot.get();
        if (snapshot == null || !snapshot.hasActiveIntent()) {
            return Optional.empty();
        }
        return Optional.of(snapshot.getIntent());
    }

    /**
     * Register a navigation/pathing target for the window-level background observer.
     *
     * <p>This method only records intent; it never sends input. The watcher thread uses it to refresh
     * cached map/coordinate state while the task turn is released, so later task phases can resume
     * without paying a fresh slow sync just to discover the window already arrived.</p>
     *
     * @param intent destination and diagnostic source for the active pathing operation.
     */
    public void markPathingStarted(WindowPathingIntent intent) {
        if (intent == null) {
            clearPathingSignal("null intent");
            return;
        }
        pathingSnapshot.set(WindowPathingSnapshot.builder()
                .state(WindowPathingState.ACTIVE)
                .intent(intent)
                .locationChangedAtMs(intent.getCreatedAtMs())
                .message("pathing intent registered")
                .build());
    }

    /**
     * Update the latest background observation for this window's active pathing intent.
     *
     * @param snapshot fresh observation from the window watcher. Null is ignored.
     */
    public void updatePathingSnapshot(WindowPathingSnapshot snapshot) {
        if (snapshot != null) {
            pathingSnapshot.set(snapshot);
        }
    }

    public void clearPathingSignal(String reason) {
        pathingSnapshot.set(WindowPathingSnapshot.builder()
                .state(WindowPathingState.NONE)
                .message(normalize(reason))
                .build());
    }

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

    /**
     * Update the user-facing message while preserving the current running/busy state.
     *
     * <p>This is for non-fatal conditions that need human attention but should not stop an idle
     * helper task, such as auto-battle panel refresh staying unverified for a long time.</p>
     */
    public void markRuntimeWarning(String message) {
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
        clearPathingSignal("runtime reset");
        clearDialogPreparationRequest("runtime reset");
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

    private static boolean sameNativeBinding(WindowNativeBinding left, WindowNativeBinding right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getNativeHandle(), right.getNativeHandle())
                && left.getX() == right.getX()
                && left.getY() == right.getY()
                && left.getWidth() == right.getWidth()
                && left.getHeight() == right.getHeight();
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
