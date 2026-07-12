package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionMode;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.model.MapCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class MiniMapLocationCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(MiniMapLocationCloudDecisionService.class);
    private static final String DEFAULT_TASK_CODE = "unknown";
    private static final String DEFAULT_PHASE = "minimap-location";
    private static final String LOCAL_SHADOW_DECISION = "status=LOCAL_SHADOW;reason=local-comparator-not-executed";
    private static final String STATUS_HIT = "HIT";
    private static final String STATUS_NO_RESULT = "NO_RESULT";
    private static final double MIN_EXECUTE_CONFIDENCE = 0.50d;
    private static final double MIN_LOCATION_SCORE = 0.0d;
    private static final int MAX_REASONABLE_COORDINATE = 999;

    private final CloudDecisionCoordinator coordinator;

    public MiniMapLocationCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isActive() {
        return coordinator.isActive(CloudDecisionServiceId.MINIMAP_LOCATION);
    }

    /**
     * Recognizes a mini-map coordinate strip or map-label image through cloud-required vision.
     *
     * @param request raw PNG payload plus operation and safety metadata. `requiresCoordinate` and
     *                `requiresMapName` select the caller's fail-closed contract for the public
     *                `MiniMapCoordinateReader` API currently being served.
     * @return cloud hit/no-result, disabled, or required failure. Required failures must not be
     *         followed by local digit/template recognition.
     */
    public MiniMapLocationCloudDecision recognize(MiniMapLocationCloudRequest request) {
        if (!coordinator.isActive(CloudDecisionServiceId.MINIMAP_LOCATION)) {
            return MiniMapLocationCloudDecision.builder()
                    .status(MiniMapLocationCloudDecision.Status.DISABLED)
                    .reason("service disabled")
                    .build();
        }
        String validationError = requestValidationError(request);
        if (validationError != null) {
            return requiredFailure(null, validationError);
        }

        String taskCode = normalize(request.getTaskCode(), DEFAULT_TASK_CODE);
        String phase = normalize(request.getPhase(), DEFAULT_PHASE);
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.MINIMAP_LOCATION)
                .traceId(traceId(request, taskCode, phase))
                .taskCode(taskCode)
                .phase(phase)
                .windowId(request.getWindowId())
                .taskRunId(request.getTaskRunId())
                .policyVersion(request.getPolicyVersion())
                .localDecision(LOCAL_SHADOW_DECISION)
                .context(context(request, taskCode, phase))
                .build();

        ParseResult[] parsedHolder = new ParseResult[1];
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                LOCAL_SHADOW_DECISION,
                miniMapExecutionGate(request, parsedHolder));
        if (cloudResult.isExecuted()) {
            ParseResult parsed = parsedHolder[0];
            if (parsed.noResult()) {
                Map<String, String> missDiagnostics = diagnostics(cloudResult.getResponse());
                return MiniMapLocationCloudDecision.builder()
                        .status(MiniMapLocationCloudDecision.Status.CLOUD_NO_RESULT)
                        .reason(parsed.reason())
                        .ocrFallbackReason(missDiagnostics.get("ocrFallbackReason"))
                        .ocrRejectedLocation(missDiagnostics.get("ocrRejectedLocation"))
                        .confidence(cloudResult.getResponse().getConfidence())
                        .cloudResult(cloudResult)
                        .build();
            }
            CloudDecisionResponse response = cloudResult.getResponse();
            Map<String, String> fields = fields(response.getDecision());
            Map<String, String> diagnostics = diagnostics(response);
            return MiniMapLocationCloudDecision.builder()
                    .status(MiniMapLocationCloudDecision.Status.CLOUD_EXECUTED)
                    .coordinate(parsed.coordinate())
                    .mapName(parsed.mapName())
                    .score(parsed.score())
                    .reason(firstText(fields.get("reason"), cloudResult.getReason()))
                    .debugToken(diagnostics.get("debugToken"))
                    .labelPath(diagnostics.get("labelPath"))
                    .labelImagePayloadBase64(diagnostics.get("labelImagePayloadBase64"))
                    .labelPayloadMimeType(diagnostics.get("labelPayloadMimeType"))
                    .labelImageSha256(diagnostics.get("labelImageSha256"))
                    .labelWidth(parsePositiveInt(diagnostics.get("labelWidth"), 0))
                    .labelHeight(parsePositiveInt(diagnostics.get("labelHeight"), 0))
                    .confidence(response.getConfidence())
                    .cloudResult(cloudResult)
                    .build();
        }

        if (cloudResult.getMode() == CloudDecisionMode.DISABLED) {
            return MiniMapLocationCloudDecision.builder()
                    .status(MiniMapLocationCloudDecision.Status.DISABLED)
                    .reason(cloudResult.getReason())
                    .cloudResult(cloudResult)
                    .build();
        }

        log.warn("cloud.execute serviceId=MINIMAP_LOCATION accepted=false taskCode={} phase={} reason={} cloudDecision={}",
                cloudRequest.getTaskCode(),
                cloudRequest.getPhase(),
                cloudResult.getReason(),
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision());
        return requiredFailure(cloudResult, cloudResult.getReason());
    }

    private CloudDecisionExecutionGate miniMapExecutionGate(
            MiniMapLocationCloudRequest miniMapRequest,
            ParseResult[] parsedHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.MINIMAP_LOCATION;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                if (response.getConfidence() < MIN_EXECUTE_CONFIDENCE) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "low confidence: " + response.getConfidence());
                }
                ParseResult parsed = parse(miniMapRequest, response);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                parsedHolder[0] = parsed;
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(),
                        parsed.noResult()
                                ? "execute percent gate hit; status=NO_RESULT"
                                : "execute percent gate hit; using MINIMAP_LOCATION cloud result");
            }
        };
    }

    private static ParseResult parse(MiniMapLocationCloudRequest request, CloudDecisionResponse response) {
        Map<String, String> fields = fields(response.getDecision());
        String status = upper(fields.get("status"));
        if (STATUS_NO_RESULT.equals(status)) {
            return ParseResult.noResult(firstText(fields.get("reason"), "status=NO_RESULT"));
        }
        if (!STATUS_HIT.equals(status)) {
            return ParseResult.rejected("status must be HIT or NO_RESULT");
        }

        MapCoordinate coordinate = null;
        if (request.isRequiresCoordinate()) {
            String xText = fields.get("x");
            String yText = fields.get("y");
            if (!hasText(xText) || !hasText(yText)) {
                return ParseResult.rejected("coordinate x/y are required");
            }
            Integer x = parseCoordinatePart(xText);
            Integer y = parseCoordinatePart(yText);
            if (x == null || y == null) {
                return ParseResult.rejected("coordinate outside reasonable bounds: x="
                        + safe(xText) + " y=" + safe(yText));
            }
            coordinate = new MapCoordinate(x, y);
        } else {
            Integer x = parseCoordinatePart(fields.get("x"));
            Integer y = parseCoordinatePart(fields.get("y"));
            if (x != null && y != null) {
                coordinate = new MapCoordinate(x, y);
            }
        }

        String mapName = normalize(fields.get("mapName"), null);
        if (request.isRequiresMapName() && !hasText(mapName)) {
            return ParseResult.rejected("mapName is required");
        }
        double score = parseScore(fields.get("score"));
        if (score < MIN_LOCATION_SCORE) {
            return ParseResult.rejected("score below minimum: " + score);
        }
        return ParseResult.hit(coordinate, mapName, score, firstText(fields.get("reason"), "cloud hit"));
    }

    private static String requestValidationError(MiniMapLocationCloudRequest request) {
        if (request == null || request.getOperation() == null) {
            return "missing minimap location request/operation";
        }
        if (!hasText(request.getImagePayloadBase64())) {
            return "missing transferable image payload: imagePayloadBase64 is required";
        }
        if (!hasText(request.getPayloadMimeType())) {
            return "missing transferable image payload metadata: payloadMimeType is required";
        }
        if (!hasText(request.getImageSha256())) {
            return "missing transferable image payload metadata: imageSha256 is required";
        }
        if (request.getWindowWidth() <= 0 || request.getWindowHeight() <= 0) {
            return "window size must be explicit positive pixels";
        }
        MiniMapLocationCloudRequest.Roi roi = request.getRoi();
        if (roi == null) {
            return "ROI is required for minimap location recognition";
        }
        if (roi.getX() < 0 || roi.getY() < 0 || roi.getWidth() <= 0 || roi.getHeight() <= 0) {
            return "ROI must be window-relative pixels with non-negative origin and positive size";
        }
        long maxX = (long) roi.getX() + roi.getWidth();
        long maxY = (long) roi.getY() + roi.getHeight();
        if (maxX > request.getWindowWidth() || maxY > request.getWindowHeight()) {
            return "ROI outside window: roi=" + roiText(roi)
                    + " window=0,0," + request.getWindowWidth() + "," + request.getWindowHeight();
        }
        return null;
    }

    private static Map<String, String> context(
            MiniMapLocationCloudRequest request,
            String taskCode,
            String phase) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("hook", "minimap-location");
        context.put("operation", request.getOperation().name());
        context.put("source", safe(request.getSource()));
        context.put("phase", phase);
        context.put("taskCode", taskCode);
        context.put("windowId", safe(request.getWindowId()));
        context.put("taskRunId", safe(request.getTaskRunId()));
        context.put("policyVersion", safe(request.getPolicyVersion()));
        context.put("hwnd", safe(request.getHwnd()));
        context.put("mapNameHint", safe(request.getMapNameHint()));
        context.put("requiresCoordinate", Boolean.toString(request.isRequiresCoordinate()));
        context.put("requiresMapName", Boolean.toString(request.isRequiresMapName()));
        context.put("imagePayloadBase64", request.getImagePayloadBase64().trim());
        context.put("payloadMimeType", request.getPayloadMimeType().trim());
        context.put("imageSha256", request.getImageSha256().trim());
        context.put("rawImagePath", safe(request.getRawImagePath()));
        context.put("debugImageId", safe(request.getDebugImageId()));
        context.put("windowSize", request.getWindowWidth() + "," + request.getWindowHeight());
        context.put("roi", roiText(request.getRoi()));
        return Map.copyOf(context);
    }

    private static MiniMapLocationCloudDecision requiredFailure(CloudDecisionResult cloudResult, String reason) {
        return MiniMapLocationCloudDecision.builder()
                .status(MiniMapLocationCloudDecision.Status.REQUIRED_FAILURE)
                .reason(safe(reason))
                .cloudResult(cloudResult)
                .build();
    }

    private static String traceId(MiniMapLocationCloudRequest request, String taskCode, String phase) {
        return "minimap-location:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(phase)
                + ":" + safeTracePart(request.getOperation().name())
                + ":" + safeTracePart(request.getWindowId())
                + ":" + safeTracePart(firstText(request.getDebugImageId(), request.getRawImagePath()));
    }

    private static Map<String, String> diagnostics(CloudDecisionResponse response) {
        return response == null || response.getDiagnostics() == null ? Map.of() : response.getDiagnostics();
    }

    private static Map<String, String> fields(String decision) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!hasText(decision)) {
            return result;
        }
        for (String part : decision.split(";", -1)) {
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

    private static Integer parseCoordinatePart(String value) {
        Integer parsed = parseNonNegativeInt(value);
        return parsed != null && parsed >= 0 && parsed <= MAX_REASONABLE_COORDINATE ? parsed : null;
    }

    private static Integer parseNonNegativeInt(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseScore(String value) {
        if (!hasText(value)) {
            return 0.0d;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0d;
        }
    }

    private static String roiText(MiniMapLocationCloudRequest.Roi roi) {
        if (roi == null) {
            return "";
        }
        return roi.getX() + "," + roi.getY() + "," + roi.getWidth() + "," + roi.getHeight();
    }

    private static String firstText(String first, String second) {
        return hasText(first) ? first : safe(second);
    }

    private static String normalize(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private static String upper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static String safeTracePart(String value) {
        return normalize(value, "unknown").replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ParseResult(
            boolean accepted,
            boolean noResult,
            MapCoordinate coordinate,
            String mapName,
            double score,
            String reason) {
        static ParseResult hit(MapCoordinate coordinate, String mapName, double score, String reason) {
            return new ParseResult(true, false, coordinate, mapName, score, reason);
        }

        static ParseResult noResult(String reason) {
            return new ParseResult(true, true, null, null, 0.0d, reason);
        }

        static ParseResult rejected(String reason) {
            return new ParseResult(false, false, null, null, 0.0d, reason);
        }
    }
}
