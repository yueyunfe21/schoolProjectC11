package com.bot.dhxy.tools.manual;

import com.bot.dhxy.AutoBot;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.control.WindowTaskControlService;
import com.bot.dhxy.window.control.WindowTaskCommandResult;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Explicit task-free G056 acceptance for the production double-experience maintenance chain.
 *
 * <p>The tool starts the Client without JavaFX, registers all detected HWNDs, starts the selected
 * leader on G056 and every other window on its production AUTO_BATTLE member queue. Armed mode deliberately skips the remaining-time
 * detector so the operator can test the complete path at any current double-experience value. The
 * production owners still perform every action: world-map 长安 label navigation, exact Runner
 * arrival, NPC click planning, and Client-local raw {@code lingshuang.png} matching.</p>
 */
public final class G056DoubleExperienceAcceptanceTool {

    private static final ObjectMapper JSON = new ObjectMapper();

    private G056DoubleExperienceAcceptanceTool() {
    }

    public static void main(String[] args) throws Exception {
        List<String> arguments = List.of(args);
        if (!arguments.contains("--armed")) {
            System.out.println("DRY-RUN only. Use --armed and optionally --window-id=<exact-id>.");
            return;
        }
        String requestedWindowId = arguments.stream()
                .filter(argument -> argument.startsWith("--window-id="))
                .map(argument -> argument.substring("--window-id=".length()).trim())
                .filter(argument -> !argument.isBlank())
                .findFirst()
                .orElse(null);

        System.setProperty("bot.run.show-ui", "false");
        System.setProperty("bot.run.auto-start", "false");
        System.setProperty("cloud.turn.long-wait-timeout-ms", "1000");
        try (ConfigurableApplicationContext application = new SpringApplicationBuilder(AutoBot.class)
                .headless(false)
                .run(args)) {
            AcceptanceWindows windows = registerAndResolveWindows(application, requestedWindowId);
            WindowTaskControlService control = application.getBean(WindowTaskControlService.class);
            long acceptanceStartedAt = System.currentTimeMillis();
            WindowTaskCommandResult started = control.startG056DoubleExperienceAcceptance(
                    windows.allWindowIds(), windows.leader().getWindowId());
            if (!started.isAllSuccess()) {
                throw new IllegalStateException("G056 acceptance did not start: " + started.getMessage());
            }
            MultiWindowTaskManager manager = application.getBean(MultiWindowTaskManager.class);
            WindowTaskSnapshot result;
            Map<String, Long> handledAtByWindow;
            try {
                handledAtByWindow = awaitAllBroadcastClicks(windows, acceptanceStartedAt);
                result = manager.getRunner(windows.leader().getWindowId()).orElseThrow().snapshot();
            } finally {
                control.stopWindows(windows.allWindowIds());
            }

            Path output = Path.of("logs", "g056-double-experience-manual",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
            Files.createDirectories(output);
            Files.writeString(output.resolve("result.json"), JSON.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(java.util.Map.of(
                            "windowId", result.getWindowId(),
                            "lastTask", String.valueOf(result.getLastTaskType()),
                            "acceptanceResult", "SUCCESS",
                            "message", String.valueOf(result.getLastResultMessage()),
                            "broadcastHandledAtByWindow", handledAtByWindow)));
            System.out.println("G056 double-experience acceptance PASS: " + output.toAbsolutePath());
        }
    }

    private static Map<String, Long> awaitAllBroadcastClicks(AcceptanceWindows windows, long startedAtMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 15_000L;
        Map<String, Long> handled = new LinkedHashMap<>();
        while (System.currentTimeMillis() < deadline) {
            handled.clear();
            for (WindowRuntimeContext context : windows.contexts()) {
                long handledAt = context.getLocalMaintenanceBroadcastHandledAt("double-experience-two-hours");
                if (handledAt >= startedAtMs) {
                    handled.put(context.getWindowId(), handledAt);
                }
            }
            if (handled.size() == windows.contexts().size()) {
                return Map.copyOf(handled);
            }
            Thread.sleep(250L);
        }
        List<String> missing = windows.contexts().stream()
                .map(WindowRuntimeContext::getWindowId)
                .filter(windowId -> !handled.containsKey(windowId))
                .toList();
        throw new IllegalStateException("G056 broadcast was not consumed by windows: " + missing);
    }

    private static AcceptanceWindows registerAndResolveWindows(ConfigurableApplicationContext application,
                                                                String requestedWindowId) {
        GameWindowRegistrationService registration = application.getBean(GameWindowRegistrationService.class);
        int detected = registration.scanGameWindows().size();
        if (detected == 0) {
            throw new IllegalStateException("G056 --armed found no game window");
        }
        if (detected > 1 && (requestedWindowId == null || requestedWindowId.isBlank())) {
            throw new IllegalStateException(
                    "G056 found multiple game windows; pass --window-id=<exact-id>");
        }
        registration.registerDetectedGameWindows(TaskType.UNKNOWN);
        MultiWindowTaskManager manager = application.getBean(MultiWindowTaskManager.class);
        String windowId = requestedWindowId == null
                ? manager.getAllSnapshots().getFirst().getWindowId()
                : requestedWindowId;
        List<WindowRuntimeContext> contexts = manager.getAllSnapshots().stream()
                .map(snapshot -> manager.getRunner(snapshot.getWindowId()).orElseThrow().getWindowContext())
                .toList();
        for (WindowRuntimeContext context : contexts) {
            WindowNativeBinding binding = context.getNativeBinding();
            if (binding == null || binding.getNativeHandle() == null || binding.getNativeHandle().isBlank()
                    || binding.getWidth() <= 0 || binding.getHeight() <= 0) {
                throw new IllegalStateException(
                        "G056 registered HWND/geometry is unavailable: " + context.getWindowId());
            }
        }
        WindowRuntimeContext leader = contexts.stream()
                .filter(context -> context.getWindowId().equals(windowId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("G056 leader window is not registered: " + windowId));
        return new AcceptanceWindows(leader, contexts);
    }

    private record AcceptanceWindows(WindowRuntimeContext leader, List<WindowRuntimeContext> contexts) {
        private List<String> allWindowIds() {
            return contexts.stream().map(WindowRuntimeContext::getWindowId).toList();
        }
    }
}
