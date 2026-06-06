package com.bot.dhxy.task.wubei;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.model.maintenance.TaskMaintenanceResult;
import com.bot.dhxy.model.maintenance.TaskMaintenanceStatus;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcClickRequest;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.MapNameCanonicalizer;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.service.NpcClickService;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.service.QuestManagerService;
import com.bot.dhxy.service.TaskMaintenanceService;
import com.bot.dhxy.service.TeamReturnService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.vision.OcrWindowScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

/**
 * First playable 五倍 task loop.
 *
 * <p>This intentionally reuses the common NavigationService, NpcClickService, DialogService,
 * AutoCombatService, and BagService boundaries instead of introducing a private 五倍 input stack.
 * Coordinates are logical game-map coordinates; template-click points are screen-absolute pixels
 * after CoordinateHelper resolves the active bound window.</p>
 */
@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class WubeiTask implements GameTask {

    private static final String TASK_CODE = "wubei";
    private static final String TASK_NAME = "五倍";
    private static final String START_MAP_NAME = "宝象国";
    private static final String ACCEPT_NPC_NAME = "降魔侍卫";
    private static final String HEAL_PET_NPC_NAME = "沙拉买提";
    private static final String REPAIR_EQUIPMENT_NPC_NAME = "李道宗";
    private static final String REPAIR_EQUIPMENT_MAP_NAME = "洛阳城";
    private static final int ACCEPT_NPC_X = 86;
    private static final int ACCEPT_NPC_Y = 87;
    private static final int HEAL_PET_NPC_X = 95;
    private static final int HEAL_PET_NPC_Y = 126;
    private static final int REPAIR_EQUIPMENT_NPC_X = 324;
    private static final int REPAIR_EQUIPMENT_NPC_Y = 109;
    private static final int ACCEPT_NPC_DIRECT_CLICK_DISTANCE = 12;
    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/wubei/wubei_accept_chumoweiguo.png";
    private static final String ENTER_BATTLE_TEMPLATE = "images/template/dialog/wubei/wubei_enter_battle_xiaomie.png";
    private static final String ENTER_BATTLE_PROVE_TEMPLATE = "images/template/dialog/wubei/wubei_enter_battle_zhengming.png";
    private static final String ENTER_BATTLE_KUIXING_TEMPLATE = "images/template/dialog/wubei/wubei_enter_battle_kuixing.png";
    private static final String PROBE_STORY_TEMPLATE = "images/template/dialog/wubei/wubei_probe_story_koukou.png";
    private static final String PROBE_WRONG_POSITION_TEMPLATE = "images/template/dialog/wubei/wubei_probe_story_wrong_position.png";
    private static final String HEAL_PET_OPTION_TEMPLATE = "images/template/dialog/maintenance/heal_pet_option.png";
    private static final String HEAL_PET_NPC_TOOLTIP_TEMPLATE = "images/template/npc/npc_wuyi_tooltip.png";
    private static final String REPAIR_EQUIPMENT_OPTION_TEMPLATE = "images/template/dialog/maintenance/repair_equipment_option.png";
    private static final String REPAIR_EQUIPMENT_TOOLTIP_TEMPLATE = "images/template/npc/npc_xiuli_tooltip.png";
    private static final String TRACKER_ANCHOR_TEMPLATE = "images/template/task/wubei_tracker_anchor.png";
    private static final String PROBE_ITEM_TEMPLATE = "bag/wubei_probe_item.png";
    private static final String RETURN_ITEM_TEMPLATE = "bag/wubei_return_item.png";
    private static final String OPTION_ACCEPT_TASK = "wubei.acceptTask";
    private static final String OPTION_ENTER_BATTLE = "wubei.enterBattle";
    private static final String OPTION_ENTER_BATTLE_PROVE = "wubei.enterBattle.prove";
    private static final String OPTION_ENTER_BATTLE_KUIXING = "wubei.enterBattle.kuixing";
    private static final String STORY_PROBE_TARGET_READY = "wubei.probeTargetReady";
    private static final String STORY_PROBE_WRONG_POSITION = "wubei.probeWrongPosition";
    private static final String BUSINESS_ACTION_HEAL_PET = "heal-pet";
    private static final String BUSINESS_ACTION_REPAIR_EQUIPMENT = "repair-equipment";
    private static final int MAX_MAINTENANCE_HOOK_ATTEMPTS = 5;
    private static final String TEAM_RETURN_WAIT_SOURCE_PREFIX = "team-return-wait";
    private static final String TEAM_RETURN_BEFORE_ACCEPT_SOURCE = TEAM_RETURN_WAIT_SOURCE_PREFIX + ":before-accept";
    private static final String TEAM_RETURN_ROUND_DONE_SOURCE = TEAM_RETURN_WAIT_SOURCE_PREFIX + ":round-done";
    private static final String DARK_THUNDER_KEYWORD = "暗雷怪";
    private static final String CHAINED_COMBAT_TARGET_KEYWORD = "黄袍";
    private static final String PROBE_TARGET_NPC_NAME = "白龙马";
    private static final int MAX_CHAINED_COMBAT_ATTEMPTS = 5;
    private static final int MAX_TRACKER_CLICK_ATTEMPTS = 12;
    private static final int MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS = 5;
    private static final int MAX_PROBE_ITEM_ATTEMPTS_PER_LINK = 2;
    private static final int TRACKER_LINK_SINGLE_MAX_WIDTH = 72;
    private static final int TRACKER_LINK_SPLIT_GAP = 8;
    private static final int TRACKER_LINK_MIN_PIXELS = 20;
    private static final int TRACKER_LINK_DELIMITER_MAX_WIDTH = 5;
    private static final int TRACKER_LINK_DELIMITER_MAX_PIXELS = 18;
    private static final int TRACKER_ANCHOR_SEARCH_REL_LEFT = 6;
    private static final int TRACKER_ANCHOR_SEARCH_REL_TOP = 196;
    private static final int TRACKER_ANCHOR_SEARCH_REL_RIGHT = 207;
    private static final int TRACKER_ANCHOR_SEARCH_REL_BOTTOM = 551;
    private static final int TRACKER_PANEL_FROM_ANCHOR_LEFT = -96;
    private static final int TRACKER_PANEL_FROM_ANCHOR_TOP = 12;
    private static final int TRACKER_PANEL_FROM_ANCHOR_RIGHT = 86;
    private static final int TRACKER_PANEL_FROM_ANCHOR_BOTTOM = 73;
    private static final long TRACKER_REFRESH_AFTER_ACCEPT_MS = 450L;
    private static final int TRACKER_DEST_HINT_SAMPLES = 3;
    private static final int TRACKER_DEST_HINT_SAMPLE_DELAY_MS = 180;
    private static final long TRACKER_DEST_HINT_MAX_CAPTURE_MS = 3_000L;
    private static final OcrWindowRegion TRACKER_DEST_HINT_REGION =
            new OcrWindowRegion(250, 345, 779, 488);
    private static final int TRACKER_DEST_HINT_ARRIVAL_TOLERANCE = 12;
    private static final int RETURN_ITEM_VERIFY_ATTEMPTS = 2;
    private static final long RETURN_VERIFY_DELAY_MS = 500L;
    private static final long TASK_TURN_HANDOFF_DELAY_MS = 900L;
    private static final long MAINTENANCE_BROADCAST_HANDOFF_PER_WINDOW_MS = 2_000L;
    private static final long CHAINED_POST_BATTLE_RECOVERY_PER_MEMBER_MS = 2_200L;
    private static final long CHAINED_POST_BATTLE_RECOVERY_MAX_MS = 10_000L;
    private static final long WAIT_BATTLE_TIMEOUT_MS = 180_000L;
    private static final long PAUSE_TIMER_COMPENSATION_THRESHOLD_MS = 1_000L;
    private static final Pattern TRACKER_DEST_HINT_PATTERN =
            Pattern.compile("前往(.+?)[(（]\\s*(\\d+)\\s*[,，]\\s*(\\d+)\\s*[)）]");
    private static final Pattern TRACKER_COMBAT_TARGET_PATTERN =
            Pattern.compile("([^|丨:：\\s]+)$");

    private static final NpcTarget ACCEPT_NPC = NpcTarget.builder()
            .key("wubei.acceptNpc")
            .mapName(START_MAP_NAME)
            .name(ACCEPT_NPC_NAME)
            .x(ACCEPT_NPC_X)
            .y(ACCEPT_NPC_Y)
            .role(NpcRole.QUEST_GIVER)
            .movementType(NpcMovementType.FIXED)
            .expectedDialogTemplatePath(ACCEPT_OPTION_TEMPLATE)
            .source("wubei")
            .build();

    private static final NpcTarget HEAL_PET_NPC = NpcTarget.builder()
            .key("wubei.healPetNpc")
            .mapName(START_MAP_NAME)
            .name(HEAL_PET_NPC_NAME)
            .x(HEAL_PET_NPC_X)
            .y(HEAL_PET_NPC_Y)
            .role(NpcRole.INTERACTION_TARGET)
            .movementType(NpcMovementType.FIXED)
            .tooltipTemplatePath(HEAL_PET_NPC_TOOLTIP_TEMPLATE)
            .expectedDialogTemplatePath(HEAL_PET_OPTION_TEMPLATE)
            .source("wubei")
            .build();

    private static final NpcTarget REPAIR_EQUIPMENT_NPC = NpcTarget.builder()
            .key("wubei.repairEquipmentNpc")
            .mapName(REPAIR_EQUIPMENT_MAP_NAME)
            .name(REPAIR_EQUIPMENT_NPC_NAME)
            .x(REPAIR_EQUIPMENT_NPC_X)
            .y(REPAIR_EQUIPMENT_NPC_Y)
            .role(NpcRole.INTERACTION_TARGET)
            .movementType(NpcMovementType.FIXED)
            .tooltipTemplatePath(REPAIR_EQUIPMENT_TOOLTIP_TEMPLATE)
            .expectedDialogTemplatePath(REPAIR_EQUIPMENT_OPTION_TEMPLATE)
            .source("wubei")
            .build();

    private final BotProperties botProperties;
    private final GameContext gameContext;
    private final NavigationService navigationService;
    private final NpcClickService npcClickService;
    private final DialogService dialogService;
    private final AutoCombatService autoCombatService;
    private final BagService bagService;
    private final QuestManagerService questManagerService;
    private final PlayerStateService playerStateService;
    private final TaskMaintenanceService taskMaintenanceService;
    private final TeamReturnService teamReturnService;
    private final UICleanerService uiCleanerService;
    private final CoordinateHelper coordinateHelper;
    private final GameStateUtil gameStateUtil;
    private final GameClientTracker tracker;
    private final WindowScopedTempPath windowScopedTempPath;
    private final TextRecognizer textRecognizer;
    private final InputSequences inputSequences;
    private final TaskTransactionRunner taskTransactionRunner;
    private final TaskTurnCoordinator taskTurnCoordinator;
    private final MultiWindowTaskManager multiWindowTaskManager;
    private final MapNameCanonicalizer mapNameCanonicalizer;
    private boolean currentRoundChainedCombatExpected;
    private int currentRoundChainedCombatContinueCount;
    private TrackerPanelSnapshot currentTrackerSnapshot;
    private TrackerDestinationHint currentTrackerDestinationHint;
    private List<TrackerGreenLinkSegment> currentProbeSegments = List.of();
    private boolean[] currentProbeUsed = new boolean[0];
    private int[] currentProbeItemAttempts = new int[0];
    private int currentProbeIndex = -1;
    private long waitBattleStartedAt;
    private long waitBattleNextTrackerRetryAt;
    private boolean waitBattleSawCombat;
    private long lastHealPetMaintenanceAt;
    private long lastRepairEquipmentMaintenanceAt;
    private int lastLeaderPathingSummonAttemptRound;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void stop() {
        log.info("[wubei] stop requested");
        gameContext.setBotStatus(GameContext.BotStatus.IDLE);
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    /**
     * Run 五倍 for the configured round count.
     *
     * @param executionContext nullable task context from the window runner. When null, a minimal
     *                         context is created so local debug execution still has stop checks.
     * @return SUCCESS after the configured count, STOPPED on cooperative stop, otherwise FAILED.
     */
    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        TaskExecutionContext context = resolveExecutionContext(executionContext);
        int maxRuns = botProperties.getFivefoldMaxRuns();
        int completedRuns = 0;
        gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        autoCombatService.initializeForCurrentWindow();
        long maintenanceStartAt = botProperties.isXiuluoMaintenanceRunImmediatelyOnStart()
                ? 0L
                : System.currentTimeMillis();
        lastHealPetMaintenanceAt = maintenanceStartAt;
        lastRepairEquipmentMaintenanceAt = maintenanceStartAt;
        taskMaintenanceService.initializeForTaskStart(context, TASK_CODE);
        log.info("[wubei] task started: maxRuns={}", maxRuns <= 0 ? "unlimited" : maxRuns);

        try {
            while (maxRuns <= 0 || completedRuns < maxRuns) {
                TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
                int round = completedRuns + 1;
                resetRoundState(round);
                WubeiRoundContext roundContext = completedRuns == 0
                        ? WubeiRoundContext.hotStart(round)
                        : WubeiRoundContext.normalStart(round);
                taskMaintenanceService.beginTeamMaintenanceRound(context, TASK_CODE, round,
                        "wubei:round-start");
                TaskRunResult roundResult = runRoundPhases(context, roundContext);
                if (roundResult == TaskRunResult.STOPPED) {
                    gameContext.setBotStatus(GameContext.BotStatus.IDLE);
                    return TaskRunResult.STOPPED;
                }
                if (roundResult != TaskRunResult.SUCCESS) {
                    gameContext.setBotStatus(GameContext.BotStatus.ERROR);
                    return TaskRunResult.FAILED;
                }

                completedRuns++;
                log.info("[wubei] round {} finished: completed={}/{}", round, completedRuns,
                        maxRuns <= 0 ? "unlimited" : maxRuns);
            }
            gameContext.setBotStatus(GameContext.BotStatus.IDLE);
            return TaskRunResult.SUCCESS;
        } catch (TaskStopRequestedException e) {
            log.info("[wubei] task stopped: {}", e.getMessage());
            gameContext.setBotStatus(GameContext.BotStatus.IDLE);
            return TaskRunResult.STOPPED;
        } catch (Exception e) {
            log.error("[wubei] task failed", e);
            gameContext.setBotStatus(GameContext.BotStatus.ERROR);
            return TaskRunResult.FAILED;
        } finally {
            taskTransactionRunner.forceReleaseTurn("wubei:execute-finished");
        }
    }

    private void resetRoundState(int round) {
        currentRoundChainedCombatExpected = false;
        currentTrackerSnapshot = null;
        currentTrackerDestinationHint = null;
        resetProbeRuntime();
        lastLeaderPathingSummonAttemptRound = -1;
        log.info("[wubei] round {} started", round);
    }

    /*
     * 五倍目前先采用修罗 V2 的轻量 phase runner：每个 phase 只执行一个业务动作，
     * PATHING/COMBAT/POST_BATTLE 等共享状态会释放 task turn，让其他窗口有机会补血或响应。
     */
    private TaskRunResult runRoundPhases(TaskExecutionContext context, WubeiRoundContext initialState) {
        WubeiRoundContext roundState = initialState;
        int phaseLoopGuard = 0;
        while (!roundState.phase().isTerminal()) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            WubeiRoundContext currentState = roundState;

            AtomicReference<WubeiStepOutcome> phaseOutcome = new AtomicReference<>();
            TaskTransactionOutcome transaction = taskTransactionRunner.run(
                    "wubei:" + currentState.phase(),
                    TaskTransactionResult.READY_TO_CONTINUE,
                    TaskYieldPolicy.CONTINUE_CHAIN,
                    () -> {
                        WubeiStepOutcome outcome = runPhase(context, currentState);
                        phaseOutcome.set(outcome);
                        return outcome.transactionResult();
                    });

            WubeiStepOutcome outcome = phaseOutcome.get();
            if (outcome == null) {
                outcome = WubeiStepOutcome.failed(currentState, "phase produced no outcome");
            }
            log.info("[wubei] phase outcome: phase={} result={} yield={} next={} message={}",
                    currentState.phase(), outcome.transactionResult(), outcome.yieldPolicy(),
                    outcome.nextState().phase(), outcome.message());

            if (transaction.result() == TaskTransactionResult.STOPPED
                    || outcome.transactionResult() == TaskTransactionResult.STOPPED) {
                return TaskRunResult.STOPPED;
            }
            if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
                roundState = recoverRoundAfterFailure(context, currentState, outcome);
                phaseLoopGuard = 0;
                continue;
            }
            if (outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED
                    || outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
                /*
                 * Pathing and combat waits are external game states. They may yield the same phase
                 * many times while other windows refresh auto-combat or recover, so they must not
                 * trip the generic loop guard.
                 */
                phaseLoopGuard = 0;
                yieldAfterMustYield(context, outcome);
                roundState = outcome.nextState();
                continue;
            }
            if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD
                    || outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED
                    || outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
                yieldAfterMustYield(context, outcome);
            }
            if (++phaseLoopGuard > 32) {
                log.error("[wubei] phase loop guard exceeded: state={}", roundState);
                roundState = recoverRoundAfterFailure(context, roundState,
                        WubeiStepOutcome.failed(roundState, "phase loop guard exceeded"));
                phaseLoopGuard = 0;
                continue;
            }
            roundState = outcome.nextState();
        }

        if (roundState.phase() == WubeiPhase.ROUND_DONE) {
            return TaskRunResult.SUCCESS;
        }
        return roundState.phase() == WubeiPhase.STOPPED ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
    }

    private WubeiRoundContext recoverRoundAfterFailure(TaskExecutionContext context,
                                                       WubeiRoundContext failedState,
                                                       WubeiStepOutcome outcome) {
        log.warn("[wubei] phase failed; recover current round from accept task: phase={} message={} recoveryCount={}",
                failedState.phase(), outcome.message(), failedState.recoveryCount());
        if (failedState.recoveryCount() >= 3) {
            log.error("[wubei] recovery limit reached: phase={} message={}", failedState.phase(), outcome.message());
            return failedState.next(WubeiPhase.FAILED, "recovery-limit");
        }
        taskTransactionRunner.forceReleaseTurn("wubei-recover:" + failedState.phase());
        uiCleanerService.cleanUpAll();
        TaskSleep.sleepOrStop(context, 800L, "Wubei task interrupted");
        return failedState.recoverTo(WubeiPhase.ACCEPT_TASK, "recover-from-" + failedState.phase());
    }

    private void yieldAfterMustYield(TaskExecutionContext context, WubeiStepOutcome outcome) {
        long delayMs = handoffDelayMs(outcome);
        log.info("[wubei] task turn handoff delay: result={} next={} source={} delayMs={}",
                outcome.transactionResult(), outcome.nextState().phase(), outcome.nextState().source(), delayMs);
        TaskSleep.sleepOrStop(context, delayMs, "Wubei task interrupted");
        maybeRunLeaderPathingSummonMaintenance(context, outcome);
    }

    private long handoffDelayMs(WubeiStepOutcome outcome) {
        WubeiRoundContext nextState = outcome.nextState();
        if (nextState != null
                && nextState.source() != null
                && nextState.source().endsWith("-broadcast-handled")) {
            int windowCount = Math.max(1, multiWindowTaskManager.getRegisteredWindowCount());
            long delayMs = windowCount * MAINTENANCE_BROADCAST_HANDOFF_PER_WINDOW_MS;
            log.info("[wubei] maintenance broadcast handoff delay: source={} windowCount={} perWindowMs={} delayMs={}",
                    nextState.source(), windowCount, MAINTENANCE_BROADCAST_HANDOFF_PER_WINDOW_MS, delayMs);
            return delayMs;
        }
        if (nextState != null
                && "post-battle-chained-recovered".equals(nextState.source())) {
            int windowCount = Math.max(1, multiWindowTaskManager.getRegisteredWindowCount());
            long memberSlots = Math.max(0, windowCount - 1L);
            long delayMs = Math.min(CHAINED_POST_BATTLE_RECOVERY_MAX_MS,
                    memberSlots * CHAINED_POST_BATTLE_RECOVERY_PER_MEMBER_MS);
            log.info("[wubei] chained post-battle team recovery delay: windowCount={} memberSlots={} perMemberMs={} delayMs={}",
                    windowCount, memberSlots, CHAINED_POST_BATTLE_RECOVERY_PER_MEMBER_MS, delayMs);
            return delayMs;
        }
        if (nextState != null
                && nextState.source() != null
                && nextState.source().startsWith(TEAM_RETURN_WAIT_SOURCE_PREFIX)) {
            long configured = botProperties.getReturnTeamLeaderWaitPollMs();
            return configured > 0 ? configured : 3_000L;
        }
        return TASK_TURN_HANDOFF_DELAY_MS;
    }

    private void maybeRunLeaderPathingSummonMaintenance(TaskExecutionContext context, WubeiStepOutcome outcome) {
        if (outcome.transactionResult() != TaskTransactionResult.PATHING_STARTED
                || outcome.nextState() == null
                || outcome.nextState().phase() != WubeiPhase.RESOLVE_AFTER_PATHING) {
            return;
        }
        WubeiRoundContext nextState = outcome.nextState();
        if (lastLeaderPathingSummonAttemptRound == nextState.round()) {
            log.debug("[wubei] leader pathing summon maintenance skipped: already attempted this round round={} source={}",
                    nextState.round(), nextState.source());
            return;
        }
        if (!isLeaderPathingSummonActionStateSafe()) {
            log.debug("[wubei] leader pathing summon maintenance skipped: actionState={} round={} source={}",
                    gameContext.getCurrentActionState(), nextState.round(), nextState.source());
            return;
        }
        if (hasDialogBeforeLeaderPathingSummon(nextState)) {
            return;
        }

        boolean ran = taskTurnCoordinator.tryRun("wubei:leaderPathingSummonMaintenance", () -> {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            TaskMaintenanceResult result = taskMaintenanceService.runOpportunisticMaintenance(context,
                    TaskMaintenanceRequest.builder()
                            .sourceTask("wubei:leader-pathing")
                            .handleMaintenanceBroadcast(false)
                            .cleanSummonSkill(true)
                            .requireFreeStateForSummonSkill(false)
                            .oneSummonSkillPerTeamRound(true)
                            .maxSummonSkillCleanersPerTeamRound(1)
                            .teamMaintenanceKey(TASK_CODE)
                            .teamRound(nextState.round())
                            .build());
            if (result.isSummonSkillAttempted()) {
                lastLeaderPathingSummonAttemptRound = nextState.round();
                log.info("[wubei] leader pathing summon maintenance barrier finished: round={} status={} message={} resumePhase={}",
                        nextState.round(), result.getStatus(), result.getMessage(), nextState.phase());
            }
            if (result.isSummonSkillSucceeded()) {
                log.info("[wubei] leader pathing summon maintenance completed: round={} status={} message={}",
                        nextState.round(), result.getStatus(), result.getMessage());
            } else if (result.getStatus() != TaskMaintenanceStatus.SUMMON_SKILL_NOT_DUE
                    && result.getStatus() != TaskMaintenanceStatus.SUMMON_SKILL_ROUND_ALREADY_CLAIMED) {
                log.info("[wubei] leader pathing summon maintenance skipped/deferred: round={} status={} message={}",
                        nextState.round(), result.getStatus(), result.getMessage());
            } else {
                log.debug("[wubei] leader pathing summon maintenance skipped: round={} status={} message={}",
                        nextState.round(), result.getStatus(), result.getMessage());
            }
            return true;
        });
        if (!ran) {
            log.debug("[wubei] leader pathing summon maintenance skipped: task turn busy round={}",
                    nextState.round());
        }
    }

    private boolean isLeaderPathingSummonActionStateSafe() {
        GameContext.ActionState state = gameContext.getCurrentActionState();
        return state == GameContext.ActionState.FREE || state == GameContext.ActionState.NAVIGATING;
    }

    private boolean hasDialogBeforeLeaderPathingSummon(WubeiRoundContext nextState) {
        DialogResult dialogResult = dialogService.handleDialog(DialogHandleRequest.inspect(
                "wubei:leader-pathing-summon-preflight"));
        DialogResultStatus status = dialogResult.getStatus();
        if (status == DialogResultStatus.NO_DIALOG) {
            return false;
        }
        log.info("[wubei] leader pathing summon maintenance skipped: dialog pending round={} phase={} source={} status={} kind={}",
                nextState.round(), nextState.phase(), nextState.source(), status, dialogResult.getKind());
        return true;
    }

    private WubeiStepOutcome runPhase(TaskExecutionContext context, WubeiRoundContext state) {
        return switch (state.phase()) {
            case HOT_START_DETECT -> runHotStartDetectPhase(state);
            case ACCEPT_TASK -> runAcceptTaskPhase(context, state);
            case WAIT_TEAM_READY -> runWaitTeamReadyPhase(context, state);
            case READ_TRACKER -> runReadTrackerPhase(context, state);
            case TRACKER_PATHING -> runTrackerPathingPhase(context, state);
            case RESOLVE_AFTER_PATHING -> runResolveAfterPathingPhase(context, state);
            case ENTER_BATTLE -> WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.WAIT_BATTLE_FINISH, "enter-battle-delegated"),
                    "enter battle dialog is handled by battle wait loop");
            case WAIT_BATTLE_FINISH -> runWaitBattleFinishPhase(context, state);
            case POST_BATTLE_RECOVER -> runPostBattleRecoverPhase(context, state);
            case RETURN_HOME -> runReturnHomePhase(context, state);
            case WAIT_TEAM_RETURN -> runWaitTeamReturnPhase(context, state);
            case ROUND_DONE, FAILED, STOPPED -> WubeiStepOutcome.continueTo(state, "terminal");
        };
    }

    private WubeiStepOutcome runHotStartDetectPhase(WubeiRoundContext state) {
        if (shouldYieldForTeamReturnSignal()) {
            log.warn("[wubei] team return signal present before accept flow; yield for members");
            return WubeiStepOutcome.sharedState(
                    state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_BEFORE_ACCEPT_SOURCE),
                    "team return pending before accept flow");
        }
        if (hasAcceptedTaskFromPanel()) {
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.READ_TRACKER, "hot-start-active-task"),
                    "accepted task found from task panel");
        }
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.ACCEPT_TASK, "hot-start-no-active-task"),
                "no accepted task found");
    }

    private WubeiStepOutcome runAcceptTaskPhase(TaskExecutionContext context, WubeiRoundContext state) {
        if (!acceptTask(context)) {
            return WubeiStepOutcome.failed(state, "accept task failed");
        }
        /*
         * 接任务点击返回不代表左侧任务追踪已经同步刷新。之前日志里 70ms 内就开始裁任务面板，
         * 容易把旧面板或过渡状态当成当前任务来解析。
         */
        TaskSleep.sleepOrStop(context, TRACKER_REFRESH_AFTER_ACCEPT_MS, "Wubei task interrupted");
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.WAIT_TEAM_READY, "task-accepted"),
                "task accepted");
    }

    private WubeiStepOutcome runWaitTeamReadyPhase(TaskExecutionContext context, WubeiRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        WubeiStepOutcome healPetOutcome = triggerHealPetBroadcastBeforeTracker(context, state);
        if (healPetOutcome != null) {
            return healPetOutcome;
        }
        WubeiStepOutcome repairOutcome = triggerRepairEquipmentBroadcastBeforeTracker(context, state);
        if (repairOutcome != null) {
            return repairOutcome;
        }
        log.info("[wubei] wait team ready passed: round={} source={}", state.round(), state.source());
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.READ_TRACKER, "team-ready"),
                "team ready passed");
    }

    private WubeiStepOutcome triggerHealPetBroadcastBeforeTracker(TaskExecutionContext context, WubeiRoundContext state) {
        if (!isHealPetMaintenanceDue()) {
            log.info("[wubei] skip heal-pet hook: cooldown not due intervalMs={} lastAt={}",
                    botProperties.getXiuluoHealPetMaintenanceIntervalMs(), lastHealPetMaintenanceAt);
            return null;
        }

        WubeiStepOutcome outcome = triggerMaintenanceBroadcastBeforeTracker(
                context, state, HEAL_PET_NPC, BUSINESS_ACTION_HEAL_PET,
                "heal-pet", "wubei:heal-pet-npc");
        if (outcome != null && outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
            lastHealPetMaintenanceAt = System.currentTimeMillis();
        }
        return outcome;
    }

    private WubeiStepOutcome triggerRepairEquipmentBroadcastBeforeTracker(TaskExecutionContext context,
                                                                          WubeiRoundContext state) {
        if (!isRepairEquipmentMaintenanceDue()) {
            log.info("[wubei] skip repair-equipment hook: cooldown not due intervalMs={} lastAt={}",
                    botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs(), lastRepairEquipmentMaintenanceAt);
            return null;
        }

        WubeiStepOutcome outcome = triggerMaintenanceBroadcastBeforeTracker(
                context, state, REPAIR_EQUIPMENT_NPC, BUSINESS_ACTION_REPAIR_EQUIPMENT,
                "repair-equipment", "wubei:repair-equipment-npc");
        if (outcome != null && outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
            lastRepairEquipmentMaintenanceAt = System.currentTimeMillis();
        }
        return outcome;
    }

    private WubeiStepOutcome triggerMaintenanceBroadcastBeforeTracker(TaskExecutionContext context,
                                                                      WubeiRoundContext state,
                                                                      NpcTarget npc,
                                                                      String expectedAction,
                                                                      String hookName,
                                                                      String navigationSource) {
        for (int attempt = 1; attempt <= MAX_MAINTENANCE_HOOK_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            log.info("[wubei] {} hook attempt: attempt={}/{} npc={} map={} coord=({}, {})",
                    hookName, attempt, MAX_MAINTENANCE_HOOK_ATTEMPTS, npc.getName(),
                    npc.getMapName(), npc.getX(), npc.getY());
            NavigationResult nav = navigationService.navigateToNPC(NavigationRequest.builder()
                    .targetMapName(npc.getMapName())
                    .targetX(npc.getX())
                    .targetY(npc.getY())
                    .targetName(npc.getName())
                    .returnOnPathingStarted(false)
                    .source(navigationSource)
                    .build());
            if (!nav.success()) {
                log.warn("[wubei] {} navigation failed: attempt={} status={} message={}",
                        hookName, attempt, nav.getStatus(), nav.getMessage());
                continue;
            }
            if (!npcClickService.clickNpcSmart(npc.toClickRequest(gameContext.getMe()))) {
                log.warn("[wubei] {} NPC smart-click failed: attempt={}", hookName, attempt);
                continue;
            }

            TaskMaintenanceResult maintenanceResult = taskMaintenanceService.runOpportunisticMaintenance(context,
                    TaskMaintenanceRequest.builder()
                            .sourceTask("wubei:" + hookName + "-broadcast")
                            .handleMaintenanceBroadcast(true)
                            .cleanSummonSkill(false)
                            .build());
            if (maintenanceResult.getStatus() == TaskMaintenanceStatus.INTERRUPTED) {
                return WubeiStepOutcome.stopped(state, hookName + " maintenance interrupted");
            }
            if (maintenanceResult.isBroadcastHandled()) {
                log.info("[wubei] {} broadcast handled: expectedAction={} status={}",
                        hookName, expectedAction, maintenanceResult.getStatus());
                return WubeiStepOutcome.sharedState(
                        state.next(WubeiPhase.READ_TRACKER, hookName + "-broadcast-handled"),
                        hookName + " broadcast handled; yield for team");
            }
            log.warn("[wubei] {} broadcast not handled: attempt={} expectedAction={} status={} message={}",
                    hookName, attempt, expectedAction,
                    maintenanceResult.getStatus(), maintenanceResult.getMessage());
        }

        log.warn("[wubei] {} hook skipped after {} attempts; continue main task",
                hookName, MAX_MAINTENANCE_HOOK_ATTEMPTS);
        return null;
    }

    private boolean isHealPetMaintenanceDue() {
        long intervalMs = botProperties.getXiuluoHealPetMaintenanceIntervalMs();
        if (intervalMs <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        return lastHealPetMaintenanceAt <= 0 || now - lastHealPetMaintenanceAt >= intervalMs;
    }

    private boolean isRepairEquipmentMaintenanceDue() {
        long intervalMs = botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs();
        if (intervalMs <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        return lastRepairEquipmentMaintenanceAt <= 0 || now - lastRepairEquipmentMaintenanceAt >= intervalMs;
    }

    private WubeiStepOutcome runReadTrackerPhase(TaskExecutionContext context, WubeiRoundContext state) {
        resetProbeRuntime();
        currentTrackerSnapshot = resolveTrackerPanelSnapshotWithAnchorRecovery(context);
        if (!currentTrackerSnapshot.anchorFound()) {
            return WubeiStepOutcome.failed(state, "tracker anchor not found");
        }
        if (containsDarkThunder(currentTrackerSnapshot.yellowText())) {
            log.info("[wubei] dark-thunder task detected; reroll by accepting task again: yellow='{}'",
                    currentTrackerSnapshot.yellowText());
            TaskSleep.sleepOrStop(context, 4_000L, "Wubei task interrupted");
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.ACCEPT_TASK, "dark-thunder-reroll"),
                    "dark thunder reroll");
        }
        currentRoundChainedCombatExpected = containsChainedCombatTarget(currentTrackerSnapshot.yellowText());
        currentRoundChainedCombatContinueCount = 0;
        log.info("[wubei] tracker snapshot ready: yellow='{}' probe={} chainedCombatExpected={}",
                currentTrackerSnapshot.yellowText(),
                currentTrackerSnapshot.greenScan().isProbeObjective(),
                currentRoundChainedCombatExpected);
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.TRACKER_PATHING, "tracker-ready"),
                "tracker objective ready");
    }

    private WubeiStepOutcome runTrackerPathingPhase(TaskExecutionContext context, WubeiRoundContext state) {
        if (currentTrackerSnapshot == null) {
            return WubeiStepOutcome.failed(state, "tracker pathing failed");
        }
        TrackerGreenLinkScan scan = currentTrackerSnapshot.greenScan();
        if (containsProbeTask(currentTrackerSnapshot.yellowText()) || scan.isProbeObjective()) {
            if (!startProbeTrackerPathing(context, scan)) {
                return WubeiStepOutcome.failed(state, "probe tracker pathing failed");
            }
            return WubeiStepOutcome.pathingStarted(
                    state.next(WubeiPhase.RESOLVE_AFTER_PATHING, "probe-pathing-started"),
                    "probe tracker pathing started");
        }
        if (!triggerCombatTrackerPathing(context, scan)) {
            return WubeiStepOutcome.failed(state, "tracker pathing failed");
        }
        return WubeiStepOutcome.pathingStarted(
                state.next(WubeiPhase.RESOLVE_AFTER_PATHING, "tracker-pathing-started"),
                "tracker pathing started");
    }

    private WubeiStepOutcome runResolveAfterPathingPhase(TaskExecutionContext context, WubeiRoundContext state) {
        if (isProbeRuntimeActive()) {
            return resolveProbeAfterPathing(context, state);
        }
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.WAIT_BATTLE_FINISH, "pathing-resolve-delegated"),
                "pathing resolve handled by battle wait loop");
    }

    private WubeiStepOutcome runWaitBattleFinishPhase(TaskExecutionContext context, WubeiRoundContext state) {
        WubeiStepOutcome outcome = tickWaitBattleFinish(context, state);
        if (outcome.transactionResult() == TaskTransactionResult.FAILED
                || outcome.transactionResult() == TaskTransactionResult.STOPPED) {
            resetWaitBattleRuntime();
            return outcome;
        }
        if (outcome.nextState().phase() != WubeiPhase.WAIT_BATTLE_FINISH) {
            resetWaitBattleRuntime();
        }
        return outcome;
    }

    private WubeiStepOutcome runPostBattleRecoverPhase(TaskExecutionContext context, WubeiRoundContext state) {
        /*
         * 普通五倍战斗结束后，队长要连续完成回城和归队检查，不能让成员补给插到下一轮前。
         * 黄袍怪是例外：它会在原地连续战斗，战后要释放 task turn，并按窗口数给队员补给窗口再续打。
         */
        TaskSleep.sleepOrStop(context, 800L, "Wubei task interrupted");
        if (currentRoundChainedCombatExpected) {
            return WubeiStepOutcome.sharedState(
                    state.next(WubeiPhase.RETURN_HOME, "post-battle-chained-recovered"),
                    "chained combat post battle recovery window");
        }
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.RETURN_HOME, "post-battle-recovered"),
                "post battle recovery window");
    }

    private WubeiStepOutcome runReturnHomePhase(TaskExecutionContext context, WubeiRoundContext state) {
        return returnHomeAfterCombatOrContinueSpecialTarget(context, state);
    }

    private WubeiStepOutcome runWaitTeamReturnPhase(TaskExecutionContext context, WubeiRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        /*
         * After the leader returns to 宝象国, dead members may need a turn to click 归队. Keep this
         * as a shared-state wait instead of blocking the input turn, otherwise follower windows
         * cannot acquire the turn needed to click their return button.
         */
        if (shouldYieldForTeamReturnSignal()) {
            log.warn("[wubei] team return signal still present; yield for members source={}", state.source());
            return WubeiStepOutcome.sharedState(
                    state.next(WubeiPhase.WAIT_TEAM_RETURN, keepTeamReturnWaitSource(state)),
                    "team return still pending");
        }
        if (TEAM_RETURN_BEFORE_ACCEPT_SOURCE.equals(state.source())) {
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.ACCEPT_TASK, "team-return-ready-before-accept"),
                    "team return ready before accept flow");
        }
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.ROUND_DONE, "team-return-not-needed"),
                "team return wait not needed");
    }

    private boolean shouldYieldForTeamReturnSignal() {
        return teamReturnService.isReturnTeamSignalPresent();
    }

    private String keepTeamReturnWaitSource(WubeiRoundContext state) {
        if (TEAM_RETURN_BEFORE_ACCEPT_SOURCE.equals(state.source())) {
            return TEAM_RETURN_BEFORE_ACCEPT_SOURCE;
        }
        return TEAM_RETURN_ROUND_DONE_SOURCE;
    }

    /*
     * 热启动入口：只在五倍任务刚启动时检查一次。五倍有任务时左侧任务栏会出现
     * wubei_active；匹配到就跳过接任务，不读取右侧详情。正常跑完一轮回城后必须
     * 直接重新接任务，不能每轮都打开任务栏查 active。
     */
    private boolean hasAcceptedTaskFromPanel() {
        boolean active = questManagerService.activateTaskIfPresentExclusive(TASK_CODE, false);
        log.info("[wubei] quest panel active-task check: active={}", active);
        return active;
    }

    private boolean acceptTask(TaskExecutionContext context) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        /*
         * This nearby fast path can submit a real NPC click. Refresh location first so a stale map
         * cache from the previous round cannot make us click 宝象国 coordinates on another map.
         */
        LocationInfo current = playerStateService.syncMyPosition();
        if (current != null && gameStateUtil.isNearCoordinate(current.mapName, current.x, current.y,
                ACCEPT_NPC.getMapName(), ACCEPT_NPC.getX(), ACCEPT_NPC.getY(), ACCEPT_NPC_DIRECT_CLICK_DISTANCE)) {
            log.info("[wubei] accept NPC nearby; try direct smart click before minimap navigation: playerMap={} player=({}, {}) targetMap={} target=({}, {}) tolerance={}",
                    current.mapName, current.x, current.y,
                    ACCEPT_NPC.getMapName(), ACCEPT_NPC.getX(), ACCEPT_NPC.getY(), ACCEPT_NPC_DIRECT_CLICK_DISTANCE);
            if (clickAcceptNpcAndOption("wubei:accept-task-nearby")) {
                return true;
            }
            log.info("[wubei] nearby accept click did not finish task acceptance; fallback to minimap navigation");
        }

        NavigationResult nav = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(START_MAP_NAME)
                .targetX(ACCEPT_NPC_X)
                .targetY(ACCEPT_NPC_Y)
                .targetName(ACCEPT_NPC_NAME)
                .source("wubei:accept-npc")
                .returnOnPathingStarted(false)
                .build());
        if (!nav.success()) {
            log.warn("[wubei] accept NPC navigation failed: status={} message={}",
                    nav.getStatus(), nav.getMessage());
            return false;
        }

        return clickAcceptNpcAndOption("wubei:accept-task");
    }

    private boolean clickAcceptNpcAndOption(String source) {
        boolean clicked = npcClickService.clickNpcSmart(ACCEPT_NPC.toClickRequest(gameContext.getMe()));
        if (!clicked) {
            log.warn("[wubei] accept NPC smart-click failed; try visible accept dialog before failing");
        }
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
                source,
                // 五倍接任务第一行绿字偏短，只随机到模板右侧中段，避免点到选项外侧。
                List.of(new GreenTemplateClickSpec(OPTION_ACCEPT_TASK, ACCEPT_OPTION_TEMPLATE, 32, 78, 3)),
                true));
        boolean accepted = OPTION_ACCEPT_TASK.equals(result.getActionKey())
                && result.getStatus() == DialogResultStatus.GREEN_TEMPLATE_CLICKED;
        log.info("[wubei] accept option result: status={} action={} clicked={}",
                result.getStatus(), result.getActionKey(), result.isClicked());
        return accepted;
    }

    private boolean containsDarkThunder(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return normalized.contains(DARK_THUNDER_KEYWORD);
    }

    private boolean containsChainedCombatTarget(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return normalized.contains(CHAINED_COMBAT_TARGET_KEYWORD);
    }

    private String resolveTrackerCombatTargetName() {
        if (currentRoundChainedCombatExpected) {
            return CHAINED_COMBAT_TARGET_KEYWORD;
        }
        if (currentTrackerSnapshot == null || currentTrackerSnapshot.yellowText() == null) {
            return "";
        }
        String normalized = currentTrackerSnapshot.yellowText().replace('丨', '|').replaceAll("\\s+", "");
        Matcher matcher = TRACKER_COMBAT_TARGET_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return "";
        }
        String candidate = matcher.group(1);
        if (candidate.contains(DARK_THUNDER_KEYWORD) || ACCEPT_NPC_NAME.equals(candidate)) {
            return "";
        }
        return candidate;
    }

    private TrackerPanelSnapshot resolveTrackerPanelSnapshotWithAnchorRecovery(TaskExecutionContext context) {
        for (int attempt = 1; attempt <= MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");

            TrackerPanelSnapshot snapshot = captureTrackerPanelSnapshot("attempt-" + attempt, false);
            if (snapshot.anchorFound()) {
                return snapshot;
            }

            log.warn("[wubei] tracker anchor narrow search missed: attempt={}/{}",
                    attempt, MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS);
            if (!hasAcceptedTaskFromPanel()) {
                log.warn("[wubei] no active task after tracker anchor miss; reaccept task: attempt={}/{}",
                        attempt, MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS);
                if (!acceptTask(context)) {
                    return TrackerPanelSnapshot.empty();
                }
                continue;
            }

            TrackerPanelSnapshot expandedSnapshot = captureTrackerPanelSnapshot("attempt-" + attempt + "-expanded", true);
            if (expandedSnapshot.anchorFound()) {
                return expandedSnapshot;
            }

            log.warn("[wubei] active task exists but tracker anchor still missing in expanded area; reaccept task: attempt={}/{}",
                    attempt, MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS);
            if (!acceptTask(context)) {
                return TrackerPanelSnapshot.empty();
            }
        }
        log.warn("[wubei] tracker anchor recovery exhausted: attempts={}", MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS);
        return TrackerPanelSnapshot.empty();
    }

    private boolean containsProbeTask(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return normalized.contains("显形")
                || normalized.contains("显行")
                || normalized.contains("显形镜")
                || normalized.contains("显行镜");
    }

    private boolean startProbeTrackerPathing(TaskExecutionContext context, TrackerGreenLinkScan scan) {
        initializeProbeRuntimeIfNeeded(scan);
        int nextIndex = nextProbeIndexToPath();
        if (nextIndex < 0) {
            log.warn("[wubei] probe objective has no remaining green segment to path: used={} attempts={}",
                    probeUsedSummary(), probeAttemptSummary());
            return false;
        }
        currentProbeIndex = nextIndex;
        String label = probeLabel(nextIndex);
        TrackerGreenLinkSegment segment = currentProbeSegments.get(nextIndex);
        currentTrackerDestinationHint = null;
        log.info("[wubei] probe-objective pathing start: label={} index={}/{} used={} attempts={} segment={}",
                label, nextIndex + 1, currentProbeSegments.size(), probeUsedSummary(), probeAttemptSummary(), segment);
        return clickTaskTrackerGreen(context, segment, label, 1);
    }

    private WubeiStepOutcome resolveProbeAfterPathing(TaskExecutionContext context, WubeiRoundContext state) {
        int index = currentProbeIndex;
        String label = probeLabel(index);
        GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
        log.info("[wubei] resolve probe after pathing: label={} state={} used={} attempts={} hint={}",
                label, movementState, probeUsedSummary(), probeAttemptSummary(), currentTrackerDestinationHint);
        if (movementState == GameStateUtil.MovementState.MOVING
                || movementState == GameStateUtil.MovementState.PATHING_ACTIVE
                || movementState == GameStateUtil.MovementState.MAYBE_MOVING
                || movementState == GameStateUtil.MovementState.UNKNOWN) {
            return WubeiStepOutcome.sharedState(state, "probe pathing still active");
        }

        if (tryClickKnownEnterBattleDialog("wubei:probe-before-item:" + label)) {
            log.info("[wubei] probe path reached direct battle dialog; treat as combat target: label={}", label);
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.WAIT_BATTLE_FINISH, "probe-direct-battle-dialog"),
                    "probe direct battle dialog clicked");
        }

        if (!useProbeItemWithRuntimeRecord(context, index, label)) {
            if (probeCanRetryItem(index)) {
                log.warn("[wubei] probe item use failed; retry same probe point later: label={} used={} attempts={}",
                        label, probeUsedSummary(), probeAttemptSummary());
                return WubeiStepOutcome.sharedState(state, "probe item use retry");
            }
            return WubeiStepOutcome.failed(state, "probe item use failed");
        }

        if (isProbeTargetReadyStoryVisible(label)) {
            markProbeResolved(index);
            log.info("[wubei] probe target-ready story matched; smart-click spawned target: label={}", label);
            if (tryClickProbeSpawnedTarget(context, label, true)) {
                return WubeiStepOutcome.continueTo(
                        state.next(WubeiPhase.WAIT_BATTLE_FINISH, "probe-tooltip-clicked"),
                        "probe target tooltip clicked");
            }
            return WubeiStepOutcome.failed(state, "probe target story visible but target click failed");
        }

        if (isProbeWrongPositionStoryVisible(label)) {
            rollbackProbeItemAttempt(index, label, "wrong-position-story");
            /*
             * “位置不对”只证明当前绿字还没真正到位，通常是移动停稳误判导致提前使用显形镜。
             * 这里必须保留 currentProbeIndex，不清 story、不切下一条绿字，让同一条绿字重新寻路。
             */
            log.warn("[wubei] probe item used at wrong position; retry current probe pathing: label={} used={} attempts={}",
                    label, probeUsedSummary(), probeAttemptSummary());
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.TRACKER_PATHING, "probe-wrong-position-repath"),
                    "probe wrong position; retry current green link");
        }

        // 成功 story 偶尔会漏检；如果白龙马 tooltip 已经出现，先尝试进入战斗，
        // 不要过早重试显形镜或切到第二个绿字。
        if (tryClickProbeSpawnedTarget(context, label, false)) {
            markProbeResolved(index);
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.WAIT_BATTLE_FINISH, "probe-tooltip-clicked-without-story"),
                    "probe target tooltip clicked without story confirmation");
        }

        if (probeCanRetryItem(index)) {
            log.warn("[wubei] probe item used but target-ready story not matched; retry same probe point: label={} used={} attempts={}",
                    label, probeUsedSummary(), probeAttemptSummary());
            return WubeiStepOutcome.sharedState(state, "probe story missing; retry item");
        }

        markProbeResolved(index);
        int nextUnused = nextUnusedProbeIndex();
        if (nextUnused >= 0) {
            currentProbeIndex = nextUnused;
            log.info("[wubei] probe story still missing after retries; switch to next unused probe: current={} next={} used={} attempts={}",
                    label, probeLabel(nextUnused), probeUsedSummary(), probeAttemptSummary());
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.TRACKER_PATHING, "probe-next-unused"),
                    "probe next unused point");
        }

        log.warn("[wubei] probe exhausted without target-ready story: used={} attempts={}",
                probeUsedSummary(), probeAttemptSummary());
        return WubeiStepOutcome.failed(state, "probe exhausted without target-ready story");
    }

    private boolean triggerCombatTrackerPathing(TaskExecutionContext context, TrackerGreenLinkScan scan) {
        if (scan.segments().isEmpty()) {
            log.warn("[wubei] no tracker green segment for combat pathing");
            return false;
        }
        TrackerGreenLinkSegment segment = scan.segments().get(0);
        for (int attempt = 1; attempt <= MAX_TRACKER_CLICK_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            if (clickTaskTrackerGreen(context, segment, "combat", attempt)) {
                return true;
            }
            TaskSleep.sleepOrStop(context, 800L, "Wubei task interrupted");
        }
        log.warn("[wubei] tracker green click failed after {} attempts", MAX_TRACKER_CLICK_ATTEMPTS);
        return false;
    }

    private void initializeProbeRuntimeIfNeeded(TrackerGreenLinkScan scan) {
        if (isProbeRuntimeActive()) {
            return;
        }
        currentProbeSegments = List.copyOf(scan.segments());
        currentProbeUsed = new boolean[currentProbeSegments.size()];
        currentProbeItemAttempts = new int[currentProbeSegments.size()];
        currentProbeIndex = currentProbeSegments.isEmpty() ? -1 : 0;
        log.info("[wubei] probe runtime initialized: links={} segments={}",
                currentProbeSegments.size(), currentProbeSegments);
    }

    private void resetProbeRuntime() {
        currentProbeSegments = List.of();
        currentProbeUsed = new boolean[0];
        currentProbeItemAttempts = new int[0];
        currentProbeIndex = -1;
    }

    private boolean isProbeRuntimeActive() {
        return currentProbeIndex >= 0 && !currentProbeSegments.isEmpty();
    }

    private int nextProbeIndexToPath() {
        if (!isProbeRuntimeActive()) {
            return -1;
        }
        if (isValidProbeIndex(currentProbeIndex) && !currentProbeUsed[currentProbeIndex]) {
            return currentProbeIndex;
        }
        return nextUnusedProbeIndex();
    }

    private int nextUnusedProbeIndex() {
        for (int i = 0; i < currentProbeUsed.length; i++) {
            if (!currentProbeUsed[i]) {
                return i;
            }
        }
        return -1;
    }

    private boolean isValidProbeIndex(int index) {
        return index >= 0 && index < currentProbeSegments.size()
                && index < currentProbeUsed.length
                && index < currentProbeItemAttempts.length;
    }

    private boolean useProbeItemWithRuntimeRecord(TaskExecutionContext context, int index, String label) {
        if (!isValidProbeIndex(index)) {
            log.warn("[wubei] probe item skipped for invalid index: label={} index={} links={}",
                    label, index, currentProbeSegments.size());
            return false;
        }
        currentProbeItemAttempts[index]++;
        boolean used = useProbeItem(context, label);
        log.info("[wubei] probe item runtime record: label={} used={} usedState={} attempts={}",
                label, used, probeUsedSummary(), probeAttemptSummary());
        return used;
    }

    private void rollbackProbeItemAttempt(int index, String label, String reason) {
        if (!isValidProbeIndex(index) || currentProbeItemAttempts[index] <= 0) {
            return;
        }
        currentProbeItemAttempts[index]--;
        log.info("[wubei] rollback probe item attempt: label={} reason={} attempts={}",
                label, reason, probeAttemptSummary());
    }

    private void markProbeResolved(int index) {
        if (!isValidProbeIndex(index)) {
            return;
        }
        currentProbeUsed[index] = true;
    }

    private boolean probeCanRetryItem(int index) {
        return isValidProbeIndex(index)
                && !currentProbeUsed[index]
                && currentProbeItemAttempts[index] < MAX_PROBE_ITEM_ATTEMPTS_PER_LINK;
    }

    private String probeLabel(int index) {
        if (index == 0) {
            return "first-probe";
        }
        if (index == 1) {
            return "second-probe";
        }
        return "probe-" + (index + 1);
    }

    private String probeUsedSummary() {
        if (currentProbeUsed.length == 0) {
            return "[]";
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < currentProbeUsed.length; i++) {
            values.add(probeLabel(i) + "=" + currentProbeUsed[i]);
        }
        return values.toString();
    }

    private String probeAttemptSummary() {
        if (currentProbeItemAttempts.length == 0) {
            return "[]";
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < currentProbeItemAttempts.length; i++) {
            values.add(probeLabel(i) + "=" + currentProbeItemAttempts[i]);
        }
        return values.toString();
    }

    private boolean clickTaskTrackerGreen(TaskExecutionContext context, TrackerGreenLinkSegment segment, String label, int attempt) {
        int baseX = segment.minX() + Math.min(18, Math.max(0, segment.width() / 3));
        int baseY = (segment.minY() + segment.maxY()) / 2;
        int randomRadiusX = Math.min(6, Math.max(2, segment.width() / 8));
        Point click = coordinateHelper.getRandomizedPoint(baseX, baseY, randomRadiusX, 3);
        log.info("[wubei] click tracker green: label={} attempt={} segment={} click=({}, {})",
                label, attempt, segment, click.x, click.y);
        boolean clicked = inputSequences.submitAndWait("wubei:tracker-green-click:" + label, List.of(
                InputAction.moveMouse(click.x, click.y),
                InputAction.sleep(120),
                InputAction.clickLeft(click.x, click.y, 300)
        ));
        if (clicked) {
            gameStateUtil.recordMovementIntent("wubei:tracker-green-click:" + label);
            captureTrackerDestinationHint(context, label).ifPresent(hint -> currentTrackerDestinationHint = hint);
        }
        return clicked;
    }

    /*
     * 五倍绿字寻路会短暂弹出“正在自动寻路前往地图(x,y)”浮框。这里只做 HWND 截图、
     * 洗图和 OCR，不发送鼠标键盘输入；它可以和后续窗口输入调度解耦，但采样时机必须贴近
     * 绿字点击，否则浮框会自然消失。
     */
    private Optional<TrackerDestinationHint> captureTrackerDestinationHint(TaskExecutionContext context, String label) {
        long deadline = System.currentTimeMillis() + TRACKER_DEST_HINT_MAX_CAPTURE_MS;
        for (int sample = 1; sample <= TRACKER_DEST_HINT_SAMPLES; sample++) {
            long remainingMs = deadline - System.currentTimeMillis();
            if (remainingMs <= 0) {
                log.info("[wubei] destination hint capture timeout: label={} maxMs={}",
                        label, TRACKER_DEST_HINT_MAX_CAPTURE_MS);
                return Optional.empty();
            }
            TaskSleep.sleepOrStop(context, TRACKER_DEST_HINT_SAMPLE_DELAY_MS, "Wubei task interrupted");
            tracker.refreshWindowState();
            String safeLabel = safeFileToken(label);
            String rawPath = windowScopedTempPath.resolve(
                    "wubei_tracker_destination_hint_" + safeLabel + "_" + sample + "_raw.png");
            String yellowPath = windowScopedTempPath.resolve(
                    "wubei_tracker_destination_hint_" + safeLabel + "_" + sample + "_yellow.png");
            OcrWindowRegion hintRegion = TRACKER_DEST_HINT_REGION;
            int left = tracker.getWindowBaseX() + hintRegion.x1();
            int top = tracker.getWindowBaseY() + hintRegion.y1();
            int right = tracker.getWindowBaseX() + hintRegion.x2();
            int bottom = tracker.getWindowBaseY() + hintRegion.y2();
            long captureStartedAt = System.currentTimeMillis();
            if (!tracker.captureToFile("wubei-destination-hint:" + label, rawPath, left, top, right, bottom)) {
                log.warn("[wubei] destination hint capture failed: label={} sample={} rect=({}, {})-({}, {})",
                        label, sample, left, top, right, bottom);
                continue;
            }
            long captureMs = System.currentTimeMillis() - captureStartedAt;
            /*
             * 浮框位置和小对话框一致，直接复用 dialog-small 窗口相对区域。
             * 这里不再截整窗，也不再套全局 mask，避免把任务追踪/聊天里的黄字带进 OCR。
             */
            ImagePreprocessor.washYellowText(rawPath, yellowPath);
            /*
             * 绿字点击后的寻路 hint 生命周期很短，最多只有几秒。这里必须只做本地 OCR；
             * 混合 matcher 会在本地失败时等待外部 OCR，反而拖住任务窗口权。
             */
            List<OcrWordResult> words = textRecognizer.getAllTextResultsLocalOnly(yellowPath);
            String text = words.stream().map(OcrWordResult::getText).collect(Collectors.joining(""));
            Optional<TrackerDestinationHint> parsed = parseTrackerDestinationHint(text);
            if (parsed.isPresent()) {
                TrackerDestinationHint hint = parsed.get();
                log.info("[wubei] destination hint parsed: label={} sample={} region={} captureMs={} map={} coord=({}, {}) text='{}' raw={} yellow={}",
                        label, sample, hintRegion.toShortText(), captureMs, hint.mapName(), hint.x(), hint.y(),
                        hint.rawText(), rawPath, yellowPath);
                return parsed;
            }
            log.info("[wubei] destination hint not parsed: label={} sample={} region={} captureMs={} text='{}' raw={} yellow={}",
                    label, sample, hintRegion.toShortText(), captureMs, text, rawPath, yellowPath);
        }
        return Optional.empty();
    }

    private Optional<TrackerDestinationHint> parseTrackerDestinationHint(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.replaceAll("\\s+", "");
        Matcher matcher = TRACKER_DEST_HINT_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            String mapName = matcher.group(1).trim();
            int x = Integer.parseInt(matcher.group(2));
            int y = Integer.parseInt(matcher.group(3));
            if (mapName.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new TrackerDestinationHint(mapName, x, y, text));
        } catch (NumberFormatException e) {
            log.warn("[wubei] destination hint coordinate parse failed: text='{}'", text, e);
            return Optional.empty();
        }
    }

    private TrackerPanelSnapshot captureTrackerPanelSnapshot(String source, boolean allowExpandedAnchorSearch) {
        int[] rect = resolveTrackerPanelRect("green-links", allowExpandedAnchorSearch);
        if (rect == null) {
            return TrackerPanelSnapshot.empty();
        }
        int left = rect[0];
        int top = rect[1];
        int right = rect[2];
        int bottom = rect[3];
        String captureId = safeFileToken(source) + "_" + System.currentTimeMillis();
        String widePath = windowScopedTempPath.resolve("wubei_tracker_panel_" + captureId + "_wide_raw.png");
        int wideLeft = tracker.getWindowBaseX() + TRACKER_ANCHOR_SEARCH_REL_LEFT;
        int wideTop = tracker.getWindowBaseY() + TRACKER_ANCHOR_SEARCH_REL_TOP;
        int wideRight = tracker.getWindowBaseX() + TRACKER_ANCHOR_SEARCH_REL_RIGHT;
        int wideBottom = tracker.getWindowBaseY() + TRACKER_ANCHOR_SEARCH_REL_BOTTOM;
        boolean wideCaptured = tracker.captureToFile(
                "wubei-tracker-panel-wide:" + source,
                widePath,
                wideLeft,
                wideTop,
                wideRight,
                wideBottom);
        String rawPath = windowScopedTempPath.resolve("wubei_tracker_panel_" + captureId + "_raw.png");
        if (!tracker.captureToFile("wubei-tracker-panel:" + source, rawPath, left, top, right, bottom)) {
            log.warn("[wubei] tracker panel capture failed: source={} rect=({}, {})-({}, {})",
                    source, left, top, right, bottom);
            return TrackerPanelSnapshot.empty();
        }
        BufferedImage frame;
        try {
            frame = ImageIO.read(new File(rawPath));
        } catch (Exception e) {
            log.warn("[wubei] tracker panel image read failed: source={} path={}", source, rawPath, e);
            return TrackerPanelSnapshot.empty();
        }
        if (frame == null) {
            log.warn("[wubei] tracker panel image unreadable: source={} path={}", source, rawPath);
            return TrackerPanelSnapshot.empty();
        }
        try {
            log.info("[wubei] tracker panel capture: source={} id={} base=({}, {}) rect=({}, {})-({}, {}) size={}x{} raw={} wideCaptured={} wideRect=({}, {})-({}, {}) wide={}",
                    source, captureId,
                    tracker.getWindowBaseX(), tracker.getWindowBaseY(),
                    left, top, right, bottom,
                    frame.getWidth(), frame.getHeight(),
                    rawPath, wideCaptured,
                    wideLeft, wideTop, wideRight, wideBottom,
                    widePath);
            String yellowText = readTrackerYellowText(captureId, rawPath);
            TrackerGreenLinkScan greenScan = scanTrackerGreenLinks(frame, left, top);
            log.info("[wubei] tracker panel snapshot: source={} yellow='{}' greenSegments={} probe={} raw={}",
                    source, yellowText, greenScan.segments(), greenScan.isProbeObjective(), rawPath);
            return new TrackerPanelSnapshot(true, rect, yellowText, greenScan);
        } finally {
            frame.flush();
        }
    }

    private String readTrackerYellowText(String captureId, String rawPath) {
        String yellowPath = windowScopedTempPath.resolve("wubei_tracker_yellow_" + captureId + ".png");
        ImagePreprocessor.washYellowText(rawPath, yellowPath);
        List<OcrWordResult> words = textRecognizer.getAllTextResultsForMatch(
                yellowPath,
                "wubei-tracker-yellow:" + captureId,
                result -> !result.isEmpty());
        String text = words.stream().map(OcrWordResult::getText).collect(Collectors.joining("|"));
        log.info("[wubei] tracker yellow OCR: id={} text='{}' raw={} yellow={}",
                captureId, text, rawPath, yellowPath);
        return text;
    }

    private TrackerGreenLinkScan scanTrackerGreenLinks(BufferedImage frame, int left, int top) {
        List<ImagePreprocessor.GreenTextBand> bands = ImagePreprocessor.findGreenTextBands(frame);
        ImagePreprocessor.GreenTextBand band = ImagePreprocessor.pickGreenTextBand(bands, true);
        if (band == null) {
            log.info("[wubei] tracker green link scan: no green band");
            return TrackerGreenLinkScan.empty();
        }
        List<TrackerGreenLinkSegment> segments = splitTrackerGreenLinkSegments(frame, band, left, top);
        int bandWidth = band.maxX() - band.minX() + 1;
        boolean probe = segments.size() >= 2
                || (segments.size() == 1 && bandWidth > TRACKER_LINK_SINGLE_MAX_WIDTH);
        log.info("[wubei] tracker green link scan: bands={} band=({}, {})-({}, {}) width={} segments={} probe={}",
                bands.size(), left + band.minX(), top + band.minY(), left + band.maxX(), top + band.maxY(),
                bandWidth, segments, probe);
        return new TrackerGreenLinkScan(probe, segments, bandWidth);
    }

    /*
     * 五倍任务追踪不是用全屏随便扫绿字：先找“任务追踪”anchor，再按 anchor
     * 的相对偏移裁出任务内容区。这样后续黄字/绿字判断都只看当前任务面板。
     */
    private int[] resolveTrackerPanelRect(String source) {
        return resolveTrackerPanelRect(source, false);
    }

    private int[] resolveTrackerPanelRect(String source, boolean allowExpandedAnchorSearch) {
        tracker.refreshWindowState();
        int[] searchRect = new int[]{
                tracker.getWindowBaseX() + TRACKER_ANCHOR_SEARCH_REL_LEFT,
                tracker.getWindowBaseY() + TRACKER_ANCHOR_SEARCH_REL_TOP,
                tracker.getWindowBaseX() + TRACKER_ANCHOR_SEARCH_REL_RIGHT,
                tracker.getWindowBaseY() + TRACKER_ANCHOR_SEARCH_REL_BOTTOM
        };
        Point anchor = coordinateHelper.findImageInRegion(TRACKER_ANCHOR_TEMPLATE, searchRect, 0.82);
        if (anchor != null) {
            return trackerPanelRectFromAnchor(source, anchor, "narrow");
        }

        log.warn("[wubei] tracker anchor not found in narrow area: source={} searchRect=({}, {})-({}, {}) allowExpanded={}",
                source,
                searchRect[0], searchRect[1], searchRect[2], searchRect[3],
                allowExpandedAnchorSearch);
        if (!allowExpandedAnchorSearch) {
            return null;
        }

        OcrWindowRegion defaultRegion = OcrWindowScanService.defaultMaskedWindowRegion();
        int[] expandedRect = new int[]{
                tracker.getWindowBaseX() + defaultRegion.x1(),
                tracker.getWindowBaseY() + defaultRegion.y1(),
                tracker.getWindowBaseX() + defaultRegion.x2(),
                tracker.getWindowBaseY() + defaultRegion.y2()
        };
        Point expandedAnchor = coordinateHelper.findImageInRegion(TRACKER_ANCHOR_TEMPLATE, expandedRect, 0.82);
        if (expandedAnchor != null) {
            return trackerPanelRectFromAnchor(source, expandedAnchor, "expanded-default");
        }
        log.warn("[wubei] tracker anchor not found in expanded default region: source={} rect=({}, {})-({}, {})",
                source, expandedRect[0], expandedRect[1], expandedRect[2], expandedRect[3]);
        return null;
    }

    private int[] trackerPanelRectFromAnchor(String source, Point anchor, String searchMode) {
        int[] panelRect = new int[]{
                anchor.x + TRACKER_PANEL_FROM_ANCHOR_LEFT,
                anchor.y + TRACKER_PANEL_FROM_ANCHOR_TOP,
                anchor.x + TRACKER_PANEL_FROM_ANCHOR_RIGHT,
                anchor.y + TRACKER_PANEL_FROM_ANCHOR_BOTTOM
        };
        log.info("[wubei] tracker panel rect resolved by anchor: source={} mode={} anchor=({}, {}) rect=({}, {})-({}, {})",
                source, searchMode, anchor.x, anchor.y,
                panelRect[0], panelRect[1], panelRect[2], panelRect[3]);
        return panelRect;
    }

    private List<TrackerGreenLinkSegment> splitTrackerGreenLinkSegments(
            BufferedImage frame,
            ImagePreprocessor.GreenTextBand band,
            int absoluteLeft,
            int absoluteTop) {
        List<TrackerGreenGlyph> glyphs = collectTrackerGreenGlyphs(frame, band);
        List<TrackerGreenLinkSegment> segments = new ArrayList<>();
        int startX = -1;
        int endX = -1;
        int pixels = 0;
        TrackerGreenGlyph previous = null;
        for (int i = 0; i < glyphs.size(); i++) {
            TrackerGreenGlyph glyph = glyphs.get(i);
            boolean delimiter = isTrackerLinkDelimiter(glyph, pixels, remainingPixels(glyphs, i + 1));
            boolean largeGap = startX >= 0
                    && previous != null
                    && glyph.minX() - previous.maxX() - 1 >= TRACKER_LINK_SPLIT_GAP;
            if (delimiter) {
                addTrackerSegment(segments, absoluteLeft, absoluteTop, startX, endX, band, pixels);
                startX = -1;
                endX = -1;
                pixels = 0;
                previous = glyph;
                continue;
            }
            if (largeGap) {
                addTrackerSegment(segments, absoluteLeft, absoluteTop, startX, endX, band, pixels);
                startX = -1;
                endX = -1;
                pixels = 0;
            }

            if (startX < 0) {
                startX = glyph.minX();
            }
            endX = glyph.maxX();
            pixels += glyph.pixels();
            previous = glyph;
        }
        addTrackerSegment(segments, absoluteLeft, absoluteTop, startX, endX, band, pixels);
        log.info("[wubei] tracker green glyph split: glyphs={} segments={}", glyphs, segments);
        return segments;
    }

    private List<TrackerGreenGlyph> collectTrackerGreenGlyphs(BufferedImage frame, ImagePreprocessor.GreenTextBand band) {
        List<TrackerGreenGlyph> glyphs = new ArrayList<>();
        int startX = -1;
        int endX = -1;
        int pixels = 0;
        for (int x = band.minX(); x <= band.maxX(); x++) {
            int columnPixels = 0;
            for (int y = band.minY(); y <= band.maxY(); y++) {
                if (ImagePreprocessor.isOptionGreen(frame.getRGB(x, y))) {
                    columnPixels++;
                }
            }
            if (columnPixels > 0) {
                if (startX < 0) {
                    startX = x;
                }
                endX = x;
                pixels += columnPixels;
            } else if (startX >= 0) {
                glyphs.add(new TrackerGreenGlyph(startX, endX, pixels));
                startX = -1;
                endX = -1;
                pixels = 0;
            }
        }
        if (startX >= 0) {
            glyphs.add(new TrackerGreenGlyph(startX, endX, pixels));
        }
        return glyphs;
    }

    private boolean isTrackerLinkDelimiter(TrackerGreenGlyph glyph, int leftPixels, int rightPixels) {
        return glyph.width() <= TRACKER_LINK_DELIMITER_MAX_WIDTH
                && glyph.pixels() <= TRACKER_LINK_DELIMITER_MAX_PIXELS
                && leftPixels >= TRACKER_LINK_MIN_PIXELS
                && rightPixels >= TRACKER_LINK_MIN_PIXELS;
    }

    private int remainingPixels(List<TrackerGreenGlyph> glyphs, int fromIndex) {
        int total = 0;
        for (int i = fromIndex; i < glyphs.size(); i++) {
            total += glyphs.get(i).pixels();
        }
        return total;
    }

    private void addTrackerSegment(
            List<TrackerGreenLinkSegment> segments,
            int absoluteLeft,
            int absoluteTop,
            int startX,
            int endX,
            ImagePreprocessor.GreenTextBand band,
            int pixels) {
        if (pixels < TRACKER_LINK_MIN_PIXELS || endX < startX) {
            return;
        }
        segments.add(new TrackerGreenLinkSegment(
                absoluteLeft + startX,
                absoluteTop + band.minY(),
                absoluteLeft + endX,
                absoluteTop + band.maxY(),
                pixels));
    }

    private boolean useProbeItem(TaskExecutionContext context, String label) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        boolean used = bagService.findAndUseItemFromBack(BagService.MAIN_BAG, PROBE_ITEM_TEMPLATE, 5, context);
        log.info("[wubei] probe item used: label={} used={}", label, used);
        TaskSleep.sleepOrStop(context, 700L, "Wubei task interrupted");
        return used;
    }

    private boolean isProbeTargetReadyStoryVisible(String label) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyWhiteTemplate(
                "wubei:probe-story:" + label,
                STORY_PROBE_TARGET_READY,
                PROBE_STORY_TEMPLATE));
        boolean visible = STORY_PROBE_TARGET_READY.equals(result.getActionKey())
                && result.getStatus() == DialogResultStatus.WHITE_TEMPLATE_VISIBLE;
        log.info("[wubei] probe target-ready story check: label={} visible={} status={}",
                label, visible, result.getStatus());
        return visible;
    }

    private boolean isProbeWrongPositionStoryVisible(String label) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyWhiteTemplate(
                "wubei:probe-wrong-position:" + label,
                STORY_PROBE_WRONG_POSITION,
                PROBE_WRONG_POSITION_TEMPLATE));
        boolean visible = STORY_PROBE_WRONG_POSITION.equals(result.getActionKey())
                && result.getStatus() == DialogResultStatus.WHITE_TEMPLATE_VISIBLE;
        log.info("[wubei] probe wrong-position story check: label={} visible={} status={}",
                label, visible, result.getStatus());
        return visible;
    }

    private boolean tryClickProbeSpawnedTarget(TaskExecutionContext context, String label, boolean storyConfirmed) {
        boolean clicked = tryClickTrackerCombatTargetSmart(context, label + (storyConfirmed ? "-story" : "-no-story"));
        if (clicked || !storyConfirmed) {
            return clicked;
        }
        return tryDirectCombatFromTrackerHint(context, label + "-direct-combat");
    }

    private boolean tryClickTrackerCombatTargetSmart(TaskExecutionContext context, String label) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        if (currentTrackerDestinationHint == null) {
            log.info("[wubei] smart combat target click skipped: label={} reason=no-destination-hint", label);
            return false;
        }
        String targetName = resolveDirectCombatTargetName(label);
        if (targetName.isBlank()) {
            log.warn("[wubei] smart combat target click skipped: label={} reason=no-combat-target yellow='{}' hint={}",
                    label,
                    currentTrackerSnapshot == null ? null : currentTrackerSnapshot.yellowText(),
                    currentTrackerDestinationHint);
            return false;
        }

        /*
         * Do not use the old all-screen task-tooltip shortcut here. Smart click constrains tooltip,
         * yellow-name, formula, and Ctrl probes to the current tracker destination, then the 五倍
         * dialog templates below prove we opened the correct battle dialog before clicking it.
         *
         * 显形镜刷出的白龙马有一个额外坑：如果“位置不对”的 story 还挡在屏幕中间，
         * tooltip 仍可能露出来可点，但黄字/紫字/记忆点直点会被 story 遮住。只对白龙马
         * 启用 tooltip-first，并且只在 tooltip 失败后让 NpcClickService 清一次 story。
         */
        boolean probeTarget = PROBE_TARGET_NPC_NAME.equals(targetName);
        NpcClickRequest request = NpcClickRequest.builder()
                .player(gameContext.getMe())
                .mapName(currentTrackerDestinationHint.mapName())
                .mapX(currentTrackerDestinationHint.x())
                .mapY(currentTrackerDestinationHint.y())
                .npcName(targetName)
                .tuneX(-10)
                .tuneY(0)
                .expectedDialogTemplatePaths(List.of(
                        ENTER_BATTLE_TEMPLATE,
                        ENTER_BATTLE_PROVE_TEMPLATE,
                        ENTER_BATTLE_KUIXING_TEMPLATE))
                .roamingTarget(true)
                .tooltipFirst(probeTarget)
                .closeStoryBeforeDirectSceneClick(probeTarget)
                .build();
        log.info("[wubei] try smart combat target click: label={} target={} hint={}",
                label, targetName, currentTrackerDestinationHint);
        if (!npcClickService.clickNpcSmart(request)) {
            return false;
        }
        return tryClickKnownEnterBattleDialog("wubei:smart-combat-target:" + label);
    }

    private boolean tryDirectCombatFromTrackerHint(TaskExecutionContext context, String label) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        if (currentTrackerDestinationHint == null) {
            log.info("[wubei] direct combat fallback skipped: label={} reason=no-destination-hint", label);
            return false;
        }

        String targetName = resolveDirectCombatTargetName(label);
        if (targetName.isBlank()) {
            log.warn("[wubei] direct combat fallback skipped: label={} reason=no-combat-target yellow='{}' hint={}",
                    label,
                    currentTrackerSnapshot == null ? null : currentTrackerSnapshot.yellowText(),
                    currentTrackerDestinationHint);
            return false;
        }

        /*
         * This Alt+A fallback is only reached after 五倍 tracker pathing has stopped near the
         * tracker destination. It must not be used for accept/maintenance NPCs, because Alt+A turns
         * the next click into a direct battle click instead of opening the normal NPC dialog.
         */
        NpcClickRequest request = NpcClickRequest.builder()
                .player(gameContext.getMe())
                .mapName(currentTrackerDestinationHint.mapName())
                .mapX(currentTrackerDestinationHint.x())
                .mapY(currentTrackerDestinationHint.y())
                .npcName(targetName)
                .tuneX(-10)
                .tuneY(0)
                .expectedDialogTemplatePath(ENTER_BATTLE_TEMPLATE)
                .roamingTarget(true)
                .build();
        log.info("[wubei] try direct combat fallback: label={} target={} hint={}",
                label, targetName, currentTrackerDestinationHint);
        boolean enteredCombat = npcClickService.tryDirectCombatTargetClick(request);
        if (!enteredCombat) {
            return false;
        }
        autoCombatService.initializeForCurrentWindow();
        TaskSleep.sleepOrStop(context, 1200L, "Wubei task interrupted");
        return true;
    }

    private String resolveDirectCombatTargetName(String label) {
        if (isProbeRuntimeActive() || (label != null && label.contains("probe"))) {
            return PROBE_TARGET_NPC_NAME;
        }
        return resolveTrackerCombatTargetName();
    }

    private boolean tryClickKnownEnterBattleDialog(String source) {
        DialogResult confirm = dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
                source,
                List.of(
                        new GreenTemplateClickSpec(OPTION_ENTER_BATTLE, ENTER_BATTLE_TEMPLATE, -6, 18, 4),
                        new GreenTemplateClickSpec(OPTION_ENTER_BATTLE_PROVE, ENTER_BATTLE_PROVE_TEMPLATE, -6, 18, 4),
                        new GreenTemplateClickSpec(OPTION_ENTER_BATTLE_KUIXING, ENTER_BATTLE_KUIXING_TEMPLATE, -6, 18, 4)),
                /*
                 * 五倍的进战斗弹窗有些会被全局 dialog mask 判成 STORY，但里面仍然有绿色
                 * 业务选项。这里按任务已知模板优先匹配，避免被 OPTION/STORY 粗分类挡住。
                 */
                false));
        boolean clicked = OPTION_ENTER_BATTLE.equals(confirm.getActionKey())
                || OPTION_ENTER_BATTLE_PROVE.equals(confirm.getActionKey())
                || OPTION_ENTER_BATTLE_KUIXING.equals(confirm.getActionKey());
        log.info("[wubei] known enter-battle dialog check: source={} clicked={} status={} action={}",
                source, clicked, confirm.getStatus(), confirm.getActionKey());
        return clicked;
    }

    private WubeiStepOutcome tickWaitBattleFinish(TaskExecutionContext context, WubeiRoundContext state) {
        long now = System.currentTimeMillis();
        if (waitBattleStartedAt <= 0L) {
            waitBattleStartedAt = now;
            waitBattleNextTrackerRetryAt = now + 6_000L;
            waitBattleSawCombat = false;
        }

        /*
         * The pause checkpoint may block while the user pauses during combat. That wall-clock gap
         * must not count toward WAIT_BATTLE_FINISH, otherwise resume can instantly time out.
         */
        long checkpointStartedAt = now;
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        now = System.currentTimeMillis();
        long checkpointBlockedMs = now - checkpointStartedAt;
        if (checkpointBlockedMs >= PAUSE_TIMER_COMPENSATION_THRESHOLD_MS) {
            waitBattleStartedAt += checkpointBlockedMs;
            waitBattleNextTrackerRetryAt += checkpointBlockedMs;
            log.info("[wubei] wait battle timer paused: blockedMs={} adjustedStartAt={} adjustedNextRetryAt={}",
                    checkpointBlockedMs, waitBattleStartedAt, waitBattleNextTrackerRetryAt);
        }

        if (now - waitBattleStartedAt >= WAIT_BATTLE_TIMEOUT_MS) {
            log.warn("[wubei] wait battle timeout: chained={} elapsedMs={} timeoutMs={}",
                    currentRoundChainedCombatExpected, now - waitBattleStartedAt, WAIT_BATTLE_TIMEOUT_MS);
            return WubeiStepOutcome.failed(state, "wait battle timeout");
        }

        AutoCombatService.TickResult tick = autoCombatService.handleCombatTick(context, TASK_CODE, true);
        if (tick == AutoCombatService.TickResult.EXIT_RECOVERED) {
            log.info("[wubei] battle finished and recovered");
            if (currentRoundChainedCombatExpected) {
                return WubeiStepOutcome.sharedState(
                        state.next(WubeiPhase.POST_BATTLE_RECOVER, "chained-battle-finished"),
                        "chained battle finished");
            }
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.POST_BATTLE_RECOVER, "battle-finished"),
                    "battle finished");
        }
        if (tick == AutoCombatService.TickResult.IN_COMBAT) {
            waitBattleSawCombat = true;
            /*
             * Battle is shared state. Return quickly so member auto-battle tasks can acquire the
             * task turn and refresh their own automatic-combat panels before rounds expire.
             */
            return WubeiStepOutcome.sharedState(state, "combat still running");
        }

        if (tryClickKnownEnterBattleDialog("wubei:enter-battle")) {
            autoCombatService.initializeForCurrentWindow();
            TaskSleep.sleepOrStop(context, 1200L, "Wubei task interrupted");
            return WubeiStepOutcome.sharedState(state, "battle dialog clicked; wait for combat entry");
        }

        if (!waitBattleSawCombat && now >= waitBattleNextTrackerRetryAt) {
            GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
            if (movementState == GameStateUtil.MovementState.MOVING
                    || movementState == GameStateUtil.MovementState.PATHING_ACTIVE
                    || movementState == GameStateUtil.MovementState.MAYBE_MOVING) {
                log.info("[wubei] skip tracker green retry while pathing: state={} nextRetryMs={}",
                        movementState, 3_000);
                waitBattleNextTrackerRetryAt = now + 3_000L;
                return WubeiStepOutcome.sharedState(state, "pathing toward combat target");
            }
            if (isNearCurrentTrackerDestination()) {
                log.info("[wubei] destination hint says leader has arrived; try tooltip fallback: hint={}",
                        currentTrackerDestinationHint);
                if (tryClickTrackerCombatTargetSmart(context, "destination-hint-smart-click")) {
                    waitBattleNextTrackerRetryAt = now + 6_000L;
                    return WubeiStepOutcome.sharedState(state, "destination smart target clicked");
                }
                if (tryDirectCombatFromTrackerHint(context, "destination-hint-direct-combat")) {
                    waitBattleNextTrackerRetryAt = now + 6_000L;
                    return WubeiStepOutcome.sharedState(state, "direct combat click confirmed; wait for combat entry");
                }
            }
            if (shouldRetryTrackerGreenInBattleWait()
                    && currentTrackerSnapshot != null
                    && !currentTrackerSnapshot.greenScan().segments().isEmpty()) {
                clickTaskTrackerGreen(context, currentTrackerSnapshot.greenScan().segments().get(0), "combat-retry", 0);
            } else {
                log.info("[wubei] skip generic tracker green retry: probeActive={} chained={} hasSnapshot={}",
                        isProbeRuntimeActive(),
                        currentRoundChainedCombatExpected,
                        currentTrackerSnapshot != null);
            }
            waitBattleNextTrackerRetryAt = now + 6_000L;
        }
        return WubeiStepOutcome.sharedState(state, "waiting for battle");
    }

    private boolean shouldRetryTrackerGreenInBattleWait() {
        /*
         * 普通单绿字和黄袍怪可以用“再点任务追踪绿字”重新拉起自动寻路/战斗弹窗。
         * 显形镜双绿字必须保留当前 probe 状态，不能在等待战斗时固定回点第一个绿字。
         */
        return !isProbeRuntimeActive();
    }

    private void resetWaitBattleRuntime() {
        waitBattleStartedAt = 0L;
        waitBattleNextTrackerRetryAt = 0L;
        waitBattleSawCombat = false;
    }

    private boolean isNearCurrentTrackerDestination() {
        if (currentTrackerDestinationHint == null) {
            return false;
        }
        LocationInfo location = playerStateService.syncMyPosition();
        if (location == null || location.mapName == null) {
            log.info("[wubei] destination hint fallback skipped: current location unavailable hint={}",
                    currentTrackerDestinationHint);
            return false;
        }
        return isNearCurrentTrackerDestination(location, currentTrackerDestinationHint);
    }

    private boolean isNearCurrentTrackerDestination(LocationInfo location, TrackerDestinationHint hint) {
        boolean sameMap = sameLooseMapName(location.mapName, hint.mapName());
        int dx = Math.abs(location.x - hint.x());
        int dy = Math.abs(location.y - hint.y());
        boolean near = sameMap && dx <= TRACKER_DEST_HINT_ARRIVAL_TOLERANCE
                && dy <= TRACKER_DEST_HINT_ARRIVAL_TOLERANCE;
        log.info("[wubei] destination hint arrival check: current={}({}, {}) hint={}({}, {}) sameMap={} dx={} dy={} near={}",
                location.mapName, location.x, location.y,
                hint.mapName(), hint.x(), hint.y(),
                sameMap, dx, dy, near);
        return near;
    }

    private boolean sameLooseMapName(String current, String expected) {
        String a = normalizeMapName(current, "wubei:current-location");
        String b = normalizeMapName(expected, "wubei:tracker-destination-hint");
        return !a.isEmpty() && !b.isEmpty() && (a.equals(b) || a.contains(b) || b.contains(a));
    }

    private String normalizeMapName(String value, String source) {
        if (value == null) {
            return "";
        }
        return mapNameCanonicalizer.canonicalize(value, source).replaceAll("\\s+", "");
    }

    private String safeFileToken(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private boolean useReturnItem(TaskExecutionContext context, String source, int attempt, int maxAttempts) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        log.info("[wubei] use return item and verify start map: source={} attempt={}/{}",
                source, attempt, maxAttempts);
        boolean used = bagService.findAndUseItemFromBack(BagService.MAIN_BAG, RETURN_ITEM_TEMPLATE, 5, context);
        if (!used) {
            log.warn("[wubei] return item not found/used: source={} attempt={}/{}",
                    source, attempt, maxAttempts);
            return false;
        }

        /*
         * Clicking the 五倍 return item is not enough evidence. Verify that the current map changed
         * back to 宝象国 before the round is allowed to finish and the next accept-task cycle starts.
         */
        TaskSleep.sleepOrStop(context, RETURN_VERIFY_DELAY_MS, "Wubei task interrupted");
        LocationInfo afterReturn = playerStateService.syncMyPosition();
        if (afterReturn != null && gameStateUtil.isSameMapName(afterReturn.mapName, START_MAP_NAME)) {
            log.info("[wubei] return item verified: source={} location={}", source, afterReturn);
            return true;
        }
        log.warn("[wubei] return item used but start map not verified: source={} location={}",
                source, afterReturn);
        return false;
    }

    private boolean useReturnItemAndVerifyStartMap(TaskExecutionContext context, String source) {
        for (int attempt = 1; attempt <= RETURN_ITEM_VERIFY_ATTEMPTS; attempt++) {
            if (useReturnItem(context, source, attempt, RETURN_ITEM_VERIFY_ATTEMPTS)) {
                return true;
            }
        }
        return false;
    }

    private WubeiStepOutcome returnHomeAfterCombatOrContinueSpecialTarget(TaskExecutionContext context,
                                                                          WubeiRoundContext state) {
        if (!currentRoundChainedCombatExpected) {
            if (!useReturnItemAndVerifyStartMap(context, "normal-combat")) {
                return WubeiStepOutcome.failed(state, "return home failed");
            }
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_ROUND_DONE_SOURCE),
                    "return home finished; check team return");
        }

        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        int combatCount = currentRoundChainedCombatContinueCount + 1;
        if (combatCount > MAX_CHAINED_COMBAT_ATTEMPTS) {
            log.warn("[wubei] chained combat limit reached: count={} max={}",
                    combatCount, MAX_CHAINED_COMBAT_ATTEMPTS);
            return WubeiStepOutcome.failed(state, "chained combat limit reached");
        }
        /*
         * 黄袍怪是否继续只信战后左侧任务追踪：还有“黄袍”就继续，没有才允许回程。
         * 战斗中右上角标记不能证明战后仍需要续打，所以这里不再做战斗内 marker 扫描。
         */
        TrackerPanelSnapshot postCombatSnapshot = captureTrackerPanelSnapshot(
                "post-combat-chained-" + combatCount, true);
        if (!postCombatSnapshot.anchorFound()) {
            log.warn("[wubei] chained combat tracker unreadable after battle: count={}", combatCount);
            return WubeiStepOutcome.failed(state, "chained combat tracker unreadable");
        }

        boolean stillChained = containsChainedCombatTarget(postCombatSnapshot.yellowText());
        log.info("[wubei] chained combat post-battle tracker: count={} stillChained={} yellow='{}'",
                combatCount, stillChained, postCombatSnapshot.yellowText());
        if (!stillChained) {
            currentRoundChainedCombatContinueCount = 0;
            if (!useReturnItemAndVerifyStartMap(context, "chained-combat-completed")) {
                return WubeiStepOutcome.failed(state, "return home failed");
            }
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_ROUND_DONE_SOURCE),
                    "chained combat completed; check team return");
        }

        currentTrackerSnapshot = postCombatSnapshot;
        currentRoundChainedCombatContinueCount = combatCount;
        if (!continueChainedCombatFromTracker(context, postCombatSnapshot, combatCount)) {
            log.warn("[wubei] chained combat tracker still has target but continue click failed: count={} yellow='{}'",
                    combatCount, postCombatSnapshot.yellowText());
            return WubeiStepOutcome.failed(state, "chained combat continue click failed");
        }
        log.info("[wubei] chained combat target continues: currentCount={} nextState=WAIT_BATTLE_FINISH",
                combatCount);
        return WubeiStepOutcome.sharedState(
                state.next(WubeiPhase.WAIT_BATTLE_FINISH, "chained-combat-continued-" + combatCount),
                "chained combat target clicked; wait for next battle");
    }

    private boolean continueChainedCombatFromTracker(
            TaskExecutionContext context,
            TrackerPanelSnapshot snapshot,
            int combatCount) {
        if (!snapshot.greenScan().segments().isEmpty()) {
            TrackerGreenLinkSegment segment = snapshot.greenScan().segments().get(0);
            return clickTaskTrackerGreen(context, segment, "chained-combat-" + combatCount, 1);
        }
        log.warn("[wubei] chained combat tracker has no green segment; try visible tooltip: count={} yellow='{}'",
                combatCount, snapshot.yellowText());
        return tryClickTrackerCombatTargetSmart(context, "chained-combat-tracker-fallback-" + combatCount);
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

    private record TrackerPanelSnapshot(boolean anchorFound,
                                        int[] rect,
                                        String yellowText,
                                        TrackerGreenLinkScan greenScan) {
        private static TrackerPanelSnapshot empty() {
            return new TrackerPanelSnapshot(false, null, "", TrackerGreenLinkScan.empty());
        }
    }

    private record TrackerGreenLinkScan(boolean isProbeObjective,
                                        List<TrackerGreenLinkSegment> segments,
                                        int bandWidth) {
        private static TrackerGreenLinkScan empty() {
            return new TrackerGreenLinkScan(false, List.of(), 0);
        }
    }

    private record TrackerGreenLinkSegment(int minX, int minY, int maxX, int maxY, int pixels) {
        private int width() {
            return maxX - minX + 1;
        }
    }

    private record TrackerGreenGlyph(int minX, int maxX, int pixels) {
        private int width() {
            return maxX - minX + 1;
        }
    }

    private record TrackerDestinationHint(String mapName, int x, int y, String rawText) {
    }
}
