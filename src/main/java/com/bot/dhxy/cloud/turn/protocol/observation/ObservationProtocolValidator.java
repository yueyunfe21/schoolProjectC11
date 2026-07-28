package com.bot.dhxy.cloud.turn.protocol.observation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * TURN-40G: strict validator for the observation plane. Mirrors the turn-protocol validator idiom: every
 * {@code requireValid} returns the validated object unchanged and throws {@link IllegalArgumentException} on the
 * first violated invariant. Both repos carry a byte-identical copy of this class.
 *
 * <p>Structural guarantees enforced here: bounded payloads (no whole-window frames, bounded ROI bytes and counts),
 * nonblank exact identities, monotone-compatible sequences, unique event/roi/interest/analysis identities, and the
 * response-side rule that the Cloud only acknowledges event ids actually carried by the request it answers.
 */
public final class ObservationProtocolValidator {


    public static final int CONTRACT_VERSION = 1;

    /** Small-ROI bound: a region larger than this on either axis is treated as a whole-window upload and rejected. */
    public static final int MAX_ROI_DIMENSION = 640;
    /** Per-ROI encoded PNG byte bound. */
    public static final int MAX_ROI_PNG_BYTES = 640 * 1024;
    /** Per-request element bounds (bounded backlog; ordinary snapshots are latest-wins, never unbounded queues). */
    public static final int MAX_FACTS_PER_REQUEST = 64;
    public static final int MAX_PATHING_FACTS_PER_REQUEST = 1;
    public static final int MAX_EVENTS_PER_REQUEST = 32;
    public static final int MAX_ROIS_PER_REQUEST = 8;
    public static final int MAX_DIALOG_INTERESTS_PER_REQUEST = 1;
    public static final int MAX_PREPARED_DIALOGS_PER_REQUEST = 1;
    public static final int MAX_TERMINAL_FRAMES_PER_REQUEST = 1;
    public static final int MAX_PREPARED_FRAMES_PER_REQUEST = 1;
    public static final int MAX_PREPARED_FRAME_DEMANDS_PER_RESPONSE = 1;
    public static final int TERMINAL_FRAME_WIDTH = 1024;
    public static final int TERMINAL_FRAME_HEIGHT = 768;
    public static final int MAX_TERMINAL_FRAME_PNG_BYTES = 8 * 1024 * 1024;
    public static final int MAX_DIALOG_OPERATIONS = 8;
    public static final int MAX_PATHING_COORDINATE = 10_000;
    public static final int MAX_PATHING_TOLERANCE = 1_000;
    public static final int MAX_PATHING_TEXT_LENGTH = 256;

    private ObservationProtocolValidator() {
    }

