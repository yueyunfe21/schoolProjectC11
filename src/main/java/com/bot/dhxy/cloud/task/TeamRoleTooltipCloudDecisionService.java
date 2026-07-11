package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionMode;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class TeamRoleTooltipCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(TeamRoleTooltipCloudDecisionService.class);
    private static final String DEFAULT_TASK_CODE = "startup";
    private static final String DEFAULT_PHASE = "team-role-tooltip";
    private static final String LOCAL_SHADOW_DECISION = "status=LOCAL_SHADOW;role=UNKNOWN;reason=local";
    private static final String STATUS_FOUND = "FOUND";
    private static final String STATUS_NO_RESULT = "NO_RESULT";

    private final CloudDecisionCoordinator coordinator;

    public TeamRoleTooltipCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isActive() {
        return coordinator.isActive(CloudDecisionServiceId.TEAM_ROLE_TOOLTIP);
    }

    /**
     * Sends one masked hovered team-tooltip image to cloud business vision.
     *
     * @param request mask PNG tooltip payload plus current player/window context.
     * @return parsed cloud role result, disabled, or required-failure. The local caller owns any
     *         fallback strategy; this service does not run local OCR.
     */
    public TeamRoleTooltipCloudDecision detect(TeamRoleTooltipCloudRequest request) {
        if (!coordinator.isActive(CloudDecisionServiceId.TEAM_ROLE_TOOLTIP)) {
            return TeamRoleTooltipCloudDecision.builder()
                    .status(TeamRoleTooltipCloudDecision.Status.DISABLED)
                    .reason("service disabled")
                    .build();
        }
        String validationError = requestValidationError(request);
        if (validationError != null) {
            return requiredFailure(null, validationError);
        }

        String taskCode = normalize(request.getTaskCode(), DEFAULT_TASK_CODE);
        String phase = normalize(request.getPhase(), DEFAULT_PHASE);
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.TEAM_ROLE_TOOLTIP)
                .traceId(traceId(request, taskCode, phase))
                .taskCode(taskCode)
                .phase(phase)
                .windowId(normalize(request.getWindowId(), null))
                .taskRunId(normalize(request.getTaskRunId(), null))
                .policyVersion(normalize(request.getPolicyVersion(), null))
                .localDecision(LOCAL_SHADOW_DECISION)
                .context(context(request, taskCode, phase))
                .build();

        ParseResult[] parsedHolder = new ParseResult[1];
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                LOCAL_SHADOW_DECISION,
                executionGate(parsedHolder));
        if (cloudResult.isExecuted()) {
            ParseResult parsed = parsedHolder[0];
            if (parsed == null) {
                return requiredFailure(cloudResult, "missing parsed TEAM_ROLE_TOOLTIP result");
            }
            return TeamRoleTooltipCloudDecision.builder()
                    .status(parsed.found()
                            ? TeamRoleTooltipCloudDecision.Status.CLOUD_FOUND
                            : TeamRoleTooltipCloudDecision.Status.CLOUD_NO_RESULT)
                    .role(parsed.role())
                    .leaderClientId(parsed.leaderClientId())
                    .currentPlayerId(parsed.currentPlayerId())
                    .reason(parsed.reason())
                    .debugToken(parsed.debugToken())
                    .confidence(parsed.confidence())
                    .cloudResult(cloudResult)
                    .build();
        }

        if (cloudResult.getMode() == CloudDecisionMode.DISABLED) {
            return TeamRoleTooltipCloudDecision.builder()
                    .status(TeamRoleTooltipCloudDecision.Status.DISABLED)
                    .reason(cloudResult.getReason())
                    .cloudResult(cloudResult)
                    .build();
        }

        log.warn("cloud.execute serviceId=TEAM_ROLE_TOOLTIP accepted=false taskCode={} phase={} reason={} cloudDecision={}",
                cloudRequest.getTaskCode(),
                cloudRequest.getPhase(),
                cloudResult.getReason(),
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision());
        return requiredFailure(cloudResult, cloudResult.getReason());
    }

    private CloudDecisionExecutionGate executionGate(ParseResult[] parsedHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.TEAM_ROLE_TOOLTIP;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                ParseResult parsed = parse(response);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                parsedHolder[0] = parsed;
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(),
                        parsed.found()
                                ? "execute percent gate hit; using TEAM_ROLE_TOOLTIP cloud result"
                                : "execute percent gate hit; TEAM_ROLE_TOOLTIP no-result");
            }
        };
    }

    private static ParseResult parse(CloudDecisionResponse response) {
        Map<String, String> fields = fields(response.getDecision());
        Map<String, String> diagnostics = diagnostics(response);
        String status = upper(firstText(fields.get("status"), diagnostics.get("status")));
        String roleText = upper(firstText(fields.get("role"), diagnostics.get("role")));
        String reason = firstText(fields.get("reason"), diagnostics.get("reason"));
        TeamRoleTooltipCloudDecision.Role role = parseRole(roleText);
        if (STATUS_NO_RESULT.equals(status)) {
            return ParseResult.noResult(role, firstText(reason, "status=NO_RESULT"), diagnostics);
        }
        if (!STATUS_FOUND.equals(status)) {
            return ParseResult.rejected("status must be FOUND or NO_RESULT");
        }
        if (role == TeamRoleTooltipCloudDecision.Role.UNKNOWN) {
            return ParseResult.rejected("FOUND requires role=LEADER or MEMBER");
        }
        String leaderClientId = firstText(fields.get("leaderClientId"), diagnostics.get("leaderClientId"));
        if (!hasText(leaderClientId)) {
            return ParseResult.rejected("FOUND requires leaderClientId");
        }
        return ParseResult.found(
                role,
                leaderClientId,
                firstText(fields.get("currentPlayerId"), diagnostics.get("currentPlayerId")),
                firstText(reason, "cloud hit"),
                diagnostics,
                response.getConfidence());
    }

    private static String requestValidationError(TeamRoleTooltipCloudRequest request) {
        if (request == null) {
            return "missing team role tooltip request";
        }
        if (!hasText(request.getImagePayloadBase64())) {
            return "imagePayloadBase64 is required";
        }
        if (!hasText(request.getPayloadMimeType())) {
            return "payloadMimeType is required";
        }
        if (request.getWindowWidth() <= 0 || request.getWindowHeight() <= 0) {
            return "windowWidth/windowHeight are required";
        }
        return null;
    }

    private static Map<String, String> context(
            TeamRoleTooltipCloudRequest request,
            String taskCode,
            String phase) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("hook", "team-role-tooltip");
        result.put("taskCode", taskCode);
        result.put("phase", phase);
        result.put("source", safe(request.getSource()));
        result.put("imagePayloadBase64", request.getImagePayloadBase64());
        result.put("payloadMimeType", safe(request.getPayloadMimeType()));
        result.put("imageSha256", safe(request.getImageSha256()));
        result.put("rawImagePath", safe(request.getRawImagePath()));
        result.put("debugImageId", safe(request.getDebugImageId()));
        result.put("currentPlayerId", safe(request.getCurrentPlayerId()));
        result.put("clientId", safe(request.getCurrentPlayerId()));
        result.put("windowWidth", Integer.toString(request.getWindowWidth()));
        result.put("windowHeight", Integer.toString(request.getWindowHeight()));
        result.put("windowSize", request.getWindowWidth() + "," + request.getWindowHeight());
        result.put("windowId", safe(request.getWindowId()));
        result.put("taskRunId", safe(request.getTaskRunId()));
        result.put("policyVersion", safe(request.getPolicyVersion()));
        result.put("hwnd", safe(request.getHwnd()));
        if (request.getRoi() != null) {
            result.put("roi", request.getRoi().getX()
                    + "," + request.getRoi().getY()
                    + "," + request.getRoi().getWidth()
                    + "," + request.getRoi().getHeight());
        }
        return Map.copyOf(result);
    }

    private static TeamRoleTooltipCloudDecision requiredFailure(
            CloudDecisionResult cloudResult,
            String reason) {
        return TeamRoleTooltipCloudDecision.builder()
                .status(TeamRoleTooltipCloudDecision.Status.REQUIRED_FAILURE)
                .reason(reason)
                .cloudResult(cloudResult)
                .build();
    }

    private static String traceId(TeamRoleTooltipCloudRequest request, String taskCode, String phase) {
        return "team-role-tooltip:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(phase)
                + ":" + safeTracePart(request.getWindowId())
                + ":" + safeTracePart(request.getTaskRunId());
    }

    private static Map<String, String> fields(String decision) {
        Map<String, String> result = new LinkedHashMap<>();
        if (decision == null || decision.isBlank()) {
            return result;
        }
        for (String part : decision.split(";", -1)) {
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

    private static Map<String, String> diagnostics(CloudDecisionResponse response) {
        if (response == null || response.getDiagnostics() == null) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        response.getDiagnostics().forEach((key, value) ->
                result.put(key, value == null ? "" : value));
        return result;
    }

    private static TeamRoleTooltipCloudDecision.Role parseRole(String value) {
        return switch (upper(value)) {
            case "LEADER" -> TeamRoleTooltipCloudDecision.Role.LEADER;
            case "MEMBER" -> TeamRoleTooltipCloudDecision.Role.MEMBER;
            default -> TeamRoleTooltipCloudDecision.Role.UNKNOWN;
        };
    }

    private static String firstText(String first, String second) {
        return hasText(first) ? first.trim() : hasText(second) ? second.trim() : "";
    }

    private static String normalize(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private static String upper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeTracePart(String value) {
        return value == null ? "-" : value.trim().replaceAll("[^\\p{IsHan}A-Za-z0-9_.:-]", "_");
    }

    private record ParseResult(
            boolean accepted,
            boolean found,
            TeamRoleTooltipCloudDecision.Role role,
            String leaderClientId,
            String currentPlayerId,
            String reason,
            String debugToken,
            double confidence) {

        static ParseResult rejected(String reason) {
            return new ParseResult(false, false, TeamRoleTooltipCloudDecision.Role.UNKNOWN,
                    "", "", reason, "", 0.0d);
        }

        static ParseResult noResult(
                TeamRoleTooltipCloudDecision.Role role,
                String reason,
                Map<String, String> diagnostics) {
            return new ParseResult(true, false, role,
                    "", "", reason, diagnostics.getOrDefault("debugToken", ""), 0.0d);
        }

        static ParseResult found(
                TeamRoleTooltipCloudDecision.Role role,
                String leaderClientId,
                String currentPlayerId,
                String reason,
                Map<String, String> diagnostics,
                double confidence) {
            return new ParseResult(true, true, role, leaderClientId, currentPlayerId,
                    reason, diagnostics.getOrDefault("debugToken", ""), confidence);
        }
    }
}
