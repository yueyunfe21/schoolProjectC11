package com.bot.dhxy.debug;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for the manual Alt+5/Alt+6 startup status probe.
 */
public class StartupAlt5ShoppingProbeDebugWiringTest {

    public static void main(String[] args) throws Exception {
        String debugMain = source("src", "main", "java", "com", "bot", "dhxy", "debug",
                "StartupAlt5ShoppingProbeDebugMain.java");
        String keyboard = source("src", "main", "java", "com", "bot", "dhxy", "driver",
                "BoundWindowKeyboardService.java");
        String startup = source("src", "main", "java", "com", "bot", "dhxy", "window", "startup",
                "TaskStartupWindowPreparationService.java");

        assertContains(keyboard, "ALT_5(\"Alt+5\", 0x35, 0x06, true)");
        assertContains(debugMain, "BoundWindowKeyboardService.AltShortcut.ALT_5");
        assertContains(debugMain, "TaskStartupWindowPreparationService");
        assertContains(debugMain, "startupPreparationService.ensureAlt6Visibility()");
        assertContains(debugMain, "images/template/status/blacklist_shopping.png");
        assertContains(debugMain, "ALT5_SHOPPING_RECT_X_OFFSET = 359");
        assertContains(debugMain, "ALT5_SHOPPING_RECT_Y_OFFSET = 271");
        assertContains(debugMain, "ALT5_SHOPPING_RECT_WIDTH = 317");
        assertContains(debugMain, "ALT5_SHOPPING_RECT_HEIGHT = 288");
        assertContains(debugMain, "alt5ShoppingProbeAfterMarked");
        assertContains(debugMain, "alt6VisibilityExistingLogic");
        assertContains(debugMain, "Imgproc.matchTemplate");

        assertContains(startup, "ALT5_SHOPPING_TEMPLATE = \"images/template/status/blacklist_shopping.png\"");
        assertContains(startup, "ensureStartupVisibilityOverlays()");
        assertContains(startup, "ensureAlt5ShoppingBlacklist()");
        assertContains(startup, "BoundWindowKeyboardService.AltShortcut.ALT_5");
        assertContains(startup, "BoundWindowKeyboardService.AltShortcut.ALT_6");

        System.out.println("StartupAlt5ShoppingProbeDebugWiringTest passed");
    }

    private static String source(String first, String... more) throws Exception {
        return Files.readString(Path.of(first, more), StandardCharsets.UTF_8);
    }

    private static void assertContains(String value, String token) {
        if (!value.contains(token)) {
            throw new AssertionError("Expected token missing: " + token);
        }
    }

}
