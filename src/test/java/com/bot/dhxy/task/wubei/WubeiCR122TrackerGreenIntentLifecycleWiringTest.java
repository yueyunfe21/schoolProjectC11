package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR122 stale 五倍 tracker-green pathing intents.
 *
 * <p>Live proof needs a real 五倍 tracker green click that enters combat, returns home, and starts
 * a later round. This guard keeps source-prefix cleanup wired at the boundaries that make a later
 * STOPPED_AWAY terminal stale.</p>
 */
public class WubeiCR122TrackerGreenIntentLifecycleWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runtime = read(root, "src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java");
        String runner = read(root, "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java");
        String wubei = read(root, "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java");

        runtimeHasSourceScopedPathingCleanup(runtime);
        runnerClearsTrackerGreenIntentOnWubeiCombatEntry(runner);
        wubeiClearsTrackerGreenIntentAtPreparedConsumeReturnRoundAndNewClick(wubei);
    }

    private static void runtimeHasSourceScopedPathingCleanup(String runtime) {
        String method = between(runtime,
                "public boolean clearPathingSignalIfSourcePrefix(",
                "public void clearPathingSignal(");
        require(method.contains("pathingSnapshot.compareAndSet(snapshot, clearedSnapshot)"),
                "runtime cleanup must atomically clear only the matching active snapshot");
        require(method.contains("activeSource.startsWith(normalizedSourcePrefix)"),
                "runtime cleanup must be scoped by source prefix");
    }

    private static void runnerClearsTrackerGreenIntentOnWubeiCombatEntry(String runner) {
        String publish = between(runner,
                "private void publishCombatStateChanged(",
                "private void clearXiuluoTrackerShortcutPathingOnCombatEntry(");
        require(publish.contains("clearWubeiTrackerGreenPathingOnCombatEntry(taskType, oldTick, newTick, hwnd);"),
                "runner combat-state publish must clear WUBEI tracker-green intent on IN_COMBAT");

        String clearMethod = between(runner,
                "private void clearWubeiTrackerGreenPathingOnCombatEntry(",
                "private void clearXiuluoTrackerShortcutPathingOnCombatEntry(");
        require(clearMethod.contains("taskType != TaskType.WUBEI"),
                "runner cleanup must be scoped to WUBEI only");
        require(clearMethod.contains("newTick != AutoCombatService.TickResult.IN_COMBAT"),
                "runner cleanup must only run on confirmed combat entry");
        require(clearMethod.contains("windowContext.clearPathingSignalIfSourcePrefix("),
                "runner cleanup must use runtime source-prefix cleanup");
        require(clearMethod.contains("wubei:tracker-green-click"),
                "runner cleanup must only target WUBEI tracker-green sources");
    }

    private static void wubeiClearsTrackerGreenIntentAtPreparedConsumeReturnRoundAndNewClick(String wubei) {
        require(wubei.contains("TRACKER_GREEN_PATHING_SOURCE_PREFIX = \"wubei:tracker-green-click\""),
                "Wubei task must own a single tracker-green source prefix constant");

        String resetRound = between(wubei,
                "private void resetRoundState(int round)",
                "private String roundMetricId(");
        require(resetRound.contains("clearTrackerGreenPathingIntent(\"wubei:round-start\")"),
                "round transition must clear any previous-round WUBEI tracker-green pathing intent");

        String consumePrepared = between(wubei,
                "private WubeiStepOutcome consumePreparedEnterBattleBeforeNormalPhase(",
                "private boolean hasFreshPreparedAction(");
        require(consumePrepared.contains("clearTrackerGreenPathingIntent(\"wubei prepared enter battle consumed\")"),
                "prepared enter-battle consume must clear stale WUBEI tracker-green pathing intent");

        String returnHome = between(wubei,
                "private boolean useReturnItemAndVerifyStartMap(",
                "private WubeiStepOutcome returnHomeAfterCombatOrContinueSpecialTarget(");
        require(returnHome.contains("clearTrackerGreenPathingIntent(\"wubei:return-home-verified:\" + source)"),
                "verified return-home must clear any surviving WUBEI tracker-green pathing intent");

        String register = between(wubei,
                "private void registerTrackerPathingIntent(String intentSource)",
                "private Optional<TrackerDestinationHint> captureTrackerDestinationHint(");
        require(register.contains("clearTrackerGreenPathingIntent(\"wubei:new-tracker-green-click:\" + intentSource)"),
                "new tracker green click must clear any older WUBEI tracker-green pathing intent before registering");

        String helper = between(wubei,
                "private boolean clearTrackerGreenPathingIntent(",
                "private void clearCurrentPathingSignal(");
        require(helper.contains("runtime.clearPathingSignalIfSourcePrefix(")
                        && helper.contains("TRACKER_GREEN_PATHING_SOURCE_PREFIX")
                        && helper.contains("reason"),
                "Wubei helper must call runtime source-prefix cleanup");
    }

    private static String read(Path root, String relativePath) throws Exception {
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex + start.length());
        if (endIndex < 0) {
            throw new AssertionError("Missing source marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
