package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
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
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final String ZIDONG_GREEN_PATH = "images/template/battle/zidong_green.png";
    private static final String AUTO_PANEL_FALLBACK_ANCHOR_PATH = "images/template/battle/auto_panel_fallback_anchor.png";

    private static final int TARGET_PANEL_X_OFFSET = 489;
    private static final int TARGET_PANEL_Y_OFFSET = 726;
    private static final int AUTO_PANEL_WIDTH = 1751 - 1555;
    private static final int AUTO_PANEL_HEIGHT = 940 - 828;
    private static final int AUTO_PANEL_ROUNDS_SCAN_HEIGHT = AUTO_PANEL_HEIGHT / 2;
    private static final int ROUND_SCAN_TOP_OFFSET_FROM_GREEN_MARKER = -96;
    private static final int ROUND_SCAN_HEIGHT_FROM_GREEN_MARKER = 30;
    private static final int FALLBACK_ANCHOR_TO_GREEN_MARKER_X = 30;
    private static final int FALLBACK_ANCHOR_TO_GREEN_MARKER_Y = 30;
    private static final int ROUND_DIGIT_OCR_SCALE = 4;
    private static final int DEFAULT_ESTIMATED_ROUNDS = 25;
    private static final int LOW_ROUNDS_REFRESH_THRESHOLD = 10;
    private static final int ESTIMATED_ROUNDS_PER_COMBAT = 3;
    private static final int AUTO_PANEL_REFRESH_WAIT_MS = 1000;
    private static final long AUTO_PANEL_MISSING_ATTENTION_MS = 10 * 60 * 1000L;
    private static final long AUTO_PANEL_MISSING_ATTENTION_REPEAT_MS = 60 * 1000L;
    private static final long REFRESH_DUE_TEAM_BURST_GUARD_MS = 30_000L;
    private static final Pattern AUTO_PANEL_ROUND_DIGITS = Pattern.compile("\\d{1,2}");

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TextRecognizer textRecognizer;
    private final AutomationMetricsService automationMetricsService;
    private final GameContext gameContext;
    private final BotProperties botProperties;

    private final Map<String, AutoCombatPanelRuntimeState> runtimeStates = new ConcurrentHashMap<>();

    public void verifyAndAlignPanel() {
        verifyAndAlignPanel(PanelVerifyMode.VERIFY_AND_REFRESH);
    }

    public boolean verifyAndAlignPanel(PanelVerifyMode mode) {
        PanelVerifyMode safeMode = mode == null ? PanelVerifyMode.VERIFY_AND_REFRESH : mode;
        log.info("auto-combat panel: verify and align mode={}", safeMode);
        AutoCombatPanelMatch panelMatch = ensurePanelMatchVisible(safeMode.source(), 1000);
        if (panelMatch == null) {
            return false;
        }

        panelMatch = alignPanelIfNeeded(panelMatch);
        if (!safeMode.refreshRounds()) {
            log.info("auto-combat panel rounds refresh skipped: source={} reason=verify-only mode={}",
                    safeMode.source(), safeMode);
            return false;
        }
        return refreshAutoCombatRoundsIfNeeded(panelMatch, safeMode.source());
    }

    public Point ensurePanelVisible(String source, int waitAfterOpenMs) {
        AutoCombatPanelMatch match = ensurePanelMatchVisible(source, waitAfterOpenMs);
        return match == null ? null : match.panelCenter;
    }

    private AutoCombatPanelMatch ensurePanelMatchVisible(String source, int waitAfterOpenMs) {
        AutoCombatPanelRuntimeState state = state();
        AutoCombatPanelMatch panelMatch = findAutoCombatBox();
        if (panelMatch != null) {
            clearAutoPanelMissing(state);
            log.info("auto-combat panel visible: source={} method={} center=({}, {}) marker=({}, {})",
                    source, panelMatch.detectionSource, panelMatch.panelCenter.x, panelMatch.panelCenter.y,
                    panelMatch.greenMarker == null ? -1 : panelMatch.greenMarker.x,
                    panelMatch.greenMarker == null ? -1 : panelMatch.greenMarker.y);
            return panelMatch;
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

        panelMatch = findAutoCombatBox();
        if (panelMatch == null) {
            log.warn("auto-combat panel still not found after Alt+8: source={}", source);
            recordAutoPanelMissing(state, source + ":not-found-after-alt8");
            return null;
        }

        recordAutoCombatRefresh("openAutoPanel:" + source);
        clearAutoPanelMissing(state);
        log.info("auto-combat panel visible after Alt+8: source={} method={} center=({}, {}) marker=({}, {})",
                source, panelMatch.detectionSource, panelMatch.panelCenter.x, panelMatch.panelCenter.y,
                panelMatch.greenMarker == null ? -1 : panelMatch.greenMarker.x,
                panelMatch.greenMarker == null ? -1 : panelMatch.greenMarker.y);
        return panelMatch;
    }

    private AutoCombatPanelMatch alignPanelIfNeeded(AutoCombatPanelMatch panelMatch) {
        Point panelPoint = panelMatch.panelCenter;
        int dropX = tracker.getWindowBaseX() + TARGET_PANEL_X_OFFSET;
        int dropY = tracker.getWindowBaseY() + TARGET_PANEL_Y_OFFSET;
        if (panelPoint.distance(dropX, dropY) > 20.0) {
            log.info("auto-combat panel is outside safe area; drag from=({}, {}) to=({}, {})",
                    panelPoint.x, panelPoint.y, dropX, dropY);
            inputSequences.submitAndWait("battle:dragAutoPanel", List.of(
                    InputAction.dragAndDrop(panelPoint.x, panelPoint.y, dropX, dropY),
                    InputAction.sleep(500)
            ));
            AutoCombatPanelMatch refreshed = findAutoCombatBox();
            if (refreshed != null) {
                panelMatch = refreshed;
            } else {
                panelMatch = new AutoCombatPanelMatch(new Point(dropX, dropY), null, 0, "drag-target-fallback");
            }
        } else {
            log.info("auto-combat panel is already in safe area");
        }

        state().panelAligned = true;
        return panelMatch;
    }

    public static RoundsRefreshReason resolveRoundsRefreshReason(int estimatedRounds,
                                                                 long lastRefreshAt,
                                                                 long refreshIntervalMs,
                                                                 long now) {
        if (estimatedRounds < 0) {
            return RoundsRefreshReason.UNKNOWN;
        }
        if (estimatedRounds <= LOW_ROUNDS_REFRESH_THRESHOLD) {
            return RoundsRefreshReason.LOW_ROUNDS;
        }
        if (refreshIntervalMs > 0L && (lastRefreshAt <= 0L || now - lastRefreshAt >= refreshIntervalMs)) {
            return RoundsRefreshReason.REFRESH_DUE;
        }
        return null;
    }

    private boolean refreshAutoCombatRoundsIfNeeded(AutoCombatPanelMatch panelMatch, String source) {
        int estimatedRounds = gameContext.getAutoCombatEstimatedRounds();
        long lastRefreshAt = gameContext.getLastAutoCombatRefreshAt();
        long now = System.currentTimeMillis();
        long refreshIntervalMs = Math.max(0L, botProperties.getAutoBattleRefreshIntervalMs());
        OptionalInt visibleRounds = readRemainingRounds(panelMatch, source);
        if (visibleRounds.isPresent()) {
            gameContext.setAutoCombatEstimatedRounds(visibleRounds.getAsInt());
            estimatedRounds = visibleRounds.getAsInt();
        }
        RoundsRefreshReason refreshReason = resolveRoundsRefreshReason(
                estimatedRounds, lastRefreshAt, refreshIntervalMs, now);

        if (refreshReason == null) {
            log.info("auto-combat panel rounds estimate healthy after visible/cache check: source={} estimate={} visible={} threshold={} lastRefreshAgoMs={} intervalMs={}",
                    source, estimatedRounds, LOW_ROUNDS_REFRESH_THRESHOLD,
                    visibleRounds.isPresent() ? visibleRounds.getAsInt() : -1,
                    lastRefreshAt <= 0L ? -1L : now - lastRefreshAt, refreshIntervalMs);
            return false;
        }

        log.info("auto-combat panel rounds refresh by Alt+8 without OCR: source={} reason={} estimate={} threshold={} lastRefreshAgoMs={} intervalMs={}",
                source, refreshReason.logValue(), estimatedRounds, LOW_ROUNDS_REFRESH_THRESHOLD,
                lastRefreshAt <= 0L ? -1L : now - lastRefreshAt, refreshIntervalMs);
        boolean sent = inputSequences.submitAndWait("battle:refreshAutoPanelRounds:" + source + ":" + refreshReason.logValue(), List.of(
                InputAction.pressAlt8(),
                InputAction.sleep(AUTO_PANEL_REFRESH_WAIT_MS)
        ));
        if (sent) {
            recordAutoCombatRefresh("refresh:" + source + ":" + refreshReason.logValue());
            return true;
        } else {
            log.warn("auto-combat panel rounds refresh input failed: source={} reason={} estimate={}",
                    source, refreshReason.logValue(), estimatedRounds);
            return false;
        }
    }

    private void recordAutoCombatRefresh(String source) {
        long now = System.currentTimeMillis();
        gameContext.setAutoCombatEstimatedRounds(DEFAULT_ESTIMATED_ROUNDS);
        gameContext.setLastAutoCombatRefreshAt(now);
        log.info("auto-combat rounds estimate reset by Alt+8: source={} estimate={} refreshAt={}",
                source, DEFAULT_ESTIMATED_ROUNDS, now);
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
        int estimatedRounds = gameContext.getAutoCombatEstimatedRounds();
        if (estimatedRounds > 0) {
            int updated = Math.max(0, estimatedRounds - ESTIMATED_ROUNDS_PER_COMBAT);
            gameContext.setAutoCombatEstimatedRounds(updated);
            log.info("auto-combat panel rounds estimate after combat exit: before={} after={} decrement={}",
                    estimatedRounds, updated, ESTIMATED_ROUNDS_PER_COMBAT);
        }
    }

    private AutoCombatPanelMatch findAutoCombatBox() {
        boolean captured = tracker.updateGlobalVision();
        String rawPath = tracker.getLatestVisionPath();
        GameClientTracker.CaptureAudit captureAudit = tracker.getLastCaptureAudit();
        log.info("auto-combat panel screenshot audit: captured={} rawPath={} audit={}",
                captured, rawPath, captureAudit.toLogText());

        BufferedImage rawImage = ImagePreprocessor.pathToBufferedImage(rawPath);
        if (rawImage == null) {
            log.error("auto-combat panel scan failed: cannot read screenshot path={} audit={}",
                    rawPath, captureAudit.toLogText());
            return null;
        }

        Point fallbackPoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(
                AUTO_PANEL_FALLBACK_ANCHOR_PATH, rawPath, 0.80);
        if (fallbackPoint != null) {
            Point inferredGreenMarker = new Point(
                    fallbackPoint.x + FALLBACK_ANCHOR_TO_GREEN_MARKER_X,
                    fallbackPoint.y + FALLBACK_ANCHOR_TO_GREEN_MARKER_Y);
            log.info("auto-combat panel anchor matched: point=({}, {}) inferredGreenMarker=({}, {}) audit={}",
                    fallbackPoint.x, fallbackPoint.y, inferredGreenMarker.x, inferredGreenMarker.y,
                    captureAudit.toLogText());
            int greenTemplateWidth = readImageWidth(QUXIAO_ZIDONG_PATH);
            rawImage.flush();
            return new AutoCombatPanelMatch(
                    fallbackPoint,
                    inferredGreenMarker,
                    greenTemplateWidth,
                    "panel-anchor");
        }
        log.warn("auto-combat panel anchor not matched: template={} audit={}",
                AUTO_PANEL_FALLBACK_ANCHOR_PATH, captureAudit.toLogText());

        String washedGreenPath = windowScopedTempPath.resolve("debug_hsv_mask_green.png");
        ImagePreprocessor.countGreenPixelsHSV(rawImage, washedGreenPath);
        rawImage.flush();
        Point greenPoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(ZIDONG_GREEN_PATH, washedGreenPath, 0.80);
        if (greenPoint == null) {
            log.warn("auto-combat panel green auto marker not matched: path={} template={} audit={}",
                    washedGreenPath, ZIDONG_GREEN_PATH, captureAudit.toLogText());
            return null;
        }
        Point inferredPanelAnchor = new Point(
                greenPoint.x - FALLBACK_ANCHOR_TO_GREEN_MARKER_X,
                greenPoint.y - FALLBACK_ANCHOR_TO_GREEN_MARKER_Y);
        log.info("auto-combat panel green auto marker matched: point=({}, {}) inferredPanelAnchor=({}, {}) audit={}",
                greenPoint.x, greenPoint.y, inferredPanelAnchor.x, inferredPanelAnchor.y,
                captureAudit.toLogText());
        int greenTemplateWidth = readImageWidth(QUXIAO_ZIDONG_PATH);
        return new AutoCombatPanelMatch(inferredPanelAnchor, greenPoint, greenTemplateWidth, "green-auto");
    }

    private OptionalInt readRemainingRounds(AutoCombatPanelMatch panelMatch, String source) {
        if (panelMatch == null || panelMatch.panelCenter == null) {
            return OptionalInt.empty();
        }
        Point panelCenter = panelMatch.panelCenter;
        int left;
        int top;
        int right;
        int bottom;
        if (panelMatch.greenMarker != null && panelMatch.greenTemplateWidth > 0) {
            left = panelMatch.greenMarker.x;
            top = panelMatch.greenMarker.y + ROUND_SCAN_TOP_OFFSET_FROM_GREEN_MARKER;
            right = left + Math.max(1, panelMatch.greenTemplateWidth / 2);
            bottom = top + ROUND_SCAN_HEIGHT_FROM_GREEN_MARKER;
        } else {
            left = panelCenter.x - AUTO_PANEL_WIDTH / 2;
            top = panelCenter.y - AUTO_PANEL_HEIGHT / 2;
            right = left + AUTO_PANEL_WIDTH;
            bottom = top + AUTO_PANEL_ROUNDS_SCAN_HEIGHT;
        }
        log.info("auto-combat panel rounds capture plan: source={} method={} center=({}, {}) marker=({}, {}) rect=({}, {})-({}, {})",
                source, panelMatch.detectionSource, panelCenter.x, panelCenter.y,
                panelMatch.greenMarker == null ? -1 : panelMatch.greenMarker.x,
                panelMatch.greenMarker == null ? -1 : panelMatch.greenMarker.y,
                left, top, right, bottom);
        BufferedImage raw = tracker.captureToMemory(
                "auto-combat-panel-rounds-" + source,
                left, top, right, bottom);
        if (raw == null) {
            log.warn("auto-combat panel rounds capture failed: source={} rect=({}, {})-({}, {}) center=({}, {})",
                    source, left, top, right, bottom, panelCenter.x, panelCenter.y);
            return OptionalInt.empty();
        }
        BufferedImage washed = null;
        String rawPath = windowScopedTempPath.resolve("auto_combat_panel_rounds_" + source + "_raw.png");
        String washedPath = windowScopedTempPath.resolve("auto_combat_panel_rounds_" + source + "_red_digits.png");
        try {
            washed = washRoundRedDigits(raw);
            int redDigitPixels = countBlackPixels(washed);
            ImagePreprocessor.saveImage(washed, washedPath);
            List<OcrWordResult> words = textRecognizer.getAllTextResultsLocalOnly(washedPath);
            String text = words.stream()
                    .map(OcrWordResult::getText)
                    .filter(value -> value != null && !value.isBlank())
                    .reduce("", String::concat);
            Matcher matcher = AUTO_PANEL_ROUND_DIGITS.matcher(text == null ? "" : text);
            if (!matcher.find()) {
                ImagePreprocessor.saveImage(raw, rawPath);
                log.info("auto-combat panel rounds OCR returned no digits: source={} method={} redPixels={} rawPath={} washedPath={} text='{}'",
                        source, panelMatch.detectionSource, redDigitPixels, rawPath, washedPath, text);
                return OptionalInt.empty();
            }
            int rounds = Integer.parseInt(matcher.group());
            deleteQuietly(washedPath);
            log.info("auto-combat panel rounds OCR result: source={} method={} rounds={} redPixels={} text='{}' rect=({}, {})-({}, {})",
                    source, panelMatch.detectionSource, rounds, redDigitPixels, text, left, top, right, bottom);
            return OptionalInt.of(rounds);
        } catch (Exception e) {
            ImagePreprocessor.saveImage(raw, rawPath);
            if (washed != null) {
                ImagePreprocessor.saveImage(washed, washedPath);
            }
            log.warn("auto-combat panel rounds OCR failed: source={} method={} rawPath={} washedPath={} error={}",
                    source, panelMatch.detectionSource, rawPath, washedPath, e.toString());
            return OptionalInt.empty();
        } finally {
            raw.flush();
            if (washed != null) {
                washed.flush();
            }
        }
    }

    private int countBlackPixels(BufferedImage image) {
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

    private void deleteQuietly(String imagePath) {
        try {
            Files.deleteIfExists(Path.of(imagePath));
        } catch (Exception ignored) {
            // Best-effort cleanup for success-path debug images only.
        }
    }

    private int readImageWidth(String imagePath) {
        BufferedImage image = ImagePreprocessor.pathToBufferedImage(imagePath);
        if (image == null) {
            return 0;
        }
        try {
            return image.getWidth();
        } finally {
            image.flush();
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
        private long autoPanelMissingSinceAt = 0L;
        private long lastAutoPanelMissingAttentionAt = 0L;
    }

    public enum PanelVerifyMode {
        ENTRY_MAINTENANCE(false, "entry-maintenance"),
        VERIFY_AND_REFRESH(true, "verify");

        private final boolean refreshRounds;
        private final String source;

        PanelVerifyMode(boolean refreshRounds, String source) {
            this.refreshRounds = refreshRounds;
            this.source = source;
        }

        private boolean refreshRounds() {
            return refreshRounds;
        }

        private String source() {
            return source;
        }
    }

    public enum RoundsRefreshReason {
        UNKNOWN("unknown"),
        LOW_ROUNDS("low-rounds"),
        REFRESH_DUE("refresh-due");

        private final String logValue;

        RoundsRefreshReason(String logValue) {
            this.logValue = logValue;
        }

        private String logValue() {
            return logValue;
        }
    }

    public record RefreshDueBurstDecision(boolean deferred, long retryAfterMs, long lastTeamRefreshAgeMs) {
        private static RefreshDueBurstDecision allowed() {
            return new RefreshDueBurstDecision(false, 0L, -1L);
        }

        private static RefreshDueBurstDecision deferred(long retryAfterMs, long lastTeamRefreshAgeMs) {
            return new RefreshDueBurstDecision(true, retryAfterMs, lastTeamRefreshAgeMs);
        }
    }

    public static class TeamRefreshDueBurstGuard {
        private final Map<String, Long> lastRefreshDueByTeam = new ConcurrentHashMap<>();

        public RefreshDueBurstDecision reserveIfAllowed(String teamKey, String windowId, String reason, long now) {
            if (!RoundsRefreshReason.REFRESH_DUE.logValue().equals(reason)) {
                return RefreshDueBurstDecision.allowed();
            }
            String safeTeamKey = teamKey == null || teamKey.isBlank() ? windowId : teamKey;
            String key = safeTeamKey == null || safeTeamKey.isBlank() ? "default" : safeTeamKey;
            Long lastAt = lastRefreshDueByTeam.get(key);
            if (lastAt != null) {
                long age = now - lastAt;
                if (age >= 0L && age < REFRESH_DUE_TEAM_BURST_GUARD_MS) {
                    return RefreshDueBurstDecision.deferred(REFRESH_DUE_TEAM_BURST_GUARD_MS - age, age);
                }
            }
            lastRefreshDueByTeam.put(key, now);
            return RefreshDueBurstDecision.allowed();
        }
    }

    private static class AutoCombatPanelMatch {
        private final Point panelCenter;
        private final Point greenMarker;
        private final int greenTemplateWidth;
        private final String detectionSource;

        private AutoCombatPanelMatch(Point panelCenter, Point greenMarker, int greenTemplateWidth) {
            this(panelCenter, greenMarker, greenTemplateWidth, "green-marker");
        }

        private AutoCombatPanelMatch(Point panelCenter, Point greenMarker, int greenTemplateWidth, String detectionSource) {
            this.panelCenter = panelCenter;
            this.greenMarker = greenMarker;
            this.greenTemplateWidth = greenTemplateWidth;
            this.detectionSource = detectionSource;
        }
    }
}
