package com.bot.dhxy.task;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.service.*;
import com.bot.dhxy.service.QuestManagerService.PathingResult; // 🌟 引入咱们定义的三态枚举
import com.bot.dhxy.tools.GameStateUtil;
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
    private final BagService bagService; // 🌟 注入您心心念念的万能包裹引擎

    private static final int DIALOG_START_OFFSET_X = 427;
    private static final int DIALOG_START_OFFSET_Y = 420;

    private final String targetMapName = "长安";
    private final String targetNPCName = "墨意";
    private final int npc_coor_x = 87;
    private final int npc_coor_y = 174;

    // 🌟 首领特调的常量，打死不能删！
    private static final int TUNE_X = -10;
    private static final int TUNE_Y = 0;
    // 稍微修剪了开头的 "/" 以适配 ImageFinder 的相对路径要求
    private static final String KEY_ITEM_NAME = "wuhuan/shoe.png";

    private static final int MAX_RETRY = 5; // 用于接初始任务的兜底
    private final GameStateUtil gameStateUtil;

    public void execute() {
        log.info("====================================");
        log.info("🔥 启动【全自动五环印钞机】(纯享版: 无状态响应式架构)");
        log.info("====================================");

        playerStateService.syncAll();
        context.setBotStatus(GameContext.BotStatus.RUNNING);

        // ==========================================
        // 🌟 战前准备：情报收集
        // ==========================================
        log.info("▶️ 战前准备：清点背包物资，寻找特征 [{}]...", KEY_ITEM_NAME);
        // 使用只读模式寻找鞋子，绝不点击！
        Integer shoeBagIndex = bagService.findItemPageIndex(BagService.MAIN_BAG, KEY_ITEM_NAME);

        if (shoeBagIndex != null) {
            log.info("✅ 情报确认：鞋子在第 {} 页，随时准备上交！", shoeBagIndex + 1);
        } else {
            log.warn("⚠️ 情报确认：没发现鞋子！可能是刚开始跑还没买，继续执行...");
        }

        // ==========================================
        // ==========================================
        // 🌟 阶段一：中途接管侦测 (热启动)
        // ==========================================
        log.info("▶️ 阶段一：正在进行【中途接管】侦测...");

        boolean isTaskAlreadyRunning = questManager.activateTaskIfPresent("wuhuan");
        if (isTaskAlreadyRunning) {
            log.info("✅ 侦测到五环任务已经在列表中！跳过起始 NPC 交涉，直接切入无缝接管模式！");
        } else {
            log.info("▶️ 未发现进行中的五环，前往长安寻找墨意接取初始任务...");
            if (!setupInitialTask()) {
                log.error("❌ 经过多次重试，彻底无法接取起始任务，印钞机停机！");
                context.setBotStatus(GameContext.BotStatus.ERROR);
                return;
            }
            sleep(2000); // 等待任务刷新到 Alt+Q 面板
        }

        // ==========================================
        // 阶段二：无限跑环流水线 (降维打击：纯响应式行为树)
        // ==========================================
        // ... (下方的 while 循环代码完全不用动) ...

        // ==========================================
        // 阶段二：无限跑环流水线 (降维打击：纯响应式行为树)
        // ==========================================
        log.info("▶️ 阶段二：启动无状态响应式跑环引擎...");

        int finishConfirmCount = 0; // 专属防抖计数器

        while (context.getBotStatus() == GameContext.BotStatus.RUNNING) {

            // 🥇 绝对优先级 1：战斗挂起 (雷达发现交火，立刻原地装死)
            if (battleRadarService.checkAndSyncCombatState()) {
                sleep(battleRadarService.getDynamicPollingIntervalMs());
                continue;
            }

            // 🥇 绝对优先级 2：跑路挂起 (只要角色在移动，绝对不抢鼠标按 Alt+Q！)
            // ⚠️ 注意：这里假设您的 playerStateService 中有 isMoving() 方法，如果没有，请换成您对应的移动检测判定。
            if (gameStateUtil.isMovingByPixelDiff()) {
                sleep(800); // 让子弹飞一会儿
                continue;
            }

            // 🥈 突发层：对话框绞肉机 (彻底干掉原来的 P2 和 INTERACTING 状态)
            // 这里调用咱们升级后的带情报透传的 handleDialog
            boolean hasDialogProcessed = dialogService.handleDialog(null, null, KEY_ITEM_NAME, shoeBagIndex);
            if (hasDialogProcessed) {
                log.info("💬 成功粉碎了一个对话框！");
                finishConfirmCount = 0; // 干活了，清零防抖
                continue; // 对话处理完，立刻进入下一轮查岗
            }

            // --- 走到这里，说明角色肯定像个木头人一样站着发呆，没打架、没跑路、没弹窗 ---

            // 🥉 业务层 1：找怪 (优先执行 P3 打怪)
            PathingResult p3Result = questManager.triggerWuHuanNativePathingP2();
            if (p3Result == PathingResult.SUCCESS) {
                finishConfirmCount = 0;
                log.info("🏃 锁定怪物，引擎轰鸣，全速追击！");
                sleep(2000); // 稍微睡一下等角色起步，下一次循环就会被 isMoving() 完美接管
                continue;
            }

            // 🥉 业务层 2：找怪失败，那肯定是找人 (执行 P1 找替身)
            PathingResult p1Result = questManager.triggerWuHuanNativePathingP1();
            if (p1Result == PathingResult.SUCCESS) {
                finishConfirmCount = 0;
                log.info("🏃 锁定下一环 NPC，全速前往交涉！");
                sleep(2000); // 稍微睡一下等起步
                continue;
            } else if (p1Result == PathingResult.FINISHED) {
                // 🏁 收工防抖层
                finishConfirmCount++;
                if (finishConfirmCount >= 3) {
                    log.info("🎉 连续 3 次确认任务栏清空，五环任务圆满结束！");
                    context.setBotStatus(GameContext.BotStatus.IDLE);
                    context.setCurrentActionState(GameContext.ActionState.FREE);
                    return; // 唯一正常结束的出口
                }
                log.warn("⚠️ 面板未发现五环，可能是延迟，{} 秒后进行第 {} 次最终核实...",
                        finishConfirmCount * 2, finishConfirmCount + 1);
                sleep(2000);
            } else {
                // p1Result == UI_ERROR (界面卡了，锚点没找到)
                sleep(1000);
            }
        }
        log.info("🎉 印钞机停机！");
    }

    /**
     * 独立封装的接取初始任务逻辑 (自带死磕重试机制，保持不变)
     */
    private boolean setupInitialTask() {
        int retry = 0;
        while (retry < MAX_RETRY) {
            // 1. 寻路
            if (!navigationService.navigateToNPC(targetMapName, npc_coor_x, npc_coor_y)) {
                log.warn("⚠️ 无法到达墨意身边 (重试 {}/{})", retry + 1, MAX_RETRY);
                retry++;
                sleep(2000);
                continue;
            }

            // 2. 点击
            if (!npcClickService.clickNpcSmart(context.getMe(), targetMapName, npc_coor_x, npc_coor_y, targetNPCName, TUNE_X, TUNE_Y)) {
                log.warn("⚠️ 无法点中墨意 (重试 {}/{})", retry + 1, MAX_RETRY);
                retry++;
                sleep(2000);
                continue;
            }

            // 3. 对话
            // ⚠️ 这里调用您 DialogService 里原本保留的 acceptTask 快捷方法即可
            dialogService.acceptTask(DIALOG_START_OFFSET_X, DIALOG_START_OFFSET_Y);
            log.info("✅ 成功接取五环总任务！");
            return true;
        }
        return false;
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}