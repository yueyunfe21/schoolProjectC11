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
        List<String> normalizedCodes = request == null ? List.of() : request.getNormalizedTaskCodes();
        List<String> executableCodes = normalizeAndFilterTasks(normalizedCodes);

        return TaskExecutionPlan.builder()
                .request(request)
                .taskCodes(executableCodes)
                .loop(request != null && request.isLoop())
                .testMode(request != null && request.isTestMode())
                .initGameWindow(request != null && request.isInitGameWindow())
                .summaryText(buildSummaryText(executableCodes))
                .build();
    }

    private List<String> normalizeAndFilterTasks(List<String> taskCodes) {
        if (taskCodes == null || taskCodes.isEmpty()) {
            return List.of();
        }

        Set<String> deduplicated = new LinkedHashSet<>();
        for (String taskCode : taskCodes) {
            if (taskCode == null || taskCode.isBlank()) {
                continue;
            }
            String normalizedCode = taskCode.trim();
            if (taskRegistryService.getTaskByCode(normalizedCode).isPresent()) {
                deduplicated.add(normalizedCode);
            }
        }
        return new ArrayList<>(deduplicated);
    }

    private String buildSummaryText(List<String> taskCodes) {
        if (taskCodes == null || taskCodes.isEmpty()) {
            return "无可执行任务";
        }
        return "共 " + taskCodes.size() + " 个任务: " + String.join(", ", taskCodes);
    }
}
