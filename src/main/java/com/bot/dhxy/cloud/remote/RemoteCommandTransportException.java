package com.bot.dhxy.cloud.remote;

import lombok.Getter;

import java.net.URI;

@Getter
public class RemoteCommandTransportException extends RuntimeException {

    private final FailureType failureType;
    private final URI endpoint;
    private final Integer statusCode;
    private final String responseBody;
    private final String serverCode;
    private final String serverMessage;

    RemoteCommandTransportException(
            FailureType failureType,
            URI endpoint,
            Integer statusCode,
            String responseBody,
            String serverCode,
            String serverMessage,
            String message,
            Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
        this.endpoint = endpoint;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.serverCode = serverCode;
        this.serverMessage = serverMessage;
    }

    public enum FailureType {
        INVALID_REQUEST,
        SERIALIZATION,
        HTTP_TIMEOUT,
        INTERRUPTED,
        IO,
        HTTP_STATUS,
        OUTCOME_REJECTED,
        EMPTY_RESPONSE,
        DESERIALIZATION,
        SCHEMA_MISMATCH
    }
}
