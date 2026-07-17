package com.bot.dhxy.service.dialog;

import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

/**
 * Exact-window local mechanics for the committed {@code 696a12b0} no-focus dialog detection. It
 * reproduces {@code DialogService.detectDialogSnapshotDirect}/{@code captureDialogSnapshot} and the
 * three classification helpers as one pure-local closed operation: optional player-name hide (Alt+4),
 * a single dialog-area capture, and the fixed {@code mask stddev -> lower-half green option ->
 * upper-half thin-white/green row pattern} order.
 *
 * <p>All pixel classification runs on the single captured frame through the committed pure-CPU/OpenCV
 * {@link ImagePreprocessor} helpers (no Cloud-routable {@code ImageProcessorService}). Option/story,
 * business and fallback decisions remain in the Cloud caller; this entry only captures and classifies.
 * Every terminal is a closed {@link State}: input, capture, classification and encoding exceptions all
 * resolve to {@link State#MECHANICS_FAILED}; only an absent captured frame is
 * {@link State#CAPTURE_UNAVAILABLE}; a pre-capture interrupt and a non-input-worker hide have their own
 * closed states, never a thrown half-open contract.</p>
 */
@Slf4j
@Service
public final class DialogDetectionLocalMechanics {

    // Committed dialog geometry (window-client pixels) from DialogService.
    private static final int DIALOG_LARGE_X = 250;
    private static final int DIALOG_LARGE_Y = 312;
    private static final int DIALOG_LARGE_W = 529;
    private static final int DIALOG_LARGE_H = 208;
    private static final int DIALOG_SMALL_X = 250;
    private static final int DIALOG_SMALL_Y = 345;
    private static final int DIALOG_SMALL_W = 529;
    private static final int DIALOG_SMALL_H = 143;
    private static final int CROP_TOP_Y = 42;
    private static final int CROP_DEV_Y = 58;
    private static final int CROP_LEFT_X = 161;
    private static final int HIDE_PLAYER_NAMES_SETTLE_MS = 220;
    private static final double DIALOG_MASK_STDDEV_MAX = 30.0;
    private static final int OPTION_MIN_GREEN = 150;
    private static final int STORY_MIN_TEXT_PIXELS = 450;
    private static final int STORY_MIN_TEXT_ROWS = 10;
    private static final int STORY_MIN_MAX_ROW_WHITE = 40;
    private static final int STORY_MIN_MAX_CLUSTERS = 20;
    private static final int STORY_MIN_MAX_SPAN = 120;
    private static final String INPUT_WORKER_THREAD_NAME_TOKEN = "dhxy-input-action-worker";

    private final BoundWindowCaptureService captureService;
    private final InputProvider inputProvider;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowNativeBindingRefreshService bindingRefreshService;

