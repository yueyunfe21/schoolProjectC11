package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR83 五倍 formal-maintenance timer compensation.
 */
public class WubeiMaintenanceTimerPauseWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        require(task.contains("compensateFormalMaintenanceTimers("),
                "WubeiTask must have a formal-maintenance timer compensation boundary");
        String method = between(task,
                "private void compensateFormalMaintenanceTimers(",
                "private WubeiStepOutcome runHotStartDetectPhase(");
        require(method.contains("compensateProbeTimersAfterPause"),
                "formal maintenance compensation must shift probe timers");
        require(method.contains("compensateEnterBattleTimersAfterPause"),
                "formal maintenance compensation must shift enter-battle timers");
        require(method.contains("pauseOrdinaryPreBattleTimer"),
                "formal maintenance compensation must shift WindowRuntime ordinary pre-battle timer");
        require(method.contains("[wubei ordinary-prebattle] timer paused"),
                "ordinary pre-battle maintenance compensation must have a structured log");

        String handoffDelay = between(task,
                "private long handoffDelayMs(",
                "private boolean isChainedPostBattleBroadcastSource(");
        require(handoffDelay.contains("compensateFormalMaintenanceTimers(delayMs"),
                "maintenance broadcast handoff delay must be compensated when it is intentionally returned");
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
