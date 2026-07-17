package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Closed wire command for the {@code LOCAL_MACRO / DIALOG_DETECTION} macro. Mirrors the Cloud
 * {@code DialogDetectionMacroCommand} field-for-field so the handler can restore the committed same-path
 * no-focus dialog detection inputs and drive the existing exact-window capture / optional Alt+4 hide
 * mechanics without losing any caller-visible behavior.
 *
 * <p>{@code source} is the optional diagnostic label ({@code null} allowed, matching the baseline
 * {@code reason}); {@code hidePlayerNames} chooses the committed Alt+4 pre-capture hide; and
 * {@code waitBeforeCaptureMs} is the committed non-negative pre-capture wait. Carries no
 * owner/session/queue/retry.</p>
 */
@Value
@Jacksonized
public class RemoteDialogDetectionMacroCommandPayload implements RemoteLocalMacroCommandPayload {
    RemoteLocalMacroKind macroKind;
    String source;
    boolean hidePlayerNames;
    long waitBeforeCaptureMs;

    @Builder
    public RemoteDialogDetectionMacroCommandPayload(
            RemoteLocalMacroKind macroKind,
            String source,
            boolean hidePlayerNames,
            long waitBeforeCaptureMs) {
        if (macroKind != RemoteLocalMacroKind.DIALOG_DETECTION) {
            throw new IllegalArgumentException("macroKind must be DIALOG_DETECTION");
        }
        if (waitBeforeCaptureMs < 0L) {
            throw new IllegalArgumentException("waitBeforeCaptureMs must not be negative");
        }
        this.macroKind = macroKind;
        this.source = source;
        this.hidePlayerNames = hidePlayerNames;
        this.waitBeforeCaptureMs = waitBeforeCaptureMs;
    }
}
