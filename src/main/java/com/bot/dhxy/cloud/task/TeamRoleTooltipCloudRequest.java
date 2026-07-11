package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class TeamRoleTooltipCloudRequest {
    /**
     * Base64-encoded masked hover tooltip PNG bytes sent to cloud business vision.
     * Local paths are diagnostic only and are not production authority.
     */
    String imagePayloadBase64;
    String payloadMimeType;
    String imageSha256;
    String rawImagePath;
    String debugImageId;
    /**
     * Window-relative tooltip crop in pixels.
     */
    Roi roi;
    int windowWidth;
    int windowHeight;
    String currentPlayerId;
    String taskCode;
    String source;
    String phase;
    String windowId;
    String taskRunId;
    String policyVersion;
    String hwnd;

    @Value
    @Builder
    public static class Roi {
        int x;
        int y;
        int width;
        int height;
    }
}
