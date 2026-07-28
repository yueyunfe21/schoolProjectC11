package com.bot.dhxy.cloud.turn.protocol.observation;

/**
 * TURN-40G: one small interest-selected region-of-interest frame. A request never uploads a whole window frame by
 * default; each ROI is bounded in dimensions and encoded bytes (see the protocol validator).
 *
 * @param roiKey interest-defined key naming what this region is (e.g. a coordinate strip or dialog patch)
 * @param left window-relative left edge in pixels
 * @param top window-relative top edge in pixels
 * @param width region width in pixels (bounded)
 * @param height region height in pixels (bounded)
 * @param pngBytes raw PNG bytes of the region (bounded; base64 on the wire)
 */
public record ObservationRoi(
        String roiKey,
        int left,
        int top,
        int width,
        int height,
        byte[] pngBytes,
        String interestId,
        String intentId,
        String attemptId,
        Integer round) {

    public ObservationRoi(String roiKey, int left, int top, int width, int height, byte[] pngBytes) {
        this(roiKey, left, top, width, height, pngBytes, null, null, null, null);
    }

    public ObservationRoi {
        pngBytes = pngBytes == null ? null : pngBytes.clone();
    }

    @Override
    public byte[] pngBytes() {
        return pngBytes == null ? null : pngBytes.clone();
    }
}
