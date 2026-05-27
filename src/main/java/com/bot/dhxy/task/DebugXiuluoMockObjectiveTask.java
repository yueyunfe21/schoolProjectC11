package com.bot.dhxy.task;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class DebugXiuluoMockObjectiveTask implements GameTask {

    private static final String MOCK_MAP_NAME = "瑶池";
    private static final int MOCK_X = 69;
    private static final int MOCK_Y = 95;

    private final XiuluoTask xiuluoTask;

    @Override
    public String getTaskCode() {
        return "debug_xiuluo_mock_objective";
    }

    @Override
    public String getTaskName() {
        return "修罗模拟目标导航测试";
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext context) {
        String prefix = context == null ? "[window=unknown]" : context.getLogPrefix();
        log.info("{} [debug-xiuluo-mock-objective] start: mock existing task objective {}({}, {})",
                prefix, MOCK_MAP_NAME, MOCK_X, MOCK_Y);
        return xiuluoTask.executeDebugMockObjective(context, MOCK_MAP_NAME, MOCK_X, MOCK_Y);
    }

    @Override
    public void stop() {
        xiuluoTask.stop();
    }
}