    public static ObservationRequest requireValid(ObservationRequest request) {
        require(request != null, "observation request must not be null");
        require(request.contractVersion() == CONTRACT_VERSION,
                "observation request contractVersion must be " + CONTRACT_VERSION);
        requireText(request.tenantId(), "request.tenantId");
        requireText(request.deviceId(), "request.deviceId");
        requireText(request.windowId(), "request.windowId");
        requireText(request.hwnd(), "request.hwnd");
        requireText(request.taskCode(), "request.taskCode");
        requireText(request.taskRunId(), "request.taskRunId");
        require(request.observerSeq() >= 0L, "request.observerSeq must be nonnegative");
        require(request.capturedAtMs() > 0L, "request.capturedAtMs must be positive");
        require(request.interestRevision() >= 0L, "request.interestRevision must be nonnegative");
        requireOptionalText(request.intentId(), "request.intentId");
        requireOptionalText(request.attemptId(), "request.attemptId");
        require(request.round() == null || request.round() >= 0, "request.round must be nonnegative when present");
        requireOptionalText(request.source(), "request.source");
        requireOptionalText(request.activeCommandActionId(), "request.activeCommandActionId");

        List<ObservationPathingFact> pathingFacts = request.pathingFacts();
        require(pathingFacts != null, "request.pathingFacts must not be null");
        require(pathingFacts.size() <= MAX_PATHING_FACTS_PER_REQUEST,
                "request.pathingFacts must not exceed " + MAX_PATHING_FACTS_PER_REQUEST + " entry");
        for (ObservationPathingFact pathingFact : pathingFacts) {
            requirePathingFact(pathingFact, request);
        }

        List<ObservationFact> facts = request.facts();
        require(facts != null, "request.facts must not be null");
        require(facts.size() <= MAX_FACTS_PER_REQUEST,
                "request.facts must not exceed " + MAX_FACTS_PER_REQUEST + " entries");
        for (ObservationFact fact : facts) {
            requireFact(fact);
        }

        List<ObservationKeyEvent> events = request.events();
        require(events != null, "request.events must not be null");
        require(events.size() <= MAX_EVENTS_PER_REQUEST,
                "request.events must not exceed " + MAX_EVENTS_PER_REQUEST + " entries");
        Set<String> eventIds = new HashSet<>();
        for (ObservationKeyEvent event : events) {
            requireKeyEvent(event);
            require(eventIds.add(event.eventId()), "request.events must not repeat eventId " + event.eventId());
        }

        List<ObservationRoi> rois = request.rois();
        require(rois != null, "request.rois must not be null");
        require(rois.size() <= MAX_ROIS_PER_REQUEST,
                "request.rois must not exceed " + MAX_ROIS_PER_REQUEST + " entries");
        Set<String> roiKeys = new HashSet<>();
        for (ObservationRoi roi : rois) {
            requireRoi(roi);
            require(roiKeys.add(roi.roiKey()), "request.rois must not repeat roiKey " + roi.roiKey());
        }
        List<ObservationDialogInterestFact> dialogInterests = request.dialogInterests();
        require(dialogInterests != null, "request.dialogInterests must not be null");
        require(dialogInterests.size() <= MAX_DIALOG_INTERESTS_PER_REQUEST,
                "request.dialogInterests must not exceed " + MAX_DIALOG_INTERESTS_PER_REQUEST + " entry");
        for (ObservationDialogInterestFact fact : dialogInterests) {
            requireDialogInterestFact(fact, request);
        }
        List<ObservationPreparedDialogFact> preparedDialogs = request.preparedDialogs();
        require(preparedDialogs != null, "request.preparedDialogs must not be null");
        require(preparedDialogs.size() <= MAX_PREPARED_DIALOGS_PER_REQUEST,
                "request.preparedDialogs must not exceed " + MAX_PREPARED_DIALOGS_PER_REQUEST + " entry");
        for (ObservationPreparedDialogFact fact : preparedDialogs) {
            requirePreparedDialogFact(fact, request, dialogInterests);
        }
        List<ObservationTerminalFrame> terminalFrames = request.terminalFrames();
        require(terminalFrames != null, "request.terminalFrames must not be null");
        require(terminalFrames.size() <= MAX_TERMINAL_FRAMES_PER_REQUEST,
                "request.terminalFrames must not exceed " + MAX_TERMINAL_FRAMES_PER_REQUEST + " entry");
        for (ObservationTerminalFrame frame : terminalFrames) {
            requireTerminalFrame(frame, request);
        }
        List<ObservationPreparedFrame> preparedFrames = request.preparedFrames();
        require(preparedFrames != null, "request.preparedFrames must not be null");
        require(preparedFrames.size() <= MAX_PREPARED_FRAMES_PER_REQUEST,
                "request.preparedFrames must not exceed " + MAX_PREPARED_FRAMES_PER_REQUEST + " entry");
        for (ObservationPreparedFrame frame : preparedFrames) {
            requirePreparedFrame(frame, request);
        }
        return request;
    }

    public static ObservationResponse requireValid(ObservationResponse response) {
        require(response != null, "observation response must not be null");
        require(response.contractVersion() == CONTRACT_VERSION,
                "observation response contractVersion must be " + CONTRACT_VERSION);
        require(response.acceptedObserverSeq() >= 0L, "response.acceptedObserverSeq must be nonnegative");
        require(response.interestRevision() >= 0L, "response.interestRevision must be nonnegative");

        List<String> acknowledgedEventIds = response.acknowledgedEventIds();
        require(acknowledgedEventIds != null, "response.acknowledgedEventIds must not be null");
        Set<String> ackIds = new HashSet<>();
        for (String eventId : acknowledgedEventIds) {
            requireText(eventId, "response.acknowledgedEventIds entry");
            require(ackIds.add(eventId), "response.acknowledgedEventIds must not repeat " + eventId);
        }

        List<ObservationInterest> interests = response.interests();
        require(interests != null, "response.interests must not be null");
        Set<String> interestKeys = new HashSet<>();
        for (ObservationInterest interest : interests) {
            requireInterest(interest);
            require(interestKeys.add(interest.interestKey()),
                    "response.interests must not repeat interestKey " + interest.interestKey());
        }

        List<ObservationAnalysisResult> analysisResults = response.analysisResults();
        require(analysisResults != null, "response.analysisResults must not be null");
        Set<String> analysisIds = new HashSet<>();
        for (ObservationAnalysisResult result : analysisResults) {
            requireAnalysisResult(result);
            require(analysisIds.add(result.analysisId()),
                    "response.analysisResults must not repeat analysisId " + result.analysisId());
        }
        List<ObservationPreparedFrameDemand> preparedFrameDemands = response.preparedFrameDemands();
        require(preparedFrameDemands != null, "response.preparedFrameDemands must not be null");
        require(preparedFrameDemands.size() <= MAX_PREPARED_FRAME_DEMANDS_PER_RESPONSE,
                "response.preparedFrameDemands must not exceed "
                        + MAX_PREPARED_FRAME_DEMANDS_PER_RESPONSE + " entry");
        for (ObservationPreparedFrameDemand demand : preparedFrameDemands) {
            requirePreparedFrameDemand(demand);
        }
        return response;
    }

