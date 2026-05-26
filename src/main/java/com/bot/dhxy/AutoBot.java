package com.bot.dhxy;

import com.bot.dhxy.config.TaskRunProperties;
import com.bot.dhxy.input.GlobalEmergencyStopHotkeyService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.ui.MainWindowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Arrays;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class AutoBot implements CommandLineRunner {

    private final TaskRunProperties taskRunProperties;
    private final MainWindowService mainWindowService;
    private final GlobalEmergencyStopHotkeyService emergencyStopHotkeyService;

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

        log.warn("bot.run.auto-start=true is ignored in multi-window mode. Use the window registration/start flow from UI.");
    }
}
