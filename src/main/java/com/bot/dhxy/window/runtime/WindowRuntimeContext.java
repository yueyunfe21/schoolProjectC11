package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.DialogPreparationRequest;
import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogPreparationStatus;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.navigation.PendingTransferChoiceMemory;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;
import com.bot.dhxy.service.dialog.DialogOperation;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
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
    private final AtomicReference<WindowDialogSnapshot> visibleDialogSnapshot = new AtomicReference<>();
    private final AtomicReference<WindowDialogInterest> dialogInterest = new AtomicReference<>();
    private final AtomicReference<DialogPreparationRequest> dialogPreparationRequest = new AtomicReference<>();
    private final AtomicReference<PreparedDialogAction> preparedDialogAction = new AtomicReference<>();
    private final AtomicReference<PendingTransferChoiceMemory> pendingTransferChoiceMemory = new AtomicReference<>();
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
            clearVisibleDialogSnapshot("native binding changed");
            clearDialogInterest("native binding changed");
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

    public Optional<WindowDialogSnapshot> getVisibleDialogSnapshot() {
        return Optional.ofNullable(visibleDialogSnapshot.get());
    }

    /**
     * Return the latest visible dialog observation only while it is still fresh.
     *
     * @param maxAgeMs maximum accepted age in milliseconds; non-positive values reject all cached
     *                 observations.
     * @return fresh visible dialog snapshot, or empty when absent/stale.
     */
    public Optional<WindowDialogSnapshot> getVisibleDialogSnapshot(long maxAgeMs) {
        WindowDialogSnapshot snapshot = visibleDialogSnapshot.get();
        if (snapshot == null || maxAgeMs <= 0L) {
            return Optional.empty();
        }
        long ageMs = Math.max(0L, System.currentTimeMillis() - snapshot.getDetectedAtMs());
        if (ageMs > maxAgeMs) {
            clearVisibleDialogSnapshot("stale:" + ageMs + "ms");
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    public Optional<WindowDialogInterest> getDialogInterest() {
        WindowDialogInterest interest = dialogInterest.get();
        if (interest == null) {
            return Optional.empty();
        }
        if (interest.isExpired(System.currentTimeMillis())) {
            clearDialogInterest("expired");
            return Optional.empty();
        }
        return Optional.of(interest);
    }

    public DialogPreparationRequest getDialogPreparationRequest() { return dialogPreparationRequest.get(); }

    public PreparedDialogAction getPreparedDialogAction() { return preparedDialogAction.get(); }

    public PendingTransferChoiceMemory getPendingTransferChoiceMemory() { return pendingTransferChoiceMemory.get(); }

    public DialogPreparationStatus getDialogPreparationStatus() { return dialogPreparationStatus.get(); }

    /**
     * Store the latest dialog shape observed by this window's background watcher.
     *
     * @param snapshot visible dialog fact for this bound window; null is ignored.
     * @param reason diagnostic reason describing why the snapshot was written.
     */
    public void updateVisibleDialogSnapshot(WindowDialogSnapshot snapshot, String reason) {
        if (snapshot == null) {
            return;
        }
        visibleDialogSnapshot.set(snapshot);
        long now = System.currentTimeMillis();
        WindowPathingIntent activeIntent = getActivePathingIntent().orElse(null);
        log.info("[latency] event=window.dialog.visible.update windowId={} hwnd={} type={} source={} reason={} detectedAgeMs={} rect={} provider={} activeIntentId={} activeIntentTarget={} activeIntentSource={} activeIntentAgeMs={}",
                windowId, snapshot.getHwnd(), snapshot.getType(), normalize(snapshot.getSource()),
                normalize(reason), ageMs(now, snapshot.getDetectedAtMs()), formatRect(snapshot.getDialogRect()),
                normalize(snapshot.getCaptureProvider()),
                activeIntent == null ? null : activeIntent.getIntentId(),
                activeIntent == null ? null : activeIntent.getTargetMapName(),
                activeIntent == null ? null : activeIntent.getSource(),
                activeIntent == null ? -1L : ageMs(now, activeIntent.getCreatedAtMs()));
    }

    /**
     * Clear the latest visible dialog observation without touching prepared click actions.
     *
     * @param reason diagnostic reason describing why the observation was cleared.
     */
    public void clearVisibleDialogSnapshot(String reason) {
        WindowDialogSnapshot cleared = visibleDialogSnapshot.getAndSet(null);
        if (cleared != null) {
            long now = System.currentTimeMillis();
            log.info("[latency] event=window.dialog.visible.clear windowId={} hwnd={} oldType={} oldSource={} reason={} oldAgeMs={} rect={} provider={}",
                    windowId, cleared.getHwnd(), cleared.getType(), normalize(cleared.getSource()),
                    normalize(reason), ageMs(now, cleared.getDetectedAtMs()), formatRect(cleared.getDialogRect()),
                    normalize(cleared.getCaptureProvider()));
        }
    }

    /**
     * Register task-owned dialog operations that the generic watcher may prepare.
     *
     * @param interest task-scoped operations, target task type, and expiration time.
     * @param reason diagnostic reason written to logs.
     */
    public void updateDialogInterest(WindowDialogInterest interest, String reason) {
        if (interest == null) {
            clearDialogInterest(reason);
            return;
        }
        dialogInterest.set(interest);
        log.info("[latency] event=window.dialog.interest.update windowId={} task={} operations={} source={} reason={} ttlMs={}",
                windowId, interest.getTaskType(), interest.getOperations(), normalize(interest.getSource()),
                normalize(reason), Math.max(0L, interest.getExpiresAtMs() - System.currentTimeMillis()));
    }

    public void clearDialogInterest(String reason) {
        WindowDialogInterest cleared = dialogInterest.getAndSet(null);
        if (cleared != null) {
            log.info("[latency] event=window.dialog.interest.clear windowId={} task={} operations={} source={} reason={}",
                    windowId, cleared.getTaskType(), cleared.getOperations(), normalize(cleared.getSource()),
                    normalize(reason));
        }
    }

    public void updateDialogPreparationRequest(DialogPreparationRequest request) {
        dialogPreparationRequest.set(request);
        if (request == null) {
            clearPreparedDialogAction("dialog preparation request cleared");
            dialogPreparationStatus.set(DialogPreparationStatus.none());
            logDialogPreparationState("request-cleared", null, "dialog preparation request cleared");
            return;
        }
        dialogPreparationStatus.set(DialogPreparationStatus.builder()
                .phase(DialogPreparationPhase.REQUESTED)
                .operation(request.getOperation())
                .targetKeyword(request.getTargetKeyword())
                .source(request.getSource())
                .requestCreatedAtMs(request.getCreatedAtMs())
                .build());
        logDialogPreparationState("requested", request, null);
    }

    public void clearDialogPreparationRequest(String reason) {
        DialogPreparationRequest clearedRequest = dialogPreparationRequest.get();
        dialogPreparationRequest.set(null);
        clearPreparedDialogAction(reason);
        dialogPreparationStatus.set(DialogPreparationStatus.none());
        logDialogPreparationState("request-clear", clearedRequest, reason);
    }

    public void markDialogPreparationStarted(DialogPreparationRequest request) {
        if (request == null || dialogPreparationRequest.get() != request) {
            return;
        }
        long now = System.currentTimeMillis();
        dialogPreparationStatus.set(DialogPreparationStatus.builder()
                .phase(DialogPreparationPhase.PREPARING)
                .operation(request.getOperation())
                .targetKeyword(request.getTargetKeyword())
                .source(request.getSource())
                .requestCreatedAtMs(request.getCreatedAtMs())
                .preparingStartedAtMs(now)
                .build());
        logDialogPreparationState("preparing", request, null, now);
    }

    public void markDialogPreparationFailed(DialogPreparationRequest request, String reason) {
        if (request == null || dialogPreparationRequest.get() != request) {
            return;
        }
        long now = System.currentTimeMillis();
        dialogPreparationStatus.set(DialogPreparationStatus.builder()
                .phase(DialogPreparationPhase.FAILED)
                .operation(request.getOperation())
                .targetKeyword(request.getTargetKeyword())
                .source(request.getSource())
                .requestCreatedAtMs(request.getCreatedAtMs())
                .completedAtMs(now)
                .failureReason(normalize(reason))
                .build());
        logDialogPreparationState("failed", request, reason, now);
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
            DialogPreparationStatus previous = dialogPreparationStatus.get();
            long requestCreatedAtMs = previous != null
                    && previous.matches(action.getOperation(), action.getTargetKeyword())
                    ? previous.getRequestCreatedAtMs()
                    : 0L;
            long preparingStartedAtMs = previous != null
                    && previous.matches(action.getOperation(), action.getTargetKeyword())
                    && previous.getPreparingStartedAtMs() > 0L
                    ? previous.getPreparingStartedAtMs()
                    : action.getPreparedAtMs();
            long now = System.currentTimeMillis();
            dialogPreparationStatus.set(DialogPreparationStatus.builder()
                    .phase(DialogPreparationPhase.READY)
                    .operation(action.getOperation())
                    .targetKeyword(action.getTargetKeyword())
                    .source(action.getSource())
                    .requestCreatedAtMs(requestCreatedAtMs)
                    .preparingStartedAtMs(preparingStartedAtMs)
                    .completedAtMs(now)
                    .build());
            log.info("[latency] event=window.dialog.prepare.state phase=READY windowId={} hwnd={} operation={} target={} source={} requestAgeMs={} preparingAgeMs={} preparedAgeMs={} verifiedAgeMs={} matchedText={} click=({}, {})",
                    windowId, action.getHwnd(), action.getOperation(), action.getTargetKeyword(),
                    normalize(action.getSource()), ageMs(now, requestCreatedAtMs), ageMs(now, preparingStartedAtMs),
                    ageMs(now, action.getPreparedAtMs()), ageMs(now, action.getLastVerifiedAtMs()),
                    normalize(action.getMatchedText()), action.getAbsoluteX(), action.getAbsoluteY());
        }
    }

    public void clearPreparedDialogAction(String reason) {
        PreparedDialogAction cleared = preparedDialogAction.getAndSet(null);
        if (cleared != null) {
            long now = System.currentTimeMillis();
            log.info("[latency] event=window.ready.clearPrepared windowId={} hwnd={} reason={} operation={} target={} source={} preparedAgeMs={} verifiedAgeMs={}",
                    windowId, cleared.getHwnd(), normalize(reason), cleared.getOperation(), cleared.getTargetKeyword(),
                    cleared.getSource(), ageMs(now, cleared.getPreparedAtMs()), ageMs(now, cleared.getLastVerifiedAtMs()));
        }
        clearReadyDialogPreparationStatus();
    }

    /**
     * Atomically take the prepared dialog action for execution by task/navigation code.
     *
     * <p>This method is intentionally separate from {@link #clearPreparedDialogAction(String)}:
     * clear is for stale/reset cleanup, while consume means a caller is about to execute the cached
     * click action and no later caller should see the same candidate.</p>
     *
     * @param reason diagnostic reason written to logs.
     * @return consumed prepared action, or null when no action was cached.
     */
    public PreparedDialogAction consumePreparedDialogAction(String reason) {
        while (true) {
            PreparedDialogAction current = preparedDialogAction.get();
            if (current == null) {
                logPreparedConsumeAbsent(reason, null, null);
                return null;
            }
            if (!preparedDialogAction.compareAndSet(current, null)) {
                continue;
            }
            clearReadyDialogPreparationStatusFor(current);
            logPreparedConsume("consumed", reason, current, null, null);
            return current;
        }
    }

    /**
     * Atomically consume a prepared dialog action only when it matches the expected operation and
     * target keyword.
     *
     * <p>Mismatch is deliberately non-destructive: route code can probe for its own action without
     * deleting another task's prepared candidate. The final removal is a CAS loop, so a watcher can
     * safely replace the prepared action while a task is checking it; only the exact action that was
     * validated by this caller can be consumed.</p>
     *
     * @param expectedOperation operation the caller intends to execute, such as route transfer.
     * @param expectedTargetKeyword expected target keyword; blank means operation-only matching.
     * @param reason diagnostic reason written to logs.
     * @return consumed prepared action, or null when absent or mismatched.
     */
    public PreparedDialogAction consumePreparedDialogAction(DialogOperation expectedOperation,
                                                           String expectedTargetKeyword,
                                                           String reason) {
        return consumePreparedDialogAction(expectedOperation, expectedTargetKeyword, reason, false);
    }

    /**
     * Atomically consume a prepared dialog action only when the watcher verified it recently enough.
     *
     * @param expectedOperation operation the caller intends to execute.
     * @param expectedTargetKeyword expected target keyword; blank means operation-only matching.
     * @param reason diagnostic reason written to logs.
     * @param maxVerifiedAgeMs maximum accepted age of {@link PreparedDialogAction#getLastVerifiedAtMs()}.
     *                         Negative disables the freshness check.
     * @return consumed prepared action, or null when absent, mismatched, or stale.
     */
    public PreparedDialogAction consumePreparedDialogAction(DialogOperation expectedOperation,
                                                           String expectedTargetKeyword,
                                                           String reason,
                                                           long maxVerifiedAgeMs) {
        return consumePreparedDialogAction(expectedOperation, expectedTargetKeyword, reason, false, maxVerifiedAgeMs);
    }

    /**
     * Atomically consume a prepared dialog action with an explicit route-only recovery policy.
     *
     * <p>Route dialogs are prepared by the window watcher while a pathing intent is active. A task
     * may later consume a STOPPED_AWAY terminal snapshot and clear that intent before it gets back
     * to the prepared route option. In that narrow case the prepared action is still safe if it is a
     * fresh ROUTE_TRANSFER for the requested target and bound hwnd. Other dialog operations keep the
     * original strict intent check.</p>
     *
     * @param expectedOperation operation the caller intends to execute, such as route transfer.
     * @param expectedTargetKeyword expected target keyword; blank means operation-only matching.
     * @param reason diagnostic reason written to logs.
     * @param allowClearedRouteIntent true only for route-transfer consumers that may recover an
     *                                action after the active pathing intent has been cleared.
     * @return consumed prepared action, or null when absent or mismatched.
     */
    public PreparedDialogAction consumePreparedDialogAction(DialogOperation expectedOperation,
                                                           String expectedTargetKeyword,
                                                           String reason,
                                                           boolean allowClearedRouteIntent) {
        return consumePreparedDialogAction(
                expectedOperation, expectedTargetKeyword, reason, allowClearedRouteIntent, -1L);
    }

    private PreparedDialogAction consumePreparedDialogAction(DialogOperation expectedOperation,
                                                            String expectedTargetKeyword,
                                                            String reason,
                                                            boolean allowClearedRouteIntent,
                                                            long maxVerifiedAgeMs) {
        while (true) {
            PreparedDialogAction current = preparedDialogAction.get();
            if (current == null) {
                logPreparedConsumeAbsent(reason, expectedOperation, expectedTargetKeyword);
                return null;
            }
            String mismatchReason = preparedActionMismatchReason(
                    current, expectedOperation, expectedTargetKeyword, allowClearedRouteIntent);
            if (mismatchReason != null) {
                logPreparedConsume("mismatch", reason, current, expectedOperation, expectedTargetKeyword,
                        "mismatchReason", mismatchReason);
                return null;
            }
            if (maxVerifiedAgeMs >= 0L && !current.verifiedWithin(System.currentTimeMillis(), maxVerifiedAgeMs)) {
                if (!preparedDialogAction.compareAndSet(current, null)) {
                    continue;
                }
                clearReadyDialogPreparationStatusFor(current);
                logPreparedConsume("stale", reason, current, expectedOperation, expectedTargetKeyword,
                        "maxVerifiedAgeMs", maxVerifiedAgeMs);
                return null;
            }
            if (!preparedDialogAction.compareAndSet(current, null)) {
                continue;
            }
            clearReadyDialogPreparationStatusFor(current);
            logPreparedConsume("consumed", reason, current, expectedOperation, expectedTargetKeyword);
            return current;
        }
    }

    /**
     * Remember a route-dialog option click until the pathing watcher proves the target map changed.
     *
     * @param memory clicked route option metadata. Null clears the pending record.
     */
    public void updatePendingTransferChoiceMemory(PendingTransferChoiceMemory memory) {
        pendingTransferChoiceMemory.set(memory);
    }

    public PendingTransferChoiceMemory consumePendingTransferChoiceMemory() {
        return pendingTransferChoiceMemory.getAndSet(null);
    }

    public void clearPendingTransferChoiceMemory(String reason) {
        pendingTransferChoiceMemory.set(null);
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

    /**
     * Mark that the current pathing handoff may have left a foreground UI blocker, such as the
     * Alt+1 mini-map panel, on this window. The watcher still owns movement observation; the task
     * phase consumes this flag before business clicks that must not be covered by stale UI.
     *
     * @param reason diagnostic reason written into the per-window pathing snapshot.
     */
    public void markPathingUiCleanupRecommended(String reason) {
        WindowPathingSnapshot snapshot = pathingSnapshot.get();
        if (snapshot == null || snapshot.getState() == WindowPathingState.NONE) {
            return;
        }
        pathingSnapshot.set(snapshot.toBuilder()
                .uiCleanupRecommended(true)
                .uiCleanupReason(normalize(reason))
                .uiCleanupRecommendedAtMs(System.currentTimeMillis())
                .build());
    }

    public void clearPathingUiCleanupRecommendation(String reason) {
        WindowPathingSnapshot snapshot = pathingSnapshot.get();
        if (snapshot == null || !snapshot.isUiCleanupRecommended()) {
            return;
        }
        pathingSnapshot.set(snapshot.toBuilder()
                .uiCleanupRecommended(false)
                .uiCleanupReason(normalize(reason))
                .uiCleanupRecommendedAtMs(0L)
                .build());
    }

    /**
     * Return a fresh prepared route-dialog action that still belongs to the terminal pathing
     * snapshot, so task code can delay clearing the active intent and let Navigation consume it.
     *
     * @param terminalSnapshot watcher terminal snapshot being consumed by the task layer.
     * @param maxAgeMs maximum allowed age since the watcher last verified the prepared action.
     * @return matching route-transfer action, or null when the terminal snapshot should be cleared
     *         normally.
     */
    public PreparedDialogAction freshPreparedRouteActionForPathingTerminal(WindowPathingSnapshot terminalSnapshot,
                                                                          long maxAgeMs) {
        PreparedDialogAction action = preparedDialogAction.get();
        if (action == null || action.getOperation() != DialogOperation.ROUTE_TRANSFER) {
            return null;
        }
        if (!preparedActionMatchesCurrentWindow(action)) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (!action.verifiedWithin(now, maxAgeMs)) {
            return null;
        }
        WindowPathingIntent activeIntent = getActivePathingIntent().orElse(null);
        WindowPathingIntent terminalIntent = terminalSnapshot == null ? null : terminalSnapshot.getIntent();
        if (isSamePreparedRouteIntent(action, activeIntent) || isSamePreparedRouteIntent(action, terminalIntent)
                || isSamePreparedRouteTarget(action, activeIntent) || isSamePreparedRouteTarget(action, terminalIntent)) {
            return action;
        }
        return null;
    }

    public void clearPathingSignal(String reason) {
        pathingSnapshot.set(WindowPathingSnapshot.builder()
                .state(WindowPathingState.NONE)
                .message(normalize(reason))
                .build());
        clearPendingTransferChoiceMemory("pathing signal cleared");
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
        clearVisibleDialogSnapshot("runtime reset");
        clearDialogPreparationRequest("runtime reset");
        clearDialogInterest("runtime reset");
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

    private static String formatRect(int[] rect) {
        if (rect == null || rect.length < 4) {
            return "-";
        }
        return rect[0] + "," + rect[1] + " " + rect[2] + "x" + rect[3];
    }

    private static long ageMs(long now, long timestampMs) {
        return timestampMs <= 0L ? -1L : Math.max(0L, now - timestampMs);
    }

    private void logDialogPreparationState(String result,
                                           DialogPreparationRequest request,
                                           String reason) {
        logDialogPreparationState(result, request, reason, System.currentTimeMillis());
    }

    private void logDialogPreparationState(String result,
                                           DialogPreparationRequest request,
                                           String reason,
                                           long now) {
        log.info("[latency] event=window.dialog.prepare.state phase={} windowId={} hwnd={} operation={} target={} source={} requestAgeMs={} expiresInMs={} reason={}",
                result,
                windowId,
                nativeBinding == null ? null : nativeBinding.getNativeHandle(),
                request == null ? null : request.getOperation(),
                request == null ? null : request.getTargetKeyword(),
                request == null ? null : normalize(request.getSource()),
                request == null ? -1L : ageMs(now, request.getCreatedAtMs()),
                request == null || request.getExpiresAtMs() <= 0L ? -1L : request.getExpiresAtMs() - now,
                normalize(reason));
    }

    private void clearReadyDialogPreparationStatus() {
        DialogPreparationStatus status = dialogPreparationStatus.get();
        if (status != null && status.getPhase() == DialogPreparationPhase.READY) {
            dialogPreparationStatus.set(DialogPreparationStatus.none());
        }
    }

    private void clearReadyDialogPreparationStatusFor(PreparedDialogAction consumed) {
        if (consumed == null) {
            return;
        }
        while (true) {
            DialogPreparationStatus status = dialogPreparationStatus.get();
            if (!isReadyStatusForAction(status, consumed)) {
                return;
            }
            /*
             * READY state belongs to the prepared action being consumed. Do not clear it unless the
             * status still matches this action; the watcher may have prepared a newer route/dialog
             * candidate after the action CAS, and that newer READY state must remain visible.
             */
            if (dialogPreparationStatus.compareAndSet(status, DialogPreparationStatus.none())) {
                return;
            }
        }
    }

    private boolean isReadyStatusForAction(DialogPreparationStatus status, PreparedDialogAction action) {
        return status != null
                && status.getPhase() == DialogPreparationPhase.READY
                && status.matches(action.getOperation(), action.getTargetKeyword())
                && Objects.equals(normalize(status.getSource()), normalize(action.getSource()));
    }

    private String preparedActionMismatchReason(PreparedDialogAction action,
                                                DialogOperation expectedOperation,
                                                String expectedTargetKeyword) {
        return preparedActionMismatchReason(action, expectedOperation, expectedTargetKeyword, false);
    }

    private String preparedActionMismatchReason(PreparedDialogAction action,
                                                DialogOperation expectedOperation,
                                                String expectedTargetKeyword,
                                                boolean allowClearedRouteIntent) {
        if (action == null) {
            return "absent";
        }
        if (action.getWindowId() != null && !Objects.equals(action.getWindowId(), windowId)) {
            return "windowId";
        }
        String currentHwnd = nativeBinding == null ? null : nativeBinding.getNativeHandle();
        if (action.getHwnd() != null && !Objects.equals(action.getHwnd(), currentHwnd)) {
            return "hwnd";
        }
        String currentIntentId = currentActiveIntentId();
        if (action.getIntentId() != null && !Objects.equals(action.getIntentId(), currentIntentId)
                && !isClearedRouteIntentRecoveryAllowed(action, expectedOperation, currentIntentId, allowClearedRouteIntent)) {
            return "intentId";
        }
        if (expectedOperation != null && action.getOperation() != expectedOperation) {
            return "operation";
        }
        String expectedTarget = normalize(expectedTargetKeyword);
        if (expectedTarget != null && !Objects.equals(action.getTargetKeyword(), expectedTarget)) {
            return "target";
        }
        return null;
    }

    private boolean isClearedRouteIntentRecoveryAllowed(PreparedDialogAction action,
                                                        DialogOperation expectedOperation,
                                                        String currentIntentId,
                                                        boolean allowClearedRouteIntent) {
        return allowClearedRouteIntent
                && currentIntentId == null
                && expectedOperation == DialogOperation.ROUTE_TRANSFER
                && action != null
                && action.getOperation() == DialogOperation.ROUTE_TRANSFER;
    }

    private boolean preparedActionMatchesCurrentWindow(PreparedDialogAction action) {
        if (action.getWindowId() != null && !action.getWindowId().isBlank()
                && !Objects.equals(action.getWindowId(), windowId)) {
            return false;
        }
        String currentHwnd = nativeBinding == null ? null : nativeBinding.getNativeHandle();
        return action.getHwnd() == null || action.getHwnd().isBlank()
                || Objects.equals(action.getHwnd(), currentHwnd);
    }

    private boolean isSamePreparedRouteIntent(PreparedDialogAction action, WindowPathingIntent intent) {
        return action != null
                && intent != null
                && action.getIntentId() != null
                && Objects.equals(action.getIntentId(), intent.getIntentId());
    }

    private boolean isSamePreparedRouteTarget(PreparedDialogAction action, WindowPathingIntent intent) {
        return action != null
                && intent != null
                && normalize(action.getTargetKeyword()) != null
                && Objects.equals(normalize(action.getTargetKeyword()), normalize(intent.getTargetMapName()));
    }

    private void logPreparedConsumeAbsent(String reason,
                                          DialogOperation expectedOperation,
                                          String expectedTargetKeyword) {
        log.info("[latency] event=window.ready.consumePrepared result=absent windowId={} hwnd={} activeIntentId={} reason={} expectedOperation={} expectedTarget={}",
                windowId, nativeBinding == null ? null : nativeBinding.getNativeHandle(),
                currentActiveIntentId(), normalize(reason), expectedOperation, normalize(expectedTargetKeyword));
    }

    private void logPreparedConsume(String result,
                                    String reason,
                                    PreparedDialogAction action,
                                    DialogOperation expectedOperation,
                                    String expectedTargetKeyword,
                                    Object... extraPairs) {
        long now = System.currentTimeMillis();
        String extraText = formatExtraPairs(extraPairs);
        log.info("[latency] event=window.ready.consumePrepared result={} windowId={} hwnd={} intentId={} activeIntentId={} reason={} operation={} target={} source={} expectedOperation={} expectedTarget={} preparedAgeMs={} verifiedAgeMs={}{}",
                result, windowId, action == null ? null : action.getHwnd(),
                action == null ? null : action.getIntentId(), currentActiveIntentId(), normalize(reason),
                action == null ? null : action.getOperation(), action == null ? null : action.getTargetKeyword(),
                action == null ? null : action.getSource(), expectedOperation, normalize(expectedTargetKeyword),
                action == null ? -1L : ageMs(now, action.getPreparedAtMs()),
                action == null ? -1L : ageMs(now, action.getLastVerifiedAtMs()),
                extraText);
    }

    private String currentActiveIntentId() {
        WindowPathingSnapshot snapshot = pathingSnapshot.get();
        return snapshot == null || snapshot.getIntent() == null ? null : snapshot.getIntent().getIntentId();
    }

    private static String formatExtraPairs(Object... extraPairs) {
        if (extraPairs == null || extraPairs.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i + 1 < extraPairs.length; i += 2) {
            builder.append(' ')
                    .append(extraPairs[i])
                    .append('=')
                    .append(extraPairs[i + 1]);
        }
        return builder.toString();
    }

    private TaskType resolveTaskForRuntimeEvent(TaskType taskType) {
        if (taskType != null && taskType != TaskType.UNKNOWN) {
            return taskType;
        }
        return selectedTaskType == null ? TaskType.UNKNOWN : selectedTaskType;
    }
}
