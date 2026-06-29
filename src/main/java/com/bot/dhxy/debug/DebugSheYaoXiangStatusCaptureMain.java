package com.bot.dhxy.debug;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * No-UI live probe for the 摄妖香 status crop.
 *
 * <p>Run this only from an explicit debug launch while the game client is already open. It registers
 * detected windows, binds the selected window context, captures the current status probe rectangle,
 * writes the probe PNGs through {@link PlayerStateService}, and logs the cyan-hour/green-minute
 * read result. It does not click or open the bag.</p>
 */
public class DebugSheYaoXiangStatusCaptureMain {

    public static void main(String[] args) {
        ConfigurableApplicationContext app = new SpringApplicationBuilder(ToolSpringConfig.class)
                .headless(false)
                .web(WebApplicationType.NONE)
                .properties(
                        "bot.run.show-ui=false",
                        "bot.run.auto-start=false",
                        "bot.run.init-game-window=false"
                )
                .run(args);

        try {
            String path = run(app);
            System.out.println("sheyaoxiangStatusDebugPath=" + path);
        } finally {
            app.close();
            System.exit(0);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(
            basePackages = "com.bot.dhxy",
            excludeFilters = {
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bot\\.dhxy\\.AutoBot"),
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bot\\.dhxy\\.ui\\..*"),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CommandLineRunner.class)
            }
    )
    static class ToolSpringConfig {
    }

    private static String run(ConfigurableApplicationContext app) {
        GameWindowRegistrationService registrationService = app.getBean(GameWindowRegistrationService.class);
        MultiWindowTaskManager taskManager = app.getBean(MultiWindowTaskManager.class);
        WindowTaskContextHolder windowHolder = app.getBean(WindowTaskContextHolder.class);
        WindowFocusService focusService = app.getBean(WindowFocusService.class);
        PlayerStateService playerStateService = app.getBean(PlayerStateService.class);

        registrationService.registerDetectedGameWindows(TaskType.DEBUG_COORDINATE);
        WindowTaskSnapshot selectedSnapshot = selectWindow(taskManager.getAllSnapshots(), focusService);
        WindowRuntimeContext window = taskManager.getRunner(selectedSnapshot.getWindowId())
                .map(WindowTaskRunner::getWindowContext)
                .orElseThrow(() -> new IllegalStateException("runner not found: " + selectedSnapshot.getWindowId()));

        TaskExecutionContext context = TaskExecutionContext.builder()
                .taskCode("debug-sheyaoxiang-status-capture")
                .taskName("摄妖香状态截图调试")
                .requestedTaskCode(TaskType.DEBUG_COORDINATE.getCode())
                .requestedTaskName(TaskType.DEBUG_COORDINATE.getDisplayName())
                .windowId(selectedSnapshot.getWindowId())
                .windowRole(selectedSnapshot.getRole() == null ? null : selectedSnapshot.getRole().name())
                .nativeWindowHandle(selectedSnapshot.getNativeHandle())
                .nativeWindowTitle(selectedSnapshot.getNativeTitle())
                .nativeWindowClassName(selectedSnapshot.getNativeClassName())
                .nativeWindowProcessId(selectedSnapshot.getNativeProcessId())
                .nativeWindowX(selectedSnapshot.getNativeBinding().getX())
                .nativeWindowY(selectedSnapshot.getNativeBinding().getY())
                .nativeWindowWidth(selectedSnapshot.getNativeBinding().getWidth())
                .nativeWindowHeight(selectedSnapshot.getNativeBinding().getHeight())
                .retryPolicy(TaskRetryPolicy.none())
                .startedAt(LocalDateTime.now())
                .build();

        System.out.println("sheyaoxiangStatusDebugSelectedWindow=" + selectedSnapshot.getWindowId()
                + " title=" + selectedSnapshot.getNativeTitle()
                + " hwnd=" + selectedSnapshot.getNativeHandle()
                + " geometry=" + selectedSnapshot.getGeometryText());

        return windowHolder.callWith(window, () -> window.getGameContext().callWithState(window.getGameState(),
                () -> playerStateService.captureSheYaoXiangStatusDebugImage(context, "manual")));
    }

    private static WindowTaskSnapshot selectWindow(List<WindowTaskSnapshot> snapshots,
                                                   WindowFocusService focusService) {
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalStateException("no registered game window");
        }

        String explicitWindowId = System.getProperty("sheyaoxiang.debug.windowId", "").trim();
        String titleContains = System.getProperty("sheyaoxiang.debug.windowTitleContains", "").trim();
        String foregroundHandle = focusService.getForegroundNativeHandleText();
        Long foreground = WindowHandleParser.parseHandle(foregroundHandle);

        return snapshots.stream()
                .filter(snapshot -> explicitWindowId.isBlank()
                        || explicitWindowId.equals(snapshot.getWindowId()))
                .filter(snapshot -> titleContains.isBlank()
                        || (snapshot.getNativeTitle() != null && snapshot.getNativeTitle().contains(titleContains)))
                .filter(snapshot -> foreground == null
                        || Objects.equals(foreground, WindowHandleParser.parseHandle(snapshot.getNativeHandle())))
                .findFirst()
                .or(() -> snapshots.stream()
                        .filter(snapshot -> explicitWindowId.isBlank()
                                || explicitWindowId.equals(snapshot.getWindowId()))
                        .filter(snapshot -> titleContains.isBlank()
                                || (snapshot.getNativeTitle() != null && snapshot.getNativeTitle().contains(titleContains)))
                        .findFirst())
                .orElseGet(() -> snapshots.stream()
                        .sorted(Comparator.comparing(WindowTaskSnapshot::getWindowId,
                                Comparator.nullsLast(String::compareTo)))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("no registered game window")));
    }
}
