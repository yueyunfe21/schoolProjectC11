package com.bot.dhxy.debug;

import com.bot.dhxy.model.navigation.NavigationRequest;
import com.bot.dhxy.model.navigation.NavigationResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.service.NavigationService;
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
 * No-UI live probe for navigating the current real game window to Chang'an East.
 *
 * <p>Run this only from an explicit debug launch while the game client is already open. It uses the
 * normal {@link NavigationService#navigateToNPC(NavigationRequest)} path and current window binding,
 * so it tests the same screenshot, map search, mini-map click, and physical input queue behavior as
 * task code without adding a new UI task type.</p>
 */
public class CurrentToChanganEastDebugMain {

    private static final String TARGET_MAP = "长安城东";
    private static final int TARGET_X = 160;
    private static final int TARGET_Y = 120;

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
            NavigationResult result = run(app);
            System.out.println("currentToChanganEastResult status=" + result.getStatus()
                    + " message=" + result.getMessage());
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

    private static NavigationResult run(ConfigurableApplicationContext app) {
        GameWindowRegistrationService registrationService = app.getBean(GameWindowRegistrationService.class);
        MultiWindowTaskManager taskManager = app.getBean(MultiWindowTaskManager.class);
        WindowTaskContextHolder windowHolder = app.getBean(WindowTaskContextHolder.class);
        TaskExecutionContextHolder executionHolder = app.getBean(TaskExecutionContextHolder.class);
        WindowFocusService focusService = app.getBean(WindowFocusService.class);
        NavigationService navigationService = app.getBean(NavigationService.class);

        registrationService.registerDetectedGameWindows(TaskType.DEBUG_COORDINATE);
        WindowTaskSnapshot selectedSnapshot = selectWindow(taskManager.getAllSnapshots(), focusService);
        WindowRuntimeContext window = taskManager.getRunner(selectedSnapshot.getWindowId())
                .map(WindowTaskRunner::getWindowContext)
                .orElseThrow(() -> new IllegalStateException("runner not found: " + selectedSnapshot.getWindowId()));

        TaskExecutionContext context = buildContext(selectedSnapshot);
        System.out.println("currentToChanganEast: selectedWindow=" + selectedSnapshot.getWindowId()
                + " title=" + selectedSnapshot.getNativeTitle()
                + " hwnd=" + selectedSnapshot.getNativeHandle()
                + " geometry=" + selectedSnapshot.getGeometryText()
                + " target=" + TARGET_MAP + "(" + TARGET_X + "," + TARGET_Y + ")");

        return windowHolder.callWith(window, () -> window.getGameContext().callWithState(window.getGameState(), () ->
                executionHolder.callWith(context, () -> navigationService.navigateToNPC(NavigationRequest.builder()
                        .targetMapName(TARGET_MAP)
                        .targetX(TARGET_X)
                        .targetY(TARGET_Y)
                        .targetName("debug-" + TARGET_MAP)
                        .source("debug-current-to-changan-east")
                        .build()))));
    }

    private static WindowTaskSnapshot selectWindow(List<WindowTaskSnapshot> snapshots,
                                                   WindowFocusService focusService) {
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalStateException("no registered game window");
        }

        String explicitWindowId = System.getProperty("nav.single.windowId", "").trim();
        String titleContains = System.getProperty("nav.single.windowTitleContains", "").trim();
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

    private static TaskExecutionContext buildContext(WindowTaskSnapshot snapshot) {
        return TaskExecutionContext.builder()
                .taskCode("debug-current-to-changan-east")
                .taskName("当前位置到长安城东调试")
                .requestedTaskCode(TaskType.DEBUG_COORDINATE.getCode())
                .requestedTaskName(TaskType.DEBUG_COORDINATE.getDisplayName())
                .windowId(snapshot.getWindowId())
                .windowRole(snapshot.getRole() == null ? null : snapshot.getRole().name())
                .nativeWindowHandle(snapshot.getNativeHandle())
                .nativeWindowTitle(snapshot.getNativeTitle())
                .nativeWindowClassName(snapshot.getNativeClassName())
                .nativeWindowProcessId(snapshot.getNativeProcessId())
                .nativeWindowX(snapshot.getNativeBinding().getX())
                .nativeWindowY(snapshot.getNativeBinding().getY())
                .nativeWindowWidth(snapshot.getNativeBinding().getWidth())
                .nativeWindowHeight(snapshot.getNativeBinding().getHeight())
                .retryPolicy(TaskRetryPolicy.none())
                .startedAt(LocalDateTime.now())
                .build();
    }
}