    /**
     * Cross-checks a response against the exact request it answers: same contract version, and the Cloud may only
     * acknowledge event ids that this request actually carried (the client resends unacknowledged key events, so a
     * lost response never strands an acknowledgement).
     */
    public static ObservationResponse requireValid(ObservationResponse response, ObservationRequest request) {
        requireValid(request);
        requireValid(response);
        require(response.contractVersion() == request.contractVersion(),
                "observation response contractVersion must match the request");
        Set<String> carriedEventIds = new HashSet<>();
        for (ObservationKeyEvent event : request.events()) {
            carriedEventIds.add(event.eventId());
        }
        for (String acknowledged : response.acknowledgedEventIds()) {
            require(carriedEventIds.contains(acknowledged),
                    "response acknowledges eventId " + acknowledged + " that the request did not carry");
        }
        for (ObservationPreparedFrameDemand demand : response.preparedFrameDemands()) {
            require(Objects.equals(demand.windowId(), request.windowId()),
                    "preparedFrameDemand.windowId must match request.windowId");
            require(Objects.equals(demand.hwnd(), request.hwnd()),
                    "preparedFrameDemand.hwnd must match request.hwnd");
            require(Objects.equals(demand.taskRunId(), request.taskRunId()),
                    "preparedFrameDemand.taskRunId must match request.taskRunId");
        }
        return response;
    }

    private static void requireFact(ObservationFact fact) {
        require(fact != null, "request.facts entry must not be null");
        require(fact.factType() != null, "fact.factType must not be null");
        requireText(fact.value(), "fact.value");
        require(fact.observedAtMs() > 0L, "fact.observedAtMs must be positive");
    }

    private static void requirePathingFact(ObservationPathingFact fact, ObservationRequest request) {
        require(fact != null, "request.pathingFacts entry must not be null");
        requireBoundedText(fact.taskRunId(), "pathingFact.taskRunId");
        requireBoundedText(fact.windowId(), "pathingFact.windowId");
        requireBoundedText(fact.hwnd(), "pathingFact.hwnd");
        requireBoundedText(fact.intentId(), "pathingFact.intentId");
        requireOptionalBoundedText(fact.replacedIntentId(), "pathingFact.replacedIntentId");
        requireBoundedText(fact.source(), "pathingFact.source");
        requireOptionalBoundedText(fact.targetMapName(), "pathingFact.targetMapName");
        requireOptionalBoundedText(fact.currentMapName(), "pathingFact.currentMapName");
        requireOptionalBoundedText(fact.dialogBlockingReason(), "pathingFact.dialogBlockingReason");
        require(fact.taskRunId().equals(request.taskRunId()),
                "pathingFact.taskRunId must match request.taskRunId");
        require(fact.windowId().equals(request.windowId()),
                "pathingFact.windowId must match request.windowId");
        require(fact.hwnd().equals(request.hwnd()),
                "pathingFact.hwnd must match request.hwnd");
        require(fact.intentId().equals(request.intentId()),
                "pathingFact.intentId must match request.intentId");
        require(fact.pathingType() != null, "pathingFact.pathingType must not be null");
        require(fact.state() != null, "pathingFact.state must not be null");
        require(fact.transition() != null, "pathingFact.transition must not be null");
        requireCoordinatePair(fact.targetX(), fact.targetY(), "pathingFact.target");
        requireCoordinatePair(fact.currentX(), fact.currentY(), "pathingFact.current");
        if (fact.currentX() != null) {
            require(fact.currentMapName() != null,
                    "pathingFact.currentMapName must be present with current coordinates");
        }
        require(fact.tolerance() >= 0 && fact.tolerance() <= MAX_PATHING_TOLERANCE,
                "pathingFact.tolerance must be within [0, " + MAX_PATHING_TOLERANCE + "]");
        if (fact.pathingType() == ObservationPathingType.TARGETED) {
            require(fact.targetMapName() != null,
                    "targeted pathingFact.targetMapName must be present");
        } else {
            require(fact.targetMapName() == null && fact.targetX() == null,
                    "untargeted pathing fact must not carry a target");
        }
        require(fact.pathingStartedAtMs() > 0L,
                "pathingFact.pathingStartedAtMs must be positive");
        require(fact.pathingUpdatedAtMs() >= fact.pathingStartedAtMs(),
                "pathingFact.pathingUpdatedAtMs must not precede pathingStartedAtMs");
        require(fact.pathingUpdatedAtMs() <= request.capturedAtMs(),
                "pathingFact.pathingUpdatedAtMs must not exceed request.capturedAtMs");
        require(fact.locationChangedAtMs() == 0L
                        || fact.locationChangedAtMs() >= fact.pathingStartedAtMs()
                        && fact.locationChangedAtMs() <= fact.pathingUpdatedAtMs(),
                "pathingFact.locationChangedAtMs must be zero or within the pathing interval");
        require(fact.movementObservedAtMs() == 0L
                        || fact.movementObservedAtMs() >= fact.pathingStartedAtMs()
                        && fact.movementObservedAtMs() <= fact.pathingUpdatedAtMs(),
                "pathingFact.movementObservedAtMs must be zero or within the pathing interval");
        if (fact.dialogBlocking()) {
            require(fact.dialogBlockingDetectedAtMs() >= fact.pathingStartedAtMs()
                            && fact.dialogBlockingDetectedAtMs() <= fact.pathingUpdatedAtMs(),
                    "blocking pathingFact.dialogBlockingDetectedAtMs must be within the pathing interval");
        } else {
        require(fact.dialogBlockingReason() == null && fact.dialogBlockingDetectedAtMs() == 0L,
                    "non-blocking pathing fact must carry null reason and zero detectedAtMs");
        }
        require((fact.terminalFrameId() == null) == (fact.terminalFrameGeneration() == null),
                "pathingFact terminal frame id/generation must be both present or both absent");
        if (fact.terminalFrameId() != null) {
            require(fact.state() == ObservationPathingState.ARRIVED,
                    "only ARRIVED pathingFact may carry terminal frame lineage");
            require(fact.terminalFrameId() > 0L && fact.terminalFrameGeneration() > 0L,
                    "pathingFact terminal frame id/generation must be positive");
        }
        switch (fact.transition()) {
            case CURRENT -> {
                require(fact.state() != ObservationPathingState.NONE,
                        "current pathing fact must not have NONE state");
                require(fact.replacedIntentId() == null,
                        "current pathing fact must not carry replacedIntentId");
            }
            case REPLACED -> {
                require(fact.state() != ObservationPathingState.NONE,
                        "replacement pathing fact must not have NONE state");
                require(fact.replacedIntentId() != null
                                && !fact.replacedIntentId().equals(fact.intentId()),
                        "replacement pathing fact must carry a different replacedIntentId");
            }
            case CLEARED -> {
                require(fact.state() == ObservationPathingState.NONE,
                        "cleared pathing fact must have NONE state");
                require(fact.replacedIntentId() == null,
                        "cleared pathing fact must not carry replacedIntentId");
                require(fact.currentMapName() == null && fact.currentX() == null,
                        "cleared pathing fact must not carry a current location");
                require(fact.locationChangedAtMs() == 0L && fact.movementObservedAtMs() == 0L,
                        "cleared pathing fact must clear observation timestamps");
                require(!fact.dialogBlocking() && fact.dialogBlockingReason() == null
                                && fact.dialogBlockingDetectedAtMs() == 0L,
                        "cleared pathing fact must clear dialog-blocking state");
            }
        }
    }

