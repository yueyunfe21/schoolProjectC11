package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Stable semantic address for one retained action or read-only observation attempt. */
@Value
@Jacksonized
@JsonPropertyOrder({"phaseCode", "actionSlot", "occurrence", "attempt"})
public class RemoteSemanticAddress {
    String phaseCode;
    String actionSlot;
    long occurrence;
    int attempt;

    @Builder
    public RemoteSemanticAddress(
            String phaseCode,
            String actionSlot,
            long occurrence,
            int attempt) {
        this.phaseCode = requiredText(phaseCode, "phaseCode");
        this.actionSlot = requiredText(actionSlot, "actionSlot");
        if (occurrence < 0L) {
            throw new IllegalArgumentException("occurrence must be non-negative");
        }
        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must be non-negative");
        }
        this.occurrence = occurrence;
        this.attempt = attempt;
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
