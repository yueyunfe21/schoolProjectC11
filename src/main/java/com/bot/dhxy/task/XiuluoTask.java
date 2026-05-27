package com.bot.dhxy.task;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.navigation.MapNavigationRequest;
import com.bot.dhxy.model.navigation.NpcNavigationRequest;
import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.model.quest.QuestDetailCapture;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.BattleRadarService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.service.NpcClickService;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.service.QuestManagerService;
import com.bot.dhxy.service.TeamReturnService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.task.hotstart.TaskHotStartScreenState;
import com.bot.dhxy.task.hotstart.TaskHotStartService;
import com.bot.dhxy.task.hotstart.TaskHotStartSnapshot;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.vision.ObjectiveTextRecognitionService;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Leader-only Xiuluo task flow for one bound game window.
 *
 * <p>The task owns the leader chain: accept/read objective, path near the monster coordinate, click
 * the target, wait for battle, return after combat, and optionally wait for members to rejoin.
 * Member windows should be reassigned to {@code auto_battle}; this task assumes it can hold the task
 * turn during acceptance and target-click preparation, then yields only when navigation/combat makes
 * it safe for other windows to perform quiet maintenance.</p>
 */
@Component
@Scope(value="prototype")
public class XiuluoTask implements GameTask {
    private static final Logger log = LoggerFactory.getLogger(XiuluoTask.class);
    private static final String TASK_CODE = "xiuluo";
    private static final String TASK_NAME = "修罗";
    private static final String START_MAP_NAME = "灵兽村";
    private static final String ACCEPT_NPC_NAME = "灵兽村使者";
    private static final int ACCEPT_NPC_X = 112;
    private static final int ACCEPT_NPC_Y = 93;
    private static final int EXIT_POINT_X = 11;
    private static final int EXIT_POINT_Y = 8;
    private static final String XIULUO_TARGET_KEYWORD = "修罗";
    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/xiuluo_accept_xianlaiwu.png";
    private static final String UNDER_FIVE_CONFIRM_TEMPLATE = "images/template/dialog/xiuluo_underfive_confirm.png";
    private static final String UNDER_FIVE_WAIT_TEMPLATE = "images/template/dialog/xiuluo_underfive_wait.png";
    private static final String ENTER_BATTLE_TEMPLATE = "images/template/dialog/xiuluo_enter_battle_kanda.png";
    private static final String NPC_TAG_TEMPLATE = "images/template/npc/npc_tag.png";
    private static final String RETURN_ITEM_TEMPLATE = "bag/xiuluo_return_item.png";
    private static final String OPTION_ACCEPT_TASK = "accept-task";
    private static final String OPTION_UNDER_FIVE_CONFIRM = "under-five-confirm";
    private static final String OPTION_UNDER_FIVE_WAIT = "under-five-wait";
    private static final String OPTION_ENTER_BATTLE = "enter-battle";
    private static final int ACCEPT_OBJECTIVE_MAX_ATTEMPTS = 4;
    private static final int OBJECTIVE_FAILURE_ACCEPT_RECOVERY_ATTEMPTS = 1;
    private final GameContext gameContext;
    private final BotProperties botProperties;
    private final PlayerStateService playerStateService;
    private final NavigationService navigationService;
    private final NpcClickService npcClickService;
    private final DialogService dialogService;
    private final BagService bagService;
    private final AutoCombatService autoCombatService;
    private final BattleRadarService battleRadarService;
    private final GameStateUtil gameStateUtil;
    private final TaskTransactionRunner taskTransactionRunner;
    private final QuestManagerService questManagerService;
    private final ObjectiveTextRecognitionService objectiveTextRecognitionService;
    private final TeamReturnService teamReturnService;
    private final TaskHotStartService taskHotStartService;
    private final UICleanerService uiCleanerService;
    private final CoordinateHelper coordinateHelper;

    /**
     * @return stable task code used by startup policy and UI selection.
     */
    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    /**
     * @return display name shown in logs and the task selector.
     */
    @Override
    public String getTaskName() {
        return "修罗";
    }

    /**
     * Execute without an explicit window execution context.
     *
     * @return final task result; delegates to {@link #execute(TaskExecutionContext)}.
     */
    @Override
    public TaskRunResult execute() {
        return this.execute(null);
    }

