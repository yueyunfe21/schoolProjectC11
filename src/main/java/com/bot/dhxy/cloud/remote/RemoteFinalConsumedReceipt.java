package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Client-to-cloud receipt for one applied final-consumed acknowledgement. */
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "contractVersion",
        "tenantId",
        "userId",
        "deviceId",
        "clientSessionId",
        "taskRunId",
        "semanticAddress",
        "ackDigest",
        "applyStatus",
        "appliedCompletedOccurrence",
        "appliedOpenOccurrence",
        "appliedThroughAttempt",
        "code",
        "message",
        "receiptDigest"
})
public class RemoteFinalConsumedReceipt {
    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-fA-F]{64}");

    int contractVersion;
    String tenantId;
    String userId;
    String deviceId;
    String clientSessionId;
    String taskRunId;
    RemoteSemanticAddress semanticAddress;
    String ackDigest;
    ApplyStatus applyStatus;
    long appliedCompletedOccurrence;
    @JsonSetter(nulls = Nulls.FAIL)
    Long appliedOpenOccurrence;
    int appliedThroughAttempt;
    RemoteOutcomeCode code;
    String message;
    String receiptDigest;

    @Builder
    @Jacksonized
    public RemoteFinalConsumedReceipt(
            int contractVersion,
            String tenantId,
            String userId,
            String deviceId,
            String clientSessionId,
            String taskRunId,
            RemoteSemanticAddress semanticAddress,
            String ackDigest,
            ApplyStatus applyStatus,
            long appliedCompletedOccurrence,
            Long appliedOpenOccurrence,
            int appliedThroughAttempt,
            RemoteOutcomeCode code,
            String message,
            String receiptDigest) {
        if (contractVersion != 1) {
            throw new IllegalArgumentException("contractVersion must be 1");
        }
        this.contractVersion = contractVersion;
        this.tenantId = requiredText(tenantId, "tenantId");
        this.userId = requiredText(userId, "userId");
        this.deviceId = requiredText(deviceId, "deviceId");
        this.clientSessionId = requiredText(clientSessionId, "clientSessionId");
        this.taskRunId = requiredText(taskRunId, "taskRunId");
        this.semanticAddress = Objects.requireNonNull(
                semanticAddress, "semanticAddress must not be null");
        this.ackDigest = sha256(ackDigest, "ackDigest");
        this.applyStatus = Objects.requireNonNull(applyStatus, "applyStatus must not be null");
        if (appliedCompletedOccurrence < -1L) {
            throw new IllegalArgumentException(
                    "appliedCompletedOccurrence must be at least -1");
        }
        this.appliedCompletedOccurrence = appliedCompletedOccurrence;
        if (appliedOpenOccurrence == null) {
            require(appliedThroughAttempt == -1,
                    "appliedThroughAttempt must be -1 when appliedOpenOccurrence is absent");
        } else {
            require(appliedCompletedOccurrence < Long.MAX_VALUE
                            && appliedOpenOccurrence == appliedCompletedOccurrence + 1L,
                    "appliedOpenOccurrence must immediately follow appliedCompletedOccurrence");
            require(appliedThroughAttempt >= 0,
                    "appliedThroughAttempt must be non-negative when an occurrence is open");
        }
        this.appliedOpenOccurrence = appliedOpenOccurrence;
        this.appliedThroughAttempt = appliedThroughAttempt;
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.message = message == null ? "" : message;
        this.receiptDigest = sha256(receiptDigest, "receiptDigest");
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String sha256(String value, String field) {
        if (value == null || !SHA256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a SHA-256 hex string");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static class RemoteFinalConsumedReceiptBuilder {
        @JsonSetter(value = "appliedOpenOccurrence", nulls = Nulls.FAIL)
        public RemoteFinalConsumedReceiptBuilder appliedOpenOccurrence(
                Long appliedOpenOccurrence) {
            this.appliedOpenOccurrence = appliedOpenOccurrence;
            return this;
        }
    }

    public enum ApplyStatus {
        APPLIED,
        DUPLICATE_APPLIED,
        REJECTED
    }
}
