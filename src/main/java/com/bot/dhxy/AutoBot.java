package com.bot.dhxy;

import com.bot.dhxy.config.TaskRunProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.runner.TaskControlService;
import com.bot.dhxy.service.NavigationService;
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

    private final GameClientTracker tracker;
    private final NavigationService navigationService;
    private final TaskControlService taskControlService;
    private final TaskRunProperties taskRunProperties;
    private final MainWindowService mainWindowService;

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(AutoBot.class);
        builder.headless(false).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
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
            if (!initGameWindow()) {
                return;
            }
        } else {
            log.warn("⚠️ 当前配置跳过游戏窗口初始化，仅适合测试任务队列或 UI。正式运行请保持 bot.run.init-game-window=true。");
        }

        taskControlService.startConfiguredTasks();
    }

    private boolean initGameWindow() throws InterruptedException {
        log.info("🎮 准备初始化游戏窗口...");
        Thread.sleep(1000);

        boolean success = tracker.locateWindow();
        if (!success) {
            log.error("❌ 定位失败，请确认大话西游没有被最小化，并且 GAME_WINDOW_KEYWORD 配置正确。");
            return false;
        }

        log.info("🎉 Win32 API 成功抓到大话西游窗口。");
        boolean ready = tracker.bringWindowToFront();
        if (!ready) {
            log.error("❌ 无法唤醒游戏窗口，停止任务。");
            return false;
        }

        navigationService.ensureMapTrackingOption();
        return true;
    }
}
