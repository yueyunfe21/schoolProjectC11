package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Purely mechanical readiness evidence for one locally published resumed revision. */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class ResumeExecutorReadinessFact {
    String taskType;
    String windowId;
    String nativeHandle;
    long processId;
    long playerIdentityEpoch;
    long stopEpoch;
    long resumedFromRunRevision;
    long newActiveRunRevision;
    long localRegistrationGeneration;
    String localRegistrationStatus;
    String previousLocalStatus;
    long pauseTokenMechanicalGeneration;
    long operationLedgerRevision;
    long inFlightCaptureCount;
    long inFlightFactCount;
    long inFlightInputCount;
    long observedAtEpochMs;
    String producer;
    String factDigest;
}
