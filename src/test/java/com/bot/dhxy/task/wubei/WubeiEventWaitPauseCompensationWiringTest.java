package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR92 五倍 event-wait pause compensation.
 */
public class WubeiEventWaitPauseCompensationWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        String parkBlock = between(task,
                "private void parkAfterYieldIfNeeded(",
                "private DialogOperation waitOperationForDiagnostics(");
        require(parkBlock.contains("long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested"),
                "五倍 park must capture checkpoint pause blocked time after WindowReadyEventBus wait");
        require(parkBlock.contains("compensateFormalMaintenanceTimers(pauseBlockedMs"),
                "五倍 event-wait pause must reuse CR83 compensation for probe, enter-battle, and ordinary timers");
        require(parkBlock.contains("\"wubei:event-wait:\" + waitSpec.getReason()"),
                "五倍 event-wait pause compensation must have a distinct wait-reason source");
        require(parkBlock.contains("pauseBlockedMs"),
                "五倍 park diagnostics must log pauseBlockedMs beside wall-clock elapsedMs");

        String compensationBlock = between(task,
                "private void compensateFormalMaintenanceTimers(",
                "private WubeiStepOutcome runPhase(");
        require(compensationBlock.contains("compensateProbeTimersAfterPause"),
                "event-wait compensation must keep probe timer coverage");
        require(compensationBlock.contains("compensateEnterBattleTimersAfterPause"),
                "event-wait compensation must keep enter-battle timer coverage");
        require(compensationBlock.contains("pauseOrdinaryPreBattleTimer"),
                "event-wait compensation must keep ordinary pre-battle runtime coverage");
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
