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

@Service
public class SheyaoxiangStatusCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(SheyaoxiangStatusCloudDecisionService.class);
    private static final String DEFAULT_TASK_CODE = "unknown";
    private static final String DEFAULT_PHASE = "sheyaoxiang-status";
    private static final String LOCAL_FAIL_CLOSED_DECISION = "action=FAIL_CLOSED;reason=local-no-fallback";

    private final CloudDecisionCoordinator coordinator;

    public SheyaoxiangStatusCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isActive() {
        return coordinator.isActive(CloudDecisionServiceId.SHEYAOXIANG_STATUS);
    }

    /**
     * Ask the cloud-owned incense policy/recognizer what the local runner should do next.
     *
     * @param request tick, status-image, or outcome report. Image requests must carry a raw PNG
     *                status-bar ROI; tick/outcome requests intentionally carry facts only.
     * @return a parsed cloud command. Required failures are fail-closed and must not be followed by
     *         local OCR, local template learning, or local "guess and refill" behavior.
     */
    public SheyaoxiangStatusCloudDecision decide(SheyaoxiangStatusCloudRequest request) {
        if (!coordinator.isActive(CloudDecisionServiceId.SHEYAOXIANG_STATUS)) {
            return SheyaoxiangStatusCloudDecision.builder()
                    .status(SheyaoxiangStatusCloudDecision.Status.DISABLED)
                    .action(SheyaoxiangStatusCloudDecision.Action.FAIL_CLOSED)
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
                .serviceId(CloudDecisionServiceId.SHEYAOXIANG_STATUS)
                .traceId(traceId(request, taskCode, phase))
                .taskCode(taskCode)
                .phase(phase)
                .windowId(normalize(request.getWindowId(), null))
                .taskRunId(normalize(request.getTaskRunId(), null))
                .policyVersion(normalize(request.getPolicyVersion(), null))
                .localDecision(LOCAL_FAIL_CLOSED_DECISION)
                .context(context(request, taskCode, phase))
                .build();

        ParseResult[] parsedHolder = new ParseResult[1];
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                LOCAL_FAIL_CLOSED_DECISION,
                executionGate(parsedHolder));
        if (cloudResult.isExecuted()) {
            ParseResult parsed = parsedHolder[0];
            if (parsed == null) {
                return requiredFailure(cloudResult, "missing parsed SHEYAOXIANG_STATUS result");
            }
            return SheyaoxiangStatusCloudDecision.builder()
                    .status(SheyaoxiangStatusCloudDecision.Status.CLOUD_EXECUTED)
                    .action(parsed.action())
                    .present(parsed.present())
                    .remainingMs(parsed.remainingMs())
                    .remainingSource(parsed.remainingSource())
                    .iconBox(parsed.iconBox())
                    .text(parsed.text())
                    .confidence(parsed.confidence())
                    .reason(parsed.reason())
                    .decisionId(parsed.decisionId())
                    .cloudResult(cloudResult)
                    .build();
        }

        if (cloudResult.getMode() == CloudDecisionMode.DISABLED) {
            return SheyaoxiangStatusCloudDecision.builder()
                    .status(SheyaoxiangStatusCloudDecision.Status.DISABLED)
                    .action(SheyaoxiangStatusCloudDecision.Action.FAIL_CLOSED)
                    .reason(cloudResult.getReason())
                    .cloudResult(cloudResult)
                    .build();
        }

        log.warn("cloud.execute serviceId=SHEYAOXIANG_STATUS accepted=false taskCode={} phase={} reason={} cloudDecision={}",
                cloudRequest.getTaskCode(),
                cloudRequest.getPhase(),
                cloudResult.getReason(),
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision());
        return requiredFailure(cloudResult, cloudResult.getReason());
    }

    private CloudDecisionExecutionGate executionGate(ParseResult[] parsedHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.SHEYAOXIANG_STATUS;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                ParseResult parsed = parse(response);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                parsedHolder[0] = parsed;
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(),
                        "execute percent gate hit; using SHEYAOXIANG_STATUS action=" + parsed.action());
            }
        };
    }

    private static ParseResult parse(CloudDecisionResponse response) {
        Map<String, String> fields = fields(response.getDecision());
        Map<String, String> diagnostics = diagnostics(response);
        SheyaoxiangStatusCloudDecision.Action action =
                parseAction(firstText(fields.get("action"), diagnostics.get("action")));
        if (action == null) {
            return ParseResult.rejected("action is required");
        }
        String decisionId = firstText(fields.get("decisionId"), diagnostics.get("decisionId"));
        if (action == SheyaoxiangStatusCloudDecision.Action.USE_INCENSE && !hasText(decisionId)) {
            return ParseResult.rejected("USE_INCENSE requires decisionId");
        }
        SheyaoxiangStatusCloudDecision.Present present =
                parsePresent(firstText(fields.get("present"), diagnostics.get("present")));
        Long remainingMs = parseNonNegativeLong(firstText(fields.get("remainingMs"), diagnostics.get("remainingMs")));
        SheyaoxiangStatusCloudDecision.Box iconBox =
                parseBox(firstText(fields.get("iconBox"), diagnostics.get("iconBox")));
        double confidence = parseDouble(firstText(fields.get("confidence"), diagnostics.get("confidence")),
                response.getConfidence());
        return ParseResult.accepted(
                action,
                present,
                remainingMs,
                firstText(fields.get("remainingSource"), diagnostics.get("remainingSource")),
                iconBox,
                firstText(fields.get("text"), diagnostics.get("text")),
                confidence,
                firstText(fields.get("reason"), diagnostics.get("reason")),
                decisionId);
    }

    private static String requestValidationError(SheyaoxiangStatusCloudRequest request) {
        if (request == null || request.getHook() == null) {
            return "missing sheyaoxiang request/hook";
        }
        if (request.getHook() == SheyaoxiangStatusCloudRequest.Hook.STATUS_IMAGE) {
            if (!hasText(request.getImagePayloadBase64())) {
                return "imagePayloadBase64 is required for STATUS_IMAGE";
            }
            if (!"image/png".equals(request.getPayloadMimeType())) {
                return "payloadMimeType must be image/png";
            }
            if (!hasText(request.getImageSha256())) {
                return "imageSha256 is required for STATUS_IMAGE";
            }
            if (request.getWindowRelativeRoi() == null || request.getScreenAbsoluteRoi() == null) {
                return "windowRelativeRoi and screenAbsoluteRoi are required for STATUS_IMAGE";
            }
        }
        if (request.getHook() == SheyaoxiangStatusCloudRequest.Hook.OUTCOME
                && request.getOutcome() == null) {
            return "outcome is required for OUTCOME hook";
        }
        return null;
    }

    private static Map<String, String> context(
            SheyaoxiangStatusCloudRequest request,
            String taskCode,
            String phase) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("hook", request.getHook().name());
        context.put("taskCode", taskCode);
        context.put("phase", phase);
        context.put("source", safe(request.getSource()));
        context.put("windowId", safe(request.getWindowId()));
        context.put("taskRunId", safe(request.getTaskRunId()));
        context.put("policyVersion", safe(request.getPolicyVersion()));
        context.put("hwnd", safe(request.getHwnd()));
        context.put("nowMs", Long.toString(request.getNowMs()));
        context.put("lastIncenseUsedTimeMs", Long.toString(request.getLastIncenseUsedTimeMs()));
        context.put("nextIncenseRetryTimeMs", Long.toString(request.getNextIncenseRetryTimeMs()));
        context.put("incenseIconOffset", request.getIncenseIconOffsetX() + "," + request.getIncenseIconOffsetY());
        context.put("openMainBagSession", Boolean.toString(request.isOpenMainBagSession()));
        if (request.getOutcome() != null) {
            context.put("outcome", request.getOutcome().name());
        }
        context.put("decisionId", safe(request.getDecisionId()));
        context.put("reason", safe(request.getReason()));
        if (request.getWindowRelativeRoi() != null) {
            context.put("windowRelativeRoi", roiText(request.getWindowRelativeRoi()));
        }
        if (request.getScreenAbsoluteRoi() != null) {
            context.put("screenAbsoluteRoi", roiText(request.getScreenAbsoluteRoi()));
        }
        context.put("windowSize", request.getWindowWidth() + "," + request.getWindowHeight());
        if (hasText(request.getImagePayloadBase64())) {
            context.put("imagePayloadBase64", request.getImagePayloadBase64().trim());
            context.put("payloadMimeType", safe(request.getPayloadMimeType()));
            context.put("imageSha256", safe(request.getImageSha256()));
            context.put("rawImagePath", safe(request.getRawImagePath()));
        }
        return Map.copyOf(context);
    }

    private static SheyaoxiangStatusCloudDecision requiredFailure(
            CloudDecisionResult cloudResult,
            String reason) {
        return SheyaoxiangStatusCloudDecision.builder()
                .status(SheyaoxiangStatusCloudDecision.Status.REQUIRED_FAILURE)
                .action(SheyaoxiangStatusCloudDecision.Action.FAIL_CLOSED)
                .present(SheyaoxiangStatusCloudDecision.Present.UNKNOWN)
                .reason(safe(reason))
                .cloudResult(cloudResult)
                .build();
    }

    private static String traceId(SheyaoxiangStatusCloudRequest request, String taskCode, String phase) {
        return "sheyaoxiang-status:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(phase)
                + ":" + safeTracePart(request.getHook().name())
                + ":" + safeTracePart(request.getWindowId())
                + ":" + safeTracePart(firstText(request.getImageSha256(), request.getDecisionId()));
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

    private static Map<String, String> diagnostics(CloudDecisionResponse response) {
        return response == null || response.getDiagnostics() == null ? Map.of() : response.getDiagnostics();
    }

    private static SheyaoxiangStatusCloudDecision.Action parseAction(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return SheyaoxiangStatusCloudDecision.Action.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static SheyaoxiangStatusCloudDecision.Present parsePresent(String value) {
        String upper = hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
        return switch (upper) {
            case "TRUE", "PRESENT" -> SheyaoxiangStatusCloudDecision.Present.TRUE;
            case "FALSE", "ABSENT" -> SheyaoxiangStatusCloudDecision.Present.FALSE;
            default -> SheyaoxiangStatusCloudDecision.Present.UNKNOWN;
        };
    }

    private static SheyaoxiangStatusCloudDecision.Box parseBox(String value) {
        int[] parts = parseInts(value, 4);
        if (parts == null || parts[2] <= 0 || parts[3] <= 0) {
            return null;
        }
        return SheyaoxiangStatusCloudDecision.Box.builder()
                .x(parts[0])
                .y(parts[1])
                .width(parts[2])
                .height(parts[3])
                .build();
    }

    private static Long parseNonNegativeLong(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed >= 0L ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double parseDouble(String value, double fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int[] parseInts(String value, int expected) {
        if (!hasText(value)) {
            return null;
        }
        String[] parts = value.split(",", -1);
        if (parts.length != expected) {
            return null;
        }
        int[] result = new int[expected];
        try {
            for (int i = 0; i < expected; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String roiText(SheyaoxiangStatusCloudRequest.Roi roi) {
        return roi.getX() + "," + roi.getY() + "," + roi.getWidth() + "," + roi.getHeight();
    }

    private static String firstText(String first, String second) {
        return hasText(first) ? first.trim() : hasText(second) ? second.trim() : "";
    }

    private static String normalize(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private static String safeTracePart(String value) {
        return normalize(value, "unknown").replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ParseResult(
            boolean accepted,
            SheyaoxiangStatusCloudDecision.Action action,
            SheyaoxiangStatusCloudDecision.Present present,
            Long remainingMs,
            String remainingSource,
            SheyaoxiangStatusCloudDecision.Box iconBox,
            String text,
            double confidence,
            String reason,
            String decisionId) {

        static ParseResult accepted(
                SheyaoxiangStatusCloudDecision.Action action,
                SheyaoxiangStatusCloudDecision.Present present,
                Long remainingMs,
                String remainingSource,
                SheyaoxiangStatusCloudDecision.Box iconBox,
                String text,
                double confidence,
                String reason,
                String decisionId) {
            return new ParseResult(true, action, present, remainingMs, remainingSource,
                    iconBox, text, confidence, reason, decisionId);
        }

        static ParseResult rejected(String reason) {
            return new ParseResult(false, SheyaoxiangStatusCloudDecision.Action.FAIL_CLOSED,
                    SheyaoxiangStatusCloudDecision.Present.UNKNOWN, null, "", null,
                    "", 0.0d, reason, "");
        }
    }
}
