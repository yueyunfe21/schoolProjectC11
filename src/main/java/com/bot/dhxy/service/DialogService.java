package com.bot.dhxy.service;

import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

import static com.bot.dhxy.config.TeleportConfig.MAP_ALIASES;

@Slf4j
@Component
@RequiredArgsConstructor
public class DialogService {

    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer ocr;
    private final GiveItemService giveItemService; // 🌟 注入您的全新大将！

    private final Random random = new Random();

    // 测绘数据
    private static final int DIALOG_X = 250;
    private static final int DIALOG_Y = 312;
    private static final int DIALOG_W = 529;
    private static final int DIALOG_H = 208;
    private static final int CROP_TOP_Y = 85;

    // 🌟 特殊选项的绿色特征文字
    private static final String OPTION_GIVE_TEXT = "images/template/dialog/dialog_opt_give.png";


    public enum DialogType { NONE, OPTION, STORY }

    // ==========================================
    // 🚀 唯一的 Master 入口：万能处理器
    // ==========================================

    /**
     * 🧠 智能对话框总线
     * @param targetKeyword 如果不为空，则在选项中寻找并匹配文字。
     * @param initialClick 如果不为空，则在启动处理流程前，先点击该绝对坐标。
     * @param itemToGive 🌟 新增参数：如果当前任务包含给东西，传入物品的图片名称（如 shoe.png）
     */
    public boolean handleDialog(String targetKeyword, Point initialClick, String itemToGive, Integer knownBagIndex) {
        if (initialClick != null) {
            log.info("🎯 [对话框总线] 执行初始点击（盲狙接任务）: ({},{})", initialClick.x, initialClick.y);
            clickAbsolutePoint(initialClick.x, initialClick.y);
            sleep(600 + random.nextInt(200));
        }

        boolean hasHandled = false;
        int maxSafetyPages = 20;
        int currentPage = 0;

        while (currentPage < maxSafetyPages) {
            DialogType type = detectDialogType();

            if (type == DialogType.NONE) {
                break; // 战场打扫干净，撤退
            }

            hasHandled = true;

            if (type == DialogType.STORY) {
                log.info("⏩ [对话框总线] 第{}页：发现纯剧情，物理超度跳过...", currentPage + 1);
                fastClickStoryDialog();
            }
            else if (type == DialogType.OPTION) {
                log.info("🎯 [对话框总线] 第{}页：发现选项，执行策略决策...", currentPage + 1);

// 🌟 寻物给予侦测：把页码情报透传下去！
                if (itemToGive != null) {
                    Point giveTextPt = coordinateHelper.findImageAbsoluteCoordinate(OPTION_GIVE_TEXT, 0.85);
                    if (giveTextPt != null) {
                        log.info("🎯 [对话框总线] 发现【送你啦】特殊选项！携带包裹情报[{}]呼叫给予引擎...", knownBagIndex);

                        Point safeClick = coordinateHelper.getRandomizedPoint(giveTextPt, 20, 5);
                        clickAbsolutePoint(safeClick.x, safeClick.y);
                        sleep(800);

                        // 🌟 把 knownBagIndex 传给 GiveItemService
                        if (giveItemService.executeGive(itemToGive, knownBagIndex)) {
                            break;
                        }
                    }
                }

                // 常规选项侦测
                if (targetKeyword != null) {
                    processOptionsWithOCR(targetKeyword);
                } else {
                    doFallbackClick(getDialogRect(), "无关键字需求，默认选第一个");
                }
            }
            currentPage++;
        }

        return hasHandled;
    }

    // ==========================================
    // 🛠️ 快捷方式重载 (为了兼容其他老代码)
    // ==========================================
    public boolean processDialog(String targetMapName) {
        return handleDialog(targetMapName, null, null, null);
    }

    public void acceptTask(int offsetX, int offsetY) {
        double scale = coordinateHelper.getScaleRatio();
        int targetX = tracker.getWindowBaseX() + (int)Math.round(offsetX / scale);
        int targetY = tracker.getWindowBaseY() + (int)Math.round(offsetY / scale);
        int finalX = targetX + randomOffset((int)(15/scale));
        int finalY = targetY + randomOffset((int)(3/scale));

        handleDialog(null, new Point(finalX, finalY), null, null);
    }

    // ... 下方的 detectDialogType, hasOptionDialog, hasStoryDialog 等底层方法保持您原有代码完全不变 ...

    public DialogType detectDialogType() {
        if (hasOptionDialog()) return DialogType.OPTION;
        if (hasStoryDialog()) return DialogType.STORY;
        return DialogType.NONE;
    }

    private boolean hasOptionDialog() {
        int[] area = coordinateHelper.getScaledRect(DIALOG_X, DIALOG_Y + CROP_TOP_Y, DIALOG_W, DIALOG_H - CROP_TOP_Y);
        BufferedImage frame = tracker.captureToMemory("green-scan", area[0], area[1], area[2], area[3]);
        if (frame == null) return false;
        int count = ImagePreprocessor.countGreenPixelsHSV(frame);
        frame.flush();
        return count > 150;
    }

    private boolean hasStoryDialog() {
        int[] area = coordinateHelper.getScaledRect(255, 418, 525, 70);
        BufferedImage frame = tracker.captureToMemory("std-scan", area[0], area[1], area[2], area[3]);
        if (frame == null) return false;
        double stddev = ImagePreprocessor.getImageStandardDeviation(frame);
        frame.flush();
        return stddev < 20.0;
    }

    private void fastClickStoryDialog() {
        int[] rect = getDialogRect();
        double scale = coordinateHelper.getScaleRatio();
        int cx = rect[0] + (rect[2] - rect[0]) / 2;
        int cy = rect[3] - (int)Math.round(40 / scale);
        clickAbsolutePoint(cx + randomOffset(30), cy + randomOffset(10));
        sleep(400 + random.nextInt(100));
    }

    private boolean processOptionsWithOCR(String targetKeyword) {
        int[] rect = getDialogRect();
        String path = "images/temp/dialog_active_scan.png";
        if (!tracker.captureToFile("OCR-Scan", path, rect[0], rect[1], rect[2], rect[3])) return false;

        java.util.List<TextRecognizer.OcrWordResult> results = ocr.getAllTextResults(path);
        java.util.List<String> aliases = MAP_ALIASES.getOrDefault(targetKeyword, java.util.Collections.singletonList(targetKeyword));

        for (String alias : aliases) {
            for (TextRecognizer.OcrWordResult word : results) {
                if (word.getText().contains(alias)) {
                    log.info("✅ OCR 命中 [{}]", alias);
                    clickAbsolutePoint(rect[0] + word.getX(), rect[1] + word.getY());
                    return true;
                }
            }
        }
        return doFallbackClick(rect, "OCR未匹配到目标地名");
    }

    //reminder for needing refactoy
    private boolean doFallbackClick(int[] rect, String reason) {
        log.warn("🛡️ [兜底] {} -> 盲选第一个选项", reason);
        double scale = coordinateHelper.getScaleRatio();
        int cx = rect[0] + (rect[2] - rect[0]) / 2;
        int cy = rect[3] - (int)Math.round(100 / scale);
        clickAbsolutePoint(cx, cy);
        return true;
    }

    private int[] getDialogRect() {
        return coordinateHelper.getScaledRect(DIALOG_X, DIALOG_Y, DIALOG_W, DIALOG_H);
    }

    private void clickAbsolutePoint(int x, int y) {
        inputProvider.clickLeft(x, y, 150);
    }

    private int randomOffset(int r) { return r <= 0 ? 0 : random.nextInt(r * 2 + 1) - r; }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}