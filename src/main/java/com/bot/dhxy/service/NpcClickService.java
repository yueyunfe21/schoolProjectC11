package com.bot.dhxy.service;

import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.ImagePreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;

import static java.lang.Thread.sleep;

@Slf4j
@Component
@RequiredArgsConstructor
public class NpcClickService {

    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final GameStateUtil gameStateUtil;
    private final TextRecognizer ocr;
    private final LocationVisionService locationVisionService;
    private final DialogService dialogService;

    // ========================================================================
    // 🚀 轨道炮核心引擎参数 (由首领实测数据暴力破解)
    // ========================================================================
    private static final double UX = 20.0;
    private static final double UY = 0.0;
    private static final double VX = 0.0;
    private static final double VY = -20.0;

    // ==========================================
    // 🧱 盲狙专用：近战散弹枪模式 (高密度微调阵列)
    // ==========================================
    private static final int[][] DENSE_BLIND_OFFSETS = {
            {0, 0},      // 原点
            {0, -10},    // 微上
            {0, 10},     // 微下
            {-10, 0},    // 微左
            {10, 0},
            {0, -15},    // 微上
            {0, 15},     // 微下
            {-15, 0},    // 微左
            {15, 0},
            {0, -20},    // 微上
            {0, 20},     // 微下
            {-20, 0},    // 微左
            {20, 0},     // 微右
            {-10, -10},  // 稍高
            {-15, -15},
            {-20, -20},  // 左上
            {10, 10},    // 右上
            {15, 15},
            {20, 20},
    };

    private static final String NPC_regex = ".*\\(NPC\\).*";

    private boolean executeClickAndVerify(int x, int y, long firstWaitMs, int maxRetries) {
        inputProvider.clickLeft(x, y, 100);
        sleep(firstWaitMs);

        if (dialogService.hasOptionDialog()) return true;

        for (int i = 1; i <= maxRetries; i++) {
            log.warn("⚠️ 首发未触发对话，进行第 {} 次火力覆盖...", i);
            inputProvider.clickLeft(x, y, 100);
            sleep(1000);
            if (dialogService.hasOptionDialog()) return true;
        }
        return false;
    }

    private boolean scanMenuAndVerify(int testX, int testY) {
        int scanW = 150;
        int scanH = 120;
        int scanX = testX;
        int scanY = testY - scanH;
        String menuScanPath = "images/temp/npc_menu_scan.png";

        // 🌟 1. 改用装甲截图，防止菜单被别人名字干扰
        tracker.captureToFileWithShield("菜单侦查", menuScanPath, scanX, scanY, scanX + scanW, testY);
        List<TextRecognizer.OcrWordResult> menuWords = ocr.getAllTextResults(menuScanPath);

        if (menuWords != null) {
            for (TextRecognizer.OcrWordResult w : menuWords) {
                String text = w.getText();
                if (text != null && text.matches(NPC_regex)) {
                    log.info("🎯 锁定(NPC)下拉菜单: {}", text);
                    int clickX = scanX + w.getX();
                    int clickY = scanY + w.getY();
                    inputProvider.moveMouse(clickX, clickY);
                    sleep(100);
                    return executeClickAndVerify(clickX, clickY, 800, 1);
                }
            }
        }
        return false;
    }

