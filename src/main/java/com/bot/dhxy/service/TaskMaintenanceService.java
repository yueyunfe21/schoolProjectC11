package com.bot.dhxy.service;

import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.cloud.task.CapabilityGateCloudDecisionService;
import com.bot.dhxy.cloud.task.MaintenanceThresholdCloudDecision;
import com.bot.dhxy.cloud.task.MaintenanceThresholdCloudDecisionService;
import com.bot.dhxy.cloud.runtime.RuntimeDecisionShadowService;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupRequest;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupResult;
import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;
import com.bot.dhxy.model.maintenance.TeamMaintenanceWindowState;
import com.bot.dhxy.model.maintenance.TeamSupportCapability;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.model.maintenance.TaskMaintenanceResult;
import com.bot.dhxy.model.maintenance.TaskMaintenanceStatus;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowReadyEvent;
import com.bot.dhxy.window.model.WindowReadyEventType;
import com.bot.dhxy.window.runtime.WindowReadyEventBus;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin scheduler for task maintenance that is shared by auto-battle and formal task flows.
 *
 * <p>This service owns priority, cooldown, and logging. Longer summon-skill cleanup still goes
 * through {@link SummonSkillService}. Callers decide when a maintenance pass is safe for their task
 * turn.</p>
 */
@Slf4j
@Service
public class TaskMaintenanceService {

    private static final String DEFAULT_WINDOW_KEY = "default";
    private static final long SUMMON_SKILL_NOT_DUE_LOG_INTERVAL_MS = 60_000L;
    private static final long SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS = 2 * 60 * 60 * 1000L;
    private static final long SUMMON_SKILL_COUNT_CACHE_TTL_MS = 2 * 60 * 60 * 1000L;
    private static final long SUMMON_SKILL_DUE_LEAD_TIME_MS = 90_000L;
    private static final long COMPLETED_LOCAL_TEAM_SESSION_TTL_MS = 2 * 60 * 60 * 1000L;
    private static final long LOCAL_TEAM_IDLE_BROADCAST_SUPPRESS_CACHE_TTL_MS = 30_000L;
    private static final long LOCAL_TEAM_IDLE_BROADCAST_SUPPRESS_LOG_INTERVAL_MS = 60_000L;
    private static final long MAINTENANCE_NO_ACTION_LOG_INTERVAL_MS = 60_000L;
    private static final int COMPLETED_LOCAL_TEAM_SESSION_MAX_TOMBSTONES = 256;
    // Window-relative ROI for the maintenance broadcast option strip. CoordinateHelper converts it
    // through the current bound window before matching.
    private static final int MAINTENANCE_BROADCAST_ROI_LEFT = 260;
    private static final int MAINTENANCE_BROADCAST_ROI_TOP = 381;
    private static final int MAINTENANCE_BROADCAST_ROI_RIGHT = 378;
    private static final int MAINTENANCE_BROADCAST_ROI_BOTTOM = 413;
    private static final String MAINTENANCE_HEAL_ALL_REPAIR_TEMPLATE =
            "images/template/dialog/maintenance/maintenance_heal_all_repair_raw.png";
    private static final String MAINTENANCE_REPAIR_CONFIRM_TEMPLATE =
            "images/template/dialog/maintenance/maintenance_repair_confirm_raw.png";
    private static final double MAINTENANCE_BROADCAST_TEMPLATE_THRESHOLD = 0.85d;
    private static final int MAINTENANCE_BROADCAST_CLICK_SETTLE_MS = 150;
    private static final int MAINTENANCE_BROADCAST_CLICK_DELAY_MS = 800;

    private final BotProperties botProperties;
    private final GameContext gameContext;
    private final DialogService dialogService;
    private final SummonSkillService summonSkillService;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final RuntimeDecisionShadowService runtimeDecisionShadowService;
    private final CapabilityGateCloudDecisionService capabilityGateCloudDecisionService;
    private final MaintenanceThresholdCloudDecisionService maintenanceThresholdCloudDecisionService;
    private final Map<String, Long> lastSummonSkillCleanAtByWindow = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSummonSkillNotDueLogAtByWindow = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSummonSkillDeferredLogAtByKey = new ConcurrentHashMap<>();
    private final Map<String, Long> summonSkillUnknownRetryAfterByWindow = new ConcurrentHashMap<>();
    private final Map<String, SummonSkillWindowState> summonSkillStateByWindow = new ConcurrentHashMap<>();
    private final Map<String, Integer> activeTeamRoundByKey = new ConcurrentHashMap<>();
    private final Map<String, TeamMaintenanceWindowState> teamMaintenanceWindowStateByRound = new ConcurrentHashMap<>();
    private final Map<String, Long> maintenanceSnapshotOpenedAtByRound = new ConcurrentHashMap<>();
    private final Map<String, LocalTeamSessionState> localTeamSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> completedLocalTeamSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> summonSkillClaimsByTeamRound = new ConcurrentHashMap<>();
    private final Object summonSkillQueueMonitor = new Object();
    private final Deque<SummonSkillQueueItem> summonSkillQueue = new ArrayDeque<>();
    private final Set<String> summonSkillQueueKeys = ConcurrentHashMap.newKeySet();
    /**
     * CR243: per-team post-combat first-aid queue. Scope key is the local team session key, or a
     * window-scoped key for windows without a session, so unrelated windows never share a queue.
     */
    private final Object postCombatFirstAidMonitor = new Object();
    private final Map<String, PostCombatFirstAidQueueState> postCombatFirstAidQueueByScope = new ConcurrentHashMap<>();
    /**
     * CR244: soft wake bus for member pending-return state changes. Field injection keeps the two
     * legacy constructors (and their test call sites) source-compatible; publish is null-guarded.
     */
    @Autowired(required = false)
    private WindowReadyEventBus windowReadyEventBus;

    public TaskMaintenanceService(BotProperties botProperties,
                                   GameContext gameContext,
                                   DialogService dialogService,
                                   SummonSkillService summonSkillService,
                                   WindowTaskContextHolder windowTaskContextHolder,
                                   RuntimeDecisionShadowService runtimeDecisionShadowService,
                                   CapabilityGateCloudDecisionService capabilityGateCloudDecisionService,
                                   MaintenanceThresholdCloudDecisionService maintenanceThresholdCloudDecisionService) {
        this(botProperties,
                gameContext,
                dialogService,
                summonSkillService,
                windowTaskContextHolder,
                null,
                null,
                runtimeDecisionShadowService,
                capabilityGateCloudDecisionService,
                maintenanceThresholdCloudDecisionService);
    }

