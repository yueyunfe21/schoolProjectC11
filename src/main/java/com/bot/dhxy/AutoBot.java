package com.bot.dhxy;

import com.bot.dhxy.config.TaskRunProperties;
import com.bot.dhxy.runner.TaskControlService;
import com.bot.dhxy.service.GameWindowService;
import com.bot.dhxy.ui.MainWindowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class AutoBot implements CommandLineRunner {

    private final TaskControlService taskControlService;
    private final TaskRunProperties taskRunProperties;
    private final MainWindowService mainWindowService;
    private final GameWindowService gameWindowService;

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(AutoBot.class);
        builder.headless(false).run(args);
    }

    @Override
    public void run(String... args) {
        log.info("🚀 Spring 容器装配完毕。");
        log.info("📋 当前可用任务: {}", taskControlService.getRegisteredTaskSummary());
        log.info("🧾 当前任务配置: {}", taskRunProperties.toLogText());

        if (taskRunProperties.isShowUi()) {
            mainWindowService.showMainWindow();
        }

        if (!taskRunProperties.hasTasks()) {
            log.warn("⚠️ 当前没有配置任何任务，程序不会执行任务队列。");
            return;
        }

        if (!taskRunProperties.isAutoStart()) {
            log.info("🕹️ autoStart=false，等待用户从 UI 点击开始。若不显示 UI 且希望自动运行，请设置 bot.run.auto-start=true。");
            return;
        }

        if (taskRunProperties.isInitGameWindow()) {
            if (!gameWindowService.initGameWindow()) {
                return;
            }
        } else {
            log.warn("⚠️ 当前配置跳过游戏窗口初始化，仅适合测试任务队列或 UI。正式运行请保持 bot.run.init-game-window=true。");
        }

        taskControlService.startConfiguredTasks();
    }
}
