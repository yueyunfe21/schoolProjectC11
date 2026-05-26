package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.runner.context.TaskExecutionContext;
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

    private final GameContext gameContext;
    private final BattleRadarService battleRadarService;
    private final AutoCombatPanelService autoCombatPanelService;
    private final PlayerStateService playerStateService;
    private final UICleanerService uiCleanerService;
    private final BotProperties botProperties;
    private final WindowTaskContextHolder windowTaskContextHolder;

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
        maybeScheduleCombatEntryMaintenance(source);

        if (consumeExitAndRecover(context, source, checkSheYaoXiangForLeaderTask)) {
            return TickResult.EXIT_RECOVERED;
        }

        if (gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            maybeRunCombatMaintenance(context, source);
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

    private void maybeScheduleCombatEntryMaintenance(String source) {
        if (!battleRadarService.consumeCombatEnterSignal()) {
            return;
        }
        AutoCombatRuntimeState state = state();
        long now = System.currentTimeMillis();
        state.pendingCombatEntryMaintenanceAt = now + COMBAT_ENTRY_MAINTENANCE_DELAY_MS;
        state.lastCombatUiCleanAt = now;
        log.info("{} auto-combat enter detected: schedule entry maintenance after {} ms",
                source, COMBAT_ENTRY_MAINTENANCE_DELAY_MS);
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
        playerStateService.performFirstAidCheckNow(context);
        context.throwIfStopRequested();
        if (checkSheYaoXiangForLeaderTask) {
            playerStateService.ensureSheYaoXiangActiveForLeaderTask(source + ":post-combat", context);
        }
        gameContext.setCurrentActionState(GameContext.ActionState.FREE);
        return true;
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
    }
}
