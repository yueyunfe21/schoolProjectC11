package com.bot.dhxy.ui.viewmodel;

import lombok.Builder;
import lombok.Getter;

/**
 * UI 任务勾选项展示模型。
 */
@Getter
@Builder
public class TaskOptionView {

    /**
     * 任务编码，例如：wuhuan、zhuagui、xiuluo。
     */
    private final String taskCode;

    /**
     * 任务显示名，例如：五环、抓鬼、修罗。
     */
    private final String taskName;

    /**
     * UI 上是否默认勾选。
     */
    private final boolean selected;

    /**
     * UI 上是否允许用户勾选。
     */
    private final boolean enabled;
}
