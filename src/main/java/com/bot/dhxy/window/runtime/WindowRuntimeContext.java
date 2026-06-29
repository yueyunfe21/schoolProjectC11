package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.DialogPreparationRequest;
import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogPreparationStatus;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.navigation.PendingTransferChoiceMemory;
import com.bot.dhxy.model.navigation.WorldMapRouteResultPendingMemory;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.tools.GameStateUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 单个游戏窗口的运行上下文。
 *
 * 一个独立游戏窗口对应一个 WindowRuntimeContext。
 * 这里保存窗口元信息、窗口级 GameContext.State、当前任务状态和最近一次运行信息。
 */
@Slf4j
public class WindowRuntimeContext {
    private static final long ORDINARY_PRE_BATTLE_PAUSE_COMPENSATION_THRESHOLD_MS = 500L;


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
    private final AtomicLong observerWakeSeq = new AtomicLong();
    private final AtomicReference<DialogPreparationRequest> dialogPreparationRequest = new AtomicReference<>();
    private final AtomicReference<PreparedDialogAction> preparedDialogAction = new AtomicReference<>();
    private final AtomicReference<PendingTransferChoiceMemory> pendingTransferChoiceMemory = new AtomicReference<>();
    private final AtomicReference<WorldMapRouteResultPendingMemory> pendingWorldMapRouteResultMemory =
            new AtomicReference<>();
    private final AtomicReference<String> pendingSmartClickEvidenceProofToken = new AtomicReference<>();
    private final AtomicReference<String> leftTopStatusSwitchClosePending = new AtomicReference<>();
    private final AtomicReference<String> taskQueueStartupPreparationDone = new AtomicReference<>();
    private final AtomicReference<GameStateUtil.FlyingState> taskQueueStartupFlyingState = new AtomicReference<>();
    private final AtomicReference<String> taskQueueStartupFlyingStateSource = new AtomicReference<>();
    private final AtomicReference<DialogPreparationStatus> dialogPreparationStatus =
            new AtomicReference<>(DialogPreparationStatus.none());
    private final AtomicReference<String> runningTaskProgressText = new AtomicReference<>("-");
    private final AtomicLong playerIdentityEpoch = new AtomicLong();
    private final AtomicLong ordinaryPreBattleStartedAtMs = new AtomicLong();
    private final AtomicLong ordinaryPreBattleTimeoutPublishedAtMs = new AtomicLong();
    private final AtomicLong ordinaryEnterBattleTargetMapGateStartedAtMs = new AtomicLong();
    private final AtomicLong ordinaryEnterBattleTargetMapOpenedAtMs = new AtomicLong();
    private volatile TaskType ordinaryPreBattleTaskType = TaskType.UNKNOWN;
    private volatile String ordinaryPreBattleSource;
    private volatile String ordinaryPreBattleTargetKeyword;
    private volatile String ordinaryEnterBattleTargetMapName;
    private volatile String ordinaryEnterBattleTargetMapSource;
    private volatile String taskOwnerPlayerId;
    private volatile String taskOwnerPlayerName;
    private volatile String visiblePlayerId;
    private volatile String visiblePlayerName;
    private volatile boolean identitySuspended;

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

    public synchronized WindowIdentityDrift setNativeBinding(WindowNativeBinding nativeBinding) {
        WindowNativeBinding next = nativeBinding == null ? WindowNativeBinding.empty() : nativeBinding;
        WindowNativeBinding previous = this.nativeBinding;
        if (previous != null
                && previous.hasNativeHandle()
                && next.hasNativeHandle()
                && Objects.equals(previous.getNativeHandle(), next.getNativeHandle())
                && normalize(previous.getTitle()) != null
                && normalize(next.getTitle()) == null) {
            next = next.withLiveState(previous.getTitle(), next.getClassName(), next.getProcessId(),
                    next.getX(), next.getY(), next.getWidth(), next.getHeight());
        }
        WindowIdentityDrift drift = detectIdentityDrift(previous, next);
        boolean hardNativeChange = !sameNativeWindow(previous, next);
        if (hardNativeChange) {
            if (previous != null && previous.hasNativeHandle() && next.hasNativeHandle()) {
                playerIdentityEpoch.incrementAndGet();
                WindowTitleIdentityParser.parse(next.getTitle()).ifPresent(this::applyParsedIdentity);
            }
            clearPlayerScopedRuntimeState("native binding changed");
            clearIdentitySuspension("native binding changed");
        } else if (drift.isDrifted()) {
            applyParsedIdentity(drift.newIdentity());
            if (isBusy()) {
                clearPlayerScopedTransientState("native title/player drift");
                updateIdentitySuspension(drift.oldIdentity(), drift.newIdentity());
            } else {
                clearPlayerScopedRuntimeState("native title/player drift");
                clearIdentitySuspension("idle native title/player drift");
            }
            log.warn("[window identity drift] same HWND title/player changed: windowId={} hwnd={} oldTitle={} newTitle={} oldPlayer={}/{} newPlayer={}/{} epoch={}",
                    windowId,
                    next.getNativeHandle(),
                    previous == null ? null : previous.getTitle(),
                    next.getTitle(),
                    drift.oldIdentity() == null ? null : drift.oldIdentity().playerName(),
                    drift.oldIdentity() == null ? null : drift.oldIdentity().playerId(),
                    drift.newIdentity() == null ? null : drift.newIdentity().playerName(),
                    drift.newIdentity() == null ? null : drift.newIdentity().playerId(),
                    drift.epoch());
        }
        this.nativeBinding = next;
        return drift;
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
        /*
         * Dialog interest is task phase state. 五倍 specifically relies on this staying alive
         * while the leader waits for Runner-prepared dialogs; explicit task/runner boundaries own
         * cleanup, not a passive TTL check here.
         */
        return Optional.ofNullable(dialogInterest.get());
    }

