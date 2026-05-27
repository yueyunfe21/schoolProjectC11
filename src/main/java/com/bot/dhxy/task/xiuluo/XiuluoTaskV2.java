package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
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

    private final BotProperties botProperties;
    private final GameContext gameContext;
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
                checkpoint(context);
                int round = completedRuns + 1;
                /*
                 * Task-panel hot-start fallback is only for process startup: the player may have
                 * manually accepted Xiuluo before pressing Start. Normal later rounds should enter
                 * from PREPARE_ROUND and let the accept/read-objective phases own task-panel reads.
                 */
                boolean allowStartupTaskPanelFallback = completedRuns == 0;
                XiuluoRoundState state = hotStartResolver.resolve(round, allowStartupTaskPanelFallback);
                log.info("[xiuluo-v2] round {} initial phase: phase={} source={} objective={}",
                        round, state.phase(), state.source(), state.objective());

                TaskRunResult roundResult = runRoundPhases(context, state);
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

    private TaskRunResult runRoundPhases(TaskExecutionContext context, XiuluoRoundState initialState) {
        XiuluoRoundState state = initialState;
        int phaseLoopGuard = 0;
        while (!state.phase().isTerminal()) {
            checkpoint(context);
            if (++phaseLoopGuard > 32) {
                log.error("[xiuluo-v2] phase loop guard exceeded: state={}", state);
                return TaskRunResult.FAILED;
            }

            XiuluoRoundState currentState = state;
            AtomicReference<XiuluoStepOutcome> phaseOutcome = new AtomicReference<>();
            /*
             * The phase is executed exactly once, inside the task transaction. The AtomicReference is
             * only used to bring the structured outcome back out so the phase machine can advance.
             */
            TaskTransactionOutcome transaction = taskTransactionRunner.run(
                    "xiuluo-v2:" + currentState.phase(),
                    TaskTransactionResult.READY_TO_CONTINUE,
                    TaskYieldPolicy.CONTINUE_CHAIN,
                    () -> {
                        XiuluoStepOutcome outcome = runPhase(context, currentState);
                        phaseOutcome.set(outcome);
                        return outcome.transactionResult();
                    });

            XiuluoStepOutcome outcome = phaseOutcome.get();
            if (outcome == null) {
                outcome = XiuluoStepOutcome.failed(currentState, "phase produced no outcome");
            }
            log.info("[xiuluo-v2] phase outcome: phase={} result={} yield={} next={} message={}",
                    currentState.phase(), outcome.transactionResult(), outcome.yieldPolicy(),
                    outcome.nextState().phase(), outcome.message());

            if (transaction.result() == TaskTransactionResult.STOPPED
                    || outcome.transactionResult() == TaskTransactionResult.STOPPED) {
                return TaskRunResult.STOPPED;
            }
            if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
                return TaskRunResult.FAILED;
            }
            state = outcome.nextState();
        }

        return state.phase() == XiuluoPhase.ROUND_DONE ? TaskRunResult.SUCCESS : TaskRunResult.FAILED;
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
    private XiuluoStepOutcome runPhase(TaskExecutionContext context, XiuluoRoundState state) {
        checkpoint(context);
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

    private XiuluoStepOutcome prepareRound(TaskExecutionContext context, XiuluoRoundState state) {
        return skeletonContinue(context, state, XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC);
    }

    private XiuluoStepOutcome navigateToTaskNpc(TaskExecutionContext context, XiuluoRoundState state) {
        // Real version: navigate to Ling Shou Village task NPC, then yield if pathing starts.
        return skeletonContinue(context, state, XiuluoPhase.ACCEPT_TASK_CLICK_NPC);
    }

    private XiuluoStepOutcome clickTaskNpc(TaskExecutionContext context, XiuluoRoundState state) {
        return skeletonContinue(context, state, XiuluoPhase.ACCEPT_TASK_DIALOG);
    }

    private XiuluoStepOutcome acceptTaskDialog(TaskExecutionContext context, XiuluoRoundState state) {
        return skeletonContinue(context, state, XiuluoPhase.READ_OBJECTIVE);
    }

    private XiuluoStepOutcome readObjective(TaskExecutionContext context, XiuluoRoundState state) {
        return skeletonContinue(context, state, XiuluoPhase.NAVIGATE_TO_TARGET_MAP);
    }

    private XiuluoStepOutcome navigateToTargetMap(TaskExecutionContext context, XiuluoRoundState state) {
        // Real version: world-map route starts here and should yield once long pathing begins.
        return skeletonContinue(context, state, XiuluoPhase.NAVIGATE_TO_TARGET_POINT);
    }

    private XiuluoStepOutcome navigateToTargetPoint(TaskExecutionContext context, XiuluoRoundState state) {
        // Real version: mini-map click starts here and should yield once current-map pathing begins.
        return skeletonContinue(context, state, XiuluoPhase.CLICK_TARGET_NPC);
    }

    private XiuluoStepOutcome clickTargetNpc(TaskExecutionContext context, XiuluoRoundState state) {
        return skeletonContinue(context, state, XiuluoPhase.CONFIRM_ENTER_BATTLE);
    }

    private XiuluoStepOutcome confirmEnterBattle(TaskExecutionContext context, XiuluoRoundState state) {
        return skeletonContinue(context, state, XiuluoPhase.WAIT_COMBAT);
    }

    private XiuluoStepOutcome waitCombat(TaskExecutionContext context, XiuluoRoundState state) {
        // Real version: battle handoff belongs here; once in combat, the task must yield.
        return skeletonContinue(context, state, XiuluoPhase.RETURN_HOME);
    }

    private XiuluoStepOutcome returnHome(TaskExecutionContext context, XiuluoRoundState state) {
        return skeletonContinue(context, state, XiuluoPhase.WAIT_TEAM_RETURN);
    }

    private XiuluoStepOutcome waitTeamReturn(TaskExecutionContext context, XiuluoRoundState state) {
        return skeletonContinue(context, state, XiuluoPhase.ROUND_DONE);
    }

    private XiuluoStepOutcome skeletonContinue(TaskExecutionContext context,
                                               XiuluoRoundState state,
                                               XiuluoPhase nextPhase) {
        checkpoint(context);
        log.info("[xiuluo-v2] skeleton phase: round={} phase={} source={} objective={} -> {}",
                state.round(), state.phase(), state.source(), state.objective(), nextPhase);
        return XiuluoStepOutcome.continueTo(state.next(nextPhase, "skeleton:" + state.phase()), "skeleton transition");
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

    private void checkpoint(TaskExecutionContext context) {
        if (context != null) {
            context.throwIfStopRequested();
        }
        taskExecutionContextHolder.checkpointIfPresent();
    }

}
