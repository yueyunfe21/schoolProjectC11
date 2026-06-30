package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupRequest;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupResult;
import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;
import com.bot.dhxy.model.maintenance.TeamMaintenanceWindowState;
import com.bot.dhxy.model.maintenance.TeamSupportCapability;
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

import java.util.Collection;
import java.util.HashSet;
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
    private static final long SUMMON_SKILL_COUNT_CACHE_TTL_MS = 2 * 60 * 60 * 1000L;

    private final BotProperties botProperties;
    private final GameContext gameContext;
    private final DialogService dialogService;
    private final SummonSkillService summonSkillService;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final Object teamMaintenanceWindowMonitor = new Object();
    private final Map<String, Long> lastSummonSkillCleanAtByWindow = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSummonSkillNotDueLogAtByWindow = new ConcurrentHashMap<>();
    private final Map<String, Long> summonSkillUnknownRetryAfterByWindow = new ConcurrentHashMap<>();
    private final Map<String, SummonSkillWindowState> summonSkillStateByWindow = new ConcurrentHashMap<>();
    private final Map<String, Integer> activeTeamRoundByKey = new ConcurrentHashMap<>();
    private final Map<String, TeamMaintenanceWindowState> teamMaintenanceWindowStateByRound = new ConcurrentHashMap<>();
    private final Map<String, LocalTeamSessionState> localTeamSessions = new ConcurrentHashMap<>();
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
        openLocalTeamSupportCapability(context, TeamSupportCapability.FIRST_AID, sourceTask);
        openLocalTeamSupportCapability(context, TeamSupportCapability.PATHING_WINDOW, sourceTask);
        openLocalTeamSupportCapability(context, TeamSupportCapability.COMMON_BOX, sourceTask);
        openLocalTeamSupportCapability(context, TeamSupportCapability.SUMMON_SKILL, sourceTask);
        openLocalTeamSupportCapability(context, TeamSupportCapability.LEFT_TOP_STATUS, sourceTask);
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
        openLocalTeamSupportCapability(context, TeamSupportCapability.FIRST_AID, sourceTask);
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
        closeLocalTeamSupportCapabilities(context, sourceTask,
                TeamSupportCapability.FIRST_AID,
                TeamSupportCapability.PATHING_WINDOW,
                TeamSupportCapability.COMMON_BOX,
                TeamSupportCapability.SUMMON_SKILL,
                TeamSupportCapability.LEFT_TOP_STATUS);
        if (previous != null && previous != TeamMaintenanceWindowState.CLOSED) {
            log.info("{} maintenance team window closed: teamRound={} previous={} source={}",
                    logPrefix(context), roundKey, previous, sourceTask);
        }
    }

    /**
     * Release local support members to click the return-team button after the leader has already
     * reached its own post-combat return wait point.
     *
     * @param context current leader task context.
     * @param sourceTask diagnostic source written to logs.
     */
    public void openLocalTeamReturnSupportWindow(TaskExecutionContext context, String sourceTask) {
        openLocalTeamSupportCapability(context, TeamSupportCapability.TEAM_RETURN, sourceTask);
        openLocalTeamSupportCapability(context, TeamSupportCapability.COMMON_BOX, sourceTask);
        synchronized (teamMaintenanceWindowMonitor) {
            teamMaintenanceWindowMonitor.notifyAll();
        }
    }

    /**
     * Close the local return-team click opportunity once the leader no longer sees a return signal.
     *
     * @param context current leader task context.
     * @param sourceTask diagnostic source written to logs.
     */
    public void closeLocalTeamReturnSupportWindow(TaskExecutionContext context, String sourceTask) {
        closeLocalTeamSupportCapabilities(context, sourceTask,
                TeamSupportCapability.TEAM_RETURN,
                TeamSupportCapability.COMMON_BOX);
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

    /**
     * Wait for a local leader capability in the same UI-started session.
     *
     * @param context current member context. The local session key and support-member flag are
     *                trusted only from the runner-created {@link TaskExecutionContext}.
     * @param capability capability requested by the member path.
     * @param timeoutMs maximum wait time for this check.
     * @return true when the local leader has opened the requested capability for this session.
     */
    public boolean awaitLocalTeamSupportCapabilityOpen(TaskExecutionContext context,
                                                       TeamSupportCapability capability,
                                                       long timeoutMs) {
        if (!isLocalSupportMemberSession(context) || capability == null) {
            return false;
        }
        if (isLocalTeamSupportCapabilityOpen(context, capability)) {
            return true;
        }
        if (timeoutMs <= 0L) {
            return false;
        }

        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (teamMaintenanceWindowMonitor) {
            while (!isLocalTeamSupportCapabilityOpen(context, capability)) {
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

    /**
     * @param context current task context.
     * @return true only for local member support workers that belong to a runner-created session.
     */
    public boolean isLocalSupportMemberSession(TaskExecutionContext context) {
        return context != null
                && context.isLocalLeaderPresent()
                && context.isLocalSupportMember()
                && context.hasLocalTeamSession()
                && hasDetectedLocalLeader(context);
    }

    /**
     * Register all selected windows for a local-team session whose leader may only become known
     * after runner live role preflight.
     *
     * @param sessionKey shared local-team session id from the UI same-queue submit.
     * @param windowIds selected window ids in this submit batch.
     * @param sourceTask diagnostic source written to logs.
     */
    public void registerLocalTeamSessionCandidate(String sessionKey,
                                                  Collection<String> windowIds,
                                                  String sourceTask) {
        if (sessionKey == null || sessionKey.isBlank() || windowIds == null || windowIds.isEmpty()) {
            return;
        }
        LocalTeamSessionState state = localTeamSessions.computeIfAbsent(
                sessionKey, ignored -> new LocalTeamSessionState());
        for (String windowId : windowIds) {
            if (windowId != null && !windowId.isBlank()) {
                state.candidateWindows.add(windowId);
            }
        }
        state.leaderAbsent = false;
        log.info("maintenance local-team candidate registered: session={} windows={} source={}",
                sessionKey, state.candidateWindows, sourceTask);
    }

    /**
     * Record one runner's live role preflight for candidate-session resolution.
     *
     * @param context current runner context carrying local session metadata.
     * @param windowId window whose live role was detected.
     * @param roleName live role name, such as {@code LEADER}, {@code MEMBER}, or {@code SOLO}.
     * @param sourceTask diagnostic source written to logs.
     */
    public void markLocalTeamWindowRoleDetected(TaskExecutionContext context,
                                                String windowId,
                                                String roleName,
                                                String sourceTask) {
        if (context == null || !context.isLocalLeaderPresent() || !context.hasLocalTeamSession()
                || windowId == null || windowId.isBlank()) {
            return;
        }
        String sessionKey = context.getLocalTeamSessionKey();
        LocalTeamSessionState state = localTeamSessions.computeIfAbsent(
                sessionKey, ignored -> new LocalTeamSessionState());
        state.roleDetectedWindows.add(windowId);
        if ("LEADER".equalsIgnoreCase(roleName)) {
            markLocalTeamLeaderDetected(context, windowId, sourceTask);
            return;
        }
        boolean leaderAbsentConfirmed = false;
        synchronized (state) {
            Set<String> resolvedWindows = new HashSet<>(state.roleDetectedWindows);
            resolvedWindows.addAll(state.completedWindows);
            if (!state.candidateWindows.isEmpty()
                    && resolvedWindows.containsAll(state.candidateWindows)
                    && state.leaderWindowId == null
                    && !state.leaderAbsent) {
                state.leaderAbsent = true;
                leaderAbsentConfirmed = true;
            }
        }
        if (leaderAbsentConfirmed) {
            log.info("{} maintenance local-team leader absent confirmed: session={} detected={} expected={} source={}",
                    logPrefix(context), sessionKey, state.roleDetectedWindows, state.candidateWindows, sourceTask);
            synchronized (teamMaintenanceWindowMonitor) {
                teamMaintenanceWindowMonitor.notifyAll();
            }
        }
    }

    /**
     * @param context current task context.
     * @return true for member auto-battle workers that belong to a local-team candidate/session.
     */
    public boolean isLocalSupportMemberCandidate(TaskExecutionContext context) {
        return context != null
                && context.isLocalLeaderPresent()
                && context.isLocalSupportMember()
                && context.hasLocalTeamSession();
    }

    /**
     * @param context current member context.
     * @return true while a local-team candidate has not yet discovered a live local leader and has
     *         not yet proven that no selected local leader exists.
     */
    public boolean isPendingLocalSupportLeaderDetection(TaskExecutionContext context) {
        LocalTeamSessionState state = context == null || !context.hasLocalTeamSession()
                ? null
                : localTeamSessions.get(context.getLocalTeamSessionKey());
        return isLocalSupportMemberCandidate(context)
                && !hasDetectedLocalLeader(context)
                && state != null
                && !state.leaderAbsent;
    }

    /**
     * Mark that a runner in this local-team session has live-detected itself as leader.
     *
     * @param context current runner context carrying the local session key.
     * @param leaderWindowId window id that detected leader role.
     * @param sourceTask diagnostic source written to logs.
     */
    public void markLocalTeamLeaderDetected(TaskExecutionContext context,
                                            String leaderWindowId,
                                            String sourceTask) {
        if (context == null || !context.isLocalLeaderPresent() || !context.hasLocalTeamSession()
                || leaderWindowId == null || leaderWindowId.isBlank()) {
            return;
        }
        String sessionKey = context.getLocalTeamSessionKey();
        LocalTeamSessionState state = localTeamSessions.computeIfAbsent(
                sessionKey, ignored -> new LocalTeamSessionState());
        String previous;
        synchronized (state) {
            state.leaderAbsent = false;
            previous = state.leaderWindowId;
            if (previous == null) {
                state.leaderWindowId = leaderWindowId;
            }
        }
        if (previous == null) {
            log.info("{} maintenance local-team leader detected: session={} leaderWindow={} source={}",
                    logPrefix(context), sessionKey, leaderWindowId, sourceTask);
        } else if (!previous.equals(leaderWindowId)) {
            log.warn("{} maintenance local-team leader conflict ignored: session={} existingLeader={} newLeader={} source={}",
                    logPrefix(context), sessionKey, previous, leaderWindowId, sourceTask);
        }
    }

    /**
     * Check a local-session capability without waiting.
     *
     * @param context current leader/member task context. The local session key is used as the
     *                capability namespace.
     * @param capability capability requested by the caller.
     * @return true only when the leader has explicitly opened the capability for this session.
     */
    public boolean isLocalTeamSupportCapabilityOpen(TaskExecutionContext context,
                                                    TeamSupportCapability capability) {
        if (context == null || capability == null || !context.hasLocalTeamSession()) {
            return false;
        }
        LocalTeamSessionState state = localTeamSessions.get(context.getLocalTeamSessionKey());
        return state != null && state.capabilities.contains(capability);
    }

    private boolean hasDetectedLocalLeader(TaskExecutionContext context) {
        LocalTeamSessionState state = context == null || !context.hasLocalTeamSession()
                ? null
                : localTeamSessions.get(context.getLocalTeamSessionKey());
        return state != null && state.leaderWindowId != null;
    }

    private void openLocalTeamSupportCapability(TaskExecutionContext context,
                                                TeamSupportCapability capability,
                                                String sourceTask) {
        if (context == null || !context.isLocalLeaderPresent() || !context.hasLocalTeamSession()
                || capability == null) {
            return;
        }
        LocalTeamSessionState state = localTeamSessions.computeIfAbsent(
                context.getLocalTeamSessionKey(), ignored -> new LocalTeamSessionState());
        boolean added = state.capabilities.add(capability);
        if (added) {
            int epoch = state.capabilityEpochByCapability.merge(
                    capability,
                    1,
                    Integer::sum);
            log.info("{} maintenance local-team capability opened: session={} capability={} epoch={} leaderWindow={} task={} source={}",
                    logPrefix(context), context.getLocalTeamSessionKey(), capability,
                    epoch, context.getLocalLeaderWindowId(), context.getTaskCode(), sourceTask);
        }
    }

    private void closeLocalTeamSupportCapabilities(TaskExecutionContext context,
                                                   String sourceTask,
                                                   TeamSupportCapability... capabilitiesToClose) {
        if (context == null || !context.hasLocalTeamSession() || capabilitiesToClose == null
                || capabilitiesToClose.length == 0) {
            return;
        }
        LocalTeamSessionState state = localTeamSessions.get(context.getLocalTeamSessionKey());
        if (state == null || state.capabilities.isEmpty()) {
            return;
        }
        for (TeamSupportCapability capability : capabilitiesToClose) {
            if (capability != null && state.capabilities.remove(capability)) {
                log.info("{} maintenance local-team capability closed: session={} capability={} leaderWindow={} task={} source={}",
                        logPrefix(context), context.getLocalTeamSessionKey(), capability,
                        context.getLocalLeaderWindowId(), context.getTaskCode(), sourceTask);
            }
        }
    }

    /**
     * Mark one selected window as finished for a UI-started local-team session.
     *
     * <p>CR138 local support state is shared by all windows that were submitted together. A single
     * finished leader/member must not clear the session while another candidate window is still
     * running, but once every registered candidate has finished, stale live-leader evidence and open
     * capabilities must be removed so a later queue cannot inherit them.</p>
     *
     * @param sessionKey shared local-team session id.
     * @param windowId selected window id that just left its task queue.
     * @param sourceTask diagnostic source written to logs.
     */
    public void completeLocalTeamSessionWindow(String sessionKey, String windowId, String sourceTask) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return;
        }
        LocalTeamSessionState state = localTeamSessions.get(sessionKey);
        if (state == null) {
            return;
        }
        if (windowId != null && !windowId.isBlank()) {
            state.completedWindows.add(windowId);
        }
        boolean allCandidatesFinished = state.candidateWindows.isEmpty()
                || state.completedWindows.containsAll(state.candidateWindows);
        if (allCandidatesFinished && localTeamSessions.remove(sessionKey, state)) {
            log.info("maintenance local-team session completed: session={} completedWindows={} candidates={} source={}",
                    sessionKey, state.completedWindows, state.candidateWindows, sourceTask);
            synchronized (teamMaintenanceWindowMonitor) {
                teamMaintenanceWindowMonitor.notifyAll();
            }
        }
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
            TaskMaintenanceResult broadcastResult = handleMaintenanceBroadcast(context, safeRequest);
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

    private TaskMaintenanceResult handleMaintenanceBroadcast(TaskExecutionContext context,
                                                             TaskMaintenanceRequest safeRequest) {
        String sourceTask = safeRequest.getSourceTask();
        DialogResult dialogResult = dialogService.handleDialog(
                DialogHandleRequest.handleMaintenanceBroadcastOption(
                        sourceTask, safeRequest.isAllowFullMaintenanceBroadcastFallback()));
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
        SummonSkillWindowState windowState = summonSkillState(windowKey);
        Long lastCleanAt = lastSummonSkillCleanAtByWindow.get(windowKey);
        if (lastCleanAt != null && now - lastCleanAt < intervalMs) {
            logSummonSkillNotDue(context, request, windowKey, now, lastCleanAt, intervalMs);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_NOT_DUE,
                    "summon skill not due");
        }
        log.info("{} maintenance: summon skill due source={} windowKey={} lastCleanAt={} elapsedMs={} intervalMs={}",
                logPrefix(context), request.getSourceTask(), windowKey, lastCleanAt,
                lastCleanAt == null ? -1 : now - lastCleanAt, intervalMs);
        Long unknownRetryAfterAt = summonSkillUnknownRetryAfterByWindow.get(windowKey);
        if (unknownRetryAfterAt != null && now < unknownRetryAfterAt) {
            long remainingMs = unknownRetryAfterAt - now;
            log.info("{} maintenance: summon skill unknown retry backoff active source={} windowKey={} remainingMs={} retryAfterAt={}",
                    logPrefix(context), request.getSourceTask(), windowKey, remainingMs, unknownRetryAfterAt);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                    "summon skill unknown retry backoff active");
        }
        if (unknownRetryAfterAt != null) {
            summonSkillUnknownRetryAfterByWindow.remove(windowKey, unknownRetryAfterAt);
        }
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
                log.info("{} maintenance: summon skill deferred, no active team round source={} windowKey={} teamKey={} localCapability={}",
                        logPrefix(context), request.getSourceTask(), windowKey,
                        normalizeTeamKey(request.getTeamMaintenanceKey(), context),
                        request.getRequiredLocalSupportCapability());
                return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                        "summon skill deferred: no active team round");
            }
            if (request.getRequiredLocalSupportCapability() != null
                    && !isLocalTeamSupportCapabilityOpen(context, request.getRequiredLocalSupportCapability())) {
                log.info("{} maintenance: summon skill deferred, local capability closed capability={} teamRound={} source={} windowKey={}",
                        logPrefix(context), request.getRequiredLocalSupportCapability(), teamRoundKey,
                        request.getSourceTask(), windowKey);
                return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                        "summon skill deferred: local support capability closed");
            }
            if (request.getRequiredLocalSupportCapability() == null
                    && request.isRequireOpenTeamMaintenanceWindow()
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
            log.info("{} maintenance: start summon skill clean source={} windowKey={} previousState={} teamRound={} cachedSkillCount={} trustSkillCount={} cachedStartSlot={} skipUltimateCorner={}",
                    logPrefix(context), request.getSourceTask(), windowKey, previousState, teamRoundKey,
                    windowState.skillCount, cleanupRequest.isTrustExpectedSkillCount(),
                    windowState.nextStartIndex == null ? null : windowState.nextStartIndex + 1,
                    cleanupRequest.isSkipUltimateCornerCheck());
            cleanupResult = summonSkillService.cleanSummonSkillsOnce(cleanupRequest);
            log.info("{} maintenance: summon skill clean finished success={} source={} windowKey={} elapsedMs={} skillCount={} nextStartSlot={} ultimateClicked={} ultimateSucceeded={} message={}",
                    logPrefix(context), cleanupResult.isSuccess(), request.getSourceTask(), windowKey,
                    System.currentTimeMillis() - startedAt, cleanupResult.getSkillCount(),
                    cleanupResult.getNextStartIndex() + 1, cleanupResult.isUltimateGenerateClicked(),
                    cleanupResult.isUltimateGenerateSucceeded(), cleanupResult.getMessage());
        } finally {
            if (cleanupResult.isSuccess()) {
                summonSkillUnknownRetryAfterByWindow.remove(windowKey);
                updateSummonSkillWindowState(windowKey, windowState, cleanupResult);
                lastSummonSkillCleanAtByWindow.put(windowKey, System.currentTimeMillis());
                lastSummonSkillNotDueLogAtByWindow.remove(windowKey);
            } else if (cleanupResult.isUltimateGenerateSucceeded()) {
                windowState.lastUltimateGenerateSuccessAt = System.currentTimeMillis();
                log.info("{} maintenance: summon skill ultimate generation succeeded before cleanup failure, cooldown recorded windowKey={} lastSuccessAt={}",
                        logPrefix(context), windowKey, windowState.lastUltimateGenerateSuccessAt);
            }
            if (!cleanupResult.isSuccess() && isUnknownSummonSkillFailure(cleanupResult)) {
                long retryMs = Math.max(0L, botProperties.getSummonSkillUnknownFailureRetryAfterMs());
                long retryAfterAt = System.currentTimeMillis() + retryMs;
                summonSkillUnknownRetryAfterByWindow.put(windowKey, retryAfterAt);
                invalidateSummonSkillLayoutCache(windowKey, windowState, cleanupResult);
                log.warn("{} maintenance: summon skill unknown failure backoff recorded source={} windowKey={} retryAfterMs={} retryAfterAt={} message={} observedSlots={}",
                        logPrefix(context), request.getSourceTask(), windowKey, retryMs, retryAfterAt,
                        cleanupResult.getMessage(), cleanupResult.getObservedStatusesByIndex());
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
        boolean trustSkillCount = state.skillCount != null
                && state.skillCountCachedAt > 0
                && now - state.skillCountCachedAt < SUMMON_SKILL_COUNT_CACHE_TTL_MS;
        if (state.skillCount != null) {
            log.info("maintenance: summon skill count cache state skillCount={} trust={} ageMs={} ttlMs={}",
                    state.skillCount, trustSkillCount,
                    state.skillCountCachedAt <= 0 ? -1 : now - state.skillCountCachedAt,
                    SUMMON_SKILL_COUNT_CACHE_TTL_MS);
        }
        if (skipUltimateCorner) {
            log.info("maintenance: summon skill ultimate corner cooldown active elapsedMs={} remainingMs={}",
                    now - state.lastUltimateGenerateSuccessAt,
                    ultimateCooldownMs - (now - state.lastUltimateGenerateSuccessAt));
        }
        return SummonSkillCleanupRequest.builder()
                .expectedSkillCount(state.skillCount)
                .trustExpectedSkillCount(trustSkillCount)
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
        state.skillCountCachedAt = now;
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
        log.info("maintenance: summon skill window state updated windowKey={} skillCount={} skillCountCachedAt={} nextStartSlot={} lastEffectiveSlot={} tailSafeCachedAt={} observedSlots={} ultimateLastSuccessAt={}",
                windowKey, state.skillCount, state.skillCountCachedAt,
                state.nextStartIndex == null ? null : state.nextStartIndex + 1,
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

    private boolean isUnknownSummonSkillFailure(SummonSkillCleanupResult cleanupResult) {
        if (cleanupResult == null || cleanupResult.isSuccess()) {
            return false;
        }
        String message = cleanupResult.getMessage();
        if (message != null && message.toLowerCase().contains("unknown")) {
            return true;
        }
        return cleanupResult.getObservedStatusesByIndex().containsValue(SummonSkillSlotStatus.UNKNOWN);
    }

    private void invalidateSummonSkillLayoutCache(String windowKey,
                                                  SummonSkillWindowState state,
                                                  SummonSkillCleanupResult cleanupResult) {
        Integer oldSkillCount = state.skillCount;
        Integer oldNextStartIndex = state.nextStartIndex;
        Integer oldLastEffectiveIndex = state.lastConfirmedEffectiveSlotIndex;
        long oldTailSafeCachedAt = state.tailSafeCachedAt;
        int oldObservedCount = state.slotStatusByIndex.size();
        state.skillCount = null;
        state.skillCountCachedAt = 0L;
        state.nextStartIndex = null;
        state.lastConfirmedEffectiveSlotIndex = null;
        state.tailSafeCachedAt = 0L;
        state.slotStatusByIndex.clear();
        log.info("maintenance: summon skill layout cache invalidated after unknown failure windowKey={} oldSkillCount={} oldNextStartSlot={} oldLastEffectiveSlot={} oldTailSafeCachedAt={} oldObservedCount={} message={}",
                windowKey, oldSkillCount,
                oldNextStartIndex == null ? null : oldNextStartIndex + 1,
                oldLastEffectiveIndex == null ? null : oldLastEffectiveIndex + 1,
                oldTailSafeCachedAt, oldObservedCount, cleanupResult.getMessage());
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

    private SummonSkillWindowState summonSkillState(String windowKey) {
        long epoch = currentPlayerIdentityEpoch();
        return summonSkillStateByWindow.compute(windowKey, (key, existing) -> {
            if (existing == null) {
                SummonSkillWindowState created = new SummonSkillWindowState();
                created.playerIdentityEpoch = epoch;
                return created;
            }
            if (existing.playerIdentityEpoch != epoch) {
                log.warn("maintenance: invalidate summon skill cache by player identity drift windowKey={} oldEpoch={} newEpoch={} cachedSkillCount={} nextStartSlot={} tailSafeCachedAt={}",
                        windowKey, existing.playerIdentityEpoch, epoch, existing.skillCount,
                        existing.nextStartIndex == null ? null : existing.nextStartIndex + 1,
                        existing.tailSafeCachedAt);
                lastSummonSkillCleanAtByWindow.remove(windowKey);
                lastSummonSkillNotDueLogAtByWindow.remove(windowKey);
                summonSkillUnknownRetryAfterByWindow.remove(windowKey);
                SummonSkillWindowState reset = new SummonSkillWindowState();
                reset.playerIdentityEpoch = epoch;
                return reset;
            }
            return existing;
        });
    }

    private long currentPlayerIdentityEpoch() {
        return windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getPlayerIdentityEpoch)
                .orElse(0L);
    }

    private String logPrefix(TaskExecutionContext context) {
        return context == null ? "[window=unknown]" : context.getLogPrefix();
    }

    private String resolveTeamRoundKey(TaskExecutionContext context, TaskMaintenanceRequest request) {
        if (!request.isOneSummonSkillPerTeamRound()) {
            return null;
        }
        if (request.getRequiredLocalSupportCapability() != null) {
            return resolveLocalSupportCapabilityRoundKey(context, request.getRequiredLocalSupportCapability());
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

    private String resolveLocalSupportCapabilityRoundKey(TaskExecutionContext context,
                                                         TeamSupportCapability capability) {
        if (!isLocalSupportMemberSession(context) || capability == null) {
            return null;
        }
        LocalTeamSessionState state = localTeamSessions.get(context.getLocalTeamSessionKey());
        Integer epoch = state == null ? null : state.capabilityEpochByCapability.get(capability);
        if (epoch == null || epoch <= 0) {
            return null;
        }
        return "local-team:" + context.getLocalTeamSessionKey() + "#" + capability.name() + "#" + epoch;
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
        private long playerIdentityEpoch;
        private Integer skillCount;
        private long skillCountCachedAt;
        private Integer nextStartIndex;
        private Integer lastConfirmedEffectiveSlotIndex;
        private long tailSafeCachedAt;
        private long lastUltimateGenerateSuccessAt;
        private final Map<Integer, SummonSkillSlotStatus> slotStatusByIndex = new ConcurrentHashMap<>();
    }

    private static final class LocalTeamSessionState {
        private final Set<TeamSupportCapability> capabilities = ConcurrentHashMap.newKeySet();
        private final Map<TeamSupportCapability, Integer> capabilityEpochByCapability = new ConcurrentHashMap<>();
        private final Set<String> candidateWindows = ConcurrentHashMap.newKeySet();
        private final Set<String> roleDetectedWindows = ConcurrentHashMap.newKeySet();
        private final Set<String> completedWindows = ConcurrentHashMap.newKeySet();
        private volatile String leaderWindowId;
        private volatile boolean leaderAbsent;
    }
}
