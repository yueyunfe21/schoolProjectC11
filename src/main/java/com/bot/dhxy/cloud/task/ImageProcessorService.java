package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ImageProcessorService {

    ImageProcessorResult washYellowText(BufferedImage raw, RequestMetadata metadata);

    ImageProcessorResult washGreenTextToBlackAndWhite(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult washPurpleTextToBlackAndWhite(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult washThinWhiteTextToBlackAndWhite(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult washDialogOptionTemplateTextToBlackAndWhite(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult washAutoCombatRoundRedDigits(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult routePackedLineMask(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult routeDestinationSegments(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult washToPath(
            Path rawPath,
            Path outputPath,
            ImagePreprocessOperation operation,
            RequestMetadata metadata);

    ImageProcessorResult countYellowPixels(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult countGreenPixelsHSV(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult countThinWhitePixelsHSV(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult findGreenTextBands(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult pickGreenTextBand(
            BufferedImage raw,
            boolean first,
            RequestMetadata metadata);

    ImageProcessorResult buildBinaryFingerprint(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult binaryFingerprintDistance(
            String left,
            String right,
            RequestMetadata metadata);

    ImageProcessorResult detectThinWhiteTextLinePattern(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult measureStddev(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult findTextCandidates(
            BufferedImage raw,
            RequestMetadata metadata);

    ImageProcessorResult measureTeamTooltipText(
            BufferedImage raw,
            RequestMetadata metadata);

    @Value
    @Builder(toBuilder = true)
    class RequestMetadata {
        String rawImagePath;
        String debugImageId;
        String source;
        String taskCode;
        String phase;
        String windowId;
        String taskRunId;
        String policyVersion;
        String hwnd;
        @Builder.Default
        Map<String, String> parameters = Map.of();
    }

    record ImageProcessorResult(
            ImagePreprocessCloudDecision.Status status,
            ImagePreprocessOperation operation,
            BufferedImage image,
            Integer pixelCount,
            List<GreenTextBand> greenTextBands,
            GreenTextBand selectedGreenTextBand,
            String binaryFingerprint,
            Integer binaryFingerprintDistance,
            Double stddev,
            TextLinePatternStats textLinePatternStats,
            List<TextCandidateBox> textCandidates,
            List<PackedLineMapping> packedLineMappings,
            TeamTooltipTextStats teamTooltipTextStats,
            String reason,
            ImagePreprocessCloudDecision decision) {

        public ImageProcessorResult {
            greenTextBands = greenTextBands == null ? List.of() : List.copyOf(greenTextBands);
            textCandidates = textCandidates == null ? List.of() : List.copyOf(textCandidates);
            packedLineMappings = packedLineMappings == null ? List.of() : List.copyOf(packedLineMappings);
        }

        public boolean hasImage() {
            return image != null;
        }

        public boolean hasPixelCount() {
            return pixelCount != null;
        }

        public boolean hasBinaryFingerprint() {
            return binaryFingerprint != null && !binaryFingerprint.isBlank();
        }

        public boolean hasRequiredOutput() {
            if (status != ImagePreprocessCloudDecision.Status.CLOUD_EXECUTED || operation == null) {
                return false;
            }
            return switch (operation) {
                case WASH_YELLOW, WASH_GREEN, WASH_WHITE, WASH_PURPLE, WASH_DIALOG_OPTION_TEMPLATE -> hasImage();
                case WASH_AUTO_COMBAT_ROUND_RED_DIGITS -> hasImage() && pixelCount != null;
                case ROUTE_PACKED_LINE_MASK -> hasImage() && !packedLineMappings.isEmpty();
                case ROUTE_DESTINATION_SEGMENTS -> !textCandidates.isEmpty()
                        || decision != null
                        && decision.getCandidatePoints() != null
                        && !decision.getCandidatePoints().isEmpty();
                case COUNT_YELLOW_PIXELS, COUNT_GREEN_PIXELS_HSV, COUNT_THIN_WHITE_PIXELS_HSV -> pixelCount != null;
                case FIND_GREEN_TEXT_BANDS -> !greenTextBands.isEmpty();
                case PICK_GREEN_TEXT_BAND -> selectedGreenTextBand != null;
                case BUILD_BINARY_FINGERPRINT -> hasBinaryFingerprint();
                case BINARY_FINGERPRINT_DISTANCE -> binaryFingerprintDistance != null;
                case DETECT_THIN_WHITE_TEXT_LINE_PATTERN -> textLinePatternStats != null;
                case MEASURE_STDDEV -> stddev != null;
                case TEXT_CANDIDATES -> !textCandidates.isEmpty();
                case MEASURE_TEAM_TOOLTIP_TEXT -> teamTooltipTextStats != null;
                case FINGERPRINT -> false;
                default -> false;
            };
        }
    }

    record GreenTextBand(int x, int y, int width, int height, int pixels) {
    }

    record TextLinePatternStats(
            boolean matched,
            int qualifyingRows,
            int maxWhitePixelsInRow,
            int maxClustersInRow,
            int maxSpanInRow) {
    }

    record TextCandidateBox(
            int x,
            int y,
            int width,
            int height,
            Integer clickX,
            Integer clickY,
            int score,
            int pixelCount,
            int componentCount,
            double density,
            int longRowCount,
            int longColumnCount,
            String reason) {
    }

    record PackedLineMapping(
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight,
            int packedX,
            int packedY,
            int packedWidth,
            int packedHeight) {
    }

    record TeamTooltipTextStats(
            int whitePixels,
            int purplePixels,
            int rows,
            int columns,
            int transitions,
            int maxRowPixels) {
    }
}
