package com.bot.dhxy.runner.definition;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 可执行任务的展示信息。
 *
 * 后面界面可以用它生成任务勾选列表。
 */
@Getter
@AllArgsConstructor
public class TaskDefinition {

    /**
     * 任务编码，例如：wuhuan、zhuagui、xiuluo。
     */
    private final String taskCode;

    /**
     * 任务展示名，例如：五环、抓鬼、修罗。
     */
    private final String taskName;
}
