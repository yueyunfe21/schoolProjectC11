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
import com.bot.dhxy.cloud.task.ImagePreprocessOperation;
import com.bot.dhxy.cloud.task.ImageProcessorService;
import com.bot.dhxy.cloud.task.ImageProcessorService.ImageProcessorResult;
import com.bot.dhxy.cloud.task.ImageProcessorService.PackedLineMapping;
import com.bot.dhxy.cloud.task.ImageProcessorService.RequestMetadata;
import com.bot.dhxy.cloud.task.ImageProcessorService.TextCandidateBox;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extracts colored in-game name text into OCR-friendly line images.
 *
 * <p>The service is intentionally local/OCR-sidecar oriented: it receives an already captured
 * image, asks the cloud image processor for OCR-friendly masks/candidate geometry, and maps OCR
 * word boxes back to the original image coordinate space. It does not capture windows or send any
 * physical input.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameTextLineOcrService {

    private static final int OCR_SCALE = 4;
    private static final int WORD_SUMMARY_LIMIT = 12;
    private static final int YELLOW_TARGET_TEXT_CANDIDATE_LIMIT = 12;
    private static final int YELLOW_TARGET_TEXT_CANDIDATE_MIN_SCORE = 5;
    private static final String STRICT_YELLOW_TARGET_JIANGMO_SHIWEI = "降魔侍卫";
    private static final int STRICT_YELLOW_TARGET_JIANGMO_SHIWEI_MIN_COMMON = 3;
    private static final int ROUTE_YELLOW_ROW_TOLERANCE_PX = 8;
    private static final int ROUTE_YELLOW_WRAP_LEFT_MAX_X = 80;
    private static final int ROUTE_YELLOW_WRAP_RIGHT_MARGIN_X = 80;

    private final TextRecognizer textRecognizer;
    private final ImageProcessorService imageProcessorService;

    /**
     * Extract purple player-name style text from an image.
     *
     * @param raw source image in image-local pixels; ownership stays with the caller.
     * @param outputPath file path where the packed black/white OCR image should be written.
     * @return OCR words mapped back to the source image coordinate space; empty result if no line is kept.
     * @throws Exception if the debug image cannot be written or local OCR throws.
     */
    public OcrLineResult scanPurpleLines(BufferedImage raw, Path outputPath) throws Exception {
        return scanWashedLines(raw, outputPath, ImagePreprocessOperation.WASH_PURPLE, "purple-line");
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
        return scanWashedLines(raw, outputPath, ImagePreprocessOperation.WASH_YELLOW, "yellow-line");
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

        ImageProcessorResult greenWash = imageProcessorService.washToPath(
                Path.of(rawPath),
                greenPath,
                ImagePreprocessOperation.WASH_GREEN,
                imagePreprocessMetadata(rawPath, "dialog-options-green", targetLabel));
        if (!greenWash.hasImage()) {
            log.info("[game-text-ocr] dialog green wash miss: target={} status={} reason={}",
                    targetLabel, greenWash.status(), greenWash.reason());
            return dialogOptionWordsResult("cloud-green-miss", rawPath, List.of());
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

        ImageProcessorResult yellowWash = imageProcessorService.washToPath(
                Path.of(rawPath),
                yellowPath,
                ImagePreprocessOperation.WASH_YELLOW,
                imagePreprocessMetadata(rawPath, "dialog-options-yellow", targetLabel));
        if (!yellowWash.hasImage()) {
            log.info("[game-text-ocr] dialog yellow wash miss: target={} status={} reason={}",
                    targetLabel, yellowWash.status(), yellowWash.reason());
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
        return findYellowTextCandidateResult(raw, null, washedPath, overlayPath);
    }

    /**
     * Find ranked NPC-name-like yellow text candidates using the target-specific yellow profile.
     *
     * <p>Most targets intentionally use the strict default profile. Known outlier NPC names such as
     * 白龙马 can select a dedicated profile so the direct OCR path and this shape-only fallback use
     * the same color policy instead of disagreeing about which yellow glyph pixels exist.</p>
     *
     * @param raw source screenshot in image-local pixels; ownership stays with caller.
     * @param expectedTarget target NPC name used only to choose the yellow profile.
     * @param washedPath optional black-on-white yellow text mask output path.
     * @param overlayPath optional candidate overlay output path.
     * @return result object that owns an immutable score-sorted candidate list. Empty means the
     *         screenshot did not contain any stable yellow text-like candidate after filtering.
     * @throws Exception when debug image writing fails.
     */
    public TextCandidateScanResult findYellowTextCandidateResult(BufferedImage raw,
                                                                 String expectedTarget,
                                                                 Path washedPath,
                                                                 Path overlayPath) throws Exception {
        if (raw == null) {
            return TextCandidateScanResult.empty("raw image is null");
        }
        ImageProcessorResult result = imageProcessorService.findTextCandidates(
                raw,
                yellowCandidateMetadata(null, expectedTarget));
        return textCandidateScanResultFromCloud(raw, result, washedPath, overlayPath);
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
        ImageProcessorResult result = imageProcessorService.findTextCandidates(
                washed,
                RequestMetadata.builder()
                        .debugImageId("game-text-ocr:washed-text-candidates")
                        .source("game-text-ocr")
                        .phase("washed-text-candidates")
                        .build());
        return textCandidateScanResultFromCloud(washed, result, null, overlayPath);
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
     * expected map name before the caller is allowed to click the yellow destination row. That row
     * opens the destination mini-map, where the caller then clicks its already-known final map
     * coordinate. Do not relax this to substring matching: map names such as 长安/长安城东 and
     * 凤巢六层/凤巢七层 must stay distinct.</p>
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
        if (yellowPath == null || yellowPath.equals(rawImagePath)) {
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

    private String preprocessWorldMapRouteDestinationYellow(String rawImagePath) {
        Path rawPath = Path.of(rawImagePath);
        String rawFileName = rawPath.getFileName().toString();
        String yellowFileName = rawFileName.endsWith(".png")
                ? rawFileName.substring(0, rawFileName.length() - ".png".length()) + "_yellow.png"
                : rawFileName + "_yellow.png";
        Path yellowPath = rawPath.resolveSibling(yellowFileName);
        try {
            ImageProcessorResult result =
                    imageProcessorService.washToPath(
                            rawPath,
                            yellowPath,
                            ImagePreprocessOperation.WASH_YELLOW,
                            imagePreprocessMetadata(rawImagePath, "route-destination-yellow", null));
            if (result.hasImage()) {
                log.info("[game-text-ocr] route destination yellow preprocessing raw={} yellow={}",
                        rawImagePath, yellowPath);
                return yellowPath.toString();
            }
            log.warn("[game-text-ocr] route destination yellow preprocessing missed raw={} status={} reason={}",
                    rawImagePath, result.status(), result.reason());
            return null;
        } catch (Exception e) {
            log.warn("[game-text-ocr] route destination yellow preprocessing failed raw={} reason={}",
                    rawImagePath, e.getMessage(), e);
            return null;
        }
    }

    private RequestMetadata imagePreprocessMetadata(
            String rawPath,
            String phase,
            String debugLabel) {
        String label = debugLabel == null || debugLabel.isBlank() ? phase : debugLabel;
        return RequestMetadata.builder()
                .rawImagePath(rawPath)
                .debugImageId("game-text-ocr:" + label)
                .source("game-text-ocr")
                .phase(phase)
                .build();
    }

    private RequestMetadata yellowCandidateMetadata(Path debugPath, String expectedTarget) {
        String normalizedTarget = OcrTextMatcher.normalizeName(expectedTarget);
        return RequestMetadata.builder()
                .rawImagePath(debugPath == null ? "" : debugPath.toString())
                .debugImageId("game-text-ocr:yellow-candidates:" + normalizedTarget)
                .source("game-text-ocr")
                .phase("yellow-text-candidates")
                .parameters(Map.of(
                        "textColor", "YELLOW",
                        "candidateProfile", yellowTargetProfile(expectedTarget).name(),
                        "expectedTarget", normalizedTarget,
                        "candidateLimit", Integer.toString(YELLOW_TARGET_TEXT_CANDIDATE_LIMIT),
                        "minimumScore", Integer.toString(YELLOW_TARGET_TEXT_CANDIDATE_MIN_SCORE),
                        "includeShadow", "true",
                        "clickPointPolicy", "NPC_NAME_BELOW"))
                .build();
    }

    private TextCandidateScanResult textCandidateScanResultFromCloud(BufferedImage source,
                                                                     ImageProcessorResult result,
                                                                     Path washedPath,
                                                                     Path overlayPath) throws Exception {
        if (result == null) {
            return TextCandidateScanResult.empty("image processor returned null");
        }
        if (result.hasImage() && washedPath != null) {
            writeImage(result.image(), washedPath);
        }
        List<TextCandidate> candidates = textCandidatesFromCloud(
                result,
                source == null ? 0 : source.getWidth(),
                source == null ? 0 : source.getHeight());
        if (overlayPath != null) {
            BufferedImage overlaySource = result.hasImage() ? result.image() : source;
            if (overlaySource != null) {
                writeCandidateOverlay(overlaySource, candidates, overlayPath);
            }
        }
        if (candidates.isEmpty()) {
            return TextCandidateScanResult.empty(
                    "cloud text candidates unavailable: status=" + result.status() + ", reason=" + result.reason());
        }
        return TextCandidateScanResult.of(candidates, overlayPath == null ? null : overlayPath.toString());
    }

    private List<TextCandidate> textCandidatesFromCloud(ImageProcessorResult result, int imageWidth, int imageHeight) {
        if (result == null || result.textCandidates().isEmpty()) {
            return List.of();
        }
        List<TextCandidate> candidates = new ArrayList<>();
        for (TextCandidateBox cloud : result.textCandidates()) {
            OcrWindowRegion region = new OcrWindowRegion(
                    cloud.x(),
                    cloud.y(),
                    cloud.x() + Math.max(1, cloud.width()),
                    cloud.y() + Math.max(1, cloud.height()))
                    .clamp(Math.max(1, imageWidth), Math.max(1, imageHeight));
            int clickX = cloud.clickX() == null ? (region.x1() + region.x2()) / 2 : cloud.clickX();
            int clickY = cloud.clickY() == null ? region.y2() - 50 : cloud.clickY();
            candidates.add(new TextCandidate(
                    region,
                    new Point(clickX, clickY),
                    cloud.score(),
                    cloud.pixelCount(),
                    cloud.componentCount(),
                    cloud.density(),
                    cloud.longRowCount(),
                    cloud.longColumnCount(),
                    cloud.reason()));
        }
        return List.copyOf(candidates);
    }

    private List<OcrWordResult> wordsInCandidate(List<OcrWordResult> words, TextCandidate candidate) {
        if (words == null || words.isEmpty() || candidate == null || candidate.region() == null) {
            return List.of();
        }
        OcrWindowRegion region = candidate.region().expand(6, 6, Integer.MAX_VALUE / 4, Integer.MAX_VALUE / 4);
        List<OcrWordResult> selected = new ArrayList<>();
        for (OcrWordResult word : words) {
            if (word == null) {
                continue;
            }
            int centerX = word.getLeft() + Math.max(1, word.getWidth()) / 2;
            int centerY = word.getTop() + Math.max(1, word.getHeight()) / 2;
            if (centerX >= region.x1() && centerX <= region.x2()
                    && centerY >= region.y1() && centerY <= region.y2()) {
                selected.add(word);
            }
        }
        return List.copyOf(selected);
    }

    private OcrLineResult scanWashedLines(BufferedImage raw,
                                          Path outputPath,
                                          ImagePreprocessOperation operation,
                                          String variantName) throws Exception {
        ImageProcessorResult result = switch (operation) {
            case WASH_PURPLE -> imageProcessorService.washPurpleTextToBlackAndWhite(
                    raw,
                    imagePreprocessMetadata(outputPath == null ? "" : outputPath.toString(), variantName, variantName));
            case WASH_YELLOW -> imageProcessorService.washYellowText(
                    raw,
                    imagePreprocessMetadata(outputPath == null ? "" : outputPath.toString(), variantName, variantName));
            default -> imageProcessorService.washGreenTextToBlackAndWhite(
                    raw,
                    imagePreprocessMetadata(outputPath == null ? "" : outputPath.toString(), variantName, variantName));
        };
        if (!result.hasImage()) {
            writeBlank(outputPath);
            return OcrLineResult.empty(outputPath, variantName + "-cloud-miss");
        }
        writeImage(result.image(), outputPath);
        List<OcrWordResult> words = textRecognizer.getAllTextResultsLocalOnly(outputPath.toString());
        return new OcrLineResult(outputPath.toString(), variantName, 0,
                words.size(), summarizeWords(words), words);
    }

    private void writeImage(BufferedImage image, Path outputPath) throws Exception {
        if (image == null || outputPath == null) {
            return;
        }
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(image, "png", outputPath.toFile());
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
            RouteDestinationMatch segmentValue = findRouteDestinationFromCloudPackedYellowSegments(
                    yellowImagePath, expected);
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

    private RouteDestinationMatch findRouteDestinationFromCloudPackedYellowSegments(String yellowImagePath,
                                                                                   String expected) {
        try {
            BufferedImage yellowImage = ImageIO.read(Path.of(yellowImagePath).toFile());
            if (yellowImage == null) {
                return RouteDestinationMatch.empty();
            }
            try {
                /*
                 * Whole-image OCR can miss very short map names that touch the left edge of the
                 * route result, for example a final "长安" line. Packing each yellow text segment
                 * gives the OCR sidecar margin and scale. The packed image and source mapping are
                 * cloud-owned; this service only OCRs the packed PNG and maps boxes back.
                 */
                Path yellowPath = Path.of(yellowImagePath);
                String fileName = yellowPath.getFileName().toString();
                String packedFileName = fileName.endsWith(".png")
                        ? fileName.substring(0, fileName.length() - ".png".length()) + "_segments.png"
                        : fileName + "_segments.png";
                Path packedPath = yellowPath.resolveSibling(packedFileName);
                ImageProcessorResult packed = imageProcessorService.routePackedLineMask(
                        yellowImage,
                        imagePreprocessMetadata(yellowImagePath, "route-packed-line-mask", expected));
                if (!packed.hasImage() || packed.packedLineMappings().isEmpty()) {
                    log.info("[game-text-ocr] route destination packed-segment cloud miss expected={} status={} reason={} image={}",
                            expected, packed.status(), packed.reason(), yellowImagePath);
                    return RouteDestinationMatch.empty();
                }
                writeImage(packed.image(), packedPath);
                List<OcrWordResult> packedWords = textRecognizer.getAllTextResultsLocalOnly(packedPath.toString());
                List<OcrWordResult> mappedWords = mapPackedWordsToRaw(packedWords, packed.packedLineMappings());
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
        YellowTargetProfile profile = yellowTargetProfile(expectedTarget);
        if (normalizedTarget.isBlank()) {
            OcrLineResult result = scanYellowLines(raw, outputPath);
            TargetMatch match = TargetMatch.empty();
            log.info("[game-text-ocr] findYellowTarget skipped target matching: reason=blank-target elapsedMs={}",
                    elapsedMillis(startedAtNanos));
            return new TargetOcrResult(result, false, match.editDistance(), match.longestCommonSubstring(),
                    normalizedTarget, OcrTextMatcher.normalizeName(result.joinedText()));
        }

        ImageProcessorResult cloudCandidates = imageProcessorService.findTextCandidates(
                raw,
                yellowCandidateMetadata(outputPath, expectedTarget));
        List<TextCandidate> visualCandidates = textCandidatesFromCloud(cloudCandidates, raw.getWidth(), raw.getHeight());
        if (visualCandidates.isEmpty() || !cloudCandidates.hasImage()) {
            writeBlank(outputPath);
            OcrLineResult empty = OcrLineResult.empty(outputPath, "yellow-target-empty");
            log.info("[game-text-ocr] findYellowTarget done: target={} profile={} hit=false candidates={} ocrCalls=0 elapsedMs={} reason={} status={}",
                    normalizedTarget, profile, visualCandidates.size(), elapsedMillis(startedAtNanos),
                    cloudCandidates.reason(), cloudCandidates.status());
            return new TargetOcrResult(empty, false, 999, 0, normalizedTarget, "");
        }

        writeImage(cloudCandidates.image(), outputPath);
        List<OcrWordResult> allWords = textRecognizer.getAllTextResultsLocalOnly(outputPath.toString());
        List<CandidateResult> candidates = new ArrayList<>();
        for (int i = 0; i < visualCandidates.size(); i++) {
            TextCandidate visualCandidate = visualCandidates.get(i);
            List<OcrWordResult> words = wordsInCandidate(allWords, visualCandidate);
            String joinedText = joinText(words);
            TargetMatch match = targetMatch(joinedText, expectedTarget);
            int score = score(match, joinedText, expectedTarget, words.size());
            candidates.add(new CandidateResult("yellow-target-cloud", outputPath,
                    Math.max(0, visualCandidate.pixelCount()), words, joinedText, match, score));
            boolean strongHit = isStrongTargetMatch(match, joinedText, expectedTarget);
            log.info("[game-text-ocr] yellow target cloud candidate OCR: profile={} index={} visualScore={} ocrScore={} hit={} strong={} text={} reason={}",
                    profile, i + 1, visualCandidate.score(), score, match.hit(), strongHit,
                    OcrTextMatcher.normalizeName(joinedText), visualCandidate.reason());
            if (strongHit) {
                break;
            }
        }
        CandidateResult best = bestCandidate(candidates);
        if (best == null) {
            writeBlank(outputPath);
            OcrLineResult empty = OcrLineResult.empty(outputPath, "yellow-target-empty");
            log.info("[game-text-ocr] findYellowTarget done: target={} profile={} hit=false candidates=0 ocrCalls={} elapsedMs={}",
                    normalizedTarget, profile, 1, elapsedMillis(startedAtNanos));
            return new TargetOcrResult(empty, false, 999, 0, normalizedTarget, "");
        }

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
        log.info("[game-text-ocr] findYellowTarget done: target={} profile={} hit={} bestVariant={} candidates={} ocrCalls={} elapsedMs={} detail={}",
                normalizedTarget, profile, acceptedHit, best.variantName(), candidates.size(), 1,
                elapsedMillis(startedAtNanos), summary);
        return new TargetOcrResult(result, acceptedHit, match.editDistance(), match.longestCommonSubstring(),
                normalizedTarget, OcrTextMatcher.normalizeName(best.joinedText()));
    }

    private CandidateResult bestCandidate(List<CandidateResult> candidates) {
        return candidates == null ? null : candidates.stream()
                .max(Comparator.comparingInt(CandidateResult::score))
                .orElse(null);
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
                                                    List<PackedLineMapping> packedLines) {
        if (words == null || words.isEmpty() || packedLines == null || packedLines.isEmpty()) {
            return List.of();
        }
        List<OcrWordResult> mapped = new ArrayList<>();
        for (OcrWordResult word : words) {
            if (word == null) {
                continue;
            }
            PackedLineMapping line = findPackedLine(word, packedLines);
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

    private PackedLineMapping findPackedLine(OcrWordResult word, List<PackedLineMapping> packedLines) {
        int centerY = word.getY();
        int centerX = word.getX();
        for (PackedLineMapping line : packedLines) {
            boolean insideY = centerY >= line.packedY() && centerY <= line.packedY() + line.packedHeight();
            boolean insideX = centerX >= line.packedX() && centerX <= line.packedX() + line.packedWidth();
            if (insideY && insideX) {
                return line;
            }
        }
        return null;
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

    private YellowTargetProfile yellowTargetProfile(String expectedTarget) {
        String normalizedTarget = OcrTextMatcher.normalizeName(expectedTarget);
        return "白龙马".equals(normalizedTarget)
                ? YellowTargetProfile.BAILONGMA
                : YellowTargetProfile.DEFAULT;
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

    private long elapsedMillis(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    private enum YellowTargetProfile {
        DEFAULT,
        BAILONGMA
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
