package com.bot.dhxy.input.action;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable terminal snapshot of one serialized input request.
 *
 * <p>Step indexes are zero-based and refer to the ordered {@link InputAction} list. A value of
 * {@code -1} means that no step reached that progress boundary. {@code startedStepIndex} is fixed
 * to the first step that actually started, while {@code lastCompletedStepIndex} advances as an
 * ordered prefix completes. {@link Status#STARTED_UNKNOWN} means the current or last-started step
 * cannot be proven complete; an earlier completed prefix may still exist.</p>
 */
@Value
@Builder
public class InputActionExecutionResult {

    String requestId;
    boolean started;
    int startedStepIndex;
    int lastCompletedStepIndex;
    Status status;
    InputActionSafetyReason safetyReason;
    String reason;

    /** @return true only when the worker normally completed the entire request. */
    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    /** Terminal execution classification for a queued input request. */
    public enum Status {
        /** Every requested step completed and the worker returned normal success. */
        COMPLETED,
        /** No requested input step started. */
        NOT_STARTED,
        /** A completed prefix is known, but the request did not reach normal completion. */
        PARTIALLY_COMPLETED,
        /** The current or last-started step cannot be proven complete, possibly after a completed prefix. */
        STARTED_UNKNOWN
    }
}
