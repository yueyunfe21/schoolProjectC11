package com.bot.dhxy.task.wuhuan;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
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
import com.bot.dhxy.runner.context.TaskExecutionContext;
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
import com.bot.dhxy.service.QuestManagerService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.task.startup.TaskStartupCheckResult;
import com.bot.dhxy.task.startup.TaskStartupCheckService;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
    private static final String QUEST_PANEL_TASK_CODE = "wuhuan";
    private static final String TASK_NAME = "五环V2";
    private static final String TARGET_MAP_NAME = "长安";
    private static final String TARGET_NPC_NAME = "云游大师";
    private static final int NPC_COOR_X = 87;
    private static final int NPC_COOR_Y = 174;
    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/wuhuan/wuhuan_accept_first_option.png";
    private static final String TRACKER_ANCHOR_TEMPLATE = "images/template/task/wubei_tracker_anchor.png";
    private static final String KEY_ITEM_NAME = "wuhuan/shoe.png";
    private static final String QUICK_SHOE_ANCHOR_TEMPLATE = "images/template/wuhuan/wuhuan_quick_shoe_anchor.png";
    private static final String QUICK_SHOE_FAST_ITEM_TEMPLATE = "images/template/fastItem/wuhuan_quick_shoe_shop_item.png";
    private static final String SHOE_SHOP_BUY_OPTION_TEMPLATE = "images/template/dialog/wuhuan/wuhuan_shop_buy_option.png";
    private static final String SHOE_SHOP_SHOE_TEMPLATE = "images/template/wuhuan/shoe.png";
    private static final String SHOE_SHOP_BUY_BUTTON_TEMPLATE = "images/template/wuhuan/wuhuan_buy_button.png";
    private static final int TASK_NPC_DIRECT_CLICK_DISTANCE = 10;
    private static final String SHOE_SHOP_ENTRY_TARGET_NAME = "牛记布店入口";
    private static final String SHOE_SHOP_MAP_NAME = "牛记布店";
    private static final String SHOE_SHOP_RETURN_FALLBACK_MAP_NAME = "洛阳城";
    private static final String SHOE_SHOP_RETURN_FALLBACK_NPC_NAME = "李道宗";
    private static final int SHOE_SHOP_RETURN_FALLBACK_NPC_X = 324;
    private static final int SHOE_SHOP_RETURN_FALLBACK_NPC_Y = 109;
    private static final String SHOE_SHOP_OWNER_NAME = "服装店老板";
    private static final int SHOE_SHOP_ENTRY_X = 130;
    private static final int SHOE_SHOP_ENTRY_Y = 130;
    private static final int SHOE_SHOP_OWNER_X = 13;
    private static final int SHOE_SHOP_OWNER_Y = 9;
    private static final int SHOE_SHOP_ITEM_REL_LEFT = 364;
    private static final int SHOE_SHOP_ITEM_REL_TOP = 253;
    private static final int SHOE_SHOP_ITEM_REL_RIGHT = 672;
    private static final int SHOE_SHOP_ITEM_REL_BOTTOM = 458;
    private static final int SHOE_SHOP_RETURN_REL_X = 364;
    private static final int SHOE_SHOP_RETURN_REL_Y = 554;
    private static final int SHOE_SHOP_ENTRY_MAX_ATTEMPTS = 3;
    private static final int SHOE_SHOP_BUY_PHASE_MAX_RETRIES = 8;
    private static final long SHOE_SHOP_ENTRY_CONFIRM_TIMEOUT_MS = 10_000L;
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
    private static final int QUICK_SHOE_FAST_ITEM_REL_LEFT = 880;
    private static final int QUICK_SHOE_FAST_ITEM_REL_TOP = 700;
    private static final int QUICK_SHOE_FAST_ITEM_REL_RIGHT = 1000;
    private static final int QUICK_SHOE_FAST_ITEM_REL_BOTTOM = 742;
    private static final int QUICK_SHOE_SHOP_ITEM_REL_LEFT = 365;
    private static final int QUICK_SHOE_SHOP_ITEM_REL_TOP = 250;
    private static final int QUICK_SHOE_SHOP_ITEM_REL_RIGHT = 673;
    private static final int QUICK_SHOE_SHOP_ITEM_REL_BOTTOM = 454;
    private static final int QUICK_SHOE_BUY_BUTTON_REL_X = 623;
    private static final int QUICK_SHOE_BUY_BUTTON_REL_Y = 534;
    private static final int MAX_ACCEPT_RETRY = 5;
    private static final int MAX_UI_ERROR_BEFORE_CLEANUP = 3;
    private static final int MAX_PATHING_START_CONFIRM_ATTEMPTS = 1;
    private static final int MAX_PATHING_WEAK_MOVEMENT_CONFIRM_ATTEMPTS = 2;
    private static final int MAX_PHASE_LOOP_GUARD = 80;
    private static final long TASK_TURN_HANDOFF_DELAY_MS = 900L;
    private static final long STORY_IGNORED_COMBAT_CHECK_DELAY_MS = 1_500L;
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int TRACKER_ANCHOR_SEARCH_REL_LEFT = 6;
    private static final int TRACKER_ANCHOR_SEARCH_REL_TOP = 196;
    private static final int TRACKER_ANCHOR_SEARCH_REL_RIGHT = 207;
    private static final int TRACKER_ANCHOR_SEARCH_REL_BOTTOM = 551;
    private static final int TRACKER_PANEL_FROM_ANCHOR_LEFT = -96;
    private static final int TRACKER_PANEL_FROM_ANCHOR_TOP = 12;
    private static final int TRACKER_PANEL_FROM_ANCHOR_RIGHT = 86;
    private static final int TRACKER_PANEL_HEIGHT = 200;
    private static final int WUHUAN_TRACKER_BLOCK_HEIGHT = 60;
    private static final int WUHUAN_TITLE_CENTER_FALLBACK_LEFT_SHIFT = 24;
    private static final int TRACKER_LINK_MIN_PIXELS = 20;
    private static final int TRACKER_LINK_SINGLE_MAX_WIDTH = 72;
    private static final int TRACKER_LINK_SPLIT_GAP = 8;
    private static final int TRACKER_LINK_DELIMITER_MAX_WIDTH = 5;
    private static final int TRACKER_LINK_DELIMITER_MAX_PIXELS = 18;
    private static final double TRACKER_ANCHOR_THRESHOLD = 0.82;

    private final BotProperties botProperties;
    private final GameContext gameContext;
    private final NavigationService navigationService;
    private final NpcClickService npcClickService;
    private final DialogService dialogService;
    private final PlayerStateService playerStateService;
    private final QuestManagerService questManager;
    private final AutoCombatService autoCombatService;
    private final BagService bagService;
    private final GameStateUtil gameStateUtil;
    private final UICleanerService uiCleanerService;
    private final TaskStartupCheckService taskStartupCheckService;
    private final TaskTransactionRunner taskTransactionRunner;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final TextRecognizer textRecognizer;
    private final InputSequences inputSequences;

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
            while (shouldStartNextRun(maxRuns, completedRuns)) {
                TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
                int round = completedRuns + 1;
                FiveRingPhaseContext phaseContext = FiveRingPhaseContext.start(round);
                log.info("[five-ring-v2] run {} initial phase: phase={} source={}",
                        round, phaseContext.phase(), phaseContext.source());

                TaskRunResult runResult = runPhases(context, phaseContext);
                if (runResult != TaskRunResult.SUCCESS) {
                    gameContext.setBotStatus(runResult == TaskRunResult.STOPPED
                            ? GameContext.BotStatus.IDLE
                            : GameContext.BotStatus.ERROR);
                    return runResult;
                }

                completedRuns++;
                log.info("[five-ring-v2] run {} finished, completed={}", round, completedRuns);
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

    private TaskRunResult runPhases(TaskExecutionContext context, FiveRingPhaseContext initialContext) {
        FiveRingPhaseContext phaseContext = initialContext;
        int phaseLoopGuard = 0;

        while (!phaseContext.phase().isTerminal()) {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

            FiveRingPhaseContext currentContext = phaseContext;
            AtomicReference<FiveRingStepOutcome> phaseOutcome = new AtomicReference<>();
            /*
             * PREPARE -> HANDOVER_DETECT -> ACCEPT_TASK is one startup chain for the same window.
             * Keep the task turn while phases return READY_TO_CONTINUE, then release only when a
             * real shared state appears, such as pathing/combat/waiting. Otherwise five windows
             * interleave bag checks and task-panel probes before any one character starts moving.
             */
            TaskTransactionOutcome transaction = taskTransactionRunner.run(
                    "wuhuan-v2:" + currentContext.phase(),
                    TaskTransactionResult.READY_TO_CONTINUE,
                    TaskYieldPolicy.CONTINUE_CHAIN,
                    () -> {
                        FiveRingStepOutcome outcome = runPhase(context, currentContext);
                        phaseOutcome.set(outcome);
                        return outcome.transactionResult();
                    });

            FiveRingStepOutcome outcome = phaseOutcome.get();
            if (outcome == null) {
                outcome = FiveRingStepOutcome.failed(currentContext, "phase produced no outcome");
            }
            log.info("[five-ring-v2] phase outcome: round={} phase={} result={} yield={} next={} accepted={} message={}",
                    currentContext.round(), currentContext.phase(), outcome.transactionResult(),
                    outcome.yieldPolicy(), outcome.nextState().phase(), outcome.nextState().taskAccepted(),
                    outcome.message());

            if (transaction.result() == TaskTransactionResult.STOPPED
                    || outcome.transactionResult() == TaskTransactionResult.STOPPED) {
                return TaskRunResult.STOPPED;
            }
            if (outcome.transactionResult() == TaskTransactionResult.FAILED) {
                return TaskRunResult.FAILED;
            }
            if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD) {
                yieldAfterMustYield(context, outcome);
            }
            if (outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED
                    || outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
                phaseLoopGuard = 0;
            } else if (++phaseLoopGuard > MAX_PHASE_LOOP_GUARD) {
                log.error("[five-ring-v2] phase loop guard exceeded: round={} phase={} source={}",
                        currentContext.round(), currentContext.phase(), currentContext.source());
                return TaskRunResult.FAILED;
            }

            phaseContext = outcome.nextState();
        }

        if (phaseContext.phase() == FiveRingPhase.STOPPED) {
            return TaskRunResult.STOPPED;
        }
        return phaseContext.phase() == FiveRingPhase.FAILED ? TaskRunResult.FAILED : TaskRunResult.SUCCESS;
    }

    private FiveRingStepOutcome runPhase(TaskExecutionContext context, FiveRingPhaseContext state) {
        return switch (state.phase()) {
            case PREPARE -> prepare(context, state);
            case BUY_SHOES -> buyShoes(context, state);
            case HANDOVER_DETECT -> detectHandover(context, state);
            case ACCEPT_TASK -> acceptTask(context, state);
            case WAIT_PATHING -> waitPathing(context, state);
            case CHECK_COMBAT -> checkCombat(context, state);
            case HANDLE_DIALOG -> handleDialog(context, state);
            case SYNC_TASK_PANEL -> syncTaskPanel(context, state, true);
            case FINISHED, FAILED, STOPPED ->
                    FiveRingStepOutcome.failed(state, "terminal phase should not be executed: " + state.phase());
        };
    }

    private FiveRingStepOutcome prepare(TaskExecutionContext context, FiveRingPhaseContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        log.info("[five-ring-v2 prepare-1] clean maps/dialogs/common windows");
        gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        uiCleanerService.cleanUpAll();
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        log.info("[five-ring-v2 prepare-2] startup first-aid check before bag supply scan");
        playerStateService.performStartupFirstAidCheck(context);
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        log.info("[five-ring-v2 prepare-3] check sheyaoxiang and shoe in one bag session");
        FiveRingSupplyCheck supplyCheck = checkFiveRingSuppliesInOneBagSession(context);
        Integer shoeBagIndex = supplyCheck == null ? null : supplyCheck.shoeBagIndex();
        if (shoeBagIndex != null) {
            log.info("[five-ring-v2 prepare done] shoe found on page {}, ready to give", shoeBagIndex + 1);
        } else {
            log.warn("[five-ring-v2 prepare] shoe template not found; try quick-buy shoe from fast item panel");
            boolean boughtShoes = quickBuyShoe(context);
            if (!boughtShoes) {
                log.warn("[five-ring-v2 prepare] quick-buy shoe failed; fall back to shop-owner buy flow");
                return FiveRingStepOutcome.continueTo(
                        state.next(FiveRingPhase.BUY_SHOES, "prepare-shoe-shop-required"),
                        "shoe missing; buy through shop-owner flow");
            }
            shoeBagIndex = boughtShoes ? bagService.findItemPageIndex(BagService.MAIN_BAG, KEY_ITEM_NAME, context) : null;
            log.info("[five-ring-v2 prepare] quick-buy shoe result={} verifiedPage={}",
                    boughtShoes, shoeBagIndex == null ? "none" : shoeBagIndex + 1);
        }

        return FiveRingStepOutcome.continueTo(
                state.withShoeBagIndex(shoeBagIndex, "prepare-done")
                        .next(FiveRingPhase.HANDOVER_DETECT, "prepare-done"),
                "prepare finished");
    }

    private FiveRingStepOutcome buyShoes(TaskExecutionContext context, FiveRingPhaseContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        if (state.phaseRetryCount() > SHOE_SHOP_BUY_PHASE_MAX_RETRIES) {
            return FiveRingStepOutcome.failed(state, "shoe-shop buy phase exceeded retries");
        }

        if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, 0L,
                "wuhuan-v2:shoe-shop-phase-already-inside")) {
            log.info("[five-ring-v2 shoe-shop] already inside {}, buy from shop owner", SHOE_SHOP_MAP_NAME);
            if (!buyShoeFromShopOwnerWithRetry(context)) {
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
                            .next(FiveRingPhase.HANDOVER_DETECT, "shoe-shop-bought"),
                    "shoe-shop buy finished");
        }

        GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        if (isShoeShopEntryMovement(movementState)) {
            log.info("[five-ring-v2 shoe-shop] entry pathing still active: state={}", movementState);
            return FiveRingStepOutcome.pathingStarted(
                    state.retrySamePhase("shoe-shop-entry-still-pathing"),
                    "shoe-shop entry still pathing");
        }

        /*
         * After the first exact 130,130 click, the character may stop at the shop door while still
         * mounted. Ctrl+C dismount can trigger the entrance without another mini-map click, so do
         * this before retrying the coordinate and then re-confirm the map.
         */
        if (state.phaseRetryCount() > 0) {
            boolean submitted = inputSequences.pressCtrlC("wuhuan-v2:shoe-shop-entry-dismount-before-retry");
            log.info("[five-ring-v2 shoe-shop] dismount before entry retry: retry={} submitted={}",
                    state.phaseRetryCount(), submitted);
            TaskSleep.sleepOrStop(context, SHOE_SHOP_DISMOUNT_SETTLE_MS, "Five-ring V2 task interrupted");
            if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, 0L,
                    "wuhuan-v2:shoe-shop-phase-after-dismount")) {
                return FiveRingStepOutcome.continueTo(
                        state.next(FiveRingPhase.BUY_SHOES, "shoe-shop-entered-after-dismount"),
                        "shoe-shop entered after dismount");
            }
        }

        NavigationResult result = clickShoeShopEntryExact(context, true);
        NavigationResultStatus status = result.getStatus();
        log.info("[five-ring-v2 shoe-shop] entry exact navigation result: status={} message={} retry={}/{}",
                status, result.getMessage(), state.phaseRetryCount(), SHOE_SHOP_BUY_PHASE_MAX_RETRIES);
        if (status == NavigationResultStatus.STOPPED) {
            return FiveRingStepOutcome.stopped(state, "shoe-shop entry navigation stopped");
        }
        if (status == NavigationResultStatus.PATHING_STARTED || result.success()) {
            return FiveRingStepOutcome.pathingStarted(
                    state.retrySamePhase("shoe-shop-entry-clicked"),
                    "shoe-shop entry pathing started");
        }
        if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, 0L,
                "wuhuan-v2:shoe-shop-phase-after-entry-failure")) {
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.BUY_SHOES, "shoe-shop-entry-confirmed-after-failure"),
                    "shoe-shop entry confirmed after input failure");
        }
        return FiveRingStepOutcome.sharedState(
                state.retrySamePhase("shoe-shop-entry-retry"),
                "shoe-shop entry click failed; retry later");
    }

    /**
     * Run the 五环 startup inventory checks with one main-bag open/close cycle.
     *
     * <p>摄妖香 and shoe detection both operate on the main bag. Keeping them in the same exclusive
     * input section avoids the old visual churn where Alt+E opened/closed the bag for incense and
     * immediately opened/closed it again for the shoe pre-scan.</p>
     *
     * @param context optional stop token for the current window task.
     * @return combined startup supply check result, or null if the bag could not be opened.
     */
    private FiveRingSupplyCheck checkFiveRingSuppliesInOneBagSession(TaskExecutionContext context) {
        return bagService.withMainBagOpen("wuhuan-v2:prepare-supplies", context, mainBag -> {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
            boolean incenseRefilled = playerStateService.ensureSheYaoXiangActiveInOpenMainBag(mainBag, context);
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

            log.info("[five-ring-v2 prepare] scan shoe template in already-open main bag: {}", KEY_ITEM_NAME);
            Integer shoeBagIndex = mainBag.findItemPageIndex(KEY_ITEM_NAME);
            return new FiveRingSupplyCheck(incenseRefilled, shoeBagIndex);
        });
    }

    /**
     * Buy one 五环 shoe through the user-prepared shortcut item panel.
     *
     * <p>This path is only entered after the normal one-bag startup scan cannot find shoes. All
     * coordinates are window-relative values measured from the user's current 1024x768 client base.
     * The method does not navigate: it opens the shortcut shop from the bottom-right fast item slot,
     * chooses the shoe, clicks purchase, and closes the shop so the caller can rescan the bag.</p>
     *
     * @param context current task stop token; nullable only for legacy direct task execution.
     * @return true when the shortcut panel, shoe template, and buy click all completed; false when the
     *         shortcut is not available or a template is missing.
     */
    private boolean quickBuyShoe(TaskExecutionContext context) {
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
        if (!rightClickTemplateCenter(context, QUICK_SHOE_FAST_ITEM_TEMPLATE, fastItemRect, 0.80,
                "wuhuan-v2:quick-buy-shoe:open-shop")) {
            return false;
        }
        TaskSleep.sleepOrStop(context, 500, "Five-ring V2 task interrupted");

        int[] itemRect = windowRelativeRect(QUICK_SHOE_SHOP_ITEM_REL_LEFT, QUICK_SHOE_SHOP_ITEM_REL_TOP,
                QUICK_SHOE_SHOP_ITEM_REL_RIGHT, QUICK_SHOE_SHOP_ITEM_REL_BOTTOM);
        if (!clickTemplateCenterInRect(context, SHOE_SHOP_SHOE_TEMPLATE, itemRect, 0.82,
                "wuhuan-v2:quick-buy-shoe:select-shoe")) {
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

    private boolean goShoeShopAndBuyShoes(TaskExecutionContext context) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        log.info("[five-ring-v2 shoe-shop] start: navigate to {} ({}, {})",
                SHOE_SHOP_ENTRY_TARGET_NAME, SHOE_SHOP_ENTRY_X, SHOE_SHOP_ENTRY_Y);

        if (!enterShoeShopWithRetry(context)) {
            return false;
        }
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        if (!buyShoeFromShopOwnerWithRetry(context)) {
            return false;
        }

        BufferedImage beforeReturn = captureShoeShopReturnSnapshot("wuhuan-v2:shoe-shop-return-before");
        try {
            return returnToChanganFromShoeShopWithRetry(context, beforeReturn);
        } finally {
            if (beforeReturn != null) {
                beforeReturn.flush();
            }
        }
    }

    private boolean enterShoeShopWithRetry(TaskExecutionContext context) {
        for (int attempt = 1; attempt <= SHOE_SHOP_ENTRY_MAX_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
            log.info("[five-ring-v2 shoe-shop] exact entry attempt {}/{}",
                    attempt, SHOE_SHOP_ENTRY_MAX_ATTEMPTS);
            if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, 0L,
                    "wuhuan-v2:shoe-shop-entry-precheck-" + attempt)) {
                log.info("[five-ring-v2 shoe-shop] already inside target shop before retry: attempt={}/{}",
                        attempt, SHOE_SHOP_ENTRY_MAX_ATTEMPTS);
                return true;
            }
            if (attempt > 1) {
                boolean submitted = inputSequences.pressCtrlC("wuhuan-v2:shoe-shop-entry-dismount-attempt-" + attempt);
                log.info("[five-ring-v2 shoe-shop] dismount before exact entry retry: attempt={}/{} submitted={}",
                        attempt, SHOE_SHOP_ENTRY_MAX_ATTEMPTS, submitted);
                TaskSleep.sleepOrStop(context, SHOE_SHOP_DISMOUNT_SETTLE_MS, "Five-ring V2 task interrupted");
                if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, 0L,
                        "wuhuan-v2:shoe-shop-entry-after-dismount-" + attempt)) {
                    log.info("[five-ring-v2 shoe-shop] entered target shop after dismount: attempt={}/{}",
                            attempt, SHOE_SHOP_ENTRY_MAX_ATTEMPTS);
                    return true;
                }
            }
            NavigationResult entryResult = clickShoeShopEntryExact(context, false);
            if (!entryResult.success()) {
                log.warn("[five-ring-v2 shoe-shop] exact entry click failed: attempt={}/{}",
                        attempt, SHOE_SHOP_ENTRY_MAX_ATTEMPTS);
                if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, 0L,
                        "wuhuan-v2:shoe-shop-entry-after-failed-click-" + attempt)) {
                    log.info("[five-ring-v2 shoe-shop] entered target shop despite failed click status: attempt={}/{} status={}",
                            attempt, SHOE_SHOP_ENTRY_MAX_ATTEMPTS, entryResult.getStatus());
                    return true;
                }
                continue;
            }

            if (gameStateUtil.confirmCurrentMapFresh(SHOE_SHOP_MAP_NAME, SHOE_SHOP_ENTRY_CONFIRM_TIMEOUT_MS,
                    "wuhuan-v2:shoe-shop-entered-attempt-" + attempt)) {
                log.info("[five-ring-v2 shoe-shop] target shop entered: attempt={}/{}",
                        attempt, SHOE_SHOP_ENTRY_MAX_ATTEMPTS);
                return true;
            }
            log.warn("[five-ring-v2 shoe-shop] shop entry not confirmed, retry exact 130,130: attempt={}/{}",
                    attempt, SHOE_SHOP_ENTRY_MAX_ATTEMPTS);
        }
        log.warn("[five-ring-v2 shoe-shop] did not enter target shop after {} attempts: expected={}",
                SHOE_SHOP_ENTRY_MAX_ATTEMPTS, SHOE_SHOP_MAP_NAME);
        return false;
    }

    private boolean buyShoeFromShopOwnerWithRetry(TaskExecutionContext context) {
        for (int attempt = 1; attempt <= SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS; attempt++) {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
            log.info("[five-ring-v2 shoe-shop] shop-owner buy flow attempt {}/{}",
                    attempt, SHOE_SHOP_BUY_FLOW_MAX_ATTEMPTS);

            // 进牛记布店后默认落在老板附近；店内没有小地图 transform，直接用 NPC 点击链路。
            uiCleanerService.closeAllGenericWindows();
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

            if (!npcClickService.clickNpcSmart(shoeShopOwnerNpc().toClickRequest(gameContext.getMe()))) {
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
                    "wuhuan-v2:shoe-shop-click-shoe")) {
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

    private NavigationResult clickShoeShopEntryExact(TaskExecutionContext context, boolean returnOnPathingStarted) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        /*
         * 牛记布店入口必须点长安小地图精确 130,130；通用 current-map navigation 会加随机和
         * 兜底逻辑，可能偏到入口旁边而不是进店。这里仍走 NavigationRequest，不暴露额外
         * NavigationService 入口；精确点击只作为本次请求的策略。
         */
        NavigationResult result = navigationService.navigateToNPC(NavigationRequest.builder()
                .targetMapName(TARGET_MAP_NAME)
                .targetX(SHOE_SHOP_ENTRY_X)
                .targetY(SHOE_SHOP_ENTRY_Y)
                .targetName(SHOE_SHOP_ENTRY_TARGET_NAME)
                .exactMiniMapClickOnly(true)
                .returnOnPathingStarted(returnOnPathingStarted)
                .source("wuhuan-v2:shoe-shop-entry-exact-130-130")
                .build());
        if (!result.success() && result.getStatus() != NavigationResultStatus.PATHING_STARTED) {
            log.warn("[five-ring-v2 shoe-shop] exact entry click request failed: status={} message={}",
                    result.getStatus(), result.getMessage());
        }
        return result;
    }

    private boolean isShoeShopEntryMovement(GameStateUtil.MovementState movementState) {
        return movementState == GameStateUtil.MovementState.MOVING
                || movementState == GameStateUtil.MovementState.PATHING_ACTIVE;
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
                .source("wuhuan-v2:shoe-shop-return-fallback-repair-npc")
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
        log.info("[five-ring-v2 shoe-shop] template matched: description={} template={} center=({}, {}) click=({}, {})",
                description, templatePath, matchedCenter.x, matchedCenter.y, clickPoint.x, clickPoint.y);
        return inputSequences.submitAndWait(description, List.of(
                InputAction.moveMouse(clickPoint.x, clickPoint.y),
                InputAction.sleep(120),
                InputAction.clickLeft(clickPoint.x, clickPoint.y, 150),
                InputAction.sleep(250)
        ));
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

    private boolean rightClickTemplateCenter(TaskExecutionContext context,
                                             String templatePath,
                                             int[] rect,
                                             double matchRate,
                                             String description) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        Point matchedCenter = coordinateHelper.findImageInRegion(templatePath, rect, matchRate);
        if (matchedCenter == null) {
            log.warn("[five-ring-v2 quick-buy-shoe] template not matched for right click: description={} template={} rect=({}, {})-({}, {})",
                    description, templatePath, rect[0], rect[1], rect[2], rect[3]);
            return false;
        }

        Point clickPoint = coordinateHelper.getRandomizedPoint(matchedCenter, 2, 2);
        log.info("[five-ring-v2 quick-buy-shoe] right click template: description={} center=({}, {}) click=({}, {})",
                description, matchedCenter.x, matchedCenter.y, clickPoint.x, clickPoint.y);
        return inputSequences.submitAndWait(description, List.of(
                InputAction.moveMouse(clickPoint.x, clickPoint.y),
                InputAction.sleep(120),
                InputAction.clickRight(clickPoint.x, clickPoint.y, 150),
                InputAction.sleep(350)
        ));
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
                    trackerAttempt.state.withTaskAccepted("handover-tracker-task-found")
                            .next(FiveRingPhase.WAIT_PATHING, "handover-tracker-pathing-started"),
                    "handover tracker pathing started");
        }
        if (trackerAttempt.status == TrackerPathingStatus.TASK_FOUND_NO_GREEN
                || trackerAttempt.status == TrackerPathingStatus.TASK_FOUND_NO_LINK) {
            log.info("[five-ring-v2 handover] tracker has 五环 but no usable green pathing link yet; status={}",
                    trackerAttempt.status);
            return FiveRingStepOutcome.continueTo(
                    trackerAttempt.state.withTaskAccepted("handover-tracker-task-found")
                            .next(FiveRingPhase.SYNC_TASK_PANEL, "handover-tracker-no-usable-link"),
                    "handover tracker needs sync");
        }
        if (trackerAttempt.status == TrackerPathingStatus.CLICK_FAILED) {
            log.warn("[five-ring-v2 handover] tracker has 五环 but green click failed; resync tracker");
            return FiveRingStepOutcome.continueTo(
                    trackerAttempt.state.withTaskAccepted("handover-tracker-click-failed")
                            .next(FiveRingPhase.SYNC_TASK_PANEL, "handover-tracker-click-failed"),
                    "handover tracker click failed");
        }
        boolean isTaskAlreadyRunning = questManager.activateTaskIfPresentExclusive(QUEST_PANEL_TASK_CODE, false);
        if (isTaskAlreadyRunning) {
            log.info("[five-ring-v2 handover] existing task found; sync task panel next");
            return FiveRingStepOutcome.continueTo(
                    state.withTaskAccepted("handover-existing-task")
                            .next(FiveRingPhase.SYNC_TASK_PANEL, "handover-existing-task"),
                    "existing task needs sync");
        }

        log.info("[five-ring-v2 handover] no running task found; initial setup is required");
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

            boolean nearAcceptNpc = isNearAcceptNpc();
            FiveRingStepOutcome currentScreenAccept = tryAcceptInitialTaskFromCurrentScreen(
                    context, activeState, "setup:current-screen");
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
                if (tryClickNearbyAcceptNpc(context)) {
                    AcceptDialogPathingResult acceptResult = acceptInitialDialogAndTriggerPathing(
                            context, "nearbyAcceptNpcPathing");
                    if (acceptResult == AcceptDialogPathingResult.TASK_ACCEPTED_NEEDS_SYNC) {
                        return FiveRingStepOutcome.continueTo(
                                activeState.withTaskAccepted("nearby-accept-clicked")
                                        .next(FiveRingPhase.SYNC_TASK_PANEL, "nearby-accept-pathing-unconfirmed"),
                                "nearby accept clicked; read tracker next");
                    }
                    cleanupUnexpectedAcceptDialog("setup:nearby-accept-template-not-matched");
                    retry++;
                    TaskSleep.sleepOrStop(context, 1000, "Five-ring V2 task interrupted");
                    continue;
                }
                NavigationResult navigationResult = navigationService.navigateToNPC(NavigationRequest.builder()
                        .targetMapName(TARGET_MAP_NAME)
                        .targetX(NPC_COOR_X)
                        .targetY(NPC_COOR_Y)
                        .targetName(TARGET_NPC_NAME)
                        .returnOnPathingStarted(true)
                        .source("wuhuan-v2:acceptNpc:navigate")
                        .build());
                NavigationResultStatus navigationStatus = navigationResult.getStatus();
                log.info("[five-ring-v2 accept] accept NPC navigation result: npc={} status={} message={}",
                        TARGET_NPC_NAME, navigationStatus, navigationResult.getMessage());
                if (navigationStatus == NavigationResultStatus.PATHING_STARTED) {
                    return FiveRingStepOutcome.pathingStarted(
                            activeState.waitForAcceptNpcPathing("accept-npc-navigation-pathing"),
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

                /*
                 * Current-map navigation can leave the mini-map/search panel visible after route
                 * retries. Close only generic X-button windows before smart-clicking 云游大师, so
                 * the NPC click is not blocked by map UI.
                 */
                uiCleanerService.closeAllGenericWindows();
                TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

                if (!clickInitialNpcForAccept(context)) {
                    TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
                    FiveRingStepOutcome npcClickFailedAccept = tryAcceptInitialTaskFromCurrentScreen(
                            context, activeState, "setup:npc-click-failed");
                    if (npcClickFailedAccept != null) {
                        return npcClickFailedAccept;
                    }
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
                        activeState.withTaskAccepted("initial-accept-clicked")
                                .next(FiveRingPhase.SYNC_TASK_PANEL, "initial-accept-pathing-unconfirmed"),
                        "initial task accepted; read tracker instead of accepting again");
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

    private FiveRingStepOutcome continueIfAcceptNpcNavigationStillPathing(TaskExecutionContext context,
                                                                          FiveRingPhaseContext state) {
        if (!state.waitingAcceptNpcPathing()) {
            return null;
        }

        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        LocationInfo current = playerStateService.syncMyPosition();
        if (current != null && gameStateUtil.isNearCoordinate(current.mapName, current.x, current.y,
                TARGET_MAP_NAME, NPC_COOR_X, NPC_COOR_Y, TASK_NPC_DIRECT_CLICK_DISTANCE)) {
            log.info("[five-ring-v2 accept] accept NPC navigation wait ended by coordinate: playerMap={} player=({}, {}) target=({}, {})",
                    current.mapName, current.x, current.y, NPC_COOR_X, NPC_COOR_Y);
            return null;
        }

        GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
        if (movementState == GameStateUtil.MovementState.MOVING
                || movementState == GameStateUtil.MovementState.PATHING_ACTIVE) {
            log.info("[five-ring-v2 accept] accept NPC navigation still pathing: state={}", movementState);
            return FiveRingStepOutcome.pathingStarted(state, "accept NPC navigation still pathing");
        }
        if (movementState == GameStateUtil.MovementState.MAYBE_MOVING) {
            log.info("[five-ring-v2 accept] accept NPC navigation weak movement ignored: state={}", movementState);
        }

        log.info("[five-ring-v2 accept] accept NPC navigation wait ended; retry navigation/click from current state");
        return null;
    }

    private boolean tryClickNearbyAcceptNpc(TaskExecutionContext context) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        NpcTarget acceptNpc = fiveRingAcceptNpc();
        if (!isNearAcceptNpc()) {
            return false;
        }
        PlayerCharacter me = gameContext.getMe();

        /*
         * A yielded accept-NPC navigation can resume while the mini-map/search overlay is still on
         * top of the NPC. Clean generic X-button blockers before the nearby direct click.
         */
        uiCleanerService.closeAllGenericWindows();
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

    private boolean isNearAcceptNpc() {
        NpcTarget acceptNpc = fiveRingAcceptNpc();
        /*
         * The result controls whether we can skip map navigation and click the task NPC directly.
         * Use a fresh no-focus position sync instead of cached state so previous-round map names do
         * not leak into the new round.
         */
        LocationInfo current = playerStateService.syncMyPosition();
        return current != null && gameStateUtil.isNearCoordinate(current.mapName, current.x, current.y,
                acceptNpc.getMapName(), acceptNpc.getX(), acceptNpc.getY(), TASK_NPC_DIRECT_CLICK_DISTANCE);
    }

    private FiveRingStepOutcome waitPathing(TaskExecutionContext context, FiveRingPhaseContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        /*
         * Combat has priority over movement confirmation. A tracker/pathing click can enter combat
         * without any useful movement sample, and after combat exits the character is already stopped.
         * Do not spend this task turn proving "not moving"; recover and read the tracker directly.
         */
        AutoCombatService.TickResult combatResult = autoCombatService.handleCombatTick(
                context, "five-ring-v2:pathing-precheck", true);
        if (combatResult == AutoCombatService.TickResult.EXIT_RECOVERED) {
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "pathing-combat-exit-recovered"),
                    "combat exited during pathing wait; sync task panel");
        }
        if (combatResult == AutoCombatService.TickResult.IN_COMBAT) {
            return FiveRingStepOutcome.sharedState(
                    state.next(FiveRingPhase.CHECK_COMBAT, "pathing-combat-running")
                            .withCombatObservedSincePathing("pathing-combat-running"),
                    "combat running during pathing wait");
        }

        if (!state.pathingMovementObserved()) {
            DialogType dialogType = dialogService.handleDialog(
                    DialogHandleRequest.inspect("wuhuan-v2:pathing-no-move-dialog-check")).getDialogType();
            if (dialogType != DialogType.NONE) {
                log.info("[five-ring-v2] pathing produced dialog before confirmed movement: type={}", dialogType);
                return FiveRingStepOutcome.sharedState(
                        state.next(FiveRingPhase.CHECK_COMBAT, "pathing-dialog-before-move-check-combat"),
                        "pathing opened dialog before confirmed movement; check combat before tracker sync");
            }

            if (state.phaseRetryCount() < MAX_PATHING_START_CONFIRM_ATTEMPTS) {
                return FiveRingStepOutcome.sharedState(
                        state.retrySamePhase("pathing-start-confirm-retry"),
                        "pathing start not observed yet; retry without heavy stop detection");
            }

            log.warn("[five-ring-v2] pathing start was not confirmed after {} lightweight attempts; resync task panel",
                    MAX_PATHING_START_CONFIRM_ATTEMPTS);
            return FiveRingStepOutcome.continueTo(
                    state.increaseUiErrorCount("pathing-start-not-confirmed")
                            .next(FiveRingPhase.SYNC_TASK_PANEL, "pathing-start-not-confirmed"),
                    "pathing start was not confirmed; sync task panel");
        }

        GameStateUtil.MovementState movementState = gameStateUtil.detectMovementState();
        if (movementState == GameStateUtil.MovementState.MOVING) {
            return FiveRingStepOutcome.sharedState(
                    state.next(FiveRingPhase.WAIT_PATHING, "pathing-moving-observed")
                            .withPathingMovementObserved("pathing-moving-observed"),
                    "pathing still moving: " + movementState);
        }

        if (movementState == GameStateUtil.MovementState.PATHING_ACTIVE
                || movementState == GameStateUtil.MovementState.MAYBE_MOVING) {
            /*
             * PATHING_ACTIVE may only mean "we just clicked a pathing link". Do not count it as a
             * confirmed movement sample; otherwise a missed click can look like "moved then stopped"
             * after the movement-intent grace window expires.
             */
            if (state.pathingMovementObserved()
                    && state.phaseRetryCount() >= MAX_PATHING_WEAK_MOVEMENT_CONFIRM_ATTEMPTS) {
                log.info("[five-ring-v2] pathing weak/protected movement reached limit; treat as stopped: state={} retryCount={}",
                        movementState, state.phaseRetryCount());
                return FiveRingStepOutcome.continueTo(
                        state.next(FiveRingPhase.CHECK_COMBAT, "pathing-weak-movement-treated-stopped"),
                        "pathing weak/protected movement treated as stopped");
            }
            if (!state.pathingMovementObserved()
                    && state.phaseRetryCount() >= MAX_PATHING_START_CONFIRM_ATTEMPTS) {
                log.warn("[five-ring-v2] pathing click did not produce confirmed movement; resync task panel: state={} retryCount={}",
                        movementState, state.phaseRetryCount());
                return FiveRingStepOutcome.continueTo(
                        state.increaseUiErrorCount("pathing-start-not-confirmed")
                                .next(FiveRingPhase.SYNC_TASK_PANEL, "pathing-start-not-confirmed"),
                        "pathing start was not confirmed; sync task panel");
            }
            return FiveRingStepOutcome.sharedState(
                    state.retrySamePhase("pathing-weak-movement-retry"),
                    "pathing weak/protected movement without confirmed movement; retry later: " + movementState);
        }

        if (state.pathingMovementObserved()) {
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.CHECK_COMBAT, "pathing-stopped-after-movement"),
                    "pathing stopped after confirmed movement");
        }

        if (state.phaseRetryCount() < MAX_PATHING_START_CONFIRM_ATTEMPTS) {
            return FiveRingStepOutcome.sharedState(
                    state.retrySamePhase("pathing-start-confirm-retry"),
                    "pathing start not observed yet; retry movement confirmation");
        }

        log.warn("[five-ring-v2] pathing start was not confirmed after {} attempts; resync task panel",
                MAX_PATHING_START_CONFIRM_ATTEMPTS);
        return FiveRingStepOutcome.continueTo(
                state.increaseUiErrorCount("pathing-start-not-confirmed")
                        .next(FiveRingPhase.SYNC_TASK_PANEL, "pathing-start-not-confirmed"),
                "pathing start was not confirmed; sync task panel");
    }

    private FiveRingStepOutcome checkCombat(TaskExecutionContext context, FiveRingPhaseContext state) {
        AutoCombatService.TickResult result = autoCombatService.handleCombatTick(
                context, "five-ring-v2", true);
        if (result == AutoCombatService.TickResult.EXIT_RECOVERED) {
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "combat-exit-recovered"),
                    "combat exited; sync task panel");
        }
        if (result == AutoCombatService.TickResult.IN_COMBAT) {
            return FiveRingStepOutcome.sharedState(state, "combat still running");
        }
        if ("story-ignored-check-combat".equals(state.source())) {
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "story-ignored-no-combat"),
                    "story ignored and no combat detected; sync task panel");
        }
        if ("pathing-dialog-before-move-check-combat".equals(state.source())
                || "pathing-combat-running".equals(state.source())
                || state.combatObservedSincePathing()) {
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "pathing-combat-or-dialog-no-combat"),
                    "pathing combat/dialog check ended outside combat; sync task panel");
        }
        return FiveRingStepOutcome.continueTo(
                state.next(FiveRingPhase.HANDLE_DIALOG, "no-combat"),
                "no combat detected");
    }

    private FiveRingStepOutcome handleDialog(TaskExecutionContext context, FiveRingPhaseContext state) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        DialogResultStatus giveResult = tryGiveItemAndTriggerPathingIfPossible(context, state);
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        if (giveResult == DialogResultStatus.GIVE_ITEM_DONE) {
            return FiveRingStepOutcome.continueTo(
                    state.next(FiveRingPhase.SYNC_TASK_PANEL, "give-item-done-read-tracker"),
                    "gave shoe; read tracker for next green link");
        }
        if (giveResult == DialogResultStatus.STORY_IGNORED) {
            /*
             * Story text can appear during the short transition into combat. Do not immediately open
             * the task tracker and click green links again; yield once and let the combat detector get the
             * next word before any further pathing attempt.
             */
            return FiveRingStepOutcome.sharedState(
                    state.next(FiveRingPhase.CHECK_COMBAT, "story-ignored-check-combat"),
                    "story dialog ignored; check combat before task-panel pathing");
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

        log.warn("[five-ring-v2] give-item dialog handling failed: status={}", giveResult);
        return FiveRingStepOutcome.continueTo(
                state.next(FiveRingPhase.SYNC_TASK_PANEL, "give-item-failed"),
                "give item failed; sync task panel");
    }

    private FiveRingStepOutcome syncTaskPanel(TaskExecutionContext context,
                                              FiveRingPhaseContext state,
                                              boolean allowFinished) {
        log.info("[five-ring-v2 tracker] scan left task tracker and click 五环 green pathing link");
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");

        TrackerPathingAttempt trackerAttempt = tryClickWuhuanTrackerLink(context, state, "sync", true);
        if (trackerAttempt.status == TrackerPathingStatus.PATHING_STARTED) {
            log.info("[five-ring-v2 tracker] 五环 tracker green link clicked");
            return FiveRingStepOutcome.pathingStarted(
                    trackerAttempt.state.withTaskAccepted("tracker-link-clicked")
                            .resetUiErrorCount("sync-success")
                            .next(FiveRingPhase.WAIT_PATHING, "tracker-pathing-started"),
                    "tracker green link clicked");
        }
        if (trackerAttempt.status == TrackerPathingStatus.TASK_FOUND_NO_GREEN
                || trackerAttempt.status == TrackerPathingStatus.TASK_FOUND_NO_LINK) {
            String reason = trackerAttempt.status == TrackerPathingStatus.TASK_FOUND_NO_GREEN
                    ? "tracker-no-green"
                    : "tracker-no-coordinate-link";
            FiveRingPhaseContext errorState = trackerAttempt.state.increaseUiErrorCount(reason);
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
            FiveRingPhaseContext errorState = trackerAttempt.state.increaseUiErrorCount("tracker-click-failed");
            log.warn("[five-ring-v2 tracker] 五环 tracker green click failed: errorCount={}",
                    errorState.uiErrorCount());
            return FiveRingStepOutcome.sharedState(
                    errorState.next(FiveRingPhase.SYNC_TASK_PANEL, "tracker-click-failed-retry"),
                    "tracker green click failed; retry");
        }
        if (allowFinished && isWuhuanAbsentByLegacyTaskPanel()) {
            log.info("[five-ring-v2 tracker] left tracker has no 五环 and legacy task panel also has no 五环; finish round");
            return FiveRingStepOutcome.finished(trackerAttempt.state, "五环 tracker and legacy task panel are both empty");
        }

        FiveRingPhaseContext errorState = trackerAttempt.state.increaseUiErrorCount("tracker-not-found");
        log.warn("[five-ring-v2 tracker] 五环 tracker block not found but legacy task panel still needs retry: errorCount={}",
                errorState.uiErrorCount());
        if (errorState.uiErrorCount() >= MAX_UI_ERROR_BEFORE_CLEANUP) {
            log.error("[five-ring-v2 tracker] tracker failed {} times; clear cached tracker block only",
                    MAX_UI_ERROR_BEFORE_CLEANUP);
            /*
             * Generic UI cleanup cannot remove player-name/tooltip overlays on the left tracker.
             * Avoid spending input turns on a cleanup that cannot reveal the 五环 block; the next
             * pass should recapture the tracker area from scratch instead.
             */
            errorState = errorState.clearWuhuanTrackerBlockRegion("tracker-not-found-cache-cleared")
                    .resetUiErrorCount("tracker-cache-cleared");
        }
        return FiveRingStepOutcome.continueTo(
                errorState.next(FiveRingPhase.SYNC_TASK_PANEL, "tracker-retry-later"),
                "tracker task not found; retry");
    }

    private FiveRingStepOutcome tryAcceptInitialTaskFromCurrentScreen(TaskExecutionContext context,
                                                                      FiveRingPhaseContext state,
                                                                      String reason) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        if (!isNearAcceptNpc()) {
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

        log.info("[five-ring-v2 accept] try accepting task from current screen: reason={}", reason);
        AcceptDialogPathingResult acceptResult = acceptInitialDialogAndTriggerPathing(context, "currentScreenAcceptPathing");
        if (acceptResult == AcceptDialogPathingResult.TASK_ACCEPTED_NEEDS_SYNC) {
            log.info("[five-ring-v2 accept] current screen accept clicked; read tracker next");
            return FiveRingStepOutcome.continueTo(
                    state.withTaskAccepted("current-screen-accept-clicked")
                            .next(FiveRingPhase.SYNC_TASK_PANEL, "current-screen-accept-pathing-unconfirmed"),
                    "current-screen accept clicked; read tracker instead of accepting again");
        }
        if (acceptResult == AcceptDialogPathingResult.NOT_ACCEPTED) {
            log.info("[five-ring-v2 accept] current screen is not a five-ring accept dialog; clean unexpected option dialog: reason={}",
                    reason);
            cleanupUnexpectedAcceptDialog(reason);
        }
        return null;
    }

    private AcceptDialogPathingResult acceptInitialDialogAndTriggerPathing(TaskExecutionContext context, String movementSource) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        AtomicReference<Boolean> acceptedClicked = new AtomicReference<>(false);
        TaskTransactionOutcome outcome = taskTransactionRunner.runExclusive(
                "wuhuan-v2:acceptDialogAndTriggerPathing",
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
                    DialogResult acceptResult = dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
                            "wuhuan-v2:accept-dialog",
                            List.of(new GreenTemplateClickSpec("wuhuan.acceptTask", ACCEPT_OPTION_TEMPLATE, 20, 20, 4)),
                            true));
                    boolean clickedAccept = acceptResult.isClicked();
                    acceptedClicked.set(clickedAccept);
                    log.info("[five-ring-v2 accept] accept dialog click result={}", clickedAccept);
                    if (!clickedAccept) {
                        return TaskTransactionResult.RETRYABLE_ERROR;
                    }

                    /*
                     * 接任务成功后不再打开 Alt+Q 走 P2/P1。等左侧任务追踪刷新出来，
                     * 下一阶段会直接读取“五环”任务块并点击绿色自动寻路。
                     */
                    TaskSleep.sleepOrStop(context, 600, "Five-ring V2 task interrupted");
                    return TaskTransactionResult.READY_TO_CONTINUE;
                });
        if (!outcome.completed()) {
            return AcceptDialogPathingResult.NOT_ACCEPTED;
        }
        log.info("[five-ring-v2 accept] accept flow finished without P2/P1: source={} txResult={} clicked={}",
                movementSource, outcome.result(), acceptedClicked.get());
        /*
         * Once the 五环 accept option was clicked, the character already owns a task. Do not loop
         * back into ACCEPT_TASK and click 云游大师 again; hand control to the left tracker reader.
         */
        return acceptedClicked.get()
                ? AcceptDialogPathingResult.TASK_ACCEPTED_NEEDS_SYNC
                : AcceptDialogPathingResult.NOT_ACCEPTED;
    }

    private boolean clickInitialNpcForAccept(TaskExecutionContext context) {
        TaskTransactionOutcome outcome = taskTransactionRunner.run(
                "wuhuan-v2:clickInitialNpcForAccept",
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
                    TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
                    boolean clicked = npcClickService.clickNpcSmart(fiveRingAcceptNpc().toClickRequest(gameContext.getMe()));
                    return clicked ? TaskTransactionResult.READY_TO_CONTINUE : TaskTransactionResult.RETRYABLE_ERROR;
                });
        if (outcome.result() == TaskTransactionResult.STOPPED) {
            TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        }
        return outcome.reachedExpectedResult();
    }

    private DialogResultStatus tryGiveItemAndTriggerPathingIfPossible(TaskExecutionContext context,
                                                                      FiveRingPhaseContext state) {
        AtomicReference<DialogResultStatus> dialogResult = new AtomicReference<>(DialogResultStatus.NO_DIALOG);

        TaskTransactionOutcome outcome = taskTransactionRunner.runExclusive(
                "wuhuan-v2:giveItemAndTriggerPathing",
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
                    dialogResult.set(dialogService.handleDialog(DialogHandleRequest.giveItemIfAvailable(
                            "wuhuan-v2:give-item", KEY_ITEM_NAME, state.shoeBagIndex())).getStatus());
                    if (dialogResult.get() != DialogResultStatus.GIVE_ITEM_DONE) {
                        return mapGiveDialogResultToTransactionResult(dialogResult.get());
                    }

                    log.info("[five-ring-v2] give item done; next phase will read left tracker instead of P2/P1");
                    return TaskTransactionResult.READY_TO_CONTINUE;
                });

        if (!outcome.completed()) {
            return DialogResultStatus.INTERRUPTED;
        }
        return dialogResult.get();
    }

    private TaskTransactionResult mapGiveDialogResultToTransactionResult(DialogResultStatus result) {
        return switch (result) {
            case NO_DIALOG, STORY_IGNORED -> TaskTransactionResult.READY_TO_CONTINUE;
            case GIVE_OPTION_NOT_FOUND -> TaskTransactionResult.RETRYABLE_ERROR;
            case INTERRUPTED -> TaskTransactionResult.STOPPED;
            case GIVE_ITEM_FAILED, FAILED -> TaskTransactionResult.FAILED;
            default -> TaskTransactionResult.READY_TO_CONTINUE;
        };
    }

    private TrackerPathingAttempt tryClickWuhuanTrackerLink(TaskExecutionContext context,
                                                            FiveRingPhaseContext state,
                                                            String source,
                                                            boolean allowAnchorSearch) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        WuhuanTrackerSnapshot snapshot = captureWuhuanTrackerSnapshot(state, source, allowAnchorSearch);
        if (!snapshot.taskFound) {
            return new TrackerPathingAttempt(TrackerPathingStatus.TASK_NOT_FOUND, snapshot.state);
        }
        if (snapshot.greenScan.segments.isEmpty()) {
            return new TrackerPathingAttempt(TrackerPathingStatus.TASK_FOUND_NO_GREEN, snapshot.state);
        }

        Optional<TrackerGreenLinkSegment> linkSegment = findTrackerPathingNameSegment(snapshot.greenScan);
        if (linkSegment.isEmpty()) {
            log.warn("[five-ring-v2 tracker] 五环 green text found but pathing name link was not resolved: segments={}",
                    snapshot.greenScan.segments);
            return new TrackerPathingAttempt(TrackerPathingStatus.TASK_FOUND_NO_LINK, snapshot.state);
        }

        TrackerGreenLinkSegment segment = linkSegment.get();
        if (clickWuhuanTrackerGreen(context, segment, source, snapshot)) {
            return new TrackerPathingAttempt(TrackerPathingStatus.PATHING_STARTED, snapshot.state);
        }
        return new TrackerPathingAttempt(TrackerPathingStatus.CLICK_FAILED, snapshot.state);
    }

    private WuhuanTrackerSnapshot captureWuhuanTrackerSnapshot(FiveRingPhaseContext state,
                                                               String source,
                                                               boolean allowAnchorSearch) {
        FiveRingPhaseContext workingState = state;
        if (workingState.wuhuanTrackerBlockRegion() != null) {
            WuhuanTrackerSnapshot cachedBlock = captureWuhuanBlockSnapshot(
                    workingState,
                    workingState.trackerPanelRegion(),
                    workingState.wuhuanTrackerBlockRegion(),
                    source + ":cached-block");
            if (cachedBlock.taskFound) {
                return cachedBlock;
            }
            workingState = workingState.clearWuhuanTrackerBlockRegion("tracker-cached-block-missed");
        }

        if (workingState.trackerPanelRegion() != null) {
            WuhuanTrackerSnapshot cachedPanel = captureTrackerPanelAndFindWuhuanBlock(
                    workingState,
                    workingState.trackerPanelRegion(),
                    source + ":cached-panel");
            if (cachedPanel.taskFound) {
                return cachedPanel;
            }
            workingState = cachedPanel.state;
        }

        if (!allowAnchorSearch) {
            return WuhuanTrackerSnapshot.empty(workingState);
        }

        OcrWindowRegion trackerPanelRegion = resolveTrackerPanelRegion(source);
        if (trackerPanelRegion == null) {
            return WuhuanTrackerSnapshot.empty(workingState);
        }
        FiveRingPhaseContext trackerState = workingState.withTrackerRegions(
                trackerPanelRegion, null, "tracker-anchor-resolved");
        return captureTrackerPanelAndFindWuhuanBlock(trackerState, trackerPanelRegion, source + ":anchor-panel");
    }

    private WuhuanTrackerSnapshot captureTrackerPanelAndFindWuhuanBlock(FiveRingPhaseContext state,
                                                                        OcrWindowRegion trackerPanelRegion,
                                                                        String source) {
        String rawPath = captureRegionToFile(source, trackerPanelRegion, "panel_raw");
        if (rawPath == null) {
            return WuhuanTrackerSnapshot.empty(state);
        }

        Optional<WuhuanTitleAnchor> title = findWuhuanTitleAnchor(rawPath, trackerPanelRegion, source);
        if (title.isEmpty()) {
            log.info("[five-ring-v2 tracker] 五环 title not found in tracker panel: source={} raw={}",
                    source, rawPath);
            return WuhuanTrackerSnapshot.empty(state.withTrackerRegions(trackerPanelRegion, null,
                    "tracker-panel-title-missed"));
        }

        OcrWindowRegion blockRegion = buildWuhuanBlockRegion(trackerPanelRegion, title.get());
        FiveRingPhaseContext nextState = state.withTrackerRegions(
                trackerPanelRegion, blockRegion, "tracker-wuhuan-block-found");
        return captureWuhuanBlockSnapshot(nextState, trackerPanelRegion, blockRegion, source + ":block");
    }

    private WuhuanTrackerSnapshot captureWuhuanBlockSnapshot(FiveRingPhaseContext state,
                                                            OcrWindowRegion trackerPanelRegion,
                                                            OcrWindowRegion blockRegion,
                                                            String source) {
        String rawPath = captureRegionToFile(source, blockRegion, "block_raw");
        if (rawPath == null) {
            return WuhuanTrackerSnapshot.empty(state);
        }
        Optional<WuhuanTitleAnchor> title = findWuhuanTitleAnchor(rawPath, blockRegion, source + ":block-title");
        if (title.isEmpty()) {
            log.info("[five-ring-v2 tracker] cached 五环 block no longer contains title: source={} raw={}",
                    source, rawPath);
            return WuhuanTrackerSnapshot.empty(state.clearWuhuanTrackerBlockRegion("tracker-block-title-missed"));
        }

        BufferedImage blockImage = readImage(rawPath);
        if (blockImage == null) {
            return WuhuanTrackerSnapshot.empty(state);
        }
        try {
            int absoluteLeft = tracker.getWindowBaseX() + blockRegion.x1();
            int absoluteTop = tracker.getWindowBaseY() + blockRegion.y1();
            TrackerGreenLinkScan greenScan = scanTrackerGreenLinks(blockImage, absoluteLeft, absoluteTop);
            log.info("[five-ring-v2 tracker] block snapshot: source={} region={} greenSegments={} raw={}",
                    source, blockRegion.toShortText(), greenScan.segments, rawPath);
            return new WuhuanTrackerSnapshot(true, state.withTaskAccepted("tracker-wuhuan-title-visible"),
                    greenScan, rawPath, absoluteLeft, absoluteTop);
        } finally {
            blockImage.flush();
        }
    }

    private Optional<WuhuanTitleAnchor> findWuhuanTitleAnchor(String rawPath,
                                                              OcrWindowRegion captureRegion,
                                                              String source) {
        String yellowPath = windowScopedTempPath.resolve(
                "wuhuan_tracker_" + safeFileToken(source) + "_" + System.currentTimeMillis() + "_yellow.png");
        ImagePreprocessor.washYellowText(rawPath, yellowPath);
        List<OcrWordResult> words = textRecognizer.getAllTextResultsForMatch(
                yellowPath,
                "wuhuan-tracker-yellow:" + source,
                result -> !result.isEmpty());
        StringBuilder text = new StringBuilder();
        for (OcrWordResult word : words) {
            if (word.getText() != null) {
                text.append(word.getText()).append('|');
            }
        }
        for (OcrWordResult word : words) {
            String recognized = word.getText() == null ? "" : word.getText().replaceAll("\\s+", "");
            if (!recognized.contains("五环")) {
                continue;
            }
            int localLeft = word.getWidth() > 0
                    ? word.getLeft()
                    : word.getX() - WUHUAN_TITLE_CENTER_FALLBACK_LEFT_SHIFT;
            int localTop = word.getHeight() > 0 ? word.getTop() : word.getY() - 8;
            int left = clamp(localLeft, 0, Math.max(0, captureRegion.width() - 1));
            int top = clamp(localTop, 0, Math.max(0, captureRegion.height() - 1));
            log.info("[five-ring-v2 tracker] 五环 title matched: source={} text='{}' local=({}, {}) yellow={} all='{}'",
                    source, recognized, left, top, yellowPath, text);
            return Optional.of(new WuhuanTitleAnchor(left, top, recognized));
        }
        log.info("[five-ring-v2 tracker] yellow OCR did not contain 五环: source={} yellow={} text='{}'",
                source, yellowPath, text);
        return Optional.empty();
    }

    private OcrWindowRegion buildWuhuanBlockRegion(OcrWindowRegion trackerPanelRegion, WuhuanTitleAnchor title) {
        int left = trackerPanelRegion.x1() + title.localLeft;
        int top = trackerPanelRegion.y1() + title.localTop;
        return new OcrWindowRegion(left, top, trackerPanelRegion.x2(), top + WUHUAN_TRACKER_BLOCK_HEIGHT)
                .clamp(GAME_CLIENT_WIDTH, GAME_CLIENT_HEIGHT);
    }

    private OcrWindowRegion resolveTrackerPanelRegion(String source) {
        tracker.refreshWindowState();
        int[] searchRect = new int[]{
                tracker.getWindowBaseX() + TRACKER_ANCHOR_SEARCH_REL_LEFT,
                tracker.getWindowBaseY() + TRACKER_ANCHOR_SEARCH_REL_TOP,
                tracker.getWindowBaseX() + TRACKER_ANCHOR_SEARCH_REL_RIGHT,
                tracker.getWindowBaseY() + TRACKER_ANCHOR_SEARCH_REL_BOTTOM
        };
        Point anchor = coordinateHelper.findImageInRegion(TRACKER_ANCHOR_TEMPLATE, searchRect, TRACKER_ANCHOR_THRESHOLD);
        if (anchor == null) {
            log.warn("[five-ring-v2 tracker] tracker anchor not found: source={} searchRect=({}, {})-({}, {})",
                    source, searchRect[0], searchRect[1], searchRect[2], searchRect[3]);
            return null;
        }

        int left = anchor.x + TRACKER_PANEL_FROM_ANCHOR_LEFT - tracker.getWindowBaseX();
        int top = anchor.y + TRACKER_PANEL_FROM_ANCHOR_TOP - tracker.getWindowBaseY();
        OcrWindowRegion region = new OcrWindowRegion(
                left,
                top,
                anchor.x + TRACKER_PANEL_FROM_ANCHOR_RIGHT - tracker.getWindowBaseX(),
                top + TRACKER_PANEL_HEIGHT).clamp(GAME_CLIENT_WIDTH, GAME_CLIENT_HEIGHT);
        log.info("[five-ring-v2 tracker] tracker panel region resolved: source={} anchor=({}, {}) region={}",
                source, anchor.x, anchor.y, region.toShortText());
        return region;
    }

    private String captureRegionToFile(String source, OcrWindowRegion region, String suffix) {
        if (region == null || !region.isValid()) {
            log.warn("[five-ring-v2 tracker] invalid capture region: source={} suffix={} region={}",
                    source, suffix, region == null ? null : region.toShortText());
            return null;
        }
        tracker.refreshWindowState();
        String path = windowScopedTempPath.resolve(
                "wuhuan_tracker_" + safeFileToken(source) + "_" + System.currentTimeMillis() + "_" + suffix + ".png");
        int left = tracker.getWindowBaseX() + region.x1();
        int top = tracker.getWindowBaseY() + region.y1();
        int right = tracker.getWindowBaseX() + region.x2();
        int bottom = tracker.getWindowBaseY() + region.y2();
        if (!tracker.captureToFile("wuhuan-tracker:" + source + ":" + suffix, path, left, top, right, bottom)) {
            log.warn("[five-ring-v2 tracker] capture failed: source={} suffix={} rect=({}, {})-({}, {})",
                    source, suffix, left, top, right, bottom);
            return null;
        }
        return path;
    }

    private BufferedImage readImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (Exception e) {
            log.warn("[five-ring-v2 tracker] image read failed: path={}", path, e);
            return null;
        }
    }

    private TrackerGreenLinkScan scanTrackerGreenLinks(BufferedImage frame, int absoluteLeft, int absoluteTop) {
        List<ImagePreprocessor.GreenTextBand> bands = ImagePreprocessor.findGreenTextBands(frame);
        ImagePreprocessor.GreenTextBand band = ImagePreprocessor.pickGreenTextBand(bands, true);
        if (band == null) {
            log.info("[five-ring-v2 tracker] green link scan: no green band");
            return TrackerGreenLinkScan.empty();
        }
        List<TrackerGreenLinkSegment> segments = splitTrackerGreenLinkSegments(frame, band, absoluteLeft, absoluteTop);
        int bandWidth = band.maxX() - band.minX() + 1;
        log.info("[five-ring-v2 tracker] green link scan: bands={} band=({}, {})-({}, {}) width={} segments={}",
                bands.size(), absoluteLeft + band.minX(), absoluteTop + band.minY(),
                absoluteLeft + band.maxX(), absoluteTop + band.maxY(), bandWidth, segments);
        return new TrackerGreenLinkScan(segments, bandWidth);
    }

    /*
     * 五环任务追踪的可点击目标是括号后、进度 "[n/5]" 前的怪/NPC 名称。
     * 坐标数字只是描述文本，不作为主要锚点；这里优先使用同一行进度作为右锚，
     * 没有进度时再取当前行最后一段可点击绿字。
     */
    private Optional<TrackerGreenLinkSegment> findTrackerPathingNameSegment(TrackerGreenLinkScan scan) {
        List<TrackerGreenLinkSegment> segments = scan.segments;
        if (segments.size() < 3) {
            return Optional.empty();
        }

        TrackerGreenLinkSegment last = segments.get(segments.size() - 1);
        TrackerGreenLinkSegment beforeProgress = segments.get(segments.size() - 2);
        if (looksLikeProgressTailSegment(last) && looksLikePathingLinkSegment(beforeProgress)) {
            log.info("[five-ring-v2 tracker] pathing name selected before progress: link={} progress={}",
                    beforeProgress, last);
            return Optional.of(beforeProgress);
        }

        if (looksLikePathingLinkSegment(last)) {
            log.info("[five-ring-v2 tracker] pathing name selected from last green segment: link={}", last);
            return Optional.of(last);
        }
        return Optional.empty();
    }

    private boolean looksLikePathingLinkSegment(TrackerGreenLinkSegment segment) {
        return segment.width() >= 18 && segment.pixels >= 50;
    }

    private boolean looksLikeProgressTailSegment(TrackerGreenLinkSegment segment) {
        return segment.width() <= 18 && segment.pixels <= 70;
    }

    private List<TrackerGreenLinkSegment> splitTrackerGreenLinkSegments(BufferedImage frame,
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
                    && glyph.minX - previous.maxX - 1 >= TRACKER_LINK_SPLIT_GAP;
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
                startX = glyph.minX;
            }
            endX = glyph.maxX;
            pixels += glyph.pixels;
            previous = glyph;
        }
        addTrackerSegment(segments, absoluteLeft, absoluteTop, startX, endX, band, pixels);
        return segments;
    }

    private List<TrackerGreenGlyph> collectTrackerGreenGlyphs(BufferedImage frame,
                                                              ImagePreprocessor.GreenTextBand band) {
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
                && glyph.pixels <= TRACKER_LINK_DELIMITER_MAX_PIXELS
                && leftPixels >= TRACKER_LINK_MIN_PIXELS
                && rightPixels >= TRACKER_LINK_MIN_PIXELS;
    }

    private int remainingPixels(List<TrackerGreenGlyph> glyphs, int fromIndex) {
        int total = 0;
        for (int i = fromIndex; i < glyphs.size(); i++) {
            total += glyphs.get(i).pixels;
        }
        return total;
    }

    private void addTrackerSegment(List<TrackerGreenLinkSegment> segments,
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

    private boolean clickWuhuanTrackerGreen(TaskExecutionContext context,
                                            TrackerGreenLinkSegment segment,
                                            String source,
                                            WuhuanTrackerSnapshot snapshot) {
        TaskCheckpoint.throwIfStopRequested(context, "Five-ring V2 task interrupted");
        /*
         * Tracker links are thin green glyphs. A bounding-box center can land in a character gap,
         * so derive horizontal placement from the target-name green pixels and keep vertical
         * placement at the text-row center instead of the dense bottom strokes.
         */
        Point click = resolveTrackerGreenClickPoint(snapshot, segment);
        saveTrackerClickDebugImage(snapshot, click, source);
        log.info("[five-ring-v2 tracker] click green link: source={} segment={} click=({}, {})",
                source, segment, click.x, click.y);
        boolean clicked = inputSequences.submitAndWait("wuhuan-v2:tracker-green-click:" + safeFileToken(source), List.of(
                InputAction.moveMouse(click.x, click.y),
                InputAction.sleep(120),
                InputAction.clickLeft(click.x, click.y, 300)
        ));
        if (!clicked) {
            return false;
        }

        /*
         * Tracker green-link clicks should not force a coordinate OCR sync. The click target was
         * already resolved from the task tracker itself; after clicking, only confirm that the scene
         * began changing with a short two-edge pixel probe before yielding the task turn.
         */
        if (gameStateUtil.confirmPathingStartedByEdgePixelDiff("wuhuan-v2:tracker-green-click:" + source)) {
            gameStateUtil.recordMovementIntent("wuhuan-v2:tracker-green-click:" + source);
            log.info("[five-ring-v2 tracker] green click confirmed pathing by edge pixels: source={} click=({}, {})",
                    source, click.x, click.y);
            return true;
        }
        DialogType newDialogType = dialogService.handleDialog(
                DialogHandleRequest.inspect("wuhuan-v2:tracker-post-click-dialog-check:" + source)).getDialogType();
        if (newDialogType != DialogType.NONE) {
            /*
             * Some 五环 tracker links complete by opening a dialog instead of visible movement.
             * Pre-click inspection already ruled out an old dialog, so this is treated as a
             * successful click and the WAIT_PATHING phase will route the dialog normally.
             */
            log.info("[five-ring-v2 tracker] green click produced dialog: source={} type={} click=({}, {})",
                    source, newDialogType, click.x, click.y);
            return true;
        }
        log.warn("[five-ring-v2 tracker] green click did not confirm pathing by edge pixels: source={} click=({}, {})",
                source, click.x, click.y);
        return false;
    }

    private Point resolveTrackerGreenClickPoint(WuhuanTrackerSnapshot snapshot, TrackerGreenLinkSegment segment) {
        Point center = new Point((segment.minX + segment.maxX) / 2, (segment.minY + segment.maxY) / 2);
        if (snapshot == null || snapshot.rawPath == null) {
            return center;
        }
        BufferedImage image = readImage(snapshot.rawPath);
        if (image == null) {
            return center;
        }
        try {
            int localX1 = Math.max(0, segment.minX - snapshot.absoluteLeft);
            int localX2 = Math.min(image.getWidth() - 1, segment.maxX - snapshot.absoluteLeft);
            int localY1 = Math.max(0, segment.minY - snapshot.absoluteTop);
            int localY2 = Math.min(image.getHeight() - 1, segment.maxY - snapshot.absoluteTop);
            int[] primaryRun = resolvePrimaryTrackerGreenRun(image, localX1, localX2, localY1, localY2);
            localX1 = primaryRun[0];
            localX2 = primaryRun[1];
            int totalPixels = 0;
            long weightedX = 0L;
            for (int y = localY1; y <= localY2; y++) {
                for (int x = localX1; x <= localX2; x++) {
                    if (ImagePreprocessor.isOptionGreen(image.getRGB(x, y))) {
                        totalPixels++;
                        weightedX += x;
                    }
                }
            }
            if (totalPixels < TRACKER_LINK_MIN_PIXELS) {
                return center;
            }

            int clickX = (int) Math.round(weightedX / (double) totalPixels);
            int clickY = (localY1 + localY2) / 2;
            return new Point(snapshot.absoluteLeft + clickX, snapshot.absoluteTop + clickY);
        } finally {
            image.flush();
        }
    }

    private int[] resolvePrimaryTrackerGreenRun(BufferedImage image, int localX1, int localX2, int localY1, int localY2) {
        int runStart = -1;
        int runEnd = -1;
        int bestStart = localX1;
        int bestEnd = localX2;
        int bestPixels = 0;
        for (int x = localX1; x <= localX2; x++) {
            int columnPixels = 0;
            for (int y = localY1; y <= localY2; y++) {
                if (ImagePreprocessor.isOptionGreen(image.getRGB(x, y))) {
                    columnPixels++;
                }
            }
            if (columnPixels > 0) {
                if (runStart < 0) {
                    runStart = x;
                }
                runEnd = x;
                bestPixels += columnPixels;
            } else if (runStart >= 0) {
                if (bestPixels >= TRACKER_LINK_MIN_PIXELS) {
                    bestStart = runStart;
                    bestEnd = runEnd;
                    break;
                }
                runStart = -1;
                runEnd = -1;
                bestPixels = 0;
            }
        }
        if (runStart >= 0 && bestPixels >= TRACKER_LINK_MIN_PIXELS) {
            bestStart = runStart;
            bestEnd = runEnd;
        }
        return new int[]{bestStart, bestEnd};
    }

    private void saveTrackerClickDebugImage(WuhuanTrackerSnapshot snapshot, Point click, String source) {
        if (snapshot == null || snapshot.rawPath == null) {
            return;
        }
        BufferedImage image = readImage(snapshot.rawPath);
        if (image == null) {
            return;
        }
        try {
            int localX = click.x - snapshot.absoluteLeft;
            int localY = click.y - snapshot.absoluteTop;
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(Color.RED);
                graphics.setStroke(new BasicStroke(2.0f));
                graphics.drawOval(localX - 5, localY - 5, 10, 10);
                graphics.drawLine(localX - 10, localY, localX + 10, localY);
                graphics.drawLine(localX, localY - 10, localX, localY + 10);
            } finally {
                graphics.dispose();
            }
            String debugPath = windowScopedTempPath.resolve(
                    "wuhuan_tracker_" + safeFileToken(source) + "_" + System.currentTimeMillis() + "_click_debug.png");
            ImageIO.write(image, "png", new File(debugPath));
            log.info("[five-ring-v2 tracker] click debug image saved: source={} click=({}, {}) local=({}, {}) image={}",
                    source, click.x, click.y, localX, localY, debugPath);
        } catch (Exception e) {
            log.warn("[five-ring-v2 tracker] failed to save click debug image: source={} raw={}",
                    source, snapshot.rawPath, e);
        } finally {
            image.flush();
        }
    }

    private boolean isWuhuanAbsentByLegacyTaskPanel() {
        boolean found = questManager.activateTaskIfPresentExclusive(QUEST_PANEL_TASK_CODE, false);
        log.info("[five-ring-v2 tracker] legacy task panel confirm: task={} found={}",
                QUEST_PANEL_TASK_CODE, found);
        return !found;
    }

    private String safeFileToken(String source) {
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        return source.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    private void runUiCleanupContinue(String name) {
        taskTransactionRunner.run(
                name,
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
                    uiCleanerService.cleanUpAll();
                    return TaskTransactionResult.READY_TO_CONTINUE;
                });
    }

    private void runUiCleanupRetryLater(String name) {
        taskTransactionRunner.run(
                name,
                TaskTransactionResult.RETRYABLE_ERROR,
                TaskYieldPolicy.RETRY_LATER,
                () -> {
                    uiCleanerService.cleanUpAll();
                    return TaskTransactionResult.RETRYABLE_ERROR;
                });
    }

    private void yieldAfterMustYield(TaskExecutionContext context, FiveRingStepOutcome outcome) {
        long delayMs = handoffDelayMs(outcome);
        log.info("[five-ring-v2] yield after phase: result={} next={} delayMs={}",
                outcome.transactionResult(), outcome.nextState().phase(), delayMs);
        TaskSleep.sleepOrStop(context, delayMs, "Five-ring V2 task interrupted");
    }

    private long handoffDelayMs(FiveRingStepOutcome outcome) {
        if (outcome.nextState().phase() == FiveRingPhase.CHECK_COMBAT) {
            if ("story-ignored-check-combat".equals(outcome.nextState().source())) {
                return STORY_IGNORED_COMBAT_CHECK_DELAY_MS;
            }
            return autoCombatService.getDynamicPollingIntervalMs();
        }
        if (outcome.nextState().phase() == FiveRingPhase.WAIT_PATHING) {
            return 800L;
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

    private enum TrackerPathingStatus {
        PATHING_STARTED,
        TASK_FOUND_NO_GREEN,
        TASK_FOUND_NO_LINK,
        TASK_NOT_FOUND,
        CLICK_FAILED
    }

    private static final class TrackerPathingAttempt {
        private final TrackerPathingStatus status;
        private final FiveRingPhaseContext state;

        private TrackerPathingAttempt(TrackerPathingStatus status, FiveRingPhaseContext state) {
            this.status = status;
            this.state = state;
        }
    }

    private static final class WuhuanTrackerSnapshot {
        private final boolean taskFound;
        private final FiveRingPhaseContext state;
        private final TrackerGreenLinkScan greenScan;
        private final String rawPath;
        private final int absoluteLeft;
        private final int absoluteTop;

        private WuhuanTrackerSnapshot(boolean taskFound,
                                      FiveRingPhaseContext state,
                                      TrackerGreenLinkScan greenScan,
                                      String rawPath,
                                      int absoluteLeft,
                                      int absoluteTop) {
            this.taskFound = taskFound;
            this.state = state;
            this.greenScan = greenScan;
            this.rawPath = rawPath;
            this.absoluteLeft = absoluteLeft;
            this.absoluteTop = absoluteTop;
        }

        private static WuhuanTrackerSnapshot empty(FiveRingPhaseContext state) {
            return new WuhuanTrackerSnapshot(false, state, TrackerGreenLinkScan.empty(), null, 0, 0);
        }
    }

    private static final class TrackerGreenLinkScan {
        private final List<TrackerGreenLinkSegment> segments;
        private final int bandWidth;

        private TrackerGreenLinkScan(List<TrackerGreenLinkSegment> segments, int bandWidth) {
            this.segments = segments;
            this.bandWidth = bandWidth;
        }

        private static TrackerGreenLinkScan empty() {
            return new TrackerGreenLinkScan(List.of(), 0);
        }

        @Override
        public String toString() {
            return "TrackerGreenLinkScan{segments=" + segments + ", bandWidth=" + bandWidth + '}';
        }
    }

    private static final class TrackerGreenLinkSegment {
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;
        private final int pixels;

        private TrackerGreenLinkSegment(int minX, int minY, int maxX, int maxY, int pixels) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.pixels = pixels;
        }

        private int width() {
            return maxX - minX + 1;
        }

        @Override
        public String toString() {
            return "TrackerGreenLinkSegment{minX=" + minX + ", minY=" + minY
                    + ", maxX=" + maxX + ", maxY=" + maxY + ", pixels=" + pixels + '}';
        }
    }

    private static final class TrackerGreenGlyph {
        private final int minX;
        private final int maxX;
        private final int pixels;

        private TrackerGreenGlyph(int minX, int maxX, int pixels) {
            this.minX = minX;
            this.maxX = maxX;
            this.pixels = pixels;
        }

        private int width() {
            return maxX - minX + 1;
        }
    }

    private static final class WuhuanTitleAnchor {
        private final int localLeft;
        private final int localTop;
        private final String text;

        private WuhuanTitleAnchor(int localLeft, int localTop, String text) {
            this.localLeft = localLeft;
            this.localTop = localTop;
            this.text = text;
        }
    }

    private enum AcceptDialogPathingResult {
        NOT_ACCEPTED,
        TASK_ACCEPTED_NEEDS_SYNC
    }

    private record FiveRingSupplyCheck(boolean incenseRefilled, Integer shoeBagIndex) {
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
