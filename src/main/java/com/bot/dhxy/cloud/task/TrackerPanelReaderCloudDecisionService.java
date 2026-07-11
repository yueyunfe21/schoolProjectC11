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

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TrackerPanelReaderCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(TrackerPanelReaderCloudDecisionService.class);
    private static final String DEFAULT_PHASE = "tracker-panel-reader";
    private static final String LOCAL_SHADOW_DECISION = "NO_ACTION";
    private static final String ACTION_CLICK_TRACKER_LINK = "CLICK_TRACKER_LINK";
    private static final String ACTION_NO_ACTION = "NO_ACTION";
    private static final String ACTION_REROLL = "REROLL";
    private static final String STATUS_FOUND = "FOUND";
    private static final String STATUS_NOT_FOUND = "NOT_FOUND";
    private static final String STATUS_AMBIGUOUS = "AMBIGUOUS";
    private static final String STATUS_ERROR = "ERROR";
    private static final String COORDINATE_SPACE_WINDOW_RELATIVE = "WINDOW_RELATIVE";
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;

    private final CloudDecisionCoordinator coordinator;

    public TrackerPanelReaderCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isActive() {
        return coordinator.isActive(CloudDecisionServiceId.TRACKER_PANEL_READER);
    }

    /**
     * Reads the left task tracker through cloud-owned image parsing.
     *
     * @param request raw PNG tracker crop plus its window-relative origin. Local code must not
     *                derive green-link click points from this image when cloud execute is required;
     *                it only validates and consumes the cloud window-relative result.
     * @return cloud click/no-action or required failure. Required failures are explicit no-click
     *         outcomes so callers cannot silently fall back to local green-link scanning.
     */
    public TrackerPanelReaderCloudDecision read(TrackerPanelReaderCloudRequest request) {
        if (!coordinator.isActive(CloudDecisionServiceId.TRACKER_PANEL_READER)) {
            return TrackerPanelReaderCloudDecision.builder()
                    .status(TrackerPanelReaderCloudDecision.Status.DISABLED)
                    .action(ACTION_NO_ACTION)
                    .reason("service disabled")
                    .build();
        }
        String validationError = requestValidationError(request);
        if (validationError != null) {
            return requiredFailure(null, validationError);
        }

        String taskCode = normalizeTaskCode(request.getTaskCode());
        String phase = normalize(request.getPhase(), DEFAULT_PHASE);
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.TRACKER_PANEL_READER)
                .traceId(traceId(request, taskCode, phase))
                .taskCode(taskCode)
                .phase(phase)
                .localDecision(LOCAL_SHADOW_DECISION)
                .context(context(request, taskCode, phase))
                .build();

        ParseResult[] parsedHolder = new ParseResult[1];
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                LOCAL_SHADOW_DECISION,
                executionGate(taskCode, parsedHolder));
        if (cloudResult.isExecuted()) {
            ParseResult parsed = parsedHolder[0];
            if (parsed == null) {
                return requiredFailure(cloudResult, "missing parsed TRACKER_PANEL_READER result");
            }
            if (parsed.noAction()) {
                return TrackerPanelReaderCloudDecision.builder()
                        .status(TrackerPanelReaderCloudDecision.Status.CLOUD_NO_ACTION)
                        .action(parsed.action())
                        .reason(parsed.reason())
                        .taskKey(parsed.taskKey())
                        .targetName(parsed.targetName())
                        .yellowText(parsed.yellowText())
                        .links(parsed.links())
                        .cloudResult(cloudResult)
                        .build();
            }
            return TrackerPanelReaderCloudDecision.builder()
                    .status(TrackerPanelReaderCloudDecision.Status.CLOUD_FOUND)
                    .action(parsed.action())
                    .taskKey(parsed.taskKey())
                    .targetName(parsed.targetName())
                    .yellowText(parsed.yellowText())
                    .clickWindowRelative(parsed.click())
                    .links(parsed.links())
                    .reason(parsed.reason())
                    .cloudResult(cloudResult)
                    .build();
        }

        if (cloudResult.getMode() == CloudDecisionMode.DISABLED) {
            return TrackerPanelReaderCloudDecision.builder()
                    .status(TrackerPanelReaderCloudDecision.Status.DISABLED)
                    .action(ACTION_NO_ACTION)
                    .reason(cloudResult.getReason())
                    .cloudResult(cloudResult)
                    .build();
        }

        log.warn("cloud.execute serviceId=TRACKER_PANEL_READER accepted=false taskCode={} phase={} reason={} cloudDecision={}",
                cloudRequest.getTaskCode(),
                cloudRequest.getPhase(),
                cloudResult.getReason(),
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision());
        return requiredFailure(cloudResult, cloudResult.getReason());
    }

    private CloudDecisionExecutionGate executionGate(String taskCode, ParseResult[] parsedHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.TRACKER_PANEL_READER;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                ParseResult parsed = parse(taskCode, response);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                parsedHolder[0] = parsed;
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(),
                        parsed.noAction()
                                ? "execute percent gate hit; TRACKER_PANEL_READER no-action"
                                : "execute percent gate hit; using TRACKER_PANEL_READER cloud result");
            }
        };
    }

    private static ParseResult parse(String taskCode, CloudDecisionResponse response) {
        Map<String, String> diagnostics = diagnostics(response);
        Map<String, String> fields = fields(response.getDecision());
        String status = upper(firstText(diagnostics.get("status"), fields.get("status")));
        String action = upper(firstText(diagnostics.get("action"), fields.get("action")));
        String reason = firstText(diagnostics.get("reason"), fields.get("reason"));
        String taskKey = normalize(firstText(diagnostics.get("taskKey"), fields.get("taskKey")), null);
        String targetName = normalize(firstText(diagnostics.get("targetName"), fields.get("targetName")), "");
        String yellowText = normalize(firstText(diagnostics.get("yellowText"), fields.get("yellowText")), "");
        List<TrackerPanelReaderCloudDecision.Link> links = parseLinks(diagnostics.get("links"));

        if (!hasText(action) && "NO_ACTION".equalsIgnoreCase(response.getDecision())) {
            action = ACTION_NO_ACTION;
        }
        if (!hasText(status) && ACTION_NO_ACTION.equals(action)) {
            status = STATUS_NOT_FOUND;
        }
        if (STATUS_AMBIGUOUS.equals(status) || STATUS_ERROR.equals(status)) {
            return ParseResult.rejected("status=" + status + " fail-closed: " + firstText(reason, status));
        }
        if (STATUS_NOT_FOUND.equals(status) || ACTION_NO_ACTION.equals(action)) {
            return ParseResult.noAction(ACTION_NO_ACTION, taskKey, targetName, yellowText,
                    links, firstText(reason, "status=NOT_FOUND"));
        }
        if (!STATUS_FOUND.equals(status)) {
            return ParseResult.rejected("status must be FOUND, NOT_FOUND, AMBIGUOUS, or ERROR");
        }
        if ("wubei".equals(taskCode)
                && (ACTION_CLICK_TRACKER_LINK.equals(action) || ACTION_REROLL.equals(action))
                && !hasText(taskKey)) {
            return ParseResult.rejected("wubei TRACKER_PANEL_READER requires taskKey for action=" + action);
        }
        if (ACTION_REROLL.equals(action)) {
            return ParseResult.found(action, taskKey, targetName, yellowText,
                    null, links, firstText(reason, "reroll"));
        }
        if (!ACTION_CLICK_TRACKER_LINK.equals(action)) {
            return ParseResult.rejected("action must be CLICK_TRACKER_LINK, REROLL, or NO_ACTION");
        }
        String coordinateSpace = firstText(diagnostics.get("coordinateSpace"), fields.get("coordinateSpace"));
        if (!COORDINATE_SPACE_WINDOW_RELATIVE.equals(coordinateSpace)) {
            return ParseResult.rejected("coordinateSpace must be WINDOW_RELATIVE");
        }
        Point click = parseClick(firstText(fields.get("click"), diagnostics.get("clickWindowRelative")));
        if (click == null) {
            return ParseResult.rejected("cloud decision must include click=<windowX>,<windowY>");
        }
        if (!insideWindow(click)) {
            return ParseResult.rejected("window-relative click outside 1024x768 window: click="
                    + click.x + "," + click.y);
        }
        return ParseResult.found(action, taskKey, targetName, yellowText,
                click, links, firstText(reason, "cloud hit"));
    }

    private static String requestValidationError(TrackerPanelReaderCloudRequest request) {
        if (request == null) {
            return "missing tracker panel reader request";
        }
        if (!hasText(request.getImagePayloadBase64())) {
            return "imagePayloadBase64 is required";
        }
        if (!"image/png".equals(request.getPayloadMimeType())) {
            return "payloadMimeType must be image/png";
        }
        if (!hasText(request.getImageSha256())) {
            return "imageSha256 is required";
        }
        if (normalizeTaskCode(request.getTaskCode()) == null) {
            return "taskCode must be one of wuhuan, xiuluo, wubei";
        }
        if (request.getImageOriginWindowX() < 0 || request.getImageOriginWindowY() < 0) {
            return "image origin must be non-negative window-relative pixels";
        }
        return null;
    }

    private static Map<String, String> context(
            TrackerPanelReaderCloudRequest request,
            String taskCode,
            String phase) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("source", safe(request.getSource()));
        context.put("taskCode", taskCode);
        context.put("phase", phase);
        context.put("imagePayloadBase64", request.getImagePayloadBase64().trim());
        context.put("payloadMimeType", request.getPayloadMimeType().trim());
        context.put("imageSha256", request.getImageSha256().trim());
        context.put("imageMode", safe(request.getImageMode()));
        context.put("imageOriginWindow", request.getImageOriginWindowX() + "," + request.getImageOriginWindowY());
        context.put("requestedLinkIndex", Integer.toString(request.getRequestedLinkIndex()));
        context.put("selectionPolicy", safe(request.getSelectionPolicy()));
        // CR248: locally established task key; cloud detail modes must not re-run title matching.
        context.put("taskKey", safe(request.getTaskKey()));
        return Map.copyOf(context);
    }

    private static TrackerPanelReaderCloudDecision requiredFailure(CloudDecisionResult cloudResult, String reason) {
        return TrackerPanelReaderCloudDecision.builder()
                .status(TrackerPanelReaderCloudDecision.Status.REQUIRED_FAILURE)
                .action(ACTION_NO_ACTION)
                .reason(safe(reason))
                .cloudResult(cloudResult)
                .build();
    }

    private static String traceId(TrackerPanelReaderCloudRequest request, String taskCode, String phase) {
        return "tracker-panel-reader:"
                + safeTracePart(taskCode)
                + ":" + safeTracePart(phase)
                + ":" + safeTracePart(request.getSource())
                + ":" + safeTracePart(request.getImageSha256());
    }

    private static List<TrackerPanelReaderCloudDecision.Link> parseLinks(String serialized) {
        if (!hasText(serialized)) {
            return List.of();
        }
        List<TrackerPanelReaderCloudDecision.Link> links = new ArrayList<>();
        for (String part : serialized.split(";", -1)) {
            if (!hasText(part)) {
                continue;
            }
            String[] tokens = part.split(":", -1);
            if (tokens.length < 2) {
                continue;
            }
            Integer index = parseNonNegativeInt(tokens[0]);
            Point click = parseClick(tokens[1]);
            if (index == null || click == null || !insideWindow(click)) {
                continue;
            }
            String attributes = "";
            if (tokens.length >= 4) {
                attributes = String.join(":", java.util.Arrays.copyOfRange(tokens, 3, tokens.length));
            }
            links.add(TrackerPanelReaderCloudDecision.Link.builder()
                    .index(index)
                    .clickWindowRelative(click)
                    .windowRelativeRect(tokens.length >= 3 ? tokens[2] : "")
                    .targetMapName(parseLinkAttribute(attributes, "targetMapName"))
                    .build());
        }
        return List.copyOf(links);
    }

    private static String parseLinkAttribute(String attributes, String key) {
        if (!hasText(attributes) || !hasText(key)) {
            return "";
        }
        String prefix = key + "=";
        for (String token : attributes.split(",", -1)) {
            String trimmed = token.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private static Point parseClick(String value) {
        if (!hasText(value)) {
            return null;
        }
        String text = value.trim();
        if (text.startsWith("click=")) {
            text = text.substring("click=".length());
        }
        String[] parts = text.split(",", -1);
        if (parts.length != 2) {
            return null;
        }
        Integer x = parseNonNegativeInt(parts[0]);
        Integer y = parseNonNegativeInt(parts[1]);
        return x == null || y == null ? null : new Point(x, y);
    }

    private static boolean insideWindow(Point point) {
        return point.x >= 0 && point.x < WINDOW_WIDTH && point.y >= 0 && point.y < WINDOW_HEIGHT;
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

    private static String normalizeTaskCode(String taskCode) {
        String normalized = normalize(taskCode, null);
        if (!hasText(normalized)) {
            return null;
        }
        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "wuhuan", "xiuluo", "wubei" -> normalized.toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    private static String upper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static String firstText(String first, String second) {
        return hasText(first) ? first : safe(second);
    }

    private static String normalize(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
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
            boolean noAction,
            String action,
            String taskKey,
            String targetName,
            String yellowText,
            Point click,
            List<TrackerPanelReaderCloudDecision.Link> links,
            String reason) {
        static ParseResult found(String action,
                                 String taskKey,
                                 String targetName,
                                 String yellowText,
                                 Point click,
                                 List<TrackerPanelReaderCloudDecision.Link> links,
                                 String reason) {
            return new ParseResult(true, false, action, taskKey,
                    safe(targetName), safe(yellowText), click,
                    links == null ? List.of() : List.copyOf(links), reason);
        }

        static ParseResult noAction(String action,
                                    String taskKey,
                                    String targetName,
                                    String yellowText,
                                    List<TrackerPanelReaderCloudDecision.Link> links,
                                    String reason) {
            return new ParseResult(true, true, action, taskKey,
                    safe(targetName), safe(yellowText), null,
                    links == null ? List.of() : List.copyOf(links), reason);
        }

        static ParseResult rejected(String reason) {
            return new ParseResult(false, false, ACTION_NO_ACTION, null, "", "", null, List.of(), reason);
        }
    }
}
