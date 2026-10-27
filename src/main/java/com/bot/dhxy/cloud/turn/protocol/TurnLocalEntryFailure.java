package com.bot.dhxy.cloud.turn.protocol;

/** Local entry failure category and the Client clock's exact stationary-interval identity.
 * stationarySinceMs is required only for PATHING_TEMPLATE_MISSING; it is never compared to Cloud time.
 */
public record TurnLocalEntryFailure(Kind kind, Long stationarySinceMs) {
    public enum Kind { PATHING_TEMPLATE_MISSING, CLICK_CONFIRMATION_FAILED }
    public TurnLocalEntryFailure {
        if (kind == null || (kind == Kind.PATHING_TEMPLATE_MISSING
                && (stationarySinceMs == null || stationarySinceMs <= 0))
                || (kind == Kind.CLICK_CONFIRMATION_FAILED && stationarySinceMs != null)) {
            throw new IllegalArgumentException("Invalid local entry failure identity");
        }
    }
}
