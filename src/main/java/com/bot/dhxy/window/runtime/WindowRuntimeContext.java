package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.DialogPreparationRequest;
import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogPreparationStatus;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.job.PreparedActionJob;
import com.bot.dhxy.model.job.PreparedActionJobType;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.navigation.PendingTransferChoiceMemory;
import com.bot.dhxy.model.navigation.PendingRouteOutcome;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelCacheEntry;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelNegativeResult;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowRetainedReturnHomeReplay;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.model.WindowTaskRunProgress;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.window.model.WindowFlyingState;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private static final long EXPECTED_BATTLE_ENTRY_TICKET_MAX_AGE_MS = 10_000L;
    private static final long ORDINARY_PRE_BATTLE_PAUSE_COMPENSATION_THRESHOLD_MS = 500L;
    private static final long XIULUO_LOCAL_KANDA_CONFIRM_WINDOW_MS = 4_000L;
    private static final int MAX_XIULUO_LOCAL_KANDA_CLICKS = 3;


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
    /** Last logical coordinate resolved by Runner, retained across intent clears as the next leg's baseline. */
    private final AtomicReference<WindowPathingSnapshot> lastKnownPathingLocation = new AtomicReference<>();
    private final AtomicReference<WindowDialogSnapshot> visibleDialogSnapshot = new AtomicReference<>();
    /** Latest shared-frame structural dialog observation, used only to stop a local tracker-link retry. */
    private final AtomicLong dialogFrameObservedAtMs = new AtomicLong();
    private final AtomicLong storyDialogVisibleSequence = new AtomicLong();
    private final AtomicReference<WindowDialogInterest> dialogInterest = new AtomicReference<>();
    private final AtomicLong observerWakeSeq = new AtomicLong();
    private final AtomicLong observationPathingFactResetGeneration = new AtomicLong();
    private final AtomicReference<DialogPreparationRequest> dialogPreparationRequest = new AtomicReference<>();
    private final AtomicReference<PreparedDialogAction> preparedDialogAction = new AtomicReference<>();
    // CR253: 修罗 green-chain typed prepared jobs plus the attempt identity they are gated on.
    private final AtomicReference<XiuluoGreenChainSchedule> xiuluoGreenChainSchedule = new AtomicReference<>();
    // TURN-40G: attempt-scoped one-shot local-kanda click claim (winner's attemptId; reset with the schedule).
    private final AtomicReference<String> xiuluoEnterBattleClickClaim = new AtomicReference<>();
    // A physical 看打 click is not a battle confirmation. This keeps the bounded local confirmation/retry state
    // attached to the exact schedule, while Cloud remains the only owner of a later green-link fallback.
    private final AtomicReference<XiuluoLocalKandaClickProgress> xiuluoLocalKandaClickProgress = new AtomicReference<>();
    /** Exact attempt whose terminal pathing ended before any local 看打 template became available. */
    private final AtomicReference<String> xiuluoMissingKandaAfterPathingTerminalClaim = new AtomicReference<>();
    /** Exact local-kanda attempt whose IN_COMBAT edge won before a concurrent recovery reset. */
    private final AtomicReference<XiuluoConfirmedCombatAttempt> xiuluoConfirmedCombatAttempt =
            new AtomicReference<>();
    // TURN-40G review#3 P1: single monitor for every transition of the paired xiuluo probe state — a probe-only
    // interest installed together with its green-chain schedule, schedule open/replace/clear, and the one-shot
    // click claim all mutate under this lock, and the observation sampler snapshots the pair under the same lock.
    // A reader can therefore never observe a new interest paired with the previous attempt's schedule (or the
    // reverse) in the middle of an attempt replacement.
    private final Object xiuluoKandaTransitionLock = new Object();
    private final ConcurrentHashMap<PreparedActionJobType, PreparedActionJob> preparedActionJobs =
            new ConcurrentHashMap<>();
    private final AtomicReference<PendingTransferChoiceMemory> pendingTransferChoiceMemory = new AtomicReference<>();
    private final AtomicReference<PendingRouteOutcome> pendingRouteOutcome =
            new AtomicReference<>();
    private final ConcurrentLinkedQueue<PendingRouteOutcomeAbandonment> pendingRouteOutcomeAbandonments =
            new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PendingRouteOutcomeReplacement> pendingRouteOutcomeReplacements =
            new ConcurrentLinkedQueue<>();
    private final AtomicReference<TaskTrackerPanelCacheEntry> taskTrackerPanelCache = new AtomicReference<>();
    private final AtomicReference<WindowTrackerAnchorMemory> taskTrackerAnchorMemory = new AtomicReference<>();
    private final AtomicReference<TaskTrackerPanelNegativeResult> taskTrackerPanelNegativeResult =
            new AtomicReference<>();
    private final AtomicLong taskTrackerPanelNegativeSequence = new AtomicLong();
    private final AtomicReference<String> pendingSmartClickEvidenceProofToken = new AtomicReference<>();
    /** G005: identity of the 天庭 dialog instance this window has already answered; null re-arms it. */
    private final AtomicReference<String> tiantingDialogOptionClaim = new AtomicReference<>();
    /**
     * The 多谢 click that opened a still-unconsumed 使用封妖符 follow-up.
     *
     * <p>This is action-owned state rather than a wall-clock window. Observation sampling includes a
     * synchronous HTTP round trip, so even a nominal one-second probe can legitimately take longer than
     * the former 2.5-second deadline to see its next frame.</p>
     */
    private final AtomicReference<String> tiantingFengyaoPending = new AtomicReference<>();
    private final AtomicReference<String> leftTopStatusSwitchClosePending = new AtomicReference<>();
    /** Last successful local-only maintenance option click by action key, retained for diagnostics/acceptance. */
    private final ConcurrentHashMap<String, Long> localMaintenanceBroadcastHandledAtByAction =
            new ConcurrentHashMap<>();
    private final AtomicReference<WindowRetainedReturnHomeReplay> retainedReturnHomeReplay =
            new AtomicReference<>();
    private final AtomicLong returnHomeReplayLifecycleGeneration = new AtomicLong();
    private final AtomicReference<WindowExpectedCombatEnterClaim> expectedCombatEnterClaim =
            new AtomicReference<>();
    /** A Cloud-correlated direct-combat ticket; it is not expected until the marked local click succeeds. */
    private final AtomicReference<PendingDirectCombatEnterTicket> pendingDirectCombatEnterClaim =
            new AtomicReference<>();
    private final AtomicLong localCombatGeneration = new AtomicLong();
    private volatile boolean localCombatVisible;
    private final AtomicReference<String> taskQueueStartupPreparationDone = new AtomicReference<>();
    private final AtomicReference<TaskQueueStartupUiCleanupProbe> taskQueueStartupUiCleanupProbe =
            new AtomicReference<>();
    private final AtomicReference<WindowFlyingState> taskQueueStartupFlyingState = new AtomicReference<>();
    private final AtomicReference<String> taskQueueStartupFlyingStateSource = new AtomicReference<>();
    private final AtomicReference<DialogPreparationStatus> dialogPreparationStatus =
            new AtomicReference<>(DialogPreparationStatus.none());
    /** Current accepted run's numeric progress; null when the task has no finite counter. */
    private final AtomicReference<WindowTaskRunProgress> runningTaskProgress = new AtomicReference<>();
    /** Only the cumulative counter retained across a user PAUSE -> PAUSE_RESUME boundary. */
    private final AtomicReference<WindowTaskRunProgress> pausedTaskRunProgress = new AtomicReference<>();
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
    /*
     * 2026-08-18 切号事故:终局抹除 taskOwner 后,自动重启的新 run 会把"窗口里现在是谁"直接
     * 认作新主人——用户契约是切走=暂停、切回主人才继续。终局抹除时把主人侧存到 lastTaskOwner*,
     * 供 WindowTaskControlService 自动重启守门;手动新开任务照常按当前标题认主(意图明确)。
     */
    private volatile String lastTaskOwnerPlayerId;
    private volatile String lastTaskOwnerPlayerName;
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

    public void recordLocalMaintenanceBroadcastHandled(String actionKey, long handledAtMs) {
        String normalizedActionKey = normalize(actionKey);
        if (normalizedActionKey != null && handledAtMs > 0L) {
            localMaintenanceBroadcastHandledAtByAction.put(normalizedActionKey, handledAtMs);
        }
    }

    public long getLocalMaintenanceBroadcastHandledAt(String actionKey) {
        String normalizedActionKey = normalize(actionKey);
        if (normalizedActionKey == null) {
            return 0L;
        }
        return localMaintenanceBroadcastHandledAtByAction.getOrDefault(normalizedActionKey, 0L);
    }

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
        // A capture may refresh the same live HWND snapshot between two input fragments of one turn.
        // Preserve object identity for a byte-for-byte equivalent snapshot so the exact-generation
        // input fence rejects only a real binding change, not a harmless refresh allocation.
        if (sameNativeBindingSnapshot(previous, next)) {
            return WindowIdentityDrift.none(windowId, previous, previous, playerIdentityEpoch.get());
        }
        WindowIdentityDrift drift = detectIdentityDrift(previous, next);
        boolean hardNativeChange = !sameNativeWindow(previous, next);
        if (hardNativeChange) {
            if (previous != null && previous.hasNativeHandle() && next.hasNativeHandle()) {
                playerIdentityEpoch.incrementAndGet();
            }
            clearPlayerScopedRuntimeState("native binding changed");
            invalidateReturnHomeReplayLifecycle("native binding changed");
            clearExpectedCombatEnterClaim("native binding changed");
            clearIdentitySuspension("native binding changed");
            WindowTitleIdentityParser.parse(next.getTitle()).ifPresent(this::applyParsedIdentity);
        } else if (isIdentityEnrichment(previous, next)) {
            // The same live HWND may first expose a short generic title and only later expose the
            // player suffix. That enriches an unknown identity; it is not a player replacement.
            WindowTitleIdentityParser.parse(next.getTitle()).ifPresent(this::applyParsedIdentity);
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

    public void retainReturnHomeReplay(WindowRetainedReturnHomeReplay replay) {
        retainedReturnHomeReplay.set(Objects.requireNonNull(replay, "replay"));
        log.info("[local-runner] retained post-combat return replay: windowId={} task={} observationRunId={} businessTaskRunId={} intent={} hwnd={}",
                windowId, replay.taskCode(), replay.observationRunId(), replay.businessTaskRunId(),
                replay.arguments().intent(), replay.sourceHwnd());
    }

    public ReplayArmResult armRetainedReturnHomeReplay(
            String taskCode,
            String observationRunId,
            String businessTaskRunId,
            String exactWindowId,
            String exactHwnd) {
        while (true) {
            WindowRetainedReturnHomeReplay current = retainedReturnHomeReplay.get();
            if (current == null) {
                return ReplayArmResult.NO_RETAINED_COMMAND;
            }
            if (!current.taskCode().equalsIgnoreCase(taskCode)
                    || !current.observationRunId().equals(observationRunId)
                    || !current.businessTaskRunId().equals(businessTaskRunId)
                    || !current.windowId().equals(exactWindowId)
                    || !current.sourceHwnd().equals(exactHwnd)) {
                return ReplayArmResult.IDENTITY_REJECTED;
            }
            if (current.state() == WindowRetainedReturnHomeReplay.State.ARMED
                    || current.state() == WindowRetainedReturnHomeReplay.State.REPLAYING) {
                return ReplayArmResult.ARMED;
            }
            if (retainedReturnHomeReplay.compareAndSet(
                    current, current.withState(WindowRetainedReturnHomeReplay.State.ARMED))) {
                return ReplayArmResult.ARMED;
            }
        }
    }

    public ReplayClaim claimArmedReturnHomeReplay(
            String taskCode,
            String observationRunId,
            String businessTaskRunId,
            String exactWindowId,
            String exactHwnd) {
        while (true) {
            WindowRetainedReturnHomeReplay current = retainedReturnHomeReplay.get();
            if (current == null || current.state() != WindowRetainedReturnHomeReplay.State.ARMED) {
                return ReplayClaim.none();
            }
            if (!current.taskCode().equalsIgnoreCase(taskCode)
                    || !current.observationRunId().equals(observationRunId)
                    || !current.businessTaskRunId().equals(businessTaskRunId)
                    || !current.windowId().equals(exactWindowId)
                    || !current.sourceHwnd().equals(exactHwnd)) {
                return ReplayClaim.rejected(current);
            }
            WindowRetainedReturnHomeReplay replaying =
                    current.withState(WindowRetainedReturnHomeReplay.State.REPLAYING);
            if (retainedReturnHomeReplay.compareAndSet(current, replaying)) {
                return ReplayClaim.claimed(replaying);
            }
        }
    }

    public boolean hasArmedReturnHomeReplay(
            String taskCode,
            String observationRunId,
            String businessTaskRunId) {
        WindowRetainedReturnHomeReplay current = retainedReturnHomeReplay.get();
        return current != null
                && current.state() == WindowRetainedReturnHomeReplay.State.ARMED
                && current.taskCode().equalsIgnoreCase(taskCode)
                && current.observationRunId().equals(observationRunId)
                && current.businessTaskRunId().equals(businessTaskRunId)
                && current.windowId().equals(windowId);
    }

    /**
     * Returns the armed replay for this exact observation run without claiming it.
     *
     * @param observationRunId client observation-run identity; must be non-blank and exact.
     * @return the immutable armed replay, or {@code null} when no exact replay is armed.
     */
    public WindowRetainedReturnHomeReplay currentArmedReturnHomeReplay(String observationRunId) {
        if (observationRunId == null || observationRunId.isBlank()) {
            return null;
        }
        WindowRetainedReturnHomeReplay current = retainedReturnHomeReplay.get();
        return current != null
                && current.state() == WindowRetainedReturnHomeReplay.State.ARMED
                && current.observationRunId().equals(observationRunId)
                && current.windowId().equals(windowId)
                ? current
                : null;
    }

    public long currentReturnHomeReplayLifecycleGeneration() {
        return returnHomeReplayLifecycleGeneration.get();
    }

    public boolean isReturnHomeReplayActive(WindowRetainedReturnHomeReplay replay) {
        if (replay == null
                || replay.lifecycleGeneration() != returnHomeReplayLifecycleGeneration.get()) {
            return false;
        }
        WindowRetainedReturnHomeReplay current = retainedReturnHomeReplay.get();
        return current != null
                && current.state() == WindowRetainedReturnHomeReplay.State.REPLAYING
                && current.tokenId().equals(replay.tokenId())
                && current.observationRunId().equals(replay.observationRunId())
                && current.businessTaskRunId().equals(replay.businessTaskRunId())
                && current.taskCode().equalsIgnoreCase(replay.taskCode())
                && current.windowId().equals(replay.windowId());
    }

    public boolean completeRetainedReturnHomeReplay(
            WindowRetainedReturnHomeReplay replay,
            String reason) {
        if (replay == null) {
            return false;
        }
        while (true) {
            WindowRetainedReturnHomeReplay current = retainedReturnHomeReplay.get();
            if (current == null
                    || !current.tokenId().equals(replay.tokenId())
                    || current.lifecycleGeneration() != replay.lifecycleGeneration()
                    || !current.observationRunId().equals(replay.observationRunId())
                    || !current.businessTaskRunId().equals(replay.businessTaskRunId())) {
                return false;
            }
            if (retainedReturnHomeReplay.compareAndSet(current, null)) {
                log.info("[local-runner] completed exact retained replay: windowId={} tokenId={} task={} observationRunId={} businessTaskRunId={} reason={}",
                        windowId, replay.tokenId(), replay.taskCode(), replay.observationRunId(),
                        replay.businessTaskRunId(), normalize(reason));
                return true;
            }
        }
    }

    public long invalidateReturnHomeReplayLifecycle(String reason) {
        long generation = returnHomeReplayLifecycleGeneration.incrementAndGet();
        WindowRetainedReturnHomeReplay cleared = retainedReturnHomeReplay.getAndSet(null);
        log.info("[local-runner] invalidated return replay lifecycle: windowId={} generation={} tokenId={} reason={}",
                windowId, generation, cleared == null ? null : cleared.tokenId(), normalize(reason));
        return generation;
    }

    public void clearRetainedReturnHomeReplay(String reason) {
        WindowRetainedReturnHomeReplay cleared = retainedReturnHomeReplay.getAndSet(null);
        if (cleared != null) {
            log.info("[local-runner] cleared retained post-combat return replay: windowId={} task={} observationRunId={} businessTaskRunId={} reason={}",
                    windowId, cleared.taskCode(), cleared.observationRunId(),
                    cleared.businessTaskRunId(), normalize(reason));
        }
    }

    public boolean registerExpectedCombatEnterClaim(WindowExpectedCombatEnterClaim claim) {
        Objects.requireNonNull(claim, "claim");
        WindowNativeBinding binding = nativeBinding;
        if (!windowId.equals(claim.windowId())
                || binding == null
                || !binding.hasNativeHandle()
                || !binding.getNativeHandle().equals(claim.hwnd())
                || (!"local-template".equals(claim.source())
                && !"local-alt-a".equals(claim.source())
                && !"cloud-task-dialog".equals(claim.source()))) {
            return false;
        }
        WindowExpectedCombatEnterClaim stored = localCombatVisible && localCombatGeneration.get() > 0L
                ? claim.bind(localCombatGeneration.get()) : claim;
        expectedCombatEnterClaim.set(stored);
        return true;
    }

    /** Stores an exact direct-combat ticket until its marked task-owned target click has actually completed. */
    public boolean armPendingDirectCombatEnterClaim(WindowExpectedCombatEnterClaim claim) {
        Objects.requireNonNull(claim, "claim");
        WindowNativeBinding binding = nativeBinding;
        if (!windowId.equals(claim.windowId()) || binding == null || !binding.hasNativeHandle()
                || !binding.getNativeHandle().equals(claim.hwnd())
                || (!"local-alt-a".equals(claim.source())
                && !"cloud-task-dialog".equals(claim.source()))) {
            return false;
        }
        pendingDirectCombatEnterClaim.set(new PendingDirectCombatEnterTicket(claim, System.currentTimeMillis()));
        return true;
    }

    /** Consumes the pending ticket only after the exact marked target click completed. */
    public boolean consumePendingDirectCombatEnterClaim(String claimId) {
        PendingDirectCombatEnterTicket ticket = currentPendingDirectCombatEnterTicket();
        if (ticket == null || !ticket.claim().claimId().equals(claimId)
                || !pendingDirectCombatEnterClaim.compareAndSet(ticket, null)) {
            return false;
        }
        return registerExpectedCombatEnterClaim(ticket.claim());
    }

    public WindowExpectedCombatEnterClaim currentPendingDirectCombatEnterClaim() {
        PendingDirectCombatEnterTicket ticket = currentPendingDirectCombatEnterTicket();
        return ticket == null ? null : ticket.claim();
    }

    private PendingDirectCombatEnterTicket currentPendingDirectCombatEnterTicket() {
        while (true) {
            PendingDirectCombatEnterTicket ticket = pendingDirectCombatEnterClaim.get();
            if (ticket == null) {
                return null;
            }
            long ageMs = System.currentTimeMillis() - ticket.armedAtMs();
            if (ageMs >= 0L && ageMs <= EXPECTED_BATTLE_ENTRY_TICKET_MAX_AGE_MS) {
                return ticket;
            }
            if (pendingDirectCombatEnterClaim.compareAndSet(ticket, null)) {
                log.info("[local-runner] cleared expired direct-combat enter ticket: windowId={} claimId={} ageMs={}",
                        windowId, ticket.claim().claimId(), ageMs);
                return null;
            }
        }
    }

    public void clearPendingDirectCombatEnterClaim(String reason) {
        pendingDirectCombatEnterClaim.set(null);
    }

    public void updateLocalCombatGeneration(long generation, boolean visible) {
        if (generation > 0L) {
            localCombatGeneration.set(generation);
        }
        localCombatVisible = visible;
    }

    /**
     * Returns the latest local Runner combat fact for this exact window.
     *
     * <p>This is deliberately a local safety fact, not a Cloud reclassification: physical mouse actions must
     * stop as soon as the Runner sees combat, even while the corresponding observation is still in flight to
     * Cloud.</p>
     *
     * @return {@code true} after the local Runner has observed combat entry and before it observes exit.
     */
    public boolean isLocalCombatVisible() {
        return localCombatVisible;
    }

    public WindowExpectedCombatEnterClaim bindExpectedCombatEnterClaim(
            String observationRunId, String taskCode, long combatGeneration) {
        while (true) {
            WindowExpectedCombatEnterClaim current = expectedCombatEnterClaim.get();
            if (current == null || current.combatGeneration() != null
                    || !current.observationRunId().equals(observationRunId)
                    || !current.taskCode().equalsIgnoreCase(taskCode)
                    || !current.windowId().equals(windowId)
                    || !isCurrentLocalTemplateClaim(current)) {
                return null;
            }
            WindowExpectedCombatEnterClaim bound = current.bind(combatGeneration);
            if (expectedCombatEnterClaim.compareAndSet(current, bound)) {
                return bound;
            }
        }
    }

    public WindowExpectedCombatEnterClaim bindExpectedCombatEnterClaim(
            String observationRunId, long combatGeneration) {
        while (true) {
            WindowExpectedCombatEnterClaim current = expectedCombatEnterClaim.get();
            if (current == null || current.combatGeneration() != null
                    || !current.observationRunId().equals(observationRunId)
                    || !current.windowId().equals(windowId)
                    || !isCurrentLocalTemplateClaim(current)) {
                return null;
            }
            WindowExpectedCombatEnterClaim bound = current.bind(combatGeneration);
            if (expectedCombatEnterClaim.compareAndSet(current, bound)) {
                return bound;
            }
        }
    }

    public WindowExpectedCombatEnterClaim currentExpectedCombatEnterClaim(
            String observationRunId, String taskCode, long combatGeneration) {
        WindowExpectedCombatEnterClaim current = expectedCombatEnterClaim.get();
        return current != null
                && Objects.equals(current.combatGeneration(), combatGeneration)
                && current.observationRunId().equals(observationRunId)
                && current.taskCode().equalsIgnoreCase(taskCode)
                && current.windowId().equals(windowId)
                ? current : null;
    }

    public WindowExpectedCombatEnterClaim currentExpectedCombatEnterClaim(
            String observationRunId, long combatGeneration) {
        WindowExpectedCombatEnterClaim current = expectedCombatEnterClaim.get();
        return current != null
                && Objects.equals(current.combatGeneration(), combatGeneration)
                && current.observationRunId().equals(observationRunId)
                && current.windowId().equals(windowId)
                ? current : null;
    }

    public void clearExpectedCombatEnterClaim(String reason) {
        WindowExpectedCombatEnterClaim cleared;
        synchronized (xiuluoKandaTransitionLock) {
            cleared = expectedCombatEnterClaim.getAndSet(null);
            XiuluoConfirmedCombatAttempt confirmed = xiuluoConfirmedCombatAttempt.get();
            if (cleared != null && confirmed != null
                    && confirmed.matches(cleared.businessTaskRunId(), confirmed.round(), cleared.attemptId())) {
                xiuluoConfirmedCombatAttempt.compareAndSet(confirmed, null);
            }
        }
        if (cleared != null) {
            log.info("[local-runner] cleared expected combat enter claim: windowId={} claimId={} observationRunId={} businessTaskRunId={} reason={}",
                    windowId, cleared.claimId(), cleared.observationRunId(),
                    cleared.businessTaskRunId(), normalize(reason));
        }
    }

    private void clearUnboundExpectedCombatClaimForReplacedAttempt(
            XiuluoGreenChainSchedule schedule,
            String reason) {
        while (true) {
            WindowExpectedCombatEnterClaim current = expectedCombatEnterClaim.get();
            if (current == null
                    || current.combatGeneration() != null
                    || (!"XIULUO_V2".equalsIgnoreCase(current.taskCode())
                    && !"XINSHOU_TRAINING".equalsIgnoreCase(current.taskCode())
                    && !"CATCH_GHOST".equalsIgnoreCase(current.taskCode())
                    && !"GHOST_KING".equalsIgnoreCase(current.taskCode()))
                    || !current.businessTaskRunId().equals(schedule.getTaskRunId())
                    || Objects.equals(current.attemptId(), schedule.getAttemptId())) {
                return;
            }
            if (expectedCombatEnterClaim.compareAndSet(current, null)) {
                log.info("[local-runner] cleared stale expected enter claim on attempt replacement: windowId={} claimId={} businessTaskRunId={} oldAttemptId={} newAttemptId={} reason={}",
                        windowId, current.claimId(), current.businessTaskRunId(), current.attemptId(),
                        schedule.getAttemptId(), normalize(reason));
                return;
            }
        }
    }

    public enum ReplayArmResult {
        ARMED,
        NO_RETAINED_COMMAND,
        IDENTITY_REJECTED
    }

    public record ReplayClaim(ReplayClaimStatus status, WindowRetainedReturnHomeReplay replay) {
        static ReplayClaim none() {
            return new ReplayClaim(ReplayClaimStatus.NONE, null);
        }

        static ReplayClaim claimed(WindowRetainedReturnHomeReplay replay) {
            return new ReplayClaim(ReplayClaimStatus.CLAIMED, replay);
        }

        static ReplayClaim rejected(WindowRetainedReturnHomeReplay replay) {
            return new ReplayClaim(ReplayClaimStatus.IDENTITY_REJECTED, replay);
        }
    }

    public enum ReplayClaimStatus {
        NONE,
        CLAIMED,
        IDENTITY_REJECTED
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
     * Records a read-only structural dialog-frame observation from this bound window's shared capture.
     * It is intentionally separate from {@link #visibleDialogSnapshot}: no local component infers the
     * dialog type or business meaning from this stop-retry safety fact.
     */
    public void markDialogFrameObserved(long observedAtMs) {
        dialogFrameObservedAtMs.accumulateAndGet(observedAtMs, Math::max);
    }

    /** @return latest structural dialog-frame observation time, or zero when none has been recorded. */
    public long getDialogFrameObservedAtMs() {
        return dialogFrameObservedAtMs.get();
    }

    /**
     * Returns whether the bound window's shared capture has structurally shown a dialog recently.
     * This remains a local input-suppression fact only; it does not identify or interpret the dialog.
     */
    public boolean hasRecentDialogFrameObservation(long maxAgeMs) {
        long observedAtMs = dialogFrameObservedAtMs.get();
        return observedAtMs > 0L && maxAgeMs > 0L
                && Math.max(0L, System.currentTimeMillis() - observedAtMs) <= maxAgeMs;
    }

    /** Monotonic edge sequence for transitions into a visible story dialog. */
    public long getStoryDialogVisibleSequence() {
        return storyDialogVisibleSequence.get();
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

    public PendingRouteOutcome getPendingRouteOutcome() {
        return pendingRouteOutcome.get();
    }

    public TaskTrackerPanelCacheEntry getTaskTrackerPanelCache() {
        return taskTrackerPanelCache.get();
    }

    /** @return the last window-relative tracker anchor for this exact runtime, or empty. */
    public Optional<WindowTrackerAnchorMemory> getTaskTrackerAnchorMemory() {
        return Optional.ofNullable(taskTrackerAnchorMemory.get());
    }

    /** @param anchor window-relative tracker anchor, or null to clear the exact-window cache. */
    public void setTaskTrackerAnchorMemory(WindowTrackerAnchorMemory anchor) {
        taskTrackerAnchorMemory.set(anchor);
    }

    public TaskTrackerPanelNegativeResult getTaskTrackerPanelNegativeResult() {
        return taskTrackerPanelNegativeResult.get();
    }

    /**
     * Store the latest successful tracker-panel parse for this runtime only.
     *
     * @param entry window-relative task-tracker cache entry; null clears the cache.
     */
    public void updateTaskTrackerPanelCache(TaskTrackerPanelCacheEntry entry) {
        taskTrackerPanelCache.set(entry);
    }

    /**
     * Clear the task-tracker panel cache when the runtime/window identity no longer owns it.
     *
     * @param reason diagnostic reason written to logs; nullable.
     */
    public void clearTaskTrackerPanelCache(String reason) {
        TaskTrackerPanelCacheEntry cleared = taskTrackerPanelCache.getAndSet(null);
        if (cleared != null) {
            log.info("[task-tracker cache] cleared: windowId={} taskCode={} source={} reason={}",
                    windowId, cleared.getTaskCode(), cleared.getSource(), normalize(reason));
        }
    }

    /**
     * Store the latest Runner-owned tracker negative for this runtime.
     *
     * @param result fresh tracker no-action result; null clears the current negative.
     */
    public void updateTaskTrackerPanelNegativeResult(TaskTrackerPanelNegativeResult result) {
        if (result == null) {
            clearTaskTrackerPanelNegativeResult("null tracker negative");
            return;
        }
        long now = System.currentTimeMillis();
        TaskTrackerPanelNegativeResult stored = result.toBuilder()
                .windowId(windowId)
                .observedAtMs(result.getObservedAtMs() > 0L ? result.getObservedAtMs() : now)
                .sequence(taskTrackerPanelNegativeSequence.incrementAndGet())
                .build();
        taskTrackerPanelNegativeResult.set(stored);
        log.info("[task-tracker negative] updated: windowId={} taskType={} taskCode={} status={} source={} reason={} sequence={} ageMs={}",
                windowId, stored.getTaskType(), stored.getTaskCode(), stored.getStatus(),
                normalize(stored.getSource()), normalize(stored.getReason()), stored.getSequence(),
                ageMs(now, stored.getObservedAtMs()));
    }

    /**
     * Atomically consume a fresh tracker negative only for the expected task/window.
     *
     * @param expectedTaskType task type that is allowed to consume this negative.
     * @param expectedTaskCode tracker-reader task code such as {@code wuhuan}.
     * @param maxAgeMs maximum accepted age in milliseconds; negative disables freshness check.
     * @param reason diagnostic reason written to logs.
     * @return consumed negative result, or null when absent, stale, or mismatched.
     */
    public TaskTrackerPanelNegativeResult consumeFreshTaskTrackerPanelNegativeResult(TaskType expectedTaskType,
                                                                                     String expectedTaskCode,
                                                                                     long maxAgeMs,
                                                                                     String reason) {
        return consumeFreshTaskTrackerPanelNegativeResult(
                expectedTaskType, expectedTaskCode, maxAgeMs, reason, null, null, -1L);
    }

    /**
     * Atomically consume a fresh tracker negative unless a fresher higher-priority prepared action
     * is currently ready for this window.
     *
     * @param expectedTaskType task type that is allowed to consume this negative.
     * @param expectedTaskCode tracker-reader task code such as {@code wuhuan}.
     * @param maxAgeMs maximum accepted age in milliseconds; negative disables freshness check.
     * @param reason diagnostic reason written to logs.
     * @param allowedPreparedOperation prepared action operation that may coexist with this negative.
     * @param allowedPreparedTargetKeyword prepared action target that may coexist with this negative.
     * @param preparedMaxAgeMs maximum accepted age for the blocking prepared action; negative disables
     *                         the prepared-action block.
     * @return consumed negative result, or null when absent, stale, mismatched, or preempted.
     */
    public TaskTrackerPanelNegativeResult consumeFreshTaskTrackerPanelNegativeResult(TaskType expectedTaskType,
                                                                                     String expectedTaskCode,
                                                                                     long maxAgeMs,
                                                                                     String reason,
                                                                                     DialogOperation allowedPreparedOperation,
                                                                                     String allowedPreparedTargetKeyword,
                                                                                     long preparedMaxAgeMs) {
        while (true) {
            TaskTrackerPanelNegativeResult current = taskTrackerPanelNegativeResult.get();
            if (current == null) {
                return null;
            }
            if (!current.matches(windowId, expectedTaskType, expectedTaskCode)) {
                logTaskTrackerPanelNegativeConsume("mismatch", reason, current, expectedTaskType, expectedTaskCode);
                return null;
            }
            if (!current.freshWithin(System.currentTimeMillis(), maxAgeMs)) {
                if (!taskTrackerPanelNegativeResult.compareAndSet(current, null)) {
                    continue;
                }
                logTaskTrackerPanelNegativeConsume("stale", reason, current, expectedTaskType, expectedTaskCode);
                return null;
            }
            if (freshPreparedActionBlocksTrackerNegative(
                    allowedPreparedOperation, allowedPreparedTargetKeyword, preparedMaxAgeMs) != null) {
                logTaskTrackerPanelNegativeConsume(
                        "prepared-blocked", reason, current, expectedTaskType, expectedTaskCode);
                return null;
            }
            if (!taskTrackerPanelNegativeResult.compareAndSet(current, null)) {
                continue;
            }
            logTaskTrackerPanelNegativeConsume("consumed", reason, current, expectedTaskType, expectedTaskCode);
            return current;
        }
    }

    private PreparedDialogAction freshPreparedActionBlocksTrackerNegative(DialogOperation allowedPreparedOperation,
                                                                         String allowedPreparedTargetKeyword,
                                                                         long preparedMaxAgeMs) {
        if (allowedPreparedOperation == null || preparedMaxAgeMs < 0L) {
            return null;
        }
        PreparedDialogAction action = preparedDialogAction.get();
        if (action == null || !preparedActionMatchesCurrentWindow(action)) {
            return null;
        }
        if (!action.verifiedWithin(System.currentTimeMillis(), preparedMaxAgeMs)) {
            return null;
        }
        return action.matches(allowedPreparedOperation, allowedPreparedTargetKeyword) ? null : action;
    }

    private void logTaskTrackerPanelNegativeConsume(String result,
                                                    String reason,
                                                    TaskTrackerPanelNegativeResult current,
                                                    TaskType expectedTaskType,
                                                    String expectedTaskCode) {
        long now = System.currentTimeMillis();
        log.info("[task-tracker negative] consume: result={} windowId={} reason={} expectedTaskType={} expectedTaskCode={} status={} taskType={} taskCode={} source={} sequence={} ageMs={}",
                result, windowId, normalize(reason), expectedTaskType, expectedTaskCode,
                current == null ? null : current.getStatus(),
                current == null ? null : current.getTaskType(),
                current == null ? null : current.getTaskCode(),
                current == null ? null : normalize(current.getSource()),
                current == null ? -1L : current.getSequence(),
                current == null ? -1L : ageMs(now, current.getObservedAtMs()));
    }

    public void clearTaskTrackerPanelNegativeResult(String reason) {
        TaskTrackerPanelNegativeResult cleared = taskTrackerPanelNegativeResult.getAndSet(null);
        if (cleared != null) {
            long now = System.currentTimeMillis();
            log.info("[task-tracker negative] cleared: windowId={} taskType={} taskCode={} status={} source={} reason={} sequence={} ageMs={}",
                    windowId, cleared.getTaskType(), cleared.getTaskCode(), cleared.getStatus(),
                    normalize(cleared.getSource()), normalize(reason), cleared.getSequence(),
                    ageMs(now, cleared.getObservedAtMs()));
        }
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
        taskQueueStartupUiCleanupProbe.set(null);
        clearTaskQueueStartupFlyingState(source);
    }

    /** @return true when this accepted queue already completed common startup UI preparation. */
    public boolean isTaskQueueStartupPreparationDone() {
        return taskQueueStartupPreparationDone.get() != null;
    }

    /** @return true when this accepted queue already completed common startup UI preparation. */
    public boolean isTaskQueueStartupPreparationDone(String taskCode) {
        return isTaskQueueStartupPreparationDone();
    }

    /** Mark queue-scoped common startup UI preparation as completed. */
    public void markTaskQueueStartupPreparationDone(String taskCode) {
        taskQueueStartupPreparationDone.set(normalize(taskCode));
    }

    /**
     * Record whether background startup proved that no UI cleanup is needed.
     *
     * @param taskCode queue/task code that produced the startup probe.
     * @param clean true only when the background probe saw no map, dialog, or generic close window.
     * @param source diagnostic source.
     */
    public void markTaskQueueStartupUiCleanupProbe(String taskCode, boolean clean, String source) {
        taskQueueStartupUiCleanupProbe.set(new TaskQueueStartupUiCleanupProbe(
                normalize(taskCode), clean, System.currentTimeMillis(), normalize(source)));
        log.info("[window startup ui-clean] marked: windowId={} taskCode={} clean={} source={}",
                windowId, normalize(taskCode), clean, normalize(source));
    }

    /**
     * @return true only when a fresh background startup probe already confirmed the UI is clean for
     *         the same accepted task queue.
     */
    public boolean consumeFreshTaskQueueStartupUiCleanupClean(String taskCode, long maxAgeMs) {
        TaskQueueStartupUiCleanupProbe probe = taskQueueStartupUiCleanupProbe.getAndSet(null);
        String normalizedTaskCode = normalize(taskCode);
        long ageMs = probe == null ? -1L : Math.max(0L, System.currentTimeMillis() - probe.createdAtMs());
        boolean fresh = probe != null
                && probe.clean()
                && (normalizedTaskCode == null || Objects.equals(normalizedTaskCode, probe.taskCode()))
                && (maxAgeMs <= 0L || ageMs <= maxAgeMs);
        log.info("[window startup ui-clean] consumed: windowId={} taskCode={} freshClean={} ageMs={} source={}",
                windowId, normalizedTaskCode, fresh, ageMs, probe == null ? null : probe.source());
        return fresh;
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
    public void markTaskQueueStartupFlyingState(WindowFlyingState state, String source) {
        WindowFlyingState normalizedState = state == null ? WindowFlyingState.UNKNOWN : state;
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
    public WindowFlyingState consumeTaskQueueStartupFlyingState(String source) {
        WindowFlyingState state = taskQueueStartupFlyingState.getAndSet(null);
        String observedSource = taskQueueStartupFlyingStateSource.getAndSet(null);
        WindowFlyingState result = state == null ? WindowFlyingState.UNKNOWN : state;
        log.info("[window startup flying] consumed: windowId={} state={} observedSource={} consumeSource={}",
                windowId, result, observedSource, normalize(source));
        return result;
    }

    /** Clear stale startup flying evidence without consuming it as a task decision. */
    public void clearTaskQueueStartupFlyingState(String source) {
        WindowFlyingState cleared = taskQueueStartupFlyingState.getAndSet(null);
        String observedSource = taskQueueStartupFlyingStateSource.getAndSet(null);
        if (cleared != null || observedSource != null) {
            log.info("[window startup flying] cleared: windowId={} state={} observedSource={} source={}",
                    windowId, cleared, observedSource, normalize(source));
        }
    }

    public DialogPreparationStatus getDialogPreparationStatus() { return dialogPreparationStatus.get(); }

    public String getRunningTaskProgressText() {
        WindowTaskRunProgress progress = runningTaskProgress.get();
        if (progress == null && status == WindowRuntimeStatus.PAUSED) {
            progress = pausedTaskRunProgress.get();
        }
        return progress == null ? "-" : progress.toDisplayText();
    }

    /** Clears only the unbound expected-combat claim created for one failed asynchronous input. */
    public void clearExpectedCombatEnterClaim(String claimId, String reason) {
        if (claimId == null || claimId.isBlank()) {
            return;
        }
        while (true) {
            WindowExpectedCombatEnterClaim current = expectedCombatEnterClaim.get();
            if (current == null || !claimId.equals(current.claimId()) || current.combatGeneration() != null) {
                return;
            }
            if (expectedCombatEnterClaim.compareAndSet(current, null)) {
                log.info("[local-runner] cleared unbound expected combat enter claim: windowId={} claimId={} reason={}",
                        windowId, claimId, normalize(reason));
                return;
            }
        }
    }

    public WindowTaskRunProgress getPausedTaskRunProgress() { return pausedTaskRunProgress.get(); }

    /** 云端可经 WHOLE_TASK_PROGRESS_UPDATE 写入的实时次数账本；终局调度要与暂停快照一起取较大值。 */
    public WindowTaskRunProgress getRunningTaskRunProgress() { return runningTaskProgress.get(); }

    /**
     * Update the user-facing in-task run counter for finite repeatable tasks.
     *
     * @param completedRuns number of fully completed task rounds/runs, zero-based at task start.
     * @param totalRuns configured finite run count. Non-positive values do not produce a UI count
     *                  because there is no right-hand total to display.
     */
    public void updateTaskRunProgress(int completedRuns, int totalRuns) {
        if (totalRuns <= 0) {
            runningTaskProgress.set(null);
            return;
        }
        int safeCompleted = Math.max(0, Math.min(completedRuns, totalRuns));
        TaskType taskType = lastTaskType == null || lastTaskType == TaskType.UNKNOWN
                ? selectedTaskType
                : lastTaskType;
        runningTaskProgress.set(WindowTaskRunProgress.builder()
                .taskType(taskType == null ? TaskType.UNKNOWN : taskType)
                .completedRuns(safeCompleted)
                .totalRuns(totalRuns)
                .build());
    }

    /** Retain only the current numeric counter before pause clears all executable task state. */
    public void retainTaskRunProgressForPause() {
        WindowTaskRunProgress retained = runningTaskProgress.get();
        pausedTaskRunProgress.set(retained);
        log.info("[task-progress] pause snapshot: windowId={} task={} completed={} total={}",
                windowId,
                retained == null ? null : retained.getTaskType(),
                retained == null ? null : retained.getCompletedRuns(),
                retained == null ? null : retained.getTotalRuns());
    }

    /** Drop a pause-only counter at a cold/terminal boundary so it can never cross task ownership. */
    public void clearPausedTaskRunProgress(String reason) {
        WindowTaskRunProgress cleared = pausedTaskRunProgress.getAndSet(null);
        if (cleared != null) {
            log.info("[task-progress] pause snapshot cleared: windowId={} task={} completed={} total={} reason={}",
                    windowId, cleared.getTaskType(), cleared.getCompletedRuns(), cleared.getTotalRuns(),
                    normalize(reason));
        }
    }

    /**
     * Clear the user-facing in-task run counter when no finite task progress is available.
     */
    public void clearTaskRunProgress() {
        runningTaskProgress.set(null);
    }

    public long getObserverWakeSeq() { return observerWakeSeq.get(); }

    /**
     * Wake the window observer loop without publishing a task-ready business event.
     *
     * <p>The observer uses this monotonic sequence to break out of short sleeps when control-plane
     * state changes, such as leader pause entering read-only combat observation. This method does
     * not prepare actions, send input, or advance any task phase.</p>
     *
     * @param reason diagnostic reason written to logs; nullable.
     * @return new observer wake sequence value.
     */
    public long wakeObserver(String reason) {
        long wakeSeq = observerWakeSeq.incrementAndGet();
        log.info("[latency] event=window.observer.wake windowId={} reason={} wakeSeq={}",
                windowId, normalize(reason), wakeSeq);
        return wakeSeq;
    }

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

    public long getOrdinaryPreBattleTimeoutPublishedAtMs() { return ordinaryPreBattleTimeoutPublishedAtMs.get(); }

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
     * Atomically open the active target-map gate and install its exact dialog interest.
     * Replays are idempotent: an already-open gate refreshes the same interest in the same call.
     */
    public synchronized boolean openOrdinaryEnterBattleTargetMapGateAndUpdateDialogInterest(
            WindowDialogInterest interest, String source, long nowMs) {
        if (ordinaryEnterBattleTargetMapGateStartedAtMs.get() <= 0L || interest == null) {
            return false;
        }
        ordinaryEnterBattleTargetMapOpenedAtMs.compareAndSet(0L, Math.max(1L, nowMs));
        updateDialogInterest(interest, source);
        return ordinaryEnterBattleTargetMapOpenedAtMs.get() > 0L;
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
     * Shift automation-owned volatile cache timestamps after CR160 has proven the pause/resume
     * fingerprint still matches.
     *
     * <p>This method compensates only framework wait budgets and cache ages: prepared dialog age,
     * visible-dialog cache age, dialog preparation request/status, dialog-interest allowance, and
     * pathing observer timestamps. Real game-world durations such as item pending TTL, incense, boxes,
     * or buffs must never be adjusted here.</p>
     *
     * @param blockedMs wall-clock milliseconds spent blocked by user pause.
     * @param source diagnostic source written to logs.
     * @return names of volatile automation timers that were shifted.
     */
    public List<String> compensateVolatileAutomationTimersAfterPause(long blockedMs, String source) {
        if (blockedMs <= 0L) {
            return List.of();
        }
        List<String> compensatedTimers = new ArrayList<>();
        if (shiftPreparedDialogAction(blockedMs)) {
            compensatedTimers.add("preparedActionAge");
        }
        if (shiftDialogPreparationRequest(blockedMs)) {
            compensatedTimers.add("dialogPreparationRequest");
        }
        if (shiftDialogPreparationStatus(blockedMs)) {
            compensatedTimers.add("dialogPreparationStatus");
        }
        if (shiftVisibleDialogSnapshot(blockedMs)) {
            compensatedTimers.add("visibleDialogCache");
        }
        if (shiftDialogInterest(blockedMs)) {
            compensatedTimers.add("dialogInterest");
        }
        if (shiftPathingSnapshot(blockedMs)) {
            compensatedTimers.add("pathingWaitBudget");
        }
        log.info("[cr160 pause-resume] volatile timers compensated: windowId={} source={} pauseBlockedMs={} compensatedTimers={}",
                windowId, normalize(source), blockedMs, compensatedTimers);
        return List.copyOf(compensatedTimers);
    }

    /**
     * Clear stale volatile state after CR160 decides the paused fingerprint no longer matches.
     *
     * @param reason diagnostic reason written to each clear log.
     * @return names of volatile state groups that were cleared.
     */
    public List<String> clearPauseResumeVolatileState(String reason) {
        List<String> cleared = new ArrayList<>();
        String clearReason = "cr160 pause-resume mismatch: " + normalize(reason);
        if (preparedDialogAction.get() != null) {
            cleared.add("preparedAction");
        }
        if (dialogPreparationRequest.get() != null
                || dialogPreparationStatus.get().getPhase() != DialogPreparationPhase.NONE) {
            cleared.add("dialogPreparationRequest");
        }
        if (dialogPreparationRequest.get() != null
                || dialogPreparationStatus.get().getPhase() != DialogPreparationPhase.NONE) {
            clearDialogPreparationRequest(clearReason);
        } else if (preparedDialogAction.get() != null) {
            clearPreparedDialogAction(clearReason);
        }
        if (dialogInterest.get() != null) {
            cleared.add("dialogInterest");
            clearDialogInterest(clearReason);
        }
        if (visibleDialogSnapshot.get() != null) {
            cleared.add("visibleDialogCache");
            clearVisibleDialogSnapshot(clearReason);
        }
        WindowPathingSnapshot snapshot = pathingSnapshot.get();
        if (snapshot != null
                && (snapshot.getState() != WindowPathingState.NONE || snapshot.getIntent() != null)) {
            cleared.add("pathingSignal");
            clearPathingSignal(clearReason);
        }
        log.info("[cr160 pause-resume] volatile state cleared: windowId={} reason={} clearedVolatileState={}",
                windowId, clearReason, cleared);
        return List.copyOf(cleared);
    }

    private boolean shiftPreparedDialogAction(long blockedMs) {
        while (true) {
            PreparedDialogAction current = preparedDialogAction.get();
            if (current == null) {
                return false;
            }
            PreparedDialogAction shifted = current.toBuilder()
                    .preparedAtMs(shiftPositiveTimestamp(current.getPreparedAtMs(), blockedMs))
                    .lastVerifiedAtMs(shiftPositiveTimestamp(current.getLastVerifiedAtMs(), blockedMs))
                    .build();
            if (preparedDialogAction.compareAndSet(current, shifted)) {
                return true;
            }
        }
    }

    private boolean shiftDialogPreparationRequest(long blockedMs) {
        while (true) {
            DialogPreparationRequest current = dialogPreparationRequest.get();
            if (current == null) {
                return false;
            }
            DialogPreparationRequest shifted = DialogPreparationRequest.builder()
                    .operation(current.getOperation())
                    .targetKeyword(current.getTargetKeyword())
                    .source(current.getSource())
                    .fromMap(current.getFromMap())
                    .rememberedRelativeX(current.getRememberedRelativeX())
                    .rememberedRelativeY(current.getRememberedRelativeY())
                    .rememberedOptionText(current.getRememberedOptionText())
                    .createdAtMs(shiftPositiveTimestamp(current.getCreatedAtMs(), blockedMs))
                    .expiresAtMs(shiftPositiveTimestamp(current.getExpiresAtMs(), blockedMs))
                    .build();
            if (dialogPreparationRequest.compareAndSet(current, shifted)) {
                return true;
            }
        }
    }

    private boolean shiftDialogPreparationStatus(long blockedMs) {
        while (true) {
            DialogPreparationStatus current = dialogPreparationStatus.get();
            if (current == null || current.getPhase() == DialogPreparationPhase.NONE) {
                return false;
            }
            DialogPreparationStatus shifted = DialogPreparationStatus.builder()
                    .phase(current.getPhase())
                    .operation(current.getOperation())
                    .targetKeyword(current.getTargetKeyword())
                    .source(current.getSource())
                    .requestCreatedAtMs(shiftPositiveTimestamp(current.getRequestCreatedAtMs(), blockedMs))
                    .preparingStartedAtMs(shiftPositiveTimestamp(current.getPreparingStartedAtMs(), blockedMs))
                    .completedAtMs(shiftPositiveTimestamp(current.getCompletedAtMs(), blockedMs))
                    .failureReason(current.getFailureReason())
                    .build();
            if (dialogPreparationStatus.compareAndSet(current, shifted)) {
                return true;
            }
        }
    }

    private boolean shiftVisibleDialogSnapshot(long blockedMs) {
        while (true) {
            WindowDialogSnapshot current = visibleDialogSnapshot.get();
            if (current == null) {
                return false;
            }
            WindowDialogSnapshot shifted = WindowDialogSnapshot.builder()
                    .windowId(current.getWindowId())
                    .hwnd(current.getHwnd())
                    .type(current.getType())
                    .source(current.getSource())
                    .detectedAtMs(shiftPositiveTimestamp(current.getDetectedAtMs(), blockedMs))
                    .dialogRect(copyRect(current.getDialogRect()))
                    .captureProvider(current.getCaptureProvider())
                    .build();
            if (visibleDialogSnapshot.compareAndSet(current, shifted)) {
                return true;
            }
        }
    }

    private boolean shiftDialogInterest(long blockedMs) {
        while (true) {
            WindowDialogInterest current = dialogInterest.get();
            if (current == null) {
                return false;
            }
            WindowDialogInterest shifted = current.toBuilder()
                    .createdAtMs(shiftPositiveTimestamp(current.getCreatedAtMs(), blockedMs))
                    .expiresAtMs(shiftPositiveTimestamp(current.getExpiresAtMs(), blockedMs))
                    .absentAllowedAtMs(shiftPositiveTimestamp(current.getAbsentAllowedAtMs(), blockedMs))
                    .build();
            if (dialogInterest.compareAndSet(current, shifted)) {
                return true;
            }
        }
    }

    private boolean shiftPathingSnapshot(long blockedMs) {
        while (true) {
            WindowPathingSnapshot current = pathingSnapshot.get();
            if (current == null
                    || (current.getState() == WindowPathingState.NONE && current.getIntent() == null)) {
                return false;
            }
            WindowPathingIntent intent = current.getIntent();
            WindowPathingIntent shiftedIntent = intent == null
                    ? null
                    : intent.toBuilder()
                    .createdAtMs(shiftPositiveTimestamp(intent.getCreatedAtMs(), blockedMs))
                    .build();
            WindowPathingSnapshot shifted = current.toBuilder()
                    .intent(shiftedIntent)
                    .locationChangedAtMs(shiftPositiveTimestamp(current.getLocationChangedAtMs(), blockedMs))
                    .updatedAtMs(shiftPositiveTimestamp(current.getUpdatedAtMs(), blockedMs))
                    .probeStartedAtMs(shiftPositiveTimestamp(current.getProbeStartedAtMs(), blockedMs))
                    .probeFinishedAtMs(shiftPositiveTimestamp(current.getProbeFinishedAtMs(), blockedMs))
                    .uiCleanupRecommendedAtMs(shiftPositiveTimestamp(current.getUiCleanupRecommendedAtMs(), blockedMs))
                    .dialogBlockingDetectedAtMs(shiftPositiveTimestamp(current.getDialogBlockingDetectedAtMs(), blockedMs))
                    .build();
            if (pathingSnapshot.compareAndSet(current, shifted)) {
                return true;
            }
        }
    }

    private long shiftPositiveTimestamp(long timestamp, long deltaMs) {
        return timestamp <= 0L ? timestamp : timestamp + deltaMs;
    }

    private int[] copyRect(int[] rect) {
        return rect == null ? null : rect.clone();
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
        WindowDialogSnapshot previous = visibleDialogSnapshot.getAndSet(snapshot);
        if (snapshot.getType() == DialogType.STORY
                && (previous == null || previous.getType() != DialogType.STORY)) {
            storyDialogVisibleSequence.incrementAndGet();
        }
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
        interest = normalizeDialogInterestProbeStart(interest, reason);
        WindowDialogInterest previous = dialogInterest.getAndSet(interest);
        boolean sameInterestIdentity = previous != null
                && previous.getTaskType() == interest.getTaskType()
                && previous.getCreatedAtMs() == interest.getCreatedAtMs()
                && Objects.equals(previous.getSource(), interest.getSource())
                && Objects.equals(previous.getOperations(), interest.getOperations());
        if (!sameInterestIdentity) {
            // A dialog claim belongs only to the interest that matched it. Task/phase replacement
            // must re-arm matching instead of letting the previous task suppress the new dialog.
            clearTiantingDialogOptionClaim();
        }
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

    /**
     * Keeps a cloud-issued probe delay valid after a late local interest installation.
     *
     * <p>The observation wire contract rejects a probe start before the local interest creation time. A
     * queued Cloud action can arrive after its originally calculated start time, so retaining that stale
     * timestamp would fail-close every later observation batch. Future delays remain unchanged; only an
     * already elapsed delay is advanced to this local interest's creation boundary.</p>
     *
     * @param interest interest about to become visible to the local observation runner; never null.
     * @param reason diagnostic installation reason.
     * @return the original interest when its timing is valid, otherwise a timing-normalized immutable copy.
     */
    private WindowDialogInterest normalizeDialogInterestProbeStart(WindowDialogInterest interest, String reason) {
        long createdAtMs = interest.getCreatedAtMs();
        long probeStartAtMs = interest.getProbeStartAtMs();
        if (createdAtMs <= 0L || probeStartAtMs <= 0L || probeStartAtMs >= createdAtMs) {
            return interest;
        }
        log.info("[window dialog-interest] normalized stale probe start windowId={} task={} source={} reason={} probeStartAtMs={} createdAtMs={}",
                windowId, interest.getTaskType(), normalize(interest.getSource()), normalize(reason),
                probeStartAtMs, createdAtMs);
        return interest.toBuilder()
                .probeStartAtMs(createdAtMs)
                .build();
    }

    public void clearDialogInterest(String reason) {
        WindowDialogInterest cleared = dialogInterest.getAndSet(null);
        clearTiantingDialogOptionClaim();
        clearTiantingFengyaoPending();
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
        synchronized (xiuluoKandaTransitionLock) {
            if (action != null && action.getOperation() == DialogOperation.XIULUO_ENTER_BATTLE
                    && action.getIntentId() != null) {
                XiuluoGreenChainSchedule schedule = xiuluoGreenChainSchedule.get();
                if (schedule == null || !Objects.equals(schedule.getAttemptId(), action.getIntentId())) {
                    log.info("[latency] event=window.dialog.prepare.reject-stale-attempt windowId={} attemptId={} source={}",
                            windowId, action.getIntentId(), normalize(action.getSource()));
                    return;
                }
            }
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
     * CR253: open (or replace) the 修罗 green-chain attempt identity that gates typed prepared jobs.
     *
     * <p>Replacing the schedule drops every pending job that does not match the new identity, so a
     * saved-green re-press (new attemptId) atomically invalidates all work produced for the old
     * attempt.</p>
     *
     * @param schedule new attempt identity; null is ignored (use {@link #clearXiuluoGreenChainSchedule}).
     * @param reason diagnostic reason written to logs.
     */
    public void updateXiuluoGreenChainSchedule(XiuluoGreenChainSchedule schedule, String reason) {
        if (schedule == null) {
            return;
        }
        synchronized (xiuluoKandaTransitionLock) {
            applyXiuluoGreenChainScheduleLocked(schedule, reason);
        }
    }

    /**
     * TURN-40G review#3 P1: installs (or replaces) the paired probe-only dialog interest AND green-chain attempt
     * schedule as ONE atomic transition under the kanda monitor. The stale-job discard, stale prepared-kanda
     * discard and one-shot click-claim re-arm run inside the same transition, so a concurrent sampler that reads
     * through {@link #getXiuluoKandaProbeView()} can never pair the new interest with the old attempt's schedule
     * (or click for a stale attempt before the fence runs). Ordinary interest-only updates keep using
     * {@link #updateDialogInterest} unchanged.
     *
     * @param interest probe-only interest published by the attempt; required.
     * @param schedule exact attempt identity opened/replaced with the interest; required.
     * @param reason diagnostic reason written to logs.
     */
    public void updateDialogInterestWithXiuluoGreenChainSchedule(WindowDialogInterest interest,
                                                                  XiuluoGreenChainSchedule schedule,
                                                                  String reason) {
        Objects.requireNonNull(interest, "interest");
        Objects.requireNonNull(schedule, "schedule");
        interest = normalizeDialogInterestProbeStart(interest, reason);
        synchronized (xiuluoKandaTransitionLock) {
            applyXiuluoGreenChainScheduleLocked(schedule, reason);
            dialogInterest.set(interest);
        }
        long wakeSeq = observerWakeSeq.incrementAndGet();
        log.info("[latency] event=window.dialog.interest-with-schedule.update windowId={} task={} operations={} source={} reason={} schedule=[{}] wakeSeq={}",
                windowId, interest.getTaskType(), interest.getOperations(), normalize(interest.getSource()),
                normalize(reason), schedule.identityText(), wakeSeq);
    }

    /** Immutable single-transition snapshot of the paired xiuluo probe state (TURN-40G review#3 P1). */
    public record XiuluoKandaProbeView(
            WindowDialogInterest interest,
            XiuluoGreenChainSchedule schedule,
            boolean enterBattleClaimed) {
    }

    /**
     * A local 看打 claim belongs to one exact green-chain attempt. A later attempt must never inherit
     * that click merely because it is running in the same window and observation session.
     */
    private boolean isCurrentLocalTemplateClaim(WindowExpectedCombatEnterClaim claim) {
        if (!isLocalTemplateCombatTask(claim) || !"local-template".equals(claim.source())) {
            return true;
        }
        synchronized (xiuluoKandaTransitionLock) {
            XiuluoGreenChainSchedule schedule = xiuluoGreenChainSchedule.get();
            // Direct unit consumers may exercise the generic combat ticket without a green-chain
            // schedule. The production local-template path always has one, and then it is exact.
            return schedule == null
                    || (Objects.equals(schedule.getWindowId(), claim.windowId())
                    && Objects.equals(schedule.getHwnd(), claim.hwnd())
                    && Objects.equals(schedule.getObservationRunId(), claim.observationRunId())
                    && Objects.equals(schedule.getTaskRunId(), claim.businessTaskRunId())
                    && Objects.equals(schedule.getAttemptId(), claim.attemptId()));
        }
    }

    /** Result of checking whether the current local 看打 click may be retried for this exact attempt. */
    public enum XiuluoKandaRetryState {
        AVAILABLE,
        WAITING_FOR_COMBAT,
        COMBAT_CONFIRMED,
        RETRY_AVAILABLE,
        EXHAUSTED_NEW,
        EXHAUSTED_REPORTED,
        STALE
    }

    /**
     * Reads the interest+schedule pair under the same monitor as every paired transition. The two components are
     * always from one consistent state: mid-replacement the reader sees either the complete old pair or the
     * complete new pair, never a torn mixture.
     */
    public XiuluoKandaProbeView getXiuluoKandaProbeView() {
        synchronized (xiuluoKandaTransitionLock) {
            XiuluoGreenChainSchedule schedule = xiuluoGreenChainSchedule.get();
            return new XiuluoKandaProbeView(
                    dialogInterest.get(),
                    schedule,
                    schedule != null
                            && Objects.equals(schedule.getAttemptId(), xiuluoEnterBattleClickClaim.get()));
        }
    }

    private void applyXiuluoGreenChainScheduleLocked(XiuluoGreenChainSchedule schedule, String reason) {
        XiuluoGreenChainSchedule previous = xiuluoGreenChainSchedule.getAndSet(schedule);
        xiuluoConfirmedCombatAttempt.set(null);
        clearUnboundExpectedCombatClaimForReplacedAttempt(schedule, reason);
        preparedActionJobs.entrySet().removeIf(entry -> {
            PreparedActionJob job = entry.getValue();
            if (schedule.sameIdentity(job)) {
                return false;
            }
            log.info("[latency] event=window.prepared-job.discard windowId={} reason=schedule-replaced:{} job=[{}] newSchedule=[{}]",
                    windowId, normalize(reason), job.identityText(), schedule.identityText());
            return true;
        });
        // CR253 review P1: the local kanda prepared shares the attempt identity rule — a dialog
        // prepared stamped for another (or no) attempt dies with the schedule replacement.
        discardStaleXiuluoEnterBattlePrepared(schedule.getAttemptId(),
                "schedule-replaced:" + normalize(reason));
        // TURN-40G: a replaced attempt re-arms the local-kanda one-shot click claim; the previous attempt's
        // click (if any) can never satisfy the new attempt.
        xiuluoEnterBattleClickClaim.set(null);
        xiuluoLocalKandaClickProgress.set(null);
        if (previous == null || !previous.sameFullIdentity(schedule)) {
            xiuluoMissingKandaAfterPathingTerminalClaim.set(null);
        }
        log.info("[latency] event=window.green-chain.schedule.update windowId={} reason={} schedule=[{}] previous=[{}]",
                windowId, normalize(reason), schedule.identityText(),
                previous == null ? null : previous.identityText());
    }

    /**
     * TURN-40G: attempt-scoped one-shot arbitration for the restored xiuluo local-kanda click. Exactly one winner
     * may submit the enter-battle input per green-chain attempt — a second local hit for the same attempt, or any
     * competitor racing this attempt, loses the CAS and only records superseded. A physically unexecuted click
     * releases the claim so the still-open attempt keeps its fast path (execution failure consumes nothing).
     */
    /**
     * G005: claim the right to answer one 天庭 dialog instance.
     *
     * <p>The 天庭 option interest deliberately stays installed for the whole movement leg, so nothing
     * upstream stops a second click while the answered dialog is still closing. This claim is what
     * makes the answer one-shot; it lives on the context rather than on the sampler so a sampler
     * rebuilt mid-run cannot re-answer a dialog this window already handled.</p>
     *
     * @param optionKey identity of the dialog instance being answered (interest instant + option).
     * @return true when this caller may click; false when the same instance was already answered.
     */
    public boolean tryClaimTiantingDialogOption(String optionKey) {
        if (optionKey == null || optionKey.isBlank()) {
            return false;
        }
        return !optionKey.equals(tiantingDialogOptionClaim.getAndSet(optionKey));
    }

    /**
     * Read-only check used before the expensive revalidation capture, so an already-answered dialog
     * costs nothing per cycle.
     *
     * @param optionKey identity of the dialog instance about to be probed.
     * @return true when this window already answered exactly this instance.
     */
    public boolean hasTiantingDialogOptionClaim(String optionKey) {
        return optionKey != null && optionKey.equals(tiantingDialogOptionClaim.get());
    }

    /**
     * Remembers that an executed 多谢 click requires local 使用封妖符 matching on later samples.
     *
     * @param sourceOptionKey identity of the executed 多谢 option; must be nonblank.
     */
    public void markTiantingFengyaoPending(String sourceOptionKey) {
        if (sourceOptionKey == null || sourceOptionKey.isBlank()) {
            throw new IllegalArgumentException("sourceOptionKey must not be blank");
        }
        tiantingFengyaoPending.set(sourceOptionKey);
    }

    /**
     * @return true after 多谢 executed and before 使用封妖符 executed or the owning interest/run was cleared.
     */
    public boolean isTiantingFengyaoPending() {
        return tiantingFengyaoPending.get() != null;
    }

    /*
     * 2026-08-21 用户拍板(18:12 鬼王接任务空点事故):任务侧装的对话兴趣(如 GHOST_KING_ACCEPT_TASK)
     * 本来就每个采样周期在匹配"接任务"选项,匹配到就点。把"这个选项真的被点掉了"这一刻记在这里,
     * NPC 点击的 FIFO 就能拿它当"这一下点中没中 NPC"的判据:等到=成功收工;等不到=点空,直接取
     * 下一个候选(记忆点/tooltip/黄名)。以前 defer 模式下 FIFO 点完就当成功,tooltip 永远轮不到。
     */
    private final java.util.concurrent.atomic.AtomicLong lastTaskDialogOptionAnsweredAtMs =
            new java.util.concurrent.atomic.AtomicLong();

    /** Stamp the moment a task-owned dialog interest actually clicked its option. */
    public void markTaskDialogOptionAnswered(String actionKey) {
        long now = System.currentTimeMillis();
        lastTaskDialogOptionAnsweredAtMs.set(now);
        log.info("[local-runner] task dialog option answered: windowId={} actionKey={} atMs={}",
                windowId, normalize(actionKey), now);
    }

    /** @return epoch millis of the last task-owned dialog option click, or 0 when never. */
    public long lastTaskDialogOptionAnsweredAtMs() {
        return lastTaskDialogOptionAnsweredAtMs.get();
    }

    /** Clears the action-owned 使用封妖符 follow-up state. */
    public void clearTiantingFengyaoPending() {
        tiantingFengyaoPending.set(null);
    }

    /**
     * Release the 天庭 dialog claim once no known option is on screen, which is the only evidence the
     * client has that the answered dialog actually closed.
     */
    public void clearTiantingDialogOptionClaim() {
        tiantingDialogOptionClaim.set(null);
    }

    public boolean tryClaimXiuluoEnterBattleClick(XiuluoGreenChainSchedule expected, String reason) {
        if (expected == null || expected.getAttemptId() == null || expected.getAttemptId().isBlank()) {
            return false;
        }
        boolean claimed;
        // TURN-40G review#3 P1: the identity check and the claim CAS are one transition under the kanda
        // monitor — a schedule replacement can never slip between them and let a stale attempt claim the click.
        // TURN-40G review#5 P1: the live schedule must match the caller's FULL five-field identity (windowId,
        // hwnd, taskRunId, round, attemptId), so an old runner OR a same-run replacement that changed round/hwnd
        // (even while ids collide/reuse) can never claim the current schedule's click.
        synchronized (xiuluoKandaTransitionLock) {
            XiuluoGreenChainSchedule schedule = xiuluoGreenChainSchedule.get();
            if (schedule == null || !schedule.sameFullIdentity(expected)) {
                return false;
            }
            claimed = xiuluoEnterBattleClickClaim.compareAndSet(null, expected.getAttemptId());
        }
        log.info("[latency] event=window.local-kanda.claim windowId={} claimed={} expected=[{}] reason={}",
                windowId, claimed, expected.identityText(), normalize(reason));
        return claimed;
    }

    /**
     * Checks the Runner-owned confirmation window after a local 看打 click. A physical click only earns a
     * bounded wait for {@code IN_COMBAT}; after four seconds without combat, the same still-open attempt may
     * re-click at most twice. The final failure is reported once so Cloud can decide the green-link fallback.
     */
    public XiuluoKandaRetryState evaluateXiuluoLocalKandaRetry(
            XiuluoGreenChainSchedule expected, long nowMs, boolean combatVisible) {
        if (expected == null || expected.getAttemptId() == null || expected.getAttemptId().isBlank()) {
            return XiuluoKandaRetryState.STALE;
        }
        synchronized (xiuluoKandaTransitionLock) {
            XiuluoConfirmedCombatAttempt confirmed = xiuluoConfirmedCombatAttempt.get();
            if (confirmed != null && confirmed.matches(
                    expected.getTaskRunId(), expected.getRound(), expected.getAttemptId())) {
                return XiuluoKandaRetryState.COMBAT_CONFIRMED;
            }
            XiuluoGreenChainSchedule schedule = xiuluoGreenChainSchedule.get();
            if (schedule == null || !schedule.sameFullIdentity(expected)) {
                return XiuluoKandaRetryState.STALE;
            }
            XiuluoLocalKandaClickProgress progress = xiuluoLocalKandaClickProgress.get();
            if (progress == null || !expected.getAttemptId().equals(progress.attemptId())) {
                return XiuluoKandaRetryState.AVAILABLE;
            }
            if (progress.combatConfirmed()) {
                return XiuluoKandaRetryState.COMBAT_CONFIRMED;
            }
            if (combatVisible || nowMs - progress.lastClickAtMs() < XIULUO_LOCAL_KANDA_CONFIRM_WINDOW_MS) {
                return XiuluoKandaRetryState.WAITING_FOR_COMBAT;
            }
            if (progress.executedClicks() >= MAX_XIULUO_LOCAL_KANDA_CLICKS) {
                if (!progress.failureReported()) {
                    xiuluoLocalKandaClickProgress.set(progress.withFailureReported());
                    return XiuluoKandaRetryState.EXHAUSTED_NEW;
                }
                return XiuluoKandaRetryState.EXHAUSTED_REPORTED;
            }
            xiuluoEnterBattleClickClaim.compareAndSet(expected.getAttemptId(), null);
            log.info("[latency] event=window.local-kanda.retry-rearm windowId={} expected=[{}] clicks={} reason=no-in-combat-within-{}ms",
                    windowId, expected.identityText(), progress.executedClicks(), XIULUO_LOCAL_KANDA_CONFIRM_WINDOW_MS);
            return XiuluoKandaRetryState.RETRY_AVAILABLE;
        }
    }

    /** Records only an actually executed local 看打 click; failed queue execution does not spend the budget. */
    public void recordXiuluoLocalKandaClick(XiuluoGreenChainSchedule expected, long clickedAtMs) {
        if (expected == null || expected.getAttemptId() == null) {
            return;
        }
        synchronized (xiuluoKandaTransitionLock) {
            XiuluoGreenChainSchedule schedule = xiuluoGreenChainSchedule.get();
            if (schedule == null || !schedule.sameFullIdentity(expected)) {
                return;
            }
            XiuluoLocalKandaClickProgress current = xiuluoLocalKandaClickProgress.get();
            int clicks = current != null && expected.getAttemptId().equals(current.attemptId())
                    ? current.executedClicks() + 1 : 1;
            xiuluoLocalKandaClickProgress.set(new XiuluoLocalKandaClickProgress(
                    expected.getAttemptId(), clicks, clickedAtMs, false, false));
            log.info("[latency] event=window.local-kanda.click-recorded windowId={} expected=[{}] clicks={}",
                    windowId, expected.identityText(), clicks);
        }
    }

    /**
     * Claims the bounded failure edge for a tracker shortcut that has already reached a terminal
     * pathing state but never exposed a local 看打 template.
     *
     * <p>The terminal snapshot is retained across Cloud's pathing-clear command. Matching the exact
     * attempt id prevents an old route from failing a replacement attempt, while the current ACTIVE
     * guard prevents this recovery from firing during normal movement.</p>
     *
     * @param expected exact green-chain schedule being sampled.
     * @return true exactly once when the same pathing attempt is terminal and no newer pathing owns
     *         the window; false while moving, for stale attempts, or after the edge was claimed.
     */
    public boolean tryClaimXiuluoMissingKandaAfterPathingTerminal(XiuluoGreenChainSchedule expected) {
        if (expected == null || expected.getAttemptId() == null || expected.getAttemptId().isBlank()) {
            return false;
        }
        synchronized (xiuluoKandaTransitionLock) {
            XiuluoGreenChainSchedule liveSchedule = xiuluoGreenChainSchedule.get();
            if (liveSchedule == null || !liveSchedule.sameFullIdentity(expected)) {
                return false;
            }

            WindowPathingSnapshot current = pathingSnapshot.get();
            WindowPathingIntent currentIntent = current == null ? null : current.getIntent();
            if (currentIntent != null) {
                if (!Objects.equals(expected.getAttemptId(), currentIntent.getIntentId())) {
                    return false;
                }
                if (current.getState() == WindowPathingState.ACTIVE) {
                    return false;
                }
            }

            WindowPathingSnapshot terminal = currentIntent != null
                    ? current : lastKnownPathingLocation.get();
            WindowPathingIntent terminalIntent = terminal == null ? null : terminal.getIntent();
            if (terminalIntent == null
                    || !Objects.equals(expected.getAttemptId(), terminalIntent.getIntentId())
                    || (terminal.getState() != WindowPathingState.ARRIVED
                    && terminal.getState() != WindowPathingState.STOPPED_AWAY
                    && terminal.getState() != WindowPathingState.UNKNOWN)) {
                return false;
            }
            return xiuluoMissingKandaAfterPathingTerminalClaim.compareAndSet(
                    null, expected.getAttemptId());
        }
    }

    /**
     * Ends retry ownership once the Runner has confirmed that this exact local 看打 click entered combat.
     * A later world frame after combat exit must not re-arm the completed attempt.
     */
    public void confirmLocalTemplateCombatEntry(WindowExpectedCombatEnterClaim claim) {
        if (!isLocalTemplateCombatTask(claim) || !"local-template".equals(claim.source())) {
            return;
        }
        synchronized (xiuluoKandaTransitionLock) {
            XiuluoGreenChainSchedule schedule = xiuluoGreenChainSchedule.get();
            XiuluoLocalKandaClickProgress progress = xiuluoLocalKandaClickProgress.get();
            if (schedule == null
                    || !Objects.equals(schedule.getAttemptId(), claim.attemptId())
                    || !Objects.equals(schedule.getTaskRunId(), claim.businessTaskRunId())
                    || progress == null
                    || !Objects.equals(progress.attemptId(), claim.attemptId())) {
                return;
            }
            log.info("[latency] event=window.local-kanda.combat-confirmed windowId={} attemptId={} generation={}",
                    windowId, claim.attemptId(), claim.combatGeneration());
            xiuluoConfirmedCombatAttempt.set(new XiuluoConfirmedCombatAttempt(
                    schedule.getTaskRunId(), schedule.getRound(), schedule.getAttemptId(), claim.combatGeneration()));
            /*
             * The 看打 wait state dies here, whole. Schedule, click claim, click progress and the stamped
             * prepared action exist only to let the local template click 看打 once before the fight; the
             * Runner confirming combat entry is that job finished. It used to be merely marked
             * COMBAT_CONFIRMED and kept alive across the whole fight — its own clear method's javadoc even
             * lists confirmed combat entry as a mandatory discard boundary, but nothing called it — so a
             * death, an odd exit or the next round inherited a completed attempt and wedged on it. The
             * expected combat claim is a different thing and is not touched here: it is the identity fence
             * that pairs this entry with its exit, and it lives until the Runner reports the exit.
             *
             * Reentrant on the same kanda monitor, so the identity check above and the clear are atomic —
             * a replacement schedule cannot slip in between and be wrongly discarded.
             */
            clearXiuluoGreenChainSchedule("local-kanda-combat-confirmed:" + claim.attemptId());
        }
    }

    private static boolean isLocalTemplateCombatTask(WindowExpectedCombatEnterClaim claim) {
        return claim != null
                && ("XIULUO_V2".equalsIgnoreCase(claim.taskCode())
                || "XINSHOU_TRAINING".equalsIgnoreCase(claim.taskCode())
                || "CATCH_GHOST".equalsIgnoreCase(claim.taskCode())
                || "GHOST_KING".equalsIgnoreCase(claim.taskCode()));
    }

    /**
     * TURN-40G: releases the one-shot click claim after a physically unexecuted click (nothing was consumed).
     * TURN-40G review#5: the release is fenced to the caller's FULL five-field schedule identity — the live
     * schedule must still be that exact identity, so an old runner or a same-run round/hwnd replacement can
     * never release (and thereby re-arm) a different schedule's claim.
     */
    public void releaseXiuluoEnterBattleClick(XiuluoGreenChainSchedule expected, String reason) {
        if (expected == null || expected.getAttemptId() == null) {
            return;
        }
        boolean released;
        synchronized (xiuluoKandaTransitionLock) {
            XiuluoGreenChainSchedule schedule = xiuluoGreenChainSchedule.get();
            if (schedule == null || !schedule.sameFullIdentity(expected)) {
                return;
            }
            released = xiuluoEnterBattleClickClaim.compareAndSet(expected.getAttemptId(), null);
        }
        if (released) {
            log.info("[latency] event=window.local-kanda.claim-release windowId={} expected=[{}] reason={}",
                    windowId, expected.identityText(), normalize(reason));
        }
    }

    /**
     * CR253: close the green-chain attempt identity and drop every pending typed prepared job.
     *
     * <p>Called on new attempt setup failure, round restart/abandon, stop, binding/session loss,
     * watchdog cleanup, and confirmed combat entry — the contract's mandatory discard boundaries.</p>
     *
     * @param reason diagnostic reason written to logs.
     */
    public void clearXiuluoGreenChainSchedule(String reason) {
        XiuluoGreenChainSchedule cleared;
        // TURN-40G review#3 P1: the close runs under the same kanda monitor as open/replace and the paired reader.
        synchronized (xiuluoKandaTransitionLock) {
            cleared = xiuluoGreenChainSchedule.getAndSet(null);
            // TURN-40G: the local-kanda one-shot click claim dies with its attempt.
            xiuluoEnterBattleClickClaim.set(null);
            xiuluoLocalKandaClickProgress.set(null);
            xiuluoMissingKandaAfterPathingTerminalClaim.set(null);
        }
        clearPreparedActionJobs(reason);
        if (cleared != null) {
            // CR253 review P1: closing the attempt also discards its stamped local kanda prepared;
            // an unstamped action (no schedule was open when it was prepared) is not attempt work
            // and keeps its own lifecycle (e.g. WAIT_COMBAT re-registration).
            discardStaleXiuluoEnterBattlePrepared(null, "schedule-closed:" + normalize(reason));
            log.info("[latency] event=window.green-chain.schedule.clear windowId={} reason={} schedule=[{}]",
                    windowId, normalize(reason), cleared.identityText());
        }
    }

    /**
     * Atomically abandons only the local facts owned by one exact 修罗 attempt.
     *
     * <p>The task run, one-based round and attempt id are one indivisible identity. A stale reset is
     * therefore a complete no-op, including the pathing snapshot and observation-lineage generation.
     * If local Runner combat confirmation already won for the same identity, no state is cleared and
     * {@code combatAlreadyConfirmed} tells Cloud to continue the combat path.</p>
     *
     * @param taskRunId exact business task-run id; never null or blank.
     * @param round one-based 修罗 round.
     * @param attemptId exact local-kanda/pathing attempt id; never null or blank.
     * @param reason diagnostic reason written to local logs; nullable.
     * @return per-slot acknowledgement describing exactly what this transition cleared.
     */
    public ExactAttemptAbandonResult abandonExactXiuluoAttempt(
            String taskRunId, int round, String attemptId, String reason) {
        if (taskRunId == null || taskRunId.isBlank()) {
            throw new IllegalArgumentException("taskRunId must not be blank");
        }
        if (round <= 0) {
            throw new IllegalArgumentException("round must be positive");
        }
        if (attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("attemptId must not be blank");
        }

        synchronized (xiuluoKandaTransitionLock) {
            XiuluoConfirmedCombatAttempt confirmed = xiuluoConfirmedCombatAttempt.get();
            if (confirmed != null && confirmed.matches(taskRunId, round, attemptId)) {
                return ExactAttemptAbandonResult.combatConfirmed(taskRunId, round, attemptId);
            }

            XiuluoGreenChainSchedule schedule = xiuluoGreenChainSchedule.get();
            boolean scheduleMatched = schedule != null
                    && Objects.equals(taskRunId, schedule.getTaskRunId())
                    && round == schedule.getRound()
                    && Objects.equals(attemptId, schedule.getAttemptId())
                    && Objects.equals(windowId, schedule.getWindowId())
                    && nativeBinding != null
                    && Objects.equals(nativeBinding.getNativeHandle(), schedule.getHwnd());
            boolean matchingJobPresent = preparedActionJobs.values().stream()
                    .anyMatch(job -> exactAttemptMatches(job, taskRunId, round, attemptId));
            if (!scheduleMatched && !matchingJobPresent) {
                return ExactAttemptAbandonResult.noMatch(taskRunId, round, attemptId);
            }

            boolean clickClaimCleared = xiuluoEnterBattleClickClaim.compareAndSet(attemptId, null);
            XiuluoLocalKandaClickProgress progress = xiuluoLocalKandaClickProgress.get();
            boolean clickProgressCleared = progress != null && attemptId.equals(progress.attemptId());
            if (clickProgressCleared) {
                xiuluoLocalKandaClickProgress.set(null);
            }

            WindowExpectedCombatEnterClaim expectedClaim = expectedCombatEnterClaim.get();
            boolean expectedCombatClaimCleared = exactAttemptMatches(expectedClaim, taskRunId, attemptId)
                    && expectedCombatEnterClaim.compareAndSet(expectedClaim, null);
            PendingDirectCombatEnterTicket pendingTicket = pendingDirectCombatEnterClaim.get();
            boolean pendingCombatTicketCleared = pendingTicket != null
                    && exactAttemptMatches(pendingTicket.claim(), taskRunId, attemptId)
                    && pendingDirectCombatEnterClaim.compareAndSet(pendingTicket, null);

            PreparedDialogAction preparedDialog = preparedDialogAction.get();
            boolean preparedDialogActionCleared = preparedDialog != null
                    && preparedDialog.getOperation() == DialogOperation.XIULUO_ENTER_BATTLE
                    && Objects.equals(attemptId, preparedDialog.getIntentId())
                    && preparedDialogAction.compareAndSet(preparedDialog, null);
            if (preparedDialogActionCleared) {
                clearReadyDialogPreparationStatusFor(preparedDialog);
            }

            boolean preparedActionJobCleared = false;
            for (var entry : preparedActionJobs.entrySet()) {
                PreparedActionJob job = entry.getValue();
                if (exactAttemptMatches(job, taskRunId, round, attemptId)
                        && preparedActionJobs.remove(entry.getKey(), job)) {
                    preparedActionJobCleared = true;
                }
            }

            boolean pathingCleared = false;
            WindowPathingSnapshot snapshot = pathingSnapshot.get();
            if (snapshot != null && snapshot.getIntent() != null
                    && Objects.equals(attemptId, snapshot.getIntent().getIntentId())) {
                pathingCleared = pathingSnapshot.compareAndSet(snapshot, WindowPathingSnapshot.builder()
                        .state(WindowPathingState.NONE)
                        .message(normalize(reason))
                        .build());
                if (pathingCleared) {
                    clearPendingTransferChoiceMemory("exact attempt pathing reset");
                }
            }

            boolean scheduleCleared = scheduleMatched && xiuluoGreenChainSchedule.compareAndSet(schedule, null);
            long resetGeneration = observationPathingFactResetGeneration.incrementAndGet();
            log.info("[local-runner] exact attempt abandoned: windowId={} taskRunId={} round={} attemptId={} pathingCleared={} scheduleCleared={} clickClaimCleared={} clickProgressCleared={} expectedCombatClaimCleared={} pendingCombatTicketCleared={} preparedDialogActionCleared={} preparedActionJobCleared={} observationResetGeneration={} reason={}",
                    windowId, taskRunId, round, attemptId, pathingCleared, scheduleCleared,
                    clickClaimCleared, clickProgressCleared, expectedCombatClaimCleared,
                    pendingCombatTicketCleared, preparedDialogActionCleared, preparedActionJobCleared,
                    resetGeneration, normalize(reason));
            return new ExactAttemptAbandonResult(
                    taskRunId, round, attemptId, true, pathingCleared, true, scheduleCleared,
                    clickClaimCleared, clickProgressCleared, expectedCombatClaimCleared,
                    pendingCombatTicketCleared, preparedDialogActionCleared,
                    preparedActionJobCleared, false);
        }
    }

    private boolean exactAttemptMatches(
            PreparedActionJob job, String taskRunId, int round, String attemptId) {
        WindowNativeBinding binding = nativeBinding;
        return job != null
                && Objects.equals(taskRunId, job.getTaskRunId())
                && round == job.getRound()
                && Objects.equals(attemptId, job.getAttemptId())
                && Objects.equals(windowId, job.getWindowId())
                && binding != null
                && Objects.equals(binding.getNativeHandle(), job.getHwnd());
    }

    private static boolean exactAttemptMatches(
            WindowExpectedCombatEnterClaim claim, String taskRunId, String attemptId) {
        return claim != null
                && "XIULUO_V2".equalsIgnoreCase(claim.taskCode())
                && Objects.equals(taskRunId, claim.businessTaskRunId())
                && Objects.equals(attemptId, claim.attemptId());
    }

    public record ExactAttemptAbandonResult(
            String taskRunId,
            int round,
            String attemptId,
            boolean exactAttemptMatched,
            boolean pathingCleared,
            boolean observationLineageCleared,
            boolean scheduleCleared,
            boolean clickClaimCleared,
            boolean clickProgressCleared,
            boolean expectedCombatClaimCleared,
            boolean pendingCombatTicketCleared,
            boolean preparedDialogActionCleared,
            boolean preparedActionJobCleared,
            boolean combatAlreadyConfirmed) {

        private static ExactAttemptAbandonResult noMatch(String taskRunId, int round, String attemptId) {
            return new ExactAttemptAbandonResult(taskRunId, round, attemptId,
                    false, false, false, false, false, false, false, false, false, false, false);
        }

        private static ExactAttemptAbandonResult combatConfirmed(String taskRunId, int round, String attemptId) {
            return new ExactAttemptAbandonResult(taskRunId, round, attemptId,
                    true, false, false, false, false, false, false, false, false, false, true);
        }
    }

    /**
     * CR253 review P1: drop an XIULUO_ENTER_BATTLE prepared dialog action whose stamped attempt
     * identity no longer matches the green chain.
     *
     * @param allowedAttemptId attemptId that may survive; null means no stamped action may survive.
     * @param reason diagnostic reason written to logs.
     */
    private void discardStaleXiuluoEnterBattlePrepared(String allowedAttemptId, String reason) {
        PreparedDialogAction current = preparedDialogAction.get();
        if (current == null || current.getOperation() != DialogOperation.XIULUO_ENTER_BATTLE) {
            return;
        }
        String stampedAttemptId = current.getIntentId();
        if (stampedAttemptId == null) {
            // Unstamped: prepared outside any green-chain schedule; owned by its own consumer.
            return;
        }
        if (allowedAttemptId != null && allowedAttemptId.equals(stampedAttemptId)) {
            return;
        }
        if (preparedDialogAction.compareAndSet(current, null)) {
            clearReadyDialogPreparationStatus();
            log.info("[latency] event=window.prepared-dialog.stale-attempt-discarded windowId={} reason={} stampedAttemptId={} allowedAttemptId={} source={}",
                    windowId, reason, stampedAttemptId, allowedAttemptId, normalize(current.getSource()));
        }
    }

    public Optional<XiuluoGreenChainSchedule> getXiuluoGreenChainSchedule() {
        return Optional.ofNullable(xiuluoGreenChainSchedule.get());
    }

    /**
     * CR253 publish gate (background half of the double invalidation gate): a typed prepared job is
     * stored only while its full identity still matches the current green-chain schedule and this
     * window's native binding.
     *
     * @param job candidate job stamped by the producer.
     * @param reason diagnostic reason written to logs.
     * @return true when the job was published.
     */
    public boolean publishPreparedActionJob(PreparedActionJob job, String reason) {
        synchronized (xiuluoKandaTransitionLock) {
            if (job == null || job.getType() == null) {
                return false;
            }
            XiuluoGreenChainSchedule schedule = xiuluoGreenChainSchedule.get();
            if (schedule == null || !schedule.sameIdentity(job)) {
                log.info("[latency] event=window.prepared-job.publish-rejected windowId={} reason={} job=[{}] schedule=[{}]",
                        windowId, normalize(reason), job.identityText(),
                        schedule == null ? null : schedule.identityText());
                return false;
            }
            WindowNativeBinding binding = nativeBinding;
            String currentHwnd = binding == null ? null : binding.getNativeHandle();
            if (currentHwnd == null || !currentHwnd.equals(job.getHwnd()) || !windowId.equals(job.getWindowId())) {
                log.info("[latency] event=window.prepared-job.publish-rejected windowId={} reason={}:binding-mismatch job=[{}] currentHwnd={}",
                        windowId, normalize(reason), job.identityText(), currentHwnd);
                return false;
            }
            PreparedActionJob previous = preparedActionJobs.put(job.getType(), job);
            log.info("[latency] event=window.prepared-job.publish windowId={} reason={} job=[{}] source={} click=({}, {}) replaced={}",
                    windowId, normalize(reason), job.identityText(), normalize(job.getSource()),
                    job.getWindowRelativeX(), job.getWindowRelativeY(), previous != null);
            return true;
        }
    }

    /**
     * CR253 consume gate (foreground half of the double invalidation gate): the consumer supplies
     * the identity it is currently executing under; a stored job that does not match is a stale
     * leftover from an older attempt/round/run and is discarded here — never clicked.
     *
     * @param type typed work kind the consumer owns.
     * @param expectedTaskRunId task run the consumer is executing.
     * @param expectedRound current 修罗 round.
     * @param expectedAttemptId current green-click attemptId (pathing intent id).
     * @param reason diagnostic reason written to logs.
     * @return consumed job, or null when absent or stale.
     */
    public PreparedActionJob consumePreparedActionJobValidated(PreparedActionJobType type,
                                                               String expectedTaskRunId,
                                                               int expectedRound,
                                                               String expectedAttemptId,
                                                               String reason) {
        synchronized (xiuluoKandaTransitionLock) {
            if (type == null) {
                return null;
            }
            PreparedActionJob job = preparedActionJobs.get(type);
            if (job == null) {
                return null;
            }
            WindowNativeBinding binding = nativeBinding;
            String currentHwnd = binding == null ? null : binding.getNativeHandle();
            boolean identityMatched = expectedAttemptId != null
                    && expectedAttemptId.equals(job.getAttemptId())
                    && expectedTaskRunId != null && expectedTaskRunId.equals(job.getTaskRunId())
                    && expectedRound == job.getRound()
                    && windowId.equals(job.getWindowId())
                    && currentHwnd != null && currentHwnd.equals(job.getHwnd());
            if (!identityMatched) {
                preparedActionJobs.remove(type, job);
                log.info("[latency] event=window.prepared-job.stale-discarded windowId={} reason={} job=[{}] expectedTaskRunId={} expectedRound={} expectedAttemptId={} currentHwnd={}",
                        windowId, normalize(reason), job.identityText(), expectedTaskRunId, expectedRound,
                        expectedAttemptId, currentHwnd);
                return null;
            }
            if (!preparedActionJobs.remove(type, job)) {
                return null;
            }
            log.info("[latency] event=window.prepared-job.consume windowId={} reason={} job=[{}] source={} preparedAgeMs={}",
                    windowId, normalize(reason), job.identityText(), normalize(job.getSource()),
                    ageMs(System.currentTimeMillis(), job.getPreparedAtMs()));
            return job;
        }
    }

    /**
     * Return one pending typed prepared job without consuming it (producer dedupe only).
     */
    public PreparedActionJob peekPreparedActionJob(PreparedActionJobType type) {
        return type == null ? null : preparedActionJobs.get(type);
    }

    /**
     * CR253: drop all pending typed prepared jobs without touching the schedule.
     */
    public void clearPreparedActionJobs(String reason) {
        if (preparedActionJobs.isEmpty()) {
            return;
        }
        preparedActionJobs.forEach((type, job) ->
                log.info("[latency] event=window.prepared-job.clear windowId={} reason={} job=[{}]",
                        windowId, normalize(reason), job.identityText()));
        preparedActionJobs.clear();
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

    public PendingTransferChoiceMemory consumePendingTransferChoiceMemoryIfPathingCurrent(
            String expectedIntentId, String expectedSource) {
        WindowPathingIntent active = getActivePathingIntent().orElse(null);
        if (active == null || !Objects.equals(active.getIntentId(), normalize(expectedIntentId))
                || !Objects.equals(active.getSource(), normalize(expectedSource))) {
            return null;
        }
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
     * Retain a cloud-authorized route click until the pathing watcher reports its outcome.
     *
     * @param outcome window-relative route click evidence. Null clears the live record.
     */
    public void updatePendingRouteOutcome(PendingRouteOutcome outcome) {
        pendingRouteOutcome.set(outcome);
    }

    /**
     * Ask the runner owner to replace the live route outcome without allowing this pure runtime
     * context to send HTTP.
     *
     * <p>The current outcome remains installed until the runner has reported it as
     * {@code ABANDONED}; only then may the runner install this replacement. This ordering keeps a
     * second navigation from silently dropping the first cloud-issued decision id.</p>
     *
     * @param outcome next live route outcome; null only clears the live slot without creating a
     *                replacement record.
     * @param reason diagnostic replacement reason for the runner's cloud outcome report.
     */
    public void requestPendingRouteOutcomeReplacement(PendingRouteOutcome outcome, String reason) {
        if (outcome == null) {
            return;
        }
        if (pendingRouteOutcome.get() == null) {
            pendingRouteOutcome.set(outcome);
            return;
        }
        pendingRouteOutcomeReplacements.offer(new PendingRouteOutcomeReplacement(outcome, normalize(reason)));
        wakeObserver("route-outcome-replacement-requested:" + normalize(reason));
    }

    public PendingRouteOutcome consumePendingRouteOutcome() {
        return pendingRouteOutcome.getAndSet(null);
    }

    public PendingRouteOutcome consumePendingRouteOutcomeIfPathingCurrent(
            String expectedIntentId, String expectedSource) {
        WindowPathingIntent active = getActivePathingIntent().orElse(null);
        if (active == null || !Objects.equals(active.getIntentId(), normalize(expectedIntentId))
                || !Objects.equals(active.getSource(), normalize(expectedSource))) {
            return null;
        }
        return pendingRouteOutcome.getAndSet(null);
    }

    /**
     * Mark the live outcome abandoned for the runner owner. This context never reports directly.
     */
    public void markPendingRouteOutcomeAbandoned(String reason) {
        PendingRouteOutcome pending = pendingRouteOutcome.getAndSet(null);
        if (pending != null) {
            pendingRouteOutcomeAbandonments.offer(new PendingRouteOutcomeAbandonment(pending, normalize(reason)));
            wakeObserver("route-outcome-abandoned:" + normalize(reason));
        }
    }

    public PendingRouteOutcomeAbandonment pollPendingRouteOutcomeAbandonment() {
        return pendingRouteOutcomeAbandonments.poll();
    }

    public PendingRouteOutcomeReplacement pollPendingRouteOutcomeReplacement() {
        return pendingRouteOutcomeReplacements.poll();
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
        WindowPathingSnapshot snapshot = WindowPathingSnapshot.builder()
                .state(WindowPathingState.ACTIVE)
                .intent(intent)
                .locationChangedAtMs(intent.getCreatedAtMs())
                .message("pathing intent registered")
                .build();
        pathingSnapshot.set(snapshot);
        log.info("window pathing intent registered without stale location baseline: windowId={} intentId={} source={}",
                windowId, intent.getIntentId(), intent.getSource());
    }

    /**
     * Commits a location recognized from one complete map-name/coordinate ROI for this exact window.
     * The value updates compatibility caches, but never establishes movement for a newly registered intent.
     *
     * @param mapName recognized canonical game map name; nonblank.
     * @param mapX recognized logical map X coordinate, in game tiles.
     * @param mapY recognized logical map Y coordinate, in game tiles.
     * @param observedAtMs capture/result time in epoch milliseconds.
     * @param source diagnostic producer name; nullable.
     */
    public void updateRecognizedPlayerLocation(String mapName,
                                               int mapX,
                                               int mapY,
                                               long observedAtMs,
                                               String source) {
        PlayerCharacter me = gameState.getMe();
        me.setCurrentMapName(mapName);
        me.setX(mapX);
        me.setY(mapY);
        while (true) {
            WindowPathingSnapshot current = pathingSnapshot.get();
            if (current == null) {
                WindowPathingSnapshot recognized = WindowPathingSnapshot.builder()
                        .state(WindowPathingState.NONE)
                        .currentMapName(mapName)
                        .currentX(mapX)
                        .currentY(mapY)
                        .message(normalize(source))
                        .updatedAtMs(observedAtMs)
                        .build();
                lastKnownPathingLocation.set(recognized);
                return;
            }
            WindowPathingSnapshot recognized = current.toBuilder()
                    .currentMapName(mapName)
                    .currentX(mapX)
                    .currentY(mapY)
                    .updatedAtMs(observedAtMs)
                    .build();
            if (pathingSnapshot.compareAndSet(current, recognized)) {
                lastKnownPathingLocation.set(recognized);
                return;
            }
        }
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
            if (snapshot.getCurrentX() != null && snapshot.getCurrentY() != null) {
                lastKnownPathingLocation.set(snapshot);
            }
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

    /**
     * Clear a terminal pathing signal only when both its owner source and current state still match.
     *
     * <p>This is used by task-specific recovery code after the watcher has already written and
     * published a terminal snapshot. The state guard prevents a stale task thread from clearing a
     * newer active navigation intent that reused the same source family.</p>
     *
     * @param sourcePrefixes allowed prefixes of {@link WindowPathingIntent#getSource()}.
     * @param expectedState required current pathing state, usually {@link WindowPathingState#STOPPED_AWAY}.
     * @param reason diagnostic reason written into the replacement idle snapshot.
     * @return true when a matching snapshot was cleared.
     */
    public boolean clearPathingSignalIfSourcePrefixesAndState(List<String> sourcePrefixes,
                                                              WindowPathingState expectedState,
                                                              String reason) {
        if (sourcePrefixes == null || sourcePrefixes.isEmpty() || expectedState == null) {
            return false;
        }
        List<String> normalizedPrefixes = sourcePrefixes.stream()
                .map(WindowRuntimeContext::normalize)
                .filter(Objects::nonNull)
                .toList();
        if (normalizedPrefixes.isEmpty()) {
            return false;
        }
        while (true) {
            WindowPathingSnapshot snapshot = pathingSnapshot.get();
            if (snapshot == null || !snapshot.hasActiveIntent() || snapshot.getState() != expectedState) {
                return false;
            }
            WindowPathingIntent intent = snapshot.getIntent();
            String activeSource = intent == null ? null : normalize(intent.getSource());
            String matchedPrefix = normalizedPrefixes.stream()
                    .filter(prefix -> activeSource != null && activeSource.startsWith(prefix))
                    .findFirst()
                    .orElse(null);
            if (matchedPrefix == null) {
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
            log.info("window pathing intent cleared by source prefixes and state: windowId={} intentId={} source={} matchedPrefix={} target={} state={} reason={}",
                    windowId, intent.getIntentId(), activeSource, matchedPrefix, intent.getTargetMapName(),
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

    /**
     * Cloud-commanded recovery: asks the observation sampler to drop its retained pathing-fact
     * lineage on its next sampling cycle. The sampler lives on the observation thread, so the
     * request travels through this monotonic generation counter instead of a direct call.
     */
    public void requestObservationPathingFactReset(String reason) {
        long generation = observationPathingFactResetGeneration.incrementAndGet();
        log.info("[local-runner] observation pathing-fact reset requested: windowId={} generation={} reason={}",
                windowId, generation, normalize(reason));
    }

    /** Current pathing-fact reset generation; the sampler consumes changes of this value. */
    public long getObservationPathingFactResetGeneration() {
        return observationPathingFactResetGeneration.get();
    }

    public void markQueued(TaskType taskType) {
        this.lastTaskType = resolveTaskForRuntimeEvent(taskType);
        this.status = WindowRuntimeStatus.QUEUED;
        this.lastMessage = "任务已排队：" + this.lastTaskType.getDisplayName();
        this.lastResultMessage = null;
    }

    public void markStarted(TaskType taskType, WindowTaskRunProgress initialProgress) {
        this.lastTaskType = resolveTaskForRuntimeEvent(taskType);
        this.status = WindowRuntimeStatus.RUNNING;
        this.lastStartedAt = LocalDateTime.now();
        this.lastMessage = "任务开始：" + this.lastTaskType.getDisplayName();
        this.lastResult = null;
        this.lastResultMessage = null;
        if (initialProgress != null
                && initialProgress.getTaskType() == this.lastTaskType
                && initialProgress.getTotalRuns() > 0) {
            runningTaskProgress.set(initialProgress);
        } else {
            clearTaskRunProgress();
        }
        clearPausedTaskRunProgress("remote task start accepted");
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
            clearPausedTaskRunProgress("task finished");
            clearIdentitySuspension("task finished");
            stashLastTaskOwnerBeforeWipe();
            taskOwnerPlayerId = null;
            taskOwnerPlayerName = null;
        }
    }

    private void stashLastTaskOwnerBeforeWipe() {
        if (taskOwnerPlayerId != null) {
            lastTaskOwnerPlayerId = taskOwnerPlayerId;
            lastTaskOwnerPlayerName = taskOwnerPlayerName;
        }
    }

    /** 上一个 run 的任务主人 id（终局抹除前侧存）；自动重启守门用，可能为 null。 */
    public String getLastTaskOwnerPlayerId() {
        return lastTaskOwnerPlayerId;
    }

    public String getLastTaskOwnerPlayerName() {
        return lastTaskOwnerPlayerName;
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
            clearPausedTaskRunProgress("task queue finished");
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

    /**
     * Clears every task-run-scoped fact that could make the next accepted task continue an older run.
     *
     * <p>Window registration, HWND binding, role, selected task, and parsed player identity deliberately
     * survive this boundary. Everything else that can schedule, wake, click, resume navigation, or interpret
     * a former task is discarded. The separate pause-only numeric progress snapshot is deliberately not executable
     * state and is cleared by explicit cold/stop/terminal lifecycle owners. This method is called both before a
     * newly accepted remote turn and after a remote turn becomes terminal; do not weaken it into a UI-only reset.</p>
     *
     * @param reason lifecycle boundary recorded in diagnostics; nullable.
     */
    /**
     * 2026-08-23 用户契约（停止=彻底清空）：清掉本窗口跨轮存活的"现实记忆"——识别到的
     * 玩家位置/坐标、任何相位的对话准备残留、维护广播已处理表。仅由 fresh-start 复位链
     * 调用（NORMAL 启动、崩溃自动重启）；暂停恢复不得调用。
     */
    public void clearCrossRunRealityMemory(String reason) {
        lastKnownPathingLocation.set(null);
        // 审查修正：该字段的不变量是永不为 null（其余清理点全部写 none()）。
        dialogPreparationStatus.set(DialogPreparationStatus.none());
        localMaintenanceBroadcastHandledAtByAction.clear();
        PlayerCharacter me = gameState.getMe();
        me.setCurrentMapName(null);
        me.setX(0);
        me.setY(0);
        log.info("[fresh-start] cross-run reality memory cleared: windowId={} reason={}", windowId, reason);
    }

    public void clearTaskExecutionState(String reason) {
        String clearReason = normalize(reason) == null ? "task execution reset" : normalize(reason);

        // Attempt-owned work must go first so no prepared click can outlive the intent that authorized it.
        clearXiuluoGreenChainSchedule(clearReason);
        clearPreparedDialogAction(clearReason);
        clearPlayerScopedTransientState(clearReason);
        pendingRouteOutcomeAbandonments.clear();
        pendingRouteOutcomeReplacements.clear();

        // These are queue/run facts rather than player identity. A new task must rediscover every one of them.
        clearTaskQueueStartupPreparationState(clearReason);
        invalidateReturnHomeReplayLifecycle(clearReason);
        clearExpectedCombatEnterClaim(clearReason);
        clearPendingDirectCombatEnterClaim(clearReason);
        clearTiantingDialogOptionClaim();
        clearTiantingFengyaoPending();
        pendingSmartClickEvidenceProofToken.set(null);
        dialogFrameObservedAtMs.set(0L);
        localCombatVisible = false;
        localCombatGeneration.set(0L);
        clearIdentitySuspension(clearReason);
        stashLastTaskOwnerBeforeWipe();
        taskOwnerPlayerId = null;
        taskOwnerPlayerName = null;
        clearTaskRunProgress();
        gameState.resetRuntimeState();

        log.info("[task-runtime-reset] windowId={} reason={} retainedBinding={} retainedRole={} retainedSelectedTask={}",
                windowId, clearReason, nativeBinding != null && nativeBinding.hasNativeHandle(), role, selectedTaskType);
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
        clearPausedTaskRunProgress("runtime reset");
        clearTaskExecutionState("runtime reset");
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

    private static boolean sameNativeBindingSnapshot(WindowNativeBinding left, WindowNativeBinding right) {
        if (left == right) {
            return true;
        }
        return left != null
                && right != null
                && Objects.equals(left.getNativeHandle(), right.getNativeHandle())
                && Objects.equals(left.getTitle(), right.getTitle())
                && Objects.equals(left.getClassName(), right.getClassName())
                && left.getProcessId() == right.getProcessId()
                && left.getX() == right.getX()
                && left.getY() == right.getY()
                && left.getWidth() == right.getWidth()
                && left.getHeight() == right.getHeight();
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
        boolean playerChanged = oldIdentity != null
                && newIdentity != null
                && !newIdentity.samePlayer(oldIdentity);
        if (!titleChanged || !playerChanged) {
            return WindowIdentityDrift.none(windowId, previous, next, playerIdentityEpoch.get());
        }
        long epoch = playerIdentityEpoch.incrementAndGet();
        return WindowIdentityDrift.detected(windowId, previous, next, oldIdentity, newIdentity, epoch);
    }

    private boolean isIdentityEnrichment(WindowNativeBinding previous, WindowNativeBinding next) {
        if (previous == null || next == null || !sameNativeWindow(previous, next)) {
            return false;
        }
        WindowTitleIdentity oldIdentity = WindowTitleIdentityParser.parse(previous.getTitle()).orElse(null);
        WindowTitleIdentity newIdentity = WindowTitleIdentityParser.parse(next.getTitle()).orElse(null);
        return oldIdentity == null && newIdentity != null;
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
        markPendingRouteOutcomeAbandoned(reason);
        clearTaskTrackerPanelCache(reason);
        taskTrackerAnchorMemory.set(null);
        clearTaskTrackerPanelNegativeResult(reason);
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

    private record TaskQueueStartupUiCleanupProbe(String taskCode,
                                                  boolean clean,
                                                  long createdAtMs,
                                                  String source) {
    }

    private static final class PendingDirectCombatEnterTicket {
        private final WindowExpectedCombatEnterClaim claim;
        private final long armedAtMs;

        private PendingDirectCombatEnterTicket(WindowExpectedCombatEnterClaim claim, long armedAtMs) {
            this.claim = claim;
            this.armedAtMs = armedAtMs;
        }

        private WindowExpectedCombatEnterClaim claim() {
            return claim;
        }

        private long armedAtMs() {
            return armedAtMs;
        }
    }

    private record XiuluoLocalKandaClickProgress(
            String attemptId, int executedClicks, long lastClickAtMs, boolean failureReported,
            boolean combatConfirmed) {
        private XiuluoLocalKandaClickProgress withFailureReported() {
            return new XiuluoLocalKandaClickProgress(attemptId, executedClicks, lastClickAtMs, true, combatConfirmed);
        }

        private XiuluoLocalKandaClickProgress withCombatConfirmed() {
            return new XiuluoLocalKandaClickProgress(attemptId, executedClicks, lastClickAtMs, failureReported, true);
        }
    }

    private record XiuluoConfirmedCombatAttempt(
            String taskRunId, int round, String attemptId, Long combatGeneration) {
        private boolean matches(String expectedTaskRunId, int expectedRound, String expectedAttemptId) {
            return Objects.equals(taskRunId, expectedTaskRunId)
                    && round == expectedRound
                    && Objects.equals(attemptId, expectedAttemptId);
        }
    }

    /**
     * A local state-only request for the runner owner to report a discarded cloud route decision.
     */
    public record PendingRouteOutcomeAbandonment(PendingRouteOutcome outcome, String reason) {
    }

    /**
     * A local state-only request for the runner to report the previous outcome before installing
     * the next one.
     */
    public record PendingRouteOutcomeReplacement(PendingRouteOutcome outcome, String reason) {
    }

    public interface PreparedDialogActionValidator {
        PreparedDialogAction validate(PreparedDialogAction action);
    }
}
