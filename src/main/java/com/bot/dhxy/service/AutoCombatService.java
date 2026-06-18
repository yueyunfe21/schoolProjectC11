package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
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
    private static final long FOLLOWER_FIRST_AID_GATE_WAIT_MS = 3_000L;

    private final GameContext gameContext;
    private final BattleRadarService battleRadarService;
    private final AutoCombatPanelService autoCombatPanelService;
    private final PlayerStateService playerStateService;
    private final UICleanerService uiCleanerService;
    private final TaskMaintenanceService taskMaintenanceService;
    private final BotProperties botProperties;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TaskTurnCoordinator taskTurnCoordinator;

    private final Map<String, AutoCombatRuntimeState> runtimeStates = new ConcurrentHashMap<>();

    public enum TickResult {
        NONE,
        IN_COMBAT,
        EXIT_RECOVERED
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
        context.throwIfStopRequested();
        battleRadarService.checkAndSyncCombatState();
        maybeHandleCombatEnter(source);

        if (consumeExitAndRecover(context, source, checkSheYaoXiangForLeaderTask)) {
            runPendingFollowerFirstAidIfAllowed(context, source);
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
        battleRadarService.checkAndSyncCombatState();
        maybeHandleCombatEnter(source);

        if (gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
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
     * @return true when the current bound follower window already proved it needs focused
     *         HP/MP supply after battle and is waiting for a task-turn slot to run it.
     */
    public boolean hasPendingFollowerFirstAidForCurrentWindow() {
        return state().pendingFollowerFirstAid;
    }

    private void maybeHandleCombatEnter(String source) {
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
                                          boolean checkSheYaoXiangForLeaderTask) {
        if (!battleRadarService.consumeCombatExitSignal()) {
            return false;
        }

        AutoCombatRuntimeState state = state();
        state.pendingCombatEntryMaintenanceAt = 0L;
        autoCombatPanelService.recordCombatExit();
        playerStateService.resetCheckCounter();

        log.info("{} auto-combat exit detected: run unified post-combat recovery", source);
        if (shouldDeferFollowerFirstAid(context)) {
            PlayerStateService.FirstAidNoFocusProbeResult probeResult =
                    playerStateService.probeAndConsumeHealthyFirstAidNoFocus(context, source + ":post-combat");
            if (probeResult == PlayerStateService.FirstAidNoFocusProbeResult.SUPPLY_NEEDED
                    || probeResult == PlayerStateService.FirstAidNoFocusProbeResult.UNKNOWN) {
                state.pendingFollowerFirstAid = true;
                state.pendingFollowerFirstAidSource = source;
                log.info("{} post-combat first-aid queued: follower-support window will wait in task-turn queue task={} requested={} role={} precheck={}",
                        source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), probeResult);
            } else {
                state.pendingFollowerFirstAid = false;
                state.pendingFollowerFirstAidSource = null;
                log.info("{} post-combat first-aid skipped before task-turn queue: task={} requested={} role={} precheck={}",
                        source, safeTaskCode(context), safeRequestedTaskCode(context), safeRole(context), probeResult);
            }
        } else {
            PlayerStateService.FirstAidNoFocusProbeResult probeResult =
                    playerStateService.probeAndConsumeHealthyFirstAidNoFocus(context, source + ":post-combat");
            if (probeResult == PlayerStateService.FirstAidNoFocusProbeResult.SUPPLY_NEEDED
                    && !playerStateService.performCachedFirstAidPlanNow(context)) {
                log.warn("{} post-combat first-aid skipped: no-focus plan unavailable task={} role={}",
                        source, safeTaskCode(context), safeRole(context));
            } else if (probeResult == PlayerStateService.FirstAidNoFocusProbeResult.UNKNOWN) {
                log.warn("{} post-combat first-aid skipped: no-focus probe unknown task={} role={}",
                        source, safeTaskCode(context), safeRole(context));
            }
        }
        context.throwIfStopRequested();
        if (checkSheYaoXiangForLeaderTask) {
            playerStateService.ensureSheYaoXiangActiveForLeaderTask(source + ":post-combat", context);
        }
        gameContext.setCurrentActionState(GameContext.ActionState.FREE);
        return true;
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
        if (("wubei".equalsIgnoreCase(requestedTaskCode) || "xiuluo_v2".equalsIgnoreCase(requestedTaskCode))
                && !taskMaintenanceService.awaitTeamFirstAidMaintenanceWindowOpen(
                context, requestedTaskCode, FOLLOWER_FIRST_AID_GATE_WAIT_MS)) {
            log.info("{} pending follower first-aid deferred: team first-aid gate closed task={} requested={} role={} originalSource={}",
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
                PlayerStateService.FirstAidNoFocusProbeResult retryProbe =
                        playerStateService.probeFirstAidSupplyNoFocus(context);
                if (retryProbe == PlayerStateService.FirstAidNoFocusProbeResult.SUPPLY_NEEDED) {
                    playerStateService.performCachedFirstAidPlanNow(context);
                } else if (retryProbe == PlayerStateService.FirstAidNoFocusProbeResult.UNKNOWN) {
                    log.warn("{} pending follower first-aid still unknown after gate opened; keep pending for next safe window",
                            source);
                    return false;
                }
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

        // First maintenance after entering combat is delayed so battle UI has time to settle.
        if (state.pendingCombatEntryMaintenanceAt > 0 && now >= state.pendingCombatEntryMaintenanceAt) {
            log.info("{} auto-combat entry maintenance: clean generic windows and verify panel",
                    context.getLogPrefix());
            uiCleanerService.closeAllGenericWindows();
            autoCombatPanelService.verifyAndAlignPanel();
            state.pendingCombatEntryMaintenanceAt = 0L;
            state.lastCombatUiCleanAt = System.currentTimeMillis();
            state.lastAutoBattleRefreshAt = state.lastCombatUiCleanAt;
            return;
        }

        // Long combats get occasional generic cleanup, but this must stay sparse to avoid noise.
        if (state.lastCombatUiCleanAt == 0L || now - state.lastCombatUiCleanAt >= COMBAT_UI_CLEAN_INTERVAL_MS) {
            log.info("{} auto-combat maintenance: clean generic windows source={}",
                    context.getLogPrefix(), source);
            uiCleanerService.closeAllGenericWindows();
            state.lastCombatUiCleanAt = System.currentTimeMillis();
        }

        // Auto panel refresh is optional and driven by user configuration.
        long refreshIntervalMs = botProperties.getAutoBattleRefreshIntervalMs();
        if (refreshIntervalMs > 0
                && (state.lastAutoBattleRefreshAt == 0L || now - state.lastAutoBattleRefreshAt >= refreshIntervalMs)) {
            log.info("{} auto-combat maintenance: refresh auto combat panel source={}",
                    context.getLogPrefix(), source);
            autoCombatPanelService.verifyAndAlignPanel();
            state.lastAutoBattleRefreshAt = System.currentTimeMillis();
        }
    }

    private AutoCombatRuntimeState state() {
        String key = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
        return runtimeStates.computeIfAbsent(key, ignored -> new AutoCombatRuntimeState());
    }

    private static class AutoCombatRuntimeState {
        private long lastAutoBattleRefreshAt = 0L;
        private long lastCombatUiCleanAt = 0L;
        private long pendingCombatEntryMaintenanceAt = 0L;
        private boolean pendingFollowerFirstAid = false;
        private String pendingFollowerFirstAidSource;
    }
}
