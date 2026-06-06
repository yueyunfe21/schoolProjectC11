package com.bot.dhxy.model.dialog;

import com.bot.dhxy.service.dialog.DialogOperation;
import lombok.Builder;
import lombok.Value;

/**
 * Current background dialog-preparation state for one bound window.
 *
 * @param phase coarse lifecycle stage. PREPARING means the watcher is actively calculating a click
 *              candidate and task code should not take the turn only to wait.
 * @param operation operation being prepared.
 * @param targetKeyword destination/option keyword for operation matching.
 * @param source diagnostic source that created the request.
 * @param requestCreatedAtMs epoch millis when the request was registered.
 * @param preparingStartedAtMs epoch millis when the watcher started OCR/template work.
 * @param completedAtMs epoch millis when READY/FAILED was recorded.
 * @param failureReason short failure reason for logs.
 */
@Value
@Builder
public class DialogPreparationStatus {
    @Builder.Default
    DialogPreparationPhase phase = DialogPreparationPhase.NONE;
    DialogOperation operation;
    String targetKeyword;
    String source;
    long requestCreatedAtMs;
    long preparingStartedAtMs;
    long completedAtMs;
    String failureReason;

    public static DialogPreparationStatus none() {
        return DialogPreparationStatus.builder().phase(DialogPreparationPhase.NONE).build();
    }

    public boolean matches(DialogOperation expectedOperation, String expectedKeyword) {
        if (operation != expectedOperation) {
            return false;
        }
        if (expectedKeyword == null || expectedKeyword.isBlank()) {
            return true;
        }
        return targetKeyword != null && targetKeyword.equals(expectedKeyword);
    }
}
