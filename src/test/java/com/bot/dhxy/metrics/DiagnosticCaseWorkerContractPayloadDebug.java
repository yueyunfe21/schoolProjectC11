package com.bot.dhxy.metrics;

import com.bot.dhxy.model.metrics.AutomationMetricEvent;
import com.bot.dhxy.model.metrics.AutomationMetricEventType;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DiagnosticCaseWorkerContractPayloadDebug {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("target", "diagnostic-case-worker-contract").toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path console = root.resolve("dhxy-console.log");
        Files.writeString(console,
                "2026-06-27 12:15:12.444 WARN  [xiuluo-v2] worker contract payload",
                StandardCharsets.UTF_8);
        DiagnosticCaseCaptureService service = new DiagnosticCaseCaptureService(
                root.resolve("cases"),
                console,
                "machine-worker-contract",
                "dhxy",
                "0.0.contract",
                () -> "license-worker-contract",
                DiagnosticCaseUploaderService.disabled());
        Path casePath = service.captureIfNeeded(event(), List.of(event()))
                .orElseThrow(() -> new AssertionError("case was not written"));
        System.out.println(casePath);
    }

    private static AutomationMetricEvent event() {
        return AutomationMetricEvent.builder()
                .timestamp("2026-06-27T12:15:12.444-04:00")
                .sessionId("worker-contract-session")
                .runId("xiuluo_v2-round-worker-contract")
                .windowId("hwnd-worker-contract")
                .windowRole("LEADER")
                .nativeWindowHandle("12345")
                .taskCode("xiuluo_v2")
                .taskName("修罗")
                .phase("WAIT_TRACKER_SHORTCUT_PATHING")
                .eventType(AutomationMetricEventType.TASK_ROUND_FINISHED)
                .status(AutomationMetricStatus.FAILED)
                .elapsedMs(180_622L)
                .errorCode("PRE_COMBAT_TIMEOUT")
                .message("worker contract payload")
                .attributes(Map.of(
                        "roundId", "xiuluo_v2-round-worker-contract",
                        "teamKey", "xiuluo_v2#worker-contract",
                        "caseCapture", "always"))
                .build();
    }
}
