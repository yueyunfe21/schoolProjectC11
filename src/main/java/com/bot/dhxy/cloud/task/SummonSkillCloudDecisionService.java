package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionMode;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class SummonSkillCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(SummonSkillCloudDecisionService.class);
    private static final String DEFAULT_TASK_CODE = "maintenance";
    private static final String DEFAULT_PHASE = "summon-skill-slot-status";
    private static final String LOCAL_SHADOW_DECISION = "slotStatus=UNKNOWN;action=RETRY;reason=local-shadow";
    private static final double MIN_EXECUTE_CONFIDENCE = 0.70d;

    private final CloudDecisionCoordinator coordinator;

    public SummonSkillCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isActive() {
        return coordinator.isActive(CloudDecisionServiceId.SUMMON_SKILL);
    }

    /**
     * Classify a summon-skill hover tooltip or static slot ROI through cloud-required recognition.
     *
     * @param request raw tooltip or static slot image payload and window-relative ROI metadata. The payload fields
     *                are the production transport contract; local paths are diagnostic references
     *                only.
     * @return accepted cloud slot status, disabled state, or `UNKNOWN` required failure. Required
     * failures must not be followed by local yellow washing or template fallback.
     */
    public SummonSkillCloudDecision inspectCurrentHoverTip(SummonSkillCloudRequest request) {
        if (!coordinator.isActive(CloudDecisionServiceId.SUMMON_SKILL)) {
            return SummonSkillCloudDecision.builder()
                    .status(SummonSkillCloudDecision.Status.DISABLED)
                    .slotStatus(SummonSkillSlotStatus.UNKNOWN)
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
                .serviceId(CloudDecisionServiceId.SUMMON_SKILL)
                .traceId(traceId(request, taskCode, phase))
                .taskCode(taskCode)
                .phase(phase)
                .windowId(request.getWindowId())
                .taskRunId(request.getTaskRunId())
                .policyVersion(request.getPolicyVersion())
                .localDecision(LOCAL_SHADOW_DECISION)
                .context(context(request, taskCode, phase))
                .build();

        SlotParseResult[] parsedHolder = new SlotParseResult[1];
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                LOCAL_SHADOW_DECISION,
                summonSkillExecutionGate(parsedHolder));
        if (cloudResult.isExecuted()) {
            SlotParseResult parsed = parsedHolder[0];
            Map<String, String> fields = fields(cloudResult.getResponse().getDecision());
            return SummonSkillCloudDecision.builder()
                    .status(SummonSkillCloudDecision.Status.CLOUD_EXECUTED)
                    .slotStatus(parsed.slotStatus())
                    .action(normalize(fields.get("action"), null))
                    .reason(firstText(fields.get("reason"), cloudResult.getReason()))
                    .confidence(cloudResult.getResponse().getConfidence())
                    .debugToken(debugToken(cloudResult.getResponse()))
                    .cloudResult(cloudResult)
                    .build();
        }

        if (cloudResult.getMode() == CloudDecisionMode.DISABLED) {
            return SummonSkillCloudDecision.builder()
                    .status(SummonSkillCloudDecision.Status.DISABLED)
                    .slotStatus(SummonSkillSlotStatus.UNKNOWN)
                    .reason(cloudResult.getReason())
                    .cloudResult(cloudResult)
                    .build();
        }

        log.warn("cloud.execute serviceId=SUMMON_SKILL accepted=false taskCode={} phase={} reason={} cloudDecision={}",
                cloudRequest.getTaskCode(),
                cloudRequest.getPhase(),
                cloudResult.getReason(),
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision());
        return requiredFailure(cloudResult, cloudResult.getReason());
    }

    private CloudDecisionExecutionGate summonSkillExecutionGate(SlotParseResult[] parsedHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.SUMMON_SKILL;
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
                SlotParseResult parsed = parse(response);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                parsedHolder[0] = parsed;
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(),
                        "execute percent gate hit; using summon skill cloud slot status");
            }
        };
    }

    private static SlotParseResult parse(CloudDecisionResponse response) {
        Map<String, String> fields = fields(response.getDecision());
        String slotStatusText = upper(fields.get("slotStatus"));
        if (!hasText(slotStatusText)) {
            return SlotParseResult.rejected("slotStatus is required");
        }
        try {
            return SlotParseResult.accepted(SummonSkillSlotStatus.valueOf(slotStatusText));
        } catch (IllegalArgumentException e) {
            return SlotParseResult.rejected("unsupported slotStatus: " + slotStatusText);
        }
    }

    private static Map<String, String> context(SummonSkillCloudRequest request, String taskCode, String phase) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("hook", "summon-skill-slot-status");
        context.put("source", safe(request.getSource()));
        context.put("phase", phase);
        context.put("taskCode", taskCode);
        context.put("windowId", safe(request.getWindowId()));
        context.put("taskRunId", safe(request.getTaskRunId()));
        context.put("policyVersion", safe(request.getPolicyVersion()));
        context.put("hwnd", safe(request.getHwnd()));
        context.put("slotIndex", request.getSlotIndex() == null ? "" : request.getSlotIndex().toString());
        context.put("imagePayloadBase64", request.getImagePayloadBase64().trim());
        context.put("payloadMimeType", request.getPayloadMimeType().trim());
        context.put("imageSha256", request.getImageSha256().trim());
        context.put("rawImagePath", safe(request.getRawImagePath()));
        context.put("debugImageId", safe(request.getDebugImageId()));
        context.put("windowSize", request.getWindowWidth() + "," + request.getWindowHeight());
        context.put("roi", roiText(request.getRoi()));
        return Map.copyOf(context);
    }

    private static String requestValidationError(SummonSkillCloudRequest request) {
        if (request == null) {
            return "missing summon skill cloud request";
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
        SummonSkillCloudRequest.Roi roi = request.getRoi();
        if (roi == null) {
            return "ROI is required for summon skill tooltip recognition";
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

    private static SummonSkillCloudDecision requiredFailure(CloudDecisionResult cloudResult, String reason) {
        return SummonSkillCloudDecision.builder()
                .status(SummonSkillCloudDecision.Status.REQUIRED_FAILURE)
                .slotStatus(SummonSkillSlotStatus.UNKNOWN)
                .reason(safe(reason))
                .cloudResult(cloudResult)
                .build();
    }

    private static String traceId(SummonSkillCloudRequest request, String taskCode, String phase) {
        return "summon-skill:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(phase)
                + ":" + safeTracePart(request.getWindowId())
                + ":" + safeTracePart(request.getDebugImageId());
    }

    private static String debugToken(CloudDecisionResponse response) {
        return response.getDiagnostics() == null ? null : response.getDiagnostics().get("debugToken");
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

    private static String roiText(SummonSkillCloudRequest.Roi roi) {
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

    private record SlotParseResult(boolean accepted, SummonSkillSlotStatus slotStatus, String reason) {
        static SlotParseResult accepted(SummonSkillSlotStatus slotStatus) {
            return new SlotParseResult(true, slotStatus, null);
        }

        static SlotParseResult rejected(String reason) {
            return new SlotParseResult(false, SummonSkillSlotStatus.UNKNOWN, reason);
        }
    }
}
