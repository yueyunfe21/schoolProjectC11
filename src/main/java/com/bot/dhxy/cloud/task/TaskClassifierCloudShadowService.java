package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerTitleTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskClassifierCloudShadowService {

    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String WUBEI_TASK_CODE = "wubei";
    private static final String XIULUO_TASK_CODE = "xiuluo";
    private static final String PHASE = "tracker-title-classification";
    private static final String TRACE_PREFIX = "wubei-task-classifier:";
    private static final String XIULUO_TRACE_PREFIX = "xiuluo-task-classifier:";

    private final CloudDecisionCoordinator coordinator;

    public TaskClassifierCloudShadowService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Reports the local 五倍 task-tracker classification to cloud shadow mode only.
     *
     * @param source caller-provided tracker read source label; may be {@code null}
     * @param result local task-tracker read result; {@code null}, empty, or not-found results are reported as
     *               {@code NOT_FOUND} when the cloud task-classifier service is active
     */
    public CloudDecisionResult shadowWubeiTrackerResult(String source, TaskTrackerPanelReadResult result) {
        return shadowTrackerResult(WUBEI_TASK_CODE, TRACE_PREFIX, source, result);
    }

    /**
     * Reports the local 修罗 tracker shortcut classification to cloud shadow mode only.
     *
     * @param source caller-provided tracker read source label; may be {@code null}
     * @param result local 修罗 tracker read result; empty or not-found results are reported as
     *               {@code NOT_FOUND} when the cloud task-classifier service is active
     */
    public CloudDecisionResult shadowXiuluoTrackerResult(String source, TaskTrackerPanelReadResult result) {
        return shadowTrackerResult(XIULUO_TASK_CODE, XIULUO_TRACE_PREFIX, source, result);
    }

    private CloudDecisionResult shadowTrackerResult(String taskCode,
                                                    String tracePrefix,
                                                    String source,
                                                    TaskTrackerPanelReadResult result) {
        if (!coordinator.isActive(CloudDecisionServiceId.TASK_CLASSIFIER)) {
            return null;
        }

        String localDecision = localDecision(result);
        CloudDecisionRequest request = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.TASK_CLASSIFIER)
                .traceId(tracePrefix + safe(source))
                .taskCode(taskCode)
                .phase(PHASE)
                .localDecision(localDecision)
                .context(context(source, result, localDecision))
                .build();

        return coordinator.shadow(request, localDecision);
    }

    private String localDecision(TaskTrackerPanelReadResult result) {
        TaskTrackerTitleTemplate titleTemplate = result == null ? null : result.getTitleTemplate();
        if (result != null && result.isFound() && titleTemplate != null && hasText(titleTemplate.getTaskKey())) {
            return titleTemplate.getTaskKey();
        }
        return NOT_FOUND;
    }

    private Map<String, String> context(String source, TaskTrackerPanelReadResult result, String localDecision) {
        TaskTrackerTitleTemplate titleTemplate = result == null ? null : result.getTitleTemplate();
        Map<String, String> context = new LinkedHashMap<>();
        context.put("source", safe(source));
        context.put("found", Boolean.toString(result != null && result.isFound()));
        context.put("taskKey", localDecision);
        context.put("title", safe(titleTemplate == null ? null : titleTemplate.getDisplayName()));
        context.put("yellowText", safe(result == null ? null : result.getYellowText()));
        context.put("greenLinkCount", Integer.toString(greenLinkCount(result)));
        context.put("probeObjective", Boolean.toString(result != null && result.isProbeObjective()));
        context.put("detailRawPath", safe(result == null ? null : result.getDetailRawPath()));
        context.put("detailYellowPath", safe(result == null ? null : result.getDetailYellowPath()));
        return context;
    }

    private int greenLinkCount(TaskTrackerPanelReadResult result) {
        List<?> greenLinks = result == null ? null : result.getGreenLinks();
        return greenLinks == null ? 0 : greenLinks.size();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
