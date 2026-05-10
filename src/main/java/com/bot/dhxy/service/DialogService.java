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
    private static final int DIALOG_SMALL_X = 250;
    private static final int DIALOG_SMALL_Y = 345;
    private static final int DIALOG_SMALL_W = 529;
    private static final int DIALOG_SMALL_H = 143;
    private static final int CROP_TOP_Y = 42;
    private static final int CROP_DEV_Y = 58;
    private static final int CROP_LEFT_X = 161; // 主要为了测devidance,还是最好避免对话字准确一些。

    private static final int DIALOG_LARGE_X = 250;
    private static final int DIALOG_LARGE_Y = 312;
    private static final int DIALOG_LARGE_W = 529;
    private static final int DIALOG_LARGE_H = 208;


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
                    Point giveTextPt = coordinateHelper.findGreenTextInRegion(OPTION_GIVE_TEXT,
                            getSmallDialogRect(), 0.85);
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

// ==========================================
    // 🛡️ 三道防线：终极 UI 状态机引擎
    // ==========================================

    public DialogType detectDialogType() {
        // 🛡️ 第 0 关：平滑度门卫 (大区探测)
        if (!hasDialogMask()) {
            // 背景太粗糙（比如草地、砖头），绝对没有对话框，直接省下后续的洗图算力
            return DialogType.NONE;
        }

        // 🟢 第 1 关：看脚下测 Option (下半区洗绿字)
        if (hasOptionInLowerHalf()) {
            return DialogType.OPTION;
        }

        // ⚪ 第 2 关：看头顶测 Story (上半区洗白/绿字)
        if (hasStoryInUpperHalf()) {
            return DialogType.STORY;
        }

        // 🌑 终极绝杀：保安放行了(背景平滑)，但上下半区都洗不出字
        // 这 1000% 就是您之前遇到的【平滑黑石头/黑夜】！直接无情拦截！
        log.debug("🚧 [雷达拦截] 发现平滑遮罩，但区域内无字，确认为黑石头伪装，已拦截！");
        return DialogType.NONE;
    }

    /**
     * 防线二：下半区选项测定 (只扫描 Y + 85 以下的区域)
     */
    private boolean hasOptionInLowerHalf() {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y + CROP_TOP_Y, DIALOG_SMALL_W, DIALOG_SMALL_H - CROP_TOP_Y);
        BufferedImage frame = tracker.captureToMemory("opt-scan", area[0], area[1], area[2], area[3]);
        ImagePreprocessor.saveDebugImage(frame, "opt-scan-debug.png"); // 调试用
        if (frame == null) return false;

        int count = ImagePreprocessor.countGreenPixelsHSV(frame);
        frame.flush();

        // 选项的绿字比较密集，150是个极其安全的阈值
        return count > 150;
    }

    /**
     * 防线一：大区平滑度测定 (依然扫描 DIALOG_W * DIALOG_H 全图)
     */
    private boolean hasDialogMask() {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X + CROP_LEFT_X, DIALOG_SMALL_Y + CROP_DEV_Y, DIALOG_SMALL_W - CROP_LEFT_X, DIALOG_SMALL_H - CROP_DEV_Y);
        BufferedImage frame = tracker.captureToMemory("std-scan", area[0], area[1], area[2], area[3]);
        if (frame == null) return false;

        double stddev = ImagePreprocessor.getImageStandardDeviation(frame);
        frame.flush();
        // 保留您的神级参数 30.0 (如果之后发现真对话框被误杀，可微调到 22.0)
        log.info("sstddev: {}", stddev);
        return stddev < 30.0;
    }

    /**
     * 防线三：上半区剧情测定 (只扫描顶部的 85 像素高度)
     */
    private boolean hasStoryInUpperHalf() {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, CROP_TOP_Y);
        BufferedImage frame = tracker.captureToMemory("story-scan", area[0], area[1], area[2], area[3]);
        if (frame == null) return false;
        ImagePreprocessor.saveDebugImage(frame,"story_scan.png");
        // 🌟 换用带有【腐蚀滤网】的测算器，彻底无视白衣服和雪地的干扰！
        int thinWhiteCount = ImagePreprocessor.countThinWhitePixelsHSV(frame);
        int greenCount = ImagePreprocessor.countGreenPixelsHSV(frame);
        frame.flush();

        int totalTextPixels = thinWhiteCount + greenCount;

        // 经过腐蚀过滤后，剩下的绝对是纯净的文字，不再有胖白色的干扰
        boolean hasText = totalTextPixels > 100;

        if (hasText) {
            log.debug("📝 [雷达] 上半区洗出细丝文字: 白={}, 绿={}, 确认剧情！", thinWhiteCount, greenCount);
        }

        return hasText;
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
        return coordinateHelper.getScaledRect(DIALOG_LARGE_X, DIALOG_LARGE_Y, DIALOG_LARGE_W, DIALOG_LARGE_H);
    }

    private int[] getSmallDialogRect() {
        return coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, DIALOG_SMALL_H);
    }

    private void clickAbsolutePoint(int x, int y) {
        inputProvider.clickLeft(x, y, 150);
    }

    private int randomOffset(int r) { return r <= 0 ? 0 : random.nextInt(r * 2 + 1) - r; }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}