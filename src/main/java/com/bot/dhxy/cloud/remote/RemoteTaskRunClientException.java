package com.bot.dhxy.cloud.remote;

import java.net.URI;

/** Structured transport or domain failure from the lifecycle endpoint. */
public final class RemoteTaskRunClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final FailureType failureType;
    private final URI endpoint;
    private final Integer statusCode;
    private final String responseBody;
    private final String remoteErrorCode;
    private final String remoteErrorMessage;

    public RemoteTaskRunClientException(
            FailureType failureType,
            URI endpoint,
            Integer statusCode,
            String responseBody,
            String remoteErrorCode,
            String remoteErrorMessage,
            String message,
            Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
        this.endpoint = endpoint;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.remoteErrorCode = remoteErrorCode;
        this.remoteErrorMessage = remoteErrorMessage;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getRemoteErrorCode() {
        return remoteErrorCode;
    }

    public String getRemoteErrorMessage() {
        return remoteErrorMessage;
    }

    public boolean isOutcomeUncertain() {
        return switch (failureType) {
            case HTTP_TIMEOUT, INTERRUPTED, IO, EMPTY_RESPONSE,
                    DESERIALIZATION, SCHEMA_MISMATCH -> true;
            case HTTP_STATUS -> statusCode == null || statusCode >= 500;
            case REMOTE_REJECTED -> "INTERNAL_ERROR".equals(remoteErrorCode);
            case INVALID_REQUEST, SERIALIZATION -> false;
        };
    }

    public enum FailureType {
        INVALID_REQUEST,
        SERIALIZATION,
        HTTP_TIMEOUT,
        INTERRUPTED,
        IO,
        HTTP_STATUS,
        EMPTY_RESPONSE,
        DESERIALIZATION,
        SCHEMA_MISMATCH,
        REMOTE_REJECTED
    }
}
