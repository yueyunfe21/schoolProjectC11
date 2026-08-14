package com.bot.dhxy.tools.manual;

import com.bot.dhxy.AutoBot;
import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyCommand;
import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyResult;
import com.bot.dhxy.model.navigation.WorldMapSearchTargetCatalog;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.control.WindowTaskControlService;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.observation.StartupCombatGateService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Explicit G051 task-free production-chain acceptance harness.
 *
 * <p>Default invocation is dry-run. Armed mode only submits exact-window MapSurvey commands; Cloud
 * {@code NavigationService} owns route input, OCR, exact-name selection, selected-input confirmation
 * and cleanup. It never presses Enter or clicks a route result. Before the first command it delegates
 * to the Runner's existing local combat gate, so a
 * combat-time launch only observes the exact HWND until combat exits. This tool never recognizes,
 * matches or sends physical input itself.</p>
 */
public final class G051WorldMapCandidateAcceptanceTool {

    private static final ObjectMapper JSON = new ObjectMapper();

    private G051WorldMapCandidateAcceptanceTool() {
    }

    public static void main(String[] args) throws Exception {
        boolean armed = List.of(args).contains("--armed");
        String requestedTarget = List.of(args).stream()
                .filter(argument -> argument.startsWith("--target="))
                .map(argument -> argument.substring("--target=".length()).trim())
                .filter(argument -> !argument.isBlank())
                .findFirst()
                .orElse(null);
        List<WorldMapSearchTargetCatalog.Target> targets = requestedTarget == null
                ? WorldMapSearchTargetCatalog.all()
                : WorldMapSearchTargetCatalog.find(requestedTarget).stream().toList();
        if (requestedTarget != null && targets.isEmpty()) {
            throw new IllegalArgumentException("G051 target is absent from the audited catalog: " + requestedTarget);
        }
        System.out.println("G051 production catalog targets=" + WorldMapSearchTargetCatalog.all().size());
        targets.forEach(target -> System.out.println(
                target.targetMap() + " -> " + target.token() + " [" + target.owningSource() + "]"));
        if (!armed) {
            System.out.println("DRY-RUN only. Use --armed explicitly after opening exactly one game window.");
            return;
        }

        // SpringApplicationBuilder.properties(...) only supplies low-priority defaults and is overridden by
        // application.properties. Use process-local system properties so this armed harness can keep AWT/native
        // capture available without ever creating the JavaFX main window or auto-starting a normal task.
        System.setProperty("bot.run.show-ui", "false");
        System.setProperty("bot.run.auto-start", "false");
        // This catalog drives commands back-to-back. A production 60s idle long-poll would add up to two
        // minutes between targets while the terminal result and its ACK are flushed, without testing any
        // additional behavior. Keep the override process-local; normal clients retain the production default.
        System.setProperty("cloud.turn.long-wait-timeout-ms", "1000");
        try (ConfigurableApplicationContext application = new SpringApplicationBuilder(AutoBot.class)
                .headless(false)
                .run(args)) {
            WindowRuntimeContext windowContext = registerAndResolveExactWindow(application);
            String windowId = windowContext.getWindowId();
            application.getBean(StartupCombatGateService.class).awaitCombatExit(
                    Map.of(windowContext, TaskType.UNKNOWN), Thread.currentThread()::isInterrupted);
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("G051 acceptance interrupted while waiting for combat exit");
            }
            WindowTaskControlService control = application.getBean(WindowTaskControlService.class);
            Path output = Path.of("logs", "g051-manual",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
            Files.createDirectories(output);

            boolean allPassed = true;
            for (WorldMapSearchTargetCatalog.Target target : targets) {
                Path targetDir = output.resolve(target.token() + "-" + target.targetMap());
                Files.createDirectories(targetDir);
                TurnMapSurveyResult result = control.submitMapSurvey(
                                windowId,
                                TurnMapSurveyCommand.Operation.G051_WORLD_MAP_CANDIDATE_ACCEPTANCE,
                                target.targetMap())
                        .get(90, TimeUnit.SECONDS);
                JsonNode evidence = persistProductionEvidence(targetDir, result);
                boolean closeSucceeded = evidence.path("closeSucceeded").asBoolean(false);
                boolean passed = result.status() == TurnMapSurveyResult.Status.COMPLETED
                        && evidence.path("passed").asBoolean(false);
                allPassed &= passed;
                if (!closeSucceeded) {
                    throw new IllegalStateException(
                            "G051 route-panel cleanup failed; aborting before next target: " + target.targetMap());
                }
            }
            if (!allPassed) {
                throw new IllegalStateException("G051 production-chain acceptance FAIL; inspect "
                        + output.toAbsolutePath());
            }
            System.out.println("G051 production-chain acceptance PASS: " + output.toAbsolutePath());
        }
    }

