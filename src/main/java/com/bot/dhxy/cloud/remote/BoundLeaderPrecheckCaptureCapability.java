package com.bot.dhxy.cloud.remote;

import java.awt.image.BufferedImage;

/**
 * W-TEAMRETURN-MECH-LEAF-IMP1/IMP2 (CR TeamReturnService lift-and-shift): the exact-window capture seam
 * for the leader-precheck mechanics. This is a functional capability the trusted {@code
 * LocalRemoteGameCommandHandler} mints <em>after</em> its first {@code requireRegistration ->
 * requireBoundWindow -> requireRegistration} gate; the closure captures the original command/access and
 * generates the screen-absolute corner ROI from the verified {@code access.binding().getX/getY} plus the
 * return-team area configuration, then captures via the handler's existing {@code
 * BoundWindowCaptureService} and re-verifies registration/runRevision/binding-geometry after capture.
 *
 * <p>This dormant leaf only declares the type; it neither injects nor copies {@code
 * RemoteTaskRunRegistry}, {@code MultiWindowTaskManager}, or any binding-refresh authority. The minting
 * (M2) stays in the frozen handler and is a registered owner gate. Because the capability owns the ROI
 * generation from the verified binding, the mechanics no longer reads {@code CoordinateHelper}/tracker.</p>
 *
 * <p>Contract: on success the returned {@link CaptureAttempt} carries exactly one frame plus the exact
 * screen-absolute corner {@code (x1,y1,x2,y2)} it was cropped from; on any capture/post-capture
 * re-verification failure the closure must flush its own frame first and return no frame with a typed
 * reason.</p>
 */
@FunctionalInterface
interface BoundLeaderPrecheckCaptureCapability {

    /**
     * Capture the leader-return signal ROI for the bound window. The capability itself generates the
     * screen-absolute corner rect from the verified binding, so no rect is passed in.
     *
     * @return a captured frame with its exact corner, or a typed failure reason with no frame.
     */
    CaptureAttempt capture();

    /**
     * Closed capture outcome with immutable corner. Exactly one of the two shapes holds: a success carries
     * a non-null {@code frame} plus the screen-absolute corner {@code (x1,y1,x2,y2)} it was cropped from;
     * a failure carries a non-null {@code failureReason} and no frame. The corner integers are immutable
     * value components, so nothing mutable crosses the async analysis boundary.
     */
    record CaptureAttempt(BufferedImage frame, int x1, int y1, int x2, int y2, String failureReason) {

        public CaptureAttempt {
            if (frame != null) {
                // success: a frame, no reason, and a positive-area corner the mechanics can key hits on
                if (failureReason != null) {
                    throw new IllegalArgumentException("a captured attempt must not carry a failure reason");
                }
                if (x2 <= x1 || y2 <= y1) {
                    throw new IllegalArgumentException(
                            "a captured attempt requires a positive-area corner (x2 > x1 and y2 > y1)");
                }
            } else {
                // failure: no frame, a non-blank reason, and zero corners
                if (failureReason == null || failureReason.isBlank()) {
                    throw new IllegalArgumentException("a failed attempt requires a non-blank failure reason");
                }
                if (x1 != 0 || y1 != 0 || x2 != 0 || y2 != 0) {
                    throw new IllegalArgumentException("a failed attempt must carry zero corners");
                }
            }
        }

        static CaptureAttempt captured(BufferedImage frame, int x1, int y1, int x2, int y2) {
            return new CaptureAttempt(frame, x1, y1, x2, y2, null);
        }

        static CaptureAttempt failed(String failureReason) {
            return new CaptureAttempt(null, 0, 0, 0, 0, failureReason);
        }
    }
}
