package com.bot.dhxy.service;

import com.bot.dhxy.cloud.task.DialogPolicyCloudDecision;
import com.bot.dhxy.cloud.task.DialogPolicyCloudDecisionService;
import com.bot.dhxy.cloud.task.DialogPolicyPreClickCloudDecision;
import com.bot.dhxy.cloud.task.DialogPolicyPreClickCloudRequest;
import com.bot.dhxy.cloud.task.ImagePreprocessOperation;
import com.bot.dhxy.cloud.task.ImageProcessorService;
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
import com.bot.dhxy.model.dialog.WhiteTemplateSpec;
import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.service.dialog.DialogOptionPolicy;
import com.bot.dhxy.service.dialog.DialogStoryPolicy;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.vision.ObjectiveTextRecognitionService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Detects and handles game dialogs for the currently bound window.
 *
 * <p>The service separates story dialogs, option dialogs, and give-item flows. Public methods either
 * submit their own input sequence or explicitly state that they are intended for an existing
 * exclusive input section. Capture paths are window-scoped through {@link WindowScopedTempPath} so
 * concurrent windows do not overwrite each other's diagnostic images.</p>
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
    private final ObjectiveTextRecognitionService objectiveTextRecognitionService;
    private final ObjectProvider<SmartClickEvidenceConfirmationService> smartClickEvidenceConfirmationService;
    private final DialogPolicyCloudDecisionService dialogPolicyCloudDecisionService;
    private final ImageProcessorService imageProcessorService;

    private final Random random = new Random();

    private static final int DIALOG_SMALL_X = 250;
    private static final int DIALOG_SMALL_Y = 345;
    private static final int DIALOG_SMALL_W = 529;
    private static final int DIALOG_SMALL_H = 143;
    private static final int STORY_FAST_CLICK_RANDOM_X = 100;
    private static final int STORY_FAST_CLICK_RANDOM_Y = 18;
    private static final int CROP_TOP_Y = 42;
    private static final int CROP_DEV_Y = 58;
    private static final int CROP_LEFT_X = 161;
    private static final int STORY_MIN_TEXT_PIXELS = 450;
    private static final int STORY_MIN_TEXT_ROWS = 10;
    private static final int STORY_MIN_MAX_ROW_WHITE = 40;
    private static final int STORY_MIN_MAX_CLUSTERS = 20;
    private static final int STORY_MIN_MAX_SPAN = 120;

    private static final int DIALOG_LARGE_X = 250;
    private static final int DIALOG_LARGE_Y = 312;
    private static final int DIALOG_LARGE_W = 529;
    private static final int DIALOG_LARGE_H = 208;
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final double WHITE_STORY_TEMPLATE_THRESHOLD = 0.85;
    private static final int HIDE_PLAYER_NAMES_SETTLE_MS = 220;

    private static final int PREPARED_DIALOG_FINGERPRINT_MAX_DISTANCE = 8;
    private static final int XIULUO_ENTER_BATTLE_PREPARED_FINGERPRINT_MAX_DISTANCE = 16;
    private static final String XIULUO_ENTER_BATTLE_ACTION_KEY = "xiuluo.enterBattle";
    private static final String XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE =
            "images/template/dialog/xiuluo/xiuluo_enter_battle_kanda2.png";
    // User sampled the button at screen absolute (416,474)-(457,495) while window base was (152,98).
    // Store only window-relative offsets here; CoordinateHelper adds the current window base at runtime.
    private static final int XIULUO_ENTER_BATTLE_LOCAL_ROI_LEFT = 264;
    private static final int XIULUO_ENTER_BATTLE_LOCAL_ROI_TOP = 376;
    private static final int XIULUO_ENTER_BATTLE_LOCAL_ROI_RIGHT = 305;
    private static final int XIULUO_ENTER_BATTLE_LOCAL_ROI_BOTTOM = 397;
    private static final double XIULUO_ENTER_BATTLE_LOCAL_MATCH_RATE = 0.82;

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
        logHandleRequest(request);

        // Stage 1: optional opening click, used by tasks that first need to poke an NPC or object.
        if (request.getInitialClick() != null) {
            Point p = request.getInitialClick();
            log.info("dialog request initial click: ({},{})", p.x, p.y);
            inputSequences.clickLeft("dialog:requestInitialClick", p.x, p.y, 150);
            if (!TaskSleep.sleep(600 + random.nextInt(200))) {
                return finishRequest(request, DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.NONE), true);
            }
        }

        Optional<DialogResult> localXiuluoEnterBattle = tryHandleXiuluoEnterBattleLocalTemplate(request);
        if (localXiuluoEnterBattle.isPresent()) {
            return finishRequest(request, localXiuluoEnterBattle.get(), false);
        }

        // Stage 2: classify once. A successful screenshot is not proof of a dialog:
        // when type is NONE, the fixed dialog area may still contain route text or labels.
        DialogDetection detection;
        if (isDialogOptionPreClickCovered(request) && !request.isVerifyDialogType()) {
            /*
             * Known-option callers used to skip detection and click a remembered/template point
             * directly. Cloud-owned Dialog click strategy still needs raw dialog pixels, so capture
             * the ROI as payload and let DIALOG_POLICY decide whether any option should be clicked.
             */
            detection = captureDialogSnapshot(
                    "handle-dialog-known-option-cloud:" + request.getOperation(),
                    request.isHidePlayerNamesBeforeCapture());
            if (detection.image() != null) {
                detection = detection.withType(DialogType.OPTION);
            }
        } else {
            detection = detectDialogSnapshotDirect(
                    "handle-dialog:" + request.getOperation(),
                    request.isHidePlayerNamesBeforeCapture());
        }
        DialogType type = detection.type();

        if (type == DialogType.NONE) {
            return finishRequest(request, DialogResult.simple(DialogResultStatus.NO_DIALOG, type), true);
        }

        // Stage 3: story dialogs are either clicked through or deliberately left alone.
        if (type == DialogType.STORY) {
            if (request.getOperation() == com.bot.dhxy.service.dialog.DialogOperation.READ_STORY_OBJECTIVE) {
                return finishRequest(request, handleStoryObjective(request, detection), true);
            }
            if (request.getOperation() == com.bot.dhxy.service.dialog.DialogOperation.VERIFY_WHITE_TEMPLATE) {
                return finishRequest(request, verifyWhiteStoryTemplateInCloud(request, detection), false);
            }
            if (request.getStoryPolicy() == DialogStoryPolicy.CLICK_THROUGH) {
                Optional<DialogResult> cloudPreClickResult = tryHandleCloudPreClickOption(request, detection);
                if (cloudPreClickResult.isPresent()) {
                    return finishRequest(request, cloudPreClickResult.get(), false);
                }
                return finishRequest(request, DialogResult.simple(DialogResultStatus.FAILED, type), true);
            }
            return finishRequest(request, DialogResult.simple(DialogResultStatus.STORY_IGNORED, type), true);
        }

        Optional<DialogResult> cloudPreClickResult = tryHandleCloudPreClickOption(request, detection);
        if (cloudPreClickResult.isPresent()) {
            return finishRequest(request, cloudPreClickResult.get(), false);
        }

        if (request.getOptionPolicy() == DialogOptionPolicy.VERIFY_GREEN_TEMPLATE) {
            return finishRequest(request, verifyGreenTemplateOption(request, detection), false);
        }

        // Stage 4: option dialogs are handled by the request's explicit option policy.
        DialogResult result = switch (request.getOptionPolicy()) {
            case IGNORE -> DialogResult.simple(DialogResultStatus.OPTION_IGNORED, type);
            case VERIFY_OPTION -> DialogResult.simple(DialogResultStatus.OPTION_VISIBLE, type);
            case CLICK_KEYWORD,
                    CLICK_REMEMBERED_POINT,
                    CLICK_BUSINESS_OPTION,
                    GIVE_ITEM_IF_AVAILABLE,
                    CLICK_GREEN_TEMPLATE,
                    FALLBACK_FIRST_OPTION,
                    FALLBACK_LAST_OPTION -> DialogResult.simple(DialogResultStatus.FAILED, type);
            case VERIFY_GREEN_TEMPLATE -> verifyGreenTemplateOption(request, detection);
        };
        return finishRequest(request, result, true);
    }

    private void logHandleRequest(DialogHandleRequest request) {
        log.info("dialog handle request: source={} operation={} storyPolicy={} optionPolicy={} fallbackPolicy={} itemToGive={} targetKeyword={}",
                request.getSourceTask(), request.getOperation(), request.getStoryPolicy(), request.getOptionPolicy(),
                request.getFallbackPolicy(), request.getItemToGive(), request.getTargetKeyword());
    }

    private Optional<DialogResult> tryHandleXiuluoEnterBattleLocalTemplate(DialogHandleRequest request) {
        if (!isXiuluoEnterBattleTemplateRequest(request)) {
            return Optional.empty();
        }
        Optional<LocalDialogTemplateMatch> matched = findXiuluoEnterBattleLocalTemplate(
                request.getSourceTask(), "handle-dialog");
        if (matched.isEmpty()) {
            log.info("dialog xiuluo enter-battle local template miss: source={} template={} roi=({}, {})-({}, {})",
                    request.getSourceTask(), XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE,
                    XIULUO_ENTER_BATTLE_LOCAL_ROI_LEFT, XIULUO_ENTER_BATTLE_LOCAL_ROI_TOP,
                    XIULUO_ENTER_BATTLE_LOCAL_ROI_RIGHT, XIULUO_ENTER_BATTLE_LOCAL_ROI_BOTTOM);
            return Optional.of(DialogResult.simple(DialogResultStatus.GREEN_TEMPLATE_NOT_FOUND, DialogType.NONE));
        }
        LocalDialogTemplateMatch match = matched.get();
        boolean clicked;
        if (isInputWorkerThread()) {
            if (!InputActionScope.checkpoint()) {
                return Optional.of(DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.OPTION));
            }
            inputProvider.moveMouse(match.absoluteX(), match.absoluteY());
            if (!TaskSleep.sleep(80) || !InputActionScope.checkpoint()) {
                return Optional.of(DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.OPTION));
            }
            inputProvider.clickLeft(match.absoluteX(), match.absoluteY(), 150);
            clicked = TaskSleep.sleep(650 + random.nextInt(150)) && InputActionScope.checkpoint();
        } else {
            clicked = inputSequences.moveAndClickLeft(
                    "dialog:xiuluoEnterBattleLocal:" + safeDebugName(request.getSourceTask()),
                    match.absoluteX(),
                    match.absoluteY(),
                    80,
                    650 + random.nextInt(150));
        }
        log.info("dialog xiuluo enter-battle local template action: source={} actionKey={} template={} "
                        + "score={} click=({}, {}) roi=({}, {})-({}, {}) clicked={}",
                request.getSourceTask(), firstGreenTemplateSpecName(request), XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE,
                String.format("%.4f", match.score()), match.absoluteX(), match.absoluteY(),
                match.rect()[0], match.rect()[1], match.rect()[2], match.rect()[3], clicked);
        if (!clicked) {
            return Optional.of(DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.OPTION));
        }
        return Optional.of(DialogResult.statusBuilder(DialogResultStatus.GREEN_TEMPLATE_CLICKED, DialogType.OPTION)
                .actionKey(firstGreenTemplateSpecName(request))
                .matchedText("local-template:" + XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE)
                .relativeX(match.absoluteX() - match.rect()[0])
                .relativeY(match.absoluteY() - match.rect()[1])
                .absoluteX(match.absoluteX())
                .absoluteY(match.absoluteY())
                .build());
    }

    /**
     * CR232: submit ONE static image of the current stop state to the cloud and return the cloud's
     * explicit verdict. No local template matching runs and no click is executed here — the caller
     * maps CLOUD_EXECUTED(point) / CLOUD_NO_ACTION / everything-else to its
     * CLOUD_PREPARED / CLOUD_FALLBACK / CLOUD_UNAVAILABLE tri-state. Returns {@code null} when the
     * static capture or payload build fails (never a fallback condition).
     */
    public DialogPolicyPreClickCloudDecision decideXiuluoEnterBattleStopStatic(DialogHandleRequest request) {
        return decideXiuluoEnterBattleStopStatic(request, null, null);
    }

    /**
     * CR232: same stop-static cloud request, but tagged with the outcome of a previously
     * cloud-supplied enter-battle click for this attempt (e.g. {@code CLICK_FAILED}). The cloud
     * confirms the failure and returns an explicit NO_ACTION fallback; no local template or click
     * runs here.
     */
    public DialogPolicyPreClickCloudDecision decideXiuluoEnterBattleStopStatic(
            DialogHandleRequest request, String priorClickOutcome, String priorClickAttemptId) {
        if (!dialogPolicyCloudDecisionService.isPreClickActive()) {
            return null;
        }
        DialogDetection detection = detectDialogSnapshotDirect(
                "xiuluo-kanda-static:" + request.getSourceTask(),
                request.isHidePlayerNamesBeforeCapture());
        if (detection == null || detection.image() == null || detection.dialogRect() == null) {
            log.info("xiuluo kanda static capture unavailable; cloud verdict not requested: source={} type={}",
                    request.getSourceTask(), detection == null ? null : detection.type());
            return null;
        }
        DialogPolicyPreClickCloudRequest cloudRequest = buildDialogPreClickCloudRequest(request, detection);
        if (cloudRequest == null) {
            return null;
        }
        if (priorClickOutcome != null) {
            cloudRequest = cloudRequest.toBuilder()
                    .priorClickOutcome(priorClickOutcome)
                    .priorClickAttemptId(priorClickAttemptId)
                    .build();
        }
        return dialogPolicyCloudDecisionService.decidePreClick(cloudRequest);
    }

    private Optional<DialogResult> tryHandleCloudPreClickOption(DialogHandleRequest request, DialogDetection detection) {
        if (!isDialogPreClickCovered(request)) {
            return Optional.empty();
        }
        if (!dialogPolicyCloudDecisionService.isPreClickActive()) {
            log.warn("dialog pre-click cloud inactive for covered option: source={} operation={} optionPolicy={}",
                    request.getSourceTask(), request.getOperation(), request.getOptionPolicy());
            return Optional.of(DialogResult.simple(DialogResultStatus.FAILED, DialogType.OPTION));
        }
        DialogDetection optionDetection = detection;
        if (optionDetection == null) {
            optionDetection = detectDialogSnapshotDirect(
                    "dialog-pre-click-cloud:" + request.getSourceTask(),
                    request.isHidePlayerNamesBeforeCapture());
        }
        if (optionDetection.type() == DialogType.STORY
                && request.getStoryPolicy() != DialogStoryPolicy.CLICK_THROUGH) {
            return Optional.of(DialogResult.simple(DialogResultStatus.OPTION_IGNORED, optionDetection.type()));
        }
        if (optionDetection.image() == null || optionDetection.dialogRect() == null) {
            log.warn("dialog pre-click cloud payload unavailable for covered option: source={} operation={} optionPolicy={} type={}",
                    request.getSourceTask(), request.getOperation(), request.getOptionPolicy(), optionDetection.type());
            return Optional.of(DialogResult.simple(DialogResultStatus.FAILED, DialogType.OPTION));
        }

        DialogPolicyPreClickCloudRequest cloudRequest = buildDialogPreClickCloudRequest(request, optionDetection);
        if (cloudRequest == null) {
            return Optional.of(DialogResult.simple(DialogResultStatus.FAILED, DialogType.OPTION));
        }
        DialogPolicyPreClickCloudDecision decision = dialogPolicyCloudDecisionService.decidePreClick(cloudRequest);
        if (decision.getStatus() == DialogPolicyPreClickCloudDecision.Status.DISABLED) {
            log.warn("dialog pre-click cloud disabled/shadow for covered option: source={} operation={} optionPolicy={} reason={}",
                    request.getSourceTask(), request.getOperation(), request.getOptionPolicy(), decision.getReason());
            return Optional.of(DialogResult.simple(DialogResultStatus.FAILED, DialogType.OPTION));
        }
        if (!decision.isCloudExecuted()) {
            log.warn("dialog pre-click cloud no executable action: source={} operation={} optionPolicy={} status={} action={} reason={}",
                    request.getSourceTask(), request.getOperation(), request.getOptionPolicy(),
                    decision.getStatus(), decision.getAction(), decision.getReason());
            return Optional.of(DialogResult.simple(DialogResultStatus.FAILED, DialogType.OPTION));
        }
        return Optional.of(executeDialogCloudPreClick(request, optionDetection, cloudRequest, decision));
    }

    /**
     * Clicks the known small story dialog without detection or cloud policy.
     *
     * @param source business source for logs/input queue descriptions; no semantics are inferred from it
     * @return {@code true} when the click sequence completed without pause/stop interruption
     */
    public boolean fastClickKnownSmallStoryDialog(String source) {
        int[] rect = getSmallDialogRect();
        Point clickPoint = coordinateHelper.getRandomizedPoint(
                new Point(rect[0] + (rect[2] - rect[0]) / 2,
                        rect[1] + Math.max(0, (rect[3] - rect[1]) - 40)),
                STORY_FAST_CLICK_RANDOM_X,
                STORY_FAST_CLICK_RANDOM_Y);
        int clickX = clickPoint.x;
        int clickY = clickPoint.y;
        boolean clicked;
        if (isInputWorkerThread()) {
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.moveMouse(clickX, clickY);
            if (!TaskSleep.sleep(80) || !InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.clickLeft(clickX, clickY, 120);
            clicked = TaskSleep.sleep(350) && InputActionScope.checkpoint();
        } else {
            clicked = inputSequences.moveAndClickLeft(
                    "dialog:fastStoryClick:" + safeDebugName(source),
                    clickX,
                    clickY,
                    80,
                    350);
        }
        log.info("dialog known small story fast-click: source={} click=({}, {}) rect=({}, {})-({}, {}) clicked={}",
                source, clickX, clickY, rect[0], rect[1], rect[2], rect[3], clicked);
        return clicked;
    }

    /**
     * Return whether CR167 may let DIALOG_POLICY own the dialog interaction before local click logic.
     */
    private boolean isDialogPreClickCovered(DialogHandleRequest request) {
        if (request == null) {
            return false;
        }
        if (request.getStoryPolicy() == DialogStoryPolicy.CLICK_THROUGH) {
            return true;
        }
        return isDialogOptionPreClickCovered(request);
    }

    private boolean isDialogOptionPreClickCovered(DialogHandleRequest request) {
        if (request == null) {
            return false;
        }
        DialogOptionPolicy policy = request.getOptionPolicy();
        return switch (policy) {
            case CLICK_KEYWORD,
                 CLICK_REMEMBERED_POINT,
                 CLICK_BUSINESS_OPTION,
                 CLICK_GREEN_TEMPLATE,
                 GIVE_ITEM_IF_AVAILABLE -> true;
            case FALLBACK_FIRST_OPTION, FALLBACK_LAST_OPTION -> true;
            case IGNORE, VERIFY_OPTION, VERIFY_GREEN_TEMPLATE -> false;
        };
    }

    private DialogPolicyPreClickCloudRequest buildDialogPreClickCloudRequest(
            DialogHandleRequest request,
            DialogDetection detection) {
        return buildDialogPolicyImageCloudRequest(request, detection, "dialog-pre-click", "dialog-pre-click-option");
    }

    private DialogPolicyPreClickCloudRequest buildDialogPolicyImageCloudRequest(
            DialogHandleRequest request,
            DialogDetection detection,
            String source,
            String phase) {
        if (request == null || detection == null || detection.image() == null || detection.dialogRect() == null) {
            return null;
        }
        int[] rect = detection.dialogRect();
        int windowBaseX = tracker.getWindowBaseX();
        int windowBaseY = tracker.getWindowBaseY();
        int roiX = rect[0] - windowBaseX;
        int roiY = rect[1] - windowBaseY;
        int roiWidth = rect[2] - rect[0];
        int roiHeight = rect[3] - rect[1];
        String debugImageId = "dialog-pre-click-" + UUID.randomUUID();
        String rawPath = detection.rawPath();
        if (rawPath == null || rawPath.isBlank()) {
            rawPath = windowScopedTempPath.resolve("dialog_pre_click_cloud_raw.png");
            if (!ImagePreprocessor.saveImage(detection.image(), rawPath)) {
                rawPath = "";
            }
        }
        try {
            byte[] pngBytes = pngBytes(detection.image());
            WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
            return DialogPolicyPreClickCloudRequest.builder()
                    .imagePayloadBase64(Base64.getEncoder().encodeToString(pngBytes))
                    .payloadMimeType("image/png")
                    .imageSha256(sha256Hex(pngBytes))
                    .rawImagePath(rawPath)
                    .debugImageId(debugImageId)
                    .roi(DialogPolicyPreClickCloudRequest.Roi.builder()
                            .x(roiX)
                            .y(roiY)
                            .width(roiWidth)
                            .height(roiHeight)
                    .build())
                    .windowWidth(GAME_CLIENT_WIDTH)
                    .windowHeight(GAME_CLIENT_HEIGHT)
                    .dialogRequest(request)
                    .detectedDialogType(detection.type() == null ? null : detection.type().name())
                    .taskCode(taskCode(request.getSourceTask()))
                    .source(source)
                    .phase(phase)
                    .windowId(runtime == null ? null : runtime.getWindowId())
                    .hwnd(runtime == null || runtime.getNativeBinding() == null
                            ? null
                            : runtime.getNativeBinding().getNativeHandle())
                    .build();
        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("dialog pre-click cloud payload build failed: source={} operation={} reason={}",
                    request.getSourceTask(), request.getOperation(), e.getMessage(), e);
            return null;
        }
    }

    private DialogResult executeDialogCloudPreClick(
            DialogHandleRequest request,
            DialogDetection detection,
            DialogPolicyPreClickCloudRequest cloudRequest,
            DialogPolicyPreClickCloudDecision decision) {
        Point clickRel = decision.getWindowRelativeClickPoint();
        int clickX = tracker.getWindowBaseX() + clickRel.x;
        int clickY = tracker.getWindowBaseY() + clickRel.y;
        boolean clicked;
        if (isInputWorkerThread()) {
            if (!InputActionScope.checkpoint()) {
                return DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.OPTION);
            }
            inputProvider.moveMouse(clickX, clickY);
            if (!TaskSleep.sleep(150) || !InputActionScope.checkpoint()) {
                return DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.OPTION);
            }
            inputProvider.clickLeft(clickX, clickY, 150);
            clicked = TaskSleep.sleep(650 + random.nextInt(150)) && InputActionScope.checkpoint();
        } else {
            clicked = inputSequences.moveAndClickLeft(
                    "dialog:cloudPreClick:" + safeDebugName(request.getSourceTask()),
                    clickX,
                    clickY,
                    150,
                    650 + random.nextInt(150));
        }
        log.info("dialog pre-click cloud action outcome: source={} operation={} optionPolicy={} actionId={} decisionId={} "
                        + "clickRel=({}, {}) clickAbs=({}, {}) clicked={} candidateBox={} reason={}",
                request.getSourceTask(), request.getOperation(), request.getOptionPolicy(), decision.getActionId(),
                decision.getDecisionId(), clickRel.x, clickRel.y, clickX, clickY, clicked,
                decision.getCandidateBox(), decision.getReason());
        if (!clicked) {
            return DialogResult.simple(DialogResultStatus.INTERRUPTED, DialogType.OPTION);
        }
        int relativeX = clickRel.x - cloudRequest.getRoi().getX();
        int relativeY = clickRel.y - cloudRequest.getRoi().getY();
        if (request.getOptionPolicy() == DialogOptionPolicy.GIVE_ITEM_IF_AVAILABLE) {
            boolean given = giveItemService.executeGiveDirectForExclusive(
                    request.getItemToGive(), request.getKnownBagIndex());
            return DialogResult.statusBuilder(
                            given ? DialogResultStatus.GIVE_ITEM_DONE : DialogResultStatus.GIVE_ITEM_FAILED,
                            detection.type())
                    .actionKey(cloudPreClickActionKey(request, decision))
                    .matchedText("cloud-pre-click:" + (decision.getReason() == null ? "" : decision.getReason()))
                    .relativeX(relativeX)
                    .relativeY(relativeY)
                    .absoluteX(clickX)
                    .absoluteY(clickY)
                    .build();
        }
        return DialogResult.statusBuilder(cloudPreClickStatus(request), detection.type())
                .actionKey(cloudPreClickActionKey(request, decision))
                .matchedText("cloud-pre-click:" + (decision.getReason() == null ? "" : decision.getReason()))
                .relativeX(relativeX)
                .relativeY(relativeY)
                .absoluteX(clickX)
                .absoluteY(clickY)
                .build();
    }

    private DialogResultStatus cloudPreClickStatus(DialogHandleRequest request) {
        if (request.getStoryPolicy() == DialogStoryPolicy.CLICK_THROUGH) {
            return DialogResultStatus.STORY_CLICKED;
        }
        if (request.getOptionPolicy() == DialogOptionPolicy.CLICK_GREEN_TEMPLATE) {
            return DialogResultStatus.GREEN_TEMPLATE_CLICKED;
        }
        if (request.getOptionPolicy() == DialogOptionPolicy.CLICK_BUSINESS_OPTION) {
            return DialogResultStatus.BUSINESS_OPTION_CLICKED;
        }
        if (request.getOptionPolicy() == DialogOptionPolicy.FALLBACK_FIRST_OPTION
                || request.getOptionPolicy() == DialogOptionPolicy.FALLBACK_LAST_OPTION) {
            return DialogResultStatus.FALLBACK_CLICKED;
        }
        return DialogResultStatus.OPTION_KEYWORD_CLICKED;
    }

    private static String cloudPreClickActionKey(
            DialogHandleRequest request,
            DialogPolicyPreClickCloudDecision decision) {
        if (request.getOptionPolicy() == DialogOptionPolicy.CLICK_KEYWORD) {
            return request.getTargetKeyword();
        }
        return decision.getActionId();
    }

    private static byte[] pngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static String sha256Hex(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(bytes);
        StringBuilder result = new StringBuilder(hashed.length * 2);
        for (byte value : hashed) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

    private static String taskCode(String sourceTask) {
        if (sourceTask == null || sourceTask.isBlank()) {
            return "unknown";
        }
        String lower = sourceTask.toLowerCase();
        if (lower.contains("xiuluo")) {
            return "xiuluo_v2";
        }
        if (lower.contains("wubei")) {
            return "wubei";
        }
        if (lower.contains("wuhuan") || lower.contains("five-ring") || lower.contains("five_ring")) {
            return "wuhuan_v2";
        }
        if (lower.contains("auto-battle") || lower.contains("auto_battle")) {
            return "auto_battle";
        }
        return "unknown";
    }

    private DialogResult verifyGreenTemplateOption(DialogHandleRequest request, DialogDetection detection) {
        List<GreenTemplateClickSpec> specs = request.getGreenTemplateSpecs();
        if (specs == null || specs.isEmpty()) {
            log.warn("dialog expected template verification requested without specs: source={}", request.getSourceTask());
            return DialogResult.simple(DialogResultStatus.GREEN_TEMPLATE_NOT_FOUND, detection.type());
        }

        int[] rect = detection.dialogRect() != null ? detection.dialogRect() : getDialogRect();
        DialogPolicyPreClickCloudRequest cloudRequest = buildDialogPreClickCloudRequest(request, detection);
        if (cloudRequest == null) {
            log.warn("dialog expected template cloud payload unavailable: source={} operation={} type={}",
                    request.getSourceTask(), request.getOperation(), detection.type());
            return DialogResult.simple(DialogResultStatus.FAILED, detection.type());
        }

        DialogPolicyPreClickCloudDecision decision = dialogPolicyCloudDecisionService.decidePreClick(cloudRequest);
        if (decision.getStatus() == DialogPolicyPreClickCloudDecision.Status.DISABLED
                || decision.getStatus() == DialogPolicyPreClickCloudDecision.Status.REQUIRED_FAILURE) {
            log.warn("dialog expected template cloud verification failed: source={} status={} action={} actionId={} reason={}",
                    request.getSourceTask(), decision.getStatus(), decision.getAction(),
                    decision.getActionId(), decision.getReason());
            return DialogResult.simple(DialogResultStatus.FAILED, detection.type());
        }
        boolean cloudGreenVisible = decision.getStatus() == DialogPolicyPreClickCloudDecision.Status.CLOUD_NO_ACTION
                && decision.getAction() == DialogPolicyPreClickCloudDecision.Action.NO_ACTION
                && greenTemplateSpecNames(request).contains(decision.getActionId());
        if (!cloudGreenVisible && decision.getStatus() == DialogPolicyPreClickCloudDecision.Status.CLOUD_NO_ACTION
                && decision.getAction() == DialogPolicyPreClickCloudDecision.Action.NO_ACTION
                && "OPTION_DIALOG_VISIBLE".equals(decision.getActionId())) {
            log.info("dialog expected template cloud recovered generic option visibility only: source={} reason={} candidateBox={}",
                    request.getSourceTask(), decision.getReason(), decision.getCandidateBox());
            return DialogResult.simple(DialogResultStatus.OPTION_VISIBLE, detection.type());
        }
        if (!cloudGreenVisible) {
            log.info("dialog expected template cloud not visible: source={} status={} action={} actionId={} reason={}",
                    request.getSourceTask(), decision.getStatus(), decision.getAction(),
                    decision.getActionId(), decision.getReason());
            return DialogResult.simple(DialogResultStatus.GREEN_TEMPLATE_NOT_FOUND, detection.type());
        }

        int absoluteX = rect[0];
        int absoluteY = rect[1];
        int[] candidateBox = parseWindowRelativeBox(decision.getCandidateBox());
        if (candidateBox != null) {
            absoluteX = tracker.getWindowBaseX() + candidateBox[0] + candidateBox[2] / 2;
            absoluteY = tracker.getWindowBaseY() + candidateBox[1] + candidateBox[3] / 2;
        }
        String matchedText = decision.getMatchedText() == null || decision.getMatchedText().isBlank()
                ? "cloud-semantic:" + (decision.getReason() == null ? "" : decision.getReason())
                : decision.getMatchedText();
        log.info("dialog expected template cloud visible: source={} actionId={} decisionId={} point=({}, {}) candidateBox={} reason={}",
                request.getSourceTask(), decision.getActionId(), decision.getDecisionId(),
                absoluteX, absoluteY, decision.getCandidateBox(), decision.getReason());
        return DialogResult.statusBuilder(DialogResultStatus.GREEN_TEMPLATE_VISIBLE, detection.type())
                .actionKey(decision.getActionId())
                .matchedText(matchedText)
                .relativeX(Math.max(0, absoluteX - rect[0]))
                .relativeY(Math.max(0, absoluteY - rect[1]))
                .absoluteX(absoluteX)
                .absoluteY(absoluteY)
                .build();
    }

    private DialogResult verifyWhiteStoryTemplate(DialogHandleRequest request, DialogDetection detection) {
        List<WhiteTemplateSpec> specs = request.getWhiteTemplateSpecs() == null
                ? List.of()
                : request.getWhiteTemplateSpecs();
        if (specs.isEmpty()) {
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
        if (!cloudWashToPath(request.getSourceTask(), "dialog-verify-white-template",
                rawPath, washedPath, ImagePreprocessOperation.WASH_WHITE)) {
            log.warn("dialog white template wash failed: source={} raw={} washed={}",
                    request.getSourceTask(), rawPath, washedPath);
            return DialogResult.simple(DialogResultStatus.WHITE_TEMPLATE_NOT_FOUND, detection.type());
        }
        for (WhiteTemplateSpec spec : specs) {
            if (spec == null || spec.templatePath() == null || spec.templatePath().isBlank()) {
                continue;
            }
            double[] result = ImageFinder.find(washedPath, spec.templatePath(), 0.85);
            if (result == null || result.length < 2) {
                continue;
            }

            Point point = coordinateHelper.resolveMatchedPointInRect(rect, result);
            log.info("dialog white template visible: source={} template={} point=({}, {})",
                    request.getSourceTask(), spec.templatePath(), point.x, point.y);
            return DialogResult.statusBuilder(DialogResultStatus.WHITE_TEMPLATE_VISIBLE, detection.type())
                    .actionKey(spec.name())
                    .matchedText(spec.templatePath())
                    .relativeX(point.x - rect[0])
                    .relativeY(point.y - rect[1])
                    .absoluteX(point.x)
                    .absoluteY(point.y)
                    .build();
        }

        log.info("dialog white template not visible: source={} candidates={}",
                request.getSourceTask(), specs.size());
        return DialogResult.simple(DialogResultStatus.WHITE_TEMPLATE_NOT_FOUND, detection.type());
    }

    private DialogResult verifyWhiteStoryTemplateInCloud(DialogHandleRequest request, DialogDetection detection) {
        DialogPolicyPreClickCloudRequest cloudRequest = buildDialogPolicyImageCloudRequest(
                request, detection, "dialog-white-template", "dialog-white-template");
        DialogPolicyCloudDecision decision = dialogPolicyCloudDecisionService.decideWhiteTemplate(cloudRequest);
        DialogResult result = decision.getEffectiveResult();
        if (result == null) {
            return DialogResult.simple(DialogResultStatus.FAILED, detection == null ? DialogType.NONE : detection.type());
        }
        if (result.getStatus() == DialogResultStatus.WHITE_TEMPLATE_VISIBLE) {
            log.info("dialog white template visible by cloud: source={} actionKey={} matchedText={}",
                    request.getSourceTask(), result.getActionKey(), result.getMatchedText());
        } else {
            log.info("dialog white template cloud result: source={} status={} actionKey={} matchedText={}",
                    request.getSourceTask(), result.getStatus(), result.getActionKey(), result.getMatchedText());
        }
        return result;
    }

    /**
     * Prepare a route-transfer option click without sending input.
     *
     * @param source diagnostic source for logs.
     * @param targetKeyword destination text to match in the option dialog.
     * @return prepared action when the target option is visible and matched.
     */
    public Optional<PreparedDialogAction> prepareRouteKeywordOption(String source, String targetKeyword) {
        return prepareRouteKeywordOption(source, targetKeyword, null);
    }

    /**
     * Prepare a route-transfer option click, reusing a tick-scoped dialog detection when valid.
     *
     * @param source diagnostic source for logs.
     * @param targetKeyword destination text to match in the option dialog.
     * @param suppliedDetection optional fresh detection captured earlier in the same watcher tick;
     *                          it is accepted only when it is an OPTION with an image.
     * @return prepared action when the target option is visible and matched.
     */
    public Optional<PreparedDialogAction> prepareRouteKeywordOption(String source,
                                                                    String targetKeyword,
                                                                    DialogDetection suppliedDetection) {
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
        DialogDetection detection = usableSuppliedDialogDetection(
                suppliedDetection, DialogType.OPTION, "prepare-route:" + source)
                .orElseGet(() -> detectDialogSnapshotDirect("prepare-route:" + source, false, 0));
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
        DialogHandleRequest request = DialogHandleRequest.handleRouteKeywordOption(source, targetKeyword, false);
        Optional<PreparedDialogAction> prepared = prepareCloudDialogAction(
                request, DialogOperation.ROUTE_TRANSFER, detection, "dialog prepare route");
        log.info("dialog prepare route result: source={} target={} prepared={} detectMs={} totalMs={}",
                source, targetKeyword, prepared.isPresent(), detectElapsedMs,
                Math.max(0L, System.currentTimeMillis() - startedAt));
        return prepared;
    }

    public Optional<PreparedDialogAction> prepareRememberedRouteOption(String source,
                                                                       String targetKeyword,
                                                                       int relativeX,
                                                                       int relativeY,
                                                                       String optionText) {
        return prepareRememberedRouteOption(source, targetKeyword, relativeX, relativeY, optionText, null);
    }

    public Optional<PreparedDialogAction> prepareRememberedRouteOption(String source,
                                                                       String targetKeyword,
                                                                       int relativeX,
                                                                       int relativeY,
                                                                       String optionText,
                                                                       DialogDetection suppliedDetection) {
        return prepareRememberedChoiceOption(source, DialogOperation.ROUTE_TRANSFER,
                targetKeyword, relativeX, relativeY, optionText, true, suppliedDetection);
    }

    public Optional<PreparedDialogAction> prepareRememberedChoiceOption(String source,
                                                                        DialogOperation operation,
                                                                        String targetKeyword,
                                                                        int relativeX,
                                                                        int relativeY,
                                                                        String optionText,
                                                                        boolean verifyDialogType) {
        return prepareRememberedChoiceOption(source, operation, targetKeyword, relativeX, relativeY,
                optionText, verifyDialogType, null);
    }

    public Optional<PreparedDialogAction> prepareRememberedChoiceOption(String source,
                                                                        DialogOperation operation,
                                                                        String targetKeyword,
                                                                        int relativeX,
                                                                        int relativeY,
                                                                        String optionText,
                                                                        boolean verifyDialogType,
                                                                        DialogDetection suppliedDetection) {
        long startedAt = System.currentTimeMillis();
        DialogDetection detection = usableSuppliedDialogDetection(
                suppliedDetection, verifyDialogType ? DialogType.OPTION : null,
                "prepare-choice-memory:" + source)
                .orElseGet(() -> detectDialogSnapshotDirect("prepare-choice-memory:" + source, false, 0));
        boolean typeAccepted = !verifyDialogType || detection != null && detection.type() == DialogType.OPTION;
        if (detection == null || !typeAccepted || detection.image() == null
                || detection.dialogRect() == null) {
            log.info("dialog prepare remembered choice miss: source={} operation={} target={} type={} hasImage={} totalMs={}",
                    source, operation, targetKeyword,
                    detection == null ? null : detection.type(),
                    detection != null && detection.image() != null,
                    Math.max(0L, System.currentTimeMillis() - startedAt));
            return Optional.empty();
        }
        DialogHandleRequest request = DialogHandleRequest.builder()
                .sourceTask(source)
                .operation(operation)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_REMEMBERED_POINT)
                .fallbackPolicy(com.bot.dhxy.service.dialog.DialogFallbackPolicy.RETURN_UNRESOLVED)
                .targetKeyword(targetKeyword)
                .rememberedRelativeX(relativeX)
                .rememberedRelativeY(relativeY)
                .allowFallbackOptionClick(false)
                .verifyDialogType(verifyDialogType)
                .build();
        Optional<PreparedDialogAction> prepared = prepareCloudDialogAction(
                request, operation, detection, "dialog prepare remembered choice");
        log.info("dialog prepare remembered choice result: source={} operation={} target={} matched={} rel=({}, {}) prepared={} totalMs={}",
                source, operation, targetKeyword, optionText, relativeX, relativeY, prepared.isPresent(),
                Math.max(0L, System.currentTimeMillis() - startedAt));
        return prepared;
    }

    public Optional<PreparedDialogAction> prepareRememberedOrGreenTemplateOption(String source,
                                                                                 DialogOperation operation,
                                                                                 String targetKeyword,
                                                                                 Integer rememberedRelativeX,
                                                                                 Integer rememberedRelativeY,
                                                                                 String rememberedOptionText,
                                                                                 List<GreenTemplateClickSpec> specs,
                                                                                 boolean verifyDialogType,
                                                                                 DialogDetection suppliedDetection) {
        DialogHandleRequest request = DialogHandleRequest.builder()
                .sourceTask(source)
                .operation(operation)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_REMEMBERED_POINT)
                .fallbackPolicy(com.bot.dhxy.service.dialog.DialogFallbackPolicy.RETURN_UNRESOLVED)
                .targetKeyword(targetKeyword)
                .rememberedRelativeX(rememberedRelativeX)
                .rememberedRelativeY(rememberedRelativeY)
                .rememberedOptionText(rememberedOptionText)
                .greenTemplateSpecs(specs)
                .allowFallbackOptionClick(false)
                .verifyDialogType(verifyDialogType)
                .build();
        return prepareCloudDialogOptionAction(request, operation, suppliedDetection,
                "dialog prepare remembered-or-green");
    }

    /**
     * Prepare a known green-template option without sending input.
     *
     * @param source diagnostic source for logs.
     * @param operation operation stored on the prepared action.
     * @param specs ordered green-template candidates sent to DIALOG_POLICY for cloud-owned matching.
     * @param verifyDialogType true when the dialog must be classified as OPTION before matching.
     * @return prepared click action, or empty when no candidate is visible.
     */
    public Optional<PreparedDialogAction> prepareGreenTemplateOption(String source,
                                                                     DialogOperation operation,
                                                                     List<GreenTemplateClickSpec> specs,
                                                                     boolean verifyDialogType) {
        return prepareGreenTemplateOption(source, operation, specs, verifyDialogType, null, null);
    }

    /**
     * Prepare a known green-template option, optionally returning a negative option signal.
     *
     * @param source diagnostic source for logs.
     * @param operation operation stored on the prepared action.
     * @param specs ordered green-template candidates sent to DIALOG_POLICY for cloud-owned matching.
     * @param verifyDialogType true when the dialog must be classified as OPTION before matching.
     * @param missTargetKeyword optional action key to publish when an option dialog is visible but
     *                          none of the templates matched.
     * @return prepared click action when cloud returns an executable click, or empty when cloud
     *         misses/fails; no local template fallback is allowed.
     */
    public Optional<PreparedDialogAction> prepareGreenTemplateOption(String source,
                                                                     DialogOperation operation,
                                                                     List<GreenTemplateClickSpec> specs,
                                                                     boolean verifyDialogType,
                                                                     String missTargetKeyword) {
        return prepareGreenTemplateOption(source, operation, specs, verifyDialogType,
                missTargetKeyword, null);
    }

    public Optional<PreparedDialogAction> prepareGreenTemplateOption(String source,
                                                                     DialogOperation operation,
                                                                     List<GreenTemplateClickSpec> specs,
                                                                     boolean verifyDialogType,
                                                                     String missTargetKeyword,
                                                                     DialogDetection suppliedDetection) {
        DialogHandleRequest request = DialogHandleRequest.builder()
                .sourceTask(source)
                .operation(DialogOperation.CLICK_GREEN_TEMPLATE)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_GREEN_TEMPLATE)
                .fallbackPolicy(com.bot.dhxy.service.dialog.DialogFallbackPolicy.RETURN_UNRESOLVED)
                .greenTemplateSpecs(specs)
                .verifyDialogType(verifyDialogType)
                .build();
        return prepareGreenTemplateOption(request, operation, missTargetKeyword, suppliedDetection);
    }

    /**
     * Prepare a white-story semantic signal through DIALOG_POLICY.
     *
     * @param source diagnostic source for logs.
     * @param operation operation stored on the prepared action.
     * @param specs white story specs sent to DIALOG_POLICY; cloud owns matched/miss/absent decision and order.
     * @param missTargetKeyword optional cloud action key for "STORY exists but no known template matched".
     * @param absentTargetKeyword optional cloud action key for "no STORY frame is present".
     * @param absentMatchedText matched-text hint sent to cloud for the no-STORY result.
     * @return prepared no-click story signal from a validated cloud semantic action, or empty on cloud miss/failure.
     */
    public Optional<PreparedDialogAction> prepareCloudWhiteStoryTemplateOrAbsent(String source,
                                                                                 DialogOperation operation,
                                                                                 List<WhiteTemplateSpec> specs,
                                                                                 String missTargetKeyword,
                                                                                 String absentTargetKeyword,
                                                                                 String absentMatchedText) {
        return prepareCloudWhiteStoryTemplateOrAbsent(source, operation, specs, missTargetKeyword,
                absentTargetKeyword, absentMatchedText, null);
    }

    public Optional<PreparedDialogAction> prepareCloudWhiteStoryTemplateOrAbsent(String source,
                                                                                 DialogOperation operation,
                                                                                 List<WhiteTemplateSpec> specs,
                                                                                 String missTargetKeyword,
                                                                                 String absentTargetKeyword,
                                                                                 String absentMatchedText,
                                                                                 DialogDetection suppliedDetection) {
        DialogDetection detection = usableSuppliedStoryDetection(
                suppliedDetection, absentTargetKeyword != null, "prepare-white-story:" + source)
                .orElseGet(() -> detectDialogSnapshotDirect("prepare-white-story:" + source, false, 0));
        if (detection == null || detection.image() == null || detection.dialogRect() == null) {
            log.info("dialog prepare white story cloud miss: source={} operation={} type={} hasImage={}",
                    source, operation, detection == null ? null : detection.type(),
                    detection != null && detection.image() != null);
            return Optional.empty();
        }
        DialogHandleRequest request = DialogHandleRequest.builder()
                .sourceTask(source)
                .operation(operation)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.IGNORE)
                .fallbackPolicy(com.bot.dhxy.service.dialog.DialogFallbackPolicy.RETURN_UNRESOLVED)
                .whiteTemplateSpecs(specs)
                .storyMissTargetKeyword(missTargetKeyword)
                .storyAbsentTargetKeyword(absentTargetKeyword)
                .storyAbsentMatchedText(absentMatchedText)
                .verifyDialogType(false)
                .build();
        DialogPolicyPreClickCloudRequest cloudRequest = buildDialogPreClickCloudRequest(request, detection);
        if (cloudRequest == null) {
            return Optional.empty();
        }
        DialogPolicyPreClickCloudDecision decision = dialogPolicyCloudDecisionService.decidePreClick(cloudRequest);
        if (decision.getStatus() != DialogPolicyPreClickCloudDecision.Status.CLOUD_NO_ACTION
                || decision.getAction() != DialogPolicyPreClickCloudDecision.Action.NO_ACTION
                || decision.getActionId() == null || decision.getActionId().isBlank()) {
            log.warn("dialog prepare white story cloud no semantic action: source={} operation={} status={} action={} actionId={} reason={}",
                    source, operation, decision.getStatus(), decision.getAction(),
                    decision.getActionId(), decision.getReason());
            return Optional.empty();
        }
        Optional<PreparedDialogAction> prepared = buildCloudSemanticPreparedDialogAction(
                source, operation, detection, cloudRequest, decision);
        log.info("dialog prepare white story cloud semantic action: source={} operation={} actionId={} decisionId={} prepared={} reason={}",
                source, operation, decision.getActionId(), decision.getDecisionId(),
                prepared.isPresent(), decision.getReason());
        return prepared;
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

    /**
     * Revalidate a click-required prepared action immediately before atomic consumption.
     *
     * @param action prepared action whose validation rectangle is in screen-absolute coordinates.
     * @param reason diagnostic source for logs and screenshot capture.
     * @return the same action with refreshed {@code lastVerifiedAtMs}, or null when the validation
     *         crop no longer matches.
     */
    public PreparedDialogAction validatePreparedDialogActionForConsume(PreparedDialogAction action, String reason) {
        if (action == null) {
            return null;
        }
        if (!action.isClickRequired()) {
            log.info("prepared dialog consume skips fingerprint: reason={} operation={} target={} source={} clickRequired=false",
                    reason, action.getOperation(), action.getTargetKeyword(), action.getSource());
            return action;
        }
        if (isXiuluoEnterBattleLocalTemplateAction(action)) {
            Optional<LocalDialogTemplateMatch> matched = findXiuluoEnterBattleLocalTemplate(
                    action.getSource(), "consume-validate:" + reason);
            if (matched.isEmpty()) {
                log.info("prepared dialog consume local template failed: reason={} operation={} target={} source={} template={}",
                        reason, action.getOperation(), action.getTargetKeyword(), action.getSource(),
                        XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE);
                return null;
            }
            LocalDialogTemplateMatch match = matched.get();
            PreparedDialogAction refreshed = action.toBuilder()
                    .relativeX(match.absoluteX() - match.rect()[0])
                    .relativeY(match.absoluteY() - match.rect()[1])
                    .absoluteX(match.absoluteX())
                    .absoluteY(match.absoluteY())
                    .validationLeft(match.rect()[0])
                    .validationTop(match.rect()[1])
                    .validationRight(match.rect()[2])
                    .validationBottom(match.rect()[3])
                    .lastVerifiedAtMs(System.currentTimeMillis())
                    .build();
            log.info("prepared dialog consume local template passed: reason={} operation={} target={} source={} "
                            + "score={} click=({}, {}) roi=({}, {})-({}, {})",
                    reason, action.getOperation(), action.getTargetKeyword(), action.getSource(),
                    String.format("%.4f", match.score()), match.absoluteX(), match.absoluteY(),
                    match.rect()[0], match.rect()[1], match.rect()[2], match.rect()[3]);
            return refreshed;
        }
        if (action.getFingerprint() == null || action.getFingerprint().isBlank()) {
            log.warn("prepared dialog consume validation failed: reason={} operation={} target={} source={} cause=missing-fingerprint",
                    reason, action.getOperation(), action.getTargetKeyword(), action.getSource());
            return null;
        }
        int left = action.getValidationLeft();
        int top = action.getValidationTop();
        int right = action.getValidationRight();
        int bottom = action.getValidationBottom();
        if (right <= left || bottom <= top) {
            log.warn("prepared dialog consume validation failed: reason={} operation={} target={} source={} cause=invalid-rect rect=({}, {})-({}, {})",
                    reason, action.getOperation(), action.getTargetKeyword(), action.getSource(),
                    left, top, right, bottom);
            return null;
        }
        BufferedImage raw = null;
        BufferedImage washed = null;
        try {
            raw = captureDialogValidationImage("dialog-consume-validate:" + reason, left, top, right, bottom);
            washed = washPreparedValidationCrop(raw, action.getWashMode());
            if (washed == null) {
                log.warn("prepared dialog consume validation failed: reason={} operation={} target={} source={} cause=capture-empty",
                        reason, action.getOperation(), action.getTargetKeyword(), action.getSource());
                return null;
            }
            String currentFingerprint = cloudBinaryFingerprint(
                    washed, action.getSource(), "dialog-prepared-consume-fingerprint");
            if (currentFingerprint == null || currentFingerprint.isBlank()) {
                log.warn("prepared dialog consume validation failed: reason={} operation={} target={} source={} cause=fingerprint-unavailable",
                        reason, action.getOperation(), action.getTargetKeyword(), action.getSource());
                return null;
            }
            Integer distance = cloudBinaryFingerprintDistance(
                    action.getFingerprint(), currentFingerprint, action.getSource(),
                    "dialog-prepared-consume-fingerprint-distance");
            if (distance == null) {
                log.warn("prepared dialog consume validation failed: reason={} operation={} target={} source={} cause=fingerprint-distance-unavailable",
                        reason, action.getOperation(), action.getTargetKeyword(), action.getSource());
                return null;
            }
            int maxDistance = preparedDialogFingerprintMaxDistance(action);
            if (distance <= maxDistance) {
                PreparedDialogAction refreshed = action.toBuilder()
                        .lastVerifiedAtMs(System.currentTimeMillis())
                        .build();
                log.info("prepared dialog consume validation passed: reason={} operation={} target={} source={} distance={} maxDistance={} click=({}, {})",
                        reason, action.getOperation(), action.getTargetKeyword(), action.getSource(),
                        distance, maxDistance,
                        action.getAbsoluteX(), action.getAbsoluteY());
                return refreshed;
            }
            log.info("prepared dialog consume validation failed: reason={} operation={} target={} source={} distance={} maxDistance={} click=({}, {})",
                    reason, action.getOperation(), action.getTargetKeyword(), action.getSource(),
                    distance, maxDistance,
                    action.getAbsoluteX(), action.getAbsoluteY());
            return null;
        } catch (RuntimeException e) {
            log.debug("prepared dialog consume validation failed: reason={} operation={} target={} source={} cause={}",
                    reason, action.getOperation(), action.getTargetKeyword(), action.getSource(), e.getMessage());
            return null;
        } finally {
            if (raw != null) {
                raw.flush();
            }
            if (washed != null && washed != raw) {
                washed.flush();
            }
        }
    }

    private int preparedDialogFingerprintMaxDistance(PreparedDialogAction action) {
        if (action != null && action.getOperation() == DialogOperation.XIULUO_ENTER_BATTLE) {
            return XIULUO_ENTER_BATTLE_PREPARED_FINGERPRINT_MAX_DISTANCE;
        }
        return PREPARED_DIALOG_FINGERPRINT_MAX_DISTANCE;
    }

    private boolean cloudWashToPath(String source,
                                    String phase,
                                    String rawPath,
                                    String washedPath,
                                    ImagePreprocessOperation operation) {
        if (rawPath == null || rawPath.isBlank() || washedPath == null || washedPath.isBlank()) {
            return false;
        }
        ImageProcessorService.ImageProcessorResult result = imageProcessorService.washToPath(
                Path.of(rawPath),
                Path.of(washedPath),
                operation,
                imageProcessorMetadata(source, phase, rawPath));
        if (result != null && result.hasImage()) {
            result.image().flush();
            return true;
        }
        log.debug("dialog image preprocess wash miss: source={} phase={} operation={} status={} reason={}",
                source, phase, operation, result == null ? null : result.status(), result == null ? null : result.reason());
        return false;
    }

    private BufferedImage cloudWashImage(BufferedImage raw,
                                         DialogFingerprintWashMode washMode,
                                         String source,
                                         String phase) {
        if (raw == null) {
            return null;
        }
        ImageProcessorService.ImageProcessorResult result;
        if (washMode == DialogFingerprintWashMode.YELLOW) {
            result = imageProcessorService.washYellowText(raw, imageProcessorMetadata(source, phase, null));
        } else if (washMode == DialogFingerprintWashMode.GREEN) {
            result = imageProcessorService.washGreenTextToBlackAndWhite(raw, imageProcessorMetadata(source, phase, null));
        } else if (washMode == DialogFingerprintWashMode.WHITE) {
            result = imageProcessorService.washThinWhiteTextToBlackAndWhite(raw, imageProcessorMetadata(source, phase, null));
        } else {
            result = imageProcessorService.washDialogOptionTemplateTextToBlackAndWhite(raw,
                    imageProcessorMetadata(source, phase, null));
        }
        if (result != null && result.hasImage()) {
            return result.image();
        }
        log.debug("dialog image preprocess in-memory wash miss: source={} phase={} mode={} status={} reason={}",
                source, phase, washMode, result == null ? null : result.status(), result == null ? null : result.reason());
        return null;
    }

    private String cloudBinaryFingerprint(BufferedImage image, String source, String phase) {
        ImageProcessorService.ImageProcessorResult result =
                imageProcessorService.buildBinaryFingerprint(image, imageProcessorMetadata(source, phase, null));
        if (result != null && result.hasBinaryFingerprint()) {
            return result.binaryFingerprint();
        }
        log.debug("dialog binary fingerprint miss: source={} phase={} status={} reason={}",
                source, phase, result == null ? null : result.status(), result == null ? null : result.reason());
        return null;
    }

    private Integer cloudBinaryFingerprintDistance(String left, String right, String source, String phase) {
        ImageProcessorService.ImageProcessorResult result =
                imageProcessorService.binaryFingerprintDistance(left, right, imageProcessorMetadata(source, phase, null));
        if (result != null && result.binaryFingerprintDistance() != null) {
            return result.binaryFingerprintDistance();
        }
        log.debug("dialog binary fingerprint distance miss: source={} phase={} status={} reason={}",
                source, phase, result == null ? null : result.status(), result == null ? null : result.reason());
        return null;
    }

    private ImageProcessorService.RequestMetadata imageProcessorMetadata(String source, String phase, String rawPath) {
        String safePhase = phase == null || phase.isBlank() ? "dialog-image-preprocess" : phase;
        String safeSource = source == null || source.isBlank() ? "dialog" : source;
        return ImageProcessorService.RequestMetadata.builder()
                .rawImagePath(rawPath)
                .debugImageId(safeDebugName(safeSource + "-" + safePhase))
                .source(safeSource)
                .phase(safePhase)
                .build();
    }

    private BufferedImage washPreparedValidationCrop(BufferedImage raw, DialogFingerprintWashMode washMode) {
        return cloudWashImage(raw, washMode, "dialog-prepared-consume", "dialog-prepared-consume-wash");
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

    private DialogResult finishRequest(
            DialogHandleRequest request,
            DialogResult result,
            boolean runAfterLocalDialogPolicy) {
        confirmPendingSmartClickIfExpectedOptionProved(request, result);
        if (runAfterLocalDialogPolicy) {
            DialogPolicyCloudDecision cloudDecision = dialogPolicyCloudDecisionService.decide(request, result);
            result = cloudDecision.getEffectiveResult();
        }
        log.info("dialog handle result: source={} operation={} type={} status={} kind={} actionKey={} clicked={}",
                request.getSourceTask(), request.getOperation(), result.getDialogType(), result.getStatus(),
                result.getKind(), result.getActionKey(), result.isClicked());
        return result;
    }

    private void confirmPendingSmartClickIfExpectedOptionProved(DialogHandleRequest request, DialogResult result) {
        if (!isExpectedOptionProof(request, result)) {
            return;
        }
        SmartClickEvidenceConfirmationService confirmationService =
                smartClickEvidenceConfirmationService.getIfAvailable();
        if (confirmationService == null) {
            return;
        }
        String proofToken = windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getPendingSmartClickEvidenceProofToken)
                .orElse(null);
        confirmationService.confirmExpectedOptionProof(
                request.getSourceTask(),
                result.getActionKey(),
                result.getMatchedText(),
                proofToken,
                result.getStatus() == DialogResultStatus.OPTION_KEYWORD_CLICKED ? "DIALOG_OCR" : "DIALOG_TEMPLATE",
                "dialog-service:" + request.getOperation() + ":" + result.getStatus());
    }

    private boolean isExpectedOptionProof(DialogHandleRequest request, DialogResult result) {
        if (request == null || result == null) {
            return false;
        }
        DialogResultStatus status = result.getStatus();
        return status == DialogResultStatus.GREEN_TEMPLATE_VISIBLE
                || status == DialogResultStatus.GREEN_TEMPLATE_CLICKED
                || status == DialogResultStatus.BUSINESS_OPTION_CLICKED
                || status == DialogResultStatus.OPTION_KEYWORD_CLICKED;
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

    /**
     * Detect the current dialog type without focus and with caller-controlled pre-capture wait.
     *
     * @param reason short diagnostic label for logs and screenshots.
     * @param hidePlayerNames whether to send Alt+4 before capture.
     * @param waitBeforeCaptureMs delay before screenshot capture. Use {@code 0} for pure current-screen
     *                            probes; use a positive value only after a click/keypress that may need
     *                            time to open a dialog.
     * @return detected dialog type; {@link DialogType#NONE} means no known dialog frame matched.
     */
    public DialogType detectDialogTypeNoFocus(String reason, boolean hidePlayerNames, long waitBeforeCaptureMs) {
        return detectDialogSnapshotDirect(reason, hidePlayerNames, waitBeforeCaptureMs).type();
    }

    /**
     * Capture and classify the current dialog area without focusing the game window.
     *
     * @param reason diagnostic label used in logs and debug screenshot names.
     * @param hidePlayerNames whether to hide player-name overlays before the dialog capture.
     * @param waitBeforeCaptureMs optional wait before capture, in milliseconds.
     * @return in-memory dialog detection owned by the caller for immediate same-tick reuse.
     */
    public DialogDetection detectDialogSnapshotNoFocus(String reason,
                                                       boolean hidePlayerNames,
                                                       long waitBeforeCaptureMs) {
        return detectDialogSnapshotDirect(reason, hidePlayerNames, waitBeforeCaptureMs);
    }

    private DialogDetection detectDialogSnapshotDirect(String reason) {
        return detectDialogSnapshotDirect(reason, true);
    }

    private DialogDetection detectDialogSnapshotDirect(String reason, boolean hidePlayerNames) {
        return detectDialogSnapshotDirect(reason, hidePlayerNames, 700 + random.nextInt(100));
    }

    private DialogDetection detectDialogSnapshotDirect(String reason, boolean hidePlayerNames, long waitBeforeCaptureMs) {
        long latencyStart = LatencyMetrics.start();
        DialogDetection detection = DialogDetection.none();
        try {
            if (waitBeforeCaptureMs > 0 && !TaskSleep.sleep(waitBeforeCaptureMs)) {
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

    private Optional<DialogDetection> usableSuppliedDialogDetection(DialogDetection suppliedDetection,
                                                                    DialogType requiredType,
                                                                    String source) {
        if (suppliedDetection == null || suppliedDetection.image() == null
                || suppliedDetection.dialogRect() == null) {
            return Optional.empty();
        }
        if (requiredType != null && suppliedDetection.type() != requiredType) {
            log.info("dialog supplied detection rejected: source={} requiredType={} actualType={} hasImage={}",
                    source, requiredType, suppliedDetection.type(), true);
            return Optional.empty();
        }
        log.info("dialog supplied detection reused: source={} type={} raw={}",
                source, suppliedDetection.type(), suppliedDetection.rawPath());
        return Optional.of(suppliedDetection);
    }

    private Optional<DialogDetection> usableSuppliedStoryDetection(DialogDetection suppliedDetection,
                                                                   boolean absentAllowed,
                                                                   String source) {
        if (suppliedDetection == null || suppliedDetection.image() == null
                || suppliedDetection.dialogRect() == null) {
            return Optional.empty();
        }
        if (suppliedDetection.type() == DialogType.STORY
                || (absentAllowed && suppliedDetection.type() == DialogType.NONE)) {
            log.info("dialog supplied story detection reused: source={} type={} absentAllowed={} raw={}",
                    source, suppliedDetection.type(), absentAllowed, suppliedDetection.rawPath());
            return Optional.of(suppliedDetection);
        }
        log.info("dialog supplied story detection rejected: source={} actualType={} absentAllowed={}",
                source, suppliedDetection.type(), absentAllowed);
        return Optional.empty();
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
            if (!InputActionScope.checkpoint()) {
                return;
            }
            inputProvider.pressAlt4();
            if (!TaskSleep.sleep(HIDE_PLAYER_NAMES_SETTLE_MS)) {
                return;
            }
            InputActionScope.checkpoint();
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
        cloudWashToPath(reason, "dialog-option-lower-green-debug",
                optionRawPath, optionGreenPath, ImagePreprocessOperation.WASH_GREEN);
        ImageProcessorService.ImageProcessorResult greenCountResult = imageProcessorService.countGreenPixelsHSV(
                frame, imageProcessorMetadata(reason, "dialog-option-lower-green-count", optionRawPath));
        if (greenCountResult == null || !greenCountResult.hasPixelCount()) {
            log.debug("dialog option lower check skipped: reason={} status={} cause=green-count-unavailable",
                    reason, greenCountResult == null ? null : greenCountResult.status());
            frame.flush();
            return false;
        }
        int greenCount = greenCountResult.pixelCount();
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

        ImageProcessorService.ImageProcessorResult stddevResult = imageProcessorService.measureStddev(
                frame, imageProcessorMetadata("dialog-mask", "dialog-mask-stddev", null));
        if (stddevResult == null || stddevResult.stddev() == null) {
            log.debug("dialog mask stddev unavailable: status={}", stddevResult == null ? null : stddevResult.status());
            frame.flush();
            return false;
        }
        double stddev = stddevResult.stddev();
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
        cloudWashToPath(reason, "dialog-story-upper-white-debug",
                storyRawPath, storyWhitePath, ImagePreprocessOperation.WASH_WHITE);
        cloudWashToPath(reason, "dialog-story-upper-green-debug",
                storyRawPath, storyGreenPath, ImagePreprocessOperation.WASH_GREEN);
        ImageProcessorService.ImageProcessorResult thinWhiteCountResult = imageProcessorService.countThinWhitePixelsHSV(
                frame, imageProcessorMetadata(reason, "dialog-story-upper-thin-white-count", storyRawPath));
        ImageProcessorService.ImageProcessorResult greenCountResult = imageProcessorService.countGreenPixelsHSV(
                frame, imageProcessorMetadata(reason, "dialog-story-upper-green-count", storyRawPath));
        ImageProcessorService.ImageProcessorResult textLineResult = imageProcessorService.detectThinWhiteTextLinePattern(
                frame, imageProcessorMetadata(reason, "dialog-story-upper-text-line-pattern", storyRawPath));
        if (thinWhiteCountResult == null || !thinWhiteCountResult.hasPixelCount()
                || greenCountResult == null || !greenCountResult.hasPixelCount()
                || textLineResult == null || textLineResult.textLinePatternStats() == null) {
            log.debug("dialog story upper check skipped: reason={} thinWhiteStatus={} greenStatus={} textLineStatus={} cause=image-preprocess-unavailable",
                    reason,
                    thinWhiteCountResult == null ? null : thinWhiteCountResult.status(),
                    greenCountResult == null ? null : greenCountResult.status(),
                    textLineResult == null ? null : textLineResult.status());
            frame.flush();
            return false;
        }
        int thinWhiteCount = thinWhiteCountResult.pixelCount();
        int greenCount = greenCountResult.pixelCount();
        ImageProcessorService.TextLinePatternStats textLineStats = textLineResult.textLinePatternStats();
        frame.flush();

        int totalTextPixels = thinWhiteCount + greenCount;
        // A real story prompt has multiple dense glyph rows. Scene highlights and white clothing can
        // produce a few thin-white rows, so keep this stricter than the row-shape helper's baseline.
        boolean story = totalTextPixels >= STORY_MIN_TEXT_PIXELS
                && textLineStats.qualifyingRows() >= STORY_MIN_TEXT_ROWS
                && textLineStats.maxWhitePixelsInRow() >= STORY_MIN_MAX_ROW_WHITE
                && textLineStats.maxClustersInRow() >= STORY_MIN_MAX_CLUSTERS
                && textLineStats.maxSpanInRow() >= STORY_MIN_MAX_SPAN;
        if (story) {
            log.info("dialog story upper check: reason={} rect={} raw={} thinWhite={} green={} total={} textRows={} maxRowWhite={} maxClusters={} maxSpan={} whitePath={} greenPath={} result={}",
                    reason, ImagePreprocessor.rectToString(area), storyRawPath, thinWhiteCount, greenCount,
                    totalTextPixels, textLineStats.qualifyingRows(), textLineStats.maxWhitePixelsInRow(),
                    textLineStats.maxClustersInRow(), textLineStats.maxSpanInRow(), storyWhitePath, storyGreenPath, true);
        } else {
            log.debug("dialog story upper check: reason={} rect={} raw={} thinWhite={} green={} total={} textRows={} maxRowWhite={} maxClusters={} maxSpan={} whitePath={} greenPath={} result={}",
                    reason, ImagePreprocessor.rectToString(area), storyRawPath, thinWhiteCount, greenCount,
                    totalTextPixels, textLineStats.qualifyingRows(), textLineStats.maxWhitePixelsInRow(),
                    textLineStats.maxClustersInRow(), textLineStats.maxSpanInRow(), storyWhitePath, storyGreenPath, false);
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

    private Optional<PreparedDialogAction> buildTemplatePreparedDialogAction(String source,
                                                                            DialogOperation operation,
                                                                            String targetKeyword,
                                                                            String matchedText,
                                                                            Integer relativeX,
                                                                            Integer relativeY,
                                                                            Integer absoluteX,
                                                                            Integer absoluteY,
                                                                            DialogDetection detection,
                                                                            String washVariant,
                                                                            String washedPath,
                                                                            boolean clickRequired) {
        if (detection == null || detection.dialogRect() == null
                || relativeX == null || relativeY == null
                || absoluteX == null || absoluteY == null) {
            return Optional.empty();
        }
        BufferedImage washed = ImagePreprocessor.pathToBufferedImage(washedPath);
        if (washed == null) {
            return Optional.empty();
        }
        try {
            int[] localRect = buildRememberedValidationLocalRect(relativeX, relativeY,
                    washed.getWidth(), washed.getHeight());
            if (localRect == null) {
                return Optional.empty();
            }
            BufferedImage crop = ImagePreprocessor.cropCopy(
                    washed,
                    localRect[0],
                    localRect[1],
                    localRect[2] - localRect[0],
                    localRect[3] - localRect[1]);
            if (crop == null) {
                return Optional.empty();
            }
            try {
                String fingerprint = cloudBinaryFingerprint(
                        crop, source, "dialog-template-prepared-fingerprint");
                if (fingerprint == null || fingerprint.isBlank()) {
                    return Optional.empty();
                }
                int[] dialogRect = detection.dialogRect();
                long now = System.currentTimeMillis();
                return Optional.of(PreparedDialogAction.builder()
                        .dialogType(detection.type())
                        .operation(operation)
                        .targetKeyword(targetKeyword)
                        .matchedText(matchedText)
                        .relativeX(relativeX)
                        .relativeY(relativeY)
                        .absoluteX(absoluteX)
                        .absoluteY(absoluteY)
                        .validationLeft(dialogRect[0] + localRect[0])
                        .validationTop(dialogRect[1] + localRect[1])
                        .validationRight(dialogRect[0] + localRect[2])
                        .validationBottom(dialogRect[1] + localRect[3])
                        .washMode(resolveFingerprintWashMode(washVariant))
                        .fingerprint(fingerprint)
                        .clickRequired(clickRequired)
                        .preparedAtMs(now)
                        .lastVerifiedAtMs(now)
                        .source(source)
                        .debugImagePath(washedPath)
                        .build());
            } finally {
                crop.flush();
            }
        } finally {
            washed.flush();
        }
    }

    private Optional<PreparedDialogAction> prepareCloudDialogAction(DialogHandleRequest request,
                                                                   DialogOperation operation,
                                                                   DialogDetection detection,
                                                                   String phase) {
        DialogPolicyPreClickCloudRequest cloudRequest = buildDialogPreClickCloudRequest(request, detection);
        if (cloudRequest == null) {
            return Optional.empty();
        }
        DialogPolicyPreClickCloudDecision decision = dialogPolicyCloudDecisionService.decidePreClick(cloudRequest);
        if (!decision.isCloudExecuted()) {
            log.warn("{} cloud no executable action: source={} operation={} status={} action={} reason={}",
                    phase, request.getSourceTask(), operation, decision.getStatus(),
                    decision.getAction(), decision.getReason());
            return Optional.empty();
        }
        return buildCloudPreparedDialogAction(request.getSourceTask(), operation, detection, cloudRequest, decision);
    }

    private Optional<PreparedDialogAction> prepareCloudDialogOptionAction(DialogHandleRequest request,
                                                                         DialogOperation operation,
                                                                         DialogDetection suppliedDetection,
                                                                         String phase) {
        long startedAt = System.currentTimeMillis();
        DialogDetection detection = usableSuppliedDialogDetection(
                suppliedDetection, request.isVerifyDialogType() ? DialogType.OPTION : null,
                phase + ":" + request.getSourceTask())
                .orElseGet(() -> detectDialogSnapshotDirect(phase + ":" + request.getSourceTask(), false, 0));
        boolean typeAccepted = !request.isVerifyDialogType()
                || detection != null && detection.type() == DialogType.OPTION;
        if (detection == null || !typeAccepted || detection.image() == null || detection.dialogRect() == null) {
            log.info("{} cloud miss before request: source={} operation={} type={} hasImage={} totalMs={}",
                    phase, request.getSourceTask(), operation,
                    detection == null ? null : detection.type(),
                    detection != null && detection.image() != null,
                    Math.max(0L, System.currentTimeMillis() - startedAt));
            return Optional.empty();
        }
        Optional<PreparedDialogAction> prepared = prepareCloudDialogAction(request, operation, detection, phase);
        log.info("{} cloud result: source={} operation={} target={} prepared={} totalMs={}",
                phase, request.getSourceTask(), operation, request.getTargetKeyword(), prepared.isPresent(),
                Math.max(0L, System.currentTimeMillis() - startedAt));
        return prepared;
    }

    private Optional<PreparedDialogAction> buildCloudPreparedDialogAction(String source,
                                                                          DialogOperation operation,
                                                                          DialogDetection detection,
                                                                          DialogPolicyPreClickCloudRequest cloudRequest,
                                                                          DialogPolicyPreClickCloudDecision decision) {
        if (detection == null || detection.dialogRect() == null || detection.image() == null
                || cloudRequest == null || cloudRequest.getRoi() == null
                || decision == null || !decision.isCloudExecuted()) {
            return Optional.empty();
        }
        Point clickRel = decision.getWindowRelativeClickPoint();
        int relativeX = clickRel.x - cloudRequest.getRoi().getX();
        int relativeY = clickRel.y - cloudRequest.getRoi().getY();
        BufferedImage raw = detection.image();
        int[] localRect = buildRememberedValidationLocalRect(relativeX, relativeY, raw.getWidth(), raw.getHeight());
        if (localRect == null) {
            return Optional.empty();
        }
        BufferedImage washed = cloudWashImage(
                raw,
                DialogFingerprintWashMode.TEMPLATE_SPECIFIC,
                source,
                "dialog-cloud-prepared-wash");
        if (washed == null) {
            return Optional.empty();
        }
        try {
            BufferedImage crop = ImagePreprocessor.cropCopy(
                    washed,
                    localRect[0],
                    localRect[1],
                    localRect[2] - localRect[0],
                    localRect[3] - localRect[1]);
            if (crop == null) {
                return Optional.empty();
            }
            try {
                String fingerprint = cloudBinaryFingerprint(
                        crop, source, "dialog-cloud-prepared-fingerprint");
                if (fingerprint == null || fingerprint.isBlank()) {
                    return Optional.empty();
                }
                int[] dialogRect = detection.dialogRect();
                long now = System.currentTimeMillis();
                return Optional.of(PreparedDialogAction.builder()
                        .windowId(cloudRequest.getWindowId())
                        .hwnd(cloudRequest.getHwnd())
                        .dialogType(detection.type())
                        .operation(operation)
                        .targetKeyword(decision.getActionId())
                        .matchedText("cloud-prepared:" + (decision.getReason() == null ? "" : decision.getReason()))
                        .relativeX(relativeX)
                        .relativeY(relativeY)
                        .absoluteX(tracker.getWindowBaseX() + clickRel.x)
                        .absoluteY(tracker.getWindowBaseY() + clickRel.y)
                        .validationLeft(dialogRect[0] + localRect[0])
                        .validationTop(dialogRect[1] + localRect[1])
                        .validationRight(dialogRect[0] + localRect[2])
                        .validationBottom(dialogRect[1] + localRect[3])
                        .washMode(DialogFingerprintWashMode.TEMPLATE_SPECIFIC)
                        .fingerprint(fingerprint)
                        .clickRequired(true)
                        .preparedAtMs(now)
                        .lastVerifiedAtMs(now)
                        .source(source + ":cloud-prepared")
                        .debugImagePath(detection.rawPath())
                        .build());
            } finally {
                crop.flush();
            }
        } finally {
            washed.flush();
        }
    }

    private Optional<PreparedDialogAction> buildCloudSemanticPreparedDialogAction(String source,
                                                                                  DialogOperation operation,
                                                                                  DialogDetection detection,
                                                                                  DialogPolicyPreClickCloudRequest cloudRequest,
                                                                                  DialogPolicyPreClickCloudDecision decision) {
        if (detection == null || detection.dialogRect() == null || detection.image() == null
                || cloudRequest == null || decision == null
                || decision.getStatus() != DialogPolicyPreClickCloudDecision.Status.CLOUD_NO_ACTION
                || decision.getAction() != DialogPolicyPreClickCloudDecision.Action.NO_ACTION
                || decision.getActionId() == null || decision.getActionId().isBlank()) {
            return Optional.empty();
        }
        int[] candidateBox = parseWindowRelativeBox(decision.getCandidateBox());
        if (candidateBox == null) {
            return Optional.empty();
        }
        int[] dialogRect = detection.dialogRect();
        int validationLeft = tracker.getWindowBaseX() + candidateBox[0];
        int validationTop = tracker.getWindowBaseY() + candidateBox[1];
        int validationRight = tracker.getWindowBaseX() + candidateBox[0] + candidateBox[2];
        int validationBottom = tracker.getWindowBaseY() + candidateBox[1] + candidateBox[3];
        int absoluteX = validationLeft + candidateBox[2] / 2;
        int absoluteY = validationTop + candidateBox[3] / 2;
        long now = System.currentTimeMillis();
        boolean storyAbsent = decision.getActionId().equals(cloudRequest.getDialogRequest().getStoryAbsentTargetKeyword());
        return Optional.of(PreparedDialogAction.builder()
                .windowId(cloudRequest.getWindowId())
                .hwnd(cloudRequest.getHwnd())
                .dialogType(storyAbsent ? DialogType.NONE : detection.type())
                .operation(operation)
                .targetKeyword(decision.getActionId())
                .matchedText(decision.getMatchedText() == null || decision.getMatchedText().isBlank()
                        ? "cloud-semantic:" + (decision.getReason() == null ? "" : decision.getReason())
                        : decision.getMatchedText())
                .relativeX(Math.max(0, absoluteX - dialogRect[0]))
                .relativeY(Math.max(0, absoluteY - dialogRect[1]))
                .absoluteX(absoluteX)
                .absoluteY(absoluteY)
                .validationLeft(validationLeft)
                .validationTop(validationTop)
                .validationRight(validationRight)
                .validationBottom(validationBottom)
                .washMode(DialogFingerprintWashMode.WHITE)
                .fingerprint("")
                .clickRequired(false)
                .preparedAtMs(now)
                .lastVerifiedAtMs(now)
                .source(source + ":cloud-semantic")
                .debugImagePath(detection.rawPath())
                .build());
    }

    private static int[] parseWindowRelativeBox(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 4) {
            return null;
        }
        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int width = Integer.parseInt(parts[2].trim());
            int height = Integer.parseInt(parts[3].trim());
            return width > 0 && height > 0 ? new int[]{x, y, width, height} : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> greenTemplateSpecNames(DialogHandleRequest request) {
        if (request == null || request.getGreenTemplateSpecs() == null || request.getGreenTemplateSpecs().isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (GreenTemplateClickSpec spec : request.getGreenTemplateSpecs()) {
            if (spec != null && spec.name() != null && !spec.name().isBlank()) {
                names.add(spec.name());
            }
        }
        return names;
    }

    private DialogFingerprintWashMode resolveFingerprintWashMode(String variantName) {
        if ("yellow".equalsIgnoreCase(variantName)) {
            return DialogFingerprintWashMode.YELLOW;
        }
        if ("green".equalsIgnoreCase(variantName)) {
            return DialogFingerprintWashMode.GREEN;
        }
        if ("white".equalsIgnoreCase(variantName)) {
            return DialogFingerprintWashMode.WHITE;
        }
        return DialogFingerprintWashMode.TEMPLATE_SPECIFIC;
    }

    private Optional<PreparedDialogAction> prepareXiuluoEnterBattleLocalTemplate(DialogHandleRequest request,
                                                                                 DialogOperation operation) {
        Optional<LocalDialogTemplateMatch> matched = findXiuluoEnterBattleLocalTemplate(
                request.getSourceTask(), "prepare");
        if (matched.isEmpty()) {
            log.info("dialog prepare xiuluo enter-battle local template miss: reason={} operation={} template={} roi=({}, {})-({}, {})",
                    request.getSourceTask(), operation, XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE,
                    XIULUO_ENTER_BATTLE_LOCAL_ROI_LEFT, XIULUO_ENTER_BATTLE_LOCAL_ROI_TOP,
                    XIULUO_ENTER_BATTLE_LOCAL_ROI_RIGHT, XIULUO_ENTER_BATTLE_LOCAL_ROI_BOTTOM);
            return Optional.empty();
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        LocalDialogTemplateMatch match = matched.get();
        long now = System.currentTimeMillis();
        PreparedDialogAction action = PreparedDialogAction.builder()
                .windowId(runtime == null ? null : runtime.getWindowId())
                .hwnd(runtime == null || runtime.getNativeBinding() == null
                        ? null
                        : runtime.getNativeBinding().getNativeHandle())
                .dialogType(DialogType.OPTION)
                .operation(operation)
                .targetKeyword(firstGreenTemplateSpecName(request))
                .matchedText("local-template:" + XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE)
                .relativeX(match.absoluteX() - match.rect()[0])
                .relativeY(match.absoluteY() - match.rect()[1])
                .absoluteX(match.absoluteX())
                .absoluteY(match.absoluteY())
                .validationLeft(match.rect()[0])
                .validationTop(match.rect()[1])
                .validationRight(match.rect()[2])
                .validationBottom(match.rect()[3])
                .washMode(DialogFingerprintWashMode.TEMPLATE_SPECIFIC)
                .fingerprint("local-template:" + XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE)
                .clickRequired(true)
                .preparedAtMs(now)
                .lastVerifiedAtMs(now)
                .source(request.getSourceTask() + ":local-kanda")
                .debugImagePath(XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE)
                .build();
        log.info("dialog prepare xiuluo enter-battle local template action: reason={} operation={} target={} "
                        + "template={} score={} click=({}, {}) roi=({}, {})-({}, {})",
                request.getSourceTask(), operation, action.getTargetKeyword(), XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE,
                String.format("%.4f", match.score()), match.absoluteX(), match.absoluteY(),
                match.rect()[0], match.rect()[1], match.rect()[2], match.rect()[3]);
        return Optional.of(action);
    }

    private Optional<LocalDialogTemplateMatch> findXiuluoEnterBattleLocalTemplate(String source, String phase) {
        int[] rect = coordinateHelper.getScaledRect(
                XIULUO_ENTER_BATTLE_LOCAL_ROI_LEFT,
                XIULUO_ENTER_BATTLE_LOCAL_ROI_TOP,
                XIULUO_ENTER_BATTLE_LOCAL_ROI_RIGHT - XIULUO_ENTER_BATTLE_LOCAL_ROI_LEFT,
                XIULUO_ENTER_BATTLE_LOCAL_ROI_BOTTOM - XIULUO_ENTER_BATTLE_LOCAL_ROI_TOP);
        BufferedImage roi = tracker.captureToMemory(
                "xiuluo-enter-battle-local-template:" + safeDebugName(source) + ":" + safeDebugName(phase),
                rect[0], rect[1], rect[2], rect[3]);
        BufferedImage template = ImagePreprocessor.pathToBufferedImage(XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE);
        if (roi == null || template == null) {
            if (roi != null) {
                roi.flush();
            }
            log.warn("dialog xiuluo enter-battle local template unavailable: source={} phase={} templateLoaded={} roiCaptured={}",
                    source, phase, template != null, roi != null);
            return Optional.empty();
        }
        try {
            double[] result = ImageFinder.find(roi, template, XIULUO_ENTER_BATTLE_LOCAL_MATCH_RATE);
            if (result == null || result.length < 3) {
                return Optional.empty();
            }
            int absoluteX = rect[0] + (int) Math.round(result[0]);
            int absoluteY = rect[1] + (int) Math.round(result[1]);
            return Optional.of(new LocalDialogTemplateMatch(rect, absoluteX, absoluteY, result[2]));
        } finally {
            roi.flush();
            template.flush();
        }
    }

    private boolean isXiuluoEnterBattleTemplateRequest(DialogHandleRequest request) {
        return request != null
                && request.getOptionPolicy() == DialogOptionPolicy.CLICK_GREEN_TEMPLATE
                && greenTemplateSpecNames(request).contains(XIULUO_ENTER_BATTLE_ACTION_KEY);
    }

    /**
     * CR256: true only for the local kanda template hit — the prepared action whose matched text
     * carries the local 看打 template hard evidence and whose consume-time validation re-runs the
     * live template match. Only such an action is a pre-authorized physical execution command for
     * the direct wake-consume path; any other XIULUO_ENTER_BATTLE prepared shape must go through
     * the normal phase re-entry.
     */
    public boolean isXiuluoEnterBattleLocalKandaAction(PreparedDialogAction action) {
        return isXiuluoEnterBattleLocalTemplateAction(action);
    }

    private boolean isXiuluoEnterBattleLocalTemplateAction(PreparedDialogAction action) {
        return action != null
                && action.getOperation() == DialogOperation.XIULUO_ENTER_BATTLE
                && XIULUO_ENTER_BATTLE_ACTION_KEY.equals(action.getTargetKeyword())
                && action.getMatchedText() != null
                && action.getMatchedText().contains(XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE);
    }

    private String firstGreenTemplateSpecName(DialogHandleRequest request) {
        List<String> names = greenTemplateSpecNames(request);
        return names.isEmpty() ? null : names.get(0);
    }

    private Optional<PreparedDialogAction> prepareGreenTemplateOption(DialogHandleRequest request,
                                                                      DialogOperation operation) {
        return prepareGreenTemplateOption(request, operation, null, null);
    }

    private Optional<PreparedDialogAction> prepareGreenTemplateOption(DialogHandleRequest request,
                                                                      DialogOperation operation,
                                                                      String missTargetKeyword) {
        return prepareGreenTemplateOption(request, operation, missTargetKeyword, null);
    }

    private Optional<PreparedDialogAction> prepareGreenTemplateOption(DialogHandleRequest request,
                                                                      DialogOperation operation,
                                                                      String missTargetKeyword,
                                                                      DialogDetection suppliedDetection) {
        long latencyStart = LatencyMetrics.start();
        try {
            if (operation == DialogOperation.XIULUO_ENTER_BATTLE) {
                return prepareXiuluoEnterBattleLocalTemplate(request, operation);
            }
            List<GreenTemplateClickSpec> specs = request.getGreenTemplateSpecs();
            if (specs == null || specs.isEmpty()) {
                log.warn("dialog prepare green template requested without specs: reason={}",
                        request.getSourceTask());
                return Optional.empty();
            }

            DialogType type = DialogType.OPTION;
            DialogDetection detection = null;
            if (request.isVerifyDialogType()) {
                detection = usableSuppliedDialogDetection(
                        suppliedDetection, DialogType.OPTION, "green-template-prepare:" + request.getSourceTask())
                        .orElseGet(() -> detectDialogSnapshotDirect(
                                "green-template-prepare:" + request.getSourceTask(), false, 0));
                type = detection.type();
                if (type != DialogType.OPTION) {
                    log.info("dialog prepare green template skipped: reason={} operation={} type={}",
                            request.getSourceTask(), operation, type);
                    return Optional.empty();
                }
            } else {
                int[] rect = getDialogRect();
                BufferedImage image = tracker.captureToMemory(
                        "dialog-green-prepare-cloud:" + request.getSourceTask(),
                        rect[0], rect[1], rect[2], rect[3]);
                if (image == null) {
                    log.warn("dialog prepare green template capture failed: reason={} operation={}",
                            request.getSourceTask(), operation);
                    return Optional.empty();
                }
                String rawPath = windowScopedTempPath.resolve("dialog_green_prepare_raw.png");
                if (!ImagePreprocessor.saveImage(image, rawPath)) {
                    rawPath = null;
                }
                detection = DialogDetection.builder()
                        .type(DialogType.OPTION)
                        .dialogRect(rect)
                        .rawPath(rawPath)
                        .image(image)
                        .build();
            }

            if (detection == null || detection.image() == null || detection.dialogRect() == null) {
                log.warn("dialog prepare green template payload unavailable: reason={} operation={} type={}",
                        request.getSourceTask(), operation, type);
                return Optional.empty();
            }
            DialogPolicyPreClickCloudRequest cloudRequest = buildDialogPreClickCloudRequest(request, detection);
            if (cloudRequest == null) {
                return Optional.empty();
            }
            DialogPolicyPreClickCloudDecision decision = dialogPolicyCloudDecisionService.decidePreClick(cloudRequest);
            if (!decision.isCloudExecuted()) {
                log.warn("dialog prepare green template cloud no executable action: reason={} operation={} status={} action={} cloudReason={} missTarget={} specs={}",
                        request.getSourceTask(), operation, decision.getStatus(), decision.getAction(),
                        decision.getReason(), missTargetKeyword, specs.size());
                return Optional.empty();
            }
            Optional<PreparedDialogAction> prepared = buildCloudPreparedDialogAction(
                    request.getSourceTask(), operation, detection, cloudRequest, decision);
            Point clickRel = decision.getWindowRelativeClickPoint();
            log.info("dialog prepare green template cloud action: reason={} operation={} actionId={} decisionId={} clickRel=({}, {}) candidateBox={} prepared={}",
                    request.getSourceTask(), operation, decision.getActionId(), decision.getDecisionId(),
                    clickRel.x, clickRel.y, decision.getCandidateBox(), prepared.isPresent());
            return prepared;
        } finally {
            LatencyMetrics.info(log, "dialog.prepareGreenTemplate", latencyStart,
                    "operation=" + operation
                            + " reason=" + request.getSourceTask()
                            + " specCount=" + (request.getGreenTemplateSpecs() == null ? 0 : request.getGreenTemplateSpecs().size()));
        }
    }

    public BufferedImage captureCurrentStoryImage(String reason) {
        DialogType type = detectDialogTypeNoFocus("capture-story-image:" + reason, false, 0);
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

    /**
     * Capture the small story-objective area from the current bound window without classifying the
     * dialog or sending input.
     *
     * @param reason diagnostic label used for capture/debug image names.
     * @return cropped story objective image owned by the caller, or null when screenshot/crop fails.
     */
    public BufferedImage captureCurrentStoryObjectiveSnapshotNoDetect(String reason) {
        int[] dialogRect = getDialogRect();
        BufferedImage image = tracker.captureToMemory("story-objective-snapshot-" + reason,
                dialogRect[0], dialogRect[1], dialogRect[2], dialogRect[3]);
        if (image == null) {
            log.warn("dialog story objective snapshot capture failed: reason={}", reason);
            return null;
        }
        BufferedImage objectiveImage = ImagePreprocessor.cropAbsoluteRect(image, dialogRect, getSmallDialogRect());
        if (objectiveImage == null) {
            image.flush();
            log.warn("dialog story objective snapshot crop failed: reason={}", reason);
            return null;
        }
        if (objectiveImage != image) {
            image.flush();
        }
        saveStoryObjectiveDebugImage(reason, objectiveImage);
        log.info("dialog story objective snapshot captured without detect: reason={} size={}x{}",
                reason, objectiveImage.getWidth(), objectiveImage.getHeight());
        return objectiveImage;
    }

    /**
     * Crops the story-objective area from a saved full game-window snapshot.
     *
     * @param windowSnapshot full current game-window image, in window-local pixels.
     * @param windowBaseX screen-absolute X coordinate of the snapshot's left edge.
     * @param windowBaseY screen-absolute Y coordinate of the snapshot's top edge.
     * @param reason diagnostic label used for debug image names.
     * @return cropped story objective image owned by the caller, or null when the crop is invalid.
     */
    public BufferedImage cropStoryObjectiveFromWindowSnapshotNoDetect(BufferedImage windowSnapshot,
                                                                      int windowBaseX,
                                                                      int windowBaseY,
                                                                      String reason) {
        if (windowSnapshot == null) {
            return null;
        }
        int[] smallRect = getSmallDialogRect();
        int left = smallRect[0] - windowBaseX;
        int top = smallRect[1] - windowBaseY;
        int width = smallRect[2] - smallRect[0];
        int height = smallRect[3] - smallRect[1];
        BufferedImage objectiveImage = ImagePreprocessor.cropCopy(windowSnapshot, left, top, width, height);
        if (objectiveImage == null) {
            log.warn("dialog story objective snapshot crop failed: reason={} windowBase=({}, {}) local=({}, {}) {}x{} snapshot={}x{}",
                    reason, windowBaseX, windowBaseY, left, top, width, height,
                    windowSnapshot.getWidth(), windowSnapshot.getHeight());
            return null;
        }
        saveStoryObjectiveDebugImage(reason, objectiveImage);
        log.info("dialog story objective cropped from accept snapshot: reason={} local=({}, {}) size={}x{}",
                reason, left, top, objectiveImage.getWidth(), objectiveImage.getHeight());
        return objectiveImage;
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

    private int[] getDialogRect() {
        return coordinateHelper.getScaledRect(DIALOG_LARGE_X, DIALOG_LARGE_Y, DIALOG_LARGE_W, DIALOG_LARGE_H);
    }

    private int[] getSmallDialogRect() {
        return coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, DIALOG_SMALL_H);
    }

    private record LocalDialogTemplateMatch(int[] rect, int absoluteX, int absoluteY, double score) {
    }

}
