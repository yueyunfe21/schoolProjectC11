package com.bot.dhxy.task;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.service.SummonSkillService;
import com.bot.dhxy.service.TeamReturnService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.task.startup.TaskStartupCheckResult;
import com.bot.dhxy.task.startup.TaskStartupCheckService;
import com.bot.dhxy.task.template.BaseTaskTemplate;
import com.bot.dhxy.task.template.TaskStepExecutor;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Background auto-battle and follower-support task for one bound game window.
 *
 * <p>For real auto-battle windows, this task lets {@link AutoCombatService} own combat detection,
 * post-combat supply, and combat-panel preparation. For member windows reassigned from a leader-only
 * team task, it intentionally stays quiet: it only runs no-focus supply prechecks, handles return-
 * team signals, responds to maintenance broadcasts, and waits for combat. Any follower-support
 * path that may focus/click a client is guarded by {@link TaskTurnCoordinator}; this keeps a member
 * window from stealing focus while the leader still owns a post-combat task chain.</p>
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class AutoBattleTask extends BaseTaskTemplate {
    private static final long FREE_PATROL_INTERVAL_MS = 3000L;

    private final AutoCombatService autoCombatService;
    private final TaskStartupCheckService taskStartupCheckService;
    private final SummonSkillService summonSkillService;
    private final TeamReturnService teamReturnService;
    private final UICleanerService uiCleanerService;
    private final PlayerStateService playerStateService;
    private final TaskTurnCoordinator taskTurnCoordinator;
    private final BotProperties botProperties;

    private long lastSummonSkillCleanAt = 0L;
    private boolean followerSupportIdleLogged = false;
    private boolean followerSupportSupplyAttempted = false;

    /**
     * Build a prototype task instance for the current window execution.
     *
     * @param gameContext mutable per-window game state.
     * @param taskStepExecutor shared template-step executor.
     * @param autoCombatService combat-state coordinator for the current bound window.
     * @param taskStartupCheckService startup gate that blocks unsupported role/task combinations.
     * @param summonSkillService focused summon-skill maintenance service.
     * @param teamReturnService return-team detector/clicker used after combat deaths.
     * @param uiCleanerService maintenance broadcast handler; generic cleanup is not run blindly from
     *                         the idle loop.
     * @param playerStateService no-focus and focused HP/MP supply service.
     * @param taskTurnCoordinator global task-turn lock used before focused follower maintenance.
     * @param botProperties runtime feature switches and maintenance intervals.
     */
    public AutoBattleTask(GameContext gameContext,
                          TaskStepExecutor taskStepExecutor,
                          AutoCombatService autoCombatService,
                          TaskStartupCheckService taskStartupCheckService,
                          SummonSkillService summonSkillService,
                          TeamReturnService teamReturnService,
                          UICleanerService uiCleanerService,
                          PlayerStateService playerStateService,
                          TaskTurnCoordinator taskTurnCoordinator,
                          BotProperties botProperties) {
        super(gameContext, taskStepExecutor);
        this.autoCombatService = autoCombatService;
        this.taskStartupCheckService = taskStartupCheckService;
        this.summonSkillService = summonSkillService;
        this.teamReturnService = teamReturnService;
        this.uiCleanerService = uiCleanerService;
        this.playerStateService = playerStateService;
        this.taskTurnCoordinator = taskTurnCoordinator;
        this.botProperties = botProperties;
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
        followerSupportIdleLogged = false;
        followerSupportSupplyAttempted = false;
        lastSummonSkillCleanAt = botProperties.isSummonSkillCleanRunImmediatelyOnStart()
                ? 0L
                : System.currentTimeMillis();
        autoCombatService.initializeForCurrentWindow();

        while (gameContext.getBotStatus() == GameContext.BotStatus.RUNNING) {
            context.throwIfStopRequested();
            AutoCombatService.TickResult combatResult = handleAutoCombatTick(context);
            if (combatResult != AutoCombatService.TickResult.NONE) {
                sleepSafely(context, getPollingIntervalMs());
                continue;
            }
            if (gameContext.getCurrentActionState() == GameContext.ActionState.FREE
                    && maybeRunFollowerSupportSupply(context)) {
                sleepSafely(context, getPollingIntervalMs());
                continue;
            }
            if (maybeClickFollowerReturnTeam(context)) {
                sleepSafely(context, getPollingIntervalMs());
                continue;
            }
            if (gameContext.getCurrentActionState() == GameContext.ActionState.FREE
                    && !isFollowerSupportMode(context)) {
                maybeRunIdleMaintenance(context);
            }
            sleepSafely(context, getPollingIntervalMs());
        }

        log.info("自动战斗任务结束：{}", context.getLogPrefix());
        return TaskRunResult.STOPPED;
    }

    /**
     * Run one auto-combat tick, applying the leader task-turn gate for reassigned member windows.
     *
     * <p>The shared {@link AutoCombatService} can submit focused input when combat exits because it
     * performs post-combat HP/MP recovery. In follower-support mode that recovery must not bypass a
     * leader that is still holding the task turn for return-item or accept-NPC work. When the turn is
     * busy, this method skips the tick for this patrol cycle; if the local state is still IN_COMBAT,
     * it reports IN_COMBAT so the loop sleeps instead of running free-state maintenance.</p>
     *
     * @param context current bound window execution context.
     * @return the combat tick result, or IN_COMBAT/NONE when a follower tick is deferred because
     * another window currently owns the task turn.
     */
    private AutoCombatService.TickResult handleAutoCombatTick(TaskExecutionContext context) {
        if (!isFollowerSupportMode(context)) {
            return autoCombatService.handleCombatTick(context, "auto-battle", false);
        }

        AtomicReference<AutoCombatService.TickResult> result =
                new AtomicReference<>(AutoCombatService.TickResult.NONE);
        boolean ran = taskTurnCoordinator.tryRun("auto-battle:followerSupportCombatTick", () -> {
            context.throwIfStopRequested();
            result.set(autoCombatService.handleCombatTick(context, "auto-battle", false));
            return true;
        });

        if (!ran) {
            log.info("{} auto-battle follower support combat tick skipped: task turn busy",
                    context.getLogPrefix());
            return gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT
                    ? AutoCombatService.TickResult.IN_COMBAT
                    : AutoCombatService.TickResult.NONE;
        }
        return result.get();
    }

    /**
     * Click the follower return-team button only while the task turn is available.
     *
     * <p>The template scan itself is safe, but the click path focuses the member window. Keeping the
     * whole detect-and-click section under {@link TaskTurnCoordinator#tryRun(String,
     * java.util.function.Supplier)} prevents the member from interrupting the leader's post-combat
     * return flow. A missing button returns false without changing task state.</p>
     *
     * @param context current member window execution context.
     * @return true only when the return-team button was found and the click was queued.
     */
    private boolean maybeClickFollowerReturnTeam(TaskExecutionContext context) {
        if (!isFollowerSupportMode(context)) {
            return false;
        }

        AtomicBoolean clicked = new AtomicBoolean(false);
        boolean ran = taskTurnCoordinator.tryRun("auto-battle:followerSupportTeamReturn", () -> {
            context.throwIfStopRequested();
            clicked.set(teamReturnService.clickReturnTeamIfPresent(context, "auto-battle-follower-support"));
            return true;
        });
        if (!ran) {
            log.info("{} auto-battle follower support team return skipped: task turn busy",
                    context.getLogPrefix());
        }
        return clicked.get();
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
     * Give reassigned member windows one focused HP/MP supply chance after startup.
     *
     * <p>The no-focus precheck runs first so healthy windows never grab focus. If supply is needed,
     * the focused operation is wrapped in {@link TaskTurnCoordinator#tryRun(String,
     * java.util.function.Supplier)} so only one window performs the full supply sequence at a time.</p>
     */
    private boolean maybeRunFollowerSupportSupply(TaskExecutionContext context) {
        if (!isFollowerSupportMode(context) || followerSupportSupplyAttempted) {
            return false;
        }
        boolean completed = taskTurnCoordinator.tryRun("auto-battle:followerSupportSupply", () -> {
            context.throwIfStopRequested();
            log.info("{} auto-battle follower support supply acquired turn", context.getLogPrefix());
            if (!playerStateService.needsFirstAidSupplyNoFocus(context)) {
                followerSupportSupplyAttempted = true;
                log.info("{} auto-battle follower support supply skipped: no-focus precheck healthy",
                        context.getLogPrefix());
                return true;
            }
            log.info("{} auto-battle follower support supply needed: run focused supply",
                    context.getLogPrefix());
            playerStateService.performFirstAidCheckNow(context);
            followerSupportSupplyAttempted = true;
            log.info("{} auto-battle follower support supply finished", context.getLogPrefix());
            return true;
        });
        if (!completed) {
            log.info("{} auto-battle follower support supply skipped: task turn busy", context.getLogPrefix());
        }
        return completed;
    }

    /**
     * Run low-frequency idle maintenance for true auto-battle windows.
     *
     * <p>Follower support mode is excluded by the caller; members should not run personal summon
     * cleanup while they are only waiting for the leader's team task.</p>
     */
    private void maybeRunIdleMaintenance(TaskExecutionContext context) {
        context.throwIfStopRequested();
        if (teamReturnService.clickReturnTeamIfPresent(context, "auto-battle")) {
            return;
        }
        if (handleMaintenanceBroadcast(context)) {
            return;
        }
        maybeCleanSummonSkills(context);
    }

    /**
     * Determine whether this auto-battle task is acting as a quiet member helper.
     *
     * @param context current window execution context; null means no role/task reassignment data.
     * @return true only for MEMBER windows that were assigned auto-battle because the requested
     * team task belongs to the leader.
     */
    private boolean isFollowerSupportMode(TaskExecutionContext context) {
        if (context == null) {
            return false;
        }
        boolean member = "MEMBER".equalsIgnoreCase(context.getWindowRole());
        boolean reassignedFromMainTask = context.getRequestedTaskCode() != null
                && !context.getRequestedTaskCode().equalsIgnoreCase(getTaskCode());
        boolean supportMode = member && reassignedFromMainTask;
        if (supportMode && !followerSupportIdleLogged) {
            followerSupportIdleLogged = true;
            log.info("{} auto-battle follower support mode: limited idle maintenance until combat, requestedTask={}",
                    context.getLogPrefix(), context.getRequestedTaskCode());
        }
        return supportMode;
    }

    private boolean handleMaintenanceBroadcast(TaskExecutionContext context) {
        boolean handled = uiCleanerService.handleMaintenanceBroadcast("auto-battle");
        if (handled) {
            log.info("{} auto-battle idle maintenance handled maintenance broadcast", context.getLogPrefix());
        }
        return handled;
    }

    /**
     * Perform summon-skill cleanup when the configured interval is due.
     *
     * <p>The cleanup changes {@link GameContext.ActionState} to INTERACTING and only updates
     * {@link #lastSummonSkillCleanAt} after a successful final click. Failed attempts deliberately do
     * not move the timer so the next idle window can retry.</p>
     */
    private void maybeCleanSummonSkills(TaskExecutionContext context) {
        if (!botProperties.isSummonSkillCleanEnabled()) {
            return;
        }
        long intervalMs = botProperties.getSummonSkillCleanIntervalMs();
        if (intervalMs <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastSummonSkillCleanAt > 0 && now - lastSummonSkillCleanAt < intervalMs) {
            return;
        }
        if (gameContext.getCurrentActionState() != GameContext.ActionState.FREE) {
            return;
        }

        context.throwIfStopRequested();
        GameContext.ActionState previousState = gameContext.getCurrentActionState();
        gameContext.setCurrentActionState(GameContext.ActionState.INTERACTING);
        boolean success = false;
        try {
            log.info("{} auto-battle maintenance: start summon skill clean", context.getLogPrefix());
            success = summonSkillService.cleanSummonSkillsOnce();
            log.info("{} auto-battle maintenance: summon skill clean finished success={}",
                    context.getLogPrefix(), success);
        } finally {
            if (success) {
                lastSummonSkillCleanAt = System.currentTimeMillis();
            }
            if (gameContext.getCurrentActionState() == GameContext.ActionState.INTERACTING) {
                gameContext.setCurrentActionState(previousState);
            }
        }
    }

    /**
     * Select the patrol interval for the next loop tick.
     *
     * @return three-second idle interval while free, otherwise the dynamic combat radar interval.
     */
    private long getPollingIntervalMs() {
        if (gameContext.getCurrentActionState() == GameContext.ActionState.FREE) {
            return FREE_PATROL_INTERVAL_MS;
        }
        return autoCombatService.getDynamicPollingIntervalMs();
    }

    @Override
    protected TaskRetryPolicy getRetryPolicy(TaskExecutionContext context, com.bot.dhxy.task.template.TaskStep step) {
        return TaskRetryPolicy.none();
    }
}
