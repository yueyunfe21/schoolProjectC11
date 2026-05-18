package com.bot.dhxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 多窗口隔离功能开关。
 *
 * 默认关闭，优先保证旧的单窗口五环稳定。
 * 后续测试多窗口时再逐项打开。
 */
@Data
@Component
@ConfigurationProperties(prefix = "bot.window")
public class WindowIsolationProperties {

    /**
     * 是否启用多窗口隔离。
     *
     * false：尽量保持旧单窗口行为。
     * true：启用任务线程绑定窗口、输入前激活当前 hwnd、tracker 线程隔离等多窗口能力。
     */
    private boolean isolationEnabled = false;
}
