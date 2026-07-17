package com.bot.dhxy.cloud.remote;

import com.bot.dhxy.model.dialog.DialogFingerprintWashMode;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Closed wire command for the {@code LOCAL_MACRO / DIALOG_PREPARED_ACTION_VALIDATION} macro. Mirrors the
 * Cloud {@code DialogPreparedActionValidationMacroCommand} field-for-field so the handler can drive the
 * exact-window fresh-geometry capture, mode-wash, binary fingerprint and distance comparison against the
 * expected fingerprint. The null / clickRequired / blank-fingerprint pre-capture gates stay on the Cloud
 * caller, so {@code expectedFingerprint} is always non-blank; the validation rectangle is committed
 * screen-absolute pixels (an empty/inverted rect is a closed INVALID_RECT terminal, not a command error).
 * Carries no owner/session/queue/retry.
 */
@Value
@Jacksonized
public class RemoteDialogPreparedActionValidationMacroCommandPayload implements RemoteLocalMacroCommandPayload {
    RemoteLocalMacroKind macroKind;
    int validationLeft;
    int validationTop;
    int validationRight;
    int validationBottom;
    DialogFingerprintWashMode washMode;
    String expectedFingerprint;
    int maxDistance;

    @Builder
    public RemoteDialogPreparedActionValidationMacroCommandPayload(
            RemoteLocalMacroKind macroKind,
            int validationLeft,
            int validationTop,
            int validationRight,
            int validationBottom,
            DialogFingerprintWashMode washMode,
            String expectedFingerprint,
            int maxDistance) {
        if (macroKind != RemoteLocalMacroKind.DIALOG_PREPARED_ACTION_VALIDATION) {
            throw new IllegalArgumentException("macroKind must be DIALOG_PREPARED_ACTION_VALIDATION");
        }
        // The wire enum is closed non-null: the Cloud caller normalizes a null action wash mode to
        // TEMPLATE_SPECIFIC before this command is built, so null never reaches the wire.
        if (washMode == null) {
            throw new IllegalArgumentException("washMode must not be null");
        }
        if (expectedFingerprint == null || expectedFingerprint.isBlank()) {
            throw new IllegalArgumentException("expectedFingerprint must not be blank");
        }
        if (maxDistance != 8 && maxDistance != 16) {
            throw new IllegalArgumentException("maxDistance must be 8 or 16");
        }
        this.macroKind = macroKind;
        this.validationLeft = validationLeft;
        this.validationTop = validationTop;
        this.validationRight = validationRight;
        this.validationBottom = validationBottom;
        this.washMode = washMode;
        this.expectedFingerprint = expectedFingerprint;
        this.maxDistance = maxDistance;
    }
}