    private static void requireKeyEvent(ObservationKeyEvent event) {
        require(event != null, "request.events entry must not be null");
        requireText(event.eventId(), "event.eventId");
        require(event.eventType() != null, "event.eventType must not be null");
        require(event.occurredAtMs() > 0L, "event.occurredAtMs must be positive");
        requireOptionalText(event.intentId(), "event.intentId");
        requireOptionalText(event.attemptId(), "event.attemptId");
        require(event.round() == null || event.round() >= 0, "event.round must be nonnegative when present");
        requireOptionalText(event.source(), "event.source");
        requireOptionalText(event.detail(), "event.detail");
        requireOptionalText(event.expectedWaitId(), "event.expectedWaitId");
        requireOptionalText(event.taskCode(), "event.taskCode");
        requireOptionalText(event.businessTaskRunId(), "event.businessTaskRunId");
        require(event.combatGeneration() == null || event.combatGeneration() > 0L,
                "event.combatGeneration must be positive when present");
        boolean enterClaim = event.eventType() == ObservationKeyEventType.ENTER_BATTLE_CLICKED
                && event.expectedWaitId() != null;
        boolean combatTransition = event.eventType() == ObservationKeyEventType.IN_COMBAT
                || event.eventType() == ObservationKeyEventType.COMBAT_EXITED;
        boolean anyCombatIdentity = event.expectedWaitId() != null || event.combatGeneration() != null;
        require((enterClaim || combatTransition) == anyCombatIdentity,
                "expected-combat identity must be present only for enter-claim/combat-transition edges");
        if (combatTransition) {
            /*
             * Combat enter/exit is a universal physical fact: every transition carries the local
             * combat generation, while expectedWaitId is OPTIONAL and only classifies the edge as
             * expected (claim-bound click entry) vs unexpected (any other combat). Unexpected edges
             * carry no task identity; Cloud resolves it from the observation-run binding.
             */
            require(event.combatGeneration() != null,
                    "combat transition must carry the local combat generation");
        }
        if (enterClaim) {
            require(event.combatGeneration() == null,
                    "enter claim must bind generation only on the next local combat-visible edge");
        }
        boolean expectedCombat = combatTransition && event.expectedWaitId() != null;
        boolean replayTerminal = event.eventType() == ObservationKeyEventType.RETURN_HOME_REPLAY_SUCCEEDED
                || event.eventType() == ObservationKeyEventType.RETURN_HOME_REPLAY_FAILED
                || event.eventType() == ObservationKeyEventType.RETURN_HOME_REPLAY_IDENTITY_REJECTED;
        boolean anyReplayIdentity = event.taskCode() != null || event.businessTaskRunId() != null;
        require((replayTerminal || enterClaim || expectedCombat) == anyReplayIdentity,
                "task/run identity must be present exactly for expected-combat or replay edges");
        if (replayTerminal || expectedCombat) {
            require(event.taskCode() != null && event.businessTaskRunId() != null,
                    "combat/replay task-run identity fields must be present together");
            require("XIULUO_V2".equalsIgnoreCase(event.taskCode())
                            || "WUBEI".equalsIgnoreCase(event.taskCode()),
                    "expected-combat/replay transition supports only XIULUO_V2/WUBEI");
        }
    }

