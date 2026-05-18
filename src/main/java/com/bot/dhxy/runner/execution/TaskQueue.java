package com.bot.dhxy.runner.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一次启动时用户勾选出来的任务队列。
 *
 * 现在先做轻量版本：按顺序执行用户选择的任务。
 * 后面 UI 成型后，可以在这里继续扩展循环、次数、优先级、失败策略等配置。
 */
public class TaskQueue {

    private final List<String> selectedTaskCodes;
    private final boolean loop;

    public TaskQueue(List<String> selectedTaskCodes, boolean loop) {
        this.selectedTaskCodes = new ArrayList<>(selectedTaskCodes == null ? Collections.emptyList() : selectedTaskCodes);
        this.loop = loop;
    }

    public static TaskQueue single(String taskCode) {
        return new TaskQueue(List.of(taskCode), false);
    }

    public static TaskQueue once(List<String> selectedTaskCodes) {
        return new TaskQueue(selectedTaskCodes, false);
    }

    public static TaskQueue loop(List<String> selectedTaskCodes) {
        return new TaskQueue(selectedTaskCodes, true);
    }

    public List<String> getSelectedTaskCodes() {
        return Collections.unmodifiableList(selectedTaskCodes);
    }

    public boolean isLoop() {
        return loop;
    }

    public boolean isEmpty() {
        return selectedTaskCodes.isEmpty();
    }
}
