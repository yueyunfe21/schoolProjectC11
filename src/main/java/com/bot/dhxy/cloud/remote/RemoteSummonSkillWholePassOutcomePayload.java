package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Closed mechanical outcome and local exclusive-owner proof for one whole pass. */
@Value
@Builder
@Jacksonized
public class RemoteSummonSkillWholePassOutcomePayload {

    MechanicalStatus mechanicalStatus;
    CleanupValue cleanupResult;
    boolean callbackStarted;
    boolean ownerNeverAcquired;
    boolean ownerReleased;

    public RemoteSummonSkillWholePassOutcomePayload(
            MechanicalStatus mechanicalStatus,
            CleanupValue cleanupResult,
            boolean callbackStarted,
            boolean ownerNeverAcquired,
            boolean ownerReleased) {
        if (mechanicalStatus == null) {
            throw new IllegalArgumentException("mechanicalStatus is required");
        }
        if (ownerNeverAcquired && (callbackStarted || ownerReleased)) {
            throw new IllegalArgumentException(
                    "ownerNeverAcquired conflicts with callbackStarted/ownerReleased");
        }
        switch (mechanicalStatus) {
            case EXECUTED -> {
                if (cleanupResult == null || !callbackStarted || !ownerReleased) {
                    throw new IllegalArgumentException(
                            "EXECUTED requires cleanupResult, callbackStarted and ownerReleased");
                }
            }
            case NOT_EXECUTED -> {
                if (cleanupResult != null || callbackStarted
                        || !(ownerNeverAcquired || ownerReleased)) {
                    throw new IllegalArgumentException(
                            "NOT_EXECUTED requires no cleanup and an exact no-owner proof");
                }
            }
            case STOPPED -> {
                if (cleanupResult != null
                        || (callbackStarted && !ownerReleased)
                        || (!callbackStarted && !(ownerNeverAcquired || ownerReleased))) {
                    throw new IllegalArgumentException(
                            "STOPPED requires no cleanup and an exact no-owner proof");
                }
            }
            case UNKNOWN -> {
                if (cleanupResult != null) {
                    throw new IllegalArgumentException("UNKNOWN must not carry cleanupResult");
                }
            }
        }
        this.mechanicalStatus = mechanicalStatus;
        this.cleanupResult = cleanupResult;
        this.callbackStarted = callbackStarted;
        this.ownerNeverAcquired = ownerNeverAcquired;
        this.ownerReleased = ownerReleased;
    }

    public enum MechanicalStatus {
        EXECUTED,
        NOT_EXECUTED,
        STOPPED,
        UNKNOWN
    }

    public enum SlotStatus {
        NORMAL_SKILL,
        KEEP_SKILL,
        EMPTY_SLOT,
        LOCKED_SLOT,
        UNKNOWN
    }

    /** Exact value mirror of the existing local SummonSkillCleanupResult. */
    @Value
    @Builder
    @Jacksonized
    public static class CleanupValue {
        boolean success;
        int skillCount;
        int nextStartIndex;
        Map<Integer, SlotStatus> observedSlotStatuses;
        boolean ultimateSkillClicked;
        boolean ultimateSkillSucceeded;
        int inspectedSlotCount;
        int deletedSkillCount;
        String message;

        public CleanupValue(
                boolean success,
                int skillCount,
                int nextStartIndex,
                Map<Integer, SlotStatus> observedSlotStatuses,
                boolean ultimateSkillClicked,
                boolean ultimateSkillSucceeded,
                int inspectedSlotCount,
                int deletedSkillCount,
                String message) {
            if (skillCount < 0 || nextStartIndex < 0
                    || inspectedSlotCount < 0 || deletedSkillCount < 0) {
                throw new IllegalArgumentException("cleanup counters must not be negative");
            }
            Map<Integer, SlotStatus> statuses = observedSlotStatuses == null
                    ? Map.of() : new LinkedHashMap<>(observedSlotStatuses);
            if (statuses.entrySet().stream().anyMatch(entry -> entry.getKey() == null
                    || entry.getKey() < 0 || entry.getValue() == null)) {
                throw new IllegalArgumentException(
                        "observedSlotStatuses requires non-negative keys and non-null values");
            }
            this.success = success;
            this.skillCount = skillCount;
            this.nextStartIndex = nextStartIndex;
            this.observedSlotStatuses = Collections.unmodifiableMap(statuses);
            this.ultimateSkillClicked = ultimateSkillClicked;
            this.ultimateSkillSucceeded = ultimateSkillSucceeded;
            this.inspectedSlotCount = inspectedSlotCount;
            this.deletedSkillCount = deletedSkillCount;
            this.message = message == null || message.isBlank() ? "" : message.trim();
        }
    }
}
