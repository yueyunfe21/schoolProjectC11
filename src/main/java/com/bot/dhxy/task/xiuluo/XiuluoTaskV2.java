package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.navigation.MapNavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.navigation.NpcNavigationRequest;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Phase-machine skeleton for the next Xiuluo implementation.
 *
 * <p>This class is intentionally not feature-complete yet. It exists so the Xiuluo task structure is
 * reviewable before we migrate the old business logic: hot-start resolves an initial phase, each
 * phase returns a {@link XiuluoStepOutcome}, and the task layer decides whether the business turn is
 * kept or yielded.</p>
 */
@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class XiuluoTaskV2 implements GameTask {

    private static final String TASK_CODE = "xiuluo_v2";
    private static final String TASK_NAME = "修罗V2";
    private static final String START_MAP_NAME = "灵兽村";
    private static final String ACCEPT_NPC_NAME = "灵兽村使者";
    private static final int ACCEPT_NPC_X = 112;
    private static final int ACCEPT_NPC_Y = 93;
    private static final NpcTarget ACCEPT_NPC = NpcTarget.builder()
            .key("xiuluo.acceptNpc")
            .mapName(START_MAP_NAME)
            .name(ACCEPT_NPC_NAME)
            .x(ACCEPT_NPC_X)
            .y(ACCEPT_NPC_Y)
            .role(NpcRole.QUEST_GIVER)
            .movementType(NpcMovementType.FIXED)
            .source("xiuluo-v2")
            .build();

    private final BotProperties botProperties;
    private final GameContext gameContext;
    private final NavigationService navigationService;
    private final CoordinateHelper coordinateHelper;
    private final XiuluoHotStartResolver hotStartResolver;
    private final TaskTransactionRunner taskTransactionRunner;
    private final TaskExecutionContextHolder taskExecutionContextHolder;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    // 🔴 TASK ENTRY: UI/runner enters Xiuluo V2 here.
    /**
     * Run the Xiuluo V2 phase skeleton.
     *
     * @param executionContext nullable task context. A minimal context is created for legacy direct
     *                         execution so stop checks and logs still have a task code/name.
     * @return SUCCESS after the skeleton reaches the configured round count, STOPPED if interrupted,
     *         or FAILED when a phase returns an unrecoverable result.
     */
    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        TaskExecutionContext context = resolveExecutionContext(executionContext);
        int maxRuns = botProperties.getXiuluoMaxRuns();
        int completedRuns = 0;
        gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        log.info("[xiuluo-v2] skeleton started: maxRuns={}", isUnlimitedRuns(maxRuns) ? "unlimited" : maxRuns);

        try {
            while (shouldStartNextRound(maxRuns, completedRuns)) {
                TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
                int round = completedRuns + 1;
                // 🧭 HOT START: decide which phase this round should enter from current screen state.
                /*
                 * Task-panel hot-start fallback is only for process startup: the player may have
                 * manually accepted Xiuluo before pressing Start. Normal later rounds should enter
                 * from PREPARE_ROUND and let the accept/read-objective phases own task-panel reads.
                 */
                boolean allowStartupTaskPanelFallback = completedRuns == 0;
                XiuluoRoundContext roundContext = hotStartResolver.resolve(round, allowStartupTaskPanelFallback);
                log.info("[xiuluo-v2] round {} initial phase: phase={} source={} objective={}",
                        round, roundContext.phase(), roundContext.source(), roundContext.objective());

                // 🧩 ROUND EXECUTION: run the selected phase chain until ROUND_DONE/FAILED/STOPPED.
                TaskRunResult roundResult = runRoundPhases(context, roundContext);
                if (roundResult != TaskRunResult.SUCCESS) {
                    gameContext.setBotStatus(roundResult == TaskRunResult.STOPPED
                            ? GameContext.BotStatus.IDLE
                            : GameContext.BotStatus.ERROR);
                    return roundResult;
                }

                completedRuns++;
                log.info("[xiuluo-v2] round {} skeleton finished, completed={}", round, completedRuns);
            }

            gameContext.setBotStatus(GameContext.BotStatus.IDLE);
            return TaskRunResult.SUCCESS;
        } finally {
            taskTransactionRunner.forceReleaseTurn("xiuluo-v2:execute-finished");
        }
    }

    @Override
    public void stop() {
        log.info("[xiuluo-v2] stop requested");
        gameContext.setBotStatus(GameContext.BotStatus.IDLE);
    }

    // 🟠 ROUND LOOP: one Xiuluo round advances through phase transactions here.
    private TaskRunResult runRoundPhases(TaskExecutionContext context, XiuluoRoundContext initialContext) {
        XiuluoRoundContext roundContext = initialContext;
        int phaseLoopGuard = 0;
        while (!roundContext.phase().isTerminal()) {
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            if (++phaseLoopGuard > 32) {
                log.error("[xiuluo-v2] phase loop guard exceeded: roundContext={}", roundContext);
                return TaskRunResult.FAILED;
            }

            XiuluoRoundContext currentContext = roundContext;
            AtomicReference<XiuluoStepOutcome> phaseOutcome = new AtomicReference<>();
            // 🔒 PHASE TRANSACTION: the current phase owns the task turn for this business step.
            /*
             * The phase is executed exactly once, inside the task transaction. The AtomicReference is
             * only used to bring the structured outcome back out so the phase machine can advance.
             */
            TaskTransactionOutcome transaction = taskTransactionRunner.run(
                    "xiuluo-v2:" + currentContext.phase(),
                    TaskTransactionResult.READY_TO_CONTINUE,
                    TaskYieldPolicy.CONTINUE_CHAIN,
                    () -> {
                        XiuluoStepOutcome outcome = runPhase(context, currentContext);
                        phaseOutcome.set(outcome);
                        return outcome.transactionResult();
                    });

            XiuluoStepOutcome outcome = phaseOutcome.get();
            if (outcome == null) {
                outcome = XiuluoStepOutcome.failed(currentContext, "phase produced no outcome");
            }
            log.info("[xiuluo-v2] phase outcome: phase={} result={} yield={} next={} message={}",
                    currentContext.phase(), outcome.transactionResult(), outcome.yieldPolicy(),
                    outcome.nextState().phase(), outcome.message());

            // 🚦 PHASE RESULT GATE: terminal results leave the round; otherwise advance context.
            if (transaction.result() == TaskTransactionResult.STOPPED
                    || outcome.transactionResult() == TaskTransactionResult.STOPPED) {
                return TaskRunResult.STOPPED;
            }
            if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
                return TaskRunResult.FAILED;
            }
            // ➡️ NEXT CONTEXT: outcome decides the next phase and carries objective/round/source forward.
            roundContext = outcome.nextState();
        }

        return roundContext.phase() == XiuluoPhase.ROUND_DONE ? TaskRunResult.SUCCESS : TaskRunResult.FAILED;
    }

    private boolean shouldStartNextRound(int maxRuns, int completedRuns) {
        return isUnlimitedRuns(maxRuns) || completedRuns < maxRuns;
    }

    private boolean isUnlimitedRuns(int maxRuns) {
        return maxRuns <= 0;
    }

    /**
     * Execute one phase in the Xiuluo V2 state machine.
     *
     * @param context current task context used only for stop checkpoints in the skeleton.
     * @param state current round state.
     * @return next phase and transaction intent. Real business logic will be migrated here phase by
     *         phase from the old Xiuluo task.
     */
    // 🟡 PHASE DISPATCH: add or reroute Xiuluo phase behavior in this switch first.
    private XiuluoStepOutcome runPhase(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        return switch (state.phase()) {
            case PREPARE_ROUND -> prepareRound(context, state);
            case ACCEPT_TASK_NAVIGATE_TO_NPC -> navigateToTaskNpc(context, state);
            case ACCEPT_TASK_CLICK_NPC -> clickTaskNpc(context, state);
            case ACCEPT_TASK_DIALOG -> acceptTaskDialog(context, state);
            case READ_OBJECTIVE -> readObjective(context, state);
            case NAVIGATE_TO_TARGET_MAP -> navigateToTargetMap(context, state);
            case NAVIGATE_TO_TARGET_POINT -> navigateToTargetPoint(context, state);
            case CLICK_TARGET_NPC -> clickTargetNpc(context, state);
            case CONFIRM_ENTER_BATTLE -> confirmEnterBattle(context, state);
            case WAIT_COMBAT -> waitCombat(context, state);
            case RETURN_HOME -> returnHome(context, state);
            case WAIT_TEAM_RETURN -> waitTeamReturn(context, state);
            case ROUND_DONE, FAILED, STOPPED ->
                    XiuluoStepOutcome.failed(state, "terminal phase should not be executed: " + state.phase());
        };
    }

    // 🟢 PHASE HANDLERS: each method owns exactly one Xiuluo phase.
    private XiuluoStepOutcome prepareRound(TaskExecutionContext context, XiuluoRoundContext state) {
        return skeletonContinue(context, state, XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC);
    }

    private XiuluoStepOutcome navigateToTaskNpc(TaskExecutionContext context, XiuluoRoundContext state) {
        // 🧭 ACCEPT NPC NAV: go to the fixed task giver before opening/handling its dialog.
        NavigationResult result = navigationService.navigateToNPC(NpcNavigationRequest.builder()
                .targetMapName(ACCEPT_NPC.getMapName())
                .targetX(ACCEPT_NPC.getX())
                .targetY(ACCEPT_NPC.getY())
                .targetName(ACCEPT_NPC.getName())
                .keepTaskTurnUntilHandled(true)
                .source("xiuluo-v2:acceptNpc")
                .build());
        return navigationOutcome(state, result, XiuluoPhase.ACCEPT_TASK_CLICK_NPC, "navigate to accept NPC");
    }

    private XiuluoStepOutcome clickTaskNpc(TaskExecutionContext context, XiuluoRoundContext state) {
        return skeletonContinue(context, state, XiuluoPhase.ACCEPT_TASK_DIALOG);
    }

    private XiuluoStepOutcome acceptTaskDialog(TaskExecutionContext context, XiuluoRoundContext state) {
        return skeletonContinue(context, state, XiuluoPhase.READ_OBJECTIVE);
    }

    private XiuluoStepOutcome readObjective(TaskExecutionContext context, XiuluoRoundContext state) {
        return skeletonContinue(context, state, XiuluoPhase.NAVIGATE_TO_TARGET_MAP);
    }

    private XiuluoStepOutcome navigateToTargetMap(TaskExecutionContext context, XiuluoRoundContext state) {
        // 🗺️ TARGET MAP NAV: objective map must be known before cross-map routing starts.
        NpcTarget objective = state.objective();
        if (objective == null) {
            return XiuluoStepOutcome.failed(state, "cannot navigate to target map without objective");
        }
        NavigationResult result = navigationService.navigateToMap(MapNavigationRequest.builder()
                .targetMapName(objective.getMapName())
                .keepTaskTurnUntilHandled(true)
                .source("xiuluo-v2:targetMap")
                .build());
        return navigationOutcome(state, result, XiuluoPhase.NAVIGATE_TO_TARGET_POINT, "navigate to target map");
    }

    private XiuluoStepOutcome navigateToTargetPoint(TaskExecutionContext context, XiuluoRoundContext state) {
        // 📍 TARGET POINT NAV: convert objective coordinate to an approach point, then mini-map path.
        NpcTarget objective = state.objective();
        if (objective == null) {
            return XiuluoStepOutcome.failed(state, "cannot navigate to target point without objective");
        }
        MapCoordinate approach = coordinateHelper.calculateApproachCoordinate(
                objective.getMapName(), objective.getX(), objective.getY());
        NavigationResult result = navigationService.navigateInCurrentMap(approach.getX(), approach.getY());
        return navigationOutcome(state, result, XiuluoPhase.CLICK_TARGET_NPC, "navigate to target point");
    }

    private XiuluoStepOutcome clickTargetNpc(TaskExecutionContext context, XiuluoRoundContext state) {
        return skeletonContinue(context, state, XiuluoPhase.CONFIRM_ENTER_BATTLE);
    }

    private XiuluoStepOutcome confirmEnterBattle(TaskExecutionContext context, XiuluoRoundContext state) {
        return skeletonContinue(context, state, XiuluoPhase.WAIT_COMBAT);
    }

    private XiuluoStepOutcome waitCombat(TaskExecutionContext context, XiuluoRoundContext state) {
        // Real version: battle handoff belongs here; once in combat, the task must yield.
        return skeletonContinue(context, state, XiuluoPhase.RETURN_HOME);
    }

    private XiuluoStepOutcome returnHome(TaskExecutionContext context, XiuluoRoundContext state) {
        return skeletonContinue(context, state, XiuluoPhase.WAIT_TEAM_RETURN);
    }

    private XiuluoStepOutcome waitTeamReturn(TaskExecutionContext context, XiuluoRoundContext state) {
        return skeletonContinue(context, state, XiuluoPhase.ROUND_DONE);
    }

    private XiuluoStepOutcome skeletonContinue(TaskExecutionContext context,
                                               XiuluoRoundContext state,
                                               XiuluoPhase nextPhase) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        log.info("[xiuluo-v2] skeleton phase: round={} phase={} source={} objective={} -> {}",
                state.round(), state.phase(), state.source(), state.objective(), nextPhase);
        return XiuluoStepOutcome.continueTo(state.next(nextPhase, "skeleton:" + state.phase()), "skeleton transition");
    }

    // 🔵 NAVIGATION RESULT BRIDGE: NavigationService facts become task phase/yield decisions here.
    private XiuluoStepOutcome navigationOutcome(XiuluoRoundContext state,
                                                NavigationResult result,
                                                XiuluoPhase arrivedPhase,
                                                String actionName) {
        NavigationResultStatus status = result.getStatus();
        log.info("[xiuluo-v2] navigation phase result: phase={} action={} status={} message={}",
                state.phase(), actionName, status, result.getMessage());
        // ✅ ARRIVED: navigation completed inside this transaction, so continue to the next phase.
        if (status == NavigationResultStatus.ARRIVED || status == NavigationResultStatus.SUCCESS) {
            return XiuluoStepOutcome.continueTo(
                    state.next(arrivedPhase, "navigation:" + state.phase()), actionName + " arrived");
        }
        // 🏃 PATHING: movement has started; yield so another window can use the task turn.
        if (status == NavigationResultStatus.PATHING_STARTED) {
            return XiuluoStepOutcome.pathingStarted(state, actionName + " pathing started");
        }
        // ⛔ STOPPED: preserve explicit stop separately from ordinary navigation failure.
        if (status == NavigationResultStatus.STOPPED) {
            return XiuluoStepOutcome.stopped(state, actionName + " stopped");
        }
        // ❌ FAILED: retry/fallback policy will live in the calling phase, not NavigationService.
        return XiuluoStepOutcome.failed(state, actionName + " failed: " + status);
    }

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

}
