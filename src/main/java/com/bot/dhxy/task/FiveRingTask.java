package com.bot.dhxy.task;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.service.*;
import com.bot.dhxy.model.QuestTargetInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FiveRingTask {

    private final GameContext context;
    private final NavigationService navigationService;
    private final NpcClickService npcClickService;
    private final DialogService dialogService;
    private final PlayerStateService playerStateService;
    private final QuestManagerService questManager;
    private final BattleRadarService battleRadarService;

    private static final int DIALOG_START_OFFSET_X = 427;
    private static final int DIALOG_START_OFFSET_Y = 420;

    private final String targetMapName = "长安";
    private final String targetNPCName = "墨意";
    private final int npc_coor_x = 87;
    private final int npc_coor_y = 174;

    private static final String WUHUAN_TASK_IMAGE = "wuhuang_unselected.png"; // 记得去游戏里截个图放在 images/template/ 下

    // 🌟 首领特调的常量，打死不能删！
    private static final int TUNE_X = -10;
    private static final int TUNE_Y = 0;
    private static final String KEY_ITEM_NAME = "/wuhuan/shoe.png";

    private static final int MAX_RETRY = 5; // 全局最大允许重试次数


    public void execute() {
        log.info("====================================");
        log.info("🔥 启动【全自动五环印钞机】(高容错重试版)");
        log.info("====================================");

        playerStateService.syncAll();
        context.setBotStatus(GameContext.BotStatus.RUNNING);

        // ==========================================
        // 阶段一：死磕墨意 (接取总任务)
        // ==========================================
        log.info("▶️ 阶段一：前往长安寻找墨意接任务...");
        if (!setupInitialTask()) {
            log.error("❌ 经过多次重试，彻底无法接取起始任务，印钞机停机！");
            context.setBotStatus(GameContext.BotStatus.ERROR);
            return;
        }

        // 接完任务，进入核验状态
        context.setCurrentActionState(GameContext.ActionState.TASK_VERIFYING);
        sleep(2000);

        // ==========================================
        // 阶段二：无限跑环流水线 (严格 Switch 状态机)
        // ==========================================
        log.info("▶️ 阶段二：启动状态机驱动跑环引擎...");

        int emptyTaskCount = 0;
        int actionRetryCount = 0; // 🌟 动作重试计数器
        QuestTargetInfo currentTarget = null; // 当前目标情报

        while (context.getBotStatus() == GameContext.BotStatus.RUNNING) {

            // 🛡️ 战时管制：如果在打架，原地待命！
            if (battleRadarService.checkAndSyncCombatState()) {
                sleep(battleRadarService.getDynamicPollingIntervalMs());
                continue;
            }

            // 🧠 根据当前大脑的状态，执行对应的动作！(失败不退出，重试！)
            switch (context.getCurrentActionState()) {

                // ------------------------------------------------
                // 状态 A：情报核验中 (查 Alt+Q)
                // ------------------------------------------------
                case TASK_VERIFYING:
                case FREE:
                    currentTarget = questManager.fetchCurrentQuestInfo(WUHUAN_TASK_IMAGE);

                    if (currentTarget == null) {
                        emptyTaskCount++;
                        if (emptyTaskCount >= 3) {
                            log.info("🎉 连续 3 次确认任务栏空白，五环任务圆满结束！");
                            context.setBotStatus(GameContext.BotStatus.IDLE);
                            context.setCurrentActionState(GameContext.ActionState.FREE);
                            return; // 唯一正常结束的出口
                        }
                        log.warn("⚠️ 未读到任务，{} 秒后重试...", emptyTaskCount * 2);
                        sleep(2000);
                    } else {
                        // 成功拿到情报，清零计数器，流转到寻路状态！
                        emptyTaskCount = 0;
                        actionRetryCount = 0;
                        log.info("🎯 锁定新目标，准备出发...");
                        context.setCurrentActionState(GameContext.ActionState.NAVIGATING);
                    }
                    break;

                // ------------------------------------------------
                // 状态 B：寻路赶路中
                // ------------------------------------------------
                case NAVIGATING:
                    if (currentTarget == null) {
                        context.setCurrentActionState(GameContext.ActionState.TASK_VERIFYING); // 异常兜底
                        break;
                    }

                    boolean navSuccess = navigationService.navigateToNPC(
                            currentTarget.getMapName(), currentTarget.getTargetX(), currentTarget.getTargetY());

                    if (navSuccess) {
                        // 寻路成功，流转到交互状态！
                        actionRetryCount = 0;
                        context.setCurrentActionState(GameContext.ActionState.INTERACTING);
                    } else {
                        // 🌟 寻路失败：死磕重试！不准跳过！
                        actionRetryCount++;
                        log.warn("⚠️ 寻路卡住失败 (重试 {}/{})", actionRetryCount, MAX_RETRY);
                        if (actionRetryCount >= MAX_RETRY) {
                            log.error("❌ 寻路彻底卡死，触发防卡死回城重置机制...");
                            // TODO: 可以在这里加一个使用飞行符回长安的兜底操作
                            context.setCurrentActionState(GameContext.ActionState.TASK_VERIFYING); // 重新读一次任务确认下
                        }
                    }
                    break;

                // ------------------------------------------------
                // 状态 C：交互点 NPC
                // ------------------------------------------------
                case INTERACTING:
                    if (currentTarget == null) break;

                    boolean clickSuccess = npcClickService.clickNpcSmart(
                            context.getMe(), currentTarget.getMapName(),
                            currentTarget.getTargetX(), currentTarget.getTargetY(),
                            currentTarget.getNpcName(), 0, 0);

                    if (clickSuccess) {
                        // 🌟 点击成功，处理对话框，流转回核验状态！
                        //dialogService.handleDialog(null, null);
                        actionRetryCount = 0;
                        log.info("✨ 当前环节动作结束，等待战术核验...");
                        context.setCurrentActionState(GameContext.ActionState.TASK_VERIFYING);
                        sleep(2000);
                    } else {
                        // 🌟 点击失败：死磕重试！可能是被人挡住了！
                        actionRetryCount++;
                        log.warn("⚠️ 无法点中目标 NPC [{}] (重试 {}/{})", currentTarget.getNpcName(), actionRetryCount, MAX_RETRY);
                        sleep(1500); // 等一下，可能挡住 NPC 的玩家走开了

                        if (actionRetryCount >= MAX_RETRY) {
                            log.error("❌ NPC 彻底点不到，强行退回寻路状态稍微走动一下...");
                            actionRetryCount = 0;
                            // 退回寻路状态，让寻路模块重新走两步
                            context.setCurrentActionState(GameContext.ActionState.NAVIGATING);
                        }
                    }
                    break;

                default:
                    sleep(1000);
                    break;
            }
        }
        log.info("🎉 印钞机停机！");
    }

    /**
     * 独立封装的接取初始任务逻辑 (自带死磕重试机制)
     */
    private boolean setupInitialTask() {
        int retry = 0;
        while (retry < MAX_RETRY) {
            // 1. 寻路
            context.setCurrentActionState(GameContext.ActionState.NAVIGATING);
            if (!navigationService.navigateToNPC(targetMapName, npc_coor_x, npc_coor_y)) {
                log.warn("⚠️ 无法到达墨意身边 (重试 {}/{})", retry + 1, MAX_RETRY);
                retry++;
                sleep(2000);
                continue;
            }

            // 2. 点击
            context.setCurrentActionState(GameContext.ActionState.INTERACTING);
            if (!npcClickService.clickNpcSmart(context.getMe(), targetMapName, npc_coor_x, npc_coor_y, targetNPCName, TUNE_X, TUNE_Y)) {
                log.warn("⚠️ 无法点中墨意 (重试 {}/{})", retry + 1, MAX_RETRY);
                retry++;
                sleep(2000);
                continue;
            }

            // 3. 对话
            dialogService.acceptTask(DIALOG_START_OFFSET_X, DIALOG_START_OFFSET_Y);
            log.info("✅ 成功接取五环总任务！");
            return true;
        }
        return false;
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}