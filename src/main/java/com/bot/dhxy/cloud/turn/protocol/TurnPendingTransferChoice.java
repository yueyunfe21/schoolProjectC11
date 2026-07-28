package com.bot.dhxy.cloud.turn.protocol;

/**
 * Protocol mirror of the baseline {@code PendingTransferChoiceMemory} carried by the
 * {@code WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE} local runtime operation (P-PROTO, parent
 * Amendment #6).
 *
 * <p>The nine fields map one-to-one to the baseline {@code WindowRuntimeContext} pending
 * transfer-choice slot; no new business inputs are introduced. Coordinate fields are nullable when
 * the baseline value was absent.</p>
 */
public record TurnPendingTransferChoice(
        String fromMap,
        Integer fromX,
        Integer fromY,
        String targetMap,
        Integer relativeX,
        Integer relativeY,
        String optionText,
        String source,
        long createdAtMs) {
}