    public boolean clickNpcSmart(PlayerCharacter player, String mapName, int mapX, int mapY, String npcName, int tuneX, int tuneY) {
        if (!tracker.bringWindowToFront()) {
            log.warn("❌ [NPC点击] 游戏窗口无法置顶，放弃点击");
            return false;
        }

        tracker.updateGlobalVision();
        int gameBaseX = tracker.getWindowBaseX();
        int gameBaseY = tracker.getWindowBaseY();
        int screenCenterX = gameBaseX + (1024 / 2);
        int screenCenterY = gameBaseY + (768 / 2);

        log.info("⚡ 启动【终极三段式商业级火控引擎】...");

        TextRecognizer.LocationInfo locInfo = locationVisionService.scanCurrentLocation();

        int scanWidth = 350;
        int scanHeight = 200;
        int scanStartX = screenCenterX - (scanWidth / 2);
        int scanStartY = screenCenterY - (scanHeight / 2);

        String centerScanPath = "images/temp/center_scan_layer1.png";
        String playerScanPath = "images/temp/center_scan_player.png";

        // 🌟 2. 第一发找自己的紫字也要套盾，防止满屏玩家干扰
        tracker.captureToFileWithShield("中心区域侦查", centerScanPath, scanStartX, scanStartY, scanStartX + scanWidth, scanStartY + scanHeight);
        ImagePreprocessor.washPurpleTextToBlackAndWhite(centerScanPath, playerScanPath);

        List<TextRecognizer.OcrWordResult> playerWords = ocr.getAllTextResults(playerScanPath);
        Point playerAnchor = null;

        if (playerWords != null && player != null && player.getName() != null) {
            playerAnchor = locationVisionService.extractPlayerPhysicalAnchor(
                    playerWords, player.getName(), scanStartX, scanStartY, 0);
        }

        // ==========================================
        // 💥 第一段：【数学轨道炮】 纯左键盲狙 (零成本)
        // ==========================================
        if (locInfo != null && playerAnchor != null) {
            log.info("🎯 玩家逻辑: [{}, {}], 目标NPC逻辑: [{}, {}]", locInfo.x, locInfo.y, mapX, mapY);

            int deltaLogicX = mapX - locInfo.x;
            int deltaLogicY = mapY - locInfo.y;

            int deltaPhysX = (int) Math.round(deltaLogicX * UX + deltaLogicY * VX);
            int deltaPhysY = (int) Math.round(deltaLogicX * UY + deltaLogicY * VY);

            int targetX = playerAnchor.x + deltaPhysX + tuneX;
            int targetY = playerAnchor.y + deltaPhysY - 50 + tuneY;

            log.info("🚀 [轨道炮] 解算完毕，向目标坐标发起纯左键打击: {}, {}", targetX, targetY);
            inputProvider.moveMouse(targetX, targetY);
            sleep(150);

            // 第一炮左键！如果成功，直接秒杀返回
            if (executeClickAndVerify(targetX, targetY, 1500, 0)) {
                log.info("✅ [轨道炮] 降维打击成功！数学不会骗人！");
                return true;
            }

            // 如果失败，大概率人物自动寻路跑过去了，等 1.5 秒让他站稳
            log.warn("⚠️ 轨道炮未能拉出菜单，人物可能发生跑动，等待贴脸...");
            sleep(1500);
        }

        // ==========================================
        // 🧱 第二段：【零成本散弹枪】 Ctrl + 像素比对
        // ==========================================
        log.warn("🧱 [盲狙层] 已贴脸！启动带“防烧钱雷达”的高密度盲狙...");

        for (int[] offset : DENSE_BLIND_OFFSETS) {
            // 贴脸状态下，以屏幕绝对中心（人物新脚底）为震中发散！
            int testX = screenCenterX + offset[0];
            int testY = screenCenterY + offset[1]  + 20; // 遵循首领的特调坐标

            int scanW = 150;
            int scanH = 120;
            int scanX = testX;
            int scanY = testY - scanH;

            log.info("   -> 微调探测点: {}, {}", testX, testY);

            sleep(50);

            // 🌟 散弹枪盲截，保留原有裸奔状态，绝不加盾卡顿！
            BufferedImage frameBefore = tracker.captureToMemory("menu_before", scanX, scanY, scanX + scanW, testY);
            inputProvider.moveMouse(testX, testY);
            inputProvider.holdCtrl();
            try {
                sleep(200);

                BufferedImage frameAfter = tracker.captureToMemory("menu_after", scanX, scanY, scanX + scanW, testY);

                if (frameBefore != null && frameAfter != null) {
                    boolean changed = !ImageFinder.isMatch(frameBefore, frameAfter, 0.05);
                    frameBefore.flush();
                    frameAfter.flush();

                    if (!changed) {
                        log.info("      🚫 画面无变化(菜单未弹出)，极速跳过 OCR！省下一次 API 调用！");
                        continue;
                    } else {
                        log.info("      ⚡ 画面突变！菜单疑似弹出，启动 OCR 精准击杀...");
                    }
                }

                // 进里面会调用一次套盾的 scanMenuAndVerify
                if (scanMenuAndVerify(testX, testY)) {
                    log.info("✅ [盲狙层] 物理微调兜底成功！死角破除！");
                    return true;
                }
            } finally {
                inputProvider.releaseCtrl();
                sleep(100);
            }
        }

        // ==========================================
        // 👁️ 第三段：【视觉 OCR 兜底】 全局找黄色名字
        // ==========================================
        log.warn("👁️ [视觉层] 物理散弹枪全部打空，NPC极其畸形或被完全遮挡。祭出视觉核武器...");

        // 🌟 3. 兜底找浮空黄字，怕干扰，套盾！
        tracker.captureToFileWithShield("中心区域侦查(新)", centerScanPath, scanStartX, scanStartY, scanStartX + scanWidth, scanStartY + scanHeight);
        List<TextRecognizer.OcrWordResult> centerWordsNew = ocr.getAllTextResults(centerScanPath);

        if (centerWordsNew != null) {
            for (TextRecognizer.OcrWordResult w : centerWordsNew) {
                if (w.getText() != null && w.getText().contains(npcName)) {
                    log.info("🎯 [视觉层] 强行锁定半空中的目标文字: {}", w.getText());
                    int clickX = scanStartX + w.getX();
                    int clickY = scanStartY + w.getY() - 50;

                    inputProvider.moveMouse(clickX, clickY);
                    sleep(150);

                    if (executeClickAndVerify(clickX, clickY, 2000, 1)) {
                        log.info("✅ [视觉层] 视觉兜底击杀成功！");
                        return true;
                    }
                    break;
                }
            }
        }

        log.error("❌ [NPC点击] 穷尽商业级三段火控，未能打开 [{}] 的对话框！", npcName);
        return false;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore flag
            return; // or break / throw, depending on context
        }
    }
}