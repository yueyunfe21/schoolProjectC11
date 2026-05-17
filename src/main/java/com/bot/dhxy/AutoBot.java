package com.bot.dhxy;

import com.bot.dhxy.config.TaskRunProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.runner.TaskQueue;
import com.bot.dhxy.runner.TaskRunner;
import com.bot.dhxy.service.NavigationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class AutoBot implements CommandLineRunner {

    private final GameClientTracker tracker;
    private final NavigationService navigationService;
    private final TaskRunner taskRunner;
    private final TaskRunProperties taskRunProperties;

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(AutoBot.class);
        builder.headless(false).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 Spring 容器装配完毕，准备初始化游戏窗口...");
        Thread.sleep(1000);

        boolean success = tracker.locateWindow();
        if (!success) {
            log.error("❌ 定位失败，请确认大话西游没有被最小化，并且 GAME_WINDOW_KEYWORD 配置正确。");
            return;
        }

        log.info("🎉 Win32 API 成功抓到大话西游窗口。");
        boolean ready = tracker.bringWindowToFront();
        if (!ready) {
            log.error("❌ 无法唤醒游戏窗口，停止任务。");
            return;
        }

        navigationService.ensureMapTrackingOption();

        log.info("🧾 当前任务配置: tasks={} | loop={} | testMode={}",
                taskRunProperties.getTasks(),
                taskRunProperties.isLoop(),
                taskRunProperties.isTestMode());

        taskRunner.run(
                new TaskQueue(taskRunProperties.getTasks(), taskRunProperties.isLoop()),
                taskRunProperties.isTestMode()
        );
    }
}
