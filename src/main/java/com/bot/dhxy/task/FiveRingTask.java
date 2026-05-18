package com.bot.dhxy.task;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.service.*;
import com.bot.dhxy.service.QuestManagerService.PathingResult;
import com.bot.dhxy.task.startup.TaskStartupCheckResult;
import com.bot.dhxy.task.startup.TaskStartupCheckService;
import com.bot.dhxy.task.template.BaseTaskTemplate;
import com.bot.dhxy.task.template.TaskStepExecutor;
import com.bot.dhxy.task.template.TaskStepResult;
import com.bot.dhxy.task.wuhuan.FiveRingHandoverState;
import com.bot.dhxy.task.wuhuan.FiveRingLoopDecision;
import com.bot.dhxy.task.wuhuan.FiveRingRuntimeState;
import com.bot.dhxy.task.wuhuan.FiveRingTaskSyncDecision;
import com.bot.dhxy.tools.GameStateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class FiveRingTask extends BaseTaskTemplate {

    private final NavigationService navigationService;
    private final NpcClickService npcClickService;
    private final DialogService dialogService;
    private final PlayerStateService playerStateService;
    private final QuestManagerService questManager;
    private final BattleRadarService battleRadarService;
    private final BagService bagService;
    private final GameStateUtil gameStateUtil;
    private final UICleanerService uiCleanerService;
    private final TaskStartupCheckService taskStartupCheckService;

    private static final int DIALOG_START_OFFSET_X = 427;
    private static final int DIALOG_START_OFFSET_Y = 420;

    private static final String TARGET_MAP_NAME = "长安";
    private static final String TARGET_NPC_NAME = "墨意";
    private static final int NPC_COOR_X = 87;
    private static final int NPC_COOR_Y = 174;

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
                        BattleRadarService battleRadarService,
                        BagService bagService,
                        GameStateUtil gameStateUtil,
                        UICleanerService uiCleanerService,
                        TaskStepExecutor taskStepExecutor,
                        TaskStartupCheckService taskStartupCheckService) {
        super(context, taskStepExecutor);
        this.navigationService = navigationService;
        this.npcClickService = npcClickService;
        this.dialogService = dialogService;
        this.playerStateService = playerStateService;
        this.questManager = questManager;
        this.battleRadarService = battleRadarService;
        this.bagService = bagService;
        this.gameStateUtil = gameStateUtil;
        this.uiCleanerService = uiCleanerService;
        this.taskStartupCheckService = taskStartupCheckService;
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
        log.info("🛑 收到停止五环任务请求");
        markTaskIdle();
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        logTaskBanner();

        TaskExecutionContext context = resolveExecutionContext(executionContext);
        TaskStartupCheckResult checkResult = taskStartupCheckService.checkFiveRing(context);
        if (checkResult.isBlocked()) {
            log.info("五环前置判断未通过：{}", checkResult.getReason());
            return checkResult.getBlockedResult();
        }
        log.info("五环前置判断通过：{}", checkResult.getReason());

        FiveRingRuntimeState runtimeState = new FiveRingRuntimeState();

        TaskRunResult startupResult = runStartupSteps(context, runtimeState);
        if (startupResult != TaskRunResult.SUCCESS) {
            return startupResult;
        }

        return runMainLoop(context, runtimeState);
    }

    private void logTaskBanner() {
        log.info("====================================");
        log.info("🔥 启动【全自动五环印钞机】(极速退出优化版)");
        log.info("====================================");
    }

    private TaskRunResult runStartupSteps(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        TaskRunResult prepareResult = resolveStepResult(
                executePrepareBeforeRunStep(executionContext, runtimeState),
                "🎉 五环任务在战前准备阶段停止",
                "❌ 五环战前准备失败，任务终止！"
        );
        if (prepareResult != TaskRunResult.SUCCESS) {
            return prepareResult;
        }

        TaskRunResult handoverResult = resolveStepResult(
                executeDetectHandoverStep(executionContext, runtimeState),
                "🎉 五环任务在中途接管侦测阶段停止",
                "❌ 五环中途接管侦测失败，任务终止！"
        );
        if (handoverResult != TaskRunResult.SUCCESS) {
            return handoverResult;
        }
        if (runtimeState.getHandoverState() == null) {
            log.error("❌ 五环中途接管侦测失败，任务状态为空，任务终止！");
            markTaskFailed();
            return TaskRunResult.FAILED;
        }

        runtimeState.setNeedTaskSync(runtimeState.getHandoverState() != FiveRingHandoverState.ALREADY_RUNNING);

        if (runtimeState.getHandoverState() == FiveRingHandoverState.NEED_SETUP) {
            log.info("▶️ 未发现进行中的五环，前往长安寻找墨意接取初始任务...");
            TaskRunResult setupResult = resolveStepResult(
                    executeSetupInitialTaskStep(executionContext),
                    "🎉 五环任务在接取初始任务阶段停止",
                    "❌ 经过多次重试，彻底无法接取起始任务，印钞机停机！"
            );
            if (setupResult != TaskRunResult.SUCCESS) {
                return setupResult;
            }
            sleepSafely(executionContext, 2000);
            runtimeState.setNeedTaskSync(true);
        }

        return TaskRunResult.SUCCESS;
    }

    private TaskRunResult runMainLoop(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        log.info("▶️ 阶段二：启动极速跑环流水线...");
        while (gameContext.getBotStatus() == GameContext.BotStatus.RUNNING) {
            TaskRunResult loopResult = resolveStepResult(
                    executeRunLoopOnceStep(executionContext, runtimeState),
                    "🎉 五环任务在跑环单轮处理阶段停止",
                    "❌ 五环跑环单轮处理失败，任务终止！"
            );
            if (loopResult != TaskRunResult.SUCCESS) {
                return loopResult;
            }
            if (runtimeState.getLoopDecision() == FiveRingLoopDecision.FINISHED) {
                return TaskRunResult.SUCCESS;
            }
        }

        log.info("🎉 印钞机停机！");
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
        return executeStep(executionContext, "五环战前准备", context -> prepareBeforeRun(context, runtimeState), TaskRetryPolicy.none());
    }

    private TaskStepResult prepareBeforeRun(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        if (executionContext != null) {
            executionContext.throwIfStopRequested();
        }

        playerStateService.syncAll();
        gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        uiCleanerService.cleanUpAll();
        playerStateService.ensureSheYaoXiangActive();

        log.info("▶️ 战前准备：清点背包物资，寻找特征 [{}]...", KEY_ITEM_NAME);
        runtimeState.setShoeBagIndex(bagService.findItemPageIndex(BagService.MAIN_BAG, KEY_ITEM_NAME, executionContext));

        if (runtimeState.getShoeBagIndex() != null) {
            log.info("✅ 情报确认：鞋子在第 {} 页，随时准备上交！", runtimeState.getShoeBagIndex() + 1);
        } else {
            log.warn("⚠️ 情报确认：没发现鞋子！可能是刚开始跑还没买，继续执行...");
        }

        return TaskStepResult.SUCCESS;
    }

    private TaskStepResult executeDetectHandoverStep(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        return executeStep(executionContext, "五环中途接管侦测", context -> detectHandover(context, runtimeState), TaskRetryPolicy.none());
    }

    private TaskStepResult detectHandover(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        if (executionContext != null) {
            executionContext.throwIfStopRequested();
        }

        log.info("▶️ 阶段一：正在进行【中途接管】侦测...");
        boolean isTaskAlreadyRunning = questManager.activateTaskIfPresent("wuhuan");

        if (isTaskAlreadyRunning) {
            log.info("✅ 侦测到五环任务已经在列表中！切入无缝接管模式！");
            runtimeState.setHandoverState(FiveRingHandoverState.ALREADY_RUNNING);
        } else {
            log.info("🧭 未发现进行中的五环，需要先接取初始任务");
            runtimeState.setHandoverState(FiveRingHandoverState.NEED_SETUP);
        }

        return TaskStepResult.SUCCESS;
    }

    private TaskStepResult executeSetupInitialTaskStep(TaskExecutionContext executionContext) {
        return executeStep(executionContext, "接取五环初始任务", this::setupInitialTask, TaskRetryPolicy.none());
    }

    private TaskStepResult setupInitialTask(TaskExecutionContext executionContext) {
        int retry = 0;
        while (retry < MAX_RETRY) {
            if (executionContext != null) {
                executionContext.throwIfStopRequested();
            }

            if (!navigationService.navigateToNPC(TARGET_MAP_NAME, NPC_COOR_X, NPC_COOR_Y)) {
                log.warn("⚠️ 无法到达墨意身边 (重试 {}/{})", retry + 1, MAX_RETRY);
                retry++;
                sleepSafely(executionContext, 2000);
                continue;
            }

            if (!npcClickService.clickNpcSmart(gameContext.getMe(), TARGET_MAP_NAME, NPC_COOR_X, NPC_COOR_Y, TARGET_NPC_NAME, TUNE_X, TUNE_Y)) {
                log.warn("⚠️ 无法点中墨意 (重试 {}/{})", retry + 1, MAX_RETRY);
                retry++;
                sleepSafely(executionContext, 2000);
                continue;
            }

            dialogService.acceptTask(DIALOG_START_OFFSET_X, DIALOG_START_OFFSET_Y);

            log.info("⏳ 点击了接任务，等待服务器响应确认...");
            sleepSafely(executionContext, 2000);

            boolean reallyGotTask = questManager.activateTaskIfPresent("wuhuan", false);

            if (reallyGotTask) {
                log.info("✅ 雷达确认：成功接取五环总任务！");
                return TaskStepResult.SUCCESS;
            } else {
                log.warn("⚠️ 疑似卡顿：点完对话框但任务没进列表！(重试 {}/{})", retry + 1, MAX_RETRY);
                retry++;
                sleepSafely(executionContext, 1500);
            }
        }
        return TaskStepResult.FAILED;
    }

    private TaskStepResult executeRunLoopOnceStep(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        return executeStep(executionContext, "五环跑环单轮处理", context -> runLoopOnce(context, runtimeState), TaskRetryPolicy.none());
    }

    private TaskStepResult runLoopOnce(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        if (executionContext != null) {
            executionContext.throwIfStopRequested();
        }
        runtimeState.resetLoopDecision();

        if (handleCombatIfNeeded(executionContext)) {
            return TaskStepResult.SUCCESS;
        }

        if (waitIfMoving(executionContext)) {
            return TaskStepResult.SUCCESS;
        }

        if (handleDialogIfNeeded(runtimeState.getShoeBagIndex())) {
            return TaskStepResult.SUCCESS;
        }

        if (runtimeState.isNeedTaskSync()) {
            FiveRingTaskSyncDecision syncDecision = syncTaskState(runtimeState);
            if (syncDecision == FiveRingTaskSyncDecision.FINISHED) {
                runtimeState.setLoopDecision(FiveRingLoopDecision.FINISHED);
                return TaskStepResult.SUCCESS;
            }
        }

        if (triggerP2PathingIfPossible(executionContext)) {
            return TaskStepResult.SUCCESS;
        }

        triggerP1PathingOrRepairState(executionContext, runtimeState);
        return TaskStepResult.SUCCESS;
    }

    private boolean handleCombatIfNeeded(TaskExecutionContext executionContext) {
        if (!battleRadarService.checkAndSyncCombatState()) {
            return false;
        }
        sleepSafely(executionContext, battleRadarService.getDynamicPollingIntervalMs());
        return true;
    }

    private boolean waitIfMoving(TaskExecutionContext executionContext) {
        if (!gameStateUtil.isMovingByPixelDiff()) {
            return false;
        }
        sleepSafely(executionContext, 800);
        return true;
    }

    private boolean handleDialogIfNeeded(Integer shoeBagIndex) {
        boolean hasDialogProcessed = dialogService.handleDialog(null, null, KEY_ITEM_NAME, shoeBagIndex);
        if (hasDialogProcessed) {
            log.info("💬 成功粉碎了一个对话框！");
        }
        return hasDialogProcessed;
    }

    private FiveRingTaskSyncDecision syncTaskState(FiveRingRuntimeState runtimeState) {
        log.info("🔍 [状态查岗] 触发全量任务雷达扫描...");
        boolean hasTask = questManager.activateTaskIfPresent("wuhuan", true);

        if (!hasTask) {
            log.warn("⚠️ 任务栏疑似清空，出动特警队洗地，防止被广告遮挡误判！");
            uiCleanerService.cleanUpAll();

            boolean realHasTask = questManager.activateTaskIfPresent("wuhuan", true);

            if (!realHasTask) {
                log.info("🎉 洗地后确认任务栏确实已清空，五环任务真·圆满结束！下班！");
                markTaskIdle();
                return FiveRingTaskSyncDecision.FINISHED;
            }

            log.info("😅 虚惊一场！洗地后发现任务其实还在，只是刚才被挡住了，继续干活！");
            runtimeState.setNeedTaskSync(false);
            return FiveRingTaskSyncDecision.CONTINUE;
        }

        log.info("✅ [状态查岗] 五环任务已重新锁定！");
        runtimeState.setNeedTaskSync(false);
        return FiveRingTaskSyncDecision.CONTINUE;
    }

    private boolean triggerP2PathingIfPossible(TaskExecutionContext executionContext) {
        PathingResult p2Result = questManager.triggerWuHuanNativePathingP2(true);
        if (p2Result != PathingResult.SUCCESS) {
            return false;
        }

        log.info("🏃 锁定怪物，引擎轰鸣，全速追击！");
        sleepSafely(executionContext, 2000);
        return true;
    }

    private void triggerP1PathingOrRepairState(TaskExecutionContext executionContext, FiveRingRuntimeState runtimeState) {
        PathingResult p1Result = questManager.triggerWuHuanNativePathingP1(true);
        if (p1Result == PathingResult.SUCCESS) {
            runtimeState.resetUiErrorCount();
            log.info("🏃 尝试点击下一环 NPC 链接...");
            sleepSafely(executionContext, 2500);

            if (!gameStateUtil.isMovingByPixelDiff()) {
                log.warn("⚠️ 盲狙 NPC 失败（角色未移动），状态发生错乱，请求重新查岗！");
                runtimeState.setNeedTaskSync(true);
            }
            return;
        }

        if (p1Result == PathingResult.UI_ERROR) {
            int errorCount = runtimeState.increaseUiErrorCount();
            log.warn("⚠️ 界面打开失败或异常，请求重新查岗！");
            if (errorCount >= 3) {
                log.error("💥 连续 3 次打不开面板，触发特警队洗地！");
                uiCleanerService.cleanUpAll();
                runtimeState.resetUiErrorCount();
            }
            runtimeState.setNeedTaskSync(true);
            sleepSafely(executionContext, 1000);
        }
    }
}
