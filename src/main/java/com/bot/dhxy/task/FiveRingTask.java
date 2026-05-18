package com.bot.dhxy.task;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.TaskExecutionContext;
import com.bot.dhxy.runner.TaskRetryPolicy;
import com.bot.dhxy.service.*;
import com.bot.dhxy.service.QuestManagerService.PathingResult;
import com.bot.dhxy.task.template.TaskStep;
import com.bot.dhxy.task.template.TaskStepExecutor;
import com.bot.dhxy.task.template.TaskStepResult;
import com.bot.dhxy.tools.GameStateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class FiveRingTask implements GameTask {

    private final GameContext context;
    private final NavigationService navigationService;
    private final NpcClickService npcClickService;
    private final DialogService dialogService;
    private final PlayerStateService playerStateService;
    private final QuestManagerService questManager;
    private final BattleRadarService battleRadarService;
    private final BagService bagService;
    private final GameStateUtil gameStateUtil;
    private final UICleanerService uiCleanerService;
    private final TaskStepExecutor taskStepExecutor;

    private static final int DIALOG_START_OFFSET_X = 427;
    private static final int DIALOG_START_OFFSET_Y = 420;

    private final String targetMapName = "长安";
    private final String targetNPCName = "墨意";
    private final int npc_coor_x = 87;
    private final int npc_coor_y = 174;

    private static final int TUNE_X = -10;
    private static final int TUNE_Y = 0;
    private static final String KEY_ITEM_NAME = "wuhuan/shoe.png";

    private static final int MAX_RETRY = 5;

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
        context.setBotStatus(GameContext.BotStatus.IDLE);
        context.setCurrentActionState(GameContext.ActionState.FREE);
    }

    @Override
    public TaskRunResult execute() {
        log.info("====================================");
        log.info("🔥 启动【全自动五环印钞机】(极速退出优化版)");
        log.info("====================================");

        TaskExecutionContext executionContext = buildStepExecutionContext();

        playerStateService.syncAll();
        context.setBotStatus(GameContext.BotStatus.RUNNING);
        uiCleanerService.cleanUpAll();
        playerStateService.ensureSheYaoXiangActive();
        log.info("▶️ 战前准备：清点背包物资，寻找特征 [{}]...", KEY_ITEM_NAME);
        Integer shoeBagIndex = bagService.findItemPageIndex(BagService.MAIN_BAG, KEY_ITEM_NAME);

        if (shoeBagIndex != null) {
            log.info("✅ 情报确认：鞋子在第 {} 页，随时准备上交！", shoeBagIndex + 1);
        } else {
            log.warn("⚠️ 情报确认：没发现鞋子！可能是刚开始跑还没买，继续执行...");
        }

        boolean needTaskSync = true;

        log.info("▶️ 阶段一：正在进行【中途接管】侦测...");
        boolean isTaskAlreadyRunning = questManager.activateTaskIfPresent("wuhuan");

        if (isTaskAlreadyRunning) {
            log.info("✅ 侦测到五环任务已经在列表中！切入无缝接管模式！");
            needTaskSync = false;
        } else {
            log.info("▶️ 未发现进行中的五环，前往长安寻找墨意接取初始任务...");
            TaskStepResult setupResult = executeSetupInitialTaskStep(executionContext);
            if (setupResult == TaskStepResult.STOPPED) {
                log.info("🎉 五环任务在接取初始任务阶段停止");
                return TaskRunResult.STOPPED;
            }
            if (setupResult != TaskStepResult.SUCCESS) {
                log.error("❌ 经过多次重试，彻底无法接取起始任务，印钞机停机！");
                context.setBotStatus(GameContext.BotStatus.ERROR);
                return TaskRunResult.FAILED;
            }
            sleep(2000);
            needTaskSync = true;
        }

        log.info("▶️ 阶段二：启动极速跑环流水线...");
        int errorCount = 0;
        while (context.getBotStatus() == GameContext.BotStatus.RUNNING) {

            if (battleRadarService.checkAndSyncCombatState()) {
                sleep(battleRadarService.getDynamicPollingIntervalMs());
                continue;
            }

            if (gameStateUtil.isMovingByPixelDiff()) {
                sleep(800);
                continue;
            }

            boolean hasDialogProcessed = dialogService.handleDialog(null, null, KEY_ITEM_NAME, shoeBagIndex);
            if (hasDialogProcessed) {
                log.info("💬 成功粉碎了一个对话框！");
                continue;
            }

            if (needTaskSync) {
                log.info("🔍 [状态查岗] 触发全量任务雷达扫描...");
                boolean hasTask = questManager.activateTaskIfPresent("wuhuan", true);

                if (!hasTask) {
                    log.warn("⚠️ 任务栏疑似清空，出动特警队洗地，防止被广告遮挡误判！");
                    uiCleanerService.cleanUpAll();

                    boolean realHasTask = questManager.activateTaskIfPresent("wuhuan", true);

                    if (!realHasTask) {
                        log.info("🎉 洗地后确认任务栏确实已清空，五环任务真·圆满结束！下班！");
                        context.setBotStatus(GameContext.BotStatus.IDLE);
                        context.setCurrentActionState(GameContext.ActionState.FREE);
                        return TaskRunResult.SUCCESS;
                    } else {
                        log.info("😅 虚惊一场！洗地后发现任务其实还在，只是刚才被挡住了，继续干活！");
                        needTaskSync = false;
                    }
                } else {
                    log.info("✅ [状态查岗] 五环任务已重新锁定！");
                    needTaskSync = false;
                }
            }

            PathingResult p2Result = questManager.triggerWuHuanNativePathingP2(true);
            if (p2Result == PathingResult.SUCCESS) {
                log.info("🏃 锁定怪物，引擎轰鸣，全速追击！");
                sleep(2000);
                continue;
            }

            PathingResult p1Result = questManager.triggerWuHuanNativePathingP1(true);
            if (p1Result == PathingResult.SUCCESS) {
                errorCount = 0;
                log.info("🏃 尝试点击下一环 NPC 链接...");
                sleep(2500);

                if (!gameStateUtil.isMovingByPixelDiff()) {
                    log.warn("⚠️ 盲狙 NPC 失败（角色未移动），状态发生错乱，请求重新查岗！");
                    needTaskSync = true;
                }
                continue;
            } else if (p1Result == PathingResult.UI_ERROR) {
                errorCount++;
                log.warn("⚠️ 界面打开失败或异常，请求重新查岗！");
                if (errorCount >= 3) {
                    log.error("💥 连续 3 次打不开面板，触发特警队洗地！");
                    uiCleanerService.cleanUpAll();
                    errorCount = 0;
                }
                needTaskSync = true;
                sleep(1000);
            }
        }
        log.info("🎉 印钞机停机！");
        return TaskRunResult.STOPPED;
    }

    private TaskStepResult executeSetupInitialTaskStep(TaskExecutionContext executionContext) {
        TaskStep setupInitialTaskStep = new TaskStep() {
            @Override
            public TaskStepResult execute(TaskExecutionContext context) {
                return setupInitialTask(context);
            }

            @Override
            public String getStepName() {
                return "接取五环初始任务";
            }
        };
        return taskStepExecutor.execute(executionContext, setupInitialTaskStep, TaskRetryPolicy.none());
    }

    private TaskStepResult setupInitialTask(TaskExecutionContext executionContext) {
        int retry = 0;
        while (retry < MAX_RETRY) {
            if (executionContext != null) {
                executionContext.throwIfStopRequested();
            }

            if (!navigationService.navigateToNPC(targetMapName, npc_coor_x, npc_coor_y)) {
                log.warn("⚠️ 无法到达墨意身边 (重试 {}/{})", retry + 1, MAX_RETRY);
                retry++;
                sleep(2000);
                continue;
            }

            if (!npcClickService.clickNpcSmart(context.getMe(), targetMapName, npc_coor_x, npc_coor_y, targetNPCName, TUNE_X, TUNE_Y)) {
                log.warn("⚠️ 无法点中墨意 (重试 {}/{})", retry + 1, MAX_RETRY);
                retry++;
                sleep(2000);
                continue;
            }

            dialogService.acceptTask(DIALOG_START_OFFSET_X, DIALOG_START_OFFSET_Y);

            log.info("⏳ 点击了接任务，等待服务器响应确认...");
            sleep(2000);

            boolean reallyGotTask = questManager.activateTaskIfPresent("wuhuan", false);

            if (reallyGotTask) {
                log.info("✅ 雷达确认：成功接取五环总任务！");
                return TaskStepResult.SUCCESS;
            } else {
                log.warn("⚠️ 疑似卡顿：点完对话框但任务没进列表！(重试 {}/{})", retry + 1, MAX_RETRY);
                retry++;
                sleep(1500);
            }
        }
        return TaskStepResult.FAILED;
    }

    private TaskExecutionContext buildStepExecutionContext() {
        return TaskExecutionContext.builder()
                .taskCode(getTaskCode())
                .taskName(getTaskName())
                .retryPolicy(TaskRetryPolicy.none())
                .startedAt(LocalDateTime.now())
                .build();
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}
