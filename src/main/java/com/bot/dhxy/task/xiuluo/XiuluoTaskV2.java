package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.model.ocr.LocationInfo;
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
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.service.QuestManagerService;
import com.bot.dhxy.service.TeamReturnService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.service.dialog.DialogOptionPolicy;
import com.bot.dhxy.service.dialog.DialogStoryPolicy;
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
    private static final String QUEST_PANEL_TASK_CODE = "xiuluo";
    private static final String TASK_NAME = "修罗V2";
    private static final String START_MAP_NAME = "灵兽村";
    private static final String ACCEPT_NPC_NAME = "灵兽村使者";
    private static final int ACCEPT_NPC_X = 112;
    private static final int ACCEPT_NPC_Y = 93;
    private static final int ACCEPT_NPC_DIRECT_CLICK_DISTANCE = 10;
    private static final int START_EXIT_X = 11;
    private static final int START_EXIT_Y = 8;
    private static final int START_EXIT_PREPATH_SKIP_DISTANCE = 3;
    private static final int TARGET_APPROACH_ARRIVAL_TOLERANCE = 4;
    private static final String XIULUO_TARGET_KEYWORD = "修罗";
    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/xiuluo_accept_xianlaiwu.png";
    private static final String CANCEL_TASK_OPTION_TEMPLATE = "images/template/dialog/xiuluo_cancel_task.png";
    private static final String UNDER_FIVE_CONFIRM_TEMPLATE = "images/template/dialog/xiuluo_underfive_confirm.png";
    private static final String UNDER_FIVE_WAIT_TEMPLATE = "images/template/dialog/xiuluo_underfive_wait.png";
    private static final String UNDER_THREE_BLOCKED_TEMPLATE = "images/template/dialog/xiuluo_underthree_yichangqiangda.png";
    private static final String ENTER_BATTLE_TEMPLATE = "images/template/dialog/xiuluo_enter_battle_kanda.png";
    private static final String RETURN_ITEM_TEMPLATE = "bag/xiuluo_return_item.png";
    private static final String OPTION_ACCEPT_TASK = "xiuluo.acceptTask";
    private static final String OPTION_CANCEL_TASK_VISIBLE = "xiuluo.cancelTaskVisible";
    private static final String OPTION_ENTER_BATTLE = "xiuluo.enterBattle";
    private static final String OPTION_UNDER_FIVE_CONFIRM = "xiuluo.underFiveConfirm";
    private static final String OPTION_UNDER_FIVE_WAIT = "xiuluo.underFiveWait";
    private static final String DIALOG_UNDER_THREE_BLOCKED = "xiuluo.underThreeBlocked";
    private static final int STORY_OBJECTIVE_ATTEMPTS = 3;
    private static final int MAX_PHASE_RETRY = 1;
    private static final int MAX_RECOVERY_COUNT = 2;
    private static final int UNKNOWN_COMBAT_TARGET_DISTANCE_TOLERANCE = 10;
    private static final int RETURN_ITEM_VERIFY_ATTEMPTS = 2;
    private static final long RETURN_VERIFY_DELAY_MS = 500L;
    private static final long TASK_TURN_HANDOFF_DELAY_MS = 900L;
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
    private final PlayerStateService playerStateService;
    private final UICleanerService uiCleanerService;
    private final TeamReturnService teamReturnService;
    private final XiuluoHotStartResolver hotStartResolver;
    private final TaskTransactionRunner taskTransactionRunner;
    private final TaskExecutionContextHolder taskExecutionContextHolder;

    /*
     * Startup incense is a task-run guard, not the incense cooldown itself. PlayerStateService still
     * owns the real time/status rules; Xiuluo only decides the first safe point to ask that service.
     */
    private boolean startupIncenseChecked;
    private boolean startupIncensePending;

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
     * @return SUCCESS after the skeleton reaches the configured round count, or STOPPED if interrupted.
     *         Ordinary phase failures are recovered inside the Xiuluo round by restarting the accept
     *         flow instead of ending the whole window task.
     */
    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        TaskExecutionContext context = resolveExecutionContext(executionContext);
        int maxRuns = botProperties.getXiuluoMaxRuns();
        int completedRuns = 0;
        startupIncenseChecked = false;
        startupIncensePending = false;
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
                if (allowStartupTaskPanelFallback && roundContext.phase() == XiuluoPhase.PREPARE_ROUND) {
                    roundContext = resolveStartupTaskPanelHotStart(context, roundContext);
                }
                if (completedRuns == 0 && !startupIncenseChecked) {
                    if (roundContext.phase() == XiuluoPhase.ACCEPT_TASK_DIALOG
                            || roundContext.phase() == XiuluoPhase.READ_OBJECTIVE) {
                        startupIncensePending = true;
                        log.info("[xiuluo-v2] startup incense check deferred until target navigation: phase={}",
                                roundContext.phase());
                    } else if (roundContext.phase() == XiuluoPhase.WAIT_COMBAT) {
                        startupIncenseChecked = true;
                        log.info("[xiuluo-v2] startup incense check covered by post-combat recovery: phase={}",
                                roundContext.phase());
                    }
                }
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
        int consecutivePathingYields = 0;
        while (!roundContext.phase().isTerminal()) {
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");

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
                roundContext = restartRoundAfterPhaseFailure(context, currentContext, outcome);
                phaseLoopGuard = 0;
                consecutivePathingYields = 0;
                continue;
            }
            if (outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED) {
                consecutivePathingYields++;
                phaseLoopGuard = 0;
                yieldAfterMustYield(context, outcome);
                roundContext = outcome.nextState();
                continue;
            }
            if (outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
                /*
                 * Combat/team-wait phases can legitimately yield the same phase for a long time.
                 * They are external game states, not an internal phase loop; do not let the generic
                 * loop guard turn a long battle into a task failure.
                 */
                phaseLoopGuard = 0;
                consecutivePathingYields = 0;
                if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD) {
                    yieldAfterMustYield(context, outcome);
                }
                roundContext = outcome.nextState();
                continue;
            }
            if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD) {
                yieldAfterMustYield(context, outcome);
            }
            consecutivePathingYields = 0;
            if (++phaseLoopGuard > 32) {
                log.error("[xiuluo-v2] phase loop guard exceeded: roundContext={}", roundContext);
                roundContext = restartRoundAfterLoopGuard(context, roundContext);
                phaseLoopGuard = 0;
                consecutivePathingYields = 0;
                continue;
            }
            // ➡️ NEXT CONTEXT: outcome decides the next phase and carries objective/round/source forward.
            roundContext = outcome.nextState();
        }

        if (roundContext.phase() == XiuluoPhase.ROUND_DONE) {
            return TaskRunResult.SUCCESS;
        }
        return roundContext.phase() == XiuluoPhase.STOPPED ? TaskRunResult.STOPPED : TaskRunResult.SUCCESS;
    }

    private XiuluoRoundContext restartRoundAfterPhaseFailure(TaskExecutionContext context,
                                                             XiuluoRoundContext failedContext,
                                                             XiuluoStepOutcome outcome) {
        /*
         * Xiuluo failures usually mean the current accepted objective became unusable: bad route,
         * missed dialog, or stale task-panel text. Do not bubble this to WindowTaskRunner as a task
         * failure; abandon the current objective and re-enter the same round from the accept flow.
         */
        log.warn("[xiuluo-v2] phase failed; restart same round from accept flow: phase={} message={} next={}",
                failedContext.phase(), outcome.message(), outcome.nextState().phase());
        uiCleanerService.cleanUpAll();
        yieldAfterMustYield(context, outcome);
        return XiuluoRoundContext.start(failedContext.round());
    }

    private XiuluoRoundContext restartRoundAfterLoopGuard(TaskExecutionContext context,
                                                          XiuluoRoundContext currentContext) {
        log.warn("[xiuluo-v2] phase loop guard recovery; restart same round from accept flow: phase={} source={}",
                currentContext.phase(), currentContext.source());
        uiCleanerService.cleanUpAll();
        TaskSleep.sleepOrStop(context, TASK_TURN_HANDOFF_DELAY_MS, "Xiuluo V2 task interrupted");
        return XiuluoRoundContext.start(currentContext.round());
    }

    private void yieldAfterMustYield(TaskExecutionContext context, XiuluoStepOutcome outcome) {
        /*
         * Releasing the task turn is not enough by itself: this leader thread can immediately loop
         * and reacquire the fair lock before follower auto-battle polling gets a chance to tryLock.
         * A short handoff delay is intentional for shared states such as pathing and combat.
         */
        log.info("[xiuluo-v2] task turn handoff delay: result={} next={} delayMs={}",
                outcome.transactionResult(), outcome.nextState().phase(), TASK_TURN_HANDOFF_DELAY_MS);
        TaskSleep.sleepOrStop(context, TASK_TURN_HANDOFF_DELAY_MS, "Xiuluo V2 task interrupted");
    }

    private XiuluoRoundContext resolveStartupTaskPanelHotStart(TaskExecutionContext context,
                                                               XiuluoRoundContext roundContext) {
        /*
         * No dialog/combat on screen does not prove the character has no accepted 修罗 objective.
         * On the first startup only, read the task panel once before entering the accept-task chain.
         */
        Optional<NpcTarget> startupObjective = tryReadObjectiveFromTaskPanel(context, "hot-start:task-panel");
        if (startupObjective.isEmpty()) {
            log.info("[xiuluo-v2] startup task-panel hot-start missed; continue normal accept flow");
            return roundContext;
        }
        startupIncensePending = true;
        log.info("[xiuluo-v2] startup task-panel hot-start hit: objective={}", startupObjective.get());
        return roundContext.withObjective(
                XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK,
                startupObjective.get(),
                "hot-start:task-panel-objective");
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
            case AFTER_ACCEPT_MAINTENANCE_CHECK -> afterAcceptMaintenanceCheck(context, state);
            case BEFORE_ROUTE_MAINTENANCE_CHECK -> beforeRouteMaintenanceCheck(context, state);
            case NAVIGATE_TO_TARGET -> navigateToTarget(context, state);
            case CLICK_TARGET_NPC -> clickTargetNpc(context, state);
            case CONFIRM_ENTER_BATTLE -> confirmEnterBattle(context, state);
            case WAIT_COMBAT -> waitCombat(context, state);
            case RETURN_HOME -> returnHome(context, state);
            case NAVIGATE_BACK_TO_START -> navigateBackToStart(context, state);
            case WAIT_TEAM_RETURN -> waitTeamReturn(context, state);
            case ROUND_DONE, FAILED, STOPPED ->
                    XiuluoStepOutcome.failed(state, "terminal phase should not be executed: " + state.phase());
        };
    }

    // 🟢 PHASE HANDLERS: each method owns exactly one Xiuluo phase.
    private XiuluoStepOutcome prepareRound(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        log.info("[xiuluo-v2] prepare round: clean UI");
        uiCleanerService.cleanUpAll();
        if (!startupIncenseChecked && !startupIncensePending) {
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            log.info("[xiuluo-v2] startup incense check at prepare round");
            playerStateService.ensureSheYaoXiangActiveForLeaderTask("xiuluo-v2:startup-prepare", context);
            startupIncenseChecked = true;
            startupIncensePending = false;
        } else {
            log.info("[xiuluo-v2] prepare round skipped startup incense: checked={} pending={}",
                    startupIncenseChecked, startupIncensePending);
        }
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "prepare-ready"),
                "round prepared");
    }

    private XiuluoStepOutcome navigateToTaskNpc(TaskExecutionContext context, XiuluoRoundContext state) {
        XiuluoStepOutcome pendingPathing = continueIfNavigationStillPathing(context, state, "navigate to accept NPC");
        if (pendingPathing != null) {
            return pendingPathing;
        }
        XiuluoRoundContext activeState = state.clearPathingWait("navigation-retry:" + state.phase());
        /*
         * Starting near 灵兽村使者 should not force a minimap detour. Try the normal smart-click
         * pipeline first; if it cannot verify the accept dialog, fall back to the randomized minimap
         * navigation path below.
         */
        if (isNearAcceptNpc(gameContext.getMe())) {
            log.info("[xiuluo-v2] accept NPC nearby; try direct smart click before minimap navigation: player=({}, {}) npc=({}, {}) tolerance={}",
                    gameContext.getMe().getX(), gameContext.getMe().getY(),
                    ACCEPT_NPC_X, ACCEPT_NPC_Y, ACCEPT_NPC_DIRECT_CLICK_DISTANCE);
            if (npcClickService.clickNpcSmart(ACCEPT_NPC.toClickRequest(gameContext.getMe()))) {
                return XiuluoStepOutcome.continueTo(
                        activeState.next(XiuluoPhase.ACCEPT_TASK_DIALOG, "nearby-accept-npc-clicked"),
                        "accept NPC clicked from nearby position");
            }
            log.info("[xiuluo-v2] nearby accept NPC direct click failed; fallback to minimap navigation");
        }
        // 🧭 ACCEPT NPC NAV: go to the fixed task giver before opening/handling its dialog.
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(ACCEPT_NPC.getMapName())
                .targetX(ACCEPT_NPC.getX())
                .targetY(ACCEPT_NPC.getY())
                .targetName(ACCEPT_NPC.getName())
                .returnOnPathingStarted(true)
                .source("xiuluo-v2:acceptNpc")
                .build());
        XiuluoStepOutcome outcome = navigationOutcome(activeState, result, XiuluoPhase.ACCEPT_TASK_CLICK_NPC, "navigate to accept NPC");
        if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
            return recoverAcceptNavigationFailure(activeState);
        }
        return outcome;
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
            return recoverAcceptNpcClickFailure(context, state);
        }
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.ACCEPT_TASK_DIALOG, "accept-npc-clicked"),
                "accept NPC clicked");
    }

    private XiuluoStepOutcome acceptTaskDialog(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        Optional<XiuluoStepOutcome> knownDialog = handleKnownXiuluoOptionDialog(
                context, state, "xiuluo-v2:accept:" + state.source(), false);
        if (knownDialog.isEmpty()) {
            return recoverAcceptDialogFailure(state);
        }
        return knownDialog.get();
    }

    private XiuluoStepOutcome readObjective(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        Optional<NpcTarget> storyObjective = tryReadCurrentStoryObjective(context, state.source());
        if (storyObjective.isPresent()) {
            return XiuluoStepOutcome.continueTo(
                    state.withObjective(XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK, storyObjective.get(), "objective:story"),
                    "objective parsed from story dialog");
        }

        log.warn("[xiuluo-v2] story objective parse failed; trying task-panel fallback");
        Optional<NpcTarget> panelObjective = tryReadObjectiveFromTaskPanel(context, state.source() + ":task-panel");
        if (panelObjective.isPresent()) {
            return XiuluoStepOutcome.continueTo(
                    state.withObjective(XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK, panelObjective.get(), "objective:task-panel"),
                    "objective parsed from task panel");
        }

        Optional<XiuluoStepOutcome> knownDialog = handleKnownXiuluoOptionDialog(
                context, state, "xiuluo-v2:read-objective-known-dialog:" + state.source(), true);
        if (knownDialog.isPresent()) {
            return knownDialog.get();
        }

        Optional<XiuluoStepOutcome> blockedDialog = handleUnderThreeBlockedDialog(
                state, "xiuluo-v2:read-objective-under-three:" + state.source());
        if (blockedDialog.isPresent()) {
            return blockedDialog.get();
        }

        return recoverObjectiveReadFailure(context, state);
    }

    private XiuluoStepOutcome afterAcceptMaintenanceCheck(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * Reserved team-maintenance hook: heal-pet is cheapest right after accepting 修罗 because the
         * maintenance NPC is expected to be near the task giver. Keep this phase as a no-op until the
         * shared maintenance transaction exists.
         */
        log.info("[xiuluo-v2] after-accept maintenance hook skipped: objective={}", state.objective());
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.BEFORE_ROUTE_MAINTENANCE_CHECK, "after-accept-maintenance-skipped"),
                "after-accept maintenance hook skipped");
    }

    private XiuluoStepOutcome beforeRouteMaintenanceCheck(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * Reserved team-maintenance hook: repair-equipment can be inserted before the long route.
         * After that transaction finishes, the same objective is preserved and Xiuluo continues
         * normal target navigation.
         */
        log.info("[xiuluo-v2] before-route maintenance hook skipped: objective={}", state.objective());
        XiuluoStepOutcome exitPathing = startLeavingStartMapIfPresent(state);
        if (exitPathing != null) {
            return exitPathing;
        }
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.NAVIGATE_TO_TARGET, "before-route-maintenance-skipped"),
                "before-route maintenance hook skipped");
    }

    private XiuluoStepOutcome startLeavingStartMapIfPresent(XiuluoRoundContext state) {
        String currentMap = gameContext.getMe() == null ? null : gameContext.getMe().getCurrentMapName();
        if (!START_MAP_NAME.equals(currentMap)) {
            log.info("[xiuluo-v2] skip start-map exit pre-pathing: current={} startMap={}",
                    currentMap, START_MAP_NAME);
            return null;
        }
        PlayerCharacter me = gameContext.getMe();
        if (isNearStartExit(me)) {
            log.info("[xiuluo-v2] skip start-map exit pre-pathing: already near exit player=({}, {}) exit=({}, {}) tolerance={}",
                    me.getX(), me.getY(), START_EXIT_X, START_EXIT_Y, START_EXIT_PREPATH_SKIP_DISTANCE);
            return null;
        }

        /*
         * Xiuluo's old fast path clicked the Ling Shou Village exit immediately after reading the
         * objective. That lets the leader start walking while the next phase opens the world-map
         * route. This is only an optimization; failure must not block the formal target navigation.
         */
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(START_MAP_NAME)
                .targetX(START_EXIT_X)
                .targetY(START_EXIT_Y)
                .targetName("灵兽村出口")
                .returnOnPathingStarted(true)
                .source("xiuluo-v2:start-exit-prepath")
                .build());
        NavigationResultStatus status = result.getStatus();
        log.info("[xiuluo-v2] start-map exit pre-pathing result: status={} message={}",
                status, result.getMessage());
        if (status == NavigationResultStatus.STOPPED) {
            return XiuluoStepOutcome.stopped(state, "start-map exit pre-pathing stopped");
        }
        if (status == NavigationResultStatus.PATHING_STARTED) {
            /*
             * Exit pre-pathing is only a head start before formal target navigation. Keep the turn
             * and immediately continue to NAVIGATE_TO_TARGET so world-map routing can be submitted
             * while the character is already walking out of 灵兽村. The real target navigation phase
             * still owns PATHING_STARTED/yield once the route to the objective begins.
             */
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.NAVIGATE_TO_TARGET, "start-exit-pathing"),
                    "start-map exit pathing started; continue target route while walking");
        }
        return null;
    }

    private boolean isNearStartExit(PlayerCharacter me) {
        if (me == null) {
            return false;
        }
        return Math.abs(me.getX() - START_EXIT_X) <= START_EXIT_PREPATH_SKIP_DISTANCE
                && Math.abs(me.getY() - START_EXIT_Y) <= START_EXIT_PREPATH_SKIP_DISTANCE;
    }

    private XiuluoStepOutcome navigateToTarget(TaskExecutionContext context, XiuluoRoundContext state) {
        XiuluoStepOutcome pendingPathing = continueIfNavigationStillPathing(context, state, "navigate to target");
        if (pendingPathing != null) {
            return pendingPathing;
        }
        if (!startupIncenseChecked && startupIncensePending) {
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            log.info("[xiuluo-v2] startup incense check before target navigation");
            playerStateService.ensureSheYaoXiangActiveForLeaderTask("xiuluo-v2:startup-before-target-nav", context);
            startupIncenseChecked = true;
            startupIncensePending = false;
        }
        XiuluoRoundContext activeState = state.clearPathingWait("navigation-retry:" + state.phase());
        // 🗺️ TARGET NAV: task layer names the target; NavigationService owns map/current-map details.
        NpcTarget objective = activeState.objective();
        if (objective == null) {
            log.warn("[xiuluo-v2] navigate target requested without objective; go back to objective reader");
            return XiuluoStepOutcome.continueTo(
                    activeState.next(XiuluoPhase.READ_OBJECTIVE, "missing-objective-before-navigation"),
                    "missing objective before navigation; reread objective");
        }
        MapCoordinate approach = coordinateHelper.calculateApproachCoordinate(
                objective.getMapName(), objective.getX(), objective.getY());
        /*
         * A resumed or recently routed Xiuluo leader may already stand beside the combat target.
         * Check the cheap minimap/template location once before submitting another mini-map click;
         * otherwise the task can waste a turn opening the mini-map even though the next valid phase
         * is to click the target and enter battle.
         */
        playerStateService.syncMyPosition();
        if (hasReachedTargetApproach(activeState)) {
            return XiuluoStepOutcome.continueTo(
                    activeState.next(XiuluoPhase.CLICK_TARGET_NPC, "target-approach-already-reached"),
                    "target approach already reached; click target");
        }
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(objective.getMapName())
                .targetX(approach.getX())
                .targetY(approach.getY())
                .targetName(objective.getName())
                .returnOnPathingStarted(true)
                .source("xiuluo-v2:target")
                .build());
        XiuluoStepOutcome outcome = navigationOutcome(activeState, result, XiuluoPhase.CLICK_TARGET_NPC, "navigate to target");
        if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
            return recoverTargetNavigationFailure(context, activeState, outcome.message());
        }
        return outcome;
    }

    private boolean isNearAcceptNpc(PlayerCharacter me) {
        if (me == null || me.getCurrentMapName() == null || !START_MAP_NAME.equals(me.getCurrentMapName())) {
            return false;
        }
        return Math.abs(me.getX() - ACCEPT_NPC_X) <= ACCEPT_NPC_DIRECT_CLICK_DISTANCE
                && Math.abs(me.getY() - ACCEPT_NPC_Y) <= ACCEPT_NPC_DIRECT_CLICK_DISTANCE;
    }

    private XiuluoStepOutcome clickTargetNpc(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        NpcTarget objective = state.objective();
        if (objective == null) {
            log.warn("[xiuluo-v2] click target requested without objective; go back to objective reader");
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.READ_OBJECTIVE, "missing-objective-before-target-click"),
                    "missing objective before target click; reread objective");
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
            return recoverTargetClickFailure(context, state);
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
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
                "xiuluo-v2:enter-battle:" + state.source(),
                List.of(new GreenTemplateClickSpec(OPTION_ENTER_BATTLE, ENTER_BATTLE_TEMPLATE, -6, 6, 4)),
                true));
        if (!OPTION_ENTER_BATTLE.equals(result.getActionKey())) {
            return recoverEnterBattleConfirmFailure(state);
        }
        autoCombatService.initializeForCurrentWindow();
        TaskSleep.sleepOrStop(context, 1200L, "Xiuluo V2 task interrupted");
        return XiuluoStepOutcome.sharedState(
                state.withXiuluoBattleStarted(XiuluoPhase.WAIT_COMBAT, "battle-confirmed"),
                "battle confirmed");
    }

    private XiuluoStepOutcome waitCombat(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        AutoCombatService.TickResult tick = autoCombatService.handleCombatTick(context, "xiuluo-v2", true);
        if (tick == AutoCombatService.TickResult.EXIT_RECOVERED) {
            if (!state.enteredBattleByXiuluo()) {
                return resolveUnknownCombatExit(context, state);
            }
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.RETURN_HOME, "combat-finished"),
                    "combat exit recovered");
        }
        if (tick == AutoCombatService.TickResult.IN_COMBAT) {
            /*
             * Do not sleep while the task turn is held. Combat is a shared state: the leader should
             * release quickly so follower auto-battle windows can acquire the turn and press their
             * own auto-combat buttons.
             */
            return XiuluoStepOutcome.sharedState(state, "combat still running");
        }
        return XiuluoStepOutcome.sharedState(state, "waiting for combat state");
    }

    private XiuluoStepOutcome resolveUnknownCombatExit(TaskExecutionContext context, XiuluoRoundContext state) {
        /*
         * Hot-start can enter WAIT_COMBAT while the player is already fighting. In that case the
         * combat may be unrelated to Xiuluo, or it may be a random encounter during navigation. Do
         * not use the return item until map/coordinate evidence says the player could have fought
         * the target, and the task panel no longer shows an active Xiuluo objective.
         */
        NpcTarget objective = state.objective();
        LocationInfo current = playerStateService.syncMyPosition();
        if (objective != null && current != null) {
            if (!isSameMap(current.mapName, objective.getMapName())) {
                log.info("[xiuluo-v2] unknown combat exit outside target map: current={} targetMap={}; continue navigation",
                        current, objective.getMapName());
                return XiuluoStepOutcome.continueTo(
                        state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, objective, "unknown-combat-map-mismatch"),
                        "unknown combat exit outside target map; continue navigation");
            }
            if (!isNearObjective(current, objective)) {
                log.info("[xiuluo-v2] unknown combat exit far from target: current={} target=({}, {}) tolerance={}; continue navigation",
                        current, objective.getX(), objective.getY(), UNKNOWN_COMBAT_TARGET_DISTANCE_TOLERANCE);
                return XiuluoStepOutcome.continueTo(
                        state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, objective, "unknown-combat-far-from-target"),
                        "unknown combat exit far from target; continue navigation");
            }
        }

        Optional<NpcTarget> activeObjective = tryReadObjectiveFromTaskPanel(
                context, state.source() + ":unknown-combat-exit");
        if (activeObjective.isPresent()) {
            log.info("[xiuluo-v2] combat exit source unknown; active Xiuluo objective still exists: target={}",
                    activeObjective.get());
            return XiuluoStepOutcome.continueTo(
                    state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, activeObjective.get(), "unknown-combat-objective-present"),
                    "unknown combat exit; continue active objective");
        }

        return attemptVerifiedReturnAfterUnknownCombat(context, state);
    }

    private XiuluoStepOutcome attemptVerifiedReturnAfterUnknownCombat(TaskExecutionContext context,
                                                                      XiuluoRoundContext state) {
        for (int attempt = 1; attempt <= RETURN_ITEM_VERIFY_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            boolean verified = useReturnItemAndVerifyStartMap(
                    context, "unknown-combat", attempt, RETURN_ITEM_VERIFY_ATTEMPTS);
            if (verified) {
                return XiuluoStepOutcome.continueTo(
                        state.next(XiuluoPhase.WAIT_TEAM_RETURN, "unknown-combat-return-verified"),
                        "unknown combat exit; return item verified");
            }

            uiCleanerService.cleanUpAll();
            Optional<NpcTarget> activeObjective = tryReadObjectiveFromTaskPanel(
                    context, state.source() + ":unknown-combat-return-attempt" + attempt);
            if (activeObjective.isPresent()) {
                log.info("[xiuluo-v2] objective found after failed return attempt: target={}", activeObjective.get());
                return XiuluoStepOutcome.continueTo(
                        state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, activeObjective.get(), "unknown-combat-objective-after-return"),
                        "unknown combat exit; objective found after return attempt");
            }
        }

        log.warn("[xiuluo-v2] unknown combat exit could not verify return and no objective was found; restart accept chain");
        return XiuluoStepOutcome.continueTo(
                state.recoverTo(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "unknown-combat-return-unverified"),
                "unknown combat exit; return unverified, restart accept chain");
    }

    private XiuluoStepOutcome returnHome(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        boolean returned = false;
        for (int attempt = 1; attempt <= RETURN_ITEM_VERIFY_ATTEMPTS; attempt++) {
            returned = useReturnItemAndVerifyStartMap(context, "known-combat", attempt, RETURN_ITEM_VERIFY_ATTEMPTS);
            if (returned) {
                break;
            }
            uiCleanerService.cleanUpAll();
        }
        if (!returned) {
            return recoverReturnHomeFailure(context, state);
        }
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.WAIT_TEAM_RETURN, "return-item-verified"),
                "return item verified");
    }

    private XiuluoStepOutcome navigateBackToStart(TaskExecutionContext context, XiuluoRoundContext state) {
        XiuluoStepOutcome pendingPathing = continueIfNavigationStillPathing(context, state, "navigate back to start");
        if (pendingPathing != null) {
            return pendingPathing;
        }

        /*
         * Return-item failure should not mark the task as fully clean while the leader is still on a
         * remote map. Use the normal navigation stack to get back to 灵兽村, then finish this round
         * without accepting a new task inside the same round.
         */
        XiuluoRoundContext activeState = state.clearPathingWait("navigation-retry:" + state.phase());
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(ACCEPT_NPC.getMapName())
                .targetX(ACCEPT_NPC.getX())
                .targetY(ACCEPT_NPC.getY())
                .targetName(ACCEPT_NPC.getName())
                .returnOnPathingStarted(true)
                .source("xiuluo-v2:returnFallback")
                .build());
        XiuluoStepOutcome outcome = navigationOutcome(activeState, result, XiuluoPhase.ROUND_DONE, "navigate back to start");
        if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
            uiCleanerService.cleanUpAll();
            return retryCurrentOrRecover(activeState, XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC,
                    "return fallback navigation failed");
        }
        return outcome;
    }

    private XiuluoStepOutcome waitTeamReturn(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * After using the Xiuluo return item, a dead member may need a short window to click return.
         * The leader only waits when the configured return signal is visible; otherwise this is a
         * cheap no-op and the next round can start while the task turn is still held.
         */
        boolean waited = teamReturnService.waitForMembersReturnIfNeeded(context, "xiuluo-v2:return-home");
        if (!botProperties.isXiuluoAllowUnderFiveMembers() && teamReturnService.isReturnTeamSignalPresent()) {
            log.warn("[xiuluo-v2] team return signal still present and under-five is disabled; keep waiting");
            return XiuluoStepOutcome.sharedState(state, "team return still pending");
        }
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.ROUND_DONE, waited ? "team-return-waited" : "team-return-not-needed"),
                waited ? "team return wait finished" : "team return wait not needed");
    }

    private XiuluoStepOutcome skeletonContinue(TaskExecutionContext context,
                                               XiuluoRoundContext state,
                                               XiuluoPhase nextPhase) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        log.info("[xiuluo-v2] skeleton phase: round={} phase={} source={} objective={} -> {}",
                state.round(), state.phase(), state.source(), state.objective(), nextPhase);
        return XiuluoStepOutcome.continueTo(state.next(nextPhase, "skeleton:" + state.phase()), "skeleton transition");
    }

    private XiuluoStepOutcome recoverAcceptNavigationFailure(XiuluoRoundContext state) {
        /*
         * The accept NPC route is already the normal navigation path. On failure, only remove
         * possible UI blockers and retry the same phase once; do not invent a separate navigation
         * mode here.
         */
        uiCleanerService.cleanUpAll();
        return retryCurrentOrRecover(state, XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC,
                "accept NPC navigation failed");
    }

    private XiuluoStepOutcome recoverAcceptNpcClickFailure(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * Smart click can return false even when the NPC dialog was already opened by the game.
         * Use the same scoped dialog handler as the normal accept phase, so recovery does not depend
         * on DialogService visibility-only internals.
         */
        Optional<XiuluoStepOutcome> knownDialog = handleKnownXiuluoOptionDialog(
                context, state, "xiuluo-v2:accept-click-failed:" + state.source(), false);
        if (knownDialog.isPresent()) {
            log.info("[xiuluo-v2] accept NPC click reported false, but a known Xiuluo dialog was handled");
            return knownDialog.get();
        }

        DialogResult inspectResult = dialogService.handleDialog(DialogHandleRequest.builder()
                .sourceTask("xiuluo-v2:accept-click-failed:inspect:" + state.source())
                .operation(DialogOperation.CLEANUP)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.IGNORE)
                .build());
        if (inspectResult.getStatus() == DialogResultStatus.STORY_IGNORED) {
            log.info("[xiuluo-v2] accept NPC click reported false, but story objective is already visible");
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.READ_OBJECTIVE, "story-already-open"),
                    "story objective already open");
        }
        uiCleanerService.cleanUpAll();
        return retryCurrentOrRecover(state, XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC,
                "accept NPC click failed");
    }

    private XiuluoStepOutcome recoverAcceptDialogFailure(XiuluoRoundContext state) {
        uiCleanerService.cleanUpAll();
        return retryCurrentOrRecover(state, XiuluoPhase.ACCEPT_TASK_CLICK_NPC,
                "accept dialog option not matched");
    }

    private XiuluoStepOutcome recoverObjectiveReadFailure(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * Objective read owns the story/task-panel fallback. If both miss, first re-check known
         * Xiuluo dialogs: an under-five/under-three prompt or reopened accept dialog can legitimately
         * appear here and should be routed by action key instead of being treated as unknown UI.
         */
        Optional<XiuluoStepOutcome> knownDialog = handleKnownXiuluoOptionDialog(
                context, state, "xiuluo-v2:objective-recovery:" + state.source(), true);
        if (knownDialog.isPresent()) {
            return knownDialog.get();
        }
        Optional<XiuluoStepOutcome> blockedDialog = handleUnderThreeBlockedDialog(
                state, "xiuluo-v2:objective-recovery-under-three:" + state.source());
        if (blockedDialog.isPresent()) {
            return blockedDialog.get();
        }

        /*
         * After scoped 修罗 checks miss, only close generic X windows. Do not click random option
         * rows here because the open dialog may be unrelated business state.
         */
        uiCleanerService.closeAllGenericWindows();
        return retryCurrentOrRecover(state, XiuluoPhase.ACCEPT_TASK_CLICK_NPC,
                "objective not found");
    }

    private XiuluoStepOutcome recoverTargetNavigationFailure(TaskExecutionContext context,
                                                             XiuluoRoundContext state,
                                                             String reason) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        uiCleanerService.cleanUpAll();
        Optional<NpcTarget> refreshed = tryReadObjectiveFromTaskPanel(context, state.source() + ":nav-recovery");
        XiuluoRoundContext recoveredState = refreshed
                .map(target -> state.recoverToWithObjective(XiuluoPhase.NAVIGATE_TO_TARGET, target, "objective-refreshed"))
                .orElseGet(() -> state.recoverTo(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "target-nav-recovery-failed"));
        if (refreshed.isPresent()) {
            if (recoveredState.recoveryCount() > MAX_RECOVERY_COUNT) {
                log.warn("[xiuluo-v2] target navigation recovery limit exceeded after objective refresh; restart accept flow: reason={}",
                        reason);
                return XiuluoStepOutcome.continueTo(
                        XiuluoRoundContext.start(state.round()),
                        "target navigation recovery limit exceeded; restart accept flow");
            }
            return recoverOrFail(recoveredState, "target navigation failed; refreshed objective");
        }
        log.warn("[xiuluo-v2] target navigation failed and objective refresh missed; restart accept flow: reason={}",
                reason);
        return XiuluoStepOutcome.continueTo(
                XiuluoRoundContext.start(state.round()),
                "target navigation failed and objective refresh missed; restart accept flow");
    }

    private XiuluoStepOutcome recoverTargetClickFailure(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * A false smart-click can still leave the "看打!" dialog open. Recovery handles the known
         * template first, then falls back to OCR keyword click through the same structured dialog
         * boundary before cleaning the UI.
         */
        DialogResult templateResult = dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
                "xiuluo-v2:target-click-failed:" + state.source(),
                List.of(new GreenTemplateClickSpec(OPTION_ENTER_BATTLE, ENTER_BATTLE_TEMPLATE, -6, 6, 4)),
                true));
        if (OPTION_ENTER_BATTLE.equals(templateResult.getActionKey())) {
            return enterBattleFromRecoveredDialog(context, state, "battle-confirmed-template-recovery");
        }

        DialogResult keywordResult = dialogService.handleDialog(DialogHandleRequest.handleKeywordOption(
                "xiuluo-v2:enter-battle-ocr:" + state.source(), "看打", false));
        if (keywordResult.getStatus() == DialogResultStatus.OPTION_KEYWORD_CLICKED) {
            log.info("[xiuluo-v2] enter-battle option clicked by OCR fallback: point=({}, {}) text={}",
                    keywordResult.getAbsoluteX(), keywordResult.getAbsoluteY(), keywordResult.getMatchedText());
            return enterBattleFromRecoveredDialog(context, state, "battle-confirmed-ocr");
        }
        uiCleanerService.cleanUpAll();
        if (state.phaseRetryCount() < MAX_PHASE_RETRY) {
            return XiuluoStepOutcome.continueTo(
                    state.retrySamePhase("retry-target-click"),
                    "target click failed; retry current phase");
        }

        Optional<NpcTarget> refreshed = tryReadObjectiveFromTaskPanel(context, state.source() + ":target-click-recovery");
        XiuluoRoundContext recoveredState = refreshed
                .map(target -> state.recoverToWithObjective(XiuluoPhase.NAVIGATE_TO_TARGET, target, "target-click-objective-refreshed"))
                .orElseGet(() -> state.recoverTo(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "target-click-recovery-failed"));
        if (refreshed.isPresent()) {
            return recoverOrFail(recoveredState, "target click failed; refreshed objective");
        }
        return recoverOrFail(recoveredState, "target click failed and objective refresh missed");
    }

    private XiuluoStepOutcome enterBattleFromRecoveredDialog(TaskExecutionContext context,
                                                             XiuluoRoundContext state,
                                                             String reason) {
        autoCombatService.initializeForCurrentWindow();
        TaskSleep.sleepOrStop(context, 1200L, "Xiuluo V2 task interrupted");
        return XiuluoStepOutcome.sharedState(
                state.withXiuluoBattleStarted(XiuluoPhase.WAIT_COMBAT, reason),
                "enter battle option clicked by recovery");
    }

    private XiuluoStepOutcome recoverEnterBattleConfirmFailure(XiuluoRoundContext state) {
        uiCleanerService.cleanUpAll();
        return retryCurrentOrRecover(state, XiuluoPhase.CLICK_TARGET_NPC,
                "enter battle option not matched");
    }

    private XiuluoStepOutcome recoverReturnHomeFailure(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        uiCleanerService.cleanUpAll();
        if (state.phaseRetryCount() < MAX_PHASE_RETRY) {
            return XiuluoStepOutcome.continueTo(
                    state.retrySamePhase("retry-return-home"),
                    "return item not found or not used; retry");
        }
        log.warn("[xiuluo-v2] return item unavailable after retry; navigate back to start before finishing round");
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.NAVIGATE_BACK_TO_START, "return-item-unavailable"),
                "return item unavailable; navigate back by normal path");
    }

    private boolean useReturnItemAndVerifyStartMap(TaskExecutionContext context,
                                                   String source,
                                                   int attempt,
                                                   int maxAttempts) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        log.info("[xiuluo-v2] use return item and verify start map: source={} attempt={}/{}",
                source, attempt, maxAttempts);
        boolean used = bagService.findAndUseItemFromBack(BagService.MAIN_BAG, RETURN_ITEM_TEMPLATE, 5, context);
        if (!used) {
            log.warn("[xiuluo-v2] return item not found/used: source={} attempt={}/{}",
                    source, attempt, maxAttempts);
            return false;
        }

        /*
         * A bag click only proves the item was clicked. The task contract is stronger: after using
         * the Xiuluo return item, the current map must become 灵兽村 before the next phase can trust
         * that the round has returned to the task giver area.
         */
        TaskSleep.sleepOrStop(context, RETURN_VERIFY_DELAY_MS, "Xiuluo V2 task interrupted");
        LocationInfo afterReturn = playerStateService.syncMyPosition();
        if (afterReturn != null && isSameMap(afterReturn.mapName, START_MAP_NAME)) {
            log.info("[xiuluo-v2] return item verified: source={} location={}", source, afterReturn);
            return true;
        }
        log.warn("[xiuluo-v2] return item used but start map not verified: source={} location={}",
                source, afterReturn);
        return false;
    }

    private boolean isSameMap(String currentMapName, String expectedMapName) {
        return currentMapName != null && expectedMapName != null && currentMapName.equals(expectedMapName);
    }

    private boolean isNearObjective(LocationInfo current, NpcTarget objective) {
        return Math.abs(current.x - objective.getX()) <= UNKNOWN_COMBAT_TARGET_DISTANCE_TOLERANCE
                && Math.abs(current.y - objective.getY()) <= UNKNOWN_COMBAT_TARGET_DISTANCE_TOLERANCE;
    }

    /**
     * Handle option dialogs that are already known to the Xiuluo phase machine.
     *
     * @param context task context used for stop checks and post-click waits.
     * @param state current Xiuluo round state; the returned outcome chooses the next phase.
     * @param source diagnostic source added to dialog logs.
     * @param verifyDialogType true when the caller needs DialogService to classify the current
     *                         dialog before template matching. False is used immediately after an
     *                         option dialog has already been established by the caller.
     * @return a phase outcome when a known template was clicked, otherwise empty so caller can clean
     *         UI or retry without guessing.
     */
    private Optional<XiuluoStepOutcome> handleKnownXiuluoOptionDialog(TaskExecutionContext context,
                                                                      XiuluoRoundContext state,
                                                                      String source,
                                                                      boolean verifyDialogType) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
                source,
                xiuluoKnownOptionSpecs(),
                verifyDialogType));
        String actionKey = result.getActionKey();
        if (actionKey == null || actionKey.isBlank()) {
            if (isAcceptDialogVisibleByCancelOption(source)) {
                log.info("[xiuluo-v2] accept dialog visible through cancel-task option: source={}", source);
                return Optional.of(retryCurrentOrRecover(state, XiuluoPhase.ACCEPT_TASK_CLICK_NPC,
                        "accept dialog visible through cancel option but accept option not matched"));
            }
            log.info("[xiuluo-v2] no known Xiuluo option dialog matched: source={} status={}",
                    source, result.getStatus());
            return Optional.empty();
        }

        log.info("[xiuluo-v2] known Xiuluo option handled: source={} actionKey={} click=({}, {})",
                source, actionKey, result.getAbsoluteX(), result.getAbsoluteY());
        return switch (actionKey) {
            case OPTION_ACCEPT_TASK -> {
                TaskSleep.sleepOrStop(context, 250L, "Xiuluo V2 task interrupted");
                yield Optional.of(XiuluoStepOutcome.continueTo(
                        state.next(XiuluoPhase.READ_OBJECTIVE, "known-dialog:accept-task"),
                        "accept option clicked"));
            }
            case OPTION_ENTER_BATTLE -> Optional.of(
                    enterBattleFromRecoveredDialog(context, state, "known-dialog:enter-battle"));
            case OPTION_UNDER_FIVE_CONFIRM -> {
                TaskSleep.sleepOrStop(context, 250L, "Xiuluo V2 task interrupted");
                yield Optional.of(XiuluoStepOutcome.continueTo(
                        state.next(XiuluoPhase.READ_OBJECTIVE, "known-dialog:under-five-confirm"),
                        "under-five prompt confirmed; read objective"));
            }
            case OPTION_UNDER_FIVE_WAIT -> Optional.of(XiuluoStepOutcome.sharedState(
                    state.next(XiuluoPhase.WAIT_TEAM_RETURN, "known-dialog:under-five-wait"),
                    "under-five prompt declined by config; wait for team"));
            default -> Optional.empty();
        };
    }

    private List<GreenTemplateClickSpec> xiuluoKnownOptionSpecs() {
        boolean allowUnderFive = botProperties.isXiuluoAllowUnderFiveMembers();
        GreenTemplateClickSpec underFiveSpec = allowUnderFive
                ? new GreenTemplateClickSpec(OPTION_UNDER_FIVE_CONFIRM, UNDER_FIVE_CONFIRM_TEMPLATE, -24, 24, 4)
                : new GreenTemplateClickSpec(OPTION_UNDER_FIVE_WAIT, UNDER_FIVE_WAIT_TEMPLATE, -24, 24, 4);
        return List.of(
                new GreenTemplateClickSpec(OPTION_ACCEPT_TASK, ACCEPT_OPTION_TEMPLATE, -5, 80, 4),
                new GreenTemplateClickSpec(OPTION_ENTER_BATTLE, ENTER_BATTLE_TEMPLATE, -6, 6, 4),
                underFiveSpec);
    }

    private boolean isAcceptDialogVisibleByCancelOption(String source) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyExpectedOptionDialog(
                "xiuluo-v2:cancel-visible:" + source,
                CANCEL_TASK_OPTION_TEMPLATE));
        boolean visible = result.getStatus() == DialogResultStatus.GREEN_TEMPLATE_VISIBLE;
        log.info("[xiuluo-v2] cancel-task option visibility: source={} visible={} status={}",
                source, visible, result.getStatus());
        return visible;
    }

    private Optional<XiuluoStepOutcome> handleUnderThreeBlockedDialog(XiuluoRoundContext state, String source) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyWhiteTemplate(
                source,
                DIALOG_UNDER_THREE_BLOCKED,
                UNDER_THREE_BLOCKED_TEMPLATE));
        if (DIALOG_UNDER_THREE_BLOCKED.equals(result.getActionKey())
                && result.getStatus() == DialogResultStatus.WHITE_TEMPLATE_VISIBLE) {
            log.warn("[xiuluo-v2] under-three blocked dialog detected: source={} point=({}, {})",
                    source, result.getAbsoluteX(), result.getAbsoluteY());
            return Optional.of(XiuluoStepOutcome.failed(state,
                    "xiuluo blocked: team has fewer than three members"));
        }
        log.info("[xiuluo-v2] under-three blocked dialog not matched: source={} status={}",
                source, result.getStatus());
        return Optional.empty();
    }

    private XiuluoStepOutcome retryCurrentOrRecover(XiuluoRoundContext state,
                                                    XiuluoPhase recoveryPhase,
                                                    String reason) {
        if (state.phaseRetryCount() < MAX_PHASE_RETRY) {
            return XiuluoStepOutcome.continueTo(
                    state.retrySamePhase("retry:" + state.phase()),
                    reason + "; retry current phase");
        }
        return recoverOrFail(state.recoverTo(recoveryPhase, "recover:" + state.phase()),
                reason + "; recover to " + recoveryPhase);
    }

    private XiuluoStepOutcome recoverOrFail(XiuluoRoundContext recoveredState, String message) {
        if (recoveredState.recoveryCount() > MAX_RECOVERY_COUNT) {
            return XiuluoStepOutcome.failed(recoveredState, message + "; recovery limit exceeded");
        }
        return XiuluoStepOutcome.continueTo(recoveredState, message);
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
            DialogResult result = dialogService.handleDialog(DialogHandleRequest.readStoryObjective(reason));
            Optional<NpcTarget> objective = Optional.ofNullable(result.getObjective())
                    .map(this::toXiuluoObjective);
            if (objective.isPresent()) {
                log.info("[xiuluo-v2] objective parsed from story: target={}", objective.get());
                return objective;
            }
            TaskSleep.sleepOrStop(context, 500L, "Xiuluo V2 task interrupted");
        }
        return Optional.empty();
    }

    private NpcTarget toXiuluoObjective(ObjectiveTextResult value) {
        return NpcTarget.builder()
                .key("xiuluo.combatTarget")
                .mapName(value.mapName())
                .name(XIULUO_TARGET_KEYWORD)
                .x(value.x())
                .y(value.y())
                .role(NpcRole.COMBAT_TARGET)
                .movementType(NpcMovementType.ROAMING)
                .source("xiuluoObjective:" + value.mapSlug() + ":" + value.mapScore())
                .build();
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
        QuestDetailCapture capture = questManagerService.captureCurrentQuestDetailForTask(QUEST_PANEL_TASK_CODE);
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
            return result.map(this::toXiuluoObjective);
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
        if (hasReachedTargetApproach(state)) {
            /*
             * Pixel movement can stay noisy near animated maps or spell effects. The phase should
             * not stay in PATHING_STARTED once the tracked minimap coordinate is already at the
             * approach point; otherwise 修罗 stands beside the target forever and never clicks it.
             */
            log.info("[xiuluo-v2] navigation pathing wait ended by approach coordinate: phase={} action={}",
                    state.phase(), actionName);
            return null;
        }
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

    private boolean hasReachedTargetApproach(XiuluoRoundContext state) {
        if (state.phase() != XiuluoPhase.NAVIGATE_TO_TARGET || state.objective() == null) {
            return false;
        }
        PlayerCharacter me = gameContext.getMe();
        if (me == null || me.getCurrentMapName() == null) {
            return false;
        }
        NpcTarget objective = state.objective();
        if (!objective.getMapName().equals(me.getCurrentMapName())) {
            return false;
        }
        MapCoordinate approach = coordinateHelper.calculateApproachCoordinate(
                objective.getMapName(), objective.getX(), objective.getY());
        int dx = Math.abs(me.getX() - approach.getX());
        int dy = Math.abs(me.getY() - approach.getY());
        boolean reached = dx <= TARGET_APPROACH_ARRIVAL_TOLERANCE && dy <= TARGET_APPROACH_ARRIVAL_TOLERANCE;
        if (reached) {
            log.info("[xiuluo-v2] target approach reached: current={} ({}, {}) objective={} approach=({}, {}) diff=({}, {})",
                    me.getCurrentMapName(), me.getX(), me.getY(), objective.getName(),
                    approach.getX(), approach.getY(), dx, dy);
        }
        return reached;
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
