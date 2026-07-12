package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.model.MapCoordinate;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * CR258 (CR251 point action contract v5): cloud evaluation of the mini-map transform assets.
 *
 * <p>The map calibration table left the client in CR247; this service asks cloud-brain to evaluate
 * it per call. {@code RESOLVE_MINIMAP_CLICK} returns the ordered candidate batch for one navigation
 * call (user-approved route B prefetch) with per-candidate single-use tokens and the echoed binding
 * fields the local execution gate checks before any physical input. The two scalar operations cover
 * the remaining production consumers of the local transform math. Cloud unreachable or a response
 * that fails the gate is a structured failure — there is no local fallback computation.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NavigationPointCloudDecisionService {

    private static final String LOCAL_SHADOW_DECISION = "status=LOCAL_SHADOW;reason=navigation-point-cloud-required";
    private static final String EXHAUSTED_REASON_CANDIDATES = "cloud-brain-minimap-candidates-exhausted";
    private static final String EXHAUSTED_REASON_TRANSFORM = "cloud-brain-minimap-transform-missing";
    private static final int CANDIDATE_WIRE_FIELDS = 12;

    private final CloudDecisionCoordinator coordinator;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final CoordinateHelper coordinateHelper;

    public boolean isActive() {
        return coordinator.isActive(CloudDecisionServiceId.MINIMAP_LOCATION);
    }

    /** One prefetched mini-map click candidate. Points are unscaled 1024x768 client-relative pixels. */
    public record MiniMapClickCandidate(String candidateId,
                                        String decisionId,
                                        int logicalX,
                                        int logicalY,
                                        int baseRelX,
                                        int baseRelY,
                                        int relX,
                                        int relY,
                                        int jitterX,
                                        int jitterY,
                                        int cursor,
                                        String reason) {
    }

    /** Ordered candidate batch bound to one navigation call (contract v5 items 1/3). */
    public record MiniMapClickCandidateBatch(String batchId,
                                             long batchExpiresAtMs,
                                             String windowId,
                                             String hwnd,
                                             String taskRunId,
                                             String navigationRequestId,
                                             String clientFrame,
                                             List<MiniMapClickCandidate> candidates) {
    }

    public enum MiniMapClickBatchStatus {
        /** Cloud returned a non-empty ordered batch. */
        BATCH,
        /** Deterministic exhaustion: no transform or the full ring was already attempted. */
        EXHAUSTED,
        /** Structured failure: cloud unreachable, rejected response, or bad echo. Fail-closed. */
        FAILED
    }

    public record MiniMapClickBatchResult(MiniMapClickBatchStatus status,
                                          MiniMapClickCandidateBatch batch,
                                          String reason) {
    }

    /** Request payload for {@link #resolveMiniMapClickBatch}. Identity fields come from the holders. */
    @Value
    @Builder
    public static class MiniMapClickBatchRequest {
        String mapName;
        int targetX;
        int targetY;
        boolean randomizeClickPoint;
        int randomRadiusPx;
        String navigationRequestId;
        long navigationDeadlineMs;
        Collection<String> attemptedCandidateIds;
        String taskCode;
        String source;
        String observedMapName;
        Integer observedX;
        Integer observedY;
        String observedSource;
        /**
         * Age of the observation in milliseconds, or negative when only cached state was available.
         * An age (never an absolute timestamp) crosses the wire so the cloud freshness check does
         * not depend on the two machines' wall clocks agreeing.
         */
        long observedAgeMs;
    }

    public MiniMapClickBatchResult resolveMiniMapClickBatch(MiniMapClickBatchRequest request) {
        Identity identity = currentIdentity();
        if (identity == null) {
            return new MiniMapClickBatchResult(MiniMapClickBatchStatus.FAILED, null,
                    "no bound window context");
        }
        if (!isActive()) {
            return new MiniMapClickBatchResult(MiniMapClickBatchStatus.FAILED, null,
                    "MINIMAP_LOCATION cloud service inactive");
        }
        String clientFrame = clientFrame();
        Map<String, String> context = new LinkedHashMap<>();
        context.put("operation", "RESOLVE_MINIMAP_CLICK");
        context.put("mapName", safe(request.getMapName()));
        context.put("targetX", Integer.toString(request.getTargetX()));
        context.put("targetY", Integer.toString(request.getTargetY()));
        context.put("randomizeClickPoint", Boolean.toString(request.isRandomizeClickPoint()));
        context.put("randomRadiusPx", Integer.toString(request.getRandomRadiusPx()));
        context.put("attemptedCandidateIds", request.getAttemptedCandidateIds() == null
                ? ""
                : String.join(",", request.getAttemptedCandidateIds()));
        context.put("navigationDeadlineMs", Long.toString(request.getNavigationDeadlineMs()));
        context.put("navigationRequestId", safe(request.getNavigationRequestId()));
        context.put("windowId", identity.windowId());
        context.put("hwnd", identity.hwnd());
        context.put("taskRunId", identity.taskRunId());
        context.put("clientFrame", clientFrame);
        context.put("observedMapName", safe(request.getObservedMapName()));
        context.put("observedX", request.getObservedX() == null ? "" : Integer.toString(request.getObservedX()));
        context.put("observedY", request.getObservedY() == null ? "" : Integer.toString(request.getObservedY()));
        context.put("observedSource", safe(request.getObservedSource()));
        context.put("observedAgeMs", Long.toString(request.getObservedAgeMs()));
        context.put("source", safe(request.getSource()));
        context.put("phase", "minimap-resolve-click");

        Map<String, String>[] fieldsHolder = newFieldsHolder();
        CloudDecisionResult cloudResult = decide(context, request.getTaskCode(), "minimap-resolve-click",
                safe(request.getNavigationRequestId()), fieldsHolder);
        if (!cloudResult.isExecuted() || fieldsHolder[0] == null) {
            log.warn("cloud minimap click batch unavailable: source={} navigationRequestId={} reason={}",
                    request.getSource(), request.getNavigationRequestId(), cloudResult.getReason());
            return new MiniMapClickBatchResult(MiniMapClickBatchStatus.FAILED, null, cloudResult.getReason());
        }
        Map<String, String> fields = fieldsHolder[0];
        String status = fields.getOrDefault("status", "");
        String reason = fields.getOrDefault("reason", "");
        if ("NO_RESULT".equalsIgnoreCase(status)) {
            if (EXHAUSTED_REASON_CANDIDATES.equals(reason) || EXHAUSTED_REASON_TRANSFORM.equals(reason)) {
                return new MiniMapClickBatchResult(MiniMapClickBatchStatus.EXHAUSTED, null, reason);
            }
            return new MiniMapClickBatchResult(MiniMapClickBatchStatus.FAILED, null, reason);
        }
        String echoError = echoMismatch(fields, identity, request.getNavigationRequestId(), clientFrame);
        if (echoError == null
                && parseLong(fields.get("batchExpiresAtMs"), -1L) != request.getNavigationDeadlineMs()) {
            // The batch expiry is defined as the local navigation deadline echoed back. A different
            // value (schema drift, stale server) would either void the batch instantly — a hot
            // request loop until the 60s clock — or keep it alive past the deadline. Reject once.
            echoError = "batchExpiresAtMs";
        }
        if (echoError != null) {
            log.warn("cloud minimap click batch rejected by binding echo: source={} navigationRequestId={} mismatch={}",
                    request.getSource(), request.getNavigationRequestId(), echoError);
            return new MiniMapClickBatchResult(MiniMapClickBatchStatus.FAILED, null,
                    "binding-echo-mismatch:" + echoError);
        }
        List<MiniMapClickCandidate> candidates = parseCandidates(fields.get("candidates"));
        if (candidates.isEmpty()) {
            return new MiniMapClickBatchResult(MiniMapClickBatchStatus.FAILED, null,
                    "cloud batch HIT without parsable candidates");
        }
        MiniMapClickCandidateBatch batch = new MiniMapClickCandidateBatch(
                fields.getOrDefault("batchId", ""),
                request.getNavigationDeadlineMs(),
                identity.windowId(),
                identity.hwnd(),
                identity.taskRunId(),
                safe(request.getNavigationRequestId()),
                clientFrame,
                candidates);
        log.info("cloud minimap click batch received: source={} navigationRequestId={} batchId={} candidates={} attempted={} expiresAtMs={}",
                request.getSource(), request.getNavigationRequestId(), batch.batchId(),
                candidates.size(),
                request.getAttemptedCandidateIds() == null ? 0 : request.getAttemptedCandidateIds().size(),
                batch.batchExpiresAtMs());
        return new MiniMapClickBatchResult(MiniMapClickBatchStatus.BATCH, batch, reason);
    }

    /**
     * CR258: cloud verdict replacing local {@code CoordinateHelper.isLogicalCoordinatePlausible} on
     * production paths. Empty means the cloud call failed — callers stay fail-closed.
     */
    public Optional<Boolean> checkCoordinatePlausible(String mapName,
                                                      int logicalX,
                                                      int logicalY,
                                                      int marginPx,
                                                      String taskCode,
                                                      String source) {
        Identity identity = currentIdentity();
        if (identity == null || !isActive()) {
            return Optional.empty();
        }
        /*
         * CR258 review P1: scalar verdicts pass the same binding-echo gate as the click batch. A
         * well-formed HIT from a stale request or another window must not filter this window's
         * objective. The per-call request id rides the navigationRequestId wire field so the cloud
         * echo path is shared.
         */
        String pointRequestId = UUID.randomUUID().toString();
        String clientFrame = clientFrame();
        Map<String, String> context = new LinkedHashMap<>();
        context.put("operation", "CHECK_COORDINATE_PLAUSIBLE");
        context.put("mapName", safe(mapName));
        context.put("logicalX", Integer.toString(logicalX));
        context.put("logicalY", Integer.toString(logicalY));
        context.put("marginPx", Integer.toString(marginPx));
        context.put("windowId", identity.windowId());
        context.put("hwnd", identity.hwnd());
        context.put("taskRunId", identity.taskRunId());
        context.put("navigationRequestId", pointRequestId);
        context.put("clientFrame", clientFrame);
        context.put("source", safe(source));
        context.put("phase", "coordinate-plausible");
        Map<String, String>[] fieldsHolder = newFieldsHolder();
        CloudDecisionResult cloudResult = decide(context, taskCode, "coordinate-plausible",
                logicalX + "-" + logicalY, fieldsHolder);
        if (!cloudResult.isExecuted() || fieldsHolder[0] == null) {
            log.warn("cloud coordinate plausibility unavailable: source={} map={} coord=({}, {}) reason={}",
                    source, mapName, logicalX, logicalY, cloudResult.getReason());
            return Optional.empty();
        }
        Map<String, String> fields = fieldsHolder[0];
        String echoError = echoMismatch(fields, identity, pointRequestId, clientFrame);
        if (echoError != null) {
            log.warn("cloud coordinate plausibility rejected by binding echo: source={} map={} coord=({}, {}) mismatch={}",
                    source, mapName, logicalX, logicalY, echoError);
            return Optional.empty();
        }
        if (!"HIT".equalsIgnoreCase(fields.getOrDefault("status", ""))
                || !fields.containsKey("plausible")) {
            log.warn("cloud coordinate plausibility malformed: source={} map={} coord=({}, {}) decision={}",
                    source, mapName, logicalX, logicalY, fields);
            return Optional.empty();
        }
        return Optional.of(Boolean.parseBoolean(fields.get("plausible")));
    }

    /**
     * CR258: cloud evaluation replacing local {@code CoordinateHelper.calculateApproachCoordinate}
     * on production paths. Empty means the cloud call failed — callers stay fail-closed and must not
     * silently navigate to the original coordinate.
     */
    public Optional<MapCoordinate> resolveApproachCoordinate(String mapName,
                                                             int targetX,
                                                             int targetY,
                                                             String taskCode,
                                                             String source) {
        Identity identity = currentIdentity();
        if (identity == null || !isActive()) {
            return Optional.empty();
        }
        // CR258 review P1: same binding-echo gate as the click batch — a stale/foreign HIT must
        // not become this window's approach coordinate.
        String pointRequestId = UUID.randomUUID().toString();
        String clientFrame = clientFrame();
        Map<String, String> context = new LinkedHashMap<>();
        context.put("operation", "RESOLVE_APPROACH_COORDINATE");
        context.put("mapName", safe(mapName));
        context.put("targetX", Integer.toString(targetX));
        context.put("targetY", Integer.toString(targetY));
        context.put("windowId", identity.windowId());
        context.put("hwnd", identity.hwnd());
        context.put("taskRunId", identity.taskRunId());
        context.put("navigationRequestId", pointRequestId);
        context.put("clientFrame", clientFrame);
        context.put("source", safe(source));
        context.put("phase", "approach-coordinate");
        Map<String, String>[] fieldsHolder = newFieldsHolder();
        CloudDecisionResult cloudResult = decide(context, taskCode, "approach-coordinate",
                targetX + "-" + targetY, fieldsHolder);
        if (!cloudResult.isExecuted() || fieldsHolder[0] == null) {
            log.warn("cloud approach coordinate unavailable: source={} map={} target=({}, {}) reason={}",
                    source, mapName, targetX, targetY, cloudResult.getReason());
            return Optional.empty();
        }
        Map<String, String> fields = fieldsHolder[0];
        String echoError = echoMismatch(fields, identity, pointRequestId, clientFrame);
        if (echoError != null) {
            log.warn("cloud approach coordinate rejected by binding echo: source={} map={} target=({}, {}) mismatch={}",
                    source, mapName, targetX, targetY, echoError);
            return Optional.empty();
        }
        Integer approachX = parseInt(fields.get("approachX"));
        Integer approachY = parseInt(fields.get("approachY"));
        if (!"HIT".equalsIgnoreCase(fields.getOrDefault("status", "")) || approachX == null || approachY == null) {
            log.warn("cloud approach coordinate malformed: source={} map={} target=({}, {}) decision={}",
                    source, mapName, targetX, targetY, fields);
            return Optional.empty();
        }
        log.info("cloud approach coordinate resolved: source={} map={} target=({}, {}) approach=({}, {}) adjusted={} detail={}",
                source, mapName, targetX, targetY, approachX, approachY,
                fields.getOrDefault("adjusted", ""), fields.getOrDefault("detail", ""));
        return Optional.of(new MapCoordinate(approachX, approachY));
    }

    private CloudDecisionResult decide(Map<String, String> context,
                                       String taskCode,
                                       String phase,
                                       String traceSuffix,
                                       Map<String, String>[] fieldsHolder) {
        String normalizedTaskCode = taskCode == null || taskCode.isBlank() ? "navigation" : taskCode;
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.MINIMAP_LOCATION)
                .traceId(traceId(normalizedTaskCode, phase, context.get("windowId"), traceSuffix))
                .taskCode(normalizedTaskCode)
                .phase(phase)
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
                return serviceId == CloudDecisionServiceId.MINIMAP_LOCATION;
            }

            @Override
            public GateResult evaluate(CloudDecisionRequest request,
                                       CloudDecisionResponse response,
                                       String localDecision) {
                String decision = response == null ? null : response.getDecision();
                Map<String, String> fields = fields(decision);
                String status = fields.getOrDefault("status", "");
                if (!"HIT".equalsIgnoreCase(status) && !"NO_RESULT".equalsIgnoreCase(status)) {
                    return GateResult.rejected("unexpected navigation-point status: " + status);
                }
                fieldsHolder[0] = fields;
                return GateResult.accepted(decision, "navigation-point-" + status.toLowerCase(Locale.ROOT));
            }
        };
    }

    private record Identity(String windowId, String hwnd, String taskRunId) {
    }

    /**
     * Live identity snapshot. hwnd deliberately comes from the current native binding (not the task
     * start snapshot) so a rebound window invalidates in-flight batches, matching the
     * PreparedDialogAction binding checks. Missing identity is a structured failure — the wildcard
     * leniency of prepared actions is not extended to remote click points.
     */
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
                                String navigationRequestId,
                                String clientFrame) {
        // The cloud echo passes binding fields through its wire-safe token filter; apply the same
        // filter locally so the comparison checks identity, not encoding.
        if (!echoToken(identity.windowId()).equals(fields.get("windowId"))) {
            return "windowId";
        }
        if (!echoToken(identity.hwnd()).equals(fields.get("hwnd"))) {
            return "hwnd";
        }
        if (!echoToken(identity.taskRunId()).equals(fields.get("taskRunId"))) {
            return "taskRunId";
        }
        if (!echoToken(safe(navigationRequestId)).equals(fields.get("navigationRequestId"))) {
            return "navigationRequestId";
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

    private static List<MiniMapClickCandidate> parseCandidates(String packed) {
        List<MiniMapClickCandidate> candidates = new ArrayList<>();
        if (packed == null || packed.isBlank()) {
            return candidates;
        }
        for (String part : packed.split("\\|")) {
            String[] fields = part.split(":", CANDIDATE_WIRE_FIELDS);
            if (fields.length < CANDIDATE_WIRE_FIELDS) {
                return List.of();
            }
            Integer logicalX = parseInt(fields[2]);
            Integer logicalY = parseInt(fields[3]);
            Integer baseRelX = parseInt(fields[4]);
            Integer baseRelY = parseInt(fields[5]);
            Integer relX = parseInt(fields[6]);
            Integer relY = parseInt(fields[7]);
            Integer jitterX = parseInt(fields[8]);
            Integer jitterY = parseInt(fields[9]);
            Integer cursor = parseInt(fields[10]);
            if (fields[0].isBlank() || fields[1].isBlank() || logicalX == null || logicalY == null
                    || baseRelX == null || baseRelY == null || relX == null || relY == null
                    || jitterX == null || jitterY == null || cursor == null) {
                return List.of();
            }
            candidates.add(new MiniMapClickCandidate(fields[0], fields[1], logicalX, logicalY,
                    baseRelX, baseRelY, relX, relY, jitterX, jitterY, cursor, fields[11]));
        }
        return candidates;
    }

    private static String traceId(String taskCode, String phase, String windowId, String suffix) {
        return sanitizeTracePart("minimap-point:" + taskCode + ":" + phase + ":"
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

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
