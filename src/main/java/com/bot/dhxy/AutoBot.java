package com.bot.dhxy;

import com.bot.dhxy.config.TaskRunProperties;
import com.bot.dhxy.input.GlobalEmergencyStopHotkeyService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.ui.MainWindowService;
import com.bot.dhxy.window.control.WindowTaskCommandResult;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class AutoBot implements CommandLineRunner {

    private final TaskRunProperties taskRunProperties;
    private final MainWindowService mainWindowService;
    private final GlobalEmergencyStopHotkeyService emergencyStopHotkeyService;
    private final GameWindowRegistrationService gameWindowRegistrationService;

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(AutoBot.class);
        builder.headless(false).run(args);
    }

    @Override
    public void run(String... args) {
        log.info("Spring container ready.");
        log.info("Available window tasks: {}", Arrays.toString(TaskType.values()));
        log.info("Current task config: {}", taskRunProperties.toLogText());
        emergencyStopHotkeyService.start();

        if (taskRunProperties.isShowUi()) {
            mainWindowService.showMainWindow();
        }

        if (!taskRunProperties.isAutoStart()) {
            log.info("autoStart=false; waiting for UI window registration/start command.");
            return;
        }

        if (!taskRunProperties.isInitGameWindow()) {
            throw new IllegalStateException("headless auto-start requires bot.run.init-game-window=true");
        }

        TaskType taskType = resolveAutoStartTask();
        WindowTaskCommandResult result =
                gameWindowRegistrationService.scanRegisterAndStartIndependentWindows(taskType);
        log.info("Headless auto-start finished: task={} requested={} success={} failed={} message={}",
                taskType, result.getRequestedCount(), result.getSuccessCount(), result.getFailedCount(),
                result.getMessage());
        if (!result.isAllSuccess()) {
            throw new IllegalStateException("headless auto-start failed: " + result.getMessage());
        }
        if (!taskRunProperties.isShowUi()) {
            log.info("Headless auto-start is active; keeping the host process alive until interrupted.");
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                log.info("Headless auto-start host interrupted; application will stop.");
            }
        }
    }

    private TaskType resolveAutoStartTask() {
        var configuredTasks = taskRunProperties.getNormalizedTasks();
        if (configuredTasks.size() != 1) {
            throw new IllegalStateException("headless auto-start requires exactly one bot.run.tasks entry");
        }
        String configuredCode = configuredTasks.getFirst();
        return Arrays.stream(TaskType.values())
                .filter(type -> type != TaskType.UNKNOWN && type != TaskType.XIULUO)
                .filter(type -> type.getCode().equalsIgnoreCase(configuredCode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "unsupported headless auto-start task: " + configuredCode));
    }
}
