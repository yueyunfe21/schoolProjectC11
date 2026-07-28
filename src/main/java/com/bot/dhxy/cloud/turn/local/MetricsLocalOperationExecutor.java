package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnMetricEventPayload;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.model.metrics.AutomationMetricEvent;
import com.bot.dhxy.model.metrics.AutomationMetricEventType;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * TURN-40B-C1 closed adapter for the three {@code METRIC_*} wire operations.
 *
 * <p>Each call reconstructs one {@link AutomationMetricEvent} from the exact typed payload — every
 * persisted identity field (taskCode/taskName/windowId/windowRole/nativeWindowHandle) comes from
 * the wire, the type-specific round/failure attribute composition mirrors the three baseline
 * record methods byte-for-byte, and {@code caseDir} is kept verbatim in
 * {@code attributes.caseDir} — then performs exactly one true record through the sole
 * {@link AutomationMetricsService#recordWireEvent} seam. Dashboard queueing follows the frozen
 * mapping STARTED=false, FINISHED=true, FAILURE_CASE=false.</p>
 *
 * <p>This adapter never touches the input queue, never captures, never retries, and holds no
 * state; metrics remain diagnostics only.</p>
 */
@Component
public final class MetricsLocalOperationExecutor {

    private final AutomationMetricsService metricsService;

    public MetricsLocalOperationExecutor(AutomationMetricsService metricsService) {
        this.metricsService = Objects.requireNonNull(metricsService, "metricsService");
    }

    /** Executes one validated METRIC_* call; the validator already enforced the field shape. */
    public LocalServiceExecution execute(TurnLocalServiceCall call) {
        TurnMetricEventPayload m = call.metric();
        if (m == null) {
            return LocalServiceExecution.failed("METRIC_ARGUMENTS_MISSING", null);
        }
        return switch (call.operation()) {
            case METRIC_RECORD_ROUND_STARTED -> {
                Map<String, String> details = roundAttributes(
                        m.roundId(), m.roundNumber(), m.roundType(), m.attributes());
                metricsService.recordWireEvent(identityBuilder(m)
                        .eventType(AutomationMetricEventType.TASK_ROUND_STARTED)
                        .status(AutomationMetricStatus.STARTED)
                        .runId(m.roundId())
                        .phase(valueOrDefault(details.get("sourcePhase"), "round"))
                        .message(m.message())
                        .attributes(details)
                        .build(), false);
                yield LocalServiceExecution.completed("METRIC_RECORDED", null, null);
            }
            case METRIC_RECORD_ROUND_FINISHED -> {
                Map<String, String> details = mutableCopy(m.attributes());
                details.putAll(roundAttributes(m.roundId(), m.roundNumber(), m.roundType(), m.attributes()));
                if (m.resultCode() != null && !m.resultCode().isBlank()) {
                    details.put("resultCode", m.resultCode());
                }
                // The validator closes the status set at the wire boundary; this adapter never
                // normalizes — an illegal value fails closed with zero record and zero rewrite.
                AutomationMetricStatus status;
                try {
                    status = AutomationMetricStatus.valueOf(m.status());
                } catch (IllegalArgumentException | NullPointerException e) {
                    yield LocalServiceExecution.failed("METRIC_STATUS_INVALID", null);
                }
                metricsService.recordWireEvent(identityBuilder(m)
                        .eventType(AutomationMetricEventType.TASK_ROUND_FINISHED)
                        .status(status)
                        .runId(m.roundId())
                        .phase(valueOrDefault(details.get("sourcePhase"), "round"))
                        .elapsedMs(m.elapsedMs())
                        .errorCode(status == AutomationMetricStatus.SUCCESS ? null : m.resultCode())
                        .caseId(details.get("caseId"))
                        .message(m.message())
                        .attributes(Map.copyOf(details))
                        .build(), true);
                yield LocalServiceExecution.completed("METRIC_RECORDED", null, null);
            }
            case METRIC_RECORD_XIULUO_FAILURE_CASE -> {
                metricsService.recordWireEvent(identityBuilder(m)
                        .eventType(AutomationMetricEventType.XIULUO_FAILURE_CASE)
                        .status(AutomationMetricStatus.FAILED)
                        .phase(m.phase())
                        .errorCode(m.reason())
                        .caseId(caseIdOf(m.caseDir()))
                        .message(m.message())
                        .attributes(Map.of(
                                "round", Integer.toString(m.round()),
                                "caseDir", m.caseDir()))
                        .build(), false);
                yield LocalServiceExecution.completed("METRIC_RECORDED", null, null);
            }
            default -> LocalServiceExecution.failed("NOT_A_METRIC_OPERATION", null);
        };
    }

    /** All persisted identity comes from the wire; the local side never synthesizes it. */
    private static AutomationMetricEvent.AutomationMetricEventBuilder identityBuilder(TurnMetricEventPayload m) {
        return AutomationMetricEvent.builder()
                .taskCode(m.taskCode())
                .taskName(m.taskName())
                .windowId(m.windowId())
                .windowRole(m.windowRole())
                .nativeWindowHandle(m.nativeWindowHandle());
    }

    /** Mirrors the baseline private round-attribute composition byte-for-byte. */
    private static Map<String, String> roundAttributes(String roundId,
                                                       Integer roundNumber,
                                                       String roundType,
                                                       Map<String, String> attributes) {
        Map<String, String> details = mutableCopy(attributes);
        details.put("roundId", valueOrDefault(roundId, "unknown-round"));
        if (roundNumber != null && roundNumber > 0) {
            details.put("roundNumber", Integer.toString(roundNumber));
        }
        details.put("roundType", valueOrDefault(roundType, "unknown"));
        return Map.copyOf(details);
    }

    private static Map<String, String> mutableCopy(Map<String, String> attributes) {
        Map<String, String> copy = new LinkedHashMap<>();
        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null) {
                    copy.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return copy;
    }

    /** Baseline: the case id is the archive directory's file name; the verbatim path stays in attributes. */
    private static String caseIdOf(String caseDir) {
        try {
            Path fileName = Path.of(caseDir).getFileName();
            return fileName == null ? caseDir : fileName.toString();
        } catch (InvalidPathException e) {
            return caseDir;
        }
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
