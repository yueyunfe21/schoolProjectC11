package com.bot.dhxy.cloud.turn.protocol.observation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * TURN-40G: one Cloud-issued observation interest. The response carries the complete current interest set for the
 * window under its {@code interestRevision}; an empty set parks the runner (no fixed-rate full polling).
 *
 * <p>An interest may carry a window-relative ROI rectangle: the runner then captures exactly that small region each
 * period and uploads it for Cloud-side recognition, so recognition pixels, thresholds and algorithms stay on the
 * Cloud unchanged. An interest without geometry names a local mechanical duty (e.g. a timer edge).
 *
 * @param interestKey what to sample (interest-defined key)
 * @param samplePeriodMs positive sampling period for this interest
 * @param detail optional Cloud-side detail for the runner (never an execution grant)
 * @param roiLeft optional window-relative ROI left edge (all four ROI fields present together)
 * @param roiTop optional window-relative ROI top edge
 * @param roiWidth optional ROI width (bounded by the protocol validator)
 * @param roiHeight optional ROI height (bounded by the protocol validator)
 */
public record ObservationInterest(
        String interestKey,
        long samplePeriodMs,
        @JsonInclude(JsonInclude.Include.NON_NULL) String detail,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer roiLeft,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer roiTop,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer roiWidth,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer roiHeight) {

    /** Backward-compatible ROI interest constructor. */
    public ObservationInterest(String interestKey, long samplePeriodMs, String detail,
                               Integer roiLeft, Integer roiTop, Integer roiWidth, Integer roiHeight) {
        this.interestKey = interestKey;
        this.samplePeriodMs = samplePeriodMs;
        this.detail = detail;
        this.roiLeft = roiLeft;
        this.roiTop = roiTop;
        this.roiWidth = roiWidth;
        this.roiHeight = roiHeight;
    }

    /** Convenience for a geometry-free local-duty interest. */
    public ObservationInterest(String interestKey, long samplePeriodMs, String detail) {
        this(interestKey, samplePeriodMs, detail, null, null, null, null);
    }

    /** Returns whether this interest carries a complete ROI rectangle. */
    public boolean hasRoi() {
        return roiLeft != null && roiTop != null && roiWidth != null && roiHeight != null;
    }
}