    public DialogDetectionLocalMechanics(BoundWindowCaptureService captureService,
                                         InputProvider inputProvider,
                                         WindowScopedTempPath windowScopedTempPath,
                                         WindowNativeBindingRefreshService bindingRefreshService) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.windowScopedTempPath = Objects.requireNonNull(windowScopedTempPath, "windowScopedTempPath");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
    }

    /**
     * Capture and classify the committed dialog area of one caller-supplied exact native-window binding.
     *
     * <p>Baseline order: optional {@code waitBeforeCaptureMs} sleep, then (when {@code hidePlayerNames})
     * the Alt+4 hide plus its settle, then a single dialog-area frame, then {@code mask -> option ->
     * story} classification. When {@code hidePlayerNames} is requested the Alt+4 keypress makes this a
     * real input operation, so it must run inside the already-held input worker; a non-input-worker
     * caller that needs the hide returns {@link State#NON_INPUT_WORKER}. No wait/hide adds any retry/TTL.</p>
     *
     * @param binding             exact binding; screen-absolute origin, native-handle backed geometry
     * @param hidePlayerNames     whether to send Alt+4 before capture
     * @param waitBeforeCaptureMs optional pre-capture wait in milliseconds; {@code 0} for a pure probe
     * @param source              caller label for diagnostics and window-scoped debug output
     * @return a non-null closed result; only {@link State#CAPTURED} carries the dialog type, screen rect,
     *         frame image and metrics
     */
    public DialogDetectionResult detectDialog(WindowNativeBinding binding,
                                              boolean hidePlayerNames,
                                              long waitBeforeCaptureMs,
                                              String source) {
        String safeSource = safeSource(source);
        if (binding == null
                || !binding.hasNativeHandle()
                || !binding.hasGeometry()
                || binding.getWidth() < DIALOG_LARGE_X + DIALOG_LARGE_W
                || binding.getHeight() < DIALOG_LARGE_Y + DIALOG_LARGE_H) {
            return DialogDetectionResult.nonCaptured(State.CAPTURE_UNAVAILABLE);
        }

        // Baseline order: pre-capture wait first, then hide inside the capture step. A false sleep is a
        // real pre-capture interruption, kept distinct from a missing captured frame.
        if (waitBeforeCaptureMs > 0 && !TaskSleep.sleep(waitBeforeCaptureMs)) {
            return DialogDetectionResult.nonCaptured(State.PRE_CAPTURE_INTERRUPTED);
        }

        if (hidePlayerNames) {
            if (!isInputWorkerThread()) {
                return DialogDetectionResult.nonCaptured(State.NON_INPUT_WORKER);
            }
            try {
                inputProvider.pressAlt4();
            } catch (RuntimeException e) {
                log.warn("[dialog-detect] player-name hide input failed: source={} reason={}",
                        safeSource, e.getMessage(), e);
                return DialogDetectionResult.nonCaptured(State.MECHANICS_FAILED);
            }
            // Baseline hidePlayerNamesBeforeDialogCapture calls this settle sleep and intentionally
            // ignores its result, continuing to the single-frame capture. The pre-capture wait above
            // keeps its own PRE_CAPTURE_INTERRUPTED terminal.
            TaskSleep.sleep(HIDE_PLAYER_NAMES_SETTLE_MS);
        }

        // The window can move during the pre-capture wait or (hide=true) while the Alt+4 keypress was
        // queued. Baseline getDialogRect/captureDialogSnapshot read the current window position after the
        // wait/hide, so re-read this exact HWND's geometry now — strictly after the wait and optional Alt+4
        // settle and before the single capture — and compute the screen-absolute rect from the fresh
        // origin. A failed refresh is a closed CAPTURE_UNAVAILABLE; the single capture/classification order
        // is otherwise unchanged and no retry/TTL is added.
        Optional<WindowNativeBinding> refreshedBinding = bindingRefreshService.refreshGeometry(binding);
        if (refreshedBinding.isEmpty()) {
            return DialogDetectionResult.nonCaptured(State.CAPTURE_UNAVAILABLE);
        }
        binding = refreshedBinding.get();

        int dialogLeft;
        int dialogTop;
        int dialogRight;
        int dialogBottom;
        try {
            dialogLeft = Math.addExact(binding.getX(), DIALOG_LARGE_X);
            dialogTop = Math.addExact(binding.getY(), DIALOG_LARGE_Y);
            dialogRight = Math.addExact(dialogLeft, DIALOG_LARGE_W);
            dialogBottom = Math.addExact(dialogTop, DIALOG_LARGE_H);
        } catch (ArithmeticException e) {
            return DialogDetectionResult.nonCaptured(State.MECHANICS_FAILED);
        }
        int[] dialogRect = new int[]{dialogLeft, dialogTop, dialogRight, dialogBottom};

        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(
                    binding, binding.getX(), binding.getY(),
                    dialogLeft, dialogTop, dialogRight, dialogBottom);
        } catch (RuntimeException e) {
            log.warn("[dialog-detect] exact-window capture mechanics failed: hwnd={} title={} reason={}",
                    binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
            return DialogDetectionResult.nonCaptured(State.MECHANICS_FAILED);
        }
        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return DialogDetectionResult.nonCaptured(State.CAPTURE_UNAVAILABLE);
        }

        BufferedImage frame = captured.get().image();
        try {
            Classification classification = classify(frame, dialogRect, safeSource);
            if (classification.state != State.CAPTURED) {
                return DialogDetectionResult.nonCaptured(classification.state);
            }

            int width = frame.getWidth();
            int height = frame.getHeight();
            if (width <= 0 || height <= 0) {
                return DialogDetectionResult.nonCaptured(State.MECHANICS_FAILED);
            }
            byte[] pngBytes = pngBytes(frame);
            String sha256 = sha256Hex(pngBytes);
            return DialogDetectionResult.captured(
                    classification.dialogType,
                    dialogLeft, dialogTop, dialogRight, dialogBottom,
                    pngBytes, sha256, width, height,
                    classification.maskStddev,
                    classification.optionGreenCount,
                    classification.storyThinWhiteCount,
                    classification.storyGreenCount,
                    classification.storyTextLineStats);
        } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
            log.warn("[dialog-detect] classification/encoding mechanics failed: source={} reason={}",
                    safeSource, e.getMessage(), e);
            return DialogDetectionResult.nonCaptured(State.MECHANICS_FAILED);
        } finally {
            frame.flush();
        }
    }

    /**
     * Committed classification order on the single captured frame: mask stddev, then lower-half green
     * option, then upper-half thin-white/green story row pattern. Every owned crop is released exactly
     * once via {@code finally}; the outer frame is released by the caller.
     */
    private Classification classify(BufferedImage frame, int[] dialogRect, String safeSource) {
        // Mask: DIALOG_SMALL + (CROP_LEFT_X, CROP_DEV_Y) inset; stddev must be below the smoothness gate.
        int[] maskArea = clientArea(DIALOG_SMALL_X + CROP_LEFT_X, DIALOG_SMALL_Y + CROP_DEV_Y,
                DIALOG_SMALL_W - CROP_LEFT_X, DIALOG_SMALL_H - CROP_DEV_Y, dialogRect);
        BufferedImage maskCrop = ImagePreprocessor.cropAbsoluteRect(frame, dialogRect, maskArea);
        if (maskCrop == null) {
            return Classification.terminal(State.MECHANICS_FAILED);
        }
        double stddev;
        try {
            stddev = ImagePreprocessor.getImageStandardDeviation(maskCrop, debugPath(safeSource, "mask_stddev"));
        } finally {
            maskCrop.flush();
        }
        if (stddev >= DIALOG_MASK_STDDEV_MAX) {
            return Classification.detected(DialogType.NONE, stddev, null, null, null, null);
        }

        // Option: lower half; bright green option text above the threshold.
        int[] optionArea = clientArea(DIALOG_SMALL_X, DIALOG_SMALL_Y + CROP_TOP_Y,
                DIALOG_SMALL_W, DIALOG_SMALL_H - CROP_TOP_Y, dialogRect);
        BufferedImage optionCrop = ImagePreprocessor.cropAbsoluteRect(frame, dialogRect, optionArea);
        if (optionCrop == null) {
            return Classification.terminal(State.MECHANICS_FAILED);
        }
        int optionGreen;
        try {
            optionGreen = ImagePreprocessor.countGreenPixelsHSV(optionCrop, debugPath(safeSource, "option_lower_green"));
        } finally {
            optionCrop.flush();
        }
        if (optionGreen > OPTION_MIN_GREEN) {
            return Classification.detected(DialogType.OPTION, stddev, optionGreen, null, null, null);
        }

        // Story: upper half; dense thin-white text rows.
        int[] storyArea = clientArea(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, CROP_TOP_Y, dialogRect);
        BufferedImage storyCrop = ImagePreprocessor.cropAbsoluteRect(frame, dialogRect, storyArea);
        if (storyCrop == null) {
            return Classification.terminal(State.MECHANICS_FAILED);
        }
        int thinWhiteCount;
        int storyGreen;
        ImagePreprocessor.TextLinePatternStats textLineStats;
        try {
            thinWhiteCount = ImagePreprocessor.countThinWhitePixelsHSV(storyCrop, debugPath(safeSource, "story_upper_white"));
            storyGreen = ImagePreprocessor.countGreenPixelsHSV(storyCrop, debugPath(safeSource, "story_upper_green"));
            textLineStats = ImagePreprocessor.detectThinWhiteTextLinePattern(storyCrop);
        } finally {
            storyCrop.flush();
        }
        int totalTextPixels = thinWhiteCount + storyGreen;
        boolean story = totalTextPixels >= STORY_MIN_TEXT_PIXELS
                && textLineStats.qualifyingRows() >= STORY_MIN_TEXT_ROWS
                && textLineStats.maxWhitePixelsInRow() >= STORY_MIN_MAX_ROW_WHITE
                && textLineStats.maxClustersInRow() >= STORY_MIN_MAX_CLUSTERS
                && textLineStats.maxSpanInRow() >= STORY_MIN_MAX_SPAN;
        if (story) {
            return Classification.detected(DialogType.STORY, stddev, optionGreen,
                    thinWhiteCount, storyGreen, textLineStats);
        }
        return Classification.detected(DialogType.NONE, stddev, optionGreen,
                thinWhiteCount, storyGreen, textLineStats);
    }

    /**
     * Screen-absolute area = window base + client offset, matching the committed getScaledRect that only
     * adds the window origin (no DPI scaling), returned as {@code [left, top, right, bottom]}.
     */
    private static int[] clientArea(int offsetX, int offsetY, int width, int height, int[] dialogRect) {
        int baseX = dialogRect[0] - DIALOG_LARGE_X;
        int baseY = dialogRect[1] - DIALOG_LARGE_Y;
        int left = baseX + offsetX;
        int top = baseY + offsetY;
        return new int[]{left, top, left + width, top + height};
    }

    /** Window-scoped, per-source debug path so concurrent windows never overwrite each other. */
    private String debugPath(String safeSource, String stage) {
        return windowScopedTempPath.resolve("dialog_detect_" + safeSource + "_" + stage + ".png");
    }

    private static String safeSource(String source) {
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        String value = source.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        // Baseline safeDebugName caps the sanitized name at 120 chars so window-scoped debug filenames
        // stay writable and diagnostics are never dropped.
        return value.length() <= 120 ? value : value.substring(0, 120);
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_WORKER_THREAD_NAME_TOKEN);
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

    private static final class Classification {
        private final State state;
        private final DialogType dialogType;
        private final Double maskStddev;
        private final Integer optionGreenCount;
        private final Integer storyThinWhiteCount;
        private final Integer storyGreenCount;
        private final ImagePreprocessor.TextLinePatternStats storyTextLineStats;

        private Classification(State state, DialogType dialogType, Double maskStddev,
                               Integer optionGreenCount, Integer storyThinWhiteCount,
                               Integer storyGreenCount,
                               ImagePreprocessor.TextLinePatternStats storyTextLineStats) {
            this.state = state;
            this.dialogType = dialogType;
            this.maskStddev = maskStddev;
            this.optionGreenCount = optionGreenCount;
            this.storyThinWhiteCount = storyThinWhiteCount;
            this.storyGreenCount = storyGreenCount;
            this.storyTextLineStats = storyTextLineStats;
        }

        private static Classification detected(DialogType dialogType, Double maskStddev,
                                               Integer optionGreenCount, Integer storyThinWhiteCount,
                                               Integer storyGreenCount,
                                               ImagePreprocessor.TextLinePatternStats storyTextLineStats) {
            return new Classification(State.CAPTURED, dialogType, maskStddev, optionGreenCount,
                    storyThinWhiteCount, storyGreenCount, storyTextLineStats);
        }

        private static Classification terminal(State state) {
            return new Classification(state, null, null, null, null, null, null);
        }
    }

    public enum State {
        CAPTURED,
        CAPTURE_UNAVAILABLE,
        PRE_CAPTURE_INTERRUPTED,
        NON_INPUT_WORKER,
        MECHANICS_FAILED
    }

    public record DialogDetectionResult(
            State state,
            DialogType dialogType,
            Integer dialogLeft,
            Integer dialogTop,
            Integer dialogRight,
            Integer dialogBottom,
            byte[] framePngBytes,
            String frameSha256,
            Integer frameWidth,
            Integer frameHeight,
            Double maskStddev,
            Integer optionGreenCount,
            Integer storyThinWhiteCount,
            Integer storyGreenCount,
            ImagePreprocessor.TextLinePatternStats storyTextLineStats) {

        public DialogDetectionResult {
            Objects.requireNonNull(state, "state");
            // Defensive copy on construction so no external canonical-constructor caller can mutate the
            // stored frame bytes after the fact.
            framePngBytes = framePngBytes == null ? null : framePngBytes.clone();
            boolean captured = state == State.CAPTURED;
            boolean hasAllCoreFields = dialogType != null
                    && dialogLeft != null && dialogTop != null && dialogRight != null && dialogBottom != null
                    && framePngBytes != null && frameSha256 != null
                    && frameWidth != null && frameHeight != null
                    && maskStddev != null;
            boolean hasAnyField = dialogType != null
                    || dialogLeft != null || dialogTop != null || dialogRight != null || dialogBottom != null
                    || framePngBytes != null || frameSha256 != null
                    || frameWidth != null || frameHeight != null
                    || maskStddev != null || optionGreenCount != null
                    || storyThinWhiteCount != null || storyGreenCount != null || storyTextLineStats != null;
            if (captured && !hasAllCoreFields) {
                throw new IllegalArgumentException("CAPTURED result must carry all core dialog fields");
            }
            if (!captured && hasAnyField) {
                throw new IllegalArgumentException("non-CAPTURED result must not carry any dialog field");
            }
            if (captured && (framePngBytes.length == 0 || frameSha256.isBlank()
                    || frameWidth <= 0 || frameHeight <= 0)) {
                throw new IllegalArgumentException("invalid CAPTURED frame image, dimensions, or hash");
            }
        }

        private static DialogDetectionResult captured(DialogType dialogType,
                                                      int dialogLeft, int dialogTop,
                                                      int dialogRight, int dialogBottom,
                                                      byte[] framePngBytes, String frameSha256,
                                                      int frameWidth, int frameHeight,
                                                      Double maskStddev, Integer optionGreenCount,
                                                      Integer storyThinWhiteCount, Integer storyGreenCount,
                                                      ImagePreprocessor.TextLinePatternStats storyTextLineStats) {
            return new DialogDetectionResult(State.CAPTURED, dialogType,
                    dialogLeft, dialogTop, dialogRight, dialogBottom,
                    framePngBytes, frameSha256, frameWidth, frameHeight,
                    maskStddev, optionGreenCount, storyThinWhiteCount, storyGreenCount, storyTextLineStats);
        }

        private static DialogDetectionResult nonCaptured(State state) {
            return new DialogDetectionResult(state, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null);
        }

        @Override
        public byte[] framePngBytes() {
            return framePngBytes == null ? null : framePngBytes.clone();
        }
    }
}
