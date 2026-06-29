package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR111 Runner-only pathing completion.
 */
public class XiuluoRunnerOnlyPathingCompletionWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String xiuluo = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);

        String waitMethod = between(xiuluo,
                "private XiuluoStepOutcome continueIfNavigationStillPathing(",
                "private XiuluoStepOutcome runLeaderPathingSummonSkillMaintenance(");

        require(waitMethod.contains("WindowPathingState.ARRIVED"),
                "CR111 修罗 pathing wait must consume watcher ARRIVED terminal");
        require(waitMethod.contains("WindowPathingState.STOPPED_AWAY"),
                "CR111 修罗 pathing wait must consume watcher STOPPED_AWAY terminal");
        require(waitMethod.contains("snapshot.isProbeInProgress()"),
                "CR111 修罗 pathing wait must treat watcher probe-in-progress as keep-waiting evidence");
        require(waitMethod.contains("WindowPathingState.ACTIVE")
                        && waitMethod.contains("WindowPathingState.UNKNOWN"),
                "CR111 修罗 pathing wait must keep waiting on watcher ACTIVE/UNKNOWN");
        require(!waitMethod.contains("gameStateUtil.detectMovementState()"),
                "CR111 修罗 intent-backed pathing wait must not use local movement detection to end waiting");
        require(!waitMethod.contains("MovementState.MAYBE_MOVING"),
                "CR111 修罗 MAYBE_MOVING must not permit an intent-backed pathing wait to end");
        require(!waitMethod.contains("navigation pathing wait weak movement ignored"),
                "CR111 修罗 weak local movement must not feed the pathing wait-ended branch");
        require(waitMethod.contains("runner-only pathing wait"),
                "CR111 修罗 wait logs should identify the Runner-only pathing policy");
        require(waitMethod.contains("RUNNER_PATHING_HARD_TIMEOUT_MS"),
                "CR111 修罗 intent-backed pathing wait must have an explicit hard timeout");
        require(indexOf(waitMethod, "intentAgeMs >= RUNNER_PATHING_HARD_TIMEOUT_MS")
                        < indexOf(waitMethod, "snapshot.isProbeInProgress() && probeAgeMs <= OBSERVER_PROBE_MAX_AGE_MS"),
                "CR111 修罗 pathing timeout must be checked before watcher ACTIVE/UNKNOWN/probe keep-wait");
        require(indexOf(waitMethod, "intentAgeMs >= RUNNER_PATHING_HARD_TIMEOUT_MS")
                        < indexOf(waitMethod, "runner-only pathing wait continues"),
                "CR111 修罗 pathing timeout must be checked before generic runner-only keep-wait");

        String shortcutWait = between(xiuluo,
                "private XiuluoStepOutcome waitTrackerShortcutPathing(",
                "private boolean isShortcutTargetMapArrival(");
        require(shortcutWait.contains("WindowPathingState.ACTIVE"),
                "CR111 修罗 shortcut wait must keep waiting on watcher ACTIVE");
        require(shortcutWait.contains("WindowPathingState.ARRIVED")
                        && shortcutWait.contains("WindowPathingState.STOPPED_AWAY"),
                "CR111 修罗 shortcut wait must consume watcher terminal states");
        require(!shortcutWait.contains("gameStateUtil.detectMovementState()"),
                "CR111 修罗 shortcut wait must not use local movement detection to re-read/re-click tracker");
        require(!shortcutWait.contains("MovementState.MAYBE_MOVING"),
                "CR111 修罗 shortcut MAYBE_MOVING must not affect tracker shortcut pathing wait");
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
