package com.bot.dhxy.vision;


import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.ocr.OcrLineResult;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.ocr.TargetOcrResult;
import com.bot.dhxy.model.ocr.TextCandidate;
import com.bot.dhxy.model.ocr.TextCandidateScanResult;
import com.bot.dhxy.core.TextRecognizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts colored in-game name text into OCR-friendly line images.
 *
 * <p>The service is intentionally local/OCR-sidecar oriented: it receives an already captured
 * image, filters pixels by game text color, groups connected components into text lines, writes a
 * compact black/white debug image, and maps OCR word boxes back to the original image coordinate
 * space. It does not capture windows or send any physical input.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameTextLineOcrService {

    private static final int OCR_SCALE = 4;
    private static final int LINE_CROP_MARGIN = 8;
    private static final int LINE_PACK_GAP = 18;
    private static final int LINE_MERGE_Y_TOLERANCE = 8;
    private static final int COMPONENT_MIN_PIXELS = 3;
    private static final int COMPONENT_MIN_WIDTH = 1;
    private static final int COMPONENT_MIN_HEIGHT = 2;
    private static final int COMPONENT_MAX_WIDTH = 120;
    private static final int COMPONENT_MAX_HEIGHT = 48;
    private static final int COMPONENT_MAX_PIXELS = 1200;
    private static final int WORD_SUMMARY_LIMIT = 12;
    private static final int DEFAULT_TEXT_CANDIDATE_LIMIT = 3;

    private final TextRecognizer textRecognizer;

    /**
     * Extract purple player-name style text from an image.
     *
     * @param raw source image in image-local pixels; ownership stays with the caller.
     * @param outputPath file path where the packed black/white OCR image should be written.
     * @return OCR words mapped back to the source image coordinate space; empty result if no line is kept.
     * @throws Exception if the debug image cannot be written or local OCR throws.
     */
    public OcrLineResult scanPurpleLines(BufferedImage raw, Path outputPath) throws Exception {
        return scanPackedLines(raw, outputPath, TextColorMode.PURPLE, false, "purple-line");
    }

    /**
     * Extract loose yellow NPC-name style text from an image.
     *
     * @param raw source image in image-local pixels; ownership stays with the caller.
     * @param outputPath file path where the packed black/white OCR image should be written.
     * @return OCR words mapped back to the source image coordinate space.
     * @throws Exception if image writing or local OCR fails.
     */
    public OcrLineResult scanYellowLines(BufferedImage raw, Path outputPath) throws Exception {
        return scanPackedLines(raw, outputPath, TextColorMode.YELLOW_LOOSE, false, "yellow-line");
    }

    /**
     * Find ranked NPC-name-like yellow text candidates directly from a raw game screenshot.
     *
     * <p>This is the formal candidate API for yellow NPC/monster names when exact OCR either has
     * not run yet or did not match the requested target. It builds the same loose yellow mask used
     * by the OCR line pipeline, expands nearby yellow shadow pixels, optionally writes a
     * black-on-white washed debug image, then runs the shape-only candidate detector. Coordinates
     * remain image-local to the supplied screenshot; callers that captured a cropped window region
     * must add that crop's origin before producing screen-absolute Ctrl-probe points.</p>
     *
     * @param raw source screenshot in image-local pixels; ownership stays with caller.
     * @param washedPath optional black-on-white yellow text mask output path.
     * @param overlayPath optional candidate overlay output path.
     * @return result object that owns an immutable score-sorted candidate list. Empty means the
     *         screenshot did not contain any stable yellow text-like candidate after filtering.
     * @throws Exception when debug image writing fails.
     */
    public TextCandidateScanResult findYellowTextCandidateResult(BufferedImage raw,
                                                                 Path washedPath,
                                                                 Path overlayPath) throws Exception {
        if (raw == null) {
            return TextCandidateScanResult.empty("raw image is null");
        }
        boolean[][] mask = buildFilteredMask(raw, TextColorMode.YELLOW_LOOSE);
        mask = includeNearbyYellowShadow(raw, mask, 2);
        BufferedImage maskImage = toTextMaskImage(mask);
        if (washedPath != null) {
            writeTextMaskImage(maskImage, washedPath);
        }
        List<TextCandidate> candidates = findTextLikeCandidates(mask, maskImage, raw.getWidth(), raw.getHeight());
        if (overlayPath != null) {
            try {
                writeCandidateOverlay(maskImage, candidates, overlayPath);
            } finally {
                maskImage.flush();
            }
        } else {
            maskImage.flush();
        }
        return TextCandidateScanResult.of(candidates, overlayPath == null ? null : overlayPath.toString());
    }

    /**
     * Convenience view of {@link #findYellowTextCandidateResult(BufferedImage, Path, Path)}.
     *
     * @param raw source screenshot in image-local pixels; ownership stays with caller.
     * @param washedPath optional black-on-white yellow text mask output path.
     * @param overlayPath optional candidate overlay output path.
     * @return immutable candidates sorted by descending score in image-local coordinates.
     * @throws Exception when debug image writing fails.
     */
    public List<TextCandidate> findYellowTextCandidates(BufferedImage raw,
                                                        Path washedPath,
                                                        Path overlayPath) throws Exception {
        return findYellowTextCandidateResult(raw, washedPath, overlayPath).candidates();
    }

    /**
     * Find text-like candidate regions from an already washed black/white text image.
     *
     * <p>This method does not OCR or require a target name. It is a shape detector for noisy yellow
     * NPC-name masks: black pixels are grouped into connected components, components are merged into
     * horizontal text-line candidates, rectangular UI frames are penalized, and the highest scoring
     * regions are returned. Coordinates are image-local; callers that crop a window region must add
     * the crop's window-relative origin before clicking or recording memory.</p>
     *
     * @param washed black/white or mostly black/white source image; ownership stays with caller.
     * @param overlayPath optional debug PNG path. When non-null, the method writes the source image
     * with candidate rectangles and score labels drawn on top.
     * @return result object that owns immutable candidates sorted by descending score. Empty means
     * the washed image did not contain a stable text-like horizontal region.
     * @throws Exception when the optional overlay cannot be written.
     */
    public TextCandidateScanResult findTextLikeCandidateResultFromWashedImage(BufferedImage washed,
                                                                              Path overlayPath) throws Exception {
        if (washed == null) {
            return TextCandidateScanResult.empty("washed image is null");
        }
        boolean[][] mask = buildBlackPixelMask(washed);
        List<TextCandidate> candidates = findTextLikeCandidates(mask, washed, washed.getWidth(), washed.getHeight());
        if (overlayPath != null) {
            writeCandidateOverlay(washed, candidates, overlayPath);
        }
        return TextCandidateScanResult.of(candidates, overlayPath == null ? null : overlayPath.toString());
    }

    /**
     * Convenience view for callers that only need the sorted immutable candidate list.
     *
     * @param washed black/white or mostly black/white source image; ownership stays with caller.
     * @param overlayPath optional debug PNG path.
     * @return immutable candidates sorted by descending score.
     * @throws Exception when the optional overlay cannot be written.
     */
    public List<TextCandidate> findTextLikeCandidatesFromWashedImage(BufferedImage washed,
                                                                     Path overlayPath) throws Exception {
        return findTextLikeCandidateResultFromWashedImage(washed, overlayPath).candidates();
    }

    private List<TextCandidate> findTextLikeCandidates(boolean[][] mask,
                                                       BufferedImage sourceForContext,
                                                       int imageWidth,
                                                       int imageHeight) {
        List<TextLineBox> lines = groupTextLines(mask);
        List<TextCandidate> candidates = new ArrayList<>();
        for (TextLineBox line : lines) {
            for (TextLineBox segment : splitLineByHorizontalGaps(mask, line)) {
                TextCandidate candidate = scoreWashedTextLine(mask, sourceForContext, segment, imageWidth, imageHeight);
                if (candidate.score() >= 25) {
                    candidates.add(candidate);
                }
            }
        }
        candidates.sort(Comparator.comparingInt(TextCandidate::score).reversed()
                .thenComparing(candidate -> candidate.region().y1())
                .thenComparing(candidate -> candidate.region().x1()));
        int keepCount = Math.min(DEFAULT_TEXT_CANDIDATE_LIMIT, candidates.size());
        return List.copyOf(candidates.subList(0, keepCount));
    }

    /**
     * Find the best yellow-text line matching an expected NPC target name.
     *
     * <p>The method tries both a strict yellow mask and a yellow+shadow mask, scores each detected
     * line with normalized edit distance/common substring, writes the selected debug image to
     * {@code outputPath}, and deletes non-selected candidate images.</p>
     *
     * @param raw source image in image-local pixels.
     * @param expectedTarget expected NPC name or fragment; blank means "scan only" and never matches.
     * @param outputPath selected packed-line debug image path.
     * @return match result with OCR words in source-image coordinates and fuzzy-match diagnostics.
     * @throws Exception if candidate images cannot be written or local OCR fails.
     */
    public TargetOcrResult findYellowTarget(BufferedImage raw, String expectedTarget, Path outputPath) throws Exception {
        long startedAtNanos = System.nanoTime();
        String normalizedTarget = OcrTextMatcher.normalizeName(expectedTarget);
        if (normalizedTarget.isBlank()) {
            OcrLineResult result = scanYellowLines(raw, outputPath);
            TargetMatch match = TargetMatch.empty();
            log.info("[game-text-ocr] findYellowTarget skipped target matching: reason=blank-target elapsedMs={}",
                    elapsedMillis(startedAtNanos));
            return new TargetOcrResult(result, false, match.editDistance(), match.longestCommonSubstring(),
                    normalizedTarget, OcrTextMatcher.normalizeName(result.joinedText()));
        }

        List<CandidateResult> candidates = new ArrayList<>();
        int ocrCandidateCount = collectYellowCandidates(
                raw, outputPath, expectedTarget, "yellow-target-loose", false, candidates);
        CandidateResult best = bestCandidate(candidates);

        /*
         * The loose mask is cheaper and usually enough after the yellow-threshold tuning. Only run
         * the shadow-expanded variant when no exact/fuzzy match was found, otherwise a successful
         * target line would pay a second round of OCR for no benefit.
         */
        if (best == null || !best.match().hit()) {
            ocrCandidateCount += collectYellowCandidates(
                    raw, outputPath, expectedTarget, "yellow-target-shadow", true, candidates);
            best = bestCandidate(candidates);
        }
        if (best == null) {
            writeBlank(outputPath);
            OcrLineResult empty = OcrLineResult.empty(outputPath, "yellow-target-empty");
            log.info("[game-text-ocr] findYellowTarget done: target={} hit=false candidates=0 ocrCalls={} elapsedMs={}",
                    normalizedTarget, ocrCandidateCount, elapsedMillis(startedAtNanos));
            return new TargetOcrResult(empty, false, 999, 0, normalizedTarget, "");
        }

        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(best.path(), outputPath, StandardCopyOption.REPLACE_EXISTING);
        cleanupCandidateImages(candidates, outputPath);

        TargetMatch match = best.match();
        String summary = "variant=" + best.variantName()
                + ", hit=" + match.hit()
                + ", dist=" + match.editDistance()
                + ", common=" + match.longestCommonSubstring()
                + ", text=" + summarizeWords(best.words());
        OcrLineResult result = new OcrLineResult(
                outputPath.toString(),
                best.variantName(),
                best.blackPixelCount(),
                best.words().size(),
                summary,
                best.words());
        log.info("[game-text-ocr] findYellowTarget done: target={} hit={} bestVariant={} candidates={} ocrCalls={} elapsedMs={} detail={}",
                normalizedTarget, match.hit(), best.variantName(), candidates.size(), ocrCandidateCount,
                elapsedMillis(startedAtNanos), summary);
        return new TargetOcrResult(result, match.hit(), match.editDistance(), match.longestCommonSubstring(),
                normalizedTarget, OcrTextMatcher.normalizeName(best.joinedText()));
    }

    /**
     * Shared colored-text extraction pipeline.
     *
     * <p>Stages: build a color mask, optionally include nearby yellow shadow pixels, group mask
     * components into text lines, pack lines into an enlarged black/white OCR image, run local OCR,
     * then map OCR boxes back to the original image.</p>
     */
    private OcrLineResult scanPackedLines(BufferedImage raw,
                                          Path outputPath,
                                          TextColorMode mode,
                                          boolean includeShadow,
                                          String variantName) throws Exception {
        boolean[][] mask = buildFilteredMask(raw, mode);
        if (includeShadow) {
            mask = includeNearbyYellowShadow(raw, mask, 2);
        }
        List<TextLineBox> lines = groupTextLines(mask);
        List<PackedLineBox> packedLines = new ArrayList<>();
        int blackPixelCount = writePackedLineMask(mask, lines, packedLines, outputPath);
        List<OcrWordResult> packedWords = Files.exists(outputPath)
                ? textRecognizer.getAllTextResultsLocalOnly(outputPath.toString())
                : List.of();
        List<OcrWordResult> rawWords = mapPackedWordsToRaw(packedWords, packedLines);
        return new OcrLineResult(outputPath.toString(), variantName, blackPixelCount,
                rawWords.size(), summarizeWords(rawWords), rawWords);
    }

    /**
     * Build one OCR candidate per yellow text line for fuzzy target matching.
     *
     * <p>Each candidate writes a temporary packed-line image beside {@code outputPath}; the caller
     * chooses the best candidate and removes the rest.</p>
     */
    private int collectYellowCandidates(BufferedImage raw,
                                        Path outputPath,
                                        String expectedTarget,
                                        String variantName,
                                        boolean includeShadow,
                                        List<CandidateResult> candidates) throws Exception {
        boolean[][] mask = buildFilteredMask(raw, TextColorMode.YELLOW_LOOSE);
        if (includeShadow) {
            mask = includeNearbyYellowShadow(raw, mask, 2);
        }
        BufferedImage maskImage = toTextMaskImage(mask);
        List<TextCandidate> visualCandidates;
        try {
            visualCandidates = findTextLikeCandidates(mask, maskImage, raw.getWidth(), raw.getHeight());
        } finally {
            maskImage.flush();
        }
        if (visualCandidates.isEmpty()) {
            log.info("[game-text-ocr] yellow target candidate collection skipped: variant={} reason=no-visual-candidate",
                    variantName);
            return 0;
        }

        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String stem = fileStem(outputPath);
        int ocrCalls = 0;
        for (int i = 0; i < visualCandidates.size(); i++) {
            TextCandidate visualCandidate = visualCandidates.get(i);
            Path candidatePath = parent == null
                    ? Path.of(stem + "__" + variantName + "_" + i + ".png")
                    : parent.resolve(stem + "__" + variantName + "_" + i + ".png");
            List<PackedLineBox> packedLines = new ArrayList<>();
            TextLineBox candidateLine = lineFromCandidate(visualCandidate);
            int blackPixelCount = writePackedLineMask(mask, List.of(candidateLine), packedLines, candidatePath);
            List<OcrWordResult> words =
                    textRecognizer.getAllTextResultsLocalOnly(candidatePath.toString());
            ocrCalls++;
            String joinedText = joinText(words);
            TargetMatch match = targetMatch(joinedText, expectedTarget);
            int score = score(match, joinedText, expectedTarget, words.size());
            candidates.add(new CandidateResult(variantName, candidatePath, blackPixelCount,
                    words, joinedText, match, score));
            log.info("[game-text-ocr] yellow target candidate OCR: variant={} index={} visualScore={} ocrScore={} hit={} text={} reason={}",
                    variantName, i + 1, visualCandidate.score(), score, match.hit(),
                    OcrTextMatcher.normalizeName(joinedText), visualCandidate.reason());
            if (match.hit()) {
                log.info("[game-text-ocr] yellow target candidate collection stopped early: variant={} index={} reason=target-hit",
                        variantName, i + 1);
                return ocrCalls;
            }
        }
        return ocrCalls;
    }

    private CandidateResult bestCandidate(List<CandidateResult> candidates) {
        return candidates == null ? null : candidates.stream()
                .max(Comparator.comparingInt(CandidateResult::score))
                .orElse(null);
    }

    private TextLineBox lineFromCandidate(TextCandidate candidate) {
        OcrWindowRegion region = candidate.region();
        return new TextLineBox(region.x1(), region.y1(), region.x2() - 1, region.y2() - 1,
                Math.max(1, candidate.pixelCount()));
    }

    /**
     * Convert source pixels into a cleaned boolean text mask.
     *
     * <p>Small isolated noise and very large blobs are filtered as connected components. The return
     * array is indexed as {@code mask[y][x]} in source-image coordinates.</p>
     */
    private boolean[][] buildFilteredMask(BufferedImage raw, TextColorMode mode) {
        int width = raw.getWidth();
        int height = raw.getHeight();
        boolean[][] sourceMask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sourceMask[y][x] = isTargetTextPixel(raw.getRGB(x, y), mode);
            }
        }

        boolean[][] keptMask = new boolean[height][width];
        boolean[][] visited = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!sourceMask[y][x] || visited[y][x]) {
                    continue;
                }
                ComponentBox component = collectComponent(sourceMask, visited, x, y);
                if (shouldKeepComponent(component)) {
                    for (Point point : component.points()) {
                        keptMask[point.y][point.x] = true;
                    }
                }
            }
        }
        return keptMask;
    }

    /**
     * Group retained text components into approximate horizontal text lines.
     *
     * @param mask cleaned text mask indexed as {@code mask[y][x]}.
     * @return top-to-bottom, left-to-right line boxes with tiny/noisy lines removed.
     */
    private List<TextLineBox> groupTextLines(boolean[][] mask) {
        int width = mask[0].length;
        int height = mask.length;
        boolean[][] visited = new boolean[height][width];
        List<ComponentBox> components = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x] || visited[y][x]) {
                    continue;
                }
                ComponentBox component = collectComponent(mask, visited, x, y);
                if (shouldKeepComponent(component)) {
                    components.add(component);
                }
            }
        }
        components.sort(Comparator.comparingInt(ComponentBox::centerY).thenComparingInt(ComponentBox::minX));

        List<TextLineBox> lines = new ArrayList<>();
        for (ComponentBox component : components) {
            TextLineBox target = null;
            for (TextLineBox line : lines) {
                if (line.isSameLine(component)) {
                    target = line;
                    break;
                }
            }
            if (target == null) {
                lines.add(TextLineBox.from(component));
            } else {
                target.include(component);
            }
        }
        lines.removeIf(line -> line.pixelCount() < 8 || line.width() < 8 || line.height() < 4);
        lines.sort(Comparator.comparingInt(TextLineBox::centerY).thenComparingInt(TextLineBox::minX));
        return lines;
    }

    private ComponentBox collectComponent(boolean[][] mask, boolean[][] visited, int startX, int startY) {
        int width = mask[0].length;
        int height = mask.length;
        ArrayDeque<Point> queue = new ArrayDeque<>();
        List<Point> points = new ArrayList<>();
        queue.add(new Point(startX, startY));
        visited[startY][startX] = true;

        int minX = startX;
        int minY = startY;
        int maxX = startX;
        int maxY = startY;
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            points.add(point);
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    int nx = point.x + dx;
                    int ny = point.y + dy;
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height || visited[ny][nx] || !mask[ny][nx]) {
                        continue;
                    }
                    visited[ny][nx] = true;
                    queue.addLast(new Point(nx, ny));
                }
            }
        }
        return new ComponentBox(minX, minY, maxX, maxY, points);
    }

    private boolean shouldKeepComponent(ComponentBox component) {
        if (component == null) {
            return false;
        }
        int width = component.width();
        int height = component.height();
        int pixels = component.pixelCount();
        return pixels >= COMPONENT_MIN_PIXELS
                && pixels <= COMPONENT_MAX_PIXELS
                && width >= COMPONENT_MIN_WIDTH
                && height >= COMPONENT_MIN_HEIGHT
                && width <= COMPONENT_MAX_WIDTH
                && height <= COMPONENT_MAX_HEIGHT;
    }

    /**
     * Write retained source lines into one enlarged black/white OCR image.
     *
     * <p>The packed image removes irrelevant background and increases glyph size by {@link #OCR_SCALE}.
     * {@code packedLines} records the mapping from packed-image coordinates back to source-image
     * coordinates so OCR word boxes can be translated after recognition.</p>
     */
    private int writePackedLineMask(boolean[][] mask,
                                    List<TextLineBox> lines,
                                    List<PackedLineBox> packedLines,
                                    Path outputPath) throws Exception {
        int height = mask.length;
        int width = mask[0].length;
        if (lines == null || lines.isEmpty()) {
            writeBlank(outputPath);
            return 0;
        }

        int outputWidth = 1;
        int outputHeight = 0;
        for (TextLineBox line : lines) {
            int sourceX = clamp(line.minX() - LINE_CROP_MARGIN, 0, width - 1);
            int sourceY = clamp(line.minY() - LINE_CROP_MARGIN, 0, height - 1);
            int sourceRight = clamp(line.maxX() + LINE_CROP_MARGIN, 0, width - 1);
            int sourceBottom = clamp(line.maxY() + LINE_CROP_MARGIN, 0, height - 1);
            int lineWidth = sourceRight - sourceX + 1;
            int lineHeight = sourceBottom - sourceY + 1;
            int packedWidth = lineWidth * OCR_SCALE;
            int packedHeight = lineHeight * OCR_SCALE;
            outputWidth = Math.max(outputWidth, packedWidth);
            packedLines.add(new PackedLineBox(sourceX, sourceY, lineWidth, lineHeight,
                    0, outputHeight, packedWidth, packedHeight));
            outputHeight += packedHeight + LINE_PACK_GAP;
        }
        outputHeight = Math.max(1, outputHeight - LINE_PACK_GAP);

        BufferedImage output = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_BYTE_BINARY);
        int blackPixelCount = 0;
        try {
            Graphics2D graphics = output.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, outputWidth, outputHeight);
            } finally {
                graphics.dispose();
            }

            for (PackedLineBox line : packedLines) {
                for (int y = 0; y < line.packedHeight(); y++) {
                    for (int x = 0; x < line.packedWidth(); x++) {
                        int sourceX = line.sourceX() + x / OCR_SCALE;
                        int sourceY = line.sourceY() + y / OCR_SCALE;
                        boolean black = mask[sourceY][sourceX];
                        if (black) {
                            blackPixelCount++;
                        }
                        output.setRGB(line.packedX() + x, line.packedY() + y, black ? 0x000000 : 0xFFFFFF);
                    }
                }
            }
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(output, "png", outputPath.toFile());
            return blackPixelCount;
        } finally {
            output.flush();
        }
    }

    private void writeBlank(Path outputPath) throws Exception {
        BufferedImage blank = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        try {
            blank.setRGB(0, 0, 0xFFFFFF);
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(blank, "png", outputPath.toFile());
        } finally {
            blank.flush();
        }
    }

    /**
     * Translate local OCR boxes from packed-image coordinates back to the original source image.
     *
     * @param words OCR words whose coordinates are relative to the packed debug image.
     * @param packedLines line mappings produced while writing the packed image.
     * @return OCR words in original source-image coordinates.
     */
    private List<OcrWordResult> mapPackedWordsToRaw(List<OcrWordResult> words,
                                                                   List<PackedLineBox> packedLines) {
        if (words == null || words.isEmpty() || packedLines == null || packedLines.isEmpty()) {
            return List.of();
        }
        List<OcrWordResult> mapped = new ArrayList<>();
        for (OcrWordResult word : words) {
            if (word == null) {
                continue;
            }
            PackedLineBox line = findPackedLine(word, packedLines);
            if (line == null) {
                continue;
            }
            int rawLeft = line.sourceX() + Math.round((float) (word.getLeft() - line.packedX()) / OCR_SCALE);
            int rawTop = line.sourceY() + Math.round((float) (word.getTop() - line.packedY()) / OCR_SCALE);
            int rawWidth = Math.max(1, Math.round((float) word.getWidth() / OCR_SCALE));
            int rawHeight = Math.max(1, Math.round((float) word.getHeight() / OCR_SCALE));
            mapped.add(new OcrWordResult(
                    word.getText(), rawLeft, rawTop, rawWidth, rawHeight));
        }
        return mapped;
    }

    private PackedLineBox findPackedLine(OcrWordResult word, List<PackedLineBox> packedLines) {
        int centerY = word.getY();
        int centerX = word.getX();
        for (PackedLineBox line : packedLines) {
            boolean insideY = centerY >= line.packedY() && centerY <= line.packedY() + line.packedHeight();
            boolean insideX = centerX >= line.packedX() && centerX <= line.packedX() + line.packedWidth();
            if (insideY && insideX) {
                return line;
            }
        }
        return null;
    }

    private boolean isTargetTextPixel(int rgb, TextColorMode mode) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        if (mode == TextColorMode.YELLOW_LOOSE) {
            return isLooseYellowTextPixel(r, g, b);
        }
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hueDegrees = hsb[0] * 360.0f;
        return hueDegrees >= 240.0f
                && hueDegrees <= 320.0f
                && hsb[1] >= 0.20f
                && hsb[2] >= 0.18f
                && b >= 80
                && r >= 60
                && g <= 170;
    }

    private boolean isLooseYellowTextPixel(int r, int g, int b) {
        if (isStallVendorGoldPixel(r, g, b)) {
            return false;
        }
        return isNpcNameYellowSamplePixel(r, g, b);
    }

    /**
     * Match the yellow family sampled from real in-world NPC name glyphs.
     *
     * <p>The game draws one NPC name with several anti-aliased yellow strokes, so this detector
     * intentionally keeps both bright pixels such as {@code 253,253,50} and dark edge pixels such
     * as {@code 94,94,18}. The RGB values are sampled from window screenshots and the return value
     * means the pixel should remain in the yellow-name mask. Wider orange/gold UI colors are
     * handled by explicit exclusions before this method is called.</p>
     */
    private boolean isNpcNameYellowSamplePixel(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hueDegrees = hsb[0] * 360.0f;
        return hueDegrees >= 48.0f
                && hueDegrees <= 66.0f
                && r >= 90
                && g >= 90
                && b <= 170
                && Math.abs(r - g) <= 20
                && r > b + 55
                && g > b + 55
                && !(r >= 245 && g >= 245 && b < 35);
    }

    /**
     * Reject the stall/vendor gold color family before candidate extraction.
     *
     * <p>The user sampled stall text around {@code 203,181,91}; later samples showed the blue
     * channel can drift into the low 100s while red/green stay close to {@code 203,181}. We require
     * that red-green separation as part of the blacklist, because real NPC-name yellow samples are
     * near-gray yellow where red and green are almost equal. This method has no side effects and
     * uses window-screenshot RGB values only.</p>
     */
    private boolean isStallVendorGoldPixel(int r, int g, int b) {
        return r >= 198
                && r <= 208
                && g >= 176
                && g <= 186
                && b >= 88
                && b <= 106
                && r - g >= 16
                && r - g <= 30;
    }

    private boolean[][] includeNearbyYellowShadow(BufferedImage raw, boolean[][] baseMask, int radius) {
        int height = baseMask.length;
        int width = baseMask[0].length;
        boolean[][] result = copyMask(baseMask);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (baseMask[y][x]) {
                    continue;
                }
                boolean near = false;
                for (int dy = -radius; dy <= radius && !near; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < width && ny < height && baseMask[ny][nx]) {
                            near = true;
                            break;
                        }
                    }
                }
                if (!near) {
                    continue;
                }
                int rgb = raw.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (isYellowShadowPixel(r, g, b)) {
                    result[y][x] = true;
                }
            }
        }
        return result;
    }

    private boolean isYellowShadowPixel(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hueDegrees = hsb[0] * 360.0f;
        return hueDegrees >= 25.0f
                && hueDegrees <= 85.0f
                && hsb[1] >= 0.22f
                && hsb[2] >= 0.16f
                && r >= 45
                && g >= 42
                && b <= 150
                && Math.max(r, g) > b + 6;
    }

    private boolean[][] copyMask(boolean[][] mask) {
        boolean[][] copy = new boolean[mask.length][mask[0].length];
        for (int y = 0; y < mask.length; y++) {
            System.arraycopy(mask[y], 0, copy[y], 0, mask[y].length);
        }
        return copy;
    }

    /**
     * Convert a washed black/white debug image to a foreground mask.
     *
     * <p>The user's yellow-name washed images are already mostly white background with black glyphs
     * and noise. A luminance threshold is safer here than yellow HSV thresholds because the original
     * color information has been intentionally destroyed.</p>
     */
    private boolean[][] buildBlackPixelMask(BufferedImage washed) {
        int width = washed.getWidth();
        int height = washed.getHeight();
        boolean[][] sourceMask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sourceMask[y][x] = isBlackWashedPixel(washed.getRGB(x, y));
            }
        }

        boolean[][] keptMask = new boolean[height][width];
        boolean[][] visited = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!sourceMask[y][x] || visited[y][x]) {
                    continue;
                }
                ComponentBox component = collectComponent(sourceMask, visited, x, y);
                if (shouldKeepWashedComponent(component)) {
                    for (Point point : component.points()) {
                        keptMask[point.y][point.x] = true;
                    }
                }
            }
        }
        return keptMask;
    }

    /**
     * Keep glyph fragments while dropping isolated dust and large UI art blocks.
     */
    private boolean shouldKeepWashedComponent(ComponentBox component) {
        if (component == null) {
            return false;
        }
        int width = component.width();
        int height = component.height();
        int pixels = component.pixelCount();
        return pixels >= 2
                && pixels <= 1800
                && width >= 1
                && height >= 2
                && width <= 180
                && height <= 80;
    }

    /**
     * Score a merged text line using geometry instead of OCR text.
     *
     * <p>Good NPC-name candidates tend to be wide enough to contain several glyphs, have moderate
     * density, and contain many small components on the same baseline. Task UI frames tend to contain
     * long horizontal/vertical strokes, so those receive a strong penalty.</p>
     */
    private TextCandidate scoreWashedTextLine(boolean[][] mask,
                                             BufferedImage source,
                                             TextLineBox line,
                                             int imageWidth,
                                             int imageHeight) {
        OcrWindowRegion region = new OcrWindowRegion(line.minX(), line.minY(), line.maxX() + 1, line.maxY() + 1)
                .expand(4, 4, imageWidth, imageHeight);
        int width = region.width();
        int height = region.height();
        int pixels = countForeground(mask, region);
        int componentCount = countComponents(mask, region);
        double density = width <= 0 || height <= 0 ? 0.0 : (double) pixels / (double) (width * height);
        int longRowCount = countLongRuns(mask, region, true);
        int longColumnCount = countLongRuns(mask, region, false);
        OcrWindowRegion contextRegion = region.expand(18, 18, imageWidth, imageHeight);
        int contextLongRowCount = Math.max(0, countLongRunsInWashedImage(source, contextRegion, true) - longRowCount);
        int contextLongColumnCount = Math.max(0, countLongRunsInWashedImage(source, contextRegion, false) - longColumnCount);
        int borderPenalty = longRowCount * 18 + longColumnCount * 14;
        int contextFramePenalty = contextLongRowCount * 35 + contextLongColumnCount * 18;
        /*
         * Score text-like evidence, then subtract penalties for shapes that our yellow-mask tests
         * repeatedly exposed as false positives: UI frame lines, tall crumbs, tiny fragments, and
         * sparse low-evidence blobs. The output is still a ranked fallback list, not an OCR hit.
         */
        int densityPenalty = density > 0.42 ? (int) Math.round((density - 0.42) * 160.0) : 0;
        int sizePenalty = height > 55 ? (height - 55) * 2 : 0;
        int sparsePenalty = density < 0.012 ? 22 : 0;
        int verticalFragmentPenalty = height > 24 && height > width * 1.25 ? 90 : 0;
        int tinyFragmentPenalty = width < 38 && (componentCount < 4 || pixels < 90) ? 70 : 0;
        int weakTextPenalty = pixels < 120 && componentCount < 5 && density < 0.08 ? 110 : 0;
        int score = (int) Math.round(width * 0.32 + Math.min(height, 40) * 1.8
                + componentCount * 7.0 + Math.min(pixels, 260) * 0.16)
                - borderPenalty - contextFramePenalty - densityPenalty - sizePenalty - sparsePenalty
                - verticalFragmentPenalty - tinyFragmentPenalty - weakTextPenalty;
        if (width < 24 || height < 6 || pixels < 12 || componentCount < 2) {
            score -= 35;
        }
        Point clickPoint = new Point((region.x1() + region.x2()) / 2, region.y2() + 18);
        String reason = "components=" + componentCount
                + ",pixels=" + pixels
                + ",density=" + String.format(java.util.Locale.ROOT, "%.3f", density)
                + ",longRows=" + longRowCount
                + ",longCols=" + longColumnCount
                + ",contextLongRows=" + contextLongRowCount
                + ",contextLongCols=" + contextLongColumnCount
                + ",verticalPenalty=" + verticalFragmentPenalty
                + ",tinyPenalty=" + tinyFragmentPenalty
                + ",weakPenalty=" + weakTextPenalty;
        return new TextCandidate(region, clickPoint, score, pixels, componentCount,
                density, longRowCount, longColumnCount, reason);
    }

    /**
     * Split a same-baseline line into separate candidates when large blank gaps appear.
     *
     * <p>The normal OCR path keeps loose line grouping so broken glyphs survive. For NPC candidate
     * detection that is too permissive: unrelated task frames and random dust can share a similar Y
     * band. This splitter keeps Chinese-character spacing intact but cuts segments across large blank
     * columns.</p>
     */
    private List<TextLineBox> splitLineByHorizontalGaps(boolean[][] mask, TextLineBox line) {
        int maxBlankGap = Math.max(16, Math.min(24, line.height() * 2));
        List<TextLineBox> segments = new ArrayList<>();
        int segmentStart = -1;
        int lastInkX = -1;
        int segmentMinY = Integer.MAX_VALUE;
        int segmentMaxY = Integer.MIN_VALUE;
        int segmentPixels = 0;

        for (int x = line.minX(); x <= line.maxX(); x++) {
            int columnPixels = 0;
            int columnMinY = Integer.MAX_VALUE;
            int columnMaxY = Integer.MIN_VALUE;
            for (int y = line.minY(); y <= line.maxY(); y++) {
                if (mask[y][x]) {
                    columnPixels++;
                    columnMinY = Math.min(columnMinY, y);
                    columnMaxY = Math.max(columnMaxY, y);
                }
            }
            if (columnPixels > 0) {
                if (segmentStart >= 0 && lastInkX >= 0 && x - lastInkX > maxBlankGap) {
                    addSplitSegment(segments, segmentStart, segmentMinY, lastInkX, segmentMaxY, segmentPixels);
                    segmentStart = -1;
                    segmentMinY = Integer.MAX_VALUE;
                    segmentMaxY = Integer.MIN_VALUE;
                    segmentPixels = 0;
                }
                if (segmentStart < 0) {
                    segmentStart = x;
                }
                lastInkX = x;
                segmentMinY = Math.min(segmentMinY, columnMinY);
                segmentMaxY = Math.max(segmentMaxY, columnMaxY);
                segmentPixels += columnPixels;
            }
        }
        if (segmentStart >= 0) {
            addSplitSegment(segments, segmentStart, segmentMinY, lastInkX, segmentMaxY, segmentPixels);
        }
        return segments.isEmpty() ? List.of(line) : segments;
    }

    private void addSplitSegment(List<TextLineBox> segments,
                                 int minX,
                                 int minY,
                                 int maxX,
                                 int maxY,
                                 int pixelCount) {
        if (pixelCount < 8 || maxX - minX + 1 < 8 || maxY - minY + 1 < 4) {
            return;
        }
        segments.add(new TextLineBox(minX, minY, maxX, maxY, pixelCount));
    }

    private int countForeground(boolean[][] mask, OcrWindowRegion region) {
        int count = 0;
        for (int y = region.y1(); y < region.y2(); y++) {
            for (int x = region.x1(); x < region.x2(); x++) {
                if (mask[y][x]) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countComponents(boolean[][] mask, OcrWindowRegion region) {
        boolean[][] visited = new boolean[region.height()][region.width()];
        int count = 0;
        for (int y = region.y1(); y < region.y2(); y++) {
            for (int x = region.x1(); x < region.x2(); x++) {
                int localY = y - region.y1();
                int localX = x - region.x1();
                if (!mask[y][x] || visited[localY][localX]) {
                    continue;
                }
                floodLocal(mask, visited, region, x, y);
                count++;
            }
        }
        return count;
    }

    private void floodLocal(boolean[][] mask, boolean[][] visited, OcrWindowRegion region, int startX, int startY) {
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));
        visited[startY - region.y1()][startX - region.x1()] = true;
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    int nx = point.x + dx;
                    int ny = point.y + dy;
                    if (nx < region.x1() || ny < region.y1() || nx >= region.x2() || ny >= region.y2()) {
                        continue;
                    }
                    int localX = nx - region.x1();
                    int localY = ny - region.y1();
                    if (visited[localY][localX] || !mask[ny][nx]) {
                        continue;
                    }
                    visited[localY][localX] = true;
                    queue.addLast(new Point(nx, ny));
                }
            }
        }
    }

    private int countLongRuns(boolean[][] mask, OcrWindowRegion region, boolean horizontal) {
        int longRuns = 0;
        int outerStart = horizontal ? region.y1() : region.x1();
        int outerEnd = horizontal ? region.y2() : region.x2();
        int innerStart = horizontal ? region.x1() : region.y1();
        int innerEnd = horizontal ? region.x2() : region.y2();
        int threshold = Math.max(12, (int) Math.round((innerEnd - innerStart) * 0.42));
        for (int outer = outerStart; outer < outerEnd; outer++) {
            int bestRun = 0;
            int currentRun = 0;
            for (int inner = innerStart; inner < innerEnd; inner++) {
                boolean black = horizontal ? mask[outer][inner] : mask[inner][outer];
                if (black) {
                    currentRun++;
                    bestRun = Math.max(bestRun, currentRun);
                } else {
                    currentRun = 0;
                }
            }
            if (bestRun >= threshold) {
                longRuns++;
            }
        }
        return longRuns;
    }

    private int countLongRunsInWashedImage(BufferedImage image, OcrWindowRegion region, boolean horizontal) {
        int longRuns = 0;
        int outerStart = horizontal ? region.y1() : region.x1();
        int outerEnd = horizontal ? region.y2() : region.x2();
        int innerStart = horizontal ? region.x1() : region.y1();
        int innerEnd = horizontal ? region.x2() : region.y2();
        int threshold = Math.max(18, (int) Math.round((innerEnd - innerStart) * 0.50));
        for (int outer = outerStart; outer < outerEnd; outer++) {
            int bestRun = 0;
            int currentRun = 0;
            for (int inner = innerStart; inner < innerEnd; inner++) {
                boolean black = horizontal ? isBlackWashedPixel(image.getRGB(inner, outer))
                        : isBlackWashedPixel(image.getRGB(outer, inner));
                if (black) {
                    currentRun++;
                    bestRun = Math.max(bestRun, currentRun);
                } else {
                    currentRun = 0;
                }
            }
            if (bestRun >= threshold) {
                longRuns++;
            }
        }
        return longRuns;
    }

    private boolean isBlackWashedPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int luminance = (r * 30 + g * 59 + b * 11) / 100;
        return luminance < 150;
    }

    /**
     * Write a boolean text mask as black glyphs on a white background.
     *
     * <p>The candidate detector expects black foreground in washed debug images. Keeping this output
     * convention consistent makes screenshots, overlay images, and standalone tests comparable.</p>
     */
    private BufferedImage toTextMaskImage(boolean[][] mask) {
        int height = mask.length;
        int width = mask[0].length;
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output.setRGB(x, y, mask[y][x] ? 0x000000 : 0xFFFFFF);
            }
        }
        return output;
    }

    private void writeTextMaskImage(BufferedImage output, Path outputPath) throws Exception {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(output, "png", outputPath.toFile());
    }

    private void writeCandidateOverlay(BufferedImage source,
                                       List<TextCandidate> candidates,
                                       Path overlayPath) throws Exception {
        BufferedImage overlay = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        try {
            Graphics2D graphics = overlay.createGraphics();
            try {
                graphics.drawImage(source, 0, 0, null);
                graphics.setStroke(new BasicStroke(2.0f));
                int index = 1;
                for (TextCandidate candidate : candidates) {
                    OcrWindowRegion region = candidate.region();
                    graphics.setColor(index == 1 ? Color.RED : new Color(0, 128, 255));
                    graphics.drawRect(region.x1(), region.y1(), region.width(), region.height());
                    graphics.fillOval(candidate.clickPoint().x - 3, candidate.clickPoint().y - 3, 6, 6);
                    graphics.drawString(index + ":" + candidate.score(), region.x1(), Math.max(12, region.y1() - 3));
                    index++;
                }
            } finally {
                graphics.dispose();
            }
            Path parent = overlayPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(overlay, "png", overlayPath.toFile());
        } finally {
            overlay.flush();
        }
    }

    /**
     * Fuzzy-match yellow OCR text against the expected target name.
     *
     * <p>Game yellow-name OCR can drop the first or last characters, for example recognizing
     * "灵兽村使者" as only "村使". This method keeps the shared matcher as the primary rule, then
     * allows a weaker two-character contiguous hit for this yellow-target path only. The caller
     * still verifies the expected dialog after clicking, so a weak yellow hit cannot complete the
     * NPC click by itself.</p>
     */
    private TargetMatch targetMatch(String ocrText, String expected) {
        OcrTextMatcher.MatchResult result = OcrTextMatcher.matchShortName(ocrText, expected);
        boolean yellowHit = result.hit() || result.longestCommonSubstring() >= 2;
        return new TargetMatch(yellowHit, result.editDistance(), result.longestCommonSubstring());
    }

    private int score(TargetMatch match, String ocrText, String expected, int wordCount) {
        if (match != null && match.hit()) {
            return OcrTextMatcher.shortNameMatchScore(ocrText, expected)
                    + Math.max(0, 20 - match.editDistance() * 2)
                    + match.longestCommonSubstring();
        }
        String text = OcrTextMatcher.normalizeName(ocrText);
        String target = OcrTextMatcher.normalizeName(expected);
        int score = 0;
        for (int i = 0; i < target.length(); i++) {
            if (text.indexOf(target.charAt(i)) >= 0) {
                score += 10;
            }
        }
        return score + Math.min(wordCount, 4);
    }

    private String summarizeWords(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "-";
        }
        return words.stream()
                .limit(WORD_SUMMARY_LIMIT)
                .map(word -> {
                    if (word == null) {
                        return "null";
                    }
                    return String.format("%s@(%d,%d,%d,%d,%d,%d,%.3f)",
                            word.getText(), word.getX(), word.getY(), word.getLeft(), word.getTop(),
                            word.getWidth(), word.getHeight(), word.getScore());
                })
                .collect(Collectors.joining(" | "));
    }

    private String joinText(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (OcrWordResult word : words) {
            if (word != null && word.getText() != null) {
                builder.append(word.getText());
            }
        }
        return builder.toString();
    }

    private void cleanupCandidateImages(List<CandidateResult> candidates, Path keepPath) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (CandidateResult candidate : candidates) {
            if (candidate == null || candidate.path() == null || candidate.path().equals(keepPath)) {
                continue;
            }
            try {
                Files.deleteIfExists(candidate.path());
            } catch (Exception e) {
                log.debug("[game-text-ocr] delete temp candidate failed: path={} reason={}",
                        candidate.path(), e.getMessage());
            }
        }
    }

    private String fileStem(Path path) {
        if (path == null || path.getFileName() == null) {
            return "ocr_line";
        }
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private long elapsedMillis(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    private enum TextColorMode {
        PURPLE,
        YELLOW_LOOSE
    }

    private record ComponentBox(int minX, int minY, int maxX, int maxY, List<Point> points) {
        int centerY() {
            return (minY + maxY) / 2;
        }

        int width() {
            return maxX - minX + 1;
        }

        int height() {
            return maxY - minY + 1;
        }

        int pixelCount() {
            return points == null ? 0 : points.size();
        }
    }

    private static final class TextLineBox {
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;
        private int pixelCount;

        private TextLineBox(int minX, int minY, int maxX, int maxY, int pixelCount) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.pixelCount = pixelCount;
        }

        static TextLineBox from(ComponentBox component) {
            return new TextLineBox(component.minX(), component.minY(), component.maxX(), component.maxY(),
                    component.pixelCount());
        }

        boolean isSameLine(ComponentBox component) {
            int centerDelta = Math.abs(centerY() - component.centerY());
            boolean yOverlaps = component.maxY() + LINE_MERGE_Y_TOLERANCE >= minY
                    && component.minY() - LINE_MERGE_Y_TOLERANCE <= maxY;
            return yOverlaps || centerDelta <= Math.max(LINE_MERGE_Y_TOLERANCE, height() / 2);
        }

        void include(ComponentBox component) {
            minX = Math.min(minX, component.minX());
            minY = Math.min(minY, component.minY());
            maxX = Math.max(maxX, component.maxX());
            maxY = Math.max(maxY, component.maxY());
            pixelCount += component.pixelCount();
        }

        int minX() {
            return minX;
        }

        int minY() {
            return minY;
        }

        int maxX() {
            return maxX;
        }

        int maxY() {
            return maxY;
        }

        int centerY() {
            return (minY + maxY) / 2;
        }

        int width() {
            return maxX - minX + 1;
        }

        int height() {
            return maxY - minY + 1;
        }

        int pixelCount() {
            return pixelCount;
        }
    }

    private record PackedLineBox(int sourceX,
                                 int sourceY,
                                 int sourceWidth,
                                 int sourceHeight,
                                 int packedX,
                                 int packedY,
                                 int packedWidth,
                                 int packedHeight) {
    }

    private record CandidateResult(String variantName,
                                   Path path,
                                   int blackPixelCount,
                                   List<OcrWordResult> words,
                                   String joinedText,
                                   TargetMatch match,
                                   int score) {
    }

    private record TargetMatch(boolean hit, int editDistance, int longestCommonSubstring) {
        static TargetMatch empty() {
            return new TargetMatch(false, 999, 0);
        }
    }

}