    public DialogPreparationRequest getDialogPreparationRequest() { return dialogPreparationRequest.get(); }

    public PreparedDialogAction getPreparedDialogAction() { return preparedDialogAction.get(); }

    public PendingTransferChoiceMemory getPendingTransferChoiceMemory() { return pendingTransferChoiceMemory.get(); }

    public String getPendingSmartClickEvidenceProofToken() { return pendingSmartClickEvidenceProofToken.get(); }

    public WorldMapRouteResultPendingMemory getPendingWorldMapRouteResultMemory() {
        return pendingWorldMapRouteResultMemory.get();
    }

    /** @return true when member startup detected an open CR107 left-top switch that still needs a safe-window close. */
    public boolean isLeftTopStatusSwitchClosePending() { return leftTopStatusSwitchClosePending.get() != null; }

    /**
     * Mark that this bound window should close the CR107 left-top status switch later.
     *
     * @param source diagnostic source such as {@code member-startup-probe}; blank values are stored
     *               as {@code unknown}.
     */
    public void markLeftTopStatusSwitchClosePending(String source) {
        leftTopStatusSwitchClosePending.set(normalize(source) == null ? "unknown" : normalize(source));
    }

    /**
     * Clear and consume the pending CR107 left-top status close flag.
     *
     * @param source diagnostic source for the consume site.
     * @return true when a pending flag existed before this call.
     */
    public boolean consumeLeftTopStatusSwitchClosePending(String source) {
        return leftTopStatusSwitchClosePending.getAndSet(null) != null;
    }

    /**
     * Clear the pending CR107 left-top status close flag without treating it as a clicked action.
     *
     * @param source diagnostic source for the clear site.
     */
    public void clearLeftTopStatusSwitchClosePending(String source) {
        leftTopStatusSwitchClosePending.set(null);
    }

    /** Clear queue-scoped startup UI preparation markers before a new accepted task queue runs. */
    public void clearTaskQueueStartupPreparationState(String source) {
        taskQueueStartupPreparationDone.set(null);
        clearTaskQueueStartupFlyingState(source);
    }

    /** @return true when this task code already ran queue-scoped startup UI preparation. */
    public boolean isTaskQueueStartupPreparationDone(String taskCode) {
        String normalized = normalize(taskCode);
        return normalized != null && normalized.equals(taskQueueStartupPreparationDone.get());
    }

    /** Mark queue-scoped startup UI preparation as completed for the current task code. */
    public void markTaskQueueStartupPreparationDone(String taskCode) {
        taskQueueStartupPreparationDone.set(normalize(taskCode));
    }

    /**
     * Record the flying/mounted state observed while startup already had the Alt+U panel open.
     *
     * <p>This is queue-scoped evidence for the first task round only. 五倍 consumes it once before
     * its first post-accept prepath so an already-flying first run does not press Alt+C again and
     * cancel movement.</p>
     *
     * @param state observed flying state; null is stored as {@link GameStateUtil.FlyingState#UNKNOWN}.
     * @param source diagnostic source that produced the observation.
     */
    public void markTaskQueueStartupFlyingState(GameStateUtil.FlyingState state, String source) {
        GameStateUtil.FlyingState normalizedState = state == null ? GameStateUtil.FlyingState.UNKNOWN : state;
        String normalizedSource = normalize(source);
        taskQueueStartupFlyingState.set(normalizedState);
        taskQueueStartupFlyingStateSource.set(normalizedSource == null ? "unknown" : normalizedSource);
        log.info("[window startup flying] marked: windowId={} state={} source={}",
                windowId, normalizedState, taskQueueStartupFlyingStateSource.get());
    }

    /**
     * Consume the startup flying observation once.
     *
     * @param source diagnostic consume source.
     * @return the observed startup state, or UNKNOWN when no startup observation exists.
     */
    public GameStateUtil.FlyingState consumeTaskQueueStartupFlyingState(String source) {
        GameStateUtil.FlyingState state = taskQueueStartupFlyingState.getAndSet(null);
        String observedSource = taskQueueStartupFlyingStateSource.getAndSet(null);
        GameStateUtil.FlyingState result = state == null ? GameStateUtil.FlyingState.UNKNOWN : state;
        log.info("[window startup flying] consumed: windowId={} state={} observedSource={} consumeSource={}",
                windowId, result, observedSource, normalize(source));
        return result;
    }

    /** Clear stale startup flying evidence without consuming it as a task decision. */
    public void clearTaskQueueStartupFlyingState(String source) {
        GameStateUtil.FlyingState cleared = taskQueueStartupFlyingState.getAndSet(null);
        String observedSource = taskQueueStartupFlyingStateSource.getAndSet(null);
        if (cleared != null || observedSource != null) {
            log.info("[window startup flying] cleared: windowId={} state={} observedSource={} source={}",
                    windowId, cleared, observedSource, normalize(source));
        }
    }

