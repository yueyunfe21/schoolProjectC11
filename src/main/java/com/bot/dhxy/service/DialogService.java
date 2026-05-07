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

@Slf4j
@Component
@RequiredArgsConstructor
public class DialogService {

    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer ocr;
    private final Random random = new Random();

    // 测绘数据
    private static final int DIALOG_X = 250;
    private static final int DIALOG_Y = 312;
    private static final int DIALOG_W = 529;
    private static final int DIALOG_H = 208;
    private static final int CROP_TOP_Y = 85; // 屏蔽上半部剧情绿字的偏移量

    private static final java.util.Map<String, java.util.List<String>> MAP_ALIASES = new java.util.HashMap<>();
    static {
        MAP_ALIASES.put("长安", java.util.Arrays.asList("长安", "长安城", "皇宫门口", "化生寺", "去长安", "回长安"));
    }

    public enum DialogType { NONE, OPTION, STORY }

    // ==========================================
    // 🚀 唯一的 Master 入口：万能处理器
    // ==========================================

    /**
     * 🧠 智能对话框总线
     * @param targetKeyword 如果不为空，则在选项中寻找并匹配文字。如果为空，则默认盲选第一个选项。
     * @param initialClick 如果不为空，则在启动处理流程前，先点击该绝对坐标（用于接任务的第一下）。
     */
    public boolean handleDialog(String targetKeyword, Point initialClick) {
        // 1. 如果有初始点击任务（接任务），先开火
        if (initialClick != null) {
            log.info("🎯 [对话框总线] 执行初始点击（盲狙接任务）: ({},{})", initialClick.x, initialClick.y);
            clickAbsolutePoint(initialClick.x, initialClick.y);
            sleep(600 + random.nextInt(200)); // 等待下一页弹出来
        }

        boolean hasHandled = false;
        int maxSafetyPages = 20;
        int currentPage = 0;

        // 🌟 2. 进入状态自旋锁：只要对话框还在，我就不走！
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
                if (targetKeyword != null) {
                    // 策略A：精准 OCR 狙击
                    processOptionsWithOCR(targetKeyword);
                } else {
                    // 策略B：硬核通关，盲选第一个
                    doFallbackClick(getDialogRect(), "无关键字需求，默认选第一个");
                }
                // 通常选项点完，这个任务流就结束了，根据大话逻辑可以考虑直接 break
                // 但为了防止点完选项还有剧情，我们不 break，继续循环
            }
            currentPage++;
        }

        return hasHandled;
    }

    // ==========================================
    // 🛠️ 为了方便调用，我们保留两个极简的“快捷方式”
    // ==========================================

    /**
     * 快捷方式 1：寻路/找 NPC 时使用（匹配地名）
     */
    public boolean processDialog(String targetMapName) {
        return handleDialog(targetMapName, null);
    }

    /**
     * 快捷方式 2：接固定坐标任务（点击指定位置 + 自动清理后续剧情）
     */
    public void acceptTask(int offsetX, int offsetY) {
        double scale = coordinateHelper.getScaleRatio();
        int targetX = tracker.getWindowBaseX() + (int)Math.round(offsetX / scale);
        int targetY = tracker.getWindowBaseY() + (int)Math.round(offsetY / scale);

        // 加上随机偏移
        int finalX = targetX + randomOffset((int)(15/scale));
        int finalY = targetY + randomOffset((int)(3/scale));

        handleDialog(null, new Point(finalX, finalY));
    }

    // ==========================================
    // 📡 侦测与底层逻辑 (保持之前的优化版本)
    // ==========================================

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