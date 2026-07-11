package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class ImagePreprocessCloudDecision {
    Status status;
    ImagePreprocessOperation operation;
    CloudDecisionResult cloudResult;
    double confidence;
    String reason;
    String debugToken;
    String washedImagePayloadBase64;
    String washedPayloadMimeType;
    String washedImageSha256;
    int washedWidth;
    int washedHeight;
    @Builder.Default
    Map<String, String> resultValues = Map.of();
    @Builder.Default
    List<CandidateBox> candidateBoxes = List.of();
    @Builder.Default
    List<CandidatePoint> candidatePoints = List.of();

    public boolean isCloudExecuted() {
        return status == Status.CLOUD_EXECUTED;
    }

    public boolean wasCloudAccepted() {
        return status == Status.CLOUD_EXECUTED || status == Status.NO_RESULT;
    }

    public boolean hasUsableResult() {
        return status == Status.CLOUD_EXECUTED && (hasCandidates() || hasWashedImage());
    }

    public boolean hasCandidates() {
        return !candidateBoxes.isEmpty() || !candidatePoints.isEmpty();
    }

    public boolean hasWashedImage() {
        return status == Status.CLOUD_EXECUTED
                && washedImagePayloadBase64 != null
                && !washedImagePayloadBase64.isBlank()
                && "image/png".equals(washedPayloadMimeType)
                && washedImageSha256 != null
                && !washedImageSha256.isBlank()
                && washedWidth > 0
                && washedHeight > 0;
    }

    public boolean isRequiredFailure() {
        return status == Status.REQUIRED_FAILURE;
    }

    public enum Status {
        CLOUD_EXECUTED,
        NO_RESULT,
        REQUIRED_FAILURE,
        DISABLED
    }

    /**
     * Candidate rectangle exposed to callers in window-relative pixels. If cloud returned
     * {@code ROI_RELATIVE}, {@link ImagePreprocessCloudService} adds the request ROI origin before
     * storing the candidate here. Width and height are pixels.
     */
    public record CandidateBox(int x, int y, int width, int height) {
    }

    /**
     * Candidate point exposed to callers in window-relative pixels. If cloud returned
     * {@code ROI_RELATIVE}, {@link ImagePreprocessCloudService} adds the request ROI origin before
     * storing the candidate here.
     */
    public record CandidatePoint(int x, int y) {
    }
}
