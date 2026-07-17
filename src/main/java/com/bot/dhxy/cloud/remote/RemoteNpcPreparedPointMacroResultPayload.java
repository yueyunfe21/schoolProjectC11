package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-696-NPC-TOOLTIP-PREPAREDPOINT-CONTRACT-COHORT-1: closed DHXY-side wire result for the NPC
 * prepared-point local macro. Mirrors the Cloud {@code NpcPreparedPointMacroResult} contract exactly,
 * including the self-verified terminal/clickProduced combination: a verify verdict requires a produced
 * click; the pre-click terminals cannot have produced a click; interruption / mechanics failure allow
 * either polarity.
 *
 * <p>Standalone contract type; the sealed transport / kind binding is the deferred shared-integration
 * seam.</p>
 */
@Value
@Jacksonized
public class RemoteNpcPreparedPointMacroResultPayload {

    Status status;
    boolean clickProduced;
    int screenX;
    int screenY;
    String reason;

    @Builder
    public RemoteNpcPreparedPointMacroResultPayload(Status status, boolean clickProduced, int screenX, int screenY, String reason) {
        this.status = requireNonNull(status, "status");
        switch (status) {
            case VERIFIED, NOT_VERIFIED -> require(clickProduced, status + " requires clickProduced=true");
            case BINDING_UNAVAILABLE, NON_INPUT_WORKER -> require(!clickProduced, status + " requires clickProduced=false");
            case INTERRUPTED, MECHANICS_FAILED -> {
                // Either polarity, matching the real point of occurrence.
            }
        }
        this.clickProduced = clickProduced;
        this.screenX = screenX;
        this.screenY = screenY;
        this.reason = reason;
    }

    public enum Status {
        VERIFIED,
        NOT_VERIFIED,
        BINDING_UNAVAILABLE,
        NON_INPUT_WORKER,
        INTERRUPTED,
        MECHANICS_FAILED
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