    private static void requireRoi(ObservationRoi roi) {
        require(roi != null, "request.rois entry must not be null");
        requireText(roi.roiKey(), "roi.roiKey");
        require(roi.left() >= 0, "roi.left must be nonnegative");
        require(roi.top() >= 0, "roi.top must be nonnegative");
        require(roi.width() > 0 && roi.width() <= MAX_ROI_DIMENSION,
                "roi.width must be within (0, " + MAX_ROI_DIMENSION + "]");
        require(roi.height() > 0 && roi.height() <= MAX_ROI_DIMENSION,
                "roi.height must be within (0, " + MAX_ROI_DIMENSION + "]");
        byte[] pngBytes = roi.pngBytes();
        require(pngBytes != null && pngBytes.length > 0, "roi.pngBytes must not be empty");
        require(pngBytes.length <= MAX_ROI_PNG_BYTES,
                "roi.pngBytes must not exceed " + MAX_ROI_PNG_BYTES + " bytes");
        requireOptionalBoundedText(roi.interestId(), "roi.interestId");
        requireOptionalBoundedText(roi.intentId(), "roi.intentId");
        requireOptionalBoundedText(roi.attemptId(), "roi.attemptId");
        require((roi.attemptId() == null) == (roi.round() == null),
                "roi.attemptId and roi.round must be present together");
        require(roi.round() == null || roi.round() > 0, "roi.round must be positive when present");
        boolean exactPathingCoordinate = "coordinate-strip".equals(roi.roiKey())
                && roi.intentId() != null
                && roi.attemptId() == null
                && roi.round() == null;
        require(roi.interestId() != null
                        || exactPathingCoordinate
                        || roi.intentId() == null && roi.attemptId() == null && roi.round() == null,
                "correlated roi must carry interestId unless it is an exact pathing coordinate");
    }

    /**
     * Validates one exact terminal PNG structurally without decoding its raster.
     *
     * @param pngBytes bounded encoded PNG bytes
     * @param field diagnostic field name used in contract failures
     */
    public static void requireExactTerminalFramePng(byte[] pngBytes, String field) {
        String label = field == null || field.isBlank() ? "terminalFrame.pngBytes" : field;
        require(pngBytes != null && pngBytes.length >= 33,
                label + " must contain a complete PNG IHDR");
        require(pngBytes.length <= MAX_TERMINAL_FRAME_PNG_BYTES,
                label + " must not exceed " + MAX_TERMINAL_FRAME_PNG_BYTES + " bytes");
        require((pngBytes[0] & 0xff) == 0x89
                        && pngBytes[1] == 0x50
                        && pngBytes[2] == 0x4e
                        && pngBytes[3] == 0x47
                        && pngBytes[4] == 0x0d
                        && pngBytes[5] == 0x0a
                        && pngBytes[6] == 0x1a
                        && pngBytes[7] == 0x0a,
                label + " must have a PNG signature");

        int offset = 8;
        boolean firstChunk = true;
        boolean sawIdat = false;
        boolean sawIend = false;
        while (offset < pngBytes.length) {
            require(pngBytes.length - offset >= 12,
                    label + " contains a truncated PNG chunk");
            long dataLength = readUnsignedInt(pngBytes, offset);
            require(dataLength <= Integer.MAX_VALUE,
                    label + " contains an oversized PNG chunk");
            long chunkEnd = (long) offset + 12L + dataLength;
            require(chunkEnd <= pngBytes.length,
                    label + " contains a truncated PNG chunk");
            int typeOffset = offset + 4;
            int dataOffset = offset + 8;
            int crcOffset = dataOffset + (int) dataLength;

            if (firstChunk) {
                require(chunkTypeEquals(pngBytes, typeOffset, "IHDR") && dataLength == 13L,
                        label + " must begin with a 13-byte IHDR");
                int width = readPositiveInt(pngBytes, dataOffset, label + " IHDR width");
                int height = readPositiveInt(pngBytes, dataOffset + 4, label + " IHDR height");
                require(width == TERMINAL_FRAME_WIDTH && height == TERMINAL_FRAME_HEIGHT,
                        label + " IHDR geometry must be "
                                + TERMINAL_FRAME_WIDTH + "x" + TERMINAL_FRAME_HEIGHT);
            } else {
                require(!chunkTypeEquals(pngBytes, typeOffset, "IHDR"),
                        label + " must contain exactly one leading IHDR");
            }

            CRC32 crc = new CRC32();
            crc.update(pngBytes, typeOffset, 4 + (int) dataLength);
            require(crc.getValue() == readUnsignedInt(pngBytes, crcOffset),
                    label + " contains a PNG chunk with an invalid CRC");

            boolean idat = chunkTypeEquals(pngBytes, typeOffset, "IDAT");
            boolean iend = chunkTypeEquals(pngBytes, typeOffset, "IEND");
            sawIdat |= idat;
            offset = (int) chunkEnd;
            firstChunk = false;
            if (iend) {
                require(dataLength == 0L && offset == pngBytes.length,
                        label + " must end with an empty IEND chunk");
                sawIend = true;
                break;
            }
        }
        require(sawIdat, label + " must contain an IDAT chunk");
        require(sawIend, label + " must contain a terminal IEND chunk");
    }

