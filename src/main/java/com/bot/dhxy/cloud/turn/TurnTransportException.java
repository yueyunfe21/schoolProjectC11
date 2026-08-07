package com.bot.dhxy.cloud.turn;

/**
 * Typed transport failure. It deliberately carries no fabricated turn outcome or action result.
 */
public final class TurnTransportException extends Exception {

    private final Kind kind;
    private final Integer httpStatus;
    private final String cloudErrorCode;

    public TurnTransportException(Kind kind, String message) {
        this(kind, message, null, null);
    }

    public TurnTransportException(Kind kind, String message, Throwable cause) {
        this(kind, message, null, cause);
    }

    public TurnTransportException(Kind kind, String message, Integer httpStatus, Throwable cause) {
        this(kind, message, httpStatus, null, cause);
    }

    /**
     * @param kind typed transport failure category
     * @param message diagnostic detail safe for the UI/log
     * @param httpStatus optional HTTP response status
     * @param cloudErrorCode optional structured Cloud problem code from an HTTP error body
     * @param cause optional underlying failure
     */
    public TurnTransportException(Kind kind,
                                  String message,
                                  Integer httpStatus,
                                  String cloudErrorCode,
                                  Throwable cause) {
        super(message, cause);
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        this.kind = kind;
        this.httpStatus = httpStatus;
        this.cloudErrorCode = cloudErrorCode;
    }

    public Kind kind() {
        return kind;
    }

    public Integer httpStatus() {
        return httpStatus;
    }

    /** @return Cloud's structured error code when an HTTP problem response supplied one. */
    public String cloudErrorCode() {
        return cloudErrorCode;
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
