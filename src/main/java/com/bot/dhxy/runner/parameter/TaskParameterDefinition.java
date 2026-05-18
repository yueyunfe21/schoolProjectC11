package com.bot.dhxy.runner.parameter;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 任务参数定义。
 *
 * 作用：描述某个任务参数在 UI 和运行时里的含义。
 */
@Getter
@Builder
public class TaskParameterDefinition {

    /** 参数 key，例如 maxRetries、autoBuyFlag。 */
    private final String key;

    /** UI 展示名称。 */
    private final String label;

    /** 参数说明。 */
    private final String description;

    /** 参数类型。 */
    private final TaskParameterType type;

    /** 默认值，统一用字符串保存，读取时再转换。 */
    private final String defaultValue;

    /** 是否必填。 */
    private final boolean required;

    /** SELECT 类型可选项。 */
    private final List<String> options;
}
