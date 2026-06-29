package com.bot.dhxy.window.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR60 WUBEI combat-entry cleanup wiring.
 *
 * <p>Fresh runtime validation still needs a live 五倍 combat-entry sample. This guard protects the
 * boundary shape: when the watcher observes 五倍 entering combat, stale dialog interest/request and
 * prepared action state must be cleared immediately so combat ticks cannot inherit old dialog
 * preparation state.</p>
 */
public class WubeiCombatEntryDialogCleanupWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runner = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"), StandardCharsets.UTF_8);

        String combatChange = between(runner,
                "private void publishCombatStateChanged(",
                "private void publishOrdinaryPreBattleTimeoutIfNeeded(");
        require(combatChange.contains("clearWubeiDialogStateOnCombatEntry("),
                "WUBEI combat entry must use a dedicated cleanup boundary");

        String cleanup = between(runner,
                "private void clearWubeiDialogStateOnCombatEntry(",
                "private void publishOrdinaryPreBattleTimeoutIfNeeded(");
        require(cleanup.contains("clearDialogInterest(\"wubei combat entered\")"),
                "cleanup must clear current WUBEI dialog interest");
        require(cleanup.contains("clearDialogPreparationRequest(\"wubei combat entered\")"),
                "cleanup must clear current dialog preparation request and READY state");
        require(cleanup.contains("clearPreparedDialogAction(\"wubei combat entered\")"),
                "cleanup must clear stale prepared action even when no request is present");
        require(cleanup.contains("clearOrdinaryEnterBattleTargetMapGate(\"wubei combat entered\")"),
                "cleanup must preserve ordinary enter-battle target-map gate cleanup");
        require(!cleanup.contains("WUBEI_ENTER_BATTLE"),
                "cleanup must not be limited to WUBEI_ENTER_BATTLE interest");

        String loop = between(runner,
                "private void runCombatWatcherLoop(",
                "private boolean sleepObserver(");
        require(loop.contains("if (tick == AutoCombatService.TickResult.IN_COMBAT)"),
                "IN_COMBAT ticks must bypass non-combat observer work");
        require(loop.contains("observerBranch = \"in-combat\""),
                "IN_COMBAT branch must be visible in observer latency logs");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
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
