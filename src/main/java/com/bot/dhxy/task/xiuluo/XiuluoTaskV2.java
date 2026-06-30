package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
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
import com.bot.dhxy.model.quest.QuestDetailCapture;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.exception.TaskFatalException;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.BagService;
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
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/xiuluo/xiuluo_accept_xianlaiwu.png";
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
    private static final long DEFAULT_TEAM_READY_WAIT_POLL_MS = 3_000L;
    private static final String UNDER_THREE_WAIT_SOURCE_PREFIX = "under-three-wait";
    private static final String TEAM_RETURN_WAIT_SOURCE_PREFIX = "team-return-wait";
    private static final String TEAM_RETURN_BEFORE_ACCEPT_SOURCE = TEAM_RETURN_WAIT_SOURCE_PREFIX + ":before-accept";
    private static final String TEAM_RETURN_ROUND_DONE_SOURCE = TEAM_RETURN_WAIT_SOURCE_PREFIX + ":round-done";
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
    private static final DateTimeFormatter FAILURE_CASE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
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
    private final TextRecognizer textRecognizer;
    private final AutoCombatService autoCombatService;
    private final BagService bagService;
    private final ReturnItemPrescanService returnItemPrescanService;
    private final PlayerStateService playerStateService;
    private final TaskMaintenanceService taskMaintenanceService;
    private final CommonBoxService commonBoxService;
    private final TaskTrackerPanelService taskTrackerPanelService;
    private final UICleanerService uiCleanerService;
    private final TeamReturnService teamReturnService;
    private final XiuluoHotStartResolver hotStartResolver;
    private final TaskTransactionRunner taskTransactionRunner;
    private final TaskTurnCoordinator taskTurnCoordinator;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final MultiWindowTaskManager multiWindowTaskManager;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowReadyEventBus windowReadyEventBus;
    private final InputSequences inputSequences;
    private final AutomationMetricsService automationMetricsService;

    /*
     * Startup incense is a task-run guard, not the incense cooldown itself. PlayerStateService still
     * owns the real time/status rules; Xiuluo only decides the first safe point to ask that service.
     */
    private boolean startupIncenseChecked;
    private boolean startupIncensePending;
    private TeamReturnService.LeaderSignalPrecheck pendingTeamReturnPrecheck;
    private long lastHealPetMaintenanceAt;
    private long lastRepairEquipmentMaintenanceAt;

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
                if (completedRuns == 0) {
                    playerStateService.performStartupFirstAidCheck(context);
                    ensureStartupIncenseBeforeHotStart(context);
                }
                XiuluoRoundContext roundContext;
                if (completedRuns == 0 && cleanQueueTransitionStartup) {
                    log.info("[xiuluo-v2] skip startup-screen resume because clean queued task transition; accept a fresh task");
                    roundContext = XiuluoRoundContext.start(round);
                } else if (completedRuns == 0) {
                    roundContext = resolveStartupTrackerOrReturnItem(context, XiuluoRoundContext.start(round),
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

    // 🟠 ROUND LOOP: one Xiuluo round advances through phase transactions here.
    private TaskRunResult runRoundPhases(TaskExecutionContext context,
                                         XiuluoRoundContext initialContext,
                                         XiuluoRoundTrace roundTrace) {
        XiuluoRoundContext roundContext = initialContext;
        int phaseLoopGuard = 0;
        int consecutivePathingYields = 0;
        int consecutiveRoundFailures = 0;
        while (!roundContext.phase().isTerminal()) {
            long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested(
                    context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
            if (pauseBlockedMs > 0L) {
                roundContext = compensatePreCombatTimerAfterMaintenance(
                        roundContext, pauseBlockedMs, roundContext.source() + ":user-pause");
            }

            XiuluoRoundContext currentContext = roundContext;
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
                transaction = taskTransactionRunner.run(
                        "xiuluo-v2:" + currentContext.phase(),
                        TaskTransactionResult.READY_TO_CONTINUE,
                        TaskYieldPolicy.CONTINUE_CHAIN,
                        () -> {
                            XiuluoStepOutcome outcome = runPhase(context, currentContext);
                            phaseOutcome.set(outcome);
                            return outcome.transactionResult();
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
            outcome = compensateMaintenanceHandoffDelay(outcome);
            log.info("[xiuluo-v2] phase outcome: phase={} result={} yield={} next={} message={}",
                    currentContext.phase(), outcome.transactionResult(), outcome.yieldPolicy(),
                    outcome.nextState().phase(), outcome.message());
            roundTrace.addPhaseOutcome(currentContext, outcome, transaction.result());

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
                outcome = yieldAfterMustYield(context, outcome);
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
                    outcome = yieldAfterMustYield(context, outcome);
                }
                roundContext = outcome.nextState();
                continue;
            }
            if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD) {
                outcome = yieldAfterMustYield(context, outcome);
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
                        + " limitMs=" + PRE_COMBAT_WATCHDOG_TIMEOUT_MS);
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
        throwIfConsecutiveRoundFailuresExceeded(failedContext, outcome.message(), consecutiveRoundFailures);
        uiCleanerService.cleanUpAll();
        yieldAfterMustYield(context, outcome);
        return XiuluoRoundContext.start(failedContext.round());
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
        throwIfConsecutiveRoundFailuresExceeded(currentContext, "phase loop guard exceeded", consecutiveRoundFailures);
        uiCleanerService.cleanUpAll();
        TaskSleep.sleepOrStop(context, TASK_TURN_HANDOFF_DELAY_MS, "Xiuluo V2 task interrupted");
        return XiuluoRoundContext.start(currentContext.round());
    }

    private void throwIfConsecutiveRoundFailuresExceeded(XiuluoRoundContext failedContext,
                                                         String message,
                                                         int consecutiveRoundFailures) {
        if (consecutiveRoundFailures < MAX_CONSECUTIVE_ROUND_FAILURES) {
            return;
        }
        throw new TaskFatalException("[xiuluo-v2] consecutive round failures exceeded: round="
                + failedContext.round()
                + " attempts=" + consecutiveRoundFailures
                + " phase=" + failedContext.phase()
                + " message=" + message);
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

    private XiuluoStepOutcome yieldAfterMustYield(TaskExecutionContext context, XiuluoStepOutcome outcome) {
        XiuluoWaitSpec waitSpec = outcome.waitSpec();
        if (waitSpec != null) {
            log.info("[xiuluo-v2] task turn event wait: result={} next={} reason={} wakeTypes={} timeoutMs={} afterSequence={}",
                    outcome.transactionResult(), outcome.nextState().phase(), waitSpec.getReason(),
                    waitSpec.getWakeTypes(), waitSpec.getTimeoutMs(), waitSpec.getAfterSequence());
            XiuluoStepOutcome maintenanceOutcome = maybeRunLeaderPathingSummonMaintenanceBeforePark(context, outcome);
            if (maintenanceOutcome != null) {
                return maintenanceOutcome;
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
        return outcome;
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
        if (reason != XiuluoWaitReason.WAIT_TRACKER_SHORTCUT_PATHING
                && reason != XiuluoWaitReason.WAIT_TARGET_PATHING_TERMINAL) {
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
    private XiuluoStepOutcome parkAfterYieldIfNeeded(TaskExecutionContext context, XiuluoStepOutcome outcome) {
        XiuluoWaitSpec waitSpec = outcome.waitSpec();
        if (waitSpec == null) {
            return outcome;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            log.warn("[xiuluo-v2 wait] skip park: no window runtime next={} reason={}",
                    outcome.nextState().phase(), waitSpec.getReason());
            return outcome;
        }
        EnumSet<WindowReadyEventType> wakeTypes = toWakeTypeEnumSet(waitSpec.getWakeTypes());
        if (wakeTypes.isEmpty()) {
            log.warn("[xiuluo-v2 wait] skip park: no wake types windowId={} next={} reason={}",
                    runtime.getWindowId(), outcome.nextState().phase(), waitSpec.getReason());
            return outcome;
        }
        long boundedTimeoutMs = boundedPreCombatWaitTimeoutMs(outcome.nextState(), waitSpec.getTimeoutMs(),
                waitSpec.getReason().name());
        if (boundedTimeoutMs == 0L) {
            return preCombatWatchdogTimeoutOutcome(outcome.nextState(), waitSpec.getReason().name(),
                    "before-event-wait");
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
        long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested(context, "Xiuluo V2 task interrupted");
        XiuluoStepOutcome adjustedOutcome = outcome;
        if (pauseBlockedMs > 0L) {
            XiuluoRoundContext adjustedState = compensatePreCombatTimerAfterMaintenance(
                    outcome.nextState(), pauseBlockedMs, "xiuluo-v2:event-wait:" + waitSpec.getReason());
            adjustedOutcome = outcome.withNextState(adjustedState);
        }
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - startedAt);
        log.info("[xiuluo-v2 wait] park finished: next={} windowId={} reason={} wakeTypes={} afterSequence={} timeoutMs={} boundedTimeoutMs={} elapsedMs={} pauseBlockedMs={} wakeResult={} wakeType={} wakeSeq={} source={}",
                adjustedOutcome.nextState().phase(), runtime.getWindowId(), waitSpec.getReason(), waitSpec.getWakeTypes(),
                waitSpec.getAfterSequence(), waitSpec.getTimeoutMs(), boundedTimeoutMs, elapsedMs, pauseBlockedMs,
                wakeEvent.isPresent() ? "event" : "timeout-or-interrupted",
                wakeEvent.map(WindowReadyEvent::getType).orElse(null),
                wakeEvent.map(WindowReadyEvent::getSequence).orElse(-1L),
                outcome.message());
        if (wakeEvent.isEmpty() && remainingPreCombatWatchdogBudgetMs(
                adjustedOutcome.nextState(), System.currentTimeMillis()) <= 0L) {
            return preCombatWatchdogTimeoutOutcome(adjustedOutcome.nextState(), waitSpec.getReason().name(),
                    "event-wait-timeout");
        }
        return adjustedOutcome;
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
                        + " limitMs=" + PRE_COMBAT_WATCHDOG_TIMEOUT_MS);
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

    private XiuluoRoundContext resolveStartupTrackerOrReturnItem(TaskExecutionContext context,
                                                                 XiuluoRoundContext roundContext,
                                                                 String source) {
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
        uiCleanerService.cleanUpAll();
        return roundContext.recoverTo(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC,
                source + "-return-unverified");
    }

    @Deprecated
    private XiuluoRoundContext resolveStartupTaskPanelHotStart(TaskExecutionContext context,
                                                               XiuluoRoundContext roundContext) {
        /*
         * Deprecated startup path: this opens/reads the old Alt+Q task panel. Startup now uses
         * the tracker-first hot-start path instead, then return item, then normal accept flow.
         * Keep this method only as retained legacy code for comparison/debugging; do not wire it
         * back into normal 修罗 startup without a new behavior card.
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
        if (shouldYieldForTeamReturnSignal()) {
            log.warn("[xiuluo-v2] team return signal present before accept flow; yield for members");
            return XiuluoStepOutcome.sharedState(
                    state.next(XiuluoPhase.WAIT_TEAM_RETURN, TEAM_RETURN_BEFORE_ACCEPT_SOURCE),
                    "team return pending before accept flow");
        }
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
            if (npcClickService.clickNpcSmart(ACCEPT_NPC.toClickRequest(me, TaskType.XIULUO_V2))) {
                return XiuluoStepOutcome.continueTo(
                        activeState.next(XiuluoPhase.ACCEPT_TASK_DIALOG, "nearby-accept-npc-clicked"),
                        "accept NPC clicked from nearby position");
            }
            log.info("[xiuluo-v2] nearby task NPC direct click failed; fallback to minimap navigation: npc={}",
                    ACCEPT_NPC.getName());
        }
        // 🧭 ACCEPT NPC NAV: go to the fixed task giver before opening/handling its dialog.
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(ACCEPT_NPC.getMapName())
                .targetX(ACCEPT_NPC.getX())
                .targetY(ACCEPT_NPC.getY())
                .targetName(ACCEPT_NPC.getName())
                .keepTurnOnCurrentMapPathing(true)
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
        boolean clicked = npcClickService.clickNpcSmart(ACCEPT_NPC.toClickRequest(me, TaskType.XIULUO_V2));
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
            DialogResult businessDialog = dialogService.handleDialog(DialogHandleRequest.handleBusinessOption(
                    "xiuluo-v2:accept-unmatched-dialog:" + state.source()));
            if (businessDialog.getStatus() == DialogResultStatus.INTERRUPTED) {
                return XiuluoStepOutcome.stopped(state, "accept dialog handler interrupted");
            }
            if (businessDialog.getStatus() == DialogResultStatus.BUSINESS_OPTION_CLICKED) {
                log.info("[xiuluo-v2] accept dialog delegated to DialogService business handler: actionKey={} status={}",
                        businessDialog.getActionKey(), businessDialog.getStatus());
                /*
                 * Only the team maintenance broadcasts need a turn handoff here. Other option
                 * dialogs should not make the leader yield during accept/hot-start recovery.
                 */
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

        log.warn("[xiuluo-v2] background objective parse failed; synchronous fallback skipped because CR56 owns the background result: round={} source={} window={}",
                state.round(), state.source(), currentWindowLabel());
        return recoverBackgroundObjectiveReadFailure(context, state, "objective background parse failed");
    }

    private XiuluoStepOutcome afterAcceptMaintenanceCheck(TaskExecutionContext context, XiuluoRoundContext state) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        /*
         * CR120: the box pending TTL is a hard 30s window from return-home detection. Consume it as
         * soon as the next task is accepted, before prepath or maintenance can spend that window.
         */
        consumeCommonBoxDuringNextTaskProgress(context, "xiuluo-v2:after-accept-maintenance-check");
        if (!isHealPetMaintenanceDue() && !isRepairEquipmentMaintenanceDue()) {
            log.info("[xiuluo-v2] accept maintenance not due; start exit prepath before consuming accept tracker snapshot: round={} source={}",
                    state.round(), state.source());
            XiuluoStepOutcome prepath = startLeavingStartMapIfPresent(
                    context,
                    state, XiuluoPhase.TRY_TRACKER_SHORTCUT,
                    "after-accept-no-maintenance-start-exit-prepath");
            if (prepath != null) {
                return prepath;
            }
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.TRY_TRACKER_SHORTCUT, "after-accept-no-maintenance-no-prepath"),
                    "after-accept maintenance not due; consume accept tracker snapshot");
        }

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

        long maintenanceStartedAt = System.currentTimeMillis();
        TaskMaintenanceResult maintenanceResult = taskMaintenanceService.runOpportunisticMaintenance(context,
                TaskMaintenanceRequest.builder()
                        .sourceTask(broadcastSource)
                        .handleMaintenanceBroadcast(true)
                        .cleanSummonSkill(false)
                        .build());
        long blockedMs = System.currentTimeMillis() - maintenanceStartedAt;
        if (maintenanceResult.getStatus() == TaskMaintenanceStatus.INTERRUPTED) {
            return MaintenanceAttemptResult.withOutcome(
                    XiuluoStepOutcome.stopped(state, hookName + " broadcast interrupted"));
        }
        if (maintenanceResult.isBroadcastHandled()) {
            log.info("[xiuluo-v2] {} hook handled broadcast: status={} message={}",
                    hookName, maintenanceResult.getStatus(), maintenanceResult.getMessage());
            return MaintenanceAttemptResult.handledResult(blockedMs);
        }
        log.warn("[xiuluo-v2] {} hook attempt failed: broadcast not handled status={} message={}",
                hookName, maintenanceResult.getStatus(), maintenanceResult.getMessage());
        return MaintenanceAttemptResult.retry();
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
            consumeCommonBoxDuringNextTaskProgress(context, "xiuluo-v2:start-exit-prepath");
            consumeDeferredPostCombatRecoveryDuringNextTaskProgress(context, "xiuluo-v2:start-exit-prepath");
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
        boolean clicked = inputSequences.moveAndClickLeft(
                "xiuluo-v2:trackerShortcutGreen:" + state.round(),
                point.x, point.y, 120, 150);
        if (!clicked) {
            return fallbackFromShortcut(context, state, "tracker-green-click-failed");
        }

        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        String intentSource = TRACKER_SHORTCUT_PATHING_SOURCE_PREFIX + ":" + state.round() + ":"
                + state.shortcutTrackerRetryCount();
        String shortcutTargetMap = resolveReadyShortcutObjectiveTargetMap(state);
        gameStateUtil.recordMovementIntent(intentSource);
        WindowPathingIntent pathingIntent = registerTrackerShortcutPathingIntent(runtime, intentSource,
                shortcutTargetMap);
        attachShortcutTargetMapUpgrade(runtime, state, pathingIntent);
        if (runtime != null) {
            registerXiuluoDialogInterest(runtime, DialogOperation.XIULUO_ENTER_BATTLE,
                    "xiuluo-v2:shortcut-enter-battle:" + state.round());
        }
        XiuluoRoundContext next = state.withShortcutTrackerClick(
                XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING,
                panel.getDetailRawPath(),
                point,
                pathingIntent == null ? null : pathingIntent.getIntentId(),
                "tracker-shortcut-green-clicked");
        openTeamPathingMaintenanceWindow(context, next, "tracker-shortcut-green-clicked");
        returnItemPrescanService.afterTrackerGreen(context, TASK_CODE, next.round(), RETURN_ITEM_TEMPLATE,
                ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                "xiuluo-v2:tracker-shortcut-green-clicked");
        consumeCommonBoxDuringNextTaskProgress(context, "xiuluo-v2:tracker-shortcut-green-clicked");
        consumeDeferredPostCombatRecoveryDuringNextTaskProgress(context, "xiuluo-v2:tracker-shortcut-green-clicked");
        log.info("[xiuluo-v2 shortcut] tracker green clicked: round={} click=({}, {}) detail={} retry={} firstAt={}",
                next.round(), point.x, point.y, panel.getDetailRawPath(),
                next.shortcutTrackerRetryCount(), next.firstTrackerGreenClickAtMs());
        return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                next, "tracker shortcut green clicked; wait runner/window facts"));
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
        XiuluoStepOutcome prepared = consumePreparedXiuluoEnterBattle(context, state,
                "shortcut-wait-prepared-enter-battle");
        if (prepared != null) {
            return prepared;
        }

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

        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot snapshot = runtime == null ? null : runtime.getPathingSnapshot();
        if (snapshot != null && snapshot.getState() == WindowPathingState.ACTIVE) {
            returnItemPrescanService.whilePathing(context, TASK_CODE, state.round(), RETURN_ITEM_TEMPLATE,
                    ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                    "xiuluo-v2:shortcut-watcher-active");
            XiuluoStepOutcome maintenanceOutcome = runLeaderPathingSummonSkillMaintenance(
                    context, state, "shortcut-watcher-active");
            if (maintenanceOutcome != null) {
                return maintenanceOutcome;
            }
            return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                    state, "tracker shortcut still pathing"));
        }
        if (snapshot != null && snapshot.getState() == WindowPathingState.ARRIVED
                && isShortcutTargetMapArrival(snapshot)) {
            closeTeamPathingMaintenanceWindow(context, state, "shortcut-target-map-arrived");
            log.info("[xiuluo-v2 shortcut] target map arrived; team maintenance closed and keep waiting for enter-battle: round={} currentMap={} targetMap={} retry={}",
                    state.round(), snapshot.getCurrentMapName(),
                    snapshot.getIntent() == null ? null : snapshot.getIntent().getTargetMapName(),
                    state.shortcutTrackerRetryCount());
            if (runtime != null) {
                runtime.clearPathingSignal("xiuluo shortcut target map arrival consumed");
            }
            return waitForTrackerShortcutWake(XiuluoStepOutcome.sharedState(
                    state, "shortcut target map arrived; wait prepared enter-battle"));
        }
        if (snapshot != null && (snapshot.getState() == WindowPathingState.ARRIVED
                || snapshot.getState() == WindowPathingState.STOPPED_AWAY)) {
            log.info("[xiuluo-v2 shortcut] pathing terminal; re-read tracker and click first green again: state={} message={} retry={}",
                    snapshot.getState(), snapshot.getMessage(), state.shortcutTrackerRetryCount());
            if (runtime != null) {
                runtime.clearPathingSignal("xiuluo shortcut consumed pathing terminal");
            }
            return tryTrackerShortcut(context,
                state.incrementShortcutTrackerRetry(XiuluoPhase.TRY_TRACKER_SHORTCUT,
                        "shortcut-pathing-terminal-retry"));
        }

        log.info("[xiuluo-v2 shortcut] runner-only pathing wait continues: round={} hasSnapshot={} state={} probeInProgress={} retry={}",
                state.round(), snapshot != null, snapshot == null ? null : snapshot.getState(),
                snapshot != null && snapshot.isProbeInProgress(), state.shortcutTrackerRetryCount());
        XiuluoStepOutcome maintenanceOutcome = runLeaderPathingSummonSkillMaintenance(
                context, state, "shortcut-runner-only-wait");
        if (maintenanceOutcome != null) {
            return maintenanceOutcome;
        }
        return waitForTrackerShortcutWake(XiuluoStepOutcome.pathingStarted(
                state, "tracker shortcut runner-only pathing wait"));
    }

    private boolean isShortcutTargetMapArrival(WindowPathingSnapshot snapshot) {
        WindowPathingIntent intent = snapshot == null ? null : snapshot.getIntent();
        return intent != null
                && intent.getType() == WindowPathingIntentType.TARGETED
                && intent.getSource() != null
                && intent.getSource().startsWith("xiuluo-v2:tracker-shortcut")
                && intent.getTargetMapName() != null
                && !intent.getTargetMapName().isBlank();
    }

    private XiuluoStepOutcome fallbackFromShortcut(TaskExecutionContext context,
                                                   XiuluoRoundContext state,
                                                   String reason) {
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
            return recoverTargetNavigationFailure(context, activeState, outcome.message());
        }
        return outcome;
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
        boolean clicked = npcClickService.clickNpcSmart(combatTarget.toClickRequest(gameContext.getMe(), TaskType.XIULUO_V2));
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
            returnItemPrescanService.whileInCombat(context, TASK_CODE, state.round(), RETURN_ITEM_TEMPLATE,
                    ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                    "xiuluo-v2:wait-combat");
            /*
             * Do not sleep while the task turn is held. Combat is a shared state: the leader should
             * release quickly so follower auto-battle windows can acquire the turn and press their
             * own auto-combat buttons.
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
            return waitForCombatStateWake(
                    XiuluoStepOutcome.sharedState(combatState, "combat still running"),
                    combatWaitAfterSequence);
        }
        /*
         * Clicking "看打!" is not proof that battle actually started. If the user nudges the mouse
         * or the game drops the click, the old flow stayed in WAIT_COMBAT forever. Keep this as a
         * short entry-confirm window, then either return to the normal confirmation phase or
         * re-register the shortcut prepared option so Runner can wake the task immediately.
         */
        if (isEnterBattleConfirmPending(state)) {
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
        return outcome.withWaitSpec(XiuluoWaitSpec.builder()
                .reason(XiuluoWaitReason.WAIT_TRACKER_SHORTCUT_PATHING)
                .wakeTypes(Set.of(WindowReadyEventType.PATHING_TERMINAL,
                        WindowReadyEventType.PREPARED_ACTION_READY,
                        WindowReadyEventType.COMBAT_STATE_CHANGED))
                .afterSequence(windowReadyEventBus.currentSequence())
                .timeoutMs(WAIT_TARGET_PATHING_TERMINAL_TIMEOUT_MS)
                .pathingSourcePrefix("xiuluo-v2:tracker-shortcut")
                .build());
    }

    private XiuluoStepOutcome consumePreparedXiuluoEnterBattle(TaskExecutionContext context,
                                                               XiuluoRoundContext state,
                                                               String reason) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return null;
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
            return XiuluoStepOutcome.failed(state, "prepared xiuluo enter battle click failed");
        }
        runtime.clearDialogInterest("xiuluo enter battle prepared consumed");
        closeTeamPathingMaintenanceWindow(context, state, "shortcut-enter-battle-prepared");
        autoCombatService.initializeForCurrentWindow();
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
                XiuluoStepOutcome.sharedState(combatState, "stale unknown-combat exit suppressed; wait for combat state"),
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
                    "unknown combat exit; return item verified");
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

        pendingTeamReturnPrecheck = null;
        log.warn("[xiuluo-v2] unknown combat exit could not verify return and no objective was found; restart accept chain");
        return XiuluoStepOutcome.continueTo(
                state.recoverTo(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "unknown-combat-return-unverified"),
                "unknown combat exit; return unverified, restart accept chain");
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
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(ACCEPT_NPC.getMapName())
                .targetX(ACCEPT_NPC.getX())
                .targetY(ACCEPT_NPC.getY())
                .targetName(ACCEPT_NPC.getName())
                .keepTurnOnCurrentMapPathing(true)
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
         * After using the Xiuluo return item, a dead member may need to click return-team. Do not
         * block inside this leader transaction: holding the task turn here prevents follower windows
         * from receiving the turn they need to click return. Instead, probe once and yield when the
         * leader-side signal is still visible.
         */
        if (!TEAM_RETURN_BEFORE_ACCEPT_SOURCE.equals(state.source())) {
            TeamReturnService.LeaderSignalPrecheckStatus precheck =
                    teamReturnService.consumeLeaderSignalPrecheck(
                            context, pendingTeamReturnPrecheck, "xiuluo-v2:" + state.source());
            pendingTeamReturnPrecheck = null;
            if (precheck.conclusive() && !precheck.signalPresent()) {
                taskMaintenanceService.closeLocalTeamReturnSupportWindow(context,
                        "xiuluo-v2:" + state.source() + ":precheck-not-needed");
                log.info("[xiuluo-v2] team return precheck says no wait needed: source={}", state.source());
                return XiuluoStepOutcome.continueTo(
                        state.next(XiuluoPhase.ROUND_DONE, "team-return-precheck-not-needed"),
                        "team return wait not needed");
            }
            if (precheck.conclusive() && precheck.signalPresent()) {
                taskMaintenanceService.openLocalTeamReturnSupportWindow(context,
                        "xiuluo-v2:" + state.source() + ":precheck-signal-present");
                log.warn("[xiuluo-v2] team return precheck saw return signal; yield for members source={}",
                        state.source());
                return XiuluoStepOutcome.sharedState(
                        state.next(XiuluoPhase.WAIT_TEAM_RETURN, keepTeamReturnWaitSource(state)),
                        "team return still pending");
            }
        }
        if (shouldYieldForTeamReturnSignal()) {
            taskMaintenanceService.openLocalTeamReturnSupportWindow(context,
                    "xiuluo-v2:" + state.source() + ":signal-present");
            log.warn("[xiuluo-v2] team return signal still present and under-five is disabled; yield for members");
            return XiuluoStepOutcome.sharedState(
                    state.next(XiuluoPhase.WAIT_TEAM_RETURN, keepTeamReturnWaitSource(state)),
                    "team return still pending");
        }
        taskMaintenanceService.closeLocalTeamReturnSupportWindow(context,
                "xiuluo-v2:" + state.source() + ":signal-cleared");
        if (TEAM_RETURN_BEFORE_ACCEPT_SOURCE.equals(state.source())) {
            return XiuluoStepOutcome.continueTo(
                    state.next(XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC, "team-return-ready-before-accept"),
                    "team return ready before accept flow");
        }
        return XiuluoStepOutcome.continueTo(
                state.next(XiuluoPhase.ROUND_DONE, "team-return-not-needed"),
                "team return wait not needed");
    }

    private boolean shouldYieldForTeamReturnSignal() {
        return !botProperties.isXiuluoAllowUnderFiveMembers()
                && teamReturnService.isReturnTeamSignalPresent();
    }

    private String keepTeamReturnWaitSource(XiuluoRoundContext state) {
        if (TEAM_RETURN_BEFORE_ACCEPT_SOURCE.equals(state.source())) {
            return TEAM_RETURN_BEFORE_ACCEPT_SOURCE;
        }
        return TEAM_RETURN_ROUND_DONE_SOURCE;
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
        return retryCurrentOrRecover(state, XiuluoPhase.ACCEPT_TASK_NAVIGATE_TO_NPC,
                "accept NPC click failed");
    }

    private XiuluoStepOutcome recoverAcceptDialogFailure(XiuluoRoundContext state) {
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
        TaskSleep.sleepOrStop(context, 1200L, "Xiuluo V2 task interrupted");
        return XiuluoStepOutcome.sharedState(
                state.next(XiuluoPhase.WAIT_COMBAT, reason),
                "direct combat click confirmed by battle radar");
    }

    private XiuluoStepOutcome enterBattleFromRecoveredDialog(TaskExecutionContext context,
                                                             XiuluoRoundContext state,
                                                             String reason) {
        autoCombatService.initializeForCurrentWindow();
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
        return verified;
    }

    private ReturnItemUseResult useReturnItem(TaskExecutionContext context,
                                              int round,
                                              String source,
                                              int attempt,
                                              int maxAttempts) {
        TaskCheckpoint.throwIfStopRequested(context, taskExecutionContextHolder, "Xiuluo V2 task interrupted");
        log.info("[xiuluo-v2] use return item and verify start map: source={} attempt={}/{}",
                source, attempt, maxAttempts);
        pendingTeamReturnPrecheck = teamReturnService.beginLeaderSignalPrecheck(
                context, "xiuluo-v2:return-home:" + source + ":attempt-" + attempt);
        boolean usedCached = returnItemPrescanService.useCached(context, TASK_CODE, round, RETURN_ITEM_TEMPLATE,
                ReturnItemPrescanService.Mode.MAIN_BAG_TASK_PAGE, 0,
                "xiuluo-v2:return-home:" + source + ":attempt-" + attempt);
        if (usedCached) {
            TaskSleep.sleepOrStop(context, RETURN_VERIFY_DELAY_MS, "Xiuluo V2 task interrupted");
            LocationInfo cachedReturn = playerStateService.syncMyPosition();
            if (cachedReturn != null && gameStateUtil.isSameMapName(cachedReturn.mapName, START_MAP_NAME)) {
                log.info("[xiuluo-v2] cached return item verified: source={} location={}", source, cachedReturn);
                returnItemPrescanService.completeRound(context, TASK_CODE, round, RETURN_ITEM_TEMPLATE,
                        "xiuluo-v2:cached-return-verified");
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
            returnItemPrescanService.completeRound(context, TASK_CODE, round, RETURN_ITEM_TEMPLATE,
                    "xiuluo-v2:return-home-verified");
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
                pendingTeamReturnPrecheck = null;
                if (trustedState == AutoCombatService.TickResult.IN_COMBAT) {
                    return ReturnHomeResult.STILL_IN_COMBAT;
                }
                return ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT;
            }
            uiCleanerService.cleanUpAll();
        }
        pendingTeamReturnPrecheck = null;
        return ReturnHomeResult.FAILED;
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
            Optional<XiuluoStepOutcome> rememberedAccept =
                    tryRememberedAcceptTaskOption(context, state, source, verifyDialogType);
            if (rememberedAccept.isPresent()) {
                return rememberedAccept;
            }
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

    private Optional<XiuluoStepOutcome> tryRememberedAcceptTaskOption(TaskExecutionContext context,
                                                                      XiuluoRoundContext state,
                                                                      String source,
                                                                      boolean verifyDialogType) {
        Optional<MemoryService.DialogChoiceEntry> remembered = memoryService.findStableTaskDialogChoice(
                TASK_CODE, ACCEPT_TASK_ACTION, ACCEPT_NPC_NAME);
        if (remembered.isEmpty()) {
            return Optional.empty();
        }

        MemoryService.DialogChoiceEntry entry = remembered.get();
        DialogResult rememberedResult = dialogService.handleDialog(DialogHandleRequest.handleRememberedChoiceOption(
                source + ":accept-memory", entry.getRelativeX(), entry.getRelativeY(),
                OPTION_ACCEPT_TASK, verifyDialogType));
        if (rememberedResult.isClicked() && OPTION_ACCEPT_TASK.equals(rememberedResult.getActionKey())) {
            log.info("[xiuluo-v2] remembered accept option clicked: source={} rel=({}, {})",
                    source, entry.getRelativeX(), entry.getRelativeY());
            npcClickService.confirmPendingSmartClick(
                    START_MAP_NAME,
                    ACCEPT_NPC_NAME,
                    ACCEPT_NPC_X,
                    ACCEPT_NPC_Y,
                    "DIALOG_TEMPLATE", source + ":accept memory consumed");
            TaskSleep.sleepOrStop(context, 250L, "Xiuluo V2 task interrupted");
            return Optional.of(continueAfterAcceptOptionClicked(state, "known-dialog:accept-task-memory"));
        }

        if (rememberedResult.getDialogType() == com.bot.dhxy.model.dialog.DialogType.OPTION
                || rememberedResult.getStatus() == DialogResultStatus.FAILED) {
            memoryService.recordDialogChoiceFailure(
                    TASK_CODE, ACCEPT_TASK_ACTION, ACCEPT_NPC_NAME, source + ":accept-memory-failed");
        }
        log.info("[xiuluo-v2] remembered accept option missed, fallback to template: source={} status={} dialogType={}",
                source, rememberedResult.getStatus(), rememberedResult.getDialogType());
        return Optional.empty();
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
        XiuluoRoundContext stateWithBackgroundParse = scheduleAcceptObjectiveBackgroundParse(state, source);
        log.info("[xiuluo-v2] accept option clicked; snapshot parse runs as fallback and shortcut will try tracker before objective route: source={} window={}",
                source, currentWindowLabel());
        return XiuluoStepOutcome.continueTo(
                stateWithBackgroundParse.next(XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK, source),
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

    private List<GreenTemplateClickSpec> xiuluoKnownOptionSpecs() {
        boolean allowUnderFive = botProperties.isXiuluoAllowUnderFiveMembers();
        GreenTemplateClickSpec underFiveSpec = allowUnderFive
                ? new GreenTemplateClickSpec(OPTION_UNDER_FIVE_CONFIRM, UNDER_FIVE_CONFIRM_TEMPLATE, -24, 24, 4)
                : new GreenTemplateClickSpec(OPTION_UNDER_FIVE_WAIT, UNDER_FIVE_WAIT_TEMPLATE, -24, 24, 4);
        return List.of(
                new GreenTemplateClickSpec(OPTION_ACCEPT_TASK, ACCEPT_OPTION_TEMPLATE, -5, 100, 4),
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
            return XiuluoStepOutcome.continueTo(
                    state.retrySamePhase("retry:" + state.phase()),
                    reason + "; retry current phase");
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
            return XiuluoStepOutcome.failed(recoveredState, message + "; recovery limit exceeded");
        }
        return XiuluoStepOutcome.continueTo(recoveredState, message);
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
             */
            return parseTaskPanelObjectiveByOcr(capture.imagePath(), source);
        } finally {
            if (image != null) {
                image.flush();
            }
        }
    }

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

    private enum ReturnHomeResult {
        VERIFIED,
        STILL_IN_COMBAT,
        FAILED_AFTER_TRUSTED_NOT_IN_COMBAT,
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
