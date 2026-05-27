package com.bot.dhxy.vision;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.navigation.MapLabelTemplateMatch;
import com.bot.dhxy.model.navigation.MiniMapSnapshot;
import com.bot.dhxy.model.navigation.TemplateLocationInfo;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiniMapCoordinateReader {

    private static final int COORD_SCAN_X = 46;
    private static final int COORD_SCAN_Y = 59;
    private static final int COORD_SCAN_W = 178;
    private static final int COORD_SCAN_H = 35;

    private static final int CHAR_PAD = 1;
    private static final int MAX_COORD_DIGITS = 3;
    private static final double TEMPLATE_MATCH_THRESHOLD = 0.45;
    private static final double MAP_LABEL_MATCH_THRESHOLD = 0.62;
    private static final String TEMPLATE_DIR = "images/template/coord_digits";
    private static final Path MAP_LABEL_TEMPLATE_DIR = Path.of("images", "template", "map_label")
            .toAbsolutePath()
            .normalize();
    private static final DateTimeFormatter FAILURE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;

    public Optional<MapCoordinate> readCurrentCoordinate() {
        MiniMapSnapshot snapshot = readCurrentSnapshot(false, false);
        return Optional.ofNullable(snapshot.coordinate());
    }

    public Optional<TemplateLocationInfo> readCurrentTemplateLocation() {
        // Stage 1: capture the mini-map coordinate strip once and reuse that same pixels
        // for both coordinate digits and map-label matching. Re-capturing here can mix
        // two different frames if the player moves or the window refreshes mid-read.
        BufferedImage raw = captureCoordinateStrip();
        if (raw == null) {
            return Optional.empty();
        }
        try {
            // Stage 2: parse the numeric coordinate with local digit templates. The map
            // label is handled separately below because Chinese map labels use full-word
            // templates instead of per-digit glyph splitting.
            CoordinateRecognition recognition = recognizeCoordinate(raw, false, false);
            MapCoordinate coordinate = recognition.coordinate().orElse(null);
            if (coordinate == null) {
                return Optional.empty();
            }

            // Stage 3: crop the cleaned map label to the left of the coordinate brackets
            // and match it against saved map-label templates. A bad label match must fail
            // this fast path so the caller can fall back to local/Baidu OCR.
            BufferedImage label = extractCleanMapLabelImage(raw);
            if (label == null) {
                return Optional.empty();
            }
            try {
                String labelPath = saveDebugImage(label, "minimap_map_label_clean.png");
                Optional<MapLabelTemplateMatch> match = recognizeMapLabelImage(label);
                if (match.isEmpty() || match.get().score() < MAP_LABEL_MATCH_THRESHOLD) {
                    match.ifPresent(best -> log.info("[minimap-location] map label low score: map={} score={} threshold={}",
                            best.mapName(), String.format("%.3f", best.score()), MAP_LABEL_MATCH_THRESHOLD));
                    return Optional.empty();
                }
                MapLabelTemplateMatch best = match.get();
                return Optional.of(new TemplateLocationInfo(
                        best.mapName(),
                        coordinate,
                        best.score(),
                        labelPath
                ));
            } finally {
                label.flush();
            }
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
            return Optional.ofNullable(extractCleanMapLabelImage(raw));
        } finally {
            raw.flush();
        }
    }

    public Optional<BufferedImage> extractCleanMapLabelImageFromCoordinateStrip(BufferedImage raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(extractCleanMapLabelImage(raw));
    }

    public Optional<MapLabelTemplateMatch> recognizeMapLabelFromCoordinateStrip(BufferedImage raw) {
        Optional<BufferedImage> label = extractCleanMapLabelImageFromCoordinateStrip(raw);
        if (label.isEmpty()) {
            return Optional.empty();
        }
        try {
            return recognizeMapLabelImage(label.get());
        } finally {
            label.ifPresent(BufferedImage::flush);
        }
    }

    public Optional<MapLabelTemplateMatch> recognizeMapLabelImage(BufferedImage label) {
        return findBestMapLabelMatch(label);
    }

    public MiniMapSnapshot readCurrentLocationSnapshot() {
        return readCurrentSnapshot(true, true);
    }

    public MiniMapSnapshot readLocationSnapshotFromCoordinateStrip(BufferedImage raw,
                                                                  boolean includeMapName,
                                                                  boolean debugOutput) {
        if (raw == null) {
            return new MiniMapSnapshot(null, null);
        }
        CoordinateRecognition recognition = recognizeCoordinate(raw, includeMapName, debugOutput);
        return new MiniMapSnapshot(recognition.mapLabelPath(), recognition.coordinate().orElse(null));
    }

    private MiniMapSnapshot readCurrentSnapshot(boolean includeMapName, boolean debugOutput) {
        BufferedImage raw = captureCoordinateStrip();
        if (raw == null) {
            return new MiniMapSnapshot(null, null);
        }

        try {
            CoordinateRecognition recognition = recognizeCoordinate(raw, includeMapName, debugOutput);
            return new MiniMapSnapshot(recognition.mapLabelPath(), recognition.coordinate().orElse(null));
        } finally {
            raw.flush();
        }
    }

    private CoordinateRecognition recognizeCoordinate(BufferedImage raw, boolean saveMapLabel, boolean debugOutput) {
        // Stage 1: threshold the mini-map strip into a binary image. Downstream glyph
        // segmentation and template scoring assume white foreground on black background.
        BufferedImage clean = cleanCoordinateText(raw);
        try {
            if (debugOutput) {
                saveDebugImage(clean, "minimap_coord_clean.png");
            }

            // Stage 2: find connected white components. This keeps the reader independent
            // from OCR and lets us reason about brackets, comma, and each digit separately.
            List<GlyphBox> glyphs = segmentGlyphs(clean);
            if (debugOutput) {
                saveGlyphDebugImages(clean, glyphs);
            }
            if (glyphs.isEmpty()) {
                if (debugOutput) {
                    String failurePath = saveFailureDebugImages(raw, clean, "no_glyph");
                    log.info("[坐标数字] 未切出任何候选字符 failPath={}", failurePath);
                }
                return new CoordinateRecognition(null, Optional.empty());
            }

            // Stage 3: locate the coordinate bracket pair first, then use the bracket span
            // as a hard boundary for comma/digit recognition. This avoids accidentally
            // reading map-name glyphs as coordinate digits.
            Optional<BracketSpan> span = findBracketSpan(clean, glyphs);
            if (span.isEmpty()) {
                if (debugOutput) {
                    String failurePath = saveFailureDebugImages(raw, clean, "no_bracket");
                    log.info("[坐标数字] 未找到坐标括号区域 glyphs={} failPath={}", glyphs.size(), failurePath);
                }
                return new CoordinateRecognition(null, Optional.empty());
            }

            // Stage 4: find a comma whose left and right sides both decode to plausible
            // coordinate numbers. The comma is not trusted by shape alone because noise
            // dots around the mini-map can look comma-like.
            String mapLabelPath = saveMapLabel ? saveMapLabelImage(raw, span.get()) : null;
            Optional<GlyphBox> comma = findCommaGlyph(clean, glyphs, span.get(), debugOutput);
            if (comma.isEmpty()) {
                if (debugOutput) {
                    String failurePath = saveFailureDebugImages(raw, clean, "no_comma");
                    log.info("[坐标数字] 未找到坐标逗号区域 mapLabelPath={} span={} failPath={}",
                            mapLabelPath, span.get(), failurePath);
                }
                return new CoordinateRecognition(mapLabelPath, Optional.empty());
            }

            // Stage 5: decode left/right digit ranges and only return a coordinate when
            // both sides are complete, non-unknown digit strings.
            DigitRecognition leftDigits = recognizeDigitRange(clean,
                    span.get().leftMaxX + 2, comma.get().minX - 1, span.get().minY - 1, span.get().maxY + 1);
            DigitRecognition rightDigits = recognizeDigitRange(clean,
                    comma.get().maxX + 1, span.get().rightMinX - 2, span.get().minY - 1, span.get().maxY + 1);
            String text = leftDigits.text() + "," + rightDigits.text();

            if (debugOutput) {
                log.info("[坐标数字] 识别结果 mapLabelPath={} raw='{}' span={} comma={} leftDigits={} rightDigits={} leftScore={} rightScore={}",
                        mapLabelPath, text, span.get(), comma.get(), leftDigits.boxes().size(), rightDigits.boxes().size(),
                        leftDigits.score(), rightDigits.score());
            }
            Optional<MapCoordinate> coordinate = parseCoordinate(text, mapLabelPath, debugOutput);
            if (coordinate.isEmpty() && debugOutput) {
                String failurePath = saveFailureDebugImages(raw, clean, "bad_raw");
                log.info("[坐标数字] 保存失败帧 failPath={} raw='{}'", failurePath, text);
            }
            return new CoordinateRecognition(mapLabelPath, coordinate);
        } finally {
            clean.flush();
        }
    }

    private BufferedImage captureCoordinateStrip() {
        int[] rect = coordinateHelper.getScaledRect(COORD_SCAN_X, COORD_SCAN_Y, COORD_SCAN_W, COORD_SCAN_H);
        return tracker.captureToMemory("minimap-coordinate", rect[0], rect[1], rect[2], rect[3]);
    }

    private BufferedImage cleanCoordinateText(BufferedImage raw) {
        BufferedImage clean = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < raw.getHeight(); y++) {
            for (int x = 0; x < raw.getWidth(); x++) {
                int rgb = raw.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                clean.setRGB(x, y, isCoordinateTextPixel(r, g, b) ? 0xFFFFFF : 0x000000);
            }
        }
        return clean;
    }

    private boolean isCoordinateTextPixel(int r, int g, int b) {
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        return max >= 145
                && min >= 100
                && hsb[1] <= 0.32f
                && hsb[2] >= 0.56f
                && (max - min) <= 85;
    }

    private List<GlyphBox> segmentGlyphs(BufferedImage clean) {
        boolean[][] visited = new boolean[clean.getHeight()][clean.getWidth()];
        List<GlyphBox> glyphs = new ArrayList<>();

        for (int y = 0; y < clean.getHeight(); y++) {
            for (int x = 0; x < clean.getWidth(); x++) {
                if (visited[y][x] || !isWhite(clean, x, y)) {
                    continue;
                }
                GlyphBox box = floodFill(clean, visited, x, y);
                if (isUsefulGlyph(box)) {
                    glyphs.add(box.expand(clean.getWidth(), clean.getHeight(), CHAR_PAD));
                }
            }
        }

        glyphs.sort(Comparator.comparingInt(g -> g.minX));
        return glyphs;
    }

    private GlyphBox floodFill(BufferedImage image, boolean[][] visited, int startX, int startY) {
        List<int[]> stack = new ArrayList<>();
        stack.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        GlyphBox box = new GlyphBox(startX, startY, startX, startY);
        for (int i = 0; i < stack.size(); i++) {
            int[] point = stack.get(i);
            int x = point[0];
            int y = point[1];
            box.include(x, y);

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) {
                        continue;
                    }
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= image.getWidth() || ny >= image.getHeight()) {
                        continue;
                    }
                    if (!visited[ny][nx] && isWhite(image, nx, ny)) {
                        visited[ny][nx] = true;
                        stack.add(new int[]{nx, ny});
                    }
                }
            }
        }
        return box;
    }

    private boolean isUsefulGlyph(GlyphBox box) {
        int w = box.width();
        int h = box.height();
        return w >= 1 && h >= 2 && box.pixelCount >= 2 && w <= 18 && h <= 18;
    }

    private Optional<BracketSpan> findBracketSpan(BufferedImage clean, List<GlyphBox> glyphs) {
        // Brackets are narrow/tall components. Restricting candidates up front keeps the
        // expensive inner validation focused on likely coordinate envelopes.
        List<GlyphBox> candidates = glyphs.stream()
                .filter(g -> g.width() <= 6 && g.height() >= 8 && g.height() <= 16 && g.pixelCount >= 8)
                .sorted(Comparator.comparingInt(g -> g.minX))
                .toList();
        BracketSpan best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                GlyphBox left = candidates.get(i);
                GlyphBox right = candidates.get(j);
                int width = right.maxX - left.minX + 1;
                if (width < 35 || width > 80) {
                    continue;
                }
                // A bracket pair is accepted only when there is a plausible comma inside
                // and both sides around that comma decode as coordinate digits.
                List<GlyphBox> commas = glyphs.stream()
                        .filter(g ->
                        g.minX > left.maxX
                                && g.maxX < right.minX
                                && g.minY >= left.minY + 6
                                && g.width() <= 4
                                && g.height() <= 5)
                        .sorted(Comparator.comparingInt(g -> g.minX))
                        .toList();
                for (GlyphBox comma : commas) {
                    BracketSpan span = new BracketSpan(
                            left.minX, left.maxX, right.minX, right.maxX,
                            Math.min(left.minY, right.minY), Math.max(left.maxY, right.maxY)
                    );
                    DigitRecognition leftDigits = recognizeDigitRange(clean,
                            span.leftMaxX + 2, comma.minX - 1, span.minY - 1, span.maxY + 1);
                    DigitRecognition rightDigits = recognizeDigitRange(clean,
                            comma.maxX + 1, span.rightMinX - 2, span.minY - 1, span.maxY + 1);
                    if (!isPlausibleCoordinateSide(leftDigits) || !isPlausibleCoordinateSide(rightDigits)) {
                        continue;
                    }

                    double score = leftDigits.score() + rightDigits.score()
                            - Math.abs(width - 55) * 0.01
                            - Math.abs(comma.minX - (span.minX + width * 0.42)) * 0.005;
                    if (score > bestScore) {
                        best = span;
                        bestScore = score;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean isPlausibleCoordinateSide(DigitRecognition recognition) {
        int len = recognition.text().length();
        return len >= 1
                && len <= MAX_COORD_DIGITS
                && countUnknowns(recognition.text()) == 0
                && recognition.score() >= TEMPLATE_MATCH_THRESHOLD;
    }

    private Optional<GlyphBox> findCommaGlyph(BufferedImage clean, List<GlyphBox> glyphs, BracketSpan span, boolean debugOutput) {
        int expectedX = (int) Math.round(span.minX + (span.maxX - span.minX) * 0.48);
        // Candidate comma components are intentionally filtered by geometry first, then
        // validated by whether the surrounding digit ranges decode cleanly.
        List<GlyphBox> candidates = glyphs.stream()
                .filter(g -> g.minX > span.leftMaxX + 1 && g.maxX < span.rightMinX - 1)
                .filter(g -> g.minY >= span.minY + 6 && g.width() <= 4 && g.height() <= 5)
                .sorted(Comparator.comparingInt(g -> g.minX))
                .toList();
        GlyphBox best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        List<String> debugCandidates = debugOutput ? new ArrayList<>() : null;
        for (GlyphBox comma : candidates) {
            DigitRecognition leftDigits = recognizeDigitRange(clean,
                    span.leftMaxX + 2, comma.minX - 1, span.minY - 1, span.maxY + 1);
            DigitRecognition rightDigits = recognizeDigitRange(clean,
                    comma.maxX + 1, span.rightMinX - 2, span.minY - 1, span.maxY + 1);
            if (!isPlausibleCoordinateSide(leftDigits) || !isPlausibleCoordinateSide(rightDigits)) {
                if (debugCandidates != null) {
                    debugCandidates.add(String.format("%s -> rejected left='%s'/%.3f right='%s'/%.3f",
                            comma, leftDigits.text(), leftDigits.score(), rightDigits.text(), rightDigits.score()));
                }
                continue;
            }
            // Prefer the comma whose surrounding digits have the strongest recognition
            // score, with a tiny bias toward the expected visual comma position.
            double score = leftDigits.score() + rightDigits.score()
                    - Math.abs(comma.minX - expectedX) * 0.001;
            if (debugCandidates != null) {
                debugCandidates.add(String.format("%s -> left='%s'/%.3f right='%s'/%.3f score=%.3f",
                        comma, leftDigits.text(), leftDigits.score(), rightDigits.text(), rightDigits.score(), score));
            }
            if (score > bestScore) {
                best = comma;
                bestScore = score;
            }
        }
        if (debugCandidates != null && !debugCandidates.isEmpty()) {
            log.info("[坐标数字] 逗号候选: {}", debugCandidates);
        }
        return Optional.ofNullable(best);
    }

    private List<GlyphBox> projectionDigitBoxes(BufferedImage clean, int startX, int endX, int startY, int endY) {
        int left = Math.max(0, Math.min(startX, endX));
        int right = Math.min(clean.getWidth() - 1, Math.max(startX, endX));
        int top = Math.max(0, Math.min(startY, endY));
        int bottom = Math.min(clean.getHeight() - 1, Math.max(startY, endY));

        List<GlyphBox> runs = new ArrayList<>();
        int runStart = -1;
        for (int x = left; x <= right; x++) {
            boolean hasWhite = false;
            for (int y = top; y <= bottom; y++) {
                if (isWhite(clean, x, y)) {
                    hasWhite = true;
                    break;
                }
            }
            if (hasWhite && runStart < 0) {
                runStart = x;
            } else if (!hasWhite && runStart >= 0) {
                runs.add(trimBoxToForeground(clean, runStart, x - 1, top, bottom));
                runStart = -1;
            }
        }
        if (runStart >= 0) {
            runs.add(trimBoxToForeground(clean, runStart, right, top, bottom));
        }
        return runs.stream()
                .filter(box -> box.width() >= 1 && box.height() >= 5)
                .sorted(Comparator.comparingInt(box -> box.minX))
                .toList();
    }

    private String recognizeDigitBoxes(BufferedImage clean, List<GlyphBox> glyphs) {
        StringBuilder sb = new StringBuilder();
        for (GlyphBox glyph : glyphs) {
            BufferedImage glyphImage = trimToForeground(crop(clean, glyph), 1);
            String symbol = recognizeOneGlyph(glyphImage);
            sb.append(symbol == null ? "?" : symbol);
            glyphImage.flush();
        }
        return sb.toString();
    }

    private DigitRecognition recognizeDigitRange(BufferedImage clean, int startX, int endX, int startY, int endY) {
        // Projection splits the requested horizontal range into white-pixel runs. Some
        // rendered digits can be broken into multiple runs, so the next stage tries all
        // reasonable merge partitions instead of assuming one run equals one digit.
        List<GlyphBox> runs = projectionDigitBoxes(clean, startX, endX, startY, endY);
        if (runs.isEmpty()) {
            return new DigitRecognition("", List.of(), 0.0);
        }

        DigitRecognition best = null;
        int maxDigits = Math.min(MAX_COORD_DIGITS, runs.size());
        int splitSlots = Math.max(0, runs.size() - 1);
        int maxMask = 1 << splitSlots;
        for (int digitCount = 1; digitCount <= maxDigits; digitCount++) {
            // Each mask says where to split runs into digit boxes. The best candidate is
            // chosen by fewer unknowns first, then by average template score.
            for (int mask = 0; mask < maxMask; mask++) {
                if (Integer.bitCount(mask) != digitCount - 1) {
                    continue;
                }
                DigitRecognition candidate = recognizePartition(clean, runs, mask);
                if (isBetterDigitRecognition(candidate, best)) {
                    best = candidate;
                }
            }
        }
        return best == null ? new DigitRecognition("", List.of(), 0.0) : best;
    }

    private DigitRecognition recognizePartition(BufferedImage clean, List<GlyphBox> runs, int splitMask) {
        StringBuilder text = new StringBuilder();
        List<GlyphBox> boxes = new ArrayList<>();
        double totalScore = 0.0;
        int unknownCount = 0;

        // Merge consecutive projection runs according to splitMask, then score each
        // merged glyph against the saved digit templates.
        int start = 0;
        for (int i = 0; i < runs.size(); i++) {
            boolean splitAfter = i == runs.size() - 1 || ((splitMask & (1 << i)) != 0);
            if (!splitAfter) {
                continue;
            }

            GlyphBox merged = mergeRuns(runs, start, i);
            BufferedImage glyphImage = trimToForeground(crop(clean, merged), 1);
            GlyphMatch match = recognizeOneGlyphScored(glyphImage);
            glyphImage.flush();

            text.append(match.symbol());
            boxes.add(merged);
            totalScore += match.score();
            if ("?".equals(match.symbol())) {
                unknownCount++;
            }
            start = i + 1;
        }

        // Unknown glyphs are penalized heavily so a complete low-score coordinate beats
        // a visually strong but incomplete one.
        double averageScore = boxes.isEmpty() ? 0.0 : totalScore / boxes.size();
        double unknownPenalty = unknownCount * 0.5;
        return new DigitRecognition(text.toString(), boxes, averageScore - unknownPenalty);
    }

    private boolean isBetterDigitRecognition(DigitRecognition candidate, DigitRecognition best) {
        if (best == null) {
            return true;
        }
        int candidateUnknowns = countUnknowns(candidate.text());
        int bestUnknowns = countUnknowns(best.text());
        if (candidateUnknowns != bestUnknowns) {
            return candidateUnknowns < bestUnknowns;
        }
        return candidate.score() > best.score();
    }

    private int countUnknowns(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    private String recognizeOneGlyph(BufferedImage glyphImage) {
        GlyphMatch match = recognizeOneGlyphScored(glyphImage);
        return "?".equals(match.symbol()) ? null : match.symbol();
    }

    private GlyphMatch recognizeOneGlyphScored(BufferedImage glyphImage) {
        double bestScore = 0.0;
        String bestSymbol = null;

        // Compare the normalized glyph with each digit template. Templates are cleaned
        // again at read time so old or hand-generated template files still use the same
        // thresholding rules as live screenshots.
        for (String symbol : List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")) {
            File templateFile = new File(TEMPLATE_DIR, symbol + ".png");
            if (!templateFile.exists()) {
                continue;
            }

            BufferedImage template = readTemplate(templateFile);
            if (template == null) {
                continue;
            }
            BufferedImage cleanTemplate = trimToForeground(template, 1);
            double score = foregroundSimilarity(glyphImage, cleanTemplate);
            cleanTemplate.flush();
            template.flush();

            if (score > bestScore) {
                bestScore = score;
                bestSymbol = symbol;
            }
        }

        if (bestSymbol == null) {
            log.debug("[坐标数字] 数字模板缺失或无法命中，请检查 {}", TEMPLATE_DIR);
            return new GlyphMatch("?", bestScore);
        }
        if (bestScore < TEMPLATE_MATCH_THRESHOLD) {
            log.debug("[坐标数字] 最佳模板低于阈值：symbol={} score={}", bestSymbol, bestScore);
            return new GlyphMatch("?", bestScore);
        }
        return new GlyphMatch(bestSymbol, bestScore);
    }

    private Optional<MapCoordinate> parseCoordinate(String text, String mapLabelPath, boolean debugOutput) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.contains("?")) {
            if (debugOutput) {
                log.info("[坐标数字] 识别结果包含未知字符，丢弃 mapLabelPath={} raw='{}'", mapLabelPath, normalized);
            }
            return Optional.empty();
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d{1,3}),(\\d{1,3})")
                .matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new MapCoordinate(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))
        ));
    }

    private BufferedImage readTemplate(File templateFile) {
        try {
            BufferedImage image = ImageIO.read(templateFile);
            if (image == null) {
                return null;
            }
            return cleanCoordinateText(image);
        } catch (Exception e) {
            log.warn("[坐标数字] 读取模板失败：{}", templateFile.getPath(), e);
            return null;
        }
    }

    private GlyphBox trimBoxToForeground(BufferedImage clean, int startX, int endX, int startY, int endY) {
        int minX = clean.getWidth();
        int minY = clean.getHeight();
        int maxX = -1;
        int maxY = -1;
        int count = 0;
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                if (isWhite(clean, x, y)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    count++;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return new GlyphBox(startX, startY, endX, endY);
        }
        GlyphBox box = new GlyphBox(minX, minY, maxX, maxY);
        box.pixelCount = count;
        return box.expand(clean.getWidth(), clean.getHeight(), 1);
    }

    private GlyphBox mergeRuns(List<GlyphBox> runs, int startInclusive, int endInclusive) {
        GlyphBox merged = runs.get(startInclusive).copy();
        for (int i = startInclusive + 1; i <= endInclusive; i++) {
            merged.include(runs.get(i));
        }
        return merged;
    }

    private BufferedImage trimToForeground(BufferedImage source, int pad) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (isWhite(source, x, y)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return source;
        }
        GlyphBox box = new GlyphBox(
                Math.max(0, minX - pad),
                Math.max(0, minY - pad),
                Math.min(source.getWidth() - 1, maxX + pad),
                Math.min(source.getHeight() - 1, maxY + pad)
        );
        BufferedImage trimmed = crop(source, box);
        source.flush();
        return trimmed;
    }

    private double foregroundSimilarity(BufferedImage a, BufferedImage b) {
        // Count foreground pixels in both images first. The final score is Dice-style
        // overlap, so blank templates must fail instead of accidentally returning a
        // perfect match.
        int whiteA = 0;
        int whiteB = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (isWhite(a, x, y)) {
                    whiteA++;
                }
            }
        }
        for (int y = 0; y < b.getHeight(); y++) {
            for (int x = 0; x < b.getWidth(); x++) {
                if (isWhite(b, x, y)) {
                    whiteB++;
                }
            }
        }
        if (whiteA == 0 || whiteB == 0) {
            return 0.0;
        }

        double best = 0.0;
        // Allow a small +/-2px offset because mini-map glyph crops are often shifted by
        // antialiasing, bracket padding, or connected-component boundaries.
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int overlap = 0;
                for (int y = 0; y < a.getHeight(); y++) {
                    for (int x = 0; x < a.getWidth(); x++) {
                        int bx = x + dx;
                        int by = y + dy;
                        if (bx < 0 || by < 0 || bx >= b.getWidth() || by >= b.getHeight()) {
                            continue;
                        }
                        if (isWhite(a, x, y) && isWhite(b, bx, by)) {
                            overlap++;
                        }
                    }
                }
                best = Math.max(best, (2.0 * overlap) / (whiteA + whiteB));
            }
        }
        // Similar-looking text at very different sizes is suspicious; penalize size
        // differences so short map names do not win against longer labels.
        double sizePenalty = Math.abs(a.getWidth() - b.getWidth()) * 0.08
                + Math.abs(a.getHeight() - b.getHeight()) * 0.04;
        return Math.max(0.0, best - sizePenalty);
    }

    private BufferedImage crop(BufferedImage source, GlyphBox box) {
        BufferedImage out = new BufferedImage(box.width(), box.height(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(source, 0, 0, box.width(), box.height(),
                    box.minX, box.minY, box.maxX + 1, box.maxY + 1, null);
            return out;
        } finally {
            g.dispose();
        }
    }

    private BufferedImage cropColor(BufferedImage source, int minX, int minY, int maxX, int maxY) {
        int left = Math.max(0, minX);
        int top = Math.max(0, minY);
        int right = Math.min(source.getWidth() - 1, maxX);
        int bottom = Math.min(source.getHeight() - 1, maxY);
        if (right < left || bottom < top) {
            return null;
        }

        BufferedImage out = new BufferedImage(right - left + 1, bottom - top + 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(source, 0, 0, out.getWidth(), out.getHeight(),
                    left, top, right + 1, bottom + 1, null);
            return out;
        } finally {
            g.dispose();
        }
    }

    private String saveMapLabelImage(BufferedImage raw, BracketSpan span) {
        int right = Math.max(0, span.minX() - 2);
        BufferedImage label = cropColor(raw, 0, 0, right, raw.getHeight() - 1);
        if (label == null) {
            return null;
        }
        try {
            return saveDebugImage(label, "minimap_map_label.png");
        } finally {
            label.flush();
        }
    }

    private BufferedImage extractCleanMapLabelImage(BufferedImage raw) {
        // Reuse the coordinate bracket detector to decide where the map label ends. The
        // map name is everything meaningful to the left of the first coordinate bracket.
        BufferedImage clean = cleanCoordinateText(raw);
        try {
            List<GlyphBox> glyphs = segmentGlyphs(clean);
            Optional<BracketSpan> span = findBracketSpan(clean, glyphs);
            if (span.isEmpty()) {
                return null;
            }
            BufferedImage tightLabel = cropTightMapLabel(clean, glyphs, span.get());
            if (tightLabel != null) {
                return tightLabel;
            }
            int right = Math.max(0, span.get().minX() - 2);
            return cropColor(clean, 0, 0, right, clean.getHeight() - 1);
        } finally {
            clean.flush();
        }
    }

    private BufferedImage cropTightMapLabel(BufferedImage clean, List<GlyphBox> glyphs, BracketSpan span) {
        int rightLimit = Math.max(0, span.minX() - 2);
        // Keep only text-like components before the coordinate bracket and ignore tiny
        // edge noise near the left border of the mini-map strip.
        List<GlyphBox> labelGlyphs = glyphs.stream()
                .filter(g -> g.maxX < rightLimit)
                .filter(g -> g.minX > 6)
                .filter(g -> g.pixelCount >= 2)
                .toList();
        if (labelGlyphs.isEmpty()) {
            return null;
        }

        GlyphBox bounds = labelGlyphs.get(0).copy();
        for (int i = 1; i < labelGlyphs.size(); i++) {
            bounds.include(labelGlyphs.get(i));
        }
        GlyphBox padded = bounds.expand(clean.getWidth(), clean.getHeight(), 2);
        return cropColor(clean, padded.minX, padded.minY, padded.maxX, padded.maxY);
    }

    private String saveFailureDebugImages(BufferedImage raw, BufferedImage clean, String reason) {
        String safeReason = reason == null || reason.isBlank()
                ? "unknown"
                : reason.replaceAll("[^A-Za-z0-9_-]", "_");
        String prefix = "minimap_coord_fail_"
                + LocalDateTime.now().format(FAILURE_TIME_FORMAT)
                + "_" + safeReason;
        String rawPath = saveDebugImage(raw, prefix + "_raw.png");
        saveDebugImage(clean, prefix + "_clean.png");
        return rawPath;
    }

    private boolean isWhite(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0x00FFFFFF) == 0x00FFFFFF;
    }

    private Optional<MapLabelTemplateMatch> findBestMapLabelMatch(BufferedImage label) {
        if (!Files.isDirectory(MAP_LABEL_TEMPLATE_DIR)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.list(MAP_LABEL_TEMPLATE_DIR)) {
            // Score every saved map-name template against the current cleaned label and
            // return the strongest candidate; thresholding is left to callers because
            // survey/debug paths may want to log low-score best matches.
            return stream
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .map(path -> scoreMapLabel(label, path))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .max(Comparator.comparingDouble(MapLabelTemplateMatch::score));
        } catch (IOException e) {
            log.warn("[minimap-location] read map label templates failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<MapLabelTemplateMatch> scoreMapLabel(BufferedImage label, Path templatePath) {
        try {
            BufferedImage template = ImageIO.read(templatePath.toFile());
            if (template == null) {
                return Optional.empty();
            }
            try {
                String fileName = templatePath.getFileName().toString();
                String mapName = fileName.substring(0, fileName.length() - 4);
                return Optional.of(new MapLabelTemplateMatch(mapName, foregroundSimilarity(label, template)));
            } finally {
                template.flush();
            }
        } catch (IOException e) {
            log.warn("[minimap-location] read map label template failed: path={} reason={}",
                    templatePath, e.getMessage());
            return Optional.empty();
        }
    }

    private String saveDebugImage(BufferedImage image, String fileName) {
        try {
            File file = new File(windowScopedTempPath.resolve(fileName));
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            ImageIO.write(image, "png", file);
            return file.getPath();
        } catch (Exception e) {
            log.debug("[坐标数字] 保存调试图失败：{}", fileName, e);
            return null;
        }
    }

    private void saveGlyphDebugImages(BufferedImage clean, List<GlyphBox> glyphs) {
        for (int i = 0; i < glyphs.size(); i++) {
            BufferedImage glyph = crop(clean, glyphs.get(i));
            saveDebugImage(glyph, String.format("minimap_coord_glyph_%02d.png", i));
            glyph.flush();
        }
    }

    private static class GlyphBox {
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;
        private int pixelCount;

        private GlyphBox(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        private int width() {
            return maxX - minX + 1;
        }

        private int height() {
            return maxY - minY + 1;
        }

        private void include(int x, int y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            pixelCount++;
        }

        private void include(GlyphBox other) {
            minX = Math.min(minX, other.minX);
            minY = Math.min(minY, other.minY);
            maxX = Math.max(maxX, other.maxX);
            maxY = Math.max(maxY, other.maxY);
            pixelCount += other.pixelCount;
        }

        private GlyphBox copy() {
            GlyphBox copy = new GlyphBox(minX, minY, maxX, maxY);
            copy.pixelCount = pixelCount;
            return copy;
        }

        private GlyphBox expand(int imageWidth, int imageHeight, int pad) {
            GlyphBox expanded = new GlyphBox(
                    Math.max(0, minX - pad),
                    Math.max(0, minY - pad),
                    Math.min(imageWidth - 1, maxX + pad),
                    Math.min(imageHeight - 1, maxY + pad)
            );
            expanded.pixelCount = pixelCount;
            return expanded;
        }

        @Override
        public String toString() {
            return "GlyphBox[x=" + minX + ",y=" + minY + ",w=" + width() + ",h=" + height() + ",p=" + pixelCount + "]";
        }
    }

    private record BracketSpan(int minX, int leftMaxX, int rightMinX, int maxX, int minY, int maxY) {
    }

    private record CoordinateRecognition(String mapLabelPath, Optional<MapCoordinate> coordinate) {
    }

    private record DigitRecognition(String text, List<GlyphBox> boxes, double score) {
    }

    private record GlyphMatch(String symbol, double score) {
    }
}
