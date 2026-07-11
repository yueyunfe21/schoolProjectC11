package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionMode;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class TaskPolicyCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(TaskPolicyCloudDecisionService.class);
    private static final String DEFAULT_TASK_CODE = "unknown";
    private static final String DEFAULT_SOURCE = "phase-outcome";
    private static final String PHASE_OUTCOME = "phase-outcome";

    private final CloudDecisionCoordinator coordinator;

    public TaskPolicyCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Evaluates a TASK_POLICY cloud response for one local phase outcome that has already been
     * computed by the task.
     *
     * @param taskCode task code such as {@code wubei} or {@code xiuluo_v2}; blank becomes
     *                 {@code unknown}
     * @param source caller/source label used in trace/context; blank becomes {@code phase-outcome}
     * @param round current task round number, written only to diagnostic context
     * @param currentPhase phase that produced the local outcome; must be from the task's own enum
     * @param runnerResult transaction runner result for the same phase; used for diagnostics and
     *                     local STOPPED protection
     * @param localResult local transaction result produced before cloud is consulted
     * @param localYieldPolicy local yield policy produced before cloud is consulted
     * @param localNextPhase local next phase produced before cloud is consulted
     * @param phaseType current task phase enum class; cloud {@code next=} must parse to this type
     * @param context extra diagnostic fields such as source/message/current/next source; values do
     *                not affect parsing or execution
     * @return task-policy envelope; callers may consume accepted cloud execution or required-failure
     *         terminal outcomes, while local STOPPED remains local stop safety.
     */
    public <P extends Enum<P>> TaskPolicyCloudDecision<P> decide(
            String taskCode,
            String source,
            int round,
            P currentPhase,
            TaskTransactionResult runnerResult,
            TaskTransactionResult localResult,
            TaskYieldPolicy localYieldPolicy,
            P localNextPhase,
            Class<P> phaseType,
            Map<String, String> context) {
        String localDecision = localDecision(currentPhase, localResult, localYieldPolicy, localNextPhase);
        if (!coordinator.isActive(CloudDecisionServiceId.TASK_POLICY)) {
            return TaskPolicyCloudDecision.localOnly(localDecision, localResult, localYieldPolicy, localNextPhase);
        }

        String normalizedTaskCode = normalize(taskCode, DEFAULT_TASK_CODE);
        String normalizedSource = normalize(source, DEFAULT_SOURCE);
        Map<String, String> requestContext = requestContext(
                normalizedSource,
                normalizedTaskCode,
                round,
                currentPhase,
                runnerResult,
                localResult,
                localYieldPolicy,
                localNextPhase,
                context);
        CloudDecisionRequest request = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.TASK_POLICY)
                .traceId(traceId(normalizedTaskCode, normalizedSource, currentPhase))
                .taskCode(normalizedTaskCode)
                .phase(PHASE_OUTCOME)
                .localDecision(localDecision)
                .context(requestContext)
                .build();

        TaskPolicyCloudDecision.AppliedOutcome<P>[] cloudOutcome = new TaskPolicyCloudDecision.AppliedOutcome[1];
        boolean[] gateEvaluated = {false};
        CloudDecisionResult cloudResult = coordinator.shadow(
                request,
                localDecision,
                taskPolicyExecutionGate(phaseType, runnerResult, localResult, cloudOutcome, gateEvaluated));
        if (cloudResult.isExecuted() && cloudOutcome[0] != null) {
            return TaskPolicyCloudDecision.cloudExecuted(
                    cloudResult, localDecision, localResult, localYieldPolicy, localNextPhase, cloudOutcome[0]);
        }
        if (isLocalStopped(runnerResult, localResult)) {
            return TaskPolicyCloudDecision.localPassthrough(
                    cloudResult,
                    localDecision,
                    localResult,
                    localYieldPolicy,
                    localNextPhase,
                    cloudResult.getReason());
        }
        if (keepsLocalPassthrough(cloudResult, gateEvaluated[0])) {
            return TaskPolicyCloudDecision.localPassthrough(
                    cloudResult, localDecision, localResult, localYieldPolicy, localNextPhase);
        }
        if (cloudResult.isRequiredExecuteFailure()) {
            P failurePhase = failurePhase(phaseType, localNextPhase);
            log.error("TASK_POLICY cloud.required failure; terminal task failure: taskCode={} source={} "
                            + "phase={} localDecision={} cloudDecision={} failurePhase={} reason={}",
                    normalizedTaskCode,
                    normalizedSource,
                    currentPhase,
                    localDecision,
                    cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision(),
                    failurePhase,
                    cloudResult.getReason());
            return TaskPolicyCloudDecision.cloudRequiredFailure(
                    cloudResult,
                    localDecision,
                    localResult,
                    localYieldPolicy,
                    localNextPhase,
                    failurePhase,
                    cloudResult.getReason());
        }

        String rejectReason = cloudResult.getReason();
        log.warn("TASK_POLICY execute rejected under non-required fallback; keep local outcome: taskCode={} source={} phase={} "
                        + "localDecision={} cloudDecision={} reason={}",
                normalizedTaskCode,
                normalizedSource,
                currentPhase,
                localDecision,
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision(),
                rejectReason);
        return TaskPolicyCloudDecision.cloudRejectedLocal(
                cloudResult, localDecision, localResult, localYieldPolicy, localNextPhase, rejectReason);
    }

    private <P extends Enum<P>> CloudDecisionExecutionGate taskPolicyExecutionGate(
            Class<P> phaseType,
            TaskTransactionResult runnerResult,
            TaskTransactionResult localResult,
            TaskPolicyCloudDecision.AppliedOutcome<P>[] cloudOutcome,
            boolean[] gateEvaluated) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.TASK_POLICY;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                gateEvaluated[0] = true;
                TaskPolicyParseResult<P> parsed = parse(response.getDecision(), phaseType);
                if (runnerResult == TaskTransactionResult.STOPPED || localResult == TaskTransactionResult.STOPPED) {
                    return CloudDecisionExecutionGate.GateResult.rejected("local STOPPED keeps local");
                }
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                if (parsed.outcome().transactionResult() == TaskTransactionResult.STOPPED) {
                    return CloudDecisionExecutionGate.GateResult.rejected("cloud result STOPPED is not allowed");
                }
                cloudOutcome[0] = parsed.outcome();
                return CloudDecisionExecutionGate.GateResult.accepted(
                        effectiveDecision(parsed.outcome(), fields(response.getDecision()).get("reason")),
                        "execute percent gate hit; using task-policy phase outcome");
            }
        };
    }

    private static <P extends Enum<P>> TaskPolicyParseResult<P> parse(String decision, Class<P> phaseType) {
        Map<String, String> fields = fields(decision);
        String resultText = fields.get("result");
        if (!hasText(resultText)) {
            return TaskPolicyParseResult.rejected("result is required");
        }
        String yieldText = fields.get("yield");
        if (!hasText(yieldText)) {
            return TaskPolicyParseResult.rejected("yield is required");
        }
        String nextText = fields.get("next");
        if (!hasText(nextText)) {
            return TaskPolicyParseResult.rejected("next is required");
        }
        String reason = fields.get("reason");
        if (!hasText(reason)) {
            return TaskPolicyParseResult.rejected("reason is required");
        }
        TaskTransactionResult result = parseEnum(TaskTransactionResult.class, resultText);
        if (result == null) {
            return TaskPolicyParseResult.rejected("result must parse to TaskTransactionResult: " + resultText);
        }
        TaskYieldPolicy yieldPolicy = parseEnum(TaskYieldPolicy.class, yieldText);
        if (yieldPolicy == null) {
            return TaskPolicyParseResult.rejected("yield must parse to TaskYieldPolicy: " + yieldText);
        }
        P nextPhase = parseEnum(phaseType, nextText);
        if (nextPhase == null) {
            return TaskPolicyParseResult.rejected("next must parse to "
                    + phaseType.getSimpleName() + ": " + nextText);
        }
        return TaskPolicyParseResult.accepted(
                new TaskPolicyCloudDecision.AppliedOutcome<>(result, yieldPolicy, nextPhase));
    }

    private static Map<String, String> requestContext(
            String source,
            String taskCode,
            int round,
            Enum<?> currentPhase,
            TaskTransactionResult runnerResult,
            TaskTransactionResult localResult,
            TaskYieldPolicy localYieldPolicy,
            Enum<?> localNextPhase,
            Map<String, String> context) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("hook", "task-policy-execute");
        result.put("source", safe(source));
        result.put("round", Integer.toString(round));
        result.put("phase", currentPhase == null ? "" : currentPhase.name());
        result.put("runnerResult", runnerResult == null ? "" : runnerResult.name());
        result.put("localResult", localResult == null ? "" : localResult.name());
        result.put("localYield", localYieldPolicy == null ? "" : localYieldPolicy.name());
        result.put("localNextPhase", localNextPhase == null ? "" : localNextPhase.name());
        result.put("activeTaskCode", normalize(taskCode, DEFAULT_TASK_CODE));
        result.put("activeTaskType", taskTypeName(taskCode));
        if (context != null) {
            context.forEach((key, value) -> {
                String normalizedKey = normalize(key, null);
                if (normalizedKey != null) {
                    result.put(normalizedKey, safe(value));
                }
            });
        }
        return Map.copyOf(result);
    }

    private boolean keepsLocalPassthrough(CloudDecisionResult cloudResult, boolean gateEvaluated) {
        if (cloudResult == null || cloudResult.getMode() != CloudDecisionMode.EXECUTE) {
            return true;
        }
        if (cloudResult.isExecuted()) {
            return false;
        }
        return !gateEvaluated
                && cloudResult.isCloudAvailable()
                && contains(cloudResult.getReason(), "percent");
    }

    private static boolean isLocalStopped(TaskTransactionResult runnerResult, TaskTransactionResult localResult) {
        return runnerResult == TaskTransactionResult.STOPPED || localResult == TaskTransactionResult.STOPPED;
    }

    private static String localDecision(
            Enum<?> currentPhase,
            TaskTransactionResult localResult,
            TaskYieldPolicy localYieldPolicy,
            Enum<?> localNextPhase) {
        return "phase=" + enumName(currentPhase)
                + ";result=" + enumName(localResult)
                + ";yield=" + enumName(localYieldPolicy)
                + ";next=" + enumName(localNextPhase);
    }

    private static String effectiveDecision(TaskPolicyCloudDecision.AppliedOutcome<?> outcome, String reason) {
        String result = "result=" + outcome.transactionResult()
                + ";yield=" + outcome.yieldPolicy()
                + ";next=" + outcome.nextPhase();
        return hasText(reason) ? result + ";reason=" + reason : result;
    }

    private static String traceId(String taskCode, String source, Enum<?> currentPhase) {
        return "task-policy:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(source)
                + ":" + safeTracePart(enumName(currentPhase));
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

    private static <P extends Enum<P>> P failurePhase(Class<P> phaseType, P fallback) {
        P failed = parseEnum(phaseType, "FAILED");
        return failed == null ? fallback : failed;
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

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private static boolean contains(String value, String expected) {
        return value != null && value.contains(expected);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeTracePart(String value) {
        String normalized = normalize(value, "unknown");
        return normalized.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private record TaskPolicyParseResult<P extends Enum<P>>(
            boolean accepted,
            TaskPolicyCloudDecision.AppliedOutcome<P> outcome,
            String reason) {
        static <P extends Enum<P>> TaskPolicyParseResult<P> accepted(
                TaskPolicyCloudDecision.AppliedOutcome<P> outcome) {
            return new TaskPolicyParseResult<>(true, outcome, null);
        }

        static <P extends Enum<P>> TaskPolicyParseResult<P> rejected(String reason) {
            return new TaskPolicyParseResult<>(false, null, reason);
        }
    }
}
