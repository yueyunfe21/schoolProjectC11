package com.bot.dhxy.window.discovery;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level regression test for the main UI refresh path.
 */
public class GameWindowRegistrationEmptyScanPruneWiringTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com", "bot", "dhxy",
                "window", "discovery", "GameWindowRegistrationService.java"), StandardCharsets.UTF_8);
        assertPrunesBeforeEmptyReturn(source, "registerDetectedGameWindows");
        assertPrunesBeforeEmptyReturn(source, "scanRegisterAndStartIndependentWindows");
        System.out.println("GameWindowRegistrationEmptyScanPruneWiringTest passed");
    }

    private static void assertPrunesBeforeEmptyReturn(String source, String methodName) {
        int methodStart = source.indexOf("public WindowTaskCommandResult " + methodName + "(");
        if (methodStart < 0) {
            throw new AssertionError("missing method: " + methodName);
        }
        int scanIndex = source.indexOf("List<NativeWindowInfo> windows = scanGameWindows();", methodStart);
        int pruneIndex = source.indexOf("pruneIdleStaleRegistrations(windows);", methodStart);
        int emptyIndex = source.indexOf("if (windows.isEmpty())", methodStart);
        if (scanIndex < 0 || pruneIndex < 0 || emptyIndex < 0) {
            throw new AssertionError("missing scan/prune/empty branch in " + methodName);
        }
        if (!(scanIndex < pruneIndex && pruneIndex < emptyIndex)) {
            throw new AssertionError(methodName
                    + " must prune stale idle registrations before returning on an empty scan");
        }
    }
}
