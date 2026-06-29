package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for 修罗 startup incense coverage.
 *
 * <p>修罗 hot-start can jump directly to accept-NPC navigation after tracker and return-item
 * fallback. Startup incense must therefore be covered before hot-start phase selection, not only by
 * later PREPARE_ROUND / NAVIGATE_TO_TARGET branches.</p>
 */
public final class XiuluoStartupIncenseBeforeHotStartWiringTest {

    private XiuluoStartupIncenseBeforeHotStartWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String source = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);

        String firstRoundStartup = between(source,
                "boolean afterCombatExitStartup = completedRuns == 0 && context.isAfterCombatExitStartup();",
                "log.info(\"[xiuluo-v2] round {} initial phase:");
        int guardIndex = firstRoundStartup.indexOf("ensureStartupIncenseBeforeHotStart(context");
        int hotStartIndex = firstRoundStartup.indexOf("resolveStartupTrackerOrReturnItem(context");
        require(guardIndex >= 0, "first 修罗 startup must call the startup incense guard");
        require(hotStartIndex >= 0, "first 修罗 startup must still use tracker-first hot-start");
        require(guardIndex < hotStartIndex, "startup incense guard must run before hot-start phase selection");

        String guard = between(source,
                "private void ensureStartupIncenseBeforeHotStart(",
                "private XiuluoStepOutcome checkPreCombatWatchdogTimeout(");
        require(guard.contains("playerStateService.ensureSheYaoXiangActiveForLeaderTask(\"xiuluo-v2:startup\""),
                "startup guard must use the leader-task sheyaoxiang check with a distinct source");
        require(guard.contains("startupIncenseChecked = true"),
                "startup guard must mark incense checked so PREPARE_ROUND does not repeat it");
        require(guard.contains("startupIncensePending = false"),
                "startup guard must clear pending so target navigation does not repeat it");
        require(!guard.contains("startupIncensePending = true"),
                "startup guard must not defer incense to later phases");
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
