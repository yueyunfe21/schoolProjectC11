package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

import static com.bot.dhxy.config.TeleportConfig.MAP_ALIASES;

@Slf4j
@Component
@RequiredArgsConstructor
public class DialogService {

    private final InputSequences inputSequences;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer ocr;
    private final GiveItemService giveItemService;
    private final WindowScopedTempPath windowScopedTempPath;

    private final Random random = new Random();

    private static final int DIALOG_SMALL_X = 250;
    private static final int DIALOG_SMALL_Y = 345;
    private static final int DIALOG_SMALL_W = 529;
    private static final int DIALOG_SMALL_H = 143;
    private static final int CROP_TOP_Y = 42;
    private static final int CROP_DEV_Y = 58;
    private static final int CROP_LEFT_X = 161;

    private static final int DIALOG_LARGE_X = 250;
    private static final int DIALOG_LARGE_Y = 312;
    private static final int DIALOG_LARGE_W = 529;
    private static final int DIALOG_LARGE_H = 208;

    private static final String OPTION_GIVE_TEXT = "images/template/dialog/dialog_opt_give.png";

    public enum DialogType { NONE, OPTION, STORY }

    public boolean handleDialog(String targetKeyword, Point initialClick, String itemToGive, Integer knownBagIndex) {
        if (initialClick != null) {
            log.info("dialog initial click: ({},{})", initialClick.x, initialClick.y);
            clickAbsolutePoint(initialClick.x, initialClick.y, "dialog:initialClick");
            sleep(600 + random.nextInt(200));
        }

        boolean hasHandled = false;
        int maxSafetyPages = 20;
        int currentPage = 0;

        while (currentPage < maxSafetyPages) {
            DialogType type = detectDialogType();

            if (type == DialogType.NONE) {
                break;
            }

            hasHandled = true;

            if (type == DialogType.STORY) {
                log.info("dialog story page: {}", currentPage + 1);
                fastClickStoryDialog();
            } else if (type == DialogType.OPTION) {
                log.info("dialog option page: {}", currentPage + 1);

                if (itemToGive != null) {
                    Point giveTextPt = coordinateHelper.findGreenTextInRegion(OPTION_GIVE_TEXT,
                            getSmallDialogRect(), 0.85);
                    if (giveTextPt != null) {
                        Point safeClick = coordinateHelper.getRandomizedPoint(giveTextPt, 20, 5);
                        clickAbsolutePoint(safeClick.x, safeClick.y, "dialog:giveOption");
                        sleep(800);

                        if (giveItemService.executeGive(itemToGive, knownBagIndex)) {
                            break;
                        }
                    }
                }

                if (targetKeyword != null) {
                    processOptionsWithOCR(targetKeyword);
                } else {
                    doFallbackClick(getDialogRect(), "no keyword, default first option");
                }
            }
            currentPage++;
        }

        return hasHandled;
    }

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

    public DialogType detectDialogType() {
        sleep(700 + random.nextInt(100));
        if (!hasDialogMask()) {
            return DialogType.NONE;
        }

        if (hasOptionInLowerHalf()) {
            return DialogType.OPTION;
        }

        if (hasStoryInUpperHalf()) {
            return DialogType.STORY;
        }

        log.debug("dialog mask exists but no text found");
        return DialogType.NONE;
    }

    private boolean hasOptionInLowerHalf() {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y + CROP_TOP_Y, DIALOG_SMALL_W, DIALOG_SMALL_H - CROP_TOP_Y);
        BufferedImage frame = tracker.captureToMemory("opt-scan", area[0], area[1], area[2], area[3]);
        ImagePreprocessor.saveDebugImage(frame, windowScopedTempPath.resolve("opt-scan-debug.png"));
        if (frame == null) return false;

        int count = ImagePreprocessor.countGreenPixelsHSV(frame);
        frame.flush();
        return count > 150;
    }

    private boolean hasDialogMask() {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X + CROP_LEFT_X, DIALOG_SMALL_Y + CROP_DEV_Y, DIALOG_SMALL_W - CROP_LEFT_X, DIALOG_SMALL_H - CROP_DEV_Y);
        BufferedImage frame = tracker.captureToMemory("std-scan", area[0], area[1], area[2], area[3]);
        if (frame == null) return false;

        double stddev = ImagePreprocessor.getImageStandardDeviation(frame);
        frame.flush();
        log.info("sstddev: {}", stddev);
        return stddev < 30.0;
    }

    private boolean hasStoryInUpperHalf() {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, CROP_TOP_Y);
        BufferedImage frame = tracker.captureToMemory("story-scan", area[0], area[1], area[2], area[3]);
        if (frame == null) return false;
        ImagePreprocessor.saveDebugImage(frame, windowScopedTempPath.resolve("story_scan.png"));
        int thinWhiteCount = ImagePreprocessor.countThinWhitePixelsHSV(frame);
        int greenCount = ImagePreprocessor.countGreenPixelsHSV(frame);
        frame.flush();

        int totalTextPixels = thinWhiteCount + greenCount;
        return totalTextPixels > 200;
    }

    public void fastClickStoryDialog() {
        sleep(600 + random.nextInt(100));
        int[] rect = getDialogRect();
        double scale = coordinateHelper.getScaleRatio();
        int cx = rect[0] + (rect[2] - rect[0]) / 2;
        int cy = rect[3] - (int)Math.round(40 / scale);
        clickAbsolutePoint(cx + randomOffset(30), cy + randomOffset(10), "dialog:storyClick");
        sleep(600 + random.nextInt(100));
    }

    private boolean processOptionsWithOCR(String targetKeyword) {
        int[] rect = getDialogRect();
        String path = windowScopedTempPath.resolve("dialog_active_scan.png");
        if (!tracker.captureToFile("OCR-Scan", path, rect[0], rect[1], rect[2], rect[3])) return false;

        List<TextRecognizer.OcrWordResult> results = ocr.getAllTextResults(path);
        List<String> aliases = MAP_ALIASES.getOrDefault(targetKeyword, java.util.Collections.singletonList(targetKeyword));

        for (String alias : aliases) {
            for (TextRecognizer.OcrWordResult word : results) {
                if (word.getText().contains(alias)) {
                    log.info("OCR hit [{}]", alias);
                    clickAbsolutePoint(rect[0] + word.getX(), rect[1] + word.getY(), "dialog:ocrOption");
                    return true;
                }
            }
        }
        return doFallbackClick(rect, "OCR target not matched");
    }

    private boolean doFallbackClick(int[] rect, String reason) {
        log.warn("dialog fallback click: {}", reason);
        double scale = coordinateHelper.getScaleRatio();
        int cx = rect[0] + (rect[2] - rect[0]) / 2;
        int cy = rect[3] - (int)Math.round(100 / scale);
        clickAbsolutePoint(cx, cy, "dialog:fallback");
        return true;
    }

    public int[] getDialogRect() {
        return coordinateHelper.getScaledRect(DIALOG_LARGE_X, DIALOG_LARGE_Y, DIALOG_LARGE_W, DIALOG_LARGE_H);
    }

    public int[] getSmallDialogRect() {
        return coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, DIALOG_SMALL_H);
    }

    private void clickAbsolutePoint(int x, int y, String description) {
        inputSequences.clickLeft(description, x, y, 150);
    }

    private int randomOffset(int r) { return r <= 0 ? 0 : random.nextInt(r * 2 + 1) - r; }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}