    public DialogPreparationStatus getDialogPreparationStatus() { return dialogPreparationStatus.get(); }

    public String getRunningTaskProgressText() { return runningTaskProgressText.get(); }

    /**
     * Update the user-facing in-task run counter for finite repeatable tasks.
     *
     * @param completedRuns number of fully completed task rounds/runs, zero-based at task start.
     * @param totalRuns configured finite run count. Non-positive values do not produce a UI count
     *                  because there is no right-hand total to display.
     */
    public void updateTaskRunProgress(int completedRuns, int totalRuns) {
        if (totalRuns <= 0) {
            runningTaskProgressText.set("-");
            return;
        }
        int safeCompleted = Math.max(0, Math.min(completedRuns, totalRuns));
        runningTaskProgressText.set(safeCompleted + "/" + totalRuns);
    }

    /**
     * Clear the user-facing in-task run counter when no finite task progress is available.
     */
    public void clearTaskRunProgress() {
        runningTaskProgressText.set("-");
    }

    public long getObserverWakeSeq() { return observerWakeSeq.get(); }

    public long getPlayerIdentityEpoch() { return playerIdentityEpoch.get(); }

    public String getTaskOwnerPlayerId() { return taskOwnerPlayerId; }

    public String getVisiblePlayerId() { return visiblePlayerId; }

    public boolean isIdentitySuspended() { return identitySuspended; }

