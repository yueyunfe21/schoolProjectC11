package com.bot.dhxy.runner;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 任务计划服务。
 *
 * 作用：把启动请求转换成真正执行的计划。
 * 后面任务去重、未知任务过滤、任务排序、UI 执行预览，都集中放这里。
 */
@Component
@RequiredArgsConstructor
public class TaskPlanService {

    private final TaskRegistryService taskRegistryService;

    public TaskExecutionPlan buildPlan(TaskRunRequest request) {
        List<String> requestedCodes = request == null ? List.of() : request.getNormalizedTaskCodes();
        PlanTaskSplit split = splitTasks(requestedCodes);

        return TaskExecutionPlan.builder()
                .request(request)
                .requestedTaskCodes(requestedCodes)
                .executableTaskCodes(split.executableTaskCodes())
                .ignoredTaskCodes(split.ignoredTaskCodes())
                .loop(request != null && request.isLoop())
                .testMode(request != null && request.isTestMode())
                .initGameWindow(request != null && request.isInitGameWindow())
                .summaryText(buildSummaryText(split.executableTaskCodes()))
                .warningText(buildWarningText(split.ignoredTaskCodes()))
                .build();
    }

    private PlanTaskSplit splitTasks(List<String> taskCodes) {
        if (taskCodes == null || taskCodes.isEmpty()) {
            return new PlanTaskSplit(List.of(), List.of());
        }

        Set<String> executable = new LinkedHashSet<>();
        List<String> ignored = new ArrayList<>();

        for (String taskCode : taskCodes) {
            if (taskCode == null || taskCode.isBlank()) {
                ignored.add(String.valueOf(taskCode));
                continue;
            }

            String normalizedCode = taskCode.trim();
            if (!taskRegistryService.getTaskByCode(normalizedCode).isPresent()) {
                ignored.add(normalizedCode);
                continue;
            }

            if (!executable.add(normalizedCode)) {
                ignored.add(normalizedCode + "(duplicate)");
            }
        }

        return new PlanTaskSplit(new ArrayList<>(executable), ignored);
    }

    private String buildSummaryText(List<String> taskCodes) {
        if (taskCodes == null || taskCodes.isEmpty()) {
            return "无可执行任务";
        }
        return "共 " + taskCodes.size() + " 个任务: " + String.join(", ", taskCodes);
    }

    private String buildWarningText(List<String> ignoredTaskCodes) {
        if (ignoredTaskCodes == null || ignoredTaskCodes.isEmpty()) {
            return "";
        }
        return "已忽略 " + ignoredTaskCodes.size() + " 个无效或重复任务: " + String.join(", ", ignoredTaskCodes);
    }

    private record PlanTaskSplit(List<String> executableTaskCodes, List<String> ignoredTaskCodes) {
    }
}
