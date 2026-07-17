package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Stable cloud receipt for an accepted readiness request. */
@Value
@Builder
@Jacksonized
public class RemoteTaskRunReceipt {
    String taskRunId;
    long confirmedRunRevision;
    String receiptId;
    String requestId;
    String requestDigest;
    String factDigest;
    long recordedAtEpochMs;
}
