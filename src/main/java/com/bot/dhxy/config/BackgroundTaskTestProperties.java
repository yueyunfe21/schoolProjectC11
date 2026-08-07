package com.bot.dhxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration for the opt-in, no-UI live-task test control host. */
@Data
@Component
@ConfigurationProperties(prefix = "bot.background-test")
public class BackgroundTaskTestProperties {

    /** The control host is inert unless a launch command explicitly enables it. */
    private boolean enabled = false;

    /** Repository-relative directory used only for session-fenced control requests and results. */
    private String controlDirectory = "logs/background-task-test";

    /** Default requested run count for the dedicated Wuhuan and Tianting live-test entry points. */
    private int defaultMaxRuns = 100;
}
