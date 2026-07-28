package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Fixed-crop, short-lived movement facts; performs no OCR or map/coordinate interpretation. */
@Slf4j
@Component
public final class LocalMovementFactMechanics {

    private static final long PIXEL_WINDOW_MS = 1200L;
    private static final long SAMPLE_MS = 300L;
    private static final int REQUIRED_HITS = 2;
    private static final double DIFF_RATIO = 0.05;
    private static final int COORD_STRIP_X = 46;
    private static final int COORD_STRIP_Y = 59;
    private static final int COORD_STRIP_W = 178;
    private static final int COORD_STRIP_H = 35;
    private static final long DEFAULT_INTENT_MS = 5500L;

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowTaskContextHolder contextHolder;
    private final Map<String, IntentFact> intentFacts = new ConcurrentHashMap<>();

    public LocalMovementFactMechanics(GameClientTracker tracker,
                                      CoordinateHelper coordinateHelper,
                                      WindowTaskContextHolder contextHolder) {
        this.tracker = tracker;
        this.coordinateHelper = coordinateHelper;
        this.contextHolder = contextHolder;
    }

    public BufferedImage captureCoordinateStrip() {
        int[] rect = coordinateHelper.getScaledRect(
                COORD_STRIP_X, COORD_STRIP_Y, COORD_STRIP_W, COORD_STRIP_H);
        return tracker.captureToMemory("pathing-coordinate-strip", rect[0], rect[1], rect[2], rect[3]);
    }

    public boolean coordinateStripChanged(BufferedImage baseline, String source) {
        if (baseline == null) {
            return false;
        }
        long deadline = System.currentTimeMillis() + 1000L;
        while (System.currentTimeMillis() < deadline) {
            BufferedImage current = captureCoordinateStrip();
            try {
                if (current != null && !ImageFinder.isMatch(baseline, current, DIFF_RATIO)) {
                    log.info("local coordinate-strip movement fact changed: source={}", source);
                    return true;
                }
            } finally {
                if (current != null) {
                    current.flush();
                }
            }
            if (!TaskSleep.sleep(200L)) {
                return false;
            }
        }
        return false;
    }

    public boolean edgePixelsChanged(String source) {
        int[] left = coordinateHelper.getScaledRect(20, 400, 30, 30);
        int[] right = coordinateHelper.getScaledRect(999, 176, 30, 30);
        int x1 = Math.min(left[0], right[0]);
        int y1 = Math.min(left[1], right[1]);
        int x2 = Math.max(left[2], right[2]);
        int y2 = Math.max(left[3], right[3]);
        long deadline = System.currentTimeMillis() + PIXEL_WINDOW_MS;
        int bothHits = 0;
        int leftHits = 0;
        int rightHits = 0;
        while (System.currentTimeMillis() < deadline) {
            BufferedImage first = tracker.captureToMemory("pathing-edge-first", x1, y1, x2, y2);
            if (first == null) {
                if (!TaskSleep.sleep(SAMPLE_MS)) {
                    return false;
                }
                continue;
            }
            if (!TaskSleep.sleep(SAMPLE_MS)) {
                first.flush();
                return false;
            }
            BufferedImage second = tracker.captureToMemory("pathing-edge-second", x1, y1, x2, y2);
            try {
                if (second == null) {
                    continue;
                }
                boolean leftChanged = changed(first, second, left, x1, y1);
                boolean rightChanged = changed(first, second, right, x1, y1);
                bothHits += leftChanged && rightChanged ? 1 : 0;
                leftHits += leftChanged ? 1 : 0;
                rightHits += rightChanged ? 1 : 0;
                if (bothHits >= REQUIRED_HITS || leftHits >= REQUIRED_HITS || rightHits >= REQUIRED_HITS) {
                    log.info("local edge movement fact changed: source={} both={} left={} right={}",
                            source, bothHits, leftHits, rightHits);
                    return true;
                }
            } finally {
                first.flush();
                if (second != null) {
                    second.flush();
                }
            }
        }
        return false;
    }

    public void recordIntent(String source, Long protectionMs) {
        long duration = protectionMs == null ? DEFAULT_INTENT_MS : protectionMs;
        intentFacts.put(windowKey(), new IntentFact(source, System.currentTimeMillis() + duration));
    }

    private boolean changed(BufferedImage first, BufferedImage second, int[] rect, int x1, int y1) {
        BufferedImage a = first.getSubimage(rect[0] - x1, rect[1] - y1,
                rect[2] - rect[0], rect[3] - rect[1]);
        BufferedImage b = second.getSubimage(rect[0] - x1, rect[1] - y1,
                rect[2] - rect[0], rect[3] - rect[1]);
        return !ImageFinder.isMatch(a, b, DIFF_RATIO);
    }

    private String windowKey() {
        return contextHolder.rawCurrent().map(c -> c.getWindowId()).orElse("default");
    }

    private record IntentFact(String source, long expiresAt) {
    }
}
