package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class MaintenanceThresholdCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceThresholdCloudDecisionService.class);
    private static final String PHASE = "maintenance-threshold";
    private static final String DEFAULT_TASK_CODE = "unknown";

    private final CloudDecisionCoordinator coordinator;

    public MaintenanceThresholdCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Decide whether the current maintenance pass may call downstream maintenance actions.
     *
     * @param context current task context; nullable for legacy tests.
     * @param request maintenance request already normalized by {@code TaskMaintenanceService}.
     * @param localAction local planned action before any downstream maintenance side effect.
     * @param localReason diagnostic reason for the local action.
     * @param contextFields extra maintenance/window facts.
     * @return cloud-required threshold decision. Required failures never allow downstream action.
     */
    public MaintenanceThresholdCloudDecision decide(TaskExecutionContext context,
                                                   TaskMaintenanceRequest request,
                                                   MaintenanceThresholdCloudDecision.Action localAction,
                                                   String localReason,
                                                   Map<String, String> contextFields) {
        MaintenanceThresholdCloudDecision.Action safeLocalAction =
                localAction == null ? MaintenanceThresholdCloudDecision.Action.NO_ACTION : localAction;
        String localDecision = decision(safeLocalAction, localReason);
        if (!coordinator.isActive(CloudDecisionServiceId.MAINTENANCE_THRESHOLD)) {
            return MaintenanceThresholdCloudDecision.localOnly(localDecision, safeLocalAction);
        }

        String taskCode = taskCode(context);
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.MAINTENANCE_THRESHOLD)
                .traceId(traceId(taskCode, request))
                .taskCode(taskCode)
                .phase(PHASE)
                .windowId(context == null ? null : normalize(context.getWindowId(), null))
                .taskRunId(context == null ? null : Long.toString(context.getTaskRunId()))
                .localDecision(localDecision)
                .context(requestContext(request, safeLocalAction, localReason, contextFields))
                .build();

        MaintenanceThresholdCloudDecision.Action[] effectiveAction = {
                MaintenanceThresholdCloudDecision.Action.NO_ACTION
        };
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                localDecision,
                maintenanceExecutionGate(safeLocalAction, localReason, effectiveAction));
        if (cloudResult.isExecuted()) {
            return MaintenanceThresholdCloudDecision.cloudExecuted(
                    cloudResult, localDecision, effectiveAction[0]);
        }
        String reason = cloudResult.getReason();
        log.warn("MAINTENANCE_THRESHOLD cloud-required failure: taskCode={} source={} localAction={} reason={}",
                taskCode, request == null ? null : request.getSourceTask(), safeLocalAction, reason);
        return MaintenanceThresholdCloudDecision.requiredFailure(cloudResult, localDecision, reason);
    }

    private CloudDecisionExecutionGate maintenanceExecutionGate(
            MaintenanceThresholdCloudDecision.Action localAction,
            String localReason,
            MaintenanceThresholdCloudDecision.Action[] effectiveAction) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.MAINTENANCE_THRESHOLD;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                Map<String, String> fields = fields(response.getDecision());
                MaintenanceThresholdCloudDecision.Action cloudAction = parseAction(fields.get("action"));
                if (cloudAction == null || cloudAction == MaintenanceThresholdCloudDecision.Action.REQUIRED_FAILURE) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "action must be ALLOW, SKIP, or NO_ACTION: " + safe(fields.get("action")));
                }
                effectiveAction[0] = localAction == MaintenanceThresholdCloudDecision.Action.ALLOW
                        ? cloudAction
                        : MaintenanceThresholdCloudDecision.Action.NO_ACTION;
                String reason = effectiveAction[0] == cloudAction
                        ? normalize(fields.get("reason"), "cloud threshold")
                        : normalize(localReason, "local no-action cannot be granted by cloud");
                return CloudDecisionExecutionGate.GateResult.accepted(
                        decision(effectiveAction[0], reason),
                        "execute percent gate hit; using maintenance threshold cloud decision");
            }
        };
    }

    private static Map<String, String> requestContext(TaskMaintenanceRequest request,
                                                      MaintenanceThresholdCloudDecision.Action localAction,
                                                      String localReason,
                                                      Map<String, String> extra) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("hook", "maintenance-threshold-execute");
        result.put("sourceTask", request == null ? "" : safe(request.getSourceTask()));
        result.put("localAction", localAction.name());
        result.put("localReason", safe(localReason));
        if (request != null) {
            result.put("handleMaintenanceBroadcast", Boolean.toString(request.isHandleMaintenanceBroadcast()));
            result.put("cleanSummonSkill", Boolean.toString(request.isCleanSummonSkill()));
            result.put("enqueueSummonSkillOnly", Boolean.toString(request.isEnqueueSummonSkillOnly()));
            result.put("requireFreeStateForSummonSkill", Boolean.toString(request.isRequireFreeStateForSummonSkill()));
            result.put("oneSummonSkillPerTeamRound", Boolean.toString(request.isOneSummonSkillPerTeamRound()));
            result.put("requiredLocalSupportCapability",
                    request.getRequiredLocalSupportCapability() == null
                            ? ""
                            : request.getRequiredLocalSupportCapability().name());
            result.put("teamMaintenanceKey", safe(request.getTeamMaintenanceKey()));
            result.put("teamRound", request.getTeamRound() == null ? "" : request.getTeamRound().toString());
        }
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

    private static String decision(MaintenanceThresholdCloudDecision.Action action, String reason) {
        return "action=" + action.name() + ";reason=" + safe(reason);
    }

    private static MaintenanceThresholdCloudDecision.Action parseAction(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MaintenanceThresholdCloudDecision.Action.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String traceId(String taskCode, TaskMaintenanceRequest request) {
        return "maintenance-threshold:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(request == null ? null : request.getSourceTask());
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
