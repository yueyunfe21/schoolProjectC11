package com.bot.dhxy.cloud.turn;

/**
 * Typed transport failure. It deliberately carries no fabricated turn outcome or action result.
 */
public final class TurnTransportException extends Exception {

    private final Kind kind;
    private final Integer httpStatus;

    public TurnTransportException(Kind kind, String message) {
        this(kind, message, null, null);
    }

    public TurnTransportException(Kind kind, String message, Throwable cause) {
        this(kind, message, null, cause);
    }

    public TurnTransportException(Kind kind, String message, Integer httpStatus, Throwable cause) {
        super(message, cause);
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
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
        INVALID_CONFIGURATION,
        REQUEST_CONTRACT,
        SERIALIZATION,
        NETWORK,
        INTERRUPTED,
        HTTP_STATUS,
        RESPONSE_TOO_LARGE,
        RESPONSE_CONTENT_TYPE,
        RESPONSE_PARSE,
        RESPONSE_CONTRACT,
        TEMPLATE_HASH_MISMATCH
    }
}
