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
import java.util.Set;

@Service
public class TaskRecoveryCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(TaskRecoveryCloudDecisionService.class);
    private static final String DEFAULT_TASK_CODE = "unknown";
    private static final String DEFAULT_SOURCE = "recovery";
    private static final String PHASE_RECOVERY = "recovery";
    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "retry-current-phase",
            "recover-to-main-task",
            "recover",
            "recovery-limit",
            "phase-failed",
            "loop-guard");

    private final CloudDecisionCoordinator coordinator;

    public TaskRecoveryCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Requires cloud authorization for one local task-recovery candidate.
     *
     * @param taskCode task code such as {@code wubei} or {@code xiuluo_v2}; blank becomes
     *                 {@code unknown}
     * @param source caller/source label used in trace/context; blank becomes {@code recovery}
     * @param round current task round number, written only to diagnostic context
     * @param failedPhase phase that failed and produced the local recovery candidate
     * @param localAction narrow local recovery action candidate, for example
     *                    {@code retry-current-phase} or {@code recover-to-main-task}
     * @param localNextPhase next phase in the local candidate; cloud must echo this same phase
     * @param phaseType task phase enum class used to parse cloud {@code next=}
     * @param context extra diagnostic fields such as message/recovery counts; values do not grant
     *                extra cloud authority
     * @return cloud recovery decision; callers may perform recovery only when
     *         {@link TaskRecoveryCloudDecision#isRecoveryAllowed()} is true
     */
    public <P extends Enum<P>> TaskRecoveryCloudDecision<P> decide(
            String taskCode,
            String source,
            int round,
            P failedPhase,
            String localAction,
            P localNextPhase,
            Class<P> phaseType,
            Map<String, String> context) {
        String normalizedTaskCode = normalize(taskCode, DEFAULT_TASK_CODE);
        String normalizedSource = normalize(source, DEFAULT_SOURCE);
        String normalizedAction = normalize(localAction, "");
        String localDecision = localDecision(failedPhase, normalizedAction, localNextPhase);
        if (!coordinator.isActive(CloudDecisionServiceId.TASK_RECOVERY)) {
            return TaskRecoveryCloudDecision.localPassthrough(
                    null, localDecision, normalizedAction, localNextPhase, "service inactive");
        }

        CloudDecisionRequest request = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.TASK_RECOVERY)
                .traceId(traceId(normalizedTaskCode, normalizedSource, failedPhase, normalizedAction))
                .taskCode(normalizedTaskCode)
                .phase(PHASE_RECOVERY)
                .localDecision(localDecision)
                .context(requestContext(
                        normalizedSource,
                        normalizedTaskCode,
                        round,
                        failedPhase,
                        normalizedAction,
                        localNextPhase,
                        context))
                .build();

        CloudDecisionResult cloudResult = coordinator.shadow(
                request,
                localDecision,
                taskRecoveryExecutionGate(phaseType, normalizedAction, localNextPhase));
        if (cloudResult.isExecuted()) {
            log.info("TASK_RECOVERY cloud accepted: taskCode={} source={} failedPhase={} action={} next={}",
                    normalizedTaskCode, normalizedSource, failedPhase, normalizedAction, localNextPhase);
            return TaskRecoveryCloudDecision.cloudExecuted(
                    cloudResult, localDecision, normalizedAction, localNextPhase);
        }
        if (keepsLocalPassthrough(cloudResult)) {
            return TaskRecoveryCloudDecision.localPassthrough(
                    cloudResult, localDecision, normalizedAction, localNextPhase, cloudResult.getReason());
        }
        log.error("TASK_RECOVERY cloud.required failure; no recovery: taskCode={} source={} failedPhase={} "
                        + "action={} next={} cloudDecision={} reason={}",
                normalizedTaskCode,
                normalizedSource,
                failedPhase,
                normalizedAction,
                localNextPhase,
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision(),
                cloudResult.getReason());
        return TaskRecoveryCloudDecision.cloudRequiredFailure(
                cloudResult, localDecision, normalizedAction, localNextPhase, cloudResult.getReason());
    }

    private <P extends Enum<P>> CloudDecisionExecutionGate taskRecoveryExecutionGate(
            Class<P> phaseType,
            String localAction,
            P localNextPhase) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.TASK_RECOVERY;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                TaskRecoveryParseResult<P> parsed = parse(response.getDecision(), phaseType);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                if (!parsed.action().equals(localAction)) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "action must match local recovery candidate: expected="
                                    + localAction + " actual=" + parsed.action());
                }
                if (parsed.nextPhase() != localNextPhase) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "next must match local recovery candidate: expected="
                                    + enumName(localNextPhase) + " actual=" + enumName(parsed.nextPhase()));
                }
                return CloudDecisionExecutionGate.GateResult.accepted(
                        effectiveDecision(parsed.action(), parsed.nextPhase(), fields(response.getDecision()).get("reason")),
                        "execute percent gate hit; using task-recovery authorization");
            }
        };
    }

    private static <P extends Enum<P>> TaskRecoveryParseResult<P> parse(String decision, Class<P> phaseType) {
        Map<String, String> fields = fields(decision);
        String action = fields.get("action");
        if (!hasText(action)) {
            return TaskRecoveryParseResult.rejected("action is required");
        }
        if (!ALLOWED_ACTIONS.contains(action)) {
            return TaskRecoveryParseResult.rejected("action is not allowed: " + action);
        }
        String nextText = fields.get("next");
        if (!hasText(nextText)) {
            return TaskRecoveryParseResult.rejected("next is required");
        }
        String reason = fields.get("reason");
        if (!hasText(reason)) {
            return TaskRecoveryParseResult.rejected("reason is required");
        }
        P nextPhase = parseEnum(phaseType, nextText);
        if (nextPhase == null) {
            return TaskRecoveryParseResult.rejected("next must parse to "
                    + phaseType.getSimpleName() + ": " + nextText);
        }
        return TaskRecoveryParseResult.accepted(action, nextPhase);
    }

    private static Map<String, String> requestContext(
            String source,
            String taskCode,
            int round,
            Enum<?> failedPhase,
            String localAction,
            Enum<?> localNextPhase,
            Map<String, String> context) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("hook", "task-recovery-execute");
        result.put("source", safe(source));
        result.put("round", Integer.toString(round));
        result.put("failedPhase", enumName(failedPhase));
        result.put("localAction", safe(localAction));
        result.put("localNextPhase", enumName(localNextPhase));
        result.put("activeTaskCode", normalize(taskCode, DEFAULT_TASK_CODE));
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

    private static boolean keepsLocalPassthrough(CloudDecisionResult cloudResult) {
        if (cloudResult == null || cloudResult.getMode() != CloudDecisionMode.EXECUTE) {
            return true;
        }
        return !cloudResult.isRequiredExecuteFailure();
    }

    private static String localDecision(Enum<?> failedPhase, String localAction, Enum<?> localNextPhase) {
        return "phase=" + enumName(failedPhase)
                + ";action=" + safe(localAction)
                + ";next=" + enumName(localNextPhase);
    }

    private static String effectiveDecision(String action, Enum<?> nextPhase, String reason) {
        String result = "action=" + action + ";next=" + enumName(nextPhase);
        return hasText(reason) ? result + ";reason=" + reason : result;
    }

    private static String traceId(String taskCode, String source, Enum<?> failedPhase, String action) {
        return "task-recovery:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(source)
                + ":" + safeTracePart(enumName(failedPhase))
                + ":" + safeTracePart(action);
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

    private static String safeTracePart(String value) {
        String normalized = normalize(value, "unknown");
        return normalized.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private record TaskRecoveryParseResult<P extends Enum<P>>(
            boolean accepted,
            String action,
            P nextPhase,
            String reason) {
        static <P extends Enum<P>> TaskRecoveryParseResult<P> accepted(String action, P nextPhase) {
            return new TaskRecoveryParseResult<>(true, action, nextPhase, null);
        }

        static <P extends Enum<P>> TaskRecoveryParseResult<P> rejected(String reason) {
            return new TaskRecoveryParseResult<>(false, null, null, reason);
        }
    }
}
