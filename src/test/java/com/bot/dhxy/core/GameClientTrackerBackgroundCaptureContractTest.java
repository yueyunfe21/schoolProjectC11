package com.bot.dhxy.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameClientTrackerBackgroundCaptureContractTest {

    @Test
    void fullWindowBackgroundRefreshDoesNotAcquirePhysicalInputLock() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/core/GameClientTracker.java"));
        int methodStart = source.indexOf("public boolean updateGlobalVision()");
        int nextMethod = source.indexOf("public boolean locateWindow()", methodStart);
        assertTrue(methodStart >= 0 && nextMethod > methodStart);

        String method = source.substring(methodStart, nextMethod);
        assertTrue(method.contains("captureToFileWithoutLock"));
        assertFalse(method.contains("globalInputLock"),
                "pure exact-HWND background capture must not wait for physical input ownership");
    }
}
