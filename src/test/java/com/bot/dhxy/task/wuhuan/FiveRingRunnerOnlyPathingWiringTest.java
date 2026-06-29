package com.bot.dhxy.task.wuhuan;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR111 五环 WAIT_PATHING policy.
 */
public class FiveRingRunnerOnlyPathingWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String wuhuan = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java"), StandardCharsets.UTF_8);

        String waitMethod = between(wuhuan,
                "private FiveRingStepOutcome waitPathing(",
                "private long pathingAgeMs(");

        require(waitMethod.contains("WindowPathingState.ARRIVED"),
                "CR111 五环 WAIT_PATHING must consume watcher ARRIVED terminal");
        require(waitMethod.contains("WindowPathingState.STOPPED_AWAY"),
                "CR111 五环 WAIT_PATHING must consume watcher STOPPED_AWAY terminal");
        require(waitMethod.contains("WindowPathingState.ACTIVE")
                        && waitMethod.contains("WindowPathingState.UNKNOWN")
                        && waitMethod.contains("snapshot.isProbeInProgress()"),
                "CR111 五环 WAIT_PATHING must keep waiting on watcher ACTIVE/UNKNOWN/probe");
        require(waitMethod.contains("PATHING_TARGET_WAIT_TIMEOUT_MS"),
                "CR111 五环 WAIT_PATHING must use an explicit watcher timeout recovery path");
        require(indexOf(waitMethod, "pathingAgeMs >= PATHING_TARGET_WAIT_TIMEOUT_MS")
                        < indexOf(waitMethod, "observed == WindowPathingState.ACTIVE"),
                "CR111 五环 WAIT_PATHING timeout must be checked before ACTIVE/UNKNOWN/probe keep-wait");
        require(!waitMethod.contains("gameStateUtil.detectMovementState()"),
                "CR111 五环 WAIT_PATHING must not use local movement detection to end waiting");
        require(!waitMethod.contains("isMovingByPixelDiff("),
                "CR111 五环 WAIT_PATHING must not use pixel diff movement to end waiting");

        String acceptNpcWait = between(wuhuan,
                "private FiveRingStepOutcome continueIfAcceptNpcNavigationStillPathing(",
                "private boolean hasActiveAcceptNpcPathingIntent(");
        require(indexOf(acceptNpcWait, "pathingAgeMs >= PATHING_TARGET_WAIT_TIMEOUT_MS")
                        < indexOf(acceptNpcWait, "observed == WindowPathingState.ACTIVE"),
                "CR111 五环 accept-NPC timeout must be checked before watcher ACTIVE/UNKNOWN/probe keep-wait");
        require(indexOf(acceptNpcWait, "pathingAgeMs >= PATHING_TARGET_WAIT_TIMEOUT_MS")
                        < indexOf(acceptNpcWait, "hasActiveAcceptNpcPathingIntent"),
                "CR111 五环 accept-NPC timeout must be checked before active-intent keep-wait");
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
