package com.bot.dhxy.vision;



import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.ocr.PlayerAnchorMatch;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.navigation.MapLabelTemplateMatch;
import com.bot.dhxy.model.navigation.TemplateLocationInfo;
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
    private final TextRecognizer ocr;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final MiniMapCoordinateReader miniMapCoordinateReader;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final MapNameCanonicalizer mapNameCanonicalizer;

    private static final int ANCHOR_DIFF_X = 46;
    private static final int ANCHOR_DIFF_Y = 59;

    private static final int height = 35;
    private static final int width = 178;
    private static final int NAME_CHAR_GAP_PX = 4;
    private static final double MAP_LABEL_CONFIDENT_DIFFERENT_MATCH_SCORE = 0.62;
    private static final double FLOOR_TEMPLATE_TRUST_SCORE = 0.995;
    private static final int MAP_LABEL_LEARN_MIN_WHITE_PIXELS = 12;
    private static final double MAP_LABEL_LEARN_MIN_DENSITY = 0.02;
    private static final double MAP_LABEL_LEARN_MAX_DENSITY = 0.65;
    private static final int LOCATION_COORDINATE_PLAUSIBLE_MARGIN_PX = 80;
    private static final Path MAP_LABEL_TEMPLATE_DIR = Path.of("images", "template", "map_label")
            .toAbsolutePath()
            .normalize();
    private static final Path LOCATION_FAILURE_CASE_DIR = Path.of("images", "failure-cases", "location")
            .toAbsolutePath()
            .normalize();
    private static final DateTimeFormatter FAILURE_CASE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    /**
     * Scans the current bound game window for the player's map name and coordinate.
     *
     * <p>CR246 (CR208-16): the cloud {@code MINIMAP_LOCATION READ_LOCATION} call owns the whole
     * fallback chain now (label/coordinate templates first, cloud OCR second). The retired local
     * sidecar and Baidu OCR stages are kept below as {@code @Deprecated} rollback implementations
     * only and must not re-enter the production path. Bound-window mode uses no-focus capture,
     * while legacy no-context callers may still focus before Robot capture.</p>
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

    public Point extractPlayerPhysicalAnchor(List<OcrWordResult> ocrResults,
                                             String fullName,
                                             int scanStartX,
                                             int scanStartY,
                                             int heightOffset) {
        PlayerAnchorMatch match = extractPlayerAnchorMatch(ocrResults, fullName, scanStartX, scanStartY, heightOffset);
        return match == null ? null : match.anchor();
    }

    public PlayerAnchorMatch extractPlayerAnchorMatch(List<OcrWordResult> ocrResults,
                                                      String fullName,
                                                      int scanStartX,
                                                      int scanStartY,
                                                      int heightOffset) {
        if (ocrResults == null || fullName == null || fullName.isEmpty()) {
            return null;
        }

        String cleanFullName = fullName.replace(" ", "");

        for (OcrWordResult w : ocrResults) {
            String text = w.getText();
            if (text == null) {
                continue;
            }

            boolean wholeNameCoreMatch = looksLikeWholeNameIgnoringSymbols(cleanFullName, text);
            String matchedFragment = wholeNameCoreMatch ? cleanFullName : findLongestValidFragment(cleanFullName, text);
            if (matchedFragment != null) {
                int compensationX = 0;
                if (!wholeNameCoreMatch) {
                    int startIndex = cleanFullName.indexOf(matchedFragment);

                    double fullCenterPixel = calculateStringPixelWidth(cleanFullName) / 2.0;
                    double fragmentCenterPixel = calculateFragmentCenterPixel(cleanFullName, startIndex, matchedFragment);

                    compensationX = (int) Math.round(fullCenterPixel - fragmentCenterPixel);
                }
                String matchMode = wholeNameCoreMatch ? "WHOLE_NAME_CORE" : "FRAGMENT";
                log.info("[location] extracted name from OCR: raw={} fragment={} mode={} compensationX={}",
                        text, matchedFragment, matchMode, compensationX);

                int absoluteX = scanStartX + w.getX() + compensationX;
                int absoluteY = scanStartY + w.getY() + heightOffset;

                log.info("[location] physical anchor corrected: fullName={} fragment={} mode={} compensationX={} anchor=({}, {})",
                        cleanFullName, matchedFragment, matchMode, compensationX, absoluteX, absoluteY);

                OcrWindowRegion textRect = new OcrWindowRegion(
                        scanStartX + w.getLeft(),
                        scanStartY + w.getTop(),
                        scanStartX + w.getLeft() + Math.max(1, w.getWidth()),
                        scanStartY + w.getTop() + Math.max(1, w.getHeight())
                );
                return new PlayerAnchorMatch(
                        new Point(absoluteX, absoluteY),
                        text,
                        matchedFragment,
                        matchMode,
                        compensationX,
                        textRect,
                        w.getScore()
                );
            }
        }
        return null;
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

    /**
     * Learn missing floor-map label templates from OCR without overriding the template hit.
     *
     * <p>This is only used after the mini-map template matcher selected a map whose name ends with
     * {@code 层}. Floor names are visually similar, so missing templates can make "一层" look like
     * the nearest saved "三层" template. OCR is used as a verifier; when it reads a different map
     * name that has no template file yet, the freshly captured coordinate strip is normalized and
     * saved into {@code images/template/map_label}. The OCR result is intentionally not returned as
     * the selected location because OCR can misread visually similar floor names and should not
     * overrule an already matched mini-map template.</p>
     *
     * @param templateLocation template-based location candidate, including a window-scoped cleaned
     *                         label image path and logical coordinate.
     * @param scanStartedAt timestamp of the outer location scan, used for consistent elapsed logs.
     */
    @Deprecated(since = "CR246")
    private void verifyFloorTemplateWithOcr(TemplateLocationInfo templateLocation,
                                            long scanStartedAt) {
        String path = windowScopedTempPath.resolve("tmp_pos_floor_verify.png");
        if (!captureCurrentLocationStrip(path)) {
            log.info("[location] floor template OCR verify skipped: map={} reason=capture-failed label={}",
                    templateLocation.mapName(), templateLocation.mapLabelPath());
            return;
        }

        long localStartedAt = System.currentTimeMillis();
        LocationInfo local = ocr.parseLocationLocalOnly(path);
        local = canonicalizeOcrLocation(local, "location:floor-template-verify");
        if (local == null) {
            log.info("[location] floor template OCR verify local miss: templateMap={} score={} label={}",
                    templateLocation.mapName(),
                    String.format("%.3f", templateLocation.mapLabelScore()),
                    templateLocation.mapLabelPath());
            return;
        }

        learnMissingMapLabelTemplate(local.mapName, path, true);
        if (!templateLocation.mapName().equals(local.mapName)) {
            log.info("[location] floor template OCR disagreed but template kept: templateMap={} ocrMap={} coord=({}, {}) "
                            + "templateScore={} elapsedMs={} localElapsedMs={} label={}",
                    templateLocation.mapName(), local.mapName, local.x, local.y,
                    String.format("%.3f", templateLocation.mapLabelScore()),
                    System.currentTimeMillis() - scanStartedAt,
                    System.currentTimeMillis() - localStartedAt,
                    templateLocation.mapLabelPath());
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
     * Persist a cleaned mini-map label as a new template when OCR identifies a map whose template is
     * missing.
     *
     * @param mapName OCR-read map name. Blank values are ignored.
     * @param sourceImagePath window-scoped cleaned label image or the captured coordinate strip.
     * @param sourceIsCoordinateStrip true when {@code sourceImagePath} is the full mini-map
     *                                coordinate strip and the map label still needs cropping.
     */
    @Deprecated(since = "CR246")
    private void learnMissingMapLabelTemplate(String mapName, String sourceImagePath, boolean sourceIsCoordinateStrip) {
        if (mapName == null || mapName.isBlank() || sourceImagePath == null || sourceImagePath.isBlank()) {
            return;
        }
        Path target = MAP_LABEL_TEMPLATE_DIR.resolve(safeTemplateFileName(mapName) + ".png").normalize();
        if (Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(MAP_LABEL_TEMPLATE_DIR);
            if (sourceIsCoordinateStrip) {
                BufferedImage strip = ImageIO.read(Path.of(sourceImagePath).toFile());
                if (strip == null) {
                    log.warn("[location] learn minimap label template skipped: map={} source={} reason=strip-read-null",
                            mapName, sourceImagePath);
                    return;
                }
                try {
                    Optional<BufferedImage> croppedLabel = miniMapCoordinateReader.extractCleanMapLabelImageFromCoordinateStrip(strip);
                    if (croppedLabel.isEmpty()) {
                        log.warn("[location] learn minimap label template skipped: map={} source={} reason=label-crop-miss",
                                mapName, sourceImagePath);
                        return;
                    }
                    try {
                        if (!shouldLearnMapLabelTemplate(mapName, croppedLabel.get(), sourceImagePath, true)) {
                            return;
                        }
                        ImageIO.write(croppedLabel.get(), "png", target.toFile());
                    } finally {
                        croppedLabel.get().flush();
                    }
                } finally {
                    strip.flush();
                }
            } else {
                BufferedImage label = ImageIO.read(Path.of(sourceImagePath).toFile());
                if (label == null) {
                    log.warn("[location] learn minimap label template skipped: map={} source={} reason=label-read-null",
                            mapName, sourceImagePath);
                    return;
                }
                try {
                    Optional<BufferedImage> normalized = miniMapCoordinateReader.normalizeMapLabelTemplateImage(mapName, label);
                    if (normalized.isEmpty()) {
                        log.warn("[location] learn minimap label template skipped: map={} source={} reason=label-normalize-miss",
                                mapName, sourceImagePath);
                        return;
                    }
                    try {
                        if (!shouldLearnMapLabelTemplate(mapName, normalized.get(), sourceImagePath, false)) {
                            return;
                        }
                        ImageIO.write(normalized.get(), "png", target.toFile());
                    } finally {
                        normalized.get().flush();
                    }
                } finally {
                    label.flush();
                }
            }
            miniMapCoordinateReader.invalidateMapLabelTemplateCache();
            log.info("[location] learned missing minimap label template: map={} path={} source={} sourceType={}",
                    mapName, target, sourceImagePath, sourceIsCoordinateStrip ? "coordinate-strip" : "clean-label");
        } catch (IOException e) {
            log.warn("[location] learn minimap label template failed: map={} source={} target={} reason={}",
                    mapName, sourceImagePath, target, e.getMessage(), e);
        }
    }

    private boolean shouldLearnMapLabelTemplate(String mapName,
                                                BufferedImage label,
                                                String sourceImagePath,
                                                boolean sourceIsCoordinateStrip) {
        if (!isLearnableMapName(mapName)) {
            log.warn("[location] learn minimap label template skipped: map={} source={} reason=bad-map-name",
                    mapName, sourceImagePath);
            return false;
        }
        if (!isLearnableLabelImage(label)) {
            log.warn("[location] learn minimap label template skipped: map={} source={} reason=bad-label-image",
                    mapName, sourceImagePath);
            return false;
        }
        if (!miniMapCoordinateReader.isMapLabelWidthPlausible(mapName, label)) {
            log.warn("[location] learn minimap label template skipped: map={} source={} reason=bad-label-width size={}x{}",
                    mapName, sourceImagePath, label.getWidth(), label.getHeight());
            return false;
        }

        Optional<MapLabelTemplateMatch> best = miniMapCoordinateReader.recognizeMapLabelImage(label);
        if (best.isPresent()
                && best.get().score() >= MAP_LABEL_CONFIDENT_DIFFERENT_MATCH_SCORE
                && !mapName.equals(best.get().mapName())) {
            log.warn("[location] learn minimap label template skipped: map={} source={} reason=confident-other-map "
                            + "bestMap={} bestScore={} sourceType={}",
                    mapName, sourceImagePath, best.get().mapName(),
                    String.format("%.3f", best.get().score()),
                    sourceIsCoordinateStrip ? "coordinate-strip" : "clean-label");
            return false;
        }
        return true;
    }

    private boolean isLearnableMapName(String mapName) {
        String value = mapName == null ? "" : mapName.trim();
        return value.length() >= 2
                && value.length() <= 12
                && value.matches(".*[\\u4E00-\\u9FFF].*")
                && !value.matches(".*[\\s,，。.;；:：()（）\\[\\]【】{}<>《》].*");
    }

    private boolean isLearnableLabelImage(BufferedImage label) {
        if (label == null || label.getWidth() < 8 || label.getHeight() < 6) {
            return false;
        }
        int whitePixels = 0;
        for (int y = 0; y < label.getHeight(); y++) {
            for (int x = 0; x < label.getWidth(); x++) {
                if ((label.getRGB(x, y) & 0x00FFFFFF) == 0x00FFFFFF) {
                    whitePixels++;
                }
            }
        }
        int totalPixels = Math.max(1, label.getWidth() * label.getHeight());
        double density = whitePixels / (double) totalPixels;
        return whitePixels >= MAP_LABEL_LEARN_MIN_WHITE_PIXELS
                && density >= MAP_LABEL_LEARN_MIN_DENSITY
                && density <= MAP_LABEL_LEARN_MAX_DENSITY;
    }

    private String safeTemplateFileName(String mapName) {
        return mapName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    /**
     * Return true for map labels whose floor number is ambiguous enough to deserve OCR verification.
     *
     * @param mapName map label returned by the template matcher; may be null or blank.
     * @return true when the name looks like a dungeon/tower floor such as 大雁塔三层 or 凤巢七层.
     */
    private boolean isDungeonFloorMap(String mapName) {
        return mapName != null && mapName.matches(".*[一二三四五六七八九十]+层$");
    }

    private boolean captureCurrentLocationStrip(String path) {
        int[] pics = coordinateHelper.getScaledRect(ANCHOR_DIFF_X, ANCHOR_DIFF_Y, width, height);
        return tracker.captureToFile("location-current", path, pics[0], pics[1], pics[2], pics[3]);
    }

    /**
     * Throws when the current task requested stop or the worker thread was interrupted.
     *
     * @param stage diagnostic name for the current location-recognition stage.
     */
    private void checkpoint(String stage) {
        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "location scan interrupted at " + stage);
    }

    // 估算角色名字符宽度，用 OCR 片段反推角色脚底锚点。
    private int getCharPixelWidth(char c) {
        if (String.valueOf(c).matches("[\u4e00-\u9fa5]")) {
            return 13;
        } else if (c >= 'a' && c <= 'z') {
            return 9;
        } else if (c >= 'A' && c <= 'Z') {
            return 9;
        } else if (c >= '0' && c <= '9') {
            return 5;
        } else {
            return 9;
        }
    }

    private int calculateStringPixelWidth(String str) {
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            if (i > 0) {
                width += NAME_CHAR_GAP_PX;
            }
            width += getCharPixelWidth(str.charAt(i));
        }
        return width;
    }

    private double calculateFragmentCenterPixel(String fullName, int startIndex, String fragment) {
        int prefixWidth = startIndex <= 0
                ? 0
                : calculateStringPixelWidth(fullName.substring(0, startIndex)) + NAME_CHAR_GAP_PX;
        return prefixWidth + calculateStringPixelWidth(fragment) / 2.0;
    }

    private String findLongestValidFragment(String fullName, String ocrText) {
        String cleanOcr = ocrText.replace(" ", "");

        for (int len = fullName.length(); len >= 2; len--) {
            for (int i = 0; i <= fullName.length() - len; i++) {
                String sub = fullName.substring(i, i + len);
                if (cleanOcr.contains(sub)) {
                    return sub;
                }
            }
        }
        return null;
    }

    private boolean looksLikeWholeNameIgnoringSymbols(String fullName, String ocrText) {
        String expectedCore = nameCore(fullName);
        String ocrCore = nameCore(ocrText);
        return expectedCore.length() >= 2 && (ocrCore.equals(expectedCore) || ocrCore.contains(expectedCore));
    }

    private String nameCore(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            if ((c >= '\u4e00' && c <= '\u9fff') || (c >= '0' && c <= '9')) {
                builder.append(c);
            } else if (c >= 'a' && c <= 'z') {
                builder.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }

}
