package com.bot.dhxy.task;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.PathingResult;
import com.bot.dhxy.model.npc.NpcMovementType;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcTarget;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.service.*;
import com.bot.dhxy.service.dialog.DialogHandleResult;
import com.bot.dhxy.task.startup.TaskStartupCheckResult;
import com.bot.dhxy.task.startup.TaskStartupCheckService;
import com.bot.dhxy.task.template.BaseTaskTemplate;
import com.bot.dhxy.task.template.TaskStepExecutor;
import com.bot.dhxy.task.template.TaskStepResult;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskTransactionRunner;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.task.wuhuan.FiveRingHandoverState;
import com.bot.dhxy.task.wuhuan.FiveRingLoopDecision;
import com.bot.dhxy.task.wuhuan.FiveRingRuntimeState;
import com.bot.dhxy.tools.GameStateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class FiveRingTask extends BaseTaskTemplate {

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

    @Value("${debug.npc-first-shot:false}")
    private boolean debugNpcFirstShot;

    private static final String TARGET_MAP_NAME = "长安";
    private static final String TARGET_NPC_NAME = "云游大师";
    private static final int NPC_COOR_X = 87;
    private static final int NPC_COOR_Y = 174;
    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/wuhuan_accept_first_option.png";

    private static final int TUNE_X = -10;
    private static final int TUNE_Y = 0;
    private static final String KEY_ITEM_NAME = "wuhuan/shoe.png";

    private static final int MAX_RETRY = 5;

    public FiveRingTask(GameContext context,
                        NavigationService navigationService,
                        NpcClickService npcClickService,
                        DialogService dialogService,
                        PlayerStateService playerStateService,
                        QuestManagerService questManager,
                        AutoCombatService autoCombatService,
                        BagService bagService,
                        GameStateUtil gameStateUtil,
                        UICleanerService uiCleanerService,
                        TaskStepExecutor taskStepExecutor,
                        TaskStartupCheckService taskStartupCheckService,
                        TaskTransactionRunner taskTransactionRunner) {
        super(context, taskStepExecutor);
        this.navigationService = navigationService;
        this.npcClickService = npcClickService;
        this.dialogService = dialogService;
        this.playerStateService = playerStateService;
        this.questManager = questManager;
        this.autoCombatService = autoCombatService;
        this.bagService = bagService;
        this.gameStateUtil = gameStateUtil;
        this.uiCleanerService = uiCleanerService;
        this.taskStartupCheckService = taskStartupCheckService;
        this.taskTransactionRunner = taskTransactionRunner;
    }

    @Override
    public String getTaskCode() {
        return "wuhuan";
    }

    @Override
    public String getTaskName() {
        return "五环";
    }

    @Override
    public void stop() {
        log.info("[five-ring] stop requested");
        markTaskIdle();
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        logTaskBanner();

        try {
            TaskExecutionContext context = resolveExecutionContext(executionContext);
            if (debugNpcFirstShot) {
                return executeNpcFirstShotDebug(context);
            }

            TaskStartupCheckResult checkResult = taskStartupCheckService.checkFiveRing(context);
            if (checkResult.isBlocked()) {
                log.info("five-ring startup check blocked: {}", checkResult.getReason());
                return checkResult.getBlockedResult();
            }
            log.info("five-ring startup check passed: {}", checkResult.getReason());

            FiveRingRuntimeState runtimeState = new FiveRingRuntimeState();

            TaskRunResult startupResult = runStartupSteps(context, runtimeState);
            if (startupResult != TaskRunResult.SUCCESS) {
                return startupResult;
            }

            return runMainLoop(context, runtimeState);
        } finally {
            taskTransactionRunner.forceReleaseTurn("wuhuan:execute-finished");
        }
    }

    private TaskRunResult executeNpcFirstShotDebug(TaskExecutionContext executionContext) {
        log.warn("[five-ring NPC first-shot debug] debug.npc-first-shot=true; this run only tests first-shot NPC click.");
        if (executionContext != null) {
            executionContext.throwIfStopRequested();
        }
        gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        try {
            log.info("[five-ring NPC first-shot debug] sync current character identity and position");
            if (executionContext != null) {
                executionContext.throwIfStopRequested();
            }
            NpcTarget acceptNpc = fiveRingAcceptNpc();
            boolean ok = npcClickService.debugClickNpcSmartFirstShot(
                    gameContext.getMe(), acceptNpc.getMapName(), acceptNpc.getX(), acceptNpc.getY(),
                    acceptNpc.getName(), acceptNpc.getTuneX(), acceptNpc.getTuneY());
            log.info("[five-ring NPC first-shot debug] finished: result={}", ok);
            markTaskIdle();
            return ok ? TaskRunResult.SUCCESS : TaskRunResult.FAILED;
        } catch (Exception e) {
            log.error("[five-ring NPC first-shot debug] failed with exception", e);
            markTaskFailed();
            return TaskRunResult.FAILED;
        }
    }

    private void logTaskBanner() {
        log.info("====================================");
        log.info("[five-ring] start automated five-ring task");
        log.info("====================================");
    }

    private TaskRunResult runStartupSteps(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        TaskRunResult prepareResult = resolveStepResult(
                executePrepareBeforeRunStep(executionContext, runtimeState),
                "five-ring stopped during prepare-before-run",
                "five-ring prepare-before-run failed"
        );
        if (prepareResult != TaskRunResult.SUCCESS) {
            return prepareResult;
        }

        TaskRunResult handoverResult = resolveStepResult(
                executeDetectHandoverStep(executionContext, runtimeState),
                "five-ring stopped during handover detection",
                "five-ring handover detection failed"
        );
        if (handoverResult != TaskRunResult.SUCCESS) {
            return handoverResult;
        }
        if (runtimeState.getHandoverState() == null) {
            log.error("five-ring handover detection failed: runtime state is null, abort task");
            markTaskFailed();
            return TaskRunResult.FAILED;
        }

        if (runtimeState.getHandoverState() == FiveRingHandoverState.NEED_SETUP) {
            runtimeState.setNeedTaskSync(true);
        }

        if (runtimeState.getHandoverState() == FiveRingHandoverState.NEED_SETUP) {
            log.info("[five-ring] no running task detected; navigate to {} and accept initial task from {}",
                    TARGET_MAP_NAME, TARGET_NPC_NAME);
            TaskRunResult setupResult = resolveStepResult(
                    executeSetupInitialTaskStep(executionContext),
                    "five-ring stopped during initial task setup",
                    "five-ring initial task setup failed"
            );
            if (setupResult != TaskRunResult.SUCCESS) {
                return setupResult;
            }
            runtimeState.setNeedTaskSync(false);
        }

        return TaskRunResult.SUCCESS;
    }

    private TaskRunResult runMainLoop(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        log.info("[five-ring] start main loop");
        while (gameContext.getBotStatus() == GameContext.BotStatus.RUNNING) {
            TaskRunResult loopResult = resolveStepResult(
                    executeRunLoopOnceStep(executionContext, runtimeState),
                    "five-ring stopped during run loop",
                    "five-ring run loop failed"
            );
            if (loopResult != TaskRunResult.SUCCESS) {
                return loopResult;
            }
            if (runtimeState.getLoopDecision() == FiveRingLoopDecision.FINISHED) {
                return TaskRunResult.SUCCESS;
            }
        }

        log.info("[five-ring] task stopped");
        return TaskRunResult.STOPPED;
    }

    private TaskRunResult resolveStepResult(TaskStepResult stepResult, String stoppedLog, String failedLog) {
        if (stepResult == TaskStepResult.STOPPED) {
            log.info(stoppedLog);
            return TaskRunResult.STOPPED;
        }
        if (stepResult != TaskStepResult.SUCCESS) {
            log.error(failedLog);
            markTaskFailed();
            return TaskRunResult.FAILED;
        }
        return TaskRunResult.SUCCESS;
    }

    private void markTaskFailed() {
        gameContext.setBotStatus(GameContext.BotStatus.ERROR);
    }

    private void markTaskIdle() {
        gameContext.setBotStatus(GameContext.BotStatus.IDLE);
        gameContext.setCurrentActionState(GameContext.ActionState.FREE);
    }

    private TaskStepResult executePrepareBeforeRunStep(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        return executeStep(executionContext, "five-ring prepare before combat", context -> prepareBeforeRun(context, runtimeState), TaskRetryPolicy.none());
    }

    private TaskStepResult prepareBeforeRun(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        TaskTransactionOutcome outcome = taskTransactionRunner.run(
                "wuhuan:prepareBeforeRun",
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> doPrepareBeforeRun(executionContext, runtimeState));

        if (outcome.result() == TaskTransactionResult.STOPPED) {
            return TaskStepResult.STOPPED;
        }
        return outcome.reachedExpectedResult() ? TaskStepResult.SUCCESS : TaskStepResult.FAILED;
    }

    private TaskTransactionResult doPrepareBeforeRun(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        if (executionContext != null) {
            executionContext.throwIfStopRequested();
        }

        log.info("[five-ring prepare-1] clean maps/dialogs/common windows");
        gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        uiCleanerService.cleanUpAll();
        checkpoint(executionContext);

        log.info("[five-ring prepare-2] check sheyaoxiang status");
        playerStateService.ensureSheYaoXiangActiveForLeaderTask("five-ring:prepare", executionContext);
        checkpoint(executionContext);

        log.info("[five-ring prepare-4] scan bag item template: {}", KEY_ITEM_NAME);
        runtimeState.setShoeBagIndex(bagService.findItemPageIndex(BagService.MAIN_BAG, KEY_ITEM_NAME, executionContext));

        if (runtimeState.getShoeBagIndex() != null) {
            log.info("[five-ring prepare done] shoe found on page {}, ready to give", runtimeState.getShoeBagIndex() + 1);
        } else {
            log.warn("[five-ring prepare done] shoe template not found; continue because early runs may not have bought it yet");
        }

        return TaskTransactionResult.READY_TO_CONTINUE;
    }


    private TaskStepResult executeDetectHandoverStep(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        return executeStep(executionContext, "five-ring handover detection", context -> detectHandover(context, runtimeState), TaskRetryPolicy.none());
    }

    private TaskStepResult detectHandover(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        if (executionContext != null) {
            executionContext.throwIfStopRequested();
        }

        log.info("[five-ring handover] check whether an existing task can be taken over");
        PathingResult handoverPathingResult = runWuHuanHandoverTransaction();
        if (handoverPathingResult != null) {
            if (handoverPathingResult == PathingResult.SUCCESS) {
                log.info("handover detected: five-ring task already exists and pathing has been triggered");
                runtimeState.setHandoverState(FiveRingHandoverState.ALREADY_RUNNING);
                runtimeState.setNeedTaskSync(false);
                sleepSafely(executionContext, 1800);
                return TaskStepResult.SUCCESS;
            }
            if (handoverPathingResult == PathingResult.FINISHED) {
                log.info("five-ring handover: no existing task, need initial setup");
                runtimeState.setHandoverState(FiveRingHandoverState.NEED_SETUP);
                runtimeState.setNeedTaskSync(true);
                return TaskStepResult.SUCCESS;
            }

            log.warn("handover found task panel state but could not trigger P2/P1 pathing, keep takeover mode and resync in main loop");
            runtimeState.setHandoverState(FiveRingHandoverState.ALREADY_RUNNING);
            runtimeState.setNeedTaskSync(true);
            sleepSafely(executionContext, 1000);
            return TaskStepResult.SUCCESS;
        }

        boolean isTaskAlreadyRunning = questManager.activateTaskIfPresentExclusive("wuhuan", false);

        if (isTaskAlreadyRunning) {
            log.info("handover detected: five-ring task already exists, switching to takeover mode");
            runtimeState.setHandoverState(FiveRingHandoverState.ALREADY_RUNNING);
        } else {
            log.info("[five-ring handover] no running five-ring task found; initial setup is required");
            runtimeState.setHandoverState(FiveRingHandoverState.NEED_SETUP);
        }

        return TaskStepResult.SUCCESS;
    }

    private TaskStepResult executeSetupInitialTaskStep(TaskExecutionContext executionContext) {
        return executeStep(executionContext, "five-ring accept initial task", this::setupInitialTask, TaskRetryPolicy.none());
    }

    private TaskStepResult setupInitialTask(TaskExecutionContext executionContext) {
        int retry = 0;
        while (retry < MAX_RETRY) {
            if (executionContext != null) {
                executionContext.throwIfStopRequested();
            }

            if (tryAcceptInitialTaskFromCurrentScreen(executionContext, "setup:current-screen")) {
                return TaskStepResult.SUCCESS;
            }

            if (dialogService.detectDialogType() == DialogType.NONE) {
                if (!navigationService.navigateToNPC(NavigationRequest.builder()
                        .targetMapName(TARGET_MAP_NAME)
                        .targetX(NPC_COOR_X)
                        .targetY(NPC_COOR_Y)
                        .targetName(TARGET_NPC_NAME)
                        .source("wuhuan:acceptNpc:navigate")
                        .build()).success()) {
                    checkpoint(executionContext);
                    if (tryAcceptInitialTaskFromCurrentScreen(executionContext, "setup:navigate-failed")) {
                        return TaskStepResult.SUCCESS;
                    }
                    log.warn("[five-ring setup] failed to navigate near {} (retry {}/{})",
                            TARGET_NPC_NAME, retry + 1, MAX_RETRY);
                    retry++;
                    sleepSafely(executionContext, 2000);
                    continue;
                }
                checkpoint(executionContext);

                if (!clickInitialNpcForAccept(executionContext)) {
                    checkpoint(executionContext);
                    if (tryAcceptInitialTaskFromCurrentScreen(executionContext, "setup:npc-click-failed")) {
                        return TaskStepResult.SUCCESS;
                    }
                    log.warn("[five-ring setup] failed to click {} (retry {}/{})",
                            TARGET_NPC_NAME, retry + 1, MAX_RETRY);
                    retry++;
                    sleepSafely(executionContext, 2000);
                    continue;
                }
                checkpoint(executionContext);
            } else {
                log.info("detected existing five-ring accept dialog, skip navigation/NPC recognition and try accept option");
            }

            if (!acceptInitialDialogAndTriggerPathing(executionContext, "initialAcceptPathing")) {
                cleanupUnexpectedAcceptDialog("setup:accept-template-not-matched");
                retry++;
                sleepSafely(executionContext, 1000);
                continue;
            }

            log.info("[five-ring] initial task accepted and pathing started");
            return TaskStepResult.SUCCESS;
        }
        return TaskStepResult.FAILED;
    }

    private boolean tryAcceptInitialTaskFromCurrentScreen(TaskExecutionContext executionContext, String reason) {
        if (executionContext != null) {
            executionContext.throwIfStopRequested();
        }

        DialogType dialogType = dialogService.detectDialogType();
        if (dialogType == DialogType.NONE) {
            log.info("[five-ring accept] no accept dialog on current screen, skip direct accept: reason={}", reason);
            return false;
        }

        log.info("[five-ring accept] try accepting task from current screen: reason={}", reason);
        if (!acceptInitialDialogAndTriggerPathing(executionContext, "currentScreenAcceptPathing")) {
            log.info("[five-ring accept] current screen is not a five-ring accept dialog; clean unexpected option dialog: reason={}", reason);
            cleanupUnexpectedAcceptDialog(reason);
            return false;
        }

        log.info("current screen takeover succeeded: task radar confirmed five-ring accepted and pathing has been triggered");
        return true;
    }

    private boolean acceptInitialDialogAndTriggerPathing(TaskExecutionContext executionContext, String movementSource) {
        checkpoint(executionContext);
        AtomicReference<PathingResult> pathingResult = new AtomicReference<>(PathingResult.UI_ERROR);
        TaskTransactionOutcome outcome = taskTransactionRunner.runExclusive(
                "wuhuan:acceptDialogAndTriggerPathing",
                TaskTransactionResult.PATHING_STARTED,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
            boolean clickedAccept = dialogService.clickFirstGreenOptionIfFiveRingAcceptDialogDirectForExclusive();
            log.info("[five-ring accept] accept dialog click result={}", clickedAccept);
            if (!clickedAccept) {
                return TaskTransactionResult.RETRYABLE_ERROR;
            }

            sleepSafely(executionContext, 2000);
            pathingResult.set(questManager.activateAndTriggerWuHuanPathingDirectForExclusive());
            if (pathingResult.get() == PathingResult.SUCCESS) {
                gameStateUtil.recordMovementIntent("wuhuan:" + movementSource);
                sleepSafely(executionContext, 1800);
                return TaskTransactionResult.PATHING_STARTED;
            }
            if (pathingResult.get() == PathingResult.FINISHED) {
                return TaskTransactionResult.TASK_FINISHED;
            }
            return TaskTransactionResult.RETRYABLE_ERROR;
        });
        if (outcome.reachedExpectedResult() && pathingResult.get() == PathingResult.SUCCESS) {
            return true;
        }

        log.warn("five-ring initial accept confirmation did not reach pathing: source={} txResult={} pathingResult={}",
                movementSource, outcome.result(), pathingResult.get());
        return false;
    }

    private boolean clickInitialNpcForAccept(TaskExecutionContext executionContext) {
        TaskTransactionOutcome outcome = taskTransactionRunner.run(
                "wuhuan:clickInitialNpcForAccept",
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
                    checkpoint(executionContext);
                    boolean clicked = npcClickService.clickNpcSmart(fiveRingAcceptNpc().toClickRequest(gameContext.getMe()));
                    return clicked ? TaskTransactionResult.READY_TO_CONTINUE : TaskTransactionResult.RETRYABLE_ERROR;
                });
        if (outcome.result() == TaskTransactionResult.STOPPED) {
            checkpoint(executionContext);
        }
        return outcome.reachedExpectedResult();
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
                .tuneX(TUNE_X)
                .tuneY(TUNE_Y)
                .expectedDialogTemplatePath(ACCEPT_OPTION_TEMPLATE)
                .source("five-ring")
                .build();
    }

    private void cleanupUnexpectedAcceptDialog(String reason) {
        taskTransactionRunner.run(
                "wuhuan:cleanupUnexpectedAcceptDialog",
                TaskTransactionResult.RETRYABLE_ERROR,
                TaskYieldPolicy.RETRY_LATER,
                () -> {
                    uiCleanerService.cleanUpAll();
                    return TaskTransactionResult.RETRYABLE_ERROR;
                });
        log.info("[five-ring accept] unexpected accept dialog cleanup finished: reason={}", reason);
    }

    private TaskStepResult executeRunLoopOnceStep(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        return executeStep(executionContext, "five-ring single loop", context -> runLoopOnce(context, runtimeState), TaskRetryPolicy.none());
    }

    private TaskStepResult runLoopOnce(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        if (executionContext != null) {
            executionContext.throwIfStopRequested();
        }
        runtimeState.resetLoopDecision();

        if (handleCombatIfNeeded(executionContext, runtimeState)) {
            return TaskStepResult.SUCCESS;
        }

        if (waitIfMoving(executionContext)) {
            return TaskStepResult.SUCCESS;
        }

        if (handleDialogIfNeeded(executionContext, runtimeState)) {
            return TaskStepResult.SUCCESS;
        }

        if (runtimeState.isNeedTaskSync()) {
            PathingResult syncPathingResult = syncTaskStateAndTriggerPathing(executionContext, runtimeState, true);
            if (syncPathingResult == PathingResult.FINISHED) {
                runtimeState.setLoopDecision(FiveRingLoopDecision.FINISHED);
                return TaskStepResult.SUCCESS;
            }
            return TaskStepResult.SUCCESS;
        }

        triggerCombinedPathingOrRepairState(executionContext, runtimeState);
        return TaskStepResult.SUCCESS;
    }

    private boolean handleCombatIfNeeded(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        AutoCombatService.TickResult result = autoCombatService.handleCombatTick(
                executionContext, "five-ring", true);
        if (result == AutoCombatService.TickResult.NONE) {
            return false;
        }
        if (result == AutoCombatService.TickResult.EXIT_RECOVERED) {
            runtimeState.setNeedTaskSync(true);
            return true;
        }
        sleepSafely(executionContext, autoCombatService.getDynamicPollingIntervalMs());
        return true;
    }

    private boolean waitIfMoving(TaskExecutionContext executionContext) {
        if (!gameStateUtil.isMovingByPixelDiff()) {
            return false;
        }
        sleepSafely(executionContext, 800);
        return true;
    }

    private boolean handleDialogIfNeeded(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        checkpoint(executionContext);

        DialogHandleResult giveResult = tryGiveItemAndTriggerPathingIfPossible(executionContext, runtimeState);
        checkpoint(executionContext);
        if (giveResult == DialogHandleResult.GIVE_ITEM_DONE) {
            return true;
        }
        if (giveResult == DialogHandleResult.NO_DIALOG) {
            return false;
        }
        if (giveResult == DialogHandleResult.STORY_IGNORED) {
            log.info("five-ring story dialog ignored; continue task-panel P2/P1 advancement");
            return false;
        }
        if (giveResult == DialogHandleResult.GIVE_OPTION_NOT_FOUND) {
            log.warn("five-ring detected an unknown option dialog without give entry; clean it and retry later");
            cleanupRetryableDialog("wuhuan:giveOptionNotFound");
            runtimeState.setNeedTaskSync(true);
            return true;
        }
        if (giveResult == DialogHandleResult.GIVE_ITEM_FAILED || giveResult == DialogHandleResult.INTERRUPTED) {
            log.warn("five-ring give-item dialog handling failed, continue with P2/P1 fallback");
            return false;
        }

        return false;
    }

    private DialogHandleResult tryGiveItemAndTriggerPathingIfPossible(TaskExecutionContext executionContext,
                                                                      FiveRingRuntimeState runtimeState) {
        AtomicReference<DialogHandleResult> dialogResult = new AtomicReference<>(DialogHandleResult.NO_DIALOG);
        AtomicReference<PathingResult> pathingResult = new AtomicReference<>(PathingResult.UI_ERROR);

        TaskTransactionOutcome outcome = taskTransactionRunner.runExclusive(
                "wuhuan:giveItemAndTriggerPathing",
                TaskTransactionResult.PATHING_STARTED,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
            dialogResult.set(dialogService.tryGiveItemIfCurrentOptionDialogDirectForExclusive(
                    KEY_ITEM_NAME, runtimeState.getShoeBagIndex()));
            if (dialogResult.get() != DialogHandleResult.GIVE_ITEM_DONE) {
                return mapGiveDialogResultToTransactionResult(dialogResult.get());
            }

            log.info("five-ring give item done inside exclusive section, immediately trigger P2/P1 pathing");
            pathingResult.set(questManager.activateAndTriggerWuHuanPathingDirectForExclusive());
            if (pathingResult.get() == PathingResult.SUCCESS) {
                gameStateUtil.recordMovementIntent("wuhuan:giveItemPathing");
                sleepSafely(executionContext, 1800);
                return TaskTransactionResult.PATHING_STARTED;
            }
            if (pathingResult.get() == PathingResult.FINISHED) {
                return TaskTransactionResult.TASK_FINISHED;
            }
            return TaskTransactionResult.RETRYABLE_ERROR;
        });

        if (!outcome.completed()) {
            return DialogHandleResult.INTERRUPTED;
        }

        if (dialogResult.get() == DialogHandleResult.GIVE_ITEM_DONE) {
            if (pathingResult.get() == PathingResult.SUCCESS) {
                runtimeState.resetUiErrorCount();
                runtimeState.setNeedTaskSync(false);
                log.info("five-ring give item finished and P2/P1 pathing was triggered in the same input transaction");
            } else {
                runtimeState.setNeedTaskSync(true);
                log.warn("five-ring give item finished but P2/P1 pathing did not start in same transaction: result={}",
                        pathingResult.get());
            }
        }
        return dialogResult.get();
    }

    private TaskTransactionResult mapGiveDialogResultToTransactionResult(DialogHandleResult result) {
        return switch (result) {
            case NO_DIALOG, STORY_IGNORED -> TaskTransactionResult.READY_TO_CONTINUE;
            case GIVE_OPTION_NOT_FOUND -> TaskTransactionResult.RETRYABLE_ERROR;
            case INTERRUPTED -> TaskTransactionResult.STOPPED;
            case GIVE_ITEM_FAILED, FAILED -> TaskTransactionResult.FAILED;
            default -> TaskTransactionResult.READY_TO_CONTINUE;
        };
    }

    private void cleanupRetryableDialog(String reason) {
        taskTransactionRunner.run(
                "wuhuan:cleanupRetryableDialog",
                TaskTransactionResult.RETRYABLE_ERROR,
                TaskYieldPolicy.RETRY_LATER,
                () -> {
                    uiCleanerService.cleanUpAll();
                    return TaskTransactionResult.RETRYABLE_ERROR;
                });
        log.info("five-ring retryable dialog cleanup finished: reason={}", reason);
    }

    private PathingResult runWuHuanPathingTransaction(String name) {
        return runWuHuanPathingTransaction(name, null, TaskTransactionResult.TASK_FINISHED);
    }

    private PathingResult runWuHuanPathingTransaction(String name,
                                                      String movementIntentSource,
                                                      TaskTransactionResult finishedTransactionResult) {
        AtomicReference<PathingResult> pathingResult = new AtomicReference<>(PathingResult.UI_ERROR);
        TaskTransactionOutcome outcome = taskTransactionRunner.runExclusive(
                name,
                TaskTransactionResult.PATHING_STARTED,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
                    pathingResult.set(questManager.activateAndTriggerWuHuanPathingDirectForExclusive());
                    if (pathingResult.get() == PathingResult.SUCCESS && movementIntentSource != null) {
                        gameStateUtil.recordMovementIntent("wuhuan:" + movementIntentSource);
                    }
                    return mapPathingResultToTransactionResult(pathingResult.get(), finishedTransactionResult);
                });
        if (!outcome.completed()) {
            return PathingResult.UI_ERROR;
        }
        return pathingResult.get();
    }

    private PathingResult runWuHuanHandoverTransaction() {
        AtomicReference<PathingResult> pathingResult = new AtomicReference<>(PathingResult.UI_ERROR);
        TaskTransactionOutcome outcome = taskTransactionRunner.runExclusive(
                "wuhuan:handoverPathingTransaction",
                TaskTransactionResult.PATHING_STARTED,
                TaskYieldPolicy.CONTINUE_CHAIN,
                () -> {
                    pathingResult.set(questManager.activateAndTriggerWuHuanPathingDirectForExclusive());
                    if (pathingResult.get() == PathingResult.SUCCESS) {
                        gameStateUtil.recordMovementIntent("wuhuan:handoverPathing");
                        return TaskTransactionResult.PATHING_STARTED;
                    }
                    if (pathingResult.get() == PathingResult.FINISHED) {
                        return TaskTransactionResult.READY_TO_CONTINUE;
                    }
                    return TaskTransactionResult.RETRYABLE_ERROR;
                });
        if (!outcome.completed()) {
            return PathingResult.UI_ERROR;
        }
        return pathingResult.get();
    }

    private TaskTransactionResult mapPathingResultToTransactionResult(PathingResult result,
                                                                      TaskTransactionResult finishedTransactionResult) {
        return switch (result) {
            case SUCCESS -> TaskTransactionResult.PATHING_STARTED;
            case FINISHED -> finishedTransactionResult;
            case UI_ERROR -> TaskTransactionResult.RETRYABLE_ERROR;
        };
    }

    private PathingResult syncTaskStateAndTriggerPathing(TaskExecutionContext executionContext,
                                                         FiveRingRuntimeState runtimeState,
                                                         boolean allowFinished) {
        log.info("[five-ring sync] scan task panel and try direct P2/P1 pathing");
        checkpoint(executionContext);
        PathingResult pathingResult = runWuHuanPathingTransaction(
                "wuhuan:syncTaskAndPathing",
                "syncPathing",
                TaskTransactionResult.READY_TO_CONTINUE);
        checkpoint(executionContext);

        if (pathingResult == PathingResult.SUCCESS) {
            log.info("[five-ring sync] task locked and P2/P1 pathing triggered");
            runtimeState.resetUiErrorCount();
            runtimeState.setNeedTaskSync(false);
            sleepSafely(executionContext, 1800);
            return PathingResult.SUCCESS;
        }

        if (pathingResult == PathingResult.FINISHED) {
            log.warn("[five-ring sync] task panel looks empty; run UI cleanup before confirming finished state");
            runUiCleanupContinue("wuhuan:syncTaskEmptyCleanup");
            checkpoint(executionContext);

            PathingResult retryResult = runWuHuanPathingTransaction(
                    "wuhuan:syncTaskAndPathingAfterCleanup",
                    "syncPathingAfterCleanup",
                    allowFinished ? TaskTransactionResult.TASK_FINISHED : TaskTransactionResult.READY_TO_CONTINUE);
            checkpoint(executionContext);

            if (retryResult == PathingResult.SUCCESS) {
                log.info("[five-ring sync] task still exists after cleanup and P2/P1 pathing was triggered");
                runtimeState.resetUiErrorCount();
                runtimeState.setNeedTaskSync(false);
                sleepSafely(executionContext, 1800);
                return PathingResult.SUCCESS;
            }

            if (retryResult == PathingResult.FINISHED && allowFinished) {
                log.info("after cleanup, task panel is still empty; five-ring finished");
                markTaskIdle();
                return PathingResult.FINISHED;
            }

            runtimeState.setNeedTaskSync(true);
            return retryResult;
        }

        int errorCount = runtimeState.increaseUiErrorCount();
        log.warn("task panel exists but P2/P1 pathing failed during sync: errorCount={}", errorCount);
        if (errorCount >= 3) {
            log.error("task sync pathing failed 3 times, run UI cleanup");
            runUiCleanupRetryLater("wuhuan:syncTaskPathingFailedCleanup");
            checkpoint(executionContext);
            runtimeState.resetUiErrorCount();
        }
        runtimeState.setNeedTaskSync(true);
        sleepSafely(executionContext, 1000);
        return PathingResult.UI_ERROR;
    }

    private void triggerCombinedPathingOrRepairState(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        checkpoint(executionContext);
        PathingResult pathingResult = runWuHuanPathingTransaction(
                "wuhuan:combinedPathing",
                "combinedPathing",
                TaskTransactionResult.TASK_FINISHED);
        checkpoint(executionContext);
        if (pathingResult == PathingResult.SUCCESS) {
            runtimeState.resetUiErrorCount();
            log.info("[five-ring] task panel activated and P2/P1 pathing triggered");
            sleepSafely(executionContext, 1800);
            return;
        }

        if (pathingResult == PathingResult.FINISHED) {
            log.warn("combined pathing did not find five-ring task, request task resync");
            runtimeState.setNeedTaskSync(true);
            sleepSafely(executionContext, 1000);
            return;
        }

        int errorCount = runtimeState.increaseUiErrorCount();
        log.warn("combined P2/P1 pathing failed: errorCount={}", errorCount);
        if (errorCount >= 3) {
            log.error("combined pathing failed 3 times, run UI cleanup");
            runUiCleanupRetryLater("wuhuan:combinedPathingFailedCleanup");
            checkpoint(executionContext);
            runtimeState.resetUiErrorCount();
        }
        runtimeState.setNeedTaskSync(true);
        sleepSafely(executionContext, 1000);
    }

    private boolean triggerP2PathingIfPossible(TaskExecutionContext executionContext) {
        checkpoint(executionContext);
        PathingResult p2Result = questManager.triggerWuHuanNativePathingP2(true);
        checkpoint(executionContext);
        if (p2Result != PathingResult.SUCCESS) {
            return false;
        }

        log.info("[five-ring] target locked, P2 pathing triggered");
        sleepSafely(executionContext, 2000);
        return true;
    }

    private void triggerP1PathingOrRepairState(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        checkpoint(executionContext);
        PathingResult p1Result = questManager.triggerWuHuanNativePathingP1(true);
        checkpoint(executionContext);
        if (p1Result == PathingResult.SUCCESS) {
            runtimeState.resetUiErrorCount();
            log.info("[five-ring] P1 blind click triggered next NPC link");
            sleepSafely(executionContext, 1200);

            if (dialogService.detectDialogType() != DialogType.NONE) {
                checkpoint(executionContext);
                log.info("P1 click opened a dialog directly; blind click succeeded");
                return;
            }

            if (!gameStateUtil.isMovingByPixelDiff()) {
                log.warn("P1 blind click failed: not moving and no dialog, request task resync");
                runtimeState.setNeedTaskSync(true);
            }
            return;
        }

        if (p1Result == PathingResult.UI_ERROR) {
            int errorCount = runtimeState.increaseUiErrorCount();
            log.warn("task panel open failed or UI is abnormal, request task resync");
            if (errorCount >= 3) {
                log.error("task panel failed to open 3 times, run UI cleanup");
                runUiCleanupRetryLater("wuhuan:legacyP1PathingFailedCleanup");
                checkpoint(executionContext);
                runtimeState.resetUiErrorCount();
            }
            runtimeState.setNeedTaskSync(true);
            sleepSafely(executionContext, 1000);
        }
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

    private void checkpoint(TaskExecutionContext executionContext) {
        TaskCheckpoint.throwIfStopRequested(executionContext, "Five-ring task interrupted");
    }
}
