package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoCombatPanelService {
    private static final String QUXIAO_ZIDONG_PATH = "images/template/battle/quxiao_zidong_green.png";
    private static final String ZIDONGHAI_PATH = "images/template/battle/zidonghai_white.png";

    private static final int TARGET_PANEL_X_OFFSET = 448;
    private static final int TARGET_PANEL_Y_OFFSET = 735;
    private static final int AUTO_PANEL_WIDTH = 1426 - 1277;
    private static final int AUTO_PANEL_HEIGHT = 1369 - 1283;
    private static final int AUTO_PANEL_ROUNDS_SCAN_HEIGHT = AUTO_PANEL_HEIGHT / 2;
    private static final int ROUND_DIGIT_OCR_SCALE = 4;
    private static final int DEFAULT_ESTIMATED_ROUNDS = 25;
    private static final int LOW_ROUNDS_REFRESH_THRESHOLD = 10;
    private static final int ESTIMATED_ROUNDS_PER_COMBAT = 3;
    private static final int AUTO_PANEL_REFRESH_WAIT_MS = 1000;
    private static final long AUTO_PANEL_MISSING_ATTENTION_MS = 10 * 60 * 1000L;
    private static final long AUTO_PANEL_MISSING_ATTENTION_REPEAT_MS = 60 * 1000L;
    private static final Pattern AUTO_PANEL_ROUND_DIGITS = Pattern.compile("\\d{1,2}");

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TextRecognizer textRecognizer;
    private final AutomationMetricsService automationMetricsService;

    private final Map<String, AutoCombatPanelRuntimeState> runtimeStates = new ConcurrentHashMap<>();

    /**
     * Fast combat-entry guard used by the window combat watcher.
     *
     * <p>This method intentionally does not drag the panel or OCR remaining rounds. The watcher calls
     * it immediately after a background combat signal so the window receives Alt+8 quickly if the
     * automatic-combat panel is missing. Slower alignment and round checks stay in
     * {@link #verifyAndAlignPanel()} on the owning task thread.</p>
     *
     * @param source short diagnostic label for the caller that detected combat entry.
     */
    public void ensureAutoCombatPanelVisibleFast(String source) {
        ensurePanelVisible("fast-start:" + source, 500);
    }

    public void verifyAndAlignPanel() {
        log.info("auto-combat panel: verify and align");
        Point panelPoint = ensurePanelVisible("verify", 1000);
        if (panelPoint == null) {
            return;
        }

        panelPoint = alignPanelIfNeeded(panelPoint);
        verifyRemainingRounds(panelPoint);
    }

    private Point ensurePanelVisible(String source, int waitAfterOpenMs) {
        AutoCombatPanelRuntimeState state = state();
        Point panelPoint = findAutoCombatBox();
        if (panelPoint != null) {
            clearAutoPanelMissing(state);
            log.info("auto-combat panel visible: source={} point=({}, {})",
                    source, panelPoint.x, panelPoint.y);
            return panelPoint;
        }

        log.warn("auto-combat panel not found; press Alt+8 and retry: source={}", source);
        boolean sent = inputSequences.submitAndWait("battle:openAutoPanel:" + source, List.of(
                InputAction.pressAlt8(),
                InputAction.sleep(waitAfterOpenMs)
        ));
        if (!sent) {
            recordAutoPanelMissing(state, source + ":input-failed");
            return null;
        }

        panelPoint = findAutoCombatBox();
        if (panelPoint == null) {
            log.warn("auto-combat panel still not found after Alt+8: source={}", source);
            recordAutoPanelMissing(state, source + ":not-found-after-alt8");
            return null;
        }

        clearAutoPanelMissing(state);
        log.info("auto-combat panel visible after Alt+8: source={} point=({}, {})",
                source, panelPoint.x, panelPoint.y);
        return panelPoint;
    }

    private Point alignPanelIfNeeded(Point panelPoint) {
        int dropX = tracker.getWindowBaseX() + TARGET_PANEL_X_OFFSET;
        int dropY = tracker.getWindowBaseY() + TARGET_PANEL_Y_OFFSET;
        if (panelPoint.distance(dropX, dropY) > 20.0) {
            log.info("auto-combat panel is outside safe area; drag from=({}, {}) to=({}, {})",
                    panelPoint.x, panelPoint.y, dropX, dropY);
            inputSequences.submitAndWait("battle:dragAutoPanel", List.of(
                    InputAction.dragAndDrop(panelPoint.x, panelPoint.y, dropX, dropY),
                    InputAction.sleep(500)
            ));
            panelPoint = new Point(dropX, dropY);
        } else {
            log.info("auto-combat panel is already in safe area");
        }

        state().panelAligned = true;
        return panelPoint;
    }

    private void verifyRemainingRounds(Point panelPoint) {
        AutoCombatPanelRuntimeState state = state();
        OptionalInt detectedRounds = readRemainingRounds(panelPoint, "verify");
        if (detectedRounds.isPresent()) {
            state.estimatedRounds = detectedRounds.getAsInt();
            log.info("auto-combat panel rounds read from panel: {}", state.estimatedRounds);
        }
        if (state.estimatedRounds < 0) {
            state.estimatedRounds = DEFAULT_ESTIMATED_ROUNDS;
            log.info("auto-combat panel rounds estimate initialized without OCR: {}", state.estimatedRounds);
        } else if (state.estimatedRounds <= LOW_ROUNDS_REFRESH_THRESHOLD) {
            log.info("auto-combat panel rounds estimate at or below threshold: estimate={} threshold={}; press Alt+8 to refresh",
                    state.estimatedRounds, LOW_ROUNDS_REFRESH_THRESHOLD);
            inputSequences.submitAndWait("battle:refreshAutoPanelRounds", List.of(
                    InputAction.pressAlt8(),
                    InputAction.sleep(AUTO_PANEL_REFRESH_WAIT_MS)
            ));
            OptionalInt refreshedRounds = readRemainingRounds(panelPoint, "refresh");
            if (refreshedRounds.isPresent()) {
                state.estimatedRounds = refreshedRounds.getAsInt();
                log.info("auto-combat panel rounds refresh verified by panel OCR: {}", state.estimatedRounds);
            } else {
                log.warn("auto-combat panel rounds refresh OCR failed; keep estimate={} for next retry",
                        state.estimatedRounds);
            }
        } else {
            log.info("auto-combat panel rounds estimate still healthy: estimate={} threshold={}",
                    state.estimatedRounds, LOW_ROUNDS_REFRESH_THRESHOLD);
        }
    }

    private void recordAutoPanelMissing(AutoCombatPanelRuntimeState state, String reason) {
        long now = System.currentTimeMillis();
        if (state.autoPanelMissingSinceAt <= 0L) {
            state.autoPanelMissingSinceAt = now;
            log.warn("auto-combat panel missing streak started: reason={}", reason);
            return;
        }

        long missingMs = now - state.autoPanelMissingSinceAt;
        if (missingMs < AUTO_PANEL_MISSING_ATTENTION_MS) {
            log.warn("auto-combat panel still missing: reason={} missingMs={}", reason, missingMs);
            return;
        }
        if (state.lastAutoPanelMissingAttentionAt > 0L
                && now - state.lastAutoPanelMissingAttentionAt < AUTO_PANEL_MISSING_ATTENTION_REPEAT_MS) {
            return;
        }
        state.lastAutoPanelMissingAttentionAt = now;
        String message = "自动战斗面板连续未识别超过10分钟，请人工检查是否已断自动";
        log.error("auto-combat panel needs attention: reason={} missingMs={} message={}",
                reason, missingMs, message);
        windowTaskContextHolder.rawCurrent()
                .ifPresent(windowContext -> {
                    windowContext.markRuntimeWarning(message);
                    automationMetricsService.recordWindowWarning(windowContext, "auto-combat-panel", message, Map.of(
                            "reason", reason,
                            "missingMs", Long.toString(missingMs)));
                });
    }

    private void clearAutoPanelMissing(AutoCombatPanelRuntimeState state) {
        if (state.autoPanelMissingSinceAt > 0L) {
            log.info("auto-combat panel missing streak cleared: missingMs={}",
                    System.currentTimeMillis() - state.autoPanelMissingSinceAt);
        }
        state.autoPanelMissingSinceAt = 0L;
        state.lastAutoPanelMissingAttentionAt = 0L;
    }

    public void recordCombatExit() {
        AutoCombatPanelRuntimeState state = state();
        if (state.estimatedRounds > 0) {
            state.estimatedRounds = Math.max(0, state.estimatedRounds - ESTIMATED_ROUNDS_PER_COMBAT);
            log.info("auto-combat panel rounds estimate after combat exit: {}", state.estimatedRounds);
        }
    }

    private Point findAutoCombatBox() {
        tracker.updateGlobalVision();
        String rawPath = tracker.getLatestVisionPath();

        BufferedImage rawImage = ImagePreprocessor.pathToBufferedImage(rawPath);
        if (rawImage == null) {
            log.error("auto-combat panel scan failed: cannot read screenshot path={}", rawPath);
            return null;
        }

        String washedGreenPath = windowScopedTempPath.resolve("debug_hsv_mask_green.png");
        ImagePreprocessor.countGreenPixelsHSV(rawImage, washedGreenPath);
        Point greenPoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(QUXIAO_ZIDONG_PATH, washedGreenPath, 0.80);
        if (greenPoint != null) {
            log.info("auto-combat panel green marker matched: point=({}, {})", greenPoint.x, greenPoint.y);
            rawImage.flush();
            return new Point(greenPoint.x + 20, greenPoint.y - 28);
        }
        log.warn("auto-combat panel green marker not matched: path={}", washedGreenPath);

        String washedWhitePath = windowScopedTempPath.resolve("debug_thin_white_text.png");
        ImagePreprocessor.countThinWhitePixelsHSV(rawImage, washedWhitePath);
        Point whitePoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(ZIDONGHAI_PATH, washedWhitePath, 0.80);
        rawImage.flush();

        if (whitePoint == null) {
            log.warn("auto-combat panel white marker not matched: path={}", washedWhitePath);
            return null;
        }
        log.info("auto-combat panel white marker matched: point=({}, {})", whitePoint.x, whitePoint.y);
        return new Point(whitePoint.x + 43, whitePoint.y + 28);
    }

    private OptionalInt readRemainingRounds(Point panelCenter, String source) {
        if (panelCenter == null) {
            return OptionalInt.empty();
        }
        int left = panelCenter.x - AUTO_PANEL_WIDTH / 2;
        int top = panelCenter.y - AUTO_PANEL_HEIGHT / 2;
        int right = left + AUTO_PANEL_WIDTH;
        int bottom = top + AUTO_PANEL_ROUNDS_SCAN_HEIGHT;
        BufferedImage raw = tracker.captureToMemory(
                "auto-combat-panel-rounds-" + source,
                left, top, right, bottom);
        if (raw == null) {
            log.warn("auto-combat panel rounds capture failed: source={} rect=({}, {})-({}, {}) center=({}, {})",
                    source, left, top, right, bottom, panelCenter.x, panelCenter.y);
            return OptionalInt.empty();
        }
        BufferedImage washed = null;
        try {
            String rawPath = windowScopedTempPath.resolve("auto_combat_panel_rounds_" + source + "_raw.png");
            ImagePreprocessor.saveImage(raw, rawPath);
            washed = washRoundRedDigits(raw);
            String washedPath = windowScopedTempPath.resolve("auto_combat_panel_rounds_" + source + "_red_digits.png");
            ImagePreprocessor.saveImage(washed, washedPath);
            List<OcrWordResult> words = textRecognizer.getAllTextResultsLocalOnly(washedPath);
            String text = words.stream()
                    .map(OcrWordResult::getText)
                    .filter(value -> value != null && !value.isBlank())
                    .reduce("", String::concat);
            Matcher matcher = AUTO_PANEL_ROUND_DIGITS.matcher(text == null ? "" : text);
            if (!matcher.find()) {
                log.info("auto-combat panel rounds OCR returned no digits: source={} path={} text='{}'",
                        source, washedPath, text);
                return OptionalInt.empty();
            }
            int rounds = Integer.parseInt(matcher.group());
            log.info("auto-combat panel rounds OCR result: source={} rounds={} text='{}' rect=({}, {})-({}, {})",
                    source, rounds, text, left, top, right, bottom);
            return OptionalInt.of(rounds);
        } finally {
            raw.flush();
            if (washed != null) {
                washed.flush();
            }
        }
    }

    private BufferedImage washRoundRedDigits(BufferedImage source) {
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

    private boolean isAutoCombatRoundRedPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r >= 130 && g <= 120 && b <= 120 && r - Math.max(g, b) >= 35;
    }

    private AutoCombatPanelRuntimeState state() {
        String key = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
        return runtimeStates.computeIfAbsent(key, ignored -> new AutoCombatPanelRuntimeState());
    }

    private static class AutoCombatPanelRuntimeState {
        private boolean panelAligned = false;
        private int estimatedRounds = -1;
        private long autoPanelMissingSinceAt = 0L;
        private long lastAutoPanelMissingAttentionAt = 0L;
    }
}
