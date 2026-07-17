package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @param region screen-absolute unscaled ROI, or null for the full bound window
 * @param resultMode closed capture result mode
 * @param clearPointerIfOverRegion optional mechanical pointer-clear policy; null performs no pointer read or input
 * @param pixelChangeProbe optional exact-window Ctrl-hover pixel-change mechanics; null preserves legacy capture
 */
public record TurnCaptureSpec(
        TurnRegion region,
        ResultMode resultMode,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ClearPointerIfOverRegion clearPointerIfOverRegion,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        PixelChangeProbe pixelChangeProbe) {

    public TurnCaptureSpec(TurnRegion region, ResultMode resultMode) {
        this(region, resultMode, null, null);
    }

    public TurnCaptureSpec(
            TurnRegion region,
            ResultMode resultMode,
            ClearPointerIfOverRegion clearPointerIfOverRegion) {
        this(region, resultMode, clearPointerIfOverRegion, null);
    }

    public enum ResultMode {
        UPLOAD_IMAGE,
        NO_IMAGE
    }

    /**
     * @param paddingPx inclusive ROI padding in unscaled screen pixels
     * @param targetX exact screen-absolute unscaled mouse target X
     * @param targetY exact screen-absolute unscaled mouse target Y
     * @param settleMs queue-owned wait after the mouse move, in milliseconds
     */
    public record ClearPointerIfOverRegion(
            @JsonProperty(value = "paddingPx", required = true) int paddingPx,
            @JsonProperty(value = "targetX", required = true) int targetX,
            @JsonProperty(value = "targetY", required = true) int targetY,
            @JsonProperty(value = "settleMs", required = true) int settleMs) {
    }

    /**
     * @param targetX exact screen-absolute unscaled mouse target X inside the requested ROI
     * @param targetY exact screen-absolute unscaled mouse target Y inside the requested ROI
     * @param ctrlDownSettleMs queue-owned wait after exact-HWND Ctrl DOWN, in milliseconds
     * @param afterMoveSettleMs queue-owned wait after the foreground mouse move, in milliseconds
     * @param ctrlUpSettleMs queue-owned cleanup wait after exact-HWND Ctrl UP, in milliseconds
     * @param differenceRatioThreshold finite unchanged-pixel ratio threshold in [0.0, 1.0]
     */
    public record PixelChangeProbe(
            @JsonProperty(value = "targetX", required = true) int targetX,
            @JsonProperty(value = "targetY", required = true) int targetY,
            @JsonProperty(value = "ctrlDownSettleMs", required = true) int ctrlDownSettleMs,
            @JsonProperty(value = "afterMoveSettleMs", required = true) int afterMoveSettleMs,
            @JsonProperty(value = "ctrlUpSettleMs", required = true) int ctrlUpSettleMs,
            @JsonProperty(value = "differenceRatioThreshold", required = true)
            double differenceRatioThreshold) {
    }
}
