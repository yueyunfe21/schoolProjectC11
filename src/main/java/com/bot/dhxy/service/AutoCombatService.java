package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.maintenance.TeamSupportCapability;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared auto-combat state machine for task-owned combat segments and member idle loops.
 *
 * <p>This service does not start a standalone task by itself. Callers tick it while they own a
 * window task context. It schedules delayed one-time maintenance after combat entry, periodically
 * cleans harmless generic windows during long combat, and runs the unified post-combat recovery
 * chain after {@link BattleRadarService} emits an exit signal.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoCombatService {
    private static final long COMBAT_ENTRY_MAINTENANCE_DELAY_MS = 4_000L;
    private static final long COMBAT_UI_CLEAN_INTERVAL_MS = 40_000L;
    private static final long REFRESH_DUE_PANEL_VERIFY_GUARD_MS = 30_000L;
    private static final long REFRESH_DUE_DEFERRED_LOG_INTERVAL_MS = 10_000L;
    private static final long URGENT_ROUNDS_PANEL_VERIFY_RETRY_MS = 30_000L;

    private final GameContext gameContext;
    private final BattleRadarService battleRadarService;
    private final AutoCombatPanelService autoCombatPanelService;
    private final PlayerStateService playerStateService;
    private final UICleanerService uiCleanerService;
    private final TaskMaintenanceService taskMaintenanceService;
    private final LeftTopStatusSwitchService leftTopStatusSwitchService;
    private final CommonBoxService commonBoxService;
    private final BotProperties botProperties;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TaskTurnCoordinator taskTurnCoordinator;

    private final Map<String, AutoCombatRuntimeState> runtimeStates = new ConcurrentHashMap<>();
    private final RefreshDuePanelVerifyGate refreshDuePanelVerifyGate = new RefreshDuePanelVerifyGate();

    public enum TickResult {
        NONE,
        IN_COMBAT,
        EXIT_RECOVERED
    }

    /**
     * Recovery mode used after a combat-exit signal is consumed by the task-owned combat tick.
     */
    public enum PostCombatRecoveryPolicy {
        FULL_RECOVERY(false, false),
        FULL_RECOVERY_WITH_LEADER_INCENSE(true, false),
        FAST_EXPECTED_EXIT(false, true);

        private final boolean checkSheYaoXiangForLeaderTask;
        private final boolean deferLeaderRecovery;

        PostCombatRecoveryPolicy(boolean checkSheYaoXiangForLeaderTask, boolean deferLeaderRecovery) {
            this.checkSheYaoXiangForLeaderTask = checkSheYaoXiangForLeaderTask;
            this.deferLeaderRecovery = deferLeaderRecovery;
        }
    }

    /**
     * Initialize per-window combat runtime counters before a task starts ticking auto-combat.
     *
     * <p>The state is keyed by the current {@link WindowTaskContextHolder} binding. No input is sent
     * here; the method only resets timestamps used by later ticks.</p>
     */
    public void initializeForCurrentWindow() {
        AutoCombatRuntimeState state = state();
        long now = System.currentTimeMillis();
        state.lastAutoBattleRefreshAt = now;
        state.lastCombatUiCleanAt = now;
        state.pendingCombatEntryMaintenanceAt = 0L;
        state.pendingFollowerFirstAid = false;
        state.pendingFollowerFirstAidSource = null;
        state.fastExpectedExitWatchArmed = false;
        state.expectedCombatExitWaitArmed = false;
        // CR252 round 4: a fresh task run starts with a clean member coverage history — a window
        // launched independently this run keeps normal bootstrap semantics even if a previous run
        // on the same window was once covered by a local leader.
        state.memberCoveredByLeader = false;
        state.memberReadOnlySelfObserve = false;
        state.lastLeaderCombatPhaseEpochId = 0L;
        // CR252: a fresh enter-battle action (or task/watcher start) means any earlier round's team
        // combat phase owned by this window is stale; members fall back to self radar until the new
        // round's entry is confirmed. No-op for member windows (phases are keyed by leader).
        windowTaskContextHolder.rawCurrent().ifPresent(runtime ->
                taskMaintenanceService.invalidateTeamCombatPhaseForLeader(runtime.getWindowId(),
                        "auto-combat-initialize"));
    }

    /**
     * CR252: a real, successful physical enter-battle action (修罗 看打 click, 五倍 enter-battle
     * dialog / direct-combat success) temporarily authorizes this window's battle-template
     * detection until the round's exit is confirmed. Recognition-only results, interest
     * registration, and navigation clicks must never call this.
     */
    public void authorizeCombatDetectionAfterEnterBattleAction(String source) {
        AutoCombatRuntimeState state = state();
        state.combatDetectionAuthorized = true;
        state.combatDetectionAuthorizedAtMs = System.currentTimeMillis();
        state.combatDetectionAuthoritySource = source;
        log.info("combat detection authorized after enter-battle action: source={}", source);
    }

    /**
     * CR252: end the temporary detection authorization; the next round must re-authorize via a new
     * confirmed enter-battle action.
     */
    public void revokeCombatDetectionAuthority(String reason) {
        AutoCombatRuntimeState state = state();
        if (!state.combatDetectionAuthorized) {
            return;
        }
        state.combatDetectionAuthorized = false;
        state.combatDetectionAuthoritySource = null;
        log.info("combat detection authority revoked: reason={}", reason);
    }

    /**
     * CR252: 修罗/五倍 task-owner windows may run the battle-template radar only inside an
     * authorized combat window, or while the window is already IN_COMBAT (hot start, incidental
     * state carried over, and correction chains must keep tracking the exit). Every other window
     * (五环, unbound members, standalone) keeps its radar unchanged.
     */
    private boolean mayRunBattleRadar(TaskExecutionContext context) {
        if (!requiresEnterBattleAuthorization(context)) {
            return true;
        }
        if (gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            return true;
        }
        return state().combatDetectionAuthorized;
    }

    private static boolean requiresEnterBattleAuthorization(TaskExecutionContext context) {
        String taskCode = context == null ? null : context.getTaskCode();
        return "xiuluo_v2".equalsIgnoreCase(taskCode) || "wubei".equalsIgnoreCase(taskCode);
    }

    /**
     * CR252: the bound local leader's live combat phase, consulted only by member auto-battle
     * windows. Absent for every other window kind so their radar path stays untouched.
     */
    private TaskMaintenanceService.MemberTeamCombatPhaseView memberLeaderCombatPhase(TaskExecutionContext context) {
        if (context == null || !"auto_battle".equalsIgnoreCase(context.getTaskCode())) {
            return TaskMaintenanceService.MemberTeamCombatPhaseView.absent();
        }
        return taskMaintenanceService.memberTeamCombatPhase(context);
    }

    /**
     * CR252 review P1 round 4: whether this member tick must degrade to the pure-read radar path.
     * True for a currently paused bound leader, and for ANY coverage loss (leader stop,
     * runner/session completion, group/binding loss) on a member that was covered at some point in
     * this run — pause, stop, and binding invalidation share one read-only degrade semantic. A
     * window that was never covered this run keeps normal standalone bootstrap semantics.
     */
    private static boolean isMemberReadOnlyDegrade(TaskMaintenanceService.MemberTeamCombatPhaseView leaderPhase,
                                                   AutoCombatRuntimeState state) {
        if (leaderPhase.leaderPaused()) {
            return true;
        }
        boolean everCovered = state.memberReadOnlySelfObserve
                || state.memberCoveredByLeader
                || state.lastLeaderCombatPhaseEpochId > 0L;
        if (!everCovered) {
            return false;
        }
        // Covered again (leader live) but read-only is sticky until the next entry broadcast;
        // a covered member that never degraded keeps the quiet-wait branch instead.
        if (leaderPhase.covered()) {
            return state.memberReadOnlySelfObserve;
        }
        // Coverage fully lost (stop / session completed / binding gone) after being covered.
        return true;
    }

    /**
     * Run one auto-combat tick for the current window.
     *
     * @param context current task execution context; used for stop checks before and after
     *                maintenance actions.
     * @param source short log/source label such as a task code.
     * @param checkSheYaoXiangForLeaderTask true when the leader task should verify sheyaoxiang
     *                                      after combat recovery; member/auto-battle idle loops
     *                                      should normally pass false.
     * @return tick outcome so callers can decide whether to keep waiting, continue task flow, or
     * handle a recovered post-combat state.
     */
    public TickResult handleCombatTick(TaskExecutionContext context,
                                       String source,
                                       boolean checkSheYaoXiangForLeaderTask) {
        return handleCombatTick(context, source,
                legacyPostCombatRecoveryPolicy(checkSheYaoXiangForLeaderTask));
    }

    /**
     * Run one auto-combat tick for the current window with an explicit post-combat recovery policy.
     *
     * @param context current task execution context; used for stop checks before and after
     *                maintenance actions.
     * @param source short log/source label such as a task code.
     * @param recoveryPolicy controls whether a consumed combat-exit signal runs synchronous
     *                       recovery now or records a deferred leader recovery check for a later
     *                       safe point.
     * @return tick outcome so callers can decide whether to keep waiting, continue task flow, or
     * handle a recovered post-combat state.
     */
    public TickResult handleCombatTick(TaskExecutionContext context,
                                       String source,
                                       PostCombatRecoveryPolicy recoveryPolicy) {
        context.throwIfStopRequested();
        PostCombatRecoveryPolicy safePolicy = recoveryPolicy == null
                ? PostCombatRecoveryPolicy.FULL_RECOVERY
                : recoveryPolicy;
        AutoCombatRuntimeState state = state();
        boolean fastExpectedExitPolicy = safePolicy == PostCombatRecoveryPolicy.FAST_EXPECTED_EXIT;
        if (fastExpectedExitPolicy && !state.expectedCombatExitWaitArmed) {
            battleRadarService.armExpectedCombatExitWait(source);
            state.expectedCombatExitWaitArmed = true;
        }
        TaskMaintenanceService.MemberTeamCombatPhaseView leaderPhase = memberLeaderCombatPhase(context);
        if (leaderPhase.present()) {
            // CR252: a member bound to a local leader consumes the leader-confirmed team combat
            // phase instead of its own battle-template radar. The verdict reuses the radar's
            // enter/exit transitions, so panel bootstrap, exit consumption, and recovery are
            // unchanged. Candidate leader quick-exits never reach here (not broadcast).
            state.memberCoveredByLeader = true;
            // A new leader-confirmed entry broadcast is the ONLY thing that ends the
            // pause-degraded read-only mode (CR252 review P1 round 3).
            state.memberReadOnlySelfObserve = false;
            state.lastLeaderCombatPhaseEpochId = leaderPhase.epochId();
            battleRadarService.applyExternalCombatStateVerdict(leaderPhase.inCombat(),
                    source + ":leader-combat-phase:epoch-" + leaderPhase.epochId());
        } else if (isMemberReadOnlyDegrade(leaderPhase, state)) {
            // CR252 review P1 rounds 3+4: EVERY coverage-loss event — leader pause, leader stop,
            // runner/session completion, group/binding loss — degrades a previously bound member
            // to a PURE READ of its own combat state: scan and sync, but never consume the enter
            // signal, schedule entry maintenance, run exit recovery, or send any auto-combat
            // panel input (Alt+8). Only the next real entry broadcast (new epoch) — or a fresh
            // task run that starts independent — restores normal handling.
            if (state.memberCoveredByLeader || state.lastLeaderCombatPhaseEpochId > 0L) {
                log.info("{} member leader-paused-fallback: leader coverage gone (paused={} covered={}); self battle radar is read-only until the next entry broadcast epoch={} source={}",
                        context == null ? "[window=unknown]" : context.getLogPrefix(),
                        leaderPhase.leaderPaused(), leaderPhase.covered(),
                        state.lastLeaderCombatPhaseEpochId, source);
                state.memberCoveredByLeader = false;
                state.lastLeaderCombatPhaseEpochId = 0L;
            }
            state.memberReadOnlySelfObserve = true;
            battleRadarService.checkAndSyncCombatState();
            return gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT
                    ? TickResult.IN_COMBAT
                    : TickResult.NONE;
        } else if (leaderPhase.covered()
                && gameContext.getCurrentActionState() != GameContext.ActionState.IN_COMBAT) {
            // CR252 review P1: bound to a live local leader with no entry broadcast yet — the
            // leader is navigating/moving/waiting tracker. Wait quietly; no template radar. A
            // member already IN_COMBAT skips this branch so its own exit stays trackable.
            state.memberCoveredByLeader = true;
        } else {
            // Reached only by windows that were never covered this run (genuinely independent /
            // external leader / unknown binding -> normal bootstrap semantics) and by covered
            // members already IN_COMBAT from their own incidental battle.
            if (mayRunBattleRadar(context)) {
                boolean fastExpectedExitWait = fastExpectedExitPolicy
                        && gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT;
                if (fastExpectedExitWait) {
                    state.fastExpectedExitWatchArmed = true;
                }
                boolean fullRadarDue = true;
                if (fastExpectedExitWait) {
                    boolean fastExitDetected = battleRadarService.checkFastExpectedCombatExitByAvatarDiff(source);
                    fullRadarDue = !fastExitDetected && battleRadarService.shouldRunFullRadarForFastExpectedExitFallback();
                }
                if (fullRadarDue) {
                    battleRadarService.checkAndSyncCombatState();
                }
            }
        }
        maybeHandleCombatEnter(source);
        battleRadarService.discardStaleCombatExitSignalIfInCombat(source);

        if (consumeExitAndRecover(context, source, recoveryPolicy)) {
            if (runPendingMemberCommonBoxIfAllowed(context, source)) {
                return TickResult.EXIT_RECOVERED;
            }
            runPendingFollowerFirstAidIfAllowed(context, source);
            /*
             * CR242 reopen: consuming the combat-exit signal IS the shared exit-recovered result.
             * Task owners (修罗/五倍 leaders) never run member common-box/first-aid, so demoting
             * this to NONE strands them in WAIT_COMBAT forever. The member same-round return
             * self-check lives in AutoBattleTask's own post-exit branch instead.
             */
            return TickResult.EXIT_RECOVERED;
        }

        if (runPendingMemberCommonBoxIfAllowed(context, source)) {
            return TickResult.EXIT_RECOVERED;
        }

        if (runPendingFollowerFirstAidIfAllowed(context, source)) {
            return TickResult.EXIT_RECOVERED;
        }

        if (gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            maybeRunCombatMaintenance(context, source);
            return TickResult.IN_COMBAT;
        }
        return TickResult.NONE;
    }

    private PostCombatRecoveryPolicy legacyPostCombatRecoveryPolicy(boolean checkSheYaoXiangForLeaderTask) {
        return checkSheYaoXiangForLeaderTask
                ? PostCombatRecoveryPolicy.FULL_RECOVERY_WITH_LEADER_INCENSE
                : PostCombatRecoveryPolicy.FULL_RECOVERY;
    }

    /**
     * Run one window-level combat guard tick without consuming the combat-exit event.
     *
     * <p>This path is used by {@code WindowTaskRunner} while a normal task is busy navigating,
     * reading dialogs, or waiting for movement. It only keeps the per-window combat state fresh.
     * The watcher is allowed to do only the fast, key-only automatic-combat bootstrap after combat
     * entry. Dragging the panel, OCR of remaining rounds, and post-combat recovery deliberately
     * remain with the owning task so one window does not run duplicate heavy work from both the task
     * thread and watcher thread.</p>
     *
     * @param context current bound task execution context for stop checks.
     * @param source short log/source label.
     * @return IN_COMBAT while combat is visible; NONE otherwise. EXIT_RECOVERED is never returned
     *         from this guard path because exit events are not consumed here.
     */
    public TickResult handleWindowCombatGuardTick(TaskExecutionContext context, String source) {
        context.throwIfStopRequested();
        if (!mayRunBattleRadar(context)) {
            // CR252: without a confirmed enter-battle authorization the watcher must not touch the
            // battle-template chain (navigation, movement, tracker waits, dialogs, maintenance and
            // return trips all pass through here). Cheap non-template probes stay with the caller.
            return TickResult.NONE;
        }
        battleRadarService.checkAndSyncCombatState();
        maybeHandleCombatEnter(source);

        if (gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            return TickResult.IN_COMBAT;
        }
        return TickResult.NONE;
    }

    /**
     * Probe the current window combat state without consuming combat-enter/exit signals or sending
     * auto-combat maintenance input.
     *
     * <p>This is intentionally narrower than {@link #handleWindowCombatGuardTick(TaskExecutionContext, String)}.
     * Startup-in-combat deferral must only wait until the current battle ends; it must not open the
     * auto-combat panel with Alt+8 or schedule task-owned post-combat recovery before the task receives
     * its {@code AFTER_COMBAT_EXIT_STARTUP} marker.</p>
     *
     * @param context current task execution context; used only for stop checks.
     * @param source short log/source label for the radar refresh path.
     * @return IN_COMBAT while combat is visible; NONE otherwise.
     */
    public TickResult probeWindowCombatStateReadOnly(TaskExecutionContext context, String source) {
        context.throwIfStopRequested();
        battleRadarService.checkAndSyncCombatState();

        if (gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            return TickResult.IN_COMBAT;
        }
        return TickResult.NONE;
    }

    /**
     * Refresh combat state for a paused leader observer without sending input or consuming signals.
     *
     * <p>This CR141 path is narrower than both normal combat ticks and window guard ticks. It only
     * lets {@link BattleRadarService} update {@link GameContext.ActionState} and leave combat
     * enter/exit signals pending for the task to consume after resume. The method checks stop
     * without consulting the pause token so a stop request still terminates the watcher promptly
     * while user pause remains active.</p>
     *
     * @param context current task execution context; supplies the stop token and diagnostics.
     * @param source short log/source label for the radar refresh path.
     * @return IN_COMBAT while combat is visible; NONE otherwise.
     */
    public TickResult probePausedWindowCombatStateReadOnly(TaskExecutionContext context, String source) {
        TaskCheckpoint.throwIfStopRequested(context == null ? null : context.getStopToken(),
                "paused leader read-only combat probe interrupted");
        GameContext.ActionState beforeState = gameContext.getCurrentActionState();
        if (!mayRunBattleRadar(context)) {
            // CR252: a paused leader outside an authorized combat window has nothing to observe via
            // battle templates; stay read-only and cheap.
            return beforeState == GameContext.ActionState.IN_COMBAT
                    ? TickResult.IN_COMBAT
                    : TickResult.NONE;
        }
        battleRadarService.checkAndSyncCombatState();
        GameContext.ActionState actionState = gameContext.getCurrentActionState();
        if (beforeState == GameContext.ActionState.IN_COMBAT
                && actionState == GameContext.ActionState.FREE) {
            battleRadarService.markCombatExitObservedDuringPause(source);
        }
        log.info("{} paused-leader-readonly-observer combat sync: source={} actionState={} inputAllowed=false preparedActionAllowed=false",
                context == null ? "[window=unknown]" : context.getLogPrefix(), source, actionState);

        if (actionState == GameContext.ActionState.IN_COMBAT) {
            return TickResult.IN_COMBAT;
        }
        return TickResult.NONE;
    }

    /**
     * @return recommended milliseconds until the next auto-combat/radar tick.
     */
    public int getDynamicPollingIntervalMs() {
        return battleRadarService.getDynamicPollingIntervalMs();
    }

    /**
     * Return how long the current bound window may safely stay parked before task-owned combat
     * maintenance should tick again.
     *
     * <p>修罗 leader combat waits are event-driven, but combat-state events only fire on enter/exit
     * edges. This delay makes scheduled in-combat work a real wake source too: the 4s entry cleanup,
     * sparse generic UI cleanup, and configured auto-combat panel refresh must not depend on another
     * combat-state event happening after they become due.</p>
     *
     * @return milliseconds until the next known combat-maintenance deadline for the current window;
     *         {@code -1} only when no deadline exists.
     */
    public long nextCombatMaintenanceDelayMs() {
        AutoCombatRuntimeState state = state();
        long now = System.currentTimeMillis();
        long nextDueAt = Long.MAX_VALUE;

        if (state.pendingCombatEntryMaintenanceAt > 0L) {
            nextDueAt = Math.min(nextDueAt, state.pendingCombatEntryMaintenanceAt);
        }
        if (state.lastCombatUiCleanAt <= 0L) {
            nextDueAt = Math.min(nextDueAt, now);
        } else {
            nextDueAt = Math.min(nextDueAt, state.lastCombatUiCleanAt + COMBAT_UI_CLEAN_INTERVAL_MS);
        }

        long refreshIntervalMs = botProperties.getAutoBattleRefreshIntervalMs();
        if (refreshIntervalMs > 0L) {
            AutoCombatPanelService.RoundsRefreshReason refreshReason =
                    AutoCombatPanelService.resolveRoundsRefreshReason(
                            gameContext.getAutoCombatEstimatedRounds(),
                            gameContext.getLastAutoCombatRefreshAt(),
                            Math.max(0L, refreshIntervalMs),
                            now);
            if (refreshReason == AutoCombatPanelService.RoundsRefreshReason.UNKNOWN
                    || refreshReason == AutoCombatPanelService.RoundsRefreshReason.LOW_ROUNDS
                    || refreshReason == AutoCombatPanelService.RoundsRefreshReason.REFRESH_DUE) {
                nextDueAt = Math.min(nextDueAt, now);
            } else if (state.lastAutoBattleRefreshAt <= 0L) {
                nextDueAt = Math.min(nextDueAt, now);
            } else {
                nextDueAt = Math.min(nextDueAt, state.lastAutoBattleRefreshAt + refreshIntervalMs);
            }
        }

        if (nextDueAt == Long.MAX_VALUE) {
            return -1L;
        }
        return Math.max(0L, nextDueAt - now);
    }

    /**
     * Return how long the current bound window may stay parked before either normal combat
     * maintenance or the lightweight expected-combat exit probe should tick again.
     *
     * <p>The fast exit probe is armed only while a task is waiting with
     * {@link PostCombatRecoveryPolicy#FAST_EXPECTED_EXIT}. It keeps 修罗/五倍 expected combat exits
     * responsive without making the global full battle radar scan every second.</p>
     *
     * @return milliseconds until the next combat wake deadline; {@code -1} when no deadline exists.
     */
    public long nextCombatWakeDelayMs() {
        AutoCombatRuntimeState state = state();
        long nextMaintenanceDelayMs = nextCombatMaintenanceDelayMs();
        long nextFastExitProbeDelayMs = state.fastExpectedExitWatchArmed
                ? battleRadarService.nextFastExpectedCombatExitProbeDelayMs()
                : -1L;
        if (nextMaintenanceDelayMs < 0L) {
            return nextFastExitProbeDelayMs;
        }
        if (nextFastExitProbeDelayMs < 0L) {
            return nextMaintenanceDelayMs;
        }
        return Math.min(nextMaintenanceDelayMs, nextFastExitProbeDelayMs);
    }

    /**
     * @return true when the current bound follower window already proved it needs focused
     *         HP/MP supply after battle and is waiting for a task-turn slot to run it.
     */
    public boolean hasPendingFollowerFirstAidForCurrentWindow() {
        return state().pendingFollowerFirstAid;
    }

    /**
     * @return true when the current bound leader window deferred HP/MP and 摄妖香 recovery after a
     *         fast expected 修罗/五倍 combat exit.
     */
    public boolean hasPendingLeaderPostCombatRecoveryForCurrentWindow() {
        return state().pendingLeaderPostCombatRecovery;
    }

    /**
     * CR243: 修罗 rounds route post-combat first-aid through the team-scoped queue instead of the
     * legacy per-window pending flag / capability polling. Other tasks keep the legacy path.
     */
    private boolean isXiuluoPostCombatFirstAidQueueMode(TaskExecutionContext context) {
        if (context == null) {
            return false;
        }
        return "xiuluo_v2".equalsIgnoreCase(context.getRequestedTaskCode())
                || "xiuluo_v2".equalsIgnoreCase(context.getTaskCode());
    }

    private TaskMaintenanceService.PostCombatFirstAidReport toPostCombatFirstAidReport(
            PlayerStateService.FirstAidNoFocusProbeResult probeResult) {
        if (probeResult == PlayerStateService.FirstAidNoFocusProbeResult.SUPPLY_NEEDED) {
            return TaskMaintenanceService.PostCombatFirstAidReport.SUPPLY_NEEDED;
        }
        if (probeResult == PlayerStateService.FirstAidNoFocusProbeResult.UNKNOWN) {
            return TaskMaintenanceService.PostCombatFirstAidReport.UNKNOWN;
        }
        return TaskMaintenanceService.PostCombatFirstAidReport.HEALTHY;
    }

    /**
     * CR243: run the leader's own queued first-aid attempt when its item is at the FIFO head.
     * The user-approved order puts that item first immediately after the green-link click; the
     * leader then parks while member items continue in the background. The leader already holds
     * the task turn, so no coordinator re-entry happens here. One attempt, unconditional dequeue.
     *
     * @return true when a head attempt was executed (success or not); false when the leader is not
     *         the head or the window is back in combat.
     */
    public boolean consumeQueuedLeaderPostCombatFirstAidIfHead(TaskExecutionContext context, String source) {
        if (!taskMaintenanceService.isPostCombatFirstAidHeadWindow(context)) {
            return false;
        }
        if (gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            return false;
        }
        if (!playerStateService.hasPendingNoFocusFirstAidPlanForCurrentWindow()) {
            // CR243 review P2: no cached plan is a NO_PLAN_TERMINAL dequeue, not a supply attempt.
            taskMaintenanceService.completePostCombatFirstAidAttempt(context, false,
                    source + ":NO_PLAN_TERMINAL");
            log.warn("{} queued leader post-combat first-aid dequeued without supply: reason=NO_PLAN_TERMINAL task={} role={}",
                    source, safeTaskCode(context), safeRole(context));
            return true;
        }
        boolean success = playerStateService.performCachedFirstAidPlanNow(context);
        taskMaintenanceService.completePostCombatFirstAidAttempt(context, success,
                success ? source + ":leader-cached-plan-executed"
                        : source + ":leader-cached-plan-attempt-failed");
        return true;
    }

    /**
     * Record the leader's HP/MP result at the moment a Xiuluo return-home map has been verified.
     * This is intentionally report-only: the existing tracker-green path remains responsible for
     * opening the queue and consuming the leader head, so the bar snapshot is never repeated there.
     *
     * @param context current Xiuluo task context used for the bound HWND capture and queue scope.
     * @param source diagnostic source for the verified return-home branch.
     */
    public void reportXiuluoLeaderFirstAidAfterVerifiedReturn(TaskExecutionContext context, String source) {
        if (!isXiuluoPostCombatFirstAidQueueMode(context)) {
            return;
        }
        PlayerStateService.FirstAidNoFocusProbeResult probeResult =
                playerStateService.probeAndConsumeHealthyFirstAidNoFocus(
                        context, source + ":return-home-first-aid");
        taskMaintenanceService.reportPostCombatFirstAid(context,
                toPostCombatFirstAidReport(probeResult), true, source + ":return-home-first-aid");
    }

    private void maybeHandleCombatEnter(String source) {
        if (gameContext.getCurrentActionState() != GameContext.ActionState.IN_COMBAT) {
            battleRadarService.discardCombatEnterSignalIfNotInCombat(source);
            return;
        }
        if (!battleRadarService.consumeCombatEnterSignal()) {
            return;
        }
        AutoCombatRuntimeState state = state();
        long now = System.currentTimeMillis();
        state.pendingCombatEntryMaintenanceAt = now + COMBAT_ENTRY_MAINTENANCE_DELAY_MS;
        state.lastCombatUiCleanAt = now;
        log.info("{} auto-combat enter detected: schedule entry maintenance after {} ms",
                source, COMBAT_ENTRY_MAINTENANCE_DELAY_MS);
        autoCombatPanelService.ensurePanelVisible(source + ":combat-enter", 500);
    }

    private boolean consumeExitAndRecover(TaskExecutionContext context,
                                          String source,
                                          PostCombatRecoveryPolicy recoveryPolicy) {
        PostCombatRecoveryPolicy safePolicy = recoveryPolicy == null
                ? PostCombatRecoveryPolicy.FULL_RECOVERY
                : recoveryPolicy;
        boolean consumedExit = safePolicy == PostCombatRecoveryPolicy.FAST_EXPECTED_EXIT
                ? battleRadarService.consumeCombatExitSignalForExpectedWait(source)
                : battleRadarService.consumeCombatExitSignal();
        if (!consumedExit) {
            return false;
        }
        AutoCombatRuntimeState state = state();
        state.expectedCombatExitWaitArmed = false;
        state.pendingCombatEntryMaintenanceAt = 0L;
        autoCombatPanelService.recordCombatExit();
        playerStateService.resetCheckCounter();

        log.info("{} auto-combat exit detected: recoveryPolicy={} task={} requested={} role={}",
                source, safePolicy, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context));
        if ("MEMBER".equalsIgnoreCase(safeRole(context))) {
            commonBoxService.detectMemberBoxAfterCombatExit(
                    context, safeRequestedTaskCode(context), source + ":combat-exit");
        }
        if (safePolicy.deferLeaderRecovery) {
            state.pendingFollowerFirstAid = false;
            state.pendingFollowerFirstAidSource = null;
            state.pendingLeaderPostCombatRecovery = true;
            state.pendingLeaderPostCombatRecoverySource = source;
            state.fastExpectedExitWatchArmed = false;
            gameContext.setCurrentActionState(GameContext.ActionState.FREE);
            log.info("{} post-combat recovery deferred for fast expected exit: task={} requested={} role={}",
                    source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context));
            return true;
        }

        if (shouldDeferFollowerFirstAid(context)) {
            PlayerStateService.FirstAidNoFocusProbeResult probeResult =
                    playerStateService.probeAndConsumeHealthyFirstAidNoFocus(context, source + ":post-combat");
            boolean needsSupply = probeResult == PlayerStateService.FirstAidNoFocusProbeResult.SUPPLY_NEEDED
                    || probeResult == PlayerStateService.FirstAidNoFocusProbeResult.UNKNOWN;
            if (isXiuluoPostCombatFirstAidQueueMode(context)) {
                /*
                 * CR243: every exited window must report exactly one of HEALTHY/SUPPLY_NEEDED/
                 * UNKNOWN; UNKNOWN enqueues conservatively even without a cached plan (the single
                 * attempt resolves it). No supply input happens here.
                 */
                taskMaintenanceService.reportPostCombatFirstAid(context,
                        toPostCombatFirstAidReport(probeResult), false, source + ":post-combat");
                state.pendingFollowerFirstAid = needsSupply;
                state.pendingFollowerFirstAidSource = needsSupply ? source : null;
            } else if (needsSupply) {
                if (playerStateService.hasPendingNoFocusFirstAidPlanForCurrentWindow()) {
                    state.pendingFollowerFirstAid = true;
                    state.pendingFollowerFirstAidSource = source;
                    log.info("{} post-combat first-aid queued: follower-support window will wait in task-turn queue task={} requested={} role={} precheck={}",
                            source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), probeResult);
                } else {
                    state.pendingFollowerFirstAid = false;
                    state.pendingFollowerFirstAidSource = null;
                    log.warn("{} post-combat first-aid not queued: no cached plan after precheck task={} requested={} role={} precheck={}",
                            source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), probeResult);
                }
            } else {
                state.pendingFollowerFirstAid = false;
                state.pendingFollowerFirstAidSource = null;
                log.info("{} post-combat first-aid skipped before task-turn queue: task={} requested={} role={} precheck={}",
                        source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), probeResult);
            }
        } else {
            PlayerStateService.FirstAidNoFocusProbeResult probeResult =
                    playerStateService.probeAndConsumeHealthyFirstAidNoFocus(context, source + ":post-combat");
            if (isXiuluoPostCombatFirstAidQueueMode(context)) {
                // CR243: the leader reports too; its supply item is consumed immediately after
                // the later tracker green click, before the member FIFO drains in background.
                taskMaintenanceService.reportPostCombatFirstAid(context,
                        toPostCombatFirstAidReport(probeResult), true, source + ":post-combat-leader");
            } else if ((probeResult == PlayerStateService.FirstAidNoFocusProbeResult.SUPPLY_NEEDED
                    || probeResult == PlayerStateService.FirstAidNoFocusProbeResult.UNKNOWN)
                    && !playerStateService.performCachedFirstAidPlanNow(context)) {
                log.warn("{} post-combat first-aid skipped: cached plan unavailable task={} role={} precheck={}",
                        source, safeTaskCode(context), safeRole(context), probeResult);
            }
        }
        context.throwIfStopRequested();
        if (safePolicy.checkSheYaoXiangForLeaderTask) {
            playerStateService.ensureSheYaoXiangActiveForLeaderTask(source + ":post-combat", context);
        }
        state.fastExpectedExitWatchArmed = false;
        gameContext.setCurrentActionState(GameContext.ActionState.FREE);
        return true;
    }

    /**
     * Refresh the avatar-diff baseline after a trusted read-only probe proves the window is still in
     * the same expected combat.
     *
     * @param source diagnostic source for the underlying radar capture.
     * @return true when a new in-combat avatar baseline was captured.
     */
    public boolean refreshFastExpectedExitBaselineAfterTrustedInCombat(String source) {
        state().fastExpectedExitWatchArmed = true;
        return battleRadarService.refreshFastExpectedCombatExitAvatarBaseline(source);
    }

    /**
     * Reconcile a stale combat action state after the owning task has already verified return-home.
     *
     * <p>This hook is intentionally tiny and synchronous. 修罗/五倍 may use the return item immediately
     * after a fast expected combat exit, while the conservative window watcher can still carry an old
     * {@link GameContext.ActionState#IN_COMBAT} for another radar miss cycle. Once the task has
     * verified the expected start map, that map proof is stronger than the stale watcher state; this
     * method clears only the in-memory combat wait flags and does not capture screenshots, OCR, click,
     * navigate, sleep, or decrement auto-combat round estimates again.</p>
     *
     * @param context current task context for log prefix; may be null only from defensive callers.
     * @param taskCode task that verified the return-home map, such as {@code xiuluo_v2} or
     *                 {@code wubei}.
     * @param verifiedMapName map name already verified by the task-owned position sync.
     * @param source short diagnostic source describing the return-home branch.
     * @return true when a stale {@code IN_COMBAT} action state was cleared.
     */
    public boolean reconcileReturnHomeVerifiedCombatState(TaskExecutionContext context,
                                                          String taskCode,
                                                          String verifiedMapName,
                                                          String source) {
        AutoCombatRuntimeState state = state();
        GameContext.ActionState before = gameContext.getCurrentActionState();
        // CR252: a verified return home is the correction chain's confirmation that this round's
        // battle truly ended. Broadcast the team exit (bound members apply FREE through their
        // normal exit machinery) and end this window's detection authorization; the next round must
        // re-authorize via a new confirmed enter-battle action.
        taskMaintenanceService.confirmTeamCombatPhaseExitedForLeader(context, source + ":return-home-verified");
        revokeCombatDetectionAuthority(source + ":return-home-verified");
        state.expectedCombatExitWaitArmed = false;
        state.fastExpectedExitWatchArmed = false;
        state.pendingCombatEntryMaintenanceAt = 0L;
        if (before != GameContext.ActionState.IN_COMBAT) {
            return false;
        }

        gameContext.setCurrentActionState(GameContext.ActionState.FREE);
        battleRadarService.discardCombatEnterSignalIfNotInCombat(source + ":return-home-verified");
        log.info("{} return-home verification reconciled stale combat state: task={} source={} verifiedMap={} before={} after={}",
                context == null ? "[window=unknown]" : context.getLogPrefix(),
                taskCode, source, verifiedMapName, before, GameContext.ActionState.FREE);
        return true;
    }

    /**
     * Consume the deferred leader HP/MP and 摄妖香 recovery created by
     * {@link PostCombatRecoveryPolicy#FAST_EXPECTED_EXIT}.
     *
     * <p>Expected 修罗/五倍 exits use this to keep the foreground path fast: they return home first,
     * then call this at a known safe point after the return item is verified. The method deliberately
     * reuses the existing first-aid and 摄妖香 mechanisms; it does not introduce new screenshot,
     * template, or click behavior.</p>
     *
     * @param context current task execution context for stop checks and existing recovery services.
     * @param source short diagnostic source for logs.
     * @return true when a pending deferred recovery was consumed; false when nothing was pending or
     *         the current window is still in combat.
     */
    public boolean consumePendingLeaderPostCombatRecoveryIfAllowed(TaskExecutionContext context, String source) {
        AutoCombatRuntimeState state = state();
        if (!state.pendingLeaderPostCombatRecovery) {
            return false;
        }
        if (gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            log.info("{} deferred post-combat recovery kept pending because window is still in combat originalSource={}",
                    source, state.pendingLeaderPostCombatRecoverySource);
            return false;
        }

        String originalSource = state.pendingLeaderPostCombatRecoverySource == null
                ? source
                : state.pendingLeaderPostCombatRecoverySource;
        state.pendingLeaderPostCombatRecovery = false;
        state.pendingLeaderPostCombatRecoverySource = null;
        log.info("{} deferred post-combat recovery consuming after fast expected exit: originalSource={} task={} requested={} role={}",
                source, originalSource, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context));

        if (isXiuluoPostCombatFirstAidQueueMode(context)) {
            /*
             * CR243 review P1-3: in queue mode the leader's first-aid was already reported and
             * drained through the team FIFO before this method runs (post-drain call site). Only
             * the non-queue remainder (摄妖香) executes here; no probe, no supply input for HP/MP.
             */
            log.info("{} deferred post-combat recovery queue-mode remainder: first-aid handled by CR243 FIFO, running 摄妖香 only originalSource={} task={} role={}",
                    source, originalSource, safeTaskCode(context), safeRole(context));
        } else {
            PlayerStateService.FirstAidNoFocusProbeResult probeResult =
                    playerStateService.probeAndConsumeHealthyFirstAidNoFocus(context, source + ":deferred-post-combat");
            if ((probeResult == PlayerStateService.FirstAidNoFocusProbeResult.SUPPLY_NEEDED
                    || probeResult == PlayerStateService.FirstAidNoFocusProbeResult.UNKNOWN)
                    && !playerStateService.performCachedFirstAidPlanNow(context)) {
                log.warn("{} deferred post-combat first-aid skipped: cached plan unavailable originalSource={} task={} role={} precheck={}",
                        source, originalSource, safeTaskCode(context), safeRole(context), probeResult);
            }
        }
        context.throwIfStopRequested();
        playerStateService.ensureSheYaoXiangActiveForLeaderTask(source + ":deferred-post-combat", context);
        return true;
    }

    private boolean runPendingMemberCommonBoxIfAllowed(TaskExecutionContext context, String source) {
        if (gameContext.getCurrentActionState() != GameContext.ActionState.FREE) {
            return false;
        }
        String requestedTaskCode = context == null ? null : context.getRequestedTaskCode();
        if (!commonBoxService.hasPendingBoxForCurrentWindow(context, requestedTaskCode)) {
            return false;
        }
        String transactionName = source + ":pending-member-common-box";
        log.info("{} pending member common-box queued for task turn: task={} requested={} role={}",
                source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context));
        taskTurnCoordinator.enter(transactionName);
        try {
            log.info("{} pending member common-box acquired task turn: task={} requested={} role={}",
                    source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context));
            boolean clicked = commonBoxService.consumePendingBoxIfAllowed(
                    context, requestedTaskCode, source + ":pending-member-common-box");
            if (clicked) {
                log.info("{} pending member common-box consumed before first-aid gate: task={} requested={} role={} firstAidStillPending={}",
                        source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context),
                        state().pendingFollowerFirstAid);
            }
            return clicked;
        } finally {
            taskTurnCoordinator.forceRelease(transactionName);
        }
    }

    private boolean runPendingFollowerFirstAidIfAllowed(TaskExecutionContext context, String source) {
        AutoCombatRuntimeState state = state();
        if (!state.pendingFollowerFirstAid) {
            return false;
        }
        if (gameContext.getCurrentActionState() != GameContext.ActionState.FREE) {
            return false;
        }

        String pendingSource = state.pendingFollowerFirstAidSource == null
                ? source
                : state.pendingFollowerFirstAidSource;
        String requestedTaskCode = context == null ? null : context.getRequestedTaskCode();
        if (isXiuluoPostCombatFirstAidQueueMode(context)) {
            /*
             * CR243: the team FIFO replaces the capability polling for 修罗. Consumption is legal
             * only when the queue is open (leader green link -> PATHING_STARTED) AND this window
             * owns the head. One real attempt, unconditional dequeue, no retry.
             */
            if (!taskMaintenanceService.hasPostCombatFirstAidQueueItem(context)) {
                state.pendingFollowerFirstAid = false;
                state.pendingFollowerFirstAidSource = null;
                log.info("{} pending follower first-aid cleared: queue item gone (drained or window cleanup) task={} requested={} role={} originalSource={}",
                        source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
                return false;
            }
            if (!taskMaintenanceService.isPostCombatFirstAidHeadWindow(context)) {
                log.info("{} pending follower first-aid deferred: post-combat queue closed or not head task={} requested={} role={} originalSource={}",
                        source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
                return false;
            }
            String queueTransactionName = source + ":post-combat-first-aid-head";
            log.info("{} pending follower first-aid queue head entering task turn: task={} requested={} role={} originalSource={}",
                    source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
            taskTurnCoordinator.enter(queueTransactionName);
            try {
                if (!playerStateService.hasPendingNoFocusFirstAidPlanForCurrentWindow()) {
                    // CR243 review P2: no cached plan is a NO_PLAN_TERMINAL dequeue, not a supply
                    // attempt. The item leaves the FIFO so the team is not blocked; the log states
                    // truthfully that no supply input happened.
                    taskMaintenanceService.completePostCombatFirstAidAttempt(context, false,
                            pendingSource + ":NO_PLAN_TERMINAL");
                    log.warn("{} pending follower first-aid dequeued without supply: reason=NO_PLAN_TERMINAL task={} requested={} role={} originalSource={}",
                            source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
                    state.pendingFollowerFirstAid = false;
                    state.pendingFollowerFirstAidSource = null;
                    return false;
                }
                boolean success = playerStateService.performCachedFirstAidPlanNow(context);
                taskMaintenanceService.completePostCombatFirstAidAttempt(context, success,
                        success ? pendingSource + ":cached-plan-executed"
                                : pendingSource + ":cached-plan-attempt-failed");
                state.pendingFollowerFirstAid = false;
                state.pendingFollowerFirstAidSource = null;
                return success;
            } finally {
                taskTurnCoordinator.forceRelease(queueTransactionName);
            }
        }
        if (taskMaintenanceService.isLocalSupportMemberSession(context)) {
            if (!taskMaintenanceService.isLocalTeamSupportCapabilityOpen(context, TeamSupportCapability.FIRST_AID)) {
                log.info("{} pending follower first-aid deferred immediately: gate=local-team capability=FIRST_AID session={} leaderWindow={} task={} requested={} role={} originalSource={}",
                        source, context.getLocalTeamSessionKey(), context.getLocalLeaderWindowId(),
                        safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
                return false;
            }
            log.info("{} pending follower first-aid gate=local-team capability=FIRST_AID opened: session={} leaderWindow={} task={} requested={} role={} originalSource={}",
                    source, context.getLocalTeamSessionKey(), context.getLocalLeaderWindowId(),
                    safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
        } else if (taskMaintenanceService.isPendingLocalSupportLeaderDetection(context)) {
            log.info("{} pending follower first-aid deferred: pending local leader detection session={} task={} requested={} role={} originalSource={}",
                    source, context.getLocalTeamSessionKey(),
                    safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
            return false;
        } else if (context != null
                && !taskMaintenanceService.isLocalSupportMemberCandidate(context)
                && context.isLocalLeaderPresent()
                && ("wubei".equalsIgnoreCase(requestedTaskCode) || "xiuluo_v2".equalsIgnoreCase(requestedTaskCode))
                && !taskMaintenanceService.isTeamFirstAidMaintenanceWindowOpen(context, requestedTaskCode)) {
            log.info("{} pending follower first-aid deferred immediately: team first-aid gate closed task={} requested={} role={} originalSource={}",
                    source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
            return false;
        }
        String transactionName = source + ":pending-follower-first-aid";
        /*
         * This is intentionally blocking. The old tryRun path made follower supply opportunistic:
         * if the leader still owned the turn, the member merely slept and could miss the next
         * release. By entering the fair task-turn queue, the already-probed first-aid request waits
         * as a real queued maintenance action and runs as soon as the coordinator hands off.
         */
        log.info("{} pending follower first-aid queued for task turn: task={} requested={} role={} originalSource={}",
                source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
        taskTurnCoordinator.enter(transactionName);
        try {
            log.info("{} pending follower first-aid acquired task turn: task={} requested={} role={} originalSource={}",
                    source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
            if (!playerStateService.performCachedFirstAidPlanNow(context)) {
                state.pendingFollowerFirstAid = false;
                state.pendingFollowerFirstAidSource = null;
                log.warn("{} pending follower first-aid cleared: cached plan unavailable after gate opened task={} requested={} role={} originalSource={}",
                        source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), pendingSource);
                return false;
            }
            state.pendingFollowerFirstAid = false;
            state.pendingFollowerFirstAidSource = null;
            return true;
        } finally {
            taskTurnCoordinator.forceRelease(transactionName);
        }
    }

    private boolean shouldDeferFollowerFirstAid(TaskExecutionContext context) {
        if (context == null) {
            return false;
        }
        String taskCode = context.getTaskCode();
        String requestedTaskCode = context.getRequestedTaskCode();
        if (!"auto_battle".equalsIgnoreCase(taskCode)) {
            return false;
        }
        if (!"MEMBER".equalsIgnoreCase(context.getWindowRole())) {
            return false;
        }
        return requestedTaskCode != null
                && !requestedTaskCode.isBlank()
                && !requestedTaskCode.equalsIgnoreCase(taskCode);
    }

    private String safeTaskCode(TaskExecutionContext context) {
        return context == null ? "-" : context.getTaskCode();
    }

    private String safeRequestedTaskCode(TaskExecutionContext context) {
        return context == null ? "-" : context.getRequestedTaskCode();
    }

    private String safeRole(TaskExecutionContext context) {
        return context == null ? "-" : context.getWindowRole();
    }

    private void maybeRunCombatMaintenance(TaskExecutionContext context, String source) {
        context.throwIfStopRequested();
        AutoCombatRuntimeState state = state();
        long now = System.currentTimeMillis();

        // Compute optional refresh pressure before entry maintenance so an allowed refresh-due
        // check does not pay a verify-only panel scan and an immediate verify-and-refresh scan.
        long refreshIntervalMs = botProperties.getAutoBattleRefreshIntervalMs();
        AutoCombatPanelService.RoundsRefreshReason refreshReason = null;
        RefreshDuePanelVerifyDecision refreshDueDecision = null;
        String windowId = currentWindowId();
        if (refreshIntervalMs > 0L) {
            refreshReason = AutoCombatPanelService.resolveRoundsRefreshReason(
                    gameContext.getAutoCombatEstimatedRounds(),
                    gameContext.getLastAutoCombatRefreshAt(),
                    Math.max(0L, refreshIntervalMs),
                    now);
            if (refreshReason == AutoCombatPanelService.RoundsRefreshReason.REFRESH_DUE) {
                state.lastRefreshDuePanelVerifyAttemptAt = now;
                refreshDueDecision = refreshDuePanelVerifyGate.reserveIfAllowed(
                        safeRequestedTaskCode(context), windowId, now);
            }
        }

        // First maintenance after entering combat is delayed so battle UI has time to settle.
        if (state.pendingCombatEntryMaintenanceAt > 0 && now >= state.pendingCombatEntryMaintenanceAt) {
            log.info("{} auto-combat entry maintenance: clean combat UI and verify panel",
                    context.getLogPrefix());
            cleanCombatUiForRole(context, "entry-maintenance");
            if (refreshReason == AutoCombatPanelService.RoundsRefreshReason.REFRESH_DUE
                    && refreshDueDecision != null
                    && !refreshDueDecision.deferred()) {
                log.info("{} auto-combat entry maintenance: merge panel verify into refresh-due check reason={} windowId={}",
                        context.getLogPrefix(), refreshReason, windowId);
            } else {
                autoCombatPanelService.verifyAndAlignPanel(AutoCombatPanelService.PanelVerifyMode.ENTRY_MAINTENANCE);
            }
            state.pendingCombatEntryMaintenanceAt = 0L;
            state.lastCombatUiCleanAt = System.currentTimeMillis();
        }

        // Long combats get occasional generic cleanup, but this must stay sparse to avoid noise.
        if (state.lastCombatUiCleanAt == 0L || now - state.lastCombatUiCleanAt >= COMBAT_UI_CLEAN_INTERVAL_MS) {
            log.info("{} auto-combat maintenance: clean combat UI source={}",
                    context.getLogPrefix(), source);
            cleanCombatUiForRole(context, source);
            if (taskMaintenanceService.isLocalSupportMemberSession(context)) {
                if (taskMaintenanceService.isLocalTeamSupportCapabilityOpen(
                        context, TeamSupportCapability.LEFT_TOP_STATUS)) {
                    leftTopStatusSwitchService.handleCombatMaintenance(context, source);
                } else {
                    log.info("{} local support combat left-top deferred: capability=LEFT_TOP_STATUS closed session={} leaderWindow={} task={} requested={} role={} source={}",
                            context.getLogPrefix(), context.getLocalTeamSessionKey(), context.getLocalLeaderWindowId(),
                            safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), source);
                }
            } else if (taskMaintenanceService.isPendingLocalSupportLeaderDetection(context)) {
                log.info("{} local support combat left-top deferred: pending local leader detection session={} task={} requested={} role={} source={}",
                        context.getLogPrefix(), context.getLocalTeamSessionKey(),
                        safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), source);
            } else {
                leftTopStatusSwitchService.handleCombatMaintenance(context, source);
            }
            state.lastCombatUiCleanAt = System.currentTimeMillis();
        }

        // Auto panel refresh is optional and driven by user configuration.
        if (refreshIntervalMs <= 0L) {
            return;
        }

        if (refreshReason == null) {
            return;
        }

        if (refreshReason == AutoCombatPanelService.RoundsRefreshReason.REFRESH_DUE) {
            RefreshDuePanelVerifyDecision decision = refreshDueDecision == null
                    ? refreshDuePanelVerifyGate.reserveIfAllowed(safeRequestedTaskCode(context), windowId, now)
                    : refreshDueDecision;
            if (decision.deferred()) {
                logRefreshDueDeferred(context, state, windowId, decision, now);
                return;
            }
        } else if (state.lastUrgentRoundsPanelVerifyAttemptAt > 0L
                && now - state.lastUrgentRoundsPanelVerifyAttemptAt < URGENT_ROUNDS_PANEL_VERIFY_RETRY_MS) {
            log.info("{} auto-combat maintenance: urgent rounds panel verify skipped by per-window retry guard source={} reason={} ageMs={} retryMs={}",
                    context.getLogPrefix(), source, refreshReason,
                    now - state.lastUrgentRoundsPanelVerifyAttemptAt, URGENT_ROUNDS_PANEL_VERIFY_RETRY_MS);
            return;
        } else {
            state.lastUrgentRoundsPanelVerifyAttemptAt = now;
        }

        log.info("{} auto-combat maintenance: refresh auto combat panel source={} reason={}",
                context.getLogPrefix(), source, refreshReason);
        long beforeActualRoundReadRefreshAt = gameContext.getLastAutoCombatRefreshAt();
        boolean refreshed = autoCombatPanelService.verifyAndAlignPanel(
                AutoCombatPanelService.PanelVerifyMode.VERIFY_AND_REFRESH);
        if (refreshed || gameContext.getLastAutoCombatRefreshAt() != beforeActualRoundReadRefreshAt) {
            state.lastAutoBattleRefreshAt = System.currentTimeMillis();
        }
    }

    private void cleanCombatUiForRole(TaskExecutionContext context, String source) {
        if ("LEADER".equals(context.getWindowRole())) {
            log.info("{} auto-combat maintenance: leader full UI cleanup source={}",
                    context.getLogPrefix(), source);
            uiCleanerService.cleanUpAll();
            return;
        }
        uiCleanerService.closeAllGenericWindows();
    }

    private void logRefreshDueDeferred(TaskExecutionContext context,
                                       AutoCombatRuntimeState state,
                                       String windowId,
                                       RefreshDuePanelVerifyDecision decision,
                                       long now) {
        if (state.lastRefreshDuePanelVerifyDeferredLogAt <= 0L
                || now - state.lastRefreshDuePanelVerifyDeferredLogAt >= REFRESH_DUE_DEFERRED_LOG_INTERVAL_MS) {
            state.lastRefreshDuePanelVerifyDeferredLogAt = now;
            log.info("{} refresh-due panel verify deferred by team gate: task={} windowId={} retryAfterMs={} lastTeamRefreshAgeMs={}",
                    context.getLogPrefix(), safeRequestedTaskCode(context), windowId,
                    decision.retryAfterMs(), decision.lastTeamRefreshAgeMs());
        } else {
            log.debug("{} refresh-due panel verify deferred suppressed by log throttle: task={} windowId={} retryAfterMs={} lastTeamRefreshAgeMs={}",
                    context.getLogPrefix(), safeRequestedTaskCode(context), windowId,
                    decision.retryAfterMs(), decision.lastTeamRefreshAgeMs());
        }
    }

    private AutoCombatRuntimeState state() {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        String windowId = current
                .map(WindowRuntimeContext::getWindowId)
                .filter(value -> value != null && !value.isBlank())
                .orElse("default");
        long epoch = currentPlayerIdentityEpoch();
        AutoCombatRuntimeState existing = runtimeStates.computeIfAbsent(windowId, ignored -> {
            AutoCombatRuntimeState created = new AutoCombatRuntimeState();
            created.playerIdentityEpoch = epoch;
            return created;
        });
        if (existing.playerIdentityEpoch != epoch) {
            log.info("auto-combat runtime state invalidated by player identity drift: windowId={} oldEpoch={} newEpoch={} pendingEntryAt={} pendingFirstAid={} pendingLeaderRecovery={}",
                    windowId, existing.playerIdentityEpoch, epoch,
                    existing.pendingCombatEntryMaintenanceAt, existing.pendingFollowerFirstAid,
                    existing.pendingLeaderPostCombatRecovery);
            AutoCombatRuntimeState reset = new AutoCombatRuntimeState();
            reset.playerIdentityEpoch = epoch;
            runtimeStates.put(windowId, reset);
            return reset;
        }
        return existing;
    }

    private long currentPlayerIdentityEpoch() {
        return windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getPlayerIdentityEpoch)
                .orElse(0L);
    }

    private String currentWindowId() {
        return windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getWindowId)
                .filter(value -> value != null && !value.isBlank())
                .orElse("default");
    }

    private static class AutoCombatRuntimeState {
        private long playerIdentityEpoch;
        private long lastAutoBattleRefreshAt = 0L;
        private long lastCombatUiCleanAt = 0L;
        private long pendingCombatEntryMaintenanceAt = 0L;
        private long lastRefreshDuePanelVerifyAttemptAt = 0L;
        private long lastRefreshDuePanelVerifyDeferredLogAt = 0L;
        private long lastUrgentRoundsPanelVerifyAttemptAt = 0L;
        private boolean pendingFollowerFirstAid = false;
        private String pendingFollowerFirstAidSource;
        private boolean pendingLeaderPostCombatRecovery = false;
        private String pendingLeaderPostCombatRecoverySource;
        private boolean fastExpectedExitWatchArmed = false;
        private boolean expectedCombatExitWaitArmed = false;
        // CR252: temporary battle-template detection authorization for 修罗/五倍 task owners.
        private volatile boolean combatDetectionAuthorized = false;
        private volatile long combatDetectionAuthorizedAtMs = 0L;
        private volatile String combatDetectionAuthoritySource;
        // CR252: last leader combat-phase epoch this member consumed; used only to log the
        // one-time fallback to self radar when the broadcast disappears.
        private long lastLeaderCombatPhaseEpochId = 0L;
        // CR252 review P1: true while this member is covered by a live local leader (with or
        // without an entry broadcast); drives the one-time leader-paused-fallback log.
        private boolean memberCoveredByLeader = false;
        // CR252 review P1 round 3: sticky read-only self-observe mode after a leader-pause
        // fallback. Stays true through leader resume until the next real entry broadcast; while
        // true the member never consumes enter signals or drives auto-combat panel input.
        private boolean memberReadOnlySelfObserve = false;
    }

    public record RefreshDuePanelVerifyDecision(boolean deferred, long retryAfterMs, long lastTeamRefreshAgeMs) {
        private static RefreshDuePanelVerifyDecision allowed() {
            return new RefreshDuePanelVerifyDecision(false, 0L, -1L);
        }

        private static RefreshDuePanelVerifyDecision deferred(long retryAfterMs, long lastTeamRefreshAgeMs) {
            return new RefreshDuePanelVerifyDecision(true, retryAfterMs, lastTeamRefreshAgeMs);
        }
    }

    public static class RefreshDuePanelVerifyGate {
        private final Map<String, Long> lastVerifyByTeam = new ConcurrentHashMap<>();

        public RefreshDuePanelVerifyDecision reserveIfAllowed(String teamKey, String windowId, long now) {
            String safeTeamKey = teamKey == null || teamKey.isBlank() ? windowId : teamKey;
            String key = safeTeamKey == null || safeTeamKey.isBlank() ? "default" : safeTeamKey;
            Long lastAt = lastVerifyByTeam.get(key);
            if (lastAt != null) {
                long age = now - lastAt;
                if (age >= 0L && age < REFRESH_DUE_PANEL_VERIFY_GUARD_MS) {
                    return RefreshDuePanelVerifyDecision.deferred(
                            REFRESH_DUE_PANEL_VERIFY_GUARD_MS - age, age);
                }
            }
            lastVerifyByTeam.put(key, now);
            return RefreshDuePanelVerifyDecision.allowed();
        }
    }
}
