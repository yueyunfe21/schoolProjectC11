package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class SummonSkillCloudRequest {
    /**
     * Base64-encoded raw tooltip/slot ROI image bytes sent to the remote summon-skill service.
     * Local file paths are diagnostic references only and must not be used as production authority.
     */
    String imagePayloadBase64;
    String payloadMimeType;
    String imageSha256;
    String rawImagePath;
    String debugImageId;
    /**
     * Window-relative raw tooltip/slot crop in pixels.
     */
    Roi roi;
    int windowWidth;
    int windowHeight;
    Integer slotIndex;
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
