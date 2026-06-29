package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR111 五倍 maintenance navigation pathing waits.
 */
public class WubeiRunnerOnlyMaintenancePathingWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String wubei = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        String waitMethod = between(wubei,
                "private WubeiStepOutcome continueIfMaintenanceNavigationStillPathing(",
                "private boolean isHealPetMaintenanceDue(");

        require(waitMethod.contains("currentWindowPathingSnapshot()"),
                "CR111 五倍 maintenance pathing wait must read the Runner/window pathing snapshot");
        require(waitMethod.contains("WindowPathingState.ARRIVED"),
                "CR111 五倍 maintenance pathing wait must consume watcher ARRIVED terminal");
        require(waitMethod.contains("WindowPathingState.STOPPED_AWAY"),
                "CR111 五倍 maintenance pathing wait must consume watcher STOPPED_AWAY terminal");
        require(waitMethod.contains("WindowPathingState.NONE")
                        && waitMethod.contains("WindowPathingState.ACTIVE")
                        && waitMethod.contains("WindowPathingState.UNKNOWN")
                        && waitMethod.contains("snapshot.isProbeInProgress()"),
                "CR111 五倍 maintenance pathing wait must keep waiting on watcher NONE/ACTIVE/UNKNOWN/probe");
        require(waitMethod.contains("WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS"),
                "CR111 五倍 maintenance pathing wait must have an explicit hard timeout");
        require(waitMethod.contains("intentAgeMs"),
                "CR111 五倍 maintenance pathing wait must derive timeout from the current pathing intent age");
        require(indexOf(waitMethod, "intentAgeMs >= WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS")
                        < indexOf(waitMethod, "pathingState == WindowPathingState.NONE"),
                "CR111 五倍 maintenance timeout must be checked before NONE/ACTIVE/UNKNOWN/probe keep-wait");
        require(!waitMethod.contains("gameStateUtil.detectMovementState()"),
                "CR111 五倍 maintenance pathing wait must not use local movement detection to end waiting");
        require(!waitMethod.contains("MovementState.MAYBE_MOVING"),
                "CR111 五倍 MAYBE_MOVING must not permit maintenance pathing wait to end");
        require(waitMethod.contains("runner-only"),
                "CR111 五倍 maintenance logs should identify Runner-only pathing policy");
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

    private static int indexOf(String source, String needle) {
        int index = source.indexOf(needle);
        if (index < 0) {
            throw new AssertionError("Missing source marker: " + needle);
        }
        return index;
    }
}
