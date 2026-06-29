package com.bot.dhxy.window.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR122 pathing watcher policy.
 *
 * <p>Production navigation now has two default movement classes: tracker/shortcut pathing and
 * mini-map coordinate handoff. The old cross-map coordinate bucket made failed yellow mini-map
 * clicks wait about 30 seconds before STOPPED_AWAY, so the runner must not branch on
 * current-map-vs-target-map for the default targeted path.</p>
 */
public class WindowPathingStoppedAwayPolicyTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runner = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"), StandardCharsets.UTF_8);

        require(!runner.contains("WINDOW_PATHING_COORDINATE_AWAY_STOPPED_MS"),
                "default runner pathing must not keep the old 30s cross-map coordinate bucket");
        require(!runner.contains("WINDOW_PATHING_MAP_ROUTE_STOPPED_AWAY_MS"),
                "default runner pathing must not keep the old 8s map-route bucket");
        require(runner.contains("WINDOW_PATHING_SHORTCUT_STOPPED_AWAY_MS = 2_200L"),
                "shortcut/tracker pathing should use the short stopped-away threshold");
        require(runner.contains("WINDOW_PATHING_MINI_MAP_HANDOFF_STOPPED_AWAY_MS = 2_200L"),
                "mini-map handoff pathing should use the short stopped-away threshold");

        String resolver = between(runner,
                "private long resolvePathingStoppedAwayMs(",
                "private boolean isCurrentPathingIntent(");
        require(resolver.contains("WindowPathingIntentType.UNTARGETED_TRACKER"),
                "resolver must still keep tracker/shortcut pathing as an explicit class");
        require(!resolver.contains("intent.getTargetX() == null || intent.getTargetY() == null"),
                "resolver must not branch into the deprecated map-route bucket");
        require(!resolver.contains("!Objects.equals(intent.getTargetMapName(), currentMapName)"),
                "resolver must not use current-map-vs-target-map to trigger a 30s wait");
        require(resolver.contains("return WINDOW_PATHING_MINI_MAP_HANDOFF_STOPPED_AWAY_MS;"),
                "all default targeted pathing should fall back to the mini-map handoff threshold");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source end marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
