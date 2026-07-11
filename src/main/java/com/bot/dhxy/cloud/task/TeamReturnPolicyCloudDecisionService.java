package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class TeamReturnPolicyCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(TeamReturnPolicyCloudDecisionService.class);
    private static final String DEFAULT_TASK_CODE = "unknown";

    private final CloudDecisionCoordinator coordinator;

    public TeamReturnPolicyCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Evaluate one team-return behavior gate through cloud-required execute.
     *
     * @param context current task/window context; nullable for tests.
     * @param phase member click, leader wait, or leader precheck phase.
     * @param source caller/source label.
     * @param localAllowed local policy result before cloud narrowing.
     * @param localReason local diagnostic reason.
     * @param contextFields extra screenshot/session facts.
     * @return effective allow/deny. Required failures deny instead of continuing locally.
     */
    public TeamReturnPolicyCloudDecision decide(TaskExecutionContext context,
                                                String phase,
                                                String source,
                                                boolean localAllowed,
                                                String localReason,
                                                Map<String, String> contextFields) {
        String localDecision = decision(phase, localAllowed, localReason);
        if (!coordinator.isActive(CloudDecisionServiceId.TEAM_RETURN_POLICY)) {
            return TeamReturnPolicyCloudDecision.localOnly(localDecision, localAllowed);
        }

        String taskCode = taskCode(context);
        CloudDecisionRequest request = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.TEAM_RETURN_POLICY)
                .traceId(traceId(taskCode, phase, source, context))
                .taskCode(taskCode)
                .phase(normalize(phase, "team-return-policy"))
                .windowId(context == null ? null : normalize(context.getWindowId(), null))
                .taskRunId(context == null ? null : Long.toString(context.getTaskRunId()))
                .localDecision(localDecision)
                .context(requestContext(context, phase, source, localAllowed, localReason, contextFields))
                .build();

        boolean[] cloudAllow = {false};
        CloudDecisionResult cloudResult = coordinator.shadow(
                request,
                localDecision,
                teamReturnExecutionGate(phase, localAllowed, localReason, cloudAllow));
        if (cloudResult.isExecuted()) {
            return TeamReturnPolicyCloudDecision.cloudExecuted(
                    cloudResult, localDecision, localAllowed, cloudAllow[0]);
        }
        String reason = cloudResult.getReason();
        log.warn("TEAM_RETURN_POLICY cloud-required deny: taskCode={} phase={} source={} localAllowed={} reason={}",
                taskCode, phase, source, localAllowed, reason);
        return TeamReturnPolicyCloudDecision.requiredFailureDeny(
                cloudResult, localDecision, localAllowed, reason);
    }

    private CloudDecisionExecutionGate teamReturnExecutionGate(String phase,
                                                              boolean localAllowed,
                                                              String localReason,
                                                              boolean[] cloudAllow) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.TEAM_RETURN_POLICY;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                Map<String, String> fields = fields(response.getDecision());
                String action = upper(fields.get("action"));
                if (!"ALLOW".equals(action) && !"DENY".equals(action)) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "action must be ALLOW or DENY: " + safe(action));
                }
                cloudAllow[0] = "ALLOW".equals(action);
                boolean effectiveAllow = localAllowed && cloudAllow[0];
                String reason = effectiveAllow
                        ? normalize(fields.get("reason"), "cloud allowed")
                        : normalize(localReason, normalize(fields.get("reason"), "cloud denied"));
                return CloudDecisionExecutionGate.GateResult.accepted(
                        decision(phase, effectiveAllow, reason),
                        "execute percent gate hit; using team-return policy cloud decision");
            }
        };
    }

    private static Map<String, String> requestContext(TaskExecutionContext context,
                                                      String phase,
                                                      String source,
                                                      boolean localAllowed,
                                                      String localReason,
                                                      Map<String, String> extra) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("hook", "team-return-policy-execute");
        result.put("phase", safe(phase));
        result.put("source", safe(source));
        result.put("localAllowed", Boolean.toString(localAllowed));
        result.put("localReason", safe(localReason));
        result.put("requestedTaskCode", context == null ? "" : safe(context.getRequestedTaskCode()));
        result.put("windowId", context == null ? "" : safe(context.getWindowId()));
        result.put("windowRole", context == null ? "" : safe(context.getWindowRole()));
        result.put("localTeamSession", context == null ? "" : safe(context.getLocalTeamSessionKey()));
        result.put("localSupportMember", context == null ? "" : Boolean.toString(context.isLocalSupportMember()));
        result.put("localLeaderPresent", context == null ? "" : Boolean.toString(context.isLocalLeaderPresent()));
        result.put("localLeaderWindow", context == null ? "" : safe(context.getLocalLeaderWindowId()));
        if (extra != null) {
            extra.forEach((key, value) -> {
                String normalizedKey = normalize(key, null);
                if (normalizedKey != null) {
                    result.put(normalizedKey, safe(value));
                }
            });
        }
        return Map.copyOf(result);
    }

    private static String decision(String phase, boolean allowed, String reason) {
        return "action=" + (allowed ? "ALLOW" : "DENY")
                + ";phase=" + safe(phase)
                + ";reason=" + safe(reason);
    }

    private static String taskCode(TaskExecutionContext context) {
        if (context == null) {
            return DEFAULT_TASK_CODE;
        }
        if (context.getRequestedTaskCode() != null && !context.getRequestedTaskCode().isBlank()) {
            return context.getRequestedTaskCode();
        }
        if (context.getTaskCode() != null && !context.getTaskCode().isBlank()) {
            return context.getTaskCode();
        }
        return DEFAULT_TASK_CODE;
    }

    private static String traceId(String taskCode, String phase, String source, TaskExecutionContext context) {
        return "team-return-policy:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(phase)
                + ":" + safeTracePart(source)
                + ":" + safeTracePart(context == null ? null : context.getWindowId())
                + ":" + safeTracePart(context == null ? null : Long.toString(context.getTaskRunId()));
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

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeTracePart(String value) {
        String normalized = normalize(value, "unknown");
        return normalized.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }
}
