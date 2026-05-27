package com.bot.dhxy.task;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;


import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DebugMapCalibratorTask implements GameTask {

    private static final int WINDOW_CAPTURE_W = 1024;
    private static final int WINDOW_CAPTURE_H = 768;
    private static final long PREPARE_DELAY_MS = 5_000L;
    private static final long STABLE_MOUSE_MS = 3_000L;
    private static final long POLL_MS = 50L;
    private static final Pattern COORD_PATTERN = Pattern.compile("[\\[\\(（]?\\s*(\\d{1,3})\\s*[,，]\\s*(\\d{1,3})\\s*[\\]\\)）]?");

    private final BotProperties botProperties;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer textRecognizer;
    private final WindowScopedTempPath windowScopedTempPath;

    private volatile boolean stopped;

    public DebugMapCalibratorTask(BotProperties botProperties,
                                  GameClientTracker tracker,
                                  CoordinateHelper coordinateHelper,
                                  TextRecognizer textRecognizer,
                                  WindowScopedTempPath windowScopedTempPath) {
        this.botProperties = botProperties;
        this.tracker = tracker;
        this.coordinateHelper = coordinateHelper;
        this.textRecognizer = textRecognizer;
        this.windowScopedTempPath = windowScopedTempPath;
    }

    @Override
    public String getTaskCode() {
        return "debug_map_calibrator";
    }

    @Override
    public String getTaskName() {
        return "地图校准";
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext context) {
        String prefix = context == null ? "[window=unknown]" : context.getLogPrefix();
        String mapName = normalizeMapName(botProperties.getDebugMapCalibratorMapName());
        if (mapName == null) {
            log.warn("{} [map-calibrator] missing map name from UI", prefix);
            return TaskRunResult.FAILED;
        }
        if (!tracker.refreshWindowState()) {
            log.warn("{} [map-calibrator] window binding/base refresh failed", prefix);
            return TaskRunResult.FAILED;
        }

        log.info("{} [map-calibrator] start map={}：打开当前地图，把鼠标放到第一个已知坐标点，保持不动 {}ms",
                prefix, mapName, STABLE_MOUSE_MS);
        if (!waitBeforeFirstSample(context, prefix, mapName)) {
            return TaskRunResult.STOPPED;
        }
        SamplePoint p1 = captureStableSample(context, prefix, "A");
        if (p1 == null) {
            return stopped || Thread.currentThread().isInterrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
        }

        log.info("{} [map-calibrator] point A ok: relative=({}, {}) logic=({}, {}) rawPath={}",
                prefix, p1.relativeX(), p1.relativeY(), p1.logicX(), p1.logicY(), p1.rawPath());
        log.info("{} [map-calibrator] move mouse to second coordinate point, keep still {}ms",
                prefix, STABLE_MOUSE_MS);
        SamplePoint p2 = captureStableSample(context, prefix, "B");
        if (p2 == null) {
            return stopped || Thread.currentThread().isInterrupted() ? TaskRunResult.STOPPED : TaskRunResult.FAILED;
        }

        if (p1.logicX() == p2.logicX() || p1.logicY() == p2.logicY()) {
            log.warn("{} [map-calibrator] two points must differ on both X and Y: A=({}, {}) B=({}, {})",
                    prefix, p1.logicX(), p1.logicY(), p2.logicX(), p2.logicY());
            return TaskRunResult.FAILED;
        }

        double scaleX = (double) (p2.relativeX() - p1.relativeX()) / (p2.logicX() - p1.logicX());
        double scaleY = (double) (p2.relativeY() - p1.relativeY()) / (p2.logicY() - p1.logicY());
        int zeroOffsetX = (int) Math.round(p1.relativeX() - p1.logicX() * scaleX);
        int zeroOffsetY = (int) Math.round(p1.relativeY() - p1.logicY() * scaleY);
        CoordinateHelper.MapTransform transform = new CoordinateHelper.MapTransform(
                zeroOffsetX, zeroOffsetY, scaleX, scaleY);
        coordinateHelper.saveNewMapConfig(mapName, transform);

        log.info("{} [map-calibrator] saved map={} zeroOffset=({}, {}) scale=({}, {})",
                prefix, mapName, zeroOffsetX, zeroOffsetY, scaleX, scaleY);
        return TaskRunResult.SUCCESS;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    private boolean waitBeforeFirstSample(TaskExecutionContext context, String prefix, String mapName) {
        long seconds = PREPARE_DELAY_MS / 1_000L;
        log.info("{} [map-calibrator] prepare window: map={} wait={}s; open map and move mouse to point A now",
                prefix, mapName, seconds);
        long remaining = PREPARE_DELAY_MS;
        while (remaining > 0) {
            if (stopped || Thread.currentThread().isInterrupted()) {
                return false;
            }
            if (context != null) {
                context.throwIfStopRequested();
            }
            long step = Math.min(1_000L, remaining);
            sleep(step);
            remaining -= step;
            if (remaining > 0) {
                log.info("{} [map-calibrator] prepare countdown: {}s", prefix, Math.ceil(remaining / 1_000.0));
            }
        }
        Toolkit.getDefaultToolkit().beep();
        log.info("{} [map-calibrator] prepare done; start point A stable detection", prefix);
        return true;
    }

    private SamplePoint captureStableSample(TaskExecutionContext context, String prefix, String pointName) {
        Point last = currentMouse();
        if (last == null) {
            log.warn("{} [map-calibrator] cannot read mouse pointer for point {}", prefix, pointName);
            return null;
        }
        long stableSince = System.currentTimeMillis();
        while (!stopped && !Thread.currentThread().isInterrupted()) {
            if (context != null) {
                context.throwIfStopRequested();
            }
            sleep(POLL_MS);
            Point current = currentMouse();
            if (current == null) {
                continue;
            }
            if (!current.equals(last)) {
                last = current;
                stableSince = System.currentTimeMillis();
                continue;
            }
            if (System.currentTimeMillis() - stableSince >= STABLE_MOUSE_MS) {
                Toolkit.getDefaultToolkit().beep();
                return captureSampleAtMouse(prefix, pointName, current);
            }
        }
        return null;
    }

    private SamplePoint captureSampleAtMouse(String prefix, String pointName, Point mousePhysical) {
        double ratio = coordinateHelper.getScaleRatio();
        int relativeX = (int) Math.round(mousePhysical.x / ratio) - tracker.getWindowBaseX();
        int relativeY = (int) Math.round(mousePhysical.y / ratio) - tracker.getWindowBaseY();
        int[] rect = coordinateHelper.getScaledRect(0, 0, WINDOW_CAPTURE_W, WINDOW_CAPTURE_H);
        String rawPath = windowScopedTempPath.resolve("map_calibrator_" + pointName + "_raw.png");
        if (!tracker.captureToFile("map-calibrator-" + pointName, rawPath, rect[0], rect[1], rect[2], rect[3])) {
            log.warn("{} [map-calibrator] capture failed for point {} path={}", prefix, pointName, rawPath);
            return null;
        }

        List<OcrWordResult> words = textRecognizer.getAllTextResults(rawPath);
        CoordinateCandidate best = findNearestCoordinateCandidate(words, relativeX, relativeY, prefix, pointName);
        if (best == null) {
            log.warn("{} [map-calibrator] no coordinate candidate for point {} rawPath={}",
                    prefix, pointName, rawPath);
            return null;
        }
        log.info("{} [map-calibrator] point {} selected candidate text='{}' logic=({}, {}) wordCenter=({}, {}) mouseRelative=({}, {}) distance={}",
                prefix, pointName, best.text(), best.logicX(), best.logicY(),
                best.centerX(), best.centerY(), relativeX, relativeY, best.distance());
        return new SamplePoint(relativeX, relativeY, best.logicX(), best.logicY(), rawPath);
    }

    private CoordinateCandidate findNearestCoordinateCandidate(List<OcrWordResult> words,
                                                               int relativeX,
                                                               int relativeY,
                                                               String prefix,
                                                               String pointName) {
        if (words == null || words.isEmpty()) {
            return null;
        }
        CoordinateCandidate best = null;
        for (OcrWordResult word : words) {
            if (word == null || word.getText() == null) {
                continue;
            }
            String text = word.getText().trim();
            if (text.contains("等级") || text.contains("级")) {
                continue;
            }
            Matcher matcher = COORD_PATTERN.matcher(text);
            while (matcher.find()) {
                int logicX = Integer.parseInt(matcher.group(1));
                int logicY = Integer.parseInt(matcher.group(2));
                int distance = Math.abs(word.getX() - relativeX) + Math.abs(word.getY() - relativeY);
                CoordinateCandidate candidate = new CoordinateCandidate(
                        text, logicX, logicY, word.getX(), word.getY(), distance);
                log.info("{} [map-calibrator] point {} candidate text='{}' logic=({}, {}) center=({}, {}) distance={}",
                        prefix, pointName, text, logicX, logicY, word.getX(), word.getY(), distance);
                if (best == null || candidate.distance() < best.distance()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private Point currentMouse() {
        return MouseInfo.getPointerInfo() == null ? null : MouseInfo.getPointerInfo().getLocation();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String normalizeMapName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    private static class SamplePoint {


        int relativeX;


        int relativeY;


        int logicX;


        int logicY;


        String rawPath;


    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    private static class CoordinateCandidate {


        String text;


        int logicX;


        int logicY;


        int centerX;


        int centerY;


        int distance;


    }
}
