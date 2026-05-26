package com.bot.dhxy.vision;

import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.tools.ImagePreprocessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ObjectiveTextRecognitionService {

    private static final Path MAP_NAME_DIR = Path.of("images", "template", "objective", "map_names");
    private static final Path MAP_NAME_MANIFEST = MAP_NAME_DIR.resolve("manifest.tsv");
    private static final Path DIGIT_DIR = Path.of("images", "template", "objective", "green_digits");
    private static final Pattern COORDINATE_TEXT = Pattern.compile("\\d{1,3},\\d{1,3}");
    private static final double MAP_NAME_MATCH_THRESHOLD = 0.82;
    private static final double DIGIT_MATCH_THRESHOLD = 0.45;
    private static final double CONTAINED_DIGIT_THRESHOLD = 0.78;
    private static final double SLIDING_DIGIT_THRESHOLD = 0.82;

    private final TaskExecutionContextHolder taskExecutionContextHolder;

    private volatile TemplateBundle templateBundle;

    public ObjectiveTextRecognitionService(TaskExecutionContextHolder taskExecutionContextHolder) {
        this.taskExecutionContextHolder = taskExecutionContextHolder;
    }

    /**
     * Reads an objective-panel screenshot from disk and recognizes the current map/coordinate text.
     *
     * @param rawImagePath path to a screenshot file; the image is expected to contain objective green text,
     *                     usually captured from the currently bound game window.
     * @param source human-readable diagnostic label written to logs; may be any non-sensitive caller tag.
     * @return a recognized objective result, or {@link Optional#empty()} when the image cannot be read or the
     *         map/coordinate text cannot be matched. If the current task requests stop, the stop exception is
     *         propagated so the window runner can finish promptly instead of treating it as a recognition miss.
     */
    public Optional<ObjectiveTextResult> recognize(Path rawImagePath, String source) {
        try {
            checkpoint("read objective image");
            BufferedImage raw = ImageIO.read(rawImagePath.toFile());
            if (raw == null) {
                log.warn("[objective-recognition] unreadable image: source={} path={}", source, rawImagePath);
                return Optional.empty();
            }
            try {
                return recognize(raw, source);
            } finally {
                raw.flush();
            }
        } catch (IOException e) {
            log.warn("[objective-recognition] read image failed: source={} path={}", source, rawImagePath, e);
            return Optional.empty();
        }
    }

    /**
     * Recognizes the map name and coordinate from an in-memory objective-panel image.
     *
     * @param raw source image in normal Java {@link BufferedImage} coordinates; ownership remains with the caller
     *            and this method only creates/disposes derived cleaned/cropped images.
     * @param source human-readable diagnostic label used in logs to identify the screenshot/caller.
     * @return a recognized objective result, or {@link Optional#empty()} for normal OCR/template misses. Stop
     *         requests from the current task context are deliberately rethrown to preserve UI stop responsiveness.
     */
    public Optional<ObjectiveTextResult> recognize(BufferedImage raw, String source) {
        if (raw == null) {
            return Optional.empty();
        }

        checkpoint("start objective recognition");
        long startedAt = System.currentTimeMillis();
        TemplateBundle templates = loadTemplates();
        checkpoint("objective templates loaded");
        if (templates.mapNames().isEmpty() || templates.digits().isEmpty()) {
            log.warn("[objective-recognition] templates unavailable: source={} mapTemplates={} digitSymbols={}",
                    source, templates.mapNames().size(), templates.digits().size());
            return Optional.empty();
        }

        // Normalize green objective text into a binary image before running local template matching.
        // This path is pure detection and must remain cooperative with task stop requests.
        BufferedImage clean = ImagePreprocessor.washGreenTextToBlackAndWhite(raw);
        try {
            ForegroundCrop foreground = cropToForeground(clean, 4);
            BufferedImage searchImage = foreground.image();
            try {
                checkpoint("match objective map name");
                MapNameMatch mapMatch = findBestMapNameNearCoordinate(searchImage, templates.mapNames())
                        .filter(match -> match.score() >= MAP_NAME_MATCH_THRESHOLD)
                        .orElseGet(() -> findBestMapName(searchImage, templates.mapNames()));
                if (mapMatch == null || mapMatch.score() < MAP_NAME_MATCH_THRESHOLD) {
                    log.info("[objective-recognition] map not matched: source={} best={} searchSize={}x{} offset=({}, {}) elapsedMs={}",
                            source, mapMatch, searchImage.getWidth(), searchImage.getHeight(),
                            foreground.offsetX(), foreground.offsetY(), System.currentTimeMillis() - startedAt);
                    return Optional.empty();
                }

                log.info("[objective-recognition] map matched: source={} map={} score={} at=({}, {}) searchSize={}x{} offset=({}, {}) elapsedMs={}",
                        source, mapMatch.mapName(), mapMatch.score(), mapMatch.x(), mapMatch.y(),
                        searchImage.getWidth(), searchImage.getHeight(), foreground.offsetX(), foreground.offsetY(),
                        System.currentTimeMillis() - startedAt);

                BufferedImage coordinateArea = cropCoordinateSearchArea(searchImage, mapMatch);
                String coordinateText;
                try {
                    checkpoint("recognize objective coordinate");
                    coordinateText = recognizeCoordinate(coordinateArea, templates.digits());
                } finally {
                    if (coordinateArea != searchImage) {
                        coordinateArea.flush();
                    }
                }
                Matcher matcher = COORDINATE_TEXT.matcher(coordinateText);
                if (!matcher.matches()) {
                    log.info("[objective-recognition] coordinate not matched: source={} map={} rawCoord={} elapsedMs={}",
                            source, mapMatch.mapName(), coordinateText, System.currentTimeMillis() - startedAt);
                    return Optional.empty();
                }

                String[] parts = coordinateText.split(",");
                ObjectiveTextResult result = new ObjectiveTextResult(
                        mapMatch.slug(),
                        mapMatch.mapName(),
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        mapMatch.score(),
                        source
                );
                log.info("[objective-recognition] matched: {} elapsedMs={}", result, System.currentTimeMillis() - startedAt);
                return Optional.of(result);
            } finally {
                if (searchImage != clean) {
                    searchImage.flush();
                }
            }
        } catch (TaskStopRequestedException e) {
            log.info("[objective-recognition] stopped: source={} elapsedMs={} reason={}",
                    source, System.currentTimeMillis() - startedAt, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("[objective-recognition] recognize failed: source={}", source, e);
            return Optional.empty();
        } finally {
            clean.flush();
        }
    }

    private TemplateBundle loadTemplates() {
        TemplateBundle cached = templateBundle;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (templateBundle == null) {
                templateBundle = doLoadTemplates();
            }
            return templateBundle;
        }
    }

    private TemplateBundle doLoadTemplates() {
        Map<String, MapNameTemplate> mapNames = new LinkedHashMap<>();
        Map<String, List<BufferedImage>> digits = new HashMap<>();

        loadMapNameTemplates(mapNames);
        loadDigitTemplates(digits);

        log.info("[objective-recognition] templates loaded: mapTemplates={} digitSymbols={}",
                mapNames.size(), digits.size());
        return new TemplateBundle(mapNames, digits);
    }

    private void loadMapNameTemplates(Map<String, MapNameTemplate> out) {
        if (!Files.exists(MAP_NAME_MANIFEST)) {
            log.warn("[objective-recognition] map manifest missing: {}", MAP_NAME_MANIFEST);
            return;
        }
        try {
            for (String line : Files.readAllLines(MAP_NAME_MANIFEST, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\t");
                if (parts.length < 3) {
                    log.warn("[objective-recognition] bad manifest line: {}", line);
                    continue;
                }
                String slug = parts[0].trim();
                String mapName = parts[1].trim();
                Path templatePath = MAP_NAME_DIR.resolve(parts[2].trim());
                BufferedImage template = ImageIO.read(templatePath.toFile());
                if (template == null) {
                    log.warn("[objective-recognition] unreadable map template: {}", templatePath);
                    continue;
                }
                out.put(slug, new MapNameTemplate(slug, mapName, templatePath, template));
            }
        } catch (IOException e) {
            log.warn("[objective-recognition] load map templates failed", e);
        }
    }

    private void loadDigitTemplates(Map<String, List<BufferedImage>> out) {
        if (!Files.exists(DIGIT_DIR)) {
            log.warn("[objective-recognition] digit template dir missing: {}", DIGIT_DIR);
            return;
        }
        try (var stream = Files.list(DIGIT_DIR)) {
            for (Path path : stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".png"))
                    .sorted()
                    .toList()) {
                String fileName = path.getFileName().toString();
                int underscore = fileName.indexOf('_');
                if (underscore <= 0) {
                    continue;
                }
                String symbol = fileName.substring(0, underscore);
                BufferedImage template = ImageIO.read(path.toFile());
                if (template == null) {
                    log.warn("[objective-recognition] unreadable digit template: {}", path);
                    continue;
                }
                out.computeIfAbsent(symbol, ignored -> new ArrayList<>()).add(template);
            }
        } catch (IOException e) {
            log.warn("[objective-recognition] load digit templates failed", e);
        }
    }

    private MapNameMatch findBestMapName(BufferedImage clean, Map<String, MapNameTemplate> mapNames) {
        MapNameMatch best = null;
        // Full-image scan is the broad fallback: every map-name template is slid across
        // the cleaned objective text image and the strongest binary match wins.
        for (MapNameTemplate template : mapNames.values()) {
            checkpoint("scan objective map template");
            TemplateMatch match = bestBinaryTemplateMatch(clean, template.image());
            MapNameMatch candidate = new MapNameMatch(
                        template.slug(),
                        template.mapName(),
                        template.path(),
                        match.score(),
                        match.x(),
                        match.y(),
                        template.image().getWidth(),
                        template.image().getHeight()
            );
            if (isBetterMapMatch(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    private Optional<MapNameMatch> findBestMapNameNearCoordinate(BufferedImage clean,
                                                                 Map<String, MapNameTemplate> mapNames) {
        // Most objective lines render the map name immediately before the coordinate.
        // Detecting coordinate glyph runs first lets us restrict matching to that area.
        List<GlyphBox> coordinateRuns = findCoordinateRunsByProjection(clean);
        if (coordinateRuns.isEmpty()) {
            return Optional.empty();
        }

        GlyphBox coordinateStart = coordinateRuns.get(0);
        int maxTemplateWidth = mapNames.values().stream()
                .mapToInt(template -> template.image().getWidth())
                .max()
                .orElse(80);
        int right = Math.max(0, coordinateStart.minX() - 1);
        int left = Math.max(0, right - maxTemplateWidth - 18);
        int top = Math.max(0, coordinateStart.minY() - 8);
        int bottom = Math.min(clean.getHeight() - 1, coordinateStart.maxY() + 8);
        if (right <= left || bottom <= top) {
            return Optional.empty();
        }

        BufferedImage mapArea = crop(clean, new GlyphBox(left, top, right, bottom, 0));
        try {
            MapNameMatch best = null;
            // Match inside the restricted map-name area, then convert local match
            // coordinates back into the original cleaned image coordinate space.
            for (MapNameTemplate template : mapNames.values()) {
                checkpoint("scan near-coordinate map template");
                TemplateMatch match = bestBinaryTemplateMatch(mapArea, template.image());
                MapNameMatch candidate = new MapNameMatch(
                            template.slug(),
                            template.mapName(),
                            template.path(),
                            match.score(),
                            left + match.x(),
                            top + match.y(),
                            template.image().getWidth(),
                            template.image().getHeight()
                );
                if (isBetterMapMatch(candidate, best)) {
                    best = candidate;
                }
            }
            if (best != null) {
                log.info("[objective-recognition] map near-coordinate scan: best={} area={}x{} rect=({}, {})-({}, {})",
                        best, mapArea.getWidth(), mapArea.getHeight(), left, top, right, bottom);
            }
            return Optional.ofNullable(best);
        } finally {
            mapArea.flush();
        }
    }

    private boolean isBetterMapMatch(MapNameMatch candidate, MapNameMatch current) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        double delta = candidate.score() - current.score();
        if (Math.abs(delta) > 0.000001) {
            return delta > 0;
        }
        // 短地图名可能是长地图名的前缀，例如“长安”会在“长安城东”里拿到满分。
        // 同分时优先选择更长的模板，避免被短模板抢先命中。
        int widthDelta = candidate.width() - current.width();
        if (widthDelta != 0) {
            return widthDelta > 0;
        }
        return candidate.height() > current.height();
    }

    private BufferedImage cropCoordinateSearchArea(BufferedImage clean, MapNameMatch mapMatch) {
        // Coordinates are expected to appear to the right of the matched map name. The
        // vertical padding keeps bracket/comma pixels that may sit slightly above/below it.
        int left = Math.min(clean.getWidth() - 1, Math.max(0, mapMatch.x() + mapMatch.width() - 2));
        int right = clean.getWidth() - 1;
        int top = Math.max(0, mapMatch.y() - 8);
        int bottom = Math.min(clean.getHeight() - 1, mapMatch.y() + mapMatch.height() + 10);
        if (right <= left || bottom <= top) {
            return clean;
        }
        return crop(clean, new GlyphBox(left, top, right, bottom, 0));
    }

    private ForegroundCrop cropToForeground(BufferedImage source, int pad) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            checkpoint("crop objective foreground");
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
            return new ForegroundCrop(source, 0, 0);
        }
        int left = Math.max(0, minX - pad);
        int top = Math.max(0, minY - pad);
        int right = Math.min(source.getWidth() - 1, maxX + pad);
        int bottom = Math.min(source.getHeight() - 1, maxY + pad);
        if (left == 0 && top == 0 && right == source.getWidth() - 1 && bottom == source.getHeight() - 1) {
            return new ForegroundCrop(source, 0, 0);
        }
        return new ForegroundCrop(crop(source, new GlyphBox(left, top, right, bottom, 0)), left, top);
    }

    private String recognizeCoordinate(BufferedImage clean, Map<String, List<BufferedImage>> digits) {
        // Fast path: segment foreground runs and try to read them as bracket/comma/digit
        // components. If segmentation is incomplete, fall back to a slower sliding scan.
        List<GlyphBox> coordinateRuns = findCoordinateRunsByProjection(clean);
        if (coordinateRuns.size() < 3) {
            return recognizeCoordinateByTemplateScan(clean, digits);
        }

        // First ignore bracket-like decorations. This usually produces clean text such as
        // "123,45" and avoids confusing right brackets with digit "1".
        String stripped = recognizeCoordinateRuns(clean, stripCoordinateDecorations(coordinateRuns), digits);
        if (COORDINATE_TEXT.matcher(stripped).matches() && !stripped.matches("\\d{3},\\d")) {
            return stripped;
        }
        Optional<String> repaired = repairDuplicatedPrefixAndMergedSuffix(stripped);
        if (repaired.isPresent()) {
            return repaired.get();
        }

        // Some screenshots segment brackets and digits together in useful ways. Try the
        // raw runs before paying the cost of full sliding-template matching.
        String direct = recognizeCoordinateRuns(clean, coordinateRuns, digits);
        if (COORDINATE_TEXT.matcher(direct).matches()) {
            return direct;
        }
        return recognizeCoordinateByTemplateScan(clean, digits);
    }

    private String recognizeCoordinateRuns(BufferedImage clean, List<GlyphBox> runs,
                                           Map<String, List<BufferedImage>> digits) {
        if (runs.size() < 3) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        // Walk left-to-right through projected glyph runs, treating bracket/comma-like
        // shapes as syntax and all other runs as digit candidates.
        for (int i = 0; i < runs.size(); i++) {
            GlyphBox run = runs.get(i);
            boolean last = i == runs.size() - 1;
            if (out.isEmpty() && isPureBracketRun(run)) {
                continue;
            }
            if (last && out.indexOf(",") >= 0 && isPureBracketRun(run)) {
                continue;
            }
            if (isCommaRun(run)) {
                out.append(',');
                continue;
            }
            BufferedImage glyph = trimToForeground(crop(clean, run), 1);
            String symbol;
            try {
                symbol = recognizeOneGlyph(glyph, digits);
                if (symbol == null) {
                    symbol = recognizeContainedDigit(glyph, digits);
                }
            } finally {
                glyph.flush();
            }
            if (symbol == null) {
                // A single unreadable run invalidates the segmented read. The caller then
                // uses the sliding-template fallback, which is slower but more tolerant.
                return recognizeCoordinateByTemplateScan(clean, digits);
            }
            out.append(symbol);
        }
        return out.toString();
    }

    private Optional<String> repairDuplicatedPrefixAndMergedSuffix(String text) {
        // Common segmentation failure: the first digit is duplicated and the final digit
        // is merged away, e.g. "112,3" should be interpreted as "12,33".
        Matcher matcher = Pattern.compile("^(\\d)\\1(\\d),(\\d)$").matcher(text);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1) + matcher.group(2) + "," + matcher.group(3) + matcher.group(3));
    }

    private String recognizeCoordinateByTemplateScan(BufferedImage clean, Map<String, List<BufferedImage>> digits) {
        List<SymbolMatch> matches = new ArrayList<>();
        // Slow fallback: slide every digit/comma template over the coordinate area. This
        // handles broken connected components but produces overlapping candidates.
        for (Map.Entry<String, List<BufferedImage>> entry : digits.entrySet()) {
            checkpoint("scan coordinate template symbol");
            String symbol = entry.getKey();
            for (BufferedImage template : entry.getValue()) {
                if (template.getWidth() > clean.getWidth() || template.getHeight() > clean.getHeight()) {
                    continue;
                }
                for (int y = 0; y <= clean.getHeight() - template.getHeight(); y++) {
                    checkpoint("scan coordinate template row");
                    for (int x = 0; x <= clean.getWidth() - template.getWidth(); x++) {
                        double score = binaryTemplateScoreAt(clean, template, x, y);
                        if (score >= SLIDING_DIGIT_THRESHOLD) {
                            matches.add(new SymbolMatch(
                                    "comma".equals(symbol) ? "," : symbol,
                                    x + template.getWidth() / 2.0,
                                    y + template.getHeight() / 2.0,
                                    score
                            ));
                        }
                    }
                }
            }
        }
        if (matches.isEmpty()) {
            return "";
        }

        // Keep only the strongest candidate per tiny neighborhood so the same glyph is
        // not emitted multiple times from adjacent high-scoring template positions.
        matches.sort(Comparator.comparingDouble((SymbolMatch match) -> match.score).reversed());
        List<SymbolMatch> accepted = new ArrayList<>();
        for (SymbolMatch candidate : matches) {
            boolean duplicate = false;
            for (SymbolMatch existing : accepted) {
                if (Math.abs(existing.centerX() - candidate.centerX()) <= 3
                        && Math.abs(existing.centerY() - candidate.centerY()) <= 3) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                accepted.add(candidate);
            }
        }
        accepted.sort(Comparator.comparingDouble(SymbolMatch::centerX));

        StringBuilder text = new StringBuilder();
        for (SymbolMatch match : accepted) {
            text.append(match.symbol());
        }
        // If noise creates extra symbols, use the last valid coordinate-looking substring.
        // Objective coordinates normally appear at the end of the cropped coordinate area.
        Matcher matcher = COORDINATE_TEXT.matcher(text);
        String best = "";
        while (matcher.find()) {
            best = matcher.group();
        }
        return best;
    }

    private List<GlyphBox> findCoordinateRunsByProjection(BufferedImage clean) {
        // Projection converts vertical columns of foreground pixels into rough glyph runs.
        // It is cheap and good enough to locate the comma and the likely coordinate start.
        List<GlyphBox> runs = projectionRuns(clean, 0, clean.getWidth() - 1, 0, clean.getHeight() - 1);
        runs = runs.stream()
                .filter(run -> run.width() >= 1 && run.height() >= 3 && run.pixelCount() >= 2)
                .sorted(Comparator.comparingInt(GlyphBox::minX))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        int commaIndex = -1;
        for (int i = 0; i < runs.size(); i++) {
            if (isCommaRun(runs.get(i))) {
                commaIndex = i;
                break;
            }
        }
        if (commaIndex < 0) {
            return List.of();
        }

        // Search left from the comma for a bracket-like coordinate start. If no clear
        // start is found, keep up to three preceding runs as a conservative fallback.
        int start = -1;
        for (int i = commaIndex - 1; i >= 0; i--) {
            if (isCoordinateStartRun(runs.get(i))) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            start = Math.max(0, commaIndex - 3);
        }

        return new ArrayList<>(runs.subList(start, runs.size()));
    }

    private List<GlyphBox> stripCoordinateDecorations(List<GlyphBox> coordinateRuns) {
        if (coordinateRuns.isEmpty()) {
            return List.of();
        }
        List<GlyphBox> out = new ArrayList<>();
        boolean skippedLeadingBracket = false;
        // Remove only the outer bracket-like runs. Internal comma/digit runs are preserved
        // because the digit reader needs their original left-to-right order.
        for (int i = 0; i < coordinateRuns.size(); i++) {
            GlyphBox run = coordinateRuns.get(i);
            boolean last = i == coordinateRuns.size() - 1;
            if (!skippedLeadingBracket && out.isEmpty() && isBracketLikeRun(run)) {
                skippedLeadingBracket = true;
                continue;
            }
            if (last && !out.isEmpty() && out.stream().anyMatch(this::isCommaRun)
                    && isBracketLikeRun(run)) {
                continue;
            }
            out.add(run);
        }
        return out;
    }

    private List<GlyphBox> projectionRuns(BufferedImage clean, int left, int right, int top, int bottom) {
        List<GlyphBox> runs = new ArrayList<>();
        int runStart = -1;
        for (int x = left; x <= right; x++) {
            checkpoint("project objective glyph columns");
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
        return runs;
    }

    private String recognizeOneGlyph(BufferedImage glyph, Map<String, List<BufferedImage>> digits) {
        double bestScore = 0.0;
        String bestSymbol = null;
        // A segmented glyph should roughly match one whole digit. Use the foreground
        // similarity scorer that tolerates a small crop offset between live glyph/template.
        for (String symbol : List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")) {
            for (BufferedImage template : digits.getOrDefault(symbol, List.of())) {
                double score = foregroundSimilarity(glyph, template);
                if (score > bestScore) {
                    bestScore = score;
                    bestSymbol = symbol;
                }
            }
        }
        if (bestScore < DIGIT_MATCH_THRESHOLD) {
            return null;
        }
        return bestSymbol;
    }

    private String recognizeContainedDigit(BufferedImage glyph, Map<String, List<BufferedImage>> digits) {
        double bestScore = 0.0;
        String bestSymbol = null;
        // Some projection runs include bracket pixels plus one digit. Sliding templates
        // inside the run can still recover the contained digit.
        for (String symbol : List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")) {
            for (BufferedImage template : digits.getOrDefault(symbol, List.of())) {
                if (template.getWidth() > glyph.getWidth() || template.getHeight() > glyph.getHeight()) {
                    continue;
                }
                for (int y = 0; y <= glyph.getHeight() - template.getHeight(); y++) {
                    checkpoint("scan contained coordinate digit");
                    for (int x = 0; x <= glyph.getWidth() - template.getWidth(); x++) {
                        double score = binaryTemplateScoreAt(glyph, template, x, y);
                        if (score > bestScore) {
                            bestScore = score;
                            bestSymbol = symbol;
                        }
                    }
                }
            }
        }
        return bestScore >= CONTAINED_DIGIT_THRESHOLD ? bestSymbol : null;
    }

    private GlyphBox trimBoxToForeground(BufferedImage clean, int startX, int endX, int startY, int endY) {
        int minX = clean.getWidth();
        int minY = clean.getHeight();
        int maxX = -1;
        int maxY = -1;
        int count = 0;
        for (int y = startY; y <= endY; y++) {
            checkpoint("trim objective glyph box");
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
            return new GlyphBox(startX, startY, endX, endY, 0);
        }
        return new GlyphBox(minX, minY, maxX, maxY, count).expand(clean.getWidth(), clean.getHeight(), 1);
    }

    private BufferedImage crop(BufferedImage source, GlyphBox box) {
        int minX = Math.max(0, box.minX());
        int minY = Math.max(0, box.minY());
        int maxX = Math.min(source.getWidth() - 1, box.maxX());
        int maxY = Math.min(source.getHeight() - 1, box.maxY());
        BufferedImage out = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(source, 0, 0, out.getWidth(), out.getHeight(), minX, minY, maxX + 1, maxY + 1, null);
            return out;
        } finally {
            g.dispose();
        }
    }

    private BufferedImage trimToForeground(BufferedImage source, int pad) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            checkpoint("trim objective foreground glyph");
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
        BufferedImage out = crop(source, new GlyphBox(
                Math.max(0, minX - pad),
                Math.max(0, minY - pad),
                Math.min(source.getWidth() - 1, maxX + pad),
                Math.min(source.getHeight() - 1, maxY + pad),
                0
        ));
        source.flush();
        return out;
    }

    private boolean isWhite(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0x00FFFFFF) == 0x00FFFFFF;
    }

    private boolean isCommaRun(GlyphBox box) {
        return box.width() <= 5 && box.height() <= 6 && box.minY() >= 8 && box.pixelCount() <= 12;
    }

    private boolean isBracketLikeRun(GlyphBox box) {
        return box.width() <= 6 && box.height() >= 8 && box.height() <= 18 && box.pixelCount() <= 22;
    }

    private boolean isCoordinateStartRun(GlyphBox box) {
        return box.width() <= 6 && box.height() >= 8 && box.height() <= 18 && box.pixelCount() <= 22;
    }

    private boolean isPureBracketRun(GlyphBox box) {
        return box.width() <= 4 && box.height() >= 8 && box.height() <= 18 && box.pixelCount() <= 12;
    }

    private TemplateMatch bestBinaryTemplateMatch(BufferedImage source, BufferedImage template) {
        if (source == null || template == null
                || template.getWidth() > source.getWidth()
                || template.getHeight() > source.getHeight()) {
            return new TemplateMatch(0.0, 0, 0);
        }
        double best = 0.0;
        int bestX = 0;
        int bestY = 0;
        // Exhaustively slide the binary template over the binary source. These objective
        // crops are small, so this deterministic scan is easier to debug than OpenCV here.
        for (int y = 0; y <= source.getHeight() - template.getHeight(); y++) {
            checkpoint("scan binary objective template row");
            for (int x = 0; x <= source.getWidth() - template.getWidth(); x++) {
                double score = binaryTemplateScoreAt(source, template, x, y);
                if (score > best) {
                    best = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return new TemplateMatch(best, bestX, bestY);
    }

    private double binaryTemplateScoreAt(BufferedImage source, BufferedImage template, int originX, int originY) {
        int templateWhite = 0;
        int sourceWhite = 0;
        int overlap = 0;
        // Dice-style foreground overlap. Both template and source foreground counts are
        // included so dense noise and sparse partial matches are both penalized.
        for (int y = 0; y < template.getHeight(); y++) {
            for (int x = 0; x < template.getWidth(); x++) {
                boolean tw = isWhite(template, x, y);
                boolean sw = isWhite(source, originX + x, originY + y);
                if (tw) {
                    templateWhite++;
                }
                if (sw) {
                    sourceWhite++;
                }
                if (tw && sw) {
                    overlap++;
                }
            }
        }
        if (templateWhite == 0 || sourceWhite == 0) {
            return 0.0;
        }
        return (2.0 * overlap) / (templateWhite + sourceWhite);
    }

    private double foregroundSimilarity(BufferedImage a, BufferedImage b) {
        int whiteA = countWhite(a);
        int whiteB = countWhite(b);
        if (whiteA == 0 || whiteB == 0) {
            return 0.0;
        }
        double best = 0.0;
        // Try small pixel offsets because foreground crops from live screenshots are not
        // always aligned exactly like the saved templates.
        for (int dy = -2; dy <= 2; dy++) {
            checkpoint("compare objective glyph offset");
            for (int dx = -2; dx <= 2; dx++) {
                int overlap = 0;
                for (int y = 0; y < a.getHeight(); y++) {
                    checkpoint("compare objective glyph row");
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
        // Penalize very different bounding-box sizes so a short glyph/template cannot
        // win just because its few white pixels overlap perfectly.
        double sizePenalty = Math.abs(a.getWidth() - b.getWidth()) * 0.08
                + Math.abs(a.getHeight() - b.getHeight()) * 0.04;
        return Math.max(0.0, best - sizePenalty);
    }

    private int countWhite(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            checkpoint("count objective foreground pixels");
            for (int x = 0; x < image.getWidth(); x++) {
                if (isWhite(image, x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Throws when the current window task has requested stop or the worker thread has been interrupted.
     *
     * @param stage short diagnostic label for the current recognition stage; used only to keep future logs and
     *              stack traces understandable when a long local template scan is interrupted.
     */
    private void checkpoint(String stage) {
        taskExecutionContextHolder.checkpointIfPresent();
        if (Thread.currentThread().isInterrupted()) {
            throw new TaskStopRequestedException("objective recognition interrupted at " + stage);
        }
    }

    public record ObjectiveTextResult(String mapSlug, String mapName, int x, int y,
                                      double mapScore, String source) {
    }

    private record TemplateBundle(Map<String, MapNameTemplate> mapNames,
                                  Map<String, List<BufferedImage>> digits) {
    }

    private record MapNameTemplate(String slug, String mapName, Path path, BufferedImage image) {
    }

    private record MapNameMatch(String slug, String mapName, Path path, double score,
                                int x, int y, int width, int height) {
    }

    private record SymbolMatch(String symbol, double centerX, double centerY, double score) {
    }

    private record TemplateMatch(double score, int x, int y) {
    }

    private record ForegroundCrop(BufferedImage image, int offsetX, int offsetY) {
    }

    private record GlyphBox(int minX, int minY, int maxX, int maxY, int pixelCount) {
        private int width() {
            return maxX - minX + 1;
        }

        private int height() {
            return maxY - minY + 1;
        }

        private GlyphBox expand(int imageWidth, int imageHeight, int pad) {
            return new GlyphBox(
                    Math.max(0, minX - pad),
                    Math.max(0, minY - pad),
                    Math.min(imageWidth - 1, maxX + pad),
                    Math.min(imageHeight - 1, maxY + pad),
                    pixelCount
            );
        }
    }
}
