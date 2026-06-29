package com.bot.dhxy.window.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR61 in-combat observer pruning.
 *
 * <p>Live validation requires a running game battle. This guard protects the watcher-loop wiring:
 * once the current tick has confirmed {@code IN_COMBAT}, the same observer tick must skip
 * non-combat dialog/pathing/tracker/attention work and must not lower the battle cadence because of
 * residual non-combat state.</p>
 */
public class WindowObserverInCombatSkipWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runner = read(root, "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java");

        String loop = between(runner,
                "private void runCombatWatcherLoop(",
                "private boolean sleepObserver(");
        require(loop.contains("if (tick == AutoCombatService.TickResult.IN_COMBAT)"),
                "runCombatWatcherLoop must branch immediately after combat state refresh when IN_COMBAT");
        require(loop.contains("observerBranch = \"in-combat\""),
                "IN_COMBAT branch must use a distinct observer branch for runtime log validation");

        String combatBranch = between(loop,
                "if (tick == AutoCombatService.TickResult.IN_COMBAT)",
                "} else {");
        require(!combatBranch.contains("refreshPathingSignal("),
                "IN_COMBAT branch must not run pathing probe");
        require(!combatBranch.contains("refreshDialogPreparationSignal("),
                "IN_COMBAT branch must not run route dialog preparation");
        require(!combatBranch.contains("refreshTaskDialogInterestPreparationSignal("),
                "IN_COMBAT branch must not run task dialog preparation");
        require(!combatBranch.contains("refreshTaskTrackerPreparationSignal("),
                "IN_COMBAT branch must not run task tracker preparation");
        require(!combatBranch.contains("publishTaskAttentionIfDialogVisible("),
                "IN_COMBAT branch must not run final task attention detection");
        require(!combatBranch.contains("resolveDialogPrepareIntervalMs("),
                "IN_COMBAT branch must not lower next interval from dialog/pathing prepare state");
        require(combatBranch.contains("autoCombatService.getDynamicPollingIntervalMs()"),
                "IN_COMBAT branch must keep the normal battle/auto-combat cadence");
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
