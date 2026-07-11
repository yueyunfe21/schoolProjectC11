package com.bot.dhxy.task.wubei;

import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.cloud.task.ImagePreprocessOperation;
import com.bot.dhxy.cloud.task.ImageProcessorService;
import com.bot.dhxy.cloud.task.ImageProcessorService.ImageProcessorResult;
import com.bot.dhxy.cloud.task.ImageProcessorService.RequestMetadata;
import com.bot.dhxy.cloud.task.TaskPolicyCloudDecision;
import com.bot.dhxy.cloud.task.TaskPolicyCloudDecisionService;
import com.bot.dhxy.cloud.task.TaskRecoveryCloudDecision;
import com.bot.dhxy.cloud.task.TaskRecoveryCloudDecisionService;
import com.bot.dhxy.cloud.task.TrackerLinkRankerCloudDecision;
import com.bot.dhxy.cloud.task.TrackerLinkRankerCloudShadowService;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogPreparationStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.model.maintenance.TaskMaintenanceResult;
import com.bot.dhxy.model.maintenance.TaskMaintenanceStatus;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.pause.TaskPauseResumeFingerprint;
import com.bot.dhxy.model.pause.TaskPauseResumeReconcileResult;
import com.bot.dhxy.model.npc.DirectCombatClickResult;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcClickRequest;
import com.bot.dhxy.model.npc.NpcTargetEvidence;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.model.tasktracker.TaskTrackerFastMatchResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerGreenLink;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelSourceType;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.CommonBoxService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.MapNameCanonicalizer;
import com.bot.dhxy.service.MemoryService;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.service.NpcClickService;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.service.ReturnItemPrescanService;
import com.bot.dhxy.service.TaskMaintenanceService;
import com.bot.dhxy.service.TaskTrackerPanelService;
import com.bot.dhxy.service.TeamReturnService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.pause.TaskPauseResumeReconciler;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowReadyEvent;
import com.bot.dhxy.window.model.WindowReadyEventType;
import com.bot.dhxy.window.runtime.WindowReadyEventBus;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int ACCEPT_NPC_X = 86;
    private static final int ACCEPT_NPC_Y = 87;
    private static final int START_EXIT_X = 88;
    private static final int START_EXIT_Y = 157;
    private static final int PREPATH_MINI_MAP_CLICK_RANDOM_RADIUS_PX = 12;
    private static final int HEAL_PET_NPC_X = 95;
    private static final int HEAL_PET_NPC_Y = 126;
    private static final int REPAIR_EQUIPMENT_NPC_X = 324;
    private static final int REPAIR_EQUIPMENT_NPC_Y = 109;
    private static final int ACCEPT_NPC_DIRECT_CLICK_DISTANCE = 12;
    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/wubei/wubei_accept_chumoweiguo.png";
    private static final String ACCEPT_DIALOG_RAW_TEMPLATE = "images/template/dialog/wubei/wubei_accept_chumoweiguo2.png";
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
    private static final String TRACKER_GREEN_PATHING_SOURCE_PREFIX = "wubei:tracker-green-click";
    private static final String POST_ACCEPT_PREPATH_SOURCE_PREFIX = "wubei:post-accept-prepath:";
    private static final String DARK_THUNDER_REROLL_PREPATH_SOURCE_PREFIX = "wubei:dark-thunder-reroll-prepath:";
    private static final String DARK_THUNDER_KEYWORD = "暗雷怪";
    private static final String CHAINED_COMBAT_TARGET_KEYWORD = "黄袍";
    private static final String PROBE_TARGET_NPC_NAME = "白龙马";
    private static final int MAX_CHAINED_COMBAT_ATTEMPTS = 5;
    private static final int MAX_TRACKER_CLICK_ATTEMPTS = 12;
    private static final int MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS = 5;
    private static final int MAX_PROBE_ITEM_ATTEMPTS_PER_LINK = 2;
    private static final long PROBE_ENTER_BATTLE_TIMEOUT_MS = 300_000L;
    private static final long PROBE_TARGET_CANDIDATE_ENTER_BATTLE_WAIT_MS = 6_000L;
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
    private static final long MAINTENANCE_BROADCAST_HANDOFF_DELAY_MS = 3_000L;
    private static final long CHAINED_POST_BATTLE_FIRST_AID_BROADCAST_MS = 5_000L;
    private static final String CHAINED_POST_BATTLE_BROADCAST_SOURCE_PREFIX = "post-battle-chained-broadcast";
    private static final long PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS = 2_500L;
    private static final long ENTER_BATTLE_DIALOG_BLOCK_MAX_AGE_MS = 5_000L;
    private static final long WUBEI_PREPARED_DIALOG_MAX_AGE_MS = 3_000L;
    private static final long WUBEI_ACCEPT_DIALOG_FOREGROUND_WAIT_MS = 15_000L;
    private static final long PROBE_ENTER_BATTLE_EVENT_RECHECK_MS = 5_000L;
    private static final long READY_EVENT_PRIORITY_MAX_AGE_MS = 3_000L;
    private static final long READY_EVENT_PENDING_WARN_MS = 3_000L;
    private static final long READY_EVENT_PRIORITY_YIELD_DELAY_MS = 180L;
    private static final long WUBEI_WAIT_UNTIL_RUNNER_EVENT_MS = -1L;
    private static final long WUBEI_COMBAT_MAINTENANCE_WAKE_MIN_TIMEOUT_MS = 500L;
    private static final long WUBEI_COMBAT_MAINTENANCE_WAKE_MAX_TIMEOUT_MS = 10_000L;
    private static final long WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS = 180_000L;
    private static final long CHAINED_ENTER_BATTLE_PHASE_RETRY_SLEEP_MS = 600L;
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
            .expectedDialogRawTemplatePath(ACCEPT_DIALOG_RAW_TEMPLATE)
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
    private final MemoryService memoryService;
    private final DialogService dialogService;
    private final AutoCombatService autoCombatService;
    private final BagService bagService;
    private final ReturnItemPrescanService returnItemPrescanService;
    private final PlayerStateService playerStateService;
    private final TaskMaintenanceService taskMaintenanceService;
    private final CommonBoxService commonBoxService;
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
    private final WindowReadyEventBus windowReadyEventBus;
    private final TaskTrackerPanelService taskTrackerPanelService;
    private final ImageProcessorService imageProcessorService;
    private final TaskPauseResumeReconciler taskPauseResumeReconciler;
    private final TrackerLinkRankerCloudShadowService trackerLinkRankerCloudShadowService;
    private final TaskPolicyCloudDecisionService taskPolicyCloudDecisionService;
    private final TaskRecoveryCloudDecisionService taskRecoveryCloudDecisionService;
    private final AutomationMetricsService automationMetricsService;
    private int currentRoundNumber;
    private boolean currentRoundChainedCombatExpected;
    private int currentRoundChainedCombatContinueCount;
    private int currentRoundChainedCombatRecoveryBroadcastCount;
    private boolean currentRoundChainedTrackerCacheAttempted;
    private PreparedDialogAction currentRoundChainedTrackerFastAction;
    private TaskTrackerPanelReadResult currentTrackerPanel;
    private volatile CompletableFuture<TaskTrackerPanelReadResult> postAcceptTrackerPanelFuture;
    private volatile TrackerDestinationHint currentTrackerDestinationHint;
    private volatile long trackerDestinationHintRequestId;
    private List<TaskTrackerGreenLink> currentProbeSegments = List.of();
    private boolean[] currentProbeUsed = new boolean[0];
    private int[] currentProbeItemAttempts = new int[0];
    private int currentProbeIndex = -1;
    private int currentProbeStoryWaitIndex = -1;
    private long currentProbeStoryWaitStartedAt;
    private long currentProbeTaskStartedAt;
    private long waitBattleStartedAt;
    private long waitBattleNextTrackerRetryAt;
    private boolean waitBattleSawCombat;
    private DialogResult lastEnterBattleDialogResult;
    private long enterBattleStartedAt;
    private long enterBattleNextRetryAt;
    private long lastHealPetMaintenanceAt;
    private long lastRepairEquipmentMaintenanceAt;
    private int consecutiveHealPetMaintenanceFailures;
    private int consecutiveRepairEquipmentMaintenanceFailures;
    private int lastLeaderPathingSummonAttemptRound;
    private TeamReturnService.LeaderSignalPrecheck pendingTeamReturnPrecheck;
    /**
     * 已验证回到宝象国后的任务级位置事实。它跨 round 边界保留，直到下一张五倍任务的
     * accept option 真正点击成功，避免云端等待后又重复读地图/坐标。
     */
    private LocationInfo verifiedReturnHomeLocation;
    private long lastPostCombatIdleTimeoutConsumedSeq;
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
        lastPostCombatIdleTimeoutConsumedSeq = windowReadyEventBus.currentSequence();
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
        if (context.getWindowRuntimeContext() != null) {
            context.getWindowRuntimeContext().updateTaskRunProgress(completedRuns, maxRuns);
        }
        log.info("[wubei] task started: maxRuns={}", maxRuns <= 0 ? "unlimited" : maxRuns);

        try {
            while (maxRuns <= 0 || completedRuns < maxRuns) {
                TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
                int round = completedRuns + 1;
                resetRoundState(round);
                boolean cleanQueueTransitionStartup = completedRuns == 0 && context.isCleanQueueTransitionStartup();
                if (cleanQueueTransitionStartup) {
                    log.info("[wubei] skip hot-start because clean queued task transition; force accept NPC route");
                }
                WubeiRoundContext roundContext = completedRuns == 0
                        ? (cleanQueueTransitionStartup
                                ? WubeiRoundContext.routeToAcceptNpc(round)
                                : WubeiRoundContext.hotStart(round))
                        : WubeiRoundContext.normalStart(round);
                taskMaintenanceService.beginTeamMaintenanceRound(context, TASK_CODE, round,
                        "wubei:round-start");
                String roundId = roundMetricId(context, TASK_CODE, round);
                long roundStartedAt = System.currentTimeMillis();
                automationMetricsService.recordRoundStarted(context, roundId, round,
                        roundMetricType(roundContext), "五倍轮次开始",
                        Map.of("sourcePhase", roundContext.phase().name(), "source", roundContext.source()));
                TaskRunResult roundResult;
                try {
                    roundResult = runRoundPhases(context, roundContext);
                } catch (RuntimeException e) {
                    finishRoundMetric(context, roundId, round, roundContext, TaskRunResult.FAILED,
                            roundStartedAt, "五倍轮次异常: " + e.getClass().getSimpleName());
                    throw e;
                }
                finishRoundMetric(context, roundId, round, roundContext, roundResult, roundStartedAt,
                        "五倍轮次结束");
                if (roundResult == TaskRunResult.STOPPED) {
                    gameContext.setBotStatus(GameContext.BotStatus.IDLE);
                    return TaskRunResult.STOPPED;
                }
                if (roundResult != TaskRunResult.SUCCESS) {
                    gameContext.setBotStatus(GameContext.BotStatus.ERROR);
                    return TaskRunResult.FAILED;
                }

                completedRuns++;
                if (context.getWindowRuntimeContext() != null) {
                    context.getWindowRuntimeContext().updateTaskRunProgress(completedRuns, maxRuns);
                }
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
        currentRoundChainedCombatRecoveryBroadcastCount = 0;
        currentRoundChainedTrackerCacheAttempted = false;
        currentRoundChainedTrackerFastAction = null;
        currentTrackerPanel = null;
        postAcceptTrackerPanelFuture = null;
        currentTrackerDestinationHint = null;
        resetProbeRuntime();
        currentProbeTaskStartedAt = 0L;
        clearTrackerGreenPathingIntent("wubei:round-start");
        windowTaskContextHolder.rawCurrent()
                .ifPresent(runtime -> runtime.clearOrdinaryPreBattleTimer("wubei round reset"));
        lastLeaderPathingSummonAttemptRound = -1;
        log.info("[wubei] round {} started", round);
    }

    private String roundMetricId(TaskExecutionContext context, String taskCode, int round) {
        long taskRunId = context == null ? 0L : context.getTaskRunId();
        String windowId = context == null ? "window" : context.getWindowId();
        return taskCode + "-" + (taskRunId > 0L ? taskRunId : windowId) + "-round-" + round;
    }

    private void finishRoundMetric(TaskExecutionContext context,
                                   String roundId,
                                   int round,
                                   WubeiRoundContext roundContext,
                                   TaskRunResult result,
                                   long roundStartedAt,
                                   String message) {
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - roundStartedAt);
        automationMetricsService.recordRoundFinished(context, roundId, round, roundMetricType(roundContext),
                roundMetricStatus(result), roundResultCode(result), message, elapsedMs,
                Map.of("sourcePhase", roundContext.phase().name(), "source", roundContext.source()));
    }

    private String roundMetricType(WubeiRoundContext roundContext) {
        String trackerTitle = trackerTaskTitle(currentTrackerPanel);
        if (!trackerTitle.isBlank()) {
            return trackerTitle;
        }
        if (currentTrackerPanel != null && currentTrackerPanel.getYellowText() != null
                && !currentTrackerPanel.getYellowText().isBlank()) {
            return currentTrackerPanel.getYellowText();
        }
        return roundContext == null ? "五倍" : "五倍/" + roundContext.phase();
    }

    private AutomationMetricStatus roundMetricStatus(TaskRunResult result) {
        if (result == TaskRunResult.SUCCESS) {
            return AutomationMetricStatus.SUCCESS;
        }
        if (result == TaskRunResult.STOPPED) {
            return AutomationMetricStatus.STOPPED;
        }
        if (result == TaskRunResult.SKIPPED) {
            return AutomationMetricStatus.SKIPPED;
        }
        return AutomationMetricStatus.FAILED;
    }

    private String roundResultCode(TaskRunResult result) {
        return result == null ? "FAILED" : result.name();
    }

    /*
     * 五倍目前先采用修罗 V2 的轻量 phase runner：每个 phase 只执行一个业务动作，
     * PATHING/COMBAT/POST_BATTLE 等共享状态会释放 task turn，让其他窗口有机会补血或响应。
     */
    private TaskRunResult runRoundPhases(TaskExecutionContext context, WubeiRoundContext initialState) {
        WubeiRoundContext roundState = initialState;
        int phaseLoopGuard = 0;
        while (!roundState.phase().isTerminal()) {
            TaskPauseResumeFingerprint pauseFingerprint = taskPauseResumeReconciler.capture(
                    context, TaskType.WUBEI, roundState.phase().name(), "phase-loop");
            long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            TaskPauseResumeReconcileResult pauseReconcile = taskPauseResumeReconciler.reconcileAfterPause(
                    pauseFingerprint, context, pauseBlockedMs);
            if (pauseReconcile.isFallbackTaskHotStart()) {
                roundState = resolvePauseResumeTaskHotStart(roundState, pauseReconcile, "phase-loop");
                phaseLoopGuard = 0;
                continue;
            }
            if (pauseReconcile.isFingerprintMatched()) {
                compensateProbeTimersAfterPause(pauseReconcile.getPauseBlockedMs(), "phase-loop");
            }
            WubeiRoundContext currentState = roundState;

            AtomicReference<WubeiStepOutcome> phaseOutcome = new AtomicReference<>();
            TaskTransactionOutcome transaction;
            String transactionName = "wubei:" + currentState.phase();
            try {
                transaction = taskTransactionRunner.runDynamic(
                        transactionName,
                        TaskTransactionResult.READY_TO_CONTINUE,
                        TaskYieldPolicy.CONTINUE_CHAIN,
                        () -> {
                            WubeiStepOutcome priorityOutcome = checkReadyPriorityBeforePhase(context, currentState);
                            WubeiStepOutcome outcome = priorityOutcome != null
                                    ? priorityOutcome
                                    : runPhase(context, currentState);
                            log.info("[wubei] phase outcome: phase={} result={} yield={} next={} message={}",
                                    currentState.phase(), outcome.transactionResult(), outcome.yieldPolicy(),
                                    outcome.nextState().phase(), outcome.message());
                            outcome = applyTaskPolicyCloudDecision(
                                    currentState, outcome, outcome.transactionResult());
                            phaseOutcome.set(outcome);
                            return TaskTransactionRunner.TaskTransactionDecision.of(
                                    outcome.transactionResult(),
                                    outcome.yieldPolicy());
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
                outcome = parkAfterYieldIfNeeded(context, currentState, outcome);
                roundState = outcome.nextState();
                continue;
            }
            if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD
                    || outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED
                    || outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
                yieldAfterMustYield(context, outcome);
                outcome = parkAfterYieldIfNeeded(context, currentState, outcome);
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

    private WubeiRoundContext resolvePauseResumeTaskHotStart(WubeiRoundContext state,
                                                             TaskPauseResumeReconcileResult reconcile,
                                                             String boundary) {
        currentTrackerPanel = null;
        currentTrackerDestinationHint = null;
        resetProbeRuntime();
        resetEnterBattleRuntime();
        resetWaitBattleRuntime();
        WubeiRoundContext hotStart = state.next(WubeiPhase.HOT_START_DETECT,
                "pause-resume-hot-start:" + boundary);
        log.warn("[wubei pause-resume] fallback task hot-start: phase={} next={} boundary={} pauseBlockedMs={} fingerprintMatched={} mismatchReason={} compensatedTimers={} clearedVolatileState={} fallbackTaskHotStart={}",
                state.phase(), hotStart.phase(), boundary, reconcile.getPauseBlockedMs(),
                reconcile.isFingerprintMatched(), reconcile.getMismatchReason(), reconcile.getCompensatedTimers(),
                reconcile.getClearedVolatileState(), reconcile.isFallbackTaskHotStart());
        return hotStart;
    }

    private WubeiRoundContext recoverRoundAfterFailure(TaskExecutionContext context,
                                                       WubeiRoundContext failedState,
                                                       WubeiStepOutcome outcome) {
        log.warn("[wubei] phase failed; recover current round from accept task: phase={} message={} recoveryCount={}",
                failedState.phase(), outcome.message(), failedState.recoveryCount());
        if (failedState.recoveryCount() >= 3) {
            log.error("[wubei] recovery limit reached: phase={} message={}", failedState.phase(), outcome.message());
            WubeiRoundContext failedRecovery = failedState.next(WubeiPhase.FAILED, "recovery-limit");
            TaskRecoveryCloudDecision<WubeiPhase> recoveryDecision = decideTaskRecovery(
                    failedState, outcome, "recovery-limit", failedRecovery);
            if (recoveryDecision.isCloudRequiredFailure()) {
                log.error("[wubei cloud-execute] {} cloud.required recovery-limit failed: phase={} reason={}",
                        CloudDecisionServiceId.TASK_RECOVERY, failedState.phase(), recoveryDecision.getRejectReason());
            }
            return failedRecovery;
        }
        WubeiRoundContext recoveryState =
                failedState.recoverTo(WubeiPhase.ROUTE_TO_MAIN_TASK, "recover-from-" + failedState.phase());
        TaskRecoveryCloudDecision<WubeiPhase> recoveryDecision = decideTaskRecovery(
                failedState, outcome, "recover-to-main-task", recoveryState);
        if (!recoveryDecision.isRecoveryAllowed()) {
            return cloudRequiredRecoveryFailure(failedState, recoveryDecision, "recover-to-main-task");
        }
        taskMaintenanceService.closeTeamMaintenanceWindow(context, TASK_CODE, failedState.round(),
                "wubei:recover-round");
        taskTransactionRunner.forceReleaseTurn("wubei-recover:" + failedState.phase());
        uiCleanerService.cleanUpAll();
        TaskSleep.sleepOrStop(context, 800L, "Wubei task interrupted");
        return recoveryState;
    }

    private WubeiStepOutcome applyTaskPolicyCloudDecision(WubeiRoundContext currentState,
                                                          WubeiStepOutcome outcome,
                                                          TaskTransactionResult runnerResult) {
        try {
            TaskPolicyCloudDecision<WubeiPhase> decision = taskPolicyCloudDecisionService.decide(
                    TASK_CODE,
                    "wubei-phase-outcome",
                    currentState.round(),
                    currentState.phase(),
                    runnerResult,
                    outcome.transactionResult(),
                    outcome.yieldPolicy(),
                    outcome.nextState().phase(),
                    WubeiPhase.class,
                    Map.of(
                            "source", safeCloudValue(currentState.source()),
                            "nextSource", safeCloudValue(outcome.nextState().source()),
                            "message", safeCloudValue(outcome.message())));
            if (decision.isCloudRequiredFailure()) {
                log.error("[wubei cloud-execute] TASK_POLICY cloud.required failure: phase={} localResult={} "
                                + "localYield={} localNext={} failureResult={} failureNext={} reason={}",
                        currentState.phase(),
                        outcome.transactionResult(),
                        outcome.yieldPolicy(),
                        outcome.nextState().phase(),
                        decision.getEffectiveResult(),
                        decision.getEffectiveNextPhase(),
                        decision.getRejectReason());
                return new WubeiStepOutcome(
                        currentState.next(decision.getEffectiveNextPhase(), "cloud-required-task-policy"),
                        decision.getEffectiveResult(),
                        decision.getEffectiveYieldPolicy(),
                        "cloud.required TASK_POLICY failure: " + decision.getRejectReason(),
                        outcome.waitSpec());
            }
            if (!decision.isCloudExecuted()) {
                return outcome;
            }
            TaskPolicyCloudDecision.AppliedOutcome<WubeiPhase> applied = decision.appliedOutcome();
            WubeiRoundContext nextState = outcome.nextState();
            WubeiRoundContext cloudNextState = WubeiRoundContext.builder()
                    .phase(applied.nextPhase())
                    .round(nextState.round())
                    .source(nextState.source())
                    .phaseRetryCount(nextState.phaseRetryCount())
                    .recoveryCount(nextState.recoveryCount())
                    .waitingPathing(nextState.waitingPathing())
                    .waitingAcceptDialog(nextState.waitingAcceptDialog())
                    .build();
            log.info("[wubei cloud-execute] TASK_POLICY accepted: phase={} localResult={} localYield={} localNext={} "
                            + "cloudResult={} cloudYield={} cloudNext={} message={}",
                    currentState.phase(),
                    outcome.transactionResult(),
                    outcome.yieldPolicy(),
                    nextState.phase(),
                    applied.transactionResult(),
                    applied.yieldPolicy(),
                    applied.nextPhase(),
                    outcome.message());
            return new WubeiStepOutcome(
                    cloudNextState,
                    applied.transactionResult(),
                    applied.yieldPolicy(),
                    outcome.message(),
                    outcome.waitSpec());
        } catch (RuntimeException e) {
            if (Thread.currentThread().isInterrupted()) {
                throw e;
            }
            log.error("[wubei cloud-execute] TASK_POLICY exception; terminal cloud.required failure: phase={} error={}",
                    currentState.phase(), e.toString());
            log.debug("[wubei cloud-execute] TASK_POLICY execute failure stack", e);
            return new WubeiStepOutcome(
                    currentState.next(WubeiPhase.FAILED, "cloud-required-task-policy-exception"),
                    TaskTransactionResult.RETRYABLE_ERROR,
                    TaskYieldPolicy.MUST_YIELD,
                    "cloud.required TASK_POLICY exception: " + e.getClass().getSimpleName(),
                    outcome.waitSpec());
        }
    }

    private TaskRecoveryCloudDecision<WubeiPhase> decideTaskRecovery(WubeiRoundContext failedState,
                                                                     WubeiStepOutcome outcome,
                                                                     String recoveryAction,
                                                                     WubeiRoundContext recoveryState) {
        return taskRecoveryCloudDecisionService.decide(
                TASK_CODE,
                "wubei-recovery",
                failedState.round(),
                failedState.phase(),
                recoveryAction,
                recoveryState.phase(),
                WubeiPhase.class,
                Map.of(
                        "serviceId", CloudDecisionServiceId.TASK_RECOVERY.name(),
                        "failedSource", safeCloudValue(failedState.source()),
                        "outcomeResult", String.valueOf(outcome.transactionResult()),
                        "outcomeNextPhase", String.valueOf(outcome.nextState().phase()),
                        "recoveryCountBefore", Integer.toString(failedState.recoveryCount()),
                        "nextSource", safeCloudValue(recoveryState.source()),
                        "message", safeCloudValue(outcome.message())));
    }

    private WubeiRoundContext cloudRequiredRecoveryFailure(WubeiRoundContext failedState,
                                                          TaskRecoveryCloudDecision<WubeiPhase> recoveryDecision,
                                                          String recoveryAction) {
        log.error("[wubei cloud-execute] {} cloud.required failure; no local recovery: phase={} action={} reason={}",
                CloudDecisionServiceId.TASK_RECOVERY,
                failedState.phase(),
                recoveryAction,
                recoveryDecision.getRejectReason());
        return failedState.next(WubeiPhase.FAILED, "cloud-required-task-recovery:" + recoveryAction);
    }

    /**
     * Consume runner-prepared 五倍 dialog actions before this task starts normal phase work.
     *
     * <p>The window watcher can often see and prepare task dialogs earlier than the task loop gets
     * scheduled again. This gate keeps prepared 接任务/进战斗 clicks from waiting behind ordinary
     * tracker OCR or navigation retries. It validates operation and phase before clicking; it does
     * not run template matching by itself.</p>
     *
     * @param context task execution context for stop-aware sleeps/checkpoints.
     * @param state current 五倍 phase state.
     * @return an outcome when a prepared action was consumed or this window should yield to a
     *         prepared action owned by another 五倍 window; null when normal phase work may continue.
     */
    private WubeiStepOutcome checkReadyPriorityBeforePhase(TaskExecutionContext context,
                                                           WubeiRoundContext state) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return null;
        }

        WubeiStepOutcome postCombatIdleTimeout = consumePostCombatIdleTimeoutBeforeNormalPhase(state, runtime);
        if (postCombatIdleTimeout != null) {
            return postCombatIdleTimeout;
        }

        WubeiStepOutcome preBattleTimeout = consumeOrdinaryPreBattleTimeoutBeforeNormalPhase(state, runtime);
        if (preBattleTimeout != null) {
            return preBattleTimeout;
        }

        WubeiStepOutcome currentPrepared = consumeCurrentPreparedBeforeNormalPhase(
                context, state, runtime, null, "phase-boundary-before:" + state.phase());
        if (currentPrepared != null) {
            return currentPrepared;
        }

        Optional<WindowReadyEvent> otherPrepared = windowReadyEventBus.latestOtherFreshPreparedAction(
                runtime.getWindowId(), TaskType.WUBEI, READY_EVENT_PRIORITY_MAX_AGE_MS);
        return otherPrepared
                .map(event -> yieldToReadyEvent(state, runtime, event,
                        "prepared-action-priority-yield", "prepared action priority yield"))
                .orElse(null);
    }

    private WubeiStepOutcome consumePostCombatIdleTimeoutBeforeNormalPhase(WubeiRoundContext state,
                                                                           WindowRuntimeContext runtime) {
        WindowReadyEvent timeoutEvent = windowReadyEventBus
                .latest(runtime.getWindowId(), WindowReadyEventType.POST_COMBAT_IDLE_TIMEOUT)
                .orElse(null);
        if (timeoutEvent == null
                || timeoutEvent.getTaskType() != TaskType.WUBEI
                || timeoutEvent.getSequence() <= lastPostCombatIdleTimeoutConsumedSeq) {
            return null;
        }
        lastPostCombatIdleTimeoutConsumedSeq = timeoutEvent.getSequence();
        WubeiPhase originalPhase = state.phase();
        WubeiPhase restartPhase = WubeiPhase.ROUTE_TO_MAIN_TASK;
        String clearScope = "dialog-preparation/prepared/dialog/pathing/ordinary-prebattle/enter-battle/probe/chained/tracker-panel";
        runtime.clearDialogPreparationRequest("wubei consumed POST_COMBAT_IDLE_TIMEOUT");
        runtime.clearDialogInterest("wubei consumed POST_COMBAT_IDLE_TIMEOUT");
        runtime.clearOrdinaryPreBattleTimer("wubei consumed POST_COMBAT_IDLE_TIMEOUT");
        clearCurrentPathingSignal("wubei post-combat idle timeout consumed");
        resetEnterBattleRuntime();
        resetProbeRuntime();
        resetChainedCombatRuntime();
        currentTrackerPanel = null;
        currentTrackerDestinationHint = null;
        postAcceptTrackerPanelFuture = null;
        log.warn("[wubei post-combat-idle] timeout consumed by task: originalPhase={} round={} windowId={} hwnd={} readySeq={} readyAgeMs={} lastCombatExitAtMs={} elapsedMs={} clearScope={} restartPhase={} source={} summary={}",
                originalPhase, state.round(), runtime.getWindowId(), timeoutEvent.getHwnd(),
                timeoutEvent.getSequence(), readyAgeMs(timeoutEvent), timeoutEvent.getLastCombatExitAtMs(),
                timeoutEvent.getElapsedMs(), clearScope, restartPhase, timeoutEvent.getSource(),
                timeoutEvent.getSummary());
        return WubeiStepOutcome.continueTo(
                state.next(restartPhase, "post-combat-idle-timeout-reaccept"),
                "post-combat idle timeout; route back and reaccept");
    }

    private WubeiStepOutcome consumeOrdinaryPreBattleTimeoutBeforeNormalPhase(WubeiRoundContext state,
                                                                              WindowRuntimeContext runtime) {
        WindowReadyEvent timeoutEvent = windowReadyEventBus
                .latest(runtime.getWindowId(), WindowReadyEventType.PRE_BATTLE_TIMEOUT)
                .orElse(null);
        long startedAt = runtime.getOrdinaryPreBattleStartedAtMs();
        if (timeoutEvent == null
                || timeoutEvent.getTaskType() != TaskType.WUBEI
                || startedAt <= 0L
                || timeoutEvent.getCreatedAtMs() < startedAt) {
            return null;
        }
        log.warn("[wubei ordinary-prebattle] timeout consumed by task: phase={} round={} windowId={} hwnd={} readySeq={} readyAgeMs={} timerElapsedMs={} target={} source={}",
                state.phase(), state.round(), runtime.getWindowId(), timeoutEvent.getHwnd(),
                timeoutEvent.getSequence(), readyAgeMs(timeoutEvent),
                Math.max(0L, timeoutEvent.getCreatedAtMs() - startedAt),
                timeoutEvent.getTargetKeyword(), timeoutEvent.getSource());
        runtime.clearOrdinaryPreBattleTimer("wubei consumed PRE_BATTLE_TIMEOUT");
        clearCurrentPathingSignal("wubei ordinary pre-battle timeout consumed");
        resetEnterBattleRuntime();
        currentTrackerPanel = null;
        currentTrackerDestinationHint = null;
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "ordinary-prebattle-timeout-reaccept"),
                "ordinary pre-battle timeout; route back and reaccept");
    }

    private WubeiStepOutcome consumeCurrentPreparedBeforeNormalPhase(TaskExecutionContext context,
                                                                     WubeiRoundContext state,
                                                                     WindowRuntimeContext runtime,
                                                                     WindowReadyEvent relatedReadyEvent,
                                                                     String checkpoint) {
        PreparedDialogAction action = runtime.getPreparedDialogAction();
        if (action == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (!action.verifiedWithin(now, WUBEI_PREPARED_DIALOG_MAX_AGE_MS)) {
            warnReadyPendingTooLongIfNeeded(relatedReadyEvent, state, runtime, false,
                    "prepared-stale:" + Math.max(0L, now - action.getLastVerifiedAtMs()) + "ms");
            return null;
        }
        if (action.getOperation() == DialogOperation.WUBEI_ACCEPT_TASK) {
            return consumePreparedAcceptBeforeNormalPhase(context, state, runtime, relatedReadyEvent, checkpoint);
        }
        if (action.getOperation() == DialogOperation.WUBEI_ENTER_BATTLE) {
            return consumePreparedEnterBattleBeforeNormalPhase(context, state, runtime, relatedReadyEvent, checkpoint);
        }
        warnReadyPendingTooLongIfNeeded(relatedReadyEvent, state, runtime, false,
                "unsupported-operation:" + action.getOperation());
        return null;
    }

    private WubeiStepOutcome consumePreparedAcceptBeforeNormalPhase(TaskExecutionContext context,
                                                                    WubeiRoundContext state,
                                                                    WindowRuntimeContext runtime,
                                                                    WindowReadyEvent relatedReadyEvent,
                                                                    String checkpoint) {
        if (state.phase() != WubeiPhase.ACCEPT_TASK) {
            warnReadyPendingTooLongIfNeeded(relatedReadyEvent, state, runtime, false,
                    "accept-prepared-phase-mismatch:" + state.phase());
            return null;
        }
        DialogResult result = tryConsumePreparedWubeiDialog(
                DialogOperation.WUBEI_ACCEPT_TASK,
                "phase-priority:" + state.phase(),
                false);
        if (result == null) {
            return null;
        }
        boolean accepted = OPTION_ACCEPT_TASK.equals(result.getActionKey())
                && (result.getStatus() == DialogResultStatus.GREEN_TEMPLATE_CLICKED
                || result.getStatus() == DialogResultStatus.OPTION_KEYWORD_CLICKED);
        log.info("[wubei priority] consumed accept dialog before normal phase: checkpoint={} phase={} windowId={} hwnd={} readySeq={} readyAgeMs={} accepted={} status={} action={} click=({}, {})",
                checkpoint, state.phase(), runtime.getWindowId(),
                relatedReadyEvent == null ? null : relatedReadyEvent.getHwnd(),
                relatedReadyEvent == null ? -1L : relatedReadyEvent.getSequence(),
                readyAgeMs(relatedReadyEvent), accepted, result.getStatus(), result.getActionKey(),
                result.getAbsoluteX(), result.getAbsoluteY());
        if (!accepted) {
            return WubeiStepOutcome.failed(state, "priority accept prepared action failed");
        }
        npcClickService.confirmPendingSmartClick(
                START_MAP_NAME, ACCEPT_NPC_NAME, ACCEPT_NPC_X, ACCEPT_NPC_Y,
                "DIALOG_TEMPLATE", "wubei priority accept prepared consumed");
        if (result.getStatus() == DialogResultStatus.GREEN_TEMPLATE_CLICKED
                && result.getRelativeX() != null
                && result.getRelativeY() != null) {
            memoryService.recordDialogChoiceSuccess(
                    TASK_CODE, "acceptTask", ACCEPT_NPC_NAME,
                    START_MAP_NAME, ACCEPT_NPC_X, ACCEPT_NPC_Y, START_MAP_NAME,
                    result.getRelativeX(), result.getRelativeY(), result.getMatchedText(),
                    "phase-priority:" + state.phase());
        }
        return afterAcceptTaskSucceeded(
                context,
                state,
                result,
                "wubei priority accept prepared consumed",
                "priority-task-accepted",
                "prepared accept dialog consumed before normal phase");
    }

    private WubeiStepOutcome consumePreparedEnterBattleBeforeNormalPhase(TaskExecutionContext context,
                                                                        WubeiRoundContext state,
                                                                        WindowRuntimeContext runtime,
                                                                        WindowReadyEvent relatedReadyEvent,
                                                                        String checkpoint) {
        if (!canConsumeEnterBattlePreparedAction(state.phase(), runtime)) {
            warnReadyPendingTooLongIfNeeded(relatedReadyEvent, state, runtime, false,
                    "enter-battle-prepared-phase-mismatch:" + state.phase());
            return null;
        }
        DialogResult confirm = tryConsumePreparedWubeiDialog(
                DialogOperation.WUBEI_ENTER_BATTLE,
                "phase-priority:" + state.phase(),
                false);
        if (confirm == null) {
            return null;
        }
        taskMaintenanceService.closeTeamMaintenanceWindow(context, TASK_CODE, state.round(),
                "wubei:priority-enter-battle-dialog-detected:" + checkpoint);
        boolean clicked = OPTION_ENTER_BATTLE.equals(confirm.getActionKey())
                || OPTION_ENTER_BATTLE_PROVE.equals(confirm.getActionKey())
                || OPTION_ENTER_BATTLE_KUIXING.equals(confirm.getActionKey());
        log.info("[wubei priority] consumed enter-battle dialog before normal phase: checkpoint={} phase={} windowId={} hwnd={} readySeq={} readyAgeMs={} clicked={} status={} action={} click=({}, {})",
                checkpoint, state.phase(), runtime.getWindowId(),
                relatedReadyEvent == null ? null : relatedReadyEvent.getHwnd(),
                relatedReadyEvent == null ? -1L : relatedReadyEvent.getSequence(),
                readyAgeMs(relatedReadyEvent), clicked, confirm.getStatus(), confirm.getActionKey(),
                confirm.getAbsoluteX(), confirm.getAbsoluteY());
        if (!clicked) {
            return WubeiStepOutcome.failed(state, "priority enter-battle prepared action failed");
        }
        clearTrackerGreenPathingIntent("wubei prepared enter battle consumed");
        clearCurrentPathingSignal("wubei consumed prepared enter battle dialog by priority");
        runtime.clearOrdinaryPreBattleTimer("wubei consumed prepared enter battle dialog by priority");
        autoCombatService.initializeForCurrentWindow();
        autoCombatService.authorizeCombatDetectionAfterEnterBattleAction(
                "wubei:priority-enter-battle-clicked:" + confirm.getActionKey());
        return WubeiStepOutcome.sharedState(
                state.next(WubeiPhase.WAIT_BATTLE_FINISH, "priority-battle-dialog-clicked"),
                "prepared enter-battle dialog consumed before normal phase");
    }

    private boolean hasFreshPreparedAction(WindowRuntimeContext runtime, DialogOperation operation) {
        if (runtime == null || operation == null) {
            return false;
        }
        PreparedDialogAction action = runtime.getPreparedDialogAction();
        return action != null
                && action.getOperation() == operation
                && action.verifiedWithin(System.currentTimeMillis(), WUBEI_PREPARED_DIALOG_MAX_AGE_MS);
    }

    private boolean canConsumeEnterBattlePreparedAction(WubeiPhase phase, WindowRuntimeContext runtime) {
        /*
         * 普通怪和黄袍第一战在点绿字后会停在 RESOLVE_AFTER_PATHING 无限等待 Runner。
         * 只有 active pre-battle timer 能证明这里是普通/黄袍第一战的 Runner 结果，不是白龙马
         * probe 或其它阶段的旧 prepared action。PATHING_TERMINAL 仍然走 CR43 的同绿字重导航。
         */
        return phase == WubeiPhase.ENTER_BATTLE
                || (phase == WubeiPhase.RESOLVE_AFTER_PATHING
                && runtime != null
                && runtime.getOrdinaryPreBattleStartedAtMs() > 0L);
    }

    private WubeiStepOutcome yieldToReadyEvent(WubeiRoundContext state,
                                               WindowRuntimeContext runtime,
                                               WindowReadyEvent event,
                                               String stateReason,
                                               String message) {
        log.info("[wubei priority] phase yields because another window has prepared action: phase={} currentWindowId={} readyWindowId={} readyHwnd={} readySource={} readyOperation={} readyTarget={} readySeq={} readyAgeMs={} reason={}",
                state.phase(), runtime.getWindowId(), event.getWindowId(), event.getHwnd(),
                event.getSource(), event.getOperation(), event.getTargetKeyword(), event.getSequence(),
                readyAgeMs(event), stateReason);
        warnReadyPendingTooLongIfNeeded(event, state, runtime, true, "yield-to-ready-window");
        return WubeiStepOutcome.sharedState(
                state.retrySamePhase(stateReason),
                message);
    }

    private long readyAgeMs(WindowReadyEvent event) {
        if (event == null || event.getCreatedAtMs() <= 0L) {
            return -1L;
        }
        return Math.max(0L, System.currentTimeMillis() - event.getCreatedAtMs());
    }

    private void warnReadyPendingTooLongIfNeeded(WindowReadyEvent event,
                                                 WubeiRoundContext state,
                                                 WindowRuntimeContext runtime,
                                                 boolean preparedUsable,
                                                 String staleReason) {
        long ageMs = readyAgeMs(event);
        if (event == null || ageMs < READY_EVENT_PENDING_WARN_MS) {
            return;
        }
        log.warn("[wubei priority] ready dialog pending too long: ageMs={} readyWindowId={} readyHwnd={} readySeq={} readySource={} readyOperation={} readyTarget={} phase={} currentWindowId={} preparedUsable={} staleReason={}",
                ageMs, event.getWindowId(), event.getHwnd(), event.getSequence(), event.getSource(),
                event.getOperation(), event.getTargetKeyword(), state.phase(),
                runtime == null ? null : runtime.getWindowId(), preparedUsable, staleReason);
    }

    private void yieldAfterMustYield(TaskExecutionContext context, WubeiStepOutcome outcome) {
        long delayMs = handoffDelayMs(outcome);
        if (delayMs > 0L) {
            log.info("[wubei] task turn handoff delay: result={} next={} source={} delayMs={}",
                    outcome.transactionResult(), outcome.nextState().phase(), outcome.nextState().source(), delayMs);
            TaskSleep.sleepOrStop(context, delayMs, "Wubei task interrupted");
        }
        WubeiWaitSpec waitSpec = outcome.waitSpec();
        if (waitSpec != null && !waitSpec.isAllowOpportunisticMaintenance()) {
            log.info("[wubei] opportunistic maintenance skipped by wait spec: reason={} next={} source={} message={}",
                    waitSpec.getReason(), outcome.nextState().phase(), outcome.nextState().source(), outcome.message());
            return;
        }
        maybeRunLeaderPathingSummonMaintenance(context, outcome);
    }

    private WubeiStepOutcome waitForPathingWake(WubeiStepOutcome outcome) {
        return outcome.withWaitSpec(WubeiWaitSpec.builder()
                .reason(WubeiWaitReason.WAIT_PATHING_TERMINAL)
                .wakeTypes(Set.of(
                        WindowReadyEventType.PATHING_TERMINAL,
                        WindowReadyEventType.PREPARED_ACTION_READY,
                        WindowReadyEventType.PRE_BATTLE_TIMEOUT))
                .timeoutMs(WUBEI_WAIT_UNTIL_RUNNER_EVENT_MS)
                .currentWindowOnly(true)
                .allowOpportunisticMaintenance(true)
                .build());
    }

    private WubeiStepOutcome waitForAcceptNpcRouteWake(WubeiStepOutcome outcome) {
        return outcome.withWaitSpec(WubeiWaitSpec.builder()
                .reason(WubeiWaitReason.WAIT_ACCEPT_NPC_ROUTE)
                .wakeTypes(Set.of(
                        WindowReadyEventType.PATHING_TERMINAL,
                        WindowReadyEventType.PREPARED_ACTION_READY,
                        WindowReadyEventType.PRE_BATTLE_TIMEOUT))
                .timeoutMs(WUBEI_WAIT_UNTIL_RUNNER_EVENT_MS)
                .currentWindowOnly(true)
                .allowOpportunisticMaintenance(false)
                .build());
    }

    private WubeiStepOutcome waitForPreparedDialogWake(WubeiStepOutcome outcome) {
        return outcome.withWaitSpec(WubeiWaitSpec.builder()
                .reason(WubeiWaitReason.WAIT_PREPARED_DIALOG)
                .wakeTypes(Set.of(
                        WindowReadyEventType.PREPARED_ACTION_READY,
                        WindowReadyEventType.PRE_BATTLE_TIMEOUT))
                .timeoutMs(WUBEI_WAIT_UNTIL_RUNNER_EVENT_MS)
                .currentWindowOnly(true)
                .allowOpportunisticMaintenance(false)
                .build());
    }

    private WubeiStepOutcome waitForCombatStateWake(WubeiStepOutcome outcome) {
        return outcome.withWaitSpec(WubeiWaitSpec.builder()
                .reason(WubeiWaitReason.WAIT_COMBAT_STATE_CHANGE)
                .wakeTypes(Set.of(WindowReadyEventType.COMBAT_STATE_CHANGED))
                .timeoutMs(wubeiCombatMaintenanceWakeTimeoutMs())
                .currentWindowOnly(true)
                .allowOpportunisticMaintenance(false)
                .build());
    }

    private long wubeiCombatMaintenanceWakeTimeoutMs() {
        long nextCombatWakeDelayMs = autoCombatService.nextCombatWakeDelayMs();
        long timeoutMs = nextCombatWakeDelayMs < 0L
                ? WUBEI_COMBAT_MAINTENANCE_WAKE_MAX_TIMEOUT_MS
                : Math.min(nextCombatWakeDelayMs, WUBEI_COMBAT_MAINTENANCE_WAKE_MAX_TIMEOUT_MS);
        timeoutMs = Math.max(timeoutMs, WUBEI_COMBAT_MAINTENANCE_WAKE_MIN_TIMEOUT_MS);
        log.debug("[wubei] WAIT_BATTLE_FINISH maintenance wake armed: timeoutMs={} nextCombatWakeDelayMs={}",
                timeoutMs, nextCombatWakeDelayMs);
        return timeoutMs;
    }

    /**
     * Park the 五倍 task after it has already released the task turn.
     *
     * @param context current task execution context for stop-aware waiting.
     * @param previousState phase state that produced the wait outcome.
     * @param outcome phase outcome that may carry a scheduling-only wait spec.
     */
    private WubeiStepOutcome parkAfterYieldIfNeeded(TaskExecutionContext context,
                                                   WubeiRoundContext previousState,
                                                   WubeiStepOutcome outcome) {
        WubeiWaitSpec waitSpec = outcome.waitSpec();
        if (waitSpec == null) {
            return outcome;
        }
        if (waitSpec.getReason() == WubeiWaitReason.WAIT_COMBAT_STATE_CHANGE
                && waitSpec.getTimeoutMs() < WUBEI_COMBAT_MAINTENANCE_WAKE_MIN_TIMEOUT_MS) {
            long originalTimeoutMs = waitSpec.getTimeoutMs();
            waitSpec = waitSpec.toBuilder()
                    .timeoutMs(wubeiCombatMaintenanceWakeTimeoutMs())
                    .build();
            log.warn("[wubei wait] corrected combat wake timeout before park: phase={} next={} originalTimeoutMs={} repairedTimeoutMs={} wakeTypes={} source={}",
                    previousState.phase(), outcome.nextState().phase(), originalTimeoutMs,
                    waitSpec.getTimeoutMs(), waitSpec.getWakeTypes(), outcome.message());
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            log.warn("[wubei wait] skip park: no window runtime phase={} next={} reason={}",
                    previousState.phase(), outcome.nextState().phase(), waitSpec.getReason());
            return outcome;
        }

        long startedAt = System.currentTimeMillis();
        long afterSequence = windowReadyEventBus.currentSequence();
        WubeiWaitRuntimeState before = captureWaitRuntimeState(runtime, waitSpec);
        if (isWaitAlreadySatisfied(waitSpec, before)) {
            if (waitSpec.getReason() == WubeiWaitReason.WAIT_PATHING_TERMINAL
                    && before.pathingTerminalMatchBasis() != null) {
                log.info("[wubei wait] late pathing terminal satisfied: phase={} next={} windowId={} source={} state={} sequence={} ageMs={} matchBasis={} afterSequence={}",
                        previousState.phase(), outcome.nextState().phase(), runtime.getWindowId(),
                        before.pathingTerminalSource(), before.pathingState(),
                        before.pathingTerminalSequence(), before.pathingTerminalAgeMs(),
                        before.pathingTerminalMatchBasis(), afterSequence);
            }
            log.info("[wubei wait] skip park; runtime already has wake state: phase={} next={} windowId={} reason={} wakeTypes={} afterSequence={} before={}",
                    previousState.phase(), outcome.nextState().phase(), runtime.getWindowId(),
                    waitSpec.getReason(), waitSpec.getWakeTypes(), afterSequence, before);
            return outcome;
        }

        if (!waitSpec.isCurrentWindowOnly()) {
            log.info("[wubei wait] cross-window wait requested but B2 only parks current window: phase={} next={} windowId={} reason={} wakeTypes={}",
                    previousState.phase(), outcome.nextState().phase(), runtime.getWindowId(),
                    waitSpec.getReason(), waitSpec.getWakeTypes());
        }

        if (waitSpec.getMinParkMs() > 0L) {
            TaskSleep.sleepOrStop(context, waitSpec.getMinParkMs(), "Wubei task interrupted");
        }

        Optional<WindowReadyEvent> wakeEvent = Optional.empty();
        EnumSet<WindowReadyEventType> wakeTypes = toWakeTypeEnumSet(waitSpec.getWakeTypes());
        if (!wakeTypes.isEmpty() && waitSpec.getTimeoutMs() != 0L) {
            wakeEvent = windowReadyEventBus.awaitNewer(
                    runtime.getWindowId(),
                    wakeTypes,
                    afterSequence,
                    waitSpec.getTimeoutMs());
        } else if (waitSpec.getTimeoutMs() > 0L) {
            TaskSleep.sleepOrStop(context, waitSpec.getTimeoutMs(), "Wubei task interrupted");
        }

        TaskPauseResumeFingerprint pauseFingerprint = taskPauseResumeReconciler.capture(
                context, TaskType.WUBEI, outcome.nextState().phase().name(), waitSpec.getReason().name());
        long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        TaskPauseResumeReconcileResult pauseReconcile = taskPauseResumeReconciler.reconcileAfterPause(
                pauseFingerprint, context, pauseBlockedMs);
        if (pauseReconcile.isFallbackTaskHotStart()) {
            WubeiRoundContext hotStart = resolvePauseResumeTaskHotStart(
                    outcome.nextState(), pauseReconcile, "event-wait:" + waitSpec.getReason());
            return WubeiStepOutcome.sharedState(hotStart,
                    "pause-resume fingerprint mismatch; task hot-start fallback");
        }
        if (pauseReconcile.isFingerprintMatched()) {
            compensateFormalMaintenanceTimers(pauseReconcile.getPauseBlockedMs(),
                    "wubei:event-wait:" + waitSpec.getReason());
        }
        WubeiWaitRuntimeState after = captureWaitRuntimeState(runtime, waitSpec);
        long elapsedMs = System.currentTimeMillis() - startedAt;
        boolean wokeByEvent = wakeEvent.isPresent();
        DialogOperation waitOperation = waitOperationForDiagnostics(before, after);
        String wakeResult = wokeByEvent ? "event" : "interrupted";
        log.info("[wubei wait] park finished: phase={} next={} windowId={} reason={} operation={} wakeTypes={} afterSequence={} timeoutMs={} minParkMs={} elapsedMs={} pauseBlockedMs={} fingerprintMatched={} mismatchReason={} compensatedTimers={} clearedVolatileState={} fallbackTaskHotStart={} wakeResult={} wakeType={} wakeSeq={} source={} before={} after={}",
                previousState.phase(), outcome.nextState().phase(), runtime.getWindowId(),
                waitSpec.getReason(), waitOperation, waitSpec.getWakeTypes(), afterSequence, waitSpec.getTimeoutMs(),
                waitSpec.getMinParkMs(), elapsedMs, pauseReconcile.getPauseBlockedMs(),
                pauseReconcile.isFingerprintMatched(), pauseReconcile.getMismatchReason(),
                pauseReconcile.getCompensatedTimers(), pauseReconcile.getClearedVolatileState(),
                pauseReconcile.isFallbackTaskHotStart(), wakeResult,
                wakeEvent.map(WindowReadyEvent::getType).orElse(null),
                wakeEvent.map(WindowReadyEvent::getSequence).orElse(-1L),
                outcome.message(), before, after);
        return outcome;
    }

    private DialogOperation waitOperationForDiagnostics(WubeiWaitRuntimeState before, WubeiWaitRuntimeState after) {
        if (after != null && after.preparedOperation() != null) {
            return after.preparedOperation();
        }
        return before == null ? null : before.preparedOperation();
    }

    private EnumSet<WindowReadyEventType> toWakeTypeEnumSet(Set<WindowReadyEventType> wakeTypes) {
        if (wakeTypes == null || wakeTypes.isEmpty()) {
            return EnumSet.noneOf(WindowReadyEventType.class);
        }
        return EnumSet.copyOf(wakeTypes);
    }

    private WubeiWaitRuntimeState captureWaitRuntimeState(WindowRuntimeContext runtime, WubeiWaitSpec waitSpec) {
        long now = System.currentTimeMillis();
        WindowPathingSnapshot pathing = runtime.getPathingSnapshot();
        PreparedDialogAction prepared = runtime.getPreparedDialogAction();
        WindowDialogSnapshot dialog = runtime.getVisibleDialogSnapshot(WUBEI_PREPARED_DIALOG_MAX_AGE_MS).orElse(null);
        Optional<WindowReadyEvent> latestTerminal = waitSpec != null
                && waitSpec.getReason() == WubeiWaitReason.WAIT_PATHING_TERMINAL
                ? windowReadyEventBus.latest(runtime.getWindowId(), WindowReadyEventType.PATHING_TERMINAL)
                : Optional.empty();
        PathingTerminalMatch terminalMatch =
                matchFreshPathingTerminal(waitSpec, runtime, pathing, latestTerminal.orElse(null), now);
        boolean satisfied = isWaitAlreadySatisfied(waitSpec, prepared) || terminalMatch.matched();
        long pathingAgeMs = pathing == null ? -1L : Math.max(0L, now - pathing.getUpdatedAtMs());
        long preparedAgeMs = prepared == null || prepared.getLastVerifiedAtMs() <= 0L
                ? -1L
                : Math.max(0L, now - prepared.getLastVerifiedAtMs());
        long dialogAgeMs = dialog == null || dialog.getDetectedAtMs() <= 0L
                ? -1L
                : Math.max(0L, now - dialog.getDetectedAtMs());
        return new WubeiWaitRuntimeState(
                pathing == null ? null : pathing.getState(),
                pathingAgeMs,
                prepared == null ? null : prepared.getOperation(),
                prepared == null ? null : prepared.getTargetKeyword(),
                preparedAgeMs,
                dialog == null ? null : dialog.getType(),
                dialogAgeMs,
                latestTerminal.map(WindowReadyEvent::getType).orElse(null),
                latestTerminal.map(event -> Math.max(0L, now - event.getCreatedAtMs())).orElse(-1L),
                terminalMatch.source(),
                terminalMatch.sequence(),
                terminalMatch.ageMs(),
                terminalMatch.matchBasis(),
                satisfied);
    }

    private boolean isWaitAlreadySatisfied(WubeiWaitSpec waitSpec, WubeiWaitRuntimeState state) {
        return state != null && state.satisfied();
    }

    private PathingTerminalMatch matchFreshPathingTerminal(WubeiWaitSpec waitSpec,
                                                           WindowRuntimeContext runtime,
                                                           WindowPathingSnapshot pathing,
                                                           WindowReadyEvent event,
                                                           long now) {
        if (waitSpec == null
                || waitSpec.getReason() != WubeiWaitReason.WAIT_PATHING_TERMINAL
                || runtime == null
                || pathing == null
                || !isTerminalPathingState(pathing.getState())) {
            return PathingTerminalMatch.none();
        }
        WindowPathingIntent activeIntent = pathing.getIntent();
        if (activeIntent == null
                || activeIntent.getSource() == null
                || !isWubeiPathingTerminalSource(activeIntent.getSource())) {
            return PathingTerminalMatch.none();
        }
        long snapshotAgeMs = Math.max(0L, now - pathing.getUpdatedAtMs());
        if (snapshotAgeMs > WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS
                || pathing.getUpdatedAtMs() + 1L < activeIntent.getCreatedAtMs()) {
            return PathingTerminalMatch.none();
        }
        if (event != null
                && event.getTaskType() == TaskType.WUBEI
                && event.getType() == WindowReadyEventType.PATHING_TERMINAL
                && isTerminalPathingState(event.getPathingState())
                && Objects.equals(event.getWindowId(), runtime.getWindowId())) {
            WindowPathingIntent eventIntent = event.getPathingIntent();
            long eventAgeMs = Math.max(0L, now - event.getCreatedAtMs());
            if (eventAgeMs <= WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS
                    && eventIntent != null
                    && Objects.equals(eventIntent.getIntentId(), activeIntent.getIntentId())
                    && eventIntent.getSource() != null
                    && Objects.equals(eventIntent.getSource(), activeIntent.getSource())
                    && isWubeiPathingTerminalSource(eventIntent.getSource())) {
                return new PathingTerminalMatch(
                        true,
                        eventIntent.getSource(),
                        event.getSequence(),
                        eventAgeMs,
                        "latest-event:intent-id");
            }
        }
        return new PathingTerminalMatch(
                true,
                activeIntent.getSource(),
                -1L,
                snapshotAgeMs,
                "current-snapshot:source");
    }

    private boolean isWubeiPathingTerminalSource(String source) {
        if (source == null || source.isBlank()) {
            return false;
        }
        if (source.startsWith(TRACKER_GREEN_PATHING_SOURCE_PREFIX)) {
            return true;
        }
        if (source.startsWith(POST_ACCEPT_PREPATH_SOURCE_PREFIX)
                || source.startsWith(DARK_THUNDER_REROLL_PREPATH_SOURCE_PREFIX)) {
            return true;
        }
        return "wubei:heal-pet-npc".equals(source)
                || "wubei:repair-equipment-npc".equals(source);
    }

    private boolean isTerminalPathingState(WindowPathingState state) {
        return state == WindowPathingState.ARRIVED || state == WindowPathingState.STOPPED_AWAY;
    }

    private boolean isWaitAlreadySatisfied(WubeiWaitSpec waitSpec, PreparedDialogAction prepared) {
        if (waitSpec == null || waitSpec.getReason() == null) {
            return false;
        }
        return switch (waitSpec.getReason()) {
            case WAIT_PREPARED_DIALOG, WAIT_PATHING_TERMINAL, WAIT_ACCEPT_NPC_ROUTE ->
                    hasFreshPreparedAction(prepared);
            case WAIT_COMBAT_STATE_CHANGE, WAIT_TEAM_ATTENTION, WAIT_RETRY_TIMER -> false;
        };
    }

    private boolean hasFreshPreparedAction(PreparedDialogAction prepared) {
        return prepared != null
                && prepared.verifiedWithin(System.currentTimeMillis(), WUBEI_PREPARED_DIALOG_MAX_AGE_MS);
    }

    private long handoffDelayMs(WubeiStepOutcome outcome) {
        WubeiRoundContext nextState = outcome.nextState();
        if ("prepared action priority yield".equals(outcome.message())) {
            return READY_EVENT_PRIORITY_YIELD_DELAY_MS;
        }
        if (nextState != null
                && nextState.source() != null
                && nextState.source().endsWith("-broadcast-handled")) {
            long delayMs = MAINTENANCE_BROADCAST_HANDOFF_DELAY_MS;
            log.info("[wubei] maintenance broadcast handoff delay: source={} delayMs={}",
                    nextState.source(), delayMs);
            compensateFormalMaintenanceTimers(delayMs, nextState.source() + ":handoff-delay");
            return delayMs;
        }
        if (nextState != null && isChainedPostBattleBroadcastSource(nextState.source())) {
            log.info("[wubei] chained post-battle first-aid broadcast delay: source={} delayMs={}",
                    nextState.source(), CHAINED_POST_BATTLE_FIRST_AID_BROADCAST_MS);
            return CHAINED_POST_BATTLE_FIRST_AID_BROADCAST_MS;
        }
        if (nextState != null
                && nextState.source() != null
                && nextState.source().startsWith(TEAM_RETURN_WAIT_SOURCE_PREFIX)) {
            long configured = botProperties.getReturnTeamLeaderWaitPollMs();
            return configured > 0 ? configured : 3_000L;
        }
        return 0L;
    }

    private boolean isChainedPostBattleBroadcastSource(String source) {
        return source != null && source.startsWith(CHAINED_POST_BATTLE_BROADCAST_SOURCE_PREFIX);
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
        boolean dialogVisibleBeforeMaintenance = hasDialogBeforeLeaderPathingSummon(nextState);
        if (!taskMaintenanceService.isTeamPathingMaintenanceWindowOpen(context, TASK_CODE)) {
            log.info("[wubei] leader pathing summon maintenance skipped: team pathing window closed round={} source={}",
                    nextState.round(), nextState.source());
            return;
        }

        boolean ran = taskTurnCoordinator.tryRun("wubei:leaderPathingSummonMaintenance", () -> {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            long maintenanceStartedAt = System.currentTimeMillis();
            TaskMaintenanceResult result = taskMaintenanceService.runOpportunisticMaintenance(context,
                    TaskMaintenanceRequest.builder()
                            .sourceTask("wubei:leader-pathing")
                            .handleMaintenanceBroadcast(false)
                            .cleanSummonSkill(true)
                            .enqueueSummonSkillOnly(dialogVisibleBeforeMaintenance)
                            .requireFreeStateForSummonSkill(false)
                            .oneSummonSkillPerTeamRound(true)
                            .maxSummonSkillCleanersPerTeamRound(1)
                            .teamMaintenanceKey(TASK_CODE)
                            .teamRound(nextState.round())
                            .requireOpenTeamMaintenanceWindow(true)
                            .build());
            compensateFormalMaintenanceTimers(System.currentTimeMillis() - maintenanceStartedAt,
                    "wubei:leader-pathing:summon-maintenance");
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
        log.info("[wubei] leader pathing summon maintenance limited to enqueue-only: runner dialog visible round={} phase={} source={} type={} dialogSource={}",
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

        log.warn("[wubei] probe task exceeded enter-battle timeout; route back and reaccept: "
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
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "probe-enter-battle-timeout-reaccept"),
                "probe enter battle timeout; route back and reaccept");
    }

    private void compensateProbeTimersAfterPause(long blockedMs, String source) {
        if (blockedMs < PAUSE_TIMER_COMPENSATION_THRESHOLD_MS || currentProbeTaskStartedAt <= 0L) {
            return;
        }
        currentProbeTaskStartedAt += blockedMs;
        if (currentProbeStoryWaitStartedAt > 0L) {
            currentProbeStoryWaitStartedAt += blockedMs;
        }
        log.info("[wubei] probe timer paused: source={} blockedMs={} adjustedTaskStartAt={} adjustedStoryWaitStartAt={}",
                source, blockedMs, currentProbeTaskStartedAt, currentProbeStoryWaitStartedAt);
    }

    private void compensateEnterBattleTimersAfterPause(long blockedMs, String source) {
        if (blockedMs < PAUSE_TIMER_COMPENSATION_THRESHOLD_MS || enterBattleStartedAt <= 0L) {
            return;
        }
        enterBattleStartedAt += blockedMs;
        if (enterBattleNextRetryAt > 0L) {
            enterBattleNextRetryAt += blockedMs;
        }
        log.info("[wubei] enter battle timer paused: source={} blockedMs={} adjustedStartAt={} adjustedNextRetryAt={}",
                source, blockedMs, enterBattleStartedAt, enterBattleNextRetryAt);
    }

    private void compensateWaitBattleTimersAfterPause(long blockedMs, String source) {
        if (blockedMs < PAUSE_TIMER_COMPENSATION_THRESHOLD_MS || waitBattleStartedAt <= 0L) {
            return;
        }
        waitBattleStartedAt += blockedMs;
        if (waitBattleNextTrackerRetryAt > 0L) {
            waitBattleNextTrackerRetryAt += blockedMs;
        }
        log.info("[wubei] wait battle timer paused: source={} blockedMs={} adjustedStartAt={} adjustedNextRetryAt={}",
                source, blockedMs, waitBattleStartedAt, waitBattleNextTrackerRetryAt);
    }

    private void compensateFormalMaintenanceTimers(long blockedMs, String source) {
        if (blockedMs < PAUSE_TIMER_COMPENSATION_THRESHOLD_MS) {
            return;
        }
        compensateWaitBattleTimersAfterPause(blockedMs, source);
        compensateProbeTimersAfterPause(blockedMs, source);
        compensateEnterBattleTimersAfterPause(blockedMs, source);
        windowTaskContextHolder.rawCurrent().ifPresent(runtime -> {
            if (runtime.pauseOrdinaryPreBattleTimer(blockedMs, source)) {
                log.info("[wubei ordinary-prebattle] timer paused: source={} blockedMs={} adjustedStartAt={}",
                        source, blockedMs, runtime.getOrdinaryPreBattleStartedAtMs());
            }
        });
    }

    private WubeiStepOutcome runPhase(TaskExecutionContext context, WubeiRoundContext state) {
        WubeiStepOutcome probeTimeout = timeoutProbeTaskBeforeBattleIfNeeded(context, state);
        if (probeTimeout != null) {
            return probeTimeout;
        }
        return switch (state.phase()) {
            case HOT_START_DETECT -> runHotStartDetectPhase(context, state);
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

    private WubeiStepOutcome runHotStartDetectPhase(TaskExecutionContext context, WubeiRoundContext state) {
        if (shouldYieldForTeamReturnSignal()) {
            log.warn("[wubei] team return signal present before accept flow; yield for members");
            return WubeiStepOutcome.sharedState(
                    state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_BEFORE_ACCEPT_SOURCE),
                    "team return pending before accept flow");
        }
            currentTrackerPanel = resolveTrackerPanelWithAnchorRecovery(null, state);
        if (currentTrackerPanel.isFound() && !currentTrackerPanel.getGreenLinks().isEmpty()) {
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.READ_TRACKER, "hot-start-active-task"),
                    "accepted task found from tracker panel");
        }
        if (context != null && context.isAfterCombatExitStartup()) {
            log.info("[wubei] after-combat startup tracker missed; verify return item before accept flow");
            if (useReturnItemAndVerifyStartMap(context, "after-combat-exit-startup")
                    == ReturnHomeResult.VERIFIED) {
                return WubeiStepOutcome.continueTo(
                        state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_ROUND_DONE_SOURCE),
                        "after-combat startup return item verified; check team return");
            }
            log.info("[wubei] after-combat startup return item unavailable or unverified; fall back to accept flow");
        }
        return WubeiStepOutcome.continueTo(
            state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "hot-start-no-active-task"),
            "no accepted task found");
    }

    private WubeiStepOutcome runAfterAcceptMaintenanceCheck(TaskExecutionContext context, WubeiRoundContext state) {
        /*
         * CR120: leader common-box pending only lives for 30s after return-home detection. Consume
         * it before any maintenance broadcast or handoff can spend that TTL.
         */
        consumeCommonBoxAfterTaskAccepted(context, "wubei:after-accept-maintenance-check");
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
        consumeCommonBoxAfterTaskAccepted(context, "wubei:before-tracker-pathing-maintenance-check");
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
                return waitForPathingWake(WubeiStepOutcome.pathingStarted(
                        activeState.waitForPathing(hookName + "-npc-pathing-started"),
                        hookName + " NPC pathing started"));
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

            long maintenanceStartedAt = System.currentTimeMillis();
            TaskMaintenanceResult maintenanceResult = taskMaintenanceService.runOpportunisticMaintenance(context,
                    TaskMaintenanceRequest.builder()
                            .sourceTask("wubei:" + hookName + "-broadcast")
                            .handleMaintenanceBroadcast(true)
                            .cleanSummonSkill(false)
                            .build());
            compensateFormalMaintenanceTimers(System.currentTimeMillis() - maintenanceStartedAt,
                    "wubei:" + hookName + "-broadcast");
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
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot snapshot = currentWindowPathingSnapshot();
        WindowPathingState pathingState = snapshot == null ? null : snapshot.getState();
        WindowPathingIntent snapshotIntent = snapshot == null ? null : snapshot.getIntent();
        WindowPathingIntent activeIntent = runtime == null ? null : runtime.getActivePathingIntent().orElse(null);
        String expectedSource = "wubei:" + hookName + "-npc";
        WindowPathingIntent expectedIntent = activeIntent != null
                && expectedSource.equals(activeIntent.getSource())
                ? activeIntent
                : snapshotIntent != null && expectedSource.equals(snapshotIntent.getSource()) ? snapshotIntent : null;
        if (pathingState == WindowPathingState.ARRIVED || pathingState == WindowPathingState.STOPPED_AWAY) {
            clearCurrentPathingSignal("wubei " + hookName + " maintenance consumed pathing terminal: " + pathingState);
            log.info("[wubei] {} maintenance runner-only pathing terminal: phase={} state={} current={}({}, {}) message={}",
                    hookName, state.phase(), pathingState, snapshot.getCurrentMapName(),
                    snapshot.getCurrentX(), snapshot.getCurrentY(), snapshot.getMessage());
            return null;
        }
        long intentAgeMs = expectedIntent == null
                ? -1L
                : Math.max(0L, System.currentTimeMillis() - expectedIntent.getCreatedAtMs());
        if (intentAgeMs >= WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS) {
            clearCurrentPathingSignal("wubei " + hookName + " maintenance runner-only hard timeout");
            log.warn("[wubei] {} maintenance runner-only pathing hard timeout before keep-wait: phase={} expectedSource={} source={} intentId={} target={} ageMs={} timeoutMs={} hasSnapshot={} state={} probeInProgress={}",
                    hookName, state.phase(), expectedSource, expectedIntent.getSource(), expectedIntent.getIntentId(),
                    expectedIntent.getTargetMapName(), intentAgeMs, WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS,
                    snapshot != null, pathingState, snapshot != null && snapshot.isProbeInProgress());
            return null;
        }
        if (snapshot == null || pathingState == WindowPathingState.NONE
                || pathingState == WindowPathingState.ACTIVE
                || pathingState == WindowPathingState.UNKNOWN || snapshot.isProbeInProgress()) {
            log.info("[wubei] {} maintenance runner-only pathing wait continues: phase={} expectedSource={} hasSnapshot={} state={} probeInProgress={} intent={} intentAgeMs={} timeoutMs={}",
                    hookName, state.phase(), expectedSource, snapshot != null, pathingState,
                    snapshot != null && snapshot.isProbeInProgress(),
                    expectedIntent, intentAgeMs, WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS);
            return waitForPathingWake(WubeiStepOutcome.pathingStarted(
                    state,
                    hookName + " maintenance runner-only pathing wait"));
        }
        log.info("[wubei] {} maintenance runner-only pathing wait ended without local movement fallback: phase={} state={}",
                hookName, state.phase(), pathingState);
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
        currentTrackerPanel = resolveTrackerPanelForReadPhase(context, state);
        if (!currentTrackerPanel.isFound()) {
            return WubeiStepOutcome.failed(state, "tracker anchor not found");
        }
        if (isTrackerDarkThunderTask(currentTrackerPanel)) {
            log.info("[wubei] dark-thunder task detected by title template; reroll by accepting task again: taskKey={} title={} yellow='{}'",
                    trackerTaskKey(currentTrackerPanel), trackerTaskTitle(currentTrackerPanel),
                    currentTrackerPanel.getYellowText());
            NavigationResult rerouteResult = startDarkThunderAcceptNpcReroute(context, state);
            currentTrackerPanel = null;
            postAcceptTrackerPanelFuture = null;
            currentTrackerDestinationHint = null;
            boolean prepathCleared = clearPostAcceptPrepathSignal("wubei dark thunder reroll after tracker read");
            log.info("[wubei] dark-thunder reroll immediate accept-NPC reroute submitted: status={} message={} prepathCleared={} source={}",
                    rerouteResult.getStatus(), rerouteResult.getMessage(), prepathCleared, state.source());
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "dark-thunder-reroll"),
                    "dark thunder reroll");
        }
        currentRoundChainedCombatExpected = isTrackerChainedCombatTask(currentTrackerPanel);
        currentRoundChainedCombatContinueCount = 0;
        currentRoundChainedCombatRecoveryBroadcastCount = 0;
        if (isTrackerProbeTask(currentTrackerPanel)) {
            currentProbeTaskStartedAt = System.currentTimeMillis();
            log.info("[wubei] probe task timer started by title template: round={} timeoutMs={} taskKey={} title={} yellow='{}'",
                    state.round(), PROBE_ENTER_BATTLE_TIMEOUT_MS, trackerTaskKey(currentTrackerPanel),
                    trackerTaskTitle(currentTrackerPanel), currentTrackerPanel.getYellowText());
        } else {
            currentProbeTaskStartedAt = 0L;
        }
        log.info("[wubei] tracker snapshot ready: taskKey={} title={} yellow='{}' probeTask={} chainedCombatExpected={}",
                trackerTaskKey(currentTrackerPanel),
                trackerTaskTitle(currentTrackerPanel),
                currentTrackerPanel.getYellowText(),
                isTrackerProbeTask(currentTrackerPanel),
                currentRoundChainedCombatExpected);
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.AFTER_ACCEPT_MAINTENANCE_CHECK, "tracker-ready"),
                "tracker objective ready");
    }

    /**
     * Reverse the post-accept prepath as soon as 暗雷怪 is recognized.
     *
     * @param context current task execution context, used only for stop/interrupt handling.
     * @param state current round state used for a distinct pathing source.
     * @return current-map navigation result. Failure is not fatal here; the next
     *         ROUTE_TO_MAIN_TASK phase still performs the normal accept-NPC route.
     */
    private NavigationResult startDarkThunderAcceptNpcReroute(TaskExecutionContext context, WubeiRoundContext state) {
        NavigationResult result = navigationService.navigateInCurrentMap(NavigationRequest.builder()
                .targetMapName(START_MAP_NAME)
                .targetX(ACCEPT_NPC_X)
                .targetY(ACCEPT_NPC_Y)
                .targetName(ACCEPT_NPC_NAME)
                .arrivalTolerance(ACCEPT_NPC_DIRECT_CLICK_DISTANCE)
                .miniMapClickRandomRadiusPx(PREPATH_MINI_MAP_CLICK_RANDOM_RADIUS_PX)
                .source(DARK_THUNDER_REROLL_PREPATH_SOURCE_PREFIX + state.round())
                .build());
        if (result.getStatus() == NavigationResultStatus.STOPPED
                || result.getStatus() == NavigationResultStatus.INTERRUPTED) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            throw new TaskStopRequestedException("Wubei task interrupted");
        }
        log.info("[wubei] dark-thunder immediate accept-NPC current-map reroute result: round={} status={} message={}",
                state.round(), result.getStatus(), result.getMessage());
        return result;
    }

    private WubeiStepOutcome runTrackerPathingPhase(TaskExecutionContext context, WubeiRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        if (currentTrackerPanel == null || !currentTrackerPanel.isFound()) {
            return WubeiStepOutcome.failed(state, "tracker pathing failed");
        }
        if (isTrackerProbeTask(currentTrackerPanel)) {
            if (!startProbeTrackerPathing(context, currentTrackerPanel)) {
                return WubeiStepOutcome.failed(state, "probe tracker pathing failed");
            }
            return waitForPathingWake(WubeiStepOutcome.pathingStarted(
                    state.next(WubeiPhase.RESOLVE_AFTER_PATHING, "probe-pathing-started"),
                    "probe tracker pathing started"));
        }
        if (!triggerCombatTrackerPathing(context, currentTrackerPanel, "combat")) {
            return WubeiStepOutcome.failed(state, "tracker pathing failed");
        }
        windowTaskContextHolder.rawCurrent().ifPresent(runtime ->
                runtime.startOrdinaryPreBattleTimer(
                        TaskType.WUBEI,
                        currentRoundChainedCombatExpected
                                ? "wubei:first-chained-green-click"
                                : "wubei:first-ordinary-green-click",
                        currentTrackerPanel.getYellowText(),
                        System.currentTimeMillis()));
        return waitForPathingWake(WubeiStepOutcome.pathingStarted(
                state.next(WubeiPhase.RESOLVE_AFTER_PATHING, "tracker-pathing-started"),
                "tracker pathing started"));
    }

    private WubeiStepOutcome runResolveAfterPathingPhase(TaskExecutionContext context, WubeiRoundContext state) {
        if (isProbeRuntimeActive()) {
            try {
                return resolveProbeAfterPathing(context, state);
            } catch (ProbeEnterBattleTimeoutSignal timeout) {
                return timeout.outcome();
            }
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
            return waitForPathingWake(WubeiStepOutcome.sharedState(
                    state,
                    "waiting tracker runner pathing snapshot"));
        }
        WindowPathingState pathingState = snapshot.getState();
        log.info("[wubei] tracker pathing snapshot consumed: state={} current={}({}, {}) probeInProgress={} message={}",
                pathingState, snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                snapshot.isProbeInProgress(), snapshot.getMessage());
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (pathingState == WindowPathingState.ACTIVE || snapshot.isProbeInProgress()) {
            returnItemPrescanService.whilePathing(context, TASK_CODE, state.round(), RETURN_ITEM_TEMPLATE,
                    ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                    "wubei:tracker-pathing-active");
            return waitForPathingWake(WubeiStepOutcome.sharedState(
                    state,
                    "tracker runner pathing still active"));
        }
        if (pathingState == WindowPathingState.UNKNOWN) {
            log.warn("[wubei] tracker runner pathing unknown; continue phase recovery instead of yielding forever: current={}({}, {}) message={}",
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(), snapshot.getMessage());
        }
        if (pathingState == WindowPathingState.ARRIVED
                || pathingState == WindowPathingState.STOPPED_AWAY) {
            clearCurrentPathingSignal("wubei consumed tracker pathing terminal snapshot: " + pathingState);
            if (currentTrackerPanel == null || !currentTrackerPanel.isFound()
                    || currentTrackerPanel.getGreenLinks().isEmpty()) {
                log.warn("[wubei] ordinary pathing terminal cannot re-click tracker green: state={} hasPanel={} found={} links={} yellow='{}'",
                        pathingState,
                        currentTrackerPanel != null,
                        currentTrackerPanel != null && currentTrackerPanel.isFound(),
                        currentTrackerPanel == null ? null : currentTrackerPanel.getGreenLinks().size(),
                        currentTrackerPanel == null ? null : currentTrackerPanel.getYellowText());
                return WubeiStepOutcome.failed(state, "ordinary pathing terminal missing tracker green link");
            }
            log.info("[wubei] ordinary pathing terminal re-clicks same tracker green: state={} yellow='{}' links={}",
                    pathingState, currentTrackerPanel.getYellowText(), currentTrackerPanel.getGreenLinks().size());
            if (!triggerCombatTrackerPathing(context, currentTrackerPanel, "combat-terminal-repath")) {
                return WubeiStepOutcome.failed(state, "ordinary pathing terminal tracker green re-click failed");
            }
            return waitForPathingWake(WubeiStepOutcome.pathingStarted(
                    state.retrySamePhase("ordinary-pathing-terminal-reclick"),
                    "ordinary pathing terminal re-clicked tracker green"));
        }
        log.warn("[wubei] ordinary pathing snapshot reached unexpected non-active state: state={} message={}",
                pathingState, snapshot.getMessage());
        return waitForPathingWake(WubeiStepOutcome.sharedState(
                state,
                "ordinary pathing waits for runner terminal"));
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
        if (outcome.nextState().phase() == WubeiPhase.WAIT_BATTLE_FINISH) {
            windowTaskContextHolder.rawCurrent()
                    .ifPresent(runtime -> runtime.clearOrdinaryPreBattleTimer(
                            "wubei entered WAIT_BATTLE_FINISH from ENTER_BATTLE"));
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
         * 黄袍怪是例外：脱战后先开 5 秒队员补血/补蓝窗口，同时队长后台预判 tracker 和自己的血蓝。
         */
        if (currentRoundChainedCombatExpected) {
            int combatCount = currentRoundChainedCombatContinueCount + 1;
            openChainedPostBattleFirstAidWindowAndProbeLeader(context, state, combatCount);
            return WubeiStepOutcome.sharedState(
                    state.next(WubeiPhase.RETURN_HOME,
                            CHAINED_POST_BATTLE_BROADCAST_SOURCE_PREFIX + "-" + combatCount),
                    "chained combat post battle; follower first-aid window and leader precheck");
        }
        TaskSleep.sleepOrStop(context, 800L, "Wubei task interrupted");
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.RETURN_HOME, "post-battle-recovered"),
                "post battle recovery window");
    }

    private void openChainedPostBattleFirstAidWindowAndProbeLeader(TaskExecutionContext context,
                                                                   WubeiRoundContext state,
                                                                   int combatCount) {
        if (currentRoundChainedCombatRecoveryBroadcastCount < combatCount) {
            currentRoundChainedCombatRecoveryBroadcastCount = combatCount;
            taskMaintenanceService.openTeamFirstAidMaintenanceWindow(context, TASK_CODE, currentRoundNumber,
                    "wubei:chained-combat-post-battle-first-aid-precheck");
        } else {
            log.info("[wubei] chained combat first-aid window already opened: count={} recordedCount={}",
                    combatCount, currentRoundChainedCombatRecoveryBroadcastCount);
        }
        PlayerStateService.FirstAidNoFocusProbeResult leaderProbe =
                playerStateService.probeFirstAidSupplyNoFocus(context);
        log.info("[wubei] chained combat leader no-focus first-aid precheck: round={} count={} result={} nextSource={}",
                state.round(), combatCount, leaderProbe,
                CHAINED_POST_BATTLE_BROADCAST_SOURCE_PREFIX + "-" + combatCount);
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
        if (!TEAM_RETURN_BEFORE_ACCEPT_SOURCE.equals(state.source())) {
            TeamReturnService.LeaderSignalPrecheckStatus precheck =
                    teamReturnService.consumeLeaderSignalPrecheck(
                            context, pendingTeamReturnPrecheck, "wubei:" + state.source());
            pendingTeamReturnPrecheck = null;
            if (precheck.conclusive() && !precheck.signalPresent()) {
                taskMaintenanceService.closeLocalTeamReturnSupportWindow(context,
                        "wubei:" + state.source() + ":precheck-not-needed");
                log.info("[wubei] team return precheck says no wait needed: source={}", state.source());
                return WubeiStepOutcome.continueTo(
                        state.next(WubeiPhase.ROUND_DONE, "team-return-precheck-not-needed"),
                        "team return wait not needed");
            }
            if (precheck.conclusive() && precheck.signalPresent()) {
                taskMaintenanceService.openLocalTeamReturnSupportWindow(context,
                        "wubei:" + state.source() + ":precheck-signal-present");
                log.warn("[wubei] team return precheck saw return signal; yield for members source={}",
                        state.source());
                return WubeiStepOutcome.sharedState(
                        state.next(WubeiPhase.WAIT_TEAM_RETURN, keepTeamReturnWaitSource(state)),
                        "team return still pending");
            }
        }
        if (shouldYieldForTeamReturnSignal()) {
            taskMaintenanceService.openLocalTeamReturnSupportWindow(context,
                    "wubei:" + state.source() + ":signal-present");
            log.warn("[wubei] team return signal still present; yield for members source={}", state.source());
            return WubeiStepOutcome.sharedState(
                    state.next(WubeiPhase.WAIT_TEAM_RETURN, keepTeamReturnWaitSource(state)),
                    "team return still pending");
        }
        taskMaintenanceService.closeLocalTeamReturnSupportWindow(context,
                "wubei:" + state.source() + ":signal-cleared");
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

        LocationInfo returnHomeSnapshot = verifiedReturnHomeLocation;
        NavigationResult nav = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(START_MAP_NAME)
                .targetX(ACCEPT_NPC_X)
                .targetY(ACCEPT_NPC_Y)
                .targetName(ACCEPT_NPC_NAME)
                .arrivalTolerance(ACCEPT_NPC_DIRECT_CLICK_DISTANCE)
                .source("wubei:accept-npc")
                .freshCurrentMapName(returnHomeSnapshot == null ? null : returnHomeSnapshot.mapName)
                .freshCurrentX(returnHomeSnapshot == null ? null : returnHomeSnapshot.x)
                .freshCurrentY(returnHomeSnapshot == null ? null : returnHomeSnapshot.y)
                .freshCurrentLocationAtMs(returnHomeSnapshot == null ? 0L : System.currentTimeMillis())
                .freshCurrentLocationPhaseBound(returnHomeSnapshot != null)
                .build());

        if (nav.getStatus() == NavigationResultStatus.PATHING_STARTED) {
            return waitForAcceptNpcRouteWake(WubeiStepOutcome.pathingStarted(
                state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "accept-npc-pathing-started"),
                "accept NPC pathing started"));
        }

        if (nav.getStatus() == NavigationResultStatus.DIALOG_PREPARING) {
            return waitForPreparedDialogWake(WubeiStepOutcome.sharedState(
                state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, "accept-npc-dialog-preparing"),
                "accept NPC dialog preparing"));
        }

        if (nav.getStatus() != NavigationResultStatus.ARRIVED) {
            log.warn("[wubei] accept NPC navigation not arrived: status={} message={}",
                nav.getStatus(), nav.getMessage());
            return WubeiStepOutcome.failed(
                state,
                "accept NPC navigation not arrived: " + nav.getStatus() + " " + nav.getMessage());
        }

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
             * ACTIVE. Keep this gate strict: only a freshly prepared, matching route-transfer click
             * may release the accept-NPC pathing wait. Generic visible dialogs or preparation
             * requests are not enough business evidence here.
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
        return waitForAcceptNpcRouteWake(WubeiStepOutcome.pathingStarted(
                state.retrySamePhase("accept-npc-pathing-wait"),
                "accept NPC pathing still active"));
    }

    private WubeiStepOutcome runAcceptTaskPhase(TaskExecutionContext context, WubeiRoundContext state) {
        boolean acceptNpcClicked = state.waitingAcceptDialog();
        if (!acceptNpcClicked) {
            windowTaskContextHolder.rawCurrent()
                    .ifPresent(runtime -> runtime.clearDialogInterest("wubei accept cycle starting"));
        }
        DialogResult result = tryConsumePreparedWubeiDialog(
                DialogOperation.WUBEI_ACCEPT_TASK,
                state.source() + ":prepared-accept",
                true);
        if (result == null && acceptNpcClicked) {
            /*
             * The quest NPC has already been clicked, so this foreground turn owns the follow-up
             * accept click. Keep the turn and wait for the runner-prepared option instead of
             * yielding to other windows; otherwise the visible accept dialog can sit idle while
             * unrelated windows take the task turn.
             */
            log.info("[wubei] accept NPC already clicked; hold turn and wait for prepared accept dialog: source={} waitMs={}",
                    state.source(), WUBEI_ACCEPT_DIALOG_FOREGROUND_WAIT_MS);
            result = waitForPreparedWubeiDialog(
                    context,
                    DialogOperation.WUBEI_ACCEPT_TASK,
                    state.source() + ":prepared-accept-waiting",
                    true,
                    WUBEI_ACCEPT_DIALOG_FOREGROUND_WAIT_MS);
        }
        if (result == null && !acceptNpcClicked) {
            consumeCommonBoxAfterTaskAccepted(context, "wubei:before-accept-npc-click");
            NpcClickRequest acceptRequest = ACCEPT_NPC.toClickRequest(gameContext.getMe(), TaskType.WUBEI);
            boolean clicked = npcClickService.clickNpcSmart(acceptRequest);
            if (!clicked) {
                log.warn("[wubei] accept NPC smart-click failed; wait for runner prepared accept dialog");
            } else {
                acceptNpcClicked = true;
            }
            result = waitForPreparedWubeiDialog(
                    context,
                    DialogOperation.WUBEI_ACCEPT_TASK,
                    state.source() + ":prepared-accept-after-npc",
                    true,
                    WUBEI_ACCEPT_DIALOG_FOREGROUND_WAIT_MS);
        }
        if (result == null) {
            /*
             * Accept-task dialog handling is part of the same foreground interaction that clicked
             * the NPC. If the runner cannot prepare the accept option within the interest TTL,
             * fail the phase and let normal recovery reopen/clean the dialog rather than yielding
             * and letting other windows steal time from an already-open accept dialog.
             */
            log.warn("[wubei] accept option not prepared before foreground timeout: source={} clickedNpc={} waitMs={}",
                    state.source(), acceptNpcClicked, WUBEI_ACCEPT_DIALOG_FOREGROUND_WAIT_MS);
            return WubeiStepOutcome.failed(state, "accept dialog preparation timeout");
        }
        if (result.getStatus() == DialogResultStatus.GREEN_TEMPLATE_CLICKED
                && OPTION_ACCEPT_TASK.equals(result.getActionKey())
                && result.getRelativeX() != null
                && result.getRelativeY() != null) {
            memoryService.recordDialogChoiceSuccess(
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
        return afterAcceptTaskSucceeded(
                context,
                state,
                result,
                state.source() + ":prepared accept consumed",
                "task-accepted",
                "task accepted");
    }

    private WubeiStepOutcome afterAcceptTaskSucceeded(TaskExecutionContext context,
                                                      WubeiRoundContext state,
                                                      DialogResult result,
                                                      String confirmSource,
                                                      String nextSource,
                                                      String message) {
        npcClickService.confirmPendingSmartClick(
                START_MAP_NAME, ACCEPT_NPC_NAME, ACCEPT_NPC_X, ACCEPT_NPC_Y,
                "DIALOG_TEMPLATE", confirmSource);
        clearVerifiedReturnHomeLocation("accept option clicked: " + confirmSource);
        /*
         * 接任务成功后马上启动后台 tracker 读图计时，再做 Alt+C/小地图预走路。
         * 这样 1 秒后的截图和模板匹配不被前台小地图点击串行卡住；后台线程会显式
         * 绑定当前窗口上下文，避免多开时截图落到别的窗口。
         */
        postAcceptTrackerPanelFuture = schedulePostAcceptTrackerPanelRead(state);
        startPostAcceptPrepath(context, state);
        currentTrackerPanel = null;
        log.info("[wubei] accept task clicked; post-accept prepath started and tracker background read scheduled: waitMs={} source={} status={} action={} click=({}, {})",
                TRACKER_REFRESH_AFTER_ACCEPT_MS, state.source(), result.getStatus(), result.getActionKey(),
                result.getAbsoluteX(), result.getAbsoluteY());
        return WubeiStepOutcome.continueTo(
                state.next(WubeiPhase.READ_TRACKER, nextSource),
                message);
    }

    private CompletableFuture<TaskTrackerPanelReadResult> schedulePostAcceptTrackerPanelRead(WubeiRoundContext state) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        String source = "wubei-post-accept-tracker-" + state.round() + "-" + state.source();
        long scheduledAt = System.currentTimeMillis();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(TRACKER_REFRESH_AFTER_ACCEPT_MS);
                long readStartedAt = System.currentTimeMillis();
                TaskTrackerPanelReadResult result = windowTaskContextHolder.callWith(runtime,
                        () -> taskTrackerPanelService.readWubeiTrackerPanel(source));
                long elapsedMs = System.currentTimeMillis() - scheduledAt;
                log.info("[wubei] post-accept background tracker read completed: round={} source={} windowId={} found={} taskKey={} title={} links={} elapsedMs={} readMs={}",
                        state.round(), source, runtime == null ? null : runtime.getWindowId(),
                        result != null && result.isFound(), trackerTaskKey(result), trackerTaskTitle(result),
                        result == null || result.getGreenLinks() == null ? 0 : result.getGreenLinks().size(),
                        elapsedMs, System.currentTimeMillis() - readStartedAt);
                return result == null ? TaskTrackerPanelReadResult.empty() : result;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[wubei] post-accept background tracker read interrupted: round={} source={}",
                        state.round(), source);
                return TaskTrackerPanelReadResult.empty();
            } catch (Exception e) {
                log.warn("[wubei] post-accept background tracker read failed: round={} source={}",
                        state.round(), source, e);
                return TaskTrackerPanelReadResult.empty();
            }
        });
    }

    /**
     * Start 五倍's first post-accept in-map movement before the slower tracker/maintenance work.
     *
     * <p>The first coordinate is only a prepath target. When 医宝宝 is due, it replaces the usual
     * 宝象国出口 target so the leader naturally walks toward 沙拉买提 first. Repair-only does not
     * replace this coordinate; the existing repair route still happens later while the leader is
     * already on the way out.</p>
     *
     * @param context current task execution context for stop-aware sleeps/checkpoints.
     * @param state current round state, used for diagnostics and source tags.
     */
    private void startPostAcceptPrepath(TaskExecutionContext context, WubeiRoundContext state) {
        WubeiPrepathTarget target = computePostAcceptPrepathTarget();
        log.info("[wubei] post-accept prepath target selected: round={} target={} map={} coord=({}, {}) reason={} source={}",
                state.round(), target.name(), target.mapName(), target.x(), target.y(),
                target.reason(), state.source());

        boolean skipAltC = shouldSkipPostAcceptAltCForStartupFlying(state);
        if (!skipAltC) {
            boolean dismounted = inputSequences.submitAndWait("wubei:post-accept-prepath:alt-c", List.of(
                    InputAction.pressAltC(),
                    InputAction.sleep(120)
            ));
            if (!dismounted) {
                log.warn("[wubei] post-accept prepath Alt+C was not submitted; continue with normal tracker flow: round={} target={} reason={}",
                        state.round(), target.name(), target.reason());
                return;
            }
        } else {
            log.info("[wubei] post-accept prepath skips Alt+C because startup already confirmed flying: round={} target={} reason={}",
                    state.round(), target.name(), target.reason());
        }

        NavigationRequest request = NavigationRequest.builder()
                .targetMapName(target.mapName())
                .targetX(target.x())
                .targetY(target.y())
                .targetName(target.name())
                .miniMapClickRandomRadiusPx(PREPATH_MINI_MAP_CLICK_RANDOM_RADIUS_PX)
                .source("wubei:post-accept-prepath:" + state.round() + ":" + target.reason())
                .build();
        NavigationResult result = navigationService.navigateInCurrentMap(request);
        if (result.getStatus() == NavigationResultStatus.STOPPED
                || result.getStatus() == NavigationResultStatus.INTERRUPTED) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            throw new TaskStopRequestedException("Wubei task interrupted");
        }
        log.info("[wubei] post-accept prepath navigation result: round={} target={} status={} message={}",
                state.round(), target.name(), result.getStatus(), result.getMessage());
    }

    private boolean shouldSkipPostAcceptAltCForStartupFlying(WubeiRoundContext state) {
        if (state == null || state.round() != 1) {
            return false;
        }
        GameStateUtil.FlyingState startupFlyingState = windowTaskContextHolder.rawCurrent()
                .map(runtime -> runtime.consumeTaskQueueStartupFlyingState("wubei:first-round-post-accept-prepath"))
                .orElse(GameStateUtil.FlyingState.UNKNOWN);
        boolean skip = startupFlyingState == GameStateUtil.FlyingState.FLYING;
        log.info("[wubei] first-round startup flying state consumed before post-accept Alt+C: state={} skipAltC={}",
                startupFlyingState, skip);
        return skip;
    }

    private WubeiPrepathTarget computePostAcceptPrepathTarget() {
        if (isHealPetMaintenanceDue()) {
            return new WubeiPrepathTarget(
                    HEAL_PET_NPC.getMapName(), HEAL_PET_NPC.getX(), HEAL_PET_NPC.getY(),
                    HEAL_PET_NPC.getName(), "heal-pet-due");
        }
        return new WubeiPrepathTarget(START_MAP_NAME, START_EXIT_X, START_EXIT_Y,
                "宝象国出口", "default-or-repair-only");
    }

    private DialogResult tryConsumePreparedWubeiDialog(DialogOperation operation,
                                                       String source,
                                                       boolean refreshInterest) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            log.debug("[wubei] consume prepared dialog skipped: operation={} source={} reason=no-window-runtime",
                    operation, source);
            return null;
        }
        if (refreshInterest) {
            registerWubeiDialogInterest(runtime, operation, source);
        }
        PreparedDialogAction action = runtime.consumePreparedDialogActionValidated(
                operation, null, source,
                prepared -> dialogService.validatePreparedDialogActionForConsume(prepared, source));
        if (action == null) {
            return null;
        }
        if (operation == DialogOperation.WUBEI_PROBE_STORY && currentProbeStoryWaitStartedAt > 0L) {
            if (action.getPreparedAtMs() < currentProbeStoryWaitStartedAt) {
                log.info("[wubei] discard pre-mirror prepared probe story: target={} matched={} source={} actionSource={} observedAt={} mirrorUsedAt={}",
                        action.getTargetKeyword(), action.getMatchedText(), source, action.getSource(),
                        action.getPreparedAtMs(), currentProbeStoryWaitStartedAt);
                return null;
            }
        }
        runtime.clearDialogInterest("wubei prepared consumed: " + operation);
        if (!action.isClickRequired()) {
            if (operation == DialogOperation.WUBEI_PROBE_STORY
                    && WubeiDialogCatalog.STORY_PROBE_NO_TARGET.equals(action.getTargetKeyword())) {
                log.info("[wubei] consumed prepared probe story miss: operation={} target={} source={} actionSource={}",
                        operation, action.getTargetKeyword(), source, action.getSource());
                return DialogResult.statusBuilder(DialogResultStatus.WHITE_TEMPLATE_NOT_FOUND, action.getDialogType())
                        .actionKey(action.getTargetKeyword())
                        .matchedText(action.getMatchedText())
                        .preparedAction(action)
                        .relativeX(action.getRelativeX())
                        .relativeY(action.getRelativeY())
                        .absoluteX(action.getAbsoluteX())
                        .absoluteY(action.getAbsoluteY())
                        .build();
            }
            log.info("[wubei] consumed prepared dialog signal: operation={} target={} matched={} source={} actionSource={}",
                    operation, action.getTargetKeyword(), action.getMatchedText(), source, action.getSource());
            DialogResultStatus signalStatus = action.getDialogType() == DialogType.NONE
                    ? DialogResultStatus.STORY_ABSENT
                    : DialogResultStatus.WHITE_TEMPLATE_VISIBLE;
            return DialogResult.statusBuilder(signalStatus, action.getDialogType())
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

    private DialogResult waitForPreparedWubeiDialog(TaskExecutionContext context,
                                                    DialogOperation operation,
                                                    String source,
                                                    boolean refreshInterest,
                                                    long waitMs) {
        long afterSequence = windowReadyEventBus.currentSequence();
        DialogResult result = tryConsumePreparedWubeiDialog(operation, source + ":initial", refreshInterest);
        if (result != null) {
            return result;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            log.warn("[wubei] prepared dialog wait skipped: operation={} source={} reason=no-window-runtime",
                    operation, source);
            return null;
        }
        long deadline = System.currentTimeMillis() + waitMs;
        while (System.currentTimeMillis() <= deadline) {
            long remainingMs = Math.max(0L, deadline - System.currentTimeMillis());
            if (remainingMs <= 0L) {
                break;
            }
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            Optional<WindowReadyEvent> ready = windowReadyEventBus.awaitNewer(
                    runtime.getWindowId(),
                    EnumSet.of(WindowReadyEventType.PREPARED_ACTION_READY),
                    afterSequence,
                    remainingMs);
            if (ready.isEmpty()) {
                TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
                break;
            }
            WindowReadyEvent event = ready.get();
            afterSequence = event.getSequence();
            if (!isPreparedDialogReadyEventFor(event, operation)) {
                log.info("[wubei] prepared dialog wait ignored mismatched ready event: operation={} source={} eventOperation={} eventTarget={} eventSource={} sequence={}",
                        operation, source, event.getOperation(), event.getTargetKeyword(),
                        event.getSource(), event.getSequence());
                continue;
            }
            result = tryConsumePreparedWubeiDialog(operation, source + ":ready-event", false);
            if (result != null) {
                log.info("[wubei] prepared dialog consumed after ready event: operation={} source={} waitMs={} readySeq={} eventTarget={}",
                        operation, source, waitMs - Math.max(0L, deadline - System.currentTimeMillis()),
                        event.getSequence(), event.getTargetKeyword());
                return result;
            }
        }
        return null;
    }

    private boolean isPreparedDialogReadyEventFor(WindowReadyEvent event, DialogOperation operation) {
        return event != null
                && event.getType() == WindowReadyEventType.PREPARED_ACTION_READY
                && event.getOperation() == operation;
    }

    private void registerWubeiDialogInterest(WindowRuntimeContext runtime,
                                             DialogOperation operation,
                                             String source) {
        registerWubeiDialogInterest(runtime, operation, source, 0L);
    }

    private void registerWubeiDialogInterest(WindowRuntimeContext runtime,
                                             DialogOperation operation,
                                             String source,
                                             long absentAllowedAtMs) {
        runtime.updateDialogInterest(WindowDialogInterest.builder()
                .taskType(TaskType.WUBEI)
                .operations(List.of(operation))
                .source(source)
                .absentAllowedAtMs(absentAllowedAtMs)
                .build(), source);
    }

    private boolean isTrackerDarkThunderTask(TaskTrackerPanelReadResult panel) {
        return isTrackerTask(panel, TaskTrackerPanelService.WUBEI_TASK_KEY_DIANQIAN_XIANYI);
    }

    private boolean isTrackerProbeTask(TaskTrackerPanelReadResult panel) {
        return isTrackerTask(panel, TaskTrackerPanelService.WUBEI_TASK_KEY_BAOXIANG_MIQING);
    }

    private boolean isTrackerChainedCombatTask(TaskTrackerPanelReadResult panel) {
        return isTrackerTask(panel, TaskTrackerPanelService.WUBEI_TASK_KEY_ZHIDOU_HUANGPAO);
    }

    private boolean isTrackerTask(TaskTrackerPanelReadResult panel, String taskKey) {
        return taskKey != null
                && panel != null
                && panel.getTitleTemplate() != null
                && taskKey.equals(panel.getTitleTemplate().getTaskKey());
    }

    private String trackerTaskKey(TaskTrackerPanelReadResult panel) {
        return panel == null || panel.getTitleTemplate() == null
                ? ""
                : panel.getTitleTemplate().getTaskKey();
    }

    private String trackerTaskTitle(TaskTrackerPanelReadResult panel) {
        return panel == null || panel.getTitleTemplate() == null
                ? ""
                : panel.getTitleTemplate().getDisplayName();
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

    private TaskTrackerPanelReadResult resolveTrackerPanelForReadPhase(TaskExecutionContext context,
                                                                       WubeiRoundContext state) {
        CompletableFuture<TaskTrackerPanelReadResult> future = postAcceptTrackerPanelFuture;
        postAcceptTrackerPanelFuture = null;
        if (future != null) {
            TaskTrackerPanelReadResult result = waitForPostAcceptTrackerPanelRead(context, state, future);
            if (result != null && result.isFound()) {
                log.info("[wubei] READ_TRACKER uses post-accept background tracker result: round={} taskKey={} title={} links={}",
                        state.round(), trackerTaskKey(result), trackerTaskTitle(result),
                        result.getGreenLinks() == null ? 0 : result.getGreenLinks().size());
                return result;
            }
            log.warn("[wubei] post-accept background tracker result unavailable; fallback to direct tracker read: round={} found={}",
                    state.round(), result != null && result.isFound());
        }
        return resolveTrackerPanelWithAnchorRecovery(context, state);
    }

    private TaskTrackerPanelReadResult waitForPostAcceptTrackerPanelRead(TaskExecutionContext context,
                                                                         WubeiRoundContext state,
                                                                         CompletableFuture<TaskTrackerPanelReadResult> future) {
        while (!future.isDone()) {
            TaskSleep.sleepOrStop(context, 50L, "Wubei task interrupted");
        }
        try {
            return future.join();
        } catch (CompletionException e) {
            log.warn("[wubei] post-accept background tracker result failed: round={} source={}",
                    state.round(), state.source(), e);
            return TaskTrackerPanelReadResult.empty();
        }
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
        TrackerLinkRankerCloudDecision cloudDecision = shadowTrackerLinkSelectionIfLocal(
                label, "wubei-probe-tracker-pathing", currentProbeSegments, nextIndex, segment);
        return clickTaskTrackerGreen(context, segment, label, 1, cloudDecision);
    }

    private WubeiStepOutcome resolveProbeAfterPathing(TaskExecutionContext context, WubeiRoundContext state) {
        int index = currentProbeIndex;
        String label = probeLabel(index);
        WindowPathingSnapshot snapshot = currentWindowPathingSnapshot();
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        boolean waitingForRunnerStory = currentProbeStoryWaitIndex == index;
        if (isCurrentTrackerPathingSnapshot(snapshot)) {
            log.info("[wubei] resolve probe after runner pathing: label={} snapshotState={} probe={} current={}({}, {}) used={} attempts={} hint={}",
                    label, snapshot.getState(), snapshot.isProbeInProgress(),
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    probeUsedSummary(), probeAttemptSummary(), currentTrackerDestinationHint);
            if (snapshot.getState() == WindowPathingState.ACTIVE || snapshot.isProbeInProgress()) {
                return waitForPathingWake(WubeiStepOutcome.sharedState(
                        state,
                        "probe runner pathing still active"));
            }
            if (snapshot.getState() == WindowPathingState.UNKNOWN) {
                log.warn("[wubei] probe runner pathing unknown; continue probe recovery instead of yielding forever: label={} current={}({}, {}) message={}",
                        label, snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(), snapshot.getMessage());
            }
        }

        if (!waitingForRunnerStory) {
            String probeStorySource = "wubei:probe-story:" + label;
            if (runtime != null) {
                registerWubeiDialogInterest(runtime, DialogOperation.WUBEI_PROBE_STORY, probeStorySource + ":before-item");
            }
            if (!useProbeItemWithRuntimeRecord(context, index, label, runtime, probeStorySource)) {
                if (probeCanRetryItem(index)) {
                    log.warn("[wubei] probe item use failed; retry same probe point later: label={} used={} attempts={}",
                            label, probeUsedSummary(), probeAttemptSummary());
                    return WubeiStepOutcome.sharedState(state, "probe item use retry");
                }
                return WubeiStepOutcome.failed(state, "probe item use failed");
            }
        }

        /*
         * 显形镜使用后一定应该由窗口 runner 准备 story 判断结果。这里不能再走
         * 800ms quick-wait 旧逻辑，也不能返回 sharedState/park 等 Runner 唤醒。白龙马
         * 显形镜后的热路径必须继续持有当前 task turn，直到 Runner/provider 给出
         * target-ready / wrong-position / no-STORY / template-miss 之一。
         */
        DialogResult probeStory = waitForPreparedProbeStory(context, label);
        currentProbeStoryWaitIndex = -1;
        currentProbeStoryWaitStartedAt = 0L;
        if (runtime != null) {
            runtime.clearDialogInterest("wubei probe story decision finished: " + label);
        }
        if (isProbeTargetReadyStoryVisible(label, probeStory)) {
            markProbeResolved(index);
            log.info("[wubei] probe target-ready story matched; smart-click spawned target: label={}", label);
            if (tryClickProbeSpawnedTarget(context, state, label, true)) {
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

        if (isProbeStoryAbsent(label, probeStory)) {
            if (probeCanRetryItem(index)) {
                log.warn("[wubei] probe story absent; retry same probe item without yielding: label={} used={} attempts={}",
                        label, probeUsedSummary(), probeAttemptSummary());
                return WubeiStepOutcome.continueTo(
                        state.next(WubeiPhase.RESOLVE_AFTER_PATHING, "probe-story-absent-retry-same"),
                        "probe story absent retry same item");
            }
            log.warn("[wubei] probe story absent after max attempts; fail current task without prompt switch or tooltip fallback: label={} used={} attempts={}",
                    label, probeUsedSummary(), probeAttemptSummary());
            return WubeiStepOutcome.failed(state, "probe story absent after max attempts");
        }

        closeUnknownProbeStoryIfNeeded(label, probeStory);

        /*
         * 成功 story 偶尔会漏检；如果白龙马 tooltip 已经出现，先尝试进入战斗，
         * 不要过早重试显形镜或切到第二个绿字。
         */
        if (!WubeiDialogCatalog.STORY_PROBE_NO_TARGET.equals(probeStory.getActionKey())
                && tryClickProbeSpawnedTarget(context, state, label, false)) {
            markProbeResolved(index);
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.WAIT_BATTLE_FINISH, "probe-tooltip-clicked-without-story"),
                    "probe target tooltip clicked without story confirmation");
        }

        markProbeResolved(index);
        int nextUnused = nextUnusedProbeIndex();
        if (nextUnused >= 0) {
            currentProbeIndex = nextUnused;
            log.info("[wubei] probe story still missing after runner wait; switch to next unused probe: current={} next={} used={} attempts={}",
                    label, probeLabel(nextUnused), probeUsedSummary(), probeAttemptSummary());
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.TRACKER_PATHING, "probe-next-unused"),
                    "probe next unused point");
        }

        log.warn("[wubei] probe exhausted without target-ready story: used={} attempts={}",
                probeUsedSummary(), probeAttemptSummary());
        return WubeiStepOutcome.failed(state, "probe exhausted without target-ready story");
    }

    private boolean triggerCombatTrackerPathing(TaskExecutionContext context,
                                                TaskTrackerPanelReadResult panel,
                                                String label) {
        if (panel.getGreenLinks().isEmpty()) {
            log.warn("[wubei] no tracker green segment for combat pathing: label={}", label);
            return false;
        }
        TaskTrackerGreenLink segment = selectedTrackerGreenLink(panel);
        TrackerLinkRankerCloudDecision cloudDecision = shadowTrackerLinkSelectionIfLocal(
                label, "wubei-combat-tracker-pathing", panel.getGreenLinks(), 0, segment);
        for (int attempt = 1; attempt <= MAX_TRACKER_CLICK_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            if (clickTaskTrackerGreen(context, segment, label, attempt, cloudDecision)) {
                return true;
            }
            if (cloudDecision != null && cloudDecision.isNoClick()) {
                return false;
            }
            TaskSleep.sleepOrStop(context, 800L, "Wubei task interrupted");
        }
        log.warn("[wubei] tracker green click failed after {} attempts: label={}",
                MAX_TRACKER_CLICK_ATTEMPTS, label);
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
        currentProbeStoryWaitIndex = -1;
        currentProbeStoryWaitStartedAt = 0L;
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

    private boolean useProbeItemWithRuntimeRecord(TaskExecutionContext context,
                                                  int index,
                                                  String label,
                                                  WindowRuntimeContext runtime,
                                                  String probeStorySource) {
        if (!isValidProbeIndex(index)) {
            log.warn("[wubei] probe item skipped for invalid index: label={} index={} links={}",
                    label, index, currentProbeSegments.size());
            return false;
        }
        currentProbeItemAttempts[index]++;
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        boolean used = bagService.findAndUseItemFromBack(BagService.MAIN_BAG, PROBE_ITEM_TEMPLATE, 5, context);
        log.info("[wubei] probe item used: label={} used={}", label, used);
        if (used) {
            currentProbeStoryWaitIndex = index;
            currentProbeStoryWaitStartedAt = System.currentTimeMillis();
            if (runtime != null) {
                registerWubeiDialogInterest(runtime, DialogOperation.WUBEI_PROBE_STORY,
                        probeStorySource + ":after-item", currentProbeStoryWaitStartedAt);
            }
        }
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
        if (currentProbeStoryWaitIndex == index) {
            currentProbeStoryWaitIndex = -1;
            currentProbeStoryWaitStartedAt = 0L;
        }
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

    private TrackerLinkRankerCloudDecision shadowTrackerLinkSelectionIfLocal(String label,
                                                                             String phase,
                                                                             List<TaskTrackerGreenLink> links,
                                                                             int selectedIndex,
                                                                             TaskTrackerGreenLink segment) {
        if (isCloudTrackerPanelReaderLink(segment)) {
            log.info("[wubei] skip TRACKER_LINK_RANKER for cloud tracker-panel-reader link: label={} phase={} segment={}",
                    label, phase, segment);
            return null;
        }
        boolean trackerBaseReady = tracker.refreshWindowState();
        return trackerLinkRankerCloudShadowService.shadowTrackerLinkSelection(
                TASK_CODE,
                label,
                phase,
                links,
                selectedIndex,
                segment,
                trackerBaseReady ? tracker.getWindowBaseX() : -1,
                trackerBaseReady ? tracker.getWindowBaseY() : -1);
    }

    private boolean clickTaskTrackerGreen(TaskExecutionContext context,
                                          TaskTrackerGreenLink segment,
                                          String label,
                                          int attempt,
                                          TrackerLinkRankerCloudDecision cloudDecision) {
        if (isCloudTrackerPanelReaderLink(segment)) {
            return clickTaskTrackerGreenAtPoint(context, segment, label, attempt, segment.centerPoint());
        }
        if (cloudDecision == null || !cloudDecision.isCloudExecuted()) {
            log.warn("[wubei] cloud decision rejected; skip local tracker green click: label={} attempt={} "
                            + "status={} reason={} segment={}",
                    label, attempt,
                    cloudDecision == null ? null : cloudDecision.getStatus(),
                    cloudDecision == null ? "missing TRACKER_LINK_RANKER decision" : cloudDecision.getRejectReason(),
                    segment);
            return false;
        }
        Point cloudClick = resolveTrackerCloudAbsolutePoint(cloudDecision, label);
        if (cloudClick == null) {
            log.warn("[wubei] cloud decision executed but absolute click unavailable; skip local tracker green click: "
                            + "label={} attempt={} reason={} cloudPoint={}",
                    label, attempt, cloudDecision.getRejectReason(),
                    cloudDecision.getCloudWindowRelativeClickPoint());
            return false;
        }
        return clickTaskTrackerGreenAtPoint(context, segment, label, attempt, cloudClick);
    }

    private boolean clickTaskTrackerGreenAtPoint(TaskExecutionContext context,
                                                 TaskTrackerGreenLink segment,
                                                 String label,
                                                 int attempt,
                                                 Point click) {
        String safeLabel = safeFileToken(label);
        DialogType dialogBeforeClick = dialogService.detectDialogTypeNoFocus(
                "wubei:tracker-green-before:" + safeLabel, false, 0);
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        boolean chainedContinuation = label != null && label.startsWith("chained-combat-");
        String chainedInterestSource = null;
        if (chainedContinuation) {
            chainedInterestSource = "wubei:chained-enter-battle-before-click:" + safeLabel;
            if (runtime != null) {
                /*
                 * 黄袍续战的进战斗框可能在绿字点击后立刻出现。这里必须先告诉 Runner
                 * 续战正在等待 WUBEI_ENTER_BATTLE，否则 watcher 会把 OPTION 当成普通可见
                 * 弹窗发布，队长后面只能空等 fresh prepared action。
                 */
                registerWubeiDialogInterest(runtime, DialogOperation.WUBEI_ENTER_BATTLE, chainedInterestSource);
            } else {
                log.info("[wubei] chained enter-battle interest skipped before click: label={} reason=no-window-runtime",
                        label);
            }
        }
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
        if (!clicked && chainedContinuation && runtime != null) {
            runtime.clearDialogInterest("wubei chained tracker click failed before enter-battle: " + safeLabel);
        }
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
                if (isProbeTrackerLabel(label)) {
                    returnItemPrescanService.afterTrackerGreenRequired(context, TASK_CODE, currentRoundNumber,
                            PROBE_ITEM_TEMPLATE, ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                            "wubei:probe-mirror-slot:" + safeFileToken(label));
                } else {
                    returnItemPrescanService.afterTrackerGreen(context, TASK_CODE, currentRoundNumber,
                            RETURN_ITEM_TEMPLATE, ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                            "wubei:tracker-green-click:" + safeFileToken(label));
                }
                consumeCommonBoxAfterTaskAccepted(context, "wubei:tracker-green-click:" + safeFileToken(label));
                autoCombatService.consumePendingLeaderPostCombatRecoveryIfAllowed(
                        context, "wubei:tracker-green-click:" + safeFileToken(label));
                startOrdinaryEnterBattleTargetMapGateIfNeeded(segment, label, safeLabel, intentSource);
            } else {
                log.info("[wubei] tracker pathing intent skipped: label={} reason=chained-combat-continuation "
                                + "enterBattleInterestSource={}",
                        label, chainedInterestSource);
            }
        }
        return clicked;
    }

    private Point resolveTrackerCloudAbsolutePoint(TrackerLinkRankerCloudDecision cloudDecision,
                                                   String label) {
        Point cloudPoint = cloudDecision == null ? null : cloudDecision.getCloudWindowRelativeClickPoint();
        if (cloudPoint == null) {
            return null;
        }
        if (tracker.refreshWindowState() && tracker.getWindowBaseX() >= 0 && tracker.getWindowBaseY() >= 0) {
            log.info("[wubei] tracker cloud click uses tracker logical base: label={} relative=({}, {}) base=({}, {})",
                    label, cloudPoint.x, cloudPoint.y, tracker.getWindowBaseX(), tracker.getWindowBaseY());
            return new Point(tracker.getWindowBaseX() + cloudPoint.x, tracker.getWindowBaseY() + cloudPoint.y);
        }
        log.warn("[wubei] tracker cloud click cannot resolve window base: label={} relative=({}, {})",
                label, cloudPoint.x, cloudPoint.y);
        return null;
    }

    private boolean isCloudTrackerPanelReaderLink(TaskTrackerGreenLink segment) {
        return segment != null
                && segment.getSourceType() == TaskTrackerPanelSourceType.CLOUD_TRACKER_PANEL_READER;
    }

    private boolean isCloudTrackerPanelReaderCachedAction(PreparedDialogAction cachedAction) {
        return cachedAction != null
                && cachedAction.getTrackerPanelSourceType() == TaskTrackerPanelSourceType.CLOUD_TRACKER_PANEL_READER;
    }

    private TaskTrackerGreenLink selectedTrackerGreenLink(TaskTrackerPanelReadResult panel) {
        if (panel != null && panel.getSelectedGreenLink() != null) {
            return panel.getSelectedGreenLink();
        }
        return panel == null || panel.getGreenLinks().isEmpty() ? null : panel.getGreenLinks().get(0);
    }

    private void consumeCommonBoxAfterTaskAccepted(TaskExecutionContext context, String source) {
        commonBoxService.consumePendingBoxIfAllowed(context, TASK_CODE, source);
    }

    private void startOrdinaryEnterBattleTargetMapGateIfNeeded(TaskTrackerGreenLink segment,
                                                              String label,
                                                              String safeLabel,
                                                              String intentSource) {
        String skipReason = normalEnterBattleInterestSkipReason(label);
        if (skipReason != null) {
            log.info("[wubei] ordinary enter-battle target map gate skipped: label={} reason={}",
                    label, skipReason);
            return;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            log.info("[wubei] ordinary enter-battle target map gate skipped: label={} reason=no-window-runtime",
                    label);
            return;
        }
        String targetMapName = segment == null ? null : segment.getTargetMapName();
        if (targetMapName == null || targetMapName.isBlank()) {
            log.warn("[wubei] ordinary enter-battle target map gate skipped: label={} windowId={} round={} reason=blank-target-map segment={} intentSource={}",
                    label, runtime.getWindowId(), currentRoundNumber, segment, intentSource);
            return;
        }
        String source = "wubei:normal-enter-battle-map-gate:" + safeLabel;
        runtime.startOrdinaryEnterBattleTargetMapGate(TaskType.WUBEI, source, targetMapName, System.currentTimeMillis());
        log.info("[wubei] ordinary enter-battle target map gate armed: label={} windowId={} round={} targetMap={} score={} debugPath={} intentSource={} source={}",
                label, runtime.getWindowId(), currentRoundNumber, targetMapName,
                segment.getTargetMapScore(), segment.getTargetMapDebugPath(), intentSource, source);
    }

    private String normalEnterBattleInterestSkipReason(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String normalized = label.toLowerCase();
        if (normalized.contains("probe")) {
            return "probe-link";
        }
        if (normalized.startsWith("chained-combat-")) {
            return "chained-combat-continuation";
        }
        return null;
    }

    private DialogResult waitForPreparedWubeiDialogReply(TaskExecutionContext context,
                                                         WubeiRoundContext state,
                                                         DialogOperation operation,
                                                         String source,
                                                         boolean refreshInterest) {
        throwProbeEnterBattleTimeoutIfNeeded(context, state, operation, source);
        long afterSequence = windowReadyEventBus.currentSequence();
        DialogResult initial = tryConsumePreparedWubeiDialog(operation, source + ":runner-reply:initial", refreshInterest);
        if (initial != null) {
            return initial;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            log.warn("[wubei] prepared dialog reply wait skipped: operation={} source={} reason=no-window-runtime",
                    operation, source);
            return null;
        }
        while (true) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            throwProbeEnterBattleTimeoutIfNeeded(context, state, operation, source);
            Optional<WindowReadyEvent> ready = windowReadyEventBus.awaitNewer(
                    runtime.getWindowId(),
                    EnumSet.of(WindowReadyEventType.PREPARED_ACTION_READY),
                    afterSequence,
                    preparedDialogReplyRecheckMs(operation));
            if (ready.isEmpty()) {
                continue;
            }
            WindowReadyEvent event = ready.get();
            afterSequence = event.getSequence();
            if (!isPreparedDialogReadyEventFor(event, operation)) {
                log.info("[wubei] prepared dialog reply ignored mismatched ready event: operation={} source={} eventOperation={} eventTarget={} eventSource={} sequence={}",
                        operation, source, event.getOperation(), event.getTargetKeyword(),
                        event.getSource(), event.getSequence());
                continue;
            }
            DialogResult result = tryConsumePreparedWubeiDialog(operation, source + ":runner-reply:ready-event", false);
            if (result != null) {
                return result;
            }
        }
    }

    private long preparedDialogReplyRecheckMs(DialogOperation operation) {
        if (operation == DialogOperation.WUBEI_ENTER_BATTLE) {
            return probeEnterBattleRecheckMs();
        }
        return WUBEI_WAIT_UNTIL_RUNNER_EVENT_MS;
    }

    private DialogResult waitForProbeEnterBattlePreparedDialog(TaskExecutionContext context,
                                                               WubeiRoundContext state,
                                                               String source) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            log.warn("[wubei] probe enter-battle wait has no window runtime: source={}", source);
            return null;
        }
        /*
         * 白龙马 targetReady 后仍然保持当前 task turn，但不能像旧逻辑那样每 80ms
         * 重新注册 interest 并 consume absent。这里把 WUBEI_ENTER_BATTLE interest 声明一次，
         * 后续只在 Runner/provider 发布 fresh prepared action 后消费；没有事件时只低频复查
         * 既有 probe 300s timeout。
         */
        registerWubeiDialogInterest(runtime, DialogOperation.WUBEI_ENTER_BATTLE, source + ":runner-wait");
        long lastProgressLogAt = 0L;
        long candidateDeadlineAt = isSmartCombatTargetSource(source)
                ? System.currentTimeMillis() + PROBE_TARGET_CANDIDATE_ENTER_BATTLE_WAIT_MS
                : Long.MAX_VALUE;
        while (true) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            throwProbeEnterBattleTimeoutIfNeeded(context, state, DialogOperation.WUBEI_ENTER_BATTLE, source);
            runtime = windowTaskContextHolder.rawCurrent().orElse(null);
            if (runtime == null) {
                log.warn("[wubei] probe enter-battle wait lost window runtime: source={}", source);
                return null;
            }
            if (hasFreshPreparedAction(runtime, DialogOperation.WUBEI_ENTER_BATTLE)) {
                DialogResult result = tryConsumePreparedWubeiDialog(
                        DialogOperation.WUBEI_ENTER_BATTLE,
                        source + ":runner-reply",
                        false);
                if (result != null) {
                    return result;
                }
            }

            long now = System.currentTimeMillis();
            if (now >= candidateDeadlineAt) {
                log.warn("[wubei] probe enter-battle candidate wait expired; allow direct-combat fallback: "
                                + "source={} candidateWaitMs={} probeElapsedMs={} used={} attempts={}",
                        source, PROBE_TARGET_CANDIDATE_ENTER_BATTLE_WAIT_MS, probeEnterBattleElapsedMs(),
                        probeUsedSummary(), probeAttemptSummary());
                return null;
            }
            if (now - lastProgressLogAt >= 5_000L) {
                lastProgressLogAt = now;
                log.info("[wubei] probe enter-battle turn-owned wait pending: source={} probeElapsedMs={} used={} attempts={}",
                        source, probeEnterBattleElapsedMs(), probeUsedSummary(), probeAttemptSummary());
            }
            long waitMs = probeEnterBattleRecheckMs();
            if (candidateDeadlineAt != Long.MAX_VALUE) {
                waitMs = Math.min(waitMs, Math.max(1L, candidateDeadlineAt - now));
            }
            long afterSequence = windowReadyEventBus.currentSequence();
            windowReadyEventBus.awaitNewer(
                    runtime.getWindowId(),
                    EnumSet.of(WindowReadyEventType.PREPARED_ACTION_READY),
                    afterSequence,
                    waitMs);
        }
    }

    private boolean isSmartCombatTargetSource(String source) {
        return source != null && source.startsWith("wubei:smart-combat-target:");
    }

    private long probeEnterBattleElapsedMs() {
        if (currentProbeTaskStartedAt <= 0L) {
            return -1L;
        }
        return Math.max(0L, System.currentTimeMillis() - currentProbeTaskStartedAt);
    }

    private long probeEnterBattleRecheckMs() {
        if (currentProbeTaskStartedAt <= 0L) {
            return PROBE_ENTER_BATTLE_EVENT_RECHECK_MS;
        }
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - currentProbeTaskStartedAt);
        long remainingMs = PROBE_ENTER_BATTLE_TIMEOUT_MS - elapsedMs;
        if (remainingMs <= 0L) {
            return 1L;
        }
        return Math.min(PROBE_ENTER_BATTLE_EVENT_RECHECK_MS, remainingMs);
    }

    private void throwProbeEnterBattleTimeoutIfNeeded(TaskExecutionContext context,
                                                      WubeiRoundContext state,
                                                      DialogOperation operation,
                                                      String source) {
        if (state == null || operation != DialogOperation.WUBEI_ENTER_BATTLE) {
            return;
        }
        WubeiStepOutcome timeout = timeoutProbeTaskBeforeBattleIfNeeded(context, state);
        if (timeout == null) {
            return;
        }
        log.warn("[wubei] probe enter-battle timeout fired inside prepared-dialog wait: source={} next={} message={}",
                source, timeout.nextState().phase(), timeout.message());
        throw new ProbeEnterBattleTimeoutSignal(timeout);
    }

    private boolean shouldCaptureTrackerDestinationHint(String label) {
        return isProbeTrackerLabel(label);
    }

    private boolean isProbeTrackerLabel(String label) {
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
            clearTrackerGreenPathingIntent("wubei:new-tracker-green-click:" + intentSource);
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
        ImageProcessorResult washResult =
                imageProcessorService.washToPath(
                        Path.of(capture.rawPath()),
                        Path.of(capture.yellowPath()),
                        ImagePreprocessOperation.WASH_YELLOW,
                        RequestMetadata.builder()
                                .rawImagePath(capture.rawPath())
                                .debugImageId("wubei:destination-hint:" + capture.label())
                                .source("wubei:destination-hint")
                                .taskCode(TASK_CODE)
                                .phase("destination-hint-yellow-wash")
                                .build());
        long afterWashAt = System.currentTimeMillis();
        if (!washResult.hasImage()) {
            log.info("[wubei] destination hint wash missed: label={} sample={} region={} status={} reason={} raw={} yellow={}",
                    capture.label(), capture.sample(), capture.region().toShortText(),
                    washResult.status(), washResult.reason(), capture.rawPath(), capture.yellowPath());
            return Optional.empty();
        }
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

    private DialogResult waitForPreparedProbeStory(TaskExecutionContext context, String label) {
        String source = "wubei:probe-story:" + label + ":runner-wait";
        /*
         * CR39: no-STORY is a Runner/provider result, not a leader-side timeout decision. After
         * using 显形镜, the leader must not release the current task turn, park, or sleep; it simply
         * keeps consuming the current window's prepared result until Runner/provider produces the
         * explicit WUBEI_PROBE_STORY outcome.
         */
        boolean refreshInterest = false;
        long lastProgressLogAt = 0L;
        while (true) {
            TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
            WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
            long afterSequence = windowReadyEventBus.currentSequence();
            DialogResult result = tryConsumePreparedWubeiDialog(
                    DialogOperation.WUBEI_PROBE_STORY,
                    source,
                    refreshInterest);
            if (result != null) {
                return result;
            }
            refreshInterest = false;
            long now = System.currentTimeMillis();
            if (now - lastProgressLogAt >= 1_000L) {
                lastProgressLogAt = now;
                log.info("[wubei] probe story turn-owned wait still pending: label={} waitAgeMs={} used={} attempts={}",
                        label, probeStoryWaitAgeMs(), probeUsedSummary(), probeAttemptSummary());
            }
            if (runtime == null) {
                log.warn("[wubei] probe story wait has no window runtime; source={}", source);
                return DialogResult.simple(DialogResultStatus.FAILED, DialogType.NONE);
            }
            windowReadyEventBus.awaitNewer(
                    runtime.getWindowId(),
                    EnumSet.of(WindowReadyEventType.PREPARED_ACTION_READY),
                    afterSequence,
                    WUBEI_WAIT_UNTIL_RUNNER_EVENT_MS);
        }
    }

    private long probeStoryWaitAgeMs() {
        if (currentProbeStoryWaitStartedAt <= 0L) {
            return -1L;
        }
        return Math.max(0L, System.currentTimeMillis() - currentProbeStoryWaitStartedAt);
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

    private boolean isProbeStoryAbsent(String label, DialogResult result) {
        boolean absent = WubeiDialogCatalog.STORY_PROBE_ABSENT.equals(result.getActionKey())
                && result.getDialogType() == DialogType.NONE
                && result.getStatus() == DialogResultStatus.STORY_ABSENT
                && WubeiDialogCatalog.STORY_ABSENT_TEXT.equals(result.getMatchedText());
        log.info("[wubei] probe story absent check: label={} absent={} status={} matched={}",
                label, absent, result.getStatus(), result.getMatchedText());
        return absent;
    }

    private void closeUnknownProbeStoryIfNeeded(String label, DialogResult result) {
        if (result.getStatus() != DialogResultStatus.WHITE_TEMPLATE_NOT_FOUND
                || result.getDialogType() != DialogType.STORY) {
            return;
        }
        /*
         * Known probe stories are semantic signals and are handled above. If a story frame is
         * present but neither known probe template matched, restore the old 五倍 behavior: clear
         * that unknown story once before the target-click fallback runs.
         */
        DialogResult closeResult = dialogService.handleDialog(DialogHandleRequest.clickStory(
                "wubei:probe-story-unknown:" + label));
        log.info("[wubei] unknown probe story cleanup: label={} status={} clicked={}",
                label, closeResult.getStatus(), closeResult.isClicked());
    }

    private boolean tryClickProbeSpawnedTarget(TaskExecutionContext context,
                                               WubeiRoundContext state,
                                               String label,
                                               boolean storyConfirmed) {

        boolean clicked = tryClickTrackerCombatTargetSmart(
                context,
                state,
                label + (storyConfirmed ? "-story" : "-no-story"),
                storyConfirmed ? NpcTargetEvidence.CONFIRMED : NpcTargetEvidence.TENTATIVE);
        if (clicked || !storyConfirmed) {
            return clicked;
        }
        log.warn("[wubei] probe story confirmed but runner enter-battle action was not consumed; use direct-combat fallback: label={}",
                label);
        return tryDirectCombatFromTrackerHint(context, label + "-direct-combat");
    }

    private boolean tryClickTrackerCombatTargetSmart(TaskExecutionContext context,
                                                    WubeiRoundContext state,
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
        return tryClickKnownEnterBattleDialog(context, state, "wubei:smart-combat-target:" + label);
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
        DirectCombatClickResult enteredCombat = npcClickService.tryDirectCombatTargetClick(request);
        if (!enteredCombat.combatEntered()) {
            if (enteredCombat.positionRefreshRequired()) {
                markTrackerRetryAfterDirectCombatDisplacement(enteredCombat, label);
            }
            return false;
        }
        autoCombatService.initializeForCurrentWindow();
        // CR252: authorized by combatEntered() — the existing confirmed result of a real Alt+A
        // direct-combat action; a bare Alt+A press or target click never authorizes.
        autoCombatService.authorizeCombatDetectionAfterEnterBattleAction(
                "wubei:direct-combat-entered:" + label);
        return true;
    }

    private void markTrackerRetryAfterDirectCombatDisplacement(DirectCombatClickResult result, String label) {
        enterBattleNextRetryAt = System.currentTimeMillis();
        log.warn("[wubei] direct-combat failed after Alt+A; next retry must refresh tracker pathing before target click: label={} reason={} hint={}",
                label, result.reason(), currentTrackerDestinationHint);
        clearCurrentPathingSignal("wubei direct-combat displaced position; require tracker repath: " + label);
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

    private boolean clearTrackerGreenPathingIntent(String reason) {
        return windowTaskContextHolder.rawCurrent()
                .map(runtime -> runtime.clearPathingSignalIfSourcePrefix(
                        TRACKER_GREEN_PATHING_SOURCE_PREFIX, reason))
                .orElse(false);
    }

    private boolean clearPostAcceptPrepathSignal(String reason) {
        return windowTaskContextHolder.rawCurrent()
                .map(runtime -> runtime.clearPathingSignalIfSourcePrefix("wubei:post-accept-prepath:", reason))
                .orElse(false);
    }

    private void clearCurrentPathingSignal(String reason) {
        windowTaskContextHolder.rawCurrent().ifPresent(runtime -> runtime.clearPathingSignal(reason));
    }

    private boolean shouldDeferEnterBattleTrackerRetryForDialog(WindowPathingSnapshot snapshot, String source) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return false;
        }
        PreparedDialogAction prepared = runtime.getPreparedDialogAction();
        if (prepared != null && prepared.verifiedWithin(System.currentTimeMillis(), ENTER_BATTLE_DIALOG_BLOCK_MAX_AGE_MS)) {
            log.info("[wubei] defer enter-battle tracker retry because prepared dialog is waiting: source={} operation={} target={} preparedSource={} verifiedAgeMs={}",
                    source, prepared.getOperation(), prepared.getTargetKeyword(), prepared.getSource(),
                    Math.max(0L, System.currentTimeMillis() - prepared.getLastVerifiedAtMs()));
            return true;
        }
        DialogPreparationStatus status = runtime.getDialogPreparationStatus();
        if (status != null && isBlockingDialogPreparation(status.getPhase())) {
            log.info("[wubei] defer enter-battle tracker retry because dialog preparation is active: source={} phase={} operation={} target={} statusSource={}",
                    source, status.getPhase(), status.getOperation(), status.getTargetKeyword(), status.getSource());
            return true;
        }
        Optional<WindowDialogSnapshot> visible = runtime.getVisibleDialogSnapshot(ENTER_BATTLE_DIALOG_BLOCK_MAX_AGE_MS);
        if (visible.isPresent() && visible.get().getType() != DialogType.NONE) {
            WindowDialogSnapshot dialog = visible.get();
            log.info("[wubei] defer enter-battle tracker retry because visible dialog is fresh: source={} type={} dialogSource={} ageMs={}",
                    source, dialog.getType(), dialog.getSource(),
                    Math.max(0L, System.currentTimeMillis() - dialog.getDetectedAtMs()));
            return true;
        }
        return false;
    }

    private boolean isBlockingDialogPreparation(DialogPreparationPhase phase) {
        return phase == DialogPreparationPhase.REQUESTED
                || phase == DialogPreparationPhase.PREPARING
                || phase == DialogPreparationPhase.READY;
    }

    private String resolveDirectCombatTargetName(String label) {
        if (isProbeRuntimeActive() || (label != null && label.contains("probe"))) {
            return PROBE_TARGET_NPC_NAME;
        }
        return resolveTrackerCombatTargetName();
    }

    private boolean tryClickKnownEnterBattleDialog(TaskExecutionContext context,
                                                  WubeiRoundContext state,
                                                  String source) {
        DialogResult confirm = isProbeEnterBattleSource(source)
                ? waitForProbeEnterBattlePreparedDialog(context, state, source)
                : waitForPreparedWubeiDialogReply(
                        context,
                        state,
                        DialogOperation.WUBEI_ENTER_BATTLE,
                        source,
                        true);
        lastEnterBattleDialogResult = confirm;
        if (confirm == null) {
            log.info("[wubei] known enter-battle dialog not prepared yet: source={}", source);
            return false;
        }
        boolean clicked = OPTION_ENTER_BATTLE.equals(confirm.getActionKey())
                || OPTION_ENTER_BATTLE_PROVE.equals(confirm.getActionKey())
                || OPTION_ENTER_BATTLE_KUIXING.equals(confirm.getActionKey());
        log.info("[wubei] known enter-battle dialog check: source={} clicked={} status={} action={}",
                source, clicked, confirm.getStatus(), confirm.getActionKey());
        if (clicked) {
            // CR252: covers normal/证明/奎星 and the 黄袍续战 final enter-battle dialog click, plus
            // 白龙马/探测 smart-target paths that resolve through this shared dialog consumer.
            autoCombatService.authorizeCombatDetectionAfterEnterBattleAction(
                    "wubei:enter-battle-dialog-clicked:" + source + ":" + confirm.getActionKey());
        }
        return clicked;
    }

    private boolean isProbeEnterBattleSource(String source) {
        return isProbeRuntimeActive()
                || (source != null && source.contains("probe"));
    }

    private WubeiStepOutcome tickEnterBattle(TaskExecutionContext context, WubeiRoundContext state) {
        long now = System.currentTimeMillis();
        if (enterBattleStartedAt <= 0L) {
            enterBattleStartedAt = now;
            enterBattleNextRetryAt = now;
            /*
             * 进战斗弹窗可能在上一阶段刚判定寻路结束时已经出现。先把 interest
             * 发布给 runner，避免 watcher 已经看到 OPTION 却因为没有目标操作而只发布
             * visible event，必须再等下一轮 observer tick 才能准备点击。
             */
            windowTaskContextHolder.rawCurrent().ifPresent(runtime ->
                    registerWubeiDialogInterest(runtime, DialogOperation.WUBEI_ENTER_BATTLE,
                            "wubei:enter-battle:phase-start"));
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

        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        boolean chainedContinuationEnterBattle = isChainedContinuationEnterBattle(state);
        AutoCombatService.TickResult tick = autoCombatService.handleCombatTick(
                context, TASK_CODE,
                AutoCombatService.PostCombatRecoveryPolicy.FULL_RECOVERY_WITH_LEADER_INCENSE);
        if (tick == AutoCombatService.TickResult.IN_COMBAT) {
            waitBattleSawCombat = true;
            currentProbeTaskStartedAt = 0L;
            // CR252: chained combat can already be running when ENTER_BATTLE re-ticks; the leader's
            // confirmed in-combat state (re-)broadcasts the team combat phase.
            taskMaintenanceService.openTeamCombatPhaseForLeader(context,
                    "wubei:combat-already-started");
            return WubeiStepOutcome.sharedState(
                    state.next(WubeiPhase.WAIT_BATTLE_FINISH, "combat-already-started"),
                    "combat already started");
        }
        WubeiStepOutcome prepared = consumeFreshEnterBattlePreparedAction(
                context, state, runtime, "wubei:enter-battle:after-combat-tick");
        if (prepared != null) {
            return prepared;
        }
        if (tick == AutoCombatService.TickResult.EXIT_RECOVERED) {
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.POST_BATTLE_RECOVER, "combat-ended-during-enter-battle"),
                    "combat ended during enter battle phase");
        }

        if (chainedContinuationEnterBattle) {
            return continueChainedEnterBattleInPhase(context, state,
                    "chained enter battle waits for fresh prepared action");
        }

        if (tryClickKnownEnterBattleDialog(context, null, "wubei:enter-battle")) {
            clearTrackerGreenPathingIntent("wubei prepared enter battle consumed");
            clearCurrentPathingSignal("wubei consumed prepared enter battle dialog");
            autoCombatService.initializeForCurrentWindow();
            return WubeiStepOutcome.sharedState(
                    state.next(WubeiPhase.WAIT_BATTLE_FINISH, "battle-dialog-clicked"),
                    "battle dialog clicked; wait for combat entry");
        }

        if (now < enterBattleNextRetryAt) {
            return waitForPreparedDialogWake(WubeiStepOutcome.sharedState(
                    state,
                    "enter battle retry waiting"));
        }

        WindowPathingSnapshot snapshot = currentWindowPathingSnapshot();
        boolean nearDestination = isNearCurrentTrackerDestination(snapshot);
        if (nearDestination) {
            log.info("[wubei] runner snapshot says leader is near tracker destination; try combat target: hint={} snapshot={}({}, {})",
                    currentTrackerDestinationHint,
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY());
            if (tryClickTrackerCombatTargetSmart(context, null, "runner-destination-smart-click",
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
                && !chainedContinuationEnterBattle
                && currentTrackerPanel != null
                && !currentTrackerPanel.getGreenLinks().isEmpty()
                && !shouldDeferEnterBattleTrackerRetryForDialog(snapshot, "enter-battle-retry")) {
            TaskTrackerGreenLink retrySegment = selectedTrackerGreenLink(currentTrackerPanel);
            TrackerLinkRankerCloudDecision cloudDecision = shadowTrackerLinkSelectionIfLocal(
                    "enter-battle-retry", "wubei-enter-battle-retry",
                    currentTrackerPanel.getGreenLinks(), 0, retrySegment);
            boolean clicked = clickTaskTrackerGreen(context, retrySegment,
                    "enter-battle-retry", 0, cloudDecision);
            if (clicked) {
                return waitForPathingWake(WubeiStepOutcome.pathingStarted(
                        state.next(WubeiPhase.RESOLVE_AFTER_PATHING, "enter-battle-retry-pathing-started"),
                        "enter battle retry pathing started"));
            }
        } else {
            log.info("[wubei] skip enter-battle tracker retry: probeActive={} chained={} hasTrackerPanel={} hasLinks={}",
                    isProbeRuntimeActive(), currentRoundChainedCombatExpected, currentTrackerPanel != null,
                    currentTrackerPanel != null && !currentTrackerPanel.getGreenLinks().isEmpty());
        }

        enterBattleNextRetryAt = now + 6_000L;
        return waitForPreparedDialogWake(WubeiStepOutcome.sharedState(
                state,
                "enter battle unresolved; wait before retry"));
    }

    private boolean isChainedContinuationEnterBattle(WubeiRoundContext state) {
        String source = state == null ? null : state.source();
        return currentRoundChainedCombatExpected
                && state != null
                && state.phase() == WubeiPhase.ENTER_BATTLE
                && source != null
                && (source.startsWith("chained-combat-continued-")
                        || "chained-enter-battle-bounded-retry".equals(source));
    }

    private WubeiStepOutcome continueChainedEnterBattleInPhase(TaskExecutionContext context,
                                                               WubeiRoundContext state,
                                                               String message) {
        TaskSleep.sleepOrStop(context, CHAINED_ENTER_BATTLE_PHASE_RETRY_SLEEP_MS, "Wubei task interrupted");
        String retrySource = state.source() != null && state.source().startsWith("chained-combat-continued-")
                ? state.source()
                : "chained-combat-continued-bounded-retry";
        return WubeiStepOutcome.continueTo(
                state.retrySamePhase(retrySource),
                message);
    }

    private WubeiStepOutcome consumeFreshEnterBattlePreparedAction(TaskExecutionContext context,
                                                                   WubeiRoundContext state,
                                                                   WindowRuntimeContext runtime,
                                                                   String checkpoint) {
        if (!hasFreshPreparedAction(runtime, DialogOperation.WUBEI_ENTER_BATTLE)) {
            return null;
        }
        WindowReadyEvent readyEvent = runtime == null
                ? null
                : windowReadyEventBus.latest(runtime.getWindowId(), WindowReadyEventType.PREPARED_ACTION_READY)
                .orElse(null);
        log.info("[wubei priority] fresh enter-battle prepared action preempts combat-exit recovery: checkpoint={} phase={} windowId={} readySeq={} readyAgeMs={}",
                checkpoint, state.phase(), runtime == null ? null : runtime.getWindowId(),
                readyEvent == null ? -1L : readyEvent.getSequence(), readyAgeMs(readyEvent));
        return consumePreparedEnterBattleBeforeNormalPhase(context, state, runtime, readyEvent, checkpoint);
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
        long checkpointBlockedMs = TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        now = System.currentTimeMillis();
        compensateWaitBattleTimersAfterPause(checkpointBlockedMs, "wubei:wait-battle-checkpoint");

        AutoCombatService.TickResult tick = autoCombatService.handleCombatTick(
                context, TASK_CODE,
                AutoCombatService.PostCombatRecoveryPolicy.FAST_EXPECTED_EXIT);
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
             * CR252: the leader confirmed its OWN combat entry after a confirmed enter-battle
             * action. Broadcast the round's team combat phase to bound real-tooltip-group members
             * so they stop template-confirming their own entry.
             */
            taskMaintenanceService.openTeamCombatPhaseForLeader(context,
                    "wubei:combat-entry-confirmed");
            if (!isProbeRuntimeActive()) {
                returnItemPrescanService.whileInCombat(context, TASK_CODE, state.round(), RETURN_ITEM_TEMPLATE,
                        ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                        "wubei:wait-battle-finish");
            } else {
                log.info("[wubei] return-item combat prescan skipped for probe runtime; mirror slot cache should already exist");
            }
            windowTaskContextHolder.rawCurrent()
                    .ifPresent(runtime -> runtime.clearOrdinaryPreBattleTimer(
                            "wubei combat observed in WAIT_BATTLE_FINISH"));
            /*
             * Battle is shared state. Release the task turn, then park briefly so member
             * auto-battle tasks can acquire the fair turn queue instead of this leader
             * immediately reacquiring the same WAIT_BATTLE_FINISH phase.
             */
            return waitForCombatStateWake(WubeiStepOutcome.sharedState(state, "combat still running"));
        }

        now = System.currentTimeMillis();
        if (now - waitBattleStartedAt >= WAIT_BATTLE_TIMEOUT_MS) {
            log.warn("[wubei] wait battle timeout: chained={} elapsedMs={} timeoutMs={}",
                    currentRoundChainedCombatExpected, now - waitBattleStartedAt, WAIT_BATTLE_TIMEOUT_MS);
            return WubeiStepOutcome.failed(state, "wait battle timeout");
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

    private static String safeCloudValue(String value) {
        return value == null ? "" : value;
    }

    private ReturnItemUseResult useReturnItem(TaskExecutionContext context,
                                              String source,
                                              int attempt,
                                              int maxAttempts) {
        TaskCheckpoint.throwIfStopRequested(context, "Wubei task interrupted");
        log.info("[wubei] use return item and verify start map: source={} attempt={}/{}",
                source, attempt, maxAttempts);
        String cachedTemplate = returnItemCacheTemplateForCurrentRuntime();
        log.info("[wubei] return item cache template selected: source={} template={} probeRuntime={}",
                source, cachedTemplate, isProbeRuntimeActive());
        boolean usedCached = returnItemPrescanService.useCached(context, TASK_CODE, currentRoundNumber,
                cachedTemplate, ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                "wubei:return-home:" + source + ":attempt-" + attempt);
        if (usedCached) {
            TaskSleep.sleepOrStop(context, RETURN_VERIFY_DELAY_MS, "Wubei task interrupted");
            LocationInfo cachedReturn = playerStateService.syncMyPosition();
            if (cachedReturn != null && gameStateUtil.isSameMapName(cachedReturn.mapName, START_MAP_NAME)) {
                log.info("[wubei] cached return item verified: source={} location={}", source, cachedReturn);
                returnItemPrescanService.completeRound(context, TASK_CODE, currentRoundNumber, cachedTemplate,
                        "wubei:cached-return-verified");
                autoCombatService.reconcileReturnHomeVerifiedCombatState(
                        context, TASK_CODE, START_MAP_NAME,
                        "wubei:cached-return-verified:" + source + ":attempt-" + attempt);
                pendingTeamReturnPrecheck = teamReturnService.beginLeaderSignalPrecheck(
                        context, "wubei:return-home-verified:" + source + ":attempt-" + attempt);
                return ReturnItemUseResult.verified(cachedReturn);
            }
            log.warn("[wubei] cached return item used but start map not verified; run trusted combat probe before any further return attempt: source={} location={}",
                    source, cachedReturn);
            returnItemPrescanService.invalidate(context, TASK_CODE, currentRoundNumber, cachedTemplate,
                    "wubei:cached-return-unverified:" + source);
            return ReturnItemUseResult.usedStartMapUnverified(cachedReturn);
        }

        boolean used = bagService.findAndUseMainBagTaskPageItem(RETURN_ITEM_TEMPLATE, context);
        if (!used) {
            log.warn("[wubei] return item not found/used: source={} attempt={}/{}",
                    source, attempt, maxAttempts);
            return ReturnItemUseResult.notUsed();
        }

        /*
         * Clicking the 五倍 return item is not enough evidence. Verify that the current map changed
         * back to 宝象国 before the round is allowed to finish and the next accept-task cycle starts.
         */
        TaskSleep.sleepOrStop(context, RETURN_VERIFY_DELAY_MS, "Wubei task interrupted");
        LocationInfo afterReturn = playerStateService.syncMyPosition();
        if (afterReturn != null && gameStateUtil.isSameMapName(afterReturn.mapName, START_MAP_NAME)) {
            log.info("[wubei] return item verified: source={} location={}", source, afterReturn);
            returnItemPrescanService.completeRound(context, TASK_CODE, currentRoundNumber, cachedTemplate,
                    "wubei:return-home-verified");
            autoCombatService.reconcileReturnHomeVerifiedCombatState(
                    context, TASK_CODE, START_MAP_NAME,
                    "wubei:return-home-verified:" + source + ":attempt-" + attempt);
            pendingTeamReturnPrecheck = teamReturnService.beginLeaderSignalPrecheck(
                    context, "wubei:return-home-verified:" + source + ":attempt-" + attempt);
            return ReturnItemUseResult.verified(afterReturn);
        }
        log.warn("[wubei] return item used but start map not verified: source={} location={}",
                source, afterReturn);
        return ReturnItemUseResult.usedStartMapUnverified(afterReturn);
    }

    private String returnItemCacheTemplateForCurrentRuntime() {
        return isProbeRuntimeActive() ? PROBE_ITEM_TEMPLATE : RETURN_ITEM_TEMPLATE;
    }

    private ReturnHomeResult useReturnItemAndVerifyStartMap(TaskExecutionContext context, String source) {
        for (int attempt = 1; attempt <= RETURN_ITEM_VERIFY_ATTEMPTS; attempt++) {
            ReturnItemUseResult result = useReturnItem(context, source, attempt, RETURN_ITEM_VERIFY_ATTEMPTS);
            if (result.status() == ReturnItemUseResult.Status.VERIFIED_START_MAP) {
                recordVerifiedReturnHomeLocation(result.location(), source);
                clearTrackerGreenPathingIntent("wubei:return-home-verified:" + source);
                commonBoxService.detectLeaderBoxAfterReturnHome(context, "wubei",
                        "wubei:return-home-verified:" + source);
                return ReturnHomeResult.VERIFIED;
            }
            if (result.status() == ReturnItemUseResult.Status.USED_START_MAP_UNVERIFIED) {
                AutoCombatService.TickResult trustedState =
                        probeTrustedCombatStateAfterReturnVerificationFailure(
                                context, source + ":attempt-" + attempt);
                if (trustedState == AutoCombatService.TickResult.IN_COMBAT) {
                    pendingTeamReturnPrecheck = null;
                    return ReturnHomeResult.STILL_IN_COMBAT;
                }
                pendingTeamReturnPrecheck = null;
                return ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT;
            }
        }
        pendingTeamReturnPrecheck = null;
        return ReturnHomeResult.FAILED;
    }

    private void recordVerifiedReturnHomeLocation(LocationInfo location, String source) {
        if (location == null) {
            return;
        }
        verifiedReturnHomeLocation = location;
        log.info("[wubei] verified return-home snapshot retained until accept option: source={} location={}",
                source, location);
    }

    private void clearVerifiedReturnHomeLocation(String source) {
        if (verifiedReturnHomeLocation == null) {
            return;
        }
        log.info("[wubei] verified return-home snapshot consumed: source={} location={}",
                source, verifiedReturnHomeLocation);
        verifiedReturnHomeLocation = null;
    }

    private WubeiStepOutcome returnHomeAfterCombatOrContinueSpecialTarget(TaskExecutionContext context,
                                                                          WubeiRoundContext state) {
        if (isChainedPostBattleBroadcastSource(state.source())) {
            taskMaintenanceService.closeTeamMaintenanceWindow(context, TASK_CODE, state.round(),
                    "wubei:chained-combat-first-aid-broadcast-expired");
        }
        if (!currentRoundChainedCombatExpected) {
            ReturnHomeResult returnHome = useReturnItemAndVerifyStartMap(context, "normal-combat");
            if (returnHome != ReturnHomeResult.VERIFIED) {
                if (returnHome == ReturnHomeResult.STILL_IN_COMBAT) {
                    return resumeWaitBattleAfterTrustedReturnCorrection(
                            state, "normal-combat-return-unverified");
                }
                if (returnHome == ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT) {
                    return WubeiStepOutcome.failed(state, "return home failed");
                }
                WubeiStepOutcome stillInCombat = correctExpectedReturnFailureIfStillInCombat(
                        context, state, "normal-combat-return-unverified");
                if (stillInCombat != null) {
                    return stillInCombat;
                }
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
         * CR54 之后，第一次战后仍允许完整读 tracker 来确认并建立绿字小区域缓存；缓存建立
         * 后只验证小区域。小区域 miss/失效就是链结束信号，不能再 fallback 完整 tracker 重读。
         */
        if (currentRoundChainedTrackerCacheAttempted) {
            TaskTrackerFastMatchResult fastMatch = taskTrackerPanelService.verifyWubeiChainedTrackerFastAction(
                    currentRoundChainedTrackerFastAction,
                    "post-combat-chained-fast-" + combatCount,
                    false);
            if (!fastMatch.isMatched()) {
                log.info("[wubei] chained tracker fast-path miss-return-home: count={} reason={} "
                                + "distance={} maxDistance={} score={} elapsedMs={} click=({}, {})",
                        combatCount, fastMatch.getReason(), fastMatch.getDistance(), fastMatch.getMaxDistance(),
                        fastMatch.getScore(), fastMatch.getElapsedMs(),
                        currentRoundChainedTrackerFastAction == null ? -1 : currentRoundChainedTrackerFastAction.getAbsoluteX(),
                        currentRoundChainedTrackerFastAction == null ? -1 : currentRoundChainedTrackerFastAction.getAbsoluteY());
                ReturnHomeResult returnHome = useReturnItemAndVerifyStartMap(context, "chained-combat-fast-miss");
                if (returnHome != ReturnHomeResult.VERIFIED) {
                    if (returnHome == ReturnHomeResult.STILL_IN_COMBAT) {
                        return resumeWaitBattleAfterTrustedReturnCorrection(
                                state, "chained-combat-fast-miss-return-unverified");
                    }
                    if (returnHome == ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT) {
                        resetChainedCombatRuntime();
                        return WubeiStepOutcome.failed(state, "return home failed");
                    }
                    WubeiStepOutcome stillInCombat = correctExpectedReturnFailureIfStillInCombat(
                            context, state, "chained-combat-fast-miss-return-unverified");
                    if (stillInCombat != null) {
                        return stillInCombat;
                    }
                    resetChainedCombatRuntime();
                    return WubeiStepOutcome.failed(state, "return home failed");
                }
                resetChainedCombatRuntime();
                return WubeiStepOutcome.continueTo(
                        state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_ROUND_DONE_SOURCE),
                        "chained combat fast-path miss; check team return");
            }
            log.info("[wubei] chained tracker fast-path hit: count={} reason={} distance={} maxDistance={} "
                            + "score={} elapsedMs={} click=({}, {})",
                    combatCount, fastMatch.getReason(), fastMatch.getDistance(), fastMatch.getMaxDistance(),
                    fastMatch.getScore(), fastMatch.getElapsedMs(),
                    currentRoundChainedTrackerFastAction.getAbsoluteX(),
                    currentRoundChainedTrackerFastAction.getAbsoluteY());
            consumeChainedLeaderCachedFirstAidBeforeClick(context, combatCount, "fast-path");
            currentRoundChainedCombatContinueCount = combatCount;
            if (!clickCachedChainedTrackerGreen(context, currentRoundChainedTrackerFastAction, combatCount)) {
                log.warn("[wubei] chained combat fast-path click failed: count={} click=({}, {})",
                        combatCount,
                        currentRoundChainedTrackerFastAction.getAbsoluteX(),
                        currentRoundChainedTrackerFastAction.getAbsoluteY());
                return WubeiStepOutcome.failed(state, "chained combat fast-path click failed");
            }
            log.info("[wubei] chained combat target continues from fast path: currentCount={} nextState=ENTER_BATTLE",
                    combatCount);
            TaskSleep.sleepOrStop(context, 450L, "Wubei task interrupted");
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.ENTER_BATTLE, "chained-combat-continued-" + combatCount),
                    "chained combat fast-path clicked; resolve enter-battle dialog");
        }

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
            ReturnHomeResult returnHome = useReturnItemAndVerifyStartMap(context, "chained-combat-title-gone");
            if (returnHome != ReturnHomeResult.VERIFIED) {
                if (returnHome == ReturnHomeResult.STILL_IN_COMBAT) {
                    return resumeWaitBattleAfterTrustedReturnCorrection(
                            state, "chained-combat-title-gone-return-unverified");
                }
                if (returnHome == ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT) {
                    resetChainedCombatRuntime();
                    return WubeiStepOutcome.failed(state, "return home failed");
                }
                WubeiStepOutcome stillInCombat = correctExpectedReturnFailureIfStillInCombat(
                        context, state, "chained-combat-title-gone-return-unverified");
                if (stillInCombat != null) {
                    return stillInCombat;
                }
                resetChainedCombatRuntime();
                return WubeiStepOutcome.failed(state, "return home failed");
            }
            resetChainedCombatRuntime();
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_ROUND_DONE_SOURCE),
                    "chained combat tracker title gone; check team return");
        }

        boolean stillChained = isTrackerChainedCombatTask(postCombatPanel);
        log.info("[wubei] chained combat post-battle tracker: count={} stillChained={} taskKey={} title={} yellow='{}'",
                combatCount, stillChained, trackerTaskKey(postCombatPanel), trackerTaskTitle(postCombatPanel),
                postCombatPanel.getYellowText());
        if (!stillChained) {
            ReturnHomeResult returnHome = useReturnItemAndVerifyStartMap(context, "chained-combat-completed");
            if (returnHome != ReturnHomeResult.VERIFIED) {
                if (returnHome == ReturnHomeResult.STILL_IN_COMBAT) {
                    return resumeWaitBattleAfterTrustedReturnCorrection(
                            state, "chained-combat-completed-return-unverified");
                }
                if (returnHome == ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT) {
                    resetChainedCombatRuntime();
                    return WubeiStepOutcome.failed(state, "return home failed");
                }
                WubeiStepOutcome stillInCombat = correctExpectedReturnFailureIfStillInCombat(
                        context, state, "chained-combat-completed-return-unverified");
                if (stillInCombat != null) {
                    return stillInCombat;
                }
                resetChainedCombatRuntime();
                return WubeiStepOutcome.failed(state, "return home failed");
            }
            resetChainedCombatRuntime();
            return WubeiStepOutcome.continueTo(
                    state.next(WubeiPhase.WAIT_TEAM_RETURN, TEAM_RETURN_ROUND_DONE_SOURCE),
                    "chained combat completed; check team return");
        }

        currentTrackerPanel = postCombatPanel;
        currentRoundChainedTrackerCacheAttempted = true;
        currentRoundChainedTrackerFastAction = taskTrackerPanelService
                .prepareWubeiChainedTrackerFastAction(postCombatPanel, "post-combat-chained-cache-" + combatCount)
                .orElse(null);
        log.info("[wubei] chained tracker fast cache state: count={} cached={} yellow='{}'",
                combatCount, currentRoundChainedTrackerFastAction != null, postCombatPanel.getYellowText());
        consumeChainedLeaderCachedFirstAidBeforeClick(context, combatCount, "full-tracker");
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

    private void consumeChainedLeaderCachedFirstAidBeforeClick(TaskExecutionContext context,
                                                               int combatCount,
                                                               String source) {
        boolean consumed = playerStateService.performCachedFirstAidPlanNow(context);
        log.info("[wubei] chained combat leader cached first-aid before tracker click: count={} source={} consumed={}",
                combatCount, source, consumed);
    }

    private WubeiStepOutcome correctExpectedReturnFailureIfStillInCombat(TaskExecutionContext context,
                                                                         WubeiRoundContext state,
                                                                         String source) {
        AutoCombatService.TickResult trustedState =
                probeTrustedCombatStateAfterReturnVerificationFailure(context, source);
        if (trustedState != AutoCombatService.TickResult.IN_COMBAT) {
            log.warn("[wubei] expected combat return verification failed and trusted combat state is not active: source={} trustedState={} phase={} chained={} sourceState={}",
                    source, trustedState, state.phase(), currentRoundChainedCombatExpected, state.source());
            return null;
        }
        return resumeWaitBattleAfterTrustedReturnCorrection(state, source);
    }

    private AutoCombatService.TickResult probeTrustedCombatStateAfterReturnVerificationFailure(
            TaskExecutionContext context,
            String source) {
        AutoCombatService.TickResult trustedState = autoCombatService.probeWindowCombatStateReadOnly(
                context, "wubei:" + source);
        if (trustedState == AutoCombatService.TickResult.IN_COMBAT) {
            autoCombatService.refreshFastExpectedExitBaselineAfterTrustedInCombat(
                    "wubei:" + source + ":trusted-in-combat");
        }
        return trustedState;
    }

    private WubeiStepOutcome resumeWaitBattleAfterTrustedReturnCorrection(WubeiRoundContext state,
                                                                          String source) {
        /*
         * The fast avatar-diff exit is allowed to be early, but failed return verification is the
         * first point where we re-check trusted combat state. If the window is still fighting, go
         * back to WAIT_BATTLE_FINISH and keep deferred leader recovery pending. The trusted probe
         * has already refreshed the avatar baseline, so the next avatar diff remains enabled but
         * compares against the current in-combat frame.
         */
        log.warn("[wubei] expected combat return verification failed but trusted combat state is still IN_COMBAT; resume WAIT_BATTLE_FINISH: source={} phase={} chained={} sourceState={}",
                source, state.phase(), currentRoundChainedCombatExpected, state.source());
        waitBattleSawCombat = true;
        return waitForCombatStateWake(WubeiStepOutcome.sharedState(
                state.next(WubeiPhase.WAIT_BATTLE_FINISH, source + "-still-in-combat"),
                "return verification failed but combat is still active; wait for real exit"));
    }

    private void resetChainedCombatRuntime() {
        currentRoundChainedCombatContinueCount = 0;
        currentRoundChainedCombatRecoveryBroadcastCount = 0;
        currentRoundChainedTrackerCacheAttempted = false;
        currentRoundChainedTrackerFastAction = null;
    }

    private boolean continueChainedCombatFromTracker(
            TaskExecutionContext context,
            TaskTrackerPanelReadResult panel,
            int combatCount) {
        if (!panel.getGreenLinks().isEmpty()) {
            TaskTrackerGreenLink segment = selectedTrackerGreenLink(panel);
            String label = "chained-combat-" + combatCount;
            TrackerLinkRankerCloudDecision cloudDecision = shadowTrackerLinkSelectionIfLocal(
                    label, "wubei-chained-combat-tracker", panel.getGreenLinks(), 0, segment);
            return clickTaskTrackerGreen(context, segment, label, 1, cloudDecision);
        }
        log.warn("[wubei] chained combat tracker has no green segment; try visible tooltip: count={} yellow='{}'",
                combatCount, panel.getYellowText());
        return tryClickTrackerCombatTargetSmart(
                context, null, "chained-combat-tracker-fallback-" + combatCount, NpcTargetEvidence.CONFIRMED);
    }

    private boolean clickCachedChainedTrackerGreen(TaskExecutionContext context,
                                                   PreparedDialogAction cachedAction,
                                                   int combatCount) {
        if (cachedAction == null) {
            return false;
        }
        String label = "chained-combat-fast-" + combatCount;
        String safeLabel = safeFileToken(label);
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        String interestSource = "wubei:chained-enter-battle-before-click:" + safeLabel;
        if (runtime != null) {
            registerWubeiDialogInterest(runtime, DialogOperation.WUBEI_ENTER_BATTLE, interestSource);
        } else {
            log.info("[wubei] chained fast enter-battle interest skipped before click: label={} reason=no-window-runtime",
                    label);
        }
        int clickX = cachedAction.getAbsoluteX();
        int clickY = cachedAction.getAbsoluteY();
        TaskTrackerGreenLink cachedCandidate = TaskTrackerGreenLink.builder()
                .minX(clickX)
                .minY(clickY)
                .maxX(clickX)
                .maxY(clickY)
                .pixels(Math.max(1, (cachedAction.getValidationRight() - cachedAction.getValidationLeft())
                        * (cachedAction.getValidationBottom() - cachedAction.getValidationTop())))
                .targetMapName(cachedAction.getTargetKeyword())
                .targetMapScore(1.0D)
                .targetMapDebugPath(cachedAction.getDebugImagePath())
                .sourceType(cachedAction.getTrackerPanelSourceType())
                .build();
        if (isCloudTrackerPanelReaderCachedAction(cachedAction)) {
            log.info("[wubei] chained tracker fast uses cached TRACKER_PANEL_READER click directly: label={} count={} click=({}, {})",
                    label, combatCount, clickX, clickY);
        } else {
            boolean trackerBaseReady = tracker.refreshWindowState();
            TrackerLinkRankerCloudDecision cloudDecision = trackerLinkRankerCloudShadowService.shadowTrackerLinkSelection(
                    TASK_CODE,
                    label,
                    "wubei-chained-combat-fast",
                    List.of(cachedCandidate),
                    0,
                    cachedCandidate,
                    trackerBaseReady ? tracker.getWindowBaseX() : -1,
                    trackerBaseReady ? tracker.getWindowBaseY() : -1);
            if (!cloudDecision.isCloudExecuted()) {
                log.warn("[wubei] chained tracker fast cloud no-click: label={} count={} status={} reason={} "
                                + "cachedClick=({}, {}) validationRect=({}, {})-({}, {})",
                        label, combatCount, cloudDecision.getStatus(), cloudDecision.getRejectReason(),
                        clickX, clickY,
                        cachedAction.getValidationLeft(), cachedAction.getValidationTop(),
                        cachedAction.getValidationRight(), cachedAction.getValidationBottom());
                if (runtime != null) {
                    runtime.clearDialogInterest("wubei chained fast tracker cloud no-click before enter-battle: " + safeLabel);
                }
                return false;
            }
            Point cloudClick = resolveTrackerCloudAbsolutePoint(cloudDecision, label);
            if (cloudClick == null) {
                log.warn("[wubei] chained tracker fast cloud click unavailable: label={} count={} reason={} cloudPoint={}",
                        label, combatCount, cloudDecision.getRejectReason(),
                        cloudDecision.getCloudWindowRelativeClickPoint());
                if (runtime != null) {
                    runtime.clearDialogInterest("wubei chained fast tracker cloud no-click before enter-battle: " + safeLabel);
                }
                return false;
            }
            clickX = cloudClick.x;
            clickY = cloudClick.y;
        }
        log.info("[wubei] chained tracker fast click: label={} count={} click=({}, {}) rect=({}, {})-({}, {})",
                label, combatCount, clickX, clickY,
                cachedAction.getValidationLeft(), cachedAction.getValidationTop(),
                cachedAction.getValidationRight(), cachedAction.getValidationBottom());
        boolean clicked = inputSequences.submitAndWait("wubei:tracker-green-click:" + label, List.of(
                InputAction.moveMouse(clickX, clickY),
                InputAction.sleep(120),
                InputAction.clickLeft(clickX, clickY, 300)
        ));
        if (!clicked && runtime != null) {
            runtime.clearDialogInterest("wubei chained fast tracker click failed before enter-battle: " + safeLabel);
        }
        log.info("[wubei] chained tracker fast click completed: label={} clicked={} click=({}, {})",
                label, clicked, clickX, clickY);
        return clicked;
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

    private enum ReturnHomeResult {
        VERIFIED,
        STILL_IN_COMBAT,
        FAILED_AFTER_TRUSTED_NOT_IN_COMBAT,
        FAILED
    }

    private record ReturnItemUseResult(Status status, LocationInfo location) {
        private enum Status {
            VERIFIED_START_MAP,
            USED_START_MAP_UNVERIFIED,
            NOT_USED
        }

        private static ReturnItemUseResult verified(LocationInfo location) {
            return new ReturnItemUseResult(Status.VERIFIED_START_MAP, location);
        }

        private static ReturnItemUseResult usedStartMapUnverified(LocationInfo location) {
            return new ReturnItemUseResult(Status.USED_START_MAP_UNVERIFIED, location);
        }

        private static ReturnItemUseResult notUsed() {
            return new ReturnItemUseResult(Status.NOT_USED, null);
        }
    }

    private record TrackerDestinationHint(String mapName, int x, int y, String rawText) {
    }

    private record WubeiPrepathTarget(String mapName, int x, int y, String name, String reason) {
    }

    private record WubeiWaitRuntimeState(
            WindowPathingState pathingState,
            long pathingAgeMs,
            DialogOperation preparedOperation,
            String preparedTarget,
            long preparedAgeMs,
            DialogType visibleDialogType,
            long visibleDialogAgeMs,
            WindowReadyEventType readyEventType,
            long readyEventAgeMs,
            String pathingTerminalSource,
            long pathingTerminalSequence,
            long pathingTerminalAgeMs,
            String pathingTerminalMatchBasis,
            boolean satisfied) {
    }

    private record PathingTerminalMatch(boolean matched,
                                        String source,
                                        long sequence,
                                        long ageMs,
                                        String matchBasis) {
        private static PathingTerminalMatch none() {
            return new PathingTerminalMatch(false, null, -1L, -1L, null);
        }
    }

    private static class ProbeEnterBattleTimeoutSignal extends RuntimeException {
        private final WubeiStepOutcome outcome;

        private ProbeEnterBattleTimeoutSignal(WubeiStepOutcome outcome) {
            super(outcome == null ? "probe enter-battle timeout" : outcome.message(), null, false, false);
            this.outcome = outcome;
        }

        private WubeiStepOutcome outcome() {
            return outcome;
        }
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
