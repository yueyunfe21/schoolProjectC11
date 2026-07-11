package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionMode;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.model.tasktracker.TaskTrackerGreenLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TrackerLinkRankerCloudShadowService {

    private static final Logger log = LoggerFactory.getLogger(TrackerLinkRankerCloudShadowService.class);
    private static final String UNKNOWN_TASK_CODE = "unknown";
    private static final String DEFAULT_PHASE = "tracker-green-link-selection";
    private static final String NO_LINK = "NO_LINK";
    private static final String ACTION_KEY = "action";
    private static final String ACTION_CLICK_TRACKER_LINK = "CLICK_TRACKER_LINK";
    private static final String COORDINATE_SPACE_KEY = "coordinateSpace";
    private static final String COORDINATE_SPACE_WINDOW_RELATIVE = "WINDOW_RELATIVE";
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final int TRACKER_ROI_MIN_X = 0;
    private static final int TRACKER_ROI_MAX_X = 260;
    private static final int TRACKER_ROI_MIN_Y = 180;
    private static final int TRACKER_ROI_MAX_Y = 620;

    private final CloudDecisionCoordinator coordinator;

    public TrackerLinkRankerCloudShadowService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Reports the local task-tracker green-link choice and optionally accepts a cloud window-relative
     * click point for execute mode.
     *
     * <p>The cloud-required execute contract is deliberately simple for CR-HC-018:
     * {@code decision=click=<windowX>,<windowY>},
     * {@code diagnostics.action=CLICK_TRACKER_LINK}, and
     * {@code diagnostics.coordinateSpace=WINDOW_RELATIVE}. The service no longer accepts local
     * candidate indexes or candidate fingerprints. If execute mode reaches the cloud/gate but the
     * response is unavailable or invalid, the returned envelope is an explicit no-click result.</p>
     *
     * @param taskCode local task code such as {@code wubei} or {@code xiuluo}; blank becomes
     *                 {@code unknown}
     * @param source caller/source label used in trace id and diagnostic context; may be {@code null}
     * @param phase business phase for this tracker-link decision; blank becomes
     *              {@code tracker-green-link-selection}
     * @param candidates local ranked tracker green-link candidates in screen-absolute coordinates;
     *                   {@code null} is treated as an empty list and is used for shadow diagnostics
     * @param selectedIndex index selected by the local business path
     * @param selectedLink selected local link in screen-absolute coordinates; when {@code null}, a
     *                     valid {@code selectedIndex} resolves the link from {@code candidates}
     * @param windowBaseX current bound game-window logical screen X base; invalid negative values
     *                    leave window-relative context fields blank
     * @param windowBaseY current bound game-window logical screen Y base; invalid negative values
     *                    leave window-relative context fields blank
     * @return decision envelope describing local passthrough, accepted cloud click, or execute
     *         rejection/no-click.
     */
    public TrackerLinkRankerCloudDecision shadowTrackerLinkSelection(
            String taskCode,
            String source,
            String phase,
            List<TaskTrackerGreenLink> candidates,
            int selectedIndex,
            TaskTrackerGreenLink selectedLink,
            int windowBaseX,
            int windowBaseY) {
        String normalizedTaskCode = normalize(taskCode, UNKNOWN_TASK_CODE);
        String normalizedPhase = normalize(phase, DEFAULT_PHASE);
        List<TaskTrackerGreenLink> safeCandidates = candidates == null ? List.of() : candidates;
        TaskTrackerGreenLink effectiveSelectedLink = selectedLink != null
                ? selectedLink
                : selectedFromIndex(safeCandidates, selectedIndex);
        boolean validSelection = selectedIndex >= 0 && effectiveSelectedLink != null;
        String localDecision = validSelection ? localDecision(selectedIndex, effectiveSelectedLink) : NO_LINK;
        int localSelectedIndex = validSelection ? selectedIndex : -1;
        TaskTrackerGreenLink localSelectedLink = validSelection ? effectiveSelectedLink : null;

        if (!coordinator.isActive(CloudDecisionServiceId.TRACKER_LINK_RANKER)) {
            return TrackerLinkRankerCloudDecision.localOnly(localSelectedIndex, localSelectedLink);
        }

        CloudDecisionRequest request = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.TRACKER_LINK_RANKER)
                .traceId("tracker-link-ranker:" + normalizedTaskCode + ":" + safe(source))
                .taskCode(normalizedTaskCode)
                .phase(normalizedPhase)
                .localDecision(localDecision)
                .context(context(source, safeCandidates, selectedIndex,
                        validSelection ? effectiveSelectedLink : null, windowBaseX, windowBaseY))
                .build();

        Point[] cloudWindowRelativePoint = new Point[1];
        boolean[] executeGateEvaluated = {false};
        CloudDecisionResult cloudResult = coordinator.shadow(
                request,
                localDecision,
                windowRelativeClickExecutionGate(cloudWindowRelativePoint, executeGateEvaluated));
        if (cloudResult.isExecuted() && cloudWindowRelativePoint[0] != null) {
            return TrackerLinkRankerCloudDecision.cloudExecuted(
                    cloudResult, localSelectedIndex, localSelectedLink, cloudWindowRelativePoint[0]);
        }
        if (keepsLocalPassthrough(cloudResult, executeGateEvaluated[0])) {
            return TrackerLinkRankerCloudDecision.localPassthrough(
                    cloudResult, localSelectedIndex, localSelectedLink);
        }

        String rejectReason = cloudResult.getReason();
        log.warn("TRACKER_LINK_RANKER execute rejected no-click: taskCode={} phase={} source={} "
                        + "localDecision={} cloudDecision={} reason={}",
                normalizedTaskCode,
                normalizedPhase,
                safe(source),
                localDecision,
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision(),
                rejectReason);
        return TrackerLinkRankerCloudDecision.cloudRejectedNoClick(
                cloudResult, localSelectedIndex, localSelectedLink, rejectReason);
    }

    private Map<String, String> context(String source,
                                        List<TaskTrackerGreenLink> candidates,
                                        int selectedIndex,
                                        TaskTrackerGreenLink selectedLink,
                                        int windowBaseX,
                                        int windowBaseY) {
        Map<String, String> context = new LinkedHashMap<>();
        String candidateSummary = candidates(candidates);
        boolean validWindowBase = windowBaseX >= 0 && windowBaseY >= 0;
        context.put("source", safe(source));
        context.put("candidateCount", Integer.toString(candidates.size()));
        context.put("selectedIndex", Integer.toString(selectedIndex));
        context.put("selectedClick", selectedLink == null ? "" : click(selectedLink));
        context.put("windowBase", validWindowBase ? windowBaseX + "," + windowBaseY : "");
        context.put("selectedWindowClick",
                selectedLink == null || !validWindowBase ? "" : windowClick(selectedLink, windowBaseX, windowBaseY));
        context.put("selectedRect", selectedLink == null ? "" : rect(selectedLink));
        context.put("selectedTargetMap", selectedLink == null ? "" : safe(selectedLink.getTargetMapName()));
        context.put("selectedTargetMapScore", selectedLink == null ? "" : score(selectedLink.getTargetMapScore()));
        context.put("candidates", candidateSummary);
        return context;
    }

    private String localDecision(int selectedIndex, TaskTrackerGreenLink selectedLink) {
        return "index=" + selectedIndex
                + ";click=" + click(selectedLink)
                + ";rect=" + rect(selectedLink);
    }

    private String candidates(List<TaskTrackerGreenLink> candidates) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0) {
                builder.append('|');
            }
            TaskTrackerGreenLink candidate = candidates.get(i);
            builder.append(i)
                    .append(":click=").append(click(candidate))
                    .append(";rect=").append(rect(candidate))
                    .append(";map=").append(safe(candidate.getTargetMapName()))
                    .append(";score=").append(score(candidate.getTargetMapScore()));
        }
        return builder.toString();
    }

    private CloudDecisionExecutionGate windowRelativeClickExecutionGate(
            Point[] cloudWindowRelativePoint,
            boolean[] executeGateEvaluated) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.TRACKER_LINK_RANKER;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                executeGateEvaluated[0] = true;
                String action = response.getDiagnostics() == null
                        ? null
                        : response.getDiagnostics().get(ACTION_KEY);
                if (!ACTION_CLICK_TRACKER_LINK.equals(action)) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "diagnostics.action must be " + ACTION_CLICK_TRACKER_LINK);
                }
                String coordinateSpace = response.getDiagnostics() == null
                        ? null
                        : response.getDiagnostics().get(COORDINATE_SPACE_KEY);
                if (!COORDINATE_SPACE_WINDOW_RELATIVE.equals(coordinateSpace)) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "diagnostics.coordinateSpace must be " + COORDINATE_SPACE_WINDOW_RELATIVE);
                }
                Point point = parseWindowRelativeClick(response.getDecision());
                if (point == null) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "schema mismatch: tracker-link-ranker cloud decision must be click=<windowX>,<windowY>");
                }
                if (!insideWindow(point)) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "window-relative click outside 1024x768 window: click=" + point.x + "," + point.y);
                }
                if (!insideTrackerRoi(point)) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "window-relative click outside left tracker ROI: click=" + point.x + "," + point.y
                                    + " roi=(" + TRACKER_ROI_MIN_X + "," + TRACKER_ROI_MIN_Y + ")-("
                                    + TRACKER_ROI_MAX_X + "," + TRACKER_ROI_MAX_Y + ")");
                }
                cloudWindowRelativePoint[0] = point;
                return CloudDecisionExecutionGate.GateResult.accepted(
                        "click=" + point.x + "," + point.y,
                        "execute percent gate hit; using tracker-link-ranker window-relative click");
            }
        };
    }

    private boolean keepsLocalPassthrough(CloudDecisionResult cloudResult, boolean executeGateEvaluated) {
        if (cloudResult == null || cloudResult.getMode() != CloudDecisionMode.EXECUTE) {
            return true;
        }
        if (cloudResult.isRequiredExecuteFailure()) {
            return false;
        }
        if (cloudResult.isExecuted()) {
            return false;
        }
        return !executeGateEvaluated
                && cloudResult.isCloudAvailable()
                && contains(cloudResult.getReason(), "percent");
    }

    private static TaskTrackerGreenLink selectedFromIndex(List<TaskTrackerGreenLink> candidates, int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex >= candidates.size()) {
            return null;
        }
        return candidates.get(selectedIndex);
    }

    private static Point parseWindowRelativeClick(String decision) {
        if (decision == null) {
            return null;
        }
        String trimmed = decision.trim();
        if (!trimmed.startsWith("click=")) {
            return null;
        }
        String[] parts = trimmed.substring("click=".length()).split(",", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new Point(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean insideWindow(Point point) {
        return point.x >= 0
                && point.x < WINDOW_WIDTH
                && point.y >= 0
                && point.y < WINDOW_HEIGHT;
    }

    private static boolean insideTrackerRoi(Point point) {
        return point.x >= TRACKER_ROI_MIN_X
                && point.x <= TRACKER_ROI_MAX_X
                && point.y >= TRACKER_ROI_MIN_Y
                && point.y <= TRACKER_ROI_MAX_Y;
    }

    private static String click(TaskTrackerGreenLink link) {
        Point point = link.centerPoint();
        return point.x + "," + point.y;
    }

    private static String windowClick(TaskTrackerGreenLink link, int windowBaseX, int windowBaseY) {
        Point point = link.centerPoint();
        return (point.x - windowBaseX) + "," + (point.y - windowBaseY);
    }

    private static String rect(TaskTrackerGreenLink link) {
        return link.minX() + "," + link.minY() + "," + link.maxX() + "," + link.maxY();
    }

    private static String score(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean contains(String value, String expected) {
        return value != null && value.contains(expected);
    }
}
