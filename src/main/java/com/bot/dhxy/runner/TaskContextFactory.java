package com.bot.dhxy.runner;

import com.bot.dhxy.runner.parameter.TaskParameterValue;
import com.bot.dhxy.runner.policy.TaskErrorPolicy;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.task.GameTask;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class TaskContextFactory {

    public TaskExecutionContext create(GameTask task,
                                       TaskRunRequest request,
                                       TaskExecutionPlan plan,
                                       TaskStopToken stopToken,
                                       Map<String, List<TaskParameterValue>> parameters) {
        return TaskExecutionContext.builder()
                .taskCode(task == null ? null : task.getTaskCode())
                .taskName(task == null ? null : task.getTaskName())
                .request(request)
                .plan(plan)
                .stopToken(stopToken)
                .errorPolicy(TaskErrorPolicy.CONTINUE_NEXT_TASK)
                .retryPolicy(TaskRetryPolicy.defaultPolicy())
                .parameters(parameters)
                .startedAt(LocalDateTime.now())
                .build();
    }
}
