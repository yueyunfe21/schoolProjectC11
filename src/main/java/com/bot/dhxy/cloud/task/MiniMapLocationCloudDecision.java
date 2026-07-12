package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.model.MapCoordinate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MiniMapLocationCloudDecision {
    @Builder.Default
    Status status = Status.REQUIRED_FAILURE;
    MapCoordinate coordinate;
    String mapName;
    @Builder.Default
    double score = 0.0d;
    String reason;
    String debugToken;
    String labelPath;
    String labelImagePayloadBase64;
    String labelPayloadMimeType;
    String labelImageSha256;
    int labelWidth;
    int labelHeight;
    /**
     * CR258: cloud-side OCR-fallback rejection detail on a NO_RESULT decision, e.g.
     * {@code coordinate-out-of-transform-bounds}. Drives the client's metadata-only failure-sample
     * archive now that the plausibility guard lives inside the cloud READ_LOCATION fallback.
     */
    String ocrFallbackReason;
    /** CR258: rejected {@code map,x,y} echoed by the cloud for the failure-sample archive. */
    String ocrRejectedLocation;
    @Builder.Default
    double confidence = 0.0d;
    CloudDecisionResult cloudResult;

    public boolean isCloudExecuted() {
        return status == Status.CLOUD_EXECUTED;
    }

    public boolean isRequiredFailure() {
        return status == Status.REQUIRED_FAILURE;
    }

    public boolean hasLabelPayload() {
        return isCloudExecuted()
                && labelImagePayloadBase64 != null
                && !labelImagePayloadBase64.isBlank()
                && "image/png".equals(labelPayloadMimeType)
                && labelImageSha256 != null
                && !labelImageSha256.isBlank()
                && labelWidth > 0
                && labelHeight > 0;
    }

    public enum Status {
        CLOUD_EXECUTED,
        CLOUD_NO_RESULT,
        DISABLED,
        REQUIRED_FAILURE
    }
}
