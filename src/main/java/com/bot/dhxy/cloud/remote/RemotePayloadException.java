package com.bot.dhxy.cloud.remote;

public class RemotePayloadException extends RuntimeException {

    public RemotePayloadException(String message) {
        super(message);
    }

    public RemotePayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
