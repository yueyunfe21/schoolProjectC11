package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.model.quest.QuestDetailCapture;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.service.NpcClickService;
import com.bot.dhxy.service.QuestManagerService;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.vision.ObjectiveTextRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private static final String XIULUO_TARGET_KEYWORD = "修罗";
    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/xiuluo_accept_xianlaiwu.png";
    private static final String ENTER_BATTLE_TEMPLATE = "images/template/dialog/xiuluo_enter_battle_kanda.png";
    private static final String RETURN_ITEM_TEMPLATE = "bag/xiuluo_return_item.png";
    private static final String OPTION_ACCEPT_TASK = "accept-task";
    private static final int STORY_OBJECTIVE_ATTEMPTS = 3;
    private static final NpcTarget ACCEPT_NPC = NpcTarget.builder()
            .key("xiuluo.acceptNpc")
            .mapName(START_MAP_NAME)
            .name(ACCEPT_NPC_NAME)
            .x(ACCEPT_NPC_X)
            .y(ACCEPT_NPC_Y)
            .role(NpcRole.QUEST_GIVER)
            .movementType(NpcMovementType.FIXED)
            .expectedDialogTemplatePath(ACCEPT_OPTION_TEMPLATE)
            .source("xiuluo-v2")
            .build();

    private final BotProperties botProperties;
    private final GameContext gameContext;
    private final NavigationService navigationService;
    private final CoordinateHelper coordinateHelper;
    private final GameStateUtil gameStateUtil;
    private final NpcClickService npcClickService;
    private final DialogService dialogService;
    private final QuestManagerService questManagerService;
    private final ObjectiveTextRecognitionService objectiveTextRecognitionService;
    private final AutoCombatService autoCombatService;
    private final BagService bagService;
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
            case NAVIGATE_TO_TARGET -> navigateToTarget(context, state);
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
        XiuluoStepOutcome pendingPathing = continueIfNavigationStillPathing(context, state, "navigate to accept NPC");
        if (pendingPathing != null) {
            return pendingPathing;
        }
        XiuluoRoundContext activeState = state.clearPathingWait("navigation-retry:" + state.phase());
        // 🧭 ACCEPT NPC NAV: go to the fixed task giver before opening/handling its dialog.
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(ACCEPT_NPC.getMapName())
                .targetX(ACCEPT_NPC.getX())
                .targetY(ACCEPT_NPC.getY())
                .targetName(ACCEPT_NPC.getName())
                .returnOnPathingStarted(true)
                .source("xiuluo-v2:acceptNpc")
                .build());
        return navigationOutcome(activeState, result, XiuluoPhase.ACCEPT_TASK_CLICK_NPC, "navigate to accept NPC");
    }

    private XiuluoStepOutcome clickTaskNpc(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * Normal entry clicks the fixed 灵兽村使者. NpcClickService owns tooltip/yellow-name/Ctrl
         * fallback order and records successful click evidence, while the phase only decides where
         * the Xiuluo transaction should continue.
         */
        boolean clicked = npcClickService.clickNpcSmart(ACCEPT_NPC.toClickRequest(gameContext.getMe()));
        if (!clicked) {
            return XiuluoStepOutcome.failed(state, "accept NPC click failed");
        }
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.ACCEPT_TASK_DIALOG, "accept-npc-clicked"),
                "accept NPC clicked");
    }

    private XiuluoStepOutcome acceptTaskDialog(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        String matched = dialogService.clickFirstKnownOptionGreenTemplateDirectForExclusive(
                List.of(new GreenTemplateClickSpec(
                        OPTION_ACCEPT_TASK, ACCEPT_OPTION_TEMPLATE, -5, 80, 4)),
                "xiuluo-v2:accept:" + state.source());
        if (!OPTION_ACCEPT_TASK.equals(matched)) {
            return XiuluoStepOutcome.failed(state, "accept dialog option not matched");
        }
        TaskSleep.sleepOrStop(context, 250L, "Xiuluo V2 task interrupted");
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.READ_OBJECTIVE, "accept-option-clicked"),
                "accept option clicked");
    }

    private XiuluoStepOutcome readObjective(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        Optional<NpcTarget> storyObjective = tryReadCurrentStoryObjective(context, state.source());
        if (storyObjective.isPresent()) {
            return XiuluoStepOutcome.continueTo(
                    state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, storyObjective.get(), "objective:story"),
                    "objective parsed from story dialog");
        }

        log.warn("[xiuluo-v2] story objective parse failed; trying task-panel fallback");
        Optional<NpcTarget> panelObjective = tryReadObjectiveFromTaskPanel(context, state.source() + ":task-panel");
        if (panelObjective.isPresent()) {
            return XiuluoStepOutcome.continueTo(
                    state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, panelObjective.get(), "objective:task-panel"),
                    "objective parsed from task panel");
        }

        return XiuluoStepOutcome.failed(state, "objective not found");
    }

    private XiuluoStepOutcome navigateToTarget(TaskExecutionContext context, XiuluoRoundContext state) {
        XiuluoStepOutcome pendingPathing = continueIfNavigationStillPathing(context, state, "navigate to target");
        if (pendingPathing != null) {
            return pendingPathing;
        }
        XiuluoRoundContext activeState = state.clearPathingWait("navigation-retry:" + state.phase());
        // 🗺️ TARGET NAV: task layer names the target; NavigationService owns map/current-map details.
        NpcTarget objective = activeState.objective();
        if (objective == null) {
            return XiuluoStepOutcome.failed(activeState, "cannot navigate to target without objective");
        }
        MapCoordinate approach = coordinateHelper.calculateApproachCoordinate(
                objective.getMapName(), objective.getX(), objective.getY());
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(objective.getMapName())
                .targetX(approach.getX())
                .targetY(approach.getY())
                .targetName(objective.getName())
                .returnOnPathingStarted(true)
                .source("xiuluo-v2:target")
                .build());
        return navigationOutcome(activeState, result, XiuluoPhase.CLICK_TARGET_NPC, "navigate to target");
    }

    private XiuluoStepOutcome clickTargetNpc(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        NpcTarget objective = state.objective();
        if (objective == null) {
            return XiuluoStepOutcome.failed(state, "cannot click target without objective");
        }
        /*
         * This is the same smart-click boundary as task NPC clicking. For combat targets, success is
         * verified by the "看打!" dialog template instead of assuming the mouse hit was enough.
         */
        NpcTarget combatTarget = NpcTarget.builder()
                .key(objective.getKey())
                .mapName(objective.getMapName())
                .name(objective.getName())
                .x(objective.getX())
                .y(objective.getY())
                .role(objective.getRole())
                .movementType(objective.getMovementType())
                .tuneX(objective.getTuneX())
                .tuneY(objective.getTuneY())
                .expectedDialogTemplatePath(ENTER_BATTLE_TEMPLATE)
                .source("xiuluo-v2:combatTarget:" + objective.getSource())
                .build();
        boolean clicked = npcClickService.clickNpcSmart(combatTarget.toClickRequest(gameContext.getMe()));
        if (!clicked) {
            return XiuluoStepOutcome.failed(state, "target click failed");
        }
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.CONFIRM_ENTER_BATTLE, "target-clicked"),
                "target clicked");
    }

    private XiuluoStepOutcome confirmEnterBattle(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * "看打!" is short; keep the click close to the matched glyph center so random offset does
         * not drift into blank dialog space.
         */
        boolean confirmed = dialogService.clickGreenTemplateOption(
                ENTER_BATTLE_TEMPLATE, "xiuluo-v2:enter-battle:" + state.source(), 6, 4);
        if (!confirmed) {
            return XiuluoStepOutcome.failed(state, "enter battle option not matched");
        }
        autoCombatService.initializeForCurrentWindow();
        TaskSleep.sleepOrStop(context, 1200L, "Xiuluo V2 task interrupted");
        return XiuluoStepOutcome.sharedState(
                state.next(XiuluoPhase.WAIT_COMBAT, "battle-confirmed"),
                "battle confirmed");
    }

    private XiuluoStepOutcome waitCombat(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        AutoCombatService.TickResult tick = autoCombatService.handleCombatTick(context, "xiuluo-v2", true);
        if (tick == AutoCombatService.TickResult.EXIT_RECOVERED) {
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.RETURN_HOME, "combat-finished"),
                    "combat exit recovered");
        }
        if (tick == AutoCombatService.TickResult.IN_COMBAT) {
            TaskSleep.sleepOrStop(context, autoCombatService.getDynamicPollingIntervalMs(), "Xiuluo V2 task interrupted");
            return XiuluoStepOutcome.sharedState(state, "combat still running");
        }
        TaskSleep.sleepOrStop(context, 1000L, "Xiuluo V2 task interrupted");
        return XiuluoStepOutcome.sharedState(state, "waiting for combat state");
    }

    private XiuluoStepOutcome returnHome(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        boolean used = bagService.findAndUseItemFromBack(BagService.MAIN_BAG, RETURN_ITEM_TEMPLATE, 5, context);
        if (!used) {
            return XiuluoStepOutcome.failed(state, "return item not found or not used");
        }
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.WAIT_TEAM_RETURN, "return-item-used"),
                "return item used");
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

    /**
     * Read the objective that appears immediately after accepting Xiuluo.
     *
     * @param context current task context for stop checks during repeated story screenshots.
     * @param source diagnostic source added to screenshot/OCR logs.
     * @return combat target parsed from the current story dialog, or empty when the dialog is not
     *         visible/recognizable.
     */
    private Optional<NpcTarget> tryReadCurrentStoryObjective(TaskExecutionContext context, String source) {
        for (int i = 1; i <= STORY_OBJECTIVE_ATTEMPTS; i++) {
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            String reason = "xiuluo-v2:story-objective:" + source + ":try" + i;
            Optional<NpcTarget> objective = parseObjective(dialogService.captureCurrentStoryImage(reason), reason);
            if (objective.isPresent()) {
                log.info("[xiuluo-v2] objective parsed from story: target={}", objective.get());
                return objective;
            }
            TaskSleep.sleepOrStop(context, 500L, "Xiuluo V2 task interrupted");
        }
        return Optional.empty();
    }

    /**
     * Fallback objective reader using the task panel after the accept story was missed.
     *
     * @param context current task context for stop checks before opening the task panel.
     * @param source diagnostic source added to screenshot/OCR logs.
     * @return combat target parsed from the current quest detail panel, or empty when unavailable.
     */
    private Optional<NpcTarget> tryReadObjectiveFromTaskPanel(TaskExecutionContext context, String source) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        QuestDetailCapture capture = questManagerService.captureCurrentQuestDetailForTask(TASK_CODE);
        return parseObjective(capture.image(), "xiuluo-v2:task-panel:" + source);
    }

    /**
     * Convert a captured green objective image into the target model used by navigation/clicking.
     *
     * @param image story or task-detail image; this method takes ownership and flushes it.
     * @param source diagnostic source passed through to the objective recognizer.
     * @return Xiuluo combat target with logical map coordinates, or empty for normal template miss.
     */
    private Optional<NpcTarget> parseObjective(BufferedImage image, String source) {
        if (image == null) {
            return Optional.empty();
        }
        try {
            Optional<ObjectiveTextResult> result = objectiveTextRecognitionService.recognize(image, source);
            return result.map(value -> NpcTarget.builder()
                    .key("xiuluo.combatTarget")
                    .mapName(value.mapName())
                    .name(XIULUO_TARGET_KEYWORD)
                    .x(value.x())
                    .y(value.y())
                    .role(NpcRole.COMBAT_TARGET)
                    .movementType(NpcMovementType.ROAMING)
                    .source("xiuluoObjective:" + value.mapSlug() + ":" + value.mapScore())
                    .build());
        } finally {
            image.flush();
        }
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
            return XiuluoStepOutcome.pathingStarted(
                    state.waitForPathing("pathing:" + state.phase()),
                    actionName + " pathing started");
        }
        // ⛔ STOPPED: preserve explicit stop separately from ordinary navigation failure.
        if (status == NavigationResultStatus.STOPPED) {
            return XiuluoStepOutcome.stopped(state, actionName + " stopped");
        }
        // ❌ FAILED: retry/fallback policy will live in the calling phase, not NavigationService.
        return XiuluoStepOutcome.failed(state, actionName + " failed: " + status);
    }

    private XiuluoStepOutcome continueIfNavigationStillPathing(TaskExecutionContext context,
                                                               XiuluoRoundContext state,
                                                               String actionName) {
        if (!state.waitingPathing()) {
            return null;
        }

        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        boolean moving = gameStateUtil.isMovingByPixelDiff();
        if (moving) {
            /*
             * The previous NavigationService call already clicked a route or mini-map point. While
             * the character is still pathing, this phase must only yield; submitting another map
             * search here is the retry storm seen in the logs.
             */
            log.info("[xiuluo-v2] navigation still pathing: phase={} action={}",
                    state.phase(), actionName);
            return XiuluoStepOutcome.pathingStarted(state, actionName + " still pathing");
        }

        log.info("[xiuluo-v2] navigation pathing wait ended: phase={} action={}",
                state.phase(), actionName);
        return null;
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
