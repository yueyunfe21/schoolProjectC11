package com.bot.dhxy.cloud.turn.protocol.observation;

import java.util.List;

/**
 * TURN-40G: one observation-plane response. Ordinary responses only acknowledge sequence progress, acknowledge key
 * events, update interests, or return analysis results — they never grant local permission to execute ordinary
 * business actions and never carry command-plane actions.
 *
 * @param contractVersion observation protocol contract version
 * @param acceptedObserverSeq highest observer sequence the Cloud has accepted for this window (idempotent for
 *        duplicates; a lower-sequence request never regresses this value)
 * @param interestRevision current Cloud interest revision for this window
 * @param acknowledgedEventIds ids of key events from this request the Cloud has durably accepted (possibly empty)
 * @param interests the complete current interest set under {@code interestRevision} (possibly empty = park)
 * @param analysisResults Cloud analysis results for uploaded material (possibly empty)
 * @param preparedFrameDemands exact one-shot non-pathing frame demands (possibly empty)
 */
public record ObservationResponse(
        int contractVersion,
        long acceptedObserverSeq,
        long interestRevision,
        List<String> acknowledgedEventIds,
        List<ObservationInterest> interests,
        List<ObservationAnalysisResult> analysisResults,
        List<ObservationPreparedFrameDemand> preparedFrameDemands) {

    public ObservationResponse(
            int contractVersion,
            long acceptedObserverSeq,
            long interestRevision,
            List<String> acknowledgedEventIds,
            List<ObservationInterest> interests,
            List<ObservationAnalysisResult> analysisResults) {
        this(contractVersion, acceptedObserverSeq, interestRevision, acknowledgedEventIds,
                interests, analysisResults, List.of());
    }
}
