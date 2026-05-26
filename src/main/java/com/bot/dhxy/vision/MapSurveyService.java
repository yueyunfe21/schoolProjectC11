package com.bot.dhxy.vision;

import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.vision.MiniMapCoordinateReader.MapLabelTemplateMatch;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapSurveyService {

    private static final int COORD_SCAN_X = 46;
    private static final int COORD_SCAN_Y = 59;
    private static final int COORD_SCAN_W = 178;
    private static final int COORD_SCAN_H = 35;
    private static final int CLIENT_CENTER_X = 512;
    private static final int CLIENT_CENTER_Y = 384;
    private static final double WORLD_TILE_PIXEL_X = 20.0;
    private static final double WORLD_TILE_PIXEL_Y = -20.0;
    private static final double MAP_LABEL_MATCH_THRESHOLD = 0.62;
    private static final long BOUNDARY_MOUSE_PREPARE_MS = 3_000L;
    private static final long CENTER_MOUSE_PREPARE_MS = 3_000L;
    private static final long CORRECTION_MOUSE_PREPARE_MS = 3_000L;
    private static final int CORRECTION_LARGE_ERROR_THRESHOLD = 500;
    private static final int LOCAL_FIT_MIN_SAMPLES = 3;
    private static final int LOCAL_FIT_MAX_SAMPLES = 8;
    private static final double LOCAL_FIT_MAX_MAP_DISTANCE = 18.0;
    private static final double LOCAL_FIT_SCREEN_CLUSTER_RADIUS = 220.0;
    private static final double LOCAL_FIT_MAX_WEIGHTED_RESIDUAL = 95.0;
    private static final double BOUNDARY_AXIS_EPSILON = 0.0001;
    private static final Path MAP_LABEL_TEMPLATE_DIR = Path.of("images", "template", "map_label")
            .toAbsolutePath()
            .normalize();
    private static final Path CAMERA_BOUNDS_CONFIG = Path.of("config", "map_camera_bounds.json")
            .toAbsolutePath()
            .normalize();
    private static final Path MAP_SURVEY_DEBUG_DIR = Path.of("images", "temp", "map_survey")
            .toAbsolutePath()
            .normalize();

    private final BoundWindowCaptureService boundWindowCaptureService;
    private final MiniMapCoordinateReader miniMapCoordinateReader;
    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final MultiWindowTaskManager multiWindowTaskManager;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<CalibrationUndoEntry>> undoHistoryByMap = new HashMap<>();

    public SurveyResult saveMapLabelSample(WindowTaskSnapshot snapshot, String mapName) {
        String normalizedMapName = normalizeMapName(mapName);
        if (normalizedMapName == null) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "地图名为空");
        }

        Optional<BufferedImage> strip = captureCoordinateStrip(snapshot);
        if (strip.isEmpty()) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "小地图坐标条截图失败");
        }
        try {
            Optional<BufferedImage> label = miniMapCoordinateReader.extractCleanMapLabelImageFromCoordinateStrip(strip.get());
            if (label.isEmpty()) {
                return SurveyResult.failed(snapshotWindowId(snapshot), "没有切出小地图名字样本");
            }
            try {
                Files.createDirectories(MAP_LABEL_TEMPLATE_DIR);
                Path output = MAP_LABEL_TEMPLATE_DIR.resolve(safeFileName(normalizedMapName) + ".png");
                ImageIO.write(label.get(), "png", output.toFile());
                log.info("[map-survey] saved map label sample windowId={} map={} path={} size={}x{}",
                        snapshotWindowId(snapshot), normalizedMapName, output, label.get().getWidth(), label.get().getHeight());
                return SurveyResult.success(snapshotWindowId(snapshot),
                        "地图名样本已保存: " + normalizedMapName + " -> " + output);
            } finally {
                label.ifPresent(BufferedImage::flush);
            }
        } catch (Exception e) {
            log.warn("[map-survey] save map label sample failed: windowId={} map={} reason={}",
                    snapshotWindowId(snapshot), normalizedMapName, e.getMessage(), e);
            return SurveyResult.failed(snapshotWindowId(snapshot), "保存地图名样本失败: " + e.getMessage());
        } finally {
            strip.get().flush();
        }
    }

    public SurveyResult recognizeCurrentMapLabel(WindowTaskSnapshot snapshot) {
        Optional<BufferedImage> strip = captureCoordinateStrip(snapshot);
        if (strip.isEmpty()) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "小地图坐标条截图失败");
        }
        try {
            Optional<MapLabelTemplateMatch> match = recognizeMapLabelFromStrip(snapshot, strip.get());
            if (match.isEmpty()) {
                return SurveyResult.failed(snapshotWindowId(snapshot), "没有匹配到地图名样本");
            }
            MapLabelTemplateMatch best = match.get();
            boolean ok = best.score() >= MAP_LABEL_MATCH_THRESHOLD;
            String message = String.format("地图名识别%s: %s score=%.3f threshold=%.2f",
                    ok ? "成功" : "低分", best.mapName(), best.score(), MAP_LABEL_MATCH_THRESHOLD);
            return ok
                    ? SurveyResult.success(snapshotWindowId(snapshot), message)
                    : SurveyResult.failed(snapshotWindowId(snapshot), message);
        } finally {
            strip.get().flush();
        }
    }

    public SurveyResult recordCameraBoundary(WindowTaskSnapshot snapshot, String mapName, CameraBoundaryDirection direction) {
        String normalizedMapName = normalizeMapName(mapName);
        if (normalizedMapName == null) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "地图名为空");
        }
        if (direction == null) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "边界方向为空");
        }

        log.info("[map-survey] boundary prepare: windowId={} map={} direction={} waitMs={} move mouse to character now",
                snapshotWindowId(snapshot), normalizedMapName, direction.displayName(), BOUNDARY_MOUSE_PREPARE_MS);
        if (!sleepInterruptible(BOUNDARY_MOUSE_PREPARE_MS)) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "记录边界已中断");
        }

        Optional<BufferedImage> strip = captureCoordinateStrip(snapshot);
        if (strip.isEmpty()) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "小地图坐标条截图失败");
        }
        try {
            MiniMapCoordinateReader.MiniMapSnapshot location =
                    miniMapCoordinateReader.readLocationSnapshotFromCoordinateStrip(strip.get(), false, true);
            MapCoordinate coordinate = location.coordinate();
            if (coordinate == null) {
                return SurveyResult.failed(snapshotWindowId(snapshot), "当前坐标识别失败");
            }

            WindowNativeBinding binding = resolveLiveBinding(snapshot);
            Point mouse = MouseInfo.getPointerInfo() == null ? null : MouseInfo.getPointerInfo().getLocation();
            if (binding == null || !binding.hasGeometry() || mouse == null) {
                return SurveyResult.failed(snapshotWindowId(snapshot), "窗口几何或鼠标位置不可用");
            }

            double ratio = coordinateHelper.getScaleRatio();
            int relativeX = (int) Math.round(mouse.x / ratio) - binding.getX();
            int relativeY = (int) Math.round(mouse.y / ratio) - binding.getY();

            Map<String, CameraBounds> all = loadCameraBounds();
            CameraBounds current = all.getOrDefault(normalizedMapName, new CameraBounds());
            pushUndo(normalizedMapName, CalibrationUndoEntry.forBoundary(direction, current.scalarValue(direction), current.samplesFor(direction)));
            double anchorX = current.centerAnchorXOrDefault();
            double anchorY = current.centerAnchorYOrDefault();
            double cameraX = coordinate.getX() - ((relativeX - anchorX) / WORLD_TILE_PIXEL_X);
            double cameraY = coordinate.getY() - ((relativeY - anchorY) / WORLD_TILE_PIXEL_Y);
            CameraBounds updated = current.with(direction, coordinate, cameraX, cameraY);
            all.put(normalizedMapName, updated);
            saveCameraBounds(all);

            String message = String.format("记录%s边界: map=%s coord=(%d,%d) mouseRel=(%d,%d) anchor=(%.1f,%.1f) camera=(%.2f,%.2f) samples=%d",
                    direction.displayName(), normalizedMapName, coordinate.getX(), coordinate.getY(),
                    relativeX, relativeY, anchorX, anchorY, cameraX, cameraY, updated.sampleCount(direction));
            log.info("[map-survey] {}", message);
            return SurveyResult.success(snapshotWindowId(snapshot), message);
        } catch (Exception e) {
            log.warn("[map-survey] record camera boundary failed: windowId={} map={} direction={} reason={}",
                    snapshotWindowId(snapshot), normalizedMapName, direction, e.getMessage(), e);
            return SurveyResult.failed(snapshotWindowId(snapshot), "记录镜头边界失败: " + e.getMessage());
        } finally {
            strip.get().flush();
        }
    }

    public SurveyResult recordCenterAnchor(WindowTaskSnapshot snapshot, String mapName) {
        String normalizedMapName = normalizeMapName(mapName);
        if (normalizedMapName == null) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "地图名为空");
        }

        log.info("[map-survey] center anchor prepare: windowId={} map={} waitMs={} move mouse to character now",
                snapshotWindowId(snapshot), normalizedMapName, CENTER_MOUSE_PREPARE_MS);
        if (!sleepInterruptible(CENTER_MOUSE_PREPARE_MS)) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "记录中心点已中断");
        }

        WindowNativeBinding binding = resolveLiveBinding(snapshot);
        Point mouse = MouseInfo.getPointerInfo() == null ? null : MouseInfo.getPointerInfo().getLocation();
        if (binding == null || !binding.hasGeometry() || mouse == null) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "窗口几何或鼠标位置不可用");
        }

        try {
            double ratio = coordinateHelper.getScaleRatio();
            int relativeX = (int) Math.round(mouse.x / ratio) - binding.getX();
            int relativeY = (int) Math.round(mouse.y / ratio) - binding.getY();

            Map<String, CameraBounds> all = loadCameraBounds();
            CameraBounds current = all.getOrDefault(normalizedMapName, new CameraBounds());
            pushUndo(normalizedMapName, new CalibrationUndoEntry("CENTER", null,
                    current.centerAnchorX(), current.centerAnchorY(), 0, 0, null));
            CameraBounds updated = current.withCenterAnchor((double) relativeX, (double) relativeY);
            all.put(normalizedMapName, updated);
            saveCameraBounds(all);

            String message = String.format("记录中心点: map=%s anchor=(%d,%d)", normalizedMapName, relativeX, relativeY);
            log.info("[map-survey] {}", message);
            return SurveyResult.success(snapshotWindowId(snapshot), message);
        } catch (Exception e) {
            log.warn("[map-survey] record center anchor failed: windowId={} map={} reason={}",
                    snapshotWindowId(snapshot), normalizedMapName, e.getMessage(), e);
            return SurveyResult.failed(snapshotWindowId(snapshot), "记录中心点失败: " + e.getMessage());
        }
    }

    public SurveyResult projectCurrentPlayerPoint(WindowTaskSnapshot snapshot, String mapName) {
        return projectCurrentPlayerPoint(snapshot, mapName, false);
    }

    public SurveyResult moveMouseToProjectedPlayerPoint(WindowTaskSnapshot snapshot, String mapName) {
        return projectCurrentPlayerPoint(snapshot, mapName, true, false);
    }

    public SurveyResult moveMouseToProjectedPlayerPointByCurrentMap(WindowTaskSnapshot snapshot) {
        return projectCurrentPlayerPoint(snapshot, null, true, true);
    }

    public SurveyResult recordPlayerPointCorrectionByCurrentMap(WindowTaskSnapshot snapshot) {
        Optional<ProjectionContext> context = buildProjectionContext(snapshot, null, true);
        if (context.isEmpty()) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "基础角色点推算失败，不能记录修正点");
        }

        ProjectionContext projection = context.get();
        log.info("[map-survey] correction prepare: windowId={} map={} coord=({}, {}) waitMs={} move mouse to real player point now",
                snapshotWindowId(snapshot), projection.mapName(), projection.coordinate().getX(), projection.coordinate().getY(),
                CORRECTION_MOUSE_PREPARE_MS);
        if (!sleepInterruptible(CORRECTION_MOUSE_PREPARE_MS)) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "记录修正点已中断");
        }

        Optional<ProjectionContext> freshContext = buildProjectionContext(snapshot, null, true);
        if (freshContext.isEmpty()) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "倒计时后重新读取角色点失败，未保存修正点");
        }
        ProjectionContext freshProjection = freshContext.get();
        if (!projection.mapName().equals(freshProjection.mapName())
                || projection.coordinate().getX() != freshProjection.coordinate().getX()
                || projection.coordinate().getY() != freshProjection.coordinate().getY()) {
            log.info("[map-survey] correction context refreshed after wait: windowId={} before={}/({},{}) after={}/({},{})",
                    snapshotWindowId(snapshot),
                    projection.mapName(), projection.coordinate().getX(), projection.coordinate().getY(),
                    freshProjection.mapName(), freshProjection.coordinate().getX(), freshProjection.coordinate().getY());
        }
        projection = freshProjection;

        WindowNativeBinding binding = resolveLiveBinding(snapshot);
        Point mouse = MouseInfo.getPointerInfo() == null ? null : MouseInfo.getPointerInfo().getLocation();
        if (binding == null || !binding.hasGeometry() || mouse == null) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "窗口几何或鼠标位置不可用");
        }

        try {
            double ratio = coordinateHelper.getScaleRatio();
            int actualRelX = (int) Math.round(mouse.x / ratio) - binding.getX();
            int actualRelY = (int) Math.round(mouse.y / ratio) - binding.getY();
            int errorX = actualRelX - projection.baseRelativeX();
            int errorY = actualRelY - projection.baseRelativeY();
            if (Math.abs(errorX) > CORRECTION_LARGE_ERROR_THRESHOLD
                    || Math.abs(errorY) > CORRECTION_LARGE_ERROR_THRESHOLD) {
                String message = String.format("修正点误差过大，未保存: map=%s coord=(%d,%d) baseRel=(%d,%d) actualRel=(%d,%d) error=(%+d,%+d) threshold=%d",
                        projection.mapName(), projection.coordinate().getX(), projection.coordinate().getY(),
                        projection.baseRelativeX(), projection.baseRelativeY(), actualRelX, actualRelY,
                        errorX, errorY, CORRECTION_LARGE_ERROR_THRESHOLD);
                log.warn("[map-survey] {}", message);
                return SurveyResult.failed(snapshotWindowId(snapshot), message);
            }

            Map<String, CameraBounds> all = loadCameraBounds();
            CameraBounds current = all.getOrDefault(projection.mapName(), new CameraBounds());
            pushUndo(projection.mapName(), new CalibrationUndoEntry("CORRECTION", null, null, null, 0, current.correctionCount(), null));
            CameraBounds updated = current.withCorrection(new PointCorrectionSample(
                    (double) projection.coordinate().getX(),
                    (double) projection.coordinate().getY(),
                    (double) projection.baseRelativeX(),
                    (double) projection.baseRelativeY(),
                    (double) actualRelX,
                    (double) actualRelY,
                    (double) errorX,
                    (double) errorY));
            all.put(projection.mapName(), updated);
            saveCameraBounds(all);

            String message = String.format("记录修正点: map=%s coord=(%d,%d) baseRel=(%d,%d) actualRel=(%d,%d) error=(%+d,%+d) corrections=%d",
                    projection.mapName(), projection.coordinate().getX(), projection.coordinate().getY(),
                    projection.baseRelativeX(), projection.baseRelativeY(), actualRelX, actualRelY,
                    errorX, errorY, updated.correctionCount());
            log.info("[map-survey] {}", message);
            return SurveyResult.success(snapshotWindowId(snapshot), message);
        } catch (Exception e) {
            log.warn("[map-survey] record correction failed: windowId={} map={} reason={}",
                    snapshotWindowId(snapshot), projection.mapName(), e.getMessage(), e);
            return SurveyResult.failed(snapshotWindowId(snapshot), "记录修正点失败: " + e.getMessage());
        }
    }

    public SurveyResult moveMouseToCorrectedPlayerPointByCurrentMap(WindowTaskSnapshot snapshot) {
        return projectCurrentPlayerPoint(snapshot, null, true, true, true);
    }

    public SurveyResult undoLastMapSurveyRecordByCurrentMap(WindowTaskSnapshot snapshot) {
        Optional<String> mapName = recognizeCurrentMapName(snapshot);
        if (mapName.isEmpty()) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "当前地图识别失败，不能撤销上次记录");
        }

        try {
            Map<String, CameraBounds> all = loadCameraBounds();
            CameraBounds current = all.get(mapName.get());
            CalibrationUndoEntry undoEntry = popUndo(mapName.get());
            if (current == null || undoEntry == null) {
                return SurveyResult.failed(snapshotWindowId(snapshot), "当前地图没有可撤销的新记录: " + mapName.get());
            }
            UndoResult undo = current.undo(undoEntry);
            CameraBounds updated = undo.bounds();
            all.put(mapName.get(), updated);
            saveCameraBounds(all);

            String message = String.format("撤销上次记录: map=%s type=%s corrections=%d",
                    mapName.get(), undo.description(), updated.correctionCount());
            log.info("[map-survey] {}", message);
            return SurveyResult.success(snapshotWindowId(snapshot), message);
        } catch (Exception e) {
            log.warn("[map-survey] undo map survey record failed: windowId={} map={} reason={}",
                    snapshotWindowId(snapshot), mapName.get(), e.getMessage(), e);
            return SurveyResult.failed(snapshotWindowId(snapshot), "撤销上次记录失败: " + e.getMessage());
        }
    }

    private SurveyResult projectCurrentPlayerPoint(WindowTaskSnapshot snapshot, String mapName, boolean moveMouse) {
        return projectCurrentPlayerPoint(snapshot, mapName, moveMouse, false, false);
    }

    private SurveyResult projectCurrentPlayerPoint(WindowTaskSnapshot snapshot,
                                                   String mapName,
                                                   boolean moveMouse,
                                                   boolean detectMapName) {
        return projectCurrentPlayerPoint(snapshot, mapName, moveMouse, detectMapName, false);
    }

    private SurveyResult projectCurrentPlayerPoint(WindowTaskSnapshot snapshot,
                                                   String mapName,
                                                   boolean moveMouse,
                                                   boolean detectMapName,
                                                   boolean applyCorrection) {
        Optional<ProjectionContext> context = buildProjectionContext(snapshot, mapName, detectMapName);
        if (context.isEmpty()) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "角色屏幕点推算失败");
        }

        ProjectionContext projection = context.get();
        int relativeX = projection.baseRelativeX();
        int relativeY = projection.baseRelativeY();
        CorrectionDelta correction = CorrectionDelta.empty();
        if (applyCorrection) {
            correction = projection.bounds().correctionAt(projection.coordinate());
            relativeX += correction.deltaX();
            relativeY += correction.deltaY();
        }

        WindowNativeBinding binding = resolveLiveBinding(snapshot);
        if (binding == null || !binding.hasGeometry()) {
            return SurveyResult.failed(snapshotWindowId(snapshot), "窗口几何不可用，不能换算绝对鼠标点");
        }
        int absoluteX = binding.getX() + relativeX;
        int absoluteY = binding.getY() + relativeY;

        String message = String.format("角色屏幕点推算%s: map=%s coord=(%d,%d) boundaryX=(%s,%s) boundaryY=(%s,%s) camera=(%.2f,%.2f) anchor=(%.1f,%.1f) baseRel=(%d,%d) correction=(%+d,%+d dist=%.1f samples=%d) rel=(%d,%d) abs=(%d,%d)",
                applyCorrection ? "[修正]" : "",
                projection.mapName(), projection.coordinate().getX(), projection.coordinate().getY(),
                formatNullable(projection.leftCameraX()), formatNullable(projection.rightCameraX()),
                formatNullable(projection.bottomCameraY()), formatNullable(projection.topCameraY()),
                projection.cameraX(), projection.cameraY(), projection.anchorX(), projection.anchorY(),
                projection.baseRelativeX(), projection.baseRelativeY(),
                correction.deltaX(), correction.deltaY(), correction.nearestDistance(), correction.sampleCount(),
                relativeX, relativeY, absoluteX, absoluteY);
        message += String.format(" cameraState=%s correctionSource=%s correctionDetail=%s",
                cameraState(projection), correction.source(), correction.detail());
        log.info("[map-survey] {}", message);
        if (relativeX < 0 || relativeY < 0 || relativeX >= binding.getWidth() || relativeY >= binding.getHeight()) {
            return SurveyResult.failed(snapshotWindowId(snapshot), message + "；推算点越界，请检查该地图边界/修正点记录");
        }
        if (moveMouse) {
            boolean moved = submitMoveMouseWithWindowContext(snapshot, absoluteX, absoluteY);
            if (!moved) {
                return SurveyResult.failed(snapshotWindowId(snapshot), message + "；鼠标移动提交失败");
            }
            message += "；鼠标已移动";
        }
        return SurveyResult.success(snapshotWindowId(snapshot), message);
    }

    private Optional<ProjectionContext> buildProjectionContext(WindowTaskSnapshot snapshot,
                                                               String mapName,
                                                               boolean detectMapName) {
        String normalizedMapName = normalizeMapName(mapName);
        if (normalizedMapName == null && !detectMapName) {
            return Optional.empty();
        }
        Optional<BufferedImage> strip = captureCoordinateStrip(snapshot);
        if (strip.isEmpty()) {
            return Optional.empty();
        }
        try {
            if (detectMapName) {
                Optional<MapLabelTemplateMatch> match = recognizeMapLabelFromStrip(snapshot, strip.get());
                if (match.isEmpty()) {
                    return Optional.empty();
                }
                MapLabelTemplateMatch best = match.get();
                if (best.score() < MAP_LABEL_MATCH_THRESHOLD) {
                    log.info("[map-survey] map label low score: windowId={} map={} score={} threshold={}",
                            snapshotWindowId(snapshot), best.mapName(), String.format("%.3f", best.score()), MAP_LABEL_MATCH_THRESHOLD);
                    return Optional.empty();
                }
                normalizedMapName = best.mapName();
            }

            MiniMapCoordinateReader.MiniMapSnapshot location =
                    miniMapCoordinateReader.readLocationSnapshotFromCoordinateStrip(strip.get(), false, true);
            MapCoordinate coordinate = location.coordinate();
            if (coordinate == null) {
                return Optional.empty();
            }

            CameraBounds bounds = loadCameraBounds().get(normalizedMapName);
            if (bounds == null || !bounds.hasAnyCalibration()) {
                return Optional.empty();
            }

            Double leftCameraX = bounds.leftCameraXAt(coordinate.getY());
            Double rightCameraX = bounds.rightCameraXAt(coordinate.getY());
            Double bottomCameraY = bounds.bottomCameraYAt(coordinate.getX());
            Double topCameraY = bounds.topCameraYAt(coordinate.getX());
            double cameraX = resolveCameraAxis(coordinate.getX(), leftCameraX, rightCameraX);
            double cameraY = resolveCameraAxis(coordinate.getY(), bottomCameraY, topCameraY);
            double anchorX = bounds.centerAnchorXOrDefault();
            double anchorY = bounds.centerAnchorYOrDefault();
            int relativeX = (int) Math.round(anchorX + (coordinate.getX() - cameraX) * WORLD_TILE_PIXEL_X);
            int relativeY = (int) Math.round(anchorY + (coordinate.getY() - cameraY) * WORLD_TILE_PIXEL_Y);
            return Optional.of(new ProjectionContext(normalizedMapName, coordinate, bounds,
                    leftCameraX, rightCameraX, bottomCameraY, topCameraY,
                    cameraX, cameraY, anchorX, anchorY, relativeX, relativeY));
        } catch (Exception e) {
            log.warn("[map-survey] build projection context failed: windowId={} map={} reason={}",
                    snapshotWindowId(snapshot), normalizedMapName, e.getMessage(), e);
            return Optional.empty();
        } finally {
            strip.get().flush();
        }
    }

    private boolean submitMoveMouseWithWindowContext(WindowTaskSnapshot snapshot, int absoluteX, int absoluteY) {
        String windowId = snapshotWindowId(snapshot);
        Optional<WindowRuntimeContext> context = multiWindowTaskManager.getRunner(windowId)
                .map(runner -> runner.getWindowContext());
        if (context.isEmpty()) {
            log.warn("[map-survey] cannot submit projected mouse move without window context: windowId={}", windowId);
            return false;
        }
        return windowTaskContextHolder.callWith(context.get(), () ->
                inputSequences.submitAndWait("mapSurvey:moveProjectedPlayerPoint",
                        List.of(InputAction.moveMouse(absoluteX, absoluteY))));
    }

    private Optional<MapLabelTemplateMatch> recognizeMapLabelFromStrip(WindowTaskSnapshot snapshot, BufferedImage strip) {
        // Crop the map-name label from the same coordinate strip used by the survey
        // operation. This keeps map detection and coordinate detection on the same frame.
        Optional<BufferedImage> label = miniMapCoordinateReader.extractCleanMapLabelImageFromCoordinateStrip(strip);
        if (label.isEmpty()) {
            log.info("[map-survey] map label crop empty: windowId={}", snapshotWindowId(snapshot));
            return Optional.empty();
        }
        try {
            // Save the exact cleaned label before matching so low-score or wrong-map
            // matches can be inspected without rerunning the same window state.
            String debugPath = saveMapSurveyDebugImage(label.get(), "current_map_label_clean.png");
            log.info("[map-survey] current map label debug image: windowId={} path={}",
                    snapshotWindowId(snapshot), debugPath);
            // Reuse the mini-map matcher so survey and runtime sync use the same map-name
            // template scoring rules.
            Optional<MapLabelTemplateMatch> match = miniMapCoordinateReader.recognizeMapLabelImage(label.get());
            match.ifPresent(best -> log.info("[map-survey] map label best match: windowId={} map={} score={} threshold={}",
                    snapshotWindowId(snapshot), best.mapName(), String.format("%.3f", best.score()), MAP_LABEL_MATCH_THRESHOLD));
            return match;
        } finally {
            label.ifPresent(BufferedImage::flush);
        }
    }

    private Optional<String> recognizeCurrentMapName(WindowTaskSnapshot snapshot) {
        Optional<BufferedImage> strip = captureCoordinateStrip(snapshot);
        if (strip.isEmpty()) {
            return Optional.empty();
        }
        try {
            Optional<MapLabelTemplateMatch> match = recognizeMapLabelFromStrip(snapshot, strip.get());
            if (match.isEmpty() || match.get().score() < MAP_LABEL_MATCH_THRESHOLD) {
                match.ifPresent(best -> log.info("[map-survey] current map low score for undo: windowId={} map={} score={} threshold={}",
                        snapshotWindowId(snapshot), best.mapName(), String.format("%.3f", best.score()), MAP_LABEL_MATCH_THRESHOLD));
                return Optional.empty();
            }
            return Optional.of(match.get().mapName());
        } finally {
            strip.get().flush();
        }
    }

    private Optional<BufferedImage> captureCoordinateStrip(WindowTaskSnapshot snapshot) {
        WindowNativeBinding binding = resolveLiveBinding(snapshot);
        if (binding == null || !binding.hasNativeHandle()) {
            return Optional.empty();
        }
        Optional<BoundWindowCaptureService.CaptureResult> full = boundWindowCaptureService.captureWindow(binding);
        if (full.isEmpty()) {
            return Optional.empty();
        }
        BufferedImage image = full.get().image();
        try {
            if (image.getWidth() < COORD_SCAN_X + COORD_SCAN_W || image.getHeight() < COORD_SCAN_Y + COORD_SCAN_H) {
                log.warn("[map-survey] window image too small for coordinate strip: windowId={} size={}x{}",
                        snapshotWindowId(snapshot), image.getWidth(), image.getHeight());
                return Optional.empty();
            }
            BufferedImage cropped = image.getSubimage(COORD_SCAN_X, COORD_SCAN_Y, COORD_SCAN_W, COORD_SCAN_H);
            return Optional.of(copyImage(cropped));
        } finally {
            image.flush();
        }
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private String saveMapSurveyDebugImage(BufferedImage image, String fileName) {
        if (image == null || fileName == null || fileName.isBlank()) {
            return null;
        }
        try {
            Files.createDirectories(MAP_SURVEY_DEBUG_DIR);
            Path output = MAP_SURVEY_DEBUG_DIR.resolve(fileName).normalize();
            ImageIO.write(image, "png", output.toFile());
            return output.toString();
        } catch (Exception e) {
            log.debug("[map-survey] save debug image failed: fileName={} reason={}", fileName, e.getMessage(), e);
            return null;
        }
    }

    private void pushUndo(String mapName, CalibrationUndoEntry entry) {
        if (mapName == null || entry == null) {
            return;
        }
        undoHistoryByMap.computeIfAbsent(mapName, ignored -> new ArrayList<>()).add(entry);
    }

    private CalibrationUndoEntry popUndo(String mapName) {
        List<CalibrationUndoEntry> history = undoHistoryByMap.get(mapName);
        if (history == null || history.isEmpty()) {
            return null;
        }
        return history.remove(history.size() - 1);
    }

    private Map<String, CameraBounds> loadCameraBounds() {
        if (!Files.exists(CAMERA_BOUNDS_CONFIG)) {
            return new HashMap<>();
        }
        try {
            Map<String, CameraBounds> loaded = objectMapper.readValue(CAMERA_BOUNDS_CONFIG.toFile(),
                    new TypeReference<Map<String, CameraBounds>>() {});
            loaded.replaceAll((mapName, bounds) -> bounds == null ? null : bounds.normalized());
            return loaded;
        } catch (IOException e) {
            log.warn("[map-survey] read camera bounds config failed: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private void saveCameraBounds(Map<String, CameraBounds> bounds) throws IOException {
        Path parent = CAMERA_BOUNDS_CONFIG.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Map<String, CameraBounds> normalized = normalizeBoundsForSave(bounds);
        Path temp = parent == null
                ? CAMERA_BOUNDS_CONFIG.resolveSibling(CAMERA_BOUNDS_CONFIG.getFileName() + ".tmp")
                : parent.resolve(CAMERA_BOUNDS_CONFIG.getFileName() + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), normalized);
        try {
            Files.move(temp, CAMERA_BOUNDS_CONFIG, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, CAMERA_BOUNDS_CONFIG, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Map<String, CameraBounds> normalizeBoundsForSave(Map<String, CameraBounds> bounds) {
        Map<String, CameraBounds> normalized = new HashMap<>();
        if (bounds == null) {
            return normalized;
        }
        bounds.forEach((mapName, cameraBounds) -> {
            if (mapName != null && cameraBounds != null) {
                normalized.put(mapName, cameraBounds.normalized());
            }
        });
        return normalized;
    }

    private WindowNativeBinding resolveLiveBinding(WindowTaskSnapshot snapshot) {
        String windowId = snapshotWindowId(snapshot);
        if (windowId != null) {
            Optional<WindowNativeBinding> live = multiWindowTaskManager.getSnapshot(windowId)
                    .map(WindowTaskSnapshot::getNativeBinding);
            if (live.isPresent() && (live.get().hasGeometry() || live.get().hasNativeHandle())) {
                return live.get();
            }
        }
        return snapshot == null ? null : snapshot.getNativeBinding();
    }

    private String normalizeMapName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String safeFileName(String mapName) {
        String safe = mapName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        if (safe.isBlank()) {
            return "map_" + Integer.toHexString(mapName.hashCode());
        }
        return safe;
    }

    private String snapshotWindowId(WindowTaskSnapshot snapshot) {
        return snapshot == null ? null : snapshot.getWindowId();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatNullable(Double value) {
        return value == null ? "null" : String.format("%.2f", value);
    }

    private String cameraState(ProjectionContext projection) {
        return xCameraState(projection.coordinate().getX(), projection.leftCameraX(), projection.rightCameraX())
                + "/"
                + yCameraState(projection.coordinate().getY(), projection.bottomCameraY(), projection.topCameraY());
    }

    private String xCameraState(double x, Double left, Double right) {
        if (left != null && x < left - 0.001) {
            return "x=left-bound";
        }
        if (right != null && x > right + 0.001) {
            return "x=right-bound";
        }
        return "x=free";
    }

    private String yCameraState(double y, Double bottom, Double top) {
        if (bottom != null && y < bottom - 0.001) {
            return "y=bottom-bound";
        }
        if (top != null && y > top + 0.001) {
            return "y=top-bound";
        }
        return "y=free";
    }

    private double resolveCameraAxis(double coordinate, Double lowerBoundary, Double upperBoundary) {
        if (lowerBoundary != null && upperBoundary != null) {
            return clamp(coordinate, Math.min(lowerBoundary, upperBoundary), Math.max(lowerBoundary, upperBoundary));
        }
        if (lowerBoundary != null && coordinate < lowerBoundary) {
            return lowerBoundary;
        }
        if (upperBoundary != null && coordinate > upperBoundary) {
            return upperBoundary;
        }
        return coordinate;
    }

    private boolean sleepInterruptible(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public enum CameraBoundaryDirection {
        LEFT("左"),
        RIGHT("右"),
        TOP("上"),
        BOTTOM("下");

        private final String displayName;

        CameraBoundaryDirection(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public record SurveyResult(String windowId, boolean success, String message) {
        public static SurveyResult success(String windowId, String message) {
            return new SurveyResult(windowId, true, message);
        }

        public static SurveyResult failed(String windowId, String message) {
            return new SurveyResult(windowId, false, message);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CameraBounds(Double leftCameraX,
                               Double rightCameraX,
                               Double topCameraY,
                               Double bottomCameraY,
                               Double centerAnchorX,
                               Double centerAnchorY,
                               List<BoundarySample> leftSamples,
                               List<BoundarySample> rightSamples,
                               List<BoundarySample> topSamples,
                               List<BoundarySample> bottomSamples,
                               List<PointCorrectionSample> correctionSamples) {
        public CameraBounds() {
            this(null, null, null, null, null, null, null, null, null, null, null);
        }

        public CameraBounds with(CameraBoundaryDirection direction, MapCoordinate coordinate, double cameraX, double cameraY) {
            return switch (direction) {
                case LEFT -> new CameraBounds(cameraX, rightCameraX, topCameraY, bottomCameraY, centerAnchorX, centerAnchorY,
                        appendSample(leftSamples, coordinate.getY(), cameraX), rightSamples, topSamples, bottomSamples, correctionSamples);
                case RIGHT -> new CameraBounds(leftCameraX, cameraX, topCameraY, bottomCameraY, centerAnchorX, centerAnchorY,
                        leftSamples, appendSample(rightSamples, coordinate.getY(), cameraX), topSamples, bottomSamples, correctionSamples);
                case TOP -> new CameraBounds(leftCameraX, rightCameraX, cameraY, bottomCameraY, centerAnchorX, centerAnchorY,
                        leftSamples, rightSamples, appendSample(topSamples, coordinate.getX(), cameraY), bottomSamples, correctionSamples);
                case BOTTOM -> new CameraBounds(leftCameraX, rightCameraX, topCameraY, cameraY, centerAnchorX, centerAnchorY,
                        leftSamples, rightSamples, topSamples, appendSample(bottomSamples, coordinate.getX(), cameraY), correctionSamples);
            };
        }

        public CameraBounds withCenterAnchor(Double centerAnchorX, Double centerAnchorY) {
            return new CameraBounds(leftCameraX, rightCameraX, topCameraY, bottomCameraY, centerAnchorX, centerAnchorY,
                    leftSamples, rightSamples, topSamples, bottomSamples, correctionSamples);
        }

        public CameraBounds withCorrection(PointCorrectionSample sample) {
            List<PointCorrectionSample> updated = new ArrayList<>(correctionSamples == null ? List.of() : correctionSamples);
            if (sample != null && sample.mapX() != null && sample.mapY() != null) {
                updated.removeIf(existing -> existing != null
                        && existing.mapX() != null
                        && existing.mapY() != null
                        && Math.abs(existing.mapX() - sample.mapX()) < 0.001
                        && Math.abs(existing.mapY() - sample.mapY()) < 0.001);
            }
            updated.add(sample);
            return new CameraBounds(leftCameraX, rightCameraX, topCameraY, bottomCameraY, centerAnchorX, centerAnchorY,
                    leftSamples, rightSamples, topSamples, bottomSamples, updated);
        }

        public UndoResult undo(CalibrationUndoEntry entry) {
            if (entry == null) {
                return new UndoResult(this, "none");
            }
            String kind = entry.kind() == null ? "" : entry.kind();
            return switch (kind) {
                case "LEFT" -> new UndoResult(new CameraBounds(entry.previousScalarValue(), rightCameraX, topCameraY, bottomCameraY,
                        centerAnchorX, centerAnchorY, copyBoundarySamples(entry.previousBoundarySamples()), rightSamples, topSamples, bottomSamples,
                        correctionSamples), "左边界");
                case "RIGHT" -> new UndoResult(new CameraBounds(leftCameraX, entry.previousScalarValue(), topCameraY, bottomCameraY,
                        centerAnchorX, centerAnchorY, leftSamples, copyBoundarySamples(entry.previousBoundarySamples()), topSamples, bottomSamples,
                        correctionSamples), "右边界");
                case "TOP" -> new UndoResult(new CameraBounds(leftCameraX, rightCameraX, entry.previousScalarValue(), bottomCameraY,
                        centerAnchorX, centerAnchorY, leftSamples, rightSamples, copyBoundarySamples(entry.previousBoundarySamples()), bottomSamples,
                        correctionSamples), "上边界");
                case "BOTTOM" -> new UndoResult(new CameraBounds(leftCameraX, rightCameraX, topCameraY, entry.previousScalarValue(),
                        centerAnchorX, centerAnchorY, leftSamples, rightSamples, topSamples, copyBoundarySamples(entry.previousBoundarySamples()),
                        correctionSamples), "下边界");
                case "CENTER" -> new UndoResult(new CameraBounds(leftCameraX, rightCameraX, topCameraY, bottomCameraY,
                        entry.previousCenterX(), entry.previousCenterY(), leftSamples, rightSamples, topSamples, bottomSamples,
                        correctionSamples), "中心点");
                case "CORRECTION" -> new UndoResult(new CameraBounds(leftCameraX, rightCameraX, topCameraY, bottomCameraY,
                        centerAnchorX, centerAnchorY, leftSamples, rightSamples, topSamples, bottomSamples,
                        trimToSize(correctionSamples, entry.previousCorrectionSize())), "修正点");
                default -> new UndoResult(new CameraBounds(leftCameraX, rightCameraX, topCameraY, bottomCameraY,
                        centerAnchorX, centerAnchorY, leftSamples, rightSamples, topSamples, bottomSamples,
                        correctionSamples), kind.isBlank() ? "未知" : kind);
            };
        }

        public Double leftCameraXAt(double y) {
            return interpolate(leftSamples, y).orElse(leftCameraX);
        }

        public Double rightCameraXAt(double y) {
            return interpolate(rightSamples, y).orElse(rightCameraX);
        }

        public Double topCameraYAt(double x) {
            return interpolate(topSamples, x).orElse(topCameraY);
        }

        public Double bottomCameraYAt(double x) {
            return interpolate(bottomSamples, x).orElse(bottomCameraY);
        }

        public int sampleCount(CameraBoundaryDirection direction) {
            return switch (direction) {
                case LEFT -> size(leftSamples);
                case RIGHT -> size(rightSamples);
                case TOP -> size(topSamples);
                case BOTTOM -> size(bottomSamples);
            };
        }

        public List<BoundarySample> samplesFor(CameraBoundaryDirection direction) {
            return switch (direction) {
                case LEFT -> copyBoundarySamples(leftSamples);
                case RIGHT -> copyBoundarySamples(rightSamples);
                case TOP -> copyBoundarySamples(topSamples);
                case BOTTOM -> copyBoundarySamples(bottomSamples);
            };
        }

        public int correctionCount() {
            return size(correctionSamples);
        }

        public CorrectionDelta correctionAt(MapCoordinate coordinate) {
            if (coordinate == null || correctionSamples == null || correctionSamples.isEmpty()) {
                return CorrectionDelta.empty(coordinate == null ? "no-coordinate" : "no-correction-samples");
            }
            List<PointCorrectionSample> validPins = correctionSamples.stream()
                    .filter(sample -> sample != null
                            && sample.mapX() != null
                            && sample.mapY() != null
                            && sample.actualRelX() != null
                            && sample.actualRelY() != null)
                    .toList();
            if (validPins.isEmpty()) {
                return CorrectionDelta.empty("no-valid-pins");
            }
            List<PointCorrectionSample> exact = validPins.stream()
                    .filter(sample -> Math.abs(coordinate.getX() - sample.mapX()) < 0.001
                            && Math.abs(coordinate.getY() - sample.mapY()) < 0.001)
                    .toList();
            if (exact.isEmpty()) {
                CorrectionFitResult localFit = localFitCorrection(coordinate, validPins);
                if (!localFit.accepted()) {
                    return CorrectionDelta.empty(localFit.rejectReason());
                }
                CorrectionCandidate candidate = localFit.candidate();
                return new CorrectionDelta(candidate.deltaX(), candidate.deltaY(), candidate.mapDistance(),
                        candidate.sampleCount(), String.format("local-fit/res=%.1f", candidate.residual()),
                        candidate.detail());
            }
            PointCorrectionSample pin = exact.get(exact.size() - 1);
            BasePoint currentBase = basePointAt(coordinate.getX(), coordinate.getY());
            int deltaX = (int) Math.round(pin.actualRelX() - currentBase.relativeX());
            int deltaY = (int) Math.round(pin.actualRelY() - currentBase.relativeY());
            return new CorrectionDelta(deltaX, deltaY, 0.0, exact.size(), "exact-pin",
                    String.format(Locale.ROOT, "pin=(%.0f,%.0f) actual=(%.0f,%.0f) currentBase=(%d,%d) exactCount=%d",
                            pin.mapX(), pin.mapY(), pin.actualRelX(), pin.actualRelY(),
                            currentBase.relativeX(), currentBase.relativeY(), exact.size()));
        }

        private CorrectionFitResult localFitCorrection(MapCoordinate coordinate, List<PointCorrectionSample> pins) {
            if (pins == null || pins.isEmpty()) {
                return CorrectionFitResult.rejected("local-fit rejected: no-valid-pins");
            }
            BasePoint currentBase = basePointAt(coordinate.getX(), coordinate.getY());
            List<FitPin> nearby = pins.stream()
                    .map(pin -> {
                        double dx = pin.mapX() - coordinate.getX();
                        double dy = pin.mapY() - coordinate.getY();
                        return new FitPin(pin, Math.sqrt(dx * dx + dy * dy));
                    })
                    .filter(pin -> pin.mapDistance() > 0.001 && pin.mapDistance() <= LOCAL_FIT_MAX_MAP_DISTANCE)
                    .sorted(Comparator.comparingDouble(FitPin::mapDistance))
                    .limit(LOCAL_FIT_MAX_SAMPLES)
                    .toList();
            if (nearby.size() < LOCAL_FIT_MIN_SAMPLES) {
                return CorrectionFitResult.rejected(String.format(Locale.ROOT,
                        "local-fit rejected: nearby=%d<%d within=%.1f validPins=%d",
                        nearby.size(), LOCAL_FIT_MIN_SAMPLES, LOCAL_FIT_MAX_MAP_DISTANCE, pins.size()));
            }
            FitPin seed = nearby.get(0);
            List<FitPin> cluster = nearby.stream()
                    .filter(pin -> screenDistance(seed.sample(), pin.sample()) <= LOCAL_FIT_SCREEN_CLUSTER_RADIUS)
                    .toList();
            if (cluster.size() < LOCAL_FIT_MIN_SAMPLES) {
                return CorrectionFitResult.rejected(String.format(Locale.ROOT,
                        "local-fit rejected: cluster=%d<%d screenRadius=%.1f seed=%s nearby=%s",
                        cluster.size(), LOCAL_FIT_MIN_SAMPLES, LOCAL_FIT_SCREEN_CLUSTER_RADIUS,
                        formatPin(seed), formatPins(nearby)));
            }
            double[] xCoefficients = solveLocalFit(coordinate, cluster, true);
            double[] yCoefficients = solveLocalFit(coordinate, cluster, false);
            if (xCoefficients == null || yCoefficients == null) {
                return CorrectionFitResult.rejected("local-fit rejected: singular-fit pins=" + formatPins(cluster));
            }
            double weightedResidual = weightedResidual(coordinate, cluster, xCoefficients, yCoefficients);
            if (weightedResidual > LOCAL_FIT_MAX_WEIGHTED_RESIDUAL) {
                return CorrectionFitResult.rejected(String.format(Locale.ROOT,
                        "local-fit rejected: residual=%.1f>%.1f pins=%s",
                        weightedResidual, LOCAL_FIT_MAX_WEIGHTED_RESIDUAL, formatPins(cluster)));
            }
            int predictedX = (int) Math.round(xCoefficients[2]);
            int predictedY = (int) Math.round(yCoefficients[2]);
            CorrectionCandidate candidate = new CorrectionCandidate(
                    predictedX - currentBase.relativeX(),
                    predictedY - currentBase.relativeY(),
                    seed.mapDistance(),
                    cluster.size(),
                    weightedResidual,
                    String.format(Locale.ROOT, "predicted=(%d,%d) currentBase=(%d,%d) nearest=%.1f pins=%s",
                            predictedX, predictedY, currentBase.relativeX(), currentBase.relativeY(),
                            seed.mapDistance(), formatPins(cluster)));
            return CorrectionFitResult.accepted(candidate);
        }

        private static double screenDistance(PointCorrectionSample left, PointCorrectionSample right) {
            double dx = left.actualRelX() - right.actualRelX();
            double dy = left.actualRelY() - right.actualRelY();
            return Math.sqrt(dx * dx + dy * dy);
        }

        private static String formatPins(List<FitPin> pins) {
            if (pins == null || pins.isEmpty()) {
                return "[]";
            }
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < pins.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(formatPin(pins.get(i)));
            }
            return builder.append(']').toString();
        }

        private static String formatPin(FitPin pin) {
            PointCorrectionSample sample = pin.sample();
            return String.format(Locale.ROOT, "(%.0f,%.0f)->(%.0f,%.0f) d=%.1f",
                    sample.mapX(), sample.mapY(), sample.actualRelX(), sample.actualRelY(), pin.mapDistance());
        }

        private double[] solveLocalFit(MapCoordinate coordinate, List<FitPin> pins, boolean screenX) {
            double[][] normal = new double[3][3];
            double[] target = new double[3];
            for (FitPin fitPin : pins) {
                PointCorrectionSample pin = fitPin.sample();
                double dx = pin.mapX() - coordinate.getX();
                double dy = pin.mapY() - coordinate.getY();
                double[] feature = new double[]{dx, dy, 1.0};
                double weight = 1.0 / Math.max(1.0, fitPin.mapDistance());
                double value = screenX ? pin.actualRelX() : pin.actualRelY();
                for (int row = 0; row < 3; row++) {
                    target[row] += weight * feature[row] * value;
                    for (int col = 0; col < 3; col++) {
                        normal[row][col] += weight * feature[row] * feature[col];
                    }
                }
            }
            return solve3x3(normal, target);
        }

        private static double weightedResidual(MapCoordinate coordinate,
                                               List<FitPin> pins,
                                               double[] xCoefficients,
                                               double[] yCoefficients) {
            double weightedError = 0.0;
            double weightSum = 0.0;
            for (FitPin fitPin : pins) {
                PointCorrectionSample pin = fitPin.sample();
                double dx = pin.mapX() - coordinate.getX();
                double dy = pin.mapY() - coordinate.getY();
                double predictedX = xCoefficients[0] * dx + xCoefficients[1] * dy + xCoefficients[2];
                double predictedY = yCoefficients[0] * dx + yCoefficients[1] * dy + yCoefficients[2];
                double errorX = predictedX - pin.actualRelX();
                double errorY = predictedY - pin.actualRelY();
                double weight = 1.0 / Math.max(1.0, fitPin.mapDistance());
                weightedError += weight * Math.sqrt(errorX * errorX + errorY * errorY);
                weightSum += weight;
            }
            return weightSum <= 0.0 ? Double.MAX_VALUE : weightedError / weightSum;
        }

        private static double[] solve3x3(double[][] matrix, double[] vector) {
            double[][] augmented = new double[3][4];
            for (int row = 0; row < 3; row++) {
                System.arraycopy(matrix[row], 0, augmented[row], 0, 3);
                augmented[row][3] = vector[row];
            }
            for (int col = 0; col < 3; col++) {
                int pivot = col;
                for (int row = col + 1; row < 3; row++) {
                    if (Math.abs(augmented[row][col]) > Math.abs(augmented[pivot][col])) {
                        pivot = row;
                    }
                }
                if (Math.abs(augmented[pivot][col]) < 0.000001) {
                    return null;
                }
                if (pivot != col) {
                    double[] tmp = augmented[col];
                    augmented[col] = augmented[pivot];
                    augmented[pivot] = tmp;
                }
                double divisor = augmented[col][col];
                for (int k = col; k < 4; k++) {
                    augmented[col][k] /= divisor;
                }
                for (int row = 0; row < 3; row++) {
                    if (row == col) {
                        continue;
                    }
                    double factor = augmented[row][col];
                    for (int k = col; k < 4; k++) {
                        augmented[row][k] -= factor * augmented[col][k];
                    }
                }
            }
            return new double[]{augmented[0][3], augmented[1][3], augmented[2][3]};
        }

        @JsonIgnore
        public double centerAnchorXOrDefault() {
            return centerAnchorX == null ? CLIENT_CENTER_X : centerAnchorX;
        }

        @JsonIgnore
        public double centerAnchorYOrDefault() {
            return centerAnchorY == null ? CLIENT_CENTER_Y : centerAnchorY;
        }

        @JsonIgnore
        public boolean isUsable() {
            return leftCameraX != null
                    && rightCameraX != null
                    && topCameraY != null
                    && bottomCameraY != null;
        }

        @JsonIgnore
        public boolean hasAnyCalibration() {
            return leftCameraX != null
                    || rightCameraX != null
                    || topCameraY != null
                    || bottomCameraY != null
                    || centerAnchorX != null
                    || centerAnchorY != null
                    || size(leftSamples) > 0
                    || size(rightSamples) > 0
                    || size(topSamples) > 0
                    || size(bottomSamples) > 0
                    || correctionCount() > 0;
        }

        public Double scalarValue(CameraBoundaryDirection direction) {
            return switch (direction) {
                case LEFT -> leftCameraX;
                case RIGHT -> rightCameraX;
                case TOP -> topCameraY;
                case BOTTOM -> bottomCameraY;
            };
        }

        public CameraBounds normalized() {
            return new CameraBounds(leftCameraX, rightCameraX, topCameraY, bottomCameraY, centerAnchorX, centerAnchorY,
                    validBoundarySamples(leftSamples),
                    validBoundarySamples(rightSamples),
                    validBoundarySamples(topSamples),
                    validBoundarySamples(bottomSamples),
                    validCorrectionSamples(correctionSamples));
        }

        private BasePoint basePointAt(double mapX, double mapY) {
            Double left = leftCameraXAt(mapY);
            Double right = rightCameraXAt(mapY);
            Double bottom = bottomCameraYAt(mapX);
            Double top = topCameraYAt(mapX);
            double cameraX = resolveCameraAxisValue(mapX, left, right);
            double cameraY = resolveCameraAxisValue(mapY, bottom, top);
            int relativeX = (int) Math.round(centerAnchorXOrDefault() + (mapX - cameraX) * WORLD_TILE_PIXEL_X);
            int relativeY = (int) Math.round(centerAnchorYOrDefault() + (mapY - cameraY) * WORLD_TILE_PIXEL_Y);
            return new BasePoint(relativeX, relativeY);
        }

        private static double resolveCameraAxisValue(double coordinate, Double lowerBoundary, Double upperBoundary) {
            if (lowerBoundary != null && upperBoundary != null) {
                return Math.max(Math.min(lowerBoundary, upperBoundary), Math.min(coordinate, Math.max(lowerBoundary, upperBoundary)));
            }
            if (lowerBoundary != null && coordinate < lowerBoundary) {
                return lowerBoundary;
            }
            if (upperBoundary != null && coordinate > upperBoundary) {
                return upperBoundary;
            }
            return coordinate;
        }

        private static List<BoundarySample> appendSample(List<BoundarySample> samples, double axisCoordinate, double cameraCoordinate) {
            List<BoundarySample> updated = new ArrayList<>(samples == null ? List.of() : samples);
            updated.removeIf(sample -> sample != null
                    && sample.axisCoordinate() != null
                    && Math.abs(sample.axisCoordinate() - axisCoordinate) < BOUNDARY_AXIS_EPSILON);
            updated.add(new BoundarySample(axisCoordinate, cameraCoordinate));
            updated.sort(Comparator.comparingDouble(BoundarySample::axisCoordinate));
            return updated;
        }

        private static List<BoundarySample> copyBoundarySamples(List<BoundarySample> samples) {
            return samples == null ? null : new ArrayList<>(samples);
        }

        private static <T> List<T> trimToSize(List<T> values, Integer size) {
            List<T> updated = new ArrayList<>(values == null ? List.of() : values);
            int targetSize = Math.max(0, size == null ? updated.size() - 1 : size);
            while (updated.size() > targetSize) {
                updated.remove(updated.size() - 1);
            }
            return updated;
        }

        private static List<BoundarySample> validBoundarySamples(List<BoundarySample> samples) {
            if (samples == null) {
                return null;
            }
            List<BoundarySample> valid = new ArrayList<>();
            for (BoundarySample sample : samples) {
                if (sample != null && sample.axisCoordinate() != null && sample.cameraCoordinate() != null) {
                    valid.removeIf(existing -> Math.abs(existing.axisCoordinate() - sample.axisCoordinate()) < BOUNDARY_AXIS_EPSILON);
                    valid.add(sample);
                }
            }
            valid.sort(Comparator.comparingDouble(BoundarySample::axisCoordinate));
            return valid.isEmpty() ? null : valid;
        }

        private static List<PointCorrectionSample> validCorrectionSamples(List<PointCorrectionSample> samples) {
            if (samples == null) {
                return null;
            }
            List<PointCorrectionSample> valid = samples.stream()
                    .filter(sample -> sample != null
                            && sample.mapX() != null
                            && sample.mapY() != null
                            && sample.baseRelX() != null
                            && sample.baseRelY() != null
                            && sample.actualRelX() != null
                            && sample.actualRelY() != null
                            && sample.errorX() != null
                            && sample.errorY() != null
                            && Math.abs(sample.errorX()) <= CORRECTION_LARGE_ERROR_THRESHOLD
                            && Math.abs(sample.errorY()) <= CORRECTION_LARGE_ERROR_THRESHOLD)
                    .toList();
            return valid.isEmpty() ? null : valid;
        }

        private static Optional<Double> interpolate(List<BoundarySample> samples, double axisCoordinate) {
            List<BoundarySample> valid = samples == null ? List.of() : samples.stream()
                    .filter(sample -> sample != null && sample.axisCoordinate() != null && sample.cameraCoordinate() != null)
                    .sorted(Comparator.comparingDouble(BoundarySample::axisCoordinate))
                    .toList();
            if (valid.isEmpty()) {
                return Optional.empty();
            }
            if (valid.size() == 1 || axisCoordinate <= valid.get(0).axisCoordinate()) {
                return Optional.of(valid.get(0).cameraCoordinate());
            }
            BoundarySample last = valid.get(valid.size() - 1);
            if (axisCoordinate >= last.axisCoordinate()) {
                return Optional.of(last.cameraCoordinate());
            }
            for (int i = 1; i < valid.size(); i++) {
                BoundarySample right = valid.get(i);
                if (axisCoordinate <= right.axisCoordinate()) {
                    BoundarySample left = valid.get(i - 1);
                    double span = right.axisCoordinate() - left.axisCoordinate();
                    if (Math.abs(span) < 0.0001) {
                        return Optional.of(right.cameraCoordinate());
                    }
                    double ratio = (axisCoordinate - left.axisCoordinate()) / span;
                    return Optional.of(left.cameraCoordinate() + (right.cameraCoordinate() - left.cameraCoordinate()) * ratio);
                }
            }
            return Optional.of(last.cameraCoordinate());
        }

        private static int size(List<?> samples) {
            return samples == null ? 0 : samples.size();
        }
    }

    public record BoundarySample(Double axisCoordinate, Double cameraCoordinate) {
    }

    public record CalibrationUndoEntry(String kind,
                                       Double previousScalarValue,
                                       Double previousCenterX,
                                       Double previousCenterY,
                                       Integer previousListSize,
                                       Integer previousCorrectionSize,
                                       List<BoundarySample> previousBoundarySamples) {
        public static CalibrationUndoEntry forBoundary(CameraBoundaryDirection direction,
                                                       Double previousScalarValue,
                                                       List<BoundarySample> previousBoundarySamples) {
            List<BoundarySample> snapshot = previousBoundarySamples == null ? null : new ArrayList<>(previousBoundarySamples);
            return new CalibrationUndoEntry(direction == null ? null : direction.name(), previousScalarValue, null, null,
                    snapshot == null ? 0 : snapshot.size(), 0, snapshot);
        }
    }

    public record UndoResult(CameraBounds bounds, String description) {
    }

    public record PointCorrectionSample(Double mapX,
                                        Double mapY,
                                        Double baseRelX,
                                        Double baseRelY,
                                        Double actualRelX,
                                        Double actualRelY,
                                        Double errorX,
                                        Double errorY) {
    }

    private record CorrectionDelta(int deltaX,
                                   int deltaY,
                                   double nearestDistance,
                                   int sampleCount,
                                   String source,
                                   String detail) {
        private static CorrectionDelta empty() {
            return empty("no-correction");
        }

        private static CorrectionDelta empty(String detail) {
            return new CorrectionDelta(0, 0, 0.0, 0, "none", detail);
        }
    }

    private record CorrectionCandidate(int deltaX,
                                       int deltaY,
                                       double mapDistance,
                                       int sampleCount,
                                       double residual,
                                       String detail) {
    }

    private record CorrectionFitResult(CorrectionCandidate candidate, String rejectReason) {
        private static CorrectionFitResult accepted(CorrectionCandidate candidate) {
            return new CorrectionFitResult(candidate, null);
        }

        private static CorrectionFitResult rejected(String reason) {
            return new CorrectionFitResult(null, reason);
        }

        private boolean accepted() {
            return candidate != null;
        }
    }

    private record FitPin(PointCorrectionSample sample, double mapDistance) {
    }

    private record BasePoint(int relativeX, int relativeY) {
    }

    private record ProjectionContext(String mapName,
                                     MapCoordinate coordinate,
                                     CameraBounds bounds,
                                     Double leftCameraX,
                                     Double rightCameraX,
                                     Double bottomCameraY,
                                     Double topCameraY,
                                     double cameraX,
                                     double cameraY,
                                     double anchorX,
                                     double anchorY,
                                     int baseRelativeX,
                                     int baseRelativeY) {
    }

}
