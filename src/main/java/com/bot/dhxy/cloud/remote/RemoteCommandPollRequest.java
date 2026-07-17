package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
public class RemoteCommandPollRequest {
    public static final long MAX_WAIT_TIMEOUT_MS = 30_000L;

    int contractVersion;
    String tenantId;
    String userId;
    String deviceId;
    String clientSessionId;
    long waitTimeoutMs;

    @Builder
    public RemoteCommandPollRequest(
            int contractVersion,
            String tenantId,
            String userId,
            String deviceId,
            String clientSessionId,
            long waitTimeoutMs) {
        validateWaitTimeout(waitTimeoutMs);
        this.contractVersion = contractVersion;
        this.tenantId = tenantId;
        this.userId = userId;
        this.deviceId = deviceId;
        this.clientSessionId = clientSessionId;
        this.waitTimeoutMs = waitTimeoutMs;
    }

    static void validateWaitTimeout(long waitTimeoutMs) {
        if (waitTimeoutMs < 0L) {
            throw new IllegalArgumentException("poll.waitTimeoutMs must not be negative");
        }
        if (waitTimeoutMs > MAX_WAIT_TIMEOUT_MS) {
            throw new IllegalArgumentException(
                    "poll.waitTimeoutMs must not exceed " + MAX_WAIT_TIMEOUT_MS);
        }
    }
}
