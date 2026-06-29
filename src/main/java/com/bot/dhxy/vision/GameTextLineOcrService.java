package com.bot.dhxy.vision;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;


import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.ocr.OcrLineResult;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.ocr.TargetOcrResult;
import com.bot.dhxy.model.ocr.TextCandidate;
import com.bot.dhxy.model.ocr.TextCandidateScanResult;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.tools.ImagePreprocessor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final int YELLOW_TARGET_TEXT_CANDIDATE_LIMIT = 12;
    private static final int YELLOW_TARGET_TEXT_CANDIDATE_MIN_SCORE = 5;
    private static final String STRICT_YELLOW_TARGET_JIANGMO_SHIWEI = "降魔侍卫";
    private static final int STRICT_YELLOW_TARGET_JIANGMO_SHIWEI_MIN_COMMON = 3;
    private static final int ROUTE_YELLOW_ROW_TOLERANCE_PX = 8;
    private static final int ROUTE_YELLOW_WRAP_LEFT_MAX_X = 80;
    private static final int ROUTE_YELLOW_WRAP_RIGHT_MARGIN_X = 80;
    private static final int ROUTE_COORDINATE_ROW_TOLERANCE_PX = 22;
    private static final int ROUTE_COORDINATE_DESTINATION_ROW_UP_PX = 7;
    private static final int ROUTE_COORDINATE_DESTINATION_ROW_DOWN_PX = 9;
    private static final Pattern COORDINATE_LINK_PATTERN =
            Pattern.compile("[\\(（]\\s*\\d+\\s*[,，]\\s*\\d+\\s*[\\)）]?");

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
     * Read option-dialog words with the standard green-first/yellow-fallback pipeline.
     *
     * <p>The caller owns dialog detection, screenshot capture, and click decisions. This method only
     * owns colored-text preprocessing and OCR routing for an already captured dialog image. Output
     * paths should be window-scoped by the caller because they are written as debug/intermediate
     * images.</p>
     *
     * @param rawPath source dialog screenshot path.
     * @param targetKeyword diagnostic target text written into OCR logs.
     * @param aliases target aliases accepted by the caller. Empty or null falls back to
     *                {@code targetKeyword}.
     * @param greenPath window-scoped path for the green-text washed image.
     * @param yellowPath window-scoped path for the yellow-text washed image.
     * @return OCR result for the first matching color pass, or a combined green/yellow result when
     * neither pass matches. Word coordinates are image-local to {@code rawPath}.
     */
    public OcrLineResult readDialogOptionWords(String rawPath,
                                               String targetKeyword,
                                               List<String> aliases,
                                               Path greenPath,
                                               Path yellowPath) {
        List<String> keywords = aliases == null || aliases.isEmpty()
                ? List.of(targetKeyword)
                : aliases;
        String targetLabel = targetKeyword == null ? "unknown" : targetKeyword;

        ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, greenPath.toString());
        if (!Files.exists(greenPath)) {
            List<OcrWordResult> words = textRecognizer.getAllTextResultsForMatch(
                    rawPath,
                    "dialog-options:" + targetLabel + ":raw",
                    result -> OcrTextMatcher.hasAnyKeyword(result, keywords));
            return dialogOptionWordsResult("raw", rawPath, words);
        }

        /*
         * Normal option text is green, but route recommendation choices can be yellow. Keep green
         * first for ordinary dialogs, then retry the same raw snapshot with the shared yellow-text
         * wash so transfer choices such as "长安桥(400两)" do not disappear before OCR.
         */
        List<OcrWordResult> greenWords = textRecognizer.getAllTextResultsForMatch(
                greenPath.toString(),
                "dialog-options:" + targetLabel + ":green",
                words -> OcrTextMatcher.hasAnyKeyword(words, keywords));
        if (OcrTextMatcher.hasAnyKeyword(greenWords, keywords)) {
            return dialogOptionWordsResult("green", greenPath.toString(), greenWords);
        }

        ImagePreprocessor.washYellowText(rawPath, yellowPath.toString());
        if (!Files.exists(yellowPath)) {
            return dialogOptionWordsResult("green", greenPath.toString(), greenWords);
        }
        List<OcrWordResult> yellowWords = textRecognizer.getAllTextResultsForMatch(
                yellowPath.toString(),
                "dialog-options:" + targetLabel + ":yellow",
                words -> OcrTextMatcher.hasAnyKeyword(words, keywords));
        return OcrTextMatcher.hasAnyKeyword(yellowWords, keywords)
                ? dialogOptionWordsResult("yellow", yellowPath.toString(), yellowWords)
                : dialogOptionWordsResult("green+yellow", yellowPath.toString(), mergeWords(greenWords, yellowWords));
    }

    /**
     * Find ranked NPC-name-like yellow text candidates directly from a raw game screenshot.
     *
     * <p>This is the formal candidate API for yellow NPC/monster names when exact OCR either has
     * not run yet or did not match the requested target. It builds the stricter NPC-name yellow
     * mask used by target clicking, expands nearby yellow shadow pixels, optionally writes a
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
        boolean[][] mask = buildFilteredMask(raw, TextColorMode.YELLOW_NPC_TARGET);
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

    /**
     * Verify the final yellow destination name in a world-map route-result screenshot.
     *
     * <p>The caller owns screenshot capture and input. This method owns only text handling: it
     * writes a yellow-only debug image beside the raw screenshot, runs local OCR on that image, then
     * parses the bottom route destination. Once yellow OCR runs, the destination must equal the
     * expected map name before the caller is allowed to click a green coordinate link. Do not relax
     * this to substring matching: map names such as 长安/长安城东 and 凤巢六层/凤巢七层 must stay distinct.</p>
     *
     * @param rawImagePath route-result screenshot path.
     * @param expectedDestinationName map name that was typed into the world-map search box.
     * @return route destination guard result, including the yellow debug image path when produced.
     */
    public WorldMapRouteDestinationResult verifyWorldMapRouteDestination(String rawImagePath,
                                                                         String expectedDestinationName) {
        if (expectedDestinationName == null || expectedDestinationName.isBlank()) {
            return WorldMapRouteDestinationResult.builder()
                    .allowClick(true)
                    .checked(false)
                    .matched(false)
                    .message("expected destination is blank")
                    .build();
        }

        String yellowPath = preprocessWorldMapRouteDestinationYellow(rawImagePath);
        if (yellowPath.equals(rawImagePath)) {
            log.info("[game-text-ocr] route destination guard skipped, yellow preprocessing unavailable raw={}",
                    rawImagePath);
            return WorldMapRouteDestinationResult.builder()
                    .allowClick(false)
                    .checked(false)
                    .matched(false)
                    .yellowImagePath(yellowPath)
                    .message("yellow preprocessing unavailable")
                    .build();
        }

        String expected = normalizeRouteDestinationName(expectedDestinationName);
        long startedAt = System.currentTimeMillis();
        RouteDestinationMatch destinationMatch = findLastWorldMapRouteDestination(yellowPath, expected);
        String rawActual = destinationMatch.text();
        String actual = normalizeRouteDestinationName(rawActual);
        boolean matched = !actual.isBlank()
                && (actual.equals(expected)
                || destinationMatch.acceptedExpected()
                || isAcceptedRouteDestinationAlias(actual, expected));
        long elapsedMs = System.currentTimeMillis() - startedAt;
        log.info("[game-text-ocr] route destination guard elapsedMs={} expected={} actual={} rawActual={} matched={} yellow={}",
                elapsedMs, expected, actual, rawActual, matched, yellowPath);
        return WorldMapRouteDestinationResult.builder()
                .allowClick(matched)
                .checked(true)
                .matched(matched)
                .expected(expected)
                .actual(actual)
                .rawActual(rawActual)
                .yellowImagePath(yellowPath)
                .destinationCenterX(destinationMatch.centerX())
                .destinationCenterY(destinationMatch.centerY())
                .elapsedMs(elapsedMs)
                .message(actual.isBlank() ? "actual destination is blank" : (matched ? "matched" : "mismatched"))
                .build();
    }

    /**
     * Find the final green coordinate link in a world-map route-result screenshot.
     *
     * <p>The method writes the same-size green-only debug image beside {@code rawImagePath}, OCRs
     * that image first, and falls back to the raw screenshot if the washed image produces no
     * coordinate. Returned points are image-local to the original route-result screenshot because
     * the washed image keeps identical dimensions.</p>
     *
     * @param rawImagePath route-result screenshot path.
     * @param destination yellow destination OCR result from the same route-result screenshot. When
     *                    present, its center row is used to choose the green route coordinate line
     *                    before falling back to full OCR.
     * @return coordinate result with the OCR image path used for the winning match.
     */
    public WorldMapRouteCoordinateResult findLastWorldMapRouteCoordinate(String rawImagePath,
                                                                         WorldMapRouteDestinationResult destination) {
        String greenPath = preprocessWorldMapRouteCoordinateGreen(rawImagePath);
        long startedAt = System.currentTimeMillis();
        String ocrImagePath = greenPath;
        boolean usedPreprocessedImage = !greenPath.equals(rawImagePath);

        /*
         * Search-result routes can wrap the last green coordinate across two visual rows. In that
         * case OCR may read the complete text in joinedText but cannot attach a word box to the
         * final coordinate. The yellow destination row is the reliable anchor: click the green text
         * segment on the same row first, then fall back to normal coordinate OCR.
         */
        Point point = findRouteCoordinateByDestinationRow(greenPath, destination);
        if (point == null) {
            point = findLastCoordinateLink(greenPath);
        }
        if (point == null && usedPreprocessedImage) {
            log.info("[game-text-ocr] green route OCR missed, fallback raw image={}", rawImagePath);
            point = findLastCoordinateLink(rawImagePath);
            ocrImagePath = rawImagePath;
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;
        log.info("[game-text-ocr] route coordinate OCR elapsedMs={} found={} image={}",
                elapsedMs, point != null, ocrImagePath);
        return WorldMapRouteCoordinateResult.builder()
                .found(point != null)
                .relativeCenter(point)
                .ocrImagePath(ocrImagePath)
                .usedPreprocessedImage(!ocrImagePath.equals(rawImagePath))
                .elapsedMs(elapsedMs)
                .message(point == null ? "coordinate not found" : "coordinate found")
                .build();
    }

    private String preprocessWorldMapRouteDestinationYellow(String rawImagePath) {
        Path rawPath = Path.of(rawImagePath);
        String rawFileName = rawPath.getFileName().toString();
        String yellowFileName = rawFileName.endsWith(".png")
                ? rawFileName.substring(0, rawFileName.length() - ".png".length()) + "_yellow.png"
                : rawFileName + "_yellow.png";
        Path yellowPath = rawPath.resolveSibling(yellowFileName);
        try {
            ImagePreprocessor.washYellowText(rawImagePath, yellowPath.toString());
            if (Files.exists(yellowPath)) {
                log.info("[game-text-ocr] route destination yellow preprocessing raw={} yellow={}",
                        rawImagePath, yellowPath);
                return yellowPath.toString();
            }
            log.warn("[game-text-ocr] route destination yellow preprocessing produced no file raw={}", rawImagePath);
        } catch (Exception e) {
            log.warn("[game-text-ocr] route destination yellow preprocessing failed raw={} reason={}",
                    rawImagePath, e.getMessage(), e);
        }
        return rawImagePath;
    }

    private String preprocessWorldMapRouteCoordinateGreen(String rawImagePath) {
        Path rawPath = Path.of(rawImagePath);
        String rawFileName = rawPath.getFileName().toString();
        String greenFileName = rawFileName.endsWith(".png")
                ? rawFileName.substring(0, rawFileName.length() - ".png".length()) + "_green.png"
                : rawFileName + "_green.png";
        Path greenPath = rawPath.resolveSibling(greenFileName);
        try {
            ImagePreprocessor.washGreenTextToBlackAndWhite(rawImagePath, greenPath.toString());
            if (Files.exists(greenPath)) {
                log.info("[game-text-ocr] route OCR green preprocessing raw={} green={}",
                        rawImagePath, greenPath);
                return greenPath.toString();
            }
            log.warn("[game-text-ocr] route OCR green preprocessing produced no file raw={}", rawImagePath);
        } catch (Exception e) {
            log.warn("[game-text-ocr] route OCR green preprocessing failed raw={} reason={}",
                    rawImagePath, e.getMessage(), e);
        }
        return rawImagePath;
    }

    private RouteDestinationMatch findLastWorldMapRouteDestination(String yellowImagePath, String expected) {
        List<OcrWordResult> words = textRecognizer.getAllTextResultsLocalOnly(yellowImagePath);
        OcrWordResult last = null;
        for (OcrWordResult word : words) {
            if (word == null || word.getText() == null || word.getText().isBlank()) {
                continue;
            }
            if (last == null || word.getTop() + word.getHeight() > last.getTop() + last.getHeight()) {
                last = word;
            }
        }
        RouteDestinationMatch value = findLastWorldMapRouteDestinationLine(words, expected);
        /*
         * Short map names can be hidden by route wrapping. If whole-image OCR only fuzzy-matches a
         * prefix such as "长安城" for target "长安", still run packed segment OCR so a bottom exact
         * destination can override the fuzzy row and anchor the coordinate search correctly.
         */
        boolean exactExpected = isExactRouteDestinationMatch(value, expected);
        if (!exactExpected && expected != null && !expected.isBlank()) {
            RouteDestinationMatch segmentValue = findRouteDestinationFromPackedYellowSegments(yellowImagePath, expected);
            if (isExactRouteDestinationMatch(segmentValue, expected)
                    || (!value.acceptedExpected() && segmentValue.acceptedExpected())) {
                value = segmentValue;
            }
        }
        if (value.text().isBlank() && last != null) {
            value = new RouteDestinationMatch(last.getText(), last.getX(), centerY(last), false);
        }
        log.info("[game-text-ocr] route destination OCR words={} last={}",
                formatRouteOcrWords(words), value.text());
        return value;
    }

    private boolean isExactRouteDestinationMatch(RouteDestinationMatch match, String expected) {
        return match != null
                && expected != null
                && !expected.isBlank()
                && expected.equals(normalizeRouteDestinationName(match.text()));
    }

    private RouteDestinationMatch findRouteDestinationFromPackedYellowSegments(String yellowImagePath, String expected) {
        try {
            BufferedImage yellowImage = ImageIO.read(Path.of(yellowImagePath).toFile());
            if (yellowImage == null) {
                return RouteDestinationMatch.empty();
            }
            try {
                boolean[][] mask = buildBrightPixelMask(yellowImage);
                List<TextLineBox> segments = new ArrayList<>();
                for (TextLineBox line : groupTextLines(mask)) {
                    segments.addAll(splitLineByHorizontalGaps(mask, line));
                }
                if (segments.isEmpty()) {
                    return RouteDestinationMatch.empty();
                }

                /*
                 * Whole-image OCR can miss very short map names that touch the left edge of the
                 * route result, for example a final "长安" line. Packing each yellow text segment
                 * gives the OCR sidecar margin and scale without changing the production click
                 * coordinate system.
                 */
                Path yellowPath = Path.of(yellowImagePath);
                String fileName = yellowPath.getFileName().toString();
                String packedFileName = fileName.endsWith(".png")
                        ? fileName.substring(0, fileName.length() - ".png".length()) + "_segments.png"
                        : fileName + "_segments.png";
                Path packedPath = yellowPath.resolveSibling(packedFileName);
                List<PackedLineBox> packedLines = new ArrayList<>();
                writePackedLineMask(mask, segments, packedLines, packedPath);
                List<OcrWordResult> packedWords = textRecognizer.getAllTextResultsLocalOnly(packedPath.toString());
                List<OcrWordResult> mappedWords = mapPackedWordsToRaw(packedWords, packedLines);
                RouteDestinationMatch match = findExpectedRouteDestination(
                        buildRouteYellowRows(mappedWords), mappedWords, expected);
                log.info("[game-text-ocr] route destination packed-segment OCR expected={} words={} matched={} image={}",
                        expected, formatRouteOcrWords(mappedWords), match.acceptedExpected(), packedPath);
                return match;
            } finally {
                yellowImage.flush();
            }
        } catch (Exception e) {
            log.warn("[game-text-ocr] route destination packed-segment OCR failed: image={} reason={}",
                    yellowImagePath, e.getMessage(), e);
            return RouteDestinationMatch.empty();
        }
    }

    private RouteDestinationMatch findLastWorldMapRouteDestinationLine(List<OcrWordResult> words, String expected) {
        List<OcrWordResult> usable = new ArrayList<>();
        for (OcrWordResult word : words) {
            if (word == null || word.getText() == null || word.getText().isBlank()) {
                continue;
            }
            usable.add(word);
        }
        if (usable.isEmpty()) {
            return RouteDestinationMatch.empty();
        }

        /*
         * One route row can contain two yellow map names:
         *   走到 洛阳城(...) 处点击车夫传送到 长安
         * Also, a destination can wrap:
         *   传送到洛
         *   阳城
         * Keep OCR rows separate first, then stitch only that right-edge/left-edge wrap pattern.
         */
        List<List<OcrWordResult>> rows = buildRouteYellowRows(usable);
        if (rows.isEmpty()) {
            return RouteDestinationMatch.empty();
        }

        /*
         * The typed map name is the strongest signal. Search every yellow cluster first and choose
         * the bottom-most exact match for that target. This avoids stitching unrelated route rows
         * such as "北俱芦" + "长安" while still allowing a real wrapped destination when the joined
         * text exactly equals the requested map name.
         */
        RouteDestinationMatch exact = findExpectedRouteDestination(rows, usable, expected);
        if (!exact.text().isBlank()) {
            return exact;
        }

        List<OcrWordResult> bottomRow = rows.get(rows.size() - 1);
        List<List<OcrWordResult>> bottomClusters = clusterRouteYellowWords(bottomRow);
        if (bottomClusters.isEmpty()) {
            return RouteDestinationMatch.empty();
        }

        List<OcrWordResult> rightMostCluster = bottomClusters.get(bottomClusters.size() - 1);
        if (isLeftWrappedRouteContinuation(rightMostCluster) && rows.size() >= 2) {
            List<List<OcrWordResult>> previousClusters = clusterRouteYellowWords(rows.get(rows.size() - 2));
            if (!previousClusters.isEmpty()) {
                List<OcrWordResult> previousRightMost = previousClusters.get(previousClusters.size() - 1);
                if (isRightEdgeRoutePrefix(previousRightMost, usable)) {
                    return new RouteDestinationMatch(
                            joinRouteYellowWords(previousRightMost) + joinRouteYellowWords(rightMostCluster),
                            averageCenterX(rightMostCluster),
                            averageCenterY(rightMostCluster),
                            false);
                }
            }
        }
        return new RouteDestinationMatch(
                joinRouteYellowWords(rightMostCluster),
                averageCenterX(rightMostCluster),
                averageCenterY(rightMostCluster),
                false);
    }

    private RouteDestinationMatch findExpectedRouteDestination(List<List<OcrWordResult>> rows,
                                                              List<OcrWordResult> usable,
                                                              String expected) {
        if (expected == null || expected.isBlank()) {
            return RouteDestinationMatch.empty();
        }

        List<RouteDestinationMatch> exactMatches = new ArrayList<>();
        RouteDestinationMatch bestFuzzy = RouteDestinationMatch.empty();
        int bestFuzzyScore = 0;
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<List<OcrWordResult>> clusters = clusterRouteYellowWords(rows.get(rowIndex));
            for (List<OcrWordResult> cluster : clusters) {
                RouteDestinationMatch direct = toRouteDestinationMatch(cluster);
                if (expected.equals(normalizeRouteDestinationName(direct.text()))) {
                    exactMatches.add(direct.accepted());
                } else {
                    OcrTextMatcher.MatchResult fuzzy = OcrTextMatcher.matchShortName(direct.text(), expected);
                    if (fuzzy.hit() && isBetterRouteFuzzyMatch(direct, fuzzy.score(), bestFuzzy, bestFuzzyScore)) {
                        bestFuzzy = direct.accepted();
                        bestFuzzyScore = fuzzy.score();
                    }
                }

                if (isLeftWrappedRouteContinuation(cluster) && rowIndex > 0) {
                    List<List<OcrWordResult>> previousClusters = clusterRouteYellowWords(rows.get(rowIndex - 1));
                    if (!previousClusters.isEmpty()) {
                        List<OcrWordResult> previousRightMost = previousClusters.get(previousClusters.size() - 1);
                        if (isRightEdgeRoutePrefix(previousRightMost, usable)) {
                            RouteDestinationMatch wrapped = new RouteDestinationMatch(
                                    joinRouteYellowWords(previousRightMost) + joinRouteYellowWords(cluster),
                                    averageCenterX(cluster),
                                    averageCenterY(cluster),
                                    false);
                            if (expected.equals(normalizeRouteDestinationName(wrapped.text()))) {
                                exactMatches.add(wrapped.accepted());
                            } else {
                                OcrTextMatcher.MatchResult fuzzy = OcrTextMatcher.matchShortName(wrapped.text(), expected);
                                if (fuzzy.hit() && isBetterRouteFuzzyMatch(wrapped, fuzzy.score(), bestFuzzy, bestFuzzyScore)) {
                                    bestFuzzy = wrapped.accepted();
                                    bestFuzzyScore = fuzzy.score();
                                }
                            }
                        }
                    }
                }
            }
        }

        RouteDestinationMatch exact = exactMatches.stream()
                .max(Comparator.comparing((RouteDestinationMatch match) -> match.centerY() == null ? -1 : match.centerY())
                        .thenComparing(match -> match.centerX() == null ? -1 : match.centerX()))
                .orElse(RouteDestinationMatch.empty());
        if (!exact.text().isBlank()) {
            return exact;
        }
        if (!bestFuzzy.text().isBlank()) {
            log.info("[game-text-ocr] route destination fuzzy match: expected={} actual={} score={}",
                    expected, bestFuzzy.text(), bestFuzzyScore);
        }
        return bestFuzzy;
    }

    private boolean isBetterRouteFuzzyMatch(RouteDestinationMatch candidate,
                                            int candidateScore,
                                            RouteDestinationMatch current,
                                            int currentScore) {
        if (candidate.text().isBlank()) {
            return false;
        }
        if (current.text().isBlank() || candidateScore != currentScore) {
            return candidateScore > currentScore;
        }
        int candidateY = candidate.centerY() == null ? -1 : candidate.centerY();
        int currentY = current.centerY() == null ? -1 : current.centerY();
        if (candidateY != currentY) {
            return candidateY > currentY;
        }
        int candidateX = candidate.centerX() == null ? -1 : candidate.centerX();
        int currentX = current.centerX() == null ? -1 : current.centerX();
        return candidateX > currentX;
    }

    private RouteDestinationMatch toRouteDestinationMatch(List<OcrWordResult> cluster) {
        if (cluster == null || cluster.isEmpty()) {
            return RouteDestinationMatch.empty();
        }
        return new RouteDestinationMatch(
                joinRouteYellowWords(cluster),
                averageCenterX(cluster),
                averageCenterY(cluster),
                false);
    }

    private Point findLastCoordinateLink(String imagePath) {
        List<OcrWordResult> words = textRecognizer.getAllTextResultsForMatch(
                imagePath,
                "world-map-route-coordinate",
                routeWords -> findLastCoordinateLinkInWords(routeWords) != null);
        return findLastCoordinateLinkInWords(words);
    }

    private Point findLastCoordinateLinkInWords(List<OcrWordResult> words) {
        int lastX = -1;
        int lastY = -1;

        for (OcrWordResult word : words) {
            if (word == null || word.getText() == null) {
                continue;
            }
            Matcher matcher = COORDINATE_LINK_PATTERN.matcher(word.getText());
            while (matcher.find()) {
                int textLength = Math.max(word.getText().length(), 1);
                int blockLeft = word.getLeft();
                int blockWidth = Math.max(word.getWidth(), 1);
                int matchLeft = blockLeft + (int) Math.round(blockWidth * (matcher.start() / (double) textLength));
                int matchRight = blockLeft + (int) Math.round(blockWidth * (matcher.end() / (double) textLength));
                lastX = (matchLeft + matchRight) / 2;
                lastX = Math.max(blockLeft, Math.min(blockLeft + blockWidth, lastX));
                lastY = word.getY();

                log.info("[game-text-ocr] route coordinate match: words=[{}] match=[{}] block=({}, {}, {}, {}) "
                                + "range=({}, {}) point=({}, {})",
                        word.getText(), matcher.group(), word.getLeft(), word.getTop(), word.getWidth(), word.getHeight(),
                        matchLeft, matchRight, lastX, lastY);
            }
        }

        return lastX == -1 ? null : new Point(lastX, lastY);
    }

    private Point findRouteCoordinateByDestinationRow(String greenImagePath,
                                                      WorldMapRouteDestinationResult destination) {
        if (destination == null || destination.destinationCenterY() == null) {
            return null;
        }
        try {
            BufferedImage greenImage = ImageIO.read(Path.of(greenImagePath).toFile());
            if (greenImage == null) {
                return null;
            }
            try {
                boolean[][] mask = buildBrightPixelMask(greenImage);
                int destinationY = destination.destinationCenterY();
                int rowMinY = Math.max(0, destinationY - ROUTE_COORDINATE_DESTINATION_ROW_UP_PX);
                int rowMaxY = Math.min(greenImage.getHeight() - 1, destinationY + ROUTE_COORDINATE_DESTINATION_ROW_DOWN_PX);
                List<TextLineBox> destinationRowSegments = splitHorizontalSegmentsInBand(mask, rowMinY, rowMaxY);
                TextLineBox sameVisualRow = null;
                for (TextLineBox segment : destinationRowSegments) {
                    if (destination.destinationCenterX() != null
                            && segment.minX() >= destination.destinationCenterX()) {
                        continue;
                    }
                    if (sameVisualRow == null
                            || segment.maxX() > sameVisualRow.maxX()
                            || (segment.maxX() == sameVisualRow.maxX()
                            && segment.pixelCount() > sameVisualRow.pixelCount())) {
                        sameVisualRow = segment;
                    }
                }
                if (sameVisualRow == null && !destinationRowSegments.isEmpty()) {
                    for (TextLineBox segment : destinationRowSegments) {
                        if (sameVisualRow == null || segment.maxX() > sameVisualRow.maxX()) {
                            sameVisualRow = segment;
                        }
                    }
                }
                if (sameVisualRow != null) {
                    Point point = new Point(sameVisualRow.centerX(), sameVisualRow.centerY());
                    log.info("[game-text-ocr] route coordinate visual-row match: destination=({}, {}) "
                                    + "sameRow=({}, {})-({}, {}) point=({}, {}) bandY=({}, {})",
                            destination.destinationCenterX(), destination.destinationCenterY(),
                            sameVisualRow.minX(), sameVisualRow.minY(), sameVisualRow.maxX(), sameVisualRow.maxY(),
                            point.x, point.y, rowMinY, rowMaxY);
                    return point;
                }

                List<TextLineBox> segments = new ArrayList<>();
                for (TextLineBox line : groupTextLines(mask)) {
                    segments.addAll(splitLineByHorizontalGaps(mask, line));
                }
                TextLineBox best = null;
                int bestDelta = Integer.MAX_VALUE;
                TextLineBox rightEdgeBest = null;
                int rightEdgeBestDelta = Integer.MAX_VALUE;
                int rightEdgeX = Math.max(0, greenImage.getWidth() - ROUTE_YELLOW_WRAP_RIGHT_MARGIN_X);
                for (TextLineBox segment : segments) {
                    int delta = Math.abs(segment.centerY() - destination.destinationCenterY());
                    if (delta > ROUTE_COORDINATE_ROW_TOLERANCE_PX) {
                        continue;
                    }
                    /*
                     * Wrapped route rows put the real final coordinate prefix at the right edge
                     * while the yellow destination is on the next visual line. Do not require the
                     * green segment to be left of the yellow text; prefer right-edge segments on
                     * the destination row, then fall back to the closest/rightmost row segment.
                     */
                    if (segment.maxX() >= rightEdgeX
                            && (rightEdgeBest == null
                            || delta < rightEdgeBestDelta
                            || (delta == rightEdgeBestDelta && segment.maxX() > rightEdgeBest.maxX()))) {
                        rightEdgeBest = segment;
                        rightEdgeBestDelta = delta;
                    }
                    if (best == null
                            || delta < bestDelta
                            || (delta == bestDelta && segment.maxX() > best.maxX())) {
                        best = segment;
                        bestDelta = delta;
                    }
                }
                TextLineBox chosen = rightEdgeBest != null ? rightEdgeBest : best;
                int chosenDelta = rightEdgeBest != null ? rightEdgeBestDelta : bestDelta;
                if (chosen == null) {
                    return null;
                }
                Point point = new Point(chosen.centerX(), chosen.centerY());
                log.info("[game-text-ocr] route coordinate visual-row match: destination=({}, {}) "
                                + "segment=({}, {})-({}, {}) point=({}, {}) deltaY={} rightEdgePreferred={}",
                        destination.destinationCenterX(), destination.destinationCenterY(),
                        chosen.minX(), chosen.minY(), chosen.maxX(), chosen.maxY(), point.x, point.y,
                        chosenDelta, rightEdgeBest != null);
                return point;
            } finally {
                greenImage.flush();
            }
        } catch (Exception e) {
            log.warn("[game-text-ocr] route coordinate visual-row match failed: image={} reason={}",
                    greenImagePath, e.getMessage(), e);
                return null;
        }
    }

    private List<TextLineBox> splitHorizontalSegmentsInBand(boolean[][] mask, int minY, int maxY) {
        int width = mask[0].length;
        int maxBlankGap = 16;
        List<TextLineBox> segments = new ArrayList<>();
        int segmentStart = -1;
        int lastInkX = -1;
        int segmentMinY = Integer.MAX_VALUE;
        int segmentMaxY = Integer.MIN_VALUE;
        int segmentPixels = 0;

        for (int x = 0; x < width; x++) {
            int columnPixels = 0;
            int columnMinY = Integer.MAX_VALUE;
            int columnMaxY = Integer.MIN_VALUE;
            for (int y = minY; y <= maxY; y++) {
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
        return segments;
    }

    private List<List<OcrWordResult>> buildRouteYellowRows(List<OcrWordResult> words) {
        List<OcrWordResult> sorted = new ArrayList<>(words);
        sorted.sort((a, b) -> {
            int byY = Integer.compare(centerY(a), centerY(b));
            return byY != 0 ? byY : Integer.compare(a.getLeft(), b.getLeft());
        });

        List<List<OcrWordResult>> rows = new ArrayList<>();
        for (OcrWordResult word : sorted) {
            if (rows.isEmpty()) {
                List<OcrWordResult> row = new ArrayList<>();
                row.add(word);
                rows.add(row);
                continue;
            }
            List<OcrWordResult> currentRow = rows.get(rows.size() - 1);
            if (Math.abs(centerY(word) - averageCenterY(currentRow)) <= ROUTE_YELLOW_ROW_TOLERANCE_PX) {
                currentRow.add(word);
            } else {
                List<OcrWordResult> row = new ArrayList<>();
                row.add(word);
                rows.add(row);
            }
        }
        for (List<OcrWordResult> row : rows) {
            row.sort((a, b) -> Integer.compare(a.getLeft(), b.getLeft()));
        }
        return rows;
    }

    private List<List<OcrWordResult>> clusterRouteYellowWords(List<OcrWordResult> row) {
        List<List<OcrWordResult>> clusters = new ArrayList<>();
        List<OcrWordResult> currentCluster = new ArrayList<>();
        for (OcrWordResult word : row) {
            if (currentCluster.isEmpty()) {
                currentCluster.add(word);
                continue;
            }
            OcrWordResult previous = currentCluster.get(currentCluster.size() - 1);
            int gap = word.getLeft() - (previous.getLeft() + previous.getWidth());
            int clusterGapThreshold = Math.max(18, Math.max(previous.getHeight(), word.getHeight()) * 2);
            if (gap > clusterGapThreshold) {
                clusters.add(currentCluster);
                currentCluster = new ArrayList<>();
            }
            currentCluster.add(word);
        }
        if (!currentCluster.isEmpty()) {
            clusters.add(currentCluster);
        }
        return clusters;
    }

    private boolean isLeftWrappedRouteContinuation(List<OcrWordResult> cluster) {
        return !cluster.isEmpty() && cluster.get(0).getLeft() <= ROUTE_YELLOW_WRAP_LEFT_MAX_X;
    }

    private boolean isRightEdgeRoutePrefix(List<OcrWordResult> cluster, List<OcrWordResult> allWords) {
        if (cluster.isEmpty()) {
            return false;
        }
        /*
         * Real wrapped destinations look like a right-edge prefix followed by a left-edge
         * continuation. A vertical result list such as 凤巢六层 / 凤巢七层 also has left-edge words,
         * so never stitch two left-edge rows together or the destination guard rejects valid input.
         */
        if (cluster.get(0).getLeft() <= ROUTE_YELLOW_WRAP_LEFT_MAX_X) {
            return false;
        }
        return clusterRight(cluster) >= maxWordRight(allWords) - ROUTE_YELLOW_WRAP_RIGHT_MARGIN_X;
    }

    private int averageCenterY(List<OcrWordResult> row) {
        int total = 0;
        for (OcrWordResult word : row) {
            total += centerY(word);
        }
        return total / Math.max(1, row.size());
    }

    private int averageCenterX(List<OcrWordResult> row) {
        int total = 0;
        for (OcrWordResult word : row) {
            total += word.getX();
        }
        return total / Math.max(1, row.size());
    }

    private int clusterRight(List<OcrWordResult> cluster) {
        int right = Integer.MIN_VALUE;
        for (OcrWordResult word : cluster) {
            right = Math.max(right, word.getLeft() + word.getWidth());
        }
        return right == Integer.MIN_VALUE ? 0 : right;
    }

    private int maxWordRight(List<OcrWordResult> words) {
        int right = 0;
        for (OcrWordResult word : words) {
            right = Math.max(right, word.getLeft() + word.getWidth());
        }
        return right;
    }

    private String joinRouteYellowWords(List<OcrWordResult> words) {
        StringBuilder builder = new StringBuilder();
        for (OcrWordResult word : words) {
            builder.append(word.getText());
        }
        return builder.toString();
    }

    private int centerY(OcrWordResult word) {
        return word.getTop() + Math.max(1, word.getHeight()) / 2;
    }

    private String formatRouteOcrWords(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "[]";
        }
        return words.stream()
                .map(word -> word == null ? "null" : word.getText() + "@("
                        + word.getLeft() + "," + word.getTop() + ","
                        + word.getWidth() + "x" + word.getHeight() + ")")
                .toList()
                .toString();
    }

    private String normalizeRouteDestinationName(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^\\u4E00-\\u9FFFA-Za-z0-9]+", "").trim();
    }

    private boolean isAcceptedRouteDestinationAlias(String actual, String expected) {
        return "北俱".equals(actual) && "北俱芦洲".equals(expected);
    }

    private List<TextCandidate> findTextLikeCandidates(boolean[][] mask,
                                                       BufferedImage sourceForContext,
                                                       int imageWidth,
                                                       int imageHeight) {
        return findTextLikeCandidates(mask, sourceForContext, imageWidth, imageHeight,
                DEFAULT_TEXT_CANDIDATE_LIMIT, 25);
    }

    private List<TextCandidate> findTextLikeCandidates(boolean[][] mask,
                                                       BufferedImage sourceForContext,
                                                       int imageWidth,
                                                       int imageHeight,
                                                       int candidateLimit,
                                                       int minimumScore) {
        List<TextLineBox> lines = groupTextLines(mask);
        List<TextCandidate> candidates = new ArrayList<>();
        for (TextLineBox line : lines) {
            for (TextLineBox segment : splitLineByHorizontalGaps(mask, line)) {
                TextCandidate candidate = scoreWashedTextLine(mask, sourceForContext, segment, imageWidth, imageHeight);
                if (candidate.score() >= minimumScore) {
                    candidates.add(candidate);
                }
            }
        }
        candidates.sort(Comparator.comparingInt(TextCandidate::score).reversed()
                .thenComparing(candidate -> candidate.region().y1())
                .thenComparing(candidate -> candidate.region().x1()));
        int keepCount = Math.min(Math.max(1, candidateLimit), candidates.size());
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
                raw, outputPath, expectedTarget, "yellow-target-npc", false, candidates);
        CandidateResult best = bestCandidate(candidates);

        /*
         * The loose mask is cheaper and usually enough after the yellow-threshold tuning. Only run
         * the shadow-expanded variant when no exact/fuzzy match was found, otherwise a successful
         * target line would pay a second round of OCR for no benefit.
         */
        if (best == null || !isStrongTargetMatch(best.match(), best.joinedText(), expectedTarget)) {
            ocrCandidateCount += collectYellowCandidates(
                    raw, outputPath, expectedTarget, "yellow-target-npc-shadow", true, candidates);
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
        boolean strongHit = isStrongTargetMatch(match, best.joinedText(), expectedTarget);
        boolean acceptedHit = match.hit()
                && (strictYellowTargetMinCommon(normalizedTarget) <= 0 || strongHit);
        String summary = "variant=" + best.variantName()
                + ", hit=" + acceptedHit
                + ", rawHit=" + match.hit()
                + ", strong=" + strongHit
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
                normalizedTarget, acceptedHit, best.variantName(), candidates.size(), ocrCandidateCount,
                elapsedMillis(startedAtNanos), summary);
        return new TargetOcrResult(result, acceptedHit, match.editDistance(), match.longestCommonSubstring(),
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
        boolean[][] mask = buildFilteredMask(raw, TextColorMode.YELLOW_NPC_TARGET);
        if (includeShadow) {
            mask = includeNearbyYellowShadow(raw, mask, 2);
        }
        BufferedImage maskImage = toTextMaskImage(mask);
        List<TextCandidate> visualCandidates;
        try {
            visualCandidates = findTextLikeCandidates(mask, maskImage, raw.getWidth(), raw.getHeight(),
                    YELLOW_TARGET_TEXT_CANDIDATE_LIMIT, YELLOW_TARGET_TEXT_CANDIDATE_MIN_SCORE);
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
            List<OcrWordResult> packedWords =
                    textRecognizer.getAllTextResultsLocalOnly(candidatePath.toString());
            List<OcrWordResult> words = mapPackedWordsToRaw(packedWords, packedLines);
            ocrCalls++;
            String joinedText = joinText(words);
            TargetMatch match = targetMatch(joinedText, expectedTarget);
            int score = score(match, joinedText, expectedTarget, words.size());
            candidates.add(new CandidateResult(variantName, candidatePath, blackPixelCount,
                    words, joinedText, match, score));
            boolean strongHit = isStrongTargetMatch(match, joinedText, expectedTarget);
            log.info("[game-text-ocr] yellow target candidate OCR: variant={} index={} visualScore={} ocrScore={} hit={} strong={} text={} reason={}",
                    variantName, i + 1, visualCandidate.score(), score, match.hit(), strongHit,
                    OcrTextMatcher.normalizeName(joinedText), visualCandidate.reason());
            if (strongHit) {
                log.info("[game-text-ocr] yellow target candidate collection stopped early: variant={} index={} reason=strong-target-hit",
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
        if (mode == TextColorMode.YELLOW_NPC_TARGET) {
            return isNpcTargetYellowTextPixel(r, g, b);
        }
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
     * Keep only the low-brightness NPC-name yellow used for in-world target labels.
     *
     * <p>Task tracker headers and UI panels often use pure or high-brightness yellow such as
     * {@code 255,255,0}; those must not become NPC click candidates. This stricter mode is used
     * only by target clicking, while generic yellow OCR keeps the wider range.</p>
     */
    private boolean isNpcTargetYellowTextPixel(int r, int g, int b) {
        if (isStallVendorGoldPixel(r, g, b)) {
            return false;
        }
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hueDegrees = hsb[0] * 360.0f;
        return hueDegrees >= 55.0f
                && hueDegrees <= 64.5f
                && r >= 110
                && g >= 110
                && r <= 220
                && g <= 220
                && b >= 45
                && b <= 120
                && Math.abs(r - g) <= 8
                && r > b + 45
                && g > b + 45;
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
     * Convert white-on-black washed text into a foreground mask.
     *
     * <p>World-map route green washes currently keep retained green glyphs as white pixels on a
     * black background. Route-row geometry must therefore read bright glyphs; using the normal
     * black-pixel mask would treat the panel background as one huge component and erase the text.</p>
     */
    private boolean[][] buildBrightPixelMask(BufferedImage washed) {
        int width = washed.getWidth();
        int height = washed.getHeight();
        boolean[][] sourceMask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sourceMask[y][x] = isBrightWashedPixel(washed.getRGB(x, y));
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
        Point clickPoint = new Point((region.x1() + region.x2()) / 2, region.y2() - 50);
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

    private boolean isBrightWashedPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int luminance = (r * 30 + g * 59 + b * 11) / 100;
        return luminance > 180;
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
     * allows a weaker two-character contiguous hit for this yellow-target path only. For two-character
     * NPC names, allow one OCR substitution as well: 张闻 can be read as 收闻 while the visual block
     * is still clearly the intended short yellow NPC name. The caller still verifies the expected
     * dialog after clicking, so a weak yellow hit cannot complete the NPC click by itself.</p>
     */
    private TargetMatch targetMatch(String ocrText, String expected) {
        OcrTextMatcher.MatchResult result = OcrTextMatcher.matchShortName(ocrText, expected);
        boolean shortNameOneGlyphOff = result.normalizedTarget().length() == 2
                && result.normalizedText().length() == 2
                && result.editDistance() <= 1;
        boolean yellowHit = result.hit() || result.longestCommonSubstring() >= 2 || shortNameOneGlyphOff;
        return new TargetMatch(yellowHit, result.editDistance(), result.longestCommonSubstring());
    }

    private int score(TargetMatch match, String ocrText, String expected, int wordCount) {
        if (match != null && match.hit()) {
            boolean strongHit = isStrongTargetMatch(match, ocrText, expected);
            int base = OcrTextMatcher.shortNameMatchScore(ocrText, expected)
                    + Math.max(0, 20 - match.editDistance() * 2)
                    + match.longestCommonSubstring();
            if (strongHit) {
                return 10_000 + base;
            }
            /*
             * Strict target names must not let a two-character fuzzy hit outrank the actual target.
             * Keep the weak score positive for diagnostics, but low enough that a later strong OCR
             * candidate wins even when its visual line score is lower.
             */
            if (strictYellowTargetMinCommon(OcrTextMatcher.normalizeName(expected)) > 0) {
                return 100 + match.longestCommonSubstring() * 10 - match.editDistance();
            }
            return 1_000 + base;
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

    private boolean isStrongTargetMatch(TargetMatch match, String ocrText, String expected) {
        if (match == null || !match.hit()) {
            return false;
        }
        String normalizedText = OcrTextMatcher.normalizeName(ocrText);
        String normalizedTarget = OcrTextMatcher.normalizeName(expected);
        int strictMinCommon = strictYellowTargetMinCommon(normalizedTarget);
        if (strictMinCommon <= 0) {
            return true;
        }
        if (normalizedText.isBlank()) {
            return false;
        }
        if (normalizedText.equals(normalizedTarget) || normalizedText.contains(normalizedTarget)) {
            return true;
        }
        int almostFull = Math.max(strictMinCommon, normalizedTarget.length() - 1);
        return normalizedTarget.contains(normalizedText)
                && normalizedText.length() >= almostFull
                && match.longestCommonSubstring() >= almostFull
                && match.editDistance() <= 1;
    }

    private int strictYellowTargetMinCommon(String normalizedTarget) {
        return STRICT_YELLOW_TARGET_JIANGMO_SHIWEI.equals(normalizedTarget)
                ? STRICT_YELLOW_TARGET_JIANGMO_SHIWEI_MIN_COMMON
                : 0;
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

    private OcrLineResult dialogOptionWordsResult(String variantName, String path, List<OcrWordResult> words) {
        List<OcrWordResult> safeWords = words == null ? List.of() : words;
        return OcrLineResult.builder()
                .path(path)
                .variantName(variantName)
                .blackPixelCount(0)
                .wordCount(safeWords.size())
                .wordsSummary(summarizeWords(safeWords))
                .words(safeWords)
                .build();
    }

    private List<OcrWordResult> mergeWords(List<OcrWordResult> first, List<OcrWordResult> second) {
        List<OcrWordResult> merged = new ArrayList<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return merged;
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
        YELLOW_NPC_TARGET,
        YELLOW_LOOSE
    }

    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    public static class WorldMapRouteDestinationResult {
        boolean allowClick;
        boolean checked;
        boolean matched;
        String expected;
        String actual;
        String rawActual;
        String yellowImagePath;
        Integer destinationCenterX;
        Integer destinationCenterY;
        long elapsedMs;
        String message;
    }

    private record RouteDestinationMatch(String text, Integer centerX, Integer centerY, boolean acceptedExpected) {
        private static RouteDestinationMatch empty() {
            return new RouteDestinationMatch("", null, null, false);
        }

        private RouteDestinationMatch accepted() {
            return new RouteDestinationMatch(text, centerX, centerY, true);
        }
    }

    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    public static class WorldMapRouteCoordinateResult {
        boolean found;
        Point relativeCenter;
        String ocrImagePath;
        boolean usedPreprocessedImage;
        long elapsedMs;
        String message;
    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    private static class ComponentBox {


        int minX;


        int minY;


        int maxX;


        int maxY;


        List<Point> points;

        int centerY() {
            return (minY + maxY) / 2;
        }

        int centerX() {
            return (minX + maxX) / 2;
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

        int centerX() {
            return (minX + maxX) / 2;
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

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    private static class PackedLineBox {


        int sourceX;


        int sourceY;


        int sourceWidth;


        int sourceHeight;


        int packedX;


        int packedY;


        int packedWidth;


        int packedHeight;


    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    private static class CandidateResult {


        String variantName;


        Path path;


        int blackPixelCount;


        List<OcrWordResult> words;


        String joinedText;


        TargetMatch match;


        int score;


    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    private static class TargetMatch {


        boolean hit;


        int editDistance;


        int longestCommonSubstring;

        static TargetMatch empty() {
            return new TargetMatch(false, 999, 0);
        }
    


    }

}
