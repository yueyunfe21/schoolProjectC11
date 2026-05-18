package com.bot.dhxy.ui;

import com.bot.dhxy.runner.TaskControlService;
import com.bot.dhxy.runner.TaskRunRequest;
import com.bot.dhxy.runner.TaskRunResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * UI 任务动作服务。
 *
 * 作用：把界面上的开始、停止、清空等动作集中到一个服务里，
 * 让 MainWindowController 只负责界面读写，不直接承载太多任务业务逻辑。
 */
@Component
@RequiredArgsConstructor
public class TaskUiActionService {

    private final TaskControlService taskControlService;

    public TaskRunResult startFromUi(List<String> taskCodes,
                                     boolean loop,
                                     boolean testMode,
                                     boolean initGameWindow) {
        TaskRunRequest request = TaskRunRequest.builder()
                .taskCodes(taskCodes)
                .loop(loop)
                .testMode(testMode)
                .initGameWindow(initGameWindow)
                .build();
        return taskControlService.startTasks(request);
    }

    public TaskRunResult stopFromUi() {
        return taskControlService.stop();
    }

    public void clearFromUi() {
        taskControlService.clearRuntimeLogs();
    }
}
