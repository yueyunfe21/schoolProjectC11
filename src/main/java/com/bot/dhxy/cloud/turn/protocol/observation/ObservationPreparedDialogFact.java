package com.bot.dhxy.cloud.turn.protocol.observation;

/**
 * A Client-prepared dialog candidate carried on the observation plane.
 *
 * <p>All geometry is window-relative. Cloud validates the exact interest and catalog identity,
 * restores screen coordinates from the run's immutable window rectangle, and publishes the
 * existing PreparedDialogAction. This fact grants no input authority.</p>
 */
public record ObservationPreparedDialogFact(
        String taskRunId,
        String windowId,
        String hwnd,
        String interestId,
        String taskCode,
        String operation,
        String actionKey,
        String templatePath,
        int matchLeft,
        int matchTop,
        int matchRight,
        int matchBottom,
        int clickX,
        int clickY,
        int validationLeft,
        int validationTop,
        int validationRight,
        int validationBottom,
        String washMode,
        String fingerprint,
        boolean clickRequired,
        long preparedAtMs,
        long capturedAtMs,
        long observerSeq,
        String source) {
}
