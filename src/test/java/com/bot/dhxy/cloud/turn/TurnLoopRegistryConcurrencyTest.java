package com.bot.dhxy.cloud.turn;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnLoopRegistryConcurrencyTest {
    @Test
    void registryUsesAtomicPerWindowLifecycleOperations() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/TurnLoopRegistry.java"));
        assertTrue(source.contains("public synchronized WindowTurnLoop create"));
        assertTrue(source.contains("public synchronized Optional<WindowTurnLoop> find"));
        assertTrue(source.contains("public synchronized WindowTurnLoop remove"));
        assertTrue(source.contains("loopsByWindowId.containsKey(windowId)"));
    }
}
