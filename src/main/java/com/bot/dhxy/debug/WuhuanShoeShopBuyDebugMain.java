package com.bot.dhxy.debug;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.wuhuan.FiveRingTaskV2;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
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

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Explicit no-UI probe for the 五环 shoe-shop purchase flow.
 *
 * <p>This debug entry still runs inside the selected {@link WindowRuntimeContext}, so screenshots,
 * temp paths, and queued physical input follow the same window binding rules as normal tasks. It is
 * only executed when launched directly from the command line or IDE.</p>
 */
public class WuhuanShoeShopBuyDebugMain {

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
            boolean result = run(app);
            System.out.println("wuhuanShoeShopBuyResult=" + result);
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

    private static boolean run(ConfigurableApplicationContext app) {
        GameWindowRegistrationService registrationService = app.getBean(GameWindowRegistrationService.class);
        MultiWindowTaskManager taskManager = app.getBean(MultiWindowTaskManager.class);
        WindowTaskContextHolder windowHolder = app.getBean(WindowTaskContextHolder.class);
        FiveRingTaskV2 task = app.getBean(FiveRingTaskV2.class);

        registrationService.registerDetectedGameWindows(TaskType.DEBUG_COORDINATE);
        WindowRuntimeContext window = selectWindow(taskManager);
        TaskExecutionContext context = TaskExecutionContext.builder()
                .taskCode("wuhuan-v2-shoe-shop-debug")
                .taskName("五环买鞋调试")
                .retryPolicy(TaskRetryPolicy.none())
                .startedAt(LocalDateTime.now())
                .build();

        AtomicBoolean result = new AtomicBoolean(false);
        windowHolder.runWith(window, () -> window.getGameContext().runWithState(window.getGameState(), () -> {
            try {
                Method method = FiveRingTaskV2.class.getDeclaredMethod(
                        "goShoeShopAndBuyShoes", TaskExecutionContext.class);
                method.setAccessible(true);
                Object value = method.invoke(task, context);
                result.set(Boolean.TRUE.equals(value));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("failed to invoke 五环买鞋调试方法", e);
            }
        }));
        return result.get();
    }

    private static WindowRuntimeContext selectWindow(MultiWindowTaskManager taskManager) {
        String titleContains = System.getProperty("wuhuan.shoe.windowTitleContains", "67555");
        String explicitWindowId = System.getProperty("wuhuan.shoe.windowId", "");
        List<WindowTaskSnapshot> snapshots = taskManager.getAllSnapshots();
        WindowTaskSnapshot selected = snapshots.stream()
                .filter(snapshot -> explicitWindowId.isBlank() || explicitWindowId.equals(snapshot.getWindowId()))
                .filter(snapshot -> titleContains.isBlank()
                        || (snapshot.getNativeTitle() != null && snapshot.getNativeTitle().contains(titleContains)))
                .findFirst()
                .orElseGet(() -> snapshots.stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("no registered game window")));
        WindowTaskRunner runner = taskManager.getRunner(selected.getWindowId())
                .orElseThrow(() -> new IllegalStateException("runner not found: " + selected.getWindowId()));
        System.out.println("selectedWindow=" + selected.getWindowId()
                + " title=" + selected.getNativeTitle()
                + " hwnd=" + selected.getNativeHandle());
        return runner.getWindowContext();
    }
}
