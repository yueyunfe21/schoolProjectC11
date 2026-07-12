package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * CR260 (CR259 design card): network layer for the cloud {@code NAVIGATION_ROUTE_PLAN} orchestrator.
 *
 * <p>Each call reports the current step's observation facts (booleans computed by the identical
 * local ladder helpers) plus the prior action's outcome, and returns exactly one directive — an
 * allowlisted ACTION or a TERMINAL. The response passes the same CR258 five-field binding-echo gate
 * (windowId/hwnd/taskRunId/routePlanRequestId/clientFrame) before use; any mismatch or transport
 * failure is a structured failure so the caller stays fail-closed (MAP_NOT_REACHED). This service
 * neither executes actions nor builds NavigationResult — that is the NavigationService shell's job,
 * which also owns the terminal-fact-gate.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NavigationRoutePlanCloudDecisionService {

    private static final String LOCAL_SHADOW_DECISION = "status=LOCAL_SHADOW;reason=navigation-route-plan-cloud-required";

    private final CloudDecisionCoordinator coordinator;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final CoordinateHelper coordinateHelper;

    public boolean isActive() {
        return coordinator.isActive(CloudDecisionServiceId.NAVIGATION_ROUTE_PLAN);
    }

    public enum DirectiveKind {ACTION, TERMINAL}

    /** One cloud step directive. Exactly one of the ACTION or TERMINAL field groups is meaningful. */
    public record RoutePlanDirective(DirectiveKind kind,
                                     String stepId,
                                     String action,
                                     String actionContext,
                                     String actionReason,
                                     String terminalStatus,
                                     String messageKey,
                                     boolean alreadyActive,
                                     boolean ownedByNestedRoute) {
    }

    public enum RoutePlanStepStatus {OK, FAILED}

    public record RoutePlanStepResult(RoutePlanStepStatus status, RoutePlanDirective directive, String reason) {
    }

    /**
     * Observation facts for one ladder step — every field is computed by the identical local helper
     * the baseline navigateToMap uses. {@code priorAction/priorContext/priorOutcome} carry the result
     * of the action the shell just executed (blank on the first step).
     */
    @Value
    @Builder
    public static class RoutePlanStepRequest {
        String routePlanRequestId;
        boolean hasRuntime;
        boolean preparedRouteDialogUsable;
        String snapshotMapCheck;
        boolean currentAlreadyTarget;
        boolean compatibleActiveIntent;
        boolean hasActiveRouteTransferPreparation;
        boolean shouldYield;
        boolean freshSameTargetRoutePending;
        boolean hasCallerFreshMap;
        boolean callerFreshMapMatchesTarget;
        boolean staleRoutePreparation;
        String priorAction;
        String priorContext;
        String priorOutcome;
        String targetMapName;
        String taskCode;
        String source;
    }

    public RoutePlanStepResult decideNextStep(RoutePlanStepRequest request) {
        Identity identity = currentIdentity();
        if (identity == null) {
            return new RoutePlanStepResult(RoutePlanStepStatus.FAILED, null, "no bound window context");
        }
        if (!isActive()) {
            return new RoutePlanStepResult(RoutePlanStepStatus.FAILED, null, "NAVIGATION_ROUTE_PLAN cloud service inactive");
        }
        String clientFrame = clientFrame();
        Map<String, String> context = new LinkedHashMap<>();
        context.put("hasRuntime", Boolean.toString(request.isHasRuntime()));
        context.put("preparedRouteDialogUsable", Boolean.toString(request.isPreparedRouteDialogUsable()));
        context.put("snapshotMapCheck", safe(request.getSnapshotMapCheck()));
        context.put("currentAlreadyTarget", Boolean.toString(request.isCurrentAlreadyTarget()));
        context.put("compatibleActiveIntent", Boolean.toString(request.isCompatibleActiveIntent()));
        context.put("hasActiveRouteTransferPreparation", Boolean.toString(request.isHasActiveRouteTransferPreparation()));
        context.put("shouldYield", Boolean.toString(request.isShouldYield()));
        context.put("freshSameTargetRoutePending", Boolean.toString(request.isFreshSameTargetRoutePending()));
        context.put("hasCallerFreshMap", Boolean.toString(request.isHasCallerFreshMap()));
        context.put("callerFreshMapMatchesTarget", Boolean.toString(request.isCallerFreshMapMatchesTarget()));
        context.put("staleRoutePreparation", Boolean.toString(request.isStaleRoutePreparation()));
        context.put("priorAction", safe(request.getPriorAction()));
        context.put("priorContext", safe(request.getPriorContext()));
        context.put("priorOutcome", safe(request.getPriorOutcome()));
        context.put("targetMapName", safe(request.getTargetMapName()));
        context.put("routePlanRequestId", safe(request.getRoutePlanRequestId()));
        context.put("windowId", identity.windowId());
        context.put("hwnd", identity.hwnd());
        context.put("taskRunId", identity.taskRunId());
        context.put("clientFrame", clientFrame);
        context.put("source", safe(request.getSource()));
        context.put("phase", "navigation-route-plan");

        Map<String, String>[] fieldsHolder = newFieldsHolder();
        CloudDecisionResult cloudResult = decide(context, request.getTaskCode(),
                safe(request.getRoutePlanRequestId()), fieldsHolder);
        if (!cloudResult.isExecuted() || fieldsHolder[0] == null) {
            log.warn("cloud route plan step unavailable: source={} routePlanRequestId={} reason={}",
                    request.getSource(), request.getRoutePlanRequestId(), cloudResult.getReason());
            return new RoutePlanStepResult(RoutePlanStepStatus.FAILED, null, cloudResult.getReason());
        }
        Map<String, String> fields = fieldsHolder[0];
        String echoError = echoMismatch(fields, identity, request.getRoutePlanRequestId(), clientFrame);
        if (echoError != null) {
            log.warn("cloud route plan step rejected by binding echo: source={} routePlanRequestId={} mismatch={}",
                    request.getSource(), request.getRoutePlanRequestId(), echoError);
            return new RoutePlanStepResult(RoutePlanStepStatus.FAILED, null, "binding-echo-mismatch:" + echoError);
        }
        String stepId = fields.get("stepId");
        String directive = fields.get("directive");
        if (!"HIT".equalsIgnoreCase(fields.getOrDefault("status", "")) || isBlank(stepId) || isBlank(directive)) {
            log.warn("cloud route plan step malformed: source={} routePlanRequestId={} decision={}",
                    request.getSource(), request.getRoutePlanRequestId(), fields);
            return new RoutePlanStepResult(RoutePlanStepStatus.FAILED, null, "malformed-route-plan-decision");
        }
        RoutePlanDirective parsed;
        if ("ACTION".equals(directive)) {
            String action = fields.get("action");
            if (isBlank(action)) {
                return new RoutePlanStepResult(RoutePlanStepStatus.FAILED, null, "action-directive-missing-action");
            }
            parsed = new RoutePlanDirective(DirectiveKind.ACTION, stepId, action,
                    fields.getOrDefault("actionContext", ""), fields.getOrDefault("actionReason", ""),
                    null, null, false, false);
        } else if ("TERMINAL".equals(directive)) {
            String terminalStatus = fields.get("terminalStatus");
            String messageKey = fields.get("messageKey");
            if (isBlank(terminalStatus) || isBlank(messageKey)) {
                return new RoutePlanStepResult(RoutePlanStepStatus.FAILED, null, "terminal-directive-missing-fields");
            }
            parsed = new RoutePlanDirective(DirectiveKind.TERMINAL, stepId, null, null, null,
                    terminalStatus, messageKey,
                    Boolean.parseBoolean(fields.get("alreadyActive")),
                    Boolean.parseBoolean(fields.get("ownedByNestedRoute")));
        } else {
            return new RoutePlanStepResult(RoutePlanStepStatus.FAILED, null, "unknown-directive:" + directive);
        }
        log.info("cloud route plan step: source={} routePlanRequestId={} stepId={} kind={} action={} terminal={}/{}",
                request.getSource(), request.getRoutePlanRequestId(), stepId, parsed.kind(),
                parsed.action(), parsed.terminalStatus(), parsed.messageKey());
        return new RoutePlanStepResult(RoutePlanStepStatus.OK, parsed, fields.getOrDefault("reason", ""));
    }

    private CloudDecisionResult decide(Map<String, String> context,
                                       String taskCode,
                                       String traceSuffix,
                                       Map<String, String>[] fieldsHolder) {
        String normalizedTaskCode = taskCode == null || taskCode.isBlank() ? "navigation" : taskCode;
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.NAVIGATION_ROUTE_PLAN)
                .traceId(traceId(normalizedTaskCode, context.get("windowId"), traceSuffix))
                .taskCode(normalizedTaskCode)
                .phase("navigation-route-plan")
                .windowId(context.get("windowId"))
                .taskRunId(context.get("taskRunId"))
                .localDecision(LOCAL_SHADOW_DECISION)
                .context(context)
                .build();
        return coordinator.shadow(cloudRequest, LOCAL_SHADOW_DECISION, executionGate(fieldsHolder));
    }

    private CloudDecisionExecutionGate executionGate(Map<String, String>[] fieldsHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.NAVIGATION_ROUTE_PLAN;
            }

            @Override
            public GateResult evaluate(CloudDecisionRequest request,
                                       CloudDecisionResponse response,
                                       String localDecision) {
                String decision = response == null ? null : response.getDecision();
                Map<String, String> fields = fields(decision);
                if (!"HIT".equalsIgnoreCase(fields.getOrDefault("status", ""))) {
                    return GateResult.rejected("unexpected route-plan status: " + fields.get("status"));
                }
                fieldsHolder[0] = fields;
                return GateResult.accepted(decision, "route-plan-step");
            }
        };
    }

    private record Identity(String windowId, String hwnd, String taskRunId) {
    }

    private Identity currentIdentity() {
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return null;
        }
        WindowNativeBinding binding = runtime.getNativeBinding();
        String hwnd = binding == null ? null : binding.getNativeHandle();
        if (hwnd == null || hwnd.isBlank()) {
            return null;
        }
        long taskRunId = taskExecutionContextHolder.current()
                .map(TaskExecutionContext::getTaskRunId)
                .orElse(0L);
        return new Identity(runtime.getWindowId(), hwnd, Long.toString(taskRunId));
    }

    private String clientFrame() {
        return String.format(Locale.ROOT, "1024x768x%.2fx1", coordinateHelper.getScaleRatio());
    }

    private String echoMismatch(Map<String, String> fields,
                                Identity identity,
                                String routePlanRequestId,
                                String clientFrame) {
        if (!echoToken(identity.windowId()).equals(fields.get("windowId"))) {
            return "windowId";
        }
        if (!echoToken(identity.hwnd()).equals(fields.get("hwnd"))) {
            return "hwnd";
        }
        if (!echoToken(identity.taskRunId()).equals(fields.get("taskRunId"))) {
            return "taskRunId";
        }
        if (!echoToken(safe(routePlanRequestId)).equals(fields.get("routePlanRequestId"))) {
            return "routePlanRequestId";
        }
        if (!echoToken(clientFrame).equals(fields.get("clientFrame"))) {
            return "clientFrame";
        }
        return null;
    }

    private static String echoToken(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(';', '_').replace('=', '_').replace('|', '_').replace(':', '_');
    }

    private static String traceId(String taskCode, String windowId, String suffix) {
        return sanitizeTracePart("navigation-route-plan:" + taskCode + ":"
                + (windowId == null ? "" : windowId) + ":" + (suffix == null ? "" : suffix));
    }

    private static String sanitizeTracePart(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            result.append(Character.isLetterOrDigit(c) || c == ':' || c == '-' || c == '_' || c == '.' ? c : '_');
        }
        return result.toString();
    }

    private static Map<String, String> fields(String decision) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (decision == null || decision.isBlank()) {
            return fields;
        }
        for (String part : decision.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                fields.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String>[] newFieldsHolder() {
        return new Map[1];
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
