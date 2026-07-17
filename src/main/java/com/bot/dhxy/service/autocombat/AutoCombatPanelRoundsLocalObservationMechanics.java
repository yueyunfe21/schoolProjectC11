package com.bot.dhxy.service.autocombat;

import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Whole continuous local observation for the committed {@code 696a12b0} auto-combat panel remaining
 * rounds read, extracted from {@code AutoCombatPanelService.java:322-457}
 * ({@code readRemainingRounds} plus its {@code washRoundRedDigits}/{@code countBlackPixels} primitives).
 *
 * <p>From a caller-supplied panel match it derives the marker or center scan rect, takes one fresh-
 * geometry single capture, washes the red round digits ({@code 4x} upscale, red -> black), counts the
 * black pixels, runs the existing local OCR sidecar and reads the first {@code \d{1,2}} to a typed
 * terminal. It renders no business verdict, sends no input and adds no retry; refresh reason,
 * estimate/state/timestamp and Alt+8 remain with the future Cloud {@code AutoCombatPanelService}. Each
 * captured/washed {@link BufferedImage} has a single owner and is flushed once.</p>
 */
@Slf4j
@Service
public final class AutoCombatPanelRoundsLocalObservationMechanics {

    // Committed panel geometry (window-client pixels) from AutoCombatPanelService.
    private static final int AUTO_PANEL_WIDTH = 1751 - 1555;
    private static final int AUTO_PANEL_HEIGHT = 940 - 828;
    private static final int AUTO_PANEL_ROUNDS_SCAN_HEIGHT = AUTO_PANEL_HEIGHT / 2;
    private static final int ROUND_SCAN_TOP_OFFSET_FROM_GREEN_MARKER = -96;
    private static final int ROUND_SCAN_HEIGHT_FROM_GREEN_MARKER = 30;
    private static final int ROUND_DIGIT_OCR_SCALE = 4;
    private static final Pattern AUTO_PANEL_ROUND_DIGITS = Pattern.compile("\\d{1,2}");

    private final BoundWindowCaptureService captureService;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final TextRecognizer textRecognizer;
    private final WindowScopedTempPath windowScopedTempPath;

