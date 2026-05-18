package com.bot.dhxy.runner.parameter;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 单个任务的参数结构定义。
 */
@Getter
@Builder
public class TaskParameterSchema {

    private final String taskCode;
    private final String taskName;
    private final List<TaskParameterDefinition> parameters;

    public static TaskParameterSchema empty(String taskCode, String taskName) {
        return TaskParameterSchema.builder()
                .taskCode(taskCode)
                .taskName(taskName)
                .parameters(List.of())
                .build();
    }
}
