package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.erdtman.jcs.NumberToJSON;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Canonical digest support for the integral-number subset used by the remote protocol. */
public final class RemoteProtocolDigests {

    public static final String ZERO_SHA256 = "0".repeat(64);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * Rebuilds the cloud typed request tree, excluding context.requestDigest, before hashing.
     *
     * @param command flat transport command envelope carrying one operation payload
     * @return lowercase SHA-256 hex over canonical UTF-8 JSON
     */
    public String computeRequestDigest(RemoteGameCommand command) {
        if (command == null || command.getPayload() == null || !command.getPayload().isObject()) {
            throw new IllegalArgumentException("command with object payload is required");
        }
        if (command.getRunRevision() == null || command.getRunRevision() < 0L) {
            throw new IllegalArgumentException("command.runRevision must be a non-negative long");
        }
        RemoteExclusiveInteractionControlCommandPayload exclusiveControl = null;
        if (command.getOperation() == RemoteGameOperation.SUMMON_SKILL_WHOLE_PASS) {
            new RemoteOperationPayloadCodec().readSummonSkillWholePass(command.getPayload());
        } else if (command.getOperation()
                == RemoteGameOperation.EXCLUSIVE_INTERACTION_CONTROL) {
            exclusiveControl = new RemoteOperationPayloadCodec().readExclusiveInteractionControl(
                    command.getPayload());
        }
        ObjectNode request = objectMapper.createObjectNode();
        ObjectNode context = objectMapper.createObjectNode();
        context.put("contractVersion", command.getContractVersion());
        context.put("operation", command.getOperation().name());
        context.put("requestId", command.getRequestId());
        context.put("actionId", command.getActionId());
        context.put("taskRunId", command.getTaskRunId());
        context.put("runRevision", command.getRunRevision());
        // NON_NULL canonical parity with the cloud mapper: the key exists only when present.
        if (command.getObservationMode() != null) {
            context.put("observationMode", command.getObservationMode().name());
        }
        if (command.getSemanticAddress() == null) {
            throw new IllegalArgumentException("command.semanticAddress is required");
        }
        if (exclusiveControl != null) {
            String expectedSuffix = ":exclusive-"
                    + exclusiveControl.getCommand().name().toLowerCase(java.util.Locale.ROOT);
            if (!command.getSemanticAddress().getActionSlot().endsWith(expectedSuffix)) {
                throw new IllegalArgumentException(
                        "exclusive control command does not match its retained child slot");
            }
        }
        context.set("semanticAddress", objectMapper.valueToTree(command.getSemanticAddress()));
        context.set("window", objectMapper.valueToTree(command.getWindow()));
        context.set("stop", objectMapper.valueToTree(command.getStop()));
        context.put("timeoutMs", command.getTimeoutMs());
        request.set("context", context);
        if (command.getOperation() == RemoteGameOperation.LOCAL_MACRO) {
            RemoteLocalMacroCommandPayload macro =
                    new RemoteOperationPayloadCodec().readLocalMacro(command.getPayload());
            request.put("macroKind", macro.getMacroKind().name());
            if (macro instanceof RemoteBagReturnItemMacroCommandPayload bagMacro) {
                ObjectNode bagReturnItem = objectMapper.createObjectNode();
                bagReturnItem.put("operation", bagMacro.getOperation().name());
                bagReturnItem.put("templatePath", bagMacro.getTemplatePath());
                bagReturnItem.put("maxBackPage", bagMacro.getMaxBackPage());
                bagReturnItem.put("source", bagMacro.getSource());
                if (bagMacro.getCachedPoint() != null) {
                    bagReturnItem.set("cachedPoint", cachePointTree(
                            bagMacro.getCachedPoint().getTemplatePath(),
                            bagMacro.getCachedPoint().getClickX(),
                            bagMacro.getCachedPoint().getClickY(),
                            bagMacro.getCachedPoint().getLearnedAtMs(),
                            bagMacro.getCachedPoint().getSource()));
                }
                request.set("bagReturnItem", bagReturnItem);
            } else if (macro instanceof RemoteBagUseIncenseMacroCommandPayload) {
                request.set("bagUseIncense", objectMapper.createObjectNode());
            } else if (macro instanceof RemoteNavigateInCurrentMapMacroCommandPayload navMacro) {
                ObjectNode navigateInCurrentMap = objectMapper.createObjectNode();
                // Mirror the Cloud NON_NULL canonical: nullable fields are omitted when null.
                if (navMacro.getTargetMapName() != null) {
                    navigateInCurrentMap.put("targetMapName", navMacro.getTargetMapName());
                }
                if (navMacro.getTargetX() != null) {
                    navigateInCurrentMap.put("targetX", navMacro.getTargetX());
                }
                if (navMacro.getTargetY() != null) {
                    navigateInCurrentMap.put("targetY", navMacro.getTargetY());
                }
                if (navMacro.getTargetName() != null) {
                    navigateInCurrentMap.put("targetName", navMacro.getTargetName());
                }
                navigateInCurrentMap.put("randomizeMiniMapClickPoint", navMacro.isRandomizeMiniMapClickPoint());
                navigateInCurrentMap.put("miniMapClickRandomRadiusPx", navMacro.getMiniMapClickRandomRadiusPx());
                navigateInCurrentMap.put("keepTurnOnCurrentMapPathing", navMacro.isKeepTurnOnCurrentMapPathing());
                navigateInCurrentMap.put("arrivalTolerance", navMacro.getArrivalTolerance());
                navigateInCurrentMap.put("source", navMacro.getSource());
                if (navMacro.getFreshCurrentMapName() != null) {
                    navigateInCurrentMap.put("freshCurrentMapName", navMacro.getFreshCurrentMapName());
                }
                if (navMacro.getFreshCurrentX() != null) {
                    navigateInCurrentMap.put("freshCurrentX", navMacro.getFreshCurrentX());
                }
                if (navMacro.getFreshCurrentY() != null) {
                    navigateInCurrentMap.put("freshCurrentY", navMacro.getFreshCurrentY());
                }
                navigateInCurrentMap.put("freshCurrentLocationAtMs", navMacro.getFreshCurrentLocationAtMs());
                navigateInCurrentMap.put("freshCurrentLocationPhaseBound", navMacro.isFreshCurrentLocationPhaseBound());
                request.set("navigateInCurrentMap", navigateInCurrentMap);
            } else if (macro instanceof RemoteUiCleanMacroCommandPayload uiCleanMacro) {
                ObjectNode uiClean = objectMapper.createObjectNode();
                uiClean.put("operation", uiCleanMacro.getOperation().name());
                if (uiCleanMacro.getSource() != null) {
                    uiClean.put("source", uiCleanMacro.getSource());
                }
                request.set("uiClean", uiClean);
            } else if (macro instanceof RemoteDialogDetectionMacroCommandPayload dialogMacro) {
                // Mirror the Cloud NON_NULL valueToTree of DialogDetectionMacroCommand: the optional
                // source is omitted when null, the two primitives are always present.
                ObjectNode dialogDetection = objectMapper.createObjectNode();
                if (dialogMacro.getSource() != null) {
                    dialogDetection.put("source", dialogMacro.getSource());
                }
                dialogDetection.put("hidePlayerNames", dialogMacro.isHidePlayerNames());
                dialogDetection.put("waitBeforeCaptureMs", dialogMacro.getWaitBeforeCaptureMs());
                request.set("dialogDetection", dialogDetection);
            } else if (macro instanceof RemotePlayerStateFirstAidMacroCommandPayload psMacro) {
                // Mirror the Cloud NON_NULL valueToTree of PlayerStateFirstAidMacroCommand: only the
                // chosen operation's variant fields appear (four toggles for probe/heal, or plan base +
                // ordered targets for cached-plan); the command carries no macroKind in this subtree.
                ObjectNode playerStateFirstAid = objectMapper.createObjectNode();
                playerStateFirstAid.put("operation", psMacro.getOperation().name());
                if (psMacro.getPlayerHp() != null) {
                    playerStateFirstAid.set("playerHp", firstAidToggleTree(psMacro.getPlayerHp()));
                }
                if (psMacro.getPlayerMp() != null) {
                    playerStateFirstAid.set("playerMp", firstAidToggleTree(psMacro.getPlayerMp()));
                }
                if (psMacro.getPetHp() != null) {
                    playerStateFirstAid.set("petHp", firstAidToggleTree(psMacro.getPetHp()));
                }
                if (psMacro.getPetMp() != null) {
                    playerStateFirstAid.set("petMp", firstAidToggleTree(psMacro.getPetMp()));
                }
                if (psMacro.getPlanBaseX() != null) {
                    playerStateFirstAid.put("planBaseX", psMacro.getPlanBaseX());
                }
                if (psMacro.getPlanBaseY() != null) {
                    playerStateFirstAid.put("planBaseY", psMacro.getPlanBaseY());
                }
                if (psMacro.getTargets() != null) {
                    ArrayNode targets = objectMapper.createArrayNode();
                    for (RemotePlayerStateFirstAidMacroCommandPayload.RemoteCachedFirstAidTarget target
                            : psMacro.getTargets()) {
                        ObjectNode targetNode = objectMapper.createObjectNode();
                        targetNode.put("name", target.getName());
                        targetNode.put("relX", target.getRelX());
                        targetNode.put("relY", target.getRelY());
                        targetNode.put("threshold", target.getThreshold());
                        targets.add(targetNode);
                    }
                    playerStateFirstAid.set("targets", targets);
                }
                request.set("playerStateFirstAid", playerStateFirstAid);
            } else if (macro instanceof RemoteDialogPreparedActionValidationMacroCommandPayload dpavMacro) {
                // Mirror the Cloud valueToTree of DialogPreparedActionValidationMacroCommand: every field is
                // always present; the command subtree carries no macroKind.
                ObjectNode dialogPreparedActionValidation = objectMapper.createObjectNode();
                dialogPreparedActionValidation.put("validationLeft", dpavMacro.getValidationLeft());
                dialogPreparedActionValidation.put("validationTop", dpavMacro.getValidationTop());
                dialogPreparedActionValidation.put("validationRight", dpavMacro.getValidationRight());
                dialogPreparedActionValidation.put("validationBottom", dpavMacro.getValidationBottom());
                dialogPreparedActionValidation.put("washMode", dpavMacro.getWashMode().name());
                dialogPreparedActionValidation.put("expectedFingerprint", dpavMacro.getExpectedFingerprint());
                dialogPreparedActionValidation.put("maxDistance", dpavMacro.getMaxDistance());
                request.set("dialogPreparedActionValidation", dialogPreparedActionValidation);
            } else if (macro instanceof RemoteDialogOptionOcrImageMacroCommandPayload ocrImageMacro) {
                // Mirror the Cloud NON_NULL valueToTree of DialogOptionOcrImageMacroCommand with the supplied
                // frame bytes excluded (same as the request-digest byte strip); the supplied-frame SHA-256
                // covers the excluded bytes' integrity. The SHA, rect quad and source appear only when
                // non-null; a fresh-capture-at-default command carries an empty subtree.
                ObjectNode dialogOptionOcrImage = objectMapper.createObjectNode();
                if (ocrImageMacro.getSuppliedFrameSha256() != null) {
                    dialogOptionOcrImage.put("suppliedFrameSha256", ocrImageMacro.getSuppliedFrameSha256());
                }
                if (ocrImageMacro.getRectLeft() != null) {
                    dialogOptionOcrImage.put("rectLeft", ocrImageMacro.getRectLeft());
                    dialogOptionOcrImage.put("rectTop", ocrImageMacro.getRectTop());
                    dialogOptionOcrImage.put("rectRight", ocrImageMacro.getRectRight());
                    dialogOptionOcrImage.put("rectBottom", ocrImageMacro.getRectBottom());
                }
                if (ocrImageMacro.getSource() != null) {
                    dialogOptionOcrImage.put("source", ocrImageMacro.getSource());
                }
                request.set("dialogOptionOcrImage", dialogOptionOcrImage);
            } else if (macro instanceof RemoteDialogOptionOcrWordsMacroCommandPayload ocrWordsMacro) {
                // Mirror the Cloud NON_NULL valueToTree of DialogOptionOcrWordsMacroCommand with the variant
                // bytes excluded (same as the request-digest byte strip); variantSha256 covers the bytes'
                // integrity. Source appears only when non-null; every other field is always present.
                ObjectNode dialogOptionOcrWords = objectMapper.createObjectNode();
                dialogOptionOcrWords.put("variant", ocrWordsMacro.getVariant().name());
                dialogOptionOcrWords.put("variantSha256", ocrWordsMacro.getVariantSha256());
                dialogOptionOcrWords.put("imageWidth", ocrWordsMacro.getImageWidth());
                dialogOptionOcrWords.put("imageHeight", ocrWordsMacro.getImageHeight());
                dialogOptionOcrWords.put("rectLeft", ocrWordsMacro.getRectLeft());
                dialogOptionOcrWords.put("rectTop", ocrWordsMacro.getRectTop());
                dialogOptionOcrWords.put("rectRight", ocrWordsMacro.getRectRight());
                dialogOptionOcrWords.put("rectBottom", ocrWordsMacro.getRectBottom());
                if (ocrWordsMacro.getSource() != null) {
                    dialogOptionOcrWords.put("source", ocrWordsMacro.getSource());
                }
                request.set("dialogOptionOcrWords", dialogOptionOcrWords);
            } else if (macro instanceof RemoteDialogWhiteStoryTemplateMacroCommandPayload whiteStoryMacro) {
                // Mirror the Cloud NON_NULL valueToTree of DialogWhiteStoryTemplateMacroCommand with the
                // supplied frame bytes excluded (same as the request-digest byte strip); suppliedFrameSha256
                // covers the excluded bytes' integrity. specs (caller order, each entry's name only when
                // non-null) and absentAllowed are always present; source, the SHA, the rect quad and the type
                // appear only when non-null. The command subtree carries no macroKind.
                ObjectNode dialogWhiteStoryTemplate = objectMapper.createObjectNode();
                ArrayNode specs = objectMapper.createArrayNode();
                if (whiteStoryMacro.getSpecs() != null) {
                    for (RemoteDialogWhiteStoryTemplateMacroCommandPayload.WhiteTemplateSpecEntry spec
                            : whiteStoryMacro.getSpecs()) {
                        ObjectNode specNode = objectMapper.createObjectNode();
                        if (spec.getName() != null) {
                            specNode.put("name", spec.getName());
                        }
                        specNode.put("templatePath", spec.getTemplatePath());
                        specs.add(specNode);
                    }
                }
                dialogWhiteStoryTemplate.set("specs", specs);
                dialogWhiteStoryTemplate.put("absentAllowed", whiteStoryMacro.isAbsentAllowed());
                if (whiteStoryMacro.getSource() != null) {
                    dialogWhiteStoryTemplate.put("source", whiteStoryMacro.getSource());
                }
                if (whiteStoryMacro.getSuppliedFrameSha256() != null) {
                    dialogWhiteStoryTemplate.put("suppliedFrameSha256", whiteStoryMacro.getSuppliedFrameSha256());
                }
                if (whiteStoryMacro.getSuppliedFrameLeft() != null) {
                    dialogWhiteStoryTemplate.put("suppliedFrameLeft", whiteStoryMacro.getSuppliedFrameLeft());
                    dialogWhiteStoryTemplate.put("suppliedFrameTop", whiteStoryMacro.getSuppliedFrameTop());
                    dialogWhiteStoryTemplate.put("suppliedFrameRight", whiteStoryMacro.getSuppliedFrameRight());
                    dialogWhiteStoryTemplate.put("suppliedFrameBottom", whiteStoryMacro.getSuppliedFrameBottom());
                }
                if (whiteStoryMacro.getSuppliedFrameType() != null) {
                    dialogWhiteStoryTemplate.put("suppliedFrameType", whiteStoryMacro.getSuppliedFrameType().name());
                }
                request.set("dialogWhiteStoryTemplate", dialogWhiteStoryTemplate);
            }
        } else {
            mergeNonNullFields(request, command.getPayload(), false);
        }
        return sha256Hex(canonicalJson(request).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Rebuilds the cloud typed outcome tree, excluding common.outcomeDigest and capture image bytes.
     *
     * @param outcome flat transport outcome envelope
     * @return lowercase SHA-256 hex over canonical UTF-8 JSON
     */
    public String computeOutcomeDigest(RemoteGameOutcomeEnvelope outcome) {
        if (outcome == null || outcome.getPayload() == null || !outcome.getPayload().isObject()) {
            throw new IllegalArgumentException("outcome with object payload is required");
        }
        if (outcome.getOperation() == RemoteGameOperation.SUMMON_SKILL_WHOLE_PASS) {
            new RemoteOperationPayloadCodec().readSummonSkillWholePassOutcome(outcome);
        } else if (outcome.getOperation()
                == RemoteGameOperation.EXCLUSIVE_INTERACTION_CONTROL) {
            new RemoteOperationPayloadCodec().readExclusiveInteractionControlOutcome(outcome);
        }
        ObjectNode typedOutcome = objectMapper.createObjectNode();
        ObjectNode common = objectMapper.createObjectNode();
        common.put("contractVersion", outcome.getContractVersion());
        common.put("operation", outcome.getOperation().name());
        common.put("requestId", outcome.getRequestId());
        common.put("actionId", outcome.getActionId());
        common.put("taskRunId", outcome.getTaskRunId());
        common.put("requestDigest", outcome.getRequestDigest());
        common.put("executionState", outcome.getExecutionState().name());
        common.put("code", outcome.getCode().name());
        if (outcome.getMessage() != null) {
            common.put("message", outcome.getMessage());
        }
        common.put("acceptedAtEpochMs", outcome.getAcceptedAtEpochMs());
        common.put("finishedAtEpochMs", outcome.getFinishedAtEpochMs());
        typedOutcome.set("common", common);
        if (outcome.getOperation() == RemoteGameOperation.LOCAL_MACRO) {
            RemoteLocalMacroResultPayload result =
                    new RemoteOperationPayloadCodec().readLocalMacroTerminal(outcome);
            RemoteLocalMacroKind macroKind = RemoteLocalMacroKind.valueOf(
                    outcome.getPayload().get("macroKind").asText());
            typedOutcome.put("macroKind", macroKind.name());
            if (result instanceof RemoteBagReturnItemMacroResultPayload bagResult) {
                ObjectNode bagReturnItem = objectMapper.createObjectNode();
                bagReturnItem.put("operation", bagResult.getOperation().name());
                bagReturnItem.put("state", bagResult.getState().name());
                if (bagResult.getCachePoint() != null) {
                    bagReturnItem.set("cachePoint", cachePointTree(
                            bagResult.getCachePoint().getTemplatePath(),
                            bagResult.getCachePoint().getClickX(),
                            bagResult.getCachePoint().getClickY(),
                            bagResult.getCachePoint().getLearnedAtMs(),
                            bagResult.getCachePoint().getSource()));
                }
                typedOutcome.set("bagReturnItem", bagReturnItem);
            } else if (result instanceof RemoteBagUseIncenseMacroResultPayload incenseResult) {
                ObjectNode bagUseIncense = objectMapper.createObjectNode();
                bagUseIncense.put("state", incenseResult.getState().name());
                typedOutcome.set("bagUseIncense", bagUseIncense);
            } else if (result instanceof RemoteNavigateInCurrentMapMacroResultPayload navResult) {
                ObjectNode navigateInCurrentMap = objectMapper.createObjectNode();
                navigateInCurrentMap.put("state", navResult.getState().name());
                typedOutcome.set("navigateInCurrentMap", navigateInCurrentMap);
            } else if (result instanceof RemoteUiCleanMacroResultPayload uiCleanResult) {
                ObjectNode uiClean = objectMapper.createObjectNode();
                uiClean.put("operation", uiCleanResult.getOperation().name());
                uiClean.put("state", uiCleanResult.getState().name());
                typedOutcome.set("uiClean", uiClean);
            } else if (result instanceof RemoteDialogDetectionMacroResultPayload dialogResult) {
                // Mirror the Cloud NON_NULL valueToTree of DialogDetectionMacroResult: only non-null
                // fields appear, and framePngBytes is excluded from the digest exactly like capture
                // imageBytes (frameSha256 already covers the frame's integrity).
                ObjectNode dialogDetection = objectMapper.createObjectNode();
                dialogDetection.put("state", dialogResult.getState().name());
                if (dialogResult.getDialogType() != null) {
                    dialogDetection.put("dialogType", dialogResult.getDialogType().name());
                }
                if (dialogResult.getDialogLeft() != null) {
                    dialogDetection.put("dialogLeft", dialogResult.getDialogLeft());
                }
                if (dialogResult.getDialogTop() != null) {
                    dialogDetection.put("dialogTop", dialogResult.getDialogTop());
                }
                if (dialogResult.getDialogRight() != null) {
                    dialogDetection.put("dialogRight", dialogResult.getDialogRight());
                }
                if (dialogResult.getDialogBottom() != null) {
                    dialogDetection.put("dialogBottom", dialogResult.getDialogBottom());
                }
                if (dialogResult.getFrameSha256() != null) {
                    dialogDetection.put("frameSha256", dialogResult.getFrameSha256());
                }
                if (dialogResult.getFrameWidth() != null) {
                    dialogDetection.put("frameWidth", dialogResult.getFrameWidth());
                }
                if (dialogResult.getFrameHeight() != null) {
                    dialogDetection.put("frameHeight", dialogResult.getFrameHeight());
                }
                if (dialogResult.getMaskStddev() != null) {
                    dialogDetection.put("maskStddev", dialogResult.getMaskStddev());
                }
                if (dialogResult.getOptionGreenCount() != null) {
                    dialogDetection.put("optionGreenCount", dialogResult.getOptionGreenCount());
                }
                if (dialogResult.getStoryThinWhiteCount() != null) {
                    dialogDetection.put("storyThinWhiteCount", dialogResult.getStoryThinWhiteCount());
                }
                if (dialogResult.getStoryGreenCount() != null) {
                    dialogDetection.put("storyGreenCount", dialogResult.getStoryGreenCount());
                }
                if (dialogResult.getStoryTextMatched() != null) {
                    dialogDetection.put("storyTextMatched", dialogResult.getStoryTextMatched());
                }
                if (dialogResult.getStoryQualifyingRows() != null) {
                    dialogDetection.put("storyQualifyingRows", dialogResult.getStoryQualifyingRows());
                }
                if (dialogResult.getStoryMaxWhitePixelsInRow() != null) {
                    dialogDetection.put("storyMaxWhitePixelsInRow", dialogResult.getStoryMaxWhitePixelsInRow());
                }
                if (dialogResult.getStoryMaxClustersInRow() != null) {
                    dialogDetection.put("storyMaxClustersInRow", dialogResult.getStoryMaxClustersInRow());
                }
                if (dialogResult.getStoryMaxSpanInRow() != null) {
                    dialogDetection.put("storyMaxSpanInRow", dialogResult.getStoryMaxSpanInRow());
                }
                typedOutcome.set("dialogDetection", dialogDetection);
            } else if (result instanceof RemotePlayerStateFirstAidMacroResultPayload psResult) {
                // Mirror the Cloud NON_NULL valueToTree of PlayerStateFirstAidMacroResult: only the chosen
                // operation's variant fields appear; ordered lists become arrays; null sample/click
                // coordinates are omitted; the result carries no macroKind in this subtree.
                ObjectNode playerStateFirstAid = objectMapper.createObjectNode();
                playerStateFirstAid.put("operation", psResult.getOperation().name());
                if (psResult.getProbeSnapshotStatus() != null) {
                    playerStateFirstAid.put("probeSnapshotStatus", psResult.getProbeSnapshotStatus().name());
                }
                if (psResult.getProbeObservations() != null) {
                    ArrayNode observations = objectMapper.createArrayNode();
                    for (RemotePlayerStateFirstAidMacroResultPayload.RemoteProbeObservation obs
                            : psResult.getProbeObservations()) {
                        ObjectNode obsNode = objectMapper.createObjectNode();
                        obsNode.put("name", obs.getName());
                        obsNode.put("status", obs.getStatus().name());
                        if (obs.getSampleRelX() != null) {
                            obsNode.put("sampleRelX", obs.getSampleRelX());
                        }
                        if (obs.getSampleRelY() != null) {
                            obsNode.put("sampleRelY", obs.getSampleRelY());
                        }
                        observations.add(obsNode);
                    }
                    playerStateFirstAid.set("probeObservations", observations);
                }
                if (psResult.getHealSnapshotStatus() != null) {
                    playerStateFirstAid.put("healSnapshotStatus", psResult.getHealSnapshotStatus().name());
                }
                if (psResult.getHealOutcomes() != null) {
                    ArrayNode healOutcomes = objectMapper.createArrayNode();
                    for (RemotePlayerStateFirstAidMacroResultPayload.RemoteHealOutcome out
                            : psResult.getHealOutcomes()) {
                        ObjectNode outNode = objectMapper.createObjectNode();
                        outNode.put("name", out.getName());
                        outNode.put("status", out.getStatus().name());
                        if (out.getSampleRelX() != null) {
                            outNode.put("sampleRelX", out.getSampleRelX());
                        }
                        if (out.getSampleRelY() != null) {
                            outNode.put("sampleRelY", out.getSampleRelY());
                        }
                        if (out.getClickAbsX() != null) {
                            outNode.put("clickAbsX", out.getClickAbsX());
                        }
                        if (out.getClickAbsY() != null) {
                            outNode.put("clickAbsY", out.getClickAbsY());
                        }
                        healOutcomes.add(outNode);
                    }
                    playerStateFirstAid.set("healOutcomes", healOutcomes);
                }
                if (psResult.getCachedPlanStatus() != null) {
                    playerStateFirstAid.put("cachedPlanStatus", psResult.getCachedPlanStatus().name());
                }
                if (psResult.getObservedBaseX() != null) {
                    playerStateFirstAid.put("observedBaseX", psResult.getObservedBaseX());
                }
                if (psResult.getObservedBaseY() != null) {
                    playerStateFirstAid.put("observedBaseY", psResult.getObservedBaseY());
                }
                typedOutcome.set("playerStateFirstAid", playerStateFirstAid);
            } else if (result instanceof RemoteDialogPreparedActionValidationMacroResultPayload dpavResult) {
                // Mirror the Cloud NON_NULL valueToTree of DialogPreparedActionValidationMacroResult: state
                // is always present; fingerprint/distance/maxDistance appear only for
                // VALIDATED/FINGERPRINT_MISMATCH; the result subtree carries no macroKind.
                ObjectNode dialogPreparedActionValidation = objectMapper.createObjectNode();
                dialogPreparedActionValidation.put("state", dpavResult.getState().name());
                if (dpavResult.getCurrentFingerprint() != null) {
                    dialogPreparedActionValidation.put("currentFingerprint", dpavResult.getCurrentFingerprint());
                }
                if (dpavResult.getDistance() != null) {
                    dialogPreparedActionValidation.put("distance", dpavResult.getDistance());
                }
                if (dpavResult.getMaxDistance() != null) {
                    dialogPreparedActionValidation.put("maxDistance", dpavResult.getMaxDistance());
                }
                typedOutcome.set("dialogPreparedActionValidation", dialogPreparedActionValidation);
            } else if (result instanceof RemoteDialogOptionOcrImageMacroResultPayload ocrImageResult) {
                // Mirror the Cloud NON_NULL valueToTree of DialogOptionOcrImageMacroResult: status is always
                // present; the raw/green/yellow PNG bytes are excluded from the digest exactly like the
                // dialog-detection frame bytes (each variant's own SHA-256 covers its integrity); every other
                // field appears only when non-null; the result subtree carries no macroKind.
                ObjectNode dialogOptionOcrImage = objectMapper.createObjectNode();
                dialogOptionOcrImage.put("status", ocrImageResult.getStatus().name());
                if (ocrImageResult.getRawSha256() != null) {
                    dialogOptionOcrImage.put("rawSha256", ocrImageResult.getRawSha256());
                }
                if (ocrImageResult.getGreenSha256() != null) {
                    dialogOptionOcrImage.put("greenSha256", ocrImageResult.getGreenSha256());
                }
                if (ocrImageResult.getYellowSha256() != null) {
                    dialogOptionOcrImage.put("yellowSha256", ocrImageResult.getYellowSha256());
                }
                if (ocrImageResult.getImageWidth() != null) {
                    dialogOptionOcrImage.put("imageWidth", ocrImageResult.getImageWidth());
                }
                if (ocrImageResult.getImageHeight() != null) {
                    dialogOptionOcrImage.put("imageHeight", ocrImageResult.getImageHeight());
                }
                if (ocrImageResult.getScanLeft() != null) {
                    dialogOptionOcrImage.put("scanLeft", ocrImageResult.getScanLeft());
                }
                if (ocrImageResult.getScanTop() != null) {
                    dialogOptionOcrImage.put("scanTop", ocrImageResult.getScanTop());
                }
                if (ocrImageResult.getScanRight() != null) {
                    dialogOptionOcrImage.put("scanRight", ocrImageResult.getScanRight());
                }
                if (ocrImageResult.getScanBottom() != null) {
                    dialogOptionOcrImage.put("scanBottom", ocrImageResult.getScanBottom());
                }
                if (ocrImageResult.getReason() != null) {
                    dialogOptionOcrImage.put("reason", ocrImageResult.getReason());
                }
                typedOutcome.set("dialogOptionOcrImage", dialogOptionOcrImage);
            } else if (result instanceof RemoteDialogOptionOcrWordsMacroResultPayload ocrWordsResult) {
                // Mirror the Cloud NON_NULL valueToTree of DialogOptionOcrWordsMacroResult: status is always
                // present; only WORDS carries the provider-order image-local box array; the result subtree
                // carries no macroKind and no byte fields.
                ObjectNode dialogOptionOcrWords = objectMapper.createObjectNode();
                dialogOptionOcrWords.put("status", ocrWordsResult.getStatus().name());
                if (ocrWordsResult.getWordBoxes() != null) {
                    ArrayNode wordBoxes = objectMapper.createArrayNode();
                    for (RemoteDialogOptionOcrWordsMacroResultPayload.RemoteWordBox box
                            : ocrWordsResult.getWordBoxes()) {
                        ObjectNode boxNode = objectMapper.createObjectNode();
                        boxNode.put("text", box.getText());
                        boxNode.put("x", box.getX());
                        boxNode.put("y", box.getY());
                        boxNode.put("left", box.getLeft());
                        boxNode.put("top", box.getTop());
                        boxNode.put("width", box.getWidth());
                        boxNode.put("height", box.getHeight());
                        boxNode.put("score", box.getScore());
                        wordBoxes.add(boxNode);
                    }
                    dialogOptionOcrWords.set("wordBoxes", wordBoxes);
                }
                typedOutcome.set("dialogOptionOcrWords", dialogOptionOcrWords);
            } else if (result instanceof RemoteDialogWhiteStoryTemplateMacroResultPayload whiteStoryResult) {
                // Mirror the Cloud NON_NULL valueToTree of DialogWhiteStoryTemplateMacroResult: state is
                // always present; the MATCHED frame PNG bytes are excluded from the digest exactly like the
                // dialog-detection frame bytes (frameSha256 covers their integrity); every other field
                // appears only when non-null (STORY_MISS/STORY_ABSENT carry only the frame rect + dimensions);
                // the result subtree carries no macroKind.
                ObjectNode dialogWhiteStoryTemplate = objectMapper.createObjectNode();
                dialogWhiteStoryTemplate.put("state", whiteStoryResult.getState().name());
                if (whiteStoryResult.getMatchedTemplateName() != null) {
                    dialogWhiteStoryTemplate.put("matchedTemplateName", whiteStoryResult.getMatchedTemplateName());
                }
                if (whiteStoryResult.getMatchedTemplatePath() != null) {
                    dialogWhiteStoryTemplate.put("matchedTemplatePath", whiteStoryResult.getMatchedTemplatePath());
                }
                if (whiteStoryResult.getRelativeX() != null) {
                    dialogWhiteStoryTemplate.put("relativeX", whiteStoryResult.getRelativeX());
                    dialogWhiteStoryTemplate.put("relativeY", whiteStoryResult.getRelativeY());
                    dialogWhiteStoryTemplate.put("absoluteX", whiteStoryResult.getAbsoluteX());
                    dialogWhiteStoryTemplate.put("absoluteY", whiteStoryResult.getAbsoluteY());
                }
                if (whiteStoryResult.getFrameLeft() != null) {
                    dialogWhiteStoryTemplate.put("frameLeft", whiteStoryResult.getFrameLeft());
                    dialogWhiteStoryTemplate.put("frameTop", whiteStoryResult.getFrameTop());
                    dialogWhiteStoryTemplate.put("frameRight", whiteStoryResult.getFrameRight());
                    dialogWhiteStoryTemplate.put("frameBottom", whiteStoryResult.getFrameBottom());
                }
                if (whiteStoryResult.getFrameSha256() != null) {
                    dialogWhiteStoryTemplate.put("frameSha256", whiteStoryResult.getFrameSha256());
                }
                if (whiteStoryResult.getFrameWidth() != null) {
                    dialogWhiteStoryTemplate.put("frameWidth", whiteStoryResult.getFrameWidth());
                    dialogWhiteStoryTemplate.put("frameHeight", whiteStoryResult.getFrameHeight());
                }
                typedOutcome.set("dialogWhiteStoryTemplate", dialogWhiteStoryTemplate);
            }
            return sha256Hex(canonicalJson(typedOutcome).getBytes(StandardCharsets.UTF_8));
        }
        mergeNonNullFields(
                typedOutcome,
                outcome.getPayload(),
                outcome.getOperation() == RemoteGameOperation.CAPTURE);
        if (outcome.getOperation() == RemoteGameOperation.TASK_TRACKER_READ) {
            JsonNode frames = typedOutcome.get("frames");
            if (frames != null && frames.isArray()) {
                ArrayNode sanitized = objectMapper.createArrayNode();
                frames.forEach(frame -> {
                    if (!frame.isObject()) {
                        throw new IllegalArgumentException("tracker read frame must be an object");
                    }
                    ObjectNode copy = ((ObjectNode) frame).deepCopy();
                    copy.remove("imageBytes");
                    sanitized.add(copy);
                });
                typedOutcome.set("frames", sanitized);
            }
        }
        return sha256Hex(canonicalJson(typedOutcome).getBytes(StandardCharsets.UTF_8));
    }

    /** Hashes the directive-independent tracker artifact dependency lease identity. */
    public String computeTaskTrackerLeaseDigest(
            String artifactId,
            String artifactDigest,
            String sourceReadActionId,
            RemoteSemanticAddress sourceReadSemanticAddress,
            String materializeActionId,
            RemoteSemanticAddress materializeSemanticAddress) {
        ObjectNode lease = objectMapper.createObjectNode();
        lease.put("artifactId", RemoteTaskTrackerReadCommandPayload.requireArtifactId(
                artifactId, "artifactId"));
        lease.put("artifactDigest", RemoteTaskTrackerReadCommandPayload.sha256(
                artifactDigest, "artifactDigest"));
        lease.put("sourceReadActionId", RemoteTaskTrackerReadCommandPayload.requiredText(
                sourceReadActionId, "sourceReadActionId"));
        lease.set("sourceReadSemanticAddress", objectMapper.valueToTree(
                RemoteTaskTrackerReadCommandPayload.requireNonNull(
                        sourceReadSemanticAddress, "sourceReadSemanticAddress")));
        lease.put("materializeActionId", RemoteTaskTrackerReadCommandPayload.requiredText(
                materializeActionId, "materializeActionId"));
        lease.set("materializeSemanticAddress", objectMapper.valueToTree(
                RemoteTaskTrackerReadCommandPayload.requireNonNull(
                        materializeSemanticAddress, "materializeSemanticAddress")));
        return sha256Hex(canonicalJson(lease).getBytes(StandardCharsets.UTF_8));
    }

    /** Hashes a final-consumed acknowledgement after excluding only ackDigest. */
    public String computeFinalConsumedAckDigest(RemoteFinalConsumedAck acknowledgement) {
        if (acknowledgement == null) {
            throw new IllegalArgumentException("acknowledgement is required");
        }
        ObjectNode root = objectMapper.valueToTree(acknowledgement);
        root.remove("ackDigest");
        return sha256Hex(canonicalJson(root).getBytes(StandardCharsets.UTF_8));
    }

    public boolean finalConsumedAckDigestMatches(RemoteFinalConsumedAck acknowledgement) {
        return acknowledgement != null
                && acknowledgement.getAckDigest() != null
                && acknowledgement.getAckDigest().equals(
                        computeFinalConsumedAckDigest(acknowledgement));
    }

    /** Hashes a final-consumed receipt after excluding only receiptDigest. */
    public String computeFinalConsumedReceiptDigest(RemoteFinalConsumedReceipt receipt) {
        if (receipt == null) {
            throw new IllegalArgumentException("receipt is required");
        }
        ObjectNode root = objectMapper.valueToTree(receipt);
        root.remove("receiptDigest");
        return sha256Hex(canonicalJson(root).getBytes(StandardCharsets.UTF_8));
    }

    public boolean finalConsumedReceiptDigestMatches(RemoteFinalConsumedReceipt receipt) {
        return receipt != null
                && receipt.getReceiptDigest() != null
                && receipt.getReceiptDigest().equals(
                        computeFinalConsumedReceiptDigest(receipt));
    }

    /** Hashes a readiness fact after excluding its own digest field. */
    public String computeResumeFactDigest(ResumeExecutorReadinessFact fact) {
        if (fact == null) {
            throw new IllegalArgumentException("fact is required");
        }
        ObjectNode root = objectMapper.valueToTree(fact);
        root.remove("factDigest");
        return sha256Hex(canonicalJson(root).getBytes(StandardCharsets.UTF_8));
    }

    /** Hashes the finalized lifecycle request after excluding only requestDigest. */
    public String computeTaskRunActionDigest(RemoteTaskRunActionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        ObjectNode root = objectMapper.valueToTree(request);
        root.remove("requestDigest");
        return sha256Hex(canonicalJson(root).getBytes(StandardCharsets.UTF_8));
    }

    public boolean requestDigestMatches(RemoteGameCommand command) {
        return command != null
                && command.getRequestDigest() != null
                && command.getRequestDigest().equals(computeRequestDigest(command));
    }

    public String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(Character.forDigit((value >>> 4) & 0x0f, 16));
                hex.append(Character.forDigit(value & 0x0f, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static void mergeNonNullFields(ObjectNode target, JsonNode source, boolean omitCaptureBytes) {
        source.fields().forEachRemaining(entry -> {
            if ((omitCaptureBytes && "imageBytes".equals(entry.getKey()))
                    || entry.getValue() == null
                    || entry.getValue().isNull()) {
                return;
            }
            target.set(entry.getKey(), entry.getValue());
        });
    }

    private ObjectNode cachePointTree(
            String templatePath, int clickX, int clickY, long learnedAtMs, String source) {
        ObjectNode cachePoint = objectMapper.createObjectNode();
        cachePoint.put("templatePath", templatePath);
        cachePoint.put("clickX", clickX);
        cachePoint.put("clickY", clickY);
        cachePoint.put("learnedAtMs", learnedAtMs);
        cachePoint.put("source", source);
        return cachePoint;
    }

    private ObjectNode firstAidToggleTree(
            RemotePlayerStateFirstAidMacroCommandPayload.RemoteFirstAidToggle toggle) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("enabled", toggle.isEnabled());
        node.put("threshold", toggle.getThreshold());
        return node;
    }

    private String canonicalJson(JsonNode node) {
        StringBuilder result = new StringBuilder();
        appendCanonical(node, result);
        return result.toString();
    }

    private void appendCanonical(JsonNode node, StringBuilder target) {
        if (node == null || node.isNull()) {
            target.append("null");
            return;
        }
        if (node.isObject()) {
            target.append('{');
            List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            iterator.forEachRemaining(fields::add);
            fields.sort(Comparator.comparing(Map.Entry::getKey));
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    target.append(',');
                }
                appendJsonString(fields.get(i).getKey(), target);
                target.append(':');
                appendCanonical(fields.get(i).getValue(), target);
            }
            target.append('}');
            return;
        }
        if (node.isArray()) {
            target.append('[');
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) {
                    target.append(',');
                }
                appendCanonical(node.get(i), target);
            }
            target.append(']');
            return;
        }
        if (node.isTextual()) {
            appendJsonString(node.textValue(), target);
            return;
        }
        if (node.isIntegralNumber()) {
            target.append(node.bigIntegerValue());
            return;
        }
        if (node.isFloatingPointNumber()) {
            appendCanonicalDouble(node.doubleValue(), target);
            return;
        }
        if (node.isBoolean()) {
            target.append(node.booleanValue());
            return;
        }
        throw new IllegalArgumentException(
                "remote protocol canonical JSON forbids binary values");
    }

    // Canonical protocol v1 representation of one finite IEEE-754 binary64 number: the RFC 8785
    // (ECMAScript) number serialization, produced by the RFC 8785 reference implementation. Only a
    // finite value is accepted; the checked IOException converges to a fail-closed argument error.
    private static void appendCanonicalDouble(double value, StringBuilder target) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "remote protocol canonical JSON forbids non-finite numbers");
        }
        try {
            target.append(NumberToJSON.serializeNumber(value));
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot canonicalize finite number", e);
        }
    }

    private void appendJsonString(String value, StringBuilder target) {
        try {
            target.append(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("cannot canonicalize JSON string", e);
        }
    }
}
