package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.model.dialog.DialogDetection;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import com.bot.dhxy.model.ocr.OcrLineResult;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogOptionClickResult;
import com.bot.dhxy.service.dialog.DialogStoryPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.vision.GameTextLineOcrService;
import com.bot.dhxy.vision.ObjectiveTextRecognitionService;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
    private final GiveItemService giveItemService;
    private final WindowScopedTempPath windowScopedTempPath;
    private final GameTextLineOcrService gameTextLineOcrService;
    private final ObjectiveTextRecognitionService objectiveTextRecognitionService;

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
    private static final String HEAL_PET_OPTION_TEXT = "images/template/dialog/heal_pet_option.png";
    private static final String REPAIR_EQUIPMENT_OPTION_TEXT = "images/template/dialog/repair_equipment_option.png";
    private static final String REPAIR_EQUIPMENT_GIVEUP_OPTION_TEXT = "images/template/dialog/repair_equipment_option_giveup.png";
    private static final double BUSINESS_OPTION_MATCH_RATE = 0.70;
    private static final int ROUTE_TRANSFER_DIALOG_ATTEMPTS = 2;
    private static final long ROUTE_TRANSFER_RETRY_DELAY_MS = 650L;

    /**
     * Handle one dialog according to an explicit operation policy and return structured details.
     *
     * @param request policy object that declares the narrow dialog operation to run. Green-template
     *                requests carry only the templates that this task phase expects, so the service
     *                does not sweep every known task dialog.
     * @return structured status, optional action key, and click coordinates when a concrete option
     * was clicked.
     */
    public DialogResult handleDialog(DialogHandleRequest request) {
        log.info("dialog handle request: source={} operation={} storyPolicy={} optionPolicy={} fallbackPolicy={} itemToGive={} targetKeyword={}",
                request.getSourceTask(), request.getOperation(), request.getStoryPolicy(), request.getOptionPolicy(),
                request.getFallbackPolicy(),
                request.getItemToGive(), request.getTargetKeyword());

        // Stage 1: optional opening click, used by tasks that first need to poke an NPC or object.
        if (request.getInitialClick() != null) {
            Point p = request.getInitialClick();
            log.info("dialog request initial click: ({},{})", p.x, p.y);
            inputSequences.clickLeft("dialog:requestInitialClick", p.x, p.y, 150);
            if (!TaskSleep.sleep(600 + random.nextInt(200))) {
                return finishRequest(request, DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.NONE));
            }
        }

        if (request.getOptionPolicy() == com.bot.dhxy.service.dialog.DialogOptionPolicy.CLICK_GREEN_TEMPLATE) {
            return finishRequest(request, handleGreenTemplateOption(request));
        }

        // Stage 2: classify once and pass the same screenshot into option OCR.
        DialogDetection detection = detectDialogSnapshotDirect("handle-dialog:" + request.getOperation());
        DialogType type = detection.type();
        if (type == DialogType.NONE) {
            if (request.getOperation() == com.bot.dhxy.service.dialog.DialogOperation.ROUTE_TRANSFER
                    && request.getOptionPolicy() == com.bot.dhxy.service.dialog.DialogOptionPolicy.CLICK_KEYWORD
                    && detection.image() != null) {
                DialogResult uncertainResult = handleRouteKeywordOptionWithRetry(request, detection);
                if (uncertainResult.getStatus() == DialogResultStatus.OPTION_KEYWORD_CLICKED) {
                    return finishRequest(request, uncertainResult);
                }
            }
            return finishRequest(request, DialogResult.simple(DialogResultStatus.NO_DIALOG, type));
        }

        // Stage 3: story dialogs are either clicked through or deliberately left alone.
        if (type == DialogType.STORY) {
            if (request.getOperation() == com.bot.dhxy.service.dialog.DialogOperation.READ_STORY_OBJECTIVE) {
                return finishRequest(request, handleStoryObjective(request, detection));
            }
            if (request.getOperation() == com.bot.dhxy.service.dialog.DialogOperation.VERIFY_WHITE_TEMPLATE) {
                return finishRequest(request, verifyWhiteStoryTemplate(request, detection));
            }
            if (request.getStoryPolicy() == DialogStoryPolicy.CLICK_THROUGH) {
                handleStoryDialog();
                return finishRequest(request, DialogResult.simple(DialogResultStatus.STORY_CLICKED, type));
            }
            return finishRequest(request, DialogResult.simple(DialogResultStatus.STORY_IGNORED, type));
        }

        // Stage 4: option dialogs are handled by the request's explicit option policy.
        DialogResult result = switch (request.getOptionPolicy()) {
            case IGNORE -> DialogResult.simple(DialogResultStatus.OPTION_IGNORED, type);
            case VERIFY_OPTION -> DialogResult.simple(DialogResultStatus.OPTION_VISIBLE, type);
            case CLICK_KEYWORD -> request.getOperation() == com.bot.dhxy.service.dialog.DialogOperation.ROUTE_TRANSFER
                    ? handleRouteKeywordOptionWithRetry(request, detection)
                    : handleKeywordOption(request, detection);
            case CLICK_REMEMBERED_POINT -> handleRememberedOption(request, detection);
            case CLICK_BUSINESS_OPTION -> fromHandleResult(handleBusinessOption(request.isIncludeCleanupBusinessOptions()), type);
            case GIVE_ITEM_IF_AVAILABLE -> fromHandleResult(
                    tryGiveItemFromCurrentOptionDialog(request.getItemToGive(), request.getKnownBagIndex()), type);
            case CLICK_GREEN_TEMPLATE -> handleGreenTemplateOption(request);
            case VERIFY_GREEN_TEMPLATE -> verifyGreenTemplateOption(request, detection);
            case FALLBACK_FIRST_OPTION -> DialogResult.simple(
                    clickGreenOption(getDialogRect(), "request fallback first green option", true)
                            ? DialogResultStatus.FALLBACK_CLICKED
                            : DialogResultStatus.FAILED,
                    type);
            case FALLBACK_LAST_OPTION -> DialogResult.simple(
                    clickGreenOption(getDialogRect(), "request fallback last green option", false)
                            ? DialogResultStatus.FALLBACK_CLICKED
                            : DialogResultStatus.FAILED,
                    type);
        };
        return finishRequest(request, result);
    }

    private DialogResult verifyGreenTemplateOption(DialogHandleRequest request, DialogDetection detection) {
        List<GreenTemplateClickSpec> specs = request.getGreenTemplateSpecs();
        if (specs == null || specs.isEmpty()) {
            log.warn("dialog expected template verification requested without specs: source={}", request.getSourceTask());
            return DialogResult.simple(DialogResultStatus.GREEN_TEMPLATE_NOT_FOUND, detection.type());
        }

        int[] rect = detection.dialogRect() != null ? detection.dialogRect() : getDialogRect();
        String rawPath = detection.rawPath();
        if ((rawPath == null || rawPath.isBlank()) && detection.image() != null) {
            rawPath = windowScopedTempPath.resolve("dialog_expected_verify_raw.png");
            if (!ImagePreprocessor.saveImage(detection.image(), rawPath)) {
                rawPath = null;
            }
        }
        if (rawPath == null || rawPath.isBlank()) {
            return DialogResult.simple(DialogResultStatus.FAILED, detection.type());
        }

        String washedPath = windowScopedTempPath.resolve("dialog_expected_verify_green.png");
        ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(rawPath, washedPath);
        for (GreenTemplateClickSpec spec : specs) {
            if (spec == null || spec.templatePath() == null || spec.templatePath().isBlank()) {
                continue;
            }
            double[] result = ImageFinder.find(washedPath, spec.templatePath(), 0.85);
            if (result == null || result.length < 2) {
                continue;
            }
            Point point = coordinateHelper.resolveMatchedPointInRect(rect, result);
            log.info("dialog expected template visible: source={} template={} point=({}, {})",
                    request.getSourceTask(), spec.templatePath(), point.x, point.y);
            return DialogResult.statusBuilder(DialogResultStatus.GREEN_TEMPLATE_VISIBLE, detection.type())
                    .actionKey(spec.name())
                    .matchedText(spec.templatePath())
                    .relativeX(point.x - rect[0])
                    .relativeY(point.y - rect[1])
                    .absoluteX(point.x)
                    .absoluteY(point.y)
                    .build();
        }
        log.info("dialog expected template not visible: source={} candidates={}",
                request.getSourceTask(), specs.size());
        return DialogResult.simple(DialogResultStatus.GREEN_TEMPLATE_NOT_FOUND, detection.type());
    }

    private DialogResult verifyWhiteStoryTemplate(DialogHandleRequest request, DialogDetection detection) {
        String templatePath = request.getExpectedTemplatePath();
        if (templatePath == null || templatePath.isBlank()) {
            log.warn("dialog white template verification requested without template: source={}", request.getSourceTask());
            return DialogResult.simple(DialogResultStatus.WHITE_TEMPLATE_NOT_FOUND, detection.type());
        }

        int[] rect = detection.dialogRect() != null ? detection.dialogRect() : getDialogRect();
        String rawPath = detection.rawPath();
        if ((rawPath == null || rawPath.isBlank()) && detection.image() != null) {
            rawPath = windowScopedTempPath.resolve("dialog_white_template_raw.png");
            if (!ImagePreprocessor.saveImage(detection.image(), rawPath)) {
                rawPath = null;
            }
        }
        if (rawPath == null || rawPath.isBlank()) {
            return DialogResult.simple(DialogResultStatus.FAILED, detection.type());
        }

        String washedPath = windowScopedTempPath.resolve("dialog_white_template_washed.png");
        ImagePreprocessor.washThinWhiteTextToBlackAndWhite(rawPath, washedPath);
        double[] result = ImageFinder.find(washedPath, templatePath, 0.85);
        if (result == null || result.length < 2) {
            log.info("dialog white template not visible: source={} template={}",
                    request.getSourceTask(), templatePath);
            return DialogResult.simple(DialogResultStatus.WHITE_TEMPLATE_NOT_FOUND, detection.type());
        }

        Point point = coordinateHelper.resolveMatchedPointInRect(rect, result);
        log.info("dialog white template visible: source={} template={} point=({}, {})",
                request.getSourceTask(), templatePath, point.x, point.y);
        return DialogResult.statusBuilder(DialogResultStatus.WHITE_TEMPLATE_VISIBLE, detection.type())
                .actionKey(request.getExpectedTemplateActionKey())
                .matchedText(templatePath)
                .relativeX(point.x - rect[0])
                .relativeY(point.y - rect[1])
                .absoluteX(point.x)
                .absoluteY(point.y)
                .build();
    }

    private DialogResult handleRouteKeywordOptionWithRetry(DialogHandleRequest request,
                                                           DialogDetection firstDetection) {
        if (request.getTargetKeyword() == null) {
            log.warn("dialog route keyword option requested without targetKeyword");
            return DialogResult.simple(DialogResultStatus.OPTION_KEYWORD_NOT_FOUND,
                    firstDetection == null ? DialogType.NONE : firstDetection.type());
        }

        DialogDetection detection = firstDetection;
        DialogResult lastResult = DialogResult.simple(DialogResultStatus.NO_DIALOG, DialogType.NONE);
        for (int attempt = 1; attempt <= ROUTE_TRANSFER_DIALOG_ATTEMPTS; attempt++) {
            if (detection == null || detection.image() == null) {
                lastResult = DialogResult.simple(DialogResultStatus.NO_DIALOG, DialogType.NONE);
            } else {
                /*
                 * Route option text is normally green. Yellow is only a supplement for rare
                 * recommendation choices; fresh transfer fee/status text can also be yellow and can
                 * briefly overlap the dialog area. Therefore fallback clicking is delayed until the
                 * final attempt, after the transient yellow hint has had a chance to disappear.
                 */
                boolean allowFallback = attempt == ROUTE_TRANSFER_DIALOG_ATTEMPTS
                        && request.isAllowFallbackOptionClick();
                DialogOptionClickResult clickResult = processOptionsWithOCRDetailed(
                        request.getTargetKeyword(),
                        allowFallback,
                        detection);
                lastResult = fromOptionClickResult(clickResult, detection.type(), request.getTargetKeyword());
                if (clickResult.getResult() == DialogResultStatus.OPTION_KEYWORD_CLICKED
                        || clickResult.getResult() == DialogResultStatus.FALLBACK_CLICKED) {
                    log.info("dialog route keyword handled: source={} target={} attempt={} status={}",
                            request.getSourceTask(), request.getTargetKeyword(), attempt, clickResult.getResult());
                    return lastResult;
                }
            }

            if (attempt < ROUTE_TRANSFER_DIALOG_ATTEMPTS) {
                log.info("dialog route keyword not matched, retry current dialog after transient hint: source={} target={} attempt={} delayMs={}",
                        request.getSourceTask(), request.getTargetKeyword(), attempt, ROUTE_TRANSFER_RETRY_DELAY_MS);
                if (!TaskSleep.sleep(ROUTE_TRANSFER_RETRY_DELAY_MS)) {
                    return DialogResult.simple(DialogResultStatus.INTERRUPTED,
                            detection == null ? DialogType.NONE : detection.type());
                }
                detection = detectDialogSnapshotDirect("route-transfer-retry:" + request.getSourceTask());
            }
        }
        return lastResult;
    }

    /**
     * Match known maintenance business options in the current option dialog.
     *
     * <p>This path is used by idle/member windows to handle team broadcasts such as heal-pet and
     * repair-equipment. The optional repair-giveup cleanup template is only included when the caller
     * explicitly allows cleanup options.</p>
     */
    private DialogResultStatus handleBusinessOption(boolean includeCleanupOptions) {
        int[] rect = getDialogRect();
        String rawPath = windowScopedTempPath.resolve("business_dialog_raw.png");
        String washedPath = windowScopedTempPath.resolve("business_dialog_washed.png");
        boolean captured;
        if (isInputWorkerThread()) {
            BufferedImage image = tracker.captureToMemory("business-dialog-scan", rect[0], rect[1], rect[2], rect[3]);
            try {
                captured = ImagePreprocessor.saveImage(image, rawPath);
            } finally {
                if (image != null) {
                    image.flush();
                }
            }
        } else {
            captured = inputSequences.submitExclusiveAndWait("dialog:businessCapture", () -> {
                BufferedImage image = tracker.captureToMemory("business-dialog-scan", rect[0], rect[1], rect[2], rect[3]);
                try {
                    return ImagePreprocessor.saveImage(image, rawPath);
                } finally {
                    if (image != null) {
                        image.flush();
                    }
                }
            });
        }
        if (!captured) {
            log.warn("business dialog capture failed");
            return DialogResultStatus.FAILED;
        }

        ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath);
        if (tryClickBusinessOptionInWashedImage(washedPath, rect, HEAL_PET_OPTION_TEXT, "heal-pet")) {
            return DialogResultStatus.BUSINESS_OPTION_CLICKED;
        }
        if (tryClickBusinessOptionInWashedImage(washedPath, rect, REPAIR_EQUIPMENT_OPTION_TEXT, "repair-equipment")) {
            return DialogResultStatus.BUSINESS_OPTION_CLICKED;
        }
        if (includeCleanupOptions
                && tryClickBusinessOptionInWashedImage(washedPath, rect, REPAIR_EQUIPMENT_GIVEUP_OPTION_TEXT, "repair-equipment-giveup")) {
            return DialogResultStatus.BUSINESS_OPTION_CLICKED;
        }

        log.info("business dialog option not matched");
        return DialogResultStatus.BUSINESS_OPTION_NOT_FOUND;
    }

    private boolean tryClickBusinessOptionInWashedImage(String washedPath, int[] rect, String templatePath, String optionName) {
        double[] result = ImageFinder.find(washedPath, templatePath, BUSINESS_OPTION_MATCH_RATE);
        if (result == null || result.length < 2) {
            log.info("business dialog option not matched: option={} template={}", optionName, templatePath);
            return false;
        }

        Point optionPoint = coordinateHelper.resolveMatchedPointInRect(rect, result);
        Point safeClick = coordinateHelper.getRandomizedPoint(optionPoint, 4, 3);
        log.info("business dialog option matched: option={} score={} click=({}, {})",
                optionName, result.length > 2 ? result[2] : 0.0, safeClick.x, safeClick.y);
        inputSequences.clickLeft("dialog:businessOption:" + optionName, safeClick.x, safeClick.y, 150);
        return TaskSleep.sleep(800 + random.nextInt(300));
    }

    private DialogResult handleKeywordOption(DialogHandleRequest request, DialogDetection detection) {
        if (request.getTargetKeyword() == null) {
            log.warn("dialog keyword option requested without targetKeyword");
            return DialogResult.simple(DialogResultStatus.OPTION_KEYWORD_NOT_FOUND, detection.type());
        }
        DialogOptionClickResult clickResult = processOptionsWithOCRDetailed(
                request.getTargetKeyword(),
                request.isAllowFallbackOptionClick(),
                detection);
        return fromOptionClickResult(clickResult, detection.type(), request.getTargetKeyword());
    }

    private DialogResult handleRememberedOption(DialogHandleRequest request, DialogDetection detection) {
        Integer relativeX = request.getRememberedRelativeX();
        Integer relativeY = request.getRememberedRelativeY();
        if (relativeX == null || relativeY == null) {
            log.warn("dialog remembered option requested without relative point: source={}", request.getSourceTask());
            return DialogResult.simple(DialogResultStatus.OPTION_KEYWORD_NOT_FOUND, detection.type());
        }
        if (detection.type() != DialogType.OPTION) {
            return DialogResult.simple(DialogResultStatus.OPTION_IGNORED, detection.type());
        }

        int[] rect = detection.dialogRect() != null ? detection.dialogRect() : getDialogRect();
        int x = rect[0] + relativeX;
        int y = rect[1] + relativeY;
        Point safeClick = coordinateHelper.getRandomizedPoint(new Point(x, y), 4, 3);
        log.info("dialog remembered option click: source={} target={} rel=({}, {}) click=({}, {})",
                request.getSourceTask(), request.getTargetKeyword(), relativeX, relativeY, safeClick.x, safeClick.y);
        inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
        if (!TaskSleep.sleep(500 + random.nextInt(150))) {
            return DialogResult.simple(DialogResultStatus.INTERRUPTED, detection.type());
        }
        return DialogResult.statusBuilder(DialogResultStatus.OPTION_KEYWORD_CLICKED, detection.type())
                .actionKey(request.getTargetKeyword())
                .matchedText("remembered-option")
                .relativeX(relativeX)
                .relativeY(relativeY)
                .absoluteX(safeClick.x)
                .absoluteY(safeClick.y)
                .build();
    }

    private DialogResultStatus tryGiveItemFromCurrentOptionDialog(String itemToGive, Integer knownBagIndex) {
        if (itemToGive == null) {
            log.warn("give-item option requested without itemToGive");
            return DialogResultStatus.GIVE_ITEM_FAILED;
        }

        if (!isInputWorkerThread()) {
            AtomicReference<DialogResultStatus> result = new AtomicReference<>(DialogResultStatus.GIVE_ITEM_FAILED);
            boolean completed = inputSequences.submitExclusiveAndWait("dialog:giveItemFlow", () -> {
                result.set(tryGiveItemFromCurrentOptionDialog(itemToGive, knownBagIndex));
                return true;
            });
            return completed ? result.get() : DialogResultStatus.INTERRUPTED;
        }

        log.info("give-item option dialog detected, checking give option");
        Point giveTextPt = coordinateHelper.findGreenTextInRegion(OPTION_GIVE_TEXT,
                getSmallDialogRect(), 0.85);
        if (giveTextPt == null) {
            log.warn("give-item option dialog has no give entry");
            return DialogResultStatus.GIVE_OPTION_NOT_FOUND;
        }

        Point safeClick = coordinateHelper.getRandomizedPoint(giveTextPt, 20, 5);
        inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
        if (!TaskSleep.sleep(800)) {
            return DialogResultStatus.INTERRUPTED;
        }

        if (giveItemService.executeGiveDirectForExclusive(itemToGive, knownBagIndex)) {
            return DialogResultStatus.GIVE_ITEM_DONE;
        }
        return DialogResultStatus.GIVE_ITEM_FAILED;
    }

    private DialogResult finishRequest(DialogHandleRequest request, DialogResult result) {
        log.info("dialog handle result: source={} operation={} type={} status={} kind={} actionKey={} clicked={}",
                request.getSourceTask(), request.getOperation(), result.getDialogType(), result.getStatus(),
                result.getKind(), result.getActionKey(), result.isClicked());
        return result;
    }

    private DialogResult handleStoryObjective(DialogHandleRequest request, DialogDetection detection) {
        BufferedImage image = detection.image();
        if (image == null) {
            return DialogResult.simple(DialogResultStatus.STORY_OBJECTIVE_NOT_FOUND, detection.type());
        }

        /*
         * 修罗接任务后的 story 文本在小对话框内。detection 为了统一判断会截最大框，
         * 但最大框在小 story 场景下会带入上下背景，洗绿字时可能把场景特效当成文本。
         * 这里复用同一次截图，只裁出小 story 框，避免重新截图和背景污染。
         */
        BufferedImage objectiveImage = cropStoryObjectiveImage(detection);
        saveStoryObjectiveDebugImage(request.getSourceTask(), objectiveImage);
        Optional<ObjectiveTextResult> objective = objectiveTextRecognitionService.recognize(
                objectiveImage, "dialog-story-objective:" + request.getSourceTask());
        if (objectiveImage != image) {
            objectiveImage.flush();
        }
        if (objective.isEmpty()) {
            return DialogResult.simple(DialogResultStatus.STORY_OBJECTIVE_NOT_FOUND, detection.type());
        }
        ObjectiveTextResult value = objective.get();
        return DialogResult.statusBuilder(DialogResultStatus.STORY_OBJECTIVE_READ, detection.type())
                .objective(value)
                .matchedText(value.mapName() + "(" + value.x() + "," + value.y() + ")")
                .build();
    }

    private BufferedImage cropStoryObjectiveImage(DialogDetection detection) {
        BufferedImage image = detection.image();
        if (image == null || detection.dialogRect() == null) {
            return image;
        }
        BufferedImage cropped = ImagePreprocessor.cropAbsoluteRect(image, detection.dialogRect(), getSmallDialogRect());
        return cropped != null ? cropped : image;
    }

    private DialogResult fromOptionClickResult(DialogOptionClickResult clickResult,
                                               DialogType type,
                                               String actionKey) {
        DialogResultStatus status = clickResult.getResult();
        return DialogResult.statusBuilder(status, type)
                .actionKey(status == DialogResultStatus.OPTION_KEYWORD_CLICKED ? actionKey : null)
                .matchedText(clickResult.getMatchedText())
                .relativeX(clickResult.getRelativeX())
                .relativeY(clickResult.getRelativeY())
                .absoluteX(clickResult.getAbsoluteX())
                .absoluteY(clickResult.getAbsoluteY())
                .build();
    }

    private DialogResult fromHandleResult(DialogResultStatus status, DialogType type) {
        return DialogResult.simple(status, type);
    }

    /**
     * Detect the current dialog type without requesting focus.
     *
     * @param reason short diagnostic label for logs and screenshots.
     * @return detected dialog type; {@link DialogType#NONE} means no known dialog frame matched.
     */
    private DialogType detectDialogTypeNoFocus(String reason) {
        return detectDialogSnapshotDirect(reason).type();
    }

    private DialogDetection detectDialogSnapshotDirect(String reason) {
        long latencyStart = LatencyMetrics.start();
        DialogDetection detection = DialogDetection.none();
        try {
            if (!TaskSleep.sleep(700 + random.nextInt(100))) {
                return detection;
            }
            detection = captureDialogSnapshot(reason);
            if (detection.image() == null) {
                return detection;
            }
            if (!hasDialogMask(detection)) {
                return detection;
            }

            if (hasOptionInLowerHalf(detection, reason)) {
                detection = detection.withType(DialogType.OPTION);
                return detection;
            }

            if (hasStoryInUpperHalf(detection, reason)) {
                detection = detection.withType(DialogType.STORY);
                return detection;
            }

            log.debug("dialog mask exists but no text found");
            return detection;
        } finally {
            if (detection.type() == DialogType.NONE) {
                log.debug("dialog detect no-focus: reason={} result={}", reason, detection.type());
                log.debug("[latency] event=dialog.detect elapsedMs={} detail={}",
                        LatencyMetrics.elapsedMs(latencyStart),
                        "reason=" + reason + " result=" + detection.type());
            } else {
                log.info("dialog detect no-focus: reason={} result={}", reason, detection.type());
                LatencyMetrics.info(log, "dialog.detect", latencyStart,
                        "reason=" + reason + " result=" + detection.type());
            }
        }
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }

    private DialogDetection captureDialogSnapshot(String reason) {
        int[] rect = getDialogRect();
        BufferedImage image = tracker.captureToMemory("dialog-snapshot:" + reason, rect[0], rect[1], rect[2], rect[3]);
        if (image == null) {
            return DialogDetection.none();
        }
        String rawPath = windowScopedTempPath.resolve("dialog_detect_" + safeDebugName(reason) + "_raw.png");
        if (!ImagePreprocessor.saveImage(image, rawPath)) {
            rawPath = null;
        }
        return DialogDetection.builder()
                .type(DialogType.NONE)
                .dialogRect(rect)
                .rawPath(rawPath)
                .image(image)
                .build();
    }

    private boolean hasOptionInLowerHalf(DialogDetection detection, String reason) {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y + CROP_TOP_Y, DIALOG_SMALL_W, DIALOG_SMALL_H - CROP_TOP_Y);
        BufferedImage frame = ImagePreprocessor.cropAbsoluteRect(detection.image(), detection.dialogRect(), area);
        if (frame == null) return false;

        String debugPrefix = "dialog_detect_" + safeDebugName(reason) + "_option_lower";
        String optionRawPath = windowScopedTempPath.resolve(debugPrefix + "_raw.png");
        String optionGreenPath = windowScopedTempPath.resolve(debugPrefix + "_green.png");
        String optionYellowPath = windowScopedTempPath.resolve(debugPrefix + "_yellow.png");
        ImagePreprocessor.saveImage(frame, optionRawPath);
        int greenCount = ImagePreprocessor.countGreenPixelsHSV(frame, optionGreenPath);
        int yellowCount = ImagePreprocessor.countYellowPixels(frame);
        ImagePreprocessor.washYellowText(optionRawPath, optionYellowPath);
        frame.flush();
        boolean option = greenCount > 150 || yellowCount > 120;
        if (option) {
            log.info("dialog option lower check: reason={} rect={} raw={} green={} yellow={} greenPath={} yellowPath={} result={}",
                    reason, ImagePreprocessor.rectToString(area), optionRawPath, greenCount, yellowCount,
                    optionGreenPath, optionYellowPath, true);
        } else {
            log.debug("dialog option lower check: reason={} rect={} raw={} green={} yellow={} greenPath={} yellowPath={} result={}",
                    reason, ImagePreprocessor.rectToString(area), optionRawPath, greenCount, yellowCount,
                    optionGreenPath, optionYellowPath, false);
        }
        return option;
    }

    private boolean hasDialogMask(DialogDetection detection) {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X + CROP_LEFT_X, DIALOG_SMALL_Y + CROP_DEV_Y, DIALOG_SMALL_W - CROP_LEFT_X, DIALOG_SMALL_H - CROP_DEV_Y);
        BufferedImage frame = ImagePreprocessor.cropAbsoluteRect(detection.image(), detection.dialogRect(), area);
        if (frame == null) return false;

        double stddev = ImagePreprocessor.getImageStandardDeviation(frame, windowScopedTempPath.resolve("debug_smoothness_gray.png"));
        frame.flush();
        log.debug("dialog mask stddev={}", stddev);
        return stddev < 30.0;
    }

    private boolean hasStoryInUpperHalf(DialogDetection detection, String reason) {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, CROP_TOP_Y);
        BufferedImage frame = ImagePreprocessor.cropAbsoluteRect(detection.image(), detection.dialogRect(), area);
        if (frame == null) return false;
        String debugPrefix = "dialog_detect_" + safeDebugName(reason) + "_story_upper";
        String storyRawPath = windowScopedTempPath.resolve(debugPrefix + "_raw.png");
        String storyWhitePath = windowScopedTempPath.resolve(debugPrefix + "_white.png");
        String storyGreenPath = windowScopedTempPath.resolve(debugPrefix + "_green.png");
        ImagePreprocessor.saveImage(frame, storyRawPath);
        int thinWhiteCount = ImagePreprocessor.countThinWhitePixelsHSV(frame, storyWhitePath);
        int greenCount = ImagePreprocessor.countGreenPixelsHSV(frame, storyGreenPath);
        frame.flush();

        int totalTextPixels = thinWhiteCount + greenCount;
        boolean story = totalTextPixels > 200;
        if (story) {
            log.info("dialog story upper check: reason={} rect={} raw={} thinWhite={} green={} total={} whitePath={} greenPath={} result={}",
                    reason, ImagePreprocessor.rectToString(area), storyRawPath, thinWhiteCount, greenCount,
                    totalTextPixels, storyWhitePath, storyGreenPath, true);
        } else {
            log.debug("dialog story upper check: reason={} rect={} raw={} thinWhite={} green={} total={} whitePath={} greenPath={} result={}",
                    reason, ImagePreprocessor.rectToString(area), storyRawPath, thinWhiteCount, greenCount,
                    totalTextPixels, storyWhitePath, storyGreenPath, false);
        }
        return story;
    }

    private String safeDebugName(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown";
        }
        String value = reason.replaceAll("[^a-zA-Z0-9._-]+", "_");
        return value.length() <= 120 ? value : value.substring(0, 120);
    }

    private void handleStoryDialog() {
        if (isInputWorkerThread()) {
            fastClickStoryDialogDirect();
            return;
        }

        inputSequences.submitExclusiveAndWait("dialog:storyClick", this::fastClickStoryDialogDirect);
    }

    private boolean fastClickStoryDialogDirect() {
        if (!TaskSleep.sleep(600 + random.nextInt(100))) return false;
        int[] rect = getDialogRect();
        double scale = coordinateHelper.getScaleRatio();
        int cx = rect[0] + (rect[2] - rect[0]) / 2;
        int cy = rect[3] - (int)Math.round(40 / scale);
        Point safeClick = coordinateHelper.getRandomizedPoint(new Point(cx, cy), 30, 10);
        inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
        return TaskSleep.sleep(600 + random.nextInt(100));
    }


    private DialogOptionClickResult processOptionsWithOCRDetailed(String targetKeyword,
                                                                  boolean allowFallbackOptionClick,
                                                                  DialogDetection detection) {
        int[] rect = detection != null && detection.dialogRect() != null ? detection.dialogRect() : getDialogRect();
        String rawPath = detection == null ? null : detection.rawPath();
        if ((rawPath == null || rawPath.isBlank()) && detection != null && detection.image() != null) {
            rawPath = windowScopedTempPath.resolve("dialog_snapshot_raw.png");
            if (!ImagePreprocessor.saveImage(detection.image(), rawPath)) {
                rawPath = null;
            }
        }
        if (rawPath == null || rawPath.isBlank()) {
            rawPath = windowScopedTempPath.resolve("dialog_active_scan.png");
            boolean captured;
            if (isInputWorkerThread()) {
                BufferedImage image = tracker.captureToMemory("OCR-Scan", rect[0], rect[1], rect[2], rect[3]);
                try {
                    captured = ImagePreprocessor.saveImage(image, rawPath);
                } finally {
                    if (image != null) {
                        image.flush();
                    }
                }
            } else {
                String capturePath = rawPath;
                captured = inputSequences.submitExclusiveAndWait("dialog:ocrCapture", () -> {
                    BufferedImage image = tracker.captureToMemory("OCR-Scan", rect[0], rect[1], rect[2], rect[3]);
                    try {
                        return ImagePreprocessor.saveImage(image, capturePath);
                    } finally {
                        if (image != null) {
                            image.flush();
                        }
                    }
                });
            }
            if (!captured) {
                return DialogOptionClickResult.of(DialogResultStatus.FAILED);
            }
        }

        List<String> aliases = MAP_ALIASES.getOrDefault(targetKeyword, java.util.Collections.singletonList(targetKeyword));
        OcrLineResult scan = gameTextLineOcrService.readDialogOptionWords(
                rawPath,
                targetKeyword,
                aliases,
                Path.of(windowScopedTempPath.resolve("dialog_active_green.png")),
                Path.of(windowScopedTempPath.resolve("dialog_active_yellow.png")));

        for (String alias : aliases) {
            for (OcrWordResult word : scan.words()) {
                if (word.getText().contains(alias)) {
                    log.info("dialog OCR hit alias={} target={} color={} path={}",
                            alias, targetKeyword, scan.variantName(), scan.path());
                    int absoluteX = rect[0] + word.getX();
                    int absoluteY = rect[1] + word.getY();
                    inputSequences.clickLeft("dialog:ocrOption", absoluteX, absoluteY, 150);
                    return DialogOptionClickResult.builder()
                            .result(DialogResultStatus.OPTION_KEYWORD_CLICKED)
                            .relativeX(word.getX())
                            .relativeY(word.getY())
                            .absoluteX(absoluteX)
                            .absoluteY(absoluteY)
                            .matchedText(word.getText())
                            .build();
                }
            }
        }
        if (!allowFallbackOptionClick) {
            log.warn("dialog OCR target not matched and fallback disabled: target={} aliases={} path={} words={}",
                    targetKeyword, aliases, scan.path(), scan.wordsSummary());
            return DialogOptionClickResult.of(DialogResultStatus.OPTION_KEYWORD_NOT_FOUND);
        }
        return DialogOptionClickResult.of(clickGreenOption(rect, "OCR target not matched", false)
                ? DialogResultStatus.FALLBACK_CLICKED
                : DialogResultStatus.OPTION_KEYWORD_NOT_FOUND);
    }

    /**
     * Handle the first matching green option template declared by the request.
     *
     * @param request must use {@link com.bot.dhxy.service.dialog.DialogOptionPolicy#CLICK_GREEN_TEMPLATE}
     *                and carry one or more {@link GreenTemplateClickSpec}; each spec owns its own
     *                click X range and Y radius in screen pixels.
     * @return structured result with the matched spec name in {@code actionKey}.
     */
    private DialogResult handleGreenTemplateOption(DialogHandleRequest request) {
        if (isInputWorkerThread()) {
            return handleGreenTemplateOptionDirect(request);
        }

        AtomicReference<DialogResult> result = new AtomicReference<>(
                DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.NONE));
        boolean completed = inputSequences.submitExclusiveAndWait("dialog:greenTemplateOption:" + request.getSourceTask(), () -> {
            result.set(handleGreenTemplateOptionDirect(request));
            return true;
        });
        return completed ? result.get() : DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.NONE);
    }

    private DialogResult handleGreenTemplateOptionDirect(DialogHandleRequest request) {
        long latencyStart = LatencyMetrics.start();
        DialogResult outcome = DialogResult.simple(DialogResultStatus.GREEN_TEMPLATE_NOT_FOUND, DialogType.NONE);
        try {
            List<GreenTemplateClickSpec> specs = request.getGreenTemplateSpecs();
            if (specs == null || specs.isEmpty()) {
                log.warn("dialog green template requested without specs: reason={}", request.getSourceTask());
                outcome = DialogResult.simple(DialogResultStatus.GREEN_TEMPLATE_NOT_FOUND, DialogType.NONE);
                return outcome;
            }

            DialogType type = DialogType.OPTION;
            if (request.isVerifyDialogType()) {
                type = detectDialogSnapshotDirect("green-template-click:" + request.getSourceTask()).type();
                if (type != DialogType.OPTION) {
                    log.info("dialog green template skipped: reason={} type={}", request.getSourceTask(), type);
                    outcome = DialogResult.simple(DialogResultStatus.GREEN_TEMPLATE_NOT_FOUND, type);
                    return outcome;
                }
            }

            int[] rect = getDialogRect();
            String rawPath = windowScopedTempPath.resolve("dialog_green_multi_raw.png");
            String washedPath = windowScopedTempPath.resolve("dialog_green_multi_washed.png");
            if (!tracker.captureToFile("dialog-green-multi", rawPath, rect[0], rect[1], rect[2], rect[3])) {
                log.warn("dialog green template capture failed: reason={}", request.getSourceTask());
                outcome = DialogResult.simple(DialogResultStatus.FAILED, type);
                return outcome;
            }
            /*
             * 多模板 option 匹配的快路径：dialog 类型、截图、洗绿字都只做一次。
             * 调用方负责按业务优先级传 specs，谁先命中就点谁，避免同一个对话框重复截图。
             */
            ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(rawPath, washedPath);

            for (GreenTemplateClickSpec spec : specs) {
                if (spec == null || spec.templatePath() == null || spec.templatePath().isBlank()) {
                    continue;
                }
                double[] result = ImageFinder.find(washedPath, spec.templatePath(), 0.85);
                if (result == null || result.length < 2) {
                    log.info("dialog green template multi-match miss: reason={} name={} template={}",
                            request.getSourceTask(), spec.name(), spec.templatePath());
                    continue;
                }

                Point optionPoint = coordinateHelper.resolveMatchedPointInRect(rect, result);
                Point safeClick = coordinateHelper.getRandomizedPoint(
                        optionPoint,
                        spec.minOffsetX(),
                        spec.maxOffsetX(),
                        spec.randomRadiusY());
                int offsetX = safeClick.x - optionPoint.x;
                int offsetY = safeClick.y - optionPoint.y;
                log.info("dialog green template multi-match hit: reason={} name={} template={} match=({}, {}) offset=({}, {}) click=({}, {})",
                        request.getSourceTask(), spec.name(), spec.templatePath(), optionPoint.x, optionPoint.y,
                        offsetX, offsetY, safeClick.x, safeClick.y);
                inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
                outcome = DialogResult.statusBuilder(DialogResultStatus.GREEN_TEMPLATE_CLICKED, type)
                        .actionKey(spec.name())
                        .matchedText(spec.templatePath())
                        .relativeX(safeClick.x - rect[0])
                        .relativeY(safeClick.y - rect[1])
                        .absoluteX(safeClick.x)
                        .absoluteY(safeClick.y)
                        .build();
                return outcome;
            }

            log.info("dialog green template multi-match no hit: reason={} candidates={}",
                    request.getSourceTask(), specs.size());
            outcome = DialogResult.simple(DialogResultStatus.GREEN_TEMPLATE_NOT_FOUND, type);
            return outcome;
        } finally {
            LatencyMetrics.info(log, "dialog.greenTemplateFirst", latencyStart,
                    "status=" + outcome.getStatus()
                            + " actionKey=" + (outcome.getActionKey() == null ? "-" : outcome.getActionKey())
                            + " reason=" + request.getSourceTask()
                            + " specCount=" + (request.getGreenTemplateSpecs() == null ? 0 : request.getGreenTemplateSpecs().size()));
        }
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
        if (ImagePreprocessor.saveImage(image, path)) {
            log.info("dialog story objective debug saved: reason={} path={}", reason, path);
        } else {
            log.warn("dialog story objective debug save failed: reason={} path={}", reason, path);
        }
    }

    private boolean clickGreenOption(int[] rect, String reason, boolean first) {
        if (!isInputWorkerThread()) {
            return inputSequences.submitExclusiveAndWait(first ? "dialog:firstGreenOption" : "dialog:lastGreenOption",
                    () -> clickGreenOption(rect, reason, first));
        }
        String elementName = first ? "dialog-first-green-option" : "dialog-last-green-option";
        BufferedImage frame = tracker.captureToMemory(elementName, rect[0], rect[1], rect[2], rect[3]);
        if (frame == null) {
            log.warn("dialog green option capture failed: reason={} first={}", reason, first);
            return false;
        }
        try {
            List<ImagePreprocessor.GreenTextBand> bands = ImagePreprocessor.findGreenTextBands(frame);
            ImagePreprocessor.GreenTextBand band = ImagePreprocessor.pickGreenTextBand(bands, first);
            if (band == null) {
                log.warn("dialog green option not found: reason={} first={}", reason, first);
                return false;
            }
            int clickX = rect[0] + (band.minX() + band.maxX()) / 2;
            int clickY = rect[1] + (band.minY() + band.maxY()) / 2;
            Point safeClick = coordinateHelper.getRandomizedPoint(new Point(clickX, clickY), 12, 3);
            log.info("dialog click green option: reason={} first={} band={} click=({}, {})",
                    reason, first, band, safeClick.x, safeClick.y);
            inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
            return true;
        } finally {
            frame.flush();
        }
    }

    private int[] getDialogRect() {
        return coordinateHelper.getScaledRect(DIALOG_LARGE_X, DIALOG_LARGE_Y, DIALOG_LARGE_W, DIALOG_LARGE_H);
    }

    private int[] getSmallDialogRect() {
        return coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, DIALOG_SMALL_H);
    }

}