    /**
     * Run Xiuluo rounds for the current leader window.
     *
     * @param executionContext nullable window execution context. When provided, stop checks and log
     *                         prefixes are tied to that registered window; null is accepted for
     *                         legacy single-window execution.
     * @return SUCCESS when the configured run count is reached, STOPPED on stop/interruption, or
     * FAILED when a navigation/dialog/combat step cannot recover.
     */
    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        TaskExecutionContext context = this.resolveExecutionContext(executionContext);
        log.info("====================================");
        log.info("[xiuluo] task started: maxRuns={} allowUnderFive={}",
                this.botProperties.getXiuluoMaxRuns(), this.botProperties.isXiuluoAllowUnderFiveMembers());
        log.info("====================================");
        int maxRuns = this.botProperties.getXiuluoMaxRuns();
        int completedRuns = 0;
        this.gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        try {
            this.autoCombatService.initializeForCurrentWindow();
            TaskRunResult prepareResult = this.prepareBeforeRun(context);
            if (prepareResult != TaskRunResult.SUCCESS) {
                return prepareResult;
            }
            while (maxRuns <= 0 || completedRuns < maxRuns) {
                this.checkpoint(context);
                int round = completedRuns + 1;
                log.info("[xiuluo] start round {}/{}", (Object)round, maxRuns <= 0 ? "unlimited" : Integer.valueOf(maxRuns));
                TaskRunResult result = this.runOneRound(context, round, completedRuns == 0);
                if (result != TaskRunResult.SUCCESS) {
                    this.gameContext.setBotStatus(result == TaskRunResult.STOPPED ? GameContext.BotStatus.IDLE : GameContext.BotStatus.ERROR);
                    TaskRunResult taskRunResult = result;
                    return taskRunResult;
                }
                log.info("[xiuluo] round {} finished, completed={}", (Object)round, (Object)(++completedRuns));
            }
            this.gameContext.setBotStatus(GameContext.BotStatus.IDLE);
            log.info("[xiuluo] reached configured run count: {}", (Object)completedRuns);
            TaskRunResult taskRunResult = TaskRunResult.SUCCESS;
            return taskRunResult;
        }
        finally {
            this.taskTransactionRunner.forceReleaseTurn("xiuluo:execute-finished");
        }
    }

    /**
     * Ensure the Xiuluo task always has a non-null execution context.
     *
     * <p>Most JavaFX multi-window runs pass a fully populated window context, but legacy single-window
     * debug calls may still invoke {@link #execute()} directly. The shared auto-combat coordinator
     * expects a non-null context for stop checks and log prefixes, so this method creates a minimal
     * context without native-window ownership when the caller does not provide one.</p>
     *
     * @param executionContext nullable caller-provided context.
     * @return the original context, or a minimal legacy-safe context for direct execution.
     */
    private TaskExecutionContext resolveExecutionContext(TaskExecutionContext executionContext) {
        if (executionContext != null) {
            return executionContext;
        }
        return TaskExecutionContext.builder()
                .taskCode(TASK_CODE)
                .taskName(TASK_NAME)
                .retryPolicy(TaskRetryPolicy.none())
                .startedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Run one leader-only startup maintenance pass before the Xiuluo round loop.
     *
     * <p>This method sends real input through the lower services because checking/using
     * sheyaoxiang may open the bag and click an item. It is intentionally executed once per task
     * start, before accepting the first objective, so Xiuluo has the same anti-encounter baseline as
     * Five Ring without adding bag checks between every pathing step.</p>
     *
     * @param context nullable task execution context for stop checks and window-scoped input.
     * @return SUCCESS when startup maintenance completed, STOPPED if the task was interrupted.
     */
    private TaskRunResult prepareBeforeRun(TaskExecutionContext context) {
        TaskTransactionOutcome outcome = this.taskTransactionRunner.run(
                "xiuluo:prepareBeforeRun",
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
                    this.checkpoint(context);
                    log.info("[xiuluo prepare] check sheyaoxiang status before first round");
                    this.playerStateService.ensureSheYaoXiangActiveForLeaderTask("xiuluo:prepare", context);
                    this.checkpoint(context);
                    return TaskTransactionResult.READY_TO_CONTINUE;
                });
        if (outcome.result() == TaskTransactionResult.STOPPED) {
            return TaskRunResult.STOPPED;
        }
        return outcome.reachedExpectedResult() ? TaskRunResult.SUCCESS : TaskRunResult.FAILED;
    }

    /**
     * Request the Xiuluo loop to stop.
     *
     * <p>This updates the shared window game status only; active inner loops still honor the stop
     * token through {@link #checkpoint(TaskExecutionContext)}.</p>
     */
    @Override
    public void stop() {
        log.info("[xiuluo] stop requested");
        this.gameContext.setBotStatus(GameContext.BotStatus.IDLE);
    }

    /**
     * Debug-only entry used by the mock Xiuluo task to skip accepting a task and start from a known
     * target map/coordinate.
     *
     * @param context current leader window context for stop checks and task-turn ownership.
     * @param mapName logical target map name returned by the mocked objective.
     * @param x logical in-game target X coordinate.
     * @param y logical in-game target Y coordinate.
     * @return final result from the normal target-ready Xiuluo flow.
     */
    TaskRunResult executeDebugMockObjective(TaskExecutionContext context, String mapName, int x, int y) {
        XiuluoObjective objective = new XiuluoObjective(mapName, x, y, mapName + "(" + x + "," + y + ")");
        log.info("[xiuluo-debug-mock-objective] start from mocked existing objective: targetMap={} target=({}, {})", new Object[]{objective.mapName(), objective.x(), objective.y()});
        this.gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        try {
            TaskRunResult result;
            PlayerCharacter me = this.gameContext.getMe();
            if (me != null) {
                me.setCurrentMapName(objective.mapName());
                log.info("[xiuluo-debug-mock-objective] mock current map as target for target-map-ready test: {}", (Object)objective.mapName());
            }
            this.gameContext.setBotStatus((result = this.runObjectiveReadyFlow(context, objective, 1, true)) == TaskRunResult.SUCCESS ? GameContext.BotStatus.IDLE : GameContext.BotStatus.ERROR);
            TaskRunResult taskRunResult = result;
            return taskRunResult;
        }
        finally {
            this.taskTransactionRunner.forceReleaseTurn("xiuluo-debug-mock-objective:execute-finished");
        }
    }

    private TaskRunResult runOneRound(TaskExecutionContext context, int round, boolean allowExistingTaskPanelHotStart) {
        /*
         * Hot-start before accepting a new objective: reuse an existing task/story/combat state when
         * safe, otherwise fall back to the formal accept-NPC chain.
         */
        this.checkpoint(context);
        XiuluoHotStartResult roundStart = this.prepareObjectiveForPathing(context, allowExistingTaskPanelHotStart);
        if (roundStart.state() == XiuluoHotStartState.STOPPED) {
            return TaskRunResult.STOPPED;
        }
        if (roundStart.state() == XiuluoHotStartState.FAILED) {
            return this.interrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
        }
        if (roundStart.state() == XiuluoHotStartState.IN_COMBAT || roundStart.state() == XiuluoHotStartState.ENTER_BATTLE_CONFIRMED) {
            if (!this.waitCombatToFinish(context, round)) {
                return this.interrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
            }
            return this.finishRoundAfterCombat(context);
        }
        XiuluoObjective objective = roundStart.objective();
        if (objective == null) {
            return this.interrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
        }
        log.info("[xiuluo] objective ready, trigger formal pathing: source={} targetMap={} target=({}, {}) raw={}", new Object[]{roundStart.state(), objective.mapName(), objective.x(), objective.y(), objective.rawText()});
        TaskRunResult result = this.runObjectiveReadyFlow(context, objective, round);
        if (result == TaskRunResult.SUCCESS || result == TaskRunResult.STOPPED) {
            return result;
        }
        return this.retryRoundFromAcceptNpcAfterObjectiveFailure(context, round, result);
    }

    private TaskRunResult runObjectiveReadyFlow(TaskExecutionContext context, XiuluoObjective objective, int round) {
        return this.runObjectiveReadyFlow(context, objective, round, false);
    }

    private TaskRunResult runObjectiveReadyFlow(TaskExecutionContext context, XiuluoObjective objective, int round, boolean assumeTargetMapReady) {
        if (assumeTargetMapReady) {
            PlayerCharacter me = this.gameContext.getMe();
            if (me != null) {
                me.setCurrentMapName(objective.mapName());
            }
            log.info("[xiuluo] target map assumed ready; skip formal pathing and target-map wait: objective={}", (Object)objective);
            if (!this.navigateToObjectiveCoordinateWithCleanupRetry(context, objective, "assume-target-map")) {
                log.warn("[xiuluo] failed to navigate in target map: objective={}", (Object)objective);
                return this.interrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
            }
        } else {
            this.checkpoint(context);
            boolean alreadyInTargetMap = this.gameStateUtil.confirmCurrentMap(
                    objective.mapName(), 0L, "xiuluo:formalPathingPrecheck");
            PlayerCharacter me = this.gameContext.getMe();
            String currentMap = me == null ? null : me.getCurrentMapName();
            log.info("[xiuluo] current map precheck before formal pathing: current={} target={} matched={}",
                    currentMap, objective.mapName(), alreadyInTargetMap);
            if (alreadyInTargetMap) {
                log.info("[xiuluo] already in target map, skip world map pathing: targetMap={}", objective.mapName());
            }
            if (!this.navigationService.navigateToMap(MapNavigationRequest.toMap(objective.mapName())).success()) {
                log.warn("[xiuluo] map navigation to objective failed: objective={}", (Object)objective);
                return this.interrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
            }
            if (!this.navigateToObjectiveCoordinateWithCleanupRetry(context, objective, "formal-pathing")) {
                log.warn("[xiuluo] current-map objective navigation failed, try target-map cleanup retry if already arrived: objective={}", (Object)objective);
                if (!this.retryObjectiveCoordinateIfAlreadyInTargetMap(context, objective, "formal-pathing-failed")) {
                    return this.interrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
                }
            }
        }
        if (!this.clickTargetAndEnterBattle(context, objective)) {
            return this.interrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
        }
        if (!this.waitCombatToFinish(context, round)) {
            return this.interrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
        }
        return this.finishRoundAfterCombat(context);
    }

    /**
     * Recover a Xiuluo round by returning to the accept-NPC chain after target execution fails.
     *
     * <p>Failing to approach/click the monster does not mean the whole task should exit. The safe
     * human fallback is to clear blocking UI, go back through the accept NPC, obtain a fresh
     * objective, and run the same objective-ready flow again. This method is intentionally bounded:
     * if the fresh accept chain also fails, the task returns FAILED so the UI/logs expose the real
     * repeated problem instead of looping forever.</p>
     *
     * @param context current task context used for stop checks.
     * @param round one-based Xiuluo round number, used only for logs and combat wait calls.
     * @param previousResult result that triggered recovery; normally FAILED.
     * @return SUCCESS if the fresh accept chain completes the round, STOPPED on interruption, or
     *         FAILED when the bounded recovery cannot obtain/complete a fresh objective.
     */
    private TaskRunResult retryRoundFromAcceptNpcAfterObjectiveFailure(TaskExecutionContext context,
                                                                      int round,
                                                                      TaskRunResult previousResult) {
        for (int attempt = 1; attempt <= OBJECTIVE_FAILURE_ACCEPT_RECOVERY_ATTEMPTS; attempt++) {
            this.checkpoint(context);
            log.warn("[xiuluo] objective flow failed; clean UI and return to accept NPC recovery attempt={}/{} previousResult={}",
                    attempt, OBJECTIVE_FAILURE_ACCEPT_RECOVERY_ATTEMPTS, previousResult);
            this.uiCleanerService.cleanUpAll();
            TaskSleep.sleepOrStop(context, 500L, "Xiuluo task interrupted");

            XiuluoObjective recoveredObjective = this.obtainObjectiveForRoundStart(context);
            if (recoveredObjective == null) {
                log.warn("[xiuluo] accept-NPC recovery failed to obtain objective attempt={}/{}",
                        attempt, OBJECTIVE_FAILURE_ACCEPT_RECOVERY_ATTEMPTS);
                continue;
            }

            log.info("[xiuluo] accept-NPC recovery obtained objective: attempt={} targetMap={} target=({}, {}) raw={}",
                    attempt, recoveredObjective.mapName(), recoveredObjective.x(), recoveredObjective.y(),
                    recoveredObjective.rawText());
            TaskRunResult recoveredResult = this.runObjectiveReadyFlow(context, recoveredObjective, round);
            if (recoveredResult == TaskRunResult.SUCCESS || recoveredResult == TaskRunResult.STOPPED) {
                return recoveredResult;
            }
            previousResult = recoveredResult;
        }

        return this.interrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
    }

    /**
     * Retry current-map objective-coordinate navigation after clearing blocking UI.
     *
     * <p>Xiuluo target navigation has two layers: generic map routing gets the leader to the target
     * map, then current-map mini-map routing moves to the task coordinate. If the latter fails, the common
     * cause is a stale map/search/dialog overlay blocking Alt+1 or the mini-map click. The task layer
     * owns this retry because it knows the objective is still valid and that the next safe action is
     * to clear UI before trying the current-map approach again.</p>
     *
     * @param context current execution context used for stop checkpoints.
     * @param objective Xiuluo objective whose map name and logical monster coordinate remain valid.
     * @param source diagnostic source for logs.
     * @return true when either the first coordinate navigation succeeds or the cleanup retry succeeds.
     */
    private boolean navigateToObjectiveCoordinateWithCleanupRetry(TaskExecutionContext context,
                                                                 XiuluoObjective objective,
                                                                 String source) {
        this.checkpoint(context);
        MapCoordinate approach = this.coordinateHelper.calculateApproachCoordinate(
                objective.mapName(), objective.x(), objective.y());
        log.info("[xiuluo] navigate to objective approach coordinate: source={} objective=({}, {}) approach=({}, {}) map={}",
                source, objective.x(), objective.y(), approach.getX(), approach.getY(), objective.mapName());
        if (this.navigationService.navigateInCurrentMap(approach.getX(), approach.getY()).success()) {
            return true;
        }

        /*
         * Do one bounded retry only. More retries belong to a higher-level task restart decision;
         * this helper only removes UI that may have blocked the first mini-map click.
         */
        this.checkpoint(context);
        log.warn("[xiuluo] current-map objective navigation failed; clean UI and retry once: source={} objective={}",
                source, objective);
        this.uiCleanerService.cleanUpAll();
        TaskSleep.sleepOrStop(context, 300L, "Xiuluo task interrupted");
        this.checkpoint(context);
        return this.navigationService.navigateInCurrentMap(approach.getX(), approach.getY()).success();
    }

    /**
     * Recover from a failed map+coordinate navigation when the leader has already reached the target map.
     *
     * <p>World-map routing can succeed while the current-map coordinate click fails. In that case Xiuluo should not
     * end immediately; it should clear stale UI and retry only the current-map mini-map coordinate.</p>
     *
     * @param context current execution context used for stop checkpoints.
     * @param objective Xiuluo objective being pursued.
     * @param source diagnostic source for logs.
     * @return true when recovery is possible and current-map retry succeeds; false otherwise.
     */
    private boolean retryObjectiveCoordinateIfAlreadyInTargetMap(TaskExecutionContext context,
                                                                XiuluoObjective objective,
                                                                String source) {
        this.checkpoint(context);
        boolean reachedTargetMap = this.gameStateUtil.confirmCurrentMapFresh(
                objective.mapName(), 0L, "xiuluo:retryObjectiveCoordinate:" + source);
        PlayerCharacter me = this.gameContext.getMe();
        String currentMap = me == null ? null : me.getCurrentMapName();
        if (!reachedTargetMap) {
            log.warn("[xiuluo] skip current-map cleanup retry because target map is not reached: source={} current={} objective={}",
                    source, currentMap, objective);
            return false;
        }
        log.info("[xiuluo] target map reached after navigation failure; retry current-map coordinate: source={} current={} objective={}",
                source, currentMap, objective);
        return this.navigateToObjectiveCoordinateWithCleanupRetry(context, objective, source);
    }

    private XiuluoHotStartResult prepareObjectiveForPathing(TaskExecutionContext context, boolean allowExistingTaskPanelHotStart) {
        boolean sharedState;
        AtomicReference<XiuluoHotStartResult> prepared = new AtomicReference<XiuluoHotStartResult>(XiuluoHotStartResult.failed());
        TaskTransactionOutcome outcome = this.taskTransactionRunner.run("xiuluo:prepareObjectiveForPathing", TaskTransactionResult.READY_TO_CONTINUE, TaskYieldPolicy.CONTINUE_CHAIN, () -> {
            XiuluoObjective objective;
            this.checkpoint(context);
            /*
             * On true startup, Xiuluo must be able to resume an already accepted task from the
             * Quest Manager instead of blindly navigating back to the accept NPC. After a completed
             * round we deliberately skip this fallback because stale task-panel text can belong to
             * the previous round before the next accept action.
             */
            XiuluoHotStartResult hotStart = this.tryTakeOverCurrentScreen(
                    context, "round-start", allowExistingTaskPanelHotStart);
            if (hotStart.state() == XiuluoHotStartState.FAILED) {
                prepared.set(hotStart);
                return TaskTransactionResult.FAILED;
            }
            if (hotStart.state() == XiuluoHotStartState.IN_COMBAT || hotStart.state() == XiuluoHotStartState.ENTER_BATTLE_CONFIRMED) {
                prepared.set(hotStart);
                return TaskTransactionResult.SHARED_STATE_TRIGGERED;
            }
            XiuluoObjective xiuluoObjective = objective = hotStart.objective() == null ? this.obtainObjectiveForRoundStart(context) : hotStart.objective();
            if (objective == null && allowExistingTaskPanelHotStart) {
                objective = this.tryReadObjectiveFromTaskPanel(context, "round-start:existing-task-after-accept-failed");
            }
            if (objective == null) {
                prepared.set(XiuluoHotStartResult.failed());
                return TaskTransactionResult.RETRYABLE_ERROR;
            }
            prepared.set(XiuluoHotStartResult.objectiveReady(objective));
            return TaskTransactionResult.READY_TO_CONTINUE;
        });
        XiuluoHotStartResult result = prepared.get();
        boolean bl = sharedState = result.state() == XiuluoHotStartState.IN_COMBAT || result.state() == XiuluoHotStartState.ENTER_BATTLE_CONFIRMED;
        /*
         * TaskTransactionRunner converts cooperative stop exceptions into a STOPPED transaction
         * result. Preserve that state explicitly; otherwise the caller sees a generic FAILED hot
         * start and the UI keeps showing an error after a normal user stop.
         */
        if (outcome.result() == TaskTransactionResult.STOPPED) {
            log.info("[xiuluo] prepare objective transaction stopped: completed={} prepared={}",
                    outcome.completed(), result);
            return XiuluoHotStartResult.stopped();
        }
        if (!outcome.reachedExpectedResult() && !sharedState) {
            log.warn("[xiuluo] prepare objective transaction failed: result={} completed={} prepared={}", new Object[]{outcome.result(), outcome.completed(), result});
            return XiuluoHotStartResult.failed();
        }
        log.info("[xiuluo] prepare objective transaction outcome: result={} completed={} keepTurn={}", new Object[]{outcome.result(), outcome.completed(), outcome.reachedExpectedResult()});
        return result;
    }

    private TaskRunResult finishRoundAfterCombat(TaskExecutionContext context) {
        log.info("[xiuluo] post-combat chain: start return item, keep turn until next formal pathing starts");
        if (!this.useReturnItem(context)) {
            return this.interrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
        }
        this.teamReturnService.waitForMembersReturnIfNeeded(context, "xiuluo:return-item-safe-point");
        log.info("[xiuluo] post-combat chain: return handled, next loop should accept next round before yielding");
        return TaskRunResult.SUCCESS;
    }

    private XiuluoHotStartResult tryTakeOverCurrentScreen(TaskExecutionContext context, String source, boolean allowExistingTaskPanelFallback) {
        TaskHotStartSnapshot snapshot = this.taskHotStartService.snapshot(TASK_CODE, source);
        if (snapshot.state() == TaskHotStartScreenState.IN_COMBAT) {
            this.logHotStartAction(source, "IN_COMBAT", "WAIT_COMBAT_THEN_RETURN", null);
            log.info("[xiuluo] hot-start takeover: source={} state=IN_COMBAT", (Object)source);
            return XiuluoHotStartResult.inCombat();
        }
        if (snapshot.state() == TaskHotStartScreenState.STORY_DIALOG) {
            XiuluoObjective objective = this.tryReadCurrentStoryObjective(context, source, 2);
            if (objective != null) {
                this.logHotStartAction(source, "STORY_DIALOG", "READ_STORY_OBJECTIVE", objective);
                log.info("[xiuluo] hot-start takeover: source={} state=STORY objective={}", (Object)source, (Object)objective);
                return XiuluoHotStartResult.objectiveReady(objective);
            }
            this.logHotStartAction(source, "STORY_DIALOG", "STORY_OBJECTIVE_NOT_FOUND", null);
            return XiuluoHotStartResult.none();
        }
        if (snapshot.state() == TaskHotStartScreenState.OPTION_DIALOG) {
            return this.handleVisibleOptionHotStart(context, source);
        }
        if (allowExistingTaskPanelFallback) {
            XiuluoObjective existingObjective = this.tryReadObjectiveFromTaskPanel(context, source + ":existing-task");
            if (existingObjective != null) {
                this.logHotStartAction(source, "NONE", "READ_EXISTING_TASK_PANEL", existingObjective);
                log.info("[xiuluo] hot-start takeover: source={} state=EXISTING_TASK objective={}", (Object)source, (Object)existingObjective);
                return XiuluoHotStartResult.objectiveReady(existingObjective);
            }
        } else {
            this.logHotStartAction(source, "NONE", "SKIP_EXISTING_TASK_PANEL_AFTER_COMPLETED_ROUND", null);
        }
        this.logHotStartAction(source, "NONE", "NO_TAKEOVER", null);
        return XiuluoHotStartResult.none();
    }

    private XiuluoHotStartResult handleVisibleOptionHotStart(TaskExecutionContext context, String source) {
        ArrayList<GreenTemplateClickSpec> specs = new ArrayList<GreenTemplateClickSpec>();
        specs.add(new GreenTemplateClickSpec(OPTION_ACCEPT_TASK, ACCEPT_OPTION_TEMPLATE, -5, 80, 4));
        /*
         * "看打!" is a very short option. A wide +/-30px random X offset can land outside the
         * clickable green text, leaving the dialog open while the task believes battle was
         * confirmed. Keep this click close to the template center; longer accept-task options use
         * their own wider offsets in the accept flow.
         */
        specs.add(new GreenTemplateClickSpec(OPTION_ENTER_BATTLE, ENTER_BATTLE_TEMPLATE, -6, 6, 4));
        if (this.botProperties.isXiuluoAllowUnderFiveMembers()) {
            specs.add(new GreenTemplateClickSpec(OPTION_UNDER_FIVE_CONFIRM, UNDER_FIVE_CONFIRM_TEMPLATE, 24, 24, 4));
        } else {
            specs.add(new GreenTemplateClickSpec(OPTION_UNDER_FIVE_WAIT, UNDER_FIVE_WAIT_TEMPLATE, 24, 24, 4));
        }
        String matched = this.dialogService.clickFirstKnownOptionGreenTemplateDirectForExclusive(specs, "xiuluo:hot-start-option:" + source);
        if (matched == null) {
            this.logHotStartAction(source, "OPTION_DIALOG", "UNRECOGNIZED_OPTION", null);
            return XiuluoHotStartResult.none();
        }
        if (OPTION_ACCEPT_TASK.equals(matched)) {
            TaskSleep.sleepOrStop(context, 250L, "Xiuluo task interrupted");
            XiuluoObjective objective = this.readObjectiveAfterAcceptOptionClicked(context, source);
            if (objective != null) {
                this.logHotStartAction(source, "OPTION_DIALOG", "ACCEPT_TASK", objective);
                log.info("[xiuluo] hot-start takeover: source={} state=ACCEPTED objective={}", (Object)source, (Object)objective);
                return XiuluoHotStartResult.objectiveReady(objective);
            }
            this.logHotStartAction(source, "OPTION_DIALOG", "ACCEPT_TASK_OBJECTIVE_NOT_FOUND", null);
            return XiuluoHotStartResult.none();
        }
        if (OPTION_UNDER_FIVE_CONFIRM.equals(matched)) {
            TaskSleep.sleepOrStop(context, 900L, "Xiuluo task interrupted");
            XiuluoObjective objective = this.tryReadCurrentStoryObjective(context, source, 3);
            if (objective != null) {
                this.logHotStartAction(source, "OPTION_DIALOG", "HANDLE_UNDER_FIVE", objective);
                log.info("[xiuluo] hot-start takeover: source={} state=UNDER_FIVE objective={}", (Object)source, (Object)objective);
                return XiuluoHotStartResult.objectiveReady(objective);
            }
            this.logHotStartAction(source, "OPTION_DIALOG", "UNDER_FIVE_OBJECTIVE_NOT_FOUND", null);
            return XiuluoHotStartResult.none();
        }
        if (OPTION_UNDER_FIVE_WAIT.equals(matched)) {
            TaskSleep.sleepOrStop(context, 600L, "Xiuluo task interrupted");
            this.logHotStartAction(source, "OPTION_DIALOG", "DECLINE_UNDER_FIVE", null);
            return XiuluoHotStartResult.failed();
        }
        this.logHotStartAction(source, "OPTION_DIALOG", "CONFIRM_ENTER_BATTLE", null);
        log.info("[xiuluo] hot-start takeover: source={} state=ENTER_BATTLE_CONFIRMED", (Object)source);
        return XiuluoHotStartResult.enterBattleConfirmed();
    }

    private void logHotStartAction(String source, String screenState, String action, XiuluoObjective objective) {
        if (objective == null) {
            log.info("[XIULUO_HOT_START] source={} screen={} action={}", new Object[]{source, screenState, action});
            return;
        }
        log.info("[XIULUO_HOT_START] source={} screen={} action={} targetMap={} target=({}, {}) raw={}", new Object[]{source, screenState, action, objective.mapName(), objective.x(), objective.y(), objective.rawText()});
    }

    private XiuluoObjective obtainObjectiveForRoundStart(TaskExecutionContext context) {
        long startedAt = System.currentTimeMillis();
        for (int attempt = 1; attempt <= 4; ++attempt) {
            this.checkpoint(context);
            AcceptDialogProbeResult currentDialog = this.tryGetObjectiveFromVisibleXiuluoDialog(context, "round-start-attempt" + attempt);
            if (currentDialog.state() == AcceptDialogProbeState.OBJECTIVE_READY) {
                log.info("[xiuluo][accept-flow] objective ready from current dialog attempt={} elapsedMs={}", (Object)attempt, (Object)(System.currentTimeMillis() - startedAt));
                return currentDialog.objective();
            }
            if (currentDialog.state() == AcceptDialogProbeState.BLOCKING_DIALOG_UNHANDLED) {
                this.cleanBlockingAcceptDialog(context, "round-start-attempt" + attempt);
                continue;
            }
            if (!this.navigationService.navigateToNPC(NpcNavigationRequest.builder()
                    .targetMapName(START_MAP_NAME)
                    .targetX(ACCEPT_NPC_X)
                    .targetY(ACCEPT_NPC_Y)
                    .targetName(ACCEPT_NPC_NAME)
                    .keepTaskTurnUntilHandled(true)
                    .source("xiuluo:acceptNpc:navigate")
                    .build()).success()) {
                log.warn("[xiuluo] failed to navigate to accept NPC attempt={}", (Object)attempt);
                AcceptDialogProbeResult visibleAfterNavigationFailure = this.tryGetObjectiveFromVisibleXiuluoDialog(context, "after-navigation-failed-attempt" + attempt);
                if (visibleAfterNavigationFailure.state() == AcceptDialogProbeState.BLOCKING_DIALOG_UNHANDLED) {
                    this.cleanBlockingAcceptDialog(context, "after-navigation-failed-attempt" + attempt);
                    continue;
                }
                if (visibleAfterNavigationFailure.state() != AcceptDialogProbeState.OBJECTIVE_READY) continue;
                log.info("[xiuluo][accept-flow] objective recovered after navigation failure attempt={} elapsedMs={}", (Object)attempt, (Object)(System.currentTimeMillis() - startedAt));
                return visibleAfterNavigationFailure.objective();
            }
            this.checkpoint(context);
            XiuluoObjective afterNavigation = this.tryAcceptKnownDialogAfterNpcNavigation(context, "after-navigation-attempt" + attempt);
            if (afterNavigation != null) {
                log.info("[xiuluo][accept-flow] objective ready after navigation attempt={} elapsedMs={}", (Object)attempt, (Object)(System.currentTimeMillis() - startedAt));
                return afterNavigation;
            }
            boolean clickedAcceptNpc = this.clickAcceptNpcAndOpenDialog();
            if (clickedAcceptNpc) {
                /*
                 * clickNpcSmart only returns true after the expected Xiuluo accept dialog template
                 * has already been verified. Do not run the full dialog-type precheck again here:
                 * that path repeats std/option scans and was adding several seconds between the
                 * visible accept dialog and the actual "闲来无事" click. Go straight to the known
                 * accept-option click, then read the story objective that appears after accepting.
                 */
                XiuluoObjective objective = this.tryAcceptKnownDialogAfterNpcNavigation(context,
                        "after-npc-click-verified-attempt" + attempt);
                if (objective != null) {
                    log.info("[xiuluo][accept-flow] objective ready after verified npc click attempt={} elapsedMs={}",
                            (Object) attempt, (Object) (System.currentTimeMillis() - startedAt));
                    return objective;
                }
                log.warn("[xiuluo] accept NPC click was verified, but accept option/objective handling failed attempt={}",
                        (Object) attempt);
                continue;
            }

            if (!clickedAcceptNpc) {
                log.warn("[xiuluo] accept NPC click returned false; checking visible dialog before retry attempt={}", (Object)attempt);
            }
            AcceptDialogProbeResult afterNpcClick = this.tryGetObjectiveFromVisibleXiuluoDialog(context, "after-npc-click-attempt" + attempt);
            if (afterNpcClick.state() == AcceptDialogProbeState.OBJECTIVE_READY) {
                log.info("[xiuluo][accept-flow] objective ready after npc click attempt={} elapsedMs={}", (Object)attempt, (Object)(System.currentTimeMillis() - startedAt));
                return afterNpcClick.objective();
            }
            if (afterNpcClick.state() == AcceptDialogProbeState.BLOCKING_DIALOG_UNHANDLED) {
                this.cleanBlockingAcceptDialog(context, "after-npc-click-attempt" + attempt);
                continue;
            }
            if (clickedAcceptNpc) continue;
            log.warn("[xiuluo] failed to click accept NPC attempt={}", (Object)attempt);
        }
        log.warn("[xiuluo][accept-flow] failed to obtain objective elapsedMs={}", (Object)(System.currentTimeMillis() - startedAt));
        return null;
    }

    private XiuluoObjective tryAcceptKnownDialogAfterNpcNavigation(TaskExecutionContext context, String source) {
        long startedAt = System.currentTimeMillis();
        if (!this.clickVisibleAcceptOption(context, source)) {
            log.info("[xiuluo][accept-flow] known accept option not matched after npc navigation: source={} elapsedMs={}", (Object)source, (Object)(System.currentTimeMillis() - startedAt));
            return null;
        }
        XiuluoObjective objective = this.readObjectiveAfterAcceptOptionClicked(context, source);
        log.info("[xiuluo][accept-flow] known accept option handled after npc navigation: source={} objective={} elapsedMs={}", new Object[]{source, objective, System.currentTimeMillis() - startedAt});
        return objective;
    }

    private boolean clickAcceptNpcAndOpenDialog() {
        boolean clicked = this.npcClickService.clickNpcSmart(xiuluoAcceptNpc().toClickRequest(this.gameContext.getMe()));
        if (!clicked) {
            log.warn("[xiuluo] failed to click accept NPC");
        }
        return clicked;
    }

    /**
     * Probe the current screen for a Xiuluo accept/story dialog.
     *
     * <p>The accept loop must distinguish "there is no dialog" from "there is a dialog, but this
     * task cannot safely handle it". Only the former may continue into navigation or NPC clicking.
     * The latter means a visible dialog is blocking the screen and must be cleared before retrying.</p>
     *
     * @param context current task execution context; used for stop checks while reading story text.
     * @param source diagnostic source written into logs and screenshot reasons.
     * @return structured probe result; {@code OBJECTIVE_READY} carries the parsed objective,
     *         {@code NO_DIALOG} means the NPC path may continue, and
     *         {@code BLOCKING_DIALOG_UNHANDLED} means cleanup is required before any click fallback.
     */
    private AcceptDialogProbeResult tryGetObjectiveFromVisibleXiuluoDialog(TaskExecutionContext context, String source) {
        long startedAt = System.currentTimeMillis();
        DialogType dialogType = this.dialogService.detectDialogTypeNoFocus("xiuluo:accept-fast:" + source);
        log.info("[xiuluo][accept-flow] dialog precheck source={} type={} elapsedMs={}", new Object[]{source, dialogType, System.currentTimeMillis() - startedAt});
        TaskHotStartScreenState screenState = switch (dialogType) {
            case STORY -> TaskHotStartScreenState.STORY_DIALOG;
            case OPTION -> TaskHotStartScreenState.OPTION_DIALOG;
            case NONE -> TaskHotStartScreenState.NONE;
        };
        TaskHotStartSnapshot snapshot = new TaskHotStartSnapshot(TASK_CODE, source, screenState, dialogType);
        if (dialogType == DialogType.NONE) {
            return AcceptDialogProbeResult.noDialog();
        }
        if (snapshot.state() == TaskHotStartScreenState.STORY_DIALOG) {
            XiuluoObjective objective = this.tryReadCurrentStoryObjective(context, source, 3);
            if (objective != null) {
                return AcceptDialogProbeResult.objectiveReady(objective);
            }
            log.warn("[xiuluo][accept-flow] story dialog visible but objective not recognized: source={}", (Object)source);
            return AcceptDialogProbeResult.blockingDialogUnhandled();
        }
        if (snapshot.state() == TaskHotStartScreenState.OPTION_DIALOG) {
            if (!this.clickVisibleAcceptOption(context, source)) {
                log.info("[xiuluo][accept-flow] option dialog visible but accept option not matched: source={}", (Object)source);
                return AcceptDialogProbeResult.blockingDialogUnhandled();
            }
            XiuluoObjective objective = this.readObjectiveAfterAcceptOptionClicked(context, source);
            if (objective != null) {
                return AcceptDialogProbeResult.objectiveReady(objective);
            }
            log.warn("[xiuluo][accept-flow] accept option clicked but objective not recovered: source={}", (Object)source);
            return AcceptDialogProbeResult.blockingDialogUnhandled();
        }
        return AcceptDialogProbeResult.blockingDialogUnhandled();
    }

    /**
     * Clear a visible dialog that blocks the Xiuluo accept retry path.
     *
     * <p>This method is called only after a dialog was detected and proved unusable for the current
     * accept step. It runs shared UI cleanup and then retries from the top instead of clicking the
     * NPC through a covered screen.</p>
     *
     * @param context current task execution context; checked before and after cleanup.
     * @param source diagnostic source written into logs.
     */
    private void cleanBlockingAcceptDialog(TaskExecutionContext context, String source) {
        this.checkpoint(context);
        log.warn("[xiuluo][accept-flow] blocking dialog not handled; clean before retry: source={}", (Object)source);
        this.uiCleanerService.cleanUpAll();
        TaskSleep.sleepOrStop(context, 300L, "Xiuluo task interrupted");
    }

    private boolean isVisibleDialogUsableForXiuluoObjective(TaskHotStartSnapshot snapshot, String source) {
        if (snapshot.state() == TaskHotStartScreenState.NONE || snapshot.state() == TaskHotStartScreenState.IN_COMBAT) {
            log.info("[xiuluo] accept dialog precheck skipped: source={} state={}", (Object)source, (Object)snapshot.state());
            return false;
        }
        return true;
    }

    private boolean clickVisibleAcceptOption(TaskExecutionContext context, String source) {
        TaskTransactionOutcome outcome = this.taskTransactionRunner.runExclusive("xiuluo:acceptOption:" + source, TaskTransactionResult.READY_TO_CONTINUE, TaskYieldPolicy.CONTINUE_CHAIN, () -> {
            this.checkpoint(context);
            String matched = this.dialogService.clickFirstKnownOptionGreenTemplateDirectForExclusive(List.of(new GreenTemplateClickSpec(OPTION_ACCEPT_TASK, ACCEPT_OPTION_TEMPLATE, -5, 80, 4)), "xiuluo:accept:" + source);
            if (!OPTION_ACCEPT_TASK.equals(matched)) {
                return TaskTransactionResult.RETRYABLE_ERROR;
            }
            TaskSleep.sleepOrStop(context, 250L, "Xiuluo task interrupted");
            return TaskTransactionResult.READY_TO_CONTINUE;
        });
        return outcome.reachedExpectedResult();
    }

    private XiuluoObjective readObjectiveAfterAcceptOptionClicked(TaskExecutionContext context, String source) {
        XiuluoObjective storyObjective = this.tryReadCurrentStoryObjective(context, source, 3);
        if (storyObjective != null) {
            return storyObjective;
        }
        UnderFivePromptResult underFiveResult = this.tryReadObjectiveFromUnderFivePromptIfPresent(context, source);
        if (underFiveResult.declined()) {
            return null;
        }
        if (underFiveResult.objective() != null) {
            return underFiveResult.objective();
        }
        log.warn("[xiuluo] accept story objective parse failed; trying task-panel fallback");
        XiuluoObjective fallbackObjective = this.tryReadObjectiveFromTaskPanel(context, source + ":task-panel");
        if (fallbackObjective != null) {
            return fallbackObjective;
        }
        log.warn("[xiuluo] task-panel fallback objective parse failed");
        return null;
    }

    private UnderFivePromptResult tryReadObjectiveFromUnderFivePromptIfPresent(TaskExecutionContext context, String source) {
        if (!this.teamReturnService.isReturnTeamSignalPresent()) {
            return new UnderFivePromptResult(false, null);
        }
        log.warn("[xiuluo] return-team signal exists after accept story parse failed; try under-five prompt first");
        return this.tryHandleUnderFivePromptAndReadObjective(context, source);
    }

    private XiuluoObjective tryReadCurrentStoryObjective(TaskExecutionContext context, String source, int attempts) {
        for (int i = 1; i <= attempts; ++i) {
            this.checkpoint(context);
            String reason = "xiuluo:story-objective:" + source + ":try" + i;
            BufferedImage storyImage = this.dialogService.captureCurrentStoryImage(reason);
            XiuluoObjective templateObjective = this.parseObjective(storyImage, reason);
            if (templateObjective != null) {
                log.info("[xiuluo] objective parsed from story template: {}", (Object)templateObjective);
                return templateObjective;
            }
            TaskSleep.sleepOrStop(context, 500L, "Xiuluo task interrupted");
        }
        return null;
    }

    private XiuluoObjective tryReadObjectiveFromTaskPanel(TaskExecutionContext context, String source) {
        this.checkpoint(context);
        log.info("[xiuluo] trying task-panel objective fallback: source={}", (Object)source);
        QuestDetailCapture detailCapture = this.questManagerService.captureCurrentQuestDetailForTask(TASK_CODE);
        XiuluoObjective detailObjective = this.parseObjective(detailCapture.image(), "xiuluo:task-panel-detail");
        if (detailObjective != null) {
            log.info("[xiuluo] objective parsed from task panel template fallback: {}", (Object)detailObjective);
            return detailObjective;
        }
        log.warn("[xiuluo] task-panel template fallback failed; OCR fallback disabled");
        return null;
    }

    private UnderFivePromptResult tryHandleUnderFivePromptAndReadObjective(TaskExecutionContext context, String source) {
        TaskTransactionOutcome outcome = this.taskTransactionRunner.runExclusive("xiuluo:underFiveFallback:" + source, TaskTransactionResult.READY_TO_CONTINUE, TaskYieldPolicy.CONTINUE_CHAIN, () -> {
            this.checkpoint(context);
            if (this.botProperties.isXiuluoAllowUnderFiveMembers()) {
                boolean confirmed = this.dialogService.clickGreenTemplateOptionDirectForExclusive(UNDER_FIVE_CONFIRM_TEMPLATE, "xiuluo:under-five-confirm:" + source, 24, 4);
                if (!confirmed) {
                    return TaskTransactionResult.RETRYABLE_ERROR;
                }
                log.info("[xiuluo] under-five confirmation accepted by config: source={}", (Object)source);
                TaskSleep.sleepOrStop(context, 900L, "Xiuluo task interrupted");
                return TaskTransactionResult.READY_TO_CONTINUE;
            }
            boolean declined = this.dialogService.clickGreenTemplateOptionDirectForExclusive(UNDER_FIVE_WAIT_TEMPLATE, "xiuluo:under-five-decline:" + source, 24, 4);
            if (!declined) {
                return TaskTransactionResult.RETRYABLE_ERROR;
            }
            log.warn("[xiuluo] under-five confirmation declined by config; stop this round: source={}", (Object)source);
            TaskSleep.sleepOrStop(context, 600L, "Xiuluo task interrupted");
            return TaskTransactionResult.FAILED;
        });
        if (!outcome.reachedExpectedResult()) {
            return new UnderFivePromptResult(outcome.result() == TaskTransactionResult.FAILED, null);
        }
        for (int i = 1; i <= 3; ++i) {
            this.checkpoint(context);
            String reason = "xiuluo:under-five-objective:" + source + ":try" + i;
            BufferedImage storyImage = this.dialogService.captureCurrentStoryImage(reason);
            XiuluoObjective templateObjective = this.parseObjective(storyImage, reason);
            if (templateObjective != null) {
                log.info("[xiuluo] objective parsed after under-five confirm template: {}", (Object)templateObjective);
                return new UnderFivePromptResult(false, templateObjective);
            }
            TaskSleep.sleepOrStop(context, 500L, "Xiuluo task interrupted");
        }
        log.warn("[xiuluo] under-five fallback clicked but objective parse still failed: source={}", (Object)source);
        return new UnderFivePromptResult(false, null);
    }

    /**
     * Parse one captured story/task-panel image with the template objective recognizer.
     *
     * @param image image owned by the caller; this method always flushes it before returning.
     * @param source diagnostic label passed through to the recognizer and debug artifacts.
     * @return parsed Xiuluo objective, or null when the objective templates do not match.
     */
    private XiuluoObjective parseObjective(BufferedImage image, String source) {
        if (image == null) {
            return null;
        }
        try {
            Optional<ObjectiveTextResult> result = this.objectiveTextRecognitionService.recognize(image, source);
            XiuluoObjective xiuluoObjective = result.map(value -> new XiuluoObjective(value.mapName(), value.x(), value.y(), "template:" + value.mapSlug() + ":" + value.mapScore())).orElse(null);
            return xiuluoObjective;
        }
        finally {
            image.flush();
        }
    }

    private boolean clickTargetAndEnterBattle(TaskExecutionContext context, XiuluoObjective objective) {
        this.checkpoint(context);
        /*
         * Normal Xiuluo progression reaches this method immediately after current-map approach
         * navigation, so the enter-battle option is not expected to be open yet. Hot-start cases
         * with an already-open option dialog are handled before the navigation transaction enters
         * this target-click stage. Skipping the pre-click dialog scan avoids an empty screenshot /
         * template pass on every successful route; the post-click check below remains the source
         * of truth for confirming battle entry.
         */
        NpcTarget combatTarget = xiuluoCombatTarget(objective);
        boolean clicked = this.npcClickService.clickNpcSmart(combatTarget.toClickRequest(this.gameContext.getMe()));
        if (!clicked) {
            log.warn("[xiuluo] target click failed: objective={}", (Object)objective);
            return false;
        }
        this.checkpoint(context);
        if (!this.tryConfirmEnterBattleDialog(context, "after-target-click")) {
            return false;
        }
        TaskSleep.sleepOrStop(context, 1200L, "Xiuluo task interrupted");
        return true;
    }

    private NpcTarget xiuluoCombatTarget(XiuluoObjective objective) {
        return NpcTarget.builder()
                .key("xiuluo.combatTarget")
                .mapName(objective.mapName())
                .name(XIULUO_TARGET_KEYWORD)
                .x(objective.x())
                .y(objective.y())
                .role(NpcRole.COMBAT_TARGET)
                .movementType(NpcMovementType.ROAMING)
                .expectedDialogTemplatePath(ENTER_BATTLE_TEMPLATE)
                .source("xiuluoObjective:" + objective.rawText())
                .build();
    }

    private static NpcTarget xiuluoAcceptNpc() {
        return NpcTarget.builder()
                .key("xiuluo.acceptNpc")
                .mapName(START_MAP_NAME)
                .name(ACCEPT_NPC_NAME)
                .x(ACCEPT_NPC_X)
                .y(ACCEPT_NPC_Y)
                .role(NpcRole.QUEST_GIVER)
                .movementType(NpcMovementType.FIXED)
                .expectedDialogTemplatePath(ACCEPT_OPTION_TEMPLATE)
                .source("xiuluo")
                .build();
    }

    private boolean tryConfirmEnterBattleDialog(TaskExecutionContext context, String source) {
        this.checkpoint(context);
        /*
         * The enter-battle option text is only "看打!". Click close to the matched glyph center so a
         * random offset cannot drift into the blank right side of the dialog.
         */
        boolean confirmed = this.dialogService.clickGreenTemplateOption(ENTER_BATTLE_TEMPLATE, "xiuluo:enter-battle:" + source, 6, 4);
        if (!confirmed) {
            log.info("[xiuluo] enter battle option not matched: source={} template={}", (Object)source, (Object)ENTER_BATTLE_TEMPLATE);
            return false;
        }
        return true;
    }

    private boolean waitCombatIfNeeded(TaskExecutionContext context) {
        boolean inCombat;
        this.checkpoint(context);
        boolean bl = inCombat = this.battleRadarService.checkAndSyncCombatState() && this.gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT;
        if (!inCombat) {
            return false;
        }
        log.info("[xiuluo] currently in combat, wait");
        TaskSleep.sleepOrStop(context, this.battleRadarService.getDynamicPollingIntervalMs(), "Xiuluo task interrupted");
        return true;
    }

    /**
     * Wait for a Xiuluo battle using the shared auto-combat coordinator.
     *
     * <p>The old Xiuluo path only polled {@link BattleRadarService}, which could observe combat
     * state but did not run the auto-combat entry maintenance that verifies/opens the automatic
     * battle panel. This loop now delegates every radar tick to {@link AutoCombatService}; that
     * keeps leader combat behavior aligned with Five Ring and also lets the unified post-combat
     * recovery perform first aid and sheyaoxiang checks when an exit signal is consumed.</p>
     *
     * @param context current leader task context used for stop checks.
     * @param round 1-based Xiuluo round number for logs.
     * @return true after a combat exit is recovered, false if combat never starts.
     */
    private boolean waitCombatToFinish(TaskExecutionContext context, int round) {
        long waitEnterStart = System.currentTimeMillis();
        while (System.currentTimeMillis() - waitEnterStart < 25000L) {
            this.checkpoint(context);
            AutoCombatService.TickResult tick = this.autoCombatService.handleCombatTick(context, "xiuluo", true);
            if (tick == AutoCombatService.TickResult.IN_COMBAT) {
                log.info("[xiuluo] combat entered for round {}", (Object)round);
                break;
            }
            if (tick == AutoCombatService.TickResult.EXIT_RECOVERED) {
                log.info("[xiuluo] combat entered and exited quickly for round {}", (Object)round);
                return true;
            }
            TaskSleep.sleepOrStop(context, 1000L, "Xiuluo task interrupted");
        }
        if (this.gameContext.getCurrentActionState() != GameContext.ActionState.IN_COMBAT) {
            log.warn("[xiuluo] battle did not start after confirm");
            return false;
        }
        while (true) {
            this.checkpoint(context);
            AutoCombatService.TickResult tick = this.autoCombatService.handleCombatTick(context, "xiuluo", true);
            if (tick == AutoCombatService.TickResult.EXIT_RECOVERED) {
                log.info("[xiuluo] combat finished for round {}", (Object)round);
                return true;
            }
            TaskSleep.sleepOrStop(context, this.battleRadarService.getDynamicPollingIntervalMs(), "Xiuluo task interrupted");
        }
    }

    private boolean useReturnItem(TaskExecutionContext context) {
        TaskTransactionOutcome outcome = this.taskTransactionRunner.run("xiuluo:returnItem", TaskTransactionResult.READY_TO_CONTINUE, TaskYieldPolicy.CONTINUE_CHAIN, () -> {
            this.checkpoint(context);
            if (!this.confirmOutOfCombatForReturnBaseline(context)) {
                return TaskTransactionResult.RETRYABLE_ERROR;
            }
            BufferedImage beforeReturn = this.gameStateUtil.captureCurrentMapLabelSnapshot("xiuluo:return-before");
            if (beforeReturn == null) {
                log.warn("[xiuluo] return verify baseline map label failed");
                return TaskTransactionResult.RETRYABLE_ERROR;
            }
            try {
                boolean used = this.bagService.findAndUseItemFromBack(BagService.MAIN_BAG, RETURN_ITEM_TEMPLATE, 5, context);
                if (!used) {
                    log.warn("[xiuluo] return item not found/used: template={}", (Object)RETURN_ITEM_TEMPLATE);
                    TaskTransactionResult taskTransactionResult = TaskTransactionResult.RETRYABLE_ERROR;
                    return taskTransactionResult;
                }
                if (!this.waitUntilReturnMapLabelChanged(context, beforeReturn)) {
                    TaskTransactionResult taskTransactionResult = TaskTransactionResult.RETRYABLE_ERROR;
                    return taskTransactionResult;
                }
            }
            finally {
                beforeReturn.flush();
            }
            return TaskTransactionResult.READY_TO_CONTINUE;
        });
        return outcome.reachedExpectedResult();
    }

    private boolean waitUntilReturnMapLabelChanged(TaskExecutionContext context, BufferedImage beforeReturn) {
        long timeoutMs = Math.max(this.botProperties.getXiuluoReturnVerifyTimeoutMs(), 1000L);
        long pollMs = Math.max(this.botProperties.getXiuluoReturnVerifyPollMs(), 300L);
        long deadlineAtMs = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadlineAtMs) {
            this.checkpoint(context);
            TaskSleep.sleepOrStop(context, pollMs, "Xiuluo task interrupted");
            if (this.bagService.isMainBagOpen(context)) {
                log.warn("[xiuluo] return verify skipped because main bag is still open");
                continue;
            }
            if (!this.gameStateUtil.isCurrentMapLabelChangedFrom(beforeReturn, "xiuluo:return-after")) continue;
            log.info("[xiuluo] return map label changed, treat return item as completed");
            return true;
        }
        log.warn("[xiuluo] return map label did not change within {} ms", (Object)timeoutMs);
        return false;
    }

    private boolean confirmOutOfCombatForReturnBaseline(TaskExecutionContext context) {
        this.checkpoint(context);
        this.battleRadarService.checkAndSyncCombatState();
        if (this.gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            log.warn("[xiuluo] return verify baseline blocked: still in combat");
            return false;
        }
        if (this.bagService.isMainBagOpen(context)) {
            log.warn("[xiuluo] return verify baseline blocked: main bag is already open");
            return false;
        }
        return true;
    }

    private void checkpoint(TaskExecutionContext context) {
        TaskCheckpoint.throwIfStopRequested(context, "Xiuluo task interrupted");
    }

    private boolean interrupted() {
        return Thread.currentThread().isInterrupted();
    }

    /**
     * Create a prototype Xiuluo task bound to the injected window-aware services.
     *
     * @param gameContext per-window runtime state; mutated for bot/action status and current map.
     * @param botProperties task timing and Xiuluo feature configuration.
     * @param playerStateService player identity/state service kept for task parity with other flows.
     * @param navigationService window-bound navigation and mini-map pathing service.
     * @param npcClickService NPC/monster click strategy service; may submit queued physical input.
     * @param dialogService dialog detector and green-template click service.
     * @param bagService bag/item service used for the Xiuluo return item.
     * @param autoCombatService unified combat tick service used to verify auto-combat panel and
     *                          recover after confirmed battle exit for the bound leader window.
     * @param battleRadarService combat-state detector and dynamic polling source.
     * @param gameStateUtil map-label snapshot utility used for return verification.
     * @param taskTransactionRunner task-turn owner used to serialize leader-only critical sections.
     * @param questManagerService task-panel capture service for objective fallback.
     * @param objectiveTextRecognitionService template recognizer for Xiuluo objective text images.
     * @param teamReturnService post-return team-member wait policy.
     * @param taskHotStartService current-screen classifier used before accepting a new objective.
     * @param uiCleanerService shared UI cleanup service used only after a blocking dialog is detected.
     * @param coordinateHelper map-coordinate helper used to derive approach coordinates from raw task targets.
     */
    public XiuluoTask(GameContext gameContext, BotProperties botProperties, PlayerStateService playerStateService, NavigationService navigationService, NpcClickService npcClickService, DialogService dialogService, BagService bagService, AutoCombatService autoCombatService, BattleRadarService battleRadarService, GameStateUtil gameStateUtil, TaskTransactionRunner taskTransactionRunner, QuestManagerService questManagerService, ObjectiveTextRecognitionService objectiveTextRecognitionService, TeamReturnService teamReturnService, TaskHotStartService taskHotStartService, UICleanerService uiCleanerService, CoordinateHelper coordinateHelper) {
        this.gameContext = gameContext;
        this.botProperties = botProperties;
        this.playerStateService = playerStateService;
        this.navigationService = navigationService;
        this.npcClickService = npcClickService;
        this.dialogService = dialogService;
        this.bagService = bagService;
        this.autoCombatService = autoCombatService;
        this.battleRadarService = battleRadarService;
        this.gameStateUtil = gameStateUtil;
        this.taskTransactionRunner = taskTransactionRunner;
        this.questManagerService = questManagerService;
        this.objectiveTextRecognitionService = objectiveTextRecognitionService;
        this.teamReturnService = teamReturnService;
        this.taskHotStartService = taskHotStartService;
        this.uiCleanerService = uiCleanerService;
        this.coordinateHelper = coordinateHelper;
    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class XiuluoObjective {

        String mapName;

        int x;

        int y;

        String rawText;

    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    private static class XiuluoHotStartResult {


        XiuluoHotStartState state;


        XiuluoObjective objective;

        private static XiuluoHotStartResult none() {
            return new XiuluoHotStartResult(XiuluoHotStartState.NONE, null);
        }

        private static XiuluoHotStartResult objectiveReady(XiuluoObjective objective) {
            return new XiuluoHotStartResult(XiuluoHotStartState.OBJECTIVE_READY, objective);
        }

        private static XiuluoHotStartResult enterBattleConfirmed() {
            return new XiuluoHotStartResult(XiuluoHotStartState.ENTER_BATTLE_CONFIRMED, null);
        }

        private static XiuluoHotStartResult inCombat() {
            return new XiuluoHotStartResult(XiuluoHotStartState.IN_COMBAT, null);
        }

        private static XiuluoHotStartResult failed() {
            return new XiuluoHotStartResult(XiuluoHotStartState.FAILED, null);
        }

        private static XiuluoHotStartResult stopped() {
            return new XiuluoHotStartResult(XiuluoHotStartState.STOPPED, null);
        }
    


    }

    private static enum XiuluoHotStartState {
        NONE,
        OBJECTIVE_READY,
        ENTER_BATTLE_CONFIRMED,
        IN_COMBAT,
        STOPPED,
        FAILED;

    }

    /**
     * Result of checking the current dialog during Xiuluo task acceptance.
     *
     * @param state probe state. {@code NO_DIALOG} means the screen is clean enough for navigation;
     *              {@code BLOCKING_DIALOG_UNHANDLED} means a dialog is visible but not safely handled.
     * @param objective parsed objective when {@code state} is {@code OBJECTIVE_READY}; otherwise null.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class AcceptDialogProbeResult {

        AcceptDialogProbeState state;

        XiuluoObjective objective;

        private static AcceptDialogProbeResult noDialog() {
            return new AcceptDialogProbeResult(AcceptDialogProbeState.NO_DIALOG, null);
        }

        private static AcceptDialogProbeResult objectiveReady(XiuluoObjective objective) {
            return new AcceptDialogProbeResult(AcceptDialogProbeState.OBJECTIVE_READY, objective);
        }

        private static AcceptDialogProbeResult blockingDialogUnhandled() {
            return new AcceptDialogProbeResult(AcceptDialogProbeState.BLOCKING_DIALOG_UNHANDLED, null);
        }
    

    }

    /**
     * Dialog probe states used to keep NPC click fallback from running through a covered screen.
     */
    private static enum AcceptDialogProbeState {
        NO_DIALOG,
        OBJECTIVE_READY,
        BLOCKING_DIALOG_UNHANDLED
    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    private static class UnderFivePromptResult {


        boolean declined;


        XiuluoObjective objective;


    }
}
