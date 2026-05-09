package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ⚔️ 战斗检测雷达 (极速版)
 * 采用 ROI 局部定点扫描与自适应心跳，零 CPU 损耗！
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BattleRadarService {

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final GameContext context;

    private static final String BATTLE_FLAG_PATH = "images/template/flag_battle.png";

    // ==========================================
    // 📐 ROI 测绘数据：请首领量一下“自动”按钮可能出现的矩形区域！
    // 只需要框住那个按钮的范围即可，越小越快！(相对游戏窗口左上角的逻辑坐标)
    // ==========================================
    private static final int AUTO_BTN_AREA_X = 974;  // 示例值，请修改
    private static final int AUTO_BTN_AREA_Y = 630;  // 示例值，请修改
    private static final int AUTO_BTN_AREA_W = 51;  // 示例值，越小越好
    private static final int AUTO_BTN_AREA_H = 20;   // 示例值，越小越好

    /**
     * 👁️ 雷达扫描：当前是否处于战斗画面？
     * @return true = 在打架, false = 没打架
     */
    public boolean checkAndSyncCombatState() {
        // 🌟 1. 局部极速截图 (仅截取右下角很小的一块)
        int[] rect = coordinateHelper.getScaledRect(AUTO_BTN_AREA_X, AUTO_BTN_AREA_Y, AUTO_BTN_AREA_W, AUTO_BTN_AREA_H);
        String localScanPath = "images/temp/flag_battle.png";

        // 用裸奔截图，因为这块区域通常没有需要套盾的紫字
        tracker.captureToFile("战斗局部雷达", localScanPath, rect[0], rect[1], rect[2], rect[3]);

        // 🌟 2. 局部找图 (极速，通常 < 1ms)
        double[] result = ImageFinder.find(localScanPath, BATTLE_FLAG_PATH, 0.85);
        boolean isCurrentlyInCombat = (result != null && result.length >= 2);

        // 3. 获取大脑里记忆的旧状态
        GameContext.ActionState rememberedState = context.getCurrentActionState();

        // 🧠 4. 神经反射逻辑：状态机突变处理
        if (isCurrentlyInCombat && rememberedState != GameContext.ActionState.IN_COMBAT) {
            log.warn("⚔️ [战斗雷达] 警报！检测到进入战斗画面，大脑状态强制切入 IN_COMBAT！");
            context.setCurrentActionState(GameContext.ActionState.IN_COMBAT);

        } else if (!isCurrentlyInCombat && rememberedState == GameContext.ActionState.IN_COMBAT) {
            log.info("🕊️ [战斗雷达] 战斗结束！脱离战斗状态，进入战后核验阶段...");
            context.setCurrentActionState(GameContext.ActionState.TASK_VERIFYING);
        }

        return isCurrentlyInCombat;
    }

    /**
     * ⏱️ 智能心跳节拍器：根据当前状态，告诉主循环该睡多久再扫下一次
     */
    public int getDynamicPollingIntervalMs() {
        GameContext.ActionState state = context.getCurrentActionState();
        switch (state) {
            case IN_COMBAT:
                return 3000; // 正在打架，不着急，3秒扫一次看看打完没
            case NAVIGATING:
            case INTERACTING:
                return 2000; // 极速狂飙中，提防暗雷，2秒扫一次
            case FREE:
            default:
                return 10000; // 闲置发呆，基本可以不扫（5秒一次兜底）
        }
    }
}