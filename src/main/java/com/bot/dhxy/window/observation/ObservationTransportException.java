package com.bot.dhxy.window.observation;

/**
 * TURN-40G: typed observation-plane transport failure. A transport failure is never interpreted as a business
 * fact — it never becomes a miss, {@code NONE}, {@code FREE}, {@code ARRIVED} or a retryable command; the local
 * runner just keeps its bounded latest snapshot and unacknowledged key events for the next attempt.
 */
public final class ObservationTransportException extends Exception {

    private final Kind kind;
    private final Integer httpStatus;

    public ObservationTransportException(Kind kind, String message) {
        this(kind, message, null, null);
    }

    public ObservationTransportException(Kind kind, String message, Throwable cause) {
        this(kind, message, null, cause);
    }

    public ObservationTransportException(Kind kind, String message, Integer httpStatus, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.httpStatus = httpStatus;
    }

    public Kind kind() {
        return kind;
    }

    public Integer httpStatus() {
        return httpStatus;
    }

    public enum Kind {
        REQUEST_CONTRACT,
        SERIALIZATION,
        NETWORK,
        INTERRUPTED,
        HTTP_STATUS,
        RESPONSE_TOO_LARGE,
        RESPONSE_CONTENT_TYPE,
        RESPONSE_PARSE,
        RESPONSE_CONTRACT
    }
}