    @Autowired
    public TaskMaintenanceService(BotProperties botProperties,
                                   GameContext gameContext,
                                   DialogService dialogService,
                                   SummonSkillService summonSkillService,
                                   WindowTaskContextHolder windowTaskContextHolder,
                                   CoordinateHelper coordinateHelper,
                                   InputSequences inputSequences,
                                   RuntimeDecisionShadowService runtimeDecisionShadowService,
                                   CapabilityGateCloudDecisionService capabilityGateCloudDecisionService,
                                   MaintenanceThresholdCloudDecisionService maintenanceThresholdCloudDecisionService) {
        this.botProperties = botProperties;
        this.gameContext = gameContext;
        this.dialogService = dialogService;
        this.summonSkillService = summonSkillService;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.coordinateHelper = coordinateHelper;
        this.inputSequences = inputSequences;
        this.runtimeDecisionShadowService = runtimeDecisionShadowService;
        this.capabilityGateCloudDecisionService = capabilityGateCloudDecisionService;
        this.maintenanceThresholdCloudDecisionService = maintenanceThresholdCloudDecisionService;
    }

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
            reportFeatureFlagShadow(context, sourceTask, "summonSkillCleanRunImmediatelyOnStart", true,
                    "summon skill cooldown cleared for task start");
            return;
        }
        lastSummonSkillCleanAtByWindow.put(windowKey, System.currentTimeMillis());
        log.info("{} maintenance init: summon skill cooldown starts now source={}",
                logPrefix(context), sourceTask);
        reportFeatureFlagShadow(context, sourceTask, "summonSkillCleanRunImmediatelyOnStart", false,
                "summon skill cooldown starts at task start");
    }

    /**
     * Clear queued summon-skill work owned by a window that is leaving the active runner lifecycle.
     *
     * @param windowKey window id whose pending summon-skill item should no longer block the FIFO.
     * @param sourceTask diagnostic source written to logs.
     */
    public void clearSummonSkillQueueForWindow(String windowKey, String sourceTask) {
        if (windowKey == null || windowKey.isBlank()) {
            return;
        }
        int removed = removeSummonSkillQueueItemsForWindow(windowKey);
        summonSkillUnknownRetryAfterByWindow.remove(windowKey);
        if (removed > 0) {
            log.info("maintenance: cleared summon skill queue items for inactive window windowKey={} removed={} source={}",
                    windowKey, removed, sourceTask);
        }
    }

    /** CR243 post-combat first-aid report states; UNKNOWN must be enqueued conservatively. */
    public enum PostCombatFirstAidReport {
        HEALTHY,
        SUPPLY_NEEDED,
        UNKNOWN
    }

    /**
     * CR243: record one window's post-combat first-aid report for its team-scoped queue.
     *
     * <p>Every window that exits combat must report exactly one of HEALTHY / SUPPLY_NEEDED /
     * UNKNOWN. SUPPLY_NEEDED and UNKNOWN enqueue a FIFO supply item (dedupe per window); the
     * leader's item is always placed ahead of member items. Reporting never performs supply input;
     * consumption is gated by {@link #openPostCombatFirstAidQueue}.</p>
     */
    public void reportPostCombatFirstAid(TaskExecutionContext context,
                                         PostCombatFirstAidReport report,
                                         boolean leaderWindow,
                                         String source) {
        if (report == null) {
            return;
        }
        String scopeKey = postCombatFirstAidScopeKey(context);
        String windowKey = currentWindowKey(context);
        synchronized (postCombatFirstAidMonitor) {
            PostCombatFirstAidQueueState state = postCombatFirstAidQueueByScope.get(scopeKey);
            if (state == null) {
                /*
                 * CR243 follow-up review P1: the round's participant set is FIXED as an immutable
                 * snapshot of the confirmed tooltip group at the FIRST report. Later group cache
                 * changes or late attaches must neither remove unreported members from the barrier
                 * nor add new ones; only departedWindows (explicit lifecycle exits) shrink it.
                 */
                state = new PostCombatFirstAidQueueState();
                state.participantSnapshot.addAll(
                        resolvePostCombatFirstAidParticipantSnapshot(scopeKey, windowKey));
                postCombatFirstAidQueueByScope.put(scopeKey, state);
                log.info("{} maintenance: post-combat first-aid participant snapshot fixed scope={} participants={} firstReporter={} source={}",
                        logPrefix(context), scopeKey, state.participantSnapshot, windowKey, source);
            }
            String previous = state.reportsByWindow.put(windowKey, report.name());
            boolean queued = state.fifo.stream().anyMatch(item -> item.windowKey.equals(windowKey));
            if (report == PostCombatFirstAidReport.HEALTHY) {
                if (queued) {
                    state.fifo.removeIf(item -> item.windowKey.equals(windowKey));
                    log.info("{} maintenance: post-combat first-aid item removed by fresh HEALTHY report scope={} windowKey={} source={}",
                            logPrefix(context), scopeKey, windowKey, source);
                }
            } else if (!queued) {
                PostCombatFirstAidQueueItem item =
                        new PostCombatFirstAidQueueItem(windowKey, leaderWindow, System.currentTimeMillis(), source);
                if (leaderWindow) {
                    // User-approved CR243 order: after the leader clicks the green link, its own
                    // first-aid must run before member supply work, then the leader parks.
                    state.fifo.addFirst(item);
                } else {
                    state.fifo.addLast(item);
                }
            }
            log.info("{} maintenance: post-combat first-aid reported scope={} windowKey={} report={} previous={} leader={} queueSize={} open={} source={}",
                    logPrefix(context), scopeKey, windowKey, report, previous, leaderWindow,
                    state.fifo.size(), state.open, source);
            closePostCombatFirstAidQueueIfComplete(scopeKey, state, source + ":report");
        }
    }

    /**
     * CR243: open the team's post-combat first-aid queue for consumption. The only legal caller is
     * the 修罗 leader after the tracker green link click has really returned PATHING_STARTED.
     *
     * @return true when a queue exists and has pending supply items after opening.
     */
    public boolean openPostCombatFirstAidQueue(TaskExecutionContext context, String source) {
        String scopeKey = postCombatFirstAidScopeKey(context);
        synchronized (postCombatFirstAidMonitor) {
            PostCombatFirstAidQueueState state = postCombatFirstAidQueueByScope.get(scopeKey);
            if (state == null) {
                /*
                 * Nothing collected: no combat preceded this green click (e.g. round 1). Opening
                 * an empty round here would arm the strict all-reported barrier against windows
                 * that never fought. A report arriving after this point collects into a fresh
                 * unopened round and drains at the next green click.
                 */
                log.info("{} maintenance: post-combat first-aid queue open skipped, nothing collected scope={} source={}",
                        logPrefix(context), scopeKey, source);
                return false;
            }
            boolean wasOpen = state.open;
            state.open = true;
            if (!wasOpen) {
                state.openedAtMs = System.currentTimeMillis();
                state.openSource = source;
                log.info("{} maintenance: post-combat first-aid queue opened scope={} queueSize={} reported={} source={}",
                        logPrefix(context), scopeKey, state.fifo.size(), state.reportsByWindow.size(), source);
            }
            return !state.fifo.isEmpty();
        }
    }

    /**
     * CR243: true when the queue is open and the current window owns the FIFO head, i.e. this
     * window may enter the fair task-turn queue and run exactly one cached first-aid attempt.
     */
    public boolean isPostCombatFirstAidHeadWindow(TaskExecutionContext context) {
        String scopeKey = postCombatFirstAidScopeKey(context);
        String windowKey = currentWindowKey(context);
        synchronized (postCombatFirstAidMonitor) {
            PostCombatFirstAidQueueState state = postCombatFirstAidQueueByScope.get(scopeKey);
            if (state == null || !state.open) {
                return false;
            }
            PostCombatFirstAidQueueItem head = state.fifo.peekFirst();
            return head != null && head.windowKey.equals(windowKey);
        }
    }

    /** CR243: true when the current window still has a queued supply item (any position). */
    public boolean hasPostCombatFirstAidQueueItem(TaskExecutionContext context) {
        String scopeKey = postCombatFirstAidScopeKey(context);
        String windowKey = currentWindowKey(context);
        synchronized (postCombatFirstAidMonitor) {
            PostCombatFirstAidQueueState state = postCombatFirstAidQueueByScope.get(scopeKey);
            return state != null && state.fifo.stream().anyMatch(item -> item.windowKey.equals(windowKey));
        }
    }

    /**
     * CR243: dequeue the current window's supply item after one real attempt, success or not. No
     * retry is allowed; failure reasons are log-only diagnostics.
     */
    public void completePostCombatFirstAidAttempt(TaskExecutionContext context,
                                                  boolean success,
                                                  String reason) {
        String scopeKey = postCombatFirstAidScopeKey(context);
        String windowKey = currentWindowKey(context);
        synchronized (postCombatFirstAidMonitor) {
            PostCombatFirstAidQueueState state = postCombatFirstAidQueueByScope.get(scopeKey);
            if (state == null) {
                return;
            }
            boolean removed = state.fifo.removeIf(item -> item.windowKey.equals(windowKey));
            log.info("{} maintenance: post-combat first-aid attempt finished and dequeued scope={} windowKey={} success={} removed={} remaining={} reason={}",
                    logPrefix(context), scopeKey, windowKey, success, removed, state.fifo.size(), reason);
            closePostCombatFirstAidQueueIfComplete(scopeKey, state, reason);
        }
    }

    /**
     * CR243 COMPLETE gate: true only when every participating window of the local team session has
     * reported AND the FIFO is empty. A temporarily empty queue with unreported participants stays
     * DRAINING. When complete, the round queue itself is closed (removed) so the next combat round
     * collects fresh reports.
     */
    public boolean isPostCombatFirstAidQueueCompleteAndClose(TaskExecutionContext context, String source) {
        String scopeKey = postCombatFirstAidScopeKey(context);
        synchronized (postCombatFirstAidMonitor) {
            PostCombatFirstAidQueueState state = postCombatFirstAidQueueByScope.get(scopeKey);
            if (state == null) {
                return true;
            }
            return closePostCombatFirstAidQueueIfComplete(scopeKey, state, source);
        }
    }

    /**
     * Closes an open first-aid round as soon as all fixed participants have reported and no queued
     * supply item remains. This is intentionally queue-owned background bookkeeping: the leader
     * must not poll the queue from its tracker-pathing phase to make this happen.
     */
    private boolean closePostCombatFirstAidQueueIfComplete(String scopeKey,
                                                           PostCombatFirstAidQueueState state,
                                                           String source) {
        if (state == null || !state.open || !state.fifo.isEmpty()) {
            return false;
        }
        Set<String> participants = postCombatFirstAidParticipants(state);
        Set<String> unreported = new HashSet<>(participants);
        unreported.removeAll(state.reportsByWindow.keySet());
        if (!unreported.isEmpty()) {
            log.info("maintenance: post-combat first-aid queue still DRAINING, unreported participants scope={} unreported={} reported={} source={}",
                    scopeKey, unreported, state.reportsByWindow.keySet(), source);
            return false;
        }
        postCombatFirstAidQueueByScope.remove(scopeKey, state);
        log.info("maintenance: post-combat first-aid queue COMPLETE and closed scope={} reported={} source={}",
                scopeKey, state.reportsByWindow.keySet(), source);
        return true;
    }

    /**
     * CR243: an explicit lifecycle exit (runner shutdown / queue-finished) drops the window's
     * report and supply item and marks it departed, so a dead window can neither block the FIFO
     * head nor hold the COMPLETE barrier. This is the ONLY sanctioned way a living participant
     * leaves the barrier; there is no timeout path.
     */
    public void clearPostCombatFirstAidForWindow(String windowKey, String sourceTask) {
        if (windowKey == null || windowKey.isBlank()) {
            return;
        }
        synchronized (postCombatFirstAidMonitor) {
            for (Map.Entry<String, PostCombatFirstAidQueueState> entry : postCombatFirstAidQueueByScope.entrySet()) {
                PostCombatFirstAidQueueState state = entry.getValue();
                boolean removedItem = state.fifo.removeIf(item -> item.windowKey.equals(windowKey));
                boolean removedReport = state.reportsByWindow.remove(windowKey) != null;
                boolean departed = state.departedWindows.add(windowKey);
                if (removedItem || removedReport || departed) {
                    log.info("maintenance: post-combat first-aid cleared for inactive window scope={} windowKey={} removedItem={} removedReport={} departed={} source={}",
                            entry.getKey(), windowKey, removedItem, removedReport, departed, sourceTask);
                }
            }
        }
    }

    private static final String POST_COMBAT_FIRST_AID_GROUP_SCOPE_SEPARATOR = "#group:";

    /**
     * CR243 review P1-1: the queue scope is the tooltip-confirmed real team grouping, never the
     * whole launch session. A window without a confirmed group stays in its own window-scoped
     * queue so unrelated teams cannot block or dequeue each other.
     */
    private String postCombatFirstAidScopeKey(TaskExecutionContext context) {
        String sessionKey = context == null ? null : context.getLocalTeamSessionKey();
        String windowKey = currentWindowKey(context);
        if (sessionKey != null && !sessionKey.isBlank()) {
            LocalTeamSessionState session = localTeamSessions.get(sessionKey);
            String groupHash = session == null
                    ? null
                    : resolvePostCombatFirstAidGroupHash(session, windowKey);
            if (groupHash != null && !groupHash.isBlank()) {
                return sessionKey + POST_COMBAT_FIRST_AID_GROUP_SCOPE_SEPARATOR + groupHash;
            }
        }
        return "window:" + windowKey;
    }

    private String resolvePostCombatFirstAidGroupHash(LocalTeamSessionState session, String windowKey) {
        if (session == null || windowKey == null || windowKey.isBlank()) {
            return null;
        }
        String direct = session.windowTooltipGroupHash.get(windowKey);
        if (direct != null && !direct.isBlank() && session.tooltipGroupsByHash.containsKey(direct)) {
            return direct;
        }
        // The leader window is not always present in windowTooltipGroupHash; resolve through the
        // group snapshots themselves. Ambiguity (window claimed by more than one group) refuses a
        // group scope rather than guessing.
        String matched = null;
        for (Map.Entry<String, LocalTeamTooltipGroup> entry : session.tooltipGroupsByHash.entrySet()) {
            LocalTeamTooltipGroup group = entry.getValue();
            if (group == null) {
                continue;
            }
            if (windowKey.equals(group.leaderWindowId) || group.memberWindowIds.contains(windowKey)) {
                if (matched != null && !matched.equals(entry.getKey())) {
                    return null;
                }
                matched = entry.getKey();
            }
        }
        return matched;
    }

    /**
     * CR243 follow-up review P1: the participant snapshot copied at the first report of the round
     * is the ONLY membership source for COMPLETE. No live tooltip-group read may move the barrier;
     * only departedWindows (explicit runner lifecycle exits) shrink it.
     */
    private Set<String> postCombatFirstAidParticipants(PostCombatFirstAidQueueState state) {
        Set<String> participants = new HashSet<>(state.participantSnapshot);
        participants.removeAll(state.departedWindows);
        return participants;
    }

    /**
     * Resolve the immutable participant snapshot at round creation: the confirmed tooltip group's
     * members plus its leader for group scopes, or just the reporting window for window scopes.
     * The first reporter is always included — it is definitionally a participant.
     */
    private Set<String> resolvePostCombatFirstAidParticipantSnapshot(String scopeKey, String firstReporterWindowKey) {
        Set<String> participants = new HashSet<>();
        if (firstReporterWindowKey != null && !firstReporterWindowKey.isBlank()) {
            participants.add(firstReporterWindowKey);
        }
        int groupIndex = scopeKey.indexOf(POST_COMBAT_FIRST_AID_GROUP_SCOPE_SEPARATOR);
        if (groupIndex < 0) {
            return participants;
        }
        String sessionKey = scopeKey.substring(0, groupIndex);
        String groupHash = scopeKey.substring(groupIndex + POST_COMBAT_FIRST_AID_GROUP_SCOPE_SEPARATOR.length());
        LocalTeamSessionState session = localTeamSessions.get(sessionKey);
        LocalTeamTooltipGroup group = session == null ? null : session.tooltipGroupsByHash.get(groupHash);
        if (group == null) {
            log.warn("maintenance: post-combat first-aid snapshot falls back to first reporter, group missing at fix time scope={} firstReporter={}",
                    scopeKey, firstReporterWindowKey);
            return participants;
        }
        participants.addAll(group.memberWindowIds);
        if (group.leaderWindowId != null && !group.leaderWindowId.isBlank()) {
            participants.add(group.leaderWindowId);
        }
        return participants;
    }

    private static final class PostCombatFirstAidQueueItem {
        private final String windowKey;
        private final boolean leader;
        private final long enqueuedAt;
        private final String source;

        private PostCombatFirstAidQueueItem(String windowKey, boolean leader, long enqueuedAt, String source) {
            this.windowKey = windowKey;
            this.leader = leader;
            this.enqueuedAt = enqueuedAt;
            this.source = source;
        }
    }

    private static final class PostCombatFirstAidQueueState {
        private final Map<String, String> reportsByWindow = new LinkedHashMap<>();
        private final Deque<PostCombatFirstAidQueueItem> fifo = new ArrayDeque<>();
        /** CR243 follow-up P1: immutable round membership fixed at the first report. */
        private final Set<String> participantSnapshot = new HashSet<>();
        /** CR243 review P1-2: explicit lifecycle exits only (runner shutdown/queue-finished). */
        private final Set<String> departedWindows = new HashSet<>();
        private boolean open;
        private long openedAtMs;
        private String openSource;
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
        long openedAt = System.currentTimeMillis();
        TeamMaintenanceWindowState previous = teamMaintenanceWindowStateByRound.put(
                roundKey, TeamMaintenanceWindowState.PATHING_WINDOW_OPEN);
        // A task round can submit more than one green link (for example, WUBEI probe retries).
        // The first real pathing snapshot is this round's queue boundary; a later link in the same
        // round must not turn already-queued summon-skill work into a consumable "next" window.
        Long existingSnapshotOpenedAt = maintenanceSnapshotOpenedAtByRound.putIfAbsent(roundKey, openedAt);
        long snapshotOpenedAt = existingSnapshotOpenedAt == null ? openedAt : existingSnapshotOpenedAt;
        openLocalTeamSupportCapability(context, TeamSupportCapability.FIRST_AID, sourceTask);
        openLocalTeamSupportCapability(context, TeamSupportCapability.PATHING_WINDOW, sourceTask);
        openLocalTeamSupportCapability(context, TeamSupportCapability.COMMON_BOX, sourceTask);
        openLocalTeamSupportCapability(context, TeamSupportCapability.SUMMON_SKILL, sourceTask);
        openLocalTeamSupportCapability(context, TeamSupportCapability.LEFT_TOP_STATUS, sourceTask);
        log.info("{} maintenance team pathing window opened: teamRound={} previous={} source={} snapshotOpenedAt={} snapshotCreated={}",
                logPrefix(context), roundKey, previous, sourceTask, snapshotOpenedAt,
                existingSnapshotOpenedAt == null);
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
        maintenanceSnapshotOpenedAtByRound.remove(roundKey);
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
     * CR244: member-side applicability of the session pending-return set.
     *
     * <p>The set only coordinates confirmed local leader-follower relations. A window may add itself
     * only when its own tooltip group proves a locally controlled leader window that is not itself.
     * Candidate-only sessions, external leaders, all-member/all-leader batches, and ambiguous
     * windows are all not applicable and must keep their standalone return behavior.</p>
     */
    public record TeamReturnCoordination(boolean applicable, String sessionKey, String leaderWindowId) {
        private static final TeamReturnCoordination NOT_APPLICABLE =
                new TeamReturnCoordination(false, null, null);
    }

    /**
     * Resolve whether this member window belongs to a confirmed local leader for pending-return
     * coordination. Paused leaders keep the relation: member self-checks must not stop while the
     * leader window is paused.
     *
     * @param context current member task context.
     * @return applicable coordination with session key and leader window id, or not-applicable.
     */
    public TeamReturnCoordination resolveTeamReturnCoordination(TaskExecutionContext context) {
        LocalTeamLeaderGroupMatch match = resolveLocalControlledLeaderGroup(context, false, true);
        if (!match.matched || match.state == null || match.group == null || match.windowId == null) {
            return TeamReturnCoordination.NOT_APPLICABLE;
        }
        String leaderWindowId = normalizeText(match.group.leaderWindowId);
        if (leaderWindowId == null || leaderWindowId.equals(match.windowId)) {
            return TeamReturnCoordination.NOT_APPLICABLE;
        }
        return new TeamReturnCoordination(true, context.getLocalTeamSessionKey(), leaderWindowId);
    }

    /**
     * CR244 member self-report: this window currently sees its own return marker. Idempotent; only a
     * real set change publishes {@link WindowReadyEventType#TEAM_RETURN_STATE_CHANGED} to the leader.
     *
     * @param context current member task context; must resolve an applicable coordination.
     * @param sourceTask diagnostic source written to logs.
     */
    public void markPendingTeamReturnWindow(TaskExecutionContext context, String sourceTask) {
        TeamReturnCoordination coordination = resolveTeamReturnCoordination(context);
        if (!coordination.applicable()) {
            return;
        }
        LocalTeamSessionState state = localTeamSessions.get(coordination.sessionKey());
        String windowId = normalizeText(context.getWindowId());
        if (state == null || windowId == null || isCompletedLocalTeamSession(coordination.sessionKey())) {
            return;
        }
        if (state.pendingReturnWindowIds.add(windowId)) {
            publishTeamReturnStateChanged(coordination.sessionKey(), coordination.leaderWindowId(),
                    windowId, "add", sourceTask, state.pendingReturnWindowIds);
        }
    }

    /**
     * CR244 member self-report: this window confirmed its return marker is gone (or it can no longer
     * belong to any leader), so its pending-return entry must not block the leader anymore.
     *
     * <p>Unlike {@link #markPendingTeamReturnWindow}, removal deliberately does not require a live
     * group match: a window whose attribution degraded while still inside the set must still be able
     * to clear its own stale entry.</p>
     *
     * @param context current member task context.
     * @param sourceTask diagnostic source written to logs.
     */
    public void clearPendingTeamReturnWindow(TaskExecutionContext context, String sourceTask) {
        if (context == null || !context.hasLocalTeamSession()) {
            return;
        }
        String sessionKey = context.getLocalTeamSessionKey();
        LocalTeamSessionState state = localTeamSessions.get(sessionKey);
        String windowId = normalizeText(context.getWindowId());
        if (state == null || windowId == null) {
            return;
        }
        if (state.pendingReturnWindowIds.remove(windowId)) {
            String leaderWindowId = normalizeText(state.leaderWindowId) != null
                    ? state.leaderWindowId
                    : state.knownLeaderWindowId;
            publishTeamReturnStateChanged(sessionKey, leaderWindowId,
                    windowId, "remove", sourceTask, state.pendingReturnWindowIds);
        }
    }

    /**
     * CR244 leader-side read for Gate A / Gate B. Pure read: the leader never mutates the set and
     * never scans member windows here.
     *
     * @param context current leader task context. Non-leader windows always read zero so they can
     *                never park on another window's facts.
     * @return number of member windows still pending return in this leader's session.
     */
    public int pendingTeamReturnWindowCount(TaskExecutionContext context) {
        if (context == null || !context.hasLocalTeamSession()
                || isCompletedLocalTeamSession(context.getLocalTeamSessionKey())) {
            return 0;
        }
        LocalTeamSessionState state = localTeamSessions.get(context.getLocalTeamSessionKey());
        String windowId = normalizeText(context.getWindowId());
        if (state == null || windowId == null) {
            return 0;
        }
        boolean isLeaderWindow = windowId.equals(state.leaderWindowId)
                || windowId.equals(state.knownLeaderWindowId);
        if (!isLeaderWindow) {
            return 0;
        }
        return state.pendingReturnWindowIds.size();
    }

    private void publishTeamReturnStateChanged(String sessionKey,
                                               String leaderWindowId,
                                               String memberWindowId,
                                               String action,
                                               String sourceTask,
                                               Set<String> pendingSnapshot) {
        String pending = String.join(",", pendingSnapshot);
        log.info("maintenance team-return pending set changed: session={} action={} memberWindow={} leaderWindow={} pending=[{}] source={}",
                sessionKey, action, memberWindowId, leaderWindowId, pending, sourceTask);
        if (windowReadyEventBus == null || leaderWindowId == null || leaderWindowId.isBlank()) {
            return;
        }
        windowReadyEventBus.publish(WindowReadyEvent.builder()
                .windowId(leaderWindowId)
                .type(WindowReadyEventType.TEAM_RETURN_STATE_CHANGED)
                .source("team-return-state:" + sessionKey + ":" + action + ":" + memberWindowId)
                .targetKeyword(sessionKey)
                .summary("pending=[" + pending + "]")
                .build());
    }

    /**
     * CR245: leader-opened maintenance broadcast FIFO. The queue existing IS the members'
     * authorization to run one broadcast confirm attempt each; one attempt (hit, miss, or failed
     * click) always dequeues so a missing dialog can never wedge the queue.
     */
    /**
     * CR252: one leader-confirmed team combat phase per real tooltip group. Created only after the
     * leader physically entered battle via a confirmed enter-battle action AND confirmed its own
     * combat entry; removed (without an exit broadcast) on leader pause/stop/session end, or
     * replaced by the next round's entry. Members bound to the leader consume this instead of
     * running their own battle-template radar.
     */
    private static final class TeamCombatPhaseState {
        private final String sessionKey;
        private final String groupHash;
        private final String leaderWindowId;
        private final long epochId;
        private final long enteredAtMs;
        private final String openSource;
        private volatile boolean exited;
        private volatile long exitedAtMs;

        private TeamCombatPhaseState(String sessionKey, String groupHash, String leaderWindowId,
                                     long epochId, long enteredAtMs, String openSource) {
            this.sessionKey = sessionKey;
            this.groupHash = groupHash;
            this.leaderWindowId = leaderWindowId;
            this.epochId = epochId;
            this.enteredAtMs = enteredAtMs;
            this.openSource = openSource;
        }
    }

    /**
     * CR252 member-side view of the bound leader's team combat phase.
     *
     * @param covered true when this window is a member bound to a locally-controlled, non-paused
     *                leader — regardless of whether a combat-phase broadcast exists yet. Covered
     *                members never run their own battle-template radar outside combat; they wait
     *                quietly for the leader's entry broadcast (CR252 review P1).
     * @param present true only when the leader's current combat-phase broadcast is live.
     * @param inCombat true while the leader-confirmed round battle is running; false once the
     *                 leader confirmed the round's final exit.
     * @param leaderPaused true when this window IS bound to a locally-controlled leader but that
     *                     leader is currently paused (CR252 review P1 round 3): the member may only
     *                     READ its own combat state — no enter-signal consumption, no entry
     *                     maintenance, no auto-combat panel input.
     */
    public record MemberTeamCombatPhaseView(boolean covered, boolean present, boolean inCombat,
                                            long epochId, String leaderWindowId,
                                            boolean leaderPaused) {
        private static final MemberTeamCombatPhaseView ABSENT =
                new MemberTeamCombatPhaseView(false, false, false, 0L, null, false);

        public static MemberTeamCombatPhaseView absent() {
            return ABSENT;
        }
    }

    private final Map<String, TeamCombatPhaseState> teamCombatPhaseByScope = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong teamCombatPhaseEpochSeq =
            new java.util.concurrent.atomic.AtomicLong();

    /**
     * CR252 leader-side: the leader confirmed its own combat entry for this round. Opens (or
     * replaces) the team combat phase for every real tooltip group this window leads locally.
     * Non-leader windows, external-leader groups, and paused leaders never open a phase; with no
     * confirmed group nothing is broadcast (never batch-wide).
     *
     * @return number of groups the phase was opened for (0 = nothing broadcast).
     */
    public int openTeamCombatPhaseForLeader(TaskExecutionContext context, String sourceTask) {
        if (context == null || !context.hasLocalTeamSession()
                || isCompletedLocalTeamSession(context.getLocalTeamSessionKey())) {
            return 0;
        }
        String sessionKey = context.getLocalTeamSessionKey();
        LocalTeamSessionState state = localTeamSessions.get(sessionKey);
        String windowId = normalizeText(context.getWindowId());
        if (state == null || windowId == null) {
            return 0;
        }
        int opened = 0;
        synchronized (state) {
            for (LocalTeamTooltipGroup group : state.tooltipGroupsByHash.values()) {
                if (group == null || !group.localLeaderControlled || group.leaderPaused
                        || group.leaderWindowId == null || !group.leaderWindowId.equals(windowId)
                        || group.groupHash == null) {
                    continue;
                }
                String scope = teamCombatPhaseScopeKey(sessionKey, group.groupHash);
                TeamCombatPhaseState existing = teamCombatPhaseByScope.get(scope);
                if (existing != null && !existing.exited && existing.leaderWindowId.equals(windowId)) {
                    opened++;
                    continue;
                }
                TeamCombatPhaseState phase = new TeamCombatPhaseState(sessionKey, group.groupHash,
                        windowId, teamCombatPhaseEpochSeq.incrementAndGet(),
                        System.currentTimeMillis(), sourceTask);
                teamCombatPhaseByScope.put(scope, phase);
                opened++;
                log.info("team combat phase opened: session={} group={} leaderWindow={} epoch={} source={}",
                        sessionKey, group.groupHash, windowId, phase.epochId, sourceTask);
            }
        }
        return opened;
    }

    /**
     * CR252 leader-side: a task-owned combat exit passed the existing correction chain (for example
     * return-home verification or a confirmed 黄袍 post-battle boundary). Marks every phase this
     * window leads as exited so bound members apply the FREE verdict once through their normal exit
     * machinery. Candidate quick-exits must NOT call this.
     */
    public void confirmTeamCombatPhaseExitedForLeader(TaskExecutionContext context, String sourceTask) {
        String windowId = context == null ? null : normalizeText(context.getWindowId());
        if (windowId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (TeamCombatPhaseState phase : teamCombatPhaseByScope.values()) {
            if (phase != null && !phase.exited && windowId.equals(phase.leaderWindowId)) {
                phase.exited = true;
                phase.exitedAtMs = now;
                log.info("team combat phase exit confirmed: session={} group={} leaderWindow={} epoch={} source={}",
                        phase.sessionKey, phase.groupHash, windowId, phase.epochId, sourceTask);
            }
        }
    }

    /**
     * CR252: hard-invalidate every phase owned by this leader WITHOUT an exit broadcast. Used for
     * leader pause/stop/session end and stale-phase resets: bound members fall back to their own
     * battle radar; the fallback itself must not synthesize an exit or drive any input.
     */
    public void invalidateTeamCombatPhaseForLeader(String leaderWindowId, String reason) {
        String normalized = normalizeText(leaderWindowId);
        if (normalized == null) {
            return;
        }
        teamCombatPhaseByScope.entrySet().removeIf(entry -> {
            TeamCombatPhaseState phase = entry.getValue();
            if (phase == null || !normalized.equals(phase.leaderWindowId)) {
                return false;
            }
            log.info("team combat phase invalidated: session={} group={} leaderWindow={} epoch={} exited={} reason={}",
                    phase.sessionKey, phase.groupHash, normalized, phase.epochId, phase.exited, reason);
            return true;
        });
    }

    /**
     * CR252 member-side read: the bound local leader's live combat phase for this window's real
     * tooltip group. Absent for unbound/independent windows, external leaders, unknown binding,
     * paused leaders, and leaders that never confirmed entry this round — those windows keep their
     * own battle radar.
     */
    public MemberTeamCombatPhaseView memberTeamCombatPhase(TaskExecutionContext context) {
        LocalTeamLeaderGroupMatch match = resolveLocalControlledLeaderGroup(context, false, true);
        if (!match.matched || match.group == null || match.group.groupHash == null
                || match.group.leaderWindowId == null
                || match.windowId == null || match.windowId.equals(match.group.leaderWindowId)) {
            return MemberTeamCombatPhaseView.ABSENT;
        }
        if (match.group.leaderPaused) {
            // CR252 review P1 round 3: still bound, but the leader is paused — read-only fallback.
            return new MemberTeamCombatPhaseView(false, false, false, 0L,
                    match.group.leaderWindowId, true);
        }
        String scope = teamCombatPhaseScopeKey(context.getLocalTeamSessionKey(), match.group.groupHash);
        TeamCombatPhaseState phase = teamCombatPhaseByScope.get(scope);
        if (phase == null || !phase.leaderWindowId.equals(match.group.leaderWindowId)) {
            // CR252 review P1: bound to a live local leader but no broadcast yet — covered, waiting.
            return new MemberTeamCombatPhaseView(true, false, false, 0L,
                    match.group.leaderWindowId, false);
        }
        return new MemberTeamCombatPhaseView(true, true, !phase.exited, phase.epochId,
                phase.leaderWindowId, false);
    }

    private static String teamCombatPhaseScopeKey(String sessionKey, String groupHash) {
        return sessionKey + "#combat-phase-group:" + groupHash;
    }

    private static final class MaintenanceBroadcastQueueState {
        private final String label;
        private final String leaderWindowId;
        private final long openedAtMs;
        private final long deadlineAtMs;
        private final Deque<String> fifo = new ArrayDeque<>();

        private MaintenanceBroadcastQueueState(String label,
                                               String leaderWindowId,
                                               long openedAtMs,
                                               long deadlineAtMs) {
            this.label = label;
            this.leaderWindowId = leaderWindowId;
            this.openedAtMs = openedAtMs;
            this.deadlineAtMs = deadlineAtMs;
        }
    }

    private final Object maintenanceBroadcastQueueMonitor = new Object();
    private final Map<String, MaintenanceBroadcastQueueState> maintenanceBroadcastQueueByScope =
            new ConcurrentHashMap<>();

    /**
     * CR245: open (or replace) this leader session's maintenance broadcast queue with the confirmed
     * local member windows frozen at this moment. Candidate-only, external-leader, or ambiguous
     * windows never enter; heal-pet and repair-equipment reuse the structure via {@code label}.
     *
     * @param context leader task context; must be the session's leader window.
     * @param label maintenance kind for logs/keys, e.g. {@code heal-pet}.
     * @param capMs insurance deadline from now; reaching it releases the leader.
     * @param sourceTask diagnostic source written to logs.
     * @return number of queued member windows; 0 means no queue was created and the caller should
     *         use the no-local-member courtesy path.
     */
    public int openMaintenanceBroadcastQueue(TaskExecutionContext context,
                                             String label,
                                             long capMs,
                                             String sourceTask) {
        if (context == null || !context.hasLocalTeamSession()
                || isCompletedLocalTeamSession(context.getLocalTeamSessionKey())) {
            return 0;
        }
        String sessionKey = context.getLocalTeamSessionKey();
        LocalTeamSessionState state = localTeamSessions.get(sessionKey);
        String windowId = normalizeText(context.getWindowId());
        if (state == null || windowId == null) {
            return 0;
        }
        boolean isLeaderWindow = windowId.equals(state.leaderWindowId)
                || windowId.equals(state.knownLeaderWindowId);
        if (!isLeaderWindow) {
            return 0;
        }
        Set<String> members = new HashSet<>();
        synchronized (state) {
            for (LocalTeamTooltipGroup group : state.tooltipGroupsByHash.values()) {
                if (group == null || !group.localLeaderControlled
                        || group.leaderWindowId == null || !group.leaderWindowId.equals(windowId)) {
                    continue;
                }
                for (String memberWindowId : group.memberWindowIds) {
                    if (memberWindowId != null && !memberWindowId.isBlank()
                            && !memberWindowId.equals(windowId)
                            && !state.completedWindows.contains(memberWindowId)) {
                        members.add(memberWindowId);
                    }
                }
            }
        }
        if (members.isEmpty()) {
            log.info("maintenance broadcast queue not created: no confirmed local members session={} label={} leaderWindow={} source={}",
                    sessionKey, label, windowId, sourceTask);
            maintenanceBroadcastQueueByScope.remove(sessionKey);
            return 0;
        }
        long now = System.currentTimeMillis();
        MaintenanceBroadcastQueueState queue = new MaintenanceBroadcastQueueState(
                label, windowId, now, now + Math.max(1000L, capMs));
        synchronized (maintenanceBroadcastQueueMonitor) {
            queue.fifo.addAll(members);
            maintenanceBroadcastQueueByScope.put(sessionKey, queue);
        }
        log.info("maintenance broadcast queue opened: session={} label={} leaderWindow={} members={} capMs={} source={}",
                sessionKey, label, windowId, members, Math.max(1000L, capMs), sourceTask);
        return members.size();
    }

    /**
     * CR245 member-side consumption: when this window is at the head of its session's open queue,
     * run exactly one broadcast confirm attempt (fixed ROI scan + click) and dequeue unconditionally.
     * The queue's existence is the authorization, so this path deliberately bypasses the idle
     * quiet-member suppression; opportunistic idle scanning stays suppressed elsewhere (CR237).
     *
     * @param context current member task context.
     * @param sourceTask diagnostic source written to logs.
     * @return true when a queue turn was consumed (attempted + dequeued) this tick.
     */
    public boolean consumeMaintenanceBroadcastQueueTurnIfHead(TaskExecutionContext context,
                                                              String sourceTask) {
        if (context == null || !context.hasLocalTeamSession()) {
            return false;
        }
        String sessionKey = context.getLocalTeamSessionKey();
        String windowId = normalizeText(context.getWindowId());
        if (windowId == null) {
            return false;
        }
        MaintenanceBroadcastQueueState queue;
        synchronized (maintenanceBroadcastQueueMonitor) {
            queue = maintenanceBroadcastQueueByScope.get(sessionKey);
            if (queue == null) {
                return false;
            }
            if (System.currentTimeMillis() > queue.deadlineAtMs) {
                maintenanceBroadcastQueueByScope.remove(sessionKey, queue);
                publishMaintenanceBroadcastQueueChanged(sessionKey, queue, windowId,
                        "expired-on-member-tick", sourceTask);
                return false;
            }
            if (!windowId.equals(queue.fifo.peekFirst())) {
                return false;
            }
        }
        TaskMaintenanceResult attempt = handleMaintenanceBroadcast(context, TaskMaintenanceRequest.builder()
                .sourceTask(sourceTask + ":queue-head:" + queue.label)
                .handleMaintenanceBroadcast(true)
                .build());
        synchronized (maintenanceBroadcastQueueMonitor) {
            MaintenanceBroadcastQueueState current = maintenanceBroadcastQueueByScope.get(sessionKey);
            if (current == queue) {
                queue.fifo.remove(windowId);
            }
        }
        log.info("maintenance broadcast queue turn consumed: session={} label={} memberWindow={} attemptStatus={} attemptMessage={} remaining={} source={}",
                sessionKey, queue.label, windowId, attempt.getStatus(), attempt.getMessage(),
                queue.fifo.size(), sourceTask);
        publishMaintenanceBroadcastQueueChanged(sessionKey, queue, windowId, "dequeued", sourceTask);
        return true;
    }

    /**
     * CR245 leader-side gate: drained means the queue is gone, empty, or past its insurance
     * deadline. Unlike CR244's timeout, reaching the deadline here IS a release by design.
     *
     * @param context leader task context.
     * @param sourceTask diagnostic source written to logs.
     * @return true when the leader may click its own confirm and continue.
     */
    public boolean isMaintenanceBroadcastQueueDrained(TaskExecutionContext context, String sourceTask) {
        if (context == null || !context.hasLocalTeamSession()) {
            return true;
        }
        String sessionKey = context.getLocalTeamSessionKey();
        synchronized (maintenanceBroadcastQueueMonitor) {
            MaintenanceBroadcastQueueState queue = maintenanceBroadcastQueueByScope.get(sessionKey);
            if (queue == null) {
                return true;
            }
            long now = System.currentTimeMillis();
            if (queue.fifo.isEmpty() || now > queue.deadlineAtMs) {
                maintenanceBroadcastQueueByScope.remove(sessionKey, queue);
                log.info("maintenance broadcast queue drained: session={} label={} empty={} expired={} remaining={} elapsedMs={} source={}",
                        sessionKey, queue.label, queue.fifo.isEmpty(), now > queue.deadlineAtMs,
                        queue.fifo, Math.max(0L, now - queue.openedAtMs), sourceTask);
                return true;
            }
            return false;
        }
    }

    /**
     * @param context member task context.
     * @return remaining insurance time when this window is inside its session's open queue, else -1.
     *         Used by member loops to switch to the fast poll interval while queued.
     */
    public boolean isInOpenMaintenanceBroadcastQueue(TaskExecutionContext context) {
        if (context == null || !context.hasLocalTeamSession()) {
            return false;
        }
        String windowId = normalizeText(context.getWindowId());
        MaintenanceBroadcastQueueState queue = maintenanceBroadcastQueueByScope.get(context.getLocalTeamSessionKey());
        return queue != null
                && windowId != null
                && System.currentTimeMillis() <= queue.deadlineAtMs
                && queue.fifo.contains(windowId);
    }

    private void publishMaintenanceBroadcastQueueChanged(String sessionKey,
                                                         MaintenanceBroadcastQueueState queue,
                                                         String memberWindowId,
                                                         String action,
                                                         String sourceTask) {
        if (windowReadyEventBus == null || queue == null
                || queue.leaderWindowId == null || queue.leaderWindowId.isBlank()) {
            return;
        }
        windowReadyEventBus.publish(WindowReadyEvent.builder()
                .windowId(queue.leaderWindowId)
                .type(WindowReadyEventType.MAINTENANCE_BROADCAST_QUEUE_CHANGED)
                .source("maintenance-broadcast-queue:" + sessionKey + ":" + queue.label
                        + ":" + action + ":" + memberWindowId)
                .targetKeyword(sessionKey)
                .summary("remaining=[" + String.join(",", queue.fifo) + "]")
                .build());
    }

    /**
     * CR245: probe the maintenance broadcast confirm point on the CURRENT bound window without
     * clicking. Used by the leader to pre-recognize its own confirm while parked; null means not
     * found (or capture failed) and the caller must fall back to a live scan.
     */
    public Point probeMaintenanceBroadcastPointForCurrentWindow(String sourceTask) {
        if (coordinateHelper == null) {
            return null;
        }
        int[] rect = coordinateHelper.getScaledRect(
                MAINTENANCE_BROADCAST_ROI_LEFT,
                MAINTENANCE_BROADCAST_ROI_TOP,
                MAINTENANCE_BROADCAST_ROI_RIGHT - MAINTENANCE_BROADCAST_ROI_LEFT,
                MAINTENANCE_BROADCAST_ROI_BOTTOM - MAINTENANCE_BROADCAST_ROI_TOP);
        String[] templatePaths = {
                MAINTENANCE_HEAL_ALL_REPAIR_TEMPLATE,
                MAINTENANCE_REPAIR_CONFIRM_TEMPLATE
        };
        for (String templatePath : templatePaths) {
            Point match = coordinateHelper.findImageInRegion(
                    templatePath, rect, MAINTENANCE_BROADCAST_TEMPLATE_THRESHOLD);
            if (match != null) {
                log.info("maintenance broadcast self-confirm probe matched: template={} point=({}, {}) source={}",
                        templatePath, match.x, match.y, sourceTask);
                return match;
            }
        }
        return null;
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
     * @param context current task context.
     * @return true only for local member support workers that belong to a runner-created session.
     */
    public boolean isLocalSupportMemberSession(TaskExecutionContext context) {
        return resolveLocalControlledLeaderGroup(context, true).matched;
    }

    /**
     * @param context current member context.
     * @return true when this member belongs to a known local team whose local leader is currently
     *         paused. In this mode members may run first-aid and maintenance-broadcast checks as if
     *         the leader were manually controlled, but summon-skill cleanup must stay closed.
     */
    public boolean isLocalTeamLeaderPausedForMember(TaskExecutionContext context) {
        LocalTeamLeaderGroupMatch match = resolveLocalControlledLeaderGroup(context, true, true);
        return match.matched && match.group != null && match.group.leaderPaused;
    }

    /**
     * Update local-team pause state when the UI pauses or resumes the leader window.
     *
     * @param leaderWindowId local window id for the paused/resumed leader.
     * @param paused true after pause, false after resume.
     * @param sourceTask diagnostic source written to logs.
     */
    public void markLocalTeamLeaderPaused(String leaderWindowId, boolean paused, String sourceTask) {
        String normalizedLeaderWindowId = normalizeText(leaderWindowId);
        if (normalizedLeaderWindowId == null) {
            return;
        }
        if (paused) {
            // CR252: leader pause is a hard invalidation event for this round's team combat phase.
            // Members fall back to their own battle radar; resume must NOT reuse the old broadcast —
            // only the next real enter-battle confirmation re-establishes leader coverage.
            invalidateTeamCombatPhaseForLeader(normalizedLeaderWindowId, "leader-paused:" + sourceTask);
        }
        for (Map.Entry<String, LocalTeamSessionState> entry : localTeamSessions.entrySet()) {
            String sessionKey = entry.getKey();
            LocalTeamSessionState state = entry.getValue();
            if (state == null || isCompletedLocalTeamSession(sessionKey)) {
                continue;
            }
            boolean changed = false;
            synchronized (state) {
                for (LocalTeamTooltipGroup group : state.tooltipGroupsByHash.values()) {
                    if (group != null
                            && normalizedLeaderWindowId.equals(group.leaderWindowId)
                            && group.localLeaderControlled
                            && group.leaderPaused != paused) {
                        group.leaderPaused = paused;
                        changed = true;
                    }
                }
                if ((normalizedLeaderWindowId.equals(state.leaderWindowId)
                        || normalizedLeaderWindowId.equals(state.knownLeaderWindowId))
                        && state.localLeaderPaused != paused) {
                    state.localLeaderPaused = paused;
                    changed = true;
                }
                if (changed) {
                    state.idleBroadcastSuppressCacheByWindow.clear();
                }
            }
            if (changed) {
                log.info("maintenance local-team leader pause state changed: session={} leaderWindow={} paused={} source={}",
                        sessionKey, normalizedLeaderWindowId, paused, sourceTask);
            }
        }
    }

    /**
     * Attach a separately started member window to an already-running local-team session when the
     * tooltip leader ID uniquely matches a local leader controlled by this process.
     *
     * @param windowId restarted/late-started member window id.
     * @param playerId current player id from the member tooltip request.
     * @param groupHash local tooltip group signature for this team tooltip.
     * @param leaderPlayerId leader player id returned by TEAM_ROLE_TOOLTIP.
     * @param roleName live role name for diagnostics.
     * @param sourceTask diagnostic source written to logs.
     * @return attachment status and matched session metadata.
     */
    public LocalTeamSessionAttachResult attachExistingLocalTeamSessionForMember(String windowId,
                                                                               String playerId,
                                                                               String groupHash,
                                                                               String leaderPlayerId,
                                                                               String roleName,
                                                                               String sourceTask) {
        String normalizedWindowId = normalizeText(windowId);
        String normalizedPlayerId = normalizeText(playerId);
        String normalizedGroupHash = normalizeText(groupHash);
        String normalizedLeaderPlayerId = normalizeText(leaderPlayerId);
        if (normalizedWindowId == null || normalizedPlayerId == null || normalizedGroupHash == null
                || normalizedLeaderPlayerId == null) {
            log.warn("maintenance local-team late member attach skipped: status={} windowId={} playerId={} groupHash={} leaderPlayerId={} role={} source={}",
                    LocalTeamSessionAttachStatus.NO_TOOLTIP_EVIDENCE, windowId, playerId, groupHash,
                    leaderPlayerId, roleName, sourceTask);
            return LocalTeamSessionAttachResult.noTooltipEvidence();
        }

        int activeSessions = 0;
        int matches = 0;
        String matchedSessionKey = null;
        LocalTeamSessionState matchedState = null;
        String matchedLeaderWindowId = null;
        for (Map.Entry<String, LocalTeamSessionState> entry : localTeamSessions.entrySet()) {
            String sessionKey = entry.getKey();
            LocalTeamSessionState state = entry.getValue();
            if (state == null) {
                continue;
            }
            String leaderWindowId;
            synchronized (state) {
                if (localTeamSessions.get(sessionKey) != state || isCompletedLocalTeamSession(sessionKey)) {
                    continue;
                }
                activeSessions++;
                leaderWindowId = resolveLocalLeaderWindowForLateAttach(state, normalizedLeaderPlayerId);
            }
            if (leaderWindowId == null || leaderWindowId.isBlank()) {
                continue;
            }
            matches++;
            matchedSessionKey = sessionKey;
            matchedState = state;
            matchedLeaderWindowId = leaderWindowId;
        }

        if (matches == 0) {
            LocalTeamSessionAttachStatus status = activeSessions == 0
                    ? LocalTeamSessionAttachStatus.NO_ACTIVE_LOCAL_TEAM_SESSION
                    : LocalTeamSessionAttachStatus.NO_MATCHING_LOCAL_LEADER;
            log.warn("maintenance local-team late member not attached: status={} activeSessions={} windowId={} playerId={} groupHash={} leaderPlayerId={} role={} source={}",
                    status, activeSessions, normalizedWindowId, normalizedPlayerId, normalizedGroupHash,
                    normalizedLeaderPlayerId, roleName, sourceTask);
            return new LocalTeamSessionAttachResult(status, null, null);
        }
        if (matches > 1) {
            log.error("maintenance local-team late member attach ambiguous: matches={} windowId={} playerId={} groupHash={} leaderPlayerId={} role={} source={}",
                    matches, normalizedWindowId, normalizedPlayerId, normalizedGroupHash,
                    normalizedLeaderPlayerId, roleName, sourceTask);
            return LocalTeamSessionAttachResult.ambiguous();
        }

        LocalTeamTooltipGroup group;
        synchronized (matchedState) {
            if (localTeamSessions.get(matchedSessionKey) != matchedState
                    || isCompletedLocalTeamSession(matchedSessionKey)) {
                log.warn("maintenance local-team late member attach aborted: status={} session={} windowId={} playerId={} groupHash={} leaderPlayerId={} role={} source={}",
                        LocalTeamSessionAttachStatus.SESSION_COMPLETED_OR_REMOVED, matchedSessionKey,
                        normalizedWindowId, normalizedPlayerId, normalizedGroupHash,
                        normalizedLeaderPlayerId, roleName, sourceTask);
                return LocalTeamSessionAttachResult.completedOrRemoved();
            }
            matchedLeaderWindowId = resolveLocalLeaderWindowForLateAttach(
                    matchedState, normalizedLeaderPlayerId);
            if (matchedLeaderWindowId == null || matchedLeaderWindowId.isBlank()) {
                log.warn("maintenance local-team late member attach aborted: status={} session={} windowId={} playerId={} groupHash={} leaderPlayerId={} role={} source={}",
                        LocalTeamSessionAttachStatus.NO_MATCHING_LOCAL_LEADER, matchedSessionKey,
                        normalizedWindowId, normalizedPlayerId, normalizedGroupHash,
                        normalizedLeaderPlayerId, roleName, sourceTask);
                return new LocalTeamSessionAttachResult(
                        LocalTeamSessionAttachStatus.NO_MATCHING_LOCAL_LEADER, null, null);
            }
            matchedState.candidateWindows.add(normalizedWindowId);
            matchedState.completedWindows.remove(normalizedWindowId);
            matchedState.roleDetectedWindows.add(normalizedWindowId);
            recordLocalTeamPlayerIdentity(matchedState, normalizedWindowId, normalizedPlayerId);
            group = matchedState.tooltipGroupsByHash.computeIfAbsent(
                    normalizedGroupHash, ignored -> new LocalTeamTooltipGroup(normalizedGroupHash));
            group.leaderPlayerId = normalizedLeaderPlayerId;
            group.leaderWindowId = matchedLeaderWindowId;
            group.localLeaderControlled = true;
            group.externalLeader = false;
            group.memberPlayerIds.add(normalizedPlayerId);
            group.memberWindowIds.add(normalizedWindowId);
            matchedState.windowTooltipGroupHash.put(normalizedWindowId, normalizedGroupHash);
            matchedState.leaderPlayerId = normalizedLeaderPlayerId;
            matchedState.leaderWindowId = matchedLeaderWindowId;
            matchedState.knownLeaderWindowId = matchedLeaderWindowId;
            matchedState.localLeaderControlled = true;
            matchedState.externalLeader = false;
            matchedState.leaderAbsent = false;
            if (localTeamSessions.get(matchedSessionKey) != matchedState
                    || isCompletedLocalTeamSession(matchedSessionKey)) {
                log.warn("maintenance local-team late member attach discarded: status={} session={} windowId={} playerId={} groupHash={} leaderPlayerId={} role={} source={}",
                        LocalTeamSessionAttachStatus.SESSION_COMPLETED_OR_REMOVED, matchedSessionKey,
                        normalizedWindowId, normalizedPlayerId, normalizedGroupHash,
                        normalizedLeaderPlayerId, roleName, sourceTask);
                return LocalTeamSessionAttachResult.completedOrRemoved();
            }
        }
        log.info("maintenance local-team late member attached: session={} leaderPlayerId={} leaderWindow={} windowId={} playerId={} groupHash={} role={} source={} groupPlayers={} groupWindows={}",
                matchedSessionKey, normalizedLeaderPlayerId, matchedLeaderWindowId,
                normalizedWindowId, normalizedPlayerId, normalizedGroupHash, roleName, sourceTask,
                group.memberPlayerIds, group.memberWindowIds);
        return LocalTeamSessionAttachResult.attached(matchedSessionKey, matchedLeaderWindowId);
    }

    private String resolveLocalLeaderWindowForLateAttach(LocalTeamSessionState state,
                                                         String leaderPlayerId) {
        if (state == null || leaderPlayerId == null || leaderPlayerId.isBlank()) {
            return null;
        }
        String leaderWindowId = state.playerWindowIds.get(leaderPlayerId);
        if (leaderWindowId != null && !leaderWindowId.isBlank()) {
            return leaderWindowId;
        }
        for (LocalTeamTooltipGroup group : state.tooltipGroupsByHash.values()) {
            if (group != null
                    && group.localLeaderControlled
                    && leaderPlayerId.equals(group.leaderPlayerId)
                    && group.leaderWindowId != null
                    && !group.leaderWindowId.isBlank()
                    && group.leaderWindowId.equals(state.leaderWindowId)) {
                return group.leaderWindowId;
            }
        }
        return null;
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
        registerLocalTeamSessionCandidate(sessionKey, windowIds, sourceTask, null);
    }

    /**
     * Register selected windows and the UI-known leader for a local-team session.
     *
     * @param sessionKey shared local-team session id from the UI same-queue submit.
     * @param windowIds selected window ids in this submit batch.
     * @param sourceTask diagnostic source written to logs.
     * @param knownLeaderWindowId leader window id known before live role preflight; nullable.
     */
    public void registerLocalTeamSessionCandidate(String sessionKey,
                                                  Collection<String> windowIds,
                                                  String sourceTask,
                                                  String knownLeaderWindowId) {
        registerLocalTeamSessionCandidate(sessionKey, windowIds, sourceTask, knownLeaderWindowId, null);
    }

    /**
     * Register selected windows, optional submit-time leader, and known player IDs for a local-team
     * startup batch. Player IDs are the stable same-team identity; window IDs are only the current
     * runtime binding.
     *
     * @param sessionKey shared local-team session id from the UI same-queue submit.
     * @param windowIds selected window ids in this submit batch.
     * @param sourceTask diagnostic source written to logs.
     * @param knownLeaderWindowId leader window id known before live role preflight; nullable.
     * @param windowPlayerIds selected window id to account/player id map; nullable.
     */
    public void registerLocalTeamSessionCandidate(String sessionKey,
                                                  Collection<String> windowIds,
                                                  String sourceTask,
                                                  String knownLeaderWindowId,
                                                  Map<String, String> windowPlayerIds) {
        if (sessionKey == null || sessionKey.isBlank() || windowIds == null || windowIds.isEmpty()) {
            return;
        }
        if (isCompletedLocalTeamSession(sessionKey)) {
            log.warn("maintenance local-team candidate ignored for completed session: session={} windows={} source={}",
                    sessionKey, windowIds, sourceTask);
            return;
        }
        LocalTeamSessionState state = localTeamSessions.computeIfAbsent(
                sessionKey, ignored -> new LocalTeamSessionState());
        for (String windowId : windowIds) {
            if (windowId != null && !windowId.isBlank()) {
                state.candidateWindows.add(windowId);
            }
        }
        if (knownLeaderWindowId != null && !knownLeaderWindowId.isBlank()) {
            state.knownLeaderWindowId = knownLeaderWindowId;
        }
        if (windowPlayerIds != null && !windowPlayerIds.isEmpty()) {
            windowPlayerIds.forEach((windowId, playerId) -> recordLocalTeamPlayerIdentity(state, windowId, playerId));
        }
        state.leaderAbsent = false;
        log.info("maintenance local-team candidate registered: session={} windows={} playerIds={} knownLeader={} source={}",
                sessionKey, state.candidateWindows, state.windowPlayerIds, state.knownLeaderWindowId, sourceTask);
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
        if (isCompletedLocalTeamSession(context.getLocalTeamSessionKey())) {
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
        }
    }

    /**
     * Record same-team evidence for one local window based on a local tooltip group signature.
     *
     * <p>The cloud tooltip result owns only the leader player ID for one local group signature. This method
     * maps that stable player ID back to the selected local windows and decides whether the leader is
     * locally controlled. It deliberately does not infer anything from cloud-side OCR beyond the
     * returned leader ID.</p>
     *
     * @param context current runner context carrying local-team session metadata.
     * @param windowId window whose tooltip role was just detected.
     * @param playerId account/player id for {@code windowId}.
     * @param groupHash local tooltip group signature.
     * @param leaderPlayerId leader account/player id returned by cloud.
     * @param roleName derived role for diagnostics.
     * @param sourceTask diagnostic source written to logs.
     */
    public void recordLocalTeamTooltipGroup(TaskExecutionContext context,
                                            String windowId,
                                            String playerId,
                                            String groupHash,
                                            String leaderPlayerId,
                                            String roleName,
                                            String sourceTask) {
        if (context == null || !context.hasLocalTeamSession()
                || windowId == null || windowId.isBlank()
                || playerId == null || playerId.isBlank()
                || groupHash == null || groupHash.isBlank()
                || leaderPlayerId == null || leaderPlayerId.isBlank()) {
            return;
        }
        if (isCompletedLocalTeamSession(context.getLocalTeamSessionKey())) {
            return;
        }
        String sessionKey = context.getLocalTeamSessionKey();
        LocalTeamSessionState state = localTeamSessions.computeIfAbsent(
                sessionKey, ignored -> new LocalTeamSessionState());
        String normalizedWindowId = normalizeText(windowId);
        String normalizedPlayerId = normalizeText(playerId);
        String normalizedGroupHash = normalizeText(groupHash);
        String normalizedLeaderPlayerId = normalizeText(leaderPlayerId);
        LocalTeamTooltipGroup group;
        synchronized (state) {
            recordLocalTeamPlayerIdentity(state, normalizedWindowId, normalizedPlayerId);
            group = state.tooltipGroupsByHash.computeIfAbsent(
                    normalizedGroupHash, ignored -> new LocalTeamTooltipGroup(normalizedGroupHash));
            group.leaderPlayerId = normalizedLeaderPlayerId;
            group.memberPlayerIds.add(normalizedPlayerId);
            group.memberWindowIds.add(normalizedWindowId);
            state.windowTooltipGroupHash.put(normalizedWindowId, normalizedGroupHash);
            state.leaderPlayerId = normalizedLeaderPlayerId;
            String localLeaderWindowId = state.playerWindowIds.get(normalizedLeaderPlayerId);
            if (localLeaderWindowId != null && !localLeaderWindowId.isBlank()) {
                group.leaderWindowId = localLeaderWindowId;
                group.localLeaderControlled = true;
                group.externalLeader = false;
                state.localLeaderControlled = true;
                state.externalLeader = false;
                state.leaderAbsent = false;
                state.knownLeaderWindowId = localLeaderWindowId;
                if (state.leaderWindowId == null) {
                    state.leaderWindowId = localLeaderWindowId;
                }
            } else if (!state.playerWindowIds.isEmpty()) {
                group.leaderWindowId = null;
                group.localLeaderControlled = false;
                group.externalLeader = true;
                state.localLeaderControlled = false;
                state.externalLeader = true;
                state.leaderAbsent = state.tooltipGroupsByHash.values().stream()
                        .noneMatch(candidate -> candidate.localLeaderControlled);
            }
        }
        log.info("{} maintenance local-team tooltip group recorded: session={} groupHash={} leaderPlayerId={} leaderWindow={} localLeaderControlled={} externalLeader={} windowId={} playerId={} role={} groupPlayers={} groupWindows={} source={}",
                logPrefix(context), sessionKey, normalizedGroupHash, normalizedLeaderPlayerId,
                group.leaderWindowId, group.localLeaderControlled, group.externalLeader,
                normalizedWindowId, normalizedPlayerId, roleName,
                group.memberPlayerIds,
                group.memberWindowIds,
                sourceTask);
    }

    /**
     * @param context current task context.
     * @return true for member auto-battle workers that belong to a local-team candidate/session.
     */
    public boolean isLocalSupportMemberCandidate(TaskExecutionContext context) {
        return context != null
                && context.isLocalLeaderPresent()
                && context.isLocalSupportMember()
                && context.hasLocalTeamSession()
                && !isCompletedLocalTeamSession(context.getLocalTeamSessionKey());
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
     * @param context current auto-battle member context.
     * @return true when this member belongs to a strict-tooltip-hash group whose same-team leader is
     *         a locally controlled window in the same active session.
     */
    public boolean shouldSuppressIdleMaintenanceBroadcast(TaskExecutionContext context) {
        if (!isLocalSupportMemberCandidate(context)) {
            return false;
        }
        String sessionKey = context.getLocalTeamSessionKey();
        LocalTeamSessionState state = localTeamSessions.get(sessionKey);
        String windowId = normalizeText(context.getWindowId());
        if (state == null || windowId == null || isCompletedLocalTeamSession(sessionKey)) {
            return false;
        }
        long now = System.currentTimeMillis();
        String groupHash = state.windowTooltipGroupHash.get(windowId);
        IdleBroadcastSuppressCacheEntry cached = state.idleBroadcastSuppressCacheByWindow.get(windowId);
        if (cached != null
                && cached.matches(groupHash, state.leaderWindowId, state.leaderPlayerId)
                && now - cached.verifiedAtMs < LOCAL_TEAM_IDLE_BROADCAST_SUPPRESS_CACHE_TTL_MS) {
            logCachedIdleBroadcastSuppressIfDue(context, state, windowId, cached, now);
            return true;
        }

        LocalTeamLeaderGroupMatch leaderMatch = resolveLocalControlledLeaderGroup(context, false);
        if (!leaderMatch.matched || leaderMatch.group == null || leaderMatch.state == null) {
            clearIdleBroadcastSuppressCache(context, state, windowId, "leader group unmatched");
            return false;
        }
        LocalTeamTooltipGroup group = leaderMatch.group;
        if (group.leaderPaused) {
            clearIdleBroadcastSuppressCache(context, state, windowId, "local leader paused");
            return false;
        }
        boolean suppress = !group.leaderWindowId.equals(leaderMatch.windowId);
        if (suppress) {
            recordIdleBroadcastSuppressCache(context, leaderMatch, group, now);
        } else {
            clearIdleBroadcastSuppressCache(context, state, windowId, "window is leader");
        }
        return suppress;
    }

    private void recordIdleBroadcastSuppressCache(TaskExecutionContext context,
                                                  LocalTeamLeaderGroupMatch leaderMatch,
                                                  LocalTeamTooltipGroup group,
                                                  long now) {
        String windowId = leaderMatch.windowId;
        String memberPlayerId = leaderMatch.state.windowPlayerIds.get(windowId);
        IdleBroadcastSuppressCacheEntry previous =
                leaderMatch.state.idleBroadcastSuppressCacheByWindow.get(windowId);
        boolean sameIdentity = previous != null
                && previous.matches(group.groupHash, group.leaderWindowId, group.leaderPlayerId);
        long lastInfoLogAt = sameIdentity ? previous.lastInfoLogAtMs : 0L;
        boolean logInfo = lastInfoLogAt <= 0L
                || now - lastInfoLogAt >= LOCAL_TEAM_IDLE_BROADCAST_SUPPRESS_LOG_INTERVAL_MS;
        IdleBroadcastSuppressCacheEntry next = new IdleBroadcastSuppressCacheEntry(
                group.groupHash,
                group.leaderWindowId,
                group.leaderPlayerId,
                memberPlayerId,
                now,
                logInfo ? now : lastInfoLogAt);
        leaderMatch.state.idleBroadcastSuppressCacheByWindow.put(windowId, next);
        if (logInfo) {
            log.info("{} maintenance idle broadcast scan suppressed: session={} groupHash={} leaderPlayerId={} leaderWindow={} memberWindow={} memberPlayerId={} cacheTtlMs={} logIntervalMs={}",
                    logPrefix(context), context.getLocalTeamSessionKey(), group.groupHash,
                    group.leaderPlayerId, group.leaderWindowId, windowId, memberPlayerId,
                    LOCAL_TEAM_IDLE_BROADCAST_SUPPRESS_CACHE_TTL_MS,
                    LOCAL_TEAM_IDLE_BROADCAST_SUPPRESS_LOG_INTERVAL_MS);
        } else {
            log.debug("{} maintenance idle broadcast scan suppressed by cache refresh: session={} groupHash={} leaderWindow={} memberWindow={} memberPlayerId={}",
                    logPrefix(context), context.getLocalTeamSessionKey(), group.groupHash,
                    group.leaderWindowId, windowId, memberPlayerId);
        }
    }

    private void logCachedIdleBroadcastSuppressIfDue(TaskExecutionContext context,
                                                     LocalTeamSessionState state,
                                                     String windowId,
                                                     IdleBroadcastSuppressCacheEntry cached,
                                                     long now) {
        if (now - cached.lastInfoLogAtMs < LOCAL_TEAM_IDLE_BROADCAST_SUPPRESS_LOG_INTERVAL_MS) {
            return;
        }
        IdleBroadcastSuppressCacheEntry updated = cached.withLastInfoLogAt(now);
        if (state.idleBroadcastSuppressCacheByWindow.replace(windowId, cached, updated)) {
            log.info("{} maintenance idle broadcast scan suppressed: session={} groupHash={} leaderPlayerId={} leaderWindow={} memberWindow={} memberPlayerId={} cached=true cacheAgeMs={} cacheTtlMs={}",
                    logPrefix(context), context.getLocalTeamSessionKey(), cached.groupHash,
                    cached.leaderPlayerId, cached.leaderWindowId, windowId, cached.memberPlayerId,
                    now - cached.verifiedAtMs, LOCAL_TEAM_IDLE_BROADCAST_SUPPRESS_CACHE_TTL_MS);
        }
    }

    private void clearIdleBroadcastSuppressCache(TaskExecutionContext context,
                                                 LocalTeamSessionState state,
                                                 String windowId,
                                                 String reason) {
        if (state == null || windowId == null) {
            return;
        }
        IdleBroadcastSuppressCacheEntry previous = state.idleBroadcastSuppressCacheByWindow.remove(windowId);
        if (previous != null) {
            log.info("{} maintenance idle broadcast suppress cache cleared: session={} memberWindow={} leaderWindow={} groupHash={} reason={}",
                    logPrefix(context), context == null ? null : context.getLocalTeamSessionKey(),
                    windowId, previous.leaderWindowId, previous.groupHash, reason);
        }
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
        if (isCompletedLocalTeamSession(context.getLocalTeamSessionKey())) {
            return;
        }
        String sessionKey = context.getLocalTeamSessionKey();
        LocalTeamSessionState state = localTeamSessions.computeIfAbsent(
                sessionKey, ignored -> new LocalTeamSessionState());
        String previous;
        synchronized (state) {
            state.leaderAbsent = false;
            state.knownLeaderWindowId = leaderWindowId;
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
        if (context == null || capability == null || !context.hasLocalTeamSession()
                || isCompletedLocalTeamSession(context.getLocalTeamSessionKey())) {
            return false;
        }
        LocalTeamSessionState state = localTeamSessions.get(context.getLocalTeamSessionKey());
        return state != null && state.capabilities.contains(capability);
    }

    private boolean hasDetectedLocalLeader(TaskExecutionContext context) {
        LocalTeamSessionState state = context == null || !context.hasLocalTeamSession()
                ? null
                : localTeamSessions.get(context.getLocalTeamSessionKey());
        return state != null
                && state.leaderWindowId != null
                && !isCompletedLocalTeamSession(context.getLocalTeamSessionKey());
    }

    private LocalTeamLeaderGroupMatch resolveLocalControlledLeaderGroup(TaskExecutionContext context,
                                                                        boolean allowSessionFallbackWithoutGroup) {
        return resolveLocalControlledLeaderGroup(context, allowSessionFallbackWithoutGroup, false);
    }

    private LocalTeamLeaderGroupMatch resolveLocalControlledLeaderGroup(TaskExecutionContext context,
                                                                        boolean allowSessionFallbackWithoutGroup,
                                                                        boolean allowPausedLeader) {
        if (!isLocalSupportMemberCandidate(context)) {
            return LocalTeamLeaderGroupMatch.unmatched();
        }
        LocalTeamSessionState state = localTeamSessions.get(context.getLocalTeamSessionKey());
        if (state == null) {
            return LocalTeamLeaderGroupMatch.unmatched();
        }
        String windowId = normalizeText(context.getWindowId());
        String groupHash = windowId == null ? null : state.windowTooltipGroupHash.get(windowId);
        if (groupHash != null) {
            LocalTeamTooltipGroup group = state.tooltipGroupsByHash.get(groupHash);
            boolean matched = group != null
                    && group.localLeaderControlled
                    && group.leaderWindowId != null
                    && (allowPausedLeader || !group.leaderPaused);
            return new LocalTeamLeaderGroupMatch(matched, state, group, windowId);
        }
        boolean matched = allowSessionFallbackWithoutGroup
                && state.leaderWindowId != null
                && (allowPausedLeader || !state.localLeaderPaused);
        return new LocalTeamLeaderGroupMatch(matched, state, null, windowId);
    }

    private void openLocalTeamSupportCapability(TaskExecutionContext context,
                                                TeamSupportCapability capability,
                                                String sourceTask) {
        if (context == null || !context.isLocalLeaderPresent() || !context.hasLocalTeamSession()
                || capability == null) {
            return;
        }
        if (isCompletedLocalTeamSession(context.getLocalTeamSessionKey())) {
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
            maintenanceSnapshotOpenedAtByRound.put(
                    localSupportCapabilityRoundKey(context.getLocalTeamSessionKey(), capability, epoch),
                    System.currentTimeMillis());
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
                Integer epoch = state.capabilityEpochByCapability.get(capability);
                if (epoch != null && epoch > 0) {
                    maintenanceSnapshotOpenedAtByRound.remove(
                            localSupportCapabilityRoundKey(context.getLocalTeamSessionKey(), capability, epoch));
                }
                log.info("{} maintenance local-team capability closed: session={} capability={} leaderWindow={} task={} source={}",
                        logPrefix(context), context.getLocalTeamSessionKey(), capability,
                        context.getLocalLeaderWindowId(), context.getTaskCode(), sourceTask);
            }
        }
    }

    /**
     * Mark one selected window as finished for a UI-started local-team session.
     *
     * <p>CR148 ends the local support lifecycle as soon as the live-detected leader leaves its
     * queue. Member auto-battle loops may keep running with an old final
     * {@link TaskExecutionContext#localTeamSessionKey}, so the service keeps a completed-session
     * tombstone and makes old contexts fall back to standalone maintenance semantics.</p>
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
        synchronized (state) {
            if (localTeamSessions.get(sessionKey) != state || isCompletedLocalTeamSession(sessionKey)) {
                return;
            }
            if (windowId != null && !windowId.isBlank()) {
                state.completedWindows.add(windowId);
                // CR244: a finished/stopped window must not keep the leader parked on its stale
                // pending-return entry; publish the change so a waiting leader re-reads the set.
                if (state.pendingReturnWindowIds.remove(windowId)) {
                    String leaderWindowId = state.leaderWindowId != null
                            ? state.leaderWindowId
                            : state.knownLeaderWindowId;
                    publishTeamReturnStateChanged(sessionKey, leaderWindowId, windowId,
                            "remove-window-completed", sourceTask, state.pendingReturnWindowIds);
                }
                // CR245: same rule for the maintenance broadcast queue — an exiting member must not
                // hold the head slot until the insurance deadline.
                synchronized (maintenanceBroadcastQueueMonitor) {
                    MaintenanceBroadcastQueueState queue = maintenanceBroadcastQueueByScope.get(sessionKey);
                    if (queue != null && queue.fifo.remove(windowId)) {
                        publishMaintenanceBroadcastQueueChanged(sessionKey, queue, windowId,
                                "remove-window-completed", sourceTask);
                    }
                }
            }
            boolean allCandidatesFinished = state.candidateWindows.isEmpty()
                    || state.completedWindows.containsAll(state.candidateWindows);
            boolean leaderFinished = windowId != null
                    && !windowId.isBlank()
                    && (windowId.equals(state.leaderWindowId) || windowId.equals(state.knownLeaderWindowId));
            if ((leaderFinished || allCandidatesFinished) && localTeamSessions.remove(sessionKey, state)) {
                markLocalTeamSessionCompleted(sessionKey, state, sourceTask, leaderFinished);
            }
        }
    }

    private boolean isCompletedLocalTeamSession(String sessionKey) {
        pruneCompletedLocalTeamSessions(System.currentTimeMillis());
        return sessionKey != null
                && !sessionKey.isBlank()
                && completedLocalTeamSessions.containsKey(sessionKey);
    }

    private void markLocalTeamSessionCompleted(String sessionKey,
                                               LocalTeamSessionState state,
                                               String sourceTask,
                                               boolean leaderFinished) {
        long now = System.currentTimeMillis();
        completedLocalTeamSessions.put(sessionKey, now);
        pruneCompletedLocalTeamSessions(now);
        String localRoundPrefix = "local-team:" + sessionKey + "#";
        maintenanceSnapshotOpenedAtByRound.keySet().removeIf(key -> key.startsWith(localRoundPrefix));
        summonSkillClaimsByTeamRound.keySet().removeIf(key -> key.startsWith(localRoundPrefix));
        clearSummonSkillQueuesForLocalTeamSession(state, sourceTask);
        synchronized (postCombatFirstAidMonitor) {
            String groupScopePrefix = sessionKey + POST_COMBAT_FIRST_AID_GROUP_SCOPE_SEPARATOR;
            boolean removedAny = postCombatFirstAidQueueByScope.keySet().removeIf(
                    key -> key.equals(sessionKey) || key.startsWith(groupScopePrefix));
            if (removedAny) {
                log.info("maintenance: post-combat first-aid queues removed with completed local-team session session={} source={}",
                        sessionKey, sourceTask);
            }
        }
        state.capabilities.clear();
        state.capabilityEpochByCapability.clear();
        state.leaderAbsent = true;
        state.localLeaderControlled = false;
        // CR244: completed sessions must not leave pending-return entries that could park a leader
        // forever; clear and publish once so any waiting leader wakes and re-reads an empty set.
        if (!state.pendingReturnWindowIds.isEmpty()) {
            state.pendingReturnWindowIds.clear();
            String leaderWindowId = state.leaderWindowId != null
                    ? state.leaderWindowId
                    : state.knownLeaderWindowId;
            publishTeamReturnStateChanged(sessionKey, leaderWindowId, "*",
                    "clear-session-completed", sourceTask, state.pendingReturnWindowIds);
        }
        // CR245: drop the session's maintenance broadcast queue with the session.
        synchronized (maintenanceBroadcastQueueMonitor) {
            MaintenanceBroadcastQueueState queue = maintenanceBroadcastQueueByScope.remove(sessionKey);
            if (queue != null) {
                queue.fifo.clear();
                publishMaintenanceBroadcastQueueChanged(sessionKey, queue, "*",
                        "clear-session-completed", sourceTask);
            }
        }
        // CR252: drop the session's team combat phases; departed members fall back to self radar.
        teamCombatPhaseByScope.entrySet().removeIf(entry -> {
            TeamCombatPhaseState phase = entry.getValue();
            return phase != null && sessionKey.equals(phase.sessionKey);
        });
        log.info("maintenance local-team session completed: session={} leaderFinished={} leaderPlayerId={} leaderWindow={} completedWindows={} candidates={} playerIds={} groupHashes={} source={}",
                sessionKey, leaderFinished, state.leaderPlayerId, state.leaderWindowId,
                state.completedWindows, state.candidateWindows, state.windowPlayerIds,
                state.tooltipGroupsByHash.keySet(), sourceTask);
    }

    private void clearSummonSkillQueuesForLocalTeamSession(LocalTeamSessionState state, String sourceTask) {
        Set<String> windowKeys = new HashSet<>(state.candidateWindows);
        windowKeys.addAll(state.completedWindows);
        if (state.leaderWindowId != null && !state.leaderWindowId.isBlank()) {
            windowKeys.add(state.leaderWindowId);
        }
        if (state.knownLeaderWindowId != null && !state.knownLeaderWindowId.isBlank()) {
            windowKeys.add(state.knownLeaderWindowId);
        }
        for (String windowKey : windowKeys) {
            if (windowKey == null || windowKey.isBlank()) {
                continue;
            }
            clearSummonSkillQueueForWindow(windowKey, sourceTask + ":local-team-session-completed");
        }
    }

    private void pruneCompletedLocalTeamSessions(long now) {
        completedLocalTeamSessions.entrySet().removeIf(
                entry -> now - entry.getValue() > COMPLETED_LOCAL_TEAM_SESSION_TTL_MS);
        while (completedLocalTeamSessions.size() > COMPLETED_LOCAL_TEAM_SESSION_MAX_TOMBSTONES) {
            String oldestSession = null;
            long oldestCompletedAt = Long.MAX_VALUE;
            for (Map.Entry<String, Long> entry : completedLocalTeamSessions.entrySet()) {
                if (entry.getValue() < oldestCompletedAt) {
                    oldestCompletedAt = entry.getValue();
                    oldestSession = entry.getKey();
                }
            }
            if (oldestSession == null) {
                return;
            }
            completedLocalTeamSessions.remove(oldestSession, oldestCompletedAt);
        }
    }

    /**
     * Return whether this team currently permits follower first aid; this is a pure state read and
     * never waits for a future leader action.
     */
    public boolean isTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext context, String teamMaintenanceKey) {
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
        MaintenanceThresholdCloudDecision thresholdDecision = decideMaintenanceThreshold(context, safeRequest);
        if (thresholdDecision.isRequiredFailure()) {
            TaskMaintenanceResult failure = cloudRequiredMaintenanceFailure(thresholdDecision);
            reportMaintenanceFailureShadow(context, safeRequest, failure);
            return failure;
        }
        if (!thresholdDecision.shouldRunMaintenance()) {
            TaskMaintenanceResult skipped = TaskMaintenanceResult.noAction(
                    "maintenance skipped by cloud threshold: " + thresholdDecision.getEffectiveDecision());
            return skipped;
        }

        if (safeRequest.isHandleMaintenanceBroadcast()) {
            TaskMaintenanceResult broadcastResult = handleMaintenanceBroadcast(context, safeRequest);
            if (broadcastResult.isHandled()
                    || broadcastResult.getStatus() == TaskMaintenanceStatus.BROADCAST_FAILED
                    || broadcastResult.getStatus() == TaskMaintenanceStatus.INTERRUPTED) {
                reportMaintenanceFailureShadow(context, safeRequest, broadcastResult);
                return broadcastResult;
            }
        }

        if (safeRequest.isCleanSummonSkill()) {
            TaskMaintenanceResult summonSkillResult = maybeCleanSummonSkill(context, safeRequest);
            reportMaintenanceFailureShadow(context, safeRequest, summonSkillResult);
            return summonSkillResult;
        }

        TaskMaintenanceResult noAction = TaskMaintenanceResult.noAction("no maintenance action");
        return noAction;
    }

    private MaintenanceThresholdCloudDecision decideMaintenanceThreshold(TaskExecutionContext context,
                                                                        TaskMaintenanceRequest request) {
        MaintenanceThresholdCloudDecision.Action localAction = plannedMaintenanceAction(request);
        String localReason = localAction == MaintenanceThresholdCloudDecision.Action.ALLOW
                ? "local maintenance request has an enabled action"
                : "local maintenance request has no enabled action";
        /*
         * This is only the local maintenance-pass entry gate. It decides whether the current
         * request has any enabled maintenance work; image understanding or policy choice belongs
         * to later, explicit brain/vision steps.
         */
        return MaintenanceThresholdCloudDecision.localOnly(
                "action=" + localAction + ";reason=" + localReason,
                localAction);
    }

    private MaintenanceThresholdCloudDecision.Action plannedMaintenanceAction(TaskMaintenanceRequest request) {
        if (request.isHandleMaintenanceBroadcast() || request.isCleanSummonSkill()) {
            return MaintenanceThresholdCloudDecision.Action.ALLOW;
        }
        return MaintenanceThresholdCloudDecision.Action.NO_ACTION;
    }

    private TaskMaintenanceResult cloudRequiredMaintenanceFailure(MaintenanceThresholdCloudDecision decision) {
        return TaskMaintenanceResult.builder()
                .status(TaskMaintenanceStatus.CLOUD_REQUIRED_FAILURE)
                .handled(true)
                .message("MAINTENANCE_THRESHOLD cloud-required failure: " + decision.getRejectReason())
                .build();
    }

    public TaskMaintenanceResult handleMaintenanceBroadcast(TaskExecutionContext context,
                                                            TaskMaintenanceRequest safeRequest) {
        String sourceTask = safeRequest.getSourceTask();
        checkpoint(context);
        if (coordinateHelper == null || inputSequences == null) {
            log.info("{} maintenance broadcast skipped: matcher dependencies unavailable source={}",
                    logPrefix(context), sourceTask);
            return TaskMaintenanceResult.noAction("no maintenance broadcast");
        }

        int[] rect = coordinateHelper.getScaledRect(
                MAINTENANCE_BROADCAST_ROI_LEFT,
                MAINTENANCE_BROADCAST_ROI_TOP,
                MAINTENANCE_BROADCAST_ROI_RIGHT - MAINTENANCE_BROADCAST_ROI_LEFT,
                MAINTENANCE_BROADCAST_ROI_BOTTOM - MAINTENANCE_BROADCAST_ROI_TOP);
        String[] templatePaths = {
                MAINTENANCE_HEAL_ALL_REPAIR_TEMPLATE,
                MAINTENANCE_REPAIR_CONFIRM_TEMPLATE
        };
        String[] actionKeys = {
                "heal-all-repair",
                "repair-confirm"
        };
        for (int i = 0; i < templatePaths.length; i++) {
            Point match = coordinateHelper.findImageInRegion(
                    templatePaths[i], rect, MAINTENANCE_BROADCAST_TEMPLATE_THRESHOLD);
            if (match == null) {
                continue;
            }
            boolean clicked = inputSequences.moveAndClickLeft(
                    "maintenance:broadcast:" + actionKeys[i] + ":" + sourceTask,
                    match.x,
                    match.y,
                    MAINTENANCE_BROADCAST_CLICK_SETTLE_MS,
                    MAINTENANCE_BROADCAST_CLICK_DELAY_MS);
            log.info("{} maintenance broadcast template match: source={} actionKey={} template={} click=({}, {}) roi=({}, {})-({}, {}) clicked={}",
                    logPrefix(context), sourceTask, actionKeys[i], templatePaths[i],
                    match.x, match.y, rect[0], rect[1], rect[2], rect[3],
                    clicked);
            if (clicked) {
                return TaskMaintenanceResult.broadcastHandled("maintenance broadcast handled");
            }
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.INTERRUPTED,
                    "maintenance broadcast click not completed");
        }
        log.info("{} maintenance broadcast not found in ROI: source={} roi=({}, {})-({}, {})",
                logPrefix(context), sourceTask, rect[0], rect[1], rect[2], rect[3]);
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
        String queueKey = summonSkillQueueKey(windowKey);
        Long lastCleanAt = lastSummonSkillCleanAtByWindow.get(windowKey);
        long effectiveIntervalMs = effectiveSummonSkillCleanIntervalMs(intervalMs);
        if (lastCleanAt != null && now - lastCleanAt < effectiveIntervalMs) {
            logSummonSkillNotDue(context, request, windowKey, now, lastCleanAt, intervalMs, effectiveIntervalMs);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_NOT_DUE,
                    "summon skill not due");
        }
        log.info("{} maintenance: summon skill due source={} windowKey={} lastCleanAt={} elapsedMs={} intervalMs={} effectiveIntervalMs={} leadTimeMs={}",
                logPrefix(context), request.getSourceTask(), windowKey, lastCleanAt,
                lastCleanAt == null ? -1 : now - lastCleanAt, intervalMs, effectiveIntervalMs,
                SUMMON_SKILL_DUE_LEAD_TIME_MS);
        enqueueSummonSkillIfAbsent(context, request, windowKey, queueKey, now);
        if (request.isEnqueueSummonSkillOnly()) {
            log.info("{} maintenance: summon skill enqueue-only deferred source={} windowKey={} queueKey={}",
                    logPrefix(context), request.getSourceTask(), windowKey, queueKey);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                    "summon skill queued: enqueue-only maintenance pass");
        }

        String teamRoundKey = resolveTeamRoundKey(context, request);
        Long windowOpenedAt = resolveSummonSkillWindowOpenedAt(context, request, teamRoundKey);
        if (windowOpenedAt == null) {
            log.info("{} maintenance: summon skill queued, no consumable maintenance snapshot source={} windowKey={} teamRound={}",
                    logPrefix(context), request.getSourceTask(), windowKey, teamRoundKey);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                    "summon skill queued: no maintenance snapshot");
        }
        SummonSkillQueueItem queueItem = peekEligibleSummonSkillHead(queueKey, windowOpenedAt);
        if (queueItem == null) {
            moveRetryBackoffSummonSkillHeadsToTail(windowOpenedAt, now, context, request);
            queueItem = peekEligibleSummonSkillHead(queueKey, windowOpenedAt);
        }
        if (queueItem == null) {
            log.info("{} maintenance: summon skill queued for later snapshot source={} windowKey={} teamRound={} windowOpenedAt={}",
                    logPrefix(context), request.getSourceTask(), windowKey, teamRoundKey, windowOpenedAt);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                    "summon skill queued: waiting for next maintenance window");
        }

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
        if (request.isOneSummonSkillPerTeamRound()) {
            if (teamRoundKey == null) {
                logSummonSkillDeferredNoAction(context, request, windowKey, "no-active-team-round", null);
                return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                        "summon skill deferred: no active team round");
            }
            if (request.getRequiredLocalSupportCapability() != null
                    && !isLocalTeamSupportCapabilityOpen(context, request.getRequiredLocalSupportCapability())) {
                logSummonSkillDeferredNoAction(context, request, windowKey, "local-capability-closed", teamRoundKey);
                return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED,
                        "summon skill deferred: local support capability closed");
            }
            if (request.getRequiredLocalSupportCapability() == null
                    && request.isRequireOpenTeamMaintenanceWindow()
                    && teamMaintenanceWindowStateByRound.get(teamRoundKey) != TeamMaintenanceWindowState.PATHING_WINDOW_OPEN) {
                logSummonSkillDeferredNoAction(context, request, windowKey, "team-pathing-window-closed", teamRoundKey);
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
        if (isSummonSkillTailSafeCacheFresh(windowState, now)) {
            removeSummonSkillQueueItem(queueItem);
            lastSummonSkillCleanAtByWindow.put(windowKey, now);
            lastSummonSkillNotDueLogAtByWindow.remove(windowKey);
            log.info("{} maintenance: summon skill skipped by fresh tail-safe cache and dequeued source={} windowKey={} queueKey={} cacheAgeMs={} ttlMs={} lastEffectiveSlot={} nextStartSlot={}",
                    logPrefix(context), request.getSourceTask(), windowKey,
                    queueItem.queueKey,
                    now - windowState.tailSafeCachedAt, SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS,
                    windowState.lastConfirmedEffectiveSlotIndex + 1,
                    windowState.nextStartIndex + 1);
            return TaskMaintenanceResult.simple(TaskMaintenanceStatus.SUMMON_SKILL_NOT_DUE,
                    "summon skill tail-safe cache fresh");
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
            removeSummonSkillQueueItem(queueItem);
            return TaskMaintenanceResult.summonSkillCleaned("summon skill cleaned");
        }
        moveSummonSkillQueueItemToTail(queueItem, cleanupResult);
        return TaskMaintenanceResult.builder()
                .status(TaskMaintenanceStatus.SUMMON_SKILL_FAILED_RETRY_LATER)
                .summonSkillAttempted(true)
                .message("summon skill failed; retry later")
                .build();
    }

    private void reportFeatureFlagShadow(TaskExecutionContext context,
                                         String sourceTask,
                                         String flagName,
                                         boolean enabled,
                                         String reason) {
        Map<String, String> shadowContext = baseShadowContext(context, sourceTask);
        shadowContext.put("flagName", safe(flagName));
        shadowContext.put("flagValue", Boolean.toString(enabled));
        shadowContext.put("reason", safe(reason));
        shadowRuntimeDecision(
                CloudDecisionServiceId.FEATURE_FLAG,
                context,
                "maintenance-feature-flag",
                sourceTask,
                "flag=" + safe(flagName) + ";enabled=" + enabled,
                shadowContext);
    }

    private void reportCapabilityGateShadow(TaskExecutionContext context,
                                            TeamSupportCapability capability,
                                            long timeoutMs,
                                            boolean allowed,
                                            String reason) {
        Map<String, String> shadowContext = baseShadowContext(context, "local-capability-gate");
        shadowContext.put("capability", capability == null ? "" : capability.name());
        shadowContext.put("allowed", Boolean.toString(allowed));
        shadowContext.put("timeoutMs", Long.toString(timeoutMs));
        shadowContext.put("reason", safe(reason));
        shadowRuntimeDecision(
                CloudDecisionServiceId.CAPABILITY_GATE,
                context,
                "local-support-capability-gate",
                "local-capability-gate",
                "capability=" + (capability == null ? "" : capability.name()) + ";allowed=" + allowed,
                shadowContext);
    }

    private void reportMaintenanceThresholdShadow(TaskExecutionContext context,
                                                  TaskMaintenanceRequest request,
                                                  TaskMaintenanceResult result) {
        Map<String, String> shadowContext = baseShadowContext(context, request.getSourceTask());
        shadowContext.put("status", result.getStatus() == null ? "" : result.getStatus().name());
        shadowContext.put("handled", Boolean.toString(result.isHandled()));
        shadowContext.put("broadcastHandled", Boolean.toString(result.isBroadcastHandled()));
        shadowContext.put("summonSkillAttempted", Boolean.toString(result.isSummonSkillAttempted()));
        shadowContext.put("summonSkillSucceeded", Boolean.toString(result.isSummonSkillSucceeded()));
        shadowContext.put("message", safe(result.getMessage()));
        shadowContext.put("handleMaintenanceBroadcast", Boolean.toString(request.isHandleMaintenanceBroadcast()));
        shadowContext.put("cleanSummonSkill", Boolean.toString(request.isCleanSummonSkill()));
        shadowContext.put("enqueueSummonSkillOnly", Boolean.toString(request.isEnqueueSummonSkillOnly()));
        shadowContext.put("requireFreeStateForSummonSkill",
                Boolean.toString(request.isRequireFreeStateForSummonSkill()));
        shadowContext.put("oneSummonSkillPerTeamRound", Boolean.toString(request.isOneSummonSkillPerTeamRound()));
        shadowContext.put("maxSummonSkillCleanersPerTeamRound",
                Integer.toString(request.getMaxSummonSkillCleanersPerTeamRound()));
        shadowContext.put("teamMaintenanceKey", safe(request.getTeamMaintenanceKey()));
        shadowContext.put("teamRound", request.getTeamRound() == null ? "" : request.getTeamRound().toString());
        shadowContext.put("requireOpenTeamMaintenanceWindow",
                Boolean.toString(request.isRequireOpenTeamMaintenanceWindow()));
        shadowContext.put("requiredLocalSupportCapability",
                request.getRequiredLocalSupportCapability() == null
                        ? ""
                        : request.getRequiredLocalSupportCapability().name());
        shadowContext.put("summonSkillEnabled",
                botProperties == null ? "" : Boolean.toString(botProperties.isSummonSkillCleanEnabled()));
        shadowContext.put("summonSkillIntervalMs",
                botProperties == null ? "" : Long.toString(botProperties.getSummonSkillCleanIntervalMs()));
        shadowContext.put("summonSkillUnknownRetryAfterMs",
                botProperties == null ? "" : Long.toString(botProperties.getSummonSkillUnknownFailureRetryAfterMs()));
        shadowRuntimeDecision(
                CloudDecisionServiceId.MAINTENANCE_THRESHOLD,
                context,
                "maintenance-threshold-result",
                request.getSourceTask(),
                maintenanceDecision(result),
                shadowContext);
    }

    private void reportMaintenanceFailureShadow(TaskExecutionContext context,
                                                TaskMaintenanceRequest request,
                                                TaskMaintenanceResult result) {
        if (result.getStatus() != TaskMaintenanceStatus.BROADCAST_FAILED
                && result.getStatus() != TaskMaintenanceStatus.INTERRUPTED
                && result.getStatus() != TaskMaintenanceStatus.CLOUD_REQUIRED_FAILURE
                && result.getStatus() != TaskMaintenanceStatus.SUMMON_SKILL_FAILED_RETRY_LATER) {
            return;
        }
        Map<String, String> shadowContext = baseShadowContext(context, request.getSourceTask());
        shadowContext.put("status", result.getStatus() == null ? "" : result.getStatus().name());
        shadowContext.put("message", safe(result.getMessage()));
        shadowContext.put("handled", Boolean.toString(result.isHandled()));
        shadowContext.put("broadcastHandled", Boolean.toString(result.isBroadcastHandled()));
        shadowContext.put("summonSkillAttempted", Boolean.toString(result.isSummonSkillAttempted()));
        shadowContext.put("summonSkillSucceeded", Boolean.toString(result.isSummonSkillSucceeded()));
        shadowRuntimeDecision(
                CloudDecisionServiceId.FAILURE_CLASSIFIER,
                context,
                "maintenance-failure-classifier",
                request.getSourceTask(),
                "class=maintenance;status=" + (result.getStatus() == null ? "" : result.getStatus().name()),
                shadowContext);
    }

    private String maintenanceDecision(TaskMaintenanceResult result) {
        return "status=" + (result.getStatus() == null ? "" : result.getStatus().name())
                + ";handled=" + result.isHandled()
                + ";broadcastHandled=" + result.isBroadcastHandled()
                + ";summonSkillAttempted=" + result.isSummonSkillAttempted()
                + ";summonSkillSucceeded=" + result.isSummonSkillSucceeded();
    }

    private Map<String, String> baseShadowContext(TaskExecutionContext context, String sourceTask) {
        Map<String, String> shadowContext = new LinkedHashMap<>();
        shadowContext.put("sourceTask", safe(sourceTask));
        shadowContext.put("taskCode", taskCode(context));
        shadowContext.put("requestedTaskCode", context == null ? "" : safe(context.getRequestedTaskCode()));
        shadowContext.put("windowId", context == null ? "" : safe(context.getWindowId()));
        shadowContext.put("windowRole", context == null ? "" : safe(context.getWindowRole()));
        shadowContext.put("taskRunId", context == null ? "" : Long.toString(context.getTaskRunId()));
        shadowContext.put("localTeamSession", context == null ? "" : safe(context.getLocalTeamSessionKey()));
        shadowContext.put("localSupportMember",
                context == null ? "" : Boolean.toString(context.isLocalSupportMember()));
        shadowContext.put("localLeaderPresent",
                context == null ? "" : Boolean.toString(context.isLocalLeaderPresent()));
        shadowContext.put("localLeaderWindow", context == null ? "" : safe(context.getLocalLeaderWindowId()));
        shadowContext.put("windowKey", currentWindowKey(context));
        return shadowContext;
    }

    private void shadowRuntimeDecision(CloudDecisionServiceId serviceId,
                                       TaskExecutionContext context,
                                       String phase,
                                       String sourceTask,
                                       String localDecision,
                                       Map<String, String> shadowContext) {
        if (runtimeDecisionShadowService == null) {
            return;
        }
        try {
            runtimeDecisionShadowService.shadow(
                    serviceId,
                    taskCode(context),
                    phase,
                    sourceTask,
                    localDecision,
                    shadowContext);
        } catch (RuntimeException e) {
            log.warn("{} cloud runtime shadow ignored after maintenance local decision: serviceId={} phase={} source={} reason={}",
                    logPrefix(context), serviceId, phase, sourceTask, e.toString());
            log.debug("maintenance cloud shadow failure stack", e);
        }
    }

    private void enqueueSummonSkillIfAbsent(TaskExecutionContext context,
                                            TaskMaintenanceRequest request,
                                            String windowKey,
                                            String queueKey,
                                            long now) {
        boolean added;
        synchronized (summonSkillQueueMonitor) {
            added = summonSkillQueueKeys.add(queueKey);
            if (added) {
                summonSkillQueue.addLast(new SummonSkillQueueItem(queueKey, windowKey, now));
            }
        }
        if (!added) {
            log.info("{} maintenance: summon skill queue duplicate ignored source={} windowKey={} queueKey={}",
                    logPrefix(context), request.getSourceTask(), windowKey, queueKey);
            return;
        }
        log.info("{} maintenance: summon skill queued source={} windowKey={} queueKey={} enqueuedAt={}",
                logPrefix(context), request.getSourceTask(), windowKey, queueKey, now);
    }

    private Long resolveSummonSkillWindowOpenedAt(TaskExecutionContext context,
                                                  TaskMaintenanceRequest request,
                                                  String teamRoundKey) {
        if (request.isOneSummonSkillPerTeamRound()) {
            if (teamRoundKey == null) {
                return null;
            }
            if (request.getRequiredLocalSupportCapability() != null) {
                if (!isLocalTeamSupportCapabilityOpen(context, request.getRequiredLocalSupportCapability())) {
                    return null;
                }
                return maintenanceSnapshotOpenedAtByRound.get(teamRoundKey);
            }
            if (teamMaintenanceWindowStateByRound.get(teamRoundKey) != TeamMaintenanceWindowState.PATHING_WINDOW_OPEN) {
                return null;
            }
            return maintenanceSnapshotOpenedAtByRound.get(teamRoundKey);
        }
        log.info("{} maintenance: summon skill queued until a team maintenance window is available source={}",
                logPrefix(context), request.getSourceTask());
        return null;
    }

    private SummonSkillQueueItem peekEligibleSummonSkillHead(String queueKey, long windowOpenedAt) {
        synchronized (summonSkillQueueMonitor) {
            SummonSkillQueueItem item = summonSkillQueue.peekFirst();
            if (item != null && item.queueKey.equals(queueKey) && item.enqueuedAt < windowOpenedAt) {
                return item;
            }
        }
        return null;
    }

    private void removeSummonSkillQueueItem(SummonSkillQueueItem item) {
        synchronized (summonSkillQueueMonitor) {
            summonSkillQueue.remove(item);
            summonSkillQueueKeys.remove(item.queueKey);
        }
    }

    private int removeSummonSkillQueueItemsForWindow(String windowKey) {
        int removed = 0;
        synchronized (summonSkillQueueMonitor) {
            Iterator<SummonSkillQueueItem> iterator = summonSkillQueue.iterator();
            while (iterator.hasNext()) {
                SummonSkillQueueItem item = iterator.next();
                if (item.windowKey.equals(windowKey)) {
                    iterator.remove();
                    summonSkillQueueKeys.remove(item.queueKey);
                    removed++;
                }
            }
            String queueKeyPrefix = windowKey + "#";
            int sizeBeforeSetCleanup = summonSkillQueueKeys.size();
            summonSkillQueueKeys.removeIf(key -> key.startsWith(queueKeyPrefix));
            removed += sizeBeforeSetCleanup - summonSkillQueueKeys.size();
        }
        return removed;
    }

    private void moveSummonSkillQueueItemToTail(SummonSkillQueueItem item,
                                                SummonSkillCleanupResult cleanupResult) {
        moveSummonSkillQueueItemToTail(item, cleanupResult.getMessage(), true);
    }

    private void moveSummonSkillQueueItemToTail(SummonSkillQueueItem item,
                                                String reason,
                                                boolean countAttempt) {
        synchronized (summonSkillQueueMonitor) {
            Iterator<SummonSkillQueueItem> iterator = summonSkillQueue.iterator();
            while (iterator.hasNext()) {
                if (iterator.next() == item) {
                    iterator.remove();
                    break;
                }
            }
            if (countAttempt) {
                item.attemptCount++;
            }
            item.lastFailureReason = reason;
            summonSkillQueue.addLast(item);
        }
        log.info("maintenance: summon skill queue item moved to tail after failure windowKey={} queueKey={} attempts={} reason={}",
                item.windowKey, item.queueKey, item.attemptCount, item.lastFailureReason);
    }

    private void moveRetryBackoffSummonSkillHeadsToTail(long windowOpenedAt,
                                                        long now,
                                                        TaskExecutionContext context,
                                                        TaskMaintenanceRequest request) {
        synchronized (summonSkillQueueMonitor) {
            int inspected = 0;
            int maxInspections = summonSkillQueue.size();
            while (inspected < maxInspections) {
                SummonSkillQueueItem head = summonSkillQueue.peekFirst();
                if (head == null || head.enqueuedAt >= windowOpenedAt) {
                    return;
                }
                Long retryAfterAt = summonSkillUnknownRetryAfterByWindow.get(head.windowKey);
                if (retryAfterAt == null || now >= retryAfterAt) {
                    return;
                }
                summonSkillQueue.removeFirst();
                head.lastFailureReason = "unknown retry backoff active until " + retryAfterAt;
                summonSkillQueue.addLast(head);
                inspected++;
                log.info("{} maintenance: summon skill queue head moved to tail by retry backoff source={} headWindowKey={} queueKey={} retryAfterAt={} remainingMs={}",
                        logPrefix(context), request.getSourceTask(), head.windowKey, head.queueKey,
                        retryAfterAt, retryAfterAt - now);
            }
        }
    }

    private void logSummonSkillNotDue(TaskExecutionContext context,
                                      TaskMaintenanceRequest request,
                                      String windowKey,
                                      long now,
                                      long lastCleanAt,
                                      long intervalMs,
                                      long effectiveIntervalMs) {
        Long lastLogAt = lastSummonSkillNotDueLogAtByWindow.get(windowKey);
        if (lastLogAt != null && now - lastLogAt < SUMMON_SKILL_NOT_DUE_LOG_INTERVAL_MS) {
            return;
        }
        lastSummonSkillNotDueLogAtByWindow.put(windowKey, now);
        long elapsedMs = now - lastCleanAt;
        long remainingMs = Math.max(0, effectiveIntervalMs - elapsedMs);
        log.info("{} maintenance: summon skill not due source={} windowKey={} elapsedMs={} remainingMs={} intervalMs={} effectiveIntervalMs={} leadTimeMs={} lastCleanAt={}",
                logPrefix(context), request.getSourceTask(), windowKey, elapsedMs, remainingMs,
                intervalMs, effectiveIntervalMs, SUMMON_SKILL_DUE_LEAD_TIME_MS, lastCleanAt);
    }

    private void logSummonSkillDeferredNoAction(TaskExecutionContext context,
                                                TaskMaintenanceRequest request,
                                                String windowKey,
                                                String reason,
                                                String teamRoundKey) {
        String teamKey = normalizeTeamKey(request.getTeamMaintenanceKey(), context);
        TeamMaintenanceWindowState windowState = teamRoundKey == null
                ? null
                : teamMaintenanceWindowStateByRound.get(teamRoundKey);
        String logKey = noActionLogKey("summon-skill-deferred",
                context,
                windowKey,
                reason,
                teamKey,
                teamRoundKey,
                request.getRequiredLocalSupportCapability() == null
                        ? ""
                        : request.getRequiredLocalSupportCapability().name(),
                windowState == null ? "" : windowState.name());
        if (shouldLogNoAction(lastSummonSkillDeferredLogAtByKey, logKey, System.currentTimeMillis())) {
            log.info("{} maintenance: summon skill deferred, no action reason={} source={} windowKey={} teamKey={} teamRound={} localCapability={} windowState={} logIntervalMs={}",
                    logPrefix(context), reason, request.getSourceTask(), windowKey, teamKey, teamRoundKey,
                    request.getRequiredLocalSupportCapability(), windowState,
                    MAINTENANCE_NO_ACTION_LOG_INTERVAL_MS);
        } else {
            log.debug("{} maintenance: summon skill deferred suppressed by log throttle reason={} source={} windowKey={} teamRound={}",
                    logPrefix(context), reason, request.getSourceTask(), windowKey, teamRoundKey);
        }
    }

    private boolean shouldLogNoAction(Map<String, Long> lastLogAtByKey, String key, long now) {
        Long lastLogAt = lastLogAtByKey.get(key);
        if (lastLogAt != null && now - lastLogAt < MAINTENANCE_NO_ACTION_LOG_INTERVAL_MS) {
            return false;
        }
        lastLogAtByKey.put(key, now);
        return true;
    }

    private String noActionLogKey(String type, TaskExecutionContext context, String... parts) {
        StringBuilder builder = new StringBuilder(type == null ? "" : type);
        builder.append('|').append(context == null ? "" : safe(context.getLocalTeamSessionKey()));
        builder.append('|').append(context == null ? "" : safe(context.getWindowId()));
        if (parts != null) {
            for (String part : parts) {
                builder.append('|').append(safe(part));
            }
        }
        return builder.toString();
    }

    private static long effectiveSummonSkillCleanIntervalMs(long intervalMs) {
        return Math.max(0L, intervalMs - SUMMON_SKILL_DUE_LEAD_TIME_MS);
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

    /**
     * CR253 read-only due probe for the background green-chain publisher: same config gates and the
     * same per-window cooldown comparison as {@code maybeCleanSummonSkill}, but with no side effects
     * (no state creation, no epoch reconciliation, no queueing). The consumer still runs the full
     * gated maintenance pass, so an over-eager true here can only cost one deferred job.
     *
     * @param context bound execution context of the observed window.
     * @return true when a summon-skill clean would be considered due for this window right now.
     */
    public boolean isSummonSkillCleanDueForCurrentWindow(TaskExecutionContext context) {
        if (!botProperties.isSummonSkillCleanEnabled()) {
            return false;
        }
        long intervalMs = botProperties.getSummonSkillCleanIntervalMs();
        if (intervalMs <= 0) {
            return false;
        }
        String windowKey = currentWindowKey(context);
        long now = System.currentTimeMillis();
        // CR253 review P1: the publisher must respect the subsystem's OWN retry timing — while the
        // unknown-failure backoff is active the clean is not consumable, so it is not "due" either.
        Long unknownRetryAfterAt = summonSkillUnknownRetryAfterByWindow.get(windowKey);
        if (unknownRetryAfterAt != null && now < unknownRetryAfterAt) {
            return false;
        }
        Long lastCleanAt = lastSummonSkillCleanAtByWindow.get(windowKey);
        long effectiveIntervalMs = effectiveSummonSkillCleanIntervalMs(intervalMs);
        return lastCleanAt == null || now - lastCleanAt >= effectiveIntervalMs;
    }

    private String summonSkillQueueKey(String windowKey) {
        return windowKey + "#" + currentPlayerIdentityEpoch() + "#SUMMON_SKILL";
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
                boolean initialIdentityBinding = existing.playerIdentityEpoch == 0L && epoch != 0L;
                log.warn("maintenance: invalidate summon skill cache by player identity drift windowKey={} oldEpoch={} newEpoch={} cachedSkillCount={} nextStartSlot={} tailSafeCachedAt={}",
                        windowKey, existing.playerIdentityEpoch, epoch, existing.skillCount,
                        existing.nextStartIndex == null ? null : existing.nextStartIndex + 1,
                        existing.tailSafeCachedAt);
                if (initialIdentityBinding) {
                    // Task start already armed this window's cooldown before the first identity sync.
                    log.info("maintenance: preserve summon skill cooldown on initial identity binding windowKey={} newEpoch={}",
                            windowKey, epoch);
                } else {
                    lastSummonSkillCleanAtByWindow.remove(windowKey);
                }
                lastSummonSkillNotDueLogAtByWindow.remove(windowKey);
                summonSkillUnknownRetryAfterByWindow.remove(windowKey);
                removeSummonSkillQueueItemsForWindow(windowKey);
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

    private String taskCode(TaskExecutionContext context) {
        if (context == null) {
            return DEFAULT_WINDOW_KEY;
        }
        if (context.getRequestedTaskCode() != null && !context.getRequestedTaskCode().isBlank()) {
            return context.getRequestedTaskCode();
        }
        if (context.getTaskCode() != null && !context.getTaskCode().isBlank()) {
            return context.getTaskCode();
        }
        return DEFAULT_WINDOW_KEY;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void recordLocalTeamPlayerIdentity(LocalTeamSessionState state,
                                                      String windowId,
                                                      String playerId) {
        if (state == null || windowId == null || windowId.isBlank()
                || playerId == null || playerId.isBlank()) {
            return;
        }
        String normalizedWindowId = normalizeText(windowId);
        String normalizedPlayerId = normalizeText(playerId);
        state.windowPlayerIds.put(normalizedWindowId, normalizedPlayerId);
        state.playerWindowIds.put(normalizedPlayerId, normalizedWindowId);
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
            return localSupportCapabilityRoundKey(context.getLocalTeamSessionKey(), capability, epoch);
        }

    private String localSupportCapabilityRoundKey(String sessionKey,
                                                  TeamSupportCapability capability,
                                                  int epoch) {
        return "local-team:" + sessionKey + "#" + capability.name() + "#" + epoch;
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
        maintenanceSnapshotOpenedAtByRound.keySet().removeIf(key -> {
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

    private static class SummonSkillQueueItem {
        private final String queueKey;
        private final String windowKey;
        private final long enqueuedAt;
        private int attemptCount;
        private String lastFailureReason;

        private SummonSkillQueueItem(String queueKey, String windowKey, long enqueuedAt) {
            this.queueKey = queueKey;
            this.windowKey = windowKey;
            this.enqueuedAt = enqueuedAt;
        }
    }

    private static final class LocalTeamSessionState {
        private final Set<TeamSupportCapability> capabilities = ConcurrentHashMap.newKeySet();
        private final Map<TeamSupportCapability, Integer> capabilityEpochByCapability = new ConcurrentHashMap<>();
        private final Set<String> candidateWindows = ConcurrentHashMap.newKeySet();
        private final Map<String, String> windowPlayerIds = new ConcurrentHashMap<>();
        private final Map<String, String> playerWindowIds = new ConcurrentHashMap<>();
        private final Map<String, String> windowTooltipGroupHash = new ConcurrentHashMap<>();
        private final Map<String, LocalTeamTooltipGroup> tooltipGroupsByHash = new ConcurrentHashMap<>();
        private final Map<String, IdleBroadcastSuppressCacheEntry> idleBroadcastSuppressCacheByWindow =
                new ConcurrentHashMap<>();
        private final Set<String> roleDetectedWindows = ConcurrentHashMap.newKeySet();
        private final Set<String> completedWindows = ConcurrentHashMap.newKeySet();
        /**
         * CR244: member-owned pending-return facts keyed by stable window id. Members add/remove
         * only their own id; the leader only reads emptiness at its two accept gates.
         */
        private final Set<String> pendingReturnWindowIds = ConcurrentHashMap.newKeySet();
        private volatile String leaderPlayerId;
        private volatile String knownLeaderWindowId;
        private volatile String leaderWindowId;
        private volatile boolean leaderAbsent;
        private volatile boolean localLeaderControlled;
        private volatile boolean localLeaderPaused;
        private volatile boolean externalLeader;
    }

    private static final class IdleBroadcastSuppressCacheEntry {
        private final String groupHash;
        private final String leaderWindowId;
        private final String leaderPlayerId;
        private final String memberPlayerId;
        private final long verifiedAtMs;
        private final long lastInfoLogAtMs;

        private IdleBroadcastSuppressCacheEntry(String groupHash,
                                                String leaderWindowId,
                                                String leaderPlayerId,
                                                String memberPlayerId,
                                                long verifiedAtMs,
                                                long lastInfoLogAtMs) {
            this.groupHash = groupHash;
            this.leaderWindowId = leaderWindowId;
            this.leaderPlayerId = leaderPlayerId;
            this.memberPlayerId = memberPlayerId;
            this.verifiedAtMs = verifiedAtMs;
            this.lastInfoLogAtMs = lastInfoLogAtMs;
        }

        private boolean matches(String currentGroupHash,
                                String currentLeaderWindowId,
                                String currentLeaderPlayerId) {
            return same(groupHash, currentGroupHash)
                    && same(leaderWindowId, currentLeaderWindowId)
                    && same(leaderPlayerId, currentLeaderPlayerId);
        }

        private IdleBroadcastSuppressCacheEntry withLastInfoLogAt(long now) {
            return new IdleBroadcastSuppressCacheEntry(
                    groupHash,
                    leaderWindowId,
                    leaderPlayerId,
                    memberPlayerId,
                    verifiedAtMs,
                    now);
        }

        private static boolean same(String left, String right) {
            return left == null ? right == null : left.equals(right);
        }
    }

    private static final class LocalTeamTooltipGroup {
        private final String groupHash;
        private final Set<String> memberPlayerIds = ConcurrentHashMap.newKeySet();
        private final Set<String> memberWindowIds = ConcurrentHashMap.newKeySet();
        private volatile String leaderPlayerId;
        private volatile String leaderWindowId;
        private volatile boolean localLeaderControlled;
        private volatile boolean leaderPaused;
        private volatile boolean externalLeader;

        private LocalTeamTooltipGroup(String groupHash) {
            this.groupHash = groupHash;
        }
    }

    private static final class LocalTeamLeaderGroupMatch {
        private static final LocalTeamLeaderGroupMatch UNMATCHED =
                new LocalTeamLeaderGroupMatch(false, null, null, null);

        private final boolean matched;
        private final LocalTeamSessionState state;
        private final LocalTeamTooltipGroup group;
        private final String windowId;

        private LocalTeamLeaderGroupMatch(boolean matched,
                                          LocalTeamSessionState state,
                                          LocalTeamTooltipGroup group,
                                          String windowId) {
            this.matched = matched;
            this.state = state;
            this.group = group;
            this.windowId = windowId;
        }

        private static LocalTeamLeaderGroupMatch unmatched() {
            return UNMATCHED;
        }
    }

    public enum LocalTeamSessionAttachStatus {
        ATTACHED,
        NO_TOOLTIP_EVIDENCE,
        NO_ACTIVE_LOCAL_TEAM_SESSION,
        NO_MATCHING_LOCAL_LEADER,
        SESSION_COMPLETED_OR_REMOVED,
        AMBIGUOUS_MATCH
    }

    public record LocalTeamSessionAttachResult(LocalTeamSessionAttachStatus status,
                                               String sessionKey,
                                               String leaderWindowId) {
        private static LocalTeamSessionAttachResult attached(String sessionKey, String leaderWindowId) {
            return new LocalTeamSessionAttachResult(LocalTeamSessionAttachStatus.ATTACHED,
                    sessionKey, leaderWindowId);
        }

        private static LocalTeamSessionAttachResult noTooltipEvidence() {
            return new LocalTeamSessionAttachResult(LocalTeamSessionAttachStatus.NO_TOOLTIP_EVIDENCE,
                    null, null);
        }

        private static LocalTeamSessionAttachResult ambiguous() {
            return new LocalTeamSessionAttachResult(LocalTeamSessionAttachStatus.AMBIGUOUS_MATCH,
                    null, null);
        }

        private static LocalTeamSessionAttachResult completedOrRemoved() {
            return new LocalTeamSessionAttachResult(
                    LocalTeamSessionAttachStatus.SESSION_COMPLETED_OR_REMOVED, null, null);
        }
    }
}
