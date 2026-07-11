package com.bot.dhxy.vision;



import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.service.MapNameCanonicalizer;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 位置视觉服务：只负责截图、OCR/template 识别和返回坐标，不执行真实输入。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationVisionService {
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final MiniMapCoordinateReader miniMapCoordinateReader;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final MapNameCanonicalizer mapNameCanonicalizer;

    private static final int LOCATION_COORDINATE_PLAUSIBLE_MARGIN_PX = 80;
    private static final Path LOCATION_FAILURE_CASE_DIR = Path.of("images", "failure-cases", "location")
            .toAbsolutePath()
            .normalize();
    private static final DateTimeFormatter FAILURE_CASE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    /**
     * Scans the current bound game window for the player's map name and coordinate.
     *
     * <p>CR246 (CR208-16): the cloud {@code MINIMAP_LOCATION READ_LOCATION} call owns the whole
     * fallback chain now (label/coordinate templates first, cloud OCR second). The retired local
     * sidecar and Baidu OCR stages were deleted entirely by CR257 (C1/C2/C3); a cloud miss is a
     * miss. Bound-window mode uses no-focus capture, while legacy no-context callers may still
     * focus before Robot capture.</p>
     *
     * @return recognized location, or {@code null} when capture/recognition fails. If the current
     *         task has requested stop, this method throws {@link TaskStopRequestedException}.
     */
    public LocationInfo scanCurrentLocation() {
        long startedAt = System.currentTimeMillis();
        long latencyStart = LatencyMetrics.start();
        String provider = "NONE";
        LocationInfo selected = null;
        try {
            checkpoint("start location scan");
            if (windowTaskContextHolder.rawCurrent().isPresent()) {
                log.info("[location] scan current no-focus");
            } else {
                log.info("[location] scan current legacy focused fallback");
                if (!tracker.bringWindowToFront()) {
                    log.warn("[location] failed to bring window to front before coordinate scan");
                    return null;
                }
            }

            LocationInfo cloudLocation = scanByMiniMapTemplate(startedAt);
            checkpoint("after minimap cloud location scan");
            if (cloudLocation != null) {
                provider = "MINIMAP_CLOUD";
                selected = cloudLocation;
                return cloudLocation;
            }
            log.info("[location] cloud location miss; no local OCR fallback (CR246): elapsedMs={}",
                    System.currentTimeMillis() - startedAt);
            return null;
        } finally {
            log.info("[latency] event=location.scanCurrent.breakdown provider={} result={} totalMs={}",
                    provider,
                    selected == null ? "NONE" : selected.toString(),
                    System.currentTimeMillis() - startedAt);
            LatencyMetrics.info(log, "location.scanCurrent", latencyStart,
                    "provider=" + provider + " result=" + (selected == null ? "NONE" : selected.toString()));
        }
    }

    private boolean isPlausibleLocation(LocationInfo location, String sourceImagePath, String provider) {
        if (location == null) {
            return false;
        }
        boolean plausible = coordinateHelper.isLogicalCoordinatePlausible(
                location.mapName,
                location.x,
                location.y,
                LOCATION_COORDINATE_PLAUSIBLE_MARGIN_PX);
        if (plausible) {
            return true;
        }
        archiveRejectedLocationSample(location, sourceImagePath, provider, "coordinate-out-of-transform-bounds");
        log.warn("[location] rejected implausible OCR coordinate: provider={} map={} coord=({}, {}) source={} reason=coordinate-out-of-transform-bounds",
                provider, location.mapName, location.x, location.y, sourceImagePath);
        return false;
    }

    /**
     * Archive rejected coordinate strips as local regression samples.
     *
     * <p>The live window-scoped {@code tmp_pos.png} is overwritten by the next location scan. When a
     * bad OCR value is rejected, copy the image immediately so future coordinate/parser fixes can be
     * tested against every known failure before another in-game run.</p>
     */
    private void archiveRejectedLocationSample(LocationInfo location,
                                               String sourceImagePath,
                                               String provider,
                                               String reason) {
        try {
            String time = LocalDateTime.now().format(FAILURE_CASE_TIME_FORMAT);
            String mapName = safeFailureFileName(location == null ? "unknown" : location.mapName);
            String safeReason = safeFailureFileName(reason == null ? "unknown" : reason);
            Path caseDir = LOCATION_FAILURE_CASE_DIR.resolve(time + "_" + mapName + "_" + safeReason).normalize();
            Files.createDirectories(caseDir);

            Path copiedImage = null;
            if (sourceImagePath != null && !sourceImagePath.isBlank()) {
                Path source = Path.of(sourceImagePath);
                if (Files.exists(source)) {
                    copiedImage = caseDir.resolve("tmp_pos.png");
                    Files.copy(source, copiedImage, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            String metadata = "reason=" + reason + '\n'
                    + "provider=" + provider + '\n'
                    + "map=" + (location == null ? "-" : location.mapName) + '\n'
                    + "coord=" + (location == null ? "-" : location.x + "," + location.y) + '\n'
                    + "sourceImage=" + sourceImagePath + '\n'
                    + "copiedImage=" + (copiedImage == null ? "-" : copiedImage) + '\n';
            Files.writeString(caseDir.resolve("metadata.txt"), metadata, StandardCharsets.UTF_8);
            log.warn("[location] rejected sample archived: dir={} provider={} location={} reason={}",
                    caseDir, provider, location, reason);
        } catch (Exception e) {
            log.warn("[location] rejected sample archive failed: provider={} location={} source={} reason={} error={}",
                    provider, location, sourceImagePath, reason, e.getMessage(), e);
        }
    }

    private String safeFailureFileName(String value) {
        String text = value == null || value.isBlank() ? "unknown" : value.trim();
        return text.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private LocationInfo scanByMiniMapTemplate(long startedAt) {
        long templateStartedAt = System.currentTimeMillis();
        try {
            checkpoint("before minimap template reader");
            return miniMapCoordinateReader.readCurrentTemplateLocation()
                    .map(location -> {
                        checkpoint("convert minimap template result");
                        MapCoordinate coordinate = location.coordinate();
                        LocationInfo info = new LocationInfo(
                                location.mapName(),
                                coordinate.getX(),
                                coordinate.getY()
                        );
                        if (location.ocrFallback()) {
                            /*
                             * CR246: cloud OCR fallback results keep the retired local-OCR
                             * discipline — canonical map name plus the local coordinate-transform
                             * plausibility gate. Rejected values archive a metadata-only failure
                             * sample (the strip image lives cloud-side now).
                             */
                            info = canonicalizeOcrLocation(info, "location:cloud-ocr-fallback");
                            if (!isPlausibleLocation(info, null, "CLOUD_OCR")) {
                                return null;
                            }
                        }
                        log.info("[location] selected provider={} elapsedMs={} templateElapsedMs={} "
                                        + "map={} coord=({}, {}) score={} label={}",
                                location.ocrFallback() ? "CLOUD_OCR" : "MINIMAP_TEMPLATE",
                                System.currentTimeMillis() - startedAt,
                                System.currentTimeMillis() - templateStartedAt,
                                info.mapName, info.x, info.y,
                                String.format("%.3f", location.mapLabelScore()),
                                location.mapLabelPath());
                        return info;
                    })
                    .orElse(null);
        } catch (TaskStopRequestedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[location] minimap template location failed: reason={}", e.getMessage(), e);
            return null;
        }
    }

    private LocationInfo canonicalizeOcrLocation(LocationInfo location, String source) {
        if (location == null || location.mapName == null || location.mapName.isBlank()) {
            return location;
        }
        String canonicalMapName = mapNameCanonicalizer.canonicalize(location.mapName, source);
        if (canonicalMapName.isBlank() || canonicalMapName.equals(location.mapName)) {
            return location;
        }
        log.info("[location] OCR map name canonicalized: source={} raw={} canonical={} coord=({}, {})",
                source, location.mapName, canonicalMapName, location.x, location.y);
        return new LocationInfo(canonicalMapName, location.x, location.y);
    }


    /**
     * Throws when the current task requested stop or the worker thread was interrupted.
     *
     * @param stage diagnostic name for the current location-recognition stage.
     */
    private void checkpoint(String stage) {
        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "location scan interrupted at " + stage);
    }

}
