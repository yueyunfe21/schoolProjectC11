package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogHandleResult;
import com.bot.dhxy.service.dialog.DialogStoryPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static com.bot.dhxy.config.TeleportConfig.MAP_ALIASES;

/**
 * Detects and handles game dialogs for the currently bound window.
 *
 * <p>The service separates story dialogs, option dialogs, give-item flows, and lightweight
 * maintenance business prompts. Public methods either submit their own input sequence or explicitly
 * state that they are intended for an existing exclusive input section. Capture paths are
 * window-scoped through {@link WindowScopedTempPath} so concurrent windows do not overwrite each
 * other's diagnostic images.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DialogService {

    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
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
    private static final String WUHAN_ACCEPT_FIRST_OPTION_TEXT = "images/template/dialog/wuhuan_accept_first_option.png";
    private static final String HEAL_PET_OPTION_TEXT = "images/template/dialog/heal_pet_option.png";
    private static final String REPAIR_EQUIPMENT_OPTION_TEXT = "images/template/dialog/repair_equipment_option.png";
    private static final String REPAIR_EQUIPMENT_GIVEUP_OPTION_TEXT = "images/template/dialog/repair_equipment_option_giveup.png";
    private static final double BUSINESS_OPTION_MATCH_RATE = 0.70;

    /**
     * High-level dialog shape detected from the current window screenshot.
     */
    public enum DialogType { NONE, OPTION, STORY }

    /**
     * Template-click descriptor for green option text inside the standard dialog rectangle.
     *
     * @param name diagnostic label written to logs.
     * @param templatePath template image path under the project image folder.
     * @param minOffsetX minimum randomized X offset from the template anchor, in screen pixels.
     * @param maxOffsetX maximum randomized X offset from the template anchor, in screen pixels.
     * @param randomRadiusY randomized Y radius from the template anchor, in screen pixels.
     */
    public record GreenTemplateClickSpec(String name,
                                         String templatePath,
                                         int minOffsetX,
                                         int maxOffsetX,
                                         int randomRadiusY) {
    }

    /**
     * Handle one dialog according to an explicit operation policy.
     *
     * @param request policy object that declares whether to click story dialogs, match keywords,
     * give an item, handle business maintenance options, or use a fallback green option. The optional
     * initial click is screen-absolute and is sent before detection.
     * @return result describing whether a dialog was absent, clicked, ignored, interrupted, or failed.
     * Side effects may include OCR, screenshot writes, item usage, and serialized mouse clicks.
     */
    public DialogHandleResult handleDialog(DialogHandleRequest request) {
        log.info("dialog handle request: source={} operation={} storyPolicy={} optionPolicy={} fallbackPolicy={} itemToGive={} targetKeyword={}",
                request.getSourceTask(), request.getOperation(), request.getStoryPolicy(), request.getOptionPolicy(),
                request.getFallbackPolicy(),
                request.getItemToGive(), request.getTargetKeyword());

        // Stage 1: optional opening click, used by tasks that first need to poke an NPC or object.
        if (request.getInitialClick() != null) {
            Point p = request.getInitialClick();
            log.info("dialog request initial click: ({},{})", p.x, p.y);
            inputSequences.clickLeft("dialog:requestInitialClick", p.x, p.y, 150);
            if (!sleep(600 + random.nextInt(200))) {
                return DialogHandleResult.INTERRUPTED;
            }
        }

        // Stage 2: classify the visible dialog without focusing the window again.
        DialogType type = detectDialogTypeNoFocus("handle-dialog:" + request.getOperation());
        if (type == DialogType.NONE) {
            return finishRequest(request, type, DialogHandleResult.NO_DIALOG);
        }

        // Stage 3: story dialogs are either clicked through or deliberately left alone.
        if (type == DialogType.STORY) {
            if (request.getStoryPolicy() == DialogStoryPolicy.CLICK_THROUGH) {
                fastClickStoryDialog();
                return finishRequest(request, type, DialogHandleResult.STORY_CLICKED);
            }
            return finishRequest(request, type, DialogHandleResult.STORY_IGNORED);
        }

        // Stage 4: option dialogs are handled by the request's explicit option policy.
        DialogHandleResult result = switch (request.getOptionPolicy()) {
            case IGNORE -> DialogHandleResult.OPTION_IGNORED;
            case CLICK_KEYWORD -> handleKeywordOption(request);
            case CLICK_BUSINESS_OPTION -> handleBusinessOption(request.isIncludeCleanupBusinessOptions());
            case GIVE_ITEM_IF_AVAILABLE -> tryGiveItemFromCurrentOptionDialog(request.getItemToGive(), request.getKnownBagIndex());
            case FALLBACK_FIRST_OPTION -> clickFirstGreenOption(getDialogRect(), "request fallback first green option")
                    ? DialogHandleResult.FALLBACK_CLICKED
                    : DialogHandleResult.FAILED;
            case FALLBACK_LAST_OPTION -> clickLastGreenOption(getDialogRect(), "request fallback last green option")
                    ? DialogHandleResult.FALLBACK_CLICKED
                    : DialogHandleResult.FAILED;
        };
        return finishRequest(request, type, result);
    }

    /**
     * Match known maintenance business options in the current option dialog.
     *
     * <p>This path is used by idle/member windows to handle team broadcasts such as heal-pet and
     * repair-equipment. The optional repair-giveup cleanup template is only included when the caller
     * explicitly allows cleanup options.</p>
     */
    private DialogHandleResult handleBusinessOption(boolean includeCleanupOptions) {
        int[] rect = getDialogRect();
        String rawPath = windowScopedTempPath.resolve("business_dialog_raw.png");
        String washedPath = windowScopedTempPath.resolve("business_dialog_washed.png");
        if (!captureToFileExclusive("dialog:businessCapture", "business-dialog-scan", rawPath, rect)) {
            log.warn("business dialog capture failed");
            return DialogHandleResult.FAILED;
        }

        ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath);
        if (tryClickBusinessOptionInWashedImage(washedPath, rect, HEAL_PET_OPTION_TEXT, "heal-pet")) {
            return DialogHandleResult.BUSINESS_OPTION_CLICKED;
        }
        if (tryClickBusinessOptionInWashedImage(washedPath, rect, REPAIR_EQUIPMENT_OPTION_TEXT, "repair-equipment")) {
            return DialogHandleResult.BUSINESS_OPTION_CLICKED;
        }
        if (includeCleanupOptions
                && tryClickBusinessOptionInWashedImage(washedPath, rect, REPAIR_EQUIPMENT_GIVEUP_OPTION_TEXT, "repair-equipment-giveup")) {
            return DialogHandleResult.BUSINESS_OPTION_CLICKED;
        }

        log.info("business dialog option not matched");
        return DialogHandleResult.BUSINESS_OPTION_NOT_FOUND;
    }

    private boolean tryClickBusinessOptionInWashedImage(String washedPath, int[] rect, String templatePath, String optionName) {
        double[] result = ImageFinder.find(washedPath, templatePath, BUSINESS_OPTION_MATCH_RATE);
        if (result == null || result.length < 2) {
            log.info("business dialog option not matched: option={} template={}", optionName, templatePath);
            return false;
        }

        int absoluteX = rect[0] + (int) Math.round(result[0]);
        int absoluteY = rect[1] + (int) Math.round(result[1]);
        Point safeClick = coordinateHelper.getRandomizedPoint(new Point(absoluteX, absoluteY), 4, 3);
        log.info("business dialog option matched: option={} score={} click=({}, {})",
                optionName, result.length > 2 ? result[2] : 0.0, safeClick.x, safeClick.y);
        inputSequences.clickLeft("dialog:businessOption:" + optionName, safeClick.x, safeClick.y, 150);
        return sleep(800 + random.nextInt(300));
    }

    private DialogHandleResult handleKeywordOption(DialogHandleRequest request) {
        if (request.getTargetKeyword() == null) {
            log.warn("dialog keyword option requested without targetKeyword");
            return DialogHandleResult.OPTION_KEYWORD_NOT_FOUND;
        }
        boolean clicked = processOptionsWithOCR(request.getTargetKeyword(), request.isAllowFallbackOptionClick());
        if (clicked) {
            return DialogHandleResult.OPTION_KEYWORD_CLICKED;
        }
        return DialogHandleResult.OPTION_KEYWORD_NOT_FOUND;
    }

    private DialogHandleResult tryGiveItemFromCurrentOptionDialog(String itemToGive, Integer knownBagIndex) {
        if (itemToGive == null) {
            log.warn("give-item option requested without itemToGive");
            return DialogHandleResult.GIVE_ITEM_FAILED;
        }

        if (isInputWorkerThread()) {
            return tryGiveItemFromCurrentOptionDialogDirect(itemToGive, knownBagIndex);
        }

        AtomicReference<DialogHandleResult> result = new AtomicReference<>(DialogHandleResult.GIVE_ITEM_FAILED);
        boolean completed = inputSequences.submitExclusiveAndWait("dialog:giveItemFlow", () -> {
            result.set(tryGiveItemFromCurrentOptionDialogDirect(itemToGive, knownBagIndex));
            return true;
        });
        return completed ? result.get() : DialogHandleResult.INTERRUPTED;
    }

    /**
     * Give an item only when the current dialog is an option dialog.
     *
     * @param itemToGive template path for the item to give; null means the operation fails.
     * @param knownBagIndex optional bag page hint owned by the caller; null allows the bag service
     * to search.
     * @return give-item result or a dialog classification result. When already inside the input
     * worker, this method uses direct input calls; otherwise it delegates to {@link #handleDialog}.
     */
    public DialogHandleResult tryGiveItemIfCurrentOptionDialogDirectForExclusive(String itemToGive, Integer knownBagIndex) {
        if (!isInputWorkerThread()) {
            return handleDialog(DialogHandleRequest.giveItemIfAvailable("dialog-direct", itemToGive, knownBagIndex));
        }

        DialogType type = detectDialogTypeDirect();
        if (type == DialogType.NONE) {
            return DialogHandleResult.NO_DIALOG;
        }
        if (type != DialogType.OPTION) {
            return DialogHandleResult.STORY_IGNORED;
        }
        return tryGiveItemFromCurrentOptionDialogDirect(itemToGive, knownBagIndex);
    }

    private DialogHandleResult tryGiveItemFromCurrentOptionDialogDirect(String itemToGive, Integer knownBagIndex) {
        if (itemToGive == null) {
            log.warn("give-item option requested without itemToGive");
            return DialogHandleResult.GIVE_ITEM_FAILED;
        }

        log.info("give-item option dialog detected, checking give option");
        Point giveTextPt = coordinateHelper.findGreenTextInRegion(OPTION_GIVE_TEXT,
                getSmallDialogRect(), 0.85);
        if (giveTextPt == null) {
            log.warn("give-item option dialog has no give entry");
            return DialogHandleResult.GIVE_OPTION_NOT_FOUND;
        }

        Point safeClick = coordinateHelper.getRandomizedPoint(giveTextPt, 20, 5);
        inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
        if (!sleep(800)) {
            return DialogHandleResult.INTERRUPTED;
        }

        if (giveItemService.executeGiveDirectForExclusive(itemToGive, knownBagIndex)) {
            return DialogHandleResult.GIVE_ITEM_DONE;
        }
        return DialogHandleResult.GIVE_ITEM_FAILED;
    }

    private DialogHandleResult finishRequest(DialogHandleRequest request, DialogType type, DialogHandleResult result) {
        log.info("dialog handle result: source={} operation={} type={} result={}",
                request.getSourceTask(), request.getOperation(), type, result);
        return result;
    }

    /**
     * Detect the current dialog type using the current window screenshot.
     *
     * @return {@link DialogType#NONE} when no known dialog frame is visible.
     */
    public DialogType detectDialogType() {
        return detectDialogTypeNoFocus("detect-dialog-type");
    }

    /**
     * Detect the current dialog type without requesting focus.
     *
     * @param reason short diagnostic label for logs and screenshots.
     * @return detected dialog type; {@link DialogType#NONE} means no known dialog frame matched.
     */
    public DialogType detectDialogTypeNoFocus(String reason) {
        long latencyStart = LatencyMetrics.start();
        DialogType type = detectDialogTypeDirect();
        log.info("dialog detect no-focus: reason={} result={}", reason, type);
        LatencyMetrics.info(log, "dialog.detect", latencyStart,
                "reason=" + reason + " result=" + type);
        return type;
    }

    private DialogType detectDialogTypeDirect() {
        if (!sleep(700 + random.nextInt(100))) {
            return DialogType.NONE;
        }
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

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }

    private boolean hasOptionInLowerHalf() {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y + CROP_TOP_Y, DIALOG_SMALL_W, DIALOG_SMALL_H - CROP_TOP_Y);
        BufferedImage frame = tracker.captureToMemory("opt-scan", area[0], area[1], area[2], area[3]);
        ImagePreprocessor.saveDebugImage(frame, windowScopedTempPath.resolve("opt-scan-debug.png"));
        if (frame == null) return false;

        int count = ImagePreprocessor.countGreenPixelsHSV(frame, windowScopedTempPath.resolve("debug_hsv_mask_green.png"));
        frame.flush();
        return count > 150;
    }

    private boolean hasDialogMask() {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X + CROP_LEFT_X, DIALOG_SMALL_Y + CROP_DEV_Y, DIALOG_SMALL_W - CROP_LEFT_X, DIALOG_SMALL_H - CROP_DEV_Y);
        BufferedImage frame = tracker.captureToMemory("std-scan", area[0], area[1], area[2], area[3]);
        if (frame == null) return false;

        String diagnosticPrefix = windowScopedTempPath.resolve("debug_dialog_std_scan");
        ImagePreprocessor.saveDebugImage(frame, diagnosticPrefix + "_capture_raw.png");
        tracker.captureRegionDiagnostics("dialog-std-scan", diagnosticPrefix,
                area[0], area[1], area[2], area[3]);

        double stddev = ImagePreprocessor.getImageStandardDeviation(frame, windowScopedTempPath.resolve("debug_smoothness_gray.png"));
        frame.flush();
        log.info("sstddev: {}", stddev);
        return stddev < 30.0;
    }

    private boolean hasStoryInUpperHalf() {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, CROP_TOP_Y);
        BufferedImage frame = tracker.captureToMemory("story-scan", area[0], area[1], area[2], area[3]);
        if (frame == null) return false;
        ImagePreprocessor.saveDebugImage(frame, windowScopedTempPath.resolve("story_scan.png"));
        int thinWhiteCount = ImagePreprocessor.countThinWhitePixelsHSV(frame, windowScopedTempPath.resolve("debug_thin_white_text.png"));
        int greenCount = ImagePreprocessor.countGreenPixelsHSV(frame, windowScopedTempPath.resolve("debug_hsv_mask_green.png"));
        frame.flush();

        int totalTextPixels = thinWhiteCount + greenCount;
        return totalTextPixels > 200;
    }

    public void fastClickStoryDialog() {
        if (isInputWorkerThread()) {
            fastClickStoryDialogDirect();
            return;
        }

        inputSequences.submitExclusiveAndWait("dialog:storyClick", this::fastClickStoryDialogDirect);
    }

    private boolean fastClickStoryDialogDirect() {
        if (!sleep(600 + random.nextInt(100))) return false;
        int[] rect = getDialogRect();
        double scale = coordinateHelper.getScaleRatio();
        int cx = rect[0] + (rect[2] - rect[0]) / 2;
        int cy = rect[3] - (int)Math.round(40 / scale);
        Point safeClick = coordinateHelper.getRandomizedPoint(new Point(cx, cy), 30, 10);
        inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
        return sleep(600 + random.nextInt(100));
    }

    private boolean processOptionsWithOCR(String targetKeyword) {
        return processOptionsWithOCR(targetKeyword, true);
    }

    private boolean processOptionsWithOCR(String targetKeyword, boolean allowFallbackOptionClick) {
        int[] rect = getDialogRect();
        String path = windowScopedTempPath.resolve("dialog_active_scan.png");
        if (!captureToFileExclusive("dialog:ocrCapture", "OCR-Scan", path, rect)) return false;

        List<String> aliases = MAP_ALIASES.getOrDefault(targetKeyword, java.util.Collections.singletonList(targetKeyword));
        List<TextRecognizer.OcrWordResult> results = ocr.getAllTextResultsForMatch(
                path,
                "dialog-options:" + targetKeyword,
                words -> hasAnyKeyword(words, aliases));

        for (String alias : aliases) {
            for (TextRecognizer.OcrWordResult word : results) {
                if (word.getText().contains(alias)) {
                    log.info("dialog OCR hit alias={} target={} path={}", alias, targetKeyword, path);
                    inputSequences.clickLeft("dialog:ocrOption", rect[0] + word.getX(), rect[1] + word.getY(), 150);
                    return true;
                }
            }
        }
        if (!allowFallbackOptionClick) {
            log.warn("dialog OCR target not matched and fallback disabled: target={} aliases={} path={} words={}",
                    targetKeyword, aliases, path, summarizeWords(results));
            return false;
        }
        return clickLastGreenOption(rect, "OCR target not matched");
    }

    private boolean hasAnyKeyword(List<TextRecognizer.OcrWordResult> words, List<String> keywords) {
        if (words == null || words.isEmpty() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (TextRecognizer.OcrWordResult word : words) {
            if (word == null || word.getText() == null) {
                continue;
            }
            for (String keyword : keywords) {
                if (keyword != null && !keyword.isBlank() && word.getText().contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String summarizeWords(List<TextRecognizer.OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (TextRecognizer.OcrWordResult word : words) {
            if (word == null) {
                continue;
            }
            if (count++ >= 8) {
                builder.append("...");
                break;
            }
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append(word.getText()).append("@(")
                    .append(word.getX()).append(",")
                    .append(word.getY()).append(")");
        }
        return builder.isEmpty() ? "-" : builder.toString();
    }

    private boolean clickFirstGreenOption(int[] rect, String reason) {
        return clickGreenOption(rect, reason, true);
    }

    private boolean clickLastGreenOption(int[] rect, String reason) {
        return clickGreenOption(rect, reason, false);
    }

    public boolean clickLastGreenOptionDirectForExclusive(String reason) {
        if (!isInputWorkerThread()) {
            return inputSequences.submitExclusiveAndWait("dialog:lastGreenOption:" + reason,
                    () -> clickLastGreenOptionDirectForExclusive(reason));
        }
        DialogType type = detectDialogTypeDirect();
        if (type != DialogType.OPTION) {
            log.info("dialog last green option skipped: reason={} type={}", reason, type);
            return false;
        }
        return clickGreenOptionDirect(getDialogRect(), reason, false);
    }

    public boolean clickFirstGreenOptionIfFiveRingAcceptDialog() {
        return inputSequences.submitExclusiveAndWait("dialog:wuhuanAcceptFirstOption",
                this::clickFirstGreenOptionIfFiveRingAcceptDialogDirect);
    }

    public boolean clickFirstGreenOptionIfFiveRingAcceptDialogDirectForExclusive() {
        if (!isInputWorkerThread()) {
            return clickFirstGreenOptionIfFiveRingAcceptDialog();
        }
        return clickFirstGreenOptionIfFiveRingAcceptDialogDirect();
    }

    private boolean clickFirstGreenOptionIfFiveRingAcceptDialogDirect() {
        if (detectDialogType() != DialogType.OPTION) {
            log.info("wuhuan accept dialog verification failed: current dialog is not OPTION");
            return false;
        }
        Point optionPoint = coordinateHelper.findGreenTextInRegion(WUHAN_ACCEPT_FIRST_OPTION_TEXT, getDialogRect(), 0.85);
        if (optionPoint == null) {
            log.info("五环接任务对话验证失败：未匹配到模板 {}", WUHAN_ACCEPT_FIRST_OPTION_TEXT);
            return false;
        }
        Point safeClick = coordinateHelper.getRandomizedPoint(optionPoint, 20, 4);
        log.info("五环接任务对话验证成功：template={} click=({}, {})",
                WUHAN_ACCEPT_FIRST_OPTION_TEXT, safeClick.x, safeClick.y);
        inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
        return true;
    }

    public boolean clickGreenTemplateOption(String templatePath, String reason, int randomRadiusX, int randomRadiusY) {
        return inputSequences.submitExclusiveAndWait("dialog:greenTemplateOption:" + reason,
                () -> clickGreenTemplateOptionDirect(templatePath, reason, randomRadiusX, randomRadiusY));
    }

    public boolean clickGreenTemplateOptionWithXRange(String templatePath, String reason,
                                                      int minOffsetX, int maxOffsetX, int randomRadiusY) {
        return inputSequences.submitExclusiveAndWait("dialog:greenTemplateOption:" + reason,
                () -> clickGreenTemplateOptionDirectWithXRange(templatePath, reason, minOffsetX, maxOffsetX, randomRadiusY));
    }

    public boolean clickGreenTemplateOptionDirectForExclusive(String templatePath, String reason,
                                                              int randomRadiusX, int randomRadiusY) {
        if (!isInputWorkerThread()) {
            return clickGreenTemplateOption(templatePath, reason, randomRadiusX, randomRadiusY);
        }
        return clickGreenTemplateOptionDirect(templatePath, reason, randomRadiusX, randomRadiusY);
    }

    public boolean clickGreenTemplateOptionDirectForExclusive(String templatePath, String reason,
                                                             int minOffsetX, int maxOffsetX, int randomRadiusY) {
        if (!isInputWorkerThread()) {
            return clickGreenTemplateOptionWithXRange(templatePath, reason, minOffsetX, maxOffsetX, randomRadiusY);
        }
        return clickGreenTemplateOptionDirectWithXRange(templatePath, reason, minOffsetX, maxOffsetX, randomRadiusY);
    }

    public boolean isGreenTemplateOptionVisibleDirectForExclusive(String templatePath, String reason) {
        if (!isInputWorkerThread()) {
            return inputSequences.submitExclusiveAndWait("dialog:greenTemplateVisible:" + reason,
                    () -> isGreenTemplateOptionVisibleDirect(templatePath, reason));
        }
        return isGreenTemplateOptionVisibleDirect(templatePath, reason);
    }

    public String clickFirstGreenTemplateOptionDirectForExclusive(List<GreenTemplateClickSpec> specs, String reason) {
        if (isInputWorkerThread()) {
            return clickFirstGreenTemplateOptionDirect(specs, reason);
        }

        AtomicReference<String> matched = new AtomicReference<>();
        boolean completed = inputSequences.submitExclusiveAndWait("dialog:greenTemplateFirst:" + reason, () -> {
            matched.set(clickFirstGreenTemplateOptionDirect(specs, reason));
            return true;
        });
        return completed ? matched.get() : null;
    }

    public String clickFirstKnownOptionGreenTemplateDirectForExclusive(List<GreenTemplateClickSpec> specs, String reason) {
        if (isInputWorkerThread()) {
            return clickFirstGreenTemplateOptionDirect(specs, reason, false);
        }

        AtomicReference<String> matched = new AtomicReference<>();
        boolean completed = inputSequences.submitExclusiveAndWait("dialog:knownOptionGreenTemplateFirst:" + reason, () -> {
            matched.set(clickFirstGreenTemplateOptionDirect(specs, reason, false));
            return true;
        });
        return completed ? matched.get() : null;
    }

    private boolean isGreenTemplateOptionVisibleDirect(String templatePath, String reason) {
        if (templatePath == null || templatePath.isBlank()) {
            log.warn("dialog green template visibility requested without template: reason={}", reason);
            return false;
        }
        DialogType type = detectDialogTypeDirect();
        if (type != DialogType.OPTION) {
            log.info("dialog green template visibility skipped: reason={} type={}", reason, type);
            return false;
        }
        Point optionPoint = coordinateHelper.findGreenTextInRegion(templatePath, getDialogRect(), 0.85);
        boolean matched = optionPoint != null;
        log.info("dialog green template visibility: reason={} template={} matched={} point={}",
                reason, templatePath, matched, matched ? "(" + optionPoint.x + "," + optionPoint.y + ")" : "-");
        return matched;
    }

    private boolean clickGreenTemplateOptionDirect(String templatePath, String reason,
                                                   int randomRadiusX, int randomRadiusY) {
        return clickGreenTemplateOptionDirectWithXRange(templatePath, reason, -randomRadiusX, randomRadiusX, randomRadiusY);
    }

    private boolean clickGreenTemplateOptionDirectWithXRange(String templatePath, String reason,
                                                            int minOffsetX, int maxOffsetX, int randomRadiusY) {
        long latencyStart = LatencyMetrics.start();
        boolean result = clickGreenTemplateOptionDirectWithXRangeInternal(
                templatePath, reason, minOffsetX, maxOffsetX, randomRadiusY);
        LatencyMetrics.info(log, "dialog.greenTemplateClick", latencyStart,
                "result=" + result + " reason=" + reason + " template=" + templatePath);
        return result;
    }

    private boolean clickGreenTemplateOptionDirectWithXRangeInternal(String templatePath, String reason,
                                                                    int minOffsetX, int maxOffsetX, int randomRadiusY) {
        if (templatePath == null || templatePath.isBlank()) {
            log.warn("dialog green template option requested without template: reason={}", reason);
            return false;
        }
        DialogType type = detectDialogTypeDirect();
        if (type != DialogType.OPTION) {
            log.info("dialog green template option skipped: reason={} type={}", reason, type);
            return false;
        }
        Point optionPoint = coordinateHelper.findGreenTextInRegion(templatePath, getDialogRect(), 0.85);
        if (optionPoint == null) {
            log.info("dialog green template option not matched: reason={} template={}", reason, templatePath);
            return false;
        }
        int lowX = Math.min(minOffsetX, maxOffsetX);
        int highX = Math.max(minOffsetX, maxOffsetX);
        int offsetX = lowX == highX ? lowX : random.nextInt(highX - lowX + 1) + lowX;
        int offsetY = randomRadiusY <= 0 ? 0 : random.nextInt(randomRadiusY * 2 + 1) - randomRadiusY;
        Point safeClick = new Point(optionPoint.x + offsetX, optionPoint.y + offsetY);
        log.info("dialog green template option matched: reason={} template={} match=({}, {}) offset=({}, {}) click=({}, {})",
                reason, templatePath, optionPoint.x, optionPoint.y, offsetX, offsetY, safeClick.x, safeClick.y);
        inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
        return true;
    }

    private String clickFirstGreenTemplateOptionDirect(List<GreenTemplateClickSpec> specs, String reason) {
        return clickFirstGreenTemplateOptionDirect(specs, reason, true);
    }

    private String clickFirstGreenTemplateOptionDirect(List<GreenTemplateClickSpec> specs,
                                                       String reason,
                                                       boolean verifyDialogType) {
        long latencyStart = LatencyMetrics.start();
        String matched = clickFirstGreenTemplateOptionDirectInternal(specs, reason, verifyDialogType);
        LatencyMetrics.info(log, "dialog.greenTemplateFirst", latencyStart,
                "matched=" + (matched == null ? "-" : matched) + " reason=" + reason
                        + " specCount=" + (specs == null ? 0 : specs.size()));
        return matched;
    }

    private String clickFirstGreenTemplateOptionDirectInternal(List<GreenTemplateClickSpec> specs,
                                                              String reason,
                                                              boolean verifyDialogType) {
        /*
         * 多模板 option 匹配的快路径：dialog 类型、截图、洗绿字都只做一次。
         * 调用方负责按业务优先级传 specs，谁先命中就点谁，避免同一个对话框重复截图。
         */
        if (specs == null || specs.isEmpty()) {
            log.warn("dialog green template multi-match requested without specs: reason={}", reason);
            return null;
        }
        if (verifyDialogType) {
            DialogType type = detectDialogTypeDirect();
            if (type != DialogType.OPTION) {
                log.info("dialog green template multi-match skipped: reason={} type={}", reason, type);
                return null;
            }
        }

        int[] rect = getDialogRect();
        String rawPath = windowScopedTempPath.resolve("dialog_green_multi_raw.png");
        String washedPath = windowScopedTempPath.resolve("dialog_green_multi_washed.png");
        if (!tracker.captureToFile("dialog-green-multi", rawPath, rect[0], rect[1], rect[2], rect[3])) {
            log.warn("dialog green template multi-match capture failed: reason={}", reason);
            return null;
        }
        // Normalize green option text before template matching so background texture is less noisy.
        ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath);

        for (GreenTemplateClickSpec spec : specs) {
            if (spec == null || spec.templatePath() == null || spec.templatePath().isBlank()) {
                continue;
            }
            double[] result = ImageFinder.find(washedPath, spec.templatePath(), 0.85);
            if (result == null || result.length < 2) {
                log.info("dialog green template multi-match miss: reason={} name={} template={}",
                        reason, spec.name(), spec.templatePath());
                continue;
            }

            int absoluteX = rect[0] + (int) Math.round(result[0]);
            int absoluteY = rect[1] + (int) Math.round(result[1]);
            int lowX = Math.min(spec.minOffsetX(), spec.maxOffsetX());
            int highX = Math.max(spec.minOffsetX(), spec.maxOffsetX());
            int offsetX = lowX == highX ? lowX : random.nextInt(highX - lowX + 1) + lowX;
            int offsetY = spec.randomRadiusY() <= 0
                    ? 0
                    : random.nextInt(spec.randomRadiusY() * 2 + 1) - spec.randomRadiusY();
            Point safeClick = new Point(absoluteX + offsetX, absoluteY + offsetY);
            log.info("dialog green template multi-match hit: reason={} name={} template={} match=({}, {}) offset=({}, {}) click=({}, {})",
                    reason, spec.name(), spec.templatePath(), absoluteX, absoluteY,
                    offsetX, offsetY, safeClick.x, safeClick.y);
            inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
            return spec.name();
        }

        log.info("dialog green template multi-match no hit: reason={} candidates={}", reason, specs.size());
        return null;
    }

    public String readCurrentStoryGreenText(String reason) {
        DialogType type = detectDialogTypeNoFocus("read-story-green:" + reason);
        if (type != DialogType.STORY) {
            log.info("dialog story green read skipped: reason={} type={}", reason, type);
            return "";
        }
        int[] rect = getDialogRect();
        String rawPath = windowScopedTempPath.resolve("story_green_raw.png");
        String washedPath = windowScopedTempPath.resolve("story_green_washed.png");
        if (!tracker.captureToFile("story-green-read", rawPath, rect[0], rect[1], rect[2], rect[3])) {
            log.warn("dialog story green read capture failed: reason={}", reason);
            return "";
        }
        ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath);
        String text = ocr.readText(washedPath);
        log.info("dialog story green read: reason={} text={}", reason, text);
        return text == null ? "" : text.trim();
    }

    public BufferedImage captureCurrentStoryImage(String reason) {
        DialogType type = detectDialogTypeNoFocus("capture-story-image:" + reason);
        if (type != DialogType.STORY) {
            log.info("dialog story capture skipped: reason={} type={}", reason, type);
            return null;
        }
        int[] rect = getDialogRect();
        BufferedImage image = tracker.captureToMemory("story-objective-" + reason,
                rect[0], rect[1], rect[2], rect[3]);
        if (image == null) {
            log.warn("dialog story capture failed: reason={}", reason);
            return null;
        }
        saveStoryObjectiveDebugImage(reason, image);
        return image;
    }

    private void saveStoryObjectiveDebugImage(String reason, BufferedImage image) {
        String safeReason = reason == null || reason.isBlank() ? "unknown" : reason;
        String latestPath = windowScopedTempPath.resolve("story_objective_" + safeReason + ".png");
        String historyPath = windowScopedTempPath.resolve("story_objective_" + safeReason + "_" + System.currentTimeMillis() + ".png");
        saveStoryObjectiveDebugImageToPath(safeReason, image, latestPath);
        saveStoryObjectiveDebugImageToPath(safeReason, image, historyPath);
    }

    private void saveStoryObjectiveDebugImageToPath(String reason, BufferedImage image, String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("dialog story objective debug mkdir failed: path={}", parent);
            return;
        }
        try {
            ImageIO.write(image, "png", file);
            log.info("dialog story objective debug saved: reason={} path={}", reason, path);
        } catch (IOException e) {
            log.warn("dialog story objective debug save failed: reason={} path={}", reason, path, e);
        }
    }

    private boolean clickGreenOption(int[] rect, String reason, boolean first) {
        return inputSequences.submitExclusiveAndWait(first ? "dialog:firstGreenOption" : "dialog:lastGreenOption",
                () -> clickGreenOptionDirect(rect, reason, first));
    }

    private boolean clickGreenOptionDirect(int[] rect, String reason, boolean first) {
        String elementName = first ? "dialog-first-green-option" : "dialog-last-green-option";
        BufferedImage frame = tracker.captureToMemory(elementName, rect[0], rect[1], rect[2], rect[3]);
        if (frame == null) {
            log.warn("dialog green option capture failed: reason={} first={}", reason, first);
            return false;
        }
        try {
            List<GreenTextBand> bands = findGreenTextBands(frame);
            GreenTextBand band = pickGreenTextBand(bands, first);
            if (band == null) {
                log.warn("dialog green option not found: reason={} first={}", reason, first);
                return false;
            }
            int clickX = rect[0] + (band.minX + band.maxX) / 2;
            int clickY = rect[1] + (band.minY + band.maxY) / 2;
            Point safeClick = coordinateHelper.getRandomizedPoint(new Point(clickX, clickY), 12, 3);
            log.info("dialog click green option: reason={} first={} band={} click=({}, {})",
                    reason, first, band, safeClick.x, safeClick.y);
            inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
            return true;
        } finally {
            frame.flush();
        }
    }

    private List<GreenTextBand> findGreenTextBands(BufferedImage frame) {
        int height = frame.getHeight();
        int width = frame.getWidth();
        int[] rowCounts = new int[height];
        for (int y = 0; y < height; y++) {
            int count = 0;
            for (int x = 0; x < width; x++) {
                if (ImagePreprocessor.isOptionGreen(frame.getRGB(x, y))) {
                    count++;
                }
            }
            rowCounts[y] = count;
        }

        List<GreenTextBand> bands = new java.util.ArrayList<>();
        int startY = -1;
        int endY = -1;
        int gap = 0;
        for (int y = 0; y < height; y++) {
            if (rowCounts[y] >= 3) {
                if (startY < 0) {
                    startY = y;
                }
                endY = y;
                gap = 0;
            } else if (startY >= 0) {
                gap++;
                if (gap > 2) {
                    GreenTextBand band = buildGreenTextBand(frame, startY, endY);
                    if (band != null) {
                        bands.add(band);
                    }
                    startY = -1;
                    endY = -1;
                    gap = 0;
                }
            }
        }
        if (startY >= 0) {
            GreenTextBand band = buildGreenTextBand(frame, startY, endY);
            if (band != null) {
                bands.add(band);
            }
        }

        return bands;
    }

    private GreenTextBand pickGreenTextBand(List<GreenTextBand> bands, boolean first) {
        if (bands == null || bands.isEmpty()) {
            return null;
        }
        return first ? bands.get(0) : bands.get(bands.size() - 1);
    }

    private GreenTextBand buildGreenTextBand(BufferedImage frame, int startY, int endY) {
        int minX = frame.getWidth();
        int maxX = -1;
        int greenPixels = 0;
        for (int y = startY; y <= endY; y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                if (ImagePreprocessor.isOptionGreen(frame.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    greenPixels++;
                }
            }
        }

        if (greenPixels < 30 || maxX < minX) {
            return null;
        }
        return new GreenTextBand(minX, startY, maxX, endY, greenPixels);
    }

    private record GreenTextBand(int minX, int minY, int maxX, int maxY, int pixels) {
    }

    private boolean doFallbackClick(int[] rect, String reason) {
        log.warn("dialog fixed fallback click: {}", reason);
        double scale = coordinateHelper.getScaleRatio();
        int cx = rect[0] + (rect[2] - rect[0]) / 2;
        int cy = rect[3] - (int)Math.round(100 / scale);
        inputSequences.clickLeft("dialog:fallback", cx, cy, 150);
        return true;
    }

    public int[] getDialogRect() {
        return coordinateHelper.getScaledRect(DIALOG_LARGE_X, DIALOG_LARGE_Y, DIALOG_LARGE_W, DIALOG_LARGE_H);
    }

    public int[] getSmallDialogRect() {
        return coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, DIALOG_SMALL_H);
    }

    private boolean captureToFileExclusive(String description, String elementName, String path, int[] rect) {
        return inputSequences.submitExclusiveAndWait(description,
                () -> tracker.captureToFile(elementName, path, rect[0], rect[1], rect[2], rect[3]));
    }

    private boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
