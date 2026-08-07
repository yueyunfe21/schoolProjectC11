package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundTaskTestControlContractTest {

    @Test
    void dedicatedEntriesReuseProductionControlAndRemainSessionFenced() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/control/BackgroundTaskTestControlService.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("public WindowTaskCommandResult startWuhuanTest()"));
        assertTrue(source.contains("public WindowTaskCommandResult startTiantingTest()"));
        assertTrue(source.contains("startTest(TaskType.WUHuan_V2, maxRuns)"));
        assertTrue(source.contains("startTest(TaskType.TIANTING, maxRuns)"));
        assertTrue(source.contains("registrationService.scanRegisterAndStartIndependentWindows(taskType)"));
        assertTrue(source.contains("windowTaskControlService.pauseAll()"));
        assertTrue(source.contains("windowTaskControlService.resumeAll()"));
        assertTrue(source.contains("windowTaskControlService.stopAll()"));
        assertTrue(source.contains("Objects.equals(sessionId, request.getProperty(\"sessionId\", \"\").trim())"));
        assertTrue(source.contains("hostLockChannel.tryLock()"));

        int stop = source.indexOf("result = ControlResult.from(stop());", source.indexOf("case \"shutdown\""));
        int close = source.indexOf("applicationContext::close", stop);
        assertTrue(stop >= 0 && close > stop, "shutdown must stop production tasks before closing the host");
    }

    @Test
    void defaultRunCountIsOneHundredAndHostIsOptIn() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/config/BackgroundTaskTestProperties.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("private boolean enabled = false;"));
        assertTrue(source.contains("private int defaultMaxRuns = 100;"));

        String script = Files.readString(Path.of("scripts/background-task-test.ps1"), StandardCharsets.UTF_8);
        assertTrue(script.contains("Existing DHXY Client detected; refusing to start a second window owner"));
        assertTrue(script.contains("bot\\.background-test\\.enabled=true"));
        assertTrue(script.contains("[switch]$ElevatedHost"));
        assertTrue(script.contains("$startParameters[\"Verb\"] = \"RunAs\""));
        assertTrue(script.contains("$hostFileInfo.LastWriteTimeUtc -lt $hostProcess.StartTime.ToUniversalTime().AddSeconds(-2)"));
    }
}
