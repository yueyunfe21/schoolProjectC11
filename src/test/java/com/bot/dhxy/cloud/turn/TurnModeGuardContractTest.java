package com.bot.dhxy.cloud.turn;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnModeGuardContractTest {
    @Test
    void localAndRemoteOwnershipRemainMutuallyExclusivePerWindow() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/TurnModeGuard.java"));
        assertTrue(source.contains("startLocal"));
        assertTrue(source.contains("startRemote"));
        assertTrue(source.contains("synchronized (modeMonitor)"));
        assertTrue(source.contains("loopRegistry.find(windowId).isPresent()"));
        assertTrue(source.contains("runner.isRunning()"));
        assertTrue(source.contains("existingLoop.lastFailure()"));
        assertTrue(source.contains("existingLoop.start()"));
    }
}
