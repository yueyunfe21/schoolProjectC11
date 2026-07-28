package com.bot.dhxy.input;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundInputIsolationContractTest {

    @Test
    void businessCodeHasNoDirectPhysicalKeyboardCalls() throws Exception {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.matches("(?s).*inputProvider\\.(pressAlt|pressCtrl|pressEnter|holdCtrl|releaseCtrl|typeTextUnicode|pasteText).*"),
                        file + " must use exact-HWND keyboard delivery");
            }
        }
    }

    @Test
    void focusAndCapturePathsCannotUseKeyboardOrRobotFallback() throws Exception {
        String focus = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/interaction/WindowFocusService.java"));
        String tracker = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/core/GameClientTracker.java"));
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertFalse(focus.contains("keyPress("));
        assertFalse(focus.contains("keyRelease("));
        assertFalse(tracker.contains("focusCurrentWindowForScreenCaptureWithoutLock"));
        assertTrue(tracker.contains("HWND_CAPTURE_FAILED_NO_FOREGROUND_FALLBACK"));
        assertTrue(properties.contains("bot.window.hwnd-capture-fallback-to-robot-enabled=false"));
    }
}
