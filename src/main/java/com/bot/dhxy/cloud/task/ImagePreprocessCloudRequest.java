package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ImagePreprocessCloudRequest {
    ImagePreprocessOperation operation;
    /**
     * Base64-encoded raw image bytes sent to the remote image-preprocess service. This is the
     * production transport field; local file paths below are debug references only.
     */
    String imagePayloadBase64;
    /**
     * MIME type for {@link #imagePayloadBase64}, for example {@code image/png}.
     */
    String payloadMimeType;
    /**
     * SHA-256 digest of the decoded image payload, recorded as lowercase hex or an equivalent
     * stable digest string chosen by the caller/cloud contract.
     */
    String imageSha256;
    String rawImagePath;
    String debugImageId;
    /**
     * Optional window-relative crop rectangle in pixels. If present, x/y are measured from the
     * current game-window top-left, and width/height must be positive.
     */
    Roi roi;
    /**
     * Explicit current game-window width in pixels. Must be positive; no silent default is allowed
     * because it is used for safety validation.
     */
    int windowWidth;
    /**
     * Explicit current game-window height in pixels. Must be positive; no silent default is allowed
     * because it is used for safety validation.
     */
    int windowHeight;
    String taskCode;
    String source;
    String phase;
    String windowId;
    String taskRunId;
    String policyVersion;
    String hwnd;
    /**
     * Small operation parameters that do not contain image pixels, for example band selection
     * policy or two fingerprints that cloud should compare. These parameters are transport data
     * only; local runtime must not use them to run image-processing algorithms.
     */
    @Builder.Default
    Map<String, String> parameters = Map.of();

    @Value
    @Builder
    public static class Roi {
        int x;
        int y;
        int width;
        int height;
    }
}
