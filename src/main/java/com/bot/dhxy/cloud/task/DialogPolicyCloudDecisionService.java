package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionMode;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.config.TeleportConfig;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.dialog.WhiteTemplateSpec;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.service.dialog.DialogOptionPolicy;
import com.bot.dhxy.service.dialog.DialogStoryPolicy;
import com.bot.dhxy.task.model.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Narrow DIALOG_POLICY execute gate.
 *
 * <p>The after-local hook is retained for diagnostics and non-covered transition paths, but execute
 * mode no longer accepts {@code USE_LOCAL_RESULT} as success. Covered dialog click/interaction paths
 * must use {@link #decidePreClick(DialogPolicyPreClickCloudRequest)} before any local OCR/template
 * click code could run.</p>
 */
@Service
public class DialogPolicyCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(DialogPolicyCloudDecisionService.class);
    private static final String PHASE = "dialog-policy";
    private static final String PRE_CLICK_PHASE = "dialog-pre-click-option";
    private static final String DEFAULT_TASK_CODE = "unknown";
    private static final String LOCAL_CANDIDATE_ID = "local-result";
    private static final String LOCAL_PRE_CLICK_DECISION = "action=LOCAL_SHADOW_ONLY;reason=local-option-scan-not-executed";
    private static final String COORDINATE_SPACE_KEY = "coordinateSpace";
    private static final String WINDOW_RELATIVE = "WINDOW_RELATIVE";
    private static final String OPTION_GIVE_TEXT = "images/template/dialog/maintenance/dialog_opt_give.png";
    private static final String XIULUO_ENTER_BATTLE_TEMPLATE = "images/template/dialog/xiuluo/xiuluo_enter_battle_kanda.png";
    private static final double MIN_EXECUTE_CONFIDENCE = 0.50d;
    private static final double MIN_PRE_CLICK_CONFIDENCE = 0.70d;

    private final CloudDecisionCoordinator coordinator;

    public DialogPolicyCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isPreClickActive() {
        return coordinator.isActive(CloudDecisionServiceId.DIALOG_POLICY);
    }

    public DialogPolicyCloudDecision decideWhiteTemplate(DialogPolicyPreClickCloudRequest request) {
        DialogHandleRequest dialogRequest = request == null ? null : request.getDialogRequest();
        DialogType dialogType = "STORY".equalsIgnoreCase(request == null ? null : request.getDetectedDialogType())
                ? DialogType.STORY
                : DialogType.NONE;
        DialogResult missing = DialogResult.simple(DialogResultStatus.WHITE_TEMPLATE_NOT_FOUND, dialogType);
        if (!coordinator.isActive(CloudDecisionServiceId.DIALOG_POLICY)) {
            return DialogPolicyCloudDecision.cloudRejectedNoClick(
                    null,
                    "action=LOCAL_DISABLED;operation=VERIFY_WHITE_TEMPLATE",
                    missing,
                    requiredFailureResult(missing, "DIALOG_POLICY disabled for VERIFY_WHITE_TEMPLATE"),
                    "DIALOG_POLICY disabled for VERIFY_WHITE_TEMPLATE");
        }
        String validationError = preClickRequestValidationError(request);
        if (validationError != null) {
            DialogResult failure = requiredFailureResult(missing, validationError);
            return DialogPolicyCloudDecision.cloudRejectedNoClick(
                    null,
                    "action=LOCAL_INVALID;operation=VERIFY_WHITE_TEMPLATE",
                    missing,
                    failure,
                    validationError);
        }

        String taskCode = normalize(request.getTaskCode(), dialogTaskCode(dialogRequest.getSourceTask()));
        String phase = normalize(request.getPhase(), "dialog-white-template");
        String localDecision = "candidateId=" + LOCAL_CANDIDATE_ID
                + ";operation=VERIFY_WHITE_TEMPLATE;status=CLOUD_REQUIRED;type=" + enumName(dialogType)
                + ";clicked=false";
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.DIALOG_POLICY)
                .traceId(preClickTraceId(request, taskCode, phase))
                .taskCode(taskCode)
                .phase(phase)
                .windowId(request.getWindowId())
                .taskRunId(request.getTaskRunId())
                .policyVersion(request.getPolicyVersion())
                .localDecision(localDecision)
                .context(preClickContext(request, taskCode, phase, localDecision))
                .build();

        DialogResult[] cloudDialogResult = new DialogResult[1];
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                localDecision,
                whiteTemplateExecutionGate(request, dialogType, cloudDialogResult));
        if (cloudResult.isExecuted() && cloudDialogResult[0] != null) {
            log.info("cloud.execute serviceId=DIALOG_POLICY accepted=true source={} operation=VERIFY_WHITE_TEMPLATE status={} actionKey={} elapsedMs={}",
                    dialogRequest.getSourceTask(),
                    cloudDialogResult[0].getStatus(),
                    cloudDialogResult[0].getActionKey(),
                    cloudResult.getElapsedMs());
            return DialogPolicyCloudDecision.cloudExecuted(
                    cloudResult, localDecision, missing, cloudDialogResult[0]);
        }
        String rejectReason = cloudResult == null ? "cloud result missing" : cloudResult.getReason();
        log.warn("cloud.execute serviceId=DIALOG_POLICY accepted=false rejectReason={} source={} operation=VERIFY_WHITE_TEMPLATE cloudDecision={}",
                rejectReason,
                dialogRequest.getSourceTask(),
                cloudResult == null || cloudResult.getResponse() == null
                        ? null
                        : cloudResult.getResponse().getDecision());
        return DialogPolicyCloudDecision.cloudRejectedNoClick(
                cloudResult,
                localDecision,
                missing,
                requiredFailureResult(missing, rejectReason),
                rejectReason);
    }

    /**
     * Decide a covered dialog pre-click action through DIALOG_POLICY.
     *
     * @param request raw dialog/window ROI payload plus task/window context. Cloud `CLICK` actions
     *                must return a plain left `WINDOW_RELATIVE` point inside both the game window and
     *                the request ROI. The local dialog scan/click is comparator evidence only for
     *                CR167 covered paths and never becomes the production fallback while execute/STOP
     *                is active.
     * @return executable cloud click, cloud-owned no-action, disabled passthrough, or fail-closed
     *         required failure.
     */
    public DialogPolicyPreClickCloudDecision decidePreClick(DialogPolicyPreClickCloudRequest request) {
        if (!coordinator.isActive(CloudDecisionServiceId.DIALOG_POLICY)) {
            return DialogPolicyPreClickCloudDecision.builder()
                    .status(DialogPolicyPreClickCloudDecision.Status.DISABLED)
                    .action(DialogPolicyPreClickCloudDecision.Action.ABORT)
                    .reason("service disabled")
                    .build();
        }
        String validationError = preClickRequestValidationError(request);
        if (validationError != null) {
            return preClickRequiredFailure(null, validationError);
        }

        DialogHandleRequest dialogRequest = request.getDialogRequest();
        String taskCode = normalize(request.getTaskCode(), dialogTaskCode(dialogRequest.getSourceTask()));
        String phase = normalize(request.getPhase(), PRE_CLICK_PHASE);
        String localDecision = LOCAL_PRE_CLICK_DECISION;
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.DIALOG_POLICY)
                .traceId(preClickTraceId(request, taskCode, phase))
                .taskCode(taskCode)
                .phase(phase)
                .windowId(request.getWindowId())
                .taskRunId(request.getTaskRunId())
                .policyVersion(request.getPolicyVersion())
                .localDecision(localDecision)
                .context(preClickContext(request, taskCode, phase, localDecision))
                .build();

        PreClickParseResult[] parsedHolder = new PreClickParseResult[1];
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                localDecision,
                preClickExecutionGate(request, parsedHolder));
        if (cloudResult.isExecuted()) {
            PreClickParseResult parsed = parsedHolder[0];
            Map<String, String> fields = fields(cloudResult.getResponse().getDecision());
            if (parsed.action() == DialogPolicyPreClickCloudDecision.Action.CLICK) {
                return DialogPolicyPreClickCloudDecision.builder()
                        .status(DialogPolicyPreClickCloudDecision.Status.CLOUD_EXECUTED)
                        .action(parsed.action())
                        .windowRelativeClickPoint(parsed.click())
                        .actionId(normalize(fields.get("actionId"), null))
                        .decisionId(normalize(fields.get("decisionId"), null))
                        .reason(firstText(fields.get("reason"), cloudResult.getReason()))
                        .matchedText(normalize(fields.get("matchedText"), null))
                        .debugToken(diagnostic(cloudResult.getResponse(), "debugToken"))
                        .candidateBox(parsed.candidateBox().toDecisionText())
                        .ctrl(Boolean.parseBoolean(fields.getOrDefault("ctrl", "false")))
                        .alt(Boolean.parseBoolean(fields.getOrDefault("alt", "false")))
                        .confidence(cloudResult.getResponse().getConfidence())
                        .cloudResult(cloudResult)
                        .build();
            }
            return DialogPolicyPreClickCloudDecision.builder()
                    .status(DialogPolicyPreClickCloudDecision.Status.CLOUD_NO_ACTION)
                    .action(parsed.action())
                    .actionId(normalize(fields.get("actionId"), null))
                    .decisionId(normalize(fields.get("decisionId"), null))
                    .reason(firstText(fields.get("reason"), cloudResult.getReason()))
                    .matchedText(normalize(fields.get("matchedText"), null))
                    .debugToken(diagnostic(cloudResult.getResponse(), "debugToken"))
                    .candidateBox(parsed.candidateBox() == null ? null : parsed.candidateBox().toDecisionText())
                    .confidence(cloudResult.getResponse().getConfidence())
                    .cloudResult(cloudResult)
                    .build();
        }

        if (cloudResult.getMode() == CloudDecisionMode.DISABLED || cloudResult.getMode() == CloudDecisionMode.SHADOW) {
            return DialogPolicyPreClickCloudDecision.builder()
                    .status(DialogPolicyPreClickCloudDecision.Status.DISABLED)
                    .action(DialogPolicyPreClickCloudDecision.Action.ABORT)
                    .reason(cloudResult.getReason())
                    .cloudResult(cloudResult)
                    .build();
        }

        log.warn("cloud.execute serviceId=DIALOG_POLICY hook=pre-click accepted=false taskCode={} phase={} reason={} cloudDecision={}",
                cloudRequest.getTaskCode(),
                cloudRequest.getPhase(),
                cloudResult.getReason(),
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision());
        return preClickRequiredFailure(cloudResult, cloudResult.getReason());
    }

    /**
     * Decide whether a DIALOG_POLICY cloud response may replace the current local dialog result.
     *
     * @param request local dialog request that already produced {@code localResult}; nullable values
     *                are converted to diagnostic blanks.
     * @param localResult locally detected/clicked result. This remains the fallback and is the only
     *                    candidate in the first execute-gate version.
     * @return envelope whose effective result is always a local safe result or a required-failure
     *         no-click failure; never a cloud coordinate or direct input instruction.
     */
    public DialogPolicyCloudDecision decide(DialogHandleRequest request, DialogResult localResult) {
        String localDecision = localDecision(request, localResult);
        if (request == null || localResult == null || !coordinator.isActive(CloudDecisionServiceId.DIALOG_POLICY)) {
            return DialogPolicyCloudDecision.localOnly(localDecision, localResult);
        }

        String taskCode = dialogTaskCode(request.getSourceTask());
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.DIALOG_POLICY)
                .traceId(traceId(taskCode, request))
                .taskCode(taskCode)
                .phase(PHASE)
                .localDecision(localDecision)
                .context(context(request, localResult))
                .build();

        DialogCandidate[] selectedCandidate = new DialogCandidate[1];
        boolean[] gateEvaluated = {false};
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                localDecision,
                dialogPolicyExecutionGate(request, localResult, selectedCandidate, gateEvaluated));
        if (cloudResult.isExecuted() && selectedCandidate[0] != null) {
            log.info("dialog cloud policy accepted: source={} operation={} action={} candidateId={} effectiveDecision={} elapsedMs={}",
                    request.getSourceTask(),
                    request.getOperation(),
                    action(cloudResult),
                    selectedCandidate[0].candidateId(),
                    cloudResult.getEffectiveDecision(),
                    cloudResult.getElapsedMs());
            log.info("cloud.execute serviceId=DIALOG_POLICY accepted=true source={} operation={} candidateId={} elapsedMs={}",
                    request.getSourceTask(),
                    request.getOperation(),
                    selectedCandidate[0].candidateId(),
                    cloudResult.getElapsedMs());
            return DialogPolicyCloudDecision.cloudExecuted(
                    cloudResult, localDecision, localResult, selectedCandidate[0].result());
        }
        if (keepsLocalPassthrough(cloudResult, gateEvaluated[0])) {
            return DialogPolicyCloudDecision.localPassthrough(cloudResult, localDecision, localResult);
        }
        if (keepsAfterLocalSafeNoActionPassthrough(localResult, cloudResult, gateEvaluated[0])) {
            log.info("cloud.execute serviceId=DIALOG_POLICY accepted=false hook=after-local safeNoActionPassthrough=true source={} operation={} localStatus={} cloudDecision={} rejectReason={} note=local safe no-action passthrough, not cloud execution success",
                    request.getSourceTask(),
                    request.getOperation(),
                    localResult.getStatus(),
                    cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision(),
                    cloudResult.getReason());
            return DialogPolicyCloudDecision.localPassthrough(cloudResult, localDecision, localResult);
        }

        String rejectReason = cloudResult.getReason();
        log.warn("cloud.execute serviceId=DIALOG_POLICY accepted=false rejectReason={} source={} operation={} localDecision={} cloudDecision={}",
                rejectReason,
                request.getSourceTask(),
                request.getOperation(),
                localDecision,
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision());
        if (cloudResult.isRequiredExecuteFailure()) {
            return DialogPolicyCloudDecision.cloudRejectedNoClick(
                    cloudResult,
                    localDecision,
                    localResult,
                    requiredFailureResult(localResult, rejectReason),
                    rejectReason);
        }
        return DialogPolicyCloudDecision.cloudRejectedLocal(cloudResult, localDecision, localResult, rejectReason);
    }

    private CloudDecisionExecutionGate dialogPolicyExecutionGate(
            DialogHandleRequest request,
            DialogResult localResult,
            DialogCandidate[] selectedCandidate,
            boolean[] gateEvaluated) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.DIALOG_POLICY;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest cloudRequest,
                    CloudDecisionResponse response,
                    String localDecision) {
                gateEvaluated[0] = true;
                if (response.getConfidence() < MIN_EXECUTE_CONFIDENCE) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "low confidence: " + response.getConfidence());
                }

                Map<String, String> fields = fields(response.getDecision());
                String coordinateField = rawCoordinateField(fields);
                if (coordinateField != null) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "raw click coordinate fields are not allowed: " + coordinateField);
                }

                String mismatch = declaredFieldMismatch(fields, request, localResult);
                if (mismatch != null) {
                    return CloudDecisionExecutionGate.GateResult.rejected(mismatch);
                }

                String action = normalizedUpper(fields.get("action"));
                if (!hasText(action)) {
                    return CloudDecisionExecutionGate.GateResult.rejected("action is required");
                }
                DialogCandidate localCandidate = new DialogCandidate(
                        LOCAL_CANDIDATE_ID,
                        localDecision,
                        localResult);
                switch (action) {
                    case "SELECT_CANDIDATE" -> {
                        String candidateId = fields.get("candidateId");
                        if (!LOCAL_CANDIDATE_ID.equals(candidateId)) {
                            return CloudDecisionExecutionGate.GateResult.rejected(
                                    "candidateId not found: " + safe(candidateId));
                        }
                        selectedCandidate[0] = localCandidate;
                        return CloudDecisionExecutionGate.GateResult.accepted(
                                localCandidate.decision(),
                                acceptedReason(fields, "selected local dialog candidate"));
                    }
                    case "REJECT" -> {
                        return CloudDecisionExecutionGate.GateResult.rejected(
                                "cloud rejected: " + safe(fields.get("reason")));
                    }
                    default -> {
                        return CloudDecisionExecutionGate.GateResult.rejected(
                                "unsupported action: " + action);
                    }
                }
            }
        };
    }

    private CloudDecisionExecutionGate preClickExecutionGate(
            DialogPolicyPreClickCloudRequest preClickRequest,
            PreClickParseResult[] parsedHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.DIALOG_POLICY;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                if (response.getConfidence() < MIN_PRE_CLICK_CONFIDENCE) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "low confidence: " + response.getConfidence());
                }
                PreClickParseResult parsed = parsePreClick(preClickRequest, response);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                parsedHolder[0] = parsed;
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(),
                        parsed.action() == DialogPolicyPreClickCloudDecision.Action.CLICK
                                ? "execute percent gate hit; using DIALOG_POLICY pre-click WINDOW_RELATIVE click"
                                : "execute percent gate hit; DIALOG_POLICY pre-click returned no-click action");
            }
        };
    }

    private CloudDecisionExecutionGate whiteTemplateExecutionGate(
            DialogPolicyPreClickCloudRequest request,
            DialogType dialogType,
            DialogResult[] resultHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.DIALOG_POLICY;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest cloudRequest,
                    CloudDecisionResponse response,
                    String localDecision) {
                if (response.getConfidence() < MIN_EXECUTE_CONFIDENCE) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "low confidence: " + response.getConfidence());
                }
                Map<String, String> fields = fields(response.getDecision());
                if (!"NO_ACTION".equals(normalizedUpper(fields.get("action")))) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "VERIFY_WHITE_TEMPLATE expects action=NO_ACTION");
                }
                String coordinateField = rawCoordinateField(fields);
                if (coordinateField != null) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "raw click coordinate fields are not allowed: " + coordinateField);
                }
                String status = normalizedUpper(fields.get("status"));
                if ("FOUND".equals(status)) {
                    String actionId = fields.get("actionId");
                    String actionError = preClickNoClickActionIdValidationError(
                            request,
                            DialogPolicyPreClickCloudDecision.Action.NO_ACTION,
                            actionId);
                    if (actionError != null) {
                        return CloudDecisionExecutionGate.GateResult.rejected(actionError);
                    }
                    resultHolder[0] = DialogResult.statusBuilder(DialogResultStatus.WHITE_TEMPLATE_VISIBLE, dialogType)
                            .actionKey(actionId)
                            .matchedText(firstText(fields.get("matchedText"), fields.get("reason")))
                            .build();
                    return CloudDecisionExecutionGate.GateResult.accepted(
                            response.getDecision(),
                            "execute percent gate hit; cloud VERIFY_WHITE_TEMPLATE visible");
                }
                if ("NOT_FOUND".equals(status)) {
                    resultHolder[0] = DialogResult.statusBuilder(DialogResultStatus.WHITE_TEMPLATE_NOT_FOUND, dialogType)
                            .matchedText(firstText(fields.get("matchedText"), fields.get("reason")))
                            .build();
                    return CloudDecisionExecutionGate.GateResult.accepted(
                            response.getDecision(),
                            "execute percent gate hit; cloud VERIFY_WHITE_TEMPLATE not found");
                }
                return CloudDecisionExecutionGate.GateResult.rejected(
                        "unsupported VERIFY_WHITE_TEMPLATE status: " + status);
            }
        };
    }

    private static String localDecision(DialogHandleRequest request, DialogResult result) {
        return "candidateId=" + LOCAL_CANDIDATE_ID
                + ";operation=" + enumName(request == null ? null : request.getOperation())
                + ";optionPolicy=" + enumName(request == null ? null : request.getOptionPolicy())
                + ";fallbackPolicy=" + enumName(request == null ? null : request.getFallbackPolicy())
                + ";status=" + enumName(result == null ? null : result.getStatus())
                + ";type=" + enumName(result == null ? null : result.getDialogType())
                + ";actionKey=" + safe(result == null ? null : result.getActionKey())
                + ";clicked=" + (result != null && result.isClicked());
    }

    private static Map<String, String> context(DialogHandleRequest request, DialogResult result) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("hook", "dialog-policy-execute");
        context.put("sourceTask", safe(request.getSourceTask()));
        context.put("operation", enumName(request.getOperation()));
        context.put("storyPolicy", enumName(request.getStoryPolicy()));
        context.put("optionPolicy", enumName(request.getOptionPolicy()));
        context.put("fallbackPolicy", enumName(request.getFallbackPolicy()));
        context.put("allowFallbackOptionClick", Boolean.toString(request.isAllowFallbackOptionClick()));
        context.put("verifyDialogType", Boolean.toString(request.isVerifyDialogType()));
        context.put("targetKeyword", safe(request.getTargetKeyword()));
        context.put("itemToGive", safe(request.getItemToGive()));
        context.put("status", enumName(result.getStatus()));
        context.put("kind", enumName(result.getKind()));
        context.put("dialogType", enumName(result.getDialogType()));
        context.put("actionKey", safe(result.getActionKey()));
        context.put("matchedText", safe(result.getMatchedText()));
        context.put("clicked", Boolean.toString(result.isClicked()));
        context.put("candidateIds", LOCAL_CANDIDATE_ID);
        context.put("candidateCount", "1");
        context.put("candidate.local-result", localDecision(request, result));
        context.put("activeTaskCode", dialogTaskCode(request.getSourceTask()));
        context.put("activeTaskType", taskTypeName(request.getSourceTask()));
        return Map.copyOf(context);
    }

    private static Map<String, String> preClickContext(
            DialogPolicyPreClickCloudRequest request,
            String taskCode,
            String phase,
            String localDecision) {
        DialogHandleRequest dialogRequest = request.getDialogRequest();
        Map<String, String> context = new LinkedHashMap<>();
        context.put("hook", phase);
        context.put("sourceTask", safe(dialogRequest.getSourceTask()));
        context.put("source", safe(request.getSource()));
        context.put("phase", phase);
        context.put("taskCode", taskCode);
        context.put("windowId", safe(request.getWindowId()));
        context.put("taskRunId", safe(request.getTaskRunId()));
        context.put("policyVersion", safe(request.getPolicyVersion()));
        context.put("hwnd", safe(request.getHwnd()));
        context.put("operation", enumName(dialogRequest.getOperation()));
        context.put("storyPolicy", enumName(dialogRequest.getStoryPolicy()));
        context.put("optionPolicy", enumName(dialogRequest.getOptionPolicy()));
        context.put("detectedDialogType", safe(request.getDetectedDialogType()));
        context.put("fallbackPolicy", enumName(dialogRequest.getFallbackPolicy()));
        context.put("allowFallbackOptionClick", Boolean.toString(dialogRequest.isAllowFallbackOptionClick()));
        context.put("verifyDialogType", Boolean.toString(dialogRequest.isVerifyDialogType()));
        context.put("targetKeyword", safe(dialogRequest.getTargetKeyword()));
        context.put("targetKeywordAliases", targetKeywordAliases(dialogRequest.getTargetKeyword()));
        context.put("itemToGive", safe(dialogRequest.getItemToGive()));
        context.put("priorClickOutcome", safe(request.getPriorClickOutcome()));
        context.put("priorClickAttemptId", safe(request.getPriorClickAttemptId()));
        context.put("greenTemplateSpecs", greenTemplateSpecs(dialogRequest));
        context.put("whiteTemplateSpecs", whiteTemplateSpecs(dialogRequest));
        context.put("targetKeywordTemplateSpecs", targetKeywordTemplateSpecs(dialogRequest));
        context.put("rememberedRelativeX", dialogRequest.getRememberedRelativeX() == null
                ? ""
                : String.valueOf(dialogRequest.getRememberedRelativeX()));
        context.put("rememberedRelativeY", dialogRequest.getRememberedRelativeY() == null
                ? ""
                : String.valueOf(dialogRequest.getRememberedRelativeY()));
        context.put("rememberedOptionText", safe(dialogRequest.getRememberedOptionText()));
        context.put("storyMissTargetKeyword", safe(dialogRequest.getStoryMissTargetKeyword()));
        context.put("storyAbsentTargetKeyword", safe(dialogRequest.getStoryAbsentTargetKeyword()));
        context.put("storyAbsentMatchedText", safe(dialogRequest.getStoryAbsentMatchedText()));
        putGreenTemplateSpecContext(context, dialogRequest);
        putWhiteTemplateSpecContext(context, dialogRequest);
        context.put("localShadowDecision", safe(localDecision));
        context.put("imagePayloadBase64", request.getImagePayloadBase64().trim());
        context.put("payloadMimeType", request.getPayloadMimeType().trim());
        context.put("imageSha256", request.getImageSha256().trim());
        context.put("rawImagePath", safe(request.getRawImagePath()));
        context.put("debugImageId", safe(request.getDebugImageId()));
        context.put("windowWidth", String.valueOf(request.getWindowWidth()));
        context.put("windowHeight", String.valueOf(request.getWindowHeight()));
        context.put("roi", roiText(request.getRoi()));
        context.put("payloadCoordinateSpace", "ROI_OR_WINDOW_RELATIVE_RAW");
        context.put("activeTaskType", taskTypeName(dialogRequest.getSourceTask()));
        return Map.copyOf(context);
    }

    private static String preClickRequestValidationError(DialogPolicyPreClickCloudRequest request) {
        if (request == null) {
            return "missing dialog pre-click cloud request";
        }
        if (request.getDialogRequest() == null) {
            return "dialog request is required";
        }
        if (!hasText(request.getImagePayloadBase64())) {
            return "missing transferable image payload: imagePayloadBase64 is required";
        }
        if (!hasText(request.getPayloadMimeType())) {
            return "missing transferable image payload metadata: payloadMimeType is required";
        }
        if (!hasText(request.getImageSha256())) {
            return "missing transferable image payload metadata: imageSha256 is required";
        }
        if (request.getWindowWidth() <= 0 || request.getWindowHeight() <= 0) {
            return "window size must be explicit positive pixels";
        }
        DialogPolicyPreClickCloudRequest.Roi roi = request.getRoi();
        if (roi == null) {
            return "ROI is required for dialog pre-click";
        }
        if (roi.getX() < 0 || roi.getY() < 0 || roi.getWidth() <= 0 || roi.getHeight() <= 0) {
            return "ROI must be window-relative pixels with non-negative origin and positive size";
        }
        long maxX = (long) roi.getX() + roi.getWidth();
        long maxY = (long) roi.getY() + roi.getHeight();
        if (maxX > request.getWindowWidth() || maxY > request.getWindowHeight()) {
            return "ROI outside window: roi=" + roiText(roi)
                    + " window=0,0," + request.getWindowWidth() + "," + request.getWindowHeight();
        }
        return null;
    }

    private static PreClickParseResult parsePreClick(
            DialogPolicyPreClickCloudRequest request,
            CloudDecisionResponse response) {
        Map<String, String> fields = fields(response.getDecision());
        String actionText = normalizedUpper(fields.get("action"));
        if (!hasText(actionText)) {
            return PreClickParseResult.rejected("action is required");
        }
        DialogPolicyPreClickCloudDecision.Action action;
        try {
            action = DialogPolicyPreClickCloudDecision.Action.valueOf(actionText);
        } catch (IllegalArgumentException e) {
            return PreClickParseResult.rejected("unsupported action: " + actionText);
        }
        if (action != DialogPolicyPreClickCloudDecision.Action.CLICK) {
            String actionId = normalize(fields.get("actionId"), null);
            String actionIdError = preClickNoClickActionIdValidationError(request, action, actionId);
            if (actionIdError != null) {
                return PreClickParseResult.rejected(actionIdError);
            }
            Box candidateBox = parseBox(firstText(diagnostic(response, "candidateBox"), fields.get("candidateBox")));
            if (candidateBox != null) {
                if (!insideWindow(candidateBox, request.getWindowWidth(), request.getWindowHeight())) {
                    return PreClickParseResult.rejected("candidateBox outside window: candidateBox="
                            + candidateBox.toDecisionText());
                }
                if (!insideRoi(candidateBox, request.getRoi())) {
                    return PreClickParseResult.rejected("candidateBox outside ROI: candidateBox="
                            + candidateBox.toDecisionText() + " roi=" + roiText(request.getRoi()));
                }
            }
            return PreClickParseResult.acceptedNoClick(action, candidateBox);
        }

        String actionId = normalize(fields.get("actionId"), null);
        if (!hasText(actionId)) {
            return PreClickParseResult.rejected("actionId is required when action=CLICK");
        }
        String actionIdError = preClickActionIdValidationError(request, actionId);
        if (actionIdError != null) {
            return PreClickParseResult.rejected(actionIdError);
        }
        if (Boolean.parseBoolean(fields.getOrDefault("ctrl", "false"))
                || Boolean.parseBoolean(fields.getOrDefault("alt", "false"))) {
            return PreClickParseResult.rejected("only plain left CLICK is supported for dialog pre-click");
        }
        String coordinateSpace = diagnostic(response, COORDINATE_SPACE_KEY);
        if (!WINDOW_RELATIVE.equals(coordinateSpace)) {
            return PreClickParseResult.rejected(
                    "diagnostics.coordinateSpace must be WINDOW_RELATIVE when action=CLICK");
        }
        Point click = parseClick(fields.get("click"));
        if (click == null) {
            return PreClickParseResult.rejected("click must parse as click=<windowX>,<windowY>");
        }
        if (!insideWindow(click, request.getWindowWidth(), request.getWindowHeight())) {
            return PreClickParseResult.rejected("WINDOW_RELATIVE click outside window: click="
                    + click.x + "," + click.y + " window=" + request.getWindowWidth() + "x" + request.getWindowHeight());
        }
        if (!insideRoi(click, request.getRoi())) {
            return PreClickParseResult.rejected("WINDOW_RELATIVE click outside ROI: click="
                    + click.x + "," + click.y + " roi=" + roiText(request.getRoi()));
        }
        Box candidateBox = parseBox(firstText(diagnostic(response, "candidateBox"), fields.get("candidateBox")));
        if (candidateBox == null) {
            return PreClickParseResult.rejected("candidateBox is required when action=CLICK");
        }
        if (!insideWindow(candidateBox, request.getWindowWidth(), request.getWindowHeight())) {
            return PreClickParseResult.rejected("candidateBox outside window: candidateBox="
                    + candidateBox.toDecisionText());
        }
        if (!insideRoi(candidateBox, request.getRoi())) {
            return PreClickParseResult.rejected("candidateBox outside ROI: candidateBox="
                    + candidateBox.toDecisionText() + " roi=" + roiText(request.getRoi()));
        }
        return PreClickParseResult.acceptedClick(click, candidateBox);
    }

    private static String preClickNoClickActionIdValidationError(
            DialogPolicyPreClickCloudRequest cloudRequest,
            DialogPolicyPreClickCloudDecision.Action action,
            String actionId) {
        if (!hasText(actionId)) {
            return null;
        }
        if (action != DialogPolicyPreClickCloudDecision.Action.NO_ACTION) {
            return "no-click actionId is only supported with action=NO_ACTION: action=" + action;
        }
        if (cloudRequest == null || cloudRequest.getDialogRequest() == null) {
            return "dialog request is required";
        }
        DialogHandleRequest request = cloudRequest.getDialogRequest();
        if (whiteStorySemanticActionIds(request).contains(actionId)) {
            return null;
        }
        if (request.getOptionPolicy() == DialogOptionPolicy.VERIFY_GREEN_TEMPLATE) {
            if ("OPTION_DIALOG_VISIBLE".equals(actionId)) {
                return null;
            }
            if (hasGreenTemplateSpecName(request, actionId)) {
                return null;
            }
            return "no-click actionId must match GreenTemplateClickSpec.name for VERIFY_GREEN_TEMPLATE: expected one of="
                    + greenTemplateSpecNames(request) + " actual=" + safe(actionId);
        }
        return "no-click actionId must match white story semantic allowlist: expected one of="
                + String.join("|", whiteStorySemanticActionIds(request))
                + " actual=" + safe(actionId);
    }

    private static String preClickActionIdValidationError(DialogPolicyPreClickCloudRequest cloudRequest, String actionId) {
        if (cloudRequest == null || cloudRequest.getDialogRequest() == null) {
            return "dialog request is required";
        }
        DialogHandleRequest request = cloudRequest.getDialogRequest();
        if ("STORY".equalsIgnoreCase(cloudRequest.getDetectedDialogType())
                && request.getStoryPolicy() == DialogStoryPolicy.CLICK_THROUGH) {
            if ("STORY_CLICK_THROUGH".equals(actionId)) {
                return null;
            }
            return "actionId must be STORY_CLICK_THROUGH for observed STORY click-through: actual="
                    + safe(actionId);
        }
        DialogOptionPolicy policy = request.getOptionPolicy();
        if (policy == DialogOptionPolicy.CLICK_KEYWORD) {
            String targetKeyword = normalize(request.getTargetKeyword(), null);
            if (!hasText(targetKeyword)) {
                return "targetKeyword is required for CLICK_KEYWORD pre-click actionId validation";
            }
            if (!targetKeyword.equals(actionId)) {
                return "actionId must match targetKeyword: expected="
                        + targetKeyword + " actual=" + safe(actionId);
            }
            return null;
        }
        if (policy == DialogOptionPolicy.CLICK_GREEN_TEMPLATE) {
            if (request.getGreenTemplateSpecs() == null || request.getGreenTemplateSpecs().isEmpty()) {
                return "GreenTemplateClickSpec.name is required for CLICK_GREEN_TEMPLATE pre-click actionId validation";
            }
            for (GreenTemplateClickSpec spec : request.getGreenTemplateSpecs()) {
                if (spec != null && actionId.equals(spec.name())) {
                    return null;
                }
            }
            return "actionId must match GreenTemplateClickSpec.name: expected one of="
                    + greenTemplateSpecNames(request) + " actual=" + safe(actionId);
        }
        if (policy == DialogOptionPolicy.CLICK_REMEMBERED_POINT) {
            String targetKeyword = normalize(request.getTargetKeyword(), null);
            if (!hasText(targetKeyword)) {
                return "targetKeyword is required for CLICK_REMEMBERED_POINT pre-click actionId validation";
            }
            if (targetKeyword.equals(actionId) || hasGreenTemplateSpecName(request, actionId)) {
                return null;
            }
            return "actionId must match targetKeyword or GreenTemplateClickSpec.name: expected="
                    + targetKeyword + "|" + greenTemplateSpecNames(request) + " actual=" + safe(actionId);
        }
        if (policy == DialogOptionPolicy.CLICK_BUSINESS_OPTION) {
            return "CLICK_BUSINESS_OPTION no longer supports maintenance broadcast actions: actual="
                    + safe(actionId);
        }
        if (policy == DialogOptionPolicy.GIVE_ITEM_IF_AVAILABLE) {
            String itemToGive = normalize(request.getItemToGive(), null);
            if (!hasText(itemToGive)) {
                return "itemToGive is required for GIVE_ITEM_IF_AVAILABLE pre-click actionId validation";
            }
            if (!itemToGive.equals(actionId)) {
                return "actionId must match itemToGive: expected="
                        + itemToGive + " actual=" + safe(actionId);
            }
            return null;
        }
        if (policy == DialogOptionPolicy.FALLBACK_FIRST_OPTION
                || policy == DialogOptionPolicy.FALLBACK_LAST_OPTION) {
            String expected = enumName(policy);
            if (!expected.equals(actionId)) {
                return "actionId must match fallback optionPolicy: expected="
                        + expected + " actual=" + safe(actionId);
            }
            return null;
        }
        return "unsupported pre-click optionPolicy for actionId validation: " + enumName(policy);
    }

    private static boolean hasGreenTemplateSpecName(DialogHandleRequest request, String actionId) {
        if (!hasText(actionId) || request.getGreenTemplateSpecs() == null) {
            return false;
        }
        for (GreenTemplateClickSpec spec : request.getGreenTemplateSpecs()) {
            if (spec != null && actionId.equals(spec.name())) {
                return true;
            }
        }
        return false;
    }

    private static java.util.Set<String> whiteStorySemanticActionIds(DialogHandleRequest request) {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        if (request == null) {
            return ids;
        }
        if (request.getWhiteTemplateSpecs() != null) {
            for (WhiteTemplateSpec spec : request.getWhiteTemplateSpecs()) {
                if (spec != null && hasText(spec.name())) {
                    ids.add(spec.name());
                }
            }
        }
        if (hasText(request.getStoryMissTargetKeyword())) {
            ids.add(request.getStoryMissTargetKeyword().trim());
        }
        if (hasText(request.getStoryAbsentTargetKeyword())) {
            ids.add(request.getStoryAbsentTargetKeyword().trim());
        }
        return ids;
    }

    private static String greenTemplateSpecNames(DialogHandleRequest request) {
        if (request == null || request.getGreenTemplateSpecs() == null || request.getGreenTemplateSpecs().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (GreenTemplateClickSpec spec : request.getGreenTemplateSpecs()) {
            if (spec == null) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('|');
            }
            builder.append(safe(spec.name()));
        }
        return builder.toString();
    }

    private static String whiteTemplateSpecs(DialogHandleRequest request) {
        if (request.getWhiteTemplateSpecs() == null || request.getWhiteTemplateSpecs().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("[");
        int count = 0;
        for (WhiteTemplateSpec spec : request.getWhiteTemplateSpecs()) {
            if (spec == null) {
                continue;
            }
            if (count++ > 0) {
                builder.append(',');
            }
            appendWhiteTemplateSpecJson(builder, spec.name(), spec.templatePath());
        }
        return builder.append(']').toString();
    }

    private static void appendWhiteTemplateSpecJson(StringBuilder builder, String name, String templatePath) {
        builder.append('{')
                .append("\"name\":\"").append(jsonEscape(safe(name))).append("\",")
                .append("\"templatePath\":\"").append(jsonEscape(safe(templatePath))).append("\"")
                .append('}');
    }

    private static DialogPolicyPreClickCloudDecision preClickRequiredFailure(
            CloudDecisionResult cloudResult,
            String reason) {
        return DialogPolicyPreClickCloudDecision.builder()
                .status(DialogPolicyPreClickCloudDecision.Status.REQUIRED_FAILURE)
                .action(DialogPolicyPreClickCloudDecision.Action.ABORT)
                .reason(safe(reason))
                .cloudResult(cloudResult)
                .build();
    }

    private static Point parseClick(String value) {
        if (!hasText(value)) {
            return null;
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new Point(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Box parseBox(String value) {
        if (!hasText(value)) {
            return null;
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 4) {
            return null;
        }
        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int width = Integer.parseInt(parts[2].trim());
            int height = Integer.parseInt(parts[3].trim());
            return width > 0 && height > 0 ? new Box(x, y, width, height) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean insideWindow(Point point, int width, int height) {
        return point != null && point.x >= 0 && point.y >= 0 && point.x < width && point.y < height;
    }

    private static boolean insideWindow(Box box, int width, int height) {
        return box != null
                && box.x() >= 0
                && box.y() >= 0
                && box.width() > 0
                && box.height() > 0
                && (long) box.x() + box.width() <= width
                && (long) box.y() + box.height() <= height;
    }

    private static boolean insideRoi(Point point, DialogPolicyPreClickCloudRequest.Roi roi) {
        return point != null
                && roi != null
                && point.x >= roi.getX()
                && point.x < roi.getX() + roi.getWidth()
                && point.y >= roi.getY()
                && point.y < roi.getY() + roi.getHeight();
    }

    private static boolean insideRoi(Box box, DialogPolicyPreClickCloudRequest.Roi roi) {
        return box != null
                && roi != null
                && box.x() >= roi.getX()
                && box.y() >= roi.getY()
                && (long) box.x() + box.width() <= (long) roi.getX() + roi.getWidth()
                && (long) box.y() + box.height() <= (long) roi.getY() + roi.getHeight();
    }

    private static String roiText(DialogPolicyPreClickCloudRequest.Roi roi) {
        if (roi == null) {
            return "";
        }
        return roi.getX() + "," + roi.getY() + "," + roi.getWidth() + "," + roi.getHeight();
    }

    private static String greenTemplateSpecs(DialogHandleRequest request) {
        if (request.getGreenTemplateSpecs() == null || request.getGreenTemplateSpecs().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("[");
        int count = 0;
        for (GreenTemplateClickSpec spec : request.getGreenTemplateSpecs()) {
            if (spec == null) {
                continue;
            }
            if (count++ > 0) {
                builder.append(',');
            }
            appendTemplateSpecJson(builder, spec.name(), spec.templatePath(),
                    spec.minOffsetX(), spec.maxOffsetX(), spec.randomRadiusY());
        }
        return builder.append(']').toString();
    }

    private static String targetKeywordTemplateSpecs(DialogHandleRequest request) {
        StringBuilder builder = new StringBuilder("[");
        int count = 0;
        if (request.getOptionPolicy() == DialogOptionPolicy.CLICK_GREEN_TEMPLATE
                && request.getGreenTemplateSpecs() != null) {
            for (GreenTemplateClickSpec spec : request.getGreenTemplateSpecs()) {
                if (spec == null) {
                    continue;
                }
                if (count++ > 0) {
                    builder.append(',');
                }
                appendTemplateSpecJson(builder, spec.name(), spec.templatePath(),
                        spec.minOffsetX(), spec.maxOffsetX(), spec.randomRadiusY());
            }
        }
        if (request.getOptionPolicy() == DialogOptionPolicy.GIVE_ITEM_IF_AVAILABLE
                && hasText(request.getItemToGive())) {
            if (count++ > 0) {
                builder.append(',');
            }
            appendTemplateSpecJson(builder, request.getItemToGive(), OPTION_GIVE_TEXT, 0, 0, 0);
        }
        if (request.getOptionPolicy() == DialogOptionPolicy.CLICK_KEYWORD
                && hasText(request.getTargetKeyword())
                && request.getTargetKeyword().contains("看打")) {
            if (count++ > 0) {
                builder.append(',');
            }
            appendTemplateSpecJson(builder, request.getTargetKeyword(), XIULUO_ENTER_BATTLE_TEMPLATE, -6, 6, 4);
        }
        return count == 0 ? "" : builder.append(']').toString();
    }

    private static String targetKeywordAliases(String targetKeyword) {
        if (!hasText(targetKeyword)) {
            return "";
        }
        java.util.LinkedHashSet<String> aliases = new java.util.LinkedHashSet<>();
        aliases.add(targetKeyword.trim());
        for (String alias : TeleportConfig.MAP_ALIASES.getOrDefault(targetKeyword.trim(), java.util.List.of())) {
            if (hasText(alias)) {
                aliases.add(alias.trim());
            }
        }
        return String.join("|", aliases);
    }

    private static void appendTemplateSpecJson(
            StringBuilder builder,
            String name,
            String templatePath,
            int minOffsetX,
            int maxOffsetX,
            int randomRadiusY) {
        builder.append('{')
                .append("\"name\":\"").append(jsonEscape(safe(name))).append("\",")
                .append("\"templatePath\":\"").append(jsonEscape(safe(templatePath))).append("\",")
                .append("\"minOffsetX\":").append(minOffsetX).append(',')
                .append("\"maxOffsetX\":").append(maxOffsetX).append(',')
                .append("\"randomRadiusY\":").append(randomRadiusY);
        if (minOffsetX == maxOffsetX && randomRadiusY == 0) {
            builder.append(',')
                    .append("\"clickOffsetX\":").append(minOffsetX).append(',')
                    .append("\"clickOffsetY\":0");
        }
        builder.append('}');
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '"') {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }

    private static void putGreenTemplateSpecContext(Map<String, String> context, DialogHandleRequest request) {
        if (request.getGreenTemplateSpecs() == null || request.getGreenTemplateSpecs().isEmpty()) {
            context.put("greenTemplateSpecCount", "0");
            context.put("greenTemplateSpecNames", "");
            return;
        }
        int index = 0;
        StringBuilder names = new StringBuilder();
        for (GreenTemplateClickSpec spec : request.getGreenTemplateSpecs()) {
            if (spec == null) {
                continue;
            }
            if (!names.isEmpty()) {
                names.append('|');
            }
            names.append(safe(spec.name()));
            String prefix = "greenTemplateSpec." + index + ".";
            context.put(prefix + "name", safe(spec.name()));
            context.put(prefix + "templatePath", safe(spec.templatePath()));
            context.put(prefix + "minOffsetX", String.valueOf(spec.minOffsetX()));
            context.put(prefix + "maxOffsetX", String.valueOf(spec.maxOffsetX()));
            context.put(prefix + "randomRadiusY", String.valueOf(spec.randomRadiusY()));
            if (spec.minOffsetX() == spec.maxOffsetX() && spec.randomRadiusY() == 0) {
                context.put(prefix + "clickOffsetX", String.valueOf(spec.minOffsetX()));
                context.put(prefix + "clickOffsetY", "0");
            }
            index++;
        }
        context.put("greenTemplateSpecCount", String.valueOf(index));
        context.put("greenTemplateSpecNames", names.toString());
    }

    private static void putWhiteTemplateSpecContext(Map<String, String> context, DialogHandleRequest request) {
        if (request.getWhiteTemplateSpecs() == null || request.getWhiteTemplateSpecs().isEmpty()) {
            context.put("whiteTemplateSpecCount", "0");
            context.put("whiteTemplateSpecNames", "");
            return;
        }
        int index = 0;
        StringBuilder names = new StringBuilder();
        for (WhiteTemplateSpec spec : request.getWhiteTemplateSpecs()) {
            if (spec == null) {
                continue;
            }
            if (!names.isEmpty()) {
                names.append('|');
            }
            names.append(safe(spec.name()));
            String prefix = "whiteTemplateSpec." + index + ".";
            context.put(prefix + "name", safe(spec.name()));
            context.put(prefix + "templatePath", safe(spec.templatePath()));
            index++;
        }
        context.put("whiteTemplateSpecCount", String.valueOf(index));
        context.put("whiteTemplateSpecNames", names.toString());
    }

    private static String declaredFieldMismatch(
            Map<String, String> fields,
            DialogHandleRequest request,
            DialogResult result) {
        String mismatch = enumFieldMismatch(fields, "operation", request.getOperation(), "operation");
        if (mismatch != null) {
            return mismatch;
        }
        mismatch = enumFieldMismatch(fields, "optionPolicy", request.getOptionPolicy(), "optionPolicy");
        if (mismatch != null) {
            return mismatch;
        }
        mismatch = enumFieldMismatch(fields, "fallbackPolicy", request.getFallbackPolicy(), "fallbackPolicy");
        if (mismatch != null) {
            return mismatch;
        }
        mismatch = enumFieldMismatch(fields, "status", result.getStatus(), "status");
        if (mismatch != null) {
            return mismatch;
        }
        mismatch = enumFieldMismatch(fields, "type", result.getDialogType(), "type");
        if (mismatch != null) {
            return mismatch;
        }
        mismatch = enumFieldMismatch(fields, "dialogType", result.getDialogType(), "dialogType");
        if (mismatch != null) {
            return mismatch;
        }
        if (fields.containsKey("actionKey") && !safe(result.getActionKey()).equals(fields.get("actionKey"))) {
            return "actionKey mismatch: expected=" + safe(result.getActionKey())
                    + " actual=" + safe(fields.get("actionKey"));
        }
        return null;
    }

    private static String enumFieldMismatch(
            Map<String, String> fields,
            String field,
            Enum<?> expected,
            String label) {
        if (!fields.containsKey(field)) {
            return null;
        }
        String expectedText = enumName(expected);
        String actual = fields.get(field);
        if (!expectedText.equalsIgnoreCase(safe(actual))) {
            return label + " mismatch: expected=" + expectedText + " actual=" + safe(actual);
        }
        return null;
    }

    private static boolean keepsLocalPassthrough(CloudDecisionResult cloudResult, boolean gateEvaluated) {
        if (cloudResult == null || cloudResult.getMode() != CloudDecisionMode.EXECUTE) {
            return true;
        }
        if (cloudResult.isRequiredExecuteFailure()) {
            return false;
        }
        if (cloudResult.isExecuted()) {
            return false;
        }
        return !gateEvaluated
                && cloudResult.isCloudAvailable()
                && contains(cloudResult.getReason(), "percent");
    }

    private static boolean keepsAfterLocalSafeNoActionPassthrough(
            DialogResult localResult,
            CloudDecisionResult cloudResult,
            boolean gateEvaluated) {
        if (!gateEvaluated
                || !isSafeLocalNoAction(localResult)
                || cloudResult == null
                || cloudResult.getMode() != CloudDecisionMode.EXECUTE
                || cloudResult.isExecuted()
                || cloudResult.getResponse() == null) {
            return false;
        }
        Map<String, String> fields = fields(cloudResult.getResponse().getDecision());
        return "NO_ACTION".equals(normalizedUpper(fields.get("action")))
                && "NOT_FOUND".equals(normalizedUpper(fields.get("status")))
                && contains(fields.get("reason"), "requires-cloud-input")
                && rawCoordinateField(fields) == null;
    }

    private static boolean isSafeLocalNoAction(DialogResult localResult) {
        if (localResult == null
                || localResult.isClicked()
                || hasText(localResult.getActionKey())
                || localResult.getPreparedAction() != null
                || localResult.getRelativeX() != null
                || localResult.getRelativeY() != null
                || localResult.getAbsoluteX() != null
                || localResult.getAbsoluteY() != null) {
            return false;
        }
        DialogResultStatus status = localResult.getStatus();
        return status == DialogResultStatus.NO_DIALOG
                || status == DialogResultStatus.STORY_IGNORED;
    }

    private static String action(CloudDecisionResult cloudResult) {
        if (cloudResult == null || cloudResult.getResponse() == null) {
            return "";
        }
        return fields(cloudResult.getResponse().getDecision()).getOrDefault("action", "");
    }

    private static DialogResult requiredFailureResult(DialogResult localResult, String rejectReason) {
        return DialogResult.statusBuilder(
                        DialogResultStatus.FAILED,
                        localResult == null ? null : localResult.getDialogType())
                .matchedText("cloud-required-failure:" + safe(rejectReason))
                .build();
    }

    private static String acceptedReason(Map<String, String> fields, String fallback) {
        String reason = fields.get("reason");
        return hasText(reason)
                ? "execute percent gate hit; " + fallback + ": " + reason
                : "execute percent gate hit; " + fallback;
    }

    private static Map<String, String> fields(String decision) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!hasText(decision)) {
            return result;
        }
        String[] parts = decision.split(";", -1);
        for (String part : parts) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = part.substring(0, separator).trim();
            if (!key.isEmpty()) {
                result.put(key, part.substring(separator + 1).trim());
            }
        }
        return result;
    }

    private static String rawCoordinateField(Map<String, String> fields) {
        for (String key : fields.keySet()) {
            String lower = key.toLowerCase(Locale.ROOT);
            if ("click".equals(lower)
                    || "relativeclick".equals(lower)
                    || "absoluteclick".equals(lower)
                    || "coordinatespace".equals(lower)
                    || "x".equals(lower)
                    || "y".equals(lower)
                    || "windowx".equals(lower)
                    || "windowy".equals(lower)
                    || "screenx".equals(lower)
                    || "screeny".equals(lower)) {
                return key;
            }
        }
        return null;
    }

    private static String traceId(String taskCode, DialogHandleRequest request) {
        return "dialog-policy:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(request.getSourceTask())
                + ":" + safeTracePart(enumName(request.getOperation()));
    }

    private static String preClickTraceId(
            DialogPolicyPreClickCloudRequest request,
            String taskCode,
            String phase) {
        DialogHandleRequest dialogRequest = request.getDialogRequest();
        return "dialog-policy-pre-click:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(phase)
                + ":" + safeTracePart(request.getWindowId())
                + ":" + safeTracePart(dialogRequest.getSourceTask())
                + ":" + safeTracePart(enumName(dialogRequest.getOperation()))
                + ":" + safeTracePart(request.getDebugImageId());
    }

    private static String dialogTaskCode(String sourceTask) {
        if (sourceTask == null || sourceTask.isBlank()) {
            return DEFAULT_TASK_CODE;
        }
        String lower = sourceTask.toLowerCase(Locale.ROOT);
        if (lower.contains("xiuluo")) {
            return "xiuluo_v2";
        }
        if (lower.contains("wubei")) {
            return "wubei";
        }
        if (lower.contains("wuhuan") || lower.contains("five-ring") || lower.contains("five_ring")) {
            return "wuhuan_v2";
        }
        if (lower.contains("auto-battle") || lower.contains("auto_battle")) {
            return "auto_battle";
        }
        return DEFAULT_TASK_CODE;
    }

    private static String taskTypeName(String sourceTask) {
        String taskCode = dialogTaskCode(sourceTask);
        for (TaskType taskType : TaskType.values()) {
            if (taskType.getCode().equalsIgnoreCase(taskCode)) {
                return taskType.name();
            }
        }
        return TaskType.UNKNOWN.name();
    }

    private static String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private static String normalizedUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static String normalize(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private static String firstText(String first, String second) {
        return hasText(first) ? first : safe(second);
    }

    private static String diagnostic(CloudDecisionResponse response, String key) {
        if (response == null || response.getDiagnostics() == null || key == null) {
            return "";
        }
        return safe(response.getDiagnostics().get(key));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean contains(String value, String expected) {
        return value != null && value.contains(expected);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safeTracePart(String value) {
        String normalized = hasText(value) ? value.trim() : "unknown";
        return normalized.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private record DialogCandidate(String candidateId, String decision, DialogResult result) {
    }

    private record Box(int x, int y, int width, int height) {
        String toDecisionText() {
            return x + "," + y + "," + width + "," + height;
        }
    }

    private record PreClickParseResult(
            boolean accepted,
            DialogPolicyPreClickCloudDecision.Action action,
            Point click,
            Box candidateBox,
            String reason) {

        static PreClickParseResult acceptedClick(Point click, Box candidateBox) {
            return new PreClickParseResult(
                    true,
                    DialogPolicyPreClickCloudDecision.Action.CLICK,
                    click,
                    candidateBox,
                    "");
        }

        static PreClickParseResult acceptedNoClick(DialogPolicyPreClickCloudDecision.Action action, Box candidateBox) {
            return new PreClickParseResult(true, action, null, candidateBox, "");
        }

        static PreClickParseResult rejected(String reason) {
            return new PreClickParseResult(
                    false,
                    DialogPolicyPreClickCloudDecision.Action.ABORT,
                    null,
                    null,
                    reason);
        }
    }
}
