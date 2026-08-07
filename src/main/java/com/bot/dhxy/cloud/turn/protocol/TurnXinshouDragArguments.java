package com.bot.dhxy.cloud.turn.protocol;

/**
 * 新手 §8.2 hold-sweep arguments for one 轮回 segment.
 *
 * <p>The sweep is a single local operation that presses the left button, walks the row pattern and
 * returns WITHOUT releasing, so the caller can read the progress counter mid-hold. Every coordinate
 * is window-relative; the client resolves them against the exact bound window. The progress ROI is
 * captured after the sweep and returned as the operation frame so Cloud can OCR it.</p>
 *
 * @param segment 1-based segment index, only for diagnostics
 * @param startX sweep start, window-relative
 * @param startY sweep start, window-relative
 * @param leftX row left bound, window-relative
 * @param rightX row right bound, window-relative
 * @param endY last row Y, window-relative
 * @param rowStepPx vertical step between rows
 * @param progressRoiX progress counter ROI, window-relative
 * @param progressRoiY progress counter ROI, window-relative
 * @param progressRoiWidth progress counter ROI width
 * @param progressRoiHeight progress counter ROI height
 */
public record TurnXinshouDragArguments(
        int segment,
        int startX,
        int startY,
        int leftX,
        int rightX,
        int endY,
        int rowStepPx,
        int progressRoiX,
        int progressRoiY,
        int progressRoiWidth,
        int progressRoiHeight) {
}