    private static void requireTerminalFrame(ObservationTerminalFrame frame, ObservationRequest request) {
        require(frame != null, "request.terminalFrames entry must not be null");
        require(frame.frameId() > 0L, "terminalFrame.frameId must be positive");
        require(frame.pathingGeneration() > 0L,
                "terminalFrame.pathingGeneration must be positive");
        require(Objects.equals(frame.tenantId(), request.tenantId()),
                "terminalFrame.tenantId must match request.tenantId");
        require(Objects.equals(frame.deviceId(), request.deviceId()),
                "terminalFrame.deviceId must match request.deviceId");
        require(Objects.equals(frame.windowId(), request.windowId()),
                "terminalFrame.windowId must match request.windowId");
        require(Objects.equals(frame.hwnd(), request.hwnd()),
                "terminalFrame.hwnd must match request.hwnd");
        require(Objects.equals(frame.taskRunId(), request.taskRunId()),
                "terminalFrame.taskRunId must match request.taskRunId");
        require(request.intentId() != null && Objects.equals(frame.intentId(), request.intentId()),
                "terminalFrame.intentId must match request.intentId");
        require(frame.left() == 0 && frame.top() == 0,
                "terminalFrame origin must be the exact window-client origin");
        require(frame.width() == TERMINAL_FRAME_WIDTH && frame.height() == TERMINAL_FRAME_HEIGHT,
                "terminalFrame geometry must be " + TERMINAL_FRAME_WIDTH + "x" + TERMINAL_FRAME_HEIGHT);
        require("PNG".equals(frame.encoding()), "terminalFrame.encoding must be PNG");
        require(frame.capturedAtMs() > 0L && frame.capturedAtMs() <= request.capturedAtMs(),
                "terminalFrame.capturedAtMs must be positive and not after request capture");
        requireExactTerminalFramePng(frame.pngBytes(), "terminalFrame.pngBytes");
    }

    private static void requirePreparedFrame(ObservationPreparedFrame frame, ObservationRequest request) {
        require(frame != null, "request.preparedFrames entry must not be null");
        requireBoundedText(frame.demandId(), "preparedFrame.demandId");
        requireBoundedText(frame.purpose(), "preparedFrame.purpose");
        require(frame.generation() > 0L, "preparedFrame.generation must be positive");
        require(frame.left() == 0 && frame.top() == 0,
                "preparedFrame origin must be the exact window-client origin");
        require(frame.width() == TERMINAL_FRAME_WIDTH && frame.height() == TERMINAL_FRAME_HEIGHT,
                "preparedFrame geometry must be " + TERMINAL_FRAME_WIDTH + "x" + TERMINAL_FRAME_HEIGHT);
        require("PNG".equals(frame.encoding()), "preparedFrame.encoding must be PNG");
        require(frame.capturedAtMs() > 0L && frame.capturedAtMs() <= request.capturedAtMs(),
                "preparedFrame.capturedAtMs must be positive and not after request capture");
        requireExactTerminalFramePng(frame.pngBytes(), "preparedFrame.pngBytes");
    }

    private static void requirePreparedFrameDemand(ObservationPreparedFrameDemand demand) {
        require(demand != null, "response.preparedFrameDemands entry must not be null");
        requireBoundedText(demand.demandId(), "preparedFrameDemand.demandId");
        requireBoundedText(demand.purpose(), "preparedFrameDemand.purpose");
        requireBoundedText(demand.correlationId(), "preparedFrameDemand.correlationId");
        requireBoundedText(demand.windowId(), "preparedFrameDemand.windowId");
        requireBoundedText(demand.hwnd(), "preparedFrameDemand.hwnd");
        requireBoundedText(demand.taskRunId(), "preparedFrameDemand.taskRunId");
        require(demand.generation() > 0L, "preparedFrameDemand.generation must be positive");
        require(demand.issuedAtMs() > 0L, "preparedFrameDemand.issuedAtMs must be positive");
        require(demand.expiresAtMs() > demand.issuedAtMs(),
                "preparedFrameDemand.expiresAtMs must be after issuedAtMs");
    }

    private static long readUnsignedInt(byte[] bytes, int offset) {
        return ((long) bytes[offset] & 0xffL) << 24
                | ((long) bytes[offset + 1] & 0xffL) << 16
                | ((long) bytes[offset + 2] & 0xffL) << 8
                | (long) bytes[offset + 3] & 0xffL;
    }

    private static int readPositiveInt(byte[] bytes, int offset, String field) {
        long value = readUnsignedInt(bytes, offset);
        require(value > 0L && value <= Integer.MAX_VALUE, field + " must be a positive integer");
        return (int) value;
    }

    private static boolean chunkTypeEquals(byte[] bytes, int offset, String type) {
        return bytes[offset] == (byte) type.charAt(0)
                && bytes[offset + 1] == (byte) type.charAt(1)
                && bytes[offset + 2] == (byte) type.charAt(2)
                && bytes[offset + 3] == (byte) type.charAt(3);
    }

