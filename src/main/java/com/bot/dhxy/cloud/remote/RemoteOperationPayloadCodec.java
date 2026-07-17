package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

public final class RemoteOperationPayloadCodec {

    private static final Set<String> CAPTURE_FIELDS = Set.of(
            "captureId", "region", "imageFormat", "capturePurpose", "sessionRef");
    private static final Set<String> CAPTURE_REQUIRED_FIELDS = Set.of(
            "captureId", "region", "imageFormat", "capturePurpose");
    private static final Set<String> CAPTURE_REGION_FIELDS = Set.of(
            "coordinateSpace", "x", "y", "width", "height");
    private static final Set<String> WINDOW_FACT_FIELDS = Set.of("factKind");
    private static final Set<String> BAG_RETURN_ITEM_COMMAND_FIELDS = Set.of(
            "macroKind", "operation", "templatePath", "maxBackPage", "source", "cachedPoint");
    private static final Set<String> BAG_RETURN_ITEM_COMMAND_REQUIRED_FIELDS = Set.of(
            "macroKind", "operation", "templatePath", "maxBackPage", "source");
    private static final Set<String> BAG_USE_INCENSE_COMMAND_FIELDS = Set.of("macroKind");
    private static final Set<String> NAVIGATE_IN_CURRENT_MAP_COMMAND_FIELDS = Set.of(
            "macroKind", "targetMapName", "targetX", "targetY", "targetName",
            "randomizeMiniMapClickPoint", "miniMapClickRandomRadiusPx", "keepTurnOnCurrentMapPathing",
            "arrivalTolerance", "source", "freshCurrentMapName", "freshCurrentX", "freshCurrentY",
            "freshCurrentLocationAtMs", "freshCurrentLocationPhaseBound");
    // Nullable request fields (targetMapName/targetX/targetY/targetName/freshCurrentMapName/
    // freshCurrentX/freshCurrentY) are NON_NULL-omitted on the wire, so only the always-present
    // primitives and macroKind/source are required.
    private static final Set<String> NAVIGATE_IN_CURRENT_MAP_COMMAND_REQUIRED_FIELDS = Set.of(
            "macroKind", "randomizeMiniMapClickPoint", "miniMapClickRandomRadiusPx",
            "keepTurnOnCurrentMapPathing", "arrivalTolerance", "source",
            "freshCurrentLocationAtMs", "freshCurrentLocationPhaseBound");
    // UI_CLEAN command: source is NON_NULL-omitted for the null-source operations, so only
    // macroKind and operation are required.
    private static final Set<String> UI_CLEAN_COMMAND_FIELDS = Set.of("macroKind", "operation", "source");
    private static final Set<String> UI_CLEAN_COMMAND_REQUIRED_FIELDS = Set.of("macroKind", "operation");
    // DIALOG_DETECTION command: source is NON_NULL-omitted on the wire, so only macroKind and the two
    // always-present primitives are required.
    private static final Set<String> DIALOG_DETECTION_COMMAND_FIELDS = Set.of(
            "macroKind", "source", "hidePlayerNames", "waitBeforeCaptureMs");
    private static final Set<String> DIALOG_DETECTION_COMMAND_REQUIRED_FIELDS = Set.of(
            "macroKind", "hidePlayerNames", "waitBeforeCaptureMs");
    // DIALOG_DETECTION result keeps its own flat closed key set: the richer captured terminal cannot ride
    // the 4-key BAG/NAV/UI_CLEAN shape, and folding it in would change those macros' canonical tree/digest.
    private static final Set<String> LOCAL_MACRO_DIALOG_RESULT_FIELDS = Set.of(
            "macroKind", "state",
            "dialogType", "dialogLeft", "dialogTop", "dialogRight", "dialogBottom",
            "framePngBytes", "frameSha256", "frameWidth", "frameHeight",
            "maskStddev", "optionGreenCount", "storyThinWhiteCount", "storyGreenCount",
            "storyTextMatched", "storyQualifyingRows", "storyMaxWhitePixelsInRow",
            "storyMaxClustersInRow", "storyMaxSpanInRow");
    // PLAYER_STATE_FIRST_AID command: variant fields are NON_NULL-omitted on the wire, so only macroKind
    // and operation are required; the chosen operation carries either the four bar toggles or the cached
    // plan base + ordered targets.
    private static final Set<String> PLAYER_STATE_FIRST_AID_COMMAND_FIELDS = Set.of(
            "macroKind", "operation", "playerHp", "playerMp", "petHp", "petMp",
            "planBaseX", "planBaseY", "targets");
    private static final Set<String> PLAYER_STATE_FIRST_AID_COMMAND_REQUIRED_FIELDS = Set.of(
            "macroKind", "operation");
    // PLAYER_STATE_FIRST_AID result keeps its own flat closed key set: the richer per-operation terminal
    // cannot ride the 4-key BAG/NAV/UI_CLEAN shape, and folding it in would change those macros' tree.
    private static final Set<String> LOCAL_MACRO_PLAYER_STATE_RESULT_FIELDS = Set.of(
            "macroKind", "operation", "probeSnapshotStatus", "probeObservations",
            "healSnapshotStatus", "healOutcomes", "cachedPlanStatus",
            "observedBaseX", "observedBaseY");
    // DIALOG_PREPARED_ACTION_VALIDATION command: every field is always present on the wire.
    private static final Set<String> DIALOG_PREPARED_ACTION_COMMAND_FIELDS = Set.of(
            "macroKind", "validationLeft", "validationTop", "validationRight", "validationBottom",
            "washMode", "expectedFingerprint", "maxDistance");
    // DIALOG_PREPARED_ACTION_VALIDATION result keeps its own flat closed key set; only
    // VALIDATED/FINGERPRINT_MISMATCH populate fingerprint/distance/maxDistance, else explicit null.
    private static final Set<String> LOCAL_MACRO_PREPARED_ACTION_RESULT_FIELDS = Set.of(
            "macroKind", "state", "currentFingerprint", "distance", "maxDistance");
    // DIALOG_OPTION_OCR_IMAGE command: the supplied frame + rect are NON_NULL-omitted on the wire (a fresh
    // capture carries none), so only macroKind is always required.
    private static final Set<String> DIALOG_OPTION_OCR_IMAGE_COMMAND_FIELDS = Set.of(
            "macroKind", "suppliedFramePngBytes", "suppliedFrameSha256",
            "rectLeft", "rectTop", "rectRight", "rectBottom", "source");
    private static final Set<String> DIALOG_OPTION_OCR_IMAGE_COMMAND_REQUIRED_FIELDS = Set.of("macroKind");
    // DIALOG_OPTION_OCR_IMAGE result keeps its own flat closed key set: the same-frame raw/green/yellow PNG
    // bundle cannot ride the 4-key BAG/NAV/UI_CLEAN shape; non-CAPTURED fields are explicit null.
    private static final Set<String> LOCAL_MACRO_DIALOG_OPTION_OCR_IMAGE_RESULT_FIELDS = Set.of(
            "macroKind", "status",
            "rawPngBytes", "rawSha256", "greenPngBytes", "greenSha256", "yellowPngBytes", "yellowSha256",
            "imageWidth", "imageHeight", "scanLeft", "scanTop", "scanRight", "scanBottom", "reason");
    // DIALOG_OPTION_OCR_WORDS command: every field except the NON_NULL-omitted source is always present.
    private static final Set<String> DIALOG_OPTION_OCR_WORDS_COMMAND_FIELDS = Set.of(
            "macroKind", "variant", "variantPngBytes", "variantSha256",
            "imageWidth", "imageHeight", "rectLeft", "rectTop", "rectRight", "rectBottom", "source");
    private static final Set<String> DIALOG_OPTION_OCR_WORDS_COMMAND_REQUIRED_FIELDS = Set.of(
            "macroKind", "variant", "variantPngBytes", "variantSha256",
            "imageWidth", "imageHeight", "rectLeft", "rectTop", "rectRight", "rectBottom");
    // DIALOG_OPTION_OCR_WORDS result keeps its own flat closed key set; only WORDS carries a non-null list.
    private static final Set<String> LOCAL_MACRO_DIALOG_OPTION_OCR_WORDS_RESULT_FIELDS = Set.of(
            "macroKind", "status", "wordBoxes");
    // DIALOG_WHITE_STORY_TEMPLATE command: specs and absentAllowed are always present; source and the
    // supplied frame (bytes + SHA + rect + type) are NON_NULL-omitted (a fresh-detection command carries none).
    private static final Set<String> DIALOG_WHITE_STORY_TEMPLATE_COMMAND_FIELDS = Set.of(
            "macroKind", "specs", "absentAllowed", "source",
            "suppliedFramePngBytes", "suppliedFrameSha256",
            "suppliedFrameLeft", "suppliedFrameTop", "suppliedFrameRight", "suppliedFrameBottom",
            "suppliedFrameType");
    private static final Set<String> DIALOG_WHITE_STORY_TEMPLATE_COMMAND_REQUIRED_FIELDS = Set.of(
            "macroKind", "specs", "absentAllowed");
    // DIALOG_WHITE_STORY_TEMPLATE result keeps its own flat closed key set: MATCHED same-frame evidence and
    // STORY_MISS/STORY_ABSENT rect+dimensions cannot ride the 4-key shape; state-unused fields are explicit null.
    private static final Set<String> LOCAL_MACRO_DIALOG_WHITE_STORY_TEMPLATE_RESULT_FIELDS = Set.of(
            "macroKind", "state",
            "matchedTemplateName", "matchedTemplatePath",
            "relativeX", "relativeY", "absoluteX", "absoluteY",
            "frameLeft", "frameTop", "frameRight", "frameBottom",
            "framePngBytes", "frameSha256", "frameWidth", "frameHeight");
    private static final Set<String> LOCAL_MACRO_CACHE_POINT_FIELDS = Set.of(
            "templatePath", "clickX", "clickY", "learnedAtMs", "source");
    private static final Set<String> LOCAL_MACRO_RESULT_FIELDS = Set.of(
            "macroKind", "operation", "state", "cachePoint");
    private static final Set<String> INPUT_BUNDLE_FIELDS = Set.of(
            "description", "coordinateSpace", "actions", "sessionRef");
    private static final Set<String> INPUT_BUNDLE_REQUIRED_FIELDS = Set.of(
            "description", "coordinateSpace", "actions");
    private static final Set<String> EXCLUSIVE_SESSION_REF_FIELDS = Set.of(
            "exclusiveSessionId", "bindingGeneration", "step");
    private static final Set<String> EXCLUSIVE_CONTROL_FIELDS = Set.of(
            "command", "exclusiveSessionId", "bindingGeneration", "step");
    private static final Set<String> EXCLUSIVE_CONTROL_OUTCOME_FIELDS = Set.of(
            "command", "exclusiveSessionId", "bindingGeneration", "step",
            "mechanicalStatus", "ownerReleased");
    private static final Set<String> SUMMON_SKILL_WHOLE_PASS_FIELDS = Set.of(
            "expectedSkillCount", "trustExpectedSkillCount", "startSlotIndex",
            "skipUltimateCornerCheck", "exclusiveSessionId", "bindingGeneration");
    private static final Set<String> SUMMON_SKILL_WHOLE_PASS_REQUIRED_FIELDS = Set.of(
            "trustExpectedSkillCount", "skipUltimateCornerCheck",
            "exclusiveSessionId", "bindingGeneration");
    private static final Set<String> SUMMON_SKILL_WHOLE_PASS_OUTCOME_FIELDS = Set.of(
            "mechanicalStatus", "cleanupResult", "callbackStarted",
            "ownerNeverAcquired", "ownerReleased");
    private static final Set<String> SUMMON_SKILL_CLEANUP_FIELDS = Set.of(
            "success", "skillCount", "nextStartIndex", "observedSlotStatuses",
            "ultimateSkillClicked", "ultimateSkillSucceeded", "inspectedSlotCount",
            "deletedSkillCount", "message");
    private static final Set<String> TRACKER_READ_FIELDS = Set.of(
            "captureId", "readProfile", "source", "allowPanelReposition");
    private static final Set<String> TRACKER_SOURCE_FIELDS = Set.of("kind", "sourceArtifact");
    private static final Set<String> TRACKER_SOURCE_ARTIFACT_FIELDS = Set.of(
            "captureId", "imageSha256", "artifactId");
    private static final Set<String> TRACKER_ARTIFACT_FIELDS = Set.of(
            "artifactId", "artifactDigest");
    private static final Set<String> TRACKER_MATERIALIZE_FIELDS = Set.of(
            "artifact", "observationDigest", "dependencyLease", "selectedLink",
            "preparedOperation", "targetKeyword", "validationPolicy");
    private static final Set<String> TRACKER_DEPENDENCY_FIELDS = Set.of(
            "sourceReadActionId", "sourceReadSemanticAddress", "leaseDigest");
    private static final Set<String> TRACKER_SELECTED_LINK_FIELDS = Set.of(
            "stableIndex", "rect", "click");
    private static final Set<String> TRACKER_POINT_FIELDS = Set.of("x", "y");
    private static final Set<String> TRACKER_READ_OUTCOME_FIELDS = Set.of(
            "captureId", "readProfile", "source", "artifact", "frames",
            "mechanicalFact", "observedWindow");
    private static final Set<String> TRACKER_FRAME_FIELDS = Set.of(
            "ordinal", "role", "imageBytes", "imageSha256", "region");
    private static final Set<String> TRACKER_MECHANICAL_FACT_FIELDS = Set.of(
            "templateId", "taskKey", "templateScore", "titleDisposition",
            "panelFingerprint", "captureOccurrence");
    private static final Set<String> TRACKER_MATERIALIZE_OUTCOME_FIELDS = Set.of(
            "artifact", "observationDigest", "preparedActionId", "publishDisposition",
            "validationFingerprintDigest", "observedWindow");
    private static final Set<String> OBSERVED_WINDOW_FIELDS = Set.of(
            "windowId", "nativeHandle", "processId", "playerIdentityEpoch");
    private static final Set<String> INPUT_ACTION_FIELDS = Set.of(
            "type", "x", "y", "endX", "endY", "delayMs", "intervalMs", "clicks", "text");
    private static final Set<String> KEY_ACTION_FIELDS = Set.of("type");
    private static final Set<String> CLICK_ACTION_FIELDS = Set.of("type", "x", "y", "delayMs");
    private static final Set<String> DOUBLE_CLICK_ACTION_FIELDS = Set.of(
            "type", "x", "y", "delayMs", "intervalMs");
    private static final Set<String> MOVE_ACTION_FIELDS = Set.of("type", "x", "y");
    private static final Set<String> DRAG_ACTION_FIELDS = Set.of("type", "x", "y", "endX", "endY");
    private static final Set<String> TEXT_ACTION_FIELDS = Set.of("type", "text");
    private static final Set<String> SCROLL_ACTION_FIELDS = Set.of("type", "clicks");
    private static final Set<String> SLEEP_ACTION_FIELDS = Set.of("type", "delayMs");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);

    public RemoteCaptureCommandPayload readCapture(JsonNode payload) {
        requireObjectWithFields(payload, CAPTURE_FIELDS, CAPTURE_REQUIRED_FIELDS, "capture payload");
        JsonNode region = payload.get("region");
        requireObjectWithFields(region, CAPTURE_REGION_FIELDS, CAPTURE_REGION_FIELDS, "capture.region");
        RemoteCaptureCommandPayload value = read(payload, RemoteCaptureCommandPayload.class, "capture payload");
        validateSessionRef(payload.get("sessionRef"), value.getSessionRef(), "capture.sessionRef");
        if (value.getCaptureId() == null || value.getCaptureId().isBlank()) {
            throw new RemotePayloadException("captureId must not be blank");
        }
        if (value.getImageFormat() != RemoteCaptureImageFormat.PNG) {
            throw new RemotePayloadException("v1 imageFormat must be PNG");
        }
        if (value.getCapturePurpose() == null) {
            throw new RemotePayloadException("capturePurpose is required");
        }
        if (value.getRegion() == null || value.getRegion().getCoordinateSpace() == null) {
            throw new RemotePayloadException("capture region and coordinateSpace are required");
        }
        if (value.getRegion().getWidth() <= 0 || value.getRegion().getHeight() <= 0) {
            throw new RemotePayloadException("capture width and height must be positive");
        }
        return value;
    }

    public RemoteWindowFactCommandPayload readWindowFact(JsonNode payload) {
        requireObjectWithFields(payload, WINDOW_FACT_FIELDS, WINDOW_FACT_FIELDS, "window fact payload");
        RemoteWindowFactCommandPayload value = read(
                payload, RemoteWindowFactCommandPayload.class, "window fact payload");
        if (value.getFactKind() == null) {
            throw new RemotePayloadException("factKind is required");
        }
        return value;
    }

    public RemoteInputBundleCommandPayload readInputBundle(JsonNode payload) {
        requireObjectWithFields(payload, INPUT_BUNDLE_FIELDS, INPUT_BUNDLE_REQUIRED_FIELDS,
                "input bundle payload");
        JsonNode actionsNode = payload.get("actions");
        if (actionsNode == null || !actionsNode.isArray()) {
            throw new RemotePayloadException("actions must be an array");
        }
        for (JsonNode actionNode : actionsNode) {
            requireObjectWithFields(actionNode, INPUT_ACTION_FIELDS, Set.of("type"), "input action");
            RemoteInputActionType actionType = readActionType(actionNode);
            Set<String> actionFields = actionFields(actionType);
            requireObjectWithFields(
                    actionNode,
                    actionFields,
                    actionFields,
                    "input action " + actionType);
        }
        RemoteInputBundleCommandPayload value = read(
                payload, RemoteInputBundleCommandPayload.class, "input bundle payload");
        validateSessionRef(payload.get("sessionRef"), value.getSessionRef(),
                "input bundle.sessionRef");
        if (value.getDescription() == null || value.getDescription().isBlank()) {
            throw new RemotePayloadException("input description must not be blank");
        }
        if (value.getCoordinateSpace() != RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX
                && value.getCoordinateSpace() != RemoteCoordinateSpace.WINDOW_CLIENT_PX) {
            throw new RemotePayloadException(
                    "v1 input coordinateSpace must be SCREEN_ABSOLUTE_PX or WINDOW_CLIENT_PX");
        }
        List<RemoteInputActionDto> actions = value.getActions();
        if (actions == null || actions.isEmpty() || actions.size() > 256) {
            throw new RemotePayloadException("actions size must be between 1 and 256");
        }
        for (RemoteInputActionDto action : actions) {
            if (action == null) {
                throw new RemotePayloadException("actions must not contain null");
            }
            try {
                action.validate();
            } catch (IllegalArgumentException e) {
                throw new RemotePayloadException("invalid input action: " + e.getMessage(), e);
            }
        }
        return value;
    }

    public RemoteExclusiveInteractionControlCommandPayload readExclusiveInteractionControl(
            JsonNode payload) {
        requireObjectWithFields(payload, EXCLUSIVE_CONTROL_FIELDS, EXCLUSIVE_CONTROL_FIELDS,
                "exclusive interaction control payload");
        RemoteExclusiveInteractionControlCommandPayload value = read(
                payload, RemoteExclusiveInteractionControlCommandPayload.class,
                "exclusive interaction control payload");
        if (value.getCommand() == null || value.getExclusiveSessionId() == null
                || value.getExclusiveSessionId().isBlank()
                || value.getBindingGeneration() < 0L || value.getStep() <= 0L) {
            throw new RemotePayloadException("invalid exclusive interaction control cursor");
        }
        return value;
    }

    public RemoteExclusiveInteractionControlOutcomePayload readExclusiveInteractionControlOutcome(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.EXCLUSIVE_INTERACTION_CONTROL);
        requireObjectWithFields(
                outcome.getPayload(), EXCLUSIVE_CONTROL_OUTCOME_FIELDS,
                EXCLUSIVE_CONTROL_OUTCOME_FIELDS,
                "exclusive interaction control outcome payload");
        RemoteExclusiveInteractionControlOutcomePayload value = read(
                outcome.getPayload(), RemoteExclusiveInteractionControlOutcomePayload.class,
                "exclusive interaction control outcome payload");
        if (value.getCommand() == null || value.getMechanicalStatus() == null
                || value.getExclusiveSessionId() == null
                || value.getExclusiveSessionId().isBlank()
                || value.getBindingGeneration() < 0L || value.getStep() <= 0L) {
            throw new RemotePayloadException("invalid exclusive interaction control outcome");
        }
        RemoteExecutionState expected = switch (value.getMechanicalStatus()) {
            case ACQUIRED, RELEASED, ABORTED -> RemoteExecutionState.EXECUTED;
            case NOT_EXECUTED -> RemoteExecutionState.NOT_EXECUTED;
            case STOPPED -> RemoteExecutionState.STOPPED;
            case UNKNOWN -> RemoteExecutionState.UNKNOWN;
        };
        if (outcome.getExecutionState() != expected) {
            throw new RemotePayloadException(
                    "exclusive control mechanicalStatus does not match executionState");
        }
        boolean commandMatches = switch (value.getCommand()) {
            case ACQUIRE -> value.getMechanicalStatus()
                    != RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.RELEASED
                    && value.getMechanicalStatus()
                    != RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.ABORTED;
            case RELEASE -> value.getMechanicalStatus()
                    != RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.ACQUIRED
                    && value.getMechanicalStatus()
                    != RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.ABORTED;
            case ABORT -> value.getMechanicalStatus()
                    != RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.ACQUIRED
                    && value.getMechanicalStatus()
                    != RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.RELEASED;
        };
        if (!commandMatches) {
            throw new RemotePayloadException(
                    "exclusive control command does not match mechanicalStatus");
        }
        boolean releasedStatus = value.getMechanicalStatus()
                == RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.RELEASED
                || value.getMechanicalStatus()
                == RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.ABORTED;
        boolean terminalStatus = releasedStatus
                || value.getMechanicalStatus()
                == RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.NOT_EXECUTED
                || value.getMechanicalStatus()
                == RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.STOPPED
                || value.getMechanicalStatus()
                == RemoteExclusiveInteractionControlOutcomePayload.MechanicalStatus.UNKNOWN;
        if ((value.isOwnerReleased() && !terminalStatus)
                || (releasedStatus && !value.isOwnerReleased())) {
            throw new RemotePayloadException(
                    "exclusive control ownerReleased does not match mechanicalStatus");
        }
        return value;
    }

    private void validateSessionRef(
            JsonNode node,
            RemoteExclusiveSessionStepRef value,
            String label) {
        if (node == null) {
            if (value != null) {
                throw new RemotePayloadException(label + " must be absent when null");
            }
            return;
        }
        requireObjectWithFields(node, EXCLUSIVE_SESSION_REF_FIELDS,
                EXCLUSIVE_SESSION_REF_FIELDS, label);
        if (value == null || value.getExclusiveSessionId() == null
                || value.getExclusiveSessionId().isBlank()
                || value.getBindingGeneration() < 0L || value.getStep() <= 0L) {
            throw new RemotePayloadException("invalid " + label);
        }
    }

    public RemoteSummonSkillWholePassCommandPayload readSummonSkillWholePass(
            JsonNode payload) {
        requireObjectWithFields(
                payload,
                SUMMON_SKILL_WHOLE_PASS_FIELDS,
                SUMMON_SKILL_WHOLE_PASS_REQUIRED_FIELDS,
                "summon skill whole-pass payload");
        return read(
                payload,
                RemoteSummonSkillWholePassCommandPayload.class,
                "summon skill whole-pass payload");
    }

    public RemoteSummonSkillWholePassOutcomePayload readSummonSkillWholePassOutcome(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.SUMMON_SKILL_WHOLE_PASS);
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                SUMMON_SKILL_WHOLE_PASS_OUTCOME_FIELDS,
                "summon skill whole-pass outcome payload");
        JsonNode cleanup = payload.get("cleanupResult");
        if (cleanup != null && !cleanup.isNull()) {
            requireObjectWithFields(
                    cleanup,
                    SUMMON_SKILL_CLEANUP_FIELDS,
                    SUMMON_SKILL_CLEANUP_FIELDS,
                    "summon skill whole-pass cleanupResult");
        }
        RemoteSummonSkillWholePassOutcomePayload value = read(
                payload,
                RemoteSummonSkillWholePassOutcomePayload.class,
                "summon skill whole-pass outcome payload");
        RemoteExecutionState expectedState = switch (value.getMechanicalStatus()) {
            case EXECUTED -> RemoteExecutionState.EXECUTED;
            case NOT_EXECUTED -> RemoteExecutionState.NOT_EXECUTED;
            case STOPPED -> RemoteExecutionState.STOPPED;
            case UNKNOWN -> RemoteExecutionState.UNKNOWN;
        };
        if (outcome.getExecutionState() != expectedState) {
            throw new RemotePayloadException(
                    "whole-pass mechanicalStatus does not match executionState");
        }
        return value;
    }

    public RemoteLocalMacroCommandPayload readLocalMacro(JsonNode payload) {
        return switch (requireLocalMacroKind(payload, "local macro command payload")) {
            case BAG_RETURN_ITEM -> readBagReturnItemMacro(payload);
            case BAG_USE_INCENSE -> readBagUseIncenseMacro(payload);
            case NAVIGATE_IN_CURRENT_MAP -> readNavigateInCurrentMapMacro(payload);
            case UI_CLEAN -> readUiCleanMacro(payload);
            case DIALOG_DETECTION -> readDialogDetectionMacro(payload);
            case PLAYER_STATE_FIRST_AID -> readPlayerStateFirstAidMacro(payload);
            case DIALOG_PREPARED_ACTION_VALIDATION -> readDialogPreparedActionValidationMacro(payload);
            case DIALOG_OPTION_OCR_IMAGE -> readDialogOptionOcrImageMacro(payload);
            case DIALOG_OPTION_OCR_WORDS -> readDialogOptionOcrWordsMacro(payload);
            case DIALOG_WHITE_STORY_TEMPLATE -> readDialogWhiteStoryTemplateMacro(payload);
        };
    }

    public RemoteDialogWhiteStoryTemplateMacroCommandPayload readDialogWhiteStoryTemplateMacro(JsonNode payload) {
        requireObjectWithFields(
                payload,
                DIALOG_WHITE_STORY_TEMPLATE_COMMAND_FIELDS,
                DIALOG_WHITE_STORY_TEMPLATE_COMMAND_REQUIRED_FIELDS,
                "local macro dialog white story-template payload");
        return read(
                payload,
                RemoteDialogWhiteStoryTemplateMacroCommandPayload.class,
                "local macro dialog white story-template payload");
    }

    public RemoteDialogPreparedActionValidationMacroCommandPayload readDialogPreparedActionValidationMacro(
            JsonNode payload) {
        requireObjectWithFields(
                payload,
                DIALOG_PREPARED_ACTION_COMMAND_FIELDS,
                DIALOG_PREPARED_ACTION_COMMAND_FIELDS,
                "local macro dialog prepared-action validation payload");
        return read(
                payload,
                RemoteDialogPreparedActionValidationMacroCommandPayload.class,
                "local macro dialog prepared-action validation payload");
    }

    public RemotePlayerStateFirstAidMacroCommandPayload readPlayerStateFirstAidMacro(JsonNode payload) {
        requireObjectWithFields(
                payload,
                PLAYER_STATE_FIRST_AID_COMMAND_FIELDS,
                PLAYER_STATE_FIRST_AID_COMMAND_REQUIRED_FIELDS,
                "local macro player-state first-aid payload");
        return read(
                payload,
                RemotePlayerStateFirstAidMacroCommandPayload.class,
                "local macro player-state first-aid payload");
    }

    public RemoteDialogDetectionMacroCommandPayload readDialogDetectionMacro(JsonNode payload) {
        requireObjectWithFields(
                payload,
                DIALOG_DETECTION_COMMAND_FIELDS,
                DIALOG_DETECTION_COMMAND_REQUIRED_FIELDS,
                "local macro dialog-detection payload");
        return read(
                payload,
                RemoteDialogDetectionMacroCommandPayload.class,
                "local macro dialog-detection payload");
    }

    public RemoteDialogOptionOcrImageMacroCommandPayload readDialogOptionOcrImageMacro(JsonNode payload) {
        requireObjectWithFields(
                payload,
                DIALOG_OPTION_OCR_IMAGE_COMMAND_FIELDS,
                DIALOG_OPTION_OCR_IMAGE_COMMAND_REQUIRED_FIELDS,
                "local macro dialog option ocr image payload");
        return read(
                payload,
                RemoteDialogOptionOcrImageMacroCommandPayload.class,
                "local macro dialog option ocr image payload");
    }

    public RemoteDialogOptionOcrWordsMacroCommandPayload readDialogOptionOcrWordsMacro(JsonNode payload) {
        requireObjectWithFields(
                payload,
                DIALOG_OPTION_OCR_WORDS_COMMAND_FIELDS,
                DIALOG_OPTION_OCR_WORDS_COMMAND_REQUIRED_FIELDS,
                "local macro dialog option ocr words payload");
        return read(
                payload,
                RemoteDialogOptionOcrWordsMacroCommandPayload.class,
                "local macro dialog option ocr words payload");
    }

    public RemoteUiCleanMacroCommandPayload readUiCleanMacro(JsonNode payload) {
        requireObjectWithFields(
                payload,
                UI_CLEAN_COMMAND_FIELDS,
                UI_CLEAN_COMMAND_REQUIRED_FIELDS,
                "local macro ui-clean payload");
        return read(
                payload,
                RemoteUiCleanMacroCommandPayload.class,
                "local macro ui-clean payload");
    }

    public RemoteNavigateInCurrentMapMacroCommandPayload readNavigateInCurrentMapMacro(JsonNode payload) {
        requireObjectWithFields(
                payload,
                NAVIGATE_IN_CURRENT_MAP_COMMAND_FIELDS,
                NAVIGATE_IN_CURRENT_MAP_COMMAND_REQUIRED_FIELDS,
                "local macro navigate-in-current-map payload");
        return read(
                payload,
                RemoteNavigateInCurrentMapMacroCommandPayload.class,
                "local macro navigate-in-current-map payload");
    }

    public RemoteBagReturnItemMacroCommandPayload readBagReturnItemMacro(JsonNode payload) {
        requireObjectWithFields(
                payload,
                BAG_RETURN_ITEM_COMMAND_FIELDS,
                BAG_RETURN_ITEM_COMMAND_REQUIRED_FIELDS,
                "local macro bag-return-item payload");
        JsonNode cached = payload.get("cachedPoint");
        if (cached != null && !cached.isNull()) {
            requireObjectWithFields(
                    cached,
                    LOCAL_MACRO_CACHE_POINT_FIELDS,
                    LOCAL_MACRO_CACHE_POINT_FIELDS,
                    "local macro cachedPoint");
        }
        return read(
                payload,
                RemoteBagReturnItemMacroCommandPayload.class,
                "local macro bag-return-item payload");
    }

    public RemoteBagUseIncenseMacroCommandPayload readBagUseIncenseMacro(JsonNode payload) {
        requireObjectWithFields(
                payload,
                BAG_USE_INCENSE_COMMAND_FIELDS,
                BAG_USE_INCENSE_COMMAND_FIELDS,
                "local macro bag-use-incense payload");
        return read(
                payload,
                RemoteBagUseIncenseMacroCommandPayload.class,
                "local macro bag-use-incense payload");
    }

    public RemoteBagReturnItemMacroResultPayload readBagReturnItemMacroResult(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        if (outcome.getExecutionState() != RemoteExecutionState.EXECUTED) {
            throw new RemotePayloadException(
                    "local macro bag-return-item typed result is only present on an EXECUTED envelope");
        }
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_RESULT_FIELDS,
                "local macro bag-return-item result payload");
        JsonNode cachePoint = payload.get("cachePoint");
        if (cachePoint != null && !cachePoint.isNull()) {
            requireObjectWithFields(
                    cachePoint,
                    LOCAL_MACRO_CACHE_POINT_FIELDS,
                    LOCAL_MACRO_CACHE_POINT_FIELDS,
                    "local macro result cachePoint");
        }
        return read(
                payload,
                RemoteBagReturnItemMacroResultPayload.class,
                "local macro bag-return-item result payload");
    }

    public RemoteBagUseIncenseMacroResultPayload readBagUseIncenseMacroResult(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        if (outcome.getExecutionState() != RemoteExecutionState.EXECUTED) {
            throw new RemotePayloadException(
                    "local macro bag-use-incense typed result is only present on an EXECUTED envelope");
        }
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_RESULT_FIELDS,
                "local macro bag-use-incense result payload");
        if (requireLocalMacroKind(payload, "local macro bag-use-incense result payload")
                != RemoteLocalMacroKind.BAG_USE_INCENSE) {
            throw new RemotePayloadException(
                    "local macro bag-use-incense result macroKind must be BAG_USE_INCENSE");
        }
        JsonNode operation = payload.get("operation");
        JsonNode state = payload.get("state");
        JsonNode cachePoint = payload.get("cachePoint");
        if (operation == null || !operation.isNull()
                || cachePoint == null || !cachePoint.isNull()
                || state == null || state.isNull() || !state.isTextual()) {
            throw new RemotePayloadException(
                    "BAG_USE_INCENSE result requires null operation/cachePoint and textual state");
        }
        RemoteBagUseIncenseMacroResultPayload.State typedState;
        try {
            typedState = RemoteBagUseIncenseMacroResultPayload.State.valueOf(state.asText());
        } catch (IllegalArgumentException invalidState) {
            throw new RemotePayloadException(
                    "BAG_USE_INCENSE result state must be USED or NOT_FOUND");
        }
        return RemoteBagUseIncenseMacroResultPayload.builder()
                .macroKind(RemoteLocalMacroKind.BAG_USE_INCENSE)
                .state(typedState)
                .build();
    }

    public RemoteNavigateInCurrentMapMacroResultPayload readNavigateInCurrentMapMacroResult(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        if (outcome.getExecutionState() != RemoteExecutionState.EXECUTED) {
            throw new RemotePayloadException(
                    "local macro navigate-in-current-map typed result is only present on an EXECUTED envelope");
        }
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_RESULT_FIELDS,
                "local macro navigate-in-current-map result payload");
        if (requireLocalMacroKind(payload, "local macro navigate-in-current-map result payload")
                != RemoteLocalMacroKind.NAVIGATE_IN_CURRENT_MAP) {
            throw new RemotePayloadException(
                    "local macro navigate-in-current-map result macroKind must be NAVIGATE_IN_CURRENT_MAP");
        }
        JsonNode operation = payload.get("operation");
        JsonNode state = payload.get("state");
        JsonNode cachePoint = payload.get("cachePoint");
        if (operation == null || !operation.isNull()
                || cachePoint == null || !cachePoint.isNull()
                || state == null || state.isNull() || !state.isTextual()) {
            throw new RemotePayloadException(
                    "NAVIGATE_IN_CURRENT_MAP result requires null operation/cachePoint and textual state");
        }
        RemoteNavigateInCurrentMapMacroResultPayload.State typedState;
        try {
            typedState = RemoteNavigateInCurrentMapMacroResultPayload.State.valueOf(state.asText());
        } catch (IllegalArgumentException invalidState) {
            throw new RemotePayloadException(
                    "NAVIGATE_IN_CURRENT_MAP result state must be a committed NavigationResultStatus value");
        }
        return RemoteNavigateInCurrentMapMacroResultPayload.builder()
                .macroKind(RemoteLocalMacroKind.NAVIGATE_IN_CURRENT_MAP)
                .state(typedState)
                .build();
    }

    public RemoteDialogPreparedActionValidationMacroResultPayload readDialogPreparedActionValidationMacroResult(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        if (outcome.getExecutionState() != RemoteExecutionState.EXECUTED) {
            throw new RemotePayloadException(
                    "local macro dialog prepared-action validation typed result is only present on an EXECUTED envelope");
        }
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_PREPARED_ACTION_RESULT_FIELDS,
                "local macro dialog prepared-action validation result payload");
        if (requireLocalMacroKind(payload, "local macro dialog prepared-action validation result payload")
                != RemoteLocalMacroKind.DIALOG_PREPARED_ACTION_VALIDATION) {
            throw new RemotePayloadException(
                    "local macro dialog prepared-action validation result macroKind must be DIALOG_PREPARED_ACTION_VALIDATION");
        }
        // The typed wire record's own constructor enforces the measured/non-measured metric invariant.
        return read(
                payload,
                RemoteDialogPreparedActionValidationMacroResultPayload.class,
                "local macro dialog prepared-action validation result payload");
    }

    public RemotePlayerStateFirstAidMacroResultPayload readPlayerStateFirstAidMacroResult(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        if (outcome.getExecutionState() != RemoteExecutionState.EXECUTED) {
            throw new RemotePayloadException(
                    "local macro player-state first-aid typed result is only present on an EXECUTED envelope");
        }
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_PLAYER_STATE_RESULT_FIELDS,
                "local macro player-state first-aid result payload");
        if (requireLocalMacroKind(payload, "local macro player-state first-aid result payload")
                != RemoteLocalMacroKind.PLAYER_STATE_FIRST_AID) {
            throw new RemotePayloadException(
                    "local macro player-state first-aid result macroKind must be PLAYER_STATE_FIRST_AID");
        }
        // The typed wire record's own constructor enforces the per-operation variant invariant.
        return read(
                payload,
                RemotePlayerStateFirstAidMacroResultPayload.class,
                "local macro player-state first-aid result payload");
    }

    public RemoteDialogDetectionMacroResultPayload readDialogDetectionMacroResult(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        if (outcome.getExecutionState() != RemoteExecutionState.EXECUTED) {
            throw new RemotePayloadException(
                    "local macro dialog-detection typed result is only present on an EXECUTED envelope");
        }
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_DIALOG_RESULT_FIELDS,
                "local macro dialog-detection result payload");
        if (requireLocalMacroKind(payload, "local macro dialog-detection result payload")
                != RemoteLocalMacroKind.DIALOG_DETECTION) {
            throw new RemotePayloadException(
                    "local macro dialog-detection result macroKind must be DIALOG_DETECTION");
        }
        // The flat wire record's own constructor enforces the CAPTURED-core / non-CAPTURED-null invariant.
        return read(
                payload,
                RemoteDialogDetectionMacroResultPayload.class,
                "local macro dialog-detection result payload");
    }

    public RemoteDialogOptionOcrImageMacroResultPayload readDialogOptionOcrImageMacroResult(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        if (outcome.getExecutionState() != RemoteExecutionState.EXECUTED) {
            throw new RemotePayloadException(
                    "local macro dialog option ocr image typed result is only present on an EXECUTED envelope");
        }
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_DIALOG_OPTION_OCR_IMAGE_RESULT_FIELDS,
                "local macro dialog option ocr image result payload");
        if (requireLocalMacroKind(payload, "local macro dialog option ocr image result payload")
                != RemoteLocalMacroKind.DIALOG_OPTION_OCR_IMAGE) {
            throw new RemotePayloadException(
                    "local macro dialog option ocr image result macroKind must be DIALOG_OPTION_OCR_IMAGE");
        }
        // The flat wire record's own constructor enforces the CAPTURED-evidence / non-CAPTURED-null invariant.
        return read(
                payload,
                RemoteDialogOptionOcrImageMacroResultPayload.class,
                "local macro dialog option ocr image result payload");
    }

    public RemoteDialogWhiteStoryTemplateMacroResultPayload readDialogWhiteStoryTemplateMacroResult(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        if (outcome.getExecutionState() != RemoteExecutionState.EXECUTED) {
            throw new RemotePayloadException(
                    "local macro dialog white story-template typed result is only present on an EXECUTED envelope");
        }
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_DIALOG_WHITE_STORY_TEMPLATE_RESULT_FIELDS,
                "local macro dialog white story-template result payload");
        if (requireLocalMacroKind(payload, "local macro dialog white story-template result payload")
                != RemoteLocalMacroKind.DIALOG_WHITE_STORY_TEMPLATE) {
            throw new RemotePayloadException(
                    "local macro dialog white story-template result macroKind must be DIALOG_WHITE_STORY_TEMPLATE");
        }
        // The flat wire record's own constructor enforces the MATCHED same-frame evidence authority and the
        // STORY_MISS/STORY_ABSENT rect-only invariants.
        return read(
                payload,
                RemoteDialogWhiteStoryTemplateMacroResultPayload.class,
                "local macro dialog white story-template result payload");
    }

    public RemoteDialogOptionOcrWordsMacroResultPayload readDialogOptionOcrWordsMacroResult(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        if (outcome.getExecutionState() != RemoteExecutionState.EXECUTED) {
            throw new RemotePayloadException(
                    "local macro dialog option ocr words typed result is only present on an EXECUTED envelope");
        }
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_DIALOG_OPTION_OCR_WORDS_RESULT_FIELDS,
                "local macro dialog option ocr words result payload");
        if (requireLocalMacroKind(payload, "local macro dialog option ocr words result payload")
                != RemoteLocalMacroKind.DIALOG_OPTION_OCR_WORDS) {
            throw new RemotePayloadException(
                    "local macro dialog option ocr words result macroKind must be DIALOG_OPTION_OCR_WORDS");
        }
        // The flat wire record's own constructor enforces the WORDS-only-carries-boxes invariant.
        return read(
                payload,
                RemoteDialogOptionOcrWordsMacroResultPayload.class,
                "local macro dialog option ocr words result payload");
    }

    public RemoteUiCleanMacroResultPayload readUiCleanMacroResult(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        if (outcome.getExecutionState() != RemoteExecutionState.EXECUTED) {
            throw new RemotePayloadException(
                    "local macro ui-clean typed result is only present on an EXECUTED envelope");
        }
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_RESULT_FIELDS,
                "local macro ui-clean result payload");
        if (requireLocalMacroKind(payload, "local macro ui-clean result payload")
                != RemoteLocalMacroKind.UI_CLEAN) {
            throw new RemotePayloadException(
                    "local macro ui-clean result macroKind must be UI_CLEAN");
        }
        JsonNode operation = payload.get("operation");
        JsonNode state = payload.get("state");
        JsonNode cachePoint = payload.get("cachePoint");
        if (operation == null || operation.isNull() || !operation.isTextual()
                || state == null || state.isNull() || !state.isTextual()
                || cachePoint == null || !cachePoint.isNull()) {
            throw new RemotePayloadException(
                    "UI_CLEAN result requires textual operation/state and null cachePoint");
        }
        RemoteUiCleanMacroCommandPayload.Operation typedOperation;
        RemoteUiCleanMacroResultPayload.State typedState;
        try {
            typedOperation = RemoteUiCleanMacroCommandPayload.Operation.valueOf(operation.asText());
            typedState = RemoteUiCleanMacroResultPayload.State.valueOf(state.asText());
        } catch (IllegalArgumentException invalid) {
            throw new RemotePayloadException(
                    "UI_CLEAN result operation/state must be committed closed values");
        }
        return RemoteUiCleanMacroResultPayload.builder()
                .macroKind(RemoteLocalMacroKind.UI_CLEAN)
                .operation(typedOperation)
                .state(typedState)
                .build();
    }

    /**
     * W-BAG-MACRO-DHXY-WIRE-IMP1-R1: single all-terminal strict boundary for a {@code LOCAL_MACRO}
     * outcome, matching Cloud {@code RemoteCommandOutcomeEnvelope.localMacroOutcome} enforcement so a
     * non-EXECUTED terminal cannot bypass strict validation before a client-side digest is computed.
     * Every execution state must carry exactly the four flat result keys and a closed macroKind.
     * EXECUTED validates the kind-specific result matrix and returns its sealed typed variant;
     * NOT_EXECUTED/STOPPED/UNKNOWN must have null operation/state/cachePoint and return {@code null}.
     * Only the four states {@code EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN} are
     * accepted; {@code OBSERVED} (and a null state) are rejected up front, matching Cloud
     * {@code RemoteCommandOutcomeEnvelope.localMacroOutcome} which refuses {@code OBSERVED} before the
     * digest check.
     */
    public RemoteLocalMacroResultPayload readLocalMacroTerminal(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.LOCAL_MACRO);
        JsonNode payload = outcome.getPayload();
        // macroKind is present in every terminal shape, so read it before choosing the per-kind key
        // contract: an EXECUTED DIALOG_DETECTION terminal uses its own richer flat key set, while
        // BAG/NAV/UI_CLEAN (and every non-EXECUTED terminal) keep the unchanged 4-key shape.
        RemoteLocalMacroKind macroKind = requireLocalMacroKind(
                payload, "local macro terminal outcome payload");
        RemoteExecutionState executionState = outcome.getExecutionState();
        if (executionState != RemoteExecutionState.EXECUTED
                && executionState != RemoteExecutionState.NOT_EXECUTED
                && executionState != RemoteExecutionState.STOPPED
                && executionState != RemoteExecutionState.UNKNOWN) {
            throw new RemotePayloadException(
                    "local macro terminal executionState must be one of "
                            + "EXECUTED, NOT_EXECUTED, STOPPED or UNKNOWN");
        }
        if (executionState == RemoteExecutionState.EXECUTED
                && macroKind == RemoteLocalMacroKind.DIALOG_DETECTION) {
            return readDialogDetectionMacroResult(outcome);
        }
        if (executionState == RemoteExecutionState.EXECUTED
                && macroKind == RemoteLocalMacroKind.PLAYER_STATE_FIRST_AID) {
            return readPlayerStateFirstAidMacroResult(outcome);
        }
        if (executionState == RemoteExecutionState.EXECUTED
                && macroKind == RemoteLocalMacroKind.DIALOG_PREPARED_ACTION_VALIDATION) {
            return readDialogPreparedActionValidationMacroResult(outcome);
        }
        if (executionState == RemoteExecutionState.EXECUTED
                && macroKind == RemoteLocalMacroKind.DIALOG_OPTION_OCR_IMAGE) {
            return readDialogOptionOcrImageMacroResult(outcome);
        }
        if (executionState == RemoteExecutionState.EXECUTED
                && macroKind == RemoteLocalMacroKind.DIALOG_OPTION_OCR_WORDS) {
            return readDialogOptionOcrWordsMacroResult(outcome);
        }
        if (executionState == RemoteExecutionState.EXECUTED
                && macroKind == RemoteLocalMacroKind.DIALOG_WHITE_STORY_TEMPLATE) {
            return readDialogWhiteStoryTemplateMacroResult(outcome);
        }
        requireExactFieldsPresentAllowNull(
                payload,
                LOCAL_MACRO_RESULT_FIELDS,
                "local macro terminal outcome payload");
        if (executionState == RemoteExecutionState.EXECUTED) {
            return switch (macroKind) {
                case BAG_RETURN_ITEM -> readBagReturnItemMacroResult(outcome);
                case BAG_USE_INCENSE -> readBagUseIncenseMacroResult(outcome);
                case NAVIGATE_IN_CURRENT_MAP -> readNavigateInCurrentMapMacroResult(outcome);
                case UI_CLEAN -> readUiCleanMacroResult(outcome);
                case DIALOG_PREPARED_ACTION_VALIDATION -> throw new RemotePayloadException(
                        "EXECUTED DIALOG_PREPARED_ACTION_VALIDATION terminal must use the prepared-action payload shape");
                case DIALOG_DETECTION -> throw new RemotePayloadException(
                        "EXECUTED DIALOG_DETECTION terminal must use the dialog-detection payload shape");
                case PLAYER_STATE_FIRST_AID -> throw new RemotePayloadException(
                        "EXECUTED PLAYER_STATE_FIRST_AID terminal must use the player-state payload shape");
                case DIALOG_OPTION_OCR_IMAGE -> throw new RemotePayloadException(
                        "EXECUTED DIALOG_OPTION_OCR_IMAGE terminal must use the dialog option ocr image payload shape");
                case DIALOG_OPTION_OCR_WORDS -> throw new RemotePayloadException(
                        "EXECUTED DIALOG_OPTION_OCR_WORDS terminal must use the dialog option ocr words payload shape");
                case DIALOG_WHITE_STORY_TEMPLATE -> throw new RemotePayloadException(
                        "EXECUTED DIALOG_WHITE_STORY_TEMPLATE terminal must use the dialog white story-template payload shape");
            };
        }
        JsonNode operation = payload.get("operation");
        JsonNode state = payload.get("state");
        JsonNode cachePoint = payload.get("cachePoint");
        if ((operation != null && !operation.isNull())
                || (state != null && !state.isNull())
                || (cachePoint != null && !cachePoint.isNull())) {
            throw new RemotePayloadException(
                    "non-EXECUTED local macro outcome must have null operation, state and cachePoint");
        }
        return null;
    }

    public RemoteBagReturnItemMacroResultPayload readBagReturnItemMacroTerminal(
            RemoteGameOutcomeEnvelope outcome) {
        if (requireLocalMacroKind(
                        outcome.getPayload(), "local macro bag-return-item terminal outcome payload")
                != RemoteLocalMacroKind.BAG_RETURN_ITEM) {
            throw new RemotePayloadException(
                    "local macro bag-return-item terminal macroKind must be BAG_RETURN_ITEM");
        }
        RemoteLocalMacroResultPayload result = readLocalMacroTerminal(outcome);
        return result == null ? null : (RemoteBagReturnItemMacroResultPayload) result;
    }

    public RemoteTaskTrackerReadCommandPayload readTaskTrackerRead(JsonNode payload) {
        requireObjectWithFields(payload, TRACKER_READ_FIELDS, TRACKER_READ_FIELDS,
                "tracker read payload");
        requireClosedTrackerSource(payload.get("source"), "tracker read source");
        return read(payload, RemoteTaskTrackerReadCommandPayload.class, "tracker read payload");
    }

    public RemoteTaskTrackerMaterializeCommandPayload readTaskTrackerMaterialize(
            RemoteGameCommand command) {
        if (command == null
                || command.getOperation() != RemoteGameOperation.TASK_TRACKER_MATERIALIZE_ACTION) {
            throw new RemotePayloadException(
                    "TASK_TRACKER_MATERIALIZE_ACTION command is required");
        }
        JsonNode payload = command.getPayload();
        requireObjectWithFields(payload, TRACKER_MATERIALIZE_FIELDS, TRACKER_MATERIALIZE_FIELDS,
                "tracker materialize payload");
        requireObjectWithFields(payload.get("artifact"), TRACKER_ARTIFACT_FIELDS,
                TRACKER_ARTIFACT_FIELDS, "tracker materialize artifact");
        requireObjectWithFields(payload.get("dependencyLease"), TRACKER_DEPENDENCY_FIELDS,
                TRACKER_DEPENDENCY_FIELDS, "tracker materialize dependencyLease");
        requireObjectWithFields(payload.get("selectedLink"), TRACKER_SELECTED_LINK_FIELDS,
                TRACKER_SELECTED_LINK_FIELDS, "tracker materialize selectedLink");
        requireObjectWithFields(payload.path("selectedLink").get("rect"), CAPTURE_REGION_FIELDS,
                CAPTURE_REGION_FIELDS, "tracker materialize selectedLink.rect");
        requireObjectWithFields(payload.path("selectedLink").get("click"), TRACKER_POINT_FIELDS,
                TRACKER_POINT_FIELDS, "tracker materialize selectedLink.click");
        RemoteTaskTrackerMaterializeCommandPayload value = read(
                payload, RemoteTaskTrackerMaterializeCommandPayload.class,
                "tracker materialize payload");
        String expectedLease = new RemoteProtocolDigests().computeTaskTrackerLeaseDigest(
                value.getArtifact().getArtifactId(), value.getArtifact().getArtifactDigest(),
                value.getDependencyLease().getSourceReadActionId(),
                value.getDependencyLease().getSourceReadSemanticAddress(),
                command.getActionId(), command.getSemanticAddress());
        if (!expectedLease.equals(value.getDependencyLease().getLeaseDigest())) {
            throw new RemotePayloadException(
                    "dependencyLease.leaseDigest does not match the exact read/materialize identities");
        }
        return value;
    }

    public RemoteTaskTrackerReadOutcomePayload readTaskTrackerReadOutcome(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.TASK_TRACKER_READ);
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(
                payload, TRACKER_READ_OUTCOME_FIELDS, "tracker read outcome payload");
        JsonNode captureId = payload.get("captureId");
        if (captureId == null || !captureId.isTextual() || captureId.textValue().isBlank()) {
            throw new RemotePayloadException("tracker read outcome captureId must not be blank");
        }
        boolean observed = outcome.getExecutionState() == RemoteExecutionState.OBSERVED;
        requireStateNullMatrix(payload, List.of("readProfile", "source", "artifact", "frames",
                "mechanicalFact", "observedWindow"), observed, "tracker read outcome");
        if (observed) {
            requireClosedTrackerSource(payload.get("source"), "tracker read outcome source");
            requireObjectWithFields(payload.get("artifact"), TRACKER_ARTIFACT_FIELDS,
                    TRACKER_ARTIFACT_FIELDS, "tracker read outcome artifact");
            JsonNode frames = payload.get("frames");
            if (!frames.isArray() || frames.isEmpty() || frames.size() > 2) {
                throw new RemotePayloadException("tracker read outcome frames size must be 1 or 2");
            }
            for (JsonNode frame : frames) {
                requireObjectWithFields(frame, TRACKER_FRAME_FIELDS, TRACKER_FRAME_FIELDS,
                        "tracker read outcome frame");
                requireObjectWithFields(frame.get("region"), CAPTURE_REGION_FIELDS,
                        CAPTURE_REGION_FIELDS, "tracker read outcome frame.region");
            }
            requireObjectWithFields(payload.get("mechanicalFact"),
                    TRACKER_MECHANICAL_FACT_FIELDS,
                    Set.of("templateId", "templateScore", "titleDisposition",
                            "panelFingerprint", "captureOccurrence"),
                    "tracker read outcome mechanicalFact");
            requireObjectWithFields(payload.get("observedWindow"), OBSERVED_WINDOW_FIELDS,
                    OBSERVED_WINDOW_FIELDS, "tracker read outcome observedWindow");
        }
        RemoteTaskTrackerReadOutcomePayload value = read(
                payload, RemoteTaskTrackerReadOutcomePayload.class,
                "tracker read outcome payload");
        if (observed && value.getMechanicalFact().getCaptureOccurrence()
                != outcome.getSemanticAddress().getOccurrence()) {
            throw new RemotePayloadException(
                    "tracker read captureOccurrence must match semanticAddress.occurrence");
        }
        return value;
    }

    public RemoteTaskTrackerMaterializeOutcomePayload readTaskTrackerMaterializeOutcome(
            RemoteGameOutcomeEnvelope outcome) {
        requireOutcomeOperation(outcome, RemoteGameOperation.TASK_TRACKER_MATERIALIZE_ACTION);
        JsonNode payload = outcome.getPayload();
        requireExactFieldsPresentAllowNull(payload, TRACKER_MATERIALIZE_OUTCOME_FIELDS,
                "tracker materialize outcome payload");
        RemoteExecutionState state = outcome.getExecutionState();
        if (state == RemoteExecutionState.EXECUTED) {
            requireStateNullMatrix(payload, List.copyOf(TRACKER_MATERIALIZE_OUTCOME_FIELDS), true,
                    "EXECUTED tracker materialize outcome");
        } else if (state == RemoteExecutionState.NOT_EXECUTED) {
            requireStateNullMatrix(payload,
                    List.of("artifact", "observationDigest", "publishDisposition"), true,
                    "NOT_EXECUTED tracker materialize outcome");
            requireStateNullMatrix(payload,
                    List.of("preparedActionId", "validationFingerprintDigest", "observedWindow"),
                    false, "NOT_EXECUTED tracker materialize outcome");
        } else {
            if (state == RemoteExecutionState.OBSERVED) {
                throw new RemotePayloadException("tracker materialize outcome cannot be OBSERVED");
            }
            requireStateNullMatrix(payload, List.copyOf(TRACKER_MATERIALIZE_OUTCOME_FIELDS), false,
                    "UNKNOWN or STOPPED tracker materialize outcome");
        }
        if (!payload.get("artifact").isNull()) {
            requireObjectWithFields(payload.get("artifact"), TRACKER_ARTIFACT_FIELDS,
                    TRACKER_ARTIFACT_FIELDS, "tracker materialize outcome artifact");
        }
        if (!payload.get("observedWindow").isNull()) {
            requireObjectWithFields(payload.get("observedWindow"), OBSERVED_WINDOW_FIELDS,
                    OBSERVED_WINDOW_FIELDS, "tracker materialize outcome observedWindow");
        }
        RemoteTaskTrackerMaterializeOutcomePayload value = read(
                payload, RemoteTaskTrackerMaterializeOutcomePayload.class,
                "tracker materialize outcome payload");
        validateMaterializeDisposition(state, value.getPublishDisposition());
        return value;
    }

    private static RemoteLocalMacroKind requireLocalMacroKind(JsonNode payload, String label) {
        if (payload == null || !payload.isObject()) {
            throw new RemotePayloadException(label + " must be an object");
        }
        JsonNode macroKind = payload.get("macroKind");
        if (macroKind == null || !macroKind.isTextual()) {
            throw new RemotePayloadException(label + ".macroKind must be a string enum");
        }
        try {
            return RemoteLocalMacroKind.valueOf(macroKind.textValue());
        } catch (IllegalArgumentException invalidKind) {
            throw new RemotePayloadException(label + ".macroKind is unsupported", invalidKind);
        }
    }

    private static RemoteInputActionType readActionType(JsonNode actionNode) {
        JsonNode typeNode = actionNode.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            throw new RemotePayloadException("input action type must be a string enum");
        }
        try {
            return RemoteInputActionType.valueOf(typeNode.textValue());
        } catch (IllegalArgumentException e) {
            throw new RemotePayloadException("unsupported input action type " + typeNode.textValue(), e);
        }
    }

    private void requireClosedTrackerSource(JsonNode source, String label) {
        requireObjectWithFields(source, TRACKER_SOURCE_FIELDS, Set.of("kind"), label);
        JsonNode kind = source.get("kind");
        if (kind == null || !kind.isTextual()) {
            throw new RemotePayloadException(label + ".kind must be a string enum");
        }
        RemoteTaskTrackerReadCommandPayload.SourceKind sourceKind;
        try {
            sourceKind = RemoteTaskTrackerReadCommandPayload.SourceKind.valueOf(kind.textValue());
        } catch (IllegalArgumentException e) {
            throw new RemotePayloadException(label + ".kind is unsupported", e);
        }
        if (sourceKind == RemoteTaskTrackerReadCommandPayload.SourceKind.LIVE_BOUND_WINDOW) {
            if (source.has("sourceArtifact")) {
                throw new RemotePayloadException(label + " forbids sourceArtifact for live source");
            }
        } else {
            requireObjectWithFields(source.get("sourceArtifact"), TRACKER_SOURCE_ARTIFACT_FIELDS,
                    TRACKER_SOURCE_ARTIFACT_FIELDS, label + ".sourceArtifact");
        }
    }

    private static void requireOutcomeOperation(
            RemoteGameOutcomeEnvelope outcome,
            RemoteGameOperation expected) {
        if (outcome == null || outcome.getOperation() != expected) {
            throw new RemotePayloadException(expected + " outcome is required");
        }
    }

    private static void validateMaterializeDisposition(
            RemoteExecutionState state,
            RemoteTaskTrackerMaterializeOutcomePayload.PublishDisposition disposition) {
        if (state == RemoteExecutionState.EXECUTED
                && disposition != RemoteTaskTrackerMaterializeOutcomePayload.PublishDisposition.PUBLISHED
                && disposition != RemoteTaskTrackerMaterializeOutcomePayload.PublishDisposition.ALREADY_PUBLISHED) {
            throw new RemotePayloadException(
                    "EXECUTED tracker materialize requires a published disposition");
        }
        if (state == RemoteExecutionState.NOT_EXECUTED
                && disposition != RemoteTaskTrackerMaterializeOutcomePayload.PublishDisposition.DEPENDENCY_NOT_READY
                && disposition != RemoteTaskTrackerMaterializeOutcomePayload.PublishDisposition.STALE
                && disposition != RemoteTaskTrackerMaterializeOutcomePayload.PublishDisposition.SAFETY_REJECTED) {
            throw new RemotePayloadException(
                    "NOT_EXECUTED tracker materialize requires a fail-closed disposition");
        }
    }

    private static Set<String> actionFields(RemoteInputActionType actionType) {
        return switch (actionType) {
            case CLICK_LEFT, CLICK_RIGHT -> CLICK_ACTION_FIELDS;
            case DOUBLE_RIGHT_CLICK -> DOUBLE_CLICK_ACTION_FIELDS;
            case MOVE_MOUSE -> MOVE_ACTION_FIELDS;
            case DRAG_AND_DROP -> DRAG_ACTION_FIELDS;
            case TYPE_TEXT_UNICODE, PASTE_TEXT -> TEXT_ACTION_FIELDS;
            case SCROLL_DOWN, SCROLL_UP -> SCROLL_ACTION_FIELDS;
            case SLEEP -> SLEEP_ACTION_FIELDS;
            default -> KEY_ACTION_FIELDS;
        };
    }

    public JsonNode toPayloadTree(Object payload) {
        if (payload == null) {
            throw new RemotePayloadException("outcome payload is required");
        }
        JsonNode tree = objectMapper.valueToTree(payload);
        if (!tree.isObject()) {
            throw new RemotePayloadException("outcome payload must serialize as an object");
        }
        return tree;
    }

    private <T> T read(JsonNode payload, Class<T> type, String label) {
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RemotePayloadException("invalid " + label + ": " + e.getMessage(), e);
        }
    }

    private static void requireObjectWithFields(
            JsonNode node,
            Set<String> allowedFields,
            Set<String> requiredFields,
            String label) {
        if (node == null || !node.isObject()) {
            throw new RemotePayloadException(label + " must be an object");
        }
        node.fieldNames().forEachRemaining(field -> {
            if (!allowedFields.contains(field)) {
                throw new RemotePayloadException(label + " contains unknown field " + field);
            }
        });
        for (String field : requiredFields) {
            if (!node.has(field) || node.get(field).isNull()) {
                throw new RemotePayloadException(label + " requires field " + field);
            }
        }
    }

    private static void requireExactFieldsPresentAllowNull(
            JsonNode node,
            Set<String> fields,
            String label) {
        if (node == null || !node.isObject()) {
            throw new RemotePayloadException(label + " must be an object");
        }
        Set<String> present = new java.util.HashSet<>();
        node.fieldNames().forEachRemaining(present::add);
        if (!present.equals(fields)) {
            throw new RemotePayloadException(label + " must contain exactly " + fields);
        }
    }

    private static void requireStateNullMatrix(
            JsonNode node,
            List<String> fields,
            boolean required,
            String label) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (required && (value == null || value.isNull())) {
                throw new RemotePayloadException(label + " requires non-null " + field);
            }
            if (!required && (value == null || !value.isNull())) {
                throw new RemotePayloadException(label + " requires explicit null " + field);
            }
        }
    }
}