    public AutoCombatPanelRoundsLocalObservationMechanics(
            BoundWindowCaptureService captureService,
            WindowNativeBindingRefreshService bindingRefreshService,
            TextRecognizer textRecognizer,
            WindowScopedTempPath windowScopedTempPath) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.textRecognizer = Objects.requireNonNull(textRecognizer, "textRecognizer");
        this.windowScopedTempPath = Objects.requireNonNull(windowScopedTempPath, "windowScopedTempPath");
    }

    /** Closed terminal for one rounds observation. Only {@link #ROUNDS_READ} carries a rounds value. */
    public enum Status {
        ROUNDS_READ,
        NO_DIGITS,
        CAPTURE_UNAVAILABLE,
        OCR_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        MECHANICS_FAILED
    }

    /**
     * Closed immutable panel match input: only the screen-absolute panel center, the nullable green
     * marker pair, the green template width and the detection source. Independent of the visibility
     * mechanics type so this class never depends on that just-approved class.
     */
    public record PanelMatchInput(
            int panelCenterX,
            int panelCenterY,
            Integer greenMarkerX,
            Integer greenMarkerY,
            int greenTemplateWidth,
            String detectionSource) {

        public PanelMatchInput {
            boolean hasMarker = greenMarkerX != null && greenMarkerY != null;
            boolean anyMarker = greenMarkerX != null || greenMarkerY != null;
            if (anyMarker != hasMarker) {
                throw new IllegalArgumentException("green marker must be a full pair or fully absent");
            }
        }
    }

    /**
     * Immutable closed result carrying the scan rect, red pixel count and OCR text as defensive
     * evidence; only {@link Status#ROUNDS_READ} carries the parsed rounds. The scan rect is a full
     * {@code [left,top,right,bottom]} quad or fully absent.
     */
    public record RoundsObservationResult(
            Status status,
            Integer rounds,
            Integer scanLeft,
            Integer scanTop,
            Integer scanRight,
            Integer scanBottom,
            Integer redPixels,
            String ocrText,
            String detectionSource,
            String reason) {

        public RoundsObservationResult {
            Objects.requireNonNull(status, "status");
            if ((rounds != null) != (status == Status.ROUNDS_READ)) {
                throw new IllegalArgumentException("rounds is present exactly for ROUNDS_READ");
            }
            boolean hasRect = scanLeft != null && scanTop != null && scanRight != null && scanBottom != null;
            boolean anyRect = scanLeft != null || scanTop != null || scanRight != null || scanBottom != null;
            if (anyRect != hasRect) {
                throw new IllegalArgumentException("scan rect must be a full quad or fully absent");
            }
        }
    }

    /**
     * Read the remaining rounds from one caller-supplied panel match. Mirrors
     * {@code readRemainingRounds}: marker/center scan rect, one fresh-geometry capture, {@code 4x} red
     * wash, black-pixel count, local OCR, and the first {@code \d{1,2}}.
     */
    public RoundsObservationResult readRemainingRounds(
            WindowNativeBinding binding, PanelMatchInput input, String source) {
        String safeSource = safeSource(source);
        if (input == null) {
            return failure(Status.MECHANICS_FAILED, null, null, null, "invalid-input source=" + safeSource);
        }
        String detectionSource = input.detectionSource();

        // Two scan-rect branches, byte-for-byte with the baseline.
        int left;
        int top;
        int right;
        int bottom;
        if (input.greenMarkerX() != null && input.greenMarkerY() != null && input.greenTemplateWidth() > 0) {
            left = input.greenMarkerX();
            top = input.greenMarkerY() + ROUND_SCAN_TOP_OFFSET_FROM_GREEN_MARKER;
            right = left + Math.max(1, input.greenTemplateWidth() / 2);
            bottom = top + ROUND_SCAN_HEIGHT_FROM_GREEN_MARKER;
        } else {
            left = input.panelCenterX() - AUTO_PANEL_WIDTH / 2;
            top = input.panelCenterY() - AUTO_PANEL_HEIGHT / 2;
            right = left + AUTO_PANEL_WIDTH;
            bottom = top + AUTO_PANEL_ROUNDS_SCAN_HEIGHT;
        }
        int[] rect = {left, top, right, bottom};

        Optional<WindowNativeBinding> fresh = bindingRefreshService.refreshGeometry(binding);
        if (fresh.isEmpty()) {
            return failureWithRect(Status.BINDING_UNAVAILABLE, rect, null, null, detectionSource,
                    "binding-unavailable source=" + safeSource);
        }
        WindowNativeBinding live = fresh.get();
        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(
                    live, live.getX(), live.getY(), left, top, right, bottom);
        } catch (RuntimeException e) {
            log.warn("auto-combat rounds capture mechanics failed: source={} reason={}", safeSource, e.getMessage(), e);
            return failureWithRect(Status.MECHANICS_FAILED, rect, null, null, detectionSource,
                    "capture-mechanics-failed source=" + safeSource);
        }
        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return failureWithRect(Status.CAPTURE_UNAVAILABLE, rect, null, null, detectionSource,
                    "capture-unavailable source=" + safeSource);
        }

        BufferedImage raw = captured.get().image();
        BufferedImage washed = null;
        String rawPath = windowScopedTempPath.resolve("auto_combat_panel_rounds_" + safeSource + "_raw.png");
        String washedPath = windowScopedTempPath.resolve("auto_combat_panel_rounds_" + safeSource + "_red_digits.png");
        try {
            washed = washRoundRedDigits(raw);
            int redPixels = countBlackPixels(washed);
            ImagePreprocessor.saveImage(washed, washedPath);

            Optional<List<OcrWordResult>> wordsOptional = textRecognizer.getAllTextResultsLocalOnly(washedPath);
            if (wordsOptional.isEmpty()) {
                // Local OCR sidecar unavailable is its own terminal, never a faked no-digits read.
                ImagePreprocessor.saveImage(raw, rawPath);
                return failureWithRect(Status.OCR_UNAVAILABLE, rect, redPixels, null, detectionSource,
                        "ocr-unavailable source=" + safeSource);
            }
            String text = wordsOptional.get().stream()
                    .map(OcrWordResult::getText)
                    .filter(value -> value != null && !value.isBlank())
                    .reduce("", String::concat);
            Matcher matcher = AUTO_PANEL_ROUND_DIGITS.matcher(text == null ? "" : text);
            if (!matcher.find()) {
                ImagePreprocessor.saveImage(raw, rawPath);
                log.info("auto-combat rounds OCR returned no digits: source={} method={} redPixels={} text='{}'",
                        safeSource, detectionSource, redPixels, text);
                return new RoundsObservationResult(Status.NO_DIGITS, null,
                        left, top, right, bottom, redPixels, text, detectionSource, "no-digits source=" + safeSource);
            }
            int rounds = Integer.parseInt(matcher.group());
            deleteQuietly(washedPath);
            log.info("auto-combat rounds OCR result: source={} method={} rounds={} redPixels={} text='{}' rect=({}, {})-({}, {})",
                    safeSource, detectionSource, rounds, redPixels, text, left, top, right, bottom);
            return new RoundsObservationResult(Status.ROUNDS_READ, rounds,
                    left, top, right, bottom, redPixels, text, detectionSource, "rounds-read source=" + safeSource);
        } catch (RuntimeException e) {
            ImagePreprocessor.saveImage(raw, rawPath);
            if (washed != null) {
                ImagePreprocessor.saveImage(washed, washedPath);
            }
            log.warn("auto-combat rounds OCR mechanics failed: source={} method={} error={}",
                    safeSource, detectionSource, e.toString());
            return failureWithRect(Status.MECHANICS_FAILED, rect, null, null, detectionSource,
                    "ocr-mechanics-failed source=" + safeSource);
        } finally {
            raw.flush();
            if (washed != null) {
                washed.flush();
            }
        }
    }

    /** Baseline red-digit wash: {@code 4x} upscale, red pixel -> black, everything else -> white. */
    private static BufferedImage washRoundRedDigits(BufferedImage source) {
        BufferedImage washed = new BufferedImage(
                source.getWidth() * ROUND_DIGIT_OCR_SCALE,
                source.getHeight() * ROUND_DIGIT_OCR_SCALE,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int outputRgb = isAutoCombatRoundRedPixel(source.getRGB(x, y)) ? 0x000000 : 0xFFFFFF;
                for (int dy = 0; dy < ROUND_DIGIT_OCR_SCALE; dy++) {
                    for (int dx = 0; dx < ROUND_DIGIT_OCR_SCALE; dx++) {
                        washed.setRGB(
                                x * ROUND_DIGIT_OCR_SCALE + dx,
                                y * ROUND_DIGIT_OCR_SCALE + dy,
                                outputRgb);
                    }
                }
            }
        }
        return washed;
    }

    /** Exact baseline auto-combat round red-pixel predicate. */
    private static boolean isAutoCombatRoundRedPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r >= 130 && g <= 120 && b <= 120 && r - Math.max(g, b) >= 35;
    }

    /** Count fully-black pixels (the washed red digits), mirroring the baseline. */
    private static int countBlackPixels(BufferedImage image) {
        if (image == null) {
            return 0;
        }
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0x00FFFFFF) == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void deleteQuietly(String imagePath) {
        try {
            Files.deleteIfExists(Path.of(imagePath));
        } catch (Exception ignored) {
            // Best-effort cleanup for the success-path washed debug image only.
        }
    }

    private static RoundsObservationResult failure(
            Status status, Integer redPixels, String ocrText, String detectionSource, String reason) {
        return new RoundsObservationResult(status, null, null, null, null, null,
                redPixels, ocrText, detectionSource, reason);
    }

    private static RoundsObservationResult failureWithRect(
            Status status, int[] rect, Integer redPixels, String ocrText, String detectionSource, String reason) {
        return new RoundsObservationResult(status, null, rect[0], rect[1], rect[2], rect[3],
                redPixels, ocrText, detectionSource, reason);
    }

    private static String safeSource(String source) {
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        String value = source.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return value.length() <= 120 ? value : value.substring(0, 120);
    }
}
