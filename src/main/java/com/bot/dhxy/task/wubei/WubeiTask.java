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
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.model.maintenance.TaskMaintenanceResult;
import com.bot.dhxy.model.maintenance.TaskMaintenanceStatus;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcClickRequest;
import com.bot.dhxy.model.npc.NpcTargetEvidence;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.model.tasktracker.TaskTrackerGreenLink;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.DialogChoiceMemoryService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.MapNameCanonicalizer;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.service.NpcClickService;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.service.TaskMaintenanceService;
import com.bot.dhxy.service.TaskTrackerPanelService;
import com.bot.dhxy.service.TeamReturnService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
    private static final int MAX_CONSECUTIVE_MAINTENANCE_HOOK_FAILURES = 3;
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
    private static final long PROBE_ENTER_BATTLE_TIMEOUT_MS = 300_000L;
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
    private static final int TRACKER_TASK_BLOCK_HEIGHT = 60;
    private static final int TRACKER_TITLE_CENTER_FALLBACK_LEFT_SHIFT = 24;
    private static final long TRACKER_REFRESH_AFTER_ACCEPT_MS = 1_000L;
    private static final long TRACKER_REFRESH_RETRY_INTERVAL_MS = 350L;
    private static final int[] TRACKER_DEST_HINT_CAPTURE_OFFSETS_MS = {500, 1_000, 1_500};
    private static final int TRACKER_DEST_HINT_SAMPLES = TRACKER_DEST_HINT_CAPTURE_OFFSETS_MS.length;
    private static final OcrWindowRegion TRACKER_DEST_HINT_REGION =
            new OcrWindowRegion(350, 370, 679, 463);
    private static final int TRACKER_DEST_HINT_ARRIVAL_TOLERANCE = 12;
    private static final int RETURN_ITEM_VERIFY_ATTEMPTS = 2;
    private static final long RETURN_VERIFY_DELAY_MS = 500L;
    private static final long TASK_TURN_HANDOFF_DELAY_MS = 900L;
    private static final long MAINTENANCE_BROADCAST_HANDOFF_PER_WINDOW_MS = 2_000L;
    private static final long CHAINED_POST_BATTLE_RECOVERY_PER_MEMBER_MS = 2_200L;
    private static final long CHAINED_POST_BATTLE_RECOVERY_MAX_MS = 10_000L;
    private static final long PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS = 2_500L;
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
    private final DialogChoiceMemoryService dialogChoiceMemoryService;
    private final DialogService dialogService;
    private final AutoCombatService autoCombatService;
    private final BagService bagService;
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
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TaskTrackerPanelService taskTrackerPanelService;
    private int currentRoundNumber;
    private boolean currentRoundChainedCombatExpected;
    private int currentRoundChainedCombatContinueCount;
    private TaskTrackerPanelReadResult currentTrackerPanel;
    private volatile TrackerDestinationHint currentTrackerDestinationHint;
    private volatile long trackerDestinationHintRequestId;
    private List<TaskTrackerGreenLink> currentProbeSegments = List.of();
    private boolean[] currentProbeUsed = new boolean[0];
    private int[] currentProbeItemAttempts = new int[0];
    private int currentProbeIndex = -1;
    private long currentProbeTaskStartedAt;
    private long waitBattleStartedAt;
    private long waitBattleNextTrackerRetryAt;
    private boolean waitBattleSawCombat;
    private long enterBattleStartedAt;
    private long enterBattleNextRetryAt;
    private long lastHealPetMaintenanceAt;
    private long lastRepairEquipmentMaintenanceAt;
    private int consecutiveHealPetMaintenanceFailures;
    private int consecutiveRepairEquipmentMaintenanceFailures;
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
        /*
         * 五倍 can enter combat almost immediately after accepting the first task. Run the same
         * startup recovery boundary used by auto-battle/five-ring before the first accept flow so
         * low player or summon HP/MP is handled before navigation starts.
         */
        playerStateService.performStartupFirstAidCheck(context);
        playerStateService.ensureSheYaoXiangActiveForLeaderTask("wubei:startup", context);
        long maintenanceStartAt = botProperties.isXiuluoMaintenanceRunImmediatelyOnStart()
                ? 0L
                : System.currentTimeMillis();
        lastHealPetMaintenanceAt = maintenanceStartAt;
        lastRepairEquipmentMaintenanceAt = maintenanceStartAt;
        consecutiveHealPetMaintenanceFailures = 0;
        consecutiveRepairEquipmentMaintenanceFailures = 0;
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
        currentRoundNumber = round;
        currentRoundChainedCombatExpected = false;
        currentTrackerPanel = null;
        currentTrackerDestinationHint = null;
        resetProbeRuntime();
        currentProbeTaskStartedAt = 0L;
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
            TaskTransactionOutcome transaction;
            try {
                transaction = taskTransactionRunner.run(
                        "wubei:" + currentState.phase(),
                        TaskTransactionResult.READY_TO_CONTINUE,
                        TaskYieldPolicy.CONTINUE_CHAIN,
                        () -> {
                            WubeiStepOutcome outcome = runPhase(context, currentState);
                            phaseOutcome.set(outcome);
                            return outcome.transactionResult();
                        });
            } catch (RuntimeException e) {
                /*
                 * Business phase exceptions should be handled like a failed phase, so one stale
                 * screenshot/null hint does not kill the whole 五倍 task. Fatal JVM Errors still
                 * bubble through TaskTransactionRunner and remain hard failures.
                 */
                log.error("[wubei] phase exception; recover current round: phase={} recoveryCount={}",
                        currentState.phase(), currentState.recoveryCount(), e);
                roundState = recoverRoundAfterFailure(context, currentState,
                        WubeiStepOutcome.failed(currentState, "phase exception: " + e.getClass().getSimpleName()));
                phaseLoopGuard = 0;
                continue;
            }

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
        taskMaintenanceService.closeTeamMaintenanceWindow(context, TASK_CODE, failedState.round(),
                "wubei:recover-round");
        if (failedState.recoveryCount() >= 3) {
            log.error("[wubei] recovery limit reached: phase={} message={}", failedState.phase(), outcome.message());
            return failedState.next(WubeiPhase.FAILED, "recovery-limit");
        }
        taskTransactionRunner.forceReleaseTurn("wubei-recover:" + failedState.phase());
        uiCleanerService.cleanUpAll();
        TaskSleep.sleepOrStop(context, 800L, "Wubei task interrupted");
        return failedState.recoverTo(WubeiPhase.ROUTE_TO_MAIN_TASK, "recover-from-" + failedState.phase());
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
        Optional<WindowDialogSnapshot> snapshot = windowTaskContextHolder.rawCurrent()
                .flatMap(WindowRuntimeContext::getVisibleDialogSnapshot);
        if (snapshot.isEmpty() || snapshot.get().getType() == DialogType.NONE) {
            return false;
        }
        log.info("[wubei] leader pathing summon maintenance skipped: runner dialog visible round={} phase={} source={} type={} dialogSource={}",
                nextState.round(), nextState.phase(), nextState.source(),
                snapshot.get().getType(), snapshot.get().getSource());
        return true;
    }

    private WubeiStepOutcome timeoutProbeTaskBeforeBattleIfNeeded(TaskExecutionContext context,
                                                                  WubeiRoundContext state) {
        if (currentProbeTaskStartedAt <= 0L) {
            return null;
        }
        long elapsedMs = System.currentTimeMillis() - currentProbeTaskStartedAt;
        if (elapsedMs < PROBE_ENTER_BATTLE_TIMEOUT_MS) {
            return null;
        }

        log.warn("[wubei] probe task exceeded enter-battle timeout; return home and reaccept: "
                        + "round={} phase={} elapsedMs={} timeoutMs={} yellow='{}' probeUsed={} attempts={}",
                state.round(), state.phase(), elapsedMs, PROBE_ENTER_BATTLE_TIMEOUT_MS,
                currentTrackerPanel == null ? null : currentTrackerPanel.getYellowText(),
                probeUsedSummary(), probeAttemptSummary());
        currentProbeTaskStartedAt = 0L;
        currentTrackerPanel = null;
        currentTrackerDestinationHint = null;
        resetProbeRuntime();
        resetEnterBattleRuntime();
        resetWaitBattleRuntime();
        taskMaintenanceService.closeTeamMaintenanceWindow(context, TASK_CODE, state.round(),
                "wubei:probe-enter-battle-timeout");
        if (!useReturnItemAndVerifyStartMap(context, "probe-enter-battle-timeout")) {
            return WubeiStepOutcome.failed(state, "probe enter battle timeout return home failed");
        }
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "probe-enter-battle-timeout-reaccept"),
                "probe enter battle timeout; returned home and reaccept");
    }

    private WubeiStepOutcome runPhase(TaskExecutionContext context, WubeiRoundContext state) {
        WubeiStepOutcome probeTimeout = timeoutProbeTaskBeforeBattleIfNeeded(context, state);
        if (probeTimeout != null) {
            return probeTimeout;
        }
        return switch (state.phase()) {
            case HOT_START_DETECT -> runHotStartDetectPhase(state);
            case ROUTE_TO_MAIN_TASK -> runRouteToNPC(context, state);
            case ACCEPT_TASK -> runAcceptTaskPhase(context, state);
            case READ_TRACKER -> runReadTrackerPhase(context, state);
            case AFTER_ACCEPT_MAINTENANCE_CHECK -> runAfterAcceptMaintenanceCheck(context, state);
            case BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK -> runBeforeTrackerPathingMaintenanceCheck(context, state);
            case TRACKER_PATHING -> runTrackerPathingPhase(context, state);
            case RESOLVE_AFTER_PATHING -> runResolveAfterPathingPhase(context, state);
            case ENTER_BATTLE -> runEnterBattlePhase(context, state);
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
        currentTrackerPanel = resolveTrackerPanelWithAnchorRecovery(state);
        if (currentTrackerPanel.isFound()) {
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.READ_TRACKER, "hot-start-active-task"),
                    "accepted task found from tracker panel");
        }
        return WubeiStepOutcome.continueTo(
            state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "hot-start-no-active-task"),
            "no accepted task found");
    }

    private WubeiStepOutcome runAfterAcceptMaintenanceCheck(TaskExecutionContext context, WubeiRoundContext state) {
        /*
         * Match 修罗's split maintenance model: 医宝宝 belongs right after accepting/reading
         * the objective, while repair belongs just before the tracker route starts.
         */
        WubeiStepOutcome healPetOutcome = triggerHealPetBroadcastBeforeTracker(context, state);
        if (healPetOutcome != null) {
            return healPetOutcome;
        }
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK, "after-accept-maintenance-checked"),
                "after-accept maintenance checked");
    }

    private WubeiStepOutcome runBeforeTrackerPathingMaintenanceCheck(TaskExecutionContext context,
                                                                     WubeiRoundContext state) {
        WubeiStepOutcome repairOutcome = triggerRepairEquipmentBroadcastBeforeTracker(context, state);
        if (repairOutcome != null) {
            return repairOutcome;
        }
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.TRACKER_PATHING, "before-tracker-maintenance-checked"),
                "before tracker maintenance checked");
    }

    private WubeiStepOutcome triggerHealPetBroadcastBeforeTracker(TaskExecutionContext context, WubeiRoundContext state) {
        if (!isHealPetMaintenanceDue()) {
            log.info("[wubei] skip heal-pet hook: cooldown not due intervalMs={} lastAt={}",
                    botProperties.getXiuluoHealPetMaintenanceIntervalMs(), lastHealPetMaintenanceAt);
            return null;
        }
        if (consecutiveHealPetMaintenanceFailures >= MAX_CONSECUTIVE_MAINTENANCE_HOOK_FAILURES) {
            log.warn("[wubei] skip heal-pet hook: consecutive failures reached limit failures={} limit={}",
                    consecutiveHealPetMaintenanceFailures, MAX_CONSECUTIVE_MAINTENANCE_HOOK_FAILURES);
            return null;
        }

        WubeiStepOutcome outcome = triggerMaintenanceBroadcastBeforeTracker(
                context, state, HEAL_PET_NPC, BUSINESS_ACTION_HEAL_PET,
                "heal-pet", "wubei:heal-pet-npc",
                WubeiPhase.BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK);
        if (outcome != null && outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
            lastHealPetMaintenanceAt = System.currentTimeMillis();
            consecutiveHealPetMaintenanceFailures = 0;
        } else if (outcome == null) {
            consecutiveHealPetMaintenanceFailures++;
            log.warn("[wubei] heal-pet hook failed this round: consecutiveFailures={} limit={}",
                    consecutiveHealPetMaintenanceFailures, MAX_CONSECUTIVE_MAINTENANCE_HOOK_FAILURES);
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
        if (consecutiveRepairEquipmentMaintenanceFailures >= MAX_CONSECUTIVE_MAINTENANCE_HOOK_FAILURES) {
            log.warn("[wubei] skip repair-equipment hook: consecutive failures reached limit failures={} limit={}",
                    consecutiveRepairEquipmentMaintenanceFailures, MAX_CONSECUTIVE_MAINTENANCE_HOOK_FAILURES);
            return null;
        }

        WubeiStepOutcome outcome = triggerMaintenanceBroadcastBeforeTracker(
                context, state, REPAIR_EQUIPMENT_NPC, BUSINESS_ACTION_REPAIR_EQUIPMENT,
                "repair-equipment", "wubei:repair-equipment-npc",
                WubeiPhase.TRACKER_PATHING);
        if (outcome != null && outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
            lastRepairEquipmentMaintenanceAt = System.currentTimeMillis();
            consecutiveRepairEquipmentMaintenanceFailures = 0;
        } else if (outcome == null) {
            consecutiveRepairEquipmentMaintenanceFailures++;
            log.warn("[wubei] repair-equipment hook failed this round: consecutiveFailures={} limit={}",
                    consecutiveRepairEquipmentMaintenanceFailures, MAX_CONSECUTIVE_MAINTENANCE_HOOK_FAILURES);
        }
        return outcome;
    }

    private WubeiStepOutcome triggerMaintenanceBroadcastBeforeTracker(TaskExecutionContext context,
                                                                      WubeiRoundContext state,
                                                                      NpcTarget npc,
                                                                      String expectedAction,
                                                                      String hookName,
                                                                      String navigationSource,
                                                                      WubeiPhase nextPhaseAfterMaintenance) {
        WubeiRoundContext activeState = state;
        for (int attempt = 1; attempt <= MAX_MAINTENANCE_HOOK_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            WubeiStepOutcome pendingPathing = continueIfMaintenanceNavigationStillPathing(
                    context, activeState, hookName);
            if (pendingPathing != null) {
                return pendingPathing;
            }
            if (activeState.waitingPathing()) {
                activeState = activeState.clearPathingWait(hookName + "-navigation-arrived:" + state.phase());
            }
            log.info("[wubei] {} hook attempt: attempt={}/{} npc={} map={} coord=({}, {})",
                    hookName, attempt, MAX_MAINTENANCE_HOOK_ATTEMPTS, npc.getName(),
                    npc.getMapName(), npc.getX(), npc.getY());
            NavigationResult nav = navigationService.navigateToNPC(NavigationRequest.builder()
                    .targetMapName(npc.getMapName())
                    .targetX(npc.getX())
                    .targetY(npc.getY())
                    .targetName(npc.getName())
                    .source(navigationSource)
                    .build());
            if (nav.getStatus() == NavigationResultStatus.PATHING_STARTED) {
                return WubeiStepOutcome.pathingStarted(
                        activeState.waitForPathing(hookName + "-npc-pathing-started"),
                        hookName + " NPC pathing started");
            }
            if (nav.getStatus() == NavigationResultStatus.STOPPED) {
                return WubeiStepOutcome.stopped(activeState, hookName + " navigation stopped");
            }
            if (!nav.success()) {
                log.warn("[wubei] {} navigation failed: attempt={} status={} message={}",
                        hookName, attempt, nav.getStatus(), nav.getMessage());
                continue;
            }
            if (!npcClickService.clickNpcSmart(npc.toClickRequest(gameContext.getMe(), TaskType.WUBEI))) {
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
                    activeState.next(nextPhaseAfterMaintenance, hookName + "-broadcast-handled"),
                    hookName + " broadcast handled; recheck before tracker pathing");
            }
            log.warn("[wubei] {} broadcast not handled: attempt={} expectedAction={} status={} message={}",
                    hookName, attempt, expectedAction,
                    maintenanceResult.getStatus(), maintenanceResult.getMessage());
        }

        log.warn("[wubei] {} hook skipped after {} attempts; continue main task",
                hookName, MAX_MAINTENANCE_HOOK_ATTEMPTS);
        return null;
    }

    private WubeiStepOutcome continueIfMaintenanceNavigationStillPathing(TaskExecutionContext context,
                                                                         WubeiRoundContext state,
                                                                         String hookName) {
        if (!state.waitingPathing()) {
            return null;
        }

        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
        if (movementState == GameStateUtil.MovementState.MOVING
                || movementState == GameStateUtil.MovementState.PATHING_ACTIVE) {
            /*
             * The previous maintenance NPC navigation already submitted a click. While the runner
             * still sees pathing, this phase must yield instead of re-submitting maintenance or
             * falling through into the other maintenance hook.
             */
            log.info("[wubei] {} maintenance navigation still pathing: phase={} state={}",
                    hookName, state.phase(), movementState);
            return WubeiStepOutcome.pathingStarted(state, hookName + " maintenance navigation still pathing");
        }
        if (movementState == GameStateUtil.MovementState.MAYBE_MOVING) {
            log.info("[wubei] {} maintenance navigation weak movement ignored: phase={} state={}",
                    hookName, state.phase(), movementState);
        }
        log.info("[wubei] {} maintenance navigation wait ended: phase={} state={}",
                hookName, state.phase(), movementState);
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
        /*
         * READ_TRACKER is the authoritative snapshot boundary. The left task tracker can change
         * after accepting/rerolling/finishing chained combat, so do not reuse an older cached
         * currentTrackerPanel here. Later phases may reuse this freshly captured snapshot.
         */
        currentTrackerPanel = resolveTrackerPanelWithAnchorRecovery(context);
        if (!currentTrackerPanel.isFound()) {
            return WubeiStepOutcome.failed(state, "tracker anchor not found");
        }
        if (containsDarkThunder(currentTrackerPanel.getYellowText())) {
            log.info("[wubei] dark-thunder task detected; reroll by accepting task again: yellow='{}'",
                    currentTrackerPanel.getYellowText());
            /*
             * 暗雷怪会回到接任务 NPC 重抽任务。这里必须丢掉本次左侧追踪快照，
             * 否则下一次接完任务后 READ_TRACKER 会复用旧的“暗雷怪”结果。
             */
            currentTrackerPanel = null;
            TaskSleep.sleepOrStop(context, 4_000L, "Wubei task interrupted");
            return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "dark-thunder-reroll"),
                "dark thunder reroll");
        }
        currentRoundChainedCombatExpected = containsChainedCombatTarget(currentTrackerPanel.getYellowText());
        currentRoundChainedCombatContinueCount = 0;
        if (currentTrackerPanel.isProbeObjective() || containsProbeTask(currentTrackerPanel.getYellowText())) {
            currentProbeTaskStartedAt = System.currentTimeMillis();
            log.info("[wubei] probe task timer started: round={} timeoutMs={} yellow='{}'",
                    state.round(), PROBE_ENTER_BATTLE_TIMEOUT_MS, currentTrackerPanel.getYellowText());
        } else {
            currentProbeTaskStartedAt = 0L;
        }
        log.info("[wubei] tracker snapshot ready: yellow='{}' probe={} chainedCombatExpected={}",
                currentTrackerPanel.getYellowText(),
                currentTrackerPanel.isProbeObjective(),
                currentRoundChainedCombatExpected);
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.AFTER_ACCEPT_MAINTENANCE_CHECK, "tracker-ready"),
                "tracker objective ready");
    }

    private WubeiStepOutcome runTrackerPathingPhase(TaskExecutionContext context, WubeiRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        if (currentTrackerPanel == null || !currentTrackerPanel.isFound()) {
            return WubeiStepOutcome.failed(state, "tracker pathing failed");
        }
        if (containsProbeTask(currentTrackerPanel.getYellowText()) || currentTrackerPanel.isProbeObjective()) {
            if (!startProbeTrackerPathing(context, currentTrackerPanel)) {
                return WubeiStepOutcome.failed(state, "probe tracker pathing failed");
            }
            return WubeiStepOutcome.pathingStarted(
                    state.next(WubeiPhase.RESOLVE_AFTER_PATHING, "probe-pathing-started"),
                    "probe tracker pathing started");
        }
        if (!triggerCombatTrackerPathing(context, currentTrackerPanel)) {
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
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        WindowPathingSnapshot snapshot = currentWindowPathingSnapshot();
        if (!isCurrentTrackerPathingSnapshot(snapshot)) {
            /*
             * 普通五倍绿字寻路的到达/停住信号由 WindowTaskRunner 后台刷新。这里不再
             * 现场 OCR 或像素判移动；如果暂时没有对应快照，就让下一轮 runner 继续刷新。
             */
            log.info("[wubei] tracker pathing resolve waits for runner snapshot: hasSnapshot={} state={} intent={}",
                    snapshot != null, snapshot == null ? null : snapshot.getState(),
                    snapshot == null ? null : snapshot.getIntent());
            return WubeiStepOutcome.sharedState(state, "waiting tracker runner pathing snapshot");
        }
        WindowPathingState pathingState = snapshot.getState();
        log.info("[wubei] tracker pathing snapshot consumed: state={} current={}({}, {}) probeInProgress={} message={}",
                pathingState, snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                snapshot.isProbeInProgress(), snapshot.getMessage());
        if (pathingState == WindowPathingState.ACTIVE || snapshot.isProbeInProgress()) {
            return WubeiStepOutcome.sharedState(state, "tracker runner pathing still active");
        }
        if (pathingState == WindowPathingState.UNKNOWN) {
            log.warn("[wubei] tracker runner pathing unknown; continue phase recovery instead of yielding forever: current={}({}, {}) message={}",
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(), snapshot.getMessage());
        }
        if (pathingState == WindowPathingState.ARRIVED
                || pathingState == WindowPathingState.STOPPED_AWAY) {
            WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
            PreparedDialogAction preparedRoute = runtime != null && pathingState == WindowPathingState.STOPPED_AWAY
                    ? runtime.freshPreparedRouteActionForPathingTerminal(
                    snapshot, PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS)
                    : null;
            if (preparedRoute != null) {
                WindowPathingIntent activeIntent = runtime.getActivePathingIntent().orElse(null);
                long verifiedAgeMs = Math.max(0L, System.currentTimeMillis() - preparedRoute.getLastVerifiedAtMs());
                log.info("[wubei] pathing terminal clear delayed because prepared route dialog is ready: state={} target={} actionIntentId={} activeIntentId={} verifiedAgeMs={}",
                        pathingState, preparedRoute.getTargetKeyword(), preparedRoute.getIntentId(),
                        activeIntent == null ? null : activeIntent.getIntentId(), verifiedAgeMs);
            } else {
                clearCurrentPathingSignal("wubei consumed tracker pathing terminal snapshot: " + pathingState);
            }
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.ENTER_BATTLE, "tracker-pathing-terminal-" + pathingState),
                    "tracker pathing terminal; resolve combat entry");
        }
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.ENTER_BATTLE, "tracker-pathing-no-active-state"),
                "tracker pathing no longer active; resolve combat entry");
    }

    private WubeiStepOutcome runEnterBattlePhase(TaskExecutionContext context, WubeiRoundContext state) {
        /*
         * 五倍的三技能窗口只允许存在于“队长已点左侧任务追踪寻路、尚未开始打怪交互”
         * 这一段。进入打怪处理后不再依赖地图/坐标读数判断关闭时机，直接关 gate，
         * 避免队员在开打弹窗、显形镜或黄袍怪续战阶段抢输入。
         */
        taskMaintenanceService.closeTeamMaintenanceWindow(context, TASK_CODE, state.round(),
                "wubei:enter-battle");
        WubeiStepOutcome outcome = tickEnterBattle(context, state);
        if (outcome.transactionResult() == TaskTransactionResult.FAILED
                || outcome.transactionResult() == TaskTransactionResult.STOPPED
                || outcome.nextState().phase() != WubeiPhase.ENTER_BATTLE) {
            resetEnterBattleRuntime();
        }
        return outcome;
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

    private WubeiStepOutcome runRouteToNPC(TaskExecutionContext context, WubeiRoundContext state){
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");

        WubeiStepOutcome activePathing = waitForAcceptNpcPathingIfStillActive(state);
        if (activePathing != null) {
            return activePathing;
        }

        NavigationResult nav = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(START_MAP_NAME)
                .targetX(ACCEPT_NPC_X)
                .targetY(ACCEPT_NPC_Y)
                .targetName(ACCEPT_NPC_NAME)
                .arrivalTolerance(ACCEPT_NPC_DIRECT_CLICK_DISTANCE)
                .source("wubei:accept-npc")
                .build());

        if (nav.getStatus() == NavigationResultStatus.PATHING_STARTED) {
            return WubeiStepOutcome.pathingStarted(
                state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "accept-npc-pathing-started"),
                "accept NPC pathing started");
        }

        if (nav.getStatus() == NavigationResultStatus.DIALOG_PREPARING) {
            return WubeiStepOutcome.sharedState(
                state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "accept-npc-dialog-preparing"),
                "accept NPC dialog preparing");
        }

        if (nav.getStatus() != NavigationResultStatus.ARRIVED) {
            log.warn("[wubei] accept NPC navigation not arrived: status={} message={}",
                nav.getStatus(), nav.getMessage());
            return WubeiStepOutcome.failed(
                state,
                "accept NPC navigation not arrived: " + nav.getStatus() + " " + nav.getMessage());
        }


        TaskSleep.sleepOrStop(context, TRACKER_REFRESH_AFTER_ACCEPT_MS, "Wubei task interrupted");

        return WubeiStepOutcome.continueTo(
            state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_BEFORE_ACCEPT_SOURCE),
            "accept NPC arrived; wait team return before accepting task");
    }

    private WubeiStepOutcome waitForAcceptNpcPathingIfStillActive(WubeiRoundContext state) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return null;
        }
        WindowPathingSnapshot snapshot = runtime.getPathingSnapshot();
        if (snapshot == null) {
            return null;
        }
        WindowPathingIntent intent = snapshot.getIntent();
        if (intent == null || !gameStateUtil.isSameMapName(intent.getTargetMapName(), START_MAP_NAME)) {
            return null;
        }
        boolean stillWorking = snapshot.getState() == WindowPathingState.ACTIVE
                || snapshot.isProbeInProgress();
        if (!stillWorking) {
            return null;
        }
        PreparedDialogAction preparedRoute = runtime.getPreparedDialogAction();
        if (preparedRoute != null
                && preparedRoute.matches(DialogOperation.ROUTE_TRANSFER, START_MAP_NAME)
                && preparedRoute.verifiedWithin(System.currentTimeMillis(), PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS)) {
            /*
             * A route dialog can be ready while the previous world-map pathing intent is still
             * ACTIVE. Let NavigationService consume the verified prepared click instead of waiting
             * for the watcher to age into STOPPED_AWAY.
             */
            log.info("[wubei] accept NPC pathing gate released for prepared route dialog: target={} matched={} click=({}, {}) verifiedAgeMs={}",
                    START_MAP_NAME, preparedRoute.getMatchedText(),
                    preparedRoute.getAbsoluteX(), preparedRoute.getAbsoluteY(),
                    Math.max(0L, System.currentTimeMillis() - preparedRoute.getLastVerifiedAtMs()));
            return null;
        }

        /*
         * The accept-NPC route starts with a world-map click and then the watcher owns arrival.
         * Re-entering this phase while the same target is still ACTIVE must only yield; otherwise
         * 五倍 can submit the same world-map search again before the previous route finishes.
         */
        log.info("[wubei] accept NPC pathing still active; skip duplicate navigation: state={} probe={} target={} current={}({}, {}) source={} message={}",
                snapshot.getState(), snapshot.isProbeInProgress(), intent.getTargetMapName(),
                snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                intent.getSource(), snapshot.getMessage());
        return WubeiStepOutcome.pathingStarted(
                state.retrySamePhase("accept-npc-pathing-wait"),
                "accept NPC pathing still active");
    }

    private WubeiStepOutcome runAcceptTaskPhase(TaskExecutionContext context, WubeiRoundContext state) {
        DialogResult result = tryConsumePreparedWubeiDialog(
                DialogOperation.WUBEI_ACCEPT_TASK,
                state.source() + ":prepared-accept");
        if (result == null) {
            boolean clicked = npcClickService.clickNpcSmart(ACCEPT_NPC.toClickRequest(gameContext.getMe(), TaskType.WUBEI));
            if (!clicked) {
                log.warn("[wubei] accept NPC smart-click failed; wait for runner prepared accept dialog");
            }
            result = tryConsumePreparedWubeiDialog(
                    DialogOperation.WUBEI_ACCEPT_TASK,
                    state.source() + ":prepared-accept-after-npc");
        }
        if (result == null) {
            /*
             * Runner owns the expensive template/OCR preparation. After opening the NPC dialog,
             * this phase yields briefly and consumes the prepared action on the next loop instead
             * of scanning and clicking the dialog inside the task thread.
             */
            log.info("[wubei] accept option not prepared yet; wait for runner preparation: source={}",
                    state.source());
            return WubeiStepOutcome.sharedState(
                    state.retrySamePhase("accept-dialog-wait-prepared"),
                    "accept dialog waiting for runner preparation");
        }
        if (result.getStatus() == DialogResultStatus.GREEN_TEMPLATE_CLICKED
                && OPTION_ACCEPT_TASK.equals(result.getActionKey())
                && result.getRelativeX() != null
                && result.getRelativeY() != null) {
            dialogChoiceMemoryService.recordSuccess(
                    TASK_CODE, "acceptTask", ACCEPT_NPC_NAME,
                    START_MAP_NAME, ACCEPT_NPC_X, ACCEPT_NPC_Y, START_MAP_NAME,
                    result.getRelativeX(), result.getRelativeY(), result.getMatchedText(),
                    state.source() + ":prepared");
        }
        boolean accepted = OPTION_ACCEPT_TASK.equals(result.getActionKey())
                && (result.getStatus() == DialogResultStatus.GREEN_TEMPLATE_CLICKED
                || result.getStatus() == DialogResultStatus.OPTION_KEYWORD_CLICKED);
        log.info("[wubei] accept option result: status={} action={} clicked={}",
                result.getStatus(), result.getActionKey(), result.isClicked());
        if (!accepted) {
            return WubeiStepOutcome.failed(state, "accept NPC click or option failed");
        }
        /*
         * 接任务成功会刷新左侧任务追踪内容。READ_TRACKER 必须重新截图，
         * 不能沿用热启动或上一轮/上一次重抽留下的 currentTrackerPanel。
         * 游戏左侧任务追踪不是随点击瞬间刷新，先等一小段时间再读，避免把
         * 接任务前的旧追踪面板当成本轮任务。
         */
        currentTrackerPanel = null;
        log.info("[wubei] accept task clicked; waiting tracker refresh before READ_TRACKER: waitMs={} source={}",
                TRACKER_REFRESH_AFTER_ACCEPT_MS, state.source());
        TaskSleep.sleepOrStop(context, TRACKER_REFRESH_AFTER_ACCEPT_MS, "Wubei task interrupted");
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.READ_TRACKER, "task-accepted"),
                "task accepted");
    }

    private DialogResult tryConsumePreparedWubeiDialog(DialogOperation operation, String source) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            log.debug("[wubei] consume prepared dialog skipped: operation={} source={} reason=no-window-runtime",
                    operation, source);
            return null;
        }
        PreparedDialogAction action = runtime.consumePreparedDialogAction(operation, null, source);
        if (action == null) {
            return null;
        }
        if (!action.isClickRequired()) {
            log.info("[wubei] consumed prepared dialog signal: operation={} target={} matched={} source={} actionSource={}",
                    operation, action.getTargetKeyword(), action.getMatchedText(), source, action.getSource());
            return DialogResult.statusBuilder(DialogResultStatus.WHITE_TEMPLATE_VISIBLE, action.getDialogType())
                    .actionKey(action.getTargetKeyword())
                    .matchedText(action.getMatchedText())
                    .preparedAction(action)
                    .relativeX(action.getRelativeX())
                    .relativeY(action.getRelativeY())
                    .absoluteX(action.getAbsoluteX())
                    .absoluteY(action.getAbsoluteY())
                    .build();
        }

        boolean clicked = inputSequences.moveAndClickLeft(
                "wubei:prepared-dialog:" + operation + ":" + action.getTargetKeyword(),
                action.getAbsoluteX(),
                action.getAbsoluteY(),
                80,
                150);
        DialogResultStatus status = clicked
                ? DialogResultStatus.GREEN_TEMPLATE_CLICKED
                : DialogResultStatus.FAILED;
        log.info("[wubei] consumed prepared dialog click: operation={} target={} matched={} clicked={} click=({}, {}) source={} actionSource={}",
                operation, action.getTargetKeyword(), action.getMatchedText(), clicked,
                action.getAbsoluteX(), action.getAbsoluteY(), source, action.getSource());
        return DialogResult.statusBuilder(status, action.getDialogType())
                .actionKey(action.getTargetKeyword())
                .matchedText(action.getMatchedText())
                .preparedAction(action)
                .relativeX(action.getRelativeX())
                .relativeY(action.getRelativeY())
                .absoluteX(action.getAbsoluteX())
                .absoluteY(action.getAbsoluteY())
                .build();
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
        if (currentTrackerPanel == null || currentTrackerPanel.getYellowText() == null) {
            return "";
        }
        String normalized = currentTrackerPanel.getYellowText().replace('丨', '|').replaceAll("\\s+", "");
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

    private TaskTrackerPanelReadResult resolveTrackerPanelWithAnchorRecovery(TaskExecutionContext context) {
        return resolveTrackerPanelWithAnchorRecovery(context, null);
    }

    private TaskTrackerPanelReadResult resolveTrackerPanelWithAnchorRecovery(WubeiRoundContext state) {
        return resolveTrackerPanelWithAnchorRecovery(null, state);
    }

    /*
     * 五倍推进只认左侧任务追踪 title。Auto+Q 任务面板即使命中“五倍”，也不能提供
     * 可点击的追踪绿字；如果这里找不到 title，应回到接任务流程，而不是靠任务面板
     * 判定 active 后继续停在不可推进的状态。
     */
    private TaskTrackerPanelReadResult resolveTrackerPanelWithAnchorRecovery(TaskExecutionContext context,
                                                                             WubeiRoundContext state) {
        for (int attempt = 1; attempt <= MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS; attempt++) {
            if (context != null) {
                TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            }

            String source = state == null
                    ? "wubei-attempt-" + attempt
                    : "wubei-" + state.phase() + "-attempt-" + attempt;
            TaskTrackerPanelReadResult result = taskTrackerPanelService.readWubeiTrackerPanel(source);
            if (result.isFound()) {
                return result;
            }

            log.warn("[wubei] tracker panel title missed: attempt={}/{} source={}",
                attempt, MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS, source);
            if (attempt < MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS) {
                TaskSleep.sleepOrStop(context, TRACKER_REFRESH_RETRY_INTERVAL_MS, "Wubei task interrupted");
            }
        }

        log.warn("[wubei] tracker title recovery exhausted: attempts={}",
            MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS);
        return TaskTrackerPanelReadResult.empty();
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

    private boolean startProbeTrackerPathing(TaskExecutionContext context, TaskTrackerPanelReadResult panel) {
        initializeProbeRuntimeIfNeeded(panel);
        int nextIndex = nextProbeIndexToPath();
        if (nextIndex < 0) {
            log.warn("[wubei] probe objective has no remaining green segment to path: used={} attempts={}",
                    probeUsedSummary(), probeAttemptSummary());
            return false;
        }
        currentProbeIndex = nextIndex;
        String label = probeLabel(nextIndex);
        TaskTrackerGreenLink segment = currentProbeSegments.get(nextIndex);
        currentTrackerDestinationHint = null;
        log.info("[wubei] probe-objective pathing start: label={} index={}/{} used={} attempts={} segment={}",
                label, nextIndex + 1, currentProbeSegments.size(), probeUsedSummary(), probeAttemptSummary(), segment);
        return clickTaskTrackerGreen(context, segment, label, 1);
    }

    private WubeiStepOutcome resolveProbeAfterPathing(TaskExecutionContext context, WubeiRoundContext state) {
        int index = currentProbeIndex;
        String label = probeLabel(index);
        WindowPathingSnapshot snapshot = currentWindowPathingSnapshot();
        if (isCurrentTrackerPathingSnapshot(snapshot)) {
            log.info("[wubei] resolve probe after runner pathing: label={} snapshotState={} probe={} current={}({}, {}) used={} attempts={} hint={}",
                    label, snapshot.getState(), snapshot.isProbeInProgress(),
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    probeUsedSummary(), probeAttemptSummary(), currentTrackerDestinationHint);
            if (snapshot.getState() == WindowPathingState.ACTIVE || snapshot.isProbeInProgress()) {
                return WubeiStepOutcome.sharedState(state, "probe runner pathing still active");
            }
            if (snapshot.getState() == WindowPathingState.UNKNOWN) {
                log.warn("[wubei] probe runner pathing unknown; continue probe recovery instead of yielding forever: label={} current={}({}, {}) message={}",
                        label, snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(), snapshot.getMessage());
            }
        }

        if (!useProbeItemWithRuntimeRecord(context, index, label)) {
            if (probeCanRetryItem(index)) {
                log.warn("[wubei] probe item use failed; retry same probe point later: label={} used={} attempts={}",
                        label, probeUsedSummary(), probeAttemptSummary());
                return WubeiStepOutcome.sharedState(state, "probe item use retry");
            }
            return WubeiStepOutcome.failed(state, "probe item use failed");
        }

        DialogResult probeStory = inspectProbeStoryOnce(label);
        if (isProbeTargetReadyStoryVisible(label, probeStory)) {
            markProbeResolved(index);
            log.info("[wubei] probe target-ready story matched; smart-click spawned target: label={}", label);
            if (tryClickProbeSpawnedTarget(context, label, true)) {
                return WubeiStepOutcome.continueTo(
                        state.next(WubeiPhase.WAIT_BATTLE_FINISH, "probe-tooltip-clicked"),
                        "probe target tooltip clicked");
            }
            return WubeiStepOutcome.failed(state, "probe target story visible but target click failed");
        }

        if (isProbeWrongPositionStoryVisible(label, probeStory)) {
            rollbackProbeItemAttempt(index, label, "wrong-position-story");
            /*
             * “位置不对”只证明当前绿字还没真正到位，通常是移动停稳误判导致提前使用显形镜。
             * 这里必须保留 currentProbeIndex，不清 story、不切下一条绿字，让同一条绿字重新寻路。
             */
            log.warn("[wubei] probe item used at wrong position; retry current probe pathing: label={} "
                            + "storyStatus={} storyAction={} storyClicked={} used={} attempts={}",
                    label, probeStory.getStatus(), probeStory.getActionKey(), probeStory.isClicked(),
                    probeUsedSummary(), probeAttemptSummary());
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

//        if (probeCanRetryItem(index)) {
//            log.warn("[wubei] probe item used but target-ready story not matched; retry same probe point: label={} used={} attempts={}",
//                    label, probeUsedSummary(), probeAttemptSummary());
//            return WubeiStepOutcome.sharedState(state, "probe story missing; retry item");
//        }

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

    private boolean triggerCombatTrackerPathing(TaskExecutionContext context, TaskTrackerPanelReadResult panel) {
        if (panel.getGreenLinks().isEmpty()) {
            log.warn("[wubei] no tracker green segment for combat pathing");
            return false;
        }
        TaskTrackerGreenLink segment = panel.getGreenLinks().get(0);
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

    private void initializeProbeRuntimeIfNeeded(TaskTrackerPanelReadResult panel) {
        if (isProbeRuntimeActive()) {
            return;
        }
        currentProbeSegments = List.copyOf(panel.getGreenLinks());
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
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        boolean used = bagService.findAndUseItemFromBack(BagService.MAIN_BAG, PROBE_ITEM_TEMPLATE, 5, context);
        log.info("[wubei] probe item used: label={} used={}", label, used);
        TaskSleep.sleepOrStop(context, 700L, "Wubei task interrupted");
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

    private boolean clickTaskTrackerGreen(TaskExecutionContext context, TaskTrackerGreenLink segment, String label, int attempt) {
        int baseX = segment.minX() + Math.min(18, Math.max(0, segment.width() / 3));
        int baseY = (segment.minY() + segment.maxY()) / 2;
        int randomRadiusX = Math.min(6, Math.max(2, segment.width() / 8));
        Point click = coordinateHelper.getRandomizedPoint(baseX, baseY, randomRadiusX, 3);
        String safeLabel = safeFileToken(label);
        DialogType dialogBeforeClick = dialogService.detectDialogTypeNoFocus(
                "wubei:tracker-green-before:" + safeLabel, false, 0);
        log.info("[wubei] click tracker green: label={} attempt={} segment={} click=({}, {}) dialogBefore={}",
                label, attempt, segment, click.x, click.y, dialogBeforeClick);
        boolean clicked = inputSequences.submitAndWait("wubei:tracker-green-click:" + label, List.of(
                InputAction.moveMouse(click.x, click.y),
                InputAction.sleep(120),
                InputAction.clickLeft(click.x, click.y, 300)
        ));
        boolean captureHint = clicked && shouldCaptureTrackerDestinationHint(label);
        boolean registerPathing = clicked && (label == null || !label.startsWith("chained-combat-"));
        if (captureHint) {
            scheduleTrackerDestinationHintCapture(context, label);
        }
        DialogType dialogAfterClick = dialogService.detectDialogTypeNoFocus(
                "wubei:tracker-green-after:" + safeLabel, false, 0);
        log.info("[wubei] tracker green click completed: label={} attempt={} clicked={} click=({}, {}) "
                        + "dialogBefore={} dialogAfter={} captureHint={} registerPathing={}",
                label, attempt, clicked, click.x, click.y, dialogBeforeClick, dialogAfterClick,
                captureHint, registerPathing);
        if (clicked) {
            String intentSource = "wubei:tracker-green-click:" + safeLabel;
            /*
             * 黄袍怪连战已经由任务追踪标题确认，后续绿字只是继续原地连战。
             * 它不是一次远距离寻路，所以不要注册 runner pathing intent；点完短等后
             * 直接进入 ENTER_BATTLE 去处理弹出的进战斗对话框。
             */
            if (registerPathing) {
                gameStateUtil.recordMovementIntent(intentSource);
                registerTrackerPathingIntent(intentSource);
                taskMaintenanceService.openTeamPathingMaintenanceWindow(context, TASK_CODE, currentRoundNumber,
                        "wubei:tracker-green-click:" + safeFileToken(label));
            } else {
                log.info("[wubei] tracker pathing intent skipped: label={} reason=chained-combat-continuation",
                        label);
            }
        }
        return clicked;
    }

    private boolean shouldCaptureTrackerDestinationHint(String label) {
        return "first-probe".equals(label) || "second-probe".equals(label);
    }

    private void scheduleTrackerDestinationHintCapture(TaskExecutionContext context, String label) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        long requestId = ++trackerDestinationHintRequestId;
        log.info("[wubei] destination hint capture scheduled async: label={} requestId={}", label, requestId);
        CompletableFuture.runAsync(() -> {
            try {
                windowTaskContextHolder.runWith(runtime, () -> {
                    Optional<TrackerDestinationHint> hint = captureTrackerDestinationHint(context, label, requestId);
                    hint.ifPresent(value -> {
                        if (trackerDestinationHintRequestId == requestId) {
                            currentTrackerDestinationHint = value;
                            log.info("[wubei] destination hint stored async: label={} requestId={} hint={}({}, {}) text='{}'",
                                    label, requestId, value.mapName(), value.x(), value.y(), value.rawText());
                        } else {
                            log.info("[wubei] destination hint ignored as stale: label={} requestId={} currentRequestId={} hint={}({}, {})",
                                    label, requestId, trackerDestinationHintRequestId,
                                    value.mapName(), value.x(), value.y());
                        }
                    });
                });
            } catch (Exception e) {
                log.warn("[wubei] destination hint async capture failed: label={} requestId={}", label, requestId, e);
            }
        });
    }

    private void registerTrackerPathingIntent(String intentSource) {
        windowTaskContextHolder.rawCurrent().ifPresent(runtime -> {
            WindowPathingIntent intent = WindowPathingIntent.builder()
                    .source(intentSource)
                    .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                    .targetMapName(null)
                    .targetX(null)
                    .targetY(null)
                    .tolerance(0)
                    .build();
            runtime.markPathingStarted(intent);
            log.info("[wubei] window pathing intent registered for tracker click: windowId={} source={}",
                    runtime.getWindowId(), intentSource);
        });
    }

    /*
     * 五倍绿字寻路会短暂弹出“正在自动寻路前往地图(x,y)”浮框。这里只做 HWND 截图、
     * 洗图和 OCR，不发送鼠标键盘输入；它可以和后续窗口输入调度解耦，但采样时机必须贴近
     * 绿字点击，否则浮框会自然消失。
     */
    private Optional<TrackerDestinationHint> captureTrackerDestinationHint(TaskExecutionContext context, String label, long requestId) {
        long startedAt = System.currentTimeMillis();
        List<TrackerDestinationHintCapture> captures = new ArrayList<>();
        CompletableFuture<Optional<TrackerDestinationHint>> firstParseFuture = null;
        log.info("[wubei] destination hint capture start: label={} offsetsMs={} region={}",
                label, List.of(1_500, 2_500, 3_500),
                TRACKER_DEST_HINT_REGION.toShortText());
        for (int sample = 1; sample <= TRACKER_DEST_HINT_SAMPLES; sample++) {
            long targetElapsedMs = TRACKER_DEST_HINT_CAPTURE_OFFSETS_MS[sample - 1];
            long targetAt = startedAt + targetElapsedMs;
            Optional<TrackerDestinationHint> earlyParsed = waitForDestinationHintOrTarget(
                    context, label, sample, targetElapsedMs, targetAt, firstParseFuture);
            if (earlyParsed.isPresent()) {
                return earlyParsed;
            }
            long waitMs = Math.max(0L, targetAt - System.currentTimeMillis());
            long afterDelayAt = System.currentTimeMillis();
            tracker.refreshWindowState();
            long afterRefreshAt = System.currentTimeMillis();
            String safeLabel = safeFileToken(label);
            String rawPath = windowScopedTempPath.resolve(
                    "wubei_tracker_destination_hint_" + safeLabel + "_r" + requestId + "_" + sample + "_raw.png");
            String yellowPath = windowScopedTempPath.resolve(
                    "wubei_tracker_destination_hint_" + safeLabel + "_r" + requestId + "_" + sample + "_yellow.png");
            OcrWindowRegion hintRegion = TRACKER_DEST_HINT_REGION;
            int left = tracker.getWindowBaseX() + hintRegion.x1();
            int top = tracker.getWindowBaseY() + hintRegion.y1();
            int right = tracker.getWindowBaseX() + hintRegion.x2();
            int bottom = tracker.getWindowBaseY() + hintRegion.y2();
            long captureStartedAt = System.currentTimeMillis();
            log.info("[wubei] destination hint capture attempt: label={} sample={} elapsedMs={} delayMs={} refreshMs={} rect=({}, {})-({}, {}) raw={}",
                    label, sample, captureStartedAt - startedAt,
                    Math.max(0L, waitMs), afterRefreshAt - afterDelayAt,
                    left, top, right, bottom, rawPath);
            if (!tracker.captureToFile("wubei-destination-hint:" + label, rawPath, left, top, right, bottom)) {
                log.warn("[wubei] destination hint capture failed: label={} sample={} elapsedMs={} delayMs={} refreshMs={} rect=({}, {})-({}, {})",
                        label, sample, System.currentTimeMillis() - startedAt,
                        Math.max(0L, waitMs), afterRefreshAt - afterDelayAt,
                        left, top, right, bottom);
                continue;
            }
            long afterCaptureAt = System.currentTimeMillis();
            long captureMs = afterCaptureAt - captureStartedAt;
            log.info("[wubei] destination hint capture saved: label={} sample={} elapsedMs={} captureMs={} raw={}",
                    label, sample, afterCaptureAt - startedAt, captureMs, rawPath);
            TrackerDestinationHintCapture capture = new TrackerDestinationHintCapture(
                    label, sample, hintRegion, rawPath, yellowPath, startedAt,
                    afterCaptureAt - startedAt, Math.max(0L, waitMs),
                    afterRefreshAt - afterDelayAt, captureMs);
            captures.add(capture);
            if (sample == 1) {
                firstParseFuture = CompletableFuture.supplyAsync(() -> parseTrackerDestinationHintCapture(capture));
                Optional<TrackerDestinationHint> parsed = readCompletedDestinationHint(label, firstParseFuture);
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
        }

        Optional<TrackerDestinationHint> parsed = waitForDestinationHintParsers(label, captures, firstParseFuture);
        if (parsed.isPresent()) {
            return parsed;
        }
        log.info("[wubei] destination hint capture exhausted: label={} elapsedMs={} samples={}",
                label, System.currentTimeMillis() - startedAt, TRACKER_DEST_HINT_SAMPLES);
        return Optional.empty();
    }

    private Optional<TrackerDestinationHint> waitForDestinationHintOrTarget(
            TaskExecutionContext context,
            String label,
            int sample,
            long targetElapsedMs,
            long targetAt,
            CompletableFuture<Optional<TrackerDestinationHint>> firstParseFuture) {
        long initialWaitMs = Math.max(0L, targetAt - System.currentTimeMillis());
        if (initialWaitMs > 0) {
            log.info("[wubei] destination hint sample scheduled: label={} sample={} targetElapsedMs={} waitMs={}",
                    label, sample, targetElapsedMs, initialWaitMs);
        }
        while (System.currentTimeMillis() < targetAt) {
            Optional<TrackerDestinationHint> parsed = readCompletedDestinationHint(label, firstParseFuture);
            if (parsed.isPresent()) {
                return parsed;
            }
            long waitMs = Math.min(80L, targetAt - System.currentTimeMillis());
            if (waitMs > 0) {
                TaskSleep.sleepOrStop(context, waitMs, "Wubei task interrupted");
            }
        }
        return readCompletedDestinationHint(label, firstParseFuture);
    }

    private Optional<TrackerDestinationHint> waitForDestinationHintParsers(
            String label,
            List<TrackerDestinationHintCapture> captures,
            CompletableFuture<Optional<TrackerDestinationHint>> firstParseFuture) {
        Optional<TrackerDestinationHint> first = readDestinationHintFuture(label, firstParseFuture);
        if (first.isPresent()) {
            return first;
        }
        for (int i = 1; i < captures.size(); i++) {
            Optional<TrackerDestinationHint> parsed = parseTrackerDestinationHintCapture(captures.get(i));
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    private Optional<TrackerDestinationHint> readDestinationHintFuture(
            String label,
            CompletableFuture<Optional<TrackerDestinationHint>> future) {
        if (future == null) {
            return Optional.empty();
        }
        try {
            return future.join();
        } catch (CompletionException e) {
            log.warn("[wubei] destination hint async parser failed: label={}", label, e);
            return Optional.empty();
        }
    }

    private Optional<TrackerDestinationHint> readCompletedDestinationHint(
            String label,
            CompletableFuture<Optional<TrackerDestinationHint>> future) {
        if (future == null || !future.isDone()) {
            return Optional.empty();
        }
        try {
            return future.join();
        } catch (CompletionException e) {
            log.warn("[wubei] destination hint async parser failed: label={}", label, e);
            return Optional.empty();
        }
    }

    private Optional<TrackerDestinationHint> parseTrackerDestinationHintCapture(TrackerDestinationHintCapture capture) {
        long parseStartedAt = System.currentTimeMillis();
        /*
         * 每张截图落盘后立刻启动解析；截图调度仍按固定时间点推进，避免第一张 OCR 慢时
         * 错过第二、第三张短生命周期浮框。
         */
        ImagePreprocessor.washYellowText(capture.rawPath(), capture.yellowPath());
        long afterWashAt = System.currentTimeMillis();
        List<OcrWordResult> words = textRecognizer.getAllTextResultsLocalOnly(capture.yellowPath());
        long afterOcrAt = System.currentTimeMillis();
        String text = words.stream().map(OcrWordResult::getText).collect(Collectors.joining(""));
        Optional<TrackerDestinationHint> parsed = parseTrackerDestinationHint(text);
        long afterParseAt = System.currentTimeMillis();
        if (parsed.isPresent()) {
            TrackerDestinationHint hint = parsed.get();
            log.info("[wubei] destination hint parsed: label={} sample={} region={} captureElapsedMs={} parseElapsedMs={} delayMs={} refreshMs={} captureMs={} washMs={} ocrMs={} parseMs={} map={} coord=({}, {}) text='{}' raw={} yellow={}",
                    capture.label(), capture.sample(), capture.region().toShortText(),
                    capture.captureElapsedMs(), afterParseAt - capture.startedAt(),
                    capture.delayMs(), capture.refreshMs(), capture.captureMs(),
                    afterWashAt - parseStartedAt, afterOcrAt - afterWashAt,
                    afterParseAt - afterOcrAt, hint.mapName(), hint.x(), hint.y(),
                    hint.rawText(), capture.rawPath(), capture.yellowPath());
            return parsed;
        }
        log.info("[wubei] destination hint not parsed: label={} sample={} region={} captureElapsedMs={} parseElapsedMs={} delayMs={} refreshMs={} captureMs={} washMs={} ocrMs={} parseMs={} text='{}' raw={} yellow={}",
                capture.label(), capture.sample(), capture.region().toShortText(),
                capture.captureElapsedMs(), afterParseAt - capture.startedAt(),
                capture.delayMs(), capture.refreshMs(), capture.captureMs(),
                afterWashAt - parseStartedAt, afterOcrAt - afterWashAt,
                afterParseAt - afterOcrAt, text, capture.rawPath(), capture.yellowPath());
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
            String rawMapName = matcher.group(1).trim();
            int x = Integer.parseInt(matcher.group(2));
            int y = Integer.parseInt(matcher.group(3));
            if (rawMapName.isEmpty()) {
                return Optional.empty();
            }
            String mapName = mapNameCanonicalizer.canonicalize(
                    rawMapName, "wubei:tracker-destination-hint-parse");
            if (mapName.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new TrackerDestinationHint(mapName, x, y, text));
        } catch (NumberFormatException e) {
            log.warn("[wubei] destination hint coordinate parse failed: text='{}'", text, e);
            return Optional.empty();
        }
    }

    private DialogResult inspectProbeStoryOnce(String label) {
        DialogResult result = tryConsumePreparedWubeiDialog(
                DialogOperation.WUBEI_PROBE_STORY,
                "wubei:probe-story:" + label);
        return result == null
                ? DialogResult.simple(DialogResultStatus.WHITE_TEMPLATE_NOT_FOUND, DialogType.NONE)
                : result;
    }

    private boolean isProbeTargetReadyStoryVisible(String label, DialogResult result) {
        boolean visible = STORY_PROBE_TARGET_READY.equals(result.getActionKey())
                && result.getStatus() == DialogResultStatus.WHITE_TEMPLATE_VISIBLE;
        log.info("[wubei] probe target-ready story check: label={} visible={} status={}",
                label, visible, result.getStatus());
        return visible;
    }

    private boolean isProbeWrongPositionStoryVisible(String label, DialogResult result) {
        boolean visible = STORY_PROBE_WRONG_POSITION.equals(result.getActionKey())
                && result.getStatus() == DialogResultStatus.WHITE_TEMPLATE_VISIBLE;
        log.info("[wubei] probe wrong-position story check: label={} visible={} status={}",
                label, visible, result.getStatus());
        return visible;
    }

    private boolean tryClickProbeSpawnedTarget(TaskExecutionContext context, String label, boolean storyConfirmed) {
        boolean clicked = tryClickTrackerCombatTargetSmart(
                context,
                label + (storyConfirmed ? "-story" : "-no-story"),
                storyConfirmed ? NpcTargetEvidence.CONFIRMED : NpcTargetEvidence.TENTATIVE);
        if (clicked || !storyConfirmed) {
            return clicked;
        }
        return tryDirectCombatFromTrackerHint(context, label + "-direct-combat");
    }

    private boolean tryClickTrackerCombatTargetSmart(TaskExecutionContext context,
                                                    String label,
                                                    NpcTargetEvidence targetEvidence) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        String targetName = resolveDirectCombatTargetName(label);
        if (targetName.isBlank()) {
            log.warn("[wubei] smart combat target click skipped: label={} reason=no-combat-target yellow='{}' hint={}",
                    label,
                    currentTrackerPanel == null ? null : currentTrackerPanel.getYellowText(),
                    currentTrackerDestinationHint);
            return false;
        }

        /*
         * Do not use the old all-screen task-tooltip shortcut here. Smart click constrains tooltip,
         * yellow-name, formula, and Ctrl probes to the current tracker destination, then the 五倍
         * dialog templates below prove we opened the correct battle dialog before clicking it.
         *
         * Probe story cleanup is owned by resolveProbeAfterPathing(), because only 五倍 knows
         * whether the current story is an expected probe signal or an unknown blocker.
         */
        boolean probeTarget = PROBE_TARGET_NPC_NAME.equals(targetName)
                || targetName.contains(CHAINED_COMBAT_TARGET_KEYWORD);
        TrackerDestinationHint hint = currentTrackerDestinationHint;
        String mapName = hint == null ? "" : hint.mapName();
        int mapX = hint == null ? -1 : hint.x();
        int mapY = hint == null ? -1 : hint.y();
        NpcClickRequest request = NpcClickRequest.builder()
                .player(gameContext.getMe())
                .mapName(mapName)
                .mapX(mapX)
                .mapY(mapY)
                .npcName(targetName)
                .tuneX(-10)
                .tuneY(0)
                .expectedDialogTemplatePaths(List.of(
                        ENTER_BATTLE_TEMPLATE,
                        ENTER_BATTLE_PROVE_TEMPLATE,
                        ENTER_BATTLE_KUIXING_TEMPLATE))
                .roamingTarget(false)
                .targetRole(NpcRole.COMBAT_TARGET)
                .sourceTask(TaskType.WUBEI)
                .tooltipFirst(probeTarget)
                .closeStoryBeforeDirectSceneClick(false)
                .targetEvidence(targetEvidence)
                .build();
        log.info("[wubei] try smart combat target click: label={} target={} evidence={} hint={} requestMap={}({}, {})",
                label, targetName, targetEvidence, hint, mapName, mapX, mapY);
        if (!npcClickService.clickNpcSmart(request)) {
            return false;
        }
        return tryClickKnownEnterBattleDialog("wubei:smart-combat-target:" + label);
    }

    private boolean tryDirectCombatFromTrackerHint(TaskExecutionContext context, String label) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        String targetName = resolveDirectCombatTargetName(label);
        if (targetName.isBlank()) {
            log.warn("[wubei] direct combat fallback skipped: label={} reason=no-combat-target yellow='{}' hint={}",
                    label,
                    currentTrackerPanel == null ? null : currentTrackerPanel.getYellowText(),
                    currentTrackerDestinationHint);
            return false;
        }

        TrackerDestinationHint hint = currentTrackerDestinationHint;
        String mapName = hint == null ? "" : hint.mapName();
        int mapX = hint == null ? -1 : hint.x();
        int mapY = hint == null ? -1 : hint.y();
        /*
         * This Alt+A fallback is only reached after 五倍 tracker pathing has stopped near the
         * tracker destination. It must not be used for accept/maintenance NPCs, because Alt+A turns
         * the next click into a direct battle click instead of opening the normal NPC dialog.
         */
        NpcClickRequest request = NpcClickRequest.builder()
                .player(gameContext.getMe())
                .mapName(mapName)
                .mapX(mapX)
                .mapY(mapY)
                .npcName(targetName)
                .tuneX(-10)
                .tuneY(0)
                .expectedDialogTemplatePath(ENTER_BATTLE_TEMPLATE)
                .roamingTarget(true)
                .targetRole(NpcRole.COMBAT_TARGET)
                .sourceTask(TaskType.WUBEI)
                .build();
        log.info("[wubei] try direct combat fallback: label={} target={} hint={} requestMap={}({}, {})",
                label, targetName, hint, mapName, mapX, mapY);
        boolean enteredCombat = npcClickService.tryDirectCombatTargetClick(request);
        if (!enteredCombat) {
            return false;
        }
        autoCombatService.initializeForCurrentWindow();
        TaskSleep.sleepOrStop(context, 1200L, "Wubei task interrupted");
        return true;
    }

    private WindowPathingSnapshot currentWindowPathingSnapshot() {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        return runtime == null ? null : runtime.getPathingSnapshot();
    }

    private boolean isCurrentTrackerPathingSnapshot(WindowPathingSnapshot snapshot) {
        if (snapshot == null || snapshot.getIntent() == null) {
            return false;
        }
        WindowPathingIntent intent = snapshot.getIntent();
        return intent.getType() == WindowPathingIntentType.UNTARGETED_TRACKER
                && intent.getSource() != null
                && intent.getSource().startsWith("wubei:tracker-green-click:");
    }

    private void clearCurrentPathingSignal(String reason) {
        windowTaskContextHolder.rawCurrent().ifPresent(runtime -> runtime.clearPathingSignal(reason));
    }

    private String resolveDirectCombatTargetName(String label) {
        if (isProbeRuntimeActive() || (label != null && label.contains("probe"))) {
            return PROBE_TARGET_NPC_NAME;
        }
        return resolveTrackerCombatTargetName();
    }

    private boolean tryClickKnownEnterBattleDialog(String source) {
        DialogResult confirm = tryConsumePreparedWubeiDialog(DialogOperation.WUBEI_ENTER_BATTLE, source);
        if (confirm == null) {
            log.info("[wubei] known enter-battle dialog not prepared yet: source={}", source);
            return false;
        }
        boolean clicked = OPTION_ENTER_BATTLE.equals(confirm.getActionKey())
                || OPTION_ENTER_BATTLE_PROVE.equals(confirm.getActionKey())
                || OPTION_ENTER_BATTLE_KUIXING.equals(confirm.getActionKey());
        log.info("[wubei] known enter-battle dialog check: source={} clicked={} status={} action={}",
                source, clicked, confirm.getStatus(), confirm.getActionKey());
        return clicked;
    }

    private WubeiStepOutcome tickEnterBattle(TaskExecutionContext context, WubeiRoundContext state) {
        long now = System.currentTimeMillis();
        if (enterBattleStartedAt <= 0L) {
            enterBattleStartedAt = now;
            enterBattleNextRetryAt = now;
        }

        long checkpointStartedAt = now;
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        now = System.currentTimeMillis();
        long checkpointBlockedMs = now - checkpointStartedAt;
        if (checkpointBlockedMs >= PAUSE_TIMER_COMPENSATION_THRESHOLD_MS) {
            enterBattleStartedAt += checkpointBlockedMs;
            enterBattleNextRetryAt += checkpointBlockedMs;
            log.info("[wubei] enter battle timer paused: blockedMs={} adjustedStartAt={} adjustedNextRetryAt={}",
                    checkpointBlockedMs, enterBattleStartedAt, enterBattleNextRetryAt);
        }

        if (now - enterBattleStartedAt >= WAIT_BATTLE_TIMEOUT_MS) {
            log.warn("[wubei] enter battle timeout: elapsedMs={} timeoutMs={}",
                    now - enterBattleStartedAt, WAIT_BATTLE_TIMEOUT_MS);
            return WubeiStepOutcome.failed(state, "enter battle timeout");
        }

        AutoCombatService.TickResult tick = autoCombatService.handleCombatTick(context, TASK_CODE, true);
        if (tick == AutoCombatService.TickResult.IN_COMBAT) {
            waitBattleSawCombat = true;
            currentProbeTaskStartedAt = 0L;
            return WubeiStepOutcome.sharedState(
                    state.next(WubeiPhase.WAIT_BATTLE_FINISH, "combat-already-started"),
                    "combat already started");
        }
        if (tick == AutoCombatService.TickResult.EXIT_RECOVERED) {
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.POST_BATTLE_RECOVER, "combat-ended-during-enter-battle"),
                    "combat ended during enter battle phase");
        }

        if (tryClickKnownEnterBattleDialog("wubei:enter-battle")) {
            autoCombatService.initializeForCurrentWindow();
            TaskSleep.sleepOrStop(context, 1200L, "Wubei task interrupted");
            return WubeiStepOutcome.sharedState(
                    state.next(WubeiPhase.WAIT_BATTLE_FINISH, "battle-dialog-clicked"),
                    "battle dialog clicked; wait for combat entry");
        }

        if (now < enterBattleNextRetryAt) {
            return WubeiStepOutcome.sharedState(state, "enter battle retry waiting");
        }

        WindowPathingSnapshot snapshot = currentWindowPathingSnapshot();
        boolean nearDestination = isNearCurrentTrackerDestination(snapshot);
        if (nearDestination) {
            log.info("[wubei] runner snapshot says leader is near tracker destination; try combat target: hint={} snapshot={}({}, {})",
                    currentTrackerDestinationHint,
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY());
            if (tryClickTrackerCombatTargetSmart(context, "runner-destination-smart-click",
                    NpcTargetEvidence.CONFIRMED)) {
                enterBattleNextRetryAt = now + 6_000L;
                return WubeiStepOutcome.sharedState(
                        state.next(WubeiPhase.WAIT_BATTLE_FINISH, "smart-combat-target-clicked"),
                        "destination smart target clicked");
            }
            if (tryDirectCombatFromTrackerHint(context, "runner-destination-direct-combat")) {
                enterBattleNextRetryAt = now + 6_000L;
                return WubeiStepOutcome.sharedState(
                        state.next(WubeiPhase.WAIT_BATTLE_FINISH, "direct-combat-target-clicked"),
                        "direct combat click confirmed; wait for combat entry");
            }
        } else {
            log.info("[wubei] enter battle target fallback skipped: nearDestination={} hint={} snapshotState={} snapshot={}({}, {})",
                    false, currentTrackerDestinationHint,
                    snapshot == null ? null : snapshot.getState(),
                    snapshot == null ? null : snapshot.getCurrentMapName(),
                    snapshot == null ? null : snapshot.getCurrentX(),
                    snapshot == null ? null : snapshot.getCurrentY());
        }

        if (shouldRetryTrackerGreenInBattleWait()
                && currentTrackerPanel != null
                && !currentTrackerPanel.getGreenLinks().isEmpty()) {
            boolean clicked = clickTaskTrackerGreen(context, currentTrackerPanel.getGreenLinks().get(0),
                    "enter-battle-retry", 0);
            if (clicked) {
                return WubeiStepOutcome.pathingStarted(
                        state.next(WubeiPhase.RESOLVE_AFTER_PATHING, "enter-battle-retry-pathing-started"),
                        "enter battle retry pathing started");
            }
        } else {
            log.info("[wubei] skip enter-battle tracker retry: probeActive={} chained={} hasSnapshot={}",
                    isProbeRuntimeActive(), currentRoundChainedCombatExpected, currentTrackerPanel != null);
        }

        enterBattleNextRetryAt = now + 6_000L;
        return WubeiStepOutcome.sharedState(state, "enter battle unresolved; wait before retry");
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
            currentProbeTaskStartedAt = 0L;
            /*
             * Battle is shared state. Return quickly so member auto-battle tasks can acquire the
             * task turn and refresh their own automatic-combat panels before rounds expire.
             */
            return WubeiStepOutcome.sharedState(state, "combat still running");
        }

        if (!waitBattleSawCombat && now >= waitBattleNextTrackerRetryAt) {
            waitBattleNextTrackerRetryAt = now + 3_000L;
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.ENTER_BATTLE, "wait-battle-no-combat-yet"),
                    "combat not observed yet; return to enter battle resolver");
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

    private void resetEnterBattleRuntime() {
        enterBattleStartedAt = 0L;
        enterBattleNextRetryAt = 0L;
    }

    private boolean isNearCurrentTrackerDestination(WindowPathingSnapshot snapshot) {
        if (currentTrackerDestinationHint == null) {
            return false;
        }
        if (snapshot == null
                || snapshot.getCurrentMapName() == null
                || snapshot.getCurrentX() == null
                || snapshot.getCurrentY() == null) {
            log.info("[wubei] destination hint fallback skipped: runner location unavailable hint={} snapshot={}",
                    currentTrackerDestinationHint, snapshot);
            return false;
        }
        LocationInfo location = new LocationInfo(
                snapshot.getCurrentMapName(),
                snapshot.getCurrentX(),
                snapshot.getCurrentY());
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
        TaskTrackerPanelReadResult postCombatPanel =
                taskTrackerPanelService.readWubeiTrackerPanel("post-combat-chained-" + combatCount);
        if (!postCombatPanel.isFound()) {
            /*
             * We only reach this branch after this runtime already remembered that the current
             * round was a chained 黄袍怪 fight. After combat, the left tracker title disappearing is
             * a normal completion signal, not a hot-start unknown state. Treat it as chain complete
             * and return home instead of falling into the generic accept-task recovery route.
             */
            log.info("[wubei] chained combat tracker title gone after battle; treat as completed: count={}",
                    combatCount);
            currentRoundChainedCombatContinueCount = 0;
            if (!useReturnItemAndVerifyStartMap(context, "chained-combat-title-gone")) {
                return WubeiStepOutcome.failed(state, "return home failed");
            }
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_ROUND_DONE_SOURCE),
                    "chained combat tracker title gone; check team return");
        }

        boolean stillChained = containsChainedCombatTarget(postCombatPanel.getYellowText());
        log.info("[wubei] chained combat post-battle tracker: count={} stillChained={} yellow='{}'",
                combatCount, stillChained, postCombatPanel.getYellowText());
        if (!stillChained) {
            currentRoundChainedCombatContinueCount = 0;
            if (!useReturnItemAndVerifyStartMap(context, "chained-combat-completed")) {
                return WubeiStepOutcome.failed(state, "return home failed");
            }
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_ROUND_DONE_SOURCE),
                    "chained combat completed; check team return");
        }

        currentTrackerPanel = postCombatPanel;
        currentRoundChainedCombatContinueCount = combatCount;
        if (!continueChainedCombatFromTracker(context, postCombatPanel, combatCount)) {
            log.warn("[wubei] chained combat tracker still has target but continue click failed: count={} yellow='{}'",
                    combatCount, postCombatPanel.getYellowText());
            return WubeiStepOutcome.failed(state, "chained combat continue click failed");
        }
        log.info("[wubei] chained combat target continues: currentCount={} nextState=ENTER_BATTLE",
                combatCount);
        TaskSleep.sleepOrStop(context, 450L, "Wubei task interrupted");
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.ENTER_BATTLE, "chained-combat-continued-" + combatCount),
                "chained combat target clicked; resolve enter-battle dialog");
    }

    private boolean continueChainedCombatFromTracker(
            TaskExecutionContext context,
            TaskTrackerPanelReadResult panel,
            int combatCount) {
        if (!panel.getGreenLinks().isEmpty()) {
            TaskTrackerGreenLink segment = panel.getGreenLinks().get(0);
            return clickTaskTrackerGreen(context, segment, "chained-combat-" + combatCount, 1);
        }
        log.warn("[wubei] chained combat tracker has no green segment; try visible tooltip: count={} yellow='{}'",
                combatCount, panel.getYellowText());
        return tryClickTrackerCombatTargetSmart(
                context, "chained-combat-tracker-fallback-" + combatCount, NpcTargetEvidence.CONFIRMED);
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

    private record TrackerDestinationHint(String mapName, int x, int y, String rawText) {
    }

    private record TrackerDestinationHintCapture(
            String label,
            int sample,
            OcrWindowRegion region,
            String rawPath,
            String yellowPath,
            long startedAt,
            long captureElapsedMs,
            long delayMs,
            long refreshMs,
            long captureMs) {
    }
}
