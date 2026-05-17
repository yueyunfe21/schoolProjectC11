package com.bot.dhxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
}
