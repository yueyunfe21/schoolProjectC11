package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class SheyaoxiangStatusCloudRequest {

    Hook hook;
    Outcome outcome;
    String imagePayloadBase64;
    String payloadMimeType;
    String imageSha256;
    String rawImagePath;
    Roi windowRelativeRoi;
    Roi screenAbsoluteRoi;
    int windowWidth;
    int windowHeight;
    long nowMs;
    long lastIncenseUsedTimeMs;
    long nextIncenseRetryTimeMs;
    int incenseIconOffsetX;
    int incenseIconOffsetY;
    boolean openMainBagSession;
    String taskCode;
    String source;
    String phase;
    String windowId;
    String taskRunId;
    String policyVersion;
    String hwnd;
    String decisionId;
    String reason;

    public enum Hook {
        TICK,
        STATUS_IMAGE,
        OUTCOME
    }

    public enum Outcome {
        USED,
        ITEM_NOT_FOUND,
        STOPPED,
        FAILED
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
