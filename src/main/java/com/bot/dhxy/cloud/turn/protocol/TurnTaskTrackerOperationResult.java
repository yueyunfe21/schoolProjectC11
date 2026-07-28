package com.bot.dhxy.cloud.turn.protocol;

public record TurnTaskTrackerOperationResult(
        State state,
        Integer absoluteLeft,
        Integer absoluteTop,
        Integer width,
        Integer height,
        String sha256) {

    public TurnTaskTrackerOperationResult {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        boolean captured = state == State.CAPTURED;
        boolean allImageFields = absoluteLeft != null && absoluteTop != null
                && width != null && height != null && sha256 != null;
        boolean anyImageField = absoluteLeft != null || absoluteTop != null
                || width != null || height != null || sha256 != null;
        if ((captured && !allImageFields) || (!captured && anyImageField)) {
            throw new IllegalArgumentException("image fields must be present only for CAPTURED");
        }
        if (captured && (width <= 0 || height <= 0
                || !sha256.matches("(?i)[0-9a-f]{64}"))) {
            throw new IllegalArgumentException("CAPTURED dimensions or SHA-256 are invalid");
        }
    }

    public enum State {
        CAPTURED,
        ABSENT,
        CAPTURE_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        MECHANICS_FAILED
    }
}
