package com.bot.dhxy.tools;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;


import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameStateUtil {

    private static final long COORD_DETECT_WINDOW_MS = 1400;
    private static final long PIXEL_FALLBACK_WINDOW_MS = 1200;
    private static final long MOVE_SAMPLE_INTERVAL_MS = 300; // 🌟 加快采样：每 300 毫秒看一眼
    private static final int COORD_MIN_STABLE_SAMPLES = 4;
    private static final int COORD_STRONG_MOVE_DISTANCE = 3;
    private static final int COORD_FAST_MOVE_DISTANCE = 2;
    private static final long COORD_FAST_MOVE_WINDOW_MS = 450;
    private static final int COORD_MAYBE_MOVE_HITS_FOR_MOVING = 2;
    private static final long DEFAULT_PATHING_PROTECTION_MS = 5500;
    private static final int FAST_PASS_HITS = 2; // 🌟 极速放行：只要累计看到 2 次画面变动，立刻判定为跑动
    private static final double MOVE_DIFF_RATIO = 0.05; // 默认两帧匹配阈值// 默认两帧匹配阈值

    private static final double DEFAULT_MAP_LABEL_SAME_TOLERANCE = 0.08;
    private static final long DEFAULT_MAP_CONFIRM_POLL_MS = 1200L;
    private static final long MIN_MAP_CONFIRM_POLL_MS = 100L;
    private final GameContext context;
    private final GameClientTracker tracker;
    private final PlayerStateService playerStateService;
    private final CoordinateHelper coordinateHelper;
    private final MiniMapCoordinateReader miniMapCoordinateReader;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final Map<String, MovementIntentState> movementIntentStates = new ConcurrentHashMap<>();

    /**
     * 判断是否在移动（通过边缘像素差异防抖识别）
     */
    public boolean isMovingByPixelDiff() {
        MovementState state = detectMovementState();
        return state == MovementState.MOVING
                || state == MovementState.PATHING_ACTIVE
                || state == MovementState.MAYBE_MOVING;
    }

    public BufferedImage captureCurrentMapLabelSnapshot(String reason) {
        BufferedImage label = miniMapCoordinateReader.readCurrentMapLabelImage().orElse(null);
        if (label != null) {
            log.info("[地图标签] 截取当前地图名文字图 reason={} size={}x{}",
                    safeReason(reason), label.getWidth(), label.getHeight());
        } else {
            log.warn("[地图标签] 截取当前地图名文字图失败 reason={}", safeReason(reason));
        }
        return label;
    }

    public boolean isCurrentMapLabelChangedFrom(BufferedImage baseline, String reason) {
        return isCurrentMapLabelChangedFrom(baseline, DEFAULT_MAP_LABEL_SAME_TOLERANCE, reason);
    }

    public boolean isCurrentMapLabelChangedFrom(BufferedImage baseline, double sameTolerance, String reason) {
        if (baseline == null) {
            log.warn("[地图标签] baseline 为空，无法验证地图名是否变化 reason={}", safeReason(reason));
            return false;
        }

        BufferedImage current = captureCurrentMapLabelSnapshot(reason);
        if (current == null) {
            return false;
        }
        try {
            boolean changed = !ImageFinder.isMatch(baseline, current, sameTolerance);
            log.info("[地图标签] 地图名文字图比较 reason={} changed={} baseline={}x{} current={}x{} tolerance={}",
                    safeReason(reason), changed,
                    baseline.getWidth(), baseline.getHeight(),
                    current.getWidth(), current.getHeight(),
                    sameTolerance);
            return changed;
        } finally {
            current.flush();
        }
    }

    /**
     * Confirm that the current bound game window is already on the expected map.
     *
     * <p>This is the shared map-arrival probe for navigation, teleport, and task recovery flows. It
     * first trusts the cached {@link PlayerCharacter#getCurrentMapName()} value when it already
     * matches, then asks {@link PlayerStateService#syncMyPosition()} for fresh no-input
     * position syncs. A successful sync mutates the current {@link GameContext#getMe()} map name and
     * logical coordinate. Callers pass in-game map names such as {@code 长安}; timeout and polling
     * values are in milliseconds.</p>
     *
     * @param targetMapName expected in-game map name; blank values are treated as a failed check.
     * @param timeoutMs maximum time in milliseconds to keep scanning. Zero performs a single
     *                  immediate check.
     * @param reason log label describing the caller, for example {@code navigateToMap}.
     * @return true when the cached or scanned current map equals {@code targetMapName}; false when
     *         the timeout expires or recognition fails. Task stop checkpoints may throw
     *         {@link TaskStopRequestedException}.
     */
    public boolean confirmCurrentMap(String targetMapName, long timeoutMs, String reason) {
        return confirmCurrentMap(targetMapName, timeoutMs, DEFAULT_MAP_CONFIRM_POLL_MS, reason);
    }

    /**
     * Confirm the current map using an explicit polling interval.
     *
     * @param targetMapName expected in-game map name; blank values are treated as a failed check.
     * @param timeoutMs maximum time in milliseconds to keep scanning.
     * @param pollMs delay in milliseconds between scans; values below 100ms are clamped.
     * @param reason log label describing the caller.
     * @return true when the current map is confirmed as {@code targetMapName}; otherwise false.
     */
    public boolean confirmCurrentMap(String targetMapName, long timeoutMs, long pollMs, String reason) {
        return confirmCurrentMap(targetMapName, timeoutMs, pollMs, true, reason);
    }

    /**
     * Confirm the current map by forcing at least one fresh location scan before trusting state.
     *
     * <p>Use this when the caller is recovering from a navigation failure and stale cached map
     * state would be more dangerous than the extra scan cost.</p>
     *
     * @param targetMapName expected in-game map name; blank values are treated as a failed check.
     * @param timeoutMs maximum time in milliseconds to keep scanning.
     * @param reason log label describing the caller.
     * @return true when a fresh scan confirms {@code targetMapName}; otherwise false.
     */
    public boolean confirmCurrentMapFresh(String targetMapName, long timeoutMs, String reason) {
        return confirmCurrentMap(targetMapName, timeoutMs, DEFAULT_MAP_CONFIRM_POLL_MS, false, reason);
    }

    private boolean confirmCurrentMap(String targetMapName,
                                      long timeoutMs,
                                      long pollMs,
                                      boolean allowCachedMatch,
                                      String reason) {
        long latencyStart = LatencyMetrics.start();
        String safeReason = safeReason(reason);
        String expected = targetMapName == null ? "" : targetMapName.trim();
        boolean result = false;
        String lastMap = null;
        Integer lastX = null;
        Integer lastY = null;

        try {
            if (expected.isBlank()) {
                log.warn("[map-confirm] target map is blank reason={}", safeReason);
                return false;
            }

            long safeTimeoutMs = Math.max(timeoutMs, 0L);
            long safePollMs = Math.max(pollMs, MIN_MAP_CONFIRM_POLL_MS);
            long deadline = System.currentTimeMillis() + safeTimeoutMs;

            do {
                checkpointStateProbe("confirm current map");

                PlayerCharacter me = context.getMe();
                if (allowCachedMatch && me != null && expected.equals(me.getCurrentMapName())) {
                    lastMap = me.getCurrentMapName();
                    lastX = me.getX();
                    lastY = me.getY();
                    log.info("[map-confirm] cached match reason={} target={} coord=({}, {})",
                            safeReason, expected, lastX, lastY);
                    result = true;
                    return true;
                }

                LocationInfo locationInfo = playerStateService.syncMyPosition();
                if (locationInfo != null) {
                    lastMap = locationInfo.mapName;
                    lastX = locationInfo.x;
                    lastY = locationInfo.y;
                    log.info("[map-confirm] scanned reason={} target={} current={} coord=({}, {})",
                            safeReason, expected, locationInfo.mapName, locationInfo.x, locationInfo.y);
                    if (expected.equals(locationInfo.mapName)) {
                        result = true;
                        return true;
                    }
                } else {
                    log.info("[map-confirm] scan empty reason={} target={}", safeReason, expected);
                }

                long now = System.currentTimeMillis();
                if (now >= deadline) {
                    break;
                }
                long sleepMs = Math.min(safePollMs, Math.max(deadline - now, 1L));
                if (!TaskSleep.sleep(sleepMs)) {
                    return false;
                }
            } while (System.currentTimeMillis() <= deadline);

            PlayerCharacter me = context.getMe();
            result = allowCachedMatch && me != null && expected.equals(me.getCurrentMapName());
            return result;
        } finally {
            LatencyMetrics.info(log, "gameState.confirmCurrentMap", latencyStart,
                    "result=" + result + " reason=" + safeReason + " target=" + expected
                            + " cachedAllowed=" + allowCachedMatch
                            + " last=" + safeLatencyValue(lastMap)
                            + " coord=" + (lastX == null || lastY == null ? "-" : lastX + "," + lastY));
        }
    }

    public void recordMovementIntent(String source) {
        recordMovementIntent(source, DEFAULT_PATHING_PROTECTION_MS);
    }

    public void recordMovementIntent(String source, long protectionMs) {
        long safeProtectionMs = Math.max(protectionMs, 0);
        MovementIntentState state = movementIntentStates.computeIfAbsent(windowKey(), ignored -> new MovementIntentState());
        state.source = source == null || source.isBlank() ? "unknown" : source;
        state.expiresAt = System.currentTimeMillis() + safeProtectionMs;
        log.info("🧭 [移动意图] 已记录：source={} protectionMs={}", state.source, safeProtectionMs);
    }

    public MovementState detectMovementState() {
        CoordinateProbeResult coordinateResult = detectMovementByCoordinate();
        if (coordinateResult.state() == MovementState.MOVING
                || coordinateResult.state() == MovementState.MAYBE_MOVING) {
            return coordinateResult.state();
        }

        MovementIntentState intent = activeMovementIntent();
        if (intent != null) {
            MovementState protectedState = coordinateResult.state() == MovementState.STOPPED_STABLE
                    ? MovementState.PATHING_ACTIVE
                    : MovementState.MAYBE_MOVING;
            log.info("🧭 [移动侦测] 最近有移动意图保护：state={} source={} remainMs={}",
                    protectedState, intent.source, Math.max(intent.expiresAt - System.currentTimeMillis(), 0));
            return protectedState;
        }

        if (coordinateResult.state() == MovementState.STOPPED_STABLE) {
            log.info("🛑 [移动侦测] 坐标主证据确认停稳：coord={} validSamples={} unknownSamples={}",
                    formatCoordinate(coordinateResult.lastCoordinate()),
                    coordinateResult.validSamples(), coordinateResult.unknownSamples());
            return MovementState.STOPPED_STABLE;
        }

        boolean pixelMoving = detectMovementByPixelDiff();
        if (pixelMoving) {
            log.info("🏃 [移动侦测] 坐标未确认移动，但像素检测仍有变化，判定 MAYBE_MOVING");
            return MovementState.MAYBE_MOVING;
        }

        log.info("❔ [移动侦测] 证据不足：coordState={} validSamples={} unknownSamples={}",
                coordinateResult.state(), coordinateResult.validSamples(), coordinateResult.unknownSamples());
        return MovementState.UNKNOWN;
    }

    private CoordinateProbeResult detectMovementByCoordinate() {
        long startAt = System.currentTimeMillis();
        long deadline = startAt + COORD_DETECT_WINDOW_MS;
        int validSamples = 0;
        int unknownSamples = 0;
        int maybeMoveHits = 0;
        long previousAt = 0L;
        MapCoordinate first = null;
        MapCoordinate previous = null;

        while (System.currentTimeMillis() < deadline) {
            checkpointMovementProbe();

            MapCoordinate current = miniMapCoordinateReader.readCurrentCoordinate().orElse(null);
            if (current == null) {
                unknownSamples++;
                if (!TaskSleep.sleep(MOVE_SAMPLE_INTERVAL_MS)) {
                    return new CoordinateProbeResult(MovementState.UNKNOWN, previous, validSamples, unknownSamples);
                }
                checkpointMovementProbe();
                continue;
            }

            validSamples++;
            if (first == null) {
                first = current;
                previous = current;
                previousAt = System.currentTimeMillis();
            } else if (isDifferentCoordinate(previous, current)) {
                long now = System.currentTimeMillis();
                int distance = coordinateDistance(previous, current);
                long elapsedMs = Math.max(now - previousAt, 1);
                if (isStrongCoordinateMovement(distance, elapsedMs)) {
                    log.info("🏃 [移动侦测] 坐标速度确认移动：{} -> {} distance={} elapsedMs={} validSamples={} unknownSamples={}",
                            formatCoordinate(previous), formatCoordinate(current), distance, elapsedMs, validSamples, unknownSamples);
                    return new CoordinateProbeResult(MovementState.MOVING, current, validSamples, unknownSamples);
                }

                maybeMoveHits++;
                log.info("🟡 [移动侦测] 坐标轻微变化：{} -> {} distance={} elapsedMs={} maybeHits={} validSamples={} unknownSamples={}",
                        formatCoordinate(previous), formatCoordinate(current), distance, elapsedMs,
                        maybeMoveHits, validSamples, unknownSamples);
                previous = current;
                previousAt = now;
                if (maybeMoveHits >= COORD_MAYBE_MOVE_HITS_FOR_MOVING) {
                    return new CoordinateProbeResult(MovementState.MAYBE_MOVING, current, validSamples, unknownSamples);
                }
            } else {
                previous = current;
                previousAt = System.currentTimeMillis();
            }

            if (!TaskSleep.sleep(MOVE_SAMPLE_INTERVAL_MS)) {
                return new CoordinateProbeResult(MovementState.UNKNOWN, previous, validSamples, unknownSamples);
            }
            checkpointMovementProbe();
        }

        if (validSamples >= COORD_MIN_STABLE_SAMPLES) {
            log.info("🛑 [移动侦测] 坐标稳定，直接确认停稳：coord={} validSamples={} unknownSamples={} elapsedMs={}",
                    formatCoordinate(first), validSamples, unknownSamples, System.currentTimeMillis() - startAt);
            return new CoordinateProbeResult(MovementState.STOPPED_STABLE, previous, validSamples, unknownSamples);
        }

        log.info("❔ [移动侦测] 坐标样本不足，回退像素检测：validSamples={} unknownSamples={}",
                validSamples, unknownSamples);
        return new CoordinateProbeResult(MovementState.UNKNOWN, previous, validSamples, unknownSamples);
    }

    private boolean detectMovementByPixelDiff() {
        int[] pics = coordinateHelper.getScaledRect(20, 400, 30, 30);
        int x1 = pics[0], y1 = pics[1], x2 = pics[2], y2 = pics[3];

        long deadline = System.currentTimeMillis() + PIXEL_FALLBACK_WINDOW_MS;
        int attempts = 0;
        int changedHits = 0;

        while (System.currentTimeMillis() < deadline) {
            checkpointMovementProbe();
            BufferedImage frame1 = tracker.captureToMemory("moving-check-frame1", x1, y1, x2, y2);
            if (frame1 == null) {
                if (!TaskSleep.sleep(MOVE_SAMPLE_INTERVAL_MS)) return false;
                checkpointMovementProbe();
                continue;
            }

            if (!TaskSleep.sleep(MOVE_SAMPLE_INTERVAL_MS)) {
                frame1.flush();
                return false;
            }
            checkpointMovementProbe();

            BufferedImage frame2 = tracker.captureToMemory("moving-check-frame2", x1, y1, x2, y2);
            if (frame2 == null) {
                frame1.flush();
                continue;
            }

            attempts++;
            boolean changed = !ImageFinder.isMatch(frame1, frame2, MOVE_DIFF_RATIO);

            if (changed) {
                changedHits++;
                // 🌟 核心改造：极速放行！一旦满 2 次立刻打断循环，绝不傻等！
                if (changedHits >= FAST_PASS_HITS) {
                    // log.info("🏃 [移动侦测] 提前确认跑动！(耗时: {}ms)", System.currentTimeMillis() - (deadline - MOVE_DETECT_WINDOW_MS));
                    frame1.flush();
                    frame2.flush();
                    return true;
                }
            }

            frame1.flush();
            frame2.flush();
            checkpointMovementProbe();
        }

        // 如果死等了 3.2 秒，变动次数还是没达到 2 次，那就是彻彻底底的真停了！
        log.info("🛑 [移动侦测] 像素兜底结束 -> {}秒内仅变动 {} 次，判定为：已停下",
                PIXEL_FALLBACK_WINDOW_MS / 1000.0, changedHits);
        return false;
    }

    private boolean isDifferentCoordinate(MapCoordinate a, MapCoordinate b) {
        if (a == null || b == null) {
            return false;
        }
        return a.getX() != b.getX() || a.getY() != b.getY();
    }

    private boolean isStrongCoordinateMovement(int distance, long elapsedMs) {
        return distance >= COORD_STRONG_MOVE_DISTANCE
                || (distance >= COORD_FAST_MOVE_DISTANCE && elapsedMs <= COORD_FAST_MOVE_WINDOW_MS);
    }

    private int coordinateDistance(MapCoordinate a, MapCoordinate b) {
        if (a == null || b == null) {
            return 0;
        }
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    private String formatCoordinate(MapCoordinate coordinate) {
        if (coordinate == null) {
            return "-";
        }
        return coordinate.getX() + "," + coordinate.getY();
    }

    private String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "unknown" : reason;
    }

    public boolean isInBattle() {
        log.warn("isInBattle() is not implemented yet, defaulting to false");
        return false;
    }

    private String safeLatencyValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void checkpointMovementProbe() {
        checkpointStateProbe("movement detection");
    }

    private void checkpointStateProbe(String action) {
        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, action + " interrupted");
    }

    private MovementIntentState activeMovementIntent() {
        MovementIntentState state = movementIntentStates.get(windowKey());
        if (state == null) {
            return null;
        }
        if (System.currentTimeMillis() <= state.expiresAt) {
            return state;
        }
        return null;
    }

    private String windowKey() {
        return windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
    }

    public enum MovementState {
        MOVING,
        PATHING_ACTIVE,
        MAYBE_MOVING,
        STOPPED_STABLE,
        UNKNOWN
    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    private static class CoordinateProbeResult {


        MovementState state;


        MapCoordinate lastCoordinate;


        int validSamples;


        int unknownSamples;


    }

    private static class MovementIntentState {
        private String source = "unknown";
        private long expiresAt;
    }
}
