package com.bot.dhxy.cloud.xiuluo;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.task.xiuluo.XiuluoPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class XiuluoBrainCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(XiuluoBrainCloudDecisionService.class);
    private static final CloudDecisionServiceId SERVICE_ID = CloudDecisionServiceId.XIULUO_BRAIN;
    private static final String DEFAULT_TASK_CODE = "xiuluo_v2";
    private static final String DEFAULT_SOURCE = "xiuluo-brain";
    private static final String START_PHASE = "start";
    private static final String STEP_PHASE = "step";
    private static final String ACTION_OUTCOME_PHASE = "action-outcome";

    private final CloudDecisionCoordinator coordinator;

    public XiuluoBrainCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Requests the first required cloud command for a 修罗 brain session.
     *
     * @param request start facts for one bound window/task run; {@code windowId} and
     *                {@code taskRunId} are local safety identity fields and must match the cloud
     *                command before any command can be accepted
     * @return a fail-closed decision envelope; there is no local passthrough state
     */
    public XiuluoBrainDecision start(XiuluoBrainStartRequest request) {
        String taskCode = normalize(request == null ? null : request.getTaskCode(), DEFAULT_TASK_CODE);
        XiuluoPhase phase = request == null ? null : request.getInitialPhase();
        ExpectedIdentity expected = ExpectedIdentity.start(
                request == null ? null : request.getWindowId(),
                request == null ? null : request.getTaskRunId());
        return decide(
                taskCode,
                normalize(request == null ? null : request.getSource(), DEFAULT_SOURCE),
                START_PHASE,
                phase,
                expected,
                startContext(request, taskCode));
    }

    /**
     * Requests the next required cloud command for an existing 修罗 brain session.
     *
     * @param request step facts for one bound window/task run; {@code sessionId} and
     *                {@code stateSeq} identify the cloud-owned state being advanced
     * @return a fail-closed decision envelope; callers must only execute accepted cloud commands
     */
    public XiuluoBrainDecision step(XiuluoBrainStepRequest request) {
        String taskCode = normalize(request == null ? null : request.getTaskCode(), DEFAULT_TASK_CODE);
        XiuluoPhase phase = request == null ? null : request.getPhase();
        ExpectedIdentity expected = ExpectedIdentity.step(
                request == null ? null : request.getWindowId(),
                request == null ? null : request.getTaskRunId(),
                request == null ? null : request.getSessionId(),
                request == null ? 0L : request.getStateSeq(),
                request == null ? null : request.getPhaseToken());
        String invalidStepIdentity = invalidStepIdentity(expected);
        if (invalidStepIdentity != null) {
            XiuluoBrainDecision decision = XiuluoBrainDecision.localSafetyDenied(null, invalidStepIdentity);
            logDecision(taskCode, STEP_PHASE, expected, decision);
            return decision;
        }
        return decide(
                taskCode,
                normalize(request == null ? null : request.getSource(), DEFAULT_SOURCE),
                STEP_PHASE,
                phase,
                expected,
                stepContext(request, taskCode));
    }

    /**
     * Reports the factual result of executing the current cloud command before requesting another
     * command. The returned ack does not advance phase/state; it only proves the cloud accepted the
     * local execution report for the current {@code actionId}.
     *
     * @param request exact session/action identity plus local transaction facts for the executed
     *                phase; {@code localOutcomeNextPhase} is diagnostic evidence, not local
     *                transition authority
     * @return fail-closed outcome ack envelope
     */
    public XiuluoBrainActionOutcomeDecision actionOutcome(XiuluoBrainActionOutcomeRequest request) {
        String taskCode = normalize(request == null ? null : request.getTaskCode(), DEFAULT_TASK_CODE);
        ExpectedIdentity expected = ExpectedIdentity.actionOutcome(
                request == null ? null : request.getWindowId(),
                request == null ? null : request.getTaskRunId(),
                request == null ? null : request.getSessionId(),
                request == null ? 0L : request.getStateSeq(),
                request == null ? null : request.getPhaseToken(),
                request == null ? null : request.getActionId());
        String invalidIdentity = invalidActionOutcomeIdentity(expected, request == null ? null : request.getOutcome());
        if (invalidIdentity != null) {
            XiuluoBrainActionOutcomeDecision decision =
                    XiuluoBrainActionOutcomeDecision.localSafetyDenied(null, invalidIdentity);
            logActionOutcomeDecision(taskCode, expected, decision);
            return decision;
        }
        if (!coordinator.isActive(SERVICE_ID)) {
            XiuluoBrainActionOutcomeDecision decision =
                    XiuluoBrainActionOutcomeDecision.cloudRequiredFailure(null, "service inactive");
            logActionOutcomeDecision(taskCode, expected, decision);
            return decision;
        }

        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(SERVICE_ID)
                .traceId(traceId(taskCode,
                        normalize(request.getSource(), DEFAULT_SOURCE),
                        ACTION_OUTCOME_PHASE,
                        request.getPhase(),
                        expected))
                .taskCode(taskCode)
                .phase(ACTION_OUTCOME_PHASE)
                .windowId(expected.windowId())
                .taskRunId(expected.taskRunId())
                .localDecision("cloud-required")
                .context(actionOutcomeContext(request, taskCode))
                .build();

        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                "cloud-required",
                xiuluoBrainOutcomeExecutionGate(expected));
        XiuluoBrainActionOutcomeDecision decision = toActionOutcomeDecision(cloudResult, expected);
        logActionOutcomeDecision(taskCode, expected, decision);
        return decision;
    }

    private XiuluoBrainDecision decide(
            String taskCode,
            String source,
            String requestPhase,
            XiuluoPhase currentPhase,
            ExpectedIdentity expected,
            Map<String, String> context) {
        if (!coordinator.isActive(SERVICE_ID)) {
            XiuluoBrainDecision decision = XiuluoBrainDecision.cloudRequiredFailure(null, "service inactive");
            logDecision(taskCode, requestPhase, expected, decision);
            return decision;
        }

        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(SERVICE_ID)
                .traceId(traceId(taskCode, source, requestPhase, currentPhase, expected))
                .taskCode(taskCode)
                .phase(requestPhase)
                .windowId(expected.windowId())
                .taskRunId(expected.taskRunId())
                .localDecision("cloud-required")
                .context(context)
                .build();

        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                "cloud-required",
                xiuluoBrainExecutionGate(expected));
        XiuluoBrainDecision decision = toDecision(cloudResult);
        logDecision(taskCode, requestPhase, expected, decision);
        return decision;
    }

    private CloudDecisionExecutionGate xiuluoBrainExecutionGate(ExpectedIdentity expected) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == SERVICE_ID;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                ParseResult parsed = parse(response, expected);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                return CloudDecisionExecutionGate.GateResult.accepted(
                        serialize(parsed.response()),
                        "execute percent gate hit; using xiuluo brain command");
            }
        };
    }

    private CloudDecisionExecutionGate xiuluoBrainOutcomeExecutionGate(ExpectedIdentity expected) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == SERVICE_ID;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                OutcomeParseResult parsed = parseOutcomeAck(response, expected);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(),
                        "execute percent gate hit; using xiuluo brain action outcome ack");
            }
        };
    }

    private XiuluoBrainDecision toDecision(CloudDecisionResult cloudResult) {
        if (cloudResult == null) {
            return XiuluoBrainDecision.cloudRequiredFailure(null, "cloud result missing");
        }
        if (!cloudResult.isExecuted()) {
            String reason = cloudResult.getReason();
            if (isLocalSafetyReason(reason)) {
                return XiuluoBrainDecision.localSafetyDenied(cloudResult, reason);
            }
            return XiuluoBrainDecision.cloudRequiredFailure(cloudResult, reason);
        }
        ParseResult parsed = parseEffectiveDecision(cloudResult.getEffectiveDecision());
        if (!parsed.accepted()) {
            return XiuluoBrainDecision.cloudRequiredFailure(cloudResult, parsed.reason());
        }
        return XiuluoBrainDecision.accepted(cloudResult, parsed.response());
    }

    private XiuluoBrainActionOutcomeDecision toActionOutcomeDecision(
            CloudDecisionResult cloudResult,
            ExpectedIdentity expected) {
        if (cloudResult == null) {
            return XiuluoBrainActionOutcomeDecision.cloudRequiredFailure(null, "cloud result missing");
        }
        if (!cloudResult.isExecuted()) {
            String reason = cloudResult.getReason();
            if (isLocalSafetyReason(reason)) {
                return XiuluoBrainActionOutcomeDecision.localSafetyDenied(cloudResult, reason);
            }
            return XiuluoBrainActionOutcomeDecision.cloudRequiredFailure(cloudResult, reason);
        }
        OutcomeParseResult parsed = parseOutcomeDecision(cloudResult.getEffectiveDecision(), expected);
        if (!parsed.accepted()) {
            return XiuluoBrainActionOutcomeDecision.cloudRequiredFailure(cloudResult, parsed.reason());
        }
        if ("RESET_REQUIRED".equals(parsed.status())) {
            return XiuluoBrainActionOutcomeDecision.resetRequired(cloudResult, parsed.reason());
        }
        return XiuluoBrainActionOutcomeDecision.accepted(cloudResult, parsed.status());
    }

    private static ParseResult parse(CloudDecisionResponse response, ExpectedIdentity expected) {
        if (response == null) {
            return ParseResult.rejected("response missing");
        }
        ParseResult parsed = parseDecision(response.getDecision());
        if (!parsed.accepted()) {
            return parsed;
        }
        XiuluoBrainResponse command = parsed.response();
        if (!hasText(command.getWindowId())) {
            return ParseResult.rejected("windowId is required");
        }
        if (!command.getWindowId().equals(expected.windowId())) {
            return ParseResult.rejected("local safety denied: windowId mismatch expected="
                    + safe(expected.windowId()) + " actual=" + command.getWindowId());
        }
        if (!hasText(command.getTaskRunId())) {
            return ParseResult.rejected("taskRunId is required");
        }
        if (!command.getTaskRunId().equals(expected.taskRunId())) {
            return ParseResult.rejected("local safety denied: taskRunId mismatch expected="
                    + safe(expected.taskRunId()) + " actual=" + command.getTaskRunId());
        }
        if (hasText(expected.sessionId()) && !command.getSessionId().equals(expected.sessionId())) {
            return ParseResult.rejected("local safety denied: sessionId mismatch expected="
                    + expected.sessionId() + " actual=" + command.getSessionId());
        }
        if (expected.stateSeq() > 0L && command.getStateSeq() <= expected.stateSeq()) {
            return ParseResult.rejected("stateSeq must advance expectedGreaterThan="
                    + expected.stateSeq() + " actual=" + command.getStateSeq());
        }
        if (hasText(expected.phaseToken())) {
            if (!hasText(command.getAcceptedPhaseToken())) {
                return ParseResult.rejected("acceptedPhaseToken is required for step");
            }
            if (!command.getAcceptedPhaseToken().equals(expected.phaseToken())) {
                return ParseResult.rejected("local safety denied: acceptedPhaseToken mismatch expected="
                        + expected.phaseToken() + " actual=" + command.getAcceptedPhaseToken());
            }
        }
        return parsed;
    }

    private static ParseResult parseEffectiveDecision(String decision) {
        return parseDecision(decision);
    }

    private static OutcomeParseResult parseOutcomeAck(CloudDecisionResponse response, ExpectedIdentity expected) {
        if (response == null) {
            return OutcomeParseResult.rejected("response missing");
        }
        return parseOutcomeDecision(response.getDecision(), expected);
    }

    private static OutcomeParseResult parseOutcomeDecision(String decision, ExpectedIdentity expected) {
        Map<String, String> fields = fields(decision);
        String status = fields.get("status");
        if (!"ACCEPTED".equals(status) && !"DUPLICATE_REPLAY".equals(status) && !"RESET_REQUIRED".equals(status)) {
            return OutcomeParseResult.rejected(
                    "outcome status must be ACCEPTED, DUPLICATE_REPLAY, or RESET_REQUIRED: " + safe(status));
        }
        if (!safe(expected.windowId()).equals(fields.get("windowId"))) {
            return OutcomeParseResult.rejected("local safety denied: windowId mismatch expected="
                    + safe(expected.windowId()) + " actual=" + safe(fields.get("windowId")));
        }
        if (!safe(expected.taskRunId()).equals(fields.get("taskRunId"))) {
            return OutcomeParseResult.rejected("local safety denied: taskRunId mismatch expected="
                    + safe(expected.taskRunId()) + " actual=" + safe(fields.get("taskRunId")));
        }
        if (!safe(expected.sessionId()).equals(fields.get("sessionId"))) {
            return OutcomeParseResult.rejected("local safety denied: sessionId mismatch expected="
                    + safe(expected.sessionId()) + " actual=" + safe(fields.get("sessionId")));
        }
        Long stateSeq = parseLong(fields.get("stateSeq"));
        if (stateSeq == null || stateSeq != expected.stateSeq()) {
            return OutcomeParseResult.rejected("local safety denied: stateSeq mismatch expected="
                    + expected.stateSeq() + " actual=" + safe(fields.get("stateSeq")));
        }
        if (!safe(expected.phaseToken()).equals(fields.get("phaseToken"))) {
            return OutcomeParseResult.rejected("local safety denied: phaseToken mismatch expected="
                    + safe(expected.phaseToken()) + " actual=" + safe(fields.get("phaseToken")));
        }
        if (!safe(expected.actionId()).equals(fields.get("actionId"))) {
            return OutcomeParseResult.rejected("local safety denied: actionId mismatch expected="
                    + safe(expected.actionId()) + " actual=" + safe(fields.get("actionId")));
        }
        String reason = fields.get("reason");
        if (!hasText(reason)) {
            return OutcomeParseResult.rejected("reason is required");
        }
        if ("RESET_REQUIRED".equals(status) && !isRecoverableSessionResetReason(reason)) {
            return OutcomeParseResult.rejected("RESET_REQUIRED reason is not recoverable: " + safe(reason));
        }
        return OutcomeParseResult.accepted(status, reason);
    }

    private static ParseResult parseDecision(String decision) {
        Map<String, String> fields = fields(decision);
        String status = fields.get("status");
        if ("REJECTED".equals(status)) {
            return ParseResult.rejected("cloud rejected: " + safe(fields.get("reason")));
        }
        if ("RESET_REQUIRED".equals(status)) {
            return ParseResult.rejected("cloud reset required during command step: " + safe(fields.get("reason")));
        }
        String sessionId = fields.get("sessionId");
        if (!hasText(sessionId)) {
            return ParseResult.rejected("sessionId is required");
        }
        Long stateSeq = parseLong(fields.get("stateSeq"));
        if (stateSeq == null || stateSeq <= 0L) {
            return ParseResult.rejected("stateSeq must be positive");
        }
        String phaseToken = fields.get("phaseToken");
        if (!hasText(phaseToken)) {
            return ParseResult.rejected("phaseToken is required");
        }
        String acceptedPhaseToken = fields.get("acceptedPhaseToken");
        XiuluoPhase phase = parseEnum(XiuluoPhase.class, fields.get("phase"));
        if (phase == null) {
            return ParseResult.rejected("phase must parse to XiuluoPhase: " + safe(fields.get("phase")));
        }
        XiuluoBrainActionType actionType = parseEnum(XiuluoBrainActionType.class, fields.get("action"));
        if (actionType == null) {
            return ParseResult.rejected("action must parse to XiuluoBrainActionType: " + safe(fields.get("action")));
        }
        if (actionType == XiuluoBrainActionType.COMPLETE_ROUND && phase != XiuluoPhase.ROUND_DONE) {
            return ParseResult.rejected("COMPLETE_ROUND requires phase ROUND_DONE");
        }
        if (actionType == XiuluoBrainActionType.STOP_TASK && phase != XiuluoPhase.STOPPED) {
            return ParseResult.rejected("STOP_TASK requires phase STOPPED");
        }
        if (actionType == XiuluoBrainActionType.RESTART_ROUND && phase != XiuluoPhase.PREPARE_ROUND) {
            return ParseResult.rejected("RESTART_ROUND requires phase PREPARE_ROUND");
        }
        if (actionType == XiuluoBrainActionType.FAIL_TASK && phase != XiuluoPhase.FAILED) {
            return ParseResult.rejected("FAIL_TASK requires phase FAILED");
        }
        if (actionType == XiuluoBrainActionType.EXECUTE_PHASE && phase.isTerminal()) {
            return ParseResult.rejected("EXECUTE_PHASE target phase must be non-terminal");
        }
        if (actionType == XiuluoBrainActionType.EXECUTE_PHASE && !isCloudExecutableCommandPhase(phase)) {
            return ParseResult.rejected("EXECUTE_PHASE target phase must be executable: " + phase);
        }
        if (actionType == XiuluoBrainActionType.RUN_CLEANUP && phase.isTerminal()) {
            return ParseResult.rejected("RUN_CLEANUP target phase must be non-terminal");
        }
        if (actionType == XiuluoBrainActionType.RUN_CLEANUP && !isCloudExecutableCommandPhase(phase)) {
            return ParseResult.rejected("RUN_CLEANUP target phase must be executable: " + phase);
        }
        String actionId = fields.get("actionId");
        if (!hasText(actionId)) {
            return ParseResult.rejected("actionId is required");
        }
        String cleanupType = fields.get("cleanupType");
        String retryKey = fields.get("retryKey");
        int attempt = (int) parsePositiveLong(fields.get("attempt"), 0L);
        int maxAttempts = (int) parsePositiveLong(fields.get("maxAttempts"), 0L);
        if (actionType == XiuluoBrainActionType.RUN_CLEANUP) {
            if (!hasText(cleanupType)) {
                return ParseResult.rejected("cleanupType is required for RUN_CLEANUP");
            }
            if (!hasText(retryKey)) {
                return ParseResult.rejected("retryKey is required for RUN_CLEANUP");
            }
            if (attempt <= 0) {
                return ParseResult.rejected("attempt must be positive for RUN_CLEANUP");
            }
            if (maxAttempts <= 0) {
                return ParseResult.rejected("maxAttempts must be positive for RUN_CLEANUP");
            }
        }
        String reason = fields.get("reason");
        if (!hasText(reason)) {
            return ParseResult.rejected("reason is required");
        }
        return ParseResult.accepted(XiuluoBrainResponse.builder()
                .windowId(fields.get("windowId"))
                .taskRunId(fields.get("taskRunId"))
                .sessionId(sessionId)
                .stateSeq(stateSeq)
                .phaseToken(phaseToken)
                .acceptedPhaseToken(acceptedPhaseToken)
                .phase(phase)
                .actionType(actionType)
                .actionId(actionId)
                .cleanupType(cleanupType)
                .retryKey(retryKey)
                .attempt(attempt)
                .maxAttempts(maxAttempts)
                .reason(reason)
                .diagnostics(Map.copyOf(fields))
                .build());
    }

    private static Map<String, String> startContext(XiuluoBrainStartRequest request, String taskCode) {
        Map<String, String> context = baseContext(taskCode, request == null ? null : request.getContext());
        context.put("hook", "xiuluo-brain-start");
        context.put("windowId", safe(request == null ? null : request.getWindowId()));
        context.put("taskRunId", safe(request == null ? null : request.getTaskRunId()));
        context.put("initialPhase", enumName(request == null ? null : request.getInitialPhase()));
        return Map.copyOf(context);
    }

    private static Map<String, String> stepContext(XiuluoBrainStepRequest request, String taskCode) {
        Map<String, String> context = baseContext(taskCode, request == null ? null : request.getContext());
        context.put("hook", "xiuluo-brain-step");
        context.put("windowId", safe(request == null ? null : request.getWindowId()));
        context.put("taskRunId", safe(request == null ? null : request.getTaskRunId()));
        context.put("sessionId", safe(request == null ? null : request.getSessionId()));
        context.put("stateSeq", Long.toString(request == null ? 0L : request.getStateSeq()));
        context.put("phaseToken", safe(request == null ? null : request.getPhaseToken()));
        context.put("phase", enumName(request == null ? null : request.getPhase()));
        context.put("lastActionId", safe(request == null ? null : request.getLastActionId()));
        return Map.copyOf(context);
    }

    private static Map<String, String> actionOutcomeContext(
            XiuluoBrainActionOutcomeRequest request,
            String taskCode) {
        Map<String, String> context = baseContext(taskCode, request == null ? null : request.getContext());
        context.put("hook", "xiuluo-brain-action-outcome");
        context.put("windowId", safe(request == null ? null : request.getWindowId()));
        context.put("taskRunId", safe(request == null ? null : request.getTaskRunId()));
        context.put("sessionId", safe(request == null ? null : request.getSessionId()));
        context.put("stateSeq", Long.toString(request == null ? 0L : request.getStateSeq()));
        context.put("phaseToken", safe(request == null ? null : request.getPhaseToken()));
        context.put("phase", enumName(request == null ? null : request.getPhase()));
        context.put("actionId", safe(request == null ? null : request.getActionId()));
        context.put("outcome", safe(request == null ? null : request.getOutcome()));
        context.put("transactionResult", safe(request == null ? null : request.getTransactionResult()));
        context.put("yieldPolicy", safe(request == null ? null : request.getYieldPolicy()));
        context.put("localOutcomeNextPhase", enumName(request == null ? null : request.getLocalOutcomeNextPhase()));
        context.put("message", safe(request == null ? null : request.getMessage()));
        if (request != null && request.getEvidencePaths() != null && !request.getEvidencePaths().isEmpty()) {
            context.put("evidencePaths", String.join(",", request.getEvidencePaths()));
        }
        return Map.copyOf(context);
    }

    private static Map<String, String> baseContext(String taskCode, Map<String, String> extraContext) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("serviceId", SERVICE_ID.name());
        context.put("activeTaskCode", normalize(taskCode, DEFAULT_TASK_CODE));
        if (extraContext != null) {
            extraContext.forEach((key, value) -> {
                String normalizedKey = normalize(key, null);
                if (normalizedKey != null) {
                    context.put(normalizedKey, safe(value));
                }
            });
        }
        return context;
    }

    private static String serialize(XiuluoBrainResponse response) {
        return "windowId=" + safe(response.getWindowId())
                + ";taskRunId=" + safe(response.getTaskRunId())
                + ";sessionId=" + response.getSessionId()
                + ";stateSeq=" + response.getStateSeq()
                + ";phaseToken=" + response.getPhaseToken()
                + acceptedPhaseTokenPart(response)
                + ";phase=" + response.getPhase()
                + ";action=" + response.getActionType()
                + ";actionId=" + response.getActionId()
                + cleanupPart(response)
                + ";reason=" + response.getReason();
    }

    private static String cleanupPart(XiuluoBrainResponse response) {
        if (response.getActionType() != XiuluoBrainActionType.RUN_CLEANUP) {
            return "";
        }
        return ";cleanupType=" + safe(response.getCleanupType())
                + ";retryKey=" + safe(response.getRetryKey())
                + ";attempt=" + response.getAttempt()
                + ";maxAttempts=" + response.getMaxAttempts();
    }

    private static String acceptedPhaseTokenPart(XiuluoBrainResponse response) {
        return hasText(response.getAcceptedPhaseToken())
                ? ";acceptedPhaseToken=" + response.getAcceptedPhaseToken()
                : "";
    }

    private static Map<String, String> fields(String decision) {
        Map<String, String> result = new LinkedHashMap<>();
        if (decision == null || decision.isBlank()) {
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

    private static String traceId(
            String taskCode,
            String source,
            String requestPhase,
            XiuluoPhase currentPhase,
            ExpectedIdentity expected) {
        return "xiuluo-brain:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(source)
                + ":" + safeTracePart(requestPhase)
                + ":" + safeTracePart(enumName(currentPhase))
                + ":" + safeTracePart(expected.windowId())
                + ":" + safeTracePart(expected.taskRunId());
    }

    private void logDecision(
            String taskCode,
            String requestPhase,
            ExpectedIdentity expected,
            XiuluoBrainDecision decision) {
        XiuluoBrainResponse response = decision.getResponse();
        log.info("XIULUO_BRAIN decision serviceId={} status={} taskCode={} phase={} sessionId={} "
                        + "stateSeq={} phaseToken={} actionId={} reason={} windowId={} taskRunId={} rejectReason={}",
                SERVICE_ID,
                decision.getStatus(),
                taskCode,
                response == null ? requestPhase : response.getPhase(),
                response == null ? expectedSessionId(expected) : response.getSessionId(),
                response == null ? expectedStateSeq(expected) : response.getStateSeq(),
                response == null ? expectedPhaseToken(expected) : response.getPhaseToken(),
                response == null ? null : response.getActionId(),
                response == null ? null : response.getReason(),
                response == null ? expectedWindowId(expected) : response.getWindowId(),
                response == null ? expectedTaskRunId(expected) : response.getTaskRunId(),
                decision.getRejectReason());
    }

    private void logActionOutcomeDecision(
            String taskCode,
            ExpectedIdentity expected,
            XiuluoBrainActionOutcomeDecision decision) {
        Map<String, String> responseFields = actionOutcomeResponseFields(decision);
        String sidecarPid = actionOutcomeDiagnostic(decision, responseFields, "sidecarPid");
        String sidecarVersion = actionOutcomeDiagnostic(decision, responseFields, "sidecarVersion");
        String xiuluoResetProtocol = actionOutcomeDiagnostic(decision, responseFields, "xiuluoResetProtocol");
        log.info("XIULUO_BRAIN actionOutcome serviceId={} status={} taskCode={} sessionId={} stateSeq={} "
                        + "phaseToken={} actionId={} windowId={} taskRunId={} outcomeStatus={} rejectReason={} "
                        + "sidecarPid={} sidecarVersion={} xiuluoResetProtocol={}",
                SERVICE_ID,
                decision.getStatus(),
                taskCode,
                expectedSessionId(expected),
                expectedStateSeq(expected),
                expectedPhaseToken(expected),
                expectedActionId(expected),
                expectedWindowId(expected),
                expectedTaskRunId(expected),
                decision.getOutcomeStatus(),
                decision.getRejectReason(),
                sidecarPid,
                sidecarVersion,
                xiuluoResetProtocol);
    }

    private static Map<String, String> actionOutcomeResponseFields(XiuluoBrainActionOutcomeDecision decision) {
        if (decision == null || decision.getCloudResult() == null) {
            return Map.of();
        }
        return fields(decision.getCloudResult().getEffectiveDecision());
    }

    private static String actionOutcomeDiagnostic(
            XiuluoBrainActionOutcomeDecision decision,
            Map<String, String> responseFields,
            String key) {
        String value = responseFields == null ? null : responseFields.get(key);
        if (hasText(value)) {
            return value;
        }
        if (decision == null
                || decision.getCloudResult() == null
                || decision.getCloudResult().getResponse() == null
                || decision.getCloudResult().getResponse().getDiagnostics() == null) {
            return "";
        }
        return safe(decision.getCloudResult().getResponse().getDiagnostics().get(key));
    }

    private static boolean isLocalSafetyReason(String reason) {
        return reason != null && reason.contains("local safety denied");
    }

    private static boolean isRecoverableSessionResetReason(String reason) {
        return reason != null && reason.toLowerCase(Locale.ROOT).contains("sessionid not found");
    }

    private static Long parseLong(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String invalidStepIdentity(ExpectedIdentity expected) {
        if (!hasText(expected.windowId())) {
            return "local safety denied: windowId is required";
        }
        if (!hasText(expected.taskRunId())) {
            return "local safety denied: taskRunId is required";
        }
        if (!hasText(expected.sessionId())) {
            return "local safety denied: sessionId is required";
        }
        if (expected.stateSeq() <= 0L) {
            return "local safety denied: stateSeq must be positive";
        }
        if (!hasText(expected.phaseToken())) {
            return "local safety denied: phaseToken is required";
        }
        return null;
    }

    private static String invalidActionOutcomeIdentity(ExpectedIdentity expected, String outcome) {
        String stepIdentity = invalidStepIdentity(expected);
        if (stepIdentity != null) {
            return stepIdentity;
        }
        if (!hasText(expected.actionId())) {
            return "local safety denied: actionId is required";
        }
        if (!hasText(outcome)) {
            return "local safety denied: outcome is required";
        }
        return null;
    }

    private static String safeTracePart(String value) {
        String normalized = normalize(value, "unknown");
        return normalized.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private static String expectedSessionId(ExpectedIdentity expected) {
        return expected == null ? null : expected.sessionId();
    }

    private static long expectedStateSeq(ExpectedIdentity expected) {
        return expected == null ? 0L : expected.stateSeq();
    }

    private static String expectedWindowId(ExpectedIdentity expected) {
        return expected == null ? null : expected.windowId();
    }

    private static String expectedTaskRunId(ExpectedIdentity expected) {
        return expected == null ? null : expected.taskRunId();
    }

    private static String expectedPhaseToken(ExpectedIdentity expected) {
        return expected == null ? null : expected.phaseToken();
    }

    private static String expectedActionId(ExpectedIdentity expected) {
        return expected == null ? null : expected.actionId();
    }

    private static long parsePositiveLong(String value, long fallback) {
        Long parsed = parseLong(value);
        return parsed == null || parsed <= 0L ? fallback : parsed;
    }

    private static boolean isCloudExecutableCommandPhase(XiuluoPhase phase) {
        return switch (phase) {
            case PREPARE_ROUND,
                    ACCEPT_TASK_NAVIGATE_TO_NPC,
                    ACCEPT_TASK_CLICK_NPC,
                    ACCEPT_TASK_DIALOG,
                    READ_OBJECTIVE,
                    AFTER_ACCEPT_MAINTENANCE_CHECK,
                    BEFORE_ROUTE_MAINTENANCE_CHECK,
                    TRY_TRACKER_SHORTCUT,
                    WAIT_TRACKER_SHORTCUT_PATHING,
                    NAVIGATE_TO_TARGET,
                    CLICK_TARGET_NPC,
                    CONFIRM_ENTER_BATTLE,
                    WAIT_COMBAT,
                    RETURN_HOME,
                    NAVIGATE_BACK_TO_START,
                    WAIT_TEAM_READY,
                    WAIT_TEAM_RETURN -> true;
            default -> false;
        };
    }

    private record ExpectedIdentity(
            String windowId,
            String taskRunId,
            String sessionId,
            long stateSeq,
            String phaseToken,
            String actionId) {
        static ExpectedIdentity start(String windowId, String taskRunId) {
            return new ExpectedIdentity(windowId, taskRunId, null, 0L, null, null);
        }

        static ExpectedIdentity step(String windowId, String taskRunId, String sessionId, long stateSeq, String phaseToken) {
            return new ExpectedIdentity(windowId, taskRunId, sessionId, stateSeq, phaseToken, null);
        }

        static ExpectedIdentity actionOutcome(
                String windowId,
                String taskRunId,
                String sessionId,
                long stateSeq,
                String phaseToken,
                String actionId) {
            return new ExpectedIdentity(windowId, taskRunId, sessionId, stateSeq, phaseToken, actionId);
        }
    }

    private record ParseResult(boolean accepted, XiuluoBrainResponse response, String reason) {
        static ParseResult accepted(XiuluoBrainResponse response) {
            return new ParseResult(true, response, null);
        }

        static ParseResult rejected(String reason) {
            return new ParseResult(false, null, reason);
        }
    }

    private record OutcomeParseResult(boolean accepted, String status, String reason) {
        static OutcomeParseResult accepted(String status, String reason) {
            return new OutcomeParseResult(true, status, reason);
        }

        static OutcomeParseResult rejected(String reason) {
            return new OutcomeParseResult(false, null, reason);
        }
    }
}
