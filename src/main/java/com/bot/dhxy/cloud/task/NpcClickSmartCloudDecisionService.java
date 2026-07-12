package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionMode;
import com.bot.dhxy.cloud.decision.CloudDecisionProperties;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.model.npc.NpcClickRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * NPC_CLICK_SMART execute gate for covered ordinary NPC dialog clicks.
 *
 * <p>This class intentionally contains no yellow/purple/menu/template recognition. It only sends
 * raw screenshot payload and target metadata to the configured cloud brain, then validates that a
 * returned click is a plain left `WINDOW_RELATIVE` point inside both the window and request ROI.</p>
 */
@Service
public class NpcClickSmartCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(NpcClickSmartCloudDecisionService.class);
    private static final String PHASE = "npc-click-smart";
    private static final String NPC_CLICK_START = "NPC_CLICK_START";
    private static final String NPC_CLICK_POLL = "NPC_CLICK_POLL";
    private static final String DIRECT_COMBAT_AUTHORIZE = "DIRECT_COMBAT_AUTHORIZE";
    private static final String OUTCOME_VERIFIED = "VERIFIED";
    private static final String OUTCOME_VERIFICATION_FAILED = "VERIFICATION_FAILED";
    private static final String OUTCOME_CANCELLED = "CANCELLED";
    private static final String OUTCOME_FINAL_FAILED = "FINAL_FAILED";
    private static final String LOCAL_DECISION = "action=LOCAL_SHADOW_ONLY;reason=local-npc-click-not-executed";
    private static final String COORDINATE_SPACE_KEY = "coordinateSpace";
    private static final String WINDOW_RELATIVE = "WINDOW_RELATIVE";
    private static final double MIN_EXECUTE_CONFIDENCE = 0.70d;

    private final CloudDecisionCoordinator coordinator;
    private final CloudDecisionProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public NpcClickSmartCloudDecisionService(
            CloudDecisionCoordinator coordinator,
            CloudDecisionProperties properties) {
        this.coordinator = coordinator;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1L, properties.getTimeoutMs())))
                .build();
    }

    public boolean isActive() {
        return coordinator.isActive(CloudDecisionServiceId.NPC_CLICK_SMART);
    }

    public NpcClickSmartCloudSession startSession(NpcClickSmartCloudRequest request) {
        if (!coordinator.isActive(CloudDecisionServiceId.NPC_CLICK_SMART)) {
            return NpcClickSmartCloudSession.builder()
                    .status(NpcClickSmartCloudSession.Status.DISABLED)
                    .reason("service disabled")
                    .build();
        }
        String validationError = requestValidationError(request);
        if (validationError != null) {
            return NpcClickSmartCloudSession.builder()
                    .status(NpcClickSmartCloudSession.Status.REQUIRED_FAILURE)
                    .reason(validationError)
                    .build();
        }
        Map<String, String> context = mutableContext(request);
        context.put("queueOperation", NPC_CLICK_START);
        context.put("messageType", NPC_CLICK_START);
        context.put("sessionId", safe(request.getSessionId()));
        CloudDecisionRequest cloudRequest = cloudRequest(request, "npc-click-start", context,
                "action=NPC_CLICK_START;sessionId=" + safe(request.getSessionId()));
        CloudDecisionResult result = coordinator.shadow(cloudRequest, cloudRequest.getLocalDecision(),
                acceptAnyResponseGate());
        if (!result.isExecuted() || result.getResponse() == null) {
            return NpcClickSmartCloudSession.builder()
                    .status(NpcClickSmartCloudSession.Status.REQUIRED_FAILURE)
                    .reason(result.getReason())
                    .build();
        }
        Map<String, String> fields = fields(result.getResponse().getDecision());
        String sessionId = normalize(fields.get("sessionId"), null);
        String status = normalizedUpper(firstText(fields.get("status"), fields.get("action")));
        if (!hasText(sessionId) || (!"SESSION_STARTED".equals(status) && !"NPC_CLICK_START".equals(status))) {
            return NpcClickSmartCloudSession.builder()
                    .status(NpcClickSmartCloudSession.Status.REQUIRED_FAILURE)
                    .sessionId(sessionId)
                    .windowId(request.getWindowId())
                    .taskRunId(request.getTaskRunId())
                    .reason("cloud did not start NPC click session: " + result.getResponse().getDecision())
                    .build();
        }
        log.info("NPC_CLICK_START accepted: sessionId={} npc={} task={} windowId={} taskRunId={}",
                sessionId, request.getNpcRequest().npcName(), request.getTaskCode(),
                request.getWindowId(), request.getTaskRunId());
        return NpcClickSmartCloudSession.builder()
                .status(NpcClickSmartCloudSession.Status.STARTED)
                .sessionId(sessionId)
                .windowId(request.getWindowId())
                .taskRunId(request.getTaskRunId())
                .reason(firstText(fields.get("reason"), result.getReason()))
                .build();
    }

    public NpcClickSmartQueueMessage pollNext(
            NpcClickSmartCloudRequest request,
            NpcClickSmartCloudSession session) {
        if (request == null || session == null || !session.accepted()) {
            return NpcClickSmartQueueMessage.builder()
                    .type(NpcClickSmartQueueMessage.Type.INVALID)
                    .reason("missing accepted NPC click session")
                    .build();
        }
        String validationError = requestValidationError(request);
        if (validationError != null) {
            return NpcClickSmartQueueMessage.builder()
                    .type(NpcClickSmartQueueMessage.Type.INVALID)
                    .sessionId(session.getSessionId())
                    .windowId(request == null ? null : request.getWindowId())
                    .taskRunId(request == null ? null : request.getTaskRunId())
                    .reason(validationError)
                    .build();
        }
        Map<String, String> context = mutableContext(request);
        context.put("queueOperation", NPC_CLICK_POLL);
        context.put("messageType", NPC_CLICK_POLL);
        context.put("sessionId", session.getSessionId());
        CloudDecisionRequest cloudRequest = cloudRequest(request, "npc-click-poll", context,
                "action=NPC_CLICK_POLL;sessionId=" + session.getSessionId());
        final NpcClickSmartQueueMessage[] parsedHolder = new NpcClickSmartQueueMessage[1];
        CloudDecisionResult result = coordinator.shadow(cloudRequest, cloudRequest.getLocalDecision(),
                queueMessageGate(request, parsedHolder));
        if (!result.isExecuted()) {
            return NpcClickSmartQueueMessage.builder()
                    .type(NpcClickSmartQueueMessage.Type.INVALID)
                    .sessionId(session.getSessionId())
                    .windowId(request.getWindowId())
                    .taskRunId(request.getTaskRunId())
                    .reason(result.getReason())
                    .build();
        }
        NpcClickSmartQueueMessage parsed = parsedHolder[0];
        if (parsed == null) {
            return NpcClickSmartQueueMessage.builder()
                    .type(NpcClickSmartQueueMessage.Type.INVALID)
                    .sessionId(session.getSessionId())
                    .windowId(request.getWindowId())
                    .taskRunId(request.getTaskRunId())
                    .reason("cloud queue message parse failed")
                    .build();
        }
        log.info("NPC_CLICK_POLL consumed: sessionId={} type={} decisionId={} click={} box={} reason={}",
                parsed.getSessionId(), parsed.getType(), parsed.getDecisionId(),
                parsed.getWindowRelativeClickPoint(), parsed.getCandidateBox(), parsed.getReason());
        return parsed;
    }

    public boolean reportOutcome(
            NpcClickSmartCloudRequest request,
            NpcClickSmartQueueMessage message,
            NpcClickSmartQueueOutcome outcome,
            String verificationStrength,
            String verificationActionKey,
            String verificationMatchedText,
            String reason) {
        String disabledReason = outcomeTransportDisabledReason();
        if (disabledReason != null) {
            log.warn("NPC_CLICK_SMART queue outcome not submitted: reason={} sessionId={} decisionId={} outcome={}",
                    disabledReason,
                    message == null ? null : message.getSessionId(),
                    message == null ? null : message.getDecisionId(),
                    outcome);
            return false;
        }
        if (request == null || request.getNpcRequest() == null || message == null || outcome == null) {
            log.warn("NPC_CLICK_SMART queue outcome not submitted: reason=missing request/message/outcome");
            return false;
        }
        String body;
        try {
            body = serializeQueueOutcome(request, message, outcome,
                    verificationStrength, verificationActionKey, verificationMatchedText, reason);
        } catch (JsonProcessingException e) {
            log.warn("NPC_CLICK_SMART queue outcome not submitted: reason=json serialize failure: {} sessionId={} decisionId={}",
                    e.getOriginalMessage(), message.getSessionId(), message.getDecisionId(), e);
            return false;
        }
        return postOutcome(body, request.getNpcRequest().npcName(), message.getDecisionId(), message.getSessionId(), outcome.name());
    }

    /**
     * CR267: ask cloud whether the current structured task facts authorize entering the game's
     * direct combat-click mode ({@code Alt+A} scene transition).
     *
     * <p>The request intentionally carries no screenshot. Cloud decides only from task facts
     * (target role, 五倍 probe target-ready, ordinary-FIFO terminal state, canonical map and
     * player/target logical coordinates with the task's existing tolerance) and answers with an
     * independent {@code ENTER_DIRECT_COMBAT} action or a refusal. Any transport/config failure is
     * a refusal; it never authorizes a local scene switch.</p>
     *
     * @param request light cloud request with identity fields and the {@link NpcClickRequest}
     *                business facts; image payload fields may be empty.
     * @return structured authorization; {@code authorized()} is true only for an explicit
     *         {@code action=ENTER_DIRECT_COMBAT;status=AUTHORIZED} cloud answer.
     */
    public NpcClickSmartDirectCombatAuthorization authorizeDirectCombat(NpcClickSmartCloudRequest request) {
        if (!coordinator.isActive(CloudDecisionServiceId.NPC_CLICK_SMART)) {
            return NpcClickSmartDirectCombatAuthorization.refused("DISABLED", "service disabled", null);
        }
        if (request == null
                || request.getNpcRequest() == null
                || !hasText(request.getSessionId())
                || !hasText(request.getWindowId())
                || !hasText(request.getTaskRunId())
                || !hasText(request.getNpcRequest().npcName())) {
            return NpcClickSmartDirectCombatAuthorization.refused("REQUIRED_FAILURE",
                    "missing direct-combat authorize identity or npc facts", null);
        }
        Map<String, String> context = mutableContext(request);
        context.put("queueOperation", DIRECT_COMBAT_AUTHORIZE);
        context.put("messageType", DIRECT_COMBAT_AUTHORIZE);
        CloudDecisionRequest cloudRequest = cloudRequest(request, "npc-click-direct-combat-authorize", context,
                "action=DIRECT_COMBAT_AUTHORIZE;sessionId=" + safe(request.getSessionId()));
        CloudDecisionResult result = coordinator.shadow(cloudRequest, cloudRequest.getLocalDecision(),
                acceptAnyResponseGate());
        if (!result.isExecuted() || result.getResponse() == null) {
            return NpcClickSmartDirectCombatAuthorization.refused("REQUIRED_FAILURE", result.getReason(), null);
        }
        Map<String, String> fields = fields(result.getResponse().getDecision());
        String action = normalizedUpper(fields.get("action"));
        String status = normalizedUpper(fields.get("status"));
        boolean authorized = "ENTER_DIRECT_COMBAT".equals(action) && "AUTHORIZED".equals(status);
        log.info("NPC direct-combat cloud authorization answer: authorized={} npc={} task={} windowId={} status={} reason={} decisionId={}",
                authorized, request.getNpcRequest().npcName(), request.getTaskCode(), request.getWindowId(),
                status, fields.get("reason"), fields.get("decisionId"));
        return NpcClickSmartDirectCombatAuthorization.builder()
                .authorized(authorized)
                .status(status)
                .reason(firstText(fields.get("reason"), result.getReason()))
                .decisionId(normalize(fields.get("decisionId"), null))
                .build();
    }

    /**
     * Ask cloud for the next executable ordinary NPC click action.
     *
     * @param request raw screenshot payload and task target facts.
     * @return cloud click/no-action/disabled/failure envelope. Required failure never authorizes
     *         local fallback.
     */
    public NpcClickSmartCloudDecision decide(NpcClickSmartCloudRequest request) {
        if (!coordinator.isActive(CloudDecisionServiceId.NPC_CLICK_SMART)) {
            return NpcClickSmartCloudDecision.builder()
                    .status(NpcClickSmartCloudDecision.Status.DISABLED)
                    .action(NpcClickSmartCloudDecision.Action.ABORT)
                    .reason("service disabled")
                    .build();
        }
        String validationError = requestValidationError(request);
        if (validationError != null) {
            return requiredFailure(null, validationError);
        }

        NpcClickRequest npcRequest = request.getNpcRequest();
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.NPC_CLICK_SMART)
                .traceId(traceId(request))
                .taskCode(normalize(request.getTaskCode(), taskCode(npcRequest)))
                .phase(normalize(request.getPhase(), PHASE))
                .windowId(request.getWindowId())
                .taskRunId(request.getTaskRunId())
                .policyVersion(request.getPolicyVersion())
                .localDecision(LOCAL_DECISION)
                .context(context(request))
                .build();

        ParseResult[] parsedHolder = new ParseResult[1];
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                LOCAL_DECISION,
                executionGate(request, parsedHolder));
        if (cloudResult.isExecuted()) {
            ParseResult parsed = parsedHolder[0];
            Map<String, String> fields = fields(cloudResult.getResponse().getDecision());
            if (parsed.action().requiresClickPoint()) {
                log.info("cloud.execute serviceId=NPC_CLICK_SMART accepted=true task={} npc={} click=({}, {}) decisionId={} elapsedMs={}",
                        cloudRequest.getTaskCode(), npcRequest.npcName(), parsed.click().x, parsed.click().y,
                        fields.get("decisionId"), cloudResult.getElapsedMs());
                return NpcClickSmartCloudDecision.builder()
                        .status(NpcClickSmartCloudDecision.Status.CLOUD_EXECUTED)
                        .action(parsed.action())
                        .windowRelativeClickPoint(parsed.click())
                        .actionId(normalize(fields.get("actionId"), npcRequest.npcName()))
                        .decisionId(normalize(fields.get("decisionId"), null))
                        .reason(firstText(fields.get("reason"), cloudResult.getReason()))
                        .debugToken(diagnostic(cloudResult.getResponse(), "debugToken"))
                        .candidateBox(parsed.candidateBox().toDecisionText())
                        .hotkey(normalize(fields.get("hotkey"), null))
                        .attemptToken(normalize(fields.get("attemptToken"), null))
                        .ctrl(parsed.action() == NpcClickSmartCloudDecision.Action.CTRL_CLICK
                                || Boolean.parseBoolean(fields.getOrDefault("ctrl", "false")))
                        .alt(Boolean.parseBoolean(fields.getOrDefault("alt", "false")))
                        .confidence(cloudResult.getResponse().getConfidence())
                        .cloudResult(cloudResult)
                        .build();
            }
            log.info("cloud.execute serviceId=NPC_CLICK_SMART accepted=true no-click task={} npc={} action={} reason={} elapsedMs={}",
                    cloudRequest.getTaskCode(), npcRequest.npcName(), parsed.action(),
                    firstText(fields.get("reason"), cloudResult.getReason()), cloudResult.getElapsedMs());
            String candidateBox = firstText(diagnostic(cloudResult.getResponse(), "candidateBox"), fields.get("candidateBox"));
            return NpcClickSmartCloudDecision.builder()
                    .status(NpcClickSmartCloudDecision.Status.CLOUD_NO_ACTION)
                    .action(parsed.action())
                    .decisionId(normalize(fields.get("decisionId"), null))
                    .reason(firstText(fields.get("reason"), cloudResult.getReason()))
                    .debugToken(diagnostic(cloudResult.getResponse(), "debugToken"))
                    .candidateBox(normalize(candidateBox, null))
                    .attemptToken(normalize(fields.get("attemptToken"), null))
                    .confidence(cloudResult.getResponse().getConfidence())
                    .cloudResult(cloudResult)
                    .build();
        }

        if (cloudResult.getMode() == CloudDecisionMode.DISABLED || cloudResult.getMode() == CloudDecisionMode.SHADOW) {
            return NpcClickSmartCloudDecision.builder()
                    .status(NpcClickSmartCloudDecision.Status.DISABLED)
                    .action(NpcClickSmartCloudDecision.Action.ABORT)
                    .reason(cloudResult.getReason())
                    .cloudResult(cloudResult)
                    .build();
        }

        log.warn("cloud.execute serviceId=NPC_CLICK_SMART accepted=false task={} npc={} reason={} cloudDecision={}",
                cloudRequest.getTaskCode(),
                npcRequest.npcName(),
                cloudResult.getReason(),
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision());
        return requiredFailure(cloudResult, cloudResult.getReason());
    }

    private CloudDecisionRequest cloudRequest(
            NpcClickSmartCloudRequest request,
            String phase,
            Map<String, String> context,
            String localDecision) {
        NpcClickRequest npcRequest = request.getNpcRequest();
        return CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.NPC_CLICK_SMART)
                .traceId(traceId(request) + ":" + phase)
                .taskCode(normalize(request.getTaskCode(), taskCode(npcRequest)))
                .phase(phase)
                .windowId(request.getWindowId())
                .taskRunId(request.getTaskRunId())
                .policyVersion(request.getPolicyVersion())
                .localDecision(localDecision)
                .context(Map.copyOf(context))
                .build();
    }

    private static CloudDecisionExecutionGate acceptAnyResponseGate() {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.NPC_CLICK_SMART;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(), "execute percent gate hit; accepting NPC click queue response");
            }
        };
    }

    private CloudDecisionExecutionGate queueMessageGate(
            NpcClickSmartCloudRequest smartRequest,
            NpcClickSmartQueueMessage[] parsedHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.NPC_CLICK_SMART;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                NpcClickSmartQueueMessage parsed = parseQueueMessage(smartRequest, response);
                if (parsed.getType() == NpcClickSmartQueueMessage.Type.INVALID) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.getReason());
                }
                parsedHolder[0] = parsed;
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(), "execute percent gate hit; consuming NPC click FIFO queue message");
            }
        };
    }

    private static NpcClickSmartQueueMessage parseQueueMessage(
            NpcClickSmartCloudRequest request,
            CloudDecisionResponse response) {
        Map<String, String> fields = fields(response.getDecision());
        NpcClickSmartQueueMessage.Type type = parseMessageType(firstText(
                fields.get("messageType"),
                firstText(fields.get("type"), fields.get("strategy"))));
        if (type == NpcClickSmartQueueMessage.Type.INVALID) {
            return invalidQueueMessage("unsupported or missing queue message type: " + response.getDecision());
        }
        if (containsLegacyQueueAction(response.getDecision())) {
            return invalidQueueMessage("legacy action is not allowed inside NPC click FIFO queue: "
                    + legacyQueueAction(response.getDecision()));
        }
        String sessionId = normalize(fields.get("sessionId"), null);
        String windowId = normalize(fields.get("windowId"), null);
        String taskRunId = normalize(fields.get("taskRunId"), null);
        if (!hasText(sessionId) || !hasText(windowId) || !hasText(taskRunId)) {
            return invalidQueueMessage("queue message missing required identity fields");
        }
        Point click = parseClick(fields.get("click"));
        String candidateBoxText = firstText(diagnostic(response, "candidateBox"), fields.get("candidateBox"));
        Box candidateBox = parseBox(candidateBoxText);
        if (candidateBox != null) {
            if (!insideWindow(candidateBox, request.getWindowWidth(), request.getWindowHeight())) {
                return invalidQueueMessage("ordinary NPC queue candidateBox outside window");
            }
            if (!insideAllowedRegion(candidateBox, request)) {
                return invalidQueueMessage("ordinary NPC queue candidateBox outside scan regions");
            }
        }
        if (type == NpcClickSmartQueueMessage.Type.END) {
            return queueMessageBuilder(request, fields, type, response)
                    .reason(firstText(fields.get("reason"), "cloud queue ended"))
                    .build();
        }
        if (type == NpcClickSmartQueueMessage.Type.MEMORY && click != null) {
            if (!insideWindow(click, request.getWindowWidth(), request.getWindowHeight())) {
                return invalidQueueMessage("MEMORY queue candidate click outside window");
            }
            if (!insideAllowedRegion(click, request)) {
                return invalidQueueMessage("MEMORY queue candidate click outside scan regions");
            }
            if (candidateBox == null) {
                return invalidQueueMessage("MEMORY queue candidate missing candidateBox");
            }
        }
        if (isOrdinaryClickCandidate(type)) {
            if (click == null) {
                return queueMessageBuilder(request, fields, type, response)
                        .candidateBox(candidateBoxText)
                        .reason(firstText(fields.get("reason"), "ordinary NPC queue candidate not found"))
                        .build();
            }
            if (!insideWindow(click, request.getWindowWidth(), request.getWindowHeight())) {
                return invalidQueueMessage("ordinary NPC queue candidate click outside window");
            }
            if (!insideAllowedRegion(click, request)) {
                return invalidQueueMessage("ordinary NPC queue candidate click outside scan regions");
            }
            if (candidateBox == null) {
                return invalidQueueMessage("ordinary NPC queue candidate missing candidateBox");
            }
        }
        List<Point> ctrlPoints = type == NpcClickSmartQueueMessage.Type.CTRL_CANDIDATES
                ? parsePoints(firstText(fields.get("ctrlProbePoints"), fields.get("clicks")))
                : List.of();
        if (type == NpcClickSmartQueueMessage.Type.CTRL_CANDIDATES && ctrlPoints.isEmpty()) {
            Point single = parseClick(fields.get("click"));
            ctrlPoints = single == null ? List.of() : List.of(single);
        }
        if (type == NpcClickSmartQueueMessage.Type.CTRL_CANDIDATES && ctrlPoints.isEmpty()) {
            return invalidQueueMessage("CTRL_CANDIDATES queue message has no probe points");
        }
        return queueMessageBuilder(request, fields, type, response)
                .windowRelativeClickPoint(click)
                .candidateBox(candidateBoxText)
                .ctrlProbePoints(ctrlPoints)
                .reason(firstText(fields.get("reason"), "cloud queue candidate"))
                .build();
    }

    private static NpcClickSmartQueueMessage.NpcClickSmartQueueMessageBuilder queueMessageBuilder(
            NpcClickSmartCloudRequest request,
            Map<String, String> fields,
            NpcClickSmartQueueMessage.Type type,
            CloudDecisionResponse response) {
        return NpcClickSmartQueueMessage.builder()
                .type(type)
                .sessionId(fields.get("sessionId"))
                .windowId(fields.get("windowId"))
                .taskRunId(fields.get("taskRunId"))
                .decisionId(normalize(fields.get("decisionId"), "cloud-npc-queue"))
                .strategy(firstText(fields.get("strategy"), type.name()))
                .matchedText(firstText(diagnostic(response, "matchedText"), fields.get("matchedText")))
                .confidence(response.getConfidence());
    }

    private static NpcClickSmartQueueMessage invalidQueueMessage(String reason) {
        return NpcClickSmartQueueMessage.builder()
                .type(NpcClickSmartQueueMessage.Type.INVALID)
                .reason(reason)
                .build();
    }

    private static boolean isOrdinaryClickCandidate(NpcClickSmartQueueMessage.Type type) {
        return type == NpcClickSmartQueueMessage.Type.TOOLTIP
                || type == NpcClickSmartQueueMessage.Type.YELLOW_NAME
                || type == NpcClickSmartQueueMessage.Type.PURPLE_FORMULA;
    }

    private static boolean containsLegacyQueueAction(String decision) {
        return legacyQueueAction(decision) != null;
    }

    private static String legacyQueueAction(String decision) {
        if (!hasText(decision)) {
            return null;
        }
        if (decision.contains("REQUEST_NEW_SCREENSHOT")) {
            return "REQUEST_NEW_SCREENSHOT";
        }
        if (decision.contains("VERIFY_DIALOG")) {
            return "VERIFY_DIALOG";
        }
        if (decision.contains("CTRL_HOVER_DONE")) {
            return "CTRL_HOVER_DONE";
        }
        if (decision.contains("action=CTRL_HOVER")) {
            return "CTRL_HOVER";
        }
        return null;
    }

    private static NpcClickSmartQueueMessage.Type parseMessageType(String value) {
        String normalized = normalizedUpper(value);
        return switch (normalized) {
            case "MEMORY", "LEARNED_MEMORY" -> NpcClickSmartQueueMessage.Type.MEMORY;
            case "TOOLTIP", "TOOLTIP_TEMPLATE" -> NpcClickSmartQueueMessage.Type.TOOLTIP;
            case "YELLOW_NAME", "YELLOW_TARGET_RAW" -> NpcClickSmartQueueMessage.Type.YELLOW_NAME;
            case "PURPLE_FORMULA", "PLAYER_ANCHOR_FORMULA" -> NpcClickSmartQueueMessage.Type.PURPLE_FORMULA;
            case "CTRL_CANDIDATES", "CTRL_MENU", "CTRL_MENU_HOVER" -> NpcClickSmartQueueMessage.Type.CTRL_CANDIDATES;
            case "WAIT" -> NpcClickSmartQueueMessage.Type.WAIT;
            case "END" -> NpcClickSmartQueueMessage.Type.END;
            default -> NpcClickSmartQueueMessage.Type.INVALID;
        };
    }

    private static List<Point> parsePoints(String value) {
        if (!hasText(value)) {
            return List.of();
        }
        return List.of(value.split("\\|", -1)).stream()
                .map(NpcClickSmartCloudDecisionService::parseClick)
                .filter(point -> point != null)
                .toList();
    }

    /**
     * Report one locally verified cloud NPC click outcome back to cloud memory.
     *
     * @param request original screenshot request and task/window metadata; nullable returns false.
     * @param decision cloud action that local input executed and verified; nullable returns false.
     * @param reason local verifier reason, for example {@code local verifier accepted cloud action}.
     * @return true only when the HTTP outcome endpoint accepts the report. Transport/config/http
     *         failures are logged and return false so they cannot change the verified click result.
     */
    public boolean reportVerifiedOutcome(
            NpcClickSmartCloudRequest request,
            NpcClickSmartCloudDecision decision,
            String reason) {
        return reportVerifiedOutcome(request, decision, "UNSPECIFIED", "", "", reason);
    }

    /**
     * Report one locally verified cloud NPC click outcome with verifier strength evidence.
     *
     * @param request original screenshot request and task/window metadata; nullable returns false.
     * @param decision cloud action that local input executed and verified; nullable returns false.
     * @param verificationStrength strength of the local verifier, for example {@code DIALOG_TEMPLATE}.
     * @param verificationActionKey semantic action key matched by the verifier; nullable is allowed.
     * @param verificationMatchedText template path or OCR text matched by the verifier; nullable is allowed.
     * @param reason local verifier reason, for example {@code local verifier accepted cloud action}.
     * @return true only when the HTTP outcome endpoint accepts the report. Transport/config/http
     *         failures are logged and return false so they cannot change the verified click result.
     */
    public boolean reportVerifiedOutcome(
            NpcClickSmartCloudRequest request,
            NpcClickSmartCloudDecision decision,
            String verificationStrength,
            String verificationActionKey,
            String verificationMatchedText,
            String reason) {
        String disabledReason = outcomeTransportDisabledReason();
        if (disabledReason != null) {
            log.warn("NPC_CLICK_SMART verified outcome not submitted: reason={} decisionId={} npc={}",
                    disabledReason,
                    decision == null ? null : decision.getDecisionId(),
                    request == null || request.getNpcRequest() == null ? null : request.getNpcRequest().npcName());
            return false;
        }
        if (request == null || request.getNpcRequest() == null || decision == null) {
            log.warn("NPC_CLICK_SMART verified outcome not submitted: reason=missing request or decision");
            return false;
        }

        String body;
        try {
            body = serializeVerifiedOutcome(
                    request,
                    decision,
                    verificationStrength,
                    verificationActionKey,
                    verificationMatchedText,
                    reason);
        } catch (JsonProcessingException e) {
            log.warn("NPC_CLICK_SMART verified outcome not submitted: reason=json serialize failure: {} decisionId={} npc={}",
                    e.getOriginalMessage(), decision.getDecisionId(), request.getNpcRequest().npcName(), e);
            return false;
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(outcomeEndpointUri())
                .timeout(Duration.ofMillis(Math.max(1L, properties.getTimeoutMs())))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + properties.getToken().trim())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("NPC_CLICK_SMART verified outcome not submitted: reason=http status={} decisionId={} npc={}",
                        response.statusCode(), decision.getDecisionId(), request.getNpcRequest().npcName());
                return false;
            }
            Point click = decision.getWindowRelativeClickPoint();
            log.info("NPC_CLICK_SMART verified outcome submitted: decisionId={} attemptToken={} task={} windowId={} "
                            + "npc={} map={} target=({}, {}) action={} click=({}, {}) reason={}",
                    decision.getDecisionId(), decision.getAttemptToken(), request.getTaskCode(), request.getWindowId(),
                    request.getNpcRequest().npcName(), request.getNpcRequest().mapName(),
                    request.getNpcRequest().mapX(), request.getNpcRequest().mapY(),
                    decision.getAction(), click == null ? null : click.x, click == null ? null : click.y, reason);
            return true;
        } catch (HttpTimeoutException e) {
            log.warn("NPC_CLICK_SMART verified outcome not submitted: reason=timeout after {}ms decisionId={} npc={}",
                    properties.getTimeoutMs(), decision.getDecisionId(), request.getNpcRequest().npcName(), e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("NPC_CLICK_SMART verified outcome not submitted: reason=interrupted decisionId={} npc={}",
                    decision.getDecisionId(), request.getNpcRequest().npcName(), e);
            return false;
        } catch (IOException e) {
            log.warn("NPC_CLICK_SMART verified outcome not submitted: reason=http failure {} decisionId={} npc={}",
                    e.getClass().getSimpleName(), decision.getDecisionId(), request.getNpcRequest().npcName(), e);
            return false;
        }
    }

    private CloudDecisionExecutionGate executionGate(
            NpcClickSmartCloudRequest smartRequest,
            ParseResult[] parsedHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.NPC_CLICK_SMART;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                if (response.getConfidence() < MIN_EXECUTE_CONFIDENCE) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "low confidence: " + response.getConfidence());
                }
                ParseResult parsed = parse(smartRequest, response);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                parsedHolder[0] = parsed;
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(),
                        parsed.action() == NpcClickSmartCloudDecision.Action.CLICK
                                ? "execute percent gate hit; using NPC_CLICK_SMART WINDOW_RELATIVE click"
                                : "execute percent gate hit; using NPC_CLICK_SMART allowlisted action");
            }
        };
    }

    private static ParseResult parse(NpcClickSmartCloudRequest request, CloudDecisionResponse response) {
        Map<String, String> fields = fields(response.getDecision());
        String actionText = normalizedUpper(fields.get("action"));
        if (!hasText(actionText)) {
            return ParseResult.rejected("action is required");
        }
        NpcClickSmartCloudDecision.Action action;
        try {
            action = NpcClickSmartCloudDecision.Action.valueOf(actionText);
        } catch (IllegalArgumentException e) {
            return ParseResult.rejected("unsupported action: " + actionText);
        }
        if (!action.requiresClickPoint()) {
            return ParseResult.acceptedNoClick(action);
        }
        if (Boolean.parseBoolean(fields.getOrDefault("alt", "false"))) {
            return ParseResult.rejected("unsupported modifier for NPC_CLICK_SMART action: alt");
        }
        boolean ctrl = Boolean.parseBoolean(fields.getOrDefault("ctrl", "false"));
        if (ctrl && action != NpcClickSmartCloudDecision.Action.CTRL_CLICK) {
            return ParseResult.rejected("unsupported modifier for NPC_CLICK_SMART action: ctrl");
        }
        if (action == NpcClickSmartCloudDecision.Action.PRESS_HOTKEY_THEN_CLICK
                && !isAllowedHotkey(fields.get("hotkey"))) {
            return ParseResult.rejected("unsupported hotkey for NPC_CLICK_SMART action: " + safe(fields.get("hotkey")));
        }
        String coordinateSpace = diagnostic(response, COORDINATE_SPACE_KEY);
        if (!WINDOW_RELATIVE.equals(coordinateSpace)) {
            return ParseResult.rejected(
                    "diagnostics.coordinateSpace must be WINDOW_RELATIVE when action requires a click point");
        }
        Point click = parseClick(fields.get("click"));
        if (click == null) {
            return ParseResult.rejected("click must parse as click=<windowX>,<windowY>");
        }
        if (!insideWindow(click, request.getWindowWidth(), request.getWindowHeight())) {
            return ParseResult.rejected("WINDOW_RELATIVE click outside window: click="
                    + click.x + "," + click.y + " window=" + request.getWindowWidth() + "x" + request.getWindowHeight());
        }
        if (!insideAllowedRegion(click, request)) {
            return ParseResult.rejected("WINDOW_RELATIVE click outside NPC scan regions: click="
                    + click.x + "," + click.y + " roi=" + roiText(request.getRoi())
                    + " scanRegions=" + scanRegionsText(request.getScanRegions()));
        }
        Box candidateBox = parseBox(firstText(diagnostic(response, "candidateBox"), fields.get("candidateBox")));
        if (candidateBox == null) {
            return ParseResult.rejected("candidateBox is required when action requires a click point");
        }
        if (!insideWindow(candidateBox, request.getWindowWidth(), request.getWindowHeight())) {
            return ParseResult.rejected("candidateBox outside window: candidateBox=" + candidateBox.toDecisionText());
        }
        if (!insideAllowedRegion(candidateBox, request)) {
            return ParseResult.rejected("candidateBox outside NPC scan regions: candidateBox="
                    + candidateBox.toDecisionText() + " roi=" + roiText(request.getRoi())
                    + " scanRegions=" + scanRegionsText(request.getScanRegions()));
        }
        return ParseResult.acceptedClick(action, click, candidateBox);
    }

    private static Map<String, String> context(NpcClickSmartCloudRequest request) {
        NpcClickRequest npc = request.getNpcRequest();
        Map<String, String> context = new LinkedHashMap<>();
        context.put("hook", PHASE);
        context.put("sessionId", safe(request.getSessionId()));
        context.put("windowId", safe(request.getWindowId()));
        context.put("taskRunId", safe(request.getTaskRunId()));
        context.put("source", safe(request.getSource()));
        context.put("sourceTask", taskCode(npc));
        context.put("verificationMode", safe(request.getVerificationMode()));
        context.put("attemptIndex", Integer.toString(request.getAttemptIndex()));
        context.put("attemptToken", safe(request.getAttemptToken()));
        context.put("lastOutcomeStatus", safe(request.getLastOutcomeStatus()));
        context.put("lastOutcomeReason", safe(request.getLastOutcomeReason()));
        context.put("lastAction", safe(request.getLastAction()));
        context.put("lastClick", safe(request.getLastClick()));
        context.put("lastCandidateBox", safe(request.getLastCandidateBox()));
        context.put("npcName", safe(npc.npcName()));
        context.put("mapName", safe(npc.mapName()));
        context.put("target", npc.mapX() + "," + npc.mapY());
        context.put("playerName", firstText(request.getPlayerName(), npc.player() == null ? "" : npc.player().getName()));
        context.put("playerMapName", firstText(request.getPlayerMapName(),
                npc.player() == null ? "" : npc.player().getCurrentMapName()));
        context.put("playerMapX", integerText(request.getPlayerMapX(), npc.player() == null ? null : npc.player().getX()));
        context.put("playerMapY", integerText(request.getPlayerMapY(), npc.player() == null ? null : npc.player().getY()));
        context.put("tuneX", integerText(request.getTuneX(), npc.tuneX()));
        context.put("tuneY", integerText(request.getTuneY(), npc.tuneY()));
        context.put("targetRole", enumName(npc.targetRole()));
        context.put("targetEvidence", enumName(npc.targetEvidence()));
        context.put("roamingTarget", Boolean.toString(npc.roamingTarget()));
        context.put("tooltipFirst", booleanText(request.getTooltipFirst(), npc.tooltipFirst()));
        context.put("closeStoryBeforeDirectSceneClick", booleanText(
                request.getCloseStoryBeforeDirectSceneClick(),
                npc.closeStoryBeforeDirectSceneClick()));
        // CR267: direct-combat facts. directCombatMode marks the post-Alt+A second session; the
        // three fact fields feed the cloud DIRECT_COMBAT_AUTHORIZE decision only.
        context.put("directCombatMode",
                Boolean.toString("direct-combat".equalsIgnoreCase(safe(request.getVerificationMode()))));
        context.put("directCombatProbeTargetReady", Boolean.toString(npc.directCombatProbeTargetReady()));
        context.put("directCombatNormalFifoUnverified", Boolean.toString(npc.directCombatNormalFifoUnverified()));
        context.put("directCombatArrivalTolerance", Integer.toString(npc.directCombatArrivalTolerance()));
        context.put("directCombatScenario", enumName(npc.directCombatScenario()));
        context.put("expectedDialogTemplatePath", safe(npc.expectedDialogTemplatePath()));
        context.put("expectedDialogRawTemplatePath", safe(npc.expectedDialogRawTemplatePath()));
        context.put("expectedDialogTemplatePaths", join(npc.expectedDialogTemplatePaths()));
        context.put("tooltipTemplatePath", safe(npc.tooltipTemplatePath()));
        context.put("tooltipType", enumName(npc.tooltipType()));
        context.put("templateSpecs", join(request.getTemplateSpecs()));
        context.put("targetTemplateSpecs", join(request.getTargetTemplateSpecs()));
        context.put("yellowTemplateSpecs", join(request.getYellowTemplateSpecs()));
        context.put("targetGlyphTemplate", safe(request.getTargetGlyphTemplate()));
        context.put("glyphMetadata", request.getGlyphMetadata() == null ? "" : request.getGlyphMetadata().toString());
        context.put("imagePayloadBase64", safe(request.getImagePayloadBase64()));
        context.put("payloadMimeType", safe(request.getPayloadMimeType()));
        context.put("imageSha256", safe(request.getImageSha256()));
        context.put("rawImagePath", safe(request.getRawImagePath()));
        context.put("debugImageId", safe(request.getDebugImageId()));
        context.put("roi", roiText(request.getRoi()));
        context.put("scanRegions", scanRegionsText(request.getScanRegions()));
        context.put("scanRegionCount", Integer.toString(request.getScanRegions() == null
                ? 0
                : request.getScanRegions().size()));
        context.put("windowWidth", Integer.toString(request.getWindowWidth()));
        context.put("windowHeight", Integer.toString(request.getWindowHeight()));
        context.put("windowSize", request.getWindowWidth() + "," + request.getWindowHeight());
        context.put("coordinateNotes", "target is logical map coordinate x,y; click responses are WINDOW_RELATIVE pixels");
        context.put("hwnd", safe(request.getHwnd()));
        return Map.copyOf(context);
    }

    private static Map<String, String> mutableContext(NpcClickSmartCloudRequest request) {
        return new LinkedHashMap<>(context(request));
    }

    private static String requestValidationError(NpcClickSmartCloudRequest request) {
        if (request == null) {
            return "missing NPC_CLICK_SMART cloud request";
        }
        if (request.getNpcRequest() == null) {
            return "npcRequest is required";
        }
        if (!hasText(request.getSessionId())
                || !hasText(request.getWindowId())
                || !hasText(request.getTaskRunId())) {
            return "sessionId/windowId/taskRunId are required for NPC_CLICK_SMART queue";
        }
        if (!hasText(request.getNpcRequest().npcName())) {
            return "npcName is required";
        }
        if (!hasText(request.getImagePayloadBase64())
                || !hasText(request.getPayloadMimeType())
                || !hasText(request.getImageSha256())) {
            return "missing transferable image payload metadata";
        }
        if (request.getWindowWidth() <= 0 || request.getWindowHeight() <= 0) {
            return "window size must be explicit positive pixels";
        }
        NpcClickSmartCloudRequest.Roi roi = request.getRoi();
        if (roi == null) {
            return "ROI is required for NPC_CLICK_SMART";
        }
        if (roi.getX() < 0 || roi.getY() < 0 || roi.getWidth() <= 0 || roi.getHeight() <= 0) {
            return "ROI must be window-relative pixels with non-negative origin and positive size";
        }
        if ((long) roi.getX() + roi.getWidth() > request.getWindowWidth()
                || (long) roi.getY() + roi.getHeight() > request.getWindowHeight()) {
            return "ROI outside window: roi=" + roiText(roi)
                    + " window=0,0," + request.getWindowWidth() + "," + request.getWindowHeight();
        }
        if (request.getScanRegions() != null) {
            for (NpcClickSmartCloudRequest.ScanRegion region : request.getScanRegions()) {
                if (region == null) {
                    return "scanRegions cannot contain null entries";
                }
                if (region.getWindowX() < 0 || region.getWindowY() < 0
                        || region.getWidth() <= 0 || region.getHeight() <= 0
                        || (long) region.getWindowX() + region.getWidth() > request.getWindowWidth()
                        || (long) region.getWindowY() + region.getHeight() > request.getWindowHeight()) {
                    return "scanRegion outside window: " + scanRegionText(region)
                            + " window=0,0," + request.getWindowWidth() + "," + request.getWindowHeight();
                }
            }
        }
        return null;
    }

    private static boolean isAllowedHotkey(String hotkey) {
        if (!hasText(hotkey)) {
            return false;
        }
        for (String part : hotkey.split(",", -1)) {
            String normalized = normalizedUpper(part);
            boolean allowed = "ALT_4".equals(normalized)
                    || "ALT_C".equals(normalized)
                    || "ALT_A".equals(normalized);
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    private static NpcClickSmartCloudDecision requiredFailure(CloudDecisionResult cloudResult, String reason) {
        return NpcClickSmartCloudDecision.builder()
                .status(NpcClickSmartCloudDecision.Status.REQUIRED_FAILURE)
                .action(NpcClickSmartCloudDecision.Action.ABORT)
                .reason(safe(reason))
                .cloudResult(cloudResult)
                .build();
    }

    private static String traceId(NpcClickSmartCloudRequest request) {
        NpcClickRequest npc = request.getNpcRequest();
        return "npc-click-smart:"
                + safeTracePart(taskCode(npc))
                + ":" + safeTracePart(request.getVerificationMode())
                + ":" + safeTracePart(request.getWindowId())
                + ":" + safeTracePart(npc.npcName())
                + ":" + safeTracePart(request.getDebugImageId());
    }

    private String serializeVerifiedOutcome(
            NpcClickSmartCloudRequest request,
            NpcClickSmartCloudDecision decision,
            String verificationStrength,
            String verificationActionKey,
            String verificationMatchedText,
            String reason) throws JsonProcessingException {
        NpcClickRequest npc = request.getNpcRequest();
        ObjectNode root = objectMapper.createObjectNode();
        put(root, "decisionId", decision.getDecisionId());
        put(root, "attemptToken", decision.getAttemptToken());
        put(root, "mapName", npc.mapName());
        put(root, "npcName", npc.npcName());
        put(root, "target", npc.mapX() + "," + npc.mapY());
        put(root, "verificationMode", request.getVerificationMode());
        put(root, "action", decision.getAction() == null ? "" : decision.getAction().name());
        put(root, "actionId", decision.getActionId());
        put(root, "strategy", decision.getActionId());
        Point click = decision.getWindowRelativeClickPoint();
        ObjectNode clickNode = objectMapper.createObjectNode();
        if (click != null) {
            clickNode.put("x", click.x);
            clickNode.put("y", click.y);
        }
        root.set("click", clickNode);
        put(root, "candidateBox", decision.getCandidateBox());
        put(root, "result", "VERIFIED");
        put(root, "targetEvidence", enumName(npc.targetEvidence()));
        put(root, "verificationStrength", verificationStrength);
        put(root, "verificationActionKey", verificationActionKey);
        put(root, "verificationMatchedText", verificationMatchedText);
        put(root, "reason", firstText(reason, decision.getReason()));
        put(root, "taskCode", normalize(request.getTaskCode(), taskCode(npc)));
        put(root, "windowId", request.getWindowId());
        put(root, "taskRunId", request.getTaskRunId());
        put(root, "imageSha256", request.getImageSha256());
        put(root, "debugImageId", request.getDebugImageId());
        appendOldNpcClickMemoryFields(root, request, click, "VERIFIED",
                firstText(decision.getActionId(), "CLOUD_ACTION"), verificationMatchedText);
        return objectMapper.writeValueAsString(root);
    }

    private String serializeQueueOutcome(
            NpcClickSmartCloudRequest request,
            NpcClickSmartQueueMessage message,
            NpcClickSmartQueueOutcome outcome,
            String verificationStrength,
            String verificationActionKey,
            String verificationMatchedText,
            String reason) throws JsonProcessingException {
        NpcClickRequest npc = request.getNpcRequest();
        ObjectNode root = objectMapper.createObjectNode();
        put(root, "sessionId", message.getSessionId());
        put(root, "decisionId", message.getDecisionId());
        put(root, "messageType", message.getType().name());
        put(root, "strategy", firstText(message.getStrategy(), message.getType().name()));
        put(root, "mapName", npc.mapName());
        put(root, "npcName", npc.npcName());
        put(root, "target", npc.mapX() + "," + npc.mapY());
        put(root, "verificationMode", request.getVerificationMode());
        Point click = message.getWindowRelativeClickPoint();
        ObjectNode clickNode = objectMapper.createObjectNode();
        if (click != null) {
            clickNode.put("x", click.x);
            clickNode.put("y", click.y);
        }
        root.set("click", clickNode);
        put(root, "candidateBox", message.getCandidateBox());
        put(root, "result", outcome.name());
        put(root, "targetEvidence", enumName(npc.targetEvidence()));
        put(root, "verificationStrength", verificationStrength);
        put(root, "verificationActionKey", verificationActionKey);
        put(root, "verificationMatchedText", verificationMatchedText);
        put(root, "localVerificationReason", reason);
        put(root, "reason", firstText(message.getReason(), reason));
        put(root, "taskCode", normalize(request.getTaskCode(), taskCode(npc)));
        put(root, "windowId", request.getWindowId());
        put(root, "taskRunId", request.getTaskRunId());
        put(root, "imageSha256", request.getImageSha256());
        put(root, "debugImageId", request.getDebugImageId());
        String candidateMatchedText = firstText(message.getMatchedText(), verificationMatchedText);
        appendOldNpcClickMemoryFields(root, request, click, outcome.name(),
                firstText(message.getStrategy(), message.getType().name()), candidateMatchedText);
        return objectMapper.writeValueAsString(root);
    }

    private void appendOldNpcClickMemoryFields(ObjectNode root,
                                               NpcClickSmartCloudRequest request,
                                               Point click,
                                               String result,
                                               String actualClickSource,
                                               String matchedText) {
        NpcClickRequest npc = request.getNpcRequest();
        put(root, "source", request.getSource());
        put(root, "playerName", request.getPlayerName());
        put(root, "playerMapName", firstText(request.getPlayerMapName(),
                npc.player() == null ? "" : npc.player().getCurrentMapName()));
        putInteger(root, "playerMapX", request.getPlayerMapX(), npc.player() == null ? null : npc.player().getX());
        putInteger(root, "playerMapY", request.getPlayerMapY(), npc.player() == null ? null : npc.player().getY());
        root.put("targetMapX", npc.mapX());
        root.put("targetMapY", npc.mapY());
        putInteger(root, "tuneX", request.getTuneX(), npc.tuneX());
        putInteger(root, "tuneY", request.getTuneY(), npc.tuneY());
        root.put("windowWidth", request.getWindowWidth());
        root.put("windowHeight", request.getWindowHeight());
        put(root, "hwnd", request.getHwnd());
        put(root, "actualClickSource", actualClickSource);
        put(root, "formulaVersion", "cloud-npc-click-smart-fifo");
        put(root, "matchedText", matchedText);
        put(root, "outcome", result);
        root.put("clicked", click != null);
        boolean verified = "VERIFIED".equalsIgnoreCase(result);
        root.put("success", verified);
        root.put("actualClickMeasured", verified && click != null);
        if (click != null) {
            setPoint(root, "actualClickRel", click.x, click.y);
            setPoint(root, "predictedClickRel", click.x, click.y);
        }
        Point windowBase = firstWindowBase(request);
        if (windowBase != null) {
            setPoint(root, "windowBase", windowBase.x, windowBase.y);
            if (click != null) {
                setPoint(root, "actualClickAbs", windowBase.x + click.x, windowBase.y + click.y);
                setPoint(root, "predictedClickAbs", windowBase.x + click.x, windowBase.y + click.y);
            }
        }
    }

    private boolean postOutcome(
            String body,
            String npcName,
            String decisionId,
            String sessionId,
            String outcome) {
        HttpRequest httpRequest = HttpRequest.newBuilder(outcomeEndpointUri())
                .timeout(Duration.ofMillis(Math.max(1L, properties.getTimeoutMs())))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + properties.getToken().trim())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("NPC_CLICK_SMART outcome not submitted: reason=http status={} sessionId={} decisionId={} npc={} outcome={}",
                        response.statusCode(), sessionId, decisionId, npcName, outcome);
                return false;
            }
            log.info("NPC_CLICK_SMART outcome submitted: sessionId={} decisionId={} npc={} outcome={}",
                    sessionId, decisionId, npcName, outcome);
            return true;
        } catch (HttpTimeoutException e) {
            log.warn("NPC_CLICK_SMART outcome not submitted: reason=timeout after {}ms sessionId={} decisionId={} npc={} outcome={}",
                    properties.getTimeoutMs(), sessionId, decisionId, npcName, outcome, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("NPC_CLICK_SMART outcome not submitted: reason=interrupted sessionId={} decisionId={} npc={} outcome={}",
                    sessionId, decisionId, npcName, outcome, e);
            return false;
        } catch (IOException e) {
            log.warn("NPC_CLICK_SMART outcome not submitted: reason=http failure {} sessionId={} decisionId={} npc={} outcome={}",
                    e.getClass().getSimpleName(), sessionId, decisionId, npcName, outcome, e);
            return false;
        }
    }

    private String outcomeTransportDisabledReason() {
        if (!properties.isRealTransportEnabled()) {
            return "transport disabled: real transport not enabled";
        }
        if (!hasText(properties.getBaseUrl())) {
            return "transport disabled: missing endpoint";
        }
        if (!hasText(properties.getToken())) {
            return "transport disabled: missing token";
        }
        return null;
    }

    private URI outcomeEndpointUri() {
        String baseUrl = properties.getBaseUrl().trim();
        String endpointPath = hasText(properties.getNpcClickSmartOutcomePath())
                ? properties.getNpcClickSmartOutcomePath().trim()
                : "/api/cloud/npc-click-smart/outcome";
        if (baseUrl.endsWith("/") && endpointPath.startsWith("/")) {
            return URI.create(baseUrl.substring(0, baseUrl.length() - 1) + endpointPath);
        }
        if (!baseUrl.endsWith("/") && !endpointPath.startsWith("/")) {
            return URI.create(baseUrl + "/" + endpointPath);
        }
        return URI.create(baseUrl + endpointPath);
    }

    private static String taskCode(NpcClickRequest request) {
        return request == null || request.sourceTask() == null ? "unknown" : request.sourceTask().getCode();
    }

    private static void put(ObjectNode root, String field, String value) {
        if (value == null) {
            root.putNull(field);
        } else {
            root.put(field, value);
        }
    }

    private static void putInteger(ObjectNode root, String field, Integer value, Integer fallback) {
        Integer selected = value == null ? fallback : value;
        if (selected == null) {
            root.putNull(field);
        } else {
            root.put(field, selected);
        }
    }

    private static void setPoint(ObjectNode root, String field, int x, int y) {
        ObjectNode point = root.objectNode();
        point.put("x", x);
        point.put("y", y);
        root.set(field, point);
    }

    private static Point firstWindowBase(NpcClickSmartCloudRequest request) {
        if (request == null || request.getScanRegions() == null || request.getScanRegions().isEmpty()) {
            return null;
        }
        NpcClickSmartCloudRequest.ScanRegion first = request.getScanRegions().get(0);
        return first == null ? null : new Point(first.getWindowBaseX(), first.getWindowBaseY());
    }

    private static String normalize(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
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

    private static boolean insideRoi(Point point, NpcClickSmartCloudRequest.Roi roi) {
        return point != null
                && roi != null
                && point.x >= roi.getX()
                && point.x < roi.getX() + roi.getWidth()
                && point.y >= roi.getY()
                && point.y < roi.getY() + roi.getHeight();
    }

    private static boolean insideRoi(Box box, NpcClickSmartCloudRequest.Roi roi) {
        return box != null
                && roi != null
                && box.x() >= roi.getX()
                && box.y() >= roi.getY()
                && (long) box.x() + box.width() <= (long) roi.getX() + roi.getWidth()
                && (long) box.y() + box.height() <= (long) roi.getY() + roi.getHeight();
    }

    private static boolean insideAllowedRegion(Point point, NpcClickSmartCloudRequest request) {
        List<NpcClickSmartCloudRequest.ScanRegion> scanRegions = request == null ? null : request.getScanRegions();
        if (scanRegions != null && !scanRegions.isEmpty()) {
            return insideAnyScanRegion(point, scanRegions);
        }
        return request != null && insideRoi(point, request.getRoi());
    }

    private static boolean insideAllowedRegion(Box box, NpcClickSmartCloudRequest request) {
        List<NpcClickSmartCloudRequest.ScanRegion> scanRegions = request == null ? null : request.getScanRegions();
        if (scanRegions != null && !scanRegions.isEmpty()) {
            return insideAnyScanRegion(box, scanRegions);
        }
        return request != null && insideRoi(box, request.getRoi());
    }

    private static boolean insideAnyScanRegion(Point point, List<NpcClickSmartCloudRequest.ScanRegion> scanRegions) {
        return scanRegions != null && scanRegions.stream().anyMatch(region -> insideScanRegion(point, region));
    }

    private static boolean insideAnyScanRegion(Box box, List<NpcClickSmartCloudRequest.ScanRegion> scanRegions) {
        return scanRegions != null && scanRegions.stream().anyMatch(region -> insideScanRegion(box, region));
    }

    private static boolean insideScanRegion(Point point, NpcClickSmartCloudRequest.ScanRegion region) {
        return point != null
                && region != null
                && point.x >= region.getWindowX()
                && point.x < region.getWindowX() + region.getWidth()
                && point.y >= region.getWindowY()
                && point.y < region.getWindowY() + region.getHeight();
    }

    private static boolean insideScanRegion(Box box, NpcClickSmartCloudRequest.ScanRegion region) {
        return box != null
                && region != null
                && box.x() >= region.getWindowX()
                && box.y() >= region.getWindowY()
                && (long) box.x() + box.width() <= (long) region.getWindowX() + region.getWidth()
                && (long) box.y() + box.height() <= (long) region.getWindowY() + region.getHeight();
    }

    private static String roiText(NpcClickSmartCloudRequest.Roi roi) {
        if (roi == null) {
            return "";
        }
        return roi.getX() + "," + roi.getY() + "," + roi.getWidth() + "," + roi.getHeight();
    }

    private static String scanRegionsText(List<NpcClickSmartCloudRequest.ScanRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return "";
        }
        return String.join("|", regions.stream().map(NpcClickSmartCloudDecisionService::scanRegionText).toList());
    }

    private static String scanRegionText(NpcClickSmartCloudRequest.ScanRegion region) {
        if (region == null) {
            return "";
        }
        return "index=" + region.getIndex()
                + ",window=" + region.getWindowX() + "," + region.getWindowY()
                + "," + region.getWidth() + "," + region.getHeight()
                + ",screen=" + region.getScreenX() + "," + region.getScreenY()
                + "," + region.getScreenWidth() + "," + region.getScreenHeight()
                + ",base=" + region.getWindowBaseX() + "," + region.getWindowBaseY();
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

    private static String diagnostic(CloudDecisionResponse response, String key) {
        if (response == null || response.getDiagnostics() == null || key == null) {
            return "";
        }
        return safe(response.getDiagnostics().get(key));
    }

    private static String join(Iterable<String> values) {
        if (values == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (!hasText(value)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('|');
            }
            builder.append(value.trim());
        }
        return builder.toString();
    }

    private static boolean hasAnyText(Iterable<String> values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (hasText(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(Iterable<String> values, String... needles) {
        if (values == null || needles == null) {
            return false;
        }
        for (String value : values) {
            if (!hasText(value)) {
                continue;
            }
            for (String needle : needles) {
                if (needle != null && value.contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String firstText(String first, String second) {
        return hasText(first) ? first : safe(second);
    }

    private static String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private static String integerText(Integer value, Integer fallback) {
        Integer selected = value == null ? fallback : value;
        return selected == null ? "" : Integer.toString(selected);
    }

    private static String booleanText(Boolean value, boolean fallback) {
        return Boolean.toString(value == null ? fallback : value);
    }

    private static String normalizedUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safeTracePart(String value) {
        String normalized = hasText(value) ? value.trim() : "unknown";
        return normalized.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private record Box(int x, int y, int width, int height) {
        String toDecisionText() {
            return x + "," + y + "," + width + "," + height;
        }
    }

    private record ParseResult(
            boolean accepted,
            NpcClickSmartCloudDecision.Action action,
            Point click,
            Box candidateBox,
            String reason) {

        static ParseResult acceptedClick(NpcClickSmartCloudDecision.Action action, Point click, Box candidateBox) {
            return new ParseResult(true, action, click, candidateBox, "");
        }

        static ParseResult acceptedNoClick(NpcClickSmartCloudDecision.Action action) {
            return new ParseResult(true, action, null, null, "");
        }

        static ParseResult rejected(String reason) {
            return new ParseResult(false, NpcClickSmartCloudDecision.Action.ABORT, null, null, reason);
        }
    }
}
