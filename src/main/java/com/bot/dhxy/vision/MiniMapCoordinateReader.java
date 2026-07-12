package com.bot.dhxy.vision;

import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.cloud.task.MiniMapLocationCloudDecision;
import com.bot.dhxy.cloud.task.MiniMapLocationCloudDecisionService;
import com.bot.dhxy.cloud.task.MiniMapLocationCloudRequest;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.navigation.MapLabelTemplateMatch;
import com.bot.dhxy.model.navigation.MiniMapSnapshot;
import com.bot.dhxy.model.navigation.TemplateLocationInfo;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiniMapCoordinateReader {

    private static final int COORD_SCAN_X = 46;
    private static final int COORD_SCAN_Y = 59;
    private static final int COORD_SCAN_W = 178;
    private static final int COORD_SCAN_H = 35;
    private static final String PNG_MIME_TYPE = "image/png";

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final MiniMapLocationCloudDecisionService miniMapLocationCloudDecisionService;
    private final TaskExecutionContextHolder taskExecutionContextHolder;

    public Optional<MapCoordinate> readCurrentCoordinate() {
        BufferedImage raw = captureCoordinateStrip();
        if (raw == null) {
            return Optional.empty();
        }
        try {
            MiniMapLocationCloudDecision decision = recognizeStrip(
                    raw, MiniMapLocationCloudRequest.Operation.READ_COORDINATE,
                    "read-current-coordinate", true, false, null);
            return decision.isCloudExecuted()
                    ? Optional.ofNullable(decision.getCoordinate())
                    : Optional.empty();
        } finally {
            raw.flush();
        }
    }

    public Optional<TemplateLocationInfo> readCurrentTemplateLocation() {
        return readCurrentTemplateLocationForScan().location();
    }

    /**
     * CR258: read result of the scan path, including the cloud OCR-fallback rejection diagnostics.
     * The plausibility guard moved into the cloud READ_LOCATION fallback (CR251 Codex #3); a
     * rejected coordinate comes back as a miss whose {@code ocrFallbackMissReason} lets the caller
     * keep its failure-sample archive without any local transform math.
     */
    public record TemplateLocationScanResult(Optional<TemplateLocationInfo> location,
                                             String ocrFallbackMissReason,
                                             String ocrRejectedLocation) {

        static TemplateLocationScanResult miss() {
            return new TemplateLocationScanResult(Optional.empty(), null, null);
        }
    }

    public TemplateLocationScanResult readCurrentTemplateLocationForScan() {
        long startedAtMs = System.currentTimeMillis();
        long captureStartedAtMs = startedAtMs;
        BufferedImage raw = captureCoordinateStrip();
        long captureElapsedMs = System.currentTimeMillis() - captureStartedAtMs;
        if (raw == null) {
            log.info("[latency] event=minimap.template-location source=read-current-template-location "
                            + "captureMs={} recognizeMs=0 totalMs={} result=missing-capture",
                    captureElapsedMs, System.currentTimeMillis() - startedAtMs);
            return TemplateLocationScanResult.miss();
        }
        try {
            long recognizeStartedAtMs = System.currentTimeMillis();
            MiniMapLocationCloudDecision decision = recognizeStrip(
                    raw, MiniMapLocationCloudRequest.Operation.READ_LOCATION,
                    "read-current-template-location", true, true, null);
            long recognizeElapsedMs = System.currentTimeMillis() - recognizeStartedAtMs;
            long totalElapsedMs = System.currentTimeMillis() - startedAtMs;
            log.info("[latency] event=minimap.template-location source=read-current-template-location "
                            + "captureMs={} recognizeMs={} totalMs={} cloudExecuted={} result={}",
                    captureElapsedMs, recognizeElapsedMs, totalElapsedMs,
                    decision != null && decision.isCloudExecuted(),
                    decision == null ? "null-decision" : decision.getReason());
            if (!decision.isCloudExecuted()
                    || decision.getCoordinate() == null
                    || !hasText(decision.getMapName())) {
                return new TemplateLocationScanResult(Optional.empty(),
                        decision == null ? null : decision.getOcrFallbackReason(),
                        decision == null ? null : decision.getOcrRejectedLocation());
            }
            boolean ocrFallback = decision.getReason() != null
                    && decision.getReason().contains("minimap-ocr-fallback");
            log.info("[minimap-location] cloud hit: serviceId={} map={} coord=({}, {}) score={} ocrFallback={} reason={}",
                    CloudDecisionServiceId.MINIMAP_LOCATION,
                    decision.getMapName(),
                    decision.getCoordinate().getX(),
                    decision.getCoordinate().getY(),
                    String.format("%.3f", decision.getScore()),
                    ocrFallback,
                    decision.getReason());
            return new TemplateLocationScanResult(Optional.of(new TemplateLocationInfo(
                    decision.getMapName(),
                    decision.getCoordinate(),
                    decision.getScore(),
                    decision.getLabelPath(),
                    ocrFallback
            )), null, null);
        } finally {
            raw.flush();
        }
    }

    public Optional<BufferedImage> readCurrentMapLabelImage() {
        BufferedImage raw = captureCoordinateStrip();
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return labelImageFromDecision(recognizeStrip(
                    raw, MiniMapLocationCloudRequest.Operation.EXTRACT_MAP_LABEL,
                    "read-current-map-label-image", false, false, null));
        } finally {
            raw.flush();
        }
    }

    public Optional<BufferedImage> extractCleanMapLabelImageFromCoordinateStrip(BufferedImage raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return labelImageFromDecision(recognizeStrip(
                raw, MiniMapLocationCloudRequest.Operation.EXTRACT_MAP_LABEL,
                "extract-map-label-from-strip", false, false, null));
    }

    /**
     * Normalizes an already-cropped mini-map label through the cloud-owned label pipeline.
     *
     * @param mapName OCR-confirmed map name that this template should represent; sent as a hint
     *                only and not interpreted by local runtime.
     * @param source label-like image in mini-map coordinate text style.
     * @return cloud-normalized label image, or empty on disabled/timeout/invalid/no-result.
     */
    public Optional<BufferedImage> normalizeMapLabelTemplateImage(String mapName, BufferedImage source) {
        if (source == null) {
            return Optional.empty();
        }
        return labelImageFromDecision(recognizeImage(
                source, MiniMapLocationCloudRequest.Operation.NORMALIZE_MAP_LABEL,
                "normalize-map-label-template", false, false, mapName));
    }

    /**
     * Checks template-label plausibility by asking the cloud label pipeline to accept the image.
     *
     * @param mapName expected Chinese map name, sent to cloud as a hint.
     * @param label binary or color label image that will be saved under {@code mapName}.
     * @return true only when cloud accepts the label; disabled/failure/no-result returns false.
     */
    public boolean isMapLabelWidthPlausible(String mapName, BufferedImage label) {
        if (label == null) {
            return false;
        }
        MiniMapLocationCloudDecision decision = recognizeImage(
                label, MiniMapLocationCloudRequest.Operation.NORMALIZE_MAP_LABEL,
                "map-label-width-plausible", false, false, mapName);
        return decision.isCloudExecuted();
    }

    public Optional<MapLabelTemplateMatch> recognizeMapLabelFromCoordinateStrip(BufferedImage raw) {
        if (raw == null) {
            return Optional.empty();
        }
        MiniMapLocationCloudDecision decision = recognizeStrip(
                raw, MiniMapLocationCloudRequest.Operation.RECOGNIZE_MAP_LABEL_FROM_STRIP,
                "recognize-map-label-from-strip", false, true, null);
        return mapLabelMatch(decision);
    }

    public Optional<MapLabelTemplateMatch> recognizeMapLabelImage(BufferedImage label) {
        if (label == null) {
            return Optional.empty();
        }
        MiniMapLocationCloudDecision decision = recognizeImage(
                label, MiniMapLocationCloudRequest.Operation.RECOGNIZE_MAP_LABEL_IMAGE,
                "recognize-map-label-image", false, true, null);
        return mapLabelMatch(decision);
    }

    /**
     * Cloud-side map-label templates are not cached in the production client.
     */
    public void invalidateMapLabelTemplateCache() {
        log.debug("[minimap-location] template cache invalidation forwarded to cloud boundary as no-op");
    }

    public MiniMapSnapshot readCurrentLocationSnapshot() {
        BufferedImage raw = captureCoordinateStrip();
        if (raw == null) {
            return new MiniMapSnapshot(null, null);
        }
        try {
            return snapshotFromDecision(recognizeStrip(
                    raw, MiniMapLocationCloudRequest.Operation.READ_LOCATION_SNAPSHOT,
                    "read-current-location-snapshot", true, false, null), true);
        } finally {
            raw.flush();
        }
    }

    public MiniMapSnapshot readLocationSnapshotFromCoordinateStrip(BufferedImage raw,
                                                                  boolean includeMapName,
                                                                  boolean debugOutput) {
        if (raw == null) {
            return new MiniMapSnapshot(null, null);
        }
        MiniMapLocationCloudDecision decision = recognizeStrip(
                raw, MiniMapLocationCloudRequest.Operation.READ_LOCATION_SNAPSHOT,
                "read-location-snapshot-from-strip", true, false, null);
        return snapshotFromDecision(decision, includeMapName || debugOutput);
    }

    private MiniMapLocationCloudDecision recognizeStrip(BufferedImage raw,
                                                       MiniMapLocationCloudRequest.Operation operation,
                                                       String source,
                                                       boolean requiresCoordinate,
                                                       boolean requiresMapName,
                                                       String mapNameHint) {
        return recognize(raw, operation, source, requiresCoordinate, requiresMapName, mapNameHint, true);
    }

    private MiniMapLocationCloudDecision recognizeImage(BufferedImage raw,
                                                       MiniMapLocationCloudRequest.Operation operation,
                                                       String source,
                                                       boolean requiresCoordinate,
                                                       boolean requiresMapName,
                                                       String mapNameHint) {
        return recognize(raw, operation, source, requiresCoordinate, requiresMapName, mapNameHint, false);
    }

    private MiniMapLocationCloudDecision recognize(BufferedImage raw,
                                                  MiniMapLocationCloudRequest.Operation operation,
                                                  String source,
                                                  boolean requiresCoordinate,
                                                  boolean requiresMapName,
                                                  String mapNameHint,
                                                  boolean coordinateStrip) {
        if (raw == null) {
            return requiredFailure("missing raw image");
        }
        try {
            boolean pathingLocationRead = "read-current-template-location".equals(source);
            long startedAtMs = pathingLocationRead ? System.currentTimeMillis() : 0L;
            long encodeStartedAtMs = pathingLocationRead ? startedAtMs : 0L;
            byte[] png = encodePng(raw);
            long encodeElapsedMs = pathingLocationRead ? System.currentTimeMillis() - encodeStartedAtMs : 0L;
            TaskExecutionContext context = taskExecutionContextHolder.current().orElse(null);
            int width = raw.getWidth();
            int height = raw.getHeight();
            MiniMapLocationCloudRequest.Roi roi = MiniMapLocationCloudRequest.Roi.builder()
                    .x(0)
                    .y(0)
                    .width(width)
                    .height(height)
                    .build();
            MiniMapLocationCloudRequest request = MiniMapLocationCloudRequest.builder()
                    .operation(operation)
                    .imagePayloadBase64(Base64.getEncoder().encodeToString(png))
                    .payloadMimeType(PNG_MIME_TYPE)
                    .imageSha256(sha256Hex(png))
                    .rawImagePath("")
                    .debugImageId(source)
                    .source(source)
                    .taskCode(context == null ? "unknown" : context.getTaskCode())
                    .phase(coordinateStrip ? "minimap-coordinate-strip" : "minimap-map-label")
                    .windowId(context == null ? null : context.getWindowId())
                    .taskRunId(context == null ? null : Long.toString(context.getTaskRunId()))
                    .policyVersion(null)
                    .hwnd(context == null ? null : context.getNativeWindowHandle())
                    .mapNameHint(mapNameHint)
                    .requiresCoordinate(requiresCoordinate)
                    .requiresMapName(requiresMapName)
                    .windowWidth(width)
                    .windowHeight(height)
                    .roi(roi)
                    .build();
            long cloudStartedAtMs = pathingLocationRead ? System.currentTimeMillis() : 0L;
            MiniMapLocationCloudDecision decision = miniMapLocationCloudDecisionService.recognize(request);
            if (pathingLocationRead) {
                long cloudElapsedMs = System.currentTimeMillis() - cloudStartedAtMs;
                log.info("[latency] event=minimap.location-decision source={} operation={} windowId={} "
                                + "encodeMs={} cloudMs={} totalMs={} result={}",
                        source, operation, context == null ? null : context.getWindowId(),
                        encodeElapsedMs, cloudElapsedMs, System.currentTimeMillis() - startedAtMs,
                        decision == null ? "null-decision" : decision.getReason());
            }
            return decision;
        } catch (IOException e) {
            return requiredFailure("encode minimap payload failed: " + e.getMessage());
        }
    }

    private BufferedImage captureCoordinateStrip() {
        int[] rect = coordinateHelper.getScaledRect(COORD_SCAN_X, COORD_SCAN_Y, COORD_SCAN_W, COORD_SCAN_H);
        return tracker.captureToMemory("minimap-coordinate", rect[0], rect[1], rect[2], rect[3]);
    }

    private Optional<MapLabelTemplateMatch> mapLabelMatch(MiniMapLocationCloudDecision decision) {
        if (!decision.isCloudExecuted() || !hasText(decision.getMapName())) {
            return Optional.empty();
        }
        return Optional.of(new MapLabelTemplateMatch(decision.getMapName(), decision.getScore()));
    }

    private MiniMapSnapshot snapshotFromDecision(MiniMapLocationCloudDecision decision, boolean includeMapLabelPath) {
        if (!decision.isCloudExecuted()) {
            return new MiniMapSnapshot(null, null);
        }
        String labelPath = includeMapLabelPath ? labelPathFromDecision(decision).orElse(decision.getLabelPath()) : null;
        return new MiniMapSnapshot(labelPath, decision.getCoordinate());
    }

    private Optional<BufferedImage> labelImageFromDecision(MiniMapLocationCloudDecision decision) {
        if (!decision.hasLabelPayload()) {
            return Optional.empty();
        }
        try {
            return Optional.of(decodePng(decision.getLabelImagePayloadBase64()));
        } catch (IOException | IllegalArgumentException e) {
            log.warn("[minimap-location] decode cloud label payload failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> labelPathFromDecision(MiniMapLocationCloudDecision decision) {
        Optional<BufferedImage> label = labelImageFromDecision(decision);
        if (label.isEmpty()) {
            return Optional.ofNullable(decision.getLabelPath()).filter(MiniMapCoordinateReader::hasText);
        }
        try {
            return Optional.ofNullable(saveImage(label.get(), "minimap_map_label_cloud.png"));
        } finally {
            label.get().flush();
        }
    }

    private String saveImage(BufferedImage image, String fileName) {
        try {
            File file = new File(windowScopedTempPath.resolve(fileName));
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                Files.createDirectories(parent.toPath());
            }
            ImageIO.write(image, "png", file);
            return file.getPath();
        } catch (Exception e) {
            log.debug("[minimap-location] save debug image failed: {}", fileName, e);
            return null;
        }
    }

    private static MiniMapLocationCloudDecision requiredFailure(String reason) {
        return MiniMapLocationCloudDecision.builder()
                .status(MiniMapLocationCloudDecision.Status.REQUIRED_FAILURE)
                .reason(reason)
                .build();
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static BufferedImage decodePng(String payloadBase64) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(payloadBase64.trim());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IOException("payload is not a PNG image");
        }
        return image;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder result = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
