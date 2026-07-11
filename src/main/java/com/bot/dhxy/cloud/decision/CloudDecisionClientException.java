package com.bot.dhxy.cloud.decision;

public class CloudDecisionClientException extends RuntimeException {

    public CloudDecisionClientException(String message) {
        super(message);
    }

    public CloudDecisionClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
