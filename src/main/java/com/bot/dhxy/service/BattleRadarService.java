package com.bot.dhxy.service;

import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * ⚔️ 战斗检测雷达 & 全自动战斗大管家 (融合版)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BattleRadarService {

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final GameContext context;
    // 🌟 新增注入：物理外设驱动
    private final InputProvider inputProvider;

    private static final String BATTLE_FLAG_PATH = "images/template/battle/flag_battle.png";
    private static final String ZHAOHUAN_PATH = "images/template/battle/zhaohuan.png";
    private static final String CHEHUI_PATH = "images/template/battle/chehui.png";
    private static final String NU_PATH = "images/template/battle/nu.png";
    private static final String YUAN_PATH = "images/template/battle/yuan.png";
    private static final String QUXIAO_ZIDONG_PATH = "images/template/battle/quxiao_zidong_green.png";
    private static final String ZIDONGHAI_PATH = "images/template/battle/zidonghai_white.png";

    // ROI 测绘数据
    private static final int AUTO_BTN_AREA_X = 974;
    private static final int AUTO_BTN_AREA_Y = 630;
    private static final int AUTO_BTN_AREA_W = 51;
    private static final int AUTO_BTN_AREA_H = 20;

    private static final int SELECTION_BTN_AREA_X = 927;
    private static final int SELECTION_BTN_AREA_Y = 302;
    private static final int SELECTION_BTN_AREA_W = 100;
    private static final int SELECTION_BTN_AREA_H = 225;

    private static final int TOP_BTN_AREA_X = 456;
    private static final int TOP_BTN_AREA_Y = 62;
    private static final int TOP_BTN_AREA_W = 123;
    private static final int TOP_BTN_AREA_H = 39;

    // ==========================================
    // 🧠 战斗管家大脑中枢 (状态变量)
    // ==========================================
    private boolean isAutoPanelSet = false; // 面板是否已经开启并归位
    private int autoCombatRounds = -1;      // 脑补的剩余回合数
    private int battleCount = 0;            // 战斗场次计数器

    // 📍 绝对安全区：您希望面板被拖拽到的固定坐标 (请按需修改)
    private static final int TARGET_PANEL_X_OFFSET = 448;
    private static final int TARGET_PANEL_Y_OFFSET = 735;


    private java.awt.Point findAutoCombatBox() {
        tracker.updateGlobalVision();
        String rawPath = GameClientTracker.LATEST_VISION_PATH;

        BufferedImage rawImage = ImagePreprocessor.pathToBufferedImage(rawPath);
        if (rawImage == null) {
            log.error("❌ 无法读取游戏截图，雷达瘫痪！");
            return null;
        }

        // 🟢 第一道防线：主炮【洗绿字】
        ImagePreprocessor.countGreenPixelsHSV(rawImage);
        String washedGreenPath = "images/temp/debug_hsv_mask_green.png";
        Point greenPoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(QUXIAO_ZIDONG_PATH, washedGreenPath, 0.80);
        if (greenPoint == null) {
            log.warn("⚠️ 绿字指纹疑似被污染丢失，启动第二道防线：洗白字探测！");
        } else {
            log.info("🎯 [主炮命中] 绿字指纹识别成功！");
            return new java.awt.Point(greenPoint.x + 20, greenPoint.y - 28);
        }

        // ⚪ 第二道防线：副炮兜底【洗白字】

        ImagePreprocessor.countThinWhitePixelsHSV(rawImage);
        String washedWhitePath = "images/temp/debug_thin_white_text.png";
        Point whitePoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(ZIDONGHAI_PATH, washedWhitePath, 0.80);

        if (whitePoint == null) {
            log.warn("⚠️ 白字同样没有识别，得出结论：未发现面板");
            return null;
        } else {
            log.info("🎯 [副炮命中] 白字指纹兜底成功！极度舒适！");
            return new java.awt.Point(whitePoint.x + 43, (int)whitePoint.y + 28);
        }

    }


    public boolean checkAndSyncCombatState() {
        int[] autoRect = coordinateHelper.getScaledRect(AUTO_BTN_AREA_X, AUTO_BTN_AREA_Y, AUTO_BTN_AREA_W, AUTO_BTN_AREA_H);
        if (coordinateHelper.findImageInRegion(BATTLE_FLAG_PATH, autoRect, 0.85) != null) {
            updateCombatState(true);
            return true;
        }

        int[] selectRect = coordinateHelper.getScaledRect(SELECTION_BTN_AREA_X, SELECTION_BTN_AREA_Y, SELECTION_BTN_AREA_W, SELECTION_BTN_AREA_H);
        String selectScanPath = "images/temp/select_scan.png";
        tracker.captureToFile("战斗选项扫描", selectScanPath, selectRect[0], selectRect[1], selectRect[2], selectRect[3]);
        boolean hasSelection = ImageFinder.find(selectScanPath, ZHAOHUAN_PATH, 0.8) != null ||
                ImageFinder.find(selectScanPath, CHEHUI_PATH, 0.8) != null;
        if (hasSelection) {
            updateCombatState(true);
            return true;
        }

        int[] topRect = coordinateHelper.getScaledRect(TOP_BTN_AREA_X, TOP_BTN_AREA_Y, TOP_BTN_AREA_W, TOP_BTN_AREA_H);
        String topScanPath = "images/temp/top_scan.png";
        tracker.captureToFile("战斗顶部扫描", topScanPath, topRect[0], topRect[1], topRect[2], topRect[3]);
        boolean hasTopIcons = ImageFinder.find(topScanPath, NU_PATH, 0.8) != null &&
                ImageFinder.find(topScanPath, YUAN_PATH, 0.8) != null;
        if (hasTopIcons) {
            updateCombatState(true);
            return true;
        }

        updateCombatState(false);
        return false;
    }

    /**
     * 🌟 核心引擎：状态机与战斗动作的完美融合处！
     */
    private void updateCombatState(boolean isCurrentlyInCombat) {
        GameContext.ActionState rememberedState = context.getCurrentActionState();

        // ⚔️ 刚刚进入战斗的瞬间
        if (isCurrentlyInCombat && rememberedState != GameContext.ActionState.IN_COMBAT) {
            log.warn("⚔️ [战斗雷达] 警报！检测到进入战斗画面，大脑状态强制切入 IN_COMBAT！");
            context.setCurrentActionState(GameContext.ActionState.IN_COMBAT);

            // 触发入场逻辑
            onEnterCombat();

        }
        // 🕊️ 刚刚脱离战斗的瞬间
        else if (!isCurrentlyInCombat && rememberedState == GameContext.ActionState.IN_COMBAT) {
            log.info("🕊️ [战斗雷达] 战斗结束！脱离战斗状态，进入战后核验阶段...");
            context.setCurrentActionState(GameContext.ActionState.TASK_VERIFYING);

            // 触发出场逻辑
            onExitCombat();
        }
    }

    // ==========================================
    // 🛠️ 战斗管家战术动作
    // ==========================================

    private void onEnterCombat() {
        battleCount++;
        log.info("⚔️ [自动挂机] 当前是第 {} 场战斗", battleCount);

        // 第 1 场，或者每逢 5 的倍数，执行物理纠察
        if (!isAutoPanelSet || battleCount % 5 == 1) {
            executeStrictCheckAndAlign();
        } else {
            // 平时执行脑补信任模式
            executeTrustMode();
        }
    }

    private void onExitCombat() {
        if (autoCombatRounds > 0) {
            autoCombatRounds -= 3; // 盲扣 3 回合
            log.info("🕊️ [自动挂机] 战斗结束，自动扣除 3 回合，大脑估算剩余: {} 回合", autoCombatRounds);
        }
    }

    private void executeStrictCheckAndAlign() {
        log.info("🔍 [纠察模式] 核实自动战斗面板状态...");
        java.awt.Point p = this.findAutoCombatBox();

        // 没找到？按 Alt+8 强开！
        if (p == null) {
            log.warn("⚠️ 未发现面板！正在盲按 Alt+8 强制开启...");
            inputProvider.pressAlt8();
            sleep(1000);
            p = this.findAutoCombatBox();
        }

        if (p != null) {
            int dropX = tracker.getWindowBaseX() + TARGET_PANEL_X_OFFSET;
            int dropY = tracker.getWindowBaseY() + TARGET_PANEL_Y_OFFSET;
            // 检查它的位置有没有偏离我们的 TARGET (容差 20 像素)
            if (p.distance(dropX, dropY) > 20.0) {
                log.info("📦 发现面板不在安全区！执行强制拖拽归位...");

                // 抓取面板的标题栏（往上提 80 像素，避开按钮）

                inputProvider.dragAndDrop(p.x, p.y, dropX, dropY);
                sleep(500);
            } else {
                log.info("✅ 面板乖乖待在安全区，无需挪动。");
            }

            isAutoPanelSet = true;
            autoCombatRounds = 25;
            log.info("🔋 面板就绪，回合数初始化为 25！");
        } else {
            log.error("❌ 致命异常：按了 Alt+8 还是找不到面板！");
        }
    }

    private void executeTrustMode() {
        log.info("⚡ [信任模式] 不扫图，当前估算剩余回合：{}", autoCombatRounds);
        if (autoCombatRounds < 10) {
            log.warn("🪫 警告：粮草不足 (剩余<10)，执行 Alt+8 盲充能！");
            inputProvider.pressAlt8();
            autoCombatRounds = 25;
            log.info("🔋 充能完毕，满血复活 25 回合！");
        } else {
            log.info("🍵 粮草充足，静静挂机。");
        }
    }

    public int getDynamicPollingIntervalMs() {
        GameContext.ActionState state = context.getCurrentActionState();
        switch (state) {
            case IN_COMBAT:
                return 3000;
            case NAVIGATING:
            case INTERACTING:
                return 2000;
            case FREE:
            default:
                return 10000;
        }
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}