    private static WindowRuntimeContext registerAndResolveExactWindow(ConfigurableApplicationContext application) {
        GameWindowRegistrationService registration = application.getBean(GameWindowRegistrationService.class);
        if (registration.scanGameWindows().size() != 1) {
            throw new IllegalStateException("G051 --armed requires exactly one detected game window");
        }
        registration.registerDetectedGameWindows(TaskType.UNKNOWN);
        MultiWindowTaskManager manager = application.getBean(MultiWindowTaskManager.class);
        var snapshots = manager.getAllSnapshots();
        if (snapshots.size() != 1) {
            throw new IllegalStateException("G051 expected exactly one registered runtime");
        }
        String windowId = snapshots.getFirst().getWindowId();
        WindowRuntimeContext context = manager.getRunner(windowId).orElseThrow().getWindowContext();
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null || binding.getNativeHandle() == null || binding.getNativeHandle().isBlank()
                || binding.getWidth() <= 0 || binding.getHeight() <= 0) {
            throw new IllegalStateException("G051 exact registered HWND/geometry is unavailable");
        }
        return context;
    }

    private static JsonNode persistProductionEvidence(Path targetDir,
                                                      TurnMapSurveyResult result) throws Exception {
        Files.writeString(targetDir.resolve("result.json"), JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(result));
        if (result.evidenceJson() == null || result.evidenceJson().isBlank()) {
            throw new IllegalStateException("G051 Cloud result omitted production evidence");
        }
        JsonNode evidence = JSON.readTree(result.evidenceJson());
        Files.writeString(targetDir.resolve("evidence.json"),
                JSON.writerWithDefaultPrettyPrinter().writeValueAsString(evidence));
        writePng(evidence, "candidateRawPng", targetDir.resolve("candidate-raw.png"));
        writePng(evidence, "candidateMarkedPng", targetDir.resolve("candidate-marked.png"));
        writePng(evidence, "selectedInputRawPng", targetDir.resolve("after-selected-input.png"));
        writePng(evidence, "selectedInputMarkedPng", targetDir.resolve("after-selected-input-marked.png"));
        writePng(evidence, "routeResultRawPng", targetDir.resolve("after-route-result.png"));
        writePng(evidence, "routeResultMarkedPng", targetDir.resolve("after-route-result-marked.png"));
        Files.writeString(targetDir.resolve("candidate-ocr.txt"),
                evidence.path("candidateOcrRows").asText(""));
        Files.writeString(targetDir.resolve("selected-input-ocr.txt"),
                evidence.path("selectedInputOcrRows").asText(""));
        Files.writeString(targetDir.resolve("route-result-ocr.txt"),
                evidence.path("routeResultOcrRows").asText(""));
        return evidence;
    }

    private static void writePng(JsonNode evidence, String field, Path output) throws Exception {
        String base64 = evidence.path(field).asText(null);
        if (base64 != null && !base64.isBlank()) {
            Files.write(output, Base64.getDecoder().decode(base64));
        }
    }
}
