package com.bot.dhxy.vision;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

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

    private static final int ANCHOR_DIFF_X = 46;
    private static final int ANCHOR_DIFF_Y = 59;

    private static final int height = 35;
    private static final int width = 178;
    private static final int NAME_CHAR_GAP_PX = 4;
    private static final Path MAP_LABEL_TEMPLATE_DIR = Path.of("images", "template", "map_label")
            .toAbsolutePath()
            .normalize();

    /**
     * Scans the current bound game window for the player's map name and coordinate.
     *
     * <p>The fallback order is mini-map template, local OCR, then Baidu OCR. Bound-window mode uses
     * no-focus capture, while legacy no-context callers may still focus before Robot capture. Stop
     * checkpoints are placed before expensive fallback stages so UI stop can cut off the remaining
     * OCR chain promptly.</p>
     *
     * @return recognized location, or {@code null} when capture/recognition fails. If the current
     *         task has requested stop, this method throws {@link TaskStopRequestedException}.
     */
    public TextRecognizer.LocationInfo scanCurrentLocation() {
        long startedAt = System.currentTimeMillis();
        long latencyStart = LatencyMetrics.start();
        String provider = "NONE";
        TextRecognizer.LocationInfo selected = null;
        String path = windowScopedTempPath.resolve("tmp_pos.png");
        try {
            checkpoint("start location scan");
            if (windowTaskContextHolder.rawCurrent().isPresent()) {
                log.info("[location] scan current no-focus: path={}", path);
            } else {
                log.info("[location] scan current legacy focused fallback: path={}", path);
                if (!tracker.bringWindowToFront()) {
                    log.warn("[location] failed to bring window to front before coordinate scan");
                    return null;
                }
            }

            /*
             * Stage 1: use the fast mini-map template reader first. This keeps normal position sync off
             * the OCR/network path when the local map label and coordinate templates are confident.
             */
            TextRecognizer.LocationInfo templateLocation = scanByMiniMapTemplate(startedAt);
            checkpoint("after minimap template location scan");
            if (templateLocation != null) {
                provider = "MINIMAP_TEMPLATE";
                selected = templateLocation;
                return templateLocation;
            }

            /*
             * Stage 2: capture the coordinate strip and try local OCR. Check stop before and after the
             * capture because HWND/Robot capture can still take noticeable time on a busy desktop.
             */
            checkpoint("before coordinate strip capture");
            if (captureCurrentLocationStrip(path)) {
                checkpoint("after coordinate strip capture");
                long localStartedAt = System.currentTimeMillis();
                checkpoint("before local location OCR");
                TextRecognizer.LocationInfo local = ocr.parseLocationLocalOnly(path);
                checkpoint("after local location OCR");
                if (local != null) {
                    provider = "LOCAL_OCR";
                    selected = local;
                    log.info("[location] selected provider=LOCAL_OCR elapsedMs={} localElapsedMs={} location={}",
                            System.currentTimeMillis() - startedAt,
                            System.currentTimeMillis() - localStartedAt,
                            local);
                    return local;
                }

                /*
                 * Stage 3: Baidu OCR is the slowest fallback and can refresh tokens over the network.
                 * Do not enter it after a stop request; that was the source of the slow stop observed in
                 * the latest Xiuluo run.
                 */
                long baiduStartedAt = System.currentTimeMillis();
                checkpoint("before baidu location OCR");
                TextRecognizer.LocationInfo baidu = ocr.parseLocationBaiduOnly(path);
                checkpoint("after baidu location OCR");
                provider = baidu == null ? "NONE" : "BAIDU_OCR";
                selected = baidu;
                log.info("[location] selected provider={} elapsedMs={} baiduElapsedMs={} location={}",
                        provider,
                        System.currentTimeMillis() - startedAt,
                        System.currentTimeMillis() - baiduStartedAt,
                        baidu);
                return baidu;
            }
            log.warn("[location] coordinate strip capture failed: path={}", path);
            return null;
        } finally {
            LatencyMetrics.info(log, "location.scanCurrent", latencyStart,
                    "provider=" + provider + " result=" + (selected == null ? "NONE" : selected.toString()));
        }
    }

    public Point extractPlayerPhysicalAnchor(List<TextRecognizer.OcrWordResult> ocrResults,
                                             String fullName,
                                             int scanStartX,
                                             int scanStartY,
                                             int heightOffset) {
        PlayerAnchorMatch match = extractPlayerAnchorMatch(ocrResults, fullName, scanStartX, scanStartY, heightOffset);
        return match == null ? null : match.anchor();
    }

    public PlayerAnchorMatch extractPlayerAnchorMatch(List<TextRecognizer.OcrWordResult> ocrResults,
                                                      String fullName,
                                                      int scanStartX,
                                                      int scanStartY,
                                                      int heightOffset) {
        if (ocrResults == null || fullName == null || fullName.isEmpty()) {
            return null;
        }

        String cleanFullName = fullName.replace(" ", "");

        for (TextRecognizer.OcrWordResult w : ocrResults) {
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

    public record PlayerAnchorMatch(Point anchor,
                                    String matchedText,
                                    String matchedFragment,
                                    String matchMode,
                                    int compensationX,
                                    OcrWindowRegion textRect,
                                    double score) {
        public String toDetailText() {
            return "anchor=" + (anchor == null ? "null" : anchor.x + "," + anchor.y)
                    + ", text=" + matchedText
                    + ", fragment=" + matchedFragment
                    + ", mode=" + matchMode
                    + ", compensationX=" + compensationX
                    + ", textRect=" + (textRect == null ? "-" : textRect.toShortText())
                    + ", score=" + String.format("%.3f", score);
        }
    }

    private TextRecognizer.LocationInfo scanByMiniMapTemplate(long startedAt) {
        long templateStartedAt = System.currentTimeMillis();
        try {
            checkpoint("before minimap template reader");
            return miniMapCoordinateReader.readCurrentTemplateLocation()
                    .map(location -> {
                        checkpoint("convert minimap template result");
                        /*
                         * Dungeon floor labels share a long common prefix, for example "大雁塔一层"
                         * and "大雁塔三层". The template fast path is still useful, but if OCR reads
                         * a different floor and that floor has no saved template yet, the template
                         * hit was probably a nearest-neighbor false positive. In that case use the
                         * OCR result for this scan and persist the cleaned label so the next run can
                         * stay on the fast template path.
                         */
                        if (isDungeonFloorMap(location.mapName())) {
                            TextRecognizer.LocationInfo verified = verifyFloorTemplateWithOcr(location, startedAt);
                            if (verified != null) {
                                return verified;
                            }
                        }
                        MapCoordinate coordinate = location.coordinate();
                        TextRecognizer.LocationInfo info = new TextRecognizer.LocationInfo(
                                location.mapName(),
                                coordinate.getX(),
                                coordinate.getY()
                        );
                        log.info("[location] selected provider=MINIMAP_TEMPLATE elapsedMs={} templateElapsedMs={} "
                                        + "map={} coord=({}, {}) score={} label={}",
                                System.currentTimeMillis() - startedAt,
                                System.currentTimeMillis() - templateStartedAt,
                                location.mapName(), coordinate.getX(), coordinate.getY(),
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
     * Verify a floor-map template hit with OCR and auto-learn missing map-label templates.
     *
     * <p>This is only used after the mini-map template matcher selected a map whose name ends with
     * {@code 层}. Floor names are visually similar, so missing templates can make "一层" look like
     * the nearest saved "三层" template. OCR is used as a verifier; when it reads a different map
     * name that has no template file yet, the cleaned label image captured by the template path is
     * copied into {@code images/template/map_label}. This keeps the first encounter safe and makes
     * later encounters faster.</p>
     *
     * @param templateLocation template-based location candidate, including a window-scoped cleaned
     *                         label image path and logical coordinate.
     * @param scanStartedAt timestamp of the outer location scan, used for consistent elapsed logs.
     * @return OCR-verified location when OCR disagrees with the template or learns a missing label;
     *         null when the template candidate can remain the selected result.
     */
    private TextRecognizer.LocationInfo verifyFloorTemplateWithOcr(MiniMapCoordinateReader.TemplateLocationInfo templateLocation,
                                                                   long scanStartedAt) {
        String path = windowScopedTempPath.resolve("tmp_pos_floor_verify.png");
        if (!captureCurrentLocationStrip(path)) {
            log.info("[location] floor template OCR verify skipped: map={} reason=capture-failed label={}",
                    templateLocation.mapName(), templateLocation.mapLabelPath());
            return null;
        }

        long localStartedAt = System.currentTimeMillis();
        TextRecognizer.LocationInfo local = ocr.parseLocationLocalOnly(path);
        if (local == null) {
            log.info("[location] floor template OCR verify local miss: templateMap={} score={} label={}",
                    templateLocation.mapName(),
                    String.format("%.3f", templateLocation.mapLabelScore()),
                    templateLocation.mapLabelPath());
            return null;
        }

        learnMissingMapLabelTemplate(local.mapName, templateLocation.mapLabelPath());
        if (!templateLocation.mapName().equals(local.mapName)) {
            log.info("[location] floor template corrected by OCR: templateMap={} ocrMap={} coord=({}, {}) "
                            + "templateScore={} elapsedMs={} localElapsedMs={} label={}",
                    templateLocation.mapName(), local.mapName, local.x, local.y,
                    String.format("%.3f", templateLocation.mapLabelScore()),
                    System.currentTimeMillis() - scanStartedAt,
                    System.currentTimeMillis() - localStartedAt,
                    templateLocation.mapLabelPath());
            return local;
        }
        return null;
    }

    /**
     * Persist a cleaned mini-map label as a new template when OCR identifies a map whose template is
     * missing.
     *
     * @param mapName OCR-read map name. Blank values are ignored.
     * @param cleanedLabelPath window-scoped cleaned label image produced by the template reader.
     */
    private void learnMissingMapLabelTemplate(String mapName, String cleanedLabelPath) {
        if (mapName == null || mapName.isBlank() || cleanedLabelPath == null || cleanedLabelPath.isBlank()) {
            return;
        }
        Path target = MAP_LABEL_TEMPLATE_DIR.resolve(safeTemplateFileName(mapName) + ".png").normalize();
        if (Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(MAP_LABEL_TEMPLATE_DIR);
            Files.copy(Path.of(cleanedLabelPath), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("[location] learned missing minimap label template: map={} path={}", mapName, target);
        } catch (IOException e) {
            log.warn("[location] learn minimap label template failed: map={} source={} target={} reason={}",
                    mapName, cleanedLabelPath, target, e.getMessage(), e);
        }
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
        taskExecutionContextHolder.checkpointIfPresent();
        if (Thread.currentThread().isInterrupted()) {
            throw new TaskStopRequestedException("location scan interrupted at " + stage);
        }
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
