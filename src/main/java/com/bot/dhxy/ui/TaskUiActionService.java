package com.bot.dhxy.ui;

import com.bot.dhxy.runner.TaskControlService;
import com.bot.dhxy.runner.TaskExecutionPlan;
import com.bot.dhxy.runner.TaskPlanService;
import com.bot.dhxy.runner.TaskRunRequest;
import com.bot.dhxy.runner.TaskRunResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * UI 任务动作服务。
 *
 * 作用：把界面上的开始、停止、清空、预览计划等动作集中到一个服务里，
 * 让 MainWindowController 只负责界面读写，不直接承载太多任务业务逻辑。
 */
@Component
@RequiredArgsConstructor
public class TaskUiActionService {

    private final TaskControlService taskControlService;
    private final TaskPlanService taskPlanService;

    public TaskRunRequest buildRequestFromUi(List<String> taskCodes,
                                             boolean loop,
                                             boolean testMode,
                                             boolean initGameWindow) {
        return TaskRunRequest.builder()
                .taskCodes(taskCodes)
                .loop(loop)
                .testMode(testMode)
                .initGameWindow(initGameWindow)
                .build();
    }

    public TaskExecutionPlan previewPlanFromUi(List<String> taskCodes,
                                               boolean loop,
                                               boolean testMode,
                                               boolean initGameWindow) {
        return taskPlanService.buildPlan(buildRequestFromUi(taskCodes, loop, testMode, initGameWindow));
    }

    public TaskRunResult startFromUi(List<String> taskCodes,
                                     boolean loop,
                                     boolean testMode,
                                     boolean initGameWindow) {
        return taskControlService.startTasks(buildRequestFromUi(taskCodes, loop, testMode, initGameWindow));
    }

    public TaskRunResult stopFromUi() {
        return taskControlService.stop();
    }

    public void clearFromUi() {
        taskControlService.clearRuntimeLogs();
    }
}
