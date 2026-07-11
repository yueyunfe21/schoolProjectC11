package com.bot.dhxy.cloud.task;

import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CloudImageProcessor implements ImageProcessorService {

    private final ImagePreprocessWashedImageClient imagePreprocessWashedImageClient;

    public CloudImageProcessor(ImagePreprocessWashedImageClient imagePreprocessWashedImageClient) {
        this.imagePreprocessWashedImageClient = imagePreprocessWashedImageClient;
    }

    @Override
    public ImageProcessorResult washYellowText(
            BufferedImage raw,
            RequestMetadata metadata) {
        return requireCloudOutput(wash(raw, ImagePreprocessOperation.WASH_YELLOW, metadata),
                "missing required washed image");
    }

    @Override
    public ImageProcessorResult washGreenTextToBlackAndWhite(
            BufferedImage raw,
            RequestMetadata metadata) {
        return requireCloudOutput(wash(raw, ImagePreprocessOperation.WASH_GREEN, metadata),
                "missing required washed image");
    }

    @Override
    public ImageProcessorResult washPurpleTextToBlackAndWhite(
            BufferedImage raw,
            RequestMetadata metadata) {
        return requireCloudOutput(wash(raw, ImagePreprocessOperation.WASH_PURPLE, metadata),
                "missing required washed image");
    }

    @Override
    public ImageProcessorResult washThinWhiteTextToBlackAndWhite(
            BufferedImage raw,
            RequestMetadata metadata) {
        return requireCloudOutput(wash(raw, ImagePreprocessOperation.WASH_WHITE, metadata),
                "missing required washed image");
    }

    @Override
    public ImageProcessorResult washDialogOptionTemplateTextToBlackAndWhite(
            BufferedImage raw,
            RequestMetadata metadata) {
        return requireCloudOutput(wash(raw, ImagePreprocessOperation.WASH_DIALOG_OPTION_TEMPLATE, metadata),
                "missing required washed image");
    }

    @Override
    public ImageProcessorResult washAutoCombatRoundRedDigits(
            BufferedImage raw,
            RequestMetadata metadata) {
        return requireCloudOutput(wash(raw, ImagePreprocessOperation.WASH_AUTO_COMBAT_ROUND_RED_DIGITS, metadata),
                "missing required auto-combat red digit washed image/pixelCount");
    }

    @Override
    public ImageProcessorResult routePackedLineMask(
            BufferedImage raw,
            RequestMetadata metadata) {
        return requireCloudOutput(wash(raw, ImagePreprocessOperation.ROUTE_PACKED_LINE_MASK, metadata),
                "missing required route packed image/mapping");
    }

    @Override
    public ImageProcessorResult routeDestinationSegments(
            BufferedImage raw,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud = imagePreprocessWashedImageClient.wash(
                raw,
                ImagePreprocessOperation.ROUTE_DESTINATION_SEGMENTS,
                clientMetadata(metadata));
        return requireCloudOutput(result(
                ImagePreprocessOperation.ROUTE_DESTINATION_SEGMENTS,
                null,
                cloud.status(),
                cloud.reason(),
                cloud.decision()),
                "missing required route destination segment point");
    }

    @Override
    public ImageProcessorResult washToPath(
            Path rawPath,
            Path outputPath,
            ImagePreprocessOperation operation,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult result =
                imagePreprocessWashedImageClient.washToPath(rawPath, outputPath, operation, clientMetadata(metadata));
        return requireCloudOutput(
                result(operation, result.image(), result.status(), result.reason(), result.decision()),
                "missing required washed image");
    }

    @Override
    public ImageProcessorResult countYellowPixels(
            BufferedImage raw,
            RequestMetadata metadata) {
        return requireCloudOutput(pixelCount(raw, ImagePreprocessOperation.COUNT_YELLOW_PIXELS, metadata),
                "missing required pixelCount");
    }

    @Override
    public ImageProcessorResult countGreenPixelsHSV(
            BufferedImage raw,
            RequestMetadata metadata) {
        return requireCloudOutput(pixelCount(raw, ImagePreprocessOperation.COUNT_GREEN_PIXELS_HSV, metadata),
                "missing required pixelCount");
    }

    @Override
    public ImageProcessorResult countThinWhitePixelsHSV(
            BufferedImage raw,
            RequestMetadata metadata) {
        return requireCloudOutput(pixelCount(raw, ImagePreprocessOperation.COUNT_THIN_WHITE_PIXELS_HSV, metadata),
                "missing required pixelCount");
    }

    @Override
    public ImageProcessorResult findGreenTextBands(
            BufferedImage raw,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud =
                imagePreprocessWashedImageClient.wash(raw, ImagePreprocessOperation.FIND_GREEN_TEXT_BANDS,
                        clientMetadata(metadata));
        return requireCloudOutput(result(
                ImagePreprocessOperation.FIND_GREEN_TEXT_BANDS,
                null,
                cloud.status(),
                cloud.reason(),
                cloud.decision()),
                "missing required candidateBoxes");
    }

    @Override
    public ImageProcessorResult pickGreenTextBand(
            BufferedImage raw,
            boolean first,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud = imagePreprocessWashedImageClient.wash(
                raw,
                ImagePreprocessOperation.PICK_GREEN_TEXT_BAND,
                clientMetadata(withParameters(metadata, Map.of("first", Boolean.toString(first)))));
        return requireCloudOutput(result(
                ImagePreprocessOperation.PICK_GREEN_TEXT_BAND,
                null,
                cloud.status(),
                cloud.reason(),
                cloud.decision()),
                "missing required selected green text band");
    }

    @Override
    public ImageProcessorResult buildBinaryFingerprint(
            BufferedImage raw,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud = imagePreprocessWashedImageClient.wash(
                raw,
                ImagePreprocessOperation.BUILD_BINARY_FINGERPRINT,
                clientMetadata(metadata));
        return requireCloudOutput(result(
                ImagePreprocessOperation.BUILD_BINARY_FINGERPRINT,
                null,
                cloud.status(),
                cloud.reason(),
                cloud.decision()),
                "missing required binaryFingerprint");
    }

    @Override
    public ImageProcessorResult binaryFingerprintDistance(
            String left,
            String right,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud = imagePreprocessWashedImageClient.wash(
                transportImage(),
                ImagePreprocessOperation.BINARY_FINGERPRINT_DISTANCE,
                clientMetadata(withParameters(metadata, Map.of(
                        "leftFingerprint", left == null ? "" : left,
                        "rightFingerprint", right == null ? "" : right))));
        return requireCloudOutput(result(
                ImagePreprocessOperation.BINARY_FINGERPRINT_DISTANCE,
                null,
                cloud.status(),
                cloud.reason(),
                cloud.decision()),
                "missing required fingerprintDistance");
    }

    @Override
    public ImageProcessorResult detectThinWhiteTextLinePattern(
            BufferedImage raw,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud = imagePreprocessWashedImageClient.wash(
                raw,
                ImagePreprocessOperation.DETECT_THIN_WHITE_TEXT_LINE_PATTERN,
                clientMetadata(metadata));
        return requireCloudOutput(result(
                ImagePreprocessOperation.DETECT_THIN_WHITE_TEXT_LINE_PATTERN,
                null,
                cloud.status(),
                cloud.reason(),
                cloud.decision()),
                "missing required text line pattern stats");
    }

    @Override
    public ImageProcessorResult measureStddev(
            BufferedImage raw,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud = imagePreprocessWashedImageClient.wash(
                raw,
                ImagePreprocessOperation.MEASURE_STDDEV,
                clientMetadata(metadata));
        return requireCloudOutput(result(
                ImagePreprocessOperation.MEASURE_STDDEV,
                null,
                cloud.status(),
                cloud.reason(),
                cloud.decision()),
                "missing required stddev");
    }

    @Override
    public ImageProcessorResult findTextCandidates(
            BufferedImage raw,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud = imagePreprocessWashedImageClient.wash(
                raw,
                ImagePreprocessOperation.TEXT_CANDIDATES,
                clientMetadata(metadata));
        return requireCloudOutput(result(
                ImagePreprocessOperation.TEXT_CANDIDATES,
                cloud.image(),
                cloud.status(),
                cloud.reason(),
                cloud.decision()),
                "missing required text candidates");
    }

    @Override
    public ImageProcessorResult measureTeamTooltipText(
            BufferedImage raw,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud = imagePreprocessWashedImageClient.wash(
                raw,
                ImagePreprocessOperation.MEASURE_TEAM_TOOLTIP_TEXT,
                clientMetadata(metadata));
        return requireCloudOutput(result(
                ImagePreprocessOperation.MEASURE_TEAM_TOOLTIP_TEXT,
                null,
                cloud.status(),
                cloud.reason(),
                cloud.decision()),
                "missing required team tooltip text stats");
    }

    private ImageProcessorResult wash(
            BufferedImage raw,
            ImagePreprocessOperation operation,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud =
                imagePreprocessWashedImageClient.wash(raw, operation, clientMetadata(metadata));
        return result(operation, cloud.image(), cloud.status(), cloud.reason(), cloud.decision());
    }

    private ImageProcessorResult pixelCount(
            BufferedImage raw,
            ImagePreprocessOperation operation,
            RequestMetadata metadata) {
        ImagePreprocessWashedImageClient.WashedImageResult cloud =
                imagePreprocessWashedImageClient.wash(raw, operation, clientMetadata(metadata));
        return result(operation, null, cloud.status(), cloud.reason(), cloud.decision());
    }

    private ImageProcessorResult result(
            ImagePreprocessOperation operation,
            BufferedImage image,
            ImagePreprocessCloudDecision.Status status,
            String reason,
            ImagePreprocessCloudDecision decision) {
        return new ImageProcessorResult(
                status,
                operation,
                image,
                parseInteger(value(decision, "pixelCount")),
                greenTextBands(decision),
                selectedGreenTextBand(decision),
                value(decision, "binaryFingerprint"),
                parseInteger(value(decision, "fingerprintDistance")),
                parseDouble(value(decision, "stddev")),
                textLinePatternStats(decision),
                textCandidates(decision),
                packedLineMappings(decision),
                teamTooltipTextStats(decision),
                reason,
                decision);
    }

    private static RequestMetadata withParameters(
            RequestMetadata metadata,
            Map<String, String> parameters) {
        RequestMetadata safe = metadata == null
                ? RequestMetadata.builder().build()
                : metadata;
        Map<String, String> merged = new LinkedHashMap<>();
        if (safe.getParameters() != null) {
            merged.putAll(safe.getParameters());
        }
        merged.putAll(parameters);
        return safe.toBuilder().parameters(Map.copyOf(merged)).build();
    }

    private static ImagePreprocessWashedImageClient.RequestMetadata clientMetadata(RequestMetadata metadata) {
        RequestMetadata safe = metadata == null ? RequestMetadata.builder().build() : metadata;
        return ImagePreprocessWashedImageClient.RequestMetadata.builder()
                .rawImagePath(safe.getRawImagePath())
                .debugImageId(safe.getDebugImageId())
                .source(safe.getSource())
                .taskCode(safe.getTaskCode())
                .phase(safe.getPhase())
                .windowId(safe.getWindowId())
                .taskRunId(safe.getTaskRunId())
                .policyVersion(safe.getPolicyVersion())
                .hwnd(safe.getHwnd())
                .parameters(safe.getParameters())
                .build();
    }

    private static BufferedImage transportImage() {
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }

    private static List<GreenTextBand> greenTextBands(ImagePreprocessCloudDecision decision) {
        if (decision == null || decision.getCandidateBoxes() == null || decision.getCandidateBoxes().isEmpty()) {
            return List.of();
        }
        return decision.getCandidateBoxes().stream()
                .map(box -> new GreenTextBand(box.x(), box.y(), box.width(), box.height(), 0))
                .toList();
    }

    private static GreenTextBand selectedGreenTextBand(ImagePreprocessCloudDecision decision) {
        List<GreenTextBand> bands = greenTextBands(decision);
        return bands.isEmpty() ? null : bands.get(0);
    }

    private static TextLinePatternStats textLinePatternStats(ImagePreprocessCloudDecision decision) {
        String matched = value(decision, "matched");
        String qualifyingRows = value(decision, "qualifyingRows");
        String maxWhitePixelsInRow = value(decision, "maxWhitePixelsInRow");
        String maxClustersInRow = value(decision, "maxClustersInRow");
        String maxSpanInRow = value(decision, "maxSpanInRow");
        if (matched == null
                || qualifyingRows == null
                || maxWhitePixelsInRow == null
                || maxClustersInRow == null
                || maxSpanInRow == null) {
            return null;
        }
        Boolean matchedValue = parseBoolean(matched);
        Integer qualifyingRowsValue = parseInteger(qualifyingRows);
        Integer maxWhitePixelsValue = parseInteger(maxWhitePixelsInRow);
        Integer maxClustersValue = parseInteger(maxClustersInRow);
        Integer maxSpanValue = parseInteger(maxSpanInRow);
        if (matchedValue == null
                || qualifyingRowsValue == null
                || maxWhitePixelsValue == null
                || maxClustersValue == null
                || maxSpanValue == null) {
            return null;
        }
        return new TextLinePatternStats(
                matchedValue,
                qualifyingRowsValue,
                maxWhitePixelsValue,
                maxClustersValue,
                maxSpanValue);
    }

    private static List<TextCandidateBox> textCandidates(ImagePreprocessCloudDecision decision) {
        if (decision == null || decision.getCandidateBoxes() == null || decision.getCandidateBoxes().isEmpty()) {
            return List.of();
        }
        List<Integer> scores = integerList(value(decision, "candidateScores"));
        List<Integer> pixelCounts = integerList(value(decision, "candidatePixelCounts"));
        List<Integer> componentCounts = integerList(value(decision, "candidateComponentCounts"));
        List<Double> densities = doubleList(value(decision, "candidateDensities"));
        List<Integer> longRowCounts = integerList(value(decision, "candidateLongRowCounts"));
        List<Integer> longColumnCounts = integerList(value(decision, "candidateLongColumnCounts"));
        List<String> reasons = stringList(value(decision, "candidateReasons"));
        List<ImagePreprocessCloudDecision.CandidatePoint> points = decision.getCandidatePoints() == null
                ? List.of()
                : decision.getCandidatePoints();
        List<TextCandidateBox> result = new java.util.ArrayList<>();
        for (int i = 0; i < decision.getCandidateBoxes().size(); i++) {
            ImagePreprocessCloudDecision.CandidateBox box = decision.getCandidateBoxes().get(i);
            ImagePreprocessCloudDecision.CandidatePoint point = i < points.size() ? points.get(i) : null;
            result.add(new TextCandidateBox(
                    box.x(),
                    box.y(),
                    box.width(),
                    box.height(),
                    point == null ? null : point.x(),
                    point == null ? null : point.y(),
                    intAt(scores, i, Math.max(1, decision.getCandidateBoxes().size() - i)),
                    intAt(pixelCounts, i, 0),
                    intAt(componentCounts, i, 0),
                    doubleAt(densities, i, 0.0d),
                    intAt(longRowCounts, i, 0),
                    intAt(longColumnCounts, i, 0),
                    stringAt(reasons, i, "cloud-candidate")));
        }
        return List.copyOf(result);
    }

    private static List<PackedLineMapping> packedLineMappings(ImagePreprocessCloudDecision decision) {
        List<Integer> values = integerList(value(decision, "packedLineMappings"));
        if (values.isEmpty() || values.size() % 8 != 0) {
            return List.of();
        }
        List<PackedLineMapping> result = new java.util.ArrayList<>();
        for (int i = 0; i < values.size(); i += 8) {
            int sourceWidth = values.get(i + 2);
            int sourceHeight = values.get(i + 3);
            int packedWidth = values.get(i + 6);
            int packedHeight = values.get(i + 7);
            if (sourceWidth <= 0 || sourceHeight <= 0 || packedWidth <= 0 || packedHeight <= 0) {
                return List.of();
            }
            result.add(new PackedLineMapping(
                    values.get(i),
                    values.get(i + 1),
                    sourceWidth,
                    sourceHeight,
                    values.get(i + 4),
                    values.get(i + 5),
                    packedWidth,
                    packedHeight));
        }
        return List.copyOf(result);
    }

    private static TeamTooltipTextStats teamTooltipTextStats(ImagePreprocessCloudDecision decision) {
        Integer whitePixels = parseInteger(value(decision, "whitePixels"));
        Integer purplePixels = parseInteger(value(decision, "purplePixels"));
        Integer rows = parseInteger(value(decision, "rows"));
        Integer columns = parseInteger(value(decision, "columns"));
        Integer transitions = parseInteger(value(decision, "transitions"));
        Integer maxRowPixels = parseInteger(value(decision, "maxRowPixels"));
        if (whitePixels == null
                || purplePixels == null
                || rows == null
                || columns == null
                || transitions == null
                || maxRowPixels == null) {
            return null;
        }
        return new TeamTooltipTextStats(
                whitePixels,
                purplePixels,
                rows,
                columns,
                transitions,
                maxRowPixels);
    }

    private static ImageProcessorResult requireCloudOutput(ImageProcessorResult result, String missingReason) {
        if (result == null) {
            return new ImageProcessorResult(
                    ImagePreprocessCloudDecision.Status.REQUIRED_FAILURE,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null,
                    missingReason,
                    null);
        }
        if (result.status() != ImagePreprocessCloudDecision.Status.CLOUD_EXECUTED || result.hasRequiredOutput()) {
            return result;
        }
        return new ImageProcessorResult(
                ImagePreprocessCloudDecision.Status.REQUIRED_FAILURE,
                result.operation(),
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                appendReason(result.reason(), missingReason),
                result.decision());
    }

    private static String value(ImagePreprocessCloudDecision decision, String key) {
        if (decision == null || decision.getResultValues() == null) {
            return null;
        }
        String value = decision.getResultValues().get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<Integer> integerList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Integer> result = new java.util.ArrayList<>();
        for (String item : value.split("\\|", -1)) {
            Integer parsed = parseInteger(item);
            result.add(parsed == null ? 0 : parsed);
        }
        return List.copyOf(result);
    }

    private static List<Double> doubleList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Double> result = new java.util.ArrayList<>();
        for (String item : value.split("\\|", -1)) {
            Double parsed = parseDouble(item);
            result.add(parsed == null ? 0.0d : parsed);
        }
        return List.copyOf(result);
    }

    private static List<String> stringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\|", -1));
    }

    private static int intAt(List<Integer> values, int index, int fallback) {
        return values == null || index < 0 || index >= values.size() ? fallback : values.get(index);
    }

    private static double doubleAt(List<Double> values, int index, double fallback) {
        return values == null || index < 0 || index >= values.size() ? fallback : values.get(index);
    }

    private static String stringAt(List<String> values, int index, String fallback) {
        if (values == null || index < 0 || index >= values.size()) {
            return fallback;
        }
        String value = values.get(index);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDouble(String value) {
        try {
            return value == null ? null : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return null;
    }

    private static String appendReason(String reason, String missingReason) {
        if (reason == null || reason.isBlank()) {
            return missingReason;
        }
        return reason + "; " + missingReason;
    }
}