    private static void requireDialogInterestFact(ObservationDialogInterestFact fact,
                                                  ObservationRequest request) {
        require(fact != null, "request.dialogInterests entry must not be null");
        require(Objects.equals(request.taskRunId(), fact.taskRunId()),
                "dialogInterest.taskRunId must match request.taskRunId");
        require(Objects.equals(request.windowId(), fact.windowId()),
                "dialogInterest.windowId must match request.windowId");
        require(Objects.equals(request.hwnd(), fact.hwnd()),
                "dialogInterest.hwnd must match request.hwnd");
        requireText(fact.interestId(), "dialogInterest.interestId");
        requireOptionalText(fact.taskCode(), "dialogInterest.taskCode");
        requireOptionalText(fact.source(), "dialogInterest.source");
        requireOptionalText(fact.attemptId(), "dialogInterest.attemptId");
        requireOptionalText(fact.intentId(), "dialogInterest.intentId");
        require(fact.operations() != null, "dialogInterest.operations must not be null");
        require(fact.operations().size() <= MAX_DIALOG_OPERATIONS,
                "dialogInterest.operations must not exceed " + MAX_DIALOG_OPERATIONS);
        Set<String> operations = new HashSet<>();
        for (String operation : fact.operations()) {
            requireText(operation, "dialogInterest.operations entry");
            try {
                ObservationDialogOperation.valueOf(operation);
            } catch (IllegalArgumentException invalidOperation) {
                throw new IllegalArgumentException(
                        "dialogInterest.operations contains unsupported value " + operation);
            }
            require(operations.add(operation), "dialogInterest.operations must be unique");
        }
        if (!fact.active()) {
            require(fact.taskCode() == null && fact.operations().isEmpty() && fact.source() == null,
                    "cleared dialogInterest must not carry business fields");
            require(fact.createdAtMs() == 0L && fact.expiresAtMs() == 0L
                            && fact.absentAllowedAtMs() == 0L && fact.probeStartAtMs() == 0L
                            && !fact.probeOnly() && !fact.enterBattleClaimed(),
                    "cleared dialogInterest must carry zero timing/probe fields");
            require(fact.attemptId() == null && fact.round() == null && fact.intentId() == null,
                    "cleared dialogInterest must not carry attempt/round/intent");
            return;
        }
        requireText(fact.taskCode(), "dialogInterest.taskCode");
        require(!fact.operations().isEmpty(), "active dialogInterest.operations must not be empty");
        requireText(fact.source(), "dialogInterest.source");
        require(fact.createdAtMs() > 0L && fact.createdAtMs() <= request.capturedAtMs(),
                "dialogInterest.createdAtMs must be positive and not after capture");
        require(fact.expiresAtMs() == 0L || fact.expiresAtMs() >= fact.createdAtMs(),
                "dialogInterest.expiresAtMs must be zero or not before creation");
        require(fact.absentAllowedAtMs() == 0L || fact.absentAllowedAtMs() >= fact.createdAtMs(),
                "dialogInterest.absentAllowedAtMs must be zero or not before creation");
        require(fact.probeStartAtMs() == 0L || fact.probeStartAtMs() >= fact.createdAtMs(),
                "dialogInterest.probeStartAtMs must be zero or not before creation");
        require(fact.expiresAtMs() == 0L || fact.absentAllowedAtMs() == 0L
                        || fact.absentAllowedAtMs() <= fact.expiresAtMs(),
                "dialogInterest.absentAllowedAtMs must not exceed expiry");
        require(fact.round() == null || fact.round() > 0, "dialogInterest.round must be positive when present");
        require((fact.attemptId() == null) == (fact.round() == null),
                "dialogInterest.attemptId and round must be present together");
    }

