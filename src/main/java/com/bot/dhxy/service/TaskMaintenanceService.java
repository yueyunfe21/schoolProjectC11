package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupRequest;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupResult;
import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;
import com.bot.dhxy.model.maintenance.TeamMaintenanceWindowState;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.model.maintenance.TaskMaintenanceResult;
import com.bot.dhxy.model.maintenance.TaskMaintenanceStatus;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin scheduler for task maintenance that is shared by auto-battle and formal task flows.
 *
 * <p>This service owns priority, cooldown, and logging only. It deliberately delegates concrete UI
 * work to the existing domain services: broadcast option dialogs go through {@link DialogService},
 * and summon-skill cleanup goes through {@link SummonSkillService}. Callers still decide when a
 * maintenance pass is safe for their task turn.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskMaintenanceService {

    private static final String DEFAULT_WINDOW_KEY = "default";
    private static final long SUMMON_SKILL_NOT_DUE_LOG_INTERVAL_MS = 60_000L;
    private static final long SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS = 2 * 60 * 60 * 1000L;

    private final BotProperties botProperties;
    private final GameContext gameContext;
    private final DialogService dialogService;
    private final SummonSkillService summonSkillService;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final Object teamMaintenanceWindowMonitor = new Object();
    private final Map<String, Long> lastSummonSkillCleanAtByWindow = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSummonSkillNotDueLogAtByWindow = new ConcurrentHashMap<>();
    private final Map<String, SummonSkillWindowState> summonSkillStateByWindow = new ConcurrentHashMap<>();
    private final Map<String, Integer> activeTeamRoundByKey = new ConcurrentHashMap<>();
    private final Map<String, TeamMaintenanceWindowState> teamMaintenanceWindowStateByRound = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> summonSkillClaimsByTeamRound = new ConcurrentHashMap<>();

    /**
     * Reset summon-skill cooldown for a newly started task window.
     *
     * @param context current task execution context; null falls back to the bound window context.
     * @param sourceTask diagnostic source written to logs.
     */
    public void initializeForTaskStart(TaskExecutionContext context, String sourceTask) {
        String windowKey = currentWindowKey(context);
        if (botProperties.isSummonSkillCleanRunImmediatelyOnStart()) {
            lastSummonSkillCleanAtByWindow.remove(windowKey);
            log.info("{} maintenance init: summon skill can run immediately source={}",
                    logPrefix(context), sourceTask);
            return;
        }
        lastSummonSkillCleanAtByWindow.put(windowKey, System.currentTimeMillis());
        log.info("{} maintenance init: summon skill cooldown starts now source={}",
                logPrefix(context), sourceTask);
    }

    /**
     * Register the current formal task round so follower windows can share the same maintenance slot.
     *
     * @param context current leader task context.
     * @param teamMaintenanceKey stable team task key, usually the formal task code.
     * @param round one-based round number.
     * @param sourceTask diagnostic source written to logs.
     */
    public void beginTeamMaintenanceRound(TaskExecutionContext context,
                                          String teamMaintenanceKey,
                                          int round,
                                          String sourceTask) {
        String teamKey = normalizeTeamKey(teamMaintenanceKey, context);
        activeTeamRoundByKey.put(teamKey, round);
        pruneOlderTeamRoundClaims(teamKey, round);
        teamMaintenanceWindowStateByRound.put(teamRoundKey(teamKey, round), TeamMaintenanceWindowState.CLOSED);
        log.info("{} maintenance team round active: teamKey={} round={} source={}",
                logPrefix(context), teamKey, round, sourceTask);
    }

    /**
     * Open the short shared maintenance window after the leader has submitted real pathing.
     *
     * @param context current leader task context.
     * @param teamMaintenanceKey stable task/team key.
     * @param round one-based task round.
     * @param sourceTask diagnostic source written to logs.
     */
    public void openTeamPathingMaintenanceWindow(TaskExecutionContext context,
                                                String teamMaintenanceKey,
                                                int round,
                                                String sourceTask) {
        String teamKey = normalizeTeamKey(teamMaintenanceKey, context);
        activeTeamRoundByKey.put(teamKey, round);
        String roundKey = teamRoundKey(teamKey, round);
        TeamMaintenanceWindowState previous = teamMaintenanceWindowStateByRound.put(
                roundKey, TeamMaintenanceWindowState.PATHING_WINDOW_OPEN);
        synchronized (teamMaintenanceWindowMonitor) {
            teamMaintenanceWindowMonitor.notifyAll();
        }
        log.info("{} maintenance team pathing window opened: teamRound={} previous={} source={}",
                logPrefix(context), roundKey, previous, sourceTask);
    }

    /**
     * Open a short follower HP/MP recovery-only window.
     *
     * <p>This is intentionally weaker than {@link #openTeamPathingMaintenanceWindow(TaskExecutionContext,
     * String, int, String)}: follower first-aid may use it, but summon-skill cleanup still requires
     * a real leader pathing window. 黄袍怪连战 uses this gate between consecutive fights.</p>
     *
     * @param context current leader task context.
     * @param teamMaintenanceKey stable task/team key.
     * @param round one-based task round.
     * @param sourceTask diagnostic source written to logs.
     */
    public void openTeamFirstAidMaintenanceWindow(TaskExecutionContext context,
                                                 String teamMaintenanceKey,
                                                 int round,
                                                 String sourceTask) {
        String teamKey = normalizeTeamKey(teamMaintenanceKey, context);
        activeTeamRoundByKey.put(teamKey, round);
        String roundKey = teamRoundKey(teamKey, round);
        TeamMaintenanceWindowState previous = teamMaintenanceWindowStateByRound.put(
                roundKey, TeamMaintenanceWindowState.FIRST_AID_WINDOW_OPEN);
        synchronized (teamMaintenanceWindowMonitor) {
            teamMaintenanceWindowMonitor.notifyAll();
        }
        log.info("{} maintenance team first-aid window opened: teamRound={} previous={} source={}",
                logPrefix(context), roundKey, previous, sourceTask);
    }

    /**
     * Close the shared maintenance window once the leader reaches the target area or leaves pathing.
     *
     * @param context current leader task context.
     * @param teamMaintenanceKey stable task/team key.
     * @param round one-based task round.
     * @param sourceTask diagnostic source written to logs.
     */
    public void closeTeamMaintenanceWindow(TaskExecutionContext context,
                                           String teamMaintenanceKey,
                                           int round,
                                           String sourceTask) {
        String teamKey = normalizeTeamKey(teamMaintenanceKey, context);
        String roundKey = teamRoundKey(teamKey, round);
        TeamMaintenanceWindowState previous = teamMaintenanceWindowStateByRound.put(
                roundKey, TeamMaintenanceWindowState.CLOSED);
        if (previous != null && previous != TeamMaintenanceWindowState.CLOSED) {
            log.info("{} maintenance team window closed: teamRound={} previous={} source={}",
                    logPrefix(context), roundKey, previous, sourceTask);
        }
    }

    /**
     * Check whether the active team round has already reached the leader-pathing maintenance window.
     *
     * @param context current task context. Used only to resolve a default team key when
     *                {@code teamMaintenanceKey} is blank.
     * @param teamMaintenanceKey formal task key such as {@code wubei} or {@code xiuluo_v2}.
     * @return true only after the leader opened the pathing maintenance window for the active round.
     */
    public boolean isTeamPathingMaintenanceWindowOpen(TaskExecutionContext context,
                                                      String teamMaintenanceKey) {
        String teamKey = normalizeTeamKey(teamMaintenanceKey, context);
        Integer round = activeTeamRoundByKey.get(teamKey);
        if (round == null) {
            return false;
        }
        return teamMaintenanceWindowStateByRound.get(teamRoundKey(teamKey, round))
                == TeamMaintenanceWindowState.PATHING_WINDOW_OPEN;
    }

    /**
     * Wait without taking the task turn until the leader opens a follower first-aid-safe window.
     *
     * <p>This is used by follower HP/MP recovery. A follower may already have a no-focus recovery
     * plan, but it must not focus itself while the leader is returning, accepting, or reading the
     * next task. Real pathing windows and explicit first-aid windows both wake these waiters; longer
     * summon-skill cleanup still checks the stricter {@link TeamMaintenanceWindowState#PATHING_WINDOW_OPEN}
     * state directly.</p>
     *
     * @param context current follower context, used for stop checks and default team-key resolution.
     * @param teamMaintenanceKey formal task key that owns the current team round.
     * @param timeoutMs maximum time to wait in this call; callers may keep the pending plan and retry
     *                  later if this returns false.
     * @return true when either a real pathing window or a first-aid-only window is open.
     */
    public boolean awaitTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext context,
                                                          String teamMaintenanceKey,
                                                          long timeoutMs) {
        if (isTeamFirstAidWindowOpen(context, teamMaintenanceKey)) {
            return true;
        }
        if (timeoutMs <= 0L) {
            return false;
        }

        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (teamMaintenanceWindowMonitor) {
            while (!isTeamFirstAidWindowOpen(context, teamMaintenanceKey)) {
                checkpoint(context);
                long remainingMs = deadline - System.currentTimeMillis();
                if (remainingMs <= 0L) {
                    return false;
                }
                try {
                    teamMaintenanceWindowMonitor.wait(remainingMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isTeamFirstAidWindowOpen(TaskExecutionContext context, String teamMaintenanceKey) {
        String teamKey = normalizeTeamKey(teamMaintenanceKey, context);
        Integer round = activeTeamRoundByKey.get(teamKey);
        if (round == null) {
            return false;
        }
        TeamMaintenanceWindowState state = teamMaintenanceWindowStateByRound.get(teamRoundKey(teamKey, round));
        return state == TeamMaintenanceWindowState.PATHING_WINDOW_OPEN
                || state == TeamMaintenanceWindowState.FIRST_AID_WINDOW_OPEN;
    }

    /**
     * Run one maintenance pass in the requested priority order.
     *
     * @param context current task execution context. Used for stop/pause checkpoints and per-window
     *                cooldown identity; may be null for legacy callers.
     * @param request describes which maintenance capabilities are allowed at this task point.
     * @return structured result. Non-success statuses are defer/retry hints, not task failures.
     */
    public TaskMaintenanceResult runOpportunisticMaintenance(TaskExecutionContext context,
                                                             TaskMaintenanceRequest request) {
        TaskMaintenanceRequest safeRequest = normalize(request);
        checkpoint(context);

        if (safeRequest.isHandleMaintenanceBroadcast()) {
            TaskMaintenanceResult broadcastResult = handleMaintenanceBroadcast(context, safeRequest.getSourceTask());
            if (broadcastResult.isHandled()
                    || broadcastResult.getStatus() == TaskMaintenanceStatus.BROADCAST_FAILED
                    || broadcastResult.getStatus() == TaskMaintenanceStatus.INTERRUPTED) {
                return broadcastResult;
            }
        }

        if (safeRequest.isCleanSummonSkill()) {
            return maybeCleanSummonSkill(context, safeRequest);
        }

        return TaskMaintenanceResult.noAction("no maintenance action");
    }

    private TaskMaintenanceResult handleMaintenanceBroadcast(TaskExecutionContext context, String sourceTask) {
        DialogResult dialogResult = dialogService.handleDialog(
                DialogHandleRequest.handleMaintenanceBroadcastOption(sourceTask));
        DialogResultStatus status = dialogResult.getStatus();
        if (status == DialogResultStatus.BUSINESS_OPTION_CLICKED) {
            log.info("{} maintenance broadcast handled: source={} actionKey={}",
                    logPrefix(context), sourceTask, dialogResult.getActionKey());
            return TaskMaintenanceResult.broadcastHandled("maintenance broadcast handled");
        }
        if (status == DialogResultStatus.INTERRUPTED) {
            log.info("{} maintenance broadcast interrupted: source={}", logPrefix(context), sourceTask);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.INTERRUPTED,
                    "maintenance broadcast interrupted");
        }
        if (status == DialogResultStatus.FAILED) {
            log.warn("{} maintenance broadcast scan failed: source={}", logPrefix(context), sourceTask);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.BROADCAST_FAILED,
                    "maintenance broadcast scan failed");
        }
        return TaskMaintenanceResult.noAction("no maintenance broadcast");
    }

    private TaskMaintenanceResult maybeCleanSummonSkill(TaskExecutionContext context,
                                                        TaskMaintenanceRequest request) {
        String windowKey = currentWindowKey(context);
        if (!botProperties.isSummonSkillCleanEnabled()) {
            log.info("{} maintenance: summon skill disabled by config source={} windowKey={}",
                    logPrefix(context), request.getSourceTask(), windowKey);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DISABLED,
                    "summon skill maintenance disabled");
        }
        long intervalMs = botProperties.getSummonSkillCleanIntervalMs();
        if (intervalMs <= 0) {
            log.info("{} maintenance: summon skill interval disabled intervalMs={} source={} windowKey={}",
                    logPrefix(context), intervalMs, request.getSourceTask(), windowKey);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DISABLED,
                    "summon skill interval disabled");
        }
        if (request.isRequireFreeStateForSummonSkill()
                && gameContext.getCurrentActionState() != GameContext.ActionState.FREE) {
            log.info("{} maintenance: summon skill deferred by action state state={} source={} windowKey={}",
                    logPrefix(context), gameContext.getCurrentActionState(), request.getSourceTask(), windowKey);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                    "summon skill deferred: action state is not free");
        }

        long now = System.currentTimeMillis();
        Long lastCleanAt = lastSummonSkillCleanAtByWindow.get(windowKey);
        if (lastCleanAt != null && now - lastCleanAt < intervalMs) {
            logSummonSkillNotDue(context, request, windowKey, now, lastCleanAt, intervalMs);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_NOT_DUE,
                    "summon skill not due");
        }
        log.info("{} maintenance: summon skill due source={} windowKey={} lastCleanAt={} elapsedMs={} intervalMs={}",
                logPrefix(context), request.getSourceTask(), windowKey, lastCleanAt,
                lastCleanAt == null ? -1 : now - lastCleanAt, intervalMs);
        SummonSkillWindowState windowState = summonSkillStateByWindow.computeIfAbsent(
                windowKey, key -> new SummonSkillWindowState());
        if (isSummonSkillTailSafeCacheExpired(windowState, now)) {
            log.info("{} maintenance: summon skill tail-safe cache expired source={} windowKey={} cacheAgeMs={} ttlMs={} lastEffectiveSlot={} nextStartSlot={}",
                    logPrefix(context), request.getSourceTask(), windowKey,
                    now - windowState.tailSafeCachedAt, SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS,
                    windowState.lastConfirmedEffectiveSlotIndex == null
                            ? null
                            : windowState.lastConfirmedEffectiveSlotIndex + 1,
                    windowState.nextStartIndex == null ? null : windowState.nextStartIndex + 1);
            windowState.lastConfirmedEffectiveSlotIndex = null;
            windowState.tailSafeCachedAt = 0L;
            windowState.nextStartIndex = null;
            windowState.slotStatusByIndex.clear();
        }
        if (isSummonSkillTailSafeCacheFresh(windowState, now)) {
            lastSummonSkillCleanAtByWindow.put(windowKey, now);
            lastSummonSkillNotDueLogAtByWindow.remove(windowKey);
            log.info("{} maintenance: summon skill skipped by fresh tail-safe cache source={} windowKey={} cacheAgeMs={} ttlMs={} lastEffectiveSlot={} nextStartSlot={}",
                    logPrefix(context), request.getSourceTask(), windowKey,
                    now - windowState.tailSafeCachedAt, SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS,
                    windowState.lastConfirmedEffectiveSlotIndex + 1,
                    windowState.nextStartIndex + 1);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_NOT_DUE,
                    "summon skill tail-safe cache fresh");
        }
        String teamRoundKey = resolveTeamRoundKey(context, request);
        if (request.isOneSummonSkillPerTeamRound()) {
            if (teamRoundKey == null) {
                log.info("{} maintenance: summon skill deferred, no active team round source={} windowKey={} teamKey={}",
                        logPrefix(context), request.getSourceTask(), windowKey,
                        normalizeTeamKey(request.getTeamMaintenanceKey(), context));
                return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                        "summon skill deferred: no active team round");
            }
            if (request.isRequireOpenTeamMaintenanceWindow()
                    && teamMaintenanceWindowStateByRound.get(teamRoundKey) != TeamMaintenanceWindowState.PATHING_WINDOW_OPEN) {
                log.info("{} maintenance: summon skill deferred, team pathing window closed teamRound={} state={} source={} windowKey={}",
                        logPrefix(context), teamRoundKey, teamMaintenanceWindowStateByRound.get(teamRoundKey),
                        request.getSourceTask(), windowKey);
                return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                        "summon skill deferred: team pathing window closed");
            }
            Set<String> claims = summonSkillClaimsByTeamRound.computeIfAbsent(
                    teamRoundKey, ignored -> ConcurrentHashMap.newKeySet());
            int maxClaims = Math.max(1, request.getMaxSummonSkillCleanersPerTeamRound());
            synchronized (claims) {
                if (claims.contains(windowKey)) {
                    log.info("{} maintenance: summon skill round already claimed by same window teamRound={} windowKey={} source={}",
                            logPrefix(context), teamRoundKey, windowKey, request.getSourceTask());
                    return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_ROUND_ALREADY_CLAIMED,
                            "summon skill round already claimed by " + windowKey);
                }
                if (claims.size() >= maxClaims) {
                    log.info("{} maintenance: summon skill round claim limit reached teamRound={} claims={} maxClaims={} windowKey={} source={}",
                            logPrefix(context), teamRoundKey, claims, maxClaims, windowKey, request.getSourceTask());
                    return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_ROUND_ALREADY_CLAIMED,
                            "summon skill round claim limit reached: " + claims);
                }
                claims.add(windowKey);
            }
            log.info("{} maintenance: summon skill round claimed teamRound={} windowKey={} claimCount={} maxClaims={} source={}",
                    logPrefix(context), teamRoundKey, windowKey, claims.size(), maxClaims, request.getSourceTask());
        }

        checkpoint(context);
        GameContext.ActionState previousState = gameContext.getCurrentActionState();
        gameContext.setCurrentActionState(GameContext.ActionState.INTERACTING);
        SummonSkillCleanupRequest cleanupRequest = buildSummonSkillCleanupRequest(windowState, now);
        SummonSkillCleanupResult cleanupResult = SummonSkillCleanupResult.failed("summon skill not attempted");
        long startedAt = System.currentTimeMillis();
        try {
            log.info("{} maintenance: start summon skill clean source={} windowKey={} previousState={} teamRound={} cachedSkillCount={} cachedStartSlot={} skipUltimateCorner={}",
                    logPrefix(context), request.getSourceTask(), windowKey, previousState, teamRoundKey,
                    windowState.skillCount, windowState.nextStartIndex == null ? null : windowState.nextStartIndex + 1,
                    cleanupRequest.isSkipUltimateCornerCheck());
            cleanupResult = summonSkillService.cleanSummonSkillsOnce(cleanupRequest);
            log.info("{} maintenance: summon skill clean finished success={} source={} windowKey={} elapsedMs={} skillCount={} nextStartSlot={} ultimateClicked={} ultimateSucceeded={} message={}",
                    logPrefix(context), cleanupResult.isSuccess(), request.getSourceTask(), windowKey,
                    System.currentTimeMillis() - startedAt, cleanupResult.getSkillCount(),
                    cleanupResult.getNextStartIndex() + 1, cleanupResult.isUltimateGenerateClicked(),
                    cleanupResult.isUltimateGenerateSucceeded(), cleanupResult.getMessage());
        } finally {
            if (cleanupResult.isSuccess()) {
                updateSummonSkillWindowState(windowKey, windowState, cleanupResult);
                lastSummonSkillCleanAtByWindow.put(windowKey, System.currentTimeMillis());
                lastSummonSkillNotDueLogAtByWindow.remove(windowKey);
            } else if (cleanupResult.isUltimateGenerateSucceeded()) {
                windowState.lastUltimateGenerateSuccessAt = System.currentTimeMillis();
                log.info("{} maintenance: summon skill ultimate generation succeeded before cleanup failure, cooldown recorded windowKey={} lastSuccessAt={}",
                        logPrefix(context), windowKey, windowState.lastUltimateGenerateSuccessAt);
            }
            if (gameContext.getCurrentActionState() == GameContext.ActionState.INTERACTING) {
                gameContext.setCurrentActionState(previousState);
            }
        }

        if (cleanupResult.isSuccess()) {
            return TaskMaintenanceResult.summonSkillCleaned("summon skill cleaned");
        }
        if (!hasSummonSkillStateChange(cleanupResult)) {
            releaseSummonSkillRoundClaimIfOwned(teamRoundKey, windowKey, cleanupResult);
        }
        return TaskMaintenanceResult.builder()
                .status(TaskMaintenanceStatus.SUMMON_SKILL_FAILED_RETRY_LATER)
                .summonSkillAttempted(true)
                .message("summon skill failed; retry later")
                .build();
    }

    private void logSummonSkillNotDue(TaskExecutionContext context,
                                      TaskMaintenanceRequest request,
                                      String windowKey,
                                      long now,
                                      long lastCleanAt,
                                      long intervalMs) {
        Long lastLogAt = lastSummonSkillNotDueLogAtByWindow.get(windowKey);
        if (lastLogAt != null && now - lastLogAt < SUMMON_SKILL_NOT_DUE_LOG_INTERVAL_MS) {
            return;
        }
        lastSummonSkillNotDueLogAtByWindow.put(windowKey, now);
        long elapsedMs = now - lastCleanAt;
        long remainingMs = Math.max(0, intervalMs - elapsedMs);
        log.info("{} maintenance: summon skill not due source={} windowKey={} elapsedMs={} remainingMs={} intervalMs={} lastCleanAt={}",
                logPrefix(context), request.getSourceTask(), windowKey, elapsedMs, remainingMs, intervalMs, lastCleanAt);
    }

    private SummonSkillCleanupRequest buildSummonSkillCleanupRequest(SummonSkillWindowState state, long now) {
        long ultimateCooldownMs = botProperties.getSummonSkillUltimateGenerateCooldownMs();
        boolean skipUltimateCorner = ultimateCooldownMs > 0
                && state.lastUltimateGenerateSuccessAt > 0
                && now - state.lastUltimateGenerateSuccessAt < ultimateCooldownMs;
        if (skipUltimateCorner) {
            log.info("maintenance: summon skill ultimate corner cooldown active elapsedMs={} remainingMs={}",
                    now - state.lastUltimateGenerateSuccessAt,
                    ultimateCooldownMs - (now - state.lastUltimateGenerateSuccessAt));
        }
        return SummonSkillCleanupRequest.builder()
                .expectedSkillCount(state.skillCount)
                .startSlotIndex(state.nextStartIndex)
                .skipUltimateCornerCheck(skipUltimateCorner)
                .build();
    }

    private void updateSummonSkillWindowState(String windowKey,
                                              SummonSkillWindowState state,
                                              SummonSkillCleanupResult result) {
        if (state.skillCount == null || state.skillCount != result.getSkillCount()) {
            state.slotStatusByIndex.clear();
            state.lastUltimateGenerateSuccessAt = 0L;
            state.lastConfirmedEffectiveSlotIndex = null;
            state.tailSafeCachedAt = 0L;
        }
        long now = System.currentTimeMillis();
        state.skillCount = result.getSkillCount();
        state.nextStartIndex = result.getNextStartIndex();
        state.slotStatusByIndex.putAll(result.getObservedStatusesByIndex());
        Integer lastEffectiveIndex = findLastConfirmedEffectiveSlotIndex(result.getObservedStatusesByIndex());
        state.lastConfirmedEffectiveSlotIndex = lastEffectiveIndex;
        if (lastEffectiveIndex != null && state.nextStartIndex != null && state.nextStartIndex > lastEffectiveIndex) {
            state.tailSafeCachedAt = now;
        } else {
            state.tailSafeCachedAt = 0L;
        }
        if (result.isUltimateGenerateSucceeded()) {
            state.lastUltimateGenerateSuccessAt = now;
        }
        log.info("maintenance: summon skill window state updated windowKey={} skillCount={} nextStartSlot={} lastEffectiveSlot={} tailSafeCachedAt={} observedSlots={} ultimateLastSuccessAt={}",
                windowKey, state.skillCount, state.nextStartIndex == null ? null : state.nextStartIndex + 1,
                state.lastConfirmedEffectiveSlotIndex == null ? null : state.lastConfirmedEffectiveSlotIndex + 1,
                state.tailSafeCachedAt, state.slotStatusByIndex, state.lastUltimateGenerateSuccessAt);
    }

    private boolean isSummonSkillTailSafeCacheExpired(SummonSkillWindowState state, long now) {
        return state.tailSafeCachedAt > 0
                && now - state.tailSafeCachedAt >= SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS;
    }

    private boolean isSummonSkillTailSafeCacheFresh(SummonSkillWindowState state, long now) {
        return state.tailSafeCachedAt > 0
                && now - state.tailSafeCachedAt < SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS
                && state.lastConfirmedEffectiveSlotIndex != null
                && state.nextStartIndex != null
                && state.nextStartIndex > state.lastConfirmedEffectiveSlotIndex;
    }

    private Integer findLastConfirmedEffectiveSlotIndex(Map<Integer, SummonSkillSlotStatus> statuses) {
        Integer lastIndex = null;
        for (Map.Entry<Integer, SummonSkillSlotStatus> entry : statuses.entrySet()) {
            if (!isEffectiveSummonSkillSlot(entry.getValue())) {
                continue;
            }
            if (lastIndex == null || entry.getKey() > lastIndex) {
                lastIndex = entry.getKey();
            }
        }
        return lastIndex;
    }

    private boolean isEffectiveSummonSkillSlot(SummonSkillSlotStatus status) {
        return status == SummonSkillSlotStatus.NORMAL_SKILL
                || status == SummonSkillSlotStatus.KEEP_SKILL
                || status == SummonSkillSlotStatus.EMPTY_SLOT;
    }

    private void releaseSummonSkillRoundClaimIfOwned(String teamRoundKey,
                                                     String windowKey,
                                                     SummonSkillCleanupResult cleanupResult) {
        if (teamRoundKey == null) {
            return;
        }
        Set<String> claims = summonSkillClaimsByTeamRound.get(teamRoundKey);
        boolean released = false;
        if (claims != null) {
            synchronized (claims) {
                released = claims.remove(windowKey);
                if (claims.isEmpty()) {
                    summonSkillClaimsByTeamRound.remove(teamRoundKey, claims);
                }
            }
        }
        if (released) {
            log.info("maintenance: summon skill round claim released after failed pass teamRound={} windowKey={} ultimateSucceeded={} message={}",
                    teamRoundKey, windowKey, cleanupResult.isUltimateGenerateSucceeded(), cleanupResult.getMessage());
        }
    }

    private boolean hasSummonSkillStateChange(SummonSkillCleanupResult cleanupResult) {
        return cleanupResult.isUltimateGenerateClicked()
                || cleanupResult.isUltimateGenerateSucceeded()
                || cleanupResult.getDeletedCount() > 0;
    }

    private TaskMaintenanceRequest normalize(TaskMaintenanceRequest request) {
        if (request == null) {
            return TaskMaintenanceRequest.builder()
                    .sourceTask("unknown")
                    .build();
        }
        if (request.getSourceTask() == null || request.getSourceTask().isBlank()) {
            return request.toBuilder()
                    .sourceTask("unknown")
                    .build();
        }
        return request;
    }

    private void checkpoint(TaskExecutionContext context) {
        if (context != null) {
            context.throwIfStopRequested();
        }
    }

    private String currentWindowKey(TaskExecutionContext context) {
        if (context != null && context.hasWindow()) {
            return context.getWindowId();
        }
        return windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getWindowId)
                .filter(id -> !id.isBlank())
                .orElse(DEFAULT_WINDOW_KEY);
    }

    private String logPrefix(TaskExecutionContext context) {
        return context == null ? "[window=unknown]" : context.getLogPrefix();
    }

    private String resolveTeamRoundKey(TaskExecutionContext context, TaskMaintenanceRequest request) {
        if (!request.isOneSummonSkillPerTeamRound()) {
            return null;
        }
        String teamKey = normalizeTeamKey(request.getTeamMaintenanceKey(), context);
        Integer round = request.getTeamRound();
        if (round == null) {
            round = activeTeamRoundByKey.get(teamKey);
        }
        if (round == null || round <= 0) {
            return null;
        }
        return teamRoundKey(teamKey, round);
    }

    private String normalizeTeamKey(String explicitKey, TaskExecutionContext context) {
        if (explicitKey != null && !explicitKey.isBlank()) {
            return explicitKey;
        }
        if (context != null && context.getRequestedTaskCode() != null && !context.getRequestedTaskCode().isBlank()) {
            return context.getRequestedTaskCode();
        }
        if (context != null && context.getTaskCode() != null && !context.getTaskCode().isBlank()) {
            return context.getTaskCode();
        }
        return DEFAULT_WINDOW_KEY;
    }

    private void pruneOlderTeamRoundClaims(String teamKey, int activeRound) {
        String prefix = teamKey + "#";
        summonSkillClaimsByTeamRound.keySet().removeIf(key -> {
            if (!key.startsWith(prefix)) {
                return false;
            }
            try {
                return Integer.parseInt(key.substring(prefix.length())) < activeRound;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        teamMaintenanceWindowStateByRound.keySet().removeIf(key -> {
            if (!key.startsWith(prefix)) {
                return false;
            }
            try {
                return Integer.parseInt(key.substring(prefix.length())) < activeRound;
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }

    private String teamRoundKey(String teamKey, int round) {
        return teamKey + "#" + round;
    }

    private static class SummonSkillWindowState {
        private Integer skillCount;
        private Integer nextStartIndex;
        private Integer lastConfirmedEffectiveSlotIndex;
        private long tailSafeCachedAt;
        private long lastUltimateGenerateSuccessAt;
        private final Map<Integer, SummonSkillSlotStatus> slotStatusByIndex = new ConcurrentHashMap<>();
    }
}