    /**
     * Wait while this HWND is showing a different role tab than the task owner.
     *
     * @param stopToken optional task stop token checked while parked.
     * @return milliseconds spent waiting for the task owner tab to become visible again.
     */
    public long waitIfIdentitySuspended(TaskStopToken stopToken) {
        if (!identitySuspended) {
            return 0L;
        }
        long blockedStartMs = System.currentTimeMillis();
        log.info("[window identity suspend] checkpoint reached: windowId={} owner={}/{} visible={}/{}",
                windowId, taskOwnerPlayerName, taskOwnerPlayerId, visiblePlayerName, visiblePlayerId);
        synchronized (this) {
            while (identitySuspended) {
                if (stopToken != null) {
                    stopToken.throwIfStopRequested();
                }
                try {
                    wait(250L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TaskStopRequestedException("identity suspension wait interrupted");
                }
            }
        }
        if (stopToken != null) {
            stopToken.throwIfStopRequested();
        }
        long blockedMs = Math.max(0L, System.currentTimeMillis() - blockedStartMs);
        log.info("[window identity suspend] checkpoint resumed: windowId={} owner={}/{} blockedMs={}",
                windowId, taskOwnerPlayerName, taskOwnerPlayerId, blockedMs);
        return blockedMs;
    }

    public long getOrdinaryPreBattleStartedAtMs() { return ordinaryPreBattleStartedAtMs.get(); }

    public TaskType getOrdinaryPreBattleTaskType() { return ordinaryPreBattleTaskType; }

    public String getOrdinaryPreBattleSource() { return ordinaryPreBattleSource; }

    public String getOrdinaryPreBattleTargetKeyword() { return ordinaryPreBattleTargetKeyword; }

    public long getOrdinaryEnterBattleTargetMapGateStartedAtMs() { return ordinaryEnterBattleTargetMapGateStartedAtMs.get(); }

    public long getOrdinaryEnterBattleTargetMapOpenedAtMs() { return ordinaryEnterBattleTargetMapOpenedAtMs.get(); }

    public String getOrdinaryEnterBattleTargetMapName() { return ordinaryEnterBattleTargetMapName; }

    public String getOrdinaryEnterBattleTargetMapSource() { return ordinaryEnterBattleTargetMapSource; }

    /**
     * Start the ordinary-monster pre-battle timer for this window.
     *
     * @param taskType task that owns the ordinary-monster pathing flow.
     * @param source diagnostic source for the first successful green-link click.
     * @param targetKeyword current tracker/destination text, nullable.
     * @param nowMs wall-clock timestamp in milliseconds.
     * @return true when this call started a new timer; false when an earlier ordinary timer is
     *         already active and must not be reset by same-target re-navigation.
     */
    public boolean startOrdinaryPreBattleTimer(TaskType taskType,
                                               String source,
                                               String targetKeyword,
                                               long nowMs) {
        long startedAt = Math.max(1L, nowMs);
        if (!ordinaryPreBattleStartedAtMs.compareAndSet(0L, startedAt)) {
            log.info("[window ordinary-prebattle] timer already active: windowId={} task={} source={} target={} startedAt={} elapsedMs={}",
                    windowId, ordinaryPreBattleTaskType, normalize(source), normalize(targetKeyword),
                    ordinaryPreBattleStartedAtMs.get(), ageMs(System.currentTimeMillis(), ordinaryPreBattleStartedAtMs.get()));
            return false;
        }
        ordinaryPreBattleTimeoutPublishedAtMs.set(0L);
        ordinaryPreBattleTaskType = taskType == null ? TaskType.UNKNOWN : taskType;
        ordinaryPreBattleSource = normalize(source);
        ordinaryPreBattleTargetKeyword = normalize(targetKeyword);
        log.info("[window ordinary-prebattle] timer started: windowId={} task={} source={} target={} startedAt={}",
                windowId, ordinaryPreBattleTaskType, ordinaryPreBattleSource, ordinaryPreBattleTargetKeyword,
                startedAt);
        return true;
    }

    /**
     * Mark that the runner has already published the timeout event for the active ordinary timer.
     *
     * @param nowMs wall-clock timestamp in milliseconds.
     * @return true only for the first publisher of the active timer.
     */
    public boolean markOrdinaryPreBattleTimeoutPublished(long nowMs) {
        if (ordinaryPreBattleStartedAtMs.get() <= 0L) {
            return false;
        }
        return ordinaryPreBattleTimeoutPublishedAtMs.compareAndSet(0L, Math.max(1L, nowMs));
    }

    /**
     * Open the map-gated ordinary enter-battle watcher window for 五倍.
     *
     * @param taskType task that owns the tracker green-link pathing.
     * @param source diagnostic source for the green-link click.
     * @param targetMapName target map parsed from the clicked tracker green link; blank disables
     *                      the gate because Runner must not infer a map by itself.
     * @param nowMs wall-clock timestamp in milliseconds.
     * @return true when a new map gate was started; false when input is blank or an earlier gate is
     *         already active for this ordinary/黄袍第一战 flow.
     */
    public boolean startOrdinaryEnterBattleTargetMapGate(TaskType taskType,
                                                         String source,
                                                         String targetMapName,
                                                         long nowMs) {
        String normalizedTargetMap = normalize(targetMapName);
        if (normalizedTargetMap == null) {
            log.info("[window ordinary-enter-battle-map] gate skipped: windowId={} task={} source={} reason=blank-target-map",
                    windowId, taskType == null ? TaskType.UNKNOWN : taskType, normalize(source));
            return false;
        }
        long startedAt = Math.max(1L, nowMs);
        if (!ordinaryEnterBattleTargetMapGateStartedAtMs.compareAndSet(0L, startedAt)) {
            log.info("[window ordinary-enter-battle-map] gate already active: windowId={} task={} source={} targetMap={} startedAt={} elapsedMs={} opened={}",
                    windowId, ordinaryPreBattleTaskType, normalize(source), ordinaryEnterBattleTargetMapName,
                    ordinaryEnterBattleTargetMapGateStartedAtMs.get(),
                    ageMs(System.currentTimeMillis(), ordinaryEnterBattleTargetMapGateStartedAtMs.get()),
                    ordinaryEnterBattleTargetMapOpenedAtMs.get() > 0L);
            return false;
        }
        ordinaryEnterBattleTargetMapOpenedAtMs.set(0L);
        ordinaryEnterBattleTargetMapName = normalizedTargetMap;
        ordinaryEnterBattleTargetMapSource = normalize(source);
        log.info("[window ordinary-enter-battle-map] gate started: windowId={} task={} source={} targetMap={} startedAt={}",
                windowId, taskType == null ? TaskType.UNKNOWN : taskType,
                ordinaryEnterBattleTargetMapSource, ordinaryEnterBattleTargetMapName, startedAt);
        return true;
    }

    /**
     * Mark the target-map gate as opened so Runner registers ordinary enter-battle interest once.
     *
     * @param nowMs wall-clock timestamp in milliseconds.
     * @return true only for the first matching map observation.
     */
    public boolean markOrdinaryEnterBattleTargetMapGateOpened(long nowMs) {
        if (ordinaryEnterBattleTargetMapGateStartedAtMs.get() <= 0L) {
            return false;
        }
        return ordinaryEnterBattleTargetMapOpenedAtMs.compareAndSet(0L, Math.max(1L, nowMs));
    }

    /**
     * Clear the ordinary enter-battle target-map gate.
     *
     * @param reason diagnostic reason for ending the gate.
     */
    public void clearOrdinaryEnterBattleTargetMapGate(String reason) {
        long startedAt = ordinaryEnterBattleTargetMapGateStartedAtMs.getAndSet(0L);
        long openedAt = ordinaryEnterBattleTargetMapOpenedAtMs.getAndSet(0L);
        String source = ordinaryEnterBattleTargetMapSource;
        String targetMap = ordinaryEnterBattleTargetMapName;
        ordinaryEnterBattleTargetMapSource = null;
        ordinaryEnterBattleTargetMapName = null;
        if (startedAt > 0L || openedAt > 0L) {
            log.info("[window ordinary-enter-battle-map] gate cleared: windowId={} source={} targetMap={} reason={} elapsedMs={} opened={}",
                    windowId, source, targetMap, normalize(reason), ageMs(System.currentTimeMillis(), startedAt),
                    openedAt > 0L);
        }
    }

    /**
     * Clear the ordinary-monster pre-battle timer when battle entry is consumed or the round resets.
     *
     * @param reason diagnostic reason for ending the timer.
     */
    public void clearOrdinaryPreBattleTimer(String reason) {
        long startedAt = ordinaryPreBattleStartedAtMs.getAndSet(0L);
        long publishedAt = ordinaryPreBattleTimeoutPublishedAtMs.getAndSet(0L);
        TaskType taskType = ordinaryPreBattleTaskType;
        String source = ordinaryPreBattleSource;
        String target = ordinaryPreBattleTargetKeyword;
        ordinaryPreBattleTaskType = TaskType.UNKNOWN;
        ordinaryPreBattleSource = null;
        ordinaryPreBattleTargetKeyword = null;
        clearOrdinaryEnterBattleTargetMapGate(reason);
        if (startedAt > 0L || publishedAt > 0L) {
            log.info("[window ordinary-prebattle] timer cleared: windowId={} task={} source={} target={} reason={} elapsedMs={} timeoutPublished={}",
                    windowId, taskType, source, target, normalize(reason),
                    ageMs(System.currentTimeMillis(), startedAt), publishedAt > 0L);
        }
    }

    /**
     * Shift the active 五倍 ordinary-monster pre-battle timer after formal maintenance blocks it.
     *
     * @param blockedMs formal maintenance wall-clock duration in milliseconds.
     * @param source diagnostic source for the compensation log.
     * @return true when an active timer was shifted; false when no ordinary timer is active or the
     *         blocked duration is too small to compensate.
     */
    public boolean pauseOrdinaryPreBattleTimer(long blockedMs, String source) {
        if (blockedMs < ORDINARY_PRE_BATTLE_PAUSE_COMPENSATION_THRESHOLD_MS) {
            return false;
        }
        long oldStartedAt = shiftAtomicTimestamp(ordinaryPreBattleStartedAtMs, blockedMs);
        if (oldStartedAt <= 0L) {
            return false;
        }
        ordinaryPreBattleTimeoutPublishedAtMs.set(0L);
        long oldGateStartedAt = shiftAtomicTimestamp(ordinaryEnterBattleTargetMapGateStartedAtMs, blockedMs);
        long oldGateOpenedAt = shiftAtomicTimestamp(ordinaryEnterBattleTargetMapOpenedAtMs, blockedMs);
        log.info("[window ordinary-prebattle] timer paused: windowId={} task={} source={} blockedMs={} adjustedStartAt={} adjustedGateStartedAt={} adjustedGateOpenedAt={} previousStartAt={} previousGateStartedAt={} previousGateOpenedAt={}",
                windowId, ordinaryPreBattleTaskType, normalize(source), blockedMs,
                ordinaryPreBattleStartedAtMs.get(),
                ordinaryEnterBattleTargetMapGateStartedAtMs.get(),
                ordinaryEnterBattleTargetMapOpenedAtMs.get(),
                oldStartedAt, oldGateStartedAt, oldGateOpenedAt);
        return true;
    }

    private long shiftAtomicTimestamp(AtomicLong timestamp, long deltaMs) {
        while (true) {
            long current = timestamp.get();
            if (current <= 0L) {
                return current;
            }
            long shifted = current + deltaMs;
            if (timestamp.compareAndSet(current, shifted)) {
                return current;
            }
        }
    }

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
     * @param interest task-scoped operations and target task type.
     * @param reason diagnostic reason written to logs.
     */
    public void updateDialogInterest(WindowDialogInterest interest, String reason) {
        if (interest == null) {
            clearDialogInterest(reason);
            return;
        }
        dialogInterest.set(interest);
        long wakeSeq = observerWakeSeq.incrementAndGet();
        log.info("[latency] event=window.dialog.interest.update windowId={} task={} operations={} source={} reason={} ttl={} wakeSeq={}",
                windowId, interest.getTaskType(), interest.getOperations(), normalize(interest.getSource()),
                normalize(reason), formatDialogInterestTtl(interest), wakeSeq);
    }

    private String formatDialogInterestTtl(WindowDialogInterest interest) {
        if (interest == null || interest.getExpiresAtMs() <= 0L) {
            return "phase-owned";
        }
        return Math.max(0L, interest.getExpiresAtMs() - System.currentTimeMillis()) + "ms";
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
        PreparedDialogAction previousAction = preparedDialogAction.getAndSet(action);
        if (action != null) {
            DialogPreparationStatus previous = dialogPreparationStatus.get();
            boolean overwritten = previousAction != null && !samePreparedAction(previousAction, action);
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
            log.info("[latency] event=window.dialog.prepare.state phase=READY windowId={} hwnd={} operation={} target={} source={} requestAgeMs={} preparingAgeMs={} preparedAgeMs={} verifiedAgeMs={} matchedText={} click=({}, {}) overwritten={} previousOperation={} previousTarget={} previousSource={} previousPreparedAgeMs={} previousVerifiedAgeMs={}",
                    windowId, action.getHwnd(), action.getOperation(), action.getTargetKeyword(),
                    normalize(action.getSource()), ageMs(now, requestCreatedAtMs), ageMs(now, preparingStartedAtMs),
                    ageMs(now, action.getPreparedAtMs()), ageMs(now, action.getLastVerifiedAtMs()),
                    normalize(action.getMatchedText()), action.getAbsoluteX(), action.getAbsoluteY(),
                    overwritten,
                    previousAction == null ? null : previousAction.getOperation(),
                    previousAction == null ? null : previousAction.getTargetKeyword(),
                    previousAction == null ? null : normalize(previousAction.getSource()),
                    previousAction == null ? -1L : ageMs(now, previousAction.getPreparedAtMs()),
                    previousAction == null ? -1L : ageMs(now, previousAction.getLastVerifiedAtMs()));
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

    /**
     * Atomically consume a prepared action after optional consume-time validation.
     *
     * <p>Click-required actions validate their dialog fingerprint immediately before the CAS consume,
     * so the watcher no longer needs to keep refreshing {@code lastVerifiedAtMs} in the background.
     * No-click business signals intentionally skip fingerprint validation because their meaning is
     * carried by operation/target/window ownership, not by a button crop.</p>
     *
     * @param expectedOperation operation the caller intends to execute.
     * @param expectedTargetKeyword expected target keyword; blank means operation-only matching.
     * @param reason diagnostic reason written to logs.
     * @param validator callback that returns the action with refreshed verification metadata when
     *                  the current dialog crop still matches; null means click actions cannot be
     *                  consumed through this path.
     * @return consumed prepared action, or null when absent, mismatched, replaced, or validation
     *         fails.
     */
    public PreparedDialogAction consumePreparedDialogActionValidated(DialogOperation expectedOperation,
                                                                    String expectedTargetKeyword,
                                                                    String reason,
                                                                    PreparedDialogActionValidator validator) {
        return consumePreparedDialogActionValidated(
                expectedOperation, expectedTargetKeyword, reason, false, validator);
    }

    /**
     * Atomically consume a prepared action with route-cleared recovery plus consume-time validation.
     *
     * @param expectedOperation operation the caller intends to execute.
     * @param expectedTargetKeyword expected target keyword; blank means operation-only matching.
     * @param reason diagnostic reason written to logs.
     * @param allowClearedRouteIntent true only for route-transfer consumers that may recover an
     *                                action after the active pathing intent has been cleared.
     * @param validator callback used only for {@code clickRequired=true} actions.
     * @return consumed prepared action, or null when absent, mismatched, replaced, or validation
     *         fails.
     */
    public PreparedDialogAction consumePreparedDialogActionValidated(DialogOperation expectedOperation,
                                                                    String expectedTargetKeyword,
                                                                    String reason,
                                                                    boolean allowClearedRouteIntent,
                                                                    PreparedDialogActionValidator validator) {
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
            PreparedDialogAction consumed = current;
            if (current.isClickRequired()) {
                if (validator == null) {
                    logPreparedConsume("validation-missing", reason, current, expectedOperation, expectedTargetKeyword);
                    return null;
                }
                PreparedDialogAction validated = validator.validate(current);
                if (validated == null) {
                    if (!preparedDialogAction.compareAndSet(current, null)) {
                        continue;
                    }
                    clearReadyDialogPreparationStatusFor(current);
                    logPreparedConsume("consume-validation-failed", reason, current, expectedOperation, expectedTargetKeyword);
                    return null;
                }
                consumed = validated;
            } else {
                logPreparedConsume("no-fingerprint-validation", reason, current, expectedOperation, expectedTargetKeyword);
            }
            if (!preparedDialogAction.compareAndSet(current, null)) {
                continue;
            }
            clearReadyDialogPreparationStatusFor(current);
            logPreparedConsume(current.isClickRequired() ? "consume-validation-passed" : "consumed",
                    reason, consumed, expectedOperation, expectedTargetKeyword);
            return consumed;
        }
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

    public void setPendingSmartClickEvidenceProofToken(String proofToken) {
        pendingSmartClickEvidenceProofToken.set(normalize(proofToken));
    }

    public void clearPendingSmartClickEvidenceProofToken(String proofToken, String reason) {
        String normalizedToken = normalize(proofToken);
        if (normalizedToken == null) {
            pendingSmartClickEvidenceProofToken.set(null);
            return;
        }
        pendingSmartClickEvidenceProofToken.compareAndSet(normalizedToken, null);
    }

    /**
     * Remember a world-map route-result click until the pathing watcher proves or rejects it.
     *
     * @param memory window-relative route-result click metadata. Null clears the pending record.
     */
    public void updatePendingWorldMapRouteResultMemory(WorldMapRouteResultPendingMemory memory) {
        pendingWorldMapRouteResultMemory.set(memory);
    }

    public WorldMapRouteResultPendingMemory consumePendingWorldMapRouteResultMemory() {
        return pendingWorldMapRouteResultMemory.getAndSet(null);
    }

    public void clearPendingWorldMapRouteResultMemory(String reason) {
        pendingWorldMapRouteResultMemory.set(null);
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
     * Upgrade the active tracker intent with a target map that arrived after the green-link click.
     *
     * @param expectedIntentId active pathing intent id that the late story parse belongs to.
     * @param targetMapName target map parsed from the accept-time story objective.
     * @param reason diagnostic source for logs and the updated snapshot message.
     * @return true when the current active untargeted tracker intent was upgraded to map-targeted.
     */
    public boolean upgradeActivePathingIntentTargetMap(String expectedIntentId,
                                                       String targetMapName,
                                                       String reason) {
        String normalizedTargetMap = normalize(targetMapName);
        if (expectedIntentId == null || expectedIntentId.isBlank()
                || normalizedTargetMap == null || normalizedTargetMap.isBlank()) {
            log.info("window pathing intent target-map upgrade skipped: windowId={} reason=missing-input intentId={} targetMap={} source={}",
                    windowId, expectedIntentId, normalizedTargetMap, normalize(reason));
            return false;
        }
        while (true) {
            WindowPathingSnapshot snapshot = pathingSnapshot.get();
            if (snapshot == null || !snapshot.hasActiveIntent()) {
                log.info("window pathing intent target-map upgrade skipped: windowId={} reason=no-active-intent expectedIntentId={} targetMap={} source={}",
                        windowId, expectedIntentId, normalizedTargetMap, normalize(reason));
                return false;
            }
            WindowPathingIntent intent = snapshot.getIntent();
            if (intent == null || !Objects.equals(intent.getIntentId(), expectedIntentId)) {
                log.info("window pathing intent target-map upgrade skipped: windowId={} reason=intent-mismatch expectedIntentId={} activeIntentId={} targetMap={} source={}",
                        windowId, expectedIntentId, intent == null ? null : intent.getIntentId(),
                        normalizedTargetMap, normalize(reason));
                return false;
            }
            if (intent.getType() != WindowPathingIntentType.UNTARGETED_TRACKER) {
                log.info("window pathing intent target-map upgrade skipped: windowId={} reason=not-untargeted intentId={} type={} targetMap={} source={}",
                        windowId, expectedIntentId, intent.getType(), normalizedTargetMap, normalize(reason));
                return false;
            }
            WindowPathingIntent upgradedIntent = intent.toBuilder()
                    .type(WindowPathingIntentType.TARGETED)
                    .targetMapName(normalizedTargetMap)
                    .targetX(null)
                    .targetY(null)
                    .tolerance(0)
                    .build();
            WindowPathingSnapshot upgradedSnapshot = snapshot.toBuilder()
                    .intent(upgradedIntent)
                    .message(normalize(reason))
                    .updatedAtMs(System.currentTimeMillis())
                    .build();
            if (pathingSnapshot.compareAndSet(snapshot, upgradedSnapshot)) {
                log.info("window pathing intent target-map upgraded: windowId={} intentId={} source={} targetMap={}",
                        windowId, expectedIntentId, normalize(reason), normalizedTargetMap);
                return true;
            }
        }
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

    /**
     * Clear the current pathing signal only when its active intent belongs to the given source.
     *
     * <p>This is for task lifecycle boundaries where a specific movement owner has been superseded
     * by stronger evidence such as combat entry or verified return-home. It deliberately does not
     * clear unrelated navigation intents that might have replaced the old one between the check and
     * the cleanup attempt.</p>
     *
     * @param sourcePrefix required prefix of {@link WindowPathingIntent#getSource()}.
     * @param reason diagnostic reason written into the replacement idle snapshot.
     * @return true when a matching active pathing snapshot was cleared.
     */
    public boolean clearPathingSignalIfSourcePrefix(String sourcePrefix, String reason) {
        String normalizedSourcePrefix = normalize(sourcePrefix);
        if (normalizedSourcePrefix == null) {
            return false;
        }
        while (true) {
            WindowPathingSnapshot snapshot = pathingSnapshot.get();
            if (snapshot == null || !snapshot.hasActiveIntent()) {
                return false;
            }
            WindowPathingIntent intent = snapshot.getIntent();
            String activeSource = intent == null ? null : normalize(intent.getSource());
            if (activeSource == null || !activeSource.startsWith(normalizedSourcePrefix)) {
                return false;
            }
            WindowPathingSnapshot clearedSnapshot = WindowPathingSnapshot.builder()
                    .state(WindowPathingState.NONE)
                    .message(normalize(reason))
                    .build();
            if (!pathingSnapshot.compareAndSet(snapshot, clearedSnapshot)) {
                continue;
            }
            clearPendingTransferChoiceMemory("pathing signal cleared");
            log.info("window pathing intent cleared by source prefix: windowId={} intentId={} source={} target={} state={} reason={}",
                    windowId, intent.getIntentId(), activeSource, intent.getTargetMapName(),
                    snapshot.getState(), normalize(reason));
            return true;
        }
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
        clearTaskRunProgress();
        captureTaskOwnerIdentity();
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
        if (this.status.isTerminal()) {
            clearTaskRunProgress();
            clearIdentitySuspension("task finished");
            taskOwnerPlayerId = null;
            taskOwnerPlayerName = null;
        }
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
        if (this.status.isTerminal()) {
            clearTaskRunProgress();
        }
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
        clearOrdinaryPreBattleTimer("runtime reset");
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

    private static boolean sameNativeWindow(WindowNativeBinding left, WindowNativeBinding right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (!left.hasNativeHandle() && !right.hasNativeHandle()) {
            return true;
        }
        return Objects.equals(left.getNativeHandle(), right.getNativeHandle())
                && Objects.equals(normalize(left.getClassName()), normalize(right.getClassName()))
                && left.getProcessId() == right.getProcessId();
    }

    private WindowIdentityDrift detectIdentityDrift(WindowNativeBinding previous, WindowNativeBinding next) {
        if (previous == null
                || next == null
                || !previous.hasNativeHandle()
                || !next.hasNativeHandle()
                || !sameNativeWindow(previous, next)
                || normalize(next.getTitle()) == null) {
            return WindowIdentityDrift.none(windowId, previous, next, playerIdentityEpoch.get());
        }
        WindowTitleIdentity oldIdentity = WindowTitleIdentityParser.parse(previous.getTitle()).orElse(null);
        WindowTitleIdentity newIdentity = WindowTitleIdentityParser.parse(next.getTitle()).orElse(null);
        boolean titleChanged = !Objects.equals(normalize(previous.getTitle()), normalize(next.getTitle()));
        boolean playerChanged = newIdentity != null
                && (oldIdentity == null || !newIdentity.samePlayer(oldIdentity));
        if (!titleChanged || !playerChanged) {
            return WindowIdentityDrift.none(windowId, previous, next, playerIdentityEpoch.get());
        }
        long epoch = playerIdentityEpoch.incrementAndGet();
        return WindowIdentityDrift.detected(windowId, previous, next, oldIdentity, newIdentity, epoch);
    }

    private void applyParsedIdentity(WindowTitleIdentity identity) {
        if (identity == null) {
            return;
        }
        visiblePlayerId = normalize(identity.playerId());
        visiblePlayerName = normalize(identity.playerName());
        PlayerCharacter me = gameState.getMe();
        if (me == null) {
            me = new PlayerCharacter();
            gameState.setMe(me);
        }
        me.setGameServerName(identity.server());
        me.setName(identity.playerName());
        me.setId(identity.playerId());
    }

    private void clearPlayerScopedRuntimeState(String reason) {
        clearPlayerScopedTransientState(reason);
        clearTaskRunProgress();
        gameState.resetRuntimeState();
    }

    private void clearPlayerScopedTransientState(String reason) {
        clearPathingSignal(reason);
        clearOrdinaryPreBattleTimer(reason);
        clearVisibleDialogSnapshot(reason);
        clearDialogPreparationRequest(reason);
        clearDialogInterest(reason);
        pendingTransferChoiceMemory.set(null);
        pendingWorldMapRouteResultMemory.set(null);
        leftTopStatusSwitchClosePending.set(null);
    }

    private void captureTaskOwnerIdentity() {
        WindowTitleIdentity identity = WindowTitleIdentityParser.parse(nativeBinding.getTitle()).orElse(null);
        if (identity == null) {
            PlayerCharacter me = gameState.getMe();
            if (me != null && normalize(me.getId()) != null) {
                taskOwnerPlayerId = normalize(me.getId());
                taskOwnerPlayerName = normalize(me.getName());
                visiblePlayerId = taskOwnerPlayerId;
                visiblePlayerName = taskOwnerPlayerName;
            }
            return;
        }
        taskOwnerPlayerId = normalize(identity.playerId());
        taskOwnerPlayerName = normalize(identity.playerName());
        visiblePlayerId = taskOwnerPlayerId;
        visiblePlayerName = taskOwnerPlayerName;
        clearIdentitySuspension("task owner captured");
    }

    private void updateIdentitySuspension(WindowTitleIdentity oldIdentity, WindowTitleIdentity newIdentity) {
        if (newIdentity == null || normalize(newIdentity.playerId()) == null) {
            return;
        }
        if (taskOwnerPlayerId == null && oldIdentity != null) {
            taskOwnerPlayerId = normalize(oldIdentity.playerId());
            taskOwnerPlayerName = normalize(oldIdentity.playerName());
        }
        visiblePlayerId = normalize(newIdentity.playerId());
        visiblePlayerName = normalize(newIdentity.playerName());
        if (taskOwnerPlayerId == null || Objects.equals(taskOwnerPlayerId, visiblePlayerId)) {
            clearIdentitySuspension("task owner visible");
            return;
        }
        if (!identitySuspended) {
            identitySuspended = true;
            lastMessage = "等待切回任务角色：" + formatPlayer(taskOwnerPlayerName, taskOwnerPlayerId);
            log.warn("[window identity suspend] task owner not visible: windowId={} owner={}/{} visible={}/{}",
                    windowId, taskOwnerPlayerName, taskOwnerPlayerId, visiblePlayerName, visiblePlayerId);
        }
    }

    private synchronized void clearIdentitySuspension(String reason) {
        if (identitySuspended) {
            log.info("[window identity suspend] cleared: windowId={} owner={}/{} visible={}/{} reason={}",
                    windowId, taskOwnerPlayerName, taskOwnerPlayerId, visiblePlayerName, visiblePlayerId,
                    normalize(reason));
        }
        identitySuspended = false;
        notifyAll();
    }

    private String formatPlayer(String name, String id) {
        String normalizedName = normalize(name);
        String normalizedId = normalize(id);
        if (normalizedName == null) {
            return normalizedId == null ? "unknown" : normalizedId;
        }
        return normalizedId == null ? normalizedName : normalizedName + "/" + normalizedId;
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

    private boolean samePreparedAction(PreparedDialogAction left, PreparedDialogAction right) {
        return left != null
                && right != null
                && Objects.equals(left.getWindowId(), right.getWindowId())
                && Objects.equals(left.getHwnd(), right.getHwnd())
                && Objects.equals(left.getIntentId(), right.getIntentId())
                && left.getOperation() == right.getOperation()
                && Objects.equals(left.getTargetKeyword(), right.getTargetKeyword())
                && Objects.equals(normalize(left.getSource()), normalize(right.getSource()))
                && left.getAbsoluteX() == right.getAbsoluteX()
                && left.getAbsoluteY() == right.getAbsoluteY();
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

    public interface PreparedDialogActionValidator {
        PreparedDialogAction validate(PreparedDialogAction action);
    }
}
