package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionMode;
import com.bot.dhxy.cloud.decision.CloudDecisionProperties;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RouteCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(RouteCloudDecisionService.class);
    private static final String HOOK = "route-execute";
    private static final String DEFAULT_TASK_CODE = "navigation";
    private static final String DEFAULT_PHASE = "world-map-route-result";
    private static final String CURRENT_ROUTE_MODE = "YELLOW_DESTINATION_MINI_MAP";
    private static final String COORDINATE_SPACE_KEY = "coordinateSpace";
    private static final String COORDINATE_SPACE_WINDOW_RELATIVE = "WINDOW_RELATIVE";
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final int ROUTE_RESULT_ROI_MIN_X = 348;
    private static final int ROUTE_RESULT_ROI_MIN_Y = 376;
    private static final int ROUTE_RESULT_ROI_MAX_X = 671;
    private static final int ROUTE_RESULT_ROI_MAX_Y = 514;

    private final CloudDecisionCoordinator coordinator;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final CloudDecisionProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final Set<String> submittedOutcomeKeys = ConcurrentHashMap.newKeySet();

    public RouteCloudDecisionService(
            CloudDecisionCoordinator coordinator,
            WindowTaskContextHolder windowTaskContextHolder,
            CloudDecisionProperties properties) {
        this.coordinator = coordinator;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1L, properties.getTimeoutMs())))
                .build();
    }

    /**
     * Decide a route-candidate action through cloud shadow/execute mode.
     *
     * @param taskCode task code such as {@code wubei} or {@code xiuluo_v2}; blank becomes
     *                 {@code navigation}
     * @param source navigation source label used in trace/context; nullable
     * @param phase route-candidate phase, for example {@code world-map-route-result}; blank becomes
     *              {@code world-map-route-result}
     * @param localShadowDecision old local route-candidate decision, kept as oracle/shadow text
     * @param context route context such as from/target maps, route mode, and candidate source
     * @return route envelope containing the effective cloud click, local passthrough, or no-click
     */
    public RouteCloudDecision decideRouteCandidate(String taskCode,
                                                   String source,
                                                   String phase,
                                                   String localShadowDecision,
                                                   Map<String, String> context) {
        return decide(CloudDecisionServiceId.ROUTE_CANDIDATE, taskCode, source, phase,
                localShadowDecision, context);
    }

    private RouteCloudDecision decide(CloudDecisionServiceId serviceId,
                                      String taskCode,
                                      String source,
                                      String phase,
                                      String localShadowDecision,
                                      Map<String, String> context) {
        Point localClick = parseClick(fields(localShadowDecision).get("click"));
        if (!coordinator.isActive(serviceId)) {
            return RouteCloudDecision.localOnly(localShadowDecision, localClick);
        }

        String normalizedTaskCode = normalize(taskCode, DEFAULT_TASK_CODE);
        String normalizedPhase = normalize(phase, DEFAULT_PHASE);
        Map<String, String> requestContext = context(source, normalizedTaskCode, localShadowDecision, context);
        CloudDecisionRequest request = CloudDecisionRequest.builder()
                .serviceId(serviceId)
                .traceId(traceId(serviceId, normalizedTaskCode, normalizedPhase, source))
                .taskCode(normalizedTaskCode)
                .phase(normalizedPhase)
                .windowId(currentWindowId())
                .taskRunId(requestContext.get("taskRunId"))
                .policyVersion(requestContext.get("policyVersion"))
                .localDecision(localShadowDecision)
                .context(requestContext)
                .build();

        Point[] cloudClick = new Point[1];
        boolean[] gateEvaluated = {false};
        CloudDecisionResult cloudResult = coordinator.shadow(
                request,
                localShadowDecision,
                routeExecutionGate(serviceId, cloudClick, gateEvaluated));
        String routeDecisionId = routeDecisionId(cloudResult);
        if (cloudResult.isExecuted()) {
            if (cloudClick[0] != null) {
                return RouteCloudDecision.cloudExecuted(
                        cloudResult, localShadowDecision, localClick, cloudClick[0], routeDecisionId);
            }
            return RouteCloudDecision.cloudNoClick(
                    cloudResult, localShadowDecision, localClick, routeDecisionId, cloudResult.getReason());
        }
        if (keepsLocalPassthrough(cloudResult, gateEvaluated[0])) {
            return RouteCloudDecision.localPassthrough(cloudResult, localShadowDecision, localClick);
        }

        String rejectReason = cloudResult.getReason();
        log.warn("{} execute rejected no-click: taskCode={} phase={} source={} localShadowDecision={} cloudDecision={} reason={}",
                serviceId,
                normalizedTaskCode,
                normalizedPhase,
                safe(source),
                localShadowDecision,
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision(),
                rejectReason);
        return RouteCloudDecision.cloudRejectedNoClick(
                cloudResult, localShadowDecision, localClick, routeDecisionId, rejectReason);
    }

    /**
     * Report watcher-settled route-memory outcome to the route-memory authority.
     *
     * @param report local watcher facts; success/failure reports require a cloud-issued
     *               {@code routeDecisionId}, while {@code LEARN_CANDIDATE} may omit it.
     * @return ingest status. Failures are contained here and must not interrupt navigation.
     */
    public RouteMemoryOutcomeIngestResult reportRouteMemoryOutcome(RouteMemoryOutcomeReport report) {
        String idempotencyKey = outcomeIdempotencyKey(report);
        if (report == null) {
            return RouteMemoryOutcomeIngestResult.skipped(idempotencyKey, "missing report");
        }
        if (report.getResult() == null) {
            return RouteMemoryOutcomeIngestResult.skipped(idempotencyKey, "missing result");
        }
        if (report.getResult() != RouteMemoryOutcomeReport.Result.LEARN_CANDIDATE
                && !hasText(report.getRouteDecisionId())) {
            return RouteMemoryOutcomeIngestResult.skipped(idempotencyKey, "missing routeDecisionId");
        }
        if (!submittedOutcomeKeys.add(idempotencyKey)) {
            return RouteMemoryOutcomeIngestResult.duplicate(idempotencyKey);
        }
        String disabledReason = outcomeTransportDisabledReason();
        if (disabledReason != null) {
            submittedOutcomeKeys.remove(idempotencyKey);
            return RouteMemoryOutcomeIngestResult.skipped(idempotencyKey, disabledReason);
        }

        String body;
        try {
            body = serializeOutcome(report);
        } catch (JsonProcessingException e) {
            submittedOutcomeKeys.remove(idempotencyKey);
            return RouteMemoryOutcomeIngestResult.failed(idempotencyKey,
                    "json serialize failure: " + e.getOriginalMessage());
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(outcomeEndpointUri())
                .timeout(Duration.ofMillis(Math.max(1L, properties.getTimeoutMs())))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + properties.getToken().trim())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                submittedOutcomeKeys.remove(idempotencyKey);
                return RouteMemoryOutcomeIngestResult.failed(idempotencyKey,
                        "http status=" + response.statusCode());
            }
            log.info("cloud.route-memory.outcome submitted routeDecisionId={} intentId={} fromMap={} targetMap={} routeMode={} result={} elapsedMs={} reason={}",
                    report.getRouteDecisionId(), report.getIntentId(), report.getFromMap(), report.getTargetMap(),
                    report.getRouteMode(), report.getResult(), report.getElapsedMs(), report.getReason());
            return RouteMemoryOutcomeIngestResult.submitted(idempotencyKey);
        } catch (HttpTimeoutException e) {
            submittedOutcomeKeys.remove(idempotencyKey);
            return RouteMemoryOutcomeIngestResult.failed(idempotencyKey,
                    "timeout after " + properties.getTimeoutMs() + "ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            submittedOutcomeKeys.remove(idempotencyKey);
            return RouteMemoryOutcomeIngestResult.failed(idempotencyKey, "interrupted during outcome ingest");
        } catch (IOException e) {
            submittedOutcomeKeys.remove(idempotencyKey);
            return RouteMemoryOutcomeIngestResult.failed(idempotencyKey,
                    "http failure: " + e.getClass().getSimpleName());
        }
    }

    private CloudDecisionExecutionGate routeExecutionGate(
            CloudDecisionServiceId expectedServiceId,
            Point[] cloudClick,
            boolean[] gateEvaluated) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == expectedServiceId && serviceId == CloudDecisionServiceId.ROUTE_CANDIDATE;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                gateEvaluated[0] = true;
                RouteParseResult parsed = parse(response);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                cloudClick[0] = parsed.click();
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(),
                        parsed.click() == null
                                ? "execute percent gate hit; route cloud returned no-click decision"
                                : "execute percent gate hit; using route cloud window-relative click");
            }
        };
    }

    private RouteParseResult parse(CloudDecisionResponse response) {
        Map<String, String> fields = fields(response.getDecision());
        // CR208 P1 transitional compatibility: pre-canonical cloud responses carried the mode as
        // `mode=yellow-destination-mini-map`. Accept that legacy key/value form until every sidecar
        // returns the canonical `routeMode=YELLOW_DESTINATION_MINI_MAP`; unknown or missing modes
        // stay rejected.
        String rawRouteMode = hasText(fields.get("routeMode")) ? fields.get("routeMode") : fields.get("mode");
        String routeMode = canonicalRouteMode(rawRouteMode);
        if (!CURRENT_ROUTE_MODE.equals(routeMode)) {
            return RouteParseResult.rejected(!hasText(rawRouteMode)
                    ? "routeMode is required for route cloud decisions"
                    : "unsupported routeMode=" + rawRouteMode.trim());
        }
        String clickText = fields.get("click");
        Point click = parseClick(clickText);
        if (hasText(clickText)) {
            String coordinateSpace = response.getDiagnostics() == null
                    ? null
                    : response.getDiagnostics().get(COORDINATE_SPACE_KEY);
            if (!COORDINATE_SPACE_WINDOW_RELATIVE.equals(coordinateSpace)) {
                return RouteParseResult.rejected("diagnostics.coordinateSpace must be WINDOW_RELATIVE when click is present");
            }
            if (click == null) {
                return RouteParseResult.rejected("click must parse as click=<windowX>,<windowY>");
            }
            if (!insideWindow(click)) {
                return RouteParseResult.rejected("window-relative route click outside 1024x768 window: click="
                        + click.x + "," + click.y);
            }
            if (!insideRouteResultRoi(click)) {
                return RouteParseResult.rejected("window-relative route click outside route-result ROI "
                        + ROUTE_RESULT_ROI_MIN_X + "," + ROUTE_RESULT_ROI_MIN_Y
                        + "-" + ROUTE_RESULT_ROI_MAX_X + "," + ROUTE_RESULT_ROI_MAX_Y
                        + ": click=" + click.x + "," + click.y);
            }
        }
        return parseCandidate(fields, click);
    }

    private static String canonicalRouteMode(String routeMode) {
        if (!hasText(routeMode)) {
            return "";
        }
        return routeMode.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private RouteParseResult parseCandidate(Map<String, String> fields, Point click) {
        String status = upper(fields.get("status"));
        if (!hasText(status)) {
            return RouteParseResult.rejected("status is required for ROUTE_CANDIDATE");
        }
        if ("CLICKED".equals(status)) {
            if (click == null) {
                return RouteParseResult.rejected("click is required when ROUTE_CANDIDATE status=CLICKED");
            }
            if (!hasText(fields.get("routeDecisionId"))) {
                return RouteParseResult.rejected("routeDecisionId is required when ROUTE_CANDIDATE status=CLICKED");
            }
            return RouteParseResult.accepted(click);
        }
        if ("NOT_FOUND".equals(status) || "SKIP".equals(status) || "FAILED".equals(status)) {
            return RouteParseResult.accepted(null);
        }
        return RouteParseResult.rejected("unsupported ROUTE_CANDIDATE status=" + status);
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

    private Map<String, String> context(String source,
                                        String taskCode,
                                        String localShadowDecision,
                                        Map<String, String> context) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("hook", HOOK);
        result.put("source", safe(source));
        result.put("localShadowDecision", safe(localShadowDecision));
        if (context != null) {
            context.forEach((key, value) -> {
                String normalizedKey = normalize(key, null);
                if (normalizedKey != null) {
                    result.put(normalizedKey, safe(value));
                }
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
        context.put("activeTaskCode", activeTaskCode);
        context.put("activeTaskType", taskTypeName(activeTaskCode));
    }

    private String currentWindowId() {
        return windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getWindowId)
                .orElse(null);
    }

    private static String traceId(CloudDecisionServiceId serviceId, String taskCode, String phase, String source) {
        return "route-decision:"
                + serviceId.name()
                + ":" + safeTracePart(taskCode)
                + ":" + safeTracePart(phase)
                + ":" + safeTracePart(source);
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
            if (key.isEmpty()) {
                continue;
            }
            result.put(key, part.substring(separator + 1).trim());
        }
        return result;
    }

    private static Point parseClick(String value) {
        if (!hasText(value)) {
            return null;
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new Point(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String routeDecisionId(CloudDecisionResult result) {
        if (result == null || result.getResponse() == null) {
            return null;
        }
        return normalize(fields(result.getResponse().getDecision()).get("routeDecisionId"), null);
    }

    private String serializeOutcome(RouteMemoryOutcomeReport report) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        put(root, "routeDecisionId", report.getRouteDecisionId());
        put(root, "intentId", report.getIntentId());
        put(root, "fromMap", report.getFromMap());
        put(root, "targetMap", report.getTargetMap());
        put(root, "routeMode", report.getRouteMode());
        ObjectNode click = objectMapper.createObjectNode();
        put(click, "x", report.getClickX());
        put(click, "y", report.getClickY());
        root.set("click", click);
        ObjectNode observed = objectMapper.createObjectNode();
        put(observed, "map", report.getObservedMap());
        put(observed, "x", report.getObservedX());
        put(observed, "y", report.getObservedY());
        root.set("observed", observed);
        put(root, "result", report.getResult() == null ? null : report.getResult().name());
        root.put("elapsedMs", Math.max(0L, report.getElapsedMs()));
        put(root, "reason", report.getReason());
        put(root, "source", report.getSource());
        return objectMapper.writeValueAsString(root);
    }

    private String outcomeTransportDisabledReason() {
        if (!properties.isEnabled()) {
            return "cloud disabled";
        }
        if (!properties.isRealTransportEnabled()) {
            return "transport disabled: real transport not enabled";
        }
        if (!hasText(properties.getBaseUrl())) {
            return "transport disabled: missing endpoint";
        }
        if (!hasText(properties.getToken())) {
            return "transport disabled: missing token";
        }
        return null;
    }

    private URI outcomeEndpointUri() {
        String baseUrl = properties.getBaseUrl().trim();
        String endpointPath = hasText(properties.getRouteMemoryOutcomePath())
                ? properties.getRouteMemoryOutcomePath().trim()
                : "/api/cloud/route-memory/outcome";
        return endpointUri(baseUrl, endpointPath);
    }

    private static URI endpointUri(String baseUrl, String endpointPath) {
        if (baseUrl.endsWith("/") && endpointPath.startsWith("/")) {
            return URI.create(baseUrl.substring(0, baseUrl.length() - 1) + endpointPath);
        }
        if (!baseUrl.endsWith("/") && !endpointPath.startsWith("/")) {
            return URI.create(baseUrl + "/" + endpointPath);
        }
        return URI.create(baseUrl + endpointPath);
    }

    private static Map<String, String> diagnostics(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> values.put(entry.getKey(), entry.getValue().asText()));
        return values;
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root == null ? null : root.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static Integer integer(JsonNode root, String field) {
        JsonNode node = root == null ? null : root.get(field);
        return node == null || !node.canConvertToInt() ? null : node.asInt();
    }

    private static String outcomeIdempotencyKey(RouteMemoryOutcomeReport report) {
        if (report == null) {
            return "missing-report";
        }
        return safe(report.getRouteDecisionId())
                + "|" + safe(report.getIntentId())
                + "|" + safe(report.getFromMap())
                + "|" + safe(report.getTargetMap())
                + "|" + safe(report.getRouteMode())
                + "|" + safe(report.getResult() == null ? null : report.getResult().name());
    }

    private static void put(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private static void put(ObjectNode node, String field, Integer value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private static boolean insideWindow(Point point) {
        return point.x >= 0
                && point.x < WINDOW_WIDTH
                && point.y >= 0
                && point.y < WINDOW_HEIGHT;
    }

    private static boolean insideRouteResultRoi(Point point) {
        return point.x >= ROUTE_RESULT_ROI_MIN_X
                && point.x <= ROUTE_RESULT_ROI_MAX_X
                && point.y >= ROUTE_RESULT_ROI_MIN_Y
                && point.y <= ROUTE_RESULT_ROI_MAX_Y;
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

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean contains(String value, String expected) {
        return value != null && value.contains(expected);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeTracePart(String value) {
        String normalized = normalize(value, "unknown");
        return normalized.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private record RouteParseResult(boolean accepted, Point click, String reason) {
        static RouteParseResult accepted(Point click) {
            return new RouteParseResult(true, click == null ? null : new Point(click), null);
        }

        static RouteParseResult rejected(String reason) {
            return new RouteParseResult(false, null, reason);
        }
    }
}
