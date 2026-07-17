package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Cloud response confirming whether a final-consumed receipt compacted the exact detail. */
@Value
@Jacksonized
@JsonPropertyOrder({"status", "ackDigest", "receiptDigest", "code", "message"})
public class RemoteFinalConsumedReceiptAck {
    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-fA-F]{64}");

    Status status;
    String ackDigest;
    String receiptDigest;
    RemoteOutcomeCode code;
    String message;

    @Builder
    public RemoteFinalConsumedReceiptAck(
            Status status,
            String ackDigest,
            String receiptDigest,
            RemoteOutcomeCode code,
            String message) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.ackDigest = sha256(ackDigest, "ackDigest");
        this.receiptDigest = sha256(receiptDigest, "receiptDigest");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.message = message == null ? "" : message;
    }

    private static String sha256(String value, String field) {
        if (value == null || !SHA256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a SHA-256 hex string");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public enum Status {
        ACCEPTED_COMPACTED,
        DUPLICATE_COMPACTED,
        REJECTED
    }
}
