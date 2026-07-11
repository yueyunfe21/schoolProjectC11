package com.bot.dhxy.task.wuhuan;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.model.navigation.NavigationResultStatus;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.model.npc.NpcTooltipType;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.pause.TaskPauseResumeFingerprint;
import com.bot.dhxy.model.pause.TaskPauseResumeReconcileResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelCacheEntry;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelNegativeResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelPrepareResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.exception.TaskFatalException;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.service.NpcClickService;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.service.TaskTrackerPanelService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.pause.TaskPauseResumeReconciler;
import com.bot.dhxy.task.startup.TaskStartupCheckResult;
import com.bot.dhxy.task.startup.TaskStartupCheckService;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.GameStateUtil.FlyingState;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowReadyEvent;
import com.bot.dhxy.window.model.WindowReadyEventType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowReadyEventBus;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.vision.OcrWindowScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Five-ring task implemented as an explicit phase machine.
 *
 * <p>This V2 entry intentionally reuses the old Five-ring service calls for NPC clicking, dialog
 * template handling, bag item giving, and auto-combat recovery.
 * The new part is only the phase controller: each step returns a structured next phase so hot-start,
 * retry, and turn-yield behavior is visible in logs.</p>
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class FiveRingTaskV2 implements GameTask {

    private static final String TASK_CODE = "wuhuan_v2";
    private static final String TASK_NAME = "五环";
    private static final String TARGET_MAP_NAME = "长安";
    private static final String TARGET_NPC_NAME = "云游大师";
    private static final long STARTUP_UI_CLEAN_PRECHECK_MAX_AGE_MS = 60_000L;
    private static final long STARTUP_FIRST_AID_PRECHECK_MAX_AGE_MS = 60_000L;
    private static final int NPC_COOR_X = 87;
    private static final int NPC_COOR_Y = 174;
    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/wuhuan/wuhuan_accept_first_option.png";
    private static final String OPTION_ACCEPT_TASK = "wuhuan.acceptTask";
    private static final String ALREADY_HAS_TASK_OPTION_TEMPLATE = "images/template/dialog/wuhuan/wuhuan_already_has_task_option.png";
    private static final String FINISHED_STORY_TEMPLATE = "images/template/dialog/wuhuan/wuhuan_task_finished_story.png";
    private static final String FINISHED_ONCE_STORY_TEMPLATE = "images/template/dialog/wuhuan/wuhuan_task_finished_once_story.png";
    private static final String DAILY_LIMIT_STORY_TEMPLATE = "images/template/dialog/wuhuan/wuhuan_daily_limit_story.png";
    private static final String KEY_ITEM_NAME = "wuhuan/shoe.png";
    private static final String GIVE_ITEM_MAP_NAME = "大雁塔二层";
    private static final String QUICK_SHOE_ANCHOR_TEMPLATE = "images/template/wuhuan/wuhuan_quick_shoe_anchor.png";
    private static final String QUICK_SHOE_FAST_ITEM_TEMPLATE = "images/template/fastItem/wuhuan_quick_shoe_shop_item.png";
    private static final String QUICK_SHOE_FAST_ITEM_ALT_TEMPLATE = "images/template/fastItem/wuhuan_quick_shoe_shop_item8.png";
    private static final String SHOE_SHOP_BUY_OPTION_TEMPLATE = "images/template/dialog/wuhuan/wuhuan_shop_buy_option.png";
    private static final String SHOE_SHOP_SHOE_TEMPLATE = "images/template/wuhuan/shoe.png";
    private static final String SHOE_SHOP_BUY_BUTTON_TEMPLATE = "images/template/wuhuan/wuhuan_buy_button.png";
    private static final int TASK_NPC_DIRECT_CLICK_DISTANCE = 10;
    private static final String SHOE_SHOP_ENTRY_TARGET_NAME = "牛记布店入口";
    private static final String SHOE_SHOP_ENTRY_NAV_SOURCE = "wuhuan-v2:shoe-shop-entry-exact-130-130";
    private static final String SHOE_SHOP_MAP_NAME = "牛记布店";
    private static final String SHOE_SHOP_RETURN_FALLBACK_NAV_SOURCE = "wuhuan-v2:shoe-shop-return-fallback-repair-npc";
    private static final String SHOE_SHOP_RETURN_FALLBACK_MAP_NAME = "洛阳城";
    private static final String SHOE_SHOP_RETURN_FALLBACK_NPC_NAME = "李道宗";
    private static final int SHOE_SHOP_RETURN_FALLBACK_NPC_X = 324;
    private static final int SHOE_SHOP_RETURN_FALLBACK_NPC_Y = 109;
    private static final String SHOE_SHOP_OWNER_NAME = "服装店老板";
    private static final String PREPARED_TRACKER_PANEL_PATHING_SOURCE_PREFIX = "wuhuan-v2:prepared-tracker-panel-click:";
    private static final String TRACKER_GREEN_CLICK_PATHING_SOURCE_PREFIX = "wuhuan-v2:tracker-green-click:";
    private static final int SHOE_SHOP_ENTRY_X = 130;
    private static final int SHOE_SHOP_ENTRY_Y = 130;
    private static final int SHOE_SHOP_OWNER_X = 13;
    private static final int SHOE_SHOP_OWNER_Y = 9;
    private static final String ACCEPT_NPC_NAV_SOURCE = "wuhuan-v2:acceptNpc:navigate";
    private static final int SHOE_SHOP_ITEM_REL_LEFT = 364;
    private static final int SHOE_SHOP_ITEM_REL_TOP = 253;
    private static final int SHOE_SHOP_ITEM_REL_RIGHT = 672;
    private static final int SHOE_SHOP_ITEM_REL_BOTTOM = 458;
    private static final int SHOE_SHOP_RETURN_REL_X = 364;
    private static final int SHOE_SHOP_RETURN_REL_Y = 554;
    private static final int SHOE_SHOP_ENTRY_MAX_ATTEMPTS = 3;
    private static final long SHOE_SHOP_ENTRY_CONFIRM_TIMEOUT_MS = 10_000L;
    private static final long SHOE_SHOP_ENTRY_DOOR_CONFIRM_MS = 2_000L;
    private static final long SHOE_SHOP_ENTRY_POST_DISMOUNT_CONFIRM_MS = 1_500L;
    private static final long SHOE_SHOP_RETURN_FAST_VERIFY_TIMEOUT_MS = 2_500L;
    private static final long SHOE_SHOP_RETURN_FAST_VERIFY_POLL_MS = 250L;
    private static final double SHOE_SHOP_RETURN_FAST_VERIFY_SAME_TOLERANCE = 0.35;
    private static final long SHOE_SHOP_DISMOUNT_SETTLE_MS = 1_000L;
    private static final int SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS = 3;
    private static final int SHOE_SHOP_RETURN_MAX_ATTEMPTS = 3;
    private static final int SHOE_SHOP_BUY_BUTTON_FALLBACK_REL_X = 627;
    private static final int SHOE_SHOP_BUY_BUTTON_FALLBACK_REL_Y = 493;
    private static final int SHOE_SHOP_RETURN_VERIFY_REL_LEFT = 140;
    private static final int SHOE_SHOP_RETURN_VERIFY_REL_TOP = 160;
    private static final int SHOE_SHOP_RETURN_VERIFY_REL_WIDTH = 520;
    private static final int SHOE_SHOP_RETURN_VERIFY_REL_HEIGHT = 360;
    private static final int QUICK_SHOE_ANCHOR_REL_LEFT = 1016;
    private static final int QUICK_SHOE_ANCHOR_REL_TOP = 699;
    private static final int QUICK_SHOE_ANCHOR_REL_RIGHT = 1029;
    private static final int QUICK_SHOE_ANCHOR_REL_BOTTOM = 732;
    private static final int QUICK_SHOE_FAST_ITEM_REL_LEFT = 719;
    private static final int QUICK_SHOE_FAST_ITEM_REL_TOP = 700;
    private static final int QUICK_SHOE_FAST_ITEM_REL_RIGHT = 1013;
    private static final int QUICK_SHOE_FAST_ITEM_REL_BOTTOM = 736;
    private static final int QUICK_SHOE_SHOP_ITEM_REL_LEFT = 365;
    private static final int QUICK_SHOE_SHOP_ITEM_REL_TOP = 250;
    private static final int QUICK_SHOE_SHOP_ITEM_REL_RIGHT = 673;
    private static final int QUICK_SHOE_SHOP_ITEM_REL_BOTTOM = 454;
    private static final int QUICK_SHOE_BUY_BUTTON_REL_X = 623;
    private static final int QUICK_SHOE_BUY_BUTTON_REL_Y = 534;
    private static final int MAX_ACCEPT_RETRY = 5;
    private static final int MAX_UI_ERROR_BEFORE_CLEANUP = 3;
    private static final int MAX_GIVE_ITEM_FAILURE_BEFORE_FAIL = 6;
    private static final int MAX_TRACKER_NOT_FOUND_BEFORE_FAIL = 9;
    private static final int MAX_PHASE_LOOP_GUARD = 80;
    private static final long PATHING_HANDOFF_DELAY_MS = 250L;
    private static final long PATHING_RECHECK_GRACE_MS = 2_000L;
    private static final long PATHING_OBSERVER_FAST_WAIT_MS = 2_500L;
    private static final long PATHING_TARGET_WAIT_TIMEOUT_MS = 90_000L;
    private static final long OBSERVER_SNAPSHOT_MAX_AGE_MS = 3_000L;
    private static final long PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS = 10_000L;
    private static final long PREPARED_TRACKER_ACTION_MAX_AGE_MS = 2_500L;
    private static final long TRACKER_NEGATIVE_MAX_AGE_MS = PREPARED_TRACKER_ACTION_MAX_AGE_MS;
    private static final long ACCEPT_SETUP_POSITION_PREWARM_MAX_AGE_MS = 20_000L;
    private static final long READY_EVENT_PRIORITY_MAX_AGE_MS = 3_000L;
    private static final long READY_EVENT_PENDING_WARN_MS = 3_000L;
    private static final long READY_EVENT_SETTLE_WAIT_MS = 80L;
    private static final long READY_EVENT_PRIORITY_YIELD_DELAY_MS = 180L;
    private static final long PATHING_INTENT_CREATED_AT_GRACE_MS = 1_000L;
    private static final long TASK_TURN_HANDOFF_DELAY_MS = 900L;
    private static final double WUHUAN_TRACKER_COMBAT_ROI_SAME_TOLERANCE = 0.08;
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;

    private final BotProperties botProperties;
    private final GameContext gameContext;
    private final NavigationService navigationService;
    private final NpcClickService npcClickService;
    private final DialogService dialogService;
    private final PlayerStateService playerStateService;
    private final AutoCombatService autoCombatService;
    private final BagService bagService;
    private final GameStateUtil gameStateUtil;
    private final UICleanerService uiCleanerService;
    private final TaskStartupCheckService taskStartupCheckService;
    private final TaskTrackerPanelService taskTrackerPanelService;
    private final TaskTransactionRunner taskTransactionRunner;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowReadyEventBus windowReadyEventBus;
    private final TextRecognizer textRecognizer;
    private final InputSequences inputSequences;
    private final AutomationMetricsService automationMetricsService;
    private final TaskPauseResumeReconciler taskPauseResumeReconciler;
    private volatile CompletableFuture<AcceptSetupPositionPrewarm> acceptSetupPositionPrewarm;

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
     * Execute one or more Five-ring runs through the V2 phase machine.
     *
     * @param executionContext nullable runner context. When null, a minimal context is created so
     *                         stop checks and log labels still have a task code/name.
     * @return SUCCESS after the configured 五环 run count finishes, STOPPED if interrupted, or FAILED
     *         only when a phase reaches an unrecoverable state.
     */
    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        TaskExecutionContext context = resolveExecutionContext(executionContext);
        int maxRuns = botProperties.getWuhuanMaxRuns();
        int completedRuns = 0;

        log.info("====================================");
        log.info("[five-ring-v2] start automated five-ring task: maxRuns={}",
                isUnlimitedRuns(maxRuns) ? "unlimited" : maxRuns);
        log.info("====================================");

        try {
            TaskStartupCheckResult checkResult = taskStartupCheckService.checkFiveRing(context);
            if (checkResult.isBlocked()) {
                log.info("[five-ring-v2] startup check blocked: {}", checkResult.getReason());
                return checkResult.getBlockedResult();
            }
            log.info("[five-ring-v2] startup check passed: {}", checkResult.getReason());

            gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
            if (context.getWindowRuntimeContext() != null) {
                context.getWindowRuntimeContext().updateTaskRunProgress(completedRuns, maxRuns);
            }
            while (shouldStartNextRun(maxRuns, completedRuns)) {
                TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
                int round = completedRuns + 1;
                boolean cleanQueueTransitionStartup = completedRuns == 0 && context.isCleanQueueTransitionStartup();
                if (cleanQueueTransitionStartup) {
                    log.info("[five-ring-v2] clean queued task transition: keep prepare/buy-shoes checks, skip handover and force accept NPC navigation");
                }
                FiveRingPhaseContext phaseContext = cleanQueueTransitionStartup
                        ? FiveRingPhaseContext.cleanTransitionStart(round)
                        : FiveRingPhaseContext.start(round);
                log.info("[five-ring-v2] run {} initial phase: phase={} source={}",
                        round, phaseContext.phase(), phaseContext.source());

                String roundId = roundMetricId(context, TASK_CODE, round);
                long roundStartedAt = System.currentTimeMillis();
                automationMetricsService.recordRoundStarted(context, roundId, round,
                        roundMetricType(phaseContext), "五环轮次开始",
                        Map.of("sourcePhase", phaseContext.phase().name(), "source", phaseContext.source()));
                FiveRingRunResult runResult;
                try {
                    runResult = runPhases(context, phaseContext);
                } catch (RuntimeException e) {
                    finishRoundMetric(context, roundId, round, phaseContext, TaskRunResult.FAILED,
                            roundStartedAt, "五环轮次异常: " + e.getClass().getSimpleName());
                    throw e;
                }
                finishRoundMetric(context, roundId, round, phaseContext, runResult.taskResult(),
                        roundStartedAt, runResult.terminalTask() ? "五环终止条件结束" : "五环轮次结束");
                if (runResult.taskResult() != TaskRunResult.SUCCESS) {
                    gameContext.setBotStatus(runResult.taskResult() == TaskRunResult.STOPPED
                            ? GameContext.BotStatus.IDLE
                            : GameContext.BotStatus.ERROR);
                    return runResult.taskResult();
                }

                completedRuns++;
                if (context.getWindowRuntimeContext() != null) {
                    context.getWindowRuntimeContext().updateTaskRunProgress(completedRuns, maxRuns);
                }
                log.info("[five-ring-v2] run {} finished, completed={} terminalTask={}",
                        round, completedRuns, runResult.terminalTask());
                if (runResult.terminalTask()) {
                    break;
                }
            }

            markTaskIdle();
            return TaskRunResult.SUCCESS;
        } catch (TaskStopRequestedException e) {
            log.info("[five-ring-v2] stopped: {}", e.getMessage());
            markTaskIdle();
            return TaskRunResult.STOPPED;
        } catch (Exception e) {
            log.error("[five-ring-v2] task failed with exception", e);
            markTaskFailed();
            return TaskRunResult.FAILED;
        } finally {
            taskTransactionRunner.forceReleaseTurn("wuhuan-v2:execute-finished");
        }
    }

    @Override
    public void stop() {
        log.info("[five-ring-v2] stop requested");
        markTaskIdle();
    }

    private String roundMetricId(TaskExecutionContext context, String taskCode, int round) {
        long taskRunId = context == null ? 0L : context.getTaskRunId();
        String windowId = context == null ? "window" : context.getWindowId();
        return taskCode + "-" + (taskRunId > 0L ? taskRunId : windowId) + "-round-" + round;
    }

    private void finishRoundMetric(TaskExecutionContext context,
                                   String roundId,
                                   int round,
                                   FiveRingPhaseContext phaseContext,
                                   TaskRunResult result,
                                   long roundStartedAt,
                                   String message) {
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - roundStartedAt);
        automationMetricsService.recordRoundFinished(context, roundId, round, roundMetricType(phaseContext),
                roundMetricStatus(result), roundResultCode(result), message, elapsedMs,
                Map.of("sourcePhase", phaseContext.phase().name(), "source", phaseContext.source()));
    }

    private String roundMetricType(FiveRingPhaseContext phaseContext) {
        return phaseContext == null ? "五环" : "五环/" + phaseContext.phase();
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

    private FiveRingRunResult runPhases(TaskExecutionContext context, FiveRingPhaseContext initialContext) {
        FiveRingPhaseContext phaseContext = initialContext;
        int phaseLoopGuard = 0;

        while (!phaseContext.phase().isTerminal()) {
            TaskPauseResumeFingerprint pauseFingerprint = taskPauseResumeReconciler.capture(
                    context, TaskType.WUHuan_V2, phaseContext.phase().name(), "phase-loop");
            long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
            TaskPauseResumeReconcileResult pauseReconcile = taskPauseResumeReconciler.reconcileAfterPause(
                    pauseFingerprint, context, pauseBlockedMs);
            if (pauseReconcile.isFallbackTaskHotStart()) {
                FiveRingPhaseContext hotStart = phaseContext.pauseResumeHotStart(
                        "pause-resume-hot-start:phase-loop");
                log.warn("[five-ring-v2 pause-resume] fallback task hot-start: round={} phase={} next={} pauseBlockedMs={} fingerprintMatched={} mismatchReason={} compensatedTimers={} clearedVolatileState={} fallbackTaskHotStart={}",
                        phaseContext.round(), phaseContext.phase(), hotStart.phase(),
                        pauseReconcile.getPauseBlockedMs(), pauseReconcile.isFingerprintMatched(),
                        pauseReconcile.getMismatchReason(), pauseReconcile.getCompensatedTimers(),
                        pauseReconcile.getClearedVolatileState(), pauseReconcile.isFallbackTaskHotStart());
                phaseContext = hotStart;
                phaseLoopGuard = 0;
                continue;
            }
            if (pauseReconcile.isFingerprintMatched() && pauseReconcile.getPauseBlockedMs() > 0L) {
                phaseContext = phaseContext.pauseInternalAutomationTimers(
                        pauseReconcile.getPauseBlockedMs(), "pause-resume:phase-loop");
                log.info("[five-ring-v2 pause-resume] continue original phase: round={} phase={} pauseBlockedMs={} fingerprintMatched={} mismatchReason={} compensatedTimers={} clearedVolatileState={} fallbackTaskHotStart={}",
                        phaseContext.round(), phaseContext.phase(), pauseReconcile.getPauseBlockedMs(),
                        pauseReconcile.isFingerprintMatched(), pauseReconcile.getMismatchReason(),
                        pauseReconcile.getCompensatedTimers(), pauseReconcile.getClearedVolatileState(),
                        pauseReconcile.isFallbackTaskHotStart());
            }

            FiveRingPhaseContext currentContext = phaseContext;
            AtomicReference<FiveRingStepOutcome> phaseOutcome = new AtomicReference<>();
            /*
             * Only short startup/business decisions should hold the coarse task turn. Navigation,
             * pathing waits, and tracker OCR follow the validated navigation-stress model: compute
             * in the bound window context, serialize only physical input through InputSequences,
             * and release the coarse turn as soon as a movement click has been submitted.
             */
            String transactionName = "wuhuan-v2:" + currentContext.phase();
            TaskTransactionOutcome transaction;
            boolean outsideTaskTurnPhase = currentContext.phase() == FiveRingPhase.WAIT_PATHING
                    || currentContext.phase() == FiveRingPhase.BUY_SHOES
                    || currentContext.phase() == FiveRingPhase.ACCEPT_TASK
                    || currentContext.phase() == FiveRingPhase.HANDLE_DIALOG
                    || currentContext.phase() == FiveRingPhase.SYNC_TASK_PANEL;
            FiveRingStepOutcome priorityOutcome = checkReadyPriorityBeforeOutsidePhase(
                    context, currentContext, outsideTaskTurnPhase);
            boolean releaseTurnOnOutsideEnter = shouldReleaseTurnOnOutsidePhaseEnter(currentContext);
            if (priorityOutcome != null) {
                phaseOutcome.set(priorityOutcome);
                transaction = new TaskTransactionOutcome(
                        transactionName,
                        TaskTransactionResult.READY_TO_CONTINUE,
                        TaskYieldPolicy.MUST_YIELD,
                        priorityOutcome.transactionResult(),
                        true);
            } else if (currentContext.phase() == FiveRingPhase.BUY_SHOES) {
                transaction = runPhaseWithoutTaskTurn(
                        currentContext, phaseOutcome, transactionName, "buyShoes",
                        releaseTurnOnOutsideEnter,
                        () -> buyShoes(context, currentContext));
            } else if (currentContext.phase() == FiveRingPhase.WAIT_PATHING) {
                transaction = runPhaseWithoutTaskTurn(
                        currentContext, phaseOutcome, transactionName, "pathWait",
                        releaseTurnOnOutsideEnter,
                        () -> waitPathing(context, currentContext));
            } else if (currentContext.phase() == FiveRingPhase.ACCEPT_TASK) {
                transaction = runPhaseWithoutTaskTurn(
                        currentContext, phaseOutcome, transactionName, "accept",
                        releaseTurnOnOutsideEnter,
                        () -> acceptTask(context, currentContext));
            } else if (currentContext.phase() == FiveRingPhase.HANDLE_DIALOG) {
                transaction = runPhaseWithoutTaskTurn(
                        currentContext, phaseOutcome, transactionName, "handleDialog",
                        releaseTurnOnOutsideEnter,
                        () -> handleDialog(context, currentContext));
            } else if (currentContext.phase() == FiveRingPhase.SYNC_TASK_PANEL) {
                transaction = runPhaseWithoutTaskTurn(
                        currentContext, phaseOutcome, transactionName, "trackerSync",
                        releaseTurnOnOutsideEnter,
                        () -> syncTaskPanel(context, currentContext, true));
            } else {
                transaction = taskTransactionRunner.run(
                        transactionName,
                        TaskTransactionResult.READY_TO_CONTINUE,
                        TaskYieldPolicy.CONTINUE_CHAIN,
                        () -> {
                            FiveRingStepOutcome outcome = runPhase(context, currentContext);
                            phaseOutcome.set(outcome);
                            return outcome.transactionResult();
                        });
            }

            FiveRingStepOutcome outcome = phaseOutcome.get();
            if (outcome == null) {
                outcome = FiveRingStepOutcome.failed(currentContext, "phase produced no outcome");
            }
            log.info("[five-ring-v2] phase outcome: round={} phase={} result={} yield={} next={} accepted={} message={}",
                    currentContext.round(), currentContext.phase(), outcome.transactionResult(),
                    outcome.yieldPolicy(), outcome.nextState().phase(), outcome.nextState().taskAccepted(),
                    outcome.message());
            releaseHeldTurnAfterOutsidePhaseYield(outsideTaskTurnPhase, transactionName, outcome);

            if (transaction.result() == TaskTransactionResult.STOPPED
                    || outcome.transactionResult() == TaskTransactionResult.STOPPED) {
                return new FiveRingRunResult(TaskRunResult.STOPPED, false);
            }
            if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
                return new FiveRingRunResult(TaskRunResult.FAILED, false);
            }
            if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD) {
                yieldAfterMustYield(context, outcome);
            }
            if (outcome.terminalTask()) {
                return new FiveRingRunResult(TaskRunResult.SUCCESS, true);
            }
            if (outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED
                    || outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
                phaseLoopGuard = 0;
            } else if (++phaseLoopGuard > MAX_PHASE_LOOP_GUARD) {
                log.error("[five-ring-v2] phase loop guard exceeded: round={} phase={} source={}",
                        currentContext.round(), currentContext.phase(), currentContext.source());
                return new FiveRingRunResult(TaskRunResult.FAILED, false);
            }

            phaseContext = outcome.nextState();
        }

        if (phaseContext.phase() == FiveRingPhase.STOPPED) {
            return new FiveRingRunResult(TaskRunResult.STOPPED, false);
        }
        return new FiveRingRunResult(
                phaseContext.phase() == FiveRingPhase.FAILED ? TaskRunResult.FAILED : TaskRunResult.SUCCESS,
                false);
    }

    private void releaseHeldTurnAfterOutsidePhaseYield(boolean outsideTaskTurnPhase,
                                                       String transactionName,
                                                       FiveRingStepOutcome outcome) {
        if (!outsideTaskTurnPhase || outcome == null || !shouldYieldTaskTurn(outcome)) {
            return;
        }
        /*
         * Some 五环 phases run outside TaskTransactionRunner so expensive watcher/OCR waits do not
         * hold the coarse turn. They may still be entered after an earlier PREPARE/HANDOVER step kept
         * the same thread's turn. When such an outside phase starts pathing or reaches a shared state,
         * explicitly release that inherited hold; otherwise the log shows PATHING_STARTED while every
         * other window is still waiting for the stale turn.
         */
        taskTransactionRunner.forceReleaseTurn(transactionName + ":outside-yield");
    }

    private boolean shouldYieldTaskTurn(FiveRingStepOutcome outcome) {
        TaskTransactionResult result = outcome.transactionResult();
        return result == TaskTransactionResult.PATHING_STARTED
                || result == TaskTransactionResult.SHARED_STATE_TRIGGERED
                || result == TaskTransactionResult.RETRYABLE_ERROR
                || result == TaskTransactionResult.TASK_FINISHED
                || result == TaskTransactionResult.FAILED
                || result == TaskTransactionResult.STOPPED
                || outcome.yieldPolicy() != TaskYieldPolicy.CONTINUE_CHAIN;
    }

    private boolean shouldReleaseTurnOnOutsidePhaseEnter(FiveRingPhaseContext state) {
        if (state == null) {
            return true;
        }
        if (state.waitingAcceptNpcPathing()
                || state.pathingIntentExpected()
                || state.pathingStartedAtMs() > 0L) {
            return true;
        }
        String source = state.source() == null ? "" : state.source();
        /*
         * Startup preparation is still a short, input-sensitive chain. Keep its inherited coarse
         * turn until the first concrete navigation click has either started pathing or yielded a
         * retryable/shared result; later watcher/OCR waits keep the CR186 outside-enter release.
         */
        if (state.phase() == FiveRingPhase.BUY_SHOES) {
            return !source.startsWith("prepare-shoe-shop");
        }
        if (state.phase() == FiveRingPhase.ACCEPT_TASK) {
            return !("prepare-done".equals(source)
                    || "handover-setup-required".equals(source)
                    || "handover-tracker-unavailable-setup-required".equals(source));
        }
        return true;
    }

    private FiveRingStepOutcome checkReadyPriorityBeforeOutsidePhase(TaskExecutionContext context,
                                                                     FiveRingPhaseContext state,
                                                                     boolean outsideTaskTurnPhase) {
        if (!outsideTaskTurnPhase) {
            return null;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return null;
        }

        WindowReadyEvent currentReady = windowReadyEventBus
                .latest(runtime.getWindowId(), WindowReadyEventType.TASK_ATTENTION_REQUIRED)
                .orElse(null);
        FiveRingStepOutcome currentPrepared = consumeCurrentPreparedBeforeNormalPhase(
                context, state, runtime, currentReady, "phase-boundary-before:" + state.phase());
        if (currentPrepared != null) {
            return currentPrepared;
        }

        long currentLatestSequence = windowReadyEventBus
                .latest(runtime.getWindowId(), WindowReadyEventType.TASK_ATTENTION_REQUIRED)
                .map(WindowReadyEvent::getSequence)
                .orElse(0L);
        currentLatestSequence = Math.max(currentLatestSequence, windowReadyEventBus
                .latest(runtime.getWindowId(), WindowReadyEventType.TASK_TRACKER_NEGATIVE_READY)
                .map(WindowReadyEvent::getSequence)
                .orElse(0L));
        currentLatestSequence = Math.max(currentLatestSequence, windowReadyEventBus
                .latest(runtime.getWindowId(), WindowReadyEventType.PREPARED_ACTION_READY)
                .map(WindowReadyEvent::getSequence)
                .orElse(0L));
        EnumSet<WindowReadyEventType> currentWakeTypes = EnumSet.of(
                WindowReadyEventType.TASK_ATTENTION_REQUIRED,
                WindowReadyEventType.TASK_TRACKER_NEGATIVE_READY,
                WindowReadyEventType.PREPARED_ACTION_READY);
        if (state.waitingAcceptNpcPathing() || state.pathingIntentExpected()) {
            /*
             * The window's own pathing terminal (ARRIVED/STOPPED_AWAY) must wake this boundary wait
             * directly; otherwise a stopped character sits through the settle timeout and only the
             * other-window priority logic ever sees the terminal event.
             */
            currentWakeTypes.add(WindowReadyEventType.PATHING_TERMINAL);
        }
        Optional<WindowReadyEvent> currentWake = windowReadyEventBus.awaitNewer(
                runtime.getWindowId(),
                currentWakeTypes,
                currentLatestSequence,
                READY_EVENT_SETTLE_WAIT_MS);
        if (currentWake.isPresent()) {
            WindowReadyEvent event = currentWake.get();
            log.info("[five-ring-v2 priority] current window ready event observed before normal phase: checkpoint={} phase={} windowId={} hwnd={} source={} operation={} target={} sequence={} ageMs={}",
                    "phase-boundary-wait:" + state.phase(), state.phase(), runtime.getWindowId(),
                    event.getHwnd(), event.getSource(), event.getOperation(), event.getTargetKeyword(),
                    event.getSequence(), readyAgeMs(event));
            if (event.getType() == WindowReadyEventType.PATHING_TERMINAL) {
                // Own pathing terminal: fall through into the phase so the pathing-wait branch
                // consumes the snapshot; there is no prepared action to click for this event.
                return null;
            }
            currentPrepared = consumeCurrentPreparedBeforeNormalPhase(
                    context, state, runtime, event, "phase-boundary-wait:" + state.phase());
            if (currentPrepared != null) {
                return currentPrepared;
            }
            warnReadyPendingTooLongIfNeeded(event, state, runtime, false, "current-prepared-not-usable");
        }

        Optional<WindowReadyEvent> otherPrepared = windowReadyEventBus.latestOtherFreshPreparedAction(
                runtime.getWindowId(), TaskType.WUHuan_V2, READY_EVENT_PRIORITY_MAX_AGE_MS);
        if (otherPrepared.isPresent()) {
            return yieldToReadyEvent(state, runtime, otherPrepared.get(),
                    "prepared-action-priority-yield",
                    "prepared action priority yield");
        }

        if (state.phase() == FiveRingPhase.WAIT_PATHING) {
            return null;
        }
        Optional<WindowReadyEvent> otherTerminal = windowReadyEventBus.latestOtherFreshPathingTerminal(
                runtime.getWindowId(), TaskType.WUHuan_V2, READY_EVENT_PRIORITY_MAX_AGE_MS);
        if (otherTerminal.isEmpty()) {
            return null;
        }
        return yieldToReadyEvent(state, runtime, otherTerminal.get(),
                "pathing-terminal-priority-yield",
                "pathing terminal priority yield");
    }

    private FiveRingStepOutcome yieldToReadyEvent(FiveRingPhaseContext state,
                                                  WindowRuntimeContext runtime,
                                                  WindowReadyEvent event,
                                                  String stateReason,
                                                  String message) {
        long ageMs = readyAgeMs(event);
        log.info("[five-ring-v2 priority] phase yields because another window has executable ready event: checkpoint={} phase={} currentWindowId={} readyWindowId={} readyHwnd={} readyType={} readyState={} readySource={} readyOperation={} readyTarget={} readySeq={} readyAgeMs={} reason={}",
                "phase-boundary-other:" + state.phase(), state.phase(), runtime.getWindowId(),
                event.getWindowId(), event.getHwnd(), event.getType(), event.getPathingState(),
                event.getSource(), event.getOperation(), event.getTargetKeyword(), event.getSequence(),
                ageMs, stateReason);
        log.info("[five-ring-v2 priority] normal retry skipped because executable ready event has priority: phase={} source={} currentWindowId={} readyWindowId={} reason={}",
                state.phase(), state.source(), runtime.getWindowId(), event.getWindowId(), stateReason);
        warnReadyPendingTooLongIfNeeded(event, state, runtime, true, "yield-to-ready-window");
        return FiveRingStepOutcome.sharedState(
                state.retrySamePhase(stateReason),
                message);
    }

    private FiveRingStepOutcome consumeCurrentPreparedBeforeNormalPhase(TaskExecutionContext context,
                                                                       FiveRingPhaseContext state,
                                                                       WindowRuntimeContext runtime,
                                                                       WindowReadyEvent relatedReadyEvent,
                                                                       String checkpoint) {
        PreparedDialogAction action = runtime.getPreparedDialogAction();
        if (action == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (action.getOperation() == DialogOperation.TASK_TRACKER_PATHING
                && action.matches(DialogOperation.TASK_TRACKER_PATHING, "wuhuan")
                && action.verifiedWithin(now, PREPARED_TRACKER_ACTION_MAX_AGE_MS)
                && clickPreparedWuhuanTrackerGreen(context, "phase-priority:" + state.phase())) {
            log.info("[five-ring-v2 priority] long phase consumes current prepared action before continuing: checkpoint={} phase={} windowId={} hwnd={} readySeq={} readyAgeMs={} operation={} target={} source={} preparedAgeMs={} verifiedAgeMs={} click=({}, {})",
                    checkpoint, state.phase(), runtime.getWindowId(), action.getHwnd(),
                    relatedReadyEvent == null ? -1L : relatedReadyEvent.getSequence(),
                    readyAgeMs(relatedReadyEvent), action.getOperation(), action.getTargetKeyword(),
                    action.getSource(), Math.max(0L, now - action.getPreparedAtMs()),
                    Math.max(0L, now - action.getLastVerifiedAtMs()), action.getAbsoluteX(), action.getAbsoluteY());
            FiveRingPhaseContext trackerState = mergeWuhuanTrackerRegionsFromRuntimeCache(
                    state, runtime, "priority-prepared-tracker-link:" + checkpoint);
            return FiveRingStepOutcome.pathingStarted(
                    trackerState.withTaskAccepted("priority-prepared-tracker-link")
                            .next(FiveRingPhase.WAIT_PATHING, "priority-prepared-tracker-pathing-started")
                            .withNewWatcherPathingStarted(
                                    "priority-prepared-tracker-pathing-started",
                                    trackerPathingIntentSource("phase-priority:" + state.phase(), true)),
                    "prepared tracker dialog consumed before normal phase");
        }
        if (action.getOperation() == DialogOperation.ROUTE_TRANSFER
                && action.verifiedWithin(now, PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS)) {
            /*
             * Route clicks keep using NavigationService's existing consume path because that code
             * owns route-target and intent validation. This breadcrumb proves the phase gate saw the
             * prepared route and did not start unrelated normal OCR before the navigation path can
             * consume it.
             */
            log.info("[five-ring-v2 priority] long phase continues with current route prepared action reserved for navigation: checkpoint={} phase={} windowId={} hwnd={} readySeq={} readyAgeMs={} target={} source={} preparedAgeMs={} verifiedAgeMs={} click=({}, {})",
                    checkpoint, state.phase(), runtime.getWindowId(), action.getHwnd(),
                    relatedReadyEvent == null ? -1L : relatedReadyEvent.getSequence(),
                    readyAgeMs(relatedReadyEvent), action.getTargetKeyword(), action.getSource(),
                    Math.max(0L, now - action.getPreparedAtMs()),
                    Math.max(0L, now - action.getLastVerifiedAtMs()), action.getAbsoluteX(), action.getAbsoluteY());
            warnReadyPendingTooLongIfNeeded(relatedReadyEvent, state, runtime, true,
                    "route-action-awaiting-navigation-consumer");
            return null;
        }
        warnReadyPendingTooLongIfNeeded(relatedReadyEvent, state, runtime, false,
                stalePreparedReason(action, now));
        return null;
    }

    private long readyAgeMs(WindowReadyEvent event) {
        if (event == null || event.getCreatedAtMs() <= 0L) {
            return -1L;
        }
        return Math.max(0L, System.currentTimeMillis() - event.getCreatedAtMs());
    }

    private void warnReadyPendingTooLongIfNeeded(WindowReadyEvent event,
                                                 FiveRingPhaseContext state,
                                                 WindowRuntimeContext runtime,
                                                 boolean preparedUsable,
                                                 String staleReason) {
        long ageMs = readyAgeMs(event);
        if (event == null || ageMs < READY_EVENT_PENDING_WARN_MS) {
            return;
        }
        log.warn("[five-ring-v2 priority] ready dialog pending too long: ageMs={} readyWindowId={} readyHwnd={} readySeq={} readySource={} readyOperation={} readyTarget={} phase={} currentWindowId={} preparedUsable={} staleReason={}",
                ageMs, event.getWindowId(), event.getHwnd(), event.getSequence(), event.getSource(),
                event.getOperation(), event.getTargetKeyword(), state.phase(),
                runtime == null ? null : runtime.getWindowId(), preparedUsable, staleReason);
    }

    private String stalePreparedReason(PreparedDialogAction action, long now) {
        if (action == null) {
            return "no-prepared-action";
        }
        if (action.getOperation() == DialogOperation.TASK_TRACKER_PATHING
                && !action.matches(DialogOperation.TASK_TRACKER_PATHING, "wuhuan")) {
            return "prepared-target-mismatch:" + action.getTargetKeyword();
        }
        if (action.getOperation() == DialogOperation.TASK_TRACKER_PATHING
                && !action.verifiedWithin(now, PREPARED_TRACKER_ACTION_MAX_AGE_MS)) {
            return "tracker-prepared-stale:" + Math.max(0L, now - action.getLastVerifiedAtMs());
        }
        if (action.getOperation() == DialogOperation.ROUTE_TRANSFER
                && !action.verifiedWithin(now, PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS)) {
            return "route-prepared-stale:" + Math.max(0L, now - action.getLastVerifiedAtMs());
        }
        return "unsupported-prepared-operation:" + action.getOperation();
    }

    /**
     * Runs a 五环 phase outside the coarse task turn while preserving normal phase output handling.
     *
     * <p>This is the shared boundary for the pressure-test handoff model: expensive per-window reads
     * such as watcher polling, navigation preparation, or tracker OCR run in the bound window thread;
     * physical mouse/keyboard input remains serialized by {@link InputSequences} inside the called
     * services. The caller receives a transaction-shaped result so the existing state machine can
     * keep one result path instead of carrying multiple near-identical wrappers.</p>
     *
     * @param state current 五环 phase state.
     * @param phaseOutcome destination for the normal phase outcome used by the outer state machine.
     * @param transactionName diagnostic name written to latency logs.
     * @param latencyName short latency label, for example {@code pathWait} or {@code trackerSync}.
     * @param phaseAction actual phase body to execute without acquiring the coarse task turn.
     * @return transaction-shaped outcome so the existing phase loop can handle stop/fail/yield paths.
     */
    private TaskTransactionOutcome runPhaseWithoutTaskTurn(FiveRingPhaseContext state,
                                                           AtomicReference<FiveRingStepOutcome> phaseOutcome,
                                                           String transactionName,
                                                           String latencyName,
                                                           boolean releaseTurnOnEnter,
                                                           Supplier<FiveRingStepOutcome> phaseAction) {
        long startedAt = System.currentTimeMillis();
        TaskTransactionResult result = TaskTransactionResult.FAILED;
        boolean completed = false;
        /*
         * CR186: ready-priority has already had first chance in the caller. If this thread inherited
         * a CONTINUE_CHAIN turn from the previous phase, release only that coarse task turn before
         * entering navigation/OCR/pathing slow work; physical input still stays serialized by
         * InputSequences inside the called services. forceReleaseTurn is a no-op when no turn is held.
         */
        if (releaseTurnOnEnter) {
            taskTransactionRunner.forceReleaseTurn(transactionName + ":outside-enter");
        } else {
            log.info("[five-ring-v2 latency] {}OutsideTurnKeepInherited round={} phase={} source={} accepted={} ageMs={}",
                    latencyName, state.round(), state.phase(), state.source(), state.taskAccepted(),
                    pathingAgeMs(state));
        }
        log.info("[five-ring-v2 latency] {}OutsideTurnStart round={} phase={} source={} accepted={} ageMs={}",
                latencyName, state.round(), state.phase(), state.source(), state.taskAccepted(), pathingAgeMs(state));
        try {
            FiveRingStepOutcome outcome = phaseAction.get();
            phaseOutcome.set(outcome);
            result = outcome.transactionResult();
            completed = true;
            return new TaskTransactionOutcome(
                    transactionName,
                    TaskTransactionResult.READY_TO_CONTINUE,
                    TaskYieldPolicy.MUST_YIELD,
                    result,
                    true);
        } catch (TaskStopRequestedException e) {
            result = TaskTransactionResult.STOPPED;
            completed = true;
            FiveRingStepOutcome stopped = FiveRingStepOutcome.stopped(state, latencyName + " interrupted");
            phaseOutcome.set(stopped);
            return new TaskTransactionOutcome(
                    transactionName,
                    TaskTransactionResult.READY_TO_CONTINUE,
                    TaskYieldPolicy.MUST_YIELD,
                    result,
                    true);
        } catch (RuntimeException e) {
            if (Thread.currentThread().isInterrupted()) {
                result = TaskTransactionResult.STOPPED;
                completed = true;
                FiveRingStepOutcome stopped = FiveRingStepOutcome.stopped(state, latencyName + " interrupted");
                phaseOutcome.set(stopped);
                return new TaskTransactionOutcome(
                        transactionName,
                        TaskTransactionResult.READY_TO_CONTINUE,
                        TaskYieldPolicy.MUST_YIELD,
                        result,
                        true);
            }
            throw e;
        } finally {
            long elapsedMs = Math.max(0L, System.currentTimeMillis() - startedAt);
            log.info("[five-ring-v2 latency] {}OutsideTurnEnd round={} phase={} source={} result={} completed={} elapsedMs={} accepted={} ageMs={}",
                    latencyName, state.round(), state.phase(), state.source(), result, completed, elapsedMs,
                    state.taskAccepted(), pathingAgeMs(state));
        }
    }

    private FiveRingStepOutcome runPhase(TaskExecutionContext context, FiveRingPhaseContext state) {
        return switch (state.phase()) {
            case PREPARE -> prepare(context, state);
            case BUY_SHOES -> buyShoes(context, state);
            case HANDOVER_DETECT -> detectHandover(context, state);
            case ACCEPT_TASK -> acceptTask(context, state);
            case WAIT_PATHING -> waitPathing(context, state);
            case HANDLE_DIALOG -> handleDialog(context, state);
            case SYNC_TASK_PANEL -> syncTaskPanel(context, state, true);
            case FINISHED, FAILED, STOPPED ->
                    FiveRingStepOutcome.failed(state, "terminal phase should not be executed: " + state.phase());
        };
    }

    private FiveRingStepOutcome prepare(TaskExecutionContext context, FiveRingPhaseContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        log.info("[five-ring-v2 prepare-1] clean startup chrome before supply checks");
        gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        boolean startupUiCleanPrechecked = windowTaskContextHolder.rawCurrent()
                .map(runtime -> runtime.consumeFreshTaskQueueStartupUiCleanupClean(
                        TASK_CODE, STARTUP_UI_CLEAN_PRECHECK_MAX_AGE_MS))
                .orElse(false);
        if (startupUiCleanPrechecked) {
            log.info("[five-ring-v2 prepare-1] skip foreground UI cleanup: fresh background precheck clean");
        } else {
            uiCleanerService.cleanUpAll();
        }
        startAcceptSetupPositionPrewarm(startupUiCleanPrechecked
                ? "prepare-after-startup-ui-clean-precheck"
                : "prepare-after-cleanup");
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        log.info("[five-ring-v2 prepare-2] startup first-aid check before bag supply scan");
        playerStateService.performStartupFirstAidCheckFromPrecheckOrRun(
                context, STARTUP_FIRST_AID_PRECHECK_MAX_AGE_MS);
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        int requiredShoeCount = requiredShoeCountForRun(state);
        log.info("[five-ring-v2 prepare-3] check sheyaoxiang and shoe in one bag session: round={} requiredShoes={}",
                state.round(), requiredShoeCount);
        FiveRingSupplyCheck supplyCheck = checkFiveRingSuppliesInOneBagSession(context, requiredShoeCount);
        Integer shoeBagIndex = supplyCheck == null ? null : supplyCheck.shoeBagIndex();
        int shoeCount = supplyCheck == null ? 0 : supplyCheck.shoeCount();
        int missingShoeCount = Math.max(0, requiredShoeCount - shoeCount);
        if (missingShoeCount == 0) {
            log.info("[five-ring-v2 prepare done] shoes found: count={} required={} firstPage={}",
                    shoeCount, requiredShoeCount, shoeBagIndex == null ? "none" : shoeBagIndex + 1);
        } else {
            log.warn("[five-ring-v2 prepare] shoe count insufficient: count={} required={} missing={}; try quick-buy shoe from fast item panel",
                    shoeCount, requiredShoeCount, missingShoeCount);
            boolean boughtShoes = quickBuyShoe(context, missingShoeCount);
            if (!boughtShoes) {
                log.warn("[five-ring-v2 prepare] quick-buy shoe failed; fall back to shop-owner buy flow");
                return FiveRingStepOutcome.continueTo(state.withShoePurchaseCount(missingShoeCount,
                                "prepare-shoe-shop-required")
                                .next(FiveRingPhase.BUY_SHOES, "prepare-shoe-shop-required"),
                        "shoe count insufficient; buy through shop-owner flow");
            }
            int postBuyVerifyRequiredShoeCount = 1;
            FiveRingSupplyCheck verifiedCheck = checkFiveRingSuppliesInOneBagSession(context,
                    postBuyVerifyRequiredShoeCount);
            shoeBagIndex = verifiedCheck == null ? null : verifiedCheck.shoeBagIndex();
            int verifiedShoeCount = verifiedCheck == null ? 0 : verifiedCheck.shoeCount();
            log.info("[five-ring-v2 prepare] quick-buy shoe result={} verifiedCount={} required={} firstPage={}",
                    boughtShoes, verifiedShoeCount, postBuyVerifyRequiredShoeCount,
                    shoeBagIndex == null ? "none" : shoeBagIndex + 1);
            int verifiedMissingShoeCount = Math.max(0, postBuyVerifyRequiredShoeCount - verifiedShoeCount);
            if (verifiedMissingShoeCount > 0) {
                log.warn("[five-ring-v2 prepare] quick-buy completed but verified shoes are still insufficient: verified={} required={} missing={}; fall back to shop-owner buy flow",
                        verifiedShoeCount, postBuyVerifyRequiredShoeCount, verifiedMissingShoeCount);
                return FiveRingStepOutcome.continueTo(state.withShoePurchaseCount(verifiedMissingShoeCount,
                                "prepare-shoe-shop-after-quick-buy-insufficient")
                                .next(FiveRingPhase.BUY_SHOES, "prepare-shoe-shop-after-quick-buy-insufficient"),
                        "quick-buy verified shoe count insufficient; buy through shop-owner flow");
            }
        }

        return FiveRingStepOutcome.continueTo(
                state.withShoeBagIndex(shoeBagIndex, "prepare-done")
                        .nextAfterPreparation("prepare-done"),
                "prepare finished");
    }

    private FiveRingStepOutcome buyShoes(TaskExecutionContext context, FiveRingPhaseContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, 0L,
                "wuhuan-v2:shoe-shop-phase-already-inside")) {
            log.info("[five-ring-v2 shoe-shop] already inside {}, buy from shop owner", SHOE_SHOP_MAP_NAME);
            int purchaseCount = shoePurchaseCountForState(state);
            if (!buyShoeFromShopOwnerWithRetry(context, purchaseCount)) {
                return FiveRingStepOutcome.sharedState(
                        state.retrySamePhase("shoe-shop-buy-retry"),
                        "shoe-shop owner buy failed; retry later");
            }
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
            BufferedImage beforeReturn = captureShoeShopReturnSnapshot("wuhuan-v2:shoe-shop-return-before");
            try {
                if (!returnToChanganFromShoeShopWithRetry(context, beforeReturn)) {
                    return FiveRingStepOutcome.sharedState(
                            state.retrySamePhase("shoe-shop-return-retry"),
                            "shoe-shop return failed; retry later");
                }
            } finally {
                if (beforeReturn != null) {
                    beforeReturn.flush();
                }
            }
            Integer shoeBagIndex = bagService.findItemPageIndex(BagService.MAIN_BAG, KEY_ITEM_NAME, context);
            log.info("[five-ring-v2 shoe-shop] buy phase done: verifiedPage={}",
                    shoeBagIndex == null ? "none" : shoeBagIndex + 1);
            return FiveRingStepOutcome.continueTo(
                    state.withShoeBagIndex(shoeBagIndex, "shoe-shop-bought")
                            .nextAfterPreparation("shoe-shop-bought"),
                    "shoe-shop buy finished");
        }

        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot snapshot = runtime == null ? null : runtime.getPathingSnapshot();
        if (isUsablePathingSnapshot(state, snapshot, runtime)) {
            WindowPathingState observed = snapshot.getState();
            long snapshotAgeMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
            if (observed == WindowPathingState.ACTIVE
                    || observed == WindowPathingState.UNKNOWN
                    || snapshot.isProbeInProgress()) {
                log.info("[five-ring-v2 shoe-shop] entry watcher still pathing: state={} current={}({}, {}) ageMs={} probeInProgress={}",
                        observed, snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                        snapshotAgeMs, snapshot.isProbeInProgress());
                return FiveRingStepOutcome.pathingStarted(
                        state.withWatcherPathingStarted("shoe-shop-entry-watcher-still-pathing", SHOE_SHOP_ENTRY_NAV_SOURCE),
                        "shoe-shop entry watcher still pathing");
            }
            if (observed == WindowPathingState.ARRIVED || observed == WindowPathingState.STOPPED_AWAY) {
                if (runtime != null) {
                    PreparedDialogAction preparedRoute = observed == WindowPathingState.STOPPED_AWAY
                            ? runtime.freshPreparedRouteActionForPathingTerminal(
                            snapshot, PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS)
                            : null;
                    if (preparedRoute != null) {
                        WindowPathingIntent activeIntent = runtime.getActivePathingIntent().orElse(null);
                        long verifiedAgeMs = Math.max(0L, System.currentTimeMillis() - preparedRoute.getLastVerifiedAtMs());
                        log.info("[five-ring-v2 shoe-shop] pathing terminal clear delayed because prepared route dialog is ready: state={} target={} actionIntentId={} activeIntentId={} verifiedAgeMs={}",
                                observed, preparedRoute.getTargetKeyword(), preparedRoute.getIntentId(),
                                activeIntent == null ? null : activeIntent.getIntentId(), verifiedAgeMs);
                    } else {
                        runtime.clearPathingSignal("five-ring shoe-shop entry consumed watcher terminal state");
                    }
                }
                log.info("[five-ring-v2 shoe-shop] entry watcher terminal; continue entry confirmation/retry: state={} current={}({}, {}) ageMs={}",
                        observed, snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(), snapshotAgeMs);
                if (observed == WindowPathingState.ARRIVED
                        && isShoeShopDoorArrivalSnapshot(snapshot)) {
                    if (handleShoeShopDoorAfterArrival(context, state, snapshot)) {
                        return FiveRingStepOutcome.continueTo(
                                state.next(FiveRingPhase.BUY_SHOES, "shoe-shop-entered-after-door-handling"),
                                "shoe-shop entered after door handling");
                    }
                    FiveRingStepOutcome alreadyInside = continueIfAlreadyInsideShoeShop(
                            state,
                            "wuhuan-v2:shoe-shop-door-after-handler-false",
                            "shoe-shop-entered-after-door-handler-false",
                            "shoe-shop entered after delayed door handling");
                    if (alreadyInside != null) {
                        return alreadyInside;
                    }
                }
            }
        } else if (pathingAgeMs(state) < PATHING_OBSERVER_FAST_WAIT_MS) {
            log.info("[five-ring-v2 shoe-shop] wait for entry watcher before retry: ageMs={} fastWaitMs={}",
                    pathingAgeMs(state), PATHING_OBSERVER_FAST_WAIT_MS);
            return FiveRingStepOutcome.pathingStarted(
                    state.withWatcherPathingStarted("shoe-shop-entry-watcher-wait", SHOE_SHOP_ENTRY_NAV_SOURCE),
                    "shoe-shop entry waiting for watcher");
        }

        FiveRingStepOutcome alreadyInside = continueIfAlreadyInsideShoeShop(
                state,
                "wuhuan-v2:shoe-shop-before-entry-retry",
                "shoe-shop-already-inside-before-entry-retry",
                "shoe-shop already inside before entry retry");
        if (alreadyInside != null) {
            return alreadyInside;
        }

        NavigationResult result = clickShoeShopEntryExact(context);
        NavigationResultStatus status = result.getStatus();
        log.info("[five-ring-v2 shoe-shop] entry exact navigation result: status={} message={} retry={}",
                status, result.getMessage(), state.phaseRetryCount());
        if (status == NavigationResultStatus.STOPPED) {
            return FiveRingStepOutcome.stopped(state, "shoe-shop entry navigation stopped");
        }
        if (status == NavigationResultStatus.PATHING_STARTED) {
            return FiveRingStepOutcome.pathingStarted(
                    state.retrySamePhase("shoe-shop-entry-clicked")
                            .withNewWatcherPathingStarted("shoe-shop-entry-clicked", SHOE_SHOP_ENTRY_NAV_SOURCE),
                    "shoe-shop entry pathing started");
        }
        if (status == NavigationResultStatus.ARRIVED || status == NavigationResultStatus.SUCCESS) {
            WindowRuntimeContext currentRuntime = windowTaskContextHolder.rawCurrent().orElse(null);
            if (currentRuntime == null) {
                log.warn("[five-ring-v2 shoe-shop] entry navigation returned {} but no window runtime is bound; retry later",
                        status);
                return FiveRingStepOutcome.sharedState(
                        state.retrySamePhase("shoe-shop-entry-no-runtime"),
                        "shoe-shop entry arrived but watcher runtime is unavailable");
            }
            /*
             * Do not finish the shoe-shop entry inside the foreground turn. When the character is
             * already at 长安(130,130), NavigationService can report ARRIVED from cached coordinates
             * without creating a watcher signal. Register the same target explicitly so the next pass
             * consumes a fresh WindowPathingSnapshot and runs the unified door/dismount handling.
             */
            WindowPathingIntent intent = WindowPathingIntent.builder()
                    .source(SHOE_SHOP_ENTRY_NAV_SOURCE)
                    .type(WindowPathingIntentType.TARGETED)
                    .targetMapName(TARGET_MAP_NAME)
                    .targetX(SHOE_SHOP_ENTRY_X)
                    .targetY(SHOE_SHOP_ENTRY_Y)
                    .tolerance(0)
                    .build();
            currentRuntime.markPathingStarted(intent);
            log.info("[five-ring-v2 shoe-shop] entry navigation returned {}; registered watcher intent instead of completing in foreground: windowId={} target={}({}, {})",
                    status, currentRuntime.getWindowId(), TARGET_MAP_NAME, SHOE_SHOP_ENTRY_X, SHOE_SHOP_ENTRY_Y);
            return FiveRingStepOutcome.pathingStarted(
                    state.retrySamePhase("shoe-shop-entry-arrived-await-watcher")
                            .withNewWatcherPathingStarted("shoe-shop-entry-arrived-await-watcher", SHOE_SHOP_ENTRY_NAV_SOURCE),
                    "shoe-shop entry arrived; waiting for watcher door handling");
        }
        if (status == NavigationResultStatus.DIALOG_OPENED) {
            return FiveRingStepOutcome.sharedState(
                    state.retrySamePhase("shoe-shop-entry-dialog-opened"),
                    "shoe-shop entry saw dialog; retry through watcher/prepared flow");
        }
        alreadyInside = continueIfAlreadyInsideShoeShop(
                state,
                "wuhuan-v2:shoe-shop-phase-after-entry-failure",
                "shoe-shop-entry-confirmed-after-failure",
                "shoe-shop entry confirmed after input failure");
        if (alreadyInside != null) {
            return alreadyInside;
        }
        return FiveRingStepOutcome.sharedState(
                state.retrySamePhase("shoe-shop-entry-retry"),
                "shoe-shop entry click failed; retry later");
    }

    private FiveRingStepOutcome continueIfAlreadyInsideShoeShop(FiveRingPhaseContext state,
                                                                String source,
                                                                String stateReason,
                                                                String message) {
        if (!gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, 0L, source)) {
            return null;
        }
        PlayerCharacter me = gameContext.getMe();
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        log.info("[five-ring-v2 shoe-shop] skip shoe-shop-entry exact navigation because current map is {}: windowId={} currentMap={} current=({}, {}) oldTargetMap={} oldTarget=({}, {}) source={}",
                SHOE_SHOP_MAP_NAME,
                runtime == null ? null : runtime.getWindowId(),
                me == null ? null : me.getCurrentMapName(),
                me == null ? null : me.getX(),
                me == null ? null : me.getY(),
                TARGET_MAP_NAME,
                SHOE_SHOP_ENTRY_X,
                SHOE_SHOP_ENTRY_Y,
                source);
        return FiveRingStepOutcome.continueTo(
                state.next(FiveRingPhase.BUY_SHOES, stateReason),
                message);
    }

    private boolean handleShoeShopDoorAfterArrival(TaskExecutionContext context,
                                                   FiveRingPhaseContext state,
                                                   WindowPathingSnapshot snapshot) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        log.info("[five-ring-v2 shoe-shop] arrived at entry door, wait for auto-enter before dismount: retry={} current={}({}, {})",
                state.phaseRetryCount(), snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY());
        if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, SHOE_SHOP_ENTRY_DOOR_CONFIRM_MS,
                "wuhuan-v2:shoe-shop-door-auto-enter")) {
            log.info("[five-ring-v2 shoe-shop] entered shop after door arrival without dismount");
            return true;
        }

        /*
         * If 130,130 was reached but the shop map did not load, the common cause is still being
         * mounted/flying. Alt+C is the game's mount toggle, so try it once before opening the
         * status panel; only use the panel probe if this direct recovery does not enter the shop.
         */
        boolean firstDismountSubmitted = inputSequences.pressAltC("wuhuan-v2:shoe-shop-door-first-dismount");
        log.info("[five-ring-v2 shoe-shop] door auto-enter missed, first dismount submitted={}",
                firstDismountSubmitted);
        TaskSleep.sleepOrStop(context, SHOE_SHOP_DISMOUNT_SETTLE_MS, "Five-ring V2 task interrupted");
        if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, SHOE_SHOP_ENTRY_POST_DISMOUNT_CONFIRM_MS,
                "wuhuan-v2:shoe-shop-door-after-first-dismount")) {
            log.info("[five-ring-v2 shoe-shop] entered shop after first dismount");
            return true;
        }

        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        FlyingState flyingState = gameStateUtil.detectFlyingState("wuhuan-v2:shoe-shop-door");
        log.info("[five-ring-v2 shoe-shop] flying state after failed first dismount: state={}", flyingState);
        if (flyingState == FlyingState.FLYING) {
            boolean secondDismountSubmitted = inputSequences.pressAltC("wuhuan-v2:shoe-shop-door-confirmed-flying-dismount");
            log.info("[five-ring-v2 shoe-shop] confirmed flying, second dismount submitted={}",
                    secondDismountSubmitted);
            TaskSleep.sleepOrStop(context, SHOE_SHOP_DISMOUNT_SETTLE_MS, "Five-ring V2 task interrupted");
            if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, SHOE_SHOP_ENTRY_POST_DISMOUNT_CONFIRM_MS,
                    "wuhuan-v2:shoe-shop-door-after-confirmed-flying-dismount")) {
                log.info("[five-ring-v2 shoe-shop] entered shop after confirmed-flying dismount");
                return true;
            }
        } else if (flyingState == FlyingState.NOT_FLYING) {
            log.info("[five-ring-v2 shoe-shop] status panel says not flying; retry exact 130,130 without extra dismount");
        } else {
            log.warn("[five-ring-v2 shoe-shop] flying status unknown; retry exact 130,130 without extra dismount");
        }
        return false;
    }

    private boolean isShoeShopDoorArrivalSnapshot(WindowPathingSnapshot snapshot) {
        if (snapshot == null || snapshot.getIntent() == null) {
            return false;
        }
        WindowPathingIntent intent = snapshot.getIntent();
        if (!TARGET_MAP_NAME.equals(snapshot.getCurrentMapName())) {
            return false;
        }
        boolean exactDoorIntent = Integer.valueOf(SHOE_SHOP_ENTRY_X).equals(intent.getTargetX())
                && Integer.valueOf(SHOE_SHOP_ENTRY_Y).equals(intent.getTargetY());
        if (!exactDoorIntent) {
            log.info("[five-ring-v2 shoe-shop] arrived in target map but not door intent; skip dismount probe: current={}({}, {}) source={} target=({}, {})",
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    intent.getSource(), intent.getTargetX(), intent.getTargetY());
            return false;
        }
        if (snapshot.getCurrentX() == null || snapshot.getCurrentY() == null) {
            return true;
        }
        boolean exactDoor = snapshot.getCurrentX() == SHOE_SHOP_ENTRY_X
                && snapshot.getCurrentY() == SHOE_SHOP_ENTRY_Y;
        if (!exactDoor) {
            log.info("[five-ring-v2 shoe-shop] exact door intent arrived away from 130,130; skip dismount probe: current={}({}, {})",
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY());
        }
        return exactDoor;
    }

    private int requiredShoeCountForRun(FiveRingPhaseContext state) {
        int configuredRuns = botProperties.getWuhuanMaxRuns();
        if (isUnlimitedRuns(configuredRuns)) {
            return 1;
        }
        int currentRound = state == null ? 1 : Math.max(1, state.round());
        return normalizeShoePurchaseCount(configuredRuns - currentRound + 1);
    }

    private int shoePurchaseCountForState(FiveRingPhaseContext state) {
        int requested = state == null ? 0 : state.shoePurchaseCount();
        return requested > 0 ? normalizeShoePurchaseCount(requested) : requiredShoeCountForRun(state);
    }

    private int normalizeShoePurchaseCount(int count) {
        return count >= 2 ? 2 : 1;
    }

    /**
     * Run the 五环 startup inventory checks with one main-bag open/close cycle.
     *
     * <p>摄妖香 and shoe detection both operate on the main bag. Keeping them in the same exclusive
     * input section avoids the old visual churn where Alt+E opened/closed the bag for incense and
     * immediately opened/closed it again for the shoe pre-scan.</p>
     *
     * @param context optional stop token for the current window task.
     * @param requiredShoeCount maximum number of shoes the scan needs to prove are available.
     * @return combined startup supply check result, or null if the bag could not be opened.
     */
    private FiveRingSupplyCheck checkFiveRingSuppliesInOneBagSession(TaskExecutionContext context,
                                                                     int requiredShoeCount) {
        return bagService.withMainBagOpen("wuhuan-v2:prepare-supplies", context, mainBag -> {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
            boolean incenseRefilled = playerStateService.ensureSheYaoXiangActiveInOpenMainBag(mainBag, context);
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

            log.info("[five-ring-v2 prepare] count shoe template in already-open main bag: template={} required={}",
                    KEY_ITEM_NAME, requiredShoeCount);
            BagService.ItemCountResult shoeCount = mainBag.countItemUpTo(KEY_ITEM_NAME, requiredShoeCount);
            return new FiveRingSupplyCheck(incenseRefilled, shoeCount.firstPageIndex(), shoeCount.count());
        });
    }

    /**
     * Buy the missing 五环 shoes through the user-prepared shortcut item panel.
     *
     * <p>This path is only entered after the normal one-bag startup scan cannot find shoes. All
     * coordinates are window-relative values measured from the user's current 1024x768 client base.
     * The method does not navigate: it opens the shortcut shop from the bottom-right fast item slot,
     * chooses the shoe once per missing item, clicks purchase, and closes the shop so the caller can
     * rescan the bag.</p>
     *
     * @param context current task stop token; nullable only for legacy direct task execution.
     * @param purchaseCount number of shoes to select before clicking buy; clamped to the supported
     *                      五环 UI range of 1..2.
     * @return true when the shortcut panel, shoe template, and buy click all completed; false when the
     *         shortcut is not available or a template is missing.
     */
    private boolean quickBuyShoe(TaskExecutionContext context, int purchaseCount) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        int[] shortcutRect = windowRelativeRect(QUICK_SHOE_ANCHOR_REL_LEFT, QUICK_SHOE_ANCHOR_REL_TOP,
                QUICK_SHOE_ANCHOR_REL_RIGHT, QUICK_SHOE_ANCHOR_REL_BOTTOM);
        Point shortcutCenter = findQuickShoeShortcut(shortcutRect);
        if (shortcutCenter == null) {
            Point revealPoint = rectCenter(shortcutRect);
            log.info("[five-ring-v2 quick-buy-shoe] shortcut anchor missing; click center to reveal panel: point=({}, {})",
                    revealPoint.x, revealPoint.y);
            inputSequences.submitAndWait("wuhuan-v2:quick-buy-shoe:reveal-fast-item", List.of(
                    InputAction.moveMouse(revealPoint.x, revealPoint.y),
                    InputAction.sleep(100),
                    InputAction.clickLeft(revealPoint.x, revealPoint.y, 120),
                    InputAction.sleep(1500)
            ));
            shortcutCenter = findQuickShoeShortcut(shortcutRect);
        }
        if (shortcutCenter == null) {
            log.warn("[five-ring-v2 quick-buy-shoe] shortcut anchor/template still missing; skip quick buy");
            return false;
        }

        int[] fastItemRect = windowRelativeRect(QUICK_SHOE_FAST_ITEM_REL_LEFT, QUICK_SHOE_FAST_ITEM_REL_TOP,
                QUICK_SHOE_FAST_ITEM_REL_RIGHT, QUICK_SHOE_FAST_ITEM_REL_BOTTOM);
        if (!rightClickAnyTemplateCenter(context,
                List.of(QUICK_SHOE_FAST_ITEM_TEMPLATE, QUICK_SHOE_FAST_ITEM_ALT_TEMPLATE), fastItemRect, 0.80,
                "wuhuan-v2:quick-buy-shoe:open-shop")) {
            return false;
        }
        TaskSleep.sleepOrStop(context, 500, "Five-ring V2 task interrupted");

        int[] itemRect = windowRelativeRect(QUICK_SHOE_SHOP_ITEM_REL_LEFT, QUICK_SHOE_SHOP_ITEM_REL_TOP,
                QUICK_SHOE_SHOP_ITEM_REL_RIGHT, QUICK_SHOE_SHOP_ITEM_REL_BOTTOM);
        int safePurchaseCount = normalizeShoePurchaseCount(purchaseCount);
        if (!clickTemplateCenterInRect(context, SHOE_SHOP_SHOE_TEMPLATE, itemRect, 0.82,
                "wuhuan-v2:quick-buy-shoe:select-shoe", safePurchaseCount)) {
            return false;
        }
        TaskSleep.sleepOrStop(context, 250, "Five-ring V2 task interrupted");

        Point buyPoint = windowRelativePoint(QUICK_SHOE_BUY_BUTTON_REL_X, QUICK_SHOE_BUY_BUTTON_REL_Y);
        log.info("[five-ring-v2 quick-buy-shoe] click buy button: point=({}, {})", buyPoint.x, buyPoint.y);
        boolean clickedBuy = inputSequences.submitAndWait("wuhuan-v2:quick-buy-shoe:buy", List.of(
                InputAction.moveMouse(buyPoint.x, buyPoint.y),
                InputAction.sleep(120),
                InputAction.clickLeft(buyPoint.x, buyPoint.y, 150),
                InputAction.sleep(500)
        ));
        if (!clickedBuy) {
            return false;
        }

        boolean closed = uiCleanerService.closeAllGenericWindows();
        log.info("[five-ring-v2 quick-buy-shoe] close shop panel result={}", closed);
        return true;
    }

    private boolean buyShoeFromShopOwnerWithRetry(TaskExecutionContext context, int purchaseCount) {
        int safePurchaseCount = normalizeShoePurchaseCount(purchaseCount);
        for (int attempt = 1; attempt <= SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
            log.info("[five-ring-v2 shoe-shop] shop-owner buy flow attempt {}/{}",
                    attempt, SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS);

            // 进牛记布店后默认落在老板附近；店内没有小地图 transform，直接用 NPC 点击链路。
            uiCleanerService.closeAllGenericWindows();
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

            if (!npcClickService.clickNpcSmart(shoeShopOwnerNpc().toClickRequest(gameContext.getMe(), TaskType.WUHuan_V2))) {
                log.warn("[five-ring-v2 shoe-shop] failed to click shop owner NPC: attempt={}/{} npc={} map={} target=({}, {})",
                        attempt, SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS,
                        SHOE_SHOP_OWNER_NAME, SHOE_SHOP_MAP_NAME, SHOE_SHOP_OWNER_X, SHOE_SHOP_OWNER_Y);
                continue;
            }

            DialogResult buyOptionResult = dialogService.handleDialog(
                    DialogHandleRequest.handleWuhuanShoeShopBuyOption("wuhuan-v2:shoe-shop-buy-option"));
            boolean buyOptionClicked = buyOptionResult.getStatus() == DialogResultStatus.GREEN_TEMPLATE_CLICKED
                    || buyOptionResult.getStatus() == DialogResultStatus.OPTION_KEYWORD_CLICKED;
            log.info("[five-ring-v2 shoe-shop] buy option handleDialog result: attempt={}/{} status={} actionKey={} text={} clicked={}",
                    attempt, SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS, buyOptionResult.getStatus(),
                    buyOptionResult.getActionKey(), buyOptionResult.getMatchedText(), buyOptionResult.isClicked());
            if (!buyOptionClicked) {
                log.warn("[five-ring-v2 shoe-shop] buy option not clicked: attempt={}/{}",
                        attempt, SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS);
                uiCleanerService.closeAllGenericWindows();
                continue;
            }
            TaskSleep.sleepOrStop(context, 600, "Five-ring V2 task interrupted");

            int[] itemRect = windowRelativeRect(SHOE_SHOP_ITEM_REL_LEFT, SHOE_SHOP_ITEM_REL_TOP,
                    SHOE_SHOP_ITEM_REL_RIGHT, SHOE_SHOP_ITEM_REL_BOTTOM);
            if (!clickTemplateCenterInRect(context, SHOE_SHOP_SHOE_TEMPLATE, itemRect, 0.82,
                    "wuhuan-v2:shoe-shop-click-shoe", safePurchaseCount)) {
                log.warn("[five-ring-v2 shoe-shop] shoe template not matched, close panel and reopen NPC: attempt={}/{}",
                        attempt, SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS);
                uiCleanerService.closeAllGenericWindows();
                continue;
            }
            TaskSleep.sleepOrStop(context, 300, "Five-ring V2 task interrupted");

            if (!clickShoeShopBuyButton(context)) {
                log.warn("[five-ring-v2 shoe-shop] buy button click failed: attempt={}/{}",
                        attempt, SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS);
                uiCleanerService.closeAllGenericWindows();
                continue;
            }
            TaskSleep.sleepOrStop(context, 600, "Five-ring V2 task interrupted");

            boolean closed = uiCleanerService.closeAllGenericWindows();
            log.info("[five-ring-v2 shoe-shop] close shop panel result={}", closed);
            return true;
        }
        log.warn("[five-ring-v2 shoe-shop] shop-owner buy flow failed after {} attempts",
                SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS);
        return false;
    }

    private NavigationResult clickShoeShopEntryExact(TaskExecutionContext context) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(TARGET_MAP_NAME)
                .targetX(SHOE_SHOP_ENTRY_X)
                .targetY(SHOE_SHOP_ENTRY_Y)
                .targetName(SHOE_SHOP_ENTRY_TARGET_NAME)
                .randomizeMiniMapClickPoint(false)
                .arrivalTolerance(0)
                .source(SHOE_SHOP_ENTRY_NAV_SOURCE)
                .build());
        if (!result.success() && result.getStatus() != NavigationResultStatus.PATHING_STARTED) {
            log.warn("[five-ring-v2 shoe-shop] exact entry click request failed: status={} message={}",
                    result.getStatus(), result.getMessage());
        }
        return result;
    }

    private boolean clickShoeShopBuyButton(TaskExecutionContext context) {
        int[] fullWindowRect = windowRelativeRect(0, 0, GAME_CLIENT_WIDTH, GAME_CLIENT_HEIGHT);
        if (clickTemplateCenterInRect(context, SHOE_SHOP_BUY_BUTTON_TEMPLATE, fullWindowRect, 0.85,
                "wuhuan-v2:shoe-shop-click-buy")) {
            return true;
        }

        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        Point fallbackPoint = windowRelativePoint(
                SHOE_SHOP_BUY_BUTTON_FALLBACK_REL_X,
                SHOE_SHOP_BUY_BUTTON_FALLBACK_REL_Y);
        log.warn("[five-ring-v2 shoe-shop] buy button template missed; fallback relative click: relative=({}, {}) click=({}, {})",
                SHOE_SHOP_BUY_BUTTON_FALLBACK_REL_X, SHOE_SHOP_BUY_BUTTON_FALLBACK_REL_Y,
                fallbackPoint.x, fallbackPoint.y);
        return inputSequences.submitAndWait("wuhuan-v2:shoe-shop-click-buy-fallback", List.of(
                InputAction.moveMouse(fallbackPoint.x, fallbackPoint.y),
                InputAction.sleep(120),
                InputAction.clickLeft(fallbackPoint.x, fallbackPoint.y, 150),
            InputAction.sleep(350)
        ));
    }

    private boolean returnToChanganFromShoeShopWithRetry(TaskExecutionContext context, BufferedImage beforeReturn) {
        for (int attempt = 1; attempt <= SHOE_SHOP_RETURN_MAX_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
            rightClickWindowRelative(context, SHOE_SHOP_RETURN_REL_X, SHOE_SHOP_RETURN_REL_Y,
                    "wuhuan-v2:shoe-shop-return-changan-attempt-" + attempt);
            if (waitShoeShopReturnVisualChange(context, beforeReturn,
                    "wuhuan-v2:shoe-shop-return-fast-attempt-" + attempt)) {
                log.info("[five-ring-v2 shoe-shop] returned from shop by fast visual change: attempt={}/{}",
                        attempt, SHOE_SHOP_RETURN_MAX_ATTEMPTS);
                return true;
            }
            if (gameStateUtil.confirmCurrentMapFresh(TARGET_MAP_NAME, SHOE_SHOP_ENTRY_CONFIRM_TIMEOUT_MS,
                    "wuhuan-v2:shoe-shop-return-changan-attempt-" + attempt)) {
                log.info("[five-ring-v2 shoe-shop] returned to {}: attempt={}/{}",
                        TARGET_MAP_NAME, attempt, SHOE_SHOP_RETURN_MAX_ATTEMPTS);
                return true;
            }
            log.warn("[five-ring-v2 shoe-shop] return to {} not confirmed, retry: attempt={}/{}",
                    TARGET_MAP_NAME, attempt, SHOE_SHOP_RETURN_MAX_ATTEMPTS);
        }
        log.warn("[five-ring-v2 shoe-shop] failed to return to {} after {} attempts",
                TARGET_MAP_NAME, SHOE_SHOP_RETURN_MAX_ATTEMPTS);
        NavigationResult fallbackResult = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(SHOE_SHOP_RETURN_FALLBACK_MAP_NAME)
                .targetX(SHOE_SHOP_RETURN_FALLBACK_NPC_X)
                .targetY(SHOE_SHOP_RETURN_FALLBACK_NPC_Y)
                .targetName(SHOE_SHOP_RETURN_FALLBACK_NPC_NAME)
                .source(SHOE_SHOP_RETURN_FALLBACK_NAV_SOURCE)
                .build());
        boolean fallbackArrived = fallbackResult.success();
        log.info("[five-ring-v2 shoe-shop] fallback navigate to repair NPC {} {}({}, {}) result: success={} status={} message={}",
                SHOE_SHOP_RETURN_FALLBACK_MAP_NAME, SHOE_SHOP_RETURN_FALLBACK_NPC_NAME,
                SHOE_SHOP_RETURN_FALLBACK_NPC_X, SHOE_SHOP_RETURN_FALLBACK_NPC_Y, fallbackArrived,
                fallbackResult.getStatus(), fallbackResult.getMessage());
        return fallbackArrived;
    }

    private BufferedImage captureShoeShopReturnSnapshot(String reason) {
        int[] rect = coordinateHelper.getScaledRect(
                SHOE_SHOP_RETURN_VERIFY_REL_LEFT,
                SHOE_SHOP_RETURN_VERIFY_REL_TOP,
                SHOE_SHOP_RETURN_VERIFY_REL_WIDTH,
                SHOE_SHOP_RETURN_VERIFY_REL_HEIGHT);
        BufferedImage image = tracker.captureToMemory(reason, rect[0], rect[1], rect[2], rect[3]);
        if (image == null) {
            log.warn("[five-ring-v2 shoe-shop] fast return snapshot failed: reason={} rect=({}, {})-({}, {})",
                    reason, rect[0], rect[1], rect[2], rect[3]);
            return null;
        }
        log.info("[five-ring-v2 shoe-shop] fast return snapshot captured: reason={} size={}x{} rect=({}, {})-({}, {})",
                reason, image.getWidth(), image.getHeight(), rect[0], rect[1], rect[2], rect[3]);
        return image;
    }

    private boolean waitShoeShopReturnVisualChange(TaskExecutionContext context,
                                                   BufferedImage beforeReturn,
                                                   String reason) {
        if (beforeReturn == null) {
            log.warn("[five-ring-v2 shoe-shop] fast return verify skipped: reason={} baseline=null", reason);
            return false;
        }
        long deadline = System.currentTimeMillis() + SHOE_SHOP_RETURN_FAST_VERIFY_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
            TaskSleep.sleepOrStop(context, SHOE_SHOP_RETURN_FAST_VERIFY_POLL_MS, "Five-ring V2 task interrupted");
            BufferedImage current = captureShoeShopReturnSnapshot(reason);
            if (current == null) {
                continue;
            }
            try {
                boolean same = ImageFinder.isMatch(beforeReturn, current, SHOE_SHOP_RETURN_FAST_VERIFY_SAME_TOLERANCE);
                log.info("[five-ring-v2 shoe-shop] fast return visual compare: reason={} changed={} tolerance={}",
                        reason, !same, SHOE_SHOP_RETURN_FAST_VERIFY_SAME_TOLERANCE);
                if (!same) {
                    return true;
                }
            } finally {
                current.flush();
            }
        }
        log.warn("[five-ring-v2 shoe-shop] fast return verify timed out: reason={} timeoutMs={}",
                reason, SHOE_SHOP_RETURN_FAST_VERIFY_TIMEOUT_MS);
        return false;
    }

    private boolean clickTemplateCenterInRect(TaskExecutionContext context,
                                              String templatePath,
                                              int[] rect,
                                              double matchRate,
                                              String description) {
        return clickTemplateCenterInRect(context, templatePath, rect, matchRate, description, 1);
    }

    private boolean clickTemplateCenterInRect(TaskExecutionContext context,
                                              String templatePath,
                                              int[] rect,
                                              double matchRate,
                                              String description,
                                              int clickCount) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        File template = new File(templatePath);
        if (!template.exists()) {
            log.warn("[five-ring-v2 shoe-shop] template missing: description={} template={}",
                    description, templatePath);
            return false;
        }

        Point matchedCenter = coordinateHelper.findImageInRegion(templatePath, rect, matchRate);
        if (matchedCenter == null) {
            log.warn("[five-ring-v2 shoe-shop] template not matched: description={} template={} rect=({}, {})-({}, {}) rate={}",
                    description, templatePath, rect[0], rect[1], rect[2], rect[3], matchRate);
            return false;
        }

        Point clickPoint = coordinateHelper.getRandomizedPoint(matchedCenter, 3, 2);
        int safeClickCount = Math.max(1, clickCount);
        log.info("[five-ring-v2 shoe-shop] template matched: description={} template={} center=({}, {}) click=({}, {}) clickCount={}",
                description, templatePath, matchedCenter.x, matchedCenter.y, clickPoint.x, clickPoint.y,
                safeClickCount);
        List<InputAction> actions = new ArrayList<>();
        actions.add(InputAction.moveMouse(clickPoint.x, clickPoint.y));
        actions.add(InputAction.sleep(120));
        for (int i = 0; i < safeClickCount; i++) {
            actions.add(InputAction.clickLeft(clickPoint.x, clickPoint.y, 150));
            actions.add(InputAction.sleep(i + 1 == safeClickCount ? 250 : 120));
        }
        return inputSequences.submitAndWait(description, actions);
    }

    private Point findQuickShoeShortcut(int[] shortcutRect) {
        Point matchedCenter = coordinateHelper.findImageInRegion(QUICK_SHOE_ANCHOR_TEMPLATE, shortcutRect, 0.80);
        if (matchedCenter == null) {
            log.info("[five-ring-v2 quick-buy-shoe] quick shoe anchor not visible: rect=({}, {})-({}, {})",
                    shortcutRect[0], shortcutRect[1], shortcutRect[2], shortcutRect[3]);
        } else {
            log.info("[five-ring-v2 quick-buy-shoe] quick shoe anchor matched: center=({}, {})",
                    matchedCenter.x, matchedCenter.y);
        }
        return matchedCenter;
    }

    private boolean rightClickAnyTemplateCenter(TaskExecutionContext context,
                                                List<String> templatePaths,
                                                int[] rect,
                                                double matchRate,
                                                String description) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        for (String templatePath : templatePaths) {
            Point matchedCenter = coordinateHelper.findImageInRegion(templatePath, rect, matchRate);
            if (matchedCenter == null) {
                continue;
            }

            Point clickPoint = coordinateHelper.getRandomizedPoint(matchedCenter, 2, 2);
            log.info("[five-ring-v2 quick-buy-shoe] right click template: description={} template={} center=({}, {}) click=({}, {})",
                    description, templatePath, matchedCenter.x, matchedCenter.y, clickPoint.x, clickPoint.y);
            return inputSequences.submitAndWait(description, List.of(
                    InputAction.moveMouse(clickPoint.x, clickPoint.y),
                    InputAction.sleep(120),
                    InputAction.clickRight(clickPoint.x, clickPoint.y, 150),
                    InputAction.sleep(350)
            ));
        }
        log.warn("[five-ring-v2 quick-buy-shoe] templates not matched for right click: description={} templates={} rect=({}, {})-({}, {}) rate={}",
                description, templatePaths, rect[0], rect[1], rect[2], rect[3], matchRate);
        return false;
    }

    private int[] windowRelativeRect(int left, int top, int right, int bottom) {
        tracker.refreshWindowState();
        int baseX = tracker.getWindowBaseX();
        int baseY = tracker.getWindowBaseY();
        return new int[]{baseX + left, baseY + top, baseX + right, baseY + bottom};
    }

    private Point windowRelativePoint(int relativeX, int relativeY) {
        tracker.refreshWindowState();
        return new Point(tracker.getWindowBaseX() + relativeX, tracker.getWindowBaseY() + relativeY);
    }

    private void rightClickWindowRelative(TaskExecutionContext context, int relativeX, int relativeY, String description) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        Point clickPoint = coordinateHelper.getRandomizedPoint(windowRelativePoint(relativeX, relativeY), 5, 4);
        log.info("[five-ring-v2 shoe-shop] right click return point: description={} relative=({}, {}) click=({}, {})",
                description, relativeX, relativeY, clickPoint.x, clickPoint.y);
        inputSequences.submitAndWait(description, List.of(
                InputAction.moveMouse(clickPoint.x, clickPoint.y),
                InputAction.sleep(120),
                InputAction.clickRight(clickPoint.x, clickPoint.y, 150),
                InputAction.sleep(700)
        ));
    }

    private Point rectCenter(int[] rect) {
        return new Point((rect[0] + rect[2]) / 2, (rect[1] + rect[3]) / 2);
    }

    private FiveRingStepOutcome detectHandover(TaskExecutionContext context, FiveRingPhaseContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        log.info("[five-ring-v2 handover] check whether an existing task can be taken over");

        TrackerPathingAttempt trackerAttempt = tryClickWuhuanTrackerLink(context, state, "handover", true);
        if (trackerAttempt.status == TrackerPathingStatus.PATHING_STARTED) {
            log.info("[five-ring-v2 handover] existing task found in tracker and pathing started");
            return FiveRingStepOutcome.pathingStarted(
                    trackerAttempt.state.withTaskAccepted("handover-tracker-task-found"),
                    "handover tracker pathing started");
        }
        if (trackerAttempt.status == TrackerPathingStatus.RUNNER_PREPARED_NOT_READY) {
            log.info("[five-ring-v2 handover] runner prepared tracker action not ready; use tracker title gate before initial setup");
        }
        WuhuanTrackerTitleGate titleGate = readWuhuanTrackerTitleGate(
                trackerAttempt.state, "handover:" + trackerAttempt.status);
        if (titleGate.titleVisible()) {
            log.info("[five-ring-v2 handover] 五环 title is visible during handover; sync tracker instead of accepting again: status={}",
                    trackerAttempt.status);
            return FiveRingStepOutcome.continueTo(
                    titleGate.state().next(FiveRingPhase.SYNC_TASK_PANEL, "handover-wuhuan-title-visible"),
                    "handover tracker title visible; sync tracker");
        }
        if (trackerAttempt.status == TrackerPathingStatus.TRACKER_UNAVAILABLE) {
            log.warn("[five-ring-v2 handover] left tracker is not readable before task acceptance; fall back to accept task instead of failing startup");
            return FiveRingStepOutcome.continueTo(
                    trackerAttempt.state.next(FiveRingPhase.ACCEPT_TASK, "handover-tracker-unavailable-setup-required"),
                    "tracker unavailable before acceptance; initial setup required");
        }
        log.info("[five-ring-v2 handover] no 五环 task found on left tracker; initial setup is required");
        return FiveRingStepOutcome.continueTo(
                state.next(FiveRingPhase.ACCEPT_TASK, "handover-setup-required"),
                "initial setup required");
    }

    private FiveRingStepOutcome acceptTask(TaskExecutionContext context, FiveRingPhaseContext state) {
        FiveRingStepOutcome pendingAcceptNavigation = continueIfAcceptNpcNavigationStillPathing(context, state);
        if (pendingAcceptNavigation != null) {
            return pendingAcceptNavigation;
        }
        FiveRingPhaseContext activeState = state.clearAcceptNpcPathingWait("accept-navigation-ready");
        FiveRingStepOutcome preparedTrackerAtEntry = consumeCurrentPreparedTrackerDuringAccept(
                context, activeState, "accept-entry-before-navigation");
        if (preparedTrackerAtEntry != null) {
            return preparedTrackerAtEntry;
        }
        if (activeState.taskAccepted()) {
            log.warn("[five-ring-v2 accept] task is already accepted in this round; skip accept NPC and resync task panel: source={}",
                    activeState.source());
            return FiveRingStepOutcome.continueTo(
                    activeState.next(FiveRingPhase.SYNC_TASK_PANEL, "accept-skipped-task-already-accepted"),
                    "task already accepted; do not accept again");
        }
        int retry = 0;
        while (retry < MAX_ACCEPT_RETRY) {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

            LocationInfo acceptNavigationPosition = acceptSetupPositionFromPrewarmOrSync("accept-loop");
            boolean nearAcceptNpc = isNearAcceptNpc(acceptNavigationPosition);
            FiveRingStepOutcome currentScreenAccept = tryAcceptInitialTaskFromCurrentScreen(
                    context, activeState, "setup:current-screen", nearAcceptNpc);
            if (currentScreenAccept != null) {
                return currentScreenAccept;
            }

            DialogType setupDialogType = dialogService.handleDialog(
                    DialogHandleRequest.inspect("wuhuan-v2:setup-dialog-check")).getDialogType();
            if (setupDialogType == DialogType.NONE || !nearAcceptNpc) {
                if (setupDialogType != DialogType.NONE) {
                    /*
                     * During multi-hop travel to 云游大师 an inn/transfer option can already be open.
                     * Only NavigationService knows how to choose that route option; accepting or
                     * cleaning it here turns a valid route dialog into a failed Five-ring accept.
                     */
                    log.info("[five-ring-v2 setup] existing dialog while not near accept NPC; delegate to navigation: type={}",
                            setupDialogType);
                }
                if (tryClickNearbyAcceptNpc(context, nearAcceptNpc)) {
                    AcceptDialogPathingResult acceptResult = acceptInitialDialogAndTriggerPathing(
                            context, "nearbyAcceptNpcPathing");
                    if (acceptResult == AcceptDialogPathingResult.TASK_ACCEPTED_NEEDS_SYNC) {
                        return FiveRingStepOutcome.continueTo(
                                activeState.next(FiveRingPhase.SYNC_TASK_PANEL, "nearby-accept-pathing-unconfirmed"),
                                "nearby accept clicked; read tracker next");
                    }
                    if (acceptResult == AcceptDialogPathingResult.TASK_ALREADY_FINISHED) {
                        return FiveRingStepOutcome.finishedTerminal(
                                activeState,
                                "five-ring accept reported finished/daily limit");
                    }
                    cleanupUnexpectedAcceptDialog("setup:nearby-accept-template-not-matched");
                    retry++;
                    TaskSleep.sleepOrStop(context, 1000, "Five-ring V2 task interrupted");
                    continue;
                }
                FiveRingStepOutcome preparedTrackerBeforeNavigation = consumeCurrentPreparedTrackerDuringAccept(
                        context, activeState, "accept-before-npc-navigation");
                if (preparedTrackerBeforeNavigation != null) {
                    return preparedTrackerBeforeNavigation;
                }
                NavigationResult navigationResult = navigationService.navigateToNPC(NavigationRequest.builder()
                        .targetMapName(TARGET_MAP_NAME)
                        .targetX(NPC_COOR_X)
                        .targetY(NPC_COOR_Y)
                        .targetName(TARGET_NPC_NAME)
                        .source(ACCEPT_NPC_NAV_SOURCE)
                        .freshCurrentMapName(acceptNavigationPosition == null ? null : acceptNavigationPosition.mapName)
                        .freshCurrentX(acceptNavigationPosition == null ? null : acceptNavigationPosition.x)
                        .freshCurrentY(acceptNavigationPosition == null ? null : acceptNavigationPosition.y)
                        .freshCurrentLocationAtMs(acceptNavigationPosition == null ? 0L : System.currentTimeMillis())
                        .build());
                NavigationResultStatus navigationStatus = navigationResult.getStatus();
                log.info("[five-ring-v2 accept] accept NPC navigation result: npc={} status={} message={}",
                        TARGET_NPC_NAME, navigationStatus, navigationResult.getMessage());
                if (navigationStatus == NavigationResultStatus.PATHING_STARTED) {
                    return FiveRingStepOutcome.pathingStarted(
                            activeState.waitForAcceptNpcPathing("accept-npc-navigation-pathing", ACCEPT_NPC_NAV_SOURCE),
                            "accept NPC navigation pathing started");
                }
                if (navigationStatus == NavigationResultStatus.STOPPED) {
                    return FiveRingStepOutcome.stopped(activeState, "accept NPC navigation stopped");
                }
                if (navigationStatus == NavigationResultStatus.DIALOG_PREPARING) {
                    log.info("[five-ring-v2 setup] route dialog preparing in background; release turn before retry: npc={} message={}",
                            TARGET_NPC_NAME, navigationResult.getMessage());
                    return FiveRingStepOutcome.sharedState(
                            activeState.retrySamePhase("accept-npc-navigation-dialog-preparing"),
                            "route dialog preparing; retry later");
                }
                if (navigationStatus == NavigationResultStatus.POINT_NOT_REACHED) {
                    /*
                     * A yield-mode current-map mini-map click can fail to confirm movement. Do not
                     * stay inside the local accept retry loop while holding the task turn; release
                     * and let the next phase attempt retry from the latest window state.
                     */
                    log.warn("[five-ring-v2 setup] accept NPC navigation did not start pathing; release turn before retry: npc={} message={}",
                            TARGET_NPC_NAME, navigationResult.getMessage());
                    return FiveRingStepOutcome.sharedState(
                            activeState.retrySamePhase("accept-npc-navigation-no-pathing-retry"),
                            "accept NPC navigation did not start pathing; retry later");
                }
                if (!navigationResult.success()) {
                    TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
                    FiveRingStepOutcome navigateFailedAccept = tryAcceptInitialTaskFromCurrentScreen(
                            context, activeState, "setup:navigate-failed");
                    if (navigateFailedAccept != null) {
                        return navigateFailedAccept;
                    }
                    log.warn("[five-ring-v2 setup] failed to navigate near {} status={} (retry {}/{})",
                            TARGET_NPC_NAME, navigationStatus, retry + 1, MAX_ACCEPT_RETRY);
                    retry++;
                    TaskSleep.sleepOrStop(context, 2000, "Five-ring V2 task interrupted");
                    continue;
                }
                TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

                cleanupUiBeforeAcceptNpcClick("setup:before-accept-npc-click");
                TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

                if (!clickInitialNpcForAccept(context)) {
                    TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
                    FiveRingStepOutcome npcClickFailedAccept = tryAcceptInitialTaskFromCurrentScreen(
                            context, activeState, "setup:npc-click-failed");
                    if (npcClickFailedAccept != null) {
                        return npcClickFailedAccept;
                    }
                    cleanupUiBeforeAcceptNpcClick("setup:npc-click-failed-no-current-accept-dialog");
                    log.warn("[five-ring-v2 setup] failed to click {} (retry {}/{})",
                            TARGET_NPC_NAME, retry + 1, MAX_ACCEPT_RETRY);
                    retry++;
                    TaskSleep.sleepOrStop(context, 2000, "Five-ring V2 task interrupted");
                    continue;
                }
                TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
            } else {
                log.info("[five-ring-v2 setup] existing dialog detected, skip navigation/NPC recognition and try accept option");
            }

            AcceptDialogPathingResult acceptResult = acceptInitialDialogAndTriggerPathing(context, "initialAcceptPathing");
            if (acceptResult == AcceptDialogPathingResult.TASK_ACCEPTED_NEEDS_SYNC) {
                log.info("[five-ring-v2] initial task accepted; switch to left tracker");
                return FiveRingStepOutcome.continueTo(
                        activeState.next(FiveRingPhase.SYNC_TASK_PANEL, "initial-accept-pathing-unconfirmed"),
                        "initial task accepted; read tracker instead of accepting again");
            }
            if (acceptResult == AcceptDialogPathingResult.TASK_ALREADY_FINISHED) {
                return FiveRingStepOutcome.finishedTerminal(
                        activeState,
                        "five-ring accept reported finished/daily limit");
            }
            if (acceptResult == AcceptDialogPathingResult.NOT_ACCEPTED) {
                cleanupUnexpectedAcceptDialog("setup:accept-template-not-matched");
                retry++;
                TaskSleep.sleepOrStop(context, 1000, "Five-ring V2 task interrupted");
                continue;
            }
        }

        return FiveRingStepOutcome.failed(activeState, "initial task setup failed after retries");
    }

    private FiveRingStepOutcome consumeCurrentPreparedTrackerDuringAccept(TaskExecutionContext context,
                                                                          FiveRingPhaseContext state,
                                                                          String checkpoint) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return null;
        }
        FiveRingStepOutcome preparedTracker = consumeCurrentPreparedBeforeNormalPhase(
                context, state, runtime, null, checkpoint);
        if (preparedTracker != null) {
            log.info("[five-ring-v2 accept] prepared 五环 tracker consumed before accept NPC navigation: checkpoint={} source={}",
                    checkpoint, state.source());
        }
        return preparedTracker;
    }

    private FiveRingStepOutcome continueIfAcceptNpcNavigationStillPathing(TaskExecutionContext context,
                                                                          FiveRingPhaseContext state) {
        if (!state.waitingAcceptNpcPathing()) {
            return null;
        }

        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot snapshot = runtime == null ? null : runtime.getPathingSnapshot();
        long pathingAgeMs = pathingAgeMs(state);
        if (isUsablePathingSnapshot(state, snapshot, runtime)) {
            WindowPathingState observed = snapshot.getState();
            long ageMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
            if (observed == WindowPathingState.ARRIVED
                    || isSnapshotNearAcceptNpc(snapshot)) {
                if (runtime != null) {
                    runtime.clearPathingSignal("five-ring accept NPC navigation consumed watcher arrival");
                }
                log.info("[five-ring-v2 accept] accept NPC navigation wait ended by watcher: state={} current={}({}, {}) ageMs={}",
                        observed, snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(), ageMs);
                return null;
            }
            if (observed == WindowPathingState.STOPPED_AWAY) {
                if (runtime != null) {
                    PreparedDialogAction preparedRoute = runtime.freshPreparedRouteActionForPathingTerminal(
                            snapshot, PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS);
                    if (preparedRoute != null) {
                        WindowPathingIntent activeIntent = runtime.getActivePathingIntent().orElse(null);
                        long verifiedAgeMs = Math.max(0L, System.currentTimeMillis() - preparedRoute.getLastVerifiedAtMs());
                        log.info("[five-ring-v2 accept] pathing terminal clear delayed because prepared route dialog is ready: state={} target={} actionIntentId={} activeIntentId={} verifiedAgeMs={}",
                                observed, preparedRoute.getTargetKeyword(), preparedRoute.getIntentId(),
                                activeIntent == null ? null : activeIntent.getIntentId(), verifiedAgeMs);
                    } else {
                        runtime.clearPathingSignal("five-ring accept NPC navigation consumed stopped-away");
                    }
                }
                log.info("[five-ring-v2 accept] accept NPC navigation stopped away; retry navigation: current={}({}, {}) ageMs={}",
                        snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(), ageMs);
                return null;
            }
            if (pathingAgeMs >= PATHING_TARGET_WAIT_TIMEOUT_MS) {
                log.warn("[five-ring-v2 accept] accept NPC navigation hard timeout before watcher keep-wait; retry navigation/click from current state: source={} ageMs={} timeoutMs={} observed={} snapshotAgeMs={} probeInProgress={}",
                        state.pathingIntentSource(), pathingAgeMs, PATHING_TARGET_WAIT_TIMEOUT_MS,
                        observed, ageMs, snapshot.isProbeInProgress());
                return null;
            }
            if (observed == WindowPathingState.ACTIVE
                    || observed == WindowPathingState.UNKNOWN
                    || snapshot.isProbeInProgress()) {
                log.info("[five-ring-v2 accept] accept NPC navigation watcher still pathing: state={} current={}({}, {}) ageMs={} probeInProgress={}",
                        observed, snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                        ageMs, snapshot.isProbeInProgress());
                return FiveRingStepOutcome.pathingStarted(state, "accept NPC navigation watcher still pathing");
            }
        }

        if (pathingAgeMs < PATHING_OBSERVER_FAST_WAIT_MS) {
            log.info("[five-ring-v2 accept] wait for accept NPC watcher before retry: ageMs={} fastWaitMs={}",
                    pathingAgeMs, PATHING_OBSERVER_FAST_WAIT_MS);
            return FiveRingStepOutcome.pathingStarted(state, "accept NPC navigation waiting for watcher");
        }

        if (pathingAgeMs >= PATHING_TARGET_WAIT_TIMEOUT_MS) {
            log.warn("[five-ring-v2 accept] accept NPC navigation hard timeout; retry navigation/click from current state: source={} ageMs={} timeoutMs={}",
                    state.pathingIntentSource(), pathingAgeMs, PATHING_TARGET_WAIT_TIMEOUT_MS);
            return null;
        }

        if (hasActiveAcceptNpcPathingIntent(state, runtime)) {
            log.info("[five-ring-v2 accept] accept NPC navigation active intent still in flight: source={} ageMs={} timeoutMs={}",
                    state.pathingIntentSource(), pathingAgeMs, PATHING_TARGET_WAIT_TIMEOUT_MS);
            return FiveRingStepOutcome.pathingStarted(state, "accept NPC navigation active intent still in flight");
        }

        log.info("[five-ring-v2 accept] accept NPC navigation has no terminal snapshot yet; keep waiting: source={} ageMs={} timeoutMs={}",
                state.pathingIntentSource(), pathingAgeMs, PATHING_TARGET_WAIT_TIMEOUT_MS);
        return FiveRingStepOutcome.pathingStarted(state, "accept NPC navigation waiting for terminal snapshot");
    }

    private boolean hasActiveAcceptNpcPathingIntent(FiveRingPhaseContext state, WindowRuntimeContext runtime) {
        if (state == null || runtime == null || !state.pathingIntentExpected()) {
            return false;
        }
        WindowPathingIntent activeIntent = runtime.getActivePathingIntent().orElse(null);
        return activeIntent != null
                && isExpectedPathingSource(state.pathingIntentSource(), activeIntent.getSource())
                && isExpectedPathingTarget(state.pathingIntentSource(), activeIntent);
    }

    private boolean tryClickNearbyAcceptNpc(TaskExecutionContext context, boolean nearAcceptNpc) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        NpcTarget acceptNpc = fiveRingAcceptNpc();
        if (!nearAcceptNpc) {
            return false;
        }
        PlayerCharacter me = gameContext.getMe();

        cleanupUiBeforeAcceptNpcClick("nearby-accept-npc-click");
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        log.info("[five-ring-v2] accept NPC nearby; try direct smart click before minimap navigation: npc={} playerMap={} player=({}, {}) targetMap={} target=({}, {}) tolerance={}",
                acceptNpc.getName(),
                me.getCurrentMapName(), me.getX(), me.getY(),
                acceptNpc.getMapName(), acceptNpc.getX(), acceptNpc.getY(),
                TASK_NPC_DIRECT_CLICK_DISTANCE);
        if (clickInitialNpcForAccept(context)) {
            return true;
        }
        log.info("[five-ring-v2] nearby accept NPC direct click failed; fallback to minimap navigation: npc={}",
                acceptNpc.getName());
        return false;
    }

    private void cleanupUiBeforeAcceptNpcClick(String source) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot snapshot = runtime == null ? null : runtime.getPathingSnapshot();
        if (snapshot != null && snapshot.isUiCleanupRecommended()) {
            long ageMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUiCleanupRecommendedAtMs());
            log.info("[five-ring-v2 accept] runner requested generic UI cleanup before accept NPC click: source={} reason={} ageMs={}",
                    source, snapshot.getUiCleanupReason(), ageMs);
            uiCleanerService.closeAllGenericWindows();
            runtime.clearPathingUiCleanupRecommendation("five-ring accepted runner UI cleanup request: " + source);
            return;
        }
        uiCleanerService.closeAllGenericWindows();
    }

    private void startAcceptSetupPositionPrewarm(String source) {
        CompletableFuture<AcceptSetupPositionPrewarm> existing = acceptSetupPositionPrewarm;
        if (existing != null && !existing.isDone()) {
            log.info("[five-ring-v2 accept] setup position prewarm already running: source={}", source);
            return;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            log.info("[five-ring-v2 accept] setup position prewarm skipped because window context is unavailable: source={}",
                    source);
            return;
        }
        acceptSetupPositionPrewarm = CompletableFuture.supplyAsync(() -> {
            try {
                LocationInfo location = windowTaskContextHolder.callWith(runtime, this::syncAcceptNpcSetupPosition);
                return new AcceptSetupPositionPrewarm(location, System.currentTimeMillis(), source);
            } catch (RuntimeException e) {
                throw new CompletionException(e);
            }
        });
        acceptSetupPositionPrewarm.whenComplete((result, error) -> {
            if (error != null) {
                log.warn("[five-ring-v2 accept] setup position prewarm failed: source={} reason={}",
                        source, error.getMessage());
                return;
            }
            LocationInfo location = result == null ? null : result.location();
            log.info("[five-ring-v2 accept] setup position prewarm finished: source={} current={}({}, {})",
                    source,
                    location == null ? null : location.mapName,
                    location == null ? null : location.x,
                    location == null ? null : location.y);
        });
    }

    private LocationInfo acceptSetupPositionFromPrewarmOrSync(String source) {
        CompletableFuture<AcceptSetupPositionPrewarm> prewarm = acceptSetupPositionPrewarm;
        if (prewarm != null && prewarm.isDone() && !prewarm.isCompletedExceptionally() && !prewarm.isCancelled()) {
            AcceptSetupPositionPrewarm result = prewarm.getNow(null);
            acceptSetupPositionPrewarm = null;
            if (result != null && result.location() != null) {
                long ageMs = Math.max(0L, System.currentTimeMillis() - result.capturedAtMs());
                if (ageMs <= ACCEPT_SETUP_POSITION_PREWARM_MAX_AGE_MS) {
                    LocationInfo location = result.location();
                    log.info("[five-ring-v2 accept] use prewarmed setup position: source={} prewarmSource={} ageMs={} current={}({}, {})",
                            source, result.source(), ageMs, location.mapName, location.x, location.y);
                    return location;
                }
                log.info("[five-ring-v2 accept] discard stale setup position prewarm: source={} prewarmSource={} ageMs={} maxAgeMs={}",
                        source, result.source(), ageMs, ACCEPT_SETUP_POSITION_PREWARM_MAX_AGE_MS);
            }
        } else if (prewarm != null && !prewarm.isDone()) {
            log.info("[five-ring-v2 accept] setup position prewarm not finished; fall back to synchronous read: source={}",
                    source);
        }
        return syncAcceptNpcSetupPosition();
    }

    private LocationInfo syncAcceptNpcSetupPosition() {
        NpcTarget acceptNpc = fiveRingAcceptNpc();
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot snapshot = runtime == null ? null : runtime.getPathingSnapshot();
        if (snapshot != null
                && snapshot.getCurrentMapName() != null
                && snapshot.getCurrentX() != null
                && snapshot.getCurrentY() != null
                && System.currentTimeMillis() - snapshot.getUpdatedAtMs() <= OBSERVER_SNAPSHOT_MAX_AGE_MS) {
            boolean nearByRunner = gameStateUtil.isNearCoordinate(snapshot.getCurrentMapName(),
                    snapshot.getCurrentX(), snapshot.getCurrentY(),
                    acceptNpc.getMapName(), acceptNpc.getX(), acceptNpc.getY(), TASK_NPC_DIRECT_CLICK_DISTANCE);
            log.info("[five-ring-v2 accept] near accept NPC checked by runner snapshot: current={}({}, {}) target={}({}, {}) near={}",
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    acceptNpc.getMapName(), acceptNpc.getX(), acceptNpc.getY(), nearByRunner);
            return new LocationInfo(snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY());
        }
        /*
         * This single fresh scan is used for both the near-NPC direct-click decision and the following
         * map-navigation stale-cache guard. Do not let accept setup pay for two back-to-back minimap
         * reads when the first one already proved the current map.
         */
        return playerStateService.syncMyPosition();
    }

    private boolean isNearAcceptNpc(LocationInfo current) {
        NpcTarget acceptNpc = fiveRingAcceptNpc();
        if (current == null) {
            return false;
        }
        return gameStateUtil.isNearCoordinate(current.mapName, current.x, current.y,
                acceptNpc.getMapName(), acceptNpc.getX(), acceptNpc.getY(), TASK_NPC_DIRECT_CLICK_DISTANCE);
    }

    private FiveRingStepOutcome waitPathing(TaskExecutionContext context, FiveRingPhaseContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        /*
         * Combat state is owned by the per-window observer. Do not run the old task-level combat
         * scan here: it can race the window watcher and open task panels while the window is already
         * fighting. 五环 only consumes the remembered window state and waits until the watcher marks
         * this window free again.
         */
        if (isWindowCombatActive()) {
            if (!state.combatObservedSincePathing()) {
                windowTaskContextHolder.rawCurrent().ifPresent(runtime ->
                        clearConsumedWuhuanTrackerPathingIntent(runtime, state, "combat-entry"));
            }
            FiveRingPhaseContext combatState = state.withCombatObservedSincePathing(
                    "pathing-window-combat-active");
            if (combatState.wuhuanTrackerCombatBaselineImage() == null) {
                combatState = captureWuhuanTrackerCombatBaseline(
                        combatState, "wuhuan-v2:combat-entry-tracker-roi-baseline");
                return FiveRingStepOutcome.sharedState(
                        combatState,
                        "window combat state active during pathing wait; tracker ROI baseline captured or unavailable");
            }
            FiveRingStepOutcome trackerRoiExit = tryResolvePostCombatFromWuhuanTrackerRoiCandidate(
                    context, combatState);
            if (trackerRoiExit != null) {
                return trackerRoiExit;
            }
            return FiveRingStepOutcome.sharedState(
                    combatState,
                    "window combat state active during pathing wait");
        }
        if (state.combatObservedSincePathing()) {
            AutoCombatService.TickResult recovery = autoCombatService.handleCombatTick(context, "wuhuan-v2", true);
            if (recovery == AutoCombatService.TickResult.IN_COMBAT) {
                return FiveRingStepOutcome.sharedState(
                        state.withCombatObservedSincePathing("pathing-window-combat-still-active-during-recovery"),
                        "window combat still active during post-combat recovery");
            }
            if (recovery == AutoCombatService.TickResult.NONE) {
                /*
                 * The window watcher owns the live combat verdict, but only the owning task may
                 * consume combat-exit recovery. If the exit signal was already absent, continue the
                 * 五环 flow and leave a clear breadcrumb instead of looping forever.
                 */
                log.warn("[five-ring-v2] combat was observed during pathing but post-combat recovery had no exit signal; continue tracker sync");
            }
            WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
            if (runtime != null) {
                /*
                 * Combat consumed the tracker click handoff. If the old tracker pathing intent stays
                 * active, Runner remains in active-pathing and will never prepare the next tracker
                 * link, so clear only 五环 tracker-click intents before syncing the panel again.
                 */
                clearConsumedWuhuanTrackerPathingIntent(runtime, state, "combat-recovered");
            }
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "pathing-window-combat-recovered"),
                    "window combat ended; post-combat recovery done; sync task panel");
        }

        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot snapshot = runtime == null ? null : runtime.getPathingSnapshot();
        long pathingAgeMs = pathingAgeMs(state);
        if (isUsablePathingSnapshot(state, snapshot, runtime)) {
            WindowPathingState observed = snapshot.getState();
            long snapshotAgeMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
            log.info("[five-ring-v2 pathing] watcher snapshot: source={} state={} current={}({}, {}) message={} ageMs={} probeInProgress={}",
                    state.source(), observed, snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    snapshot.getMessage(), snapshotAgeMs, snapshot.isProbeInProgress());
            if (observed == WindowPathingState.ARRIVED) {
                runtime.clearPathingSignal("five-ring consumed watcher arrival");
                return FiveRingStepOutcome.continueTo(
                        state.next(FiveRingPhase.HANDLE_DIALOG, "pathing-arrived-by-watcher"),
                        "pathing arrived by watcher");
            }
            if (observed == WindowPathingState.STOPPED_AWAY) {
                PreparedDialogAction preparedRoute = runtime.freshPreparedRouteActionForPathingTerminal(
                        snapshot, PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS);
                if (preparedRoute != null) {
                    WindowPathingIntent activeIntent = runtime.getActivePathingIntent().orElse(null);
                    long verifiedAgeMs = Math.max(0L, System.currentTimeMillis() - preparedRoute.getLastVerifiedAtMs());
                    log.info("[five-ring-v2 pathing] pathing terminal clear delayed because prepared route dialog is ready: state={} target={} actionIntentId={} activeIntentId={} verifiedAgeMs={}",
                            observed, preparedRoute.getTargetKeyword(), preparedRoute.getIntentId(),
                            activeIntent == null ? null : activeIntent.getIntentId(), verifiedAgeMs);
                } else if (isStoppedAwayWuhuanTrackerPathing(snapshot)
                        && !isGiveItemMap(snapshot.getCurrentMapName())) {
                    boolean cleared = clearStoppedAwayTrackerIntent(runtime,
                            "stopped-away outside give-item map; tracker sync first");
                    log.info("[five-ring-v2 pathing] stopped-away tracker intent outside give-item map; skip handle dialog and sync tracker: current={}({}, {}) giveMap={} cleared={}",
                            snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                            GIVE_ITEM_MAP_NAME, cleared);
                    return FiveRingStepOutcome.continueTo(
                            state.next(FiveRingPhase.SYNC_TASK_PANEL, "pathing-stopped-away-outside-give-map"),
                            "pathing stopped away outside give-item map; sync task panel");
                } else {
                    runtime.clearPathingSignal("five-ring consumed watcher stopped-away");
                }
                return FiveRingStepOutcome.continueTo(
                        state.next(FiveRingPhase.HANDLE_DIALOG, "pathing-stopped-away-by-watcher"),
                        "pathing stopped away by watcher");
            }
            if (pathingAgeMs >= PATHING_TARGET_WAIT_TIMEOUT_MS) {
                log.warn("[five-ring-v2 pathing] watcher did not produce terminal state before timeout; sync task panel: source={} ageMs={} timeoutMs={} observed={} snapshotAgeMs={} probeInProgress={}",
                        state.source(), pathingAgeMs, PATHING_TARGET_WAIT_TIMEOUT_MS,
                        observed, snapshotAgeMs, snapshot.isProbeInProgress());
                return FiveRingStepOutcome.continueTo(
                        state.increaseUiErrorCount("pathing-timeout")
                                .next(FiveRingPhase.SYNC_TASK_PANEL, "pathing-timeout"),
                        "pathing watcher timeout; sync task panel");
            }
            if (observed == WindowPathingState.ACTIVE
                    || observed == WindowPathingState.UNKNOWN
                    || snapshot.isProbeInProgress()
                    || snapshotAgeMs <= OBSERVER_SNAPSHOT_MAX_AGE_MS) {
                return FiveRingStepOutcome.sharedState(
                    state.withPathingMovementObserved("pathing-watcher-active"),
                    "pathing watcher still active: " + observed);
            }
        } else if (state.pathingIntentExpected()) {
            log.info("[five-ring-v2 pathing] watcher snapshot unavailable or stale for expected intent: source={} expectedIntent={} ageMs={}",
                    state.source(), state.pathingIntentSource(), pathingAgeMs(state));
        }

        if (pathingAgeMs < PATHING_RECHECK_GRACE_MS) {
            log.info("[five-ring-v2 pathing] grace active before watcher retry: source={} ageMs={} graceMs={}",
                    state.source(), pathingAgeMs, PATHING_RECHECK_GRACE_MS);
            return FiveRingStepOutcome.sharedState(
                    state.retrySamePhase("pathing-grace-wait"),
                    "pathing grace active; wait for watcher");
        }

        if (pathingAgeMs < PATHING_OBSERVER_FAST_WAIT_MS) {
            log.info("[five-ring-v2 pathing] wait for background watcher without foreground movement probe: source={} ageMs={} fastWaitMs={}",
                    state.source(), pathingAgeMs, PATHING_OBSERVER_FAST_WAIT_MS);
            return FiveRingStepOutcome.sharedState(
                    state.retrySamePhase("pathing-watcher-fast-wait"),
                    "wait for background watcher");
        }

        if (!state.pathingIntentExpected()) {
            log.info("[five-ring-v2 pathing] tracker pathing has no watcher intent after fast wait; resync task panel instead of waiting: source={} ageMs={} fastWaitMs={}",
                    state.source(), pathingAgeMs, PATHING_OBSERVER_FAST_WAIT_MS);
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "tracker-pathing-no-intent-resync"),
                    "tracker pathing has no watcher intent; sync task panel");
        }

        if (pathingAgeMs >= PATHING_TARGET_WAIT_TIMEOUT_MS) {
            log.warn("[five-ring-v2 pathing] watcher did not produce terminal state before timeout; sync task panel: source={} ageMs={} timeoutMs={}",
                    state.source(), pathingAgeMs, PATHING_TARGET_WAIT_TIMEOUT_MS);
            return FiveRingStepOutcome.continueTo(
                    state.increaseUiErrorCount("pathing-timeout")
                            .next(FiveRingPhase.SYNC_TASK_PANEL, "pathing-timeout"),
                    "pathing watcher timeout; sync task panel");
        }

        log.info("[five-ring-v2 pathing] no terminal watcher state yet; keep yielding: source={} ageMs={}",
                state.source(), pathingAgeMs);
        return FiveRingStepOutcome.sharedState(
                state.retrySamePhase("pathing-keep-yielding"),
                "pathing still waiting for watcher");
    }

    private void clearConsumedWuhuanTrackerPathingIntent(WindowRuntimeContext runtime,
                                                         FiveRingPhaseContext state,
                                                         String reason) {
        WindowPathingSnapshot snapshot = runtime.getPathingSnapshot();
        WindowPathingIntent intent = snapshot == null ? null : snapshot.getIntent();
        String activeSource = intent == null ? null : intent.getSource();
        WindowPathingState activeState = snapshot == null ? null : snapshot.getState();
        boolean preparedCleared = runtime.clearPathingSignalIfSourcePrefix(
                PREPARED_TRACKER_PANEL_PATHING_SOURCE_PREFIX,
                "five-ring " + reason + " consumed prepared tracker pathing");
        boolean greenCleared = runtime.clearPathingSignalIfSourcePrefix(
                TRACKER_GREEN_CLICK_PATHING_SOURCE_PREFIX,
                "five-ring " + reason + " consumed tracker green pathing");
        log.info("[five-ring-v2 combat-exit] clears consumed 五环 tracker pathing intent: reason={} windowId={} phase={} source={} activePathingState={} activeSource={} preparedCleared={} greenCleared={} allowedPrefixes=[{}, {}]",
                reason, runtime.getWindowId(), state.phase(), state.source(), activeState, activeSource,
                preparedCleared, greenCleared,
                PREPARED_TRACKER_PANEL_PATHING_SOURCE_PREFIX, TRACKER_GREEN_CLICK_PATHING_SOURCE_PREFIX);
    }

    private FiveRingPhaseContext captureWuhuanTrackerCombatBaseline(FiveRingPhaseContext state,
                                                                    String reason) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        OcrWindowRegion roi = validWuhuanTrackerBlockRegion(state, reason);
        if (roi == null) {
            log.info("[five-ring-v2 combat-exit] tracker ROI baseline skipped: windowId={} reason={} roi=missing-or-invalid source={} phase={}",
                    runtime == null ? null : runtime.getWindowId(), reason, state.source(), state.phase());
            return state;
        }
        int[] captureRect = wuhuanTrackerRoiAbsoluteCaptureRect(runtime, roi, reason);
        if (captureRect == null) {
            return state;
        }
        BufferedImage image = tracker.captureToMemory(
                reason, captureRect[0], captureRect[1], captureRect[2], captureRect[3]);
        if (image == null) {
            log.warn("[five-ring-v2 combat-exit] tracker ROI baseline capture failed: windowId={} reason={} windowRelativeRoi={} absoluteRect=({}, {})-({}, {}) source={} phase={}",
                    runtime == null ? null : runtime.getWindowId(), reason, roi.toShortText(),
                    captureRect[0], captureRect[1], captureRect[2], captureRect[3], state.source(), state.phase());
            return state;
        }
        long capturedAtMs = System.currentTimeMillis();
        log.info("[five-ring-v2 combat-exit] tracker ROI baseline captured: windowId={} reason={} windowRelativeRoi={} absoluteRect=({}, {})-({}, {}) size={}x{} source={} phase={}",
                runtime == null ? null : runtime.getWindowId(), reason, roi.toShortText(),
                captureRect[0], captureRect[1], captureRect[2], captureRect[3],
                image.getWidth(), image.getHeight(), state.source(), state.phase());
        return state.withWuhuanTrackerCombatBaseline(image, capturedAtMs, reason);
    }

    private FiveRingStepOutcome tryResolvePostCombatFromWuhuanTrackerRoiCandidate(TaskExecutionContext context,
                                                                                  FiveRingPhaseContext state) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        OcrWindowRegion roi = validWuhuanTrackerBlockRegion(state, "post-combat-roi-compare");
        BufferedImage baseline = state.wuhuanTrackerCombatBaselineImage();
        if (runtime == null || roi == null || baseline == null) {
            return null;
        }

        int[] captureRect = wuhuanTrackerRoiAbsoluteCaptureRect(
                runtime, roi, "wuhuan-v2:combat-current-tracker-roi");
        if (captureRect == null) {
            return null;
        }
        BufferedImage current = tracker.captureToMemory(
                "wuhuan-v2:combat-current-tracker-roi",
                captureRect[0], captureRect[1], captureRect[2], captureRect[3]);
        if (current == null) {
            log.warn("[five-ring-v2 combat-exit] tracker ROI current capture failed: windowId={} windowRelativeRoi={} absoluteRect=({}, {})-({}, {}) baselineAgeMs={} source={}",
                    runtime.getWindowId(), roi.toShortText(),
                    captureRect[0], captureRect[1], captureRect[2], captureRect[3],
                    baselineAgeMs(state), state.source());
            return null;
        }

        boolean changed = !ImageFinder.isMatch(baseline, current, WUHUAN_TRACKER_COMBAT_ROI_SAME_TOLERANCE);
        log.info("[five-ring-v2 combat-exit] tracker ROI compare while IN_COMBAT: windowId={} windowRelativeRoi={} absoluteRect=({}, {})-({}, {}) changedCandidate={} tolerance={} baselineAgeMs={} source={}",
                runtime.getWindowId(), roi.toShortText(),
                captureRect[0], captureRect[1], captureRect[2], captureRect[3], changed,
                WUHUAN_TRACKER_COMBAT_ROI_SAME_TOLERANCE, baselineAgeMs(state), state.source());
        if (!changed) {
            current.flush();
            return null;
        }

        FiveRingStepOutcome positiveEvidenceOutcome = tryResolvePostCombatPositiveEvidence(
                context, state, runtime);
        if (positiveEvidenceOutcome != null) {
            current.flush();
            return positiveEvidenceOutcome;
        }

        AutoCombatService.TickResult trustedStatus = autoCombatService.probeWindowCombatStateReadOnly(
                context, "wuhuan-v2:tracker-roi-candidate-no-positive");
        log.info("[five-ring-v2 combat-exit] tracker ROI candidate has no positive evidence; trusted battle status={} windowId={} roi={} source={}",
                trustedStatus, runtime.getWindowId(), roi.toShortText(), state.source());
        if (trustedStatus == AutoCombatService.TickResult.IN_COMBAT) {
            FiveRingPhaseContext refreshed = state.withWuhuanTrackerCombatBaseline(
                    current, System.currentTimeMillis(), "post-combat-roi-false-positive-refresh-baseline");
            log.info("[five-ring-v2 combat-exit] tracker ROI candidate rejected by trusted IN_COMBAT; refresh baseline and keep waiting: windowId={} roi={} source={}",
                    runtime.getWindowId(), roi.toShortText(), state.source());
            return FiveRingStepOutcome.sharedState(
                    refreshed,
                    "tracker ROI changed but trusted battle state is still IN_COMBAT; baseline refreshed");
        }

        current.flush();
        releaseWindowCombatStateAfterWuhuanEvidence(context,
                "trusted-battle-state-not-in-combat",
                "wuhuan-v2:tracker-roi-candidate-trusted-free");
        return FiveRingStepOutcome.continueTo(
                state.clearWuhuanTrackerCombatBaseline("post-combat-roi-trusted-free")
                        .next(FiveRingPhase.SYNC_TASK_PANEL, "post-combat-roi-trusted-free-sync"),
                "tracker ROI changed and trusted battle state is free; sync tracker");
    }

    private int[] wuhuanTrackerRoiAbsoluteCaptureRect(WindowRuntimeContext runtime,
                                                       OcrWindowRegion windowRelativeRoi,
                                                       String reason) {
        if (runtime == null) {
            log.warn("[five-ring-v2 combat-exit] tracker ROI capture skipped: reason={} windowRelativeRoi={} windowId=null",
                    reason, windowRelativeRoi == null ? null : windowRelativeRoi.toShortText());
            return null;
        }
        if (windowRelativeRoi == null) {
            log.warn("[five-ring-v2 combat-exit] tracker ROI capture skipped: reason={} windowId={} roi=null",
                    reason, runtime.getWindowId());
            return null;
        }
        tracker.refreshWindowState();
        int baseX = tracker.getWindowBaseX();
        int baseY = tracker.getWindowBaseY();
        String hwnd = runtime.getNativeBinding() == null ? null : runtime.getNativeBinding().getNativeHandle();
        if (baseX < 0 || baseY < 0) {
            log.warn("[five-ring-v2 combat-exit] tracker ROI capture skipped: reason={} windowId={} hwnd={} windowRelativeRoi={} base=({}, {})",
                    reason, runtime.getWindowId(), hwnd, windowRelativeRoi.toShortText(), baseX, baseY);
            return null;
        }

        int x1 = baseX + windowRelativeRoi.x1();
        int y1 = baseY + windowRelativeRoi.y1();
        int x2 = baseX + windowRelativeRoi.x2();
        int y2 = baseY + windowRelativeRoi.y2();
        if (x2 <= x1 || y2 <= y1) {
            log.warn("[five-ring-v2 combat-exit] tracker ROI capture skipped: reason={} windowId={} hwnd={} windowRelativeRoi={} absoluteRect=({}, {})-({}, {}) base=({}, {})",
                    reason, runtime.getWindowId(), hwnd, windowRelativeRoi.toShortText(), x1, y1, x2, y2, baseX, baseY);
            return null;
        }

        log.info("[five-ring-v2 combat-exit] tracker ROI capture rect resolved: reason={} windowId={} hwnd={} windowRelativeRoi={} absoluteRect=({}, {})-({}, {}) base=({}, {})",
                reason, runtime.getWindowId(), hwnd, windowRelativeRoi.toShortText(), x1, y1, x2, y2, baseX, baseY);
        return new int[]{x1, y1, x2, y2};
    }

    private FiveRingStepOutcome tryResolvePostCombatPositiveEvidence(TaskExecutionContext context,
                                                                     FiveRingPhaseContext state,
                                                                     WindowRuntimeContext runtime) {
        PreparedDialogAction preparedTracker = freshPreparedWuhuanTrackerGreenAction(runtime);
        FiveRingPhaseContext evidenceState = mergeWuhuanTrackerRegionsFromRuntimeCache(
                state, runtime, "post-combat-roi-existing-prepared");
        if (preparedTracker == null) {
            TaskTrackerPanelPrepareResult freshPrepare = taskTrackerPanelService.prepareWuhuanPathingLink(
                    "wuhuan-v2:post-combat-roi-positive-tracker-prepare", false);
            evidenceState = mergeWuhuanTrackerRegionsFromPrepareResult(
                    evidenceState, freshPrepare, "post-combat-roi-fresh-prepare");
            if (freshPrepare != null && freshPrepare.hasAction()) {
                PreparedDialogAction action = freshPrepare.getPreparedAction().toBuilder()
                        .windowId(runtime.getWindowId())
                        .hwnd(runtime.getNativeBinding().getNativeHandle())
                        .source("wuhuan-v2:post-combat-roi-positive-tracker-prepare:"
                                + freshPrepare.getPreparedAction().getSource())
                        .build();
                runtime.clearTaskTrackerPanelNegativeResult("post-combat ROI fresh positive tracker action prepared");
                runtime.updatePreparedDialogAction(action);
                preparedTracker = action;
                log.info("[five-ring-v2 combat-exit] fresh tracker-link prepared after ROI candidate: windowId={} source={} click=({}, {}) trackerPanelRegion={} wuhuanBlockRegion={}",
                        runtime.getWindowId(), action.getSource(), action.getAbsoluteX(), action.getAbsoluteY(),
                        freshPrepare.getTrackerPanelRegion() == null ? null : freshPrepare.getTrackerPanelRegion().toShortText(),
                        freshPrepare.getWuhuanTrackerBlockRegion() == null ? null : freshPrepare.getWuhuanTrackerBlockRegion().toShortText());
            } else if (freshPrepare != null && freshPrepare.hasNegative()) {
                TaskTrackerPanelNegativeResult negative = freshPrepare.getNegativeResult();
                log.info("[five-ring-v2 combat-exit] fresh tracker prepare after ROI candidate produced negative; not positive evidence: windowId={} status={} reason={} trackerPanelRegion={} wuhuanBlockRegion={}",
                        runtime.getWindowId(), negative.getStatus(), negative.getReason(),
                        negative.getTrackerPanelRegion() == null ? null : negative.getTrackerPanelRegion().toShortText(),
                        negative.getWuhuanTrackerBlockRegion() == null ? null : negative.getWuhuanTrackerBlockRegion().toShortText());
            } else {
                log.info("[five-ring-v2 combat-exit] fresh tracker prepare after ROI candidate produced no action; not positive evidence: windowId={} source={}",
                        runtime.getWindowId(), state.source());
            }
        }
        if (preparedTracker != null) {
            log.info("[five-ring-v2 combat-exit] positive evidence after tracker ROI candidate: type=tracker-link windowId={} preparedSource={} matched={} click=({}, {}) verifiedAgeMs={}",
                    runtime.getWindowId(), preparedTracker.getSource(), preparedTracker.getMatchedText(),
                    preparedTracker.getAbsoluteX(), preparedTracker.getAbsoluteY(),
                    Math.max(0L, System.currentTimeMillis() - preparedTracker.getLastVerifiedAtMs()));
            releaseWindowCombatStateAfterWuhuanEvidence(context,
                    "tracker-link",
                    "wuhuan-v2:tracker-roi-positive-tracker-link");
            TrackerPathingAttempt trackerAttempt = tryClickWuhuanTrackerLink(
                    context,
                    evidenceState.clearWuhuanTrackerCombatBaseline("post-combat-roi-positive-tracker-link"),
                    "post-combat-roi-positive-tracker-link",
                    true);
            if (trackerAttempt.status == TrackerPathingStatus.PATHING_STARTED) {
                return FiveRingStepOutcome.pathingStarted(
                        trackerAttempt.state.withTaskAccepted("post-combat-roi-tracker-link-clicked")
                                .resetUiErrorCount("post-combat-roi-tracker-link-success"),
                        "post-combat tracker ROI positive evidence clicked tracker green link");
            }
            log.warn("[five-ring-v2 combat-exit] prepared tracker positive evidence vanished or click failed after release: status={} windowId={} source={}",
                    trackerAttempt.status, runtime.getWindowId(), state.source());
            return FiveRingStepOutcome.continueTo(
                    trackerAttempt.state.next(FiveRingPhase.SYNC_TASK_PANEL,
                            "post-combat-roi-tracker-evidence-click-not-started"),
                    "post-combat tracker evidence did not start pathing; sync tracker");
        }

        FiveRingPhaseContext noBaselineState = evidenceState.clearWuhuanTrackerCombatBaseline(
                "post-combat-roi-check-completion-dialog");
        FiveRingStepOutcome completionStory = resolveFiveRingCompletionStoryOutcome(
                noBaselineState,
                "wuhuan-v2:tracker-roi-positive-completion",
                "after tracker ROI candidate");
        if (completionStory != null) {
            log.info("[five-ring-v2 combat-exit] positive evidence after tracker ROI candidate: type=completion-dialog windowId={} source={}",
                    runtime.getWindowId(), state.source());
            releaseWindowCombatStateAfterWuhuanEvidence(context,
                    "completion-dialog",
                    "wuhuan-v2:tracker-roi-positive-completion");
            return completionStory;
        }

        log.info("[five-ring-v2 combat-exit] positive evidence after tracker ROI candidate: type=none windowId={} source={}",
                runtime.getWindowId(), state.source());
        return null;
    }

    private PreparedDialogAction freshPreparedWuhuanTrackerGreenAction(WindowRuntimeContext runtime) {
        if (runtime == null) {
            return null;
        }
        PreparedDialogAction action = runtime.getPreparedDialogAction();
        if (action == null || !action.matches(DialogOperation.TASK_TRACKER_PATHING, "wuhuan")) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (!action.verifiedWithin(now, PREPARED_TRACKER_ACTION_MAX_AGE_MS)) {
            log.info("[five-ring-v2 combat-exit] prepared tracker action is not fresh enough for positive evidence: windowId={} source={} verifiedAgeMs={} maxAgeMs={} operation={} target={}",
                    runtime.getWindowId(), action.getSource(),
                    Math.max(0L, now - action.getLastVerifiedAtMs()), PREPARED_TRACKER_ACTION_MAX_AGE_MS,
                    action.getOperation(), action.getTargetKeyword());
            return null;
        }
        return action;
    }

    private FiveRingPhaseContext mergeWuhuanTrackerRegionsFromPrepareResult(FiveRingPhaseContext state,
                                                                            TaskTrackerPanelPrepareResult result,
                                                                            String source) {
        if (result == null) {
            return state;
        }
        return mergeWuhuanTrackerRegions(
                state,
                result.getTrackerPanelRegion(),
                result.getWuhuanTrackerBlockRegion(),
                source);
    }

    private FiveRingPhaseContext mergeWuhuanTrackerRegionsFromNegative(FiveRingPhaseContext state,
                                                                       TaskTrackerPanelNegativeResult negative,
                                                                       String source) {
        if (negative == null) {
            return state;
        }
        return mergeWuhuanTrackerRegions(
                state,
                negative.getTrackerPanelRegion(),
                negative.getWuhuanTrackerBlockRegion(),
                source);
    }

    private FiveRingPhaseContext mergeWuhuanTrackerRegionsFromRuntimeCache(FiveRingPhaseContext state,
                                                                           WindowRuntimeContext runtime,
                                                                           String source) {
        TaskTrackerPanelCacheEntry cache = runtime == null ? null : runtime.getTaskTrackerPanelCache();
        if (cache == null || !"wuhuan".equals(cache.getTaskCode())) {
            log.info("[five-ring-v2 tracker] no 五环 tracker ROI cache to merge: source={} windowId={} reason={}",
                    source, runtime == null ? null : runtime.getWindowId(),
                    cache == null ? "no-cache" : "task-code-mismatch:" + cache.getTaskCode());
            return state;
        }
        return mergeWuhuanTrackerRegions(
                state,
                cache.getTrackerPanelRegion(),
                cache.getWuhuanTrackerBlockRegion(),
                source + ":cache:" + cache.getSource());
    }

    private FiveRingPhaseContext mergeWuhuanTrackerRegions(FiveRingPhaseContext state,
                                                           OcrWindowRegion trackerPanelRegion,
                                                           OcrWindowRegion wuhuanTrackerBlockRegion,
                                                           String source) {
        if (state == null || wuhuanTrackerBlockRegion == null) {
            log.info("[five-ring-v2 tracker] skip 五环 tracker ROI merge: source={} reason={} trackerPanelRegion={} wuhuanBlockRegion={}",
                    source, state == null ? "state-null" : "wuhuan-block-missing",
                    trackerPanelRegion == null ? null : trackerPanelRegion.toShortText(),
                    wuhuanTrackerBlockRegion == null ? null : wuhuanTrackerBlockRegion.toShortText());
            return state;
        }
        OcrWindowRegion block = wuhuanTrackerBlockRegion.clamp(GAME_CLIENT_WIDTH, GAME_CLIENT_HEIGHT);
        if (!block.isValid()) {
            log.warn("[five-ring-v2 tracker] skip invalid 五环 tracker ROI merge: source={} raw={} clamped={}",
                    source, wuhuanTrackerBlockRegion.toShortText(), block.toShortText());
            return state;
        }
        OcrWindowRegion panel = trackerPanelRegion == null
                ? state.trackerPanelRegion()
                : trackerPanelRegion.clamp(GAME_CLIENT_WIDTH, GAME_CLIENT_HEIGHT);
        if (panel != null && !panel.isValid()) {
            panel = state.trackerPanelRegion();
        }
        if (block.equals(state.wuhuanTrackerBlockRegion())
                && (panel == null ? state.trackerPanelRegion() == null : panel.equals(state.trackerPanelRegion()))) {
            return state;
        }
        log.info("[five-ring-v2 tracker] merge 五环 tracker ROI into phase state: source={} trackerPanelRegion={} wuhuanBlockRegion={} previousBlock={}",
                source,
                panel == null ? null : panel.toShortText(),
                block.toShortText(),
                state.wuhuanTrackerBlockRegion() == null ? null : state.wuhuanTrackerBlockRegion().toShortText());
        return state.withTrackerRegions(panel, block, source);
    }

    private void releaseWindowCombatStateAfterWuhuanEvidence(TaskExecutionContext context,
                                                            String evidenceType,
                                                            String source) {
        GameContext.ActionState before = gameContext.getCurrentActionState();
        if (before == GameContext.ActionState.IN_COMBAT) {
            gameContext.setCurrentActionState(GameContext.ActionState.FREE);
            // Match the normal AutoCombatService combat-exit path before running post-combat first-aid.
            playerStateService.resetCheckCounter();
        }
        log.info("{} [five-ring-v2 combat-exit] release/correct combat state after tracker ROI verification: evidenceType={} source={} before={} after={}",
                context == null ? "[window=unknown]" : context.getLogPrefix(),
                evidenceType, source, before, gameContext.getCurrentActionState());
        PlayerStateService.FirstAidNoFocusProbeResult firstAidProbe =
                playerStateService.probeAndConsumeHealthyFirstAidNoFocus(context, source + ":post-combat");
        if ((firstAidProbe == PlayerStateService.FirstAidNoFocusProbeResult.SUPPLY_NEEDED
                || firstAidProbe == PlayerStateService.FirstAidNoFocusProbeResult.UNKNOWN)
                && !playerStateService.performCachedFirstAidPlanNow(context)) {
            log.warn("{} [five-ring-v2 combat-exit] no-focus first-aid skipped: cached plan unavailable evidenceType={} source={} precheck={}",
                    context == null ? "[window=unknown]" : context.getLogPrefix(),
                    evidenceType, source, firstAidProbe);
        }
        playerStateService.ensureSheYaoXiangActiveForLeaderTask(source + ":post-combat", context);
    }

    private OcrWindowRegion validWuhuanTrackerBlockRegion(FiveRingPhaseContext state, String reason) {
        if (state == null || state.wuhuanTrackerBlockRegion() == null) {
            return null;
        }
        OcrWindowRegion roi = state.wuhuanTrackerBlockRegion().clamp(GAME_CLIENT_WIDTH, GAME_CLIENT_HEIGHT);
        if (!roi.isValid()) {
            log.warn("[five-ring-v2 combat-exit] invalid cached 五环 tracker ROI: reason={} raw={} clamped={}",
                    reason, state.wuhuanTrackerBlockRegion().toShortText(), roi.toShortText());
            return null;
        }
        return roi;
    }

    private long baselineAgeMs(FiveRingPhaseContext state) {
        if (state == null || state.wuhuanTrackerCombatBaselineCapturedAtMs() <= 0L) {
            return -1L;
        }
        return Math.max(0L, System.currentTimeMillis() - state.wuhuanTrackerCombatBaselineCapturedAtMs());
    }

    private long pathingAgeMs(FiveRingPhaseContext state) {
        if (state == null || state.pathingStartedAtMs() <= 0L) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, System.currentTimeMillis() - state.pathingStartedAtMs());
    }

    private boolean isUsablePathingSnapshot(FiveRingPhaseContext state,
                                            WindowPathingSnapshot snapshot,
                                            WindowRuntimeContext runtime) {
        if (state == null || !state.pathingIntentExpected() || snapshot == null || snapshot.getIntent() == null) {
            return false;
        }
        WindowPathingIntent intent = snapshot.getIntent();
        if (!isExpectedPathingSource(state.pathingIntentSource(), intent.getSource())) {
            return false;
        }
        if (!isExpectedPathingTarget(state.pathingIntentSource(), intent)) {
            return false;
        }
        /*
         * The created-at grace guard only rejects snapshots from a PREVIOUS navigation of the same
         * source. When NavigationService reused the already-active intent, the phase re-stamped
         * pathingStartedAtMs but the intent createdAt stayed old; that snapshot still describes the
         * current navigation and its terminal (ARRIVED/STOPPED_AWAY) must stay consumable.
         */
        if (state.pathingStartedAtMs() > 0L
                && intent.getCreatedAtMs() + PATHING_INTENT_CREATED_AT_GRACE_MS < state.pathingStartedAtMs()
                && !isCurrentActivePathingIntent(runtime, intent)) {
            return false;
        }
        WindowPathingState observed = snapshot.getState();
        if (observed == WindowPathingState.ARRIVED || observed == WindowPathingState.STOPPED_AWAY) {
            return true;
        }
        long snapshotAgeMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
        /*
         * ACTIVE/UNKNOWN from the same intent is still unsafe if the watcher has not refreshed it
         * recently. Five-window runs can otherwise consume an old "still moving" snapshot and keep
         * yielding long after the route dialog or target state has changed.
         */
        return snapshot.isProbeInProgress() || snapshotAgeMs <= OBSERVER_SNAPSHOT_MAX_AGE_MS;
    }

    private boolean isCurrentActivePathingIntent(WindowRuntimeContext runtime, WindowPathingIntent intent) {
        if (runtime == null || intent == null || intent.getIntentId() == null) {
            return false;
        }
        WindowPathingIntent active = runtime.getActivePathingIntent().orElse(null);
        return active != null && intent.getIntentId().equals(active.getIntentId());
    }

    private boolean isExpectedPathingSource(String expectedPrefix, String actualSource) {
        if (expectedPrefix == null || expectedPrefix.isBlank()
                || actualSource == null || actualSource.isBlank()) {
            return false;
        }
        return actualSource.equals(expectedPrefix) || actualSource.startsWith(expectedPrefix + ":");
    }

    private boolean isExpectedPathingTarget(String expectedSource, WindowPathingIntent intent) {
        if (intent == null || expectedSource == null) {
            return false;
        }
        if (expectedSource.startsWith(TRACKER_GREEN_CLICK_PATHING_SOURCE_PREFIX)
                || expectedSource.startsWith(PREPARED_TRACKER_PANEL_PATHING_SOURCE_PREFIX)) {
            return intent.getType() == WindowPathingIntentType.UNTARGETED_TRACKER
                    && intent.getTargetMapName() == null
                    && intent.getTargetX() == null
                    && intent.getTargetY() == null;
        }
        if (SHOE_SHOP_ENTRY_NAV_SOURCE.equals(expectedSource)) {
            if (!TARGET_MAP_NAME.equals(intent.getTargetMapName())) {
                return false;
            }
            if (intent.getTargetX() == null && intent.getTargetY() == null) {
                return true;
            }
            return Integer.valueOf(SHOE_SHOP_ENTRY_X).equals(intent.getTargetX())
                    && Integer.valueOf(SHOE_SHOP_ENTRY_Y).equals(intent.getTargetY());
        }
        if (ACCEPT_NPC_NAV_SOURCE.equals(expectedSource)) {
            if (!TARGET_MAP_NAME.equals(intent.getTargetMapName())) {
                return false;
            }
            if (intent.getTargetX() == null && intent.getTargetY() == null) {
                return true;
            }
            return Integer.valueOf(NPC_COOR_X).equals(intent.getTargetX())
                    && Integer.valueOf(NPC_COOR_Y).equals(intent.getTargetY());
        }
        return false;
    }

    private boolean isSnapshotNearAcceptNpc(WindowPathingSnapshot snapshot) {
        return snapshot != null
                && snapshot.getCurrentMapName() != null
                && snapshot.getCurrentX() != null
                && snapshot.getCurrentY() != null
                && gameStateUtil.isNearCoordinate(
                snapshot.getCurrentMapName(),
                snapshot.getCurrentX(),
                snapshot.getCurrentY(),
                TARGET_MAP_NAME,
                NPC_COOR_X,
                NPC_COOR_Y,
                TASK_NPC_DIRECT_CLICK_DISTANCE);
    }

    private boolean isWindowCombatActive() {
        return gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT;
    }

    private FiveRingStepOutcome handleDialog(TaskExecutionContext context, FiveRingPhaseContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        boolean giveItemMap = isGiveItemMap(currentObservedMapName());
        if (!giveItemMap) {
            FiveRingStepOutcome completionStory = resolveFiveRingCompletionStoryOutcome(
                    state, "wuhuan-v2:handle-dialog-finished-story", "after battle");
            if (completionStory != null) {
                return completionStory;
            }
        }

        DialogResultStatus giveResult = tryGiveItemAndTriggerPathingIfPossible(context, state);
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        if (giveResult == DialogResultStatus.GIVE_ITEM_DONE) {
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "give-item-done-read-tracker"),
                    "gave shoe; read tracker for next green link");
        }
        if (giveResult == DialogResultStatus.STORY_IGNORED) {
            if (!giveItemMap) {
                FiveRingStepOutcome storyIgnoredCompletion = resolveFiveRingCompletionStoryOutcome(
                        state, "wuhuan-v2:handle-dialog-story-ignored-completion", "after ignored story");
                if (storyIgnoredCompletion != null) {
                    return storyIgnoredCompletion;
                }
            }
            /*
             * Story text can appear during the transition into combat. The window observer owns the
             * combat verdict now; 五环 only waits if that observer has already marked this window as
             * fighting, otherwise it can safely re-read the task tracker.
             */
            if (isWindowCombatActive()) {
                return FiveRingStepOutcome.sharedState(
                        state.withCombatObservedSincePathing("story-ignored-window-combat-active"),
                        "story dialog ignored; window combat state active");
            }
            return FiveRingStepOutcome.sharedState(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "story-ignored-window-combat-free"),
                    "story dialog ignored; sync task panel");
        }
        if (giveResult == DialogResultStatus.NO_DIALOG) {
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "dialog-not-actionable"),
                    "no actionable dialog; sync task panel");
        }
        if (giveResult == DialogResultStatus.GIVE_OPTION_NOT_FOUND) {
            log.warn("[five-ring-v2] unknown option dialog without give entry; clean and resync");
            cleanupRetryableDialog("wuhuan-v2:giveOptionNotFound");
            return FiveRingStepOutcome.sharedState(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "give-option-not-found"),
                    "give option not found; cleanup done");
        }
        if (giveResult == DialogResultStatus.INTERRUPTED) {
            return FiveRingStepOutcome.stopped(state, "give item interrupted");
        }

        FiveRingPhaseContext errorState = state.increaseUiErrorCount("give-item-failed");
        log.warn("[five-ring-v2] give-item dialog handling failed: status={} errorCount={} max={}",
                giveResult, errorState.uiErrorCount(), MAX_GIVE_ITEM_FAILURE_BEFORE_FAIL);
        if (errorState.uiErrorCount() >= MAX_GIVE_ITEM_FAILURE_BEFORE_FAIL) {
            return FiveRingStepOutcome.failed(
                    errorState,
                    "give item failed too many times; stop five-ring round");
        }
        return FiveRingStepOutcome.sharedState(
                errorState.next(FiveRingPhase.SYNC_TASK_PANEL, "give-item-failed"),
                "give item failed; yield before tracker resync");
    }

    private FiveRingStepOutcome syncTaskPanel(TaskExecutionContext context,
                                              FiveRingPhaseContext state,
                                              boolean allowFinished) {
        log.info("[five-ring-v2 tracker] scan left task tracker and click 五环 green pathing link");
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        if (isWindowCombatActive()) {
            return FiveRingStepOutcome.sharedState(
                    state.withCombatObservedSincePathing("window-combat-active-before-tracker-sync"),
                    "window combat state active; skip task panel sync");
        }
        FiveRingStepOutcome stoppedAwayRecovery = resolveStoppedAwayTrackerIntentBeforeSync(context, state);
        if (stoppedAwayRecovery != null) {
            return stoppedAwayRecovery;
        }
        TrackerPathingAttempt trackerAttempt = tryClickWuhuanTrackerLink(context, state, "sync", true);
        if (trackerAttempt.status == TrackerPathingStatus.PATHING_STARTED) {
            log.info("[five-ring-v2 tracker] 五环 tracker green link clicked");
            return FiveRingStepOutcome.pathingStarted(
                    trackerAttempt.state.withTaskAccepted("tracker-link-clicked")
                            .resetUiErrorCount("sync-success"),
                    "tracker green link clicked");
        }
        WuhuanTrackerTitleGate titleGate = readWuhuanTrackerTitleGate(
                trackerAttempt.state, "sync:" + trackerAttempt.status);
        FiveRingPhaseContext titleState = titleGate.state();
        if (trackerAttempt.status == TrackerPathingStatus.RUNNER_PREPARED_NOT_READY) {
            /*
             * Runner not-ready is only a scheduling fact. The business fact is the left 五环 title:
             * title visible means the task still exists and we wait for/click the tracker; title
             * missing means the tracker is gone and the returned-dialog fallback may run.
             */
            if (!titleGate.titleVisible() && allowFinished) {
                FiveRingStepOutcome acceptReturnedDialog = tryHandleAcceptReturnedDialogAfterTrackerMiss(
                        titleState, "runner-prepared-not-ready-title-miss");
                if (acceptReturnedDialog != null) {
                    log.info("[five-ring-v2 tracker] runner prepared tracker action not ready but returned dialog handled: "
                                    + "source={} next={} message={}",
                            state.source(), acceptReturnedDialog.nextState().phase(), acceptReturnedDialog.message());
                    return acceptReturnedDialog;
                }
            } else {
                log.info("[five-ring-v2 tracker] skip returned-dialog fallback because 五环 title is still visible "
                                + "or fallback is disabled: source={} allowFinished={} titleVisible={} taskAccepted={}",
                        state.source(), allowFinished, titleGate.titleVisible(), titleState.taskAccepted());
            }
            log.info("[five-ring-v2 tracker] runner prepared tracker action not ready; wait for Runner prepared action: "
                            + "allowFinished={} titleVisible={} taskAccepted={}",
                    allowFinished, titleGate.titleVisible(), titleState.taskAccepted());
            return FiveRingStepOutcome.sharedState(
                    titleState.retrySamePhase("runner-prepared-tracker-action-not-ready"),
                    "runner prepared tracker action not ready; retry");
        }
        if (titleGate.titleVisible()
                && (trackerAttempt.status == TrackerPathingStatus.TASK_FOUND_NO_GREEN
                || trackerAttempt.status == TrackerPathingStatus.TASK_FOUND_NO_LINK)) {
            String reason = trackerAttempt.status == TrackerPathingStatus.TASK_FOUND_NO_GREEN
                    ? "tracker-no-green"
                    : "tracker-no-coordinate-link";
            FiveRingPhaseContext errorState = titleState.increaseUiErrorCount(reason);
            log.warn("[five-ring-v2 tracker] 五环 tracker block found but no usable green pathing link: status={} errorCount={}",
                    trackerAttempt.status, errorState.uiErrorCount());
            if (errorState.uiErrorCount() >= MAX_UI_ERROR_BEFORE_CLEANUP) {
                /*
                 * The left tracker block is already visible here. Generic UI cleanup cannot make a
                 * missing/temporarily hidden green link appear, and in five-window runs it was adding
                 * ~2s every three retries. Drop only the cached block and re-read the tracker.
                 */
                errorState = errorState.clearWuhuanTrackerBlockRegion(reason + "-cache-cleared")
                        .resetUiErrorCount(reason + "-reset");
            }
            return FiveRingStepOutcome.sharedState(
                    errorState.next(FiveRingPhase.SYNC_TASK_PANEL, reason + "-retry"),
                    "tracker has 五环 but no usable green pathing link; retry");
        }
        if (trackerAttempt.status == TrackerPathingStatus.CLICK_FAILED) {
            FiveRingPhaseContext errorState = titleState.increaseUiErrorCount("tracker-click-failed");
            log.warn("[five-ring-v2 tracker] 五环 tracker green click failed: errorCount={}",
                    errorState.uiErrorCount());
            return FiveRingStepOutcome.sharedState(
                    errorState.next(FiveRingPhase.SYNC_TASK_PANEL, "tracker-click-failed-retry"),
                    "tracker green click failed; retry");
        }
        if (allowFinished && !titleGate.titleVisible()) {
            FiveRingStepOutcome acceptReturnedDialog = tryHandleAcceptReturnedDialogAfterTrackerMiss(
                    titleState, "tracker-title-miss");
            if (acceptReturnedDialog != null) {
                return acceptReturnedDialog;
            }
        } else {
            log.info("[five-ring-v2 tracker] skip returned-dialog fallback after tracker miss because 五环 title is visible "
                            + "or fallback is disabled: allowFinished={} titleVisible={} taskAccepted={} source={}",
                    allowFinished, titleGate.titleVisible(), titleState.taskAccepted(), state.source());
        }
        FiveRingPhaseContext errorState = titleState.increaseUiErrorCount("tracker-not-found");
        log.warn("[five-ring-v2 tracker] 五环 tracker block not found; retry left tracker without opening legacy task panel: "
                        + "allowFinished={} taskAccepted={} errorCount={}",
                allowFinished, titleState.taskAccepted(), errorState.uiErrorCount());
        if (errorState.uiErrorCount() >= MAX_TRACKER_NOT_FOUND_BEFORE_FAIL) {
            /*
             * 五环 V2 treats the left task tracker as the source of truth after startup/accept.
             * Re-entering ACCEPT_TASK here can make a character that already has a task click 云游大师
             * again and loop. Surface the failure so the UI can show the reason instead.
             */
            log.error("[five-ring-v2 tracker] tracker missing after {} attempts; fail instead of returning to accept task: "
                            + "phase={} source={} taskAccepted={} allowFinished={}",
                    errorState.uiErrorCount(), state.phase(), state.source(), titleState.taskAccepted(), allowFinished);
            return FiveRingStepOutcome.failed(
                    errorState,
                    "五环左侧任务追踪找不到，已超过重试上限；请检查任务追踪面板是否被遮挡或任务是否丢失");
        }
        if (errorState.uiErrorCount() >= MAX_UI_ERROR_BEFORE_CLEANUP) {
            log.error("[five-ring-v2 tracker] tracker failed {} times; clear cached tracker block only",
                    errorState.uiErrorCount());
            /*
             * Generic UI cleanup cannot remove player-name/tooltip overlays on the left tracker.
             * Avoid spending input turns on a cleanup that cannot reveal the 五环 block; the next
             * pass should recapture the tracker area from scratch instead.
             */
            errorState = errorState.clearWuhuanTrackerBlockRegion("tracker-not-found-cache-cleared");
        }
        return FiveRingStepOutcome.sharedState(
                errorState.next(FiveRingPhase.SYNC_TASK_PANEL, "tracker-retry-later"),
                "tracker task not found; retry");
    }

    private WuhuanTrackerTitleGate readWuhuanTrackerTitleGate(FiveRingPhaseContext state, String reason) {
        TaskTrackerPanelReadResult titleRead = taskTrackerPanelService.readWuhuanTrackerTitle(
                "wuhuan-v2:title-gate:" + safeFileToken(reason), true);
        if (titleRead.isFound()) {
            log.info("[five-ring-v2 tracker] 五环 title gate hit: reason={} title={} taskAccepted={}",
                    reason,
                    titleRead.getTitleTemplate() == null ? null : titleRead.getTitleTemplate().getDisplayName(),
                    state != null && state.taskAccepted());
            FiveRingPhaseContext acceptedState = state == null
                    ? state
                    : state.withTaskAccepted("wuhuan-title-visible:" + reason);
            return new WuhuanTrackerTitleGate(true, acceptedState);
        }
        log.info("[five-ring-v2 tracker] 五环 title gate miss: reason={} taskAccepted={}",
                reason, state != null && state.taskAccepted());
        FiveRingPhaseContext missingState = state == null
                ? state
                : state.clearWuhuanTrackerBlockRegion("wuhuan-title-miss:" + reason);
        return new WuhuanTrackerTitleGate(false, missingState);
    }

    private FiveRingStepOutcome resolveStoppedAwayTrackerIntentBeforeSync(TaskExecutionContext context,
                                                                          FiveRingPhaseContext state) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot snapshot = runtime == null ? null : runtime.getPathingSnapshot();
        if (!isStoppedAwayWuhuanTrackerPathing(snapshot)) {
            return null;
        }
        WindowPathingIntent intent = snapshot.getIntent();
        long intentAgeMs = Math.max(0L, System.currentTimeMillis() - intent.getCreatedAtMs());
        long snapshotAgeMs = Math.max(0L, System.currentTimeMillis() - snapshot.getUpdatedAtMs());
        log.info("[five-ring-v2 tracker] stopped-away tracker intent before sync: windowId={} intentId={} source={} pathingState={} current={}({}, {}) intentAgeMs={} snapshotAgeMs={} reason=check-business-dialog-before-clear",
                runtime.getWindowId(), intent.getIntentId(), intent.getSource(), snapshot.getState(),
                snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                intentAgeMs, snapshotAgeMs);

        if (!isGiveItemMap(snapshot.getCurrentMapName())) {
            boolean cleared = clearStoppedAwayTrackerIntent(runtime,
                    "stopped-away before sync outside give-item map; tracker sync first");
            log.info("[five-ring-v2 tracker] stopped-away tracker intent outside give-item map before sync; skip business dialog and continue tracker prepare: windowId={} intentId={} source={} current={}({}, {}) giveMap={} cleared={}",
                    runtime.getWindowId(), intent.getIntentId(), intent.getSource(),
                    snapshot.getCurrentMapName(), snapshot.getCurrentX(), snapshot.getCurrentY(),
                    GIVE_ITEM_MAP_NAME, cleared);
            return null;
        }

        DialogResultStatus giveResult = tryGiveItemAndTriggerPathingIfPossible(context, state);
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        if (giveResult == DialogResultStatus.GIVE_ITEM_DONE) {
            boolean cleared = clearStoppedAwayTrackerIntent(runtime, "give-item-done before tracker sync");
            log.info("[five-ring-v2 tracker] stopped-away tracker intent business handled by give-item: windowId={} intentId={} source={} pathingState={} giveResult={} cleared={} reason=clear-after-give",
                    runtime.getWindowId(), intent.getIntentId(), intent.getSource(), snapshot.getState(),
                    giveResult, cleared);
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "stopped-away-give-item-done-read-tracker"),
                    "gave shoe after stopped-away tracker intent; read tracker");
        }

        if (giveResult == DialogResultStatus.GIVE_OPTION_NOT_FOUND) {
            log.warn("[five-ring-v2 tracker] stopped-away tracker intent sees option dialog without give entry; run original cleanup before tracker retry: windowId={} intentId={} source={} pathingState={} giveResult={} reason=cleanup-before-clear",
                    runtime.getWindowId(), intent.getIntentId(), intent.getSource(), snapshot.getState(), giveResult);
            cleanupRetryableDialog("wuhuan-v2:stopped-away-giveOptionNotFound");
            boolean cleared = clearStoppedAwayTrackerIntent(runtime,
                    "give option not found cleanup done; allow runner tracker prepare");
            log.warn("[five-ring-v2 tracker] stopped-away tracker intent retry after give-option cleanup: windowId={} intentId={} source={} pathingState={} giveResult={} cleared={} reason=give-option-not-found-retry-clear",
                    runtime.getWindowId(), intent.getIntentId(), intent.getSource(), snapshot.getState(),
                    giveResult, cleared);
            return FiveRingStepOutcome.sharedState(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "stopped-away-give-option-not-found"),
                    "give option not found after stopped-away tracker intent; cleanup done and tracker intent cleared");
        }
        if (giveResult == DialogResultStatus.INTERRUPTED) {
            log.info("[five-ring-v2 tracker] stopped-away tracker intent give-item interrupted; keep intent and stop: windowId={} intentId={} source={} pathingState={} giveResult={}",
                    runtime.getWindowId(), intent.getIntentId(), intent.getSource(), snapshot.getState(), giveResult);
            return FiveRingStepOutcome.stopped(state, "give item interrupted after stopped-away tracker intent");
        }
        if (giveResult != DialogResultStatus.NO_DIALOG && giveResult != DialogResultStatus.STORY_IGNORED) {
            FiveRingPhaseContext errorState = state.increaseUiErrorCount("stopped-away-give-item-failed");
            log.warn("[five-ring-v2 tracker] stopped-away tracker intent preserves give-item failure semantics: windowId={} intentId={} source={} pathingState={} giveResult={} errorCount={} max={}",
                    runtime.getWindowId(), intent.getIntentId(), intent.getSource(), snapshot.getState(),
                    giveResult, errorState.uiErrorCount(), MAX_GIVE_ITEM_FAILURE_BEFORE_FAIL);
            if (errorState.uiErrorCount() >= MAX_GIVE_ITEM_FAILURE_BEFORE_FAIL) {
                return FiveRingStepOutcome.failed(
                        errorState,
                        "give item failed too many times after stopped-away tracker intent; stop five-ring round");
            }
            boolean cleared = clearStoppedAwayTrackerIntent(runtime,
                    "retryable give item failure below fail limit; allow runner tracker prepare");
            log.warn("[five-ring-v2 tracker] stopped-away tracker intent retry after give-item failure: windowId={} intentId={} source={} pathingState={} giveResult={} errorCount={} max={} cleared={} reason=retryable-give-item-failure-clear",
                    runtime.getWindowId(), intent.getIntentId(), intent.getSource(), snapshot.getState(),
                    giveResult, errorState.uiErrorCount(), MAX_GIVE_ITEM_FAILURE_BEFORE_FAIL, cleared);
            return FiveRingStepOutcome.sharedState(
                    errorState.next(FiveRingPhase.SYNC_TASK_PANEL, "stopped-away-give-item-failed"),
                    "give item failed after stopped-away tracker intent; tracker intent cleared before resync");
        }

        boolean cleared = clearStoppedAwayTrackerIntent(runtime,
                "no give dialog before tracker sync; allow runner tracker prepare");
        log.info("[five-ring-v2 tracker] stopped-away tracker intent recovery decision: windowId={} intentId={} source={} pathingState={} giveResult={} cleared={} reason=no-business-dialog-allow-runner-tracker-prepare",
                runtime.getWindowId(), intent.getIntentId(), intent.getSource(), snapshot.getState(),
                giveResult, cleared);
        if (cleared) {
            return FiveRingStepOutcome.sharedState(
                    state.retrySamePhase("stopped-away-tracker-intent-cleared"),
                    "stopped-away tracker intent cleared; wait for Runner tracker prepare");
        }
        return null;
    }

    private boolean isStoppedAwayWuhuanTrackerPathing(WindowPathingSnapshot snapshot) {
        if (snapshot == null || snapshot.getState() != WindowPathingState.STOPPED_AWAY
                || snapshot.getIntent() == null) {
            return false;
        }
        WindowPathingIntent intent = snapshot.getIntent();
        return intent.getType() == WindowPathingIntentType.UNTARGETED_TRACKER
                && isWuhuanTrackerPathingSource(intent.getSource());
    }

    private boolean isWuhuanTrackerPathingSource(String source) {
        return source != null
                && (source.startsWith(PREPARED_TRACKER_PANEL_PATHING_SOURCE_PREFIX)
                || source.startsWith(TRACKER_GREEN_CLICK_PATHING_SOURCE_PREFIX));
    }

    private boolean clearStoppedAwayTrackerIntent(WindowRuntimeContext runtime, String reason) {
        return runtime != null && runtime.clearPathingSignalIfSourcePrefixesAndState(
                List.of(PREPARED_TRACKER_PANEL_PATHING_SOURCE_PREFIX, TRACKER_GREEN_CLICK_PATHING_SOURCE_PREFIX),
                WindowPathingState.STOPPED_AWAY,
                "five-ring stopped-away tracker intent: " + reason);
    }

    private FiveRingStepOutcome tryHandleAcceptReturnedDialogAfterTrackerMiss(FiveRingPhaseContext state,
                                                                              String reason) {
        DialogResult alreadyTaskResult = dialogService.handleDialog(
                DialogHandleRequest.verifyExpectedOptionDialog(
                        "wuhuan-v2:tracker-miss-accept-returned-already-has-task",
                        ALREADY_HAS_TASK_OPTION_TEMPLATE));
        if (alreadyTaskResult.getStatus() == DialogResultStatus.GREEN_TEMPLATE_VISIBLE) {
            log.info("[five-ring-v2 accept] left tracker unavailable but already-has-task dialog is visible: reason={} actionKey={}",
                    reason, alreadyTaskResult.getActionKey());
            cleanupRetryableDialog("wuhuan-v2:tracker-miss-already-has-task");
            return FiveRingStepOutcome.sharedState(
                    state.withTaskAccepted("tracker-miss-already-has-task")
                            .clearWuhuanTrackerBlockRegion("tracker-miss-already-has-task")
                            .next(FiveRingPhase.SYNC_TASK_PANEL, "tracker-miss-already-has-task-retry"),
                    "already-has-task dialog returned after accept; cleanup and read tracker");
        }

        FiveRingStepOutcome completionStory = resolveFiveRingCompletionStoryOutcome(
                state, "wuhuan-v2:tracker-miss-finished-story", "after tracker miss");
        if (completionStory != null) {
            return completionStory;
        }
        return null;
    }

    private FiveRingStepOutcome tryAcceptInitialTaskFromCurrentScreen(TaskExecutionContext context,
                                                                      FiveRingPhaseContext state,
                                                                      String reason) {
        return tryAcceptInitialTaskFromCurrentScreen(context, state, reason,
                isNearAcceptNpc(syncAcceptNpcSetupPosition()));
    }

    private FiveRingStepOutcome tryAcceptInitialTaskFromCurrentScreen(TaskExecutionContext context,
                                                                      FiveRingPhaseContext state,
                                                                      String reason,
                                                                      boolean nearAcceptNpc) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        if (!nearAcceptNpc) {
            log.info("[five-ring-v2 accept] current option dialog is not checked as accept dialog before near NPC: reason={}",
                    reason);
            return null;
        }

        DialogType dialogType = dialogService.handleDialog(
                DialogHandleRequest.inspect("wuhuan-v2:current-screen-accept-check")).getDialogType();
        if (dialogType == DialogType.NONE) {
            log.info("[five-ring-v2 accept] no accept dialog on current screen, skip direct accept: reason={}", reason);
            return null;
        }

        if (dialogType == DialogType.STORY) {
            boolean dailyLimit = isFiveRingDailyLimitStoryVisible("wuhuan-v2:current-screen-accept-story");
            if (dailyLimit) {
                log.info("[five-ring-v2 finish] daily-limit story verified while accepting; finish all configured runs without closing dialog");
                return FiveRingStepOutcome.finishedTerminal(
                        state,
                        "five-ring daily-limit story visible while accepting");
            }
            FiveRingStepOutcome completionStory = resolveFiveRingCompletionStoryOutcome(
                    state, "wuhuan-v2:current-screen-accept-story", "while accepting");
            if (completionStory != null) {
                return completionStory;
            }
            log.info("[five-ring-v2 accept] story dialog is not an accept terminal story; close and retry NPC: reason={}",
                    reason);
            dialogService.handleDialog(DialogHandleRequest.clickStory("wuhuan-v2:current-screen-unexpected-story-close"));
            return null;
        }

        log.info("[five-ring-v2 accept] try accepting task from current option dialog: reason={}", reason);
        AcceptDialogPathingResult acceptResult = acceptInitialDialogAndTriggerPathing(context, "currentScreenAcceptPathing");
        if (acceptResult == AcceptDialogPathingResult.TASK_ACCEPTED_NEEDS_SYNC) {
            log.info("[five-ring-v2 accept] current screen accept clicked; read tracker next");
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "current-screen-accept-pathing-unconfirmed"),
                    "current-screen accept clicked; read tracker instead of accepting again");
        }
        if (acceptResult == AcceptDialogPathingResult.TASK_ALREADY_FINISHED) {
            return FiveRingStepOutcome.finishedTerminal(
                    state,
                    "five-ring current screen accept reported finished/daily limit");
        }
        if (acceptResult == AcceptDialogPathingResult.NOT_ACCEPTED) {
            DialogResult alreadyTaskResult = dialogService.handleDialog(
                    DialogHandleRequest.verifyExpectedOptionDialog(
                            "wuhuan-v2:current-screen-already-has-task",
                            ALREADY_HAS_TASK_OPTION_TEMPLATE));
            if (alreadyTaskResult.getStatus() == DialogResultStatus.GREEN_TEMPLATE_VISIBLE) {
                log.info("[five-ring-v2 accept] current option says task already exists; cleanup and read tracker: reason={} actionKey={} point=({}, {})",
                        reason, alreadyTaskResult.getActionKey(),
                        alreadyTaskResult.getAbsoluteX(), alreadyTaskResult.getAbsoluteY());
                cleanupRetryableDialog("wuhuan-v2:current-screen-already-has-task");
                return FiveRingStepOutcome.sharedState(
                        state.next(FiveRingPhase.SYNC_TASK_PANEL, "current-screen-already-has-task"),
                        "already-has-task dialog visible; read tracker");
            }
            log.info("[five-ring-v2 accept] current option is neither accept nor already-has-task; clean unexpected option dialog: reason={}",
                    reason);
            cleanupUnexpectedAcceptDialog(reason);
        }
        return null;
    }

    private AcceptDialogPathingResult acceptInitialDialogAndTriggerPathing(TaskExecutionContext context, String movementSource) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        AtomicReference<Boolean> acceptVerified = new AtomicReference<>(false);
        AtomicReference<AcceptDialogPathingResult> acceptFlowResult = new AtomicReference<>(AcceptDialogPathingResult.NOT_ACCEPTED);
        TaskTransactionOutcome outcome = taskTransactionRunner.runExclusive(
                "wuhuan-v2:acceptDialogAndTriggerPathing",
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
                    for (int attempt = 1; attempt <= 2; attempt++) {
                        DialogResult acceptResult = clickAcceptTaskOption("wuhuan-v2:accept-dialog");
                        boolean clickedAccept = acceptResult.isClicked();
                        log.info("[five-ring-v2 accept] accept dialog click result={} attempt={}/{} status={} actionKey={}",
                                clickedAccept, attempt, 2, acceptResult.getStatus(), acceptResult.getActionKey());
                        if (!clickedAccept) {
                            return TaskTransactionResult.RETRYABLE_ERROR;
                        }

                        /*
                         * “今日次数已完”只会在接任务点击后返回，必须在这里判断；已有任务
                         * 对话仍留给左侧任务追踪缺失后的兜底分支处理。
                         */
                        if (isFiveRingDailyLimitStoryVisible("wuhuan-v2:accept-dialog-daily-limit-story")) {
                            dialogService.handleDialog(DialogHandleRequest.clickStory("wuhuan-v2:daily-limit-story-close"));
                            acceptFlowResult.set(AcceptDialogPathingResult.TASK_ALREADY_FINISHED);
                            return TaskTransactionResult.READY_TO_CONTINUE;
                        }
                        acceptVerified.set(true);
                        acceptFlowResult.set(AcceptDialogPathingResult.TASK_ACCEPTED_NEEDS_SYNC);
                        return TaskTransactionResult.READY_TO_CONTINUE;
                    }

                    log.warn("[five-ring-v2 accept] accept option remained visible after retries; treat as not accepted");
                    return TaskTransactionResult.RETRYABLE_ERROR;
                });
        if (!outcome.completed()) {
            return AcceptDialogPathingResult.NOT_ACCEPTED;
        }
        log.info("[five-ring-v2 accept] accept flow finished without P2/P1: source={} txResult={} verified={}",
                movementSource, outcome.result(), acceptVerified.get());
        if (acceptFlowResult.get() == AcceptDialogPathingResult.TASK_ALREADY_FINISHED) {
            return AcceptDialogPathingResult.TASK_ALREADY_FINISHED;
        }
        /*
         * Clicking the 五环 accept option only hands control to tracker sync. The round is not
         * confirmed until the left 五环 tracker title is visible. Runner not-ready / no-green /
         * no-link statuses are scheduling/click facts, not task-existence facts.
         */
        return acceptVerified.get()
                ? AcceptDialogPathingResult.TASK_ACCEPTED_NEEDS_SYNC
                : AcceptDialogPathingResult.NOT_ACCEPTED;
    }

    private DialogResult clickAcceptTaskOption(String source) {
        return dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
                source,
                List.of(new GreenTemplateClickSpec(OPTION_ACCEPT_TASK, ACCEPT_OPTION_TEMPLATE, 20, 20, 4)),
                true));
    }

    private boolean clickInitialNpcForAccept(TaskExecutionContext context) {
        TaskTransactionOutcome outcome = taskTransactionRunner.run(
                "wuhuan-v2:clickInitialNpcForAccept",
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
                    TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
                    boolean clicked = npcClickService.clickNpcSmart(fiveRingAcceptNpc().toClickRequest(gameContext.getMe(), TaskType.WUHuan_V2));
                    return clicked ? TaskTransactionResult.READY_TO_CONTINUE : TaskTransactionResult.RETRYABLE_ERROR;
                });
        if (outcome.result() == TaskTransactionResult.STOPPED) {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        }
        return outcome.reachedExpectedResult();
    }

    private DialogResultStatus tryGiveItemAndTriggerPathingIfPossible(TaskExecutionContext context,
                                                                      FiveRingPhaseContext state) {
        String currentMap = currentObservedMapName();
        if (!isGiveItemMap(currentMap)) {
            log.info("[five-ring-v2] skip give-item dialog handling outside give-item map: currentMap={} giveMap={} phase={} source={}",
                    currentMap, GIVE_ITEM_MAP_NAME, state.phase(), state.source());
            return DialogResultStatus.NO_DIALOG;
        }
        DialogResultStatus result = dialogService.handleDialog(DialogHandleRequest.giveItemIfAvailable(
                "wuhuan-v2:give-item", KEY_ITEM_NAME, state.shoeBagIndex())).getStatus();
        if (result == DialogResultStatus.GIVE_ITEM_DONE) {
            log.info("[five-ring-v2] give item done; next phase will read left tracker instead of P2/P1");
        }
        return result;
    }

    private String currentObservedMapName() {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowPathingSnapshot snapshot = runtime == null ? null : runtime.getPathingSnapshot();
        if (snapshot != null && snapshot.getCurrentMapName() != null && !snapshot.getCurrentMapName().isBlank()) {
            return snapshot.getCurrentMapName();
        }
        PlayerCharacter me = gameContext.getMe();
        return me == null ? null : me.getCurrentMapName();
    }

    private boolean isGiveItemMap(String mapName) {
        return GIVE_ITEM_MAP_NAME.equals(mapName);
    }

    private FiveRingStepOutcome resolveFiveRingCompletionStoryOutcome(FiveRingPhaseContext state,
                                                                      String source,
                                                                      String contextMessage) {
        FiveRingCompletionPolicy.Decision decision = resolveFiveRingCompletionStory(state.round(), source);
        if (decision == FiveRingCompletionPolicy.Decision.STOP_ALL_RUNS) {
            return FiveRingStepOutcome.finishedTerminal(
                    state,
                    "five-ring terminal completion story visible " + contextMessage);
        }
        if (decision == FiveRingCompletionPolicy.Decision.FINISH_CURRENT_RUN) {
            return FiveRingStepOutcome.finished(
                    state,
                    "five-ring once completion story visible " + contextMessage);
        }
        return null;
    }

    private FiveRingCompletionPolicy.Decision resolveFiveRingCompletionStory(int currentRound, String source) {
        boolean finalTemplateVisible = isFiveRingWhiteStoryTemplateVisible(
                source + ":finished",
                "wuhuan.finished",
                FINISHED_STORY_TEMPLATE,
                "terminal completion");
        if (finalTemplateVisible) {
            return FiveRingCompletionPolicy.decide(botProperties.getWuhuanMaxRuns(), currentRound, true, false);
        }
        boolean onceTemplateVisible = isFiveRingWhiteStoryTemplateVisible(
                source + ":finished-once",
                "wuhuan.finishedOnce",
                FINISHED_ONCE_STORY_TEMPLATE,
                "once completion");
        FiveRingCompletionPolicy.Decision decision = FiveRingCompletionPolicy.decide(
                botProperties.getWuhuanMaxRuns(), currentRound, false, onceTemplateVisible);
        if (decision != FiveRingCompletionPolicy.Decision.NO_MATCH) {
            log.info("[five-ring-v2 finish] once completion policy: source={} configuredRuns={} currentRound={} decision={}",
                    source, botProperties.getWuhuanMaxRuns(), currentRound, decision);
        }
        return decision;
    }

    private boolean isFiveRingWhiteStoryTemplateVisible(String source,
                                                       String actionKey,
                                                       String templatePath,
                                                       String label) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyWhiteTemplate(
                source,
                actionKey,
                templatePath));
        if (result.getStatus() == DialogResultStatus.WHITE_TEMPLATE_VISIBLE) {
            log.info("[five-ring-v2 finish] {} story visible: source={} actionKey={} template={} point=({}, {})",
                    label, source, result.getActionKey(), templatePath, result.getAbsoluteX(), result.getAbsoluteY());
            return true;
        }
        return false;
    }

    private boolean isFiveRingDailyLimitStoryVisible(String source) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyWhiteTemplate(
                source + ":daily-limit",
                "wuhuan.dailyLimit",
                DAILY_LIMIT_STORY_TEMPLATE));
        if (result.getStatus() == DialogResultStatus.WHITE_TEMPLATE_VISIBLE) {
            log.info("[five-ring-v2 finish] daily-limit story visible: source={} actionKey={} point=({}, {})",
                    source, result.getActionKey(), result.getAbsoluteX(), result.getAbsoluteY());
            return true;
        }
        return false;
    }

    private TrackerPathingAttempt tryClickWuhuanTrackerLink(TaskExecutionContext context,
                                                            FiveRingPhaseContext state,
                                                            String source,
                                                            boolean allowAnchorSearch) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (clickPreparedWuhuanTrackerGreen(context, source)) {
            FiveRingPhaseContext trackerState = mergeWuhuanTrackerRegionsFromRuntimeCache(
                    state, runtime, "prepared-tracker-link-clicked:" + source);
            return new TrackerPathingAttempt(
                    TrackerPathingStatus.PATHING_STARTED,
                    trackerState.withTaskAccepted("prepared-tracker-link-clicked")
                            .next(FiveRingPhase.WAIT_PATHING, "prepared-tracker-pathing-started")
                            .withNewWatcherPathingStarted(
                                    "prepared-tracker-pathing-started",
                                    trackerPathingIntentSource(source, true)));
        }
        if (runtime != null) {
            TaskTrackerPanelNegativeResult negative = runtime.consumeFreshTaskTrackerPanelNegativeResult(
                    TaskType.WUHuan_V2, "wuhuan", TRACKER_NEGATIVE_MAX_AGE_MS,
                    "wuhuan-v2:tracker-negative:" + source,
                    DialogOperation.TASK_TRACKER_PATHING, "wuhuan",
                    PREPARED_TRACKER_ACTION_MAX_AGE_MS);
            if (negative != null) {
                TrackerPathingStatus status = mapTrackerNegativeStatus(negative);
                FiveRingPhaseContext trackerState = mergeWuhuanTrackerRegionsFromNegative(
                        state, negative, "runner-tracker-negative:" + source);
                log.info("[five-ring-v2 tracker] consume fresh Runner tracker negative: source={} status={} mappedStatus={} reason={} sequence={} trackerPanelRegion={} wuhuanBlockRegion={}",
                        source, negative.getStatus(), status, negative.getReason(), negative.getSequence(),
                        negative.getTrackerPanelRegion() == null ? null : negative.getTrackerPanelRegion().toShortText(),
                        negative.getWuhuanTrackerBlockRegion() == null ? null : negative.getWuhuanTrackerBlockRegion().toShortText());
                return new TrackerPathingAttempt(status, trackerState);
            }
        }
        log.info("[five-ring-v2 tracker] no fresh Runner prepared tracker action yet; source={} allowAnchorSearch={}",
                source, allowAnchorSearch);
        return new TrackerPathingAttempt(TrackerPathingStatus.RUNNER_PREPARED_NOT_READY, state);
    }

    private TrackerPathingStatus mapTrackerNegativeStatus(TaskTrackerPanelNegativeResult negative) {
        if (negative == null || negative.getStatus() == null) {
            return TrackerPathingStatus.RUNNER_PREPARED_NOT_READY;
        }
        return switch (negative.getStatus()) {
            case TASK_FOUND_NO_GREEN -> TrackerPathingStatus.TASK_FOUND_NO_GREEN;
            case TASK_FOUND_NO_LINK -> TrackerPathingStatus.TASK_FOUND_NO_LINK;
            case TASK_NOT_FOUND -> TrackerPathingStatus.TASK_NOT_FOUND;
        };
    }

    private boolean clickPreparedWuhuanTrackerGreen(TaskExecutionContext context, String source) {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return false;
        }
        PreparedDialogAction action = runtime.getPreparedDialogAction();
        if (action == null || !action.matches(DialogOperation.TASK_TRACKER_PATHING, "wuhuan")) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (!action.verifiedWithin(now, PREPARED_TRACKER_ACTION_MAX_AGE_MS)) {
            runtime.clearPreparedDialogAction("stale wuhuan tracker panel action");
            log.info("[five-ring-v2 tracker] prepared panel action stale, fallback to live scan: source={} ageMs={} click=({}, {})",
                    source, Math.max(0L, now - action.getLastVerifiedAtMs()), action.getAbsoluteX(), action.getAbsoluteY());
            return false;
        }

        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        log.info("[five-ring-v2 tracker] click prepared panel green link: source={} preparedSource={} matched={} click=({}, {})",
                source, action.getSource(), action.getMatchedText(), action.getAbsoluteX(), action.getAbsoluteY());
        boolean clicked = inputSequences.submitAndWait("wuhuan-v2:prepared-tracker-panel-click:" + safeFileToken(source), List.of(
                InputAction.moveMouse(action.getAbsoluteX(), action.getAbsoluteY()),
                InputAction.sleep(120),
                InputAction.clickLeft(action.getAbsoluteX(), action.getAbsoluteY(), 300)
        ));
        if (!clicked) {
            return false;
        }
        runtime.clearPreparedDialogAction("wuhuan tracker panel action handled");
        String intentSource = trackerPathingIntentSource(source, true);
        gameStateUtil.recordMovementIntent(intentSource);
        registerTrackerPathingIntent(intentSource);
        log.info("[five-ring-v2 tracker] prepared panel click submitted; hand off to WAIT_PATHING without foreground edge probe: source={} click=({}, {})",
                source, action.getAbsoluteX(), action.getAbsoluteY());
        return true;
    }

    private String trackerPathingIntentSource(String source, boolean prepared) {
        return (prepared ? PREPARED_TRACKER_PANEL_PATHING_SOURCE_PREFIX : TRACKER_GREEN_CLICK_PATHING_SOURCE_PREFIX)
                + safeFileToken(source);
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
            log.info("[five-ring-v2 tracker] window pathing intent registered for tracker click: windowId={} source={}",
                    runtime.getWindowId(), intentSource);
        });
    }

    private String safeFileToken(String source) {
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        return source.replaceAll("[^a-zA-Z0-9._-]", "_");
    }


    private void cleanupUnexpectedAcceptDialog(String reason) {
        taskTransactionRunner.run(
                "wuhuan-v2:cleanupUnexpectedAcceptDialog",
                TaskTransactionResult.RETRYABLE_ERROR,
                TaskYieldPolicy.RETRY_LATER,
                () -> {
                    uiCleanerService.cleanUpAll();
                    return TaskTransactionResult.RETRYABLE_ERROR;
                });
        log.info("[five-ring-v2 accept] unexpected accept dialog cleanup finished: reason={}", reason);
    }

    private void cleanupRetryableDialog(String reason) {
        taskTransactionRunner.run(
                "wuhuan-v2:cleanupRetryableDialog",
                TaskTransactionResult.RETRYABLE_ERROR,
                TaskYieldPolicy.RETRY_LATER,
                () -> {
                    uiCleanerService.cleanUpAll();
                    return TaskTransactionResult.RETRYABLE_ERROR;
                });
        log.info("[five-ring-v2] retryable dialog cleanup finished: reason={}", reason);
    }

    private void yieldAfterMustYield(TaskExecutionContext context, FiveRingStepOutcome outcome) {
        long delayMs = handoffDelayMs(outcome);
        log.info("[five-ring-v2] yield after phase: result={} next={} delayMs={}",
                outcome.transactionResult(), outcome.nextState().phase(), delayMs);
        TaskSleep.sleepOrStop(context, delayMs, "Five-ring V2 task interrupted");
    }

    private long handoffDelayMs(FiveRingStepOutcome outcome) {
        if ("prepared action priority yield".equals(outcome.message())
                || "pathing terminal priority yield".equals(outcome.message())) {
            return READY_EVENT_PRIORITY_YIELD_DELAY_MS;
        }
        /*
         * Some pathing handoffs deliberately stay in their current phase, for example accept-NPC
         * navigation keeps ACCEPT_TASK while the watcher proves arrival. Treat the transaction result,
         * not only the next phase, as the handoff signal so those paths match the stress-test model.
         */
        if (outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED
                || outcome.nextState().phase() == FiveRingPhase.WAIT_PATHING) {
            return PATHING_HANDOFF_DELAY_MS;
        }
        return TASK_TURN_HANDOFF_DELAY_MS;
    }

    private static NpcTarget fiveRingAcceptNpc() {
        return NpcTarget.builder()
                .key("wuhuan.acceptNpc")
                .mapName(TARGET_MAP_NAME)
                .name(TARGET_NPC_NAME)
                .x(NPC_COOR_X)
                .y(NPC_COOR_Y)
                .role(NpcRole.QUEST_GIVER)
                .movementType(NpcMovementType.FIXED)
                .expectedDialogTemplatePath(ACCEPT_OPTION_TEMPLATE)
                .source("five-ring-v2")
                .build();
    }

    private static NpcTarget shoeShopOwnerNpc() {
        return NpcTarget.builder()
                .key("wuhuan.shoeShopOwner")
                .mapName(SHOE_SHOP_MAP_NAME)
                .name(SHOE_SHOP_OWNER_NAME)
                .x(SHOE_SHOP_OWNER_X)
                .y(SHOE_SHOP_OWNER_Y)
                .role(NpcRole.INTERACTION_TARGET)
                .movementType(NpcMovementType.FIXED)
                .tooltipType(NpcTooltipType.NONE)
                .expectedDialogTemplatePath(SHOE_SHOP_BUY_OPTION_TEMPLATE)
                .source("five-ring-v2:shoe-shop")
                .build();
    }

    private record AcceptSetupPositionPrewarm(LocationInfo location, long capturedAtMs, String source) {
    }

    private enum TrackerPathingStatus {
        PATHING_STARTED,
        RUNNER_PREPARED_NOT_READY,
        TASK_FOUND_NO_GREEN,
        TASK_FOUND_NO_LINK,
        TASK_NOT_FOUND,
        TRACKER_UNAVAILABLE,
        CLICK_FAILED
    }

    private record WuhuanTrackerTitleGate(boolean titleVisible, FiveRingPhaseContext state) {
    }

    private static final class TrackerPathingAttempt {
        private final TrackerPathingStatus status;
        private final FiveRingPhaseContext state;

        private TrackerPathingAttempt(TrackerPathingStatus status, FiveRingPhaseContext state) {
            this.status = status;
            this.state = state;
        }
    }


    private enum AcceptDialogPathingResult {
        NOT_ACCEPTED,
        TASK_ACCEPTED_NEEDS_SYNC,
        TASK_ALREADY_FINISHED
    }

    private record FiveRingRunResult(TaskRunResult taskResult, boolean terminalTask) {
    }

    private record FiveRingSupplyCheck(boolean incenseRefilled, Integer shoeBagIndex, int shoeCount) {
    }

    private TaskExecutionContext resolveExecutionContext(TaskExecutionContext executionContext) {
        return executionContext == null ? buildExecutionContext() : executionContext;
    }

    private TaskExecutionContext buildExecutionContext() {
        return TaskExecutionContext.builder()
                .taskCode(getTaskCode())
                .taskName(getTaskName())
                .retryPolicy(TaskRetryPolicy.none())
                .startedAt(LocalDateTime.now())
                .build();
    }

    private boolean shouldStartNextRun(int maxRuns, int completedRuns) {
        return isUnlimitedRuns(maxRuns) || completedRuns < maxRuns;
    }

    private boolean isUnlimitedRuns(int maxRuns) {
        return maxRuns <= 0;
    }

    private void markTaskFailed() {
        gameContext.setBotStatus(GameContext.BotStatus.ERROR);
    }

    private void markTaskIdle() {
        gameContext.setBotStatus(GameContext.BotStatus.IDLE);
        gameContext.setCurrentActionState(GameContext.ActionState.FREE);
    }
}
