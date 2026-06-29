package com.bot.dhxy.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR73: Alt+U flying-state probing is a safety helper and must not abort a task.
 */
public class GameStateUtilFlyingStatusExceptionSafetyWiringTest {

    private static final Path GAME_STATE_UTIL = Path.of("src/main/java/com/bot/dhxy/tools/GameStateUtil.java");

    public static void main(String[] args) throws Exception {
        String source = Files.readString(GAME_STATE_UTIL, StandardCharsets.UTF_8);
        int methodStart = source.indexOf("public FlyingState detectFlyingState(String reason)");
        require(methodStart >= 0, "detectFlyingState method must exist");
        int methodEnd = source.indexOf("public BufferedImage captureCurrentMapLabelSnapshot", methodStart);
        require(methodEnd > methodStart, "detectFlyingState source slice must be bounded by the next method");
        String method = source.substring(methodStart, methodEnd);

        require(method.contains("catch (RuntimeException e)"),
                "detectFlyingState must catch template/runtime probe exceptions");
        require(method.contains("return FlyingState.UNKNOWN;"),
                "detectFlyingState must degrade probe exceptions to UNKNOWN");
        require(method.indexOf("catch (RuntimeException e)") < method.indexOf("finally"),
                "detectFlyingState must catch probe exceptions before the Alt+U close finally block");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
