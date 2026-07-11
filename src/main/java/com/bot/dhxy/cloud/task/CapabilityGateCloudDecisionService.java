package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.model.maintenance.TeamSupportCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class CapabilityGateCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(CapabilityGateCloudDecisionService.class);
    private static final String PHASE = "local-support-capability-gate";
    private static final String DEFAULT_TASK_CODE = "unknown";

    private final CloudDecisionCoordinator coordinator;

    public CapabilityGateCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Evaluate a local support capability gate through cloud-required execute.
     *
     * @param taskCode task code that owns this support check; blank becomes {@code unknown}.
     * @param source caller/source label for trace and diagnostics.
     * @param windowId current local window id; may be blank for tests.
     * @param taskRunId current runner id as text; may be blank.
     * @param capability local capability being requested.
     * @param timeoutMs local wait timeout in milliseconds.
     * @param localAllowed result from the local session/capability gate.
     * @param localReason local diagnostic reason. Cloud can only narrow this result.
     * @param context extra local session/window fields for cloud diagnostics.
     * @return effective allow/deny. Required execute failures and invalid cloud responses deny.
     */
    public CapabilityGateCloudDecision decide(String taskCode,
                                              String source,
                                              String windowId,
                                              String taskRunId,
                                              TeamSupportCapability capability,
                                              long timeoutMs,
                                              boolean localAllowed,
                                              String localReason,
                                              Map<String, String> context) {
        String normalizedTaskCode = normalize(taskCode, DEFAULT_TASK_CODE);
        String localDecision = localDecision(capability, localAllowed, localReason);
        if (!coordinator.isActive(CloudDecisionServiceId.CAPABILITY_GATE)) {
            return CapabilityGateCloudDecision.localOnly(localDecision, localAllowed);
        }

        CloudDecisionRequest request = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.CAPABILITY_GATE)
                .traceId(traceId(normalizedTaskCode, source, capability, windowId, taskRunId))
                .taskCode(normalizedTaskCode)
                .phase(PHASE)
                .windowId(normalize(windowId, null))
                .taskRunId(normalize(taskRunId, null))
                .localDecision(localDecision)
                .context(requestContext(source, capability, timeoutMs, localAllowed, localReason, context))
                .build();

        boolean[] cloudAllow = {false};
        CloudDecisionResult cloudResult = coordinator.shadow(
                request,
                localDecision,
                capabilityGateExecutionGate(capability, localAllowed, localReason, cloudAllow));
        if (cloudResult.isExecuted()) {
            return CapabilityGateCloudDecision.cloudExecuted(
                    cloudResult, localDecision, localAllowed, cloudAllow[0]);
        }
        String reason = cloudResult.getReason();
        log.warn("CAPABILITY_GATE cloud-required deny: taskCode={} source={} capability={} localAllowed={} reason={}",
                normalizedTaskCode, source, capability, localAllowed, reason);
        return CapabilityGateCloudDecision.requiredFailureDeny(
                cloudResult, localDecision, localAllowed, reason);
    }

    private CloudDecisionExecutionGate capabilityGateExecutionGate(TeamSupportCapability capability,
                                                                  boolean localAllowed,
                                                                  String localReason,
                                                                  boolean[] cloudAllow) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.CAPABILITY_GATE;
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
                        effectiveDecision(capability, effectiveAllow, reason),
                        "execute percent gate hit; using capability gate cloud decision");
            }
        };
    }

    private static Map<String, String> requestContext(String source,
                                                      TeamSupportCapability capability,
                                                      long timeoutMs,
                                                      boolean localAllowed,
                                                      String localReason,
                                                      Map<String, String> extra) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("hook", "capability-gate-execute");
        result.put("source", safe(source));
        result.put("capability", capability == null ? "" : capability.name());
        result.put("timeoutMs", Long.toString(timeoutMs));
        result.put("localAllowed", Boolean.toString(localAllowed));
        result.put("localReason", safe(localReason));
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

    private static String localDecision(TeamSupportCapability capability, boolean allowed, String reason) {
        return effectiveDecision(capability, allowed, reason);
    }

    private static String effectiveDecision(TeamSupportCapability capability, boolean allowed, String reason) {
        return "action=" + (allowed ? "ALLOW" : "DENY")
                + ";capability=" + (capability == null ? "" : capability.name())
                + ";reason=" + safe(reason);
    }

    private static String traceId(String taskCode,
                                  String source,
                                  TeamSupportCapability capability,
                                  String windowId,
                                  String taskRunId) {
        return "capability-gate:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(source)
                + ":" + safeTracePart(capability == null ? "" : capability.name())
                + ":" + safeTracePart(windowId)
                + ":" + safeTracePart(taskRunId);
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
