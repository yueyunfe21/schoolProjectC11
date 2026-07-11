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
