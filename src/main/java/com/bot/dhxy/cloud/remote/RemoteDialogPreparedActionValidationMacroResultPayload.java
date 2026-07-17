package com.bot.dhxy.cloud.remote;

import com.bot.dhxy.service.dialog.DialogPreparedActionValidationLocalMechanics.State;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Strict typed EXECUTED result for {@code LOCAL_MACRO / DIALOG_PREPARED_ACTION_VALIDATION}. Mirrors the
 * Cloud closed {@code DialogPreparedActionValidationMacroResult}, reusing the committed mechanics state
 * enum verbatim. Only {@link State#VALIDATED} and {@link State#FINGERPRINT_MISMATCH} carry the current
 * fingerprint, the bit-level distance and the max distance; every other state leaves all three null.
 */
@Value
@Jacksonized
public class RemoteDialogPreparedActionValidationMacroResultPayload implements RemoteLocalMacroResultPayload {
    RemoteLocalMacroKind macroKind;
    State state;
    String currentFingerprint;
    Integer distance;
    Integer maxDistance;

    @Builder
    public RemoteDialogPreparedActionValidationMacroResultPayload(
            RemoteLocalMacroKind macroKind,
            State state,
            String currentFingerprint,
            Integer distance,
            Integer maxDistance) {
        if (macroKind != RemoteLocalMacroKind.DIALOG_PREPARED_ACTION_VALIDATION) {
            throw new IllegalArgumentException("macroKind must be DIALOG_PREPARED_ACTION_VALIDATION");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        boolean measured = state == State.VALIDATED || state == State.FINGERPRINT_MISMATCH;
        boolean hasMetrics = currentFingerprint != null && distance != null && maxDistance != null;
        boolean hasAnyMetric = currentFingerprint != null || distance != null || maxDistance != null;
        if (measured && !hasMetrics) {
            throw new IllegalArgumentException(
                    "VALIDATED/FINGERPRINT_MISMATCH result requires fingerprint, distance and maxDistance");
        }
        if (!measured && hasAnyMetric) {
            throw new IllegalArgumentException("non-measured result must not carry fingerprint/distance metrics");
        }
        if (measured) {
            if (currentFingerprint.isBlank()) {
                throw new IllegalArgumentException("measured result requires a non-blank fingerprint");
            }
            if (distance < 0) {
                throw new IllegalArgumentException("measured result requires a non-negative distance");
            }
            if (maxDistance != 8 && maxDistance != 16) {
                throw new IllegalArgumentException("measured result maxDistance must be 8 or 16");
            }
            if ((state == State.VALIDATED) != (distance <= maxDistance)) {
                throw new IllegalArgumentException(
                        "VALIDATED requires distance <= maxDistance; FINGERPRINT_MISMATCH requires distance > maxDistance");
            }
        }
        this.macroKind = macroKind;
        this.state = state;
        this.currentFingerprint = currentFingerprint;
        this.distance = distance;
        this.maxDistance = maxDistance;
    }
}
