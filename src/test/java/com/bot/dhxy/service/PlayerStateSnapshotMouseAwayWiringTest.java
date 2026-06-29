package com.bot.dhxy.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerStateSnapshotMouseAwayWiringTest {

    private static final Path PLAYER_STATE_SERVICE =
            Path.of("src/main/java/com/bot/dhxy/service/PlayerStateService.java");

    public static void main(String[] args) throws Exception {
        String source = Files.readString(PLAYER_STATE_SERVICE);

        int safeRelY = extractIntConstant(source, "SAFE_MOUSE_REL_Y");
        require(safeRelY >= 320,
                "player-state snapshot safe mouse point must leave the top/right hover band: SAFE_MOUSE_REL_Y="
                        + safeRelY);

        int hoverClearDelayMs = extractIntConstant(source, "SAFE_MOUSE_HOVER_CLEAR_DELAY_MS");
        require(hoverClearDelayMs >= 250,
                "player-state snapshot must wait long enough for old buff/status hover to disappear: "
                        + hoverClearDelayMs);

        require(source.contains("InputAction.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS)"),
                "queued player-state snapshot move-away must use SAFE_MOUSE_HOVER_CLEAR_DELAY_MS");
        require(source.contains("TaskSleep.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS)"),
                "direct player-state snapshot move-away must use SAFE_MOUSE_HOVER_CLEAR_DELAY_MS");
    }

    private static int extractIntConstant(String source, String name) {
        Pattern pattern = Pattern.compile("private static final int " + Pattern.quote(name) + " = (\\d+);");
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            throw new AssertionError("missing int constant: " + name);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
