package com.bot.dhxy.task.wuhuan;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR108 accept-NPC pathing waits.
 *
 * <p>The 五环 accept-NPC route may use CR99 yellow-destination mini-map navigation. That route can
 * take longer than the old 2.5s observer grace, so the accept phase must not retry current-map
 * navigation while the same pathing intent is still active or the watcher is still probing.</p>
 */
public class FiveRingAcceptNpcPathingWaitPolicyTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String fiveRing = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java"), StandardCharsets.UTF_8);
        String runner = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"), StandardCharsets.UTF_8);

        String acceptWait = between(fiveRing,
                "private FiveRingStepOutcome continueIfAcceptNpcNavigationStillPathing(",
                "private boolean tryClickNearbyAcceptNpc(");
        int fastWaitIndex = acceptWait.indexOf("PATHING_OBSERVER_FAST_WAIT_MS");
        int activeIntentIndex = acceptWait.indexOf("hasActiveAcceptNpcPathingIntent(");
        int hardTimeoutIndex = acceptWait.indexOf("PATHING_TARGET_WAIT_TIMEOUT_MS");
        int hardTimeoutRetryIndex = acceptWait.indexOf("accept NPC navigation hard timeout; retry navigation/click from current state");
        int keepWaitingIndex = acceptWait.indexOf("accept NPC navigation has no terminal snapshot yet; keep waiting");
        require(fastWaitIndex >= 0, "accept wait must keep the fast initial observer grace");
        require(activeIntentIndex > fastWaitIndex,
                "same active accept-NPC pathing intent must be checked after the fast grace");
        require(hardTimeoutIndex > activeIntentIndex,
                "long hard timeout must be checked after in-flight active intent evidence");
        require(hardTimeoutRetryIndex > hardTimeoutIndex,
                "current-state retry must happen only in the hard-timeout branch");
        require(keepWaitingIndex > hardTimeoutRetryIndex,
                "no terminal snapshot before hard timeout must keep waiting instead of retrying");
        require(acceptWait.contains("accept NPC navigation active intent still in flight"),
                "active same-intent branch must keep waiting instead of retrying");
        require(acceptWait.contains("accept NPC navigation hard timeout"),
                "hard timeout branch must be explicit and logged");

        String helper = between(fiveRing,
                "private boolean hasActiveAcceptNpcPathingIntent(",
                "private boolean tryClickNearbyAcceptNpc(");
        require(helper.contains("runtime.getActivePathingIntent()"),
                "same-intent helper must read the runtime active intent");
        require(helper.contains("isExpectedPathingSource(state.pathingIntentSource(), activeIntent.getSource())"),
                "same-intent helper must verify the expected accept-NPC source");
        require(helper.contains("isExpectedPathingTarget(state.pathingIntentSource(), activeIntent)"),
                "same-intent helper must verify the expected accept-NPC target");

        String watcherLoop = between(runner,
                "private void runCombatWatcherLoop(",
                "private boolean sleepObserver(");
        require(watcherLoop.contains("Optional<WindowPathingIntent> activePathingIntentSnapshot"),
                "watcher tick must capture one active-intent snapshot for branch/log consistency");
        require(watcherLoop.contains("activePathingIntentSnapshot.orElse(null)"),
                "watcher tick must pass the captured active intent to logging");

        String logSlow = between(runner,
                "private void logSlowObserverTick(",
                "private void publishCombatStateChanged(");
        require(logSlow.contains("WindowPathingIntent activeIntent"),
                "slow observer log must accept the captured active intent");
        require(!logSlow.contains("windowContext.getActivePathingIntent().orElse(null)"),
                "slow observer log must not reread live active intent after branch decision");

        System.out.println("FiveRingAcceptNpcPathingWaitPolicyTest passed");
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
