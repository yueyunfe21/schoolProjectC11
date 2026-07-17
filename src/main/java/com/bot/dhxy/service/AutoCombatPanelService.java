package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.cloud.remote.RemoteAutoCombatPanelFact;
import com.bot.dhxy.cloud.remote.RemoteCoordinateSpace;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoCombatPanelService {
    private static final String AUTO_REMAINING_TEMPLATE_PATH = "images/template/battle/auto_remaining.png";

    private static final int TARGET_PANEL_X_OFFSET = 489;
    private static final int TARGET_PANEL_Y_OFFSET = 726;
    private static final int AUTO_REMAINING_TO_PANEL_CENTER_X = 43;
    private static final int AUTO_REMAINING_TO_PANEL_CENTER_Y = 28;
    private static final int DEFAULT_ESTIMATED_ROUNDS = 25;
    private static final int LOW_ROUNDS_REFRESH_THRESHOLD = 10;
    private static final int ESTIMATED_ROUNDS_PER_COMBAT = 3;
    private static final int AUTO_PANEL_REFRESH_WAIT_MS = 1000;
    private static final long AUTO_PANEL_MISSING_ATTENTION_MS = 10 * 60 * 1000L;
    private static final long AUTO_PANEL_MISSING_ATTENTION_REPEAT_MS = 60 * 1000L;
    private static final long REFRESH_DUE_TEAM_BURST_GUARD_MS = 30_000L;

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final WindowTaskContextHolder windowTaskContextHolder;
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

    /**
     * Reads the current bound window's auto-combat panel as a closed remote fact.
     *
     * <p>This probe delegates to the baseline {@link #findAutoCombatBox()} capture/template path,
     * preserves its screen-absolute coordinate calculation, and performs no input, round refresh,
     * missing-streak mutation, or metrics mutation.</p>
     *
     * @return typed panel observation; only {@code FOUND} contains screen-absolute coordinates
     */
    public RemoteAutoCombatPanelFact probeAutoCombatPanelFact() {
        GameClientTracker.CaptureAudit beforeCapture = tracker.getLastCaptureAudit();
        AutoCombatPanelMatch match = findAutoCombatBox();
        GameClientTracker.CaptureAudit afterCapture = tracker.getLastCaptureAudit();
        if (match != null) {
            return RemoteAutoCombatPanelFact.builder()
                    .state(RemoteAutoCombatPanelFact.State.FOUND)
                    .panelCenterX(match.panelCenter.x)
                    .panelCenterY(match.panelCenter.y)
                    .greenMarkerX(match.greenMarker == null ? null : match.greenMarker.x)
                    .greenMarkerY(match.greenMarker == null ? null : match.greenMarker.y)
                    .greenTemplateWidth(match.greenTemplateWidth)
                    .detectionSource(match.detectionSource)
                    .coordinateSpace(RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX)
                    .build();
        }

        boolean captureFailed = afterCapture == beforeCapture
                || afterCapture == null
                || !afterCapture.success();
        return RemoteAutoCombatPanelFact.builder()
                .state(captureFailed
                        ? RemoteAutoCombatPanelFact.State.CAPTURE_FAILED
                        : RemoteAutoCombatPanelFact.State.NOT_FOUND)
                .panelCenterX(null)
                .panelCenterY(null)
                .greenMarkerX(null)
                .greenMarkerY(null)
                .greenTemplateWidth(0)
                .detectionSource("auto-remaining")
                .coordinateSpace(RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX)
                .build();
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
        RoundsRefreshReason refreshReason = resolveRoundsRefreshReason(
                estimatedRounds, lastRefreshAt, refreshIntervalMs, now);

        if (refreshReason == null) {
            log.info("auto-combat panel rounds estimate healthy by cached counter: source={} estimate={} threshold={} lastRefreshAgoMs={} intervalMs={}",
                    source, estimatedRounds, LOW_ROUNDS_REFRESH_THRESHOLD,
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

        Point autoRemainingPoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(
                AUTO_REMAINING_TEMPLATE_PATH, rawPath, 0.80);
        if (autoRemainingPoint == null) {
            log.warn("auto-combat panel auto-remaining template not matched: template={} raw={} audit={}",
                    AUTO_REMAINING_TEMPLATE_PATH, rawPath, captureAudit.toLogText());
            return null;
        }
        Point panelCenter = new Point(
                autoRemainingPoint.x + AUTO_REMAINING_TO_PANEL_CENTER_X,
                autoRemainingPoint.y + AUTO_REMAINING_TO_PANEL_CENTER_Y);
        log.info("auto-combat panel auto-remaining template matched: point=({}, {}) inferredPanelCenter=({}, {}) audit={}",
                autoRemainingPoint.x, autoRemainingPoint.y, panelCenter.x, panelCenter.y,
                captureAudit.toLogText());
        return new AutoCombatPanelMatch(panelCenter, null, 0, "auto-remaining");
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
