package com.bot.dhxy.cloud.turn.protocol.observation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * TURN-40G: one observation-plane request from a local per-window observation runner. This plane is physically
 * separate from the command plane: an observation request never reads, occupies or resolves the per-window
 * unresolved command action slot, and its response never grants permission to execute ordinary business actions.
 *
 * <p>Ordinary snapshots are latest-wins (a newer unsent sample replaces an older one); key events are retained and
 * resent until acknowledged. At most one request is in flight per window.
 *
 * @param contractVersion observation protocol contract version
 * @param tenantId authenticated tenant identity
 * @param deviceId exact device identity
 * @param windowId exact logical window identity
 * @param hwnd exact native window handle the samples were captured from
 * @param taskCode wire task code of the Cloud task this runner observes for
 * @param taskRunId exact Cloud task run identity; stale-run requests are rejected
 * @param observerSeq per-window monotonic sample sequence
 * @param capturedAtMs epoch millis of the newest sample in this request
 * @param interestRevision the interest revision the runner sampled under
 * @param intentId optional current pathing intent correlation
 * @param attemptId optional current xiuluo green-chain attempt correlation
 * @param round optional current xiuluo round correlation
 * @param source optional producer source tag
 * @param activeCommandActionId optional id of the command-plane action currently executing locally (telemetry only)
 * @param pathingFacts exact current typed pathing snapshot or clear/replacement fact (zero or one)
 * @param facts mechanical fact samples (possibly empty)
 * @param events unacknowledged key events, including resends (possibly empty)
 * @param rois interest-selected small ROI frames (possibly empty; never a whole window frame by default)
 * @param dialogInterests exact current local dialog-interest snapshot or clear (zero or one)
 * @param terminalFrames dedicated stationary pathing candidate frame (zero or one)
 * @param preparedFrames dedicated non-pathing exact-window prepared frame (zero or one)
 */
public record ObservationRequest(
        int contractVersion,
        String tenantId,
        String deviceId,
        String windowId,
        String hwnd,
        String taskCode,
        String taskRunId,
        long observerSeq,
        long capturedAtMs,
        long interestRevision,
        @JsonInclude(JsonInclude.Include.NON_NULL) String intentId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String attemptId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer round,
        @JsonInclude(JsonInclude.Include.NON_NULL) String source,
        @JsonInclude(JsonInclude.Include.NON_NULL) String activeCommandActionId,
        List<ObservationPathingFact> pathingFacts,
        List<ObservationFact> facts,
        List<ObservationKeyEvent> events,
        List<ObservationRoi> rois,
        List<ObservationDialogInterestFact> dialogInterests,
        List<ObservationPreparedDialogFact> preparedDialogs,
        List<ObservationTerminalFrame> terminalFrames,
        List<ObservationPreparedFrame> preparedFrames) {

    public ObservationRequest(
            int contractVersion, String tenantId, String deviceId, String windowId, String hwnd,
            String taskCode, String taskRunId, long observerSeq, long capturedAtMs,
            long interestRevision, String intentId, String attemptId, Integer round, String source,
            String activeCommandActionId, List<ObservationPathingFact> pathingFacts,
            List<ObservationFact> facts, List<ObservationKeyEvent> events, List<ObservationRoi> rois,
            List<ObservationDialogInterestFact> dialogInterests,
            List<ObservationPreparedDialogFact> preparedDialogs,
            List<ObservationTerminalFrame> terminalFrames) {
        this(contractVersion, tenantId, deviceId, windowId, hwnd, taskCode, taskRunId, observerSeq,
                capturedAtMs, interestRevision, intentId, attemptId, round, source,
                activeCommandActionId, pathingFacts, facts, events, rois, dialogInterests,
                preparedDialogs, terminalFrames, List.of());
    }

    public ObservationRequest(
            int contractVersion, String tenantId, String deviceId, String windowId, String hwnd,
            String taskCode, String taskRunId, long observerSeq, long capturedAtMs,
            long interestRevision, String intentId, String attemptId, Integer round, String source,
            String activeCommandActionId, List<ObservationPathingFact> pathingFacts,
            List<ObservationFact> facts, List<ObservationKeyEvent> events, List<ObservationRoi> rois,
            List<ObservationDialogInterestFact> dialogInterests,
            List<ObservationPreparedDialogFact> preparedDialogs) {
        this(contractVersion, tenantId, deviceId, windowId, hwnd, taskCode, taskRunId, observerSeq,
                capturedAtMs, interestRevision, intentId, attemptId, round, source,
                activeCommandActionId, pathingFacts, facts, events, rois, dialogInterests,
                preparedDialogs, List.of(), List.of());
    }

    public ObservationRequest(
            int contractVersion, String tenantId, String deviceId, String windowId, String hwnd,
            String taskCode, String taskRunId, long observerSeq, long capturedAtMs,
            long interestRevision, String intentId, String attemptId, Integer round, String source,
            String activeCommandActionId, List<ObservationPathingFact> pathingFacts,
            List<ObservationFact> facts, List<ObservationKeyEvent> events, List<ObservationRoi> rois,
            List<ObservationDialogInterestFact> dialogInterests) {
        this(contractVersion, tenantId, deviceId, windowId, hwnd, taskCode, taskRunId, observerSeq,
                capturedAtMs, interestRevision, intentId, attemptId, round, source,
                activeCommandActionId, pathingFacts, facts, events, rois, dialogInterests,
                List.of(), List.of(), List.of());
    }

    /** Backward-compatible construction for callers that carry no typed dialog-interest fact. */
    public ObservationRequest(
            int contractVersion,
            String tenantId,
            String deviceId,
            String windowId,
            String hwnd,
            String taskCode,
            String taskRunId,
            long observerSeq,
            long capturedAtMs,
            long interestRevision,
            String intentId,
            String attemptId,
            Integer round,
            String source,
            String activeCommandActionId,
            List<ObservationPathingFact> pathingFacts,
            List<ObservationFact> facts,
            List<ObservationKeyEvent> events,
            List<ObservationRoi> rois) {
        this(contractVersion, tenantId, deviceId, windowId, hwnd, taskCode, taskRunId, observerSeq,
                capturedAtMs, interestRevision, intentId, attemptId, round, source, activeCommandActionId,
                pathingFacts, facts, events, rois, List.of(), List.of(), List.of(), List.of());
    }

    /** Backward-compatible construction for callers that carry no typed pathing fact. */
    public ObservationRequest(
            int contractVersion,
            String tenantId,
            String deviceId,
            String windowId,
            String hwnd,
            String taskCode,
            String taskRunId,
            long observerSeq,
            long capturedAtMs,
            long interestRevision,
            String intentId,
            String attemptId,
            Integer round,
            String source,
            String activeCommandActionId,
            List<ObservationFact> facts,
            List<ObservationKeyEvent> events,
            List<ObservationRoi> rois) {
        this(contractVersion, tenantId, deviceId, windowId, hwnd, taskCode, taskRunId, observerSeq,
                capturedAtMs, interestRevision, intentId, attemptId, round, source, activeCommandActionId,
                List.of(), facts, events, rois, List.of(), List.of(), List.of(), List.of());
    }
}
