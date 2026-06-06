package com.bot.dhxy.debug;

import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.execution.WindowTaskSubmitResult;
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

import java.util.List;

/**
 * No-UI runner for navigation latency debugging against real registered game windows.
 *
 * <p>The runner intentionally uses the normal multi-window registration and
 * {@link TaskType#DEBUG_NAVIGATION_STRESS} task path, so it exercises the same window binding,
 * input queue, navigation service, and task-turn handoff as the JavaFX start button. Before
 * submitting tasks, it clears generic UI once per bound window to avoid stale overlays from a
 * previous failed run.</p>
 */
public class NavigationStressDebugMain {

    private static final long DEFAULT_TIMEOUT_MS = 1_200_000L;
    private static final long POLL_MS = 1_000L;
    private static final int DEFAULT_WINDOW_LIMIT = 2;
    private static final String WINDOW_LIMIT_KEY = "navigation.stress.windowLimit";

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
            run(app);
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

    private static void run(ConfigurableApplicationContext app) {
        GameWindowRegistrationService registrationService = app.getBean(GameWindowRegistrationService.class);
        MultiWindowTaskManager taskManager = app.getBean(MultiWindowTaskManager.class);
        WindowTaskContextHolder windowHolder = app.getBean(WindowTaskContextHolder.class);
        UICleanerService uiCleanerService = app.getBean(UICleanerService.class);

        registrationService.registerDetectedGameWindows(TaskType.DEBUG_NAVIGATION_STRESS);
        List<WindowTaskSnapshot> snapshots = taskManager.getAllSnapshots();
        if (snapshots.isEmpty()) {
            System.out.println("navigationStressDebug: no game windows registered");
            return;
        }
        int windowLimit = Math.max(1, Integer.getInteger(WINDOW_LIMIT_KEY, DEFAULT_WINDOW_LIMIT));
        if (windowLimit < snapshots.size()) {
            snapshots = snapshots.stream().limit(windowLimit).toList();
            System.out.println("navigationStressDebug: window limit=" + windowLimit
                    + " overrideKey=" + WINDOW_LIMIT_KEY);
        }

        clearUiForRegisteredWindows(taskManager, windowHolder, uiCleanerService, snapshots);
        submitStressTasks(taskManager, snapshots);
        waitForCompletion(taskManager);
        printFinalSnapshots(taskManager);
    }

    private static void clearUiForRegisteredWindows(MultiWindowTaskManager taskManager,
                                                    WindowTaskContextHolder windowHolder,
                                                    UICleanerService uiCleanerService,
                                                    List<WindowTaskSnapshot> snapshots) {
        for (WindowTaskSnapshot snapshot : snapshots) {
            taskManager.getRunner(snapshot.getWindowId()).ifPresent(runner -> {
                WindowRuntimeContext window = runner.getWindowContext();
                windowHolder.runWith(window, () -> window.getGameContext().runWithState(window.getGameState(), () -> {
                    System.out.println("navigationStressDebug: clearUi window=" + snapshot.getWindowId()
                            + " title=" + snapshot.getNativeTitle());
                    uiCleanerService.closeAllGenericWindows();
                }));
            });
        }
    }

    private static void submitStressTasks(MultiWindowTaskManager taskManager, List<WindowTaskSnapshot> snapshots) {
        for (WindowTaskSnapshot snapshot : snapshots) {
            WindowTaskSubmitResult result = taskManager.submitWithResult(snapshot.getWindowId(), TaskType.DEBUG_NAVIGATION_STRESS);
            System.out.println("navigationStressDebug: submit window=" + snapshot.getWindowId()
                    + " success=" + result.isSuccess()
                    + " status=" + result.getStatus()
                    + " message=" + result.getMessage());
        }
    }

    private static void waitForCompletion(MultiWindowTaskManager taskManager) {
        long timeoutMs = Long.getLong("navigation.stress.debug.timeoutMs", DEFAULT_TIMEOUT_MS);
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!taskManager.hasRunningTasks()) {
                return;
            }
            sleep(POLL_MS);
        }
        System.out.println("navigationStressDebug: timeout reached; stopping all tasks");
        taskManager.stopAll();
    }

    private static void printFinalSnapshots(MultiWindowTaskManager taskManager) {
        for (WindowTaskSnapshot snapshot : taskManager.getAllSnapshots()) {
            System.out.println("navigationStressDebug: final window=" + snapshot.getWindowId()
                    + " status=" + snapshot.getStatus()
                    + " lastTask=" + snapshot.getLastTaskType()
                    + " result=" + snapshot.getLastResult()
                    + " message=" + snapshot.getLastMessage()
                    + " queueMessage=" + snapshot.getLastQueueMessage());
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
