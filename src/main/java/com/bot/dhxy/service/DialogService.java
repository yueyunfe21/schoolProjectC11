package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.dialog.DialogDetection;
import com.bot.dhxy.model.dialog.DialogFingerprintWashMode;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import com.bot.dhxy.model.ocr.OcrLineResult;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.service.dialog.DialogOptionClickResult;
import com.bot.dhxy.service.dialog.DialogStoryPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.vision.GameTextLineOcrService;
import com.bot.dhxy.vision.ObjectiveTextRecognitionService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Comparator;
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
    private final WindowTaskContextHolder windowTaskContextHolder;
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
    private static final int HIDE_PLAYER_NAMES_SETTLE_MS = 220;

    private static final String OPTION_GIVE_TEXT = "images/template/dialog/maintenance/dialog_opt_give.png";
    private static final String HEAL_PET_OPTION_TEXT = "images/template/dialog/maintenance/heal_pet_option.png";
    private static final String REPAIR_EQUIPMENT_OPTION_TEXT = "images/template/dialog/maintenance/repair_equipment_option.png";
    private static final String REPAIR_EQUIPMENT_GIVEUP_OPTION_TEXT = "images/template/dialog/maintenance/repair_equipment_option_giveup.png";
    private static final double BUSINESS_OPTION_MATCH_RATE = 0.70;
    private static final int AUTO_BATTLE_HEAL_OPTION_LEFT = 258;
    private static final int AUTO_BATTLE_HEAL_OPTION_TOP = 352;
    private static final int AUTO_BATTLE_HEAL_OPTION_RIGHT = 390;
    private static final int AUTO_BATTLE_HEAL_OPTION_BOTTOM = 425;
    private static final int AUTO_BATTLE_REPAIR_OPTION_LEFT = 250;
    private static final int AUTO_BATTLE_REPAIR_OPTION_TOP = 360;
    private static final int AUTO_BATTLE_REPAIR_OPTION_RIGHT = 360;
    private static final int AUTO_BATTLE_REPAIR_OPTION_BOTTOM = 424;
    private static final int ROUTE_TRANSFER_DIALOG_ATTEMPTS = 2;
    private static final long ROUTE_TRANSFER_RETRY_DELAY_MS = 650L;
    private static final long PREPARED_ROUTE_CLICK_MAX_AGE_MS = 2_500L;

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

        if (isMaintenanceBroadcastOptionRequest(request)) {
            return finishRequest(request, handleMaintenanceBroadcastOptionFastPath(request));
        }

        // Stage 1: optional opening click, used by tasks that first need to poke an NPC or object.
        if (request.getInitialClick() != null) {
            Point p = request.getInitialClick();
            log.info("dialog request initial click: ({},{})", p.x, p.y);
            inputSequences.clickLeft("dialog:requestInitialClick", p.x, p.y, 150);
            if (!TaskSleep.sleep(600 + random.nextInt(200))) {
                return finishRequest(request, DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.NONE));
            }
        }

        if (request.getOperation() == com.bot.dhxy.service.dialog.DialogOperation.WUHUAN_SHOE_SHOP_BUY_OPTION) {
            return finishRequest(request, handleWuhuanShoeShopBuyOption(request));
        }

        if (request.getOptionPolicy() == com.bot.dhxy.service.dialog.DialogOptionPolicy.CLICK_GREEN_TEMPLATE) {
            return finishRequest(request, handleGreenTemplateOption(request));
        }

        // Stage 2: classify once. A successful screenshot is not proof of a dialog:
        // when type is NONE, the fixed dialog area may still contain route text or labels.
        DialogDetection detection = detectDialogSnapshotDirect("handle-dialog:" + request.getOperation());
        DialogType type = detection.type();
        if (type == DialogType.NONE) {
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
            case CLICK_BUSINESS_OPTION -> handleBusinessOption(request.isIncludeCleanupBusinessOptions(), detection);
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

    private boolean isMaintenanceBroadcastOptionRequest(DialogHandleRequest request) {
        return request.getOptionPolicy() == com.bot.dhxy.service.dialog.DialogOptionPolicy.CLICK_BUSINESS_OPTION
                && !request.isIncludeCleanupBusinessOptions();
    }

    private DialogResult handleMaintenanceBroadcastOptionFastPath(DialogHandleRequest request) {
        DialogResult healPet = tryClickMaintenanceBroadcastOption(
                request,
                "heal-pet",
                HEAL_PET_OPTION_TEXT,
                AUTO_BATTLE_HEAL_OPTION_LEFT,
                AUTO_BATTLE_HEAL_OPTION_TOP,
                AUTO_BATTLE_HEAL_OPTION_RIGHT,
                AUTO_BATTLE_HEAL_OPTION_BOTTOM);
        if (healPet.getStatus() == DialogResultStatus.BUSINESS_OPTION_CLICKED
                || healPet.getStatus() == DialogResultStatus.INTERRUPTED
                || healPet.getStatus() == DialogResultStatus.FAILED) {
            return healPet;
        }
        DialogResult repairEquipment = tryClickMaintenanceBroadcastOption(
                request,
                "repair-equipment",
                REPAIR_EQUIPMENT_OPTION_TEXT,
                AUTO_BATTLE_REPAIR_OPTION_LEFT,
                AUTO_BATTLE_REPAIR_OPTION_TOP,
                AUTO_BATTLE_REPAIR_OPTION_RIGHT,
                AUTO_BATTLE_REPAIR_OPTION_BOTTOM);
        if (repairEquipment.getStatus() == DialogResultStatus.BUSINESS_OPTION_CLICKED
                || repairEquipment.getStatus() == DialogResultStatus.INTERRUPTED
                || repairEquipment.getStatus() == DialogResultStatus.FAILED) {
            return repairEquipment;
        }

        /*
         * The fixed-strip path is intentionally cheap for auto-battle maintenance polling, but the
         * formal task path must not give up when the option is slightly shifted. Fall back once to
         * the normal dialog business-option scan so 沙拉买提/修理 dialogs that were already verified
         * by NpcClickService can still be clicked.
         */
        DialogDetection detection = detectDialogSnapshotDirect(
                "maintenance-broadcast-fallback:" + request.getSourceTask());
        try {
            if (detection.type() == DialogType.NONE) {
                log.info("maintenance broadcast fallback skipped: source={} reason=no-dialog firstStatus={} secondStatus={}",
                        request.getSourceTask(), healPet.getStatus(), repairEquipment.getStatus());
                return repairEquipment;
            }
            DialogResult fallback = handleBusinessOption(false, detection);
            log.info("maintenance broadcast fallback result: source={} firstStatus={} secondStatus={} fallbackStatus={} actionKey={}",
                    request.getSourceTask(), healPet.getStatus(), repairEquipment.getStatus(),
                    fallback.getStatus(), fallback.getActionKey());
            return fallback;
        } finally {
            if (detection.image() != null) {
                detection.image().flush();
            }
        }
    }

    private DialogResult tryClickMaintenanceBroadcastOption(DialogHandleRequest request,
                                                            String optionName,
                                                            String templatePath,
                                                            int relativeLeft,
                                                            int relativeTop,
                                                            int relativeRight,
                                                            int relativeBottom) {
        tracker.refreshWindowState();
        int left = tracker.getWindowBaseX() + relativeLeft;
        int top = tracker.getWindowBaseY() + relativeTop;
        int right = tracker.getWindowBaseX() + relativeRight;
        int bottom = tracker.getWindowBaseY() + relativeBottom;
        int[] rect = new int[]{left, top, right, bottom};
        String safeOption = safeDebugName("maintenance-broadcast-" + optionName);
        String rawPath = windowScopedTempPath.resolve(safeOption + "_raw.png");
        String washedPath = windowScopedTempPath.resolve(safeOption + "_washed.png");
        if (!tracker.captureToFile("dialog:maintenance-broadcast:" + optionName,
                rawPath, left, top, right, bottom)) {
            log.warn("maintenance broadcast option capture failed: source={} option={} rect={}",
                    request.getSourceTask(), optionName, ImagePreprocessor.rectToString(rect));
            return DialogResult.simple(DialogResultStatus.FAILED, DialogType.NONE);
        }

        ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath);
        double[] result = ImageFinder.find(washedPath, templatePath, BUSINESS_OPTION_MATCH_RATE);
        if (result == null || result.length < 2) {
            log.debug("maintenance broadcast option not matched: source={} option={} template={} rect={} raw={} washed={}",
                    request.getSourceTask(), optionName, templatePath, ImagePreprocessor.rectToString(rect),
                    rawPath, washedPath);
            return DialogResult.simple(DialogResultStatus.BUSINESS_OPTION_NOT_FOUND, DialogType.NONE);
        }

        Point matched = coordinateHelper.resolveMatchedPointInRect(rect, result);
        Point safeClick = coordinateHelper.getRandomizedPoint(matched, 4, 3);
        log.info("maintenance broadcast option matched: source={} option={} score={} rect={} click=({}, {}) raw={} washed={}",
                request.getSourceTask(), optionName, result.length > 2 ? result[2] : 0.0,
                ImagePreprocessor.rectToString(rect), safeClick.x, safeClick.y, rawPath, washedPath);
        if (isInputWorkerThread()) {
            inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
        } else {
            inputSequences.clickLeft("dialog:maintenanceBroadcast:" + optionName, safeClick.x, safeClick.y, 150);
        }
        if (!TaskSleep.sleep(800 + random.nextInt(300))) {
            return DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.NONE);
        }
        return DialogResult.statusBuilder(DialogResultStatus.BUSINESS_OPTION_CLICKED, DialogType.OPTION)
                .actionKey(optionName)
                .matchedText(templatePath)
                .relativeX(safeClick.x - rect[0])
                .relativeY(safeClick.y - rect[1])
                .absoluteX(safeClick.x)
                .absoluteY(safeClick.y)
                .build();
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
            if (detection == null || detection.image() == null || detection.type() == DialogType.NONE) {
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
                        request,
                        allowFallback,
                        true,
                        true,
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
    private DialogResult handleBusinessOption(boolean includeCleanupOptions, DialogDetection detection) {
        DialogType type = detection.type();
        int[] rect = detection.dialogRect() != null ? detection.dialogRect() : getDialogRect();
        String rawPath = detection.rawPath();
        if ((rawPath == null || rawPath.isBlank()) && detection.image() != null) {
            rawPath = windowScopedTempPath.resolve("business_dialog_raw.png");
            if (!ImagePreprocessor.saveImage(detection.image(), rawPath)) {
                rawPath = null;
            }
        }
        String washedPath = windowScopedTempPath.resolve("business_dialog_washed.png");

        /*
         * This branch stays behind handleDialog as the business-option route. Do not recapture it
         * through an exclusive input callback: idle auto-battle windows poll this path frequently,
         * and exclusive capture focuses the game window even when no maintenance option exists.
         */
        if (rawPath == null || rawPath.isBlank()) {
            log.warn("business dialog capture unavailable");
            return DialogResult.simple(DialogResultStatus.FAILED, type);
        }

        ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath);
        DialogResult result = tryClickBusinessOptionInWashedImage(
                washedPath, rect, HEAL_PET_OPTION_TEXT, "heal-pet", type);
        if (result != null) {
            return result;
        }
        result = tryClickBusinessOptionInWashedImage(
                washedPath, rect, REPAIR_EQUIPMENT_OPTION_TEXT, "repair-equipment", type);
        if (result != null) {
            return result;
        }
        if (includeCleanupOptions
                && (result = tryClickBusinessOptionInWashedImage(
                washedPath, rect, REPAIR_EQUIPMENT_GIVEUP_OPTION_TEXT, "repair-equipment-giveup", type)) != null) {
            return result;
        }

        log.info("business dialog option not matched");
        return DialogResult.simple(DialogResultStatus.BUSINESS_OPTION_NOT_FOUND, type);
    }

    /**
     * 五环买鞋的“我想买点东西”只在五环 request 下检查，避免普通清窗/维护扫描误点。
     *
     * <p>先走绿字模板快路径；模板没命中时才复用 option OCR 找“买点”。任务侧只调用
     * {@link #handleDialog(DialogHandleRequest)}，不再自己维护一套买鞋对话框识别。</p>
     */
    private DialogResult handleWuhuanShoeShopBuyOption(DialogHandleRequest request) {
        DialogResult templateResult = handleGreenTemplateOption(request);
        if (templateResult.getStatus() == DialogResultStatus.GREEN_TEMPLATE_CLICKED
                || templateResult.getStatus() == DialogResultStatus.INTERRUPTED
                || templateResult.getStatus() == DialogResultStatus.FAILED) {
            return templateResult;
        }

        DialogDetection detection = detectDialogSnapshotDirect(
                "wuhuan-shoe-shop-buy-option-ocr:" + request.getSourceTask());
        if (detection.type() == DialogType.NONE) {
            log.warn("wuhuan shoe-shop buy option OCR skipped: source={} reason=no-dialog templateStatus={}",
                    request.getSourceTask(), templateResult.getStatus());
            return DialogResult.simple(DialogResultStatus.NO_DIALOG, DialogType.NONE);
        }

        DialogResult ocrResult = handleKeywordOption(request, detection);
        log.info("wuhuan shoe-shop buy option fallback result: source={} templateStatus={} ocrStatus={} matchedText={} click=({}, {})",
                request.getSourceTask(), templateResult.getStatus(), ocrResult.getStatus(),
                ocrResult.getMatchedText(), ocrResult.getAbsoluteX(), ocrResult.getAbsoluteY());
        return ocrResult;
    }

    /**
     * Check whether the current bound window appears to have a team maintenance broadcast dialog.
     *
     * <p>This is a no-focus, no-click prefilter for follower windows before they compete for the
     * shared task turn. The full {@link #handleDialog(DialogHandleRequest)} path still rechecks and
     * clicks under the turn after this method returns true.</p>
     *
     * @param sourceTask diagnostic source for debug image names and logs.
     * @return matched maintenance action key such as {@code heal-pet} or {@code repair-equipment};
     *         null when no known maintenance broadcast option is visible.
     */
    public String detectMaintenanceBroadcastActionNoFocus(String sourceTask) {
        DialogDetection detection = captureDialogSnapshot("maintenance-broadcast-prefilter:" + sourceTask);
        try {
            if (detection.image() == null || !hasDialogMask(detection)
                    || !hasOptionInLowerHalf(detection, "maintenance-broadcast-prefilter:" + sourceTask)) {
                log.debug("maintenance broadcast prefilter miss: source={}", sourceTask);
                return null;
            }

            String rawPath = detection.rawPath();
            if (rawPath == null || rawPath.isBlank()) {
                log.debug("maintenance broadcast prefilter miss: source={} reason=no-raw-path", sourceTask);
                return null;
            }
            String washedPath = windowScopedTempPath.resolve("maintenance_broadcast_prefilter_washed.png");
            ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath);
            String actionKey = null;
            if (ImageFinder.find(washedPath, HEAL_PET_OPTION_TEXT, BUSINESS_OPTION_MATCH_RATE) != null) {
                actionKey = "heal-pet";
            } else if (ImageFinder.find(washedPath, REPAIR_EQUIPMENT_OPTION_TEXT, BUSINESS_OPTION_MATCH_RATE) != null) {
                actionKey = "repair-equipment";
            }
            log.info("maintenance broadcast prefilter: source={} matched={} actionKey={} raw={} washed={}",
                    sourceTask, actionKey != null, actionKey, rawPath, washedPath);
            return actionKey;
        } finally {
            if (detection.image() != null) {
                detection.image().flush();
            }
        }
    }

    private DialogResult tryClickBusinessOptionInWashedImage(String washedPath,
                                                             int[] rect,
                                                             String templatePath,
                                                             String optionName,
                                                             DialogType type) {
        double[] result = ImageFinder.find(washedPath, templatePath, BUSINESS_OPTION_MATCH_RATE);
        if (result == null || result.length < 2) {
            log.info("business dialog option not matched: option={} template={}", optionName, templatePath);
            return null;
        }

        Point optionPoint = coordinateHelper.resolveMatchedPointInRect(rect, result);
        Point safeClick = coordinateHelper.getRandomizedPoint(optionPoint, 4, 3);
        log.info("business dialog option matched: option={} score={} click=({}, {})",
                optionName, result.length > 2 ? result[2] : 0.0, safeClick.x, safeClick.y);
        if (isInputWorkerThread()) {
            inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
        } else {
            inputSequences.clickLeft("dialog:businessOption:" + optionName, safeClick.x, safeClick.y, 150);
        }
        if (!TaskSleep.sleep(800 + random.nextInt(300))) {
            return DialogResult.simple(DialogResultStatus.INTERRUPTED, type);
        }
        return DialogResult.statusBuilder(DialogResultStatus.BUSINESS_OPTION_CLICKED, type)
                .actionKey(optionName)
                .matchedText(templatePath)
                .relativeX(safeClick.x - rect[0])
                .relativeY(safeClick.y - rect[1])
                .absoluteX(safeClick.x)
                .absoluteY(safeClick.y)
                .build();
    }

    private DialogResult handleKeywordOption(DialogHandleRequest request, DialogDetection detection) {
        if (request.getTargetKeyword() == null) {
            log.warn("dialog keyword option requested without targetKeyword");
            return DialogResult.simple(DialogResultStatus.OPTION_KEYWORD_NOT_FOUND, detection.type());
        }
        DialogOptionClickResult clickResult = processOptionsWithOCRDetailed(
                request,
                request.isAllowFallbackOptionClick(),
                false,
                true,
                detection);
        return fromOptionClickResult(clickResult, detection.type(), request.getTargetKeyword());
    }

    /**
     * Prepare a route-transfer option click without sending input.
     *
     * @param source diagnostic source for logs.
     * @param targetKeyword destination text to match in the option dialog.
     * @return prepared action when the target option is visible and matched.
     */
    public Optional<PreparedDialogAction> prepareRouteKeywordOption(String source, String targetKeyword) {
        if (targetKeyword == null || targetKeyword.isBlank()) {
            return Optional.empty();
        }
        long startedAt = System.currentTimeMillis();
        /*
         * Background preparation only observes an already-open route dialog. Alt+4 is useful for
         * NPC/player-name-heavy scene scans, but it would turn this watcher path into real input and
         * can steal the global input queue from active task navigation.
         */
        long detectStartedAt = System.currentTimeMillis();
        DialogDetection detection = detectDialogSnapshotDirect("prepare-route:" + source, false);
        long detectElapsedMs = Math.max(0L, System.currentTimeMillis() - detectStartedAt);
        if (detection == null || detection.type() != DialogType.OPTION || detection.image() == null) {
            log.info("dialog prepare route miss: source={} target={} type={} hasImage={} detectMs={} totalMs={}",
                    source, targetKeyword,
                    detection == null ? null : detection.type(),
                    detection != null && detection.image() != null,
                    detectElapsedMs,
                    Math.max(0L, System.currentTimeMillis() - startedAt));
            return Optional.empty();
        }
        DialogHandleRequest request = DialogHandleRequest.handleRouteKeywordOption(
                source, targetKeyword, false);
        long ocrStartedAt = System.currentTimeMillis();
        DialogOptionClickResult result = processOptionsWithOCRDetailed(
                request,
                false,
                false,
                false,
                detection);
        long ocrElapsedMs = Math.max(0L, System.currentTimeMillis() - ocrStartedAt);
        log.info("dialog prepare route result: source={} target={} status={} matched={} click=({}, {}) detectMs={} ocrMs={} totalMs={}",
                source, targetKeyword, result.getResult(), result.getMatchedText(),
                result.getAbsoluteX(), result.getAbsoluteY(),
                detectElapsedMs, ocrElapsedMs,
                Math.max(0L, System.currentTimeMillis() - startedAt));
        return Optional.ofNullable(result.getPreparedAction());
    }

    public Optional<PreparedDialogAction> prepareRememberedRouteOption(String source,
                                                                       String targetKeyword,
                                                                       int relativeX,
                                                                       int relativeY,
                                                                       String optionText) {
        long startedAt = System.currentTimeMillis();
        DialogDetection detection = detectDialogSnapshotDirect("prepare-route-memory:" + source, false);
        if (detection == null || detection.type() != DialogType.OPTION || detection.image() == null
                || detection.dialogRect() == null) {
            log.info("dialog prepare remembered route miss: source={} target={} type={} hasImage={} totalMs={}",
                    source, targetKeyword,
                    detection == null ? null : detection.type(),
                    detection != null && detection.image() != null,
                    Math.max(0L, System.currentTimeMillis() - startedAt));
            return Optional.empty();
        }
        PreparedDialogAction action = buildRememberedPreparedDialogAction(
                source, targetKeyword, relativeX, relativeY, optionText, detection);
        log.info("dialog prepare remembered route result: source={} target={} matched={} rel=({}, {}) prepared={} totalMs={}",
                source, targetKeyword, optionText, relativeX, relativeY, action != null,
                Math.max(0L, System.currentTimeMillis() - startedAt));
        return Optional.ofNullable(action);
    }

    /**
     * Capture a small screen-absolute validation crop for an already prepared dialog action.
     *
     * @param reason diagnostic source for capture logs.
     * @param left screen-absolute left.
     * @param top screen-absolute top.
     * @param right screen-absolute right.
     * @param bottom screen-absolute bottom.
     * @return in-memory crop owned by the caller, or null when capture fails.
     */
    public BufferedImage captureDialogValidationImage(String reason, int left, int top, int right, int bottom) {
        if (right <= left || bottom <= top) {
            return null;
        }
        return tracker.captureToMemory(reason, left, top, right, bottom);
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

        DialogResult preparedResult = tryConsumePreparedRememberedRouteOption(request, detection);
        if (preparedResult != null) {
            return preparedResult;
        }

        int[] rect = detection.dialogRect() != null ? detection.dialogRect() : getDialogRect();
        int x = rect[0] + relativeX;
        int y = rect[1] + relativeY;
        Point safeClick = coordinateHelper.getRandomizedPoint(new Point(x, y), 4, 3);
        log.info("dialog remembered option click: source={} target={} rel=({}, {}) click=({}, {})",
                request.getSourceTask(), request.getTargetKeyword(), relativeX, relativeY, safeClick.x, safeClick.y);
        boolean clicked;
        if (isInputWorkerThread()) {
            if (InputActionScope.isCancelled()) {
                return DialogResult.simple(DialogResultStatus.INTERRUPTED, detection.type());
            }
            inputProvider.clickLeft(safeClick.x, safeClick.y, 150);
            clicked = true;
        } else {
            clicked = inputSequences.moveAndClickLeft("dialog:rememberedOption:" + request.getTargetKeyword(),
                    safeClick.x, safeClick.y, 80, 150);
        }
        if (!clicked) {
            return DialogResult.simple(DialogResultStatus.FAILED, detection.type());
        }
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

    private DialogResult tryConsumePreparedRememberedRouteOption(DialogHandleRequest request, DialogDetection detection) {
        if (request.getOperation() != DialogOperation.CLICK_REMEMBERED_OPTION
                || request.getTargetKeyword() == null
                || request.getTargetKeyword().isBlank()) {
            return null;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return null;
        }
        PreparedDialogAction action = runtime.getPreparedDialogAction();
        long now = System.currentTimeMillis();
        if (action == null
                || !action.matches(DialogOperation.ROUTE_TRANSFER, request.getTargetKeyword())
                || !matchesCurrentPreparedDialogBinding(runtime, action)
                || !action.verifiedWithin(now, PREPARED_ROUTE_CLICK_MAX_AGE_MS)) {
            return null;
        }

        log.info("dialog remembered option uses prepared action: source={} target={} matched={} click=({}, {}) verifiedAgeMs={}",
                request.getSourceTask(), request.getTargetKeyword(), action.getMatchedText(),
                action.getAbsoluteX(), action.getAbsoluteY(), Math.max(0L, now - action.getLastVerifiedAtMs()));
        boolean clicked;
        if (isInputWorkerThread()) {
            if (InputActionScope.isCancelled()) {
                return DialogResult.simple(DialogResultStatus.INTERRUPTED, detection.type());
            }
            inputProvider.clickLeft(action.getAbsoluteX(), action.getAbsoluteY(), 150);
            clicked = true;
        } else {
            clicked = inputSequences.moveAndClickLeft("dialog:rememberedPreparedRoute:" + request.getTargetKeyword(),
                    action.getAbsoluteX(), action.getAbsoluteY(), 80, 150);
        }
        if (!clicked) {
            return DialogResult.simple(DialogResultStatus.FAILED, detection.type());
        }
        runtime.clearDialogPreparationRequest("dialog remembered option consumed prepared action");
        if (!TaskSleep.sleep(500 + random.nextInt(150))) {
            return DialogResult.simple(DialogResultStatus.INTERRUPTED, detection.type());
        }
        return DialogResult.statusBuilder(DialogResultStatus.OPTION_KEYWORD_CLICKED, detection.type())
                .actionKey(request.getTargetKeyword())
                .matchedText(action.getMatchedText())
                .relativeX(action.getRelativeX())
                .relativeY(action.getRelativeY())
                .absoluteX(action.getAbsoluteX())
                .absoluteY(action.getAbsoluteY())
                .preparedAction(action)
                .build();
    }

    private boolean matchesCurrentPreparedDialogBinding(WindowRuntimeContext runtime, PreparedDialogAction action) {
        if (runtime == null || action == null) {
            return false;
        }
        if (action.getWindowId() != null && !action.getWindowId().equals(runtime.getWindowId())) {
            return false;
        }
        String currentHwnd = runtime.getNativeBinding().getNativeHandle();
        return action.getHwnd() == null || action.getHwnd().equals(currentHwnd);
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
                .preparedAction(clickResult.getPreparedAction())
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
    public DialogType detectDialogTypeNoFocus(String reason) {
        return detectDialogSnapshotDirect(reason).type();
    }

    /**
     * Detect the current dialog type without requesting focus, optionally avoiding real input.
     *
     * @param reason short diagnostic label for logs and screenshots.
     * @param hidePlayerNames whether to send Alt+4 before capture; watcher/background probes should
     *                        normally pass false so they do not occupy the serialized input queue.
     * @return detected dialog type; {@link DialogType#NONE} means no known dialog frame matched.
     */
    public DialogType detectDialogTypeNoFocus(String reason, boolean hidePlayerNames) {
        return detectDialogSnapshotDirect(reason, hidePlayerNames).type();
    }

    private DialogDetection detectDialogSnapshotDirect(String reason) {
        return detectDialogSnapshotDirect(reason, true);
    }

    private DialogDetection detectDialogSnapshotDirect(String reason, boolean hidePlayerNames) {
        long latencyStart = LatencyMetrics.start();
        DialogDetection detection = DialogDetection.none();
        try {
            if (!TaskSleep.sleep(700 + random.nextInt(100))) {
                return detection;
            }
            detection = captureDialogSnapshot(reason, hidePlayerNames);
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
        return captureDialogSnapshot(reason, true);
    }

    private DialogDetection captureDialogSnapshot(String reason, boolean hidePlayerNames) {
        if (hidePlayerNames) {
            hidePlayerNamesBeforeDialogCapture(reason);
        }
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

    /**
     * Hide other-player name overlays before dialog color checks.
     *
     * <p>Dialog detection deliberately samples the fixed dialog area before deciding whether an
     * actual dialog exists. In crowded scenes that area can contain bright green player labels, which
     * look exactly like option text after washing. Alt+4 hides those player labels without changing
     * bag, map, battle, or quest-panel screenshots because this hook is scoped only to dialog
     * captures.</p>
     *
     * @param reason diagnostic label for logs.
     */
    private void hidePlayerNamesBeforeDialogCapture(String reason) {
        if (isInputWorkerThread()) {
            inputProvider.pressAlt4();
            TaskSleep.sleep(HIDE_PLAYER_NAMES_SETTLE_MS);
            return;
        }
        boolean ok = inputSequences.submitAndWait("dialog:hidePlayerNames:" + safeDebugName(reason), List.of(
                InputAction.pressAlt4(),
                InputAction.sleep(HIDE_PLAYER_NAMES_SETTLE_MS)
        ));
        if (!ok) {
            log.warn("dialog hide player names shortcut did not complete: reason={}", reason);
        }
    }

    private boolean hasOptionInLowerHalf(DialogDetection detection, String reason) {
        int[] area = coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y + CROP_TOP_Y, DIALOG_SMALL_W, DIALOG_SMALL_H - CROP_TOP_Y);
        BufferedImage frame = ImagePreprocessor.cropAbsoluteRect(detection.image(), detection.dialogRect(), area);
        if (frame == null) return false;

        String debugPrefix = "dialog_detect_" + safeDebugName(reason) + "_option_lower";
        String optionRawPath = windowScopedTempPath.resolve(debugPrefix + "_raw.png");
        String optionGreenPath = windowScopedTempPath.resolve(debugPrefix + "_green.png");
        ImagePreprocessor.saveImage(frame, optionRawPath);
        int greenCount = ImagePreprocessor.countGreenPixelsHSV(frame, optionGreenPath);
        frame.flush();
        // Player/NPC labels behind the dialog crop can be yellow; bright green option text is the
        // stable proof that this is really an option dialog.
        boolean option = greenCount > 150;
        if (option) {
            log.info("dialog option lower check: reason={} rect={} raw={} green={} greenPath={} result={}",
                    reason, ImagePreprocessor.rectToString(area), optionRawPath, greenCount,
                    optionGreenPath, true);
        } else {
            log.debug("dialog option lower check: reason={} rect={} raw={} green={} greenPath={} result={}",
                    reason, ImagePreprocessor.rectToString(area), optionRawPath, greenCount,
                    optionGreenPath, false);
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


    private DialogOptionClickResult processOptionsWithOCRDetailed(DialogHandleRequest request,
                                                                  boolean allowFallbackOptionClick,
                                                                  boolean preferOcrFallbackOption,
                                                                  boolean clickMatchedOption,
                                                                  DialogDetection detection) {
        String targetKeyword = request.getTargetKeyword();
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
                    PreparedDialogAction preparedAction = buildPreparedDialogAction(
                            request, detection, word, scan, absoluteX, absoluteY);
                    if (clickMatchedOption) {
                        inputSequences.clickLeft("dialog:ocrOption", absoluteX, absoluteY, 150);
                    }
                    return DialogOptionClickResult.builder()
                            .result(DialogResultStatus.OPTION_KEYWORD_CLICKED)
                            .relativeX(word.getX())
                            .relativeY(word.getY())
                            .absoluteX(absoluteX)
                            .absoluteY(absoluteY)
                            .matchedText(word.getText())
                            .preparedAction(preparedAction)
                            .build();
                }
            }
        }
        if (!allowFallbackOptionClick) {
            log.warn("dialog OCR target not matched and fallback disabled: target={} aliases={} path={} words={}",
                    targetKeyword, aliases, scan.path(), scan.wordsSummary());
            return DialogOptionClickResult.of(DialogResultStatus.OPTION_KEYWORD_NOT_FOUND);
        }
        if (preferOcrFallbackOption) {
            Optional<OcrWordResult> fallbackWord = selectOcrFallbackOption(scan.words());
            if (fallbackWord.isPresent()) {
                OcrWordResult word = fallbackWord.get();
                int absoluteX = rect[0] + word.getX();
                int absoluteY = rect[1] + word.getY();
                log.info("dialog OCR fallback option click: target={} color={} text={} path={} click=({}, {}) words={}",
                        targetKeyword, scan.variantName(), word.getText(), scan.path(), absoluteX, absoluteY,
                        scan.wordsSummary());
                inputSequences.clickLeft("dialog:ocrFallbackOption", absoluteX, absoluteY, 150);
                return DialogOptionClickResult.builder()
                        .result(DialogResultStatus.FALLBACK_CLICKED)
                        .relativeX(word.getX())
                        .relativeY(word.getY())
                        .absoluteX(absoluteX)
                        .absoluteY(absoluteY)
                        .matchedText(word.getText())
                        .build();
            }
        }
        return DialogOptionClickResult.of(clickGreenOption(rect, "OCR target not matched", false)
                ? DialogResultStatus.FALLBACK_CLICKED
                : DialogResultStatus.OPTION_KEYWORD_NOT_FOUND);
    }

    private PreparedDialogAction buildPreparedDialogAction(DialogHandleRequest request,
                                                           DialogDetection detection,
                                                           OcrWordResult word,
                                                           OcrLineResult scan,
                                                           int absoluteX,
                                                           int absoluteY) {
        if (request == null || detection == null || detection.dialogRect() == null
                || word == null || scan == null || scan.path() == null || scan.path().isBlank()) {
            return null;
        }
        BufferedImage washed = ImagePreprocessor.pathToBufferedImage(scan.path());
        if (washed == null) {
            return null;
        }
        try {
            int[] localRect = buildValidationLocalRect(word, washed.getWidth(), washed.getHeight());
            if (localRect == null) {
                return null;
            }
            BufferedImage crop = ImagePreprocessor.cropCopy(
                    washed,
                    localRect[0],
                    localRect[1],
                    localRect[2] - localRect[0],
                    localRect[3] - localRect[1]);
            if (crop == null) {
                return null;
            }
            try {
                String fingerprint = ImagePreprocessor.buildBinaryFingerprint(crop);
                if (fingerprint.isBlank()) {
                    return null;
                }
                int[] dialogRect = detection.dialogRect();
                long now = System.currentTimeMillis();
                return PreparedDialogAction.builder()
                        .dialogType(detection.type())
                        .operation(request.getOperation())
                        .targetKeyword(request.getTargetKeyword())
                        .matchedText(word.getText())
                        .relativeX(word.getX())
                        .relativeY(word.getY())
                        .absoluteX(absoluteX)
                        .absoluteY(absoluteY)
                        .validationLeft(dialogRect[0] + localRect[0])
                        .validationTop(dialogRect[1] + localRect[1])
                        .validationRight(dialogRect[0] + localRect[2])
                        .validationBottom(dialogRect[1] + localRect[3])
                        .washMode(resolveFingerprintWashMode(scan.variantName()))
                        .fingerprint(fingerprint)
                        .preparedAtMs(now)
                        .lastVerifiedAtMs(now)
                        .source(request.getSourceTask())
                        .debugImagePath(scan.path())
                        .build();
            } finally {
                crop.flush();
            }
        } finally {
            washed.flush();
        }
    }

    private int[] buildValidationLocalRect(OcrWordResult word, int imageWidth, int imageHeight) {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return null;
        }
        int textWidth = word.getWidth() > 0 ? word.getWidth() : 24;
        int textHeight = word.getHeight() > 0 ? word.getHeight() : 18;
        int left = word.getWidth() > 0 ? word.getLeft() : word.getX() - textWidth / 2;
        int top = word.getHeight() > 0 ? word.getTop() : word.getY() - textHeight / 2;
        left = Math.max(0, left - 4);
        top = Math.max(0, top - 4);
        int right = Math.min(imageWidth, left + textWidth + 8);
        int bottom = Math.min(imageHeight, top + textHeight + 8);
        return right > left && bottom > top ? new int[]{left, top, right, bottom} : null;
    }

    private PreparedDialogAction buildRememberedPreparedDialogAction(String source,
                                                                     String targetKeyword,
                                                                     int relativeX,
                                                                     int relativeY,
                                                                     String optionText,
                                                                     DialogDetection detection) {
        int[] dialogRect = detection.dialogRect();
        BufferedImage raw = detection.image();
        BufferedImage washed = ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(raw);
        if (washed == null) {
            return null;
        }
        try {
            int[] localRect = buildRememberedValidationLocalRect(relativeX, relativeY,
                    washed.getWidth(), washed.getHeight());
            if (localRect == null) {
                return null;
            }
            BufferedImage crop = ImagePreprocessor.cropCopy(
                    washed,
                    localRect[0],
                    localRect[1],
                    localRect[2] - localRect[0],
                    localRect[3] - localRect[1]);
            if (crop == null) {
                return null;
            }
            try {
                String fingerprint = ImagePreprocessor.buildBinaryFingerprint(crop);
                if (fingerprint.isBlank()) {
                    return null;
                }
                long now = System.currentTimeMillis();
                return PreparedDialogAction.builder()
                        .dialogType(detection.type())
                        .operation(DialogOperation.ROUTE_TRANSFER)
                        .targetKeyword(targetKeyword)
                        .matchedText(optionText == null || optionText.isBlank() ? "remembered-option" : optionText)
                        .relativeX(relativeX)
                        .relativeY(relativeY)
                        .absoluteX(dialogRect[0] + relativeX)
                        .absoluteY(dialogRect[1] + relativeY)
                        .validationLeft(dialogRect[0] + localRect[0])
                        .validationTop(dialogRect[1] + localRect[1])
                        .validationRight(dialogRect[0] + localRect[2])
                        .validationBottom(dialogRect[1] + localRect[3])
                        .washMode(DialogFingerprintWashMode.TEMPLATE_SPECIFIC)
                        .fingerprint(fingerprint)
                        .preparedAtMs(now)
                        .lastVerifiedAtMs(now)
                        .source(source)
                        .debugImagePath(detection.rawPath())
                        .build();
            } finally {
                crop.flush();
            }
        } finally {
            washed.flush();
        }
    }

    private int[] buildRememberedValidationLocalRect(int relativeX, int relativeY, int imageWidth, int imageHeight) {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return null;
        }
        int left = Math.max(0, relativeX - 44);
        int top = Math.max(0, relativeY - 18);
        int right = Math.min(imageWidth, relativeX + 44);
        int bottom = Math.min(imageHeight, relativeY + 18);
        return right > left && bottom > top ? new int[]{left, top, right, bottom} : null;
    }

    private DialogFingerprintWashMode resolveFingerprintWashMode(String variantName) {
        if ("yellow".equalsIgnoreCase(variantName)) {
            return DialogFingerprintWashMode.YELLOW;
        }
        if ("green".equalsIgnoreCase(variantName)) {
            return DialogFingerprintWashMode.GREEN;
        }
        return DialogFingerprintWashMode.TEMPLATE_SPECIFIC;
    }

    private Optional<OcrWordResult> selectOcrFallbackOption(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return Optional.empty();
        }
        return words.stream()
                .filter(word -> word != null && word.getText() != null && !word.getText().isBlank())
                .filter(word -> !word.getText().matches(".*\\d+.*"))
                .max(Comparator.comparingInt(OcrWordResult::getY)
                        .thenComparingInt(OcrWordResult::getX));
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
