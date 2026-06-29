package com.bot.dhxy.metrics;

import com.bot.dhxy.model.metrics.AutomationMetricEvent;
import com.bot.dhxy.model.metrics.AutomationMetricEventType;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Behavior checks for first-version local diagnostic case generation.
 */
public class DiagnosticCaseCaptureServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        Path testDir = Files.createTempDirectory("dhxy-diagnostic-case-test");
        Path consoleLog = testDir.resolve("dhxy-console.log");
        Path casesDir = testDir.resolve("cases");
        Files.writeString(consoleLog, String.join(System.lineSeparator(),
                "2026-06-27 12:14:10.111 INFO  [window-runner] member auto-battle waiting windowId=hwnd-member-1 teamKey=xiuluo_v2#8",
                "2026-06-27 12:14:20.222 WARN  [window-runner] route prepared operation=ROUTE_TRANSFER target=蟠桃园 windowId=hwnd-leader",
                "2026-06-27 12:15:03.333 WARN  [xiuluo-v2] consumePrepared result=mismatch expectedOperation=XIULUO_ENTER_BATTLE actual=ROUTE_TRANSFER windowId=hwnd-leader",
                "2026-06-27 12:15:12.444 WARN  [xiuluo-v2] xiuluo pre-combat watchdog timeout round=8 phase=WAIT_TRACKER_SHORTCUT_PATHING elapsedMs=180622 limitMs=180000 source=tracker-shortcut-green-clicked"),
                StandardCharsets.UTF_8);

        DiagnosticCaseCaptureService caseCaptureService = new DiagnosticCaseCaptureService(casesDir, consoleLog);
        AutomationMetricsService metricsService = new AutomationMetricsService(
                testDir.resolve("automation-metrics.jsonl"),
                testDir.resolve("automation-dashboard-data.json"),
                testDir.resolve("automation-dashboard.html"),
                caseCaptureService);

        try {
            metricsService.record(memberTransaction("2026-06-27T12:14:10.111-04:00"));
            metricsService.record(leaderFailedRound("2026-06-27T12:15:12.444-04:00"));
            metricsService.record(memberDuplicateFailure("2026-06-27T12:15:13.100-04:00"));

            List<Path> cases = Files.walk(casesDir)
                    .filter(path -> path.getFileName().toString().endsWith(".case.json"))
                    .sorted()
                    .toList();
            require(cases.size() == 1, "same teamKey incident should create one root case, not " + cases.size());

            JsonNode root = OBJECT_MAPPER.readTree(Files.readString(cases.getFirst(), StandardCharsets.UTF_8));
            require("1".equals(root.path("schemaVersion").asText()), "case schema version must be present");
            require(root.path("caseId").asText().contains("xiuluo_v2"), "case id must name the task");
            require("TASK_ROUND_FINISHED".equals(root.path("trigger").path("eventType").asText()),
                    "case must preserve trigger event type");
            require("PRE_COMBAT_TIMEOUT".equals(root.path("failure").path("errorCode").asText()),
                    "case must preserve stable failure code");
            require("xiuluo_v2".equals(root.path("task").path("taskCode").asText()),
                    "case must preserve task code");
            require("hwnd-leader".equals(root.path("window").path("windowId").asText()),
                    "case must preserve root window id");
            require(root.path("timeline").size() >= 2, "case must include recent metrics timeline");
            require(root.path("metrics").size() >= 2, "case must include raw related metric events");
            require(root.path("consoleLogExcerpt").path("lines").toString().contains("consumePrepared result=mismatch"),
                    "case must include raw console excerpt around failure");
            require(root.path("relatedWindows").size() == 1, "member window must be linked as lightweight evidence");
            require("hwnd-member-1".equals(root.path("relatedWindows").get(0).path("windowId").asText()),
                    "related member evidence must name member window");
            require(root.path("sizePolicy").path("hardCapBytes").asLong() == 2_000_000L,
                    "case must declare the hard size cap");
        } finally {
            metricsService.stop();
        }
    }

    private static AutomationMetricEvent leaderFailedRound(String timestamp) {
        return AutomationMetricEvent.builder()
                .timestamp(timestamp)
                .sessionId("test-session")
                .runId("xiuluo_v2-round-8")
                .windowId("hwnd-leader")
                .windowRole("LEADER")
                .nativeWindowHandle("12345")
                .taskCode("xiuluo_v2")
                .taskName("修罗")
                .phase("WAIT_TRACKER_SHORTCUT_PATHING")
                .eventType(AutomationMetricEventType.TASK_ROUND_FINISHED)
                .status(AutomationMetricStatus.FAILED)
                .elapsedMs(180_622L)
                .errorCode("PRE_COMBAT_TIMEOUT")
                .message("xiuluo pre-combat watchdog timeout")
                .attributes(Map.of(
                        "roundId", "xiuluo_v2-round-8",
                        "roundNumber", "8",
                        "roundType", "修罗",
                        "teamKey", "xiuluo_v2#8",
                        "source", "tracker-shortcut-green-clicked",
                        "pathingState", "ACTIVE",
                        "preparedOperation", "ROUTE_TRANSFER",
                        "visibleDialog", "OPTION"))
                .build();
    }

    private static AutomationMetricEvent memberTransaction(String timestamp) {
        return AutomationMetricEvent.builder()
                .timestamp(timestamp)
                .sessionId("test-session")
                .windowId("hwnd-member-1")
                .windowRole("MEMBER")
                .nativeWindowHandle("67890")
                .taskCode("auto_battle")
                .taskName("自动战斗")
                .phase("auto-battle:WAIT_COMBAT")
                .eventType(AutomationMetricEventType.TASK_TRANSACTION)
                .status(AutomationMetricStatus.SUCCESS)
                .elapsedMs(300L)
                .message("member waiting")
                .attributes(Map.of("teamKey", "xiuluo_v2#8", "actionState", "IN_COMBAT"))
                .build();
    }

    private static AutomationMetricEvent memberDuplicateFailure(String timestamp) {
        return AutomationMetricEvent.builder()
                .timestamp(timestamp)
                .sessionId("test-session")
                .runId("xiuluo_v2-round-8")
                .windowId("hwnd-member-1")
                .windowRole("MEMBER")
                .nativeWindowHandle("67890")
                .taskCode("auto_battle")
                .taskName("自动战斗")
                .phase("auto-battle:WAIT_COMBAT")
                .eventType(AutomationMetricEventType.TASK_ROUND_FINISHED)
                .status(AutomationMetricStatus.FAILED)
                .elapsedMs(180_900L)
                .errorCode("TEAM_ROOT_ALREADY_FAILED")
                .message("member observed same failed team round")
                .attributes(Map.of("roundId", "xiuluo_v2-round-8", "teamKey", "xiuluo_v2#8"))
                .build();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
