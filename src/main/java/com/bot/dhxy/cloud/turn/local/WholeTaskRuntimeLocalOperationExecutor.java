package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnDialogRuntimeFact;
import com.bot.dhxy.cloud.turn.protocol.TurnExactAttemptRecoveryResetAck;
import com.bot.dhxy.cloud.turn.protocol.TurnCombatCleanupFact;
import com.bot.dhxy.cloud.turn.protocol.TurnPreBattleFact;
import com.bot.dhxy.cloud.turn.protocol.TurnPendingRouteOutcome;
import com.bot.dhxy.cloud.turn.protocol.TurnPendingTransferChoice;
import com.bot.dhxy.cloud.turn.protocol.TurnPathingIntent;
import com.bot.dhxy.cloud.turn.protocol.TurnPathingSnapshot;
import com.bot.dhxy.cloud.turn.protocol.TurnWholeTaskRuntimeArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnWholeTaskRuntimeResult;
import com.bot.dhxy.model.dialog.DialogPreparationStatus;
import com.bot.dhxy.model.navigation.PendingRouteOutcome;
import com.bot.dhxy.model.navigation.PendingTransferChoiceMemory;
import com.bot.dhxy.model.navigation.WorldMapRouteResultMode;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowFlyingState;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Closed adapter for the whole-task runtime local operations (TURN-35 Amendment #6/#7).
 *
 * <p>The Cloud whole Tasks own business ordering; the DHXY-local runtime remains the sole owner of
 * pathing/timer/dialog-interest/progress/flying facts and physical input exclusivity. This adapter
 * only forwards one validated call to the existing bound {@link WindowRuntimeContext} or
 * {@link GameStateUtil} method and maps the outcome to a typed
 * {@link TurnWholeTaskRuntimeResult} (boolean/enum/timestamp/cleared-intent identity only — never a
 * local object reference). It never copies detector/watcher algorithms, adds no poll/sleep/TTL, and
 * introduces no second store.</p>
 *
 * <p>The exact-window binding is the current bound runtime from {@link WindowTaskContextHolder}; a
 * missing bound runtime is a fail-closed result and never a fabricated business false.</p>
 */
@Component
public final class WholeTaskRuntimeLocalOperationExecutor {

    private final WindowTaskContextHolder windowTaskContextHolder;
    private final LocalMovementFactMechanics movementFacts;
    private final XiuluoAcceptDialogLocalOperation xiuluoAcceptDialogLocalOperation;
    private final JianghuLilianDialogLocalOperation jianghuLilianDialogLocalOperation;
    private final CatchGhostDialogLocalOperation catchGhostDialogLocalOperation;
    private final com.bot.dhxy.window.observation.DeferredReturnHomeReplayCoordinator returnHomeReplayCoordinator;
    private final NpcArrivalFrameFifoLocalExecutor npcArrivalFrameFifoLocalExecutor;
    private final ObjectMapper objectMapper;

    @Autowired
    public WholeTaskRuntimeLocalOperationExecutor(WindowTaskContextHolder windowTaskContextHolder,
                                                  LocalMovementFactMechanics movementFacts,
                                                  XiuluoAcceptDialogLocalOperation xiuluoAcceptDialogLocalOperation,
                                                  JianghuLilianDialogLocalOperation jianghuLilianDialogLocalOperation,
                                                  CatchGhostDialogLocalOperation catchGhostDialogLocalOperation,
                                                  com.bot.dhxy.window.observation.DeferredReturnHomeReplayCoordinator returnHomeReplayCoordinator,
                                                  NpcArrivalFrameFifoLocalExecutor npcArrivalFrameFifoLocalExecutor,
                                                  ObjectMapper objectMapper) {
        this.windowTaskContextHolder = Objects.requireNonNull(windowTaskContextHolder, "windowTaskContextHolder");
        this.movementFacts = Objects.requireNonNull(movementFacts, "movementFacts");
        this.xiuluoAcceptDialogLocalOperation = Objects.requireNonNull(
                xiuluoAcceptDialogLocalOperation, "xiuluoAcceptDialogLocalOperation");
        this.jianghuLilianDialogLocalOperation = Objects.requireNonNull(
                jianghuLilianDialogLocalOperation, "jianghuLilianDialogLocalOperation");
        this.catchGhostDialogLocalOperation = Objects.requireNonNull(
                catchGhostDialogLocalOperation, "catchGhostDialogLocalOperation");
        this.returnHomeReplayCoordinator = Objects.requireNonNull(
                returnHomeReplayCoordinator, "returnHomeReplayCoordinator");
        this.npcArrivalFrameFifoLocalExecutor = Objects.requireNonNull(
                npcArrivalFrameFifoLocalExecutor, "npcArrivalFrameFifoLocalExecutor");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    WholeTaskRuntimeLocalOperationExecutor(WindowTaskContextHolder windowTaskContextHolder,
                                           LocalMovementFactMechanics movementFacts,
                                           ObjectMapper objectMapper) {
        this.windowTaskContextHolder = Objects.requireNonNull(windowTaskContextHolder, "windowTaskContextHolder");
        this.movementFacts = Objects.requireNonNull(movementFacts, "movementFacts");
        this.xiuluoAcceptDialogLocalOperation = null;
        this.jianghuLilianDialogLocalOperation = null;
        this.catchGhostDialogLocalOperation = null;
        this.returnHomeReplayCoordinator = null;
        this.npcArrivalFrameFifoLocalExecutor = null;
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Execute one validated whole-task runtime local-Service call.
     *
     * @param call typed closed local-Service request; only whole-task runtime operations are supported.
     * @return a completed typed result, or a fail-closed result when no window runtime is bound.
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call,
                                         String actionId,
                                         int sourceStepIndex,
                                         TurnContinuationGateway continuationGateway) {
        if (call == null || call.operation() == null || call.wholeTaskRuntime() == null) {
            return LocalServiceExecution.failed("INVALID_WHOLE_TASK_CALL", null);
        }
        if (call.operation() == com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation.XIULUO_ACCEPT_DIALOG_TEMPLATE) {
            if (xiuluoAcceptDialogLocalOperation == null) {
                return LocalServiceExecution.failed("XIULUO_ACCEPT_DIALOG_NOT_WIRED", null);
            }
            return completedEnum("WHOLE_TASK_XIULUO_ACCEPT_DIALOG_TEMPLATE",
                    xiuluoAcceptDialogLocalOperation.execute().name());
        }
        if (call.operation()
                == com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation.JIANGHU_LILIAN_ACCEPT_DIALOG_TEMPLATE) {
            if (jianghuLilianDialogLocalOperation == null) {
                return LocalServiceExecution.failed("JIANGHU_LILIAN_DIALOG_NOT_WIRED", null);
            }
            return completedEnum("WHOLE_TASK_JIANGHU_LILIAN_ACCEPT_DIALOG_TEMPLATE",
                    jianghuLilianDialogLocalOperation.executeAccept().name());
        }
        if (call.operation()
                == com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation.CATCH_GHOST_ACCEPT_DIALOG_TEMPLATE) {
            if (catchGhostDialogLocalOperation == null) {
                return LocalServiceExecution.failed("CATCH_GHOST_DIALOG_NOT_WIRED", null);
            }
            return completedEnum("WHOLE_TASK_CATCH_GHOST_ACCEPT_DIALOG_TEMPLATE",
                    catchGhostDialogLocalOperation.executeAccept().name());
        }
        if (call.operation()
                == com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation.CATCH_GHOST_CANCEL_DIALOG_TEMPLATE) {
            if (catchGhostDialogLocalOperation == null) {
                return LocalServiceExecution.failed("CATCH_GHOST_DIALOG_NOT_WIRED", null);
            }
            return completedEnum("WHOLE_TASK_CATCH_GHOST_CANCEL_DIALOG_TEMPLATE",
                    catchGhostDialogLocalOperation.executeCancel().name());
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return LocalServiceExecution.failed("NO_BOUND_WINDOW_RUNTIME", null);
        }
        TurnWholeTaskRuntimeArguments a = call.wholeTaskRuntime();
        return switch (call.operation()) {
            case WHOLE_TASK_PATHING_REGISTER -> {
                runtime.markPathingStarted(toPathingIntent(a.pathingIntent()));
                yield completedBoolean("WHOLE_TASK_PATHING_REGISTERED", true);
            }
            case WHOLE_TASK_RETURN_HOME_REPLAY_ARM -> {
                com.bot.dhxy.window.model.WindowNativeBinding binding = runtime.getNativeBinding();
                WindowRuntimeContext.ReplayArmResult result = returnHomeReplayCoordinator.arm(
                        runtime,
                        a.taskCode(),
                        a.replayObservationRunId(),
                        a.replayBusinessTaskRunId(),
                        runtime.getWindowId(),
                        binding == null ? null : binding.getNativeHandle());
                yield completedEnum("WHOLE_TASK_RETURN_HOME_REPLAY_ARM", result.name());
            }
            case WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM -> {
                com.bot.dhxy.window.model.WindowNativeBinding binding = runtime.getNativeBinding();
                boolean registered = binding != null && runtime.armPendingDirectCombatEnterClaim(
                        new com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim(
                                a.expectedCombatClaimId(),
                                a.expectedCombatObservationRunId(),
                                a.expectedCombatBusinessTaskRunId(),
                                a.taskCode(),
                                a.expectedCombatAttemptId(),
                                runtime.getWindowId(),
                                binding.getNativeHandle(),
                                "local-alt-a",
                                null));
                yield completedBoolean("WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM", registered);
            }
            case WHOLE_TASK_PATHING_READ -> completed(
                    "WHOLE_TASK_PATHING_READ",
                    new TurnWholeTaskRuntimeResult(null, null, null, null, null, null,
                            toTurnPathingSnapshot(runtime.getPathingSnapshot())));
            case WHOLE_TASK_PATHING_CLEAR_INTENT -> {
                Optional<WindowPathingIntent> active = runtime.getActivePathingIntent();
                String clearedIntentId = null;
                if (active.isPresent() && a.intentId().equals(active.get().getIntentId())) {
                    // clearPathingSignal preserves the baseline pending transfer-choice cleanup.
                    runtime.clearPathingSignal(a.source());
                    clearedIntentId = active.get().getIntentId();
                }
                yield completed("WHOLE_TASK_PATHING_CLEAR_INTENT",
                        new TurnWholeTaskRuntimeResult(null, null, null, clearedIntentId));
            }
            case WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX -> completedBoolean(
                    "WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX",
                    runtime.clearPathingSignalIfSourcePrefix(a.sourcePrefix(), a.source()));
            case WHOLE_TASK_PATHING_CLEAR -> {
                // Baseline unconditional clear (Amendment #11): set the snapshot to NONE and drop the
                // pending transfer-choice memory in one call, with no intent/prefix read-then-decide.
                runtime.clearPathingSignal(a.source());
                yield completedBoolean("WHOLE_TASK_PATHING_CLEAR", true);
            }
            case WHOLE_TASK_RECOVERY_RESET -> {
                /*
                 * Cloud-commanded recovery reconcile: when the Cloud abandons a round (watchdog
                 * timeout / round failure), local state left behind by that round must be reset in
                 * the same recovery decision — otherwise the two sides drift (stale pathing intent,
                 * retained observation pathing-fact lineage) and the Cloud keeps deciding on a stale
                 * mirror. Travels over the command plane, so it works even while the observation
                 * plane is failing.
                 */
                if (a.recoveryTaskRunId() == null) {
                    // Legacy CatchGhost/Xinshou callers keep their source-only pathing reconcile and
                    // cannot clear any exact 修罗 attempt-owned slot.
                    runtime.clearPathingSignal(a.source());
                    runtime.requestObservationPathingFactReset(a.source());
                    yield completedBoolean("WHOLE_TASK_RECOVERY_RESET", true);
                }
                WindowRuntimeContext.ExactAttemptAbandonResult reset = runtime.abandonExactXiuluoAttempt(
                        a.recoveryTaskRunId(), a.recoveryRound(), a.recoveryAttemptId(), a.source());
                TurnExactAttemptRecoveryResetAck ack = new TurnExactAttemptRecoveryResetAck(
                        reset.taskRunId(), reset.round(), reset.attemptId(), reset.exactAttemptMatched(),
                        reset.pathingCleared(), reset.observationLineageCleared(), reset.scheduleCleared(),
                        reset.clickClaimCleared(), reset.clickProgressCleared(),
                        reset.expectedCombatClaimCleared(), reset.pendingCombatTicketCleared(),
                        reset.preparedDialogActionCleared(), reset.preparedActionJobCleared(),
                        reset.combatAlreadyConfirmed());
                yield completed("WHOLE_TASK_RECOVERY_RESET",
                        new TurnWholeTaskRuntimeResult(
                                null, null, null, null, null, null, null, null, null, null, ack));
            }
            case WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP -> completedBoolean(
                    "WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP",
                    runtime.upgradeActivePathingIntentTargetMap(a.intentId(), a.targetMapName(), a.source()));
            case WHOLE_TASK_MOVEMENT_INTENT_RECORD -> {
                if (a.protectionMs() != null) {
                    movementFacts.recordIntent(a.source(), a.protectionMs());
                } else {
                    movementFacts.recordIntent(a.source(), null);
                }
                yield completedBoolean("WHOLE_TASK_MOVEMENT_INTENT_RECORDED", true);
            }
            case WHOLE_TASK_TARGET_MAP_GATE_START -> completedBoolean(
                    "WHOLE_TASK_TARGET_MAP_GATE_START",
                    runtime.startOrdinaryEnterBattleTargetMapGate(
                            taskType(a.taskCode()), a.source(), a.targetMapName(), System.currentTimeMillis()));
            case WHOLE_TASK_TARGET_MAP_GATE_OPEN -> completedBoolean(
                    "WHOLE_TASK_TARGET_MAP_GATE_OPEN",
                    runtime.markOrdinaryEnterBattleTargetMapGateOpened(System.currentTimeMillis()));
            case WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST -> completedBoolean(
                    "WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST",
                    runtime.openOrdinaryEnterBattleTargetMapGateAndUpdateDialogInterest(
                            toDialogInterest(a), a.source(), System.currentTimeMillis()));
            case WHOLE_TASK_PRE_BATTLE_TIMER_READ -> completed(
                    "WHOLE_TASK_PRE_BATTLE_TIMER_READ",
                    new TurnWholeTaskRuntimeResult(null, null, runtime.getOrdinaryPreBattleStartedAtMs(), null));
            case WHOLE_TASK_PRE_BATTLE_FACT_READ -> completed(
                    "WHOLE_TASK_PRE_BATTLE_FACT_READ",
                    new TurnWholeTaskRuntimeResult(null, null, null, null, null, null, null, null,
                            preBattleFact(runtime, false), null, null));
            case WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK -> {
                long now = System.currentTimeMillis();
                long startedAt = runtime.getOrdinaryPreBattleStartedAtMs();
                boolean published = startedAt > 0L && now - startedAt >= 300_000L
                        && runtime.markOrdinaryPreBattleTimeoutPublished(now);
                yield completed("WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK",
                        new TurnWholeTaskRuntimeResult(null, null, null, null, null, null, null, null,
                                preBattleFact(runtime, published), null, null));
            }
            case WHOLE_TASK_PRE_BATTLE_TIMER_START -> completedBoolean(
                    "WHOLE_TASK_PRE_BATTLE_TIMER_START",
                    runtime.startOrdinaryPreBattleTimer(
                            taskType(a.taskCode()), a.source(), a.targetKeyword(), System.currentTimeMillis()));
            case WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE -> completedBoolean(
                    "WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE",
                    runtime.pauseOrdinaryPreBattleTimer(a.blockedMs(), a.source()));
            case WHOLE_TASK_PRE_BATTLE_TIMER_CLEAR -> {
                runtime.clearOrdinaryPreBattleTimer(a.source());
                yield completedBoolean("WHOLE_TASK_PRE_BATTLE_TIMER_CLEAR", true);
            }
            case WHOLE_TASK_DIALOG_INTEREST_UPDATE -> {
                // TURN-40G repair (baseline restoration + review#3 P1): when the payload carries a schedule
                // identity, the probe-only interest and the exact green-chain attempt are installed as ONE
                // WindowRuntimeContext transition (single monitor; stale-job/claim cleanup inside it) — an
                // observation reader can never pair the new interest with the old attempt's schedule. The whole
                // tuple is materialized (fail-fast) BEFORE any runtime mutation, so a partial identity mutates
                // nothing; the wire validator already rejects partial tuples all-or-none.
                boolean scheduleCarried = a.scheduleAttemptId() != null || a.scheduleRound() != null
                        || a.scheduleTaskRunId() != null || a.scheduleOpenedAtMs() != null;
                if (scheduleCarried) {
                    com.bot.dhxy.model.job.XiuluoGreenChainSchedule schedule =
                            com.bot.dhxy.model.job.XiuluoGreenChainSchedule.builder()
                                    .windowId(runtime.getWindowId())
                                    .hwnd(runtime.getNativeBinding() == null
                                            ? null : runtime.getNativeBinding().getNativeHandle())
                                    .observationRunId(a.scheduleObservationRunId())
                                    .taskRunId(Objects.requireNonNull(a.scheduleTaskRunId(), "scheduleTaskRunId"))
                                    .round(Objects.requireNonNull(a.scheduleRound(), "scheduleRound"))
                                    .attemptId(Objects.requireNonNull(a.scheduleAttemptId(), "scheduleAttemptId"))
                                    .openedAtMs(Objects.requireNonNull(a.scheduleOpenedAtMs(), "scheduleOpenedAtMs"))
                                    .build();
                    runtime.updateDialogInterestWithXiuluoGreenChainSchedule(
                            toDialogInterest(a), schedule, a.source());
                } else {
                    runtime.updateDialogInterest(toDialogInterest(a), a.source());
                }
                yield completedBoolean("WHOLE_TASK_DIALOG_INTEREST_UPDATE", true);
            }
            case WHOLE_TASK_DIALOG_INTEREST_CLEAR -> {
                runtime.clearDialogInterest(a.source());
                yield completedBoolean("WHOLE_TASK_DIALOG_INTEREST_CLEAR", true);
            }
            case WHOLE_TASK_PROGRESS_UPDATE -> {
                runtime.updateTaskRunProgress(a.completedRuns(), a.totalRuns());
                yield completedBoolean("WHOLE_TASK_PROGRESS_UPDATE", true);
            }
            case WHOLE_TASK_STARTUP_FLYING_STATE_CONSUME -> completedEnum(
                    "WHOLE_TASK_STARTUP_FLYING_STATE_CONSUME",
                    runtime.consumeTaskQueueStartupFlyingState(a.source()).name());
            case WHOLE_TASK_STARTUP_FLYING_STATE_UPDATE -> {
                runtime.markTaskQueueStartupFlyingState(
                        WindowFlyingState.valueOf(a.startupFlyingState()), a.source());
                yield completedBoolean("WHOLE_TASK_STARTUP_FLYING_STATE_UPDATED", true);
            }
            case WHOLE_TASK_DIALOG_RUNTIME_READ -> {
                // Read-only fact copy of the two baseline dialog signals; the Cloud caller keeps every
                // DialogType.NONE / fresh-vs-unbounded / blocking-phase judgement. Null maxAge = baseline
                // unbounded getVisibleDialogSnapshot(); a nonnull maxAge only filters the visible snapshot.
                // All visible fields are null when no snapshot is present; enum names (including NONE) are
                // carried when a snapshot/status exists so the caller reproduces its checks exactly.
                Optional<WindowDialogSnapshot> visible = a.dialogSnapshotMaxAgeMs() == null
                        ? runtime.getVisibleDialogSnapshot()
                        : runtime.getVisibleDialogSnapshot(a.dialogSnapshotMaxAgeMs());
                DialogPreparationStatus prep = runtime.getDialogPreparationStatus();
                WindowDialogInterest interest = runtime.getDialogInterest().orElse(null);
                TurnDialogRuntimeFact fact = new TurnDialogRuntimeFact(
                        visible.map(d -> d.getType() == null ? null : d.getType().name()).orElse(null),
                        visible.map(WindowDialogSnapshot::getSource).orElse(null),
                        visible.map(WindowDialogSnapshot::getDetectedAtMs).orElse(null),
                        prep == null || prep.getPhase() == null ? null : prep.getPhase().name(),
                        prep == null || prep.getOperation() == null ? null : prep.getOperation().name(),
                        prep == null ? null : prep.getTargetKeyword(),
                        prep == null ? null : prep.getSource(),
                        interest == null || interest.getTaskType() == null
                                ? null : interest.getTaskType().getCode(),
                        interest == null || interest.getOperations() == null
                                ? java.util.List.of()
                                : interest.getOperations().stream().map(Enum::name).toList(),
                        interest == null ? null : interest.getSource(),
                        interest == null ? null : interest.getCreatedAtMs(),
                        interest == null ? null : interest.getExpiresAtMs(),
                        interest == null ? null : interest.getAbsentAllowedAtMs(),
                        interest == null ? null : interest.isLocalTemplateProbeOnly());
                yield completed("WHOLE_TASK_DIALOG_RUNTIME_READ",
                        new TurnWholeTaskRuntimeResult(null, null, null, null, fact));
            }
            case WHOLE_TASK_COMBAT_ENTRY_CLEANUP -> {
                TaskType taskType = taskType(a.taskCode());
                Optional<WindowPathingIntent> active = runtime.getActivePathingIntent();
                String clearedIntentId = active
                        .filter(intent -> intent.getSource() != null
                                && intent.getSource().startsWith(a.sourcePrefix()))
                        .map(WindowPathingIntent::getIntentId)
                        .orElse(null);
                boolean dialogCleanup = taskType == TaskType.WUBEI;
                if (dialogCleanup) {
                    runtime.clearDialogInterest(a.source());
                    runtime.clearDialogPreparationRequest(a.source());
                    runtime.clearOrdinaryEnterBattleTargetMapGate(a.source());
                }
                boolean pathingCleared = runtime.clearPathingSignalIfSourcePrefix(a.sourcePrefix(), a.source());
                yield completed("WHOLE_TASK_COMBAT_ENTRY_CLEANUP",
                        new TurnWholeTaskRuntimeResult(null, null, null, null, null, null, null, null, null,
                                new TurnCombatCleanupFact(pathingCleared ? clearedIntentId : null,
                                        dialogCleanup, dialogCleanup, dialogCleanup), null));
            }
            case WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE -> {
                // Overwrite the sole local pending transfer-choice memory; the pathing watcher confirms
                // it exactly as in the baseline. Typed field transcription only.
                runtime.updatePendingTransferChoiceMemory(toTransferChoiceMemory(a.transferChoice()));
                yield completedBoolean("WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE", true);
            }
            case WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME -> completed(
                    "WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME",
                    new TurnWholeTaskRuntimeResult(null, null, null, null, null,
                            toWireTransferChoice(runtime.consumePendingTransferChoiceMemoryIfPathingCurrent(
                                    a.intentId(), a.sourcePrefix())), null, null, null, null, null));
            case WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ -> {
                // Read-only typed copy of the sole local pending route outcome; null when absent. The
                // Cloud caller keeps every settlement/consume decision.
                PendingRouteOutcome outcome = runtime.getPendingRouteOutcome();
                yield completed("WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ",
                        new TurnWholeTaskRuntimeResult(null, null, null, null, null, toWireRouteOutcome(outcome)));
            }
            case WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE -> {
                // Request replacement of the sole local pending route outcome with the cloud-issued
                // reason; the runtime owns the atomic replace exactly as in the baseline.
                runtime.requestPendingRouteOutcomeReplacement(
                        toRouteOutcome(a.routeOutcome()), a.routeOutcomeReplacementReason());
                yield completedBoolean("WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE", true);
            }
            case WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME -> completed(
                    "WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME",
                    new TurnWholeTaskRuntimeResult(null, null, null, null, null, null,
                            toWireRouteOutcome(runtime.consumePendingRouteOutcomeIfPathingCurrent(
                                    a.intentId(), a.sourcePrefix())), null, null, null, null));
            case WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME -> completedBoolean(
                    "WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME",
                    npcArrivalFrameFifoLocalExecutor != null
                            && npcArrivalFrameFifoLocalExecutor.execute(a));
            default -> LocalServiceExecution.failed("UNSUPPORTED_WHOLE_TASK_OPERATION", null);
        };
    }

    private static TurnPreBattleFact preBattleFact(WindowRuntimeContext runtime, boolean newlyPublished) {
        TaskType taskType = runtime.getOrdinaryPreBattleTaskType();
        return new TurnPreBattleFact(
                taskType == null ? null : taskType.getCode(),
                runtime.getOrdinaryPreBattleSource(),
                runtime.getOrdinaryPreBattleTargetKeyword(),
                runtime.getOrdinaryPreBattleStartedAtMs(),
                runtime.getOrdinaryPreBattleTimeoutPublishedAtMs(),
                newlyPublished,
                runtime.getOrdinaryEnterBattleTargetMapName(),
                runtime.getOrdinaryEnterBattleTargetMapSource(),
                runtime.getOrdinaryEnterBattleTargetMapGateStartedAtMs(),
                runtime.getOrdinaryEnterBattleTargetMapOpenedAtMs());
    }

    private WindowPathingIntent toPathingIntent(com.bot.dhxy.cloud.turn.protocol.TurnPathingIntent wire) {
        return WindowPathingIntent.builder()
                .source(wire.source())
                .intentId(wire.intentId())
                .targetMapName(wire.targetMapName())
                .targetX(wire.targetX())
                .targetY(wire.targetY())
                .tolerance(wire.tolerance())
                .type(WindowPathingIntentType.valueOf(wire.type()))
                .build();
    }

    private TurnPathingSnapshot toTurnPathingSnapshot(com.bot.dhxy.window.model.WindowPathingSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        WindowPathingIntent intent = snapshot.getIntent();
        TurnPathingIntent wireIntent = intent == null ? null : new TurnPathingIntent(
                intent.getSource(), intent.getIntentId(), intent.getTargetMapName(),
                intent.getTargetX(), intent.getTargetY(), intent.getTolerance(),
                intent.getType() == null ? null : intent.getType().name());
        return new TurnPathingSnapshot(
                snapshot.getState().name(), wireIntent, snapshot.getCurrentMapName(),
                snapshot.getCurrentX(), snapshot.getCurrentY(), snapshot.getLocationChangedAtMs(),
                snapshot.getMovementObservedAtMs(), snapshot.getUpdatedAtMs(),
                snapshot.isDialogBlocking(), snapshot.getDialogBlockingReason(),
                snapshot.getDialogBlockingDetectedAtMs());
    }

    private WindowDialogInterest toDialogInterest(TurnWholeTaskRuntimeArguments a) {
        List<DialogOperation> operations = new ArrayList<>();
        for (String name : a.interestOperations()) {
            operations.add(DialogOperation.valueOf(name));
        }
        return WindowDialogInterest.builder()
                .taskType(taskType(a.taskCode()))
                .operations(operations)
                .source(a.source())
                .absentAllowedAtMs(a.absentAllowedAtMs() == null ? 0L : a.absentAllowedAtMs())
                .localTemplateProbeOnly(a.probeOnly() != null && a.probeOnly())
                .probeStartAtMs(a.probeStartAtMs() == null ? 0L : a.probeStartAtMs())
                .build();
    }

    private PendingTransferChoiceMemory toTransferChoiceMemory(TurnPendingTransferChoice wire) {
        return PendingTransferChoiceMemory.builder()
                .fromMap(wire.fromMap())
                .fromX(wire.fromX())
                .fromY(wire.fromY())
                .targetMap(wire.targetMap())
                .relativeX(wire.relativeX())
                .relativeY(wire.relativeY())
                .optionText(wire.optionText())
                .source(wire.source())
                .createdAtMs(wire.createdAtMs())
                .build();
    }

    private TurnPendingTransferChoice toWireTransferChoice(PendingTransferChoiceMemory memory) {
        if (memory == null) {
            return null;
        }
        return new TurnPendingTransferChoice(memory.getFromMap(), memory.getFromX(), memory.getFromY(),
                memory.getTargetMap(), memory.getRelativeX(), memory.getRelativeY(), memory.getOptionText(),
                memory.getSource(), memory.getCreatedAtMs());
    }

    private PendingRouteOutcome toRouteOutcome(TurnPendingRouteOutcome wire) {
        // routeMode is validated to the sole wire value YELLOW_DESTINATION_MINI_MAP by
        // TurnProtocolValidator before this runs, so valueOf never throws on the bound local enum.
        return PendingRouteOutcome.builder()
                .fromMap(wire.fromMap())
                .targetMap(wire.targetMap())
                .routeMode(WorldMapRouteResultMode.valueOf(wire.routeMode()))
                .relativeX(wire.relativeX())
                .relativeY(wire.relativeY())
                .matchedText(wire.matchedText())
                .source(wire.source())
                .usedMemory(wire.usedMemory())
                .routeDecisionId(wire.routeDecisionId())
                .intentId(wire.intentId())
                .createdAtMs(wire.createdAtMs())
                .build();
    }

    private TurnPendingRouteOutcome toWireRouteOutcome(PendingRouteOutcome outcome) {
        if (outcome == null) {
            return null;
        }
        return new TurnPendingRouteOutcome(
                outcome.getFromMap(),
                outcome.getTargetMap(),
                outcome.getRouteMode() == null ? null : outcome.getRouteMode().name(),
                outcome.getRelativeX(),
                outcome.getRelativeY(),
                outcome.getMatchedText(),
                outcome.getSource(),
                outcome.isUsedMemory(),
                outcome.getRouteDecisionId(),
                outcome.getIntentId(),
                outcome.getCreatedAtMs());
    }

    private static TaskType taskType(String code) {
        for (TaskType type : TaskType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return TaskType.UNKNOWN;
    }

    private LocalServiceExecution completedBoolean(String code, boolean value) {
        return completed(code, new TurnWholeTaskRuntimeResult(value, null, null, null));
    }

    private LocalServiceExecution completedEnum(String code, String enumName) {
        return completed(code, new TurnWholeTaskRuntimeResult(null, enumName, null, null));
    }

    private LocalServiceExecution completed(String code, TurnWholeTaskRuntimeResult result) {
        return LocalServiceExecution.completed(code, json(result), null);
    }

    private String json(TurnWholeTaskRuntimeResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("whole-task runtime result serialization failed", e);
        }
    }
}
