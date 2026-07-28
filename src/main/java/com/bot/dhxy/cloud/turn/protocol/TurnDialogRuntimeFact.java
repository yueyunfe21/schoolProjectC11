package com.bot.dhxy.cloud.turn.protocol;

import java.util.List;

/**
 * Typed read-only dialog runtime fact for {@code WHOLE_TASK_DIALOG_RUNTIME_READ} (TURN-35 Amendment #12).
 *
 * <p>Carries only the flat visible-dialog snapshot fields and the dialog-preparation status fields the
 * Cloud Wubei task needs to reproduce its {@code 696a12b0} defer/skip decisions in the original order.
 * All visible-dialog fields are null when no snapshot is present (or when a nonnull
 * {@code dialogSnapshotMaxAgeMs} filtered it out); all preparation fields are null when no preparation
 * status is present. It never carries a local object reference, dialog rect, capture provider, or any
 * derived business boolean — the Cloud caller keeps every {@code DialogType.NONE}, fresh/unbounded and
 * blocking-phase judgement itself.</p>
 */
public record TurnDialogRuntimeFact(
        String visibleDialogType,
        String visibleDialogSource,
        Long visibleDialogDetectedAtMs,
        String preparationPhase,
        String preparationOperation,
        String preparationTargetKeyword,
        String preparationSource,
        String interestTaskCode,
        List<String> interestOperations,
        String interestSource,
        Long interestCreatedAtMs,
        Long interestExpiresAtMs,
        Long interestAbsentAllowedAtMs,
        Boolean interestProbeOnly) {

    public TurnDialogRuntimeFact(String visibleDialogType, String visibleDialogSource,
                                 Long visibleDialogDetectedAtMs, String preparationPhase,
                                 String preparationOperation, String preparationTargetKeyword,
                                 String preparationSource, String interestTaskCode,
                                 List<String> interestOperations, String interestSource,
                                 Long interestCreatedAtMs, Long interestExpiresAtMs) {
        this(visibleDialogType, visibleDialogSource, visibleDialogDetectedAtMs, preparationPhase,
                preparationOperation, preparationTargetKeyword, preparationSource, interestTaskCode,
                interestOperations, interestSource, interestCreatedAtMs, interestExpiresAtMs, null, null);
    }

    public TurnDialogRuntimeFact(
            String visibleDialogType,
            String visibleDialogSource,
            Long visibleDialogDetectedAtMs,
            String preparationPhase,
            String preparationOperation,
            String preparationTargetKeyword,
            String preparationSource) {
        this(visibleDialogType, visibleDialogSource, visibleDialogDetectedAtMs,
                preparationPhase, preparationOperation, preparationTargetKeyword, preparationSource,
                null, List.of(), null, null, null, null, null);
    }
}
