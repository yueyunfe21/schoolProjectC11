package com.bot.dhxy.task;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.maintenance.TeamSupportCapability;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.model.maintenance.TaskMaintenanceResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.CommonBoxService;
import com.bot.dhxy.service.LeftTopStatusSwitchService;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.service.TaskMaintenanceService;
import com.bot.dhxy.service.TeamReturnService;
import com.bot.dhxy.task.startup.TaskStartupCheckResult;
import com.bot.dhxy.task.startup.TaskStartupCheckService;
import com.bot.dhxy.task.template.BaseTaskTemplate;
import com.bot.dhxy.task.template.TaskStepExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Background auto-battle task for one bound game window.
 *
 * <p>Every window assigned to auto-battle uses the same patrol loop. Team-role based assignment may
 * still choose auto-battle for member windows, but this class does not create a second internal
 * mode.</p>
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class AutoBattleTask extends BaseTaskTemplate {
    private static final long FREE_PATROL_INTERVAL_MS = 3000L;
    private static final long PENDING_FIRST_AID_POLL_INTERVAL_MS = 500L;

    private final AutoCombatService autoCombatService;
    private final PlayerStateService playerStateService;
    private final TaskStartupCheckService taskStartupCheckService;
    private final TaskMaintenanceService taskMaintenanceService;
    private final TeamReturnService teamReturnService;
    private final CommonBoxService commonBoxService;
    private final LeftTopStatusSwitchService leftTopStatusSwitchService;

    /**
     * Build a prototype task instance for the current window execution.
     *
     * @param gameContext mutable per-window game state.
     * @param taskStepExecutor shared template-step executor.
     * @param autoCombatService combat-state coordinator for the current bound window.
     * @param playerStateService player-state service that owns HP/MP detection and focused supply.
     * @param taskStartupCheckService startup gate that blocks unsupported role/task combinations.
     * @param taskMaintenanceService shared maintenance scheduler for broadcast prompts and
     *                               summon-skill cleanup.
     * @param teamReturnService return-team detector/clicker used after combat deaths.
     * @param commonBoxService delayed common-box consumer used before local return-team clicks.
     * @param leftTopStatusSwitchService CR107 left-top status switch guard consumed during follower
     *                                   pathing maintenance windows.
     */
    public AutoBattleTask(GameContext gameContext,
                          TaskStepExecutor taskStepExecutor,
                          AutoCombatService autoCombatService,
                          PlayerStateService playerStateService,
                          TaskStartupCheckService taskStartupCheckService,
                          TaskMaintenanceService taskMaintenanceService,
                          TeamReturnService teamReturnService,
                          CommonBoxService commonBoxService,
                          LeftTopStatusSwitchService leftTopStatusSwitchService) {
        super(gameContext, taskStepExecutor);
        this.autoCombatService = autoCombatService;
        this.playerStateService = playerStateService;
        this.taskStartupCheckService = taskStartupCheckService;
        this.taskMaintenanceService = taskMaintenanceService;
        this.teamReturnService = teamReturnService;
        this.commonBoxService = commonBoxService;
        this.leftTopStatusSwitchService = leftTopStatusSwitchService;
    }

    /**
     * @return stable task code used by the task registry and member reassignment policy.
     */
    @Override
    public String getTaskCode() {
        return "auto_battle";
    }

    /**
     * @return display name for logs/UI.
     */
    @Override
    public String getTaskName() {
        return "自动战斗";
    }

    /**
     * Execute without an explicit window execution context.
     *
     * @return final task result; delegates to {@link #execute(TaskExecutionContext)}.
     */
    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    /**
     * Run the auto-battle patrol loop for one window.
     *
     * @param executionContext nullable current window execution context. When null, the base task
     *                         resolves the active context from thread-local state.
     * @return {@link TaskRunResult#STOPPED} when the loop exits normally, or a blocked startup result
     * from {@link TaskStartupCheckService}. The loop mutates {@link GameContext} status/action state
     * and may submit focused input through downstream services only after their own safety gates.
     */
    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        TaskExecutionContext context = resolveExecutionContext(executionContext);
        log.info("====================================");
        log.info("启动自动战斗任务：{}", context.getLogPrefix());
        log.info("====================================");
        logWindowContext(context);

        TaskStartupCheckResult checkResult = taskStartupCheckService.checkAutoBattle(context);
        if (checkResult.isBlocked()) {
            log.info("自动战斗前置判断未通过：{}", checkResult.getReason());
            return checkResult.getBlockedResult();
        }
        log.info("自动战斗前置判断通过：{}", checkResult.getReason());

        gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        /*
         * Startup supply is task-owned, not window-owned. Auto-battle/member windows may start with
         * low MP before their first combat, so run one real HP/MP pass before entering the quiet
         * patrol loop.
         */
        playerStateService.performStartupFirstAidCheck(context);
        taskMaintenanceService.initializeForTaskStart(context, "auto-battle");
        autoCombatService.initializeForCurrentWindow();

        while (gameContext.getBotStatus() == GameContext.BotStatus.RUNNING) {
            context.throwIfStopRequested();
            AutoCombatService.TickResult combatResult = handleAutoCombatTick(context);
            /*
             * CR242 reopen: the member same-round return self-check after our own combat exit is
             * handled here, in this task's own branch. The shared EXIT_RECOVERED result must stay
             * intact for 修罗/五倍 task owners.
             */
            if (combatResult == AutoCombatService.TickResult.EXIT_RECOVERED
                    && gameContext.getCurrentActionState() == GameContext.ActionState.FREE) {
                maybeRunIdleMaintenance(context);
                sleepSafely(context, getPollingIntervalMs(context));
                continue;
            }
            if (combatResult != AutoCombatService.TickResult.NONE) {
                sleepSafely(context, getPollingIntervalMs(context));
                continue;
            }
            if (gameContext.getCurrentActionState() == GameContext.ActionState.FREE) {
                maybeRunIdleMaintenance(context);
            }
            sleepSafely(context, getPollingIntervalMs(context));
        }

        log.info("自动战斗任务结束：{}", context.getLogPrefix());
        return TaskRunResult.STOPPED;
    }

    /**
     * Run one auto-combat tick for the current window.
     *
     * @param context current bound window execution context.
     * @return the combat tick result.
     */
    private AutoCombatService.TickResult handleAutoCombatTick(TaskExecutionContext context) {
        return autoCombatService.handleCombatTick(context, "auto-battle", false);
    }

    /**
     * Request this task to stop.
     *
     * <p>The method only updates per-window {@link GameContext} state. Long-running loops still need
     * to observe the context stop token or interrupted flag in their own polling points.</p>
     */
    @Override
    public void stop() {
        log.info("收到停止自动战斗任务请求");
        gameContext.setBotStatus(GameContext.BotStatus.IDLE);
        gameContext.setCurrentActionState(GameContext.ActionState.FREE);
    }

    /**
     * Run low-frequency idle maintenance for auto-battle windows.
     */
    private void maybeRunIdleMaintenance(TaskExecutionContext context) {
        context.throwIfStopRequested();
        if (tryRunLocalTeamReturnSelfCheck(context)) {
            return;
        }
        /*
         * CR245: the leader-opened maintenance broadcast queue is its own authorization. A queued
         * member at the head runs exactly one confirm attempt and dequeues regardless of the result;
         * this path does not touch the idle quiet-member suppression below.
         */
        if (taskMaintenanceService.consumeMaintenanceBroadcastQueueTurnIfHead(
                context, "auto-battle:maintenance-broadcast-queue")) {
            return;
        }
        if (taskMaintenanceService.isPendingLocalSupportLeaderDetection(context)) {
            log.info("{} auto-battle idle maintenance deferred: pending local leader detection session={} requested={} role={}",
                    context.getLogPrefix(), context.getLocalTeamSessionKey(),
                    context.getRequestedTaskCode(), context.getWindowRole());
            return;
        }
        boolean localLeaderPaused = taskMaintenanceService.isLocalTeamLeaderPausedForMember(context);
        boolean localSupportSession = !localLeaderPaused
                && taskMaintenanceService.isLocalSupportMemberSession(context);
        if (!localSupportSession
                && !localLeaderPaused
                && teamReturnService.clickReturnTeamIfPresent(context, "auto-battle")) {
            return;
        }
        boolean followerSupportMode = isFollowerSupportMode(context);
        boolean requestedTeamTask = leftTopStatusSwitchService.isSupportedTaskCode(context.getRequestedTaskCode());
        boolean requireLocalSupportGate = localSupportSession && followerSupportMode;
        boolean requireLegacyTeamPathingGate = followerSupportMode
                && !localSupportSession
                && requestedTeamTask
                && !localLeaderPaused;
        boolean handleIdleMaintenanceBroadcast =
                !taskMaintenanceService.shouldSuppressIdleMaintenanceBroadcast(context);
        if (requireLocalSupportGate
                && taskMaintenanceService.isLocalTeamSupportCapabilityOpen(
                context, TeamSupportCapability.LEFT_TOP_STATUS)) {
            leftTopStatusSwitchService.consumeFollowerSafeWindow(context, context.getRequestedTaskCode());
            context.throwIfStopRequested();
        }
        TaskMaintenanceResult result = taskMaintenanceService.runOpportunisticMaintenance(context,
                TaskMaintenanceRequest.builder()
                        .sourceTask("auto-battle")
                        .handleMaintenanceBroadcast(handleIdleMaintenanceBroadcast)
                        /*
                         * Local follower-support members share the current UI-started leader session,
                         * not their original requested task label. This prevents a member that started
                         * as requested=wubei from waiting on wubei#80 after the leader has moved on to
                         * xiuluo_v2 in the same queue.
                         */
                        .cleanSummonSkill(!localLeaderPaused)
                        .oneSummonSkillPerTeamRound(requireLocalSupportGate || requireLegacyTeamPathingGate)
                        .maxSummonSkillCleanersPerTeamRound(summonSkillBudgetForRequestedTask(context.getRequestedTaskCode()))
                        .teamMaintenanceKey(requireLegacyTeamPathingGate
                                ? context.getRequestedTaskCode()
                                : null)
                        .requireOpenTeamMaintenanceWindow(requireLegacyTeamPathingGate)
                        .requiredLocalSupportCapability(requireLocalSupportGate
                                ? TeamSupportCapability.SUMMON_SKILL
                                : null)
                        .build());
        if (result.isHandled()) {
            log.info("{} auto-battle idle maintenance handled: status={} message={}",
                    context.getLogPrefix(), result.getStatus(), result.getMessage());
        }
    }

    private static int summonSkillBudgetForRequestedTask(String requestedTaskCode) {
        String normalized = requestedTaskCode == null ? "" : requestedTaskCode.trim().toLowerCase();
        return "xiuluo_v2".equals(normalized) ? 2 : 1;
    }

    /**
     * CR244 member self-check: the member owns its own return fact. When the return marker is
     * visible it adds its window id to the session pending-return set and runs the existing return
     * click chain; only a confirmed marker disappearance removes the entry. The leader no longer
     * opens a TEAM_RETURN capability window for this — the check runs on every applicable idle tick.
     */
    private boolean tryRunLocalTeamReturnSelfCheck(TaskExecutionContext context) {
        TaskMaintenanceService.TeamReturnCoordination coordination =
                taskMaintenanceService.resolveTeamReturnCoordination(context);
        if (!coordination.applicable()) {
            // Attribution lost while still in the set must not park the leader forever.
            taskMaintenanceService.clearPendingTeamReturnWindow(context,
                    "auto-battle:team-return-not-applicable");
            return false;
        }
        TeamReturnService.MemberReturnMarkerProbe probe =
                teamReturnService.probeMemberReturnMarker("auto-battle:self-check");
        if (probe == TeamReturnService.MemberReturnMarkerProbe.UNKNOWN) {
            // Capture/analysis failure is not evidence of anything. The pending-return set must
            // stay exactly as it is; the next idle tick re-probes.
            return false;
        }
        if (probe == TeamReturnService.MemberReturnMarkerProbe.ABSENT) {
            // A CONFIRMED marker disappearance is the only member-side remove condition. Click
            // failures and a still-visible marker keep the entry for the next tick.
            taskMaintenanceService.clearPendingTeamReturnWindow(context,
                    "auto-battle:return-marker-gone");
            return false;
        }
        taskMaintenanceService.markPendingTeamReturnWindow(context,
                "auto-battle:return-marker-present");

        String requestedTaskCode = context.getRequestedTaskCode();
        boolean consumedBox = commonBoxService.consumePendingBoxIfAllowed(
                context, requestedTaskCode, "auto-battle:team-return-release");
        if (consumedBox) {
            log.info("{} auto-battle local return release consumed common-box before return-team: requested={} role={}",
                    context.getLogPrefix(), requestedTaskCode, context.getWindowRole());
        }
        boolean clickedReturn = teamReturnService.clickReturnTeamIfPresent(
                context, "auto-battle:local-team-return-release");
        return consumedBox || clickedReturn;
    }

    /**
     * @param context current window context.
     * @return true when this auto-battle instance was assigned as a quiet member helper for a
     *         different leader task, rather than selected as standalone auto-battle by the user.
     */
    private boolean isFollowerSupportMode(TaskExecutionContext context) {
        if (context == null) {
            return false;
        }
        boolean member = "MEMBER".equalsIgnoreCase(context.getWindowRole());
        boolean localSupportSession = taskMaintenanceService.isLocalSupportMemberSession(context);
        String requestedTaskCode = context.getRequestedTaskCode();
        boolean reassignedFromLeaderTask = requestedTaskCode != null
                && !requestedTaskCode.equalsIgnoreCase(getTaskCode());
        return member && (localSupportSession || reassignedFromLeaderTask);
    }

    /**
     * Select the patrol interval for the next loop tick.
     *
     * @param context current window context.
     * @return three-second idle interval while free, otherwise the dynamic combat radar interval.
     */
    private long getPollingIntervalMs(TaskExecutionContext context) {
        if (gameContext.getCurrentActionState() == GameContext.ActionState.FREE) {
            if (autoCombatService.hasPendingFollowerFirstAidForCurrentWindow()) {
                return PENDING_FIRST_AID_POLL_INTERVAL_MS;
            }
            // CR245: a member queued in an open maintenance broadcast queue must reach its head
            // turn well inside the leader's 5s insurance cap; the 3s free patrol interval cannot.
            if (taskMaintenanceService.isInOpenMaintenanceBroadcastQueue(context)) {
                return PENDING_FIRST_AID_POLL_INTERVAL_MS;
            }
            return FREE_PATROL_INTERVAL_MS;
        }
        return autoCombatService.getDynamicPollingIntervalMs();
    }

    @Override
    protected TaskRetryPolicy getRetryPolicy(TaskExecutionContext context, com.bot.dhxy.task.template.TaskStep step) {
        return TaskRetryPolicy.none();
    }
}
