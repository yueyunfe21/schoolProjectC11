package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.cloud.decision.CloudDecisionProperties;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.cloud.task.TaskPolicyCloudDecision;
import com.bot.dhxy.cloud.task.TaskPolicyCloudDecisionService;
import com.bot.dhxy.cloud.task.TaskRecoveryCloudDecision;
import com.bot.dhxy.cloud.task.TaskRecoveryCloudDecisionService;
import com.bot.dhxy.cloud.task.TrackerLinkRankerCloudDecision;
import com.bot.dhxy.cloud.task.TrackerLinkRankerCloudShadowService;
import com.bot.dhxy.cloud.xiuluo.XiuluoBrainActionType;
import com.bot.dhxy.cloud.xiuluo.XiuluoBrainActionOutcomeDecision;
import com.bot.dhxy.cloud.xiuluo.XiuluoBrainActionOutcomeRequest;
import com.bot.dhxy.cloud.xiuluo.XiuluoBrainCloudDecisionService;
import com.bot.dhxy.cloud.xiuluo.XiuluoBrainDecision;
import com.bot.dhxy.cloud.xiuluo.XiuluoBrainResponse;
import com.bot.dhxy.cloud.xiuluo.XiuluoBrainStartRequest;
import com.bot.dhxy.cloud.xiuluo.XiuluoBrainStepRequest;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.job.PreparedActionJob;
import com.bot.dhxy.model.job.PreparedActionJobType;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.model.maintenance.TaskMaintenanceResult;
import com.bot.dhxy.model.maintenance.TaskMaintenanceStatus;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.model.npc.NpcTooltipType;
import com.bot.dhxy.model.npc.DirectCombatClickResult;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.pause.TaskPauseResumeFingerprint;
import com.bot.dhxy.model.pause.TaskPauseResumeReconcileResult;
import com.bot.dhxy.model.quest.QuestDetailCapture;
import com.bot.dhxy.model.tasktracker.TaskTrackerGreenLink;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelSourceType;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.exception.TaskFatalException;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.BattleRadarService;
import com.bot.dhxy.service.CommonBoxService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.MemoryService;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.service.NpcClickService;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.service.QuestManagerService;
import com.bot.dhxy.service.ReturnItemPrescanService;
import com.bot.dhxy.service.TaskMaintenanceService;
import com.bot.dhxy.service.TaskTrackerPanelService;
import com.bot.dhxy.service.TeamReturnService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.service.dialog.DialogOptionPolicy;
import com.bot.dhxy.service.dialog.DialogStoryPolicy;
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
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.vision.ObjectiveTextRecognitionService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowNativeBinding;
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
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String TASK_NAME = "修罗";
    private static final String START_MAP_NAME = "灵兽村";
    private static final String ACCEPT_NPC_NAME = "灵兽村使者";
    private static final String HEAL_PET_NPC_NAME = "超级巫医";
    private static final String REPAIR_EQUIPMENT_NPC_NAME = "李道宗";
    private static final String REPAIR_EQUIPMENT_MAP_NAME = "洛阳城";
    private static final int ACCEPT_NPC_X = 112;
    private static final int ACCEPT_NPC_Y = 93;
    private static final int HEAL_PET_NPC_X = 116;
    private static final int HEAL_PET_NPC_Y = 70;
    private static final int REPAIR_EQUIPMENT_NPC_X = 324;
    private static final int REPAIR_EQUIPMENT_NPC_Y = 109;
    // isNearCoordinate uses per-axis tolerance; 灵兽村 (101,83) -> 接任务 NPC (112,93) needs 11.
    private static final int TASK_NPC_DIRECT_CLICK_DISTANCE = 11;
    private static final int START_EXIT_X = 11;
    private static final int START_EXIT_Y = 8;
    private static final int START_EXIT_PREPATH_SKIP_DISTANCE = 3;
    private static final int PREPATH_MINI_MAP_CLICK_RANDOM_RADIUS_PX = 12;
    private static final String XIULUO_TARGET_KEYWORD = "修罗";
    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/xiuluo/xiuluo_accept_xianlaiwu2.png";
    private static final int ACCEPT_OPTION_ROI_X = 250;
    private static final int ACCEPT_OPTION_ROI_Y = 312;
    private static final int ACCEPT_OPTION_ROI_W = 529;
    private static final int ACCEPT_OPTION_ROI_H = 208;
    private static final int ACCEPT_OPTION_TEMPLATE_CLICK_OFFSET_X = 48;
    private static final double ACCEPT_OPTION_TEMPLATE_MATCH_RATE = 0.82;
    private static final long ACCEPT_DIALOG_CLOUD_FALLBACK_TTL_MS = 180_000L;
    private static final long ACCEPT_DIALOG_CLOUD_FALLBACK_POLL_MS = 200L;
    private static final String CANCEL_TASK_OPTION_TEMPLATE = "images/template/dialog/xiuluo/xiuluo_cancel_task.png";
    private static final String UNDER_FIVE_CONFIRM_TEMPLATE = "images/template/dialog/xiuluo/xiuluo_underfive_confirm.png";
    private static final String UNDER_FIVE_WAIT_TEMPLATE = "images/template/dialog/xiuluo/xiuluo_underfive_wait.png";
    private static final String UNDER_THREE_BLOCKED_TEMPLATE = "images/template/dialog/xiuluo/xiuluo_underthree_yichangqiangda.png";
    private static final String ENTER_BATTLE_TEMPLATE = XiuluoDialogCatalog.ENTER_BATTLE_TEMPLATE;
    private static final String XIULUO_WILD_MONSTER_CANCEL_TEMPLATE = "images/template/dialog/xiuluo/xiuluo_wild_monster_cancel.png";
    private static final String HEAL_PET_OPTION_TEMPLATE = "images/template/dialog/maintenance/heal_pet_option.png";
    private static final String HEAL_PET_NPC_TOOLTIP_TEMPLATE = "images/template/npc/npc_wuyi_tooltip.png";
    private static final String REPAIR_EQUIPMENT_OPTION_TEMPLATE = "images/template/dialog/maintenance/repair_equipment_option.png";
    private static final String REPAIR_EQUIPMENT_TOOLTIP_TEMPLATE = "images/template/npc/npc_xiuli_tooltip.png";
    private static final String RETURN_ITEM_TEMPLATE = "bag/xiuluo_return_item.png";
    private static final String ACCEPT_TASK_ACTION = "acceptTask";
    private static final String OPTION_ACCEPT_TASK = "xiuluo.acceptTask";
    private static final String OPTION_CANCEL_TASK_VISIBLE = "xiuluo.cancelTaskVisible";
    private static final String OPTION_ENTER_BATTLE = XiuluoDialogCatalog.OPTION_ENTER_BATTLE;
    private static final String OPTION_WILD_MONSTER_CANCEL = "xiuluo.wildMonsterCancel";
    private static final String OPTION_UNDER_FIVE_CONFIRM = "xiuluo.underFiveConfirm";
    private static final String OPTION_UNDER_FIVE_WAIT = "xiuluo.underFiveWait";
    private static final String DIALOG_UNDER_THREE_BLOCKED = "xiuluo.underThreeBlocked";
    private static final String BUSINESS_ACTION_HEAL_PET = "heal-pet";
    private static final String BUSINESS_ACTION_REPAIR_EQUIPMENT = "repair-equipment";
    private static final int MAX_PHASE_RETRY = 1;
    private static final int MAX_RECOVERY_COUNT = 2;
    private static final int MAX_CONSECUTIVE_ROUND_FAILURES = 10;
    private static final int UNKNOWN_COMBAT_TARGET_DISTANCE_TOLERANCE = 10;
    private static final int RETURN_ITEM_VERIFY_ATTEMPTS = 2;
    private static final long RETURN_VERIFY_DELAY_MS = 500L;
    private static final long TASK_TURN_HANDOFF_DELAY_MS = 900L;
    private static final long WAIT_COMBAT_MAINTENANCE_WAKE_MIN_TIMEOUT_MS = 500L;
    private static final long WAIT_COMBAT_MAINTENANCE_WAKE_MAX_TIMEOUT_MS = 10_000L;
    private static final long WAIT_TARGET_PATHING_TERMINAL_TIMEOUT_MS = -1L;
    private static final long PRE_COMBAT_WATCHDOG_TIMEOUT_MS = 180_000L;
    private static final long RUNNER_PATHING_HARD_TIMEOUT_MS = 180_000L;
    private static final long MAINTENANCE_BROADCAST_HANDOFF_DELAY_MS = 3_000L;
    private static final long OBSERVER_SNAPSHOT_MAX_AGE_MS = 3_000L;
    private static final long OBSERVER_PROBE_MAX_AGE_MS = 10_000L;
    private static final long PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS = 10_000L;
    private static final int MAX_MAINTENANCE_HOOK_ATTEMPTS = 5;
    private static final int TASK_NPC_SHORT_PATH_KEEP_TURN_DISTANCE = 15;
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int ENTER_BATTLE_CONFIRM_NONE_TICKS = 4;
    private static final int MAX_ENTER_BATTLE_CONFIRM_RETRIES = 2;
    /**
     * CR232: hard cap on cloud-issued, actually-executed saved-green fallback re-presses per
     * round. Stale cloud responses, undelivered commands, and physically failed re-presses do NOT
     * count. Reaching the cap abandons the shortcut for the round via the existing recovery chain.
     */
    private static final int MAX_CLOUD_ENTER_BATTLE_FALLBACKS = 3;
    /**
     * CR232: delay before the watcher-side local kanda2 small-ROI probe may start. The spec anchor
     * is accept-success + 25s; this delay is anchored on the first tracker green click instead,
     * which lands about 5s after accept success, so 20s here reproduces the spec timing (user
     * confirmed 2026-07-09).
     */
    private static final long ENTER_BATTLE_LOCAL_PROBE_DELAY_MS = 20_000L;
    private static final long DEFAULT_TEAM_READY_WAIT_POLL_MS = 3_000L;
    private static final String UNDER_THREE_WAIT_SOURCE_PREFIX = "under-three-wait";
    private static final String TEAM_RETURN_WAIT_SOURCE_PREFIX = "team-return-wait";
    private static final String TEAM_RETURN_BEFORE_ACCEPT_SOURCE = TEAM_RETURN_WAIT_SOURCE_PREFIX + ":before-accept";
    private static final String TEAM_RETURN_ROUND_DONE_SOURCE = TEAM_RETURN_WAIT_SOURCE_PREFIX + ":round-done";
    /** CR244 Gate B: blocked right before the accept-option atomic click; wake returns to ACCEPT_TASK_DIALOG. */
    private static final String TEAM_RETURN_GATE_B_SOURCE = TEAM_RETURN_WAIT_SOURCE_PREFIX + ":gate-b";
    /**
     * CR244: insurance timeout for the pending-return event park. Timeout wake-ups only re-read the
     * member-owned set; they are never treated as "members returned" and never busy-poll the turn.
     */
    private static final long TEAM_RETURN_STATE_WAKE_TIMEOUT_MS = 20_000L;
    /**
     * CR245: leader insurance cap for the maintenance broadcast queue. Unlike CR244, reaching this
     * cap IS a release by design (user decision 2026-07-10): remaining members are dropped and the
     * next cooldown round re-triggers.
     */
    private static final long MAINTENANCE_BROADCAST_QUEUE_CAP_MS = 5_000L;
    /**
     * CR245: courtesy wait when the leader has no confirmed local members — the other teammates are
     * real players outside this program's control, so give them fixed reaction time instead.
     */
    private static final long MAINTENANCE_NO_LOCAL_MEMBER_COURTESY_WAIT_MS = 3_000L;
    /** CR245: freshness bound for the parked leader's background self-confirm probe point. */
    private static final long MAINTENANCE_SELF_CONFIRM_PROBE_TTL_MS = 10_000L;
    private static final String MAINTENANCE_BROADCAST_HANDOFF_SOURCE_PREFIX = "maintenance-broadcast-handoff";
    private static final String TRACKER_SHORTCUT_PATHING_SOURCE_PREFIX = "xiuluo-v2:tracker-shortcut";
    private static final String REPAIR_EQUIPMENT_DONE_SOURCE = "repair-equipment-done";
    private static final String NAV_MSG_CURRENT_MAP_PATHING_STARTED = "current-map mini-map click started pathing";
    private static final String NAV_MSG_WORLD_MAP_ROUTE_CLICKED = "world-map route clicked";
    private static final String NAV_MSG_ROUTE_DIALOG_CLICKED_BEFORE_PATHING_GUARD =
            "route dialog clicked before pathing guard; observer will confirm pathing";
    private static final String NAV_MSG_ROUTE_DIALOG_CLICKED_BEFORE_WORLD_MAP =
            "route dialog clicked before world-map search";
    private static final String NAV_MSG_SAME_TARGET_ROUTE_PENDING =
            "same target route already submitted; watcher will confirm pathing";
    private static final String NAV_MSG_SAME_TARGET_ROUTE_PENDING_BEFORE_WORLD_MAP =
            "same target route already submitted before world-map search; watcher will confirm pathing";
    private static final Pattern TASK_PANEL_OBJECTIVE_PATTERN =
            Pattern.compile("前往\\s*([^\\(（\\s:：，,。]+?)\\s*[\\(（]\\s*(\\d{1,4})\\s*[,，]\\s*(\\d{1,4})\\s*[\\)）]?");
    private static final Path XIULUO_FAILURE_CASE_DIR = Path.of("images", "failure-cases", "xiuluo");
    private static final Path XIULUO_FAILURE_CASE_REPORT =
            Path.of("docs", "run-reports", "xiuluo-failure-cases.md");
    private static final DateTimeFormatter FAILURE_CASE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final DateTimeFormatter CONSOLE_LOG_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Object XIULUO_FAILURE_CASE_REPORT_MONITOR = new Object();
    private static final int MAX_ROUND_TRACE_EVENTS = 400;
    private static final NpcTarget ACCEPT_NPC = NpcTarget.builder()
            .key("xiuluo.acceptNpc")
            .mapName(START_MAP_NAME)
            .name(ACCEPT_NPC_NAME)
            .x(ACCEPT_NPC_X)
            .y(ACCEPT_NPC_Y)
            .role(NpcRole.QUEST_GIVER)
            .movementType(NpcMovementType.FIXED)
            .expectedDialogTemplatePath(ACCEPT_OPTION_TEMPLATE)
            .deferDialogVerificationToTask(true)
            .source("xiuluo-v2")
            .build();
    private static final NpcTarget HEAL_PET_NPC = NpcTarget.builder()
            .key("xiuluo.healPetNpc")
            .mapName(START_MAP_NAME)
            .name(HEAL_PET_NPC_NAME)
            .x(HEAL_PET_NPC_X)
            .y(HEAL_PET_NPC_Y)
            .role(NpcRole.INTERACTION_TARGET)
            .movementType(NpcMovementType.FIXED)
            .tooltipType(NpcTooltipType.WUYI)
            .tooltipTemplatePath(HEAL_PET_NPC_TOOLTIP_TEMPLATE)
            .expectedDialogTemplatePath(HEAL_PET_OPTION_TEMPLATE)
            .source("xiuluo-v2")
            .build();
    private static final NpcTarget REPAIR_EQUIPMENT_NPC = NpcTarget.builder()
            .key("xiuluo.repairEquipmentNpc")
            .mapName(REPAIR_EQUIPMENT_MAP_NAME)
            .name(REPAIR_EQUIPMENT_NPC_NAME)
            .x(REPAIR_EQUIPMENT_NPC_X)
            .y(REPAIR_EQUIPMENT_NPC_Y)
            .role(NpcRole.INTERACTION_TARGET)
            .movementType(NpcMovementType.FIXED)
            .tooltipTemplatePath(REPAIR_EQUIPMENT_TOOLTIP_TEMPLATE)
            .expectedDialogTemplatePath(REPAIR_EQUIPMENT_OPTION_TEMPLATE)
            .source("xiuluo-v2")
            .build();

    private final BotProperties botProperties;
    private final GameContext gameContext;
    private final GameClientTracker tracker;
    private final NavigationService navigationService;
    private final CoordinateHelper coordinateHelper;
    private final GameStateUtil gameStateUtil;
    private final NpcClickService npcClickService;
    private final DialogService dialogService;
    private final MemoryService memoryService;
    private final QuestManagerService questManagerService;
    private final ObjectiveTextRecognitionService objectiveTextRecognitionService;
    private final com.bot.dhxy.cloud.task.ObjectiveTextReaderCloudDecisionService objectiveTextReaderCloudDecisionService;
    private final TextRecognizer textRecognizer;
    private final AutoCombatService autoCombatService;
    private final BattleRadarService battleRadarService;
    private final BagService bagService;
    private final ReturnItemPrescanService returnItemPrescanService;
    private final PlayerStateService playerStateService;
    private final TaskMaintenanceService taskMaintenanceService;
    private final CommonBoxService commonBoxService;
    private final TaskTrackerPanelService taskTrackerPanelService;
    private final UICleanerService uiCleanerService;
    private final TeamReturnService teamReturnService;
    private final XiuluoHotStartResolver hotStartResolver;
    private final TaskPauseResumeReconciler taskPauseResumeReconciler;
    private final TaskTransactionRunner taskTransactionRunner;
    private final TaskTurnCoordinator taskTurnCoordinator;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final MultiWindowTaskManager multiWindowTaskManager;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowReadyEventBus windowReadyEventBus;
    private final InputSequences inputSequences;
    private final AutomationMetricsService automationMetricsService;
    private final TrackerLinkRankerCloudShadowService trackerLinkRankerCloudShadowService;
    private final CloudDecisionProperties cloudDecisionProperties;
    private final XiuluoBrainCloudDecisionService xiuluoBrainCloudDecisionService;
    private final TaskPolicyCloudDecisionService taskPolicyCloudDecisionService;
    private final TaskRecoveryCloudDecisionService taskRecoveryCloudDecisionService;
    private final BoundWindowCaptureService boundWindowCaptureService;

    /*
     * Startup incense is a task-run guard, not the incense cooldown itself. PlayerStateService still
     * owns the real time/status rules; Xiuluo only decides the first safe point to ask that service.
     */
    private boolean startupIncenseChecked;
    private boolean startupIncensePending;
    private long lastHealPetMaintenanceAt;
    private long lastRepairEquipmentMaintenanceAt;
    private long lastPostCombatIdleTimeoutConsumedSeq;
    private volatile CompletableFuture<Optional<PreparedDialogAction>> acceptDialogCloudFallbackFuture;
    private volatile int acceptDialogCloudFallbackRound;
    /** CR245: hook label whose maintenance broadcast queue is currently draining for this leader. */
    private volatile String pendingMaintenanceQueueHook;
    private volatile CompletableFuture<Point> maintenanceSelfConfirmProbeFuture;
    private volatile long maintenanceSelfConfirmProbeStartedAtMs;
    /**
     * Latest position sync that confirmed the leader is on 灵兽村, kept so the next accept-NPC
     * navigation can reuse it as the caller-fresh location instead of re-running the stale-cache
     * map confirmation. NavigationService only trusts it within its own 3s freshness window.
     */
    private volatile LocationInfo lastStartMapVerifiedLocation;
    private volatile long lastStartMapVerifiedAtMs;

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
        lastPostCombatIdleTimeoutConsumedSeq = windowReadyEventBus.currentSequence();
        int maxRuns = botProperties.getXiuluoMaxRuns();
        int completedRuns = 0;
        startupIncenseChecked = false;
        startupIncensePending = false;
        long maintenanceStartAt = botProperties.isXiuluoMaintenanceRunImmediatelyOnStart()
                ? 0L
                : System.currentTimeMillis();
        lastHealPetMaintenanceAt = maintenanceStartAt;
        lastRepairEquipmentMaintenanceAt = maintenanceStartAt;
        gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        taskMaintenanceService.initializeForTaskStart(context, TASK_CODE);
        if (context.getWindowRuntimeContext() != null) {
            context.getWindowRuntimeContext().updateTaskRunProgress(completedRuns, maxRuns);
        }
        log.info("[xiuluo-v2] skeleton started: maxRuns={} maintenanceRunImmediatelyOnStart={}",
                isUnlimitedRuns(maxRuns) ? "unlimited" : maxRuns,
                botProperties.isXiuluoMaintenanceRunImmediatelyOnStart());

        try {
            while (shouldStartNextRound(maxRuns, completedRuns)) {
                TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
                int round = completedRuns + 1;
                clearTrackerShortcutPathingIntent(context.getWindowRuntimeContext(), "xiuluo-v2:round-start");
                if (completedRuns == 0) {
                    playerStateService.performStartupFirstAidCheck(context);
                    ensureStartupIncenseBeforeHotStart(context);
                }
                if (isXiuluoBrainLoopEnabled()) {
                    XiuluoRoundContext cloudMetricContext = XiuluoRoundContext.start(round);
                    String roundId = roundMetricId(context, TASK_CODE, round);
                    long roundStartedAt = System.currentTimeMillis();
                    automationMetricsService.recordRoundStarted(context, roundId, round,
                            roundMetricType(cloudMetricContext), "修罗云脑轮次开始",
                            Map.of("sourcePhase", cloudMetricContext.phase().name(),
                                    "source", cloudMetricContext.source()));
                    TaskRunResult roundResult = runRoundWithXiuluoBrain(context, round);
                    finishRoundMetric(context, roundId, round, cloudMetricContext, roundResult, roundStartedAt,
                            "修罗云脑轮次结束");
                    if (roundResult != TaskRunResult.SUCCESS) {
                        gameContext.setBotStatus(roundResult == TaskRunResult.STOPPED
                                ? GameContext.BotStatus.IDLE
                                : GameContext.BotStatus.ERROR);
                        return roundResult;
                    }
                    completedRuns++;
                    if (context.getWindowRuntimeContext() != null) {
                        context.getWindowRuntimeContext().updateTaskRunProgress(completedRuns, maxRuns);
                    }
                    log.info("[xiuluo-v2] round {} cloud-brain scaffold finished, completed={}",
                            round, completedRuns);
                    continue;
                }
                // 🧭 STARTUP RESUME: true task startup can resume from tracker/return-item evidence.
                /*
                 * Normal startup and CR98 after-combat startup are the same resume shape: first
                 * trust the left tracker shortcut if it has an actionable 修罗 green link, then try
                 * the task return item before falling back to the accept-task chain. The older
                 * Alt+Q task-panel OCR startup path remains in this class as a legacy diagnostic path,
                 * but it is no longer the first startup decision.
                 */
                boolean afterCombatExitStartup = completedRuns == 0 && context.isAfterCombatExitStartup();
                boolean cleanQueueTransitionStartup = completedRuns == 0 && context.isCleanQueueTransitionStartup();
                XiuluoRoundContext roundContext;
                if (completedRuns == 0 && cleanQueueTransitionStartup) {
                    log.info("[xiuluo-v2] skip startup-screen resume because clean queued task transition; force accept NPC navigation");
                    roundContext = XiuluoRoundContext.routeToAcceptNpc(round);
                } else if (completedRuns == 0) {
                    roundContext = resolveTaskHotStart(context, XiuluoRoundContext.start(round),
                            afterCombatExitStartup
                                    ? "after-combat-exit-startup-screen-resume"
                                    : "startup-screen-resume");
                } else {
                    roundContext = XiuluoRoundContext.start(round);
                }
                log.info("[xiuluo-v2] round {} initial phase: phase={} source={} objective={}",
                        round, roundContext.phase(), roundContext.source(), roundContext.objective());
                taskMaintenanceService.beginTeamMaintenanceRound(context, TASK_CODE, round,
                        "xiuluo-v2:round-start");
                String roundId = roundMetricId(context, TASK_CODE, round);
                long roundStartedAt = System.currentTimeMillis();
                automationMetricsService.recordRoundStarted(context, roundId, round,
                        roundMetricType(roundContext), "修罗轮次开始",
                        Map.of("sourcePhase", roundContext.phase().name(), "source", roundContext.source()));

                // 🧩 ROUND EXECUTION: run the selected phase chain until ROUND_DONE/FAILED/STOPPED.
                XiuluoRoundTrace roundTrace = XiuluoRoundTrace.start(context, roundContext);
                TaskRunResult roundResult;
                try {
                    roundResult = runRoundPhases(context, roundContext, roundTrace);
                } catch (RuntimeException e) {
                    finishRoundMetric(context, roundId, round, roundContext, TaskRunResult.FAILED,
                            roundStartedAt, "修罗轮次异常: " + e.getClass().getSimpleName());
                    throw e;
                }
                finishRoundMetric(context, roundId, round, roundContext, roundResult, roundStartedAt,
                        "修罗轮次结束");
                if (roundResult != TaskRunResult.SUCCESS) {
                    gameContext.setBotStatus(roundResult == TaskRunResult.STOPPED
                            ? GameContext.BotStatus.IDLE
                            : GameContext.BotStatus.ERROR);
                    return roundResult;
                }

                completedRuns++;
                if (context.getWindowRuntimeContext() != null) {
                    context.getWindowRuntimeContext().updateTaskRunProgress(completedRuns, maxRuns);
                }
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

    private String roundMetricId(TaskExecutionContext context, String taskCode, int round) {
        long taskRunId = context == null ? 0L : context.getTaskRunId();
        String windowId = context == null ? "window" : context.getWindowId();
        return taskCode + "-" + (taskRunId > 0L ? taskRunId : windowId) + "-round-" + round;
    }

    private void finishRoundMetric(TaskExecutionContext context,
                                   String roundId,
                                   int round,
                                   XiuluoRoundContext roundContext,
                                   TaskRunResult result,
                                   long roundStartedAt,
                                   String message) {
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - roundStartedAt);
        automationMetricsService.recordRoundFinished(context, roundId, round, roundMetricType(roundContext),
                roundMetricStatus(result), roundResultCode(result), message, elapsedMs,
                Map.of("sourcePhase", roundContext.phase().name(), "source", roundContext.source()));
    }

    private String roundMetricType(XiuluoRoundContext roundContext) {
        String objective = roundContext == null ? null : objectiveSummary(roundContext.objective());
        if (objective != null && !objective.isBlank()) {
            return objective;
        }
        return roundContext == null ? "修罗" : "修罗/" + roundContext.routeMode();
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

    private boolean isXiuluoBrainLoopEnabled() {
        return cloudDecisionProperties != null
                && cloudDecisionProperties.service(CloudDecisionServiceId.XIULUO_BRAIN).isExecuteEnabled();
    }

    /**
     * Runs the CR195 cloud-brain scaffold for one 修罗 round.
     *
     * @param context current task execution context; window id and task run id are local safety
     *                identity fields passed to `XIULUO_BRAIN`
     * @param round one-based 修罗 round number used only for diagnostics in this scaffold
     * @return SUCCESS only when cloud returns a terminal complete command, STOPPED for an explicit
     *         cloud stop command, otherwise FAILED fail-closed without entering the old local brain
     */
    private TaskRunResult runRoundWithXiuluoBrain(TaskExecutionContext context, int round) {
        String windowId = xiuluoBrainWindowId(context);
        String taskRunId = xiuluoBrainTaskRunId(context);
        if (windowId.isBlank() || taskRunId.isBlank()) {
            return failClosedXiuluoBrainLoop(round, null, null,
                    "missing local identity windowId=" + windowId + " taskRunId=" + taskRunId);
        }

        XiuluoRoundTrace roundTrace = XiuluoRoundTrace.start(context, XiuluoRoundContext.start(round));
        Map<String, String> facts = collectXiuluoBrainHotStartFacts(context, round, "start");
        log.info("xiuluo.brain.loop.start xiuluo.brain.start round={} windowId={} taskRunId={} source=cr197-hot-start",
                round, windowId, taskRunId);
        XiuluoBrainDecision decision = xiuluoBrainCloudDecisionService.start(
                XiuluoBrainStartRequest.builder()
                        .taskCode(TASK_CODE)
                        .source("xiuluo-v2:cr197-hot-start")
                        .windowId(windowId)
                        .taskRunId(taskRunId)
                        .initialPhase(null)
                        .context(facts)
                        .build());

        XiuluoBrainRoundState cloudRoundState = XiuluoBrainRoundState.start(round);
        while (true) {
            long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested(
                    context, taskExecutionContextHolder, "Xiuluo V2 cloud brain interrupted");
            if (pauseBlockedMs > 0L) {
                // A user pause parked at this checkpoint must not burn the pre-combat watchdog
                // budget of the round context created at hot-start.
                cloudRoundState.adjustCurrent(state -> compensatePreCombatTimerAfterMaintenance(
                        state, pauseBlockedMs, state.source() + ":user-pause"));
            }
            if (decision == null || !decision.isAcceptedCloudCommand()) {
                String reason = decision == null ? "cloud decision missing" : decision.getRejectReason();
                roundTrace.addCloudFailure(cloudRoundState.current(), reason);
                decision = restartXiuluoBrainAfterFailure(context, round, cloudRoundState, roundTrace, reason);
                cloudRoundState = XiuluoBrainRoundState.start(round);
                roundTrace = XiuluoRoundTrace.start(context, XiuluoRoundContext.start(round));
                continue;
            }

            XiuluoBrainResponse command = decision.getResponse();
            roundTrace.addCloudCommand(cloudRoundState.current(), command);
            log.info("xiuluo.brain.loop.command round={} windowId={} taskRunId={} sessionId={} stateSeq={} "
                            + "phaseToken={} actionType={} phase={} actionId={} reason={}",
                    round, windowId, taskRunId, command.getSessionId(), command.getStateSeq(),
                    command.getPhaseToken(), command.getActionType(), command.getPhase(), command.getActionId(),
                    command.getReason());

            XiuluoBrainShellResult shellResult = executeXiuluoBrainCommandShell(
                    context, cloudRoundState, command, roundTrace);
            if (!shellResult.accepted()) {
                roundTrace.addCloudFailure(cloudRoundState.current(), shellResult.reason());
                decision = restartXiuluoBrainAfterFailure(context, round, cloudRoundState, roundTrace,
                        "command shell rejected: " + shellResult.reason());
                cloudRoundState = XiuluoBrainRoundState.start(round);
                roundTrace = XiuluoRoundTrace.start(context, XiuluoRoundContext.start(round));
                continue;
            }
            if (shellResult.reportOutcome()) {
                if (context.isStopRequested()) {
                    return XiuluoBrainShellResult.terminalStopped(
                            "cloud brain stopped before action outcome report", shellResult.outcome()).taskRunResult();
                }
                Map<String, String> outcomeFacts =
                        xiuluoBrainOutcomeFacts(context, round, command, shellResult.outcome());
                if (!shellResult.extraOutcomeFacts().isEmpty()) {
                    Map<String, String> mergedOutcomeFacts = new LinkedHashMap<>(outcomeFacts);
                    mergedOutcomeFacts.putAll(shellResult.extraOutcomeFacts());
                    outcomeFacts = Map.copyOf(mergedOutcomeFacts);
                }
                XiuluoBrainActionOutcomeRequest outcomeRequest = XiuluoBrainActionOutcomeRequest.builder()
                        .taskCode(TASK_CODE)
                        .source("xiuluo-v2:cr198-action-outcome")
                        .windowId(windowId)
                        .taskRunId(taskRunId)
                        .sessionId(command.getSessionId())
                        .stateSeq(command.getStateSeq())
                        .phaseToken(command.getPhaseToken())
                        .phase(command.getPhase())
                        .actionId(command.getActionId())
                        .outcome(shellResult.outcomeStatus())
                        .transactionResult(shellResult.transactionResult())
                        .yieldPolicy(shellResult.yieldPolicy())
                        .localOutcomeNextPhase(shellResult.localOutcomeNextPhase())
                        .message(shellResult.reason())
                        .context(outcomeFacts)
                        .build();
                XiuluoBrainActionOutcomeDecision outcomeAck =
                        xiuluoBrainCloudDecisionService.actionOutcome(outcomeRequest);
                if (context.isStopRequested()) {
                    return XiuluoBrainShellResult.terminalStopped(
                            "cloud brain stopped after action outcome report", shellResult.outcome()).taskRunResult();
                }
                if (outcomeAck == null || !outcomeAck.isAcceptedOutcome()) {
                    if (outcomeAck != null && outcomeAck.isResetRequired()) {
                        cloudRoundState.recordOutcome(shellResult.outcome());
                        decision = restartXiuluoBrainAfterSessionReset(
                                context, round, command, shellResult, outcomeAck, outcomeFacts);
                        if (decision == null || !decision.isAcceptedCloudCommand()) {
                            String reason = decision == null
                                    ? "cloud brain reset returned no command"
                                    : decision.getRejectReason();
                            roundTrace.addCloudFailure(cloudRoundState.current(), reason);
                            decision = restartXiuluoBrainAfterFailure(context, round, cloudRoundState, roundTrace,
                                    reason);
                            cloudRoundState = XiuluoBrainRoundState.start(round);
                            roundTrace = XiuluoRoundTrace.start(context, XiuluoRoundContext.start(round));
                        }
                        continue;
                    }
                    String reason = outcomeAck == null ? "action outcome ack missing" : outcomeAck.getRejectReason();
                    roundTrace.addCloudFailure(cloudRoundState.current(), reason);
                    decision = restartXiuluoBrainAfterFailure(context, round, cloudRoundState, roundTrace,
                            "action outcome rejected: " + reason);
                    cloudRoundState = XiuluoBrainRoundState.start(round);
                    roundTrace = XiuluoRoundTrace.start(context, XiuluoRoundContext.start(round));
                    continue;
                }
                cloudRoundState.recordOutcome(shellResult.outcome());
                if (command.getActionType() == XiuluoBrainActionType.RESTART_ROUND) {
                    roundTrace = XiuluoRoundTrace.start(context, XiuluoRoundContext.start(round));
                }
            }
            if (shellResult.terminal()) {
                if (shellResult.taskRunResult() == TaskRunResult.FAILED) {
                    String reason = "cloud terminal failure: " + shellResult.reason();
                    roundTrace.addCloudFailure(cloudRoundState.current(), reason);
                    decision = restartXiuluoBrainAfterFailure(context, round, cloudRoundState, roundTrace, reason);
                    cloudRoundState = XiuluoBrainRoundState.start(round);
                    roundTrace = XiuluoRoundTrace.start(context, XiuluoRoundContext.start(round));
                    continue;
                }
                return shellResult.taskRunResult();
            }
            /*
             * CR207 P1: only immediate cloud/local churn should consume the hot-loop guard. A real
             * WAIT_FOR_EVENT park can legitimately wake many times during one long combat because of
             * maintenance or timeout rechecks, so that cycle must reset the immediate counter.
             *
             * CR230: a tripped loop guard is no longer a local failClosed. It is reported to the
             * cloud as a structured step fact so XIULUO_BRAIN can command RESTART_ROUND (and escalate
             * to FAIL_TASK via its consecutive-failure breaker).
             */
            /*
             * CR230 review fix: baseline loop guard is `> 32` (696a12b L569). The counter here uses
             * `>=`, so 33 trips on the 33rd immediate cycle — strictly equivalent. The old value 16
             * came from the CR207 failClosed hot-loop guard and fired twice as early.
             */
            boolean loopGuardTripped =
                    cloudRoundState.noteCommandCycleAndCheckExceeded(shellResult.realEventWaitCompleted(), 33);
            Map<String, String> stepFacts = xiuluoBrainLoopFacts(context, cloudRoundState.current().round(), "step");
            if (loopGuardTripped) {
                log.warn("xiuluo.brain.loop.guardTripped round={} phase={} actionId={}; requesting cloud round restart",
                        cloudRoundState.current().round(), command.getPhase(), command.getActionId());
                Map<String, String> merged = new LinkedHashMap<>(stepFacts);
                merged.put("loopGuardTripped", "true");
                stepFacts = Map.copyOf(merged);
            }

            decision = xiuluoBrainCloudDecisionService.step(
                    XiuluoBrainStepRequest.builder()
                            .taskCode(TASK_CODE)
                            .source("xiuluo-v2:cr195-loop-step")
                            .windowId(windowId)
                            .taskRunId(taskRunId)
                            .sessionId(command.getSessionId())
                            .stateSeq(command.getStateSeq())
                            .phaseToken(command.getPhaseToken())
                            .phase(command.getPhase())
                            .lastActionId(command.getActionId())
                            .context(stepFacts)
                            .build());
        }
    }

    private XiuluoBrainDecision restartXiuluoBrainAfterSessionReset(
            TaskExecutionContext context,
            int round,
            XiuluoBrainResponse oldCommand,
            XiuluoBrainShellResult shellResult,
            XiuluoBrainActionOutcomeDecision resetAck,
            Map<String, String> outcomeFacts) {
        XiuluoPhase resumePhase = shellResult.localOutcomeNextPhase();
        if (resumePhase == null || resumePhase.isTerminal()) {
            log.warn("xiuluo.brain.session.reset refused: round={} oldSessionId={} oldActionId={} resumePhase={} reason={}",
                    round,
                    oldCommand == null ? null : oldCommand.getSessionId(),
                    oldCommand == null ? null : oldCommand.getActionId(),
                    resumePhase,
                    resetAck.getResetReason());
            return null;
        }
        Map<String, String> resyncFacts =
                xiuluoBrainSessionResetFacts(context, round, oldCommand, shellResult, resetAck, outcomeFacts);
        log.warn("xiuluo.brain.session.reset resync start: round={} oldSessionId={} oldStateSeq={} "
                        + "oldPhaseToken={} oldActionId={} resumePhase={} reason={}",
                round,
                oldCommand == null ? null : oldCommand.getSessionId(),
                oldCommand == null ? 0L : oldCommand.getStateSeq(),
                oldCommand == null ? null : oldCommand.getPhaseToken(),
                oldCommand == null ? null : oldCommand.getActionId(),
                resumePhase,
                resetAck.getResetReason());
        return xiuluoBrainCloudDecisionService.start(
                XiuluoBrainStartRequest.builder()
                        .taskCode(TASK_CODE)
                        .source("xiuluo-v2:cr223-session-reset-resync")
                        .windowId(xiuluoBrainWindowId(context))
                        .taskRunId(xiuluoBrainTaskRunId(context))
                        .initialPhase(resumePhase)
                        .context(resyncFacts)
                        .build());
    }

    /**
     * Converts a recoverable local/cloud-brain failure into a fresh same-round accept chain. The
     * failure archive is deliberately written before cleanup so later analysis retains the exact
     * pre-cleanup state. User stop remains authoritative and is observed at the next checkpoint.
     */
    private XiuluoBrainDecision restartXiuluoBrainAfterFailure(
            TaskExecutionContext context,
            int round,
            XiuluoBrainRoundState cloudRoundState,
            XiuluoRoundTrace roundTrace,
            String reason) {
        XiuluoRoundContext failedContext = cloudRoundState == null || cloudRoundState.current() == null
                ? XiuluoRoundContext.start(round)
                : cloudRoundState.current();
        archiveRoundFailureCase(context, roundTrace, "cloud-brain-failure", failedContext, reason, null);
        if (context.isStopRequested()) {
            return null;
        }
        log.warn("xiuluo.brain.loop.failureRestart round={} phase={} reason={}",
                round, failedContext.phase(), reason);
        uiCleanerService.cleanUpAll();
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime != null) {
            runtime.clearDialogInterest("xiuluo cloud-brain failure restart");
            runtime.clearPreparedDialogAction("xiuluo cloud-brain failure restart");
            runtime.clearDialogPreparationRequest("xiuluo cloud-brain failure restart");
        }
        clearTrackerShortcutPathingIntent(runtime, "xiuluo-v2:cloud-brain-failure-restart");
        TaskSleep.sleepOrStop(context, TASK_TURN_HANDOFF_DELAY_MS, "Xiuluo V2 task interrupted");
        Map<String, String> restartFacts = new LinkedHashMap<>(
                collectXiuluoBrainHotStartFacts(context, round, "failure-restart"));
        restartFacts.put("failureRestart", "true");
        restartFacts.put("failureReason", safeCloudValue(reason));
        restartFacts.put("failedPhase", failedContext.phase().name());
        return xiuluoBrainCloudDecisionService.start(
                XiuluoBrainStartRequest.builder()
                        .taskCode(TASK_CODE)
                        .source("xiuluo-v2:failure-restart")
                        .windowId(xiuluoBrainWindowId(context))
                        .taskRunId(xiuluoBrainTaskRunId(context))
                        .initialPhase(XiuluoPhase.PREPARE_ROUND)
                        .context(Map.copyOf(restartFacts))
                        .build());
    }

    private XiuluoBrainShellResult executeXiuluoBrainCommandShell(
            TaskExecutionContext context,
            XiuluoBrainRoundState cloudRoundState,
            XiuluoBrainResponse command,
            XiuluoRoundTrace roundTrace) {
        if (command == null) {
            return XiuluoBrainShellResult.rejected("cloud command missing");
        }
        XiuluoBrainActionType actionType = command.getActionType();
        if (command.getPhase() == null) {
            return XiuluoBrainShellResult.rejected("cloud command phase missing");
        }
        if (actionType == XiuluoBrainActionType.COMPLETE_ROUND) {
            if (command.getPhase() != XiuluoPhase.ROUND_DONE) {
                return XiuluoBrainShellResult.rejected("COMPLETE_ROUND requires phase ROUND_DONE");
            }
            return XiuluoBrainShellResult.terminal(TaskRunResult.SUCCESS, "cloud completed round");
        }
        if (actionType == XiuluoBrainActionType.STOP_TASK) {
            if (command.getPhase() != XiuluoPhase.STOPPED) {
                return XiuluoBrainShellResult.rejected("STOP_TASK requires phase STOPPED");
            }
            return XiuluoBrainShellResult.terminal(TaskRunResult.STOPPED, "cloud stopped task");
        }
        if (actionType == XiuluoBrainActionType.FAIL_TASK) {
            if (command.getPhase() != XiuluoPhase.FAILED) {
                return XiuluoBrainShellResult.rejected("FAIL_TASK requires phase FAILED");
            }
            return XiuluoBrainShellResult.terminal(TaskRunResult.FAILED, "cloud failed task");
        }
        if (actionType == XiuluoBrainActionType.RESTART_ROUND) {
            if (command.getPhase() != XiuluoPhase.PREPARE_ROUND) {
                return XiuluoBrainShellResult.rejected("RESTART_ROUND requires phase PREPARE_ROUND");
            }
            return executeXiuluoBrainCommandedRoundRestart(context, cloudRoundState, command, roundTrace);
        }
        if (actionType == XiuluoBrainActionType.WAIT_FOR_EVENT) {
            // CR244: the pending-return Gate A/Gate B park uses the same stashed-wait-spec protocol
            // as WAIT_COMBAT, so WAIT_TEAM_RETURN is a legal WAIT_FOR_EVENT phase now.
            // CR245: the leader's maintenance broadcast-queue drain parks the maintenance-check
            // phases through the same protocol.
            // CR253: WAIT_TRACKER_SHORTCUT_PATHING joins the real event-park protocol — the
            // green-chain phase arms its wait spec and the cloud commands the park.
            if (command.getPhase() != XiuluoPhase.WAIT_COMBAT
                    && command.getPhase() != XiuluoPhase.WAIT_TEAM_RETURN
                    && command.getPhase() != XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK
                    && command.getPhase() != XiuluoPhase.BEFORE_ROUTE_MAINTENANCE_CHECK
                    && command.getPhase() != XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING) {
                return XiuluoBrainShellResult.rejected(
                        "WAIT_FOR_EVENT supports WAIT_COMBAT/WAIT_TEAM_RETURN/maintenance-check/"
                                + "WAIT_TRACKER_SHORTCUT_PATHING phases only: " + command.getPhase());
            }
            return waitForXiuluoBrainEvent(context, cloudRoundState, command);
        }
        if (actionType == XiuluoBrainActionType.RUN_CLEANUP) {
            if (command.getPhase().isTerminal()) {
                return XiuluoBrainShellResult.rejected("RUN_CLEANUP target phase must be non-terminal");
            }
            if (!isXiuluoBrainExecutablePreCombatPhase(command.getPhase())) {
                return XiuluoBrainShellResult.rejected(
                        "RUN_CLEANUP target phase must be executable: " + command.getPhase());
            }
            XiuluoBrainShellResult cleanupResult = runXiuluoBrainCommandedCleanup(cloudRoundState, command);
            if (!cleanupResult.accepted()) {
                return cleanupResult;
            }
        } else if (actionType == XiuluoBrainActionType.EXECUTE_PHASE) {
            if (command.getPhase().isTerminal()) {
                return XiuluoBrainShellResult.rejected("EXECUTE_PHASE target phase must be non-terminal");
            }
            if (!isXiuluoBrainExecutablePreCombatPhase(command.getPhase())) {
                return XiuluoBrainShellResult.rejected(
                        "EXECUTE_PHASE target phase must be executable: " + command.getPhase());
            }
        } else {
            return XiuluoBrainShellResult.rejected("unsupported cloud command actionType=" + actionType);
        }

        XiuluoRoundContext cloudPhaseState = cloudRoundState.executionStateFor(command.getPhase(), command.getActionId());
        boolean cloudPhaseAppliedExactly = cloudPhaseState.phase() == command.getPhase();
        if (!cloudPhaseAppliedExactly) {
            return XiuluoBrainShellResult.rejected("cloud command phase did not apply exactly: requested="
                    + command.getPhase() + " actual=" + cloudPhaseState.phase());
        }
        Map<String, String> commandExtraOutcomeFacts = new LinkedHashMap<>();
        XiuluoBrainShellResult startupReturnGate = guardCloudAcceptNavigationWithStartupReturn(
                context, command, cloudRoundState, cloudPhaseState, commandExtraOutcomeFacts);
        if (startupReturnGate != null) {
            return startupReturnGate;
        }
        if (isXiuluoBrainExecutablePreCombatPhase(command.getPhase())) {
            int localYieldGuard = 0;
            while (localYieldGuard++ < 8) {
                AtomicReference<XiuluoStepOutcome> phaseOutcome = new AtomicReference<>();
                XiuluoRoundContext executionState = cloudPhaseState;
                /*
                 * CR234 watchdog compensation: time spent waiting for the task turn before this
                 * phase actually runs is not "pre-combat progress". Without shifting
                 * preCombatStartedAtMs by the blocked time, a long multi-window turn wait makes
                 * the 180s watchdog budget go negative before the phase even starts, and every
                 * accept-tracker/objective wait then times out instantly (2026-07-09 round 19:
                 * remainingMs=-200936 on entry). The user-pause half of the same bug is
                 * compensated at the loop-top checkpoint.
                 */
                long turnWaitStartedAtMs = System.currentTimeMillis();
                TaskTransactionOutcome transaction = taskTransactionRunner.runDynamic(
                        "xiuluo-v2:xiuluo-brain:" + command.getPhase(),
                        TaskTransactionResult.READY_TO_CONTINUE,
                        TaskYieldPolicy.CONTINUE_CHAIN,
                        () -> {
                            long turnWaitMs = System.currentTimeMillis() - turnWaitStartedAtMs;
                            XiuluoStepOutcome outcome = runPhase(context,
                                    executionState.pausePreCombatTimer(turnWaitMs,
                                            "turn-wait-compensation:" + command.getPhase()));
                            outcome = compensateMaintenanceHandoffDelay(outcome);
                            log.info("xiuluo.brain.loop.phaseOutcome phase={} result={} yield={} localNext={} message={}",
                                    command.getPhase(), outcome.transactionResult(), outcome.yieldPolicy(),
                                    outcome.nextState().phase(), outcome.message());
                            phaseOutcome.set(outcome);
                            return TaskTransactionRunner.TaskTransactionDecision.of(
                                    outcome.transactionResult(),
                                    outcome.yieldPolicy());
                        });
                XiuluoStepOutcome outcome = phaseOutcome.get();
                if (outcome == null) {
                    outcome = XiuluoStepOutcome.failed(executionState, "cloud brain phase produced no outcome");
                }
                if (transaction.result() == TaskTransactionResult.STOPPED
                        || outcome.transactionResult() == TaskTransactionResult.STOPPED) {
                    return XiuluoBrainShellResult.terminalStopped("cloud brain phase stopped", outcome)
                            .withExtraOutcomeFacts(commandExtraOutcomeFacts);
                }
                if (XiuluoBrainRoundState.mustReportBeforeLocalYield(command.getPhase(), outcome)) {
                    return XiuluoBrainShellResult.accepted(
                            "cloud brain phase shared-state fact reported before local yield: " + command.getPhase(),
                            outcome).withExtraOutcomeFacts(commandExtraOutcomeFacts);
                }
                // CR245: a maintenance-check phase that armed the broadcast-queue wait spec is a
                // legal same-phase park; report it so the cloud commands a real WAIT_FOR_EVENT
                // instead of burning the local yield guard on inline re-execution.
                boolean maintenanceQueueParkRequested = outcome.waitSpec() != null
                        && outcome.waitSpec().getReason() == XiuluoWaitReason.WAIT_MAINTENANCE_BROADCAST_QUEUE
                        && (command.getPhase() == XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK
                                || command.getPhase() == XiuluoPhase.BEFORE_ROUTE_MAINTENANCE_CHECK);
                // CR253: a same-phase armed green-chain park must reach the cloud instead of being
                // consumed by inline yields — the cloud answers with a real WAIT_FOR_EVENT, so the
                // local yield guard never burns on a legal infinite park.
                boolean trackerShortcutParkRequested = outcome.waitSpec() != null
                        && outcome.waitSpec().getReason() == XiuluoWaitReason.WAIT_TRACKER_SHORTCUT_PATHING
                        && command.getPhase() == XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING;
                if (outcome.waitSpec() != null
                        && (command.getPhase() == XiuluoPhase.WAIT_COMBAT
                                || command.getPhase() == XiuluoPhase.WAIT_TEAM_RETURN
                                || maintenanceQueueParkRequested
                                || trackerShortcutParkRequested)
                        && outcome.nextState() != null
                        && outcome.nextState().phase() == command.getPhase()) {
                    return XiuluoBrainShellResult.accepted(
                            "cloud brain phase requested event wait: " + command.getPhase(),
                            outcome).withExtraOutcomeFacts(commandExtraOutcomeFacts);
                }
                if (XiuluoBrainRoundState.mayRequestCloudStepAfter(outcome)) {
                    return XiuluoBrainShellResult.accepted(
                            "cloud brain phase executed: " + command.getPhase(), outcome)
                            .withExtraOutcomeFacts(commandExtraOutcomeFacts);
                }
                XiuluoStepOutcome yieldedOutcome = outcome;
                if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD
                        || outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED
                        || outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
                    XiuluoEventParkResult inlinePark = yieldAfterMustYield(context, outcome);
                    yieldedOutcome = inlinePark.outcome();
                    /*
                     * CR256: a green-chain park that just woke may already hold the authorized
                     * local kanda prepared action (its template evidence + click point). Consume it
                     * NOW on this task turn — safety validation plus one atomic InputSequences
                     * click — instead of first spending an action-outcome/step round-trip to have
                     * the cloud re-enter the phase. The structured click outcome is what gets
                     * reported; the cloud still owns the next phase decision.
                     */
                    if (inlinePark.realParkCompleted()) {
                        XiuluoStepOutcome directConsume = maybeDirectConsumePreparedEnterBattleAfterWake(
                                context, yieldedOutcome);
                        if (directConsume != null) {
                            yieldedOutcome = directConsume;
                        }
                    }
                }
                if (yieldedOutcome.nextState() == null
                        || yieldedOutcome.nextState().phase() != command.getPhase()) {
                    return XiuluoBrainShellResult.accepted(
                            "cloud brain phase yielded before next command: " + command.getPhase(),
                            yieldedOutcome).withExtraOutcomeFacts(commandExtraOutcomeFacts);
                }
                cloudPhaseState = yieldedOutcome.nextState();
            }
            return XiuluoBrainShellResult.rejected(
                    "cloud brain local yield guard exceeded phase=" + command.getPhase());
        }
        return XiuluoBrainShellResult.rejected(
                "unsupported cloud executable phase; no local fallback phase=" + cloudPhaseState.phase());
    }

    private XiuluoBrainShellResult guardCloudAcceptNavigationWithStartupReturn(
            TaskExecutionContext context,
            XiuluoBrainResponse command,
            XiuluoBrainRoundState cloudRoundState,
            XiuluoRoundContext cloudPhaseState,
            Map<String, String> commandExtraOutcomeFacts) {
        if (command == null || command.getPhase() != XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC
                || cloudPhaseState == null) {
            return null;
        }
        if (cloudPhaseState.round() > 1) {
            /*
             * CR235 baseline alignment: the CR220 gate exists for HOT-START rounds where the
             * player may resume away from the start map. Later rounds begin after ROUND_DONE with
             * a verified return, and the unconditional syncMyPosition() here cost ~1s per round
             * (2026-07-09 02:02:42.279 -> 02:02:43.238). Baseline had no per-round position probe
             * before accept navigation; a round restart also matches baseline by walking back via
             * the normal accept navigation instead of the return item.
             */
            log.info("[xiuluo-v2 CR220] cloud accept-navigation return gate skipped: round={} reason=non-hot-start-round",
                    cloudPhaseState.round());
            return null;
        }
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 cloud brain interrupted");
        LocationInfo beforeReturn = playerStateService.syncMyPosition();
        boolean startMapConfirmed = beforeReturn != null
                && gameStateUtil.isSameMapName(beforeReturn.mapName, START_MAP_NAME);
        if (startMapConfirmed) {
            log.info("[xiuluo-v2 CR220] cloud accept-navigation return gate pass: window={} currentMap={} current=({}, {}) phase={} reason=already-on-start-map",
                    currentWindowLabel(), beforeReturn.mapName, beforeReturn.x, beforeReturn.y, command.getPhase());
            recordStartMapVerifiedLocation(beforeReturn, "cr220-already-on-start-map");
            return null;
        }

        if (cloudRoundState != null && cloudRoundState.startupReturnItemTriedAndUnverified()) {
            Map<String, String> skippedFacts = cr220ReturnGateFacts(ReturnHomeResult.UNAVAILABLE,
                    beforeReturn, beforeReturn,
                    XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "cr220-hot-start-return-already-tried",
                    "hot-start-return-item-already-tried-unverified");
            if (commandExtraOutcomeFacts != null) {
                commandExtraOutcomeFacts.putAll(skippedFacts);
            }
            log.warn("[xiuluo-v2 CR220] cloud accept-navigation return gate skip repeated hot-start return item: window={} currentMap={} current=({}, {}) phase={} actionId={} targetStartMap={} returnTemplate={} reason=hot-start-return-item-already-tried-unverified",
                    currentWindowLabel(),
                    beforeReturn == null ? null : beforeReturn.mapName,
                    beforeReturn == null ? null : beforeReturn.x,
                    beforeReturn == null ? null : beforeReturn.y,
                    command.getPhase(), command.getActionId(), START_MAP_NAME, RETURN_ITEM_TEMPLATE);
            return null;
        }

        log.warn("[xiuluo-v2 CR220] cloud accept-navigation return gate try return before accept NPC: window={} currentMap={} current=({}, {}) phase={} actionId={} reason=not-on-start-map-or-location-unknown targetStartMap={} returnTemplate={}",
                currentWindowLabel(),
                beforeReturn == null ? null : beforeReturn.mapName,
                beforeReturn == null ? null : beforeReturn.x,
                beforeReturn == null ? null : beforeReturn.y,
                command.getPhase(), command.getActionId(), START_MAP_NAME, RETURN_ITEM_TEMPLATE);
        ReturnHomeResult returnHome = useReturnItemAndVerifyStartMap(
                context, cloudPhaseState.round(), "cr220-cloud-accept-nav-gate");
        LocationInfo afterReturn = playerStateService.syncMyPosition();
        if (returnHome == ReturnHomeResult.VERIFIED) {
            log.info("[xiuluo-v2 CR220] cloud accept-navigation return gate verified: window={} before={} after={} returnResult={} returnTemplate={} nextPhase={} nextSource={} reason=return-item-before-accept-navigation",
                    currentWindowLabel(), beforeReturn, afterReturn, returnHome, RETURN_ITEM_TEMPLATE,
                    XiuluoPhase.WAIT_TEAM_RETURN, TEAM_RETURN_BEFORE_ACCEPT_SOURCE);
            return XiuluoBrainShellResult.accepted(
                    "CR220 return item verified before accept navigation",
                    XiuluoStepOutcome.continueTo(
                            cloudPhaseState.next(XiuluoPhase.WAIT_TEAM_RETURN, TEAM_RETURN_BEFORE_ACCEPT_SOURCE),
                            "return item verified before accept navigation"),
                    cr220ReturnGateFacts(returnHome, beforeReturn, afterReturn,
                            XiuluoPhase.WAIT_TEAM_RETURN, TEAM_RETURN_BEFORE_ACCEPT_SOURCE,
                            "return-item-before-accept-navigation"));
        }
        if (returnHome == ReturnHomeResult.STILL_IN_COMBAT) {
            log.warn("[xiuluo-v2 CR220] cloud accept-navigation return gate found trusted combat: window={} before={} after={} returnResult={} returnTemplate={} nextPhase={} reason=return-attempt-still-in-combat",
                    currentWindowLabel(), beforeReturn, afterReturn, returnHome, RETURN_ITEM_TEMPLATE,
                    XiuluoPhase.WAIT_COMBAT);
            return XiuluoBrainShellResult.accepted(
                    "CR220 return attempt found trusted combat before accept navigation",
                    XiuluoStepOutcome.sharedState(
                            cloudPhaseState.next(XiuluoPhase.WAIT_COMBAT, "cr220-return-still-in-combat"),
                            "return attempt found trusted combat before accept navigation"),
                    cr220ReturnGateFacts(returnHome, beforeReturn, afterReturn,
                            XiuluoPhase.WAIT_COMBAT, "cr220-return-still-in-combat",
                            "return-attempt-still-in-combat"));
        }
        if (returnHome == ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT) {
            if (cloudRoundState != null) {
                cloudRoundState.markStartupReturnItemTriedAndUnverified();
            }
            log.error("[xiuluo-v2 CR220] cloud accept-navigation return gate blocked: window={} before={} after={} returnResult={} returnTemplate={} reason=return-item-used-but-start-map-unverified",
                    currentWindowLabel(), beforeReturn, afterReturn, returnHome, RETURN_ITEM_TEMPLATE);
            return XiuluoBrainShellResult.accepted(
                    "CR220 return item used but start map not verified before accept navigation",
                    XiuluoStepOutcome.failed(cloudPhaseState,
                            "return item used but start map not verified before accept navigation"),
                    cr220ReturnGateFacts(returnHome, beforeReturn, afterReturn,
                            XiuluoPhase.FAILED, "failed",
                            "return-item-used-but-start-map-unverified"));
        }

        Map<String, String> unavailableFacts = cr220ReturnGateFacts(returnHome, beforeReturn, afterReturn,
                XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "cr220-return-unavailable-before-accept",
                "return-item-unavailable-or-not-used");
        if (commandExtraOutcomeFacts != null) {
            commandExtraOutcomeFacts.putAll(unavailableFacts);
        }
        log.warn("[xiuluo-v2 CR220] cloud accept-navigation return gate allow navigation with explicit unavailable reason: window={} before={} after={} returnResult={} returnTemplate={} phase={} actionId={} reason=return-item-unavailable-or-not-used",
                currentWindowLabel(), beforeReturn, afterReturn, returnHome, RETURN_ITEM_TEMPLATE,
                command.getPhase(), command.getActionId());
        return null;
    }

    private Map<String, String> cr220ReturnGateFacts(ReturnHomeResult returnHome,
                                                     LocationInfo beforeReturn,
                                                     LocationInfo afterReturn,
                                                     XiuluoPhase nextPhase,
                                                     String nextSource,
                                                     String reason) {
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("returnGate", "CR220_ACCEPT_NAVIGATION");
        facts.put("returnHomeResult", returnHome == null ? "" : returnHome.name());
        facts.put("returnTemplate", RETURN_ITEM_TEMPLATE);
        facts.put("returnGateReason", safeCloudValue(reason));
        facts.put("returnGateBeforeMap", safeCloudValue(beforeReturn == null ? null : beforeReturn.mapName));
        facts.put("returnGateBeforeX", beforeReturn == null ? "" : Integer.toString(beforeReturn.x));
        facts.put("returnGateBeforeY", beforeReturn == null ? "" : Integer.toString(beforeReturn.y));
        facts.put("returnGateAfterMap", safeCloudValue(afterReturn == null ? null : afterReturn.mapName));
        facts.put("returnGateAfterX", afterReturn == null ? "" : Integer.toString(afterReturn.x));
        facts.put("returnGateAfterY", afterReturn == null ? "" : Integer.toString(afterReturn.y));
        facts.put("returnGateNextPhase", nextPhase == null ? "" : nextPhase.name());
        facts.put("returnGateNextSource", safeCloudValue(nextSource));
        return facts;
    }

    private XiuluoBrainShellResult waitForXiuluoBrainEvent(
            TaskExecutionContext context,
            XiuluoBrainRoundState cloudRoundState,
            XiuluoBrainResponse command) {
        XiuluoWaitSpec waitSpec = cloudRoundState.consumePendingWaitSpec();
        if (waitSpec == null) {
            return XiuluoBrainShellResult.rejected("WAIT_FOR_EVENT requires pending wait spec");
        }
        XiuluoRoundContext waitState = cloudRoundState.current();
        XiuluoStepOutcome waitOutcome = XiuluoStepOutcome.sharedState(
                        waitState,
                "cloud requested " + command.getPhase() + " event wait")
                .withWaitSpec(waitSpec);
        XiuluoEventParkResult parked = yieldAfterMustYield(context, waitOutcome);
        if (!parked.realParkCompleted()) {
            String noParkReason = parked.outcome() == null || parked.outcome().message() == null
                    ? waitSpec.getReason().name()
                    : parked.outcome().message();
            return XiuluoBrainShellResult.rejected(
                    "WAIT_FOR_EVENT did not perform a real park: " + noParkReason);
        }
        XiuluoStepOutcome parkedOutcome = parked.outcome();
        if (parkedOutcome == null) {
            return XiuluoBrainShellResult.rejected("WAIT_FOR_EVENT completed without parked outcome");
        }
        if (parkedOutcome.transactionResult() == TaskTransactionResult.FAILED
                || parkedOutcome.transactionResult() == TaskTransactionResult.STOPPED
                || parkedOutcome.nextState() != null
                && (parkedOutcome.nextState().phase() == XiuluoPhase.FAILED
                || parkedOutcome.nextState().phase() == XiuluoPhase.STOPPED)) {
            return XiuluoBrainShellResult.acceptedRealEventWait(
                    "cloud WAIT_FOR_EVENT preserved local terminal outcome: " + parkedOutcome.message(),
                    parkedOutcome);
        }
        /*
         * CR256: when the green-chain WAIT_FOR_EVENT park was woken by the local kanda prepared
         * action, that action already carries the authorization (attempt identity + fresh template
         * evidence + click point). Consume it on this same task turn and report the CLICK outcome
         * as the wait completion — the previous "report READY, ask for step, re-enter phase, then
         * click" chain spent two cloud round-trips before the physical click. The cloud remains
         * the only brain: it receives the structured outcome and decides WAIT_COMBAT/fallback/
         * recovery as before. A miss (job wake, stale/unstamped action, combat change) falls
         * through to the unchanged completed path.
         */
        if (command.getPhase() == XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING) {
            XiuluoStepOutcome directConsume = maybeDirectConsumePreparedEnterBattleAfterWake(
                    context, parkedOutcome);
            if (directConsume != null) {
                return XiuluoBrainShellResult.acceptedRealEventWait(
                        "cloud WAIT_FOR_EVENT completed with direct prepared enter-battle consume: "
                                + command.getPhase(), directConsume);
            }
        }
        XiuluoStepOutcome completed = new XiuluoStepOutcome(
                parkedOutcome.nextState(),
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                "cloud requested " + command.getPhase() + " event wait finished",
                null);
        return XiuluoBrainShellResult.acceptedRealEventWait(
                "cloud WAIT_FOR_EVENT completed: " + command.getPhase(), completed);
    }

    private XiuluoBrainShellResult runXiuluoBrainCommandedCleanup(
            XiuluoBrainRoundState cloudRoundState,
            XiuluoBrainResponse command) {
        String cleanupType = safeCloudValue(command.getCleanupType());
        if (cleanupType.isBlank()) {
            return XiuluoBrainShellResult.rejected("cleanupType is required for RUN_CLEANUP");
        }
        log.info("xiuluo.brain.command.cleanup cleanupType={} retryKey={} attempt={}/{} cloudNextPhase={} actionId={}",
                cleanupType, safeCloudValue(command.getRetryKey()), command.getAttempt(), command.getMaxAttempts(),
                command.getPhase(), command.getActionId());
        switch (cleanupType) {
            case "GENERIC_UI" -> uiCleanerService.cleanUpAll();
            case "GENERIC_WINDOWS" -> uiCleanerService.closeAllGenericWindows();
            // CR230: baseline L2617/L2756 cleanup + mount toggle before retrying an NPC click phase.
            case "GENERIC_UI_TOGGLE_MOUNT" -> {
                uiCleanerService.cleanUpAll();
                toggleMountBeforeClickRetry(cloudRoundState.current(), "cloud cleanup " + command.getRetryKey());
            }
            default -> {
                return XiuluoBrainShellResult.rejected("unknown cleanupType=" + cleanupType);
            }
        }
        return XiuluoBrainShellResult.cleanupAccepted("cloud cleanup executed: " + cleanupType);
    }

    /**
     * CR230: execute a cloud-commanded round restart. This is the client half of the first-class
     * `RESTART_ROUND` command that replaces the baseline `restartRoundAfterPhaseFailure` fallback:
     * archive the failure evidence, clean up, yield the task turn, then rebuild the round context
     * with the SAME round number and report `EXECUTED` so the cloud can issue a fresh
     * `PREPARE_ROUND` command. Stop always wins over restart.
     */
    private XiuluoBrainShellResult executeXiuluoBrainCommandedRoundRestart(
            TaskExecutionContext context,
            XiuluoBrainRoundState cloudRoundState,
            XiuluoBrainResponse command,
            XiuluoRoundTrace roundTrace) {
        if (context.isStopRequested()) {
            return XiuluoBrainShellResult.terminal(TaskRunResult.STOPPED,
                    "stop requested before cloud commanded round restart");
        }
        int round = cloudRoundState.current().round();
        XiuluoPhase failedPhase = cloudRoundState.current().phase();
        log.warn("xiuluo.brain.loop.restartRound round={} failedPhase={} actionId={} reason={}",
                round, failedPhase, command.getActionId(), command.getReason());
        if (roundTrace != null) {
            roundTrace.addCloudFailure(cloudRoundState.current(), "cloud restart: " + command.getReason());
            archiveRoundFailureCase(context, roundTrace, "cloud-restart-round", cloudRoundState.current(),
                    command.getReason(), null);
        }
        uiCleanerService.cleanUpAll();
        // CR232: a fresh round must not inherit dialog interest / prepared actions / requests.
        WindowRuntimeContext restartRuntime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (restartRuntime != null) {
            restartRuntime.clearDialogInterest("cloud commanded round restart");
            restartRuntime.clearPreparedDialogAction("cloud commanded round restart");
            restartRuntime.clearDialogPreparationRequest("cloud commanded round restart");
        }
        // A restart begins a fresh command cycle; the hot-loop guard must not carry over.
        cloudRoundState.noteRealEventWaitCompleted();
        XiuluoStepOutcome restartOutcome = XiuluoStepOutcome.continueTo(
                XiuluoRoundContext.start(round),
                "cloud commanded round restart: " + safeCloudValue(command.getReason()));
        XiuluoEventParkResult handoff = yieldAfterMustYield(context, restartOutcome);
        return XiuluoBrainShellResult.accepted("cloud commanded round restart executed", handoff.outcome());
    }

    private boolean isXiuluoBrainExecutablePreCombatPhase(XiuluoPhase phase) {
        return switch (phase) {
            case PREPARE_ROUND,
                    ACCEPT_TASK_NAVIGATE_TO_NPC,
                    ACCEPT_TASK_CLICK_NPC,
                    ACCEPT_TASK_DIALOG,
                    READ_OBJECTIVE,
                    AFTER_ACCEPT_MAINTENANCE_CHECK,
                    BEFORE_ROUTE_MAINTENANCE_CHECK,
                    TRY_TRACKER_SHORTCUT,
                    WAIT_TRACKER_SHORTCUT_PATHING,
                    NAVIGATE_TO_TARGET,
                    CLICK_TARGET_NPC,
                    CONFIRM_ENTER_BATTLE,
                    WAIT_COMBAT,
                    RETURN_HOME,
                    NAVIGATE_BACK_TO_START,
                    WAIT_TEAM_READY,
                    WAIT_TEAM_RETURN -> true;
            default -> false;
        };
    }

    private TaskRunResult failClosedXiuluoBrainLoop(
            int round,
            XiuluoBrainResponse command,
            XiuluoBrainDecision decision,
            String reason) {
        log.error("xiuluo.brain.loop.failClosed round={} status={} phase={} actionType={} actionId={} "
                        + "sessionId={} stateSeq={} reason={}",
                round,
                decision == null ? null : decision.getStatus(),
                command == null ? null : command.getPhase(),
                command == null ? null : command.getActionType(),
                command == null ? null : command.getActionId(),
                command == null ? null : command.getSessionId(),
                command == null ? 0L : command.getStateSeq(),
                reason);
        return TaskRunResult.FAILED;
    }

    private Map<String, String> xiuluoBrainLoopFacts(TaskExecutionContext context, int round, String stage) {
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("stage", safeCloudValue(stage));
        facts.put("round", Integer.toString(round));
        facts.put("windowId", xiuluoBrainWindowId(context));
        facts.put("taskRunId", xiuluoBrainTaskRunId(context));
        facts.put("windowTitle", safeCloudValue(context == null ? null : context.getNativeWindowTitle()));
        facts.put("cloudLoop", "cr195-scaffold");
        return Map.copyOf(facts);
    }

    private Map<String, String> xiuluoBrainOutcomeFacts(
            TaskExecutionContext context,
            int round,
            XiuluoBrainResponse command,
            XiuluoStepOutcome outcome) {
        Map<String, String> facts = new LinkedHashMap<>(xiuluoBrainLoopFacts(context, round, "action-outcome"));
        facts.put("cloudLoop", "cr198-accept-tracker");
        facts.put("commandPhase", command == null || command.getPhase() == null ? "" : command.getPhase().name());
        facts.put("commandActionType", command == null || command.getActionType() == null
                ? ""
                : command.getActionType().name());
        facts.put("commandActionId", safeCloudValue(command == null ? null : command.getActionId()));
        facts.put("cleanupType", safeCloudValue(command == null ? null : command.getCleanupType()));
        facts.put("retryKey", safeCloudValue(command == null ? null : command.getRetryKey()));
        facts.put("attempt", command == null ? "" : Integer.toString(command.getAttempt()));
        facts.put("maxAttempts", command == null ? "" : Integer.toString(command.getMaxAttempts()));
        facts.put("maxPhaseRetry", Integer.toString(MAX_PHASE_RETRY));
        facts.put("maxEnterBattleConfirmRetries", Integer.toString(MAX_ENTER_BATTLE_CONFIRM_RETRIES));
        facts.put("maxRecoveryCount", Integer.toString(MAX_RECOVERY_COUNT));
        facts.put("outcomeResult", outcome == null || outcome.transactionResult() == null
                ? ""
                : outcome.transactionResult().name());
        facts.put("outcomeYield", outcome == null || outcome.yieldPolicy() == null
                ? ""
                : outcome.yieldPolicy().name());
        facts.put("outcomeMessage", safeCloudValue(outcome == null ? null : outcome.message()));
        String returnHomeResult = xiuluoBrainReturnHomeResultFact(command, outcome);
        if (!returnHomeResult.isBlank()) {
            facts.put("returnHomeResult", returnHomeResult);
        }
        facts.put("localOutcomeNextPhase", outcome == null
                || outcome.nextState() == null
                || outcome.nextState().phase() == null
                ? ""
                : outcome.nextState().phase().name());
        if (outcome != null && outcome.nextState() != null) {
            XiuluoRoundContext next = outcome.nextState();
            facts.put("localOutcomeSource", safeCloudValue(next.source()));
            facts.put("phaseRetryCount", Integer.toString(next.phaseRetryCount()));
            facts.put("enterBattleConfirmRetryCount", Integer.toString(next.enterBattleConfirmRetryCount()));
            facts.put("shortcutTrackerRetryCount", Integer.toString(next.shortcutTrackerRetryCount()));
            facts.put("recoveryCount", Integer.toString(next.recoveryCount()));
            facts.put("startExitPrepathStarted", Boolean.toString(next.startExitPrepathStarted()));
            facts.put("routeMode", next.routeMode() == null ? "" : next.routeMode().name());
            facts.put("shortcutTrackerDetailPath", safeCloudValue(next.shortcutTrackerDetailPath()));
            facts.put("shortcutTrackerClickX", next.shortcutTrackerClickX() == null
                    ? ""
                    : Integer.toString(next.shortcutTrackerClickX()));
            facts.put("shortcutTrackerClickY", next.shortcutTrackerClickY() == null
                    ? ""
                    : Integer.toString(next.shortcutTrackerClickY()));
            facts.put("shortcutPathingIntentId", safeCloudValue(next.shortcutPathingIntentId()));
            facts.put("firstTrackerGreenClickAtMs", Long.toString(next.firstTrackerGreenClickAtMs()));
            facts.put("enteredBattleByXiuluo", Boolean.toString(next.enteredBattleByXiuluo()));
            facts.put("combatSource", next.combatSource() == null ? "" : next.combatSource().name());
            if (next.objective() != null) {
                facts.put("objectiveName", safeCloudValue(next.objective().getName()));
                facts.put("objectiveMap", safeCloudValue(next.objective().getMapName()));
                facts.put("objectiveX", Integer.toString(next.objective().getX()));
                facts.put("objectiveY", Integer.toString(next.objective().getY()));
            }
        }
        // CR230: structured facts attached by the phase executor (watchdogTimeout, pathingTerminal,
        // preparedEnterBattleFailed, ...) win over derived fields so the cloud never needs to match
        // human log messages.
        if (outcome != null && outcome.facts() != null && !outcome.facts().isEmpty()) {
            outcome.facts().forEach((key, value) -> facts.put(key, safeCloudValue(value)));
        }
        // CR207: whether THIS outcome armed a consumable local wait spec is derived from the
        // outcome itself and must never be shadowed by a stale executor fact; the cloud may only
        // command WAIT_FOR_EVENT when this is true.
        facts.put("eventWaitArmed", Boolean.toString(outcome != null && outcome.waitSpec() != null));
        return Map.copyOf(facts);
    }

    private Map<String, String> xiuluoBrainSessionResetFacts(
            TaskExecutionContext context,
            int round,
            XiuluoBrainResponse oldCommand,
            XiuluoBrainShellResult shellResult,
            XiuluoBrainActionOutcomeDecision resetAck,
            Map<String, String> outcomeFacts) {
        Map<String, String> facts = new LinkedHashMap<>(xiuluoBrainLoopFacts(context, round, "session-reset-resync"));
        if (outcomeFacts != null) {
            facts.putAll(outcomeFacts);
        }
        facts.put("cloudLoop", "cr223-session-reset-resync");
        facts.put("resetProtocol", "RESET_REQUIRED");
        facts.put("resetReason", safeCloudValue(resetAck == null ? null : resetAck.getResetReason()));
        facts.put("oldSessionId", safeCloudValue(oldCommand == null ? null : oldCommand.getSessionId()));
        facts.put("oldStateSeq", oldCommand == null ? "" : Long.toString(oldCommand.getStateSeq()));
        facts.put("oldPhaseToken", safeCloudValue(oldCommand == null ? null : oldCommand.getPhaseToken()));
        facts.put("oldActionId", safeCloudValue(oldCommand == null ? null : oldCommand.getActionId()));
        facts.put("oldCommandPhase", oldCommand == null || oldCommand.getPhase() == null
                ? ""
                : oldCommand.getPhase().name());
        facts.put("resyncInitialPhase", shellResult == null || shellResult.localOutcomeNextPhase() == null
                ? ""
                : shellResult.localOutcomeNextPhase().name());
        /*
         * CR223: a reset/resync after a lost cloud session must not re-run startup hot-start
         * detection and jump to tracker shortcut. The just-finished outcome determines the factual
         * resume phase sent back to the single cloud brain.
         */
        facts.put("hotStartFacts", "false");
        facts.put("trackerFound", "not_collected_session_reset");
        facts.put("trackerGreenLinkCount", "0");
        facts.put("trackerFactSource", "not_collected_session_reset");
        facts.put("trackerFactReason", "session_reset_resync_uses_outcome_resume_phase");
        return Map.copyOf(facts);
    }

    private String xiuluoBrainReturnHomeResultFact(XiuluoBrainResponse command, XiuluoStepOutcome outcome) {
        if (command == null || command.getPhase() != XiuluoPhase.RETURN_HOME || outcome == null) {
            return "";
        }
        XiuluoPhase nextPhase = outcome.nextState() == null ? null : outcome.nextState().phase();
        if (outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED
                && nextPhase == XiuluoPhase.WAIT_COMBAT) {
            return "STILL_IN_COMBAT";
        }
        String message = safeCloudValue(outcome.message()).toLowerCase(Locale.ROOT);
        if (message.contains("trusted combat is not active")
                || message.contains("trusted non-combat")
                || message.contains("trusted-not-in-combat")) {
            return "FAILED_AFTER_TRUSTED_NOT_IN_COMBAT";
        }
        if (message.contains("not found or not used") || message.contains("unavailable")) {
            return "UNAVAILABLE";
        }
        return "";
    }

    private Map<String, String> collectXiuluoBrainHotStartFacts(TaskExecutionContext context, int round, String stage) {
        Map<String, String> facts = new LinkedHashMap<>(xiuluoBrainLoopFacts(context, round, stage));
        facts.put("cloudLoop", "cr197-hot-start");
        facts.put("hotStartFacts", "true");
        facts.put("actionState", safeCloudValue(gameContext.getCurrentActionState() == null
                ? null
                : gameContext.getCurrentActionState().name()));
        facts.put("combatObserved", Boolean.toString(gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT));

        PlayerCharacter cachedPlayer = gameContext.getMe();
        facts.put("currentMap", safeCloudValue(cachedPlayer == null ? null : cachedPlayer.getCurrentMapName()));
        facts.put("currentX", cachedPlayer == null ? "" : Integer.toString(cachedPlayer.getX()));
        facts.put("currentY", cachedPlayer == null ? "" : Integer.toString(cachedPlayer.getY()));
        facts.put("currentLocationFactReason", cachedPlayer == null
                ? "no_input_reader_unavailable"
                : "cached_game_context");

        /*
         * CR235 baseline alignment: the visual tracker probe is a TRUE hot-start helper. Baseline
         * (696a12b L362/L996) resumed from tracker/return-item evidence only on the first round of
         * a task run; every later round starts fresh at PREPARE_ROUND after ROUND_DONE at the
         * start map, and re-scanning the tracker there added ~1.9s of WASH_YELLOW/classify to the
         * ROUND_DONE -> next-round gap (2026-07-09 02:02:40 -> 02:02:42 evidence).
         */
        if (round > 1) {
            facts.put("trackerFound", "not_collected_no_input_safe_source");
            facts.put("trackerGreenLinkCount", "0");
            facts.put("trackerFactSource", "not_collected_no_input_safe_source");
            facts.put("trackerFactReason", "non_first_round_skips_hot_start_probe");
            return Map.copyOf(facts);
        }
        /*
         * This startup probe is allowed to read only a bound-HWND snapshot. Do not use
         * GameClientTracker.captureToMemory/captureToFile here: those methods can fall back to
         * foreground/Robot capture when HWND capture fails, which would break the CR197 no-input
         * boundary before the first XIULUO_BRAIN command.
         */
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = runtime == null ? null : runtime.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            facts.put("trackerFound", "not_collected_no_input_safe_source");
            facts.put("trackerGreenLinkCount", "0");
            facts.put("trackerFactSource", "not_collected_no_input_safe_source");
            facts.put("trackerFactReason", binding == null || !binding.hasNativeHandle()
                    ? "current_window_native_binding_missing"
                    : "current_window_geometry_missing");
        } else {
            try {
                Optional<BoundWindowCaptureService.CaptureResult> snapshot =
                        boundWindowCaptureService.captureWindow(binding);
                if (snapshot.isEmpty()) {
                    facts.put("trackerFound", "not_collected_no_input_safe_source");
                    facts.put("trackerGreenLinkCount", "0");
                    facts.put("trackerFactSource", "not_collected_no_input_safe_source");
                    facts.put("trackerFactReason", "hwnd_capture_unavailable");
                } else {
                    Path snapshotPath = Path.of(windowScopedTempPath.resolve(
                            "xiuluo_hot_start_tracker_snapshot_" + safeSnapshotName(stage) + ".png"));
                    Path parent = snapshotPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    BufferedImage image = snapshot.get().image();
                    try {
                        ImageIO.write(image, "png", snapshotPath.toFile());
                    } finally {
                        image.flush();
                    }
                    TaskTrackerPanelReadResult panel = taskTrackerPanelService.readXiuluoTrackerPanelFromSnapshot(
                            snapshotPath, binding.getX(), binding.getY(), "xiuluo-v2:hot-start:" + stage);
                    facts.put("trackerFound", Boolean.toString(panel.isFound()));
                    facts.put("trackerGreenLinkCount", Integer.toString(panel.getGreenLinks().size()));
                    facts.put("trackerFactSource", "hwnd_snapshot:" + snapshot.get().provider().name());
                    facts.put("trackerFactReason", panel.isFound() ? "snapshot_read_completed" : "snapshot_read_no_tracker");
                    facts.put("trackerSnapshotPath", snapshotPath.toString());
                    facts.put("trackerDetailPath", safeCloudValue(panel.getDetailRawPath()));
                    facts.put("trackerYellowText", safeCloudValue(panel.getYellowText()));
                    facts.put("trackerSourceType", panel.getSourceType() == null ? "" : panel.getSourceType().name());
                    TaskTrackerGreenLink selected = panel.getSelectedGreenLink() == null
                            ? (panel.getGreenLinks().isEmpty() ? null : panel.getGreenLinks().get(0))
                            : panel.getSelectedGreenLink();
                    if (selected != null) {
                        Point click = selected.centerPoint();
                        facts.put("trackerSelectedClickX", Integer.toString(click.x));
                        facts.put("trackerSelectedClickY", Integer.toString(click.y));
                        facts.put("trackerSelectedTargetMap", safeCloudValue(selected.getTargetMapName()));
                    }
                    log.info("[xiuluo-v2] hot-start tracker facts from HWND snapshot: round={} stage={} window={} "
                                    + "provider={} found={} links={} snapshot={} detail={} reason={}",
                            round, stage, currentWindowLabel(), snapshot.get().provider(), panel.isFound(),
                            panel.getGreenLinks().size(), snapshotPath, panel.getDetailRawPath(),
                            facts.get("trackerFactReason"));
                }
            } catch (RuntimeException | IOException e) {
                facts.put("trackerFound", "not_collected_no_input_safe_source");
                facts.put("trackerGreenLinkCount", "0");
                facts.put("trackerFactSource", "not_collected_no_input_safe_source");
                facts.put("trackerFactReason", "hwnd_snapshot_exception:" + e.getClass().getSimpleName());
                log.warn("[xiuluo-v2] hot-start tracker facts HWND snapshot failed: round={} stage={} window={} reason={}",
                        round, stage, currentWindowLabel(), e.getMessage(), e);
            }
        }

        boolean hasCachedReturnItem = returnItemPrescanService.hasCached(context, TASK_CODE, round, RETURN_ITEM_TEMPLATE);
        facts.put("returnItemAvailability", hasCachedReturnItem ? "cached" : "not_cached");
        return Map.copyOf(facts);
    }

    private String xiuluoBrainWindowId(TaskExecutionContext context) {
        return safeCloudValue(context == null ? null : context.getWindowId());
    }

    private String xiuluoBrainTaskRunId(TaskExecutionContext context) {
        if (context == null || context.getTaskRunId() <= 0L) {
            return "";
        }
        return Long.toString(context.getTaskRunId());
    }

    // 🟠 ROUND LOOP: one Xiuluo round advances through phase transactions here.
    private TaskRunResult runRoundPhases(TaskExecutionContext context,
                                         XiuluoRoundContext initialContext,
                                         XiuluoRoundTrace roundTrace) {
        XiuluoRoundContext roundContext = initialContext;
        int phaseLoopGuard = 0;
        int consecutivePathingYields = 0;
        int consecutiveRoundFailures = 0;
        while (!roundContext.phase().isTerminal()) {
            TaskPauseResumeFingerprint pauseFingerprint = taskPauseResumeReconciler.capture(
                    context, TaskType.XIULUO_V2, roundContext.phase().name(), "phase-loop");
            long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested(
                    context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            TaskPauseResumeReconcileResult pauseReconcile = taskPauseResumeReconciler.reconcileAfterPause(
                    pauseFingerprint, context, pauseBlockedMs);
            if (pauseReconcile.isFallbackTaskHotStart()) {
                roundContext = resolvePauseResumeTaskHotStart(context, roundContext, pauseReconcile, "phase-loop");
                phaseLoopGuard = 0;
                consecutivePathingYields = 0;
                continue;
            }
            if (pauseReconcile.isFingerprintMatched() && pauseReconcile.getPauseBlockedMs() > 0L) {
                roundContext = compensatePreCombatTimerAfterMaintenance(
                        roundContext, pauseReconcile.getPauseBlockedMs(), roundContext.source() + ":user-pause");
            }

            XiuluoRoundContext currentContext = roundContext;
            XiuluoStepOutcome postCombatIdleOutcome = consumePostCombatIdleTimeoutBeforePhase(currentContext);
            if (postCombatIdleOutcome != null) {
                roundTrace.addPhaseOutcome(currentContext, postCombatIdleOutcome,
                        TaskTransactionResult.SHARED_STATE_TRIGGERED);
                roundContext = postCombatIdleOutcome.nextState();
                phaseLoopGuard = 0;
                consecutivePathingYields = 0;
                continue;
            }

            XiuluoStepOutcome watchdogOutcome = checkPreCombatWatchdogTimeout(currentContext);
            if (watchdogOutcome != null) {
                roundTrace.addPhaseOutcome(currentContext, watchdogOutcome, TaskTransactionResult.FAILED);
                consecutiveRoundFailures++;
                roundContext = restartRoundAfterPhaseFailure(
                        context, currentContext, watchdogOutcome, roundTrace, consecutiveRoundFailures);
                phaseLoopGuard = 0;
                consecutivePathingYields = 0;
                continue;
            }

            AtomicReference<XiuluoStepOutcome> phaseOutcome = new AtomicReference<>();
            // 🔒 PHASE TRANSACTION: the current phase owns the task turn for this business step.
            /*
             * The phase is executed exactly once, inside the task transaction. The AtomicReference is
             * only used to bring the structured outcome back out so the phase machine can advance.
             */
            TaskTransactionOutcome transaction;
            try {
                transaction = taskTransactionRunner.runDynamic(
                        "xiuluo-v2:" + currentContext.phase(),
                        TaskTransactionResult.READY_TO_CONTINUE,
                        TaskYieldPolicy.CONTINUE_CHAIN,
                        () -> {
                            XiuluoStepOutcome outcome = runPhase(context, currentContext);
                            outcome = compensateMaintenanceHandoffDelay(outcome);
                            log.info("[xiuluo-v2] phase outcome: phase={} result={} yield={} next={} message={}",
                                    currentContext.phase(), outcome.transactionResult(), outcome.yieldPolicy(),
                                    outcome.nextState().phase(), outcome.message());
                            roundTrace.addPhaseOutcome(currentContext, outcome, outcome.transactionResult());
                            outcome = applyTaskPolicyCloudDecision(
                                    currentContext, outcome, outcome.transactionResult());
                            phaseOutcome.set(outcome);
                            return TaskTransactionRunner.TaskTransactionDecision.of(
                                    outcome.transactionResult(),
                                    outcome.yieldPolicy());
                        });
            } catch (RuntimeException e) {
                if (e instanceof TaskStopRequestedException
                        || e instanceof TaskFatalException
                        || Thread.currentThread().isInterrupted()) {
                    throw e;
                }
                consecutiveRoundFailures++;
                roundContext = restartRoundAfterUnexpectedPhaseException(
                        context, currentContext, roundTrace, e, consecutiveRoundFailures);
                phaseLoopGuard = 0;
                consecutivePathingYields = 0;
                continue;
            }

            XiuluoStepOutcome outcome = phaseOutcome.get();
            if (outcome == null) {
                outcome = XiuluoStepOutcome.failed(currentContext, "phase produced no outcome");
            }

            // 🚦 PHASE RESULT GATE: terminal results leave the round; otherwise advance context.
            if (transaction.result() == TaskTransactionResult.STOPPED
                    || outcome.transactionResult() == TaskTransactionResult.STOPPED) {
                return TaskRunResult.STOPPED;
            }
            if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
                consecutiveRoundFailures++;
                roundContext = restartRoundAfterPhaseFailure(
                        context, currentContext, outcome, roundTrace, consecutiveRoundFailures);
                phaseLoopGuard = 0;
                consecutivePathingYields = 0;
                continue;
            }
            if (outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED) {
                consecutivePathingYields++;
                phaseLoopGuard = 0;
                outcome = yieldAfterMustYield(context, outcome).outcome();
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
                    outcome = yieldAfterMustYield(context, outcome).outcome();
                }
                roundContext = outcome.nextState();
                continue;
            }
            if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD) {
                outcome = yieldAfterMustYield(context, outcome).outcome();
            }
            consecutivePathingYields = 0;
            if (++phaseLoopGuard > 32) {
                log.error("[xiuluo-v2] phase loop guard exceeded: roundContext={}", roundContext);
                roundTrace.addLoopGuard(roundContext, phaseLoopGuard);
                consecutiveRoundFailures++;
                roundContext = restartRoundAfterLoopGuard(context, roundContext, roundTrace, consecutiveRoundFailures);
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

    private XiuluoStepOutcome applyTaskPolicyCloudDecision(XiuluoRoundContext currentContext,
                                                           XiuluoStepOutcome outcome,
                                                           TaskTransactionResult runnerResult) {
        try {
            TaskPolicyCloudDecision<XiuluoPhase> decision = taskPolicyCloudDecisionService.decide(
                    TASK_CODE,
                    "xiuluo-v2-phase-outcome",
                    currentContext.round(),
                    currentContext.phase(),
                    runnerResult,
                    outcome.transactionResult(),
                    outcome.yieldPolicy(),
                    outcome.nextState().phase(),
                    XiuluoPhase.class,
                    Map.of(
                            "source", safeCloudValue(currentContext.source()),
                            "nextSource", safeCloudValue(outcome.nextState().source()),
                            "message", safeCloudValue(outcome.message())));
            if (decision.isCloudRequiredFailure()) {
                log.error("[xiuluo-v2 cloud-execute] TASK_POLICY cloud.required failure: phase={} localResult={} "
                                + "localYield={} localNext={} failureResult={} failureNext={} reason={}",
                        currentContext.phase(),
                        outcome.transactionResult(),
                        outcome.yieldPolicy(),
                        outcome.nextState().phase(),
                        decision.getEffectiveResult(),
                        decision.getEffectiveNextPhase(),
                        decision.getRejectReason());
                return new XiuluoStepOutcome(
                        currentContext.next(decision.getEffectiveNextPhase(), "cloud-required-task-policy"),
                        decision.getEffectiveResult(),
                        decision.getEffectiveYieldPolicy(),
                        "cloud.required TASK_POLICY failure: " + decision.getRejectReason(),
                        outcome.waitSpec());
            }
            if (!decision.isCloudExecuted()) {
                return outcome;
            }
            TaskPolicyCloudDecision.AppliedOutcome<XiuluoPhase> applied = decision.appliedOutcome();
            XiuluoRoundContext nextState = outcome.nextState();
            XiuluoRoundContext cloudNextState = new XiuluoRoundContext(
                    applied.nextPhase(),
                    nextState.objective(),
                    nextState.objectiveParseFuture(),
                    nextState.shortcutTrackerParseFuture(),
                    nextState.preCombatStartedAtMs(),
                    nextState.round(),
                    nextState.source(),
                    nextState.waitingPathing(),
                    nextState.startExitPrepathStarted(),
                    nextState.enteredBattleByXiuluo(),
                    nextState.routeMode(),
                    nextState.combatSource(),
                    nextState.shortcutTrackerDetailPath(),
                    nextState.shortcutTrackerClickX(),
                    nextState.shortcutTrackerClickY(),
                    nextState.shortcutTrackerClickWindowRelativeX(),
                    nextState.shortcutTrackerClickWindowRelativeY(),
                    nextState.firstTrackerGreenClickAtMs(),
                    nextState.shortcutTrackerRetryCount(),
                    nextState.shortcutPathingIntentId(),
                    nextState.phaseRetryCount(),
                    nextState.enterBattleConfirmRetryCount(),
                    nextState.recoveryCount());
            log.info("[xiuluo-v2 cloud-execute] TASK_POLICY accepted: phase={} localResult={} localYield={} localNext={} "
                            + "cloudResult={} cloudYield={} cloudNext={} message={}",
                    currentContext.phase(),
                    outcome.transactionResult(),
                    outcome.yieldPolicy(),
                    nextState.phase(),
                    applied.transactionResult(),
                    applied.yieldPolicy(),
                    applied.nextPhase(),
                    outcome.message());
            return new XiuluoStepOutcome(
                    cloudNextState,
                    applied.transactionResult(),
                    applied.yieldPolicy(),
                    outcome.message(),
                    outcome.waitSpec());
        } catch (RuntimeException e) {
            if (Thread.currentThread().isInterrupted()) {
                throw e;
            }
            log.error("[xiuluo-v2 cloud-execute] TASK_POLICY exception; terminal cloud.required failure: phase={} error={}",
                    currentContext.phase(), e.toString());
            log.debug("[xiuluo-v2 cloud-execute] TASK_POLICY execute failure stack", e);
            return new XiuluoStepOutcome(
                    currentContext.next(XiuluoPhase.FAILED, "cloud-required-task-policy-exception"),
                    TaskTransactionResult.RETRYABLE_ERROR,
                    TaskYieldPolicy.MUST_YIELD,
                    "cloud.required TASK_POLICY exception: " + e.getClass().getSimpleName(),
                    outcome.waitSpec());
        }
    }

    private TaskRecoveryCloudDecision<XiuluoPhase> decideTaskRecovery(XiuluoRoundContext state,
                                                                      XiuluoStepOutcome outcome,
                                                                      String recoveryAction,
                                                                      String message,
                                                                      Map<String, String> extraContext) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("serviceId", CloudDecisionServiceId.TASK_RECOVERY.name());
        context.put("source", safeCloudValue(state.source()));
        context.put("outcomeResult", String.valueOf(outcome.transactionResult()));
        context.put("yieldPolicy", String.valueOf(outcome.yieldPolicy()));
        context.put("nextSource", safeCloudValue(outcome.nextState().source()));
        context.put("message", safeCloudValue(message));
        if (extraContext != null) {
            extraContext.forEach((key, value) -> context.put(key, safeCloudValue(value)));
        }
        return taskRecoveryCloudDecisionService.decide(
                TASK_CODE,
                "xiuluo-v2-recovery",
                state.round(),
                state.phase(),
                recoveryAction,
                outcome.nextState().phase(),
                XiuluoPhase.class,
                context);
    }

    private TaskRecoveryCloudDecision<XiuluoPhase> decideRoundRestartRecovery(XiuluoRoundContext failedContext,
                                                                              XiuluoStepOutcome outcome,
                                                                              XiuluoRoundContext restartState,
                                                                              String recoveryAction,
                                                                              int consecutiveRoundFailures) {
        return taskRecoveryCloudDecisionService.decide(
                TASK_CODE,
                "xiuluo-v2-recovery",
                failedContext.round(),
                failedContext.phase(),
                recoveryAction,
                restartState.phase(),
                XiuluoPhase.class,
                Map.of(
                        "serviceId", CloudDecisionServiceId.TASK_RECOVERY.name(),
                        "failedSource", safeCloudValue(failedContext.source()),
                        "outcomeResult", outcome == null ? "" : String.valueOf(outcome.transactionResult()),
                        "outcomeNextPhase", outcome == null ? "" : String.valueOf(outcome.nextState().phase()),
                        "consecutiveRoundFailures", Integer.toString(consecutiveRoundFailures),
                        "nextSource", safeCloudValue(restartState.source())));
    }

    private void ensureStartupIncenseBeforeHotStart(TaskExecutionContext context) {
        if (startupIncenseChecked) {
            return;
        }
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        log.info("[xiuluo-v2] startup incense check before hot-start");
        playerStateService.ensureSheYaoXiangActiveForLeaderTask("xiuluo-v2:startup", context);
        startupIncenseChecked = true;
        startupIncensePending = false;
    }

    private XiuluoStepOutcome consumePostCombatIdleTimeoutBeforePhase(XiuluoRoundContext state) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return null;
        }
        WindowReadyEvent timeoutEvent = windowReadyEventBus
                .latest(runtime.getWindowId(), WindowReadyEventType.POST_COMBAT_IDLE_TIMEOUT)
                .orElse(null);
        if (timeoutEvent == null
                || timeoutEvent.getTaskType() != TaskType.XIULUO_V2
                || timeoutEvent.getSequence() <= lastPostCombatIdleTimeoutConsumedSeq) {
            return null;
        }
        lastPostCombatIdleTimeoutConsumedSeq = timeoutEvent.getSequence();
        XiuluoPhase originalPhase = state.phase();
        XiuluoPhase restartPhase = XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC;
        String clearScope = "dialog-preparation/prepared/dialog/pathing/tracker-shortcut/pre-combat-runtime";
        runtime.clearDialogPreparationRequest("xiuluo-v2 consumed POST_COMBAT_IDLE_TIMEOUT");
        runtime.clearDialogInterest("xiuluo-v2 consumed POST_COMBAT_IDLE_TIMEOUT");
        clearTrackerShortcutPathingIntent(runtime, "xiuluo-v2 post-combat idle timeout consumed");
        log.warn("[xiuluo-v2 post-combat-idle] timeout consumed by task: originalPhase={} round={} windowId={} hwnd={} readySeq={} readyAgeMs={} lastCombatExitAtMs={} elapsedMs={} clearScope={} restartPhase={} source={} summary={}",
                originalPhase, state.round(), runtime.getWindowId(), timeoutEvent.getHwnd(),
                timeoutEvent.getSequence(), readyAgeMs(timeoutEvent), timeoutEvent.getLastCombatExitAtMs(),
                timeoutEvent.getElapsedMs(), clearScope, restartPhase, timeoutEvent.getSource(),
                timeoutEvent.getSummary());
        return XiuluoStepOutcome.sharedState(
                state.recoverTo(restartPhase, "post-combat-idle-timeout-reaccept"),
                "post-combat idle timeout; restart accept-task flow");
    }

    private long readyAgeMs(WindowReadyEvent event) {
        return event == null ? -1L : Math.max(0L, System.currentTimeMillis() - event.getCreatedAtMs());
    }

    private XiuluoStepOutcome checkPreCombatWatchdogTimeout(XiuluoRoundContext state) {
        if (!shouldApplyPreCombatWatchdog(state)) {
            return null;
        }
        long startedAt = state.preCombatStartedAtMs();
        if (startedAt <= 0L) {
            return null;
        }
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - startedAt);
        if (elapsedMs < PRE_COMBAT_WATCHDOG_TIMEOUT_MS) {
            return null;
        }
        log.warn("[xiuluo-v2] xiuluo pre-combat watchdog timeout: round={} phase={} elapsedMs={} limitMs=180000 source={}",
                state.round(), state.phase(), elapsedMs, state.source());
        clearPreCombatWaitOwnedRuntimeState(state, "xiuluo pre-combat watchdog timeout");
        return XiuluoStepOutcome.failed(state,
                "xiuluo pre-combat watchdog timeout: elapsedMs=" + elapsedMs
                        + " limitMs=" + PRE_COMBAT_WATCHDOG_TIMEOUT_MS)
                .withFact("watchdogTimeout", "true")
                .withFact("watchdogKind", "PRE_COMBAT");
    }

    private void clearPreCombatWaitOwnedRuntimeState(XiuluoRoundContext state, String reason) {
        if (state == null || !shouldClearPreCombatWaitOwnedRuntimeState(state)) {
            return;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return;
        }
        String clearReason = reason + ": phase=" + state.phase() + " source=" + state.source();
        runtime.clearPathingSignal(clearReason);
        // CR253: watchdog/failure cleanup also ends the green-chain attempt — a late prepared job
        // must not leak into the restarted round (round-87 class).
        runtime.clearXiuluoGreenChainSchedule(clearReason);
        PreparedDialogAction prepared = runtime.getPreparedDialogAction();
        if (prepared != null && prepared.getSource() != null
                && prepared.getSource().startsWith("xiuluo-v2:")) {
            runtime.clearPreparedDialogAction(clearReason);
        }
    }

    private boolean shouldClearPreCombatWaitOwnedRuntimeState(XiuluoRoundContext state) {
        return state.phase() == XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING
                || state.phase() == XiuluoPhase.NAVIGATE_TO_TARGET
                || state.waitingPathing();
    }

    private boolean shouldApplyPreCombatWatchdog(XiuluoRoundContext state) {
        if (state == null || state.enteredBattleByXiuluo() || state.phase().isTerminal()) {
            return false;
        }
        return switch (state.phase()) {
            case PREPARE_ROUND,
                    ACCEPT_TASK_NAVIGATE_TO_NPC,
                    ACCEPT_TASK_CLICK_NPC,
                    ACCEPT_TASK_DIALOG,
                    READ_OBJECTIVE,
                    AFTER_ACCEPT_MAINTENANCE_CHECK,
                    BEFORE_ROUTE_MAINTENANCE_CHECK,
                    TRY_TRACKER_SHORTCUT,
                    WAIT_TRACKER_SHORTCUT_PATHING,
                    NAVIGATE_TO_TARGET,
                    CLICK_TARGET_NPC,
                    CONFIRM_ENTER_BATTLE,
                    WAIT_TEAM_READY -> true;
            case WAIT_COMBAT,
                    RETURN_HOME,
                    NAVIGATE_BACK_TO_START,
                    WAIT_TEAM_RETURN,
                    ROUND_DONE,
                    FAILED,
                    STOPPED -> false;
        };
    }

    private XiuluoRoundContext restartRoundAfterUnexpectedPhaseException(TaskExecutionContext context,
                                                                         XiuluoRoundContext failedContext,
                                                                         XiuluoRoundTrace roundTrace,
                                                                         RuntimeException exception,
                                                                         int consecutiveRoundFailures) {
        log.error("[xiuluo-v2] phase exception; restart same round from accept flow: phase={} consecutiveFailures={}/{}",
                failedContext.phase(), consecutiveRoundFailures, MAX_CONSECUTIVE_ROUND_FAILURES, exception);
        XiuluoStepOutcome outcome = XiuluoStepOutcome.failed(
                failedContext, "phase exception: " + exception.getClass().getSimpleName());
        roundTrace.addPhaseOutcome(failedContext, outcome, TaskTransactionResult.FAILED);
        return restartRoundAfterPhaseFailure(context, failedContext, outcome, roundTrace, consecutiveRoundFailures);
    }

    private XiuluoRoundContext restartRoundAfterPhaseFailure(TaskExecutionContext context,
                                                             XiuluoRoundContext failedContext,
                                                             XiuluoStepOutcome outcome,
                                                             XiuluoRoundTrace roundTrace,
                                                             int consecutiveRoundFailures) {
        /*
         * Xiuluo failures usually mean the current accepted objective became unusable: bad route,
         * missed dialog, or stale task-panel text. Do not bubble this to WindowTaskRunner as a task
         * failure immediately; abandon the current objective and re-enter the same round from the
         * accept flow. Only a long streak of failed round attempts is treated as fatal.
         */
        log.warn("[xiuluo-v2] phase failed; restart same round from accept flow: phase={} message={} next={} consecutiveFailures={}/{}",
                failedContext.phase(), outcome.message(), outcome.nextState().phase(),
                consecutiveRoundFailures, MAX_CONSECUTIVE_ROUND_FAILURES);
        archiveRoundFailureCase(context, roundTrace, "phase-failed", failedContext, outcome.message(), outcome);
        XiuluoRoundContext restartState = XiuluoRoundContext.start(failedContext.round());
        TaskRecoveryCloudDecision<XiuluoPhase> recoveryDecision = decideRoundRestartRecovery(
                failedContext,
                outcome,
                restartState,
                "phase-failed",
                consecutiveRoundFailures);
        if (!recoveryDecision.isRecoveryAllowed()) {
            log.warn("[xiuluo-v2] recovery decision unavailable; keep collecting failures and restart locally: "
                            + "phase={} action=phase-failed status={} reason={}",
                    failedContext.phase(), recoveryDecision.getStatus(), recoveryDecision.getRejectReason());
        }
        uiCleanerService.cleanUpAll();
        yieldAfterMustYield(context, outcome);
        return restartState;
    }

    private XiuluoRoundContext restartRoundAfterLoopGuard(TaskExecutionContext context,
                                                          XiuluoRoundContext currentContext,
                                                          XiuluoRoundTrace roundTrace,
                                                          int consecutiveRoundFailures) {
        log.warn("[xiuluo-v2] phase loop guard recovery; restart same round from accept flow: phase={} source={} consecutiveFailures={}/{}",
                currentContext.phase(), currentContext.source(),
                consecutiveRoundFailures, MAX_CONSECUTIVE_ROUND_FAILURES);
        archiveRoundFailureCase(context, roundTrace, "loop-guard", currentContext,
                "phase loop guard exceeded", null);
        XiuluoRoundContext restartState = XiuluoRoundContext.start(currentContext.round());
        TaskRecoveryCloudDecision<XiuluoPhase> recoveryDecision = decideRoundRestartRecovery(
                currentContext,
                null,
                restartState,
                "loop-guard",
                consecutiveRoundFailures);
        if (!recoveryDecision.isRecoveryAllowed()) {
            log.warn("[xiuluo-v2] loop recovery decision unavailable; keep collecting failures and restart locally: "
                            + "phase={} status={} reason={}",
                    currentContext.phase(), recoveryDecision.getStatus(), recoveryDecision.getRejectReason());
        }
        uiCleanerService.cleanUpAll();
        TaskSleep.sleepOrStop(context, TASK_TURN_HANDOFF_DELAY_MS, "Xiuluo V2 task interrupted");
        return restartState;
    }

    private void archiveRoundFailureCase(TaskExecutionContext context,
                                         XiuluoRoundTrace roundTrace,
                                         String reason,
                                         XiuluoRoundContext failedContext,
                                         String message,
                                         XiuluoStepOutcome outcome) {
        if (roundTrace == null || failedContext == null) {
            return;
        }
        try {
            String time = LocalDateTime.now().format(FAILURE_CASE_TIME_FORMAT);
            String window = safeFailureFileName(context.getWindowId(), "unknown-window");
            String phase = safeFailureFileName(failedContext.phase().name(), "unknown-phase");
            String safeReason = safeFailureFileName(reason, "unknown-reason");
            Path caseDir = XIULUO_FAILURE_CASE_DIR
                    .resolve(time + "_round-" + failedContext.round() + "_" + phase + "_" + safeReason + "_" + window)
                    .normalize();
            Files.createDirectories(caseDir);

            Files.writeString(caseDir.resolve("summary.md"),
                    roundTrace.summaryMarkdown(reason, failedContext, message, outcome, caseDir),
                    StandardCharsets.UTF_8);
            Files.writeString(caseDir.resolve("events.jsonl"), roundTrace.eventsJsonl(), StandardCharsets.UTF_8);
            LocalDateTime failedAt = LocalDateTime.now();
            copyXiuluoFailureLogSlice(Path.of("logs", "dhxy-console.log"),
                    caseDir.resolve("dhxy-console-slice.log"), roundTrace.startedAt(), failedAt);
            copyXiuluoFailureLogSlice(Path.of("logs", "tracker-coordinate.log"),
                    caseDir.resolve("tracker-coordinate-slice.log"), roundTrace.startedAt(), failedAt);
            appendXiuluoFailureCaseReport(caseDir, reason, failedContext, message, roundTrace, failedAt);
            automationMetricsService.recordXiuluoFailureCase(context, caseDir, reason,
                    failedContext.phase().name(), failedContext.round(), message);
            log.warn("[xiuluo-v2] failure case archived: dir={} round={} phase={} reason={} events={} dropped={}",
                    caseDir, failedContext.round(), failedContext.phase(), reason,
                    roundTrace.eventCount(), roundTrace.droppedEventCount());
        } catch (Exception e) {
            log.warn("[xiuluo-v2] failure case archive failed: round={} phase={} reason={} error={}",
                    failedContext.round(), failedContext.phase(), reason, e.getMessage(), e);
        }
    }

    /**
     * Saves the original console/tracker lines that fall within one failed round. This runs only on
     * failure, so the relatively broad file read does not sit on the task's normal execution path.
     */
    private void copyXiuluoFailureLogSlice(
            Path sourceLog, Path targetLog, LocalDateTime startedAt, LocalDateTime failedAt) throws IOException {
        if (!Files.exists(sourceLog)) {
            Files.writeString(targetLog, "source log not found: " + sourceLog + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            return;
        }
        List<String> selected = new ArrayList<>();
        for (String line : Files.readAllLines(sourceLog, StandardCharsets.UTF_8)) {
            if (line.length() < 23) {
                continue;
            }
            try {
                LocalDateTime logTime = LocalDateTime.parse(line.substring(0, 23), CONSOLE_LOG_TIME_FORMAT);
                if (!logTime.isBefore(startedAt) && !logTime.isAfter(failedAt)) {
                    selected.add(line);
                }
            } catch (RuntimeException ignored) {
                // Lines without the standard timestamp cannot be placed in this round safely.
            }
        }
        Files.write(targetLog, selected, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Appends one human-reviewable case index entry. The raw slices remain in the case directory so
     * repeated failures do not turn the Markdown index into a copy of the full console log.
     */
    private void appendXiuluoFailureCaseReport(
            Path caseDir,
            String reason,
            XiuluoRoundContext failedContext,
            String message,
            XiuluoRoundTrace roundTrace,
            LocalDateTime failedAt) throws IOException {
        XiuluoFailureAssessment assessment = assessXiuluoFailure(reason, message);
        String casePath = caseDir.toString().replace('\\', '/');
        StringBuilder entry = new StringBuilder();
        entry.append(System.lineSeparator()).append("## Case ")
                .append(caseDir.getFileName()).append(System.lineSeparator()).append(System.lineSeparator());
        entry.append("- 轮次: `").append(failedContext.round()).append("`；失败阶段: `")
                .append(failedContext.phase()).append("`。 ").append(System.lineSeparator());
        entry.append("- 时间: `").append(roundTrace.startedAt()).append("` -> `")
                .append(failedAt).append("`。 ").append(System.lineSeparator());
        entry.append("- 原始日志: `").append(casePath).append("/dhxy-console-slice.log`；")
                .append(" tracker: `").append(casePath).append("/tracker-coordinate-slice.log`；")
                .append(" 事件: `").append(casePath).append("/events.jsonl`。 ").append(System.lineSeparator());
        entry.append("- 失败原因: `").append(safeCloudValue(reason)).append("` - ")
                .append(safeCloudValue(message)).append(System.lineSeparator());
        entry.append("- 初步结论: ").append(assessment.conclusion()).append(System.lineSeparator());
        entry.append("- 修改建议: ").append(assessment.recommendation()).append(System.lineSeparator());
        entry.append("- 后续: 已自动清理并从 `PREPARE_ROUND -> ACCEPT_TASK_NAVIGATE_TO_NPC` 重开；待集中复盘。")
                .append(System.lineSeparator());
        synchronized (XIULUO_FAILURE_CASE_REPORT_MONITOR) {
            Files.createDirectories(XIULUO_FAILURE_CASE_REPORT.getParent());
            Files.writeString(XIULUO_FAILURE_CASE_REPORT, entry.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private XiuluoFailureAssessment assessXiuluoFailure(String reason, String message) {
        String detail = (safeCloudValue(reason) + " " + safeCloudValue(message)).toLowerCase(Locale.ROOT);
        if (detail.contains("cloud") || detail.contains("session") || detail.contains("ack")) {
            return new XiuluoFailureAssessment("云端链路、会话或协议未能完成本轮推进。",
                    "核对该 case 的 cloud request/response、session/stateSeq/actionId 与本地接受门；修复后再看同类 case 是否消失。");
        }
        if (detail.contains("loop")) {
            return new XiuluoFailureAssessment("本轮 phase 出现无进展循环。",
                    "按 events.jsonl 定位重复 phase 与无变化事实，收敛该 phase 的 retry/park/next-phase 分支。");
        }
        if (detail.contains("return") || detail.contains("combat")) {
            return new XiuluoFailureAssessment("战斗退出或回程状态未能形成可继续的业务事实。",
                    "结合原始日志核对 combat state、return item 与 start-map 验证顺序，再决定修复边界。");
        }
        return new XiuluoFailureAssessment("本轮业务 phase 返回失败，当前任务已自动放弃该轮上下文。",
                "先按失败 phase 与原始日志聚类；仅在同类 case 重复后统一调整对应业务分支。");
    }

    private XiuluoEventParkResult yieldAfterMustYield(TaskExecutionContext context, XiuluoStepOutcome outcome) {
        XiuluoWaitSpec waitSpec = outcome.waitSpec();
        if (waitSpec != null) {
            log.info("[xiuluo-v2] task turn event wait: result={} next={} reason={} wakeTypes={} timeoutMs={} afterSequence={}",
                    outcome.transactionResult(), outcome.nextState().phase(), waitSpec.getReason(),
                    waitSpec.getWakeTypes(), waitSpec.getTimeoutMs(), waitSpec.getAfterSequence());
            XiuluoStepOutcome maintenanceOutcome = maybeRunLeaderPathingSummonMaintenanceBeforePark(context, outcome);
            if (maintenanceOutcome != null) {
                return XiuluoEventParkResult.notParked(maintenanceOutcome);
            }
            return parkAfterYieldIfNeeded(context, outcome);
        }
        /*
         * Releasing the task turn is not enough by itself: this leader thread can immediately loop
         * and reacquire the fair lock before follower auto-battle polling gets a chance to tryLock.
         * A short handoff delay is intentional for shared states such as pathing and combat.
         */
        long delayMs = handoffDelayMs(outcome);
        log.info("[xiuluo-v2] task turn handoff delay: result={} next={} delayMs={}",
                outcome.transactionResult(), outcome.nextState().phase(), delayMs);
        TaskSleep.sleepOrStop(context, delayMs, "Xiuluo V2 task interrupted");
        return XiuluoEventParkResult.notParked(outcome);
    }

    private XiuluoStepOutcome maybeRunLeaderPathingSummonMaintenanceBeforePark(TaskExecutionContext context,
                                                                               XiuluoStepOutcome outcome) {
        if (outcome == null
                || outcome.transactionResult() != TaskTransactionResult.PATHING_STARTED
                || outcome.nextState() == null
                || outcome.waitSpec() == null) {
            return null;
        }
        XiuluoWaitReason reason = outcome.waitSpec().getReason();
        /*
         * CR253: the green-chain park (WAIT_TRACKER_SHORTCUT_PATHING) no longer runs opportunistic
         * summon maintenance before parking — the background due detector publishes a
         * SUMMON_SKILL_CLEANUP prepared job and its consumer runs the full flow. Only the
         * non-shortcut navigation wait keeps the before-park hook.
         */
        if (reason != XiuluoWaitReason.WAIT_TARGET_PATHING_TERMINAL) {
            return null;
        }
        return runLeaderPathingSummonSkillMaintenance(context, outcome.nextState(), "before-park");
    }

    /**
     * Park a 修罗 phase after it released the task turn and wait for the runner's current-window
     * signal. Business state is still consumed by the next phase transaction after this method
     * returns. If the user paused while the thread was parked, the pause duration is returned through
     * the outcome's next state before the watchdog is checked again.
     *
     * @param context current task context for stop-aware waiting.
     * @param outcome phase outcome carrying a scheduling-only wait spec.
     */
    private XiuluoEventParkResult parkAfterYieldIfNeeded(TaskExecutionContext context, XiuluoStepOutcome outcome) {
        XiuluoWaitSpec waitSpec = outcome.waitSpec();
        if (waitSpec == null) {
            return XiuluoEventParkResult.notParked(outcome);
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            log.warn("[xiuluo-v2 wait] skip park: no window runtime next={} reason={}",
                    outcome.nextState().phase(), waitSpec.getReason());
            return XiuluoEventParkResult.notParked(outcome);
        }
        EnumSet<WindowReadyEventType> wakeTypes = toWakeTypeEnumSet(waitSpec.getWakeTypes());
        if (wakeTypes.isEmpty()) {
            log.warn("[xiuluo-v2 wait] skip park: no wake types windowId={} next={} reason={}",
                    runtime.getWindowId(), outcome.nextState().phase(), waitSpec.getReason());
            return XiuluoEventParkResult.notParked(outcome);
        }
        if (waitSpec.getTimeoutMs() == 0L) {
            log.warn("[xiuluo-v2 wait] skip park: invalid zero-timeout wait spec windowId={} next={} reason={} timeoutMs=0",
                    runtime.getWindowId(), outcome.nextState().phase(), waitSpec.getReason());
            return XiuluoEventParkResult.notParked(new XiuluoStepOutcome(
                    outcome.nextState(),
                    outcome.transactionResult(),
                    outcome.yieldPolicy(),
                    "invalid no-real-park wait spec: zero timeout reason=" + waitSpec.getReason(),
                    null));
        }
        long boundedTimeoutMs = boundedPreCombatWaitTimeoutMs(outcome.nextState(), waitSpec.getTimeoutMs(),
                waitSpec.getReason().name());
        if (boundedTimeoutMs == 0L) {
            return XiuluoEventParkResult.notParked(preCombatWatchdogTimeoutOutcome(
                    outcome.nextState(), waitSpec.getReason().name(), "before-event-wait"));
        }
        long startedAt = System.currentTimeMillis();
        Optional<WindowReadyEvent> wakeEvent = waitSpec.getReason() == XiuluoWaitReason.WAIT_TARGET_PATHING_TERMINAL
                ? windowReadyEventBus.awaitNewerPathingTerminalOrPreparedRoute(
                        runtime.getWindowId(),
                        waitSpec.getPathingIntentId(),
                        waitSpec.getPathingSourcePrefix(),
                        waitSpec.getPathingTargetMapName(),
                        waitSpec.getAfterSequence(),
                        boundedTimeoutMs)
                : windowReadyEventBus.awaitNewer(
                        runtime.getWindowId(),
                        wakeTypes,
                        waitSpec.getAfterSequence(),
                        boundedTimeoutMs);
        TaskPauseResumeFingerprint pauseFingerprint = taskPauseResumeReconciler.capture(
                context, TaskType.XIULUO_V2, outcome.nextState().phase().name(), waitSpec.getReason().name());
        long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested(context, "Xiuluo V2 task interrupted");
        TaskPauseResumeReconcileResult pauseReconcile = taskPauseResumeReconciler.reconcileAfterPause(
                pauseFingerprint, context, pauseBlockedMs);
        XiuluoStepOutcome adjustedOutcome = outcome;
        if (pauseReconcile.isFallbackTaskHotStart()) {
            XiuluoRoundContext hotStart = resolvePauseResumeTaskHotStart(
                    context, outcome.nextState(), pauseReconcile, "event-wait:" + waitSpec.getReason());
            long elapsedMs = Math.max(0L, System.currentTimeMillis() - startedAt);
            log.warn("[xiuluo-v2 wait] park pause-resume fallback: next={} windowId={} reason={} wakeTypes={} afterSequence={} timeoutMs={} boundedTimeoutMs={} elapsedMs={} pauseBlockedMs={} fingerprintMatched={} mismatchReason={} compensatedTimers={} clearedVolatileState={} fallbackTaskHotStart={} wakeResult={} wakeType={} wakeSeq={} source={}",
                    hotStart.phase(), runtime.getWindowId(), waitSpec.getReason(), waitSpec.getWakeTypes(),
                    waitSpec.getAfterSequence(), waitSpec.getTimeoutMs(), boundedTimeoutMs, elapsedMs,
                    pauseReconcile.getPauseBlockedMs(), pauseReconcile.isFingerprintMatched(),
                    pauseReconcile.getMismatchReason(), pauseReconcile.getCompensatedTimers(),
                    pauseReconcile.getClearedVolatileState(), pauseReconcile.isFallbackTaskHotStart(),
                    wakeEvent.isPresent() ? "event" : "timeout-or-interrupted",
                    wakeEvent.map(WindowReadyEvent::getType).orElse(null),
                    wakeEvent.map(WindowReadyEvent::getSequence).orElse(-1L),
                    outcome.message());
            return XiuluoEventParkResult.parked(XiuluoStepOutcome.sharedState(
                    hotStart,
                    "pause-resume fingerprint mismatch; task hot-start fallback"));
        }
        if (pauseReconcile.isFingerprintMatched() && pauseReconcile.getPauseBlockedMs() > 0L) {
            XiuluoRoundContext adjustedState = compensatePreCombatTimerAfterMaintenance(
                    outcome.nextState(), pauseReconcile.getPauseBlockedMs(),
                    "xiuluo-v2:event-wait:" + waitSpec.getReason());
            adjustedOutcome = outcome.withNextState(adjustedState);
        }
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - startedAt);
        log.info("[xiuluo-v2 wait] park finished: next={} windowId={} reason={} wakeTypes={} afterSequence={} timeoutMs={} boundedTimeoutMs={} elapsedMs={} pauseBlockedMs={} fingerprintMatched={} mismatchReason={} compensatedTimers={} clearedVolatileState={} fallbackTaskHotStart={} wakeResult={} wakeType={} wakeSeq={} source={}",
                adjustedOutcome.nextState().phase(), runtime.getWindowId(), waitSpec.getReason(), waitSpec.getWakeTypes(),
                waitSpec.getAfterSequence(), waitSpec.getTimeoutMs(), boundedTimeoutMs, elapsedMs,
                pauseReconcile.getPauseBlockedMs(), pauseReconcile.isFingerprintMatched(),
                pauseReconcile.getMismatchReason(), pauseReconcile.getCompensatedTimers(),
                pauseReconcile.getClearedVolatileState(), pauseReconcile.isFallbackTaskHotStart(),
                wakeEvent.isPresent() ? "event" : "timeout-or-interrupted",
                wakeEvent.map(WindowReadyEvent::getType).orElse(null),
                wakeEvent.map(WindowReadyEvent::getSequence).orElse(-1L),
                outcome.message());
        if ((waitSpec.getReason() == XiuluoWaitReason.WAIT_TEAM_RETURN_STATE_CHANGE
                || waitSpec.getReason() == XiuluoWaitReason.WAIT_MAINTENANCE_BROADCAST_QUEUE)
                && elapsedMs > 0L) {
            /*
             * CR244/CR245: waiting for member facts (pending-return set, broadcast queue drain) is
             * legitimate coordination time. Shift the pre-combat anchor by the parked duration so
             * the wait is not billed to the accept-chain budget (same compensation model as the
             * CR234 pause/checkpoint shifts; maintenance hooks already compensate their own
             * blocking work the same way).
             */
            adjustedOutcome = adjustedOutcome.withNextState(compensatePreCombatTimerAfterMaintenance(
                    adjustedOutcome.nextState(), elapsedMs,
                    "xiuluo-v2:member-fact-park:" + waitSpec.getReason()));
        }
        if (wakeEvent.isEmpty() && remainingPreCombatWatchdogBudgetMs(
                adjustedOutcome.nextState(), System.currentTimeMillis()) <= 0L) {
            return XiuluoEventParkResult.parked(preCombatWatchdogTimeoutOutcome(
                    adjustedOutcome.nextState(), waitSpec.getReason().name(), "event-wait-timeout"));
        }
        return XiuluoEventParkResult.parked(adjustedOutcome);
    }

    private long boundedPreCombatWaitTimeoutMs(XiuluoRoundContext state, long requestedTimeoutMs, String waitContext) {
        long remainingMs = remainingPreCombatWatchdogBudgetMs(state, System.currentTimeMillis());
        if (remainingMs == Long.MAX_VALUE) {
            return requestedTimeoutMs;
        }
        if (remainingMs <= 0L) {
            log.warn("[xiuluo-v2 wait] pre-combat budget exhausted before wait: phase={} waitContext={} source={} requestedTimeoutMs={} remainingMs={}",
                    state == null ? null : state.phase(), waitContext, state == null ? null : state.source(),
                    requestedTimeoutMs, remainingMs);
            return 0L;
        }
        if (requestedTimeoutMs < 0L) {
            return remainingMs;
        }
        return Math.min(requestedTimeoutMs, remainingMs);
    }

    private long remainingPreCombatWatchdogBudgetMs(XiuluoRoundContext state, long nowMs) {
        if (!shouldApplyPreCombatWatchdog(state)) {
            return Long.MAX_VALUE;
        }
        long startedAt = state.preCombatStartedAtMs();
        if (startedAt <= 0L) {
            return Long.MAX_VALUE;
        }
        long elapsedMs = Math.max(0L, nowMs - startedAt);
        return PRE_COMBAT_WATCHDOG_TIMEOUT_MS - elapsedMs;
    }

    private XiuluoStepOutcome preCombatWatchdogTimeoutOutcome(XiuluoRoundContext state,
                                                              String waitContext,
                                                              String trigger) {
        long nowMs = System.currentTimeMillis();
        long startedAt = state == null ? 0L : state.preCombatStartedAtMs();
        long elapsedMs = startedAt <= 0L ? 0L : Math.max(0L, nowMs - startedAt);
        long remainingMs = remainingPreCombatWatchdogBudgetMs(state, nowMs);
        log.warn("[xiuluo-v2] pre-combat watchdog wait budget timeout: round={} phase={} waitContext={} trigger={} elapsedMs={} remainingMs={} limitMs={} source={} routeMode={} retry={} shortcutIntent={}",
                state == null ? -1 : state.round(),
                state == null ? null : state.phase(),
                waitContext,
                trigger,
                elapsedMs,
                remainingMs,
                PRE_COMBAT_WATCHDOG_TIMEOUT_MS,
                state == null ? null : state.source(),
                state == null ? null : state.routeMode(),
                state == null ? -1 : state.shortcutTrackerRetryCount(),
                state == null ? null : state.shortcutPathingIntentId());
        clearPreCombatWaitOwnedRuntimeState(state, "xiuluo pre-combat watchdog wait budget timeout:" + waitContext);
        return XiuluoStepOutcome.failed(state,
                "xiuluo pre-combat watchdog wait budget timeout: waitContext=" + waitContext
                        + " trigger=" + trigger
                        + " elapsedMs=" + elapsedMs
                        + " remainingMs=" + remainingMs
                        + " limitMs=" + PRE_COMBAT_WATCHDOG_TIMEOUT_MS)
                .withFact("watchdogTimeout", "true")
                .withFact("watchdogKind", "PRE_COMBAT");
    }

    private EnumSet<WindowReadyEventType> toWakeTypeEnumSet(Set<WindowReadyEventType> wakeTypes) {
        if (wakeTypes == null || wakeTypes.isEmpty()) {
            return EnumSet.noneOf(WindowReadyEventType.class);
        }
        return EnumSet.copyOf(wakeTypes);
    }

    private long handoffDelayMs(XiuluoStepOutcome outcome) {
        XiuluoRoundContext nextState = outcome.nextState();
        if (nextState != null && isMaintenanceBroadcastHandoffSource(nextState.source(), null)) {
            long delayMs = maintenanceBroadcastHandoffDelayMs();
            log.info("[xiuluo-v2] maintenance broadcast handoff delay: source={} delayMs={}",
                    nextState.source(), delayMs);
            return delayMs;
        }
        if (nextState != null
                && nextState.source() != null
                && (nextState.source().startsWith(UNDER_THREE_WAIT_SOURCE_PREFIX)
                || nextState.source().startsWith(TEAM_RETURN_WAIT_SOURCE_PREFIX))) {
            long configured = botProperties.getReturnTeamLeaderWaitPollMs();
            return configured > 0 ? configured : DEFAULT_TEAM_READY_WAIT_POLL_MS;
        }
        return TASK_TURN_HANDOFF_DELAY_MS;
    }

    private XiuluoStepOutcome compensateMaintenanceHandoffDelay(XiuluoStepOutcome outcome) {
        if (outcome == null || outcome.nextState() == null
                || !isMaintenanceBroadcastHandoffSource(outcome.nextState().source(), null)) {
            return outcome;
        }
        XiuluoRoundContext adjusted = compensatePreCombatTimerAfterMaintenance(
                outcome.nextState(), maintenanceBroadcastHandoffDelayMs(), outcome.nextState().source() + ":handoff-delay");
        return adjusted == outcome.nextState() ? outcome : outcome.withNextState(adjusted);
    }

    private long maintenanceBroadcastHandoffDelayMs() {
        return MAINTENANCE_BROADCAST_HANDOFF_DELAY_MS;
    }

    private String maintenanceBroadcastHandoffSource(String stage, String actionKey) {
        return MAINTENANCE_BROADCAST_HANDOFF_SOURCE_PREFIX + ":" + stage + ":" + actionKey;
    }

    private boolean isMaintenanceBroadcastHandoffSource(String source, String requiredSuffix) {
        if (source == null || !source.startsWith(MAINTENANCE_BROADCAST_HANDOFF_SOURCE_PREFIX)) {
            return false;
        }
        return requiredSuffix == null || source.endsWith(":" + requiredSuffix);
    }

    private XiuluoRoundContext compensatePreCombatTimerAfterMaintenance(XiuluoRoundContext state,
                                                                        long blockedMs,
                                                                        String source) {
        if (state == null) {
            return null;
        }
        XiuluoRoundContext adjusted = state.pausePreCombatTimer(blockedMs, source);
        if (adjusted.preCombatStartedAtMs() != state.preCombatStartedAtMs()) {
            log.info("[xiuluo-v2] pre-combat timer paused: source={} blockedMs={} adjustedStartAt={}",
                    source, blockedMs, adjusted.preCombatStartedAtMs());
        }
        return adjusted;
    }

    /**
     * Resolve the task-level 修罗 hot-start entry without running window startup preparation.
     *
     * @param context current task execution context; used for stop/pause checkpoints and existing
     *                task actions that may consume dialog/return-item input.
     * @param roundContext current round state; CR160 resume fallback must pass the live instance so
     *                     saved {@code objective} / {@code objectiveParseFuture} can be reused.
     * @param source diagnostic source attached to logs and returned state.
     * @return the phase state selected by current screen facts, using the fixed CR159 order:
     * combat, 修罗 enter-battle dialog, tracker, return item, saved objective, then reaccept.
     */
    public XiuluoRoundContext resolveTaskHotStart(TaskExecutionContext context,
                                                  XiuluoRoundContext roundContext,
                                                  String source) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        if (isInCombatForTaskHotStart(context, source)) {
            log.info("[xiuluo-v2] task hot-start in combat; resume WAIT_COMBAT: source={} round={} window={}",
                    source, roundContext.round(), currentWindowLabel());
            return roundContext.next(XiuluoPhase.WAIT_COMBAT, source + "-in-combat");
        }

        XiuluoRoundContext dialogState = tryConsumeHotStartEnterBattleDialog(context, roundContext, source);
        if (dialogState != null) {
            return dialogState;
        }

        TaskTrackerPanelReadResult panel = taskTrackerPanelService.readXiuluoTrackerPanel(
                "xiuluo-v2:" + source);
        if (panel.isFound() && !panel.getGreenLinks().isEmpty()) {
            CompletableFuture<Optional<NpcTarget>> objectiveFuture =
                    CompletableFuture.completedFuture(Optional.empty());
            CompletableFuture<TaskTrackerPanelReadResult> trackerFuture =
                    CompletableFuture.completedFuture(panel);
            startupIncensePending = true;
            log.info("[xiuluo-v2] startup tracker hit: source={} links={} detail={}",
                    source, panel.getGreenLinks().size(), panel.getDetailRawPath());
            return roundContext.withAcceptParseFutures(XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK,
                    objectiveFuture, trackerFuture, source + "-tracker-active");
        }
        log.info("[xiuluo-v2] startup tracker missed; try return item before accept flow: source={} found={} links={}",
                source, panel.isFound(), panel.getGreenLinks().size());
        if (tryUseStartupReturnItemOnce(context, source)) {
            return roundContext.next(XiuluoPhase.WAIT_TEAM_RETURN, source + "-return-verified");
        }

        XiuluoRoundContext savedObjectiveState = resumeHotStartFromSavedObjective(context, roundContext, source);
        if (savedObjectiveState != null) {
            return savedObjectiveState;
        }

        uiCleanerService.cleanUpAll();
        return roundContext.recoverTo(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC,
                source + "-return-unverified");
    }

    private XiuluoRoundContext resolvePauseResumeTaskHotStart(TaskExecutionContext context,
                                                              XiuluoRoundContext state,
                                                              TaskPauseResumeReconcileResult reconcile,
                                                              String boundary) {
        startupIncensePending = false;
        startupIncenseChecked = false;
        // CR245: a pause-resume hot start abandons any in-flight broadcast queue wait; the stale
        // flag must not route the next maintenance hook straight to a blind self-confirm.
        pendingMaintenanceQueueHook = null;
        maintenanceSelfConfirmProbeFuture = null;
        log.warn("[xiuluo-v2 pause-resume] fallback task hot-start: phase={} boundary={} pauseBlockedMs={} fingerprintMatched={} mismatchReason={} compensatedTimers={} clearedVolatileState={} fallbackTaskHotStart={}",
                state.phase(), boundary, reconcile.getPauseBlockedMs(), reconcile.isFingerprintMatched(),
                reconcile.getMismatchReason(), reconcile.getCompensatedTimers(), reconcile.getClearedVolatileState(),
                reconcile.isFallbackTaskHotStart());
        return resolveTaskHotStart(context, state, "pause-resume-hot-start:" + boundary);
    }

    private boolean isInCombatForTaskHotStart(TaskExecutionContext context, String source) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        boolean radarCombat = battleRadarService.checkAndSyncCombatState();
        boolean inCombat = radarCombat && gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT;
        log.info("[xiuluo-v2] task hot-start combat probe: source={} radarCombat={} actionState={} inCombat={}",
                source, radarCombat, gameContext.getCurrentActionState(), inCombat);
        return inCombat;
    }

    private XiuluoRoundContext tryConsumeHotStartEnterBattleDialog(TaskExecutionContext context,
                                                                  XiuluoRoundContext state,
                                                                  String source) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
                "xiuluo-v2:hot-start-enter-battle:" + source,
                List.of(new GreenTemplateClickSpec(OPTION_ENTER_BATTLE, ENTER_BATTLE_TEMPLATE, -6, 6, 4)),
                true));
        if (!OPTION_ENTER_BATTLE.equals(result.getActionKey())) {
            log.info("[xiuluo-v2] task hot-start enter-battle dialog miss: source={} status={} dialogType={} action={}",
                    source, result.getStatus(), result.getDialogType(), result.getActionKey());
            return null;
        }
        autoCombatService.initializeForCurrentWindow();
        autoCombatService.authorizeCombatDetectionAfterEnterBattleAction(
                "xiuluo-v2:hot-start-enter-battle-clicked:" + source);
        TaskSleep.sleepOrStop(context, 1200L, "Xiuluo V2 task interrupted");
        log.info("[xiuluo-v2] task hot-start enter-battle dialog consumed: source={} click=({}, {})",
                source, result.getAbsoluteX(), result.getAbsoluteY());
        return state.withPendingEnterBattleConfirm(XiuluoPhase.WAIT_COMBAT, XiuluoCombatSource.TRACKER_CONFIRM,
                source + "-enter-battle-dialog-consumed");
    }

    private XiuluoRoundContext resumeHotStartFromSavedObjective(TaskExecutionContext context,
                                                               XiuluoRoundContext state,
                                                               String source) {
        if (state.objective() != null) {
            log.info("[xiuluo-v2] task hot-start saved objective hit after tracker/return miss: source={} target={}",
                    source, state.objective());
            return state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, state.objective(),
                    source + "-saved-objective");
        }
        CompletableFuture<Optional<NpcTarget>> parseFuture = state.objectiveParseFuture();
        if (parseFuture == null) {
            log.info("[xiuluo-v2] task hot-start saved objective miss: source={} reason=no-objective-future",
                    source);
            return null;
        }
        Optional<NpcTarget> parsed = waitForBackgroundObjectiveResult(context, parseFuture, state);
        if (parsed.isEmpty()) {
            log.warn("[xiuluo-v2] task hot-start saved objective future failed or empty: source={}", source);
            return null;
        }
        log.info("[xiuluo-v2] task hot-start saved objective future hit after tracker/return miss: source={} target={}",
                source, parsed.get());
        return state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, parsed.get(),
                source + "-saved-objective-future");
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
            case TRY_TRACKER_SHORTCUT -> tryTrackerShortcut(context, state);
            case WAIT_TRACKER_SHORTCUT_PATHING -> waitTrackerShortcutPathing(context, state);
            case NAVIGATE_TO_TARGET -> navigateToTarget(context, state);
            case CLICK_TARGET_NPC -> clickTargetNpc(context, state);
            case CONFIRM_ENTER_BATTLE -> confirmEnterBattle(context, state);
            case WAIT_COMBAT -> waitCombat(context, state);
            case RETURN_HOME -> returnHome(context, state);
            case NAVIGATE_BACK_TO_START -> navigateBackToStart(context, state);
            case WAIT_TEAM_READY -> waitTeamReady(context, state);
            case WAIT_TEAM_RETURN -> waitTeamReturn(context, state);
            case ROUND_DONE, FAILED, STOPPED ->
                    XiuluoStepOutcome.failed(state, "terminal phase should not be executed: " + state.phase());
        };
    }

    // 🟢 PHASE HANDLERS: each method owns exactly one Xiuluo phase.
    private XiuluoStepOutcome prepareRound(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * CR244 review P2: no pending-return gate here. The card defines exactly two leader gates —
         * Gate A after the return-home landing (WAIT_TEAM_RETURN) and Gate B right before the
         * accept-option atomic click. A third read here would park before NPC smart, shrink the
         * natural wait interval between the two gates, and add one extra cloud handoff per round.
         */
        if (state.round() == 1) {
            /*
             * Only startup/hot-start needs a broad UI sweep here. Later phase failures already own
             * their cleanup paths, so repeating cleanUpAll after every successful return-home makes
             * the leader visibly idle in town before the next accept flow.
             */
            log.info("[xiuluo-v2] prepare round: clean UI round={}", state.round());
            uiCleanerService.cleanUpAll();
        } else {
            log.info("[xiuluo-v2] prepare round: skip clean UI round={} reason=phase-fallbacks-own-cleanup",
                    state.round());
        }
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
         * Starting near the target task NPC should not force a minimap detour. Try the normal smart-click
         * pipeline first; if it cannot verify the accept dialog, fall back to the randomized minimap
         * navigation path below.
         */
        PlayerCharacter me = gameContext.getMe();
        if (me != null && gameStateUtil.isNearCoordinate(me.getCurrentMapName(), me.getX(), me.getY(),
                ACCEPT_NPC.getMapName(), ACCEPT_NPC.getX(), ACCEPT_NPC.getY(), TASK_NPC_DIRECT_CLICK_DISTANCE)) {
            log.info("[xiuluo-v2] task NPC nearby; try direct smart click before minimap navigation: npc={} playerMap={} player=({}, {}) targetMap={} target=({}, {}) tolerance={}",
                    ACCEPT_NPC.getName(),
                    me.getCurrentMapName(), me.getX(), me.getY(),
                    ACCEPT_NPC.getMapName(), ACCEPT_NPC.getX(), ACCEPT_NPC.getY(), TASK_NPC_DIRECT_CLICK_DISTANCE);
            consumeCommonBoxDuringNextTaskProgress(context, "xiuluo-v2:before-accept-npc-click");
            if (npcClickService.clickNpcSmart(ACCEPT_NPC.toClickRequest(me, TaskType.XIULUO_V2))) {
                return XiuluoStepOutcome.continueTo(
                        activeState.next(XiuluoPhase.ACCEPT_TASK_DIALOG, "nearby-accept-npc-clicked"),
                        "accept NPC clicked from nearby position");
            }
            log.info("[xiuluo-v2] nearby task NPC direct click failed; fallback to minimap navigation: npc={}",
                    ACCEPT_NPC.getName());
        }
        // 🧭 ACCEPT NPC NAV: go to the fixed task giver before opening/handling its dialog.
        // The return-home verification is the task-owned accept snapshot. It stays valid across
        // navigation retries and cloud bookkeeping until the accept option is actually clicked.
        LocationInfo verifiedStart = lastStartMapVerifiedLocation;
        long verifiedStartAtMs = lastStartMapVerifiedAtMs;
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(ACCEPT_NPC.getMapName())
                .targetX(ACCEPT_NPC.getX())
                .targetY(ACCEPT_NPC.getY())
                .targetName(ACCEPT_NPC.getName())
                .keepTurnOnCurrentMapPathing(true)
                .source("xiuluo-v2:acceptNpc")
                .freshCurrentMapName(verifiedStart == null ? null : verifiedStart.mapName)
                .freshCurrentX(verifiedStart == null ? null : verifiedStart.x)
                .freshCurrentY(verifiedStart == null ? null : verifiedStart.y)
                .freshCurrentLocationAtMs(verifiedStart == null ? 0L : verifiedStartAtMs)
                .freshCurrentLocationPhaseBound(verifiedStart != null)
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
        PlayerCharacter me = gameContext.getMe();
        if (me == null || !gameStateUtil.isNearCoordinate(me.getCurrentMapName(), me.getX(), me.getY(),
                ACCEPT_NPC.getMapName(), ACCEPT_NPC.getX(), ACCEPT_NPC.getY(), TASK_NPC_DIRECT_CLICK_DISTANCE)) {
            log.warn("[xiuluo-v2] skip task NPC smart click: player is not near target; npc={} playerMap={} player=({}, {}) targetMap={} target=({}, {}) tolerance={} source={}",
                    ACCEPT_NPC.getName(),
                    me == null ? null : me.getCurrentMapName(),
                    me == null ? null : me.getX(),
                    me == null ? null : me.getY(),
                    ACCEPT_NPC.getMapName(), ACCEPT_NPC.getX(), ACCEPT_NPC.getY(),
                    TASK_NPC_DIRECT_CLICK_DISTANCE, state.source());
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "accept-click-not-near-npc"),
                    "task NPC not nearby; navigate before smart click");
        }
        consumeCommonBoxDuringNextTaskProgress(context, "xiuluo-v2:before-accept-npc-click");
        /*
         * CR255: ONLY this accept-phase smart click consumes Runner-confirmed STORY_DIALOG_VISIBLE
         * facts — at the FIFO natural boundary, one fast story click per fresh event sequence,
         * then the smart session restarts. Every other smart click keeps the default (no story
         * event behavior, no added detection).
         */
        boolean clicked = npcClickService.clickNpcSmart(ACCEPT_NPC.toClickRequest(me, TaskType.XIULUO_V2)
                .toBuilder()
                .consumeStoryDialogVisibleEvents(true)
                .build());
        if (!clicked) {
            return recoverAcceptNpcClickFailure(context, state);
        }
        XiuluoRoundContext acceptDialogState = state.next(XiuluoPhase.ACCEPT_TASK_DIALOG, "accept-npc-clicked");
        scheduleAcceptDialogCloudFallback(acceptDialogState, "accept-npc-clicked");
        return XiuluoStepOutcome.continueTo(
                acceptDialogState,
                "accept NPC clicked");
    }

    private XiuluoStepOutcome acceptTaskDialog(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        scheduleAcceptDialogCloudFallbackIfAbsent(state, "accept-dialog-entry");
        Optional<XiuluoStepOutcome> knownDialog = handleKnownXiuluoOptionDialog(
                context, state, "xiuluo-v2:accept:" + state.source(), false);
        if (knownDialog.isEmpty()) {
            DialogResult businessDialog = dialogService.handleDialog(DialogHandleRequest.handleBusinessOption(
                    "xiuluo-v2:accept-unmatched-dialog:" + state.source()));
            if (businessDialog.getStatus() == DialogResultStatus.INTERRUPTED) {
                return XiuluoStepOutcome.stopped(state, "accept dialog handler interrupted");
            }
            if (businessDialog.getStatus() == DialogResultStatus.BUSINESS_OPTION_CLICKED) {
                log.info("[xiuluo-v2] accept dialog delegated to DialogService business handler: actionKey={} status={}",
                        businessDialog.getActionKey(), businessDialog.getStatus());
                if (BUSINESS_ACTION_HEAL_PET.equals(businessDialog.getActionKey())
                        || BUSINESS_ACTION_REPAIR_EQUIPMENT.equals(businessDialog.getActionKey())) {
                    return XiuluoStepOutcome.sharedState(
                            state.next(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC,
                                    maintenanceBroadcastHandoffSource("accept", businessDialog.getActionKey())),
                            "accept maintenance broadcast handled; yield before retry accept navigation");
                }
                return XiuluoStepOutcome.continueTo(
                        state.next(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "accept-unmatched-business-dialog-handled"),
                        "accept unmatched business dialog handled; retry accept navigation");
            }
            return recoverAcceptDialogFailure(state);
        }
        return knownDialog.get();
    }

    private XiuluoStepOutcome readObjective(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        CompletableFuture<Optional<NpcTarget>> parseFuture = state.objectiveParseFuture();
        if (parseFuture == null) {
            log.warn("[xiuluo-v2] READ_OBJECTIVE missing accept-time background result; synchronous fallback skipped because CR56 owns the background result: round={} source={} window={}",
                    state.round(), state.source(), currentWindowLabel());
            return recoverBackgroundObjectiveReadFailure(context, state, "objective background future missing");
        }

        log.info("[xiuluo-v2] READ_OBJECTIVE waiting for background objective result: round={} source={} done={} window={}",
                state.round(), state.source(), parseFuture.isDone(), currentWindowLabel());
        Optional<NpcTarget> storyObjective = waitForBackgroundObjectiveResult(context, parseFuture, state);
        if (storyObjective.isPresent()) {
            XiuluoPhase nextPhase = state.startExitPrepathStarted()
                    ? XiuluoPhase.NAVIGATE_TO_TARGET
                    : XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK;
            String nextSource = state.startExitPrepathStarted()
                    ? "objective:bg:after-start-exit-prepath"
                    : "objective:bg";
            return XiuluoStepOutcome.continueTo(
                    state.withObjective(nextPhase, storyObjective.get(), nextSource),
                    "objective parsed from accept-time background snapshot");
        }

        if (!parseFuture.isDone()
                && remainingPreCombatWatchdogBudgetMs(state, System.currentTimeMillis()) <= 0L) {
            // Budget exhaustion is a watchdog timeout, not a parse failure; the fact lets the
            // brain restart the round instead of walking the read.objective recovery chain.
            return preCombatWatchdogTimeoutOutcome(state, "objective-parse", "budget-exhausted");
        }
        log.warn("[xiuluo-v2] background objective parse failed; synchronous fallback skipped because CR56 owns the background result: round={} source={} window={}",
                state.round(), state.source(), currentWindowLabel());
        return recoverBackgroundObjectiveReadFailure(context, state, "objective background parse failed");
    }

    private XiuluoStepOutcome afterAcceptMaintenanceCheck(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        if (!isHealPetMaintenanceDue() && !isRepairEquipmentMaintenanceDue()) {
            log.info("[xiuluo-v2] accept maintenance not due; start exit prepath before consuming accept tracker snapshot: round={} source={}",
                    state.round(), state.source());
            XiuluoStepOutcome prepath = startLeavingStartMapIfPresent(
                    context,
                    state, XiuluoPhase.TRY_TRACKER_SHORTCUT,
                    "after-accept-no-maintenance-start-exit-prepath");
            if (prepath != null) {
                return attachAcceptObjectiveBackgroundParseAfterStartExitPrepath(prepath);
            }
            return XiuluoStepOutcome.continueTo(
                    scheduleAcceptObjectiveBackgroundParse(state, "after-accept-no-maintenance-no-prepath")
                            .next(XiuluoPhase.TRY_TRACKER_SHORTCUT, "after-accept-no-maintenance-no-prepath"),
                    "after-accept maintenance not due; consume accept tracker snapshot");
        }

        /*
         * CR191 keeps the no-maintenance shortcut focused on prepath + tracker green first. When
         * true heal/repair maintenance is due, preserve the CR120 early box window before that
         * maintenance spends the pending TTL.
         */
        consumeCommonBoxDuringNextTaskProgress(context, "xiuluo-v2:after-accept-maintenance-check");

        /*
         * First maintenance hook: the leader is still in 灵兽村 after accepting 修罗, so this is the
         * cheapest place to trigger 医宝宝 from 超级巫医. The actual broadcast option is still handled
         * by the shared maintenance service so team members and later tasks use one dialog path.
         */
        XiuluoStepOutcome healPetOutcome = triggerHealPetBroadcastAfterAccept(context, state);
        if (healPetOutcome != null) {
            return healPetOutcome;
        }

        long maintenanceStartedAt = System.currentTimeMillis();
        TaskMaintenanceResult maintenanceResult = taskMaintenanceService.runOpportunisticMaintenance(context,
                TaskMaintenanceRequest.builder()
                        .sourceTask("xiuluo-v2:after-accept")
                        .handleMaintenanceBroadcast(true)
                        .cleanSummonSkill(false)
                        .build());
        XiuluoRoundContext adjustedState = compensatePreCombatTimerAfterMaintenance(
                state, System.currentTimeMillis() - maintenanceStartedAt, "xiuluo-v2:after-accept");
        if (maintenanceResult.getStatus() == TaskMaintenanceStatus.INTERRUPTED) {
            return XiuluoStepOutcome.stopped(adjustedState, "after-accept maintenance interrupted");
        }
        log.info("[xiuluo-v2] after-accept maintenance checked: status={} message={} objective={}",
                maintenanceResult.getStatus(), maintenanceResult.getMessage(), adjustedState.objective());
        return XiuluoStepOutcome.continueTo(
                adjustedState.next(XiuluoPhase.BEFORE_ROUTE_MAINTENANCE_CHECK, "after-accept-maintenance-checked"),
                "after-accept maintenance checked");
    }

    private XiuluoStepOutcome triggerHealPetBroadcastAfterAccept(TaskExecutionContext context,
                                                                 XiuluoRoundContext state) {
        if (!isHealPetMaintenanceDue()) {
            log.info("[xiuluo-v2] skip heal-pet hook: cooldown not due intervalMs={} lastAt={}",
                    botProperties.getXiuluoHealPetMaintenanceIntervalMs(), lastHealPetMaintenanceAt);
            return null;
        }

        PlayerCharacter me = gameContext.getMe();
        if (me == null || !START_MAP_NAME.equals(me.getCurrentMapName())) {
            log.info("[xiuluo-v2] skip heal-pet hook: currentMap={} expected={}",
                    me == null ? null : me.getCurrentMapName(), START_MAP_NAME);
            return null;
        }

        XiuluoRoundContext activeState = state;
        for (int attempt = 1; attempt <= MAX_MAINTENANCE_HOOK_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            XiuluoStepOutcome pendingPathing = continueIfNavigationStillPathing(context, activeState,
                    "navigate to heal-pet NPC");
            if (pendingPathing != null) {
                return pendingPathing;
            }
            if (activeState.waitingPathing()) {
                activeState = activeState.clearPathingWait("heal-pet-navigation-arrived:" + state.phase());
            }
            MaintenanceAttemptResult attemptResult = runMaintenanceBroadcastAttempt(context, activeState,
                    HEAL_PET_NPC, "heal-pet", "xiuluo-v2:healPetNpc", "xiuluo-v2:heal-pet-npc");
            if (attemptResult.outcome() != null) {
                return attemptResult.outcome();
            }
            if (attemptResult.handled()) {
                lastHealPetMaintenanceAt = System.currentTimeMillis();
                activeState = compensatePreCombatTimerAfterMaintenance(
                        activeState, attemptResult.blockedMs(), "xiuluo-v2:heal-pet-broadcast");
                return XiuluoStepOutcome.sharedState(
                        activeState.next(XiuluoPhase.BEFORE_ROUTE_MAINTENANCE_CHECK,
                                maintenanceBroadcastHandoffSource("heal-pet", "handled")),
                        "heal-pet maintenance broadcast handled; yield for team");
            }
            cleanupAndLogMaintenanceRetry("heal-pet", HEAL_PET_NPC, attempt);
            activeState = activeState.clearPathingWait("heal-pet-navigation-retry:" + state.phase());
        }
        log.warn("[xiuluo-v2] heal-pet hook skipped after {} attempts; continue main task",
                MAX_MAINTENANCE_HOOK_ATTEMPTS);
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

    private XiuluoStepOutcome beforeRouteMaintenanceCheck(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * Second maintenance hook: repair equipment is a longer detour, so it runs only when its
         * configurable cooldown is due. The task objective stays in XiuluoRoundContext and resumes
         * after the broadcast dialog is handled.
         */
        XiuluoStepOutcome repairOutcome = triggerRepairEquipmentBroadcastBeforeRoute(context, state);
        if (repairOutcome != null) {
            return repairOutcome;
        }

        /*
         * Keep this pass lightweight for now: team broadcast dialogs are safe to handle, while
         * summon-skill cleanup waits for a dedicated safe/yielded maintenance window.
         */
        long maintenanceStartedAt = System.currentTimeMillis();
        TaskMaintenanceResult maintenanceResult = taskMaintenanceService.runOpportunisticMaintenance(context,
                TaskMaintenanceRequest.builder()
                        .sourceTask("xiuluo-v2:before-route")
                        .handleMaintenanceBroadcast(true)
                        .cleanSummonSkill(false)
                        .build());
        XiuluoRoundContext adjustedState = compensatePreCombatTimerAfterMaintenance(
                state, System.currentTimeMillis() - maintenanceStartedAt, "xiuluo-v2:before-route");
        if (maintenanceResult.getStatus() == TaskMaintenanceStatus.INTERRUPTED) {
            return XiuluoStepOutcome.stopped(adjustedState, "before-route maintenance interrupted");
        }
        log.info("[xiuluo-v2] before-route maintenance checked: status={} message={} objective={}",
                maintenanceResult.getStatus(), maintenanceResult.getMessage(), adjustedState.objective());
        return XiuluoStepOutcome.continueTo(
                adjustedState.clearShortcutTrackerParseFuture(
                        XiuluoPhase.TRY_TRACKER_SHORTCUT, "before-route-maintenance-checked"),
                "before-route maintenance checked; fresh-read tracker shortcut");
    }

    private XiuluoStepOutcome triggerRepairEquipmentBroadcastBeforeRoute(TaskExecutionContext context,
                                                                         XiuluoRoundContext state) {
        if (REPAIR_EQUIPMENT_DONE_SOURCE.equals(state.source())
                || isMaintenanceBroadcastHandoffSource(state.source(), REPAIR_EQUIPMENT_DONE_SOURCE)) {
            return null;
        }
        if (!isRepairEquipmentMaintenanceDue()) {
            log.info("[xiuluo-v2] skip repair-equipment hook: cooldown not due intervalMs={} lastAt={}",
                    botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs(), lastRepairEquipmentMaintenanceAt);
            return null;
        }

        XiuluoRoundContext activeState = state;
        for (int attempt = 1; attempt <= MAX_MAINTENANCE_HOOK_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            XiuluoStepOutcome pendingPathing = continueIfNavigationStillPathing(context, activeState,
                    "navigate to repair-equipment NPC");
            if (pendingPathing != null) {
                return pendingPathing;
            }
            if (activeState.waitingPathing()) {
                activeState = activeState.clearPathingWait("repair-equipment-navigation-arrived:" + state.phase());
            }
            MaintenanceAttemptResult attemptResult = runMaintenanceBroadcastAttempt(context, activeState,
                    REPAIR_EQUIPMENT_NPC, "repair-equipment", "xiuluo-v2:repairEquipmentNpc",
                    "xiuluo-v2:repair-equipment-npc");
            if (attemptResult.outcome() != null) {
                return attemptResult.outcome();
            }
            if (attemptResult.handled()) {
                lastRepairEquipmentMaintenanceAt = System.currentTimeMillis();
                log.info("[xiuluo-v2] repair-equipment hook handled; cooldown reset");
                activeState = compensatePreCombatTimerAfterMaintenance(
                        activeState, attemptResult.blockedMs(), "xiuluo-v2:repair-equipment-broadcast");
                return XiuluoStepOutcome.sharedState(
                        activeState.next(XiuluoPhase.BEFORE_ROUTE_MAINTENANCE_CHECK,
                                maintenanceBroadcastHandoffSource("repair-equipment", REPAIR_EQUIPMENT_DONE_SOURCE)),
                        "repair-equipment maintenance broadcast handled; yield for team");
            }
            cleanupAndLogMaintenanceRetry("repair-equipment", REPAIR_EQUIPMENT_NPC, attempt);
            activeState = activeState.clearPathingWait("repair-equipment-navigation-retry:" + state.phase());
        }
        log.warn("[xiuluo-v2] repair-equipment hook skipped after {} attempts; continue main task",
                MAX_MAINTENANCE_HOOK_ATTEMPTS);
        return XiuluoStepOutcome.continueTo(
                activeState.next(XiuluoPhase.BEFORE_ROUTE_MAINTENANCE_CHECK, REPAIR_EQUIPMENT_DONE_SOURCE),
                "repair-equipment maintenance checked");
    }

    private boolean isRepairEquipmentMaintenanceDue() {
        long intervalMs = botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs();
        if (intervalMs <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        return lastRepairEquipmentMaintenanceAt <= 0 || now - lastRepairEquipmentMaintenanceAt >= intervalMs;
    }

    private MaintenanceAttemptResult runMaintenanceBroadcastAttempt(TaskExecutionContext context,
                                                                    XiuluoRoundContext state,
                                                                    NpcTarget npc,
                                                                    String hookName,
                                                                    String navigationSource,
                                                                    String broadcastSource) {
        if (hookName.equals(pendingMaintenanceQueueHook)) {
            /*
             * CR245 re-entry while this hook's broadcast queue drains: never navigate or click the
             * NPC again. A stale flag (round restarted long ago; probe far beyond its TTL) falls
             * through to a normal fresh attempt instead of blindly self-confirming at a wrong spot.
             */
            /*
             * CR245 review P2: capture the event-bus sequence BEFORE reading the drained flag (same
             * rule as the CR244 Gate A read). A member dequeue between the read and the park would
             * otherwise carry a sequence at or below the park's afterSequence and the leader would
             * sleep through the full queue cap instead of waking on it.
             */
            long queueAfterSequence = windowReadyEventBus.currentSequence();
            if (!taskMaintenanceService.isMaintenanceBroadcastQueueDrained(context, broadcastSource)) {
                return parkForMaintenanceBroadcastQueue(state, hookName, queueAfterSequence);
            }
            long probeAgeMs = Math.max(0L, System.currentTimeMillis() - maintenanceSelfConfirmProbeStartedAtMs);
            if (probeAgeMs <= MAINTENANCE_SELF_CONFIRM_PROBE_TTL_MS * 2) {
                return finishMaintenanceSelfConfirm(context, state, hookName, broadcastSource);
            }
            log.warn("[xiuluo-v2] {} hook stale broadcast-queue flag discarded; restart normal attempt probeAgeMs={}",
                    hookName, probeAgeMs);
            pendingMaintenanceQueueHook = null;
            maintenanceSelfConfirmProbeFuture = null;
        }
        boolean cleanupBeforeNavigation = gameStateUtil.isSameMapName(npc.getMapName(), START_MAP_NAME);
        if (cleanupBeforeNavigation) {
            log.info("[xiuluo-v2] {} hook pre-NPC cleanup started: npc={} timing=before-navigation",
                    hookName, npc.getName());
            uiCleanerService.cleanUpAll();
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        }

        Point randomizedTarget = coordinateHelper.getRandomizedPoint(npc.getX(), npc.getY(), 1, 1);
        NavigationResult navigationResult = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(npc.getMapName())
                .targetX(randomizedTarget.x)
                .targetY(randomizedTarget.y)
                .targetName(npc.getName())
                .keepTurnOnCurrentMapPathing(BUSINESS_ACTION_HEAL_PET.equals(hookName))
                .source(navigationSource)
                .build());
        NavigationResultStatus navigationStatus = navigationResult.getStatus();
        log.info("[xiuluo-v2] {} NPC navigation result: npc={} target=({}, {}) randomized=({}, {}) status={} message={}",
                hookName, npc.getName(), npc.getX(), npc.getY(), randomizedTarget.x, randomizedTarget.y,
                navigationStatus, navigationResult.getMessage());
        if (navigationStatus == NavigationResultStatus.PATHING_STARTED) {
            return MaintenanceAttemptResult.withOutcome(XiuluoStepOutcome.pathingStarted(
                    state.waitForPathing("pathing:" + state.phase()), hookName + " NPC pathing started"));
        }
        if (navigationStatus == NavigationResultStatus.DIALOG_PREPARING) {
            /*
             * Route-transfer dialogs may already be owned by the window watcher. Do not treat this
             * as a failed maintenance attempt; yield the hook phase so the prepared click can be
             * consumed on the next turn without cleanup closing the dialog.
             */
            return MaintenanceAttemptResult.withOutcome(XiuluoStepOutcome.sharedState(
                    state.retrySamePhase(hookName + "-dialog-preparing"),
                    hookName + " NPC route dialog preparing"));
        }
        if (navigationStatus == NavigationResultStatus.STOPPED) {
            return MaintenanceAttemptResult.withOutcome(
                    XiuluoStepOutcome.stopped(state, hookName + " NPC navigation stopped"));
        }
        if (navigationStatus == NavigationResultStatus.POINT_NOT_REACHED) {
            log.info("[xiuluo-v2] {} hook NPC point not reached; retry without cleanup: npc={} message={}",
                    hookName, npc.getName(), navigationResult.getMessage());
            return MaintenanceAttemptResult.withOutcome(XiuluoStepOutcome.sharedState(
                    state.retrySamePhase(hookName + "-point-not-reached"),
                    hookName + " NPC point not reached; retry next turn"));
        }
        if (navigationStatus == NavigationResultStatus.DIALOG_OPENED) {
            log.info("[xiuluo-v2] {} hook NPC navigation opened dialog; continue broadcast handling: npc={} message={}",
                    hookName, npc.getName(), navigationResult.getMessage());
        } else if (navigationStatus != NavigationResultStatus.ARRIVED
                && navigationStatus != NavigationResultStatus.SUCCESS) {
            log.warn("[xiuluo-v2] {} hook attempt failed: navigation status={} message={}",
                    hookName, navigationStatus, navigationResult.getMessage());
            return MaintenanceAttemptResult.retry();
        }

        if (!cleanupBeforeNavigation) {
            /*
             * Cross-map maintenance can open route/arrival panels after the first cleanup. Clean only
             * after the NPC coordinate is reached so the next click sees the final local screen.
             */
            log.info("[xiuluo-v2] {} hook pre-NPC cleanup started: npc={} timing=after-arrival",
                    hookName, npc.getName());
            uiCleanerService.cleanUpAll();
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        }

        boolean clicked = npcClickService.clickNpcSmart(npc.toClickRequest(gameContext.getMe(), TaskType.XIULUO_V2));
        if (!clicked) {
            log.warn("[xiuluo-v2] {} hook attempt failed: NPC click failed npc={}", hookName, npc);
            return MaintenanceAttemptResult.retry();
        }

        /*
         * CR245: the verified NPC hit is what fires the team broadcast (user-confirmed business
         * fact, 2026-07-10). The leader's own confirm is the SECOND click and now runs LAST: freeze
         * the confirmed-member FIFO, yield immediately so members consume their confirms, and click
         * our own from the background-probed point after the queue drains or the 5s cap releases us.
         */
        // CR245 review P2: capture the sequence BEFORE opening the queue so a member confirm that
        // fires between the open and the park still wakes the leader.
        long queueAfterSequence = windowReadyEventBus.currentSequence();
        int queuedMembers = taskMaintenanceService.openMaintenanceBroadcastQueue(
                context, hookName, MAINTENANCE_BROADCAST_QUEUE_CAP_MS, broadcastSource);
        if (queuedMembers <= 0) {
            return finishNoLocalMemberMaintenanceBroadcast(context, state, hookName, broadcastSource);
        }
        pendingMaintenanceQueueHook = hookName;
        scheduleMaintenanceSelfConfirmProbe(hookName, broadcastSource);
        log.info("[xiuluo-v2] {} hook broadcast queue opened; yield for member confirms: members={} capMs={}",
                hookName, queuedMembers, MAINTENANCE_BROADCAST_QUEUE_CAP_MS);
        return parkForMaintenanceBroadcastQueue(state, hookName, queueAfterSequence);
    }

    private MaintenanceAttemptResult parkForMaintenanceBroadcastQueue(XiuluoRoundContext state,
                                                                      String hookName,
                                                                      long afterSequence) {
        return MaintenanceAttemptResult.withOutcome(XiuluoStepOutcome.sharedState(
                        state.retrySamePhase(hookName + "-broadcast-queue-waiting"),
                        hookName + " broadcast queue draining; park for member attempts")
                .withFact("maintenanceQueueWaitSpecArmed", "true")
                .withFact("maintenanceQueueHook", hookName)
                .withWaitSpec(XiuluoWaitSpec.builder()
                        .reason(XiuluoWaitReason.WAIT_MAINTENANCE_BROADCAST_QUEUE)
                        .wakeTypes(Set.of(WindowReadyEventType.MAINTENANCE_BROADCAST_QUEUE_CHANGED))
                        .afterSequence(afterSequence)
                        .timeoutMs(MAINTENANCE_BROADCAST_QUEUE_CAP_MS)
                        .build()));
    }

    /**
     * CR245 no-local-member path: no queue is created. Click our own confirm immediately, then hold
     * a fixed courtesy window for out-of-process human teammates (there is no fact to wait on for
     * windows this program does not control), then continue.
     */
    private MaintenanceAttemptResult finishNoLocalMemberMaintenanceBroadcast(TaskExecutionContext context,
                                                                             XiuluoRoundContext state,
                                                                             String hookName,
                                                                             String broadcastSource) {
        long startedAt = System.currentTimeMillis();
        TaskMaintenanceResult maintenanceResult = taskMaintenanceService.runOpportunisticMaintenance(context,
                TaskMaintenanceRequest.builder()
                        .sourceTask(broadcastSource + ":no-local-member")
                        .handleMaintenanceBroadcast(true)
                        .cleanSummonSkill(false)
                        .build());
        if (maintenanceResult.getStatus() == TaskMaintenanceStatus.INTERRUPTED) {
            return MaintenanceAttemptResult.withOutcome(
                    XiuluoStepOutcome.stopped(state, hookName + " broadcast interrupted"));
        }
        if (!maintenanceResult.isBroadcastHandled()) {
            log.warn("[xiuluo-v2] {} hook attempt failed: broadcast not handled status={} message={}",
                    hookName, maintenanceResult.getStatus(), maintenanceResult.getMessage());
            return MaintenanceAttemptResult.retry();
        }
        log.info("[xiuluo-v2] {} hook self confirm done without local members; courtesy wait {} ms for external teammates",
                hookName, MAINTENANCE_NO_LOCAL_MEMBER_COURTESY_WAIT_MS);
        TaskSleep.sleepOrStop(context, MAINTENANCE_NO_LOCAL_MEMBER_COURTESY_WAIT_MS,
                "Xiuluo V2 task interrupted");
        return MaintenanceAttemptResult.handledResult(System.currentTimeMillis() - startedAt);
    }

    private void scheduleMaintenanceSelfConfirmProbe(String hookName, String broadcastSource) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        maintenanceSelfConfirmProbeStartedAtMs = System.currentTimeMillis();
        maintenanceSelfConfirmProbeFuture = CompletableFuture.supplyAsync(() ->
                windowTaskContextHolder.callWith(runtime, () ->
                        taskMaintenanceService.probeMaintenanceBroadcastPointForCurrentWindow(
                                broadcastSource + ":self-confirm-probe:" + hookName)));
    }

    /**
     * CR245 leader's own confirm after the queue drained (or the cap released us): consume the
     * background-probed point when fresh, otherwise one live ROI rescan. One prepared click plus
     * one rescan is the whole budget — a missing own confirm is tolerated and the next cooldown
     * round re-triggers; the queue attempt itself already counts as this round's maintenance.
     */
    private MaintenanceAttemptResult finishMaintenanceSelfConfirm(TaskExecutionContext context,
                                                                  XiuluoRoundContext state,
                                                                  String hookName,
                                                                  String broadcastSource) {
        long startedAt = System.currentTimeMillis();
        pendingMaintenanceQueueHook = null;
        CompletableFuture<Point> probeFuture = maintenanceSelfConfirmProbeFuture;
        maintenanceSelfConfirmProbeFuture = null;
        Point prepared = probeFuture != null && probeFuture.isDone() ? probeFuture.getNow(null) : null;
        boolean probeFresh = prepared != null
                && System.currentTimeMillis() - maintenanceSelfConfirmProbeStartedAtMs
                        <= MAINTENANCE_SELF_CONFIRM_PROBE_TTL_MS;
        boolean confirmed = false;
        if (probeFresh) {
            confirmed = inputSequences.moveAndClickLeft(
                    "xiuluo:maintenanceSelfConfirm:" + hookName,
                    prepared.x, prepared.y, 150, 800);
            log.info("[xiuluo-v2] {} hook self confirm via pre-recognized point: point=({}, {}) clicked={}",
                    hookName, prepared.x, prepared.y, confirmed);
        }
        if (!confirmed) {
            TaskMaintenanceResult maintenanceResult = taskMaintenanceService.runOpportunisticMaintenance(context,
                    TaskMaintenanceRequest.builder()
                            .sourceTask(broadcastSource + ":self-confirm-live")
                            .handleMaintenanceBroadcast(true)
                            .cleanSummonSkill(false)
                            .build());
            if (maintenanceResult.getStatus() == TaskMaintenanceStatus.INTERRUPTED) {
                return MaintenanceAttemptResult.withOutcome(
                        XiuluoStepOutcome.stopped(state, hookName + " self confirm interrupted"));
            }
            confirmed = maintenanceResult.isBroadcastHandled();
            log.info("[xiuluo-v2] {} hook self confirm live rescan: handled={} status={} message={}",
                    hookName, confirmed, maintenanceResult.getStatus(), maintenanceResult.getMessage());
        }
        if (!confirmed) {
            log.warn("[xiuluo-v2] {} hook self confirm not found after queue drain; continue and rely on next cooldown round",
                    hookName);
        }
        return MaintenanceAttemptResult.handledResult(System.currentTimeMillis() - startedAt);
    }

    private void cleanupAndLogMaintenanceRetry(String hookName, NpcTarget npc, int attempt) {
        uiCleanerService.cleanLightweightInterruptions("xiuluo-v2:" + hookName + "-retry-cleanup:" + attempt);
        LocationInfo current = playerStateService.syncMyPosition();
        boolean sameMap = current != null && gameStateUtil.isSameMapName(current.mapName, npc.getMapName());
        boolean nearTarget = current != null && gameStateUtil.isNearCoordinate(current.mapName, current.x, current.y,
                npc.getMapName(), npc.getX(), npc.getY(), TASK_NPC_SHORT_PATH_KEEP_TURN_DISTANCE);
        log.warn("[xiuluo-v2] {} hook retry cleanup done: attempt={}/{} current={} sameMap={} nearTarget={} target={}({}, {})",
                hookName, attempt, MAX_MAINTENANCE_HOOK_ATTEMPTS, current, sameMap, nearTarget,
                npc.getMapName(), npc.getX(), npc.getY());
    }

    private XiuluoStepOutcome startLeavingStartMapIfPresent(TaskExecutionContext context,
                                                            XiuluoRoundContext state,
                                                            XiuluoPhase nextPhase,
                                                            String nextSource) {
        if (state.startExitPrepathStarted()) {
            log.info("[xiuluo-v2] skip start-map exit pre-pathing: already started source={}", state.source());
            return null;
        }
        String currentMap = gameContext.getMe() == null ? null : gameContext.getMe().getCurrentMapName();
        if (!gameStateUtil.isSameMapName(currentMap, START_MAP_NAME)) {
            log.info("[xiuluo-v2] skip start-map exit pre-pathing: current={} startMap={}",
                    currentMap, START_MAP_NAME);
            return null;
        }
        PlayerCharacter me = gameContext.getMe();
        if (me != null && gameStateUtil.isNearCoordinate(me.getCurrentMapName(), me.getX(), me.getY(),
                START_MAP_NAME, START_EXIT_X, START_EXIT_Y, START_EXIT_PREPATH_SKIP_DISTANCE)) {
            log.info("[xiuluo-v2] skip start-map exit pre-pathing: already near exit player=({}, {}) exit=({}, {}) tolerance={}",
                    me.getX(), me.getY(), START_EXIT_X, START_EXIT_Y, START_EXIT_PREPATH_SKIP_DISTANCE);
            return null;
        }

        /*
         * Xiuluo's old fast path clicked the Ling Shou Village exit immediately after reading the
         * objective. That lets the leader start walking while the next phase opens the world-map
         * route. This is only an optimization; failure must not block the formal target navigation.
         */
        NavigationResult result = navigationService.navigateInCurrentMap(NavigationRequest.builder()
                .targetMapName(START_MAP_NAME)
                .targetX(START_EXIT_X)
                .targetY(START_EXIT_Y)
                .targetName("灵兽村出口")
                .miniMapClickRandomRadiusPx(PREPATH_MINI_MAP_CLICK_RANDOM_RADIUS_PX)
                .source("xiuluo-v2:start-exit-prepath:currentMap")
                .build());
        NavigationResultStatus status = result.getStatus();
        log.info("[xiuluo-v2] start-map exit pre-pathing result: status={} message={}",
                status, result.getMessage());
        if (status == NavigationResultStatus.STOPPED) {
            return XiuluoStepOutcome.stopped(state, "start-map exit pre-pathing stopped");
        }
        if (status == NavigationResultStatus.PATHING_STARTED) {
            /*
             * Exit pre-pathing is only a head start before the real route decision. CR91 uses it to
             * overlap walking out of 灵兽村 with tracker parsing, then consumes the accept-time
             * tracker snapshot without opening the old objective route unless shortcut startup fails.
             */
            return XiuluoStepOutcome.continueTo(
                    state.withStartExitPrepathStarted(nextPhase, nextSource),
                    "start-map exit pathing started; continue shortcut startup while walking");
        }
        return null;
    }

    private XiuluoStepOutcome tryTrackerShortcut(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        TaskTrackerPanelReadResult panel = resolveShortcutTrackerPanel(context, state);
        return tryTrackerShortcutWithPanel(context, state, panel);
    }

    private TaskTrackerPanelReadResult resolveShortcutTrackerPanel(TaskExecutionContext context,
                                                                   XiuluoRoundContext state) {
        CompletableFuture<TaskTrackerPanelReadResult> acceptFuture = state.shortcutTrackerParseFuture();
        if (acceptFuture != null && state.shortcutTrackerRetryCount() == 0) {
            TaskTrackerPanelReadResult panel = waitForAcceptTrackerPanelResult(context, acceptFuture, state);
            log.info("[xiuluo-v2 shortcut] accept-time tracker snapshot consumed: round={} found={} links={} detail={} source={}",
                    state.round(), panel.isFound(), panel.getGreenLinks().size(),
                    panel.getDetailRawPath(), state.source());
            return panel;
        }
        return taskTrackerPanelService.readXiuluoTrackerPanel(
                "xiuluo-v2:shortcut:" + state.round() + ":" + state.shortcutTrackerRetryCount());
    }

    private XiuluoStepOutcome tryTrackerShortcutWithPanel(TaskExecutionContext context,
                                                          XiuluoRoundContext state,
                                                          TaskTrackerPanelReadResult panel) {
        Optional<Point> clickPoint = taskTrackerPanelService.resolveXiuluoTrackerGreenClickPoint(panel);
        if (clickPoint.isEmpty()) {
            return fallbackFromShortcut(context, state, "tracker-miss-or-no-green");
        }

        Point point = clickPoint.get();
        Point effectivePoint = point;
        if (panel.getSourceType() != TaskTrackerPanelSourceType.CLOUD_TRACKER_PANEL_READER) {
            boolean trackerBaseReady = tracker.refreshWindowState();
            TrackerLinkRankerCloudDecision cloudDecision = trackerLinkRankerCloudShadowService.shadowTrackerLinkSelection(
                    TASK_CODE,
                    "xiuluo-v2:trackerShortcutGreen:" + state.round(),
                    "xiuluo-tracker-shortcut",
                    panel.getGreenLinks(),
                    0,
                    panel.getGreenLinks().isEmpty() ? null : panel.getGreenLinks().get(0),
                    trackerBaseReady ? tracker.getWindowBaseX() : -1,
                    trackerBaseReady ? tracker.getWindowBaseY() : -1);
            if (cloudDecision.isNoClick()) {
                log.warn("[xiuluo-v2 shortcut] tracker cloud decision rejected; skip local tracker green click: "
                                + "round={} retry={} reason={} detail={}",
                        state.round(), state.shortcutTrackerRetryCount(),
                        cloudDecision.getRejectReason(), panel.getDetailRawPath());
                return fallbackFromShortcut(context, state, "tracker-cloud-no-click");
            }
            Point cloudPoint = resolveTrackerCloudAbsolutePoint(cloudDecision,
                    "xiuluo-v2:trackerShortcutGreen:" + state.round());
            if (cloudDecision.isCloudExecuted() && cloudPoint == null) {
                log.warn("[xiuluo-v2 shortcut] tracker cloud decision executed but absolute click unavailable; "
                                + "skip local tracker green click: round={} retry={} relative={}",
                        state.round(), state.shortcutTrackerRetryCount(),
                        cloudDecision.getCloudWindowRelativeClickPoint());
                return fallbackFromShortcut(context, state, "tracker-cloud-click-base-unavailable");
            }
            effectivePoint = cloudPoint == null ? point : cloudPoint;
        } else {
            log.info("[xiuluo-v2 shortcut] use TRACKER_PANEL_READER click directly: round={} retry={} click=({}, {}) detail={}",
                    state.round(), state.shortcutTrackerRetryCount(), point.x, point.y, panel.getDetailRawPath());
        }
        if (state.firstTrackerGreenClickAtMs() > 0L) {
            /*
             * CR232 arbitration, last gate: a kanda2 prepared action that arrived while the panel
             * was being re-read must win over the retry click. Registering a new intent below
             * would orphan it behind the intentId consume gate for the rest of the round.
             */
            XiuluoStepOutcome lateEnterBattle = consumePreparedXiuluoEnterBattle(context, state,
                    state.shortcutPathingIntentId(), "shortcut-retry-preclick-rearbitration");
            if (lateEnterBattle != null) {
                return lateEnterBattle;
            }
        }
        Point windowRelativeClick = resolveCurrentWindowRelativePoint(effectivePoint,
                "xiuluo-v2:trackerShortcutGreen:" + state.round());
        if (windowRelativeClick == null) {
            log.warn("[xiuluo-v2 shortcut] tracker green click rejected because current window-relative point is unavailable: round={} absolute=({}, {})",
                    state.round(), effectivePoint.x, effectivePoint.y);
            return fallbackFromShortcut(context, state, "tracker-green-relative-point-unavailable");
        }
        boolean clicked = inputSequences.moveAndClickLeft(
                "xiuluo-v2:trackerShortcutGreen:" + state.round(),
                effectivePoint.x, effectivePoint.y, 120, 150);
        if (!clicked) {
            return fallbackFromShortcut(context, state, "tracker-green-click-failed");
        }

        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        String intentSource = TRACKER_SHORTCUT_PATHING_SOURCE_PREFIX + ":" + state.round() + ":"
                + state.shortcutTrackerRetryCount();
        gameStateUtil.recordMovementIntent(intentSource);
        // CR232 review P2: the map does not participate in 看打 recognition or fallback. The intent
        // exists only so the observer can report stop events for this attemptId — no target map,
        // no late target-map upgrade.
        WindowPathingIntent pathingIntent = registerTrackerShortcutPathingIntent(runtime, intentSource,
                null);
        if (runtime != null) {
            /*
             * CR232: probe-only interest — during active pathing the runner runs ONLY the local
             * kanda2 small-ROI probe (no generic dialog detection, no cloud), starting 25s after
             * this green click. Anchor note: the spec anchor is accept-success + 25s; the green
             * click lands only a few seconds after accept success, so anchoring here is the safe,
             * slightly-later approximation (never earlier than spec). The pathing terminal later
             * upgrades this interest to the full parallel local+cloud chain.
             */
            registerXiuluoEnterBattleProbeOnlyInterest(runtime, state,
                    "xiuluo-v2:shortcut-enter-battle:" + state.round());
        }
        XiuluoRoundContext next = state.withShortcutTrackerClick(
                XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING,
                panel.getDetailRawPath(),
                effectivePoint,
                windowRelativeClick,
                pathingIntent == null ? null : pathingIntent.getIntentId(),
                "tracker-shortcut-green-clicked");
        openXiuluoGreenChainSchedule(context, runtime, next, pathingIntent,
                "tracker-shortcut-green-clicked");
        openTeamPathingMaintenanceWindow(context, next, "tracker-shortcut-green-clicked");
        returnItemPrescanService.afterTrackerGreen(context, TASK_CODE, next.round(), RETURN_ITEM_TEMPLATE,
                ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                "xiuluo-v2:tracker-shortcut-green-clicked");
        /*
         * CR243 / user-approved 2026-07-10 order: the leader handles its own pending recovery
         * immediately after the green click, then permanently parks for pathing/看打 events. The
         * member FIFO continues in background; this phase must never poll it or wait for it.
         */
        autoCombatService.reportQueuedLeaderPostCombatFirstAidIfPending(context,
                "xiuluo-v2:tracker-shortcut-green-clicked:round-" + next.round());
        taskMaintenanceService.openPostCombatFirstAidQueue(context,
                "xiuluo-v2:tracker-shortcut-green-clicked:round-" + next.round());
        autoCombatService.consumeQueuedLeaderPostCombatFirstAidIfHead(context,
                "xiuluo-v2:tracker-shortcut-leader-first-aid:round-" + next.round());
        autoCombatService.consumePendingLeaderPostCombatRecoveryIfAllowed(context,
                "xiuluo-v2:tracker-shortcut-leader-recovery:round-" + next.round());
        log.info("[xiuluo-v2 shortcut] tracker green clicked: round={} click=({}, {}) relative=({}, {}) detail={} retry={} firstAt={}",
                next.round(), effectivePoint.x, effectivePoint.y, windowRelativeClick.x, windowRelativeClick.y,
                panel.getDetailRawPath(),
                next.shortcutTrackerRetryCount(), next.firstTrackerGreenClickAtMs());
        return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                next, "tracker shortcut green clicked; wait runner/window facts"));
    }

    private Point resolveTrackerCloudAbsolutePoint(TrackerLinkRankerCloudDecision cloudDecision,
                                                   String label) {
        Point cloudPoint = cloudDecision == null ? null : cloudDecision.getCloudWindowRelativeClickPoint();
        if (cloudPoint == null) {
            return null;
        }
        if (tracker.refreshWindowState() && tracker.getWindowBaseX() >= 0 && tracker.getWindowBaseY() >= 0) {
            log.info("[xiuluo-v2 shortcut] tracker cloud click uses tracker logical base: label={} "
                            + "relative=({}, {}) base=({}, {})",
                    label, cloudPoint.x, cloudPoint.y, tracker.getWindowBaseX(), tracker.getWindowBaseY());
            return new Point(tracker.getWindowBaseX() + cloudPoint.x, tracker.getWindowBaseY() + cloudPoint.y);
        }
        log.warn("[xiuluo-v2 shortcut] tracker cloud click cannot resolve window base: label={} relative=({}, {})",
                label, cloudPoint.x, cloudPoint.y);
        return null;
    }

    /**
     * Converts a just-executed tracker click to window-relative coordinates for the same-round
     * terminal retry. The persisted value is independent of a later desktop/window move; retry
     * resolves it again against the then-current bound window base.
     */
    private Point resolveCurrentWindowRelativePoint(Point absolutePoint, String source) {
        if (absolutePoint == null) {
            return null;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = runtime == null ? null : runtime.getNativeBinding();
        if (binding != null && binding.hasGeometry()) {
            return new Point(absolutePoint.x - binding.getX(), absolutePoint.y - binding.getY());
        }
        if (tracker.refreshWindowState() && tracker.getWindowBaseX() >= 0 && tracker.getWindowBaseY() >= 0) {
            log.warn("[xiuluo-v2 shortcut] tracker green relative point uses legacy tracker base: source={} base=({}, {})",
                    source, tracker.getWindowBaseX(), tracker.getWindowBaseY());
            return new Point(absolutePoint.x - tracker.getWindowBaseX(),
                    absolutePoint.y - tracker.getWindowBaseY());
        }
        return null;
    }

    private XiuluoStepOutcome attachAcceptObjectiveBackgroundParseAfterStartExitPrepath(XiuluoStepOutcome outcome) {
        if (outcome == null
                || outcome.nextState() == null
                || outcome.nextState().phase() != XiuluoPhase.TRY_TRACKER_SHORTCUT
                || !outcome.nextState().startExitPrepathStarted()) {
            return outcome;
        }
        XiuluoRoundContext nextState = outcome.nextState();
        XiuluoRoundContext parsedState = scheduleAcceptObjectiveBackgroundParse(nextState, nextState.source())
                .next(nextState.phase(), nextState.source());
        return outcome.withNextState(parsedState);
    }

    private TaskTrackerPanelReadResult waitForAcceptTrackerPanelResult(
            TaskExecutionContext context,
            CompletableFuture<TaskTrackerPanelReadResult> future,
            XiuluoRoundContext state) {
        long startedAt = System.currentTimeMillis();
        XiuluoRoundContext waitState = state;
        while (true) {
            long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested(
                    context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            if (pauseBlockedMs > 0L) {
                waitState = compensatePreCombatTimerAfterMaintenance(
                        waitState, pauseBlockedMs, "xiuluo-v2:accept-tracker-parse:user-pause");
            }
            long remainingMs = remainingPreCombatWatchdogBudgetMs(waitState, System.currentTimeMillis());
            if (remainingMs <= 0L) {
                // A completed parse must never be discarded as a fake failure just because the
                // watchdog budget ran out while it was in flight.
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    TaskTrackerPanelReadResult completed = future.getNow(null);
                    if (completed != null) {
                        log.info("[xiuluo-v2 shortcut] consumed completed accept-time tracker parse despite exhausted watchdog budget: round={} source={} elapsedMs={} window={}",
                                waitState.round(), waitState.source(),
                                System.currentTimeMillis() - startedAt, currentWindowLabel());
                        return completed;
                    }
                }
                log.warn("[xiuluo-v2 shortcut] accept-tracker-parse watchdog budget timeout: round={} phase={} source={} elapsedMs={} remainingMs={} window={}",
                        waitState.round(), waitState.phase(), waitState.source(),
                        System.currentTimeMillis() - startedAt, remainingMs, currentWindowLabel());
                return TaskTrackerPanelReadResult.empty();
            }
            long waitSliceMs = remainingMs == Long.MAX_VALUE ? 250L : Math.min(250L, remainingMs);
            try {
                TaskTrackerPanelReadResult result = future.get(waitSliceMs, TimeUnit.MILLISECONDS);
                return result == null ? TaskTrackerPanelReadResult.empty() : result;
            } catch (TimeoutException e) {
                log.info("[xiuluo-v2 shortcut] waiting accept-time tracker parse: round={} source={} elapsedMs={} remainingMs={} waitSliceMs={} window={}",
                        waitState.round(), waitState.source(), System.currentTimeMillis() - startedAt,
                        remainingMs, waitSliceMs, currentWindowLabel());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
                return TaskTrackerPanelReadResult.empty();
            } catch (ExecutionException e) {
                log.warn("[xiuluo-v2 shortcut] accept-time tracker parse failed: round={} source={} elapsedMs={} window={}",
                        waitState.round(), waitState.source(), System.currentTimeMillis() - startedAt, currentWindowLabel(),
                        e.getCause() == null ? e : e.getCause());
                return TaskTrackerPanelReadResult.empty();
            }
        }
    }

    private String resolveReadyShortcutObjectiveTargetMap(XiuluoRoundContext state) {
        if (state.objective() != null) {
            return state.objective().getMapName();
        }
        CompletableFuture<Optional<NpcTarget>> future = state.objectiveParseFuture();
        if (future == null || !future.isDone() || future.isCompletedExceptionally() || future.isCancelled()) {
            return null;
        }
        Optional<NpcTarget> objective = future.getNow(Optional.empty());
        String targetMap = objective.map(NpcTarget::getMapName).orElse(null);
        log.info("[xiuluo-v2 shortcut] ready objective target map resolved for runner: round={} hit={} targetMap={} source={}",
                state.round(), targetMap != null && !targetMap.isBlank(), targetMap, state.source());
        return targetMap;
    }

    private WindowPathingIntent registerTrackerShortcutPathingIntent(WindowRuntimeContext runtime,
                                                                     String intentSource,
                                                                     String targetMapName) {
        if (runtime == null) {
            log.warn("[xiuluo-v2 shortcut] tracker pathing intent skipped: reason=no-window-runtime source={}",
                    intentSource);
            return null;
        }
        boolean hasTargetMap = targetMapName != null && !targetMapName.isBlank();
        WindowPathingIntent intent = WindowPathingIntent.builder()
                .source(intentSource)
                .type(hasTargetMap ? WindowPathingIntentType.TARGETED : WindowPathingIntentType.UNTARGETED_TRACKER)
                .targetMapName(hasTargetMap ? targetMapName : null)
                .targetX(null)
                .targetY(null)
                .tolerance(0)
                .build();
        runtime.markPathingStarted(intent);
        log.info("[xiuluo-v2 shortcut] tracker pathing intent registered: windowId={} source={} intentId={} type={} targetMap={}",
                runtime.getWindowId(), intentSource, intent.getIntentId(), intent.getType(), intent.getTargetMapName());
        return intent;
    }

    private void attachShortcutTargetMapUpgrade(WindowRuntimeContext runtime,
                                                XiuluoRoundContext state,
                                                WindowPathingIntent pathingIntent) {
        if (runtime == null || state == null || pathingIntent == null
                || pathingIntent.getType() != WindowPathingIntentType.UNTARGETED_TRACKER) {
            return;
        }
        CompletableFuture<Optional<NpcTarget>> future = state.objectiveParseFuture();
        if (future == null) {
            return;
        }
        String intentId = pathingIntent.getIntentId();
        String upgradeSource = "xiuluo-v2:shortcut-late-target-map:" + state.round();
        future.thenAccept(objective -> {
            String targetMap = objective == null
                    ? null
                    : objective.map(NpcTarget::getMapName).orElse(null);
            if (targetMap == null || targetMap.isBlank()) {
                log.info("[xiuluo-v2 shortcut] late target-map upgrade skipped: round={} intentId={} reason=no-objective-map source={}",
                        state.round(), intentId, state.source());
                return;
            }
            boolean upgraded = runtime.upgradeActivePathingIntentTargetMap(intentId, targetMap, upgradeSource);
            log.info("[xiuluo-v2 shortcut] late target-map upgrade result: round={} intentId={} targetMap={} upgraded={} source={}",
                    state.round(), intentId, targetMap, upgraded, state.source());
        }).exceptionally(e -> {
            log.warn("[xiuluo-v2 shortcut] late target-map upgrade skipped: round={} intentId={} reason=objective-future-failed source={}",
                    state.round(), intentId, state.source(), e);
            return null;
        });
    }

    private boolean clearTrackerShortcutPathingIntent(WindowRuntimeContext runtime, String reason) {
        if (runtime == null) {
            return false;
        }
        return runtime.clearPathingSignalIfSourcePrefix(TRACKER_SHORTCUT_PATHING_SOURCE_PREFIX, reason);
    }

    private XiuluoStepOutcome waitTrackerShortcutPathing(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        long combatWaitAfterSequence = windowReadyEventBus.currentSequence();
        AutoCombatService.TickResult combatTick = autoCombatService.handleCombatTick(
                context, "xiuluo-v2:shortcut",
                AutoCombatService.PostCombatRecoveryPolicy.FULL_RECOVERY_WITH_LEADER_INCENSE);
        if (combatTick == AutoCombatService.TickResult.IN_COMBAT) {
            closeTeamPathingMaintenanceWindow(context, state, "shortcut-incidental-combat-started");
            return waitForCombatStateWake(XiuluoStepOutcome.sharedState(
                    state.withCombatSource(XiuluoPhase.WAIT_COMBAT, XiuluoCombatSource.INCIDENTAL,
                            "shortcut-incidental-combat-detected"),
                    "shortcut incidental combat detected"), combatWaitAfterSequence);
        }

        // CR253 typed prepared source 1: local kanda probe hit (prepared dialog action, stamped
        // with the current attemptId — same identity rule as the typed jobs).
        XiuluoStepOutcome prepared = consumePreparedXiuluoEnterBattle(context, state,
                state.shortcutPathingIntentId(), "shortcut-wait-prepared-enter-battle");
        if (prepared != null) {
            return prepared;
        }

        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        // CR253 typed prepared source 2: background stop-static cloud 看打 hit.
        XiuluoStepOutcome cloudEnterBattle = consumeXiuluoEnterBattleCloudJob(context, state, runtime);
        if (cloudEnterBattle != null) {
            return cloudEnterBattle;
        }
        // CR253 typed prepared source 3: the cloud's explicit fallback for the current stop static.
        XiuluoStepOutcome greenRetry = consumeTrackerGreenRetryJob(context, state, runtime);
        if (greenRetry != null) {
            return greenRetry;
        }
        // CR253 typed prepared source 4: background-detected summon-skill due job.
        XiuluoStepOutcome summonCleanup = consumeSummonSkillCleanupJob(context, state, runtime);
        if (summonCleanup != null) {
            return summonCleanup;
        }

        /*
         * CR253: PATHING_TERMINAL is background work — the runner captures the stop static and owns
         * the cloud arbitration; stale-attempt terminals are discarded there (round-87 class). With
         * no consumable typed prepared for the current round+attempt the foreground neither re-reads
         * the terminal snapshot nor executes any input: it parks again until prepared work (or a
         * combat state change) arrives. The pre-combat watchdog remains the recovery net.
         */
        WindowPathingSnapshot snapshot = runtime == null ? null : runtime.getPathingSnapshot();
        log.info("[xiuluo-v2 shortcut] no consumable typed prepared; park for prepared work: round={} attempt={} snapshotState={} retry={}",
                state.round(), state.shortcutPathingIntentId(),
                snapshot == null ? null : snapshot.getState(), state.shortcutTrackerRetryCount());
        return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                state, "tracker shortcut parked; waiting typed prepared work"));
    }

    /**
     * CR253: open (or renew) the green-chain attempt identity on the window runtime. Background
     * producers stamp and gate typed prepared jobs against this schedule; replacing it atomically
     * discards jobs of any previous attempt.
     */
    private void openXiuluoGreenChainSchedule(TaskExecutionContext context,
                                              WindowRuntimeContext runtime,
                                              XiuluoRoundContext state,
                                              WindowPathingIntent attemptIntent,
                                              String reason) {
        if (runtime == null || attemptIntent == null || attemptIntent.getIntentId() == null) {
            log.warn("[xiuluo-v2 shortcut] green-chain schedule not opened: reason={} runtimePresent={} intentPresent={}",
                    reason, runtime != null, attemptIntent != null);
            return;
        }
        WindowNativeBinding binding = runtime.getNativeBinding();
        runtime.updateXiuluoGreenChainSchedule(XiuluoGreenChainSchedule.builder()
                .windowId(runtime.getWindowId())
                .hwnd(binding == null ? null : binding.getNativeHandle())
                .taskRunId(context.getTaskRunId())
                .round(state.round())
                .attemptId(attemptIntent.getIntentId())
                .openedAtMs(System.currentTimeMillis())
                .build(), "xiuluo-v2:" + reason);
    }

    /**
     * CR253 consumer for the background cloud 看打 hit: validates the job against the current
     * round+attempt identity (stale jobs are discarded without input), then clicks the cloud
     * coordinate exactly once.
     */
    private XiuluoStepOutcome consumeXiuluoEnterBattleCloudJob(TaskExecutionContext context,
                                                               XiuluoRoundContext state,
                                                               WindowRuntimeContext runtime) {
        if (runtime == null) {
            return null;
        }
        if (state.enteredBattleByXiuluo()
                || gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            // CR232 one-shot rule: background work arriving after combat entry is stale.
            runtime.clearPreparedActionJobs("xiuluo enter battle already handled; discard late jobs");
            return null;
        }
        PreparedActionJob job = runtime.consumePreparedActionJobValidated(
                PreparedActionJobType.XIULUO_ENTER_BATTLE,
                context.getTaskRunId(), state.round(), state.shortcutPathingIntentId(),
                "xiuluo-v2:shortcut-cloud-enter-battle");
        if (job == null) {
            return null;
        }
        return clickCloudEnterBattlePoint(context, state, runtime, job);
    }

    /**
     * CR253 consumer for the cloud's explicit fallback verdict: the background arbitration already
     * confirmed CLOUD_FALLBACK for this attempt's stop static, so the re-press runs under the
     * existing CR232 budget and creates the next attemptId.
     */
    private XiuluoStepOutcome consumeTrackerGreenRetryJob(TaskExecutionContext context,
                                                          XiuluoRoundContext state,
                                                          WindowRuntimeContext runtime) {
        if (runtime == null) {
            return null;
        }
        PreparedActionJob job = runtime.consumePreparedActionJobValidated(
                PreparedActionJobType.TRACKER_GREEN_RETRY,
                context.getTaskRunId(), state.round(), state.shortcutPathingIntentId(),
                "xiuluo-v2:shortcut-green-retry");
        if (job == null) {
            return null;
        }
        log.info("[xiuluo-v2 shortcut] cloud verdict CLOUD_FALLBACK consumed from background arbitration: round={} attempt={} reason={}",
                state.round(), job.getAttemptId(), job.getReason());
        return repressSavedGreenOnCloudFallback(context, state, runtime,
                runtime.getPathingSnapshot(), job.getAttemptId());
    }

    /**
     * CR253 consumer for the background-detected summon-skill due job: run the complete three-skill
     * maintenance flow on the task thread, then park again. This replaces the previous
     * opportunistic phase-rerun maintenance, which an infinite park would have starved.
     */
    private XiuluoStepOutcome consumeSummonSkillCleanupJob(TaskExecutionContext context,
                                                           XiuluoRoundContext state,
                                                           WindowRuntimeContext runtime) {
        if (runtime == null) {
            return null;
        }
        PreparedActionJob job = runtime.consumePreparedActionJobValidated(
                PreparedActionJobType.SUMMON_SKILL_CLEANUP,
                context.getTaskRunId(), state.round(), state.shortcutPathingIntentId(),
                "xiuluo-v2:shortcut-summon-cleanup");
        if (job == null) {
            return null;
        }
        log.info("[xiuluo-v2 shortcut] summon-skill cleanup job consumed; run full maintenance then park: round={} attempt={} source={}",
                state.round(), job.getAttemptId(), job.getSource());
        XiuluoStepOutcome maintenanceOutcome = runLeaderPathingSummonSkillMaintenance(
                context, state, "prepared-job:summon-skill-cleanup");
        if (maintenanceOutcome != null
                && maintenanceOutcome.transactionResult() == TaskTransactionResult.STOPPED) {
            return maintenanceOutcome;
        }
        XiuluoRoundContext adjusted = maintenanceOutcome == null || maintenanceOutcome.nextState() == null
                ? state
                : maintenanceOutcome.nextState();
        return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                adjusted, "summon-skill cleanup job consumed; park again"));
    }

    /**
     * CR232 semantics preserved under CR253 consumption: the cloud recognized 看打 on the stop
     * static image (now delivered as a typed prepared job) — the foreground clicks the cloud
     * coordinate. The local kanda2 probe, interest, and pathing intent are deliberately NOT cleared
     * here: only the combat guard's confirmed entry (the existing combat-entry cleanup) stops the
     * probe. A physically failed click is this attempt's confirmed failure and routes to the cloud
     * fallback handshake under the same budget.
     */
    private XiuluoStepOutcome clickCloudEnterBattlePoint(TaskExecutionContext context,
                                                         XiuluoRoundContext state,
                                                         WindowRuntimeContext runtime,
                                                         PreparedActionJob job) {
        String attemptId = job.getAttemptId();
        WindowNativeBinding binding = runtime == null ? null : runtime.getNativeBinding();
        Integer relativeX = job.getWindowRelativeX();
        Integer relativeY = job.getWindowRelativeY();
        if (binding == null || !binding.hasGeometry() || relativeX == null || relativeY == null) {
            log.warn("[xiuluo-v2 shortcut] cloud enter-battle point unusable: round={} attempt={} relative=({}, {}) binding={}",
                    state.round(), attemptId, relativeX, relativeY,
                    binding == null ? null : binding.getGeometryText());
            return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                    state, "cloud enter-battle point unusable; keep waiting"));
        }
        boolean clicked = inputSequences.moveAndClickLeft(
                "xiuluo-v2:cloudEnterBattle:" + state.round() + ":attempt-" + attemptId,
                binding.getX() + relativeX, binding.getY() + relativeY, 80, 150);
        if (!clicked) {
            /*
             * CR232 review: a failed physical click of the cloud coordinate is NOT a local fallback
             * decision. Report the click-failed outcome for this attemptId to the cloud and
             * re-press the saved green link only if the cloud explicitly returns CLOUD_FALLBACK
             * (CLOUD_NO_ACTION). Cloud unavailable/reject/no-decision -> keep waiting, no re-press,
             * no count.
             */
            log.info("[xiuluo-v2 shortcut] cloud enter-battle click failed; report click-failed outcome and await cloud fallback: round={} attempt={}",
                    state.round(), attemptId);
            return awaitCloudFallbackAfterClickFailure(context, state, runtime, attemptId);
        }
        // Heartbeat review P1: click success is NOT combat entry — keep the green-chain schedule
        // (and with it the background arbitration + local kanda2 probe) until the combat guard
        // confirms IN_COMBAT (clearXiuluoDialogStateOnCombatEntry) or a stop/failure/new-attempt
        // boundary clears it.
        autoCombatService.initializeForCurrentWindow();
        autoCombatService.authorizeCombatDetectionAfterEnterBattleAction(
                "xiuluo-v2:cloud-enter-battle-clicked:attempt-" + attemptId);
        log.info("[xiuluo-v2 shortcut] cloud enter-battle clicked: round={} attempt={} relative=({}, {}) matched={}",
                state.round(), attemptId, relativeX, relativeY, job.getMatchedText());
        return XiuluoStepOutcome.sharedState(
                state.withPendingEnterBattleConfirm(XiuluoPhase.WAIT_COMBAT, XiuluoCombatSource.TRACKER_CONFIRM,
                        "cloud-enter-battle-clicked"),
                "cloud enter-battle coordinate clicked; wait for combat entry");
    }

    /**
     * CR232 review fix: report the current attempt's cloud-coordinate click failure back to the
     * cloud. Only an explicit fallback confirmation (CLOUD_NO_ACTION for this attempt) produces
     * follow-up work, and per CR253 review that work is a {@code TRACKER_GREEN_RETRY} typed job for
     * the unified consumer — never an inline re-press. Any other cloud verdict — a new coordinate,
     * unavailable, disabled, required-failure, or no static capture — keeps the task waiting on the
     * local kanda2 probe/events and does not consume the fallback budget.
     */
    private XiuluoStepOutcome awaitCloudFallbackAfterClickFailure(TaskExecutionContext context,
                                                                  XiuluoRoundContext state,
                                                                  WindowRuntimeContext runtime,
                                                                  String attemptId) {
        com.bot.dhxy.cloud.task.DialogPolicyPreClickCloudDecision decision =
                dialogService.decideXiuluoEnterBattleStopStatic(
                        DialogHandleRequest.handleGreenTemplateOption(
                                "xiuluo-v2:kanda-click-failed:" + state.round() + ":attempt-" + attemptId,
                                List.of(new GreenTemplateClickSpec(OPTION_ENTER_BATTLE, ENTER_BATTLE_TEMPLATE, -6, 6, 4)),
                                true),
                        "CLICK_FAILED", attemptId);
        if (decision != null && decision.getStatus()
                == com.bot.dhxy.cloud.task.DialogPolicyPreClickCloudDecision.Status.CLOUD_NO_ACTION) {
            /*
             * CR253 二轮复审 P1-2: the click-failed confirmation must travel the SAME
             * publish -> PREPARED_ACTION_READY wake -> consumer -> park boundary as every other
             * typed prepared. Publish the job AND its ready event, then return the normal infinite
             * park. The wait spec's afterSequence is captured BEFORE the ready event is published,
             * so the park consumes that wake in a fresh command cycle — the physical re-press never
             * happens inside this wake chain.
             */
            XiuluoGreenChainSchedule schedule = runtime == null
                    ? null
                    : runtime.getXiuluoGreenChainSchedule().orElse(null);
            if (schedule == null || !schedule.getAttemptId().equals(attemptId)) {
                log.info("[xiuluo-v2 shortcut] cloud fallback confirmed after click failure but attempt schedule is gone/stale; discard: round={} attempt={} schedule=[{}]",
                        state.round(), attemptId, schedule == null ? null : schedule.identityText());
                return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                        state, "stale click-failed fallback confirmation discarded; keep parked"));
            }
            PreparedActionJob retryJob = PreparedActionJob.builder()
                    .type(PreparedActionJobType.TRACKER_GREEN_RETRY)
                    .windowId(schedule.getWindowId())
                    .hwnd(schedule.getHwnd())
                    .taskRunId(schedule.getTaskRunId())
                    .round(schedule.getRound())
                    .attemptId(schedule.getAttemptId())
                    .reason(decision.getReason())
                    .source("xiuluo-v2:click-failed-cloud-fallback")
                    .preparedAtMs(System.currentTimeMillis())
                    .build();
            long afterSequence = windowReadyEventBus.currentSequence();
            boolean published = runtime.publishPreparedActionJob(retryJob, "click-failed-cloud-fallback");
            if (published) {
                WindowNativeBinding readyBinding = runtime.getNativeBinding();
                windowReadyEventBus.publish(WindowReadyEvent.builder()
                        .windowId(runtime.getWindowId())
                        .hwnd(readyBinding == null ? null : readyBinding.getNativeHandle())
                        .type(WindowReadyEventType.PREPARED_ACTION_READY)
                        .taskType(TaskType.XIULUO_V2)
                        .source("click-failed-cloud-fallback:" + retryJob.getType() + ":" + retryJob.getSource())
                        .createdAtMs(System.currentTimeMillis())
                        .build());
            }
            log.info("[xiuluo-v2 shortcut] cloud confirmed CLOUD_FALLBACK after click failure; published TRACKER_GREEN_RETRY job and prepared-ready wake for unified consumer: round={} attempt={} published={} reason={}",
                    state.round(), attemptId, published, decision.getReason());
            return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(state,
                    "click-failed cloud fallback published as TRACKER_GREEN_RETRY job; park for prepared-ready wake"),
                    afterSequence);
        }
        log.info("[xiuluo-v2 shortcut] no cloud fallback confirmation after click failure; keep waiting: round={} attempt={} status={} reason={}",
                state.round(), attemptId,
                decision == null ? "CAPTURE_OR_SERVICE_UNAVAILABLE" : decision.getStatus(),
                decision == null ? null : decision.getReason());
        return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                state, "cloud click-failed outcome reported; awaiting cloud fallback"));
    }

    /**
     * CR232 cloud-owned fallback: only an explicit CLOUD_FALLBACK verdict for the CURRENT attempt
     * — a fresh static-image miss, or the cloud's confirmation of a reported click failure —
     * reaches this re-press. Executed re-presses are the ONLY events counted toward the round's
     * fallback limit ({@code shortcutTrackerRetryCount}); three executed fallbacks without combat
     * entry abandon the shortcut via the existing recovery chain.
     */
    private XiuluoStepOutcome repressSavedGreenOnCloudFallback(TaskExecutionContext context,
                                                               XiuluoRoundContext state,
                                                               WindowRuntimeContext runtime,
                                                               WindowPathingSnapshot snapshot,
                                                               String attemptId) {
        // Third-review P1: snapshot is diagnostic-only here and may be null on the click-failed
        // handshake (the runtime signal was cleared at the terminal branch); never dereference.
        Object snapshotState = snapshot == null ? null : snapshot.getState();
        if (state.shortcutTrackerRetryCount() >= MAX_CLOUD_ENTER_BATTLE_FALLBACKS) {
            log.warn("[xiuluo-v2 shortcut] cloud fallback limit reached; abandon shortcut this round: round={} executedFallbacks={} limit={} attempt={} state={}",
                    state.round(), state.shortcutTrackerRetryCount(), MAX_CLOUD_ENTER_BATTLE_FALLBACKS,
                    attemptId, snapshotState);
            return fallbackFromShortcut(context, state, "cloud-fallback-limit-reached");
        }
        Integer relativeX = state.shortcutTrackerClickWindowRelativeX();
        Integer relativeY = state.shortcutTrackerClickWindowRelativeY();
        WindowNativeBinding binding = runtime == null ? null : runtime.getNativeBinding();
        if (relativeX == null || relativeY == null || binding == null || !binding.hasGeometry()) {
            log.warn("[xiuluo-v2 shortcut] cloud fallback re-press unavailable: round={} relative=({}, {}) binding={} state={} message={}",
                    state.round(), relativeX, relativeY,
                    binding == null ? null : binding.getGeometryText(),
                    snapshotState, snapshot == null ? null : snapshot.getMessage());
            return fallbackFromShortcut(context, state, "terminal-saved-green-unavailable");
        }
        Point retryPoint = new Point(binding.getX() + relativeX, binding.getY() + relativeY);
        boolean clicked = inputSequences.moveAndClickLeft(
                "xiuluo-v2:trackerShortcutGreenRetry:" + state.round(),
                retryPoint.x, retryPoint.y, 120, 150);
        if (!clicked) {
            /*
             * Fourth-review P1: an un-executed re-press is NOT a consumed fallback — it must not
             * count and must not abandon the round. Keep the current attempt parked on the local
             * kanda2 probe/events; the green-chain schedule of the current attempt stays open
             * (it is only renewed by an executed re-press), so the background arbitration can
             * confirm CLOUD_FALLBACK for this attempt again. Only the limit check at the top of
             * this method — three actually executed fallbacks — reaches the abandon path.
             */
            log.warn("[xiuluo-v2 shortcut] cloud fallback re-press not executed; keep waiting current attempt: round={} attempt={} absolute=({}, {}) executedFallbacks={}",
                    state.round(), attemptId, retryPoint.x, retryPoint.y, state.shortcutTrackerRetryCount());
            return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                    state, "cloud fallback re-press not executed; keep waiting current attempt"));
        }
        int nextRetryCount = state.shortcutTrackerRetryCount() + 1;
        String intentSource = TRACKER_SHORTCUT_PATHING_SOURCE_PREFIX + ":" + state.round() + ":"
                + nextRetryCount;
        gameStateUtil.recordMovementIntent(intentSource);
        // CR232 review P2: observation-only intent — no target map, no late target-map upgrade.
        WindowPathingIntent retryIntent = registerTrackerShortcutPathingIntent(runtime, intentSource,
                null);
        XiuluoRoundContext next = state.incrementShortcutTrackerRetry(
                XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING,
                retryIntent == null ? null : retryIntent.getIntentId(),
                "shortcut-pathing-terminal-saved-green-retry");
        // The local kanda2 probe keeps running across fallbacks: same anchor, no reset.
        registerXiuluoEnterBattleProbeOnlyInterest(runtime, next,
                "xiuluo-v2:shortcut-enter-battle:" + next.round());
        // CR253: the executed re-press IS the next attempt — renewing the schedule atomically
        // discards every job still gated on the previous attemptId.
        openXiuluoGreenChainSchedule(context, runtime, next, retryIntent, "shortcut-saved-green-retry");
        log.info("[xiuluo-v2 shortcut] terminal saved-green retry clicked: round={} relative=({}, {}) currentBase=({}, {}) absolute=({}, {}) retry={} limit={} previousAttempt={} newAttempt={} state={}",
                next.round(), relativeX, relativeY, binding.getX(), binding.getY(), retryPoint.x, retryPoint.y,
                next.shortcutTrackerRetryCount(), MAX_CLOUD_ENTER_BATTLE_FALLBACKS, attemptId,
                retryIntent == null ? null : retryIntent.getIntentId(), snapshotState);
        return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                next, "shortcut terminal saved-green retry clicked"));
    }

    private XiuluoStepOutcome fallbackFromShortcut(TaskExecutionContext context,
                                                   XiuluoRoundContext state,
                                                   String reason) {
        // CR253: abandoning the shortcut ends the green-chain attempt; drop its schedule and jobs.
        WindowRuntimeContext fallbackRuntime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (fallbackRuntime != null) {
            fallbackRuntime.clearXiuluoGreenChainSchedule("xiuluo-v2:shortcut-fallback:" + reason);
        }
        closeTeamPathingMaintenanceWindow(context, state, "shortcut-fallback:" + reason);
        if (state.firstTrackerGreenClickAtMs() <= 0L) {
            log.warn("[xiuluo-v2 shortcut] initial shortcut failed; consume accept-time objective fallback: reason={} source={}",
                    reason, state.source());
            return consumeSavedObjectiveForNonShortcutRoute(context, state.toObjectiveRoute(
                    XiuluoPhase.NAVIGATE_TO_TARGET, "shortcut-initial-fallback:" + reason), reason);
        }
        log.warn("[xiuluo-v2 shortcut] mid-shortcut failure; abandon round through existing reaccept policy: reason={} retry={} routeMode={} combatSource={}",
                reason, state.shortcutTrackerRetryCount(), state.routeMode(), state.combatSource());
        return XiuluoStepOutcome.failed(state, "shortcut failure: " + reason);
    }

    private XiuluoStepOutcome consumeSavedObjectiveForNonShortcutRoute(TaskExecutionContext context,
                                                                       XiuluoRoundContext state,
                                                                       String reason) {
        CompletableFuture<Optional<NpcTarget>> parseFuture = state.objectiveParseFuture();
        if (parseFuture == null) {
            log.warn("[xiuluo-v2 shortcut] fallback missing accept-time objective future: reason={}", reason);
            return recoverBackgroundObjectiveReadFailure(context, state, "shortcut fallback missing objective future");
        }
        Optional<NpcTarget> objective = waitForBackgroundObjectiveResult(context, parseFuture, state);
        if (objective.isEmpty()) {
            if (!parseFuture.isDone()
                    && remainingPreCombatWatchdogBudgetMs(state, System.currentTimeMillis()) <= 0L) {
                // Budget exhaustion is a watchdog timeout, not a parse failure; the fact lets the
                // brain restart the round instead of walking the read.objective recovery chain.
                return preCombatWatchdogTimeoutOutcome(state, "objective-parse", "budget-exhausted");
            }
            return recoverBackgroundObjectiveReadFailure(context, state, "shortcut fallback objective parse failed");
        }
        return XiuluoStepOutcome.continueTo(
                state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, objective.get(),
                        "shortcut-fallback-objective:" + reason),
                "shortcut fallback consumed saved objective");
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
            if (isXiuluoBrainLoopEnabled()) {
                return XiuluoStepOutcome.failed(activeState,
                        "missing objective before navigation; report to XIULUO_BRAIN");
            }
            return XiuluoStepOutcome.continueTo(
                    activeState.next(XiuluoPhase.READ_OBJECTIVE, "missing-objective-before-navigation"),
                    "missing objective before navigation; reread objective");
        }
        MapCoordinate approach = coordinateHelper.calculateApproachCoordinate(
                objective.getMapName(), objective.getX(), objective.getY());
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(objective.getMapName())
                .targetX(approach.getX())
                .targetY(approach.getY())
                .targetName(objective.getName())
                .source("xiuluo-v2:target")
                .build());
        if (result.getStatus() == NavigationResultStatus.PATHING_STARTED) {
            /*
             * Cross-map travel can leave time for member maintenance, but once the leader starts the
             * current-map walk toward the combat target, summon-skill cleanup must stop competing
             * for the input queue. The existing maintenance gate is enough; do not add watcher-side
             * service callbacks for this policy.
             */
            if (NAV_MSG_CURRENT_MAP_PATHING_STARTED.equals(result.getMessage())) {
                closeTeamPathingMaintenanceWindow(context, activeState, "target-current-map-pathing-started");
            } else if (shouldOpenTeamPathingMaintenanceWindowAfterTargetNavigation(result)) {
                openTeamPathingMaintenanceWindow(context, activeState, "target-navigation-pathing-started");
            } else {
                closeTeamPathingMaintenanceWindow(context, activeState, "target-navigation-route-not-submitted");
                log.info("[xiuluo-v2] target navigation pathing did not open maintenance window: message={}",
                        result.getMessage());
            }
            consumeCommonBoxDuringNextTaskProgress(context, "xiuluo-v2:target-navigation-pathing-started");
            consumeDeferredPostCombatRecoveryDuringNextTaskProgress(context, "xiuluo-v2:target-navigation-pathing-started");
        } else if (result.getStatus() == NavigationResultStatus.ARRIVED
                || result.getStatus() == NavigationResultStatus.SUCCESS) {
            closeTeamPathingMaintenanceWindow(context, activeState, "target-route-arrived");
        }
        XiuluoStepOutcome outcome = navigationOutcome(activeState, result, XiuluoPhase.CLICK_TARGET_NPC, "navigate to target");
        if (outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED) {
            return waitForTargetPathingWake(outcome);
        }
        if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
            if (isXiuluoBrainLoopEnabled()) {
                return outcome;
            }
            return recoverTargetNavigationFailure(context, activeState, outcome.message());
        }
        return outcome;
    }

    private XiuluoStepOutcome clickTargetNpc(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        NpcTarget objective = state.objective();
        if (objective == null) {
            log.warn("[xiuluo-v2] click target requested without objective; go back to objective reader");
            if (isXiuluoBrainLoopEnabled()) {
                return XiuluoStepOutcome.failed(state,
                        "missing objective before target click; report to XIULUO_BRAIN");
            }
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
        boolean clicked = npcClickService.clickNpcSmart(combatTarget.toClickRequest(gameContext.getMe(), TaskType.XIULUO_V2));
        if (!clicked) {
            if (isXiuluoBrainLoopEnabled()) {
                return XiuluoStepOutcome.failed(state, "target click failed; report to XIULUO_BRAIN");
            }
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
            if (isXiuluoBrainLoopEnabled()) {
                return XiuluoStepOutcome.failed(state, "enter-battle option not matched; report to XIULUO_BRAIN");
            }
            return recoverEnterBattleConfirmFailure(state);
        }
        NpcTarget objective = state.objective();
        if (objective != null) {
            npcClickService.confirmPendingSmartClick(
                    objective.getMapName(),
                    objective.getName(),
                    objective.getX(),
                    objective.getY(),
                    "DIALOG_TEMPLATE", "xiuluo-v2:enter-battle:" + state.source() + ":option consumed");
        }
        autoCombatService.initializeForCurrentWindow();
        autoCombatService.authorizeCombatDetectionAfterEnterBattleAction("xiuluo-v2:battle-confirm-clicked");
        TaskSleep.sleepOrStop(context, 1200L, "Xiuluo V2 task interrupted");
        return XiuluoStepOutcome.sharedState(
                state.withPendingEnterBattleConfirm(XiuluoPhase.WAIT_COMBAT, XiuluoCombatSource.TRACKER_CONFIRM,
                        "battle-confirm-clicked"),
                "battle confirm clicked; wait for combat entry");
    }

    private XiuluoStepOutcome waitCombat(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        long combatWaitAfterSequence = windowReadyEventBus.currentSequence();
        AutoCombatService.TickResult tick = autoCombatService.handleCombatTick(
                context, "xiuluo-v2", postCombatRecoveryPolicyForXiuluoWait(state));
        if (tick == AutoCombatService.TickResult.EXIT_RECOVERED) {
            if (state.combatSource() == XiuluoCombatSource.INCIDENTAL) {
                uiCleanerService.cleanLightweightInterruptions("xiuluo-v2:shortcut-incidental-combat-exit");
                return XiuluoStepOutcome.continueTo(
                        state.next(XiuluoPhase.TRY_TRACKER_SHORTCUT, "incidental-combat-finished"),
                        "incidental combat finished; resume tracker shortcut");
            }
            if (!state.enteredBattleByXiuluo()) {
                Optional<XiuluoStepOutcome> suppressed = suppressUnknownCombatExitIfActiveCombat(
                        "wait-combat-exit-recovered", state);
                if (suppressed.isPresent()) {
                    return suppressed.get();
                }
                return resolveUnknownCombatExit(context, state);
            }
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.RETURN_HOME, "combat-finished"),
                    "combat exit recovered");
        }
        if (tick == AutoCombatService.TickResult.IN_COMBAT) {
            /*
             * Cloud brain owns the next phase, but entering combat is still an external game state.
             * Preserve the old event wait so WAIT_COMBAT does not hot-loop while the battle runs.
             */
            XiuluoRoundContext combatState = state.enteredBattleByXiuluo()
                    ? state
                    : isEnterBattleConfirmPending(state)
                    ? state.withCombatSource(XiuluoPhase.WAIT_COMBAT, state.combatSource(),
                    "combat-entry-detected-after-confirm")
                    : state.routeMode() == XiuluoRouteMode.TRACKER_SHORTCUT
                    ? state.withCombatSource(XiuluoPhase.WAIT_COMBAT, XiuluoCombatSource.INCIDENTAL,
                    "shortcut-incidental-combat-entry-detected")
                    : state.withXiuluoBattleStarted(XiuluoPhase.WAIT_COMBAT, "combat-entry-detected");
            /*
             * CR252: the leader confirmed its OWN combat entry after the 看打 click. Broadcast the
             * round's team combat phase to real-tooltip-group members bound to this local leader so
             * they stop template-confirming their own entry. Incidental combat is not the team's
             * round battle and never broadcasts.
             */
            if (combatState.combatSource() != XiuluoCombatSource.INCIDENTAL) {
                taskMaintenanceService.openTeamCombatPhaseForLeader(context,
                        "xiuluo-v2:combat-entry-confirmed");
            }
            if (isXiuluoBrainLoopEnabled()) {
                return waitForCombatStateWake(
                        XiuluoStepOutcome.sharedState(combatState, "combat still running")
                                .withFact("combatObserved", "true"),
                        combatWaitAfterSequence);
            }
            returnItemPrescanService.whileInCombat(context, TASK_CODE, state.round(), RETURN_ITEM_TEMPLATE,
                    ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                    "xiuluo-v2:wait-combat");
            /*
             * Do not sleep while the task turn is held. Combat is a shared state: the leader should
             * release quickly so follower auto-battle windows can acquire the turn and press their
             * own auto-combat buttons.
             */
            return waitForCombatStateWake(
                    XiuluoStepOutcome.sharedState(combatState, "combat still running")
                            .withFact("combatObserved", "true"),
                    combatWaitAfterSequence);
        }
        /*
         * Clicking "看打!" is not proof that battle actually started. If the user nudges the mouse
         * or the game drops the click, the old flow stayed in WAIT_COMBAT forever. Keep this as a
         * short entry-confirm window, then either return to the normal confirmation phase or
         * re-register the shortcut prepared option so Runner can wake the task immediately.
         */
        if (isEnterBattleConfirmPending(state)) {
            if (isXiuluoBrainLoopEnabled()) {
                return XiuluoStepOutcome.failed(state,
                        "combat entry not detected after battle confirm; report to XIULUO_BRAIN")
                        .withFact("combatEntryNotDetected", "true");
            }
            XiuluoStepOutcome prepared = consumePreparedXiuluoEnterBattle(context, state,
                    "wait-combat-retry-prepared-enter-battle");
            if (prepared != null) {
                return prepared;
            }
            if (state.phaseRetryCount() < ENTER_BATTLE_CONFIRM_NONE_TICKS) {
                return XiuluoStepOutcome.sharedState(
                        state.retrySamePhase("wait-combat-entry-detect"),
                        "waiting for combat entry after battle confirm click");
            }
            if (state.objective() != null) {
                return XiuluoStepOutcome.continueTo(
                        state.next(XiuluoPhase.CONFIRM_ENTER_BATTLE, "combat-entry-not-detected"),
                        "combat not detected after battle confirm click; retry enter-battle confirmation");
            }
            if (state.routeMode() == XiuluoRouteMode.TRACKER_SHORTCUT) {
                if (state.enterBattleConfirmRetryCount() < MAX_ENTER_BATTLE_CONFIRM_RETRIES) {
                    WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
                    if (runtime == null) {
                        return fallbackFromShortcut(context, state, "prepared-enter-battle-no-runtime");
                    }
                    registerXiuluoDialogInterest(runtime, DialogOperation.XIULUO_ENTER_BATTLE,
                            "xiuluo-v2:shortcut-enter-battle-retry:" + state.round() + ":"
                                    + (state.enterBattleConfirmRetryCount() + 1));
                    closeTeamPathingMaintenanceWindow(context, state, "shortcut-enter-battle-retry-reregister");
                    return waitForTrackerShortcutWake(XiuluoStepOutcome.sharedState(
                            state.incrementEnterBattleConfirmRetry(XiuluoPhase.WAIT_COMBAT,
                                    "shortcut-enter-battle-retry-registered"),
                            "combat not detected after prepared enter-battle; re-register visible option"));
                }
                return fallbackFromShortcut(context, state, "prepared-enter-battle-no-combat-after-retry");
            }
        }
        return XiuluoStepOutcome.sharedState(state, "waiting for combat state");
    }

    private boolean isEnterBattleConfirmPending(XiuluoRoundContext state) {
        return state != null
                && !state.enteredBattleByXiuluo()
                && state.combatSource() == XiuluoCombatSource.TRACKER_CONFIRM;
    }

    private AutoCombatService.PostCombatRecoveryPolicy postCombatRecoveryPolicyForXiuluoWait(XiuluoRoundContext state) {
        if (state != null
                && state.enteredBattleByXiuluo()
                && state.combatSource() == XiuluoCombatSource.TRACKER_CONFIRM) {
            return AutoCombatService.PostCombatRecoveryPolicy.FAST_EXPECTED_EXIT;
        }
        if (state != null && state.combatSource() == XiuluoCombatSource.INCIDENTAL) {
            return AutoCombatService.PostCombatRecoveryPolicy.FULL_RECOVERY_WITH_LEADER_INCENSE;
        }
        return AutoCombatService.PostCombatRecoveryPolicy.FULL_RECOVERY_WITH_LEADER_INCENSE;
    }

    private XiuluoStepOutcome waitForTargetPathingWake(XiuluoStepOutcome outcome) {
        return waitForNavigationPathingWake(outcome, "xiuluo-v2:target", null);
    }

    private XiuluoStepOutcome waitForNavigationPathingWake(XiuluoStepOutcome outcome,
                                                           String sourcePrefix,
                                                           String targetMapName) {
        WindowPathingSnapshot snapshot = windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getPathingSnapshot)
                .orElse(null);
        WindowPathingIntent intent = snapshot == null ? null : snapshot.getIntent();
        return outcome.withWaitSpec(XiuluoWaitSpec.builder()
                .reason(XiuluoWaitReason.WAIT_TARGET_PATHING_TERMINAL)
                .wakeTypes(Set.of(WindowReadyEventType.PATHING_TERMINAL,
                        WindowReadyEventType.PREPARED_ACTION_READY))
                .afterSequence(windowReadyEventBus.currentSequence())
                .timeoutMs(WAIT_TARGET_PATHING_TERMINAL_TIMEOUT_MS)
                .pathingIntentId(intent == null ? null : intent.getIntentId())
                .pathingSourcePrefix(sourcePrefix)
                .pathingTargetMapName(targetMapName != null && !targetMapName.isBlank()
                        ? targetMapName
                        : intent == null ? null : intent.getTargetMapName())
                .build());
    }

    private XiuluoStepOutcome waitForTrackerShortcutWake(XiuluoStepOutcome outcome) {
        return waitForTrackerShortcutWake(outcome, windowReadyEventBus.currentSequence());
    }

    /**
     * CR253: PATHING_TERMINAL is deliberately NOT a wake type any more — a stop only feeds the
     * background stop-static pipeline. The foreground wakes for typed prepared work
     * (PREPARED_ACTION_READY covers both the local kanda prepared action and the background
     * prepared jobs) and for combat state changes before the enter-battle click.
     *
     * @param afterSequence ready-event sequence the park filters on. A producer that publishes a
     *                      prepared-ready wake in the SAME turn (二轮复审 P1-2: the click-failed
     *                      fallback job) captures this value BEFORE publishing, so its own wake is
     *                      newer than the filter and ends the park in a fresh command cycle.
     */
    private XiuluoStepOutcome waitForTrackerShortcutWake(XiuluoStepOutcome outcome, long afterSequence) {
        return outcome.withWaitSpec(XiuluoWaitSpec.builder()
                .reason(XiuluoWaitReason.WAIT_TRACKER_SHORTCUT_PATHING)
                .wakeTypes(Set.of(WindowReadyEventType.PREPARED_ACTION_READY,
                        WindowReadyEventType.COMBAT_STATE_CHANGED))
                .afterSequence(afterSequence)
                .timeoutMs(WAIT_TARGET_PATHING_TERMINAL_TIMEOUT_MS)
                .pathingSourcePrefix("xiuluo-v2:tracker-shortcut")
                .build());
    }

    private XiuluoStepOutcome consumePreparedXiuluoEnterBattle(TaskExecutionContext context,
                                                               XiuluoRoundContext state,
                                                               String reason) {
        return consumePreparedXiuluoEnterBattle(context, state, null, reason);
    }

    /**
     * CR256 direct consume at a green-chain park wake: if the parked outcome still belongs to the
     * WAIT_TRACKER_SHORTCUT_PATHING wait, is non-terminal, and carries a concrete attemptId, try to
     * consume the local kanda prepared action right now — the same execution-safety validation
     * (window binding, attempt identity, one-shot rule, live template re-verification) and the same
     * single atomic InputSequences click as the phase consumer. No cloud step is requested before
     * the click; the returned outcome (clicked -> WAIT_COMBAT pending confirm, physical failure ->
     * preparedEnterBattleFailed) is reported to the cloud, which alone decides the next phase.
     *
     * @return the consume outcome, or null when nothing was consumable (the caller's unchanged
     *         report/execute path then runs — zero behavior difference on a miss).
     */
    private XiuluoStepOutcome maybeDirectConsumePreparedEnterBattleAfterWake(TaskExecutionContext context,
                                                                             XiuluoStepOutcome parkedOutcome) {
        if (parkedOutcome == null
                || parkedOutcome.transactionResult() == TaskTransactionResult.FAILED
                || parkedOutcome.transactionResult() == TaskTransactionResult.STOPPED
                || parkedOutcome.nextState() == null
                || parkedOutcome.nextState().phase() != XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING) {
            return null;
        }
        XiuluoRoundContext state = parkedOutcome.nextState();
        if (state.shortcutPathingIntentId() == null) {
            // No concrete attempt (e.g. pause-resume hot-start rebuild): let the phase re-run
            // normally instead of consuming under a weaker identity.
            return null;
        }
        /*
         * CR256 review P2: contract #2 authorizes ONLY the local kanda action — the prepared shape
         * whose matched text carries the local 看打 template hard evidence (its consume validator
         * re-runs the live template match). Any other XIULUO_ENTER_BATTLE prepared shape is not a
         * pre-authorized execution command and must go through the normal phase re-entry.
         */
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        PreparedDialogAction candidate = runtime == null ? null : runtime.getPreparedDialogAction();
        if (candidate == null || !dialogService.isXiuluoEnterBattleLocalKandaAction(candidate)) {
            return null;
        }
        return consumePreparedXiuluoEnterBattle(context, state,
                state.shortcutPathingIntentId(), "wait-wake-direct-consume");
    }

    /**
     * CR253 review P1: the green-chain phase supplies {@code requiredAttemptId} so the local kanda
     * prepared obeys the same attempt-identity rule as the typed jobs — an action stamped for a
     * different (or no) attempt is discarded without input. The WAIT_COMBAT re-registration retry
     * passes {@code null} and keeps its existing operation/template semantics (no schedule is open
     * there, so its prepared actions are unstamped).
     */
    private XiuluoStepOutcome consumePreparedXiuluoEnterBattle(TaskExecutionContext context,
                                                               XiuluoRoundContext state,
                                                               String requiredAttemptId,
                                                               String reason) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return null;
        }
        if (state.enteredBattleByXiuluo()
                || gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            /*
             * CR232 one-shot consumption: enter-battle was already handled this round (or combat is
             * running). Any late-prepared result — local watcher republish or slower parallel
             * branch — must be discarded, never re-clicked.
             */
            runtime.clearPreparedDialogAction("xiuluo enter battle already handled; discard late result");
            runtime.clearDialogInterest("xiuluo enter battle already handled");
            log.info("[xiuluo-v2 shortcut] late enter-battle result discarded: reason={} enteredBattleByXiuluo={} round={}",
                    reason, state.enteredBattleByXiuluo(), state.round());
            return null;
        }
        if (requiredAttemptId != null) {
            PreparedDialogAction candidate = runtime.getPreparedDialogAction();
            if (candidate != null
                    && candidate.getOperation() == DialogOperation.XIULUO_ENTER_BATTLE
                    && !requiredAttemptId.equals(candidate.getIntentId())) {
                // Double invalidation gate, foreground half: stale/unstamped local kanda work of
                // another attempt is dropped here — never clicked (round-87 class).
                runtime.clearPreparedDialogAction(
                        "xiuluo enter battle prepared stale-attempt discarded: required=" + requiredAttemptId
                                + " stamped=" + candidate.getIntentId());
                log.info("[xiuluo-v2 shortcut] stale-attempt local kanda prepared discarded: reason={} round={} requiredAttempt={} stampedAttempt={} source={}",
                        reason, state.round(), requiredAttemptId, candidate.getIntentId(),
                        candidate.getSource());
                return null;
            }
        }
        PreparedDialogAction action = runtime.consumePreparedDialogActionValidated(
                DialogOperation.XIULUO_ENTER_BATTLE,
                OPTION_ENTER_BATTLE,
                "xiuluo-v2:" + reason,
                prepared -> dialogService.validatePreparedDialogActionForConsume(
                        prepared, "xiuluo-v2:" + reason));
        if (action == null) {
            return null;
        }
        clearTrackerShortcutPathingIntent(runtime, "xiuluo enter battle prepared consumed");
        boolean clicked = inputSequences.moveAndClickLeft(
                "xiuluo-v2:preparedEnterBattle:" + state.round(),
                action.getAbsoluteX(), action.getAbsoluteY(), 80, 150);
        if (!clicked) {
            return XiuluoStepOutcome.failed(state, "prepared xiuluo enter battle click failed")
                    .withFact("preparedEnterBattleFailed", "true");
        }
        // Heartbeat review P1 (schedule) + follow-up P1 (interest): click success is NOT combat
        // entry — both the green-chain schedule AND the dialog interest stay open so the Runner's
        // local kanda2 probe keeps covering a click that fails to enter combat. Cleanup of both is
        // owned by the confirmed IN_COMBAT boundary (clearXiuluoDialogStateOnCombatEntry) and the
        // stop/failure/new-attempt boundaries; a late prepared is discarded by the existing
        // WAIT_COMBAT / combat-entry one-shot guard, not by closing the probe early.
        closeTeamPathingMaintenanceWindow(context, state, "shortcut-enter-battle-prepared");
        autoCombatService.initializeForCurrentWindow();
        autoCombatService.authorizeCombatDetectionAfterEnterBattleAction(
                "xiuluo-v2:prepared-enter-battle-clicked:" + reason);
        log.info("[xiuluo-v2 shortcut] prepared enter-battle consumed: round={} click=({}, {}) matched={} source={}",
                state.round(), action.getAbsoluteX(), action.getAbsoluteY(), action.getMatchedText(), action.getSource());
        return XiuluoStepOutcome.sharedState(
                state.withPendingEnterBattleConfirm(XiuluoPhase.WAIT_COMBAT, XiuluoCombatSource.TRACKER_CONFIRM,
                        "shortcut-enter-battle-confirmed"),
                "shortcut enter-battle prepared action consumed");
    }

    private XiuluoStepOutcome waitForCombatStateWake(XiuluoStepOutcome outcome, long afterSequence) {
        return outcome.withWaitSpec(XiuluoWaitSpec.builder()
                .reason(XiuluoWaitReason.WAIT_COMBAT_STATE_CHANGE)
                .wakeTypes(Set.of(WindowReadyEventType.COMBAT_STATE_CHANGED))
                .afterSequence(afterSequence)
                .timeoutMs(combatMaintenanceWakeTimeoutMs())
                .build());
    }

    private long combatMaintenanceWakeTimeoutMs() {
        long nextCombatWakeDelayMs = autoCombatService.nextCombatWakeDelayMs();
        long timeoutMs = nextCombatWakeDelayMs < 0L
                ? WAIT_COMBAT_MAINTENANCE_WAKE_MAX_TIMEOUT_MS
                : Math.min(nextCombatWakeDelayMs, WAIT_COMBAT_MAINTENANCE_WAKE_MAX_TIMEOUT_MS);
        timeoutMs = Math.max(timeoutMs, WAIT_COMBAT_MAINTENANCE_WAKE_MIN_TIMEOUT_MS);
        log.debug("[xiuluo-v2] WAIT_COMBAT maintenance wake armed: timeoutMs={} nextCombatWakeDelayMs={}",
                timeoutMs, nextCombatWakeDelayMs);
        return timeoutMs;
    }

    /**
     * Suppress unknown-combat recovery when the current bound window is still actively fighting.
     *
     * @param source diagnostic source for the stale exit signal.
     * @param state current 修罗 round state.
     * @return a wait outcome when recovery must be suppressed; empty when unknown-combat recovery may proceed.
     */
    private Optional<XiuluoStepOutcome> suppressUnknownCombatExitIfActiveCombat(String source,
                                                                               XiuluoRoundContext state) {
        GameContext.ActionState actionState = gameContext.getCurrentActionState();
        if (actionState != GameContext.ActionState.IN_COMBAT) {
            return Optional.empty();
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        Optional<WindowReadyEvent> latestCombatEvent = runtime == null
                ? Optional.empty()
                : windowReadyEventBus.latest(runtime.getWindowId(), WindowReadyEventType.COMBAT_STATE_CHANGED);
        long now = System.currentTimeMillis();
        long combatEventAgeMs = latestCombatEvent
                .map(event -> Math.max(0L, now - event.getCreatedAtMs()))
                .orElse(-1L);
        XiuluoRoundContext combatState = state.enteredBattleByXiuluo()
                ? state
                : state.withXiuluoBattleStarted(XiuluoPhase.WAIT_COMBAT, "stale-unknown-combat-exit-suppressed");
        log.warn("[xiuluo-v2] stale/contradictory unknown-combat exit suppressed: source={} window={} phase={} enteredBattleByXiuluo={} actionState={} latestCombatEventSeq={} latestCombatEventAgeMs={} latestCombatEventSource={} suppressedSource=unknown-combat-exit",
                source,
                currentWindowLabel(runtime),
                state.phase(),
                state.enteredBattleByXiuluo(),
                actionState,
                latestCombatEvent.map(WindowReadyEvent::getSequence).orElse(-1L),
                combatEventAgeMs,
                latestCombatEvent.map(WindowReadyEvent::getSource).orElse(null));
        long combatWaitAfterSequence = windowReadyEventBus.currentSequence();
        return Optional.of(waitForCombatStateWake(
                XiuluoStepOutcome.sharedState(combatState, "stale unknown-combat exit suppressed; wait for combat state")
                        .withFact("combatObserved", "true"),
                combatWaitAfterSequence));
    }

    private XiuluoStepOutcome resolveUnknownCombatExit(TaskExecutionContext context, XiuluoRoundContext state) {
        /*
         * Hot-start can enter WAIT_COMBAT while the player is already fighting. In that case the
         * combat may be unrelated to Xiuluo, or it may be a random encounter during navigation. Do
         * not use the return item until map/coordinate evidence says the player could have fought
         * the target, and the task panel no longer shows an active Xiuluo objective.
         */
        Optional<XiuluoStepOutcome> entrySuppressed = suppressUnknownCombatExitIfActiveCombat(
                "resolve-unknown-combat-entry", state);
        if (entrySuppressed.isPresent()) {
            return entrySuppressed.get();
        }
        NpcTarget objective = state.objective();
        LocationInfo current = playerStateService.syncMyPosition();
        if (objective != null && current != null) {
            if (!gameStateUtil.isSameMapName(current.mapName, objective.getMapName())) {
                log.info("[xiuluo-v2] unknown combat exit outside target map: current={} targetMap={}; continue navigation",
                        current, objective.getMapName());
                return XiuluoStepOutcome.continueTo(
                        state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, objective, "unknown-combat-map-mismatch"),
                        "unknown combat exit outside target map; continue navigation");
            }
            if (!gameStateUtil.isNearCoordinate(current.mapName, current.x, current.y,
                    objective.getMapName(), objective.getX(), objective.getY(),
                    UNKNOWN_COMBAT_TARGET_DISTANCE_TOLERANCE)) {
                log.info("[xiuluo-v2] unknown combat exit far from target: current={} target=({}, {}) tolerance={}; continue navigation",
                        current, objective.getX(), objective.getY(), UNKNOWN_COMBAT_TARGET_DISTANCE_TOLERANCE);
                return XiuluoStepOutcome.continueTo(
                        state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, objective, "unknown-combat-far-from-target"),
                        "unknown combat exit far from target; continue navigation");
            }
        }

        Optional<XiuluoStepOutcome> beforeTaskPanelSuppressed = suppressUnknownCombatExitIfActiveCombat(
                "resolve-unknown-combat-before-task-panel", state);
        if (beforeTaskPanelSuppressed.isPresent()) {
            return beforeTaskPanelSuppressed.get();
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

        Optional<XiuluoStepOutcome> beforeReturnSuppressed = suppressUnknownCombatExitIfActiveCombat(
                "resolve-unknown-combat-before-return", state);
        if (beforeReturnSuppressed.isPresent()) {
            return beforeReturnSuppressed.get();
        }
        return attemptVerifiedReturnAfterUnknownCombat(context, state);
    }

    private XiuluoStepOutcome attemptVerifiedReturnAfterUnknownCombat(TaskExecutionContext context,
                                                                      XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        Optional<XiuluoStepOutcome> returnSuppressed = suppressUnknownCombatExitIfActiveCombat(
                "unknown-combat-return-attempt", state);
        if (returnSuppressed.isPresent()) {
            return returnSuppressed.get();
        }
        ReturnHomeResult returnHome = useReturnItemAndVerifyStartMap(context, state.round(), "unknown-combat");
        if (returnHome == ReturnHomeResult.VERIFIED) {
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.WAIT_TEAM_RETURN, "unknown-combat-return-verified"),
                    "unknown combat exit; return item verified")
                    .withFact("returnHomeResult", "VERIFIED");
        }
        if (returnHome == ReturnHomeResult.STILL_IN_COMBAT) {
            return resumeWaitCombatAfterTrustedReturnCorrection(
                    state, "unknown-combat-return-attempt");
        }

        uiCleanerService.cleanUpAll();
        Optional<NpcTarget> activeObjective = tryReadObjectiveFromTaskPanel(
                context, state.source() + ":unknown-combat-return-attempt");
        if (activeObjective.isPresent()) {
            log.info("[xiuluo-v2] objective found after failed return attempt: target={}", activeObjective.get());
            return XiuluoStepOutcome.continueTo(
                    state.withObjective(XiuluoPhase.NAVIGATE_TO_TARGET, activeObjective.get(), "unknown-combat-objective-after-return"),
                    "unknown combat exit; objective found after return attempt");
        }

        log.warn("[xiuluo-v2] unknown combat exit could not verify return and no objective was found; restart accept chain");
        return XiuluoStepOutcome.continueTo(
                state.recoverTo(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "unknown-combat-return-unverified"),
                "unknown combat exit; return unverified, restart accept chain")
                .withFact("returnHomeResult", "UNVERIFIED");
    }

    private XiuluoStepOutcome returnHome(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        ReturnHomeResult returnHome = useReturnItemAndVerifyStartMap(context, state.round(), "known-combat");
        if (returnHome != ReturnHomeResult.VERIFIED) {
            if (returnHome == ReturnHomeResult.STILL_IN_COMBAT) {
                return resumeWaitCombatAfterTrustedReturnCorrection(
                        state, "known-combat-return-unverified");
            }
            if (returnHome == ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT) {
                if (isXiuluoBrainLoopEnabled()) {
                    return XiuluoStepOutcome.failed(state,
                            "return item used but start map not verified and trusted combat is not active; report to XIULUO_BRAIN before cleanup/fallback");
                }
                uiCleanerService.cleanUpAll();
                log.warn("[xiuluo-v2] return item was used but start map was not verified and trusted combat state is not active; navigate back by normal path");
                return XiuluoStepOutcome.continueTo(
                        state.next(XiuluoPhase.NAVIGATE_BACK_TO_START, "return-item-used-unverified"),
                        "return item used but not verified; navigate back by normal path");
            }
            Optional<XiuluoStepOutcome> stillInCombat = correctKnownCombatReturnFailureIfStillInCombat(
                    context, state, "known-combat-return-unverified");
            if (stillInCombat.isPresent()) {
                return stillInCombat.get();
            }
            return recoverReturnHomeFailure(context, state);
        }
        clearTrackerShortcutPathingIntent(context.getWindowRuntimeContext(), "xiuluo-v2:return-home-verified");
        commonBoxService.detectLeaderBoxAfterReturnHome(context, "xiuluo_v2",
                "xiuluo-v2:return-home-verified");
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.WAIT_TEAM_RETURN, "return-item-verified"),
                "return item verified");
    }

    private Optional<XiuluoStepOutcome> correctKnownCombatReturnFailureIfStillInCombat(
            TaskExecutionContext context,
            XiuluoRoundContext state,
            String source) {
        AutoCombatService.TickResult trustedState = probeTrustedCombatStateAfterReturnVerificationFailure(
                context, source);
        if (trustedState != AutoCombatService.TickResult.IN_COMBAT) {
            log.warn("[xiuluo-v2] expected combat return verification failed and trusted combat state is not active: source={} trustedState={} phase={} combatSource={} enteredBattleByXiuluo={}",
                    source, trustedState, state.phase(), state.combatSource(), state.enteredBattleByXiuluo());
            // CR252: trusted non-combat is the confirmed round exit; see useReturnItemAndVerifyStartMap.
            taskMaintenanceService.confirmTeamCombatPhaseExitedForLeader(context,
                    "xiuluo-v2:trusted-not-in-combat:" + source);
            autoCombatService.revokeCombatDetectionAuthority(
                    "xiuluo-v2:trusted-not-in-combat:" + source);
            return Optional.empty();
        }
        return Optional.of(resumeWaitCombatAfterTrustedReturnCorrection(state, source));
    }

    private AutoCombatService.TickResult probeTrustedCombatStateAfterReturnVerificationFailure(
            TaskExecutionContext context,
            String source) {
        AutoCombatService.TickResult trustedState = autoCombatService.probeWindowCombatStateReadOnly(
                context, "xiuluo-v2:" + source);
        if (trustedState == AutoCombatService.TickResult.IN_COMBAT) {
            autoCombatService.refreshFastExpectedExitBaselineAfterTrustedInCombat(
                    "xiuluo-v2:" + source + ":trusted-in-combat");
        }
        return trustedState;
    }

    private XiuluoStepOutcome resumeWaitCombatAfterTrustedReturnCorrection(XiuluoRoundContext state,
                                                                           String source) {
        /*
         * FAST_EXPECTED_EXIT is only a shortcut. If the return item does not get us to 灵兽村 and
         * the trusted radar still says combat is active, treat the avatar-diff exit as stale and
         * go back to WAIT_COMBAT without consuming deferred leader recovery.
         */
        log.warn("[xiuluo-v2] expected combat return verification failed but trusted combat state is still IN_COMBAT; resume WAIT_COMBAT: source={} phase={} combatSource={} enteredBattleByXiuluo={}",
                source, state.phase(), state.combatSource(), state.enteredBattleByXiuluo());
        long afterSequence = windowReadyEventBus.currentSequence();
        XiuluoRoundContext combatState = state.withCombatSource(
                XiuluoPhase.WAIT_COMBAT,
                state.combatSource() == null || state.combatSource() == XiuluoCombatSource.NONE
                        ? XiuluoCombatSource.TRACKER_CONFIRM
                        : state.combatSource(),
                source + "-still-in-combat");
        return waitForCombatStateWake(
                XiuluoStepOutcome.sharedState(combatState,
                        "return verification failed but combat is still active; wait for real exit"),
                afterSequence);
    }

    private void consumeDeferredPostCombatRecoveryDuringNextTaskProgress(TaskExecutionContext context, String source) {
        if (!autoCombatService.hasPendingLeaderPostCombatRecoveryForCurrentWindow()) {
            return;
        }
        autoCombatService.consumePendingLeaderPostCombatRecoveryIfAllowed(context, source);
    }

    private void consumeCommonBoxDuringNextTaskProgress(TaskExecutionContext context, String source) {
        commonBoxService.consumePendingBoxIfAllowed(context, "xiuluo_v2", source);
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
        /*
         * CR244: the dead leader's walk back must not own the task turn. Remove the synchronous
         * keep-turn, park on this intent's own PATHING_TERMINAL after the mini-map click, and run
         * Gate A (WAIT_TEAM_RETURN reads the member pending-return set) once 灵兽村 is reached.
         */
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(ACCEPT_NPC.getMapName())
                .targetX(ACCEPT_NPC.getX())
                .targetY(ACCEPT_NPC.getY())
                .targetName(ACCEPT_NPC.getName())
                .source("xiuluo-v2:returnFallback")
                .build());
        XiuluoStepOutcome outcome = navigationOutcome(
                activeState, result, XiuluoPhase.WAIT_TEAM_RETURN, "navigate back to start");
        if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
            if (isXiuluoBrainLoopEnabled()) {
                return XiuluoStepOutcome.failed(activeState,
                        "return fallback navigation failed; report to XIULUO_BRAIN before cleanup/retry");
            }
            uiCleanerService.cleanUpAll();
            return retryCurrentOrRecover(activeState, XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC,
                    "return fallback navigation failed");
        }
        if (outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED) {
            return waitForNavigationPathingWake(outcome, "xiuluo-v2:returnFallback", ACCEPT_NPC.getMapName());
        }
        return outcome;
    }

    private XiuluoStepOutcome waitTeamReady(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * Under-three is not a broken click/navigation phase; the game explicitly says the team is
         * not eligible to accept Xiuluo yet. Keep the leader task alive in a pending loop, close the
         * prompt, release the task turn, then retry accepting after the team-readiness poll delay.
         */
        log.warn("[xiuluo-v2] under-three team pending: clean prompt and retry accept later");
        uiCleanerService.cleanUpAll();
        return XiuluoStepOutcome.sharedState(
                state.next(XiuluoPhase.ACCEPT_TASK_CLICK_NPC, UNDER_THREE_WAIT_SOURCE_PREFIX + ":retry-accept"),
                "under-three team pending; retry accept after wait");
    }

    private XiuluoStepOutcome waitTeamReturn(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * CR244 Gate A: the member-owned pending-return set replaces the old leader-side 招 template
         * precheck/poll. The leader only reads emptiness here — it never scans member windows and
         * never opens/closes member capabilities for the return flow. Capture the event-bus sequence
         * BEFORE reading the set so an add/remove between the read and the park cannot be missed.
         * In-game few-member prompts (under-five/under-three) stay owned by the existing accept
         * dialog business and are never faked as "members returned".
         */
        long afterSequence = windowReadyEventBus.currentSequence();
        int pendingCount = taskMaintenanceService.pendingTeamReturnWindowCount(context);
        if (pendingCount > 0) {
            log.info("[xiuluo-v2] team return pending set non-empty; park for member state change: source={} pendingCount={} afterSequence={}",
                    state.source(), pendingCount, afterSequence);
            return XiuluoStepOutcome.sharedState(
                            state.next(XiuluoPhase.WAIT_TEAM_RETURN, keepTeamReturnWaitSource(state)),
                            "team return still pending; wait for member pending-return change")
                    .withFact("teamReturnWaitSpecArmed", "true")
                    .withFact("teamReturnPendingCount", Integer.toString(pendingCount))
                    .withWaitSpec(XiuluoWaitSpec.builder()
                            .reason(XiuluoWaitReason.WAIT_TEAM_RETURN_STATE_CHANGE)
                            .wakeTypes(Set.of(WindowReadyEventType.TEAM_RETURN_STATE_CHANGED))
                            .afterSequence(afterSequence)
                            .timeoutMs(TEAM_RETURN_STATE_WAKE_TIMEOUT_MS)
                            .build());
        }
        if (TEAM_RETURN_BEFORE_ACCEPT_SOURCE.equals(state.source())) {
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "team-return-ready-before-accept"),
                    "team return ready before accept flow");
        }
        if (TEAM_RETURN_GATE_B_SOURCE.equals(state.source())) {
            /*
             * Gate B wake-up path: never trust the pre-park click point. Go back to the accept
             * dialog phase so the existing template/cloud validation re-establishes the current
             * option before Gate B runs again ahead of the atomic click.
             */
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.ACCEPT_TASK_DIALOG, "team-return-gate-b-cleared"),
                    "team return cleared at Gate B; re-validate accept option before clicking");
        }
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.ROUND_DONE, "team-return-not-needed"),
                "team return wait not needed");
    }

    private String keepTeamReturnWaitSource(XiuluoRoundContext state) {
        if (TEAM_RETURN_BEFORE_ACCEPT_SOURCE.equals(state.source())) {
            return TEAM_RETURN_BEFORE_ACCEPT_SOURCE;
        }
        if (TEAM_RETURN_GATE_B_SOURCE.equals(state.source())) {
            return TEAM_RETURN_GATE_B_SOURCE;
        }
        return TEAM_RETURN_ROUND_DONE_SOURCE;
    }

    /**
     * CR244 Gate B: last pending-return read right before the accept-option atomic input. A blocked
     * gate never cancels already-submitted input (there is none yet), never re-clicks the NPC, and
     * never reuses the pre-park click point: the wake path re-enters ACCEPT_TASK_DIALOG so the
     * existing template/cloud validation re-establishes the option before this gate runs again.
     *
     * @param state current accept-dialog round state.
     * @param source diagnostic click-chain source (local template / cloud prepared).
     * @return park outcome when members are still pending, or empty to submit the click now.
     */
    private Optional<XiuluoStepOutcome> gateBlockAcceptOptionClickForTeamReturn(TaskExecutionContext context,
                                                                                XiuluoRoundContext state,
                                                                                String source) {
        long afterSequence = windowReadyEventBus.currentSequence();
        int pendingCount = taskMaintenanceService.pendingTeamReturnWindowCount(context);
        if (pendingCount <= 0) {
            return Optional.empty();
        }
        log.warn("[xiuluo-v2] Gate B blocked accept option click; members still pending return: source={} pendingCount={} afterSequence={}",
                source, pendingCount, afterSequence);
        return Optional.of(XiuluoStepOutcome.sharedState(
                        state.next(XiuluoPhase.WAIT_TEAM_RETURN, TEAM_RETURN_GATE_B_SOURCE),
                        "accept option click blocked at Gate B; members still pending return")
                .withFact("teamReturnWaitSpecArmed", "true")
                .withFact("teamReturnPendingCount", Integer.toString(pendingCount))
                .withWaitSpec(XiuluoWaitSpec.builder()
                        .reason(XiuluoWaitReason.WAIT_TEAM_RETURN_STATE_CHANGE)
                        .wakeTypes(Set.of(WindowReadyEventType.TEAM_RETURN_STATE_CHANGED))
                        .afterSequence(afterSequence)
                        .timeoutMs(TEAM_RETURN_STATE_WAKE_TIMEOUT_MS)
                        .build()));
    }

    private XiuluoStepOutcome recoverAcceptNavigationFailure(XiuluoRoundContext state) {
        /*
         * The accept NPC route is already the normal navigation path. On failure, only remove
         * possible UI blockers and retry the same phase once; do not invent a separate navigation
         * mode here.
         */
        if (isXiuluoBrainLoopEnabled()) {
            return XiuluoStepOutcome.failed(state,
                    "accept NPC navigation failed; report to XIULUO_BRAIN before cleanup/retry");
        }
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
        XiuluoRoundContext acceptDialogState = state.next(
                XiuluoPhase.ACCEPT_TASK_DIALOG,
                "accept-click-failed-local-dialog-check");
        scheduleAcceptDialogCloudFallbackIfAbsent(acceptDialogState, "accept-click-failed");
        Optional<XiuluoStepOutcome> knownDialog = handleKnownXiuluoOptionDialog(
                context, acceptDialogState, "xiuluo-v2:accept-click-failed:" + state.source(), false);
        if (knownDialog.isPresent()) {
            log.info("[xiuluo-v2] accept NPC click reported false, but a known Xiuluo dialog was handled");
            return knownDialog.get();
        }
        if (isXiuluoBrainLoopEnabled()) {
            return XiuluoStepOutcome.continueTo(
                    acceptDialogState,
                    "accept NPC click unverified; local accept template missed, delegate accept dialog fallback");
        }
        Optional<XiuluoStepOutcome> blockedDialog = handleUnderThreeBlockedDialog(
                context, state, "xiuluo-v2:accept-click-under-three:" + state.source());
        if (blockedDialog.isPresent()) {
            return blockedDialog.get();
        }

        DialogResult inspectResult = dialogService.handleDialog(DialogHandleRequest.builder()
                .sourceTask("xiuluo-v2:accept-click-failed:inspect:" + state.source())
                .operation(DialogOperation.CLEANUP)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.IGNORE)
                .build());
        if (inspectResult.getStatus() == DialogResultStatus.STORY_IGNORED) {
            log.info("[xiuluo-v2] accept NPC click reported false, but story objective is already visible");
            return continueAfterAcceptOptionClicked(state, "story-already-open");
        }
        uiCleanerService.cleanUpAll();
        if (state.phaseRetryCount() < MAX_PHASE_RETRY) {
            toggleMountBeforeClickRetry(state, "accept NPC click failed");
            return XiuluoStepOutcome.continueTo(
                    state.retrySamePhase("retry:" + state.phase()),
                    "accept NPC click failed; retry current phase");
        }
        if (isXiuluoBrainLoopEnabled()) {
            return XiuluoStepOutcome.failed(state,
                    "accept NPC dialog local memory/template failed after cleanup retry; report outcome to XIULUO_BRAIN");
        }
        return retryCurrentOrRecover(state, XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC,
                "accept NPC click failed");
    }

    private XiuluoStepOutcome recoverAcceptDialogFailure(XiuluoRoundContext state) {
        if (isXiuluoBrainLoopEnabled()) {
            return XiuluoStepOutcome.failed(state,
                    "accept dialog option not matched; report to XIULUO_BRAIN before cleanup/retry");
        }
        uiCleanerService.cleanUpAll();
        return retryCurrentOrRecover(state, XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC,
                "accept dialog option not matched");
    }

    private XiuluoStepOutcome recoverBackgroundObjectiveReadFailure(TaskExecutionContext context,
                                                                    XiuluoRoundContext state,
                                                                    String reason) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * CR56 makes READ_OBJECTIVE consume only the accept-time background parse. A miss here must
         * leave the phase through accept recovery instead of taking another screenshot or detecting
         * the same dialog again in-place.
         */
        if (isXiuluoBrainLoopEnabled()) {
            return XiuluoStepOutcome.failed(state, reason + "; report to XIULUO_BRAIN before cleanup/retry");
        }
        uiCleanerService.closeAllGenericWindows();
        return retryCurrentOrRecover(state, XiuluoPhase.ACCEPT_TASK_CLICK_NPC, reason);
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
        NpcTarget objective = state.objective();
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
            if (objective != null) {
                npcClickService.confirmPendingSmartClick(
                        objective.getMapName(),
                        objective.getName(),
                        objective.getX(),
                        objective.getY(),
                        "DIALOG_TEMPLATE", "xiuluo-v2:target-click-failed:" + state.source() + ":option consumed");
            }
            return enterBattleFromRecoveredDialog(context, state, "battle-confirmed-template-recovery");
        }
        log.info("[xiuluo-v2] normal enter-battle template missed; try wild-monster cancel template: source={} status={}",
                state.source(), templateResult.getStatus());
        DialogResult wildMonsterCancelResult = dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
                "xiuluo-v2:wild-monster-cancel:" + state.source(),
                List.of(new GreenTemplateClickSpec(OPTION_WILD_MONSTER_CANCEL, XIULUO_WILD_MONSTER_CANCEL_TEMPLATE, -6, 6, 4)),
                false));
        if (OPTION_WILD_MONSTER_CANCEL.equals(wildMonsterCancelResult.getActionKey())) {
            log.info("[xiuluo-v2] wild-monster cancel template clicked; retry target click: source={} click=({}, {})",
                    state.source(), wildMonsterCancelResult.getAbsoluteX(), wildMonsterCancelResult.getAbsoluteY());
            return retryCurrentOrRecover(state, XiuluoPhase.CLICK_TARGET_NPC,
                    "wild-monster cancel dialog closed; retry target click");
        }
        log.info("[xiuluo-v2] wild-monster cancel template missed; continue old enter-battle recovery: source={} status={}",
                state.source(), wildMonsterCancelResult.getStatus());

        DialogResult keywordResult = dialogService.handleDialog(DialogHandleRequest.handleKeywordOption(
                "xiuluo-v2:enter-battle-ocr:" + state.source(), "看打", false));
        if (keywordResult.getStatus() == DialogResultStatus.OPTION_KEYWORD_CLICKED) {
            log.info("[xiuluo-v2] enter-battle option clicked by OCR fallback: point=({}, {}) text={}",
                    keywordResult.getAbsoluteX(), keywordResult.getAbsoluteY(), keywordResult.getMatchedText());
            if (objective != null) {
                npcClickService.confirmPendingSmartClick(
                        objective.getMapName(),
                        objective.getName(),
                        objective.getX(),
                        objective.getY(),
                        "DIALOG_OCR", "xiuluo-v2:enter-battle-ocr:" + state.source() + ":option consumed");
            }
            return enterBattleFromRecoveredDialog(context, state, "battle-confirmed-ocr");
        }

        if (objective != null) {
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
                    .source("xiuluo-v2:directCombat:" + objective.getSource())
                    .build();
            DirectCombatClickResult directCombat = npcClickService.tryDirectCombatTargetClick(
                    combatTarget.toClickRequest(gameContext.getMe(), TaskType.XIULUO_V2));
            if (directCombat.combatEntered()) {
                return enterBattleFromDirectCombatClick(context, state, "direct-combat-click");
            }
            if (directCombat.positionRefreshRequired()) {
                log.warn("[xiuluo-v2] direct-combat failed after Alt+A; rerun target navigation before retry: reason={} objective={}",
                        directCombat.reason(), objective);
                return recoverOrFail(
                        state.recoverToWithObjective(XiuluoPhase.NAVIGATE_TO_TARGET, objective,
                                "direct-combat-position-refresh-required"),
                        "direct combat failed after Alt+A; recover to target navigation");
            }
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        }
        uiCleanerService.cleanUpAll();
        if (state.phaseRetryCount() < MAX_PHASE_RETRY) {
            toggleMountBeforeClickRetry(state, "target click failed");
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

    private XiuluoStepOutcome enterBattleFromDirectCombatClick(TaskExecutionContext context,
                                                               XiuluoRoundContext state,
                                                               String reason) {
        NpcTarget objective = state.objective();
        if (objective != null) {
            npcClickService.confirmPendingSmartClick(
                    objective.getMapName(),
                    objective.getName(),
                    objective.getX(),
                    objective.getY(),
                    "BATTLE_RADAR", "xiuluo-v2:" + reason + ":combat radar confirmed");
        }
        autoCombatService.initializeForCurrentWindow();
        autoCombatService.authorizeCombatDetectionAfterEnterBattleAction(
                "xiuluo-v2:direct-combat-entered:" + reason);
        TaskSleep.sleepOrStop(context, 1200L, "Xiuluo V2 task interrupted");
        return XiuluoStepOutcome.sharedState(
                state.next(XiuluoPhase.WAIT_COMBAT, reason),
                "direct combat click confirmed by battle radar");
    }

    private XiuluoStepOutcome enterBattleFromRecoveredDialog(TaskExecutionContext context,
                                                             XiuluoRoundContext state,
                                                             String reason) {
        autoCombatService.initializeForCurrentWindow();
        autoCombatService.authorizeCombatDetectionAfterEnterBattleAction(
                "xiuluo-v2:recovered-enter-battle-clicked:" + reason);
        TaskSleep.sleepOrStop(context, 1200L, "Xiuluo V2 task interrupted");
        /*
         * Recovery only proves that we clicked a known battle option. Do not mark the Xiuluo
         * combat as started until WAIT_COMBAT sees the battle radar; otherwise a dropped click can
         * strand the task in WAIT_COMBAT just like the normal confirm path used to do.
         */
        return XiuluoStepOutcome.sharedState(
                state.withPendingEnterBattleConfirm(XiuluoPhase.WAIT_COMBAT, XiuluoCombatSource.TRACKER_CONFIRM,
                        reason + ":confirm-clicked"),
                "enter battle option clicked by recovery");
    }

    private XiuluoStepOutcome recoverEnterBattleConfirmFailure(XiuluoRoundContext state) {
        uiCleanerService.cleanUpAll();
        return retryCurrentOrRecover(state, XiuluoPhase.CLICK_TARGET_NPC,
                "enter battle option not matched");
    }

    private XiuluoStepOutcome recoverReturnHomeFailure(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        if (isXiuluoBrainLoopEnabled()) {
            return XiuluoStepOutcome.failed(state,
                    "return item not found or not used; report to XIULUO_BRAIN before cleanup/retry");
        }
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

    private boolean tryUseStartupReturnItemOnce(TaskExecutionContext context, String source) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        log.info("[xiuluo-v2] startup-return-item task-page probe start: source={} template={}",
                source, RETURN_ITEM_TEMPLATE);
        boolean used = bagService.findAndUseMainBagTaskPageItem(RETURN_ITEM_TEMPLATE, context);
        if (!used) {
            log.info("[xiuluo-v2] startup-return-item not found on task page: source={} template={}",
                    source, RETURN_ITEM_TEMPLATE);
            return false;
        }

        /*
         * Startup fallback is intentionally lightweight: one task-page probe only. If the click
         * succeeds, still verify that the item actually returned the leader to 灵兽村 before
         * trusting the shortcut path.
         */
        TaskSleep.sleepOrStop(context, RETURN_VERIFY_DELAY_MS, "Xiuluo V2 task interrupted");
        LocationInfo afterReturn = playerStateService.syncMyPosition();
        boolean verified = afterReturn != null && gameStateUtil.isSameMapName(afterReturn.mapName, START_MAP_NAME);
        log.info("[xiuluo-v2] startup-return-item verify result: source={} used={} verified={} location={} targetMap={}",
                source, used, verified, afterReturn, START_MAP_NAME);
        if (verified) {
            recordStartMapVerifiedLocation(afterReturn, "startup-return-verify:" + source);
        }
        return verified;
    }

    private void recordStartMapVerifiedLocation(LocationInfo location, String source) {
        if (location == null || !gameStateUtil.isSameMapName(location.mapName, START_MAP_NAME)) {
            return;
        }
        lastStartMapVerifiedLocation = location;
        lastStartMapVerifiedAtMs = System.currentTimeMillis();
        log.info("[xiuluo-v2] start-map verified location recorded for accept navigation reuse: source={} location={}",
                source, location);
    }

    private void clearStartMapVerifiedLocation(String source) {
        if (lastStartMapVerifiedLocation == null) {
            return;
        }
        log.info("[xiuluo-v2] start-map accept snapshot consumed: source={} location={}",
                source, lastStartMapVerifiedLocation);
        lastStartMapVerifiedLocation = null;
        lastStartMapVerifiedAtMs = 0L;
    }

    private ReturnItemUseResult useReturnItem(TaskExecutionContext context,
                                              int round,
                                              String source,
                                              int attempt,
                                              int maxAttempts) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        log.info("[xiuluo-v2] use return item and verify start map: source={} attempt={}/{}",
                source, attempt, maxAttempts);
        boolean usedCached = returnItemPrescanService.useCached(context, TASK_CODE, round, RETURN_ITEM_TEMPLATE,
                ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                "xiuluo-v2:return-home:" + source + ":attempt-" + attempt);
        if (usedCached) {
            TaskSleep.sleepOrStop(context, RETURN_VERIFY_DELAY_MS, "Xiuluo V2 task interrupted");
            LocationInfo cachedReturn = playerStateService.syncMyPosition();
            if (cachedReturn != null && gameStateUtil.isSameMapName(cachedReturn.mapName, START_MAP_NAME)) {
                log.info("[xiuluo-v2] cached return item verified: source={} location={}", source, cachedReturn);
                recordStartMapVerifiedLocation(cachedReturn, "cached-return-verified:" + source);
                returnItemPrescanService.completeRound(context, TASK_CODE, round, RETURN_ITEM_TEMPLATE,
                        "xiuluo-v2:cached-return-verified");
                autoCombatService.reconcileReturnHomeVerifiedCombatState(
                        context, TASK_CODE, START_MAP_NAME,
                        "xiuluo-v2:cached-return-verified:" + source + ":attempt-" + attempt);
                return ReturnItemUseResult.verified(cachedReturn);
            }
            log.warn("[xiuluo-v2] cached return item used but start map not verified; run trusted combat probe before any further return attempt: source={} location={}",
                    source, cachedReturn);
            returnItemPrescanService.invalidate(context, TASK_CODE, round, RETURN_ITEM_TEMPLATE,
                    "xiuluo-v2:cached-return-unverified:" + source);
            return ReturnItemUseResult.usedStartMapUnverified(cachedReturn);
        }

        boolean used = bagService.findAndUseMainBagTaskPageItem(RETURN_ITEM_TEMPLATE, context);
        if (!used) {
            log.warn("[xiuluo-v2] return item not found/used: source={} attempt={}/{}",
                    source, attempt, maxAttempts);
            return ReturnItemUseResult.notUsed();
        }

        /*
         * A bag click only proves the item was clicked. The task contract is stronger: after using
         * the Xiuluo return item, the current map must become 灵兽村 before the next phase can trust
         * that the round has returned to the task giver area.
         */
        TaskSleep.sleepOrStop(context, RETURN_VERIFY_DELAY_MS, "Xiuluo V2 task interrupted");
        LocationInfo afterReturn = playerStateService.syncMyPosition();
        if (afterReturn != null && gameStateUtil.isSameMapName(afterReturn.mapName, START_MAP_NAME)) {
            log.info("[xiuluo-v2] return item verified: source={} location={}", source, afterReturn);
            recordStartMapVerifiedLocation(afterReturn, "return-item-verified:" + source);
            returnItemPrescanService.completeRound(context, TASK_CODE, round, RETURN_ITEM_TEMPLATE,
                    "xiuluo-v2:return-home-verified");
            autoCombatService.reconcileReturnHomeVerifiedCombatState(
                    context, TASK_CODE, START_MAP_NAME,
                    "xiuluo-v2:return-home-verified:" + source + ":attempt-" + attempt);
            return ReturnItemUseResult.verified(afterReturn);
        }
        log.warn("[xiuluo-v2] return item used but start map not verified: source={} location={}",
                source, afterReturn);
        return ReturnItemUseResult.usedStartMapUnverified(afterReturn);
    }

    private ReturnHomeResult useReturnItemAndVerifyStartMap(TaskExecutionContext context,
                                                            int round,
                                                            String source) {
        for (int attempt = 1; attempt <= RETURN_ITEM_VERIFY_ATTEMPTS; attempt++) {
            ReturnItemUseResult result = useReturnItem(context, round, source, attempt, RETURN_ITEM_VERIFY_ATTEMPTS);
            if (result.verifiedStartMap()) {
                return ReturnHomeResult.VERIFIED;
            }
            if (result.usedStartMapUnverified()) {
                AutoCombatService.TickResult trustedState = probeTrustedCombatStateAfterReturnVerificationFailure(
                        context, source + ":attempt-" + attempt);
                if (trustedState == AutoCombatService.TickResult.IN_COMBAT) {
                    return ReturnHomeResult.STILL_IN_COMBAT;
                }
                // CR252: the correction chain's trusted non-combat verdict is this round's
                // confirmed final exit even though the return is unverified; broadcast the team
                // exit and end the detection authorization.
                taskMaintenanceService.confirmTeamCombatPhaseExitedForLeader(context,
                        "xiuluo-v2:trusted-not-in-combat:" + source);
                autoCombatService.revokeCombatDetectionAuthority(
                        "xiuluo-v2:trusted-not-in-combat:" + source);
                if (attempt < RETURN_ITEM_VERIFY_ATTEMPTS) {
                    if (isXiuluoBrainLoopEnabled()) {
                        log.warn("[xiuluo-v2] return item used but start map unverified after trusted non-combat; report to XIULUO_BRAIN before local retry: source={} attempt={}/{} trustedState={} location={}",
                                source, attempt, RETURN_ITEM_VERIFY_ATTEMPTS, trustedState, result.location());
                        return ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT;
                    }
                    uiCleanerService.cleanUpAll();
                    log.warn("[xiuluo-v2] return item used but start map unverified after trusted non-combat; retry return item before navigation fallback: source={} attempt={}/{} trustedState={} location={}",
                            source, attempt, RETURN_ITEM_VERIFY_ATTEMPTS, trustedState, result.location());
                    continue;
                }
                return ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT;
            }
            if (isXiuluoBrainLoopEnabled()) {
                return ReturnHomeResult.UNAVAILABLE;
            }
            uiCleanerService.cleanUpAll();
        }
        return ReturnHomeResult.UNAVAILABLE;
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
        if (state.phase() == XiuluoPhase.ACCEPT_TASK_DIALOG) {
            /*
             * CR236: 接任务禁止双记忆假成功。NPC 点位记忆命中只代表"尝试点了 NPC"，不能独立证明
             * 修罗接任务 dialog 已打开。因此这里不再把 accept option 记忆当第一快路径，第一硬证据
             * 必须是固定 ROI 模板（tryLocalAcceptTaskTemplateOption）；模板 miss 交云端 fallback
             * 做更重识别。只有模板或云端命中并点击成功，才允许 continueAfterAcceptOptionClicked。
             */
            Optional<XiuluoStepOutcome> localTemplateAccept =
                    tryLocalAcceptTaskTemplateOption(context, state, source);
            if (localTemplateAccept.isPresent()) {
                return localTemplateAccept;
            }
            Optional<XiuluoStepOutcome> cloudFallbackAccept =
                    tryAcceptTaskCloudFallbackOption(context, state, source);
            if (cloudFallbackAccept.isPresent()) {
                return cloudFallbackAccept;
            }
            /*
             * 接任务 option 的快路径只做固定 ROI 模板匹配。miss 以后不要再走通用
             * DialogService 绿色模板/keyword 分支；上层会把失败 outcome 交给云端判断是未弹框、
             * 模板 miss，还是应该重新点击 NPC。
             */
            return Optional.empty();
        }

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
                recordAcceptTaskOptionSuccess(source, result);
                npcClickService.confirmPendingSmartClick(
                        START_MAP_NAME,
                        ACCEPT_NPC_NAME,
                        ACCEPT_NPC_X,
                        ACCEPT_NPC_Y,
                        "DIALOG_TEMPLATE", source + ":accept option consumed");
                TaskSleep.sleepOrStop(context, 250L, "Xiuluo V2 task interrupted");
                yield Optional.of(continueAfterAcceptOptionClicked(state, "known-dialog:accept-task"));
            }
            case OPTION_ENTER_BATTLE -> {
                NpcTarget objective = state.objective();
                if (objective != null) {
                    npcClickService.confirmPendingSmartClick(
                            objective.getMapName(),
                            objective.getName(),
                            objective.getX(),
                            objective.getY(),
                            "DIALOG_TEMPLATE", source + ":enter battle option consumed");
                }
                yield Optional.of(enterBattleFromRecoveredDialog(context, state, "known-dialog:enter-battle"));
            }
            case OPTION_UNDER_FIVE_CONFIRM -> {
                npcClickService.confirmPendingSmartClick(
                        START_MAP_NAME,
                        ACCEPT_NPC_NAME,
                        ACCEPT_NPC_X,
                        ACCEPT_NPC_Y,
                        "DIALOG_TEMPLATE", source + ":under-five confirm consumed");
                TaskSleep.sleepOrStop(context, 250L, "Xiuluo V2 task interrupted");
                yield Optional.of(continueAfterAcceptOptionClicked(state, "known-dialog:under-five-confirm"));
            }
            case OPTION_UNDER_FIVE_WAIT -> {
                npcClickService.confirmPendingSmartClick(
                        START_MAP_NAME,
                        ACCEPT_NPC_NAME,
                        ACCEPT_NPC_X,
                        ACCEPT_NPC_Y,
                        "DIALOG_TEMPLATE", source + ":under-five wait consumed");
                yield Optional.of(XiuluoStepOutcome.sharedState(
                        state.next(XiuluoPhase.WAIT_TEAM_RETURN, "known-dialog:under-five-wait"),
                        "under-five prompt declined by config; wait for team"));
            }
            default -> Optional.empty();
        };
    }

    private void scheduleAcceptDialogCloudFallbackIfAbsent(XiuluoRoundContext state, String source) {
        CompletableFuture<Optional<PreparedDialogAction>> current = acceptDialogCloudFallbackFuture;
        if (current != null && acceptDialogCloudFallbackRound == state.round()) {
            return;
        }
        scheduleAcceptDialogCloudFallback(state, source);
    }

    private void scheduleAcceptDialogCloudFallback(XiuluoRoundContext state, String source) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        String prepareSource = "xiuluo-v2:accept-cloud-fallback:" + source + ":round-" + state.round();
        acceptDialogCloudFallbackRound = state.round();
        acceptDialogCloudFallbackFuture = CompletableFuture.supplyAsync(() ->
                windowTaskContextHolder.callWith(runtime, () -> {
                    long startedAt = System.currentTimeMillis();
                    try {
                        Optional<PreparedDialogAction> prepared = dialogService.prepareGreenTemplateOption(
                                prepareSource,
                                DialogOperation.ACCEPT_TASK,
                                acceptTaskCloudFallbackSpecs(),
                                false);
                        log.info("[xiuluo-v2] accept cloud fallback prepare completed: source={} round={} prepared={} elapsedMs={} window={}",
                                source, state.round(), prepared.isPresent(),
                                Math.max(0L, System.currentTimeMillis() - startedAt), currentWindowLabel(runtime));
                        return prepared;
                    } catch (RuntimeException e) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw e;
                        }
                        log.warn("[xiuluo-v2] accept cloud fallback prepare failed: source={} round={} elapsedMs={} window={}",
                                source, state.round(), Math.max(0L, System.currentTimeMillis() - startedAt),
                                currentWindowLabel(runtime), e);
                        return Optional.<PreparedDialogAction>empty();
                    }
                }));
        log.info("[xiuluo-v2] accept cloud fallback prepare scheduled: source={} round={} window={}",
                source, state.round(), currentWindowLabel(runtime));
    }

    private List<GreenTemplateClickSpec> acceptTaskCloudFallbackSpecs() {
        return List.of(new GreenTemplateClickSpec(
                OPTION_ACCEPT_TASK,
                ACCEPT_OPTION_TEMPLATE,
                ACCEPT_OPTION_TEMPLATE_CLICK_OFFSET_X,
                ACCEPT_OPTION_TEMPLATE_CLICK_OFFSET_X,
                0));
    }

    private Optional<XiuluoStepOutcome> tryAcceptTaskCloudFallbackOption(TaskExecutionContext context,
                                                                         XiuluoRoundContext state,
                                                                         String source) {
        CompletableFuture<Optional<PreparedDialogAction>> future = acceptDialogCloudFallbackFuture;
        if (future == null || acceptDialogCloudFallbackRound != state.round()) {
            return Optional.empty();
        }

        Optional<PreparedDialogAction> prepared;
        try {
            prepared = waitForAcceptCloudFallback(context, state, source, future);
            if (prepared == null) {
                return Optional.of(XiuluoStepOutcome.continueTo(
                        state.next(XiuluoPhase.ACCEPT_TASK_CLICK_NPC, "accept-cloud-fallback-timeout"),
                        "accept cloud fallback did not finish before TTL; retry NPC click"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.of(XiuluoStepOutcome.stopped(state, "accept cloud fallback wait interrupted"));
        } catch (ExecutionException e) {
            log.warn("[xiuluo-v2] accept cloud fallback future failed: source={} round={} reason={}",
                    source, state.round(), e.getCause() == null ? e.toString() : e.getCause().toString());
            clearAcceptDialogCloudFallback("future failed");
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("[xiuluo-v2] accept cloud fallback future failed before consume: source={} round={} reason={}",
                    source, state.round(), e.toString());
            clearAcceptDialogCloudFallback("future failed before consume");
            return Optional.empty();
        }

        if (prepared.isEmpty()) {
            /*
             * CR236 规则6：云端 fallback 完成但没有可执行接任务 option，说明本轮没有打开可识别的
             * 修罗接任务 dialog。降权接任务选项记忆，避免下一轮再用同一个旧点位假成功；回退到重新
             * 接任务由上层 recoverAcceptDialogFailure -> 云端 acceptDialogNext 负责，不进出村预寻路。
             * 说明：NPC 点位记忆（LEARNED_MEMORY）是云端所有，CR169 之后本地 confirmPendingSmartClick
             * 已是空操作，任务侧无可达杠杆直接降权它，本轮防假成功依赖上面"必须硬证据"的门禁。
             */
            memoryService.recordDialogChoiceFailure(
                    TASK_CODE, ACCEPT_TASK_ACTION, ACCEPT_NPC_NAME,
                    source + ":accept-cloud-fallback-no-option");
            clearAcceptDialogCloudFallback("cloud fallback empty");
            return Optional.empty();
        }
        PreparedDialogAction action = dialogService.validatePreparedDialogActionForConsume(
                prepared.get(), "xiuluo-v2:accept-cloud-prepared:" + source);
        if (action == null) {
            clearAcceptDialogCloudFallback("cloud fallback stale");
            return Optional.empty();
        }
        if (!OPTION_ACCEPT_TASK.equals(action.getTargetKeyword())) {
            log.warn("[xiuluo-v2] accept cloud prepared action discarded: source={} expected={} actual={} operation={}",
                    source, OPTION_ACCEPT_TASK, action.getTargetKeyword(), action.getOperation());
            clearAcceptDialogCloudFallback("cloud fallback wrong action");
            return Optional.empty();
        }

        Optional<XiuluoStepOutcome> gateBlocked = gateBlockAcceptOptionClickForTeamReturn(
                context, state, source + ":cloud-prepared");
        if (gateBlocked.isPresent()) {
            // Keep the prepared action for post-wake revalidation; its consume TTL owns staleness.
            return gateBlocked;
        }

        boolean clicked = inputSequences.moveAndClickLeft(
                "xiuluo:acceptOptionCloudPrepared:" + safeSnapshotName(source),
                action.getAbsoluteX(),
                action.getAbsoluteY(),
                150,
                650);
        log.info("[xiuluo-v2] accept option cloud prepared click: source={} actionKey={} matched={} click=({}, {}) clicked={}",
                source, action.getTargetKeyword(), action.getMatchedText(),
                action.getAbsoluteX(), action.getAbsoluteY(), clicked);
        if (!clicked) {
            clearAcceptDialogCloudFallback("cloud fallback input failed");
            return Optional.empty();
        }

        DialogResult result = DialogResult.statusBuilder(DialogResultStatus.GREEN_TEMPLATE_CLICKED, action.getDialogType())
                .actionKey(OPTION_ACCEPT_TASK)
                .matchedText(action.getMatchedText())
                .preparedAction(action)
                .relativeX(action.getRelativeX())
                .relativeY(action.getRelativeY())
                .absoluteX(action.getAbsoluteX())
                .absoluteY(action.getAbsoluteY())
                .build();
        recordAcceptTaskOptionSuccess(source + ":cloud-prepared", result);
        npcClickService.confirmPendingSmartClick(
                START_MAP_NAME,
                ACCEPT_NPC_NAME,
                ACCEPT_NPC_X,
                ACCEPT_NPC_Y,
                "CLOUD_PREPARED", source + ":accept cloud prepared consumed");
        clearAcceptDialogCloudFallback("cloud fallback consumed");
        TaskSleep.sleepOrStop(context, 250L, "Xiuluo V2 task interrupted");
        return Optional.of(continueAfterAcceptOptionClicked(state, "accept-task-cloud-prepared"));
    }

    /**
     * Wait for the task-owned accept fallback cloud request after local memory/template both miss.
     *
     * @param context current task context; checked between short waits so stop remains responsive.
     * @param state current 修罗 round state, used only for logs and retry routing.
     * @param source diagnostic source for logs.
     * @param future task-owned cloud fallback future; this is not a Runner-published prepared action.
     * @return prepared click action when cloud finished, empty when cloud returned no action, or null
     *         when the three-minute TTL expired and the caller should retry the NPC click phase.
     */
    private Optional<PreparedDialogAction> waitForAcceptCloudFallback(
            TaskExecutionContext context,
            XiuluoRoundContext state,
            String source,
            CompletableFuture<Optional<PreparedDialogAction>> future)
            throws InterruptedException, ExecutionException {
        long startedAt = System.currentTimeMillis();
        long deadlineAt = startedAt + ACCEPT_DIALOG_CLOUD_FALLBACK_TTL_MS;
        while (true) {
            TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            long remainingMs = deadlineAt - System.currentTimeMillis();
            if (remainingMs <= 0L) {
                log.warn("[xiuluo-v2] accept cloud fallback timed out: source={} round={} ttlMs={} futureDone={}",
                        source, state.round(), ACCEPT_DIALOG_CLOUD_FALLBACK_TTL_MS, future.isDone());
                clearAcceptDialogCloudFallback("cloud fallback ttl timeout");
                return null;
            }
            try {
                Optional<PreparedDialogAction> prepared = future.get(
                        Math.min(ACCEPT_DIALOG_CLOUD_FALLBACK_POLL_MS, remainingMs),
                        TimeUnit.MILLISECONDS);
                log.info("[xiuluo-v2] accept cloud fallback ready for consume: source={} round={} prepared={} elapsedMs={}",
                        source, state.round(), prepared.isPresent(),
                        Math.max(0L, System.currentTimeMillis() - startedAt));
                return prepared;
            } catch (TimeoutException ignored) {
                // Continue polling so task stop/interruption can be honored while the cloud fallback is pending.
            }
        }
    }

    private void clearAcceptDialogCloudFallback(String reason) {
        acceptDialogCloudFallbackFuture = null;
        acceptDialogCloudFallbackRound = 0;
        log.debug("[xiuluo-v2] accept cloud fallback cleared: reason={}", reason);
    }


    private Optional<XiuluoStepOutcome> tryLocalAcceptTaskTemplateOption(TaskExecutionContext context,
                                                                        XiuluoRoundContext state,
                                                                        String source) {
        long startedAt = System.currentTimeMillis();
        int[] rect = coordinateHelper.getScaledRect(
                ACCEPT_OPTION_ROI_X, ACCEPT_OPTION_ROI_Y, ACCEPT_OPTION_ROI_W, ACCEPT_OPTION_ROI_H);
        BufferedImage roi = tracker.captureToMemory(
                "xiuluo-accept-option-template:" + safeSnapshotName(source),
                rect[0], rect[1], rect[2], rect[3]);
        if (roi == null) {
            log.warn("[xiuluo-v2] accept option local template capture failed: source={} roi={} totalMs={}",
                    source, ImagePreprocessor.rectToString(rect),
                    Math.max(0L, System.currentTimeMillis() - startedAt));
            return Optional.empty();
        }

        BufferedImage template = ImagePreprocessor.pathToBufferedImage(ACCEPT_OPTION_TEMPLATE);
        if (template == null) {
            roi.flush();
            log.warn("[xiuluo-v2] accept option template image unavailable: source={} template={}",
                    source, ACCEPT_OPTION_TEMPLATE);
            return Optional.empty();
        }

        try {
            double[] match = ImageFinder.find(roi, template, ACCEPT_OPTION_TEMPLATE_MATCH_RATE);
            if (match == null || match.length < 3) {
                log.info("[xiuluo-v2] accept option local template not matched: source={} template={} roi={} threshold={} totalMs={}",
                        source, ACCEPT_OPTION_TEMPLATE, ImagePreprocessor.rectToString(rect),
                        ACCEPT_OPTION_TEMPLATE_MATCH_RATE, Math.max(0L, System.currentTimeMillis() - startedAt));
                return Optional.empty();
            }

            Point anchor = coordinateHelper.resolveMatchedPointInRect(rect, match);
            if (anchor == null) {
                log.warn("[xiuluo-v2] accept option local template matched without usable anchor: source={} template={} roi={}",
                        source, ACCEPT_OPTION_TEMPLATE, ImagePreprocessor.rectToString(rect));
                return Optional.empty();
            }

            int clickX = anchor.x + ACCEPT_OPTION_TEMPLATE_CLICK_OFFSET_X;
            int clickY = anchor.y;
            Optional<XiuluoStepOutcome> gateBlocked = gateBlockAcceptOptionClickForTeamReturn(
                    context, state, source + ":local-template");
            if (gateBlocked.isPresent()) {
                return gateBlocked;
            }
            boolean clicked = inputSequences.moveAndClickLeft(
                    "xiuluo:acceptOptionTemplate:" + safeSnapshotName(source),
                    clickX,
                    clickY,
                    150,
                    650);
            log.info("[xiuluo-v2] accept option local template click: source={} template={} score={} roi={} anchor=({}, {}) click=({}, {}) clicked={} totalMs={}",
                    source, ACCEPT_OPTION_TEMPLATE, String.format("%.4f", match[2]),
                    ImagePreprocessor.rectToString(rect), anchor.x, anchor.y, clickX, clickY, clicked,
                    Math.max(0L, System.currentTimeMillis() - startedAt));
            if (!clicked) {
                return Optional.empty();
            }

            DialogResult result = DialogResult.statusBuilder(DialogResultStatus.GREEN_TEMPLATE_CLICKED, DialogType.OPTION)
                    .actionKey(OPTION_ACCEPT_TASK)
                    .matchedText(ACCEPT_OPTION_TEMPLATE)
                    .relativeX(clickX - rect[0])
                    .relativeY(clickY - rect[1])
                    .absoluteX(clickX)
                    .absoluteY(clickY)
                    .build();
            recordAcceptTaskOptionSuccess(source + ":local-template", result);
            npcClickService.confirmPendingSmartClick(
                    START_MAP_NAME,
                    ACCEPT_NPC_NAME,
                    ACCEPT_NPC_X,
                    ACCEPT_NPC_Y,
                    "LOCAL_TEMPLATE", source + ":accept local template consumed");
            TaskSleep.sleepOrStop(context, 250L, "Xiuluo V2 task interrupted");
            return Optional.of(continueAfterAcceptOptionClicked(state, "accept-task-local-template"));
        } finally {
            roi.flush();
            template.flush();
        }
    }

    private void recordAcceptTaskOptionSuccess(String source, DialogResult result) {
        if (result.getStatus() != DialogResultStatus.GREEN_TEMPLATE_CLICKED
                || !OPTION_ACCEPT_TASK.equals(result.getActionKey())
                || result.getRelativeX() == null
                || result.getRelativeY() == null) {
            return;
        }
        memoryService.recordDialogChoiceSuccess(
                TASK_CODE,
                ACCEPT_TASK_ACTION,
                ACCEPT_NPC_NAME,
                START_MAP_NAME,
                ACCEPT_NPC_X,
                ACCEPT_NPC_Y,
                START_MAP_NAME,
                result.getRelativeX(),
                result.getRelativeY(),
                result.getMatchedText(),
                source + ":template");
    }

    private XiuluoStepOutcome continueAfterAcceptOptionClicked(XiuluoRoundContext state, String source) {
        clearAcceptDialogCloudFallback("accept option clicked: " + source);
        clearStartMapVerifiedLocation("accept option clicked: " + source);
        log.info("[xiuluo-v2] accept option clicked; start exit prepath before scheduling snapshot parse: source={} window={}",
                source, currentWindowLabel());
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK, source),
                "accept option clicked; maintenance then tracker shortcut");
    }

    private XiuluoRoundContext scheduleAcceptObjectiveBackgroundParse(XiuluoRoundContext state, String source) {
        long scheduledAt = System.currentTimeMillis();
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        String windowLabel = currentWindowLabel(runtime);
        String snapshotReason = "xiuluo-v2-accept-objective-" + state.round() + "-" + source.replace(':', '-');
        AcceptWindowSnapshot acceptSnapshot = captureAcceptWindowSnapshot(state.round(), snapshotReason);
        if (acceptSnapshot == null) {
            log.warn("[xiuluo-v2] objective snapshot failed; background result is empty: round={} source={} window={}",
                    state.round(), source, windowLabel);
            CompletableFuture<Optional<NpcTarget>> failedFuture = CompletableFuture.completedFuture(Optional.empty());
            CompletableFuture<TaskTrackerPanelReadResult> failedTrackerFuture =
                    CompletableFuture.completedFuture(TaskTrackerPanelReadResult.empty());
            return state.withAcceptParseFutures(XiuluoPhase.READ_OBJECTIVE, failedFuture, failedTrackerFuture,
                    source + ":accept-snapshot-missing");
        }

        BufferedImage objectiveSnapshot = dialogService.cropStoryObjectiveFromWindowSnapshotNoDetect(
                acceptSnapshot.image(), acceptSnapshot.baseX(), acceptSnapshot.baseY(), snapshotReason);
        if (objectiveSnapshot == null) {
            log.warn("[xiuluo-v2] objective snapshot crop failed; background result is empty: round={} source={} window={} snapshot={}",
                    state.round(), source, windowLabel, acceptSnapshot.path());
            CompletableFuture<Optional<NpcTarget>> failedFuture = CompletableFuture.completedFuture(Optional.empty());
            CompletableFuture<TaskTrackerPanelReadResult> trackerFuture = scheduleAcceptTrackerBackgroundParse(
                    state, source, scheduledAt, windowLabel, runtime, acceptSnapshot);
            acceptSnapshot.image().flush();
            return state.withAcceptParseFutures(XiuluoPhase.READ_OBJECTIVE, failedFuture, trackerFuture,
                    source + ":objective-snapshot-crop-missing");
        }

        log.info("[xiuluo-v2] accept window snapshot captured; background parses scheduled: round={} source={} window={} snapshot={} objectiveSize={}x{}",
                state.round(), source, windowLabel, acceptSnapshot.path(),
                objectiveSnapshot.getWidth(), objectiveSnapshot.getHeight());
        CompletableFuture<Optional<NpcTarget>> future = CompletableFuture.supplyAsync(() ->
                windowTaskContextHolder.callWith(runtime, () -> {
                    long startedAt = System.currentTimeMillis();
                    log.info("[xiuluo-v2] background objective parse started: round={} source={} window={} delayMs={}",
                            state.round(), source, windowLabel, startedAt - scheduledAt);
                    try {
                        Optional<NpcTarget> parsed = parseObjective(objectiveSnapshot, "xiuluo-v2:objective-bg:" + source);
                        log.info("[xiuluo-v2] background objective parse completed: round={} source={} window={} hit={} target={} elapsedMs={}",
                                state.round(), source, windowLabel, parsed.isPresent(), parsed.orElse(null),
                                System.currentTimeMillis() - startedAt);
                        return parsed;
                    } catch (RuntimeException e) {
                        log.warn("[xiuluo-v2] background objective parse failed with exception: round={} source={} window={} elapsedMs={}",
                                state.round(), source, windowLabel, System.currentTimeMillis() - startedAt, e);
                        return Optional.<NpcTarget>empty();
                    } finally {
                        objectiveSnapshot.flush();
                    }
                }));
        CompletableFuture<TaskTrackerPanelReadResult> trackerFuture = scheduleAcceptTrackerBackgroundParse(
                state, source, scheduledAt, windowLabel, runtime, acceptSnapshot);
        acceptSnapshot.image().flush();
        return state.withAcceptParseFutures(XiuluoPhase.READ_OBJECTIVE, future, trackerFuture,
                source + ":accept-snapshot-bg-started");
    }

    private CompletableFuture<TaskTrackerPanelReadResult> scheduleAcceptTrackerBackgroundParse(
            XiuluoRoundContext state,
            String source,
            long scheduledAt,
            String windowLabel,
            WindowRuntimeContext runtime,
            AcceptWindowSnapshot snapshot) {
        return CompletableFuture.supplyAsync(() ->
                windowTaskContextHolder.callWith(runtime, () -> {
                    long startedAt = System.currentTimeMillis();
                    log.info("[xiuluo-v2] background tracker parse started: round={} source={} window={} delayMs={} snapshot={}",
                            state.round(), source, windowLabel, startedAt - scheduledAt, snapshot.path());
                    try {
                        TaskTrackerPanelReadResult parsed = taskTrackerPanelService.readXiuluoTrackerPanelFromSnapshot(
                                snapshot.path(), snapshot.baseX(), snapshot.baseY(), "xiuluo-v2:tracker-bg:" + source);
                        log.info("[xiuluo-v2] background tracker parse completed: round={} source={} window={} found={} links={} detail={} elapsedMs={}",
                                state.round(), source, windowLabel, parsed.isFound(), parsed.getGreenLinks().size(),
                                parsed.getDetailRawPath(), System.currentTimeMillis() - startedAt);
                        return parsed;
                    } catch (RuntimeException e) {
                        log.warn("[xiuluo-v2] background tracker parse failed with exception: round={} source={} window={} elapsedMs={}",
                                state.round(), source, windowLabel, System.currentTimeMillis() - startedAt, e);
                        return TaskTrackerPanelReadResult.empty();
                    }
                }));
    }

    private AcceptWindowSnapshot captureAcceptWindowSnapshot(int round, String reason) {
        if (!tracker.refreshWindowState()) {
            log.warn("[xiuluo-v2] accept window snapshot skipped: reason={} round={} cause=refresh-window-failed",
                    reason, round);
            return null;
        }
        int baseX = tracker.getWindowBaseX();
        int baseY = tracker.getWindowBaseY();
        BufferedImage image = tracker.captureToMemory("xiuluo-v2-accept-window-snapshot:" + reason,
                baseX, baseY, baseX + GAME_CLIENT_WIDTH, baseY + GAME_CLIENT_HEIGHT);
        if (image == null) {
            log.warn("[xiuluo-v2] accept window snapshot capture failed: reason={} round={} base=({}, {})",
                    reason, round, baseX, baseY);
            return null;
        }
        Path path = Path.of(windowScopedTempPath.resolve("xiuluo_accept_snapshot_" + safeSnapshotName(reason) + ".png"));
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(image, "png", path.toFile());
            log.info("[xiuluo-v2] accept window snapshot saved: round={} reason={} base=({}, {}) path={} size={}x{}",
                    round, reason, baseX, baseY, path, image.getWidth(), image.getHeight());
            return new AcceptWindowSnapshot(image, path, baseX, baseY);
        } catch (IOException e) {
            image.flush();
            log.warn("[xiuluo-v2] accept window snapshot save failed: round={} reason={} path={}",
                    round, reason, path, e);
            return null;
        }
    }

    private String safeSnapshotName(String value) {
        String text = value == null || value.isBlank() ? "unknown" : value;
        return text.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Optional<NpcTarget> waitForBackgroundObjectiveResult(TaskExecutionContext context,
                                                                CompletableFuture<Optional<NpcTarget>> future,
                                                                XiuluoRoundContext state) {
        long startedAt = System.currentTimeMillis();
        XiuluoRoundContext waitState = state;
        while (true) {
            long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested(
                    context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            if (pauseBlockedMs > 0L) {
                waitState = compensatePreCombatTimerAfterMaintenance(
                        waitState, pauseBlockedMs, "xiuluo-v2:objective-parse:user-pause");
            }
            long remainingMs = remainingPreCombatWatchdogBudgetMs(waitState, System.currentTimeMillis());
            if (remainingMs <= 0L) {
                // A completed parse must never be discarded as a fake failure just because the
                // watchdog budget ran out while it was in flight.
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    Optional<NpcTarget> completed = future.getNow(Optional.empty());
                    log.info("[xiuluo-v2] READ_OBJECTIVE consumed completed background objective result despite exhausted watchdog budget: round={} source={} hit={} target={} elapsedMs={} window={}",
                            waitState.round(), waitState.source(), completed.isPresent(), completed.orElse(null),
                            System.currentTimeMillis() - startedAt, currentWindowLabel());
                    return completed;
                }
                log.warn("[xiuluo-v2] objective-parse watchdog budget timeout: round={} phase={} source={} elapsedMs={} remainingMs={} window={}",
                        waitState.round(), waitState.phase(), waitState.source(),
                        System.currentTimeMillis() - startedAt, remainingMs, currentWindowLabel());
                return Optional.empty();
            }
            long waitSliceMs = remainingMs == Long.MAX_VALUE ? 250L : Math.min(250L, remainingMs);
            try {
                Optional<NpcTarget> result = future.get(waitSliceMs, TimeUnit.MILLISECONDS);
                log.info("[xiuluo-v2] READ_OBJECTIVE consumed background objective result: round={} source={} hit={} target={} elapsedMs={} window={}",
                        waitState.round(), waitState.source(), result.isPresent(), result.orElse(null),
                        System.currentTimeMillis() - startedAt, currentWindowLabel());
                return result;
            } catch (TimeoutException e) {
                log.info("[xiuluo-v2] READ_OBJECTIVE still waiting for background objective result: round={} source={} elapsedMs={} remainingMs={} waitSliceMs={} window={}",
                        waitState.round(), waitState.source(), System.currentTimeMillis() - startedAt,
                        remainingMs, waitSliceMs, currentWindowLabel());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
                return Optional.empty();
            } catch (ExecutionException e) {
                log.warn("[xiuluo-v2] READ_OBJECTIVE background objective result failed: round={} source={} elapsedMs={} window={}",
                        waitState.round(), waitState.source(), System.currentTimeMillis() - startedAt, currentWindowLabel(),
                        e.getCause() == null ? e : e.getCause());
                return Optional.empty();
            }
        }
    }

    private String currentWindowLabel() {
        return currentWindowLabel(windowTaskContextHolder.rawCurrent().orElse(null));
    }

    private String currentWindowLabel(WindowRuntimeContext runtime) {
        if (runtime == null) {
            return "-";
        }
        WindowNativeBinding binding = runtime.getNativeBinding();
        String title = binding == null ? "" : binding.getTitle();
        return runtime.getWindowId() + "/" + (title == null || title.isBlank() ? "-" : title);
    }

    private void registerXiuluoDialogInterest(WindowRuntimeContext runtime,
                                              DialogOperation operation,
                                              String source) {
        if (runtime == null) {
            return;
        }
        runtime.updateDialogInterest(WindowDialogInterest.builder()
                .taskType(TaskType.XIULUO_V2)
                .operations(List.of(operation))
                .source(source)
                .build(), source);
    }

    /**
     * CR232: register the probe-only enter-battle interest for the tracker-shortcut route. The
     * watcher may only run the local kanda2 small-ROI probe for it (no generic dialog detection,
     * no cloud), and only from {@code anchor + ENTER_BATTLE_LOCAL_PROBE_DELAY_MS}. The anchor is
     * the first tracker green click (safe, slightly-later approximation of accept-success + 25s).
     */
    private void registerXiuluoEnterBattleProbeOnlyInterest(WindowRuntimeContext runtime,
                                                            XiuluoRoundContext state,
                                                            String source) {
        if (runtime == null) {
            return;
        }
        long anchorMs = state.firstTrackerGreenClickAtMs() > 0L
                ? state.firstTrackerGreenClickAtMs()
                : System.currentTimeMillis();
        long probeStartAtMs = anchorMs + ENTER_BATTLE_LOCAL_PROBE_DELAY_MS;
        runtime.updateDialogInterest(WindowDialogInterest.builder()
                .taskType(TaskType.XIULUO_V2)
                .operations(List.of(DialogOperation.XIULUO_ENTER_BATTLE))
                .source(source)
                .localTemplateProbeOnly(true)
                .probeStartAtMs(probeStartAtMs)
                .build(), source);
        log.info("[xiuluo-v2 shortcut] probe-only enter-battle interest registered: round={} anchorMs={} probeStartAtMs={} delayMs={}",
                state.round(), anchorMs, probeStartAtMs, ENTER_BATTLE_LOCAL_PROBE_DELAY_MS);
    }

    /**
     * The known Xiuluo option dialog specs for the accept/confirm chains.
     */
    private List<GreenTemplateClickSpec> xiuluoKnownOptionSpecs() {
        boolean allowUnderFive = botProperties.isXiuluoAllowUnderFiveMembers();
        GreenTemplateClickSpec underFiveSpec = allowUnderFive
                ? new GreenTemplateClickSpec(OPTION_UNDER_FIVE_CONFIRM, UNDER_FIVE_CONFIRM_TEMPLATE, -24, 24, 4)
                : new GreenTemplateClickSpec(OPTION_UNDER_FIVE_WAIT, UNDER_FIVE_WAIT_TEMPLATE, -24, 24, 4);
        return List.of(
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

    private Optional<XiuluoStepOutcome> handleUnderThreeBlockedDialog(TaskExecutionContext context,
                                                                      XiuluoRoundContext state,
                                                                      String source) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyWhiteTemplate(
                source,
                DIALOG_UNDER_THREE_BLOCKED,
                UNDER_THREE_BLOCKED_TEMPLATE));
        if (DIALOG_UNDER_THREE_BLOCKED.equals(result.getActionKey())
                && result.getStatus() == DialogResultStatus.WHITE_TEMPLATE_VISIBLE) {
            log.warn("[xiuluo-v2] under-three blocked dialog detected: source={} point=({}, {})",
                    source, result.getAbsoluteX(), result.getAbsoluteY());
            return Optional.of(XiuluoStepOutcome.sharedState(
                    state.next(XiuluoPhase.WAIT_TEAM_READY, "under-three-blocked"),
                    "xiuluo pending: team has fewer than three members"));
        }
        log.info("[xiuluo-v2] under-three blocked dialog not matched: source={} status={}",
                source, result.getStatus());
        return Optional.empty();
    }

    private XiuluoStepOutcome retryCurrentOrRecover(XiuluoRoundContext state,
                                                    XiuluoPhase recoveryPhase,
                                                    String reason) {
        if (state.phaseRetryCount() < MAX_PHASE_RETRY) {
            XiuluoStepOutcome retryOutcome = XiuluoStepOutcome.continueTo(
                    state.retrySamePhase("retry:" + state.phase()),
                    reason + "; retry current phase");
            TaskRecoveryCloudDecision<XiuluoPhase> recoveryDecision = decideTaskRecovery(
                    state,
                    retryOutcome,
                    "retry-current-phase",
                    reason,
                    Map.of("recoveryPhase", recoveryPhase.name()));
            if (!recoveryDecision.isRecoveryAllowed()) {
                return cloudRequiredRecoveryFailureOutcome(state, recoveryDecision, "retry-current-phase");
            }
            return retryOutcome;
        }
        return recoverOrFail(state.recoverTo(recoveryPhase, "recover:" + state.phase()),
                reason + "; recover to " + recoveryPhase);
    }

    private void toggleMountBeforeClickRetry(XiuluoRoundContext state, String reason) {
        /*
         * The second click attempt should see a different scene if a mount is covering purple
         * player text or yellow NPC labels. Alt+C toggles mount state; we intentionally do not
         * restore it because either mounted or unmounted is acceptable after the retry.
         */
        boolean submitted = inputSequences.pressAltC("xiuluo-v2:retry-toggle-mount:" + state.phase());
        log.info("[xiuluo-v2] retry click toggled mount with Alt+C: phase={} reason={} submitted={}",
                state.phase(), reason, submitted);
    }

    private XiuluoStepOutcome recoverOrFail(XiuluoRoundContext recoveredState, String message) {
        if (recoveredState.recoveryCount() > MAX_RECOVERY_COUNT) {
            XiuluoStepOutcome failedOutcome =
                    XiuluoStepOutcome.failed(recoveredState, message + "; recovery limit exceeded");
            TaskRecoveryCloudDecision<XiuluoPhase> recoveryDecision = decideTaskRecovery(
                    recoveredState,
                    failedOutcome,
                    "recovery-limit",
                    message,
                    Map.of("recoveryCount", Integer.toString(recoveredState.recoveryCount())));
            if (!recoveryDecision.isRecoveryAllowed() && recoveryDecision.isCloudRequiredFailure()) {
                return cloudRequiredRecoveryFailureOutcome(recoveredState, recoveryDecision, "recovery-limit");
            }
            return failedOutcome;
        }
        XiuluoStepOutcome recoveryOutcome = XiuluoStepOutcome.continueTo(recoveredState, message);
        TaskRecoveryCloudDecision<XiuluoPhase> recoveryDecision = decideTaskRecovery(
                recoveredState,
                recoveryOutcome,
                "recover",
                message,
                Map.of("recoveryCount", Integer.toString(recoveredState.recoveryCount())));
        if (!recoveryDecision.isRecoveryAllowed()) {
            return cloudRequiredRecoveryFailureOutcome(recoveredState, recoveryDecision, "recover");
        }
        return recoveryOutcome;
    }

    private XiuluoStepOutcome cloudRequiredRecoveryFailureOutcome(XiuluoRoundContext state,
                                                                  TaskRecoveryCloudDecision<XiuluoPhase> recoveryDecision,
                                                                  String recoveryAction) {
        if (recoveryDecision.isCloudRequiredFailure()) {
            log.error("[xiuluo-v2 cloud-execute] {} cloud.required failure; no local recovery: phase={} action={} reason={}",
                    CloudDecisionServiceId.TASK_RECOVERY,
                    state.phase(),
                    recoveryAction,
                    recoveryDecision.getRejectReason());
        } else {
            log.warn("[xiuluo-v2 cloud-execute] {} did not authorize recovery; no local recovery: phase={} action={} status={} reason={}",
                    CloudDecisionServiceId.TASK_RECOVERY,
                    state.phase(),
                    recoveryAction,
                    recoveryDecision.getStatus(),
                    recoveryDecision.getRejectReason());
        }
        return new XiuluoStepOutcome(
                state.next(XiuluoPhase.FAILED, "cloud-required-task-recovery:" + recoveryAction),
                TaskTransactionResult.RETRYABLE_ERROR,
                TaskYieldPolicy.MUST_YIELD,
                "cloud.required TASK_RECOVERY failure: " + recoveryDecision.getRejectReason(),
                null);
    }

    private NpcTarget toXiuluoObjective(ObjectiveTextResult value) {
        return NpcTarget.builder()
                .key("xiuluo.combatTarget")
                .mapName(value.mapName())
                .name(XIULUO_TARGET_KEYWORD)
                .x(value.x())
                .y(value.y())
                .role(NpcRole.COMBAT_TARGET)
                .movementType(NpcMovementType.FIXED)
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
        BufferedImage image = capture.image();
        log.info("[xiuluo-v2] task-panel objective capture: source={} task={} hasImage={} path={} size={}x{}",
                source, QUEST_PANEL_TASK_CODE, image != null, capture.imagePath(),
                image == null ? 0 : image.getWidth(), image == null ? 0 : image.getHeight());
        Optional<NpcTarget> target = parseTaskPanelObjective(capture, "xiuluo-v2:task-panel:" + source);
        log.info("[xiuluo-v2] task-panel objective parse result: source={} hit={} target={}",
                source, target.isPresent(), target.orElse(null));
        return target;
    }

    /**
     * Parse Xiuluo objective text from the quest-detail panel through OCR.
     *
     * <p>The story dialog keeps using the green-template objective recognizer. The task panel uses
     * OCR intentionally so it is an independent fallback; otherwise both paths can repeat the same
     * template digit bug, such as reading {@code 瑶池(87,36)} as {@code 瑶池(787,36)}.</p>
     *
     * @param capture quest-detail panel capture. The image is only kept for cleanup/template fallback
     *                and the OCR path reads the saved debug path.
     * @param source diagnostic source label for logs.
     * @return parsed combat target, or empty when OCR text is missing or implausible.
     */
    private Optional<NpcTarget> parseTaskPanelObjective(QuestDetailCapture capture, String source) {
        BufferedImage image = capture.image();
        try {
            /*
             * Xiuluo task-panel hot-start only trusts OCR text that explicitly contains
             * "前往 地图(x,y)". If the right-side task detail is a normal description, treating it as
             * a template fallback costs tens of seconds and can invent stale objectives.
             *
             * CR247 (CR208 items 5/6): the OCR + parse now run in the cloud QUEST_DETAIL_READER —
             * the panel image is the only local product. The reader stays OCR-based on purpose so
             * this path remains mechanically independent from the template-based story reader.
             */
            if (image != null && objectiveTextReaderCloudDecisionService.isQuestDetailActive()) {
                Optional<ObjectiveTextResult> cloud = objectiveTextReaderCloudDecisionService.readQuestDetail(
                        image, TASK_CODE, source);
                log.info("[xiuluo-v2] task-panel cloud reader result: source={} hit={} value={}",
                        source, cloud.isPresent(), cloud.orElse(null));
                return cloud.map(this::toXiuluoObjective)
                        .filter(target -> isObjectivePlausible(target, source + ":cloud"));
            }
            return parseTaskPanelObjectiveByOcr(capture.imagePath(), source);
        } finally {
            if (image != null) {
                image.flush();
            }
        }
    }

    /**
     * Legacy local OCR parse of the quest-detail panel. CR247 moved production recognition to the
     * cloud {@code QUEST_DETAIL_READER}; this remains only for disabled/offline dev mode and rollback.
     */
    @Deprecated(since = "CR247", forRemoval = false)
    private Optional<NpcTarget> parseTaskPanelObjectiveByOcr(String imagePath, String source) {
        if (imagePath == null || imagePath.isBlank()) {
            log.info("[xiuluo-v2] task-panel OCR skipped: source={} reason=image-path-blank", source);
            return Optional.empty();
        }
        List<OcrWordResult> words = textRecognizer.getAllTextResultsForMatch(
                imagePath,
                source + ":quest-detail-ocr",
                this::matchesTaskPanelObjectiveText);
        String text = joinOcrText(words);
        log.info("[xiuluo-v2] task-panel OCR text: source={} path={} text='{}'",
                source, imagePath, text);
        Optional<ObjectiveTextResult> parsed = parseTaskPanelObjectiveText(text, source + ":ocr");
        return parsed.map(this::toXiuluoObjective)
                .filter(target -> isObjectivePlausible(target, source + ":ocr"));
    }

    private boolean matchesTaskPanelObjectiveText(List<OcrWordResult> words) {
        return parseTaskPanelObjectiveText(joinOcrText(words), "task-panel-ocr-match").isPresent();
    }

    private Optional<ObjectiveTextResult> parseTaskPanelObjectiveText(String text, String source) {
        String normalized = normalizeTaskPanelObjectiveText(text);
        Matcher matcher = TASK_PANEL_OBJECTIVE_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            log.info("[xiuluo-v2] task-panel OCR objective miss: source={} text='{}'",
                    source, normalized);
            return Optional.empty();
        }
        String mapName = cleanupTaskPanelMapName(matcher.group(1));
        int x = Integer.parseInt(matcher.group(2));
        int y = Integer.parseInt(matcher.group(3));
        ObjectiveTextResult result = ObjectiveTextResult.builder()
                .mapSlug(mapName)
                .mapName(mapName)
                .x(x)
                .y(y)
                .mapScore(1.0)
                .source(source)
                .build();
        log.info("[xiuluo-v2] task-panel OCR objective parsed: source={} value={}",
                source, result);
        return Optional.of(result);
    }

    private String normalizeTaskPanelObjectiveText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(" ", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace("[", "(")
                .replace("]", ")")
                .replace("，", ",");
    }

    private String cleanupTaskPanelMapName(String mapName) {
        if (mapName == null) {
            return "";
        }
        return mapName.replace("任务目的", "")
                .replace("目的", "")
                .replace("前往", "")
                .replace("：", "")
                .replace(":", "")
                .trim();
    }

    private String joinOcrText(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "";
        }
        StringBuilder fullText = new StringBuilder();
        for (OcrWordResult word : words) {
            if (word != null && word.getText() != null) {
                fullText.append(word.getText());
            }
        }
        return fullText.toString();
    }

    private boolean isObjectivePlausible(NpcTarget target, String source) {
        boolean plausible = coordinateHelper.isLogicalCoordinatePlausible(
                target.getMapName(), target.getX(), target.getY(), 80);
        if (!plausible) {
            log.warn("[xiuluo-v2] objective rejected by coordinate plausibility: source={} target={}",
                    source, target);
        }
        return plausible;
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
            log.info("[xiuluo-v2] objective parse skipped: source={} reason=image-null", source);
            return Optional.empty();
        }
        log.info("[xiuluo-v2] objective parse start: source={} size={}x{}",
                source, image.getWidth(), image.getHeight());
        try {
            Optional<ObjectiveTextResult> result = objectiveTextRecognitionService.recognize(image, source);
            log.info("[xiuluo-v2] objective parse recognized: source={} hit={} value={}",
                    source, result.isPresent(), result.orElse(null));
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
        /*
         * Route-transfer dialogs may be discovered by the background watcher before the foreground
         * task can click them. Treat that as a normal yield state: the next turn should retry this
         * phase and consume the prepared dialog action instead of entering navigation recovery.
         */
        if (status == NavigationResultStatus.DIALOG_PREPARING) {
            return XiuluoStepOutcome.sharedState(
                    state.retrySamePhase("dialog-preparing:" + state.phase()),
                    actionName + " dialog preparing");
        }
        if (status == NavigationResultStatus.POINT_NOT_REACHED) {
            return XiuluoStepOutcome.sharedState(
                    state.retrySamePhase("point-not-reached:" + state.phase()),
                    actionName + " point not reached; retry next turn");
        }
        if (status == NavigationResultStatus.DIALOG_OPENED) {
            return XiuluoStepOutcome.continueTo(
                    state.next(arrivedPhase, "navigation-dialog-opened:" + state.phase()),
                    actionName + " dialog opened");
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
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot snapshot = runtime == null ? null : runtime.getPathingSnapshot();
        PreparedDialogAction preparedAction = runtime == null ? null : runtime.getPreparedDialogAction();
        WindowPathingIntent snapshotIntent = snapshot == null ? null : snapshot.getIntent();
        WindowPathingIntent activePathingIntent = runtime == null ? null : runtime.getActivePathingIntent().orElse(null);
        String expectedSourcePrefix = null;
        String expectedMapName = null;
        if (state.phase() == XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC) {
            expectedSourcePrefix = "xiuluo-v2:acceptNpc";
            expectedMapName = ACCEPT_NPC.getMapName();
        } else if (state.phase() == XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK) {
            expectedSourcePrefix = "xiuluo-v2:healPetNpc";
            expectedMapName = HEAL_PET_NPC.getMapName();
        } else if (state.phase() == XiuluoPhase.BEFORE_ROUTE_MAINTENANCE_CHECK) {
            expectedSourcePrefix = "xiuluo-v2:repairEquipmentNpc";
            expectedMapName = REPAIR_EQUIPMENT_NPC.getMapName();
        } else if (state.phase() == XiuluoPhase.NAVIGATE_TO_TARGET && state.objective() != null) {
            expectedSourcePrefix = "xiuluo-v2:target";
            expectedMapName = state.objective().getMapName();
        } else if (state.phase() == XiuluoPhase.NAVIGATE_BACK_TO_START) {
            expectedSourcePrefix = "xiuluo-v2:returnFallback";
            expectedMapName = ACCEPT_NPC.getMapName();
        }
        boolean snapshotBelongsToPhase = false;
        if (snapshotIntent != null && expectedSourcePrefix != null && expectedMapName != null) {
            String actualSource = snapshotIntent.getSource();
            snapshotBelongsToPhase = actualSource != null
                    && (actualSource.equals(expectedSourcePrefix) || actualSource.startsWith(expectedSourcePrefix + ":"))
                    && gameStateUtil.isSameMapName(snapshotIntent.getTargetMapName(), expectedMapName);
            if (!snapshotBelongsToPhase) {
                /*
                 * Window pathing state is shared inside the window runtime. 修罗 must only consume
                 * the intent submitted by the current phase, otherwise an older route can clear the
                 * current route-dialog memory or prematurely end this phase's wait.
                 */
                log.info("[xiuluo-v2] skip unrelated navigation watcher snapshot: phase={} action={} expectedSource={} expectedMap={} actualSource={} actualTarget={} state={}",
                        state.phase(), actionName, expectedSourcePrefix, expectedMapName,
                        actualSource, snapshotIntent.getTargetMapName(), snapshot.getState());
            }
        }
        boolean activeIntentBelongsToPhase = false;
        if (activePathingIntent != null && expectedSourcePrefix != null && expectedMapName != null) {
            String activeSource = activePathingIntent.getSource();
            activeIntentBelongsToPhase = activeSource != null
                    && (activeSource.equals(expectedSourcePrefix) || activeSource.startsWith(expectedSourcePrefix + ":"))
                    && gameStateUtil.isSameMapName(activePathingIntent.getTargetMapName(), expectedMapName);
        }
        WindowPathingIntent currentExpectedIntent = activeIntentBelongsToPhase
                ? activePathingIntent
                : snapshotBelongsToPhase ? snapshotIntent : null;
        String routeTarget = expectedMapName;
        long nowMs = System.currentTimeMillis();
        boolean preparedSameBinding = runtime != null && preparedAction != null;
        if (preparedSameBinding) {
            String actionWindowId = preparedAction.getWindowId();
            WindowNativeBinding binding = runtime.getNativeBinding();
            String actionHwnd = preparedAction.getHwnd();
            String currentHwnd = binding == null ? null : binding.getNativeHandle();
            boolean windowIdMatches = actionWindowId == null || actionWindowId.isBlank()
                    || actionWindowId.equals(runtime.getWindowId());
            boolean hwndMatches = actionHwnd == null || actionHwnd.isBlank()
                    || currentHwnd == null || currentHwnd.isBlank()
                    || actionHwnd.equals(currentHwnd);
            preparedSameBinding = windowIdMatches && hwndMatches;
        }
        if (routeTarget != null && !routeTarget.isBlank()
                && preparedAction != null
                && preparedAction.matches(DialogOperation.ROUTE_TRANSFER, routeTarget)
                && preparedSameBinding
                && preparedAction.verifiedWithin(nowMs, PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS)) {
            /*
             * The watcher has already prepared the route-dialog click. Do not keep waiting on
             * foreground movement probes; re-enter NavigationService so it can consume the cached
             * action immediately.
             */
            log.info("[xiuluo-v2] navigation pathing wait ended by prepared route action: phase={} action={} target={} matched={} point=({}, {})",
                    state.phase(), actionName, routeTarget, preparedAction.getMatchedText(),
                    preparedAction.getAbsoluteX(), preparedAction.getAbsoluteY());
            return null;
        }
        if (snapshot != null && snapshotBelongsToPhase) {
            WindowPathingState observed = snapshot.getState();
            long snapshotAgeMs = Math.max(0L, nowMs - snapshot.getUpdatedAtMs());
            log.info("[xiuluo-v2] navigation watcher snapshot: phase={} action={} source={} state={} current={}({}, {}) target={}({}, {}) ageMs={} probeInProgress={} message={}",
                    state.phase(), actionName,
                    snapshotIntent.getSource(), observed,
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    snapshotIntent.getTargetMapName(), snapshotIntent.getTargetX(),
                    snapshotIntent.getTargetY(), snapshotAgeMs, snapshot.isProbeInProgress(),
                    snapshot.getMessage());
            if (observed == WindowPathingState.ARRIVED) {
                runtime.clearPathingSignal("xiuluo consumed watcher arrival");
                log.info("[xiuluo-v2] navigation pathing wait ended by watcher arrival: phase={} action={}",
                        state.phase(), actionName);
                if (state.phase() == XiuluoPhase.NAVIGATE_TO_TARGET) {
                    closeTeamPathingMaintenanceWindow(context, state, "target-watcher-arrived");
                }
                return null;
            }
            if (observed == WindowPathingState.STOPPED_AWAY) {
                PreparedDialogAction preparedRoute = runtime.freshPreparedRouteActionForPathingTerminal(
                        snapshot, PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS);
                if (preparedRoute != null) {
                    WindowPathingIntent activeIntent = runtime.getActivePathingIntent().orElse(null);
                    long verifiedAgeMs = Math.max(0L, nowMs - preparedRoute.getLastVerifiedAtMs());
                    log.info("[xiuluo-v2] pathing terminal clear delayed because prepared route dialog is ready: state={} target={} actionIntentId={} activeIntentId={} verifiedAgeMs={}",
                            observed, preparedRoute.getTargetKeyword(), preparedRoute.getIntentId(),
                            activeIntent == null ? null : activeIntent.getIntentId(), verifiedAgeMs);
                } else {
                    runtime.clearPathingSignal("xiuluo consumed watcher stopped-away");
                }
                log.info("[xiuluo-v2] navigation pathing wait ended by watcher stopped-away; retry navigation: phase={} action={}",
                        state.phase(), actionName);
                if (state.phase() == XiuluoPhase.NAVIGATE_TO_TARGET) {
                    closeTeamPathingMaintenanceWindow(context, state, "target-watcher-stopped-away");
                }
                return null;
            }
            long probeAgeMs = snapshot.getProbeStartedAtMs() <= 0L
                    ? 0L
                    : Math.max(0L, nowMs - snapshot.getProbeStartedAtMs());
            long intentAgeMs = currentExpectedIntent == null
                    ? -1L
                    : Math.max(0L, nowMs - currentExpectedIntent.getCreatedAtMs());
            if (intentAgeMs >= RUNNER_PATHING_HARD_TIMEOUT_MS) {
                runtime.clearPathingSignal("xiuluo runner-only pathing hard timeout");
                log.warn("[xiuluo-v2] runner-only pathing hard timeout before watcher keep-wait: phase={} action={} source={} target={} intentId={} intentAgeMs={} timeoutMs={} observed={} snapshotAgeMs={} probeInProgress={}",
                        state.phase(), actionName, currentExpectedIntent.getSource(),
                        currentExpectedIntent.getTargetMapName(), currentExpectedIntent.getIntentId(),
                        intentAgeMs, RUNNER_PATHING_HARD_TIMEOUT_MS,
                        observed, snapshotAgeMs, snapshot.isProbeInProgress());
                if (state.phase() == XiuluoPhase.NAVIGATE_TO_TARGET) {
                    closeTeamPathingMaintenanceWindow(context, state, "target-runner-pathing-hard-timeout");
                }
                return null;
            }
            if ((snapshot.isProbeInProgress() && probeAgeMs <= OBSERVER_PROBE_MAX_AGE_MS)
                    || (snapshotAgeMs <= OBSERVER_SNAPSHOT_MAX_AGE_MS
                    && (observed == WindowPathingState.ACTIVE || observed == WindowPathingState.UNKNOWN))) {
                XiuluoStepOutcome maintenanceOutcome = runLeaderPathingSummonSkillMaintenance(
                        context, state, "watcher-" + observed);
                if (maintenanceOutcome != null) {
                    return maintenanceOutcome;
                }
                if (state.phase() == XiuluoPhase.NAVIGATE_TO_TARGET) {
                    return waitForNavigationPathingWake(XiuluoStepOutcome.pathingStarted(
                            state, actionName + " watcher still pathing: " + observed),
                            expectedSourcePrefix, expectedMapName);
                }
                return waitForNavigationPathingWake(XiuluoStepOutcome.pathingStarted(
                        state, actionName + " watcher still pathing: " + observed),
                        expectedSourcePrefix, expectedMapName);
            }
            if (snapshot.isProbeInProgress()) {
                log.warn("[xiuluo-v2] navigation watcher probe ignored because it is stale: phase={} action={} source={} probeAgeMs={} maxAgeMs={}",
                        state.phase(), actionName, snapshotIntent.getSource(), probeAgeMs, OBSERVER_PROBE_MAX_AGE_MS);
            }
        }
        if (expectedSourcePrefix != null && expectedMapName != null) {
            long intentAgeMs = currentExpectedIntent == null
                    ? -1L
                    : Math.max(0L, nowMs - currentExpectedIntent.getCreatedAtMs());
            if (intentAgeMs >= RUNNER_PATHING_HARD_TIMEOUT_MS) {
                if (runtime != null) {
                    runtime.clearPathingSignal("xiuluo runner-only pathing hard timeout without usable snapshot");
                }
                log.warn("[xiuluo-v2] runner-only pathing hard timeout before generic keep-wait: phase={} action={} source={} target={} intentId={} intentAgeMs={} timeoutMs={} hasSnapshot={} snapshotBelongs={}",
                        state.phase(), actionName,
                        currentExpectedIntent == null ? null : currentExpectedIntent.getSource(),
                        currentExpectedIntent == null ? null : currentExpectedIntent.getTargetMapName(),
                        currentExpectedIntent == null ? null : currentExpectedIntent.getIntentId(),
                        intentAgeMs, RUNNER_PATHING_HARD_TIMEOUT_MS,
                        snapshot != null, snapshotBelongsToPhase);
                if (state.phase() == XiuluoPhase.NAVIGATE_TO_TARGET) {
                    closeTeamPathingMaintenanceWindow(context, state, "target-runner-pathing-hard-timeout");
                }
                return null;
            }
            /*
             * CR111: once NavigationService registered a window pathing intent for this phase, the
             * task thread is not allowed to end the wait with foreground pixel/coordinate probes.
             * Runner/window watcher owns the terminal verdict; the task can only consume ARRIVED,
             * STOPPED_AWAY, or a fresh prepared route action above.
             */
            log.info("[xiuluo-v2] runner-only pathing wait continues: phase={} action={} expectedSource={} expectedMap={} hasSnapshot={} snapshotBelongs={} snapshotState={} probeInProgress={}",
                    state.phase(), actionName, expectedSourcePrefix, expectedMapName,
                    snapshot != null, snapshotBelongsToPhase,
                    snapshot == null ? null : snapshot.getState(),
                    snapshot != null && snapshot.isProbeInProgress());
            XiuluoStepOutcome maintenanceOutcome = runLeaderPathingSummonSkillMaintenance(
                    context, state, "runner-only-wait");
            if (maintenanceOutcome != null) {
                return maintenanceOutcome;
            }
            return waitForNavigationPathingWake(XiuluoStepOutcome.pathingStarted(
                    state, actionName + " runner-only pathing wait"),
                    expectedSourcePrefix, expectedMapName);
        }

        log.info("[xiuluo-v2] navigation pathing wait ended: phase={} action={}",
                state.phase(), actionName);
        if (state.phase() == XiuluoPhase.NAVIGATE_TO_TARGET) {
            closeTeamPathingMaintenanceWindow(context, state, "target-pathing-wait-ended");
        }
        return null;
    }

    private XiuluoStepOutcome runLeaderPathingSummonSkillMaintenance(TaskExecutionContext context,
                                                                     XiuluoRoundContext state,
                                                                     String source) {
        boolean routeOwnedMovement = state.phase() == XiuluoPhase.NAVIGATE_TO_TARGET
                || (state.phase() == XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING
                && state.routeMode() == XiuluoRouteMode.TRACKER_SHORTCUT
                && state.firstTrackerGreenClickAtMs() > 0L);
        if (!routeOwnedMovement || isMemberWindow(context)) {
            return null;
        }
        long maintenanceStartedAt = System.currentTimeMillis();
        TaskMaintenanceResult maintenanceResult = taskMaintenanceService.runOpportunisticMaintenance(context,
                TaskMaintenanceRequest.builder()
                        .sourceTask("xiuluo-v2:leader-pathing:" + source)
                        .handleMaintenanceBroadcast(false)
                        .cleanSummonSkill(true)
                        .oneSummonSkillPerTeamRound(true)
                        .maxSummonSkillCleanersPerTeamRound(2)
                        .teamMaintenanceKey(TASK_CODE)
                        .teamRound(state.round())
                        .requireOpenTeamMaintenanceWindow(true)
                        .build());
        XiuluoRoundContext adjustedState = compensatePreCombatTimerAfterMaintenance(
                state, System.currentTimeMillis() - maintenanceStartedAt, "xiuluo-v2:leader-pathing:" + source);
        if (maintenanceResult.getStatus() == TaskMaintenanceStatus.INTERRUPTED) {
            return XiuluoStepOutcome.stopped(adjustedState, "leader pathing summon-skill maintenance interrupted");
        }
        if (maintenanceResult.isHandled()) {
            log.info("[xiuluo-v2] leader pathing summon-skill maintenance handled: status={} message={} source={}",
                    maintenanceResult.getStatus(), maintenanceResult.getMessage(), source);
            return XiuluoStepOutcome.sharedState(adjustedState,
                    "leader pathing summon-skill maintenance handled; timer paused");
        }
        if (adjustedState != state) {
            return XiuluoStepOutcome.sharedState(adjustedState,
                    "leader pathing summon-skill maintenance checked; timer paused");
        }
        return null;
    }

    private boolean isMemberWindow(TaskExecutionContext context) {
        return context != null && "MEMBER".equalsIgnoreCase(context.getWindowRole());
    }

    private void openTeamPathingMaintenanceWindow(TaskExecutionContext context,
                                                  XiuluoRoundContext state,
                                                  String source) {
        taskMaintenanceService.openTeamPathingMaintenanceWindow(context, TASK_CODE, state.round(),
                "xiuluo-v2:" + source);
    }

    static boolean shouldOpenTeamPathingMaintenanceWindowAfterTargetNavigation(NavigationResult result) {
        if (result == null || result.getStatus() != NavigationResultStatus.PATHING_STARTED) {
            return false;
        }
        String message = result.getMessage();
        if (message == null) {
            return false;
        }
        return switch (message) {
            case NAV_MSG_WORLD_MAP_ROUTE_CLICKED,
                    NAV_MSG_ROUTE_DIALOG_CLICKED_BEFORE_PATHING_GUARD,
                    NAV_MSG_ROUTE_DIALOG_CLICKED_BEFORE_WORLD_MAP,
                    NAV_MSG_SAME_TARGET_ROUTE_PENDING,
                    NAV_MSG_SAME_TARGET_ROUTE_PENDING_BEFORE_WORLD_MAP -> true;
            default -> false;
        };
    }

    private void closeTeamPathingMaintenanceWindow(TaskExecutionContext context,
                                                   XiuluoRoundContext state,
                                                   String source) {
        taskMaintenanceService.closeTeamMaintenanceWindow(context, TASK_CODE, state.round(),
                "xiuluo-v2:" + source);
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

    private static String safeFailureFileName(String value, String fallback) {
        String text = value == null || value.isBlank() ? fallback : value.trim();
        String safe = text.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return safe.isBlank() ? fallback : safe;
    }

    private static String objectiveSummary(NpcTarget objective) {
        if (objective == null) {
            return null;
        }
        return objective.getMapName() + "(" + objective.getX() + "," + objective.getY() + ")"
                + "/" + objective.getName()
                + "/" + objective.getSource();
    }

    private static String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String safeCloudValue(String value) {
        return value == null ? "" : value;
    }

    private record XiuluoBrainShellResult(boolean accepted,
                                          boolean terminal,
                                          boolean reportOutcome,
                                          boolean realEventWaitCompleted,
                                          TaskRunResult taskRunResult,
                                          String reason,
                                          XiuluoStepOutcome outcome,
                                          Map<String, String> extraOutcomeFacts) {
        private static XiuluoBrainShellResult accepted(String reason, XiuluoStepOutcome outcome) {
            return accepted(reason, outcome, Map.of());
        }

        private static XiuluoBrainShellResult accepted(String reason,
                                                       XiuluoStepOutcome outcome,
                                                       Map<String, String> extraOutcomeFacts) {
            return new XiuluoBrainShellResult(
                    true, false, true, false, null, reason, outcome, safeExtraOutcomeFacts(extraOutcomeFacts));
        }

        private static XiuluoBrainShellResult acceptedRealEventWait(String reason, XiuluoStepOutcome outcome) {
            return new XiuluoBrainShellResult(true, false, true, true, null, reason, outcome, Map.of());
        }

        private static XiuluoBrainShellResult terminal(TaskRunResult taskRunResult, String reason) {
            return new XiuluoBrainShellResult(true, true, false, false, taskRunResult, reason, null, Map.of());
        }

        private static XiuluoBrainShellResult terminalAfterOutcome(
                TaskRunResult taskRunResult,
                String reason,
                XiuluoStepOutcome outcome) {
            return new XiuluoBrainShellResult(true, true, true, false, taskRunResult, reason, outcome, Map.of());
        }

        private static XiuluoBrainShellResult terminalStopped(String reason, XiuluoStepOutcome outcome) {
            return new XiuluoBrainShellResult(true, true, false, false, TaskRunResult.STOPPED, reason, outcome, Map.of());
        }

        private static XiuluoBrainShellResult rejected(String reason) {
            return new XiuluoBrainShellResult(false, false, false, false, null, reason, null, Map.of());
        }

        private static XiuluoBrainShellResult cleanupAccepted(String reason) {
            return new XiuluoBrainShellResult(true, false, false, false, null, reason, null, Map.of());
        }

        private static Map<String, String> safeExtraOutcomeFacts(Map<String, String> extraOutcomeFacts) {
            return extraOutcomeFacts == null || extraOutcomeFacts.isEmpty()
                    ? Map.of()
                    : Map.copyOf(extraOutcomeFacts);
        }

        private XiuluoBrainShellResult withExtraOutcomeFacts(Map<String, String> extraFacts) {
            if (extraFacts == null || extraFacts.isEmpty()) {
                return this;
            }
            Map<String, String> merged = new LinkedHashMap<>(extraOutcomeFacts);
            merged.putAll(extraFacts);
            return new XiuluoBrainShellResult(accepted, terminal, reportOutcome, realEventWaitCompleted,
                    taskRunResult, reason, outcome, Map.copyOf(merged));
        }

        private String outcomeStatus() {
            if (outcome == null || outcome.transactionResult() == null) {
                return "NOT_RUN";
            }
            return switch (outcome.transactionResult()) {
                case STOPPED -> "STOPPED";
                case FAILED -> "FAILED";
                case PATHING_STARTED, READY_TO_CONTINUE, SHARED_STATE_TRIGGERED, TASK_FINISHED -> "EXECUTED";
                case RETRYABLE_ERROR -> "RETRYABLE_ERROR";
            };
        }

        private String transactionResult() {
            return outcome == null || outcome.transactionResult() == null ? "" : outcome.transactionResult().name();
        }

        private String yieldPolicy() {
            return outcome == null || outcome.yieldPolicy() == null ? "" : outcome.yieldPolicy().name();
        }

        private XiuluoPhase localOutcomeNextPhase() {
            return outcome == null || outcome.nextState() == null ? null : outcome.nextState().phase();
        }
    }

    private record XiuluoEventParkResult(XiuluoStepOutcome outcome, boolean realParkCompleted) {
        private static XiuluoEventParkResult parked(XiuluoStepOutcome outcome) {
            return new XiuluoEventParkResult(outcome, true);
        }

        private static XiuluoEventParkResult notParked(XiuluoStepOutcome outcome) {
            return new XiuluoEventParkResult(outcome, false);
        }
    }

    private static class MaintenanceAttemptResult {
        private final XiuluoStepOutcome outcome;
        private final boolean handled;
        private final long blockedMs;

        private MaintenanceAttemptResult(XiuluoStepOutcome outcome, boolean handled, long blockedMs) {
            this.outcome = outcome;
            this.handled = handled;
            this.blockedMs = blockedMs;
        }

        private XiuluoStepOutcome outcome() {
            return outcome;
        }

        private boolean handled() {
            return handled;
        }

        private long blockedMs() {
            return blockedMs;
        }

        private static MaintenanceAttemptResult retry() {
            return new MaintenanceAttemptResult(null, false, 0L);
        }

        private static MaintenanceAttemptResult handledResult(long blockedMs) {
            return new MaintenanceAttemptResult(null, true, blockedMs);
        }

        private static MaintenanceAttemptResult withOutcome(XiuluoStepOutcome outcome) {
            return new MaintenanceAttemptResult(outcome, false, 0L);
        }
    }

    private static class XiuluoRoundTrace {
        private final LocalDateTime startedAt;
        private final int round;
        private final String windowId;
        private final String windowRole;
        private final String nativeWindowHandle;
        private final String nativeWindowTitle;
        private final XiuluoPhase initialPhase;
        private final String initialSource;
        private final String initialObjective;
        private final List<String> eventJsonLines = new ArrayList<>();
        private final List<String> summaryLines = new ArrayList<>();
        private int droppedEventCount;

        private XiuluoRoundTrace(TaskExecutionContext context, XiuluoRoundContext initialContext) {
            this.startedAt = LocalDateTime.now();
            this.round = initialContext.round();
            this.windowId = context.getWindowId();
            this.windowRole = context.getWindowRole();
            this.nativeWindowHandle = context.getNativeWindowHandle();
            this.nativeWindowTitle = context.getNativeWindowTitle();
            this.initialPhase = initialContext.phase();
            this.initialSource = initialContext.source();
            this.initialObjective = objectiveSummary(initialContext.objective());
        }

        static XiuluoRoundTrace start(TaskExecutionContext context, XiuluoRoundContext initialContext) {
            XiuluoRoundTrace trace = new XiuluoRoundTrace(context, initialContext);
            trace.addRoundStart(initialContext);
            return trace;
        }

        LocalDateTime startedAt() {
            return startedAt;
        }

        void addPhaseOutcome(XiuluoRoundContext currentContext,
                             XiuluoStepOutcome outcome,
                             TaskTransactionResult transactionResult) {
            XiuluoRoundContext nextState = outcome.nextState();
            addEvent(
                    jsonLine("phase-outcome",
                            currentContext.phase(),
                            currentContext.source(),
                            objectiveSummary(currentContext.objective()),
                            outcome.transactionResult(),
                            transactionResult,
                            outcome.yieldPolicy(),
                            nextState == null ? null : nextState.phase(),
                            nextState == null ? null : nextState.source(),
                            nextState == null ? null : objectiveSummary(nextState.objective()),
                            outcome.message(),
                            currentContext.waitingPathing(),
                            currentContext.enteredBattleByXiuluo(),
                            currentContext.phaseRetryCount(),
                            currentContext.recoveryCount()),
                    "- " + LocalDateTime.now() + " phase=" + currentContext.phase()
                            + " result=" + outcome.transactionResult()
                            + " transaction=" + transactionResult
                            + " next=" + (nextState == null ? "-" : nextState.phase())
                            + " message=" + safeValue(outcome.message())
                            + " objective=" + safeValue(objectiveSummary(currentContext.objective())));
        }

        void addLoopGuard(XiuluoRoundContext currentContext, int loopGuardCount) {
            addEvent(
                    jsonLine("loop-guard",
                            currentContext.phase(),
                            currentContext.source(),
                            objectiveSummary(currentContext.objective()),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "phase loop guard exceeded: count=" + loopGuardCount,
                            currentContext.waitingPathing(),
                            currentContext.enteredBattleByXiuluo(),
                            currentContext.phaseRetryCount(),
                            currentContext.recoveryCount()),
                    "- " + LocalDateTime.now() + " loopGuard phase=" + currentContext.phase()
                            + " count=" + loopGuardCount
                            + " source=" + safeValue(currentContext.source())
                            + " objective=" + safeValue(objectiveSummary(currentContext.objective())));
        }

        void addCloudCommand(XiuluoRoundContext currentContext, XiuluoBrainResponse command) {
            if (command == null) {
                return;
            }
            String message = "action=" + command.getActionType()
                    + " actionId=" + safeValue(command.getActionId())
                    + " sessionId=" + safeValue(command.getSessionId())
                    + " stateSeq=" + command.getStateSeq()
                    + " reason=" + safeValue(command.getReason());
            addEvent(
                    jsonLine("cloud-command",
                            currentContext.phase(),
                            currentContext.source(),
                            objectiveSummary(currentContext.objective()),
                            null,
                            null,
                            null,
                            command.getPhase(),
                            null,
                            null,
                            message,
                            currentContext.waitingPathing(),
                            currentContext.enteredBattleByXiuluo(),
                            currentContext.phaseRetryCount(),
                            currentContext.recoveryCount()),
                    "- " + LocalDateTime.now() + " cloudCommand phase=" + command.getPhase()
                            + " " + message);
        }

        void addCloudFailure(XiuluoRoundContext currentContext, String reason) {
            addEvent(
                    jsonLine("cloud-failure",
                            currentContext.phase(),
                            currentContext.source(),
                            objectiveSummary(currentContext.objective()),
                            TaskTransactionResult.FAILED,
                            null,
                            null,
                            XiuluoPhase.PREPARE_ROUND,
                            "failure-restart",
                            null,
                            reason,
                            currentContext.waitingPathing(),
                            currentContext.enteredBattleByXiuluo(),
                            currentContext.phaseRetryCount(),
                            currentContext.recoveryCount()),
                    "- " + LocalDateTime.now() + " cloudFailure phase=" + currentContext.phase()
                            + " reason=" + safeValue(reason));
        }

        String summaryMarkdown(String reason,
                               XiuluoRoundContext failedContext,
                               String message,
                               XiuluoStepOutcome outcome,
                               Path caseDir) {
            LocalDateTime endedAt = LocalDateTime.now();
            StringBuilder builder = new StringBuilder();
            builder.append("# Xiuluo Failure Case").append(System.lineSeparator()).append(System.lineSeparator());
            builder.append("- dir: ").append(caseDir).append(System.lineSeparator());
            builder.append("- reason: ").append(safeValue(reason)).append(System.lineSeparator());
            builder.append("- round: ").append(round).append(System.lineSeparator());
            builder.append("- windowId: ").append(safeValue(windowId)).append(System.lineSeparator());
            builder.append("- windowRole: ").append(safeValue(windowRole)).append(System.lineSeparator());
            builder.append("- hwnd: ").append(safeValue(nativeWindowHandle)).append(System.lineSeparator());
            builder.append("- title: ").append(safeValue(nativeWindowTitle)).append(System.lineSeparator());
            builder.append("- startedAt: ").append(startedAt).append(System.lineSeparator());
            builder.append("- failedAt: ").append(endedAt).append(System.lineSeparator());
            builder.append("- elapsedMs: ").append(Duration.between(startedAt, endedAt).toMillis()).append(System.lineSeparator());
            builder.append("- initialPhase: ").append(initialPhase).append(System.lineSeparator());
            builder.append("- initialSource: ").append(safeValue(initialSource)).append(System.lineSeparator());
            builder.append("- initialObjective: ").append(safeValue(initialObjective)).append(System.lineSeparator());
            builder.append("- failedPhase: ").append(failedContext.phase()).append(System.lineSeparator());
            builder.append("- failedSource: ").append(safeValue(failedContext.source())).append(System.lineSeparator());
            builder.append("- failedObjective: ").append(safeValue(objectiveSummary(failedContext.objective()))).append(System.lineSeparator());
            builder.append("- message: ").append(safeValue(message)).append(System.lineSeparator());
            if (outcome != null && outcome.nextState() != null) {
                builder.append("- outcomeResult: ").append(outcome.transactionResult()).append(System.lineSeparator());
                builder.append("- outcomeNextPhase: ").append(outcome.nextState().phase()).append(System.lineSeparator());
                builder.append("- outcomeNextSource: ").append(safeValue(outcome.nextState().source())).append(System.lineSeparator());
            }
            builder.append("- droppedEvents: ").append(droppedEventCount).append(System.lineSeparator());
            builder.append(System.lineSeparator()).append("## Events").append(System.lineSeparator()).append(System.lineSeparator());
            for (String line : summaryLines) {
                builder.append(line).append(System.lineSeparator());
            }
            return builder.toString();
        }

        String eventsJsonl() {
            return String.join(System.lineSeparator(), eventJsonLines) + System.lineSeparator();
        }

        int eventCount() {
            return eventJsonLines.size();
        }

        int droppedEventCount() {
            return droppedEventCount;
        }

        private void addRoundStart(XiuluoRoundContext initialContext) {
            addEvent(
                    jsonLine("round-start",
                            initialContext.phase(),
                            initialContext.source(),
                            objectiveSummary(initialContext.objective()),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "round trace started",
                            initialContext.waitingPathing(),
                            initialContext.enteredBattleByXiuluo(),
                            initialContext.phaseRetryCount(),
                            initialContext.recoveryCount()),
                    "- " + startedAt + " roundStart phase=" + initialContext.phase()
                            + " source=" + safeValue(initialContext.source())
                            + " objective=" + safeValue(objectiveSummary(initialContext.objective())));
        }

        private void addEvent(String jsonLine, String summaryLine) {
            if (eventJsonLines.size() >= MAX_ROUND_TRACE_EVENTS) {
                int indexToDrop = eventJsonLines.size() > 1 ? 1 : 0;
                eventJsonLines.remove(indexToDrop);
                summaryLines.remove(indexToDrop);
                droppedEventCount++;
            }
            eventJsonLines.add(jsonLine);
            summaryLines.add(summaryLine);
        }

        private String jsonLine(String kind,
                                XiuluoPhase phase,
                                String source,
                                String objective,
                                TaskTransactionResult outcomeResult,
                                TaskTransactionResult transactionResult,
                                TaskYieldPolicy yieldPolicy,
                                XiuluoPhase nextPhase,
                                String nextSource,
                                String nextObjective,
                                String message,
                                boolean waitingPathing,
                                boolean enteredBattleByXiuluo,
                                int phaseRetryCount,
                                int recoveryCount) {
            StringBuilder builder = new StringBuilder("{");
            appendJson(builder, "time", LocalDateTime.now().toString());
            appendJson(builder, "kind", kind);
            appendJson(builder, "round", round);
            appendJson(builder, "windowId", windowId);
            appendJson(builder, "windowRole", windowRole);
            appendJson(builder, "nativeWindowHandle", nativeWindowHandle);
            appendJson(builder, "phase", phase == null ? null : phase.name());
            appendJson(builder, "source", source);
            appendJson(builder, "objective", objective);
            appendJson(builder, "outcomeResult", outcomeResult == null ? null : outcomeResult.name());
            appendJson(builder, "transactionResult", transactionResult == null ? null : transactionResult.name());
            appendJson(builder, "yieldPolicy", yieldPolicy == null ? null : yieldPolicy.name());
            appendJson(builder, "nextPhase", nextPhase == null ? null : nextPhase.name());
            appendJson(builder, "nextSource", nextSource);
            appendJson(builder, "nextObjective", nextObjective);
            appendJson(builder, "message", message);
            appendJson(builder, "waitingPathing", waitingPathing);
            appendJson(builder, "enteredBattleByXiuluo", enteredBattleByXiuluo);
            appendJson(builder, "phaseRetryCount", phaseRetryCount);
            appendJson(builder, "recoveryCount", recoveryCount);
            return builder.append("}").toString();
        }

        private void appendJson(StringBuilder builder, String name, Object value) {
            if (builder.length() > 1) {
                builder.append(',');
            }
            builder.append('"').append(escapeJson(name)).append("\":");
            if (value == null) {
                builder.append("null");
                return;
            }
            if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
                return;
            }
            builder.append('"').append(escapeJson(String.valueOf(value))).append('"');
        }

        private String escapeJson(String value) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                switch (ch) {
                    case '"' -> builder.append("\\\"");
                    case '\\' -> builder.append("\\\\");
                    case '\b' -> builder.append("\\b");
                    case '\f' -> builder.append("\\f");
                    case '\n' -> builder.append("\\n");
                    case '\r' -> builder.append("\\r");
                    case '\t' -> builder.append("\\t");
                    default -> {
                        if (ch < 0x20) {
                            builder.append(String.format("\\u%04x", (int) ch));
                        } else {
                            builder.append(ch);
                        }
                    }
                }
            }
            return builder.toString();
        }
    }

    private record XiuluoFailureAssessment(String conclusion, String recommendation) {
    }

    private enum ReturnHomeResult {
        VERIFIED,
        STILL_IN_COMBAT,
        FAILED_AFTER_TRUSTED_NOT_IN_COMBAT,
        UNAVAILABLE,
        FAILED
    }

    private record ReturnItemUseResult(boolean verifiedStartMap,
                                       boolean usedStartMapUnverified,
                                       LocationInfo location) {

        private static ReturnItemUseResult verified(LocationInfo location) {
            return new ReturnItemUseResult(true, false, location);
        }

        private static ReturnItemUseResult usedStartMapUnverified(LocationInfo location) {
            return new ReturnItemUseResult(false, true, location);
        }

        private static ReturnItemUseResult notUsed() {
            return new ReturnItemUseResult(false, false, null);
        }
    }

    private record AcceptWindowSnapshot(BufferedImage image, Path path, int baseX, int baseY) {
    }

}