    private static void requirePreparedDialogFact(ObservationPreparedDialogFact fact,
                                                  ObservationRequest request,
                                                  List<ObservationDialogInterestFact> interests) {
        require(fact != null, "request.preparedDialogs entry must not be null");
        require(Objects.equals(request.taskRunId(), fact.taskRunId()),
                "preparedDialog.taskRunId must match request.taskRunId");
        require(Objects.equals(request.windowId(), fact.windowId()),
                "preparedDialog.windowId must match request.windowId");
        require(Objects.equals(request.hwnd(), fact.hwnd()),
                "preparedDialog.hwnd must match request.hwnd");
        requireText(fact.interestId(), "preparedDialog.interestId");
        requireText(fact.taskCode(), "preparedDialog.taskCode");
        requireText(fact.operation(), "preparedDialog.operation");
        requireText(fact.actionKey(), "preparedDialog.actionKey");
        requireText(fact.templatePath(), "preparedDialog.templatePath");
        requireText(fact.washMode(), "preparedDialog.washMode");
        requireText(fact.fingerprint(), "preparedDialog.fingerprint");
        requireOptionalText(fact.source(), "preparedDialog.source");
        require(fact.observerSeq() == request.observerSeq(),
                "preparedDialog.observerSeq must match request.observerSeq");
        require(fact.capturedAtMs() > 0L && fact.capturedAtMs() <= request.capturedAtMs(),
                "preparedDialog.capturedAtMs must be positive and not after request capture");
        require(fact.preparedAtMs() > 0L && fact.preparedAtMs() <= fact.capturedAtMs(),
                "preparedDialog.preparedAtMs must be positive and not after capture");
        requireRect(fact.matchLeft(), fact.matchTop(), fact.matchRight(), fact.matchBottom(),
                "preparedDialog.match");
        requireRect(fact.validationLeft(), fact.validationTop(),
                fact.validationRight(), fact.validationBottom(), "preparedDialog.validation");
        require(fact.clickX() >= 0 && fact.clickX() <= MAX_ROI_DIMENSION * 4
                        && fact.clickY() >= 0 && fact.clickY() <= MAX_ROI_DIMENSION * 4,
                "preparedDialog click must be bounded window-relative geometry");
        require(fact.clickX() >= fact.validationLeft() && fact.clickX() < fact.validationRight()
                        && fact.clickY() >= fact.validationTop() && fact.clickY() < fact.validationBottom(),
                "preparedDialog click must lie inside validation rect");
        require(fact.clickRequired(), "preparedDialog must require a click");
        boolean exactInterest = interests.stream().anyMatch(interest -> interest.active()
                && Objects.equals(interest.interestId(), fact.interestId())
                && Objects.equals(interest.taskCode(), fact.taskCode())
                && interest.operations().contains(fact.operation()));
        require(exactInterest, "preparedDialog must match an active dialogInterest in the same request");
    }

    private static void requireRect(int left, int top, int right, int bottom, String field) {
        require(left >= 0 && top >= 0 && right > left && bottom > top,
                field + " must be a positive window-relative rect");
        require(right <= MAX_ROI_DIMENSION * 4 && bottom <= MAX_ROI_DIMENSION * 4,
                field + " must be bounded");
    }

    private static void requireInterest(ObservationInterest interest) {
        require(interest != null, "response.interests entry must not be null");
        requireText(interest.interestKey(), "interest.interestKey");
        require(interest.samplePeriodMs() > 0L, "interest.samplePeriodMs must be positive");
        requireOptionalText(interest.detail(), "interest.detail");
        boolean anyRoiField = interest.roiLeft() != null || interest.roiTop() != null
                || interest.roiWidth() != null || interest.roiHeight() != null;
        if (anyRoiField) {
            require(interest.hasRoi(), "interest ROI fields must be present together");
            require(interest.roiLeft() >= 0, "interest.roiLeft must be nonnegative");
            require(interest.roiTop() >= 0, "interest.roiTop must be nonnegative");
            require(interest.roiWidth() > 0 && interest.roiWidth() <= MAX_ROI_DIMENSION,
                    "interest.roiWidth must be within (0, " + MAX_ROI_DIMENSION + "]");
            require(interest.roiHeight() > 0 && interest.roiHeight() <= MAX_ROI_DIMENSION,
                    "interest.roiHeight must be within (0, " + MAX_ROI_DIMENSION + "]");
        }
    }

    private static void requireAnalysisResult(ObservationAnalysisResult result) {
        require(result != null, "response.analysisResults entry must not be null");
        requireText(result.analysisId(), "analysisResult.analysisId");
        requireText(result.resultType(), "analysisResult.resultType");
        requireOptionalText(result.roiKey(), "analysisResult.roiKey");
        requireOptionalText(result.intentId(), "analysisResult.intentId");
        requireOptionalText(result.attemptId(), "analysisResult.attemptId");
        requireOptionalText(result.mapName(), "analysisResult.mapName");
        requireCoordinatePair(result.coordinateX(), result.coordinateY(), "analysisResult.pathing");
        if ("PATHING_COORDINATE_RESOLVED".equals(result.resultType())) {
            requireText(result.mapName(), "analysisResult.mapName");
            require(result.coordinateX() != null,
                    "resolved pathing coordinate result must carry coordinates");
        }
    }

    private static void requireOptionalText(String value, String field) {
        require(value == null || !value.isBlank(), field + " must be nonblank when present");
    }

    private static void requireBoundedText(String value, String field) {
        requireText(value, field);
        require(value.length() <= MAX_PATHING_TEXT_LENGTH,
                field + " must not exceed " + MAX_PATHING_TEXT_LENGTH + " characters");
    }

    private static void requireOptionalBoundedText(String value, String field) {
        if (value != null) {
            requireBoundedText(value, field);
        }
    }

    private static void requireCoordinatePair(Integer x, Integer y, String field) {
        require((x == null) == (y == null), field + " coordinates must be present together");
        if (x != null) {
            require(x >= 0 && x <= MAX_PATHING_COORDINATE,
                    field + "X must be within [0, " + MAX_PATHING_COORDINATE + "]");
            require(y >= 0 && y <= MAX_PATHING_COORDINATE,
                    field + "Y must be within [0, " + MAX_PATHING_COORDINATE + "]");
        }
    }

    private static void requireText(String value, String field) {
        require(value != null && !value.isBlank(), field + " must be nonblank");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
