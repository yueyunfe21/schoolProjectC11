package com.bot.dhxy.cloud.runtime;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fire-and-forget reporter for runtime decision shadow samples.
 *
 * <p>This service is intentionally diagnostic-only: callers pass a local decision that has already
 * been made by existing business code, and this helper builds the cloud request plus current window
 * metadata before delegating to {@link CloudDecisionCoordinator#shadow(CloudDecisionRequest, String)}.
 * It does not return the coordinator result, so runtime business callers cannot accidentally consume
 * a cloud {@code effectiveDecision} in the CR-HC-011 shadow wave.</p>
 */
@Service
@RequiredArgsConstructor
public class RuntimeDecisionShadowService {

    private static final String HOOK = "runtime-shadow";
    private static final String DEFAULT_TASK_CODE = "unknown";
    private static final String DEFAULT_PHASE = "runtime-decision";

    private final CloudDecisionCoordinator coordinator;
    private final WindowTaskContextHolder windowTaskContextHolder;

    /**
     * Report one local runtime decision to the cloud shadow framework.
     *
     * @param serviceId CR-HC cloud service id to report; null requests are ignored.
     * @param taskCode local task code such as {@code wubei} or {@code xiuluo_v2}; blank becomes
     *                 {@code unknown}.
     * @param phase local phase/hook label after the decision is known; blank becomes
     *              {@code runtime-decision}.
     * @param source caller source label used for trace/context; nullable.
     * @param localDecision local business decision/result that remains authoritative; nullable.
     * @param context additional diagnostic fields; values are copied as strings and never used for
     *                execution.
     */
    public void shadow(CloudDecisionServiceId serviceId,
                       String taskCode,
                       String phase,
                       String source,
                       String localDecision,
                       Map<String, String> context) {
        if (serviceId == null || !coordinator.isActive(serviceId)) {
            return;
        }

        String normalizedTaskCode = normalize(taskCode, DEFAULT_TASK_CODE);
        String normalizedPhase = normalize(phase, DEFAULT_PHASE);
        Map<String, String> safeContext = context(source, normalizedTaskCode, context);
        CloudDecisionRequest request = CloudDecisionRequest.builder()
                .serviceId(serviceId)
                .traceId(traceId(serviceId, normalizedTaskCode, normalizedPhase, source))
                .taskCode(normalizedTaskCode)
                .phase(normalizedPhase)
                .windowId(currentWindowId())
                .taskRunId(safeContext.get("taskRunId"))
                .policyVersion(safeContext.get("policyVersion"))
                .localDecision(localDecision)
                .context(safeContext)
                .build();

        coordinator.shadow(request, localDecision);
    }

    private Map<String, String> context(String source, String taskCode, Map<String, String> context) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("hook", HOOK);
        result.put("source", safe(source));
        if (context != null) {
            context.forEach((key, value) -> {
                String normalizedKey = normalize(key, null);
                if (normalizedKey == null) {
                    return;
                }
                String targetKey = Objects.equals("source", normalizedKey) && hasText(source)
                        ? "callerSource"
                        : normalizedKey;
                result.put(targetKey, safe(value));
            });
        }
        windowTaskContextHolder.rawCurrent().ifPresent(runtime -> enrichRuntimeContext(result, runtime));
        putActiveTaskContext(result, taskCode);
        return Map.copyOf(result);
    }

    private static void enrichRuntimeContext(Map<String, String> context, WindowRuntimeContext runtime) {
        context.put("windowId", safe(runtime.getWindowId()));
        context.put("windowRole", runtime.getRole() == null ? "" : runtime.getRole().name());
        context.put("windowSelectedTaskType", runtime.getSelectedTaskType() == null ? "" : runtime.getSelectedTaskType().name());
        WindowNativeBinding binding = runtime.getNativeBinding();
        if (binding != null) {
            context.put("hwnd", safe(binding.getNativeHandle()));
            context.put("nativeTitle", safe(binding.getTitle()));
        }
    }

    private static void putActiveTaskContext(Map<String, String> context, String taskCode) {
        String activeTaskCode = normalize(taskCode, DEFAULT_TASK_CODE);
        String activeTaskType = taskTypeName(activeTaskCode);
        context.put("activeTaskCode", activeTaskCode);
        context.put("activeTaskType", activeTaskType);
        /*
         * Compatibility field for cloud dashboards/sidecar payloads. It must describe the current
         * request task, not the window's stale UI/default selection.
         */
        context.put("selectedTaskType", activeTaskType);
    }

    private String currentWindowId() {
        return windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getWindowId)
                .orElse(null);
    }

    private static String traceId(CloudDecisionServiceId serviceId, String taskCode, String phase, String source) {
        return "runtime-decision:"
                + serviceId.name()
                + ":" + safeTracePart(taskCode)
                + ":" + safeTracePart(phase)
                + ":" + safeTracePart(source);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String taskTypeName(String taskCode) {
        String normalizedTaskCode = normalize(taskCode, DEFAULT_TASK_CODE);
        for (TaskType taskType : TaskType.values()) {
            if (taskType.getCode().equalsIgnoreCase(normalizedTaskCode)) {
                return taskType.name();
            }
        }
        return TaskType.UNKNOWN.name();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeTracePart(String value) {
        String normalized = normalize(value, "unknown");
        return normalized.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }
}
