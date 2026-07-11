package com.bot.dhxy.cloud.decision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class CloudDecisionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(CloudDecisionCoordinator.class);
    private static final String IMAGE_PAYLOAD_BASE64_KEY = "imagePayloadBase64";
    private static final String IMAGE_SHA256_KEY = "imageSha256";
    private static final String SHADOW_LOCAL_DECISION_REASON =
            "shadow mode keeps local decision; cloud decision not executed";
    private static final Set<CloudDecisionServiceId> EXECUTABLE_SERVICES = EnumSet.of(
            CloudDecisionServiceId.TASK_CLASSIFIER);
    private static final CloudDecisionExecutionGate DEFAULT_EXECUTION_GATE = new CloudDecisionExecutionGate() {
        @Override
        public boolean allowsExecution(CloudDecisionServiceId serviceId) {
            return isExecutableService(serviceId);
        }

        @Override
        public CloudDecisionExecutionGate.GateResult evaluate(
                CloudDecisionRequest request,
                CloudDecisionResponse response,
                String localDecision) {
            return CloudDecisionExecutionGate.GateResult.accepted(
                    response.getDecision(), "execute percent gate hit; using cloud decision");
        }
    };

    private final CloudDecisionProperties properties;
    private final CloudDecisionClient client;
    private final CloudDecisionMetricsService metricsService;

    public CloudDecisionCoordinator(CloudDecisionProperties properties, CloudDecisionClient client, CloudDecisionMetricsService metricsService) {
        this.properties = properties;
        this.client = client;
        this.metricsService = Objects.requireNonNull(metricsService, "metricsService must not be null");
    }

    public boolean isActive(CloudDecisionServiceId serviceId) {
        if (!properties.isEnabled() || serviceId == null) {
            return false;
        }
        CloudDecisionProperties.Service service = properties.service(serviceId);
        return service.isShadowEnabled() || service.isExecuteEnabled();
    }

    /**
     * Calls the configured cloud-decision client and returns the local-vs-cloud decision envelope.
     *
     * <p>Cloud responses become effective only when the service is explicitly configured for execute
     * mode, the service id is allowed by the execution gate, the deterministic percent gate hits, the
     * response schema matches the request, and the gate maps the response to a locally safe effective
     * decision. Shadow mode, non-executable services, rollout misses, client failures, empty
     * responses, schema mismatches, and gate rejections all keep the local decision with
     * {@code executed=false}.</p>
     *
     * @param request cloud-decision request containing service id, trace id, window/task metadata,
     *                and the caller's local decision context; must not be {@code null}
     * @param localDecision the decision the local business path will execute; may be {@code null}
     * @return local-vs-cloud comparison result with the effective decision selected by the safe
     *         shadow/execute gate.
     */
    public CloudDecisionResult shadow(CloudDecisionRequest request, String localDecision) {
        return shadow(request, localDecision, DEFAULT_EXECUTION_GATE);
    }

    /**
     * Calls the cloud-decision client with a service-specific execute gate.
     *
     * @param request cloud-decision request; must not be {@code null}
     * @param localDecision local business decision used for fallback
     * @param executionGate service gate that decides whether execute mode can safely map the cloud
     *                      response to a local effective decision; when {@code null}, the default
     *                      coordinator allowlist is used
     * @return local-vs-cloud comparison result after the common and service-specific execute gates
     */
    public CloudDecisionResult shadow(
            CloudDecisionRequest request,
            String localDecision,
            CloudDecisionExecutionGate executionGate) {
        Objects.requireNonNull(request, "request must not be null");
        CloudDecisionExecutionGate safeExecutionGate =
                executionGate == null ? DEFAULT_EXECUTION_GATE : executionGate;

        if (!properties.isEnabled()) {
            return disabled(request, localDecision, properties.getDefaultFallback(), "cloud disabled");
        }

        if (request.getServiceId() == null) {
            return disabled(request, localDecision, properties.getDefaultFallback(), "missing service id");
        }

        CloudDecisionProperties.Service service = properties.service(request.getServiceId());
        CloudFallbackMode fallbackMode = fallbackMode(service);
        if (!service.isShadowEnabled() && !service.isExecuteEnabled()) {
            return disabled(request, localDecision, fallbackMode, "service disabled");
        }

        CloudDecisionMode mode = service.isExecuteEnabled()
                ? CloudDecisionMode.EXECUTE
                : CloudDecisionMode.SHADOW;
        long startNanos = System.nanoTime();
        try {
            CloudDecisionResponse response = client.decide(request);
            long elapsedMs = elapsedMs(startNanos);
            if (response == null) {
                return unavailable(request, localDecision, mode, fallbackMode, elapsedMs, "empty response");
            }
            String schemaMismatch = schemaMismatch(request, response);
            if (schemaMismatch != null) {
                return unavailable(request, localDecision, mode, fallbackMode, elapsedMs,
                        "schema mismatch: " + schemaMismatch);
            }

            boolean executeAllowed = safeExecutionGate.allowsExecution(request.getServiceId());
            boolean executePercentHit = mode == CloudDecisionMode.EXECUTE
                    && executeAllowed
                    && executePercentHit(request, service.getExecutePercent());
            boolean execute = false;
            String effectiveDecision = localDecision;
            String reason = decisionReason(mode, executeAllowed, false);
            if (executePercentHit) {
                CloudDecisionExecutionGate.GateResult gateResult;
                try {
                    gateResult = safeExecutionGate.evaluate(request, response, localDecision);
                } catch (RuntimeException e) {
                    return unavailable(request, localDecision, mode, fallbackMode, elapsedMs, response,
                            "execute gate exception: " + e.getClass().getSimpleName() + messageSuffix(e));
                }
                if (gateResult == null) {
                    return unavailable(request, localDecision, mode, fallbackMode, elapsedMs, response,
                            "execute gate rejected: empty gate result");
                }
                if (!gateResult.accepted()) {
                    return unavailable(request, localDecision, mode, fallbackMode, elapsedMs, response,
                            "execute gate rejected: " + gateReason(gateResult));
                }
                if (!hasText(gateResult.effectiveDecision())) {
                    return unavailable(request, localDecision, mode, fallbackMode, elapsedMs, response,
                            "execute gate rejected: effective decision missing");
                }
                execute = true;
                effectiveDecision = gateResult.effectiveDecision();
                reason = gateReason(gateResult);
            }
            boolean agreement = decisionsAgree(request.getServiceId(), localDecision, response.getDecision());
            CloudDecisionResult result = CloudDecisionResult.builder()
                    .mode(mode)
                    .fallbackMode(fallbackMode)
                    .request(request)
                    .response(response)
                    .localDecision(localDecision)
                    .effectiveDecision(effectiveDecisionFor(mode, fallbackMode, execute, effectiveDecision))
                    .cloudAvailable(true)
                    .agreement(agreement)
                    .executed(execute)
                    .elapsedMs(elapsedMs)
                    .reason(reason)
                    .build();
            logDecision(result);
            recordMetrics(result);
            return result;
        } catch (CloudDecisionClientException e) {
            long elapsedMs = elapsedMs(startNanos);
            return unavailable(request, localDecision, mode, fallbackMode, elapsedMs, e.getMessage());
        } catch (RuntimeException e) {
            long elapsedMs = elapsedMs(startNanos);
            return unavailable(request, localDecision, mode, fallbackMode, elapsedMs,
                    "client exception: " + e.getClass().getSimpleName() + messageSuffix(e));
        }
    }

    private CloudDecisionResult disabled(
            CloudDecisionRequest request,
            String localDecision,
            CloudFallbackMode fallbackMode,
            String reason) {
        CloudDecisionResult result = CloudDecisionResult.builder()
                .mode(CloudDecisionMode.DISABLED)
                .fallbackMode(fallbackMode)
                .request(request)
                .localDecision(localDecision)
                .effectiveDecision(localDecision)
                .cloudAvailable(false)
                .agreement(false)
                .executed(false)
                .elapsedMs(0L)
                .reason(reason)
                .build();
        logDecision(result);
        recordMetrics(result);
        return result;
    }

    private CloudDecisionResult unavailable(
            CloudDecisionRequest request,
            String localDecision,
            CloudDecisionMode mode,
            CloudFallbackMode fallbackMode,
            long elapsedMs,
            String reason) {
        return unavailable(request, localDecision, mode, fallbackMode, elapsedMs, null, reason);
    }

    private CloudDecisionResult unavailable(
            CloudDecisionRequest request,
            String localDecision,
            CloudDecisionMode mode,
            CloudFallbackMode fallbackMode,
            long elapsedMs,
            CloudDecisionResponse response,
            String reason) {
        CloudDecisionResult result = CloudDecisionResult.builder()
                .mode(mode)
                .fallbackMode(fallbackMode)
                .request(request)
                .response(response)
                .localDecision(localDecision)
                .effectiveDecision(effectiveDecisionFor(mode, fallbackMode, false, localDecision))
                .cloudAvailable(false)
                .agreement(false)
                .executed(false)
                .elapsedMs(elapsedMs)
                .reason(reason)
                .build();
        logDecision(result);
        recordMetrics(result);
        return result;
    }

    private CloudFallbackMode fallbackMode(CloudDecisionProperties.Service service) {
        return service.getFallback() != null ? service.getFallback() : properties.getDefaultFallback();
    }

    private static boolean isExecutableService(CloudDecisionServiceId serviceId) {
        return EXECUTABLE_SERVICES.contains(serviceId);
    }

    private static String effectiveDecisionFor(
            CloudDecisionMode mode,
            CloudFallbackMode fallbackMode,
            boolean executed,
            String candidateDecision) {
        if (mode == CloudDecisionMode.EXECUTE
                && fallbackMode == CloudFallbackMode.STOP
                && !executed) {
            return null;
        }
        return candidateDecision;
    }

    private static boolean decisionsAgree(
            CloudDecisionServiceId serviceId,
            String localDecision,
            String cloudDecision) {
        if (serviceId == CloudDecisionServiceId.TASK_POLICY) {
            return taskPolicyBehaviorFieldsAgree(localDecision, cloudDecision);
        }
        return Objects.equals(localDecision, cloudDecision);
    }

    private static boolean taskPolicyBehaviorFieldsAgree(String localDecision, String cloudDecision) {
        Map<String, String> localFields = decisionFields(localDecision);
        Map<String, String> cloudFields = decisionFields(cloudDecision);
        return fieldAgrees(localFields, cloudFields, "result")
                && fieldAgrees(localFields, cloudFields, "yield")
                && fieldAgrees(localFields, cloudFields, "next");
    }

    private static boolean fieldAgrees(Map<String, String> localFields, Map<String, String> cloudFields, String field) {
        String localValue = localFields.get(field);
        String cloudValue = cloudFields.get(field);
        return hasText(localValue) && Objects.equals(localValue, cloudValue);
    }

    private static Map<String, String> decisionFields(String decision) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (!hasText(decision)) {
            return fields;
        }
        String[] parts = decision.split(";", -1);
        for (String part : parts) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = part.substring(0, separator).trim();
            if (!key.isEmpty()) {
                fields.put(key, part.substring(separator + 1).trim());
            }
        }
        return fields;
    }

    /**
     * Returns whether an execute-mode cloud result may replace the local decision for this request.
     *
     * @param request cloud decision request; its trace/service/task/phase fields define the stable sample key.
     * @param percent rollout percentage in integer percent units; {@code <=0} disables execution and
     *                {@code >=100} executes all schema-valid cloud responses.
     * @return true when the deterministic rollout bucket falls inside {@code percent}.
     */
    private static boolean executePercentHit(CloudDecisionRequest request, int percent) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        String key = safe(request.getTraceId())
                + "|" + request.getServiceId()
                + "|" + safe(request.getTaskCode())
                + "|" + safe(request.getPhase());
        int bucket = Math.floorMod(key.hashCode(), 100);
        return bucket < percent;
    }

    private static String decisionReason(CloudDecisionMode mode, boolean executeAllowed, boolean execute) {
        if (execute) {
            return "execute percent gate hit; using cloud decision";
        }
        if (mode == CloudDecisionMode.EXECUTE) {
            if (!executeAllowed) {
                return "execute not allowed for service; service not executable; keeping local decision";
            }
            return "execute percent gate missed; keeping local decision";
        }
        return SHADOW_LOCAL_DECISION_REASON;
    }

    private static String gateReason(CloudDecisionExecutionGate.GateResult gateResult) {
        return hasText(gateResult.reason())
                ? gateResult.reason()
                : "execute percent gate hit; using cloud decision";
    }

    private static long elapsedMs(long startNanos) {
        long elapsedNanos = System.nanoTime() - startNanos;
        long elapsedMs = elapsedNanos / 1_000_000L;
        return elapsedNanos > 0L ? Math.max(1L, elapsedMs) : 0L;
    }

    private static String schemaMismatch(CloudDecisionRequest request, CloudDecisionResponse response) {
        if (response.getServiceId() != request.getServiceId()) {
            return "serviceId expected=" + request.getServiceId() + " actual=" + response.getServiceId();
        }
        if (!Objects.equals(request.getTraceId(), response.getTraceId())) {
            return "traceId expected=" + request.getTraceId() + " actual=" + response.getTraceId();
        }
        if (response.getDecision() == null || response.getDecision().isBlank()) {
            return "decision missing";
        }
        return null;
    }

    private static String messageSuffix(RuntimeException e) {
        return e.getMessage() == null || e.getMessage().isBlank() ? "" : ": " + e.getMessage();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void recordMetrics(CloudDecisionResult result) {
        try {
            metricsService.record(result);
        } catch (RuntimeException e) {
            CloudDecisionRequest request = result.getRequest();
            log.warn(
                    "cloud.metrics record failed serviceId={} taskCode={} phase={} traceId={} reason={}",
                    request == null ? null : request.getServiceId(),
                    request == null ? null : request.getTaskCode(),
                    request == null ? null : request.getPhase(),
                    request == null ? null : request.getTraceId(),
                    e.toString());
            log.debug("cloud.metrics record failure stack", e);
        }
    }

    private static void logDecision(CloudDecisionResult result) {
        CloudDecisionRequest request = result.getRequest();
        CloudDecisionResponse response = result.getResponse();
        String cloudDecision = response == null ? null : response.getDecision();
        log.info(
                "cloud.decision serviceId={} mode={} taskCode={} phase={} traceId={} context={} localDecision={} cloudDecision={} effectiveDecision={} agree={} executed={} elapsedMs={} success={} fallback={} reason={}",
                request.getServiceId(),
                result.getMode(),
                request.getTaskCode(),
                request.getPhase(),
                request.getTraceId(),
                logSafeContext(request),
                result.getLocalDecision(),
                cloudDecision,
                result.getEffectiveDecision(),
                result.isAgreement(),
                result.isExecuted(),
                result.getElapsedMs(),
                result.isCloudAvailable(),
                result.getFallbackMode(),
                result.getReason());
    }

    static Map<String, String> logSafeContext(CloudDecisionRequest request) {
        if (request == null || request.getContext() == null || request.getContext().isEmpty()) {
            return Map.of();
        }
        Map<String, String> safeContext = new LinkedHashMap<>();
        String imageSha256 = request.getContext().get(IMAGE_SHA256_KEY);
        request.getContext().forEach((key, value) -> {
            if (IMAGE_PAYLOAD_BASE64_KEY.equals(key)) {
                safeContext.put(key, redactedPayload(value, imageSha256));
            } else {
                safeContext.put(key, value);
            }
        });
        return Map.copyOf(safeContext);
    }

    private static String redactedPayload(String value, String imageSha256) {
        int length = value == null ? 0 : value.length();
        String sha = hasText(imageSha256) ? imageSha256 : "unknown";
        return "<redacted len=" + length + " sha256=" + sha + ">";
    }
}
