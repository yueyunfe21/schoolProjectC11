package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class UICleanerService {

    // 🌟 把所有需要用到的工具全注入进来
    private final InputProvider inputProvider;
    private final UITemplateLocatorService uiTemplateLocator;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer ocr;
    private final GameStateUtil gameStateUtil;
    private final BotProperties config;

    private final Random random = new Random();

    /**
     * 👑 总指挥：全局 UI 大扫除主入口
     * 顺序：关大地图 -> 关对话框 -> 关通用 X 窗口
     */
    public void cleanUpAll() {
        log.info("🧹 [UI清理] 开始执行全局 UI 状态大扫除...");
        boolean needWait = false;

        // 1. 查地图
        if (isWorldMapOpened()) {
            log.info("🧹 [UI清理] 发现残留【世界地图】，执行关闭...");
            closeMapByDoubleRightClick();
            needWait = true;
        }

        // 2. 查对话框
        if (gameStateUtil.isDialogOpened()) {
            log.info("🧹 [UI清理] 发现残留【NPC对话框】，执行强制关闭...");
            forceCloseDialog();
            needWait = true;
        }

        // 稍微等一下地图和对话框的关闭动画
        if (needWait) {
            sleepInterruptible(1000);
        }

        // 3. 查其他所有带 X 的面板（包裹、帮派、活动等）
        if (closeAllGenericWindows()) {
            needWait = true;
        }

        if (needWait) {
            log.info("🧹 [UI清理] 清理完毕，当前界面已干净！");
        } else {
            log.info("🧹 [UI清理] 当前界面非常清爽，无需清理。");
        }
    }

    /**
     * 🗺️ 检查并关闭世界地图
     */
    private boolean isWorldMapOpened() {
        return uiTemplateLocator.findTemplateCenter("images/template/world_map_title.png") != null;
    }

    private void closeMapByDoubleRightClick() {
        int closeX = tracker.getWindowBaseX() + config.getAnchor_windowTo_map_scroll_X();
        int closeY = tracker.getWindowBaseY() + config.getAnchor_windowTo_map_scroll_Y();
        inputProvider.doubleRightClick(closeX, closeY, 150, 500);
    }

    /**
     * ❌ 暴力大扫除：扫描屏幕上的所有通用“X”关闭按钮并点击
     */
    public boolean closeAllGenericWindows() {
        boolean closedAny = false;
        String[] closeButtonTemplates = {
                "images/template/x1.png",
                "images/template/x2.png"
        };

        log.info("🧹 [UI清理] 开始扫描通用关闭按钮...");

        for (int i = 0; i < 3; i++) {
            boolean foundInThisPass = false;

            for (String templatePath : closeButtonTemplates) {
                Point closeBtnPoint = uiTemplateLocator.findTemplateCenter(templatePath);

                if (closeBtnPoint != null) {
                    log.info("🧹 [UI清理] 发现关闭按钮 [{}], 坐标: {}, 执行点击",
                            templatePath, closeBtnPoint.x, closeBtnPoint.y);

                    int clickX = closeBtnPoint.x + (random.nextInt(5) - 2);
                    int clickY = closeBtnPoint.y + (random.nextInt(5) - 2);

                    inputProvider.clickLeft(clickX, clickY, 150);
                    sleepInterruptible(500);

                    foundInThisPass = true;
                    closedAny = true;
                    break;
                }
            }

            if (!foundInThisPass) {
                break;
            }
        }
        return closedAny;
    }

    /**
     * 💬 专属的“强杀对话框”逻辑，不掺杂任何找路代码
     */
    private void forceCloseDialog() {
        int[] dialogRect = coordinateHelper.getScaledRect(250, 312, 529, 208);
        String imgPath = "images/temp/dialog_close_scan.png";
        tracker.captureToFile("扫除对话框", imgPath, dialogRect[0], dialogRect[1], dialogRect[2], dialogRect[3]);

        List<TextRecognizer.OcrWordResult> allWords = ocr.getAllTextResults(imgPath);
        if (allWords != null && !allWords.isEmpty()) {
            List<String> closeKeywords = Arrays.asList(
                    "取消", "离开", "看一看", "哪儿也", "以后再说", "原来你",
                    "看看", "我还有事", "不", "算了", "暂时", "路过", "再会"
            );
            for (String keyword : closeKeywords) {
                for (TextRecognizer.OcrWordResult word : allWords) {
                    if (word.getText().contains(keyword)) {
                        clickAbsolutePoint(dialogRect[0] + word.getX(), dialogRect[1] + word.getY());
                        return;
                    }
                }
            }
        }
        // 兜底中心点击
        log.warn("🛡️ [UI清理] 对话框未找到关闭词，触发中心点击兜底！");
        clickAbsolutePoint(dialogRect[0] + (dialogRect[2] - dialogRect[0]) / 2, dialogRect[1] + (dialogRect[3] - dialogRect[1]) / 2);
    }

    private void clickAbsolutePoint(int x, int y) {
        inputProvider.clickLeft(x + (random.nextInt(5) - 2), y + (random.nextInt(5) - 2), 150);
    }

    private void sleepInterruptible(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}