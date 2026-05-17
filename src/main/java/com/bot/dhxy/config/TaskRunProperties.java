package com.bot.dhxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务队列启动配置。
 *
 * 目前先从 application.properties 读取。
 * 后面接 UI 后，可以由界面生成/修改这些配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "bot.run")
public class TaskRunProperties {

    /**
     * 用户选择的任务编码，例如：wuhuan、zhuagui、xiuluo。
     */
    private List<String> tasks = new ArrayList<>(List.of("wuhuan"));

    /**
     * 是否循环执行任务队列。
     */
    private boolean loop = false;

    /**
     * 测试模式：只验证任务注册与队列调度，不真正执行任务逻辑。
     */
    private boolean testMode = false;

    /**
     * 是否在启动前自动初始化游戏窗口。
     *
     * 正式运行时应该保持 true。
     * 后面如果只想测试 UI 或任务队列，可以临时改成 false。
     */
    private boolean initGameWindow = true;

    /**
     * 是否显示 JavaFX 主界面。
     */
    private boolean showUi = true;

    /**
     * 显示 UI 后是否自动启动任务。
     *
     * showUi=true 时建议先保持 false，让用户从界面点开始。
     * showUi=false 时可以保持 true，让程序启动后直接跑任务。
     */
    private boolean autoStart = false;

    /**
     * 清洗后的任务编码列表，去掉空白项。
     */
    public List<String> getNormalizedTasks() {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream()
                .map(String::trim)
                .filter(task -> !task.isEmpty())
                .collect(Collectors.toList());
    }

    public boolean hasTasks() {
        return !getNormalizedTasks().isEmpty();
    }

    public String toLogText() {
        return "tasks=" + getNormalizedTasks()
                + " | loop=" + loop
                + " | testMode=" + testMode
                + " | initGameWindow=" + initGameWindow
                + " | showUi=" + showUi
                + " | autoStart=" + autoStart;
    }
}
