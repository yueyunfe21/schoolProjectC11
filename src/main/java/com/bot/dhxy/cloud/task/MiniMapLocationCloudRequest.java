package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class MiniMapLocationCloudRequest {
    Operation operation;
    /**
     * Base64-encoded raw mini-map coordinate strip or already-cropped map-label image.
     * Local file paths are diagnostic references only.
     */
    String imagePayloadBase64;
    String payloadMimeType;
    String imageSha256;
    String rawImagePath;
    String debugImageId;
    /**
     * Window-relative ROI of the uploaded image. For API calls that operate on an in-memory
     * strip/label without a live window, callers may use 0,0,width,height.
     */
    Roi roi;
    int windowWidth;
    int windowHeight;
    String mapNameHint;
    boolean requiresCoordinate;
    boolean requiresMapName;
    String taskCode;
    String source;
    String phase;
    String windowId;
    String taskRunId;
    String policyVersion;
    String hwnd;

    public enum Operation {
        READ_COORDINATE,
        READ_LOCATION,
        READ_LOCATION_SNAPSHOT,
        EXTRACT_MAP_LABEL,
        NORMALIZE_MAP_LABEL,
        RECOGNIZE_MAP_LABEL_FROM_STRIP,
        RECOGNIZE_MAP_LABEL_IMAGE
    }

    @Value
    @Builder
    public static class Roi {
        int x;
        int y;
        int width;
        int height;
    }
}
