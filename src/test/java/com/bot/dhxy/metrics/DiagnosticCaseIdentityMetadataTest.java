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
import java.util.Optional;

public class DiagnosticCaseIdentityMetadataTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        writesUploadReadyIdentityWhenLicenseExists();
        writesLocalCaseWithUploadBlockerWhenLicenseMissing();
    }

    private static void writesUploadReadyIdentityWhenLicenseExists() throws Exception {
        Path testDir = Files.createTempDirectory("dhxy-case-identity-ready");
        DiagnosticCaseCaptureService service = new DiagnosticCaseCaptureService(
                testDir.resolve("cases"),
                writeConsole(testDir),
                "machine-test-001",
                "dhxy",
                "0.0.test",
                () -> "license-test-001",
                DiagnosticCaseUploaderService.disabled());

        Optional<Path> path = service.captureIfNeeded(failedEvent(), List.of(failedEvent()));
        require(path.isPresent(), "case should be written");
        JsonNode root = OBJECT_MAPPER.readTree(Files.readString(path.get(), StandardCharsets.UTF_8));

        require("machine-test-001".equals(root.path("runtimeSnapshots").path("environment").path("machineHash").asText()),
                "machine hash must be populated");
        require("dhxy".equals(root.path("app").path("appId").asText()), "app id must be populated");
        require("0.0.test".equals(root.path("app").path("version").asText()), "app version must be populated");
        require("license-test-001".equals(root.path("app").path("licenseId").asText()),
                "app license id must be populated");
        require("license-test-001".equals(root.path("licenseId").asText()),
                "top-level license id must be populated for Worker compatibility");
        require(root.path("upload").path("eligible").asBoolean(false), "licensed case must be upload eligible");
        require("PENDING".equals(root.path("upload").path("status").asText()),
                "licensed case should start as PENDING upload");
    }

    private static void writesLocalCaseWithUploadBlockerWhenLicenseMissing() throws Exception {
        Path testDir = Files.createTempDirectory("dhxy-case-identity-blocked");
        DiagnosticCaseCaptureService service = new DiagnosticCaseCaptureService(
                testDir.resolve("cases"),
                writeConsole(testDir),
                "machine-test-002",
                "dhxy",
                "0.0.test",
                () -> "",
                DiagnosticCaseUploaderService.disabled());

        Optional<Path> path = service.captureIfNeeded(failedEvent(), List.of(failedEvent()));
        require(path.isPresent(), "case should still be written without license");
        JsonNode root = OBJECT_MAPPER.readTree(Files.readString(path.get(), StandardCharsets.UTF_8));

        require("machine-test-002".equals(root.path("runtimeSnapshots").path("environment").path("machineHash").asText()),
                "machine hash must still be populated");
        require(root.path("app").path("licenseId").asText("").isBlank(), "missing license must stay blank");
        require(!root.path("upload").path("eligible").asBoolean(true), "unlicensed case must not be upload eligible");
        require("missing-license-id".equals(root.path("upload").path("blocker").asText()),
                "unlicensed case must record upload blocker");
        require("LOCAL_ONLY".equals(root.path("upload").path("status").asText()),
                "unlicensed case must remain local-only");
    }

    private static Path writeConsole(Path dir) throws Exception {
        Path console = dir.resolve("dhxy-console.log");
        Files.writeString(console,
                "2026-06-27 12:15:12.444 WARN  [xiuluo-v2] failure windowId=hwnd-leader",
                StandardCharsets.UTF_8);
        return console;
    }

    private static AutomationMetricEvent failedEvent() {
        return AutomationMetricEvent.builder()
                .timestamp("2026-06-27T12:15:12.444-04:00")
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
                        "teamKey", "xiuluo_v2#8",
                        "caseCapture", "always"))
                .build();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
